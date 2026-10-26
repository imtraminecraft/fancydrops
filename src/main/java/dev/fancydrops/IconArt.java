package dev.fancydrops;

/**
 * The pictures of every ":name:" icon, drawn in code as 16 x 16 pixel art with depth: a light from the top left (bright top and left
 * edges, darker bottom and right), a deep-coloured outline, and a few frames of animation. Pure code, no Minecraft classes, so the
 * whole set can be rendered anywhere. render() returns 256 ARGB pixels, or null when the name has no picture.
 */
final class IconArt {
    private IconArt() {}

    static final int FRAMES = 16;
    private static Canvas c;
    private static final float TAU = 6.2832f;

    // ------------------------------------------------------------------ the entry point
    /** The picture of one icon at one frame (0..FRAMES-1). tint (RGB, or 0) colours the icons that take a colour. */
    static int[] render(String name, int tint, int frame) {
        c = new Canvas(16, 16);
        float f = (frame % FRAMES) / (float) FRAMES;
        if (!paint(name, tint, f)) return null;
        return c.px.clone();
    }

    static boolean has(String name) { return render(name, 0, 0) != null; }

    // ------------------------------------------------------------------ drawing helpers
    private static int mix(int a, int b, float f) { return Canvas.mix(0xFF000000 | a, 0xFF000000 | b, f) & 0xFFFFFF; }
    private static int lite(int rgb, float f) { return mix(rgb, 0xFFFFFF, f); }
    private static int dark(int rgb, float f) { return mix(rgb, 0x000000, f); }
    private static void px(int x, int y, int rgb) { c.set(x, y, 0xFF000000 | rgb); }
    private static void pxa(int x, int y, int rgb, float a) { if (a > 0.02f) c.set(x, y, Canvas.rgb(Math.round(Math.min(1f, a) * 255), rgb)); }
    private static void rect(int x, int y, int w, int h, int rgb) { for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) px(x + i, y + j, rgb); }
    /** A block with a lit top and left edge and a shaded bottom and right edge. */
    private static void bev(int x, int y, int w, int h, int rgb) {
        rect(x, y, w, h, rgb);
        for (int i = 0; i < w; i++) { px(x + i, y, lite(rgb, 0.42f)); px(x + i, y + h - 1, dark(rgb, 0.38f)); }
        for (int j = 1; j < h - 1; j++) { px(x, y + j, lite(rgb, 0.22f)); px(x + w - 1, y + j, dark(rgb, 0.28f)); }
    }
    private static void ln(float x0, float y0, float x1, float y1, int rgb) {
        int n = (int) Math.max(1, Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
        for (int i = 0; i <= n; i++) px(Math.round(x0 + (x1 - x0) * i / n), Math.round(y0 + (y1 - y0) * i / n), rgb);
    }
    /** A ball lit from the top left: bright on that side, dark on the other, with a white glint. */
    private static void ball(float cx, float cy, float r, int rgb) {
        for (int y = (int) Math.floor(cy - r); y <= Math.ceil(cy + r); y++) for (int x = (int) Math.floor(cx - r); x <= Math.ceil(cx + r); x++) {
            float dx = (x + 0.5f - cx) / r, dy = (y + 0.5f - cy) / r, d2 = dx * dx + dy * dy;
            if (d2 > 1f) continue;
            float nz = (float) Math.sqrt(1f - d2), l = Math.max(0f, Math.min(1f, 0.55f + 0.55f * (-dx * 0.55f - dy * 0.7f + nz * 0.45f) - 0.25f));
            px(x, y, l > 0.55f ? lite(rgb, (l - 0.55f) * 1.1f) : dark(rgb, (0.55f - l) * 0.9f));
        }
        px(Math.round(cx - r * 0.38f), Math.round(cy - r * 0.42f), 0xFFFFFF);
    }
    private static boolean inside(float[] xs, float[] ys, float x, float y) {
        boolean in = false;
        for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) if ((ys[i] > y) != (ys[j] > y) && x < (xs[j] - xs[i]) * (y - ys[i]) / (ys[j] - ys[i]) + xs[i]) in = !in;
        return in;
    }
    /** A filled shape, lit from the top left (the colour runs from light to dark across it). */
    private static void poly(float[] xs, float[] ys, int rgb) {
        float x0 = 99, x1 = -99, y0 = 99, y1 = -99;
        for (int i = 0; i < xs.length; i++) { x0 = Math.min(x0, xs[i]); x1 = Math.max(x1, xs[i]); y0 = Math.min(y0, ys[i]); y1 = Math.max(y1, ys[i]); }
        for (int y = Math.max(0, (int) y0); y <= Math.min(15, (int) Math.ceil(y1)); y++) for (int x = Math.max(0, (int) x0); x <= Math.min(15, (int) Math.ceil(x1)); x++) {
            if (!inside(xs, ys, x + 0.5f, y + 0.5f)) continue;
            float t = ((x + 0.5f - x0) / Math.max(1f, x1 - x0) * 0.45f + (y + 0.5f - y0) / Math.max(1f, y1 - y0) * 0.55f);
            px(x, y, t < 0.3f ? lite(rgb, 0.4f * (1f - t / 0.3f)) : dark(rgb, (t - 0.3f) / 0.7f * 0.5f));
        }
    }
    private static void tri(float ax, float ay, float bx, float by, float cx, float cy, int rgb) { poly(new float[]{ax, bx, cx}, new float[]{ay, by, cy}, rgb); }
    private static void quad(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int rgb) { poly(new float[]{ax, bx, cx, dx}, new float[]{ay, by, cy, dy}, rgb); }
    private static void outline() { c.softOutline(0.34f); }
    private static void glint(int x, int y, int r, int rgb, float a) { c.glint(x, y, r, rgb, Math.max(0f, Math.min(1f, a))); }
    private static void glow(float x, float y, float r, int rgb, float s) { c.glow(x, y, r, rgb, s); }
    private static float tri(float f) { return 1f - Math.abs(2f * f - 1f); }                  // 0 .. 1 .. 0 over the loop
    private static float sn(float f) { return (float) Math.sin(f * TAU); }
    private static int hue(float h, float s, float v) { return java.awt.Color.HSBtoRGB(((h % 1f) + 1f) % 1f, s, v) & 0xFFFFFF; }

    /** 3 x 5 letters for the text-effect tiles and a few labels. */
    private static final String[] GLYPH_A = {".#.", "#.#", "###", "#.#", "#.#"};
    private static void letter(String[] g, int x, int y, int rgb, boolean shade) {
        for (int j = 0; j < g.length; j++) for (int i = 0; i < g[j].length(); i++) if (g[j].charAt(i) == '#') px(x + i, y + j, shade ? (j < 2 ? lite(rgb, 0.3f) : j > 3 ? dark(rgb, 0.3f) : rgb) : rgb);
    }

    // ------------------------------------------------------------------ the icons
    private static boolean paint(String n, int tint, float f) {
        float s = sn(f), t = tri(f);
        if (n.startsWith("fx") && n.length() <= 4 && Character.isDigit(n.charAt(n.length() - 1))) return fx(Integer.parseInt(n.substring(2)), f);
        switch (n) {
            case "home" -> {                                                                 // a cosy house, smoke rising from the chimney
                bev(10, 3, 2, 5, 0x8A4B3A);
                bev(3, 8, 10, 6, 0xF2E3C0);
                tri(1, 8.6f, 8, 2, 15, 8.6f, 0xD4503A);
                rect(2, 8, 12, 1, 0x8F2F22);
                bev(7, 10, 3, 4, 0x7A4A24); px(9, 12, 0xFFD25A);
                int wc = mix(0x7FC8FF, 0xFFE08A, t);
                bev(4, 10, 2, 2, wc); bev(11, 10, 2, 2, wc);
                outline();
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; pxa(11 + Math.round((float) Math.sin(u * 6 + i)), Math.round(3 - u * 3), 0xE8EEF8, (1f - u) * 0.9f); }
                glow(8, 11, 5, 0xFFD890, 0.12f + 0.08f * t);
            }
            case "dropped" -> {                                                              // a bookshelf: a book slides out and back
                int slide = Math.round(2.4f * t);
                bev(1, 1, 14, 14, 0x7A4A24);
                rect(2, 2, 12, 12, 0x3A2410);
                rect(1, 7, 14, 1, 0x9A622E); rect(1, 14, 14, 1, 0x9A622E);
                int[] cols = {0xE0484E, 0x4A8AE0, 0xF0C040, 0x58B868, 0xB56BFF, 0xE08A3A, 0x55D8FF};
                int[] ws = {2, 1, 2, 2, 1, 2, 2};
                int x = 2; for (int i = 0; i < 6 && x < 13; i++) { int w = ws[i], h = 4 - (i % 3 == 1 ? 1 : 0); int yy = 7 - h; if (i == 2) { rect(x, yy + 1 - slide, w, h, cols[i]); px(x, yy + 1 - slide, lite(cols[i], 0.5f)); } else { rect(x, yy, w, h, cols[i]); px(x, yy, lite(cols[i], 0.5f)); } x += w; }
                x = 2; for (int i = 0; i < 6 && x < 13; i++) { int w = ws[(i + 3) % 7], h = 4 - (i % 2); rect(x, 14 - h, w, h, cols[(i + 3) % 7]); px(x, 14 - h, lite(cols[(i + 3) % 7], 0.5f)); x += w; }
                outline();
            }
            case "animations" -> {                                                           // a film clapperboard: the striped clapper swings shut on its hinge
                float th = f < 0.4f ? 0.55f * (float) Math.sin(f / 0.4f * 1.2f) / 0.93f : f < 0.55f ? 0.55f * (1f - (f - 0.4f) / 0.15f) : 0f;      // opens, then snaps shut
                bev(1, 8, 14, 7, 0x1B1F2C);                                                          // the slate
                rect(2, 9, 12, 1, 0xF4F4F8); rect(2, 11, 5, 1, 0xCBD2E4); rect(8, 11, 6, 1, 0xCBD2E4); rect(2, 13, 8, 1, 0xCBD2E4); rect(11, 13, 3, 1, 0xCBD2E4);
                float cs = (float) Math.cos(th), sn2 = (float) Math.sin(th);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {                          // the clapper: a bar turned about the hinge at (1.5, 8)
                    float dx = x + 0.5f - 1.5f, dy = y + 0.5f - 8f, u = dx * cs - dy * sn2, v = dx * sn2 + dy * cs;
                    if (u < -0.5f || u > 13f || v < -3.6f || v > -0.2f) continue;
                    boolean light = (int) Math.floor((u + (v + 3.6f) * 0.9f) / 2.1f) % 2 == 0;
                    px(x, y, light ? (v < -2.6f ? 0xFFFFFF : 0xE4E8F2) : (v > -1f ? 0x0E1018 : 0x1B1F2C));
                }
                rect(1, 8, 14, 1, 0x0E1018);
                outline();
                if (f >= 0.55f && f < 0.8f) { glint(14, 5, 2, 0xFFFFFF, 1f - (f - 0.55f) * 3f); px(15, 7, 0xFFE27A); px(0, 6, 0xFFE27A); }
            }
            case "sounds" -> {                                                               // a speaker with waves going out
                bev(2, 6, 3, 4, 0xB8C2DA);
                quad(5, 6, 9, 3, 9, 13, 5, 10, 0x8FA0C8);
                outline();
                for (int i = 0; i < 3; i++) {
                    float u = (f * 3f - i * 0.7f) % 3f; float a = u < 1f ? u : u < 2f ? 1f : 3f - u; float r = 3 + i * 2.2f;
                    for (int k = -3; k <= 3; k++) { float ang = k * 0.3f; pxa(Math.round(9.5f + (float) Math.cos(ang) * r * 0.9f), Math.round(8 + (float) Math.sin(ang) * r), hue(0.52f + i * 0.04f, 0.5f, 1f), Math.min(1f, a * 1.3f)); }
                }
            }
            case "chat" -> {                                                                 // a speech bubble with three bouncing dots
                bev(1, 2, 14, 9, 0xE8F0FF);
                tri(4, 11, 4, 14.5f, 8, 11, 0xBFD0F0);
                rect(4, 10, 4, 1, 0xBFD0F0);
                outline();
                for (int i = 0; i < 3; i++) { int dy = Math.round(Math.max(0f, (float) Math.sin(f * TAU - i * 1.1f)) * 1.8f); rect(4 + i * 3, 6 - dy, 2, 2, mix(0x5A74D8, 0x9AB0FF, i / 2f)); }
            }
            case "settings" -> {                                                             // a gear, turning one tooth every loop
                float rot = f * TAU / 8f;
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                    float dx = x + 0.5f - 8, dy = y + 0.5f - 8, r = (float) Math.hypot(dx, dy), th = (float) Math.atan2(dy, dx) + rot;
                    float lim = Math.cos(8 * th) > 0.15f ? 7.4f : 5.7f;
                    if (r > lim || r < 2.4f) continue;
                    float l = (-dx * 0.5f - dy * 0.8f) / 8f;
                    px(x, y, l > 0.2f ? lite(0xA9B4CC, (l - 0.2f) * 1.2f) : dark(0xA9B4CC, (0.2f - l) * 0.9f + 0.08f));
                }
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { float r = (float) Math.hypot(x + 0.5f - 8, y + 0.5f - 8); if (r < 2.4f) px(x, y, r < 1.6f ? 0x1B2236 : 0x59637F); }
                outline();
                glint(5, 4, 1, 0xFFFFFF, 0.5f + 0.5f * t);
            }
            case "stats" -> {                                                                // bars that grow, with a rising arrow
                int h1 = Math.round(4 + 2 * t), h2 = Math.round(7 + 2 * (1f - t)), h3 = Math.round(9 + 2 * t);
                bev(2, 14 - h1, 3, h1, 0x62E6A8); bev(6, 14 - h2, 3, h2, 0xFFC857); bev(10, 14 - h3, 3, h3, 0x55D8FF);
                rect(1, 14, 14, 1, 0x7F8AB0);
                outline();
                glint(11, 14 - h3 - 2, 2, 0xFFFFFF, 0.4f + 0.6f * t);
            }
            case "mytho" -> {                                                                // the minotaur: a horned head with glaring eyes
                tri(2, 7, 1, 2, 5.5f, 5.2f, 0xF4EBD2); tri(14, 7, 15, 2, 10.5f, 5.2f, 0xF4EBD2);
                quad(4, 5, 12, 5, 11.5f, 12, 4.5f, 12, 0x7A4A2B);
                quad(5.5f, 9, 10.5f, 9, 10, 14, 6, 14, 0xA8714A);
                rect(6, 12, 1, 1, 0x2A1A10); rect(9, 12, 1, 1, 0x2A1A10);
                outline();
                float e = 0.5f + 0.5f * t; int ec = mix(0xFF7A3A, 0xFFE9A0, e);
                rect(5, 8, 2, 1, ec); rect(9, 8, 2, 1, ec);
                glow(6, 8, 3, 0xFF5A2A, 0.35f * e); glow(10, 8, 3, 0xFF5A2A, 0.35f * e);
                if (f > 0.6f && f < 0.85f) { px(8, 1, 0xFFFFFF); px(7, 2, 0x9FEFFF); px(8, 3, 0xFFFFFF); }
            }
            case "treasure" -> {                                                             // a spade digging into the earth: dirt flies, then a gold glint
                float dig = f < 0.4f ? f / 0.4f : f < 0.6f ? 1f : 1f - (f - 0.6f) / 0.4f;           // 0 high .. 1 in the ground
                float sy = -5f * (1f - dig), sx = 1.5f * (1f - dig);
                fillMound();
                ln(12 + sx, 0 + sy, 8 + sx, 8 + sy, 0x8A5A2C); ln(13 + sx, 0 + sy, 9 + sx, 8 + sy, 0x6B4423);
                rect(11 + Math.round(sx), Math.max(0, Math.round(sy)), 3, 1, 0x3A2410);                // the T handle
                poly(new float[]{6.2f + sx, 10.6f + sx, 10.4f + sx, 8.6f + sx, 7.2f + sx}, new float[]{8 + sy, 9 + sy, 11.5f + sy, 14.8f + sy, 11.8f + sy}, 0xA8B2C8);
                quad(6.6f + sx, 8.2f + sy, 8.2f + sx, 8.5f + sy, 7.8f + sx, 12 + sy, 7f + sx, 11.4f + sy, 0xE8EEF8);
                rect(Math.round(6 + sx), Math.round(8 + sy), 5, 1, 0x6B7488);
                outline();
                if (f > 0.55f) { float u = (f - 0.55f) / 0.45f; for (int i = 0; i < 4; i++) pxa(Math.round(5 + i * 2.5f + u * (i - 1.5f) * 2f), Math.round(11 - u * (5 + (i % 2) * 3) + u * u * 6), i % 2 == 0 ? 0x8A5A2C : 0xB0793A, 1f - u * 0.5f); }
                if (f > 0.7f) { px(3, 12, 0xFFD25A); px(4, 12, 0xFFF3B0); glint(4, 11, 1, 0xFFF3B0, (f - 0.7f) / 0.3f); }
            }
            case "slayers" -> {                                                              // a thin blade, a blood-red gem, a red glow
                glow(8, 8, 8, 0xFF3A3A, 0.18f + 0.1f * t);
                ln(13.5f, 1, 5, 10, 0xE8EEF8); ln(14, 1.5f, 5.5f, 10.5f, 0x9AA4C0);                    // a slim blade, one pixel wide with a shaded edge
                px(14, 1, 0xFFFFFF); px(13, 1, 0xFFFFFF);
                ln(3.5f, 8, 8, 12.5f, 0xD8A02A); ln(3.5f, 9, 7.5f, 13, 0x8A5A12);                    // a narrow guard
                ln(4.5f, 11.5f, 2, 14, 0x5A1A22); ln(5, 12, 2.5f, 14.5f, 0x7A2A32);                  // the grip
                px(1, 15, 0xFF3A3A); px(2, 15, 0xC82A2A);                                              // a red pommel gem
                outline();
                glint(13 - Math.round(f * 7), 2 + Math.round(f * 7), 1, 0xFFFFFF, 0.35f + 0.65f * t);
            }
            case "shovel" -> {                                                               // a digging shovel (the blade takes the spade's colour): it dips into the ground and flicks up a lump of dirt
                int bl = tint != 0 ? tint : 0xC9D1E0;
                float dip = t * 2f;
                for (int x = 1; x < 15; x++) px(x, 14, 0x5A3E22);
                for (int x = 2; x < 14; x += 3) px(x, 13, 0x7A5A38);
                ln(12, 1 + dip, 6.5f, 9 + dip, 0x6B4423); ln(13, 1 + dip, 7.5f, 9 + dip, 0x9A6A38);
                rect(11, 1 + (int) dip, 4, 1, 0x3A2410); px(11, 2 + (int) dip, 0x3A2410); px(14, 2 + (int) dip, 0x3A2410);                       // the D grip
                quad(6, 9 + dip, 8.5f, 9.8f + dip, 6.8f, 14, 3.2f, 13, bl);
                ln(6, 9 + dip, 3.3f, 13, lite(bl, 0.5f)); px((int) 6, (int) (14), dark(bl, 0.4f));
                ln(5.5f, 9.5f + dip, 8, 10 + dip, dark(bl, 0.35f));
                float u = f < 0.5f ? 0f : (f - 0.5f) * 2f;
                if (u > 0f) { px(Math.round(8 + u * 4), Math.round(12 - 5 * (float) Math.sin(u * 3.14f)), 0x9A7A52); px(Math.round(9 + u * 5), Math.round(12 - 4 * (float) Math.sin(u * 3.14f)), 0x6B4A2B); }
                outline();
            }
            case "sword" -> {                                                                // the iron sword: grey blade, dark guard, wooden grip
                int[][] bl = {{14, 1}, {13, 2}, {12, 3}, {11, 4}, {10, 5}, {9, 6}, {8, 7}, {7, 8}, {6, 9}};
                for (int i = 0; i < bl.length; i++) {
                    int x = bl[i][0], y = bl[i][1];
                    px(x, y, 0xD8DDE6); px(x - 1, y, 0xB4BBC8); px(x + 1, y, 0xF4F6FA); px(x, y - 1, i == 0 ? 0xFFFFFF : 0xE8ECF2);
                    px(x - 1, y + 1, 0x8A93A6);
                }
                ln(3.5f, 8, 8.5f, 13, 0x6B7488); ln(3.5f, 9, 7.5f, 13, 0x4A5164);                      // the cross guard
                px(4, 7, 0x8A93A6); px(9, 12, 0x8A93A6); px(8, 13, 0x4A5164);
                ln(5, 10, 2, 13, 0x8A5A2C); ln(6, 11, 3, 14, 0x6B4423);                                 // the wooden grip
                px(1, 14, 0x5A3418); px(2, 14, 0x5A3418); px(1, 15, 0x3A2410);
                outline();
                glint(12 - Math.round(f * 6), 3 + Math.round(f * 6), 1, 0xFFFFFF, 0.3f + 0.7f * t);
            }
            case "farm" -> {                                                                 // a farmer in a straw hat, hoeing the rows
                float hoe = f < 0.5f ? f / 0.5f : 1f - (f - 0.5f) / 0.5f;                              // the swing: up then down
                rect(0, 13, 16, 3, 0x6B4423); rect(0, 13, 16, 1, 0x8A5A2C);                              // tilled soil
                for (int i = 0; i < 4; i++) { rect(1 + i * 4, 14, 2, 1, 0x4A2E16); }
                px(13, 12, 0x7BE05A); px(13, 11, 0x5FBF40); px(12, 11, 0x7BE05A); px(14, 11, 0x7BE05A);    // a young crop
                rect(4, 3, 5, 1, 0xF0D060); rect(3, 4, 7, 1, 0xD4B040); px(2, 4, 0xB89430); px(10, 4, 0xB89430);       // straw hat
                rect(5, 5, 3, 2, 0xF2C9A0); px(6, 5, 0x3A2410); px(7, 5, 0x3A2410);                       // face
                rect(4, 7, 5, 4, 0x4FA0E0); rect(4, 7, 5, 1, 0x7CC0F8); rect(5, 7, 1, 4, 0x2E6FB0);       // shirt with overalls
                rect(4, 11, 2, 2, 0x3A4A8A); rect(7, 11, 2, 2, 0x3A4A8A); rect(4, 13, 2, 1, 0x3A2410); rect(7, 13, 2, 1, 0x3A2410);
                rect(3, 8, 1, 2, 0xF2C9A0); rect(9, 8, 1, 2, 0xF2C9A0);                                   // arms
                float hx = 9 + 3 * hoe, hy = 3 + 6 * hoe;                                                   // the hoe: handle then a blade
                ln(9.5f, 8.5f, hx + 2.5f, hy - 1.5f, 0x8A5A2C); ln(10, 8.5f, hx + 3, hy - 1, 0x6B4423);
                rect(Math.round(hx + 2), Math.round(hy - 2), 3, 1, 0xB0BAD0); px(Math.round(hx + 4), Math.round(hy - 1), 0x8A93A6);
                outline();
                if (hoe > 0.85f) { px(12, 12, 0xB0793A); px(14, 13, 0x8A5A2C); }
            }
            case "experiments" -> {                                                          // the enchanting table's book, a page flipping, runes rising
                float u = f;                                                                           // the flip: a page swings from right to left
                float fl = (float) Math.cos(u * 3.1416f);                                              // 1 .. -1
                bev(1, 10, 14, 4, 0x8A1F2E);                                                            // cover
                quad(2, 6, 8, 7, 8, 11, 2, 10, 0xF2E8C8);                                               // left page
                quad(8, 7, 14, 6, 14, 10, 8, 11, 0xFFF6DC);                                             // right page
                ln(3, 8, 7, 8.8f, 0xB8A878); ln(9, 8.8f, 13, 8, 0xB8A878);
                float px0 = 8 + fl * 6;                                                                 // the turning page's edge
                quad(8, 7, px0, 6 - Math.abs(fl) * 2.2f, px0, 10 - Math.abs(fl) * 2.2f, 8, 11, fl > 0 ? 0xFFFFF0 : 0xE8DCB4);
                rect(7, 6, 2, 6, 0x5A1018);                                                             // spine
                outline();
                for (int i = 0; i < 3; i++) { float v = (f + i / 3f) % 1f; int rx = 4 + i * 4 + Math.round((float) Math.sin(v * 6 + i) * 1.5f), ry = Math.round(5 - v * 5);
                    pxa(rx, ry, 0xC9A0FF, 1f - v * 0.6f); pxa(rx + 1, ry, 0xFFFFFF, (1f - v) * 0.7f); }
            }
            case "mining" -> {                                                               // a pickaxe swings onto a rock and cracks it
                float sw = f < 0.55f ? f / 0.55f : 1f;                                                  // 0 raised .. 1 hits
                float rec = f < 0.55f ? 0f : (f - 0.55f) / 0.45f;
                float ang = (1f - sw) * 0.9f;                                                           // the handle's lean
                float hx = 9 - (1f - sw) * 5, hy = 5 - (1f - sw) * 4;                                    // where the head is
                poly(new float[]{1, 6, 14, 15, 13, 7, 3}, new float[]{15, 8.5f, 8.5f, 12, 15, 15, 15}, 0x7A8398);      // the rock
                poly(new float[]{3, 7, 11, 13, 8}, new float[]{13, 9.5f, 9.5f, 12, 11.5f}, 0x9AA3B8);
                if (rec > 0f) { ln(9, 9, 8 + Math.round(rec), 11, 0x2A2F3C); ln(8, 11, 6, 13, 0x2A2F3C); if (rec > 0.4f) { ln(9, 10, 11, 12, 0x2A2F3C); ln(6, 13, 4, 14, 0x2A2F3C); } }
                ln(3.5f, 14, hx, hy, 0x8A5A2C); ln(4.5f, 14, hx + 1, hy, 0x6B4423);                      // the handle
                quad(hx - 4, hy - 1, hx, hy - 3, hx + 5, hy + 1, hx + 1, hy + 0.5f, 0xC8D0E2);          // the curved pick
                tri(hx - 4, hy - 1, hx - 5, hy + 1, hx - 3, hy, 0xE8EEF8); tri(hx + 5, hy + 1, hx + 6, hy + 3, hx + 4, hy + 1.5f, 0x9AA3B8);
                outline();
                if (sw >= 1f) { float v = Math.min(1f, rec * 1.6f); px(9 + Math.round(v * 4), 8 - Math.round(v * 4), 0xFFE27A); px(9 - Math.round(v * 4), 8 - Math.round(v * 3), 0xFFB03A); px(9, 7 - Math.round(v * 3), 0xFFFFFF); }
            }
            case "fishing" -> {                                                              // a fish leaping
                float jy = -3.5f * 4f * f * (1f - f) * 1f;
                float yy = 8 + jy;
                tri(11, yy, 15, yy - 3 + s, 15, yy + 3 - s, 0x2E8FD8);
                poly(new float[]{1.5f, 5, 9, 12, 9, 5}, new float[]{yy, yy - 3.4f, yy - 3, yy, yy + 3, yy + 3.4f}, 0x4FC3FF);
                tri(6, yy - 3.2f, 8, yy - 5, 9, yy - 3, 0x2E8FD8);
                rect(3, Math.round(yy) - 1, 1, 1, 0x10202E); rect(4, Math.round(yy) + 1, 4, 1, 0xBDEBFF);
                outline();
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; pxa(2 + i * 4, 14 - Math.round(u * 3), 0xBDEBFF, (1f - u) * 0.9f * (f < 0.2f || f > 0.8f ? 1f : 0.3f)); }
            }
            case "dye" -> {                                                                  // a paint drop falls, splashes, and spreads into a puddle
                int col = hue(0.9f - 0.9f * f, 0.7f, 1f);
                float fall = Math.min(1f, f / 0.4f), splash = f < 0.4f ? 0f : (f - 0.4f) / 0.6f;
                rect(0, 14, 16, 2, 0x4E4034); rect(0, 14, 16, 1, 0x6E5A46);                              // the ground
                if (splash <= 0f) {                                                                    // the drop falls
                    float dy = 0.5f + fall * 7;
                    poly(new float[]{8, 10.4f, 11, 8, 5, 5.6f}, new float[]{dy, dy + 3.4f, dy + 6, dy + 7, dy + 6, dy + 3.4f}, col);
                    px(7, Math.round(dy + 4f), 0xFFFFFF); px(7, Math.round(dy + 5f), lite(col, 0.6f));
                } else {                                                                               // the puddle spreads, droplets fly up
                    float rx = 2.5f + splash * 4.5f, ry = 1.2f + splash * 1.3f;
                    for (int y = 11; y < 16; y++) for (int x = 0; x < 16; x++) { float dx = (x + 0.5f - 8) / rx, dy2 = (y + 0.5f - 13.6f) / ry; if (dx * dx + dy2 * dy2 <= 1f) px(x, y, dy2 < -0.4f ? lite(col, 0.45f) : dy2 > 0.45f ? dark(col, 0.3f) : col); }
                    float h = (float) Math.sin(Math.min(1f, splash * 1.3f) * 3.14f) * 6f;
                    for (int i = 0; i < 5; i++) { float dir = (i - 2) / 2f; int fx = Math.round(8 + dir * (2 + splash * 5.5f)), fy = Math.round(12 - h * (1f - Math.abs(dir) * 0.45f)); rect(fx, fy, i % 2 == 0 ? 2 : 1, i % 2 == 0 ? 2 : 1, i % 2 == 0 ? col : lite(col, 0.45f)); }
                    if (splash < 0.45f) { rect(7, 9, 2, 3, col); px(7, 9, lite(col, 0.5f)); }
                }
                outline();
            }
            case "nodrops" -> {                                                              // an empty box, a puff of dust
                bev(2, 8, 12, 6, 0x6A7388);
                rect(3, 9, 10, 2, 0x1B2030);
                quad(2, 8, 4, 5, 12, 5, 14, 8, 0x8A93A8);
                outline();
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; pxa(5 + i * 3, Math.round(7 - u * 5), 0xB8C0D4, (1f - u) * 0.9f); }
            }
            case "normal" -> {                                                               // a loot bag, tied at the neck, a coin peeking out
                poly(new float[]{4.5f, 11.5f, 14, 14.5f, 1.5f, 2}, new float[]{5, 5, 11, 14, 14, 11}, 0xB5773A);
                poly(new float[]{4.5f, 8, 3, 1.8f}, new float[]{5, 5, 12.5f, 11}, lite(0xB5773A, 0.35f));
                quad(5, 2.2f, 11, 2.2f, 11.5f, 5.5f, 4.5f, 5.5f, 0xC98A48);                              // the gathered neck
                rect(4, 5, 8, 1, 0xE8D08A); px(3, 6, 0xE8D08A); px(12, 6, 0xE8D08A); px(2, 7, 0xB89A4A); px(13, 7, 0xB89A4A);   // the rope
                float cy2 = 1 - 0.8f * t;
                ball(8, 2.5f + cy2 * 0.4f, 2.2f, 0xFFD25A); px(8, Math.round(2 + cy2 * 0.4f), 0xFFF3B0);
                outline();
                glint(11 - Math.round(f * 4), 8 + Math.round((float) Math.sin(f * 6.28f) * 1.5f), 1, 0xFFE9A0, 0.3f + 0.7f * t);
            }
            case "dice" -> dice(f);
            case "emblem", "emblem_special" -> {                                             // a set badge: a heraldic shield (13 wide, centred on column 8) with a star, in the set's colour
                boolean sp = n.equals("emblem_special");
                int col = tint != 0 ? tint : sp ? 0xB48CFF : 0x55D8FF;
                int[][] span = {{2, 14}, {2, 14}, {2, 14}, {2, 14}, {2, 14}, {2, 14}, {2, 14}, {3, 13}, {3, 13}, {4, 12}, {5, 11}, {6, 10}, {7, 9}, {8, 8}};       // columns of each row, y = 1 .. 14
                for (int r = 0; r < span.length; r++) for (int x = span[r][0]; x <= span[r][1]; x++) {
                    boolean edge = r == 0 || x == span[r][0] || x == span[r][1] || r == span.length - 1 || (r + 1 < span.length && (x < span[r + 1][0] || x > span[r + 1][1]));
                    boolean hi = !edge && (r == 1 || x == span[r][0] + 1) && r < 10;
                    px(x, 1 + r, edge ? dark(col, 0.55f) : hi ? lite(col, 0.45f) : mix(dark(col, 0.45f), col, r < 7 ? 0.25f : 0.1f));
                }
                for (int x = 3; x <= 13; x++) px(x, 2, lite(col, 0.55f));
                if (!sp) {
                    String[] star = {"...#...", "...#...", "#######", ".#####.", "..###..", ".##.##.", ".#...#."};       // a 5 point star, 7 x 7, its middle on column 8
                    float k = 0.55f + 0.45f * t;
                    for (int y = 0; y < 7; y++) for (int x = 0; x < 7; x++) if (star[y].charAt(x) == '#') px(5 + x, 4 + y, mix(0xFFE9A0, 0xFFFFFF, k));
                    glint(5, 4, 1, 0xFFFFFF, 0.3f + 0.7f * t);
                } else {                                                                      // Special: a four-point sparkle with two little twinkles
                    float k = 0.5f + 0.5f * t;
                    for (int d = -4; d <= 4; d++) { int w = 4 - Math.abs(d); px(8 + d, 7, mix(0xFFF3C8, 0xFFFFFF, k)); px(8, 7 + d, mix(0xFFF3C8, 0xFFFFFF, k)); }
                    for (int d = -1; d <= 1; d++) { px(8 + d, 6, 0xFFFFFF); px(8 + d, 8, 0xFFFFFF); px(7, 7 + d, 0xFFFFFF); px(9, 7 + d, 0xFFFFFF); }
                    px(8, 7, 0xFFFFFF);
                    int tw = (int) (f * 8) % 4; px(5, 4, tw == 0 ? 0xFFFFFF : lite(col, 0.4f)); px(11, 10, tw == 2 ? 0xFFFFFF : lite(col, 0.4f)); px(11, 4, tw == 1 ? 0xFFFFFF : lite(col, 0.3f)); px(5, 10, tw == 3 ? 0xFFFFFF : lite(col, 0.3f));
                    glow(8, 7, 7, 0xB48CFF, 0.18f * t);
                }
            }
            case "snd_beat" -> {                                                             // Beat: a drum being hit, rings leaving it
                bev(2, 8, 12, 6, 0xB5532E); rect(2, 8, 12, 2, 0xF0D8A8); rect(3, 10, 1, 4, 0xE8C080); rect(8, 10, 1, 4, 0xE8C080); rect(12, 10, 1, 4, 0xE8C080);
                float hit = f < 0.3f ? f / 0.3f : 0f;
                ln(3, 3 + 4 * hit, 7, 7 + hit, 0xE8D8B0); ln(13, 3 + 4 * (1f - hit), 9, 7, 0xE8D8B0);
                outline();
                if (f > 0.25f) { float u = (f - 0.25f) / 0.75f; pxa(8 - Math.round(2 + u * 5), 7, 0xFFE27A, 1f - u); pxa(8 + Math.round(2 + u * 5), 7, 0xFFE27A, 1f - u); pxa(8, 6 - Math.round(u * 3), 0xFFF3B0, 1f - u); }
            }
            case "snd_burst" -> {                                                            // Burst: a starburst
                glow(8, 8, 8, 0xFF9A3A, 0.25f + 0.15f * t);
                float r = 3f + 3.5f * t;
                for (int i = 0; i < 8; i++) { float a = i * 0.7854f + f * 0.4f; float r2 = i % 2 == 0 ? r + 2 : r; ln(8, 8, 8 + (float) Math.cos(a) * r2, 8 + (float) Math.sin(a) * r2, i % 2 == 0 ? 0xFFE27A : 0xFFB03A); }
                ball(8, 8, 2.6f, 0xFFF3B0);
                outline();
            }
            case "snd_reveal" -> {                                                           // Reveal: a curtain opening on a bright stage
                rect(1, 2, 14, 12, 0xFFF3B0); glow(8, 8, 6, 0xFFFFFF, 0.4f);
                float o = 2f + 3.5f * t;
                quad(1, 2, 1 + (8 - o), 2, 1 + (8 - o) - 1, 14, 1, 14, 0xC8263A); quad(15, 2, 15 - (8 - o), 2, 15 - (8 - o) + 1, 14, 15, 14, 0xC8263A);
                rect(1, 1, 14, 2, 0x8A1F2E); rect(2, 2, 12, 1, 0xE0484E);
                ball(8, 9, 1.8f, 0xFFD25A);
                outline();
            }
            case "snd_linger" -> {                                                           // Linger: a note whose echoes fade away
                ball(5, 12, 2.4f, 0x9AB8FF); ln(7, 12, 7, 3, 0x9AB8FF); ln(7, 3, 11, 5, 0x9AB8FF); ln(7, 4, 11, 6, 0x6A88CF);
                for (int i = 1; i <= 3; i++) { float a = Math.max(0f, 1f - i * 0.3f - 0.3f * (1f - t)); for (int k = 0; k < 6; k++) pxa(9 + i * 2, 5 + k * 2 - (k % 2), 0x9AB8FF, a); }
                outline();
            }
            case "snd_end" -> {                                                              // End: a bell struck once, a last ring
                float sw = s * 1.2f;
                poly(new float[]{8 + sw * 0.3f, 12 + sw * 0.5f, 13 + sw, 3 + sw, 4 + sw * 0.5f}, new float[]{2, 6, 12, 12, 6}, 0xFFD25A);
                bev(2 + Math.round(sw), 12, 12, 2, 0xE8A62A);
                px(Math.round(8 + sw), 14, 0x8A5A12);
                outline();
                if (t > 0.3f) { pxa(1, 7, 0xFFE9A0, t); pxa(0, 9, 0xFFE9A0, t * 0.6f); pxa(14, 7, 0xFFE9A0, t); pxa(15, 9, 0xFFE9A0, t * 0.6f); }
            }
            case "bell" -> {                                                                 // a bell swinging
                float sw = s * 1.6f;
                poly(new float[]{8 + sw * 0.3f, 12 + sw * 0.6f, 13.5f + sw, 2.5f + sw, 4 + sw * 0.6f}, new float[]{2, 6, 12, 12, 6}, 0xFFD25A);
                bev(1 + Math.round(sw), 12, 14, 2, 0xE8A62A);
                px(Math.round(8 + sw * 1.2f), 14, 0x8A5A12); px(Math.round(8 + sw * 1.2f), 15, 0x8A5A12);
                rect(7, 1, 2, 1, 0xB8861E);
                outline();
                glint(5 + Math.round(sw * 0.6f), 5, 1, 0xFFFFFF, 0.9f);
            }
            case "coin" -> {                                                                 // a coin spinning
                float w = Math.abs((float) Math.cos(f * TAU / 2f)) * 6.2f + 0.8f;
                ball(8, 8, 6.5f, 0xFFC93A);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { float dx = (x + 0.5f - 8); if (Math.hypot(dx, y + 0.5f - 8) <= 6.5f && Math.abs(dx) > w) px(x, y, 0xFFC93A); }
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { float dx = x + 0.5f - 8, dy = y + 0.5f - 8; if (Math.abs(dx) <= w && Math.hypot(dx / (w / 6.5f), dy) <= 6.5f) { float l = (-dx * 0.4f - dy * 0.7f) / 6f; px(x, y, l > 0.1f ? lite(0xFFC93A, l * 0.9f) : dark(0xE8A62A, -l * 0.5f)); } }
                if (w > 3.5f) { rect(7, 5, 2, 6, 0xB8861E); rect(6, 6, 4, 1, 0xB8861E); rect(6, 9, 4, 1, 0xB8861E); }
                outline();
            }
            case "size" -> {                                                                 // a frame growing, a corner arrow
                int g = Math.round(2 * t);
                bev(3 - g / 2, 3 - g / 2, 8 + g, 8 + g, 0x7A92E0);
                rect(4 - g / 2, 4 - g / 2, 6 + g, 6 + g, 0x1B2236);
                ln(9, 9, 14, 14, 0xFFFFFF); ln(14, 11, 14, 14, 0xFFFFFF); ln(11, 14, 14, 14, 0xFFFFFF);
                outline();
            }
            case "text" -> {                                                                 // a letter in a tile, a blinking cursor
                bev(1, 2, 14, 12, 0x5A74D8);
                letter(GLYPH_A, 4, 4, 0xFFFFFF, true);
                if (f < 0.5f) rect(9, 4, 1, 5, 0xFFE08A);
                rect(10, 11, 3, 1, 0xBFD0FF);
                outline();
            }
            case "bars" -> {                                                                 // cinematic bars sliding over a picture
                bev(1, 2, 14, 12, 0x6FC8FF);
                rect(2, 9, 12, 4, 0x4FA84A); ball(11, 5, 1.8f, 0xFFE08A);
                int bh = Math.round(1 + 2.5f * t);
                rect(1, 2, 14, bh, 0x0B0E1A); rect(1, 14 - bh, 14, bh, 0x0B0E1A);
                outline();
            }
            case "format" -> {                                                               // { } with a cursor
                ln(5, 3, 3, 5, 0xFFC857); ln(3, 5, 3, 7, 0xFFC857); ln(3, 7, 2, 8, 0xFFC857); ln(2, 8, 3, 9, 0xFFC857); ln(3, 9, 3, 11, 0xFFC857); ln(3, 11, 5, 13, 0xFFC857);
                ln(10, 3, 12, 5, 0xFFC857); ln(12, 5, 12, 7, 0xFFC857); ln(12, 7, 13, 8, 0xFFC857); ln(13, 8, 12, 9, 0xFFC857); ln(12, 9, 12, 11, 0xFFC857); ln(12, 11, 10, 13, 0xFFC857);
                outline();
                if (f < 0.5f) rect(7, 4, 2, 8, 0xFFFFFF);
            }
            case "note" -> {                                                                 // a music note bobbing
                int by = Math.round(s * 1f);
                ball(5.5f, 11.5f + by, 2.6f, 0xA8B8FF);
                rect(7, 2 + by, 2, 10, 0xA8B8FF); rect(7, 2 + by, 2, 1, 0xFFFFFF);
                quad(9, 2 + by, 13, 4 + by, 13, 7 + by, 9, 5 + by, 0x7F92F0);
                outline();
            }
            case "glass" -> {                                                                // a magnifying glass with a gleam
                ball(6.5f, 6.5f, 4.8f, 0x9AD8FF);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { float r = (float) Math.hypot(x + 0.5f - 6.5f, y + 0.5f - 6.5f); if (r > 3.3f && r <= 4.8f) px(x, y, 0xD8A52A); else if (r <= 3.3f) { px(x, y, mix(0xBFE8FF, 0x6FB8E8, (x + y) / 18f)); } }
                ln(10, 10, 14, 14, 0x8A5A2C); ln(11, 10, 14, 13, 0xB5733A);
                outline();
                int gx = 4 + Math.round(f * 3); px(gx, 5, 0xFFFFFF); px(gx + 1, 4, 0xFFFFFF);
            }
            case "cosmetics" -> {                                                            // a wizard hat with a star
                tri(8, 1, 3, 12, 13, 12, 0x7A5AE8);
                tri(8, 1, 8, 12, 13, 12, 0x5A3AC8);
                bev(1, 11, 14, 3, 0x4A2AA8);
                rect(4, 11, 8, 1, 0xFFD25A);
                outline();
                glint(8, 5, 1, 0xFFE9A8, 0.4f + 0.6f * t); glint(12 - Math.round(f * 2), 3, 1, 0xFFFFFF, 0.5f + 0.5f * (1f - t));
            }
            case "cos_aura" -> {                                                             // a glowing ring pulsing round a figure
                glow(8, 8, 8, 0xB56BFF, 0.35f + 0.25f * t);
                for (int a = 0; a < 40; a++) { float ang = a / 40f * TAU; px(Math.round(8 + (float) Math.cos(ang) * (5.5f + t)), Math.round(8 + (float) Math.sin(ang) * (5.5f + t)), mix(0xFF9AF0, 0x9A7AFF, a / 40f)); }
                ball(8, 7, 2.2f, 0xFFE0C0); quad(5.5f, 9.5f, 10.5f, 9.5f, 11, 14, 5, 14, 0x8A6AE8);
                outline();
            }
            case "cos_name" -> {                                                             // a name tag
                bev(1, 4, 14, 8, 0xFFE9A8);
                rect(2, 5, 12, 1, 0xFF5A5A);
                rect(3, 8, 7, 1, 0x6A5A3A); rect(3, 10, 5, 1, 0x6A5A3A);
                outline();
                glint(12, 9, 1, 0xFFFFFF, 0.3f + 0.7f * t);
            }
            case "cos_suffix" -> {                                                           // a tag with a plus, glittering
                quad(1, 5, 11, 5, 15, 8, 11, 11, 0xB48CFF); poly(new float[]{1, 11, 15, 11, 1}, new float[]{5, 5, 8, 11, 11}, 0xB48CFF);
                rect(3, 7, 4, 1, 0xFFFFFF); rect(4, 6, 1, 3, 0xFFFFFF);
                px(11, 8, 0x3A2A6A);
                outline();
                glint(13, 4, 1, 0xFFFFFF, 0.3f + 0.7f * t);
            }
            case "chan_party" -> {                                                           // three friends
                for (int i = 0; i < 3; i++) { int bx = 3 + i * 5, by = i == 1 ? 3 : 5 + Math.round(Math.max(0f, (float) Math.sin(f * TAU + i * 2)) * -1f); ball(bx, by + 1, 2.1f, i == 1 ? 0xFFD0A0 : 0xF0C090); quad(bx - 2.5f, by + 3.5f, bx + 2.5f, by + 3.5f, bx + 3, by + 10 - (i == 1 ? 0 : 1), bx - 3, by + 10 - (i == 1 ? 0 : 1), i == 0 ? 0x5AA8FF : i == 1 ? 0x62E68A : 0xFF8AB0); }
                outline();
            }
            case "chan_coop" -> {                                                            // a little house shared by two
                tri(1, 7.5f, 8, 1.5f, 15, 7.5f, 0x4FB07A);
                bev(3, 7, 10, 7, 0xD8F0E0);
                ball(6, 10, 1.6f, 0xF0C090); ball(10, 10, 1.6f, 0xF0C090); rect(5, 12, 2, 2, 0x5AA8FF); rect(9, 12, 2, 2, 0xFF8AB0);
                outline();
                px(8, 5, 0xFFFFFF);
            }
            case "chan_public" -> {                                                          // a globe turning
                ball(8, 8, 6.6f, 0x3F8FE0);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                    float dx = x + 0.5f - 8, dy = y + 0.5f - 8; if (Math.hypot(dx, dy) > 6.5f) continue;
                    float lon = (float) Math.asin(dx / 6.6f) + f * TAU;
                    float land = (float) (Math.sin(lon * 2.0 + dy * 0.55) + Math.sin(lon * 3.0 - dy * 0.4) * 0.6);
                    if (land > 0.55f) { float l = (-dx * 0.5f - dy * 0.7f) / 7f; px(x, y, l > 0.1f ? 0x7BE05A : dark(0x4FA84A, Math.max(0f, -l) * 0.7f)); }
                }
                outline();
                px(5, 4, 0xFFFFFF); px(6, 3, 0xDDEEFF);
            }
            case "chan_off" -> {                                                             // a bubble crossed out
                bev(2, 3, 12, 8, 0x6A7388); tri(4, 11, 4, 14, 8, 11, 0x6A7388);
                outline();
                ln(2, 2, 14, 13, 0xFF5A5A); ln(3, 2, 14, 12, 0xFF8A8A);
            }
            case "bazaar" -> {                                                               // a market stall with a striped awning
                for (int i = 0; i < 6; i++) quad(1 + i * 2.3f, 3, 3.3f + i * 2.3f, 3, 3.3f + i * 2.3f + (i % 2 == 0 ? 0 : 0), 7, 1 + i * 2.3f, 7, i % 2 == 0 ? 0xE8503A : 0xF4F4F8);
                rect(1, 7, 14, 1, 0x8F2F22);
                bev(2, 8, 12, 6, 0xB5733A);
                ball(5.5f, 10.5f, 1.7f, 0xFF7A5A); ball(9, 10.5f, 1.7f, 0xFFC93A); ball(12, 11, 1.4f, 0x7BE05A);
                outline();
                glint(9, 9, 1, 0xFFFFFF, t);
            }
            case "hammer" -> {                                                               // an auction gavel
                float sw = f < 0.5f ? t : 1f - (f - 0.5f) * 2f;
                float a = -0.9f + 0.9f * (f < 0.5f ? f * 2f : 1f - (f - 0.5f) * 2f * 0.0f);
                float hx = 10 - 2 * sw, hy = 4 + 3 * sw;
                ln(3, 13, hx - 1, hy + 2, 0x8A5A2C); ln(4, 13, hx, hy + 2, 0x6B4423);
                quad(hx - 3, hy - 1, hx + 3, hy - 3, hx + 5, hy + 1, hx - 1, hy + 3, 0xB5733A);
                quad(hx - 3, hy - 1, hx + 3, hy - 3, hx + 3.6f, hy - 2, hx - 2.4f, hy, 0xE8A06A);
                rect(2, 13, 6, 2, 0x6A7388);
                outline();
                if (sw > 0.85f) { glint(2, 12, 1, 0xFFFFFF, 1f); px(1, 11, 0xFFE27A); px(8, 12, 0xFFE27A); }
            }
            case "back" -> {                                                                 // an arrow pointing back, nudging
                int nx = Math.round(-1f * t);
                tri(1 + nx, 8, 8 + nx, 2, 8 + nx, 14, 0x8FA8FF);
                bev(7 + nx, 5, 7, 6, 0x8FA8FF);
                outline();
            }
            case "updates", "refresh" -> {                                                   // a circular arrow turning
                float rot = f * TAU;
                for (int a = 0; a < 44; a++) { float ang = rot + a / 44f * 5.0f; float r = 5.2f; px(Math.round(8 + (float) Math.cos(ang) * r), Math.round(8 + (float) Math.sin(ang) * r), a > 36 ? 0xFFFFFF : mix(0x4AD8A0, 0x8FFFD0, a / 44f)); px(Math.round(8 + (float) Math.cos(ang) * (r - 1)), Math.round(8 + (float) Math.sin(ang) * (r - 1)), dark(0x4AD8A0, 0.15f)); }
                float ea = rot + 5.0f; float ex = 8 + (float) Math.cos(ea) * 5.2f, ey = 8 + (float) Math.sin(ea) * 5.2f;
                tri(ex + (float) Math.cos(ea + 1.57f) * 3, ey + (float) Math.sin(ea + 1.57f) * 3, ex - (float) Math.cos(ea + 1.57f) * 2.4f, ey - (float) Math.sin(ea + 1.57f) * 2.4f, ex + (float) Math.cos(ea + 1.57f * 2) * 3.2f, ey + (float) Math.sin(ea + 1.57f * 2) * 3.2f, 0xBFFFE8);
                outline();
            }
            case "switch" -> {                                                               // a toggle sliding
                bev(1, 4, 14, 8, mix(0x55606E, 0x4AD8A0, t));
                int kx = 2 + Math.round(t * 7);
                bev(kx, 5, 5, 6, 0xF4F8FF);
                outline();
            }
            case "speed" -> {                                                                // a bolt
                poly(new float[]{10, 4, 8, 13, 5, 7, 11}, new float[]{1, 8, 8, 15, 9, 9, 1}, 0xFFD25A);
                outline();
                glow(8, 8, 7, 0xFFE27A, 0.2f + 0.2f * t);
                glint(12, 4 + Math.round(t * 2), 1, 0xFFFFFF, 1f);
            }
            case "clock" -> {                                                                // a ticking clock
                ball(8, 8, 6.8f, 0xF4F0E4);
                for (int i = 0; i < 12; i++) { float a = i * TAU / 12; px(Math.round(8 + (float) Math.cos(a) * 5.4f), Math.round(8 + (float) Math.sin(a) * 5.4f), 0x6A5A3A); }
                float ma = f * TAU - 1.57f; ln(8, 8, 8 + (float) Math.cos(ma) * 4.6f, 8 + (float) Math.sin(ma) * 4.6f, 0xD8503A);
                ln(8, 8, 8 + (float) Math.cos(-0.5f) * 3f, 8 + (float) Math.sin(-0.5f) * 3f, 0x2A2A3A);
                px(8, 8, 0x2A2A3A);
                outline();
            }
            case "send" -> {                                                                 // a paper plane flying off
                int ox = Math.round(t * -1.5f), oy = Math.round(t * 1.5f);
                tri(1 + ox, 7 + oy, 15 + ox, 2 + oy, 6 + ox, 13 + oy, 0xF4F8FF);
                tri(6 + ox, 13 + oy, 15 + ox, 2 + oy, 8 + ox, 9 + oy, 0x9AB0E8);
                outline();
                pxa(1, 12, 0xBFD0FF, 1f - t); pxa(2, 14, 0xBFD0FF, 0.6f);
            }
            case "play" -> {                                                                 // a play button, pulsing
                float sc = 0.9f + 0.1f * t;
                tri(4, 8 - 6 * sc, 4, 8 + 6 * sc, 14, 8, 0x62E68A);
                outline();
                px(6, 5, 0xFFFFFF); px(6, 6, 0xDFFFE8);
            }
            case "menu" -> {                                                                 // three bars sliding
                for (int i = 0; i < 3; i++) { int sh = Math.round((float) Math.sin(f * TAU + i * 0.9f) * 1f); bev(2 + sh, 3 + i * 4, 12, 2, 0xB8C4F0); }
                outline();
            }
            case "disc" -> {                                                                 // a record spinning
                ball(8, 8, 7f, 0x2A2F44);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { float r = (float) Math.hypot(x + 0.5f - 8, y + 0.5f - 8); if (r < 7 && r > 2.4f && ((int) (r * 1.6f) % 2 == 0)) px(x, y, 0x3C4260); else if (r <= 2.4f) px(x, y, 0xE0508A); }
                float a = f * TAU; px(Math.round(8 + (float) Math.cos(a) * 4.5f), Math.round(8 + (float) Math.sin(a) * 4.5f), 0xFFFFFF); px(8, 8, 0x14161F);
                outline();
            }
            case "cube" -> {                                                                 // a cube turning
                float a = f * TAU / 4f; float k = (float) Math.cos(a), q = (float) Math.sin(a);
                float cx = 8, cy = 8;
                float[] fx = {cx - 5 * k - 5 * q, cx + 5 * k - 5 * q, cx + 5 * k + 5 * q, cx - 5 * k + 5 * q};
                poly(new float[]{cx, cx + 6, cx, cx - 6}, new float[]{cy - 7, cy - 3.5f, cy, cy - 3.5f}, 0xA8E8FF);
                poly(new float[]{cx - 6, cx, cx, cx - 6}, new float[]{cy - 3.5f, cy, cy + 7, cy + 3.5f}, 0x55B8F0);
                poly(new float[]{cx, cx + 6, cx + 6, cx}, new float[]{cy, cy - 3.5f, cy + 3.5f, cy + 7}, 0x2E88C8);
                outline();
                glint(5 + Math.round(f * 6), 4, 1, 0xFFFFFF, 0.7f);
            }
            case "cone" -> {                                                                 // a construction cone
                quad(6, 2, 10, 2, 13, 12, 3, 12, 0xFF8A2A);
                quad(5.2f, 6, 10.8f, 6, 11.5f, 8.2f, 4.5f, 8.2f, 0xF4F4F8);
                bev(1, 12, 14, 2, 0xE06A1A);
                outline();
                glint(7, 4, 1, 0xFFFFFF, 0.4f + 0.6f * t);
            }
            case "griffin" -> {                                                              // a feather in the colour you give it
                int col = tint != 0 ? tint : 0xFFD25A;
                poly(new float[]{13, 8, 2, 4.5f, 9, 11}, new float[]{1.5f, 2, 13, 11.5f, 6, 7}, col);
                poly(new float[]{13, 9, 4, 12}, new float[]{1.5f, 6, 11.5f, 5}, lite(col, 0.4f));
                ln(2, 14, 12.5f, 2, lite(col, 0.75f));
                outline();
                glint(11, 4 + Math.round(t * 2), 1, 0xFFFFFF, 0.4f + 0.6f * t);
            }
            case "dock_top", "dock_bottom", "dock_off" -> {                                  // a screen with the preview docked
                bev(1, 2, 14, 12, 0x2B3350);
                if (n.equals("dock_top")) bev(3 + Math.round(t), 3, 10, 4, 0x5A74D8);
                else if (n.equals("dock_bottom")) bev(3 + Math.round(t), 9, 10, 4, 0x5A74D8);
                else { ln(3, 4, 12, 11, 0xFF5A5A); ln(3, 11, 12, 4, 0xFF5A5A); }
                outline();
            }
            case "anim_menu" -> {                                                            // a window bouncing open
                int h = Math.round(5 + 3 * t);
                bev(2, 8 - h / 2, 12, h, 0x7A92E0); rect(3, 9 - h / 2, 10, 1, 0xBFD0FF);
                outline();
            }
            case "anim_icons" -> {                                                           // a small icon in motion
                int bx = 5 + Math.round(s * 1.5f);
                ball(bx + 2, 8, 3.4f, 0xFFC857);
                for (int i = 1; i <= 3; i++) pxa(bx - i * 2, 8, 0xFFC857, 0.7f / i);
                outline();
                glint(bx + 4, 5, 1, 0xFFFFFF, 1f);
            }
            case "fancydrops" -> {                                                           // the logo: a gem over a chest
                bev(2, 10, 12, 5, 0x9A5E2C); bev(7, 10, 2, 3, 0xFFD25A);
                int gy = Math.round(-1f * t);
                poly(new float[]{5, 11, 13, 8, 3}, new float[]{3 + gy, 3 + gy, 6 + gy, 11 + gy, 6 + gy}, 0x55D8FF);
                poly(new float[]{5, 8, 11, 8}, new float[]{3 + gy, 6 + gy, 3 + gy, 3 + gy}, 0xBFF1FF);
                outline();
                glint(3, 3, 2, 0xFFFFFF, t); glint(13, 2 + Math.round(t), 1, 0xFF72C6, 1f - t);
            }
            case "discord" -> {                                                              // community: two bubbles chatting, bobbing against each other, the dots typing in turn
                int o1 = Math.round((float) Math.sin(f * Math.PI * 2) * 1.2f), o2 = -o1, lit = (int) (f * 3f) % 3, lit2 = (int) (f * 3f + 1f) % 3;
                bev(1, 2 + o1, 9, 7, 0x7A8CF0); tri(3, 9 + o1, 3, 12 + o1, 6, 9 + o1, 0x7A8CF0);
                bev(6, 7 + o2, 9, 6, 0xB8C2FF); tri(12, 13 + o2, 13, 15.5f + o2, 10, 13 + o2, 0xB8C2FF);
                for (int d = 0; d < 3; d++) rect(3 + d * 2, 4 + o1 - (d == lit ? 1 : 0), 1, 1, d == lit ? 0xFFFFFF : 0xC9D2FF);
                outline();
                for (int d = 0; d < 3; d++) rect(8 + d * 2, 9 + o2 - (d == lit2 ? 1 : 0), 1, 1, d == lit2 ? 0x2A36B8 : 0x4A5AD8);
            }
            case "patreon" -> {                                                              // support: a heart that beats, with a spark
                float k = 1f + 0.17f * t, cxh = 8f, cyh = 8.5f;
                float[] hx = {8, 14, 15, 8, 1, 2}, hy = {3.2f, 1.5f, 6, 15, 6, 1.5f};
                for (int q = 0; q < 6; q++) { hx[q] = cxh + (hx[q] - cxh) * k; hy[q] = cyh + (hy[q] - cyh) * k; }
                poly(hx, hy, 0xFF6A5A);
                ball(cxh + (4.6f - cxh) * k, cyh + (5 - cyh) * k, 3.4f * k, 0xFF6A5A); ball(cxh + (11.4f - cxh) * k, cyh + (5 - cyh) * k, 3.4f * k, 0xFF6A5A);
                tri(cxh + (1.4f - cxh) * k, cyh + (6.2f - cyh) * k, cxh + (14.6f - cxh) * k, cyh + (6.2f - cyh) * k, 8, cyh + (14.5f - cyh) * k, 0xE04A4A);
                outline();
                px(4, 3, 0xFFFFFF); glint(13, 2, 1, 0xFFE08A, Math.max(0f, t));
            }
            case "github" -> {                                                               // source: a branching tree, nodes breathing in turn, a commit travelling up
                ln(5, 3, 5, 13, 0xC8D0E4); ln(6, 3, 6, 13, 0x9AA4C0);
                ln(5, 8, 11, 4, 0xC8D0E4); ln(6, 8, 12, 4, 0x9AA4C0);
                float w = f * (float) Math.PI * 2;
                ball(5.5f, 3, 2.2f + 0.7f * (float) Math.sin(w), 0x62E6A8); ball(5.5f, 13, 2.2f + 0.7f * (float) Math.sin(w + 2.1f), 0x62E6A8); ball(11.5f, 4, 2.2f + 0.7f * (float) Math.sin(w + 4.2f), 0xFFC857);
                outline();
                float u = f; int cx2, cy2;
                if (u < 0.5f) { cx2 = 5; cy2 = Math.round(13 - 10 * u); } else { float v = (u - 0.5f) * 2f; cx2 = Math.round(5.5f + 6 * v); cy2 = Math.round(8 - 4 * v); }
                px(cx2, cy2, 0xFFFFFF); pxa(cx2, cy2 + 1, 0xFFFFFF, 0.5f);
            }
            default -> {
                if (n.startsWith("cx_")) return generic(n, f);
                if (n.startsWith("mob_")) { mob(n.substring(4), f); outline(); return true; }
                if (n.startsWith("boss_")) { mob(n.substring(5), f); outline(); return true; }
                if (n.startsWith("emblem")) return paint("emblem", tint, f);
                return false;
            }
        }
        return true;
    }

    /** The mound of earth under the spade. */
    private static void fillMound() { for (int y = 11; y < 15; y++) for (int x = 1; x < 15; x++) { float dx = (x + 0.5f - 8) / 7.2f, dy = (y + 0.5f - 14.5f) / 3.8f; if (dx * dx + dy * dy <= 1f) px(x, y, y < 12 ? 0x9A7A52 : y < 14 ? 0x6B4A2B : 0x4A3220); } }

    /** Cosmetic effects and auras without their own picture: a glowing orb in a colour taken from the name. */
    private static boolean generic(String n, float f) {
        float h = (n.hashCode() & 0xFFFF) / 65535f; int col = hue(h, 0.55f, 1f);
        glow(8, 8, 8, col, 0.3f + 0.2f * tri(f));
        ball(8, 8, 4.6f, col);
        for (int a = 0; a < 3; a++) { float ang = f * TAU + a * 2.09f; px(Math.round(8 + (float) Math.cos(ang) * 7), Math.round(8 + (float) Math.sin(ang) * 5.5f), 0xFFFFFF); }
        outline();
        return true;
    }

    // ------------------------------------------------------------------ the 25 text effects: the letter A, doing what the effect does
    private static final String[] BIG_A = {".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"};
    // ---- the twelve Mythological creatures (Drop Library sidebar and tiles)
    private static void mob(String id, float f) {
        float t = tri(f), s = sn(f);
        switch (id) {
            case "minos_hunter" -> {                                                         // a hooded hunter drawing a bow
                tri(3, 14, 8, 2, 13, 14, 0x2F6B3A); tri(5, 14, 8, 4, 11, 14, 0x3F8A4C);
                ball(8, 7, 2.6f, 0x1A2A1C); px(7, 7, 0xFFE6A0); px(9, 7, 0xFFE6A0);
                rect(5, 12, 6, 3, 0x6B4A2B);
                float pull = 1.5f * t;
                ln(13, 3, 14 + 0.5f, 8, 0xC8A060); ln(14.5f, 8, 13, 13, 0xC8A060);
                ln(13 - pull, 8, 13, 3, 0xE8E8E8); ln(13 - pull, 8, 13, 13, 0xE8E8E8);
                ln(6 + pull, 8, 14, 8, 0xD8D0B8); px(15, 8, 0xFFFFFF);
            }
            case "siamese_lynxes" -> {                                                       // two cats side by side
                for (int k = 0; k < 2; k++) {
                    int ox = k * 7, base = k == 0 ? 0xD8B27A : 0xB8946A;
                    tri(1 + ox, 7, 1 + ox, 2, 4 + ox, 5, base); tri(8 + ox - 1, 7, 7 + ox + 1, 2, 5 + ox, 5, base);
                    px(2 + ox, 4, 0xF0A0A0);
                    ball(4.5f + ox, 9, 3.4f, base); px(1 + ox, 11, lite(base, 0.5f)); px(8 + ox, 11, lite(base, 0.5f));
                    boolean blink = (k == 0 ? f > 0.55f && f < 0.65f : f > 0.2f && f < 0.3f);
                    if (blink) { px(3 + ox, 9, 0x1A1208); px(6 + ox, 9, 0x1A1208); } else { px(3 + ox, 8, 0x7CFF9A); px(3 + ox, 9, 0x101810); px(6 + ox, 8, 0x7CFF9A); px(6 + ox, 9, 0x101810); }
                    px(4 + ox, 10, 0xF08A8A); px(5 + ox, 10, 0xF08A8A);
                    px(4 + ox, 2 + (k == 0 ? 0 : 0), 0x3A2A18);
                }
                rect(1, 13, 14, 2, 0x8A6A44);
            }
            case "stranded_nymph" -> {                                                       // a sea nymph with flowing aqua hair
                float sw = s * 1.2f;
                for (int y = 3; y < 15; y++) { float k = (y - 3) / 12f; int x0 = Math.round(3 - sw * k), x1 = Math.round(12 + sw * k); px(x0, y, 0x2FB8B0); px(x0 + 1, y, 0x58E0D0); px(x1, y, 0x2FB8B0); px(x1 - 1, y, 0x58E0D0); }
                ball(8, 6, 3.2f, 0x2FB8B0); ball(8, 7, 2.6f, 0xF6D2B4);
                px(7, 7, 0x203040); px(9, 7, 0x203040); px(8, 9, 0xE08A8A);
                rect(4, 3, 8, 2, 0x58E0D0); px(8, 2, 0xFFFFFF);
                tri(5, 15, 8, 11, 11, 15, 0x3F8AD8);
                float d = (f * 12) % 12; pxa(14, 2 + Math.round(d), 0x9AE8FF, 1f - d / 12f);
            }
            case "cretan_bull" -> {                                                          // a bull's head with a ring in its nose
                ln(1, 6, 3, 3, 0xF0E8D0); ln(1, 6, 2, 8, 0xF0E8D0); ln(15, 6, 13, 3, 0xF0E8D0); ln(15, 6, 14, 8, 0xF0E8D0);
                px(2, 3, 0xFFFFFF); px(13, 3, 0xFFFFFF);
                rect(3, 5, 10, 6, 0x7A4A2A); rect(4, 4, 8, 1, 0x7A4A2A);
                rect(5, 10, 6, 4, 0xC89870); rect(6, 13, 4, 1, 0xA87850);
                px(5, 7, t > 0.5f ? 0x101010 : 0xFF6A3A); px(10, 7, t > 0.5f ? 0x101010 : 0xFF6A3A);
                px(6, 11, 0x301810); px(9, 11, 0x301810);
                ln(7, 13, 8, 15, 0xE8D060); px(9, 13, 0xE8D060);
                if (t > 0.5f) { pxa(5, 15, 0xD8E8F0, 0.7f); pxa(10, 15, 0xD8E8F0, 0.7f); }
            }
            case "harpy" -> {                                                                // a flapping harpy
                float fl = s * 3f;
                poly(new float[]{7, 0, 1, 7}, new float[]{8, 5 - fl, 9 - fl, 11}, 0x9A5AB8); poly(new float[]{9, 16, 15, 9}, new float[]{8, 5 - fl, 9 - fl, 11}, 0x9A5AB8);
                ln(1, 9 - fl, 7, 9, 0xC890E0); ln(15, 9 - fl, 9, 9, 0xC890E0);
                rect(6, 8, 4, 5, 0xB87AD0); tri(6, 13, 8, 15, 10, 13, 0x7A3A98);
                ball(8, 6, 2.4f, 0xF2CBA8); rect(5, 3, 6, 2, 0x5A2A78); px(5, 5, 0x5A2A78); px(10, 5, 0x5A2A78);
                px(7, 6, 0x301020); px(9, 6, 0x301020);
                px(6, 14, 0xE8D060); px(10, 14, 0xE8D060);
            }
            case "gaia_construct" -> {                                                       // a stone golem with green eyes
                bev(3, 3, 10, 10, 0x8A8F9A); rect(4, 4, 8, 2, 0xA8ADB8);
                rect(1, 6, 2, 6, 0x6E7380); rect(13, 6, 2, 6, 0x6E7380);
                rect(4, 7, 3, 2, 0x161A18); rect(9, 7, 3, 2, 0x161A18);
                int ey = mix(0x3FD16A, 0xB8FFC8, t); px(5, 7, ey); px(6, 7, ey); px(10, 7, ey); px(11, 7, ey);
                rect(5, 11, 6, 1, 0x4A4F5A); for (int i = 5; i < 11; i += 2) px(i, 12, 0x4A4F5A);
                px(4, 4, 0x4F9A4A); px(5, 3, 0x4F9A4A); px(11, 12, 0x4F9A4A); px(12, 11, 0x4F9A4A);
                glow(8, 8, 6, 0x3FD16A, 0.12f * t);
            }
            case "minotaur" -> {                                                             // the horned dark head, eyes burning
                ln(2, 6, 2, 3, 0xE8E0C8); ln(2, 3, 4, 2, 0xE8E0C8); ln(13, 6, 13, 3, 0xE8E0C8); ln(13, 3, 11, 2, 0xE8E0C8);
                rect(4, 3, 8, 7, 0x4A2C2C); rect(3, 5, 10, 4, 0x4A2C2C); rect(5, 9, 6, 5, 0x3A2222); rect(6, 13, 4, 1, 0x2A1616);
                int ey = mix(0xFF4A2A, 0xFFE0A0, t); px(5, 6, ey); px(6, 6, ey); px(9, 6, ey); px(10, 6, ey);
                px(7, 11, 0x1A0E0E); px(8, 11, 0x1A0E0E); px(7, 12, 0xA8A0A0); px(8, 12, 0xA8A0A0);
                glow(8, 6, 7, 0xFF4A2A, 0.12f * t);
            }
            case "minos_champion" -> {                                                       // a gold helm with a red plume
                for (int i = 0; i < 6; i++) { float w = (float) Math.sin((f - i * 0.08f) * TAU) * 1.2f; px(Math.round(8 + w), 1 + i, i % 2 == 0 ? 0xD8402A : 0xA82A1A); px(Math.round(9 + w), 1 + i, 0xA82A1A); }
                rect(4, 6, 8, 4, 0xE0B83A); rect(3, 8, 10, 6, 0xC89A28); rect(4, 6, 8, 1, 0xFFE27A);
                rect(5, 9, 6, 1, 0x1A1208); rect(7, 9, 2, 5, 0x1A1208);
                rect(3, 13, 10, 1, 0x8A6A18); px(5, 9, mix(0x1A1208, 0xFFE27A, t * 0.7f)); px(10, 9, mix(0x1A1208, 0xFFE27A, t * 0.7f));
                px(4, 7, 0xFFF3B0);
            }
            case "sphinx" -> {                                                               // the headdress of a sphinx, gold and blue stripes
                poly(new float[]{3, 8, 13, 14, 2}, new float[]{3, 1, 3, 14, 14}, 0x2F5FB0);
                for (int y = 3; y < 14; y += 2) { rect(3, y, 2, 1, 0xE8C040); rect(11, y, 2, 1, 0xE8C040); }
                rect(5, 3, 6, 9, 0xE6B87A); rect(5, 3, 6, 1, 0xE8C040);
                px(6, 6, 0x101828); px(9, 6, 0x101828); px(6, 5, 0xE8C040); px(9, 5, 0xE8C040);
                rect(7, 8, 2, 2, 0xC8986A); rect(6, 11, 4, 1, 0xB87A5A);
                px(8, 1, 0xE8C040); if (t > 0.6f) px(8, 2, 0xFFF3B0);
            }
            case "minos_inquisitor" -> {                                                     // a white mask in a dark purple hood
                tri(2, 15, 8, 0, 14, 15, 0x4A2A6A); tri(4, 15, 8, 3, 12, 15, 0x2A1A3A);
                rect(5, 4, 6, 8, 0xEAEAF0); rect(6, 12, 4, 1, 0xEAEAF0); rect(7, 13, 2, 1, 0xC8C8D8);
                int ey = mix(0xB46AFF, 0xF0D8FF, t); rect(5, 7, 2, 1, ey); rect(9, 7, 2, 1, ey);
                ln(8, 8, 8, 10, 0xB8B8C8); px(8, 5, 0xB46AFF);
                glow(8, 7, 6, 0xB46AFF, 0.12f * t);
            }
            case "manticore" -> {                                                            // a lion's face with a mane and a stinging tail
                for (int i = 0; i < 12; i++) { float a = i * 0.5236f; px(Math.round(8 + (float) Math.cos(a) * 6), Math.round(9 + (float) Math.sin(a) * 5.5f), 0xC8601A); px(Math.round(8 + (float) Math.cos(a) * 5), Math.round(9 + (float) Math.sin(a) * 4.5f), 0xE08A2A); }
                ball(8, 9, 4.4f, 0xE8B060); ball(8, 9, 3.4f, 0xF0C47A);
                px(6, 8, 0x201008); px(10, 8, 0x201008); px(8, 10, 0x6A2A18); px(7, 11, 0x6A2A18); px(9, 11, 0x6A2A18);
                px(5, 5, 0xE8B060); px(11, 5, 0xE8B060);
                float sw = s * 1.5f;
                ln(13, 5, 14 + sw * 0.4f, 2, 0x7A3A8A); ln(14 + sw * 0.4f, 2, 12 + sw, 0, 0x7A3A8A); px(12 + Math.round(sw), 0, 0xD0F060);
            }
            case "king_minos" -> {                                                           // a crowned, bearded king in purple
                rect(2, 12, 12, 3, 0x6A2A9A); rect(3, 12, 10, 1, 0xA860D8);
                rect(5, 5, 6, 6, 0xF2CBA8); rect(4, 9, 8, 3, 0xE8E0D0); rect(5, 12, 6, 1, 0xC8C0B0);
                px(6, 7, 0x203040); px(9, 7, 0x203040); px(7, 9, 0xB89888); px(8, 9, 0xB89888);
                rect(4, 3, 8, 2, 0xE8C040); px(4, 2, 0xE8C040); px(7, 1, 0xE8C040); px(8, 1, 0xE8C040); px(11, 2, 0xE8C040);
                px(7, 4, 0xE84A4A); px(5, 3, mix(0x4A9AFF, 0xFFFFFF, t)); px(10, 3, mix(0x4AD86A, 0xFFFFFF, 1f - t)); px(8, 2, 0xFFF3B0);
                glint(8, 1, 1, 0xFFFFFF, t);
            }
            case "zombie" -> {                                                               // Revenant Horror: a rotting green head, one arm reaching, a drip of ooze
                float sway = s * 0.6f;
                rect(3, 2, 10, 9, 0x5E9E52); rect(4, 1, 8, 1, 0x5E9E52); rect(3, 2, 10, 1, 0x7FBF70); rect(3, 10, 10, 1, 0x3D6B36);
                rect(4, 5, 3, 2, 0x10200E); rect(9, 5, 3, 2, 0x10200E);
                int ey = mix(0xC03020, 0xFF8A50, t); px(5, 6, ey); px(10, 6, ey);
                rect(5, 9, 6, 1, 0x20120E); px(6, 9, 0xD8D0A8); px(8, 9, 0xD8D0A8); px(10, 9, 0xD8D0A8);
                px(3, 4, 0x3D6B36); px(12, 7, 0x3D6B36); px(7, 3, 0x3D6B36); px(8, 3, 0x7C3A30);
                rect(5, 11, 6, 2, 0x4A6EA8); rect(3, 12, 2, 2, 0x5E9E52); rect(1 + Math.round(sway), 12, 2, 2, 0x5E9E52); rect(11, 12, 4, 2, 0x5E9E52);
                float d = (f * 3) % 1f; pxa(7, 11 + Math.round(d * 4), 0x8CE060, 1f - d);
            }
            case "spider" -> {                                                               // Tarantula Broodfather: a hairy body, eight legs that creep, a red eye cluster
                float cr = s * 0.9f;
                for (int k = 0; k < 4; k++) {
                    float y0 = 5 + k * 1.6f, w = (k % 2 == 0 ? cr : -cr);
                    ln(6, y0 + 1, 2, y0 - 2 + w, 0x2A2325); ln(2, y0 - 2 + w, 0.5f, y0 + 3, 0x2A2325);
                    ln(10, y0 + 1, 14, y0 - 2 - w, 0x2A2325); ln(14, y0 - 2 - w, 15.5f, y0 + 3, 0x2A2325);
                }
                ball(8, 11, 3.8f, 0x4A3F42); ball(8, 6.5f, 2.9f, 0x5A4D50);
                px(7, 4, 0x6A5A5E); px(5, 10, 0x6A5A5E); px(10, 11, 0x3A2F32); px(9, 13, 0x2A2325);
                int ey = mix(0xE03030, 0xFF9A7A, t); px(6, 6, ey); px(9, 6, ey); px(7, 5, 0xB02020); px(8, 5, 0xB02020);
                px(7, 8, 0xE8E0D0); px(8, 8, 0xE8E0D0);
            }
            case "wolf" -> {                                                                 // Sven Packmaster: a grey wolf head, ears up, a snarl, eyes that flash
                tri(2, 8, 3, 0, 7, 5, 0x565B63); tri(14, 8, 13, 0, 9, 5, 0x565B63); px(3, 2, 0xC8A0A0); px(12, 2, 0xC8A0A0);
                rect(3, 5, 10, 6, 0x8A9099); rect(4, 4, 8, 1, 0x8A9099); rect(2, 7, 1, 3, 0x6A7078); rect(13, 7, 1, 3, 0x6A7078);
                rect(5, 9, 6, 5, 0xB8BEC6); rect(6, 13, 4, 1, 0x565B63); rect(7, 8, 2, 2, 0x20242A);
                int ey = t > 0.5f ? 0xFF4040 : 0xC03030; px(4, 6, ey); px(5, 6, ey); px(10, 6, ey); px(11, 6, ey);
                px(5, 11, 0xFFFFFF); px(10, 11, 0xFFFFFF); px(7, 11, 0x20242A); px(8, 11, 0x20242A);
                px(7, 4, 0x6A7078); px(8, 4, 0x6A7078);
                if (t > 0.6f) { px(1, 5, 0xE8EEF4); px(14, 5, 0xE8EEF4); }
            }
            case "enderman" -> {                                                             // Voidgloom Seraph: a tall dark shape, violet eyes, drifting void motes
                rect(5, 1, 6, 5, 0x241B30); rect(4, 6, 8, 1, 0x241B30); rect(6, 7, 4, 7, 0x1A1424); rect(3, 7, 1, 7, 0x241B30); rect(12, 7, 1, 7, 0x241B30);
                rect(6, 14, 1, 2, 0x241B30); rect(9, 14, 1, 2, 0x241B30); px(5, 1, 0x3A2C4A); px(6, 1, 0x3A2C4A);
                int ey = mix(0xD060FF, 0xF0C8FF, t); rect(5, 3, 2, 1, ey); rect(9, 3, 2, 1, ey); px(5, 4, 0x8A3AC0); px(10, 4, 0x8A3AC0);
                for (int i = 0; i < 4; i++) { float a = (f + i * 0.25f) % 1f; pxa(Math.round(1 + i * 4 + sn(f + i * 0.3f) * 1.5f), Math.round(14 - a * 13), 0xD060FF, 1f - a); }
                glow(8, 3, 6, 0xD060FF, 0.14f * t);
            }
            case "blaze" -> {                                                                // Inferno Demonlord: a fiery head ringed by turning blaze rods
                for (int i = 0; i < 4; i++) { float a = (f + i * 0.25f) * TAU; int rx = Math.round(8 + (float) Math.cos(a) * 6), ry = Math.round(8 + (float) Math.sin(a) * 2.2f * 1.6f); rect(rx, ry - 1, 1, 3, i % 2 == 0 ? 0xFFD040 : 0xE8A020); px(rx, ry - 1, 0xFFF080); }
                rect(4, 4, 8, 8, 0xE8A020); rect(5, 3, 6, 1, 0xE8A020); rect(5, 12, 6, 1, 0xB05010); rect(4, 4, 8, 1, 0xFFD040);
                rect(5, 6, 2, 2, 0x401808); rect(9, 6, 2, 2, 0x401808); px(5, 6, 0xFFF080); px(10, 6, 0xFFF080);
                rect(6, 10, 4, 1, 0x401808); px(6, 9, 0xB05010); px(9, 9, 0xB05010);
                float fl = s * 1.2f; px(6, 2, 0xFF7A20); px(Math.round(8 + fl), 1, 0xFFD040); px(10, 2, 0xFF7A20); px(8, 2, 0xFF7A20);
                glow(8, 8, 7, 0xFF8A20, 0.12f * t);
            }
            case "vampire" -> {                                                              // Riftstalker Bloodfiend: a pale face, fangs, a high crimson collar, a drip of blood
                tri(0, 15, 2, 6, 6, 15, 0x6A2634); tri(16, 15, 14, 6, 10, 15, 0x6A2634); rect(5, 12, 6, 3, 0xA03048);
                rect(5, 2, 6, 9, 0xB8A4A8); rect(4, 3, 8, 7, 0xC8B8BC); rect(4, 2, 8, 2, 0x20141C); px(8, 4, 0x20141C); px(7, 4, 0x20141C);
                int ey = mix(0xD01030, 0xFF6070, t); px(6, 6, ey); px(9, 6, ey); px(5, 6, 0x6A2634); px(10, 6, 0x6A2634);
                rect(6, 9, 4, 1, 0x7A2A38); px(6, 10, 0xFFFFFF); px(9, 10, 0xFFFFFF);
                float d = (f * 2) % 1f; pxa(6, 11 + Math.round(d * 3), 0xD01030, 1f - d * 0.8f);
            }
            default -> ball(8, 8, 4, 0x8890A0);
        }
    }

    private static void bigA(int x, int y, int rgb) { letter(BIG_A, x, y, rgb, true); }
    /** The big A with a colour per pixel: col(x, y) gets the pixel's place in the letter. */
    private interface Tint { int at(int x, int y); }
    private static void bigA(int x, int y, Tint t) { for (int j = 0; j < 7; j++) for (int i = 0; i < 5; i++) if (BIG_A[j].charAt(i) == '#') px(x + i, y + j, t.at(i, j)); }

    /** A white die with black pips (a red 1) that hops, tumbles in the air and settles on a 6, with its shadow below. A real cube is turned and drawn face by face. */
    private static void dice(float f) {
        float fl = f < 0.62f ? f / 0.62f : 1f, e = 1f - (1f - fl) * (1f - fl) * (1f - fl);
        float ang = (1f - e) * TAU * 1.75f;
        float hh = f < 0.38f ? (float) Math.sin(f / 0.38f * Math.PI) * 3.2f : f < 0.62f ? (float) Math.sin((f - 0.38f) / 0.24f * Math.PI) * 1.4f : 0f;
        float cy = 7.9f - hh, cx = 8f, sc = 3.45f;
        // shadow on the ground: it shrinks and fades as the die rises
        float sh = 1f - hh / 6f;
        for (int y = 12; y <= 15; y++) for (int x = 0; x < 16; x++) { float dx = (x + 0.5f - 8) / (6.4f * sh), dy = (y + 0.5f - 13.8f) / (1.7f * sh); if (dx * dx + dy * dy <= 1f) pxa(x, y, 0x8C93AE, 0.42f * sh); }
        // rotation: a turn about a slanted axis (shrinking to nothing as it lands), then the fixed view from above
        double[] ax = {0.62, 0.70, 0.35}; double n = Math.sqrt(ax[0] * ax[0] + ax[1] * ax[1] + ax[2] * ax[2]); ax[0] /= n; ax[1] /= n; ax[2] /= n;
        double ca = Math.cos(ang), sa = Math.sin(ang), ic = 1 - ca;
        double[][] r1 = {{ca + ax[0] * ax[0] * ic, ax[0] * ax[1] * ic - ax[2] * sa, ax[0] * ax[2] * ic + ax[1] * sa}, {ax[1] * ax[0] * ic + ax[2] * sa, ca + ax[1] * ax[1] * ic, ax[1] * ax[2] * ic - ax[0] * sa}, {ax[2] * ax[0] * ic - ax[1] * sa, ax[2] * ax[1] * ic + ax[0] * sa, ca + ax[2] * ax[2] * ic}};
        double ty = 0.7854, tx = 0.6155;                                                      // yaw, then tilt
        double[][] ry = {{Math.cos(ty), 0, Math.sin(ty)}, {0, 1, 0}, {-Math.sin(ty), 0, Math.cos(ty)}}, rx = {{1, 0, 0}, {0, Math.cos(tx), -Math.sin(tx)}, {0, Math.sin(tx), Math.cos(tx)}};
        double[][] m = mul(rx, mul(ry, r1));
        int[][] pips = {{}, {0, 0}, {-1, -1, 1, 1}, {-1, -1, 0, 0, 1, 1}, {-1, -1, 1, -1, -1, 1, 1, 1}, {-1, -1, 1, -1, 0, 0, -1, 1, 1, 1}, {-1, -1, -1, 0, -1, 1, 1, -1, 1, 0, 1, 1}};
        int[][] cnt = {{3, 4}, {6, 1}, {5, 2}};                                          // faces +X/-X, +Y/-Y, +Z/-Z
        boolean[][] mask = new boolean[16][16];
        double[] L = {-0.45, 0.80, 0.40};
        // draw the visible faces
        for (int a = 0; a < 3; a++) for (int sg = 1; sg >= -1; sg -= 2) {
            double[] nrm = new double[3]; nrm[a] = sg; double[] nt = apply(m, nrm);
            if (nt[2] <= 0.02) continue;
            int b = (a + 1) % 3, c2 = (a + 2) % 3;
            float[] xs = new float[4], ys = new float[4]; int[][] cs = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
            for (int q = 0; q < 4; q++) { double[] v = new double[3]; v[a] = sg; v[b] = cs[q][0]; v[c2] = cs[q][1]; double[] w = apply(m, v); xs[q] = (float) (cx + w[0] * sc); ys[q] = (float) (cy - w[1] * sc); }
            double lit = Math.max(0, nt[0] * L[0] + nt[1] * L[1] + nt[2] * L[2]);
            int face = lit > 0.55 ? 0xFFFFFF : lit > 0.2 ? 0xDADEEE : 0xB0B6CE;                  // three flat tones, so the cube reads at this size
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (inside(xs, ys, x + 0.5f, y + 0.5f)) { px(x, y, face); mask[y][x] = true; }
            double area = 0; for (int q = 0; q < 4; q++) area += xs[q] * ys[(q + 1) % 4] - xs[(q + 1) % 4] * ys[q]; area = Math.abs(area) / 2;
            if (area < 9) continue;                                                           // a face seen edge-on shows no pips
            int cn = cnt[a][sg > 0 ? 0 : 1], pc = cn == 1 ? 0xD02030 : 0x16171F;
            int[] pl = pips[cn];
            for (int pi = 0; pi + 1 < pl.length; pi += 2) {
                double[] v = new double[3]; v[a] = sg; v[b] = pl[pi] * 0.5; v[c2] = pl[pi + 1] * 0.5; double[] w = apply(m, v);
                int px0 = (int) Math.floor(cx + w[0] * sc), py0 = (int) Math.floor(cy - w[1] * sc);
                px(px0, py0, pc);
                if (cn == 1 && area > 18) { px(px0 + 1, py0, pc); px(px0, py0 + 1, pc); }
            }
        }
        // the grey-blue outline
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (!mask[y][x]) {
            boolean nb = (x > 0 && mask[y][x - 1]) || (x < 15 && mask[y][x + 1]) || (y > 0 && mask[y - 1][x]) || (y < 15 && mask[y + 1][x]);
            if (nb) px(x, y, 0x7B83A3);
        }
        if (f >= 0.62f && f < 0.9f) glint(11, 3, 2, 0xFFFFFF, 1f - (f - 0.62f) / 0.28f);       // a little shine as it settles
    }
    private static double[][] mul(double[][] a, double[][] b) { double[][] r = new double[3][3]; for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) for (int k = 0; k < 3; k++) r[i][j] += a[i][k] * b[k][j]; return r; }
    private static double[] apply(double[][] m, double[] v) { return new double[]{m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2], m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2], m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]}; }

    private static boolean fx(int i, float f) {
        float s = sn(f), t = tri(f);
        bev(0, 0, 16, 16, 0x1B2236);
        switch (i) {
            case 0 -> { bigA(5, 4, 0xFFD25A); for (int a = 0; a < 8; a++) { float ang = a * 0.785f; pxa(Math.round(7.5f + (float) Math.cos(ang) * (5 + 2.5f * t)), Math.round(7.5f + (float) Math.sin(ang) * (5 + 2.5f * t)), 0xFFE9A8, 1f - t); } }
            case 1 -> { bigA(5, 4, 0xB8C4E8); int sx = Math.round(f * 18) - 3; for (int y = 3; y < 12; y++) pxa(sx + (11 - y) / 2, y, 0xFFFFFF, 0.85f); }
            case 2 -> { for (int k = 0; k < 3; k++) letter(new String[]{".#.", "#.#", "###", "#.#"}, 1 + k * 5, 6 + Math.round((float) Math.sin(f * TAU + k * 1.3f) * 2f), 0x7FE0D0, true); }
            case 3 -> bigA(5, 4, (x, y) -> hue(f + y * 0.1f + x * 0.06f, 0.7f, 1f));
            case 4 -> { int dy = Math.round(t * 5); bigA(5, 1 + dy, 0x8FD0FF); for (int k = 0; k < 3; k++) pxa(6 + k * 2, 0 + dy / 2, 0x8FD0FF, 0.5f - k * 0.1f); }
            case 5 -> { bigA(5, 4, 0xFF9AC8); int r = 6 + Math.round(t * 2); for (int a = 0; a < 28; a++) { float ang = a * TAU / 28; pxa(Math.round(7.5f + (float) Math.cos(ang) * r), Math.round(7.5f + (float) Math.sin(ang) * r), 0xFF9AC8, 1f - t); } }
            case 6 -> { letter(BIG_A, 4, 4, 0x55D8FF, false); letter(BIG_A, 6, 4, 0xFF72C6, false); letter(BIG_A, 5, 4, 0xFFFFFF, false); if (f > 0.5f) rect(2, 8, 12, 1, 0x1B2236); }
            case 7 -> { glow(8, 8, 8, 0xFFE9A8, 0.35f + 0.3f * t); bigA(5, 4, 0xFFF3C8); }
            case 8 -> { int dy = Math.round(Math.abs((float) Math.cos(f * 3.14f)) * -6f) + 6; bigA(5, 1 + dy, 0xFF9A5A); rect(4, 14, 8, 1, 0x4A5470); }
            case 9 -> { bigA(3, 4, 0x9AF0A8); if (f < 0.5f) rect(10, 4, 2, 7, 0xFFFFFF); }
            case 10 -> { int dy = Math.round(s * 2f); bigA(5, 4 + dy, 0xBFD0FF); px(2, 11 - dy, 0x7F92F0); px(13, 12 + dy, 0x7F92F0); px(13, 3 + dy, 0x7F92F0); }
            case 11 -> { int dx = Math.round(sn(f * 4f) * 1.4f); bigA(5 + dx, 4, 0xFF8A8A); px(2, 6, 0xFF8A8A); px(13, 9, 0xFF8A8A); px(2, 11, 0xFF8A8A); }
            case 12 -> { bigA(5, 4, (x, y) -> mix(0xFFD84A, 0xFF4A1C, y / 6f)); for (int k = 0; k < 4; k++) { float u = (f + k / 4f) % 1f; pxa(5 + k * 1, Math.round(4 - u * 4), mix(0xFFD84A, 0xFF4A1C, u), 1f - u); } }
            case 13 -> { bigA(5, 4, 0x9FE8FF); px(2, 3, 0xFFFFFF); px(13, 12, 0xFFFFFF); if (f > 0.5f) { px(1, 3, 0xBFF4FF); px(3, 3, 0xBFF4FF); px(2, 2, 0xBFF4FF); px(2, 4, 0xBFF4FF); } }
            case 14 -> { bigA(5, 4, 0xFFE9A8); glint(3, 3, 2, 0xFFFFFF, t); glint(13, 12, 1, 0xFFFFFF, 1f - t); }
            case 15 -> bigA(5, 4, (x, y) -> mix(0xE8EEFF, 0x2A3350, ((x + y) % 3) / 2f * t));
            case 16 -> { int dy = Math.round(t * -4f); bigA(5, 6 + dy, 0xB8FFD0); for (int k = 0; k < 3; k++) pxa(6 + k, 13 + 0, 0xB8FFD0, 0.45f - k * 0.1f); }
            case 17 -> { if (t < 0.5f) bigA(5, 4, 0xFFB8D8); else letter(new String[]{".#.", "#.#", "###", "#.#"}, 6, 8, 0xFFB8D8, true); rect(3, 14, 10, 1, 0x4A5470); }
            case 18 -> { float k = t * 3.2f; for (int y = 0; y < 7; y++) for (int x = 0; x < 5; x++) if (BIG_A[y].charAt(x) == '#') px(Math.round(5 + x + (x - 2) * k), Math.round(4 + y + (y - 3) * k * 0.6f), 0xFFC857); }
            case 19 -> { int dx = Math.round((1f - t) * -6f); bigA(5 + dx, 4, 0x9AD8FF); for (int k = 1; k < 4; k++) pxa(5 + dx - k * 2, 7, 0x9AD8FF, 0.5f / k); }
            case 20 -> { float w = Math.max(0.2f, Math.abs((float) Math.cos(f * 3.14f))); for (int y = 0; y < 7; y++) for (int x = 0; x < 5; x++) if (BIG_A[y].charAt(x) == '#') px(Math.round(7.5f + (x - 2) * w), 4 + y, 0xE0B8FF); }
            case 21 -> { int sh = Math.round(s * 2f); for (int y = 0; y < 7; y++) for (int x = 0; x < 5; x++) if (BIG_A[y].charAt(x) == '#') px(5 + x + Math.round(sh * (y - 3) / 3f), 4 + y, 0xFFD0A0); ln(3, 2, 12, 2, 0x7F6A4A); }
            case 22 -> bigA(5, 4, (x, y) -> mix(0xE0A21B, 0xFFF0A0, 0.5f + 0.5f * (float) Math.sin(f * TAU - (x + y) * 0.8f)));
            case 23 -> { int dx = Math.round(t * 6f); bigA(5 + dx - 3, 4, 0xFFA8A8); for (int k = 1; k < 4; k++) pxa(2 + dx - k * 2 + 3, 7, 0xFFA8A8, 0.5f / k); }
            case 24 -> { letter(BIG_A, 5, 2, 0xA8F0A8, true); for (int k = 0; k < 5; k += 2) { int len = 2 + Math.round(t * (2 + k)); for (int j = 0; j < len; j++) pxa(5 + k, 9 + j, 0xA8F0A8, 1f - j * 0.15f); } }
            default -> { return false; }
        }
        outline();
        return true;
    }
}
