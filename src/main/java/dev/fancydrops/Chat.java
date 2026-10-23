package dev.fancydrops;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** How the mod talks in the player's chat: a gradient name tag, then the message with numbers, Magic Find and commands picked out in their own hex colours. */
final class Chat {
    static final int[] GRADIENT = {0x56D8FF, 0x6885FF, 0xB56BFF, 0xFF72C6, 0xFFB86B};
    static final int TEXT = 0xD5DAE4, DIM = 0x7C8494, NUMBER = 0xFFC857, MF = 0x55E6FF, COMMAND = 0x9AA6FF, GOOD = 0x62E6A8, BAD = 0xFF7B7B, BRACKET = 0x4B5266;

    private static final Pattern TOKEN = Pattern.compile(
            "(?<mf>\\+\\d[\\d,.]*%)|(?<cmd>/(?:fd|fancydrops)\\b)|(?<par>\\([^)]*\\))|(?<good>\\b(?:enabled|on|Sent|Simulating)\\b)|(?<bad>\\b(?:disabled|DISABLED|off|muted)\\b)|(?<num>\\b\\d[\\d,.]*(?:[kKMB]\\b|%)?)");

    static MutableComponent color(String s, int rgb) { return Component.literal(s).withStyle(st -> st.withColor(rgb)); }

    /** [FancyDrops] with the name in the menu's cyan-violet-pink-amber gradient. */
    static MutableComponent prefix() {
        MutableComponent out = Component.empty().append(color("[", BRACKET));
        String name = "FancyDrops";
        for (int i = 0; i < name.length(); i++) {
            float u = i / (float) (name.length() - 1) * (GRADIENT.length - 1);
            int a = GRADIENT[(int) u], b = GRADIENT[Math.min(GRADIENT.length - 1, (int) u + 1)];
            out.append(Component.literal(String.valueOf(name.charAt(i))).withStyle(st -> st.withColor(mix(a, b, u - (int) u)).withBold(true)));
        }
        return out.append(color("]", BRACKET));
    }

    /** The message body: plain text in soft grey-white, with the interesting parts coloured. */
    static MutableComponent body(String text) {
        MutableComponent out = Component.empty();
        Matcher m = TOKEN.matcher(text);
        int at = 0;
        while (m.find()) {
            if (m.start() > at) out.append(color(text.substring(at, m.start()), TEXT));
            int col = m.group("mf") != null ? MF : m.group("cmd") != null ? COMMAND : m.group("par") != null ? DIM
                    : m.group("good") != null ? GOOD : m.group("bad") != null ? BAD : NUMBER;
            out.append(color(m.group(), col));
            at = m.end();
        }
        if (at < text.length()) out.append(color(text.substring(at), TEXT));
        return out;
    }

    static Component msg(String text) {
        String t = text.startsWith("FancyDrops » ") ? text.substring(13) : text;
        return prefix().append(" ").append(body(t));
    }

    static int mix(int a, int b, float f) {
        int r = (int) (((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f), g = (int) (((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f), bl = (int) ((a & 255) * (1 - f) + (b & 255) * f);
        return (r << 16) | (g << 8) | bl;
    }

    private Chat() {}
}
