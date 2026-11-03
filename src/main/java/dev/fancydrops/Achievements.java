package dev.fancydrops;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Achievements and the cosmetics they unlock. Nothing here is decided on this computer: the server (tools/worn-worker.js)
 * keeps every player's drop counts, works out which achievements are earned and which cosmetics that unlocks.
 * The mod only reports real drops (signed with the player's Minecraft login) and shows what the server says.
 */
final class Achievements {
    private Achievements() {}

    /** One achievement as the server describes it. */
    record Ach(String id, String ch, String chapter, String name, String desc, String icon, double goal, double progress, boolean done, long at, String reward, boolean secret) {}
    /** Everything the server knows about you. */
    record Profile(Map<String, Double> stats, Map<String, double[]> groups, List<Ach> list, Set<String> cosmetics, LinkedHashMap<String, String> chapters, long since) {}

    private static volatile Profile profile;
    private static volatile long fetchedAt, failedAt; private static volatile boolean fetching, posting;
    static volatile String status = "Connecting to the ledger...";
    private static final List<JsonObject> PENDING = new ArrayList<>();
    private static long firstPending;

    static Profile profile() { refresh(false); return profile; }
    static boolean ready() { return profile != null; }
    /** True when the server says you have unlocked this cosmetic through an achievement. */
    static boolean hasCosmetic(String id) { Profile p = profile; return p != null && p.cosmetics().contains(id); }
    /** The achievement that unlocks a cosmetic, or null. */
    static Ach unlocking(String cosmetic) { Profile p = profile; if (p == null) return null; for (Ach a : p.list()) if (cosmetic.equals(a.reward())) return a; return null; }

    private static String base() { return Cosmetics.wornUrl; }

    /** Reads your profile from the server (at most once a minute unless forced). */
    static void refresh(boolean force) {
        String b = base(); UUID me = Cosmetics.me(); long now = System.currentTimeMillis();
        if (b == null || me == null || fetching) return;
        if (!force && (now - fetchedAt < 60_000L || now - failedAt < 20_000L)) return;
        fetching = true; fetchedAt = now;
        CompletableFuture.runAsync(() -> {
            try {
                var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(b + "/profile/" + Cosmetics.code(me))).header("User-Agent", "FancyDrops").GET().build(), HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) { status = "The ledger server answered " + res.statusCode() + "."; failedAt = System.currentTimeMillis(); return; }
                profile = parse(JsonParser.parseString(res.body()).getAsJsonObject()); status = ""; CosmeticData.changed();
            } catch (Throwable t) { status = "Can't reach the ledger right now."; failedAt = System.currentTimeMillis(); }
            finally { fetching = false; }
        });
    }

    static Profile parse(JsonObject o) {
        Map<String, Double> stats = new HashMap<>(); Map<String, double[]> groups = new HashMap<>(); long since = 0;
        if (o.has("stats")) {
            JsonObject s = o.getAsJsonObject("stats");
            for (var e : s.entrySet()) if (e.getValue().isJsonPrimitive()) stats.put(e.getKey(), e.getValue().getAsDouble());
            if (s.has("groups")) for (var e : s.getAsJsonObject("groups").entrySet()) { JsonObject gg = e.getValue().getAsJsonObject(); groups.put(e.getKey(), new double[]{gg.has("d") ? gg.get("d").getAsDouble() : 0, gg.has("rng") ? gg.get("rng").getAsDouble() : 0}); }
            if (s.has("since")) since = s.get("since").getAsLong();
        }
        List<Ach> list = new ArrayList<>();
        for (var e : o.getAsJsonArray("achievements")) {
            JsonObject a = e.getAsJsonObject();
            list.add(new Ach(str(a, "id"), str(a, "ch"), str(a, "chapter"), str(a, "name"), str(a, "desc"), str(a, "icon"), num(a, "goal"), num(a, "progress"), a.has("done") && a.get("done").getAsBoolean(), (long) num(a, "at"), str(a, "reward"), a.has("secret") && a.get("secret").getAsBoolean()));
        }
        Set<String> cos = new HashSet<>(); if (o.has("cosmetics")) for (var e : o.getAsJsonArray("cosmetics")) cos.add(e.getAsString());
        LinkedHashMap<String, String> ch = new LinkedHashMap<>(); if (o.has("chapters")) for (var e : o.getAsJsonObject("chapters").entrySet()) ch.put(e.getKey(), e.getValue().getAsString());
        return new Profile(stats, groups, list, cos, ch, since);
    }
    private static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
    private static double num(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0; }

    // ---------------------------------------------------------------- reporting drops
    /** A real drop happened (never a simulated one): it is sent to the server a few seconds later, when its price is known. */
    static void report(AnimationManager.Item it, String group, boolean rng) {
        if (it == null || it.sim || base() == null) return;
        JsonObject e = new JsonObject();
        e.addProperty("g", group); e.addProperty("src", it.source == null ? "" : it.source.id); e.addProperty("id", it.id == null ? it.name : it.id);
        e.addProperty("item", it.name); e.addProperty("r", it.rank); e.addProperty("rng", rng);
        synchronized (PENDING) { if (PENDING.isEmpty()) firstPending = System.currentTimeMillis(); PENDING.add(e); PRICED.put(e, it); }
    }
    private static final Map<JsonObject, AnimationManager.Item> PRICED = new IdentityHashMap<>();

    /** Every tick: sends what has waited 6 seconds (prices have arrived by then), signed with your Minecraft login. */
    static void tick(Minecraft mc) {
        refresh(false);
        List<JsonObject> batch;
        synchronized (PENDING) {
            if (PENDING.isEmpty() || posting || System.currentTimeMillis() - firstPending < 6000L) return;
            batch = new ArrayList<>(PENDING); PENDING.clear();
            for (JsonObject e : batch) { AnimationManager.Item it = PRICED.remove(e); double v = it == null || it.price < 0 ? 0 : it.price * Math.max(1, it.amount); e.addProperty("v", Math.round(v)); }
        }
        String b = base(); UUID me = Cosmetics.me(); if (b == null || me == null) return;
        String name = mc.getUser().getName(), token = mc.getUser().getAccessToken();
        posting = true;
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject o = signed(mc, me, name, token);
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray(); for (JsonObject e : batch) arr.add(e); o.add("events", arr);
                var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(b + "/event")).header("User-Agent", "FancyDrops").header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(o.toString())).build(), HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) { System.err.println("[FancyDrops] ledger: " + res.statusCode() + " " + res.body()); return; }
                JsonObject r = JsonParser.parseString(res.body()).getAsJsonObject();
                Profile before = profile; profile = parse(r); fetchedAt = System.currentTimeMillis(); CosmeticData.changed();
                if (r.has("unlocked")) for (var u : r.getAsJsonArray("unlocked")) announce(u.getAsString());
            } catch (Throwable t) { System.err.println("[FancyDrops] ledger: could not send drops: " + t); }
            finally { posting = false; }
        });
    }

    /** Your Minecraft login proves to Mojang (and so to the server) that a request comes from you. */
    static JsonObject signed(Minecraft mc, UUID me, String name, String token) throws Exception {
        byte[] rb = new byte[20]; new java.security.SecureRandom().nextBytes(rb); StringBuilder sid = new StringBuilder(); for (byte x : rb) sid.append(String.format("%02x", x));
        mc.services().sessionService().joinServer(me, token, sid.toString());
        JsonObject o = new JsonObject(); o.addProperty("name", name); o.addProperty("serverId", sid.toString());
        return o;
    }

    /** "Achievement unlocked" in chat and as a notice in the menu. */
    private static void announce(String id) {
        Profile p = profile; if (p == null) return;
        for (Ach a : p.list()) if (a.id().equals(id)) {
            Minecraft.getInstance().execute(() -> {
                var mc = Minecraft.getInstance(); if (mc.player == null) return;
                var msg = Chat.prefix().append(" ").append(Chat.color("Achievement unlocked: ", Chat.TEXT)).append(Chat.color(a.name(), Chat.NUMBER).withStyle(s -> s.withBold(true)));
                if (!a.reward().isEmpty()) { CosmeticData.Def d = CosmeticData.byId(a.reward()); if (d != null) msg.append(Chat.color("  ·  unlocks " + d.name(), Chat.DIM)); }
                mc.player.sendSystemMessage(msg);
                SoundFx.achievement();
            });
        }
    }
}
