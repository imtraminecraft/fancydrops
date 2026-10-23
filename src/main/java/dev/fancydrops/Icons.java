package dev.fancydrops;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Hand-drawn, always-moving 16x16 icons for the menu (a row's icon "fd:name" draws one of these instead of an item). */
final class Icons {
    static boolean has(String k) { return k != null && k.startsWith("fd:"); }

    private static void r(GuiGraphicsExtractor g, int x, int y, int cx, int cy, int w, int h, int c) { g.fill(x + cx, y + cy, x + cx + w, y + cy + h, c); }
    private static void disc(GuiGraphicsExtractor g, int x, int y, float cx, float cy, float rad, int c) {
        for (int dy = (int) -Math.ceil(rad); dy <= Math.ceil(rad); dy++) {
            float w2 = rad * rad - dy * dy; if (w2 < 0) continue;
            int dx = Math.round((float) Math.sqrt(w2));
            g.fill(x + Math.round(cx) - dx, y + Math.round(cy) + dy, x + Math.round(cx) + dx + 1, y + Math.round(cy) + dy + 1, c);
        }
    }
    /** A turning gear: toothed rim, a dark hub with a hole, and a bright glint travelling round the rim. */
    private static void gear(GuiGraphicsExtractor g, int x, int y, float cx, float cy, float rad, int teeth, float rot, int base, int dark, float glint) {
        for (int j = 0; j < 16; j++) for (int i = 0; i < 16; i++) {
            float dx = i + 0.5f - cx, dy = j + 0.5f - cy, d = (float) Math.sqrt(dx * dx + dy * dy), a = (float) Math.atan2(dy, dx);
            boolean tooth = Math.cos(teeth * (a - rot)) > -0.35;                             // half the way round is tooth, half is gap
            float body = rad - 0.6f, limit = tooth ? rad + 1.3f : body, hole = rad * 0.38f;
            if (d > limit || d < hole) continue;
            int c = base;
            if (d < hole + 1.3f) c = dark;                                                  // dark ring round the hole
            else if (d > body - 0.9f && d <= body) c = Canvas.shade(base, 0.78f);             // the rim of the wheel
            else if (d > body) c = Canvas.shade(base, tooth ? 1.2f : 1f);                     // the tooth faces
            else if (dx + dy < -rad * 0.5f) c = Canvas.shade(base, 1.18f);                    // light from the top left
            if (d > body - 2f && Math.cos(a - glint) > 0.93f) c = Canvas.shade(base, 1.55f); // a glint running round the edge
            g.fill(x + i, y + j, x + i + 1, y + j + 1, c);
        }
    }
    /** A filled triangle (barycentric test over the 16x16 cell). */
    private static void tri(GuiGraphicsExtractor g, int x, int y, float x0, float y0, float x1, float y1, float x2, float y2, int c) {
        float d = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2); if (Math.abs(d) < 1e-4f) return;
        for (int j = 0; j < 16; j++) for (int i = 0; i < 16; i++) {
            float px = i + 0.5f, py = j + 0.5f;
            float a = ((y1 - y2) * (px - x2) + (x2 - x1) * (py - y2)) / d, b = ((y2 - y0) * (px - x2) + (x0 - x2) * (py - y2)) / d, e = 1 - a - b;
            if (a >= -0.02f && b >= -0.02f && e >= -0.02f) g.fill(x + i, y + j, x + i + 1, y + j + 1, c);
        }
    }
    private static int al(int a, int rgb) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }

    private static void ln(GuiGraphicsExtractor g, int x, int y, float x0, float y0, float x1, float y1, int c) {
        int n = Math.max(1, Math.round(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
        for (int i = 0; i <= n; i++) { int px = Math.round(x0 + (x1 - x0) * i / n), py = Math.round(y0 + (y1 - y0) * i / n); g.fill(x + px, y + py, x + px + 1, y + py + 1, c); }
    }
    private static void spark(GuiGraphicsExtractor g, int x, int y, int cx, int cy, int len, int c) {
        r(g, x, y, cx - len, cy, len * 2 + 1, 1, c); r(g, x, y, cx, cy - len, 1, len * 2 + 1, c);
        if (len > 1) { r(g, x, y, cx - 1, cy - 1, 3, 3, al(150, c)); }
    }
    private static final String[] A = {".XXX.", "X...X", "XXXXX", "X...X", "X...X"};
    private static final int[][] PIPS = {{4}, {0, 8}, {0, 4, 8}, {0, 2, 6, 8}, {0, 2, 4, 6, 8}, {0, 2, 3, 5, 6, 8}};

    static boolean animated = true;

    /** Draws an icon. Every icon keeps its place and its size; it only glows, blinks and glints. "fd:name#RRGGBB" tints icons that take a colour. */
    static void draw(GuiGraphicsExtractor g, String key, int x, int y, long now) {
        if (!animated) now = 2400L;
        body(g, key, x, y, now);
    }

    private static void body(GuiGraphicsExtractor g, String key, int x, int y, long now) {
        float s = (now % 3600000L) / 1000f;
        String name = key.startsWith("fd:") ? key.substring(3) : key; int tint = 0xFF888888;
        int hash = name.indexOf('#');
        if (hash >= 0) { try { tint = 0xFF000000 | Integer.parseInt(name.substring(hash + 1), 16); } catch (Exception ignored) {} name = name.substring(0, hash); }
        if (name.startsWith("cx_")) { Cosmetics.drawCosIcon(g, name.substring(3), x, y, now); return; }
        if (PixArt.draw(g, name, x, y, s, tint == 0xFF888888 ? 0 : tint & 0xFFFFFF)) return;                                                          // the hand-pixeled pictures
        if (name.startsWith("suf_")) { Cosmetics.drawIcon(g, name.substring(4), x, y, now); return; }
        switch (name) {
            case "home" -> {                                                                            // a house, its window glowing, smoke fading in and out
                r(g, x, y, 11, 2, 2, 4, 0xFF8A4B2E);
                for (int i = 0; i < 3; i++) { float u = (s * 0.8f + i / 3f) % 1f; disc(g, x, y, 12.5f, 0.5f + i * 1.4f, 0.8f + i * 0.35f, al((int) (200 * Math.sin(u * Math.PI)), 0xDDE3EE)); }
                for (int k = 0; k < 6; k++) r(g, x, y, 6 - k, 3 + k, 4 + 2 * k, 1, k % 2 == 0 ? 0xFFE0643C : 0xFFC9502C);
                r(g, x, y, 3, 9, 10, 6, 0xFFE4D3AE); r(g, x, y, 7, 11, 3, 4, 0xFF7A4A28);
                r(g, x, y, 4, 10, 3, 3, al(150 + (int) (105 * Math.sin(s * 5)), 0xFFE066));
            }
            case "animations" -> {                                                                      // a film frame, its holes chasing, a star pulsing
                r(g, x, y, 0, 2, 16, 12, 0xFF2A2F3F);
                int hot = (int) (s * 6f) % 4;
                for (int i = 0; i < 4; i++) { int c = i == hot ? 0xFFFFFFFF : 0xFF8E98AC; r(g, x, y, 1 + i * 4, 3, 2, 2, c); r(g, x, y, 1 + i * 4, 11, 2, 2, c); }
                r(g, x, y, 2, 6, 12, 4, 0xFF10141D);
                float pu = 0.5f + 0.5f * (float) Math.sin(s * 5f);
                disc(g, x, y, 8f, 8f, 1.2f + pu * 1.4f, al(255, (int) (s * 3) % 2 == 0 ? 0xFFC857 : 0xFF72C6));
            }
            case "sounds" -> {                                                                          // a speaker with waves rolling out
                r(g, x, y, 1, 6, 3, 5, 0xFFC4CBD9);
                for (int k = 0; k < 4; k++) r(g, x, y, 4 + k, 6 - k, 1, 5 + 2 * k, 0xFFA9B2C4);
                for (int i = 0; i < 3; i++) {
                    float ph = (s * 1.5f - i * 0.28f) % 1f, a = ph < 0 ? 0 : (float) Math.sin(ph * Math.PI);
                    int h = 2 + i * 2;
                    r(g, x, y, 9 + i * 2, 8 - h / 2 - 1, 1, h + 2, al((int) (255 * a), i == 1 ? 0xFFB300 : 0x56D8FF));
                }
            }
            case "chat" -> {                                                                            // a speech bubble, dots lighting in turn
                r(g, x, y, 1, 2, 14, 9, 0xFFE8ECF4); r(g, x, y, 2, 1, 12, 11, 0xFFE8ECF4);
                r(g, x, y, 3, 11, 3, 3, 0xFFE8ECF4); r(g, x, y, 3, 14, 1, 1, 0xFFE8ECF4);
                int hot = (int) (s * 4f) % 3;
                for (int i = 0; i < 3; i++) r(g, x, y, 4 + i * 3, 5, 2, 3, i == hot ? 0xFF8FA8FF : 0xFFB8BECC);
            }
            case "settings" -> {                                                                        // a gear that turns, a glint running round its rim
                gear(g, x, y, 8f, 8f, 5.4f, 6, s * 0.7f, 0xFFB4BED2, 0xFF5C677E, s * 2.4f);
            }
            case "stats" -> {                                                                           // bars that stay put, a glint running along their tops
                r(g, x, y, 1, 14, 14, 1, 0xFF6C7080);
                int[] cols = {0xFF56D8FF, 0xFFB56BFF, 0xFFFFB300, 0xFFFF72C6}, hs = {5, 9, 7, 11};
                int hot = (int) (s * 4f) % 4;
                for (int i = 0; i < 4; i++) { r(g, x, y, 2 + i * 3, 14 - hs[i], 2, hs[i], cols[i]); r(g, x, y, 2 + i * 3, 14 - hs[i], 2, 1, i == hot ? 0xFFFFFFFF : Canvas.shade(cols[i], 1.3f)); }
            }
            case "library" -> {                                                                         // a chest, lid ajar, a gem glinting inside
                float u = (s % 1.8f) / 1.8f;
                r(g, x, y, 1, 8, 14, 7, 0xFF8A5A2C); r(g, x, y, 1, 11, 14, 1, 0xFF5E3C1B); r(g, x, y, 7, 10, 2, 3, 0xFFFFC857);
                r(g, x, y, 1, 3, 14, 3, 0xFFA8733A); r(g, x, y, 1, 3, 14, 1, 0xFFC98C4A);
                r(g, x, y, 6, 5, 4, 4, 0xFF56D8FF); r(g, x, y, 7, 4, 2, 1, 0xFFB8F0FF); r(g, x, y, 7, 9, 2, 1, 0xFF2AA6D0);
                int a = (int) (255 * Math.max(0, Math.sin(u * Math.PI * 2)));
                spark(g, x, y, 11, 3, 2, al(a, 0xFFFFFF)); spark(g, x, y, 4, 2, 1, al(255 - a, 0xFFFFFF));
            }
            case "profit" -> {                                                                          // a coin over bars, glinting
                for (int i = 0; i < 3; i++) r(g, x, y, 1 + i * 4, 15 - (3 + i * 3), 3, 3 + i * 3, 0xFF3FB27F);
                r(g, x, y, 8, 2, 6, 6, 0xFFFFB300); r(g, x, y, 8, 2, 6, 1, 0xFFFFE08A); spark(g, x, y, 11, 5, 1 + (int) (s * 3f) % 2, 0xFFFFFFFF);
            }
            case "custom" -> {                                                                          // three sliders whose knobs light in turn
                int[] cols = {0xFF56D8FF, 0xFF8FA8FF, 0xFFFF72C6};
                int hot = (int) (s * 3f) % 3;
                for (int i = 0; i < 3; i++) {
                    int ty = 3 + i * 5; float kx = 4 + (float) (4 + 4 * Math.sin(s * 1.5f + i * 2.1f));
                    r(g, x, y, 1, ty, 14, 1, 0xFF3A4254); r(g, x, y, 1, ty, Math.round(kx), 1, cols[i]);
                    disc(g, x, y, kx, ty + 0.5f, 2.4f, i == hot ? 0xFFFFFFFF : 0xFFC4CBD9); disc(g, x, y, kx, ty + 0.5f, 1.2f, cols[i]);
                }
            }
            case "wip" -> {                                                                             // two gears meshing, turning against each other
                float r1 = 3.7f, r2 = 3.2f, a1 = s * 1.3f;
                gear(g, x, y, 4.8f, 4.8f, r1, 6, a1, 0xFFFFB300, 0xFF9A6A00, s * 2.4f);
                gear(g, x, y, 11.2f, 11.2f, r2, 5, -a1 * r1 / r2 + 0.35f, 0xFFE0A21B, 0xFF8A5C0A, s * 2.4f + 2f);
            }
            case "discord" -> {                                                                         // blurple face, blinking
                int dy = 0;
                r(g, x, y, 1, 3 + dy, 14, 9, 0xFF5865F2); r(g, x, y, 2, 2 + dy, 12, 11, 0xFF5865F2);
                r(g, x, y, 3, 1 + dy, 3, 2, 0xFF5865F2); r(g, x, y, 10, 1 + dy, 3, 2, 0xFF5865F2);
                boolean blink = s % 3f < 0.14f; int eh = blink ? 1 : 4;
                r(g, x, y, 4, 6 + dy + (blink ? 2 : 0), 3, eh, 0xFFFFFFFF); r(g, x, y, 9, 6 + dy + (blink ? 2 : 0), 3, eh, 0xFFFFFFFF);
                r(g, x, y, 6, 11 + dy, 4, 1, al(200, 0xFFFFFF));
            }
            case "patreon" -> {                                                                         // the coral circle pulses beside its bar
                r(g, x, y, 2, 1, 3, 14, 0xFFF2F4F8);
                disc(g, x, y, 10, 6.5f, 4.6f + 0.8f * (float) Math.sin(s * 3.2f), 0xFFF96854);
                r(g, x, y, 9, 5, 2, 1, al(120, 0xFFFFFF));
            }
            case "github" -> {                                                                          // the cat head with blinking eyes and a waving tail
                disc(g, x, y, 7.5f, 7.5f, 7.4f, 0xFFF0F6FC);
                r(g, x, y, 4, 5, 8, 6, 0xFF161B22); r(g, x, y, 4, 3, 2, 3, 0xFF161B22); r(g, x, y, 10, 3, 2, 3, 0xFF161B22);
                boolean blink = s % 2.6f < 0.12f;
                r(g, x, y, 5, blink ? 8 : 6, 2, blink ? 1 : 3, 0xFFF0F6FC); r(g, x, y, 9, blink ? 8 : 6, 2, blink ? 1 : 3, 0xFFF0F6FC);
                r(g, x, y, 7, 11, 2, 3, 0xFF161B22);
            }
            case "nothing" -> {                                                                         // a "no" sign that glows
                int a = 170 + (int) (85 * Math.sin(s * 4f));
                disc(g, x, y, 7.5f, 7.5f, 6f, al(a, 0xE5484D)); disc(g, x, y, 7.5f, 7.5f, 4f, 0xFF1A2030);
                for (int k = -4; k <= 4; k++) r(g, x, y, 8 + k, 8 - k - 1, 2, 2, al(a, 0xE5484D));
            }
            case "chop" -> {                                                                            // a sword with a gleam running up the blade
                for (int k = 0; k < 10; k++) { r(g, x, y, 5 + k, 9 - k, 2, 1, 0xFFE9F0FA); r(g, x, y, 5 + k, 10 - k, 2, 1, 0xFF9FB0C8); r(g, x, y, 4 + k, 10 - k, 1, 1, 0xFF6F7F99); }
                int hot = Math.round(((s * 1.4f) % 1.3f) * 9f); for (int k = Math.max(0, hot - 2); k <= Math.min(9, hot); k++) r(g, x, y, 5 + k, 9 - k, 2, 1, 0xFFFFFFFF);
                ln(g, x, y, 3, 8, 8, 13, 0xFFF2C14E); ln(g, x, y, 3, 9, 7, 13, 0xFFB98A24);
                ln(g, x, y, 4, 12, 1, 15, 0xFF8A5A2C); ln(g, x, y, 5, 12, 2, 15, 0xFF6A4220); r(g, x, y, 0, 14, 2, 2, 0xFFF2C14E);
                spark(g, x, y, 14, 1, 1, al(120 + (int) (130 * Math.sin(s * 5f)), 0xFFFFFF));
            }
            case "rare" -> {                                                                            // an amethyst shard that glints
                for (int k = 0; k < 5; k++) { r(g, x, y, 7 - k, 2 + k, 2 + 2 * k, 1, k < 2 ? 0xFFD9A8FF : 0xFFB56BFF); r(g, x, y, 7 - k, 12 - k, 2 + 2 * k, 1, 0xFF8E48D8); }
                float u = (s % 1.8f) / 1.8f; if (u < 0.4f) { int a = (int) (255 * Math.sin(u / 0.4f * Math.PI)); r(g, x, y, 10, 3, 1, 5, al(a, 0xFFFFFF)); r(g, x, y, 8, 5, 5, 1, al(a, 0xFFFFFF)); }
            }
            case "rng" -> {                                                                             // a star whose rays twinkle in turn
                int hot = (int) (s * 8f) % 8;
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4; float len = i % 2 == 0 ? 7f : 4.5f;
                    for (float d = 2.5f; d < len; d += 1f) r(g, x, y, Math.round(7.5f + (float) Math.cos(a) * d), Math.round(7.5f + (float) Math.sin(a) * d), 1, 1, al(i == hot ? 255 : 150, i % 2 == 0 ? 0xFFC857 : 0xFFFFFF));
                }
                disc(g, x, y, 7.5f, 7.5f, 2.4f, 0xFFFFFFFF);
            }
            case "dye" -> {                                                                             // a paint palette, its dabs glowing in turn
                disc(g, x, y, 8f, 8.5f, 7.2f, 0xFF8A5A2C); disc(g, x, y, 8f, 8f, 6.8f, 0xFFD9A868); disc(g, x, y, 11.6f, 11.4f, 1.7f, 0xFF7A4E26);
                int[] cols = {0xFFE5484D, 0xFFFFC857, 0xFF56D8FF, 0xFFB56BFF};
                float[][] at = {{4.2f, 6.6f}, {7.6f, 3.6f}, {11.4f, 5.2f}, {4.6f, 11f}};
                for (int k = 0; k < 4; k++) {
                    float pu = (float) (Math.sin(s * 3f - k * 1.1f) * 0.5f + 0.5f);
                    disc(g, x, y, at[k][0], at[k][1], 2.1f, al(70 + (int) (90 * pu), cols[k] & 0xFFFFFF));
                    disc(g, x, y, at[k][0], at[k][1], 1.5f, cols[k]);
                    r(g, x, y, Math.round(at[k][0]) - 1, Math.round(at[k][1]) - 1, 1, 1, al(120 + (int) (135 * pu), 0xFFFFFF));
                }
            }
            case "slayers" -> {                                                                         // a sword in a red aura: gleam up the blade, a slash arc, twinkling sparks
                final String[] sw = {"............oooo", "...........oolbo", "..........oolbdo", ".........oolbdoo", "........oolbdoo.", ".......oolbdoo..", "...ooooolbdoo...", "...oggolbdoo....", "...ooggGdoo.....", "....oogGoo......", ".oooohoGGo......", ".oPPhoooGo......", ".oPPGo.ooo......", ".oPGGo..........", ".ooooo..........", "................"};
                float pu = 0.5f + 0.5f * (float) Math.sin(s * 2.4f); int oy = Math.round((float) Math.sin(s * 2.2f) * 0.7f);
                disc(g, x, y, 8.5f, 7.5f, 7.5f, al(8 + Math.round(12 * pu), 0xFF5A78)); disc(g, x, y, 8.5f, 7.5f, 5f, al(10 + Math.round(14 * pu), 0xFFA0B4));
                for (int yy = 0; yy < 16; yy++) for (int xx = 0; xx < 16; xx++) {
                    int c = switch (sw[yy].charAt(xx)) { case 'o' -> 0xFF22263A; case 'l' -> 0xFFE2EAF8; case 'b' -> 0xFFAABAD4; case 'd' -> 0xFF687696; case 'g' -> 0xFFFFD05A; case 'G' -> 0xFFB07A1C; case 'h' -> 0xFF8C5C34; case 'P' -> 0xFFF0BE46; default -> 0; };
                    if (c != 0) r(g, x, y + oy, xx, yy, 1, 1, c);
                }
                r(g, x, y + oy, 3, 12, 1, 1, al(180 + Math.round(75 * pu), 0xFF4A55));       // the pommel ruby pulses
                float u = (s * 0.9f) % 1f; int k = Math.round(u * 6f);
                r(g, x, y + oy, 8 + k, 7 - k, 1, 1, al(Math.round(255 * (1f - u * 0.4f)), 0xFFFFFF)); r(g, x, y + oy, 9 + k, 7 - k, 1, 1, al(Math.round(120 * (1f - u)), 0xFFFFFF));
                float v = (s * 0.55f) % 1f;                                                    // a slash arc sweeps across the blade
                if (v > 0.62f) {
                    float pg = (v - 0.62f) / 0.38f;
                    for (int i = 0; i <= 14; i++) {
                        float q = i / 14f; if (q > pg * 1.25f) break;
                        int px = Math.round(1 + 13f * q), py = Math.round(1 + 9f * q + 4f * (float) Math.sin(Math.PI * q) - 2f);
                        float fade = Math.max(0f, 1f - Math.abs(q - pg) * 2.6f) * (1f - pg * 0.5f);
                        if (fade > 0.05f) { r(g, x, y, px, py, 1, 1, al(Math.round(240 * fade), 0xFFFFFF)); r(g, x, y, px, py + 1, 1, 1, al(Math.round(110 * fade), 0xFF8AA0)); }
                    }
                }
                for (int i = 0; i < 4; i++) {                                                   // sparks twinkling round the sword
                    float ph = (s * 0.8f + i * 0.27f) % 1f, tw = (float) Math.sin(ph * Math.PI);
                    int px = new int[]{13, 2, 14, 5}[i], py = new int[]{2, 5, 9, 1}[i];
                    if (tw > 0.55f) spark(g, x, y, px, py, tw > 0.9f ? 1 : 0, al(Math.round(255 * tw), i % 2 == 0 ? 0xFFFFFF : 0xFFC0D0));
                }
            }
            case "res_heart" -> {                                                                       // a heart that beats, for the credits page
                float bt = 1f;
                final String[] hs = {"................", "..oo..oo........", ".orro.orro......", "orrwrorrrro.....", "orrrrrrrrro.....", "orrrrrrrrro.....", ".orrrrrrro......", "..orrrrro.......", "...orrro........", "....oro.........", ".....o..........", "................", "................", "................", "................", "................"};
                for (int yy = 0; yy < 11; yy++) for (int xx = 0; xx < 12; xx++) {
                    char ch = hs[yy].length() > xx ? hs[yy].charAt(xx) : '.';
                    int c = ch == 'o' ? 0xFF5A1230 : ch == 'r' ? Canvas.mix(0xFFE5484D, 0xFFFF7A9A, (float) (0.5 + 0.5 * Math.sin(s * 2 + xx * 0.4))) : ch == 'w' ? 0xFFFFFFFF : 0;
                    if (c != 0) r(g, x, y, 2 + Math.round(xx * bt - (bt - 1f) * 6f), 3 + Math.round(yy * bt - (bt - 1f) * 5f), 1, 1, c);
                }
                if (((int) (s * 2f)) % 2 == 0) spark(g, x, y, 13, 3, 1, 0xFFFFD0E0);
            }
            case "res_wiki" -> {                                                                        // an open book with a globe pin
                r(g, x, y, 1, 3, 14, 11, 0xFF1F4E96); r(g, x, y, 2, 4, 6, 9, 0xFFF3EBD0); r(g, x, y, 8, 4, 6, 9, 0xFFE6DCBA); r(g, x, y, 7, 3, 2, 11, 0xFF143468);
                for (int i = 0; i < 4; i++) { r(g, x, y, 3, 6 + i * 2, 4, 1, 0xFF8A8266); r(g, x, y, 9, 6 + i * 2, 4, 1, 0xFFA09A7E); }
                float pu = 0.5f + 0.5f * (float) Math.sin(s * 3f); disc(g, x, y, 11.5f, 4f, 2.2f, al(255, 0xFF5A9CE8)); r(g, x, y, 11, 3, 1, 1, al(160 + Math.round(90 * pu), 0xFFFFFF));
            }
            case "res_neu" -> {                                                                         // NEU: an orange tile with a white N
                r(g, x, y, 1, 1, 14, 14, 0xFF3A2208); r(g, x, y, 2, 2, 12, 12, 0xFFE08A2A); r(g, x, y, 2, 2, 12, 1, 0xFFF8B85A); r(g, x, y, 2, 13, 12, 1, 0xFFB86418);
                r(g, x, y, 4, 4, 2, 8, 0xFFFFFFFF); r(g, x, y, 10, 4, 2, 8, 0xFFFFFFFF); r(g, x, y, 6, 5, 1, 2, 0xFFFFFFFF); r(g, x, y, 7, 7, 2, 2, 0xFFFFFFFF); r(g, x, y, 9, 9, 1, 2, 0xFFFFFFFF);
                int gl = (int) ((s * 6f) % 16f); if (gl < 14) r(g, x, y, 2 + gl, 2, 1, 1, 0xFFFFFFFF);
            }
            case "slot_chop" -> {                                                                       // a blade that slashes, leaving a cyan arc
                float ph = (s * 1.8f) % 1f;
                for (int i = 0; i <= 14; i++) { float u = i / 14f; if (u > ph) break;
                    float ang = (float) (Math.PI * (0.95 + 0.9 * u)); int px = Math.round(8 + 6.5f * (float) Math.cos(ang)), py = Math.round(11 + 7f * (float) Math.sin(ang));
                    float tail = 1f - (ph - u) * 1.6f; r(g, x, y, px, py, 2, 1, al(Math.round(255 * Math.max(0.15f, tail)), i % 3 == 0 ? 0xE8FBFF : 0x56D8FF)); }
                float bx = 3 + 9 * ph, by = 12 - 9 * ph;
                ln(g, x, y, bx - 2, by + 2, bx + 2, by - 2, 0xFFDDE6F0); ln(g, x, y, bx - 1, by + 2, bx + 3, by - 2, 0xFF8FA0B8);
                r(g, x, y, Math.round(bx - 3), Math.round(by + 2), 2, 2, 0xFF7A4B22);
                if (ph > 0.7f) spark(g, x, y, 13, 3, 1, al(Math.round(255 * (ph - 0.7f) / 0.3f), 0xFFFFFF));
            }
            case "slot_split" -> {                                                                      // a cube that cracks open and comes apart
                float gp = 0.5f + 0.5f * (float) Math.sin(s * 2.6f); int gap = Math.round(gp * 3);
                for (int j = 0; j < 9; j++) for (int i = 0; i < 4; i++) {
                    int shade = j < 2 ? 0xFFFF9AD2 : j > 6 ? 0xFFB83C86 : 0xFFFF72C6;
                    r(g, x, y, 3 - gap + i, 4 + j, 1, 1, i == 0 ? 0xFF7A2358 : shade);
                    r(g, x, y, 9 + gap + i, 4 + j, 1, 1, i == 3 ? 0xFF7A2358 : shade);
                }
                for (int j = 0; j < 9; j += 2) r(g, x, y, 7 + (j % 4 == 0 ? 0 : 1), 4 + j, 1, 2, al(Math.round(120 + 135 * gp), 0xFFFFFF));
                if (gp > 0.5f) { spark(g, x, y, 8, 3, 1, al(Math.round(255 * gp), 0xFFFFFF)); spark(g, x, y, 4, 1, 0, al(200, 0xFFB0E0)); spark(g, x, y, 12, 14, 0, al(200, 0xFFB0E0)); }
            }
            case "slot_reveal" -> {                                                                     // a gem that lifts out of a ray of light
                float bob = (float) Math.sin(s * 2.4f) * 1.2f; int cy0 = Math.round(8 - bob);
                for (int i = 0; i < 8; i++) { double an = s * 0.9 + i * Math.PI / 4; ln(g, x, y, 8, cy0, 8 + (float) Math.cos(an) * 7.5f, cy0 + (float) Math.sin(an) * 7.5f, al(i % 2 == 0 ? 90 : 45, 0xFFC857)); }
                for (int j = -3; j <= 3; j++) { int w = 3 - Math.abs(j); r(g, x, y, 8 - w, cy0 + j, w * 2 + 1, 1, j < 0 ? 0xFFFFE08A : j == 0 ? 0xFFFFC857 : 0xFFE0951F); }
                r(g, x, y, 6, cy0 - 2, 2, 1, 0xFFFFFFFF);
                if (((int) (s * 3)) % 2 == 0) spark(g, x, y, 13, 3, 1, 0xFFFFFFFF); else spark(g, x, y, 2, 12, 1, 0xFFFFE08A);
            }
            case "slot_linger" -> {                                                                     // soft rings spreading out and fading, like a glow that stays
                for (int k = 0; k < 3; k++) {
                    float u = (s * 0.7f + k / 3f) % 1f, rad = 1.5f + u * 6.5f; int a = Math.round((1f - u) * 230);
                    for (int j = 0; j < 16; j++) for (int i = 0; i < 16; i++) {
                        float dx = i + 0.5f - 8, dy = j + 0.5f - 8, d = (float) Math.sqrt(dx * dx + dy * dy);
                        if (Math.abs(d - rad) < 0.75f) g.fill(x + i, y + j, x + i + 1, y + j + 1, al(a, 0x7CE06A));
                    }
                }
                disc(g, x, y, 8, 8, 1.6f, al(255, 0xD8FFC8)); r(g, x, y, 7, 7, 1, 1, 0xFFFFFFFF);
            }
            case "slot_end" -> {                                                                        // a wave of bars that dies away to nothing
                for (int i = 0; i < 7; i++) {
                    float decay = (float) Math.exp(-i * 0.38), pulse = 0.65f + 0.35f * (float) Math.sin(s * 5f - i * 0.9f);
                    int h = Math.max(1, Math.round(11 * decay * pulse)); int bx = 1 + i * 2;
                    r(g, x, y, bx, 8 - h / 2, 1, h, al(Math.round(255 * (0.35f + 0.65f * decay)), i < 2 ? 0xD6B0FF : 0xB56BFF));
                }
                r(g, x, y, 15, 7, 1, 2, al(120 + Math.round(100 * (0.5f + 0.5f * (float) Math.sin(s * 3f))), 0x8A5CCF));
            }
            case "slot_chop_dye" -> {                                                                   // a paint drop falls and splats
                float ph = (s * 1.5f) % 1f; int[] cols = {0xFF56D8FF, 0xFFFF72C6, 0xFFFFC857};
                if (ph < 0.45f) { float fy = 1 + ph / 0.45f * 8; int c0 = cols[(int) (s * 1.5f) % 3];
                    r(g, x, y, 7, Math.round(fy), 2, 3, c0); r(g, x, y, 8, Math.round(fy) - 1, 1, 1, c0); r(g, x, y, 7, Math.round(fy) + 1, 1, 1, 0xFFFFFFFF); }
                else { float u = (ph - 0.45f) / 0.55f, rad = 1.5f + u * 4.5f; int c0 = cols[(int) (s * 1.5f) % 3];
                    for (int i = 0; i < 16; i++) for (int j = 0; j < 8; j++) { float dx = i + 0.5f - 8, dy = (j + 9.5f - 12) * 2f; if (dx * dx + dy * dy <= rad * rad) r(g, x, y, i, 9 + j - 1, 1, 1, al(Math.round(255 * (1f - u * 0.4f)), c0)); }
                    for (int i = 0; i < 5; i++) { double an = i * 1.26 + 0.4; r(g, x, y, Math.round(8 + (float) Math.cos(an) * (rad + 2)), Math.round(10 - Math.abs((float) Math.sin(an)) * (rad * 0.8f + u * 2)), 1, 1, al(Math.round(255 * (1 - u)), cols[(i + 1) % 3])); } }
            }
            case "slot_split_dye" -> {                                                                  // paint bursting open
                float u = (s * 1.4f) % 1f, rad = 1f + u * 6f; int[] cols = {0xFFFF72C6, 0xFF56D8FF, 0xFFFFC857, 0xFF7CE06A};
                disc(g, x, y, 8, 8, 2.5f * (1f - u * 0.5f), al(255, 0xFFFF72C6));
                for (int i = 0; i < 8; i++) { double an = i * Math.PI / 4 + 0.3; int px = Math.round(8 + (float) Math.cos(an) * rad), py = Math.round(8 + (float) Math.sin(an) * rad);
                    r(g, x, y, px - 1, py - 1, 2, 2, al(Math.round(255 * (1f - u * 0.7f)), cols[i % 4])); }
            }
            case "slot_chop_farm" -> {                                                                  // a scythe swinging through the crops
                float ph = 0.5f + 0.5f * (float) Math.sin(s * 3.2f); float ang = -0.9f + ph * 1.5f;
                float hx = 3, hy = 14; float tx = hx + (float) Math.cos(ang - 0.35) * 10, ty = hy + (float) Math.sin(ang - 0.35) * 11 - 1;
                ln(g, x, y, hx, hy, tx, ty, 0xFF8A5A2B);
                for (int i = 0; i <= 8; i++) { float u = i / 8f; double a2 = ang - 0.35 - 0.2 + u * 1.0; int bx = Math.round(tx + (float) Math.cos(a2) * 5 * u), by = Math.round(ty + (float) Math.sin(a2) * 5 * u + u * u * 2); r(g, x, y, bx, by, 1, 1, u > 0.6f ? 0xFFE8F0F8 : 0xFFB0BCCC); }
                for (int i = 0; i < 4; i++) { float u = (ph - i * 0.1f); if (u > 0.1f && u < 0.9f) r(g, x, y, Math.round(2 + u * 12), 12 - i, 2, 1, al(150 - i * 30, 0x7CE06A)); }
                r(g, x, y, 13, 13, 1, 3, 0xFF4FA83A); r(g, x, y, 11, 14, 1, 2, 0xFF4FA83A);
            }
            case "slot_split_farm" -> {                                                                 // the last crop opens and a golden glow bursts out
                float u = (s * 1.2f) % 1f; int open = Math.round(Math.min(1f, u * 2f) * 3);
                r(g, x, y, 7, 8, 2, 7, 0xFF4FA83A); r(g, x, y, 4, 11, 3, 2, 0xFF6BCB4A); r(g, x, y, 9, 10, 3, 2, 0xFF6BCB4A);
                r(g, x, y, 5 - open, 4, 3, 4, 0xFFE0A21B); r(g, x, y, 8 + open, 4, 3, 4, 0xFFE0A21B); r(g, x, y, 7, 3 - (open > 1 ? 1 : 0), 2, 3, 0xFFFFD36B);
                if (u > 0.4f) for (int i = 0; i < 6; i++) { double an = i * Math.PI / 3 - Math.PI / 2; float rr = (u - 0.4f) * 12; r(g, x, y, Math.round(8 + (float) Math.cos(an) * rr), Math.round(4 + (float) Math.sin(an) * rr * 0.8f), 1, 1, al(Math.round(255 * (1 - u)), 0xFFE08A)); }
            }
            case "slot_chop_myth" -> {                                                                  // a lightning bolt striking
                boolean on = ((int) (s * 4)) % 3 != 2; float fl = on ? 1f : 0.3f;
                int[][] pts = {{9, 0}, {6, 5}, {9, 6}, {5, 11}, {7, 11}, {4, 15}};
                for (int i = 0; i < pts.length - 1; i++) { ln(g, x, y, pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], al(Math.round(255 * fl), 0xFFFFE066)); ln(g, x, y, pts[i][0] + 1, pts[i][1], pts[i + 1][0] + 1, pts[i + 1][1], al(Math.round(200 * fl), 0xFFFFFFFF)); }
                if (on) { spark(g, x, y, 12, 4, 1, 0xFFB8E0FF); spark(g, x, y, 3, 8, 0, 0xFF9AB8FF); }
                for (int i = 0; i < 3; i++) r(g, x, y, 2 + i * 5, 15, 2, 1, al(120, 0xB56BFF));
            }
            case "slot_split_myth" -> {                                                                 // the rune circle erupts in a column of light
                float u = (s * 1.1f) % 1f;
                for (int i = 0; i < 16; i++) { double an = i * Math.PI / 8 + s; r(g, x, y, Math.round(8 + (float) Math.cos(an) * 6.5f), Math.round(12 + (float) Math.sin(an) * 2.2f), 1, 1, al(i % 2 == 0 ? 255 : 150, 0xFFB56BFF)); }
                int h = Math.round(Math.min(1f, u * 1.6f) * 11); r(g, x, y, 6, 12 - h, 4, h, al(Math.round(190 * (1f - u * 0.5f)), 0xFFE0C8FF)); r(g, x, y, 7, 12 - h, 2, h, al(255, 0xFFFFFFFF));
                if (u > 0.3f) for (int i = 0; i < 4; i++) r(g, x, y, 3 + i * 3, Math.round(10 - u * 9 + (i % 2) * 2), 1, 1, al(220, 0xFFFFFFFF));
            }
            case "slot_chop_exp" -> {                                                                   // a book with a page flipping over
                r(g, x, y, 1, 5, 14, 9, 0xFF3A2160); r(g, x, y, 2, 6, 6, 7, 0xFFF3EBD0); r(g, x, y, 8, 6, 6, 7, 0xFFE6DCBA); r(g, x, y, 7, 5, 2, 9, 0xFF2A1748);
                float ph = (s * 1.6f) % 1f, wdt = (float) Math.cos(ph * Math.PI); int px = Math.round(8 + wdt * 6);
                for (int j = 0; j < 7; j++) r(g, x, y, Math.min(8, px), 6 + j - Math.round((float) Math.sin(ph * Math.PI) * 2), Math.max(1, Math.abs(px - 8)), 1, wdt > 0 ? 0xFFFFFFFF : 0xFFD9CFAA);
                for (int i = 0; i < 3; i++) r(g, x, y, 3, 8 + i * 2, 3, 1, 0xFF9A9078);
                r(g, x, y, 12, 2 + Math.round(ph * 2), 1, 1, al(Math.round(255 * (1 - ph)), 0xFFD0A8FF));
            }
            case "slot_split_exp" -> {                                                                  // the book bursts into light
                float pu = 0.5f + 0.5f * (float) Math.sin(s * 3.4f);
                for (int i = 0; i < 12; i++) { double an = i * Math.PI / 6 + s * 0.6; float len = 6.5f + pu * 1.5f - (i % 2) * 2f; ln(g, x, y, 8, 8, 8 + (float) Math.cos(an) * len, 8 + (float) Math.sin(an) * len, al(i % 2 == 0 ? 230 : 120, 0xFFD0A8FF)); }
                disc(g, x, y, 8, 8, 3.2f + pu, al(255, 0xFFB56BFF)); disc(g, x, y, 8, 8, 1.8f + pu * 0.6f, al(255, 0xFFFFFFFF));
            }
            case "res_hypixel" -> {                                                                     // Hypixel: a gold H on dark
                r(g, x, y, 1, 1, 14, 14, 0xFF0C1018); r(g, x, y, 2, 2, 12, 12, 0xFF1B2330);
                r(g, x, y, 4, 4, 2, 8, 0xFFFFB400); r(g, x, y, 10, 4, 2, 8, 0xFFFFB400); r(g, x, y, 6, 7, 4, 2, 0xFFFFB400);
                r(g, x, y, 4, 4, 1, 8, 0xFFFFE08A); r(g, x, y, 10, 4, 1, 8, 0xFFFFE08A);
                int gl = (int) ((s * 7f) % 18f); if (gl < 12) r(g, x, y, 2 + gl, 3 + (gl % 3), 1, 1, 0xFFFFFFFF);
            }
            case "res_fabric" -> {                                                                      // Fabric: woven cloth with a stitch
                for (int yy = 0; yy < 14; yy++) for (int xx = 0; xx < 14; xx++) r(g, x, y, 1 + xx, 1 + yy, 1, 1, ((xx / 2 + yy / 2) % 2 == 0) ? 0xFFDCC8A0 : 0xFFBBA27A);
                r(g, x, y, 1, 1, 14, 1, 0xFF8A7650); r(g, x, y, 1, 14, 14, 1, 0xFF8A7650); r(g, x, y, 1, 1, 1, 14, 0xFF8A7650); r(g, x, y, 14, 1, 1, 14, 0xFF8A7650);
                for (int i = 0; i < 6; i++) r(g, x, y, 3 + i * 2, 3 + i * 2, 1, 1, ((int) (s * 4f) + i) % 3 == 0 ? 0xFFFFFFFF : 0xFFF6EBD0);
            }
            case "res_mc" -> {                                                                          // Minecraft: a grass block
                for (int yy = 0; yy < 14; yy++) for (int xx = 0; xx < 14; xx++) {
                    int v = (xx * 7 + yy * 13) % 5; int c = yy < 4 ? (v < 2 ? 0xFF5FA83A : v < 4 ? 0xFF4F9230 : 0xFF74C24A) : (v < 2 ? 0xFF8A5A2C : v < 4 ? 0xFF7A4C22 : 0xFF6B4423);
                    if (yy == 4 && v % 2 == 0) c = 0xFF4F9230;
                    r(g, x, y, 1 + xx, 1 + yy, 1, 1, c);
                }
                r(g, x, y, 1, 14, 14, 1, 0xFF3C2812); r(g, x, y, 14, 1, 1, 14, 0xFF3C2812);
                int gl = (int) ((s * 8f) % 20f); if (gl < 14) r(g, x, y, 1 + gl, 1, 1, 1, 0xFFE8FFD0);
            }
            case "switch" -> {                                                                          // a toggle sliding on and off
                float v = 0.5f + 0.5f * (float) Math.sin(s * 2.2f);
                int tr = Canvas.mix(0xFF4A4D5C, 0xFF3FD08F, v);
                disc(g, x, y, 4.5f, 8f, 4.2f, tr); disc(g, x, y, 11.5f, 8f, 4.2f, tr); r(g, x, y, 4, 4, 8, 8, tr);
                r(g, x, y, 4, 4, 8, 1, al(70, 0xFFFFFF));
                disc(g, x, y, 4.5f + 7f * v, 8f, 3.1f, 0xFFF4F6FA); disc(g, x, y, 4.5f + 7f * v, 7.4f, 2f, 0xFFFFFFFF);
            }
            case "farm" -> {                                                                            // a sprout growing out of soil, a leaf swaying
                r(g, x, y, 1, 12, 14, 3, 0xFF6B4423); r(g, x, y, 1, 12, 14, 1, 0xFF8A5A2C);
                float w = (float) Math.sin(s * 3f);
                r(g, x, y, 7, 6, 2, 6, 0xFF4FB848); r(g, x, y, 3 + Math.round(w), 4, 4, 3, 0xFF7BE05A); r(g, x, y, 9 - Math.round(w), 3, 4, 3, 0xFF7BE05A);
                if (s % 1.8f < 0.3f) spark(g, x, y, 12, 4, 1, 0xFFFFFFFF);
            }
            case "farm_rare_crops" -> {                                                                 // a wheat ear with a twinkle
                r(g, x, y, 7, 6, 2, 9, 0xFF8FB84A);
                for (int i = 0; i < 4; i++) { r(g, x, y, 4 + (i % 2) * 0, 2 + i * 2, 3, 2, 0xFFE6C34A); r(g, x, y, 9, 2 + i * 2, 3, 2, 0xFFE6C34A); }
                r(g, x, y, 7, 0, 2, 3, 0xFFF2D86A);
                if (s % 1.6f < 0.3f) spark(g, x, y, 13, 3, 1, 0xFFFFFFFF);
            }
            case "farm_harvest_feast" -> {                                                              // a pumpkin on a plate, steam rising
                r(g, x, y, 2, 13, 12, 2, 0xFFD9D9E0); r(g, x, y, 4, 7, 8, 6, 0xFFE8892B); r(g, x, y, 3, 8, 10, 4, 0xFFF29A3A); r(g, x, y, 7, 7, 1, 6, 0xFFB86418); r(g, x, y, 7, 4, 2, 3, 0xFF4F8A2E);
                int st = Math.round((float) Math.sin(s * 3f)); r(g, x, y, 5 + st, 1, 1, 3, al(140, 0xFFFFFF)); r(g, x, y, 11 - st, 2, 1, 3, al(110, 0xFFFFFF));
            }
            case "farm_pests" -> {                                                                      // a bug with feelers and blinking eyes
                r(g, x, y, 5, 5, 6, 7, 0xFF7A8F2E); r(g, x, y, 6, 4, 4, 2, 0xFF5E7022); r(g, x, y, 7, 7, 2, 4, 0xFF9DB34A);
                r(g, x, y, 3, 7, 2, 1, 0xFF4A4A3A); r(g, x, y, 11, 7, 2, 1, 0xFF4A4A3A); r(g, x, y, 3, 10, 2, 1, 0xFF4A4A3A); r(g, x, y, 11, 10, 2, 1, 0xFF4A4A3A);
                int fe = Math.round((float) Math.sin(s * 5f)); r(g, x, y, 5 + fe, 2, 1, 2, 0xFF4A4A3A); r(g, x, y, 10 - fe, 2, 1, 2, 0xFF4A4A3A);
                r(g, x, y, 6, 5, 1, 1, (s % 2.4f < 0.2f) ? 0xFF7A8F2E : 0xFFE5484D); r(g, x, y, 9, 5, 1, 1, (s % 2.4f < 0.2f) ? 0xFF7A8F2E : 0xFFE5484D);
            }
            case "mining" -> {                                                                          // a pickaxe slamming a diamond ore block: sparks, chips, a shaking block
                float u = (s * 1.1f) % 1f, hit = 0.62f, since = u >= hit ? (u - hit) / (1f - hit) : -1f;
                int shk = since >= 0f && since < 0.25f ? (Math.round((float) Math.sin(since * 70f)) ) : 0;
                disc(g, x, y, 11.5f, 11.5f, 7f, al(22 + Math.round(18 * (float) Math.sin(s * 3f) * (float) Math.sin(s * 3f)), 0x55E6FF));
                int bx = 7 + shk, by = 7;                                                         // the block: stone with a light top and dark right
                r(g, x, y, bx, by, 9, 9, 0xFF6F7690); r(g, x, y, bx, by, 9, 1, 0xFFA8B0C8); r(g, x, y, bx, by, 1, 9, 0xFF8A92AC);
                r(g, x, y, bx + 8, by, 1, 9, 0xFF4B5068); r(g, x, y, bx, by + 8, 9, 1, 0xFF3C4156);
                r(g, x, y, bx + 3, by + 5, 2, 1, 0xFF5B6178); r(g, x, y, bx + 6, by + 2, 1, 2, 0xFF5B6178);
                int[][] ore = {{2, 2}, {5, 3}, {3, 5}, {6, 6}};                                    // diamond ore, twinkling
                for (int i = 0; i < ore.length; i++) {
                    float tw = 0.5f + 0.5f * (float) Math.sin(s * 4f + i * 1.7f);
                    r(g, x, y, bx + ore[i][0], by + ore[i][1], 2, 2, Canvas.mix(0xFF2EC8D8, 0xFF9AF8FF, tw)); r(g, x, y, bx + ore[i][0], by + ore[i][1], 1, 1, 0xFFE8FFFF);
                }
                double ang = Math.toRadians(u < hit ? -105 + 30 * (u / hit) * (u / hit) : -75 + 33 * Math.min(1f, (u - hit) / 0.07f));
                float px = 2.5f, py = 14.5f, len = 9.5f, tx = px + (float) Math.cos(ang) * len, ty = py + (float) Math.sin(ang) * len;
                float nx = (float) -Math.sin(ang), ny = (float) Math.cos(ang);
                ln(g, x, y, px, py, tx, ty, 0xFF9A6634); ln(g, x, y, px, py + 1, tx, ty + 1, 0xFF6B4423);        // the shaded handle
                for (int i = -4; i <= 4; i++) {                                                                // the head: a curved iron crescent
                    float cu = 1f - Math.abs(i) / 4f * 0.5f; float hx = tx + nx * i - (float) Math.cos(ang) * (1 - cu) * 3f, hy = ty + ny * i - (float) Math.sin(ang) * (1 - cu) * 3f;
                    r(g, x, y, Math.round(hx), Math.round(hy), 1, 1, i < -1 ? 0xFFEAF0FA : i > 1 ? 0xFF8A94AC : 0xFFC2CCE0);
                    r(g, x, y, Math.round(hx + (float) Math.cos(ang)), Math.round(hy + (float) Math.sin(ang)), 1, 1, 0xFF5A6478);
                }
                if (since >= 0f && since < 0.6f) {                                                             // sparks and chips fly out
                    for (int i = 0; i < 7; i++) {
                        double an = -Math.PI * (0.15 + 0.7 * ((i * 37) % 10) / 10.0); float sp = 6f + (i % 3) * 3f, tt = since;
                        int sx = Math.round(bx + 1 + (float) Math.cos(an) * sp * tt * 2f), sy = Math.round(by + 2 + (float) Math.sin(an) * sp * tt * 2f + 14f * tt * tt);
                        r(g, x, y, sx, sy, 1, 1, al(Math.round(255 * (1f - since / 0.6f)), i % 2 == 0 ? 0xFFE08A : i % 3 == 0 ? 0x9AF8FF : 0xFFFFFF));
                    }
                    if (since < 0.12f) spark(g, x, y, bx + 1, by + 1, 2, 0xFFFFFFFF);
                }
            }
            case "fx0" -> {                                                                             // Slam in: a block slams down, impact lines flash
                float p = (s * 1.1f) % 1f, sc = p < 0.35f ? 1.7f - p / 0.35f * 0.7f : 1f; int w = Math.round(6 * sc);
                r(g, x, y, 8 - w / 2, 8 - w / 2, w, w, 0xFFF4C26B);
                if (p > 0.3f && p < 0.7f) { int a2 = 220; r(g, x, y, 1, 8, 3, 1, al(a2, 0xFFFFFF)); r(g, x, y, 12, 8, 3, 1, al(a2, 0xFFFFFF)); r(g, x, y, 8, 1, 1, 3, al(a2, 0xFFFFFF)); r(g, x, y, 8, 12, 1, 3, al(a2, 0xFFFFFF)); }
            }
            case "fx1" -> {                                                                             // Light sweep: a bright band running over a bar of words
                r(g, x, y, 1, 5, 14, 6, 0xFF4B5575);
                int gx = Math.round(((s * 1.2f) % 1.5f) * 12f) - 2; g.enableScissor(x + 1, y + 5, x + 15, y + 11); for (int j = 0; j < 6; j++) r(g, x, y, gx - j / 2 + 1, 5 + j, 3, 1, 0xDDFFFFFF); g.disableScissor();
            }
            case "fx2" -> { for (int i = 0; i < 5; i++) r(g, x, y, 1 + i * 3, 7 + Math.round((float) Math.sin(s * 4f + i * 0.9f) * 3.2f), 3, 3, 0xFF7BC8FF); }          // Wave
            case "fx3" -> { int[] rb = {0xFFFF4D4D, 0xFFFFA84D, 0xFFFFE44D, 0xFF62E66A, 0xFF4DB8FF, 0xFFB06BFF}; int off = (int) (s * 5f); for (int i = 0; i < 6; i++) r(g, x, y, 1 + i * 2 + 1, 3, 2, 10, rb[(i + off) % 6]); }       // Rainbow
            case "fx4" -> {                                                                             // Drop in
                float p = (s * 0.9f) % 1f; int by = Math.round(p * p * 9f); r(g, x, y, 5, 1 + by, 6, 5, 0xFFF4C26B);
                for (int k = 1; k <= 3; k++) r(g, x, y, 6 + k, Math.max(0, by - k * 2), 1, 1, al(150 - k * 40, 0xFFFFFF));
                r(g, x, y, 2, 14, 12, 1, 0xFF4B5575);
            }
            case "fx5" -> { float k = 0.5f + 0.5f * (float) Math.sin(s * 3.4f); disc(g, x, y, 7.5f, 7.5f, 3.5f + k * 3f, al(70, 0xFFC857)); disc(g, x, y, 7.5f, 7.5f, 2.6f + k * 1.4f, 0xFFFFC857); }      // Pulse
            case "fx6" -> {                                                                             // Glitch: a block with offset colour slices
                int sh = (int) (s * 7f) % 5 - 2; r(g, x, y, 3, 3, 10, 10, 0xFFE8EAF2);
                r(g, x, y, 3 + sh, 5, 10, 2, 0xAA56D8FF); r(g, x, y, 3 - sh, 9, 10, 2, 0xAAFF72C6); r(g, x, y, 5, 7, 6, 2, 0xFF2A2F40);
            }
            case "fx7" -> { float k = 0.5f + 0.5f * (float) Math.sin(s * 3f); disc(g, x, y, 7.5f, 7.5f, 7f, al(40 + Math.round(40 * k), 0x56D8FF)); disc(g, x, y, 7.5f, 7.5f, 5f, al(70 + Math.round(60 * k), 0x56D8FF)); disc(g, x, y, 7.5f, 7.5f, 2.5f, 0xFFFFFFFF); }  // Neon glow
            case "fx8" -> {                                                                             // Bounce in
                float p = (s * 0.8f) % 1f; int by = Math.round((float) Math.abs(Math.cos(p * Math.PI * 2.5)) * (1f - p) * 9f); int h = by == 0 ? 4 : 5;
                r(g, x, y, 5, 10 - by, 6, h, 0xFF8FE88F); r(g, x, y, 4, 14, 8, 1, 0xFF4B5575);
            }
            case "fx9" -> {                                                                             // Typewriter: letters typed out, a cursor blinking after them
                int n = 1 + (int) (s * 2.5f) % 4; for (int i = 0; i < n; i++) r(g, x, y, 2 + i * 3, 7, 2, 4, 0xFFF4F4F8);
                if ((int) (s * 3f) % 2 == 0) r(g, x, y, 2 + n * 3, 5, 1, 7, 0xFF62E6A8);
            }
            case "fx10" -> { int by = Math.round((float) Math.sin(s * 1.8f) * 2.5f); r(g, x, y, 3, 6 + by, 10, 4, 0xFFBFE6FF); r(g, x, y, 5, 4 + by, 6, 2, 0xFFDDF3FF); r(g, x, y, 4 + (int) (Math.sin(s * 1.8f) * 1), 13, 8, 1, al(70, 0x000000)); }       // Float
            case "fx11" -> { int sh = (int) (s * 14f) % 3 - 1; r(g, x, y, 4 + sh, 5 + (int) (s * 11f) % 3 - 1, 8, 6, 0xFFFF8A5C); r(g, x, y, 1, 7, 1, 2, al(150, 0xFFFFFF)); r(g, x, y, 14, 7, 1, 2, al(150, 0xFFFFFF)); }      // Shake
            case "fx12" -> {                                                                            // Fire
                float f = 0.5f + 0.5f * (float) Math.sin(s * 9f);
                for (int yy = 2; yy <= 13; yy++) { int hw = Math.round((yy < 7 ? 1 + (yy - 2) * 0.6f : 4f - (yy - 7) * 0.4f)); int c0 = yy < 6 ? 0xFFFFD84A : yy < 10 ? 0xFFFF8A2A : 0xFFE0381C; r(g, x, y, 8 - hw, yy + (f > 0.5f && yy < 5 ? 0 : 0), hw * 2, 1, c0); }
                r(g, x, y, 7, 1 + Math.round(f), 2, 2, 0xFFFFF0A0);
            }
            case "fx13" -> {                                                                            // Frost: a snowflake, glinting
                int c0 = 0xFF9FE8FF; r(g, x, y, 7, 1, 2, 14, c0); r(g, x, y, 1, 7, 14, 2, c0);
                for (int k = 2; k <= 5; k++) { r(g, x, y, 7 - k + 0, 7 - k + 0, 1, 1, c0); r(g, x, y, 8 + k - 1, 7 - k, 1, 1, c0); r(g, x, y, 7 - k, 8 + k - 1, 1, 1, c0); r(g, x, y, 8 + k - 1, 8 + k - 1, 1, 1, c0); }
                if ((int) (s * 3f) % 2 == 0) r(g, x, y, 7, 7, 2, 2, 0xFFFFFFFF);
            }
            case "fx14" -> { int a1 = 120 + (int) (135 * Math.sin(s * 4f)), a2 = 120 + (int) (135 * Math.sin(s * 4f + 2.1f)), a3 = 120 + (int) (135 * Math.sin(s * 4f + 4.2f)); spark(g, x, y, 4, 5, 3, al(a1, 0xFFE9A0)); spark(g, x, y, 11, 4, 2, al(a2, 0xFFFFFF)); spark(g, x, y, 9, 11, 3, al(a3, 0xFFD84A)); }      // Twinkle
            case "fx15" -> { for (int i = 0; i < 5; i++) { float k = ((s * 1.2f) % 2f) - i * 0.25f; int a2 = Math.round(255 * Math.max(0f, Math.min(1f, 1.2f - k))); r(g, x, y, 1 + i * 3, 5, 2, 6, al(a2, 0xFFF4C26B)); } }       // Fade away: letters fading one by one
            case "fx16" -> { float p = (s * 0.9f) % 1f; for (int i = 0; i < 3; i++) { int by = Math.round(10 - (p * 8f) - i * 0.5f); r(g, x, y, 2 + i * 4, Math.max(1, by), 3, 4, al(Math.round(255 * (1f - p)), 0xFFB9E6FF)); } r(g, x, y, 1, 14, 14, 1, 0xFF4B5575); }      // Fly up
            case "fx17" -> { float p = (s * 0.9f) % 1f; int w = Math.round(10 * (1f - p)); if (w > 0) r(g, x, y, 8 - w / 2, 8 - w / 2, w, w, 0xFFFF9AC8); r(g, x, y, 1, 14, 14, 1, 0xFF4B5575); }      // Shrink
            case "fx18" -> { float p = (s * 0.9f) % 1f; int[][] d = {{-3, -2}, {3, -3}, {-2, 3}, {3, 3}, {0, -4}, {-4, 1}}; for (int[] q : d) r(g, x, y, 7 + Math.round(q[0] * p * 2f), 7 + Math.round(q[1] * p * 2f), 2, 2, al(Math.round(255 * (1f - p * 0.8f)), 0xFFFFB86B)); }      // Scatter
            case "fx19" -> { float p = (s * 0.9f) % 1f; for (int i = 0; i < 3; i++) { int bx = Math.round(-8 + (1f - (1f - p) * (1f - p)) * (8 + i * 3)); r(g, x, y, 1 + bx, 3 + i * 4, 4, 3, al(Math.round(255 * Math.min(1f, p * 2f)), 0xFF9DD6FF)); } }       // Slide in
            case "fx20" -> { float p = (float) Math.abs(Math.cos(s * 2.2f)); int w = Math.max(1, Math.round(10 * p)); r(g, x, y, 8 - w / 2, 3, w, 10, 0xFFB6A0FF); r(g, x, y, 8 - w / 2, 3, w, 2, 0xFFE0D6FF); }       // Flip in
            case "fx21" -> { float a2 = (float) Math.sin(s * 3f); for (int i = 0; i < 9; i++) { int ox = Math.round((i - 4) * 1.5f), oy = Math.round(a2 * (i - 4) * 0.5f); r(g, x, y, 7 + ox, 7 + oy, 2, 2, 0xFFFFD27A); } r(g, x, y, 7, 2, 2, 2, 0xFF4B5575); }       // Swing
            case "fx22" -> { r(g, x, y, 3, 3, 10, 10, 0xFFE0A21B); r(g, x, y, 4, 4, 8, 8, 0xFFFFC83C); r(g, x, y, 4, 4, 8, 2, 0xFFFFF0A0); int sh = Math.round(((s * 1.4f) % 2f) * 12f) - 2; if (sh >= 3 && sh < 12) r(g, x, y, sh, 4, 2, 8, al(190, 0xFFFFFFFF)); }       // Gold
            case "fx23" -> { float p = (s * 0.9f) % 1f; for (int i = 0; i < 3; i++) r(g, x, y, 1 + Math.round(p * p * (7 + i * 2)), 3 + i * 4, 4, 3, al(Math.round(255 * (1f - p)), 0xFFFFA0A0)); }       // Slide out
            case "fx24" -> { float p = (s * 0.9f) % 1f; r(g, x, y, 2, 2, 12, 5, 0xFFFF9A5C); for (int i = 0; i < 4; i++) { int dl = Math.round(p * (3 + (i % 3) * 3)); r(g, x, y, 3 + i * 3, 7, 2, 1 + dl, al(Math.round(255 * (1f - p * 0.5f)), 0xFFFF9A5C)); } }       // Melt
            case "exp_rare" -> {                                                                        // a cut gem with a glint passing over it
                int top = 0xFF9BEAFF, mid = 0xFF3FB8E8, low = 0xFF1C7FB8;
                for (int yy = 3; yy <= 5; yy++) { int hw = 3 + (yy - 3); r(g, x, y, 8 - hw, yy, hw * 2, 1, top); }
                for (int yy = 6; yy <= 13; yy++) { int hw = Math.max(1, 6 - (yy - 6) * 6 / 8 - (yy > 11 ? 1 : 0)); r(g, x, y, 8 - hw, yy, hw * 2, 1, yy < 9 ? mid : low); }
                r(g, x, y, 5, 3, 6, 1, 0xFFE8FAFF); r(g, x, y, 4, 5, 8, 1, 0xFF7ED8F6); r(g, x, y, 7, 6, 2, 6, al(90, 0xFFFFFF));
                int gx = Math.round(((s * 1.1f) % 1.7f) * 9f) + 1; g.enableScissor(x + 3, y + 3, x + 13, y + 14); for (int j = 0; j < 11; j++) r(g, x, y, gx - j / 2, 3 + j, 2, 1, 0x99FFFFFF); g.disableScissor();
            }
            case "exp_book" -> {                                                                        // an enchanted book, its runes glowing
                int cov = 0xFF6B3FA0, page = 0xFFEDE3C8;
                r(g, x, y, 2, 2, 12, 12, cov); r(g, x, y, 3, 3, 10, 10, page); r(g, x, y, 2, 2, 2, 12, 0xFF4A2A78); r(g, x, y, 2, 13, 12, 1, 0xFF3A2060);
                for (int k = 0; k < 3; k++) { int a2 = 140 + (int) (115 * Math.sin(s * 3f + k * 1.3f)); r(g, x, y, 6, 5 + k * 3, 5 - (k % 2), 1, al(a2, 0xB56BFF)); }
                r(g, x, y, 11, 2, 1, 5, 0xFFFF72C6);
            }
            case "experiments" -> {                                                                     // a flask of bubbling liquid, a glow rising in it
                int glass = 0xFFB9D2E6, liq = Canvas.mix(0xFF39D98A, 0xFF56D8FF, (float) (Math.sin(s * 1.6f) * 0.5f + 0.5f));
                r(g, x, y, 5, 1, 6, 1, 0xFFE6F2FA); r(g, x, y, 6, 2, 1, 3, glass); r(g, x, y, 9, 2, 1, 3, glass);
                for (int yy = 5; yy <= 13; yy++) {
                    int hw = Math.round(2.4f + (yy - 5) * 0.62f), x0 = 8 - hw, w = hw * 2;
                    if (yy >= 8) r(g, x, y, x0 + 1, yy, w - 2, 1, Canvas.shade(liq, 1f - (yy - 8) * 0.05f));
                    r(g, x, y, x0, yy, 1, 1, glass); r(g, x, y, x0 + w - 1, yy, 1, 1, glass);
                }
                r(g, x, y, 2, 14, 12, 1, glass);
                r(g, x, y, 5, 9, 2, 1, al(150, 0xFFFFFF));
                for (int k = 0; k < 3; k++) {
                    float p = (s * 0.8f + k * 0.34f) % 1f; int by = Math.round(12f - p * 9.5f), bx = 6 + k * 2 + Math.round((float) Math.sin(s * 3f + k) * 0.6f);
                    r(g, x, y, bx, by, 1, 1, al((int) (230 * (1f - p * 0.6f)), 0xFFFFFF));
                }
            }
            case "fishing" -> {                                                                         // a rod, a bobber with ripples, rolling waves and a fish leaping out
                float u = (s * 0.32f) % 1f;
                disc(g, x, y, 8f, 6f, 8f, al(18, 0x9AD8FF));
                for (int j = 0; j < 6; j++) {                                                                  // water in layers, light on top
                    int wy = 10 + j; int c = Canvas.mix(0xFF4FB4F0, 0xFF16407E, j / 5f);
                    r(g, x, y, 0, wy, 16, 1, c);
                    for (int i = 0; i < 16; i++) if ((int) Math.floor(Math.sin(i * 0.9 + s * 2.2 - j * 1.3) * 1.0 + 0.5) == 1 && j < 4) r(g, x, y, i, wy, 1, 1, Canvas.mix(c, 0xFFFFFFFF, 0.35f - j * 0.07f));
                }
                ln(g, x, y, 0.5f, 12f, 8f, 1.5f, 0xFF9A6634); ln(g, x, y, 0.5f, 13f, 8f, 2.5f, 0xFF5A3A1E); ln(g, x, y, 8f, 1.5f, 9f, 1f, 0xFFB07A44);   // the rod
                float dip = u > 0.18f && u < 0.30f ? 1.2f : (float) Math.sin(s * 2.6f) * 0.5f;
                float bxf = 11f, byf = 9f + dip;
                for (int i = 0; i <= 9; i++) { float q = i / 9f; r(g, x, y, Math.round(9f + (bxf - 9f) * q + (float) Math.sin(q * 3.1f) * 0.8f), Math.round(1f + (byf - 1f) * q * q), 1, 1, al(210, 0xFFEFF2FA)); }   // the line sags
                r(g, x, y, Math.round(bxf) - 1, Math.round(byf) - 1, 3, 2, 0xFFE5484D); r(g, x, y, Math.round(bxf) - 1, Math.round(byf) + 1, 3, 1, 0xFFF4F4F8); r(g, x, y, Math.round(bxf), Math.round(byf) - 2, 1, 1, 0xFFF4F4F8);
                float rp = (s * 0.9f) % 1f;                                                                    // a ripple spreads from the bobber
                for (int i = -3; i <= 3; i++) { int rx = Math.round(bxf + i * (1f + rp * 1.4f)); if (Math.abs(i) >= 2) r(g, x, y, rx, 11, 1, 1, al(Math.round(190 * (1f - rp)), 0xFFFFFF)); }
                if (u > 0.55f && u < 0.92f) {                                                                  // a fish leaps in an arc
                    float f = (u - 0.55f) / 0.37f, fx = 2f + 12f * f, fy = 12f - 11f * 4f * f * (1f - f);
                    float tilt = (2f * f - 1f) * 1.6f; int ix = Math.round(fx), iy = Math.round(fy);
                    r(g, x, y, ix, iy, 4, 2, 0xFFFF9A3C); r(g, x, y, ix + 1, iy, 2, 1, 0xFFFFD08A); r(g, x, y, ix, iy + 1, 4, 1, 0xFFD9701C);
                    r(g, x, y, f < 0.5f ? ix - 1 : ix + 4, iy - (tilt < 0 ? 1 : 0), 1, 3, 0xFFE06A1C); r(g, x, y, f < 0.5f ? ix + 3 : ix, iy, 1, 1, 0xFF22263A);
                    if (f < 0.2f || f > 0.8f) for (int i = 0; i < 5; i++) { float dd = f < 0.2f ? f : 1f - f; r(g, x, y, Math.round(fx + 2 + (i - 2) * (1f + dd * 8f)), Math.round(11f - (i % 2 == 0 ? 3f : 2f) * (1f - Math.abs(i - 2) / 3f) * dd * 6f), 1, 1, al(220, 0xFFFFFF)); }
                }
            }
            case "mytho" -> {                                                                           // a laurel wreath with a golden lightning bolt inside
                float pu = 0.5f + 0.5f * (float) Math.sin(s * 3f);
                disc(g, x, y, 8f, 8.5f, 5f, al(30 + Math.round(40 * pu), 0xFFC857));
                for (int k = 0; k < 7; k++) {
                    double an = Math.toRadians(105 + k * 22); int lx = Math.round(7.5f + (float) Math.cos(an) * 6.4f), ly = Math.round(8f - (float) Math.sin(an) * 6.4f);
                    int c1 = k % 2 == 0 ? 0xFF5FC24A : 0xFF3F9A38;
                    r(g, x, y, lx, ly, 2, 2, c1); r(g, x, y, lx, ly, 1, 1, 0xFF9BE87A);
                    int rx = 13 - lx; r(g, x, y, rx, ly, 2, 2, c1); r(g, x, y, rx + 1, ly, 1, 1, 0xFF9BE87A);
                }
                boolean fl = s % 1.7f < 0.2f; int bc = fl ? 0xFFFFFFFF : 0xFFFFC857;
                r(g, x, y, 8, 3, 3, 1, bc); r(g, x, y, 7, 4, 3, 1, bc); r(g, x, y, 6, 5, 3, 1, bc); r(g, x, y, 6, 6, 5, 1, bc); r(g, x, y, 8, 7, 3, 1, bc); r(g, x, y, 7, 8, 3, 1, bc); r(g, x, y, 6, 9, 3, 1, bc); r(g, x, y, 6, 10, 2, 1, bc);
                if (fl) spark(g, x, y, 8, 7, 3, 0xFFFFF3A0);
            }
            case "sun" -> {                                                                             // a rising sun, its rays turning
                disc(g, x, y, 8f, 9f, 3.6f, 0xFFFFC857); disc(g, x, y, 8f, 9f, 2.2f, 0xFFFFE9A8);
                for (int k = 0; k < 8; k++) { double an = k * Math.PI / 4 + s * 0.8; int rx = Math.round(8f + (float) Math.cos(an) * 6.4f), ry = Math.round(9f + (float) Math.sin(an) * 6.4f); r(g, x, y, rx, ry, 1, 1, k % 2 == 0 ? 0xFFFFE066 : 0xFFFFA83A); }
                r(g, x, y, 0, 13, 16, 3, 0xFF3F7A33); r(g, x, y, 0, 13, 16, 1, 0xFF6FCB5A);
            }
            case "myth_minos_hunter" -> {                                                               // a bow drawing an arrow, the tip glinting
                float pull = 1f + (float) Math.sin(s * 3f) * 1f; int po = Math.round(pull);
                for (int j = 1; j <= 14; j++) { float q = (j - 7.5f) / 6.5f; int bx = 3 + Math.round(q * q * 3f); r(g, x, y, bx, j, 2, 1, 0xFF9A6A38); r(g, x, y, bx, j, 1, 1, 0xFFB98A52); }
                ln(g, x, y, 6, 1, 6 - po, 7, 0xFFE8E8F0); ln(g, x, y, 6 - po, 7, 6, 14, 0xFFE8E8F0);
                r(g, x, y, 5 - po, 7, 9 + po, 1, 0xFFE6C36A); r(g, x, y, 12, 6, 1, 3, 0xFFB8C0D4); r(g, x, y, 13, 7, 2, 1, 0xFFE8EEF8); r(g, x, y, 4 - po, 6, 2, 1, 0xFFE5484D); r(g, x, y, 4 - po, 8, 2, 1, 0xFFE5484D);
                if (s % 1.2f < 0.2f) spark(g, x, y, 14, 7, 1, 0xFFFFFFFF);
            }
            case "myth_siamese_lynxes" -> {                                                             // two cats side by side, blinking blue eyes
                boolean blink = s % 3.2f < 0.14f;
                for (int k = 0; k < 2; k++) {
                    int hx = k == 0 ? 1 : 8, face = k == 0 ? 0xFFF2DDB0 : 0xFFD8B27C, mask = k == 0 ? 0xFF8A6A48 : 0xFF4E3220;
                    r(g, x, y, hx, 6, 7, 6, face); r(g, x, y, hx + 1, 12, 5, 1, face);
                    r(g, x, y, hx, 3, 2, 3, mask); r(g, x, y, hx + 5, 3, 2, 3, mask); r(g, x, y, hx, 2, 1, 1, mask); r(g, x, y, hx + 6, 2, 1, 1, mask);
                    r(g, x, y, hx + 2, 7, 3, 3, mask); r(g, x, y, hx + 3, 7, 1, 5, face);
                    r(g, x, y, hx + 1, 8, 2, blink ? 1 : 2, 0xFF3CC8F0); r(g, x, y, hx + 4, 8, 2, blink ? 1 : 2, 0xFF3CC8F0);
                    r(g, x, y, hx + 3, 10, 1, 1, 0xFFE58AA0);
                }
            }
            case "myth_gaia_construct" -> {                                                             // a mossy stone golem, its eyes glowing
                float gl = 0.6f + 0.4f * (float) Math.sin(s * 3f);
                r(g, x, y, 3, 2, 10, 12, 0xFF7C8A78); r(g, x, y, 3, 2, 10, 1, 0xFFA3B09E); r(g, x, y, 12, 3, 1, 11, 0xFF566052); r(g, x, y, 3, 13, 10, 1, 0xFF566052);
                r(g, x, y, 3, 2, 6, 2, 0xFF4FA84A); r(g, x, y, 4, 4, 2, 1, 0xFF4FA84A); r(g, x, y, 10, 3, 3, 1, 0xFF6FCB5A); r(g, x, y, 11, 4, 1, 3, 0xFF4FA84A);
                r(g, x, y, 4, 6, 3, 3, 0xFF2A332A); r(g, x, y, 9, 6, 3, 3, 0xFF2A332A);
                r(g, x, y, 5, 7, 2, 2, al(120 + Math.round(135 * gl), 0xFFE066)); r(g, x, y, 9, 7, 2, 2, al(120 + Math.round(135 * gl), 0xFFE066));
                r(g, x, y, 5, 11, 6, 1, 0xFF2A332A); r(g, x, y, 7, 5, 1, 1, 0xFF566052); r(g, x, y, 8, 9, 1, 2, 0xFF566052);
            }
            case "myth_minotaur" -> {                                                                   // a bull's head, red eyes, a ring in its nose
                r(g, x, y, 4, 4, 8, 6, 0xFF8A5A2C); r(g, x, y, 5, 10, 6, 4, 0xFFC49A6C); r(g, x, y, 4, 4, 8, 1, 0xFFA8733A);
                r(g, x, y, 3, 4, 2, 1, 0xFFF1E6C8); r(g, x, y, 2, 3, 1, 1, 0xFFF1E6C8); r(g, x, y, 1, 1, 2, 2, 0xFFF1E6C8);
                r(g, x, y, 11, 4, 2, 1, 0xFFF1E6C8); r(g, x, y, 13, 3, 1, 1, 0xFFF1E6C8); r(g, x, y, 13, 1, 2, 2, 0xFFF1E6C8);
                r(g, x, y, 3, 5, 1, 2, 0xFF6B4423); r(g, x, y, 12, 5, 1, 2, 0xFF6B4423);
                int ea = 150 + (int) (105 * Math.sin(s * 5f));
                r(g, x, y, 5, 6, 2, 2, al(ea, 0xFF3030)); r(g, x, y, 9, 6, 2, 2, al(ea, 0xFF3030));
                r(g, x, y, 6, 11, 1, 1, 0xFF3A2410); r(g, x, y, 9, 11, 1, 1, 0xFF3A2410);
                r(g, x, y, 7, 13, 2, 1, 0xFFFFC857); if (s % 2f < 0.15f) spark(g, x, y, 8, 14, 1, 0xFFFFFFFF);
            }
            case "myth_minos_champion" -> {                                                             // a gold crested helmet, the plume swaying
                int sw = Math.round((float) Math.sin(s * 2.4f) * 1.2f);
                r(g, x, y, 4 + sw, 2, 8, 2, 0xFFD33F3F); r(g, x, y, 5 + sw * 2, 1, 6, 1, 0xFFE5584E); r(g, x, y, 6 + sw, 3, 5, 1, 0xFF9E2A2A);
                r(g, x, y, 4, 4, 8, 6, 0xFFE6B422); r(g, x, y, 3, 6, 10, 4, 0xFFE6B422); r(g, x, y, 4, 4, 8, 1, 0xFFFFE08A); r(g, x, y, 3, 6, 1, 7, 0xFFC9971A); r(g, x, y, 12, 6, 1, 7, 0xFFC9971A);
                r(g, x, y, 3, 10, 3, 4, 0xFFE6B422); r(g, x, y, 10, 10, 3, 4, 0xFFE6B422); r(g, x, y, 3, 13, 3, 1, 0xFF9A7410); r(g, x, y, 10, 13, 3, 1, 0xFF9A7410);
                r(g, x, y, 6, 7, 4, 7, 0xFF2A1E10); r(g, x, y, 7, 7, 2, 5, 0xFFE6B422);
                r(g, x, y, 6, 8, 1, 1, al(150 + (int) (105 * Math.sin(s * 4f)), 0xFFFFFF)); r(g, x, y, 9, 8, 1, 1, al(150 + (int) (105 * Math.sin(s * 4f)), 0xFFFFFF));
            }
            case "myth_manticore" -> {                                                                  // a purple lion head with fangs, wings and a stinger
                int fl = Math.round((float) Math.sin(s * 3f) * 1f);
                r(g, x, y, 1, 3 + fl, 3, 4, 0xFF7A3CB0); r(g, x, y, 12, 3 + fl, 3, 4, 0xFF7A3CB0); r(g, x, y, 0, 2 + fl, 2, 2, 0xFF5B2A86); r(g, x, y, 14, 2 + fl, 2, 2, 0xFF5B2A86);
                r(g, x, y, 4, 3, 8, 3, 0xFFD9A0FF); r(g, x, y, 3, 5, 10, 7, 0xFFE0A24A); r(g, x, y, 4, 12, 8, 2, 0xFFE0A24A); r(g, x, y, 3, 5, 1, 7, 0xFFB5782C); r(g, x, y, 12, 5, 1, 7, 0xFFB5782C);
                int ea = 170 + (int) (85 * Math.sin(s * 4f));
                r(g, x, y, 5, 7, 2, 2, al(ea, 0xFFF0A0)); r(g, x, y, 9, 7, 2, 2, al(ea, 0xFFF0A0)); r(g, x, y, 7, 9, 2, 2, 0xFF5A2E10);
                r(g, x, y, 5, 11, 1, 3, 0xFFFFFFFF); r(g, x, y, 10, 11, 1, 3, 0xFFFFFFFF);
                float tw = 1f + (float) Math.sin(s * 3f); r(g, x, y, 13, 0, 1, 2, 0xFFB05AE0); r(g, x, y, Math.round(12 + tw), 0, 1, 1, 0xFFFFD166);
            }
            case "myth_king_minos" -> {                                                                 // a crowned bull king, glowing red eyes
                r(g, x, y, 4, 3, 8, 8, 0xFF8A5A2C); r(g, x, y, 5, 11, 6, 3, 0xFFC49A6C); r(g, x, y, 3, 5, 1, 4, 0xFF6B4423); r(g, x, y, 12, 5, 1, 4, 0xFF6B4423);
                r(g, x, y, 3, 4, 2, 1, 0xFFF1E6C8); r(g, x, y, 1, 2, 2, 2, 0xFFF1E6C8); r(g, x, y, 0, 0, 1, 2, 0xFFF1E6C8); r(g, x, y, 11, 4, 2, 1, 0xFFF1E6C8); r(g, x, y, 13, 2, 2, 2, 0xFFF1E6C8); r(g, x, y, 15, 0, 1, 2, 0xFFF1E6C8);
                r(g, x, y, 4, 2, 8, 2, 0xFFFFC857); for (int k = 0; k < 3; k++) r(g, x, y, 4 + k * 3, 0, 2, 2, 0xFFFFC857); r(g, x, y, 4, 2, 8, 1, 0xFFFFE9A8);
                r(g, x, y, 7, 1, 2, 1, al(150 + (int) (105 * Math.sin(s * 4f)), 0xFF3030));
                int ea = 160 + (int) (95 * Math.sin(s * 5f));
                r(g, x, y, 5, 6, 2, 2, al(ea, 0xFF3030)); r(g, x, y, 9, 6, 2, 2, al(ea, 0xFF3030)); r(g, x, y, 6, 11, 1, 1, 0xFF3A2410); r(g, x, y, 9, 11, 1, 1, 0xFF3A2410);
                if (s % 2.2f < 0.15f) spark(g, x, y, 8, 1, 1, 0xFFFFFFFF);
            }
            case "myth_stranded_nymph" -> {                                                             // a nymph with sea-green hair, a drop falling from her
                int sw = Math.round((float) Math.sin(s * 2f));
                r(g, x, y, 3 + sw, 2, 10, 6, 0xFF3FB8A5); r(g, x, y, 2 + sw, 6, 3, 8, 0xFF3FB8A5); r(g, x, y, 11 + sw, 6, 3, 8, 0xFF3FB8A5); r(g, x, y, 4 + sw, 2, 8, 1, 0xFF7FF0DC);
                r(g, x, y, 5, 5, 6, 7, 0xFFF2D2B0); r(g, x, y, 5, 5, 6, 1, 0xFF3FB8A5); r(g, x, y, 6, 8, 1, 1, 0xFF1E5E80); r(g, x, y, 9, 8, 1, 1, 0xFF1E5E80); r(g, x, y, 7, 10, 2, 1, 0xFFE58AA0);
                float dp = (s * 1.4f) % 1f; r(g, x, y, 13, 3 + Math.round(dp * 9f), 1, 2, al(220 - Math.round(dp * 120), 0x9FE6FF));
            }
            case "myth_cretan_bull" -> {                                                                // a charging bull, steam from its nostrils
                r(g, x, y, 3, 5, 10, 8, 0xFFB5642A); r(g, x, y, 4, 4, 8, 1, 0xFFD98B4A); r(g, x, y, 5, 10, 6, 4, 0xFFE0B080);
                r(g, x, y, 2, 3, 2, 2, 0xFFF1E6C8); r(g, x, y, 1, 1, 1, 3, 0xFFF1E6C8); r(g, x, y, 12, 3, 2, 2, 0xFFF1E6C8); r(g, x, y, 14, 1, 1, 3, 0xFFF1E6C8);
                int ea = 160 + (int) (95 * Math.sin(s * 6f)); r(g, x, y, 5, 7, 2, 2, al(ea, 0xFF3030)); r(g, x, y, 9, 7, 2, 2, al(ea, 0xFF3030));
                r(g, x, y, 6, 12, 1, 1, 0xFF3A2410); r(g, x, y, 9, 12, 1, 1, 0xFF3A2410);
                float st = (s * 1.8f) % 1f; r(g, x, y, 5, 14 - Math.round(st * 2f), 2, 1, al(190 - Math.round(st * 150), 0xFFFFFF)); r(g, x, y, 9, 14 - Math.round(st * 2f), 2, 1, al(190 - Math.round(st * 150), 0xFFFFFF));
            }
            case "myth_harpy" -> {                                                                      // a purple harpy, wings beating
                int fl = Math.round((float) Math.sin(s * 5f) * 2f);
                for (int j = 0; j < 8; j++) { int w = 7 - j; r(g, x, y, 5 - w, 2 + j - fl * (j < 4 ? 1 : 0), w, 1, j % 2 == 0 ? 0xFF7A5CC8 : 0xFF9C86D8); r(g, x, y, 11, 2 + j - fl * (j < 4 ? 1 : 0), w, 1, j % 2 == 0 ? 0xFF7A5CC8 : 0xFF9C86D8); }
                r(g, x, y, 5, 3, 6, 6, 0xFFF2D2B0); r(g, x, y, 5, 2, 6, 2, 0xFF5B3EA8); r(g, x, y, 6, 6, 1, 1, 0xFFFFE066); r(g, x, y, 9, 6, 1, 1, 0xFFFFE066); r(g, x, y, 7, 8, 2, 1, 0xFFE58AA0);
                r(g, x, y, 6, 9, 4, 4, 0xFF9C86D8); r(g, x, y, 5, 13, 2, 2, 0xFFE0A21B); r(g, x, y, 9, 13, 2, 2, 0xFFE0A21B);
            }
            case "myth_sphinx" -> {                                                                     // a gold-and-blue headdress, glowing eyes
                r(g, x, y, 3, 1, 10, 3, 0xFF2E6FD8); r(g, x, y, 4, 1, 8, 1, 0xFF4F94FF); r(g, x, y, 2, 4, 3, 10, 0xFFE8C874); r(g, x, y, 11, 4, 3, 10, 0xFFE8C874);
                for (int k = 0; k < 5; k++) { r(g, x, y, 2, 4 + k * 2, 3, 1, 0xFF2E6FD8); r(g, x, y, 11, 4 + k * 2, 3, 1, 0xFF2E6FD8); }
                r(g, x, y, 5, 4, 6, 8, 0xFFD8A24A); r(g, x, y, 5, 4, 6, 1, 0xFFF2C77A); r(g, x, y, 6, 12, 4, 2, 0xFF2E6FD8);
                int ea = 170 + (int) (85 * Math.sin(s * 3.4f)); r(g, x, y, 6, 6, 2, 2, al(ea, 0x7FE5FF)); r(g, x, y, 9, 6, 2, 2, al(ea, 0x7FE5FF)); r(g, x, y, 7, 9, 2, 1, 0xFF8A5A22);
                if (s % 2.4f < 0.15f) spark(g, x, y, 8, 3, 1, 0xFFFFFFFF);
            }
            case "myth_minos_inquisitor" -> {                                                           // a burning eye
                for (int k = 0; k < 3; k++) {
                    float fl = (float) Math.sin(s * 7f + k * 2.1f) * 0.5f + 0.5f; int fx = 4 + k * 4, fh = 3 + Math.round(fl * 3f) + (k == 1 ? 2 : 0);
                    r(g, x, y, fx, 7 - fh, 2, fh, 0xFFFF8A3D); r(g, x, y, fx, 7 - fh + 1, 1, fh - 1, 0xFFFFC857); r(g, x, y, fx + 1, 7 - fh + 2, 1, 1, 0xFFFFE9A8);
                }
                int[] ew = {6, 5, 4, 2};
                for (int j = -3; j <= 3; j++) { int w = ew[Math.abs(j)]; r(g, x, y, 8 - w, 10 + j, w * 2, 1, Math.abs(j) == 3 ? 0xFF8A2E0A : 0xFFFFE7B0); if (Math.abs(j) < 3) { r(g, x, y, 8 - w, 10 + j, 1, 1, 0xFF8A2E0A); r(g, x, y, 7 + w, 10 + j, 1, 1, 0xFF8A2E0A); } }
                int px = Math.round((float) Math.sin(s * 1.6f) * 2f);
                disc(g, x, y, 8 + px, 10, 2.4f, 0xFFFF6A1A); r(g, x, y, 8 + px, 8, 1, 5, 0xFF2A0F00); r(g, x, y, 7 + px, 9, 1, 1, 0xFFFFE9A8);
            }
            case "position" -> {                                                                        // a frame with a glowing marker
                r(g, x, y, 1, 2, 14, 12, 0xFF2A3042); g.fill(x, y + 1, x + 16, y + 2, 0xFF9DB2CC); g.fill(x, y + 14, x + 16, y + 15, 0xFF9DB2CC); g.fill(x, y + 1, x + 1, y + 15, 0xFF9DB2CC); g.fill(x + 15, y + 1, x + 16, y + 15, 0xFF9DB2CC);
                float p = (float) (Math.sin(s * 4f) * 0.5f + 0.5f);
                r(g, x, y, 5, 7, 6, 4, al(70 + Math.round(120 * p), 0xFFFFB300)); r(g, x, y, 6, 8, 4, 2, 0xFFFFC857);
            }
            case "pos_top", "pos_bottom" -> {                                                           // a screen with the words at the top or the bottom
                boolean top = name.equals("pos_top");
                r(g, x, y, 1, 2, 14, 12, 0xFF1B2030); g.outline(x + 1, y + 2, 14, 12, 0xFF4A5470);
                r(g, x, y, 3, top ? 8 : 4, 10, 4, 0xFF2B3A5A);
                float sh = (s * 1.6f) % 1.6f; int ty = top ? 4 : 10;
                r(g, x, y, 3, ty, 10, 1, 0xFFE8ECF4); r(g, x, y, 3, ty + 2, 7, 1, 0xFF9AA6BC);
                r(g, x, y, 3 + Math.round(sh * 6f), ty, 2, 1, 0xFFFFFFFF);
            }
            case "dock_top", "dock_bottom", "dock_off" -> {                                             // the menu with the live preview above or below it
                boolean top = !name.equals("dock_bottom"), off = name.equals("dock_off");
                float pu = 0.5f + 0.5f * (float) Math.sin(s * 3f);
                r(g, x, y, 1, 1, 14, 14, 0xFF161A28); g.outline(x + 1, y + 1, 14, 14, off ? 0xFF4A4D5C : 0xFF5A6A9A);
                int py = top ? 2 : 10;
                r(g, x, y, 2, py, 12, 4, off ? 0xFF2A2D3A : Canvas.mix(0xFF3A5AA0, 0xFF6C9CFF, pu));
                if (!off) { r(g, x, y, 7, py + 1, 1, 2, 0xFFFFFFFF); r(g, x, y, 8, py + 1, 1, 2, 0xFFE8ECF4); }
                int my = top ? 7 : 3;
                for (int k = 0; k < 3; k++) r(g, x, y, 3, my + k * 2, k == 1 ? 7 : 9, 1, off ? 0xFF3A3D4A : 0xFF7B86A6);
                if (off) { ln(g, x, y, 3, 13, 12, 2, 0xFFE5484D); ln(g, x, y, 3, 12, 11, 2, 0xFFB43A3E); }
            }
            case "bars" -> {                                                                            // a picture with cinematic bars sliding in and out
                int bh = 2 + Math.round(1.6f * (0.5f + 0.5f * (float) Math.sin(s * 2.4f)));
                r(g, x, y, 1, 2, 14, 12, 0xFF141826);
                for (int j = 0; j < 10; j++) r(g, x, y, 2, 3 + j, 12, 1, Canvas.mix(0xFF4F9BE0, 0xFFF6B77A, j / 9f));
                disc(g, x, y, 10.5f, 8f, 1.9f + 0.3f * (float) Math.sin(s * 4f), 0xFFFFE08A);
                for (int k = 0; k < 12; k++) { int hh = 2 + Math.round((float) (1.4 + 1.4 * Math.sin(k * 0.75 + 1.1))); r(g, x, y, 2 + k, 13 - hh, 1, hh, k % 3 == 0 ? 0xFF2F7A52 : 0xFF276646); }
                r(g, x, y, 2, 3, 12, bh, 0xFF05060A); r(g, x, y, 2, 13 - bh, 12, bh, 0xFF05060A);
                r(g, x, y, 2, 3 + bh, 12, 1, al(90, 0xFFFFFF)); r(g, x, y, 2, 12 - bh, 12, 1, al(90, 0xFFFFFF));
            }
            case "blur" -> {                                                                            // dots that drift in and out of focus
                float f = (float) (Math.sin(s * 2.2f) * 0.5f + 0.5f);
                for (int j = 0; j < 3; j++) for (int i = 0; i < 3; i++) disc(g, x, y, 3 + i * 5, 3 + j * 5, 1f + f * 1.6f, al(255 - Math.round(f * 150), (i + j) % 2 == 0 ? 0x56D8FF : 0xB56BFF));
            }
            case "text_top", "text_bottom" -> {                                                         // a screen with the title lines at the top or the bottom
                boolean top = name.equals("text_top");
                r(g, x, y, 1, 2, 14, 12, 0xFF1B2030); g.outline(x + 1, y + 2, 14, 12, 0xFF5A6482);
                int ty = top ? 4 : 9, sy = top ? 9 : 4; float gl = (s * 1.2f) % 1.6f;
                r(g, x, y, 3, sy, 10, 3, 0xFF2F5C8F); r(g, x, y, 3, sy, 10, 1, 0xFF4F86C2);
                r(g, x, y, 3, ty, 10, 1, 0xFFFFFFFF); r(g, x, y, 5, ty + 2, 6, 1, 0xFF8E98AC);
                int gx = Math.round(gl * 8f); if (gx < 10) r(g, x, y, 3 + gx, ty, 2, 1, 0xFFFFE08A);
            }
            case "barheight" -> {                                                                       // two bars growing and shrinking round a double arrow
                int t = 2 + Math.round(1f * (0.5f + 0.5f * (float) Math.sin(s * 3f)));
                r(g, x, y, 1, 1, 14, t, 0xFF0B0D14); r(g, x, y, 1, 15 - t, 14, t, 0xFF0B0D14);
                int a = 150 + (int) (100 * Math.sin(s * 4f));
                r(g, x, y, 1, 1 + t, 14, 1, al(a, 0xFF8FA8)); r(g, x, y, 1, 14 - t, 14, 1, al(a, 0xFF8FA8));
                r(g, x, y, 8, 5, 1, 1, 0xFFE8ECF4); r(g, x, y, 7, 6, 3, 1, 0xFFE8ECF4); r(g, x, y, 8, 7, 1, 2, 0xFFB8BECC);
                r(g, x, y, 7, 9, 3, 1, 0xFFE8ECF4); r(g, x, y, 8, 10, 1, 1, 0xFFE8ECF4);
            }
            case "size" -> {                                                                            // corner brackets breathing round a square
                int o = Math.round(1.2f * (0.5f + 0.5f * (float) Math.sin(s * 3f)));
                r(g, x, y, 5, 5, 6, 6, 0xFF2B4A6B); r(g, x, y, 5, 5, 6, 1, 0xFF4F7FB0);
                int c = 0xFF56D8FF;
                for (int q = 0; q < 4; q++) { int cx = q % 2 == 0 ? 1 - o : 14 + o, cy = q < 2 ? 1 - o : 14 + o, sx = q % 2 == 0 ? 1 : -1, sy = q < 2 ? 1 : -1;
                    r(g, x, y, sx > 0 ? cx : cx - 3, cy, 4, 1, c); r(g, x, y, cx, sy > 0 ? cy : cy - 3, 1, 4, c); }
                spark(g, x, y, 8, 8, 1, al(120 + (int) (130 * Math.sin(s * 4f)), 0xFFFFFF));
            }
            case "itemsize" -> {                                                                        // a gem that glints
                for (int j = -5; j <= 5; j++) { int w = 5 - Math.abs(j); r(g, x, y, 8 - w, 8 + j, w * 2 + 1, 1, j < 0 ? 0xFFB8F0FF : 0xFF2FC9B8); }
                int a = (int) (255 * Math.max(0, Math.sin(s * 4f))); spark(g, x, y, 6, 6, 1, al(a, 0xFFFFFF));
            }
            case "cube" -> {                                                                            // an isometric block, one edge lighting in turn
                for (int j = 0; j < 7; j++) { int half = j <= 3 ? j * 2 + 1 : (6 - j) * 2 + 1; r(g, x, y, 8 - half, 1 + j, half * 2, 1, j < 2 ? 0xFFF3DBA0 : 0xFFE0BE7C); }
                for (int xi = 0; xi < 7; xi++) { int yt = 4 + Math.round(xi * 0.66f); r(g, x, y, 1 + xi, yt, 1, 8, xi == 0 ? 0xFF8F6A38 : 0xFFB38A4E); r(g, x, y, 14 - xi, yt, 1, 8, xi == 0 ? 0xFF4F3818 : 0xFF6E5026); }
                r(g, x, y, 8, 8, 1, 7, 0xFF5A4220);
                int hot = (int) (s * 3f) % 3;
                if (hot == 0) { for (int k = 0; k < 7; k++) r(g, x, y, 1 + k, 4 + Math.round(k * 0.66f), 1, 1, 0xFFFFFFFF); }
                else if (hot == 1) r(g, x, y, 8, 8, 1, 7, 0xFFFFFFFF);
                else for (int k = 0; k < 7; k++) r(g, x, y, 14 - k, 4 + Math.round(k * 0.66f), 1, 1, 0xFFFFFFFF);
            }
            case "dice" -> {                                                                            // a die whose face keeps changing
                int v = (int) (s * 2f) % 6;
                r(g, x, y, 2, 2, 12, 12, 0xFFF4F4F8); r(g, x, y, 2, 13, 12, 1, 0xFFB8BECC);
                for (int pp : PIPS[v]) r(g, x, y, 4 + (pp % 3) * 3, 4 + (pp / 3) * 3, 2, 2, v == 0 ? 0xFFE5484D : 0xFF202432);
            }
            case "coin" -> {                                                                            // a coin with a glint sweeping over it
                disc(g, x, y, 7.5f, 7.5f, 5.6f, 0xFFFFC857); disc(g, x, y, 7.5f, 7.5f, 4f, 0xFFE0A21B); r(g, x, y, 7, 5, 2, 6, 0xFFFFE9A0);
                int gx = Math.round(((s * 1.2f) % 1.6f) * 9f) - 2; g.enableScissor(x + 2, y + 2, x + 14, y + 14); for (int j = 0; j < 12; j++) r(g, x, y, gx - j / 2, 2 + j, 2, 1, 0x99FFFFFF); g.disableScissor();
            }
            case "mf" -> {                                                                              // sparkles twinkling: luck
                int a = 120 + (int) (135 * Math.sin(s * 4f)), b2 = 120 + (int) (135 * Math.sin(s * 4f + 2f));
                spark(g, x, y, 7, 8, 4, al(a, 0x55E6FF)); spark(g, x, y, 12, 3, 1, al(b2, 0xFFFFFF)); spark(g, x, y, 3, 13, 1, al(255 - a / 2, 0xFFC857));
            }
            case "text" -> {                                                                            // an "A" that glows
                int a = 170 + (int) (85 * Math.sin(s * 4f));
                for (int j = 0; j < 5; j++) for (int i = 0; i < 5; i++) if (A[j].charAt(i) == 'X') r(g, x, y, 3 + i * 2, 3 + j * 2, 2, 2, al(a, 0xE8ECF4));
            }
            case "nothing2" -> { }
            case "bell" -> {                                                                            // a bell that rings: waves appear either side
                r(g, x, y, 7, 1, 2, 2, 0xFFFFC857); r(g, x, y, 5, 3, 6, 2, 0xFFFFC857); r(g, x, y, 4, 5, 8, 5, 0xFFFFC857); r(g, x, y, 3, 10, 10, 2, 0xFFE0A21B); r(g, x, y, 7, 13, 2, 2, 0xFF8A5A2C);
                float p = (s * 1.6f) % 1f; int a = (int) (255 * Math.sin(p * Math.PI));
                r(g, x, y, 1, 4, 1, 6, al(a, 0xFFFFFF)); r(g, x, y, 14, 4, 1, 6, al(a, 0xFFFFFF)); r(g, x, y, 0, 6, 1, 2, al(a / 2, 0xFFFFFF)); r(g, x, y, 15, 6, 1, 2, al(a / 2, 0xFFFFFF));
            }
            case "send" -> {                                                                            // a paper plane, its trail chasing
                tri(g, x, y, 14, 2, 2, 7, 7, 9, 0xFFF1F5FB); tri(g, x, y, 14, 2, 7, 9, 9, 14, 0xFFB4BED0); tri(g, x, y, 7, 9, 9, 14, 8, 10.5f, 0xFF8E9BB3);
                ln(g, x, y, 14, 2, 7, 9, 0xFF8E9BB3);
                for (int i = 0; i < 4; i++) { float q = ((s * 2f) - i * 0.25f) % 1f; if (q < 0) q += 1f; r(g, x, y, 1 + i * 2, 13 - i, 2, 1, al((int) (200 * (1f - q)), 0xFFFFFF)); }
            }
            case "disc" -> {                                                                            // a record whose shine travels round the groove
                disc(g, x, y, 7.5f, 7.5f, 7f, 0xFF15161C); disc(g, x, y, 7.5f, 7.5f, 3f, 0xFFFF72C6); disc(g, x, y, 7.5f, 7.5f, 0.8f, 0xFF15161C);
                int hot = (int) (s * 6f) % 8; double an = hot * Math.PI / 4; disc(g, x, y, 7.5f + (float) Math.cos(an) * 5f, 7.5f + (float) Math.sin(an) * 5f, 1f, 0xFF6C7692);
            }
            case "megaphone" -> {                                                                       // an announcement going out
                r(g, x, y, 1, 6, 4, 4, 0xFFFFB300); for (int k = 0; k < 5; k++) r(g, x, y, 5 + k, 5 - k / 2, 1, 6 + k, 0xFFE8ECF4);
                r(g, x, y, 2, 10, 2, 4, 0xFF8A5A2C);
                for (int i = 0; i < 2; i++) { float ph = (s * 1.6f - i * 0.3f) % 1f; if (ph >= 0) r(g, x, y, 11 + i * 2, 5 - i, 1, 6 + i * 2, al((int) (255 * Math.sin(ph * Math.PI)), 0x56D8FF)); }
            }
            case "format" -> {                                                                          // { | } with a blinking cursor
                int[][] L = {{6, 2}, {5, 3}, {5, 4}, {5, 5}, {4, 6}, {3, 7}, {4, 8}, {5, 9}, {5, 10}, {5, 11}, {5, 12}, {6, 13}};
                for (int[] q : L) { r(g, x, y, q[0] - 1, q[1], 2, 1, 0xFF9AA6FF); r(g, x, y, q[0] - 1, q[1], 1, 1, 0xFFC9D0FF); r(g, x, y, 15 - q[0], q[1], 2, 1, 0xFF9AA6FF); r(g, x, y, 16 - q[0], q[1], 1, 1, 0xFFC9D0FF); }
                if ((int) (s * 2.4f) % 2 == 0) r(g, x, y, 8, 4, 1, 8, 0xFFFFB300);
            }
            case "plus" -> {                                                                            // a plus that glows
                int a = 150 + (int) (105 * Math.sin(s * 4f));
                r(g, x, y, 3, 7, 10, 3, al(a, 0x62E6A8)); r(g, x, y, 7, 3, 3, 10, al(a, 0x62E6A8));
            }
            case "glass" -> {                                                                           // a glass pane with a shine passing over
                r(g, x, y, 2, 1, 12, 14, 0x66B8D8F0); g.outline(x + 2, y + 1, 12, 14, 0xFFB8D8F0);
                float u = (s % 2f) / 2f; int gx = Math.round(u * 24f) - 6;
                g.enableScissor(x + 3, y + 2, x + 13, y + 14); for (int j = 0; j < 12; j++) r(g, x, y, gx - j / 2, 2 + j, 3, 1, 0x99FFFFFF); g.disableScissor();
            }
            case "sparkle" -> {                                                                         // a dropped item: a gem bobbing over its shadow on the ground, a light beam above it
                float bob = (float) Math.sin(s * 3f); int by = 5 + Math.round(bob * 1.2f);
                for (int yy = 0; yy < 12; yy++) { int a2 = 16 + Math.round(34 * (1f - yy / 12f)); r(g, x, y, 5, yy, 6, 1, al(a2, 0xFF72C6)); r(g, x, y, 7, yy, 2, 1, al(a2 + 30, 0xFFFFFF)); }
                int sw = 8 - Math.round(bob * 1.4f); r(g, x, y, 8 - sw / 2, 13, sw, 1, 0xCC05060A); r(g, x, y, 8 - sw / 2 + 1, 14, sw - 2, 1, 0x88000000);
                r(g, x, y, 6, by, 4, 1, 0xFF9BEAFF); r(g, x, y, 5, by + 1, 6, 1, 0xFF56D8FF); r(g, x, y, 4, by + 2, 8, 1, 0xFF56D8FF); r(g, x, y, 5, by + 3, 6, 1, 0xFF2FA0D8); r(g, x, y, 6, by + 4, 4, 1, 0xFF1C7FB8); r(g, x, y, 7, by + 5, 2, 1, 0xFF1C7FB8);
                r(g, x, y, 6, by + 1, 2, 1, 0xFFFFFFFF); r(g, x, y, 5, by + 2, 1, 1, 0xFFE8FAFF);
                if ((int) (s * 3f) % 2 == 0) spark(g, x, y, 12, 3, 1, 0xFFFFFFFF); else spark(g, x, y, 3, 6, 1, 0xFFFFE9A0);
            }
            case "preview" -> {                                                                         // an eye that glances left and right
                r(g, x, y, 1, 6, 14, 4, 0xFFE8EAF2); r(g, x, y, 3, 4, 10, 2, 0xFFE8EAF2); r(g, x, y, 3, 10, 10, 2, 0xFFE8EAF2); r(g, x, y, 5, 3, 6, 1, 0xFFB8BECC); r(g, x, y, 5, 12, 6, 1, 0xFFB8BECC);
                int dx = Math.round((float) Math.sin(s * 1.6f) * 2f); disc(g, x, y, 8f + dx, 8f, 3f, 0xFF56D8FF); disc(g, x, y, 8f + dx, 8f, 1.5f, 0xFF10141D); r(g, x, y, 7 + dx, 6, 1, 1, 0xFFFFFFFF);
            }
            case "bazaar" -> {                                                                          // a market stall, a coin glinting on the counter
                for (int i = 0; i < 7; i++) r(g, x, y, 1 + i * 2, 3, 2, 4, i % 2 == 0 ? 0xFFE5484D : 0xFFF4F4F8);
                r(g, x, y, 1, 7, 14, 1, 0xFFB8BECC); r(g, x, y, 2, 8, 2, 7, 0xFF8A5A2C); r(g, x, y, 12, 8, 2, 7, 0xFF8A5A2C); r(g, x, y, 2, 11, 12, 2, 0xFFA8733A);
                r(g, x, y, 7, 9, 2, 2, 0xFFFFC857); spark(g, x, y, 8, 9, 1 + (int) (s * 3f) % 2, 0xFFFFFFFF);
            }
            case "hammer" -> {                                                                          // a gavel striking its block
                float p = (s * 1.4f) % 1f; int dy = p < 0.35f ? Math.round((float) Math.sin(p / 0.35f * Math.PI) * 2f) : 0;
                r(g, x, y, 1, 13, 14, 2, 0xFF6A4220); r(g, x, y, 1, 13, 14, 1, 0xFFA8733A);
                ln(g, x, y, 8, 8 + dy, 12, 12, 0xFFA8733A); ln(g, x, y, 9, 8 + dy, 13, 12, 0xFF7A4E22);
                r(g, x, y, 2, 2 + dy, 9, 6, 0xFFD9B878); r(g, x, y, 2, 2 + dy, 9, 1, 0xFFF6E8BC); r(g, x, y, 2, 7 + dy, 9, 1, 0xFF9A7A3E);
                r(g, x, y, 4, 2 + dy, 1, 6, 0xFF8A8F9E); r(g, x, y, 8, 2 + dy, 1, 6, 0xFF8A8F9E);
                if (p >= 0.3f && p < 0.5f) spark(g, x, y, 5, 12, 2, al((int) (255 * (1f - (p - 0.3f) / 0.2f)), 0xFFFFFF));
            }
            case "refresh" -> {                                                                         // two arrows, their light chasing round
                int hot = (int) (s * 8f) % 12;
                for (int h = 0; h < 2; h++) for (int i = 0; i < 12; i++) { double an = h * Math.PI + i * 0.28; int d = Math.floorMod(hot - i, 12); r(g, x, y, Math.round(7.5f + (float) Math.cos(an) * 5.5f), Math.round(7.5f + (float) Math.sin(an) * 5.5f), 2, 2, al(255 - d * 18, h == 0 ? 0x62E6A8 : 0x56D8FF)); }
            }
            case "clock" -> {                                                                           // a clock, its ring lighting in turn
                disc(g, x, y, 7.5f, 7.5f, 7f, 0xFFE8ECF4); disc(g, x, y, 7.5f, 7.5f, 5.6f, 0xFF202432);
                ln(g, x, y, 7.5f, 7.5f, 7.5f, 2.5f, 0xFFFFC857); ln(g, x, y, 7.5f, 7.5f, 10.5f, 7.5f, 0xFFE8ECF4);
                int hot = (int) (s * 6f) % 12; double an = hot * Math.PI / 6; r(g, x, y, Math.round(7.5f + (float) Math.sin(an) * 6.2f) - 1, Math.round(7.5f - (float) Math.cos(an) * 6.2f) - 1, 2, 2, 0xFFFFC857);
            }
            case "speed" -> {                                                                           // a gauge whose lights chase round
                int hot = (int) (s * 8f) % 15;
                for (int i = 0; i <= 14; i++) { double a = Math.PI + i * Math.PI / 14; int base = i < 5 ? 0x62E6A8 : i < 10 ? 0xFFC857 : 0xE5484D; r(g, x, y, Math.round(7.5f + (float) Math.cos(a) * 7f), Math.round(11f + (float) Math.sin(a) * 7f), 2, 2, al(i == hot || i == (hot + 14) % 15 ? 255 : 130, base)); }
                ln(g, x, y, 7.5f, 11f, 7.5f + 3.9f, 11f - 3.9f, 0xFFFFFFFF); disc(g, x, y, 7.5f, 11f, 1.5f, 0xFFE8ECF4);
            }
            case "trash" -> {                                                                           // a bin, its slats lighting in turn
                r(g, x, y, 3, 6, 10, 9, 0xFF8E9BB3); for (int i = 0; i < 3; i++) r(g, x, y, 5 + i * 3, 8, 1, 5, i == (int) (s * 4f) % 3 ? 0xFFE8ECF4 : 0xFF5A6072);
                r(g, x, y, 2, 4, 12, 2, 0xFFC4CCDC); r(g, x, y, 6, 2, 4, 2, 0xFFC4CCDC);
            }
            case "tnt" -> {                                                                             // TNT with a burning fuse
                r(g, x, y, 2, 6, 12, 9, 0xFFD23A3A); r(g, x, y, 2, 9, 12, 3, 0xFFF4F4F8); r(g, x, y, 4, 10, 1, 1, 0xFF202432); r(g, x, y, 7, 10, 1, 1, 0xFF202432); r(g, x, y, 10, 10, 1, 1, 0xFF202432);
                r(g, x, y, 8, 3, 1, 3, 0xFF8A5A2C); int f = (int) (s * 14f) % 3; r(g, x, y, 8 + (f == 1 ? 1 : 0), 1, 2, 2, f == 0 ? 0xFFFFC857 : f == 1 ? 0xFFFF6A3C : 0xFFFFFFFF);
            }
            case "note" -> {                                                                            // a music note changing colour
                int ph = (int) (s * 2f) % 3; int c = ph == 0 ? 0xFF9AA6FF : ph == 1 ? 0xFFFF72C6 : 0xFF56D8FF;
                r(g, x, y, 5, 10, 4, 3, c); r(g, x, y, 8, 3, 1, 8, c); r(g, x, y, 8, 3, 4, 2, c);
            }
            case "dyebottle" -> {                                                                       // a dye bottle in the dye's own colour, a glint running down the glass
                int hi = Canvas.shade(tint, 1.35f), lo = Canvas.shade(tint, 0.62f);
                r(g, x, y, 6, 0, 4, 2, 0xFF8A5A2C); r(g, x, y, 6, 2, 4, 3, 0xFFD6E6F2);
                for (int j = 0; j < 10; j++) { int w = 8 + Math.min(4, j / 2 + 1), o = 8 - w / 2; r(g, x, y, o, 5 + j, w, 1, tint); r(g, x, y, o, 5 + j, 2, 1, hi); r(g, x, y, o + w - 2, 5 + j, 2, 1, lo); }
                r(g, x, y, 3, 14, 10, 1, lo); g.outline(x + 2, y + 5, 12, 10, 0x66000000);
                int gy = (int) ((s * 7f) % 12f); if (gy < 10) { r(g, x, y, 4, 5 + gy, 2, 1, 0xCCFFFFFF); }
            }
            case "boss_zombie" -> {                                                                     // a Revenant: green rotting face, eyes flickering
                r(g, x, y, 2, 2, 12, 12, 0xFF5E9E52); r(g, x, y, 2, 2, 12, 3, 0xFF3D6B36); r(g, x, y, 2, 11, 12, 3, 0xFF4B7F42);
                int a = 150 + (int) (105 * Math.sin(s * 5f)); r(g, x, y, 4, 6, 3, 3, 0xFF10200E); r(g, x, y, 9, 6, 3, 3, 0xFF10200E); r(g, x, y, 5, 7, 1, 1, al(a, 0xFF4040)); r(g, x, y, 10, 7, 1, 1, al(a, 0xFF4040));
                r(g, x, y, 5, 11, 6, 1, 0xFF2A4A26); r(g, x, y, 6, 12, 1, 1, 0xFFC9D6A8); r(g, x, y, 9, 12, 1, 1, 0xFFC9D6A8);
            }
            case "boss_spider" -> {                                                                     // a Tarantula: black body, four red eyes, legs
                for (int i = 0; i < 4; i++) { r(g, x, y, 0, 3 + i * 3, 3, 1, 0xFF2A2A33); r(g, x, y, 13, 3 + i * 3, 3, 1, 0xFF2A2A33); r(g, x, y, 2, 4 + i * 3, 1, 1, 0xFF2A2A33); r(g, x, y, 13, 4 + i * 3, 1, 1, 0xFF2A2A33); }
                disc(g, x, y, 7.5f, 8f, 5.2f, 0xFF1A1A22); disc(g, x, y, 7.5f, 8f, 3.6f, 0xFF262633);
                int a = 140 + (int) (115 * Math.sin(s * 4f)); for (int i = 0; i < 4; i++) r(g, x, y, 4 + i * 2, i % 2 == 0 ? 6 : 8, 1, 1, al(i % 2 == 0 ? a : 255, 0xFF2A2A));
            }
            case "boss_wolf" -> {                                                                       // Sven: a grey wolf head, ears up, eyes glinting
                r(g, x, y, 2, 1, 3, 4, 0xFF8A8F9A); r(g, x, y, 11, 1, 3, 4, 0xFF8A8F9A); r(g, x, y, 3, 3, 10, 9, 0xFFB8BCC6);
                r(g, x, y, 5, 8, 6, 6, 0xFFE4E6EC); r(g, x, y, 7, 11, 2, 2, 0xFF202026);
                int a = 160 + (int) (95 * Math.sin(s * 4f)); r(g, x, y, 4, 6, 2, 2, al(a, 0xFFD34A)); r(g, x, y, 10, 6, 2, 2, al(a, 0xFFD34A));
            }
            case "boss_enderman" -> {                                                                   // a Voidgloom: black head, glowing violet eyes, drifting motes
                r(g, x, y, 2, 2, 12, 12, 0xFF14101C); r(g, x, y, 2, 2, 12, 2, 0xFF1E1830);
                int a = 150 + (int) (105 * Math.sin(s * 4f)); r(g, x, y, 3, 8, 4, 2, al(a, 0xE67CFF)); r(g, x, y, 9, 8, 4, 2, al(a, 0xE67CFF)); r(g, x, y, 4, 8, 2, 1, 0xFFFFFFFF); r(g, x, y, 10, 8, 2, 1, 0xFFFFFFFF);
                for (int i = 0; i < 3; i++) if ((int) (s * 3f + i) % 3 == 0) r(g, x, y, 1 + i * 6, 3 + i * 4, 1, 1, 0xFFC27CFF);
            }
            case "boss_blaze" -> {                                                                      // an Inferno Demonlord: a blaze face, flames flickering on top
                r(g, x, y, 3, 4, 10, 10, 0xFFE8A81E); r(g, x, y, 3, 4, 10, 2, 0xFFFFD24A); r(g, x, y, 3, 12, 10, 2, 0xFFC07A10);
                r(g, x, y, 4, 7, 3, 2, 0xFF2A1A08); r(g, x, y, 9, 7, 3, 2, 0xFF2A1A08); r(g, x, y, 6, 11, 4, 1, 0xFF2A1A08);
                int f = (int) (s * 8f) % 3; for (int i = 0; i < 4; i++) r(g, x, y, 3 + i * 3, 1 + ((i + f) % 3 == 0 ? 0 : 1), 2, 3, (i + f) % 2 == 0 ? 0xFFFF6A1A : 0xFFFFA83C);
            }
            case "boss_vampire" -> {                                                                    // a Riftstalker: pale face, red eyes, fangs, dark collar
                r(g, x, y, 1, 11, 14, 4, 0xFF2A0F1E); r(g, x, y, 3, 2, 10, 10, 0xFFE6D8DC); r(g, x, y, 3, 1, 10, 3, 0xFF1A0F18); r(g, x, y, 3, 3, 2, 2, 0xFF1A0F18); r(g, x, y, 11, 3, 2, 2, 0xFF1A0F18);
                int a = 160 + (int) (95 * Math.sin(s * 5f)); r(g, x, y, 4, 7, 3, 2, al(a, 0xFF2040)); r(g, x, y, 9, 7, 3, 2, al(a, 0xFF2040));
                r(g, x, y, 6, 10, 4, 1, 0xFF7A1F36); r(g, x, y, 6, 11, 1, 2, 0xFFFFFFFF); r(g, x, y, 9, 11, 1, 2, 0xFFFFFFFF);
            }
            default -> { }
        }
    }

    private Icons() {}
}
