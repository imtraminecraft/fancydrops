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
    public static boolean hasItem(String name) {
        return name != null && BuiltInRegistries.ITEM.containsKey(Identifier.withDefaultNamespace(name));
    }
    public static void item(GuiGraphicsExtractor g, String name, int x, int y) { g.item(stack(name), x, y); }

    public static void text(GuiGraphicsExtractor g, Font f, String s, int x, int y, int color) { g.text(f, s, x, y, color, true); }
    public static void textCentered(GuiGraphicsExtractor g, Font f, String s, int cx, int y, int color) {
        g.text(f, s, cx - f.width(s) / 2, y, color, true);
    }

    public static int argb(int alpha, int rgb) { return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF); }
    private Gfx() {}
}
