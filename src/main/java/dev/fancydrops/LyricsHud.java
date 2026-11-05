package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The lyrics in the world, like a lyric video shot in Minecraft: every new line appears at a spot in the scene in front of you (a little to the
 * side, a little up), stays pinned there while you look around, and glows warm. It types itself in, holds, then fades and drifts up when the
 * next line appears somewhere else. The song's title appears the same way when a new song starts.
 *
 * Drawn on the HUD at the projected position of its anchor (so it never touches the world's rendering), scaled with distance.
 */
final class LyricsHud {
    private LyricsHud() {}

    private static final class Floater {
        final double x, y, z; final String[] rows; final long born; long gone; final boolean title; final int col;
        Floater(double x, double y, double z, String[] rows, boolean title, int col) { this.x = x; this.y = y; this.z = z; this.rows = rows; this.title = title; this.col = col; born = System.currentTimeMillis(); }
    }
    private static final List<Floater> LIVE = new ArrayList<>();
    private static int lastLine = -2; private static String lastTrack = ""; private static final Random RND = new Random();
    private static int side = 1;

    static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get();
        Minecraft mc = Minecraft.getInstance();
        if (!c.lyricsOn || mc.level == null || mc.player == null) { LIVE.clear(); return; }
        long now = System.currentTimeMillis();
        Spotify.Track t = Spotify.track;
        if (t != null && Spotify.playing) {
            if (!t.id().equals(lastTrack)) { lastTrack = t.id(); lastLine = -2; retireAll(now); spawn(mc, new String[]{"♫ " + t.name(), t.artists()}, true, c); }
            int li = Lyrics.at(Spotify.position() + 250);                                         // a hair early, so a line is up as it is sung
            if (li != lastLine) {
                lastLine = li; retireAll(now);
                if (li >= 0) { String txt = Lyrics.lines.get(li).text(); if (!txt.isBlank()) spawn(mc, wrap(txt, 24), false, c); }
            }
        } else if (!LIVE.isEmpty() && (t == null || !Spotify.playing)) retireAll(now);
        LIVE.removeIf(f -> f.gone > 0 && now - f.gone > 900);
        if (LIVE.isEmpty()) return;
        var cam = mc.gameRenderer.mainCamera(); var cp = cam.position();
        Matrix4f vp = cam.getViewRotationProjectionMatrix(new Matrix4f());
        int W = g.guiWidth(), H = g.guiHeight(); Font font = mc.font;
        for (Floater f : LIVE) {
            Vector4f v = new Vector4f((float) (f.x - cp.x), (float) (f.y - cp.y + (f.gone > 0 ? (now - f.gone) / 900.0 * 0.6 : 0)), (float) (f.z - cp.z), 1f);
            vp.transform(v); if (v.w < 0.3f) continue;
            float sx = (v.x / v.w * 0.5f + 0.5f) * W, sy = (1f - (v.y / v.w * 0.5f + 0.5f)) * H;
            double dist = Math.sqrt((f.x - cp.x) * (f.x - cp.x) + (f.y - cp.y) * (f.y - cp.y) + (f.z - cp.z) * (f.z - cp.z));
            float scale = (float) Math.max(0.45, Math.min(3.0, 9.0 / dist)) * (c.lyricsSize / 100f) * (f.title ? 0.8f : 1f) * (H / 240f);
            float age = (now - f.born) / 1000f, fadeIn = Math.min(1f, age / 0.35f), out = f.gone > 0 ? Math.max(0f, 1f - (now - f.gone) / 900f) : 1f;
            float a = fadeIn * out; if (a < 0.02f) continue;
            int total = 0; for (String r : f.rows) total += r.length();
            int shown = f.title ? total : (int) Math.min(total, age * 45f + 1);                    // types itself in, quick
            Gfx.push(g); Gfx.translate(g, sx, sy); Gfx.scale(g, scale);
            int lh = 10, y0 = -(f.rows.length * lh) / 2, left = shown;
            for (int i = 0; i < f.rows.length; i++) {
                String row = f.rows[i]; String part = row.substring(0, Math.max(0, Math.min(row.length(), left))); left -= row.length();
                int w = font.width(row), x = -w / 2, y = y0 + i * lh;
                int glow = f.title ? 0x6AA8FF : 0xFF7A2E, base = f.title && i == 1 ? 0xB8C8E8 : f.col;
                if (!part.isEmpty()) {
                    for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) if (dx != 0 || dy != 0) g.text(font, part, x + dx, y + dy, Gfx.argb(Math.round(40 * a), glow), false);     // the soft glow round the letters
                    g.text(font, part, x, y, Gfx.argb(Math.round(255 * a), base), false);
                }
            }
            Gfx.pop(g);
        }
    }
    private static void retireAll(long now) { for (Floater f : LIVE) if (f.gone == 0) f.gone = now; }
    /** A new line pinned in the world: in front of you, alternately to the left and right, a little above eye level. */
    private static void spawn(Minecraft mc, String[] rows, boolean title, FancyConfig c) {
        var cam = mc.gameRenderer.mainCamera(); var cp = cam.position();
        side = -side;
        double yaw = Math.toRadians(cam.yRot() + side * (12 + RND.nextInt(22))), dist = (c.lyricsDistance) * (0.85 + RND.nextDouble() * 0.3);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double up = (title ? 1.6 : 0.4) + RND.nextDouble() * 1.4;
        LIVE.add(new Floater(cp.x + fx * dist, cp.y + up - Math.sin(Math.toRadians(cam.xRot())) * dist * 0.35, cp.z + fz * dist, rows, title, c.lyricsColor));
        if (LIVE.size() > 4) LIVE.remove(0);
    }
    private static String[] wrap(String s, int max) {
        if (s.length() <= max) return new String[]{s};
        int cut = s.lastIndexOf(' ', Math.min(s.length() - 1, (s.length() + 1) / 2 + 4));
        if (cut <= 0) cut = s.indexOf(' '); if (cut <= 0) return new String[]{s};
        return new String[]{s.substring(0, cut), s.substring(cut + 1)};
    }
}
