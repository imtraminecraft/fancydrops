package dev.fancydrops;

/**
 * Experimentation Table RNG meter items, from the tooltips of the wiki's RNG Meter menu (Pity and RNG Meters > Menu).
 * Work in progress: the Drop Library lists them and /fd sim can play one; nothing reads them from chat yet.
 * Fateful Stinger and Vibrant Coral are not in the wiki's menu picture; they get the chance of every other 500,000 XP item.
 */
public final class ExperimentData {
    /** {name, meter XP needed, icon, odds name, chance text}. */
    public static final String[][] ALL = {
        {"Titanic Experience Bottle", "15,000", "experience_bottle", "Extraordinary", "1 in 89 (1.1236%)"},
        {"Experiment the Fish", "50,000", "cod", "Extraordinary", "1 in 89 (1.1236%)"},
        {"Metaphysical Serum", "50,000", "glass_bottle", "Extraordinary", "1 in 89 (1.1236%)"},
        {"Guardian Pet", "150,000", "prismarine_shard", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Scavenger V", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Sharpness VI", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Life Steal IV", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Power VI", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Ender Slayer VI", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Thunderbolt VI", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Growth VI", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Chance IV", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Blast Protection VI", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Respite III", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Venomous VI", "150,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Projectile Protection VI", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Fire Protection VI", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Woodsplitter VI", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Giant Killer VI", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Drain IV", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Protection VI", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"Titan Killer VI", "150,000", "enchanted_book", "Rare", "1 in 18 (5.618%)"},
        {"A Beginner's Guide to Pesthunting", "500,000", "book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Chain of the End Times", "500,000", "string", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"End Stone Idol", "500,000", "end_stone", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Ensnared Snail", "500,000", "nautilus_shell", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Fateful Stinger", "500,000", "spider_eye", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Gold Bottle Cap", "500,000", "gold_nugget", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Golden Bounty", "500,000", "gold_ingot", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Octopus Tendril", "500,000", "ink_sac", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Severed Hand", "500,000", "bone", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Severed Pincer", "500,000", "iron_nugget", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Troubled Bubble", "500,000", "heart_of_the_sea", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Vibrant Coral", "500,000", "brain_coral", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Blast Protection VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Chance V", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Cleave VI", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Critical VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Cubism VI", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Drain V", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Execute VI", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Fire Protection VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"First Strike V", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Giant Killer VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Gravity VI", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Growth VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Life Steal V", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Looting V", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Luck VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Power VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Projectile Protection VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Prosecute VI", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Protection VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Sharpness VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Snipe IV", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Titan Killer VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Triple-Strike V", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Thunderbolt VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Thunderlord VII", "500,000", "enchanted_book", "RNGesus Incarnate", "1 in 3,704 (0.027%)"},
        {"Nadeshiko Dye", "2,500,000", "pink_dye", "RNGesus Incarnate", "1 in 8,333 (0.012%)"}};

    private static SlayerData.Slayer slayer;
    /** The table as a boss with drops, so /fd sim can play its items with the normal scenes. */
    public static synchronized SlayerData.Slayer slayer() {
        if (slayer == null) {
            SlayerData.Slayer s = new SlayerData.Slayer();
            s.id = "experiments"; s.name = "Experimentation Table"; s.boss = "Experimentation Table"; s.icon = "enchanting_table"; s.color = "#B56BFF";
            for (String[] e : ALL) {
                SlayerData.Drop d = new SlayerData.Drop();
                d.name = e[0]; d.id = id(e[0], e[2]); d.icon = e[2]; d.odds = e[3]; d.chance = e[4];
                s.drops.add(d);
            }
            slayer = s;
        }
        return slayer;
    }
    private static final java.util.Map<String, String> IDS = java.util.Map.ofEntries(
        java.util.Map.entry("Titanic Experience Bottle", "TITANIC_EXP_BOTTLE"),
        java.util.Map.entry("Experiment the Fish", "EXPERIMENT_THE_FISH"),
        java.util.Map.entry("Metaphysical Serum", "METAPHYSICAL_SERUM"),
        java.util.Map.entry("Guardian Pet", "GUARDIAN;4"),
        java.util.Map.entry("Nadeshiko Dye", "DYE_NADESHIKO"),
        java.util.Map.entry("A Beginner's Guide to Pesthunting", "PESTHUNTING_GUIDE"),
        java.util.Map.entry("Chain of the End Times", "CHAIN_END_TIMES"),
        java.util.Map.entry("End Stone Idol", "ENDSTONE_IDOL"));

    /** The SkyBlock / NEU item id: collectibles by NEU id, books as ENCHANTMENT_NAME_LEVEL (the Bazaar id). */
    public static String id(String name, String icon) {
        String k = IDS.get(name);
        if (k != null) return k;
        if ("enchanted_book".equals(icon)) {
            int sp = name.lastIndexOf(' ');
            String base = name.substring(0, sp), lvl = name.substring(sp + 1);
            int n = switch (lvl) { case "I" -> 1; case "II" -> 2; case "III" -> 3; case "IV" -> 4; case "V" -> 5; case "VI" -> 6; case "VII" -> 7; case "VIII" -> 8; case "IX" -> 9; case "X" -> 10; default -> 1; };
            return "ENCHANTMENT_" + base.toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_') + "_" + n;
        }
        return name.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    }
    /** RNG Items (pets, collectibles, dyes) get the longer book; Rare Items and the enchantment books get the normal one. */
    public static boolean isRng(String name) {
        for (String[] e : ALL) if (e[0].equals(name)) return !e[0].matches(".* [IVX]+$") && !e[3].equals("Extraordinary");
        return false;
    }
    private ExperimentData() {}
}
