package dev.fancydrops;

/**
 * Turns a flat 16x16 item sprite into a lit, slightly puffy voxel model that can spin freely.
 * Every opaque pixel becomes a column that is thicker in the middle of the sprite than at its edges, and each voxel on the
 * surface is splatted onto the canvas with a z-buffer, so any sprite (not only player heads) is properly 3D from every side.
 */
final class PuffyItem {
    private final float[] vx, vy, vz, nx, ny, nz;
    private final int[] col;
    private final int n;

    private PuffyItem(float[] vx, float[] vy, float[] vz, float[] nx, float[] ny, float[] nz, int[] col, int n) {
        this.vx = vx; this.vy = vy; this.vz = vz; this.nx = nx; this.ny = ny; this.nz = nz; this.col = col; this.n = n;
    }

    /** thick = false gives a paper-thin sprite (the "3D drop items" switch is off). */
    static PuffyItem of(int[] px, int w, int h, boolean thick) { return of(px, w, h, thick, 1.0f); }

    /** depth = how many sprite pixels thick the slab is: 1 is a vanilla dropped item; a head picture gets a little more. */
    static PuffyItem of(int[] px, int w, int h, boolean thick, float depth) {
        float[] half = new float[w * h];
        boolean[] solid = new boolean[w * h];
        for (int i = 0; i < px.length; i++) solid[i] = (px[i] >>> 24) > 24;
        // distance to the nearest see-through pixel (4-neighbour chamfer, two passes)
        int[] dist = new int[w * h];
        for (int i = 0; i < dist.length; i++) dist[i] = solid[i] ? 99 : 0;
        for (int pass = 0; pass < 2; pass++) {
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int i = y * w + x; if (!solid[i]) continue;
                int d = dist[i];
                d = Math.min(d, (x == 0 ? 0 : dist[i - 1]) + 1); d = Math.min(d, (y == 0 ? 0 : dist[i - w]) + 1);
                dist[i] = d;
            }
            for (int y = h - 1; y >= 0; y--) for (int x = w - 1; x >= 0; x--) {
                int i = y * w + x; if (!solid[i]) continue;
                int d = dist[i];
                d = Math.min(d, (x == w - 1 ? 0 : dist[i + 1]) + 1); d = Math.min(d, (y == h - 1 ? 0 : dist[i + w]) + 1);
                dist[i] = d;
            }
        }
        for (int i = 0; i < half.length; i++) half[i] = !solid[i] ? 0 : thick ? 1.5f + Math.min(1.5f, 0.5f * (dist[i] - 1)) : 0.5f;

        int cap = w * h * 14;
        float[] ax = new float[cap], ay = new float[cap], az = new float[cap], anx = new float[cap], any = new float[cap], anz = new float[cap];
        int[] ac = new int[cap]; int k = 0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int i = y * w + x; if (!solid[i]) continue;
            float H = half[i], px0 = x - w / 2f + 0.5f, py0 = h / 2f - y - 0.5f;
            float hl = x > 0 ? half[i - 1] : 0, hr = x < w - 1 ? half[i + 1] : 0, hu = y > 0 ? half[i - w] : 0, hd = y < h - 1 ? half[i + w] : 0;
            float gx = -(hr - hl) * 0.35f, gy = -(hu - hd) * 0.35f, len = (float) Math.sqrt(gx * gx + gy * gy + 1);
            int c = px[i] | 0xFF000000;
            for (int s = -1; s <= 1; s += 2) {                                   // front and back faces
                if (k >= cap) break;
                ax[k] = px0; ay[k] = py0; az[k] = s * H; anx[k] = gx / len; any[k] = gy / len; anz[k] = s / len; ac[k++] = c;
            }
            int[][] dirs = {{-1, 0}, {1, 0}, {0, 1}, {0, -1}};                  // side walls where the neighbour is lower
            float[] nh = {hl, hr, hu, hd};
            for (int d = 0; d < 4; d++) {
                if (nh[d] >= H) continue;
                for (float z = nh[d] + 0.25f; z < H; z += 0.5f) for (int s = -1; s <= 1; s += 2) {
                    if (k >= cap) break;
                    ax[k] = px0 + dirs[d][0] * 0.5f; ay[k] = py0 + dirs[d][1] * 0.5f; az[k] = s * z;
                    anx[k] = dirs[d][0]; any[k] = dirs[d][1]; anz[k] = 0; ac[k++] = Canvas.shade(c, 0.82f);
                }
            }
        }
        PuffyItem it = new PuffyItem(java.util.Arrays.copyOf(ax, k), java.util.Arrays.copyOf(ay, k), java.util.Arrays.copyOf(az, k),
                java.util.Arrays.copyOf(anx, k), java.util.Arrays.copyOf(any, k), java.util.Arrays.copyOf(anz, k), java.util.Arrays.copyOf(ac, k), k);
        it.src = px; it.sw = w; it.sh = h; it.thick = thick; it.depth = depth;
        return it;
    }

    private int[] src; private int sw, sh; private boolean thick; private float depth = 1f;

    /**
     * Draws the sprite as a clean extruded slab, like a dropped item in the game: a stack of thin layers of the sprite, seen at an angle.
     * The layer facing you keeps the true colours and a travelling glint; the layers behind it show as the darker side of the item.
     * k = canvas cells per sprite pixel, yaw spins it about the vertical axis, pitch tips it toward the camera.
     */
    void draw(Canvas c, float cx, float cy, float k, double yaw, double pitch) {
        double cs = Math.cos(yaw), sn = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);
        int layers = thick ? Math.max(3, Math.round(depth * 4f) + 1) : 1;
        float half = thick ? depth / 2f : 0.5f;
        boolean frontNear = cs >= 0;
        float glint = (float) ((yaw * 0.32) % 1.0);
        if (glint < 0) glint += 1f;
        for (int li = 0; li < layers; li++) {
            int idx = frontNear ? li : layers - 1 - li;                         // far layers first, the face you see last
            float z = layers == 1 ? 0f : -half + 2f * half * idx / (layers - 1);
            boolean face = li == layers - 1;
            float xo = (float) (z * sn), yo = (float) (-z * sp * 0.6);
            for (int v = 0; v < sh; v++) {
                float y0 = cy + (float) ((v - sh / 2f) * cp + yo) * k, y1 = cy + (float) ((v + 1 - sh / 2f) * cp + yo) * k;
                int ya = Math.round(Math.min(y0, y1)), yb = Math.max(ya + 1, Math.round(Math.max(y0, y1)));
                for (int u = 0; u < sw; u++) {
                    int p = src[v * sw + u];
                    if ((p >>> 24) <= 24) continue;
                    float x0 = cx + (float) ((u - sw / 2f) * cs + xo) * k, x1 = cx + (float) ((u + 1 - sw / 2f) * cs + xo) * k;
                    int xa = Math.round(Math.min(x0, x1)), xb = Math.max(xa + 1, Math.round(Math.max(x0, x1)));
                    float f;
                    if (face) {
                        f = 1.04f - 0.16f * (v / (float) sh);                      // lit from above
                        float d = Math.abs(((u + v) / (float) (sw + sh) - glint + 0.5f) % 1f - 0.5f);
                        if (d < 0.07f) f += 0.5f * (1 - d / 0.07f);                // a glint sweeps across now and then
                    } else f = 0.55f + 0.1f * (idx / (float) layers);
                    int col = Canvas.shade(p | 0xFF000000, f);
                    for (int yy = ya; yy < yb; yy++) for (int xx = xa; xx < xb; xx++) {
                        if (xx < 0 || yy < 0 || xx >= c.w || yy >= c.h) continue;
                        c.px[yy * c.w + xx] = col;
                    }
                }
            }
        }
    }
}
