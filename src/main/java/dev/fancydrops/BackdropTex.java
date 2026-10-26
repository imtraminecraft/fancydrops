package dev.fancydrops;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** The little landscape behind the menu's animation previews, painted once into a texture (it used to be hundreds of rectangle fills every frame). */
final class BackdropTex {
    private static final int W = 192, H = 108;
    private static final Identifier[] IDS = {Identifier.fromNamespaceAndPath("fancydrops", "backdrop_a"), Identifier.fromNamespaceAndPath("fancydrops", "backdrop_b")};
    private static final DynamicTexture[] TEX = new DynamicTexture[2];
    private static boolean failed;

    private static void paint(Canvas c, boolean blur) {
        int w = W, h = H, hor = h * 56 / 100;
        for (int j = 0; j < hor; j += 2) {
            float f = j / (float) Math.max(1, hor);
            int col = f < 0.5f ? Canvas.mix(0xFF141C3A, 0xFF5B4B88, f * 2f) : Canvas.mix(0xFF5B4B88, 0xFFE58F62, (f - 0.5f) * 2f);
            c.rect(0, j, w, Math.min(hor, j + 2), col);
        }
        if (!blur) for (int i = 0; i < 26; i++) { int sx = (i * 97 + 13) % w, sy = (i * 53 + 7) % Math.max(1, hor / 2); c.rect(sx, sy, sx + 1, sy + 1, 0xCCFFFFFF); }
        int sunX = w * 72 / 100, sunY = hor - h / 14, sr = Math.max(4, h / 9);
        for (int j = -sr; j <= sr; j++) { int half = (int) Math.sqrt(Math.max(0, sr * sr - j * j)); if (sunY + j < hor) c.rect(sunX - half, sunY + j, sunX + half, sunY + j + 1, blur ? 0x66FFD9A0 : 0xFFFFD9A0); }
        for (int i = 0; i < w; i++) {
            int h1 = (int) (h * 0.10 + Math.sin(i * 0.045 + 1.0) * h * 0.05 + Math.sin(i * 0.11) * h * 0.02);
            c.rect(i, hor - h1, i + 1, hor, blur ? 0xFF3A3461 : 0xFF2C2850);
            int h2 = (int) (h * 0.05 + Math.sin(i * 0.07 + 3.0) * h * 0.03);
            c.rect(i, hor - h2, i + 1, hor + 1, blur ? 0xFF29433A : 0xFF1D3A2E);
        }
        for (int j = hor; j < h; j += 2) c.rect(0, j, w, Math.min(h, j + 2), Canvas.mix(0xFF2A5230, 0xFF14281A, (j - hor) / (float) Math.max(1, h - hor)));
        if (!blur) {
            for (int i = 0; i < 7; i++) {
                int tx = (i * 61 + 20) % Math.max(1, w - 10) + 4, ty = hor + 1 + (i % 3) * (h / 14), th = Math.max(8, h / 6);
                c.rect(tx, ty - 1, tx + 2, ty + 2, 0xFF3B2A1A);
                for (int k = 0; k < 4; k++) c.rect(tx - 3 + k, ty - 2 - k * th / 5, tx + 5 - k, ty - 1 - k * th / 5, k % 2 == 0 ? 0xFF1E4A2C : 0xFF245A34);
            }
            for (int i = 0; i < 24; i++) { int gx = (i * 37 + 5) % w, gy = hor + 3 + (i * 19) % Math.max(1, h - hor - 4); c.rect(gx, gy, gx + 1, gy + 2, 0x663E7A44); }
            c.rect(0, hor, w, hor + 1, 0x40FFFFFF);
        } else {
            c.rect(0, 0, w, h, 0x22C8D6FF);
        }
    }

    /** Draws the landscape into the box; false if no texture could be made (the caller then draws it with fills). */
    static boolean draw(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean blur) {
        if (failed) return false;
        int k = blur ? 1 : 0;
        try {
            if (TEX[k] == null) {
                Canvas c = new Canvas(W, H); paint(c, blur);
                DynamicTexture t = new DynamicTexture(() -> "fancydrops backdrop", W, H, true);
                NativeImage img = t.getPixels(); if (img == null) { failed = true; return false; }
                for (int yy = 0; yy < H; yy++) for (int xx = 0; xx < W; xx++) { int p = c.px[yy * W + xx]; img.setPixelABGR(xx, yy, (p & 0xFF00FF00) | ((p >> 16) & 0xFF) | ((p & 0xFF) << 16)); }
                t.upload();
                Minecraft.getInstance().getTextureManager().register(IDS[k], t); TEX[k] = t;
            }
            g.blit(RenderPipelines.GUI_TEXTURED, IDS[k], x, y, 0f, 0f, w, h, W, H, W, H, 0xFFFFFFFF);
            return true;
        } catch (Throwable e) { failed = true; e.printStackTrace(); return false; }
    }
    private BackdropTex() {}
}
