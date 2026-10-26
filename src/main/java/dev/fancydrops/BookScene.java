package dev.fancydrops;

/**
 * The Experimentation Table scene, painted on the Ritual canvas (Ritual.paint adds the Normal and RNG finish on top).
 *
 * Runes spiral from the table into its floating book; the book swings open and the view leans in; its pages turn one after another
 * (three for Normal, five and faster for RNG, the pages overlapping like a riffle); then a column of light bursts out of the book and the
 * drop rises from it. Every edge is drawn with sub-pixel coverage and every move is eased, so it stays smooth at any frame rate.
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

    /** The seconds at which each page lands (also where the Beat sound plays): three for Normal, five for RNG. */
    static float[] flips(boolean rng, float O) {
        return rng ? new float[]{0.22f * O, 0.38f * O, 0.54f * O, 0.70f * O, 0.86f * O} : new float[]{0.30f * O, 0.52f * O, 0.74f * O};
    }
    /** How long one page takes to turn. */
    static float flipLen(boolean rng) { return rng ? 0.34f : 0.42f; }
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
    /** A 3 x 3 rune glyph from a number: some of its nine cells lit. */
    private static void glyph(float gx, float gy, int bits, int rgb, float a) {
        if (a <= 0.02f) return;
        for (int b = 0; b < 9; b++) if ((bits >> b & 1) != 0 && (b % 3 + b / 3) != 4) Ritual.cv.set(Math.round(gx) + b % 3, Math.round(gy) + b / 3, Ritual.A(a, rgb));
    }

    // ------------------------------------------------------------------ the scene
    /** The scene at time t. op = the opening clock (stops at O), p = seconds since the drop appeared, a = how visible it is. by = the book's resting centre. */
    static void paint(float t, float O, float p, float icx, float by0, float sz, float a, boolean rng) {
        if (a <= 0.01f) return;
        float op = Math.min(t, O), fd = flipLen(rng);
        float[] fl = flips(rng, O); int K = fl.length;
        float appear = sm(t / (0.12f * O + 0.05f));
        float zIn = ease((op - 0.06f * O) / (0.30f * O)), zOut = p > 0f ? ease(p / 0.8f) : 0f;
        float z = 1f + 0.36f * zIn * (1f - zOut);
        float open = Ritual.eo((op - 0.03f * O) / (0.26f * O));
        float al = a * appear;
        float bk = sz * 2.2f * z, bx = icx;
        float bob = (float) Math.sin(t * 2.1f) * 1.3f * sz * (1f - zIn);
        float by = by0 + bob;
        int landed = 0; float pulse = 0f;
        for (int k = 0; k < K; k++) { if (op >= fl[k]) landed = k + 1; float ag = op - fl[k]; if (ag >= 0f && ag < 0.32f) pulse = Math.max(pulse, 1f - ag / 0.32f); }
        pulse *= 1f - cl(p / 0.3f);
        boolean split = p >= 0f;

        Ritual.glowE(bx, by, 86 * sz, 60 * sz, 0x07040F, 0.50f * al);                                           // a dark soft ground so the book reads on any world
        Ritual.glowE(bx, by, (56 + 16 * pulse) * sz * z, (40 + 11 * pulse) * sz * z, rng ? 0xE0A8FF : VIO, ((0.22f + 0.24f * pulse) * open + (split ? 0.3f * cl(1f - p / 1.2f) : 0f)) * al);

        table(bx, by, bk, al * (1f - zIn));
        if (!split) runes(t, bx, by, bk, landed, rng, al);
        book(t, op, bx, by, bk, open, K, landed, fl, fd, pulse, rng, al);
        if (rng && !split) {                                                                                      // an RNG book hums gold between flips
            for (int i = 0; i < 12; i++) { float an = t * 1.8f + i * 0.5236f; Ritual.dot(bx + (float) Math.cos(an) * (22 * bk + 6), by + (float) Math.sin(an) * (12 * bk + 5), GOLD, (0.4f + 0.5f * cl(landed / (float) K)) * al); }
        }
        if (split) burst(p, bx, by, bk, rng, al);
    }

    private static void table(float cx, float by, float bk, float a) {
        if (a <= 0.02f) return;
        float ty = by + 25 * bk, tw = 24 * bk, th = 5 * bk, bh = 14 * bk;
        Ritual.fillE(cx, ty + th + bh + 1, tw * 1.25f, 3.5f * bk, 0x07050C, 0.42f * a);
        rect(Math.round(cx - tw * 0.8f), Math.round(ty + th), Math.round(cx + tw * 0.8f), Math.round(ty + th + bh), 0x171221, a);                  // the dark body of the table
        for (int i = 0; i < 12; i++) Ritual.dot(cx - tw * 0.8f + 2 + rnd(i * 5) * (tw * 1.6f - 4), ty + th + 1 + rnd(i * 7) * (bh - 2), 0x7A4BC2, 0.5f * a);
        rect(Math.round(cx - tw), Math.round(ty), Math.round(cx + tw), Math.round(ty + th), 0xA11E2E, a);                                       // the red cloth top
        rect(Math.round(cx - tw), Math.round(ty), Math.round(cx + tw), Math.round(ty) + 1, 0xD6525F, a);
        rect(Math.round(cx - tw / 3), Math.round(ty) + 1, Math.round(cx + tw / 3), Math.round(ty + th) - 1, 0xC2303F, a);
        rect(Math.round(cx - tw), Math.round(ty + th) - 1, Math.round(cx + tw), Math.round(ty + th), 0x6A1420, a);
    }

    /** Runes lifting off the table and spiralling into the book; they come faster with every page that lands. */
    private static void runes(float t, float bx, float by, float bk, int landed, boolean rng, float a) {
        float rate = 0.45f + (rng ? 0.30f : 0.16f) * landed;
        for (int i = 0; i < 16; i++) {
            float u = (t * rate + i / 16f + rnd(i * 3) ) % 1f;
            double an = i * 2.4 + t * 1.4; float rr = (1f - u) * 42f * bk;
            float gx = bx + (float) Math.cos(an) * rr, gy = by + 4 * bk + (float) Math.sin(an) * rr * 0.35f - u * 14f * bk + (1f - u) * 18f * bk;
            float al = Math.min(1f, u * 4f) * (1f - u * u);
            glyph(gx, gy, (int) (rnd(i * 9) * 511f), i % 3 == 0 ? 0xFFFFFF : i % 3 == 1 ? AQUA : VIO, al * a);
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
                col = mix(col, 0xE6D2FF, 0.18f * open * (0.5f + 0.5f * pulse));
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
            if (rng && f > 0.12f) turning(bx, by, hh, bk, wpx, (float) Math.PI * ease(f - 0.1f), k, 0.2f * a);    // a faint ghost a moment behind: the riffle blurs
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
            float lift = sn * 5f * bk * (0.3f + rr);
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
        for (int i = 0; i < (rng ? 34 : 22); i++) {                                                              // runes burst upward and fade
            float f = rnd(i + 70), sp = 24 + 40 * rnd(i + 71); double an = -Math.PI * (0.15 + 0.7 * f);
            float x = bx + (float) Math.cos(an) * sp * Math.min(p, 1.6f) * 0.9f, y = by + (float) Math.sin(an) * sp * Math.min(p, 1.6f) * 0.9f + 3.6f * p * p;
            glyph(x, y, (int) (rnd(i + 72) * 511f), i % 3 == 0 ? 0xFFFFFF : i % 3 == 1 ? AQUA : (rng ? GOLD : VIO), cl(1f - p / 2.2f) * a);
        }
    }
}
