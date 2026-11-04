package dev.fancydrops;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.*;

/**
 * Achievements as a star map. Every chapter of the ledger is a constellation in one deep sky, every achievement is a star in it.
 * Earned stars burn in the chapter's colour with rays and a halo, and the figure's lines light up between two earned stars, so a
 * finished chapter draws its whole picture. A star you earned since you last looked ignites when the sky opens.
 *
 * Opening: the sky warps in out of the button (stars streak past), then settles. Click a constellation to fly in (the stars streak
 * again): names, progress rings, rewards and the chapter's numbers in a panel. Right click, Esc or "All constellations" flies out;
 * arrows and the wheel move between constellations.
 */
final class Codex {
    private Codex() {}

    /** What the menu lends the sky: its font, toasts and the hover card. */
    interface Host {
        Font font();
        default int width(String s) { return font().width(s); }
        void toast(String text, int col);
        void tip(String lines, int col);
    }

    // ---- the constellations, in the menu's order
    private static final String[] ACH = {"ledger", "fortune", "myth", "rare", "farm", "fish", "dye"};
    private static final String[] ACH_NAME = {"The Ledger", "Fortune", "Mythological", "Rare Drops", "Farming", "Fishing", "Dyes"};
    private static final String[] FIGURE = {"The Quill", "The Coin", "The Horn", "The Sword", "The Sheaf", "The Fish", "The Drop"};
    private static final int[] ACH_COL = {0xFFFFD27A, 0xFFC9A0FF, 0xFF7AD8FF, 0xFFFF8A7A, 0xFF9CEB78, 0xFF62E6F0, 0xFFFF9AD2};
    private static final String[] QUOTE = {"Every find, written down.", "Fortune favours the patient.", "Dig, and the earth answers.", "They drop what they guard.", "The harvest remembers.", "Patience, then a splash.", "Every colour has a story."};
    private static final float[][] HOME = {{0.17f, 0.33f}, {0.42f, 0.25f}, {0.69f, 0.30f}, {0.87f, 0.63f}, {0.15f, 0.73f}, {0.40f, 0.71f}, {0.64f, 0.74f}};
    private static final float[][][] SHAPE = {
        {{-0.13f, 0.05f}, {-0.08f, -0.03f}, {-0.02f, 0.02f}, {0.04f, -0.05f}, {0.10f, -0.08f}, {0.13f, 0.00f}, {0.08f, 0.07f}, {0.02f, 0.10f}},
        {{0.00f, -0.11f}, {0.08f, -0.07f}, {0.11f, 0.01f}, {0.07f, 0.09f}, {-0.01f, 0.11f}, {-0.09f, 0.07f}, {-0.11f, -0.02f}, {-0.07f, -0.09f}},
        {{-0.11f, 0.08f}, {-0.04f, 0.03f}, {0.03f, -0.03f}, {0.10f, -0.10f}},
        {{-0.13f, 0.12f}, {-0.07f, 0.06f}, {-0.01f, 0.00f}, {0.05f, -0.06f}, {0.10f, -0.11f}, {-0.09f, -0.04f}, {0.04f, 0.09f}, {0.15f, -0.15f}},
        {{0.00f, 0.11f}, {0.00f, 0.02f}, {-0.07f, -0.06f}, {0.00f, -0.10f}, {0.07f, -0.06f}},
        {{-0.12f, 0.00f}, {-0.04f, -0.06f}, {0.07f, 0.00f}, {0.12f, -0.05f}},
        {{0.00f, -0.10f}, {0.06f, 0.04f}, {-0.06f, 0.04f}}};
    private static final int[][][] LINES = {
        {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 2}},
        {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 0}},
        {{0, 1}, {1, 2}, {2, 3}},
        {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {5, 2}, {2, 6}, {4, 7}},
        {{0, 1}, {1, 2}, {1, 3}, {1, 4}, {2, 3}, {3, 4}},
        {{0, 1}, {1, 2}, {2, 0}, {2, 3}},
        {{0, 1}, {1, 2}, {2, 0}}};

    static boolean open;
    private static long openAt, closeAt, zoomAt, hoverAt;
    private static int[] from = {0, 0, 0, 0};
    private static int focus = -1, prevFocus = -1, hoverCon = -1, lastHoverCon = -1;
    private static final long OPEN = 900, CLOSE = 300, ZOOM = 650;
    private static final List<Object[]> HITS = new ArrayList<>();
    private static String lastHoverStar = "";
    private static final Map<String, Long> IGNITE = new HashMap<>();          // achievement id -> when its star ignites (earned since you last looked)

    // ------------------------------------------------------------------ open / close
    static void open(int[] btn, int chapter) {
        open = true; openAt = System.currentTimeMillis(); closeAt = 0; from = btn.clone(); IGNITE.clear(); ignitesPlanned = false;
        focus = chapter >= 1 && chapter <= ACH.length ? chapter - 1 : -1; prevFocus = focus; zoomAt = 0;
        Achievements.refresh(true);
        sound("sky_open", 1f, 0.8f);
    }
    static void close() { if (open && closeAt == 0) { closeAt = System.currentTimeMillis(); sound("sky_close", 1f, 0.6f); } }
    static boolean busy() { return open; }
    private static void fly(int c) { if (c == focus) return; prevFocus = focus; focus = c; zoomAt = System.currentTimeMillis(); sound("sky_zoom", c < 0 ? 0.85f : 1f, 0.7f); }

    /** Stars earned since the sky was last opened ignite one after another, once the warp has settled. */
    private static boolean ignitesPlanned;
    private static void planIgnites() {
        Achievements.Profile p = Achievements.profile(); if (p == null || ignitesPlanned) return;
        ignitesPlanned = true;
        FancyConfig c = FancyConfig.get(); long at = Math.max(now, openAt + OPEN + 150); int k = 0;
        for (Achievements.Ach a : p.list()) if (a.done() && !c.seenAch.contains(a.id())) { IGNITE.put(a.id(), at + k * 380L); c.seenAch.add(a.id()); k++; }
        if (k > 0) try { FancyConfig.save(); } catch (Throwable ignored) { }
    }

    private static void sound(String name, float pitch, float gain) { try { SoundFx.Fx fx = SoundFx.byName(name); if (fx == null) fx = SoundFx.byName("exp_flip"); if (fx != null) SoundFx.play(fx, pitch, gain); } catch (Throwable ignored) { } }
    private static float cl(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    private static float ease(float v) { v = cl(v); return v * v * v * (v * (v * 6 - 15) + 10); }
    private static int mix(int a, int b, float f) { return Canvas.mix(a, b, cl(f)); }
    private static int argb(float a, int rgb) { return Gfx.argb(Math.round(cl(a) * 255), rgb & 0xFFFFFF); }
    private static float rnd(int n) { return Ritual.rnd(n); }

    // ------------------------------------------------------------------ the sky and the camera
    private static int W, H, sx0, sy0, sw, sh;
    private static Font font; private static Host host; private static int mx, my; private static long now;
    private static float camU, camV, camS, warp;                // warp: 0..1, how hard the stars streak (opening, flying)
    private static float alpha = 1f;

    private static float aspect() { return sw / (float) sh; }
    private static float[] centre(int c) { return new float[]{HOME[c][0] * aspect(), HOME[c][1]}; }
    private static float X(float u) { return sx0 + sw / 2f + (u - camU) * sh * camS; }
    private static float Y(float v) { return sy0 + sh / 2f + (v - camV) * sh * camS; }
    private static boolean inSky(float x, float y) { return x >= sx0 + 2 && x < sx0 + sw - 2 && y >= sy0 + 2 && y < sy0 + sh - 2; }

    private static List<Achievements.Ach> achOf(String id) {
        Achievements.Profile p = Achievements.profile(); List<Achievements.Ach> l = new ArrayList<>();
        if (p != null) for (Achievements.Ach a : p.list()) if (a.ch().equals(id)) l.add(a);
        return l;
    }
    private static float[] star(int c, int k) {
        float[] o = centre(c); float[][] s0 = SHAPE[c];
        if (k < s0.length) return new float[]{o[0] + s0[k][0], o[1] + s0[k][1]};
        float an = k * 2.4f; return new float[]{o[0] + (float) Math.cos(an) * 0.15f, o[1] + (float) Math.sin(an) * 0.12f};
    }

    static void draw(GuiGraphicsExtractor g, Host h, int width, int height, int mx0, int my0) {
        if (!open) return;
        try { g.nextStratum(); } catch (Throwable ignored) { }                        // above everything the menu drew, its text included
        now = System.currentTimeMillis(); host = h; font = h.font(); W = width; H = height; mx = mx0; my = my0; HITS.clear();
        float t = now - openAt, closeT = closeAt == 0 ? -1f : now - closeAt;
        if (closeT >= CLOSE) { open = false; return; }
        float grow = closeT >= 0 ? 1f - ease(closeT / CLOSE) : ease(t / 420f);
        alpha = closeT >= 0 ? 1f - cl(closeT / (CLOSE * 0.8f)) : cl(t / 200f);
        planIgnites();

        // the sky opens out of the button: a window that grows to fill most of the screen
        int fw = Math.round(Math.min(W - 24, H * 2.0f)), fh = Math.round(Math.min(H - 24, fw / 1.7f));
        float bx = (from[0] + from[2]) / 2f, by = (from[1] + from[3]) / 2f, cxs = W / 2f, cys = H / 2f;
        float gx = bx + (cxs - bx) * grow, gy = by + (cys - by) * grow, gs = Math.max(0.02f, grow);
        sw = Math.round(fw * gs); sh = Math.round(fh * gs); sx0 = Math.round(gx - sw / 2f); sy0 = Math.round(gy - sh / 2f);
        g.fill(0, 0, W, H, argb(0.72f * alpha, 0x02010A));
        if (sw < 8 || sh < 8) return;

        // the camera
        float z = zoomAt == 0 ? 1f : ease((now - zoomAt) / (float) ZOOM);
        float[] a0 = camOf(prevFocus), a1 = camOf(focus);
        camU = a0[0] + (a1[0] - a0[0]) * z; camV = a0[1] + (a1[1] - a0[1]) * z; camS = a0[2] + (a1[2] - a0[2]) * (z * z * (3 - 2 * z));
        float zoomed = focus >= 0 ? (prevFocus >= 0 ? 1f : z) : (prevFocus >= 0 ? 1f - z : 0f);
        float flyWarp = zoomAt == 0 ? 0f : (float) Math.sin(Math.PI * cl((now - zoomAt) / (float) ZOOM)) * (prevFocus >= 0 && focus >= 0 ? 0.6f : 1f);
        warp = Math.max(closeT >= 0 ? 0f : 1f - ease((t - 120f) / (OPEN - 120f)), flyWarp * 0.7f);

        g.enableScissor(sx0, sy0, sx0 + sw, sy0 + sh);
        sky(g);
        streaks(g);
        hoverCon = -1;
        for (int c = 0; c < ACH.length; c++) constellation(g, c, zoomed);
        if (hoverCon != lastHoverCon) { lastHoverCon = hoverCon; hoverAt = now; if (hoverCon >= 0) sound("sky_star", 0.75f, 0.35f); }
        shootingStars(g);
        vignette(g);
        g.disableScissor();
        frame(g);
        if (alpha > 0.6f && closeT < 0) {
            float in = cl((t - 350f) / 400f);
            header(g, zoomed, in);
            if (focus >= 0 && zoomed > 0.5f) panel(g, focus, cl((zoomed - 0.5f) * 2f));
        }
    }
    private static float[] camOf(int c) {
        if (c < 0) return new float[]{aspect() / 2f, 0.5f, 1f};
        float[] o = centre(c); float top = 0f, bot = 0f;
        for (float[] q : SHAPE[c]) { top = Math.min(top, q[1]); bot = Math.max(bot, q[1]); }
        float s = Math.min(2.8f, 0.62f / Math.max(0.12f, bot - top + 0.08f)), mid = (top + bot) / 2f;
        return new float[]{o[0] - (aspect() * 0.17f) / s, o[1] + mid + 0.025f, s};
    }

    // ------------------------------------------------------------------ the deep sky
    private static void sky(GuiGraphicsExtractor g) {
        g.fill(sx0, sy0, sx0 + sw, sy0 + sh, 0xFF05060F);
        float par = 0.18f;                                                        // the deep sky moves less than the stars in front of it
        float ow = sw * (1.25f + 0.25f * (camS - 1f) * par), oh = sh * (1.25f + 0.25f * (camS - 1f) * par);
        float dx = (camU - aspect() / 2f) * sh * camS * par, dy = (camV - 0.5f) * sh * camS * par;
        float drift = (now % 240000L) / 240000f * sw * 0.06f;                    // and turns very slowly
        int x = Math.round(sx0 + sw / 2f - ow / 2f - dx - drift + sw * 0.03f), y = Math.round(sy0 + sh / 2f - oh / 2f - dy);
        if (!NebulaTex.draw(g, x, y, Math.round(ow), Math.round(oh))) {
            for (int i = 0; i < 24; i++) { int y0 = sy0 + sh * i / 24, y1 = sy0 + sh * (i + 1) / 24; g.fill(sx0, y0, sx0 + sw, y1, mix(0xFF0A1028, 0xFF1C1236, i / 24f)); }
        }
        if (alpha < 1f) g.fill(sx0, sy0, sx0 + sw, sy0 + sh, argb(1f - alpha, 0x05060F));
        for (int i = 0; i < 90; i++) {                                           // a near layer of stars that twinkle (and move a little more)
            float u = rnd(i * 13 + 1), v = rnd(i * 29 + 7), tw = (float) Math.max(0, Math.sin(now / (500.0 + rnd(i) * 1300) + i * 2.1));
            float px = (camU - aspect() / 2f) * sh * camS * 0.35f, py = (camV - 0.5f) * sh * camS * 0.35f;
            float sx = sx0 + (((u * sw - px) % sw) + sw) % sw, sy = sy0 + (((v * sh - py) % sh) + sh) % sh;
            int c = i % 6 == 0 ? 0xFFE6B8 : i % 4 == 0 ? 0xB8D0FF : 0xFFFFFF; float al = (0.25f + 0.75f * tw * tw) * alpha * (0.4f + 0.6f * rnd(i * 5));
            g.fill(Math.round(sx), Math.round(sy), Math.round(sx) + 1, Math.round(sy) + 1, argb(al, c));
            if (tw > 0.92f && i % 3 == 0) { g.fill(Math.round(sx) - 1, Math.round(sy), Math.round(sx) + 2, Math.round(sy) + 1, argb(al * 0.5f, c)); g.fill(Math.round(sx), Math.round(sy) - 1, Math.round(sx) + 1, Math.round(sy) + 2, argb(al * 0.5f, c)); }
        }
    }
    /** The warp: stars streak out from the middle (opening) or past you (flying in and out). */
    private static void streaks(GuiGraphicsExtractor g) {
        if (warp < 0.02f) return;
        float cx = sx0 + sw / 2f, cy = sy0 + sh / 2f;
        for (int i = 0; i < 140; i++) {
            float an = rnd(i * 7 + 3) * 6.2832f, sp = 0.3f + rnd(i * 11) * 0.7f;
            float ph = ((now / 700f) * sp + rnd(i * 5)) % 1f, r0 = ph * ph * Math.max(sw, sh) * 0.75f, len = (6f + 60f * sp) * warp * ph;
            float x0 = cx + (float) Math.cos(an) * r0, y0 = cy + (float) Math.sin(an) * r0 * 0.7f;
            int n = Math.max(1, Math.round(len)); int col = i % 5 == 0 ? 0xC9A0FF : i % 3 == 0 ? 0x9AD8FF : 0xFFFFFF;
            for (int k = 0; k < n; k += 1) {
                float x = x0 - (float) Math.cos(an) * k, y = y0 - (float) Math.sin(an) * k * 0.7f; if (!inSky(x, y)) break;
                g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, argb(warp * ph * (1f - k / (float) n) * 0.9f * alpha, col));
            }
        }
    }
    /** Two or three shooting stars a minute, with a fading tail. */
    private static void shootingStars(GuiGraphicsExtractor g) {
        for (int lane = 0; lane < 2; lane++) {
            long per = 9000L + lane * 4100L, st = (now + lane * 3333L) / per; float u = ((now + lane * 3333L) % per) / 900f; if (u > 1f) continue;
            float x0 = sx0 + sw * (0.15f + 0.6f * rnd((int) st * 3 + lane)), y0 = sy0 + sh * (0.05f + 0.35f * rnd((int) st + 9 + lane));
            float dxs = sw * 0.3f, dys = sh * 0.16f, x = x0 + u * dxs, y = y0 + u * dys;
            for (int k = 0; k < 40; k++) { float b = k / 40f; float tx = x - b * dxs * 0.25f, ty = y - b * dys * 0.25f; if (inSky(tx, ty)) g.fill(Math.round(tx), Math.round(ty), Math.round(tx) + 1, Math.round(ty) + 1, argb((1 - b) * (1 - b) * (1 - u * 0.6f) * alpha, k < 3 ? 0xFFFFFF : 0xCFE4FF)); }
            glow(g, x, y, 4, 0xCFE4FF, 0.5f * (1 - u));
        }
    }
    /** Darker towards the edges, like looking through a telescope. */
    private static void vignette(GuiGraphicsExtractor g) {
        for (int i = 0; i < 14; i++) {
            int in = Math.round(Math.min(sw, sh) * 0.012f * i), a = Math.round(18 * (1f - i / 14f) * alpha);
            g.fill(sx0, sy0 + in, sx0 + sw, sy0 + in + Math.max(1, Math.round(Math.min(sw, sh) * 0.012f)), Gfx.argb(a, 0));
            g.fill(sx0, sy0 + sh - in - Math.max(1, Math.round(Math.min(sw, sh) * 0.012f)), sx0 + sw, sy0 + sh - in, Gfx.argb(a, 0));
            g.fill(sx0 + in, sy0, sx0 + in + Math.max(1, Math.round(Math.min(sw, sh) * 0.012f)), sy0 + sh, Gfx.argb(a, 0));
            g.fill(sx0 + sw - in - Math.max(1, Math.round(Math.min(sw, sh) * 0.012f)), sy0, sx0 + sw - in, sy0 + sh, Gfx.argb(a, 0));
        }
    }
    /** A gold star-chart frame: double rule, degree ticks, corner ornaments. */
    private static void frame(GuiGraphicsExtractor g) {
        int c = argb(0.8f * alpha, 0xE8C76A), c2 = argb(0.35f * alpha, 0xE8C76A);
        g.fill(sx0 - 1, sy0 - 1, sx0 + sw + 1, sy0, c); g.fill(sx0 - 1, sy0 + sh, sx0 + sw + 1, sy0 + sh + 1, c); g.fill(sx0 - 1, sy0, sx0, sy0 + sh, c); g.fill(sx0 + sw, sy0, sx0 + sw + 1, sy0 + sh, c);
        g.fill(sx0 + 3, sy0 + 3, sx0 + sw - 3, sy0 + 4, c2); g.fill(sx0 + 3, sy0 + sh - 4, sx0 + sw - 3, sy0 + sh - 3, c2); g.fill(sx0 + 3, sy0 + 3, sx0 + 4, sy0 + sh - 3, c2); g.fill(sx0 + sw - 4, sy0 + 3, sx0 + sw - 3, sy0 + sh - 3, c2);
        for (int i = 1; i < 36; i++) { int x = sx0 + sw * i / 36, l = i % 9 == 0 ? 7 : i % 3 == 0 ? 4 : 2; g.fill(x, sy0, x + 1, sy0 + l, c); g.fill(x, sy0 + sh - l, x + 1, sy0 + sh, c); }
        for (int i = 1; i < 20; i++) { int y = sy0 + sh * i / 20, l = i % 5 == 0 ? 7 : i % 5 == 2 ? 3 : 2; g.fill(sx0, y, sx0 + l, y + 1, c); g.fill(sx0 + sw - l, y, sx0 + sw, y + 1, c); }
        int[][] cn = {{sx0, sy0}, {sx0 + sw, sy0}, {sx0, sy0 + sh}, {sx0 + sw, sy0 + sh}};
        for (int[] p : cn) { disc(g, p[0], p[1], 4, argb(alpha, 0x2A1C08)); ring(g, p[0], p[1], 4, c, 1f); disc(g, p[0], p[1], 1.5f, c); }
    }

    // ------------------------------------------------------------------ a constellation
    private static void constellation(GuiGraphicsExtractor g, int c, float zoomed) {
        List<Achievements.Ach> l = achOf(ACH[c]); int n = Math.max(l.size(), SHAPE[c].length);
        boolean other = focus >= 0 && c != focus; float fade = other ? 1f - 0.8f * zoomed : 1f;
        float[] o = centre(c); float ox = X(o[0]), oy = Y(o[1]);
        float rad = 0.16f * sh * camS;
        boolean hv = focus < 0 && zoomed < 0.05f && Math.hypot(mx - ox, my - oy) < rad;
        if (hv) hoverCon = c;
        float hk = hv ? ease((now - hoverAt) / 350f) : 0f;
        int done = 0; for (Achievements.Ach a : l) if (a.done()) done++;
        boolean full = !l.isEmpty() && done == l.size();
        int col = ACH_COL[c];
        float frac = l.isEmpty() ? 0f : done / (float) l.size();
        // the figure's own light: a soft cloud in its colour that grows with what you have, brighter when complete or hovered
        float cloud = (0.025f + 0.05f * frac + (full ? 0.04f : 0f) + 0.03f * hk) * fade;
        softBlob(g, ox, oy, rad * 1.15f, rad * 0.95f, col, cloud * alpha);
        // the progress ring round the figure (whole-sky view)
        if (zoomed < 0.6f) {
            float ra = (1f - zoomed / 0.6f) * fade;
            if (frac > 0) { ring(g, ox, oy, rad * 1.02f, argb(0.65f * ra * alpha, col), frac); ring(g, ox, oy, rad * 1.02f + 1, argb(0.3f * ra * alpha, col), frac); }
            if (hv) { float sp = (now / 2600f) % 1f; double an = -Math.PI / 2 + sp * Math.PI * 2; glow(g, ox + (float) Math.cos(an) * rad * 1.02f, oy + (float) Math.sin(an) * rad * 1.02f, 3, col, 0.7f * hk); }
        }
        // the lines: unlit ones are faint threads, lit ones burn in the figure's colour with a spark running along them; hovering traces the whole figure in white
        for (int ei = 0; ei < LINES[c].length; ei++) {
            int[] e = LINES[c][ei];
            if (e[0] >= n || e[1] >= n) continue;
            boolean lit = e[0] < l.size() && e[1] < l.size() && l.get(e[0]).done() && l.get(e[1]).done();
            float[] p0 = star(c, e[0]), p1 = star(c, e[1]);
            float x0 = X(p0[0]), y0 = Y(p0[1]), x1 = X(p1[0]), y1 = Y(p1[1]);
            if (lit) { line(g, x0, y0, x1, y1, col, 0.35f * fade, 1); line(g, x0, y0, x1, y1, mix(col, 0xFFFFFFFF, 0.5f), 0.85f * fade, 0); spark(g, x0, y0, x1, y1, col, fade, ei); }
            else line(g, x0, y0, x1, y1, 0x9AA6D0, (0.16f + 0.12f * (focus == c ? 1f : 0f)) * fade, 0);
            if (hk > 0f) { float tr = cl(hk * LINES[c].length - ei); if (tr > 0f) line(g, x0, y0, x0 + (x1 - x0) * tr, y0 + (y1 - y0) * tr, 0xFFFFFF, 0.45f * fade * (1f - 0.5f * hk + 0.5f), 0); }
        }
        for (int k = l.size(); k < SHAPE[c].length; k++) { float[] p = star(c, k); plainStar(g, X(p[0]), Y(p[1]), 0.25f * fade, k + c * 13, false); }
        for (int k = 0; k < l.size(); k++) starOf(g, c, k, l.get(k), fade, zoomed);
        if (zoomed < 0.6f && inSky(ox, oy + rad)) {                               // the name, spaced capitals, and a row of dots for its stars
            float la = (1f - zoomed / 0.6f) * fade * alpha;
            String nm = spaced(ACH_NAME[c].toUpperCase(Locale.ROOT));
            int ty = Math.round(oy + rad * 1.08f) + 2, nw = host.width(nm);
            text(g, nm, Math.round(ox - nw / 2f), ty, argb(la, hv || full ? mix(col, 0xFFFFFFFF, 0.4f) : 0xE6E8F4), true);
            int dots = l.size(), dw = dots * 5 - 2, dx0 = Math.round(ox - dw / 2f);
            for (int i = 0; i < dots; i++) { boolean on = l.get(i).done(); g.fill(dx0 + i * 5, ty + 12, dx0 + i * 5 + 3, ty + 15, argb(la * (on ? 1f : 0.35f), on ? col : 0x9AA6D0)); }
            if (hk > 0f) { String fg = FIGURE[c]; small(g, fg, Math.round(ox - host.width(fg) * 0.375f), ty + 19, argb(la * hk, col)); }
            if (focus < 0) HITS.add(new Object[]{Math.round(ox - rad), Math.round(oy - rad), Math.round(ox + rad), Math.round(oy + rad + 30), (Runnable) () -> fly(c)});
        }
    }
    private static void starOf(GuiGraphicsExtractor g, int c, int k, Achievements.Ach a, float fade, float zoomed) {
        float[] p = star(c, k); float x = X(p[0]), y = Y(p[1]);
        if (!inSky(x, y)) return;
        boolean near = focus == c && zoomed > 0.5f;
        float hr = near ? 12 : 6;
        boolean hv = Math.abs(mx - x) <= hr && Math.abs(my - y) <= hr && (near || focus < 0) && fade > 0.5f;
        float tw = 0.82f + 0.18f * (float) Math.sin(now / 380.0 + k * 1.7 + c * 0.9);
        int col = ACH_COL[c];
        Long ig = IGNITE.get(a.id()); float igT = ig == null ? 9f : (now - ig) / 1000f;
        boolean lit = a.done() && igT >= 0f;
        if (ig != null && igT >= 0f && igT < 0.05f && !igSounded.contains(a.id())) { igSounded.add(a.id()); sound("sky_star", 1.2f, 0.8f); }
        if (lit) {
            float r = (near ? 3.4f : 2.1f) * tw + (hv ? 1.2f : 0f);
            if (igT < 1.4f) {                                                     // ignition: a flash, a ring rushing out, rays that settle
                float u = igT / 1.4f;
                glow(g, x, y, r * (10f - 6f * u), 0xFFFFFF, 0.9f * (1f - u) * fade);
                ring(g, x, y, r * (2f + 22f * ease(u)), argb((1f - u) * 0.9f * fade * alpha, col), 1f);
                r *= 1f + 1.2f * (1f - u);
            }
            glow(g, x, y, r * 5.5f, col, 0.22f * fade);
            glow(g, x, y, r * 2.4f, mix(col, 0xFFFFFFFF, 0.5f), 0.38f * fade);
            float rays = r * (near ? 4.2f : 3.2f) * (0.85f + 0.15f * tw);
            spikes(g, x, y, rays, mix(col, 0xFFFFFFFF, 0.55f), 0.9f * fade, false);
            spikes(g, x, y, rays * 0.45f, mix(col, 0xFFFFFFFF, 0.3f), 0.5f * fade, true);
            disc(g, x, y, r * 0.8f, argb(fade * alpha, 0xFFFFFF));
            if (hv) ring(g, x, y, r * 3.2f + (float) Math.sin(now / 200.0) * 1.2f, argb(0.6f * fade * alpha, col), 1f);
        } else {
            float prog = (float) Math.min(1.0, a.goal() <= 0 ? 0 : a.progress() / a.goal());
            plainStar(g, x, y, (hv ? 0.9f : 0.55f) * fade, k + c * 13, near);
            if (near) { ring(g, x, y, 8f, argb(0.16f * fade * alpha, 0x9AA6D0), 1f); if (prog > 0f) { ring(g, x, y, 8f, argb(0.85f * fade * alpha, col), prog); ring(g, x, y, 8.8f, argb(0.35f * fade * alpha, col), prog); } }
            if (hv) ring(g, x, y, 6f, argb(0.5f * fade * alpha, 0xFFFFFF), 1f);
        }
        if (near) {
            String nm = a.secret() && !a.done() ? "???" : a.name();
            small(g, nm, Math.round(x - host.width(nm) * 0.375f), Math.round(y + 12), argb((lit ? 1f : 0.7f) * fade * alpha, lit ? 0xFFFFFF : 0xB8C0DA));
            CosmeticData.Def rw = a.reward().isEmpty() ? null : CosmeticData.byId(a.reward());
            if (rw != null) { String r = "✦ " + rw.name(); small(g, r, Math.round(x - host.width(r) * 0.375f), Math.round(y + 20), argb(0.85f * fade * alpha, lit ? col : 0x8A94B8)); }
        }
        if (hv && alpha > 0.6f) {
            if (!lastHoverStar.equals(a.id())) { lastHoverStar = a.id(); sound("sky_star", 0.9f + 0.05f * k, 0.45f); }
            String nm = a.secret() && !a.done() ? "???" : a.name();
            String rew = a.reward().isEmpty() ? "" : Ledger.rewardName(a.reward());
            String how = (a.secret() && !a.done() ? "A secret. Keep finding drops." : a.desc()) + (rew.isEmpty() ? "" : "  Unlocks the " + rew + ".");
            String extra = a.done() ? "Lit " + new java.text.SimpleDateFormat("d MMM yyyy").format(new Date(a.at())) : Ledger.fmt(a.progress()) + " / " + Ledger.fmt(a.goal());
            host.tip(nm + "\n" + ACH_NAME[c] + "\n" + (a.done() ? 3 : 0) + "\n" + (a.done() ? "1" : "0") + "\n" + extra + "\n" + how, a.done() ? col : 0xFF807891);
        }
    }
    private static final Set<String> igSounded = new HashSet<>();
    /** A star not yet earned: a real, small, cool-white star that twinkles. */
    private static void plainStar(GuiGraphicsExtractor g, float x, float y, float a, int seed, boolean big) {
        float tw = 0.7f + 0.3f * (float) Math.sin(now / (600.0 + rnd(seed) * 700) + seed);
        glow(g, x, y, big ? 5f : 3.5f, 0x9AB4FF, 0.18f * a * tw);
        spikes(g, x, y, big ? 4f : 2.5f, 0xC8D4FF, 0.35f * a * tw, false);
        disc(g, x, y, big ? 1.4f : 0.9f, argb(a * tw * alpha, 0xE6ECFF));
    }
    /** A bright spark that runs along a lit line now and then. */
    private static void spark(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int col, float fade, int seed) {
        float per = 2.6f + rnd(seed) * 1.6f, u = ((now / 1000f + rnd(seed * 3) * per) % per) / 1.1f; if (u > 1f) return;
        float x = x0 + (x1 - x0) * u, y = y0 + (y1 - y0) * u;
        glow(g, x, y, 3.5f, mix(col, 0xFFFFFFFF, 0.6f), 0.7f * fade * (float) Math.sin(Math.PI * u));
    }
    private static String spaced(String s) { StringBuilder b = new StringBuilder(); for (int i = 0; i < s.length(); i++) { if (i > 0) b.append(' '); b.append(s.charAt(i)); } return b.toString().replace("   ", "  "); }

    // ------------------------------------------------------------------ the panel of the constellation you flew into
    private static void panel(GuiGraphicsExtractor g, int c, float a) {
        int px0 = sx0 + 16, py0 = sy0 + 30, pw = Math.round(sw * 0.3f), ph = sh - 46;
        float slide = (1f - ease(a)) * -20f;
        Gfx.push(g); Gfx.translate(g, slide, 0);
        for (int i = 0; i < 8; i++) g.fill(px0 + pw + i, py0, px0 + pw + i + 1, py0 + ph, argb(0.06f * (1f - i / 8f) * a * alpha, 0));
        g.fill(px0, py0, px0 + pw, py0 + ph, argb(0.62f * a * alpha, 0x060818));
        g.fill(px0, py0, px0 + pw, py0 + 1, argb(0.5f * a * alpha, ACH_COL[c])); g.fill(px0, py0, px0 + 2, py0 + ph, argb(0.8f * a * alpha, ACH_COL[c]));
        int x = px0 + 12, y = py0 + 12, w = pw - 24;
        small(g, spaced(FIGURE[c].toUpperCase(Locale.ROOT)), x, y, argb(0.9f * a * alpha, ACH_COL[c]));
        Gfx.push(g); Gfx.translate(g, x, y + 9); Gfx.scale(g, 1.6f); g.text(font, ACH_NAME[c], 0, 0, argb(a * alpha, 0xFFFFFF), true); Gfx.pop(g);
        List<Achievements.Ach> l = achOf(ACH[c]); int d = 0; for (Achievements.Ach q : l) if (q.done()) d++;
        int sy = y + 30;
        text(g, d + " of " + l.size() + " stars lit", x, sy, argb(a * alpha, 0xD8DCEC), false);
        int by = sy + 13, seg = Math.max(1, l.size()), gap = 2, sgw = (w - gap * (seg - 1)) / seg;          // one segment per star
        for (int i = 0; i < seg; i++) { boolean on = i < l.size() && l.get(i).done(); int sx = x + i * (sgw + gap); g.fill(sx, by, sx + sgw, by + 4, argb((on ? 1f : 0.22f) * a * alpha, on ? ACH_COL[c] : 0x9AA6D0)); }
        Achievements.Profile p = Achievements.profile(); sy = by + 14;
        if (p != null) for (String[] row : Ledger.statRows(ACH[c], p)) {
            if (sy > py0 + ph - 30) break;
            text(g, row[0], x, sy, argb(0.8f * a * alpha, 0xB8C0DA), false); int vw = host.width(row[1]); text(g, row[1], x + w - vw, sy, argb(a * alpha, 0xFFFFFF), false);
            for (int dx = x + host.width(row[0]) + 3; dx < x + w - vw - 3; dx += 3) g.fill(dx, sy + 6, dx + 1, sy + 7, argb(0.25f * a * alpha, 0x8A94B8));
            sy += 11;
        }
        small(g, "“" + QUOTE[c] + "”", x, py0 + ph - 14, argb(0.75f * a * alpha, ACH_COL[c]));
        Gfx.pop(g);
    }

    // ------------------------------------------------------------------ the header
    private static void header(GuiGraphicsExtractor g, float zoomed, float in) {
        Achievements.Profile p = Achievements.profile();
        int all = 0, lit = 0; if (p != null) for (Achievements.Ach a : p.list()) { all++; if (a.done()) lit++; }
        float ia = in * alpha;
        int x = sx0 + 14, y = sy0 + 11;
        text(g, spaced("ACHIEVEMENTS"), x, y, argb(ia, 0xFFE9B8), true);
        String cnt = p == null ? (Achievements.status.isEmpty() ? "Reading the stars..." : Achievements.status) : lit + " of " + all + " stars lit";
        small(g, cnt, x + host.width(spaced("ACHIEVEMENTS")) + 10, y + 1, argb(0.8f * ia, 0xB8C0DA));
        if (p != null && all > 0) { int bw = host.width(spaced("ACHIEVEMENTS")); g.fill(x, y + 11, x + bw, y + 12, argb(0.2f * ia, 0xFFE9B8)); g.fill(x, y + 11, x + Math.round(bw * lit / (float) all), y + 12, argb(ia, 0xFFE9B8)); }
        if (zoomed < 0.3f && p != null) { String hint = "Click a constellation to fly in"; small(g, hint, sx0 + sw / 2 - Math.round(host.width(hint) * 0.375f), sy0 + 13, argb(0.55f * ia * (1f - zoomed / 0.3f), 0xB8C0DA)); }
        compass(g, sx0 + sw - 30, sy0 + sh - 30, 16, ia);
        int cx = sx0 + sw - 20, cy = sy0 + 9;
        boolean hc = mx >= cx - 2 && mx < cx + 12 && my >= cy - 2 && my < cy + 12;
        disc(g, cx + 5, cy + 5, 7, argb((hc ? 0.45f : 0.18f) * ia, 0xFFFFFF));
        for (int i = 2; i < 9; i++) { g.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, argb(ia, 0xFFFFFF)); g.fill(cx + 10 - i, cy + i, cx + 11 - i, cy + i + 1, argb(ia, 0xFFFFFF)); }
        HITS.add(new Object[]{cx - 3, cy - 3, cx + 14, cy + 14, (Runnable) Codex::close});
        if (focus >= 0) {
            String b = "‹  All constellations"; int bw = Math.round(host.width(b) * 0.75f) + 12, bx = sx0 + sw - 34 - bw, by = sy0 + 8;
            boolean hb = mx >= bx && mx < bx + bw && my >= by && my < by + 13;
            g.fill(bx, by, bx + bw, by + 13, argb((hb ? 0.32f : 0.14f) * ia, 0xFFFFFF)); g.fill(bx, by + 12, bx + bw, by + 13, argb(0.6f * ia, ACH_COL[focus]));
            small(g, b, bx + 6, by + 3, argb(ia, 0xFFFFFF));
            HITS.add(new Object[]{bx, by, bx + bw, by + 13, (Runnable) () -> fly(-1)});
        }
    }
    /** A small compass rose in the corner, turning very slowly. */
    private static void compass(GuiGraphicsExtractor g, int cx, int cy, int r, float a) {
        int c = argb(0.45f * a, 0xE8C76A);
        ring(g, cx, cy, r, c, 1f); ring(g, cx, cy, r - 3, argb(0.2f * a, 0xE8C76A), 1f);
        double rot = now / 20000.0;
        for (int k = 0; k < 8; k++) {
            double an = rot + k * Math.PI / 4; float len = k % 2 == 0 ? r - 1 : r * 0.55f;
            for (int i = 0; i < len; i++) { int x = Math.round(cx + (float) Math.cos(an) * i), y = Math.round(cy + (float) Math.sin(an) * i); g.fill(x, y, x + 1, y + 1, argb((k == 0 ? 0.9f : 0.45f) * a * (1f - i / len * 0.5f), k == 0 ? 0xFFE9B8 : 0xE8C76A)); }
        }
        disc(g, cx, cy, 1.5f, argb(a, 0xFFE9B8));
    }

    // ------------------------------------------------------------------ drawing helpers
    private static void text(GuiGraphicsExtractor g, String s, int x, int y, int c, boolean sh) { g.text(font, s, x, y, c, sh); }
    private static void small(GuiGraphicsExtractor g, String s, int x, int y, int c) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, 0.75f); g.text(font, s, 0, 0, c, false); Gfx.pop(g); }
    private static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
        if (r < 0.6f) { g.fill(Math.round(cx), Math.round(cy), Math.round(cx) + 1, Math.round(cy) + 1, c); return; }
        int ir = (int) Math.ceil(r);
        for (int dy = -ir; dy <= ir; dy++) { int hw = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy))); if (hw > 0) g.fill(Math.round(cx) - hw, Math.round(cy) + dy, Math.round(cx) + hw, Math.round(cy) + dy + 1, c); }
    }
    private static void glow(GuiGraphicsExtractor g, float cx, float cy, float r, int rgb, float a) {
        for (int k = 5; k >= 1; k--) disc(g, cx, cy, r * k / 5f, argb(a * alpha * 0.28f, rgb));
    }
    private static void softBlob(GuiGraphicsExtractor g, float cx, float cy, float rx, float ry, int rgb, float a) {
        for (int k = 6; k >= 1; k--) {
            float fx = rx * k / 6f, fy = ry * k / 6f; int step = 2;
            for (float dy = -fy; dy <= fy; dy += step) {
                float hw = fx * (float) Math.sqrt(Math.max(0, 1 - (dy / fy) * (dy / fy)));
                g.fill(Math.round(cx - hw), Math.round(cy + dy), Math.round(cx + hw), Math.round(cy + dy) + step, argb(a, rgb));
            }
        }
    }
    /** The thin rays of a star: four arms (or the four diagonals). */
    private static void spikes(GuiGraphicsExtractor g, float cx, float cy, float len, int rgb, float a, boolean diag) {
        int x = Math.round(cx), y = Math.round(cy), L = Math.round(len);
        for (int i = 1; i <= L; i++) {
            float f = 1f - i / (float) (L + 1); int c = argb(a * alpha * f * f, rgb);
            if (diag) { g.fill(x + i, y + i, x + i + 1, y + i + 1, c); g.fill(x - i, y - i, x - i + 1, y - i + 1, c); g.fill(x + i, y - i, x + i + 1, y - i + 1, c); g.fill(x - i, y + i, x - i + 1, y + i + 1, c); }
            else { g.fill(x + i, y, x + i + 1, y + 1, c); g.fill(x - i, y, x - i + 1, y + 1, c); g.fill(x, y + i, x + 1, y + i + 1, c); g.fill(x, y - i, x + 1, y - i + 1, c); }
        }
    }
    private static void ring(GuiGraphicsExtractor g, float cx, float cy, float r, int c, float upto) {
        int n = Math.max(24, Math.round(r * 6));
        for (int i = 0; i < n; i++) { float u = i / (float) n; if (u > upto) break; double an = -Math.PI / 2 + u * Math.PI * 2; int x = Math.round(cx + (float) Math.cos(an) * r), y = Math.round(cy + (float) Math.sin(an) * r); g.fill(x, y, x + 1, y + 1, c); }
    }
    /** A line of single pixels (width 0) or a soft 3-pixel band (width 1), stopping short of the stars at both ends. */
    private static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int rgb, float a, int width) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0); int n = Math.max(1, Math.round(len));
        float nx = -(y1 - y0) / Math.max(1f, len), ny = (x1 - x0) / Math.max(1f, len);
        for (int i = 4; i < n - 3; i++) {
            float u = i / (float) n, x = x0 + (x1 - x0) * u, y = y0 + (y1 - y0) * u;
            if (!inSky(x, y)) continue;
            g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, argb(a * alpha, rgb));
            if (width > 0) { g.fill(Math.round(x + nx), Math.round(y + ny), Math.round(x + nx) + 1, Math.round(y + ny) + 1, argb(a * 0.5f * alpha, rgb)); g.fill(Math.round(x - nx), Math.round(y - ny), Math.round(x - nx) + 1, Math.round(y - ny) + 1, argb(a * 0.5f * alpha, rgb)); }
        }
    }

    // ------------------------------------------------------------------ input
    static boolean click(double x, double y) { return click(x, y, 0); }
    static boolean click(double x, double y, int button) {
        if (!open) return false;
        if (closeAt != 0) return true;
        if (button == 1) { if (focus >= 0) fly(-1); else close(); return true; }
        if (button != 0) return true;
        for (int i = HITS.size() - 1; i >= 0; i--) { Object[] h = HITS.get(i); if (x >= (int) h[0] && x < (int) h[2] && y >= (int) h[1] && y < (int) h[3]) { ((Runnable) h[4]).run(); return true; } }
        boolean onSky = x >= sx0 && x < sx0 + sw && y >= sy0 && y < sy0 + sh;
        if (!onSky) close();
        return true;
    }
    static boolean scroll(double dy) {
        if (!open) return false;
        if (dy != 0 && focus >= 0) fly(Math.floorMod(focus + (dy < 0 ? 1 : -1), ACH.length));
        return true;
    }
    static boolean key(int key) {
        if (!open) return false;
        if (key == 256) { if (focus >= 0) fly(-1); else close(); }
        else if (key == 262) fly(focus < 0 ? 0 : Math.floorMod(focus + 1, ACH.length));
        else if (key == 263) fly(focus < 0 ? ACH.length - 1 : Math.floorMod(focus - 1, ACH.length));
        return true;
    }
}
