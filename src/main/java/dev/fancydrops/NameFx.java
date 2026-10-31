package dev.fancydrops;

import net.minecraft.network.chat.Style;

/** Effects for a player's name (IGN). They keep the colour the server gave the name and add light and movement on top. Every effect is the name's main colour and white, nothing else. */
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


    /** The colour (RGB) of letter i of n at time t (ms). Every effect uses ONE main colour and white, nothing else:
     *  Beta keeps the colour the server gave the name; the Special ones have a main colour of their own. */
    static int color(String fx, int i, int n, int base, long t) {
        base &= 0xFFFFFF;
        float ph;
        switch (fx) {
            case "pulse":                                                   // Beta: the name breathes between its own colour and white
                ph = 0.5f + 0.5f * (float) Math.sin(t / 500.0);
                return mix(base, 0xFFFFFF, 0.45f * ph);
            case "sparklefx": {                                             // Starlight: pale starlight blue; a white comet runs along it and single letters twinkle white
                int main = 0xA9BBFF;
                ph = 0.5f + 0.5f * (float) Math.sin(t / 900.0 - i * 0.5);
                int c = mix(main, 0xFFFFFF, 0.18f * ph);
                float pos = (t % 2800L) / 2800f * (n + 10) - 3, d = pos - i;                       // d > 0: behind the comet's head
                if (d >= -0.4f && d < 0.9f) c = 0xFFFFFF;
                else if (d >= 0.9f && d < 4f) c = mix(0xFFFFFF, c, (d - 0.9f) / 3.1f);
                long st = t / 700L; float en = (float) Math.sin(Math.PI * ((t % 700L) / 700f));        // one letter at a time swells to white and fades back
                if (n > 0 && hash(7, st) % n == i) c = mix(c, 0xFFFFFF, 0.9f * en);
                return c;
            }
            case "glitch": {                                                // Critical: red, with a heartbeat: twice a beat ("lub-dub") a white pulse bursts from the middle of the name and runs out to both ends
                int c = 0xFF3B30; float best = 0f;
                long cyc = t % 1600L; float mid = (n - 1) / 2f, dist = Math.abs(i - mid);
                for (long b : new long[]{0L, 240L}) {
                    long dt = cyc - b; if (dt < 0 || dt >= 520L) continue;
                    float u = dt / 520f, front = u * (mid + 2.5f), k = Math.max(0f, 1f - Math.abs(dist - front) / 1.7f) * (1f - u);
                    best = Math.max(best, b == 0L ? k : k * 0.75f);
                }
                return mix(c, 0xFFFFFF, 0.92f * best);
            }
            case "contribfx": {                                             // Aqua Flow: aqua with a wave of white foam running along it
                int c = 0x38D2FF;
                float foam = (float) Math.pow(Math.max(0.0, Math.sin(t / 420.0 - i * 0.8)), 6);
                c = mix(c, 0xFFFFFF, 0.85f * foam);
                float pos = (t % 3200L) / 3200f * (n + 12) - 6, k = Math.max(0f, 1f - Math.abs(i - pos) / 3f);
                return mix(c, 0xFFFFFF, 0.6f * k);
            }
            case "royal": {                                                 // Royal: gold with a white glint that runs along the name
                int c = 0xFFB400;
                float pos = (t % 1900L) / 1900f * (n + 10) - 5, k = Math.max(0f, 1f - Math.abs(i - pos) / 2.2f);
                return mix(c, 0xFFFFFF, k);
            }
            case "shimmer": {                                               // a band of white sweeps over the name
                float pos = (t % 2400L) / 2400f * (n + 6) - 3, k = Math.max(0f, 1f - Math.abs(i - pos) / 1.6f);
                return mix(base, 0xFFFFFF, 0.9f * k);
            }
            case "wave":                                                    // white waves run along the letters
                ph = 0.5f + 0.5f * (float) Math.sin(t / 300.0 - i * 0.9);
                return mix(base, 0xFFFFFF, 0.45f * ph);
            case "glow":                                                    // a soft white glow that slowly brightens from the left
                ph = 0.5f + 0.5f * (float) Math.sin(t / 900.0);
                return mix(base, 0xFFFFFF, n <= 1 ? 0.2f * ph : (0.1f + 0.35f * ph) * i / (n - 1) + 0.1f * ph);
            case "fresh":                                                   // a lighter shade of the name drifts slowly along it
                ph = 0.5f + 0.5f * (float) Math.sin(t / 900.0 - i * 0.6);
                return mix(base, 0xFFFFFF, 0.5f * ph);
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
