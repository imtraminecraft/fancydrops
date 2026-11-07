package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Move the HUD: drag the Now Playing island/card anywhere; scroll to resize it; R puts it back; Esc or Done saves. */
final class HudEditScreen extends Screen {
    private final Screen back; private boolean drag; private double grabX, grabY;
    HudEditScreen(Screen back) { super(Component.literal("Move HUD")); this.back = back; }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() { NowPlayingHud.preview = true; }
    @Override public void removed() { NowPlayingHud.preview = false; FancyConfig.save(); }
    @Override public void onClose() { Minecraft.getInstance().setScreenAndShow(back); }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0x55000000);
        for (int x = 0; x < width; x += 20) g.fill(x, 0, x + 1, height, 0x10FFFFFF);
        for (int y = 0; y < height; y += 20) g.fill(0, y, width, y + 1, 0x10FFFFFF);
        g.fill(width / 2, 0, width / 2 + 1, height, 0x30FFD25A);
        NowPlayingHud.render(g);
        boolean hv = drag || (mx >= NowPlayingHud.bx0 - 3 && mx < NowPlayingHud.bx1 + 3 && my >= NowPlayingHud.by0 - 3 && my < NowPlayingHud.by1 + 3);
        int c = hv ? 0xFFFFD25A : 0x88FFFFFF;
        for (int x = NowPlayingHud.bx0 - 3; x < NowPlayingHud.bx1 + 3; x += 4) { g.fill(x, NowPlayingHud.by0 - 3, x + 2, NowPlayingHud.by0 - 2, c); g.fill(x, NowPlayingHud.by1 + 2, x + 2, NowPlayingHud.by1 + 3, c); }
        for (int y = NowPlayingHud.by0 - 3; y < NowPlayingHud.by1 + 3; y += 4) { g.fill(NowPlayingHud.bx0 - 3, y, NowPlayingHud.bx0 - 2, y + 2, c); g.fill(NowPlayingHud.bx1 + 2, y, NowPlayingHud.bx1 + 3, y + 2, c); }
        String h1 = "Drag the Now Playing " + (FancyConfig.get().npStyle == 1 ? "card" : "record") + "  •  scroll to resize  •  R to reset  •  Esc when done";
        g.centeredText(font, h1, width / 2, height - 24, 0xFFFFFFFF);
        g.centeredText(font, "Size " + FancyConfig.get().npScale + "%", width / 2, height - 12, 0xFFB8C0DA);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        if (e.x() >= NowPlayingHud.bx0 - 3 && e.x() < NowPlayingHud.bx1 + 3 && e.y() >= NowPlayingHud.by0 - 3 && e.y() < NowPlayingHud.by1 + 3) {
            drag = true; FancyConfig c = FancyConfig.get(); grabX = e.x() - c.npX * width; grabY = e.y() - c.npY; return true;
        }
        return super.mouseClicked(e, dbl);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (!drag) return super.mouseDragged(e, dx, dy);
        FancyConfig c = FancyConfig.get();
        double nx = (e.x() - grabX) / width; if (Math.abs(nx - 0.5) < 0.012) nx = 0.5;                      // snaps to the middle
        c.npX = (float) Math.max(0.05, Math.min(0.95, nx)); c.npY = (int) Math.max(0, Math.min(height - 20, e.y() - grabY));
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) { drag = false; return super.mouseReleased(e); }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) { FancyConfig c = FancyConfig.get(); c.npScale = (int) Math.max(60, Math.min(160, c.npScale + Math.signum(sy) * 5)); return true; }
    @Override public boolean keyPressed(KeyEvent e) {
        if (e.key() == 82) { FancyConfig c = FancyConfig.get(); c.npX = 0.5f; c.npY = 4; c.npScale = 100; return true; }
        return super.keyPressed(e);
    }
}
