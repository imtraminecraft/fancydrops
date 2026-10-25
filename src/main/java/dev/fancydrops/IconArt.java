package dev.fancydrops;

/**
 * The pictures of every ":name:" icon, drawn in code as 16 x 16 pixel art with depth: a light from the top left (bright top and left
 * edges, darker bottom and right), a deep-coloured outline, and a few frames of animation. Pure code, no Minecraft classes, so the
 * whole set can be rendered anywhere. render() returns 256 ARGB pixels, or null when the name has no picture.
 */
final class IconArt {
    private IconArt() {}

    static final int FRAMES = 8;
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
            case "dropped" -> {                                                              // a treasure chest, the lid lifting, a gleam rising
                float lift = t * 2.2f;
                int ly = Math.round(5 - lift);
                bev(2, 8, 12, 6, 0x9A5E2C);
                rect(2, 10, 12, 1, 0x6B3F1C);
                bev(7, 8, 2, 4, 0xFFD25A); px(7, 10, 0x8F6A12);
                quad(2, 8, 3, ly, 13, ly, 14, 8, 0xB5733A);
                rect(7, ly, 2, 8 - ly, 0xFFD25A);
                if (lift > 0.8f) { rect(3, 8, 10, 1, 0xFFF3B0); }
                outline();
                glow(8, 8 - lift, 5 + lift, 0xFFE27A, 0.25f * t);
                if (t > 0.35f) glint(8, 5 - Math.round(t * 3), 2, 0xFFF3B0, t);
            }
            case "animations" -> {                                                           // a clapperboard clapping
                float cl = f < 0.5f ? 0f : (f < 0.75f ? (f - 0.5f) / 0.25f : 1f - (f - 0.75f) / 0.25f);
                bev(2, 7, 12, 7, 0x2B3350);
                rect(3, 10, 4, 1, 0x7F8AB0); rect(3, 12, 7, 1, 0x7F8AB0);
                float lift = 3.5f * (1f - cl) * (f < 0.5f ? 0.3f + 0.7f * (f / 0.5f) : 1f);
                float yb = 6 - lift;
                quad(2, 7, 2 + lift * 0.3f, yb - 3, 14 + lift * 0.3f, yb - 3, 14, 7, 0xF4F4F8);
                for (int i = 0; i < 4; i++) { float x0 = 3 + i * 3; quad(x0, 7, x0 + 1 + lift * 0.3f, yb - 3, x0 + 2.2f + lift * 0.3f, yb - 3, x0 + 1.4f, 7, 0x2B3350); }
                outline();
                if (cl > 0.8f) glint(13, 4, 2, 0xFFFFFF, 1f);
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
            case "treasure" -> {                                                             // a spade in the dirt, gold glinting
                fillMound();
                ln(11, 1, 6, 9, 0x8A5A2C); ln(12, 1, 7, 9, 0x6B4423);
                tri(3.5f, 9, 9, 8, 6, 14, 0xB0BAD0);
                rect(10, 1, 3, 1, 0x3A2410);
                outline();
                px(3, 12, 0xFFD25A); px(4, 12, 0xFFF3B0); px(13, 12, 0xFFD25A);
                glint(3 + Math.round(t * 9), 11, 2, 0xFFF3B0, 0.4f + 0.6f * t);
            }
            case "slayers" -> {                                                              // a sword, a blood-red glow
                glow(8, 8, 8, 0xFF3A3A, 0.22f + 0.1f * t);
                quad(11, 1, 14, 1, 14, 4, 5.5f, 11, 0xD8DEEA);
                quad(11, 1, 14, 4, 13.2f, 4.8f, 10.2f, 1.8f, 0xFFFFFF);
                quad(4, 9, 7, 12, 5.5f, 13.5f, 2.5f, 10.5f, 0xE0A02A);
                ln(2, 14, 4.5f, 11.5f, 0x7A4A24); ln(3, 14, 5.5f, 11.5f, 0x5A3418);
                outline();
                glint(13, 2, 2, 0xFFFFFF, 0.3f + 0.7f * t);
            }
            case "farm" -> {                                                                 // wheat swaying in the wind
                for (int i = 0; i < 3; i++) {
                    float sw = s * 0.8f * (i == 1 ? 1f : 0.6f), bx = 4 + i * 4;
                    ln(bx, 14, bx + sw, 6, 0x6F9A3A);
                    for (int k = 0; k < 4; k++) { float y = 2 + k * 1.8f + (i == 1 ? 0 : 2); float x = bx + sw * (14 - y) / 8f; px(Math.round(x - 1), Math.round(y + 1), 0xE8C040); px(Math.round(x + 1), Math.round(y + 1), 0xC89A28); px(Math.round(x), Math.round(y), 0xFFE070); }
                }
                outline();
            }
            case "experiments" -> {                                                          // a flask of something bubbling
                quad(6, 1, 10, 1, 10, 6, 6, 6, 0xD8E8F8);
                poly(new float[]{6, 10, 14, 2}, new float[]{6, 6, 14, 14}, 0xBFD6F0);
                poly(new float[]{4.5f, 11.5f, 14, 2}, new float[]{10, 10, 14, 14}, mix(0xB56BFF, 0xFF72C6, t));
                outline();
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; pxa(6 + i * 2 + Math.round((float) Math.sin(u * 5 + i)), Math.round(10 - u * 8), 0xF0E6FF, 1f - u * 0.6f); }
            }
            case "mining" -> {                                                               // a pickaxe swinging, sparks off the rock
                float sw = f < 0.6f ? f / 0.6f * -1.2f : -1.2f * (1f - (f - 0.6f) / 0.4f);
                ln(3 + sw, 14, 11 + sw, 5, 0x8A5A2C); ln(4 + sw, 14, 12 + sw, 5, 0x6B4423);
                quad(5 + sw, 3, 9, 2, 14.5f + sw, 5.5f, 11 + sw, 6, 0xBFC8DA);
                quad(5 + sw, 3, 9, 2, 9.5f, 3.2f, 6.5f + sw, 4.2f, 0xFFFFFF);
                rect(9, 13, 6, 2, 0x6A7388); rect(10, 12, 4, 1, 0x8A93A8);
                outline();
                if (f > 0.55f && f < 0.85f) { px(13, 11, 0xFFE27A); px(15, 12, 0xFFFFFF); px(12, 10, 0xFFB03A); }
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
            case "dye" -> {                                                                  // a dye drop that cycles through colours
                int col = hue(f, 0.7f, 1f);
                poly(new float[]{8, 12.5f, 13, 12, 8, 3, 3, 4, 3.5f}, new float[]{1, 7, 10.5f, 13.5f, 15, 13.5f, 10.5f, 7, 7}, col);
                poly(new float[]{8, 11, 8.5f, 5.5f}, new float[]{1.5f, 7.5f, 7, 5}, lite(col, 0.55f));
                outline();
                px(6, 8, 0xFFFFFF); px(6, 9, 0xFFFFFF);
            }
            case "nodrops" -> {                                                              // an empty box, a puff of dust
                bev(2, 8, 12, 6, 0x6A7388);
                rect(3, 9, 10, 2, 0x1B2030);
                quad(2, 8, 4, 5, 12, 5, 14, 8, 0x8A93A8);
                outline();
                for (int i = 0; i < 3; i++) { float u = (f + i / 3f) % 1f; pxa(5 + i * 3, Math.round(7 - u * 5), 0xB8C0D4, (1f - u) * 0.9f); }
            }
            case "normal" -> {                                                               // a cut gem with a light running over it
                poly(new float[]{4, 12, 15, 8, 1}, new float[]{3, 3, 7, 14.5f, 7}, 0x55D8FF);
                poly(new float[]{4, 8, 12, 8}, new float[]{3, 7, 3, 3}, lite(0x55D8FF, 0.5f));
                ln(1, 7, 15, 7, lite(0x55D8FF, 0.35f)); ln(4, 3, 8, 14, dark(0x55D8FF, 0.25f)); ln(12, 3, 8, 14, dark(0x55D8FF, 0.35f));
                outline();
                glint(3 + Math.round(f * 10), 5 + Math.round((float) Math.sin(f * 3.14f) * 2), 1, 0xFFFFFF, 1f);
            }
            case "dice" -> {                                                                 // a die that hops and shows a new face
                int hy = Math.round(-2f * t);
                poly(new float[]{8, 14, 8, 2}, new float[]{1 + hy, 4.5f + hy, 8 + hy, 4.5f + hy}, 0xFFD6EC);
                poly(new float[]{2, 8, 8, 2}, new float[]{4.5f + hy, 8 + hy, 15 + hy, 11.5f + hy}, 0xFF72C6);
                poly(new float[]{8, 14, 14, 8}, new float[]{8 + hy, 4.5f + hy, 11.5f + hy, 15 + hy}, dark(0xFF72C6, 0.3f));
                outline();
                px(7, 4 + hy, 0x7A1F50); px(9, 5 + hy, 0x7A1F50);
                px(4, 8 + hy, 0xFFFFFF); px(6, 11 + hy, 0xFFFFFF); px(11, 8 + hy, 0xFFE6F4); px(11, 11 + hy, 0xFFE6F4);
                if (hy < -1) glint(13, 2, 1, 0xFFFFFF, 1f);
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
            case "discord" -> {                                                              // community: two bubbles in conversation
                bev(1, 2, 9, 7, 0x7A8CF0); tri(3, 9, 3, 12, 6, 9, 0x7A8CF0);
                bev(6, 7, 9, 6, 0xB8C2FF); tri(12, 13, 13, 15.5f, 10, 13, 0xB8C2FF);
                rect(3, 4, 1, 1, 0xFFFFFF); rect(5, 4, 1, 1, 0xFFFFFF); rect(7, 4, 1, 1, 0xFFFFFF);
                outline();
                int dy = Math.round(Math.max(0f, s) * 1f); rect(8, 9 - dy, 1, 1, 0x4A5AD8); rect(10, 9, 1, 1, 0x4A5AD8); rect(12, 9 - Math.round(Math.max(0f, -s)), 1, 1, 0x4A5AD8);
            }
            case "patreon" -> {                                                              // support: a heart that beats
                float k = 1f + 0.12f * t;
                poly(new float[]{8, 14, 15, 8 + 0, 1, 2}, new float[]{3.2f, 1.5f, 6, 15, 6, 1.5f}, 0xFF6A5A);
                ball(4.6f, 5, 3.4f * k, 0xFF6A5A); ball(11.4f, 5, 3.4f * k, 0xFF6A5A);
                tri(1.4f, 6.2f, 14.6f, 6.2f, 8, 14.5f, 0xE04A4A);
                outline();
                px(4, 3, 0xFFFFFF);
            }
            case "github" -> {                                                               // source: a branching tree
                ln(5, 3, 5, 13, 0xC8D0E4); ln(6, 3, 6, 13, 0x9AA4C0);
                ln(5, 8, 11, 4, 0xC8D0E4); ln(6, 8, 12, 4, 0x9AA4C0);
                ball(5.5f, 3, 2.2f, 0x62E6A8); ball(5.5f, 13, 2.2f, 0x62E6A8); ball(11.5f, 4, 2.2f, 0xFFC857);
                outline();
                glint(5, 3, 1, 0xFFFFFF, 0.3f + 0.7f * t);
            }
            default -> {
                if (n.startsWith("cx_")) return generic(n, f);
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
    private static void bigA(int x, int y, int rgb) { letter(BIG_A, x, y, rgb, true); }
    /** The big A with a colour per pixel: col(x, y) gets the pixel's place in the letter. */
    private interface Tint { int at(int x, int y); }
    private static void bigA(int x, int y, Tint t) { for (int j = 0; j < 7; j++) for (int i = 0; i < 5; i++) if (BIG_A[j].charAt(i) == '#') px(x + i, y + j, t.at(i, j)); }

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
