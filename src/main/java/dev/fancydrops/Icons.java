package dev.fancydrops;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Every icon in the menu is called by a plain name written between colons, like a Discord emoji: ":name:". There is one category of icons
 * (no more "suf_" or "cx_" prefixes). The old hand-pixeled set is gone; until the new animated icons are drawn, a name without a picture
 * shows a small tile with its first letter, so a missing icon is easy to spot. ":name:#RRGGBB" tints an icon that takes a colour.
 */
final class Icons {
    static boolean animated = true;
    static boolean STILL_ICONS = false;

    static boolean has(String k) { return k != null && (k.startsWith("fd:") || (k.length() > 2 && k.charAt(0) == ':' && k.indexOf(':', 1) > 1)); }
    /** The one way to name an icon: ":name:". */
    static String tok(String name) { return ":" + name + ":"; }
    /** ":name:" (or ":name:#RRGGBB", or the older "fd:name#RRGGBB") as the plain name the drawing code switches on. */
    static String nameOf(String key) {
        if (key.startsWith("fd:")) return key.substring(3);
        if (key.length() > 2 && key.charAt(0) == ':') { int e = key.indexOf(':', 1); if (e > 1) return key.substring(1, e) + key.substring(e + 1); }
        return key;
    }

    /** Draws a 16 x 16 icon at x, y. */
    static void draw(GuiGraphicsExtractor g, String key, int x, int y, long now) {
        if (!animated || STILL_ICONS) now = 2400L;
        String name = nameOf(key); int tint = 0;
        int hash = name.indexOf('#');
        if (hash >= 0) { try { tint = 0xFF000000 | Integer.parseInt(name.substring(hash + 1), 16); } catch (Exception ignored) { } name = name.substring(0, hash); }
        if (name.equals("sparkle") || Cosmetics.byId(name) != null) { Cosmetics.drawIcon(g, name, x, y, now); return; }      // the one suffix we have: shaded, six frames
        placeholder(g, name, tint, x, y, now);
    }

    /** A rounded tile in a colour taken from the name, with the first letter on it, bobbing a little while the icon is lit. */
    private static void placeholder(GuiGraphicsExtractor g, String name, int tint, int x, int y, long now) {
        int h = name.hashCode();
        int col = tint != 0 ? tint : java.awt.Color.HSBtoRGB((h & 0xFFFF) / 65535f, 0.45f, 0.78f) | 0xFF000000;
        int bob = now == 2400L ? 0 : Math.round((float) Math.sin(now / 300.0 + (h & 7)) * 1f);
        int ty = y + 1 + bob;
        g.fill(x + 2, ty, x + 14, ty + 14, Canvas.shade(col, 0.55f));
        g.fill(x + 1, ty + 2, x + 15, ty + 12, Canvas.shade(col, 0.55f));
        g.fill(x + 3, ty + 1, x + 13, ty + 7, col);
        g.fill(x + 2, ty + 3, x + 14, ty + 6, col);
        g.fill(x + 3, ty + 1, x + 13, ty + 2, Canvas.mix(col, 0xFFFFFFFF, 0.5f));
        String ch = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        net.minecraft.client.gui.Font f = net.minecraft.client.Minecraft.getInstance().font;
        Gfx.text(g, f, ch, x + 8 - Gfx.w(f, ch) / 2, ty + 3, 0xFFFFFFFF);
    }
}
