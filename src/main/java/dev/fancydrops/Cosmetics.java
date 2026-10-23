package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.core.particles.ParticleTypes;
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
        new Suffix("crown", "Crown", "v0.5.0", fr(0xE040)),
        new Suffix("gem", "Gem", "v0.5.0", fr(0xE050)),
        new Suffix("flame", "Flame", "v0.5.0", fr(0xE060)),
        new Suffix("clover", "Lucky Clover", "v0.5.0", fr(0xE070)),
        new Suffix("heart", "Heartbeat", "v0.8.0", fr(0xE080)),
        new Suffix("bolt", "Lightning", "v0.8.0", fr(0xE090)),
        new Suffix("snow", "Snowflake", "v0.8.0", fr(0xE0A0)),
        new Suffix("star", "Shooting Star", "v0.8.0", fr(0xE0B0)),
        new Suffix("ghost", "Ghost", "v0.8.0", fr(0xE0C0)),
        new Suffix("newcomer", "Newcomer", "Everyone", one(0xE0D0)),
        new Suffix("discord", "Discord", "Everyone", one(0xE0D1)),
        new Suffix("firstdrop", "First Drop", "Everyone", one(0xE0D2)),
        new Suffix("collector", "Collector", "Everyone", one(0xE0D3)),
        new Suffix("secret", "Secret", "Everyone", fr(0xE0E0))};
    /** Who owns cosmetics. Everyone with the mod sees their suffix; only they can open the Cosmetics page. */
    /** Owners are stored as SHA-256 codes of their UUID, never as the UUID itself. This one is built in; more come from cosmetics.json. */
    private static final String OWNER = "7ad5d2b31b239f2ed148d1de18f8214967bf7d1a411c838d466e9829ab7c2bf8";
    private static final Map<UUID, String> CODES = new HashMap<>();
    static String code(UUID u) { return u == null ? "" : CODES.computeIfAbsent(u, k -> { try { var md = java.security.MessageDigest.getInstance("SHA-256"); StringBuilder b = new StringBuilder(); for (byte x : md.digest(k.toString().toLowerCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8))) b.append(String.format("%02x", x)); return b.toString(); } catch (Exception e) { return ""; } }); }
    private static final Map<UUID, Component> TAB_BASE = new HashMap<>();
    private static final Map<UUID, Component> TAB_SET = new HashMap<>();       // the name we put in the tab list for each player (compared by identity: if it changed, the server rewrote it)
    private static final Map<UUID, String> TAB_ID = new HashMap<>();
    /** What the owners picked, read from cosmetics.json in the GitHub repo so everybody with the mod sees the same thing. */
    private static final String REMOTE_URL = "https://raw.githubusercontent.com/imtraminecraft/fancydrops/main/cosmetics.json";
    private static final Map<String, Set<String>> BUILTIN = Map.of(OWNER, Set.of("sparkle"));
    private static long remoteAt, remoteOkAt;
    static { CosmeticData.grants = new HashMap<>(BUILTIN); }
    /** The Refresh button: fetch cosmetics.json again right now. */
    static void forceRefresh() { remoteAt = 0; refreshRemote(); }
    static String refreshNote() { return remoteOkAt == 0 ? "Not loaded yet (offline or the file is unreachable)." : "Last updated " + Math.max(0, (System.currentTimeMillis() - remoteOkAt) / 1000) + "s ago. It also refreshes every 10 minutes by itself."; }
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
    private static Map<UUID, Look> active() {
        Map<UUID, Look> m = new LinkedHashMap<>(); UUID me = me();
        refreshRemote();
        var conn = Minecraft.getInstance().getConnection();
        if (conn != null) for (var info : conn.getOnlinePlayers()) {
            if (info.getProfile() == null) continue;
            UUID u = info.getProfile().id();
            if (u.equals(me)) continue;
            Set<String> g = CosmeticData.grantsOf(u);                    // everybody sees what an owner has been given
            if (g.isEmpty()) continue;
            m.put(u, new Look(g.contains("sparkle") ? byId("sparkle") : null, g.contains("royal") ? "royal" : g.contains("glitch") ? "glitch" : null, g.contains("stardust") ? "stardust" : null));
        }
        if (me != null) {                                                // you see what you picked
            FancyConfig c = FancyConfig.get();
            Look l = new Look(chosen(), CosmeticData.unlockedId(c.nameFx) ? c.nameFx : null, CosmeticData.unlockedId(c.aura) ? c.aura : null);
            if (l.suffix() != null || l.fx() != null || l.aura() != null) m.put(me, l);
        }
        return m;
    }
    static String frameNow(Suffix s, long now) { return s.frames()[(int) ((now / 100L) % s.frames().length)]; }
    static Component glyph(String g) { return Component.literal(g).withStyle(Style.EMPTY.withFont(EMOJI).withColor(0xFFFFFF)); }
    /** One frame of a suffix as a menu icon. */
    static void drawIcon(net.minecraft.client.gui.GuiGraphicsExtractor g, String id, int x, int y, long now) {
        Suffix s = byId(id); if (s == null) return;
        Gfx.push(g); Gfx.translate(g, x + 1, y + 1); Gfx.scale(g, 1.4f);
        g.text(Minecraft.getInstance().font, glyph(frameNow(s, now)), 0, 0, 0xFFFFFFFF, false);
        Gfx.pop(g);
    }

    static Component apply(Component message) {
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
                out = restyle(out, at, at + name.length(), l.fx(), false, 0L, l, name);            // the name: its effect (frozen here, chat animates it), and the hover card
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
        t.append(Component.literal("FancyDrops").withStyle(Style.EMPTY.withColor(0x62E6A8).withBold(true)));
        t.append(Component.literal("\n" + player + "'s cosmetics").withStyle(Style.EMPTY.withColor(0xAAAAAA)));
        if (l.suffix() != null) tipLine(t, CosmeticData.byId(l.suffix().id()));
        if (l.fx() != null) tipLine(t, CosmeticData.byId(l.fx()));
        if (l.aura() != null) tipLine(t, CosmeticData.byId(l.aura()));
        if (orig instanceof HoverEvent.ShowText st) { t.append(Component.literal("\n")); t.append(st.value()); }       // whatever the server showed here is kept underneath
        return t;
    }
    private static void tipLine(MutableComponent t, CosmeticData.Def d) {
        if (d == null) return;
        String how = CosmeticData.force.containsKey(d.id()) ? "Test menu" : d.how();
        String ln = d.line() == CosmeticData.Line.SPECIAL ? "Special" : d.line() == CosmeticData.Line.BETA ? "Beta" : "Earned";
        t.append(Component.literal("\n" + CosmeticData.kindLabel(d.kind()) + ": ").withStyle(Style.EMPTY.withColor(0x888888)));
        t.append(Component.literal(d.name()).withStyle(Style.EMPTY.withColor(d.color() & 0xFFFFFF)));
        t.append(Component.literal("\n   " + ln + " · " + how).withStyle(Style.EMPTY.withColor(0x666666)));
    }
    private static Component tippedGlyph(Look l, String player) {
        MutableComponent g = Component.empty().append(chatGlyph(l.suffix()));
        g.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(tip(l, player, "fd:sfx", null))));
        return g;
    }

    // ---- chat lines can't animate on their own, so the lines that carry an animated name effect get a wrapper that recolours the letters every frame
    private static boolean chatFx, chatFail; private static Object chatObj;
    private static java.lang.reflect.Field chatLines; private static Class<?> lineCls; private static java.lang.reflect.RecordComponent[] lineRc; private static java.lang.reflect.Constructor<?> lineCtor; private static int lineSeq;

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
    /** Every tick: wrap the freshly added chat lines that carry an animated name effect. Found by reflection; if anything is different in this Minecraft, chat just keeps the still look. */
    static void tickChat(Minecraft mc) {
        if (!chatFx || chatFail) return;
        try {
            if (chatObj == null) for (var f : mc.gui.getClass().getDeclaredFields()) {                       // the chat component, found by its type (its getter has a different name in this version)
                if (f.getType().getSimpleName().equals("ChatComponent")) { f.setAccessible(true); chatObj = f.get(mc.gui); break; }
            }
            Object chat = chatObj;
            if (chat == null) { chatFail = true; return; }
            if (chatLines == null) {
                for (var f : chat.getClass().getDeclaredFields()) {
                    if (!List.class.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true); List<?> l = (List<?>) f.get(chat);
                    if (l == null || l.isEmpty() || l.get(0) == null) continue;
                    Class<?> c = l.get(0).getClass();
                    if (!c.isRecord() || !c.getSimpleName().equals("Line")) continue;
                    var rc = c.getRecordComponents(); Class<?>[] types = new Class<?>[rc.length]; int seq = -1;
                    for (int i = 0; i < rc.length; i++) { types[i] = rc[i].getType(); rc[i].getAccessor().setAccessible(true); if (net.minecraft.util.FormattedCharSequence.class.isAssignableFrom(types[i])) seq = i; }
                    if (seq < 0) { chatFail = true; return; }
                    var ct = c.getDeclaredConstructor(types); ct.setAccessible(true);
                    chatLines = f; lineCls = c; lineRc = rc; lineCtor = ct; lineSeq = seq; break;
                }
                if (chatLines == null) return;
            }
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) chatLines.get(chat);
            for (int i = 0; i < list.size(); i++) {
                Object ln = list.get(i);
                if (ln == null || ln.getClass() != lineCls) continue;
                var cs = (net.minecraft.util.FormattedCharSequence) lineRc[lineSeq].getAccessor().invoke(ln);
                if (cs instanceof FxSeq || !hasFx(cs)) continue;
                Object[] args = new Object[lineRc.length];
                for (int k = 0; k < args.length; k++) args[k] = k == lineSeq ? new FxSeq(cs) : lineRc[k].getAccessor().invoke(ln);
                list.set(i, lineCtor.newInstance(args));
            }
        } catch (Throwable t) { chatFail = true; System.err.println("[FancyDrops] animated chat names unavailable: " + t); }
    }

    /** A name in the menu preview: the colour you have on the server, with the effect on top. */
    static MutableComponent nameComp(String name, int base, String fx, boolean live, long now) {
        if (fx == null || fx.isEmpty()) return Component.literal(name).withStyle(Style.EMPTY.withColor(base));
        MutableComponent o = Component.empty();
        for (int i = 0; i < name.length(); i++) o.append(Component.literal(String.valueOf(name.charAt(i))).withStyle(NameFx.style(fx, i, name.length(), Style.EMPTY.withColor(base), live, now)));
        return o;
    }

    /** Menu icon of a name effect ("Aa" in its colours) or an aura (rising sparks). */
    static void drawCosIcon(net.minecraft.client.gui.GuiGraphicsExtractor g, String id, int x, int y, long now) {
        CosmeticData.Def d = CosmeticData.byId(id); if (d == null) return;
        var font = Minecraft.getInstance().font;
        if (d.kind() == CosmeticData.Kind.NAME) {
            String t = "Aa"; int cx = x + 2;
            for (int i = 0; i < t.length(); i++) { Gfx.text(g, font, String.valueOf(t.charAt(i)), cx, y + 4, 0xFF000000 | NameFx.color(id, i, t.length(), 0xDDE6FF, now)); cx += font.width(String.valueOf(t.charAt(i))) + 1; }
        } else {
            for (int i = 0; i < 6; i++) {
                float u = ((now / 900f) + i / 6f) % 1f; int px = x + 2 + (i * 5) % 12, py = y + 14 - Math.round(u * 13f);
                int al = Math.round(255 * (float) Math.sin(u * Math.PI));
                g.fill(px, py, px + 2, py + 2, Gfx.argb(Math.max(0, Math.min(255, al)), 0xBFE6FF));
            }
        }
    }

    private static int auraTick;
    /** The aura: two rings of sparks spiral up round every player who wears one (you, and the owners who were given it). */
    static void tickAura(Minecraft mc) {
        try {
            if (mc.level == null) return;
            Map<UUID, Look> act = active();
            if (act.isEmpty()) return;
            auraTick++;
            for (var p : mc.level.players()) {
                Look l = act.get(p.getUUID());
                if (l == null || !"stardust".equals(l.aura())) continue;
                var rnd = mc.level.getRandom();
                for (int k = 0; k < 3; k++) {
                    double ang = auraTick * 0.25 + k * Math.PI * 2 / 3, h = ((auraTick * 0.04 + k / 3.0) % 1.0) * 2.0;
                    mc.level.addParticle(ParticleTypes.END_ROD, p.getX() + Math.cos(ang) * 0.7, p.getY() + h, p.getZ() + Math.sin(ang) * 0.7, 0.0, 0.01, 0.0);
                }
                if (auraTick % 2 == 0)
                    mc.level.addParticle(ParticleTypes.END_ROD, p.getX() + (rnd.nextDouble() - 0.5) * 1.3, p.getY() + rnd.nextDouble() * 2.0, p.getZ() + (rnd.nextDouble() - 0.5) * 1.3, 0.0, 0.03, 0.0);
            }
        } catch (Throwable ignored) { }
    }

    private static final Map<String, Component> CHAT_GLYPH = new HashMap<>();
    /** The suffix as it goes into chat: an animated GUI sprite (assets/fancydrops/textures/gui/sprites/suffix/<id>.png + .mcmeta play on their own), or the still emoji if this Minecraft can't draw one. */
    private static Component chatGlyph(Suffix s) {
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
    static void tickTab(Minecraft mc) {
        try {
            var conn = mc.getConnection();
            if (conn == null) return;
            long now = System.currentTimeMillis();
            Map<UUID, Look> act = active();
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
                boolean moving = lk.fx() != null && NameFx.animated(lk.fx());
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
                info.setTabListDisplayName(out);
            }
        } catch (Throwable ignored) { }
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
                out.append(Component.literal(" ").withStyle(s.style()));
                out.append(glyphComp);
                out.append(Component.literal(" ").withStyle(s.style()));      // the picture is a little wider than its slot: a gap keeps the colon after the name from hiding behind it
                if (cut < len) out.append(Component.literal(s.text().substring(cut)).withStyle(s.style()));
                done = true;
            } else out.append(Component.literal(s.text()).withStyle(s.style()));
            pos += len;
        }
        return out;
    }

    private Cosmetics() {}
}
