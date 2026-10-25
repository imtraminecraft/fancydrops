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
            case "shimmer": {                                              // a bright band of light sweeps over the name, which keeps its own colour
                float pos = (t % 2400L) / 2400f * (n + 6) - 3, k = Math.max(0f, 1f - Math.abs(i - pos) / 1.6f);
                return mix(mix(base, 0x000000, 0.12f), 0xFFFFFF, 0.9f * k);
            }
            case "pulse":                                                   // the whole name breathes, dimmer to brighter, in its own colour
                ph = 0.5f + 0.5f * (float) Math.sin(t / 500.0);
                return mix(mix(base, 0x000000, 0.28f), mix(base, 0xFFFFFF, 0.4f), ph);
            case "wave":                                                    // waves of light and shade run along the letters
                ph = 0.5f + 0.5f * (float) Math.sin(t / 300.0 - i * 0.9);
                return mix(mix(base, 0x000000, 0.3f), mix(base, 0xFFFFFF, 0.45f), ph);
            case "glow":                                                    // a soft glow that slowly brightens from the left
                ph = 0.5f + 0.5f * (float) Math.sin(t / 900.0);
                return mix(base, 0xFFFFFF, n <= 1 ? 0.2f * ph : (0.1f + 0.35f * ph) * i / (n - 1) + 0.1f * ph);
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
            case "glitch": {                                                // hot red-orange; a warning stripe (amber, then dark red) sweeps over it; a letter flashes white; now and then the whole name blinks like a siren
                int c = mix(0xFF4A2E, 0xFF8A2E, 0.5f + 0.5f * (float) Math.sin(t / 650.0 - i * 0.5));
                float pos = (t % 1800L) / 1800f * (n + 8) - 3, d = i - pos;
                if (d > -1.3f && d <= 0.3f) c = 0xFFD23D;
                else if (d > -2.8f && d <= -1.3f) c = 0xB01E14;
                long cyc = t % 3000L;
                if (cyc < 420L) c = ((i + (int) (t / 90L)) & 1) == 0 ? 0xFF3A2A : 0xFFC03D;
                long st = t / 800L; float en = (float) Math.sin(Math.PI * ((t % 800L) / 800f));
                if (n > 0 && hash(3, st) % n == i) c = mix(c, 0xFFFFFF, 0.9f * en);
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
            case "fresh":                                                   // a lighter shade of the name drifts slowly along it
                ph = 0.5f + 0.5f * (float) Math.sin(t / 900.0 - i * 0.6);
                return mix(base, 0xFFFFFF, 0.5f * ph);
            case "rainbow": {                                               // a rainbow glint runs along the name; the rest stays its own colour
                float pos = (t % 2600L) / 2600f * (n + 8) - 4, k = Math.max(0f, 1f - Math.abs(i - pos) / 2.4f);
                int rb = java.awt.Color.HSBtoRGB(((t / 900f) + i * 0.12f) % 1f, 0.6f, 1f) & 0xFFFFFF;
                return mix(base, rb, 0.9f * k);
            }
            case "aurora": {                                                // soft green and violet light curtains drift over the name
                float a1 = 0.5f + 0.5f * (float) Math.sin(t / 900.0 - i * 0.5), a2 = 0.5f + 0.5f * (float) Math.sin(t / 1400.0 + i * 0.35);
                int cur = mix(0x3DFFA0, 0xB07CFF, a2);
                return mix(base, cur, 0.5f * a1 * a1);
            }
            case "ember": {                                                 // warm flickers rise through the letters, and now and then one burns bright
                float fl = 0.5f + 0.5f * (float) Math.sin(t / 380.0 - i * 0.8);
                int c = mix(mix(base, 0x000000, 0.12f), 0xFF8A2E, 0.35f * fl);
                long st = t / 160L; float h = (hash(5, st + i) % 100) / 100f;
                if (h > 0.84f) c = mix(c, 0xFFE27A, (h - 0.84f) * 6f);
                return c;
            }
            case "frost": {                                                 // a cold glint runs along the name and a letter frosts over white
                float pos = (t % 2800L) / 2800f * (n + 8) - 4, k = Math.max(0f, 1f - Math.abs(i - pos) / 1.8f);
                int c = mix(base, 0xBFE4FF, 0.25f + 0.2f * (float) Math.sin(t / 1100.0 - i * 0.6));
                long st = t / 900L; float en = (float) Math.sin(Math.PI * ((t % 900L) / 900f));
                if (n > 0 && hash(13, st) % n == i) c = mix(c, 0xFFFFFF, 0.9f * en);
                return mix(c, 0xFFFFFF, 0.85f * k);
            }
            case "neon": {                                                  // letters light up in turn like a sign, and the whole name flickers now and then
                float lit = 0.5f + 0.5f * (float) Math.sin(t / 420.0 - i * 1.1);
                int c = mix(mix(base, 0x000000, 0.1f), mix(i % 2 == 0 ? 0x35F0FF : 0xFF4FE0, 0xFFFFFF, 0.3f), 0.55f * lit * lit);
                if (t % 4200L < 120L && i % 3 == 0) c = mix(c, 0x000000, 0.55f);
                return c;
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
        return st;
    }
}
