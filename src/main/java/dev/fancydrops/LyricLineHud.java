package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The Lyric line HUD (Visuals > Lyrics > Lyric line): the line being sung, on its own small glass card that you can move and resize
 * (Move it). The words fill with the album's (cool) colour as they are sung; a new line slides in over the old one. It steps aside during
 * drop animations, and shows "no lyrics for this song" when a song has none.
 */
final class LyricLineHud {
    private LyricLineHud() {}

    static boolean preview;
    static int bx0, by0, bx1, by1;
    private static float show, away; private static long last, lineAt; private static int lastLine = -2; private static String prevText = "";

    static void hud(GuiGraphicsExtractor g) { if (!preview) render(g); }

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis(); float dt = Math.min(0.1f, (now - (last == 0 ? now : last)) / 1000f); last = now;
        Spotify.Track t = Spotify.track;
        boolean demo = preview && (t == null || Lyrics.lines.isEmpty());
        long pos = Spotify.position() + 150 + c.lyricsOffset;
        int li = demo ? 0 : Lyrics.at(pos);
        String text = demo ? "The line being sung shows here" : Lyrics.lines.isEmpty() && Lyrics.status.startsWith("No synced") ? "♪ no lyrics for this song" : li >= 0 && li < Lyrics.lines.size() ? Lyrics.lines.get(li).text() : "";
        boolean want = preview || (c.lineHudOn && c.lyricsOn && Spotify.playing && t != null && mc.level != null && !text.isBlank());
        away += ((AnimationManager.isPlaying() && !preview ? 1f : 0f) - away) * Math.min(1f, dt * 8f);
        show += ((want ? 1f : 0f) - show) * Math.min(1f, dt * 8f);
        if (li != lastLine) { if (lastLine >= 0 && lastLine < Lyrics.lines.size()) prevText = Lyrics.lines.get(lastLine).text(); else prevText = ""; lastLine = li; lineAt = now; }
        float a = show * (1f - away); if (a < 0.02f) { bx0 = by0 = bx1 = by1 = 0; return; }
        long start = demo ? (now / 1000) * 1000 - 1000 : li >= 0 && li < Lyrics.lines.size() ? Lyrics.lines.get(li).atMs() : pos, end = demo ? start + 3000 : li >= 0 && li + 1 < Lyrics.lines.size() ? Lyrics.lines.get(li + 1).atMs() : start + 4000;
        draw(g, mc.font, g.guiWidth() * c.lineX, c.lineY, c.lineScale / 100f, a, text, prevText, demo ? now % 3000 + start : pos, start, end, now - lineAt);
    }

    /** cx/top: the middle of its top edge. */
    static void draw(GuiGraphicsExtractor g, Font f, float cx, float top, float k, float a, String text, String prev, long pos, long start, long end, long since) {
        int maxW = 260, tw = Math.min(maxW, f.width(text)), w = Math.max(80, tw + 20), h = 18, acc = LyricsHud.colour();
        bx0 = Math.round(cx - w / 2f * k); by0 = Math.round(top); bx1 = Math.round(cx + w / 2f * k); by1 = Math.round(top + h * k);
        Gfx.push(g); Gfx.translate(g, cx - w / 2f * k, top); Gfx.scale(g, k);
        NebulaTex.glow(g, w / 2f, h / 2f + 3, w * 0.55f, h, Gfx.argb(Math.round(60 * a), 0x000000));
        NowPlayingHud.round(g, 0, 0, w, h, Gfx.argb(Math.round(215 * a), 0x0E1018));
        g.fill(3, 0, w - 3, 1, Gfx.argb(Math.round(26 * a), 0xFFFFFF));
        float in = Math.min(1f, since / 300f), e = 1f - (1f - in) * (1f - in);
        if (in < 1f && !prev.isBlank()) {                                                     // the old line lifts away as the new one comes up
            String p = f.width(prev) > maxW ? f.plainSubstrByWidth(prev, maxW - 6) + "…" : prev;
            Gfx.txt(g, f, p, Math.round(w / 2f - f.width(p) / 2f), Math.round(5 - 6 * e), Gfx.argb(Math.round(170 * a * (1f - e)), 0xC6CAD6), false);
        }
        String s = f.width(text) > maxW ? f.plainSubstrByWidth(text, maxW - 6) + "…" : text;
        float x = w / 2f - f.width(s) / 2f, y = 5 + 6 * (1f - e);
        int total = Math.max(1, s.length()); float span = Math.max(400f, (end - start) * 0.85f); int accN = 0;
        for (String word : s.split(" ")) {                                                     // each word fills with the colour as it's sung
            float at = start + span * accN / (float) total, lit = Math.max(0f, Math.min(1f, (pos - at) / 140f));
            Gfx.txt(g, f, word, Math.round(x), Math.round(y), Gfx.argb(Math.round(150 * a * e), 0xDDE1EA), false);
            if (lit > 0f) Gfx.txt(g, f, word, Math.round(x), Math.round(y), Gfx.argb(Math.round(255 * a * e * lit), acc), false);
            x += f.width(word + " "); accN += word.length() + 1;
        }
        Gfx.pop(g);
    }
}
