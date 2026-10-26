package dev.fancydrops;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Every Minecraft-version-sensitive draw call lives here (written for 26.2, Mojang names). */
public final class Gfx {
    private static final Map<String, ItemStack> STACKS = new HashMap<>();

    public static void push(GuiGraphicsExtractor g) { g.pose().pushMatrix(); }
    public static void pop(GuiGraphicsExtractor g) { g.pose().popMatrix(); }
    public static void translate(GuiGraphicsExtractor g, float x, float y) { g.pose().translate(x, y); }
    public static void rotate(GuiGraphicsExtractor g, float radians) { g.pose().rotate(radians); }
    public static void scale(GuiGraphicsExtractor g, float s) { g.pose().scale(s, s); }

    public static ItemStack stack(String name) {
        return STACKS.computeIfAbsent(name, n -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(n))));
    }
    public static void item(GuiGraphicsExtractor g, String name, int x, int y) { g.item(stack(name), x, y); }

    /** The font of the mod's own menu (Settings > Menu). */
    public static int uiFont;
    public static int w(Font f, String s) { return uiFont <= 0 ? f.width(s) : f.width(Fonts.text(s, uiFont)); }
    public static int w(Font f, net.minecraft.network.chat.FormattedText c) { return f.width(c); }
    public static String fit(Font f, String s, int max) {
        if (uiFont <= 0) return f.plainSubstrByWidth(s, max);
        while (!s.isEmpty() && w(f, s) > max) s = s.substring(0, s.length() - 1);
        return s;
    }
    public static void text(GuiGraphicsExtractor g, Font f, String s, int x, int y, int color) { if (uiFont <= 0) g.text(f, s, x, y, color, true); else g.text(f, Fonts.text(s, uiFont), x, y, color, true); }
    public static void textCentered(GuiGraphicsExtractor g, Font f, String s, int cx, int y, int color) { text(g, f, s, cx - w(f, s) / 2, y, color); }

    public static int argb(int alpha, int rgb) { return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF); }
    private Gfx() {}
}
