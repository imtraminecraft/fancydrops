package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.UUID;

/** The player's own skin as a flat front-view figure for the aura preview. The skin is fetched from Mojang's session server once; until then (or offline) a plain figure is drawn. */
final class SkinMock {
    private SkinMock() {}
    private static volatile int[] px;                 // 64 x 64 ARGB
    private static boolean started;

    static void ensure() {
        if (started) return; started = true;
        UUID id = Cosmetics.me(); if (id == null) return;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                var http = java.net.http.HttpClient.newHttpClient();
                String body = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + id.toString().replace("-", ""))).header("User-Agent", "FancyDrops").GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString()).body();
                var props = com.google.gson.JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("properties");
                String val = props.get(0).getAsJsonObject().get("value").getAsString();
                var tex = com.google.gson.JsonParser.parseString(new String(java.util.Base64.getDecoder().decode(val))).getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN");
                byte[] png = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(tex.get("url").getAsString())).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofByteArray()).body();
                var img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
                int[] o = new int[64 * 64];
                for (int y = 0; y < Math.min(64, img.getHeight()); y++) for (int x = 0; x < 64; x++) o[y * 64 + x] = img.getRGB(x, y);
                if (img.getHeight() == 32) for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { o[(48 + y) * 64 + 16 + x] = o[(16 + y) * 64 + x]; o[(48 + y) * 64 + 32 + x] = o[(16 + y) * 64 + 40 + x]; }   // old skins: mirror the limbs
                px = o;
            } catch (Throwable t) { started = false; }
        });
    }

    private static int at(int x, int y) { int[] p = px; return p == null ? 0 : p[y * 64 + x]; }

    /** One body part: w x h skin pixels from (u, v), drawn s times bigger at (x, y); an optional overlay layer (hat, sleeves...) goes over it. */
    private static void part(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h, int ou, int ov, int s, int fallback) {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            int c = px == null ? fallback : at(u + i, v + j);
            if (px != null && ou >= 0) { int o = at(ou + i, ov + j); if ((o >>> 24) > 128) c = o; }
            if ((c >>> 24) < 10) continue;
            g.fill(x + i * s, y + j * s, x + (i + 1) * s, y + (j + 1) * s, c | 0xFF000000);
        }
    }

    /** The figure's feet stand at (cx, feetY); s = screen pixels per skin pixel (a player is 32 skin pixels tall). */
    static void draw(GuiGraphicsExtractor g, int cx, int feetY, int s) {
        ensure();
        int top = feetY - 32 * s;
        part(g, cx - 4 * s, top, 8, 8, 8, 8, 40, 8, s, 0xFFC99A6B);                      // head + hat
        part(g, cx - 4 * s, top + 8 * s, 20, 20, 8, 12, 20, 36, s, 0xFF2F6BFF);          // body
        part(g, cx - 8 * s, top + 8 * s, 44, 20, 4, 12, 44, 36, s, 0xFFC99A6B);          // right arm
        part(g, cx + 4 * s, top + 8 * s, 36, 52, 4, 12, 52, 52, s, 0xFFC99A6B);           // left arm
        part(g, cx - 4 * s, top + 20 * s, 4, 20, 4, 12, 4, 36, s, 0xFF3A3F8F);           // right leg
        part(g, cx, top + 20 * s, 20, 52, 4, 12, 4, 52, s, 0xFF3A3F8F);                   // left leg
    }
}
