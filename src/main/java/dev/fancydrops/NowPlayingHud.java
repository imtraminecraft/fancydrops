package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Now Playing, at the top middle of the screen: the album cover, the song (scrolling when it is long), the artists, a little equalizer that
 * dances while it plays, and the time bar. Tinted with the cover's colour. Slides in when a song starts, out when nothing plays, and out of the
 * way while an RNG animation has its bars down.
 */
final class NowPlayingHud {
    private NowPlayingHud() {}

    private static float show; private static long last = System.currentTimeMillis();
    private static String lastId = ""; private static long changedAt;

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis(); float dt = Math.min(0.1f, (now - last) / 1000f); last = now;
        Spotify.Track t = Spotify.track;
        boolean want = c.npOn && Spotify.linked() && t != null && (Spotify.playing || now - pausedAt() < 8000) && !AnimationManager.hidesHud() && mc.level != null;
        show += ((want ? 1f : 0f) - show) * Math.min(1f, dt * 8f);
        if (show < 0.01f || t == null) return;
        if (!t.id().equals(lastId)) { lastId = t.id(); changedAt = now; }
        Font font = mc.font; int W = g.guiWidth();
        float k = c.npScale / 100f, ease = show * show * (3 - 2 * show);
        int w = 168, h = 34, x0 = -w / 2, y0 = 4;
        int acc = AlbumArt.ready ? AlbumArt.colour : 0x1ED760;
        Gfx.push(g); Gfx.translate(g, W / 2f, (y0 - (1f - ease) * (h + 10)) * 1f); Gfx.scale(g, k); Gfx.translate(g, 0, 0);
        int a = Math.round(255 * ease);
        // the card: a dark glass panel, rounded, a soft glow of the cover's colour behind it
        NebulaTex.glow(g, 0, h / 2f, w * 0.62f, h * 0.9f, Gfx.argb(Math.round(70 * ease), acc));
        round(g, x0, 0, w, h, Gfx.argb(Math.round(200 * ease), 0x0B0A10));
        round(g, x0, 0, w, 1, Gfx.argb(Math.round(90 * ease), acc));
        // the cover
        int ax = x0 + 4, ay = 4, as = 26;
        if (AlbumArt.ready) { g.blit(RenderPipelines.GUI_TEXTURED, AlbumArt.ID, ax, ay, 0f, 0f, as, as, 64, 64, 64, 64, Gfx.argb(a, 0xFFFFFF)); }
        else { g.fill(ax, ay, ax + as, ay + as, Gfx.argb(a, 0x1E1C26)); g.text(font, "♫", ax + 9, ay + 9, Gfx.argb(a, acc), false); }
        // the song and the artists (the title scrolls when it does not fit)
        int tx = ax + as + 6, tw = w - (tx - x0) - 30;
        String title = t.name(); int tW = font.width(title);
        int off = 0;                                                                         // too long: it scrolls a letter at a time, pausing at both ends
        if (tW > tw) { int extra = 0; while (extra < title.length() && font.width(title.substring(extra)) > tw) extra++; int per = extra + 12; int u = (int) (((now - changedAt) / 220L) % (per * 2)); off = u < 6 ? 0 : u < per - 6 + 6 ? Math.min(extra, u - 6) : u < per + 6 ? extra : Math.max(0, extra - (u - per - 6)); }
        String shown = font.plainSubstrByWidth(title.substring(off), tw);
        g.text(font, shown, tx, 6, Gfx.argb(a, 0xFFFFFF), false);
        Gfx.push(g); Gfx.translate(g, tx, 17); Gfx.scale(g, 0.75f); g.text(font, font.plainSubstrByWidth(t.artists(), Math.round(tw / 0.75f)), 0, 0, Gfx.argb(Math.round(a * 0.7f), 0xC8C4D8), false); Gfx.pop(g);
        // the equalizer: five bars that dance while it plays and settle when paused
        for (int i = 0; i < 5; i++) {
            float hgt = Spotify.playing ? 3 + 9 * (0.5f + 0.5f * (float) Math.sin(now / (130.0 + i * 37) + i * 1.9) * (float) Math.sin(now / (310.0 + i * 53) + i)) : 2f;
            int bx = x0 + w - 24 + i * 4, by = 18;
            g.fill(bx, Math.round(by - Math.abs(hgt)), bx + 2, by, Gfx.argb(a, i % 2 == 0 ? acc : 0xFFFFFF));
        }
        // the time bar
        long pos = Spotify.position(), dur = Math.max(1, t.durationMs()); float f = Math.min(1f, pos / (float) dur);
        int bx0 = tx, bx1 = x0 + w - 6, by = h - 6;
        g.fill(bx0, by, bx1, by + 1, Gfx.argb(Math.round(a * 0.25f), 0xFFFFFF));
        g.fill(bx0, by, bx0 + Math.round((bx1 - bx0) * f), by + 1, Gfx.argb(a, acc));
        NebulaTex.glow(g, bx0 + (bx1 - bx0) * f, by + 0.5f, 3, 3, Gfx.argb(a, 0xFFFFFF));
        String tm = mmss(pos) + " / " + mmss(dur);
        Gfx.push(g); Gfx.translate(g, bx1 - font.width(tm) * 0.6f, by - 7); Gfx.scale(g, 0.6f); g.text(font, tm, 0, 0, Gfx.argb(Math.round(a * 0.6f), 0xFFFFFF), false); Gfx.pop(g);
        if (!Spotify.playing) { Gfx.push(g); Gfx.translate(g, x0 + w - 26, 24); Gfx.scale(g, 0.6f); g.text(font, "PAUSED", 0, 0, Gfx.argb(Math.round(a * 0.8f), acc), false); Gfx.pop(g); }
        Gfx.pop(g);
    }
    private static long pausedSince; private static boolean wasPlaying;
    private static long pausedAt() { boolean p = Spotify.playing; if (wasPlaying && !p) pausedSince = System.currentTimeMillis(); wasPlaying = p; return p ? System.currentTimeMillis() : pausedSince; }
    private static String mmss(long ms) { long s = ms / 1000; return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60; }
    /** A filled box with its four corners rounded by one pixel. */
    static void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        if (h <= 2) { g.fill(x + 1, y, x + w - 1, y + h, c); return; }
        g.fill(x + 1, y, x + w - 1, y + 1, c); g.fill(x, y + 1, x + w, y + h - 1, c); g.fill(x + 1, y + h - 1, x + w - 1, y + h, c);
    }
}
