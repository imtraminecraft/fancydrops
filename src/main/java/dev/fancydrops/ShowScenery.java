package dev.fancydrops;

/**
 * The places the Showcase scenes (and the Mythological ones) play in: a small framed vignette behind the stage, the same kind of
 * see-through rounded panel as the Farming and Slayer places, so every drop has a world of its own without taking over the screen.
 *
 *   WATER     an ocean at dusk: low sun, far islands, a glittering sea and a little dock with a lantern
 *   LAVA      the Magma Fields: a smoking volcano, ash falling, a glowing sea of lava
 *   SPOOKY    a pumpkin moon over a graveyard pond, bare trees, fog
 *   WINTER    Jerry's Workshop at night: an aurora over snowy hills and pines, snow falling
 *   CRIMSON   the Crimson Isle: a burning sky, floating rocks and the towers of a fortress
 *   MOBS      a dungeon wall of old stone, two torches flickering, cobwebs in the corners
 *   DYE       a painter's studio: a warm wall spattered with paint, a wooden floor, an easel
 *   MYTH      the Hub at twilight: stars, a crescent moon, ruined pillars on the hills (Mythological)
 *   TREASURE  a forest clearing at dusk, fireflies over the grass (Treasure)
 *   9..14     the six slayer places (SlayerShow)
 */
final class ShowScenery {
    private ShowScenery() {}

    static final int MYTH = 7, TREASURE = 8;

    private static float rnd(int n) { return Ritual.rnd(n); }
    private static int mix(int a, int b, float f) { return Ritual.mixc(a, b, Math.max(0f, Math.min(1f, f))); }
    private static void px(Canvas c, int x, int y, int rgb, float a) { c.set(x, y, Ritual.A(a, rgb)); }
    private static void rect(Canvas c, int x0, int y0, int x1, int y1, int rgb, float a) { c.rect(x0, y0, x1, y1, Ritual.A(a, rgb)); }

    /** The panel round (icx, gy): returns {x0, y0, x1, y1, horizon}. */
    static int[] box(float icx, float gy, float sz) {
        int x0 = Math.max(0, Math.round(icx - 112 * sz)), x1 = Math.min(Ritual.CW, Math.round(icx + 112 * sz)), y0 = Math.max(0, Math.round(gy - 104 * sz)), y1 = Math.min(Ritual.CH, Math.round(gy + 26 * sz));
        return new int[]{x0, y0, x1, y1, Math.round(gy - 14 * sz)};
    }

    /** Paints place `m` (ShowScene motif, or MYTH / TREASURE) faded by `a`, `tint` = the dye's colour. Everything outside the frame is cleared. */
    static void paint(Canvas c, int m, float t, float icx, float gy, float sz, float a, int tint) {
        if (a <= 0.01f) return;
        int[] b = box(icx, gy, sz); int x0 = b[0], y0 = b[1], x1 = b[2], y1 = b[3], hz = b[4];
        int[] sky = switch (m) {
            case ShowScene.WATER -> new int[]{0x142A5A, 0x5A5A98, 0xF0A070};
            case ShowScene.LAVA -> new int[]{0x0C0606, 0x3A0E08, 0x9A2A0E};
            case ShowScene.SPOOKY -> new int[]{0x0A0618, 0x2A1240, 0x5A2A50};
            case ShowScene.WINTER -> new int[]{0x040C1E, 0x0E2A40, 0x2A5A6A};
            case ShowScene.CRIMSON -> new int[]{0x1A0404, 0x6A140C, 0xE0602A};
            case ShowScene.MOBS -> new int[]{0x0C0C10, 0x16161E, 0x22222C};
            case ShowScene.DYE -> new int[]{0xE8D8C0, 0xF2E6D2, 0xF8F0E2};
            case MYTH -> new int[]{0x1A1A48, 0x6A5A9A, 0xE8A8A0};
            case 9, 10, 11, 12, 13, 14 -> SlayerShow.SKY[m - SlayerShow.BASE];
            default -> new int[]{0x1A2A3A, 0x5A5A4A, 0xE09A5A};
        };
        for (int y = y0; y < y1; y++) {
            float f = (y - y0) / (float) Math.max(1, hz - y0);
            rect(c, x0, y, x1, y + 1, f < 0.55f ? mix(sky[0], sky[1], f / 0.55f) : mix(sky[1], sky[2], (f - 0.55f) / 0.45f), a);
        }
        int frame = m <= ShowScene.DYE ? ShowScene.ENERGY[m] : m == MYTH ? 0x55D8FF : m >= SlayerShow.BASE ? SlayerShow.ENERGY[m - SlayerShow.BASE] : 0xFFD25A;
        if (m == ShowScene.DYE) frame = tint;
        int r = Math.max(7, Math.round(7 * Ritual.K));
        SlayerScenery.corners(c, x0, y0, x1, y1, r, frame, a, false);
        float k = sz / (Ritual.SMALLER * Ritual.K * Ritual.SCENE_K);                       // 1 at the default size
        switch (m) {
            case ShowScene.WATER -> ocean(c, t, x0, y0, x1, y1, hz, k, a);
            case ShowScene.LAVA -> magma(c, t, x0, y0, x1, y1, hz, k, a);
            case ShowScene.SPOOKY -> graveyard(c, t, x0, y0, x1, y1, hz, k, a);
            case ShowScene.WINTER -> aurora(c, t, x0, y0, x1, y1, hz, k, a);
            case ShowScene.CRIMSON -> isle(c, t, x0, y0, x1, y1, hz, k, a);
            case ShowScene.MOBS -> dungeon(c, t, x0, y0, x1, y1, hz, k, a);
            case ShowScene.DYE -> studio(c, t, x0, y0, x1, y1, hz, k, a, tint);
            case MYTH -> hub(c, t, x0, y0, x1, y1, hz, k, a);
            case 9, 10, 11, 12, 13, 14 -> SlayerShow.place(c, m - SlayerShow.BASE, t, x0, y0, x1, y1, hz, k, a);
            default -> clearing(c, t, x0, y0, x1, y1, hz, k, a);
        }
        for (int y = y0; y < y1; y++) {                                                    // a soft vignette, darker at the edges
            float v = Math.abs((y - (y0 + y1) / 2f) / ((y1 - y0) / 2f));
            for (int x = x0; x < x1; x++) {
                float u = Math.abs((x - (x0 + x1) / 2f) / ((x1 - x0) / 2f)), d = Math.max(u, v);
                if (d > 0.72f) px(c, x, y, 0x000000, (d - 0.72f) / 0.28f * 0.45f * a);
            }
        }
        SlayerScenery.corners(c, x0, y0, x1, y1, r, frame, a, true);
        for (int y = 0; y < c.h; y++) for (int x = 0; x < c.w; x++) if (y < y0 || y >= y1 || x < x0 || x >= x1) c.px[y * c.w + x] = 0;
    }

    // ------------------------------------------------------------------ shared pieces
    static void stars(Canvas c, float t, int x0, int y0, int x1, int y1, float a, int n) {
        for (int i = 0; i < n; i++) {
            int x = x0 + (int) (rnd(i * 13 + 1) * (x1 - x0)), y = y0 + (int) (rnd(i * 7 + 3) * (y1 - y0));
            float tw = 0.4f + 0.6f * (0.5f + 0.5f * (float) Math.sin(t * (1.5f + rnd(i) * 2f) + i));
            px(c, x, y, 0xFFFFFF, tw * a * (i % 5 == 0 ? 1f : 0.6f));
            if (i % 9 == 0) { px(c, x - 1, y, 0xFFFFFF, tw * a * 0.3f); px(c, x + 1, y, 0xFFFFFF, tw * a * 0.3f); px(c, x, y - 1, 0xFFFFFF, tw * a * 0.3f); px(c, x, y + 1, 0xFFFFFF, tw * a * 0.3f); }
        }
    }
    static void band(Canvas c, int x0, int x1, int ya, int yb, int top, int bot, float a) {
        for (int y = ya; y < yb; y++) rect(c, x0, y, x1, y + 1, mix(top, bot, (y - ya) / (float) Math.max(1, yb - ya)), a);
    }
    static void tree(Canvas c, int x, int base, int h, int rgb, float a) {               // a bare tree: a trunk and a few crooked branches
        for (int i = 0; i < h; i++) rect(c, x - Math.max(1, (h - i) / 8), base - i, x + Math.max(1, (h - i) / 8) + 1, base - i + 1, rgb, a);
        for (int k = 0; k < 4; k++) {
            int by = base - h / 2 - k * h / 9, dir = k % 2 == 0 ? 1 : -1; float len = h * (0.35f - k * 0.05f);
            for (int j = 0; j < len; j++) px(c, x + dir * j, by - j / 2 - (int) (Math.sin(j * 0.4f) * 1.5f), rgb, a);
        }
    }
    static void pine(Canvas c, int x, int base, int h, int rgb, int snow, float a) {
        for (int i = 0; i < h; i++) {
            float u = i / (float) h; int w = Math.round((1f - u) * h * 0.32f * (0.75f + 0.25f * (float) Math.sin(i * 1.3f)));
            rect(c, x - w, base - i, x + w + 1, base - i + 1, rgb, a);
            if (i % 4 == 0 && w > 1) rect(c, x - w, base - i, x + w + 1, base - i + 1, snow, a * 0.7f);
        }
        rect(c, x, base, x + 1, base + 2, 0x2A1A0E, a);
    }

    // ------------------------------------------------------------------ the places
    private static void ocean(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float k, float a) {
        int cx = (x0 + x1) / 2;
        stars(c, t, x0, y0, x1, y0 + (hz - y0) / 3, a * 0.6f, 24);
        Ritual.glowE(cx + 40 * k, hz - 4, 50 * k, 22 * k, 0xFFB070, 0.5f * a);                    // the low sun
        c.disc(Math.round(cx + 40 * k), hz - 2, Math.round(9 * k), Ritual.A(a, 0xFFD8A0));
        SlayerScenery.ridge(c, x0, x0 + (x1 - x0) * 2 / 5, hz, 10 * k, 0.05f / k, 1f, 0x2A2A58, a);       // far islands
        SlayerScenery.ridge(c, x1 - (x1 - x0) / 4, x1, hz, 7 * k, 0.07f / k, 3f, 0x34346A, a);
        band(c, x0, x1, hz, y1, 0x2A4A8A, 0x0A1A3A, a);                                           // the sea
        for (int i = 0; i < 70; i++) {                                                           // glitter on the water
            float u = rnd(i * 3), d = rnd(i * 11 + 2); int y = hz + 1 + (int) (d * d * (y1 - hz - 2)), w = 1 + (int) (d * 6 * k);
            int x = Math.round(x0 + u * (x1 - x0) + (float) Math.sin(t * 1.2f + i) * 2 * k);
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 3f + i * 1.7f);
            boolean sun = Math.abs(x - (cx + 40 * k)) < 18 * k;
            rect(c, x, y, x + w, y + 1, sun ? 0xFFD8A0 : 0x8AB8FF, a * tw * (sun ? 0.9f : 0.45f));
        }
        int dx = Math.round(x0 + 14 * k), dy = hz + Math.round(6 * k);                              // the dock and its lantern
        rect(c, x0, dy, dx + Math.round(26 * k), dy + Math.round(3 * k), 0x3A2614, a);
        for (int p = 0; p < 3; p++) rect(c, x0 + Math.round((6 + p * 10) * k), dy, x0 + Math.round((8 + p * 10) * k), dy + Math.round(12 * k), 0x2A1A0C, a);
        rect(c, dx + Math.round(20 * k), dy - Math.round(12 * k), dx + Math.round(21 * k), dy, 0x2A1A0C, a);
        float fl = 0.8f + 0.2f * (float) Math.sin(t * 9) * (float) Math.sin(t * 5.3f);
        Ritual.glowE(dx + 21 * k, dy - 13 * k, 10 * k, 10 * k, 0xFFC060, 0.6f * fl * a);
        rect(c, dx + Math.round(19 * k), dy - Math.round(15 * k), dx + Math.round(23 * k), dy - Math.round(11 * k), 0xFFD880, a);
        for (int g = 0; g < 3; g++) {                                                            // gulls
            float gx = x0 + ((t * 8 * k + g * 60 * k) % (x1 - x0)), gy = y0 + (18 + g * 7) * k; boolean up = ((int) (t * 5 + g) % 2) == 0;
            px(c, Math.round(gx), Math.round(gy), 0x1A1A30, a); px(c, Math.round(gx) - 1, Math.round(gy) + (up ? -1 : 0), 0x1A1A30, a); px(c, Math.round(gx) + 1, Math.round(gy) + (up ? -1 : 0), 0x1A1A30, a);
        }
    }
    private static void magma(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float k, float a) {
        int cx = (x0 + x1) / 2;
        int vx = Math.round(cx - 46 * k);                                                         // the volcano, smoking
        for (int i = 0; i < Math.round(46 * k); i++) { int w = Math.round(i * 1.25f) + 3; rect(c, vx - w, hz - Math.round(46 * k) + i, vx + w, hz - Math.round(46 * k) + i + 1, mix(0x1A0A08, 0x2A0E08, i / (46f * k)), a); }
        Ritual.glowE(vx, hz - 46 * k, 10 * k, 5 * k, 0xFF7A2A, 0.8f * a);
        for (int s = 0; s < 14; s++) {
            float u = (t * 0.15f + s / 14f) % 1f; float sx = vx + (float) Math.sin(u * 4 + s) * 8 * k * u + u * 20 * k, sy = hz - 46 * k - u * 40 * k;
            Ritual.fillE(sx, sy, (3 + 7 * u) * k, (2 + 5 * u) * k, 0x3A2A2A, (1 - u) * 0.6f * a);
        }
        SlayerScenery.ridge(c, cx, x1, hz, 16 * k, 0.06f / k, 2f, 0x1A0A08, a);
        band(c, x0, x1, hz, y1, 0xFF7A1A, 0x8A1A06, a);                                           // the lava sea, rippling
        for (int y = hz; y < y1; y++) for (int x = x0; x < x1; x += 2) {
            float w = (float) Math.sin(x * 0.11f / k + t * 1.4f + y * 0.3f) * (float) Math.sin(x * 0.05f / k - t * 0.9f);
            if (w > 0.55f) rect(c, x, y, x + 2, y + 1, 0xFFD060, (w - 0.55f) * 1.6f * a);
            else if (w < -0.6f) rect(c, x, y, x + 2, y + 1, 0x3A0804, 0.5f * a);
        }
        for (int i = 0; i < 40; i++) {                                                           // ash drifting down
            float u = (t * (0.05f + 0.04f * rnd(i)) + rnd(i * 7)) % 1f;
            px(c, Math.round(x0 + rnd(i * 3) * (x1 - x0) + (float) Math.sin(t + i) * 3 * k), Math.round(y0 + u * (hz - y0)), 0x8A7A7A, (float) Math.sin(u * Math.PI) * 0.6f * a);
        }
    }
    private static void graveyard(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float k, float a) {
        int cx = (x0 + x1) / 2;
        stars(c, t, x0, y0, x1, hz - 10, a * 0.7f, 30);
        float mx = cx + 52 * k, my = y0 + 26 * k;                                                // the pumpkin moon
        Ritual.glowE(mx, my, 30 * k, 30 * k, 0xFF9A2E, 0.35f * a);
        Ritual.fillE(mx, my, 12 * k, 11 * k, 0xFFA040, a);
        for (int i = -1; i <= 1; i++) Ritual.ringE(mx + i * 5 * k, my, 4 * k, 11 * k, 0xD87010, 0.35f * a, -1.4f, 1.4f, 0, 0);
        SlayerScenery.ridge(c, x0, x1, hz, 9 * k, 0.04f / k, 5f, 0x140A20, a);
        tree(c, Math.round(x0 + 22 * k), hz, Math.round(44 * k), 0x0A0612, a);
        tree(c, Math.round(x1 - 30 * k), hz, Math.round(36 * k), 0x0A0612, a);
        band(c, x0, x1, hz, y1, 0x1A1028, 0x0A0612, a);
        for (int g = 0; g < 5; g++) {                                                            // gravestones
            int gx = Math.round(x0 + (14 + g * 46) * k) % Math.max(1, x1 - x0) + x0, gh = Math.round((8 + (g % 3) * 3) * k), gw = Math.round(6 * k);
            if (Math.abs(gx - cx) < 50 * k) continue;
            rect(c, gx - gw / 2, hz + 3 - gh, gx + gw / 2, hz + 3, 0x3A3448, a); c.disc(gx, hz + 3 - gh, gw / 2, Ritual.A(a, 0x3A3448));
            rect(c, gx - 1, hz - gh + 2, gx + 1, hz - gh + Math.round(6 * k), 0x241E30, a);
        }
        for (int f = 0; f < 3; f++) {                                                            // fog drifting
            float fx = x0 + ((t * 6 * k + f * 90 * k) % (x1 - x0 + 80 * k)) - 40 * k;
            Ritual.glowE(fx, hz + 4 * k + f * 3 * k, 46 * k, 6 * k, 0x9A8AC0, 0.25f * a);
        }
    }
    private static void aurora(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float k, float a) {
        stars(c, t, x0, y0, x1, hz, a * 0.8f, 40);
        for (int b = 0; b < 3; b++) {                                                            // the aurora: ribbons of light that sway
            int col = b == 1 ? 0x6AFFC8 : b == 2 ? 0x9A7AFF : 0x4AE0A0;
            for (int x = x0; x < x1; x++) {
                float yy = y0 + (20 + b * 9) * k + (float) Math.sin(x * 0.03f / k + t * 0.5f + b) * 8 * k + (float) Math.sin(x * 0.011f / k - t * 0.3f) * 6 * k;
                float h = (10 + 6 * (float) Math.sin(x * 0.05f / k + t + b * 2)) * k;
                for (int i = 0; i < h; i++) px(c, x, Math.round(yy + i), col, (1f - i / h) * 0.18f * a);
            }
        }
        SlayerScenery.ridge(c, x0, x1, hz, 14 * k, 0.035f / k, 1.5f, 0xC8E0F0, a);                 // snowy hills
        band(c, x0, x1, hz, y1, 0xE8F4FF, 0xA8C8E0, a);
        int[] px2 = {12, 30, 190, 206, 214};
        for (int i = 0; i < px2.length; i++) { int x = Math.round(x0 + px2[i] * k); if (x < x1) pine(c, x, hz + 2, Math.round((22 + (i % 2) * 8) * k), 0x0E3A2A, 0xE8F4FF, a); }
        for (int i = 0; i < 60; i++) {                                                           // snow
            float u = (t * (0.07f + 0.05f * rnd(i)) + rnd(i * 7)) % 1f;
            px(c, Math.round(x0 + rnd(i * 3) * (x1 - x0) + (float) Math.sin(t * 1.3f + i) * 4 * k), Math.round(y0 + u * (y1 - y0)), 0xFFFFFF, (float) Math.sin(u * Math.PI) * 0.85f * a);
        }
    }
    private static void isle(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float k, float a) {
        int cx = (x0 + x1) / 2;
        Ritual.glowE(cx, hz, 120 * k, 40 * k, 0xFF8A3A, 0.35f * a);
        for (int r = 0; r < 3; r++) {                                                            // floating rocks, bobbing
            float rx = cx + (r - 1) * 70 * k + (r == 1 ? 10 * k : 0), ry = y0 + (24 + r * 6) * k + (float) Math.sin(t * 0.8f + r) * 2 * k;
            Ritual.fillE(rx, ry, (12 - r * 2) * k, 4 * k, 0x3A1414, a);
            for (int i = 0; i < 6 * k; i++) rect(c, Math.round(rx - (6 - i) * k), Math.round(ry + i), Math.round(rx + (6 - i) * k), Math.round(ry + i + 1), 0x2A0E0E, a);
        }
        for (int tw = 0; tw < 2; tw++) {                                                         // the fortress towers
            int tx = Math.round(tw == 0 ? x0 + 24 * k : x1 - 34 * k), th = Math.round((34 + tw * 8) * k), tw2 = Math.round(10 * k);
            rect(c, tx, hz - th, tx + tw2, hz, 0x2A0A0E, a);
            for (int m = 0; m < 3; m++) rect(c, tx + m * tw2 / 3, hz - th - Math.round(3 * k), tx + m * tw2 / 3 + Math.round(2 * k), hz - th, 0x2A0A0E, a);
            float fl = 0.7f + 0.3f * (float) Math.sin(t * 7 + tw * 2);
            rect(c, tx + tw2 / 2 - 1, hz - th + Math.round(8 * k), tx + tw2 / 2 + 1, hz - th + Math.round(11 * k), 0xFFB040, fl * a);
        }
        SlayerScenery.ridge(c, x0, x1, hz, 8 * k, 0.07f / k, 4f, 0x240A0A, a);
        band(c, x0, x1, hz, y1, 0x3A1612, 0x1A0808, a);
        for (int i = 0; i < 30; i++) {                                                           // embers rising
            float u = (t * (0.12f + 0.08f * rnd(i)) + rnd(i * 5)) % 1f;
            px(c, Math.round(x0 + rnd(i * 9) * (x1 - x0) + (float) Math.sin(t * 2 + i) * 3 * k), Math.round(y1 - u * (y1 - y0)), i % 2 == 0 ? 0xFFB040 : 0xFF5A20, (float) Math.sin(u * Math.PI) * 0.8f * a);
        }
    }
    private static void dungeon(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float k, float a) {
        int bh = Math.max(4, Math.round(7 * k)), bw = Math.max(8, Math.round(15 * k));
        for (int y = y0; y < hz; y += bh) {                                                       // the stone wall
            int off = ((y - y0) / bh) % 2 == 0 ? 0 : bw / 2;
            for (int x = x0 - off; x < x1; x += bw) {
                int sh = mix(0x2A2A34, 0x3A3A46, rnd(x * 7 + y * 13));
                rect(c, Math.max(x0, x + 1), y + 1, Math.min(x1, x + bw), Math.min(hz, y + bh), sh, a);
                if (rnd(x * 3 + y) > 0.85f) rect(c, Math.max(x0, x + 2), y + 2, Math.min(x1, x + bw / 2), y + 3, 0x4A6A3A, 0.5f * a);      // moss
            }
        }
        band(c, x0, x1, hz, y1, 0x2A2A30, 0x141418, a);
        for (int s = 0; s < 2; s++) {                                                            // torches
            float tx = s == 0 ? x0 + 26 * k : x1 - 26 * k, ty = y0 + 40 * k;
            float fl = 0.75f + 0.25f * (float) Math.sin(t * 11 + s * 3) * (float) Math.sin(t * 6.7f + s);
            Ritual.glowE(tx, ty, 38 * k * fl, 34 * k * fl, 0xFF9A3A, 0.4f * a);
            rect(c, Math.round(tx - 1), Math.round(ty), Math.round(tx + 1), Math.round(ty + 9 * k), 0x5A3A1A, a);
            Ritual.fillE(tx, ty - 2 * k, 2.4f * k, 4 * k * fl, 0xFFB040, a); Ritual.fillE(tx, ty - 1.5f * k, 1.2f * k, 2.4f * k * fl, 0xFFF0A0, a);
        }
        for (int q = 0; q < 2; q++) {                                                            // cobwebs in the top corners
            int cx = q == 0 ? x0 : x1 - 1, dir = q == 0 ? 1 : -1;
            for (int i = 0; i < 5; i++) { float an = i * 0.35f; for (int j = 0; j < 22 * k; j++) px(c, Math.round(cx + dir * j * (float) Math.cos(an)), Math.round(y0 + j * (float) Math.sin(an)), 0xC8C8D0, 0.35f * a); }
            for (int ring = 1; ring < 4; ring++) for (int i = 0; i < 20; i++) { float an = i / 20f * 1.4f, rr = ring * 6 * k; px(c, Math.round(cx + dir * rr * (float) Math.cos(an)), Math.round(y0 + rr * (float) Math.sin(an)), 0xC8C8D0, 0.3f * a); }
        }
    }
    private static void studio(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float k, float a, int tint) {
        int[] cols = {tint, 0x3A9AFF, 0xFFC830, 0x5AD06A, 0xFF5A5A, 0xB06AFF};
        for (int i = 0; i < 24; i++) {                                                           // paint spatters on the wall
            float sx = x0 + rnd(i * 5) * (x1 - x0), sy = y0 + rnd(i * 9 + 1) * (hz - y0) * 0.9f, r = (1.5f + 3 * rnd(i * 3)) * k;
            int col = cols[i % cols.length];
            Ritual.fillE(sx, sy, r, r * 0.9f, col, 0.55f * a);
            for (int d = 0; d < 4; d++) { float an = rnd(i * 17 + d) * 6.28f; px(c, Math.round(sx + (float) Math.cos(an) * r * 1.8f), Math.round(sy + (float) Math.sin(an) * r * 1.8f), col, 0.5f * a); }
            if (i % 4 == 0) rect(c, Math.round(sx), Math.round(sy), Math.round(sx) + 1, Math.round(sy + r * 3), col, 0.4f * a);       // a drip
        }
        band(c, x0, x1, hz, y1, 0x9A6A3A, 0x5A3A1A, a);                                            // the wooden floor
        for (int y = hz + 2; y < y1; y += Math.max(2, Math.round(4 * k))) rect(c, x0, y, x1, y + 1, 0x4A2A12, 0.5f * a);
        int ex = Math.round(x1 - 34 * k), ey = hz;                                                // an easel with a little canvas
        for (int i = 0; i < 40 * k; i++) { px(c, Math.round(ex - i * 0.25f), ey - i, 0x5A3A1A, a); px(c, Math.round(ex + i * 0.25f), ey - i, 0x5A3A1A, a); }
        rect(c, Math.round(ex - 12 * k), Math.round(ey - 36 * k), Math.round(ex + 12 * k), Math.round(ey - 18 * k), 0xF8F4EA, a);
        Ritual.fillE(ex - 3 * k, ey - 27 * k, 6 * k, 4 * k, tint, 0.8f * a); Ritual.fillE(ex + 5 * k, ey - 24 * k, 3 * k, 3 * k, 0x3A9AFF, 0.7f * a);
        Ritual.glowE((x0 + x1) / 2f, y0, 90 * k, 50 * k, 0xFFFFFF, 0.25f * a);                     // light from a skylight
    }
    private static void hub(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float k, float a) {
        int cx = (x0 + x1) / 2;
        stars(c, t, x0, y0, x1, hz - 20, a * 0.8f, 36);
        float mx = cx - 50 * k, my = y0 + 22 * k;                                               // a crescent moon
        Ritual.glowE(mx, my, 22 * k, 22 * k, 0xC8D8FF, 0.3f * a);
        Ritual.fillE(mx, my, 8 * k, 8 * k, 0xF0F4FF, a); Ritual.fillE(mx + 3 * k, my - 2 * k, 7 * k, 7 * k, 0x2A2A60, a);
        SlayerScenery.ridge(c, x0, x1, hz, 12 * k, 0.03f / k, 0.5f, 0x2A2A50, a);
        for (int p = 0; p < 4; p++) {                                                            // ruined pillars
            int px0 = Math.round(p < 2 ? x0 + (14 + p * 22) * k : x1 - (20 + (p - 2) * 22) * k), ph = Math.round((22 + (p % 2) * 10) * k), pw = Math.round(6 * k);
            rect(c, px0, hz - ph, px0 + pw, hz, 0x3A3A6A, a); rect(c, px0 - 1, hz - ph - 2, px0 + pw + 1, hz - ph, 0x4A4A7A, a);
            rect(c, px0 + 1, hz - ph, px0 + 2, hz, 0x5A5A8A, 0.6f * a);
        }
        band(c, x0, x1, hz, y1, 0x2A4A3A, 0x142A20, a);                                             // dark grass
        for (int i = 0; i < 50; i++) { int x = x0 + (int) (rnd(i * 3) * (x1 - x0)), y = hz + (int) (rnd(i * 7) * (y1 - hz)); px(c, x, y, 0x4A7A5A, 0.6f * a); px(c, x, y - 1, 0x3A6A4A, 0.4f * a); }
    }
    private static void clearing(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float k, float a) {
        Ritual.glowE((x0 + x1) / 2f, hz, 100 * k, 30 * k, 0xFFB060, 0.4f * a);
        for (int i = 0; i < 9; i++) {                                                            // trees round the clearing
            int x = Math.round(i < 5 ? x0 + i * 13 * k : x1 - (i - 4) * 13 * k), h = Math.round((30 + 10 * rnd(i)) * k);
            if (x < x0 || x >= x1) continue;
            rect(c, x - 1, hz - h / 3, x + 2, hz + 2, 0x2A1A10, a);
            Ritual.fillE(x, hz - h * 0.6f, 9 * k, h * 0.45f, mix(0x1A3A1A, 0x2A4A22, rnd(i * 3)), a);
        }
        band(c, x0, x1, hz, y1, 0x3A6A2A, 0x1A3A14, a);
        for (int i = 0; i < 16; i++) {                                                           // fireflies
            float fx = x0 + rnd(i * 5) * (x1 - x0) + (float) Math.sin(t * 0.7f + i) * 8 * k, fy = y0 + (0.4f + 0.5f * rnd(i * 3)) * (y1 - y0) + (float) Math.cos(t * 0.9f + i * 2) * 5 * k;
            float on = 0.5f + 0.5f * (float) Math.sin(t * 2.5f + i * 1.9f);
            Ritual.glowE(fx, fy, 4 * k, 4 * k, 0xE8FF8A, 0.6f * on * a); px(c, Math.round(fx), Math.round(fy), 0xF8FFC8, on * a);
        }
    }
}
