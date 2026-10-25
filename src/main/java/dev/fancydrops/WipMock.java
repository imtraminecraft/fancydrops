package dev.fancydrops;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The "coming soon" card for the Mining and Fishing drop scenes: a looping concept scene, how far each part is, and what it will include. */
final class WipMock {
    private WipMock() {}
    private static final String[][] MS = {{"Drop list", "Animation", "Sounds", "Chat message"}, {"Drop list", "Animation", "Sounds", "Chat message"}};
    private static final int[][] PCT = {{100, 45, 15, 0}, {100, 30, 10, 0}};
    private static final String[][] PLAN = {
        {"Its own animation with a live preview", "Sounds timed to every strike", "A chat message with item, price and magic find", "A page in the Drop Library"},
        {"Its own animation with a live preview", "Splash and reel sounds", "A chat message with item, price and source", "A page in the Drop Library"}};
    private static final String[] TEXT = {"Gemstones, rare ores and mining finds. A pickaxe strikes an ore block until it cracks open.", "Sea creature catches and treasure. A bobber dips, the line snaps tight and the catch leaps out."};
    private static int sx, sy, sw, sh;

    private static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int col) { g.fill(x, y, x + w, y + h, col); }
    private static void px(GuiGraphicsExtractor g, float x, float y, int w, int h, int col) { int ix = Math.round(x), iy = Math.round(y); g.fill(sx + ix, sy + iy, sx + ix + w, sy + iy + h, col); }

    static void draw(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, int c, int mx, int my, int accent) {
        box(g, x, y, w, h, 0xFF16130B);
        Gfx.text(g, font, c == 0 ? "Mining drops" : "Fishing drops", x + 10, y + 8, Ui.TEXT);
        String tag = "In development"; int tw = Gfx.w(font, tag) + 10;
        box(g, x + w - tw - 8, y + 5, tw, 13, 0xFF3A3220); Gfx.text(g, font, tag, x + w - tw - 3, y + 8, 0xFFFFC857);
        sw = Math.max(90, (int) (w * 0.4f)); sh = (int) (sw * 96f / 112f); sx = x + w - sw - 8; sy = y + 24;
        int tx = x + 10, tw2 = sx - tx - 10;
        int ty = sy;
        for (String line : wrap(font, TEXT[c], tw2)) { Gfx.text(g, font, line, tx, ty, Ui.SUB); ty += 11; }
        ty += 6;
        long now = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            Gfx.text(g, font, MS[c][i], tx, ty, Ui.TEXT);
            String st = PCT[c][i] >= 100 ? "Done" : PCT[c][i] == 0 ? "Not started" : PCT[c][i] + "%";
            Gfx.text(g, font, st, tx + tw2 - Gfx.w(font, st), ty, Ui.DIM);
            box(g, tx, ty + 10, tw2, 4, 0xFF0A0D14);
            int f = Math.round(tw2 * PCT[c][i] / 100f); if (f > 0) box(g, tx, ty + 10, f, 4, PCT[c][i] >= 100 ? 0xFF2D7D46 : accent);
            ty += 20;
        }
        int ly = Math.max(ty + 2, sy + sh + 8);
        Gfx.text(g, font, "What you will get", x + 10, ly, Ui.SUB); ly += 12;
        for (String p : PLAN[c]) { box(g, x + 10, ly + 1, 2, 8, 0xFF62E6A8); Gfx.text(g, font, Gfx.fit(font, p, w - 30), x + 17, ly, Ui.TEXT); ly += 11; }
        g.enableScissor(sx, sy, sx + sw, sy + sh);
        box(g, sx, sy, sw, sh, 0xFF0A0C14);
        float ph = (now % 3000L) / 3000f, k = sw / 112f;
        if (c == 0) mining(g, ph, k, now); else fishing(g, ph, k);
        g.disableScissor();
        g.outline(sx - 1, sy - 1, sw + 2, sh + 2, 0xFF3A3F52);
        Gfx.textCentered(g, font, "Concept only", sx + sw / 2, sy + sh + 3, Ui.DIM);
    }

    private static java.util.List<String> wrap(Font font, String text, int w) {
        java.util.List<String> out = new java.util.ArrayList<>(); StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String t = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() > 0 && Gfx.w(font, t) > w) { out.add(cur.toString()); cur = new StringBuilder(word); } else { cur = new StringBuilder(t); }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    static void reset() { }
    static boolean click(int mx, int my) { return false; }

    private static void mining(GuiGraphicsExtractor g, float ph, float k, long now) {
        box(g, sx, sy + Math.round(70 * k), sw, sh - Math.round(70 * k), 0xFF2A2F3C);
        int hit = (int) (ph * 3); float u = ph * 3 % 1f, sh2 = u < .15f ? 2 * k : 0;
        px(g, 40 * k + sh2, 38 * k, Math.round(32 * k), Math.round(32 * k), 0xFF4B5063);
        px(g, 40 * k + sh2, 38 * k, Math.round(32 * k), Math.max(1, Math.round(3 * k)), 0xFF6B7390);
        for (int i = 0; i < 9; i++) px(g, (44 + (i * 7) % 24) * k + sh2, (44 + (i * 11) % 22) * k, Math.max(1, Math.round(4 * k)), Math.max(1, Math.round(3 * k)), i % 2 == 0 ? 0xFF8FE9FF : 0xFF56D8FF);
        for (int i = 0; i < hit; i++) px(g, (48 + i * 9) * k, 40 * k, Math.max(1, Math.round(k)), Math.round(26 * k), 0xFF0A0C14);
        float a = u < .5f ? -0.9f + u * 2 * 1.5f : 0.6f - (u - .5f) * 2 * 1.5f;                // the pickaxe swings down and back
        double cs = Math.cos(a), sn = Math.sin(a);
        for (int i = 0; i < 18; i++) { float r = i * 2f * k; px(g, 88 * k + (float) (-sn * r), 26 * k + (float) (cs * r), Math.max(1, Math.round(2 * k)), Math.max(1, Math.round(2 * k)), 0xFF8A5A2C); }
        for (int i = -6; i <= 6; i++) { float r = i * 2f * k; px(g, 88 * k + (float) (cs * r), 26 * k + (float) (sn * r), Math.max(1, Math.round(2 * k)), Math.max(1, Math.round(2 * k)), 0xFFC9D1EE); }
        if (ph > .85f) for (int i = 0; i < 14; i++) { double an = i * .45, d = (ph - .85f) / .15f * 26 * k; px(g, 56 * k + (float) (Math.cos(an) * d), 52 * k + (float) (Math.sin(an) * d), 2, 2, i % 2 == 0 ? 0xFF56D8FF : 0xFFFFC857); }
    }

    private static void fishing(GuiGraphicsExtractor g, float ph, float k) {
        int wy = Math.round(48 * k);
        box(g, sx, sy + wy, sw, sh - wy, 0xFF12304F); box(g, sx, sy + wy - 1, sw, 2, 0xFF4F9FD6);
        px(g, 8 * k, 44 * k, Math.round(34 * k), Math.max(1, Math.round(3 * k)), 0xFF6B4A2A);
        px(g, 14 * k, 30 * k, Math.max(1, Math.round(2 * k)), Math.round(16 * k), 0xFF8A5A2C);
        float bx = 76 * k, dip = ph > .5f && ph < .7f ? 5 * k : 0, by = 50 * k + dip;
        for (int i = 0; i <= 16; i++) { float t = i / 16f; px(g, 15 * k + (bx - 15 * k) * t, 30 * k + (by - 4 * k - 30 * k) * t - (float) Math.sin(t * Math.PI) * 6 * k, 1, 1, 0xFFCFD6E6); }
        px(g, bx - 2 * k, by - 4 * k, Math.round(5 * k), Math.round(4 * k), 0xFFFF5C7A); px(g, bx - 2 * k, by, Math.round(5 * k), Math.round(3 * k), 0xFFF2F5FB);
        if (ph > .72f) { float u = (ph - .72f) / .28f; float fx = bx - 30 * k * u, fy = 50 * k - (float) Math.sin(u * Math.PI) * 34 * k; px(g, fx, fy, Math.round(10 * k), Math.round(5 * k), 0xFFFFC857); px(g, fx + 8 * k, fy + k, Math.round(4 * k), Math.round(3 * k), 0xFFE8A030); }
        if (ph > .5f && ph < .72f) for (int i = 0; i < 3; i++) px(g, bx - 8 * k + i * 8 * k, 52 * k + (ph * 40) % 4, 1, 2, 0xFFBFE6FF);
    }
}
