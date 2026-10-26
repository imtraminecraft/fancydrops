package dev.fancydrops;

/**
 * The Farming scenes, painted on the Ritual canvas (see Ritual.paint, which adds the Normal and RNG finish on top).
 *
 *   Rare Crops     a field of wheat, carrots and sunflowers sways in the wind; three golden scythe sweeps cut it down third by third.
 *   Harvest Feast  a lantern-lit autumn field of corn, pumpkins and melons; the scythe sweeps pop the fruit off the vines.
 *   Pest Drops     a pest gnaws the crops; a vacuum swings in, switches on, plucks it off the soil, spins it and swallows it.
 *                  The Ray of Helios swaps the vacuum for a sun that burns the pest with a beam.
 *
 * Then the soil heaves where the drop will spring from. The three beats are at 0.34 / 0.58 / 0.80 of the opening, like the slayer cuts.
 */
final class FarmScene {
    private FarmScene() {}

    static final int CROPS = 0, FEAST = 1, PEST = 2, HELIOS = 3;
    /** The colour of the light each scene gives the drop's effects. */
    static final int[] ENERGY = {0xFFD25A, 0xFFA63A, 0x9AE05A, 0xFFE27A};
    private static final float[] BEAT = {0.34f, 0.58f, 0.80f};

    private static float rnd(int n) { return Ritual.rnd(n); }
    private static float cl(float v) { return Ritual.cl(v); }
    private static float sm(float v) { return Ritual.sm(v); }
    private static int mix(int a, int b, float f) { return Ritual.mixc(a, b, f); }
    private static void dot(float x, float y, int rgb, float a) { Ritual.dot(x, y, rgb, a); }
    private static void line(float x0, float y0, float x1, float y1, int rgb, float a) { Ritual.line(x0, y0, x1, y1, rgb, a); }

    /** The scene at time t. op = the opening clock (stops at O), p = seconds since the drop appeared, a = how visible the whole scene is. */
    static void paint(float t, float O, float p, float icx, float gy, float sz, int kind, float a, boolean rng) {
        if (a <= 0.01f) return;
        float op = Math.min(t, O);
        float rumble = sm((op - 0.84f * O) / (0.16f * O)) * (1f - sm(p / 0.4f));
        soil(icx, gy, sz, kind, a, rumble);
        if (kind >= PEST) pestScene(t, O, op, p, icx, gy, sz, kind == HELIOS, a, rumble);
        else cropScene(t, O, op, p, icx, gy, sz, kind == FEAST, a, rumble);
        if (p >= 0f) heave(p, icx, gy, sz, kind, a, rng);
        float warm = kind == FEAST ? 0.14f : 0.09f;                                                // the sun (or the lanterns) lying over the field
        Ritual.glowE(icx, gy - 8 * sz, 100 * sz, 36 * sz, kind == FEAST ? 0xFFA63A : 0xFFE9A0, warm * a);
        Ritual.motes(t, icx, gy, 90 * sz, 14 * sz, 46 * sz, ENERGY[kind], 0.8f * a, kind == FEAST ? 16 : 12);          // pollen, or embers of the lanterns, drifting over the field
    }

    // ------------------------------------------------------------------ the ground
    private static float fieldRx(float sz) { return 104 * sz; }
    private static float fieldRy(float sz) { return 17 * sz; }

    private static void soil(float icx, float gy, float sz, int kind, float a, float rumble) {
        float rx = fieldRx(sz), ry = fieldRy(sz), cy = gy + 2 * sz;
        int dark = kind == FEAST ? 0x2E1A0E : 0x3A2616, mid = kind == FEAST ? 0x4A2C16 : 0x5A3E22, lite = kind == FEAST ? 0x6A4224 : 0x7C5A34;
        Ritual.fillE(icx, cy + 2.2f * sz, rx * 1.04f, ry * 1.1f, 0x000000, 0.30f * a);                       // soft shadow
        Ritual.fillE(icx, cy + 1 * sz, rx, ry, dark, a);
        Ritual.fillE(icx, cy, rx * 0.97f, ry * 0.93f, mid, a);
        Ritual.fillE(icx - rx * 0.12f, cy - ry * 0.28f, rx * 0.7f, ry * 0.5f, lite, 0.55f * a);              // lit from the upper left
        for (int r = 0; r < 3; r++) {                                                                       // the furrows between the rows
            float y = rowY(gy, sz, r) + 2.4f * sz, hw = rx * (float) Math.sqrt(Math.max(0f, 1f - sq((y - cy) / ry))) * 0.94f;
            for (float x = -hw; x <= hw; x += 1f) Ritual.dot(icx + x, y + (float) Math.sin(x * 0.4f + r) * 0.4f, dark, 0.7f * a);
        }
        for (int i = 0; i < 26; i++) {                                                                      // clods and pebbles
            float ang = rnd(i * 7) * 6.2832f, rr = (0.25f + 0.7f * rnd(i * 3)) * 0.92f;
            dot(icx + (float) Math.cos(ang) * rx * rr, cy + (float) Math.sin(ang) * ry * rr, i % 3 == 0 ? lite : dark, 0.8f * a);
        }
        if (rumble > 0.02f) for (int i = 0; i < 8; i++) dot(icx + (rnd(i * 11 + (int) (rumble * 40)) - 0.5f) * 24 * sz, gy + 2 * sz - rnd(i + 5) * 3 * sz, lite, rumble * a);        // dust jumping on the soil
    }
    private static float sq(float v) { return v * v; }
    /** The soil line of row r (0 back .. 2 front). */
    private static float rowY(float gy, float sz, int r) { return gy - 6 * sz + r * 7 * sz; }

    // ------------------------------------------------------------------ crops
    private static final int WHEAT = 0, CARROT = 1, SUN = 2, CORN = 3, PUMPKIN = 4, MELON = 5;

    private static void cropScene(float t, float O, float op, float p, float icx, float gy, float sz, boolean feast, float a, float rumble) {
        float rx = fieldRx(sz), ry = fieldRy(sz), cy = gy + 2 * sz, sp = 11 * sz, xl = icx - rx * 0.9f, xr = icx + rx * 0.9f;
        float[] ts = {BEAT[0] * O, BEAT[1] * O, BEAT[2] * O};
        for (int r = 0; r < 3; r++) {
            float yb = rowY(gy, sz, r);
            int n = (int) ((xr - xl) / sp);
            for (int i = 0; i <= n; i++) {
                float x = xl + i * sp + (rnd(r * 53 + i) - 0.5f) * sp * 0.4f, yy = yb + (rnd(r * 17 + i * 3) - 0.5f) * 1.6f * sz;
                if (sq((x - icx) / rx) + sq((yy - cy) / ry) > 0.86f) continue;
                int seed = r * 101 + i * 7;
                int type = feast ? (r == 0 ? CORN : (rnd(seed) > 0.5f ? PUMPKIN : MELON)) : (rnd(seed) < 0.5f ? WHEAT : rnd(seed) < 0.78f ? CARROT : SUN);
                float u = cl((x - xl) / (xr - xl)); int third = Math.min(2, (int) (u * 3f)); float within = cl(u * 3f - third);
                boolean ltr = third % 2 == 0;
                float tc = ts[third] + (ltr ? within : 1f - within) * 0.13f;
                float age = op - tc;
                float sway = (float) Math.sin(t * 1.9f + x * 0.21f + r) * 0.8f + rumble * (rnd(seed + (int) (t * 30)) - 0.5f) * 3f;
                crop(type, x, yy, sz, sway, seed, a, age, ltr ? 1 : -1, t);
            }
        }
        for (int k = 0; k < 3; k++) {                                                                       // the scythe sweeps
            float tau = op - ts[k];
            if (tau < 0f) continue;
            float xa = xl + k * (xr - xl) / 3f - 5 * sz, xb = xl + (k + 1) * (xr - xl) / 3f + 5 * sz;
            sweep(tau, k % 2 == 0, xa, xb, gy - 13 * sz, sz, feast ? 0xFFC060 : 0xFFE27A, a);
        }
    }

    /** One crop. age < 0: standing; after that it is cut and falls (stalks tip over, fruit hops and sinks), leaving a short stub. */
    private static void crop(int type, float x, float yb, float sz, float sway, int seed, float a, float age, int dir, float t) {
        boolean fruit = type == PUMPKIN || type == MELON;
        float h = (type == CORN ? 30 : type == SUN ? 28 : type == WHEAT ? 23 : type == CARROT ? 13 : 7) * sz * (0.9f + 0.2f * rnd(seed + 4));
        float ang = 0f, hop = 0f, fa = 1f;
        if (age >= 0f) {
            float q = sm(age / 0.42f);
            ang = fruit ? 0f : dir * q * 1.45f;
            hop = fruit ? (float) Math.sin(cl(age / 0.5f) * 3.1416f) * 6 * sz : 0f;
            fa = 1f - sm((age - 0.26f) / 0.3f);
            if (age < 0.5f) burst(x, yb - h * 0.6f, age, seed, type, sz, a);
        }
        float stub = 3 * sz;
        if (age >= 0f) {                                                                                     // the stub that stays
            if (fruit) dot(x, yb, 0x4E7A2E, 0.9f * a); else { line(x, yb, x, yb - stub, type == WHEAT ? 0x8E7A2A : 0x3E7A28, 0.9f * a); }
        }
        if (age >= 0f && fa <= 0.02f) return;
        float al = a * (age >= 0f ? fa : 1f);
        float tipX = x + (float) Math.sin(ang + sway * 0.05f) * h, tipY = yb - (float) Math.cos(ang) * h + (age >= 0f ? 5 * sz * sm(age / 0.5f) : 0f) - hop;
        float midX = x + (tipX - x) * 0.5f + sway * 0.25f, midY = yb + (tipY - yb) * 0.5f;
        switch (type) {
            case WHEAT -> {
                line(x, yb, midX, midY, 0xA8923A, al); line(midX, midY, tipX, tipY, 0xC8AE48, al);
                line(x + 1, yb, midX + 1, midY, 0x7A6826, al * 0.8f);
                line(midX, midY + 2 * sz, midX - 4 * sz, midY - 2 * sz, 0x7DA03A, al); line(midX, midY + 3 * sz, midX + 4 * sz, midY - 1 * sz, 0x6A9030, al);
                for (int i = 0; i < 4; i++) {                                                                // the ear: grains in pairs, a light on the left
                    float gy2 = tipY + i * 1.3f; dot(tipX - 1, gy2, 0xF0CE50, al); dot(tipX + 1, gy2 + 0.5f, 0xC8A438, al); dot(tipX, gy2 - 0.4f, 0xFFE98A, al * 0.9f);
                }
                dot(tipX, tipY - 2, 0xE8D890, al * 0.8f);
            }
            case CARROT -> {
                for (int i = -1; i <= 1; i++) {
                    float lx = x + i * 3.2f * sz + (tipX - x) * 0.6f, ly = yb - h * (i == 0 ? 1f : 0.75f);
                    line(x, yb, lx, ly, i == 0 ? 0x5FB040 : 0x4A9034, al); dot(lx, ly - 1, 0x7CD05A, al * 0.9f);
                }
                dot(x, yb - 0.5f, 0xFF8A2A, al); dot(x + 1, yb, 0xE06A18, al);                              // the carrot's shoulder
            }
            case SUN -> {
                line(x, yb, midX, midY, 0x4E8A2E, al); line(midX, midY, tipX, tipY, 0x5FA03A, al);
                line(midX, midY + 3 * sz, midX - 5 * sz, midY, 0x6FB040, al); line(midX, midY + 4 * sz, midX + 5 * sz, midY + 1 * sz, 0x4E8A2E, al);
                float pr = 4.6f * sz, spin = t * 0.5f;
                for (int i = 0; i < 10; i++) { float an = spin + i * 0.6283f; dot(tipX + (float) Math.cos(an) * pr, tipY + (float) Math.sin(an) * pr * 0.9f, i % 2 == 0 ? 0xFFD23A : 0xFFE46A, al); }
                Ritual.fillE(tipX, tipY, 2.4f * sz, 2.4f * sz, 0x6B4423, al); dot(tipX - 0.6f, tipY - 0.6f, 0x9A6A38, al);
            }
            case CORN -> {
                line(x, yb, midX, midY, 0x6FA33A, al); line(midX, midY, tipX, tipY, 0x7FB644, al);
                line(midX, midY + 4 * sz, midX - 7 * sz, midY - 1 * sz, 0x5A9030, al); line(midX, midY + 6 * sz, midX + 7 * sz, midY + 2 * sz, 0x4E8028, al);
                Ritual.fillE(midX + 1, midY, 1.2f, 2.6f, 0xF0D048, al); dot(midX, midY - 1, 0xFFF0A0, al * 0.9f); dot(midX + 1, midY + 2, 0xC8A020, al);
                dot(tipX, tipY - 1, 0xC8A060, al); dot(tipX + 1, tipY, 0xA88848, al);
            }
            default -> {                                                                                     // pumpkin and melon
                float r = (type == PUMPKIN ? 5.6f : 5f) * sz, cx2 = x, cy2 = yb - r * 0.9f - hop;
                int body = type == PUMPKIN ? 0xE8832A : 0x4E9A3E, shade = type == PUMPKIN ? 0xB85A1A : 0x2E6A2A, hi = type == PUMPKIN ? 0xFFB45A : 0x86CC6A;
                Ritual.fillE(cx2, cy2 + 1, r * 1.05f, r * 0.55f, 0x000000, 0.25f * al);
                Ritual.fillE(cx2, cy2, r * 1.12f, r * 0.92f, shade, al);
                Ritual.fillE(cx2 - r * 0.12f, cy2 - r * 0.1f, r * 0.95f, r * 0.8f, body, al);
                Ritual.fillE(cx2 - r * 0.4f, cy2 - r * 0.38f, r * 0.34f, r * 0.26f, hi, al * 0.9f);
                for (int g2 = -1; g2 <= 1; g2++) line(cx2 + g2 * r * 0.45f, cy2 - r * 0.8f, cx2 + g2 * r * 0.55f, cy2 + r * 0.7f, mix(shade, 0x000000, 0.2f), al * 0.55f);
                dot(cx2, cy2 - r * 0.95f, 0x4E7A2E, al); dot(cx2 + 1, cy2 - r * 1.1f, 0x6AA03A, al);
                if (type == PUMPKIN) { float g3 = 0.5f + 0.5f * (float) Math.sin(t * 3f + seed); dot(cx2 + r * 0.3f, cy2 + 0.2f, 0xFFD070, al * 0.35f * g3); }
            }
        }
    }

    /** Grain, leaves, petals or seeds thrown off a crop at the moment of the cut. */
    private static void burst(float x, float y, float age, int seed, int type, float sz, float a) {
        for (int j = 0; j < 4; j++) {
            float vx = (rnd(seed * 5 + j) - 0.5f) * 44, vy = -(10 + rnd(seed * 3 + j + 9) * 26), life = 0.55f;
            if (age > life) continue;
            float px = x + vx * age, py = y + vy * age + 90 * age * age, f = 1f - age / life;
            int col = type == WHEAT ? (j % 2 == 0 ? 0xF0CE50 : 0xFFE98A) : type == SUN ? (j % 2 == 0 ? 0xFFD23A : 0x6B4423) : type == CORN ? (j % 2 == 0 ? 0xF0D048 : 0x7FB644) : type == CARROT ? (j % 2 == 0 ? 0x7CD05A : 0xFF8A2A) : (j % 2 == 0 ? 0xFFB45A : 0x86CC6A);
            dot(px, py, col, f * a); if (j == 0) dot(px + 1, py, col, f * a * 0.7f);
        }
    }

    /** The scythe: a bright arc sweeping across one third of the field, its trail fading behind the blade. */
    private static void sweep(float tau, boolean ltr, float xa, float xb, float yMid, float sz, int rgb, float a) {
        float su = cl(tau / 0.14f), fade = 1f - cl((tau - 0.14f) / 0.4f);
        if (fade <= 0.01f) return;
        int n = 26;
        for (int i = 0; i <= n; i++) {
            float f = i / (float) n; if (f > su) break;
            float x = ltr ? xa + (xb - xa) * f : xb - (xb - xa) * f;
            float y = yMid - 5 * sz * (float) Math.sin(3.1416f * f) + (ltr ? 1f - f : f) * 2 * sz;
            float al = (float) Math.pow(f / Math.max(0.01f, su), 1.6f) * fade * a;
            dot(x, y, 0xFFFFFF, al); dot(x, y + 1, rgb, al * 0.85f); dot(x, y - 1, rgb, al * 0.4f); dot(x, y + 2, rgb, al * 0.3f);
        }
        if (su < 1f) {
            float x = ltr ? xa + (xb - xa) * su : xb - (xb - xa) * su, y = yMid - 5 * sz * (float) Math.sin(3.1416f * su) + (ltr ? 1f - su : su) * 2 * sz;
            Ritual.glint(x, y, 3, 0xFFFFFF, a); Ritual.glowE(x, y, 8 * sz + 3, 5 * sz + 2, rgb, 0.5f * a);
        }
        if (tau < 0.3f) Ritual.glowE((xa + xb) / 2, yMid + 3 * sz, (xb - xa) * 0.55f, 12 * sz, rgb, 0.3f * (1f - tau / 0.3f) * a);
    }

    // ------------------------------------------------------------------ the soil heaves, the drop is about to spring
    private static void heave(float p, float icx, float gy, float sz, int kind, float a, boolean rng) {
        float e = Ritual.eo(p / 0.45f), fade = 1f - sm((p - 0.9f) / 0.9f);
        float mh = (6 + 4 * (rng ? 1 : 0)) * sz * e * fade, mw = (14 + 6 * (rng ? 1 : 0)) * sz * (0.5f + 0.5f * e);
        int dirt = kind == FEAST ? 0x4A2C16 : 0x5A3E22;
        Ritual.fillE(icx, gy + 1 * sz, mw * 1.1f, mh * 0.5f + 0.5f, 0x000000, 0.35f * a * fade);
        Ritual.fillE(icx, gy + 1 * sz - mh * 0.3f, mw, mh, dirt, a * fade);
        Ritual.fillE(icx - mw * 0.2f, gy + 1 * sz - mh * 0.55f, mw * 0.55f, mh * 0.45f, mix(dirt, 0xFFFFFF, 0.28f), 0.8f * a * fade);
        for (int j = 0; j < 14; j++) {                                                                      // clods thrown up as it bursts
            float tau = p - 0.02f * j; if (tau < 0f || tau > 1.0f) continue;
            float vx = (rnd(j * 9) - 0.5f) * 90, vy = -(40 + rnd(j * 5) * 70);
            dot(icx + vx * tau, gy + vy * tau + 230 * tau * tau, j % 3 == 0 ? mix(dirt, 0xFFFFFF, 0.3f) : dirt, (1f - tau) * a);
        }
        float ct = sm(p / 0.3f) * (1f - sm((p - 0.7f) / 1f));                                               // a column of light up out of the soil
        for (int y = 0; y < gy; y += 1) {
            float f = y / Math.max(1f, gy), w = (2.4f + 2.6f * f) * sz * (rng ? 1.7f : 1f);
            for (int x = (int) -w; x <= w; x++) Ritual.cv.set(Math.round(icx + x), y, Ritual.A(ct * 0.34f * (0.2f + 0.8f * f) * (1f - Math.abs(x) / (w + 1f)) * a, mix(ENERGY[kind], 0xFFFFFF, 0.5f)));
        }
    }

    // ------------------------------------------------------------------ the pest
    private static void pestScene(float t, float O, float op, float p, float icx, float gy, float sz, boolean helios, float a, float rumble) {
        float rx = fieldRx(sz), ry = fieldRy(sz), cy = gy + 2 * sz, sp = 12 * sz, xl = icx - rx * 0.86f, xr = icx + rx * 0.86f;
        float t1 = BEAT[0] * O, t2 = BEAT[1] * O, t3 = BEAT[2] * O;
        float pluck = sm((op - t2) / (t3 - t2)), gulp = op - t3;
        // leafy crops, some nibbled: they stay standing, they are not the target
        for (int r = 0; r < 3; r++) {
            float yb = rowY(gy, sz, r);
            for (int i = 0; ; i++) {
                float x = xl + i * sp + (rnd(r * 53 + i) - 0.5f) * sp * 0.4f; if (x > xr) break;
                float yy = yb + (rnd(r * 17 + i * 3) - 0.5f) * 1.6f * sz;
                if (sq((x - icx) / rx) + sq((yy - cy) / ry) > 0.86f) continue;
                int seed = r * 101 + i * 7;
                float sway = (float) Math.sin(t * 2.1f + x * 0.2f + r) * 0.8f + rumble * (rnd(seed + (int) (t * 30)) - 0.5f) * 3f + (op > t1 && op < t3 ? -(x - (icx + 34 * sz)) * 0.004f * 12f : 0f);          // the leaves lean towards the vacuum
                crop(rnd(seed) < 0.5f ? CARROT : WHEAT, x, yy, sz, sway, seed, a * 0.9f, -1f, 1, t);
            }
        }
        float px = pestX(t, op, t1, icx, sz), py = gy + 3 * sz;                                              // where the pest is
        float vac = sm(op / (0.30f * O)) * (1f - sm((p - 0.2f) / 0.8f));                                      // the vacuum swings in, and out again after the drop
        float nx = icx + 36 * sz, ny = gy - 40 * sz;                                                         // the nozzle
        if (!helios) {
            if (op >= t1) suction(t, op, t1, t2, t3, px, py, nx, ny, sz, a);
            float pk = pluck;
            float qx = px + (nx - px) * pk * 0.9f, qy = py + (ny - py) * pk * 0.95f - (float) Math.sin(pk * 3.1416f) * 5 * sz;
            float sc = 1f - sm((op - t2 - (t3 - t2) * 0.55f) / ((t3 - t2) * 0.45f));
            if (op < t3 + 0.01f && sc > 0.02f) pest(qx, qy, 1.5f * sz * (0.3f + 0.7f * sc), op < t1 ? t : t * 0.4f, op >= t2, pk * 14f, a);
            vacuum(nx + (1f - vac) * 40 * sz, ny - (1f - vac) * 26 * sz, sz, op >= t1 && op < t3 + 0.35f, gulp, t, a * vac);
            if (gulp >= 0f) gulpCloud(px, py - 4 * sz, gulp, sz, a);
        } else {
            helios(t, op, t1, t2, t3, p, px, py, icx, gy, sz, a);
        }
    }

    private static float pestX(float t, float op, float t1, float icx, float sz) {
        if (op >= t1) return icx;                                                                            // it freezes in alarm and stays above its hole
        float u = op / Math.max(0.1f, t1);
        return icx + (float) Math.sin(t * 2.6f) * 30 * sz * (1f - 0.6f * u * u) + (float) Math.sin(t * 7.1f) * 2 * sz * 0.4f;
    }

    /** A pest: a round beetle on six legs, feelers up, wings flicking. `rot` spins it once it is lifted. */
    private static void pest(float x, float y, float k, float clock, boolean lifted, float rot, float a) {
        float body = 6.5f * k, hh = 4.6f * k;
        float wob = lifted ? 0f : (float) Math.sin(clock * 22f) * 0.6f * k;
        if (lifted) { Ritual.glowE(x, y, 9 * k, 6 * k, 0xFFFFFF, 0.2f * a); }
        for (int s = -1; s <= 1; s += 2) for (int l = 0; l < 3; l++) {                                      // legs: a tripod gait while it walks
            float ph = clock * 22f + l * 2.1f + (s > 0 ? 3.14f : 0f), step = lifted ? (float) Math.sin(rot + l) * 1.2f * k : (float) Math.sin(ph) * 1.1f * k;
            float lx = x + (l - 1) * 2.6f * k;
            line(lx, y + hh * 0.5f, lx + s * 3.4f * k + step, y + hh * 1.5f - (lifted ? 1.5f * k : 0f), 0x2A1810, a);
        }
        Ritual.fillE(x, y + wob, body, hh, 0x3A1A12, a);                                                      // dark rim, then the shell
        Ritual.fillE(x, y + wob - 0.3f, body * 0.92f, hh * 0.86f, 0xB8402A, a);
        Ritual.fillE(x - body * 0.28f, y + wob - hh * 0.38f, body * 0.45f, hh * 0.3f, 0xE8704A, 0.9f * a);
        line(x, y + wob - hh * 0.8f, x, y + wob + hh * 0.8f, 0x4A1A12, 0.7f * a);                            // the seam between the wing cases
        dot(x - body * 0.35f, y + wob, 0x2A1010, a); dot(x + body * 0.38f, y + wob + 0.5f, 0x2A1010, a);
        Ritual.fillE(x + body * 0.95f, y + wob, body * 0.34f, hh * 0.55f, 0x2A1410, a);                      // the head
        dot(x + body * 1.12f, y + wob - 0.6f, 0xFFE27A, a);
        float fl = (float) Math.sin(clock * 9f) * 0.8f * k;
        line(x + body * 1.1f, y + wob - hh * 0.4f, x + body * 1.6f, y + wob - hh * 1.5f + fl, 0x2A1410, a);
        line(x + body * 1.1f, y + wob - hh * 0.2f, x + body * 1.75f, y + wob - hh * 1.0f - fl, 0x2A1410, a);
    }

    /** Leaves and dust streaming from the crops into the nozzle, in a cone that gets wider towards the soil. */
    private static void suction(float t, float op, float t1, float t2, float t3, float px, float py, float nx, float ny, float sz, float a) {
        float on = sm((op - t1) / 0.12f) * (1f - sm((op - t3 - 0.1f) / 0.25f));
        if (on <= 0.01f) return;
        for (int i = 0; i < 26; i++) {
            float ph = rnd(i * 9 + 1), u = (t * (1.1f + 0.5f * rnd(i)) + ph) % 1f;
            float ang = (rnd(i * 5) - 0.5f) * 1.5f * (1f - u * 0.6f), r0 = (22 + 26 * rnd(i * 3 + 2)) * sz;
            float sx = px + (float) Math.sin(ang) * r0, sy = py - 2 * sz + (float) Math.abs(Math.cos(ang)) * 3 * sz * rnd(i);
            float x = sx + (nx - sx) * u * u, y = sy + (ny + 4 * sz - sy) * u * u;
            int col = i % 3 == 0 ? 0xE8E2D0 : i % 3 == 1 ? 0x7CD05A : 0xB8986A;
            dot(x, y, col, on * (1f - u * 0.5f) * a); if (i % 2 == 0) dot(x + 1, y, col, on * 0.5f * a);
        }
        for (int s = -1; s <= 1; s += 2) line(nx + s * 3 * sz, ny + 3 * sz, px + s * 16 * sz, py - 2 * sz, 0xBFE8FF, 0.16f * on * a);          // the edges of the draught
    }

    /** The vacuum: a steel tank with an orange band, a hose curling up out of the frame and a flared nozzle that glows when it runs. */
    private static void vacuum(float nx, float ny, float sz, boolean running, float gulp, float t, float a) {
        if (a <= 0.01f) return;
        float shake = running ? (float) Math.sin(t * 60f) * 0.5f : 0f, gl = gulp >= 0f && gulp < 0.3f ? 1f + 0.3f * (1f - gulp / 0.3f) : 1f;
        for (int i = 0; i <= 30; i++) {                                                                       // the hose: from the tank, up and over, down to the nozzle
            float f = i / 30f, x = nx + 14 * sz + f * 40 * sz * (1f - f) * 2.2f + (1f - f) * 20 * sz, y = ny - 20 * sz * (float) Math.sin(f * 3.1416f) * 1.6f - (1f - f) * 26 * sz + f * 4 * sz;
            Ritual.fillE(x, y, 1.9f * sz + 0.6f, 1.9f * sz + 0.6f, i % 3 == 0 ? 0x4A5060 : 0x5E6578, a);
        }
        float ox = nx + shake, oy = ny;
        for (int y = 0; y < 8 * sz + 2; y++) {                                                                // the flared nozzle
            float f = y / (8 * sz + 2), w = (3.2f + 4.2f * f * f) * sz * gl;
            for (float x = -w; x <= w; x += 1f) Ritual.cv.set(Math.round(ox + x), Math.round(oy + y), Ritual.A(a, x < -w * 0.4f ? 0xD8E0EE : x > w * 0.45f ? 0x7A8498 : 0xAAB4C8));
            dot(ox - w, oy + y, 0x2A3040, a); dot(ox + w, oy + y, 0x2A3040, a);
        }
        for (float x = -7.5f * sz * gl; x <= 7.5f * sz * gl; x += 1f) dot(ox + x, oy + 8 * sz + 2, 0xFF8A2A, a);                // the orange lip
        if (running) { Ritual.glowE(ox, oy + 8 * sz + 3, 8 * sz, 4 * sz, 0xBFE8FF, 0.65f * a); dot(ox, oy + 8 * sz + 3, 0xFFFFFF, a); }
        Ritual.fillE(ox + 4 * sz, oy - 5 * sz, 6 * sz, 7 * sz, 0x8A93A8, a);                                    // the tank behind
        Ritual.fillE(ox + 3 * sz, oy - 6 * sz, 4.2f * sz, 5 * sz, 0xC4CCDC, 0.8f * a);
        for (float x = -5 * sz; x <= 5 * sz; x += 1f) dot(ox + 4 * sz + x, oy - 3 * sz, 0xFF8A2A, a);
        dot(ox + 4 * sz + 2, oy - 8 * sz, running ? 0x7CFF8A : 0xFF5A4A, a);                                     // the power light
    }

    private static void gulpCloud(float x, float y, float gulp, float sz, float a) {
        if (gulp > 0.9f) return;
        float f = gulp / 0.9f;
        for (int i = 0; i < 7; i++) {
            float ang = i * 0.9f, r = (5 + 11 * f) * sz;
            Ritual.glowE(x + (float) Math.cos(ang) * r, y + (float) Math.sin(ang) * r * 0.7f, (4 + 5 * f) * sz, (3 + 4 * f) * sz, 0xE8E2D0, 0.4f * (1f - f) * a);
        }
        for (int i = 0; i < 6; i++) { float an = i * 1.0472f + 0.4f, r = (3 + 16 * Ritual.eo(f)) * sz; Ritual.glint(x + (float) Math.cos(an) * r, y + (float) Math.sin(an) * r * 0.8f, 2, i % 2 == 0 ? 0xFFFFFF : 0xFFE27A, (1f - f) * a); }
        Ritual.glint(x, y, 4, 0xFFFFFF, (1f - f) * a);
    }

    // ------------------------------------------------------------------ the Ray of Helios: a sun burns the pest away
    private static void helios(float t, float op, float t1, float t2, float t3, float p, float px, float py, float icx, float gy, float sz, float a) {
        float rise = sm(op / (0.32f * Math.max(0.1f, t3 / 0.8f))) * (1f - sm((p - 0.2f) / 0.9f));
        float sx = icx, sy = gy - 66 * sz - (1f - rise) * 26 * sz, sr = 10 * sz * (0.8f + 0.2f * rise);
        Ritual.glowE(sx, sy, sr * 4.5f, sr * 4.5f, 0xFFE27A, 0.45f * rise * a);
        for (int i = 0; i < 14; i++) {                                                                        // the rays turn
            float an = t * 0.5f + i * 0.4488f, in = sr * 1.15f, out = sr * (1.6f + 0.5f * (i % 2)) + 2 * sz;
            line(sx + (float) Math.cos(an) * in, sy + (float) Math.sin(an) * in, sx + (float) Math.cos(an) * out, sy + (float) Math.sin(an) * out, i % 2 == 0 ? 0xFFE27A : 0xFFB830, rise * a);
        }
        Ritual.fillE(sx, sy, sr, sr, 0xFFB830, rise * a); Ritual.fillE(sx - sr * 0.1f, sy - sr * 0.1f, sr * 0.82f, sr * 0.82f, 0xFFE27A, rise * a);
        Ritual.fillE(sx - sr * 0.28f, sy - sr * 0.3f, sr * 0.36f, sr * 0.3f, 0xFFFBD0, 0.9f * rise * a);
        float beam = sm((op - t1) / 0.16f) * (1f - sm((op - t3 - 0.1f) / 0.3f)), burn = sm((op - t2) / (t3 - t2));
        if (beam > 0.01f) {                                                                                   // the beam: wide and soft, a bright core
            for (int w = 5; w >= 0; w--) {
                float hw = (w * 1.1f + 0.8f) * sz * (1f + 0.15f * (float) Math.sin(t * 30f)), al = beam * a * (w == 0 ? 0.95f : 0.12f);
                for (float y = sy + sr; y < py; y += 1f) { float f = (y - sy) / Math.max(1f, py - sy); float cx2 = sx + (px - sx) * f; for (float x = -hw; x <= hw; x += 1f) dot(cx2 + x, y, w == 0 ? 0xFFFFFF : 0xFFE27A, al * (1f - Math.abs(x) / (hw + 1f))); }
            }
            Ritual.glowE(px, py, 12 * sz, 6 * sz, 0xFFFFFF, 0.6f * beam * a);
        }
        float sc = 1f - burn;
        if (op < t3 + 0.01f && sc > 0.03f) { float shake = (float) Math.sin(t * 50f) * 0.7f * sz * beam; pest(px + shake, py, 1.5f * sz * (0.5f + 0.5f * sc), t * 0.5f, false, 0f, a * (0.4f + 0.6f * sc)); }
        float gulp = op - t3;
        if (gulp >= 0f) { gulpCloud(px, py - 3 * sz, gulp, sz, a); for (int i = 0; i < 8; i++) { float u = cl(gulp / 0.8f); dot(px + (rnd(i * 3) - 0.5f) * 14 * sz * u, py - 4 * sz - u * 20 * sz * (0.5f + rnd(i)), i % 2 == 0 ? 0x4A4038 : 0xFFB830, (1f - u) * a); } }
    }
}
