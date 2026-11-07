package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Ambience (Visuals > Ambience), while a song plays:
 *   ALBUM GLOW: the edges of the screen softly take the album's colour, breathing slowly.
 *   MUSIC NOTES: a few notes drift up beside the hotbar now and then, in the album's colours.
 * Both fade away while a drop animation plays or the music is paused.
 */
final class AmbienceHud {
    private AmbienceHud() {}
    private static float on; private static long last;

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis(); float dt = Math.min(0.1f, (now - (last == 0 ? now : last)) / 1000f); last = now;
        boolean want = (c.ambGlow || c.ambNotes) && Spotify.playing && Spotify.track != null && mc.level != null && !AnimationManager.isPlaying();
        on += ((want ? 1f : 0f) - on) * Math.min(1f, dt * 2f);
        if (on < 0.01f) return;
        draw(g, mc.font, g.guiWidth(), g.guiHeight(), now, on, c.ambGlow, c.ambGlowStrength / 100f, c.ambNotes, AlbumArt.ready ? AlbumArt.colour : 0x1ED760);
    }
    static void draw(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int W, int H, long now, float on, boolean glow, float strength, boolean notes, int acc) {
        if (glow) {
            float br = (0.8f + 0.2f * (float) Math.sin(now / 2400.0)) * on * strength;
            int a = Math.round(120 * br);
            NebulaTex.glow(g, 0, H / 2f, W * 0.14f, H * 0.75f, Gfx.argb(a, acc));
            NebulaTex.glow(g, W, H / 2f, W * 0.14f, H * 0.75f, Gfx.argb(a, acc));
            NebulaTex.glow(g, W / 2f, H, W * 0.6f, H * 0.12f, Gfx.argb(Math.round(a * 0.7f), acc));
            NebulaTex.glow(g, W / 2f, 0, W * 0.6f, H * 0.08f, Gfx.argb(Math.round(a * 0.5f), acc));
        }
        if (notes) {
            int lite = Ritual.mixc(acc, 0xFFFFFF, 0.5f);
            for (int i = 0; i < 6; i++) {                                                      // each note has its own slow cycle; most of the time it rests unseen
                float per = 5.5f + i * 0.9f, u = ((now / 1000f) + i * 2.3f) % per / 2.6f; if (u > 1f) continue;
                boolean left = i % 2 == 0; float bx = W / 2f + (left ? -1 : 1) * (100 + 14 * (i / 2)), by = H - 26;
                float x = bx + (float) Math.sin(u * 5 + i) * 6 + (left ? -1 : 1) * u * 14, y = by - u * 46, a = (float) Math.sin(Math.PI * u) * on;
                NebulaTex.glow(g, x + 2.5f, y + 4, 6, 6, Gfx.argb(Math.round(70 * a), acc));
                Gfx.push(g); Gfx.translate(g, x, y); Gfx.rotate(g, (float) Math.sin(u * 4 + i) * 0.3f);
                g.text(font, i % 3 == 0 ? "♫" : "♪", 0, 0, Gfx.argb(Math.round(230 * a), i % 2 == 0 ? acc : lite), true);
                Gfx.pop(g);
            }
        }
    }
}
