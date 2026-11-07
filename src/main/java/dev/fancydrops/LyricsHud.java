package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * The lyrics (Visuals > Lyrics). Both styles draw the same clean caption: the words fill with the album's colour as they are sung (the word
 * being sung glows softly). Long lines wrap onto two rows (and shrink a little if they still don't fit), never cut off by the screen's edge.
 *
 *   FLOATING (default): each line hangs in front of you, a little to one side, then the next to the other side. It sways on a spring when
 *     you turn or look up and down and settles straight back, bobs gently, and drifts up and away when the next line comes. It is held
 *     to the screen (not the world), so it never lags behind, never leaves your view and costs next to nothing.
 *   SCREEN: a steady caption with a thin line under it showing the line's time, and the coming line faintly below.
 *
 * The lyrics step aside (fade out) while a drop animation plays.
 */
final class LyricsHud {
    private LyricsHud() {}

    static final int FLOATING = 0, SCREEN = 1;

    /** The colour of the sung words: from the album cover, made light enough to read. */
    static int colour() {
        int c = AlbumArt.ready ? AlbumArt.colour : 0x1ED760;
        float[] h = java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
        return java.awt.Color.HSBtoRGB(h[0], Math.min(0.6f, h[1]), 1f) & 0xFFFFFF;
    }

    private static int lastLine = -2, prevLine = -2, side = 1, prevSide = -1; private static String lastTrack = "";
    private static long lineAt, titleAt, lastFrame;
    private static float away;                                       // 0..1, how far the lyrics have stepped aside for a drop animation
    private static float swayX, swayY, velX, velY, lastYaw = Float.NaN, lastPitch;
    private static float jitter, prevJitter;                          // each line's own small height difference

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        if (!c.lyricsOn || mc.level == null || mc.player == null) return;
        Spotify.Track t = Spotify.track; long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - (lastFrame == 0 ? now : lastFrame)) / 1000f); lastFrame = now;
        away += ((AnimationManager.isPlaying() ? 1f : 0f) - away) * Math.min(1f, dt * 8f);
        float vis = 1f - away; if (vis < 0.02f) return;
        boolean on = t != null && Spotify.playing;
        if (on && !t.id().equals(lastTrack)) { lastTrack = t.id(); lastLine = -2; prevLine = -2; titleAt = now; }
        long pos = Spotify.position() + 150 + c.lyricsOffset;
        List<Lyrics.Line> L = Lyrics.lines;
        int li = on ? Lyrics.at(pos) : -1;
        if (li != lastLine) { prevLine = lastLine; lastLine = li; lineAt = now; prevSide = side; side = -side; prevJitter = jitter; jitter = (float) (Math.random() - 0.5); }
        sway(mc, dt);
        if (c.lyricsIntro) titleCard(g, mc, c, now, vis);
        if (on && L.isEmpty() && Lyrics.status.startsWith("No synced")) { noLyrics(g, mc, c, now, vis); return; }
        if (L.isEmpty() || !on) return;
        float since = (now - lineAt) / 1000f, in = ease(Math.min(1f, since / 0.45f)), out = Math.min(1f, since / 0.6f);
        if (c.lyricsStyle == SCREEN) {
            if (prevLine >= 0 && prevLine < L.size() && out < 1f) onScreen(g, mc, c, L, prevLine, pos, (1f - out) * vis, -out);
            if (li >= 0 && li < L.size()) onScreen(g, mc, c, L, li, pos, in * vis, 1f - in);
        } else {
            if (prevLine >= 0 && prevLine < L.size() && out < 1f) floating(g, mc, c, L, prevLine, pos, now, (1f - out) * vis, -ease(out), prevSide, prevJitter);
            if (li >= 0 && li < L.size()) floating(g, mc, c, L, li, pos, now, in * vis, 1f - in, side, jitter);
        }
    }

    // ------------------------------------------------------------------ floating
    /** The spring the floating lines hang on: turning pushes them the other way, then they settle back. */
    private static void sway(Minecraft mc, float dt) {
        var cam = mc.gameRenderer.mainCamera(); float yaw = cam.yRot(), pitch = cam.xRot();
        if (Float.isNaN(lastYaw)) { lastYaw = yaw; lastPitch = pitch; }
        float dy = yaw - lastYaw; while (dy > 180) dy -= 360; while (dy < -180) dy += 360;
        float dp = pitch - lastPitch; lastYaw = yaw; lastPitch = pitch;
        velX += -Math.max(-12f, Math.min(12f, dy)) * 18f; velY += -Math.max(-12f, Math.min(12f, dp)) * 12f;
        int steps = Math.max(1, Math.round(dt / 0.008f)); float h = dt / steps;                 // small steps: steady at any frame rate
        for (int i = 0; i < steps; i++) { velX += (-170f * swayX - 16f * velX) * h; velY += (-170f * swayY - 16f * velY) * h; swayX += velX * h; swayY += velY * h; }
        swayX = Math.max(-40f, Math.min(40f, swayX)); swayY = Math.max(-22f, Math.min(22f, swayY));
    }
    /** slide: 1..0 coming in (from a little below), 0..-1 leaving (drifting up). */
    private static void floating(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, List<Lyrics.Line> L, int i, long pos, long now, float a, float slide, int sd, float jit) {
        if (a < 0.02f) return;
        int W = g.guiWidth(), H = g.guiHeight();
        float base = c.lyricsPos == 2 ? 0.27f : c.lyricsPos == 1 ? 0.4f : 0.56f;
        float k = c.lyricsSize / 100f * 1.45f * (1f - 0.06f * Math.abs(slide));
        float bob = (float) Math.sin(now / 1300.0 + i * 1.7) * 2f;
        float cx = W * (0.5f + sd * 0.09f) + swayX * (0.8f + 0.2f * sd), cy = H * (base + jit * 0.05f) + swayY + bob + (slide > 0 ? slide * 14 : slide * 22);
        Lyrics.Line ln = L.get(i); long start = ln.atMs(), end = i + 1 < L.size() ? L.get(i + 1).atMs() : start + 4000;
        block(g, mc.font, ln.text(), W, H, cx, cy, k, W * 0.5f, start, end, pos, a, false, "");
    }

    // ------------------------------------------------------------------ the caption
    /** A whole caption centred on cx, cy: soft shade, the words, the time line; kept inside the screen. Returns nothing. */
    static void block(GuiGraphicsExtractor g, Font f, String text, int W, int H, float cx, float cy, float k, float maxW, long start, long end, long pos, float a, boolean timeBar, String next) {
        if (text.isBlank() || a < 0.02f) return;
        List<String> rows = wrap(f, text, maxW / k);
        if (rows.size() > 2) { k *= 0.8f; rows = wrap(f, text, maxW / k); }
        int widest = 0; for (String r : rows) widest = Math.max(widest, f.width(r));
        float rowH = 11 * k, bw = widest * k, bh = rows.size() * rowH;
        cx = Math.max(bw / 2f + 8, Math.min(W - bw / 2f - 8, cx)); cy = Math.max(bh / 2f + 8, Math.min(H - bh / 2f - 30, cy));   // never cut off by the screen's edge
        float y0 = cy - bh / 2f;
        NebulaTex.glow(g, cx, cy, bw * 0.62f + 14, bh * 0.85f + 8, Gfx.argb(Math.round(165 * a), 0x000000));
        caption(g, f, rows, cx, y0, k, start, end, pos, a);
        float prog = Math.max(0f, Math.min(1f, (pos - start) / (float) Math.max(1, end - start))), ly = y0 + bh + 1;
        if (timeBar) {                                                                           // the line's time, under it (not when floating)
            g.fill(Math.round(cx - bw / 2f), Math.round(ly), Math.round(cx + bw / 2f), Math.round(ly) + 1, Gfx.argb(Math.round(35 * a), 0xFFFFFF));
            g.fill(Math.round(cx - bw / 2f), Math.round(ly), Math.round(cx - bw / 2f + bw * prog), Math.round(ly) + 1, Gfx.argb(Math.round(200 * a), colour()));
        }
        if (!next.isBlank()) {
            float nk = k * 0.62f; String nt = f.width(next) * nk > maxW ? f.plainSubstrByWidth(next, (int) (maxW / nk) - 8) + "…" : next;
            text(g, f, nt, cx, ly + 5, nk, Gfx.argb(Math.round(110 * a), 0xC8CCDA));
        }
    }
    /** The rows of a line, centred on cx: each word light grey until it is sung, then it fills with the album's colour (with a small nod). */
    static void caption(GuiGraphicsExtractor g, Font f, List<String> rows, float cx, float y, float k, long start, long end, long pos, float a) {
        int total = 0; for (String r : rows) total += r.length() + 1;
        float span = Math.max(400f, (end - start) * 0.85f); int acc = 0, col = colour(); boolean sparkles = FancyConfig.get().ambSparkle;
        for (int ri = 0; ri < rows.size(); ri++) {
            String row = rows.get(ri); float x = cx - f.width(row) * k / 2f, yy = y + ri * 11 * k;
            for (String w : row.split(" ")) {
                float at = start + span * acc / (float) total, lit = Math.max(0f, Math.min(1f, (pos - at) / 140f));
                float lift = lit > 0f && lit < 1f ? (float) Math.sin(Math.PI * lit) * 1.5f * k : 0f;
                float wEnd = start + span * (acc + w.length() + 1) / (float) total, ww = f.width(w) * k;
                if (sparkles && pos >= at && pos < at + 450) AmbienceHud.sparkle(g, x + ww / 2f, yy + 3 * k, ww, (pos - at) / 450f, a, acc * 31 + ri);
                if (pos >= at && pos < wEnd + 200) {                                              // the word being sung glows softly in the album's colour
                    float gl = Math.min(1f, (pos - at) / 120f) * Math.min(1f, (wEnd + 200 - pos) / 200f);
                    NebulaTex.glow(g, x + ww / 2f, yy + 4 * k, ww * 0.6f + 6 * k, 7 * k, Gfx.argb(Math.round(110 * gl * a), col));
                }
                Gfx.push(g); Gfx.translate(g, x, yy - lift); Gfx.scale(g, k);
                g.text(f, w, 0, 0, Gfx.argb(Math.round(150 * a), 0xE4E6EE), true);
                if (lit > 0f) g.text(f, w, 0, 0, Gfx.argb(Math.round(255 * a * lit), col), true);
                Gfx.pop(g);
                x += f.width(w + " ") * k; acc += w.length() + 1;
            }
        }
    }
    static List<String> wrap(Font f, String s, float maxW) {
        List<String> out = new ArrayList<>(); StringBuilder cur = new StringBuilder();
        for (String w : s.trim().split("\\s+")) {
            String t = cur.isEmpty() ? w : cur + " " + w;
            if (f.width(t) > maxW && !cur.isEmpty()) { out.add(cur.toString()); cur = new StringBuilder(w); } else { cur = new StringBuilder(t); }
        }
        if (!cur.isEmpty()) out.add(cur.toString());
        if (out.size() == 2 && f.width(out.get(1)) < f.width(out.get(0)) * 0.35f) {           // balance two rows so the second isn't a lonely word
            String[] ws = s.trim().split("\\s+"); int best = 1; float bd = 1e9f;
            for (int i = 1; i < ws.length; i++) { float d = Math.abs(f.width(String.join(" ", java.util.Arrays.copyOfRange(ws, 0, i))) - f.width(String.join(" ", java.util.Arrays.copyOfRange(ws, i, ws.length)))); if (d < bd) { bd = d; best = i; } }
            String r0 = String.join(" ", java.util.Arrays.copyOfRange(ws, 0, best)), r1 = String.join(" ", java.util.Arrays.copyOfRange(ws, best, ws.length));
            if (f.width(r0) <= maxW && f.width(r1) <= maxW) { out.clear(); out.add(r0); out.add(r1); }
        }
        return out;
    }

    // ------------------------------------------------------------------ on screen
    private static void onScreen(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, List<Lyrics.Line> L, int i, long pos, float a, float slide) {
        String text = L.get(i).text(); if (text.isBlank() || a < 0.02f) return;
        long start = L.get(i).atMs(), end = i + 1 < L.size() ? L.get(i + 1).atMs() : start + 4000;
        int W = g.guiWidth(), H = g.guiHeight(); float k = c.lyricsSize / 100f * 1.35f;
        float cy = H * (c.lyricsPos == 1 ? 0.5f : c.lyricsPos == 2 ? 0.24f : 0.72f) + slide * 9 * k;
        String next = c.lyricsNext && slide >= 0 && i + 1 < L.size() ? L.get(i + 1).text() : "";
        block(g, mc.font, text, W, H, W / 2f, cy, k, W * 0.62f, start, end, pos, a, true, next);
    }

    // ------------------------------------------------------------------ the song's name, and songs without lyrics
    private static void titleCard(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, long now, float vis) {
        Spotify.Track t = Spotify.track; if (t == null || !Spotify.playing) return;
        float u = (now - titleAt) / 1000f; if (u > 3.5f) return;
        float a = Math.min(1f, u / 0.4f) * Math.min(1f, (3.5f - u) / 0.7f) * vis;
        intro(g, mc.font, g.guiWidth(), g.guiHeight(), t.name(), t.artists(), u, a, c.lyricsSize / 100f);
    }
    static void intro(GuiGraphicsExtractor g, Font f, int W, int H, String name, String artists, float u, float a, float size) {
        float k = size * 1.6f, cy = H * 0.3f;
        if (f.width(name) * k > W * 0.7f) name = f.plainSubstrByWidth(name, (int) (W * 0.7f / k) - 8) + "…";
        float nw = f.width(name) * k, open = ease(Math.min(1f, u / 0.6f));
        NebulaTex.glow(g, W / 2f, cy + 8 * k, nw * 0.6f + 16, 16 * k, Gfx.argb(Math.round(130 * a), 0x000000));
        g.fill(Math.round(W / 2f - nw / 2f * open), Math.round(cy - 4), Math.round(W / 2f + nw / 2f * open), Math.round(cy - 3), Gfx.argb(Math.round(180 * a), colour()));   // a line opens above the name
        text(g, f, "NOW PLAYING", W / 2f, cy - 12, 0.6f, Gfx.argb(Math.round(160 * a), colour()));
        text(g, f, name, W / 2f, cy + (1f - open) * 6, k, Gfx.argb(Math.round(255 * a), 0xFFFFFF));
        text(g, f, artists, W / 2f, cy + 12 * k, k * 0.6f, Gfx.argb(Math.round(190 * a), 0xD8D8E8));
    }
    private static void noLyrics(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, long now, float vis) {
        float u = (now - titleAt) / 1000f - 3.5f; if (u < 0f) return;
        float a = Math.min(1f, u / 0.6f) * Math.max(0f, Math.min(1f, (6f - u) / 1f)) * vis; if (a < 0.02f) return;
        int W = g.guiWidth(), H = g.guiHeight(); float k = c.lyricsSize / 100f * 1.2f, cy = H * (c.lyricsPos == 1 ? 0.5f : c.lyricsPos == 2 ? 0.24f : 0.72f);
        NebulaTex.glow(g, W / 2f, cy + 6 * k, 70 * k, 14 * k, Gfx.argb(Math.round(130 * a), 0x000000));
        text(g, mc.font, "♪  No lyrics for this one  ♪", W / 2f, cy, k, Gfx.argb(Math.round(240 * a), colour()));
        text(g, mc.font, "Just enjoy the music", W / 2f, cy + 12 * k, k * 0.7f, Gfx.argb(Math.round(170 * a), 0xD8D8E8));
    }

    static void text(GuiGraphicsExtractor g, Font f, String s, float cx, float y, float k, int argb) {
        Gfx.push(g); Gfx.translate(g, cx, y); Gfx.scale(g, k); g.text(f, s, -f.width(s) / 2, 0, argb, true); Gfx.pop(g);
    }
    private static float ease(float v) { return 1f - (1f - v) * (1f - v) * (1f - v); }
}
