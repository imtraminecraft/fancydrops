package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * The lyrics (Visuals > Lyrics). Every style draws the same clean caption: the words fill with the album's colour as they are sung, and a
 * thin line under them runs with the line's time. Long lines wrap onto two rows (and shrink a little if they still don't fit).
 *
 *   FLOATING (default): each line hangs in the air a few blocks ahead of you, a little to one side, then the next to the other side. It
 *     follows you smoothly at any speed, glides back in front of you when you turn away, comes closer instead of sinking into a wall,
 *     and is never cut off by the screen's edge. Old lines float up and fade as new ones arrive.
 *   WALLS: written flat on a wall in front of you (looked for once, when the line starts); with no wall in view, it's on the screen.
 *   SCREEN: a caption on the screen (low, middle or high).
 *
 * The lyrics step aside (fade out) while a drop animation plays.
 */
final class LyricsHud {
    private LyricsHud() {}

    static final int FLOATING = 0, WALLS = 1, SCREEN = 2;

    /** The colour of the sung words: from the album cover, made light enough to read. */
    static int colour() {
        int c = AlbumArt.ready ? AlbumArt.colour : 0x1ED760;
        float[] h = java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
        return java.awt.Color.HSBtoRGB(h[0], Math.min(0.6f, h[1]), 1f) & 0xFFFFFF;
    }

    /** A line in the air: where it hangs relative to the point that follows you. */
    private static final class Float3 { final int line; final long born; long gone; double x, y, z, dist; long offSince, checkAt; Float3(int l, long b) { line = l; born = b; } }
    private record Wall(Vec3 at, Vec3 right) {}
    private static final List<Float3> AIR = new ArrayList<>();
    private static int lastLine = -2, prevLine = -2, side = 1; private static String lastTrack = "";
    private static long lineAt, titleAt, lastFrame;
    private static Wall wall, prevWall;
    private static Vec3 follow; private static float away;          // away: 0..1, how far the lyrics have stepped aside for a drop animation

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        if (!c.lyricsOn || mc.level == null || mc.player == null) { AIR.clear(); return; }
        Spotify.Track t = Spotify.track; long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - (lastFrame == 0 ? now : lastFrame)) / 1000f); lastFrame = now;
        away += ((AnimationManager.isPlaying() ? 1f : 0f) - away) * Math.min(1f, dt * 8f);
        float vis = 1f - away; if (vis < 0.02f) return;
        boolean on = t != null && Spotify.playing;
        if (on && !t.id().equals(lastTrack)) { lastTrack = t.id(); lastLine = -2; prevLine = -2; titleAt = now; wall = null; AIR.clear(); }
        long pos = Spotify.position() + 150 + c.lyricsOffset;
        List<Lyrics.Line> L = Lyrics.lines;
        int li = on ? Lyrics.at(pos) : -1;
        if (li != lastLine) {
            prevLine = lastLine; lastLine = li; lineAt = now; prevWall = wall;
            boolean real = li >= 0 && li < L.size() && !L.get(li).text().isBlank();
            wall = c.lyricsStyle == WALLS && real ? findWall(mc) : null;
            for (Float3 f : AIR) if (f.gone == 0) f.gone = now;
            if (c.lyricsStyle == FLOATING && real) hang(mc, c, li, now);
        }
        if (!on) for (Float3 f : AIR) if (f.gone == 0) f.gone = now;
        if (c.lyricsIntro) titleCard(g, mc, c, now, vis);
        if (on && L.isEmpty() && Lyrics.status.startsWith("No synced")) { noLyrics(g, mc, c, now, vis); return; }
        if (L.isEmpty()) return;
        if (c.lyricsStyle == FLOATING) { floating(g, mc, c, L, pos, now, dt, vis); return; }
        if (!on) return;
        float since = (now - lineAt) / 1000f, in = ease(Math.min(1f, since / 0.4f)), out = Math.min(1f, since / 0.35f);
        if (prevLine >= 0 && prevLine < L.size() && out < 1f) onScreenOrWall(g, mc, c, L, prevLine, pos, (1f - out) * vis, -out, prevWall);
        if (li >= 0 && li < L.size()) onScreenOrWall(g, mc, c, L, li, pos, in * vis, 1f - in, wall);
    }

    // ------------------------------------------------------------------ floating
    /** A new line: it hangs a few blocks ahead, a little to the side (alternating), and closer if a wall is nearer than that. */
    private static void hang(Minecraft mc, FancyConfig c, int li, long now) {
        var cam = mc.gameRenderer.mainCamera(); Vec3 eye = cam.position();
        if (follow == null || follow.distanceTo(eye) > 30) follow = eye;
        side = -side; double a = Math.toRadians(cam.yRot() + side * 9);
        Float3 f = new Float3(li, now); f.dist = c.lyricsDistance;
        f.x = -Math.sin(a); f.z = Math.cos(a); f.y = 0.25;
        f.dist = room(mc, eye, f, c.lyricsDistance);
        AIR.add(f); while (AIR.size() > 3) AIR.remove(0);
    }
    /** How far the line can hang that way before a block is in the way. */
    private static double room(Minecraft mc, Vec3 eye, Float3 f, double want) {
        Vec3 to = eye.add(f.x * want, f.y, f.z * want);
        BlockHitResult h = mc.level.clip(new ClipContext(eye, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
        return h.getType() == HitResult.Type.BLOCK ? Math.max(1.6, h.getLocation().distanceTo(eye) - 0.7) : want;
    }
    private static void floating(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, List<Lyrics.Line> L, long pos, long now, float dt, float vis) {
        AIR.removeIf(f -> f.gone > 0 && now - f.gone > 700);
        if (AIR.isEmpty()) return;
        var cam = mc.gameRenderer.mainCamera(); Vec3 cp = cam.position();
        Matrix4f vp = cam.getViewRotationProjectionMatrix(new Matrix4f()); int W = g.guiWidth(), H = g.guiHeight();
        if (follow == null || follow.distanceTo(cp) > 30) follow = cp;
        follow = follow.add(cp.subtract(follow).scale(1 - Math.exp(-dt * 6)));                  // eases after you: smooth at walking pace, keeps up when you sprint or fly
        Font font = mc.font;
        for (Float3 f : new ArrayList<>(AIR)) {
            if (f.line < 0 || f.line >= L.size()) continue;
            if (now >= f.checkAt) { f.checkAt = now + 250; double want = room(mc, follow, f, c.lyricsDistance); f.dist += (want - f.dist) * 0.5; }   // a wall came between: it comes nearer
            float age = (now - f.born) / 1000f, out = f.gone > 0 ? (now - f.gone) / 700f : 0f;
            double rise = 0.5 * out * out + Math.sin(now / 1400.0 + f.line) * 0.04 - (1 - ease(Math.min(1f, age / 0.45f))) * 0.15;
            Vec3 at = follow.add(f.x * f.dist, f.y + rise, f.z * f.dist);
            Vector4f v = new Vector4f((float) (at.x - cp.x), (float) (at.y - cp.y), (float) (at.z - cp.z), 1f); vp.transform(v);
            boolean seen = v.w > 0.3f; float sx = seen ? (v.x / v.w * 0.5f + 0.5f) * W : -1, sy = seen ? (1f - (v.y / v.w * 0.5f + 0.5f)) * H : -1;
            boolean off = !seen || sx < 0 || sx > W || sy < 0 || sy > H;
            if (off && f.offSince == 0) f.offSince = now;
            if (!off && sx > W * 0.15f && sx < W * 0.85f) f.offSince = 0;
            if (f.offSince != 0 && f.gone == 0 && now - f.offSince > 300) {                       // you turned away: it swings round in front of you again
                double want = Math.toRadians(cam.yRot() + side * 9), have = Math.atan2(-f.x, f.z), d = Math.atan2(Math.sin(want - have), Math.cos(want - have));
                double na = have + d * (1 - Math.exp(-dt * 5)); f.x = -Math.sin(na); f.z = Math.cos(na);
            }
            if (!seen) continue;
            float a = Math.min(1f, age / 0.35f) * (1f - Math.min(1f, out)) * vis;
            float k = c.lyricsSize / 100f * 1.3f * (float) Math.max(0.75, Math.min(1.5, c.lyricsDistance / Math.max(0.5, v.w)));
            Lyrics.Line ln = L.get(f.line); long start = ln.atMs(), end = f.line + 1 < L.size() ? L.get(f.line + 1).atMs() : start + 4000;
            block(g, font, ln.text(), W, H, sx, sy, k, W * 0.5f, start, end, pos, a, false, "");
        }
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
        float span = Math.max(400f, (end - start) * 0.85f); int acc = 0, col = colour();
        for (int ri = 0; ri < rows.size(); ri++) {
            String row = rows.get(ri); float x = cx - f.width(row) * k / 2f, yy = y + ri * 11 * k;
            for (String w : row.split(" ")) {
                float at = start + span * acc / (float) total, lit = Math.max(0f, Math.min(1f, (pos - at) / 140f));
                float lift = lit > 0f && lit < 1f ? (float) Math.sin(Math.PI * lit) * 1.5f * k : 0f;
                float wEnd = start + span * (acc + w.length() + 1) / (float) total, ww = f.width(w) * k;
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

    // ------------------------------------------------------------------ screen and walls
    private static void onScreenOrWall(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, List<Lyrics.Line> L, int i, long pos, float a, float slide, Wall w) {
        String text = L.get(i).text(); if (text.isBlank() || a < 0.02f) return;
        long start = L.get(i).atMs(), end = i + 1 < L.size() ? L.get(i + 1).atMs() : start + 4000;
        if (w != null && onWall(g, mc, mc.font, w, text, start, end, pos, a)) return;
        int W = g.guiWidth(), H = g.guiHeight(); float k = c.lyricsSize / 100f * 1.35f;
        float cy = H * (c.lyricsPos == 1 ? 0.5f : c.lyricsPos == 2 ? 0.24f : 0.72f) + slide * 9 * k;
        String next = c.lyricsNext && slide >= 0 && i + 1 < L.size() ? L.get(i + 1).text() : "";
        block(g, mc.font, text, W, H, W / 2f, cy, k, W * 0.62f, start, end, pos, a, true, next);
    }
    /** Looks once for a wall in front of you to write on: nine fixed rays, the closest good flat face wins. */
    private static Wall findWall(Minecraft mc) {
        var cam = mc.gameRenderer.mainCamera(); Vec3 eye = cam.position(); float yaw = cam.yRot();
        BlockHitResult best = null; double bestD = 1e9;
        for (int i = 0; i < 9; i++) {
            double a = Math.toRadians(yaw + (i % 3 - 1) * 22), p = Math.toRadians((i / 3 - 1) * 12);
            Vec3 dir = new Vec3(-Math.sin(a) * Math.cos(p), -Math.sin(p), Math.cos(a) * Math.cos(p));
            BlockHitResult h = mc.level.clip(new ClipContext(eye, eye.add(dir.scale(14)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (h.getType() != HitResult.Type.BLOCK || h.getDirection().getAxis() == net.minecraft.core.Direction.Axis.Y) continue;
            double d = h.getLocation().distanceTo(eye); Vec3 n = h.getDirection().getUnitVec3();
            if (d < 2 || -(n.x * dir.x + n.z * dir.z) < 0.45) continue;
            if (d < bestD) { bestD = d; best = h; }
        }
        if (best == null) return null;
        Vec3 n = best.getDirection().getUnitVec3(), at = best.getLocation().add(n.scale(0.05));
        return new Wall(new Vec3(at.x, eye.y - 0.2, at.z), new Vec3(-n.z, 0, n.x));
    }
    private static boolean onWall(GuiGraphicsExtractor g, Minecraft mc, Font f, Wall w, String text, long start, long end, long pos, float a) {
        var cam = mc.gameRenderer.mainCamera(); Vec3 cp = cam.position();
        Matrix4f vp = cam.getViewRotationProjectionMatrix(new Matrix4f()); int W = g.guiWidth(), H = g.guiHeight();
        Vec3 n = new Vec3(w.right().z, 0, -w.right().x), to = cp.subtract(w.at());
        double dist = to.length(); if (dist > 24 || (to.x * n.x + to.z * n.z) / dist < 0.3) return false;
        float[] o = project(vp, cp, w.at(), W, H), rx = project(vp, cp, w.at().add(w.right()), W, H), uy = project(vp, cp, w.at().add(0, 1, 0), W, H);
        if (o == null || rx == null || uy == null || o[0] < W * 0.05f || o[0] > W * 0.95f || o[1] < H * 0.05f || o[1] > H * 0.95f) return false;
        List<String> rows = wrap(f, text, 170);
        float size = (float) Math.max(0.035, 0.016 * Math.sqrt(dist) + 0.01) * FancyConfig.get().lyricsSize / 100f;
        Gfx.push(g);
        g.pose().mul(new Matrix3x2f((rx[0] - o[0]) * size, (rx[1] - o[1]) * size, (o[0] - uy[0]) * size, (o[1] - uy[1]) * size, o[0], o[1]));
        int widest = 0; for (String r : rows) widest = Math.max(widest, f.width(r));
        float top = -rows.size() * 11 / 2f;
        NebulaTex.glow(g, 0, 0, widest * 0.62f + 12, rows.size() * 9 + 8, Gfx.argb(Math.round(120 * a), 0x000000));
        caption(g, f, rows, 0, top, 1f, start, end, pos, a);
        float prog = Math.max(0f, Math.min(1f, (pos - start) / (float) Math.max(1, end - start)));
        g.fill(-widest / 2, Math.round(-top) + 1, -widest / 2 + Math.round(widest * prog), Math.round(-top) + 2, Gfx.argb(Math.round(200 * a), colour()));
        Gfx.pop(g);
        return true;
    }
    private static float[] project(Matrix4f vp, Vec3 cp, Vec3 p, int W, int H) {
        Vector4f v = new Vector4f((float) (p.x - cp.x), (float) (p.y - cp.y), (float) (p.z - cp.z), 1f); vp.transform(v);
        if (v.w < 0.3f) return null;
        return new float[]{(v.x / v.w * 0.5f + 0.5f) * W, (1f - (v.y / v.w * 0.5f + 0.5f)) * H};
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
