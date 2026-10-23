package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * Little name suffixes for chosen players (ImTra gets an "Active ON" button after the name: still in chat, animated in the tab list).
 * The player is identified by UUID: we look their current name up in the tab list (or use your own account), then add the
 * emoji after that name in incoming chat lines. It only changes how YOU see the message; nobody else sees it.
 * The emoji is drawn with a font of our own (assets/fancydrops/font/emoji.json), because Minecraft's default font has no emoji.
 */
final class Cosmetics {
    /** A pre-made animated suffix: 6 glyphs of our emoji font, one per frame. Chat shows a twinkling still, the tab list plays all of them. */
    record Suffix(String id, String name, String since, String[] frames) {}
    private static String[] fr(int base) { String[] f = new String[6]; for (int i = 0; i < 6; i++) f[i] = String.valueOf((char) (base + i)); return f; }
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
        new Suffix("ghost", "Ghost", "v0.8.0", fr(0xE0C0))};
    /** Who may use the cosmetics in the public release (the dev build is open to everyone). */
    static final Set<UUID> WHITELIST = Set.of(UUID.fromString("1350c2e2-3c1e-45fe-984f-fe2e9c2e868a"));
    private static final Map<UUID, Component> TAB_BASE = new HashMap<>();
    static final FontDescription EMOJI = new FontDescription.Resource(Identifier.fromNamespaceAndPath("fancydrops", "emoji"));

    private record Seg(String text, Style style) {}
    /** A font holding only this suffix's frames: with the obfuscated style the game picks one of them at random every frame, so the suffix twinkles in chat. */
    static FontDescription chatFont(Suffix s) { return new FontDescription.Resource(Identifier.fromNamespaceAndPath("fancydrops", "suf_" + s.id())); }

    static UUID me() { try { var u = Minecraft.getInstance().getUser(); return u == null ? null : u.getProfileId(); } catch (Throwable t) { return null; } }
    /** The dev build is open to everyone; the public build only to whitelisted UUIDs. */
    static boolean allowed() { if (!"Main".equals(FancyDropsMod.CHANNEL)) return true; UUID m = me(); return m != null && WHITELIST.contains(m); }
    static Suffix byId(String id) { for (Suffix s : SUFFIXES) if (s.id().equals(id)) return s; return null; }
    /** Your own pick (null = no suffix). */
    static Suffix chosen() { return byId(FancyConfig.get().suffix); }
    /** Everybody who gets a suffix on this screen: whitelisted players (the classic sparkle) and you (your pick, when allowed). */
    private static Map<UUID, Suffix> active() {
        Map<UUID, Suffix> m = new LinkedHashMap<>(); UUID me = me();
        for (UUID u : WHITELIST) if (!u.equals(me)) m.put(u, SUFFIXES[0]);
        if (me != null && allowed()) { Suffix c = chosen(); if (c != null) m.put(me, c); }
        return m;
    }
    static String frameNow(Suffix s, long now) { return s.frames()[(int) ((now / 110L) % s.frames().length)]; }
    static Component glyph(String g) { return Component.literal(g).withStyle(Style.EMPTY.withFont(EMOJI).withColor(0xFFFFFF)); }
    /** One frame of a suffix as a menu icon. */
    static void drawIcon(net.minecraft.client.gui.GuiGraphicsExtractor g, String id, int x, int y, long now) {
        Suffix s = byId(id); if (s == null) return;
        Gfx.push(g); Gfx.translate(g, x + 1, y + 1); Gfx.scale(g, 1.4f);
        g.text(Minecraft.getInstance().font, glyph(frameNow(s, now)), 0, 0, 0xFFFFFFFF, false);
        Gfx.pop(g);
    }

    /** One line for the Cosmetics page: is the suffix reaching the game right now? */
    static String status() {
        try {
            Minecraft mc = Minecraft.getInstance(); UUID me = me();
            if (mc.getConnection() == null || me == null) return "Join a server to see it in the game. The preview above shows how it will look.";
            Suffix c = chosen();
            if (c == null) return "No suffix picked, so nothing is added to your name.";
            return mc.getConnection().getPlayerInfo(me) != null ? "Live: your " + c.name() + " suffix is showing after your name in chat and the tab list." : "Waiting for the tab list to load your name.";
        } catch (Throwable t) { return ""; }
    }

    static Component apply(Component message) {
        try {
            Minecraft mc = Minecraft.getInstance();
            String full = message.getString();
            if (full.isEmpty() || mc.getConnection() == null) return message;
            for (Map.Entry<UUID, Suffix> e : active().entrySet()) {
                String name = nameOf(mc, e.getKey());
                if (name == null) continue;
                int at = find(full, name);
                if (at < 0) continue;
                return insert(message, at + name.length(), e.getValue().frames()[0], Style.EMPTY.withFont(chatFont(e.getValue())).withColor(0xFFFFFF).withObfuscated(true));
            }
        } catch (Exception ignored) { }
        return message;
    }

    private static boolean ours(String curStr) { for (Suffix s : SUFFIXES) for (String f : s.frames()) if (curStr.contains(f)) return true; return false; }

    /** Also puts the animated suffix after the name in the tab list. SkyBlock draws its tab from many fake entries ("[496] imtra [x]" is one of them), so every entry is checked for an owner's name, not only the real one. Called every few ticks; the server rewriting an entry is picked up as its new base. */
    static void tickTab(Minecraft mc) {
        try {
            var conn = mc.getConnection();
            if (conn == null) return;
            long now = System.currentTimeMillis();
            Map<UUID, Suffix> act = active();
            Map<String, Suffix> byName = new LinkedHashMap<>();
            for (Map.Entry<UUID, Suffix> e : act.entrySet()) { String n = nameOf(mc, e.getKey()); if (n != null) byName.put(n, e.getValue()); }
            for (var info : conn.getOnlinePlayers()) {
                if (info.getProfile() == null) continue;
                UUID id = info.getProfile().id();
                Component cur = info.getTabListDisplayName();
                String curStr = cur == null ? "" : cur.getString();
                boolean ours = ours(curStr);
                Component base = ours ? TAB_BASE.get(id) : cur;
                String baseStr = base == null ? "" : base.getString();
                Map.Entry<String, Suffix> hit = null; int at = -1;
                for (Map.Entry<String, Suffix> e : byName.entrySet()) {
                    if (base != null) { int f = find(baseStr, e.getKey()); if (f >= 0) { hit = e; at = f; break; } }
                    else if (act.containsKey(id) && e.getKey().equals(info.getProfile().name())) { hit = e; break; }                 // your own entry with no display name yet
                }
                if (hit == null) {                                                                                                    // not (or no longer) an entry that carries a suffix: take ours off
                    if (ours && TAB_BASE.containsKey(id)) { info.setTabListDisplayName(TAB_BASE.get(id)); }
                    continue;
                }
                String frame = frameNow(hit.getValue(), now);
                if (curStr.contains(frame)) continue;
                if (base == null) {
                    base = Component.literal(info.getProfile().name());
                    try { base = net.minecraft.world.scores.PlayerTeam.formatNameForTeam(info.getTeam(), base); } catch (Throwable ignored) { }
                    TAB_BASE.put(id, base);
                    info.setTabListDisplayName(Component.empty().append(base).append(Component.literal(" ")).append(glyph(frame)));
                } else {
                    if (!ours) TAB_BASE.put(id, base);
                    info.setTabListDisplayName(insert(base, at + hit.getKey().length(), frame, Style.EMPTY.withFont(EMOJI).withColor(0xFFFFFF)));
                }
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

    private static Component insert(Component message, int offset, String glyph, Style gs) {
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
                out.append(Component.literal(glyph).withStyle(gs));
                if (cut < len) out.append(Component.literal(s.text().substring(cut)).withStyle(s.style()));
                done = true;
            } else out.append(Component.literal(s.text()).withStyle(s.style()));
            pos += len;
        }
        return out;
    }

    private Cosmetics() {}
}
