package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The lyrics. Two styles (Settings > Music):
 *
 *   CINEMATIC (default): always on screen, so walking and turning never hide them. The line slides in dim, then every word lights up as it
 *     is sung (karaoke), popping in with a little bounce and a glow behind it. When the next line comes the old one floats up and fades, and
 *     the line after it waits faintly below. The whole block hangs on a soft spring: turn your head and it sways behind the view, then settles.
 *   IN THE WORLD: every line pinned in the scene in front of you like a lyric video (it can go off screen when you turn away).
 *
 * Font, colour and glow are settings. Word timing inside a line is spread by the length of the words (synced lyrics only time the lines).
 */
final class LyricsHud {
    private LyricsHud() {}

    static final String[] FONTS = {"Minecraft", "Clean", "Bold", "Serif"};
    static final String[] COLOUR_NAMES = {"Peach", "Ice", "Mint", "Rose", "Gold", "Violet", "White", "Cover"};
    static final int[] COLOURS = {0xFFC8A0, 0xA8DCFF, 0x9AF0C0, 0xFF9AC8, 0xFFD25A, 0xC9A0FF, 0xFFFFFF, -1};

    static int colour(FancyConfig c) { int i = Math.max(0, Math.min(COLOURS.length - 1, c.lyricsColorIdx)); return COLOURS[i] < 0 ? (AlbumArt.ready ? AlbumArt.colour : 0xFFC8A0) : COLOURS[i]; }
    /** The glow colour: the text colour, deeper and more saturated. */
    static int glowOf(int c) { float[] h = java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null); return java.awt.Color.HSBtoRGB(h[0], Math.min(1f, h[1] + 0.45f), 0.95f) & 0xFFFFFF; }
    private static Component txt(String s, FancyConfig c) { return Component.literal(s).withStyle(Fonts.style(Math.max(0, Math.min(3, c.lyricsFont)))); }
    private static int width(Font f, String s, FancyConfig c) { return f.width(txt(s, c)); }

    // ------------------------------------------------------------------ shared timing
    private static int lastLine = -2; private static String lastTrack = ""; private static long lineAt, titleAt;
    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get(); Minecraft mc = Minecraft.getInstance();
        if (!c.lyricsOn || mc.level == null || mc.player == null) { LIVE.clear(); return; }
        Spotify.Track t = Spotify.track; long now = System.currentTimeMillis();
        boolean on = t != null && Spotify.playing;
        if (on && !t.id().equals(lastTrack)) { lastTrack = t.id(); lastLine = -2; titleAt = now; LIVE.clear(); if (c.lyricsStyle == 1) spawn(mc, new String[]{"♫ " + t.name(), t.artists()}, true, c); }
        long pos = Spotify.position() + 200;
        int li = on ? Lyrics.at(pos) : -1;
        if (li != lastLine) { prevLine = lastLine; lastLine = li; lineAt = now; if (c.lyricsStyle == 1) { retireAll(now); if (li >= 0 && !Lyrics.lines.get(li).text().isBlank()) spawn(mc, wrap(Lyrics.lines.get(li).text(), 24), false, c); } }
        if (c.lyricsStyle == 1) world(g, mc, c, now, on);
        else cinematic(g, mc, c, now, on, pos, li);
    }

    // ------------------------------------------------------------------ CINEMATIC
    private static int prevLine = -2;
    private static float swayX, swayY, velX, velY, lastYaw = Float.NaN, lastPitch;
    private static long lastFrame;
    private static void cinematic(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, long now, boolean on, long pos, int li) {
        List<Lyrics.Line> L = Lyrics.lines;
        float dt = Math.min(0.05f, (now - (lastFrame == 0 ? now : lastFrame)) / 1000f); lastFrame = now;
        var cam = mc.gameRenderer.mainCamera(); float yaw = cam.yRot(), pitch = cam.xRot();
        if (Float.isNaN(lastYaw)) { lastYaw = yaw; lastPitch = pitch; }
        float dy = yaw - lastYaw; while (dy > 180) dy -= 360; while (dy < -180) dy += 360;
        float dp = pitch - lastPitch; lastYaw = yaw; lastPitch = pitch;
        velX += -dy * 22f; velY += -dp * 16f;                                                // a soft spring: the words lag behind your turn, then settle
        velX += (-160f * swayX - 13f * velX) * dt; velY += (-160f * swayY - 13f * velY) * dt;
        swayX = Math.max(-30f, Math.min(30f, swayX + velX * dt)); swayY = Math.max(-16f, Math.min(16f, swayY + velY * dt));
        if (!on || L.isEmpty() || li < 0) { titleCard(g, mc, c, now, on); return; }
        int W = g.guiWidth(), H = g.guiHeight(); Font font = mc.font;
        float k = c.lyricsSize / 100f * 1.6f;
        float cy = H * (c.lyricsPos == 1 ? 0.5f : c.lyricsPos == 2 ? 0.26f : 0.7f) + swayY, cx = W / 2f + swayX;
        int col = colour(c), glow = glowOf(col);
        float since = (now - lineAt) / 1000f, enter = Math.min(1f, since / 0.35f), ee = 1f - (1f - enter) * (1f - enter) * (1f - enter);
        // the line before: floats up and away
        if (prevLine >= 0 && prevLine < L.size() && since < 0.7f) {
            float u = since / 0.7f; String pt = L.get(prevLine).text();
            if (!pt.isBlank()) drawLine(g, font, c, pt, cx, cy - 16 * k * (0.6f + u), k * (0.85f - 0.15f * u), Gfx.argb(Math.round(120 * (1f - u)), col), 0, 0f);
        }
        // the line being sung: slides in, then its words light up one by one
        String line = L.get(li).text();
        long start = L.get(li).atMs(), end = li + 1 < L.size() ? L.get(li + 1).atMs() : start + 4000;
        if (!line.isBlank()) {
            String[] words = line.split(" ");
            int total = 0; for (String w : words) total += w.length() + 1;
            float lineW = width(font, line, c) * k, x = cx - lineW / 2f, y = cy + (1f - ee) * 10 * k;
            float span = Math.max(400f, (end - start) * 0.82f);
            int acc = 0;
            for (String w : words) {
                float wAt = start + span * acc / (float) total, wEnd = start + span * (acc + w.length() + 1) / (float) total;
                float lit = Math.max(0f, Math.min(1f, (pos - wAt) / 140f)), pop = (pos - wAt) / 220f;
                float bounce = pop > 0 && pop < 1 ? (float) Math.sin(Math.PI * pop) * 0.16f : 0f;
                float wW = width(font, w, c) * k, wx = x + wW / 2f, wy = y - (pop > 0 && pop < 1 ? (float) Math.sin(Math.PI * pop) * 3 * k : 0);
                float singing = pos >= wAt && pos < wEnd ? 1f : 0f;
                if (c.lyricsGlow && lit > 0f) NebulaTex.glow(g, wx, wy + 4 * k, wW * 0.6f + 6 * k, 9 * k, Gfx.argb(Math.round((40 + 70 * singing) * lit * enter), glow));
                int a = Math.round((90 + 165 * lit) * enter);
                int cw = lit >= 1f ? col : mixc(0x8A8A9A, col, lit);
                drawWord(g, font, c, w, wx, wy, k * (1f + bounce), Gfx.argb(a, cw), c.lyricsGlow ? Gfx.argb(Math.round(70 * lit * enter), glow) : 0);
                x += wW + width(font, " ", c) * k; acc += w.length() + 1;
            }
        }
        // the line after: waiting faintly below
        if (c.lyricsNext && li + 1 < L.size()) {
            String nt = L.get(li + 1).text();
            if (!nt.isBlank()) drawLine(g, font, c, nt, cx, cy + 15 * k, k * 0.7f, Gfx.argb(Math.round(70 * ee), 0xD8D8E8), 0, 0f);
        }
    }
    /** The song's name, big and brief, when it starts (and while no line is up yet). */
    private static void titleCard(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, long now, boolean on) {
        Spotify.Track t = Spotify.track; if (!on || t == null) return;
        float u = (now - titleAt) / 1000f; if (u > 4.5f) return;
        float a = Math.min(1f, u / 0.5f) * Math.min(1f, (4.5f - u) / 0.8f);
        int W = g.guiWidth(), H = g.guiHeight(); float k = c.lyricsSize / 100f * 1.6f, cy = H * (c.lyricsPos == 1 ? 0.5f : c.lyricsPos == 2 ? 0.26f : 0.7f) + swayY, cx = W / 2f + swayX;
        int col = colour(c);
        if (c.lyricsGlow) NebulaTex.glow(g, cx, cy + 4 * k, width(mc.font, t.name(), c) * k * 0.6f + 10, 12 * k, Gfx.argb(Math.round(70 * a), glowOf(col)));
        drawLine(g, mc.font, c, t.name(), cx, cy - (1f - Math.min(1f, u / 0.5f)) * 8, k * 1.15f, Gfx.argb(Math.round(255 * a), col), 0, 0f);
        drawLine(g, mc.font, c, t.artists(), cx, cy + 13 * k, k * 0.7f, Gfx.argb(Math.round(170 * a), 0xD8D8E8), 0, 0f);
    }
    private static void drawLine(GuiGraphicsExtractor g, Font f, FancyConfig c, String s, float cx, float y, float k, int argb, int glow, float unused) {
        Gfx.push(g); Gfx.translate(g, cx, y); Gfx.scale(g, k); Gfx.translate(g, -width(f, s, c) / 2f, 0);
        g.text(f, txt(s, c), 0, 0, argb, c.lyricsShadow); Gfx.pop(g);
    }
    private static void drawWord(GuiGraphicsExtractor g, Font f, FancyConfig c, String s, float cx, float y, float k, int argb, int glow) {
        Gfx.push(g); Gfx.translate(g, cx, y); Gfx.scale(g, k); float hw = width(f, s, c) / 2f; Gfx.translate(g, -hw, 0);
        if ((glow >>> 24) > 4) for (int d = 0; d < 4; d++) g.text(f, txt(s, c), d == 0 ? -1 : d == 1 ? 1 : 0, d == 2 ? -1 : d == 3 ? 1 : 0, glow, false);
        g.text(f, txt(s, c), 0, 0, argb, c.lyricsShadow); Gfx.pop(g);
    }
    private static int mixc(int a, int b, float f) { return Ritual.mixc(a, b, f); }

    // ------------------------------------------------------------------ IN THE WORLD
    private static final class Floater {
        final double x, y, z; final String[] rows; final long born; long gone; final boolean title;
        Floater(double x, double y, double z, String[] rows, boolean title) { this.x = x; this.y = y; this.z = z; this.rows = rows; this.title = title; born = System.currentTimeMillis(); }
    }
    private static final List<Floater> LIVE = new ArrayList<>();
    private static final Random RND = new Random(); private static int side = 1;
    private static void world(GuiGraphicsExtractor g, Minecraft mc, FancyConfig c, long now, boolean on) {
        if (!on) retireAll(now);
        LIVE.removeIf(f -> f.gone > 0 && now - f.gone > 900);
        if (LIVE.isEmpty()) return;
        var cam = mc.gameRenderer.mainCamera(); var cp = cam.position();
        Matrix4f vp = cam.getViewRotationProjectionMatrix(new Matrix4f());
        int W = g.guiWidth(), H = g.guiHeight(); Font font = mc.font; int col = colour(c), glow = glowOf(col);
        for (Floater f : LIVE) {
            Vector4f v = new Vector4f((float) (f.x - cp.x), (float) (f.y - cp.y + (f.gone > 0 ? (now - f.gone) / 900.0 * 0.6 : 0)), (float) (f.z - cp.z), 1f);
            vp.transform(v); if (v.w < 0.3f) continue;
            float sx = (v.x / v.w * 0.5f + 0.5f) * W, sy = (1f - (v.y / v.w * 0.5f + 0.5f)) * H;
            double dist = Math.sqrt((f.x - cp.x) * (f.x - cp.x) + (f.y - cp.y) * (f.y - cp.y) + (f.z - cp.z) * (f.z - cp.z));
            float scale = (float) Math.max(0.45, Math.min(3.0, 9.0 / dist)) * (c.lyricsSize / 100f) * (f.title ? 0.8f : 1f) * (H / 240f);
            float age = (now - f.born) / 1000f, a = Math.min(1f, age / 0.35f) * (f.gone > 0 ? Math.max(0f, 1f - (now - f.gone) / 900f) : 1f);
            if (a < 0.02f) continue;
            int total = 0; for (String r : f.rows) total += r.length();
            int shown = f.title ? total : (int) Math.min(total, age * 45f + 1), left = shown;
            for (int i = 0; i < f.rows.length; i++) {
                String row = f.rows[i], part = row.substring(0, Math.max(0, Math.min(row.length(), left))); left -= row.length();
                if (part.isEmpty()) continue;
                float y = sy + (i - (f.rows.length - 1) / 2f) * 10 * scale;
                drawWord(g, font, c, part, sx - (width(font, row, c) - width(font, part, c)) / 2f * scale, y, scale, Gfx.argb(Math.round(255 * a), f.title && i == 1 ? 0xB8C8E8 : col), c.lyricsGlow ? Gfx.argb(Math.round(45 * a), glow) : 0);
            }
        }
    }
    private static void retireAll(long now) { for (Floater f : LIVE) if (f.gone == 0) f.gone = now; }
    private static void spawn(Minecraft mc, String[] rows, boolean title, FancyConfig c) {
        var cam = mc.gameRenderer.mainCamera(); var cp = cam.position();
        side = -side;
        double yaw = Math.toRadians(cam.yRot() + side * (12 + RND.nextInt(22))), dist = c.lyricsDistance * (0.85 + RND.nextDouble() * 0.3);
        double up = (title ? 1.6 : 0.4) + RND.nextDouble() * 1.4;
        LIVE.add(new Floater(cp.x - Math.sin(yaw) * dist, cp.y + up - Math.sin(Math.toRadians(cam.xRot())) * dist * 0.35, cp.z + Math.cos(yaw) * dist, rows, title));
        if (LIVE.size() > 4) LIVE.remove(0);
    }
    private static String[] wrap(String s, int max) {
        if (s.length() <= max) return new String[]{s};
        int cut = s.lastIndexOf(' ', Math.min(s.length() - 1, (s.length() + 1) / 2 + 4));
        if (cut <= 0) cut = s.indexOf(' '); if (cut <= 0) return new String[]{s};
        return new String[]{s.substring(0, cut), s.substring(cut + 1)};
    }
}
