package dev.fancydrops;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * Little name suffixes for chosen players (owners get an animated picture after the name in chat and the tab list).
 * The player is identified by UUID: we look their current name up in the tab list (or use your own account), then add the
 * emoji after that name in incoming chat lines. It only changes how YOU see the message; nobody else sees it.
 * The emoji is drawn with a font of our own (assets/fancydrops/font/emoji.json), because Minecraft's default font has no emoji.
 */
final class Cosmetics {
    /** A pre-made animated suffix: 6 glyphs of our emoji font, one per frame. Chat and the tab list show it as the same animated picture. */
    record Suffix(String id, String name, String since, String[] frames) {}
    private static String[] fr(int base) { String[] f = new String[6]; for (int i = 0; i < 6; i++) f[i] = String.valueOf((char) (base + i)); return f; }
    private static String[] one(int c) { return new String[]{String.valueOf((char) c)}; }
    /** What one player shows: a suffix, a name effect and an aura (any of them may be none). */
    record Look(Suffix suffix, String fx, String aura) {}
    static final Suffix[] SUFFIXES = {
        new Suffix("sparkle", "Sparkle", "Original", fr(0xE020)),
        new Suffix("bug", "Bug", "Name Icon", fr(0xE100)),
        new Suffix("hammer", "Hammer", "Name Icon", fr(0xE110)),
        new Suffix("patron", "Patreon", "Name Icon", fr(0xE120)),
        new Suffix("minos", "King Minos", "Name Icon", fr(0xE130)),
        new Suffix("griffin", "Griffin", "Name Icon", fr(0xE140)),
        new Suffix("dice", "Lucky Dice", "Name Icon", fr(0xE150)),
        new Suffix("coin", "Gold Coin", "Name Icon", fr(0xE160)),
        new Suffix("minotaur", "Minotaur", "Name Icon", fr(0xE170)),
        new Suffix("sphinx", "Sphinx", "Name Icon", fr(0xE180)),
        new Suffix("harpy", "Harpy", "Name Icon", fr(0xE190)),
        new Suffix("chest", "Treasure Chest", "Name Icon", fr(0xE1A0)),
        new Suffix("anvil", "Anvil", "Name Icon", fr(0xE1B0)),
        new Suffix("disc", "Music Disc", "Name Icon", fr(0xE1C0)),
        new Suffix("shovel", "Shovel", "Name Icon", fr(0xE1D0)),
        new Suffix("wheat", "Wheat", "Name Icon", fr(0xE1E0)),
        new Suffix("pickaxe", "Pickaxe", "Name Icon", fr(0xE1F0)),
        new Suffix("rod", "Fishing Rod", "Name Icon", fr(0xE200)),
        new Suffix("dye", "Dye Drop", "Name Icon", fr(0xE210)),
        new Suffix("book", "Book", "Name Icon", fr(0xE220)),
        new Suffix("tractor", "Tractor", "Name Icon", fr(0xE230)),
        new Suffix("bell", "Bell", "Name Icon", fr(0xE240)),
        new Suffix("clock", "Clock", "Name Icon", fr(0xE250))};
    /** Who owns cosmetics. Everyone with the mod sees their suffix; only they can open the Cosmetics page. */
    /** Owners are stored as SHA-256 codes of their UUID, never as the UUID itself. This one is built in; more come from cosmetics.json. */
    static final String OWNER = "7ad5d2b31b239f2ed148d1de18f8214967bf7d1a411c838d466e9829ab7c2bf8";
    static final String ADMIN2 = "b7d2115142cf5666872280381967453bb85505fb1ce2270e13225355898d38fa";
    private static final Map<UUID, String> CODES = new HashMap<>();
    static String code(UUID u) { return u == null ? "" : CODES.computeIfAbsent(u, k -> { try { var md = java.security.MessageDigest.getInstance("SHA-256"); StringBuilder b = new StringBuilder(); for (byte x : md.digest(k.toString().toLowerCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8))) b.append(String.format("%02x", x)); return b.toString(); } catch (Exception e) { return ""; } }); }
    private static final Map<UUID, Component> TAB_BASE = new HashMap<>();
    private static final Map<UUID, Component> TAB_SET = new HashMap<>();       // the name we put in the tab list for each player (compared by identity: if it changed, the server rewrote it)
    private static final Map<UUID, String> TAB_ID = new HashMap<>();
    /** What the owners picked, read from cosmetics.json in the GitHub repo so everybody with the mod sees the same thing. */
    private static final String REMOTE_URL = "https://raw.githubusercontent.com/imtraminecraft/fancydrops/main/cosmetics.json";
    private static final Map<String, Set<String>> BUILTIN = Map.of(OWNER, Set.of("sparkle"), ADMIN2, Set.of("sparkle"));          // the admins have the Special Set
    private static long remoteAt, remoteOkAt;
    static volatile String remoteRaw;                                   // the cosmetics.json text as last fetched (the Admin Panel edits a copy of it)
    /** The owner of the mod: can see everything in the Cosmetics menu and use the Admin Panel. */
    /** The owner and the other admins, as SHA-256 codes of their UUIDs. */
    static final java.util.Set<String> ADMINS = java.util.Set.of(OWNER, ADMIN2);
    static boolean isAdminCode(String code) { return code != null && ADMINS.contains(code); }
    static boolean isAdmin() { return ADMINS.contains(code(me())); }
    static { CosmeticData.grants = new HashMap<>(BUILTIN); }
    /** The Refresh button: fetch cosmetics.json again right now. */
    static void forceRefresh() { remoteAt = 0; refreshRemote(); }
    static String refreshNote() { return remoteOkAt == 0 ? "Not loaded yet (offline or the file is unreachable)." : "Last updated " + Math.max(0, (System.currentTimeMillis() - remoteOkAt) / 1000) + "s ago. It also refreshes every 10 minutes by itself."; }
    /** Admin Save: commits cosmetics.json to the GitHub repo, then applies it here straight away. */
    static void saveRemote(String json, String token, java.util.function.Consumer<String> done) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            String msg;
            try {
                var http = java.net.http.HttpClient.newHttpClient();
                String api = "https://api.github.com/repos/imtraminecraft/fancydrops/contents/cosmetics.json";
                var get = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(api + "?ref=main")).header("User-Agent", "FancyDrops").header("Accept", "application/vnd.github+json").header("Authorization", "Bearer " + token).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
                String sha = null;
                if (get.statusCode() == 200) sha = com.google.gson.JsonParser.parseString(get.body()).getAsJsonObject().get("sha").getAsString();
                else if (get.statusCode() != 404) throw new RuntimeException(get.statusCode() == 401 || get.statusCode() == 403 ? "GitHub refused the token." : "GitHub error " + get.statusCode());
                com.google.gson.JsonObject body = new com.google.gson.JsonObject();
                body.addProperty("message", "cosmetics: update"); body.addProperty("branch", "main");
                body.addProperty("content", java.util.Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                if (sha != null) body.addProperty("sha", sha);
                var put = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(api)).header("User-Agent", "FancyDrops").header("Accept", "application/vnd.github+json").header("Authorization", "Bearer " + token).PUT(java.net.http.HttpRequest.BodyPublishers.ofString(body.toString())).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
                if (put.statusCode() == 200 || put.statusCode() == 201) { applyRaw(json); msg = "Saved cosmetics.json to GitHub."; }
                else msg = put.statusCode() == 401 || put.statusCode() == 403 || put.statusCode() == 404 ? "Save refused: check the token's access to the repo." : "Save failed (" + put.statusCode() + ").";
            } catch (Throwable t) { msg = t.getMessage() != null && t.getMessage().startsWith("GitHub") ? t.getMessage() : "Could not reach GitHub."; }
            final String m = msg; Minecraft.getInstance().execute(() -> done.accept(m));
        });
    }
    /** Reads a cosmetics.json text into who-has-what. */
    static void applyRaw(String body) {
        Map<String, Set<String>> m = new HashMap<>();
        for (var e : BUILTIN.entrySet()) m.put(e.getKey(), new HashSet<>(e.getValue()));
        var root = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
        var special = root.getAsJsonObject("special");
        if (special != null) for (var e : special.entrySet()) {
            String id = CosmeticData.GRANT_KEY.get(e.getKey());
            if (id == null || !e.getValue().isJsonArray()) continue;
            for (var code : e.getValue().getAsJsonArray()) m.computeIfAbsent(code.getAsString().toLowerCase(Locale.ROOT), k -> new HashSet<>()).add(id);
        }
        var extra = root.getAsJsonObject("extra");
        if (extra != null) for (var e : extra.entrySet()) {
            if (!e.getValue().isJsonArray()) continue;
            for (var cid : e.getValue().getAsJsonArray()) m.computeIfAbsent(e.getKey().toLowerCase(Locale.ROOT), k -> new HashSet<>()).add("c:" + cid.getAsString());
        }
        remoteRaw = body; CosmeticData.grants = m; remoteOkAt = System.currentTimeMillis(); remoteAt = remoteOkAt;
    }
    private static void refreshRemote() {
        long now = System.currentTimeMillis();
        if (now - remoteAt < 600_000L) return;
        remoteAt = now;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                var res = java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(REMOTE_URL)).header("User-Agent", "FancyDrops").GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) return;
                Map<String, Set<String>> m = new HashMap<>();
                for (var e : BUILTIN.entrySet()) m.put(e.getKey(), new HashSet<>(e.getValue()));
                var special = com.google.gson.JsonParser.parseString(res.body()).getAsJsonObject().getAsJsonObject("special");
                if (special != null) for (var e : special.entrySet()) {                                       // "patreon": ["code", ...] gives only the Patreon cosmetic to those players
                    String id = CosmeticData.GRANT_KEY.get(e.getKey());
                    if (id == null || !e.getValue().isJsonArray()) continue;
                    for (var code : e.getValue().getAsJsonArray()) m.computeIfAbsent(code.getAsString().toLowerCase(Locale.ROOT), k -> new HashSet<>()).add(id);
                }
                var extra = com.google.gson.JsonParser.parseString(res.body()).getAsJsonObject().getAsJsonObject("extra");
                if (extra != null) for (var e : extra.entrySet()) {                                             // "extra": { "code": ["crown", ...] } gives single cosmetics
                    if (!e.getValue().isJsonArray()) continue;
                    for (var cid : e.getValue().getAsJsonArray()) m.computeIfAbsent(e.getKey().toLowerCase(Locale.ROOT), k -> new HashSet<>()).add("c:" + cid.getAsString());
                }
                remoteRaw = res.body();
                CosmeticData.grants = m; remoteOkAt = System.currentTimeMillis();
            } catch (Throwable ignored) { }
        });
    }
    static final FontDescription EMOJI = new FontDescription.Resource(Identifier.fromNamespaceAndPath("fancydrops", "emoji"));

    private record Seg(String text, Style style) {}
    static UUID me() { try { var u = Minecraft.getInstance().getUser(); return u == null ? null : u.getProfileId(); } catch (Throwable t) { return null; } }
    static Suffix byId(String id) { for (Suffix s : SUFFIXES) if (s.id().equals(id)) return s; return null; }
    /** Your own pick (null = no suffix). */
    static Suffix chosen() { Suffix s = byId(FancyConfig.get().suffix); return s != null && CosmeticData.unlockedId(s.id()) ? s : null; }
    /** Everybody who gets a suffix on this screen: the owners (what they picked in cosmetics.json, else the sparkle) and you (your own pick, when allowed). */
    static Map<UUID, Look> active() {
        Map<UUID, Look> m = new LinkedHashMap<>(); UUID me = me();
        FancyConfig c0 = FancyConfig.get();
        if (!(c0.masterAnimate || c0.masterAnnounce)) return m;                  // the mod is off: nothing is drawn on your screen (other players still see yours, they get it from the list, not from you)
        refreshRemote();
        var conn = Minecraft.getInstance().getConnection();
        if (conn != null) for (var info : conn.getOnlinePlayers()) {
            if (info.getProfile() == null) continue;
            UUID u = info.getProfile().id();
            if (u.equals(me)) continue;
            Set<String> g = CosmeticData.grantsOf(u);                    // everybody sees what an owner has been given
            if (g.isEmpty()) continue;
            for (CosmeticData.SetDef sd : CosmeticData.SETS) if (sd.rarity() == 3 && g.contains(sd.id())) {       // what they were given: the whole set (minus what you chose not to see)
                Look ol = new Look(c0.othersSuffix ? byId(sd.suffix()) : null, c0.othersName ? sd.fx() : null, c0.othersAura ? sd.aura() : null);
                if (ol.suffix() != null || ol.fx() != null || ol.aura() != null) m.put(u, ol);
                break;
            }
        }
        if (me != null) {                                                // you see what you picked
            FancyConfig c = FancyConfig.get();
            Look l = new Look(c.selfSuffix ? chosen() : null, c.selfName && CosmeticData.unlockedId(c.nameFx) ? c.nameFx : null, c.selfAura && CosmeticData.unlockedId(c.aura) ? c.aura : null);
            if (l.suffix() != null || l.fx() != null || l.aura() != null) m.put(me, l);
        }
        return m;
    }
    static String frameNow(Suffix s, long now) { return s.frames()[(int) ((now / 100L) % s.frames().length)]; }
    /** A suffix written like a Discord emoji: ":discord:". Every place that shows a suffix (menu tiles, previews, chat, tab list) goes through its id, and the picture is only ever read from textures/font/suffix_<id>.png, so changing that one file changes it everywhere. */
    static String token(String id) { return id == null ? "" : ":" + id + ":"; }
    /** Logs any suffix whose picture file or font entry is missing, so a wrong id never shows up as an empty box silently. */
    static void checkIcons() {
        for (Suffix s : SUFFIXES) try {                                                   // the chat/tab sprite must show what the strip shows; tools/make_suffix_sprites.py rebuilds the sprites
            String n = "sparkle".equals(s.id()) ? "sparkle" : "suffix_" + s.id();
            try (java.io.InputStream a = Cosmetics.class.getResourceAsStream("/assets/fancydrops/textures/font/" + n + ".png"); java.io.InputStream b = Cosmetics.class.getResourceAsStream("/assets/fancydrops/textures/gui/sprites/suffix/" + s.id() + ".png")) {
                if (a == null || b == null) { System.err.println("[FancyDrops] suffix " + s.id() + ": strip or chat sprite missing"); continue; }
                java.awt.image.BufferedImage sa = javax.imageio.ImageIO.read(a), sb = javax.imageio.ImageIO.read(b); boolean same = sb.getHeight() == 60;
                for (int f = 0; f < 6 && same; f++) for (int y = 0; y < 10 && same; y++) for (int x = 0; x < 10; x++) if (sa.getRGB(f * 10 + x, y) != sb.getRGB(x, f * 10 + y)) { same = false; break; }
                if (!same) System.err.println("[FancyDrops] suffix " + s.id() + ": the chat sprite differs from its strip. Run tools/make_suffix_sprites.py");
            }
        } catch (Throwable ignored) { }
        for (Suffix s : SUFFIXES) {
            if (Cosmetics.class.getResource("/assets/fancydrops/textures/font/suffix_" + s.id() + ".png") == null) System.err.println("[FancyDrops] suffix " + s.id() + " has no picture (textures/font/suffix_" + s.id() + ".png)");
        }
    }
    static Component glyph(String g) { return Component.literal(g).withStyle(Style.EMPTY.withFont(EMOJI).withColor(0xFFFFFF)); }
    private static final Map<String, int[][]> STRIPS = new HashMap<>();
    /** A suffix's six 10x10 frames, read straight from its strip picture (the one source of the suffix's looks). */
    private static int[][] strip(String id) {
        return STRIPS.computeIfAbsent(id, k -> {
            int[][] fr = new int[6][100];
            String path = "/assets/fancydrops/textures/font/" + ("sparkle".equals(k) ? "sparkle" : "suffix_" + k) + ".png";
            try (java.io.InputStream in = Cosmetics.class.getResourceAsStream(path)) {
                if (in != null) { java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(in); for (int f = 0; f < 6; f++) for (int y = 0; y < 10; y++) for (int x = 0; x < 10; x++) fr[f][y * 10 + x] = im.getRGB(f * 10 + x, y); }
            } catch (Throwable t) { System.err.println("[FancyDrops] suffix strip " + k + " unreadable: " + t); }
            return fr;
        });
    }
    /** One frame of a suffix as a menu icon: its picture drawn pixel by pixel, centred in the 16 x 16 icon box (the same place for every suffix and for the Discord button). */
    static void drawIcon(net.minecraft.client.gui.GuiGraphicsExtractor g, String id, int x, int y, long now) {
        Suffix s = byId(id); if (s == null) return;
        int[] px = strip(id)[(int) ((now / 100L) % 6)];
        Gfx.push(g); Gfx.translate(g, x + 0.5f, y + 0.5f); Gfx.scale(g, 1.5f);
        for (int yy = 0; yy < 10; yy++) { int xx = 0; while (xx < 10) { int c = px[yy * 10 + xx]; if ((c >>> 24) < 8) { xx++; continue; } int x2 = xx + 1; while (x2 < 10 && px[yy * 10 + x2] == c) x2++; g.fill(xx, yy, x2, yy + 1, c); xx = x2; } }
        Gfx.pop(g);
    }

    /** Every message we have looked at that mentions the player, kept as it arrived so it can be redone when the look changes. */
    private static final Map<Component, Component> ORIG = Collections.synchronizedMap(new WeakHashMap<>());
    static volatile boolean historyDirty;

    static Component apply(Component message) {
        try {
            Component out = build(message, false, 0L);
            if (out != message) ORIG.put(out, message);
            else {
                String me = Minecraft.getInstance().getUser() == null ? null : Minecraft.getInstance().getUser().getName();
                if (me != null && message.getString().contains(me)) { Component cp = message.copy(); ORIG.put(cp, message); return cp; }      // kept, so a suffix picked later can still be added
            }
            return out;
        } catch (Exception ignored) { return message; }
    }

    /** The message dressed for the current looks. live = the name effect's colours at time t instead of a frozen look. */
    private static Component build(Component message, boolean live, long t) {
        try {
            Minecraft mc = Minecraft.getInstance();
            String full = message.getString();
            if (full.isEmpty() || mc.getConnection() == null) return message;
            for (Map.Entry<UUID, Look> e : active().entrySet()) {
                String name = nameOf(mc, e.getKey());
                if (name == null) continue;
                int at = find(full, name);
                if (at < 0) continue;
                Look l = e.getValue(); Component out = message;
                if (l.fx() != null && NameFx.animated(l.fx())) chatFx = true;
                out = restyle(out, at, at + name.length(), l.fx(), live, t, l, name);            // the name: its effect (frozen here, chat animates it), and the hover card
                if (l.suffix() != null) out = insert(out, at + name.length(), tippedGlyph(l, name));
                return out;
            }
        } catch (Exception ignored) { }
        return message;
    }

    /** The same text with the letters from..to dressed in a name effect (fx may be null) and, when look is given, carrying the FancyDrops hover card. */
    private static Component restyle(Component msg, int from, int to, String fx, boolean live, long t, Look look, String player) {
        List<Seg> segs = new ArrayList<>();
        msg.visit((style, text) -> { segs.add(new Seg(text, style)); return Optional.empty(); }, Style.EMPTY);
        MutableComponent out = Component.empty();
        int pos = 0;
        for (Seg s : segs) {
            StringBuilder run = new StringBuilder();
            Component tp = null;
            for (int k = 0; k < s.text().length(); k++) {
                int g = pos + k;
                if (g >= from && g < to) {
                    if (run.length() > 0) { out.append(Component.literal(run.toString()).withStyle(s.style())); run.setLength(0); }
                    Style cs = fx == null ? s.style() : NameFx.style(fx, g - from, to - from, s.style(), live, t);
                    if (look != null) {
                        if (tp == null) {
                            int rgb = s.style().getColor() == null ? 0xFFFFFF : s.style().getColor().getValue();
                            tp = tip(look, player, fx == null ? "fd:nm" : "fd:fx:" + fx + ":" + String.format("%06x", rgb & 0xFFFFFF), s.style().getHoverEvent());
                        }
                        cs = cs.withHoverEvent(new HoverEvent.ShowText(tp));
                    }
                    out.append(Component.literal(String.valueOf(s.text().charAt(k))).withStyle(cs));
                } else run.append(s.text().charAt(k));
            }
            if (run.length() > 0) out.append(Component.literal(run.toString()).withStyle(s.style()));
            pos += s.text().length();
        }
        return out;
    }

    // ---- the hover card: "FancyDrops" on top, then which cosmetics the player has and how they got each one
    private static Component tip(Look l, String player, String marker, HoverEvent orig) {
        MutableComponent t = Component.empty().withStyle(Style.EMPTY.withInsertion(marker));            // the marker tells the chat which letters carry an effect
        t.append(Chat.prefix());                                                                          // the same [FancyDrops] as in chat
        t.append(Component.literal("\n" + player + "'s cosmetics").withStyle(Style.EMPTY.withColor(0xD8D8E8)));
        t.append(Component.literal("\n──────────────").withStyle(Style.EMPTY.withColor(0x4A4B58)));
        if (l.suffix() != null) tipLine(t, CosmeticData.byId(l.suffix().id()));
        if (l.fx() != null) tipLine(t, CosmeticData.byId(l.fx()));
        if (l.aura() != null) tipLine(t, CosmeticData.byId(l.aura()));
        if (orig instanceof HoverEvent.ShowText st) { t.append(Component.literal("\n")); t.append(st.value()); }       // whatever the server showed here is kept underneath
        return t;
    }
    private static void tipLine(MutableComponent t, CosmeticData.Def d) {
        if (d == null) return;
        String how = CosmeticData.force.containsKey(d.id()) ? "Test menu" : d.how();
        String ln = CosmeticData.rarityName(CosmeticData.rarityOf(d));
        t.append(Component.literal("\n" + CosmeticData.kindLabel(d.kind()) + "  ").withStyle(Style.EMPTY.withColor(0x8A93B0)));
        t.append(Component.literal(d.name()).withStyle(Style.EMPTY.withColor(d.color() & 0xFFFFFF).withBold(true)));
        t.append(Component.literal("  " + ln).withStyle(Style.EMPTY.withColor(0x6C7388).withItalic(true)));
    }
    private static Component tippedGlyph(Look l, String player) {
        return chatGlyph(l.suffix()).copy().withStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(tip(l, player, "fd:sfx", null))));
    }

    // ---- chat lines can't animate on their own, so the lines that carry an animated name effect get a wrapper that recolours the letters every frame
    private static boolean chatFx, chatFail;
    private static Object chatObj; private static java.lang.reflect.Field linesF, msgsF; private static Class<?> lineCls, msgCls;
    private static java.lang.reflect.RecordComponent[] lineRc, msgRc; private static java.lang.reflect.Constructor<?> lineCtor, msgCtor; private static int lineSeq = -1, msgContent = -1;
    private static java.lang.reflect.Method refreshM; private static boolean chatReady, logged;
    private static void log(String m) { if (!logged) System.err.println("[FancyDrops] chat: " + m); }

    private static String[] fxMark(Style s) {
        if (s.getHoverEvent() instanceof HoverEvent.ShowText st) {
            String ins = st.value().getStyle().getInsertion();
            if (ins != null && ins.startsWith("fd:fx:")) { String[] p = ins.split(":"); if (p.length == 4) return p; }
        }
        return null;
    }
    private static final class FxSeq implements net.minecraft.util.FormattedCharSequence {
        final net.minecraft.util.FormattedCharSequence in;
        FxSeq(net.minecraft.util.FormattedCharSequence in) { this.in = in; }
        @Override public boolean accept(net.minecraft.util.FormattedCharSink sink) {
            List<Style> sty = new ArrayList<>(); List<Integer> idx = new ArrayList<>(), cps = new ArrayList<>();
            in.accept((i, s, cp) -> { idx.add(i); sty.add(s); cps.add(cp); return true; });
            int n = 0; for (Style s : sty) if (fxMark(s) != null) n++;
            long now = System.currentTimeMillis(); int j = 0;
            for (int k = 0; k < sty.size(); k++) {
                Style s = sty.get(k); String[] m = fxMark(s);
                if (m != null) { try { s = NameFx.style(m[2], j++, n, s.withColor(Integer.parseInt(m[3], 16)), true, now); } catch (Exception ignored) { } }
                if (!sink.accept(idx.get(k), s, cps.get(k))) return false;
            }
            return true;
        }
    }
    private static boolean hasFx(net.minecraft.util.FormattedCharSequence cs) {
        boolean[] hit = {false};
        cs.accept((i, s, cp) -> { String[] m = fxMark(s); if (m != null && NameFx.animated(m[2])) { hit[0] = true; return false; } return true; });
        return hit[0];
    }

    /** Finds the chat's internals by shape (this Minecraft renamed its getters). */
    private static boolean findChat(Minecraft mc) {
        if (chatReady) return true;
        try {
            Object gui = mc.gui;
            Deque<Object> q = new ArrayDeque<>(); q.add(gui); Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (int depth = 0; depth < 2 && chatObj == null; depth++) {
                Deque<Object> next = new ArrayDeque<>();
                for (Object o : q) {
                    if (!seen.add(o)) continue;
                    for (var f : o.getClass().getDeclaredFields()) {
                        if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
                        f.setAccessible(true); Object v = f.get(o);
                        if (v == null) continue;
                        if (v.getClass().getSimpleName().equals("ChatComponent") || f.getType().getSimpleName().equals("ChatComponent")) { chatObj = v; break; }
                        if (v.getClass().getName().startsWith("net.minecraft")) next.add(v);
                    }
                    if (chatObj != null) break;
                }
                q = next;
            }
            if (chatObj == null) { log("chat component not found"); return false; }
            for (var f : chatObj.getClass().getDeclaredFields()) {
                if (!List.class.isAssignableFrom(f.getType())) continue;
                String gt = f.getGenericType().getTypeName();
                f.setAccessible(true);
                if (gt.contains("GuiMessage$Line")) linesF = f; else if (gt.endsWith("GuiMessage>")) msgsF = f;
            }
            for (var m : chatObj.getClass().getDeclaredMethods()) if (m.getParameterCount() == 0 && (m.getName().equals("refreshTrimmedMessages") || m.getName().equals("rescaleChat")) && refreshM == null) { m.setAccessible(true); refreshM = m; }
            log("found chat: lines=" + (linesF != null) + " messages=" + (msgsF != null) + " refresh=" + (refreshM != null));
            chatReady = linesF != null;
            return chatReady;
        } catch (Throwable t) { log("search failed: " + t); chatFail = true; return false; }
    }
    private static boolean prepLine(Object ln) throws Exception {
        if (lineCls != null) return ln.getClass() == lineCls;
        Class<?> c = ln.getClass();
        if (!c.isRecord()) { log("Line is not a record: " + c); chatFail = true; return false; }
        var rc = c.getRecordComponents(); Class<?>[] types = new Class<?>[rc.length]; int seq = -1;
        for (int i = 0; i < rc.length; i++) { types[i] = rc[i].getType(); rc[i].getAccessor().setAccessible(true); if (net.minecraft.util.FormattedCharSequence.class.isAssignableFrom(types[i])) seq = i; }
        if (seq < 0) { log("Line has no text part"); chatFail = true; return false; }
        var ct = c.getDeclaredConstructor(types); ct.setAccessible(true);
        lineCls = c; lineRc = rc; lineCtor = ct; lineSeq = seq; return true;
    }

    private static long lastLive;
    /** Every tick: (1) after you change what you wear, redo the chat lines already on screen; (2) animate the name effects in chat. */
    static void tickChat(Minecraft mc) {
        try {
            if (mc.gui == null || mc.level == null) return;
            if (historyDirty) { historyDirty = false; if (findChat(mc)) rebuildHistory(false); }
            if (!chatFx || chatFail || !findChat(mc)) return;
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) linesF.get(chatObj);
            boolean wrapped = false;
            for (int i = 0; i < list.size(); i++) {
                Object ln = list.get(i);
                if (ln == null || !prepLine(ln)) { if (chatFail) break; continue; }
                var cs = (net.minecraft.util.FormattedCharSequence) lineRc[lineSeq].getAccessor().invoke(ln);
                if (cs instanceof FxSeq) { wrapped = true; continue; }
                if (!hasFx(cs)) continue;
                Object[] args = new Object[lineRc.length];
                for (int k = 0; k < args.length; k++) args[k] = k == lineSeq ? new FxSeq(cs) : lineRc[k].getAccessor().invoke(ln);
                list.set(i, lineCtor.newInstance(args)); wrapped = true;
            }
            if (!logged && wrapped) { logged = true; System.err.println("[FancyDrops] chat: animated names active"); }
            if (chatFail && msgsF != null && refreshM != null && System.currentTimeMillis() - lastLive > 120) { lastLive = System.currentTimeMillis(); rebuildHistory(true); }      // fallback: redo the lines ten times a second
        } catch (Throwable t) { chatFail = true; System.err.println("[FancyDrops] animated chat names unavailable: " + t); }
    }

    /** Redoes every remembered chat message with the current look (live = with the moving name colours), then lets the chat re-wrap them. */
    private static void rebuildHistory(boolean live) {
        try {
            if (msgsF == null || refreshM == null) { log("cannot redo old lines (messages=" + (msgsF != null) + ", refresh=" + (refreshM != null) + ")"); return; }
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) msgsF.get(chatObj);
            long now = System.currentTimeMillis(); boolean any = false;
            for (int i = 0; i < list.size(); i++) {
                Object gm = list.get(i); if (gm == null) continue;
                if (msgCls == null) {
                    msgCls = gm.getClass();
                    if (!msgCls.isRecord()) { log("GuiMessage is not a record"); msgsF = null; return; }
                    msgRc = msgCls.getRecordComponents(); Class<?>[] types = new Class<?>[msgRc.length];
                    for (int k = 0; k < msgRc.length; k++) { types[k] = msgRc[k].getType(); msgRc[k].getAccessor().setAccessible(true); if (Component.class.isAssignableFrom(types[k])) msgContent = k; }
                    msgCtor = msgCls.getDeclaredConstructor(types); msgCtor.setAccessible(true);
                }
                if (gm.getClass() != msgCls || msgContent < 0) continue;
                Component cur = (Component) msgRc[msgContent].getAccessor().invoke(gm);
                Component orig = ORIG.get(cur); if (orig == null) continue;
                Component nu = build(orig, live, now);
                if (nu == orig) nu = orig.copy();
                if (nu.equals(cur) && !live) continue;
                ORIG.put(nu, orig);
                Object[] args = new Object[msgRc.length];
                for (int k = 0; k < args.length; k++) args[k] = k == msgContent ? nu : msgRc[k].getAccessor().invoke(gm);
                list.set(i, msgCtor.newInstance(args)); any = true;
            }
            if (any) refreshM.invoke(chatObj);
        } catch (Throwable t) { log("redo failed: " + t); }
    }

    /** The name over a player's head with their effect and suffix, or null when they wear none. */
    static volatile boolean nameTagHooked;                                  // true once the name-tag mixin has run
    static Component nameTag(Component base, Entity e) {
        if (base == null || !(e instanceof net.minecraft.world.entity.player.Player)) return null;
        Look lk = looksMap.get(e.getUUID());
        if (lk == null || (lk.fx() == null && lk.suffix() == null)) return null;
        String name = e.getName().getString(), bs = base.getString();
        int at = find(bs, name);
        if (at < 0) return null;
        Component out = base;
        if (lk.fx() != null) out = restyle(out, at, at + name.length(), lk.fx(), true, System.currentTimeMillis(), null, null);
        if (lk.suffix() != null) out = insert(out, at + name.length(), chatGlyph(lk.suffix()));
        return out;
    }

    private static volatile Map<UUID, Look> looksMap = Map.of();
    static Map<UUID, Look> looks() { return looksMap; }
    /** Every tick: who wears what right now (the aura drawing and the tab read this). */
    static void tickAura(Minecraft mc) {
        try { looksMap = mc.level == null ? Map.of() : active(); } catch (Throwable ignored) { }
    }

    /** A name in the menu preview: the colour you have on the server, with the effect on top. */
    static MutableComponent nameComp(String name, int base, String fx, boolean live, long now) {
        if (fx == null || fx.isEmpty()) return Component.literal(name).withStyle(Style.EMPTY.withColor(base));
        FancyConfig cf = FancyConfig.get(); now = cf.cosCalm ? now / 6 : now * cf.cosSpeed / 100;           // name effect speed; calm mode nearly stills it
        MutableComponent o = Component.empty();
        for (int i = 0; i < name.length(); i++) o.append(Component.literal(String.valueOf(name.charAt(i))).withStyle(NameFx.style(fx, i, name.length(), Style.EMPTY.withColor(base), live, now)));
        return o;
    }


    private static final Map<String, Component> CHAT_GLYPH = new HashMap<>();
    /** The suffix as it goes into chat: an animated GUI sprite (assets/fancydrops/textures/gui/sprites/suffix/<id>.png + .mcmeta play on their own), or the still emoji if this Minecraft can't draw one. */
    static Component chatGlyph(Suffix s) {
        return CHAT_GLYPH.computeIfAbsent(s.id(), id -> {
            try {
                var json = com.google.gson.JsonParser.parseString("{\"object\":\"atlas\",\"atlas\":\"minecraft:gui\",\"sprite\":\"fancydrops:suffix/" + id + "\"}");
                var res = net.minecraft.network.chat.ComponentSerialization.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json);
                Component c = res.result().orElse(null);
                if (c != null) return c;
            } catch (Throwable t) { System.err.println("[FancyDrops] animated chat suffix unavailable: " + t); }
            return glyph(s.frames()[0]);
        });
    }

    /** Also puts the animated suffix after the name in the tab list. SkyBlock draws its tab from many fake entries ("[496] imtra [x]" is one of them), so every entry is checked for an owner's name, not only the real one. Called every few ticks; the server rewriting an entry is picked up as its new base. */
    static volatile boolean tabMoving;
    /** Called every frame just before the tab list is drawn: moving name effects need a new colour every frame, not every tick. */
    private static long tabFrameAt;
    static void tickTabFrame(Minecraft mc) { long n = System.currentTimeMillis(); if (tabMoving && n - tabFrameAt >= 30) { tabFrameAt = n; tickTab(mc); } }
    static void tickTab(Minecraft mc) {
        try {
            var conn = mc.getConnection();
            if (conn == null) return;
            long now = System.currentTimeMillis();
            Map<UUID, Look> act = active();
            boolean anyMoving = false;
            Map<String, Look> byName = new LinkedHashMap<>();
            for (Map.Entry<UUID, Look> e : act.entrySet()) { String n = nameOf(mc, e.getKey()); if (n != null) byName.put(n, e.getValue()); }
            for (var info : conn.getOnlinePlayers()) {
                if (info.getProfile() == null) continue;
                UUID id = info.getProfile().id();
                Component cur = info.getTabListDisplayName();
                boolean ours = cur != null && cur == TAB_SET.get(id);
                Component base = ours ? TAB_BASE.get(id) : cur;
                String baseStr = base == null ? "" : base.getString();
                Map.Entry<String, Look> hit = null; int at = -1;
                for (Map.Entry<String, Look> e : byName.entrySet()) {
                    if (e.getValue().suffix() == null && e.getValue().fx() == null) continue;
                    if (base != null) { int f = find(baseStr, e.getKey()); if (f >= 0) { hit = e; at = f; break; } }
                    else if (act.containsKey(id) && e.getKey().equals(info.getProfile().name())) { hit = e; break; }                 // your own entry with no display name yet
                }
                if (hit == null) {                                                                                                    // not (or no longer) an entry that carries a cosmetic: take ours off
                    if (ours && TAB_BASE.containsKey(id)) info.setTabListDisplayName(TAB_BASE.get(id));
                    TAB_SET.remove(id); TAB_ID.remove(id);
                    continue;
                }
                Look lk = hit.getValue(); String key = (lk.suffix() == null ? "" : lk.suffix().id()) + "|" + (lk.fx() == null ? "" : lk.fx());
                boolean moving = lk.fx() != null && NameFx.animated(lk.fx()); anyMoving |= moving;
                if (ours && key.equals(TAB_ID.get(id)) && !moving) continue;                    // already showing it (the suffix picture animates by itself, same as in chat)
                if (base == null) {
                    base = Component.literal(info.getProfile().name());
                    try { base = net.minecraft.world.scores.PlayerTeam.formatNameForTeam(info.getTeam(), base); } catch (Throwable ignored) { }
                    at = find(base.getString(), hit.getKey());
                }
                int len = hit.getKey().length();
                Component out = base;
                if (at >= 0 && lk.fx() != null) out = restyle(out, at, at + len, lk.fx(), true, now, null, null);
                if (lk.suffix() != null) out = at >= 0 ? insert(out, at + len, chatGlyph(lk.suffix())) : Component.empty().append(out).append(Component.literal(" ")).append(chatGlyph(lk.suffix()));
                TAB_BASE.put(id, base); TAB_SET.put(id, out); TAB_ID.put(id, key);
                info.setTabListDisplayName(out); tabTouched++;
            }
            tabMoving = anyMoving;
            if (now - tabLogAt > 4000 && (anyMoving || tabTouched > 0)) { tabLogAt = now; System.err.println("[FancyDrops] tab: moving=" + anyMoving + " entries restyled=" + tabTouched + " skyblocker hook=" + (sbkUpd != null ? "method" : sbkFlag != null ? "flag" : "none") + " nudges=" + sbkNudges + " nudgeErrors=" + sbkErr + " skysoft nudges=" + ssNudges); tabTouched = 0; }
            if (anyMoving) nudgeCustomTabs(now);
        } catch (Throwable ignored) { }
    }

    private static int tabTouched, sbkNudges, sbkErr, ssNudges; private static Object ssInst; private static java.lang.reflect.Method ssDirty; private static long tabLogAt;
    private static java.lang.reflect.Field sbkFlag; private static java.lang.reflect.Method sbkUpd; private static boolean sbkTried; private static long sbkAt;
    /** Custom tab lists (Skyblocker's) copy the player list once in a while instead of every frame, so a moving name would stand still there. Ask it to copy again, a few times a second, while a name is moving. */
    private static void nudgeCustomTabs(long now) {
        if (now - sbkAt < 50) return; sbkAt = now;
        try {
            if (!sbkTried) {
                sbkTried = true;
                try {                                                                                        // so the log names whichever tab mod is installed
                    StringBuilder mods = new StringBuilder();
                    for (var m : net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods()) { String mid = m.getMetadata().getId(); if (mid.toLowerCase().matches(".*(sky|tab|hud).*")) mods.append(mid).append(' '); }
                    System.err.println("[FancyDrops] tab-related mods: " + mods);
                } catch (Throwable ignored) { }
                try {
                    Class<?> t = Class.forName("com.skysoft.data.hypixel.TabListApi");
                    ssInst = t.getField("INSTANCE").get(null);
                    for (var m : t.getDeclaredMethods()) if (m.getName().startsWith("markDirty") && m.getParameterCount() == 0) { m.setAccessible(true); ssDirty = m; break; }
                    System.err.println("[FancyDrops] SkySoft tab found: " + (ssDirty != null ? ssDirty.getName() : "no hook"));
                } catch (Throwable ignored) { }
                try {
                    Class<?> c = Class.forName("de.hysky.skyblocker.skyblock.tabhud.util.PlayerListManager");
                    try { sbkFlag = c.getDeclaredField("shouldUpdateNextTick"); sbkFlag.setAccessible(true); } catch (Throwable ignored) { }
                    try { sbkUpd = c.getDeclaredMethod("updateListNow"); sbkUpd.setAccessible(true); } catch (Throwable ignored) { }
                    System.err.println("[FancyDrops] custom tab list found (Skyblocker): " + (sbkFlag != null ? "flag" : sbkUpd != null ? "method" : "no hook"));
                } catch (Throwable ignored) { }
            }
            if (ssDirty != null) { ssDirty.invoke(ssInst); ssNudges++; }                                   // SkySoft's Better Tab keeps its own copy of the list: mark it stale so it reads the new names
            if (sbkUpd != null) { sbkUpd.invoke(null); sbkNudges++; }                                       // copy the list right now (the flag only copies on the next tick, after the server's own rewrite)
            else if (sbkFlag != null) { sbkFlag.setBoolean(null, true); sbkNudges++; }
        } catch (Throwable t) { if (sbkErr++ < 3) System.err.println("[FancyDrops] tab nudge failed: " + t); }
    }

    private static String nameOf(Minecraft mc, UUID id) {
        var info = mc.getConnection().getPlayerInfo(id);
        if (info != null && info.getProfile() != null) return info.getProfile().name();
        if (mc.getUser() != null && id.equals(mc.getUser().getProfileId())) return mc.getUser().getName();
        return null;
    }

    /** First whole-word, case-sensitive occurrence of the name, so "ImTrap" or "xImTra" don't match. */
    private static int find(String text, String name) {
        int from = 0;
        while (true) {
            int i = text.indexOf(name, from);
            if (i < 0) return -1;
            int end = i + name.length();
            boolean before = i == 0 || !word(text.charAt(i - 1)), after = end >= text.length() || !word(text.charAt(end));
            if (before && after) return i;
            from = i + 1;
        }
    }
    private static boolean word(char c) { return Character.isLetterOrDigit(c) || c == '_'; }

    private static Component insert(Component message, int offset, String glyph, Style gs) { return insert(message, offset, Component.literal(glyph).withStyle(gs)); }

    private static Component insert(Component message, int offset, Component glyphComp) {
        List<Seg> segs = new ArrayList<>();
        message.visit((style, text) -> { segs.add(new Seg(text, style)); return Optional.empty(); }, Style.EMPTY);
        MutableComponent out = Component.empty();
        int pos = 0; boolean done = false;
        for (Seg s : segs) {
            int len = s.text().length();
            if (!done && offset <= pos + len) {
                int cut = offset - pos;
                out.append(Component.literal(s.text().substring(0, cut)).withStyle(s.style()));
                String all = message.getString(); boolean spBefore = offset > 0 && offset <= all.length() && all.charAt(offset - 1) == ' ', spAfter = offset < all.length() && all.charAt(offset) == ' ';
                if (!spBefore) out.append(Component.literal(" ").withStyle(s.style()));
                out.append(glyphComp);
                if (!spAfter) out.append(Component.literal(" ").withStyle(s.style()));      // a gap keeps the colon after the name from hiding behind the picture (not needed when a space follows anyway)
                if (cut < len) out.append(Component.literal(s.text().substring(cut)).withStyle(s.style()));
                done = true;
            } else out.append(Component.literal(s.text()).withStyle(s.style()));
            pos += len;
        }
        return out;
    }

    private Cosmetics() {}
}
