package dev.fancydrops;

import net.fabricmc.loader.api.FabricLoader;


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

    /** Everything you can edit by hand lives in config/fancydrops/art/: icons/ (the little menu icons), heads/ (3D head skins), items/ (flat item pictures), animations/ (notes: the scenes are drawn in code). */
    public static Path art(String sub) {
        Path root = dir().resolve("art"), p = root.resolve(sub);
        try {
            if (!Files.exists(root)) {                                                    // the first run after the folders were gathered: bring the earlier icons/ and textures/ along
                Files.createDirectories(root.resolve("icons")); Files.createDirectories(root.resolve("heads")); Files.createDirectories(root.resolve("items")); Files.createDirectories(root.resolve("animations"));
                Path oi = dir().resolve("icons"), ot = dir().resolve("textures");
                if (Files.isDirectory(oi)) try (var st = Files.list(oi)) { for (Path f : (Iterable<Path>) st::iterator) { if (Files.isRegularFile(f)) Files.move(f, root.resolve("icons").resolve(f.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING); } }
                if (Files.isDirectory(ot)) try (var st = Files.list(ot)) { for (Path f : (Iterable<Path>) st::iterator) { if (Files.isRegularFile(f)) Files.move(f, root.resolve(f.getFileName().toString().endsWith("_head.png") ? "heads" : "items").resolve(f.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING); } }
            }
            Files.createDirectories(p);
        } catch (Exception ignored) {}
        return p;
    }

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
            } else if (java.util.List.of("mythological.json", "slayers.json", "farming.json", "experiments.json").contains(name)) {
                if (MythData.version(Files.readString(p)) < MythData.version(bundled(name))) Files.writeString(p, bundled(name));
            }
            return Files.readString(p);
        } catch (Exception e) {
            status = "Could not read " + name + ": " + e.getMessage();
            return bundled(name);
        }
    }


    public static void reloadAll() {
        status = "Reloaded";
        DropData.load();
    }
    private DataFiles() {}
}
