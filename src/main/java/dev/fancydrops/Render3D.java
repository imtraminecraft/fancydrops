package dev.fancydrops;

/**
 * Tiny orthographic ray-caster. One ray per canvas cell, nearest hit wins, so spinning objects never show gaps or seams
 * (the old slice-based voxel drawing did). Two shapes: textured boxes (mob cube, board, heads...) and voxel slabs (flat items).
 */
final class Render3D {
    static final class Hit { double t, nx, ny, nz; int argb; }

    abstract static class Obj {
        double x, y, z, yaw, pitch, roll, scale = 1;
        float bright = 1f;
        boolean outerYaw;                       // true: yaw is applied last (spins the whole object about the world's vertical axis)
        final double[] m = new double[9];

        Obj at(double x, double y, double z) { this.x = x; this.y = y; this.z = z; return this; }
        Obj rot(double yaw, double pitch, double roll) { this.yaw = yaw; this.pitch = pitch; this.roll = roll; return this; }

        /** object -> world = Rz(roll) * Rx(pitch) * Ry(yaw) */
        void update() {
            double cy = Math.cos(yaw), sy = Math.sin(yaw), cx = Math.cos(pitch), sx = Math.sin(pitch), cz = Math.cos(roll), sz = Math.sin(roll);
            double[] ry = {cy, 0, sy, 0, 1, 0, -sy, 0, cy}, rx = {1, 0, 0, 0, cx, -sx, 0, sx, cx}, rz = {cz, -sz, 0, sz, cz, 0, 0, 0, 1};
            if (outerYaw) mul(ry, mul(rz, rx, new double[9]), m); else mul(rz, mul(rx, ry, new double[9]), m);
        }
        private static double[] mul(double[] a, double[] b, double[] o) {
            for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) o[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c];
            return o;
        }

        final boolean cast(double ox, double oy, double oz, double dx, double dy, double dz, Hit h) {
            double px = ox - x, py = oy - y, pz = oz - z, s = 1.0 / scale;
            double lox = (m[0] * px + m[3] * py + m[6] * pz) * s, loy = (m[1] * px + m[4] * py + m[7] * pz) * s, loz = (m[2] * px + m[5] * py + m[8] * pz) * s;
            double ldx = (m[0] * dx + m[3] * dy + m[6] * dz) * s, ldy = (m[1] * dx + m[4] * dy + m[7] * dz) * s, ldz = (m[2] * dx + m[5] * dy + m[8] * dz) * s;
            if (!local(lox, loy, loz, ldx, ldy, ldz, h)) return false;
            double nx = h.nx, ny = h.ny, nz = h.nz;                    // local normal -> world normal
            h.nx = m[0] * nx + m[1] * ny + m[2] * nz; h.ny = m[3] * nx + m[4] * ny + m[5] * nz; h.nz = m[6] * nx + m[7] * ny + m[8] * nz;
            return true;
        }
        abstract boolean local(double ox, double oy, double oz, double dx, double dy, double dz, Hit h);
    }

    /** Box with a texture (square, any size) on each face: +x, -x, +y, -y, +z, -z. A null texture uses the solid colour. */
    static final class Box extends Obj {
        final double hx, hy, hz;
        final int[][] tex = new int[6][];
        final int[] size = new int[6];
        int solid = 0xFFB0B0B0;
        Box(double hx, double hy, double hz) { this.hx = hx; this.hy = hy; this.hz = hz; }
        Box face(int f, int[] t, int n) { tex[f] = t; size[f] = n; return this; }
        Box all(int[] t, int n) { for (int f = 0; f < 6; f++) face(f, t, n); return this; }

        @Override boolean local(double ox, double oy, double oz, double dx, double dy, double dz, Hit h) {
            double[] o = {ox, oy, oz}, d = {dx, dy, dz}, e = {hx, hy, hz};
            double tmin = -1e18, tmax = 1e18; int axis = 0; double sgn = 1;
            for (int a = 0; a < 3; a++) {
                if (Math.abs(d[a]) < 1e-12) { if (Math.abs(o[a]) > e[a]) return false; continue; }
                double t1 = (-e[a] - o[a]) / d[a], t2 = (e[a] - o[a]) / d[a], near = Math.min(t1, t2), far = Math.max(t1, t2);
                if (near > tmin) { tmin = near; axis = a; sgn = d[a] > 0 ? -1 : 1; }
                tmax = Math.min(tmax, far);
            }
            if (tmin > tmax || tmax < 0) return false;
            double qx = ox + tmin * dx, qy = oy + tmin * dy, qz = oz + tmin * dz;
            int f = axis * 2 + (sgn > 0 ? 0 : 1);                       // 0:+x 1:-x 2:+y 3:-y 4:+z 5:-z
            double u, v;
            switch (f) {
                case 4 -> { u = (qx + hx) / (2 * hx); v = (hy - qy) / (2 * hy); }
                case 5 -> { u = (hx - qx) / (2 * hx); v = (hy - qy) / (2 * hy); }
                case 0 -> { u = (hz - qz) / (2 * hz); v = (hy - qy) / (2 * hy); }
                case 1 -> { u = (qz + hz) / (2 * hz); v = (hy - qy) / (2 * hy); }
                case 2 -> { u = (qx + hx) / (2 * hx); v = (qz + hz) / (2 * hz); }
                default -> { u = (qx + hx) / (2 * hx); v = (hz - qz) / (2 * hz); }
            }
            int c = solid;
            if (tex[f] != null) {
                int n = size[f];
                c = tex[f][Math.min(n - 1, Math.max(0, (int) (v * n))) * n + Math.min(n - 1, Math.max(0, (int) (u * n)))];
                if (c >>> 24 < 16) return false;                          // see-through texel (hat layer)
            }
            h.t = tmin; h.argb = c; h.nx = h.ny = h.nz = 0;
            if (axis == 0) h.nx = sgn; else if (axis == 1) h.ny = sgn; else h.nz = sgn;
            return true;
        }
    }

    /** A flat sprite extruded into a slab (voxel grid, centred on the origin, 1 unit per voxel). */
    static final class Slab extends Obj {
        final int w, h, d; final int[] sprite;
        Slab(int[] sprite, int w, int h, int d) { this.sprite = sprite; this.w = w; this.h = h; this.d = d; }

        @Override boolean local(double ox, double oy, double oz, double dx, double dy, double dz, Hit hit) {
            double[] o = {ox, oy, oz}, dd = {dx, dy, dz}, e = {w / 2.0, h / 2.0, d / 2.0};
            double tmin = -1e18, tmax = 1e18; int axis = 0; double sgn = 1;
            for (int a = 0; a < 3; a++) {
                if (Math.abs(dd[a]) < 1e-12) { if (Math.abs(o[a]) > e[a]) return false; continue; }
                double t1 = (-e[a] - o[a]) / dd[a], t2 = (e[a] - o[a]) / dd[a], near = Math.min(t1, t2), far = Math.max(t1, t2);
                if (near > tmin) { tmin = near; axis = a; sgn = dd[a] > 0 ? -1 : 1; }
                tmax = Math.min(tmax, far);
            }
            if (tmin > tmax || tmax < 0) return false;
            double t = Math.max(tmin, 0) + 1e-7;
            double px = ox + t * dx + e[0], py = e[1] - (oy + t * dy), pz = oz + t * dz + e[2];   // voxel space: x right, y down, z toward viewer
            int ix = clamp((int) Math.floor(px), w), iy = clamp((int) Math.floor(py), h), iz = clamp((int) Math.floor(pz), d);
            int sx = dx > 0 ? 1 : -1, sy = -dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;
            double ndx = Math.abs(dx) < 1e-12 ? 1e18 : Math.abs(1 / dx), ndy = Math.abs(dy) < 1e-12 ? 1e18 : Math.abs(1 / dy), ndz = Math.abs(dz) < 1e-12 ? 1e18 : Math.abs(1 / dz);
            double tx = dx == 0 ? 1e18 : t + ((sx > 0 ? ix + 1 - px : px - ix) * ndx), ty = dy == 0 ? 1e18 : t + ((sy > 0 ? iy + 1 - py : py - iy) * ndy), tz = dz == 0 ? 1e18 : t + ((sz > 0 ? iz + 1 - pz : pz - iz) * ndz);
            double nx = 0, ny = 0, nz = 0;
            if (axis == 0) nx = sgn; else if (axis == 1) ny = sgn; else nz = sgn;
            double ct = t;
            for (int i = 0; i < w + h + d + 4; i++) {
                if (ix < 0 || iy < 0 || iz < 0 || ix >= w || iy >= h || iz >= d) return false;
                int c = sprite[iy * w + ix];
                if (c >>> 24 > 24) { hit.t = ct; hit.argb = c | 0xFF000000; hit.nx = nx; hit.ny = ny; hit.nz = nz; return true; }
                nx = ny = nz = 0;
                if (tx < ty && tx < tz) { ix += sx; ct = tx; tx += ndx; nx = -sx; }
                else if (ty < tz) { iy += sy; ct = ty; ty += ndy; ny = sy; }           // voxel y is flipped relative to local y
                else { iz += sz; ct = tz; tz += ndz; nz = -sz; }
            }
            return false;
        }
        private static int clamp(int v, int n) { return Math.max(0, Math.min(n - 1, v)); }
    }

    private static final double LX = -0.35, LY = 0.85, LZ = 0.55;
    private static final double LN = Math.sqrt(LX * LX + LY * LY + LZ * LZ);

    /** Draws the objects into out. World origin sits at cell (cx0, cy0); s = cells per world unit; tilt = camera pitch (radians, looking down). */
    static void render(Canvas out, Obj[] objs, double cx0, double cy0, double s, double tilt) {
        for (Obj o : objs) if (o != null) o.update();
        double ct = Math.cos(tilt), st = Math.sin(tilt);
        Hit best = new Hit(), tmp = new Hit();
        double dirX = 0, dirY = -st, dirZ = -ct;                     // view (0,0,-1) rotated back into the world
        for (int j = 0; j < out.h; j++) for (int i = 0; i < out.w; i++) {
            double vx = (i + 0.5 - cx0) / s, vy = -(j + 0.5 - cy0) / s, vz = 200;
            double ox = vx, oy = vy * ct + vz * st, oz = -vy * st + vz * ct;
            best.t = 1e18; Obj hit = null;
            for (Obj o : objs) {
                if (o == null || !o.cast(ox, oy, oz, dirX, dirY, dirZ, tmp)) continue;
                if (tmp.t < best.t) { best.t = tmp.t; best.argb = tmp.argb; best.nx = tmp.nx; best.ny = tmp.ny; best.nz = tmp.nz; hit = o; }
            }
            if (hit == null) continue;
            double dot = Math.max(0, (best.nx * LX + best.ny * LY + best.nz * LZ) / LN);
            out.set(i, j, Canvas.shade(best.argb, (float) ((0.4 + 0.78 * Math.pow(dot, 0.85)) * hit.bright)));
        }
    }
    private Render3D() {}
}
