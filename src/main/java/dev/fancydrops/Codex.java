package dev.fancydrops;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.*;

/**
 * Achievements as a star map. Every chapter of the ledger is a constellation in one deep sky, every achievement is a star in it.
 * Earned stars burn in the chapter's colour, and the figure's lines light up between two earned stars, so a finished chapter draws
 * its whole picture. Bottom left, someone sits on a hill with a telescope and follows the constellation under your mouse.
 *
 * Lights are one soft-glow picture each and lines are single rotated bars ({@link NebulaTex}), so the whole sky costs a few hundred
 * draws a frame. Opening warps in out of the button; click a constellation to fly in (names, progress rings, rewards, the chapter's
 * numbers); right click, Esc or "All constellations" flies out; arrows and the wheel move between constellations.
 */
final class Codex {
    private Codex() {}

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
    private static final float[][] HOME = {{0.14f, 0.30f}, {0.36f, 0.22f}, {0.58f, 0.20f}, {0.67f, 0.54f}, {0.45f, 0.57f}, {0.84f, 0.75f}, {0.27f, 0.58f}};      // fractions of the width, of the height

    static boolean open;
    private static long openAt, closeAt, zoomAt, hoverAt, blinkAt;
    private static String blinkId = "";
    private static int[] from = {0, 0, 0, 0};
    private static int focus = -1, prevFocus = -1, hoverCon = -1, lastHoverCon = -1;
    private static final long OPEN = 900, CLOSE = 300, ZOOM = 650;
    private static final List<Object[]> HITS = new ArrayList<>();
    private static String lastHoverStar = "";
    private static final Map<String, Long> IGNITE = new HashMap<>();
    private static final Set<String> igSounded = new HashSet<>();
    private static boolean ignitesPlanned;
    private static float scopeAng = -0.9f;                                       // where the stargazer's telescope points (radians)

    // ------------------------------------------------------------------ open / close
    static void open(int[] btn, int chapter) {
        open = true; openAt = System.currentTimeMillis(); closeAt = 0; from = btn.clone(); IGNITE.clear(); igSounded.clear(); ignitesPlanned = false; blinkId = "";
        focus = chapter >= 1 && chapter <= ACH.length ? chapter - 1 : -1; prevFocus = focus; zoomAt = 0;
        Achievements.refresh(true);
        sound("sky_open", 1f, 0.8f);
    }
    /** Opens straight on one achievement: flies to its constellation and makes its star blink. */
    static void openAt(int[] btn, String achId) {
        Achievements.Profile p = Achievements.profile(); int c = -1;
        if (p != null) for (Achievements.Ach a : p.list()) if (a.id().equals(achId)) for (int i = 0; i < ACH.length; i++) if (ACH[i].equals(a.ch())) c = i;
        open(btn, -1);
        if (c >= 0) { prevFocus = -1; focus = c; zoomAt = openAt + OPEN - 200; }
        blinkId = achId; blinkAt = openAt + OPEN + 200;
    }
    static void close() { if (open && closeAt == 0) { closeAt = System.currentTimeMillis(); sound("sky_close", 1f, 0.6f); } }
    static boolean busy() { return open; }
    private static void fly(int c) { if (c == focus) return; prevFocus = focus; focus = c; zoomAt = System.currentTimeMillis(); sound("sky_zoom", c < 0 ? 0.85f : 1f, 0.7f); }

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
    private static float camU, camV, camS, warp, alpha = 1f;
    private static GuiGraphicsExtractor G;

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
    static void draw(GuiGraphicsExtractor g, Host h, int width, int height, int mx0, int my0) {
        if (!open) return;
        try { g.nextStratum(); } catch (Throwable ignored) { }                        // above everything the menu drew, its text included
        G = g; now = System.currentTimeMillis(); host = h; font = h.font(); W = width; H = height; mx = mx0; my = my0; HITS.clear();
        float t = now - openAt, closeT = closeAt == 0 ? -1f : now - closeAt;
        if (closeT >= CLOSE) { open = false; return; }
        float grow = closeT >= 0 ? 1f - ease(closeT / CLOSE) : ease(t / 420f);
        alpha = closeT >= 0 ? 1f - cl(closeT / (CLOSE * 0.8f)) : cl(t / 200f);
        planIgnites();

        int fw = Math.round(Math.min(W - 24, H * 2.0f)), fh = Math.round(Math.min(H - 24, fw / 1.7f));
        float bx = (from[0] + from[2]) / 2f, by = (from[1] + from[3]) / 2f, cxs = W / 2f, cys = H / 2f;
        float gx = bx + (cxs - bx) * grow, gy = by + (cys - by) * grow, gs = Math.max(0.02f, grow);
        sw = Math.round(fw * gs); sh = Math.round(fh * gs); sx0 = Math.round(gx - sw / 2f); sy0 = Math.round(gy - sh / 2f);
        g.fill(0, 0, W, H, argb(0.72f * alpha, 0x02010A));
        if (sw < 8 || sh < 8) return;

        float z = zoomAt == 0 || now < zoomAt ? (zoomAt > now ? 0f : 1f) : ease((now - zoomAt) / (float) ZOOM);
        int pf = zoomAt > now ? -1 : prevFocus, fc = focus;
        float[] a0 = camOf(zoomAt > now ? -1 : prevFocus), a1 = camOf(zoomAt > now ? -1 : focus);
        if (zoomAt > now) { a0 = camOf(-1); a1 = a0; }
        camU = a0[0] + (a1[0] - a0[0]) * z; camV = a0[1] + (a1[1] - a0[1]) * z; camS = a0[2] + (a1[2] - a0[2]) * (z * z * (3 - 2 * z));
        float zoomed = zoomAt > now ? 0f : fc >= 0 ? (pf >= 0 ? 1f : z) : (pf >= 0 ? 1f - z : 0f);
        float flyWarp = zoomAt == 0 || now < zoomAt ? 0f : (float) Math.sin(Math.PI * cl((now - zoomAt) / (float) ZOOM)) * (pf >= 0 && fc >= 0 ? 0.6f : 1f);
        warp = Math.max(closeT >= 0 ? 0f : 1f - ease((t - 120f) / (OPEN - 120f)), flyWarp * 0.7f);
        int showFocus = zoomAt > now ? -1 : fc;

        g.enableScissor(sx0, sy0, sx0 + sw, sy0 + sh);
        sky(g);
        chart(g);
        streaks(g);
        hoverCon = -1;
        for (int c = 0; c < ACH.length; c++) constellation(g, c, zoomed, showFocus);
        if (hoverCon != lastHoverCon) { lastHoverCon = hoverCon; hoverAt = now; if (hoverCon >= 0) sound("sky_star", 0.75f, 0.35f); }
        shootingStars(g);
        stargazer(g, 1f - cl(zoomed * 2.5f));
        vignette(g);
        g.disableScissor();
        frame(g);
        if (alpha > 0.6f && closeT < 0) {
            float in = cl((t - 350f) / 400f);
            header(g, zoomed, in, showFocus);
            if (showFocus >= 0 && zoomed > 0.5f) panel(g, showFocus, cl((zoomed - 0.5f) * 2f));
            cards(g, in * (1f - cl(zoomed * 3f)));
        }
    }
    private static float[] camOf(int c) {
        if (c < 0) return new float[]{aspect() / 2f, 0.5f, 1f};
        float[] o = centre(c); float top = -0.06f, bot = 0.06f;
        for (Node n : tree(c)) { top = Math.min(top, n.dv()); bot = Math.max(bot, n.dv()); }
        float s = Math.min(2.6f, 0.6f / Math.max(0.12f, bot - top + 0.12f)), mid = (top + bot) / 2f;
        return new float[]{o[0] - (aspect() * 0.17f) / s, o[1] + mid + 0.02f, s};
    }

    // ------------------------------------------------------------------ the deep sky
    private static void sky(GuiGraphicsExtractor g) {
        g.fill(sx0, sy0, sx0 + sw, sy0 + sh, 0xFF05060F);
        float par = 0.18f;
        float ow = sw * (1.25f + 0.25f * (camS - 1f) * par), oh = sh * (1.25f + 0.25f * (camS - 1f) * par);
        float dx = (camU - aspect() / 2f) * sh * camS * par, dy = (camV - 0.5f) * sh * camS * par;
        float drift = (now % 240000L) / 240000f * sw * 0.06f;
        int x = Math.round(sx0 + sw / 2f - ow / 2f - dx - drift + sw * 0.03f), y = Math.round(sy0 + sh / 2f - oh / 2f - dy);
        if (!NebulaTex.draw(g, x, y, Math.round(ow), Math.round(oh))) for (int i = 0; i < 24; i++) { int y0 = sy0 + sh * i / 24, y1 = sy0 + sh * (i + 1) / 24; g.fill(sx0, y0, sx0 + sw, y1, mix(0xFF0A1028, 0xFF1C1236, i / 24f)); }
        if (alpha < 1f) g.fill(sx0, sy0, sx0 + sw, sy0 + sh, argb(1f - alpha, 0x05060F));
        float px = (camU - aspect() / 2f) * sh * camS * 0.35f, py = (camV - 0.5f) * sh * camS * 0.35f;
        for (int i = 0; i < 70; i++) {                                           // a near layer of stars that twinkle (and move a little more)
            float u = rnd(i * 13 + 1), v = rnd(i * 29 + 7), tw = (float) Math.max(0, Math.sin(now / (500.0 + rnd(i) * 1300) + i * 2.1));
            float sx = sx0 + (((u * sw - px) % sw) + sw) % sw, sy = sy0 + (((v * sh - py) % sh) + sh) % sh;
            int c = i % 6 == 0 ? 0xFFE6B8 : i % 4 == 0 ? 0xB8D0FF : 0xFFFFFF; float al = (0.25f + 0.75f * tw * tw) * alpha * (0.4f + 0.6f * rnd(i * 5));
            g.fill(Math.round(sx), Math.round(sy), Math.round(sx) + 1, Math.round(sy) + 1, argb(al, c));
            if (tw > 0.93f && i % 4 == 0) glow(sx + 0.5f, sy + 0.5f, 3f, c, al * 0.6f);
        }
    }
    private static void streaks(GuiGraphicsExtractor g) {
        if (warp < 0.02f) return;
        float cx = sx0 + sw / 2f, cy = sy0 + sh / 2f;
        for (int i = 0; i < 110; i++) {
            float an = rnd(i * 7 + 3) * 6.2832f, sp = 0.3f + rnd(i * 11) * 0.7f;
            float ph = ((now / 700f) * sp + rnd(i * 5)) % 1f, r0 = ph * ph * Math.max(sw, sh) * 0.75f, len = (6f + 70f * sp) * warp * ph;
            float x0 = cx + (float) Math.cos(an) * r0, y0 = cy + (float) Math.sin(an) * r0 * 0.7f;
            int col = i % 5 == 0 ? 0xC9A0FF : i % 3 == 0 ? 0x9AD8FF : 0xFFFFFF;
            streak(x0, y0, x0 - (float) Math.cos(an) * len, y0 - (float) Math.sin(an) * len * 0.7f, col, warp * ph * 0.9f);
        }
    }
    private static void shootingStars(GuiGraphicsExtractor g) {
        for (int lane = 0; lane < 2; lane++) {
            long per = 9000L + lane * 4100L, st = (now + lane * 3333L) / per; float u = ((now + lane * 3333L) % per) / 900f; if (u > 1f) continue;
            float x0 = sx0 + sw * (0.25f + 0.6f * rnd((int) st * 3 + lane)), y0 = sy0 + sh * (0.05f + 0.3f * rnd((int) st + 9 + lane));
            float dxs = sw * 0.3f, dys = sh * 0.16f, x = x0 + u * dxs, y = y0 + u * dys;
            streak(x, y, x - dxs * 0.22f, y - dys * 0.22f, 0xCFE4FF, (1 - u * 0.6f));
            glow(x, y, 5, 0xCFE4FF, 0.7f * (1 - u));
        }
    }
    private static void vignette(GuiGraphicsExtractor g) {
        int b = Math.max(1, Math.round(Math.min(sw, sh) * 0.012f));
        for (int i = 0; i < 12; i++) {
            int in = b * i, a = Math.round(20 * (1f - i / 12f) * alpha);
            g.fill(sx0, sy0 + in, sx0 + sw, sy0 + in + b, Gfx.argb(a, 0)); g.fill(sx0, sy0 + sh - in - b, sx0 + sw, sy0 + sh - in, Gfx.argb(a, 0));
            g.fill(sx0 + in, sy0, sx0 + in + b, sy0 + sh, Gfx.argb(a, 0)); g.fill(sx0 + sw - in - b, sy0, sx0 + sw - in, sy0 + sh, Gfx.argb(a, 0));
        }
    }
    private static void frame(GuiGraphicsExtractor g) {
        int c = argb(0.8f * alpha, 0xE8C76A), c2 = argb(0.35f * alpha, 0xE8C76A);
        g.fill(sx0 - 1, sy0 - 1, sx0 + sw + 1, sy0, c); g.fill(sx0 - 1, sy0 + sh, sx0 + sw + 1, sy0 + sh + 1, c); g.fill(sx0 - 1, sy0, sx0, sy0 + sh, c); g.fill(sx0 + sw, sy0, sx0 + sw + 1, sy0 + sh, c);
        g.fill(sx0 + 3, sy0 + 3, sx0 + sw - 3, sy0 + 4, c2); g.fill(sx0 + 3, sy0 + sh - 4, sx0 + sw - 3, sy0 + sh - 3, c2); g.fill(sx0 + 3, sy0 + 3, sx0 + 4, sy0 + sh - 3, c2); g.fill(sx0 + sw - 4, sy0 + 3, sx0 + sw - 3, sy0 + sh - 3, c2);
        for (int i = 1; i < 36; i++) { int x = sx0 + sw * i / 36, l = i % 9 == 0 ? 7 : i % 3 == 0 ? 4 : 2; g.fill(x, sy0, x + 1, sy0 + l, c); g.fill(x, sy0 + sh - l, x + 1, sy0 + sh, c); }
        for (int i = 1; i < 20; i++) { int y = sy0 + sh * i / 20, l = i % 5 == 0 ? 7 : i % 5 == 2 ? 3 : 2; g.fill(sx0, y, sx0 + l, y + 1, c); g.fill(sx0 + sw - l, y, sx0 + sw, y + 1, c); }
        int[][] cn = {{sx0, sy0}, {sx0 + sw, sy0}, {sx0, sy0 + sh}, {sx0 + sw, sy0 + sh}};
        for (int[] p : cn) { disc(g, p[0], p[1], 4, argb(alpha, 0x2A1C08)); ring(p[0], p[1], 4, c, 1f); disc(g, p[0], p[1], 1.5f, c); }
    }

    // ------------------------------------------------------------------ the stargazer: on a hill, bottom left, with a telescope that follows your mouse
    private static void stargazer(GuiGraphicsExtractor g, float a) {
        if (a < 0.02f) return;
        float k = sh / 150f, gnd = sy0 + sh, hillW = sw * 0.36f;
        int sil = argb(a * alpha, 0x05040A), rim = argb(0.55f * a * alpha, 0x8A6AD8);
        for (int i = 0; i <= Math.round(hillW); i += 2) {                       // the hill, its top edge caught by the sky's light, a few grass tufts
            float u = i / hillW, hgt = sh * 0.17f * (1f - u * u) * (0.95f + 0.05f * (float) Math.sin(i * 0.15f)) + 3;
            int x = sx0 + i, top = Math.round(gnd - hgt);
            g.fill(x, top, x + 2, Math.round(gnd), sil); g.fill(x, top - 1, x + 2, top, rim);
            if (i % 14 == 6) { float sway = (float) Math.sin(now / 900.0 + i) * 1.2f; line(x + 1, top, x + 1 + sway, top - 4 * k, 0x05040A, a * alpha, 0); }
        }
        float hx = sx0 + sw * 0.12f, hy = gnd - sh * 0.17f * (1f - 0.333f * 0.333f) + 1;   // where they sit (the hilltop)
        // the lantern beside them: warm light on the grass, a flame that flickers
        float lx = hx - 16 * k, ly = hy - 3 * k, fl = 0.8f + 0.2f * (float) Math.sin(now / 90.0) * (float) Math.sin(now / 37.0);
        glow(lx, ly, 26 * k * fl, 0xFFB050, 0.35f * a); glow(lx, ly, 8 * k, 0xFFD890, 0.8f * a * fl);
        bar(lx - 1.5f * k, ly - 2.5f * k, lx + 1.5f * k, ly + 2.5f * k, 0x1A120A, a);
        disc(g, lx, ly, 1.2f * k, argb(a * alpha * fl, 0xFFE8A0));
        // the telescope turns to the constellation under your mouse, or sweeps slowly
        float apX = hx + 9f * k, apY = hy - 12.5f * k, want;                                 // the tripod stands still; the tube turns on its head
        line(apX, apY, apX - 4.5f * k, hy, 0x05040A, a, 1); line(apX, apY, apX + 4.5f * k, hy, 0x05040A, a, 1); line(apX, apY, apX + 0.6f * k, hy + 0.5f * k, 0x05040A, a, 1);
        if (hoverCon >= 0) { float[] o = centre(hoverCon); want = (float) Math.atan2(Y(o[1]) - apY, X(o[0]) - apX); }
        else want = -0.95f + 0.25f * (float) Math.sin(now / 5000.0);
        want = Math.max(-2.6f, Math.min(-0.15f, want));
        scopeAng += (want - scopeAng) * 0.08f;
        float ca = (float) Math.cos(scopeAng), sa = (float) Math.sin(scopeAng), len = 15 * k;
        float eyeX = apX - ca * len * 0.4f, eyeY = apY - sa * len * 0.4f, tx1 = apX + ca * len * 0.6f, ty1 = apY + sa * len * 0.6f;
        thick(eyeX, eyeY, tx1, ty1, 1.7f * k, 0x0A0812, a); thick(eyeX, eyeY, tx1, ty1, 0.5f * k, 0x3A2A5A, a * 0.6f);                                                    // the tube, a sheen along it
        thick(tx1 - ca * 1.5f * k, ty1 - sa * 1.5f * k, tx1, ty1, 2.4f * k, 0x0A0812, a);
        disc(g, apX, apY, 1.1f * k, argb(a * alpha, 0x1A1424));
        glow(tx1, ty1, 4 * k, 0xBFD8FF, (hoverCon >= 0 ? 0.55f : 0.25f) * a);                                                                                              // starlight in the lens
        // the person: sitting, knees up, leaning back to the eyepiece
        thick(hx, hy - 1.5f * k, hx + 1.5f * k, hy - 11 * k, 5.2f * k, 0x05040A, a);                  // back
        thick(hx + 1.5f * k, hy - 2.5f * k, hx + 7 * k, hy - 7.5f * k, 3.2f * k, 0x05040A, a);        // thigh
        thick(hx + 7 * k, hy - 7.5f * k, hx + 9.5f * k, hy - 1 * k, 2.8f * k, 0x05040A, a);           // shin
        thick(hx + 1.5f * k, hy - 10 * k, eyeX, eyeY + 1.5f * k, 2.2f * k, 0x05040A, a);              // arm up to the scope
        float headX = hx + 3 * k, headY = hy - 14.5f * k;
        disc(g, headX, headY, 3.1f * k, sil); disc(g, headX - 0.6f * k, headY - 0.8f * k, 3.1f * k, sil);
        float wind = (float) Math.sin(now / 700.0) * 1.2f;                                               // a scarf end lifting in the wind
        thick(hx + 1 * k, hy - 11.5f * k, hx - 4 * k + wind, hy - 10.5f * k - wind * 0.5f, 1.6f * k, 0x05040A, a);
        line(headX - 2.6f * k, headY - 2.2f * k, headX + 1f * k, headY - 3.4f * k, 0x8A6AD8, 0.45f * a, 0);                                                              // rim light on the hair
        line(hx - 1.6f * k, hy - 3 * k, hx - 0.6f * k, hy - 11 * k, 0x8A6AD8, 0.35f * a, 0);
        if (hoverCon >= 0) {                                                                         // a faint sight line to what they look at
            float[] o = centre(hoverCon); float ox = X(o[0]), oy = Y(o[1]), d = (float) Math.hypot(ox - tx1, oy - ty1);
            float ha = ease((now - hoverAt) / 400f);
            for (int i = 0; i < 30; i++) { float u = (i + ((now / 60) % 3) / 3f) / 30f; if (u * d < 6) continue; float xx = tx1 + (ox - tx1) * u, yy = ty1 + (oy - ty1) * u; G.fill(Math.round(xx), Math.round(yy), Math.round(xx) + 1, Math.round(yy) + 1, argb(0.25f * ha * a * (1f - u) * alpha, ACH_COL[hoverCon])); }
        }
    }

    // ------------------------------------------------------------------ the skill tree of a constellation
    /** One node of a chapter's tree: an achievement, where it sits (sky units from the chapter centre), the node before it on its branch (-1 = the heart). */
    private record Node(Achievements.Ach a, float du, float dv, int parent, int depth) {}
    private static final Map<String, List<Node>> TREES = new HashMap<>();
    private static Object treesFor;
    /** Every chapter's tree: one branch per kind of goal (achievements with the same icon), each branch a chain from the smallest goal to the largest. */
    private static List<Node> tree(int c) {
        Achievements.Profile p = Achievements.profile();
        if (treesFor != p) { TREES.clear(); treesFor = p; }
        return TREES.computeIfAbsent(ACH[c], id -> {
            List<Achievements.Ach> l = achOf(id); LinkedHashMap<String, List<Achievements.Ach>> br = new LinkedHashMap<>();
            for (Achievements.Ach a : l) br.computeIfAbsent(a.icon() == null ? a.id() : a.icon(), k -> new ArrayList<>()).add(a);
            for (List<Achievements.Ach> b : br.values()) b.sort(Comparator.comparingDouble(Achievements.Ach::goal));
            List<Node> out = new ArrayList<>(); int nb = br.size(), bi = 0;
            for (List<Achievements.Ach> b : br.values()) {
                double base = -Math.PI / 2 + (nb == 1 ? 0 : (bi - (nb - 1) / 2.0) * Math.min(1.25, 5.6 / nb)) + (nb > 4 ? 0 : 0);
                if (nb >= 5) base = -Math.PI / 2 + bi * Math.PI * 2 / nb;                      // many branches: all the way round
                int prev = -1;
                for (int d = 0; d < b.size(); d++) {
                    double an = base + d * 0.2 * (bi % 2 == 0 ? 1 : -1), r = 0.065 + 0.055 * d;
                    out.add(new Node(b.get(d), (float) (Math.cos(an) * r * 1.15), (float) (Math.sin(an) * r), prev, d));
                    prev = out.size() - 1;
                }
                bi++;
            }
            return out;
        });
    }

    private static void constellation(GuiGraphicsExtractor g, int c, float zoomed, int fc) {
        List<Node> t = tree(c); List<Achievements.Ach> l = achOf(ACH[c]);
        boolean other = fc >= 0 && c != fc; float fade = other ? 1f - 0.8f * zoomed : 1f;
        float[] o = centre(c); float ox = X(o[0]), oy = Y(o[1]);
        float rad = 0.16f * sh * camS;
        boolean hv = fc < 0 && zoomed < 0.05f && Math.hypot(mx - ox, my - oy) < rad;
        if (hv) hoverCon = c;
        float hk = hv ? ease((now - hoverAt) / 350f) : 0f;
        int done = 0; for (Achievements.Ach a : l) if (a.done()) done++;
        boolean full = !l.isEmpty() && done == l.size(), any = done > 0;
        int col = ACH_COL[c]; float frac = l.isEmpty() ? 0f : done / (float) l.size();
        float breathe = 0.85f + 0.15f * (float) Math.sin(now / 1800.0 + c);
        NebulaTex.glow(g, ox, oy, rad * (1.2f + 0.25f * frac), rad * (1.0f + 0.2f * frac), argb((0.08f + 0.2f * frac + (full ? 0.15f : 0f) + 0.1f * hk) * fade * alpha * breathe, col));
        if (full && zoomed < 0.6f) { float sp = (now / 6000f) % 1f, ra = (1f - zoomed / 0.6f) * fade; for (int i = 0; i < 6; i++) { double an = (sp + i / 6f) * Math.PI * 2; glow(ox + (float) Math.cos(an) * rad, oy + (float) Math.sin(an) * rad * 0.9f, 3, col, 0.6f * ra); } }
        // the branches: a path lights up from the heart outward as you earn its nodes; the next node to earn sits at the end of the lit path
        for (int i = 0; i < t.size(); i++) {
            Node n = t.get(i); float x1 = X(o[0] + n.du()), y1 = Y(o[1] + n.dv());
            float x0 = n.parent() < 0 ? ox : X(o[0] + t.get(n.parent()).du()), y0 = n.parent() < 0 ? oy : Y(o[1] + t.get(n.parent()).dv());
            boolean from = n.parent() < 0 || t.get(n.parent()).a().done(), lit = from && n.a().done();
            if (lit) { line(x0, y0, x1, y1, col, 0.3f * fade, 2); line(x0, y0, x1, y1, mix(col, 0xFFFFFFFF, 0.55f), 0.9f * fade, 0); spark(x0, y0, x1, y1, col, fade, i + c * 9); }
            else if (from) { dashed(x0, y0, x1, y1, col, 0.55f * fade); }                       // the way to the next node: dashed, in the chapter's colour
            else line(x0, y0, x1, y1, 0x8A96C0, 0.16f * fade, 0);
            if (hk > 0f) { float tr = cl(hk * t.size() - i); if (tr > 0f) line(x0, y0, x0 + (x1 - x0) * tr, y0 + (y1 - y0) * tr, 0xFFFFFF, 0.45f * fade, 1); }
        }
        heart(g, c, ox, oy, any, full, fade, zoomed, fc);
        for (int i = 0; i < t.size(); i++) { Node n = t.get(i); boolean open = n.parent() < 0 || t.get(n.parent()).a().done(); nodeOf(g, c, n, open, fade, zoomed, fc); }
        if (zoomed < 0.6f && inSky(ox, oy)) {
            float la = (1f - zoomed / 0.6f) * fade * alpha;
            String nm = spaced(ACH_NAME[c].toUpperCase(Locale.ROOT));
            float bot = 0.04f; for (Node n : t) bot = Math.max(bot, n.dv());
            int ty = Math.round(Y(o[1] + bot)) + 9, nw = host.width(nm);
            text(g, nm, Math.round(ox - nw / 2f), ty, argb(la, hv || full ? mix(col, 0xFFFFFFFF, 0.4f) : 0xE6E8F4), true);
            String sub = done + " / " + l.size(); int subW = Math.round(host.width(sub) * 0.75f);
            small(g, sub, Math.round(ox - subW / 2f), ty + 11, argb(la * 0.8f, full ? col : 0xA8B2D0));
            if (full) small(g, "✦ COMPLETE ✦", Math.round(ox - host.width("✦ COMPLETE ✦") * 0.375f), ty - 9, argb(la, col));
            if (hk > 0f) { String fg = FIGURE[c]; small(g, fg, Math.round(ox - host.width(fg) * 0.375f), ty + 19, argb(la * hk, col)); }
            if (fc < 0) HITS.add(new Object[]{Math.round(ox - rad), Math.round(oy - rad), Math.round(ox + rad), Math.round(oy + rad + 30), (Runnable) () -> fly(c)});
        }
    }
    /** The heart of a constellation: a big star in the chapter's colour (dim until you earn something there), with a ring of rays. */
    private static void heart(GuiGraphicsExtractor g, int c, float x, float y, boolean any, boolean full, float fade, float zoomed, int fc) {
        int col = ACH_COL[c]; float k = 0.8f + 0.4f * cl(camS - 1f), tw = 0.85f + 0.15f * (float) Math.sin(now / 520.0 + c);
        float on = any ? 1f : 0.35f;
        glow(x, y, 16 * k, col, 0.4f * on * fade);
        glow(x, y, 6 * k * tw, mix(col, 0xFFFFFFFF, 0.5f), 0.9f * on * fade);
        NebulaTex.glow(g, x, y, 14 * k * tw, 1f, argb(0.7f * on * fade * alpha, mix(col, 0xFFFFFFFF, 0.6f)));
        NebulaTex.glow(g, x, y, 1f, 14 * k * tw, argb(0.7f * on * fade * alpha, mix(col, 0xFFFFFFFF, 0.6f)));
        ring(x, y, 7 * k, argb(0.55f * fade * alpha, col), 1f);
        if (full) { float sp = now / 3000f; for (int i = 0; i < 8; i++) { double an = sp + i * Math.PI / 4; line(x + (float) Math.cos(an) * 9 * k, y + (float) Math.sin(an) * 9 * k, x + (float) Math.cos(an) * 12 * k, y + (float) Math.sin(an) * 12 * k, col, 0.8f * fade, 0); } }
        glow(x, y, 2.2f * k, 0xFFFFFF, on * fade);
    }
    /** A dashed bar from one point to the next, its dashes slowly running outward. */
    private static void dashed(float x0, float y0, float x1, float y1, int rgb, float a) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0); if (len < 2) return;
        float off = (now / 400f) % 1f; int n = Math.max(2, Math.round(len / 5f));
        for (int i = 0; i < n; i++) { float u0 = (i + off) / n, u1 = Math.min(1f, (i + off + 0.5f) / n); if (u0 >= 1f) continue; line(x0 + (x1 - x0) * u0, y0 + (y1 - y0) * u0, x0 + (x1 - x0) * u1, y0 + (y1 - y0) * u1, rgb, a, 0); }
    }
    private static void nodeOf(GuiGraphicsExtractor g, int c, Node n, boolean open, float fade, float zoomed, int fc) {
        Achievements.Ach a = n.a(); float[] o = centre(c); float x = X(o[0] + n.du()), y = Y(o[1] + n.dv());
        if (!inSky(x, y)) return;
        boolean near = fc == c && zoomed > 0.5f;
        float hr = near ? 12 : 6;
        boolean hv = Math.abs(mx - x) <= hr && Math.abs(my - y) <= hr && (near || fc < 0) && fade > 0.5f;
        float tw = 0.82f + 0.18f * (float) Math.sin(now / 380.0 + n.depth() * 1.7 + c * 0.9);
        int col = ACH_COL[c];
        Long ig = IGNITE.get(a.id()); float igT = ig == null ? 9f : (now - ig) / 1000f;
        boolean lit = a.done() && igT >= 0f;
        if (ig != null && igT >= 0f && igT < 0.05f && igSounded.add(a.id())) sound("sky_star", 1.2f, 0.8f);
        if (lit) {
            float r = (near ? 3.4f : 2.2f) * tw + (hv ? 1.2f : 0f);
            if (igT < 1.4f) { float u = igT / 1.4f; glow(x, y, r * (12f - 7f * u), 0xFFFFFF, 0.9f * (1f - u) * fade); ring(x, y, r * (2f + 24f * ease(u)), argb((1f - u) * 0.9f * fade * alpha, col), 1f); r *= 1f + 1.2f * (1f - u); }
            glow(x, y, r * 6f, col, 0.45f * fade);
            glow(x, y, r * 2.6f, mix(col, 0xFFFFFFFF, 0.5f), 0.7f * fade);
            float rays = r * (near ? 4.5f : 3.6f) * (0.85f + 0.15f * tw);
            NebulaTex.glow(g, x, y, rays * 1.6f, 0.9f, argb(0.75f * fade * alpha, mix(col, 0xFFFFFFFF, 0.6f)));
            NebulaTex.glow(g, x, y, 0.9f, rays, argb(0.6f * fade * alpha, mix(col, 0xFFFFFFFF, 0.6f)));
            glow(x, y, r * 1.1f, 0xFFFFFF, fade);
        } else if (open) {                                                                   // the next one to earn: it breathes, with a ring showing how far along you are
            float prog = (float) Math.min(1.0, a.goal() <= 0 ? 0 : a.progress() / a.goal()), br = 0.6f + 0.4f * (float) Math.sin(now / 420.0 + c);
            plainStar(x, y, (hv ? 1f : 0.8f) * fade, n.depth() + c * 13, near);
            float rr = near ? 8f : 5f;
            ring(x, y, rr, argb(0.25f * fade * alpha, 0xC8D0F0), 1f);
            if (prog > 0f) ring(x, y, rr, argb(0.95f * fade * alpha, col), prog);
            glow(x, y, rr * 2f, col, 0.18f * br * fade);
        } else {                                                                             // further down the branch: a faint star, not yet in reach
            plainStar(x, y, 0.35f * fade, n.depth() + c * 13, false);
        }
        if (!a.reward().isEmpty() && (near || hv)) { float bx = x + (near ? 7 : 5), by = y - (near ? 7 : 5); line(bx - 2, by, bx + 2, by, lit ? 0xFFE9B8 : 0x8A94B8, fade, 0); line(bx, by - 2, bx, by + 2, lit ? 0xFFE9B8 : 0x8A94B8, fade, 0); }   // a reward waits here
        if (a.id().equals(blinkId) && now >= blinkAt && now < blinkAt + 4200) {
            float u = ((now - blinkAt) % 1400L) / 1400f, b = (float) Math.sin(Math.PI * u);
            glow(x, y, 14 + 10 * b, col, 0.6f * b * fade); ring(x, y, 6 + 26 * ease(u), argb((1f - u) * fade * alpha, 0xFFFFFF), 1f); ring(x, y, 10 + 30 * ease(u), argb((1f - u) * 0.6f * fade * alpha, col), 1f);
        }
        if (near) {                                                                          // the label sits on the outer side of the node
            String nm = a.secret() && !a.done() ? "???" : a.name();
            float side = n.dv() > 0.01f ? 1f : n.dv() < -0.01f ? -1f : 1f; int ly = Math.round(side > 0 ? y + 10 : y - 18);
            small(g, nm, Math.round(x - host.width(nm) * 0.375f), ly, argb((lit ? 1f : open ? 0.85f : 0.5f) * fade * alpha, lit ? 0xFFFFFF : 0xB8C0DA));
            CosmeticData.Def rw = a.reward().isEmpty() ? null : CosmeticData.byId(a.reward());
            if (rw != null) { String r = "✦ " + rw.name(); small(g, r, Math.round(x - host.width(r) * 0.375f), ly + Math.round(8 * side), argb(0.85f * fade * alpha, lit ? col : 0x8A94B8)); }
        }
        if (hv && alpha > 0.6f) {
            if (!lastHoverStar.equals(a.id())) { lastHoverStar = a.id(); sound("sky_star", 0.9f + 0.05f * n.depth(), 0.45f); }
            String nm = a.secret() && !a.done() ? "???" : a.name();
            String rew = a.reward().isEmpty() ? "" : Ledger.rewardName(a.reward());
            String how = (a.secret() && !a.done() ? "A secret. Keep finding drops." : a.desc()) + (rew.isEmpty() ? "" : "  Unlocks the " + rew + ".") + (!a.done() && !open ? "  Earn the star before it first." : "");
            String extra = a.done() ? "Lit " + new java.text.SimpleDateFormat("d MMM yyyy").format(new Date(a.at())) : Ledger.fmt(a.progress()) + " / " + Ledger.fmt(a.goal());
            host.tip(nm + "\n" + ACH_NAME[c] + "\n" + (a.done() ? 3 : 0) + "\n" + (a.done() ? "1" : "0") + "\n" + extra + "\n" + how, a.done() ? col : 0xFF807891);
        }
    }

    // ------------------------------------------------------------------ the chart: the sky is a star map, inked in gold
    private static void chart(GuiGraphicsExtractor g) {
        float cu = aspect() / 2f, cv = 0.5f, ink = 0.16f * alpha;
        float cx = X(cu), cy = Y(cv);
        for (int i = 1; i <= 4; i++) { float r = (0.24f * i) * sh * camS; ringDash(cx, cy, r, 0xE8C76A, ink * (i % 2 == 0 ? 1f : 0.7f)); }       // circles of declination
        for (int m = 0; m < 12; m++) { double an = m * Math.PI / 6; float r0 = 0.08f * sh * camS, r1 = 1.0f * sh * camS; line(cx + (float) Math.cos(an) * r0, cy + (float) Math.sin(an) * r0, cx + (float) Math.cos(an) * r1, cy + (float) Math.sin(an) * r1, 0xE8C76A, ink * 0.6f, 0); }   // hour lines
        float px = 0, py = 0;
        for (int i = 0; i <= 48; i++) {                                                       // the ecliptic, a long dotted wave across the chart
            float u = -0.1f + i / 48f * (aspect() + 0.2f), v = 0.5f + 0.2f * (float) Math.sin(u * 3.2f + 0.6f), x = X(u), y = Y(v);
            if (i > 0 && i % 2 == 0) line(px, py, x, y, 0xFFB86A, ink * 1.2f, 0);
            px = x; py = y;
        }
        for (int i = 1; i <= 4; i++) {                                                        // degree labels on the circles
            float r = 0.24f * i * sh * camS; String s = (90 - i * 20) + "°"; float lx = cx + r * 0.71f + 2, ly = cy - r * 0.71f - 6;
            if (inSky(lx, ly)) small(g, s, Math.round(lx), Math.round(ly), argb(ink * 2.2f, 0xE8C76A));
        }
    }
    private static void ringDash(float cx, float cy, float r, int rgb, float a) {
        int n = Math.max(24, Math.min(96, Math.round(r / 6f)));
        for (int i = 0; i < n; i += 2) { double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n; float x0 = cx + (float) Math.cos(a0) * r, y0 = cy + (float) Math.sin(a0) * r, x1 = cx + (float) Math.cos(a1) * r, y1 = cy + (float) Math.sin(a1) * r;
            if (inSky(x0, y0) || inSky(x1, y1)) line(x0, y0, x1, y1, rgb, a / Math.max(0.01f, alpha), 0); }
    }

    // ------------------------------------------------------------------ the cards on the overview: what is nearly done, what was lit last, the legend
    private static void cards(GuiGraphicsExtractor g, float a) {
        Achievements.Profile p = Achievements.profile(); if (p == null || a < 0.02f) return;
        List<Achievements.Ach> next = new ArrayList<>(), recent = new ArrayList<>();
        for (Achievements.Ach q : p.list()) { if (q.done()) recent.add(q); else if (!q.secret() && q.goal() > 0) next.add(q); }
        next.sort(Comparator.comparingDouble(q -> -(q.progress() / q.goal())));
        recent.sort(Comparator.comparingLong(q -> -q.at()));
        int w = Math.max(110, Math.round(sw * 0.2f)), x = sx0 + sw - w - 10, y = sy0 + 26;
        y = card(g, "NEARLY THERE", next.subList(0, Math.min(3, next.size())), x, y, w, a, true) + 6;
        card(g, "RECENTLY LIT", recent.subList(0, Math.min(3, recent.size())), x, y, w, a, false);
        int lx = sx0 + Math.round(sw * 0.38f), ly = sy0 + sh - 18;                              // the legend
        g.fill(lx - 4, ly - 4, lx + 196, ly + 10, argb(0.45f * a * alpha, 0x060818));
        glow(lx + 3, ly + 3, 3, 0xFFE9B8, a); small(g, "earned", lx + 9, ly + 1, argb(0.8f * a * alpha, 0xD8DCEC));
        ring(lx + 52, ly + 3, 3, argb(0.9f * a * alpha, 0x7AD8FF), 0.6f); small(g, "next up", lx + 58, ly + 1, argb(0.8f * a * alpha, 0xD8DCEC));
        glow(lx + 104, ly + 3, 1.6f, 0x9AB4FF, 0.5f * a); small(g, "locked", lx + 110, ly + 1, argb(0.8f * a * alpha, 0xD8DCEC));
        line(lx + 148, ly + 3, lx + 154, ly + 3, 0xFFE9B8, a, 0); line(lx + 151, ly, lx + 151, ly + 6, 0xFFE9B8, a, 0); small(g, "reward", lx + 158, ly + 1, argb(0.8f * a * alpha, 0xD8DCEC));
    }
    private static int card(GuiGraphicsExtractor g, String title, List<Achievements.Ach> l, int x, int y, int w, float a, boolean bars) {
        if (l.isEmpty()) return y;
        int h = 14 + l.size() * (bars ? 18 : 12);
        g.fill(x, y, x + w, y + h, argb(0.55f * a * alpha, 0x060818)); g.fill(x, y, x + 2, y + h, argb(0.8f * a * alpha, 0xE8C76A));
        small(g, title, x + 7, y + 4, argb(0.9f * a * alpha, 0xE8C76A));
        int yy = y + 14;
        for (Achievements.Ach q : l) {
            int ci = Math.max(0, Arrays.asList(ACH).indexOf(q.ch())), col = ACH_COL[ci];
            boolean hv = mx >= x && mx < x + w && my >= yy - 1 && my < yy + (bars ? 17 : 11);
            if (hv) g.fill(x + 2, yy - 1, x + w, yy + (bars ? 17 : 11), argb(0.12f * a * alpha, 0xFFFFFF));
            glow(x + 10, yy + 3, 2.4f, col, a);
            small(g, q.name(), x + 16, yy, argb(a * alpha, 0xFFFFFF));
            if (bars) {
                float f = (float) Math.min(1.0, q.progress() / q.goal()); int bw = w - 26;
                g.fill(x + 16, yy + 10, x + 16 + bw, yy + 12, argb(0.25f * a * alpha, 0x9AA6D0)); g.fill(x + 16, yy + 10, x + 16 + Math.round(bw * f), yy + 12, argb(a * alpha, col));
                String pr = Ledger.fmt(q.progress()) + "/" + Ledger.fmt(q.goal()); small(g, pr, x + w - 6 - Math.round(host.width(pr) * 0.75f), yy, argb(0.7f * a * alpha, 0xB8C0DA));
            }
            String id = q.id(); int c = ci;
            HITS.add(new Object[]{x, yy - 1, x + w, yy + (bars ? 17 : 11), (Runnable) () -> { blinkId = id; blinkAt = System.currentTimeMillis() + ZOOM; fly(c); }});
            yy += bars ? 18 : 12;
        }
        return y + h;
    }

    /** A star not yet earned: a real, small, cool-white star that twinkles. */
    private static void plainStar(float x, float y, float a, int seed, boolean big) {
        float tw = 0.7f + 0.3f * (float) Math.sin(now / (600.0 + rnd(seed) * 700) + seed);
        glow(x, y, big ? 7f : 5f, 0x9AB4FF, 0.35f * a * tw);
        NebulaTex.glow(G, x, y, big ? 5f : 3.5f, 0.6f, argb(0.5f * a * tw * alpha, 0xD8E2FF));
        NebulaTex.glow(G, x, y, 0.6f, big ? 5f : 3.5f, argb(0.5f * a * tw * alpha, 0xD8E2FF));
        glow(x, y, big ? 1.8f : 1.3f, 0xF4F6FF, a * tw);
    }
    private static void spark(float x0, float y0, float x1, float y1, int col, float fade, int seed) {
        float per = 2.6f + rnd(seed) * 1.6f, u = ((now / 1000f + rnd(seed * 3) * per) % per) / 1.1f; if (u > 1f) return;
        glow(x0 + (x1 - x0) * u, y0 + (y1 - y0) * u, 4f, mix(col, 0xFFFFFFFF, 0.6f), 0.9f * fade * (float) Math.sin(Math.PI * u));
    }
    private static String spaced(String s) { StringBuilder b = new StringBuilder(); for (int i = 0; i < s.length(); i++) { if (i > 0) b.append(' '); b.append(s.charAt(i)); } return b.toString().replace("   ", "  "); }

    // ------------------------------------------------------------------ the panel of the constellation you flew into
    private static void panel(GuiGraphicsExtractor g, int c, float a) {
        int px0 = sx0 + 16, py0 = sy0 + 30, pw = Math.round(sw * 0.3f), ph = sh - 46;
        Gfx.push(g); Gfx.translate(g, (1f - ease(a)) * -20f, 0);
        g.fill(px0, py0, px0 + pw, py0 + ph, argb(0.62f * a * alpha, 0x060818));
        g.fill(px0, py0, px0 + pw, py0 + 1, argb(0.5f * a * alpha, ACH_COL[c])); g.fill(px0, py0, px0 + 2, py0 + ph, argb(0.8f * a * alpha, ACH_COL[c]));
        int x = px0 + 12, y = py0 + 12, w = pw - 24;
        small(g, spaced(FIGURE[c].toUpperCase(Locale.ROOT)), x, y, argb(0.9f * a * alpha, ACH_COL[c]));
        Gfx.push(g); Gfx.translate(g, x, y + 9); Gfx.scale(g, 1.6f); g.text(font, ACH_NAME[c], 0, 0, argb(a * alpha, 0xFFFFFF), true); Gfx.pop(g);
        List<Achievements.Ach> l = achOf(ACH[c]); int d = 0; for (Achievements.Ach q : l) if (q.done()) d++;
        int sy = y + 30;
        text(g, d + " of " + l.size() + " stars lit", x, sy, argb(a * alpha, 0xD8DCEC), false);
        int by = sy + 13, seg = Math.max(1, l.size()), gap = 2, sgw = (w - gap * (seg - 1)) / seg;
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
    private static void header(GuiGraphicsExtractor g, float zoomed, float in, int fc) {
        Achievements.Profile p = Achievements.profile();
        int all = 0, lit = 0; if (p != null) for (Achievements.Ach a : p.list()) { all++; if (a.done()) lit++; }
        float ia = in * alpha;
        int x = sx0 + 14, y = sy0 + 11;
        String ttl = spaced("ACHIEVEMENTS");
        text(g, ttl, x, y, argb(ia, 0xFFE9B8), true);
        String cnt = p == null ? (Achievements.status.isEmpty() ? "Reading the stars..." : Achievements.status) : lit + " of " + all + " stars lit";
        small(g, cnt, x + host.width(ttl) + 10, y + 1, argb(0.8f * ia, 0xB8C0DA));
        if (p != null && all > 0) { int bw = host.width(ttl); g.fill(x, y + 11, x + bw, y + 12, argb(0.2f * ia, 0xFFE9B8)); g.fill(x, y + 11, x + Math.round(bw * lit / (float) all), y + 12, argb(ia, 0xFFE9B8)); }
        if (zoomed < 0.3f && p != null) { String hint = "Point at a constellation, click to fly in"; small(g, hint, sx0 + sw - 34 - Math.round(host.width(hint) * 0.75f), sy0 + 12, argb(0.55f * ia * (1f - zoomed / 0.3f), 0xB8C0DA)); }
        compass(g, sx0 + sw - 30, sy0 + sh - 30, 16, ia);
        float bcx = sx0 + sw - 16.5f, bcy = sy0 + 14.5f;                                       // the close button: a ring with an X through its exact middle
        boolean hc = Math.hypot(mx - bcx, my - bcy) < 9;
        glow(bcx, bcy, 9, 0xFFFFFF, (hc ? 0.35f : 0.12f) * ia);
        ring(bcx, bcy, 7.5f, argb((hc ? 0.9f : 0.5f) * ia, 0xE8C76A), 1f);
        for (int d = 0; d < 2; d++) { float s0 = d == 0 ? 1f : -1f; float x0 = bcx - 3.2f, y0 = bcy - 3.2f * s0, x1 = bcx + 3.2f, y1 = bcy + 3.2f * s0;
            int L = Math.round((float) Math.hypot(x1 - x0, y1 - y0)); Gfx.push(g); Gfx.translate(g, bcx, bcy); Gfx.rotate(g, (float) Math.atan2(y1 - y0, x1 - x0)); Gfx.translate(g, -L / 2f, -0.5f); g.fill(0, 0, L, 1, argb(ia, 0xFFFFFF)); Gfx.pop(g); }
        int cx = Math.round(bcx) - 8, cy = Math.round(bcy) - 8;
        HITS.add(new Object[]{cx, cy, cx + 17, cy + 17, (Runnable) Codex::close});
        if (fc >= 0) {
            String b = "‹  All constellations"; int bw = Math.round(host.width(b) * 0.75f) + 12, bx = sx0 + sw - 34 - bw, by = sy0 + 8;
            boolean hb = mx >= bx && mx < bx + bw && my >= by && my < by + 13;
            g.fill(bx, by, bx + bw, by + 13, argb((hb ? 0.32f : 0.14f) * ia, 0xFFFFFF)); g.fill(bx, by + 12, bx + bw, by + 13, argb(0.6f * ia, ACH_COL[fc]));
            small(g, b, bx + 6, by + 3, argb(ia, 0xFFFFFF));
            HITS.add(new Object[]{bx, by, bx + bw, by + 13, (Runnable) () -> fly(-1)});
        }
    }
    private static void compass(GuiGraphicsExtractor g, int cx, int cy, int r, float a) {
        ring(cx, cy, r, argb(0.45f * a, 0xE8C76A), 1f); ring(cx, cy, r - 3, argb(0.2f * a, 0xE8C76A), 1f);
        double rot = now / 20000.0;
        for (int k = 0; k < 8; k++) { double an = rot + k * Math.PI / 4; float len = k % 2 == 0 ? r - 1 : r * 0.55f; line(cx, cy, cx + (float) Math.cos(an) * len, cy + (float) Math.sin(an) * len, k == 0 ? 0xFFE9B8 : 0xE8C76A, (k == 0 ? 0.9f : 0.45f) * a / Math.max(0.01f, alpha), 0); }
        disc(g, cx, cy, 1.5f, argb(a, 0xFFE9B8));
    }

    // ------------------------------------------------------------------ drawing helpers: a soft light, rotated bars
    private static void text(GuiGraphicsExtractor g, String s, int x, int y, int c, boolean sh) { g.text(font, s, x, y, c, sh); }
    private static void small(GuiGraphicsExtractor g, String s, int x, int y, int c) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, 0.75f); g.text(font, s, 0, 0, c, false); Gfx.pop(g); }
    private static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
        if (r < 0.6f) { g.fill(Math.round(cx), Math.round(cy), Math.round(cx) + 1, Math.round(cy) + 1, c); return; }
        int ir = (int) Math.ceil(r);
        for (int dy = -ir; dy <= ir; dy++) { int hw = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy))); if (hw > 0) g.fill(Math.round(cx) - hw, Math.round(cy) + dy, Math.round(cx) + hw, Math.round(cy) + dy + 1, c); }
    }
    /** A soft round light of radius r. */
    private static void glow(float cx, float cy, float r, int rgb, float a) { NebulaTex.glow(G, cx, cy, r, r, argb(a * alpha, rgb)); }
    /** A bar from (x0, y0) to (x1, y1), one draw however long. width 0 = a hairline, 1 = a 3-pixel soft band, 2 = a wide soft glow. */
    private static void line(float x0, float y0, float x1, float y1, int rgb, float a, int width) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0); if (len < 1f || a < 0.01f) return;
        float trim = Math.min(4f, len * 0.2f);                                     // stop short of the stars at both ends
        Gfx.push(G); Gfx.translate(G, x0, y0); Gfx.rotate(G, (float) Math.atan2(y1 - y0, x1 - x0));
        int L = Math.round(len - trim);
        if (width == 2) NebulaTex.glow(G, len / 2f, 0, len / 2f, 3.5f, argb(a * alpha, rgb));
        else {
            G.fill(Math.round(trim), 0, L, 1, argb(a * alpha, rgb));
            if (width == 1) { G.fill(Math.round(trim), -1, L, 0, argb(a * 0.45f * alpha, rgb)); G.fill(Math.round(trim), 1, L, 2, argb(a * 0.45f * alpha, rgb)); }
        }
        Gfx.pop(G);
    }
    /** A thick bar (the stargazer's limbs and telescope), rounded off with a disc at each end. */
    private static void thick(float x0, float y0, float x1, float y1, float w, int rgb, float a) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0); int c = argb(a * alpha, rgb);
        Gfx.push(G); Gfx.translate(G, x0, y0); Gfx.rotate(G, (float) Math.atan2(y1 - y0, x1 - x0)); Gfx.translate(G, 0, -w / 2f); Gfx.scale(G, 1f);
        G.pose().scale(1f, w); G.fill(0, 0, Math.max(1, Math.round(len)), 1, c); Gfx.pop(G);
        disc(G, x0, y0, w / 2f, c); disc(G, x1, y1, w / 2f, c);
    }
    private static void bar(float x0, float y0, float x1, float y1, int rgb, float a) { G.fill(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1), argb(a * alpha, rgb)); }
    /** A streak that fades from its head to its tail (three pieces). */
    private static void streak(float hx, float hy, float tx, float ty, int rgb, float a) {
        for (int i = 0; i < 3; i++) { float u0 = i / 3f, u1 = (i + 1) / 3f; line(hx + (tx - hx) * u0, hy + (ty - hy) * u0, hx + (tx - hx) * u1, hy + (ty - hy) * u1, rgb, a * (1f - u0), 0); }
    }
    /** A ring (or the start of one, up to `upto` of the way round) made of short rotated bars. */
    private static void ring(float cx, float cy, float r, int c, float upto) {
        int n = Math.max(16, Math.min(64, Math.round(r * 1.2f)));
        float a = (c >>> 24) / 255f / Math.max(0.01f, alpha); int rgb = c & 0xFFFFFF;
        for (int i = 0; i < n; i++) {
            float u0 = i / (float) n, u1 = (i + 1) / (float) n; if (u0 >= upto) break; u1 = Math.min(u1, upto);
            double a0 = -Math.PI / 2 + u0 * Math.PI * 2, a1 = -Math.PI / 2 + u1 * Math.PI * 2;
            float x0 = cx + (float) Math.cos(a0) * r, y0 = cy + (float) Math.sin(a0) * r, x1 = cx + (float) Math.cos(a1) * r, y1 = cy + (float) Math.sin(a1) * r;
            float len = (float) Math.hypot(x1 - x0, y1 - y0);
            Gfx.push(G); Gfx.translate(G, x0, y0); Gfx.rotate(G, (float) Math.atan2(y1 - y0, x1 - x0)); G.fill(0, 0, Math.max(1, Math.round(len + 0.5f)), 1, argb(a * alpha, rgb)); Gfx.pop(G);
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
