package dev.fancydrops;

/**
 * Mythological Ritual treasure: what a burrow can hold instead of a creature, per spade (hypixelskyblock.minecraft.wiki/w/Mythological_Ritual).
 * A burrow is a treasure 25% of the time. Each row: name, item id (or ""), icon, share of treasures, 1 in N burrows with no reforge, 1 in N burrows with Erudite.
 */
final class TreasureData {
    private TreasureData() {}
    static final String[] SPADES = {"Ancestral Spade", "Archaic Spade", "Deific Spade"};
    static final int[] CHAIN = {4, 6, 8};                       // burrows in a chain (Erudite adds 2, to 10 at most)
    static final int[] COLS = {0xFF55FF55, 0xFFFFAA00, 0xFFFF55FF};

    static final String[][][] DROPS = {
        {   // Ancestral Spade
            {"Griffin Feather", "GRIFFIN_FEATHER", "feather", "66.67%", "8", "7.2"},
            {"2,500 Coins", "", "gold_nugget", "15.62%", "34.13", "30.72"},
            {"5,000 Coins", "", "gold_nugget", "10.42%", "51.2", "46.08"},
            {"10,000 Coins", "", "gold_ingot", "5.21%", "102.4", "92.16"},
            {"25,000 Coins", "", "gold_ingot", "2.08%", "256", "230.4"},
            {"Myth the Fish", "MYTH_THE_FISH", "tropical_fish", "0.01%", "53,333.33", "48,000"}},
        {   // Archaic Spade
            {"Griffin Feather", "GRIFFIN_FEATHER", "feather", "63.34%", "7.58", "6.97"},
            {"5,000 Coins", "", "gold_nugget", "16.89%", "28.42", "26.15"},
            {"10,000 Coins", "", "gold_ingot", "6.76%", "71.04", "65.37"},
            {"Mythos Fragment", "MYTHOS_FRAGMENT", "prismarine_crystals", "6.76%", "71.04", "65.37"},
            {"25,000 Coins", "", "gold_ingot", "3.38%", "142.08", "130.74"},
            {"50,000 Coins", "", "gold_block", "1.69%", "284.16", "261.49"},
            {"100,000 Coins", "", "gold_block", "0.84%", "568.32", "522.97"},
            {"250,000 Coins", "", "gold_block", "0.34%", "1,420.8", "1,353.14"},
            {"Myth the Fish", "MYTH_THE_FISH", "tropical_fish", "0.01%", "48,000", "45,714.29"}},
        {   // Deific Spade
            {"Griffin Feather", "GRIFFIN_FEATHER", "feather", "62.71%", "7.29", "7.09"},
            {"10,000 Coins", "", "gold_ingot", "15.52%", "29.45", "28.63"},
            {"Mythos Fragment", "MYTHOS_FRAGMENT", "prismarine_crystals", "9.31%", "49.08", "47.72"},
            {"25,000 Coins", "", "gold_ingot", "6.21%", "73.62", "71.58"},
            {"50,000 Coins", "", "gold_block", "3.10%", "147.25", "143.16"},
            {"100,000 Coins", "", "gold_block", "1.55%", "294.49", "286.31"},
            {"250,000 Coins", "", "gold_block", "0.78%", "588.98", "572.62"},
            {"500,000 Coins", "", "gold_block", "0.47%", "981.64", "954.37"},
            {"1,000,000 Coins", "", "gold_block", "0.31%", "1,472.46", "1,431.56"},
            {"Braided Griffin Feather", "BRAIDED_GRIFFIN_FEATHER", "feather", "0.03%", "14,724.57", "14,315.56"},
            {"Myth the Fish", "MYTH_THE_FISH", "tropical_fish", "0.01%", "45,714.29", "44,444.44"}}};
}
