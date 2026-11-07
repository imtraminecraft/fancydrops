package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Now Playing. Two looks (Visuals > Now Playing), placed wherever you drag it (Visuals > Move HUD):
 *
 *   VINYL (default): a record spinning half out of a dark glass bar, the album cover as its label; the bar holds the song, the artists,
 *     the line being sung, a small equalizer and the time. The record slows to a stop when you pause and whirls when a song changes.
 *   CARD: a dark glass card with the cover, title, artists, a dancing equalizer and the time bar.
 *
 * It slides away when nothing is playing and while an RNG animation has its bars down.
 */
final class NowPlayingHud {
    private NowPlayingHud() {}

    private static float show; private static long last = System.currentTimeMillis();
    private static String lastId = ""; private static long changedAt, wakeAt; private static boolean lastPlaying;
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
        if (playing != lastPlaying) { lastPlaying = playing; wakeAt = now; }
        boolean want = (preview || (c.npOn && Spotify.linked() && t != null && (Spotify.playing || now - pausedSince < 8000) && !AnimationManager.hidesHud() && mc.level != null));
        if (!Spotify.playing && lastPlayingSeen) pausedSince = now; lastPlayingSeen = Spotify.playing;
        show += ((want ? 1f : 0f) - show) * Math.min(1f, dt * 8f);
        if (show < 0.01f || t == null) return;
        if (!t.id().equals(lastId)) { lastId = t.id(); changedAt = now; wakeAt = now; }
        if (c.npStyle == 1) card(g, mc, c, t, now, playing); else vinyl(g, mc, c, t, now, playing, dt);
    }
    private static boolean lastPlayingSeen; private static long pausedSince;

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

    // ------------------------------------------------------------------ the vinyl
    private static float spin, spinV;
    /** A record spinning half out of a dark glass bar: the cover is its label. The bar holds the song, the artists, the line being sung and
     *  the time. A soft light in the cover's colour sits behind the record; now and then a gleam runs along the bar. */
    private static void vinyl(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, Spotify.Track t, long now, boolean playing, float dt) {
        Font font = mc.font; int W = g.guiWidth();
        float k = c.npScale / 100f, e = show * show * (3 - 2 * show);
        int w = 214, h = 36, acc = AlbumArt.ready ? AlbumArt.colour : 0x1ED760, a = Math.round(255 * e);
        float cx = W * c.npX, top = below(mc, W, cx, w * k, c.npY) - (1f - e) * (h + 14);
        spinV += ((playing ? 1.6f : 0f) + (now - changedAt < 600 ? 6f : 0f) - spinV) * Math.min(1f, dt * 3f); spin += spinV * dt;
        Gfx.push(g); Gfx.translate(g, cx - w / 2f * k, top); Gfx.scale(g, k);
        bx0 = Math.round(cx - w / 2f * k); by0 = Math.round(top); bx1 = Math.round(cx + w / 2f * k); by1 = Math.round(top + h * k);
        float rr = 20, dcx = 21, dcy = h / 2f;
        NebulaTex.glow(g, dcx, dcy, 46, 30, Gfx.argb(Math.round(110 * e), acc));                    // the cover's light behind the record
        round(g, 18, 2, w - 18, h - 4, Gfx.argb(Math.round(225 * e), 0x09080D));                // the bar
        g.fill(20, 2, w - 2, 3, Gfx.argb(Math.round(40 * e), 0xFFFFFF));
        NebulaTex.glow(g, 40, h / 2f, 50, h * 0.5f, Gfx.argb(Math.round(55 * e), acc));
        float gleam = ((now - changedAt) / 1000f) % 9f;                                           // a gleam runs along the bar now and then
        if (gleam < 1.2f) NebulaTex.glow(g, 30 + (w - 30) * (gleam / 1.2f), h / 2f, 14, h * 0.45f, Gfx.argb(Math.round(40 * e * (float) Math.sin(Math.PI * gleam / 1.2f)), 0xFFFFFF));
        // the record: grooves, the cover as its label (turning), the hole, a fixed shine
        disc(g, dcx, dcy, rr, Gfx.argb(a, 0x060508));
        for (int i = 0; i < 4; i++) ringRound(g, dcx, dcy, rr - 2.5f - i * 2.2f, 1f, Gfx.argb(Math.round(a * 0.22f), 0x9A96A8));
        Gfx.push(g); Gfx.translate(g, dcx, dcy); Gfx.rotate(g, spin); int ls = 14;
        if (AlbumArt.ready && !preview) g.blit(RenderPipelines.GUI_TEXTURED, AlbumArt.ID, -ls / 2, -ls / 2, 0f, 0f, ls, ls, 64, 64, 64, 64, Gfx.argb(a, 0xFFFFFF));
        else g.fill(-ls / 2, -ls / 2, ls / 2, ls / 2, Gfx.argb(a, acc));
        g.fill(Math.round(-rr + 3), 0, -ls / 2 - 2, 1, Gfx.argb(Math.round(a * 0.18f), 0xFFFFFF));          // a line on the record that shows it turning
        Gfx.pop(g);
        disc(g, dcx, dcy, 1.2f, Gfx.argb(a, 0x000000));
        NebulaTex.glow(g, dcx - rr * 0.45f, dcy - rr * 0.45f, rr * 0.5f, rr * 0.25f, Gfx.argb(Math.round(60 * e), 0xFFFFFF));
        // the words
        float since = Math.min(1f, (now - changedAt) / 500f), sl = (1f - since) * 8;
        int tx = 46, tw = w - tx - 8;
        String title = marquee(font, t.name(), tw - 30, now);
        g.text(font, title, Math.round(tx + sl), 6, Gfx.argb(Math.round(a * since), 0xFFFFFF), false);
        small(g, font, font.plainSubstrByWidth(t.artists(), Math.round((tw - 30) / 0.7f)), tx + sl, 16, 0.7f, Gfx.argb(Math.round(a * 0.6f * since), 0xC8C4D8));
        String lyr = c.npLyric ? currentLyric() : "";
        if (!lyr.isEmpty()) small(g, font, font.plainSubstrByWidth(lyr, Math.round(tw / 0.65f)), tx, 23.5f, 0.65f, Gfx.argb(Math.round(a * 0.9f), Ritual.mixc(acc, 0xFFFFFF, 0.35f)));
        for (int i = 0; i < 5; i++) {                                                             // a little equalizer by the title
            float amp = playing ? 0.3f + 0.7f * Math.abs((float) Math.sin(now / (140.0 + i * 37) + i * 1.9) * (float) Math.sin(now / (330.0 + i * 51) + i)) : 0.12f;
            int bx = w - 26 + i * 4; float bh = 2 + 7 * amp; g.fill(bx, Math.round(14 - bh), bx + 2, 14, Gfx.argb(a, i % 2 == 0 ? acc : 0xFFFFFF));
        }
        long pos = preview ? (now / 10) % 200_000 : Spotify.position(), dur = Math.max(1, t.durationMs()); float f = Math.min(1f, pos / (float) dur);
        int b0 = tx, b1 = w - 6, by = h - 6;
        g.fill(b0, by, b1, by + 1, Gfx.argb(Math.round(a * 0.2f), 0xFFFFFF)); g.fill(b0, by, b0 + Math.round((b1 - b0) * f), by + 1, Gfx.argb(a, acc));
        NebulaTex.glow(g, b0 + (b1 - b0) * f, by + 0.5f, 3.5f, 3.5f, Gfx.argb(a, 0xFFFFFF));
        String tm = mmss(pos); small(g, font, tm, w - 6 - font.width(tm) * 0.55f, 17, 0.55f, Gfx.argb(Math.round(a * 0.55f), 0xFFFFFF));
        if (!playing) small(g, font, "PAUSED", w - 32, 4, 0.55f, Gfx.argb(Math.round(a * 0.85f), acc));
        Gfx.pop(g);
    }
    private static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
        for (int i = -Math.round(r); i <= Math.round(r); i++) { float hw = (float) Math.sqrt(Math.max(0, r * r - i * i)); g.fill(Math.round(cx - hw), Math.round(cy + i), Math.round(cx + hw), Math.round(cy + i) + 1, c); }
    }
    private static void ringRound(GuiGraphicsExtractor g, float cx, float cy, float r, float f, int c) {
        int n = Math.max(24, Math.round(r * 4));
        for (int i = 0; i < n * f; i++) { double an = -Math.PI / 2 + i * Math.PI * 2 / n; g.fill(Math.round(cx + (float) Math.cos(an) * r), Math.round(cy + (float) Math.sin(an) * r), Math.round(cx + (float) Math.cos(an) * r) + 1, Math.round(cy + (float) Math.sin(an) * r) + 1, c); }
    }
    private static String currentLyric() {
        if (preview && Spotify.track == null) return "the line being sung";
        if (Lyrics.lines.isEmpty() && Lyrics.status.startsWith("No synced")) return "\u266A no lyrics for this song";
        int li = Lyrics.at(Spotify.position() + 200); return li >= 0 && li < Lyrics.lines.size() ? Lyrics.lines.get(li).text() : "";
    }

    // ------------------------------------------------------------------ the card
    private static void card(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, Spotify.Track t, long now, boolean playing) {
        Font font = mc.font; int W = g.guiWidth();
        float k = c.npScale / 100f, e = show * show * (3 - 2 * show);
        int w = 176, h = 40; int a = Math.round(255 * e), acc = AlbumArt.ready ? AlbumArt.colour : 0x1ED760;
        float cx = W * c.npX, top = below(mc, W, cx, w * k, c.npY) - (1f - e) * (h + 12);
        Gfx.push(g); Gfx.translate(g, cx, top); Gfx.scale(g, k);
        bx0 = Math.round(cx - w / 2f * k); by0 = Math.round(top); bx1 = Math.round(cx + w / 2f * k); by1 = Math.round(top + h * k);
        int x0 = -w / 2;
        NebulaTex.glow(g, 0, h / 2f, w * 0.6f, h * 0.85f, Gfx.argb(Math.round(60 * e), acc));
        round(g, x0, 0, w, h, Gfx.argb(Math.round(215 * e), 0x0B0A10));
        g.fill(x0 + 3, 0, x0 + w - 3, 1, Gfx.argb(Math.round(110 * e), acc));
        int ax = x0 + 5, ay = 5, as = 30;
        if (AlbumArt.ready && !preview) g.blit(RenderPipelines.GUI_TEXTURED, AlbumArt.ID, ax, ay, 0f, 0f, as, as, 64, 64, 64, 64, Gfx.argb(a, 0xFFFFFF));
        else { g.fill(ax, ay, ax + as, ay + as, Gfx.argb(a, 0x1E1C26)); g.text(font, "♫", ax + 11, ay + 11, Gfx.argb(a, acc), false); }
        int tx = ax + as + 7, tw = w - (tx - x0) - 30;
        g.text(font, marquee(font, t.name(), tw, now), tx, 7, Gfx.argb(a, 0xFFFFFF), false);
        small(g, font, font.plainSubstrByWidth(t.artists(), Math.round(tw / 0.75f)), tx, 18, 0.75f, Gfx.argb(Math.round(a * 0.7f), 0xC8C4D8));
        for (int i = 0; i < 5; i++) {
            float hgt = playing ? 3 + 9 * Math.abs((float) Math.sin(now / (130.0 + i * 37) + i * 1.9) * (float) Math.sin(now / (310.0 + i * 53) + i)) : 2f;
            int bxx = x0 + w - 24 + i * 4, byy = 20; g.fill(bxx, Math.round(byy - hgt), bxx + 2, byy, Gfx.argb(a, i % 2 == 0 ? acc : 0xFFFFFF));
        }
        long pos = preview ? (now / 10) % 200_000 : Spotify.position(), dur = Math.max(1, t.durationMs()); float f = Math.min(1f, pos / (float) dur);
        int b0 = tx, b1 = x0 + w - 7, by = h - 7;
        g.fill(b0, by, b1, by + 1, Gfx.argb(Math.round(a * 0.25f), 0xFFFFFF)); g.fill(b0, by, b0 + Math.round((b1 - b0) * f), by + 1, Gfx.argb(a, acc));
        NebulaTex.glow(g, b0 + (b1 - b0) * f, by + 0.5f, 3, 3, Gfx.argb(a, 0xFFFFFF));
        String tm = mmss(pos) + " / " + mmss(dur); small(g, font, tm, b1 - font.width(tm) * 0.6f, by - 7, 0.6f, Gfx.argb(Math.round(a * 0.6f), 0xFFFFFF));
        if (!playing) small(g, font, "PAUSED", x0 + w - 28, 25, 0.6f, Gfx.argb(Math.round(a * 0.8f), acc));
        Gfx.pop(g);
    }

    // ------------------------------------------------------------------ bits
    private static void small(GuiGraphicsExtractor g, Font f, String s, float x, float y, float k, int c) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, k); g.text(f, s, 0, 0, c, false); Gfx.pop(g); }
    /** A long title scrolls a letter at a time, pausing at both ends. */
    private static String marquee(Font font, String title, int maxW, long now) {
        if (font.width(title) <= maxW) return title;
        int extra = 0; while (extra < title.length() && font.width(title.substring(extra)) > maxW) extra++;
        int per = extra + 12, u = (int) (((now - changedAt) / 220L) % (per * 2));
        int off = u < 6 ? 0 : u < per ? Math.min(extra, u - 6) : u < per + 6 ? extra : Math.max(0, extra - (u - per - 6));
        return font.plainSubstrByWidth(title.substring(off), maxW);
    }
    private static String mmss(long ms) { long s = ms / 1000; return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60; }
    static void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x + 2, y, x + w - 2, y + 1, c); g.fill(x + 1, y + 1, x + w - 1, y + 2, c); g.fill(x, y + 2, x + w, y + h - 2, c); g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, c); g.fill(x + 2, y + h - 1, x + w - 2, y + h, c);
    }
}
