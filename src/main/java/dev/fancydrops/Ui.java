package dev.fancydrops;

/** Shared colours (ARGB). */
final class Ui {
    // The "vault" look (0.8.0): warm near-black panels, a gold accent, warm white text. Colour is kept for icons and states, not for chrome.
    static final int ACCENT = 0xFFE8B84A, GOLD = 0xFFFFB300, PANEL = 0xFF15121B, PANEL2 = 0xFF0E0C13, BORDER = 0xFF302B3B, ROW = 0xFF1C1924, ROW_HOVER = 0xFF272331,
            TEXT = 0xFFF6F1E7, SUB = 0xFFBDB5C9, DIM = 0xFF807891, OFF = 0xFF38323F, GREEN = 0xFF62E6A8, RED = 0xFFFF5555,
            HAIR = 0xFF4A3F2A, SHINE = 0x12FFFFFF, ANIM_SUB = 0xFF9DB2CC,      // ANIM_SUB: the animation text keeps its own colour
            PRICE = 0xFFFFFFFF, PRICE_SRC = 0xFFAAAAAA, MAGIC_FIND = 0xFF55FFFF,
            R_RARE = 0xFF5555FF, R_VERY = 0xFFAA00AA, R_CRAZY = 0xFFFF55FF, R_LEGEND = 0xFFFFAA00;

    /** Colour for a Hypixel drop-odds label ("Occasional", "Pray RNGesus", ...). */
    static int odds(String label) {
        if (label == null) return ANIM_SUB;
        return switch (label) {
            case "Guaranteed" -> 0xFF9AA5B1; case "Occasional" -> 0xFF8FD694; case "Rare" -> 0xFF6FA8FF;
            case "Extraordinary" -> 0xFFC77DFF; case "Pray RNGesus" -> 0xFFFFC040; case "RNGesus Incarnate" -> 0xFFFF6EC7;
            default -> ANIM_SUB;
        };
    }
    private Ui() {}
}
