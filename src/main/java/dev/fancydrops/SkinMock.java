package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
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

    // ---- a small 3D model: boxes with the skin on every face, turned and drawn texel by texel (back to front)
    private record Box(float cx, float cy, float cz, float hw, float hh, float hd, int u, int v, int fallback) {}
    private static Box[] boxes(float inflate, boolean overlay) {
        float g = overlay ? inflate : 0f;
        return new Box[]{
            new Box(0, 28, 0, 4 + g, 4 + g, 4 + g, overlay ? 32 : 0, 0, 0xFFC99A6B),                    // head
            new Box(0, 18, 0, 4 + g, 6 + g, 2 + g, 16, overlay ? 32 : 16, 0xFF2F6BFF),                   // body
            new Box(-6, 18, 0, 2 + g, 6 + g, 2 + g, 40, overlay ? 32 : 16, 0xFFC99A6B),                  // right arm (the screen's left)
            new Box(6, 18, 0, 2 + g, 6 + g, 2 + g, overlay ? 48 : 32, 48, 0xFFC99A6B),                   // left arm
            new Box(-2, 6, 0, 2 + g, 6 + g, 2 + g, 0, overlay ? 32 : 16, 0xFF3A3F8F),                    // right leg
            new Box(2, 6, 0, 2 + g, 6 + g, 2 + g, overlay ? 0 : 16, 48, 0xFF3A3F8F)};                    // left leg
    }
    private record Face(float depth, int x0, int y0, int x1, int y1, int argb) {}

    /** The figure's feet stand at (cx, feetY); s = screen pixels per skin pixel; yaw in radians (0 = facing you). */
    static void draw(GuiGraphicsExtractor g, int cx, int feetY, int s, float yaw) {
        ensure();
        double cy = Math.cos(yaw), sy = Math.sin(yaw), tilt = -0.18, ct = Math.cos(tilt), st = Math.sin(tilt);
        List<Face> out = new ArrayList<>();
        for (int layer = 0; layer < 2; layer++) {
            for (Box b : boxes(0.5f, layer == 1)) {
                float w = b.hw() * 2, h = b.hh() * 2, d = b.hd() * 2;
                int tw = Math.round(layer == 1 ? w - 1 : w), th = Math.round(layer == 1 ? h - 1 : h), td = Math.round(layer == 1 ? d - 1 : d);
                // face: normal, origin corner, U (image right), V (image down), uv rectangle, size in texels
                float[][] fs = {
                    {0, 0, 1,   -b.hw(), b.hh(), b.hd(),   1, 0, 0,   0, -1, 0,   b.u() + td, b.v() + td, tw, th},       // front
                    {0, 0, -1,   b.hw(), b.hh(), -b.hd(),  -1, 0, 0,  0, -1, 0,   b.u() + 2 * td + tw, b.v() + td, tw, th}, // back
                    {-1, 0, 0,   -b.hw(), b.hh(), -b.hd(), 0, 0, 1,   0, -1, 0,   b.u(), b.v() + td, td, th},          // player's right
                    {1, 0, 0,    b.hw(), b.hh(), b.hd(),   0, 0, -1,  0, -1, 0,   b.u() + td + tw, b.v() + td, td, th},  // player's left
                    {0, 1, 0,    -b.hw(), b.hh(), -b.hd(), 1, 0, 0,   0, 0, 1,    b.u() + td, b.v(), tw, td},            // top
                    {0, -1, 0,   -b.hw(), -b.hh(), b.hd(), 1, 0, 0,   0, 0, -1,   b.u() + td + tw, b.v(), tw, td}};      // bottom
                for (float[] f : fs) {
                    double nx = f[0] * cy + f[2] * sy, nz0 = -f[0] * sy + f[2] * cy;
                    double ny = f[1] * ct - nz0 * st, nz = f[1] * st + nz0 * ct;
                    if (nz <= 0.02) continue;                                                      // turned away
                    int nu = (int) f[14], nv = (int) f[15];
                    for (int j = 0; j < nv; j++) for (int i = 0; i < nu; i++) {
                        int c;
                        if (px == null) { if (layer == 1) continue; c = b.fallback(); }
                        else { c = at((int) f[12] + i, (int) f[13] + j); if ((c >>> 24) < 40) continue; }
                        double u = i + 0.5, v = j + 0.5;
                        double X = b.cx() + (f[3] - 0) + f[6] * u + f[9] * v, Y = b.cy() + f[4] + f[7] * u + f[10] * v, Z = b.cz() + f[5] + f[8] * u + f[11] * v;
                        double X1 = X * cy + Z * sy, Z1 = -X * sy + Z * cy;
                        double Y2 = Y * ct - Z1 * st, Z2 = Y * st + Z1 * ct;
                        double ux = f[6] * cy + f[8] * sy, vx = f[9] * cy + f[11] * sy;
                        double uy = f[7] * ct - (-f[6] * sy + f[8] * cy) * st, vy = f[10] * ct - (-f[9] * sy + f[11] * cy) * st;
                        double hx = (Math.abs(ux) + Math.abs(vx)) * s / 2 + 0.5, hy = (Math.abs(uy) + Math.abs(vy)) * s / 2 + 0.5;
                        int sx = (int) Math.round(cx + X1 * s), sy2 = (int) Math.round(feetY - Y2 * s);
                        float shade = (float) (0.62 + 0.38 * Math.max(0, nz) + 0.08 * ny);
                        int r = Math.min(255, Math.round(((c >> 16) & 255) * shade)), gg = Math.min(255, Math.round(((c >> 8) & 255) * shade)), bb = Math.min(255, Math.round((c & 255) * shade));
                        out.add(new Face((float) Z2 + layer * 0.01f, (int) Math.floor(sx - hx), (int) Math.floor(sy2 - hy), (int) Math.ceil(sx + hx), (int) Math.ceil(sy2 + hy), 0xFF000000 | (r << 16) | (gg << 8) | bb));
                    }
                }
            }
        }
        out.sort(java.util.Comparator.comparingDouble(Face::depth));
        for (Face f : out) g.fill(f.x0(), f.y0(), Math.max(f.x1(), f.x0() + 1), Math.max(f.y1(), f.y0() + 1), f.argb());
    }
}
