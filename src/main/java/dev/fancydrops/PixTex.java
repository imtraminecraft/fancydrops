package dev.fancydrops;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Draws a 16 x 16 pixel picture as ONE textured quad instead of a rectangle fill for every run of colour (up to ~100 draw calls per icon).
 * Each picture (an int[256], told apart by identity) gets a small texture the first time it is drawn. If a texture can't be made the caller falls back to fills.
 */
final class PixTex {
    private PixTex() {}
    private static final class Entry { DynamicTexture tex; Identifier id; }
    private static final Map<int[], Entry> MAP = new IdentityHashMap<>();
    private static int seq; private static boolean failed;
    private static final int CAP = 2500;

    /** Draws pix (16 x 16 ARGB) at x, y, w x h in the current matrix, multiplied by color (0xAARRGGBB). false = not available. */
    static boolean draw(GuiGraphicsExtractor g, int[] pix, int x, int y, int w, int h, int color) {
        if (failed || pix == null || pix.length != 256) return false;
        try {
            Entry e = MAP.get(pix);
            if (e == null) {
                if (MAP.size() >= CAP) return false;                                  // (a very long session: the rest are drawn with fills)
                e = new Entry();
                e.id = Identifier.fromNamespaceAndPath("fancydrops", "pix/" + (seq++));
                e.tex = new DynamicTexture(() -> "fancydrops pix", 16, 16, true);
                NativeImage img = e.tex.getPixels();
                if (img == null) { failed = true; return false; }
                for (int yy = 0; yy < 16; yy++) for (int xx = 0; xx < 16; xx++) {
                    int p = pix[yy * 16 + xx];
                    img.setPixelABGR(xx, yy, (p & 0xFF00FF00) | ((p >> 16) & 0xFF) | ((p & 0xFF) << 16));      // ARGB -> ABGR
                }
                e.tex.upload();
                Minecraft.getInstance().getTextureManager().register(e.id, e.tex);
                MAP.put(pix, e);
            }
            g.blit(RenderPipelines.GUI_TEXTURED, e.id, x, y, 0f, 0f, w, h, 16, 16, 16, 16, color);
            return true;
        } catch (Throwable t) { failed = true; t.printStackTrace(); return false; }
    }
}
