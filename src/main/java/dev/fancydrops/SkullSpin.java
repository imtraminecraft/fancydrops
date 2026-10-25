package dev.fancydrops;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * A smooth, real 3D head block. Each skull gets one small texture (N x N) that is ray-traced from its six 8x8 faces at the current turn angle
 * (3x3 samples per pixel, so the edges are soft instead of jumping from pixel to pixel) and drawn as one quad.
 * The turn is quantised to 96 steps, so it looks continuous. If a texture can't be made the caller falls back to the old 16x16 sprite.
 */
final class SkullSpin {
    private static final int N = 128, SS = 2, STEPS = 96;
    private static final class Entry { DynamicTexture tex; Identifier id; int step = -1; volatile int[] done; volatile int doneStep = -1; volatile boolean busy; }
    private static final Map<String, Entry> MAP = new HashMap<>();
    private static boolean failed;
    /** The tracing runs off the render thread (two low-priority workers): the game only copies a finished picture into the texture, so a page of spinning heads no longer costs frames. */
    private static final java.util.concurrent.ExecutorService POOL = java.util.concurrent.Executors.newFixedThreadPool(2, r -> { Thread t = new Thread(r, "fancydrops-skull"); t.setDaemon(true); t.setPriority(Thread.MIN_PRIORITY); return t; });

    /** Draws the head turned by yaw (radians), centred on the current origin, 16 gui units wide. false = not available. */
    static boolean draw(GuiGraphicsExtractor g, String key, int[][] faces, float yaw, float alpha) {
        if (failed || faces == null) return false;
        try {
            int step = Math.floorMod(Math.round(yaw / (float) (Math.PI * 2) * STEPS), STEPS);
            Entry e = MAP.get(key);
            if (e == null) {
                e = new Entry();
                e.id = Identifier.fromNamespaceAndPath("fancydrops", "skull/" + key.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "_"));
                e.tex = new DynamicTexture(() -> "fancydrops skull", N, N, true);
                Minecraft.getInstance().getTextureManager().register(e.id, e.tex);
                MAP.put(key, e);
            }
            int[] fin = e.done;
            if (fin != null) {                                                                  // a worker finished a picture: copy it in (this is all the render thread does)
                NativeImage img = e.tex.getPixels();
                if (img == null) { failed = true; return false; }
                for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
                    int p = fin[y * N + x];
                    img.setPixelABGR(x, y, (p & 0xFF00FF00) | ((p >> 16) & 0xFF) | ((p & 0xFF) << 16));
                }
                e.tex.upload(); e.step = e.doneStep; e.done = null;
            }
            if (e.step != step && !e.busy) {
                e.busy = true; final Entry ee = e; final int st = step;
                POOL.execute(() -> { try { int[] px = render(faces, st * (float) (Math.PI * 2) / STEPS); ee.doneStep = st; ee.done = px; } catch (Throwable t) { t.printStackTrace(); } finally { ee.busy = false; } });
            }
            if (e.step < 0) return true;                                                        // the first picture is still being made: nothing to show for a moment
            int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
            g.blit(RenderPipelines.GUI_TEXTURED, e.id, -8, -8, 0f, 0f, 16, 16, N, N, N, N, (a << 24) | 0xFFFFFF);
            return true;
        } catch (Throwable t) { failed = true; t.printStackTrace(); return false; }
    }

    /** Ray-traces the unit cube. Faces are in Box order +x, -x, +y, -y, +z, -z. No allocation per sample. */
    private static int[] render(int[][] f, float yaw) {
        int[] out = new int[N * N];
        float pitch = 0.46f, cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw), cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch), k = 5.3f * N / 16f;
        float lx = -0.45f, ly = 0.8f, lz = 0.55f, ll = (float) Math.sqrt(lx * lx + ly * ly + lz * lz); lx /= ll; ly /= ll; lz /= ll;
        float wdx = cp * sy, wdy = -sp, wdz = -cp * cy;
        final int n = SS * SS;
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
            float ar = 0, ag = 0, ab = 0, aa = 0;
            for (int j = 0; j < SS; j++) for (int i = 0; i < SS; i++) {
                float vx = (x + (i + 0.5f) / SS - N / 2f) / k, vy = -(y + (j + 0.5f) / SS - N * 0.525f) / k, vz = 10f;
                float ry = vy * cp + vz * sp, rz = -vy * sp + vz * cp;
                float ox = vx * cy - rz * sy, oz = vx * sy + rz * cy, oy = ry;
                float t0 = -1e9f, t1 = 1e9f; int face = -1; boolean miss = false;
                for (int ax = 0; ax < 3 && !miss; ax++) {                                         // the slab test on x, y, z without arrays
                    float oa = ax == 0 ? ox : ax == 1 ? oy : oz, da = ax == 0 ? wdx : ax == 1 ? wdy : wdz;
                    if (Math.abs(da) < 1e-6f) { if (Math.abs(oa) > 1f) miss = true; continue; }
                    float ta = (-1f - oa) / da, tb = (1f - oa) / da;
                    if (ta > tb) { float tmp = ta; ta = tb; tb = tmp; }
                    if (ta > t0) { t0 = ta; face = da > 0 ? ax * 2 + 1 : ax * 2; }
                    if (tb < t1) t1 = tb;
                }
                if (miss || t0 > t1 || face < 0) continue;
                float hx = ox + wdx * t0, hy = oy + wdy * t0, hz = oz + wdz * t0, u, v;
                switch (face) {
                    case 0 -> { u = (1f - hz) / 2f; v = (1f - hy) / 2f; }
                    case 1 -> { u = (hz + 1f) / 2f; v = (1f - hy) / 2f; }
                    case 2 -> { u = (hx + 1f) / 2f; v = (hz + 1f) / 2f; }
                    case 3 -> { u = (hx + 1f) / 2f; v = (1f - hz) / 2f; }
                    case 4 -> { u = (hx + 1f) / 2f; v = (1f - hy) / 2f; }
                    default -> { u = (1f - hx) / 2f; v = (1f - hy) / 2f; }
                }
                int c = f[face][Math.min(7, Math.max(0, (int) (v * 8))) * 8 + Math.min(7, Math.max(0, (int) (u * 8)))];
                if ((c >>> 24) < 16) c = 0xFF000000 | (c & 0xFFFFFF);
                float nx = face == 0 ? 1 : face == 1 ? -1 : 0, ny = face == 2 ? 1 : face == 3 ? -1 : 0, nz = face == 4 ? 1 : face == 5 ? -1 : 0;
                float wx = nx * cy + nz * sy, wz = -nx * sy + nz * cy, wy = ny;
                float vyv = wy * cp - wz * sp, vzv = wy * sp + wz * cp, vxv = wx;
                float lit = 0.5f + 0.62f * Math.max(0f, vxv * lx + vyv * ly + vzv * lz);
                float ex = Math.min(Math.min(Math.abs(Math.abs(hx) - 1f), Math.abs(Math.abs(hy) - 1f)), Math.abs(Math.abs(hz) - 1f));
                if (ex < 0.07f) lit *= 0.72f + 0.28f * (ex / 0.07f);
                ar += ((c >> 16) & 255) * lit; ag += ((c >> 8) & 255) * lit; ab += (c & 255) * lit; aa += 1f;
            }
            if (aa <= 0f) continue;
            out[y * N + x] = (Math.round(255f * aa / n) << 24) | (Math.min(255, Math.round(ar / aa)) << 16) | (Math.min(255, Math.round(ag / aa)) << 8) | Math.min(255, Math.round(ab / aa));
        }
        return out;
    }

    private SkullSpin() {}
}
