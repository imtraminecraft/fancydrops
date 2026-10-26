package dev.fancydrops;

import dev.fancydrops.AnimationManager.Item;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The words of a drop animation: the name, then the odds / chance / value in labelled columns, then Magic Find. Every letter can
 * move (Animations > Text Changes). The animation sets revealT (seconds since the words began to appear), waveOut (0..1 leaving)
 * and waveT (a running clock) before it calls block().
 */
final class TextFx {
    private TextFx() {}
    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    /** How many lines of text block() draws for this drop (a text lines is 9 px tall at scale 1). */
    static float blockHeight(Item it, float sc, FancyConfig c) {
        float big = 1.5f * sc * c.titleScale, small = 1.1f * sc * c.subScale;
        if (it == null) return 9 * big + 4 + 9 * small;
        boolean facts = hasFacts(it, c), mf = c.showMagicFind && it.mf != null;
        return 9 * big + 4 + 9 * small * (facts ? 1.95f : 1f) + (mf ? 3 + 9 * small : 0);
    }

    /** The whole text of one drop (or the "Nothing" text when it is null), centred on cx, its top edge at y. sc = the scene's text size. */
    static void block(GuiGraphicsExtractor g, Font font, int cx, int y, float sc, Item it, FancyConfig c, int alpha) {
        if (alpha <= 4) return;
        float big = 1.5f * sc * c.titleScale, small = 1.1f * sc * c.subScale;
        int dy = y + Math.round(9 * big) + 4;
        if (it == null) {
            line(g, font, cx, y, big, alpha, lit("Nothing :(").withStyle(ChatFormatting.BOLD), 0xFFB9BFC9);
            line(g, font, cx, dy, small, alpha, lit("Better luck next time"), 0xFF7C8494);
            return;
        }
        boolean facts = hasFacts(it, c);
        line(g, font, cx, y, big, alpha, lit(it.title()).withStyle(ChatFormatting.BOLD), it.color());
        if (facts) columns(g, font, cx, dy, dy + Math.round(9 * small * 0.9f), small, it, c, alpha);
        else detailAt(g, font, cx, dy, small, it, c, alpha);
        if (c.showMagicFind && it.mf != null) { lineDelay = 4f * c.subDelay; line(g, font, cx, dy + Math.round(9 * small * (facts ? 1.95f : 1f)) + 3, small, alpha, lit(mfText(it.mf)), mfColor(it.mf)); lineDelay = 0f; }
    }

    /** The looping words start again from the first letter (an effect was hovered or pressed). */
    /** Seconds into the looping words of the Text editor (0..period). */
    static float loopClock(float period) { return ((System.currentTimeMillis() - textT0) / 1000f) % period; }
    static void restartText() { textT0 = System.currentTimeMillis(); }

    static void outlined(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color) {
        int o = Gfx.argb(color >>> 24, 0x000000);
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) if (dx != 0 || dy != 0) g.text(font, text, x + dx, y + dy, o, false);
        g.text(font, text, x, y, color, false);
    }

    // Text: every letter slams in from large, one after another; a light sweeps across it now and then; it fades out the same way.
    static float waveOut = 0f, waveT = 0f, bounceT = -1f;
    /** Menu previews: freezeT >= 0 holds the scene that many seconds after the words appear; loopTxt replays the words; textOnly leaves the scene out; fxOnly >= 0 shows just that text effect. */
    public static float freezeT = -1f; public static boolean loopTxt, textOnly, barsOnly; public static int fxOnly = -1;
    private static long textT0 = System.currentTimeMillis(); static float textBoost = 1f;
    public static String itemKey;                                     // the Item Changes preview: show this animation's drop (the RNG page)
    public static boolean itemOnly;                                   // the Item Changes preview: just the drop, nothing else
    /** The looping words start again from the first letter (an effect was hovered or pressed). */
    private static boolean fxOn(int i, FancyConfig c) { return fxOnly >= 0 ? fxOnly == i : c.fx(i); }
    static float revealT = 9f, lineDelay = 0f;       // seconds since the words began to appear; the delay of the line being drawn (0 = with the title)

    /** The pieces of a line in the font chosen in Text Changes. */
    private static Object[] restyle(Object[] p) {
        FancyConfig fc = FancyConfig.get(); int f = Fonts.fix(fc.textFont); if (f <= 0 && !fc.textBold) return p;
        Object[] o = p.clone();
        for (int i = 0; i < o.length; i += 2) { Component c = (Component) p[i]; net.minecraft.network.chat.Style st = c.getStyle(); if (f > 0) st = st.withFont(Fonts.desc(f)); if (fc.textBold) st = st.withBold(true); o[i] = c.copy().withStyle(st); }
        return o;
    }
    /** One centred line made of 1-3 coloured pieces, drawn letter by letter. */
    static void line(GuiGraphicsExtractor g, Font font, int cx, int y, float sc, int alpha, Object... parts) {
        parts = restyle(parts);
        int n = 0, w = 0;
        for (int i = 0; i < parts.length; i += 2) { n += ((Component) parts[i]).getString().length(); w += font.width((Component) parts[i]); }
        Gfx.push(g); Gfx.translate(g, cx, y); Gfx.scale(g, sc);
        int x = -w / 2, idx = 0;
        float wi = clamp((revealT - lineDelay) / 0.6f);               // later lines start a little after the title
        float fin = wi * (n + 6), fout = waveOut * (n + 6);
        FancyConfig fx = FancyConfig.get();
        boolean ePop = fxOn(0, fx), eShine = fxOn(1, fx), eWave = fxOn(2, fx), eRain = fxOn(3, fx), eDrop = fxOn(4, fx), ePulse = fxOn(5, fx), eGlitch = fxOn(6, fx), eGlow = fxOn(7, fx);
        boolean eBounce = fxOn(8, fx), eType = fxOn(9, fx), eFloat = fxOn(10, fx), eShake = fxOn(11, fx), eFire = fxOn(12, fx), eFrost = fxOn(13, fx), eTwink = fxOn(14, fx);
        boolean eRise = fxOn(16, fx), eShrink = fxOn(17, fx), eScatter = fxOn(18, fx);
        boolean eSlide = fxOn(19, fx), eFlip = fxOn(20, fx), eSwing = fxOn(21, fx), eGold = fxOn(22, fx), eSlideO = fxOn(23, fx), eMelt = fxOn(24, fx);
        float curX = -1f;
        float sweep = wi >= 1f && eShine ? ((waveT * 0.5f) % 2.2f) / 2.2f * (n + 12) - 6f : -99f;     // a bright band travelling along the letters
        boolean glitching = eGlitch && wi >= 1f && (waveT % 2.6f) < 0.22f;
        float breathe = ePulse && wi >= 1f ? 1f + 0.07f * (float) Math.sin(waveT * 3.2) : 1f;
        if (breathe != 1f) { Gfx.translate(g, 0, 4); Gfx.scale(g, breathe); Gfx.translate(g, 0, -4); }          // Pulse: the whole line breathes together, round its middle
        for (int i = 0; i < parts.length; i += 2) {
            Component part = (Component) parts[i];
            for (char ch : part.getString().toCharArray()) {
                float ain = eType ? ((fin - idx) > 0f ? 1f : 0f) : clamp((fin - idx) / 6f), aout = clamp((fout - idx) / 6f);
                if (eType && idx == (int) fin) curX = x;
                float a = ain * (eShrink ? 1f - clamp((aout - 0.65f) / 0.35f) : 1f - aout);          // Shrink: the letters stay solid while they get small (the fade only finishes it off)
                Component c = Component.literal(String.valueOf(ch)).withStyle(ch == '\ue030' || ch == '\ue031' ? part.getStyle().withFont(EMOJI) : part.getStyle());
                int cw = font.width(c);
                if (a > 0.02f && ch != ' ') {
                    float pop = ePop ? 1f + (1f - ain) * (1f - ain) * 1.9f : 1f;                    // starts big, snaps into place
                    float shine = clamp(1f - Math.abs(idx - sweep) / 3f);
                    float s = pop * (eShrink ? 1f - aout : 1f - aout * 0.35f) * (1f + 0.1f * shine);
                    int col = Canvas.mix((Integer) parts[i + 1], 0xFFFFFFFF, shine * 0.8f);
                    if (eRain) col = Canvas.mix(col, 0xFF000000 | (java.awt.Color.HSBtoRGB((waveT * 0.35f + idx * 0.07f) % 1f, 0.75f, 1f) & 0xFFFFFF), 0.7f);
                    if (eFire) { float fl = 0.5f + 0.5f * (float) Math.sin(waveT * 7.0 + idx * 0.9); col = Canvas.mix(col, Canvas.mix(0xFFFF4A1C, 0xFFFFD84A, fl), 0.78f); }
                    if (eFrost) { col = Canvas.mix(col, 0xFF9FE8FF, 0.3f + 0.3f * (float) Math.sin(waveT * 2.0 + idx * 0.5)); if (((idx * 13) + (int) (waveT * 3f)) % 11 == 0) col = Canvas.mix(col, 0xFFFFFFFF, 0.85f); }
                    if (eGold) { float gs = 0.5f + 0.5f * (float) Math.sin(waveT * 2.4 - idx * 0.45); col = Canvas.mix(col, Canvas.mix(0xFFE0A21B, 0xFFFFF0A0, gs), 0.8f); }
                    boolean flash = eTwink && wi >= 1f && ((idx * 7919 + (int) (waveT * 5f) * 104729) & 0xFFFF) % 9 == 0;
                    if (flash) { col = Canvas.mix(col, 0xFFFFF2B0, 0.9f); s *= 1.15f; }
                    float bob = eWave && wi >= 1f ? (float) Math.sin(waveT * 4.0 + idx * 0.55) * 1.6f : 0f;
                    float jx = 0f;
                    if (eFloat && wi >= 1f) { bob += (float) Math.sin(waveT * 1.6 + idx * 0.15) * 2.2f; jx += (float) Math.sin(waveT * 1.1 + idx * 0.1) * 1.4f; }
                    if (eShake && wi >= 1f) { int hh = (idx * 31 + (int) (waveT * 26f) * 17) & 7; jx += (hh % 3 - 1) * 0.8f; bob += ((hh / 3) % 3 - 1) * 0.8f; }
                    if (eFire && wi >= 1f) bob -= (0.5f + 0.5f * (float) Math.sin(waveT * 7.0 + idx * 0.9)) * 1.2f;
                    float fall = eDrop ? -(1f - ain) * (1f - ain) * 16f : 0f;
                    if (eBounce) fall += -(float) Math.abs(Math.cos(ain * Math.PI * 2.5)) * (1f - ain) * 13f;
                    if (eRise) fall -= aout * 14f;
                    if (eSlide) jx -= (1f - ain) * (1f - ain) * 20f;
                    if (eSlideO) jx += aout * aout * 22f;
                    if (eMelt) fall += aout * aout * 14f;
                    if (eScatter) { int hs = (idx * 2654435 + 12345) & 0xFFFF; jx += ((hs % 9) - 4) * aout * 3f; fall += (((hs / 9) % 9) - 4) * aout * 3f; }
                    Gfx.push(g); Gfx.translate(g, x + cw / 2f + jx, 4f + bob + fall); Gfx.scale(g, s);
                    if (eFlip) g.pose().scale(Math.max(0.05f, (float) Math.sin(ain * Math.PI / 2)), 1f);
                    if (eSwing && wi >= 1f) g.pose().rotate((float) Math.sin(waveT * 2.6 + idx * 0.5) * 0.14f);
                    if (eGlow) { int ga = Math.round(alpha * a * (0.18f + 0.14f * (float) Math.sin(waveT * 3.0 + idx * 0.3))); int gc = Gfx.argb(ga, col & 0xFFFFFF); for (int ox = -2; ox <= 2; ox += 2) for (int oy = -2; oy <= 2; oy += 2) if (ox != 0 || oy != 0) g.text(font, c, -cw / 2 + ox, -4 + oy, gc, false); }
                    if (glitching) {
                        int h = (idx * 7 + (int) (waveT * 40)) % 5 - 2;
                        g.text(font, c, -cw / 2 - 2 + h, -4, Gfx.argb((int) (alpha * a * 0.6f), 0x56D8FF), false);
                        g.text(font, c, -cw / 2 + 2 - h, -4 + 1, Gfx.argb((int) (alpha * a * 0.6f), 0xFF72C6), false);
                    }
                    if (fx.textOutline) outlined(g, font, c, -cw / 2 + (glitching && idx % 3 == 0 ? 1 : 0), -4, Gfx.argb((int) (alpha * a), col));
                    else g.text(font, c, -cw / 2 + (glitching && idx % 3 == 0 ? 1 : 0), -4, Gfx.argb((int) (alpha * a), col), true);
                    Gfx.pop(g);
                }
                x += cw; idx++;
            }
        }
        if (eType && wi > 0f && wi < 1f && curX >= 0f && (int) (waveT * 6f) % 2 == 0) g.fill(Math.round(curX), 0, Math.round(curX) + 1, 8, Gfx.argb(alpha, 0xFFFFFF));
        Gfx.pop(g);
    }

    /** A little piece of text playing one effect: the effect tiles in Text Changes show their own name this way while you hover them. */
    public static void effectLabel(GuiGraphicsExtractor g, Font font, String text, int cx, int y, float sc, int col, int fx, float t) {
        int so = fxOnly; float r0 = revealT, w0 = waveT, wo0 = waveOut, ld = lineDelay;
        fxOnly = fx; lineDelay = 0f; waveT = t;
        if (fx >= 15) { float ph = t % 3.2f; revealT = 5f; waveOut = Math.max(0f, Math.min(1f, (ph - 0.9f) / 1.3f)); } else { revealT = t; waveOut = 0f; }       // exit effects: the words are in, then leave
        try { line(g, font, cx, y, sc, 255, lit(text), col); } finally { fxOnly = so; revealT = r0; waveT = w0; waveOut = wo0; lineDelay = ld; }
    }

    static MutableComponent lit(String s) { return Component.literal(s); }

    static int rows(Item it, FancyConfig c) {
        if (it == null) return 1;
        int n = 0;
        if (hasFacts(it, c)) n += 2;                                                   // the labelled columns: a small label line over the values
        if (c.showMagicFind && it.mf != null) n++;
        if (it.extra != null) n++;
        return n;
    }

    static boolean hasFacts(Item it, FancyConfig c) {
        return (c.showOdds && (!it.odds.isEmpty() || !it.chance.isEmpty())) || (c.showPrice && !it.untradeable && it.price != -1);
    }
    /** Every coin, no K / M / B: 8010000 -> "8,010,000". */
    static String fullCoins(double v) { return String.format(java.util.Locale.ROOT, "%,d", Math.round(v)); }

    /** Odds, chance and value in their own labelled columns with dividers. They arrive one after another, 0.35 s apart, after the title. */
    static void columns(GuiGraphicsExtractor g, Font font, int cx, int yLab, int yVal, float sc, Item it, FancyConfig c, int alpha) {
        java.util.List<Object[]> cols = new java.util.ArrayList<>();
        if (c.showOdds && !it.odds.isEmpty()) cols.add(new Object[]{"ODDS", it.odds, Ui.odds(it.odds), 0.35f});
        if (c.showOdds && !it.chance.isEmpty()) cols.add(new Object[]{"CHANCE", it.chance, Ui.SUB, 0.7f});
        if (c.showPrice && !it.untradeable && it.price != -1) {
            String lab = "VALUE" + (c.showPriceSource && it.price >= 0 && it.priceSource != null && !it.priceSource.isEmpty() ? "  ·  " + it.priceSource.toUpperCase(java.util.Locale.ROOT) : "");
            cols.add(new Object[]{lab, it.price < 0 ? "..." : fullCoins(it.price * Math.max(1, it.amount)) + " coins", Ui.PRICE, 1.05f});
        }
        if (cols.isEmpty()) return;
        float pad = 9f * sc, ls = sc * 0.82f;
        float[] w = new float[cols.size()]; float total = 0;
        for (int i = 0; i < w.length; i++) { w[i] = Math.max(font.width((String) cols.get(i)[1]) * sc, font.width((String) cols.get(i)[0]) * ls); total += w[i] + (i > 0 ? pad * 2 : 0); }
        float x = cx - total / 2f;
        for (int i = 0; i < w.length; i++) {
            Object[] k = cols.get(i); float d = (Float) k[3], xc = x + w[i] / 2f;
            if (i > 0) {
                float prog = clamp(revealT / 0.4f) * (1f - waveOut);
                int sx = Math.round(x - pad);
                g.fill(sx, yLab, sx + 1, yVal + Math.round(9 * sc), Gfx.argb(Math.round(alpha * 0.35f * prog), 0xFFFFFF));
            }
            lineDelay = d * (c.subDelay / 0.35f);
            line(g, font, Math.round(xc), yLab, ls, alpha, lit((String) k[0]), 0xFFB4BDD2);
            line(g, font, Math.round(xc), yVal, sc, alpha, lit((String) k[1]), (Integer) k[2]);
            lineDelay = 0f;
            x += w[i] + pad * 2;
        }
    }

    static void detailAt(GuiGraphicsExtractor g, Font font, int cx, int y, float sc, Item it, FancyConfig c, int alpha) {
        java.util.List<Object> parts = new java.util.ArrayList<>();
        if (c.showOdds && !it.odds.isEmpty()) { parts.add(lit(it.odds)); parts.add(Ui.odds(it.odds)); if (!it.chance.isEmpty()) { parts.add(lit("  " + it.chance)); parts.add(Ui.SUB); } }
        if (c.showPrice && !it.untradeable && it.price != -1) {
            String p = it.price < 0 ? "..." : PriceService.fmt(it.price * Math.max(1, it.amount)) + " coins";
            if (!parts.isEmpty()) { parts.add(lit("   ")); parts.add(Ui.SUB); }
            parts.add(lit(p)); parts.add(Ui.PRICE);
        }
        if (!parts.isEmpty()) line(g, font, cx, y, sc, alpha, parts.toArray());
    }

    static int mfColor(String mf) { return mf != null && mf.startsWith("OB:") ? 0xFFFFFF55 : Ui.MAGIC_FIND; }      // Overbloom is yellow, Magic Find aqua

    static final net.minecraft.network.chat.FontDescription EMOJI = new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath("fancydrops", "emoji"));
    /** "+250%" becomes "250 ✯ Magic Find". */
    static String mfText(String mf) {
        if (mf == null) return "";
        if (mf.startsWith("OB:")) return mf.substring(3).replaceFirst("^\\+", "") + " ✿ Overbloom";           // farming: Overbloom, not Magic Find
        return mf.replaceFirst("^\\+", "").replaceFirst("%$", "") + " ✯ Magic Find"; }

}
