package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * The lyrics (Visuals > Lyrics): light, readable words on your screen, a little below the crosshair (Move them to put them anywhere).
 * No panels, no world signs: just the words with a soft shadow, so they cost almost nothing.
 *
 *   POP (default): the words of the line pop in one by one as they are sung, each with a little spring (and a few stars, if Star bursts
 *     is on); the word being sung is in the album's colour, the ones before it settle to white. The old line drifts up and away.
 *   KARAOKE: the whole line is there at once, dim, and fills with the album's colour word by word; the last line fades above it and the
 *     next one waits faintly below.
 *
 * Long lines wrap onto a second row (and shrink a little if they still don't fit). The words are timed from the line's start: they are
 * spread over how long the line would take to sing (about 85 ms a letter), not over the gap until the next line, so a line followed by
 * a long instrumental doesn't crawl. Everything steps aside while a drop animation plays.
 */
final class LyricsHud {
    private LyricsHud() {}

    static final int POP = 0, KARAOKE = 1;

    /** The colour of the sung words: from the album cover, made light enough to read. */
    static int colour() { return Ritual.mixc(AlbumArt.accent(), 0xFFFFFF, 0.25f); }

    static boolean preview;
    static int bx0, by0, bx1, by1;
    private static int lastLine = -2, prevLine = -2; private static String lastTrack = "";
    private static long lineAt, titleAt, lastFrame;
    private static float away;

    /** From the HUD (not while the editor draws its own). */
    static void hud(GuiGraphicsExtractor g) { if (!preview) render(g); }

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - (lastFrame == 0 ? now : lastFrame)) / 1000f); lastFrame = now;
        if (!preview && (!c.lyricsOn || mc.level == null)) { bx0 = bx1 = 0; return; }
        away += ((AnimationManager.isPlaying() && !preview ? 1f : 0f) - away) * Math.min(1f, dt * 8f);
        float vis = 1f - away; if (vis < 0.02f) return;
        Spotify.Track t = Spotify.track;
        boolean on = t != null && Spotify.playing;
        List<Lyrics.Line> L = Lyrics.lines;
        boolean demo = preview && (!on || L.isEmpty());
        if (demo) { L = DEMO; on = true; }
        if (!demo && on && !t.id().equals(lastTrack)) { lastTrack = t.id(); lastLine = -2; prevLine = -2; titleAt = now; }
        long pos = demo ? now % 9000 : Spotify.position() + c.lyricsOffset;
        draw(g, mc.font, c, L, on, demo, pos, now, vis);
    }
    static void draw(GuiGraphicsExtractor g, Font f, FancyConfig c, List<Lyrics.Line> L, boolean on, boolean demo, long pos, long now, float vis) {
        int W = g.guiWidth(), H = g.guiHeight(); float k = c.lyricsSize / 100f, cx = W * c.lineX, cy = H * c.lyY;
        if (!demo && c.lyricsIntro) titleCard(g, f, W, H, now, vis, k);
        if (!demo && on && L.isEmpty() && Lyrics.status.startsWith("No synced")) { noLyrics(g, f, now, vis, cx, cy, k); return; }
        if (L.isEmpty() || !on) { bx0 = bx1 = 0; return; }
        int li = at(L, pos);
        if (li != lastLine) { prevLine = lastLine; lastLine = li; lineAt = now; }
        float since = (now - lineAt) / 1000f, out = Math.min(1f, since / 0.35f);
        int maxW = Math.round(Math.min(W * 0.72f, 360f) / k);
        bx0 = Math.round(cx - maxW / 2f * k); bx1 = Math.round(cx + maxW / 2f * k); by0 = Math.round(cy - 6 * k); by1 = Math.round(cy + 22 * k);
        if (c.lyricsStyle == KARAOKE) {
            if (prevLine >= 0 && prevLine < L.size() && out < 1f) block(g, f, L, prevLine, pos, cx, cy - 10 * k * out, k, (1f - out) * vis, maxW, false, c);
            else if (li > 0 && li - 1 < L.size()) faint(g, f, L.get(li - 1).text(), cx, cy - 13 * k, k * 0.75f, 0.35f * vis, maxW);
            if (li >= 0 && li < L.size()) block(g, f, L, li, pos, cx, cy + 4 * k * (1f - ease(out)), k, ease(out) * vis, maxW, false, c);
            if (c.lyricsNext && li + 1 < L.size()) faint(g, f, L.get(li + 1).text(), cx, cy + 15 * k, k * 0.75f, 0.4f * vis, maxW);
        } else {
            if (prevLine >= 0 && prevLine < L.size() && out < 1f) block(g, f, L, prevLine, Long.MAX_VALUE / 4, cx, cy - 14 * k * ease(out), k, (1f - out) * vis, maxW, true, c);
            if (li >= 0 && li < L.size()) block(g, f, L, li, pos, cx, cy, k, vis, maxW, true, c);
            if (c.lyricsNext && li + 1 < L.size()) faint(g, f, L.get(li + 1).text(), cx, cy + 15 * k, k * 0.7f, 0.3f * vis, maxW);
        }
    }
    private static int at(List<Lyrics.Line> l, long ms) { int i = -1; for (int k = 0; k < l.size(); k++) { if (l.get(k).atMs() <= ms) i = k; else break; } return i; }
    private static final List<Lyrics.Line> DEMO = List.of(new Lyrics.Line(0, "The words pop in as they're sung"), new Lyrics.Line(3000, "light and easy to read"), new Lyrics.Line(6000, "drag them anywhere you like"));

    /** When the letter at offset ch (of total) is sung: spread over how long the line takes to sing, not the whole gap. */
    private static long wordAt(long start, long end, int ch, int total) {
        long span = Math.max(300, Math.min((long) ((end - start) * 0.85f), total * 85L + 250));
        return start + span * ch / Math.max(1, total);
    }

    /** One line, wrapped to at most two rows. pop: words appear as sung; otherwise all show, filling with colour. */
    private static void block(GuiGraphicsExtractor g, Font f, List<Lyrics.Line> L, int li, long pos, float cx, float cy, float k, float a, int maxW, boolean pop, FancyConfig c) {
        if (a < 0.02f) return;
        String text = L.get(li).text(); if (text.isBlank()) return;
        long start = L.get(li).atMs(), end = li + 1 < L.size() ? L.get(li + 1).atMs() : start + 5000;
        List<String> rows = wrap(f, text, maxW); float kk = k;
        int widest = 0; for (String r : rows) widest = Math.max(widest, f.width(r));
        if (widest > maxW) kk = k * maxW / (float) widest;
        int acc = colour(), total = text.length(), ch = 0;
        float rowH = 11 * kk, y0 = cy - (rows.size() - 1) * rowH / 2f;
        NebulaTex.glow(g, cx, cy + 4 * kk, widest * kk * 0.55f + 10, 9 * kk + rows.size() * 5 * kk, Gfx.argb(Math.round(70 * a), 0x000000));   // a soft shade behind the words, nothing more
        for (String row : rows) {
            float x = cx - f.width(row) * kk / 2f, y = y0;
            for (String w : row.split(" ")) {
                if (w.isEmpty()) { x += f.width(" ") * kk; ch++; continue; }
                long wa = wordAt(start, end, ch, total), nx = wordAt(start, end, ch + w.length() + 1, total);
                float u = (pos - wa) / 1000f, ww = f.width(w) * kk; boolean singing = pos >= wa && pos < nx;
                if (pop) {
                    if (u >= 0f) {
                        float p = Math.min(1f, u / 0.22f), s = p < 1f ? 0.6f + 0.4f * spring(p) : 1f;
                        int col = singing ? acc : Ritual.mixc(acc, 0xFFFFFF, Math.min(1f, (pos - nx) / 600f + 0.5f));
                        word(g, f, w, x + ww / 2f, y + (1f - p) * 4 * kk, kk * s, Gfx.argb(Math.round(255 * a * Math.min(1f, p * 2f)), col));
                        if (c.lyricsStars && u < 0.6f) AmbienceHud.sparkle(g, x + ww / 2f, y + 3 * kk, ww, u / 0.6f, a * 0.8f, (li * 31 + ch) & 0xFFFF, kk * 0.8f);
                    }
                } else {
                    float lit = Math.max(0f, Math.min(1f, u / 0.18f));
                    word(g, f, w, x + ww / 2f, y, kk, Gfx.argb(Math.round(150 * a), 0xC8CCD8));
                    if (lit > 0f) word(g, f, w, x + ww / 2f, y, kk, Gfx.argb(Math.round(255 * a * lit), singing ? acc : Ritual.mixc(acc, 0xFFFFFF, 0.45f)));
                }
                x += ww + f.width(" ") * kk; ch += w.length() + 1;
            }
            y0 += rowH;
        }
    }
    private static void faint(GuiGraphicsExtractor g, Font f, String s, float cx, float y, float k, float a, int maxW) {
        if (s.isBlank() || a < 0.02f) return;
        if (f.width(s) > maxW) s = f.plainSubstrByWidth(s, maxW - 8) + "…";
        text(g, f, s, cx, y, k, Gfx.argb(Math.round(255 * a), 0xD8DCE6));
    }
    private static void word(GuiGraphicsExtractor g, Font f, String w, float mx, float y, float k, int argb) {
        Gfx.push(g); Gfx.translate(g, mx, y + 4 * k); Gfx.scale(g, k); Gfx.txt(g, f, w, -f.width(w) / 2, -4, argb, true); Gfx.pop(g);
    }
    /** Up to two rows, split at the space nearest the middle. */
    private static List<String> wrap(Font f, String s, int maxW) {
        List<String> l = new ArrayList<>();
        if (f.width(s) <= maxW) { l.add(s); return l; }
        int mid = s.length() / 2, best = -1;
        for (int d = 0; d < mid; d++) { if (mid + d < s.length() && s.charAt(mid + d) == ' ') { best = mid + d; break; } if (s.charAt(mid - d) == ' ') { best = mid - d; break; } }
        if (best < 0) { l.add(s); return l; }
        l.add(s.substring(0, best).trim()); l.add(s.substring(best + 1).trim()); return l;
    }

    private static void titleCard(GuiGraphicsExtractor g, Font f, int W, int H, long now, float vis, float k) {
        Spotify.Track t = Spotify.track; if (t == null || !Spotify.playing) return;
        float u = (now - titleAt) / 1000f; if (u > 3.5f) return;
        float a = Math.min(1f, u / 0.4f) * Math.min(1f, (3.5f - u) / 0.7f) * vis;
        intro(g, f, W, H, t.name(), t.artists(), u, a, k);
    }
    static void intro(GuiGraphicsExtractor g, Font f, int W, int H, String name, String artists, float u, float a, float size) {
        float k = size * 1.5f, cy = H * 0.26f;
        if (f.width(name) * k > W * 0.7f) name = f.plainSubstrByWidth(name, (int) (W * 0.7f / k) - 8) + "…";
        float nw = f.width(name) * k, open = ease(Math.min(1f, u / 0.6f));
        NebulaTex.glow(g, W / 2f, cy + 8 * k, nw * 0.6f + 16, 16 * k, Gfx.argb(Math.round(90 * a), 0x000000));
        g.fill(Math.round(W / 2f - nw / 2f * open), Math.round(cy - 4), Math.round(W / 2f + nw / 2f * open), Math.round(cy - 3), Gfx.argb(Math.round(180 * a), colour()));
        text(g, f, "NOW PLAYING", W / 2f, cy - 12, 0.6f, Gfx.argb(Math.round(160 * a), colour()));
        text(g, f, name, W / 2f, cy + (1f - open) * 6, k, Gfx.argb(Math.round(255 * a), 0xFFFFFF));
        text(g, f, artists, W / 2f, cy + 12 * k, k * 0.6f, Gfx.argb(Math.round(190 * a), 0xD8D8E8));
    }
    private static void noLyrics(GuiGraphicsExtractor g, Font f, long now, float vis, float cx, float cy, float k) {
        float u = (now - titleAt) / 1000f - 3.5f; if (u < 0f) return;
        float a = Math.min(1f, u / 0.6f) * Math.max(0f, Math.min(1f, (6f - u) / 1f)) * vis; if (a < 0.02f) return;
        text(g, f, "♪  No lyrics for this one  ♪", cx, cy, k, Gfx.argb(Math.round(230 * a), colour()));
        text(g, f, "Just enjoy the music", cx, cy + 12 * k, k * 0.7f, Gfx.argb(Math.round(170 * a), 0xD8D8E8));
    }

    static void text(GuiGraphicsExtractor g, Font f, String s, float cx, float y, float k, int argb) {
        Gfx.push(g); Gfx.translate(g, cx, y); Gfx.scale(g, k); Gfx.txt(g, f, s, -f.width(s) / 2, 0, argb, true); Gfx.pop(g);
    }
    private static float ease(float v) { return 1f - (1f - v) * (1f - v) * (1f - v); }
    /** 0 -> 1 with a small overshoot: the pop. */
    private static float spring(float p) { return 1f - (float) (Math.exp(-6 * p) * Math.cos(9 * p)); }
}
