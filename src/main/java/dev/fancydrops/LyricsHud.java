package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * The lyrics (Visuals > Lyrics). Both styles draw the same clean caption: the words fill with the album's colour as they are sung (the word
 * being sung glows softly). Long lines wrap onto two rows (and shrink a little if they still don't fit), never cut off by the screen's edge.
 *
 *   FLOATING (default): each line is written into the world around you, a few blocks away, at its own angle (like a sign standing
 *     in the air), a little to the left or right of where you look. Its words appear one by one as they're sung, each popping in, in
 *     white and the album's colours. It stays where it was written (it doesn't chase you), and fades up and away when the next line
 *     comes. No boxes or glows behind it: just the words and their shadow, so it costs almost nothing.
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

    private static int lastLine = -2, prevLine = -2; private static String lastTrack = "";
    private static long lineAt, titleAt, lastFrame;
    private static float away;                                       // 0..1, how far the lyrics have stepped aside for a drop animation
    /** A line written into the world: its middle, the directions of its own right and up, how big a text pixel is (blocks). */
    static final class Sign { final int line, seed; final long born; long gone; final Vec3 at, right, up; final float px; Sign(int l, int sd, long b, Vec3 a, Vec3 r, Vec3 u, float p) { line = l; seed = sd; born = b; at = a; right = r; up = u; px = p; } }
    private static final List<Sign> SIGNS = new ArrayList<>();
    private static int side = 1;
    private static final java.util.Random RND = new java.util.Random();

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
        if (li != lastLine) {
            prevLine = lastLine; lastLine = li; lineAt = now;
            for (Sign sg : SIGNS) if (sg.gone == 0) sg.gone = now;
            if (c.lyricsStyle == FLOATING && li >= 0 && li < L.size() && !L.get(li).text().isBlank()) write(mc, c, li, now);
        }
        if (!on) for (Sign sg : SIGNS) if (sg.gone == 0) sg.gone = now;
        if (c.lyricsIntro) titleCard(g, mc, c, now, vis);
        if (on && L.isEmpty() && Lyrics.status.startsWith("No synced")) { noLyrics(g, mc, c, now, vis); return; }
        if (L.isEmpty() || !on) return;
        float since = (now - lineAt) / 1000f, in = ease(Math.min(1f, since / 0.45f)), out = Math.min(1f, since / 0.6f);
        if (c.lyricsStyle == SCREEN) {
            if (prevLine >= 0 && prevLine < L.size() && out < 1f) onScreen(g, mc, c, L, prevLine, pos, (1f - out) * vis, -out);
            if (li >= 0 && li < L.size()) onScreen(g, mc, c, L, li, pos, in * vis, 1f - in);
        } else {
            signs(g, mc, L, pos, now, vis);
        }
    }

    // ------------------------------------------------------------------ floating: words written into the world
    /** A new line: a few blocks ahead, a little to one side (the other side each time), turned at its own angle; nearer if a wall is closer. */
    private static void write(Minecraft mc, FancyConfig c, int li, long now) {
        var cam = mc.gameRenderer.mainCamera(); Vec3 eye = cam.position();
        side = -side;
        double yaw = Math.toRadians(cam.yRot() + side * (8 + RND.nextDouble() * 16)), pitch = Math.toRadians(Math.max(-25, Math.min(35, cam.xRot())) * 0.6 + 4 + RND.nextDouble() * 8);
        Vec3 dir = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        double d = 10 + RND.nextDouble() * 4;
        var hit = mc.level.clip(new ClipContext(eye, eye.add(dir.scale(d + 1)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() == HitResult.Type.BLOCK) d = Math.max(4.5, hit.getLocation().distanceTo(eye) - 0.9);           // a wall in the way: in front of it, but never right in your face
        Vec3 at = eye.add(dir.scale(d));
        double face = Math.atan2(eye.x - at.x, eye.z - at.z) + Math.toRadians(-side * (6 + RND.nextDouble() * 14));    // turned a little away from you, so it stands in the world
        Vec3 n = new Vec3(Math.sin(face), 0, Math.cos(face)), up0 = new Vec3(0, 1, 0);
        Vec3 right = up0.cross(n).normalize();
        double roll = Math.toRadians((RND.nextDouble() - 0.5) * 6);                                // a slight tilt
        Vec3 r2 = right.scale(Math.cos(roll)).add(up0.scale(Math.sin(roll))), u2 = up0.scale(Math.cos(roll)).subtract(right.scale(Math.sin(roll)));
        float px = (float) (0.042 * c.lyricsSize / 100.0 * (0.55 + d / 12));
        SIGNS.add(new Sign(li, RND.nextInt(1000), now, at, r2, u2, px)); while (SIGNS.size() > 3) SIGNS.remove(0);
    }
    private static void signs(GuiGraphicsExtractor g, Minecraft mc, List<Lyrics.Line> L, long pos, long now, float vis) {
        SIGNS.removeIf(sg -> sg.gone > 0 && now - sg.gone > 650);
        if (SIGNS.isEmpty()) return;
        var cam = mc.gameRenderer.mainCamera(); Vec3 cp = cam.position();
        Matrix4f vp = cam.getViewRotationProjectionMatrix(new Matrix4f()); int W = g.guiWidth(), H = g.guiHeight();
        Font f = mc.font; int acc = colour(), lite = Ritual.mixc(acc, 0xFFFFFF, 0.5f);
        int[] pal = {0xFFFFFF, acc, lite, 0xFFFFFF, acc};
        for (Sign sg : new ArrayList<>(SIGNS)) {
            if (sg.line < 0 || sg.line >= L.size()) continue;
            double dist = cp.distanceTo(sg.at); if (dist > 24) continue;
            float out = sg.gone > 0 ? (now - sg.gone) / 650f : 0f, fade = (1f - out) * vis * (float) Math.min(1, (24 - dist) / 4);
            if (fade < 0.03f) continue;
            Vec3 at = sg.at.add(sg.up.scale(0.5 * out * out));                                          // leaving: it rises as it fades
            Lyrics.Line ln = L.get(sg.line); long start = ln.atMs(), end = sg.line + 1 < L.size() ? L.get(sg.line + 1).atMs() : start + 4000;
            drawSign(g, f, vp, cp, W, H, sg, at, ln.text(), start, end, pos, fade * ease(Math.min(1f, (now - sg.born) / 350f)), pal);
        }
    }
    /** A whole line on its sign, in the caption's look: every word there from the start in light grey, filling with the album's colour as it
     *  is sung (with a small nod and a soft light under the word being sung). */
    static void drawSign(GuiGraphicsExtractor g, Font f, Matrix4f vp, Vec3 cp, int W, int H, Sign sg, Vec3 at, String text, long start, long end, long pos, float fade, int[] pal) {
        if (fade < 0.02f) return;
        List<String> rows = wrap(f, text, 190);
        int total = 0; for (String r : rows) total += r.length() + 1;
        float span = Math.max(400f, (end - start) * 0.85f); int acc0 = 0, col = colour();
        for (int ri = 0; ri < rows.size(); ri++) {
            String row = rows.get(ri); float x = -f.width(row) / 2f, y = (ri - (rows.size() - 1) / 2f) * 11 - 4;
            for (String w : row.split(" ")) {
                float wAt = start + span * acc0 / (float) total, wEnd = start + span * (acc0 + w.length() + 1) / (float) total; int ww = f.width(w);
                acc0 += w.length() + 1;
                float lit = sg.gone > 0 ? 1f : Math.max(0f, Math.min(1f, (pos - wAt) / 140f));
                float glow = pos >= wAt && pos < wEnd + 200 && sg.gone == 0 ? Math.min(1f, (pos - wAt) / 120f) * Math.min(1f, (wEnd + 200 - pos) / 200f) : 0f;
                float burst = sg.gone == 0 && FancyConfig.get().lyricsStars && pos >= wAt && pos < wAt + 500 ? (pos - wAt) / 500f : -1f;
                word(g, f, vp, cp, W, H, sg, at, w, x, y, ww, lit, glow, fade, col, burst, sg.seed * 31 + acc0);
                x += ww + f.width(" ");
            }
        }
    }
    /** One word, flat on its sign: projected point by point so it sits in the world with real perspective. */
    private static void word(GuiGraphicsExtractor g, Font font, Matrix4f vp, Vec3 cp, int W, int H, Sign sg, Vec3 at, String w, float x, float y, int ww, float lit, float glow, float fade, int col, float burst, int seed) {
        Vec3 o = at.add(sg.right.scale((x + ww / 2f) * sg.px)).subtract(sg.up.scale((y + 4) * sg.px));   // the word's middle
        float[] po = project(vp, cp, o, W, H), pr = project(vp, cp, o.add(sg.right.scale(sg.px * 8)), W, H), pd = project(vp, cp, o.subtract(sg.up.scale(sg.px * 8)), W, H);
        if (po == null || pr == null || pd == null) return;
        if (po[0] < -60 || po[0] > W + 60 || po[1] < -60 || po[1] > H + 60) return;
        float ax = (pr[0] - po[0]) / 8f, ay = (pr[1] - po[1]) / 8f, bx = (pd[0] - po[0]) / 8f, by = (pd[1] - po[1]) / 8f;
        if (glow > 0.02f) { float sz = (float) Math.hypot(ax, ay); NebulaTex.glow(g, po[0], po[1], ww * 0.6f * sz + 5 * sz, 7 * sz, Gfx.argb(Math.round(110 * glow * fade), col)); }
        if (burst >= 0f) { float sz = (float) Math.hypot(ax, ay); AmbienceHud.sparkle(g, po[0], po[1] - 3 * sz, ww * sz, burst, fade, seed, Math.max(1f, sz * 1.5f)); }   // stars burst from the word as it's sung
        float lift = lit > 0f && lit < 1f ? (float) Math.sin(Math.PI * lit) * 1.5f : 0f;
        Gfx.push(g);
        g.pose().mul(new Matrix3x2f(ax, ay, bx, by, po[0], po[1])); g.pose().translate(-ww / 2f, -4 - lift);
        Gfx.txt(g, font, w, 0, 0, Gfx.argb(Math.round(170 * fade), 0xE4E6EE), true);
        if (lit > 0f) Gfx.txt(g, font, w, 0, 0, Gfx.argb(Math.round(255 * fade * lit), col), true);
        Gfx.pop(g);
    }
    private static float[] project(Matrix4f vp, Vec3 cp, Vec3 p, int W, int H) {
        Vector4f v = new Vector4f((float) (p.x - cp.x), (float) (p.y - cp.y), (float) (p.z - cp.z), 1f); vp.transform(v);
        if (v.w < 0.4f) return null;
        return new float[]{(v.x / v.w * 0.5f + 0.5f) * W, (1f - (v.y / v.w * 0.5f + 0.5f)) * H};
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
        float span = Math.max(400f, (end - start) * 0.85f); int acc = 0, col = colour(); boolean sparkles = FancyConfig.get().lyricsStars;
        for (int ri = 0; ri < rows.size(); ri++) {
            String row = rows.get(ri); float x = cx - f.width(row) * k / 2f, yy = y + ri * 11 * k;
            for (String w : row.split(" ")) {
                float at = start + span * acc / (float) total, lit = Math.max(0f, Math.min(1f, (pos - at) / 140f));
                float lift = lit > 0f && lit < 1f ? (float) Math.sin(Math.PI * lit) * 1.5f * k : 0f;
                float wEnd = start + span * (acc + w.length() + 1) / (float) total, ww = f.width(w) * k;
                if (sparkles && pos >= at && pos < at + 450) AmbienceHud.sparkle(g, x + ww / 2f, yy + 3 * k, ww, (pos - at) / 450f, a, acc * 31 + ri, k);
                if (pos >= at && pos < wEnd + 200) {                                              // the word being sung glows softly in the album's colour
                    float gl = Math.min(1f, (pos - at) / 120f) * Math.min(1f, (wEnd + 200 - pos) / 200f);
                    NebulaTex.glow(g, x + ww / 2f, yy + 4 * k, ww * 0.6f + 6 * k, 7 * k, Gfx.argb(Math.round(110 * gl * a), col));
                }
                Gfx.push(g); Gfx.translate(g, x, yy - lift); Gfx.scale(g, k);
                Gfx.txt(g, f, w, 0, 0, Gfx.argb(Math.round(150 * a), 0xE4E6EE), true);
                if (lit > 0f) Gfx.txt(g, f, w, 0, 0, Gfx.argb(Math.round(255 * a * lit), col), true);
                Gfx.pop(g);
                x += f.width(w + " ") * k; acc += w.length() + 1;
            }
        }
    }
    private static final java.util.Map<String, List<String>> WRAPS = new java.util.LinkedHashMap<>(16, 0.75f, true) { @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, List<String>> e) { return size() > 12; } };
    /** Wrapping is worked out once per line and size, not every frame. */
    static List<String> wrap(Font f, String s, float maxW) { return WRAPS.computeIfAbsent(Math.round(maxW) + "|" + s, k -> wrapNow(f, s, maxW)); }
    private static List<String> wrapNow(Font f, String s, float maxW) {
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
        Gfx.push(g); Gfx.translate(g, cx, y); Gfx.scale(g, k); Gfx.txt(g, f, s, -f.width(s) / 2, 0, argb, true); Gfx.pop(g);
    }
    private static float ease(float v) { return 1f - (1f - v) * (1f - v) * (1f - v); }
}
