package dev.fancydrops;

import dev.fancydrops.AnimationManager.Anim;
import dev.fancydrops.AnimationManager.Item;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The Mythological animations (Mythological Mobs and Treasure), all three types. One timeline, S seconds long (5 to 7, set per animation):
 *
 *   0 .. S-2.5    the OPENING, identical for No Drops, Normal and RNG.
 *                 Mobs: a ritual circle draws itself on the ground, three lightning strikes crack the ground, then the power is drawn in.
 *                 Treasure: a spade digs three times, a light grows out of the hole, then the light is drawn in.
 *   S-2.5 .. S    the PIVOT, the part that tells the three apart:
 *                 No Drops  the charge fizzles, the circle goes out, smoke and embers (a bone for treasure), grey words.
 *                 Normal    ascension rings and a burst lift the item out of the ground, it glows and sparkles, no backdrop.
 *                 RNG       white flash, shockwaves, god rays, lightning round the item, runes, a fountain of light, bars and blur.
 *
 * For all three: the item appears first, then the background fades while the words show; at the end the item fades slowly and
 * 0.5 s later the words fade. Everything is drawn into a small pixel canvas (one textured quad), then the item and the words on top.
 */
final class Ritual {
    private Ritual() {}

    static final int CW = 320, CH = 180;
    private static final Canvas main = new Canvas(CW, CH), sil = new Canvas(CW, CH);
    private static Canvas cv = main;                                                    // what the drawing helpers paint on

    private static final int CYAN = 0x7FE8FF, GOLD = 0xFFD25A, EMBER = 0xFF8A3A, WHITE = 0xFFFFFF, GREY = 0x6A7388;
    private static final float[] POS = {0.30f, 0.46f, 0.66f};

    // ------------------------------------------------------------------ small maths
    static float cl(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    static float sm(float v) { v = cl(v); return v * v * (3 - 2 * v); }
    static float eo(float v) { v = cl(v); float q = 1 - v; return 1 - q * q * q; }
    static float eob(float v) { v = cl(v); float c1 = 1.9f, c3 = c1 + 1, q = v - 1; return 1 + c3 * q * q * q + c1 * q * q; }
    static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    static float rnd(int n) { n ^= n >>> 16; n *= 0x7feb352d; n ^= n >>> 15; n *= 0x846ca68b; n ^= n >>> 16; return (n & 0xFFFFFF) / 16777216f; }
    static int A(float a, int rgb) { return Canvas.rgb(Math.round(cl(a) * 255), rgb); }
    static int mixc(int a, int b, float f) { return Canvas.mix(0xFF000000 | a, 0xFF000000 | b, f) & 0xFFFFFF; }

    // ------------------------------------------------------------------ pixel drawing
    private static void dot(float x, float y, int rgb, float a) { if (a > 0.01f) cv.set(Math.round(x), Math.round(y), A(a, rgb)); }
    private static void line(float x0, float y0, float x1, float y1, int rgb, float a) {
        int n = (int) Math.max(1, Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
        for (int i = 0; i <= n; i++) { float f = i / (float) n; dot(x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, rgb, a); }
    }
    /** An arc of an ellipse. from..to in radians (a full circle is 0..2pi), rot turns it, dash > 0 breaks it into dashes. */
    private static void ringE(float cx, float cy, float rx, float ry, int rgb, float a, float from, float to, float rot, int dash) {
        int steps = Math.max(24, (int) (6.3f * Math.max(rx, ry) * 1.3f));
        for (int i = 0; i <= steps; i++) {
            float ang = lerp(from, to, i / (float) steps);
            if (dash > 0 && (i / dash) % 2 == 1) continue;
            float cs = (float) Math.cos(ang + rot), sn = (float) Math.sin(ang + rot);
            dot(cx + cs * rx, cy + sn * ry, rgb, a);
        }
    }
    private static void fillE(float cx, float cy, float rx, float ry, int rgb, float a) {
        if (rx < 0.5f || ry < 0.5f) return;
        for (int y = (int) Math.floor(cy - ry); y <= Math.ceil(cy + ry); y++) {
            float v = (y + 0.5f - cy) / ry; if (v * v > 1f) continue;
            float hw = rx * (float) Math.sqrt(1 - v * v);
            for (int x = (int) Math.floor(cx - hw); x <= Math.ceil(cx + hw); x++) cv.set(x, y, A(a, rgb));
        }
    }
    /** A soft elliptical glow: strongest in the middle, nothing at the edge. */
    private static void glowE(float cx, float cy, float rx, float ry, int rgb, float strength) {
        if (strength <= 0.01f || rx < 1 || ry < 1) return;
        for (int y = (int) Math.floor(cy - ry); y <= Math.ceil(cy + ry); y++) for (int x = (int) Math.floor(cx - rx); x <= Math.ceil(cx + rx); x++) {
            float dx = (x + 0.5f - cx) / rx, dy = (y + 0.5f - cy) / ry, d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d >= 1f) continue;
            float f = (1f - d) * (1f - d);
            cv.set(x, y, A(strength * f, rgb));
        }
    }
    private static void glint(float x, float y, int r, int rgb, float a) { if (a > 0.03f) cv.glint(Math.round(x), Math.round(y), r, rgb, cl(a)); }

    /** A forked bolt of lightning from (x0,y0) to (x1,y1): a wide soft glow, a coloured body and a white core. */
    private static void bolt(float x0, float y0, float x1, float y1, int seed, int rgb, float a, int depth, boolean branches) {
        if (depth == 0) {
            line(x0 - 2, y0, x1 - 2, y1, rgb, a * 0.13f); line(x0 + 3, y0, x1 + 3, y1, rgb, a * 0.13f);
            line(x0 - 1, y0, x1 - 1, y1, rgb, a * 0.28f); line(x0 + 2, y0, x1 + 2, y1, rgb, a * 0.28f);
            line(x0 + 1, y0, x1 + 1, y1, rgb, a * 0.8f);
            line(x0, y0, x1, y1, mixc(rgb, WHITE, 0.7f), a);
            return;
        }
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.max(1e-3, Math.hypot(dx, dy));
        float off = (rnd(seed * 31 + depth * 7) - 0.5f) * len * 0.42f;
        float mx = (x0 + x1) / 2 - dy / len * off, my = (y0 + y1) / 2 + dx / len * off;
        bolt(x0, y0, mx, my, seed * 2 + 1, rgb, a, depth - 1, branches);
        bolt(mx, my, x1, y1, seed * 2 + 2, rgb, a, depth - 1, branches);
        if (branches && depth == 4 && rnd(seed + 99) > 0.35f) {
            float ang = (float) Math.atan2(dy, dx) + (rnd(seed + 5) > 0.5f ? 0.7f : -0.7f);
            bolt(mx, my, mx + (float) Math.cos(ang) * len * 0.45f, my + (float) Math.sin(ang) * len * 0.45f, seed * 5 + 3, rgb, a * 0.7f, 2, false);
        }
    }

    /** God rays: n bright wedges turning round (cx, cy), fading out towards reach. Drawn in 2x2 blocks to stay cheap. */
    private static void rays(float cx, float cy, float rot, int n, int rgb, float strength, float reach) {
        if (strength <= 0.01f) return;
        int x0 = Math.max(0, (int) (cx - reach)), x1 = Math.min(CW - 1, (int) (cx + reach)), y0 = Math.max(0, (int) (cy - reach)), y1 = Math.min(CH - 1, (int) (cy + reach));
        for (int y = y0 & ~1; y <= y1; y += 2) for (int x = x0 & ~1; x <= x1; x += 2) {
            float dx = x + 1 - cx, dy = y + 1 - cy, r2 = dx * dx + dy * dy;
            if (r2 > reach * reach || r2 < 16) continue;
            float r = (float) Math.sqrt(r2), th = (float) Math.atan2(dy, dx);
            float f = (float) Math.cos(n * (th - rot)); if (f <= 0) continue;
            f = f * f * f;
            float fall = 1f - r / reach, a = strength * f * fall * fall * (r < 14 ? r / 14f : 1f);
            if (a < 0.02f) continue;
            int c = A(a, rgb);
            cv.set(x, y, c); cv.set(x + 1, y, c); cv.set(x, y + 1, c); cv.set(x + 1, y + 1, c);
        }
    }

    // ------------------------------------------------------------------ the ground
    /** The ritual circle: rings that draw themselves (prog 0..1), runes round them, an eight-pointed star, a glow on the ground. dead > 0 puts it out. */
    private static void sigil(float t, float prog, float cx, float cy, float rx, float ry, int rgb, float a, float dead) {
        if (a <= 0.01f || prog <= 0f) return;
        float spin = t * 0.45f, tau = 6.2832f;
        if (dead > 0f) rgb = mixc(rgb, GREY, cl(dead * 1.4f));
        glowE(cx, cy, rx * 1.2f, ry * 1.7f, rgb, 0.30f * a * prog);
        float p1 = cl(prog * 1.4f), p2 = cl(prog * 1.4f - 0.25f), p3 = cl(prog * 1.4f - 0.5f);
        ringE(cx, cy, rx, ry, rgb, 0.95f * a, 0, tau * p1, spin * 0.3f, 0);
        ringE(cx, cy + 1, rx, ry, rgb, 0.55f * a, 0, tau * p1, spin * 0.3f, 0);
        fillE(cx, cy, rx, ry, rgb, 0.07f * a * p1);
        ringE(cx, cy, rx + 2, ry + 0.8f, rgb, 0.35f * a, 0, tau * p1, spin * 0.3f, 3);
        ringE(cx, cy, rx * 0.78f, ry * 0.78f, rgb, 0.85f * a, 0, tau * p2, -spin * 0.5f, 0);
        ringE(cx, cy, rx * 0.42f, ry * 0.42f, mixc(rgb, WHITE, 0.35f), 0.9f * a, 0, tau * p3, spin, 0);
        for (int i = 0; i < 16; i++) {                                                   // runes between the rings
            if (i / 16f > p2) break;
            if (dead > 0f && rnd(i * 13 + (int) (t * 11)) < dead) continue;
            float an = i * tau / 16f + spin * 0.3f, cs = (float) Math.cos(an), sn = (float) Math.sin(an);
            float lo = i % 2 == 0 ? 0.84f : 0.86f, hi = i % 2 == 0 ? 0.97f : 0.92f;
            line(cx + cs * rx * lo, cy + sn * ry * lo, cx + cs * rx * hi, cy + sn * ry * hi, mixc(rgb, WHITE, 0.4f), 0.9f * a);
        }
        for (int i = 0; i < 8 && p3 > 0.3f; i++) {                                       // the star
            float a0 = i * tau / 8f - spin * 0.5f, a1 = (i + 3) * tau / 8f - spin * 0.5f;
            line(cx + (float) Math.cos(a0) * rx * 0.78f, cy + (float) Math.sin(a0) * ry * 0.78f, cx + (float) Math.cos(a1) * rx * 0.78f, cy + (float) Math.sin(a1) * ry * 0.78f, rgb, 0.5f * a * p3);
        }
    }

    /** Cracks in the ground, n of them, each glowing while it is fresh. */
    private static void cracks(float t, float cx, float cy, float rx, float ry, int n, float glow, float a) {
        for (int j = 0; j < n; j++) {
            float an = j * 2.399f + 0.4f, cs = (float) Math.cos(an), sn = (float) Math.sin(an), len = rx * (0.55f + 0.4f * rnd(j * 17 + 3));
            float px = cx, py = cy;
            for (int s = 1; s <= 6; s++) {
                float f = s / 6f, jx = (rnd(j * 31 + s) - 0.5f) * 6f, jy = (rnd(j * 41 + s) - 0.5f) * 3f;
                float nx = cx + cs * len * f + jx, ny = cy + sn * len * f * ry / rx + jy;
                line(px, py, nx, ny, 0x1A0E08, 0.9f * a);
                line(px, py - 1, nx, ny - 1, mixc(GOLD, EMBER, f), (0.25f + 0.7f * glow) * a);
                px = nx; py = ny;
            }
        }
    }

    /** A column of motes turning round and rising (the ritual's breath). */
    private static void motes(float t, float cx, float cy, float rx, float ry, float height, int rgb, float a, int n) {
        for (int i = 0; i < n; i++) {
            float ph = rnd(i * 7 + 1), u = (t * (0.22f + 0.18f * rnd(i)) + ph) % 1f;
            float an = ph * 6.28f + t * (1.1f + rnd(i * 3) * 0.8f) * (i % 2 == 0 ? 1 : -1), rr = (0.85f - 0.55f * u) * (0.6f + 0.4f * rnd(i * 5));
            float x = cx + (float) Math.cos(an) * rx * rr, y = cy + (float) Math.sin(an) * ry * rr - u * height;
            float al = (float) Math.sin(u * Math.PI) * a;
            dot(x, y, mixc(rgb, WHITE, 0.4f), al);
            if (i % 4 == 0) glint(x, y, 2, rgb, al * 0.8f);
        }
    }

    /** The power being drawn in: motes spiral from far away into (tx, ty), and a pillar of light stands over the ground. */
    private static void inhale(float tt, float cx, float gy, float tx, float ty, float rx, float ry, int rgb, float a, float sz) {
        if (tt <= 0f) return;
        float e = tt * tt;
        float w = lerp(1f, 7f * sz, e);                                                  // the pillar
        for (int y = 0; y < gy; y += 1) {
            float f = 1f - y / (float) gy;
            for (int x = (int) -w; x <= w; x++) cv.set(Math.round(cx + x), y, A(a * (0.10f + 0.30f * e) * (1f - Math.abs(x) / (w + 1f)) * (0.5f + 0.5f * f), mixc(rgb, WHITE, 0.4f)));
        }
        for (int i = 0; i < 40; i++) {
            float an = rnd(i * 5) * 6.28f + tt * 2.8f, dist = 0.5f + 0.9f * rnd(i * 9 + 2), k = cl(tt * 1.15f - rnd(i) * 0.15f), q = 1f - k * k;
            float x = cx + (float) Math.cos(an) * rx * dist * q, y = lerp(gy + (float) Math.sin(an) * ry * dist * q, ty, k * k);
            dot(x, y, mixc(rgb, WHITE, 0.5f), a * Math.min(1f, k * 5f) * (1f - 0.3f * k)); if (i % 3 == 0) dot(x + 1, y, rgb, a * 0.6f);
        }
        glowE(tx, ty, (10 + 34 * e) * sz, (10 + 34 * e) * sz, rgb, 0.55f * a * e);
    }

    // ------------------------------------------------------------------ the summoned creature
    /** The minotaur the ritual calls: a dark horned shape that materialises from the ground up, glares, then shatters (shards fly) or crumbles (ash falls). */
    private static void minotaur(float cx, float gy, float k, float reveal, float breakT, boolean crumble, int rc, int rim, float a, float t) {
        if (reveal <= 0f || a <= 0.01f) return;
        Canvas saved = cv; cv = sil; sil.clear();
        int d = 0xFF070A14;
        fillE(cx - 8 * k, gy - 10 * k, 7 * k, 12 * k, d & 0xFFFFFF, 1f); fillE(cx + 8 * k, gy - 10 * k, 7 * k, 12 * k, d & 0xFFFFFF, 1f);     // legs
        fillE(cx, gy - 34 * k, 17 * k, 25 * k, d & 0xFFFFFF, 1f);                                                                               // torso
        fillE(cx, gy - 54 * k, 27 * k, 11 * k, d & 0xFFFFFF, 1f);                                                                               // shoulders
        fillE(cx - 28 * k, gy - 40 * k, 6 * k, 18 * k, d & 0xFFFFFF, 1f); fillE(cx + 28 * k, gy - 40 * k, 6 * k, 18 * k, d & 0xFFFFFF, 1f);   // arms
        fillE(cx - 29 * k, gy - 21 * k, 6 * k, 6 * k, d & 0xFFFFFF, 1f); fillE(cx + 29 * k, gy - 21 * k, 6 * k, 6 * k, d & 0xFFFFFF, 1f);       // fists
        fillE(cx, gy - 68 * k, 10 * k, 11 * k, d & 0xFFFFFF, 1f); fillE(cx, gy - 62 * k, 6 * k, 5 * k, d & 0xFFFFFF, 1f);                       // head and snout
        for (int s = -1; s <= 1; s += 2) {                                                                                                         // the horns
            float[] hx = {8, 16, 21, 23, 20}, hy = {-74, -75, -80, -88, -95};
            for (int i = 0; i < 4; i++) for (int w = -1; w <= 1; w++) line(cx + s * hx[i] * k + w, gy + hy[i] * k, cx + s * hx[i + 1] * k + w, gy + hy[i + 1] * k, d & 0xFFFFFF, 1f);
        }
        cv = saved;
        int[] sp = sil.px; float cy0 = gy - 45 * k, top = 100 * k;
        boolean[] edge = new boolean[sp.length];
        for (int y = 1; y < CH - 1; y++) for (int x = 1; x < CW - 1; x++) if (sp[y * CW + x] >>> 24 == 0 && ((sp[y * CW + x - 1] >>> 24) > 100 || (sp[y * CW + x + 1] >>> 24) > 100 || (sp[(y - 1) * CW + x] >>> 24) > 100 || (sp[(y + 1) * CW + x] >>> 24) > 100)) edge[y * CW + x] = true;
        for (int y = 0; y < CH; y++) for (int x = 0; x < CW; x++) {
            int i = y * CW + x; boolean body = sp[i] >>> 24 > 100;
            if (!body && !edge[i]) continue;
            float h = rnd(x * 7 + y * 131);
            if (breakT < 0f) {                                                          // standing: appears from the ground upward, dissolving in
                float hgt = (gy - y) / top; if (hgt > reveal * 1.08f || h > 0.35f + 0.65f * cl(reveal * 1.4f - hgt * 0.5f)) continue;
                float flick = 0.88f + 0.12f * (float) Math.sin(t * 9f + y * 0.3f);
                if (body) cv.set(x, y, A(a * 0.92f, mixc(0x070A14, rim, 0.06f))); else cv.set(x, y, A(a * 0.85f * flick, rim));
            } else if (h < 0.55f && (x + y) % 2 == 0) {                                   // breaking: every other pixel is a shard
                float dx = x - cx, dy = y - cy0, len = (float) Math.max(1, Math.hypot(dx, dy));
                float sp0 = crumble ? 4f + 10f * h : 30f + 80f * rnd(x * 3 + y * 17);
                float px = x + (crumble ? (rnd(x + y) - 0.5f) * 8f * breakT : dx / len * sp0 * breakT);
                float py = y + (crumble ? 16f * breakT * (0.4f + h) : dy / len * sp0 * breakT * 0.8f - 10f * breakT) + (crumble ? 0f : 70f * breakT * breakT);
                float al = a * (1f - cl(breakT / (crumble ? 1.4f : 1.1f))); if (al < 0.03f) continue;
                int col = crumble ? mixc(0x2A2E3A, GREY, h) : body ? mixc(rim, rc, cl(breakT * 2.2f)) : mixc(WHITE, rc, cl(breakT * 2f));
                dot(px, py, col, al); if (h < 0.2f) dot(px + 1, py, col, al * 0.8f);
            }
        }
        if (breakT < 0f && reveal > 0.55f) {                                              // glaring eyes
            float e = cl((reveal - 0.55f) / 0.3f) * (0.7f + 0.3f * (float) Math.sin(t * 7f)) * a;
            for (int s = -1; s <= 1; s += 2) { float ex = cx + s * 4.5f * k, ey = gy - 69 * k; glowE(ex, ey, 6 * k, 5 * k, 0xFF4A2A, 0.7f * e); dot(ex, ey, 0xFFE0B0, e); dot(ex + s, ey, 0xFF8A4A, e); }
        }
    }

    // ------------------------------------------------------------------ treasure: the dig
    private static final float[] DIG = {0.14f, 0.43f, 0.72f};

    private static void spade(float x, float tipY, int blade, float a) {
        float hx = x + 15, hy = tipY - 34;
        for (int k = -1; k <= 1; k++) line(x + 2 + k, tipY - 7, hx + k, hy, 0x5B3A1C, a);                // the shaft
        line(x + 2, tipY - 7, hx, hy, 0x8A5A2C, a);
        line(hx - 3, hy, hx + 3, hy, 0x3A2410, a); line(hx - 3, hy - 1, hx + 3, hy - 1, 0x6B4423, a);     // the grip
        for (int r = 0; r < 8; r++) {                                                                      // the blade, a rounded diamond
            float wd = r < 5 ? 1 + r * 0.9f : 5.5f - (r - 4) * 1.5f;
            line(x - wd + 2, tipY - 7 + r, x + wd + 2, tipY - 7 + r, r < 2 ? mixc(blade, WHITE, 0.45f) : r < 5 ? blade : mixc(blade, 0x000000, 0.25f), a);
        }
    }

    /** The mound, the hole, the spade and the clumps of dirt. Returns how far the dig has got (0..1). */
    private static float dig(float t, float O, float cx, float gy, float rx, float ry, float sz, int blade, float a) {
        float done = 0;
        for (int k = 0; k < 3; k++) done += sm((t - DIG[k] * O) / (0.15f * O));
        done /= 3f;
        fillE(cx, gy + 4 * sz, rx * 1.05f, ry * 1.05f, 0x000000, 0.28f * a);
        fillE(cx, gy + 1, rx * 0.92f, ry * 0.9f, 0x4A3220, a);
        fillE(cx, gy - 1, rx * 0.82f, ry * 0.76f, 0x6B4A2B, a);
        fillE(cx - rx * 0.18f, gy - ry * 0.3f, rx * 0.5f, ry * 0.35f, 0x82603A, a * 0.9f);
        for (int i = 0; i < 14; i++) dot(cx + (rnd(i * 3) - 0.5f) * rx * 1.5f, gy + (rnd(i * 5 + 1) - 0.5f) * ry * 1.3f, i % 3 == 0 ? 0x9A7A52 : 0x3A2616, 0.9f * a);
        for (int i = 0; i < 9; i++) { float gx = cx + (rnd(i * 11) - 0.5f) * rx * 2f; dot(gx, gy + ry * 0.8f + rnd(i) * 2, 0x4F8A3A, a); dot(gx + 1, gy + ry * 0.8f - 1 + rnd(i) * 2, 0x6FB04A, a); }   // grass
        float rh = rx * (0.13f + 0.32f * done), rhy = rh * ry / rx;
        fillE(cx, gy + 1, rh + 1, rhy + 1, 0x2B1B10, a);
        fillE(cx, gy + 1, rh, rhy, 0x0C0705, a);
        glowE(cx, gy, rh * 2.4f, rhy * 3.2f, GOLD, (0.10f + 0.50f * done) * a);
        if (done > 0.04f) { float h = 52 * sz * (0.2f + 0.8f * done); for (int y = 0; y < h; y++) { float f = 1f - y / h; for (int x = -(int) (rh * 0.6f); x <= rh * 0.6f; x++) cv.set(Math.round(cx + x), Math.round(gy - y), A(0.35f * f * a, mixc(GOLD, WHITE, 0.4f))); } }
        for (int k = 0; k < 3; k++) {                                                                        // flying dirt
            float tau = t - (DIG[k] * O + 0.075f * O);
            if (tau < 0 || tau > 0.8f) continue;
            for (int j = 0; j < 16; j++) {
                float vx = (rnd(k * 50 + j) - 0.5f) * 110, vy = -(50 + rnd(k * 60 + j) * 90);
                float x = cx + vx * tau, y = gy + vy * tau + 120 * tau * tau;
                int c = j % 3 == 0 ? 0x9A7A52 : j % 3 == 1 ? 0x6B4A2B : 0x3A2616;
                float al = (1f - tau / 0.8f) * a; dot(x, y, c, al); dot(x + 1, y, c, al); dot(x, y + 1, c, al); dot(x + 1, y + 1, c, al * 0.8f);
            }
        }
        for (int k = 0; k < 3; k++) {                                                                        // the spade
            float u = (t - DIG[k] * O) / (0.15f * O);
            if (u < -0.45f || u > 1.3f) continue;
            float y = u < 0 ? gy - 34 * sz : u < 0.5f ? lerp(gy - 34 * sz, gy + 1, eo(u / 0.5f)) : lerp(gy + 1, gy - 34 * sz, sm((u - 0.5f) / 0.8f));
            float vis = u < 0 ? sm((u + 0.45f) / 0.45f) : 1f - sm((u - 1.0f) / 0.3f) * (k == 2 ? 1f : 0f);
            spade(cx + 8 * sz, y, blade, a * vis);
        }
        return done;
    }

    // ------------------------------------------------------------------ small pieces for the pivot
    private static void fountain(float t, float cx, float cy, float t0, int n, int rgb, float a, float life, float power) {
        if (t < t0) return;
        for (int j = 0; j < n; j++) {
            float tau = (t - t0 + rnd(j * 7) * life) % life, fl = tau / life;
            float x = cx + (rnd(j * 3 + 1) - 0.5f) * 110 * power * tau, y = cy - (45 + rnd(j * 5 + 2) * 75) * power * tau + 150 * tau * tau;
            int c = j % 4 == 0 ? WHITE : j % 4 == 1 ? GOLD : rgb;
            dot(x, y, c, a * (1f - fl)); if (j % 2 == 0) dot(x + 1, y, c, a * (1f - fl) * 0.7f);
            if (j % 6 == 0) glint(x, y, 2, c, a * (1f - fl));
        }
    }
    private static void orbit(float t, float cx, float cy, float rx, float ry, float tilt, int n, int rgb, float a, float speed) {
        for (int i = 0; i < n; i++) {
            float an = t * speed + i * 6.2832f / n, x = (float) Math.cos(an) * rx, y = (float) Math.sin(an) * ry;
            float px = cx + x * (float) Math.cos(tilt) - y * (float) Math.sin(tilt), py = cy + x * (float) Math.sin(tilt) + y * (float) Math.cos(tilt);
            float depth = 0.55f + 0.45f * (float) Math.sin(an);
            glint(px, py, 2 + (i % 2), i % 3 == 0 ? WHITE : rgb, a * depth);
        }
    }
    private static void smoke(float p, float cx, float gy, float rx, float sz, float a) {
        for (int i = 0; i < 9; i++) {
            float s0 = rnd(i * 7) * 0.7f, u = (p - s0) / 1.5f; if (u < 0 || u > 1) continue;
            float x = cx + (rnd(i * 3 + 2) - 0.5f) * rx * 0.9f + (float) Math.sin(u * 4 + i) * 5, y = gy - u * 46 * sz;
            glowE(x, y, (6 + 14 * u) * sz, (5 + 11 * u) * sz, 0x8A93A6, 0.34f * (1f - u) * a);
        }
        for (int i = 0; i < 14; i++) {                                                  // embers falling back down
            float s0 = rnd(i * 5 + 1) * 0.9f, u = (p - s0) / 1.1f; if (u < 0 || u > 1) continue;
            float x = cx + (rnd(i * 2) - 0.5f) * rx * 0.8f, y = gy - 24 * sz * (float) Math.sin(u * 2.4f) + u * u * 30;
            dot(x, y, mixc(EMBER, 0x55331A, u), (1f - u) * 0.9f * a);
        }
    }
    private static void bone(float p, float cx, float gy, float sz) {
        float u = (p - 0.15f) / 1.0f; if (u < 0 || u > 1) return;
        float x = cx + 14 * sz * u, y = gy - 34 * sz * 4 * u * (1 - u) + 2 * sz * u, rot = u * 5f;
        float dx = (float) Math.cos(rot) * 4, dy = (float) Math.sin(rot) * 4;
        line(x - dx, y - dy, x + dx, y + dy, 0xE8E2D0, 1f); line(x - dx, y - dy + 1, x + dx, y + dy + 1, 0xA8A290, 1f);
        dot(x - dx, y - dy - 1, 0xE8E2D0, 1f); dot(x + dx, y + dy - 1, 0xE8E2D0, 1f); dot(x - dx, y - dy + 2, 0xE8E2D0, 1f); dot(x + dx, y + dy + 2, 0xE8E2D0, 1f);
    }
    /** A small chest rising out of the hole; open (0..1) lifts the lid and lets the light out. */
    private static void chest(float cx, float by, float sz, float open, float a) {
        float w = 15 * sz, h = 9 * sz, x0 = cx - w, lid = open * 7 * sz;
        for (int y = 0; y < h; y++) for (int x = 0; x < w * 2; x++) { int c = y < 2 ? 0x9A6B34 : y > h - 3 ? 0x4A2E14 : (x < 2 || x > w * 2 - 3) ? 0x3A2410 : ((int) x % 6 == 0 ? 0x6B4423 : 0x7C522A); cv.set(Math.round(x0 + x), Math.round(by + y), A(a, c)); }
        for (int x = 0; x < w * 2; x++) { cv.set(Math.round(x0 + x), Math.round(by + h * 0.35f), A(a, 0xD8A52A)); }
        if (open > 0.02f) { for (int x = 0; x < w * 2 - 4; x++) for (int y = -2; y < 1; y++) cv.set(Math.round(x0 + 2 + x), Math.round(by + y), A(a * open, mixc(GOLD, WHITE, 0.7f))); }
        float ly = by - 6 * sz - lid;
        for (int y = 0; y < 6 * sz; y++) for (int x = 0; x < w * 2; x++) { float ar = (float) Math.sin((x / (w * 2)) * 3.1416f); if (y < 6 * sz - ar * 2f * sz && y > -1) { int c = y < 2 ? 0xB07C3C : (x < 2 || x > w * 2 - 3) ? 0x3A2410 : 0x8A5A2C; cv.set(Math.round(x0 + x), Math.round(ly + y + (open > 0 ? -open * 1.5f : 0)), A(a, c)); } }
        for (int y = 0; y < 4; y++) for (int x = -2; x <= 2; x++) cv.set(Math.round(cx + x), Math.round(by + h * 0.2f + y), A(a, y < 1 ? 0xFFE27A : 0xD8A52A));      // the lock
    }

    // ------------------------------------------------------------------ the whole scene
    /** The canvas of the scene at time t: the opening, then the pivot of the type. Everything except the item and the words. */
    static void paint(float t, boolean tre, int type, float S, float sz, float isc, int rc, float icy, int blade, boolean itemBob, float bobAmp) {
        boolean rng = type == AnimationManager.RNG, nod = type == AnimationManager.NO_DROPS;
        float O = S - TAIL, p = t - O, icx = CW / 2f, gy = icy + 22 * sz, rx = 50 * sz, ry = 13 * sz;
        int energy = tre ? GOLD : CYAN;
        float bg = 1f - sm((p - 0.5f) / 0.9f);                                             // the background fades while the words show
        float itemA = TextFx.itemOnly ? 1f : sm(p / 0.22f) * (1f - sm((t - (S - 1.0f)) / 0.9f));
        float tb = TextFx.itemOnly ? (System.currentTimeMillis() % 100000L) / 1000f : t;                // the item keeps bobbing while the Item editor holds the scene                  // the item fades slowly at the end
        cv.clear();
        float inh = cl((t - 0.78f * O) / (0.22f * O));                                     // the last stretch of the opening: power drawn in
        float op = Math.min(t, O);

        // ---- THE OPENING (the same for every type)
        if (!tre) {
            float draw = sm(t / (0.35f * O));
            float sigA = nod ? 1f - cl(p / 0.9f) * 0.2f : 1f;
            sigil(t, draw, icx, gy, rx, ry, rng && p > 0 ? mixc(CYAN, rc, sm(p / 0.4f)) : energy, bg * sigA, nod && p > 0 ? cl(p / 0.8f) : 0f);
            float kk = Math.min(sz, (gy - 4) / 98f), rev = sm((t - 0.30f * O) / (0.55f * O));
            minotaur(icx, gy - 2, kk, rev, p < 0 ? -1f : p, nod, rc, nod ? GREY : mixc(energy, rc, rng || p > 0 ? 0.5f : 0f), nod && p > 0 ? 1f : bg > 0.2f ? 1f : 0f, t);
            int strikes = 0;
            float[] ts = {0.34f * O, 0.58f * O, 0.80f * O};
            for (int k = 0; k < 3; k++) {
                float tau = op - ts[k];
                if (tau < 0) continue;
                strikes++;
                if (tau < 0.30f) {
                    int seed = k * 101 + (int) (op * 28);
                    float fl = (rnd(seed) > 0.3f ? 1f : 0.45f) * (1f - tau / 0.30f), x0 = icx + (rnd(k * 9) - 0.5f) * 36;
                    bolt(x0, -4, icx + (rnd(k * 5) - 0.5f) * 5, gy, seed, energy, fl, 5, true);
                    if (k == 2) bolt(x0 - 22, -4, icx - 6, gy, seed + 400, energy, fl * 0.7f, 4, false);
                    glowE(icx, gy, 46 * sz * (1f - tau / 0.3f * 0.5f), 20 * sz, WHITE, 0.85f * fl);
                }
                if (tau < 0.7f) for (int j = 0; j < 12; j++) {                              // sparks thrown off the impact
                    float vx = (rnd(k * 40 + j) - 0.5f) * 150, vy = -(30 + rnd(k * 70 + j) * 90);
                    dot(icx + vx * tau, gy + vy * tau + 220 * tau * tau, j % 3 == 0 ? WHITE : energy, (1f - tau / 0.7f) * 0.95f);
                }
                float sw = tau / 0.75f;
                if (sw < 1f) ringE(icx, gy, rx * (0.4f + 1.4f * eo(sw)), ry * (0.4f + 1.4f * eo(sw)), mixc(energy, WHITE, 0.5f), (1f - sw) * 0.9f * bg, 0, 6.2832f, 0, 0);
            }
            cracks(op, icx, gy, rx, ry, strikes * 3, strikes > 0 ? cl(1f - (op - ts[strikes - 1]) / 0.8f) : 0f, bg * (nod ? 1f - cl(p / 0.9f) : 1f));
            motes(t, icx, gy, rx, ry, 70 * sz, energy, 0.9f * bg, 26);
        } else {
            float done = dig(op, O, icx, gy, rx, ry, sz, blade, Math.max(bg, 0f));
            if (done > 0.3f) motes(t, icx, gy, rx * 0.5f, ry * 0.5f, 60 * sz, GOLD, 0.9f * bg * done, 18);
        }
        if (inh > 0f && p < 0.35f) inhale(inh, icx, gy, icx, icy, rx, ry, energy, 1f - cl(p / 0.35f), sz);
        if (p < 0 && inh > 0) glowE(icx, gy, rx * 0.8f * inh, ry * 1.2f * inh, energy, 0.5f * inh);

        // ---- THE PIVOT
        if (p < 0f) return;
        if (nod) {
            float fz = cl(p / 0.5f);
            if (p < 0.4f) { int seed = (int) (p * 20) + 900; float fl = (rnd(seed) > 0.4f ? 0.8f : 0.25f) * (1f - p / 0.4f); bolt(icx + 8, 18 * sz, icx, gy, seed, GREY, fl, 4, false); }
            glowE(icx, gy - 4, (22 - 14 * fz) * sz, (14 - 8 * fz) * sz, tre ? GOLD : EMBER, 0.55f * (1f - fz) * bg);
            smoke(p, icx, gy, rx, sz, bg);
            if (tre) bone(p, icx, gy, sz);
            return;
        }
        float cx0 = icx, cy0 = itemY(t, p, gy, icy, sz, itemBob, bobAmp);
        float pulse = 0.85f + 0.15f * (float) Math.sin(t * 5f);
        if (!rng) {                                                                        // ---- NORMAL: ascension rings, a glow, orbiting sparkles
            float sw = p / 0.8f; if (sw < 1f) ringE(icx, gy, rx * (0.5f + 1.5f * eo(sw)), ry * (0.5f + 1.5f * eo(sw)), mixc(rc, WHITE, 0.45f), (1f - sw) * 0.95f, 0, 6.2832f, 0, 0);
            for (int j = 0; j < 4; j++) {
                float tau = (p - j * 0.12f) / 0.75f; if (tau < 0 || tau > 1) continue;
                float ry2 = lerp(gy, icy - 8 * sz, eo(tau)), rr = rx * 0.62f * (1f - 0.25f * tau);
                ringE(icx, ry2, rr, rr * 0.26f, mixc(rc, WHITE, 0.3f), (1f - tau) * 0.9f, 0, 6.2832f, 0, 0);
                ringE(icx, ry2 + 1, rr, rr * 0.26f, rc, (1f - tau) * 0.4f, 0, 6.2832f, 0, 0);
            }
            glowE(cx0, cy0, 32 * sz * isc * pulse, 32 * sz * isc * pulse, rc, 0.5f * itemA);
            glowE(cx0, cy0, 15 * sz * isc, 15 * sz * isc, WHITE, 0.22f * itemA);
            orbit(t, cx0, cy0, 26 * sz * isc, 8 * sz * isc, -0.3f, 6, rc, 0.95f * itemA, 2.1f);
            fountain(t, cx0, cy0 + 8, O + 0.15f, 14, rc, 0.8f * itemA, 1.3f, 0.45f);
            glowE(icx, gy, rx * 1.1f * (1f - cl(p / 0.9f)), ry * 1.6f * (1f - cl(p / 0.9f)), rc, 0.4f * bg);
            return;
        }
        // ---- RNG: the big finish
        for (int j = 0; j < 3; j++) {                                                      // shockwaves along the ground
            float tau = (p - j * 0.14f) / 1.1f; if (tau < 0 || tau > 1) continue;
            ringE(icx, gy, rx * (0.5f + 3.6f * eo(tau)), ry * (0.5f + 3.6f * eo(tau)), j == 1 ? WHITE : rc, (1f - tau) * 0.9f * bg, 0, 6.2832f, 0, 0);
        }
        float ab = p / 0.9f; if (ab < 1f) ringE(cx0, cy0, 12 + 150 * eo(ab), 12 + 150 * eo(ab), mixc(rc, WHITE, 0.6f), (1f - ab) * 0.8f, 0, 6.2832f, 0, 0);
        rays(cx0, cy0, t * 0.35f, 14, mixc(rc, WHITE, 0.5f), 0.55f * bg * sm(p / 0.6f), 190);
        rays(cx0, cy0, -t * 0.22f + 0.1f, 9, rc, 0.30f * bg * sm(p / 0.8f), 150);
        glowE(cx0, cy0, 70 * sz * pulse, 70 * sz * pulse, rc, 0.55f * (bg * 0.6f + 0.4f * itemA));
        glowE(cx0, cy0, 26 * sz * isc, 26 * sz * isc, WHITE, 0.3f * itemA);
        ringE(cx0, cy0, 36 * sz * isc, 36 * sz * isc, rc, 0.8f * itemA, 0, 6.2832f, t * 0.9f, 4);                   // turning halos
        ringE(cx0, cy0, 45 * sz * isc, 45 * sz * isc, mixc(rc, WHITE, 0.5f), 0.55f * itemA, 0, 6.2832f, -t * 0.6f, 2);
        orbit(t, cx0, cy0, 40 * sz * isc, 12 * sz * isc, 0.35f, 8, rc, 0.95f * itemA, 1.8f);
        orbit(t, cx0, cy0, 34 * sz * isc, 10 * sz * isc, -0.5f, 6, WHITE, 0.8f * itemA, -2.4f);
        for (int k = 0; k < 5; k++) {                                                      // lightning round the item, never the same twice
            int seed = k * 17 + (int) (t * 14) * 5;
            float an = rnd(seed) * 6.2832f, len = (26 + 24 * rnd(seed + 1)) * sz;
            if (rnd(seed + 2) < 0.55f * itemA) bolt(cx0 + (float) Math.cos(an) * 14 * sz, cy0 + (float) Math.sin(an) * 14 * sz, cx0 + (float) Math.cos(an) * (14 * sz + len), cy0 + (float) Math.sin(an) * (14 * sz + len), seed, rc, 0.9f * itemA, 3, false);
        }
        fountain(t, cx0, cy0 + 6, O + 0.1f, 50, rc, itemA, 1.5f, 0.85f);
        if (tre) { float cu = sm(p / 0.7f); chest(icx, gy - 15 * sz * cu - 2, sz * 1.5f, sm((p - 0.4f) / 0.4f), bg); }
    }

    /** Where the item is (canvas units, y): it springs out of the ground, overshoots, then bobs gently. */
    private static float itemY(float t, float p, float gy, float icy, float sz, boolean bob, float amp) {
        float rise = eob(p / 0.85f);
        return lerp(gy - 4, icy, rise) + (bob ? (float) Math.sin(t * 2.2f) * 1.8f * sz * amp * sm((p - 0.8f) / 0.6f) : 0f);
    }

    /** Seconds from the pivot (the moment the drop appears) to the end. */
    static final float TAIL = 3.2f;
    /** The scene is drawn 25% smaller than before: that is the new 100% of the Size slider. */
    static final float SMALLER = 0.75f;

    /** Draws one animation at time t (seconds) in a W x H screen: the scene, the bars, the item and the words. */
    static void draw(GuiGraphicsExtractor g, Font font, Anim a, float t, int W, int H) {
        FancyConfig c = FancyConfig.get();
        FancyConfig.AnimCfg cfg = a.cfg();
        Item it = a.item;
        boolean tre = a.group.equals("treasure");
        int type = a.type;
        boolean rng = type == AnimationManager.RNG, nod = type == AnimationManager.NO_DROPS;
        float S = Math.max(3.6f, cfg.secs), O = S - TAIL, p = t - O;
        float csz = Math.max(0.5f, Math.min(1.2f, cfg.size)), sz = SMALLER * csz, isc = Math.max(0.4f, c.itemScale);
        int rc = it != null ? (it.color() & 0xFFFFFF) : GREY;
        if (!tre && !rng && !nod) rc = mixc(rc, WHITE, 0.15f);
        boolean barsOn = rng && cfg.bars, blurOn = rng && cfg.blur;

        // where everything is (canvas units)
        float barH = CH * Math.max(0.052f, Math.min(0.156f, c.barPx / 96f));
        float hi = 16 * sz * isc * (rng ? 1.2f : 1f);
        float icy = CH * POS[Math.max(0, Math.min(2, cfg.pos))];
        if (barsOn) icy = Math.max(barH + hi + 4, Math.min(CH - barH - hi - 14 * sz, icy));
        float u = (float) H / CH;                                                          // gui units per canvas unit
        float itemA = TextFx.itemOnly ? 1f : sm(p / 0.22f) * (1f - sm((t - (S - 1.0f)) / 0.9f));
        float tb = TextFx.itemOnly ? (System.currentTimeMillis() % 100000L) / 1000f : t;                // the item keeps bobbing while the Item editor holds the scene
        float textT = t - (O + 0.45f), textOut = sm((t - (S - 0.6f)) / 0.5f);               // the words leave after the item
        float barSlide = Math.min(sm((t - (O - 0.8f)) / 0.6f), 1f - sm((t - (S - 0.5f)) / 0.45f));
        float shake = rng && p > 0 && p < 0.5f ? (1f - p / 0.5f) * 3f : 0f;
        int sx = Math.round((rnd((int) (t * 60)) - 0.5f) * 2f * shake * u), sy = Math.round((rnd((int) (t * 60) + 77) - 0.5f) * 2f * shake * u);

        if (!TextFx.textOnly) {
            // ---- the blur: the world dims and softens at the edges (RNG, switchable)
            if (blurOn && barSlide > 0.01f) {
                g.fill(0, 0, W, H, Gfx.argb(Math.round(110 * barSlide), 0x04050E));
                for (int i = 0; i < 7; i++) { int in = Math.round(W * 0.05f * i), inY = Math.round(H * 0.05f * i); int al = Math.round(26 * barSlide); g.fill(0, 0, W, inY + 1, Gfx.argb(al, 0x000000)); g.fill(0, H - inY - 1, W, H, Gfx.argb(al, 0x000000)); g.fill(0, 0, in + 1, H, Gfx.argb(al, 0x000000)); g.fill(W - in - 1, 0, W, H, Gfx.argb(al, 0x000000)); }
            }
            paint(t, tre, type, S, sz, isc, rc, icy, TreasureData.COLS[Math.max(0, Math.min(2, c.mythSpade))] & 0xFFFFFF, c.itemBob, c.itemBobAmp);
            SceneTexture.upload(cv);
            if (!SceneTexture.draw(g, cv, sx, sy, W, H, 1f)) { int cell = Math.max(1, Math.round((float) W / CW)); cv.flush((x0, y0, x1, y1, col) -> g.fill(x0 + sx, y0 + sy, x1 + sx, y1 + sy, col), 0, 0, cell, 1f); }
            if (rng && p > 0 && p < 0.45f) g.fill(0, 0, W, H, Gfx.argb(Math.round(235 * (1f - p / 0.45f) * (1f - p / 0.45f)), 0xFFFFFF));                  // the flash
            else if (!tre && !rng) for (int k = 0; k < 3; k++) { float tau = t - 0.34f * O - k * 0.24f * O; if (tau > 0 && tau < 0.18f) g.fill(0, 0, W, H, Gfx.argb(Math.round(26 * (1f - tau / 0.18f)), 0xD8F6FF)); }
        }

        // ---- the bars
        if (barsOn && barSlide > 0.01f && !TextFx.textOnly) {
            int bh = Math.round(barH * u * barSlide), oa = Math.round(255 * c.barOpacity / 100f);
            int bc = c.barCustom ? (c.barColor & 0xFFFFFF) : mixc(0x05060C, rc, 0.10f);
            g.fill(0, 0, W, bh, Gfx.argb(oa, bc)); g.fill(0, H - bh, W, H, Gfx.argb(oa, bc));
            if (c.barSoft) for (int i = 1; i <= 6; i++) { int al = Math.round(oa * (1f - i / 7f) * 0.5f); g.fill(0, bh + i - 1, W, bh + i, Gfx.argb(al, bc)); g.fill(0, H - bh - i, W, H - bh - i + 1, Gfx.argb(al, bc)); }
            if (c.barLine) { g.fill(0, bh, W, bh + 1, Gfx.argb(Math.round(210 * barSlide), mixc(rc, WHITE, 0.3f))); g.fill(0, H - bh - 1, W, H - bh, Gfx.argb(Math.round(210 * barSlide), mixc(rc, WHITE, 0.3f))); }
        }

        // ---- the item (flat icons stay flat, skulls are 3D and turn)
        if (it != null && p >= 0f && !nod) {
            float grow = 0.25f + 0.75f * eob(p / 0.7f);
            float s = 3f * (H / 270f) * sz * isc * grow * (rng ? 1.2f : 1f);
            float gcx = CW / 2f * W / CW + sx, gcy = itemY(tb, p, icy + 22 * sz, icy, sz, c.itemBob, c.itemBobAmp) * u + sy;
            ItemIcons.drawAlpha = Math.max(0f, Math.min(1f, itemA)); boolean still0 = Icons.STILL_ICONS; Icons.STILL_ICONS = false;                 // the dropped item always turns, whatever the menu says
            try { ItemIcons.draw(g, it.id, it.icon, Math.round(gcx), Math.round(gcy), s); } finally { ItemIcons.drawAlpha = 1f; Icons.STILL_ICONS = still0; }
        }

        // ---- the words: below the animation (top and center), above it (bottom); never over the bars
        if (!TextFx.itemOnly && (textT > 0f || TextFx.textOnly)) {
            float sc = c.textK() * Math.max(0.55f, Math.min(W, H) / 540f) * (0.8f + 0.2f * csz);
            float th = TextFx.blockHeight(it, sc, c);
            float top = cfg.pos == 2 ? (icy - hi - 14 * sz) * u - th : (icy + hi + 26 * sz) * u;
            if (barsOn) { float bhg = barH * u; top = Math.max(bhg + 6, Math.min(H - bhg - th - 6, top)); }
            float tl = TextFx.textOnly ? TextFx.loopClock(4.6f) : 0f;                                    // the Text editor replays the words on their own clock
            TextFx.revealT = TextFx.textOnly ? tl : textT; TextFx.waveT = TextFx.textOnly ? tl : t; TextFx.waveOut = TextFx.textOnly ? sm((tl - 3.9f) / 0.6f) : textOut; TextFx.lineDelay = 0f;
            TextFx.block(g, font, W / 2, Math.round(top), sc, it, c, 255);
        }
    }
}
