package dev.fancydrops;

import java.util.*;

/** Every cosmetic, which line it belongs to, and whether this player has it. */
final class CosmeticData {
    private CosmeticData() {}

    enum Line { BETA, RARE, SPECIAL, FREE }
    enum Kind { SUFFIX, NAME, AURA }
    record Def(String id, String name, Line line, Kind kind, String how, int color, String set) {
        Def(String id, String name, Line line, Kind kind, String how, int color) { this(id, name, line, kind, how, color, null); }
    }
    /** A Special set: a suffix, a name effect and an aura that belong together. */
    record SetDef(String id, String name, String how, int color, String suffix, String fx, String aura, int rarity) {}
    static final List<SetDef> SETS = List.of(
        new SetDef("sparkle", "Special", "Only given to special people.", 0xFFB48CFF, "sparkle", "sparklefx", "sparkleaura", 3),
        new SetDef("bugfinder", "Bug Finder", "Only given to bug finders who reported bugs that could break the whole mod.", 0xFFFF5A36, "glitchsfx", "glitch", "glitchaura", 3),
        new SetDef("contributors", "Contributors", "Only given to the mod contributors.", 0xFF55D8FF, "contrib", "contribfx", "stardust", 3),
        new SetDef("patreon", "Patreon", "Only given to Patreon supporters.", 0xFFFFC832, "patron", "royal", "patronaura", 3),
        new SetDef("beta", "Beta", "Only given to players who downloaded the mod before v1.0.", 0xFF3FD16A, "flame", "pulse", null, 2),
        new SetDef("common", "Common", "Everyone gets this set.", 0xFF9AA3B5, "newcomer", "fresh", null, 0));
    static SetDef setById(String id) { for (SetDef d : SETS) if (d.id().equals(id)) return d; return null; }

    private static final String BETA_HOW = "Downloaded the mod before v1.0", RARE_HOW = "Rare: given to the first players of the mod";
    static final List<Def> ALL = List.of(
        // Beta: kept by everyone who had the mod before 1.0
        new Def("crown", "Crown", Line.RARE, Kind.SUFFIX, RARE_HOW, 0xFFFFC857),
        new Def("gem", "Gem", Line.RARE, Kind.SUFFIX, RARE_HOW, 0xFF55D8FF),
        new Def("flame", "Flame", Line.BETA, Kind.SUFFIX, BETA_HOW, 0xFFFF8A3D, "beta"),
        new Def("clover", "Clover", Line.RARE, Kind.SUFFIX, RARE_HOW, 0xFF62E6A8),
        new Def("heart", "Heart", Line.RARE, Kind.SUFFIX, RARE_HOW, 0xFFFF6B8A),
        new Def("bolt", "Bolt", Line.RARE, Kind.SUFFIX, RARE_HOW, 0xFFFFE066),
        new Def("snow", "Snow", Line.RARE, Kind.SUFFIX, RARE_HOW, 0xFF9AD8FF),
        new Def("star", "Star", Line.RARE, Kind.SUFFIX, RARE_HOW, 0xFFFFD36B),
        new Def("ghost", "Ghost", Line.RARE, Kind.SUFFIX, RARE_HOW, 0xFFC9D2FF),
        new Def("shimmer", "Shimmer", Line.RARE, Kind.NAME, RARE_HOW, 0xFFBFD4FF),
        new Def("pulse", "Pulse", Line.BETA, Kind.NAME, BETA_HOW, 0xFFFF9AC8, "beta"),
        new Def("wave", "Ripple", Line.RARE, Kind.NAME, RARE_HOW, 0xFF7FE0D0),
        new Def("glow", "Glow", Line.RARE, Kind.NAME, RARE_HOW, 0xFFFFE9A8),
        // Special: handed out by hand, one set at a time (suffix + name effect + aura)
        new Def("sparkle", "Sparkle", Line.SPECIAL, Kind.SUFFIX, "Special Set", 0xFFB48CFF, "sparkle"),
        new Def("sparklefx", "Starlight", Line.SPECIAL, Kind.NAME, "Special Set", 0xFFFFE9A8, "sparkle"),
        new Def("sparkleaura", "Twinkle", Line.SPECIAL, Kind.AURA, "Special Set", 0xFFFFE680, "sparkle"),
        new Def("glitchsfx", "Hazard", Line.SPECIAL, Kind.SUFFIX, "Bug Finder Set", 0xFFFFB02E, "bugfinder"),
        new Def("glitch", "Critical", Line.SPECIAL, Kind.NAME, "Bug Finder Set", 0xFFFF5A36, "bugfinder"),
        new Def("glitchaura", "Alarm", Line.SPECIAL, Kind.AURA, "Bug Finder Set", 0xFFFF7A2E, "bugfinder"),
        new Def("contrib", "Builder", Line.SPECIAL, Kind.SUFFIX, "Contributors Set", 0xFF5AFFC8, "contributors"),
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
    private static boolean unlockedNow(Def d) {
        if (d == null) return false;
        Boolean f = force.get(d.id()); if (f != null) return f;
        if (Cosmetics.isAdmin() || grantsOf(Cosmetics.me()).contains("c:" + d.id())) return true;           // the owner has everything; single cosmetics can be handed out one by one
        FancyConfig c = FancyConfig.get();
        switch (d.line()) {
            case BETA: case RARE: return betaEligible();
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
        if (d.line() == Line.RARE) return "A rare cosmetic from the early days. Only players who downloaded before v1.0 have it.";
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
    static int rarityOf(Def d) { return d.line() == Line.SPECIAL ? 3 : d.line() == Line.BETA ? 2 : d.line() == Line.RARE ? 1 : 0; }
    static String rarityName(int r) { return r == 3 ? "Special" : r == 2 ? "Beta" : r == 1 ? "Rare" : "Common"; }
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
        c.suffix = on ? "" : s.suffix(); c.nameFx = on ? "" : s.fx(); c.aura = on || s.aura() == null ? "" : s.aura();       // a set replaces what you wore: a set with no aura also turns the old aura off
        FancyConfig.save(); changed();
    }
    static boolean wearingSet(SetDef s) { FancyConfig c = FancyConfig.get(); return s.suffix().equals(c.suffix) && s.fx().equals(c.nameFx) && (s.aura() == null || s.aura().equals(c.aura)); }
    /** Something you wear changed: chat lines already on screen are redone with the new look. */
    static void changed() { Cosmetics.historyDirty = true; UCACHE.clear(); }
    static String kindLabel(Kind k) { return k == Kind.SUFFIX ? "Suffix" : k == Kind.NAME ? "Name effect" : "Aura"; }

    // ---- Admin Panel helpers
    /** How many players hold each set, and each single cosmetic, according to cosmetics.json. */
    static Map<String, Integer> counts() {
        Map<String, Integer> m = new LinkedHashMap<>();
        Set<String> players = new LinkedHashSet<>(grants.keySet()); players.add(Cosmetics.OWNER);                 // the owner always holds everything
        for (String p : players) {
            Set<String> held = heldBy(p);
            for (SetDef sd : SETS) { List<Def> mem = members(sd); boolean all = !mem.isEmpty(); for (Def d : mem) if (!held.contains(d.id())) all = false; if (all) m.merge(sd.id(), 1, Integer::sum); }
            for (String id : held) m.merge("c:" + id, 1, Integer::sum);
        }
        return m;
    }
    /** What a player is given right now, as a list of cosmetic ids (sets expanded). */
    static Set<String> heldBy(String code) {
        Set<String> o = new LinkedHashSet<>(); Set<String> g = grants.getOrDefault(code, Set.of());
        if (code.equals(Cosmetics.OWNER)) { for (Def d : ALL) o.add(d.id()); return o; }                              // the owner has everything
        for (String x : g) {
            if (x.startsWith("c:")) o.add(x.substring(2));
            else { SetDef sd = setById(x); if (sd != null) for (Def d : members(sd)) o.add(d.id()); }
        }
        return o;
    }
    /** What the Admin Panel shows ticked: exactly what cosmetics.json gives this player (the owner, who can wear everything anyway, starts with everything ticked until something is saved for them). */
    static Set<String> pickedFor(String code) {
        Set<String> o = new LinkedHashSet<>(); Set<String> g = grants.getOrDefault(code, Set.of());
        for (String x : g) {
            if (x.startsWith("c:")) o.add(x.substring(2));
            else { SetDef sd = setById(x); if (sd != null) for (Def d : members(sd)) o.add(d.id()); }
        }
        if (code.equals(Cosmetics.OWNER) && (remoteRawHasOwner() ? false : true)) return heldBy(code);
        return o;
    }
    private static boolean remoteRawHasOwner() { String r = Cosmetics.remoteRaw; return r != null && r.contains(Cosmetics.OWNER); }
    /** cosmetics.json with this player's gifts replaced by the picks: whole Special sets go in their list, anything else under "extra". */
    static String buildJson(String code, Set<String> picks) {
        com.google.gson.JsonObject root;
        try { root = com.google.gson.JsonParser.parseString(Cosmetics.remoteRaw == null ? "{}" : Cosmetics.remoteRaw).getAsJsonObject(); } catch (Throwable t) { root = new com.google.gson.JsonObject(); }
        com.google.gson.JsonObject special = root.has("special") ? root.getAsJsonObject("special") : new com.google.gson.JsonObject(); root.add("special", special);
        com.google.gson.JsonObject extra = root.has("extra") ? root.getAsJsonObject("extra") : new com.google.gson.JsonObject();
        Set<String> left = new LinkedHashSet<>(picks);
        for (var e : GRANT_KEY.entrySet()) {
            com.google.gson.JsonArray old = special.has(e.getKey()) ? special.getAsJsonArray(e.getKey()) : new com.google.gson.JsonArray(), neu = new com.google.gson.JsonArray();
            for (var x : old) if (!x.getAsString().equalsIgnoreCase(code)) neu.add(x);
            SetDef sd = setById(e.getValue());
            if (sd != null) { List<Def> mem = members(sd); boolean all = !mem.isEmpty(); for (Def d : mem) if (!picks.contains(d.id())) all = false; if (all) { neu.add(code); for (Def d : mem) left.remove(d.id()); } }
            special.add(e.getKey(), neu);
        }
        extra.remove(code);
        if (!left.isEmpty()) { com.google.gson.JsonArray a = new com.google.gson.JsonArray(); for (String id : left) a.add(id); extra.add(code, a); }
        if (extra.size() > 0) root.add("extra", extra); else root.remove("extra");
        return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n";
    }
}
