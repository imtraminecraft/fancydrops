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

    /** These effects have their own hue. A white or grey name (the tab list, many ranks) takes it fully; a coloured one is blended halfway, so it still shows its own colour. */
    private static int tint(int own, int base) {
        int r = (base >> 16) & 255, g = (base >> 8) & 255, b = base & 255;
        boolean plain = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) < 40;
        return plain ? own : mix(base, own, 0.5f);
    }

    /** The colour (RGB) of letter i of n at time t (ms), starting from the name's own colour. */
    static int color(String fx, int i, int n, int base, long t) {
        base &= 0xFFFFFF;
        float ph;
        switch (fx) {
            case "shimmer": {                                              // an ice-blue name with a bright band of light that sweeps over it
                base = tint(0x8FB8FF, base);
                float pos = (t % 2400L) / 2400f * (n + 6) - 3, k = Math.max(0f, 1f - Math.abs(i - pos) / 1.6f);
                return mix(mix(base, 0x000000, 0.22f), 0xFFFFFF, 0.9f * k);
            }
            case "pulse":                                                   // the whole name breathes, soft pink to bright
                base = tint(0xFF7FB8, base);
                ph = 0.5f + 0.5f * (float) Math.sin(t / 500.0);
                return mix(mix(base, 0x000000, 0.3f), mix(base, 0xFFFFFF, 0.5f), ph);
            case "wave":                                                    // waves of teal light and shade run along the letters
                base = tint(0x3FD8C4, base);
                ph = 0.5f + 0.5f * (float) Math.sin(t / 300.0 - i * 0.9);
                return mix(mix(base, 0x000000, 0.4f), mix(base, 0xFFFFFF, 0.55f), ph);
            case "glow":                                                    // a warm gold name that slowly breathes towards white from the left
                base = tint(0xFFC93D, base);
                ph = 0.5f + 0.5f * (float) Math.sin(t / 900.0);
                return mix(mix(base, 0x000000, 0.1f), 0xFFFFFF, n <= 1 ? 0.2f * ph : (0.12f + 0.4f * ph) * i / (n - 1) + 0.12f * ph);
            case "royal": {                                                 // liquid gold, with a white glint that runs along the name
                ph = 0.5f + 0.5f * (float) Math.sin(t / 240.0 - i * 0.9);
                int c = mix(0xFFAA00, 0xFFF3A0, ph);
                float pos = (t % 1900L) / 1900f * (n + 10) - 5, k = Math.max(0f, 1f - Math.abs(i - pos) / 2.2f);
                return mix(c, 0xFFFFFF, k);
            }
            case "sparklefx": {                                             // gold that flows from amber to cream, a white comet with a pink-gold tail runs along it, and single letters flare up softly
                ph = 0.5f + 0.5f * (float) Math.sin(t / 700.0 - i * 0.55);
                int c = mix(mix(0xE8A21C, 0xFFF1B8, ph), mix(base, 0xFFFFFF, 0.2f), 0.2f);
                float pos = (t % 2600L) / 2600f * (n + 12) - 4, d = pos - i;                     // d > 0: the letter is behind the comet's head
                if (d >= -0.4f && d < 0.9f) c = 0xFFFFFF;
                else if (d >= 0.9f && d < 5f) c = mix(0xFFFFFF, mix(0xFFB6E6, c, 0.55f), (d - 0.9f) / 4.1f);
                long st = t / 700L; float en = (float) Math.sin(Math.PI * ((t % 700L) / 700f));        // one letter at a time swells to white and fades back
                if (n > 0 && hash(7, st) % n == i) c = mix(c, 0xFFFFFF, 0.85f * en);
                else if (n > 1 && hash(11, st) % n == i) c = mix(c, 0xFFD6F2, 0.7f * en);
                return c;
            }
            case "glitch": {                                                // calm green with a scan line sliding over it; every couple of seconds the whole name tears: letters flash cyan / magenta / white
                float pos = (t % 1900L) / 1900f * (n + 8) - 2, d = i - pos;
                int c = mix(0x1FAE72, 0x39FFA8, 0.5f + 0.5f * (float) Math.sin(t / 500.0 - i * 0.6));
                if (d > -0.7f && d < 0.7f) c = 0xFFFFFF;
                else if (d <= -0.7f && d > -3.5f) c = mix(0xB6FFE0, c, (-d - 0.7f) / 2.8f);
                long cyc = t % 2300L;
                if (cyc < 380L) {                                          // the tear
                    int h = hash(i, t / 55L) % 6;
                    c = h == 0 ? 0xFF3DDC : h == 1 ? 0x3DFFF0 : h == 2 ? 0xFFFFFF : h == 3 ? mix(c, 0x000000, 0.5f) : c;
                } else if (hash(i, t / 90L) % 40 == 0) c = 0x3DFFF0;
                return c;
            }
            case "contribfx": {                                             // deep water to bright aqua with two waves crossing, a bright foam crest, and a light that sweeps over now and then
                float w1 = 0.5f + 0.5f * (float) Math.sin(t / 420.0 - i * 0.7), w2 = 0.5f + 0.5f * (float) Math.sin(t / 770.0 + i * 0.4);
                int c = mix(mix(0x0A7BFF, 0x3DE8FF, w1), 0x5CFFC4, 0.5f * w2 * w1);
                c = mix(c, base, 0.1f);
                float foam = (float) Math.pow(Math.max(0.0, Math.sin(t / 300.0 - i * 1.1)), 12);
                c = mix(c, 0xFFFFFF, 0.8f * foam);
                float pos = (t % 3200L) / 3200f * (n + 12) - 6, k = Math.max(0f, 1f - Math.abs(i - pos) / 3f);
                return mix(c, 0xE8FFFF, 0.7f * k);
            }
            case "fresh":                                                   // a mint-green name with a lighter green that drifts slowly along it
                base = tint(0x57D68D, base);
                ph = 0.5f + 0.5f * (float) Math.sin(t / 900.0 - i * 0.6);
                return mix(base, 0xD8FFE6, 0.6f * ph);
            default: return base;
        }
    }

    /** The style for letter i: the name's own style with the effect's colour. live = animated; false = a frozen look (only used if chat can't animate). */
    static Style style(String fx, int i, int n, Style base, boolean live, long t) {
        int b = base.getColor() == null ? 0xFFFFFF : base.getColor().getValue();
        long tt = live ? t : 1500L + i * 90L;
        int c = color(fx, i, n, b, tt);
        Style st = base.withColor(c);
        return st;
    }
}
