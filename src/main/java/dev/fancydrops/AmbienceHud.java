package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Ambience (Visuals > Ambience), while a song plays. Everything takes the album cover's colours.
 *   ALBUM GLOW: the edges of the screen glow softly, breathing slowly.
 *   VISUALIZER: a row of dancing bars along the bottom of the screen, on both sides of the hotbar.
 *   MUSIC NOTES: notes drift up beside the hotbar, swaying.
 *   SONG WAVE: when a new song starts, a wave of its colour sweeps across the screen.
 *   WORD SPARKLES: tiny stars burst from each word of the lyrics as it is sung (drawn by LyricsHud).
 * All of it fades away while a drop animation plays or the music is paused.
 */
final class AmbienceHud {
    private AmbienceHud() {}
    private static float on; private static long last, waveAt; private static String waveFor = "";

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis(); float dt = Math.min(0.1f, (now - (last == 0 ? now : last)) / 1000f); last = now;
        Spotify.Track t = Spotify.track;
        if (t != null && !t.id().equals(waveFor)) { waveFor = t.id(); waveAt = now; }
        boolean any = c.ambGlow || c.ambNotes || c.ambBars || c.ambWave;
        boolean want = any && Spotify.playing && t != null && mc.level != null && !AnimationManager.isPlaying();
        on += ((want ? 1f : 0f) - on) * Math.min(1f, dt * 3f);
        if (on < 0.01f) return;
        draw(g, mc.font, g.guiWidth(), g.guiHeight(), now, on, c, now - waveAt, AlbumArt.accent());
    }

    static void draw(GuiGraphicsExtractor g, Font font, int W, int H, long now, float on, FancyConfig c, long sinceSong, int acc) {
        int lite = Ritual.mixc(acc, 0xFFFFFF, 0.45f), deep = Ritual.mixc(acc, 0x000000, 0.25f);
        float ts = (now % 3_600_000L) / 1000f;                                            // seconds, kept small (a float can't hold today's time in ms precisely)
        if (c.ambGlow) {                                                                 // soft light along each edge: four long glows, breathing
            float br = (0.75f + 0.25f * (float) Math.sin(ts * 0.45f)) * on * c.ambGlowStrength / 100f;
            int a = Math.round(120 * br);
            NebulaTex.glow(g, W / 2f, 0, W * 0.62f, H * 0.09f, Gfx.argb(Math.round(a * 0.7f), acc));
            NebulaTex.glow(g, W / 2f, H, W * 0.62f, H * 0.11f, Gfx.argb(a, acc));
            NebulaTex.glow(g, 0, H / 2f, W * 0.07f, H * 0.62f, Gfx.argb(a, deep));
            NebulaTex.glow(g, W, H / 2f, W * 0.07f, H * 0.62f, Gfx.argb(a, deep));
        }
        if (c.ambBars) {                                                                 // a visualizer along the bottom, either side of the hotbar
            int n = 16, bw = 5, gap = 2, hot = 100;
            for (int side = -1; side <= 1; side += 2) for (int i = 0; i < n; i++) {
                float x = W / 2f + side * (hot + i * (bw + gap)); if (x < 0 || x > W) continue;
                float lvl = Math.abs((float) Math.sin(ts * (5.9f - i * 0.2f) + i * 0.7f + side) * (float) Math.sin(ts * (2.3f - i * 0.05f) + i * 1.9f)) * (1f - i / (float) n * 0.6f);
                int bh = Math.max(1, Math.round((3 + 20 * lvl) * on)), x0 = Math.round(side < 0 ? x - bw : x);
                g.fill(x0, H - bh, x0 + bw, H, Gfx.argb(Math.round(150 * on), Ritual.mixc(acc, lite, i / (float) n)));
            }
        }
        if (c.ambNotes) {                                                                // notes float up from both sides of the hotbar in curling paths,
            for (int i = 0; i < 8; i++) {                                                    // growing then shrinking, turning from the album's colour to white, with a faint trail
                float per = 3.0f + (i % 4) * 0.55f, u = ((ts + i * 0.91f) % per) / per;
                float a = (float) Math.sin(Math.PI * Math.min(1f, u * 1.15f)) * on; if (a < 0.06f) continue;
                boolean left = i % 2 == 0; float dir = left ? -1 : 1, bx = W / 2f + dir * (100 + 14 * (i / 2 % 3)), by = H - 26;
                String n = i % 3 == 0 ? "\u266B" : "\u266A";
                for (int tr = 2; tr >= 0; tr--) {                                            // the trail: two fainter copies a moment behind
                    float uu = Math.max(0f, u - tr * 0.035f);
                    float x = bx + dir * (uu * 30 + (float) Math.sin(uu * 7 + i) * 8), y = by - uu * 78 - (float) Math.sin(uu * 3.14f) * 6;
                    float sc = (0.8f + 0.7f * (float) Math.sin(Math.PI * uu)) * (1f - tr * 0.18f), rot = dir * (float) Math.sin(uu * 5 + i) * 0.45f;
                    int col = Ritual.mixc(i % 2 == 0 ? acc : lite, 0xFFFFFF, uu * 0.7f);
                    Gfx.push(g); Gfx.translate(g, x, y); Gfx.rotate(g, rot); Gfx.scale(g, sc);
                    Gfx.txt(g, font, n, -3, -4, Gfx.argb(Math.round((tr == 0 ? 240 : 70 / tr) * a), col), tr == 0);
                    Gfx.pop(g);
                }
                if (u > 0.85f && u < 1f) { float pu = (u - 0.85f) / 0.15f; float x = bx + dir * (u * 30 + (float) Math.sin(u * 7 + i) * 8), y = by - u * 78;   // a little burst as it fades
                    for (int k = 0; k < 4; k++) { double an = k * Math.PI / 2 + i; g.fill(Math.round(x + (float) Math.cos(an) * 6 * pu), Math.round(y + (float) Math.sin(an) * 6 * pu), Math.round(x + (float) Math.cos(an) * 6 * pu) + 1, Math.round(y + (float) Math.sin(an) * 6 * pu) + 1, Gfx.argb(Math.round(200 * (1 - pu) * on), lite)); } }
            }
        }
        if (c.ambWave && sinceSong < 1600) {                                             // a new song: a wave of its colour sweeps across
            float u = sinceSong / 1600f, a = (float) Math.sin(Math.PI * u) * on;
            float x = -W * 0.2f + W * 1.4f * ease(u);
            NebulaTex.glow(g, x, H / 2f, W * 0.22f, H * 0.9f, Gfx.argb(Math.round(110 * a), acc));
            NebulaTex.glow(g, x, H / 2f, W * 0.05f, H * 0.7f, Gfx.argb(Math.round(140 * a), lite));
        }
    }
    /** Stars that burst from a word as it lands (Word sparkles): little four-pointed twinkles flying up and out. u: 0..1 since it landed; k: size. */
    static void sparkle(GuiGraphicsExtractor g, float cx, float cy, float w, float u, float a, int seed, float k) {
        if (u >= 1f || a < 0.02f) return;
        int acc = LyricsHud.colour(), lite = Ritual.mixc(acc, 0xFFFFFF, 0.5f);
        for (int i = 0; i < 6; i++) {
            double an = -Math.PI / 2 + (Ritual.rnd(seed * 7 + i) - 0.5) * 2.8; float d = (8 + 14 * Ritual.rnd(seed * 3 + i)) * k * ease(u);
            float x = cx + (Ritual.rnd(seed + i * 11) - 0.5f) * w + (float) Math.cos(an) * d, y = cy + (float) Math.sin(an) * d + u * u * 6 * k, al = (1f - u) * a;
            float r = (1.2f + 1.6f * Ritual.rnd(seed * 5 + i)) * k * (1f - 0.5f * u); int col = i % 3 == 0 ? 0xDDE8FF : i % 3 == 1 ? acc : lite;
            NebulaTex.glow(g, x, y, r * 2.2f, r * 2.2f, Gfx.argb(Math.round(120 * al), col));
            NebulaTex.glow(g, x, y, r * 2.6f, r * 0.45f, Gfx.argb(Math.round(150 * al), 0xE8F2FF)); NebulaTex.glow(g, x, y, r * 0.45f, r * 2.6f, Gfx.argb(Math.round(150 * al), 0xE8F2FF));   // the star's points
        }
    }
    private static float ease(float v) { v = Math.max(0f, Math.min(1f, v)); return 1f - (1f - v) * (1f - v) * (1f - v); }
}
