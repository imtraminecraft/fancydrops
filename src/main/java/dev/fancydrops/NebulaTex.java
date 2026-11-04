package dev.fancydrops;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * The deep sky behind the achievements: painted once into a texture. Layered clouds of violet, teal and rose nebula, a Milky Way band
 * across the middle with dark dust lanes, and a thousand faint stars. Drawing it is then a single picture, whatever happens on top.
 */
final class NebulaTex {
    private NebulaTex() {}

    static final int W = 640, H = 360;
    private static final Identifier ID = Identifier.fromNamespaceAndPath("fancydrops", "achievement_sky");
    private static boolean failed, made;
    private static Canvas canvas;
    /** Tests draw the canvas themselves (no game). */
    static java.util.function.Consumer<int[]> testDraw;

    private static float hash(int x, int y) { int h = x * 374761393 + y * 668265263; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) & 0xFFFFFF) / 16777216f; }
    private static float noise(float x, float y) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y); float fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
        float a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }
    private static float fbm(float x, float y, int oct) { float s = 0, amp = 0.5f, f = 1; for (int i = 0; i < oct; i++) { s += amp * noise(x * f, y * f); f *= 2.03f; amp *= 0.5f; } return s; }
    private static int add(int base, int rgb, float k) {
        k = Math.max(0f, Math.min(1f, k));
        int r = Math.min(255, ((base >> 16) & 255) + Math.round(((rgb >> 16) & 255) * k)), g = Math.min(255, ((base >> 8) & 255) + Math.round(((rgb >> 8) & 255) * k)), b = Math.min(255, (base & 255) + Math.round((rgb & 255) * k));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** The picture itself. */
    static Canvas canvas() {
        if (canvas != null) return canvas;
        Canvas c = new Canvas(W, H);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            float u = x / (float) W, v = y / (float) H;
            int col = Canvas.mix(0xFF060818, 0xFF140A26, v * 0.8f + 0.2f * u);                    // deep space: navy above, plum below
            float n1 = fbm(x / 120f, y / 120f, 5), n2 = fbm(x / 70f + 40, y / 70f + 17, 5), n3 = fbm(x / 160f + 90, y / 160f + 3, 4);
            col = add(col, 0x6A3AC8, Math.max(0, n1 - 0.48f) * 1.1f);                             // violet clouds
            col = add(col, 0x1E8AB0, Math.max(0, n2 - 0.52f) * 1.0f);                             // teal clouds
            col = add(col, 0xC83A8A, Math.max(0, n3 - 0.55f) * 0.9f);                             // rose clouds
            float band = 1f - Math.abs((v - 0.62f + (u - 0.5f) * 0.55f)) / 0.16f;                // the Milky Way runs corner to corner
            if (band > 0f) {
                float b = band * band * (0.6f + 0.6f * fbm(x / 40f + 7, y / 40f, 4));
                float dust = Math.max(0, fbm(x / 22f + 3, y / 22f + 11, 4) - 0.55f) * 2.2f;      // dark dust lanes through it
                col = add(col, 0x8A7AB8, b * 0.35f * (1f - Math.min(1f, dust)));
            }
            c.px[y * W + x] = col;
        }
        for (int i = 0; i < 1400; i++) {                                                         // stars: many faint, a few bright, more inside the band
            float u = hash(i, 1), v = hash(i, 2);
            float band = 1f - Math.abs((v - 0.62f + (u - 0.5f) * 0.55f)) / 0.16f;
            if (band < 0 && hash(i, 3) < 0.45f) continue;
            int x = Math.round(u * (W - 1)), y = Math.round(v * (H - 1)); float br = hash(i, 4); br = br * br * br;
            int sc = hash(i, 5) < 0.15f ? 0xFFE0B0 : hash(i, 5) < 0.3f ? 0xB0C8FF : 0xFFFFFF;
            c.set(x, y, Canvas.rgb(Math.round(60 + 195 * br), sc));
            if (br > 0.55f) { int a = Math.round(90 * br); c.set(x + 1, y, Canvas.rgb(a, sc)); c.set(x - 1, y, Canvas.rgb(a, sc)); c.set(x, y + 1, Canvas.rgb(a, sc)); c.set(x, y - 1, Canvas.rgb(a, sc)); }
        }
        return canvas = c;
    }

    /** Draws the sky into x, y, w, h (the picture is stretched). False when no texture could be made. */
    static boolean draw(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (testDraw != null) { canvas(); testDraw.accept(new int[]{x, y, w, h}); return true; }
        if (failed) return false;
        try {
            if (!made) {
                Canvas c = canvas();
                DynamicTexture t = new DynamicTexture(() -> "fancydrops achievement sky", W, H, true);
                NativeImage img = t.getPixels(); if (img == null) { failed = true; return false; }
                for (int yy = 0; yy < H; yy++) for (int xx = 0; xx < W; xx++) { int p = c.px[yy * W + xx]; img.setPixelABGR(xx, yy, (p & 0xFF00FF00) | ((p >> 16) & 0xFF) | ((p & 0xFF) << 16)); }
                t.upload();
                Minecraft.getInstance().getTextureManager().register(ID, t); made = true;
            }
            g.blit(RenderPipelines.GUI_TEXTURED, ID, x, y, 0f, 0f, w, h, W, H, W, H, 0xFFFFFFFF);
            return true;
        } catch (Throwable e) { failed = true; return false; }
    }
}
