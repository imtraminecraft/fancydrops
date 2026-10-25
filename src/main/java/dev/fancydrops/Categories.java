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
    record Group(String id, String name, String icon, int color, String blurb, List<Type> types) {
        String key(Type t) { return id + "." + t.id(); }
        Type type(String typeId) { for (Type t : types) if (t.id().equals(typeId)) return t; return types.get(0); }
    }
    /** A sub page of the menu. wip = nothing built yet (shows the construction sign). */
    record Cat(String id, String name, String icon, int color, boolean wip, String blurb, List<Group> groups) {
        /** Where this category's drops come from: a creature, a chest... (the Drop Library's sub-sub pages). */
        List<DropData.Source> sources() {
            List<DropData.Source> l = new ArrayList<>();
            if (id.equals("mythological")) l.addAll(MythData.spawnable(FancyConfig.get().mythPet));
            if (id.equals("treasure")) {
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

    static final List<Cat> ALL = List.of(
        new Cat("mythological", "Mythological", ":mytho:", 0xFF55D8FF, false, "Diana's Mythological Ritual: the creatures you call from a burrow.", List.of(
            new Group("myth", "Mythological Mobs", ":mytho:", 0xFF55D8FF, "What plays when a ritual creature dies.", MYTH_TYPES))),
        new Cat("treasure", "Treasure", ":treasure:", 0xFFFFD25A, false, "The treasure you dig up from a burrow with your spade.", List.of(
            new Group("treasure", "Treasure", ":treasure:", 0xFFFFD25A, "What plays when a burrow holds treasure.", MYTH_TYPES))),
        new Cat("slayers", "Slayers", ":slayers:", 0xFFFF5A5A, true, "Slayer bosses and their drops.", List.of()),
        new Cat("farming", "Farming", ":farm:", 0xFF7BE05A, true, "Crops, pests and the rare drops of the Garden.", List.of()),
        new Cat("experiments", "Experiments", ":experiments:", 0xFFB56BFF, true, "The Experimentation Table's rewards.", List.of()),
        new Cat("mining", "Mining", ":mining:", 0xFF9AB8FF, true, "Rare finds underground.", List.of()),
        new Cat("fishing", "Fishing", ":fishing:", 0xFF4FC3FF, true, "Sea creatures and what they carry.", List.of()),
        new Cat("dyes", "Dyes", ":dye:", 0xFFFF72C6, true, "Every dye in the game.", List.of()));

    static Cat byId(String id) { for (Cat c : ALL) if (c.id().equals(id)) return c; return ALL.get(0); }
    static Group group(String key) {
        String g = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
        for (Cat c : ALL) for (Group gr : c.groups()) if (gr.id().equals(g)) return gr;
        return ALL.get(0).groups().get(0);
    }
    static Cat catOfGroup(String groupId) { for (Cat c : ALL) for (Group g : c.groups()) if (g.id().equals(groupId)) return c; return ALL.get(0); }
    /** The category a drop source belongs to. */
    static Cat catOfSource(DropData.Source s) {
        if (s == TreasureData.SOURCE) return byId("treasure");
        return ALL.get(0);
    }

    private Categories() {}
}
