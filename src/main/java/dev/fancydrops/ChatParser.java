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
    // "RARE DROP! Scythe Blade (+120% ✯ Magic Find)"
    private static final Pattern DROP = Pattern.compile("(?:^|(?<=[\\s\\]>»:*]))((?:VERY |CRAZY )?RARE|CRAZY|INSANE|PET|SWEET|RNGESUS)\\s+(DROP|CROP)!\\s*(.+?)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MF_ANY = Pattern.compile("\\(\\+([\\d.,]+)%");
    // farming lines carry Overbloom instead of Magic Find: "(+25 ☘ Overbloom)"
    private static final Pattern OVERBLOOM = Pattern.compile("\\s*\\(\\+?([\\d.,]+)%?[^)]*Overbloom[^)]*\\)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_PREFIX = Pattern.compile("^(\\d+)\\s*[x×]\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_SUFFIX = Pattern.compile("^(.+?)\\s*[x×]\\s*(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_PARENS = Pattern.compile("^(.+?)\\s*\\((\\d+)x\\)$", Pattern.CASE_INSENSITIVE);

    /** A player's chat line ("[MVP+] Name: text", "Party > Name: text"): never a drop, whatever it says. Hypixel's own lines have no "Name: " in front of the keyword. */
    public static boolean isPlayerChat(String line) {
        if (line == null) return false;
        int i = line.indexOf(": ");
        if (i < 0) return false;
        String before = line.substring(0, i).toUpperCase(Locale.ROOT);
        return !before.contains("DROP!") && !before.contains("CROP!");
    }


    /** The drop on this line, or null. Tolerant: leading symbols / tags, odd spaces and any trailing "(...)" groups are fine. */
    public static Drop parseDrop(String line) {
        if (line == null) return null;
        line = line.replace('\u00a0', ' ').replaceAll("[\\u200b-\\u200f\\u2060\\ufeff]", "");
        Matcher m = DROP.matcher(line);
        if (!m.find()) return null;
        String kind = m.group(1).toUpperCase(Locale.ROOT) + (m.group(2).equalsIgnoreCase("CROP") ? " CROP" : "");
        String rest = m.group(3).trim();
        String mf = null;
        // peel "(...)" groups off the end: one of them holds Magic Find / Overbloom
        while (rest.endsWith(")")) {
            int depth = 0, open = -1;
            for (int i = rest.length() - 1; i >= 0; i--) {
                char ch = rest.charAt(i);
                if (ch == ')') depth++;
                else if (ch == '(' && --depth == 0) { open = i; break; }
            }
            if (open <= 0) break;
            String group = rest.substring(open);
            if (!group.matches("(?is).*(%|magic find|overbloom|\\+\\s*\\d).*")) break;     // "(Duplex I)" is the item's name, not a stat
            if (mf == null) {
                Matcher ob = OVERBLOOM.matcher(" " + group), mm = MF_ANY.matcher(group);
                if (ob.find()) mf = "OB:+" + ob.group(1);
                else if (mm.find()) mf = m.group(2).equalsIgnoreCase("CROP") ? "OB:+" + mm.group(1) : "+" + mm.group(1) + "%";
                else {
                    Matcher num = Pattern.compile("\\+\\s*([\\d.,]+)").matcher(group);
                    if (num.find() && m.group(2).equalsIgnoreCase("CROP")) mf = "OB:+" + num.group(1);
                }
            }
            rest = rest.substring(0, open).trim();
        }
        String name = rest.replaceAll("[\\s!.:;,✯☘☠♣-]+$", "").trim();
        name = name.replaceFirst("(?i)^You (?:dug out|found|unearthed)\\s+(?:an?\\s+|some\\s+)?", "").trim();          // a Diana burrow: "You dug out a Griffin Feather"
        name = name.replaceAll("\\s+", " ");                                      // "353x  Flawed Opal Gemstone" has two spaces
        while (name.endsWith(")") && count(name, '(') < count(name, ')')) name = name.substring(0, name.length() - 1).trim();      // "5x Wisp's Potion)": the bracket was never opened
        while (name.startsWith("(") && count(name, '(') > count(name, ')')) name = name.substring(1).trim();
        name = name.replaceFirst("^[^\\p{L}\\p{N}\\[(\"'+]+", "").trim();          // a leading symbol is not part of the name
        if (name.startsWith("(") && name.endsWith(")") && name.indexOf('(', 1) < 0) name = name.substring(1, name.length() - 1).trim();   // "(Duplex I)"
        if (name.isEmpty()) return null;
        int amount = 1;
        Matcher am = AMOUNT_PREFIX.matcher(name);
        if (am.matches()) { amount = amount(am.group(1)); name = am.group(2).trim(); }
        else if ((am = AMOUNT_PARENS.matcher(name)).matches()) { amount = amount(am.group(2)); name = am.group(1).trim(); }
        else if ((am = AMOUNT_SUFFIX.matcher(name)).matches()) { amount = amount(am.group(2)); name = am.group(1).trim(); }
        return new Drop(kind, name, amount, mf);
    }

    private static int count(String s, char c) { int n = 0; for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++; return n; }
    private static int amount(String raw) {
        try { return Math.max(1, Math.min(9999, Integer.parseInt(raw.replace(",", "")))); } catch (Exception e) { return 1; }
    }
    private ChatParser() {}
}
