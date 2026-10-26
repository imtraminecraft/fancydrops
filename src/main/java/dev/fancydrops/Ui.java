package dev.fancydrops;

/** Shared colours (ARGB). */
final class Ui {
    static final int ACCENT = 0xFF8FA8FF, GOLD = 0xFFFFB300, PANEL = 0xFF10141D, PANEL2 = 0xFF0A0D14, BORDER = 0xFF283044, ROW = 0xFF1A2030, ROW_HOVER = 0xFF273046,
            TEXT = 0xFFFFFFFF, SUB = 0xFF9DB2CC, DIM = 0xFF71809A, OFF = 0xFF353E52, GREEN = 0xFF62E6A8, RED = 0xFFFF5555,
            PRICE = 0xFFFFFFFF, PRICE_SRC = 0xFFAAAAAA, MAGIC_FIND = 0xFF55FFFF,
            R_RARE = 0xFF5555FF, R_VERY = 0xFFAA00AA, R_CRAZY = 0xFFFF55FF, R_LEGEND = 0xFFFFAA00;

    /** Colour for a Hypixel drop-odds label ("Occasional", "Pray RNGesus", ...). */
    static int odds(String label) {
        if (label == null) return SUB;
        return switch (label) {
            case "Guaranteed" -> 0xFF9AA5B1; case "Occasional" -> 0xFF8FD694; case "Rare" -> 0xFF6FA8FF;
            case "Extraordinary" -> 0xFFC77DFF; case "Pray RNGesus" -> 0xFFFFC040; case "RNGesus Incarnate" -> 0xFFFF6EC7;
            default -> SUB;
        };
    }
    private Ui() {}
}
