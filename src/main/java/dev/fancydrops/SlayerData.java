package dev.fancydrops;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Slayer + drop tables, loaded from slayers.json. */
public final class SlayerData {
    public static class Drop {
        public String name = "", id = "", icon = "nether_star", odds = "", chance = "";
        public boolean untradeable = false, skull = false, amount = false;
        public String img = "";                     // farming drops: the wiki picture file to use (without .png), when it differs from the name
        public String rarity = "";                  // pets: the usual rarity
        public java.util.Map<String, Long> george;  // pets: what George pays per rarity
        public double[] pct;                        // Mythological: the drop chance in percent for a Common..Mythic Griffin pet (0 = that pet cannot meet it)
        public int rank = -1;                       // farming drops: 0 blue, 1 purple, 2 pink, 3 gold (slayer drops take it from the chat line)
    }
    public static class Cube { public String base = "#5E9E52", dark = "#3D6B36", eye = "#10200E"; }
    public static class Slayer {
        public String id = "", name = "", boss = "", icon = "nether_star", color = "#FFB300";
        public int minPet = 0;                       // Mythological: the lowest Griffin pet rarity (0 Common .. 5 Mythic) this creature spawns for
        public Cube cube = new Cube();
        public List<Drop> drops = new ArrayList<>();
        public int color() { return hex(color, 0xFFFFB300); }
    }
    private static class File { List<Slayer> slayers = new ArrayList<>(); }

    public static volatile List<Slayer> ALL = List.of();

    public static int hex(String s, int def) {
        try { return 0xFF000000 | (int) Long.parseLong(s.replace("#", "").trim(), 16); } catch (Exception e) { return def; }
    }

    /** Used for custom / unrecognised drops. */
    public static final Slayer OTHER = new Slayer();
    static { OTHER.id = "other"; OTHER.name = "Other"; OTHER.boss = "Rare Drop"; }

    /** Drops that can never be sold, so they have no price. Also settable per drop with "untradeable": true. */
    private static final Set<String> ALWAYS_UNTRADEABLE = Set.of("MCGRUBBERS_BURGER");
    public static boolean untradeable(String id) {
        if (id == null) return false;
        if (ALWAYS_UNTRADEABLE.contains(id)) return true;
        for (Slayer s : ALL) for (Drop d : s.drops) if (d.untradeable && d.id.equals(id)) return true;
        return false;
    }

    public static void load() {
        try {
            ALL = sorted(new Gson().fromJson(DataFiles.read("slayers.json"), File.class).slayers);
        } catch (Exception e) {
            DataFiles.status = "slayers.json error: " + e.getMessage();
            ALL = sorted(new Gson().fromJson(DataFiles.bundled("slayers.json"), File.class).slayers);
        }
        DyeData.load();
        FarmData.load();
        MythData.load();
    }
    /** "1 in 7,076 (T5)" -> 7076. Drops with no chance sort last. */
    public static double oneIn(Drop d) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("1 in ([0-9][0-9,.]*)").matcher(d.chance == null ? "" : d.chance);
        if (!m.find()) return Double.MAX_VALUE;
        try { return Double.parseDouble(m.group(1).replace(",", "")); } catch (Exception e) { return Double.MAX_VALUE; }
    }

    /** Most common drop first, rarest last. */
    private static List<Slayer> sorted(List<Slayer> in) {
        for (Slayer s : in) s.drops.sort(java.util.Comparator.comparingDouble(SlayerData::oneIn));   // stable: equal chances keep file order
        return List.copyOf(in);
    }
    private SlayerData() {}
}
