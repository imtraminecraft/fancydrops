package dev.fancydrops;

import java.util.*;

/** Every cosmetic, which line it belongs to, and whether this player has it. */
final class CosmeticData {
    private CosmeticData() {}

    enum Line { BETA, SPECIAL, FREE }
    enum Kind { SUFFIX, NAME, AURA }
    record Def(String id, String name, Line line, Kind kind, String how, int color) {}

    private static final String BETA_HOW = "Only for players who downloaded before v1.0";
    static final List<Def> ALL = List.of(
        // Beta: kept by everyone who had the mod before 1.0
        new Def("crown", "Crown", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFFFC857),
        new Def("gem", "Gem", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFF55D8FF),
        new Def("flame", "Flame", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFFF8A3D),
        new Def("clover", "Lucky Clover", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFF62E6A8),
        new Def("heart", "Heartbeat", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFFF6B8A),
        new Def("bolt", "Lightning", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFFFE066),
        new Def("snow", "Snowflake", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFF9AD8FF),
        new Def("star", "Shooting Star", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFFFD36B),
        new Def("ghost", "Ghost", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFC9D2FF),
        new Def("shimmer", "Shimmer", Line.BETA, Kind.NAME, BETA_HOW, 0xFFBFD4FF),
        new Def("pulse", "Pulse", Line.BETA, Kind.NAME, BETA_HOW, 0xFFFF9AC8),
        new Def("wave", "Ripple", Line.BETA, Kind.NAME, BETA_HOW, 0xFF7FE0D0),
        new Def("glow", "Soft Glow", Line.BETA, Kind.NAME, BETA_HOW, 0xFFFFE9A8),
        // Special: handed out by hand, one by one
        new Def("sparkle", "Sparkle", Line.SPECIAL, Kind.SUFFIX, "Given to the original supporters", 0xFFB48CFF),
        new Def("royal", "Royal Gold", Line.SPECIAL, Kind.NAME, "Given to Patreon supporters", 0xFFFFC832),
        new Def("glitch", "Glitch", Line.SPECIAL, Kind.NAME, "Given to players who help with bug reports", 0xFF55FF9A),
        new Def("stardust", "Stardust", Line.SPECIAL, Kind.AURA, "Given to players who contribute to the mod", 0xFF8FD0FF),
        // Everyone: earn them yourself (all still, except the secret one)
        new Def("newcomer", "Newcomer", Line.FREE, Kind.SUFFIX, "Download the mod", 0xFF7BE07B),
        new Def("discord", "Discord", Line.FREE, Kind.SUFFIX, "Press the Discord button", 0xFF7289DA),
        new Def("firstdrop", "First Drop", Line.FREE, Kind.SUFFIX, "Get your first rare drop", 0xFFFFD700),
        new Def("collector", "Collector", Line.FREE, Kind.SUFFIX, "Get 25 rare drops", 0xFFD9A05B),
        new Def("secret", "Secret", Line.FREE, Kind.SUFFIX, "Find the hidden button", 0xFFC08BFF));

    static Def byId(String id) { for (Def d : ALL) if (d.id().equals(id)) return d; return null; }
    static List<Def> line(Line l) { List<Def> o = new ArrayList<>(); for (Def d : ALL) if (d.line() == l) o.add(d); return o; }

    // ---- testing aids (the small test menu under the main menu)
    static boolean testNewPlayer, testUnlockAll;

    // ---- Special cosmetics: who has which. cosmetics.json lists the players of each one separately.
    /** The names used in cosmetics.json, and the cosmetic each one gives. */
    static final Map<String, String> GRANT_KEY = Map.of("sparkle", "sparkle", "patreon", "royal", "bughunter", "glitch", "contributor", "stardust");
    static volatile Map<String, Set<String>> grants = new HashMap<>();
    static Set<String> grantsOf(UUID u) { Set<String> s = grants.get(Cosmetics.code(u)); return s == null ? Set.of() : s; }

    // ---- Beta
    static boolean betaEligible() {
        if (testNewPlayer) return false;
        if (testUnlockAll) return true;
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

    static boolean unlocked(Def d) {
        if (d == null) return false;
        if (testUnlockAll) return true;
        FancyConfig c = FancyConfig.get();
        switch (d.line()) {
            case BETA: return betaEligible();
            case SPECIAL: return !testNewPlayer && grantsOf(Cosmetics.me()).contains(d.id());
            default:
                if (d.id().equals("newcomer")) return true;
                if (testNewPlayer) return false;
                return switch (d.id()) {
                    case "discord" -> c.discordJoined;
                    case "secret" -> c.secretFound;
                    case "firstdrop" -> c.dropsSeen >= 1;
                    case "collector" -> c.dropsSeen >= 25;
                    default -> false;
                };
        }
    }
    static boolean unlockedId(String id) { return unlocked(byId(id)); }

    static String equipped(Kind k) { FancyConfig c = FancyConfig.get(); return k == Kind.SUFFIX ? c.suffix : k == Kind.NAME ? c.nameFx : c.aura; }
    static boolean isEquipped(Def d) { return d.id().equals(equipped(d.kind())); }
    /** Equip it, or take it off again when it is already on. */
    static void toggle(Def d) {
        FancyConfig c = FancyConfig.get(); String v = isEquipped(d) ? "" : d.id();
        switch (d.kind()) { case SUFFIX -> c.suffix = v; case NAME -> c.nameFx = v; default -> c.aura = v; }
        FancyConfig.save();
    }
    static String kindLabel(Kind k) { return k == Kind.SUFFIX ? "Suffix" : k == Kind.NAME ? "Name effect" : "Aura"; }
    static int unlockedCount(Line l) { int n = 0; for (Def d : line(l)) if (unlocked(d)) n++; return n; }
}
