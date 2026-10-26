package dev.fancydrops;

import com.google.gson.Gson;

import java.util.List;
import java.util.Locale;

/** The Experimentation Table's RNG meter items, loaded from experiments.json (one source, "exp"). Enchantment books use the Bazaar id ENCHANTMENT_NAME_LEVEL. */
public final class ExperimentData {
    private static class File { List<DropData.Source> sources = new java.util.ArrayList<>(); }

    public static volatile DropData.Source SOURCE = new DropData.Source();

    public static void load() {
        try { SOURCE = first(new Gson().fromJson(DataFiles.read("experiments.json"), File.class)); }
        catch (Exception e) {
            DataFiles.status = "experiments.json error: " + e.getMessage();
            try { SOURCE = first(new Gson().fromJson(DataFiles.bundled("experiments.json"), File.class)); } catch (Exception ignored) { SOURCE = new DropData.Source(); }
        }
        initDefaults(FancyConfig.get());
    }
    private static DropData.Source first(File f) {
        DropData.Source s = f.sources.get(0);
        s.drops.sort(java.util.Comparator.comparingDouble(DropData::oneIn));
        return s;
    }

    private static String norm(String n) { return n == null ? "" : n.trim().toLowerCase(Locale.ROOT); }
    public static DropData.Drop find(String name) {
        String n = norm(name);
        for (DropData.Drop d : SOURCE.drops) if (norm(d.name).equals(n)) return d;
        return null;
    }
    /** RNG items (pets, collectibles, dyes) get the longer book; the enchantment books and Extraordinary items get the normal one. */
    public static boolean isRng(DropData.Drop d) { return d != null && d.rank >= 3; }

    public static void initDefaults(FancyConfig c) {
        for (DropData.Drop d : SOURCE.drops) FancyConfig.initDrop(c.drop(SOURCE.id, d.name), d.rank);
    }
    private ExperimentData() {}
}
