package dev.fancydrops;

import java.util.List;

/** Every dye that drops by chance (not bought or crafted), grouped by how rare it is. Numbers are the wiki's base chances. */
final class DyeData {
    static final String[] RARITY = {"Mythic", "Legendary", "Epic", "Rare", "Uncommon", "Common", "Special"};
    static final int[] RARITY_COL = {0xFFFF55FF, 0xFFFFAA00, 0xFFAA00AA, 0xFF5555FF, 0xFF55FF55, 0xFFFFFFFF, 0xFF55FFFF};

    record Dye(String name, int rarity, String src, String slayer) {
        String id() { return name.toUpperCase(java.util.Locale.ROOT).replace(' ', '_'); }
        String owner() { return slayer == null ? "dyes" : slayer; }
    }

    private static final List<Dye> DEFAULTS = List.of(
        new Dye("Aquamarine Dye", 0, "Fishing anywhere  ·  1 in 10M", null),
        new Dye("Mango Dye", 0, "Breaking logs on public islands  ·  1 in 10M", null),
        new Dye("Wild Strawberry Dye", 0, "Breaking crops  ·  1 in 150M", null),
        new Dye("Mocha Dye", 0, "Brewing potions  ·  1 in 100M to 250k", null),
        new Dye("Cyclamen Dye", 0, "Various mobs  ·  1 in 10M to 250k", null),
        new Dye("Pearlescent Dye", 0, "End mobs and dragons  ·  1 in 10M to 50k", null),
        new Dye("Matcha Dye", 0, "Revenant Horror  ·  1 in 15M to 500k", "zombie"),
        new Dye("Brick Red Dye", 0, "Tarantula Broodfather  ·  1 in 15M to 1M", "spider"),
        new Dye("Celeste Dye", 0, "Sven Packmaster  ·  1 in 15M to 1M", "wolf"),
        new Dye("Byzantium Dye", 0, "Voidgloom Seraph  ·  1 in 15M to 1M", "enderman"),
        new Dye("Flame Dye", 0, "Inferno Demonlord  ·  1 in 15M to 1M", "blaze"),
        new Dye("Bone Dye", 1, "Killing any Skeleton  ·  1 in 3M", null),
        new Dye("Carmine Dye", 1, "Various mobs  ·  1 in 5M to 250k", null),
        new Dye("Emerald Dye", 1, "Mining emeralds  ·  1 in 5M", null),
        new Dye("Jade Dye", 1, "Crystal Nucleus  ·  1 in 2M", null),
        new Dye("Iceberg Dye", 1, "Fishing on Jerry's Workshop  ·  1 in 2M", null),
        new Dye("Fossil Dye", 1, "Excavating  ·  1 in 1M", null),
        new Dye("Midnight Dye", 1, "Spooky mobs and sea creatures  ·  1 in 1M", null),
        new Dye("Pelt Dye", 1, "Trapper animals  ·  1 in 1M to 50k", null),
        new Dye("Secret Dye", 1, "Dungeon secrets  ·  1 in 1M", null),
        new Dye("Dung Dye", 2, "Vacuuming pests  ·  1 in 500k", null),
        new Dye("Periwinkle Dye", 2, "Runic mobs  ·  1 in 500k", null),
        new Dye("Frostbitten Dye", 2, "Glacite corpses  ·  1 in 500k to 25k", null),
        new Dye("Nyanza Dye", 2, "Commissions  ·  1 in 250k", null),
        new Dye("Tentacle Dye", 2, "Kuudra  ·  1 in 250k to 50k", null),
        new Dye("Sangria Dye", 2, "Riftstalker Bloodfiend  ·  1 in 150k to 25k", "vampire"),
        new Dye("Copper Dye", 3, "Garden visitors  ·  1 in 100k to 500", null),
        new Dye("Nadeshiko Dye", 3, "Experiments  ·  1 in 50k to 10k", null),
        new Dye("Celadon Dye", 3, "Bacte  ·  1 in 20k", null),
        new Dye("Archfiend Dye", 4, "Archfiend Dice  ·  1 in 6.6k", null),
        new Dye("Livid Dye", 4, "Master Mode Floor 5 chest  ·  1 in 5k", null),
        new Dye("Holly Dye", 4, "Red gifts  ·  1 in 4k", null),
        new Dye("Necron Dye", 4, "Master Mode Floor 7 chest  ·  1 in 2.5k", null),
        new Dye("Dark Purple Dye", 5, "Dark Auction  ·  1 in 500", null),
        new Dye("Mythological Dye", 1, "Mythological Ritual creatures  ·  1 in 1M to 50k", null),
        new Dye("Pure Blue Dye", 6, "Incredible Raffle Box", null),
        new Dye("Pure Yellow Dye", 6, "Incredible Raffle Box", null));

    /** The live list: dyes.json from config/fancydrops (edit it and press Reload), or the built-in one if it can't be read. */
    static volatile List<Dye> ALL = DEFAULTS;

    private static class File { List<Dye> dyes = new java.util.ArrayList<>(); }

    static void load() {
        try {
            File f = new com.google.gson.Gson().fromJson(DataFiles.read("dyes.json"), File.class);
            List<Dye> ok = new java.util.ArrayList<>();
            if (f != null && f.dyes != null) for (Dye d : f.dyes) if (d != null && d.name() != null && !d.name().isBlank() && d.src() != null && d.rarity() >= 0 && d.rarity() < RARITY.length) ok.add(d);
            for (Dye d : DEFAULTS) { boolean has = false; for (Dye o : ok) if (o.name().equalsIgnoreCase(d.name())) { has = true; break; } if (!has && !ok.isEmpty()) ok.add(d); }     // dyes added by a newer mod appear in an older dyes.json too
            ALL = ok.isEmpty() ? DEFAULTS : List.copyOf(ok);
        } catch (Exception e) {
            DataFiles.status = "dyes.json error: " + e.getMessage();
            ALL = DEFAULTS;
        }
    }

    private static final String[] VAN = {"white_dye", "light_gray_dye", "gray_dye", "black_dye", "brown_dye", "red_dye", "orange_dye", "yellow_dye", "lime_dye", "green_dye", "cyan_dye", "light_blue_dye", "blue_dye", "purple_dye", "magenta_dye", "pink_dye"};
    private static final int[] VAN_COL = {0xF9FFFE, 0x9D9D97, 0x474F52, 0x1D1D21, 0x835432, 0xB02E26, 0xF9801D, 0xFED83D, 0x80C71F, 0x5E7C16, 0x169C9C, 0x3AB3DA, 0x3C44AA, 0x8932B8, 0xC74EBD, 0xF38BAA};

    /** The vanilla dye item closest to this dye's colour, used as its little icon. */
    static String icon(String name) {
        int c = AnimationManager.dyeColor(name, 0x888888), best = 0; long bd = Long.MAX_VALUE;
        for (int i = 0; i < VAN.length; i++) {
            int dr = ((c >> 16) & 255) - ((VAN_COL[i] >> 16) & 255), dg = ((c >> 8) & 255) - ((VAN_COL[i] >> 8) & 255), db = (c & 255) - (VAN_COL[i] & 255);
            long d = (long) dr * dr + (long) dg * dg + (long) db * db;
            if (d < bd) { bd = d; best = i; }
        }
        return VAN[best];
    }

    private DyeData() {}
}
