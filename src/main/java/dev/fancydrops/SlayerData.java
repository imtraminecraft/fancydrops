package dev.fancydrops;

import com.google.gson.Gson;

import java.util.List;
import java.util.Locale;

/** The Slayer drop library: the six bosses and their rare drops, loaded from slayers.json (same shapes as mythological.json). */
public final class SlayerData {
    private static class File { List<DropData.Source> sources = new java.util.ArrayList<>(); }

    public static volatile List<DropData.Source> ALL = List.of();

    public static void load() {
        try { ALL = sorted(new Gson().fromJson(DataFiles.read("slayers.json"), File.class).sources); }
        catch (Exception e) {
            DataFiles.status = "slayers.json error: " + e.getMessage();
            try { ALL = sorted(new Gson().fromJson(DataFiles.bundled("slayers.json"), File.class).sources); } catch (Exception ignored) { ALL = List.of(); }
        }
        initDefaults(FancyConfig.get());
    }

    private static List<DropData.Source> sorted(List<DropData.Source> in) {
        for (DropData.Source s : in) s.drops.sort(java.util.Comparator.comparingDouble(DropData::oneIn));
        return List.copyOf(in);
    }

    private static String norm(String name) { return name == null ? "" : name.trim().toLowerCase(Locale.ROOT); }

    /** A name as the words it is made of, so "Enchanted Book (Smite VI)", "(Smite VI)", "Smite VI" and "Smite 6" are the same book and "Bundle (Quantum)" equals "Quantum Bundle". */
    private static java.util.Set<String> words(String name) {
        java.util.TreeSet<String> w = new java.util.TreeSet<>();
        if (name == null) return w;
        for (String t : name.toLowerCase(Locale.ROOT).replace("\u2019", "'").replaceAll("[^a-z0-9]+", " ").trim().split(" ")) {
            if (t.isEmpty() || t.equals("enchanted") || t.equals("book") || t.equals("the")) continue;
            w.add(t);
        }
        return w;
    }
    private static boolean same(String a, String b) { return norm(a).equals(norm(b)) || (!words(a).isEmpty() && words(a).equals(words(b))); }

    /** The drop with this name, or null. */
    public static DropData.Drop find(String name) {
        for (DropData.Source s : ALL) for (DropData.Drop d : s.drops) if (same(d.name, name)) return d;
        return null;
    }
    /** Like find, but a boss that drops the item too (the one that just spawned) wins over the first one in the list. */
    public static DropData.Drop find(String name, DropData.Source prefer) {
        if (prefer != null) for (DropData.Drop d : prefer.drops) if (same(d.name, name)) return d;
        return find(name);
    }
    public static DropData.Source sourceOf(String name, DropData.Source prefer) {
        if (prefer != null) for (DropData.Drop d : prefer.drops) if (same(d.name, name)) return prefer;
        return sourceOf(name);
    }
    /** The boss that drops an item with this name, or null. */
    public static DropData.Source sourceOf(String name) {
        for (DropData.Source s : ALL) for (DropData.Drop d : s.drops) if (same(d.name, name)) return s;
        return null;
    }
    public static DropData.Source byId(String id) { for (DropData.Source s : ALL) if (s.id.equals(id)) return s; return null; }
    /** True for a rare-drop line of a boss the library lists (and not one Mythological already owns). */
    public static boolean isSlayer(ChatParser.Drop d) { return find(d.name()) != null && !MythData.isMyth(d); }

    /** The theme of the slayer scene: 0 zombie, 1 spider, 2 wolf, 3 enderman, 4 blaze, 5 vampire. */
    public static int theme(String id) {
        return switch (id) { case "spider" -> 1; case "wolf" -> 2; case "enderman" -> 3; case "blaze" -> 4; case "vampire" -> 5; default -> 0; };
    }

    /** The skull each boss uses as its icon: a SkyBlock head (a pet's) where there is no Minecraft one, and the vanilla item shown until it loads. */
    public static String headId(String id) {
        return switch (id) { case "spider" -> "SPIDER;4"; case "wolf" -> "WOLF;4"; case "enderman" -> "ENDERMAN;4"; case "blaze" -> "@BOSS2D_BLAZE"; case "vampire" -> "@BOSS2D_VAMPIRE"; default -> "@BOSS2D_ZOMBIE"; };
    }
    public static String headFallback(String id) { return id.equals("zombie") ? "zombie_head" : "player_head"; }

    public static void initDefaults(FancyConfig c) {
        for (DropData.Source s : ALL) for (DropData.Drop d : s.drops) FancyConfig.initDrop(c.drop(s.id, d.name), d.rank);
    }
    private SlayerData() {}
}
