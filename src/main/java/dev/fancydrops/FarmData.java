package dev.fancydrops;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Locale;

/** The Farming drop library: rare crops, Harvest / Grand Feast crops and pest drops, loaded from farming.json (same shapes as slayers.json). */
public final class FarmData {
    private static class File { List<SlayerData.Slayer> sources = new java.util.ArrayList<>(); }

    public static volatile List<SlayerData.Slayer> ALL = List.of();

    public static void load() {
        try {
            ALL = sorted(new Gson().fromJson(DataFiles.read("farming.json"), File.class).sources);
        } catch (Exception e) {
            DataFiles.status = "farming.json error: " + e.getMessage();
            try { ALL = sorted(new Gson().fromJson(DataFiles.bundled("farming.json"), File.class).sources); } catch (Exception ignored) { ALL = List.of(); }
        }
    }

    /** Most common drop first, rarest last (drops without a chance go to the end). */
    private static List<SlayerData.Slayer> sorted(List<SlayerData.Slayer> in) {
        for (SlayerData.Slayer s : in) s.drops.sort(java.util.Comparator.comparingDouble(SlayerData::oneIn));
        return List.copyOf(in);
    }

    /** True when this SkyBlock id is one of the farming drops. */
    public static boolean hasId(String id) {
        for (SlayerData.Slayer s : ALL) for (SlayerData.Drop d : s.drops) if (d.id.equalsIgnoreCase(id)) return true;
        return false;
    }

    private static final java.util.regex.Pattern RARITY = java.util.regex.Pattern.compile("^(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC)\\s+", java.util.regex.Pattern.CASE_INSENSITIVE);
    /** "LEGENDARY Rat Pet" -> "Rat Pet". */
    public static String stripRarity(String name) { return name == null ? "" : RARITY.matcher(name.trim()).replaceFirst(""); }
    /** "LEGENDARY Rat Pet" -> "LEGENDARY", or "" when the name has no rarity in front. */
    public static String rarityOf(String name) { var m = RARITY.matcher(name == null ? "" : name.trim()); return m.find() ? m.group(1).toUpperCase(Locale.ROOT) : ""; }
    /** The wiki picture file for a drop id (its "img", else its name), or null. */
    public static String imgOf(String id) {
        for (SlayerData.Slayer s : ALL) for (SlayerData.Drop d : s.drops) if (d.id.equalsIgnoreCase(id)) return d.img.isBlank() ? d.name : d.img;
        return null;
    }

    /** The farming source that lists this drop, or null. */
    public static SlayerData.Slayer sourceOf(String name) {
        name = stripRarity(name);
        for (SlayerData.Slayer s : ALL) for (SlayerData.Drop d : s.drops) if (d.name.equalsIgnoreCase(name)) return s;
        return null;
    }

    /** True for a "RARE CROP!" line, or a "RARE DROP!" line for an item that only the Farming library lists. */
    public static boolean isFarm(ChatParser.Drop d) {
        if (d.kind().endsWith("CROP")) return true;
        if (sourceOf(d.name()) == null) return false;
        for (SlayerData.Slayer s : SlayerData.ALL) for (SlayerData.Drop dr : s.drops) if (dr.name.equalsIgnoreCase(d.name())) return false;
        return true;
    }

    /** Version number inside the bundled file, so a newer mod refreshes an older copy in config/fancydrops. */
    public static int version(String json) {
        try { return JsonParser.parseString(json).getAsJsonObject().get("v").getAsInt(); } catch (Exception e) { return 0; }
    }

    private FarmData() {}
}
