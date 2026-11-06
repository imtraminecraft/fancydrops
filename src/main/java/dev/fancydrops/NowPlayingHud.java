package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Now Playing. Two looks (Visuals > Now Playing), placed wherever you drag it (Visuals > Move HUD):
 *
 *   ISLAND (default): a black capsule at the top of the screen, like a phone's live activity, but ours. Small most of the time: a round
 *     cover wearing a ring that fills as the song plays, the title, and a waveform in the cover's colours. When a song starts, when you pause
 *     or play, and now and then on its own, it springs open: a bigger cover, title, artists, the current lyric line, the time bar.
 *   CARD: a dark glass card with the cover, title, artists, a dancing equalizer and the time bar.
 *
 * It slides away when nothing is playing and while an RNG animation has its bars down.
 */
final class NowPlayingHud {
    private NowPlayingHud() {}

    private static float show, open, wNow = 120, hNow = 20; private static long last = System.currentTimeMillis();
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
        boolean expanded = preview || now - wakeAt < 5500 || (c.npPeek && (now / 1000) % 45 < 4);
        open += ((expanded ? 1f : 0f) - open) * Math.min(1f, dt * 7f);
        if (c.npStyle == 1) card(g, mc, c, t, now, playing); else island(g, mc, c, t, now, playing, dt);
    }
    private static boolean lastPlayingSeen; private static long pausedSince;

    // ------------------------------------------------------------------ the island
    private static void island(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, Spotify.Track t, long now, boolean playing, float dt) {
        Font font = mc.font; int W = g.guiWidth();
        float k = c.npScale / 100f, e = show * show * (3 - 2 * show), o = open;
        float tw = 128 + 92 * o, th = 18 + 30 * o;                                             // the size it springs to
        wNow += (tw - wNow) * Math.min(1f, dt * 10f); hNow += (th - hNow) * Math.min(1f, dt * 10f);
        float w = wNow, h = hNow;
        int acc = AlbumArt.ready ? AlbumArt.colour : 0x1ED760, a = Math.round(255 * e);
        float cx = W * c.npX, top = c.npY - (1f - e) * (h + 12);
        Gfx.push(g); Gfx.translate(g, cx, top); Gfx.scale(g, k);
        bx0 = Math.round(cx - w / 2 * k); by0 = Math.round(top); bx1 = Math.round(cx + w / 2 * k); by1 = Math.round(top + h * k);
        // a halo in the cover's colour that breathes with the music, then the capsule itself
        float beat = playing ? 0.5f + 0.5f * (float) Math.sin(now / 240.0) * (float) Math.sin(now / 610.0) : 0f;
        NebulaTex.glow(g, 0, h / 2, w * 0.62f, h * (0.9f + 0.2f * beat), Gfx.argb(Math.round((50 + 40 * beat) * e), acc));
        capsule(g, -w / 2, 0, w, h, Gfx.argb(Math.round(245 * e), 0x050507));
        capsule(g, -w / 2, 0, w, 1, Gfx.argb(Math.round(40 * e), 0xFFFFFF));                // a sheen along the top edge
        float r = h / 2;
        // the cover: round when small, a rounded square when open; a ring round it fills with the song
        float as = 12 + 22 * o, ax = -w / 2 + 4 + 2 * o, ay = (h - as) / 2;
        long pos = preview ? (now / 10) % 200_000 : Spotify.position(), dur = Math.max(1, t.durationMs()); float f = Math.min(1f, pos / (float) dur);
        if (AlbumArt.ready && !preview) g.blit(RenderPipelines.GUI_TEXTURED, AlbumArt.ID, Math.round(ax), Math.round(ay), 0f, 0f, Math.round(as), Math.round(as), 64, 64, 64, 64, Gfx.argb(a, 0xFFFFFF));
        else { g.fill(Math.round(ax), Math.round(ay), Math.round(ax + as), Math.round(ay + as), Gfx.argb(a, 0x1E1C26)); g.text(font, "♫", Math.round(ax + as / 2 - 3), Math.round(ay + as / 2 - 4), Gfx.argb(a, acc), false); }
        if (o < 0.5f) ringRound(g, ax + as / 2, ay + as / 2, as / 2 + 1.5f, f, Gfx.argb(Math.round(a * (1 - o * 2)), acc));
        // the words
        float tx = ax + as + 5, maxW = w - (tx + w / 2) - 30;
        String title = marquee(font, t.name(), Math.round(maxW / (o > 0.5f ? 1f : 0.85f)), now);
        if (o > 0.5f) {
            float oa = (o - 0.5f) * 2;
            g.text(font, title, Math.round(tx), Math.round(ay + 1), Gfx.argb(Math.round(a * oa), 0xFFFFFF), false);
            small(g, font, font.plainSubstrByWidth(t.artists(), Math.round(maxW / 0.75f)), tx, ay + 11, 0.75f, Gfx.argb(Math.round(a * oa * 0.65f), 0xD0CCE0));
            String lyr = currentLyric();
            if (c.npLyric && !lyr.isEmpty()) small(g, font, font.plainSubstrByWidth("“" + lyr + "”", Math.round(maxW / 0.65f)), tx, ay + 19, 0.65f, Gfx.argb(Math.round(a * oa * 0.9f), acc));
            float bx = tx, bw = w / 2 - 8 - tx, by = h - 6;                                       // the time bar
            g.fill(Math.round(bx), Math.round(by), Math.round(bx + bw), Math.round(by) + 1, Gfx.argb(Math.round(a * oa * 0.22f), 0xFFFFFF));
            g.fill(Math.round(bx), Math.round(by), Math.round(bx + bw * f), Math.round(by) + 1, Gfx.argb(Math.round(a * oa), acc));
            NebulaTex.glow(g, bx + bw * f, by + 0.5f, 3, 3, Gfx.argb(Math.round(a * oa), 0xFFFFFF));
            String tm = mmss(pos) + "  " + mmss(dur); small(g, font, tm, w / 2 - 8 - font.width(tm) * 0.55f, by - 7, 0.55f, Gfx.argb(Math.round(a * oa * 0.55f), 0xFFFFFF));
        } else {
            float oa = 1 - o * 2;
            small(g, font, title, tx, h / 2 - 3.4f, 0.85f, Gfx.argb(Math.round(a * oa), 0xFFFFFF));
        }
        // the waveform on the right: bars in the cover's colour that follow a made-up beat (still when paused)
        int n = 6; float wx = w / 2 - r - 14, wy = (o > 0.5f ? ay + 6 : h / 2);
        for (int i = 0; i < n; i++) {
            float amp = playing ? 0.35f + 0.65f * Math.abs((float) Math.sin(now / (150.0 + i * 41) + i * 1.7) * (float) Math.sin(now / (380.0 + i * 29) + i)) : 0.15f;
            float bh = 2 + 8 * amp; int col = i % 2 == 0 ? acc : Ritual.mixc(acc, 0xFFFFFF, 0.5f);
            g.fill(Math.round(wx + i * 2.6f), Math.round(wy - bh / 2), Math.round(wx + i * 2.6f) + 1, Math.round(wy + bh / 2), Gfx.argb(a, col));
        }
        if (!playing) small(g, font, "II", w / 2 - r - 4, h / 2 - 3, 0.7f, Gfx.argb(a, 0xFFFFFF));
        Gfx.pop(g);
    }
    /** A capsule: a box whose ends are half circles. */
    private static void capsule(GuiGraphicsExtractor g, float x, float y, float w, float h, int c) {
        int r = Math.max(1, Math.round(h / 2));
        if (h <= 2) { g.fill(Math.round(x + r), Math.round(y), Math.round(x + w - r), Math.round(y + h), c); return; }
        for (int i = 0; i < Math.round(h); i++) {
            float dy = i + 0.5f - h / 2f, inset = r - (float) Math.sqrt(Math.max(0, r * r - dy * dy));
            g.fill(Math.round(x + inset), Math.round(y + i), Math.round(x + w - inset), Math.round(y + i + 1), c);
        }
    }
    private static void ringRound(GuiGraphicsExtractor g, float cx, float cy, float r, float f, int c) {
        int n = 40;
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
        float cx = W * c.npX, top = c.npY - (1f - e) * (h + 12);
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
