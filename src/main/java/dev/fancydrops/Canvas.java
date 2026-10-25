package dev.fancydrops;

/** A small grid of pixel-art cells. Everything in the animation is drawn here, then flushed as a few hundred rectangle fills. */
final class Canvas {
    interface Surface { void fill(int x0, int y0, int x1, int y1, int argb); }

    final int w, h;
    final int[] px;
    Canvas(int w, int h) { this.w = w; this.h = h; px = new int[w * h]; }

    void clear() { java.util.Arrays.fill(px, 0); }

    /** Source-over blend of one cell. */
    void set(int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        int a = argb >>> 24;
        if (a == 0) return;
        int i = y * w + x, d = px[i], da = d >>> 24;
        if (a == 255 || da == 0) { px[i] = argb; return; }
        int oa = a + da * (255 - a) / 255;
        int r = Math.min(255, (((argb >> 16) & 255) * a + ((d >> 16) & 255) * da * (255 - a) / 255) / oa);      // rounding could reach 256 and wrap round to 0
        int g = Math.min(255, (((argb >> 8) & 255) * a + ((d >> 8) & 255) * da * (255 - a) / 255) / oa);
        int b = Math.min(255, ((argb & 255) * a + (d & 255) * da * (255 - a) / 255) / oa);
        px[i] = (oa << 24) | (r << 16) | (g << 8) | b;
    }

    void rect(int x0, int y0, int x1, int y1, int argb) {
        for (int y = Math.max(0, y0); y < Math.min(h, y1); y++) for (int x = Math.max(0, x0); x < Math.min(w, x1); x++) set(x, y, argb);
    }

    void disc(int cx, int cy, int r, int argb) {
        for (int y = -r; y <= r; y++) for (int x = -r; x <= r; x++) if (x * x + y * y <= r * r) set(cx + x, cy + y, argb);
    }

    void draw(Canvas src) { for (int i = 0; i < px.length; i++) if (src.px[i] >>> 24 != 0) set(i % w, i / w, src.px[i]); }

    /** One-cell black outline around everything already drawn (the thick outline of the reference scene). */
    void outline(int argb) {
        boolean[] add = new boolean[px.length];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (px[y * w + x] >>> 24 != 0) continue;
            if ((x > 0 && px[y * w + x - 1] >>> 24 > 200) || (x < w - 1 && px[y * w + x + 1] >>> 24 > 200)
                    || (y > 0 && px[(y - 1) * w + x] >>> 24 > 200) || (y < h - 1 && px[(y + 1) * w + x] >>> 24 > 200)) add[y * w + x] = true;
        }
        for (int i = 0; i < add.length; i++) if (add[i]) px[i] = argb;
    }


    /** Icon-style outline: not black, but a deep shade of the colour it hugs, so shapes stay clean and glowing. */
    void softOutline(float dark) {
        int[] add = new int[px.length];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (px[y * w + x] >>> 24 != 0) continue;
            int r = 0, g = 0, b = 0, n = 0;
            for (int k = 0; k < 4; k++) {
                int nx = x + (k == 0 ? -1 : k == 1 ? 1 : 0), ny = y + (k == 2 ? -1 : k == 3 ? 1 : 0);
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                int p = px[ny * w + nx];
                if (p >>> 24 <= 200) continue;
                r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255; n++;
            }
            if (n == 0) continue;
            add[y * w + x] = 0xD8000000 | (((int) (r / n * dark) & 255) << 16) | (((int) (g / n * dark) & 255) << 8) | ((int) (b / n * dark) & 255);
        }
        for (int i = 0; i < add.length; i++) if (add[i] != 0) px[i] = add[i];
    }

    /** A soft round glow: bright in the middle, fading smoothly to nothing (the halo behind icons). */
    void glow(float cx, float cy, float r, int rgb, float strength) {
        int x0 = (int) Math.floor(cx - r), x1 = (int) Math.ceil(cx + r), y0 = (int) Math.floor(cy - r), y1 = (int) Math.ceil(cy + r);
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            float ddx = x + 0.5f - cx, ddy = y + 0.5f - cy, d = (float) Math.sqrt(ddx * ddx + ddy * ddy) / r;
            if (d >= 1f) continue;
            float f = (1f - d) * (1f - d);
            int a = (int) (255 * strength * f);
            if (a > 0) set(x, y, rgb(a, rgb));
        }
    }

    /** A four-point sparkle with tapering arms and a white heart, like the icons' glints. */
    void glint(int cx, int cy, int r, int rgb, float alpha) {
        for (int i = 1; i <= r; i++) {
            int a = (int) (alpha * 230 * (1f - (i - 1f) / (r + 0.5f)));
            set(cx + i, cy, rgb(a, rgb)); set(cx - i, cy, rgb(a, rgb)); set(cx, cy + i, rgb(a, rgb)); set(cx, cy - i, rgb(a, rgb));
        }
        if (r >= 3) { int a = (int) (alpha * 150); set(cx - 1, cy - 1, rgb(a, rgb)); set(cx + 1, cy - 1, rgb(a, rgb)); set(cx - 1, cy + 1, rgb(a, rgb)); set(cx + 1, cy + 1, rgb(a, rgb)); }
        set(cx, cy, rgb((int) (alpha * 255), 0xFFFFFF));
    }

    /** Emits the canvas as horizontal runs of equal colour. */
    void flush(Surface s, int ox, int oy, int cell, float alpha) {
        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                int c = px[y * w + x];
                if (c >>> 24 == 0) { x++; continue; }
                int run = 1;
                while (x + run < w && px[y * w + x + run] == c) run++;
                int a = (int) ((c >>> 24) * alpha);
                if (a > 2) s.fill(ox + x * cell, oy + y * cell, ox + (x + run) * cell, oy + (y + 1) * cell, (a << 24) | (c & 0xFFFFFF));
                x += run;
            }
        }
    }

    static int rgb(int a, int rgb) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }
    static int mix(int a, int b, float f) {
        f = Math.max(0f, Math.min(1f, f));
        int r = (int) (((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f), g = (int) (((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f), bl = (int) ((a & 255) * (1 - f) + (b & 255) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
    static int shade(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 255) * f)), g = Math.min(255, (int) (((argb >> 8) & 255) * f)), b = Math.min(255, (int) ((argb & 255) * f));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
