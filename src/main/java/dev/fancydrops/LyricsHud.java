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
import java.util.Random;

/**
 * The lyrics, placed by where you are (Visuals > Lyrics, style "Your surroundings"). Each new line looks around you first:
 *
 *   SKY   out in the open (nothing over your head, little around you): the line is written across the sky ahead of you, far away and big.
 *         Its letters light up like stars as they are sung, a shooting star brings each line in, and it drifts apart when the next comes.
 *   WALL  in a cave or a room (a ceiling, walls close by): the line goes onto a wall you can see, flat on the stone like a neon sign. It
 *         flickers on, glows, and flickers off.
 *   AIR   anywhere in between: it hangs in the air a few blocks ahead. If you walk off and it leaves the screen it glides back into view.
 *
 * Every word pops as it is sung (a bounce, a flash, a glow). The other style, "On screen", keeps the karaoke block on the screen.
 * Drawn on the HUD at projected positions (nothing is added to the world's own rendering).
 */
final class LyricsHud {
    private LyricsHud() {}

    static final int SKY = 0, WALL = 1, AIR = 2;

    /** The colour of the words: from the album cover, made light enough to read. */
    static int colour() {
        int c = AlbumArt.ready ? AlbumArt.colour : 0xFFC8A0;
        float[] h = java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
        return java.awt.Color.HSBtoRGB(h[0], Math.min(0.55f, h[1]), 1f) & 0xFFFFFF;
    }
    static int glowOf(int c) { float[] h = java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null); return java.awt.Color.HSBtoRGB(h[0], Math.min(1f, h[1] + 0.5f), 0.95f) & 0xFFFFFF; }

    /** A line placed in the world. */
    private static final class Placed {
        final int kind, line; final long born; long gone; double x, y, z;      // anchor (the middle of the line)
        final Vec3 right, up;                                                  // WALL: the face's own directions; else null (faces you)
        long offSince;
        Placed(int kind, int line, double x, double y, double z, Vec3 right, Vec3 up) { this.kind = kind; this.line = line; this.x = x; this.y = y; this.z = z; this.right = right; this.up = up; born = System.currentTimeMillis(); }
    }
    private static final List<Placed> LIVE = new ArrayList<>();
    private static final Random RND = new Random();
    private static int lastLine = -2, prevLine = -2; private static String lastTrack = ""; private static long lineAt, titleAt;

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        if (!c.lyricsOn || mc.level == null || mc.player == null) { LIVE.clear(); return; }
        Spotify.Track t = Spotify.track; long now = System.currentTimeMillis();
        boolean on = t != null && Spotify.playing;
        if (on && !t.id().equals(lastTrack)) { lastTrack = t.id(); lastLine = -2; titleAt = now; LIVE.clear(); }
        long pos = Spotify.position() + 200;
        int li = on ? Lyrics.at(pos) : -1;
        if (li != lastLine) {
            prevLine = lastLine; lastLine = li; lineAt = now;
            for (Placed p : LIVE) if (p.gone == 0) p.gone = now;
            if (c.lyricsStyle == 0 && li >= 0 && !Lyrics.lines.get(li).text().isBlank()) place(mc, li);
        }
        if (!on) for (Placed p : LIVE) if (p.gone == 0) p.gone = now;
        if (c.lyricsStyle == 1) screen(g, mc, c, now, on, pos, li);
        else world(g, mc, c, now, pos);
        if (on && Lyrics.lines.isEmpty() && Lyrics.status.startsWith("No synced")) noLyrics(g, mc, c, now);
    }

    // ------------------------------------------------------------------ where does the line go?
    private static void place(Minecraft mc, int li) {
        var cam = mc.gameRenderer.mainCamera(); Vec3 eye = cam.position();
        float yaw = cam.yRot();
        BlockHitResult best = null; double bestScore = -1;                                   // a wall you can see comes first, wherever you are
        for (int i = 0; i < 28; i++) {
            double a = Math.toRadians(yaw + (RND.nextDouble() - 0.5) * 100), p = Math.toRadians((RND.nextDouble() - 0.45) * 50);
            Vec3 dir = new Vec3(-Math.sin(a) * Math.cos(p), -Math.sin(p) + 0.05, Math.cos(a) * Math.cos(p));
            BlockHitResult h = hit(mc, eye, eye.add(dir.scale(18)));
            if (h == null || h.getDirection().getAxis() == net.minecraft.core.Direction.Axis.Y) continue;
            double d = h.getLocation().distanceTo(eye); if (d < 1.4) continue;
            Vec3 n = h.getDirection().getUnitVec3(); double facing = -(n.x * dir.x + n.z * dir.z);
            if (facing < 0.2) continue;
            Vec3 at = h.getLocation(); if (!roomFor(mc, at, n)) continue;                        // a real wall: solid on both sides of the spot, room in front of it
            double score = facing * 2 - Math.abs(d - 5) * 0.12 + RND.nextDouble() * 0.5;
            if (score > bestScore) { bestScore = score; best = h; }
        }
        if (best != null) {
            Vec3 n = best.getDirection().getUnitVec3(), at = best.getLocation().add(n.scale(0.04));
            LIVE.add(new Placed(WALL, li, at.x, at.y, at.z, new Vec3(-n.z, 0, n.x), new Vec3(0, 1, 0)));
            trim(); return;
        }
        boolean roof = hit(mc, eye, eye.add(0, 24, 0)) != null;
        int kind = roof ? AIR : SKY;
        double side = (RND.nextBoolean() ? 1 : -1) * (6 + RND.nextInt(16));
        double a = Math.toRadians(yaw + side);
        if (kind == SKY) {
            double el = Math.toRadians(16 + RND.nextInt(14)), d = 70;
            LIVE.add(new Placed(SKY, li, -Math.sin(a) * Math.cos(el) * d, Math.sin(el) * d, Math.cos(a) * Math.cos(el) * d, null, null));
        } else {
            double d = FancyConfig.get().lyricsDistance * (0.8 + RND.nextDouble() * 0.3);
            LIVE.add(new Placed(AIR, li, -Math.sin(a) * d, 0.3 + RND.nextDouble() * 1.0, Math.cos(a) * d, null, null));
        }
        trim();
    }
    /** Is there a wall to write on here: the blocks beside the spot are solid too (not a lone corner), and nothing stands right in front. */
    private static boolean roomFor(Minecraft mc, Vec3 at, Vec3 n) {
        Vec3 r = new Vec3(-n.z, 0, n.x); int solid = 0;
        for (int i = -1; i <= 1; i += 2) { var bp = net.minecraft.core.BlockPos.containing(at.subtract(n.scale(0.3)).add(r.scale(i * 0.9))); if (!mc.level.getBlockState(bp).isAir()) solid++; }
        var front = net.minecraft.core.BlockPos.containing(at.add(n.scale(0.5)));
        return solid >= 1 && mc.level.getBlockState(front).isAir();
    }
    private static void trim() { while (LIVE.size() > 4) LIVE.remove(0); }
    private static BlockHitResult hit(Minecraft mc, Vec3 from, Vec3 to) {
        BlockHitResult h = mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return h.getType() == HitResult.Type.BLOCK ? h : null;
    }

    // ------------------------------------------------------------------ drawing the lines placed in the world
    private static Matrix4f vp; private static Vec3 camPos, follow; private static int W, H; private static long followAt;
    /** Where a line is now: walls stay put; sky and air lines are kept relative to you. */
    private static double ax(Placed p) { return p.kind == WALL ? p.x : (p.kind == SKY ? camPos.x : follow.x) + p.x; }
    private static double ay(Placed p) { return p.kind == WALL ? p.y : (p.kind == SKY ? camPos.y : follow.y) + p.y; }
    private static double az(Placed p) { return p.kind == WALL ? p.z : (p.kind == SKY ? camPos.z : follow.z) + p.z; }
    private static float[] project(double x, double y, double z) {
        Vector4f v = new Vector4f((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z), 1f); vp.transform(v);
        if (v.w < 0.2f) return null;
        return new float[]{(v.x / v.w * 0.5f + 0.5f) * W, (1f - (v.y / v.w * 0.5f + 0.5f)) * H, v.w};
    }
    private static void world(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, long now, long pos) {
        LIVE.removeIf(p -> p.gone > 0 && now - p.gone > 1100);
        titleCard(g, mc, c, now);
        if (LIVE.isEmpty()) return;
        var cam = mc.gameRenderer.mainCamera(); camPos = cam.position(); vp = cam.getViewRotationProjectionMatrix(new Matrix4f()); W = g.guiWidth(); H = g.guiHeight();
        float dt = Math.min(0.1f, (now - (followAt == 0 ? now : followAt)) / 1000f); followAt = now;   // sky and air lines ride along with you: they catch up whatever your speed
        if (follow == null || follow.distanceTo(camPos) > 40) follow = camPos;
        follow = follow.add(camPos.subtract(follow).scale(1f - (float) Math.exp(-dt * 9f)));   // eases after you, smoothly at any speed
        Font font = mc.font; int col = colour(), glow = glowOf(col); float k = c.lyricsSize / 100f;
        List<Lyrics.Line> L = Lyrics.lines;
        for (Placed p : new ArrayList<>(LIVE)) {
            if (p.line < 0 || p.line >= L.size()) continue;
            String text = L.get(p.line).text();
            long start = L.get(p.line).atMs(), end = p.line + 1 < L.size() ? L.get(p.line + 1).atMs() : start + 4000;
            float age = (now - p.born) / 1000f, out = p.gone > 0 ? Math.max(0f, 1f - (now - p.gone) / 1100f) : 1f;
            if (p.kind == AIR) {                                                             // walked away from it: it glides back in front of you
                float[] s = project(ax(p), ay(p), az(p));
                boolean off = s == null || s[0] < -40 || s[0] > W + 40 || s[1] < -20 || s[1] > H + 20;
                boolean centred = s != null && s[0] > W * 0.2f && s[0] < W * 0.8f;
                if (off && p.offSince == 0) p.offSince = now;                                   // off screen: after a moment it starts swinging back,
                if (p.offSince != 0 && p.gone == 0 && now - p.offSince > 250) glideBack(mc, p);  // and keeps easing in until it is well in view
                if (centred) p.offSince = 0;
            }
            float[] s = project(ax(p), ay(p) + (p.kind == AIR && p.gone > 0 ? (now - p.gone) / 1100.0 * 0.6 : 0), az(p));
            if (s == null) continue;
            if (p.kind == WALL && p.gone == 0 && camPos.distanceTo(new Vec3(p.x, p.y, p.z)) > 16) { p.gone = now; place(mc, p.line); }      // left that wall behind: the line moves to one near you
            if (p.kind == WALL) wall(g, font, p, text, start, end, pos, age, out, col, glow, k, now);
            else {
                float dist = s[2], scale = p.kind == SKY ? 3.2f * k * (H / 360f) : (float) Math.max(0.6, Math.min(3.2, 10.0 / dist)) * k * (H / 300f);
                if (p.kind == SKY) sky(g, font, text, s[0], s[1], scale, start, end, pos, age, out, col, glow, now, p);
                else words(g, font, text, s[0], s[1], scale, start, end, pos, Math.min(1f, age / 0.3f) * out, col, glow, now, true);
            }
        }
    }
    /** You turned away from a line in the air: it swings round to the front again, a little every frame. */
    private static void glideBack(Minecraft mc, Placed p) {
        var cam = mc.gameRenderer.mainCamera(); double want = Math.toRadians(cam.yRot()), have = Math.atan2(-p.x, p.z), d = Math.hypot(p.x, p.z);
        double diff = Math.atan2(Math.sin(want - have), Math.cos(want - have)), na = have + diff * 0.08;
        p.x = -Math.sin(na) * d; p.z = Math.cos(na) * d;
    }

    /** The karaoke words of one line, centred on cx, cy: sung words bright with a pop, the rest dim. */
    private static void words(GuiGraphicsExtractor g, Font font, String line, float cx, float cy, float k, long start, long end, long pos, float a, int col, int glow, long now, boolean glowOn) {
        if (a < 0.02f) return;
        String[] ws = line.split(" "); int total = 0; for (String w : ws) total += w.length() + 1;
        float lineW = font.width(line) * k, x = cx - lineW / 2f, span = Math.max(400f, (end - start) * 0.82f); int acc = 0;
        for (String w : ws) {
            float wAt = start + span * acc / (float) total, wEnd = start + span * (acc + w.length() + 1) / (float) total;
            float lit = Math.max(0f, Math.min(1f, (pos - wAt) / 120f)), pop = (pos - wAt) / 260f;
            float bounce = pop > 0 && pop < 1 ? (float) Math.sin(Math.PI * pop) * 0.28f : 0f, singing = pos >= wAt && pos < wEnd ? 1f : 0f;
            float wW = font.width(w) * k, wx = x + wW / 2f, wy = cy - (pop > 0 && pop < 1 ? (float) Math.sin(Math.PI * pop) * 4 * k : 0);
            if (glowOn && lit > 0f) NebulaTex.glow(g, wx, wy + 4 * k, wW * 0.6f + 7 * k, 9 * k * (1 + bounce), Gfx.argb(Math.round((40 + 90 * singing) * lit * a), glow));
            if (pop > 0 && pop < 0.35f) NebulaTex.glow(g, wx, wy + 4 * k, wW * 0.4f + 3 * k, 5 * k, Gfx.argb(Math.round(200 * (1 - pop / 0.35f) * a), 0xFFFFFF));     // the flash as a word lands
            int cw = lit >= 1f ? (pop < 0.3f ? Ritual.mixc(0xFFFFFF, col, pop / 0.3f) : col) : Ritual.mixc(0x7A7A8A, col, lit * 0.6f);
            drawWord(g, font, w, wx, wy, k * (1f + bounce), Gfx.argb(Math.round((110 + 145 * lit) * a), cw), glowOn ? Gfx.argb(Math.round(80 * lit * a), glow) : 0);
            x += wW + font.width(" ") * k; acc += w.length() + 1;
        }
    }
    /** Written across the sky: each letter is a star that lights up when sung; a shooting star brings the line in; it drifts apart at the end. */
    private static void sky(GuiGraphicsExtractor g, Font font, String line, float cx, float cy, float k, long start, long end, long pos, float age, float out, int col, int glow, long now, Placed p) {
        float lineW = font.width(line) * k, x0 = cx - lineW / 2f, span = Math.max(400f, (end - start) * 0.82f);
        if (age < 0.7f) {                                                                    // the shooting star that writes the line in
            float u = age / 0.7f, hx = x0 - 60 + (lineW + 120) * u, hy = cy - 30 + 26 * u;
            for (int i = 0; i < 8; i++) NebulaTex.glow(g, hx - i * 6, hy - i * 1.3f, 3 + (8 - i) * 0.5f, 2, Gfx.argb(Math.round(220 * (1 - i / 8f) * (1 - u * 0.5f)), i < 2 ? 0xFFFFFF : glow));
        }
        float x = x0; int n = line.length();
        for (int i = 0; i < n; i++) {
            String ch = String.valueOf(line.charAt(i)); float cw = font.width(ch) * k;
            float at = start + span * i / (float) n, lit = Math.max(0f, Math.min(1f, (pos - at) / 160f)), shown = Math.max(0f, Math.min(1f, (age - 0.7f * i / n) / 0.25f));
            float drift = p.gone > 0 ? (now - p.gone) / 1100f : 0f, dx = 0f, dy = -14f * drift * drift;
            float a = shown * out, tw = 0.85f + 0.15f * (float) Math.sin(now / 300.0 + i * 1.3);
            if (a > 0.02f && !ch.isBlank()) {
                float lx = x + cw / 2f + dx, ly = cy + dy;
                if (lit > 0f) { NebulaTex.glow(g, lx, ly + 4 * k, 6 * k, 6 * k, Gfx.argb(Math.round(110 * lit * a * tw), glow)); if (lit < 1f) NebulaTex.glow(g, lx, ly + 4 * k, 9 * k * (1 - lit), 1.2f, Gfx.argb(Math.round(220 * a), 0xFFFFFF)); }
                drawWord(g, font, ch, lx, ly, k * (1f + 0.25f * (lit > 0 && lit < 1 ? (float) Math.sin(Math.PI * lit) : 0f)), Gfx.argb(Math.round((70 + 185 * lit) * a * tw), lit > 0 ? col : 0x9AA6D0), 0);
            }
            x += cw;
        }
    }
    /** Flat on a wall, like a neon sign: it flickers on, glows, and flickers off. Drawn through the wall face's own directions. */
    private static void wall(GuiGraphicsExtractor g, Font font, Placed p, String text, long start, long end, long pos, float age, float out, int col, int glow, float k, long now) {
        double dist = Math.sqrt((p.x - camPos.x) * (p.x - camPos.x) + (p.y - camPos.y) * (p.y - camPos.y) + (p.z - camPos.z) * (p.z - camPos.z));
        float size = (float) (0.03 + 0.007 * dist) * k;                                     // blocks per text pixel: bigger the further the wall
        float[] o = project(p.x, p.y, p.z), rx = project(p.x + p.right.x, p.y + p.right.y, p.z + p.right.z), uy = project(p.x, p.y + 1, p.z);
        if (o == null || rx == null || uy == null) return;
        Vec3 toCam = camPos.subtract(p.x, p.y, p.z); Vec3 n = new Vec3(p.right.z, 0, -p.right.x);
        if (toCam.x * n.x + toCam.z * n.z < 0.2) return;                                  // from behind or edge on: hidden
        float flick = age < 0.5f ? (((int) (age * 22)) % 3 == 1 ? 0.25f : 1f) : 1f;              // flickers on in a few even beats
        if (p.gone > 0 && out < 0.7f) flick *= ((int) (out * 20)) % 2 == 0 ? 1f : 0.3f;
        float a = Math.min(1f, age / 0.45f) * out * flick;
        int w = font.width(text);
        Gfx.push(g);
        g.pose().mul(new Matrix3x2f((rx[0] - o[0]) * size, (rx[1] - o[1]) * size, (o[0] - uy[0]) * size, (o[1] - uy[1]) * size, o[0], o[1]));
        float lit0 = Math.max(0f, Math.min(1f, (pos - start) / 300f));
        NebulaTex.glow(g, 0, 4, w * 0.62f + 8, 11, Gfx.argb(Math.round(70 * a), glow));
        g.fill(-w / 2 - 5, -4, w / 2 + 5, -3, Gfx.argb(Math.round(150 * a), glow)); g.fill(-w / 2 - 5, 11, w / 2 + 5, 12, Gfx.argb(Math.round(150 * a), glow));      // the sign's neon tubes above and below
        words(g, font, text, 0, 0, 1f, start, end, pos, a * (0.6f + 0.4f * lit0), col, glow, now, true);
        Gfx.pop(g);
    }

    // ------------------------------------------------------------------ ON SCREEN
    private static float swayX, swayY, velX, velY, lastYaw = Float.NaN, lastPitch; private static long lastFrame;
    private static void screen(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, long now, boolean on, long pos, int li) {
        List<Lyrics.Line> L = Lyrics.lines;
        float dt = Math.min(0.05f, (now - (lastFrame == 0 ? now : lastFrame)) / 1000f); lastFrame = now;
        var cam = mc.gameRenderer.mainCamera(); float yaw = cam.yRot(), pitch = cam.xRot();
        if (Float.isNaN(lastYaw)) { lastYaw = yaw; lastPitch = pitch; }
        float dy = yaw - lastYaw; while (dy > 180) dy -= 360; while (dy < -180) dy += 360;
        float dp = pitch - lastPitch; lastYaw = yaw; lastPitch = pitch;
        velX += -dy * 22f; velY += -dp * 16f;                                                // the block hangs on a spring: it lags behind your turn, then settles
        velX += (-160f * swayX - 13f * velX) * dt; velY += (-160f * swayY - 13f * velY) * dt;
        swayX = Math.max(-30f, Math.min(30f, swayX + velX * dt)); swayY = Math.max(-16f, Math.min(16f, swayY + velY * dt));
        titleCard(g, mc, c, now);
        if (!on || L.isEmpty() || li < 0) return;
        int W0 = g.guiWidth(), H0 = g.guiHeight(); Font font = mc.font;
        float k = c.lyricsSize / 100f * 1.6f, cy = H0 * (c.lyricsPos == 1 ? 0.5f : c.lyricsPos == 2 ? 0.26f : 0.7f) + swayY, cx = W0 / 2f + swayX;
        int col = colour(), glow = glowOf(col);
        float since = (now - lineAt) / 1000f, enter = Math.min(1f, since / 0.35f), ee = 1f - (1f - enter) * (1f - enter) * (1f - enter);
        if (prevLine >= 0 && prevLine < L.size() && since < 0.7f) { float u = since / 0.7f; String pt = L.get(prevLine).text(); if (!pt.isBlank()) drawWord(g, font, pt, cx, cy - 16 * k * (0.6f + u), k * (0.85f - 0.15f * u), Gfx.argb(Math.round(120 * (1f - u)), col), 0); }
        String line = L.get(li).text();
        long start = L.get(li).atMs(), end = li + 1 < L.size() ? L.get(li + 1).atMs() : start + 4000;
        if (!line.isBlank()) words(g, font, line, cx, cy + (1f - ee) * 10 * k, k, start, end, pos, enter, col, glow, now, true);
        if (c.lyricsNext && li + 1 < L.size()) { String nt = L.get(li + 1).text(); if (!nt.isBlank()) drawWord(g, font, nt, cx, cy + 15 * k, k * 0.7f, Gfx.argb(Math.round(70 * ee), 0xD8D8E8), 0); }
    }
    /** The song's name, big and brief, when it starts. */
    private static void titleCard(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, long now) {
        Spotify.Track t = Spotify.track; if (t == null || !Spotify.playing) return;
        float u = (now - titleAt) / 1000f; if (u > 4f) return;
        float a = Math.min(1f, u / 0.5f) * Math.min(1f, (4f - u) / 0.8f);
        int W0 = g.guiWidth(), H0 = g.guiHeight(); float k = c.lyricsSize / 100f * 1.5f, cy = H0 * 0.3f, cx = W0 / 2f;
        int col = colour();
        NebulaTex.glow(g, cx, cy + 4 * k, mc.font.width(t.name()) * k * 0.6f + 10, 12 * k, Gfx.argb(Math.round(70 * a), glowOf(col)));
        drawWord(g, mc.font, t.name(), cx, cy - (1f - Math.min(1f, u / 0.5f)) * 8, k * 1.15f, Gfx.argb(Math.round(255 * a), col), 0);
        drawWord(g, mc.font, t.artists(), cx, cy + 13 * k, k * 0.7f, Gfx.argb(Math.round(170 * a), 0xD8D8E8), 0);
    }
    /** A song with no lyrics: a small sign says so, with music notes drifting up around it; after a while only the notes stay, quietly. */
    private static void noLyrics(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, long now) {
        float u = (now - titleAt) / 1000f - 4f; if (u < 0f) return;                           // after the title card
        int W0 = g.guiWidth(), H0 = g.guiHeight(); float k = c.lyricsSize / 100f * 1.3f, cx = W0 / 2f, cy = H0 * (c.lyricsStyle == 1 && c.lyricsPos == 2 ? 0.26f : c.lyricsStyle == 1 && c.lyricsPos == 1 ? 0.5f : 0.7f);
        int col = colour(), glow = glowOf(col);
        float sign = Math.min(1f, u / 0.6f) * Math.max(0f, Math.min(1f, (7f - u) / 1f));
        for (int i = 0; i < 7; i++) {                                                          // notes drifting up and swaying, in the cover's colours
            float life = 3.2f, tt = ((now / 1000f) + i * 0.47f) % life, f = tt / life, al = (float) Math.sin(Math.PI * f) * (0.35f + 0.65f * sign);
            float nx = cx + (i - 3) * 26 * k + (float) Math.sin(f * 6 + i) * 8 * k, ny = cy + 10 * k - f * 46 * k;
            String n = i % 3 == 0 ? "\u266B" : "\u266A";
            NebulaTex.glow(g, nx, ny + 4 * k, 7 * k, 7 * k, Gfx.argb(Math.round(70 * al), glow));
            drawWord(g, mc.font, n, nx, ny, k * (1f + 0.15f * (float) Math.sin(f * 9 + i)), Gfx.argb(Math.round(220 * al), i % 2 == 0 ? col : 0xFFFFFF), 0);
        }
        if (sign > 0.02f) {
            NebulaTex.glow(g, cx, cy + 16 * k, 70 * k, 10 * k, Gfx.argb(Math.round(70 * sign), 0x000000));
            drawWord(g, mc.font, "No lyrics for this one", cx, cy + 12 * k, k, Gfx.argb(Math.round(240 * sign), col), Gfx.argb(Math.round(60 * sign), glow));
            drawWord(g, mc.font, "Just enjoy the music", cx, cy + 24 * k, k * 0.7f, Gfx.argb(Math.round(170 * sign), 0xD8D8E8), 0);
        }
    }

    private static void drawWord(GuiGraphicsExtractor g, Font f, String s, float cx, float y, float k, int argb, int glow) {
        Gfx.push(g); Gfx.translate(g, cx, y); Gfx.scale(g, k); Gfx.translate(g, -f.width(s) / 2f, 0);
        if ((glow >>> 24) > 4) for (int d = 0; d < 4; d++) g.text(f, s, d == 0 ? -1 : d == 1 ? 1 : 0, d == 2 ? -1 : d == 3 ? 1 : 0, glow, false);
        g.text(f, s, 0, 0, argb, true); Gfx.pop(g);
    }
}
