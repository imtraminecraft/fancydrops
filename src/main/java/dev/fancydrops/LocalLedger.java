package dev.fancydrops;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Your drops counted on this computer, the moment they happen, so the star map and the ledger always show them, even while the
 * server can't confirm who you are. The rules mirror tools/worn-worker.js exactly; the server stays the only judge of cosmetics,
 * so a hand-edited file lights stars but never unlocks a reward. Saved in config/fancydrops/ledger_local.json.
 */
final class LocalLedger {
    private LocalLedger() {}

    /** The counts, the same shape as the server's profile. */
    static final class P {
        double drops, rng, bigRng, value, best, r3; long first = System.currentTimeMillis();
        Map<String, Integer> uniq = new HashMap<>(), days = new HashMap<>(), vinyl = new HashMap<>(), bosses = new HashMap<>(), dyes = new HashMap<>();
        Map<String, double[]> g = new HashMap<>();
        Map<String, Long> unlocked = new HashMap<>();
    }
    private record Def(String id, String ch, String name, String desc, String icon, double goal, ToDoubleFunction<P> stat, String reward, boolean secret) {}

    static final LinkedHashMap<String, String> CH = new LinkedHashMap<>();
    static { CH.put("ledger", "The Ledger"); CH.put("fortune", "Fortune"); CH.put("myth", "Mythological"); CH.put("rare", "Rare Drops"); CH.put("farm", "Farming"); CH.put("fish", "Fishing"); CH.put("dye", "Dyes"); }

    private static double g(P p, String k) { double[] v = p.g.get(k); return v == null ? 0 : v[0]; }
    private static double gr(P p, String k) { double[] v = p.g.get(k); return v == null ? 0 : v[1]; }
    private static final String[] FISH = {"fish_water", "fish_lava", "fish_spooky", "fish_winter"}, FARM = {"crops", "feast", "bounty", "pests"};
    private static double fish(P p) { double s = 0; for (String k : FISH) s += g(p, k); return s; }
    private static double fishR(P p) { double s = 0; for (String k : FISH) s += gr(p, k); return s; }
    private static double farm(P p) { double s = 0; for (String k : FARM) s += g(p, k); return s; }

    private static final List<Def> ACH = List.of(
        new Def("first_find", "ledger", "First Find", "Get your first rare drop.", "seal", 1, p -> p.drops, "sweep", false),
        new Def("town_crier", "ledger", "Town Crier", "Get 10 rare drops.", "bell", 10, p -> p.drops, "bell", false),
        new Def("hoarder", "ledger", "Hoarder", "Get 100 rare drops.", "sack", 100, p -> p.drops, "ripple", false),
        new Def("packrat", "ledger", "Pack Rat", "Get 500 rare drops.", "sack", 500, p -> p.drops, "", false),
        new Def("collector", "ledger", "Collector", "Find 25 different items.", "shelf", 25, p -> p.uniq.size(), "", false),
        new Def("curator", "ledger", "Curator", "Find 75 different items.", "shelf", 75, p -> p.uniq.size(), "", false),
        new Def("regular", "ledger", "Regular", "Find drops on 7 different days.", "clock", 7, p -> p.days.size(), "clock", false),
        new Def("devoted", "ledger", "Devoted", "Find drops on 30 different days.", "clock", 30, p -> p.days.size(), "", false),
        new Def("rngesus", "fortune", "RNGesus Smiles", "Get your first RNG drop.", "dice", 1, p -> p.rng, "", false),
        new Def("treasure_hunter", "fortune", "Treasure Hunter", "Get 10 RNG drops worth 1M coins or more each.", "chest", 10, p -> p.bigRng, "chest", false),
        new Def("blessed", "fortune", "Blessed", "Get 50 RNG drops.", "dice", 50, p -> p.rng, "dice", false),
        new Def("chosen", "fortune", "The Chosen One", "Get 100 RNG drops.", "crown", 100, p -> p.rng, "", false),
        new Def("millionaire", "fortune", "Millionaire", "Find 10M coins worth of drops.", "coin", 10_000_000, p -> p.value, "coinaura", false),
        new Def("tycoon", "fortune", "Tycoon", "Find 1B coins worth of drops.", "coin", 1_000_000_000, p -> p.value, "", false),
        new Def("jackpot", "fortune", "Jackpot", "One drop worth 100M coins or more.", "gem", 1, p -> p.best >= 100_000_000 ? 1 : 0, "", true),
        new Def("pray", "fortune", "Answered Prayers", "Get 5 drops rated Pray RNGesus or rarer.", "star", 5, p -> p.r3, "neon", false),
        new Def("digger", "myth", "Digger", "Get 25 treasure finds from burrows.", "shovel", 25, p -> g(p, "treasure"), "", false),
        new Def("myth_hunter", "myth", "Mythic Hunter", "Get 25 drops from mythological creatures.", "horn", 25, p -> g(p, "myth"), "spark", false),
        new Def("minos_bane", "myth", "Minos' Bane", "Get 5 RNG drops from mythological creatures.", "horn", 5, p -> gr(p, "myth"), "", false),
        new Def("deep_digger", "myth", "Deep Digger", "Get 200 treasure finds.", "shovel", 200, p -> g(p, "treasure"), "", false),
        new Def("first_blood", "rare", "First Blood", "Get your first slayer rare drop.", "sword", 1, p -> g(p, "slayer"), "", false),
        new Def("executioner", "rare", "Executioner", "Get 25 slayer rare drops.", "anvil", 25, p -> g(p, "slayer"), "anvil", false),
        new Def("boss_breaker", "rare", "Boss Breaker", "Get 10 RNG slayer drops.", "skull", 10, p -> gr(p, "slayer"), "", false),
        new Def("all_bosses", "rare", "Monster Hunter", "Get a rare drop from 4 different slayer bosses.", "sword", 4, p -> p.bosses.size(), "", false),
        new Def("green_thumb", "farm", "Green Thumb", "Get 100 farming drops.", "wheat", 100, LocalLedger::farm, "tractor", false),
        new Def("exterminator", "farm", "Exterminator", "Get 50 pest drops.", "bug", 50, p -> g(p, "pests"), "fireflyaura", false),
        new Def("crate_digger", "farm", "Crate Digger", "Find 5 different Vinyls.", "disc", 5, p -> p.vinyl.size(), "disc", false),
        new Def("greenhouse", "farm", "Greenhouse Keeper", "Get 10 Harvest Bounty finds.", "leaf", 10, p -> g(p, "bounty"), "", false),
        new Def("feast", "farm", "Feast", "Get 25 Harvest Feast drops.", "wheat", 25, p -> g(p, "feast"), "", false),
        new Def("scholar", "rare", "Scholar", "Get 10 Experimentation Table rewards.", "book", 10, p -> g(p, "exp"), "", false),
        new Def("mastermind", "rare", "Mastermind", "Get 5 RNG Experimentation Table rewards.", "book", 5, p -> gr(p, "exp"), "runeaura", false),
        new Def("angler", "fish", "Angler", "Get 10 rare drops from sea creatures.", "fish", 10, LocalLedger::fish, "bubbleaura", false),
        new Def("deep_sea", "fish", "Deep Sea Legend", "Get 3 RNG drops from sea creatures.", "fish", 3, LocalLedger::fishR, "", false),
        new Def("isle_raider", "rare", "Isle Raider", "Get 10 rare drops from Crimson Isle bosses.", "flame", 10, p -> g(p, "crimson"), "", false),
        new Def("rare_breed", "rare", "Rare Breed", "Get 10 rare drops in the Dwarven Mines.", "skull", 10, p -> g(p, "mobs"), "", false),
        new Def("dragon_slayer", "rare", "Dragon Slayer", "Get 5 rare drops from Ender Dragons.", "skull", 5, p -> g(p, "dragon"), "", false),
        new Def("nucleus", "rare", "Heart of the Mountain", "Get 10 rare Crystal Nucleus rewards.", "gem", 10, p -> g(p, "nucleus"), "", false),
        new Def("living_colour", "dye", "Living Colour", "Find a dye.", "dye", 1, p -> g(p, "dyes"), "", true),
        new Def("palette", "dye", "Palette", "Find 3 different dyes.", "dye", 3, p -> p.dyes.size(), "", false),
        new Def("rainbow", "dye", "Every Colour", "Find 10 different dyes.", "dye", 10, p -> p.dyes.size(), "", false),
        new Def("sea_hunter", "fish", "Sea Hunter", "Get 100 rare drops from sea creatures.", "fish", 100, LocalLedger::fish, "", false)
    );

    private static P p; private static long version;
    private static Path file() { return DataFiles.dir().resolve("ledger_local.json"); }
    static synchronized P get() {
        if (p != null) return p;
        p = new P();
        try { if (Files.exists(file())) { P q = new com.google.gson.Gson().fromJson(Files.readString(file()), P.class); if (q != null) p = q; } } catch (Throwable ignored) { }
        if (p.g == null) p.g = new HashMap<>(); if (p.unlocked == null) p.unlocked = new HashMap<>();
        return p;
    }
    private static synchronized void save() { try { Files.writeString(file(), new com.google.gson.Gson().toJson(p)); } catch (Throwable ignored) { } }
    static long version() { return version; }

    /** Counts one drop (the same event the server gets). Returns the ids of the achievements it just earned. */
    static synchronized List<String> add(JsonObject e) {
        P p = get(); long now = System.currentTimeMillis();
        String grp = e.has("g") ? e.get("g").getAsString() : "", id = e.has("id") ? e.get("id").getAsString() : "", item = e.has("item") ? e.get("item").getAsString() : "", src = e.has("src") ? e.get("src").getAsString() : "";
        if (grp.isEmpty() || id.isEmpty()) return List.of();
        boolean rng = e.has("rng") && e.get("rng").getAsBoolean(); double v = e.has("v") ? Math.max(0, e.get("v").getAsDouble()) : 0; int r = e.has("r") ? e.get("r").getAsInt() : 0;
        p.drops++; if (rng) p.rng++; if (rng && v >= 1_000_000) p.bigRng++; if (r >= 3) p.r3++;
        p.value += v; p.best = Math.max(p.best, v);
        p.uniq.put(id, 1); p.days.put(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString(), 1);
        if (item.toLowerCase(Locale.ROOT).contains("vinyl")) p.vinyl.put(id, 1);
        if (grp.equals("dyes")) p.dyes.put(id, 1);
        if (grp.equals("slayer") && !src.isEmpty()) p.bosses.put(src, 1);
        double[] gg = p.g.computeIfAbsent(grp, k -> new double[2]); gg[0]++; if (rng) gg[1]++;
        List<String> fresh = new ArrayList<>();
        for (Def d : ACH) if (!p.unlocked.containsKey(d.id) && d.stat.applyAsDouble(p) >= d.goal) { p.unlocked.put(d.id, now); fresh.add(d.id); }
        version++; save();
        return fresh;
    }

    /** Everything counted here, as a profile (no cosmetics: only the server unlocks those). */
    static synchronized Achievements.Profile profile() {
        P p = get();
        List<Achievements.Ach> list = new ArrayList<>();
        for (Def d : ACH) { Long at = p.unlocked.get(d.id); list.add(new Achievements.Ach(d.id, d.ch, CH.get(d.ch), d.name, d.desc, d.icon, d.goal, Math.min(d.goal, d.stat.applyAsDouble(p)), at != null, at == null ? 0 : at, d.reward, d.secret)); }
        Map<String, Double> st = new HashMap<>();
        st.put("drops", p.drops); st.put("rng", p.rng); st.put("bigRng", p.bigRng); st.put("value", p.value); st.put("best", p.best); st.put("r3", p.r3);
        st.put("unique", (double) p.uniq.size()); st.put("days", (double) p.days.size()); st.put("bosses", (double) p.bosses.size()); st.put("vinyls", (double) p.vinyl.size()); st.put("dyes", (double) p.dyes.size());
        Map<String, double[]> groups = new HashMap<>(); for (var e : p.g.entrySet()) groups.put(e.getKey(), e.getValue().clone());
        return new Achievements.Profile(st, groups, list, new HashSet<>(), new LinkedHashMap<>(CH), p.first);
    }

    /** The server's word and this computer's count together: each number is the larger of the two, a star is lit if either says so. */
    static Achievements.Profile merge(Achievements.Profile server, Achievements.Profile local) {
        if (server == null) return local;
        Map<String, Double> st = new HashMap<>(server.stats()); local.stats().forEach((k, v) -> st.merge(k, v, Math::max));
        Map<String, double[]> gr = new HashMap<>(); server.groups().forEach((k, v) -> gr.put(k, v.clone()));
        local.groups().forEach((k, v) -> gr.merge(k, v, (a, b) -> new double[]{Math.max(a[0], b[0]), Math.max(a[1], b[1])}));
        Map<String, Achievements.Ach> loc = new HashMap<>(); for (Achievements.Ach a : local.list()) loc.put(a.id(), a);
        List<Achievements.Ach> list = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (Achievements.Ach a : server.list()) {
            Achievements.Ach l = loc.get(a.id()); seen.add(a.id());
            if (l == null) { list.add(a); continue; }
            boolean done = a.done() || l.done(); long at = a.done() ? a.at() : l.at();
            list.add(new Achievements.Ach(a.id(), a.ch(), a.chapter(), a.name(), a.desc(), a.icon(), a.goal(), Math.max(a.progress(), l.progress()), done, at, a.reward(), a.secret()));
        }
        for (Achievements.Ach l : local.list()) if (!seen.contains(l.id())) list.add(l);
        LinkedHashMap<String, String> ch = new LinkedHashMap<>(local.chapters()); ch.putAll(server.chapters());
        return new Achievements.Profile(st, gr, list, server.cosmetics(), ch, server.since() == 0 ? local.since() : Math.min(server.since(), local.since()));
    }
}
