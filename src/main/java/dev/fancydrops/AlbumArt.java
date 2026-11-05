package dev.fancydrops;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** The playing song's album cover as a texture (64 px), and its average colour (the Now Playing card takes its tint from it). */
final class AlbumArt {
    private AlbumArt() {}

    static final Identifier ID = Identifier.fromNamespaceAndPath("fancydrops", "album_art");
    static volatile boolean ready; static volatile int colour = 0x1ED760;
    private static volatile String forUrl = "";
    private static DynamicTexture tex;

    static void load(String url) {
        if (url == null || url.isEmpty() || url.equals(forUrl)) return;
        forUrl = url; ready = false;
        Thread t = new Thread(() -> {
            try {
                byte[] b = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "FancyDrops").GET().build(), HttpResponse.BodyHandlers.ofByteArray()).body();
                java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(b));      // album pictures are JPEG: read with Java, then copy into a texture
                if (src == null || !url.equals(forUrl)) return;
                int S = 64; java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(S, S, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                var g2 = img.createGraphics(); g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR); g2.drawImage(src, 0, 0, S, S, null); g2.dispose();
                long r = 0, gg = 0, bb = 0;
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) { int p = img.getRGB(x, y); r += (p >> 16) & 255; gg += (p >> 8) & 255; bb += p & 255; }
                int n = S * S, avg = (int) (r / n) << 16 | (int) (gg / n) << 8 | (int) (bb / n);
                int[] px = new int[S * S]; img.getRGB(0, 0, S, S, px, 0, S);
                Minecraft.getInstance().execute(() -> {
                    try {
                        if (tex == null) { tex = new DynamicTexture(() -> "fancydrops album art", S, S, true); Minecraft.getInstance().getTextureManager().register(ID, tex); }
                        NativeImage ni = tex.getPixels(); if (ni == null) return;
                        for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) { int p = px[y * S + x]; ni.setPixelABGR(x, y, 0xFF000000 | (p & 0xFF00) | ((p >> 16) & 0xFF) | ((p & 0xFF) << 16)); }
                        tex.upload(); colour = brighten(avg); ready = true;
                    } catch (Throwable ignored) { }
                });
            } catch (Throwable ignored) { }
        }, "FancyDrops album art");
        t.setDaemon(true); t.start();
    }
    /** A cover's average is usually muddy: lift it so it reads as an accent. */
    private static int brighten(int c) {
        float[] hsb = java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
        return java.awt.Color.HSBtoRGB(hsb[0], Math.min(1f, hsb[1] * 1.3f + 0.15f), Math.max(0.75f, hsb[2])) & 0xFFFFFF;
    }
}
