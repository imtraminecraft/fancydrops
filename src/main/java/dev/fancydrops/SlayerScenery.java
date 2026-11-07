package dev.fancydrops;

/**
 * Shared pieces of every scene's place: the rounded frame of a panel, a ridge of far hills and a little pine.
 * (The slayer places themselves are now in SlayerShow, painted through ShowScenery like every Showcase place.)
 */
final class SlayerScenery {
    private SlayerScenery() {}

    private static int mix(int a, int b, float f) { return Ritual.mixc(a, b, Math.max(0f, Math.min(1f, f))); }
    private static void px(Canvas c, int x, int y, int rgb, float a) { c.set(x, y, Ritual.A(a, rgb)); }
    private static void rect(Canvas c, int x0, int y0, int x1, int y1, int rgb, float a) { c.rect(x0, y0, x1, y1, Ritual.A(a, rgb)); }

    /** The rounded frame (erase = false), or the clearing of everything outside its four round corners (erase = true). */
    /** Set by Ritual for each frame: true while an RNG scene paints, so only RNG gets the living border (Normal keeps a faint plain edge). t: its seconds. */
    static boolean rngBorder; static float borderT;
    static void corners(Canvas c, int x0, int y0, int x1, int y1, int r, int rgb, float a, boolean erase) {
        if (!erase && !rngBorder) a *= 0.4f;                                                   // Normal: a quiet, thin edge
        if (!erase) {
            for (int x = x0 + r; x < x1 - r; x++) { px(c, x, y0, rgb, a * 0.85f); px(c, x, y1 - 1, rgb, a * 0.85f); }
            for (int y = y0 + r; y < y1 - r; y++) { px(c, x0, y, rgb, a * 0.85f); px(c, x1 - 1, y, rgb, a * 0.85f); }
            if (rngBorder) {                                                                   // RNG: a second inner line, two sparks racing round it, and its colour breathing to white
                int w = x1 - x0 - 2 * r, h = y1 - y0 - 2 * r, per = 2 * (w + h); float br = 0.5f + 0.5f * (float) Math.sin(borderT * 4f);
                int lit = Ritual.mixc(rgb, 0xFFFFFF, 0.35f * br);
                for (int x = x0 + r; x < x1 - r; x++) { px(c, x, y0 + 2, lit, a * 0.45f); px(c, x, y1 - 3, lit, a * 0.45f); }
                for (int y = y0 + r; y < y1 - r; y++) { px(c, x0 + 2, y, lit, a * 0.45f); px(c, x1 - 3, y, lit, a * 0.45f); }
                for (int s = 0; s < 2; s++) for (int tail = 0; tail < 26; tail++) {
                    int d = Math.floorMod(Math.round(borderT * per * 0.35f) + s * per / 2 - tail, per); int x, y;
                    if (d < w) { x = x0 + r + d; y = y0; } else if (d < w + h) { x = x1 - 1; y = y0 + r + d - w; } else if (d < 2 * w + h) { x = x1 - r - (d - w - h); y = y1 - 1; } else { x = x0; y = y1 - r - (d - 2 * w - h); }
                    px(c, x, y, 0xFFFFFF, a * (1f - tail / 26f));
                }
            }
        }
        for (int q = 0; q < 4; q++) {
            int cx = q % 2 == 0 ? x0 + r : x1 - r, cy = q < 2 ? y0 + r : y1 - r;
            for (int dy = 0; dy < r; dy++) for (int dx = 0; dx < r; dx++) {
                int x = q % 2 == 0 ? cx - 1 - dx : cx + dx, y = q < 2 ? cy - 1 - dy : cy + dy;
                double d = Math.hypot(dx + 0.5, dy + 0.5);
                if (erase) { if (d > r + 0.8) c.px[y * c.w + x] = 0; }
                else if (Math.abs(d - r) < 0.8) px(c, x, y, rgb, a * 0.85f);
            }
        }
    }

    // ---- shared pieces
    /** A ridge of far hills or rooftops: its height is a few waves, so it never repeats exactly. */
    static void ridge(Canvas c, int x0, int x1, int hz, float amp, float freq, float ph, int rgb, float a) {
        for (int x = x0; x < x1; x++) { int h = (int) (amp * (0.55f + 0.3f * Math.sin(x * freq + ph) + 0.15f * Math.sin(x * freq * 2.7f + ph * 2))); rect(c, x, hz - h, x + 1, hz, rgb, a); }
    }
    static void pine(Canvas c, int x, int base, int h, int rgb, float a) {
        rect(c, x, base - 3, x + 1, base, mix(rgb, 0x3B2A1A, 0.5f), a);
        for (int i = 0; i < h; i++) { int w = 1 + (int) ((i / (float) h) * h * 0.38f); rect(c, x - w / 2, base - h + i - 2, x + (w + 1) / 2, base - h + i - 1, mix(rgb, 0xFFFFFF, i % 4 == 0 ? 0.08f : 0f), a); }
    }
}
