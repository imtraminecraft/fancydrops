package dev.fancydrops;

/**
 * The slayer scenes, built like every other Showcase scene: a framed place of the boss's own, three beats (0.34 / 0.58 / 0.80 of the
 * opening) where something happens in the middle of it, then the drop bursts out. No boss hologram and no sword any more.
 *
 *   REVENANT   a graveyard under a sickly green moon: a grave in the middle, a rotting hand claws out of the soil on every beat,
 *              then the grave bursts open in a column of green souls.
 *   TARANTULA  the Spider's Den: a silk cocoon hangs from a thread, jolts and cracks on every beat (little spiders scatter), then splits.
 *   SVEN       a snowy pine forest under a huge full moon: a wolf on a rock howls on every beat, each howl rings across the moon,
 *              then a moonbeam strikes the rock and the wolf leaps away.
 *   VOIDGLOOM  the End: obsidian pillars with crystals; each beat a crystal fires a beam into a void orb over a portal frame,
 *              then the orb implodes and bursts.
 *   INFERNO    the Smoldering Tomb: three fire vents in a basalt altar erupt one by one, lava cracks spread, then a column of fire.
 *   BLOODFIEND the Rift's château under a blood moon: a coffin rattles on every beat (bats escape), then its lid flies off.
 *
 * The place is painted by ShowScenery (motifs BASE..BASE+5) so it gets the same frame, vignette and clipping as the others.
 */
final class SlayerShow {
    private SlayerShow() {}

    static final int BASE = 9;                                                          // ShowScenery / ShowScene motif of boss 0
    static final int REV = 0, TAR = 1, SVEN = 2, VOID = 3, BLAZE = 4, VAMP = 5;
    static final int[] ENERGY = {0x7CD060, 0xE03030, 0xC8D4E8, 0xD060FF, 0xFFB030, 0xD01030};
    static final int[][] SKY = {{0x050C08, 0x10241A, 0x2A4630}, {0x080505, 0x160C0A, 0x26140F}, {0x050E22, 0x142A4C, 0x3A5C84},
                                {0x04020A, 0x160828, 0x341850}, {0x120303, 0x420C06, 0x9A260A}, {0x080210, 0x260618, 0x640E1E}};
    private static final float[] BEAT = {0.34f, 0.58f, 0.80f};
    private static final int WHITE = 0xFFFFFF;

    private static float rnd(int n) { return Ritual.rnd(n); }
    private static float cl(float v) { return Ritual.cl(v); }
    private static float sm(float v) { return Ritual.sm(v); }
    private static float eo(float v) { return Ritual.eo(v); }
    private static int mix(int a, int b, float f) { return Ritual.mixc(a, b, cl(f)); }
    private static void dot(float x, float y, int rgb, float a) { Ritual.dot(x, y, rgb, a); }
    private static void px(Canvas c, int x, int y, int rgb, float a) { c.set(x, y, Ritual.A(a, rgb)); }
    private static void rect(Canvas c, int x0, int y0, int x1, int y1, int rgb, float a) { c.rect(x0, y0, x1, y1, Ritual.A(a, rgb)); }
    private static void box(float x0, float y0, float x1, float y1, int rgb, float a) { Ritual.cv.rect(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1), Ritual.A(a, rgb)); }
    private static float beat(float op, float O, int k) { return op - BEAT[k] * O; }

    // =================================================================== the places
    static void place(Canvas c, int boss, float t, int x0, int y0, int x1, int y1, int hz, float k, float a) {
        int cx = (x0 + x1) / 2, w = x1 - x0;
        switch (boss) {
            case REV -> {
                ShowScenery.stars(c, t, x0, y0, x1, hz - 14, a * 0.6f, 26);
                float mx = x0 + w * 0.22f, my = y0 + 24 * k;                                    // the green moon, a cloud across it
                Ritual.glowE(mx, my, 34 * k, 34 * k, 0x9AF070, 0.35f * a);
                Ritual.fillE(mx, my, 11 * k, 11 * k, 0xD8F0B0, a); Ritual.fillE(mx + 4 * k, my - 3 * k, 3 * k, 2.4f * k, 0xB0D090, 0.6f * a);
                Ritual.fillE(mx + 6 * k + (float) Math.sin(t * 0.3f) * 6 * k, my + 4 * k, 18 * k, 3 * k, 0x1A2A1E, 0.75f * a);
                SlayerScenery.ridge(c, x0, x1, hz, 10 * k, 0.04f / k, 2.2f, 0x0C1A10, a);
                for (int x = x0 + 2; x < x1; x += Math.max(3, Math.round(6 * k))) {             // the iron fence along the hill
                    int top = hz - Math.round((10 + (x / 6 % 2) * 2) * k);
                    rect(c, x, top, x + 1, hz, 0x060A08, a); px(c, x, top - 1, 0x060A08, a);
                }
                rect(c, x0, hz - Math.round(7 * k), x1, hz - Math.round(6 * k), 0x060A08, a); rect(c, x0, hz - Math.round(3 * k), x1, hz - Math.round(2 * k), 0x060A08, a);
                ShowScenery.tree(c, Math.round(x0 + 16 * k), hz + 2, Math.round(48 * k), 0x050806, a);
                ShowScenery.tree(c, Math.round(x1 - 20 * k), hz + 2, Math.round(40 * k), 0x050806, a);
                ShowScenery.band(c, x0, x1, hz, y1, 0x1E2A1A, 0x080E08, a);
                for (int g = 0; g < 4; g++) {                                                   // crosses and stones at the sides
                    int gx = Math.round(g < 2 ? x0 + (30 + g * 26) * k : x1 - (34 + (g - 2) * 26) * k), gh = Math.round((7 + g % 2 * 4) * k);
                    if (g % 2 == 0) { rect(c, gx - 1, hz + 4 - gh, gx + 1, hz + 4, 0x3A4038, a); rect(c, gx - Math.round(3 * k), hz + 4 - gh + Math.round(2 * k), gx + Math.round(3 * k), hz + 5 - gh + Math.round(2 * k), 0x3A4038, a); }
                    else { rect(c, gx - Math.round(3 * k), hz + 4 - gh, gx + Math.round(3 * k), hz + 4, 0x343A34, a); c.disc(gx, hz + 4 - gh, Math.round(3 * k), Ritual.A(a, 0x343A34)); }
                }
                for (int f = 0; f < 4; f++) {                                                   // green fog rolling
                    float fx = x0 + ((t * 7 * k + f * 70 * k) % (w + 90 * k)) - 45 * k;
                    Ritual.glowE(fx, hz + (3 + f * 3) * k, 50 * k, 6 * k, 0x6AA060, 0.22f * a);
                }
            }
            case TAR -> {
                Ritual.glowE(cx, hz - 10 * k, 90 * k, 40 * k, 0x5A1A10, 0.35f * a);               // a dim red glow deep in the cave
                for (int s = 0; s < 14; s++) {                                                  // stalactites
                    int sx = Math.round(x0 + (s + 0.5f) * w / 14f + (rnd(s) - 0.5f) * 8 * k), sl = Math.round((8 + rnd(s * 3) * 22) * k);
                    for (int i = 0; i < sl; i++) { int hw = Math.max(0, Math.round((1f - i / (float) sl) * 4 * k)); rect(c, sx - hw, y0 + i, sx + hw + 1, y0 + i + 1, mix(0x1E1410, 0x2E2018, i / (float) sl), a); }
                }
                for (int side = 0; side < 2; side++) {                                          // rock walls at the sides
                    for (int y = y0; y < hz; y++) { int ww = Math.round((22 + (float) Math.sin(y * 0.15f / k + side) * 5 + (y - y0) * 0.15f) * k); if (side == 0) rect(c, x0, y, x0 + ww, y + 1, 0x140C0A, a); else rect(c, x1 - ww, y, x1, y + 1, 0x140C0A, a); }
                }
                for (int e = 0; e < 5; e++) {                                                   // red eyes in the dark, blinking
                    float on = ((t * 0.5f + rnd(e * 9)) % 1f) < 0.85f ? 1f : 0f;
                    int ex = Math.round(x0 + (0.2f + 0.6f * rnd(e * 5 + 1)) * w), ey = Math.round(y0 + (0.35f + 0.3f * rnd(e * 7)) * (hz - y0));
                    if (Math.abs(ex - cx) < 26 * k) continue;
                    px(c, ex, ey, 0xFF2A1A, on * 0.9f * a); px(c, ex + 2, ey, 0xFF2A1A, on * 0.9f * a); px(c, ex + 4, ey + 1, 0xC01A10, on * 0.6f * a); px(c, ex - 2, ey + 1, 0xC01A10, on * 0.6f * a);
                }
                for (int q = 0; q < 2; q++) {                                                   // webs in the top corners
                    int ox = q == 0 ? x0 : x1 - 1, dir = q == 0 ? 1 : -1;
                    for (int i = 0; i < 6; i++) { float an = 0.1f + i * 0.28f; for (int j = 0; j < 34 * k; j++) px(c, Math.round(ox + dir * j * (float) Math.cos(an)), Math.round(y0 + j * (float) Math.sin(an)), 0xD0D0D8, 0.4f * a); }
                    for (int ring = 1; ring < 6; ring++) for (int i = 0; i < 24; i++) { float an = 0.1f + i / 24f * 1.4f, rr = ring * 6 * k; px(c, Math.round(ox + dir * rr * (float) Math.cos(an)), Math.round(y0 + rr * (float) Math.sin(an)), 0xD0D0D8, 0.3f * a); }
                }
                for (int s = 0; s < 3; s++) {                                                   // spiders dangling on threads
                    float sx = x0 + w * (s == 0 ? 0.2f : s == 1 ? 0.78f : 0.64f), len = (18 + 10 * s) * k + (float) Math.sin(t * (1.1f + s * 0.3f) + s) * 5 * k;
                    for (int i = 0; i < len; i++) px(c, Math.round(sx), y0 + i, 0xC8C8D0, 0.35f * a);
                    int sy = Math.round(y0 + len); c.disc(Math.round(sx), sy, Math.max(1, Math.round(1.6f * k)), Ritual.A(a, 0x1A0E0A));
                    for (int l = -1; l <= 1; l += 2) for (int j = 0; j < 3; j++) px(c, Math.round(sx + l * (2 + j) * k * 0.8f), sy - 1 + j, 0x1A0E0A, a);
                }
                ShowScenery.band(c, x0, x1, hz, y1, 0x2A1C16, 0x100806, a);
                for (int i = 0; i < 40; i++) { int x = x0 + (int) (rnd(i * 3) * w), y = hz + 1 + (int) (rnd(i * 7) * (y1 - hz - 1)); px(c, x, y, 0x3E2C22, 0.8f * a); }
            }
            case SVEN -> {
                ShowScenery.stars(c, t, x0, y0, x1, hz, a * 0.7f, 34);
                float mx = x0 + w * 0.72f, my = y0 + (hz - y0) * 0.35f, mr = 17 * k;               // the full moon
                Ritual.glowE(mx, my, mr * 2.2f, mr * 2.2f, 0xC8D8F0, 0.3f * a);
                Ritual.fillE(mx, my, mr, mr, 0xE8EEF6, a);
                Ritual.fillE(mx - 5 * k, my - 3 * k, 3.5f * k, 3 * k, 0xC8D0DC, 0.7f * a); Ritual.fillE(mx + 6 * k, my + 4 * k, 4.5f * k, 3.5f * k, 0xC8D0DC, 0.6f * a); Ritual.fillE(mx + 2 * k, my - 8 * k, 2 * k, 2 * k, 0xC8D0DC, 0.6f * a);
                SlayerScenery.ridge(c, x0, x1, hz, 14 * k, 0.035f / k, 1.2f, 0x9AB0C8, a);
                for (int i = 0; i < 7; i++) {                                                   // dark pines on both sides
                    int x = Math.round(i < 4 ? x0 + (6 + i * 15) * k : x1 - (8 + (i - 4) * 16) * k);
                    ShowScenery.pine(c, x, hz + 3, Math.round((30 + 10 * rnd(i * 3)) * k), 0x0A1A24, 0xDDE8F4, a);
                }
                ShowScenery.band(c, x0, x1, hz, y1, 0xDCE8F6, 0x8AA6C6, a);
                for (int i = 0; i < 70; i++) {                                                  // snow, blown sideways
                    float u = (t * (0.08f + 0.05f * rnd(i)) + rnd(i * 7)) % 1f;
                    px(c, Math.round(x0 + ((rnd(i * 3) * w + t * 14 * k) % w)), Math.round(y0 + u * (y1 - y0)), 0xFFFFFF, (float) Math.sin(u * Math.PI) * 0.85f * a);
                }
            }
            case VOID -> {
                ShowScenery.stars(c, t, x0, y0, x1, y1, a * 0.8f, 40);
                Ritual.glowE(x0 + w * 0.3f, y0 + 20 * k, 70 * k, 26 * k, 0x8A2AD0, 0.3f * a);       // nebula
                Ritual.glowE(x0 + w * 0.75f, y0 + 34 * k, 60 * k, 20 * k, 0x3A2AA0, 0.3f * a);
                for (int r = 0; r < 3; r++) {                                                   // little end stone islands floating
                    float ix = x0 + w * (0.25f + r * 0.27f), iy = y0 + (30 + (r % 2) * 14) * k + (float) Math.sin(t * 0.7f + r * 2) * 2 * k;
                    Ritual.fillE(ix, iy, 9 * k, 2.4f * k, 0xD8D49A, 0.8f * a);
                    for (int i = 0; i < 5 * k; i++) rect(c, Math.round(ix - (5 - i) * k), Math.round(iy + i), Math.round(ix + (5 - i) * k), Math.round(iy + i + 1), 0x8A8458, 0.8f * a);
                }
                for (int s = 0; s < 2; s++) {                                                   // obsidian pillars with their crystals
                    int px0 = Math.round(s == 0 ? x0 + w * 0.12f : x1 - w * 0.12f), ph = Math.round(44 * k), pw = Math.round(9 * k);
                    rect(c, px0 - pw / 2, hz - ph, px0 + pw / 2, hz + 2, 0x140A1E, a); rect(c, px0 - pw / 2, hz - ph, px0 - pw / 2 + 1, hz + 2, 0x2A1A3A, a);
                    float cy2 = hz - ph - 7 * k, sp = t * 2f + s;
                    Ritual.glowE(px0, cy2, 12 * k, 12 * k, 0xFF70D0, 0.45f * a);
                    for (int i = 0; i < 4; i++) { float an = sp + i * 1.5708f; Ritual.line(px0 + (float) Math.cos(an) * 4 * k, cy2 + (float) Math.sin(an) * 2 * k - 3 * k, px0 + (float) Math.cos(an + 1.5708f) * 4 * k, cy2 + (float) Math.sin(an + 1.5708f) * 2 * k + 3 * k, 0xFFB0E8, a); }
                    c.disc(px0, Math.round(cy2), Math.max(1, Math.round(1.6f * k)), Ritual.A(a, 0xFFE0F4));
                }
                ShowScenery.band(c, x0, x1, hz, y1, 0x9A9666, 0x3E3A26, a);
                for (int i = 0; i < 46; i++) { int x = x0 + (int) (rnd(i * 3) * w), y = hz + 1 + (int) (rnd(i * 7) * (y1 - hz - 1)); px(c, x, y, 0x6A6644, a); }
                for (int i = 0; i < 26; i++) {                                                  // void motes drifting up
                    float u = (t * (0.1f + 0.06f * rnd(i)) + rnd(i * 5)) % 1f;
                    px(c, Math.round(x0 + rnd(i * 9) * w + (float) Math.sin(t + i) * 3 * k), Math.round(y1 - u * (y1 - y0)), i % 3 == 0 ? 0xFFFFFF : 0xB060FF, (float) Math.sin(u * Math.PI) * 0.7f * a);
                }
            }
            case BLAZE -> {
                Ritual.glowE(cx, hz, 130 * k, 46 * k, 0xFF6A1A, 0.35f * a);
                for (int f = 0; f < 2; f++) {                                                   // lava falls pouring down the back wall
                    int fx = Math.round(x0 + w * (f == 0 ? 0.3f : 0.72f)), fw = Math.round(5 * k);
                    for (int y = y0; y < hz; y++) { float fl = (float) Math.sin(y * 0.4f / k - t * 6 + f); rect(c, fx - fw / 2, y, fx + fw / 2, y + 1, fl > 0.3f ? 0xFFC040 : 0xFF6A1A, 0.8f * a); }
                    Ritual.glowE(fx, hz, 14 * k, 5 * k, 0xFFB040, 0.5f * a);
                }
                SlayerScenery.ridge(c, x0, x1, hz, 12 * k, 0.05f / k, 3.3f, 0x1E0806, a);
                for (int s = 0; s < 2; s++) {                                                   // the tomb's braziers
                    int bx = Math.round(s == 0 ? x0 + w * 0.13f : x1 - w * 0.13f), bh = Math.round(34 * k), bw = Math.round(10 * k);
                    rect(c, bx - bw / 2, hz - bh, bx + bw / 2, hz + 2, 0x2A1210, a); rect(c, bx - bw / 2 - 2, hz - bh - Math.round(3 * k), bx + bw / 2 + 2, hz - bh, 0x3A1A14, a);
                    float fl = 0.8f + 0.2f * (float) Math.sin(t * 9 + s * 2);
                    Ritual.glowE(bx, hz - bh - 8 * k, 16 * k * fl, 14 * k * fl, 0xFF8A2A, 0.6f * a);
                    for (int j = 0; j < 14; j++) { float u = (t * 1.8f + rnd(j * 3 + s)) % 1f; dot(bx + (rnd(j * 5) - 0.5f) * bw + (float) Math.sin(u * 6 + j) * 1.5f, hz - bh - 3 * k - u * 16 * k, u < 0.3f ? 0xFFF0A0 : u < 0.6f ? 0xFFA030 : 0xC03A10, (1f - u) * a); }
                }
                ShowScenery.band(c, x0, x1, hz, y1, 0x2E1410, 0x100604, a);
                for (int i = 0; i < 9; i++) {                                                   // glowing cracks in the basalt
                    float sx = x0 + rnd(i * 7) * w, sy = hz + 2 + rnd(i * 3) * (y1 - hz - 4), an = rnd(i * 11) * 3.1f;
                    float pulse = 0.6f + 0.4f * (float) Math.sin(t * 2 + i);
                    Ritual.line(sx, sy, sx + (float) Math.cos(an) * 12 * k, sy + (float) Math.sin(an) * 3 * k, 0xFF7A1A, pulse * 0.8f * a);
                }
                for (int i = 0; i < 34; i++) {
                    float u = (t * (0.12f + 0.08f * rnd(i)) + rnd(i * 5)) % 1f;
                    px(c, Math.round(x0 + rnd(i * 9) * w + (float) Math.sin(t * 2 + i) * 3 * k), Math.round(y1 - u * (y1 - y0)), i % 2 == 0 ? 0xFFB040 : 0xFF5A20, (float) Math.sin(u * Math.PI) * 0.8f * a);
                }
            }
            default -> {
                ShowScenery.stars(c, t, x0, y0, x1, hz - 20, a * 0.5f, 18);
                float mx = x0 + w * 0.76f, my = y0 + 26 * k;                                    // the blood moon
                Ritual.glowE(mx, my, 40 * k, 40 * k, 0xE0203A, 0.4f * a);
                Ritual.fillE(mx, my, 15 * k, 15 * k, 0xE8404A, a); Ritual.fillE(mx - 4 * k, my + 3 * k, 4 * k, 3 * k, 0xC02A36, 0.7f * a);
                int chx = Math.round(x0 + w * 0.2f);                                            // the château on the hill, windows lit
                SlayerScenery.ridge(c, x0, x1, hz, 16 * k, 0.03f / k, 0.4f, 0x14040C, a);
                int[][] tw = {{-14, 30, 7}, {-4, 44, 9}, {8, 34, 7}, {16, 24, 6}};
                for (int[] q : tw) {
                    int tx = chx + Math.round(q[0] * k), th = Math.round(q[1] * k), ww = Math.round(q[2] * k);
                    rect(c, tx - ww / 2, hz - th, tx + ww / 2, hz, 0x0E0208, a);
                    for (int i = 0; i < ww; i++) rect(c, tx - ww / 2 + i, hz - th - Math.round((ww / 2f - Math.abs(i - ww / 2f)) * 1.4f), tx - ww / 2 + i + 1, hz - th, 0x0E0208, a);      // pointed roof
                    float fl = 0.7f + 0.3f * (float) Math.sin(t * 5 + q[0]);
                    rect(c, tx - 1, hz - th + Math.round(6 * k), tx + 1, hz - th + Math.round(9 * k), 0xFFB040, fl * a);
                }
                rect(c, chx - Math.round(18 * k), hz - Math.round(16 * k), chx + Math.round(20 * k), hz, 0x0E0208, a);
                ShowScenery.tree(c, Math.round(x1 - 18 * k), hz + 2, Math.round(42 * k), 0x0A0206, a);
                ShowScenery.band(c, x0, x1, hz, y1, 0x2A0A14, 0x0C0206, a);
                for (int f = 0; f < 3; f++) { float fx = x0 + ((t * 6 * k + f * 80 * k) % (w + 80 * k)) - 40 * k; Ritual.glowE(fx, hz + (4 + f * 3) * k, 48 * k, 6 * k, 0xA0203A, 0.25f * a); }      // red mist
                for (int b = 0; b < 4; b++) {                                                   // bats crossing the moon
                    float bx = x0 + ((t * (16 + b * 4) * k + b * 50 * k) % (w + 20 * k)) - 10 * k, by = y0 + (14 + b * 8) * k + (float) Math.sin(t * 2 + b) * 4 * k;
                    batTiny(c, Math.round(bx), Math.round(by), t * 12 + b, a);
                }
            }
        }
    }
    private static void batTiny(Canvas c, int x, int y, float flap, float a) {
        boolean up = Math.sin(flap) > 0;
        px(c, x, y, 0x080206, a); px(c, x - 1, y + (up ? -1 : 0), 0x080206, a); px(c, x + 1, y + (up ? -1 : 0), 0x080206, a); px(c, x - 2, y + (up ? -1 : 1), 0x080206, a); px(c, x + 2, y + (up ? -1 : 1), 0x080206, a);
    }

    // =================================================================== what happens on the beats
    /** The scene's middle at time t (op = time into the opening, p = time since the drop, a = visibility, rumble = the shake before the burst). */
    static void event(int boss, float t, float O, float op, float p, float cx, float gy, float sz, float a, float rumble, boolean rng, boolean nod) {
        float shake = (float) Math.sin(t * 70f) * rumble * 1.2f * sz;
        switch (boss) {
            case REV -> grave(t, O, op, p, cx + shake, gy, sz, a, rng, nod);
            case TAR -> cocoon(t, O, op, p, cx, gy, sz, a, rumble, rng, nod);
            case SVEN -> wolf(t, O, op, p, cx, gy, sz, a, rng, nod);
            case VOID -> orb(t, O, op, p, cx, gy, sz, a, rumble, rng, nod);
            case BLAZE -> vents(t, O, op, p, cx, gy, sz, a, rumble, rng, nod);
            default -> coffin(t, O, op, p, cx, gy, sz, a, rumble, rng, nod);
        }
    }

    // ---- Revenant: hands claw out of a grave
    private static void grave(float t, float O, float op, float p, float cx, float gy, float sz0, float a, boolean rng, boolean nod) {
        float sz = sz0 * 1.35f;
        float burst = p >= 0f && !nod ? p : -1f;
        float sx = cx, sy = gy - 22 * sz;                                                      // the headstone
        if (burst < 0f || burst < 0.15f) {
            float tilt = burst >= 0f ? burst * 6f : 0f;
            for (int y = 0; y < 20 * sz; y++) { float hw = 8 * sz - (y < 5 * sz ? (5 * sz - y) * (5 * sz - y) / (5 * sz) * 1.0f : 0f); box(sx - hw + tilt * (20 * sz - y) * 0.2f, sy + y, sx + hw + tilt * (20 * sz - y) * 0.2f, sy + y + 1, mix(0x5A6258, 0x3A4038, y / (20 * sz)), a); }
            box(sx - 1 * sz, sy + 5 * sz, sx + 1 * sz, sy + 13 * sz, 0x2A302A, a); box(sx - 4 * sz, sy + 7 * sz, sx + 4 * sz, sy + 9 * sz, 0x2A302A, a);
            int cracks = (beat(op, O, 0) > 0 ? 1 : 0) + (beat(op, O, 1) > 0 ? 1 : 0) + (beat(op, O, 2) > 0 ? 1 : 0);
            for (int c = 0; c < cracks; c++) for (int i = 0; i < 8 * sz; i++) dot(sx - 4 * sz + c * 4 * sz + (float) Math.sin(i * 1.3f + c) * 1.5f, sy + 2 * sz + i, 0x1A1E1A, a);
        }
        Ritual.fillE(cx, gy, 26 * sz, 6 * sz, 0x2A1E12, a);                                  // the grave's mound of soil
        Ritual.fillE(cx, gy - 1, 24 * sz, 4.5f * sz, 0x3E2C1A, a);
        for (int i = 0; i < 12; i++) dot(cx + (rnd(i * 3) - 0.5f) * 44 * sz, gy + (rnd(i * 7) - 0.6f) * 6 * sz, 0x5A4428, a);
        float[] hx = {-14, 14, 0};
        for (int k = 0; k < 3; k++) {
            float b = beat(op, O, k); if (b < 0f) continue;
            float x = cx + hx[k] * sz, up = eo(b / 0.35f) * (burst >= 0 ? 1f - cl(burst / 0.4f) : nod && p > 0 ? 1f - cl(p / 0.6f) : 1f), sway = (float) Math.sin(t * 3 + k) * 1.2f * sz;
            if (b < 0.6f) { Ritual.glowE(x, gy, 14 * sz, 6 * sz, 0x7CD060, 0.6f * (1f - b / 0.6f) * a); for (int j = 0; j < 10; j++) { float vx = (rnd(k * 30 + j) - 0.5f) * 60 * sz, vy = -(30 + rnd(k * 50 + j) * 40) * sz; dot(x + vx * b, gy + vy * b + 160 * sz * b * b, 0x4A3420, (1f - b / 0.6f) * a); } }
            if (up <= 0.01f) continue;
            float top = gy - 16 * sz * up;                                                     // the arm and the clawing hand
            Ritual.stroke(x, gy, x + sway, top, 2.2f * sz, 0x5A7A4A, a); Ritual.stroke(x + 0.6f * sz, gy, x + sway + 0.6f * sz, top, 0.8f * sz, 0x7A9A5A, a);
            for (int f = 0; f < 4; f++) { float an = -2.4f + f * 0.45f + (float) Math.sin(t * 5 + f + k) * 0.15f; Ritual.stroke(x + sway, top, x + sway + (float) Math.cos(an) * 5 * sz, top + (float) Math.sin(an) * 5 * sz, 0.8f * sz, 0x6A8A52, a); }
            Ritual.stroke(x + sway - 1.5f * sz, top + 2 * sz, x + sway - 4 * sz, top - 1 * sz, 0.8f * sz, 0x6A8A52, a);
        }
        if (burst >= 0f && burst < 1.6f) {                                                     // the grave bursts: soil, a column of souls
            float f = burst / 1.6f;
            Ritual.glowE(cx, gy - 30 * sz * eo(f), 18 * sz, 46 * sz * eo(burst / 0.3f), 0x9AF070, 0.7f * (1f - f) * a);
            for (int j = 0; j < (rng ? 40 : 24); j++) { float vx = (rnd(j * 3) - 0.5f) * 110 * sz, vy = -(50 + rnd(j * 5) * 70) * sz; dot(cx + vx * burst, gy + vy * burst + 150 * sz * burst * burst, j % 3 == 0 ? 0x6A5030 : 0x3E2C1A, (1f - f) * a); }
            for (int j = 0; j < (rng ? 14 : 8); j++) { float u = cl(burst * 1.2f - rnd(j) * 0.3f), x = cx + (float) Math.sin(u * 8 + j) * 8 * sz, y = gy - u * 80 * sz; Ritual.glowE(x, y, 4 * sz, 4 * sz, 0x9AF070, 0.5f * (1f - u) * a); dot(x, y, 0xE0FFD0, (1f - u) * a); }
        }
        if (nod && p >= 0f) Ritual.glowE(cx, gy - 4, 20 * sz, 8 * sz, 0x5A6A5A, 0.4f * (1f - cl(p / 0.8f)) * a);
    }

    // ---- Tarantula: a cocoon jolts and splits
    private static void cocoon(float t, float O, float op, float p, float cx, float gy, float sz, float a, float rumble, boolean rng, boolean nod) {
        int[] b = ShowScenery.box(cx, gy, sz);
        float split = p >= 0f && !nod ? p : -1f;
        float jolt = 0f; for (int k = 0; k < 3; k++) { float bb = beat(op, O, k); if (bb >= 0f && bb < 0.8f) jolt += (float) Math.sin(bb * 18f) * (1f - bb / 0.8f) * 0.25f; }
        float swing = (float) Math.sin(t * 1.3f) * 0.06f + jolt + (float) Math.sin(t * 60f) * rumble * 0.05f;
        float ay = b[1], ccy = gy - 30 * sz, len = ccy - ay - 14 * sz, ex = cx + (float) Math.sin(swing) * len, ey = ay + (float) Math.cos(swing) * len;
        Ritual.line(cx, ay, ex, ey, 0xE0E0E8, 0.8f * a);                                       // the thread
        Ritual.fillE(cx, gy + 1, 20 * sz, 3 * sz, 0x000000, 0.35f * a);                       // its shadow on the ground
        if (split < 0f) {
            float pulse = 1f; for (int k = 0; k < 3; k++) { float bb = beat(op, O, k); if (bb >= 0f && bb < 0.3f) pulse += 0.12f * (1f - bb / 0.3f); }
            float rx = 9 * sz * pulse, ry = 15 * sz * pulse, ccx = ex, ccy2 = ey + ry;
            Ritual.fillE(ccx, ccy2, rx, ry, 0xC8C4C0, a);
            Ritual.fillE(ccx - rx * 0.3f, ccy2 - ry * 0.2f, rx * 0.5f, ry * 0.7f, 0xE8E4E0, 0.7f * a);
            for (int i = 0; i < 7; i++) { float yy = ccy2 - ry + (i + 0.5f) * 2 * ry / 7f, hw = rx * (float) Math.sqrt(Math.max(0, 1 - Math.pow((yy - ccy2) / ry, 2))); Ritual.line(ccx - hw, yy - 1, ccx + hw, yy + 1, 0x9A9690, 0.6f * a); }      // the silk wraps
            int cracks = 0; for (int k = 0; k < 3; k++) if (beat(op, O, k) >= 0f) cracks++;
            for (int c = 0; c < cracks; c++) { float x0 = ccx + (c - 1) * 3 * sz; for (int i = 0; i < 4; i++) Ritual.line(x0 + (i % 2 == 0 ? -1 : 1) * sz, ccy2 - ry * 0.6f + i * ry * 0.35f, x0 + (i % 2 == 0 ? 1 : -1) * sz, ccy2 - ry * 0.6f + (i + 1) * ry * 0.35f, 0xFF3A2A, a); Ritual.glowE(x0, ccy2, 4 * sz, ry * 0.6f, 0xFF3A2A, 0.35f * a); }
            if (nod && p >= 0f) Ritual.glowE(ccx, ccy2, rx * 2, ry * 1.3f, 0x6A6060, 0.3f * (1f - cl(p / 0.8f)) * a);
        } else {                                                                               // split: the halves fall apart, silk strands fly
            float f = cl(split / 1.4f), ccx = ex, ccy2 = ey + 15 * sz;
            for (int h = -1; h <= 1; h += 2) { float hx = ccx + h * (3 + 40 * eo(split / 0.8f)) * sz, hy = ccy2 + 120 * sz * split * split * 0.3f; Ritual.fillE(hx, hy, 5 * sz, 12 * sz, 0xC8C4C0, (1f - f) * a); }
            Ritual.glowE(ccx, ccy2, 30 * sz * (0.5f + eo(split / 0.3f)), 30 * sz * (0.5f + eo(split / 0.3f)), 0xFF4030, 0.6f * (1f - cl(split / 0.7f)) * a);
            for (int j = 0; j < (rng ? 22 : 14); j++) { float an = j * 6.2832f / (rng ? 22 : 14) + rnd(j) * 0.3f, r = 60 * sz * eo(split / 0.6f); Ritual.line(ccx, ccy2, ccx + (float) Math.cos(an) * r, ccy2 + (float) Math.sin(an) * r * 0.7f, 0xF0F0F4, 0.7f * (1f - f) * a); }
        }
        for (int k = 0; k < 3; k++) {                                                          // little spiders scatter on each beat
            float bb = beat(op, O, k); if (bb < 0f || bb > 1.4f) continue;
            for (int s = 0; s < 4; s++) { float dir = (s % 2 == 0 ? 1 : -1) * (0.6f + rnd(k * 10 + s)), x = cx + dir * 60 * sz * eo(bb / 1.4f), y = gy + (rnd(k * 7 + s) - 0.3f) * 6 * sz;
                float al = (1f - cl((bb - 1f) / 0.4f)) * a; Ritual.fillE(x, y, 1.6f * sz, 1.1f * sz, 0x1A0A08, al); for (int l = 0; l < 3; l++) { float lg = (float) Math.sin(t * 30 + l * 2 + s) * 0.8f * sz; dot(x - 2 * sz, y - 1 + l + lg * 0.3f, 0x1A0A08, al); dot(x + 2 * sz, y - 1 + l - lg * 0.3f, 0x1A0A08, al); } dot(x + Math.signum(dir) * 1.5f * sz, y - 0.5f, 0xFF2A1A, al); }
        }
    }

    // ---- Sven: a wolf howls at the moon
    private static void wolf(float t, float O, float op, float p, float cx, float gy, float sz0, float a, boolean rng, boolean nod) {
        int[] b = ShowScenery.box(cx, gy, sz0); float mx = b[0] + (b[2] - b[0]) * 0.72f, my = b[1] + (b[4] - b[1]) * 0.35f, sz = sz0 * 1.4f;
        Ritual.fillE(cx, gy - 4 * sz, 26 * sz, 10 * sz, 0x2A3442, a);                         // the rock
        Ritual.fillE(cx - 6 * sz, gy - 8 * sz, 16 * sz, 8 * sz, 0x3A4656, a);
        Ritual.fillE(cx - 4 * sz, gy - 13 * sz, 8 * sz, 2 * sz, 0xE8F0F8, 0.9f * a);           // snow on top
        float leap = p >= 0f && !nod ? p : -1f;
        float howl = 0f; for (int k = 0; k < 3; k++) { float bb = beat(op, O, k); if (bb >= 0f && bb < 0.9f) howl = Math.max(howl, (float) Math.sin(Math.PI * cl(bb / 0.9f))); }
        float wx = cx - 4 * sz, wy = gy - 14 * sz, wa = a;
        if (leap >= 0f) { wx += 70 * sz * leap; wy -= 40 * sz * leap - 60 * sz * leap * leap; wa = a * (1f - cl(leap / 0.8f)); }
        if (nod && p > 0f) wa = a * (1f - cl(p / 1f));
        if (wa > 0.02f) {                                                                      // the wolf, side on, head lifting to howl
            int col = 0x0A121C;
            Ritual.fillE(wx, wy - 5 * sz, 9 * sz, 4 * sz, col, wa);                            // body
            for (int l = 0; l < 4; l++) Ritual.stroke(wx - 6 * sz + l * 4 * sz, wy - 3 * sz, wx - 6.5f * sz + l * 4 * sz, wy, 1.4f * sz, col, wa);
            float hx = wx + 8 * sz, hy = wy - 8 * sz - howl * 3 * sz, an = -0.3f - howl * 0.9f;
            Ritual.stroke(wx + 5 * sz, wy - 6 * sz, hx, hy, 3 * sz, col, wa);                  // neck and head
            Ritual.stroke(hx, hy, hx + (float) Math.cos(an) * 6 * sz, hy + (float) Math.sin(an) * 6 * sz, 2 * sz, col, wa);        // the muzzle, raised
            Ritual.stroke(hx - 1 * sz, hy - 1 * sz, hx - 1.5f * sz, hy - 4 * sz, 1 * sz, col, wa);  // ear
            Ritual.stroke(wx - 8 * sz, wy - 6 * sz, wx - 13 * sz, wy - 9 * sz + (float) Math.sin(t * 3) * sz, 1.6f * sz, col, wa);      // tail
            Ritual.line(wx - 8 * sz, wy - 9 * sz, wx + 6 * sz, wy - 9.5f * sz, 0x6A8AB0, 0.5f * wa);                                 // moonlight on its back
        }
        for (int k = 0; k < 3; k++) {                                                          // every howl rings out across the moon
            float bb = beat(op, O, k); if (bb < 0f || bb > 1.4f) continue;
            float u = bb / 1.4f, r = (3 + 17 * eo(u)) * sz;
            Ritual.ringE(mx, my, r, r, 0xE0ECFF, (1f - u) * 0.7f * a, 0, 6.2832f, 0, 0);
            Ritual.glowE(mx, my, 16 * sz, 16 * sz, 0xE8F0FF, 0.35f * (1f - u) * a);
            float hx2 = cx + 4 * sz, hy2 = gy - 26 * sz; for (int q = 1; q <= 3; q++) Ritual.ringE(hx2, hy2, (3 + q * 4 + 10 * u) * sz, (3 + q * 4 + 10 * u) * sz, 0xE0ECFF, (1f - u) * 0.5f * a, -1.9f, -0.5f, 0, 0);      // the howl from the muzzle
            for (int g = 0; g < 6; g++) { float gx = cx - 100 * sz + ((bb * 160 + g * 37) % 200) * sz, gy2 = gy - (10 + g * 9) * sz; Ritual.line(gx, gy2, gx + 14 * sz, gy2, 0xFFFFFF, (1f - u) * 0.5f * a); }      // a gust of snow
        }
        if (leap >= 0f && leap < 1.5f) {                                                       // the moonbeam strikes the rock
            float f = leap / 1.5f, wbeam = 10 * sz * (1f + eo(leap / 0.3f)) * (1f - f * 0.5f);
            for (int y = b[1]; y < gy; y++) { float u = (y - b[1]) / (float) (gy - b[1]); box(cx - wbeam * (0.6f + 0.4f * u), y, cx + wbeam * (0.6f + 0.4f * u), y + 1, 0xE8F0FF, 0.35f * (1f - f) * a); }
            Ritual.glowE(cx, gy - 10 * sz, 40 * sz, 16 * sz, 0xFFFFFF, 0.7f * (1f - f) * a);
            for (int j = 0; j < (rng ? 30 : 18); j++) { float vx = (rnd(j * 3) - 0.5f) * 120 * sz, vy = -(30 + rnd(j * 5) * 60) * sz; dot(cx + vx * leap, gy - 10 * sz + vy * leap + 120 * sz * leap * leap, 0xFFFFFF, (1f - f) * a); }
        }
    }

    // ---- Voidgloom: crystals beam into a void orb
    private static void orb(float t, float O, float op, float p, float cx, float gy, float sz, float a, float rumble, boolean rng, boolean nod) {
        int[] b = ShowScenery.box(cx, gy, sz); float w = b[2] - b[0], k = sz / (Ritual.SMALLER * Ritual.K * Ritual.SCENE_K);
        float[] crX = {b[0] + w * 0.12f, b[2] - w * 0.12f}; float crY = b[4] - 44 * k - 7 * k;
        for (int i = 0; i < 12; i++) {                                                          // the portal frame on the ground
            float an = i * 6.2832f / 12, x = cx + (float) Math.cos(an) * 26 * sz, y = gy + (float) Math.sin(an) * 6 * sz;
            box(x - 2.5f * sz, y - 2 * sz, x + 2.5f * sz, y + 1.5f * sz, 0x2A4A3A, a); box(x - 2.5f * sz, y - 2 * sz, x + 2.5f * sz, y - 1.2f * sz, 0x8AB89A, a);
            if (i % 2 == 0) dot(x, y - 0.5f * sz, 0x3AD08A, a);
        }
        Ritual.fillE(cx, gy, 22 * sz, 4.5f * sz, 0x05020A, a);
        float imp = p >= 0f && !nod ? p : -1f;
        float grow = 1f; int lit = 0; for (int k2 = 0; k2 < 3; k2++) if (beat(op, O, k2) >= 0f) { grow += 0.35f; lit++; }
        float ox = cx + (float) Math.sin(t * 50f) * rumble * 1.5f * sz, oy = gy - 32 * sz + (float) Math.sin(t * 1.4f) * 2 * sz, r = 6 * sz * grow;
        if (imp >= 0f) r *= imp < 0.18f ? 1f - imp / 0.18f * 0.9f : 0f;
        if (nod && p > 0f) r *= 1f - cl(p / 0.8f);
        for (int k2 = 0; k2 < 3; k2++) {                                                       // the beams: left crystal, right crystal, then both
            float bb = beat(op, O, k2); if (bb < 0f || (imp >= 0f && imp > 0.2f)) continue;
            float on = bb < 0.12f ? bb / 0.12f : 1f - cl((bb - 0.5f) / 0.6f) * 0.6f;
            for (int s = 0; s < 2; s++) { if (k2 == 0 && s == 1 || k2 == 1 && s == 0) continue;
                Ritual.stroke(crX[s], crY, ox, oy, 2.2f * sz, 0xD060FF, 0.45f * on * a); Ritual.stroke(crX[s], crY, ox, oy, 0.7f * sz, 0xFFE0FF, on * a);
                Ritual.glowE(crX[s], crY, 10 * sz, 10 * sz, 0xFF90E0, 0.6f * on * a); }
        }
        if (r > 0.3f) {
            Ritual.glowE(ox, oy, r * 3f, r * 3f, 0x9A30E0, (0.4f + 0.12f * lit) * a);
            Ritual.fillE(ox, oy, r, r, 0x05020A, a);
            Ritual.ringE(ox, oy, r, r, 0xD060FF, 0.9f * a, t * 3, t * 3 + 4.5f, 0, 0);
            for (int j = 0; j < 16; j++) { float life = 0.9f, tau = (t + rnd(j * 11) * life) % life, f = tau / life, an = rnd(j * 3) * 6.2832f + f * 2.5f, rr = r * (1f + 3f * (1f - f)); dot(ox + (float) Math.cos(an) * rr, oy + (float) Math.sin(an) * rr, j % 4 == 0 ? WHITE : 0xC060FF, f * a); }
        }
        if (imp >= 0.18f && imp < 1.8f) {                                                      // the burst after the implosion
            float u = (imp - 0.18f) / 1.6f;
            Ritual.glowE(ox, oy, 60 * sz * eo(u * 2), 60 * sz * eo(u * 2), 0xE0A0FF, 0.8f * (1f - u) * a);
            Ritual.ringE(ox, oy, 90 * sz * eo(u), 90 * sz * eo(u) * 0.6f, 0xF0D0FF, (1f - u) * a, 0, 6.2832f, 0, 0);
            for (int j = 0; j < (rng ? 16 : 10); j++) { float an = rnd(j * 7) * 6.2832f, d = 80 * sz * eo(u) * (0.5f + rnd(j)), x = ox + (float) Math.cos(an) * d, y = oy + (float) Math.sin(an) * d * 0.7f, s0 = 2 * sz * (1f - u);
                box(x - s0, y - s0, x + s0, y + s0, 0x1A0A2A, (1f - u) * a); dot(x, y, 0xD060FF, (1f - u) * a); }
        }
    }

    // ---- Inferno: three fire vents erupt
    private static void vents(float t, float O, float op, float p, float cx, float gy, float sz, float a, float rumble, boolean rng, boolean nod) {
        Ritual.fillE(cx, gy + 1, 40 * sz, 8 * sz, 0x1A0A08, a);                               // the altar
        Ritual.fillE(cx, gy - 1, 36 * sz, 6 * sz, 0x2E1612, a);
        float[] vx = {-22, 22, 0}; float[] vy = {1, 1, -1};
        int hits = 0; for (int k = 0; k < 3; k++) if (beat(op, O, k) >= 0f) hits++;
        for (int c = 0; c < hits * 3; c++) {                                                   // lava cracks spreading over the altar
            int v = c / 3; float an = rnd(c * 13) * 6.2832f, len = (8 + 10 * rnd(c * 7)) * sz;
            float x0 = cx + vx[v] * sz, y0 = gy + vy[v] * sz;
            Ritual.line(x0, y0, x0 + (float) Math.cos(an) * len, y0 + (float) Math.sin(an) * len * 0.3f, 0xFF8A2A, (0.6f + 0.4f * (float) Math.sin(t * 4 + c)) * a);
        }
        for (int k = 0; k < 3; k++) {
            float x = cx + vx[k] * sz, y = gy + vy[k] * sz, bb = beat(op, O, k);
            Ritual.fillE(x, y, 5 * sz, 2 * sz, 0x0A0402, a);
            if (bb < 0f) { if (rnd((int) (t * 10) + k) > 0.6f) dot(x, y - 2 * sz, 0x6A6060, 0.6f * a); continue; }
            Ritual.fillE(x, y, 4 * sz, 1.4f * sz, 0xFF6A1A, a); Ritual.glowE(x, y, 10 * sz, 5 * sz, 0xFF7A2A, 0.6f * a);
            if (bb < 1.0f) {                                                                   // the eruption: a pillar of flame
                float h = 50 * sz * (float) Math.sin(Math.PI * cl(bb / 1.0f)) * (k == 2 ? 1.2f : 1f);
                for (int j = 0; j < 26; j++) { float u = rnd(j * 3 + k * 50), yy = y - u * h, ww = (1f - u) * 5 * sz + 1; dot(x + (rnd(j * 7 + (int) (t * 20)) - 0.5f) * ww * 2, yy, u < 0.3f ? 0xFFF0A0 : u < 0.6f ? 0xFFA030 : 0xD0401A, a * (1f - u * 0.5f)); }
                Ritual.glowE(x, y - h * 0.5f, 8 * sz, h * 0.6f + 1, 0xFF8A2A, 0.6f * a);
            } else for (int j = 0; j < 4; j++) { float u = (t * 1.5f + rnd(j * 5 + k)) % 1f; dot(x + (rnd(j) - 0.5f) * 4 * sz, y - u * 10 * sz, u < 0.5f ? 0xFFA030 : 0xC03A10, (1f - u) * a); }
        }
        if (p >= 0f && !nod && p < 1.6f) {                                                     // the column of fire
            float f = p / 1.6f, h = 110 * sz * eo(p / 0.3f), ww = 14 * sz * (1f - f * 0.6f);
            for (int y = Math.round(gy - h); y < gy; y++) { float u = (gy - y) / Math.max(1f, h), wv = (float) Math.sin(y * 0.3f - t * 20) * 2 * sz; box(cx - ww * (1f - u * 0.5f) + wv, y, cx + ww * (1f - u * 0.5f) + wv, y + 1, u < 0.3f ? 0xFFF0A0 : u < 0.65f ? 0xFFA030 : 0xD0401A, 0.75f * (1f - f) * a); }
            Ritual.ringE(cx, gy, 90 * sz * eo(f), 18 * sz * eo(f), 0xFFC060, (1f - f) * a, 0, 6.2832f, 0, 0);
            for (int j = 0; j < (rng ? 36 : 20); j++) { float an = rnd(j * 5) * 3.1416f + 3.1416f, sp = (60 + rnd(j) * 60) * sz; dot(cx + (float) Math.cos(an) * sp * p, gy - 20 * sz + (float) Math.sin(an) * sp * p + 100 * sz * p * p, j % 2 == 0 ? 0xFFB040 : 0xFF5A20, (1f - f) * a); }
        }
        if (nod && p >= 0f) for (int j = 0; j < 10; j++) { float u = (p * 0.6f + rnd(j)) % 1f; Ritual.fillE(cx + (rnd(j * 3) - 0.5f) * 40 * sz, gy - u * 30 * sz, 4 * sz * u + 1, 3 * sz * u + 1, 0x4A4040, 0.4f * (1f - u) * (1f - cl(p / 1.2f)) * a); }
    }

    // ---- Bloodfiend: a coffin rattles and bursts open
    private static void coffin(float t, float O, float op, float p, float cx, float gy, float sz, float a, float rumble, boolean rng, boolean nod) {
        float rattle = 0f; for (int k = 0; k < 3; k++) { float bb = beat(op, O, k); if (bb >= 0f && bb < 0.5f) rattle += (float) Math.sin(bb * 60f) * (1f - bb / 0.5f); }
        rattle += (float) Math.sin(t * 60f) * rumble;
        float open = p >= 0f && !nod ? p : -1f, h = 52 * sz, top = gy - h, dx = rattle * 1.2f * sz;
        Ritual.fillE(cx, gy + 1, 16 * sz, 3 * sz, 0x000000, 0.4f * a);
        int seams = 0; for (int k = 0; k < 3; k++) if (beat(op, O, k) >= 0f) seams++;
        for (int y = 0; y < h; y++) {                                                          // the coffin's box (a stretched hexagon), standing up
            float u = y / h, hw = (u < 0.28f ? 7 + 4 * (u / 0.28f) : 11 - 5 * ((u - 0.28f) / 0.72f)) * sz;
            box(cx - hw - 1.5f * sz, top + y, cx + hw + 1.5f * sz, top + y + 1, 0x1A0608, a);
            if (open < 0f) { box(cx - hw + dx, top + y, cx + hw + dx, top + y + 1, mix(0x4A1418, 0x2A0A0C, u), a);
                if (seams > 0) { box(cx - hw + dx, top + y, cx - hw + dx + 1, top + y + 1, 0xFF3040, 0.25f * seams * a); box(cx + hw + dx - 1, top + y, cx + hw + dx, top + y + 1, 0xFF3040, 0.25f * seams * a); } }
            else box(cx - hw + 1.5f * sz, top + y, cx + hw - 1.5f * sz, top + y + 1, 0x3A0008, a);                      // the open inside
        }
        if (open < 0f) { box(cx - 0.8f * sz + dx, top + 8 * sz, cx + 0.8f * sz + dx, top + 24 * sz, 0xC0A060, a); box(cx - 4.5f * sz + dx, top + 13 * sz, cx + 4.5f * sz + dx, top + 14.6f * sz, 0xC0A060, a);       // the cross
            if (seams > 0) Ritual.glowE(cx, gy - h * 0.5f, 24 * sz, h * 0.6f, 0xFF2030, 0.12f * seams * a); }
        else if (open < 2f) {                                                                  // the lid flies off, spinning, and bats pour out
            float f = open / 2f, lx = cx - 50 * sz * eo(open / 0.8f), ly = top + h * 0.5f - 30 * sz * open + 60 * sz * open * open, an = -open * 4f;
            Ritual.stroke(lx - (float) Math.sin(an) * h * 0.45f, ly - (float) Math.cos(an) * h * 0.45f, lx + (float) Math.sin(an) * h * 0.45f, ly + (float) Math.cos(an) * h * 0.45f, 10 * sz, 0x4A1418, (1f - cl((open - 1.2f) / 0.6f)) * a);
            Ritual.glowE(cx, gy - h * 0.5f, 30 * sz, h * 0.8f, 0xFF2030, 0.7f * (1f - f) * a);
            for (int bt = 0; bt < (rng ? 14 : 8); bt++) { float u = cl(open * 0.9f - rnd(bt) * 0.4f); if (u <= 0f) continue; float bx = cx + (rnd(bt * 3) - 0.5f) * 140 * sz * u, by = gy - h * 0.5f - 70 * sz * u * (0.5f + rnd(bt * 7));
                batShape(bx, by, sz, t * 16 + bt, (1f - u) * a); }
        }
        for (int k = 0; k < 3; k++) {                                                          // a bat or two escape on each beat
            float bb = beat(op, O, k); if (bb < 0f || bb > 1.2f) continue;
            for (int i = 0; i < 2; i++) { float u = bb / 1.2f; batShape(cx + (i == 0 ? -1 : 1) * (6 + 70 * u) * sz, top + 4 * sz - 40 * sz * u, sz * 0.8f, t * 16 + k + i, (1f - u) * a); }
        }
    }
    private static void batShape(float x, float y, float sz, float flap, float a) {
        if (a < 0.03f) return; float w = 5 * sz, up = (float) Math.sin(flap) * 2.5f * sz;
        Ritual.stroke(x - w, y - up, x, y, 1.1f * sz, 0x14030A, a); Ritual.stroke(x + w, y - up, x, y, 1.1f * sz, 0x14030A, a);
        Ritual.fillE(x, y, 1.4f * sz, 1.4f * sz, 0x14030A, a); dot(x - 0.6f * sz, y - 0.5f * sz, 0xFF3040, a); dot(x + 0.6f * sz, y - 0.5f * sz, 0xFF3040, a);
    }
}
