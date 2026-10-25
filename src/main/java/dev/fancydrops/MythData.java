package dev.fancydrops;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Locale;

/** The Mythological Ritual drop library: every Diana creature and its rare drops, loaded from mythological.json (same shapes as farming.json). */
public final class MythData {
    private static class File { List<SlayerData.Slayer> sources = new java.util.ArrayList<>(); }

    public static volatile List<SlayerData.Slayer> ALL = List.of();

    public static void load() {
        try {
            ALL = sorted(new Gson().fromJson(DataFiles.read("mythological.json"), File.class).sources);
            applyPet(FancyConfig.get().mythPet);
            initDefaults(FancyConfig.get()); TreasureData.initDefaults(FancyConfig.get());
        } catch (Exception e) {
            DataFiles.status = "mythological.json error: " + e.getMessage();
            try { ALL = sorted(new Gson().fromJson(DataFiles.bundled("mythological.json"), File.class).sources); } catch (Exception ignored) { ALL = List.of(); }
        }
    }

    /** The creatures a Griffin pet of this rarity can call up. */
    public static List<SlayerData.Slayer> spawnable(int pet) {
        List<SlayerData.Slayer> l = new java.util.ArrayList<>();
        for (SlayerData.Slayer s : ALL) if (s.minPet <= pet) l.add(s);
        return l;
    }
    public static final String[] PETS = {"Common", "Uncommon", "Rare", "Epic", "Legendary", "Mythic"};

    /** The lowest Griffin pet rarity a drop can be had with (its first non-zero chance), or 0. */
    public static int minPet(SlayerData.Drop d) {
        if (d.pct == null) return 0;
        for (int i = 0; i < d.pct.length; i++) if (d.pct[i] > 0) return i;
        return 0;
    }
    /** Writes the chance of every drop for this Griffin pet rarity ("1 in 50" for Mythic, "1 in 400" for Common) and sorts again. A pet too weak for a drop shows that drop's lowest rarity. */
    public static void applyPet(int pet) {
        pet = Math.max(0, Math.min(5, pet));
        for (SlayerData.Slayer s : ALL) {
            for (SlayerData.Drop d : s.drops) {
                if (d.pct == null || d.pct.length < 6) continue;
                double v = d.pct[Math.max(pet, minPet(d))];
                if (v > 0) d.chance = "1 in " + String.format(Locale.ROOT, "%,d", Math.round(100.0 / v));
            }
            s.drops.sort(java.util.Comparator.comparingDouble(SlayerData::oneIn));
        }
    }

    private static List<SlayerData.Slayer> sorted(List<SlayerData.Slayer> in) {
        for (SlayerData.Slayer s : in) {                                       // no Enchanted Gold Ingot and no shards; Chimera I is an RNG drop (also for a saved copy of the data)
            s.drops.removeIf(d -> d.name.equals("Enchanted Gold Ingot") || d.name.endsWith(" Shard"));
            for (SlayerData.Drop d : s.drops) if (d.name.equals("Chimera I")) d.rank = 2;
        }
        for (SlayerData.Slayer s : in) s.drops.sort(java.util.Comparator.comparingDouble(SlayerData::oneIn));
        return List.copyOf(in);
    }

    /** The creature that drops an item with this name, or null. "Enchanted Book (Chimera I)" and "Chimera" count as Chimera I. */
    public static SlayerData.Slayer sourceOf(String name) { return find(name) == null ? null : sourceFor(find(name)); }
    public static SlayerData.Drop find(String name) {
        String n = norm(name);
        for (SlayerData.Slayer s : ALL) for (SlayerData.Drop d : s.drops) if (norm(d.name).equals(n)) return d;
        return null;
    }
    private static SlayerData.Slayer sourceFor(SlayerData.Drop dr) {
        for (SlayerData.Slayer s : ALL) if (s.drops.contains(dr)) return s;
        return null;
    }
    private static String norm(String name) {
        String n = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (n.contains("chimera")) return "chimera i";
        return n;
    }

    /** True for a rare-drop line of a creature the library lists (and that no slayer lists too). */
    public static boolean isMyth(ChatParser.Drop d) {
        if (find(d.name()) == null) return false;
        for (SlayerData.Slayer s : SlayerData.ALL) for (SlayerData.Drop dr : s.drops) if (dr.name.equalsIgnoreCase(d.name())) return false;
        return true;
    }

    /** The wiki sprite of each creature (used as its icon when it can be downloaded). */
    private static final java.util.Map<String, String> SPRITES = java.util.Map.ofEntries(
        java.util.Map.entry("minos_hunter", "Minos_Hunter_Sprite"), java.util.Map.entry("siamese_lynxes", "Siamese_Lynxes_Sprite"), java.util.Map.entry("minotaur", "Minotaur_Sprite"),
        java.util.Map.entry("gaia_construct", "Iron_Golem_Sprite"), java.util.Map.entry("minos_champion", "Minos_Champion_Sprite"), java.util.Map.entry("minos_inquisitor", "Minos_Inquisitor_Sprite"),
        java.util.Map.entry("manticore", "Manticore_Sprite"), java.util.Map.entry("king_minos", "King_Minos_Sprite"), java.util.Map.entry("stranded_nymph", "Stranded_Nymph_Sprite"),
        java.util.Map.entry("harpy", "Harpy_Sprite"), java.util.Map.entry("sphinx", "Sphinx_Sprite"), java.util.Map.entry("cretan_bull", "Cretan_Bull_Sprite"));
    /** The wiki picture file a drop asks for (shards, the dye), or null. */
    public static String imgOf(String id) {
        for (SlayerData.Slayer s : ALL) for (SlayerData.Drop d : s.drops) if (d.id.equalsIgnoreCase(id) && !d.img.isBlank()) return d.img;
        return null;
    }
    public static boolean isDye(SlayerData.Drop d) { return d.name.toLowerCase(Locale.ROOT).endsWith(" dye"); }
    /** The on/off switches of a drop. The Mythological Dye shares them with the Dye page, so turning one off turns the other off too. */
    public static FancyConfig.DropCfg cfg(FancyConfig c, SlayerData.Slayer s, SlayerData.Drop d) { return isDye(d) ? c.drop("dyes", d.name) : c.drop(s.id, d.name); }

    /** Every drop starts from its rarity default (see FancyConfig.initDrop), so the menu and the game agree before the library is ever opened. */
    public static void initDefaults(FancyConfig c) {
        for (SlayerData.Slayer s : ALL) for (SlayerData.Drop d : s.drops) if (!isDye(d)) FancyConfig.initDrop(cfg(c, s, d), d.rank);
    }

    public static boolean hasId(String id) {
        for (SlayerData.Slayer s : ALL) for (SlayerData.Drop d : s.drops) if (d.id.equalsIgnoreCase(id)) return true;
        return false;
    }

    public static int version(String json) {
        try { return JsonParser.parseString(json).getAsJsonObject().get("v").getAsInt(); } catch (Exception e) { return 0; }
    }

    private MythData() {}
}
