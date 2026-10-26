package dev.fancydrops;

import com.google.gson.Gson;

import java.util.List;
import java.util.Locale;

/** The Farming drop library: Rare Crops, Harvest Feast crops and Pest Drops, loaded from farming.json (same shapes as slayers.json). A source's id is also the id of its animation group. */
public final class FarmData {
    private static class File { List<DropData.Source> sources = new java.util.ArrayList<>(); }

    public static volatile List<DropData.Source> ALL = List.of();

    public static void load() {
        try { ALL = sorted(new Gson().fromJson(DataFiles.read("farming.json"), File.class).sources); }
        catch (Exception e) {
            DataFiles.status = "farming.json error: " + e.getMessage();
            try { ALL = sorted(new Gson().fromJson(DataFiles.bundled("farming.json"), File.class).sources); } catch (Exception ignored) { ALL = List.of(); }
        }
        if (byId("crops") == null || byId("feast") == null || byId("pests") == null || byId("bounty") == null) {          // a file from an older version (other ids): use the bundled one
            try { ALL = sorted(new Gson().fromJson(DataFiles.bundled("farming.json"), File.class).sources); } catch (Exception ignored) { ALL = List.of(); }
        }
        initDefaults(FancyConfig.get());
    }

    private static List<DropData.Source> sorted(List<DropData.Source> in) {
        for (DropData.Source s : in) s.drops.sort(java.util.Comparator.comparingDouble(DropData::oneIn));
        return List.copyOf(in);
    }

    private static final java.util.regex.Pattern RARITY = java.util.regex.Pattern.compile("^(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC)\\s+", java.util.regex.Pattern.CASE_INSENSITIVE);
    /** "LEGENDARY Rat Pet" -> "Rat Pet". */
    public static String stripRarity(String name) { return name == null ? "" : RARITY.matcher(name.trim()).replaceFirst(""); }
    /** "LEGENDARY Rat Pet" -> "LEGENDARY", or "" when the name has no rarity in front. */
    public static String rarityOf(String name) { var m = RARITY.matcher(name == null ? "" : name.trim()); return m.find() ? m.group(1).toUpperCase(Locale.ROOT) : ""; }

    public static DropData.Source byId(String id) { for (DropData.Source s : ALL) if (s.id.equals(id)) return s; return null; }
    /** The drop with this name (a pet's rarity in front is ignored), or null. */
    public static DropData.Drop find(String name) {
        String n = stripRarity(name);
        for (DropData.Source s : ALL) for (DropData.Drop d : s.drops) if (d.name.equalsIgnoreCase(n)) return d;
        return null;
    }
    public static DropData.Source sourceOf(String name) {
        String n = stripRarity(name);
        for (DropData.Source s : ALL) for (DropData.Drop d : s.drops) if (d.name.equalsIgnoreCase(n)) return s;
        return null;
    }
    /** True for a "RARE CROP!" line, or a "RARE DROP!" line for an item only the Farming library lists. */
    public static boolean isFarm(ChatParser.Drop d) {
        if (d.kind().endsWith("CROP")) return find(d.name()) != null;
        return find(d.name()) != null && SlayerData.find(d.name()) == null && !MythData.isMyth(d);
    }

    public static void initDefaults(FancyConfig c) {
        for (DropData.Source s : ALL) for (DropData.Drop d : s.drops) FancyConfig.initDrop(c.drop(s.id, d.name), d.rank);
    }
    private FarmData() {}
}
