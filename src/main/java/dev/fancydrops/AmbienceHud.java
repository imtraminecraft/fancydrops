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
        draw(g, mc.font, g.guiWidth(), g.guiHeight(), now, on, c, now - waveAt, AlbumArt.ready ? AlbumArt.colour : 0x1ED760);
    }

    static void draw(GuiGraphicsExtractor g, Font font, int W, int H, long now, float on, FancyConfig c, long sinceSong, int acc) {
        int lite = Ritual.mixc(acc, 0xFFFFFF, 0.45f), deep = Ritual.mixc(acc, 0x000000, 0.25f);
        if (c.ambGlow) {                                                                 // soft light all round the edge of the screen
            float br = (0.75f + 0.25f * (float) Math.sin(now / 2200.0)) * on * c.ambGlowStrength / 100f;
            int a = Math.round(200 * br), n = 3;
            for (int i = 0; i <= n; i++) {
                float u = i / (float) n, wob = (float) Math.sin(now / 1700.0 + i * 1.3) * 0.15f + 1f;
                int col = i % 2 == 0 ? acc : deep;
                NebulaTex.glow(g, W * u, 0, W * 0.19f * wob, H * 0.07f * wob, Gfx.argb(Math.round(a * 0.7f), col));
                NebulaTex.glow(g, W * u, H, W * 0.19f * wob, H * 0.09f * wob, Gfx.argb(a, col));
                NebulaTex.glow(g, 0, H * u, W * 0.06f * wob, H * 0.22f * wob, Gfx.argb(a, col));
                NebulaTex.glow(g, W, H * u, W * 0.06f * wob, H * 0.22f * wob, Gfx.argb(a, col));
            }
        }
        if (c.ambBars) {                                                                 // a visualizer along the bottom, either side of the hotbar
            int n = 26, bw = 4, gap = 2, hot = 100;
            for (int side = -1; side <= 1; side += 2) for (int i = 0; i < n; i++) {
                float x = W / 2f + side * (hot + i * (bw + gap)); if (x < 0 || x > W) continue;
                float lvl = Math.abs((float) Math.sin(now / (170.0 + i * 13) + i * 0.7 + side) * (float) Math.sin(now / (430.0 + i * 29) + i * 1.9)) * (1f - i / (float) n * 0.6f);
                int bh = Math.max(1, Math.round((3 + 22 * lvl) * on));
                int x0 = Math.round(side < 0 ? x - bw : x);
                g.fill(x0, H - bh, x0 + bw, H, Gfx.argb(Math.round(150 * on), Ritual.mixc(acc, lite, i / (float) n)));
                g.fill(x0, H - bh, x0 + bw, H - bh + 1, Gfx.argb(Math.round(220 * on), 0xFFFFFF));
            }
        }
        if (c.ambNotes) {                                                                // notes drifting up beside the hotbar
            for (int i = 0; i < 10; i++) {
                float per = 3.2f + (i % 4) * 0.6f, u = (((now / 1000f) + i * 0.83f) % per) / per;
                boolean left = i % 2 == 0; float bx = W / 2f + (left ? -1 : 1) * (98 + 18 * (i / 2 % 3)), by = H - 22;
                float x = bx + (float) Math.sin(u * 6 + i) * 7 + (left ? -1 : 1) * u * 22, y = by - u * 70, a = (float) Math.sin(Math.PI * u) * on;
                NebulaTex.glow(g, x + 2.5f, y + 4, 7, 7, Gfx.argb(Math.round(90 * a), acc));
                Gfx.push(g); Gfx.translate(g, x, y); Gfx.rotate(g, (float) Math.sin(u * 4 + i) * 0.35f); float sc = 0.9f + 0.3f * (i % 3) / 2f; Gfx.scale(g, sc);
                Gfx.txt(g, font, i % 3 == 0 ? "♫" : "♪", 0, 0, Gfx.argb(Math.round(240 * a), i % 2 == 0 ? acc : lite), true);
                Gfx.pop(g);
            }
        }
        if (c.ambWave && sinceSong < 1600) {                                             // a new song: a wave of its colour sweeps across
            float u = sinceSong / 1600f, a = (float) Math.sin(Math.PI * u) * on;
            float x = -W * 0.2f + W * 1.4f * ease(u);
            NebulaTex.glow(g, x, H / 2f, W * 0.22f, H * 0.9f, Gfx.argb(Math.round(110 * a), acc));
            NebulaTex.glow(g, x, H / 2f, W * 0.05f, H * 0.7f, Gfx.argb(Math.round(140 * a), lite));
        }
    }
    /** Tiny stars that burst from a word as it lands (Word sparkles). u: 0..1 since it landed. */
    static void sparkle(GuiGraphicsExtractor g, float cx, float cy, float w, float u, float a, int seed) {
        if (u >= 1f || a < 0.02f) return;
        int acc = LyricsHud.colour();
        for (int i = 0; i < 5; i++) {
            double an = -Math.PI / 2 + (Ritual.rnd(seed * 7 + i) - 0.5) * 2.6; float d = (6 + 10 * Ritual.rnd(seed * 3 + i)) * ease(u);
            float x = cx + (Ritual.rnd(seed + i * 11) - 0.5f) * w + (float) Math.cos(an) * d, y = cy + (float) Math.sin(an) * d, al = (1f - u) * a;
            NebulaTex.glow(g, x, y, 2.5f, 2.5f, Gfx.argb(Math.round(230 * al), i % 2 == 0 ? 0xFFFFFF : acc));
        }
    }
    private static float ease(float v) { v = Math.max(0f, Math.min(1f, v)); return 1f - (1f - v) * (1f - v) * (1f - v); }
}
