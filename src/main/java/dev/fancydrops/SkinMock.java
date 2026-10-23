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
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                for (int attempt = 0; attempt < 4; attempt++) {                                      // the session server can say "slow down": try again a little later
                    try { if (fetch()) return; } catch (Throwable t) { System.out.println("[FancyDrops] skin fetch failed (" + attempt + "): " + t); }
                    try { Thread.sleep(8000L * (attempt + 1)); } catch (InterruptedException e) { return; }
                }
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

    /** The base64 "textures" value of the logged-in profile, straight from the game when it has it. */
    private static String localTextures() {
        try {
            Object prof = net.minecraft.client.Minecraft.getInstance().getGameProfile();
            Object props = null;
            for (String m : new String[]{"properties", "getProperties"}) { try { props = prof.getClass().getMethod(m).invoke(prof); if (props != null) break; } catch (Throwable ignored) { } }
            if (props == null) return null;
            Object col = props.getClass().getMethod("get", Object.class).invoke(props, "textures");
            if (col instanceof java.util.Collection<?> cl) for (Object pr : cl) {
                for (String m : new String[]{"value", "getValue"}) { try { Object v = pr.getClass().getMethod(m).invoke(pr); if (v != null) return v.toString(); } catch (Throwable ignored) { } }
            }
        } catch (Throwable ignored) { }
        return null;
    }
    /** The web address of the skin the game itself is wearing, found by looking through the player's skin object. */
    private static String gameSkinUrl() {
        try {
            var mc = net.minecraft.client.Minecraft.getInstance(); Object pl = mc.player; if (pl == null) return null;
            for (String m : new String[]{"getSkin", "skin"}) {
                try { Object sk = pl.getClass().getMethod(m).invoke(pl); String u = findUrl(sk, 0); if (u != null) return u; } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) { }
        return null;
    }
    private static String findUrl(Object o, int depth) {
        if (o == null || depth > 3) return null;
        if (o instanceof String s) return s.startsWith("http") && s.contains("textures") ? s : null;
        Class<?> k = o.getClass(); if (!k.getName().startsWith("net.minecraft")) return null;
        for (Class<?> c = k; c != null && c != Object.class; c = c.getSuperclass()) for (var f : c.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            try { f.setAccessible(true); String u = findUrl(f.get(o), depth + 1); if (u != null) return u; } catch (Throwable ignored) { }
        }
        return null;
    }
    private static boolean fetch() throws Exception {
        var http = java.net.http.HttpClient.newHttpClient();
        String url = null, val = localTextures();
        if (val != null) { try { url = com.google.gson.JsonParser.parseString(new String(java.util.Base64.getDecoder().decode(val))).getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString(); } catch (Throwable t) { System.out.println("[FancyDrops] skin: profile data unreadable: " + t); } }
        if (url == null) url = gameSkinUrl();
        if (url == null) {
            UUID id = null; var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) id = mc.player.getUUID();
            if (id == null) id = Cosmetics.me(); if (id == null) id = mc.getUser().getProfileId();
            if (id == null) { System.out.println("[FancyDrops] skin: no player id yet"); return false; }
            var rq = java.net.http.HttpRequest.newBuilder(java.net.URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + id.toString().replace("-", ""))).header("User-Agent", "FancyDrops").GET().build();
            var rs = http.send(rq, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (rs.statusCode() != 200) { System.out.println("[FancyDrops] skin: session server said " + rs.statusCode()); return false; }
            val = com.google.gson.JsonParser.parseString(rs.body()).getAsJsonObject().getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
            url = com.google.gson.JsonParser.parseString(new String(java.util.Base64.getDecoder().decode(val))).getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        }
        System.out.println("[FancyDrops] skin: reading " + url);
        byte[] png = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).header("User-Agent", "FancyDrops").GET().build(), java.net.http.HttpResponse.BodyHandlers.ofByteArray()).body();
        var img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        int[] o = new int[64 * 64];
        for (int y = 0; y < Math.min(64, img.getHeight()); y++) for (int x = 0; x < 64; x++) o[y * 64 + x] = img.getRGB(x, y);
        if (img.getHeight() == 32) for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { o[(48 + y) * 64 + 16 + x] = o[(16 + y) * 64 + x]; o[(48 + y) * 64 + 32 + x] = o[(16 + y) * 64 + 40 + x]; }
        skin = o; gen++; System.out.println("[FancyDrops] skin loaded");
        return true;
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
        int[] pix = new int[W * H];
        double cy = Math.cos(yaw), sy = Math.sin(yaw), tilt = -0.16, ct = Math.cos(tilt), st = Math.sin(tilt);
        for (int py = 0; py < H; py++) for (int pxl = 0; pxl < W; pxl++) {
            double X1 = (pxl + 0.5 - W / 2.0) / K, Y2 = (FEET - (py + 0.5)) / K;                 // the ray of this pixel, straight into the picture (no gaps are possible)
            double ox = X1 * cy + Y2 * st * sy, oy = Y2 * ct, oz = X1 * sy - Y2 * st * cy;         // where it starts, in the figure's own space
            double dx = -ct * sy, dy = st, dz = ct * cy;                                           // and where it goes (towards the viewer = larger t)
            double bestT = -1e9; int bestCol = 0;
            for (int layer = 0; layer < 2; layer++) for (int bi = 0; bi < 6; bi++) {
                float[] b = BASE[bi]; float g = layer == 1 ? 0.5f : 0f;
                double hw = b[3] + g, hh = b[4] + g, hd = b[5] + g;
                double tNear = -1e9, tFar = 1e9; int farAxis = -1; double farSign = 0;
                double[] o3 = {ox - b[0], oy - b[1], oz - b[2]}, d3 = {dx, dy, dz}, h3 = {hw, hh, hd};
                boolean miss = false;
                for (int ax = 0; ax < 3; ax++) {
                    if (Math.abs(d3[ax]) < 1e-9) { if (Math.abs(o3[ax]) > h3[ax]) { miss = true; break; } continue; }
                    double t1 = (-h3[ax] - o3[ax]) / d3[ax], t2 = (h3[ax] - o3[ax]) / d3[ax], lo = Math.min(t1, t2), hi = Math.max(t1, t2);
                    if (lo > tNear) tNear = lo;
                    if (hi < tFar) { tFar = hi; farAxis = ax; farSign = d3[ax] > 0 ? 1 : -1; }
                    if (tNear > tFar) { miss = true; break; }
                }
                if (miss || farAxis < 0) continue;
                double t = tFar + layer * 0.01;
                if (t <= bestT) continue;
                double[] hit = {o3[0] + d3[0] * tFar, o3[1] + d3[1] * tFar, o3[2] + d3[2] * tFar};
                float[][] fs = faces(bi, hw, hh, hd, layer);
                float[] f = null;
                for (float[] cand : fs) { int nax = cand[0] != 0 ? 0 : cand[1] != 0 ? 1 : 2; if (nax == farAxis && Math.signum(cand[nax]) == farSign) { f = cand; break; } }
                if (f == null) continue;
                double rx = hit[0] - f[3], ry = hit[1] - f[4], rz = hit[2] - f[5];
                double uu = f[6] * f[6] + f[7] * f[7] + f[8] * f[8], vv = f[9] * f[9] + f[10] * f[10] + f[11] * f[11];
                double a2 = (rx * f[6] + ry * f[7] + rz * f[8]) / uu, bb = (rx * f[9] + ry * f[10] + rz * f[11]) / vv;
                int nu = (int) f[14], nv = (int) f[15], i = Math.max(0, Math.min(nu - 1, (int) Math.floor(a2 * nu))), j = Math.max(0, Math.min(nv - 1, (int) Math.floor(bb * nv)));
                int c = sk[((int) f[13] + j) * 64 + (int) f[12] + i];
                if ((c >>> 24) < 40) continue;                                                         // a see-through part of the outer layer: look through it
                double nz0 = -f[0] * sy + f[2] * cy, nz = f[1] * st + nz0 * ct, ny = f[1] * ct - nz0 * st;
                float shade = (float) Math.max(0.62, 0.62 + 0.38 * nz + 0.08 * ny);
                bestT = t;
                bestCol = 0xFF000000 | (Math.min(255, Math.round(((c >> 16) & 255) * shade)) << 16) | (Math.min(255, Math.round(((c >> 8) & 255) * shade)) << 8) | Math.min(255, Math.round((c & 255) * shade));
            }
            if (bestCol != 0) pix[py * W + pxl] = bestCol;
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
    /** The six faces of a box: normal, origin, U and V vectors (relative to the box centre), texture rect. */
    private static float[][] faces(int bi, double hwD, double hhD, double hdD, int layer) {
        float[] b = BASE[bi]; float hw = (float) hwD, hh = (float) hhD, hd = (float) hdD;
        int u = layer == 1 ? OVER[bi][0] : (int) b[6], v = layer == 1 ? OVER[bi][1] : (int) b[7];
        int w = (int) (b[3] * 2), h = (int) (b[4] * 2), d = (int) (b[5] * 2);
        return new float[][]{
            {0, 0, 1, -hw, hh, hd, 2 * hw, 0, 0, 0, -2 * hh, 0, u + d, v + d, w, h},
            {0, 0, -1, hw, hh, -hd, -2 * hw, 0, 0, 0, -2 * hh, 0, u + 2 * d + w, v + d, w, h},
            {-1, 0, 0, -hw, hh, -hd, 0, 0, 2 * hd, 0, -2 * hh, 0, u, v + d, d, h},
            {1, 0, 0, hw, hh, hd, 0, 0, -2 * hd, 0, -2 * hh, 0, u + d + w, v + d, d, h},
            {0, 1, 0, -hw, hh, -hd, 2 * hw, 0, 0, 0, 0, 2 * hd, u + d, v, w, d},
            {0, -1, 0, -hw, -hh, hd, 2 * hw, 0, 0, 0, 0, -2 * hd, u + d + w, v, w, d}};
    }
    private static boolean inQuad(double px, double py, double[] x, double[] y) { return tri(px, py, x[0], y[0], x[1], y[1], x[2], y[2]) || tri(px, py, x[0], y[0], x[2], y[2], x[3], y[3]); }
    private static boolean tri(double px, double py, double ax, double ay, double bx, double by, double cx, double cy) {
        double d = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy);
        if (Math.abs(d) < 1e-9) return false;
        double l1 = ((by - cy) * (px - cx) + (cx - bx) * (py - cy)) / d, l2 = ((cy - ay) * (px - cx) + (ax - cx) * (py - cy)) / d, l3 = 1 - l1 - l2;
        return l1 >= -0.09 && l2 >= -0.09 && l3 >= -0.09;
    }

    /** The figure with its feet at (cx, feetY), turned by yaw (radians; 0 = facing you). One GUI pixel per picture pixel (about 150 tall). */
    // the 36 pictures live in one texture, so the figure is ONE quad instead of hundreds of rectangles
    private static final int COLS = 6, AT_W = COLS * W, AT_H = COLS * H;
    private static final net.minecraft.resources.Identifier TID = net.minecraft.resources.Identifier.fromNamespaceAndPath("fancydrops", "skinmock");
    private static net.minecraft.client.renderer.texture.DynamicTexture stex; private static int texGen = -2; private static boolean texFailed;
    private static void buildTex(int[][] c, int gen0) {
        try {
            if (stex == null) { stex = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "fancydrops skin", AT_W, AT_H, true); net.minecraft.client.Minecraft.getInstance().getTextureManager().register(TID, stex); }
            var img = stex.getPixels(); if (img == null) { texFailed = true; return; }
            for (int y = 0; y < AT_H; y++) for (int x = 0; x < AT_W; x++) img.setPixelABGR(x, y, 0);
            for (int a = 0; a < N; a++) {
                int[] r = c[a]; if (r == null) continue; int ox = a % COLS * W, oy = a / COLS * H;
                for (int i = 0; i < r.length; i += 5) {
                    int p = r[i + 4], abgr = (p & 0xFF00FF00) | ((p >> 16) & 0xFF) | ((p & 0xFF) << 16);
                    for (int yy = r[i + 1]; yy < r[i + 1] + r[i + 3]; yy++) for (int xx = r[i]; xx < r[i] + r[i + 2]; xx++) img.setPixelABGR(ox + xx, oy + yy, abgr);
                }
            }
            stex.upload(); texGen = gen0;
        } catch (Throwable t) { texFailed = true; System.out.println("[FancyDrops] skin texture: " + t); }
    }
    static void draw(GuiGraphicsExtractor g, int cx, int feetY, float yaw) {
        ensure();
        int a = Math.floorMod(Math.round(yaw / (float) (Math.PI * 2 / N)), N);
        int[][] c = cache; int[] r = c[a]; int ai = a;
        for (int d = 1; r == null && d < N / 2; d++) { ai = (a + d) % N; r = c[ai]; if (r == null) { ai = Math.floorMod(a - d, N); r = c[ai]; } }
        if (r == null) return;
        int left = cx - W / 2, top = feetY - FEET;
        int cg = cacheGen;
        if (!texFailed && cg >= 0 && texGen != cg) buildTex(c, cg);
        if (!texFailed && texGen == cg && stex != null) {
            try { g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TID, left, top, (float) (ai % COLS * W), (float) (ai / COLS * H), W, H, W, H, AT_W, AT_H, 0xFFFFFFFF); return; }
            catch (Throwable t) { texFailed = true; }
        }
        for (int i = 0; i < r.length; i += 5) g.fill(left + r[i], top + r[i + 1], left + r[i] + r[i + 2], top + r[i + 1] + r[i + 3], r[i + 4]);
    }
    static int height() { return 32 * K; }
}
