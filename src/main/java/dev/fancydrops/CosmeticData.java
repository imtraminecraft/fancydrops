package dev.fancydrops;

import java.util.*;

/** Every cosmetic, which line it belongs to, and whether this player has it. */
final class CosmeticData {
    private CosmeticData() {}

    enum Line { BETA, SPECIAL, FREE }
    enum Kind { SUFFIX, NAME, AURA }
    record Def(String id, String name, Line line, Kind kind, String how, int color, String set) {
        Def(String id, String name, Line line, Kind kind, String how, int color) { this(id, name, line, kind, how, color, null); }
    }
    /** A Special set: a suffix, a name effect and an aura that belong together. */
    record SetDef(String id, String name, String how, int color, String suffix, String fx, String aura, int rarity) {}
    static final List<SetDef> SETS = List.of(
        new SetDef("sparkle", "Special", "Only given to special people.", 0xFFB48CFF, "sparkle", "sparklefx", "sparkleaura", 2),
        new SetDef("bugfinder", "Bug Finder", "Only given to bug finders who reported bugs that could break the whole mod.", 0xFFFF5A36, "glitchsfx", "glitch", "glitchaura", 2),
        new SetDef("contributors", "Contributors", "Only given to the mod contributors.", 0xFF55D8FF, "contrib", "contribfx", "stardust", 2),
        new SetDef("patreon", "Patreon", "Only given to Patreon supporters.", 0xFFFFC832, "patron", "royal", "patronaura", 2),
        new SetDef("beta", "Beta", "Only given to players who downloaded the mod before v1.0.", 0xFF3FD16A, "crown", "shimmer", null, 1),
        new SetDef("common", "Common", "Everyone gets this set.", 0xFF9AA3B5, "newcomer", "fresh", null, 0));
    static SetDef setById(String id) { for (SetDef d : SETS) if (d.id().equals(id)) return d; return null; }

    private static final String BETA_HOW = "Downloaded the mod before v1.0";
    static final List<Def> ALL = List.of(
        // Beta: kept by everyone who had the mod before 1.0
        new Def("crown", "Crown", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFFFC857, "beta"),
        new Def("gem", "Gem", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFF55D8FF),
        new Def("flame", "Flame", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFFF8A3D),
        new Def("clover", "Clover", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFF62E6A8),
        new Def("heart", "Heart", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFFF6B8A),
        new Def("bolt", "Bolt", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFFFE066),
        new Def("snow", "Snow", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFF9AD8FF),
        new Def("star", "Star", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFFFD36B),
        new Def("ghost", "Ghost", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFC9D2FF),
        new Def("shimmer", "Shimmer", Line.BETA, Kind.NAME, BETA_HOW, 0xFFBFD4FF, "beta"),
        new Def("pulse", "Pulse", Line.BETA, Kind.NAME, BETA_HOW, 0xFFFF9AC8),
        new Def("wave", "Ripple", Line.BETA, Kind.NAME, BETA_HOW, 0xFF7FE0D0),
        new Def("glow", "Glow", Line.BETA, Kind.NAME, BETA_HOW, 0xFFFFE9A8),
        // Special: handed out by hand, one set at a time (suffix + name effect + aura)
        new Def("sparkle", "Sparkle", Line.SPECIAL, Kind.SUFFIX, "Special Set", 0xFFB48CFF, "sparkle"),
        new Def("sparklefx", "Starlight", Line.SPECIAL, Kind.NAME, "Special Set", 0xFFFFE9A8, "sparkle"),
        new Def("sparkleaura", "Twinkle", Line.SPECIAL, Kind.AURA, "Special Set", 0xFFFFE680, "sparkle"),
        new Def("glitchsfx", "Hazard", Line.SPECIAL, Kind.SUFFIX, "Bug Finder Set", 0xFFFFB02E, "bugfinder"),
        new Def("glitch", "Critical", Line.SPECIAL, Kind.NAME, "Bug Finder Set", 0xFFFF5A36, "bugfinder"),
        new Def("glitchaura", "Alarm", Line.SPECIAL, Kind.AURA, "Bug Finder Set", 0xFFFF7A2E, "bugfinder"),
        new Def("contrib", "Contrib", Line.SPECIAL, Kind.SUFFIX, "Contributors Set", 0xFF5AFFC8, "contributors"),
        new Def("contribfx", "Aqua Flow", Line.SPECIAL, Kind.NAME, "Contributors Set", 0xFF55D8FF, "contributors"),
        new Def("stardust", "Stardust", Line.SPECIAL, Kind.AURA, "Contributors Set", 0xFF8FD0FF, "contributors"),
        new Def("patron", "Patron", Line.SPECIAL, Kind.SUFFIX, "Patreon Set", 0xFFFFC832, "patreon"),
        new Def("royal", "Royal", Line.SPECIAL, Kind.NAME, "Patreon Set", 0xFFFFC832, "patreon"),
        new Def("patronaura", "Embers", Line.SPECIAL, Kind.AURA, "Patreon Set", 0xFFFF9B2E, "patreon"),
        // Everyone: earn them yourself (all still, except the secret one)
        new Def("newcomer", "Newcomer", Line.FREE, Kind.SUFFIX, "Downloaded the mod", 0xFF7BE07B, "common"),
        new Def("fresh", "Fresh", Line.FREE, Kind.NAME, "Downloaded the mod", 0xFF9BE8A8, "common"),
        new Def("discord", "Discord", Line.FREE, Kind.SUFFIX, "Pressed the Discord button", 0xFF7289DA),
        new Def("firstdrop", "1st Drop", Line.FREE, Kind.SUFFIX, "Got a first rare drop", 0xFFFFD700),
        new Def("collector", "Collector", Line.FREE, Kind.SUFFIX, "Got 25 rare drops", 0xFFD9A05B),
        new Def("hoarder", "Hoarder", Line.FREE, Kind.SUFFIX, "Got 100 rare drops", 0xFFB0763C),
        new Def("legend", "Legend", Line.FREE, Kind.SUFFIX, "Got 500 rare drops", 0xFFFFCD32),
        new Def("secret", "Secret", Line.FREE, Kind.SUFFIX, "Found the hidden button", 0xFFC08BFF));

    static Def byId(String id) { for (Def d : ALL) if (d.id().equals(id)) return d; return null; }
    static List<Def> line(Line l) { List<Def> o = new ArrayList<>(); for (Def d : ALL) if (d.line() == l) o.add(d); return o; }

    // ---- testing aids (the small test menu under the main menu): add or remove single cosmetics on yourself. Never touches the real progress.
    static boolean editMode;
    static final Map<String, Boolean> force = new HashMap<>();

    // ---- Special cosmetics: who has which. cosmetics.json lists the players of each one separately.
    /** The names used in cosmetics.json, and the cosmetic each one gives. */
    static final Map<String, String> GRANT_KEY = Map.of("sparkle", "sparkle", "patreon", "patreon", "bughunter", "bugfinder", "contributor", "contributors");
    static volatile Map<String, Set<String>> grants = new HashMap<>();
    static Set<String> grantsOf(UUID u) { Set<String> s = grants.get(Cosmetics.code(u)); return s == null ? Set.of() : s; }

    // ---- Beta
    static boolean betaEligible() {
        return versionLess(FancyConfig.get().firstVersion, "1.0.0");
    }
    static boolean versionLess(String a, String b) {
        String[] x = (a == null ? "" : a).split("[^0-9]+"), y = b.split("[^0-9]+");
        for (int i = 0; i < 3; i++) {
            int p = i < x.length && !x[i].isEmpty() ? Integer.parseInt(x[i]) : 0, q = i < y.length && !y[i].isEmpty() ? Integer.parseInt(y[i]) : 0;
            if (p != q) return p < q;
        }
        return false;
    }

    private static final Map<String, Boolean> UCACHE = new HashMap<>(); private static long ucacheAt;
    /** The menu asks this hundreds of times a frame; the answer is remembered for a fifth of a second. */
    static boolean unlocked(Def d) {
        if (d == null) return false;
        long n = System.currentTimeMillis();
        if (n - ucacheAt > 200) { UCACHE.clear(); ucacheAt = n; }
        Boolean c = UCACHE.get(d.id()); if (c != null) return c;
        boolean v = unlockedNow(d); UCACHE.put(d.id(), v); return v;
    }
    static void forget() { UCACHE.clear(); }
    private static boolean unlockedNow(Def d) {
        if (d == null) return false;
        Boolean f = force.get(d.id()); if (f != null) return f;
        FancyConfig c = FancyConfig.get();
        switch (d.line()) {
            case BETA: return betaEligible();
            case SPECIAL: return grantsOf(Cosmetics.me()).contains(d.set());
            default:
                if (d.id().equals("newcomer") || d.id().equals("fresh")) return true;
                return switch (d.id()) {
                    case "discord" -> c.discordJoined;
                    case "secret" -> c.secretFound;
                    case "firstdrop" -> c.dropsSeen >= 1;
                    case "collector" -> c.dropsSeen >= 25;
                    case "hoarder" -> c.dropsSeen >= 100;
                    case "legend" -> c.dropsSeen >= 500;
                    default -> false;
                };
        }
    }
    /** What to tell a player who presses a locked cosmetic. */
    static String lockText(Def d) {
        if (d.line() == Line.BETA) return "The beta is over. Only players who downloaded before v1.0 have it.";
        if (d.line() == Line.SPECIAL) return setById(d.set()).how();
        return switch (d.id()) {
            case "discord" -> "Press the Discord button in the menu.";
            case "firstdrop" -> "Get your first rare drop.";
            case "collector" -> "Get 25 rare drops.";
            case "hoarder" -> "Get 100 rare drops.";
            case "legend" -> "Get 500 rare drops.";
            default -> "Find the hidden button.";
        };
    }
    static int rarityOf(Def d) { return d.line() == Line.SPECIAL ? 2 : d.line() == Line.BETA ? 1 : 0; }
    static String rarityName(int r) { return r == 2 ? "Special" : r == 1 ? "Beta" : "Common"; }
    static List<Def> members(SetDef s) { List<Def> o = new ArrayList<>(); for (Def d : ALL) if (s.id().equals(d.set())) o.add(d); return o; }
    static int owned(SetDef s) { int n = 0; for (Def d : members(s)) if (unlocked(d)) n++; return n; }
    static boolean unlockedId(String id) { return unlocked(byId(id)); }

    static String equipped(Kind k) { FancyConfig c = FancyConfig.get(); return k == Kind.SUFFIX ? c.suffix : k == Kind.NAME ? c.nameFx : c.aura; }
    static boolean isEquipped(Def d) { return d.id().equals(equipped(d.kind())); }
    /** Equip it, or take it off again when it is already on. */
    static void toggle(Def d) {
        FancyConfig c = FancyConfig.get(); String v = isEquipped(d) ? "" : d.id();
        switch (d.kind()) { case SUFFIX -> c.suffix = v; case NAME -> c.nameFx = v; default -> c.aura = v; }
        FancyConfig.save(); changed();
    }
    /** Wear a whole set, or take all three off when you already wear it. */
    static void toggleSet(SetDef s) {
        FancyConfig c = FancyConfig.get();
        boolean on = wearingSet(s);
        c.suffix = on ? "" : s.suffix(); c.nameFx = on ? "" : s.fx(); if (s.aura() != null) c.aura = on ? "" : s.aura();
        FancyConfig.save(); changed();
    }
    static boolean wearingSet(SetDef s) { FancyConfig c = FancyConfig.get(); return s.suffix().equals(c.suffix) && s.fx().equals(c.nameFx) && (s.aura() == null || s.aura().equals(c.aura)); }
    /** Something you wear changed: chat lines already on screen are redone with the new look. */
    static void changed() { Cosmetics.historyDirty = true; UCACHE.clear(); }
    static String kindLabel(Kind k) { return k == Kind.SUFFIX ? "Suffix" : k == Kind.NAME ? "Name effect" : "Aura"; }
    static int unlockedCount(Line l) { int n = 0; for (Def d : line(l)) if (unlocked(d)) n++; return n; }
}
