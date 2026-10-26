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
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.max(1e-3, Math.sqrt(dx * dx + dy * dy));
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
        float top = Math.max(0f, gy - (40f + 50f * e) * sz);                                   // the pillar is short: it fades out well below the top of the screen
        for (int y = (int) top; y < gy; y += 1) {
            float f = 1f - (y - top) / Math.max(1f, gy - top);
            for (int x = (int) -w; x <= w; x++) cv.set(Math.round(cx + x), y, A(a * (0.10f + 0.30f * e) * (1f - Math.abs(x) / (w + 1f)) * (0.5f + 0.5f * (1f - f)) * (1f - f * f), mixc(rgb, WHITE, 0.4f)));
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
        float[] eyes = shape(kind, cx, gy, k);
        cv = saved;
        int[] sp = sil.px; float cy0 = gy - 45 * k, top = SHAPE_TOP[kind] * k;
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
                float dx = x - cx, dy = y - cy0, len = (float) Math.max(1, Math.sqrt(dx * dx + dy * dy));
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
            for (int q = 0; q + 1 < eyes.length; q += 2) { float ex = eyes[q], ey = eyes[q + 1], sd = q == 0 ? -1 : 1; glowE(ex, ey, 6 * k, 5 * k, 0xFF4A2A, 0.7f * e); dot(ex, ey, 0xFFE0B0, e); dot(ex + sd, ey, 0xFF8A4A, e); }
        }
    }

    // ------------------------------------------------------------------ the creatures: one silhouette for each of the twelve
    /** Which creature the scene summons: the Mythological Mobs of the Animations page rotate through all twelve; a real drop calls its own. */
    static final String[] KINDS = {"minotaur", "minos_hunter", "siamese_lynxes", "stranded_nymph", "cretan_bull", "harpy", "gaia_construct", "minos_champion", "sphinx", "minos_inquisitor", "manticore", "king_minos"};
    private static final float[] SHAPE_TOP = {100, 90, 58, 76, 60, 90, 98, 100, 74, 104, 86, 108, 70, 52, 66, 82, 90, 84};
    private static String upKey = ""; private static long upMs;
    static int kind = 0;
    static int kindOf(String sourceId) { for (int i = 0; i < KINDS.length; i++) if (KINDS[i].equals(sourceId)) return i; return 0; }
    private static final int DK = 0x070A14;
    private static void fe(float cx, float gy, float k, float x, float y, float rx, float ry) { fillE(cx + x * k, gy + y * k, rx * k, ry * k, DK, 1f); }
    private static void ln2(float cx, float gy, float k, float x0, float y0, float x1, float y1, int w) { for (int i = -(w / 2); i <= w / 2; i++) { line(cx + x0 * k + i, gy + y0 * k, cx + x1 * k + i, gy + y1 * k, DK, 1f); line(cx + x0 * k, gy + y0 * k + i, cx + x1 * k, gy + y1 * k + i, DK, 1f); } }

    /** Draws the dark shape of creature `kd` on the silhouette canvas; returns where its eyes glare (x, y pairs). */
    private static float[] shape(int kd, float cx, float gy, float k) {
        switch (kd) {
            case 1 -> {                                                                       // Minos Hunter: a hooded archer with a bow and a quiver
                fe(cx, gy, k, -5, -9, 4, 10); fe(cx, gy, k, 5, -9, 4, 10); fe(cx, gy, k, 0, -30, 12, 22); fe(cx, gy, k, 0, -46, 15, 6);
                fe(cx, gy, k, 0, -58, 8, 9); fe(cx, gy, k, 0, -68, 4, 6); fe(cx, gy, k, -16, -40, 4, 12); fe(cx, gy, k, 15, -38, 4, 10);
                ln2(cx, gy, k, -22, -66, -30, -54, 2); ln2(cx, gy, k, -30, -54, -31, -42, 2); ln2(cx, gy, k, -31, -42, -30, -30, 2); ln2(cx, gy, k, -30, -30, -22, -18, 2); ln2(cx, gy, k, -22, -66, -22, -18, 1);
                ln2(cx, gy, k, 8, -62, 15, -48, 2); ln2(cx, gy, k, 9, -64, 8, -70, 1); ln2(cx, gy, k, 12, -60, 14, -67, 1);
                return new float[]{cx - 3 * k, gy - 58 * k, cx + 3 * k, gy - 58 * k};
            }
            case 2 -> {                                                                       // Siamese Lynxes: two cats, tails curled, ears up
                for (int s = -1; s <= 1; s += 2) {
                    float o = s * 19, f = -s;                                                  // the left cat looks right, the right cat looks left
                    fe(cx, gy, k, o, -14, 14, 11); fe(cx, gy, k, o - 8 * f, -12, 8, 10); fe(cx, gy, k, o + 9 * f, -22, 7, 12); fe(cx, gy, k, o + 11 * f, -36, 8, 7);
                    ln2(cx, gy, k, o + 6 * f, -41, o + 7 * f, -53, 3); ln2(cx, gy, k, o + 14 * f, -41, o + 17 * f, -53, 3); fe(cx, gy, k, o + 17 * f, -33, 4, 3);
                    ln2(cx, gy, k, o - 14 * f, -16, o - 22 * f, -26, 3); ln2(cx, gy, k, o - 22 * f, -26, o - 24 * f, -38, 3); ln2(cx, gy, k, o - 24 * f, -38, o - 19 * f, -47, 2);
                    fe(cx, gy, k, o + 8 * f, -4, 3, 6); fe(cx, gy, k, o - 6 * f, -4, 3, 6);
                }
                return new float[]{cx - 5 * k, gy - 37 * k, cx + 5 * k, gy - 37 * k};
            }
            case 3 -> {                                                                       // Stranded Nymph: long hair, a flowing dress, seaweed, a shell
                fe(cx, gy, k, 0, -14, 10, 16); fe(cx, gy, k, 0, -8, 16, 8); fe(cx, gy, k, 0, -38, 8, 14); fe(cx, gy, k, 0, -58, 6.5f, 7.5f);
                fe(cx, gy, k, -8, -50, 5, 20); fe(cx, gy, k, 8, -50, 5, 20); ln2(cx, gy, k, -9, -40, -15, -18, 3); ln2(cx, gy, k, 9, -40, 15, -18, 3);
                fe(cx, gy, k, -11, -38, 3, 10); fe(cx, gy, k, 11, -34, 3, 9); fe(cx, gy, k, 14, -27, 5, 4);
                ln2(cx, gy, k, -18, -2, -21, -16, 2); ln2(cx, gy, k, -21, -16, -18, -26, 1); ln2(cx, gy, k, 19, -2, 23, -14, 2); ln2(cx, gy, k, 23, -14, 20, -24, 1);
                return new float[]{cx - 2.5f * k, gy - 58 * k, cx + 2.5f * k, gy - 58 * k};
            }
            case 4 -> {                                                                       // Cretan Bull: a bull on four legs, head lowered, horns forward
                float x0 = -4;
                fe(cx, gy, k, x0, -30, 28, 15); fe(cx, gy, k, x0 - 8, -42, 12, 7); fe(cx, gy, k, x0 - 20, -28, 10, 14); fe(cx, gy, k, x0 + 18, -30, 12, 14);
                fe(cx, gy, k, x0 + 30, -24, 9, 9); fe(cx, gy, k, x0 + 38, -19, 5, 4);
                ln2(cx, gy, k, x0 + 28, -31, x0 + 26, -42, 3); ln2(cx, gy, k, x0 + 26, -42, x0 + 32, -49, 2); ln2(cx, gy, k, x0 + 33, -31, x0 + 36, -43, 3); ln2(cx, gy, k, x0 + 36, -43, x0 + 43, -47, 2);
                for (int l : new int[]{-22, -12, 14, 24}) fe(cx, gy, k, x0 + l, -8, 4, 12);
                ln2(cx, gy, k, x0 - 30, -34, x0 - 36, -22, 2); ln2(cx, gy, k, x0 - 36, -22, x0 - 34, -12, 2);
                return new float[]{cx + (x0 + 33) * k, gy - 26 * k, cx + (x0 + 37) * k, gy - 25 * k};
            }
            case 5 -> {                                                                       // Harpy: a woman with great wings and talons
                fe(cx, gy, k, 0, -40, 8, 16); fe(cx, gy, k, 0, -62, 6, 7); fe(cx, gy, k, 0, -67, 7, 4);
                ln2(cx, gy, k, -3, -70, -6, -80, 2); ln2(cx, gy, k, 0, -71, 0, -82, 2); ln2(cx, gy, k, 3, -70, 6, -80, 2);
                for (int sd = -1; sd <= 1; sd += 2) for (int i = 0; i <= 12; i++) ln2(cx, gy, k, sd * 6, -52, sd * (24 + i * 2.6f), -82 + i * 5.2f, 2);
                for (int sd = -1; sd <= 1; sd += 2) fe(cx, gy, k, sd * 20, -62, 13, 11);
                fe(cx, gy, k, -4, -18, 3, 10); fe(cx, gy, k, 4, -18, 3, 10); ln2(cx, gy, k, -4, -7, -9, 0, 2); ln2(cx, gy, k, 4, -7, 9, 0, 2);
                return new float[]{cx - 2.5f * k, gy - 62 * k, cx + 2.5f * k, gy - 62 * k};
            }
            case 6 -> {                                                                       // Gaia Construct: a hulking golem of stone and moss
                fe(cx, gy, k, -10, -12, 8, 14); fe(cx, gy, k, 10, -12, 8, 14); fe(cx, gy, k, 0, -42, 22, 28); fe(cx, gy, k, 0, -66, 30, 10);
                fe(cx, gy, k, -36, -50, 8, 24); fe(cx, gy, k, 36, -50, 8, 24); fe(cx, gy, k, -37, -22, 9, 9); fe(cx, gy, k, 37, -22, 9, 9);
                fe(cx, gy, k, 0, -82, 9, 8); ln2(cx, gy, k, -20, -74, -6, -76, 2); ln2(cx, gy, k, 14, -78, 22, -90, 2); ln2(cx, gy, k, 22, -90, 18, -96, 2);
                return new float[]{cx - 4 * k, gy - 82 * k, cx + 4 * k, gy - 82 * k};
            }
            case 7 -> {                                                                       // Minos Champion: an armoured guard with a horned helm, a spear and a round shield
                fe(cx, gy, k, -8, -10, 6, 12); fe(cx, gy, k, 8, -10, 6, 12); fe(cx, gy, k, 0, -34, 14, 22); fe(cx, gy, k, 0, -54, 22, 8); fe(cx, gy, k, 0, -66, 8, 9);
                fe(cx, gy, k, 0, -76, 3, 6); ln2(cx, gy, k, 0, -80, 5, -90, 3);
                ln2(cx, gy, k, -7, -70, -14, -80, 2); ln2(cx, gy, k, 7, -70, 14, -80, 2);
                fe(cx, gy, k, -27, -40, 10, 16); fe(cx, gy, k, 24, -38, 5, 14);
                ln2(cx, gy, k, 26, -12, 26, -96, 2); ln2(cx, gy, k, 26, -96, 22, -88, 2); ln2(cx, gy, k, 26, -96, 30, -88, 2);
                return new float[]{cx - 3.5f * k, gy - 67 * k, cx + 3.5f * k, gy - 67 * k};
            }
            case 8 -> {                                                                       // Sphinx: a lion lying down, a headdress, folded wings
                float x0 = -6;
                fe(cx, gy, k, x0 + 10, -18, 34, 14); fe(cx, gy, k, x0 - 26, -5, 14, 4); fe(cx, gy, k, x0 - 8, -4, 10, 4);
                fe(cx, gy, k, x0 - 20, -34, 8, 16); fe(cx, gy, k, x0 - 22, -56, 8, 9); fe(cx, gy, k, x0 - 22, -52, 12, 12);
                fe(cx, gy, k, x0 - 31, -40, 4, 12); fe(cx, gy, k, x0 - 13, -40, 4, 12);
                fe(cx, gy, k, x0 + 14, -34, 22, 12); ln2(cx, gy, k, x0 + 30, -40, x0 + 42, -54, 3);
                ln2(cx, gy, k, x0 + 42, -14, x0 + 48, -8, 2); ln2(cx, gy, k, x0 + 48, -8, x0 + 46, -2, 2);
                return new float[]{cx + (x0 - 25) * k, gy - 57 * k, cx + (x0 - 19) * k, gy - 57 * k};
            }
            case 9 -> {                                                                       // Minos Inquisitor: a tall robed figure in a horned mask, with a staff
                fe(cx, gy, k, 0, -30, 15, 30); fe(cx, gy, k, 0, -8, 19, 10); fe(cx, gy, k, 0, -52, 11, 12); fe(cx, gy, k, 0, -72, 7, 8);
                for (int sd = -1; sd <= 1; sd += 2) { ln2(cx, gy, k, sd * 6, -76, sd * 14, -84, 3); ln2(cx, gy, k, sd * 14, -84, sd * 16, -98, 3); ln2(cx, gy, k, sd * 16, -98, sd * 12, -104, 2); }
                fe(cx, gy, k, -15, -48, 4, 12); fe(cx, gy, k, 18, -50, 3, 10);
                ln2(cx, gy, k, 22, -4, 22, -92, 2); ln2(cx, gy, k, 17, -98, 22, -92, 2); ln2(cx, gy, k, 27, -98, 22, -92, 2); fe(cx, gy, k, 22, -99, 3, 3);
                return new float[]{cx - 3 * k, gy - 72 * k, cx + 3 * k, gy - 72 * k};
            }
            case 10 -> {                                                                      // Manticore: a lion with a human face, bat wings and a scorpion tail
                float x0 = -4;
                fe(cx, gy, k, x0, -26, 24, 13); fe(cx, gy, k, x0 - 20, -24, 10, 12); fe(cx, gy, k, x0 + 16, -28, 10, 14);
                for (int l : new int[]{-22, -10, 12, 22}) fe(cx, gy, k, x0 + l, -7, 4, 10);
                fe(cx, gy, k, x0 + 26, -42, 12, 12); fe(cx, gy, k, x0 + 28, -40, 7, 8);
                for (int i = 0; i <= 10; i++) ln2(cx, gy, k, x0 - 4, -38, x0 - 26 + i * 4.2f, -70 - (i % 2) * 8 + (i > 5 ? (i - 5) * 1.5f : 0), 2);
                fe(cx, gy, k, x0 - 8, -58, 16, 14);
                ln2(cx, gy, k, x0 - 24, -30, x0 - 36, -50, 3); ln2(cx, gy, k, x0 - 36, -50, x0 - 30, -68, 3); ln2(cx, gy, k, x0 - 30, -68, x0 - 16, -76, 3); ln2(cx, gy, k, x0 - 16, -76, x0 - 8, -70, 2); ln2(cx, gy, k, x0 - 8, -70, x0 - 6, -62, 2);
                return new float[]{cx + (x0 + 24) * k, gy - 41 * k, cx + (x0 + 30) * k, gy - 41 * k};
            }
            case 11 -> {                                                                      // King Minos: a crowned giant in a cape, a sceptre in his hand
                fe(cx, gy, k, 0, -44, 34, 44);
                shape(0, cx, gy, k * 1.05f);
                for (int i = -3; i <= 3; i++) ln2(cx, gy, k, i * 3, -78, i * 3.4f, -86 - (i % 2 == 0 ? 6 : 0), 2);
                fe(cx, gy, k, 0, -78, 10, 2);
                ln2(cx, gy, k, 38, -8, 38, -98, 3); fe(cx, gy, k, 38, -102, 5, 5);
                return new float[]{cx - 4.7f * k, gy - 72.5f * k, cx + 4.7f * k, gy - 72.5f * k};
            }
            case 12 -> {                                                                      // Revenant Horror: a hunched zombie, arms stretched out
                fe(cx, gy, k, -5, -9, 4, 10); fe(cx, gy, k, 5, -9, 4, 10); fe(cx, gy, k, 0, -32, 11, 17); fe(cx, gy, k, 1, -56, 8, 9);
                ln2(cx, gy, k, -9, -44, -28, -50, 3); ln2(cx, gy, k, 9, -44, 28, -50, 3); fe(cx, gy, k, -30, -50, 3.5f, 3.5f); fe(cx, gy, k, 30, -50, 3.5f, 3.5f);
                fe(cx, gy, k, -6, -22, 3, 6);
                return new float[]{cx - 3 * k, gy - 57 * k, cx + 4 * k, gy - 57 * k};
            }
            case 13 -> {                                                                      // Tarantula Broodfather: a huge spider, eight legs, a swollen back
                fe(cx, gy, k, 12, -26, 17, 13); fe(cx, gy, k, -10, -24, 10, 9); fe(cx, gy, k, -17, -24, 5, 6);
                for (int sd = 0; sd < 2; sd++) for (int i = 0; i < 4; i++) {
                    float bx = -12 + i * 6 + sd * 3, ex = -34 + i * 18 + sd * 5, ky = -44 - (i % 2) * 8 - sd * 4;
                    ln2(cx, gy, k, bx, -26, ex * 0.6f + bx * 0.4f, ky, 2); ln2(cx, gy, k, ex * 0.6f + bx * 0.4f, ky, ex, -1, 2);
                }
                return new float[]{cx - 19 * k, gy - 27 * k, cx - 14 * k, gy - 27 * k};
            }
            case 14 -> {                                                                      // Sven Packmaster: a wolf mid-snarl, ears up
                fe(cx, gy, k, -4, -26, 22, 11); fe(cx, gy, k, 17, -30, 9, 12);
                for (int i = 0; i < 4; i++) fe(cx, gy, k, -17 + i * 11, -8, 3, 9);
                fe(cx, gy, k, 23, -42, 5, 9); fe(cx, gy, k, 29, -50, 7, 6); fe(cx, gy, k, 37, -47, 7, 3);
                ln2(cx, gy, k, 25, -54, 24, -64, 2); ln2(cx, gy, k, 32, -54, 33, -64, 2);
                ln2(cx, gy, k, -25, -30, -38, -42, 3); ln2(cx, gy, k, -38, -42, -39, -32, 2);
                return new float[]{cx + 28 * k, gy - 51 * k, cx + 33 * k, gy - 51 * k};
            }
            case 15 -> {                                                                      // Voidgloom Seraph: a very tall dark figure, long arms
                ln2(cx, gy, k, -3, -2, -3, -46, 3); ln2(cx, gy, k, 3, -2, 3, -46, 3);
                fe(cx, gy, k, 0, -56, 6, 11); ln2(cx, gy, k, -7, -62, -10, -22, 2); ln2(cx, gy, k, 7, -62, 10, -22, 2);
                fe(cx, gy, k, 0, -76, 7, 6);
                return new float[]{cx - 3 * k, gy - 76 * k, cx + 3 * k, gy - 76 * k};
            }
            case 16 -> {                                                                      // Inferno Demonlord: a blaze, a head ringed by rods over a smoky body
                fe(cx, gy, k, 0, -66, 9, 9); fe(cx, gy, k, 0, -44, 6, 9); fe(cx, gy, k, 0, -30, 4, 7);
                for (int i = 0; i < 4; i++) { float x = -26 + i * 17; ln2(cx, gy, k, x, -42 - (i % 2) * 6, x, -84 + (i % 2) * 6, 2); }
                ln2(cx, gy, k, -6, -78, -8, -88, 2); ln2(cx, gy, k, 6, -78, 8, -88, 2);
                return new float[]{cx - 4 * k, gy - 66 * k, cx + 4 * k, gy - 66 * k};
            }
            case 17 -> {                                                                      // Riftstalker Bloodfiend: a caped vampire, wings of cloth, a high collar
                fe(cx, gy, k, 0, -28, 15, 27); fe(cx, gy, k, 0, -58, 13, 6); fe(cx, gy, k, 0, -68, 7, 8);
                ln2(cx, gy, k, -14, -54, -32, -22, 3); ln2(cx, gy, k, 14, -54, 32, -22, 3); fe(cx, gy, k, -26, -26, 7, 11); fe(cx, gy, k, 26, -26, 7, 11);
                ln2(cx, gy, k, -5, -74, -8, -82, 2); ln2(cx, gy, k, 5, -74, 8, -82, 2);
                return new float[]{cx - 3 * k, gy - 68 * k, cx + 3 * k, gy - 68 * k};
            }
            default -> {                                                                      // Minotaur: a horned brute
                fe(cx, gy, k, -8, -10, 7, 12); fe(cx, gy, k, 8, -10, 7, 12); fe(cx, gy, k, 0, -34, 17, 25); fe(cx, gy, k, 0, -54, 27, 11);
                fe(cx, gy, k, -28, -40, 6, 18); fe(cx, gy, k, 28, -40, 6, 18); fe(cx, gy, k, -29, -21, 6, 6); fe(cx, gy, k, 29, -21, 6, 6);
                fe(cx, gy, k, 0, -68, 10, 11); fe(cx, gy, k, 0, -62, 6, 5);
                for (int sd = -1; sd <= 1; sd += 2) { float[] hx = {8, 16, 21, 23, 20}, hy = {-74, -75, -80, -88, -95}; for (int i = 0; i < 4; i++) ln2(cx, gy, k, sd * hx[i], hy[i], sd * hx[i + 1], hy[i + 1], 3); }
                return new float[]{cx - 4.5f * k, gy - 69 * k, cx + 4.5f * k, gy - 69 * k};
            }
        }
    }

    // ------------------------------------------------------------------ treasure: the dig
    private static final float[] DIG = {0.14f, 0.43f, 0.72f};

    /** An outlined ellipse: a dark rim first, then the colour. */
    private static void pe(float x, float y, float rx, float ry, int rgb, float a) { fillE(x, y, rx + 0.9f, ry + 0.9f, 0x0A0A12, a); fillE(x, y, rx, ry, rgb, a); }
    private static float key(float u, float[] ts, float[] vs) {
        for (int i = 1; i < ts.length; i++) if (u <= ts[i]) return lerp(vs[i - 1], vs[i], sm((u - ts[i - 1]) / (ts[i] - ts[i - 1])));
        return vs[vs.length - 1];
    }

    /** A digger in a straw hat and a proper spade: wind up, drive it in, prise, lift a lump of earth and fling it onto the pile. */
    private static void digger(float t, float O, float cx, float gy, float sz, int blade, float a, float done) {
        float k = sz, cyc = 0.29f * O;
        int kk = -1; for (int i = 0; i < 3; i++) if (t >= DIG[i] * O - 0.1f * O) kk = i;
        float u = kk < 0 ? 0f : (t - DIG[kk] * O + 0.1f * O) / cyc;
        boolean after = kk == 2 && u > 1f;
        float[] ts = {0f, 0.16f, 0.30f, 0.46f, 0.74f, 1f};
        float tx = key(u, ts, new float[]{22, -6, -3, -3, 3, 22}) * k, ty = key(u, ts, new float[]{-30, -38, 1, 1, -24, -30}) * k;
        float ang = key(u, ts, new float[]{0.55f, -0.45f, -0.38f, -0.38f, -0.05f, 0.7f});
        if (kk < 0) { tx = -6 * k; ty = -38 * k; ang = -0.45f; }
        if (after) { float q = sm((u - 1f) / 0.5f); tx = lerp(22, -14, q) * k; ty = lerp(-30, 0, q) * k; ang = lerp(0.7f, -0.2f, q); }
        float tipX = cx + tx, tipY = gy + ty, L = 40 * k, sa = (float) Math.sin(ang), ca = (float) Math.cos(ang);
        float gx = tipX + sa * L, gyy = tipY - ca * L;                                        // the grip
        float fx = cx - 36 * k + Math.max(-4 * k, Math.min(12 * k, (gx - (cx - 16 * k)) * 0.3f));    // the digger follows the spade a little
        float lean = (gx - fx) * 0.08f;
        int skin = 0xF2C9A0, shirt = 0x3F6FB5, pants = 0x4A4256, boot = 0x2A1A10;
        // legs and boots
        for (int sd = -1; sd <= 1; sd += 2) { line(fx + sd * 3 * k, gy - 14 * k, fx + sd * 4 * k, gy - 1, 0x0A0A12, a); pe(fx + sd * 3 * k, gy - 8 * k, 2.4f * k, 7 * k, pants, a); pe(fx + sd * 4 * k + sd, gy - 1, 3.2f * k, 1.8f * k, boot, a); }
        // torso, arms, head, hat
        pe(fx + lean * 0.5f, gy - 22 * k, 6.5f * k, 9 * k, shirt, a);
        fillE(fx + lean * 0.5f - 2 * k, gy - 25 * k, 2.4f * k, 4 * k, 0x5A8FD5, a * 0.9f);
        float sx0 = fx + lean, sy0 = gy - 27 * k;
        for (int sd = 0; sd < 2; sd++) { float hx = lerp(sx0, gx, 0.9f) + sd * 2, hy = lerp(sy0, gyy, 0.9f) + sd * 4 * k; line(sx0, sy0, hx, hy, 0x0A0A12, a); line(sx0 + 1, sy0, hx + 1, hy, shirt, a); pe(hx, hy, 1.8f * k, 1.8f * k, skin, a); }
        pe(fx + lean, gy - 35 * k, 4.2f * k, 4.5f * k, skin, a);
        dot(fx + lean + 1.5f * k, gy - 35.5f * k, 0x2A1A10, a);
        pe(fx + lean, gy - 38.5f * k, 8f * k, 1.7f * k, 0xC8A040, a); pe(fx + lean, gy - 41 * k, 4.4f * k, 3f * k, 0xE0BC58, a); fillE(fx + lean, gy - 39.4f * k, 4.5f * k, 0.8f * k, 0x8A5A20, a);
        // the spade: shaft, grip, blade
        float ux = sa, uy = -ca, px2 = ca, py2 = sa;                                         // along the shaft (up) and across it
        float bl = 12 * k;
        for (float r = bl; r < L; r += 0.5f) { float sxp = tipX + ux * r, syp = tipY + uy * r; dot(sxp, syp, 0x8A5A2C, a); dot(sxp + 1, syp, 0x5B3A1C, a); }
        for (float w = -3.2f * k; w <= 3.2f * k; w += 0.5f) dot(gx + px2 * w, gyy + py2 * w, 0x3A2410, a);
        for (float w = -3.2f * k; w <= 3.2f * k; w += 0.5f) { dot(gx + px2 * w - ux * 1.5f, gyy + py2 * w - uy * 1.5f, 0x6B4423, a); }
        int bc = mixc(blade, 0xC9D1E0, 0.6f);
        for (float r = 0; r <= bl; r += 0.5f) {
            float hw = (r < 3 * k ? 0.7f + r * 0.85f : r < 9 * k ? 3.4f * k : 3.4f * k - (r - 9 * k) * 0.9f);
            for (float w = -hw - 0.9f; w <= hw + 0.9f; w += 0.5f) {
                boolean rim = Math.abs(w) > hw - 0.4f || r < 0.8f;
                int col = rim ? 0x2A3040 : w < -hw * 0.35f ? mixc(bc, WHITE, 0.55f) : w > hw * 0.45f ? mixc(bc, 0x000000, 0.28f) : bc;
                if (Math.abs(w) > hw) col = 0x0A0A12;
                dot(tipX + ux * r + px2 * w, tipY + uy * r + py2 * w, col, a);
            }
        }
        // the lump of earth carried on the blade, then flung to the pile
        float pileX = cx + 40 * k;
        if (u >= 0.46f && u < 0.74f) { float q = (u - 0.46f) / 0.28f; float lx = tipX + ux * 7 * k, ly = tipY + uy * 7 * k - 1.5f * k; fillE(lx, ly, 4.5f * k * (0.6f + 0.4f * q), 2.4f * k, 0x6B4A2B, a); fillE(lx - 1, ly - 1, 2.5f * k, 1.2f * k, 0x9A7A52, a); }
        if (u >= 0.74f && u < 1f && !after) {
            float q = (u - 0.74f) / 0.26f;
            for (int j = 0; j < 12; j++) {
                float qq = cl(q * (0.85f + 0.3f * rnd(j + kk * 40)));
                float x = lerp(tipX + ux * 7 * k, pileX + (rnd(j) - 0.5f) * 14 * k, qq), y = lerp(tipY, gy, qq * qq) - (float) Math.sin(qq * 3.1416f) * (24 + 14 * rnd(j + 9)) * k;
                int c = j % 3 == 0 ? 0x9A7A52 : j % 3 == 1 ? 0x6B4A2B : 0x3A2616; dot(x, y, c, a); dot(x + 1, y, c, a); if (j % 2 == 0) dot(x, y + 1, c, a);
            }
        }
        // the pile grows with every spadeful
        fillE(pileX, gy + 3 * k, (4 + 13 * done) * k, (1.6f + 5.5f * done) * k, 0x0A0A12, a * 0.6f);
        fillE(pileX, gy + 2 * k, (3.4f + 12 * done) * k, (1.2f + 5f * done) * k, 0x5A3E22, a);
        fillE(pileX - 2 * k, gy + 1 * k - 2 * done * k, (2f + 7 * done) * k, (0.8f + 3f * done) * k, 0x8A6A44, a * 0.9f);
        if (u >= 0.3f && u < 0.46f) for (int j = 0; j < 4; j++) dot(cx - 3 * k + (rnd(j) - 0.5f) * 10, gy - rnd(j + 4) * 4, 0x9A7A52, a * 0.8f);        // dust puff when it bites
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
        if (done > 0.9f) { float cu = sm((t - (DIG[2] + 0.13f) * O) / (0.12f * O)); if (cu > 0f) chest(cx, gy - 4 * sz * cu, sz * 0.62f, 0f, a * cu); }
        digger(t, O, cx, gy, sz, blade, a, done);
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
        int energy = tre ? GOLD : slayerMode ? SL_ENERGY[Math.floorMod(kind - 12, 6)] : CYAN;
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
            float kk = Math.min(sz, (gy - 4) / 98f), kkS = kk, rev = sm((t - 0.30f * O) / (0.55f * O));
            minotaur(icx, gy - 2, kk, rev, p < 0 ? -1f : p, nod, rc, nod ? GREY : mixc(energy, rc, rng || p > 0 ? 0.5f : 0f), nod && p > 0 ? 1f : bg > 0.2f ? 1f : 0f, t);
            int strikes = 0;
            float[] ts = {0.34f * O, 0.58f * O, 0.80f * O};
            for (int k = 0; k < 3; k++) {
                float tau = op - ts[k];
                if (tau < 0) continue;
                strikes++;
                if (slayerMode) cutOpen(tau, k, icx, gy - 2, kkS);
                if (slayerMode) slash(tau, k, icx, gy - 2, kkS, energy, bg, nod ? 1f - cl(p / 0.9f) : 1f - cl(p / 0.5f));
                if (tau < 0.30f) {
                    int seed = k * 101 + (int) (op * 28);
                    float fl = (rnd(seed) > 0.3f ? 1f : 0.45f) * (1f - tau / 0.30f), x0 = icx + (rnd(k * 9) - 0.5f) * 36;
                    if (!slayerMode) { bolt(x0, -4, icx + (rnd(k * 5) - 0.5f) * 5, gy, seed, energy, fl, 5, true);
                    if (k == 2) bolt(x0 - 22, -4, icx - 6, gy, seed + 400, energy, fl * 0.7f, 4, false); }
                    glowE(icx, gy, 46 * sz * (1f - tau / 0.3f * 0.5f), 20 * sz, WHITE, 0.85f * fl);
                }
                if (tau < 0.7f) for (int j = 0; j < 12; j++) {                              // sparks thrown off the impact
                    float vx = (rnd(k * 40 + j) - 0.5f) * 150, vy = -(30 + rnd(k * 70 + j) * 90);
                    dot(icx + vx * tau, gy + vy * tau + 220 * tau * tau, j % 3 == 0 ? WHITE : energy, (1f - tau / 0.7f) * 0.95f);
                }
                float sw = tau / 0.75f;
                if (sw < 1f) ringE(icx, gy, rx * (0.4f + 1.4f * eo(sw)), ry * (0.4f + 1.4f * eo(sw)), mixc(energy, WHITE, 0.5f), (1f - sw) * 0.9f * bg, 0, 6.2832f, 0, 0);
            }
            if (!slayerMode) cracks(op, icx, gy, rx, ry, strikes * 3, strikes > 0 ? cl(1f - (op - ts[strikes - 1]) / 0.8f) : 0f, bg * (nod ? 1f - cl(p / 0.9f) : 1f));
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
            if (p < 0.4f && !slayerMode) { int seed = (int) (p * 20) + 900; float fl = (rnd(seed) > 0.4f ? 0.8f : 0.25f) * (1f - p / 0.4f); bolt(icx + 8, 18 * sz, icx, gy, seed, GREY, fl, 4, false); }
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
            if (tre) { float cu = sm(p / 0.6f); chest(icx, gy - 13 * sz * cu - 2, sz * 1.0f, sm((p - 0.15f) / 0.35f), bg); }
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


    // ------------------------------------------------------------------ the slayer bosses: the same ritual as Mythological, a sword slashes the boss instead of lightning
    static boolean slayerMode;                                                          // set by the caller: paint() draws a slayer scene
    /** The colour of each boss: zombie, spider, wolf, enderman, blaze, vampire. */
    private static final int[] SL_ENERGY = {0x7CD060, 0xE03030, 0xC8D4E8, 0xD060FF, 0xFFB030, 0xD01030};
    static boolean isSlayer(Anim a) { return a.group.equals("slayer"); }
    static int slTheme(Anim a) { return Math.floorMod(a.kind >= 0 ? a.kind : a.item != null && a.item.source != null ? SlayerData.theme(a.item.source.id) : 0, 6); }

    /** One sword slash across the boss: the blade sweeps along a line, a bright trail stays and fades. k = 0, 1 (the two diagonals) or 2 (a double cut). */
    static int slSeed;                                                                  // set by the caller: makes the cuts of this drop differ from the next
    /** The line of cut number k through the body (silhouette units): its angle, centre and length differ for every drop. */
    private static float[] slashSeg(int k) {
        int sd = slSeed * 31 + k * 7;
        float base = k == 1 ? 2.2f : k == 2 ? 1.57f : 0.95f;                              // down-right, down-left, then straight down the middle
        float ang = base + (rnd(sd + 1) - 0.5f) * 0.9f, len = 54 + rnd(sd + 2) * 22;
        float mx = (rnd(sd + 3) - 0.5f) * 24, my = -44 + (rnd(sd + 4) - 0.5f) * 20;
        float ux = (float) Math.cos(ang), uy = (float) Math.sin(ang);
        return new float[]{mx - ux * len, my - uy * len, mx + ux * len, my + uy * len};
    }
    /** Opens the hologram along cut k: the pixels along the line are taken out, the gap widening a little after the blade passes. */
    private static void cutOpen(float tau, int k, float cx, float gy, float kk) {
        float su = cl(tau / 0.09f); if (su < 1f && tau < 0.05f) return;
        float[] sg = slashSeg(k); float w = 0.5f + 2.4f * sm(tau / 0.5f);
        float x0 = cx + sg[0] * kk, y0 = gy + sg[1] * kk, x1 = cx + sg[2] * kk, y1 = gy + sg[3] * kk, dx = x1 - x0, dy = y1 - y0, l2 = dx * dx + dy * dy;
        int bx0 = Math.max(0, (int) (cx - 42 * kk)), bx1 = Math.min(CW - 1, (int) (cx + 42 * kk)), by0 = Math.max(0, (int) (gy - 90 * kk)), by1 = Math.min(CH - 1, (int) (gy - 3 * kk));   // only the body, not the ground
        float reach = su;                                                                  // the cut opens from where the blade started towards where it is now
        for (int y = by0; y <= by1; y++) for (int x = bx0; x <= bx1; x++) {
            float u = ((x - x0) * dx + (y - y0) * dy) / l2; if (u < -0.02f || u > reach + 0.02f) continue;
            float px = x0 + u * dx - x, py = y0 + u * dy - y;
            if (px * px + py * py < w * w) { int i = y * CW + x; cv.px[i] = 0; }
        }
    }
    private static void slash(float tau, int k, float cx, float gy, float kk, int rgb, float a, float keep) {
        if (tau < 0f) return;
        float[][] segs = {slashSeg(k)};
        float fade = tau < 0.09f ? 1f : Math.max(1f - cl((tau - 0.09f) / 0.4f), 0.55f * keep);   // the glow fades, the cut stays
        if (fade <= 0.01f) return;
        for (int si = 0; si < segs.length; si++) {
            float[] sg = segs[si];
            if (tau < si * 0.08f) continue;
            float su = cl((tau - si * 0.08f) / 0.09f);
            float x0 = cx + sg[0] * kk, y0 = gy + sg[1] * kk, x1 = cx + sg[2] * kk, y1 = gy + sg[3] * kk;
            float hx = lerp(x0, x1, su), hy = lerp(y0, y1, su);
            for (int w = -1; w <= 1; w++) line(x0 + w * 0.7f, y0, hx + w * 0.7f, hy, w == 0 ? WHITE : rgb, (w == 0 ? 1f : 0.55f) * fade * a);
            if (tau < 0.5f) glowE((x0 + hx) / 2, (y0 + hy) / 2, Math.abs(hx - x0) * 0.5f + 4, Math.abs(hy - y0) * 0.5f + 4, rgb, 0.35f * fade * a);
            if (su < 1f) {                                                                // the sword itself, leading the cut
                float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy), ux = dx / len, uy = dy / len;
                float bx = hx - ux * 20 * kk, by = hy - uy * 20 * kk;
                line(bx, by, hx, hy, 0xE8EEF8, a); line(bx + 1, by, hx + 1, hy, 0x9AA8C0, a * 0.8f);
                line(hx - ux * 20 * kk - uy * 4 * kk, hy - uy * 20 * kk + ux * 4 * kk, hx - ux * 20 * kk + uy * 4 * kk, hy - uy * 20 * kk - ux * 4 * kk, 0xE8C040, a);
                line(bx, by, bx - ux * 7 * kk, by - uy * 7 * kk, 0x6A4A2A, a);
                glint(hx, hy, 3, WHITE, a);
            }
        }
    }


    /** Where the animation sits when it is at the Top Center: its highest point (the boss, or the item) just touches the top of the screen, or the bottom of the top bar. */
    private static float topIcy(float sz, float hi, float topM) {
        float icy = topM + hi + 1;
        for (int n = 0; n < 160; n++, icy += 1f) {
            float gy = icy + 22 * sz, kk = Math.min(sz, (gy - 4) / 98f);
            if (gy - 2 - 86 * kk >= topM && icy - hi >= topM) break;
        }
        return icy;
    }
    /** Where the item is (canvas units, y): it springs out of the ground, overshoots, then bobs gently. */
    /** A flat round disc, one fill per row (the soft glow is a few of these stacked). */
    private static void discFill(GuiGraphicsExtractor g, int cx, int cy, int r, int col) {
        for (int y = -r; y <= r; y++) { int hw = (int) Math.round(Math.sqrt(Math.max(0, (double) r * r - (double) y * y))); g.fill(cx - hw, cy + y, cx + hw, cy + y + 1, col); }
    }
    private static float itemY(float t, float p, float gy, float icy, float sz, boolean bob, float amp) {
        float rise = eob(p / 0.85f);
        return lerp(gy - 4, icy, rise) + (bob ? (float) Math.sin(t * 2.2f) * 1.8f * sz * amp * sm((p - 0.8f) / 0.6f) : 0f);
    }

    /** Seconds from the pivot (the moment the drop appears) to the end. */
    static final float TAIL = 3.2f;
    /** The scene is drawn 25% smaller than before: that is the new 100% of the Size slider. */
    static final float SMALLER = 0.6f;

    /** The scene canvas alone (320x180 ARGB) at time t: what AnimFrames writes out as pictures. */
    static int[] snapshot(Anim a, float t) {
        FancyConfig c = FancyConfig.get(); FancyConfig.AnimCfg cfg = a.cfg(); Item it = a.item;
        boolean tre = a.group.equals("treasure"); kind = Math.max(0, a.kind); int type = a.type;
        boolean rng = type == AnimationManager.RNG, nod = type == AnimationManager.NO_DROPS;
        float S = Math.max(3.6f, cfg.secs), csz = Math.max(0.5f, Math.min(1.5f, c.animScale)), sz = SMALLER * csz, isc = Math.max(0.4f, c.itemScale) * 1.5f;
        int rc = it != null ? (it.color() & 0xFFFFFF) : GREY;
        if (!tre && !rng && !nod) rc = mixc(rc, WHITE, 0.15f);
        float barH = CH * Math.max(0.052f, Math.min(0.156f, c.barPx / 96f)), hi = 16 * sz * isc * (rng ? 1.2f : 1f), icy = CH * POS[Math.max(0, Math.min(2, c.animPos))];
        if (c.animPos == 0) icy = topIcy(sz, hi, rng && cfg.bars ? barH + 1 : 0f);
        if (rng && cfg.bars) icy = Math.max(c.animPos == 0 ? 0f : barH + hi + 4, Math.min(CH - barH - hi - 14 * sz, icy));
        slayerMode = isSlayer(a); if (slayerMode) { kind = 12 + slTheme(a); slSeed = System.identityHashCode(a) & 0xFFFF; }
        try { paint(t, tre, type, S, sz, isc, rc, icy, TreasureData.COLS[Math.max(0, Math.min(2, c.mythSpade))] & 0xFFFFFF, c.itemBob, c.itemBobAmp); } finally { slayerMode = false; }
        return cv.px.clone();
    }

    /** Draws one animation at time t (seconds) in a W x H screen: the scene, the bars, the item and the words. */
    static void draw(GuiGraphicsExtractor g, Font font, Anim a, float t, int W, int H) {
        FancyConfig c = FancyConfig.get();
        FancyConfig.AnimCfg cfg = a.cfg();
        Item it = a.item;
        boolean tre = a.group.equals("treasure"), sl = isSlayer(a);
        kind = a.kind >= 0 ? a.kind : a.item != null && a.item.source != null ? kindOf(a.item.source.id) : 0;
        if (sl) kind = 12 + slTheme(a);
        int type = a.type;
        boolean rng = type == AnimationManager.RNG, nod = type == AnimationManager.NO_DROPS;
        float S = Math.max(3.6f, cfg.secs), O = S - TAIL, p = t - O;
        float csz = Math.max(0.5f, Math.min(1.5f, c.animScale)), sz = SMALLER * csz, isc = Math.max(0.4f, c.itemScale) * 1.5f;
        int rc = it != null ? (it.color() & 0xFFFFFF) : GREY;
        if (!tre && !rng && !nod) rc = mixc(rc, WHITE, 0.15f);
        boolean barsOn = (rng && cfg.bars) || TextFx.barsOnly, blurOn = rng && cfg.blur && !TextFx.barsOnly && !TextFx.itemOnly;

        // where everything is (canvas units)
        float barH = CH * Math.max(0.052f, Math.min(0.156f, c.barPx / 96f));
        float hi = 16 * sz * isc * (rng ? 1.2f : 1f);
        float icy = CH * POS[Math.max(0, Math.min(2, c.animPos))];
        if (c.animPos == 0) icy = topIcy(sz, hi, barsOn ? barH + 1 : 0f);
        if (barsOn) icy = Math.max(c.animPos == 0 ? 0f : barH + hi + 4, Math.min(CH - barH - hi - 14 * sz, icy));
        float u = (float) H / CH;                                                          // gui units per canvas unit
        float itemA = TextFx.itemOnly ? 1f : sm(p / 0.22f) * (1f - sm((t - (S - 1.0f)) / 0.9f));
        float tb = TextFx.itemOnly ? (System.currentTimeMillis() % 100000L) / 1000f : t;                // the item keeps bobbing while the Item editor holds the scene
        float textT = t - (O + 0.45f), textOut = sm((t - (S - 0.6f)) / 0.5f);               // the words leave after the item
        float barSlide = TextFx.barsOnly ? 1f : Math.min(sm((t - (O - 0.8f)) / 0.6f), 1f - sm((t - (S - 0.5f)) / 0.45f));
        float shake = rng && p > 0 && p < 0.5f ? (1f - p / 0.5f) * 3f : 0f;
        int sx = Math.round((rnd((int) (t * 60)) - 0.5f) * 2f * shake * u), sy = Math.round((rnd((int) (t * 60) + 77) - 0.5f) * 2f * shake * u);

        boolean sceneOff = TextFx.textOnly || TextFx.itemOnly || TextFx.barsOnly;            // the Changes previews show only the part being edited
        if (!sceneOff) {
            // ---- the blur: the world dims and softens at the edges (RNG, switchable)
            if (blurOn && barSlide > 0.01f) {
                g.fill(0, 0, W, H, Gfx.argb(Math.round(110 * barSlide), 0x04050E));
                for (int i = 0; i < 7; i++) { int in = Math.round(W * 0.05f * i), inY = Math.round(H * 0.05f * i); int al = Math.round(26 * barSlide); g.fill(0, 0, W, inY + 1, Gfx.argb(al, 0x000000)); g.fill(0, H - inY - 1, W, H, Gfx.argb(al, 0x000000)); g.fill(0, 0, in + 1, H, Gfx.argb(al, 0x000000)); g.fill(W - in - 1, 0, W, H, Gfx.argb(al, 0x000000)); }
            }
            int[] edited = AnimFrames.override(a, t);                                         // a frame you edited in config/fancydrops/art/animations replaces the drawn one
            if (edited != null && edited.length == cv.px.length) System.arraycopy(edited, 0, cv.px, 0, edited.length);
            else {
                long nowMs = System.currentTimeMillis();
                String id = a.group + a.type + "/" + a.kind + "/" + (it == null ? 0 : it.id.hashCode()) + "/" + TextFx.itemOnly;
                boolean reuse = AnimationManager.previewing && id.equals(upKey) && nowMs - upMs < 33 && nowMs >= upMs;       // a menu preview repaints at ~30 fps: the picture it shows barely changes between frames
                if (!reuse) {
                    slayerMode = sl; if (sl) slSeed = System.identityHashCode(a) & 0xFFFF;
                    try { paint(t, tre, type, S, sz, isc, rc, icy, TreasureData.COLS[Math.max(0, Math.min(2, c.mythSpade))] & 0xFFFFFF, c.itemBob, c.itemBobAmp); } finally { slayerMode = false; }
                    SceneTexture.upload(cv); upKey = id; upMs = nowMs;
                }
            }
            if (edited != null && edited.length == cv.px.length) SceneTexture.upload(cv);
            if (!SceneTexture.draw(g, cv, sx, sy, W, H, 1f)) { int cell = Math.max(1, Math.round((float) W / CW)); cv.flush((x0, y0, x1, y1, col) -> g.fill(x0 + sx, y0 + sy, x1 + sx, y1 + sy, col), 0, 0, cell, 1f); }
            if (rng && p > 0 && p < 0.45f) g.fill(0, 0, W, H, Gfx.argb(Math.round(235 * (1f - p / 0.45f) * (1f - p / 0.45f)), 0xFFFFFF));                  // the flash
            else if (!tre && !rng) for (int k = 0; k < 3; k++) { float tau = t - 0.34f * O - k * 0.24f * O; if (tau > 0 && tau < 0.18f) g.fill(0, 0, W, H, Gfx.argb(Math.round(26 * (1f - tau / 0.18f)), 0xD8F6FF)); }
        }

        // ---- the bars
        if (barsOn && barSlide > 0.01f && !TextFx.textOnly && !TextFx.itemOnly) {
            int bh = Math.round(barH * u * barSlide), oa = Math.round(255 * c.barOpacity / 100f);
            int bc = c.barCustom ? (c.barColor & 0xFFFFFF) : 0x05060C;          // never tinted by the drop colour
            g.fill(0, 0, W, bh, Gfx.argb(oa, bc)); g.fill(0, H - bh, W, H, Gfx.argb(oa, bc));
             if (c.barPillars) { int pw = bh;   g.fill(0, bh, pw, H - bh, Gfx.argb(oa, bc)); g.fill(W - pw, bh, W, H - bh, Gfx.argb(oa, bc)); if (c.barLine) { int lc = Gfx.argb(Math.round(210 * barSlide), mixc(rc, WHITE, 0.3f)); g.fill(pw, bh, pw + 1, H - bh, lc); g.fill(W - pw - 1, bh, W - pw, H - bh, lc); } }
            if (c.barCurve > 0 && bh > 2) {                                                              // curved corners: the four inner corners of the picture are rounded off in the bar colour
                int pwc = c.barPillars ? bh : 0, wx0 = pwc, wx1 = W - pwc, wy0 = bh, wy1 = H - bh;
                int rr = Math.round(c.barCurve / 100f * Math.min(wx1 - wx0, wy1 - wy0) / 5f);
                int col = Gfx.argb(oa, bc);
                for (int i = 0; i < rr; i++) {
                    double dv = rr - i - 0.5; int cut = (int) Math.round(rr - Math.sqrt(Math.max(0.0, (double) rr * rr - dv * dv)));
                    if (cut <= 0) continue;
                    g.fill(wx0, wy0 + i, wx0 + cut, wy0 + i + 1, col); g.fill(wx1 - cut, wy0 + i, wx1, wy0 + i + 1, col);
                    g.fill(wx0, wy1 - 1 - i, wx0 + cut, wy1 - i, col); g.fill(wx1 - cut, wy1 - 1 - i, wx1, wy1 - i, col);
                }
            }
            // (the pillars above are exactly as thick as the bars)
            if (c.barSoft) { int nr = Math.max(8, Math.round(16 * u)); for (int i = 0; i < nr; i++) { float f = 1f - (i + 0.5f) / nr; int al = Math.round(oa * f * f * 0.9f * barSlide); if (al <= 0) continue; g.fill(0, bh + i, W, bh + i + 1, Gfx.argb(al, bc)); g.fill(0, H - bh - i - 1, W, H - bh - i, Gfx.argb(al, bc)); } }          // a smooth fade along the inner edge, one thin row per step (no visible lines)
            if (c.barLine) { g.fill(0, bh, W, bh + 1, Gfx.argb(Math.round(210 * barSlide), mixc(rc, WHITE, 0.3f))); g.fill(0, H - bh - 1, W, H - bh, Gfx.argb(Math.round(210 * barSlide), mixc(rc, WHITE, 0.3f))); }
        }

        // ---- the item (flat icons stay flat, skulls are 3D and turn)
        if (it != null && p >= 0f && !nod && !TextFx.textOnly && !TextFx.barsOnly) {
            float grow = 0.25f + 0.75f * eob(p / 0.7f);
            float s = 3f * (H / 270f) * sz * isc * grow * (rng ? 1.2f : 1f) * (TextFx.itemOnly ? 1.9f : 1f);
            float gcx = CW / 2f * W / CW + sx, gcy = itemY(tb, p, icy + 22 * sz, icy, sz, c.itemBob, c.itemBobAmp) * u + sy;
            if (c.itemGlow > 0.02f) {                                                          // the glow behind the item (Item changes > Glow), in the drop's colour
                int gr = Math.round(s * 11f), gcol = mixc(rc, WHITE, 0.25f); float ga = Math.max(0f, Math.min(1f, itemA)) * Math.min(1f, c.itemGlow);
                for (int i = 0; i < 6; i++) { float k = i / 5f; int r = Math.round(gr * (1f - 0.55f * k)); int al = Math.round(Math.min(46f, 26f * ga * (0.6f + c.itemGlow)) * (0.5f + 0.5f * k) / 1.6f); discFill(g, Math.round(gcx), Math.round(gcy), r, Gfx.argb(al, gcol)); }
            }
            ItemIcons.spinSpeed = c.itemSpin; ItemIcons.spinRev = c.itemSpinRev;
            ItemIcons.outlineRgb = -1;
            ItemIcons.drawAlpha = Math.max(0f, Math.min(1f, itemA)); boolean still0 = Icons.STILL_ICONS; Icons.STILL_ICONS = false;                 // the dropped item always turns, whatever the menu says
            try { ItemIcons.draw(g, it.id, it.icon, Math.round(gcx), Math.round(gcy), s); } finally { ItemIcons.drawAlpha = 1f; Icons.STILL_ICONS = still0; ItemIcons.spinSpeed = 1f; ItemIcons.spinRev = false; ItemIcons.outlineRgb = -1; }
            if (c.itemGlints && itemA > 0.05f) for (int i = 0; i < 9; i++) {                      // Item changes > Glints: little four-point sparks twinkle in and out round the item
                float cyc = tb * 1.7f + i * 0.113f, ph = cyc - (float) Math.floor(cyc); int slot = (int) Math.floor(cyc) * 31 + i * 7;
                float an = rnd(slot) * 6.2832f, rad = s * (9f + 8f * rnd(slot + 1)), tw = (float) Math.sin(Math.PI * ph);
                int gx = Math.round(gcx + (float) Math.cos(an) * rad), gy2 = Math.round(gcy + (float) Math.sin(an) * rad * 0.85f), arm = Math.max(1, Math.round((1.5f + s * 0.6f * rnd(slot + 2)) * tw));
                int al = Math.round(255 * tw * Math.min(1f, itemA)), col = Gfx.argb(al, i % 2 == 0 ? 0xFFFFFF : mixc(rc, WHITE, 0.6f));
                g.fill(gx - arm, gy2, gx + arm + 1, gy2 + 1, col); g.fill(gx, gy2 - arm, gx + 1, gy2 + arm + 1, col);
                g.fill(gx - 1, gy2 - 1, gx + 2, gy2 + 2, Gfx.argb(al / 2, rc));
            }
        }

        // ---- the words: below the animation (top and center), above it (bottom); never over the bars
        if (!TextFx.itemOnly && !TextFx.barsOnly && (textT > 0f || TextFx.textOnly)) {
            float sc = c.textK() * Math.max(0.55f, Math.min(W, H) / 540f) * (0.8f + 0.2f * csz); if (TextFx.textOnly) sc = c.textK() * Math.max(1.3f, Math.min(2.6f, W / 150f));          // the Text editor shows the words big enough to read
            float th = TextFx.blockHeight(it, sc, c);
            float top = c.textAbove() ? (icy - hi - 14 * sz) * u - th : (icy + hi + 26 * sz) * u;
            if (TextFx.textOnly) { while (th > H * 0.92f && sc > 0.8f) { sc *= 0.9f; th = TextFx.blockHeight(it, sc, c); } top = Math.max(2f, (H - th) / 2f); }       // the Text editor centres the words and fills the box
            if (barsOn) { float bhg = barH * u; top = Math.max(bhg + 6, Math.min(H - bhg - th - 6, top)); }
            float tl = TextFx.textOnly ? TextFx.loopClock(4.6f) : 0f;                                    // the Text editor replays the words on their own clock
            TextFx.revealT = TextFx.textOnly ? tl : textT; TextFx.waveT = TextFx.textOnly ? tl : t; TextFx.waveOut = TextFx.textOnly ? sm((tl - 2.9f) / 1.1f) : textOut; TextFx.lineDelay = 0f;
            TextFx.block(g, font, W / 2, Math.round(top), sc, it, c, 255);
        }
    }
}
