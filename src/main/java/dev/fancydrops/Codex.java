package dev.fancydrops;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.*;

/**
 * Achievements as a star map. Every chapter of the ledger is a constellation in one night sky, every achievement is a star in it:
 * an earned achievement is a lit star, and the lines of a constellation light up between stars that are both lit, so a finished
 * chapter draws its whole figure. Click a constellation to fly in (names, progress rings, rewards and the chapter's numbers on the
 * left); right click, Esc or "All constellations" flies out again. The sky opens out of the button at the bottom right of the menu.
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
    private static final int[] ACH_COL = {0xFFE8C76A, 0xFFB98CFF, 0xFF6AD0FF, 0xFFFF7A6A, 0xFF8EE06A, 0xFF5AD8E8, 0xFFFF8AC8};
    private static final String[] QUOTE = {"Every find, written down.", "Fortune favours the patient.", "Dig, and the earth answers.", "They drop what they guard.", "The harvest remembers.", "Patience, then a splash.", "Every colour has a story."};
    /** Where each constellation sits in the sky (0..1 across the width, 0..1 down). */
    private static final float[][] HOME = {{0.17f, 0.34f}, {0.43f, 0.27f}, {0.70f, 0.32f}, {0.88f, 0.64f}, {0.15f, 0.74f}, {0.41f, 0.72f}, {0.65f, 0.75f}};
    /** The figure each one draws: star positions (sky units round its centre, 1 = the sky's height) in achievement order. */
    private static final float[][][] SHAPE = {
        {{-0.13f, 0.05f}, {-0.08f, -0.03f}, {-0.02f, 0.02f}, {0.04f, -0.05f}, {0.10f, -0.08f}, {0.13f, 0.00f}, {0.08f, 0.07f}, {0.02f, 0.10f}},
        {{0.00f, -0.11f}, {0.08f, -0.07f}, {0.11f, 0.01f}, {0.07f, 0.09f}, {-0.01f, 0.11f}, {-0.09f, 0.07f}, {-0.11f, -0.02f}, {-0.07f, -0.09f}},
        {{-0.11f, 0.08f}, {-0.04f, 0.03f}, {0.03f, -0.03f}, {0.10f, -0.10f}},
        {{-0.13f, 0.12f}, {-0.07f, 0.06f}, {-0.01f, 0.00f}, {0.05f, -0.06f}, {0.10f, -0.11f}, {-0.09f, -0.04f}, {0.04f, 0.09f}, {0.15f, -0.15f}},
        {{0.00f, 0.11f}, {0.00f, 0.02f}, {-0.07f, -0.06f}, {0.00f, -0.10f}, {0.07f, -0.06f}},
        {{-0.12f, 0.00f}, {-0.04f, -0.06f}, {0.07f, 0.00f}, {0.12f, -0.05f}},
        {{0.00f, -0.10f}, {0.06f, 0.04f}, {-0.06f, 0.04f}}};
    /** Which stars a figure joins (pairs of indexes). */
    private static final int[][][] LINES = {
        {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 2}},
        {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 0}},
        {{0, 1}, {1, 2}, {2, 3}},
        {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {5, 2}, {2, 6}, {4, 7}},
        {{0, 1}, {1, 2}, {1, 3}, {1, 4}, {2, 3}, {3, 4}},
        {{0, 1}, {1, 2}, {2, 0}, {2, 3}},
        {{0, 1}, {1, 2}, {2, 0}}};

    static boolean open;
    private static long openAt, closeAt, zoomAt;
    private static int[] from = {0, 0, 0, 0};
    private static int focus = -1, prevFocus = -1;                                // the constellation flown into (-1 = the whole sky)
    private static final long OPEN = 520, CLOSE = 320, ZOOM = 560;
    private static final List<Object[]> HITS = new ArrayList<>();
    private static String lastHoverStar = "";

    // ------------------------------------------------------------------ open / close
    static void open(int[] btn, int chapter) {
        open = true; openAt = System.currentTimeMillis(); closeAt = 0; from = btn.clone();
        focus = chapter >= 1 && chapter <= ACH.length ? chapter - 1 : -1; prevFocus = focus; zoomAt = 0;
        Achievements.refresh(true);
        sound("sky_open", 1f, 0.8f);
    }
    static void close() { if (open && closeAt == 0) { closeAt = System.currentTimeMillis(); sound("sky_close", 1f, 0.6f); } }
    static boolean busy() { return open; }
    private static void fly(int c) { if (c == focus) return; prevFocus = focus; focus = c; zoomAt = System.currentTimeMillis(); sound("sky_zoom", c < 0 ? 0.85f : 1f, 0.7f); }

    private static void sound(String name, float pitch, float gain) { try { SoundFx.Fx fx = SoundFx.byName(name); if (fx == null) fx = SoundFx.byName("exp_flip"); if (fx != null) SoundFx.play(fx, pitch, gain); } catch (Throwable ignored) { } }
    private static float cl(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    private static float ease(float v) { v = cl(v); return v * v * v * (v * (v * 6 - 15) + 10); }
    private static float back(float v) { v = cl(v); float c1 = 1.4f, c3 = c1 + 1, q = v - 1; return 1 + c3 * q * q * q + c1 * q * q; }
    private static int mix(int a, int b, float f) { return Canvas.mix(a, b, cl(f)); }
    private static int argb(float a, int rgb) { return Gfx.argb(Math.round(cl(a) * 255), rgb & 0xFFFFFF); }
    private static float rnd(int n) { return Ritual.rnd(n); }

    // ------------------------------------------------------------------ the sky and the camera
    private static int W, H, sx0, sy0, sw, sh;                  // the sky's rectangle on screen
    private static Font font; private static Host host; private static int mx, my; private static long now;
    private static float camU, camV, camS;                      // the camera: the sky point in the middle, and the zoom
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
    /** Star k of constellation c, in sky units. */
    private static float[] star(int c, int k) {
        float[] o = centre(c); float[][] s0 = SHAPE[c];
        if (k < s0.length) return new float[]{o[0] + s0[k][0], o[1] + s0[k][1]};
        float an = k * 2.4f; return new float[]{o[0] + (float) Math.cos(an) * 0.15f, o[1] + (float) Math.sin(an) * 0.12f};
    }

    static void draw(GuiGraphicsExtractor g, Host h, int width, int height, int mx0, int my0) {
        if (!open) return;
        now = System.currentTimeMillis(); host = h; font = h.font(); W = width; H = height; mx = mx0; my = my0; HITS.clear();
        float t = now - openAt, closeT = closeAt == 0 ? -1f : now - closeAt;
        if (closeT >= CLOSE) { open = false; return; }
        float grow = closeT >= 0 ? 1f - ease(closeT / CLOSE) : back(t / OPEN);
        alpha = closeT >= 0 ? 1f - cl(closeT / (CLOSE * 0.7f)) : cl(t / 260f);

        // the sky grows out of the button
        int fw = Math.round(Math.min(W - 40, H * 1.9f)), fh = Math.round(Math.min(H - 40, fw / 1.75f));
        float bx = (from[0] + from[2]) / 2f, by = (from[1] + from[3]) / 2f, cxs = W / 2f, cys = H / 2f;
        float gx = bx + (cxs - bx) * cl(grow), gy = by + (cys - by) * cl(grow), gs = Math.max(0.02f, grow);
        sw = Math.round(fw * gs); sh = Math.round(fh * gs); sx0 = Math.round(gx - sw / 2f); sy0 = Math.round(gy - sh / 2f);
        g.fill(0, 0, W, H, argb(0.55f * alpha, 0x04030A));
        if (sw < 8 || sh < 8) return;

        // the camera flies between the whole sky and one constellation
        float z = zoomAt == 0 ? 1f : ease((now - zoomAt) / (float) ZOOM);
        float[] a0 = camOf(prevFocus), a1 = camOf(focus);
        float zz = z * z * (3 - 2 * z), sc = a0[2] + (a1[2] - a0[2]) * zz;
        camU = a0[0] + (a1[0] - a0[0]) * z; camV = a0[1] + (a1[1] - a0[1]) * z; camS = sc;
        float zoomed = focus >= 0 ? (prevFocus >= 0 ? 1f : z) : (prevFocus >= 0 ? 1f - z : 0f);   // 0 = the whole sky, 1 = in close

        sky(g);
        for (int c = 0; c < ACH.length; c++) constellation(g, c, zoomed);
        shootingStar(g);
        frame(g);
        if (alpha > 0.6f && closeT < 0) {
            header(g, zoomed);
            if (focus >= 0 && zoomed > 0.5f) panel(g, focus, cl((zoomed - 0.5f) * 2f));
        }
    }
    /** The camera for a constellation (-1 = the whole sky): {u, v, zoom}. The constellation sits right of centre, leaving room for its panel. */
    private static float[] camOf(int c) {
        if (c < 0) return new float[]{aspect() / 2f, 0.5f, 1f};
        float[] o = centre(c); float top = 0f, bot = 0f;                          // close enough to read, far enough that the whole figure (and its labels) fits
        for (float[] q : SHAPE[c]) { top = Math.min(top, q[1]); bot = Math.max(bot, q[1]); }
        float s = Math.min(2.8f, 0.62f / Math.max(0.12f, bot - top + 0.08f)), mid = (top + bot) / 2f;
        return new float[]{o[0] - (aspect() * 0.17f) / s, o[1] + mid + 0.025f, s};
    }

    // ------------------------------------------------------------------ the sky
    private static void sky(GuiGraphicsExtractor g) {
        int bands = 24;
        for (int i = 0; i < bands; i++) {                                         // deep blue at the top, violet at the bottom
            int y0 = sy0 + sh * i / bands, y1 = sy0 + sh * (i + 1) / bands;
            g.fill(sx0, y0, sx0 + sw, y1, argb(0.97f * alpha, mix(0xFF0A1028, 0xFF1C1236, i / (float) bands)));
        }
        float px = (camU - aspect() / 2f) * sh * camS * 0.15f, py = (camV - 0.5f) * sh * camS * 0.15f;     // the far sky drifts a little with the camera
        int[][] neb = {{30, 35, 0x3A2A7A}, {70, 60, 0x1A4A6A}, {50, 85, 0x4A1A5A}, {85, 20, 0x223A6A}};
        for (int[] n : neb) {                                                     // soft nebula clouds
            float cx = sx0 + sw * n[0] / 100f - px * 0.5f, cy = sy0 + sh * n[1] / 100f - py * 0.5f, r = sh * 0.32f;
            for (int k = 6; k >= 1; k--) { float rr = r * k / 6f; softBlob(g, cx, cy, rr, rr * 0.6f, n[2], 0.035f * alpha); }
        }
        for (int i = 0; i < 170; i++) {                                           // the background stars twinkle
            float u = rnd(i * 13 + 1), v = rnd(i * 29 + 7), tw = 0.5f + 0.5f * (float) Math.sin(now / (600.0 + rnd(i) * 900) + i);
            float x = sx0 + ((u * sw - px * (0.4f + rnd(i * 3))) % sw + sw) % sw, y = sy0 + ((v * sh - py * (0.4f + rnd(i * 3))) % sh + sh) % sh;
            int c = i % 7 == 0 ? 0xFFE0A8 : i % 5 == 0 ? 0xA8C8FF : 0xFFFFFF; float al = (0.15f + 0.5f * tw * rnd(i * 7)) * alpha;
            g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, argb(al, c));
        }
    }
    private static void softBlob(GuiGraphicsExtractor g, float cx, float cy, float rx, float ry, int rgb, float a) {
        int step = Math.max(2, Math.round(ry / 10));
        for (float dy = -ry; dy <= ry; dy += step) {
            float hw = rx * (float) Math.sqrt(Math.max(0, 1 - (dy / ry) * (dy / ry)));
            int y = Math.round(cy + dy), x0 = Math.max(sx0, Math.round(cx - hw)), x1 = Math.min(sx0 + sw, Math.round(cx + hw)), y1 = Math.min(sy0 + sh, y + step);
            if (x1 > x0 && y >= sy0 && y < sy0 + sh) g.fill(x0, Math.max(sy0, y), x1, y1, argb(a, rgb));
        }
    }
    /** A thin gold frame round the sky, like the edge of a star chart, with its tick marks. */
    private static void frame(GuiGraphicsExtractor g) {
        int c = argb(0.55f * alpha, 0xE8C76A), c2 = argb(0.25f * alpha, 0xE8C76A);
        g.fill(sx0, sy0, sx0 + sw, sy0 + 1, c); g.fill(sx0, sy0 + sh - 1, sx0 + sw, sy0 + sh, c); g.fill(sx0, sy0, sx0 + 1, sy0 + sh, c); g.fill(sx0 + sw - 1, sy0, sx0 + sw, sy0 + sh, c);
        g.fill(sx0 + 3, sy0 + 3, sx0 + sw - 3, sy0 + 4, c2); g.fill(sx0 + 3, sy0 + sh - 4, sx0 + sw - 3, sy0 + sh - 3, c2); g.fill(sx0 + 3, sy0 + 3, sx0 + 4, sy0 + sh - 3, c2); g.fill(sx0 + sw - 4, sy0 + 3, sx0 + sw - 3, sy0 + sh - 3, c2);
        for (int i = 1; i < 12; i++) { int x = sx0 + sw * i / 12; g.fill(x, sy0, x + 1, sy0 + (i % 3 == 0 ? 6 : 3), c); g.fill(x, sy0 + sh - (i % 3 == 0 ? 6 : 3), x + 1, sy0 + sh, c); }
        for (int i = 1; i < 8; i++) { int y = sy0 + sh * i / 8; g.fill(sx0, y, sx0 + (i % 2 == 0 ? 6 : 3), y + 1, c); g.fill(sx0 + sw - (i % 2 == 0 ? 6 : 3), y, sx0 + sw, y + 1, c); }
    }
    /** Now and then a shooting star crosses the sky. */
    private static void shootingStar(GuiGraphicsExtractor g) {
        long per = 7000L, st = now / per; float u = (now % per) / 1100f; if (u > 1f) return;
        float x0 = sx0 + sw * (0.2f + 0.6f * rnd((int) st)), y0 = sy0 + sh * (0.05f + 0.3f * rnd((int) st + 9));
        float x = x0 + u * sw * 0.25f, y = y0 + u * sh * 0.12f;
        for (int k = 0; k < 18; k++) { float b = k / 18f; float tx = x - b * sw * 0.05f, ty = y - b * sh * 0.024f; if (inSky(tx, ty)) g.fill(Math.round(tx), Math.round(ty), Math.round(tx) + 1, Math.round(ty) + 1, argb((1 - b) * (1 - u) * alpha, 0xFFFFFF)); }
    }

    // ------------------------------------------------------------------ a constellation
    private static void constellation(GuiGraphicsExtractor g, int c, float zoomed) {
        List<Achievements.Ach> l = achOf(ACH[c]); int n = Math.max(l.size(), SHAPE[c].length);
        boolean other = focus >= 0 && c != focus; float fade = other ? 1f - 0.8f * zoomed : 1f;
        float[] o = centre(c); float ox = X(o[0]), oy = Y(o[1]);
        float rad = 0.15f * sh * camS;
        boolean hv = focus < 0 && zoomed < 0.05f && Math.hypot(mx - ox, my - oy) < rad;
        int done = 0; for (Achievements.Ach a : l) if (a.done()) done++;
        boolean full = !l.isEmpty() && done == l.size();
        int col = ACH_COL[c];
        if (full || hv) softBlob(g, ox, oy, rad * 1.1f, rad * 0.9f, col, (full ? 0.05f : 0.035f) * fade * alpha);          // a finished figure glows
        for (int[] e : LINES[c]) {                                                // the lines: lit between two lit stars, a faint dotted line otherwise
            if (e[0] >= n || e[1] >= n) continue;
            boolean lit = e[0] < l.size() && e[1] < l.size() && l.get(e[0]).done() && l.get(e[1]).done();
            float[] p0 = star(c, e[0]), p1 = star(c, e[1]);
            line(g, X(p0[0]), Y(p0[1]), X(p1[0]), Y(p1[1]), lit ? mix(col, 0xFFFFFFFF, 0.35f) : 0xFF8A94B8, (lit ? 0.75f : hv || focus == c ? 0.4f : 0.2f) * fade, !lit, lit);
        }
        for (int k = l.size(); k < SHAPE[c].length; k++) { float[] p = star(c, k); dotStar(g, X(p[0]), Y(p[1]), 0.12f * fade, 1); }        // spare points of the figure stay faint
        for (int k = 0; k < l.size(); k++) starOf(g, c, k, l.get(k), fade, zoomed);
        if (zoomed < 0.6f && inSky(ox, oy + rad)) {                               // the figure's name under it, with how many stars are lit
            float la = (1f - zoomed / 0.6f) * fade * alpha;
            String nm = ACH_NAME[c], cnt = done + " / " + l.size() + " lit";
            int ty = Math.round(oy + rad * 0.95f);
            text(g, nm, Math.round(ox - host.width(nm) / 2f), ty, argb(la, hv ? 0xFFFFFF : full ? col : 0xD8DCEC), true);
            small(g, cnt + (full ? "  ✦" : ""), Math.round(ox - host.width(cnt) * 0.375f), ty + 10, argb(la * 0.9f, full ? col : 0x8A94B8));
            if (hv) small(g, FIGURE[c], Math.round(ox - host.width(FIGURE[c]) * 0.375f), Math.round(oy - rad * 1.05f), argb(la * 0.85f, col));
            if (focus < 0) HITS.add(new Object[]{Math.round(ox - rad), Math.round(oy - rad), Math.round(ox + rad), Math.round(oy + rad + 22), (Runnable) () -> fly(c)});
        }
    }
    private static void starOf(GuiGraphicsExtractor g, int c, int k, Achievements.Ach a, float fade, float zoomed) {
        float[] p = star(c, k); float x = X(p[0]), y = Y(p[1]);
        if (!inSky(x, y)) return;
        boolean near = focus == c && zoomed > 0.5f;
        float hr = near ? 12 : 6;
        boolean hv = Math.abs(mx - x) <= hr && Math.abs(my - y) <= hr && (near || focus < 0) && fade > 0.5f;
        float tw = 0.85f + 0.15f * (float) Math.sin(now / 420.0 + k * 1.7 + c);
        int col = ACH_COL[c];
        if (a.done()) {
            float r = (near ? 3.2f : 1.8f) * tw + (hv ? 1f : 0f);
            glow(g, x, y, r * 4.5f, col, 0.22f * fade);
            glow(g, x, y, r * 2.2f, 0xFFFFFF, 0.35f * fade);
            spikes(g, x, y, r * (near ? 3.4f : 2.6f), mix(col, 0xFFFFFFFF, 0.5f), 0.8f * fade * tw);
            disc(g, x, y, r * 0.75f, argb(fade * alpha, 0xFFFFFF));
        } else {
            float prog = (float) Math.min(1.0, a.goal() <= 0 ? 0 : a.progress() / a.goal());
            dotStar(g, x, y, (hv ? 0.7f : 0.35f) * fade, near ? 2 : 1);
            if (near) { ring(g, x, y, 7.5f, argb(0.18f * fade * alpha, 0x8A94B8), 1f); if (prog > 0f) ring(g, x, y, 7.5f, argb(0.8f * fade * alpha, col), prog); }
        }
        if (near) {                                                                // in close: the star's name (and its reward) under it
            String nm = a.secret() && !a.done() ? "???" : a.name();
            small(g, nm, Math.round(x - host.width(nm) * 0.375f), Math.round(y + 11), argb((a.done() ? 1f : 0.65f) * fade * alpha, a.done() ? 0xFFFFFF : 0xB8C0DA));
            CosmeticData.Def rw = a.reward().isEmpty() ? null : CosmeticData.byId(a.reward());
            if (rw != null) { String r = "→ " + rw.name(); small(g, r, Math.round(x - host.width(r) * 0.375f), Math.round(y + 19), argb(0.8f * fade * alpha, a.done() ? col : 0x8A94B8)); }
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

    // ------------------------------------------------------------------ the panel of the constellation you flew into
    private static void panel(GuiGraphicsExtractor g, int c, float a) {
        int px0 = sx0 + 14, py0 = sy0 + 26, pw = Math.round(sw * 0.3f), ph = sh - 40;
        g.fill(px0, py0, px0 + pw, py0 + ph, argb(0.55f * a * alpha, 0x06081A));
        g.fill(px0, py0, px0 + 1, py0 + ph, argb(0.6f * a * alpha, ACH_COL[c]));
        int x = px0 + 10, y = py0 + 10, w = pw - 20;
        Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, 1.5f); g.text(font, ACH_NAME[c], 0, 0, argb(a * alpha, 0xFFFFFF), false); Gfx.pop(g);
        small(g, FIGURE[c].toUpperCase(Locale.ROOT), x, y + 16, argb(0.9f * a * alpha, ACH_COL[c]));
        List<Achievements.Ach> l = achOf(ACH[c]); int d = 0; for (Achievements.Ach q : l) if (q.done()) d++;
        text(g, d + " of " + l.size() + " stars lit", x, y + 28, argb(a * alpha, 0xD8DCEC), false);
        int by = y + 41; g.fill(x, by, x + w, by + 3, argb(0.3f * a * alpha, 0x8A94B8)); g.fill(x, by, x + Math.round(w * (l.isEmpty() ? 0 : d / (float) l.size())), by + 3, argb(a * alpha, ACH_COL[c]));
        Achievements.Profile p = Achievements.profile(); int sy = by + 12;
        if (p != null) for (String[] row : Ledger.statRows(ACH[c], p)) {
            if (sy > py0 + ph - 30) break;
            text(g, row[0], x, sy, argb(0.8f * a * alpha, 0xB8C0DA), false); int vw = host.width(row[1]); text(g, row[1], x + w - vw, sy, argb(a * alpha, 0xFFFFFF), false);
            for (int dx = x + host.width(row[0]) + 3; dx < x + w - vw - 3; dx += 3) g.fill(dx, sy + 6, dx + 1, sy + 7, argb(0.25f * a * alpha, 0x8A94B8));
            sy += 11;
        }
        small(g, QUOTE[c], x, py0 + ph - 12, argb(0.7f * a * alpha, ACH_COL[c]));
    }

    // ------------------------------------------------------------------ the header: title, count, back and close
    private static void header(GuiGraphicsExtractor g, float zoomed) {
        Achievements.Profile p = Achievements.profile();
        int all = 0, lit = 0; if (p != null) for (Achievements.Ach a : p.list()) { all++; if (a.done()) lit++; }
        int x = sx0 + 12, y = sy0 + 9;
        text(g, "Achievements", x, y, argb(alpha, 0xFFFFFF), true);
        String cnt = p == null ? (Achievements.status.isEmpty() ? "Reading the stars..." : Achievements.status) : lit + " of " + all + " stars lit";
        small(g, cnt, x + host.width("Achievements") + 8, y + 1, argb(0.8f * alpha, 0xB8C0DA));
        if (zoomed < 0.3f && p != null) { String hint = "Click a constellation to fly in"; small(g, hint, sx0 + sw / 2 - Math.round(host.width(hint) * 0.375f), sy0 + sh - 14, argb(0.55f * alpha * (1f - zoomed / 0.3f), 0xB8C0DA)); }
        int cx = sx0 + sw - 18, cy = sy0 + 8;                                     // close
        boolean hc = mx >= cx - 2 && mx < cx + 12 && my >= cy - 2 && my < cy + 12;
        g.fill(cx, cy, cx + 10, cy + 10, argb((hc ? 0.5f : 0.2f) * alpha, 0xFFFFFF));
        for (int i = 2; i < 8; i++) { g.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, argb(alpha, 0xFFFFFF)); g.fill(cx + 9 - i, cy + i, cx + 10 - i, cy + i + 1, argb(alpha, 0xFFFFFF)); }
        HITS.add(new Object[]{cx - 2, cy - 2, cx + 12, cy + 12, (Runnable) Codex::close});
        if (focus >= 0) {                                                          // back to the whole sky
            String b = "‹ All constellations"; int bw = Math.round(host.width(b) * 0.75f) + 8, bx = sx0 + sw - 30 - bw, by = sy0 + 7;
            boolean hb = mx >= bx && mx < bx + bw && my >= by && my < by + 12;
            g.fill(bx, by, bx + bw, by + 12, argb((hb ? 0.35f : 0.15f) * alpha, 0xFFFFFF));
            small(g, b, bx + 4, by + 3, argb(alpha, 0xFFFFFF));
            HITS.add(new Object[]{bx, by, bx + bw, by + 12, (Runnable) () -> fly(-1)});
        }
    }

    // ------------------------------------------------------------------ drawing helpers
    private static void text(GuiGraphicsExtractor g, String s, int x, int y, int c, boolean sh) { g.text(font, s, x, y, c, sh); }
    private static void small(GuiGraphicsExtractor g, String s, int x, int y, int c) { Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, 0.75f); g.text(font, s, 0, 0, c, false); Gfx.pop(g); }
    private static void disc(GuiGraphicsExtractor g, float cx, float cy, float r, int c) {
        if (r < 0.6f) { g.fill(Math.round(cx), Math.round(cy), Math.round(cx) + 1, Math.round(cy) + 1, c); return; }
        int ir = (int) Math.ceil(r);
        for (int dy = -ir; dy <= ir; dy++) { int hw = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy))); if (hw > 0) g.fill(Math.round(cx) - hw, Math.round(cy) + dy, Math.round(cx) + hw, Math.round(cy) + dy + 1, c); }
    }
    /** A soft round glow: a few stacked discs, faint at the rim. */
    private static void glow(GuiGraphicsExtractor g, float cx, float cy, float r, int rgb, float a) {
        for (int k = 4; k >= 1; k--) disc(g, cx, cy, r * k / 4f, argb(a * alpha * 0.35f, rgb));
    }
    /** The four thin rays of a lit star. */
    private static void spikes(GuiGraphicsExtractor g, float cx, float cy, float len, int rgb, float a) {
        int x = Math.round(cx), y = Math.round(cy), L = Math.round(len);
        for (int i = 1; i <= L; i++) { float f = 1f - i / (float) (L + 1); int c = argb(a * alpha * f * f, rgb); g.fill(x + i, y, x + i + 1, y + 1, c); g.fill(x - i, y, x - i + 1, y + 1, c); g.fill(x, y + i, x + 1, y + i + 1, c); g.fill(x, y - i, x + 1, y - i + 1, c); }
    }
    private static void dotStar(GuiGraphicsExtractor g, float x, float y, float a, int size) {
        int c = argb(a * alpha, 0xC8D0F0); int xi = Math.round(x), yi = Math.round(y);
        g.fill(xi, yi, xi + size, yi + size, c);
        if (size > 1) { g.fill(xi - 1, yi, xi, yi + size, argb(a * alpha * 0.4f, 0xC8D0F0)); g.fill(xi + size, yi, xi + size + 1, yi + size, argb(a * alpha * 0.4f, 0xC8D0F0)); }
    }
    private static void ring(GuiGraphicsExtractor g, float cx, float cy, float r, int c, float upto) {
        int n = Math.max(24, Math.round(r * 6));
        for (int i = 0; i < n; i++) { float u = i / (float) n; if (u > upto) break; double an = -Math.PI / 2 + u * Math.PI * 2; int x = Math.round(cx + (float) Math.cos(an) * r), y = Math.round(cy + (float) Math.sin(an) * r); g.fill(x, y, x + 1, y + 1, c); }
    }
    /** A line of single pixels: dotted for a figure not yet lit; a lit one carries a slow travelling shimmer. */
    private static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int rgb, float a, boolean dotted, boolean shimmer) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0); int n = Math.max(1, Math.round(len));
        for (int i = 3; i < n - 2; i++) {
            if (dotted && i % 3 != 0) continue;
            float u = i / (float) n, x = x0 + (x1 - x0) * u, y = y0 + (y1 - y0) * u;
            if (!inSky(x, y)) continue;
            float s = shimmer ? 0.7f + 0.3f * (float) Math.sin(now / 300.0 - i * 0.25) : 1f;
            g.fill(Math.round(x), Math.round(y), Math.round(x) + 1, Math.round(y) + 1, argb(a * s * alpha, rgb));
        }
    }

    // ------------------------------------------------------------------ input
    static boolean click(double x, double y) { return click(x, y, 0); }
    /** A click while the sky is open. Always true: nothing behind it takes clicks. Right click flies back out. */
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
    /** The wheel moves between constellations while flown in. */
    static boolean scroll(double dy) {
        if (!open) return false;
        if (dy != 0 && focus >= 0) fly(Math.floorMod(focus + (dy < 0 ? 1 : -1), ACH.length));
        return true;
    }
    /** Keys: Esc flies out (or closes), the arrows move between constellations. */
    static boolean key(int key) {
        if (!open) return false;
        if (key == 256) { if (focus >= 0) fly(-1); else close(); }
        else if (key == 262) fly(focus < 0 ? 0 : Math.floorMod(focus + 1, ACH.length));
        else if (key == 263) fly(focus < 0 ? ACH.length - 1 : Math.floorMod(focus - 1, ACH.length));
        return true;
    }
}
