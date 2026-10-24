package dev.fancydrops;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Keeps the chat message format clean. The format is sent to your party / co-op chat, so it may not contain
 * swearing, slurs, links or commands. Plain Java (no Minecraft classes) so it can be unit tested.
 */
public final class ChatFilter {
    public static final Set<String> PLACEHOLDERS = new LinkedHashSet<>(List.of("{item}", "{drop}", "{price}", "{mf}", "{slayer}", "{overbloom}", "{source}"));
    public static final int MAX = 120;

    // whole words (after un-leeting). Plurals and -ing/-ed/-er endings are handled by stemming.
    private static final Set<String> WORDS = new HashSet<>(Arrays.asList(
            "fuck", "fuk", "fck", "fucker", "shit", "bullshit", "bitch", "asshole", "ass", "arse", "dick", "cock", "pussy", "cunt", "twat",
            "slut", "whore", "bastard", "wank", "wanker", "piss", "porn", "sex", "sexy", "nude", "nudes", "rape", "rapist", "cum", "jizz",
            "boob", "tit", "dildo", "penis", "vagina", "anal", "orgasm", "nazi", "hitler", "kys", "retard", "retarded", "fag", "faggot",
            "dyke", "tranny", "coon", "paki", "nigger", "nigga", "chink", "spic", "kike", "gook", "wetback", "beaner", "molest", "pedo", "pedophile",
            "niga", "nigah", "nigaz", "nigguh", "niggah", "niggaz", "niggas", "nikka", "nikker", "nibba", "nibber", "niqqa", "niqqer", "niqer", "niqa", "fagot", "fagg", "faggit", "fgt", "retarted", "chinc", "kyke", "spick", "negro"));
    // long, unambiguous ones are also caught when spaced out or glued into other text ("n i g g e r", "xxfaggotxx")
    private static final List<String> SQUASHED = List.of("nigger", "nigga", "faggot", "fuck", "shit", "bitch", "whore", "retard", "rapist", "pedophile", "wetback", "tranny", "nigg", "nikka", "nikker", "nibba", "nibber", "nigah", "nigaz", "fagg", "chink");
    private static final Pattern LINK = Pattern.compile("(?i)(https?:|www\\.|discord\\.|\\b[a-z0-9-]+\\.(com|net|org|io|gg|ru|xyz|tv|me|co)\\b)");
    private static final Pattern TOKEN = Pattern.compile("[^a-z0-9@$]+");
    private static final Pattern BRACES = Pattern.compile("\\{[^}]*}");

    /** null = fine, otherwise a short reason to show the player. */
    public static String check(String format) {
        if (format == null || format.isBlank()) return "The message can't be empty";
        if (format.length() > MAX) return "Too long (max " + MAX + " characters)";
        if (format.startsWith("/")) return "Can't start with / (that would run a command)";
        if (format.indexOf('\n') >= 0 || format.indexOf('§') >= 0) return "Can't contain colour codes or line breaks";
        var m = BRACES.matcher(format);
        while (m.find()) if (!PLACEHOLDERS.contains(m.group().toLowerCase(Locale.ROOT))) return "Unknown placeholder " + m.group();
        String plain = BRACES.matcher(format).replaceAll(" ");
        if (LINK.matcher(plain).find()) return "Links and server invites aren't allowed";
        if (dirty(plain)) return "Contains a word that isn't allowed";
        return null;
    }

    public static boolean ok(String format) { return check(format) == null; }

    private static boolean dirty(String text) {
        String lower = text.toLowerCase(Locale.ROOT).replaceAll("(?<=[a-z])!(?=[a-z])", "i");   // b!tch
        StringBuilder squash = new StringBuilder();
        for (String raw : TOKEN.split(lower)) {
            if (raw.isEmpty()) continue;
            String t = unleet(raw);
            if (bad(t) || bad(collapse(t, 2))) return true;
            squash.append(t.replaceAll("[^a-z]", ""));
        }
        String sq = collapse(squash.toString(), 2);
        for (String w : SQUASHED) if (sq.contains(w) || collapse(sq, 1).contains(collapse(w, 1))) return true;
        return false;
    }

    private static boolean bad(String t) {
        if (WORDS.contains(t)) return true;
        for (String suf : new String[]{"s", "es", "ing", "ed", "er", "ers", "y", "in"})
            if (t.length() > suf.length() + 2 && t.endsWith(suf) && WORDS.contains(t.substring(0, t.length() - suf.length()))) return true;
        return false;
    }

    private static String unleet(String t) {
        StringBuilder b = new StringBuilder();
        for (char c : t.toCharArray()) b.append(switch (c) { case '0' -> 'o'; case '1' -> 'i'; case '3' -> 'e'; case '4', '@' -> 'a'; case 'q' -> 'g'; case '5', '$' -> 's'; case '7' -> 't'; default -> c; });
        return b.toString();
    }

    /** Squeezes runs of the same letter down to at most {@code max} ("fuuuck" -> "fuuck" / "fuck"). */
    private static String collapse(String s, int max) {
        StringBuilder b = new StringBuilder(); int run = 0; char prev = 0;
        for (char c : s.toCharArray()) {
            run = c == prev ? run + 1 : 1; prev = c;
            if (run <= max) b.append(c);
        }
        return b.toString();
    }

    private ChatFilter() {}
}
