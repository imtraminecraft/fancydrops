package dev.fancydrops;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/** The fonts the mod can write in: the menu (Settings > Menu) and the drop texts (Text Changes). 0 is the game's own. */
public final class Fonts {
    public static final String[] NAMES = {"Default", "Clean", "Bold", "Serif"};
    public static final String[] SUBS = {"The game's own look", "Round and modern", "Heavy sans", "Old-book serif"};
    private static final String[][] IDS = {null, {"fancydrops", "f_clean"}, {"fancydrops", "f_bold"}, {"fancydrops", "f_serif"}};
    private static final FontDescription[] DESC = new FontDescription[IDS.length];
    static { for (int i = 1; i < IDS.length; i++) DESC[i] = new FontDescription.Resource(Identifier.fromNamespaceAndPath(IDS[i][0], IDS[i][1])); }

    public static int fix(int i) { return i < 0 || i >= NAMES.length ? 0 : i; }
    public static FontDescription desc(int i) { return i <= 0 || i >= DESC.length ? null : DESC[i]; }
    public static Style style(int i) { FontDescription d = desc(i); return d == null ? Style.EMPTY : Style.EMPTY.withFont(d); }
    public static Component text(String s, int i) { return Component.literal(s).withStyle(style(i)); }
    private Fonts() {}
}
