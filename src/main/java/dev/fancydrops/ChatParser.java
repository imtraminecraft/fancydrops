package dev.fancydrops;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads Hypixel's chat lines. No Minecraft classes in here, so it can be tested on its own (see src/test). */
public final class ChatParser {
    public record Drop(String kind, String name, int amount, String mf, boolean sim) {
        public Drop(String kind, String name, int amount, String mf) { this(kind, name, amount, mf, false); }
        /** 0 = RARE, 1 = VERY RARE, 2 = CRAZY RARE, 3 = INSANE / PET / SWEET / RNGESUS. */
        public int rank() {
            return switch (kind) { case "VERY RARE" -> 1; case "CRAZY RARE" -> 2; case "RARE", "RARE CROP" -> 0; default -> 3; };
        }
    }

    // "Your Slayer Kill gave you ..."  (also tolerates "Slayer Boss Kill")
    private static final Pattern KILL = Pattern.compile("\\byour slayer (?:boss )?kill gave you\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUEST_DONE = Pattern.compile("^\\s*SLAYER QUEST COMPLETE!?\\s*$", Pattern.CASE_INSENSITIVE);
    // "RARE DROP! Scythe Blade (+120% ✯ Magic Find)"
    private static final Pattern DROP = Pattern.compile("^\\s*((?:VERY |CRAZY )?RARE|CRAZY|INSANE|PET|SWEET|RNGESUS)\\s+(DROP|CROP)!\\s*(.+?)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MF = Pattern.compile("\\s*\\(\\+([\\d.,]+)%.*\\)\\s*$");
    // farming lines carry Overbloom instead of Magic Find: "(+25 ☘ Overbloom)"
    private static final Pattern OVERBLOOM = Pattern.compile("\\s*\\(\\+?([\\d.,]+)%?[^)]*Overbloom[^)]*\\)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_PREFIX = Pattern.compile("^(\\d+)\\s*[x×]\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_SUFFIX = Pattern.compile("^(.+?)\\s*[x×]\\s*(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_PARENS = Pattern.compile("^(.+?)\\s*\\((\\d+)x\\)$", Pattern.CASE_INSENSITIVE);

    public static boolean isKill(String line) { return line != null && KILL.matcher(line).find(); }
    public static boolean isQuestComplete(String line) { return line != null && QUEST_DONE.matcher(line).matches(); }

    /** The drop on this line, or null. */
    public static Drop parseDrop(String line) {
        if (line == null) return null;
        Matcher m = DROP.matcher(line);
        if (!m.matches()) return null;
        String kind = m.group(1).toUpperCase(Locale.ROOT) + (m.group(2).equalsIgnoreCase("CROP") ? " CROP" : "");
        Matcher mfm = MF.matcher(m.group(3)), obm = OVERBLOOM.matcher(m.group(3));
        String mf = obm.find() ? "OB:+" + obm.group(1) : mfm.find() ? "+" + mfm.group(1) + "%" : null;
        String name = OVERBLOOM.matcher(MF.matcher(m.group(3)).replaceFirst("")).replaceFirst("").trim();
        int amount = 1;
        Matcher am = AMOUNT_PREFIX.matcher(name);
        if (am.matches()) { amount = amount(am.group(1)); name = am.group(2).trim(); }
        else if ((am = AMOUNT_PARENS.matcher(name)).matches()) { amount = amount(am.group(2)); name = am.group(1).trim(); }
        else if ((am = AMOUNT_SUFFIX.matcher(name)).matches()) { amount = amount(am.group(2)); name = am.group(1).trim(); }
        return new Drop(kind, name, amount, mf);
    }

    private static int amount(String raw) {
        try { return Math.max(1, Math.min(9999, Integer.parseInt(raw.replace(",", "")))); } catch (Exception e) { return 1; }
    }
    private ChatParser() {}
}
