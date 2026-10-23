package dev.fancydrops;

import net.fabricmc.loader.api.FabricLoader;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Editable data lives in config/fancydrops/*.json (in dev: run/config/fancydrops/).
 * On first launch the bundled copy from src/main/resources/fancydrops/ is copied there,
 * so you can tweak drops / colours in VS Code and hit "Reload data files" in-game.
 */
public final class DataFiles {
    public static volatile String status = "Bundled data";

    public static Path dir() {
        Path now = FabricLoader.getInstance().getConfigDir().resolve("fancydrops");
        if (!Files.exists(now)) {                       // first run after the RNGDrops -> FancyDrops rename: bring the old data folder along
            Path old = FabricLoader.getInstance().getConfigDir().resolve("rngdrops");
            if (Files.isDirectory(old)) {
                try (var walk = Files.walk(old)) {
                    for (Path src : (Iterable<Path>) walk::iterator) {
                        Path dst = now.resolve(old.relativize(src).toString());
                        if (Files.isDirectory(src)) Files.createDirectories(dst); else Files.copy(src, dst);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
        return now;
    }

    public static String bundled(String name) {
        try (InputStream in = DataFiles.class.getResourceAsStream("/fancydrops/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new RuntimeException("Missing bundled " + name, e); }
    }

    /** Returns the user's file (creating it from the bundled copy if needed). */
    public static String read(String name) {
        try {
            Files.createDirectories(dir());
            Path p = dir().resolve(name);
            if (!Files.exists(p)) {
                Files.writeString(p, bundled(name));
            } else if ("farming.json".equals(name)) {
                if (FarmData.version(Files.readString(p)) < FarmData.version(bundled(name))) Files.writeString(p, bundled(name));     // a newer mod brings a newer drop list
            } else if ("mythological.json".equals(name)) {
                if (MythData.version(Files.readString(p)) < MythData.version(bundled(name))) Files.writeString(p, bundled(name));
            } else if ("slayers.json".equals(name)) {
                // Upgrade existing user config additively: retain custom slayers and user edits,
                // but merge any newly bundled slayer/drop entries so the main menu is current.
                mergeBundledSlayerDrops(p);
            }
            return Files.readString(p);
        } catch (Exception e) {
            status = "Could not read " + name + ": " + e.getMessage();
            return bundled(name);
        }
    }


    private static void mergeBundledSlayerDrops(Path target) {
        try {
            JsonObject current = JsonParser.parseString(Files.readString(target)).getAsJsonObject();
            JsonObject defaults = JsonParser.parseString(bundled("slayers.json")).getAsJsonObject();
            JsonArray currentSlayers = current.getAsJsonArray("slayers");
            JsonArray defaultSlayers = defaults.getAsJsonArray("slayers");
            boolean changed = false;
            for (JsonElement defaultElement : defaultSlayers) {
                JsonObject defaultSlayer = defaultElement.getAsJsonObject();
                String defaultId = stringValue(defaultSlayer, "id");
                JsonObject targetSlayer = null;
                for (JsonElement currentElement : currentSlayers) {
                    JsonObject candidate = currentElement.getAsJsonObject();
                    if (defaultId.equalsIgnoreCase(stringValue(candidate, "id"))) {
                        targetSlayer = candidate;
                        break;
                    }
                }
                if (targetSlayer == null) {
                    currentSlayers.add(defaultSlayer.deepCopy());
                    changed = true;
                    continue;
                }
                // Built-in slayer loot tables are authoritative: replace stale built-in drops
                // so removed legacy entries disappear from /fancydrops, while custom slayers remain.
                JsonArray bundledDrops = defaultSlayer.getAsJsonArray("drops");
                JsonArray targetDrops = targetSlayer.getAsJsonArray("drops");
                if (targetDrops == null || !targetDrops.toString().equals(bundledDrops.toString())) {
                    targetSlayer.add("drops", bundledDrops.deepCopy());
                    changed = true;
                }
                if (defaultSlayer.has("cube") && !defaultSlayer.get("cube").toString().equals(String.valueOf(targetSlayer.get("cube")))) {
                    targetSlayer.add("cube", defaultSlayer.get("cube").deepCopy());
                    changed = true;
                }
                // Keep the current canonical display metadata for the built-in slayer too.
                for (String key : new String[] {"name", "boss", "icon", "color"}) {
                    String value = stringValue(defaultSlayer, key);
                    if (!value.equals(stringValue(targetSlayer, key))) {
                        targetSlayer.addProperty(key, value);
                        changed = true;
                    }
                }
            }
            if (changed) Files.writeString(target, new GsonBuilder().setPrettyPrinting().create().toJson(current));
        } catch (Exception e) {
            // Leave an invalid/custom file untouched; SlayerData.load() reports the parse error.
        }
    }

    private static String stringValue(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    public static void reloadAll() {
        status = "Reloaded";
        SlayerData.load();
    }
    private DataFiles() {}
}
