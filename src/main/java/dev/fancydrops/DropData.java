package dev.fancydrops;

import java.util.ArrayList;
import java.util.List;

/** The shapes every drop library shares: a drop, and the source it comes from (a mob, a chest...). The lists themselves live in MythData, TreasureData... */
public final class DropData {
    public static class Drop {
        public String name = "", id = "", icon = "nether_star", odds = "", chance = "";
        public boolean untradeable = false, skull = false, amount = false;
        public String img = "";                     // the wiki picture file to use (without .png), when it differs from the name
        public String rarity = "";                  // pets: the usual rarity
        public java.util.Map<String, Long> george;  // pets: what George pays per rarity
        public double[] pct;                        // Mythological: the drop chance in percent for a Common..Mythic Griffin pet (0 = that pet cannot meet it)
        public int rank = -1;                       // 0 blue, 1 purple, 2 pink, 3 gold
    }
    public static class Cube { public String base = "#5E9E52", dark = "#3D6B36", eye = "#10200E"; }
    /** Where drops come from: a creature, a chest, a table. */
    public static class Source {
        public String id = "", name = "", boss = "", icon = "nether_star", color = "#FFB300";
        public int minPet = 0;                       // Mythological: the lowest Griffin pet rarity (0 Common .. 5 Mythic) this creature spawns for
        public Cube cube = new Cube();
        public List<Drop> drops = new ArrayList<>();
        public int color() { return hex(color, 0xFFFFB300); }
    }

    public static int hex(String s, int def) {
        try { return 0xFF000000 | (int) Long.parseLong(s.replace("#", "").trim(), 16); } catch (Exception e) { return def; }
    }

    /** "1 in 7,076 (T5)" -> 7076. Drops with no chance sort last. */
    public static double oneIn(Drop d) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("1 in ([0-9][0-9,.]*)").matcher(d.chance == null ? "" : d.chance);
        if (!m.find()) return Double.MAX_VALUE;
        try { return Double.parseDouble(m.group(1).replace(",", "")); } catch (Exception e) { return Double.MAX_VALUE; }
    }

    /** Every source of every library (mythological, slayers, farming, experiments). */
    public static List<Source> sources() {
        List<Source> l = new ArrayList<>(MythData.ALL);
        l.addAll(SlayerData.ALL); l.addAll(FarmData.ALL);
        if (!ExperimentData.SOURCE.id.isEmpty()) l.add(ExperimentData.SOURCE);
        return l;
    }

    /** Drops that can never be sold, so they have no price. */
    public static boolean untradeable(String id) {
        if (id == null) return false;
        for (Source s : sources()) for (Drop d : s.drops) if (d.untradeable && d.id.equals(id)) return true;
        return false;
    }

    /** Loads every drop library. */
    public static void load() { MythData.load(); SlayerData.load(); FarmData.load(); ExperimentData.load(); }

    private DropData() {}
}
