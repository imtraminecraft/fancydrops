package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Move a HUD card (Now Playing, or the lyrics): drag it anywhere; scroll to resize it; R puts it back; Esc or Done saves. */
final class HudEditScreen extends Screen {
    static final int NOW_PLAYING = 0, LINE = 1;
    private final Screen back; private final int which; private boolean drag; private double grabX, grabY;
    HudEditScreen(Screen back) { this(back, NOW_PLAYING); }
    HudEditScreen(Screen back, int which) { super(Component.literal("Move HUD")); this.back = back; this.which = which; }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() { NowPlayingHud.preview = which == NOW_PLAYING; LyricsHud.preview = which == LINE; }
    @Override public void removed() { NowPlayingHud.preview = false; LyricsHud.preview = false; FancyConfig.save(); }
    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(back); }

    private int[] box() { return which == LINE ? new int[]{LyricsHud.bx0, LyricsHud.by0, LyricsHud.bx1, LyricsHud.by1} : new int[]{NowPlayingHud.bx0, NowPlayingHud.by0, NowPlayingHud.bx1, NowPlayingHud.by1}; }
    private float x() { FancyConfig c = FancyConfig.get(); return which == LINE ? c.lineX : c.npX; }
    private int y() { FancyConfig c = FancyConfig.get(); return which == LINE ? Math.round(c.lyY * height) : c.npY; }
    private int scale() { FancyConfig c = FancyConfig.get(); return which == LINE ? c.lyricsSize : c.npScale; }
    private void set(float x, int y, int s) { FancyConfig c = FancyConfig.get(); if (which == LINE) { c.lineX = x; c.lyY = y / (float) Math.max(1, height); c.lyricsSize = s; } else { c.npX = x; c.npY = y; c.npScale = s; } }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0x55000000);
        for (int x = 0; x < width; x += 20) g.fill(x, 0, x + 1, height, 0x10FFFFFF);
        for (int y = 0; y < height; y += 20) g.fill(0, y, width, y + 1, 0x10FFFFFF);
        g.fill(width / 2, 0, width / 2 + 1, height, 0x30FFD25A);
        if (which == LINE) LyricsHud.render(g); else NowPlayingHud.render(g);
        int[] b = box();
        boolean hv = drag || (mx >= b[0] - 3 && mx < b[2] + 3 && my >= b[1] - 3 && my < b[3] + 3);
        int c = hv ? 0xFFFFD25A : 0x88FFFFFF;
        for (int x = b[0] - 3; x < b[2] + 3; x += 4) { g.fill(x, b[1] - 3, x + 2, b[1] - 2, c); g.fill(x, b[3] + 2, x + 2, b[3] + 3, c); }
        for (int y = b[1] - 3; y < b[3] + 3; y += 4) { g.fill(b[0] - 3, y, b[0] - 2, y + 2, c); g.fill(b[2] + 2, y, b[2] + 3, y + 2, c); }
        String h1 = "Drag the " + (which == LINE ? "lyrics" : "Now Playing card") + "  •  scroll to resize  •  R to reset  •  Esc when done";
        g.centeredText(font, h1, width / 2, height - 24, 0xFFFFFFFF);
        g.centeredText(font, "Size " + scale() + "%", width / 2, height - 12, 0xFFB8C0DA);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        int[] b = box();
        if (e.x() >= b[0] - 3 && e.x() < b[2] + 3 && e.y() >= b[1] - 3 && e.y() < b[3] + 3) { drag = true; grabX = e.x() - x() * width; grabY = e.y() - y(); return true; }
        return super.mouseClicked(e, dbl);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (!drag) return super.mouseDragged(e, dx, dy);
        double nx = (e.x() - grabX) / width; if (Math.abs(nx - 0.5) < 0.012) nx = 0.5;                      // snaps to the middle
        set((float) Math.max(0.05, Math.min(0.95, nx)), (int) Math.max(0, Math.min(height - 20, e.y() - grabY)), scale());
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) { drag = false; return super.mouseReleased(e); }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) { set(x(), y(), (int) Math.max(60, Math.min(160, scale() + Math.signum(sy) * 5))); return true; }
    @Override public boolean keyPressed(KeyEvent e) {
        if (e.key() == 82) { set(0.5f, which == LINE ? Math.round(height * 0.64f) : 4, 100); return true; }
        return super.keyPressed(e);
    }
}
