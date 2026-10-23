package dev.fancydrops;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Puts the scene canvas on screen as ONE textured quad instead of a couple of thousand rectangle fills.
 * (The fills were what made the cool and epic scenes drop frames.) If the texture can't be created the caller falls back to fills.
 */
final class SceneTexture {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("fancydrops", "scene");
    private static DynamicTexture tex;
    private static boolean failed;

    /** Copies the canvas into the texture. Call only when the canvas changed. */
    static void upload(Canvas c) {
        if (failed) return;
        try {
            if (tex == null) {
                tex = new DynamicTexture(() -> "fancydrops scene", c.w, c.h, true);
                Minecraft.getInstance().getTextureManager().register(ID, tex);
            }
            NativeImage img = tex.getPixels();
            if (img == null) { failed = true; return; }
            for (int y = 0; y < c.h; y++) for (int x = 0; x < c.w; x++) {
                int p = c.px[y * c.w + x];
                img.setPixelABGR(x, y, (p & 0xFF00FF00) | ((p >> 16) & 0xFF) | ((p & 0xFF) << 16));   // ARGB -> ABGR
            }
            tex.upload();
        } catch (Throwable e) { failed = true; e.printStackTrace(); }
    }

    /** Draws the texture; false if it isn't available (use Canvas.flush instead). */
    static boolean draw(GuiGraphicsExtractor g, Canvas c, int x, int y, int w, int h, float alpha) {
        if (failed || tex == null) return false;
        try {
            int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
            g.blit(RenderPipelines.GUI_TEXTURED, ID, x, y, 0f, 0f, w, h, c.w, c.h, c.w, c.h, (a << 24) | 0xFFFFFF);
            return true;
        } catch (Throwable e) { failed = true; e.printStackTrace(); return false; }
    }

    private SceneTexture() {}
}
