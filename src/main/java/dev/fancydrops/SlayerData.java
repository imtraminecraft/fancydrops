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

    /** The drop with this name, or null. */
    public static DropData.Drop find(String name) {
        String n = norm(name);
        for (DropData.Source s : ALL) for (DropData.Drop d : s.drops) if (norm(d.name).equals(n)) return d;
        return null;
    }
    /** The boss that drops an item with this name, or null. */
    public static DropData.Source sourceOf(String name) {
        String n = norm(name);
        for (DropData.Source s : ALL) for (DropData.Drop d : s.drops) if (norm(d.name).equals(n)) return s;
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
