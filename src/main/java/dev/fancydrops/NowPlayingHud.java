package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Now Playing (Visuals > Now Playing), placed wherever you drag it (Visuals > Move HUD). Three looks:
 *
 *   GLASS (default): a dark glass panel washed with the album's colour from the left. The cover sits in it (it flips in when the song
 *     changes, dims with a pause sign when you pause), the title and artists beside it, a little mirrored equalizer on the right, and
 *     the time along the bottom: elapsed, a bar with a glowing head, time left. Optionally the line being sung, under the artists.
 *   VINYL: a record spinning half out of a dark bar, the cover as its label; it slows to a stop when you pause and whirls on a new song.
 *   MINI: a slim pill: the cover, the title, and a hairline of progress.
 *
 * It slides away when nothing is playing, waits under boss bars, and hides while an RNG animation has its bars down.
 */
final class NowPlayingHud {
    private NowPlayingHud() {}

    static final int GLASS = 0, VINYL = 1, MINI = 2;
    private static float show; private static long last = System.currentTimeMillis();
    private static String lastId = ""; private static long changedAt;
    private static boolean lastPlayingSeen; private static long pausedSince;
    /** The demo shown while you move the HUD with nothing playing. */
    static boolean preview;
    /** Where it was last drawn (screen pixels), for the HUD editor. */
    static int bx0, by0, bx1, by1;

    /** From the HUD (not while the HUD editor draws its own). */
    static void hud(GuiGraphicsExtractor g) { if (!preview) render(g); }

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis(); float dt = Math.min(0.1f, (now - last) / 1000f); last = now;
        Spotify.Track t = preview && Spotify.track == null ? new Spotify.Track("demo", "Song title", "Artist", "", 200_000, "") : Spotify.track;
        boolean playing = preview || Spotify.playing;
        if (!Spotify.playing && lastPlayingSeen) pausedSince = now; lastPlayingSeen = Spotify.playing;
        long hideAfter = c.npHidePaused == 0 ? Long.MAX_VALUE : c.npHidePaused == 1 ? 8000 : 0;
        boolean want = preview || (c.npOn && Spotify.linked() && t != null && (Spotify.playing || now - pausedSince < hideAfter) && !AnimationManager.hidesHud() && mc.level != null);
        show += ((want ? 1f : 0f) - show) * Math.min(1f, dt * 8f);
        if (show < 0.01f || t == null) return;
        if (!t.id().equals(lastId)) { lastId = t.id(); changedAt = now; }
        int W = g.guiWidth(); float k = c.npScale / 100f, e = show * show * (3 - 2 * show);
        float cx = W * c.npX, h = height(c.npStyle, c.npLyric) * k;
        float top = below(mc, W, cx, width(c.npStyle) * k, c.npY) - (1f - e) * (h + 14);
        long pos = preview && Spotify.track == null ? (now / 10) % 200_000 : Spotify.position();
        draw(g, mc.font, c.npStyle, cx, top, k, e, t, now, now - changedAt, pos, playing, dt, c.npLyric ? currentLyric() : "", AlbumArt.ready && !(preview && Spotify.track == null), AlbumArt.ready ? AlbumArt.colour : 0x1ED760);
    }
    static int width(int style) { return style == MINI ? 150 : style == VINYL ? 214 : 224; }
    static int height(int style, boolean lyric) { return style == MINI ? 20 : style == VINYL ? 36 : lyric ? 54 : 46; }

    /** Draws one look. cx/top: the middle of its top edge; e: 0..1 how far it has slid in; since: ms since the song changed. */
    static void draw(GuiGraphicsExtractor g, Font font, int style, float cx, float top, float k, float e, Spotify.Track t, long now, long since,
                     long pos, boolean playing, float dt, String lyric, boolean art, int acc) {
        int w = width(style), h = height(style, !lyric.isEmpty());
        bx0 = Math.round(cx - w / 2f * k); by0 = Math.round(top); bx1 = Math.round(cx + w / 2f * k); by1 = Math.round(top + h * k);
        Gfx.push(g); Gfx.translate(g, cx - w / 2f * k, top); Gfx.scale(g, k);
        if (style == VINYL) vinyl(g, font, w, h, e, t, now, since, pos, playing, dt, lyric, art, acc);
        else if (style == MINI) mini(g, font, w, h, e, t, now, since, pos, playing, art, acc);
        else glass(g, font, w, h, e, t, now, since, pos, playing, lyric, art, acc);
        Gfx.pop(g);
    }

    // ------------------------------------------------------------------ glass
    private static void glass(GuiGraphicsExtractor g, Font font, int w, int h, float e, Spotify.Track t, long now, long since, long pos, boolean playing, String lyric, boolean art, int acc) {
        int a = Math.round(255 * e), panel = 0x0D0C14;
        NebulaTex.glow(g, w / 2f, h / 2f + 3, w * 0.56f, h * 0.95f, Gfx.argb(Math.round(110 * e), 0x000000));          // its shadow
        round(g, 0, 0, w, h, Gfx.argb(Math.round(232 * e), panel));
        for (int x = 2; x < w - 2; x += 3) { float u = x / (float) w, wash = (1f - u) * (1f - u); g.fill(x, 1, Math.min(w - 2, x + 3), h - 1, Gfx.argb(Math.round(62 * e * wash), acc)); }   // the album's colour, washing in from the left
        float drift = (float) Math.sin(now / 3200.0), drift2 = (float) Math.cos(now / 4100.0);   // two soft lights of the album's colour drifting slowly inside the glass
        NebulaTex.glow(g, w * (0.45f + 0.2f * drift), h * 0.55f, w * 0.3f, h * 0.7f, Gfx.argb(Math.round(26 * e), acc));
        NebulaTex.glow(g, w * (0.75f + 0.15f * drift2), h * 0.4f, w * 0.22f, h * 0.6f, Gfx.argb(Math.round(18 * e), Ritual.mixc(acc, 0xFFFFFF, 0.4f)));
        float sheen = ((since + 2000) / 1000f) % 10f;                                                  // a sheen runs across the glass now and then
        if (sheen < 1.1f) { float u = sheen / 1.1f; NebulaTex.glow(g, -20 + (w + 40) * u, h / 2f, 10, h * 0.6f, Gfx.argb(Math.round(34 * e * (float) Math.sin(Math.PI * u)), 0xFFFFFF)); }
        g.fill(3, 0, w - 3, 1, Gfx.argb(Math.round(36 * e), 0xFFFFFF));                         // a lit top edge
        // the cover: flips in on a new song; paused, it dims under a pause sign
        int as = 34, ax = 6, ay = 6; float flip = Math.min(1f, since / 450f), sx = Math.abs((float) Math.cos(Math.PI * 0.5 * (1f - flip)));
        NebulaTex.glow(g, ax + as / 2f, ay + as / 2f, as * 0.95f, as * 0.95f, Gfx.argb(Math.round(95 * e), acc));
        Gfx.push(g); Gfx.translate(g, ax + as / 2f, ay); Gfx.scale(g, Math.max(0.02f, sx), 1f); Gfx.translate(g, -as / 2f, 0);
        cover(g, font, 0, 0, as, a, art, acc);
        for (int[] p : new int[][]{{0, 0}, {as - 1, 0}, {0, as - 1}, {as - 1, as - 1}}) g.fill(p[0], p[1], p[0] + 1, p[1] + 1, Gfx.argb(Math.round(232 * e), panel));   // rounded corners
        if (!playing) { g.fill(0, 0, as, as, Gfx.argb(Math.round(130 * e), 0x000000)); g.fill(as / 2 - 5, as / 2 - 6, as / 2 - 2, as / 2 + 6, Gfx.argb(a, 0xFFFFFF)); g.fill(as / 2 + 2, as / 2 - 6, as / 2 + 5, as / 2 + 6, Gfx.argb(a, 0xFFFFFF)); }
        Gfx.pop(g);
        // the words: they slide in with a new song
        float in = ease(Math.min(1f, since / 420f)), sl = (1f - in) * 10; int tx = 47, eqW = 30, tw = w - tx - eqW - 8;
        g.text(font, marquee(font, t.name(), tw, since), Math.round(tx + sl), 7, Gfx.argb(Math.round(a * in), 0xFFFFFF), true);
        small(g, font, cut(font, t.artists(), tw / 0.75f), tx + sl * 0.6f, 18, 0.75f, Gfx.argb(Math.round(a * 0.7f * in), 0xC9C6D8));
        if (!lyric.isEmpty()) small(g, font, cut(font, lyric, (w - tx - 8) / 0.75f), tx, 27.5f, 0.75f, Gfx.argb(Math.round(a * 0.95f), Ritual.mixc(acc, 0xFFFFFF, 0.45f)));
        // a little mirrored equalizer, top right
        int ex = w - eqW - 6, ey = 13;
        for (int i = 0; i < 8; i++) {
            float amp = playing ? 0.25f + 0.75f * Math.abs((float) Math.sin(now / (150.0 + i * 31) + i * 1.7) * (float) Math.sin(now / (370.0 + i * 47) + i * 0.6)) : 0.08f;
            int bh = Math.max(1, Math.round(6 * amp)), bx = ex + i * 4, col = Ritual.mixc(acc, 0xFFFFFF, i / 10f);
            g.fill(bx, ey - bh, bx + 2, ey, Gfx.argb(a, col)); g.fill(bx, ey, bx + 2, ey + Math.max(1, bh / 2), Gfx.argb(Math.round(a * 0.35f), col));
        }
        // time: elapsed, the bar, time left
        long dur = Math.max(1, t.durationMs()); float f = Math.min(1f, pos / (float) dur);
        int by = h - 8; String l = mmss(pos), r = "-" + mmss(Math.max(0, dur - pos));
        float lw = font.width(l) * 0.6f, rw = font.width(r) * 0.6f; int b0 = Math.round(tx + lw + 5), b1 = Math.round(w - 8 - rw - 5);
        small(g, font, l, tx, by - 2, 0.6f, Gfx.argb(Math.round(a * 0.7f), 0xFFFFFF));
        small(g, font, r, w - 8 - rw, by - 2, 0.6f, Gfx.argb(Math.round(a * 0.5f), 0xFFFFFF));
        g.fill(b0, by, b1, by + 2, Gfx.argb(Math.round(a * 0.16f), 0xFFFFFF));
        int fx = b0 + Math.round((b1 - b0) * f); g.fill(b0, by, fx, by + 2, Gfx.argb(a, acc));
        NebulaTex.glow(g, fx, by + 1, 5, 4, Gfx.argb(Math.round(a * 0.9f), Ritual.mixc(acc, 0xFFFFFF, 0.5f)));
        g.fill(fx - 1, by - 1, fx + 1, by + 3, Gfx.argb(a, 0xFFFFFF));
    }

    // ------------------------------------------------------------------ mini
    private static void mini(GuiGraphicsExtractor g, Font font, int w, int h, float e, Spotify.Track t, long now, long since, long pos, boolean playing, boolean art, int acc) {
        int a = Math.round(255 * e);
        NebulaTex.glow(g, w / 2f, h / 2f + 2, w * 0.55f, h, Gfx.argb(Math.round(90 * e), 0x000000));
        round(g, 0, 0, w, h, Gfx.argb(Math.round(225 * e), 0x0D0C14));
        g.fill(2, 1, 22, h - 1, Gfx.argb(Math.round(50 * e), acc));
        float flip = Math.min(1f, since / 450f), sx = Math.abs((float) Math.cos(Math.PI * 0.5 * (1f - flip)));
        Gfx.push(g); Gfx.translate(g, 10, 3); Gfx.scale(g, Math.max(0.02f, sx), 1f); Gfx.translate(g, -7, 0); cover(g, font, 0, 0, 14, a, art, acc);
        if (!playing) { g.fill(0, 0, 14, 14, Gfx.argb(Math.round(130 * e), 0)); g.fill(4, 4, 6, 10, Gfx.argb(a, 0xFFFFFF)); g.fill(8, 4, 10, 10, Gfx.argb(a, 0xFFFFFF)); }
        Gfx.pop(g);
        float in = ease(Math.min(1f, since / 420f));
        small(g, font, marquee(font, t.name() + "  ·  " + t.artists(), Math.round((w - 40) / 0.85f), since), 22 + (1f - in) * 8, 6, 0.85f, Gfx.argb(Math.round(a * in), 0xFFFFFF));
        for (int i = 0; i < 3; i++) { float amp = playing ? 0.3f + 0.7f * Math.abs((float) Math.sin(now / (160.0 + i * 41) + i * 2.1)) : 0.15f; int bh = Math.max(1, Math.round(8 * amp)); g.fill(w - 12 + i * 3, 14 - bh, w - 10 + i * 3, 14, Gfx.argb(a, acc)); }
        long dur = Math.max(1, t.durationMs()); float f = Math.min(1f, pos / (float) dur);
        g.fill(22, h - 3, w - 4, h - 2, Gfx.argb(Math.round(a * 0.15f), 0xFFFFFF)); g.fill(22, h - 3, 22 + Math.round((w - 26) * f), h - 2, Gfx.argb(a, acc));
    }

    // ------------------------------------------------------------------ vinyl
    private static float spin, spinV;
    private static void vinyl(GuiGraphicsExtractor g, Font font, int w, int h, float e, Spotify.Track t, long now, long since, long pos, boolean playing, float dt, String lyric, boolean art, int acc) {
        int a = Math.round(255 * e);
        spinV += ((playing ? 1.6f : 0f) + (since < 600 ? 6f : 0f) - spinV) * Math.min(1f, dt * 3f); spin += spinV * dt;
        float rr = 20, dcx = 21, dcy = h / 2f;
        NebulaTex.glow(g, dcx, dcy, 46, 30, Gfx.argb(Math.round(110 * e), acc));
        round(g, 18, 2, w - 18, h - 4, Gfx.argb(Math.round(228 * e), 0x0D0C14));
        g.fill(20, 2, w - 2, 3, Gfx.argb(Math.round(36 * e), 0xFFFFFF));
        NebulaTex.glow(g, 40, h / 2f, 50, h * 0.5f, Gfx.argb(Math.round(55 * e), acc));
        disc(g, dcx, dcy, rr, Gfx.argb(a, 0x060508));
        for (int i = 0; i < 4; i++) ringRound(g, dcx, dcy, rr - 2.5f - i * 2.2f, Gfx.argb(Math.round(a * 0.22f), 0x9A96A8));
        Gfx.push(g); Gfx.translate(g, dcx, dcy); Gfx.rotate(g, spin); cover(g, font, -7, -7, 14, a, art, acc);
        g.fill(Math.round(-rr + 3), 0, -9, 1, Gfx.argb(Math.round(a * 0.18f), 0xFFFFFF)); Gfx.pop(g);
        disc(g, dcx, dcy, 1.2f, Gfx.argb(a, 0x000000));
        NebulaTex.glow(g, dcx - rr * 0.45f, dcy - rr * 0.45f, rr * 0.5f, rr * 0.25f, Gfx.argb(Math.round(60 * e), 0xFFFFFF));
        float in = ease(Math.min(1f, since / 420f)), sl = (1f - in) * 8; int tx = 46, tw = w - tx - 34;
        g.text(font, marquee(font, t.name(), tw, since), Math.round(tx + sl), 6, Gfx.argb(Math.round(a * in), 0xFFFFFF), true);
        small(g, font, cut(font, t.artists(), tw / 0.7f), tx + sl, 16, 0.7f, Gfx.argb(Math.round(a * 0.6f * in), 0xC8C4D8));
        if (!lyric.isEmpty()) small(g, font, cut(font, lyric, (w - tx - 8) / 0.65f), tx, 23.5f, 0.65f, Gfx.argb(Math.round(a * 0.9f), Ritual.mixc(acc, 0xFFFFFF, 0.35f)));
        for (int i = 0; i < 5; i++) {
            float amp = playing ? 0.3f + 0.7f * Math.abs((float) Math.sin(now / (140.0 + i * 37) + i * 1.9) * (float) Math.sin(now / (330.0 + i * 51) + i)) : 0.12f;
            int bx = w - 26 + i * 4; float bh = 2 + 7 * amp; g.fill(bx, Math.round(14 - bh), bx + 2, 14, Gfx.argb(a, i % 2 == 0 ? acc : 0xFFFFFF));
        }
        long dur = Math.max(1, t.durationMs()); float f = Math.min(1f, pos / (float) dur);
        int b0 = tx, b1 = w - 6, by = h - 7;
        g.fill(b0, by, b1, by + 1, Gfx.argb(Math.round(a * 0.2f), 0xFFFFFF)); g.fill(b0, by, b0 + Math.round((b1 - b0) * f), by + 1, Gfx.argb(a, acc));
        NebulaTex.glow(g, b0 + (b1 - b0) * f, by + 0.5f, 3.5f, 3.5f, Gfx.argb(a, 0xFFFFFF));
        if (!playing) small(g, font, "PAUSED", w - 32, 4, 0.55f, Gfx.argb(Math.round(a * 0.85f), acc));
    }

    // ------------------------------------------------------------------ bits
    private static void cover(GuiGraphicsExtractor g, Font font, int x, int y, int s, int a, boolean art, int acc) {
        if (art) g.blit(RenderPipelines.GUI_TEXTURED, AlbumArt.ID, x, y, 0f, 0f, s, s, 64, 64, 64, 64, Gfx.argb(a, 0xFFFFFF));
        else { g.fill(x, y, x + s, y + s, Gfx.argb(a, Ritual.mixc(acc, 0x101018, 0.55f))); small(g, font, "♫", x + s / 2f - 3 * s / 20f, y + s / 2f - 4 * s / 20f, s / 20f, Gfx.argb(a, 0xFFFFFF)); }
    }
    private static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
        for (int i = -Math.round(r); i <= Math.round(r); i++) { float hw = (float) Math.sqrt(Math.max(0, r * r - i * i)); g.fill(Math.round(cx - hw), Math.round(cy + i), Math.round(cx + hw), Math.round(cy + i) + 1, c); }
    }
    private static void ringRound(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
        int n = Math.max(24, Math.round(r * 4));
        for (int i = 0; i < n; i++) { double an = i * Math.PI * 2 / n; int x = Math.round(cx + (float) Math.cos(an) * r), y = Math.round(cy + (float) Math.sin(an) * r); g.fill(x, y, x + 1, y + 1, c); }
    }
    private static String currentLyric() {
        if (preview && Spotify.track == null) return "the line being sung";
        if (Lyrics.lines.isEmpty() && Lyrics.status.startsWith("No synced")) return "♪ no lyrics for this song";
        int li = Lyrics.at(Spotify.position() + 200 + FancyConfig.get().lyricsOffset); return li >= 0 && li < Lyrics.lines.size() ? Lyrics.lines.get(li).text() : "";
    }
    private static java.lang.reflect.Field bossEvents; private static boolean bossTried; private static float bossPush;
    /** Boss bars come first: when the card sits where they are drawn (top, middle) it slides down below the last one. */
    private static float below(Minecraft mc, int W, float cx, float w, float y) {
        int n = 0;
        try {
            Object ov = mc.gui.hud.getBossOverlay();
            if (!bossTried) { bossTried = true; for (var f : ov.getClass().getDeclaredFields()) if (java.util.Map.class.isAssignableFrom(f.getType())) { f.setAccessible(true); bossEvents = f; } }
            if (bossEvents != null) n = ((java.util.Map<?, ?>) bossEvents.get(ov)).size();
        } catch (Throwable ignored) { }
        float want = 0;
        if (n > 0 && Math.abs(cx - W / 2f) < 91 + w / 2f) want = Math.max(0, Math.min(19 * n + 3, mc.getWindow().getGuiScaledHeight() / 3f + 8) - y);
        bossPush += (want - bossPush) * 0.15f;
        return y + bossPush;
    }
    private static void small(GuiGraphicsExtractor g, Font f, String s, float x, float y, float k, int c) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, k); g.text(f, s, 0, 0, c, false); Gfx.pop(g); }
    private static String cut(Font f, String s, float maxW) { return f.width(s) <= maxW ? s : f.plainSubstrByWidth(s, Math.round(maxW) - f.width("…")) + "…"; }
    /** A long title scrolls a letter at a time, pausing at both ends. */
    private static String marquee(Font font, String title, int maxW, long since) {
        if (font.width(title) <= maxW) return title;
        int extra = 0; while (extra < title.length() && font.width(title.substring(extra)) > maxW) extra++;
        int per = extra + 12, u = (int) ((since / 220L) % (per * 2));
        int off = u < 6 ? 0 : u < per ? Math.min(extra, u - 6) : u < per + 6 ? extra : Math.max(0, extra - (u - per - 6));
        return font.plainSubstrByWidth(title.substring(off), maxW);
    }
    private static float ease(float v) { return 1f - (1f - v) * (1f - v) * (1f - v); }
    private static String mmss(long ms) { long s = ms / 1000; return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60; }
    static void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x + 2, y, x + w - 2, y + 1, c); g.fill(x + 1, y + 1, x + w - 1, y + 2, c); g.fill(x, y + 2, x + w, y + h - 2, c); g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, c); g.fill(x + 2, y + h - 1, x + w - 2, y + h, c);
    }
}
