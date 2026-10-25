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
            case "fresh":                                                   // a mint-green name with a lighter green that drifts slowly along it
                base = tint(0x57D68D, base);
                ph = 0.5f + 0.5f * (float) Math.sin(t / 900.0 - i * 0.6);
                return mix(base, 0xD8FFE6, 0.6f * ph);
            case "rainbow": {                                               // the whole spectrum rolls along the name
                float h = ((t / 1800f) + i * 0.09f) % 1f;
                return java.awt.Color.HSBtoRGB(h, 0.55f, 1f) & 0xFFFFFF;
            }
            case "aurora": {                                                // northern lights: green, teal and violet curtains drifting through the letters
                float a1 = 0.5f + 0.5f * (float) Math.sin(t / 900.0 - i * 0.5), a2 = 0.5f + 0.5f * (float) Math.sin(t / 1400.0 + i * 0.35);
                int c = mix(mix(0x3DFFA0, 0x35D8E8, a1), 0xB07CFF, 0.65f * a2 * a2);
                return mix(c, 0xFFFFFF, 0.12f * a1);
            }
            case "ember": {                                                 // glowing coals: deep red with flickers of orange and yellow rising through it
                int c = mix(0xC8281A, 0xFF7A1F, 0.5f + 0.5f * (float) Math.sin(t / 380.0 - i * 0.8));
                long st = t / 160L; float fl = (hash(5, st + i) % 100) / 100f;
                if (fl > 0.82f) c = mix(c, 0xFFE27A, (fl - 0.82f) * 5f);
                return c;
            }
            case "frost": {                                                 // pale ice, a cold glint runs along it and a letter frosts over white
                base = tint(0xA8DCFF, base);
                float pos = (t % 2800L) / 2800f * (n + 8) - 4, k = Math.max(0f, 1f - Math.abs(i - pos) / 1.8f);
                int c = mix(mix(base, 0xE8F6FF, 0.35f), 0x7FC4FF, 0.5f + 0.5f * (float) Math.sin(t / 1100.0 - i * 0.6));
                long st = t / 900L; float en = (float) Math.sin(Math.PI * ((t % 900L) / 900f));
                if (n > 0 && hash(13, st) % n == i) c = mix(c, 0xFFFFFF, 0.9f * en);
                return mix(c, 0xFFFFFF, 0.85f * k);
            }
            case "neon": {                                                  // a sign: cyan and magenta trade places, with a flicker now and then
                boolean alt = (i + (int) (t / 700L)) % 2 == 0;
                int c = alt ? 0x35F0FF : 0xFF4FE0;
                long cyc = t % 4200L; if (cyc < 120L && i % 3 == 0) c = mix(c, 0x201030, 0.7f);
                return mix(c, 0xFFFFFF, 0.25f * (0.5f + 0.5f * (float) Math.sin(t / 260.0 - i)));
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
