package dev.fancydrops;
/** Layered backgrounds: far ridges in haze, mid layers, drifting mist and light shafts, plus a few foreground silhouettes. Every layer fades toward the edges so the scene has no box. */
final class Depth {
    private Depth() {}
    static final int W = SlayerScene.W, H = SlayerScene.H;
    static float h(int a, int b) { return Tex.h(a, b); }
    static float frac(double v) { return (float) (v - Math.floor(v)); }
    static float nz(float x, int s) { int i = (int) Math.floor(x); float f = x - i; f = f * f * (3 - 2 * f); return h(i, s) * (1 - f) + h(i + 1, s) * f; }
    static int al(float in, float a, int rgb) { return Canvas.rgb(Math.max(0, Math.min(255, (int) (a * in))), rgb); }
    static float edge(int x) { return Math.min(1f, Math.min(x / 18f, (W - 1 - x) / 18f)); }
    static void ell(Canvas c, int cx, int cy, int rx, int ry, int argb) {
        if (rx < 1 || ry < 1) return;
        for (int y = -ry; y <= ry; y++) { double v = y / (double) ry; int w = (int) Math.round(rx * Math.sqrt(Math.max(0, 1 - v * v))); if (w > 0) c.rect(cx - w, cy + y, cx + w + 1, cy + y + 1, argb); }
    }
    static void line(Canvas c, int x0, int y0, int x1, int y1, int argb) {
        int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)); if (n == 0) { c.set(x0, y0, argb); return; }
        for (int i = 0; i <= n; i++) c.set(x0 + Math.round((x1 - x0) * i / (float) n), y0 + Math.round((y1 - y0) * i / (float) n), argb);
    }
    static void tri(Canvas c, int x0, int y0, int x1, int y1, int x2, int y2, int argb) {
        int miny = Math.min(y0, Math.min(y1, y2)), maxy = Math.max(y0, Math.max(y1, y2));
        for (int y = miny; y <= maxy; y++) {
            float lo = 1e9f, hi = -1e9f; int[][] e = {{x0, y0, x1, y1}, {x1, y1, x2, y2}, {x2, y2, x0, y0}};
            for (int[] s : e) { if ((s[1] <= y && s[3] > y) || (s[3] <= y && s[1] > y)) { float x = s[0] + (y - s[1]) / (float) (s[3] - s[1]) * (s[2] - s[0]); lo = Math.min(lo, x); hi = Math.max(hi, x); } }
            if (hi >= lo) c.rect(Math.round(lo), y, Math.round(hi) + 1, y + 1, argb);
        }
    }
    /** A ridge line filled down to base, hazier (lower alpha) the further away. */
    static void ridge(Canvas c, float t, float in, int base, float amp, float fq, int seed, float drift, int rgb, float a) {
        for (int x = 0; x < W; x++) {
            float n = nz(x * fq + t * drift, seed) * 0.65f + nz(x * fq * 2.3f + t * drift * 1.7f, seed + 9) * 0.35f; int top = (int) (base - amp * n);
            for (int y = top; y <= base; y++) { float v = Math.min(1f, (y - top) / 4f + 0.35f); c.set(x, y, al(in * edge(x) * v, a, rgb)); }
        }
    }
    static void mist(Canvas c, float t, float in, int y, int rgb, float a, float spd, int seed, int n, int rx, int ry) {
        for (int i = 0; i < n; i++) { float ph = frac(t * spd + i / (float) n + h(i, seed) * 0.2f); int x = (int) (ph * (W + 2 * rx)) - rx;
            ell(c, x, y + (int) (h(i, seed + 3) * 8) - 4, rx, ry, al(in, a * (0.6f + 0.4f * (float) Math.sin(t * 0.6 + i)), rgb)); }
    }
    static void shaft(Canvas c, float t, float in, int x0, int w0, float slope, int len, int rgb, float a, int seed) {
        float fl = 0.78f + 0.22f * (float) Math.sin(t * 0.9 + seed);
        for (int y = 0; y < len; y++) { float f = 1f - y / (float) len; int xs = (int) (x0 + slope * y), w = (int) (w0 + y * 0.2f);
            for (int x = xs; x < xs + w; x++) { if (x < 0 || x >= W) continue; float ef = Math.min(1f, Math.min(x - xs, xs + w - 1 - x) / 3f + 0.2f); c.set(x, y, al(in * fl * f * ef * edge(x), a, rgb)); } }
    }
    static void motes(Canvas c, float t, float in, int n, int x0, int x1, int y0, int y1, int rgb, float a, float spd, int seed) {
        for (int i = 0; i < n; i++) { float ph = frac(t * spd + h(i, seed)); int x = x0 + (int) ((x1 - x0) * h(i, seed + 1) + Math.sin(ph * 6 + i) * 3), y = y0 + (int) ((y1 - y0) * ph);
            c.set(x, y, al(in * (float) Math.sin(ph * Math.PI), a, rgb)); }
    }
    static void bokeh(Canvas c, float t, float in, int n, int rgb, float a, int seed) {
        for (int i = 0; i < n; i++) { int x = (int) (h(i, seed) * W), y = (int) (h(i, seed + 2) * H), r = 2 + (i % 3); float tw = 0.5f + 0.5f * (float) Math.sin(t * 0.8 + i * 2.1);
            c.disc(x, y, r, al(in * tw * edge(x), a, rgb)); }
    }

    // ------------------------------------------------------------------ back layers (behind the scene's own props)
    static void back(Canvas c, int theme, float t, float in) {
        switch (theme) {
            case 0 -> { // graveyard: misty ridges, a dead forest, a lit chapel, moon shafts
                ridge(c, t, in, 62, 16, 0.05f, 21, 0.01f, 0x2E5048, 70);
                for (int x = 3; x < W; x += 4) { int hh = 6 + (int) (h(x, 3) * 9), b = 61 - (int) (nz(x * 0.05f + t * 0.01f, 21) * 6); line(c, x, b, x, b - hh, al(in * edge(x), 95, 0x24403A)); if (x % 8 == 3) { line(c, x, b - hh + 3, x - 2, b - hh + 1, al(in * edge(x), 85, 0x24403A)); line(c, x, b - hh + 5, x + 2, b - hh + 3, al(in * edge(x), 85, 0x24403A)); } }
                c.rect(84, 49, 91, 60, al(in, 170, 0x1E3430)); tri(c, 83, 49, 92, 49, 87, 42, al(in, 170, 0x1E3430)); c.rect(86, 52, 88, 55, al(in * (0.7f + 0.3f * (float) Math.sin(t * 5)), 235, 0xFFC860));
                mist(c, t, in, 63, 0x7FA898, 22, 0.018f, 1, 5, 28, 3);
                ridge(c, t, in, 70, 9, 0.09f, 33, 0.014f, 0x142A28, 150);
                shaft(c, t, in, 84, 3, -0.5f, 70, 0xCFE6C0, 24, 1); shaft(c, t, in, 92, 4, -0.7f, 70, 0xCFE6C0, 16, 2);
                motes(c, t, in, 14, 40, 96, 8, 70, 0xCFE6C0, 160, 0.05f, 4);
            }
            case 1 -> spider(c, t, in);
            case 2 -> { // sunny park: haze hills, a tree line, sun shafts, clouds, birds
                ell(c, 14, 10, 20, 20, al(in, 18, 0xFFF2B0)); ell(c, 14, 10, 13, 13, al(in, 30, 0xFFF2B0)); c.disc(14, 10, 6, al(in, 235, 0xFFF6C8));
                for (int i = 0; i < 3; i++) { float ph = frac(t * 0.012 + i * 0.37); int x = (int) (ph * (W + 70)) - 35, y = 12 + i * 9; ell(c, x, y, 17, 3, al(in * edge(Math.max(0, Math.min(W - 1, x))), 120, 0xFFFFFF)); ell(c, x + 6, y - 2, 9, 3, al(in, 120, 0xFFFFFF)); }
                ridge(c, t, in, 58, 14, 0.04f, 41, 0.004f, 0x7DB0C8, 80);
                ridge(c, t, in, 63, 8, 0.07f, 43, 0.006f, 0x4C8A6A, 120);
                for (int x = 2; x < W; x += 5) { int b = 62 - (int) (nz(x * 0.07f, 43) * 8) + 2, r = 3 + (int) (h(x, 5) * 3); c.disc(x, b - r, r, al(in * edge(x), 140, 0x3E7A52)); }
                shaft(c, t, in, 16, 6, 0.55f, 80, 0xFFF2B0, 20, 3); shaft(c, t, in, 26, 8, 0.7f, 80, 0xFFF2B0, 14, 5);
                ridge(c, t, in, 72, 6, 0.10f, 47, 0.008f, 0x3E8A3A, 150);
                for (int i = 0; i < 3; i++) { float ph = frac(t * 0.03 + i * 0.3); int x = (int) (ph * (W + 20)) - 10, y = 20 + i * 5 + (int) (Math.sin(ph * 14) * 2), fl = ((int) (t * 6 + i)) % 2; int col = al(in * edge(Math.max(0, Math.min(W - 1, x))), 200, 0x30404A); c.set(x, y, col); c.set(x - 1, y - fl, col); c.set(x + 1, y - fl, col); }
                motes(c, t, in, 12, 0, W, 10, 70, 0xFFF2B0, 150, 0.04f, 7);
            }
            case 3 -> { // the End: nebula, aurora, far islands, distant pillars, floor fog
                ell(c, 28, 24, 40, 22, al(in, 20, 0x6A2AD0)); ell(c, 84, 40, 34, 20, al(in, 18, 0xC040C0)); ell(c, 56, 12, 50, 10, al(in, 14, 0x40A0FF));
                for (int x = 0; x < W; x++) { float w = (float) (Math.sin(x * 0.09 + t * 0.5) * 6 + Math.sin(x * 0.04 - t * 0.3) * 4); int y0 = 14 + (int) w;
                    for (int k = 0; k < 14; k++) { float f = 1f - k / 14f; c.set(x, y0 + k, al(in * edge(x) * f, 30, k < 4 ? 0x60FFC0 : 0x9A60FF)); } }
                int[][] isl = {{20, 44, 14}, {96, 36, 10}, {66, 30, 7}};
                for (int[] s : isl) { int bob = (int) Math.round(Math.sin(t * 0.5 + s[0]) * 1.5); ell(c, s[0], s[1] + bob, s[2], 2, al(in, 95, 0xA09ACA)); tri(c, s[0] - s[2] + 2, s[1] + bob + 1, s[0] + s[2] - 2, s[1] + bob + 1, s[0], s[1] + bob + s[2] / 2 + 3, al(in, 80, 0x5A4A8A)); }
                for (int x : new int[]{34, 50, 76, 88}) { int hh = 16 + (int) (h(x, 2) * 12); c.rect(x - 2, 70 - hh, x + 2, 71, al(in, 150, 0x1A1230)); c.disc(x, 70 - hh - 3, 2, al(in * (0.7f + 0.3f * (float) Math.sin(t * 3 + x)), 200, 0xFF70E0)); }
                mist(c, t, in, 72, 0x6A2AB0, 26, 0.02f, 2, 5, 30, 4);
                motes(c, t, in, 18, 0, W, 6, 74, 0xD9B8FF, 150, 0.03f, 3);
            }
            case 4 -> { // the Nether: lava horizon, basalt ridge, a far fortress, heat haze, ash
                ell(c, 56, 74, 64, 10, al(in, 26, 0xFF7A20)); ell(c, 56, 74, 40, 5, al(in, 40, 0xFFAA40));
                ridge(c, t, in, 68, 20, 0.05f, 51, 0.006f, 0x3A1212, 120);
                int fx = 66; c.rect(fx, 56, fx + 30, 68, al(in, 170, 0x2A0E0E)); for (int x = fx; x < fx + 30; x += 4) c.rect(x, 53, x + 2, 56, al(in, 170, 0x2A0E0E)); c.rect(fx + 12, 44, fx + 18, 56, al(in, 170, 0x2A0E0E));
                for (int[] w : new int[][]{{fx + 4, 60}, {fx + 14, 48}, {fx + 22, 60}}) c.rect(w[0], w[1], w[0] + 2, w[1] + 3, al(in * (0.7f + 0.3f * (float) Math.sin(t * 6 + w[0])), 235, 0xFFA030));
                for (int x = 0; x < W; x++) { float sh = (float) Math.sin(x * 0.5 + t * 4); if (x % 2 == 0) c.set(x, 71 + (int) Math.round(sh), al(in * edge(x), 150, 0xFFC060)); }
                for (int y = 40; y < 72; y += 5) { int off = (int) (Math.sin(y * 0.4 + t * 3) * 1.5); c.rect(0 + off, y, W + off, y + 1, al(in, 6, 0xFFB060)); }
                ridge(c, t, in, 76, 9, 0.09f, 53, 0.01f, 0x240808, 150);
                for (int i = 0; i < 16; i++) { float ph = frac(t * 0.07 + h(i, 3)); c.set((int) (h(i, 4) * W + Math.sin(ph * 5 + i) * 4), (int) (ph * 80), al(in * (float) Math.sin(ph * Math.PI), 110, 0xC8A8A0)); }
            }
            case 5 -> { // vampire: mountains, a dead forest, crimson mist, far bats, moon rays
                ridge(c, t, in, 60, 22, 0.04f, 61, 0.003f, 0x2A0C18, 90);
                ridge(c, t, in, 66, 12, 0.07f, 63, 0.005f, 0x1A0812, 150);
                for (int x = 2; x < W; x += 3) { int b = 66 - (int) (nz(x * 0.07f + t * 0.005f, 63) * 12), hh = 5 + (int) (h(x, 6) * 8); tri(c, x - 1, b, x + 1, b, x, b - hh, al(in * edge(x), 175, 0x100408)); }
                mist(c, t, in, 62, 0xA02838, 24, 0.016f, 4, 5, 30, 3); mist(c, t, in, 70, 0xC03848, 30, 0.026f, 5, 5, 30, 3);
                shaft(c, t, in, 80, 4, -0.8f, 70, 0xFF6060, 16, 4); shaft(c, t, in, 88, 5, -0.5f, 70, 0xFF6060, 12, 6);
                for (int i = 0; i < 6; i++) { float ph = frac(t * 0.025 + i * 0.17); int x = (int) (ph * (W + 20)) - 10, y = 8 + (i % 3) * 6 + (int) (Math.sin(ph * 20 + i) * 2); int col = al(in * edge(Math.max(0, Math.min(W - 1, x))), 200, 0x100408); c.set(x, y, col); c.set(x - 1, y - 1, col); c.set(x + 1, y - 1, col); }
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ the Tarantula's cave
    static void web(Canvas c, float t, float in, int cx, int cy, int spokes, int rings, int rr, int rgb, float a, int seed) {
        float sway = (float) Math.sin(t * 0.7 + seed) * 1.2f;
        int[][] pts = new int[rings][spokes];
        for (int r = 1; r <= rings; r++) for (int s = 0; s < spokes; s++) { double an = s * Math.PI * 2 / spokes + 0.2; double rad = r * rr / (double) rings; pts[r - 1][s] = (int) Math.round(cx + Math.cos(an) * rad + sway * r / rings); }
        for (int s = 0; s < spokes; s++) { double an = s * Math.PI * 2 / spokes + 0.2; line(c, cx, cy, (int) Math.round(cx + Math.cos(an) * rr + sway), (int) Math.round(cy + Math.sin(an) * rr * 0.85), al(in, a * 0.9f, rgb)); }
        for (int r = 1; r <= rings; r++) for (int s = 0; s < spokes; s++) {
            double an0 = s * Math.PI * 2 / spokes + 0.2, an1 = (s + 1) * Math.PI * 2 / spokes + 0.2, rad = r * rr / (double) rings, sag = 1.5 * Math.sin((an0 + an1) / 2 + 1.3) * 0.5 + 1.0;
            int x0 = (int) Math.round(cx + Math.cos(an0) * rad + sway * r / rings), y0 = (int) Math.round(cy + Math.sin(an0) * rad * 0.85), x1 = (int) Math.round(cx + Math.cos(an1) * rad + sway * r / rings), y1 = (int) Math.round(cy + Math.sin(an1) * rad * 0.85);
            int mx = (x0 + x1) / 2 - (int) Math.round(Math.cos((an0 + an1) / 2) * sag), my = (y0 + y1) / 2 - (int) Math.round(Math.sin((an0 + an1) / 2) * sag);
            line(c, x0, y0, mx, my, al(in, a, rgb)); line(c, mx, my, x1, y1, al(in, a, rgb));
            if (h(r * 7 + s, seed) > 0.8f) c.set(mx, my, al(in * (0.5f + 0.5f * (float) Math.sin(t * 2.5 + r * 3 + s)), 230, 0xFFFFFF));          // dew
        }
    }
    static void spider(Canvas c, float t, float in) {
        // cavern depth: darker walls, lighter centre
        ell(c, 56, 50, 66, 50, al(in, 70, 0x120A0E)); ell(c, 56, 52, 50, 38, al(in, 50, 0x2A1416)); ell(c, 56, 56, 32, 24, al(in, 36, 0x4A1C1C));
        // far rock teeth (hazy), then near ones (dark, big)
        for (int x = 2; x < W; x += 6) { int hh = 6 + (int) (h(x, 11) * 12); tri(c, x - 3, 0, x + 3, 0, x, hh, al(in * edge(x), 90, 0x3A1E20)); }
        for (int x = 4; x < W; x += 9) { int hh = 7 + (int) (h(x, 12) * 11); tri(c, x - 3, 76, x + 3, 76, x, 76 - hh, al(in * edge(x), 90, 0x34181A)); }
        ridge(c, t, in, 72, 9, 0.08f, 71, 0.0f, 0x241012, 130);
        web(c, t, in, 36, 26, 11, 5, 32, 0xD8D8E8, 44, 3);
        web(c, t, in, 100, 12, 8, 4, 22, 0xD8D8E8, 40, 7);
        // light shaft with dust
        shaft(c, t, in, 70, 4, -0.35f, 74, 0xE0D0B0, 20, 5); motes(c, t, in, 12, 52, 76, 0, 74, 0xFFF0D0, 170, 0.05f, 8);
        // cocoons on swaying threads
        int[][] co = {{12, 30, 12}, {90, 26, 14}, {102, 40, 10}, {70, 18, 9}};
        for (int i = 0; i < co.length; i++) {
            float sw = (float) Math.sin(t * 0.8 + i * 1.7) * 2.2f; int x = co[i][0] + (int) sw, y = co[i][1], len = co[i][2];
            line(c, co[i][0], 0, x, y, al(in, 160, 0xD8D8E8)); ell(c, x, y + len / 2, 3 + len / 6, len / 2, al(in, 200, 0xB8B4C0)); ell(c, x - 1, y + len / 2 - 1, 1, len / 3, al(in, 120, 0xFFFFFF));
            for (int k = 3; k < len - 2; k += 3) c.rect(x - 3, y + k, x + 4, y + k + 1, al(in, 110, 0x7A7686));
            if (i == 1) ell(c, x, y + len / 2, 5, len / 2 + 2, al(in * (0.5f + 0.5f * (float) Math.sin(t * 2.2)), 40, 0xFF2020));                                   // something moves inside
        }
        // spiderlings on threads and the floor
        for (int i = 0; i < 4; i++) { float ph = frac(t * 0.13 + i * 0.27), up = (i % 2 == 0) ? ph : 1 - ph; int x = 8 + i * 28, y = 4 + (int) (up * 44); line(c, x, 0, x, y, al(in, 90, 0xD8D8E8)); c.rect(x - 1, y, x + 2, y + 2, al(in, 235, 0x0A0606)); c.set(x - 2, y - 1, al(in, 200, 0x0A0606)); c.set(x + 2, y - 1, al(in, 200, 0x0A0606)); c.set(x, y, al(in, 255, 0xFF3030)); }
        for (int i = 0; i < 7; i++) { float ph = frac(t * (0.05 + i * 0.006) + i * 0.15), dir = i % 2 == 0 ? 1 : -1; int x = (int) ((dir > 0 ? ph : 1 - ph) * (W + 10)) - 5, y = 79 + (i % 3) * 3, st = ((int) (t * 8 + i)) % 2; int col = al(in * edge(Math.max(0, Math.min(W - 1, x))), 235, 0x080404);
            c.rect(x, y, x + 3, y + 2, col); c.set(x - 1, y + st, col); c.set(x + 3, y + 1 - st, col); c.set(x, y - 1 + st, col); c.set(x + 2, y - 1 + 1 - st, col); }
        // glowing eyes in the dark
        int[][] eyes = {{6, 46}, {106, 54}, {12, 62}, {100, 34}, {18, 14}, {62, 8}};
        for (int i = 0; i < eyes.length; i++) { float cyc = frac(t * 0.17 + i * 0.31); if (cyc > 0.86f) continue; int x = eyes[i][0], y = eyes[i][1]; float f = 0.6f + 0.4f * (float) Math.sin(t * 2 + i); ell(c, x, y, 5, 3, al(in * f, 26, 0xFF2020)); c.set(x - 2, y, al(in, 255, 0xFF3030)); c.set(x + 2, y, al(in, 255, 0xFF3030)); c.set(x - 2, y - 1, al(in, 140, 0xFFA0A0)); c.set(x + 2, y - 1, al(in, 140, 0xFFA0A0)); }
        // drips into puddles, with ripples
        for (int k = 0; k < 3; k++) { int x = new int[]{22, 80, 46}[k], tip = new int[]{10, 14, 8}[k]; float cyc = frac(t * 0.42 + k * 0.33); if (cyc < 0.7f) { int y = tip + (int) (cyc / 0.7f * cyc / 0.7f * (78 - tip)); c.set(x, y, al(in, 230, 0xA8C8E8)); c.set(x, y - 1, al(in, 120, 0xA8C8E8)); }
            else { float r = (cyc - 0.7f) / 0.3f; ell(c, x, 79, 1 + (int) (r * 7), 1 + (int) (r * 1.2f), al(in * (1 - r), 120, 0xA8C8E8)); } }
        // poison pool
        ell(c, 16, 84, 17, 4, al(in, 70, 0x3AD040)); ell(c, 16, 83, 11, 2, al(in, 90, 0x80FF60));
        for (int i = 0; i < 3; i++) { float ph = frac(t * 0.4 + i * 0.31); c.set(10 + i * 6, 83 - (int) (ph * 10), al(in * (float) Math.sin(ph * Math.PI), 200, 0x9AFF70)); }
        // egg sacs and bones on the right
        for (int i = 0; i < 4; i++) { int x = 88 + i * 6, y = 86 + (i % 2) * 2; ell(c, x, y, 3, 2, al(in, 200, 0xCFC8B0)); c.set(x - 1, y - 1, al(in, 220, 0xFFFFFF)); }
        for (int i = 0; i < 4; i++) line(c, 96 + i * 3, 84, 98 + i * 3, 80, al(in, 190, 0xD8D0B8));
        mist(c, t, in, 80, 0x7A2A2A, 26, 0.02f, 9, 6, 26, 3);
    }

    // ------------------------------------------------------------------ foreground silhouettes (drawn in front of the mob, kept to the corners)
    static void front(Canvas c, int theme, float t, float in) {
        int dark = theme == 3 ? 0x0A0614 : theme == 4 ? 0x120404 : theme == 5 ? 0x08020A : theme == 1 ? 0x080404 : theme == 2 ? 0x1F5A28 : 0x070C0A;
        float sw = (float) Math.sin(t * 1.4);
        if (theme == 1) {                                                            // stalactite tips and hanging threads at the top corners, rock lips below
            tri(c, 0, 0, 14, 0, 4, 22, al(in, 235, dark)); tri(c, 98, 0, 112, 0, 106, 26, al(in, 235, dark)); tri(c, 18, 0, 26, 0, 21, 12, al(in, 235, dark));
            line(c, 9, 0, 9 + (int) sw, 40, al(in, 120, 0xD8D8E8)); line(c, 100, 0, 100 - (int) sw, 34, al(in, 120, 0xD8D8E8));
            tri(c, 0, H, 0, 80, 22, H, al(in, 235, dark)); tri(c, 112, H, 112, 78, 92, H, al(in, 235, dark));
            return;
        }
        for (int s = -1; s <= 1; s += 2) {
            int ox = s < 0 ? 0 : W - 1;
            for (int i = 0; i < 9; i++) { int x = ox + s * (i * 2 + (int) (h(i, 3) * 2)), hh = 8 + (int) (h(i, 5) * 14) - i; if (hh < 3) continue; int top = H - 1 - hh; int bend = (int) Math.round(sw * hh * 0.12 * (theme == 2 || theme == 0 ? 1 : 0.2));
                if (theme == 4 || theme == 5 || theme == 3) tri(c, x - 2, H - 1, x + 2, H - 1, x + bend * 0, top, al(in, 235, dark)); else line(c, x, H - 1, x + bend, top, al(in, 235, dark)); }
        }
        if (theme == 0) { int cx = 6; c.rect(cx, 78, cx + 2, 96, al(in, 235, dark)); c.rect(cx - 3, 82, cx + 5, 84, al(in, 235, dark)); }
        if (theme == 5) { for (int x = 70; x < W; x += 5) { c.rect(x, 82, x + 1, H, al(in, 235, dark)); c.set(x, 80, al(in, 235, dark)); } c.rect(70, 86, W, 87, al(in, 235, dark)); }
        mist(c, t, in, 92, theme == 5 ? 0xB04858 : theme == 4 ? 0xFF8A40 : 0x9AB8A8, 18, 0.03f, 8, 3, 34, 3);
    }
}
