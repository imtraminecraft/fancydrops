package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Now Playing (Visuals > Now Playing), placed wherever you drag it (Visuals > Move HUD): one clean glass card. The cover on the left (it
 * flips in when the song changes and dims under a pause sign when you pause), the title and artists, optionally the line being sung,
 * and a thin time bar along the bottom with the time on either side. A small equalizer sits top right. It takes its accent from the
 * album (in soft, cool colours), slides away when nothing is playing, waits under boss bars and hides during RNG animations.
 */
final class NowPlayingHud {
    private NowPlayingHud() {}

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
        float cx = W * c.npX, h = height(c.npLyric) * k;
        float top = below(mc, W, cx, width() * k, c.npY) - (1f - e) * (h + 14);
        long pos = preview && Spotify.track == null ? (now / 10) % 200_000 : Spotify.position();
        draw(g, mc.font, cx, top, k, e, t, now, now - changedAt, pos, playing, dt, c.npLyric ? currentLyric() : "", AlbumArt.ready && !(preview && Spotify.track == null), AlbumArt.accent());
    }
    static int width() { return 210; }
    static int height(boolean lyric) { return lyric ? 52 : 44; }

    /** Draws the card. cx/top: the middle of its top edge; e: 0..1 how far it has slid in; since: ms since the song changed. */
    static void draw(GuiGraphicsExtractor g, Font font, float cx, float top, float k, float e, Spotify.Track t, long now, long since,
                     long pos, boolean playing, float dt, String lyric, boolean art, int acc) {
        int w = width(), h = height(!lyric.isEmpty());
        bx0 = Math.round(cx - w / 2f * k); by0 = Math.round(top); bx1 = Math.round(cx + w / 2f * k); by1 = Math.round(top + h * k);
        Gfx.push(g); Gfx.translate(g, cx - w / 2f * k, top); Gfx.scale(g, k);
        glass(g, font, w, h, e, t, now, since, pos, playing, lyric, art, acc);
        Gfx.pop(g);
    }

    private static void glass(GuiGraphicsExtractor g, Font font, int w, int h, float e, Spotify.Track t, long now, long since, long pos, boolean playing, String lyric, boolean art, int acc) {
        int a = Math.round(255 * e), panel = 0x0E1018;
        NebulaTex.glow(g, w / 2f, h / 2f + 4, w * 0.55f, h * 0.9f, Gfx.argb(Math.round(70 * e), 0x000000));            // a soft shadow
        round(g, 0, 0, w, h, Gfx.argb(Math.round(225 * e), panel));
        round(g, 0, 0, w, h / 2, Gfx.argb(Math.round(10 * e), 0xFFFFFF));                       // the glass: a touch lighter on top
        g.fill(3, 0, w - 3, 1, Gfx.argb(Math.round(28 * e), 0xFFFFFF));
        // the cover
        int as = 32, ax = 6, ay = 6; float flip = Math.min(1f, since / 400f), sx = Math.abs((float) Math.cos(Math.PI * 0.5 * (1f - flip)));
        Gfx.push(g); Gfx.translate(g, ax + as / 2f, ay); Gfx.scale(g, Math.max(0.02f, sx), 1f); Gfx.translate(g, -as / 2f, 0);
        cover(g, font, 0, 0, as, a, art, acc);
        for (int[] p : new int[][]{{0, 0}, {as - 1, 0}, {0, as - 1}, {as - 1, as - 1}}) g.fill(p[0], p[1], p[0] + 1, p[1] + 1, Gfx.argb(Math.round(225 * e), panel));
        if (!playing) { g.fill(0, 0, as, as, Gfx.argb(Math.round(120 * e), 0x000000)); g.fill(as / 2 - 4, as / 2 - 5, as / 2 - 2, as / 2 + 5, Gfx.argb(a, 0xFFFFFF)); g.fill(as / 2 + 2, as / 2 - 5, as / 2 + 4, as / 2 + 5, Gfx.argb(a, 0xFFFFFF)); }
        Gfx.pop(g);
        // the words
        float in = ease(Math.min(1f, since / 380f)), sl = (1f - in) * 8; int tx = 45, eqW = 14, tw = w - tx - eqW - 10;
        Gfx.txt(g, font, marquee(font, t.name(), tw, since), Math.round(tx + sl), 8, Gfx.argb(Math.round(a * in), 0xF2F4F8), false);
        small(g, font, cut(font, t.artists(), tw / 0.75f), tx + sl * 0.6f, 19, 0.75f, Gfx.argb(Math.round(a * 0.55f * in), 0xC6CAD6));
        if (!lyric.isEmpty()) small(g, font, cut(font, lyric, (w - tx - 8) / 0.75f), tx, 28, 0.75f, Gfx.argb(Math.round(a * 0.9f), Ritual.mixc(acc, 0xFFFFFF, 0.4f)));
        // a small equalizer, top right
        int ex = w - eqW - 6, ey = 15;
        for (int i = 0; i < 4; i++) {
            float amp = playing ? 0.25f + 0.75f * Math.abs((float) Math.sin(now / (160.0 + i * 37) + i * 1.7) * (float) Math.sin(now / (390.0 + i * 53) + i * 0.6)) : 0.1f;
            int bh = Math.max(1, Math.round(7 * amp)), bx = ex + i * 4;
            g.fill(bx, ey - bh, bx + 2, ey, Gfx.argb(Math.round(a * 0.85f), acc));
        }
        // time: elapsed, a thin bar, time left
        long dur = Math.max(1, t.durationMs()); float f = Math.min(1f, pos / (float) dur);
        int by = h - 8; String l = mmss(pos), r = mmss(dur);
        float lw = font.width(l) * 0.55f, rw = font.width(r) * 0.55f; int b0 = Math.round(tx + lw + 5), b1 = Math.round(w - 8 - rw - 5);
        small(g, font, l, tx, by - 1.5f, 0.55f, Gfx.argb(Math.round(a * 0.5f), 0xFFFFFF));
        small(g, font, r, w - 8 - rw, by - 1.5f, 0.55f, Gfx.argb(Math.round(a * 0.5f), 0xFFFFFF));
        g.fill(b0, by, b1, by + 1, Gfx.argb(Math.round(a * 0.14f), 0xFFFFFF));
        g.fill(b0, by, b0 + Math.round((b1 - b0) * f), by + 1, Gfx.argb(Math.round(a * 0.9f), acc));
    }

    // ------------------------------------------------------------------ bits
    private static void cover(GuiGraphicsExtractor g, Font font, int x, int y, int s, int a, boolean art, int acc) {
        if (art) g.blit(RenderPipelines.GUI_TEXTURED, AlbumArt.ID, x, y, 0f, 0f, s, s, 64, 64, 64, 64, Gfx.argb(a, 0xFFFFFF));
        else { g.fill(x, y, x + s, y + s, Gfx.argb(a, Ritual.mixc(acc, 0x101018, 0.55f))); small(g, font, "♫", x + s / 2f - 3 * s / 20f, y + s / 2f - 4 * s / 20f, s / 20f, Gfx.argb(a, 0xFFFFFF)); }
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
    private static void small(GuiGraphicsExtractor g, Font f, String s, float x, float y, float k, int c) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, k); Gfx.txt(g, f, s, 0, 0, c, false); Gfx.pop(g); }
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
