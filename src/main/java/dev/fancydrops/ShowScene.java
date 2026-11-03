package dev.fancydrops;

/**
 * The Showcase scenes of the newer libraries, painted on the Ritual canvas (Ritual.paint adds the soft Normal / RNG finish on top).
 * Every one has the same three beats as the other scenes (0.34 / 0.58 / 0.80 of the opening), then the drop rises out of the middle.
 *
 *   WATER    a dark pool: a bobber dips three times, each dip throws rings and droplets; the water heaves and the drop rises in a splash column.
 *   LAVA     the same with a magma pool: bubbles pop, embers rise, every dip is a burst of fire; an eruption at the end.
 *   SPOOKY   a violet pool under a pumpkin moon, bats circling; every dip blows out a candle.
 *   WINTER   a fishing hole in a sheet of ice, snow falling; every dip cracks the ice further, it shatters at the end.
 *   CRIMSON  a basalt platform on the Crimson Isle: three meteors strike it, the cracks glow, it bursts open.
 *   MOBS     a monster spawner cage with a flame inside: it pulses three times, then the bars break apart.
 *   DYE      a white basin: three drops of the dye's colour fall in, each blooms into the water; the drop rises from a swirl of colour.
 */
final class ShowScene {
    private ShowScene() {}

    static final int WATER = 0, LAVA = 1, SPOOKY = 2, WINTER = 3, CRIMSON = 4, MOBS = 5, DYE = 6;
    /** The colour of the light each scene gives the drop's effects. */
    static final int[] ENERGY = {0x4FC3FF, 0xFF7A2E, 0xFF9A2E, 0xBFF0FF, 0xFF5A2A, 0xB070FF, 0xFF72C6};
    private static final float[] BEAT = {0.34f, 0.58f, 0.80f};
    private static final int WHITE = 0xFFFFFF;

    /** The scene of an animation group, or -1 for a group that is not a Showcase. */
    static int motifOf(String group) {
        return switch (group) {
            case "fish_water" -> WATER; case "fish_lava" -> LAVA; case "fish_spooky" -> SPOOKY; case "fish_winter" -> WINTER;
            case "crimson" -> CRIMSON; case "mobs" -> MOBS; case "dyes" -> DYE;
            default -> -1;
        };
    }

    private static float rnd(int n) { return Ritual.rnd(n); }
    private static float cl(float v) { return Ritual.cl(v); }
    private static float sm(float v) { return Ritual.sm(v); }
    private static float eo(float v) { return Ritual.eo(v); }
    private static int mix(int a, int b, float f) { return Ritual.mixc(a, b, f); }
    private static void dot(float x, float y, int rgb, float a) { Ritual.dot(x, y, rgb, a); }

    /** The time since beat k (negative before it). */
    private static float beat(float op, float O, int k) { return op - BEAT[k] * O; }

    /** The scene at time t. p = seconds since the drop appeared, a = how visible the whole scene is, tint = the dye's own colour (DYE only). */
    static void paint(float t, float O, float p, float icx, float gy, float sz, int motif, float a, boolean rng, int tint) {
        if (a <= 0.01f) return;
        float op = Math.min(t, O), appear = sm(t / (0.15f * O + 0.05f)), al = a * appear;
        float rumble = sm((op - 0.84f * O) / (0.16f * O)) * (1f - sm(p / 0.4f));
        switch (motif) {
            case WATER, LAVA, SPOOKY -> pool(t, O, op, p, icx, gy, sz, motif, al, rumble, rng);
            case WINTER -> ice(t, O, op, p, icx, gy, sz, al, rumble, rng);
            case CRIMSON -> meteors(t, O, op, p, icx, gy, sz, al, rumble, rng);
            case MOBS -> spawner(t, O, op, p, icx, gy, sz, al, rumble, rng);
            default -> basin(t, O, op, p, icx, gy, sz, al, rumble, rng, tint);
        }
    }

    // ------------------------------------------------------------------ water, lava and the spooky pool
    private static void pool(float t, float O, float op, float p, float cx, float gy, float sz, int m, float a, float rumble, boolean rng) {
        int deep = m == LAVA ? 0x5A1206 : m == SPOOKY ? 0x1A0C2E : 0x062A48, surf = m == LAVA ? 0xFF6A1A : m == SPOOKY ? 0x6A3AB0 : 0x2E8FD8, hi = m == LAVA ? 0xFFD060 : m == SPOOKY ? 0xFFA040 : 0xA8E4FF;
        float rx = 62 * sz, ry = 15 * sz;
        if (m == SPOOKY) {                                                                     // the pumpkin moon and the bats
            float mx = cx + 46 * sz, my = gy - 70 * sz;
            Ritual.glowE(mx, my, 26 * sz, 26 * sz, 0xFF9A2E, 0.35f * a);
            Ritual.fillE(mx, my, 9 * sz, 8 * sz, 0xFF9A2E, 0.9f * a);
            for (int i = 0; i < 3; i++) Ritual.line(mx - 5 * sz + i * 5 * sz, my - 7 * sz, mx - 5 * sz + i * 5 * sz, my + 7 * sz, 0xC86A10, 0.5f * a);
            Ritual.line(mx, my - 8 * sz, mx + 1, my - 11 * sz, 0x4A7A2A, a);
            for (int b = 0; b < 5; b++) {
                float an = t * (0.9f + 0.2f * b) + b * 1.3f, bx = cx + (float) Math.cos(an) * (70 + 8 * b) * sz, by = gy - (44 + 10 * b) * sz + (float) Math.sin(an * 2) * 6 * sz;
                float flap = (float) Math.sin(t * 18 + b) * 2.5f * sz;
                Ritual.line(bx - 4 * sz, by - flap, bx, by, 0x0A0614, a); Ritual.line(bx, by, bx + 4 * sz, by - flap, 0x0A0614, a); dot(bx, by, 0x0A0614, a);
            }
        }
        Ritual.glowE(cx, gy, rx * 1.35f, ry * 2.2f, surf, 0.28f * a);                         // the light the pool gives off
        Ritual.fillE(cx, gy, rx + 2, ry + 1.5f, mix(deep, 0x000000, 0.5f), a);                // the rim
        Ritual.fillE(cx, gy, rx, ry, deep, a);
        for (int i = 0; i < 26; i++) {                                                        // shimmering streaks on the surface
            float u = rnd(i * 13 + 2), v = rnd(i * 5 + 9) * 2f - 1f, sx = cx + (u * 2f - 1f) * rx * 0.85f * (float) Math.sqrt(1 - v * v * 0.6f), sy = gy + v * ry * 0.7f;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * (2f + rnd(i) * 2f) + i), len = (3 + 5 * rnd(i * 3)) * sz;
            Ritual.line(sx - len, sy, sx + len, sy, mix(surf, hi, tw * 0.5f), (0.25f + 0.35f * tw) * a);
        }
        if (m == LAVA) for (int i = 0; i < 8; i++) {                                          // magma bubbles that swell and pop
            float ph = (t * (0.5f + 0.3f * rnd(i)) + rnd(i * 7)) % 1f, bx = cx + (rnd(i * 11) * 2 - 1) * rx * 0.7f, by = gy + (rnd(i * 17) * 2 - 1) * ry * 0.5f;
            if (ph < 0.8f) Ritual.ringE(bx, by, (1 + 2.5f * ph) * sz, (0.6f + 1.4f * ph) * sz, hi, 0.7f * a, 0, 6.2832f, 0, 0);
            else { float q = (ph - 0.8f) / 0.2f; for (int s = 0; s < 4; s++) dot(bx + (s - 1.5f) * 2 * q * 4 * sz, by - 5 * q * sz, hi, (1 - q) * a); }
        }
        if (m == LAVA) Ritual.motes(t, cx, gy, rx, ry, 60 * sz, 0xFF8A2A, 0.7f * a, 18);
        else Ritual.motes(t, cx, gy, rx, ry, 30 * sz, hi, 0.35f * a, 8);

        // the line and the bobber: it bobs, dips at each beat, and is yanked out just before the drop
        float yank = sm((op - 0.88f * O) / (0.1f * O)), dip = 0f;
        for (int k = 0; k < 3; k++) { float tau = beat(op, O, k); if (tau >= 0 && tau < 0.35f) dip = Math.max(dip, (float) Math.sin(tau / 0.35f * Math.PI)); }
        float bx = cx + 6 * sz, by = gy - 1 + (float) Math.sin(t * 3f) * 0.8f * sz + dip * 3.5f * sz - yank * 70 * sz;
        if (p < 0.2f) {
            float rod = 1f - cl(p / 0.2f);
            Ritual.stroke(cx + 70 * sz, gy - 90 * sz, bx, by - 3 * sz, 0.5f, 0xE8E8E8, 0.55f * a * rod);
            Ritual.fillE(bx, by - 1.5f * sz, 2.2f * sz, 1.6f * sz, 0xE83030, a * rod);
            Ritual.fillE(bx, by + 0.5f * sz, 2.2f * sz, 1.2f * sz, WHITE, a * rod);
        }
        for (int k = 0; k < 3; k++) {                                                         // each dip: rings, droplets (or fire), and a glow
            float tau = beat(op, O, k); if (tau < 0f || tau > 1.1f) continue;
            float u = tau / 1.1f;
            for (int r = 0; r < 2; r++) { float ur = cl(u * 1.2f - r * 0.18f); if (ur > 0f && ur < 1f) Ritual.ringE(bx, gy, (3 + 30 * eo(ur)) * sz, (1 + 7 * eo(ur)) * sz, hi, (1f - ur) * 0.8f * a, 0, 6.2832f, 0, 0); }
            if (tau < 0.6f) for (int j = 0; j < 10; j++) {
                float vx = (rnd(k * 40 + j) - 0.5f) * 60 * sz, vy = -(25 + rnd(k * 70 + j) * 40) * sz;
                dot(bx + vx * tau, gy + vy * tau + 160 * sz * tau * tau, j % 3 == 0 ? WHITE : hi, (1f - tau / 0.6f) * a);
            }
            if (tau < 0.3f) Ritual.glowE(bx, gy, 22 * sz, 8 * sz, m == LAVA ? 0xFFB040 : WHITE, 0.6f * (1f - tau / 0.3f) * a);
        }
        if (rumble > 0f) for (int r = 0; r < 3; r++) Ritual.ringE(cx, gy, rx * (0.2f + 0.25f * r + 0.05f * (float) Math.sin(t * 30 + r)), ry * (0.2f + 0.25f * r), hi, 0.5f * rumble * a, 0, 6.2832f, 0, 0);
        if (p >= 0f) {                                                                        // the surface bursts: a column of water (or fire) throws the drop up
            float u = p / (rng ? 1.2f : 0.9f);
            if (u < 1f) {
                float h = 60 * sz * eo(u) * (1f - u * 0.5f), w = 9 * sz * (1f - u);
                for (int i = 0; i < 40; i++) { float v = i / 40f, x = cx + (rnd(i * 3) - 0.5f) * w * 2 * (1 - v * 0.5f), y = gy - v * h; dot(x, y, i % 4 == 0 ? WHITE : hi, (1f - u) * (1f - v * 0.6f) * a); }
                for (int j = 0; j < (rng ? 34 : 20); j++) {
                    float vx = (rnd(j * 9 + 1) - 0.5f) * 110 * sz, vy = -(40 + rnd(j * 13 + 5) * 70) * sz, tt = p;
                    dot(cx + vx * tt, gy + vy * tt + 150 * sz * tt * tt, j % 3 == 0 ? WHITE : hi, (1f - u) * a);
                }
                Ritual.ringE(cx, gy, rx * (0.3f + 1.2f * eo(u)), ry * (0.3f + 1.2f * eo(u)), WHITE, (1f - u) * 0.8f * a, 0, 6.2832f, 0, 0);
            }
        }
    }

    // ------------------------------------------------------------------ the ice hole
    private static void ice(float t, float O, float op, float p, float cx, float gy, float sz, float a, float rumble, boolean rng) {
        float rx = 74 * sz, ry = 18 * sz;
        Ritual.fillE(cx, gy, rx, ry, 0xCFEFFF, 0.85f * a);                                    // the sheet of ice
        Ritual.fillE(cx, gy - 1, rx * 0.96f, ry * 0.9f, 0xE8F8FF, 0.6f * a);
        float shatter = p >= 0f ? eo(p / 0.6f) : 0f;
        if (shatter < 1f) {
            Ritual.fillE(cx, gy, 16 * sz * (1 + shatter), 5 * sz * (1 + shatter), 0x0A3A5A, a);   // the fishing hole
            Ritual.ringE(cx, gy, 16 * sz, 5 * sz, 0x8ACCE8, 0.8f * a, 0, 6.2832f, 0, 0);
        }
        int cracks = 0; for (int k = 0; k < 3; k++) if (beat(op, O, k) >= 0) cracks = k + 1;
        for (int k = 0; k < cracks * 4; k++) {                                                // cracks run out from the hole, more with every beat
            float an = rnd(k * 31) * 6.2832f, len = (24 + 30 * rnd(k * 7)) * sz * Math.min(1f, (beat(op, O, k / 4) + 0.05f) / 0.25f);
            float x0 = cx + (float) Math.cos(an) * 16 * sz, y0 = gy + (float) Math.sin(an) * 5 * sz, kx = (float) Math.cos(an + (rnd(k) - 0.5f) * 0.6f), ky = (float) Math.sin(an + (rnd(k) - 0.5f) * 0.6f) * 0.25f;
            float mx = x0 + kx * len * 0.5f, my = y0 + ky * len * 0.5f;
            Ritual.stroke(x0, y0, mx, my, 0.6f, 0x5AA8D0, 0.8f * a * (1 - shatter)); Ritual.stroke(mx, my, mx + (float) Math.cos(an) * len * 0.5f, my + (float) Math.sin(an) * len * 0.12f, 0.5f, 0x5AA8D0, 0.6f * a * (1 - shatter));
        }
        for (int k = 0; k < 3; k++) { float tau = beat(op, O, k); if (tau >= 0 && tau < 0.3f) { Ritual.glowE(cx, gy, 30 * sz, 9 * sz, WHITE, 0.7f * (1 - tau / 0.3f) * a); for (int j = 0; j < 8; j++) dot(cx + (rnd(k * 50 + j) - 0.5f) * 50 * sz * tau / 0.3f, gy - rnd(k * 9 + j) * 14 * sz * tau / 0.3f, WHITE, (1 - tau / 0.3f) * a); } }
        if (rumble > 0f) Ritual.glowE(cx, gy, 20 * sz, 6 * sz, 0x9FE4FF, 0.6f * rumble * a);
        if (p >= 0f && p < 1.2f) for (int j = 0; j < (rng ? 30 : 18); j++) {                  // shards of ice thrown out
            float vx = (rnd(j * 9 + 3) - 0.5f) * 140 * sz, vy = -(30 + rnd(j * 13 + 1) * 60) * sz, x = cx + vx * p, y = gy + vy * p + 140 * sz * p * p, s = (1.5f + rnd(j) * 2f) * sz;
            Ritual.stroke(x - s, y, x + s, y - s * 0.6f, 0.8f, j % 3 == 0 ? WHITE : 0xBFE8FF, (1 - p / 1.2f) * a);
        }
        for (int i = 0; i < 34; i++) {                                                        // the snow
            float u = (t * (0.08f + 0.06f * rnd(i)) + rnd(i * 7)) % 1f, x = cx + (rnd(i * 3) - 0.5f) * 200 * sz + (float) Math.sin(t * 1.3f + i) * 4 * sz, y = gy - 110 * sz + u * 120 * sz;
            dot(x, y, WHITE, (float) Math.sin(u * Math.PI) * 0.8f * a);
        }
    }

    // ------------------------------------------------------------------ the Crimson Isle: meteors on a basalt platform
    private static void meteors(float t, float O, float op, float p, float cx, float gy, float sz, float a, float rumble, boolean rng) {
        float rx = 66 * sz, ry = 16 * sz;
        Ritual.glowE(cx, gy - 50 * sz, 140 * sz, 70 * sz, 0x8A1A0A, 0.25f * a);                 // the red sky of the Isle
        Ritual.fillE(cx, gy + 2, rx + 2, ry + 2, 0x140A0A, a);
        Ritual.fillE(cx, gy, rx, ry, 0x2A2026, a);                                            // basalt
        for (int i = 0; i < 16; i++) { float u = rnd(i * 7), v = rnd(i * 13) * 2 - 1; Ritual.fillE(cx + (u * 2 - 1) * rx * 0.8f, gy + v * ry * 0.6f, (3 + 4 * rnd(i)) * sz, (1 + 1.5f * rnd(i + 3)) * sz, 0x3A3036, 0.8f * a); }
        int hits = 0;
        for (int k = 0; k < 3; k++) {
            float tau = beat(op, O, k), lead = 0.32f, hx = cx + (k - 1) * 22 * sz, hy = gy + (k == 1 ? -3 : 3) * sz;
            if (tau >= -lead && tau < 0f) {                                                   // the meteor falling, a trail of fire behind it
                float u = (tau + lead) / lead, sx = hx - 70 * sz, sy = gy - 120 * sz, x = Ritual.lerp(sx, hx, u * u), y = Ritual.lerp(sy, hy, u * u);
                for (int s = 0; s < 14; s++) { float b = s / 14f; dot(x - (x - sx) * b * 0.35f, y - (y - sy) * b * 0.35f, s < 3 ? WHITE : mix(0xFFD060, 0xFF3A10, b), (1 - b) * a); }
                Ritual.glowE(x, y, 8 * sz, 8 * sz, 0xFF8A2A, 0.8f * a);
            }
            if (tau >= 0f) {
                hits++;
                if (tau < 0.8f) {
                    float u = tau / 0.8f;
                    Ritual.glowE(hx, hy, 30 * sz * (1 - u * 0.5f), 10 * sz, 0xFFB040, (1 - u) * 0.9f * a);
                    Ritual.ringE(hx, hy, (4 + 34 * eo(u)) * sz, (1.5f + 9 * eo(u)) * sz, 0xFF8A2A, (1 - u) * 0.8f * a, 0, 6.2832f, 0, 0);
                    for (int j = 0; j < 12; j++) { float vx = (rnd(k * 40 + j) - 0.5f) * 90 * sz, vy = -(30 + rnd(k * 70 + j) * 60) * sz; dot(hx + vx * tau, hy + vy * tau + 170 * sz * tau * tau, j % 3 == 0 ? 0xFFE080 : 0xFF5A20, (1 - u) * a); }
                }
                Ritual.fillE(hx, hy, 4 * sz, 1.6f * sz, 0xFF6A1A, 0.8f * a);                     // the crater keeps glowing
            }
        }
        for (int k = 0; k < hits * 3; k++) {                                                  // glowing cracks join the craters to the middle
            float an = rnd(k * 19) * 6.2832f, len = (18 + 26 * rnd(k * 5)) * sz;
            Ritual.stroke(cx, gy, cx + (float) Math.cos(an) * len, gy + (float) Math.sin(an) * len * 0.25f, 0.6f, 0xFF7A2A, (0.4f + 0.4f * rumble) * a * (p > 0 ? 1 - cl(p / 0.8f) : 1));
        }
        Ritual.motes(t, cx, gy, rx, ry, 70 * sz, 0xFF6A2A, 0.6f * a, 20);
        if (p >= 0f && p < 1f) { Ritual.glowE(cx, gy, rx * (1 + p), ry * (1.6f + p), 0xFFB040, (1 - p) * 0.7f * a); }
    }

    // ------------------------------------------------------------------ rare mobs: a spawner cage
    private static void spawner(float t, float O, float op, float p, float cx, float gy, float sz, float a, float rumble, boolean rng) {
        float s = 22 * sz, top = gy - 2 * s;
        Ritual.fillE(cx, gy + 2, 60 * sz, 12 * sz, 0x101018, 0.8f * a);                      // a shadow and a stone floor
        Ritual.fillE(cx, gy, 56 * sz, 11 * sz, 0x3A3A46, 0.6f * a);
        float pulse = 0f; for (int k = 0; k < 3; k++) { float tau = beat(op, O, k); if (tau >= 0 && tau < 0.5f) pulse = Math.max(pulse, 1 - tau / 0.5f); }
        float flame = 0.6f + 0.4f * (float) Math.sin(t * 9f) * (float) Math.sin(t * 5.3f) + pulse;
        Ritual.glowE(cx, gy - s, s * (1.3f + 0.6f * pulse), s * (1.3f + 0.6f * pulse), 0xB070FF, (0.35f + 0.4f * pulse) * a);
        if (p < 0f) {                                                                         // the little mob spinning inside
            float sp = t * (2.4f + 3f * rumble), w = (float) Math.abs(Math.cos(sp)) * 7 * sz;
            Ritual.fillE(cx, gy - s - 2 * sz, Math.max(1f, w), 5 * sz, 0x2A1A3A, 0.9f * a);
            Ritual.fillE(cx, gy - s - 9 * sz, Math.max(1f, w * 0.7f), 3.5f * sz, 0x2A1A3A, 0.9f * a);
            for (int i = 0; i < 10; i++) { float u = (t * 1.6f + rnd(i * 5)) % 1f; dot(cx + (rnd(i * 3) - 0.5f) * s * 1.4f, gy - s * 0.2f - u * s * 1.6f, i % 2 == 0 ? 0xFF8A2A : 0xFFD060, (float) Math.sin(u * Math.PI) * cl(flame) * a); }
        }
        float brk = p >= 0f ? eo(p / 0.7f) : 0f;
        for (int i = 0; i <= 4; i++) {                                                        // the bars: a cube seen a little from above
            float x = cx - s + i * s / 2f, fly = brk * (i - 2) * 30 * sz, fall = brk * brk * 40 * sz;
            Ritual.stroke(x + fly, top + fall * 0.3f, x + fly * 1.2f, gy + fall * 0.5f, 0.9f, 0x2A2A36, a * (1 - brk));
            Ritual.stroke(x + fly + 0.6f, top + fall * 0.3f, x + fly * 1.2f + 0.6f, gy + fall * 0.5f, 0.4f, 0x7A7A90, a * (1 - brk));
        }
        if (brk < 1f) for (int r = 0; r < 3; r++) { float y = top + r * s; Ritual.stroke(cx - s - brk * 50 * sz, y - brk * 20 * sz, cx + s + brk * 50 * sz, y - brk * 10 * sz, 0.9f, 0x2A2A36, a * (1 - brk)); Ritual.stroke(cx - s, y + 0.6f, cx + s, y + 0.6f, 0.4f, 0x8A8AA0, a * (1 - brk)); }
        for (int k = 0; k < 3; k++) { float tau = beat(op, O, k); if (tau >= 0 && tau < 0.7f) Ritual.ringE(cx, gy - s, (s + 30 * sz * eo(tau / 0.7f)), (s + 30 * sz * eo(tau / 0.7f)) * 0.8f, 0xC090FF, (1 - tau / 0.7f) * 0.7f * a, 0, 6.2832f, t, 3); }
        Ritual.motes(t, cx, gy, 40 * sz, 10 * sz, 60 * sz, 0xB070FF, 0.5f * a, 14);
    }

    // ------------------------------------------------------------------ dyes: drops of colour into a basin
    private static void basin(float t, float O, float op, float p, float cx, float gy, float sz, float a, float rumble, boolean rng, int tint) {
        float rx = 56 * sz, ry = 14 * sz;
        int water = 0xE8F4FF;
        int filled = 0; for (int k = 0; k < 3; k++) if (beat(op, O, k) >= 0) filled = k + 1;
        float mixAmt = filled / 3f * 0.75f + (p > 0 ? 0.25f * cl(p / 0.5f) : 0f);
        Ritual.fillE(cx, gy + 4 * sz, rx + 5 * sz, ry + 5 * sz, 0x9AA0B0, a);                // the porcelain basin
        Ritual.fillE(cx, gy + 2 * sz, rx + 3 * sz, ry + 3 * sz, 0xF4F6FA, a);
        Ritual.fillE(cx, gy, rx, ry, mix(water, tint, mixAmt * 0.85f), a);
        for (int i = 0; i < 3 * filled; i++) {                                                 // swirls of colour, turning slowly
            float an = t * (0.6f + 0.2f * (i % 3)) + i * 2.1f, r = (0.25f + 0.22f * (i % 3)) * rx;
            Ritual.ringE(cx, gy, r, r * ry / rx, mix(tint, WHITE, 0.25f * (i % 2)), 0.55f * a, an, an + 2.4f, 0, 0);
        }
        for (int k = 0; k < 3; k++) {
            float tau = beat(op, O, k), lead = 0.3f, dx = cx + (k - 1) * 16 * sz;
            if (tau >= -lead && tau < 0f) {                                                   // a drop of dye falls
                float u = (tau + lead) / lead, y = Ritual.lerp(gy - 100 * sz, gy, u * u);
                Ritual.fillE(dx, y, 2.4f * sz, 3.4f * sz, tint, a); dot(dx - 0.6f * sz, y - 1.2f * sz, WHITE, 0.8f * a);
            }
            if (tau >= 0f && tau < 1f) {                                                       // it blooms into the water
                float u = tau;
                Ritual.fillE(dx, gy, (3 + 22 * eo(u)) * sz, (1 + 5.5f * eo(u)) * sz, tint, (1 - u) * 0.7f * a);
                Ritual.ringE(dx, gy, (3 + 30 * eo(u)) * sz, (1 + 7 * eo(u)) * sz, mix(tint, WHITE, 0.4f), (1 - u) * 0.8f * a, 0, 6.2832f, 0, 0);
                if (u < 0.5f) for (int j = 0; j < 8; j++) { float vx = (rnd(k * 30 + j) - 0.5f) * 50 * sz, vy = -(20 + rnd(k * 60 + j) * 30) * sz; dot(dx + vx * u, gy + vy * u + 150 * sz * u * u, tint, (1 - u * 2) * a); }
            }
        }
        if (rumble > 0f) Ritual.glowE(cx, gy, rx * 0.6f, ry * 0.8f, tint, 0.6f * rumble * a);
        if (p >= 0f && p < 1.3f) {                                                             // a fountain of colour
            for (int j = 0; j < (rng ? 44 : 26); j++) {
                float vx = (rnd(j * 9 + 1) - 0.5f) * 120 * sz, vy = -(40 + rnd(j * 13 + 5) * 80) * sz;
                int c = j % 4 == 0 ? WHITE : j % 4 == 1 ? mix(tint, WHITE, 0.5f) : tint;
                dot(cx + vx * p, gy + vy * p + 150 * sz * p * p, c, (1 - p / 1.3f) * a); dot(cx + vx * p + 1, gy + vy * p + 150 * sz * p * p, c, (1 - p / 1.3f) * 0.6f * a);
            }
        }
    }

    // ------------------------------------------------------------------ the finish round the item: one of its own for every scene
    /** Drawn round each drop once it has appeared. p = seconds since then, a = how visible the item is. */
    static void finish(float t, float p, float cx, float cy, float sz, float isc, int motif, int tint, boolean rng, float a) {
        if (a <= 0.01f) return;
        float R = (rng ? 30 : 24) * sz * isc, grow = eo(p / 0.6f);
        switch (motif) {
            case WATER -> {                                                                  // a halo of water turns round it, bubbles rise
                for (int i = 0; i < 26; i++) { float an = t * 1.6f + i * 0.2416f; float r = R * grow * (0.95f + 0.05f * (float) Math.sin(t * 4 + i)); dot(cx + (float) Math.cos(an) * r, cy + (float) Math.sin(an) * r * 0.42f, i % 4 == 0 ? WHITE : 0x7FD4FF, (0.5f + 0.4f * (float) Math.sin(an * 2)) * a); }
                for (int i = 0; i < (rng ? 16 : 9); i++) { float u = (t * 0.5f + rnd(i * 7)) % 1f, x = cx + (rnd(i * 3) - 0.5f) * R * 1.4f + (float) Math.sin(t * 3 + i) * 1.5f, y = cy + R * 0.6f - u * R * 2.2f;
                    Ritual.ringE(x, y, 1.2f + rnd(i) * 1.3f, 1.2f + rnd(i) * 1.3f, 0xBDEBFF, (float) Math.sin(u * Math.PI) * 0.8f * a, 0, 6.2832f, 0, 0); }
            }
            case LAVA -> {                                                                   // a spiral of embers winds up round it, heat ripples
                for (int i = 0; i < (rng ? 30 : 18); i++) { float u = (t * 0.45f + i / (rng ? 30f : 18f)) % 1f, an = u * 9f + i, r = R * (1f - 0.6f * u) * grow;
                    dot(cx + (float) Math.cos(an) * r, cy + R * 0.5f - u * R * 1.8f + (float) Math.sin(an) * r * 0.3f, u < 0.3f ? 0xFFF0A0 : u < 0.7f ? 0xFFA030 : 0xFF4A10, (float) Math.sin(u * Math.PI) * a); }
                for (int k = 0; k < 2; k++) { float u = (t * 0.7f + k * 0.5f) % 1f; Ritual.ringE(cx, cy, R * (0.4f + 0.8f * u), R * (0.4f + 0.8f * u) * 0.5f, 0xFF7A2E, (1f - u) * 0.5f * a, 0, 6.2832f, 0, 3); }
            }
            case SPOOKY -> {                                                                 // three wisps chase each other round it, each with a fading tail
                for (int w = 0; w < 3; w++) for (int k = 0; k < 10; k++) { float an = t * 2.2f + w * 2.094f - k * 0.12f, r = R * grow * (0.85f + 0.15f * (float) Math.sin(t * 3 + w));
                    float x = cx + (float) Math.cos(an) * r, y = cy + (float) Math.sin(an) * r * 0.55f - (float) Math.sin(t * 5 + w) * 2; int c = w == 1 ? 0xFF9A2E : 0xC8A8FF;
                    if (k == 0) { Ritual.glowE(x, y, 4 * sz * isc, 4 * sz * isc, c, 0.6f * a); Ritual.glint(x, y, 1, WHITE, a); } else dot(x, y, c, (1f - k / 10f) * 0.8f * a); }
            }
            case WINTER -> {                                                                 // a snowflake of light grows behind it and turns slowly
                for (int arm = 0; arm < 6; arm++) { float an = arm * 1.0472f + t * 0.3f, ux = (float) Math.cos(an), uy = (float) Math.sin(an), L = R * 1.1f * grow;
                    Ritual.stroke(cx + ux * 4, cy + uy * 4, cx + ux * L, cy + uy * L, 0.5f, 0xDFF6FF, 0.75f * a);
                    for (int b = 1; b <= 2; b++) { float bx = cx + ux * L * b / 3f, by = cy + uy * L * b / 3f, bl = L * 0.22f;
                        Ritual.stroke(bx, by, bx + (float) Math.cos(an + 0.7f) * bl, by + (float) Math.sin(an + 0.7f) * bl, 0.4f, 0xBFE8FF, 0.6f * a);
                        Ritual.stroke(bx, by, bx + (float) Math.cos(an - 0.7f) * bl, by + (float) Math.sin(an - 0.7f) * bl, 0.4f, 0xBFE8FF, 0.6f * a); }
                    Ritual.glint(cx + ux * L, cy + uy * L, 1, WHITE, a * (0.6f + 0.4f * (float) Math.sin(t * 5 + arm))); }
            }
            case CRIMSON -> {                                                                // a ring of flame tongues licks upward round it
                int n = rng ? 18 : 12;
                for (int i = 0; i < n; i++) { float an = i * 6.2832f / n + t * 0.8f, bx = cx + (float) Math.cos(an) * R * grow, by = cy + (float) Math.sin(an) * R * grow * 0.45f + 4 * sz;
                    float h = (4 + 4 * (0.5f + 0.5f * (float) Math.sin(t * 9 + i * 1.7f))) * sz * isc * (by > cy ? 1.2f : 0.8f);
                    for (int k = 0; k < 5; k++) { float u = k / 5f; dot(bx + (float) Math.sin(t * 7 + i + u * 3) * 0.8f, by - u * h, u < 0.3f ? 0xFFE080 : u < 0.6f ? 0xFF8A2A : 0xC02A10, (1f - u) * a); } }
            }
            case MOBS -> {                                                                   // soul flames orbit it on a tilted ring
                for (int i = 0; i < 5; i++) { float an = t * 1.8f + i * 1.2566f, x = (float) Math.cos(an) * R, y = (float) Math.sin(an) * R * 0.35f, d = 0.55f + 0.45f * (float) Math.sin(an);
                    float fx = cx + x * 0.95f - y * 0.3f, fy = cy + x * 0.3f + y;
                    for (int k = 0; k < 4; k++) dot(fx + (float) Math.sin(t * 8 + i + k) * 0.6f, fy - k * 1.3f, k == 0 ? 0xE8D0FF : 0xA060FF, (1f - k / 4f) * d * a);
                    Ritual.glowE(fx, fy, 4 * sz * isc, 4 * sz * isc, 0xA060FF, 0.35f * d * a); }
            }
            default -> {                                                                     // DYE: petals of paint burst out and settle into a flower of its colour
                int n = 8;
                for (int i = 0; i < n; i++) { float an = i * 6.2832f / n + 0.3f * (float) Math.sin(t * 0.8f), r = R * eo(p / 0.5f) * (1f + 0.08f * (float) Math.sin(t * 3 + i));
                    float px = cx + (float) Math.cos(an) * r, py = cy + (float) Math.sin(an) * r * 0.8f;
                    Ritual.fillE(px, py, 3.2f * sz * isc, 2.2f * sz * isc, i % 2 == 0 ? tint : mix(tint, WHITE, 0.35f), 0.75f * a);
                    dot(px - 0.6f, py - 0.6f, WHITE, 0.6f * a); }
                if (rng) for (int j = 0; j < 20; j++) { float life = 1.1f, tau = (t + rnd(j * 5) * life) % life, an = rnd(j * 3) * 6.2832f; dot(cx + (float) Math.cos(an) * R * 1.6f * tau / life, cy + (float) Math.sin(an) * R * 1.2f * tau / life, j % 2 == 0 ? tint : WHITE, (1f - tau / life) * a); }
            }
        }
    }
}
