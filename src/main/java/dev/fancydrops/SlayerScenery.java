package dev.fancydrops;

/**
 * The place each slayer boss stands in, painted on the scene canvas inside an opaque panel: a sky, far and near layers that give it depth,
 * and a ground the hologram stands on. 0 graveyard (zombie), 1 cave (spider), 2 pine forest (wolf), 3 the End (enderman), 4 the Nether (blaze), 5 gothic ruins (vampire).
 */
final class SlayerScenery {
    private SlayerScenery() {}

    private static float rnd(int n) { return Ritual.rnd(n); }
    private static int mix(int a, int b, float f) { return Ritual.mixc(a, b, Math.max(0f, Math.min(1f, f))); }
    private static void px(Canvas c, int x, int y, int rgb, float a) { c.set(x, y, Ritual.A(a, rgb)); }
    private static void rect(Canvas c, int x0, int y0, int x1, int y1, int rgb, float a) { c.rect(x0, y0, x1, y1, Ritual.A(a, rgb)); }

    /** Paints the whole place inside [x0,x1) x [y0,y1); `hz` is the horizon row, `a` the fade of the scene, `t` seconds for the little movements. */
    static void paint(Canvas c, int theme, float t, int x0, int y0, int x1, int y1, int hz, float a, int frameRgb) {
        MOONS.clear();
        int[] sky = switch (theme) {
            case 0 -> new int[]{0x0B0E1E, 0x3A3354};   case 1 -> new int[]{0x07060C, 0x1B1626};   case 2 -> new int[]{0x050B1E, 0x24406A};
            case 3 -> new int[]{0x05020C, 0x2A1448};   case 4 -> new int[]{0x2A0504, 0x9A3412};   default -> new int[]{0x12040A, 0x5A1424};
        };
        for (int y = y0; y < y1; y++) rect(c, x0, y, x1, y + 1, mix(sky[0], sky[1], (y - y0) / (float) Math.max(1, hz - y0)), a);   // the sky, dark above and lighter at the horizon
        if (theme != 1 && theme != 4) for (int i = 0; i < 22; i++) { int sx = x0 + 3 + (int) (rnd(i * 7) * (x1 - x0 - 6)), sy = y0 + 3 + (int) (rnd(i * 13 + 1) * (hz - y0) * 0.55f); px(c, sx, sy, 0xFFFFFF, a * (0.35f + 0.4f * (float) Math.sin(t * 2f + i))); }
        corners(c, x0, y0, x1, y1, 7, frameRgb, a, false);                                             // the frame goes down first: the trees, towers and stones that stand in front of it break out of it
        switch (theme) {
            case 0 -> graveyard(c, t, x0, y0, x1, y1, hz, a);
            case 1 -> cave(c, t, x0, y0, x1, y1, hz, a);
            case 2 -> forest(c, t, x0, y0, x1, y1, hz, a);
            case 3 -> theEnd(c, t, x0, y0, x1, y1, hz, a);
            case 4 -> nether(c, t, x0, y0, x1, y1, hz, a);
            default -> ruins(c, t, x0, y0, x1, y1, hz, a);
        }
        corners(c, x0, y0, x1, y1, 7, frameRgb, a, true);                                              // round off the corners of the panel itself
        for (int y = 0; y < c.h; y++) {                                                                    // only a little may spill out of the frame: 4 px above it, 2 px at the sides, none below
            boolean rowOut = y < y0 - 4 || y >= y1;
            for (int x = 0; x < c.w; x++) if (rowOut || x < x0 - 2 || x >= x1 + 2) c.px[y * c.w + x] = 0;
        }
        moonSpill(c);
    }

    /** The rounded frame (erase = false), or the clearing of everything outside its four round corners (erase = true). */
    static void corners(Canvas c, int x0, int y0, int x1, int y1, int r, int rgb, float a, boolean erase) {
        if (!erase) {
            for (int x = x0 + r; x < x1 - r; x++) { px(c, x, y0, rgb, a * 0.85f); px(c, x, y1 - 1, rgb, a * 0.85f); }
            for (int y = y0 + r; y < y1 - r; y++) { px(c, x0, y, rgb, a * 0.85f); px(c, x1 - 1, y, rgb, a * 0.85f); }
        }
        for (int q = 0; q < 4; q++) {
            int cx = q % 2 == 0 ? x0 + r : x1 - r, cy = q < 2 ? y0 + r : y1 - r;
            for (int dy = 0; dy < r; dy++) for (int dx = 0; dx < r; dx++) {
                int x = q % 2 == 0 ? cx - 1 - dx : cx + dx, y = q < 2 ? cy - 1 - dy : cy + dy;
                double d = Math.hypot(dx + 0.5, dy + 0.5);
                if (erase) { if (d > r + 0.8) c.px[y * c.w + x] = 0; }
                else if (Math.abs(d - r) < 0.8) px(c, x, y, rgb, a * 0.85f);
            }
        }
    }

    // ---- shared pieces
    /** A ridge of far hills or rooftops: its height is a few waves, so it never repeats exactly. */
    static void ridge(Canvas c, int x0, int x1, int hz, float amp, float freq, float ph, int rgb, float a) {
        for (int x = x0; x < x1; x++) { int h = (int) (amp * (0.55f + 0.3f * Math.sin(x * freq + ph) + 0.15f * Math.sin(x * freq * 2.7f + ph * 2))); rect(c, x, hz - h, x + 1, hz, rgb, a); }
    }
    /** The ground: a perspective floor, finer and darker far away. `mixA`/`mixB` are the two kinds of ground, `lump` how much of the second one shows. */
    static void floor(Canvas c, int x0, int y0, int x1, int y1, int hz, int ga, int gb, float lump, int seed, float a) {
        for (int y = hz; y < y1; y++) {
            float d = (y - hz) / (float) Math.max(1, y1 - hz), cell = 1f + d * 3f;
            for (int x = x0; x < x1; x++) {
                int cx = (int) (x / cell), cy = (int) (y / (cell * 0.6f)); float n = rnd(cx * 31 + cy * 57 + seed), lp = rnd((cx / 3) * 91 + (cy / 3) * 17 + seed + 5);
                int base = lp < lump ? gb : ga;
                px(c, x, y, mix(0x000000, mix(base, 0xFFFFFF, n * 0.10f - 0.03f), 0.45f + 0.55f * d), a);
            }
        }
    }
    private static void tomb(Canvas c, int x, int base, int w, int h, int rgb, float a) {
        for (int i = 0; i < h; i++) { int inset = i > h - 3 ? (3 - (h - i)) : 0; rect(c, x - w / 2 + inset, base - i, x + w / 2 - inset, base - i + 1, i < 2 ? mix(rgb, 0x000000, 0.4f) : mix(rgb, 0xFFFFFF, 0.06f * (i % 3)), a); }
        rect(c, x - 1, base - h + 5, x + 1, base - h + 6 + h / 3, mix(rgb, 0x000000, 0.45f), a); rect(c, x - h / 5, base - h + 7, x + h / 5 + 1, base - h + 8, mix(rgb, 0x000000, 0.45f), a);   // a cross cut in the stone
    }
    private static void deadTree(Canvas c, int x, int base, int h, int rgb, float a) {
        rect(c, x, base - h, x + 2, base, rgb, a);
        for (int b = 0; b < 4; b++) { int by = base - h + b * h / 6, dir = b % 2 == 0 ? -1 : 1, len = 5 + (b * 3 + x) % 6; for (int i = 0; i < len; i++) px(c, x + 1 + dir * i, by - i / 2, rgb, a); }
    }
    static void pine(Canvas c, int x, int base, int h, int rgb, float a) {
        rect(c, x, base - 3, x + 1, base, mix(rgb, 0x3B2A1A, 0.5f), a);
        for (int i = 0; i < h; i++) { int w = 1 + (int) ((i / (float) h) * h * 0.38f); rect(c, x - w / 2, base - h + i - 2, x + (w + 1) / 2, base - h + i - 1, mix(rgb, 0xFFFFFF, i % 4 == 0 ? 0.08f : 0f), a); }
    }
    private static final java.util.List<int[]> MOONS = new java.util.ArrayList<>();       // the moons of this frame: {x, y, radius, rgb, alpha in 1/1000}
    private static void moon(Canvas c, int cx, int cy, int r, int rgb, float a) {
        MOONS.add(new int[]{cx, cy, r, rgb, Math.round(a * 1000)});
        for (int g = 3; g >= 1; g--) c.disc(cx, cy, r + g * 3, Ritual.A(a * 0.06f, rgb));
        c.disc(cx, cy, r, Ritual.A(a, rgb));
    }
    /** The glow of a moon is allowed to spill past the frame (the rest of the scenery is not): paint it again, only where nothing is. */
    private static void moonSpill(Canvas c) {
        if (MOONS.isEmpty()) return;
        boolean[] empty = new boolean[c.px.length]; for (int i = 0; i < empty.length; i++) empty[i] = (c.px[i] >>> 24) == 0;      // (decided before any moon is drawn, so its glow still layers)
        for (int[] m : MOONS) {
            float a = m[4] / 1000f;
            for (int g = 3; g >= 0; g--) {
                int rad = g == 0 ? m[2] : m[2] + g * 3, col = Ritual.A(g == 0 ? a : a * 0.06f, m[3]);
                for (int y = -rad; y <= rad; y++) for (int x = -rad; x <= rad; x++) {
                    int xx = m[0] + x, yy = m[1] + y;
                    if (x * x + y * y > rad * rad || xx < 0 || yy < 0 || xx >= c.w || yy >= c.h || !empty[yy * c.w + xx]) continue;
                    c.set(xx, yy, col);
                }
            }
        }
        MOONS.clear();
    }
    static void fog(Canvas c, float t, int x0, int x1, int y, int h, int rgb, float a, int seed) {
        for (int x = x0; x < x1; x++) { float w = 0.5f + 0.5f * (float) Math.sin(x * 0.05f + t * 0.4f + seed); rect(c, x, y, x + 1, y + h, rgb, a * w * 0.18f); }
    }

    // ---- the six places
    private static void graveyard(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float a) {
        moon(c, x0 + (x1 - x0) * 3 / 4, y0 + (hz - y0) / 4, 7, 0xE8E4D0, a);
        ridge(c, x0, x1, hz, (hz - y0) * 0.30f, 0.045f, 1.3f, 0x1A1A2C, a);                                       // far hills
        for (int i = 0; i < 4; i++) deadTree(c, x0 + 14 + i * (x1 - x0) / 4 + (int) (rnd(i) * 10), hz - 1, 20 + (int) (rnd(i + 9) * 10), 0x0E0E1A, a);   // dead trees, far
        for (int i = 0; i < 9; i++) tomb(c, x0 + 8 + i * (x1 - x0 - 12) / 8 + (int) (rnd(i * 3) * 6), hz + 1, 5, 7, 0x3A3F52, a);   // the back row of stones, small
        fog(c, t, x0, x1, hz - 6, 9, 0x8A90B8, a, 1);
        floor(c, x0, y0, x1, y1, hz + 1, 0x2F5A2A, 0x5A3E24, 0.45f, 11, a);                                      // grass and dirt
        for (int i = 0; i < 40; i++) { int gx = x0 + (int) (rnd(i * 5) * (x1 - x0)), gy = hz + 2 + (int) (rnd(i * 5 + 1) * (y1 - hz - 3)); rect(c, gx, gy - 2, gx + 1, gy, 0x4F8A3A, a * 0.8f); }   // tufts
        tomb(c, x0 + 16, y1 - 9, 11, 17, 0x6A7088, a); tomb(c, x1 - 20, y1 - 5, 13, 21, 0x5A6078, a); tomb(c, x0 + (x1 - x0) / 3, y1 - 3, 9, 14, 0x4A4F66, a);   // the near stones, big
        fog(c, t, x0, x1, hz + 2, 5, 0xB0B8D8, a, 7);
    }
    private static void cave(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float a) {
        ridge(c, x0, x1, hz, (hz - y0) * 0.35f, 0.06f, 0.4f, 0x14101C, a);
        for (int i = 0; i < 9; i++) { int sx = x0 + 6 + i * (x1 - x0 - 8) / 8, len = 8 + (int) (rnd(i) * 18); for (int k = 0; k < len; k++) { int w = Math.max(1, 4 - k * 4 / len); rect(c, sx - w / 2, y0 + k, sx + (w + 1) / 2, y0 + k + 1, mix(0x1E1A28, 0x000000, k / (float) len * 0.5f), a); } }   // stalactites
        for (int k = 0; k < 4; k++) { int ax = k < 2 ? x0 : x1 - 1, dir = k < 2 ? 1 : -1; for (int r = 0; r < 4; r++) for (int i = 0; i < 20 + r * 4; i++) { px(c, ax + dir * (i * (1 + r)) / 3, y0 + 1 + (k % 2) * 10 + r * 6 + i / 2, 0xC8C8D8, a * 0.35f); } }   // cobwebs in the corners
        for (int i = 0; i < 6; i++) { int wx = x0 + 18 + i * (x1 - x0 - 30) / 5, wl = 14 + (int) (rnd(i + 3) * 20); rect(c, wx, y0, wx + 1, y0 + wl, 0xB8B8C8, a * 0.3f); c.disc(wx, y0 + wl, 1, Ritual.A(a * 0.5f, 0xB8B8C8)); }     // hanging threads
        floor(c, x0, y0, x1, y1, hz + 1, 0x2A2832, 0x3A2A1E, 0.30f, 23, a);
        for (int i = 0; i < 3; i++) { int ex = x0 + 20 + (int) (rnd(i * 3) * (x1 - x0 - 40)), ey = hz - 6 - (int) (rnd(i) * 8); if ((int) (t * 0.7f + i * 2) % 4 != 0) { px(c, ex, ey, 0xFF3030, a); px(c, ex + 3, ey, 0xFF3030, a); } }   // red eyes in the dark
        fog(c, t, x0, x1, hz - 2, 6, 0x7A708C, a, 3);
    }
    private static void forest(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float a) {
        int cx = (x0 + x1) / 2, hw = (x1 - x0) / 2;
        moon(c, cx, y0 + (hz - y0) / 3, 9, 0xEEF2FF, a * 0.8f);                                              // the moon hangs right behind the boss
        for (int x = x0; x < x1; x++) { int h = (int) ((hz - y0) * 0.30f * (0.55f + 0.3f * Math.sin(Math.abs(x - cx) * 0.05f + 2.1f))); rect(c, x, hz - h, x + 1, hz, 0x0E1A30, a); }   // far hills, the same on both sides
        for (int i = 0; i < 6; i++) { int off = 8 + i * (hw - 12) / 5, h = 18 + (int) (rnd(i + 3) * 10); pine(c, cx - off, hz, h, 0x0F2A2C, a); pine(c, cx + off, hz, h, 0x0F2A2C, a); }   // far trees, mirrored
        fog(c, t, x0, x1, hz - 7, 8, 0x9AB0D8, a, 5);
        for (int i = 0; i < 3; i++) { int off = 28 + i * (hw - 36) / 2, h = 24 + (int) (rnd(i + 5) * 10); pine(c, cx - off, hz + 4, h, 0x123A30, a); pine(c, cx + off, hz + 4, h, 0x123A30, a); }   // nearer trees, leaving the middle open
        floor(c, x0, y0, x1, y1, hz + 1, 0x2A5A34, 0x4A3A28, 0.35f, 31, a);
        for (int i = 0; i < 12; i++) { int sx = x0 + (int) (rnd(i * 5) * (x1 - x0)), sy = hz + 3 + (int) (rnd(i * 5 + 1) * (y1 - hz - 4)); rect(c, sx, sy, sx + 2, sy + 1, 0xE8F0FF, a * 0.45f); }   // a few patches of snow
        pine(c, x0 + 8, y1 + 2, 40, 0x0C2A22, a); pine(c, x1 - 9, y1 + 2, 40, 0x0C2A22, a);                                                       // the two big trees at the sides
    }
    private static void theEnd(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float a) {
        int cx = (x0 + x1) / 2, hw = (x1 - x0) / 2;
        for (int g = 4; g >= 1; g--) c.disc(cx, y0 + (hz - y0) / 2, 10 + g * 4, Ritual.A(a * 0.07f, 0xC060FF)); c.disc(cx, y0 + (hz - y0) / 2, 10, Ritual.A(a * 0.9f, 0x0A0414));   // a dark sun with a violet halo behind the boss
        for (int i = 0; i < 2; i++) { int w = 16 + i * 8, iy = y0 + 14 + i * 12, off = hw / 2 + i * hw / 4, bob = Math.round((float) Math.sin(t * 0.6f + i) * 1.5f);
            for (int side = -1; side <= 1; side += 2) for (int k = 0; k < 6; k++) rect(c, cx + side * off - w / 2 + k, iy + bob + k, cx + side * off + w / 2 - k, iy + bob + k + 1, mix(0x3A3252, 0x0E0818, k / 6f), a); }   // islands floating far away, one each side
        for (int i = 0; i < 2; i++) { int off = hw / 3 + i * hw / 3, ph = 34 - i * 8;
            for (int side = -1; side <= 1; side += 2) { int pxc = cx + side * off; rect(c, pxc - 3, hz - ph, pxc + 3, hz, 0x0C0614, a); rect(c, pxc - 3, hz - ph, pxc - 2, hz, 0x2A1A44, a);
                c.disc(pxc, hz - ph - 3, 3, Ritual.A(a * (0.7f + 0.3f * (float) Math.sin(t * 3 + i + side)), 0xE060FF)); } }                                // obsidian pillars with crystals, mirrored
        floor(c, x0, y0, x1, y1, hz + 1, 0xD8D49A, 0xA8A468, 0.40f, 41, a);
        fog(c, t, x0, x1, hz - 1, 5, 0xB070FF, a, 9);
        for (int i = 0; i < 8; i++) { float f = (t * 0.25f + i * 0.13f) % 1f; px(c, x0 + (int) (rnd(i * 9) * (x1 - x0)), hz + 4 - (int) (f * (hz - y0) * 0.5f), 0xD060FF, a * (1f - f)); }   // particles drifting up
    }
    private static void nether(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float a) {
        for (int x = x0; x < x1; x++) { float f = 0.5f + 0.5f * (float) Math.sin(x * 0.07f + t * 1.4f); rect(c, x, hz - 4 - (int) (3 * f), x + 1, hz, 0xFF7A18, a * 0.35f); }                  // the glow of the lava lake far away
        int fx = x0 + (x1 - x0) / 5; for (int b = 0; b < 5; b++) { int bw = 18 + (int) (rnd(b) * 12), bh = 18 + (int) (rnd(b + 4) * 30); if (fx + bw > x1 - 3) break; rect(c, fx, hz - bh, fx + bw, hz, 0x240808, a); for (int k = 0; k < bw; k += 4) rect(c, fx + k, hz - bh - 3, fx + k + 2, hz - bh, 0x240808, a); rect(c, fx + bw / 2 - 1, hz - bh / 2, fx + bw / 2 + 1, hz - bh / 2 + 4, 0xFFB030, a * 0.7f); fx += bw + 4 + (int) (rnd(b + 8) * 10); }   // a fortress
        floor(c, x0, y0, x1, y1, hz + 1, 0x5A1C1C, 0x2A0C0E, 0.40f, 53, a);
        for (int i = 0; i < 6; i++) { int lx = x0 + 6 + (int) (rnd(i * 4) * (x1 - x0 - 12)), ly = hz + 3 + (int) (rnd(i * 4 + 1) * (y1 - hz - 5)); float p = 0.6f + 0.4f * (float) Math.sin(t * 2 + i); rect(c, lx, ly, lx + 6, ly + 2, 0xFF8A20, a * p); rect(c, lx + 1, ly + 1, lx + 5, ly + 2, 0xFFE070, a * p * 0.8f); }   // lava pools in the floor
        for (int i = 0; i < 14; i++) { float f = (t * 0.35f + i * 0.071f) % 1f; px(c, x0 + (int) (rnd(i * 5) * (x1 - x0)) + (int) (Math.sin(t + i) * 3), y1 - 2 - (int) (f * (y1 - y0 - 6)), 0xFFB030, a * (1f - f)); }   // embers rising
    }
    private static void ruins(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float a) {
        int cx = (x0 + x1) / 2, hw = (x1 - x0) / 2;
        for (int x = x0; x < x1; x++) { int h = 4 + (int) (3 * Math.sin(Math.abs(x - cx) * 0.09f)); rect(c, x, hz - h, x + 1, hz, 0x16060E, a); }                                // low far hills
        rect(c, cx - 17, hz - 34, cx + 17, hz, 0x16060E, a); for (int k = 0; k < 4; k++) rect(c, cx - 17 + k * 10, hz - 40, cx - 13 + k * 10, hz - 34, 0x16060E, a);               // the castle, in the middle
        for (int side = -1; side <= 1; side += 2) for (int k = 0; k < 14; k++) rect(c, cx + side * 11 - 2 + (side * k) / 8, hz - 54 + k, cx + side * 11 + 2 - k / 5, hz - 53 + k, 0x16060E, a);       // two spires
        rect(c, cx - 2, hz - 26, cx + 2, hz - 18, 0xFFB050, a * 0.8f);
        moon(c, cx, y0 + (hz - y0) / 3, 11, 0xB82238, a * 0.8f);                                              // the red moon behind the castle
        for (int i = 0; i < 5; i++) { int bx = x0 + (int) ((t * 14 + i * 53) % (x1 - x0)), by = y0 + 10 + i * 6 + Math.round((float) Math.sin(t * 5 + i) * 3); boolean up = ((int) (t * 8 + i) % 2) == 0; px(c, bx, by, 0x050204, a); px(c, bx - 1, by - (up ? 1 : 0), 0x050204, a); px(c, bx + 1, by - (up ? 1 : 0), 0x050204, a); }   // bats
        for (int i = 0; i < 2; i++) { int off = hw * 2 / 3 - i * hw / 3, ch = 30 - i * 8;
            for (int side = -1; side <= 1; side += 2) { int cx2 = cx + side * off; rect(c, cx2 - 2, hz - ch, cx2 + 2, hz + 2, 0x2A1820, a); rect(c, cx2 - 3, hz - ch, cx2 + 3, hz - ch + 2, 0x3A2028, a); } }   // broken columns, mirrored
        floor(c, x0, y0, x1, y1, hz + 1, 0x2A1A22, 0x4A0E1C, 0.30f, 67, a);
        fog(c, t, x0, x1, hz - 2, 6, 0xA03050, a, 4);
    }
}
