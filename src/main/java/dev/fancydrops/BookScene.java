package dev.fancydrops;

/**
 * The Experimentation Table scene, painted on the Ritual canvas (Ritual.paint adds the finish on top).
 *
 * An open spell book lies in a dark violet hall, mist at its foot. Light pours up from the gutter and grows with every page: the book turns exactly three
 * pages (Normal: calm and bright; RNG: the same three, but each turn is a storm of lightning, rings of light and a rising book), then a column of light
 * bursts out of the book and the drop rises from it. Every edge is drawn with sub-pixel coverage and every move is eased, so it stays smooth at any frame rate.
 */
final class BookScene {
    private BookScene() {}

    private static final int VIO = 0xB56BFF, AQUA = 0x9AF0FF, GOLD = 0xFFC857;

    private static float cl(float v) { return Ritual.cl(v); }
    private static float sm(float v) { return Ritual.sm(v); }
    private static int mix(int a, int b, float f) { return Ritual.mixc(a, b, f); }
    private static float rnd(int n) { return Ritual.rnd(n); }
    /** Ease in and out (smootherstep: gentler at both ends than smoothstep). */
    private static float ease(float v) { v = cl(v); return v * v * v * (v * (v * 6 - 15) + 10); }

    /** The seconds at which each page lands (also where the Beat sound plays): three, for Normal and for RNG. */
    static float[] flips(boolean rng, float O) {
        return rng ? new float[]{0.30f * O, 0.54f * O, 0.78f * O} : new float[]{0.32f * O, 0.56f * O, 0.80f * O};
    }
    /** How long one page takes to turn. */
    static float flipLen(boolean rng) { return rng ? 0.62f : 0.72f; }
    /** Where the middle of the book rests (canvas y). The drop rises out of this spot. */
    static float baseY(float icy, float sz) { return icy + 14 * sz; }

    // ------------------------------------------------------------------ small drawing helpers (whole pixels, with partial coverage at the ends)
    /** One pixel column from y0 to y1 (floats): the end pixels are blended by how much of them the span covers. */
    private static void vspan(int x, float y0, float y1, int rgb, float a) {
        if (y1 <= y0 || a <= 0.01f) return;
        int i0 = (int) Math.ceil(y0), i1 = (int) Math.floor(y1);
        for (int y = i0; y < i1; y++) Ritual.cv.set(x, y, Ritual.A(a, rgb));
        float f0 = i0 - y0, f1 = y1 - i1;
        if (f0 > 0.04f && i0 - 1 >= 0) Ritual.cv.set(x, i0 - 1, Ritual.A(a * f0, rgb));
        if (f1 > 0.04f && i1 >= i0) Ritual.cv.set(x, i1, Ritual.A(a * f1, rgb));
    }
    private static void rect(int x0, int y0, int x1, int y1, int rgb, float a) {
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) Ritual.cv.set(x, y, Ritual.A(a, rgb));
    }
    // ------------------------------------------------------------------ the scene
    /** The scene at time t. op = the opening clock (stops at O), p = seconds since the drop appeared, a = how visible it is. by = the book's resting centre. */
    static void paint(float t, float O, float p, float icx, float by0, float sz, float a, boolean rng) {
        if (a <= 0.01f) return;
        float op = Math.min(t, O), fd = flipLen(rng);
        float[] fl = flips(rng, O); int K = fl.length;
        float appear = sm(t / (0.12f * O + 0.05f));
        float zIn = ease((op - 0.06f * O) / (0.30f * O)), zOut = p > 0f ? ease(p / 0.8f) : 0f;
        float z = 1f + 0.30f * zIn * (1f - zOut);
        float open = Ritual.eo((op - 0.03f * O) / (0.22f * O));
        float al = a * appear;
        float bk = sz * 2.7f * z, bx = icx;
        int landed = 0; float pulse = 0f;
        for (int k = 0; k < K; k++) { if (op >= fl[k]) landed = k + 1; float ag = op - fl[k]; if (ag >= 0f && ag < 0.4f) pulse = Math.max(pulse, 1f - ag / 0.4f); }
        pulse *= 1f - cl(p / 0.3f);
        boolean split = p >= 0f;
        float power = landed / (float) K;                                                                       // how much light the book has gathered
        float lev = rng ? (float) Math.sin(cl(op / (fl[K - 1] + 0.1f)) * 1.5708f) * 7f * sz * (1f - zOut) : 0f;     // an RNG book rises a little with every turn, and sinks back at the drop
        float by = by0 + (float) Math.sin(t * 2.1f) * 1.3f * sz * (1f - zIn) - lev;

        hall(t, bx, by0, sz, power, pulse, rng, al);
        Ritual.glowE(bx, by + 6 * bk, 52 * bk, 14 * bk, 0x07040F, 0.55f * al);                                  // the book's own shadow on the mist
        beams(t, bx, by, bk, power, pulse, rng, split, al);
        book(t, op, bx, by, bk, open, K, landed, fl, fd, pulse, rng, al);
        Ritual.glowE(bx, by, (50 + 16 * pulse + 16 * power) * sz * z, (30 + 12 * pulse) * sz * z, rng ? 0xE0A8FF : mix(VIO, 0xFFFFFF, 0.3f), ((0.20f + 0.26f * pulse + 0.2f * power) * open + (split ? 0.3f * cl(1f - p / 1.2f) : 0f)) * al);
        if (rng && !split) storm(t, op, bx, by, bk, fl, landed, pulse, al);
        if (split) burst(p, bx, by, bk, rng, al);
    }

    /** The hall: a see-through rounded panel, dark violet above and glowing pink-violet mist at the foot of the book, drifting lights and soft curtains. */
    private static void hall(float t, float bx, float by0, float sz, float power, float pulse, boolean rng, float a) {
        float fade = a * sm(t / 0.25f) * 0.78f; if (fade <= 0.01f) return;
        int x0 = Math.max(0, Math.round(bx - 118 * sz)), x1 = Math.min(Ritual.CW, Math.round(bx + 118 * sz)), y0 = Math.max(0, Math.round(by0 - 84 * sz)), y1 = Math.min(Ritual.CH, Math.round(by0 + 56 * sz));
        Canvas c = Ritual.cv;
        for (int y = y0; y < y1; y++) { float f = (y - y0) / (float) Math.max(1, y1 - y0); c.rect(x0, y, x1, y + 1, Ritual.A(fade, f < 0.65f ? mix(0x0C0430, 0x2A1064, f / 0.65f) : mix(0x2A1064, 0x6A32B0, (f - 0.65f) / 0.35f))); }
        for (int x = x0; x < x1; x++) {                                                                       // curtains: soft vertical folds, darker towards the sides
            float fold = 0.5f + 0.5f * (float) Math.sin(x * 0.21f + (float) Math.sin(x * 0.05f) * 2f), side = Math.abs(x - bx) / (float) (x1 - x0) * 2f;
            c.rect(x, y0, x + 1, y1, Ritual.A(fade * (0.10f + 0.20f * side * side) * (0.4f + 0.6f * fold), 0x000000));
        }
        SlayerScenery.corners(c, x0, y0, x1, y1, 7, rng ? 0xE0A8FF : 0xB56BFF, fade, false);
        float mist = 0.5f + 0.25f * power + 0.3f * pulse;
        for (int i = 0; i < 4; i++) { float dx = (float) Math.sin(t * 0.35f + i * 1.7f) * 14 * sz, w = (60 - i * 8) * sz; Ritual.glowE(bx + dx + (i - 1.5f) * 12 * sz, y1 - 6 * sz, w, (13 - i) * sz, i % 2 == 0 ? 0xE066C8 : 0x9A6AF0, 0.38f * mist * a); }
        for (int i = 0; i < 26; i++) {                                                                        // lights that float up out of the mist
            float u = (t * (0.06f + 0.05f * rnd(i)) + rnd(i * 3)) % 1f, tw = 0.5f + 0.5f * (float) Math.sin(t * 3f + i * 2.1f);
            Ritual.dot(x0 + 4 + rnd(i * 5) * (x1 - x0 - 8) + (float) Math.sin(t * 0.7f + i) * 3, y1 - 6 - u * (y1 - y0 - 10), i % 3 == 0 ? 0xFFFFFF : i % 3 == 1 ? AQUA : 0xE6B8FF, a * tw * (1f - u) * 0.9f);
        }
        SlayerScenery.corners(c, x0, y0, x1, y1, 7, 0, fade, true);
        for (int y = 0; y < c.h; y++) for (int x = 0; x < c.w; x++) if (y < y0 || y >= y1 || x < x0 || x >= x1) c.px[y * c.w + x] = 0;      // nothing leaves the frame
    }

    /** Light pouring up out of the gutter: one broad column, and a fan of slim beams that grows with every page that lands. */
    private static void beams(float t, float bx, float by, float bk, float power, float pulse, boolean rng, boolean split, float a) {
        if (split) return;                                                                                    // after the drop, burst() takes over
        float strength = (0.18f + 0.30f * power + 0.40f * pulse) * a, w0 = (2.4f + 3.6f * power) * bk;
        Ritual.glowE(bx, by - 6 * bk, 12 * bk, 26 * bk, 0xF4E8FF, (0.20f + 0.35f * power + 0.3f * pulse) * a);                           // light rising off the pages
        for (int y = 0; y < by; y++) {
            float f = 1f - (by - y) / Math.max(1f, by), w = w0 * (0.5f + 0.9f * f);
            for (int x = (int) -w; x <= w; x++) Ritual.cv.set(Math.round(bx) + x, y, Ritual.A(strength * (0.15f + 0.85f * f) * (1f - Math.abs(x) / (w + 1f)), rng ? 0xF0D8FF : 0xE0CCFF));
        }
        int n = 3 + 3 * Math.round(power * 2f) + (rng ? 3 : 0);
        for (int k = 0; k < n; k++) {
            float an = -1.5708f + (k - (n - 1) / 2f) * (rng ? 0.22f : 0.16f) + (float) Math.sin(t * 0.8f + k * 1.3f) * 0.05f, len = (40 + 70 * rnd(k + 9)) * bk * (0.4f + 0.6f * (0.3f + power));
            for (int i = 4; i < len; i++) { float ax = bx + (float) Math.cos(an) * i, ay = by - 2 + (float) Math.sin(an) * i, al2 = strength * 0.9f * (1f - i / len); Ritual.dot(ax, ay, k % 2 == 0 ? 0xFFFFFF : AQUA, al2); Ritual.dot(ax + 1, ay, 0xE0CCFF, al2 * 0.5f); }
        }
    }

    /** RNG only: lightning between the pages, rings of light turning round the book, and a ring flying out at every turn. */
    private static void storm(float t, float op, float bx, float by, float bk, float[] fl, int landed, float pulse, float a) {
        float k = 0.35f + 0.65f * landed / 3f;
        for (int r = 0; r < 2; r++) {                                                                         // two tilted rings of light, turning opposite ways
            float tilt = r == 0 ? -0.35f : 0.35f, rx = (30 + r * 8) * bk, ry = (9 + r * 2) * bk, spin = t * (r == 0 ? 1.1f : -0.8f);
            for (int i = 0; i < 40; i++) {
                float an = spin + i * 0.157f, x = (float) Math.cos(an) * rx, y = (float) Math.sin(an) * ry, depth = 0.5f + 0.5f * (float) Math.sin(an);
                float px = bx + x * (float) Math.cos(tilt) - y * (float) Math.sin(tilt), py = by + x * (float) Math.sin(tilt) + y * (float) Math.cos(tilt);
                Ritual.dot(px, py, mix(r == 0 ? 0xE0A8FF : AQUA, 0xFFFFFF, depth * 0.4f), a * k * (0.25f + 0.65f * depth) * (1f - i % 5 * 0.08f));
            }
        }
        for (int i = 0; i < 4; i++) {                                                                         // bolts from the gutter, never the same twice
            int seed = i * 19 + (int) (t * 16) * 7;
            if (rnd(seed + 2) > 0.18f + 0.5f * pulse) continue;
            float an = -3.14f * rnd(seed), len = (30 + 36 * rnd(seed + 1)) * bk;
            Ritual.bolt(bx + (float) Math.cos(an) * 4 * bk, by - 2, bx + (float) Math.cos(an) * len, by + (float) Math.sin(an) * len * 0.8f - 2, seed, i % 2 == 0 ? 0xE0A8FF : AQUA, 0.85f * a * k, 3, false);
        }
        for (int j = 0; j < fl.length; j++) {                                                                 // a ring of light flies out from the book each time a page lands
            float ag = op - fl[j]; if (ag < 0f || ag > 0.8f) continue;
            float f = ag / 0.8f; Ritual.ringE(bx, by, (14 + 90 * Ritual.eo(f)) * bk * 0.6f, (5 + 30 * Ritual.eo(f)) * bk * 0.6f, mix(0xE0A8FF, 0xFFFFFF, 0.5f), (1f - f) * 0.8f * a, 0, 6.2832f, 0, 0);
        }
    }

    // ------------------------------------------------------------------ the book
    private static void book(float t, float op, float bx, float by, float bk, float open, int K, int landed, float[] fl, float fd, float pulse, boolean rng, float a) {
        float hw = (3.2f + 18.8f * open) * bk, hh = 12 * bk;
        int bxi = Math.round(bx), L = Math.round(bx - hw), R = Math.round(bx + hw), T = Math.round(by - hh), B = Math.round(by + hh), dep = Math.max(2, Math.round(3.2f * bk));
        for (int y = T - 2; y < B + 3 + dep; y++) {                                                              // the leather cover, lit from above
            float f = (y - (T - 2)) / (float) (B + 3 + dep - (T - 2));
            int lc = mix(0x6B2E92, 0x2A1040, Math.min(1f, f * 1.15f));
            if (y >= B + 3) lc = mix(0x4A1E66, 0x1C0A2C, (y - B - 3) / (float) dep);
            rect(L - 2, y, R + 2, y + 1, lc, a);
        }
        rect(L - 2, T - 2, R + 2, T - 1, 0xB57AE0, a);
        rect(L - 2, B + 2, R + 2, B + 3, 0xC89A30, a);                                                          // a gold trim line under the pages
        rect(L - 2, B + 2 + dep, R + 2, B + 3 + dep, 0x12061C, a);
        rect(L - 2, T - 2, L - 1, B + 3 + dep, 0xD6A93C, a); rect(R + 1, T - 2, R + 2, B + 3 + dep, 0x8A6A22, a);
        for (int cxs : new int[]{L - 2, R + 1}) for (int cys : new int[]{T - 2, B + 2}) rect(cxs, cys, cxs + 2, cys + 2, 0xFFE08A, a);                  // corner studs

        int wpx = Math.max(1, Math.round(hw) - 1);
        float stackL = landed / (float) K * 3.2f * bk, stackR = (K - landed) / (float) K * 3.2f * bk;
        for (int side = -1; side <= 1; side += 2) {                                                              // the pages lying left and right
            float thick = side < 0 ? stackL : stackR;
            for (int dx = 1; dx <= wpx; dx++) {
                float u = dx / (float) wpx;
                int x = side < 0 ? bxi - dx : bxi + dx - 1;
                float top = by - hh + 1 + u * u * 2f * bk, bot = by + hh - 1 - u * 1.2f * bk;
                int col = mix(0xF6EBCB, 0xD6C292, u * 0.5f);
                col = Canvas.shade(0xFF000000 | col, 0.58f + 0.42f * Ritual.eo(u / 0.3f)) & 0xFFFFFF;               // the page curves down into the gutter
                if (u > 0.9f) col = mix(col, 0xFFFFFF, 0.25f);
                col = mix(col, rng ? 0xFFE8FF : 0xEEDCFF, Math.min(0.9f, (0.14f + 0.40f * landed / (float) K + 0.30f * pulse) * (1f - u * 0.75f)) * open);          // the light of the gutter washes over the pages
                vspan(x, top, bot, col, a);
                if (thick > 0.3f) { vspan(x, bot, bot + thick, dx % 2 == 0 ? 0xE8DCB8 : 0xBFB088, a); vspan(x, top - Math.min(1.5f, thick * 0.4f), top, 0xFFF3D0, a * 0.8f); }
            }
            if (open > 0.6f) ink(bxi, by, hh, bk, side, wpx, cl((open - 0.6f) / 0.4f), pulse, a);
        }
        rect(bxi - 1, T, bxi + 1, B + 1, 0x2A1040, a);                                                           // the gutter

        for (int k = 0; k < K; k++) {                                                                            // the pages turning over the spine
            float age = op - (fl[k] - fd);
            if (age < 0f || age > fd) continue;
            float f = age / fd;
            if (rng) for (int g = 4; g >= 1; g--) if (f > 0.05f * g) turning(bx, by, hh, bk, wpx, (float) Math.PI * ease(f - 0.05f * g), k, (0.34f - 0.06f * g) * a);       // ghost pages trail behind: the turn is a blur of light
            turning(bx, by, hh, bk, wpx, (float) Math.PI * ease(f), k, a);
            if (f > 0.85f) Ritual.glowE(bx - wpx * 0.6f, by, wpx * 0.9f, hh * 1.1f, mix(AQUA, VIO, 0.4f), 0.28f * (f - 0.85f) / 0.15f * a);     // the page lands and the left side flashes
        }
    }

    /** Lines of runes on a page: violet, with a few cyan ones; they glow when a page lands. */
    private static void ink(int bxi, float by, float hh, float bk, int side, int wpx, float ink, float pulse, float a) {
        for (int l = 0; l < 6; l++) {
            float ly = by - hh + (3.2f + l * 3.6f) * bk;
            int len = Math.round((8 + rnd(l + (side > 0 ? 7 : 0)) * 10) * bk * 0.92f);
            len = Math.min(len, wpx - Math.round(3 * bk));
            int x0 = side < 0 ? bxi - Math.round(3 * bk) - len : bxi + Math.round(3 * bk);
            float al = (0.62f + 0.3f * pulse) * ink * a;
            rect(x0, Math.round(ly), x0 + len, Math.round(ly) + 1, 0x7A4BC2, al);
            if (l % 3 == 1) rect(x0, Math.round(ly) + 1, x0 + len / 2, Math.round(ly) + 2, AQUA, 0.38f * ink * a);
        }
    }

    /** One page at angle th (0 = flat on the right, pi = flat on the left): lifted at the free edge, brighter where it faces the light. */
    private static void turning(float bx, float by, float hh, float bk, int wpx, float th, int k, float a) {
        float cs = (float) Math.cos(th), sn = (float) Math.sin(th), wd = Math.abs(cs) * wpx;
        int cols = (int) Math.ceil(wd);
        for (int dx = 0; dx <= cols; dx++) {
            float cov = dx < wd - 1f ? 1f : cl(wd - dx + 0.5f);
            if (cov <= 0.02f) continue;
            float rr = Math.min(1f, dx / Math.max(1f, wd));
            float lift = sn * 7f * bk * (0.25f + rr) + sn * sn * 2f * bk * (float) Math.sin(rr * 3.14f);
            int x = cs >= 0 ? Math.round(bx) + dx : Math.round(bx) - dx - 1;
            float top = by - hh - lift, bot = by + hh - lift * 0.35f;
            int col = mix(cs >= 0 ? 0xFFF6DC : 0xE8DCB4, 0xB8A070, Math.min(1f, sn * 0.45f + rr * 0.25f));
            vspan(x, top, bot, col, a * cov);
            if (dx % 4 == 2 && sn > 0.15f) vspan(x, top + 3 * bk * 0.8f, top + 3 * bk * 0.8f + 1f, VIO, a * cov * 0.55f);                     // a rune line catching the light as it passes
            if (dx == cols) vspan(x, top - 0.5f, bot + 0.5f, 0xFFFFFF, a * cov);                                  // the bright turning edge
        }
        int ex = cs >= 0 ? Math.round(bx) + cols : Math.round(bx) - cols - 1;
        float et = by - hh - sn * 5f * bk * 1.3f;
        for (int i = 0; i < 7; i++) {                                                                            // sparks shake off the page
            float f = rnd(i + k * 40), age = th / (float) Math.PI;
            Ritual.dot(ex + (f - 0.5f) * 14f, et + 4 + (rnd(i * 3 + 41) - 0.5f) * hh * 1.2f - age * 10f, i % 2 == 0 ? 0xFFFFFF : AQUA, a * 2.2f * sn * (1f - age));
        }
    }

    // ------------------------------------------------------------------ the burst: a column of light out of the book
    private static void burst(float p, float bx, float by, float bk, boolean rng, float a) {
        float fade = cl(1f - p / 3.2f) * 0.5f + 0.18f, ramp = cl(p / 0.25f);
        for (int y = 0; y < by; y++) {
            float f = 1f - (by - y) / Math.max(1f, by), w = (4 + 7 * f) * ramp * (rng ? 1.5f : 1f) * bk / 1.3f;
            float al = 0.43f * fade * (0.3f + 0.7f * f) * a;
            for (int x = (int) -w; x <= w; x++) Ritual.cv.set(Math.round(bx) + x, y, Ritual.A(al * (1f - Math.abs(x) / (w + 1f)), rng ? 0xF3D8FF : 0xE6D2FF));
            int cw = (int) (w / 3);
            for (int x = -cw; x <= cw; x++) Ritual.cv.set(Math.round(bx) + x, y, Ritual.A(Math.min(1f, al * 2f), 0xFFFFFF));
        }
        float fl = cl(1f - p / 0.35f);
        if (fl > 0f) Ritual.glowE(bx, by, 80, 62, 0xF0E4FF, 0.85f * fl * a);
        for (int i = 0; i < (rng ? 40 : 22); i++) {                                                              // sparks of light burst upward and fade
            float f = rnd(i + 70), sp = 24 + 40 * rnd(i + 71); double an = -Math.PI * (0.15 + 0.7 * f);
            float x = bx + (float) Math.cos(an) * sp * Math.min(p, 1.6f) * 0.9f, y = by + (float) Math.sin(an) * sp * Math.min(p, 1.6f) * 0.9f + 3.6f * p * p, al = cl(1f - p / 2.2f) * a;
            int col = i % 3 == 0 ? 0xFFFFFF : i % 3 == 1 ? AQUA : (rng ? GOLD : VIO);
            Ritual.dot(x, y, col, al); Ritual.dot(x, y - 1, col, al * 0.5f); if (i % 4 == 0) Ritual.glint(x, y, 2, 0xFFFFFF, al);
        }
        if (rng) for (int j = 0; j < 3; j++) { float tau = (p - j * 0.12f) / 1.0f; if (tau > 0f && tau < 1f) Ritual.ringE(bx, by, (10 + 120 * Ritual.eo(tau)) * bk * 0.6f, (4 + 36 * Ritual.eo(tau)) * bk * 0.6f, j == 1 ? 0xFFFFFF : 0xE0A8FF, (1f - tau) * 0.8f * a, 0, 6.2832f, 0, 0); }
    }
}
