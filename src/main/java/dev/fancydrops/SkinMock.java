package dev.fancydrops;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Your own skin as a little 3D figure for the aura preview. The skin is fetched from Mojang's session server once. The figure is drawn
 * into small pictures, one for each of 36 turns, in the background; on screen only a ready picture is shown (as a few hundred rectangles),
 * so turning it costs next to nothing.
 */
final class SkinMock {
    private SkinMock() {}
    private static volatile int[] skin;               // 64 x 64 ARGB (the real one, or a plain stand-in until it arrives)
    private static boolean started;
    private static volatile int gen;                  // bumps when a new skin arrives
    private static final int N = 36, K = 4, W = 22 * K, H = 38 * K, FEET = H - 3 * K;
    private static volatile int[][] cache = new int[N][];
    private static volatile int cacheGen = -1;
    private static volatile boolean building;

    static void ensure() {
        if (!started) {
            started = true; skin = standIn(); gen = 1;
            UUID id = Cosmetics.me();
            if (id != null) java.util.concurrent.CompletableFuture.runAsync(() -> {
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
                    if (img.getHeight() == 32) for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { o[(48 + y) * 64 + 16 + x] = o[(16 + y) * 64 + x]; o[(48 + y) * 64 + 32 + x] = o[(16 + y) * 64 + 40 + x]; }
                    skin = o; gen++;
                } catch (Throwable t) { }
            });
        }
        if (cacheGen != gen && !building) {                                                          // (re)draw the 36 pictures in the background
            building = true; final int g0 = gen; final int[] sk = skin;
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    int[][] c = new int[N][];
                    int[][] old = cache;                                                    // keep showing the old pictures until the new ones are ready
                    int failed = 0;
                    for (int a = 0; a < N; a++) {
                        try { c[a] = render(sk, a * (float) (Math.PI * 2 / N)); } catch (Throwable t) { failed++; if (failed == 1) System.out.println("[FancyDrops] skin preview: turn " + a + " failed: " + t); }
                    }
                    for (int a = 0; a < N; a++) if (c[a] == null) c[a] = old[a];
                    cache = c;
                    cacheGen = g0;
                    int n = 0; for (int[] r : c) if (r != null && r.length > 0) n++;
                    System.out.println("[FancyDrops] skin preview built: " + n + "/" + N + " turns, " + failed + " failed");
                } catch (Throwable t) { System.out.println("[FancyDrops] skin preview error: " + t); cacheGen = g0; } finally { building = false; }
            });
        }
    }

    private static int[] standIn() {
        int[] o = new int[64 * 64];
        int[][] r = {{0, 0, 8, 8, 8, 0xFFC99A6B}, {16, 16, 8, 12, 4, 0xFF2F6BFF}, {40, 16, 4, 12, 4, 0xFFC99A6B}, {32, 48, 4, 12, 4, 0xFFC99A6B}, {0, 16, 4, 12, 4, 0xFF3A3F8F}, {16, 48, 4, 12, 4, 0xFF3A3F8F}};
        for (int[] q : r) { int u = q[0], v = q[1], w = q[2], h = q[3], d = q[4];
            for (int y = v; y < v + d + h; y++) for (int x = u; x < u + 2 * d + 2 * w; x++) o[y * 64 + x] = q[5]; }
        for (int[] e : new int[][]{{10, 10}, {13, 10}}) o[e[1] * 64 + e[0]] = 0xFF1C1C28;
        return o;
    }

    // box: centre, half sizes, texture origin
    private static final float[][] BASE = {{0, 28, 0, 4, 4, 4, 0, 0}, {0, 18, 0, 4, 6, 2, 16, 16}, {-6, 18, 0, 2, 6, 2, 40, 16}, {6, 18, 0, 2, 6, 2, 32, 48}, {-2, 6, 0, 2, 6, 2, 0, 16}, {2, 6, 0, 2, 6, 2, 16, 48}};
    private static final int[][] OVER = {{32, 0}, {16, 32}, {40, 32}, {48, 48}, {0, 32}, {0, 48}};

    /** One turn of the figure: a W x H picture as a list of rectangles (x, y, w, h, colour). */
    private static int[] render(int[] sk, float yaw) {
        int[] pix = new int[W * H]; float[] zb = new float[W * H]; java.util.Arrays.fill(zb, -1e9f);
        double cy = Math.cos(yaw), sy = Math.sin(yaw), tilt = -0.16, ct = Math.cos(tilt), st = Math.sin(tilt);
        for (int layer = 0; layer < 2; layer++) for (int bi = 0; bi < 6; bi++) {
            float[] b = BASE[bi]; float g = layer == 1 ? 0.5f : 0f;
            float hw = b[3] + g, hh = b[4] + g, hd = b[5] + g; int u = layer == 1 ? OVER[bi][0] : (int) b[6], v = layer == 1 ? OVER[bi][1] : (int) b[7];
            int w = (int) (b[3] * 2), h = (int) (b[4] * 2), d = (int) (b[5] * 2);
            // normal, origin, U, V (full face vectors), texture rect, texels
            float[][] fs = {
                {0, 0, 1, -hw, hh, hd, 2 * hw, 0, 0, 0, -2 * hh, 0, u + d, v + d, w, h},
                {0, 0, -1, hw, hh, -hd, -2 * hw, 0, 0, 0, -2 * hh, 0, u + 2 * d + w, v + d, w, h},
                {-1, 0, 0, -hw, hh, -hd, 0, 0, 2 * hd, 0, -2 * hh, 0, u, v + d, d, h},
                {1, 0, 0, hw, hh, hd, 0, 0, -2 * hd, 0, -2 * hh, 0, u + d + w, v + d, d, h},
                {0, 1, 0, -hw, hh, -hd, 2 * hw, 0, 0, 0, 0, 2 * hd, u + d, v, w, d},
                {0, -1, 0, -hw, -hh, hd, 2 * hw, 0, 0, 0, 0, -2 * hd, u + d + w, v, w, d}};
            for (float[] f : fs) {
                double nz0 = -f[0] * sy + f[2] * cy, nz = f[1] * st + nz0 * ct, ny = f[1] * ct - nz0 * st;
                if (nz <= 0.02) continue;
                int tu = (int) f[12], tv = (int) f[13], nu = (int) f[14], nv = (int) f[15];
                float shade = (float) (0.62 + 0.38 * nz + 0.08 * ny);
                for (int j = 0; j < nv; j++) for (int i = 0; i < nu; i++) {
                    int c = sk[(tv + j) * 64 + tu + i];
                    if ((c >>> 24) < 40) continue;
                    double[] sx = new double[4], sy2 = new double[4], zz = new double[4];
                    for (int k = 0; k < 4; k++) {
                        double a = (i + (k == 1 || k == 2 ? 1 : 0)) / (double) nu, bb = (j + (k >= 2 ? 1 : 0)) / (double) nv;
                        double X = b[0] + f[3] + f[6] * a + f[9] * bb, Y = b[1] + f[4] + f[7] * a + f[10] * bb, Z = b[2] + f[5] + f[8] * a + f[11] * bb;
                        double X1 = X * cy + Z * sy, Z1 = -X * sy + Z * cy, Y2 = Y * ct - Z1 * st, Z2 = Y * st + Z1 * ct;
                        sx[k] = W / 2.0 + X1 * K; sy2[k] = FEET - Y2 * K; zz[k] = Z2;
                    }
                    double zc = (zz[0] + zz[1] + zz[2] + zz[3]) / 4 + layer * 0.01;
                    int x0 = (int) Math.floor(Math.min(Math.min(sx[0], sx[1]), Math.min(sx[2], sx[3]))), x1 = (int) Math.ceil(Math.max(Math.max(sx[0], sx[1]), Math.max(sx[2], sx[3])));
                    int y0 = (int) Math.floor(Math.min(Math.min(sy2[0], sy2[1]), Math.min(sy2[2], sy2[3]))), y1 = (int) Math.ceil(Math.max(Math.max(sy2[0], sy2[1]), Math.max(sy2[2], sy2[3])));
                    int col = 0xFF000000 | (Math.min(255, Math.round(((c >> 16) & 255) * shade)) << 16) | (Math.min(255, Math.round(((c >> 8) & 255) * shade)) << 8) | Math.min(255, Math.round((c & 255) * shade));
                    for (int yy = Math.max(0, y0); yy <= Math.min(H - 1, y1); yy++) for (int xx = Math.max(0, x0); xx <= Math.min(W - 1, x1); xx++) {
                        if (zc <= zb[yy * W + xx] || !inQuad(xx + 0.5, yy + 0.5, sx, sy2)) continue;
                        zb[yy * W + xx] = (float) zc; pix[yy * W + xx] = col;
                    }
                }
            }
        }
        // merge pixels into rectangles: runs along a row, then the same run on the rows below
        List<int[]> rects = new ArrayList<>(); List<int[]> open = new ArrayList<>();
        for (int y = 0; y < H; y++) {
            List<int[]> next = new ArrayList<>();
            for (int x = 0; x < W; ) {
                int c = pix[y * W + x]; if (c == 0) { x++; continue; }
                int e = x; while (e < W && pix[y * W + e] == c) e++;
                int[] hit = null; for (int[] o : open) if (o[0] == x && o[2] == e - x && o[4] == c && o[1] + o[3] == y) { hit = o; break; }
                if (hit != null) { hit[3]++; next.add(hit); } else { int[] r = {x, y, e - x, 1, c}; rects.add(r); next.add(r); }
                x = e;
            }
            open = next;
        }
        int[] out = new int[rects.size() * 5]; int p = 0;
        for (int[] r : rects) { out[p++] = r[0]; out[p++] = r[1]; out[p++] = r[2]; out[p++] = r[3]; out[p++] = r[4]; }
        return out;
    }
    private static boolean inQuad(double px, double py, double[] x, double[] y) { return tri(px, py, x[0], y[0], x[1], y[1], x[2], y[2]) || tri(px, py, x[0], y[0], x[2], y[2], x[3], y[3]); }
    private static boolean tri(double px, double py, double ax, double ay, double bx, double by, double cx, double cy) {
        double d = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy);
        if (Math.abs(d) < 1e-9) return false;
        double l1 = ((by - cy) * (px - cx) + (cx - bx) * (py - cy)) / d, l2 = ((cy - ay) * (px - cx) + (ax - cx) * (py - cy)) / d, l3 = 1 - l1 - l2;
        return l1 >= -0.02 && l2 >= -0.02 && l3 >= -0.02;
    }

    /** The figure with its feet at (cx, feetY), turned by yaw (radians; 0 = facing you). One GUI pixel per picture pixel (about 150 tall). */
    static void draw(GuiGraphicsExtractor g, int cx, int feetY, float yaw) {
        ensure();
        int a = Math.floorMod(Math.round(yaw / (float) (Math.PI * 2 / N)), N);
        int[][] c = cache; int[] r = c[a];
        for (int d = 1; r == null && d < N / 2; d++) { r = c[(a + d) % N]; if (r == null) r = c[Math.floorMod(a - d, N)]; }
        if (r == null) return;
        int left = cx - W / 2, top = feetY - FEET;
        for (int i = 0; i < r.length; i += 5) g.fill(left + r[i], top + r[i + 1], left + r[i] + r[i + 2], top + r[i + 1] + r[i + 3], r[i + 4]);
    }
    static int height() { return 32 * K; }
}
