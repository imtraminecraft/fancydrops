package dev.fancydrops;

import net.minecraft.network.chat.Style;

/** Effects for a player's name (IGN). They keep the colour the server gave the name and add light and movement on top. The Beta ones are gentle, the Special ones are loud. */
final class NameFx {
    private NameFx() {}

    /** Does it move? (The soft glow is the only still one.) */
    static boolean animated(String fx) { return !"glow".equals(fx); }

    private static int mix(int a, int b, float k) {
        k = Math.max(0f, Math.min(1f, k));
        int r = Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k), g = Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k), bl = Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }
    private static int hash(int i, long step) {
        int h = i * 73856093 ^ (int) step * 19349663;
        h ^= h >>> 13; h *= 0x5bd1e995; h ^= h >>> 15;
        return Math.abs(h);
    }

    /** The colour (RGB) of letter i of n at time t (ms), starting from the name's own colour. */
    static int color(String fx, int i, int n, int base, long t) {
        base &= 0xFFFFFF;
        float ph;
        switch (fx) {
            case "shimmer": {                                              // a soft band of light slides over the name
                float pos = (t % 2600L) / 2600f * (n + 8) - 4, k = Math.max(0f, 1f - Math.abs(i - pos) / 3f);
                return mix(base, 0xFFFFFF, 0.6f * k);
            }
            case "pulse":                                                   // the whole name breathes a little
                ph = 0.5f + 0.5f * (float) Math.sin(t / 700.0);
                return mix(mix(base, 0x000000, 0.18f), mix(base, 0xFFFFFF, 0.35f), ph);
            case "wave":                                                    // light and shade ripple along the letters
                ph = 0.5f + 0.5f * (float) Math.sin(t / 380.0 - i * 0.8);
                return mix(mix(base, 0x000000, 0.22f), mix(base, 0xFFFFFF, 0.3f), ph);
            case "glow":                                                    // a still fade from the name's colour to a lighter tint
                return mix(base, 0xFFFFFF, n <= 1 ? 0f : 0.5f * i / (n - 1));
            case "royal": {                                                 // liquid gold, with a white glint that runs along the name
                ph = 0.5f + 0.5f * (float) Math.sin(t / 240.0 - i * 0.9);
                int c = mix(0xFFAA00, 0xFFF3A0, ph);
                float pos = (t % 1900L) / 1900f * (n + 10) - 5, k = Math.max(0f, 1f - Math.abs(i - pos) / 2.2f);
                return mix(c, 0xFFFFFF, k);
            }
            case "glitch": {                                                // the name's colour, torn by flashes of green, magenta and cyan; now and then the whole name goes wild
                int h = hash(i, t / 90L) % (burst(t) ? 5 : 10);
                return h == 0 ? 0x55FF55 : h == 1 ? 0xFF55FF : h == 2 ? 0x55FFFF : h == 3 ? (burst(t) ? 0xFF5555 : 0xFFFFFF) : base;
            }
            default: return base;
        }
    }

    private static boolean burst(long t) { return hash(7, t / 900L) % 3 == 0 && (t % 900L) < 240L; }

    /** The style for letter i: the name's own style with the effect's colour. live = animated; false = a frozen look (only used if chat can't animate). */
    static Style style(String fx, int i, int n, Style base, boolean live, long t) {
        int b = base.getColor() == null ? 0xFFFFFF : base.getColor().getValue();
        long tt = live ? t : 1500L + i * 90L;
        Style st = base.withColor(color(fx, i, n, b, tt));
        if ("glitch".equals(fx)) st = st.withObfuscated(live ? hash(i, t / 110L) % (burst(t) ? 3 : 9) == 0 : i % 5 == 2);
        else if ("royal".equals(fx)) st = st.withBold(color(fx, i, n, b, tt) == 0xFFFFFF);
        return st;
    }
}
