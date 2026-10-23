package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * Little name suffixes for chosen players (ImTra gets an "Active ON" button after the name: still in chat, animated in the tab list).
 * The player is identified by UUID: we look their current name up in the tab list (or use your own account), then add the
 * emoji after that name in incoming chat lines. It only changes how YOU see the message; nobody else sees it.
 * The emoji is drawn with a font of our own (assets/fancydrops/font/emoji.json), because Minecraft's default font has no emoji.
 */
final class Cosmetics {
    /** The animated "Active ON" button: 8 glyphs of our emoji font, one per frame. Chat shows a still frame, the tab list plays all of them. */
    private static final String[] FRAMES = {"\ue010", "\ue011", "\ue012", "\ue013", "\ue014", "\ue015", "\ue016", "\ue017"};
    /** player uuid -> the still glyph shown in chat */
    private static final Map<UUID, String> SUFFIX = Map.of(UUID.fromString("1350c2e2-3c1e-45fe-984f-fe2e9c2e868a"), FRAMES[3]);
    private static final Map<UUID, Component> TAB_BASE = new HashMap<>();
    private static final FontDescription EMOJI = new FontDescription.Resource(Identifier.fromNamespaceAndPath("fancydrops", "emoji"));

    private record Seg(String text, Style style) {}

    static Component apply(Component message) {
        try {
            Minecraft mc = Minecraft.getInstance();
            String full = message.getString();
            if (full.isEmpty() || mc.getConnection() == null) return message;
            for (Map.Entry<UUID, String> e : SUFFIX.entrySet()) {
                String name = nameOf(mc, e.getKey());
                if (name == null) continue;
                int at = find(full, name);
                if (at < 0) continue;
                return insert(message, at + name.length(), e.getValue());
            }
        } catch (Exception ignored) { }
        return message;
    }

    /** Also puts the animated button after the name in the tab list (called every few ticks: the frame changes about 9 times a second; the server may rewrite the entry, which is picked up as the new base). */
    static void tickTab(Minecraft mc) {
        try {
            if (mc.getConnection() == null) return;
            String frame = FRAMES[(int) ((System.currentTimeMillis() / 110L) % FRAMES.length)];
            for (Map.Entry<UUID, String> e : SUFFIX.entrySet()) {
                var info = mc.getConnection().getPlayerInfo(e.getKey());
                if (info == null || info.getProfile() == null) continue;
                Component cur = info.getTabListDisplayName();
                String curStr = cur == null ? "" : cur.getString();
                boolean ours = false; for (String f : FRAMES) if (curStr.contains(f)) { ours = true; if (f.equals(frame)) return; }
                Component base = ours ? TAB_BASE.get(e.getKey()) : cur;
                if (base == null) {
                    base = Component.literal(info.getProfile().name());
                    try { base = net.minecraft.world.scores.PlayerTeam.formatNameForTeam(info.getTeam(), base); } catch (Throwable ignored) { }
                }
                TAB_BASE.put(e.getKey(), base);
                MutableComponent out = Component.empty().append(base).append(Component.literal(" "));
                out.append(Component.literal(frame).withStyle(Style.EMPTY.withFont(EMOJI).withColor(0xFFFFFF)));
                info.setTabListDisplayName(out);
            }
        } catch (Throwable ignored) { }
    }

    private static String nameOf(Minecraft mc, UUID id) {
        var info = mc.getConnection().getPlayerInfo(id);
        if (info != null && info.getProfile() != null) return info.getProfile().name();
        if (mc.getUser() != null && id.equals(mc.getUser().getProfileId())) return mc.getUser().getName();
        return null;
    }

    /** First whole-word, case-sensitive occurrence of the name, so "ImTrap" or "xImTra" don't match. */
    private static int find(String text, String name) {
        int from = 0;
        while (true) {
            int i = text.indexOf(name, from);
            if (i < 0) return -1;
            int end = i + name.length();
            boolean before = i == 0 || !word(text.charAt(i - 1)), after = end >= text.length() || !word(text.charAt(end));
            if (before && after) return i;
            from = i + 1;
        }
    }
    private static boolean word(char c) { return Character.isLetterOrDigit(c) || c == '_'; }

    private static Component insert(Component message, int offset, String glyph) {
        List<Seg> segs = new ArrayList<>();
        message.visit((style, text) -> { segs.add(new Seg(text, style)); return Optional.empty(); }, Style.EMPTY);
        MutableComponent out = Component.empty();
        int pos = 0; boolean done = false;
        for (Seg s : segs) {
            int len = s.text().length();
            if (!done && offset <= pos + len) {
                int cut = offset - pos;
                out.append(Component.literal(s.text().substring(0, cut)).withStyle(s.style()));
                out.append(Component.literal(" ").withStyle(s.style()));
                out.append(Component.literal(glyph).withStyle(Style.EMPTY.withFont(EMOJI).withColor(0xFFFFFF)));
                if (cut < len) out.append(Component.literal(s.text().substring(cut)).withStyle(s.style()));
                done = true;
            } else out.append(Component.literal(s.text()).withStyle(s.style()));
            pos += len;
        }
        return out;
    }

    private Cosmetics() {}
}
