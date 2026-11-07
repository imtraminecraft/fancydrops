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
 * The lyrics (Visuals > Lyrics). One line at a time, as a calm caption: the words fill with the album's colour as they are sung, a thin
 * line under them runs with the line's time, and a new line rises in while the old one lifts away. Long lines wrap onto two rows (and
 * shrink a little if they still don't fit), so they never run off the screen.
 *
 * "On walls" puts the same caption flat onto a wall in front of you when there is one (looked for once, when the line starts). With no
 * wall in view, or once the wall leaves the screen, the caption is simply on the screen. Nothing flickers or jumps.
 */
final class LyricsHud {
    private LyricsHud() {}

    /** The colour of the sung words: from the album cover, made light enough to read. */
    static int colour() {
        int c = AlbumArt.ready ? AlbumArt.colour : 0x1ED760;
        float[] h = java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
        return java.awt.Color.HSBtoRGB(h[0], Math.min(0.6f, h[1]), 1f) & 0xFFFFFF;
    }

    private record Wall(Vec3 at, Vec3 right) {}
    private static int lastLine = -2, prevLine = -2; private static String lastTrack = "";
    private static long lineAt, titleAt;
    private static Wall wall, prevWall;

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        if (!c.lyricsOn || mc.level == null || mc.player == null) return;
        Spotify.Track t = Spotify.track; long now = System.currentTimeMillis();
        boolean on = t != null && Spotify.playing;
        if (on && !t.id().equals(lastTrack)) { lastTrack = t.id(); lastLine = -2; prevLine = -2; titleAt = now; wall = null; }
        long pos = Spotify.position() + 150;
        List<Lyrics.Line> L = Lyrics.lines;
        int li = on ? Lyrics.at(pos) : -1;
        if (li != lastLine) {
            prevLine = lastLine; lastLine = li; lineAt = now; prevWall = wall;
            wall = c.lyricsStyle == 0 && li >= 0 && li < L.size() && !L.get(li).text().isBlank() ? findWall(mc) : null;
        }
        titleCard(g, mc, c, now);
        if (on && L.isEmpty() && Lyrics.status.startsWith("No synced")) { noLyrics(g, mc, c, now); return; }
        if (!on || L.isEmpty()) return;
        float since = (now - lineAt) / 1000f, in = ease(Math.min(1f, since / 0.4f)), out = Math.min(1f, since / 0.35f);
        if (prevLine >= 0 && prevLine < L.size() && out < 1f) draw(g, mc, c, L, prevLine, pos, 1f - out, -out, prevWall);
        if (li >= 0 && li < L.size()) draw(g, mc, c, L, li, pos, in, 1f - in, wall);
    }

    // ------------------------------------------------------------------ one caption
    /** Draws line i: a = how visible, slide = -1..1 (negative lifts it away, positive brings it up from below). */
    private static void draw(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, List<Lyrics.Line> L, int i, long pos, float a, float slide, Wall w) {
        String text = L.get(i).text(); if (text.isBlank() || a < 0.02f) return;
        long start = L.get(i).atMs(), end = i + 1 < L.size() ? L.get(i + 1).atMs() : start + 4000;
        Font f = mc.font; int W = g.guiWidth(), H = g.guiHeight();
        String next = c.lyricsNext && i + 1 < L.size() ? L.get(i + 1).text() : "";
        if (w != null && onWall(g, mc, f, w, text, start, end, pos, a)) return;
        float k = c.lyricsSize / 100f * 1.35f;
        List<String> rows = wrap(f, text, W * 0.62f / k);
        if (rows.size() > 2) { k *= 0.8f; rows = wrap(f, text, W * 0.62f / k); }               // still too long: a little smaller
        float rowH = 11 * k, cy = H * (c.lyricsPos == 1 ? 0.5f : c.lyricsPos == 2 ? 0.24f : 0.72f) + slide * 9 * k - rows.size() * rowH / 2f;
        int widest = 0; for (String r : rows) widest = Math.max(widest, f.width(r));
        float bw = widest * k + 28, bh = rows.size() * rowH + 10;
        NebulaTex.glow(g, W / 2f, cy + bh / 2f - 3, bw * 0.62f, bh * 0.85f, Gfx.argb(Math.round(150 * a), 0x000000));    // a soft shade behind, no hard box
        caption(g, f, rows, W / 2f, cy, k, start, end, pos, a);
        float prog = Math.max(0f, Math.min(1f, (pos - start) / (float) Math.max(1, end - start)));       // the line's time, as a thin bar under it
        float lx = W / 2f - widest * k / 2f, ly = cy + rows.size() * rowH + 2;
        g.fill(Math.round(lx), Math.round(ly), Math.round(lx + widest * k), Math.round(ly) + 1, Gfx.argb(Math.round(40 * a), 0xFFFFFF));
        g.fill(Math.round(lx), Math.round(ly), Math.round(lx + widest * k * prog), Math.round(ly) + 1, Gfx.argb(Math.round(200 * a), colour()));
        if (!next.isBlank() && slide >= 0) {
            float nk = k * 0.62f; String nt = f.width(next) * nk > W * 0.62f ? f.plainSubstrByWidth(next, (int) (W * 0.62f / nk) - 8) + "…" : next;
            text(g, f, nt, W / 2f, ly + 5, nk, Gfx.argb(Math.round(110 * a), 0xC8CCDA));
        }
    }
    /** The rows of a line, centred on cx: each word white-ish until it is sung, then it fills with the album's colour. */
    private static void caption(GuiGraphicsExtractor g, Font f, List<String> rows, float cx, float y, float k, long start, long end, long pos, float a) {
        int total = 0; for (String r : rows) total += r.length() + 1;
        float span = Math.max(400f, (end - start) * 0.85f); int acc = 0, col = colour();
        for (int ri = 0; ri < rows.size(); ri++) {
            String row = rows.get(ri); float x = cx - f.width(row) * k / 2f, yy = y + ri * 11 * k;
            for (String w : row.split(" ")) {
                float at = start + span * acc / (float) total, lit = Math.max(0f, Math.min(1f, (pos - at) / 140f));
                float lift = lit > 0f && lit < 1f ? (float) Math.sin(Math.PI * lit) * 1.5f * k : 0f;        // a small nod as each word lands
                int dim = Gfx.argb(Math.round(150 * a), 0xE4E6EE), on = Gfx.argb(Math.round(255 * a * lit), col);
                Gfx.push(g); Gfx.translate(g, x, yy - lift); Gfx.scale(g, k);
                g.text(f, w, 0, 0, dim, true);
                if (lit > 0f) g.text(f, w, 0, 0, on, true);
                Gfx.pop(g);
                x += f.width(w + " ") * k; acc += w.length() + 1;
            }
        }
    }
    private static List<String> wrap(Font f, String s, float maxW) {
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

    // ------------------------------------------------------------------ on a wall
    /** Looks once for a wall in front of you to write on: nine fixed rays, the closest good flat face wins. */
    private static Wall findWall(Minecraft mc) {
        var cam = mc.gameRenderer.mainCamera(); Vec3 eye = cam.position(); float yaw = cam.yRot();
        BlockHitResult best = null; double bestD = 1e9; Vec3 bestDir = null;
        for (int i = 0; i < 9; i++) {
            double a = Math.toRadians(yaw + (i % 3 - 1) * 22), p = Math.toRadians((i / 3 - 1) * 12);
            Vec3 dir = new Vec3(-Math.sin(a) * Math.cos(p), -Math.sin(p), Math.cos(a) * Math.cos(p));
            BlockHitResult h = mc.level.clip(new ClipContext(eye, eye.add(dir.scale(14)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (h.getType() != HitResult.Type.BLOCK || h.getDirection().getAxis() == net.minecraft.core.Direction.Axis.Y) continue;
            double d = h.getLocation().distanceTo(eye); Vec3 n = h.getDirection().getUnitVec3();
            if (d < 2 || -(n.x * dir.x + n.z * dir.z) < 0.45) continue;                                 // too close, or too sideways to read
            if (d < bestD) { bestD = d; best = h; bestDir = dir; }
        }
        if (best == null) return null;
        Vec3 n = best.getDirection().getUnitVec3(), at = best.getLocation().add(n.scale(0.05));
        at = new Vec3(at.x, eye.y - 0.2, at.z);                                                       // at eye height, so it reads straight on
        return new Wall(at, new Vec3(-n.z, 0, n.x));
    }
    /** Draws the caption flat on its wall. False when the wall is out of view (the caption then goes on the screen). */
    private static boolean onWall(GuiGraphicsExtractor g, Minecraft mc, Font f, Wall w, String text, long start, long end, long pos, float a) {
        var cam = mc.gameRenderer.mainCamera(); Vec3 cp = cam.position();
        Matrix4f vp = cam.getViewRotationProjectionMatrix(new Matrix4f()); int W = g.guiWidth(), H = g.guiHeight();
        Vec3 n = new Vec3(w.right().z, 0, -w.right().x), to = cp.subtract(w.at());
        double dist = to.length(); if (dist > 24 || (to.x * n.x + to.z * n.z) / dist < 0.3) return false;
        float[] o = project(vp, cp, w.at(), W, H), rx = project(vp, cp, w.at().add(w.right()), W, H), uy = project(vp, cp, w.at().add(0, 1, 0), W, H);
        if (o == null || rx == null || uy == null || o[0] < W * 0.05f || o[0] > W * 0.95f || o[1] < H * 0.05f || o[1] > H * 0.95f) return false;
        List<String> rows = wrap(f, text, 170);
        float size = (float) Math.max(0.035, 0.016 * Math.sqrt(dist) + 0.01) * FancyConfig.get().lyricsSize / 100f;   // blocks per text pixel
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
    private static void titleCard(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, long now) {
        Spotify.Track t = Spotify.track; if (t == null || !Spotify.playing) return;
        float u = (now - titleAt) / 1000f; if (u > 3.5f) return;
        float a = Math.min(1f, u / 0.4f) * Math.min(1f, (3.5f - u) / 0.7f), k = c.lyricsSize / 100f * 1.6f;
        int W = g.guiWidth(), H = g.guiHeight(); float cy = H * 0.3f;
        String name = t.name(); if (mc.font.width(name) * k > W * 0.7f) name = mc.font.plainSubstrByWidth(name, (int) (W * 0.7f / k) - 8) + "…";
        NebulaTex.glow(g, W / 2f, cy + 8 * k, mc.font.width(name) * k * 0.6f + 16, 16 * k, Gfx.argb(Math.round(130 * a), 0x000000));
        text(g, mc.font, name, W / 2f, cy - (1f - Math.min(1f, u / 0.4f)) * 6, k, Gfx.argb(Math.round(255 * a), colour()));
        text(g, mc.font, t.artists(), W / 2f, cy + 12 * k, k * 0.6f, Gfx.argb(Math.round(190 * a), 0xD8D8E8));
    }
    private static void noLyrics(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, long now) {
        float u = (now - titleAt) / 1000f - 3.5f; if (u < 0f) return;
        float a = Math.min(1f, u / 0.6f) * Math.max(0f, Math.min(1f, (6f - u) / 1f)); if (a < 0.02f) return;
        int W = g.guiWidth(), H = g.guiHeight(); float k = c.lyricsSize / 100f * 1.2f, cy = H * (c.lyricsPos == 1 ? 0.5f : c.lyricsPos == 2 ? 0.24f : 0.72f);
        NebulaTex.glow(g, W / 2f, cy + 6 * k, 70 * k, 14 * k, Gfx.argb(Math.round(130 * a), 0x000000));
        text(g, mc.font, "♪  No lyrics for this one  ♪", W / 2f, cy, k, Gfx.argb(Math.round(240 * a), colour()));
        text(g, mc.font, "Just enjoy the music", W / 2f, cy + 12 * k, k * 0.7f, Gfx.argb(Math.round(170 * a), 0xD8D8E8));
    }

    private static void text(GuiGraphicsExtractor g, Font f, String s, float cx, float y, float k, int argb) {
        Gfx.push(g); Gfx.translate(g, cx, y); Gfx.scale(g, k); g.text(f, s, -f.width(s) / 2, 0, argb, true); Gfx.pop(g);
    }
    private static float ease(float v) { return 1f - (1f - v) * (1f - v) * (1f - v); }
}
