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
            case "shimmer": {                                              // a bright band of light sweeps over the name, over and over
                float pos = (t % 2200L) / 2200f * (n + 10) - 5, k = Math.max(0f, 1f - Math.abs(i - pos) / 2.5f);
                return mix(mix(base, 0x000000, 0.12f), 0xFFFFFF, 0.95f * k);
            }
            case "pulse":                                                   // the whole name breathes: dark, then bright
                ph = 0.5f + 0.5f * (float) Math.sin(t / 450.0);
                return mix(mix(base, 0x000000, 0.45f), mix(base, 0xFFFFFF, 0.65f), ph);
            case "wave":                                                    // clear waves of light and shade run along the letters
                ph = 0.5f + 0.5f * (float) Math.sin(t / 260.0 - i * 0.9);
                return mix(mix(base, 0x000000, 0.5f), mix(base, 0xFFFFFF, 0.6f), ph);
            case "glow":                                                    // the name's colour fading into a bright tint, with a slow breath
                ph = 0.5f + 0.5f * (float) Math.sin(t / 900.0);
                return mix(base, 0xFFFFFF, n <= 1 ? 0f : (0.25f + 0.55f * ph) * i / (n - 1) + 0.15f * ph);
            case "royal": {                                                 // liquid gold, with a white glint that runs along the name
                ph = 0.5f + 0.5f * (float) Math.sin(t / 240.0 - i * 0.9);
                int c = mix(0xFFAA00, 0xFFF3A0, ph);
                float pos = (t % 1900L) / 1900f * (n + 10) - 5, k = Math.max(0f, 1f - Math.abs(i - pos) / 2.2f);
                return mix(c, 0xFFFFFF, k);
            }
            case "sparklefx": {                                             // soft gold light with little white and pink twinkles popping on single letters
                ph = 0.5f + 0.5f * (float) Math.sin(t / 500.0 - i * 0.6);
                int c = mix(mix(base, 0xFFE9A8, 0.5f), 0xFFFFFF, 0.25f * ph);
                int h = hash(i, t / 160L) % 7;
                return h == 0 ? 0xFFFFFF : h == 1 ? 0xFFC8F0 : c;
            }
            case "glitch": {                                                // the name's own colour; a letter now and then flashes cyan or magenta for a blink
                int h = hash(i, t / 120L) % 14;
                return h == 0 ? 0x55FFFF : h == 1 ? 0xFF55FF : h == 2 && (t / 120L) % 5 == 0 ? 0xFFFFFF : base;
            }
            case "contribfx": {                                             // aqua and mint flow along the name like a stream
                ph = 0.5f + 0.5f * (float) Math.sin(t / 420.0 - i * 0.7);
                return mix(mix(base, 0x4DD0FF, 0.55f), 0x7CFFB8, ph * 0.9f);
            }
            default: return base;
        }
    }

    /** The style for letter i: the name's own style with the effect's colour. live = animated; false = a frozen look (only used if chat can't animate). */
    static Style style(String fx, int i, int n, Style base, boolean live, long t) {
        int b = base.getColor() == null ? 0xFFFFFF : base.getColor().getValue();
        long tt = live ? t : 1500L + i * 90L;
        int c = color(fx, i, n, b, tt);
        Style st = base.withColor(c);
        if ("royal".equals(fx)) st = st.withBold(c == 0xFFFFFF);
        return st;
    }
}
