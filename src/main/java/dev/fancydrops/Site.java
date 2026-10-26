package dev.fancydrops;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The construction-site scene of the WIP pages (a port of the approved mockup, in its own 421 x 169 pixel grid with the ground at y 150). The page moves it so the ground lands on the panel's ground line. */
final class Site {
    private Site() { }

    static final int HX = 150, GY = 150;
    private static final float LOOP = 10.2f, TC = 3.0f, LD0 = 4.25f, LDP = 1.3f;

    // ---- the house: every brick has a home, a way down and a rest place
    private static final int NB; private static final float[][] B;
    static {
        java.util.List<float[]> l = new java.util.ArrayList<>();
        for (int r = 0; r < 11; r++) {
            int cols = r < 8 ? 8 : (r == 8 ? 7 : r == 9 ? 5 : 3), x0 = HX - cols * 3;
            for (int c = 0; c < cols; c++) {
                int i = l.size(), n = i + 1; float hx = x0 + c * 6, hy = GY - (r + 1) * 3;
                float d = r * 0.012f + rnd(n) * 0.12f, vx = (c - cols / 2f + 0.5f) * 8 + (rnd(n + 9) - 0.5f) * 16, vy = -rnd(n + 3) * 40;
                float rest = GY - 3 - (rnd(n + 5) < 0.35f ? 3 : 0) - (rnd(n + 7) < 0.12f ? 3 : 0), rx = Math.max(HX - 50, Math.min(HX + 46, hx + vx * 0.55f + (rnd(n + 1) - 0.5f) * 8));
                int ld = r < 3 ? 0 : r < 6 ? 1 : 2;
                l.add(new float[]{r, c, hx, hy, ld, d, vx, vy, rest, rx, new int[]{0xB5482E, 0xA03C26, 0xC4573A}[(i * 7) % 3], rnd(n + 11) < 0.18f ? 1 : 0});
            }
        }
        NB = l.size(); B = l.toArray(new float[0][]);
    }

    static float rnd(float n) { double s = Math.sin(n * 127.1 + 311.7) * 43758.5453; return (float) (s - Math.floor(s)); }
    static float sm(float x) { x = Math.max(0f, Math.min(1f, x)); return x * x * (3f - 2f * x); }
    static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    static float clamp(float x) { return Math.max(0f, Math.min(1f, x)); }

    static void R(GuiGraphicsExtractor g, float x, float y, float w, float h, int rgb) { int x0 = Math.round(x), y0 = Math.round(y); g.fill(x0, y0, x0 + Math.round(w), y0 + Math.round(h), 0xFF000000 | rgb); }
    static void RA(GuiGraphicsExtractor g, float x, float y, float w, float h, int rgb, float a) { int x0 = Math.round(x), y0 = Math.round(y); g.fill(x0, y0, x0 + Math.round(w), y0 + Math.round(h), Gfx.argb(Math.round(255 * a), rgb)); }
    static void ell(GuiGraphicsExtractor g, float cx, float cy, float rx, float ry, int rgb, float a) {
        int al = Math.round(255 * a); if (al <= 0) return;
        for (int dy = (int) -ry; dy <= (int) ry; dy++) { float k = dy / ry, hw = rx * (float) Math.sqrt(Math.max(0f, 1f - k * k)); g.fill(Math.round(cx - hw), Math.round(cy) + dy, Math.round(cx + hw), Math.round(cy) + dy + 1, Gfx.argb(al, rgb)); }
    }
    /** A light cone from a small top width to a wide bottom. */
    static void cone(GuiGraphicsExtractor g, float cx, float ty, float topHalf, float by, float botHalf, int rgb, float a) {
        int al = Math.round(255 * a); if (al <= 0) return; int n = Math.round(by - ty);
        for (int y = 0; y < n; y++) { float hw = topHalf + (botHalf - topHalf) * y / n; g.fill(Math.round(cx - hw), Math.round(ty) + y, Math.round(cx + hw), Math.round(ty) + y + 1, Gfx.argb(al, rgb)); }
    }
    static void brick(GuiGraphicsExtractor g, float x, float y, int col, boolean vert) {
        int w = vert ? 3 : 6, h = vert ? 6 : 3; R(g, x, y, w, h, col); R(g, x, y + h - 1, w, 1, 0x5A2416); R(g, x, y, w, 1, 0xE08A62); if (!vert) R(g, x + 3, y, 1, h - 1, 0x7A2F1C);
    }
    /** A builder 12 px tall, feet on y. */
    static void worker(GuiGraphicsExtractor g, float x, float y, int f, int hc, int bc, float w, boolean up, boolean arm2) {
        float lg = (float) Math.sin(w) * 1.6f;
        R(g, x - 2 + lg * 0.4f, y - 3, 2, 3, 0x2A2A38); R(g, x + 1 - lg * 0.4f, y - 3, 2, 3, 0x2A2A38);
        R(g, x - 2, y - 8, 5, 5, bc); R(g, x - 2, y - 6, 5, 1, 0xD8D8D0);
        R(g, x - (f > 0 ? 3 : -3), y - 8 + (up ? -2 : 1), 1, up ? 4 : 3, bc);
        R(g, x + (f > 0 ? 3 : -4), y - 8 + (arm2 ? -2 : 1), 1, arm2 ? 4 : 3, bc);
        R(g, x - 1, y - 11, 3, 3, 0xF0C8A0); R(g, x - 2, y - 12, 5, 2, hc); R(g, x - 2 + (f > 0 ? 2 : -1), y - 11, 1, 1, hc); R(g, x + (f > 0 ? 1 : -1), y - 10, 1, 1, 0x222222);
    }
    static void sparkAt(GuiGraphicsExtractor g, float x, float y, float t, int n) {
        int fr = (int) Math.floor(t * 14);
        for (int i = 0; i < n; i++) { float a = rnd(i + fr * 5) * 6.28f, d = 2 + rnd(i + 3 + fr) * 7; R(g, x + (float) Math.cos(a) * d, y + (float) Math.sin(a) * d * 0.8f + d * 0.3f, 1, 1, i % 2 == 1 ? 0xFFE08A : 0xFFFFFF); }
    }
    private static void dust(GuiGraphicsExtractor g, float x, float y, float r, float a) { ell(g, x, y, r, r, 0xA89680, a); }

    /** Draws the whole scene. {@code x} and {@code y} are the panel's corner, {@code w} its width, {@code h} its height. */
    static void draw(GuiGraphicsExtractor g, int px, int py, int pw, int ph, float T) {
        float t = T % LOOP;
        int ox = px + pw / 2 - 210, oy = py + ph - 12 - GY;                                              // ground line of the panel = GY
        float shk = t > TC - 0.02f && t < TC + 0.5f ? 1f - (t - TC) / 0.5f : 0f;
        int sx = Math.round((float) Math.sin(t * 90) * 1.5f * shk), sy = Math.round((float) Math.cos(t * 70) * shk);
        g.enableScissor(px + 6, py + 10, px + pw - 6, py + ph - 9);
        g.fill(px + 6, py + 10, px + pw - 6, py + ph - 9, 0xFF0D0B09);
        Gfx.push(g); Gfx.translate(g, ox + sx, oy + sy);
        // sky, stars, moon, clouds
        for (int i = 0; i < 26; i++) { float x = 14 + rnd(i) * 390, y = 20 + rnd(i + 40) * 55; RA(g, x, y, 1, 1, 0xE8DCC0, 0.35f + 0.25f * (float) Math.sin(T * 2 + i * 1.7) + 0.25f); }
        ell(g, 292, 26, 16, 16, 0xFFE6A0, 0.06f); ell(g, 292, 26, 11, 11, 0xFFE6A0, 0.08f); R(g, 286, 20, 12, 12, 0xE8D9A0); R(g, 288, 22, 3, 3, 0xCDBB80); R(g, 293, 27, 3, 3, 0xCDBB80);
        for (int i = 0; i < 3; i++) { float x = ((T * (3 + i * 1.3f) + i * 150) % 501f) - 40, y = 34 + i * 13; RA(g, x, y, 26, 2, 0x2C261D, 0.5f); RA(g, x + 5, y - 2, 14, 2, 0x2C261D, 0.5f); }
        for (int k = 0; k < 18; k++) RA(g, 8, GY - 75 + k * 75 / 18f, 405, 75 / 18f + 1, 0x50391F, 0.6f * (k / 17f));      // a warm haze on the horizon
        // far layers
        for (int i = 0; i < 28; i++) { float bw = 9 + rnd(i + 50) * 9, bx = i * 15 - 4, bh = 14 + rnd(i + 59) * 30; R(g, bx, GY - bh, bw, bh, 0x241D14); if (i % 5 == 0) R(g, bx + bw / 2, GY - bh - 6, 1, 6, 0x241D14); }
        for (int cx : new int[]{200, 330}) { int hh = 64; R(g, cx, GY - hh, 2, hh, 0x2C2418); R(g, cx - 14, GY - hh, 34, 2, 0x2C2418); R(g, cx + 12, GY - hh + 2, 1, 8, 0x2C2418); if (Math.sin(T * 2.5 + cx) > 0) R(g, cx, GY - hh - 3, 2, 2, 0xC03030); }
        R(g, 240, GY - 34, 16, 10, 0x221B12); R(g, 239, GY - 35, 18, 2, 0x2C2418); R(g, 242, GY - 24, 2, 24, 0x221B12); R(g, 252, GY - 24, 2, 24, 0x221B12);
        for (int i = 0; i < 20; i++) { float bw = 12 + rnd(i) * 10, bx = i * 22 - 4, bh = 18 + rnd(i + 9) * 40; R(g, bx, GY - bh, bw, bh, 0x1B1712); for (int k = 0; k < 4; k++) if (rnd(i * 4 + k) < 0.4f) RA(g, bx + 2 + (k % 2) * 5, GY - bh + 4 + (k >> 1) * 9, 2, 2, 0x6A5326, 0.5f + 0.3f * (float) Math.sin(T * 0.7 + i + k)); }
        frame(g, 56, 48, 4, 3, T); frame(g, 104, 12, 3, 11, T); frame(g, 212, 36, 3, 5, T); frame(g, 262, 24, 5, 8, T); frame(g, 360, 34, 4, 13, T);
        R(g, 8, GY, 405, 8, 0x221C14); R(g, 8, GY, 405, 1, 0x3A3022);
        // the crane
        R(g, 40, 52, 6, GY - 52, 0xD09A14); for (int y = 56; y < GY; y += 8) { R(g, 40, y, 6, 1, 0x8A6A0C); R(g, 41, y, 1, 8, 0x8A6A0C); } R(g, 38, GY - 3, 10, 3, 0xB88810);
        R(g, 14, 50, 146, 5, 0xE0A818); R(g, 14, 55, 146, 1, 0x8A6A0C); R(g, 36, 44, 14, 6, 0xE0A818); R(g, 18, 52, 10, 6, 0x5A5A60);
        boolean bl = Math.sin(T * 4) > 0; R(g, 42, 40, 2, 4, 0xE0A818); R(g, 41, 36, 4, 4, bl ? 0xFF3030 : 0x7F1818); if (bl) ell(g, 43, 38, 6, 6, 0xFF3030, 0.14f);
        // hook
        int k = (int) Math.floor((t - LD0) / LDP); float hy = 66, hsw = 0; boolean carry = false;
        if (t >= LD0 - 0.5f && t < LD0 + LDP * 3 + 0.6f) {
            int kk = Math.max(0, Math.min(2, k)); float u = (t - (LD0 + LDP * kk)) / LDP;
            if (t < LD0) hy = 66;
            else if (k >= 0 && k <= 2) {
                if (u < 0.3f) hy = lerp(66, GY - 14, sm(u / 0.3f));
                else if (u < 0.4f) { hy = GY - 14; carry = true; }
                else if (u < 0.65f) { hy = lerp(GY - 14, 58, sm((u - 0.4f) / 0.25f)); carry = true; hsw = (float) Math.sin((u - 0.4f) * 18) * 3 * (1 - (u - 0.4f) / 0.25f); }
                else if (u < 1f) { float stop = new float[]{GY - 13, GY - 22, GY - 37}[kk]; hy = lerp(58, stop - 12, sm((u - 0.65f) / 0.35f)); carry = true; hsw = (float) Math.sin((u - 0.4f) * 18) * 1.2f * (1 - (u - 0.65f) / 0.35f); }
            } else if (k >= 3) { float uu = (t - (LD0 + LDP * 3)) / 0.6f; hy = lerp(GY - 49, 66, sm(uu)); }
        }
        R(g, HX, 55, 1, hy - 55, 0xBFBFC6); R(g, HX - 1, hy, 3, 3, 0x9A9AA2); R(g, HX - 3, 55, 7, 3, 0x5A5A60);
        // scaffold with the welder, the bench and the three builders
        final int BX = 288;
        R(g, BX - 18, GY - 66, 2, 66, 0x6A6A72); R(g, BX + 14, GY - 66, 2, 66, 0x6A6A72); for (int y = GY - 62; y < GY - 2; y += 9) R(g, BX - 16, y, 30, 1, 0x4A4A52);
        for (int yy = GY - 64; yy < GY - 4; yy += 18) for (int q = 0; q < 9; q++) R(g, BX - 16 + q * 3.3f, yy + q * 2, 2, 2, 0x4A4A52);
        R(g, BX - 19, GY - 48, 36, 2, 0x6A4A2A); R(g, BX - 19, GY - 66, 36, 2, 0x6A4A2A); R(g, BX - 19, GY - 47, 36, 1, 0x3A2A16);
        worker(g, BX - 4, GY - 48, 1, 0x888890, 0x707078, 0, false, true);
        boolean on = Math.sin(T * 9) > -0.3; if (on) { sparkAt(g, BX + 4, GY - 52, T, 9); ell(g, BX + 4, GY - 52, 5, 5, 0xBEE1FF, 0.25f); R(g, BX + 3, GY - 53, 2, 2, 0xFFFFFF); }
        R(g, BX - 30, GY - 14, 40, 2, 0x6A4A2A); R(g, BX - 28, GY - 12, 2, 12, 0x6A4A2A); R(g, BX + 6, GY - 12, 2, 12, 0x6A4A2A);
        boolean scare = t > TC - 0.1f && t < TC + 1.3f; double wob = Math.sin(T * 3);
        worker(g, BX - 24, GY - 14, 1, 0xF2B01A, 0xF06A1A, 0, false, scare); worker(g, BX - 12, GY - 14, -1, 0xF0F0E8, 0x70C050, 0, false, !scare && wob > 0.5); worker(g, BX, GY - 14, -1, 0xF2B01A, 0xF06A1A, 0, false, false);
        R(g, BX - 8, GY - 17, 3, 3, 0xE8E0D0); for (int i = 0; i < 3; i++) { float p = (T * 0.9f + i / 3f) % 1f; RA(g, BX - 7 + (float) Math.sin(p * 7 + i) * 1.5f, GY - 19 - p * 9, 1, 2, 0xDDDDDD, 0.5f * (1 - p)); }
        int bx = BX - 22, by = GY - 34; R(g, bx, by, 16, 10, 0xF4F4EE); R(g, bx + 3, by + 10, 3, 2, 0xF4F4EE);
        if (scare) { R(g, bx + 7, by + 2, 2, 4, 0xD02020); R(g, bx + 7, by + 7, 2, 1, 0xD02020); } else { int d = (int) Math.floor(T * 2.4) % 4; for (int i = 0; i < 3; i++) R(g, bx + 3 + i * 4, by + 4, 2, 2, i < d ? 0x666666 : 0xC8C8C0); }
        R(g, 344, GY - 9, 8, 9, 0xF06A1A); R(g, 346, GY - 6, 4, 1, 0xF4F4EE); R(g, 342, GY - 1, 12, 1, 0x222222);
        R(g, 362, GY - 8, 14, 8, 0xC8302A); R(g, 366, GY - 10, 6, 2, 0x9A2018); R(g, 358, GY - 3, 22, 3, 0xB08850);
        R(g, 386, GY - 8, 6, 8, 0xE0701A); R(g, 386, GY - 6, 6, 1, 0xF4F4EE);
        // lamps
        float[][] lamps = {{96, 40, 0.7f}, {250, 34, 2.1f}, {330, 44, 4.2f}, {398, 46, 0f}, {BX, -1, 1.3f}};
        for (float[] lp : lamps) {
            int lx = (int) lp[0], lh = (int) lp[1]; float ph0 = lp[2], f = t > TC && t < TC + 0.9f && lx < 260 ? (Math.sin(t * 45 + ph0) > 0 ? 1f : 0.4f) : (0.93f + 0.07f * (float) Math.sin(T * 23 + ph0 * 5));
            if (lh < 0) { int ly = GY - 70; R(g, lx - 4, ly, 8, 3, 0x5A5A62); R(g, lx - 1, ly + 3, 2, 2, 0xFFE28A); cone(g, lx, ly + 4, 1, GY, 34, 0xFFE28C, 0.07f * f); continue; }
            R(g, lx, GY - lh, 2, lh, 0x4A4A52); R(g, lx - 5, GY - lh - 2, 12, 3, 0x5A5A62); R(g, lx - 2, GY - lh + 1, 6, 2, 0xFFE28A);
            cone(g, lx + 1, GY - lh + 3, 5, GY, 30, 0xFFE28C, 0.1f * f); ell(g, lx + 1, GY - lh + 2, 7, 4, 0xFFE28C, 0.12f * f);
        }
        { int lx = 70; float f = 0.9f + 0.1f * (float) Math.sin(T * 17); R(g, lx, 55, 1, 8, 0x7A7A82); R(g, lx - 2, 63, 5, 2, 0x5A5A62); R(g, lx - 1, 65, 3, 1, 0xFFE28A); cone(g, lx + 0.5f, 65, 1, GY, 29, 0xFFE28C, 0.07f * f); }
        // the foreman
        float fxp = HX - 62, fw = 0; boolean fup = false, fa2 = false; int ff = 1;
        if (t < 2.0f) { fxp = HX - 64 + (float) Math.sin(t * 0.8) * 4; fw = t * 8; ff = Math.cos(t * 0.8) > 0 ? 1 : -1; }
        else if (t < TC) { fxp = HX - 62; fup = t > 2.3f && t < 2.55f; }
        else if (t < TC + 0.9f) { fxp = lerp(HX - 62, HX - 76, sm((t - TC) / 0.5f)); fw = t * 16; fup = true; }
        else if (t < 4.2f) fxp = HX - 76;
        else if (t < 8.6f) { fxp = HX - 76 + (float) Math.sin(t * 1.1) * 3; fa2 = Math.sin(t * 3) > 0; fup = t % LDP > 1.0f; }
        else { fxp = HX - 76; fa2 = true; fup = true; }
        float jump = t > 2.3f && t < 2.55f ? (float) Math.sin((t - 2.3f) / 0.25f * Math.PI) * 4 : 0;
        // the bricks
        boolean standing = t < TC; float shake = t > 2.0f && t < TC ? Math.round(Math.sin(t * 60) * (0.4 + (t - 2) * 0.8)) : 0;
        for (int i = 0; i < NB; i++) {
            float[] b = B[i]; int row = (int) b[0], ld = (int) b[4]; float hx = b[2], hyb = b[3], rest = b[8]; int col = (int) b[10];
            float placeT = LD0 + LDP * ld + LDP * 0.98f + (row % 3) * 0.045f, pickT = LD0 + LDP * ld + LDP * 0.4f;
            if (standing) { brick(g, hx + shake * (row % 2 == 1 ? 1 : -1) * 0.5f, hyb, col, false); continue; }
            if (t >= placeT) { float u = t - placeT; brick(g, hx, hyb - (u < 0.17f ? Math.max(0f, 2 - u * 12) : 0), col, false); continue; }
            if (t >= pickT) continue;
            float tau = t - TC - b[5]; if (tau < 0) { brick(g, hx, hyb, col, false); continue; }
            float gr = 300, a = 0.5f * gr, vy = b[7], th = (-vy + (float) Math.sqrt(vy * vy + 4 * a * (rest - hyb))) / (2 * a), y; boolean fly = true;
            if (tau < th) y = hyb + vy * tau + a * tau * tau;
            else { float v = (vy + gr * th) * -0.28f, u = tau - th, tb = Math.max(0f, 2 * -v / gr); if (u < tb) y = rest + v * u + a * u * u; else { y = rest; fly = false; } }
            float x = lerp(hx, b[9], 1 - (float) Math.pow(1 - clamp(tau / (th + 0.28f)), 2));
            boolean vert = (b[11] > 0 && !fly) || (fly && tau < th + 0.3f && (int) Math.floor(tau * 10) % 2 == 1);
            brick(g, x, Math.min(y, rest), col, vert);
        }
        if (t > 2.5f && t < TC) { float u = t - 2.5f; R(g, HX + 8, Math.min(GY - 3, GY - 33 + u * u * 200), 6, 3, 0xB5482E); }
        boolean bodyOK = standing || t >= LD0 + LDP * 1.98f + 0.1f;
        if (bodyOK) { boolean win = standing || t > 8.7f; R(g, HX + 6, GY - 18, 8, 8, win ? 0xFFD058 : 0x2A2018); R(g, HX + 9, GY - 18, 1, 8, 0x4A3010); R(g, HX + 6, GY - 14, 8, 1, 0x4A3010); if (win) ell(g, HX + 10, GY - 14, 10, 10, 0xFFD058, 0.08f); R(g, HX - 18, GY - 12, 6, 12, 0x6A4020); R(g, HX - 13, GY - 6, 1, 1, 0xFFD058); }
        // dust
        if (t > TC - 0.3f && t < TC + 2.2f) for (int i = 0; i < 34; i++) { float st = TC + rnd(i) * 0.5f, u = t - st; if (u < 0 || u > 1.6f) continue; float dir = (rnd(i + 3) - 0.5f) * 2, x = HX + dir * (18 + u * 30), y = GY - 2 - rnd(i + 8) * 14 - u * 8, r = 3 + u * 8 * (0.6f + rnd(i + 5)); dust(g, x, y, r, 0.28f * (1 - u / 1.6f) * (1 - u / 1.6f)); }
        if (t > 2.0f && t < TC) for (int i = 0; i < 6; i++) { float u = (t * 2 + i * 0.37f) % 1f; dust(g, HX - 22 + rnd(i) * 44, GY - 34 - rnd(i + 2) * 10 + u * 10, 1.5f, 0.35f * (1 - u)); }
        if (carry) { float xx = HX + hsw - 9, yy = hy + 8; R(g, HX + hsw, hy + 2, 1, 6, 0xBFBFC6); R(g, xx, yy, 18, 7, 0xA03C26); for (int i = 0; i < 3; i++) R(g, xx, yy + i * 2 + 1, 18, 1, 0x7A2F1C); R(g, xx, yy, 18, 1, 0xE08A62); R(g, xx - 1, yy + 2, 20, 1, 0x8A6A0C); R(g, xx - 1, yy + 5, 20, 1, 0x8A6A0C); }
        worker(g, fxp, GY - jump, ff, 0xF2B01A, 0xF06A1A, fw, fup, fa2);
        if (t > 2.3f && t < 2.9f || t > TC && t < TC + 0.9f) { R(g, fxp - 3, GY - 26 - jump, 3, 8, 0xF4F4EE); R(g, fxp - 3, GY - 26 - jump, 3, 1, 0xD02020); R(g, fxp - 2, GY - 24 - jump, 1, 4, 0xD02020); R(g, fxp - 2, GY - 19 - jump, 1, 1, 0xD02020); }
        if (t > 8.7f && t < 9.6f) for (int i = 0; i < 8; i++) { float u = (t - 8.7f) / 0.9f, an = i / 8f * 6.28f + u; RA(g, HX + (float) Math.cos(an) * (10 + u * 26), GY - 30 + (float) Math.sin(an) * (8 + u * 14), 3, 1, 0xFFE08A, 1 - u); }
        // the nearest layers: fog and dark silhouettes in front
        for (int i = 0; i < 3; i++) { float fx2 = ((T * (2 + i) + i * 140) % 541f) - 60; RA(g, Math.max(8, fx2), GY - 12 - i * 8, 90, 6, 0xB8A888, 0.08f); }
        for (int[] s : new int[][]{{30, 18}, {388, 22}}) { R(g, s[0], GY - s[1] + 8, 3, s[1], 0x050403); R(g, s[0] - 6, GY - s[1] + 8, 15, 2, 0x050403); }
        R(g, 8, GY + 8, 405, 3, 0x070605); for (int x = 14; x < 411; x += 26) R(g, x, GY + 4, 2, 7, 0x070605);
        Gfx.pop(g); g.disableScissor();
    }

    /** An unfinished steel-frame building with tiny workers who hammer, walk planks or weld. */
    private static void frame(GuiGraphicsExtractor g, int x, int w, int fl, int seed, float T) {
        int fh = 14, n = w / 12; R(g, x - 2, GY - 3, w + 4, 3, 0x2A2217);
        for (int f = 0; f < fl; f++) {
            int y = GY - 3 - f * fh;
            for (int c = 0; c <= n; c++) R(g, x + c * 12, y - fh, 2, fh, 0x3A3022);
            R(g, x, y - fh, w + 2, 2, 0x4A3E2C);
            if (f < fl - 1) { for (int c = 0; c < n; c++) if (rnd(seed + f * 7 + c) < 0.5f) { R(g, x + c * 12 + 3, y - fh + 3, 8, 7, 0x15110C); if (rnd(seed + c + f) < 0.3f) RA(g, x + c * 12 + 4, y - fh + 4, 6, 5, 0x6A5326, 0.6f + 0.3f * (float) Math.sin(T + c)); } }
            else for (int c = 0; c <= n; c++) if (rnd(seed + c) < 0.5f) R(g, x + c * 12, y - fh - 5, 2, 5, 0x3A3022);
            if (f == 0) for (int q = 0; q < 14; q++) R(g, x + 2 + q * 10 / 14f, GY - 3 - q, 1, 1, 0x352B1D);
        }
        for (int yy = GY - 3 - fl * fh + 2; yy < GY - 3; yy += 3) RA(g, x, yy, 6, 1, 0x5A5238, 0.25f);
        for (int wi = 0; wi < Math.min(fl, 3); wi++) {
            int fy = GY - 3 - wi * fh - fh + 2 + fh - 3, kind = (wi + seed) % 3; float wx = x + 4 + rnd(seed + wi * 3) * (w - 14), ph = T * (1.6f + rnd(seed + wi)) + wi * 2;
            if (kind == 0) { mini(g, wx, fy, wi % 2); boolean sw = Math.sin(ph * 4) > 0; R(g, wx + (sw ? 2 : -1), fy - 6 - (sw ? 1 : 0), 1, 2, 0xCCCCCC); if (!sw) R(g, wx + 2, fy - 4, 2, 1, 0x7A5A30); }
            else if (kind == 1) { float p = (float) Math.sin(ph * 0.7) * 0.5f + 0.5f, pxx = x + 4 + p * (w - 12); mini(g, pxx, fy, (wi + 1) % 2); if (Math.cos(ph * 0.7) > 0) R(g, pxx, fy - 7, 1, 1, 0x6A6A72); R(g, pxx - 1, fy - 7, 4, 1, 0x9A7A3A); }
            else { mini(g, wx, fy, 1); if (Math.sin(T * 8 + wi) > -0.2) { sparkAt(g, wx + 3, fy - 1, T + wi, 5); R(g, wx + 2, fy - 2, 2, 2, 0xFFFFFF); } }
        }
    }
    private static void mini(GuiGraphicsExtractor g, float x, float y, int k) {
        R(g, x, y - 3, 1, 3, 0x1A1A24); R(g, x + 1, y - 3, 1, 3, 0x1A1A24); R(g, x, y - 6, 2, 3, k == 1 ? 0xE8742A : 0x8AC058); R(g, x, y - 8, 2, 2, 0xF2C030); R(g, x, y - 7, 2, 1, 0xF0C8A0);
    }
}
