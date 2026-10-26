package dev.fancydrops;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Every icon in the menu is called by a plain name written between colons, like a Discord emoji: ":name:". There is one category of icons
 * (no more "suf_" or "cx_" prefixes). The pictures live in IconArt (code-drawn, shaded, a few frames of animation); a name without a picture
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
        scanOverrides();
        int[][] ov = OVR.get(name);
        if (ov != null) { blit(g, ov[ov.length == 1 || now == 2400L ? 0 : (int) ((now / FRAME_MS) % ov.length)], x, y); return; }
        if (name.equals("coin")) { ItemIcons.draw(g, "@COIN", null, x + 8, y + 8, 1f); return; }       // the Hypixel coin head, never a drawn disc
        if (name.equals("griffin_pet")) { if (ItemIcons.hasPixels("GRIFFIN;5")) { ItemIcons.draw(g, "GRIFFIN;5", null, x + 8, y + 8, 1.15f); return; } ItemIcons.prefetch("GRIFFIN;5"); name = "griffin"; }      // the pet's own head (a feather until it loads)
        if (name.startsWith("mob_") && ItemIcons.hasPixels("MOB_" + name.substring(4))) { ItemIcons.draw(g, "MOB_" + name.substring(4), null, x + 8, y + 8, 1.15f); return; }      // the creature's own 3D head (the pixel icon shows until it has loaded)
        if (name.startsWith("mob_")) ItemIcons.prefetch("MOB_" + name.substring(4));
        if (name.startsWith("boss_")) {
            String bid = name.substring(5);
            ItemIcons.draw(g, SlayerData.headId(bid), SlayerData.headFallback(bid), x + 8, y + 8, 1.15f); return;
        }      // a slayer boss: its skull
        if (name.equals("sparkle") || Cosmetics.byId(name) != null) { Cosmetics.drawIcon(g, name, x, y, now); return; }      // the one suffix we have: shaded, six frames
        int frame = now == 2400L ? 0 : (int) ((now / FRAME_MS) % IconArt.FRAMES);
        int[] px = frameOf(name, tint & 0xFFFFFF, frame);
        if (px != null) { blit(g, px, x, y); return; }
        if (credit(g, name, x, y, now)) return;
        placeholder(g, name, tint, x, y, now);
    }

    // ---- your own icons: config/fancydrops/icons/<name>.png. A 16 px wide picture; 16 tall = one still picture, 128 tall = the 8 animation frames stacked top to bottom.
    //      Every icon is saved there once. Edit a file and save it: the game picks it up within a second, no restart.
    private static final String[] EXPORT = {"anim_icons", "anim_menu", "animations", "back", "bars", "bazaar", "bell", "chan_coop", "chan_off", "chan_party", "chan_public", "chat", "clock", "cos_aura", "cos_name", "cos_suffix", "cosmetics", "cube", "dice", "disc", "discord", "patreon", "github", "dock_bottom", "dock_off", "dock_top", "dropped", "dye", "experiments", "fancydrops", "farm", "fishing", "format", "glass", "griffin", "hammer", "menu", "mining", "mytho", "nodrops", "normal", "note", "play", "refresh", "send", "settings", "size", "slayers", "snd_beat", "snd_burst", "snd_end", "snd_linger", "snd_reveal", "sounds", "speed", "stats", "switch", "shovel", "text", "treasure", "updates"};
    private static final java.util.Map<String, int[][]> OVR = new java.util.HashMap<>();
    private static long ovrScan, ovrStamp; private static boolean exported;
    static java.nio.file.Path iconDir() { return DataFiles.art("icons"); }
    private static void scanOverrides() {
        long nw = System.currentTimeMillis();
        if (nw - ovrScan < 1000L) return; ovrScan = nw;
        try {
            java.nio.file.Path dir = iconDir();
            if (!exported) {
                exported = true; java.nio.file.Files.createDirectories(dir);
                java.nio.file.Path manP = dir.resolve("original.txt"); java.util.Map<String, Long> man = new java.util.HashMap<>();
                if (java.nio.file.Files.exists(manP)) for (String ln : java.nio.file.Files.readAllLines(manP)) { int q = ln.indexOf('='); if (q > 0) try { man.put(ln.substring(0, q), Long.parseLong(ln.substring(q + 1).trim())); } catch (NumberFormatException ignored) {} }
                for (String n : EXPORT) {                                                           // each icon is saved as a picture; one you have NOT edited is refreshed when the mod's drawing changes, one you edited is never touched
                    var im = new java.awt.image.BufferedImage(16, 16 * IconArt.FRAMES, java.awt.image.BufferedImage.TYPE_INT_ARGB); boolean ok = true;
                    for (int fr = 0; fr < IconArt.FRAMES; fr++) { int[] px = IconArt.render(n, 0, fr); if (px == null) { ok = false; break; } im.setRGB(0, fr * 16, 16, 16, px, 0, 16); }
                    if (!ok) continue;
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(im, "png", bo); byte[] nb = bo.toByteArray();
                    java.util.zip.CRC32 cn = new java.util.zip.CRC32(); cn.update(nb);
                    java.nio.file.Path f = dir.resolve(n + ".png");
                    if (java.nio.file.Files.exists(f)) {
                        byte[] cur = java.nio.file.Files.readAllBytes(f); java.util.zip.CRC32 cc = new java.util.zip.CRC32(); cc.update(cur);
                        if (java.util.Arrays.equals(cur, nb)) { man.put(n, cn.getValue()); continue; }
                        if (man.containsKey(n)) { if (man.get(n) != cc.getValue()) continue; }      // you edited it: keep your picture
                        else { java.nio.file.Files.createDirectories(dir.resolve("old")); java.nio.file.Files.move(f, dir.resolve("old").resolve(n + "_prev.png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }          // made by an older version, unknown if edited: kept in icons/old
                    }
                    java.nio.file.Files.write(f, nb); man.put(n, cn.getValue());
                }
                StringBuilder sb = new StringBuilder(); for (var e : new java.util.TreeMap<>(man).entrySet()) sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
                java.nio.file.Files.writeString(manP, sb.toString());
            }
            if (!java.nio.file.Files.isDirectory(dir)) return;
            long stamp = 0; java.util.List<java.nio.file.Path> files = new java.util.ArrayList<>();
            try (var st = java.nio.file.Files.list(dir)) { for (java.nio.file.Path f : (Iterable<java.nio.file.Path>) st::iterator) if (f.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) { files.add(f); stamp += java.nio.file.Files.getLastModifiedTime(f).toMillis() + java.nio.file.Files.size(f); } }
            if (stamp == ovrStamp) return; ovrStamp = stamp;
            OVR.clear(); CACHE.clear();
            for (java.nio.file.Path f : files) {
                try {
                    var im = javax.imageio.ImageIO.read(f.toFile()); if (im == null || im.getWidth() != 16 || im.getHeight() % 16 != 0) continue;
                    int n = im.getHeight() / 16; int[][] fr = new int[n][256];
                    for (int i = 0; i < n; i++) im.getRGB(0, i * 16, 16, 16, fr[i], 0, 16);
                    String nm = f.getFileName().toString(); OVR.put(nm.substring(0, nm.length() - 4), fr);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static final long FRAME_MS = 70L;                                          // eight frames, a bit under one second a loop
    private static final java.util.Map<String, int[]> CACHE = new java.util.HashMap<>();
    private static final int[] NONE = new int[0];
    /** One frame of an icon, drawn once and kept (null = the name has no picture). */
    private static int[] frameOf(String name, int tint, int frame) {
        String k = name + "|" + tint + "|" + frame;
        int[] p = CACHE.get(k);
        if (p == null) { p = IconArt.render(name, tint, frame); CACHE.put(k, p == null ? NONE : p); }
        return p == NONE || p == null || p.length == 0 ? null : p;
    }
    /** One fill per run of the same colour, not per pixel. */
    private static void blit(GuiGraphicsExtractor g, int[] pix, int x, int y) {
        for (int yy = 0; yy < 16; yy++) {
            int xx = 0;
            while (xx < 16) {
                int c = pix[yy * 16 + xx];
                if ((c >>> 24) == 0) { xx++; continue; }
                int x2 = xx + 1;
                while (x2 < 16 && pix[yy * 16 + x2] == c) x2++;
                g.fill(x + xx, y + yy, x + x2, y + yy + 1, c);
                xx = x2;
            }
        }
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

    // ---- Credits page icons: the same code-drawn pictures as before the rewrite ----
    private static void r(GuiGraphicsExtractor g, int x, int y, int cx, int cy, int w, int h, int c) { g.fill(x + cx, y + cy, x + cx + w, y + cy + h, c); }
    private static void disc(GuiGraphicsExtractor g, int x, int y, float cx, float cy, float rad, int c) {
        for (int dy = (int) -Math.ceil(rad); dy <= Math.ceil(rad); dy++) {
            float w2 = rad * rad - dy * dy; if (w2 < 0) continue;
            int dx = Math.round((float) Math.sqrt(w2));
            g.fill(x + Math.round(cx) - dx, y + Math.round(cy) + dy, x + Math.round(cx) + dx + 1, y + Math.round(cy) + dy + 1, c);
        }
    }
    private static int al(int a, int rgb) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }
    private static void spark(GuiGraphicsExtractor g, int x, int y, int cx, int cy, int len, int c) {
        r(g, x, y, cx - len, cy, len * 2 + 1, 1, c); r(g, x, y, cx, cy - len, 1, len * 2 + 1, c);
        if (len > 1) { r(g, x, y, cx - 1, cy - 1, 3, 3, al(150, c)); }
    }

    private static boolean credit(GuiGraphicsExtractor g, String name, int x, int y, long now) {
        float s = (now % 3600000L) / 1000f;
        switch (name) {
            case "res_heart" -> {                                                                       // a heart that beats, for the credits page
                float bt = 1f;
                final String[] hs = {"................", "..oo..oo........", ".orro.orro......", "orrwrorrrro.....", "orrrrrrrrro.....", "orrrrrrrrro.....", ".orrrrrrro......", "..orrrrro.......", "...orrro........", "....oro.........", ".....o..........", "................", "................", "................", "................", "................"};
                for (int yy = 0; yy < 11; yy++) for (int xx = 0; xx < 12; xx++) {
                    char ch = hs[yy].length() > xx ? hs[yy].charAt(xx) : '.';
                    int c = ch == 'o' ? 0xFF5A1230 : ch == 'r' ? Canvas.mix(0xFFE5484D, 0xFFFF7A9A, (float) (0.5 + 0.5 * Math.sin(s * 2 + xx * 0.4))) : ch == 'w' ? 0xFFFFFFFF : 0;
                    if (c != 0) r(g, x, y, 2 + Math.round(xx * bt - (bt - 1f) * 6f), 3 + Math.round(yy * bt - (bt - 1f) * 5f), 1, 1, c);
                }
                if (((int) (s * 2f)) % 2 == 0) spark(g, x, y, 13, 3, 1, 0xFFFFD0E0);
            }
            case "res_wiki" -> {                                                                        // an open book with a globe pin
                r(g, x, y, 1, 3, 14, 11, 0xFF1F4E96); r(g, x, y, 2, 4, 6, 9, 0xFFF3EBD0); r(g, x, y, 8, 4, 6, 9, 0xFFE6DCBA); r(g, x, y, 7, 3, 2, 11, 0xFF143468);
                for (int i = 0; i < 4; i++) { r(g, x, y, 3, 6 + i * 2, 4, 1, 0xFF8A8266); r(g, x, y, 9, 6 + i * 2, 4, 1, 0xFFA09A7E); }
                float pu = 0.5f + 0.5f * (float) Math.sin(s * 3f); disc(g, x, y, 11.5f, 4f, 2.2f, al(255, 0xFF5A9CE8)); r(g, x, y, 11, 3, 1, 1, al(160 + Math.round(90 * pu), 0xFFFFFF));
            }
            case "res_neu" -> {                                                                         // NEU: an orange tile with a white N
                r(g, x, y, 1, 1, 14, 14, 0xFF3A2208); r(g, x, y, 2, 2, 12, 12, 0xFFE08A2A); r(g, x, y, 2, 2, 12, 1, 0xFFF8B85A); r(g, x, y, 2, 13, 12, 1, 0xFFB86418);
                r(g, x, y, 4, 4, 2, 8, 0xFFFFFFFF); r(g, x, y, 10, 4, 2, 8, 0xFFFFFFFF); r(g, x, y, 6, 5, 1, 2, 0xFFFFFFFF); r(g, x, y, 7, 7, 2, 2, 0xFFFFFFFF); r(g, x, y, 9, 9, 1, 2, 0xFFFFFFFF);
                int gl = (int) ((s * 6f) % 16f); if (gl < 14) r(g, x, y, 2 + gl, 2, 1, 1, 0xFFFFFFFF);
            }
            case "res_hypixel" -> {                                                                     // Hypixel: a gold H on dark
                r(g, x, y, 1, 1, 14, 14, 0xFF0C1018); r(g, x, y, 2, 2, 12, 12, 0xFF1B2330);
                r(g, x, y, 4, 4, 2, 8, 0xFFFFB400); r(g, x, y, 10, 4, 2, 8, 0xFFFFB400); r(g, x, y, 6, 7, 4, 2, 0xFFFFB400);
                r(g, x, y, 4, 4, 1, 8, 0xFFFFE08A); r(g, x, y, 10, 4, 1, 8, 0xFFFFE08A);
                int gl = (int) ((s * 7f) % 18f); if (gl < 12) r(g, x, y, 2 + gl, 3 + (gl % 3), 1, 1, 0xFFFFFFFF);
            }
            case "res_fabric" -> {                                                                      // Fabric: woven cloth with a stitch
                for (int yy = 0; yy < 14; yy++) for (int xx = 0; xx < 14; xx++) r(g, x, y, 1 + xx, 1 + yy, 1, 1, ((xx / 2 + yy / 2) % 2 == 0) ? 0xFFDCC8A0 : 0xFFBBA27A);
                r(g, x, y, 1, 1, 14, 1, 0xFF8A7650); r(g, x, y, 1, 14, 14, 1, 0xFF8A7650); r(g, x, y, 1, 1, 1, 14, 0xFF8A7650); r(g, x, y, 14, 1, 1, 14, 0xFF8A7650);
                for (int i = 0; i < 6; i++) r(g, x, y, 3 + i * 2, 3 + i * 2, 1, 1, ((int) (s * 4f) + i) % 3 == 0 ? 0xFFFFFFFF : 0xFFF6EBD0);
            }
            case "res_mc" -> {                                                                          // Minecraft: a grass block
                for (int yy = 0; yy < 14; yy++) for (int xx = 0; xx < 14; xx++) {
                    int v = (xx * 7 + yy * 13) % 5; int c = yy < 4 ? (v < 2 ? 0xFF5FA83A : v < 4 ? 0xFF4F9230 : 0xFF74C24A) : (v < 2 ? 0xFF8A5A2C : v < 4 ? 0xFF7A4C22 : 0xFF6B4423);
                    if (yy == 4 && v % 2 == 0) c = 0xFF4F9230;
                    r(g, x, y, 1 + xx, 1 + yy, 1, 1, c);
                }
                r(g, x, y, 1, 14, 14, 1, 0xFF3C2812); r(g, x, y, 14, 1, 1, 14, 0xFF3C2812);
                int gl = (int) ((s * 8f) % 20f); if (gl < 14) r(g, x, y, 1 + gl, 1, 1, 1, 0xFFE8FFD0);
            }
            default -> { return false; }
        }
        return true;
    }
}
