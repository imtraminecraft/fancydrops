package dev.fancydrops;

import java.util.ArrayList;
import java.util.List;

/**
 * Every drop category the mod knows (Mythological, Slayers, Farming...) and what lives inside it. The menu builds the Drop Library,
 * Animations, Sounds and Chat pages from this list, so a category, an animation group or a mob added here shows up on all of them.
 */
final class Categories {
    /** One animation type of a group: what the Animation Editor lets you edit (No Drops, Normal, RNG...). */
    record Type(String id, String name, String icon, int color, String blurb) {}
    /** One thing that has its own animations: "Mythological Mobs", "Treasure". Its keys look like "myth.rng". */
    record Group(String id, String name, String icon, int color, String blurb, List<Type> types, boolean wip) {
        Group(String id, String name, String icon, int color, String blurb, List<Type> types) { this(id, name, icon, color, blurb, types, false); }
        String key(Type t) { return id + "." + t.id(); }
        Type type(String typeId) { for (Type t : types) if (t.id().equals(typeId)) return t; return types.get(0); }
    }
    /** A sub page of the menu. wip = nothing built yet (shows the construction sign). */
    record Cat(String id, String name, String icon, int color, boolean wip, String blurb, List<Group> groups) {
        /** Where this category's drops come from: a creature, a chest... (the Drop Library's sub-sub pages). */
        List<DropData.Source> sources() {
            List<DropData.Source> l = new ArrayList<>();
            if (id.equals("mythological")) { l.addAll(sources("myth")); l.addAll(sources("treasure")); }
            if (id.equals("bosses")) l.addAll(sources("slayer"));
            if (id.equals("farming")) for (Group g : groups) l.addAll(sources(g.id()));
            if (id.equals("experiments")) l.addAll(sources("exp"));
            return l;
        }
        /** The sources inside one group: the creatures of "myth", the burrow of "treasure". */
        List<DropData.Source> sources(String groupId) {
            List<DropData.Source> l = new ArrayList<>();
            if (id.equals("bosses")) { if (groupId.equals("slayer")) l.addAll(SlayerData.ALL); return l; }
            if (id.equals("farming")) { DropData.Source f = FarmData.byId(groupId); if (f != null) l.add(f); return l; }
            if (id.equals("experiments")) { if (groupId.equals("exp") && !ExperimentData.SOURCE.id.isEmpty()) l.add(ExperimentData.SOURCE); return l; }
            if (!id.equals("mythological")) return l;
            if (groupId.equals("myth")) l.addAll(MythData.spawnable(FancyConfig.get().mythPet));
            if (groupId.equals("treasure")) {
                TreasureData.SOURCE.drops = new ArrayList<>(TreasureData.drops(FancyConfig.get().mythSpade));          // the spade you picked decides what a burrow can hold
                l.add(TreasureData.SOURCE);
            }
            return l;
        }
    }

    static final Type NO_DROPS = new Type("nodrops", "No Drops", ":nodrops:", 0xFF9AA3B5, "The ritual ends and nothing drops.");
    static final Type NORMAL = new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A drop: the item shows with a glow and its text.");
    static final Type RNG = new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "A rare drop: cinematic bars, blur and the big finish.");
    private static final List<Type> MYTH_TYPES = List.of(NO_DROPS, NORMAL, RNG);
    private static final List<Type> SLAYER_TYPES = List.of(
        new Type("nodrops", "No Drops", ":nodrops:", 0xFF9AA3B5, "The boss falls and nothing drops."),
        new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A drop: a sword slices the boss and the item springs out with its text."),
        new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "A rare drop: cinematic bars, blur and a huge finishing slice."));

    /** Farming and Experiments: each group has the normal and the RNG animation only (a crop or a book always drops something). */
    private static final Type FARM_NORMAL = new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A drop: the plant grows and the item blooms with its text.");
    private static final Type FARM_RNG = new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "A rare drop: cinematic bars, blur and a golden bloom.");
    private static final List<Type> FARM_TYPES = List.of(FARM_NORMAL, FARM_RNG);
    private static final List<Type> PEST_TYPES = List.of(
        new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A drop: a pest scuttles through the barn, the vacuum pulls it up and the item springs out with its text."),
        new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "A rare drop: cinematic bars, blur and a big finish."));
    private static final List<Type> EXP_TYPES = List.of(
        new Type("normal", "Normal", ":normal:", 0xFF55D8FF, "A book opens and turns three pages: the items rise from its light with their text."),
        new Type("rng", "RNG", ":dice:", 0xFFFF72C6, "An RNG item: the book turns three pages in a storm of light, with cinematic bars and blur."));

    /** True for a group whose Drop Library page is one list of drops (no creatures to pick first). */
    static boolean single(Group g) { return !g.id().equals("myth") && !g.id().equals("slayer"); }
    /** True for a group whose Drop Library page lists creatures, each with its own page. */
    static boolean hasSubs(Group g) { return g.id().equals("myth") || g.id().equals("slayer"); }

    static final List<Cat> ALL = List.of(
        new Cat("mythological", "Mythological", ":mytho:", 0xFF55D8FF, false, "Mythological Ritual is an event that occurs with the Mythological Ritual perk.", List.of(
            new Group("myth", "Mythological Mobs", ":mytho:", 0xFF55D8FF, "What plays when a ritual creature dies.", MYTH_TYPES),
            new Group("treasure", "Treasure", ":treasure:", 0xFFFFD25A, "What plays when a burrow holds treasure.", MYTH_TYPES))),
        new Cat("bosses", "Bosses", ":slayers:", 0xFFFF5A5A, false, "Bosses of every kind and their rare drops.", List.of(
            new Group("slayer", "Slayers", ":grp_slayers:", 0xFFFF5A5A, "What plays when a slayer boss drops something.", SLAYER_TYPES),
            new Group("rift", "The Rift", ":grp_rift:", 0xFFB56BFF, "A mirror dimension of the main server.", SLAYER_TYPES, true),
            new Group("crimson", "Crimson Isle", ":grp_crimson:", 0xFFFF6A3A, "The Nether of Hypixel SkyBlock.", SLAYER_TYPES, true))),
        new Cat("farming", "Farming", ":farm:", 0xFF7BE05A, false, "Every rare drop you can find in the Garden.", List.of(
            new Group("crops", "Rare Crops", ":grp_crops:", 0xFF7CCB4F, "What plays when a crop turns out rare.", FARM_TYPES),
            new Group("feast", "Harvest Feast", ":grp_feast:", 0xFFFFA63A, "What plays when the Harvest Feast serves a rare crop.", FARM_TYPES),
            new Group("bounty", "Harvest Bounty", ":grp_bounty:", 0xFFB8E860, "What plays when a Harvest Bounty drops something.", FARM_TYPES),
            new Group("pests", "Pest Drops", ":grp_pests:", 0xFFD2603F, "What plays when a pest drops something.", PEST_TYPES))),
        new Cat("experiments", "Rare Drops", ":raredrops:", 0xFFB56BFF, false, "Rare drops from the Experimentation Table, rare mobs and events.", List.of(
            new Group("exp", "Experimentation Table", ":grp_exp:", 0xFFB56BFF, "What plays when the Table gives out rewards.", EXP_TYPES),
            new Group("mobs", "Rare Mob Drops", ":grp_mobs:", 0xFFFF8A5A, "Rare drops of everyday mobs.", EXP_TYPES, true),
            new Group("events", "Events", ":grp_events:", 0xFF55D8FF, "Rare drops of the server's events.", EXP_TYPES, true))),
        new Cat("mining", "Mining", ":mining:", 0xFF9AB8FF, true, "Rare finds underground.", List.of()),
        new Cat("fishing", "Fishing", ":fishing:", 0xFF4FC3FF, true, "Sea creatures and what they carry.", List.of()),
        new Cat("dyes", "Dyes", ":dye:", 0xFFFF72C6, true, "Every dye in the game.", List.of()));

    static Cat byId(String id) { for (Cat c : ALL) if (c.id().equals(id)) return c; return ALL.get(0); }
    static Group group(String key) {
        String g = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
        for (Cat c : ALL) for (Group gr : c.groups()) if (gr.id().equals(g)) return gr;
        return ALL.get(0).groups().get(0);
    }

    private Categories() {}
}
