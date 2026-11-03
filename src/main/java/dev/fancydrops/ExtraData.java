package dev.fancydrops;

import com.google.gson.Gson;

import java.util.*;

/**
 * The newer drop libraries, all in the same shape: Fishing (sea creatures, one group per kind of water), Crimson Isle bosses,
 * Rare Mob Drops and Dyes. Each source names its animation group ("fish_lava", "crimson", "mobs", "dyes"), and the menu
 * (Categories) builds its pages from these lists, so a creature added to a file shows up on every page.
 */
public final class ExtraData {
    private static class File { List<DropData.Source> sources = new ArrayList<>(); }
    static final String[] FILES = {"fishing.json", "crimson.json", "mobs.json", "dyes.json"};
    /** group id -> its sources. */
    public static volatile Map<String, List<DropData.Source>> BY_GROUP = Map.of();
    public static volatile List<DropData.Source> ALL = List.of();

    public static void load() {
        Map<String, List<DropData.Source>> m = new LinkedHashMap<>(); List<DropData.Source> all = new ArrayList<>();
        for (String fn : FILES) {
            List<DropData.Source> l;
            try { l = new Gson().fromJson(DataFiles.read(fn), File.class).sources; }
            catch (Exception e) {
                DataFiles.status = fn + " error: " + e.getMessage();
                try { l = new Gson().fromJson(DataFiles.bundled(fn), File.class).sources; } catch (Exception ignored) { l = List.of(); }
            }
            for (DropData.Source s : l) {
                s.drops.sort(Comparator.comparingDouble(DropData::oneIn));
                m.computeIfAbsent(s.group == null || s.group.isEmpty() ? "mobs" : s.group, k -> new ArrayList<>()).add(s); all.add(s);
            }
        }
        BY_GROUP = m; ALL = List.copyOf(all);
        FancyConfig c = FancyConfig.get();
        for (DropData.Source s : all) for (DropData.Drop d : s.drops) FancyConfig.initDrop(c.drop(s.id, d.name), d.rank);
    }

    public static List<DropData.Source> group(String g) { return BY_GROUP.getOrDefault(g, List.of()); }
    public static DropData.Source byId(String id) { for (DropData.Source s : ALL) if (s.id.equals(id)) return s; return null; }

    /** The drop with this name, or null. A pet line ("PET DROP! Baby Yeti") matches "Baby Yeti Pet" or "Epic Flying Fish Pet". */
    public static DropData.Drop find(String name) { Object[] h = hit(name); return h == null ? null : (DropData.Drop) h[1]; }
    public static DropData.Source sourceOf(String name) { Object[] h = hit(name); return h == null ? null : (DropData.Source) h[0]; }
    private static Object[] hit(String name) {
        String n = FarmData.stripRarity(name), low = n.toLowerCase(java.util.Locale.ROOT);
        for (DropData.Source s : ALL) for (DropData.Drop d : s.drops) if (DropData.loose(d.name, n) || DropData.loose(d.name, name)) return new Object[]{s, d};
        for (DropData.Source s : ALL) for (DropData.Drop d : s.drops) {
            String dl = d.name.toLowerCase(java.util.Locale.ROOT);
            if (dl.equals(low + " pet") || dl.endsWith(" " + low + " pet")) return new Object[]{s, d};
        }
        return null;
    }
    private ExtraData() {}
}
