package dev.fancydrops;

/**
 * Mythological Ritual treasure: what a burrow can hold instead of a creature, per spade (hypixelskyblock.minecraft.wiki/w/Mythological_Ritual).
 * A burrow is a treasure 25% of the time. Each row: name, item id (or ""), icon, share of treasures, 1 in N burrows with no reforge, 1 in N burrows with Erudite.
 */
final class TreasureData {
    private TreasureData() {}
    static final String[] SPADES = {"Ancestral Spade", "Archaic Spade", "Deific Spade"};
    /** Items that are player heads: their art (a 3D head) is looked up by id like any other drop. */
    static boolean headArt(String id) { return id.equals("MYTHOS_FRAGMENT") || id.equals("MYTH_THE_FISH"); }
    static final int[] CHAIN = {4, 6, 8};                       // burrows in a chain (Erudite adds 2, to 10 at most)
    static final int[] COLS = {0xFF55FF55, 0xFFFFAA00, 0xFFFF55FF};

    static final String[][][] DROPS = {
        {   // Ancestral Spade
            {"Griffin Feather", "GRIFFIN_FEATHER", "feather", "66.67%", "8", "7.2"},
            {"2,500 Coins", "", "gold_nugget", "15.62%", "34.13", "30.72"},
            {"5,000 Coins", "", "gold_nugget", "10.42%", "51.2", "46.08"},
            {"10,000 Coins", "", "gold_ingot", "5.21%", "102.4", "92.16"},
            {"25,000 Coins", "", "gold_ingot", "2.08%", "256", "230.4"},
            {"Myth the Fish", "MYTH_THE_FISH", "player_head", "0.01%", "53,333.33", "48,000"}},
        {   // Archaic Spade
            {"Griffin Feather", "GRIFFIN_FEATHER", "feather", "63.34%", "7.58", "6.97"},
            {"5,000 Coins", "", "gold_nugget", "16.89%", "28.42", "26.15"},
            {"10,000 Coins", "", "gold_ingot", "6.76%", "71.04", "65.37"},
            {"Mythos Fragment", "MYTHOS_FRAGMENT", "player_head", "6.76%", "71.04", "65.37"},
            {"25,000 Coins", "", "gold_ingot", "3.38%", "142.08", "130.74"},
            {"50,000 Coins", "", "gold_block", "1.69%", "284.16", "261.49"},
            {"100,000 Coins", "", "gold_block", "0.84%", "568.32", "522.97"},
            {"250,000 Coins", "", "gold_block", "0.34%", "1,420.8", "1,353.14"},
            {"Myth the Fish", "MYTH_THE_FISH", "player_head", "0.01%", "48,000", "45,714.29"}},
        {   // Deific Spade
            {"Griffin Feather", "GRIFFIN_FEATHER", "feather", "62.71%", "7.29", "7.09"},
            {"10,000 Coins", "", "gold_ingot", "15.52%", "29.45", "28.63"},
            {"Mythos Fragment", "MYTHOS_FRAGMENT", "player_head", "9.31%", "49.08", "47.72"},
            {"25,000 Coins", "", "gold_ingot", "6.21%", "73.62", "71.58"},
            {"50,000 Coins", "", "gold_block", "3.10%", "147.25", "143.16"},
            {"100,000 Coins", "", "gold_block", "1.55%", "294.49", "286.31"},
            {"250,000 Coins", "", "gold_block", "0.78%", "588.98", "572.62"},
            {"500,000 Coins", "", "gold_block", "0.47%", "981.64", "954.37"},
            {"1,000,000 Coins", "", "gold_block", "0.31%", "1,472.46", "1,431.56"},
            {"Braided Griffin Feather", "BRAIDED_GRIFFIN_FEATHER", "string", "0.03%", "14,724.57", "14,315.56"},
            {"Myth the Fish", "MYTH_THE_FISH", "player_head", "0.01%", "45,714.29", "44,444.44"}}};

    /** The source every treasure drop belongs to (a stand-in creature: it has no cube, its drops come from DROPS). */
    static final SlayerData.Slayer SOURCE = new SlayerData.Slayer();
    static { SOURCE.id = "myth_treasure"; SOURCE.name = "Treasure"; SOURCE.boss = "Treasure"; SOURCE.icon = "gold_ingot"; SOURCE.color = "#FFD25A"; }

    /** The chance of one burrow ("1 in 7.29 burrows" -> "1 in 7.29  |  13.72%"); a drop with no figure falls back to its share of the treasures. */
    static String perBurrow(String n, String share) {
        try {
            double v = Double.parseDouble(n.replace(",", "").trim());
            if (v <= 0) return oneIn(share);
            double pc = 100.0 / v;
            String t = v < 10 ? String.format(java.util.Locale.ROOT, "%.1f", v).replaceFirst("\\.0$", "") : String.format(java.util.Locale.ROOT, "%,d", Math.round(v));
            String ps = pc >= 0.1 ? String.format(java.util.Locale.ROOT, "%.2f%%", pc) : String.format(java.util.Locale.ROOT, "%.4f%%", pc);
            return "1 in " + t + "  |  " + ps;
        } catch (RuntimeException e) { return share == null || share.isEmpty() ? "" : oneIn(share); }
    }
    /** "66.67%" -> "1 in 1.5  |  66.67%". */
    static String oneIn(String pct) {
        try {
            double v = Double.parseDouble(pct.replace("%", "").replace(",", "").trim());
            if (v <= 0) return pct;
            double n = 100.0 / v;
            String t = n < 10 ? String.format(java.util.Locale.ROOT, "%.1f", n).replaceFirst("\\.0$", "") : String.format(java.util.Locale.ROOT, "%,d", Math.round(n));
            return "1 in " + t + "  |  " + pct.trim();
        } catch (RuntimeException e) { return pct; }
    }
    /** "1 in 735" -> "1 in 735  |  0.14%". Anything else is returned as it is. */
    static String both(String chance) {
        if (chance == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^1 in ([\\d,]+(?:\\.\\d+)?)$").matcher(chance.trim());
        if (!m.matches()) return chance;
        try { double n = Double.parseDouble(m.group(1).replace(",", "")); return n <= 0 ? chance : chance.trim() + "  |  " + String.format(java.util.Locale.ROOT, "%.2f%%", 100.0 / n); } catch (RuntimeException e) { return chance; }
    }
    /** The coins in a drop called "1,000,000 Coins", or 0. */
    static long coins(String name) {
        if (name == null || !name.toLowerCase(java.util.Locale.ROOT).endsWith(" coins")) return 0L;
        try { return Long.parseLong(name.substring(0, name.length() - 6).replace(",", "").trim()); } catch (RuntimeException e) { return 0L; }
    }
    private static SlayerData.Drop make(String[] e) {
        SlayerData.Drop d = new SlayerData.Drop();
        d.name = e[0]; d.id = e[1].isEmpty() ? e[2].toUpperCase(java.util.Locale.ROOT) : e[1]; d.icon = e[2].equals("player_head") ? "player_head" : e[2]; d.chance = perBurrow(FancyConfig.get().mythErudite ? e[5] : e[4], e[3]);
        long c = coins(e[0]);
        d.rank = c > 0 ? (c >= 500_000 ? 2 : c >= 25_000 ? 1 : 0) : e[0].startsWith("Myth the") ? 3 : e[0].startsWith("Braided") ? 2 : 1;
        return d;
    }
    /** Every drop of a spade, built fresh for the library. */
    static java.util.List<SlayerData.Drop> drops(int sp) {
        java.util.List<SlayerData.Drop> out = new java.util.ArrayList<>();
        for (String[] e : DROPS[Math.max(0, Math.min(2, sp))]) out.add(make(e));
        return out;
    }
    /** Every treasure drop starts from its rarity default (see FancyConfig.initDrop). */
    static void initDefaults(FancyConfig c) {
        for (int sp = 0; sp < 3; sp++) for (SlayerData.Drop d : drops(sp)) FancyConfig.initDrop(c.drop(SOURCE.id, d.name), d.rank);
    }
    /** A drop by its name: the chosen spade first, then the others. Null when it is not a treasure. */
    static SlayerData.Drop find(String name, int sp) {
        if (name == null) return null;
        for (int k = 0; k < 3; k++) for (String[] e : DROPS[(Math.max(0, Math.min(2, sp)) + k) % 3]) if (e[0].equalsIgnoreCase(name.trim())) return make(e);
        long cn = coins(name);                                                                 // any other amount of coins ("12,500,000 coins"): built on the spot, the exact amount
        if (cn > 0) return make(new String[]{String.format(java.util.Locale.ROOT, "%,d Coins", cn), "", cn >= 500_000 ? "gold_block" : cn >= 25_000 ? "gold_ingot" : "gold_nugget", "", "", ""});
        return null;
    }
    /** The listed coin drop whose amount is closest to this one: an unlisted amount follows that row's switches in the library. */
    static String nearestCoins(long amount, int sp) {
        String best = null; double bd = Double.MAX_VALUE;
        for (String[] e : DROPS[Math.max(0, Math.min(2, sp))]) {
            long c = coins(e[0]); if (c <= 0) continue;
            double d = Math.abs(Math.log((double) Math.max(1, amount) / c));
            if (d < bd) { bd = d; best = e[0]; }
        }
        return best;
    }
}
