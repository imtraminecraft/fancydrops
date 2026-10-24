package dev.fancydrops;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A playable mockup of the Mining and Fishing drop scenes that are still being built. Nothing here is saved. */
final class WipMock {
    // per scene: 0 Mining, 1 Fishing
    private static final int[] stage = new int[2], hits = new int[2];
    private static final long[] t0 = new long[2];
    private static final int[] caught = new int[2];
    private static final boolean[] plan = {true, true, true, false};
    private static final String[] PLAN = {"Animation", "Sounds", "Chat alert", "Library page"};
    private static final int[] planHint = {0, 0, 0, 0};
    private static int x0, y0, sx, sy, sw, sh, bx, bw;
    private static int cat;
    private static String msg = ""; private static long msgAt;

    private static float mh(int a, int b) { double v = Math.sin(a * 12.9898 + b * 78.233) * 43758.5453; return (float) (v - Math.floor(v)); }
    private static float cl(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float since(int c) { return (System.currentTimeMillis() - t0[c]) / 1000f; }
    private static void go(int c, int st) { stage[c] = st; t0[c] = System.currentTimeMillis(); }
    private static void say(String s) { msg = s; msgAt = System.currentTimeMillis(); }
    private static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int col) { g.fill(x, y, x + w, y + h, col); }
    private static void px(GuiGraphicsExtractor g, float x, float y, int s, int col) { int ix = Math.round(x), iy = Math.round(y); g.fill(ix, iy, ix + s, iy + s, col); }

    static void draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, int c, int mx, int my, int accent) {
        cat = c; x0 = x; y0 = y;
        sx = x + 8; sy = y + 24; sw = Math.max(90, (int) (w * 0.58f)); sh = h - 32;
        bx = sx + sw + 10; bw = x + w - 8 - bx;
        box(g, x, y, w, h, 0xFF16130B);
        Gfx.text(g, font, (c == 0 ? "Mining" : "Fishing") + " drop  ·  mockup", x + 10, y + 8, Ui.TEXT);
        String tag = "WIP  ·  try it"; int tw = Gfx.w(font, tag) + 10;
        box(g, x + w - tw - 8, y + 5, tw, 13, 0xFF3A3220); Gfx.text(g, font, tag, x + w - tw - 3, y + 8, 0xFFFFC857);
        if (stage[c] == 0 || hits[c] == 0 && stage[c] == 0) { }
        g.enableScissor(sx, sy, sx + sw, sy + sh);
        if (c == 0) mining(g, font); else fishing(g, font);
        g.disableScissor();
        g.outline(sx - 1, sy - 1, sw + 2, sh + 2, 0xFF3A3F52);
        boolean over = mx >= sx && mx < sx + sw && my >= sy && my < sy + sh;
        if (over && stage[c] == 0) { String hint = c == 0 ? "Click to mine" : "Click to cast"; Gfx.textCentered(g, font, hint, sx + sw / 2, sy + sh - 12, 0xFFFFFFFF); }
        if (!msg.isEmpty() && System.currentTimeMillis() - msgAt < 1400L) Gfx.textCentered(g, font, msg, sx + sw / 2, sy + 6, 0xFFFFC857);
        // the right side: what is planned, toggles to play with, and a counter
        Gfx.text(g, font, "Planned", bx, sy - 1, Ui.SUB);
        for (int i = 0; i < 4; i++) {
            int by = sy + 11 + i * 19; boolean hv = mx >= bx && mx < bx + bw && my >= by && my < by + 16;
            box(g, bx, by, bw, 16, plan[i] ? 0xFF1F2A44 : hv ? Ui.ROW_HOVER : Ui.ROW);
            if (plan[i]) box(g, bx, by + 3, 2, 10, accent);
            Gfx.text(g, font, Gfx.fit(font, PLAN[i], bw - 26), bx + 7, by + 4, plan[i] ? Ui.TEXT : Ui.SUB);
            box(g, bx + bw - 14, by + 5, 8, 6, plan[i] ? 0xFF2D7D46 : 0xFF3A3F52); box(g, bx + bw - (plan[i] ? 8 : 14), by + 5, 2, 6, 0xFFFFFFFF);
        }
        int py = sy + 11 + 4 * 19 + 4;
        Gfx.text(g, font, "Caught: " + caught[c], bx, py, Ui.SUB);
        long now = System.currentTimeMillis(); int fw = bw, fy = py + 12;
        box(g, bx, fy, fw, 5, 0xFF0A0D14);
        int fill = Math.round(fw * 0.62f); box(g, bx, fy, fill, 5, 0xFFC8962A);
        for (int q = -8 + (int) ((now / 70L) % 8); q < fill; q += 8) { int a = Math.max(bx, bx + q), b = Math.min(bx + fill, bx + q + 4); if (b > a) box(g, a, fy, b - a, 5, 0xFFE8B84A); }
        Gfx.text(g, font, "In the works", bx, fy + 8, Ui.DIM);
    }

    static void reset() { bw = 0; }

    static boolean click(int mx, int my) {
        if (bw <= 0) return false;
        for (int i = 0; i < 4; i++) { int by = sy + 11 + i * 19; if (mx >= bx && mx < bx + bw && my >= by && my < by + 16) { plan[i] = !plan[i]; return true; } }
        if (mx < sx || mx >= sx + sw || my < sy || my >= sy + sh) return false;
        int c = cat;
        if (c == 0) {
            if (stage[0] == 0) { hits[0]++; t0[0] = System.currentTimeMillis(); if (hits[0] >= 3) { go(0, 1); say("Rare ore!"); } }
        } else {
            switch (stage[1]) {
                case 0 -> go(1, 1);
                case 2 -> { say("Too early!"); go(1, 0); }
                case 3 -> go(1, 4);
                default -> { }
            }
        }
        return true;
    }

    private static void mining(GuiGraphicsExtractor g, Font font) {
        long now = System.currentTimeMillis(); float t = since(0);
        for (int yy = 0; yy < sh; yy += 2) { int v = 0x16 + (yy * 22 / sh); box(g, sx, sy + yy, sw, 2, 0xFF000000 | v << 16 | (v + 4) << 8 | (v + 12)); }
        for (int i = 0; i < 18; i++) { int rx = sx + (int) (mh(i, 1) * sw), ry = sy + (int) (mh(i, 2) * sh); box(g, rx, ry, 3 + i % 4, 2, 0x22FFFFFF); }
        box(g, sx, sy + sh - 18, sw, 18, 0xFF2B2E3A); box(g, sx, sy + sh - 18, sw, 2, 0xFF3C4052);
        int cx = sx + sw / 2, gy = sy + sh - 18, bs = 28;
        if (stage[0] == 0) {
            float shake = t < 0.2f && hits[0] > 0 ? (float) Math.sin(t * 90) * 2.5f * (1f - t / 0.2f) : 0f;
            int bxp = Math.round(cx - bs / 2 + shake), byp = gy - bs;
            box(g, bxp, byp, bs, bs, 0xFF6C6F7C); box(g, bxp, byp, bs, 3, 0xFF8A8D9A); box(g, bxp + bs - 3, byp, 3, bs, 0xFF50535F);
            for (int i = 0; i < 6; i++) { int ox = bxp + 3 + (int) (mh(i, 7) * (bs - 10)), oy = byp + 4 + (int) (mh(i, 8) * (bs - 10)); float gl = 0.6f + 0.4f * (float) Math.sin(now / 300.0 + i); box(g, ox, oy, 4, 4, Gfx.argb(255, 0x38D8E8)); box(g, ox + 1, oy + 1, 1, 1, Gfx.argb(Math.round(255 * gl), 0xFFFFFF)); }
            for (int k = 0; k < hits[0]; k++) { int ax = bxp + 6 + k * 7, ay = byp + 4; for (int j = 0; j < 12; j++) box(g, ax + (j % 2 == 0 ? 0 : 1) + (k == 1 ? j / 3 : -j / 4), ay + j, 1, 1, 0xFF14151C); }
            if (t < 0.18f && hits[0] > 0) {                                                       // the pickaxe swings in
                float u = t / 0.18f; for (int j = 0; j < 14; j++) { float a = -1.2f + u * 1.5f; px(g, bxp + bs + 6 - (float) Math.cos(a) * j, byp - 6 + (float) Math.sin(a) * j + 12, 2, j > 9 ? 0xFFB8BCC8 : 0xFF8A5A2E); }
                for (int i = 0; i < 6; i++) px(g, bxp + 6 + mh(i, hits[0]) * 18, byp + 8 + (float) Math.pow(t, 2) * 140 * mh(i, 3) - 6 * (1 - mh(i, 4)) , 2, 0xFF9A9DAA);
            }
        } else {
            float u = t;
            if (u < 0.45f) {                                                                       // the block bursts
                float f = u / 0.45f;
                for (int i = 0; i < 12; i++) px(g, cx + (mh(i, 1) - 0.5f) * 90 * f, gy - 16 - f * 30 * mh(i, 2) + f * f * 80, 4 - i % 2, i % 3 == 0 ? 0xFF38D8E8 : 0xFF6C6F7C);
                g.fill(sx, sy, sx + sw, sy + sh, Gfx.argb(Math.round(120 * (1f - f)), 0xFFFFFF));
            }
            float rise = Math.min(1f, u / 0.7f), er = 1f - (1f - rise) * (1f - rise);
            float iy = gy - 14 - er * 34 + (float) Math.sin(u * 3) * 1.5f;
            for (int r = 3; r >= 1; r--) { int rr = 12 + r * 6; for (int yy = -rr; yy <= rr; yy++) { int hw = (int) Math.sqrt(Math.max(0, rr * rr - yy * yy)); box(g, cx - hw, Math.round(iy) + yy, hw * 2, 1, Gfx.argb(Math.round(14 * (4 - r) * rise), 0x55E8FF)); } }
            float spin = (float) Math.cos(u * 5.5f);
            int wd = Math.max(2, Math.round(11 * Math.abs(spin)));
            for (int j = 0; j < 12; j++) { int hw = Math.max(1, Math.round((j < 6 ? j + 1 : 12 - j) * wd / 7f)); box(g, cx - hw, Math.round(iy) - 6 + j, hw * 2, 1, j < 5 ? 0xFFB8F6FF : j < 8 ? 0xFF38D8E8 : 0xFF1FA0B8); }
            for (int i = 0; i < 8; i++) { float a = u * 2 + i * 0.785f; px(g, cx + (float) Math.cos(a) * 26, iy + (float) Math.sin(a) * 9, i % 2 == 0 ? 3 : 2, 0xFFFFFFFF); }
            String nm = "Diamond  ·  Rare drop"; int tw2 = Gfx.w(font, nm) + 10, ty = sy + sh - 30;
            float la = cl((u - 0.5f) / 0.3f); if (la > 0) { box(g, cx - tw2 / 2, ty, tw2, 13, Gfx.argb(Math.round(190 * la), 0x0B0E2A)); Gfx.text(g, font, nm, cx - tw2 / 2 + 5, ty + 3, Gfx.argb(Math.round(255 * la), 0xFFC857)); }
            if (u > 2.6f) { caught[0]++; hits[0] = 0; go(0, 0); }
        }
    }

    private static void fishing(GuiGraphicsExtractor g, Font font) {
        long now = System.currentTimeMillis(); float t = since(1);
        int wy = sy + sh * 45 / 100;
        for (int yy = 0; yy < wy - sy; yy += 2) { int v = 40 + yy * 70 / Math.max(1, wy - sy); box(g, sx, sy + yy, sw, 2, 0xFF000000 | (v / 3) << 16 | (v / 2 + 20) << 8 | (v + 90)); }
        for (int yy = wy; yy < sy + sh; yy += 2) { int v = 30 + (yy - wy) * 30 / Math.max(1, sy + sh - wy); box(g, sx, yy, sw, 2, 0xFF000000 | 0x08 << 16 | (v + 40) << 8 | (v + 110)); }
        for (int i = 0; i < 4; i++) { int ry = wy + 6 + i * 11; for (int xx = 0; xx < sw; xx += 6) { int o = (int) (Math.sin((xx + now / 40.0) / 7 + i * 2) * 2); box(g, sx + xx, ry + o, 4, 1, 0x30FFFFFF); } }
        box(g, sx, wy, sw, 1, 0x80BDEBFF);
        int cx = sx + sw / 2; float bobX = cx, bobY = wy;
        if (stage[1] == 1) {
            float u = cl(t / 0.7f); bobX = sx + 12 + (cx - sx - 12) * u; bobY = wy - 36 * 4 * u * (1 - u);
            if (u >= 1f) { go(1, 2); }
        } else if (stage[1] >= 2) bobY = wy + (float) Math.sin(now / 260.0) * 1f;
        if (stage[1] == 2 && t > 1.3f + mh(caught[1] + 3, 5) * 1.2f) go(1, 3);
        if (stage[1] == 3) {
            bobY = wy + 3 + (float) Math.sin(t * 40) * 1.5f;
            for (int r = 0; r < 2; r++) { float rp = cl(((t * 1.6f) + r * 0.5f) % 1f); int rr = 4 + (int) (rp * 16); for (int i = 0; i < 24; i++) { double a = i * Math.PI / 12; px(g, cx + (float) Math.cos(a) * rr, wy + 1 + (float) Math.sin(a) * rr * 0.28f, 1, Gfx.argb(Math.round(200 * (1 - rp)), 0xFFFFFF)); } }
            Gfx.textCentered(g, font, "!", cx, Math.round(wy - 34 - (float) Math.abs(Math.sin(t * 12)) * 3), 0xFFFF5555);
            if (t > 1.1f) { say("It got away..."); go(1, 0); }
        }
        if (stage[1] >= 1 && stage[1] <= 3) {                                                        // the line from the rod
            int rx = sx + 6, ry = sy + 8;
            for (int i = 0; i <= 28; i++) { float u = i / 28f; px(g, rx + (bobX - rx) * u, ry + (bobY - ry) * u + (float) Math.sin(u * Math.PI) * 5, 1, 0x99FFFFFF); }
            box(g, sx, sy + 6, 10, 2, 0xFF7A4A24);
        }
        if (stage[1] <= 3) { int bxp = Math.round(bobX), byp = Math.round(bobY); if (stage[1] != 0) { box(g, bxp - 2, byp - 3, 5, 3, 0xFFE24A3A); box(g, bxp - 2, byp, 5, 3, 0xFFF2F2F2); box(g, bxp - 1, byp - 4, 3, 1, 0xFFE24A3A); } }
        if (stage[1] == 4) {
            float u = t, hgt = 4 * 38 * (u / 1.0f) * (1 - Math.min(1f, u / 1.0f)) + (u > 1f ? 0 : 0);
            float jump = u < 1f ? u : 1f; float iy = wy - 4 * 40 * jump * (1 - jump) - (u >= 1f ? 26 : 0) - (u >= 1f ? Math.min(1f, (u - 1f) / 0.4f) * 8 : 0);
            if (u < 1f) iy = wy - 4 * 44 * jump * (1 - jump);
            else iy = wy - 18 - Math.min(1f, (u - 1f) / 0.5f) * 14 + (float) Math.sin(u * 3) * 1.5f;
            float grow = Math.min(1f, u / 0.5f); int rr0 = Math.round(20 * grow);
            for (int r = 3; r >= 1; r--) { int rr = rr0 + r * 5; for (int yy = -rr; yy <= rr; yy++) { int hw = (int) Math.sqrt(Math.max(0, rr * rr - yy * yy)); box(g, cx - hw, Math.round(iy) + yy, hw * 2, 1, Gfx.argb(14 * (4 - r), 0xFFD36B)); } }
            float fl = (float) Math.cos(u * 6f); int wd = Math.max(2, Math.round(13 * Math.abs(fl)));
            int dirc = fl >= 0 ? 1 : 0;
            for (int j = 0; j < 8; j++) { int hw = Math.max(1, Math.round((j < 4 ? j + 2 : 10 - j) * wd / 6f)); box(g, cx - hw, Math.round(iy) - 4 + j, hw * 2, 1, j < 3 ? 0xFFFFD36B : j < 6 ? 0xFFFF9A3C : 0xFFC9582A); }
            box(g, cx + wd - 1, Math.round(iy) - 2, 5, 3, dirc == 1 ? 0xFFFF9A3C : 0xFFC9582A);
            if (u < 1.2f) for (int i = 0; i < 10; i++) { float a = i * 0.628f; px(g, cx + (float) Math.cos(a) * (6 + u * 24), wy + (float) Math.sin(a) * 2 - u * 6 * mh(i, 9) + u * u * 14, 2, 0xAAFFFFFF); }
            String nm = "Golden Fish  ·  Rare drop"; int tw2 = Gfx.w(font, nm) + 10, ty = sy + sh - 20;
            float la = cl((u - 0.8f) / 0.3f); if (la > 0) { box(g, cx - tw2 / 2, ty, tw2, 13, Gfx.argb(Math.round(190 * la), 0x0B0E2A)); Gfx.text(g, font, nm, cx - tw2 / 2 + 5, ty + 3, Gfx.argb(Math.round(255 * la), 0xFFC857)); }
            if (u > 3.0f) { caught[1]++; go(1, 0); }
        }
    }
}
