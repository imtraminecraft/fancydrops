package dev.fancydrops;

/**
 * The place each Farming scene plays in, painted on the scene canvas inside a see-through rounded panel (the same kind of frame as the slayer places):
 * a sky, far and near layers that give it depth, and the ground the field lies on.
 * 0 a golden-hour wheat field (Rare Crops), 1 a harvest night with a big moon and lanterns (Harvest Feast), 2 the inside of a barn (Pest Drops),
 * 3 a bright morning field where a tractor mows (Harvest Bounty). Everything is mirrored round the middle, where the drop appears.
 */
final class FarmScenery {
    private FarmScenery() {}

    private static float rnd(int n) { return Ritual.rnd(n); }
    private static int mix(int a, int b, float f) { return Ritual.mixc(a, b, Math.max(0f, Math.min(1f, f))); }
    private static void px(Canvas c, int x, int y, int rgb, float a) { c.set(x, y, Ritual.A(a, rgb)); }
    private static void rect(Canvas c, int x0, int y0, int x1, int y1, int rgb, float a) { c.rect(x0, y0, x1, y1, Ritual.A(a, rgb)); }
    private static void disc(Canvas c, int cx, int cy, int r, int rgb, float a) { c.disc(cx, cy, r, Ritual.A(a, rgb)); }
    private static void glow(Canvas c, float cx, float cy, float rx, float ry, int rgb, float s) { Ritual.glowE(cx, cy, rx, ry, rgb, s); }

    /** Paints the whole place inside [x0,x1) x [y0,y1); `hz` is the horizon row, `a` the fade, `t` seconds for the little movements. */
    static void paint(Canvas c, int theme, float t, int x0, int y0, int x1, int y1, int hz, float a, int frameRgb) {
        int[] sky = switch (theme) {
            case 0 -> new int[]{0x16224E, 0xC8604A, 0xFFC878};
            case 1 -> new int[]{0x070618, 0x3A1A3C, 0xB8501E};
            case 2 -> new int[]{0x1A0F08, 0x3A2412, 0x4A2E16};
            default -> new int[]{0x3E94F0, 0x8CC8FF, 0xD8F0FF};
        };
        for (int y = y0; y < y1; y++) {                                                                   // the sky (or the back wall of the barn): three stops, dark above
            float f = (y - y0) / (float) Math.max(1, hz - y0);
            rect(c, x0, y, x1, y + 1, f < 0.55f ? mix(sky[0], sky[1], f / 0.55f) : mix(sky[1], sky[2], (f - 0.55f) / 0.45f), a);
        }
        SlayerScenery.corners(c, x0, y0, x1, y1, 7, frameRgb, a, false);
        switch (theme) {
            case 0 -> goldenHour(c, t, x0, y0, x1, y1, hz, a);
            case 1 -> harvestNight(c, t, x0, y0, x1, y1, hz, a);
            case 2 -> barn(c, t, x0, y0, x1, y1, hz, a);
            default -> morning(c, t, x0, y0, x1, y1, hz, a);
        }
        SlayerScenery.corners(c, x0, y0, x1, y1, 7, frameRgb, a, true);
        for (int y = 0; y < c.h; y++) for (int x = 0; x < c.w; x++) if (y < y0 || y >= y1 || x < x0 || x >= x1) c.px[y * c.w + x] = 0;       // nothing leaves the frame
    }

    // ---- shared pieces
    /** A field in perspective: rows of crop colour that converge on the horizon, lit stripe by stripe. */
    private static void rows(Canvas c, int x0, int x1, int hz, int y1, int ca, int cb, int ground, float t, float a, boolean far) {
        int cx = (x0 + x1) / 2;
        for (int y = hz; y < y1; y++) {
            float d = (y - hz) / (float) Math.max(1, y1 - hz);
            for (int x = x0; x < x1; x++) {
                float u = (x - cx) / (1f + d * 60f);                                                       // which row this column belongs to: they spread out towards the viewer
                boolean stripe = Math.floorMod((int) Math.floor(u * 0.5f), 2) == 0;
                float n = rnd(x * 7 + y * 131) * 0.16f - 0.06f;
                int col = stripe ? mix(ca, 0xFFFFFF, n) : mix(cb, 0x000000, -n * 0.7f);
                if ((x + y) % 5 == 0 && d > 0.2f) col = mix(col, ground, 0.35f);
                px(c, x, y, mix(0x000000, col, 0.55f + 0.45f * d), a);
            }
        }
    }
    private static void windmill(Canvas c, int x, int base, int h, float t, int body, int blade, float dir, float a) {
        for (int i = 0; i < h; i++) { int w = 3 + (h - i) / 5; rect(c, x - w / 2, base - h + i, x + (w + 1) / 2, base - h + i + 1, mix(body, 0x000000, 0.3f * (1f - i / (float) h)), a); }
        rect(c, x - 1, base - 3, x + 1, base, 0x000000, a * 0.6f);
        int hubY = base - h; float ang = t * 0.9f * dir;
        for (int k = 0; k < 4; k++) {
            float an = ang + k * 1.5708f;
            for (int i = 2; i < h * 0.62f; i++) { float cs = (float) Math.cos(an), sn = (float) Math.sin(an); px(c, Math.round(x + cs * i), Math.round(hubY + sn * i), blade, a); if (i > h * 0.2f) px(c, Math.round(x + cs * i - sn), Math.round(hubY + sn * i + cs), blade, a * 0.8f); }
        }
        disc(c, x, hubY, 1, 0x000000, a);
    }
    private static void bird(Canvas c, float x, float y, float t, int rgb, float a) {
        boolean up = ((int) (t * 7 + x * 0.1f) % 2) == 0;
        px(c, Math.round(x), Math.round(y), rgb, a); px(c, Math.round(x) - 1, Math.round(y) + (up ? -1 : 0), rgb, a); px(c, Math.round(x) + 1, Math.round(y) + (up ? -1 : 0), rgb, a);
        px(c, Math.round(x) - 2, Math.round(y) + (up ? -2 : 1), rgb, a); px(c, Math.round(x) + 2, Math.round(y) + (up ? -2 : 1), rgb, a);
    }
    private static void fence(Canvas c, int x0, int x1, int y, int rgb, float a) {
        for (int x = x0; x < x1; x += 7) rect(c, x, y - 5, x + 1, y, rgb, a);
        rect(c, x0, y - 4, x1, y - 3, rgb, a * 0.9f); rect(c, x0, y - 2, x1, y - 1, rgb, a * 0.9f);
    }
    private static void cloud(Canvas c, float cx, float cy, float w, int rgb, float a) {
        for (int i = 0; i < 5; i++) { float ox = (i - 2) * w * 0.2f, r = w * (0.14f + 0.06f * (float) Math.sin(i * 1.3f + 1.7f)); Ritual.fillE(cx + ox, cy - Math.abs(2 - i) * 1.2f, r * 1.5f, r * 0.8f, rgb, a); }
    }
    private static void motes(Canvas c, float t, int x0, int y0, int x1, int y1, int rgb, float a, int n) {
        for (int i = 0; i < n; i++) {
            float f = (t * (0.07f + 0.05f * rnd(i)) + rnd(i * 3)) % 1f, tw = 0.5f + 0.5f * (float) Math.sin(t * 3f + i * 2.1f);
            px(c, x0 + (int) (rnd(i * 5) * (x1 - x0) + Math.sin(t * 0.8f + i) * 3), y1 - 4 - (int) (f * (y1 - y0) * 0.8f), rgb, a * tw * (1f - f * 0.6f));
        }
    }

    // ---- 0: golden hour, a wheat field with two windmills and a low sun right behind the drop
    private static void goldenHour(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float a) {
        int cx = (x0 + x1) / 2, hw = (x1 - x0) / 2;
        for (int i = 0; i < 18; i++) px(c, x0 + 3 + (int) (rnd(i * 7) * (x1 - x0 - 6)), y0 + 2 + (int) (rnd(i * 13 + 1) * (hz - y0) * 0.35f), 0xFFFFFF, a * (0.3f + 0.4f * rnd(i)));       // the first stars, high up
        float ray = t * 0.12f;                                                                             // sun rays fanning up from the horizon, turning very slowly
        for (int k = 0; k < 9; k++) { float an = -1.5708f + (k - 4) * 0.30f + (float) Math.sin(ray + k) * 0.04f; for (int i = 6; i < (hz - y0) * 1.1f; i++) px(c, Math.round((float) (cx + Math.cos(an) * i)), Math.round((float) (hz - 3 + Math.sin(an) * i)), 0xFFE9A0, a * 0.07f * (1f - i / ((hz - y0) * 1.1f))); }
        glow(c, cx, hz - 2, hw * 0.9f, (hz - y0) * 0.8f, 0xFFB060, 0.55f * a);
        disc(c, cx, hz - 3, 9, 0xFFF0B0, a * 0.85f); disc(c, cx, hz - 3, 6, 0xFFFFE0, a);
        cloud(c, x0 + hw * 0.5f, y0 + (hz - y0) * 0.30f, 30, 0xF08A6A, a * 0.45f); cloud(c, x1 - hw * 0.45f, y0 + (hz - y0) * 0.22f, 24, 0xF08A6A, a * 0.4f);
        for (int i = 0; i < 3; i++) bird(c, x0 + ((t * (9 + i * 3) + i * 70) % (x1 - x0)), y0 + 12 + i * 7 + (float) Math.sin(t + i) * 2, t + i, 0x2A1A30, a * 0.8f);
        SlayerScenery.ridge(c, x0, x1, hz, (hz - y0) * 0.22f, 0.05f, 0.7f, 0x6A3A58, a);                       // far hills in haze
        SlayerScenery.ridge(c, x0, x1, hz + 1, (hz - y0) * 0.12f, 0.09f, 2.4f, 0x4A2A44, a);
        windmill(c, cx - hw * 2 / 3, hz + 1, 30, t, 0x3A2038, 0x2A1428, 1f, a); windmill(c, cx + hw * 2 / 3, hz + 1, 30, t, 0x3A2038, 0x2A1428, -1f, a);      // two windmills, mirrored, turning opposite ways
        rows(c, x0, x1, hz + 1, y1, 0xD8B040, 0xB88A28, 0x6A4A20, t, a, true);
        for (int i = 0; i < 2; i++) fence(c, i == 0 ? x0 + 2 : x1 - 26, i == 0 ? x0 + 26 : x1 - 2, hz + 6, 0x3A2412, a * 0.9f);
        for (int x = x0; x < x1; x++) { float f = 0.5f + 0.5f * (float) Math.sin(x * 0.08f - t * 1.6f); rect(c, x, hz + 2, x + 1, hz + 4, 0xFFE9A0, a * 0.10f * f); }           // wind rolling over the wheat
        motes(c, t, x0, hz - 10, x1, y1, 0xFFE9A0, a * 0.8f, 16);
    }

    // ---- 1: harvest night: a huge orange moon, strings of lanterns, glowing pumpkins and hay
    private static void harvestNight(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float a) {
        int cx = (x0 + x1) / 2, hw = (x1 - x0) / 2;
        for (int i = 0; i < 26; i++) px(c, x0 + 3 + (int) (rnd(i * 7) * (x1 - x0 - 6)), y0 + 2 + (int) (rnd(i * 13 + 1) * (hz - y0) * 0.6f), 0xFFF0D0, a * (0.25f + 0.5f * (0.5f + 0.5f * (float) Math.sin(t * 2 + i))));
        int my = y0 + (hz - y0) * 2 / 5;
        for (int g = 5; g >= 1; g--) disc(c, cx, my, 12 + g * 4, 0xFF8A30, a * 0.06f);
        disc(c, cx, my, 12, 0xFFB050, a * 0.95f); disc(c, cx - 3, my - 2, 8, 0xFFC878, a * 0.8f);
        for (int i = 0; i < 4; i++) disc(c, cx + (int) ((rnd(i * 3) - 0.5f) * 14), my + (int) ((rnd(i * 3 + 1) - 0.5f) * 12), 1 + i % 2, 0xE08A34, a * 0.6f);               // craters
        SlayerScenery.ridge(c, x0, x1, hz, (hz - y0) * 0.26f, 0.06f, 1.9f, 0x1A0C1E, a);
        for (int side = -1; side <= 1; side += 2) for (int i = 0; i < 4; i++) SlayerScenery.pine(c, cx + side * (hw / 3 + i * hw / 6), hz, 16 + (int) (rnd(i + side) * 8), 0x140A18, a);      // a dark tree line, mirrored
        for (int s = 0; s < 2; s++) {                                                                      // two strings of lanterns across the top, swaying
            int sy = y0 + 4 + s * 5;
            for (int i = 0; i <= 10; i++) {
                float f = i / 10f, x = x0 + 6 + f * (x1 - x0 - 12), y = sy + (float) Math.sin(f * 3.1416f) * 7 + (float) Math.sin(t * 1.3f + f * 6 + s) * 0.8f;
                px(c, Math.round(x), Math.round(y), 0x2A1A10, a);
                if (i % 2 == s % 2 && i > 0 && i < 10) { float fl = 0.75f + 0.25f * (float) Math.sin(t * 7 + i * 1.7f); glow(c, x, y + 3, 7, 7, 0xFFA030, 0.55f * fl * a); rect(c, Math.round(x) - 1, Math.round(y) + 1, Math.round(x) + 1, Math.round(y) + 4, 0xFFD070, a); }
            }
        }
        rows(c, x0, x1, hz + 1, y1, 0x6A4A22, 0x4A3018, 0x2A1A0C, t, a, true);
        for (int side = -1; side <= 1; side += 2) {
            int hx = cx + side * hw * 2 / 3, hb = hz + 12;                                                  // haystacks
            for (int i = 0; i < 12; i++) { int w = 3 + (int) (7 * Math.sin(i / 11f * 2.4f + 0.3f)); rect(c, hx - w, hb - i, hx + w, hb - i + 1, mix(0xC89A3A, 0x6A4A18, i / 14f), a); }
            for (int i = 0; i < 6; i++) px(c, hx - 5 + i * 2, hb - 3 - i % 3 * 2, 0xF0CE70, a * 0.8f);
            int jx = cx + side * hw / 3, jy = hz + 15;                                                      // a glowing jack-o'-lantern
            float fl = 0.7f + 0.3f * (float) Math.sin(t * 8 + side * 2);
            glow(c, jx, jy - 3, 14, 10, 0xFF8A20, 0.5f * fl * a);
            Ritual.fillE(jx, jy - 3, 5, 4, 0xE8782A, a); px(c, jx - 2, jy - 4, 0xFFE070, a * fl); px(c, jx + 2, jy - 4, 0xFFE070, a * fl); rect(c, jx - 2, jy - 2, jx + 3, jy - 1, 0xFFE070, a * fl); rect(c, jx, jy - 8, jx + 1, jy - 6, 0x3E6A24, a);
        }
        motes(c, t, x0, hz - 14, x1, y1, 0xFFB050, a, 20);
        for (int i = 0; i < 6; i++) { float f = (t * 0.3f + rnd(i) * 3) % 1f; px(c, x0 + 4 + (int) (rnd(i * 5) * (x1 - x0 - 8)), hz + 4 + (int) (rnd(i + 8) * (y1 - hz - 8)) , 0xCFFF60, a * (0.3f + 0.7f * (float) Math.abs(Math.sin(t * 2 + i * 1.7f))) * (0.5f + 0.5f * f)); }       // fireflies
    }

    // ---- 2: the inside of a barn: plank walls with light slits, a truss over the middle, hay and a lantern each side
    private static void barn(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float a) {
        int cx = (x0 + x1) / 2, hw = (x1 - x0) / 2;
        for (int x = x0; x < x1; x++) {                                                                    // vertical planks: each its own shade, dark seams between
            int pl = (x - x0) / 6; float sh = rnd(pl * 17 + 3) * 0.22f - 0.08f;
            int col = mix(0x5A3A1E, sh > 0 ? 0xFFFFFF : 0x000000, Math.abs(sh));
            rect(c, x, y0, x + 1, hz, (x - x0) % 6 == 0 ? 0x1E1208 : col, a);
            for (int k = 0; k < 3; k++) { int ky = y0 + 6 + (int) (rnd(pl * 5 + k) * (hz - y0 - 12)); if ((x - x0) % 6 == 2 + k % 2) px(c, x, ky, 0x2A180A, a * 0.8f); }          // knots in the wood
        }
        for (int s = -1; s <= 1; s += 2) for (int k = 0; k < 2; k++) {                                     // shafts of light through the gaps, drifting dust in them
            int sx = cx + s * (hw / 3 + k * hw / 3); float sway = (float) Math.sin(t * 0.3f + k + s) * 1.5f;
            for (int y = y0 + 2; y < hz + 6; y++) { float f = (y - y0) / (float) (hz - y0 + 6); int w = 2 + (int) (f * 4); for (int x = -w; x <= w; x++) px(c, Math.round(sx + sway + x + f * 14 * s), y, 0xFFE0A0, a * 0.10f * (1f - Math.abs(x) / (w + 1f))); }
        }
        int bw = 14, bh = hz - y0 - 8;                                                                      // the big doors in the middle, a little open, the evening outside
        rect(c, cx - bw - 2, y0 + 8, cx + bw + 2, hz, 0x2A1808, a);
        for (int y = y0 + 10; y < hz; y++) rect(c, cx - 1, y, cx + 1, y + 1, mix(0xFFB060, 0xFFE9B0, (y - y0) / (float) (hz - y0)), a * 0.95f);
        for (int s = -1; s <= 1; s += 2) { rect(c, s < 0 ? cx - bw : cx + 2, y0 + 10, s < 0 ? cx - 2 : cx + bw, hz, 0x6A4220, a); for (int i = 0; i < 4; i++) rect(c, s < 0 ? cx - bw : cx + 2, y0 + 14 + i * (bh / 4), s < 0 ? cx - 2 : cx + bw, y0 + 15 + i * (bh / 4), 0x3A2210, a); line(c, s < 0 ? cx - bw : cx + bw, y0 + 10, s < 0 ? cx - 2 : cx + 2, hz - 2, 0x3A2210, a); }
        glow(c, cx, hz - 4, 20, 22, 0xFFD890, 0.35f * a);
        rect(c, x0, y0 + 4, x1, y0 + 8, 0x2A1808, a); rect(c, x0, y0 + 4, x1, y0 + 5, 0x8A5A2C, a);        // the tie beam over the top
        for (int s = -1; s <= 1; s += 2) { for (int i = 0; i < 26; i++) { int bx = cx + s * (8 + i), by = y0 + 8 + i / 2; rect(c, bx, by, bx + 2, by + 3, 0x3A2210, a); } rect(c, cx + s * (hw - 6), y0 + 8, cx + s * (hw - 3), hz, 0x3A2210, a); }       // braces and posts, mirrored
        for (int s = -1; s <= 1; s += 2) {                                                                  // a lantern hanging from the beam each side
            int lx = cx + s * (hw * 2 / 3); float sw = (float) Math.sin(t * 1.4f + s) * 1.2f, fl = 0.8f + 0.2f * (float) Math.sin(t * 9 + s * 3);
            rect(c, lx, y0 + 8, lx + 1, y0 + 17, 0x1A1008, a);
            glow(c, lx + sw, y0 + 22, 22, 18, 0xFFA838, 0.55f * fl * a);
            rect(c, Math.round(lx + sw) - 2, y0 + 17, Math.round(lx + sw) + 3, y0 + 25, 0x3A2A1A, a); rect(c, Math.round(lx + sw) - 1, y0 + 18, Math.round(lx + sw) + 2, y0 + 24, 0xFFD070, a); px(c, Math.round(lx + sw), y0 + 21, 0xFFFFFF, a * fl);
        }
        for (int y = hz; y < y1; y++) {                                                                     // the plank floor in perspective: the boards widen towards the viewer
            float d = (y - hz) / (float) Math.max(1, y1 - hz), w = 6f + d * 14f;
            for (int x = x0; x < x1; x++) {
                float q = x - cx + w / 2f, fq = q - (float) Math.floor(q / w) * w; int idx = (int) Math.floor(q / w);
                int col = fq < 1f ? 0x1A0E06 : mix(0x6A4424, 0x8A5C30, rnd(idx * 7 + (int) (d * 5) * 3) * 0.6f);
                px(c, x, y, mix(0x000000, col, 0.55f + 0.45f * d), a);
            }
        }
        for (int i = 0; i < 26; i++) { int sx = x0 + 3 + (int) (rnd(i * 5) * (x1 - x0 - 6)), sy = hz + 2 + (int) (rnd(i * 5 + 1) * (y1 - hz - 4)); rect(c, sx, sy, sx + 3 + (int) (rnd(i) * 3), sy + 1, i % 2 == 0 ? 0xE8C860 : 0xC8A440, a * 0.85f); }
        for (int s = -1; s <= 1; s += 2) {                                                                  // hay bales stacked at each side, a sack of grain by one of them
            int bx = cx + s * (hw - 14), by = hz + 14;
            for (int k = 0; k < 2; k++) { int ox = bx - 9 + (k == 1 ? s * 5 : 0), oy = by - k * 8; rect(c, ox, oy - 8, ox + 18, oy, 0xD8B048, a); rect(c, ox, oy - 8, ox + 18, oy - 7, 0xF4D880, a); rect(c, ox, oy - 1, ox + 18, oy, 0x8A6A22, a); rect(c, ox + 5, oy - 8, ox + 6, oy, 0x6A4A18, a); rect(c, ox + 12, oy - 8, ox + 13, oy, 0x6A4A18, a); for (int j = 0; j < 8; j++) px(c, ox + 1 + j * 2, oy - 7 + j % 4 * 2, 0xF0D070, a * 0.7f); }
        }
        for (int i = 0; i < 18; i++) { float f = (t * (0.05f + 0.04f * rnd(i)) + rnd(i * 3)) % 1f; px(c, x0 + (int) (rnd(i * 5) * (x1 - x0)), y0 + 8 + (int) (f * (y1 - y0 - 12)), 0xFFE9B0, a * 0.5f * (0.5f + 0.5f * (float) Math.sin(t * 2 + i))); }       // dust drifting in the light
    }
    private static void line(Canvas c, int x0, int y0, int x1, int y1, int rgb, float a) {
        int n = Math.max(1, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
        for (int i = 0; i <= n; i++) px(c, x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n, rgb, a);
    }

    // ---- 3: a bright morning, mown stripes appearing behind a tractor
    private static void morning(Canvas c, float t, int x0, int y0, int x1, int y1, int hz, float a) {
        int cx = (x0 + x1) / 2, hw = (x1 - x0) / 2;
        glow(c, cx, y0 + 8, hw * 0.9f, (hz - y0) * 0.9f, 0xFFF8D0, 0.45f * a);
        disc(c, cx, y0 + (hz - y0) / 3, 7, 0xFFFFE8, a * 0.9f);
        for (int i = 0; i < 3; i++) cloud(c, x0 + ((t * (2 + i) + i * 60) % (x1 - x0 + 40)) - 20, y0 + 10 + i * 9, 28 + i * 6, 0xFFFFFF, a * 0.7f);
        SlayerScenery.ridge(c, x0, x1, hz, (hz - y0) * 0.30f, 0.05f, 0.9f, 0x6AA8B8, a);
        SlayerScenery.ridge(c, x0, x1, hz, (hz - y0) * 0.16f, 0.08f, 3.1f, 0x4E9A5A, a);
        for (int s = -1; s <= 1; s += 2) {                                                                  // a red barn and a silo each side, far off
            int bx = cx + s * (hw * 2 / 3), bb = hz + 1;
            rect(c, bx - 10, bb - 12, bx + 10, bb, 0xC83A32, a); rect(c, bx - 10, bb - 12, bx + 10, bb - 11, 0xE86A5A, a);
            for (int i = 0; i < 6; i++) rect(c, bx - 10 + i, bb - 18 + i, bx + 10 - i, bb - 17 + i, 0x8A2420, a);
            rect(c, bx - 3, bb - 8, bx + 3, bb, 0xF4E8D8, a); line(c, bx - 3, bb - 8, bx + 3, bb, 0xC83A32, a); line(c, bx + 3, bb - 8, bx - 3, bb, 0xC83A32, a);
            int sx = bx + s * 14; rect(c, sx - 3, bb - 20, sx + 3, bb, 0xB8C0CC, a); rect(c, sx - 3, bb - 20, sx - 1, bb, 0xE0E6EE, a); for (int i = 0; i < 3; i++) rect(c, sx - 3 + i, bb - 23 + i, sx + 3 - i, bb - 22 + i, 0x8A93A8, a);
        }
        rows(c, x0, x1, hz + 1, y1, 0x6CC444, 0x58AE34, 0x2E7A22, t, a, true);
        motes(c, t, x0, hz, x1, y1, 0xFFF8B0, a * 0.8f, 10);
    }
    /** A little green tractor, seen from the side, wheels turning, exhaust puffing. Drawn with its rear wheel at x. */
    static void tractor(Canvas c, float x, float y, float t, float a) {
        int xi = Math.round(x), yi = Math.round(y);
        rect(c, xi - 2, yi - 10, xi + 12, yi - 3, 0x2E8A3A, a); rect(c, xi - 2, yi - 10, xi + 12, yi - 9, 0x6ACB5A, a);                    // body and bonnet
        rect(c, xi + 8, yi - 8, xi + 16, yi - 3, 0x2E8A3A, a); rect(c, xi + 8, yi - 8, xi + 16, yi - 7, 0x6ACB5A, a);
        rect(c, xi - 3, yi - 17, xi + 5, yi - 10, 0x3AA848, a); rect(c, xi - 2, yi - 16, xi + 4, yi - 11, 0xBFE8FF, a * 0.9f); rect(c, xi - 3, yi - 18, xi + 6, yi - 17, 0x1E6A2A, a);    // the cab and its roof
        rect(c, xi + 13, yi - 13, xi + 14, yi - 8, 0x3A3A44, a);                                                                                   // exhaust pipe
        float pf = (t * 2f) % 1f; Ritual.glowE(xi + 13.5f - pf * 6, yi - 15 - pf * 7, 2 + pf * 3, 2 + pf * 3, 0xBFBFBF, 0.5f * (1f - pf) * a);
        for (int w = 0; w < 2; w++) {
            int wr = w == 0 ? 6 : 3, wx = w == 0 ? xi + 2 : xi + 14, wy = yi - wr;
            disc(c, wx, wy, wr, 0x1A1A20, a); disc(c, wx, wy, Math.max(1, wr - 2), 0x4A4A56, a); disc(c, wx, wy, Math.max(1, wr / 2), 0xE8B83A, a);
            for (int k = 0; k < 6; k++) { float an = t * 6f + k * 1.0472f; px(c, Math.round((float) (wx + Math.cos(an) * wr)), Math.round((float) (wy + Math.sin(an) * wr)), 0x6A6A76, a); }
        }
        rect(c, xi - 9, yi - 4, xi - 3, yi - 1, 0xB8B8C8, a); rect(c, xi - 9, yi - 4, xi - 3, yi - 3, 0xE8E8F4, a);                                // the mower deck behind it
    }
}
