package dev.fancydrops;

/** Procedural pixel textures (n x n ARGB) for the board, the mob cube, its insides and the cleaver. No image files needed. */
final class Tex {
    static float h(int a, int b) { int x = a * 374761393 + b * 668265263; x = (x ^ (x >>> 13)) * 1274126177; x ^= x >>> 16; return (x & 0xFFFF) / 65535f; }

    static int[] noise(int n, int base, int dark, int seed) {
        int[] t = new int[n * n];
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            float v = h(x + seed * 31, y * 7 + seed);
            t[y * n + x] = Canvas.mix(base, dark, v < 0.3f ? 0.85f : v < 0.55f ? 0.4f : 0f);
        }
        return t;
    }

    /** Front of the mob cube: noisy skin, two eyes, a mouth. */
    static int[] mobFront(int n, int base, int dark, int eye, int seed) {
        int[] t = noise(n, base, dark, seed);
        int ey = n * 5 / 12, ew = Math.max(2, n / 6);
        for (int e = 0; e < 2; e++) {
            int ex = e == 0 ? n / 4 - ew / 2 : n * 3 / 4 - ew / 2;
            for (int y = ey; y < ey + ew; y++) for (int x = ex; x < ex + ew; x++) t[y * n + x] = (x == ex && y == ey) ? 0xFFFFFFFF : eye;
        }
        for (int x = n / 3; x < n * 2 / 3; x++) t[(n * 3 / 4) * n + x] = Canvas.shade(dark, 0.6f);
        return t;
    }



    static int[] steel(int n) {
        int[] t = new int[n * n];
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            float f = 0.82f + 0.18f * (1f - y / (float) n);
            int c = Canvas.shade(0xFFD7DCE3, f);
            if (y >= n - 2) c = 0xFFFFFFFF;                                  // bright cutting edge
            else if (y < 2) c = 0xFF9AA2AE;
            t[y * n + x] = c;
        }
        return t;
    }

    static int[] solid(int n, int c) { int[] t = new int[n * n]; java.util.Arrays.fill(t, c); return t; }

    private Tex() {}

    // ---- Minecraft-style block textures for the floor each slayer fights on (16x16)
    private static int speck(int x, int y, int seed, int base, int dark, int light) {
        float v = h(x + seed * 17, y * 5 + seed);
        return v < 0.22f ? dark : v > 0.82f ? light : base;
    }
    private static int[] fill16(int base, int dark, int light, int seed) {
        int[] t = new int[256];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) t[y * 16 + x] = speck(x, y, seed, base, dark, light) | 0xFF000000;
        return t;
    }
    private static int[] bricks(int stone, int stoneDark, int mortar, int seed, int bw, int bh) {
        int[] t = new int[256];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int row = y / bh, ox = row % 2 == 0 ? 0 : bw / 2;
            boolean m = y % bh == bh - 1 || (x + ox) % bw == bw - 1;
            t[y * 16 + x] = (m ? mortar : speck(x, y, seed, stone, stoneDark, stone)) | 0xFF000000;
        }
        return t;
    }
    private static int[] cobble(int seed) {
        int[] t = new int[256];
        int[][] c = new int[16][16]; int id = 0;
        for (int cy = 0; cy < 4; cy++) for (int cx = 0; cx < 4; cx++) { id++; int x0 = cx * 4 + (cy % 2) * 2 - 1, y0 = cy * 4 + (cx % 2); for (int y = y0; y < y0 + 4; y++) for (int x = x0; x < x0 + 4; x++) c[((y % 16) + 16) % 16][((x % 16) + 16) % 16] = id; }
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            boolean edge = c[y][x] != c[(y + 1) % 16][x] || c[y][x] != c[y][(x + 1) % 16];
            float v = h(c[y][x] * 7, seed);
            int base = v < 0.33f ? 0x8A8A8A : v < 0.66f ? 0x747474 : 0x666666;
            t[y * 16 + x] = (edge ? 0x4A4A4A : base) | 0xFF000000;
        }
        return t;
    }
    private static int[] grassSide(int seed) {
        int[] t = fill16(0x866043, 0x593D29, 0x9A7355, seed);
        for (int x = 0; x < 16; x++) { int d = 3 + (int) (h(x, seed) * 3); for (int y = 0; y < d; y++) t[y * 16 + x] = speck(x, y, seed + 2, 0x5D9B3A, 0x4A8030, 0x72B04A) | 0xFF000000; }
        return t;
    }

    /** The floor block for a slayer theme: top = the walkable face, otherwise the side. theme 0..6 as in Scene. */
    static int[] floor(int theme, boolean top, int seed) {
        switch (theme) {
            case 0: { // zombie: coarse dirt and moss - a graveyard
                if (top) { int[] t = fill16(0x6B4A2B, 0x4E3820, 0x85603B, seed);
                    for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if (h(x / 2 + seed, y / 2 + 9) > 0.78f) t[y * 16 + x] = speck(x, y, seed + 4, 0x3E6B2A, 0x2E5220, 0x4F8236) | 0xFF000000;
                    return t; }
                int[] t = fill16(0x6B4A2B, 0x4E3820, 0x85603B, seed + 1);
                for (int x = 0; x < 16; x++) { int d = 2 + (int) (h(x, seed) * 3); for (int y = 0; y < d; y++) t[y * 16 + x] = speck(x, y, seed + 2, 0x3E6B2A, 0x2E5220, 0x4F8236) | 0xFF000000; }
                return t; }
            case 1: { // spider: cobblestone with cobwebs on top
                int[] t = cobble(seed);
                if (top) for (int i = 0; i < 16; i++) { for (int[] p : new int[][]{{i, i}, {15 - i, i}, {i, 8}, {8, i}}) t[p[1] * 16 + p[0]] = 0xFFE8E8EE;
                    int r = 5; for (int k = 0; k < 16; k++) { double a = k * Math.PI / 8; t[(int) Math.round(8 + Math.sin(a) * r) * 16 + (int) Math.round(8 + Math.cos(a) * r)] = 0xFFDADAE4; } }
                return t; }
            case 2: return top ? fill16(0x5D9B3A, 0x4A8030, 0x72B04A, seed) : grassSide(seed);        // wolf: grass block park
            case 3: return top ? fill16(0xDCDFA0, 0xC8CC8A, 0xEBEDB5, seed) : bricks(0xDCDFA0, 0xC8CC8A, 0xB4B77A, seed, 8, 4);   // enderman: end stone / end stone bricks
            case 4: return top ? fill16(0x7A2E2E, 0x5C2020, 0x8F3C3A, seed) : bricks(0x3B1A20, 0x2E1218, 0x1E0B10, seed, 8, 4);    // blaze: netherrack / nether bricks
            case 5: { // vampire: dark oak planks
                int[] t = new int[256];
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { boolean line = y % 4 == 3; float v = h(x + (y / 4) * 5, seed + y / 4);
                    t[y * 16 + x] = (line ? 0x2A1B0C : v < 0.3f ? 0x3A260F : v > 0.8f ? 0x4A3318 : 0x402B14) | 0xFF000000; }
                return t; }
            default: return noise(16, 0xFF59546B, 0xFF35323F, seed + 2);
        }
    }
}
