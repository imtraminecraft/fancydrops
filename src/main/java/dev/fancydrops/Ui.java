package dev.fancydrops;

/** Shared colours (ARGB). */
final class Ui {
    // The menu palette. Menu theme (Settings > Menu) swaps the first eight; the rest never change.
    static int ACCENT = 0xFF9D8CFF, PANEL = 0xFF0F0D1C, PANEL2 = 0xFF090814, BORDER = 0xFF2E2A52, ROW = 0xFF1A1830, ROW_HOVER = 0xFF2A2650, SUB = 0xFFA9A8D6, DIM = 0xFF7774A0;
    static final int GOLD = 0xFFFFB300, TEXT = 0xFFFFFFFF, OFF = 0xFF353E52, GREEN = 0xFF62E6A8, RED = 0xFFFF5555,
            PRICE = 0xFFFFFFFF, PRICE_SRC = 0xFFAAAAAA, MAGIC_FIND = 0xFF55FFFF,
            R_RARE = 0xFF5555FF, R_VERY = 0xFFAA00AA, R_CRAZY = 0xFFFF55FF, R_LEGEND = 0xFFFFAA00;

    static final String[] THEMES = {"Midnight", "Aurora", "Ember", "Forest", "Mono"};
    static final String[] THEME_SUBS = {"Deep blue, the classic", "Violet night, the new look", "Warm coals and orange", "Dark green and mint", "Black and white"};
    static final int[] THEME_COLS = {0xFF8FA8FF, 0xFF9D8CFF, 0xFFFF9A5C, 0xFF62E6A8, 0xFFE6E8EF};
    //                                  ACCENT      PANEL       PANEL2      BORDER      ROW         ROW_HOVER   SUB         DIM
    private static final int[][] PAL = {
        {0xFF8FA8FF, 0xFF10141D, 0xFF0A0D14, 0xFF283044, 0xFF1A2030, 0xFF273046, 0xFF9DB2CC, 0xFF71809A},
        {0xFF9D8CFF, 0xFF0F0D1C, 0xFF090814, 0xFF2E2A52, 0xFF1A1830, 0xFF2A2650, 0xFFA9A8D6, 0xFF7774A0},
        {0xFFFF9A5C, 0xFF16110F, 0xFF0E0A09, 0xFF3A2A22, 0xFF241A16, 0xFF38281F, 0xFFD2B8A6, 0xFF9A8272},
        {0xFF62E6A8, 0xFF0D1613, 0xFF080E0C, 0xFF22403A, 0xFF14221E, 0xFF1F3630, 0xFF9CC9B8, 0xFF6E9486},
        {0xFFE6E8EF, 0xFF131313, 0xFF0B0B0B, 0xFF333333, 0xFF1D1D1D, 0xFF2C2C2C, 0xFFB0B0B0, 0xFF777777}};
    static void apply(int theme) {
        int[] p = PAL[Math.max(0, Math.min(PAL.length - 1, theme))];
        ACCENT = p[0]; PANEL = p[1]; PANEL2 = p[2]; BORDER = p[3]; ROW = p[4]; ROW_HOVER = p[5]; SUB = p[6]; DIM = p[7];
    }

    /** Colour for a Hypixel drop-odds label ("Occasional", "Pray RNGesus", ...). */
    static int odds(String label) {
        if (label == null) return SUB;
        return switch (label) {
            case "Guaranteed" -> 0xFF9AA5B1; case "Occasional" -> 0xFF8FD694; case "Rare" -> 0xFF6FA8FF;
            case "Extraordinary" -> 0xFFC77DFF; case "Pray RNGesus" -> 0xFFFFC040; case "RNGesus Incarnate" -> 0xFFFF6EC7;
            default -> SUB;
        };
    }
    static boolean significant(String odds) { return "Pray RNGesus".equals(odds) || "RNGesus Incarnate".equals(odds); }
    private Ui() {}
}
