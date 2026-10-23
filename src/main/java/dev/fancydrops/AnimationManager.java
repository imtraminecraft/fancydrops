package dev.fancydrops;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayDeque;

/**
 * Plays the drop scenes (Scene.java draws them; this class owns timing, layout, text and sound).
 *   tier 0  default - a quick chop; with no item at all it is the "nothing dropped" scene
 *   tier 1  cool    - crazy-rare class drops
 *   tier 2  epic    - drops worth the epic threshold (20M by default)
 * One scene at a time; the rest wait in a queue.
 */
public final class AnimationManager {
    /** Everything the text under the scene needs about one drop. */
    public static final class Item {
        public SlayerData.Slayer slayer = SlayerData.OTHER;
        public String name = "", id = "", icon = "nether_star", odds = "", chance = "", mf;
        public int rank, amount = 1;
        public boolean untradeable, announce, sim;       // sim: from /fd simulate - never counted in the profit tracker     // announce: send the party / co-op message when this drop's scene starts
        public volatile double price = -2;       // -2 = still loading, -1 = no price
        public volatile String source = "";
        public String extra; public int extraCol = Ui.SUB;     // one more line under the price (a pet's rarity and what George pays)
        public int color() {
            return switch (rank) { case 0 -> Ui.R_RARE; case 1 -> Ui.R_VERY; case 2 -> Ui.R_CRAZY; default -> Ui.R_LEGEND; };
        }
        public String title() { return amount > 1 ? amount + " " + name : name; }
    }

    public static final class Anim {
        final Item item;                         // null = nothing dropped
        Item item2;                              // a second drop from the same kill: both play in one scene
        final SlayerData.Slayer slayer;
        final int forcedTier;                    // -1 = work it out
        final long created = System.currentTimeMillis();
        final Runnable onStart;                  // lets the kill tracker close its session
        boolean exp;                             // an Experimentation Table drop: the book scene
        boolean myth;                            // a Mythological Ritual drop: the lightning-and-rune-circle scene (has bars, blur and a position like Dye)
        boolean farm, helios;                    // a Farming drop: the small bar-less scene (helios: the Ray of Helios, a longer sunrise version)
        float speed = 1f;                        // scene seconds per real second (the animation length setting)
        float textScene = -1f;                   // when the title and info start, in scene seconds (the text delay setting)
        Scene.Params p; int tier; long start; int frame = -1; float sx, sy;
        boolean isMyth() { return myth || (slayer != null && MythData.ALL.contains(slayer)); }       // every drop of a Mythological creature gets the Mythological scene, whichever way it was started
        boolean isExp() { return exp || (slayer != null && slayer == ExperimentData.slayer()); }
        String scene() { return helios ? "helios" : farm ? "farm" : item == null && !isExp() ? (isMyth() ? "mythnothing" : "nothing") : isExp() ? "exp" : isMyth() ? "mytho" : p != null && p.dye ? "dye" : SoundFx.SCENES[Math.max(0, Math.min(2, tier))]; }
        final boolean[] done = new boolean[24];
        public Anim(SlayerData.Slayer slayer, Item item, int forcedTier, Runnable onStart) {
            this.slayer = slayer; this.item = item; this.forcedTier = forcedTier; this.onStart = onStart;
        }
        boolean waiting() {
            FancyConfig c = FancyConfig.get();
            if (System.currentTimeMillis() - created > 1500) return false;                  // never hold a scene back for long
            if (item == null) return false;
            if (!ItemIcons.ready(item.id) || (item2 != null && !ItemIcons.ready(item2.id))) return true;
            if (forcedTier >= 0) return false;
            return (!item.untradeable && item.price == -2 || item2 != null && !item2.untradeable && item2.price == -2) && (c.epicEffects || c.showPrice);
        }
    }

    /** With cinematic bars: top, middle or bottom of the picture between the bars. */
    public static final String[] POS_BAR_NAMES = {"Top Center", "Middle Center", "Bottom Center"};
    /** Without bars: a 3 x 3 grid, row by row. */
    public static final String[] POS9_NAMES = {"Top Left", "Top Center", "Top Right", "Middle Left", "Middle Center", "Middle Right", "Bottom Left", "Bottom Center", "Bottom Right"};
    public static final String[] TEXT_POS_NAMES = {"Top", "Bottom"};

    /** The name of the position the current settings use. */
    public static String positionName(FancyConfig c) { return positionName(c, 0); }
    /** Farming offers the 3 x 3 grid and a custom spot you place yourself in the preview. */
    public static final String[] FARM_POS_NAMES = {"Top Left", "Top Center", "Top Right", "Middle Left", "Middle Center", "Middle Right", "Bottom Left", "Bottom Center", "Bottom Right", "Custom"};
    /** Every scene has its own position: cat 0 = Slayers, 1 = Dye, 2 = Farming. */
    public static int posBarsOf(FancyConfig c, int cat) { return Math.floorMod(cat == 1 && c.dyePosBars >= 0 ? c.dyePosBars : cat == 3 && c.mythPosBars >= 0 ? c.mythPosBars : cat == 4 && c.expPosBars >= 0 ? c.expPosBars : c.posBars, 3); }
    public static int pos9Of(FancyConfig c, int cat) { return Math.floorMod(cat == 1 && c.dyePos9 >= 0 ? c.dyePos9 : cat == 3 && c.mythPos9 >= 0 ? c.mythPos9 : cat == 4 && c.expPos9 >= 0 ? c.expPos9 : c.pos9, 9); }
    public static String positionName(FancyConfig c, int cat) {
        if (cat == 2) return FARM_POS_NAMES[Math.max(0, Math.min(9, c.farmPos))];
        return c.cine(cat) ? POS_BAR_NAMES[posBarsOf(c, cat)] : POS9_NAMES[pos9Of(c, cat)];
    }
    public static boolean textChoice(FancyConfig c) { return textChoice(c, 0); }
    /** True when the text can be put above or below the scene by choice (always with bars; in the middle row without). */
    public static boolean textChoice(FancyConfig c, int cat) {
        if (cat == 2) return c.farmPos != 9 && c.farmPos / 3 == 1;
        return c.cine(cat) || pos9Of(c, cat) / 3 == 1;
    }
    static int catOf(Anim a) { return a.farm ? 2 : a.isExp() ? 4 : a.isMyth() ? 3 : a.p != null && a.p.dye ? 1 : 0; }

    static boolean isDye(Item it) { return it != null && it.name != null && it.name.toLowerCase(java.util.Locale.ROOT).endsWith(" dye"); }

    /** The colour of a dye, from its name (the SkyBlock dye's own colour). */
    static int dyeColor(String name, int fallback) {
        String n = name == null ? "" : name.toLowerCase(java.util.Locale.ROOT);
        String[][] m = {{"matcha", "7CC23E"}, {"brick red", "C0392B"}, {"celeste", "5CC8FF"}, {"byzantium", "8E2D8F"}, {"flame", "FF6A1A"}, {"sangria", "A3123C"},
                {"aquamarine", "7FFFD4"}, {"bone", "E8E2CC"}, {"carmine", "960018"}, {"emerald", "2ECC71"}, {"jade", "00A86B"}, {"mango", "FFB52E"},
                {"nadeshiko", "F6ADC6"}, {"necron", "D23A3A"}, {"pearlescent", "EAE6FF"}, {"pelt", "7A5230"}, {"periwinkle", "8F8CFF"}, {"tentacle", "2A6F7A"}, {"warden", "0A5C5C"},
                {"holly", "1F7A3A"}, {"iceberg", "BFE8FF"}, {"midnight", "1C1C4A"}, {"cyclamen", "D4378E"}, {"copper", "B87333"}, {"dung", "5B4A2A"}, {"livid", "4A90D9"}, {"wild strawberry", "E8456B"}, {"mythological", "5CD6C8"}};
        for (String[] e : m) if (n.contains(e[0])) return Integer.parseInt(e[1], 16);
        return fallback & 0xFFFFFF;
    }

    public static int tierOf(Item it) {
        FancyConfig c = FancyConfig.get();
        if (it == null) return 0;
        if (c.epicEffects && isDye(it)) return 2;   // every dye gets the big scene
        if (c.epicEffects && !it.untradeable && it.price * Math.max(1, it.amount) >= c.epicThreshold) return 2;
        if (it.slayer != null && "vampire".equals(it.slayer.id)) {                          // Riftstalker: blue chances play Rare Drop, purple chances play RNG Drop
            if ("Extraordinary".equals(it.odds)) return c.epicEffects ? 2 : 1;
            if ("Rare".equals(it.odds)) return 1;
        }
        if (it.rank >= 2 || Ui.significant(it.odds)) return 1;
        return 0;
    }

    // ------------------------------------------------------------------ layout (shared with the HUD editor)

    /** Everything is sized from the screen, so a 1080 x 1080 window looks the same as a 512 x 512 one. */
    static float uk(int W, int H) { return Math.max(0.55f, Math.min(W, H) / 540f); }

    /** ox,oy = top-left of the scene; nameY = first text line; bx0..by1 = the box around scene and text. */
    public record Layout(int cx, float cell, float tsc, int lineH, int ox, int oy, int nameY, int sw, int sh, int bx0, int by0, int bx1, int by1) {
        public int lineY(int k) { return nameY + (k + 1) * lineH; }
    }

    /**
     * With cinematic bars the scene sits in the picture between them (top, middle or bottom centre) and never touches a bar.
     * Without bars it sits on a 3 x 3 grid, never gets cut off by the screen, and the text goes on the side with room:
     * below a scene at the top, above a scene at the bottom, and by choice (Text position) when it is in the middle.
     */
    public static Layout layout(int W, int H, FancyConfig c, int rows) { return layout(W, H, c, rows, 0); }

    /** farm = the small Farming scene: never framed by bars, has its own position and size, slightly smaller text. */
    public static Layout layout(int W, int H, FancyConfig c, int rows, int cat) {
        boolean farm = cat == 2;
        int m = Math.min(W, H);
        boolean bars = c.cine(cat) && !farm;
        float tsc = c.textK() * uk(W, H) * (farm ? 0.9f : 1f);
        int lineH = Math.max(6, Math.round(11 * tsc)), textH = lineH * (rows + 1);
        float want = (farm ? Math.max(0.5f, Math.min(1.1f, c.farmScale)) * 0.24f : (bars ? c.sizeOf(cat) : Math.max(1f, c.sizeOf(cat))) * 0.8f * 0.4f) * m / Scene.W;
        if (bars) {
            int bar = Math.round(H * c.barHeight / 100f), side = Math.round(bar * SIDE_K), frameH = H - 2 * bar, frameW = W - 2 * side;
            float fit = Math.min(frameH * 0.97f / (Scene.H * 1.12f), frameW * 0.97f / (Scene.W * 1.12f));      // 1.12 = how far the scene punches out at the reveal
            float cell = Math.max(0.2f, Math.min(want, fit));
            int sw = Math.round(Scene.W * cell), sh = Math.round(Scene.H * cell);
            int margin = Math.round(0.06f * sh + 0.02f * frameH), pb = posBarsOf(c, cat);
            int sx = (W - sw) / 2, sy = pb == 0 ? bar + margin : pb == 2 ? H - bar - sh - margin : bar + (frameH - sh) / 2;
            return new Layout(W / 2, cell, tsc, lineH, sx, sy, sy, sw, sh, sx, sy, sx + sw, sy + sh);
        }
        int mg = Math.round(0.03f * m), gap = Math.max(2, lineH / 4), p9 = farm ? Math.max(0, Math.min(9, c.farmPos)) : pos9Of(c, cat), col = p9 % 3, row = p9 / 3;
        boolean custom = farm && p9 == 9;
        if (custom) { col = 1; row = c.farmY < 0.5f ? 0 : 2; }
        boolean below = row == 0 || (row == 1 && c.textPos == 1);
        int lift = row == 2 && col == 1 && !custom ? Math.max(0, 64 - mg) : 0;               // bottom centre sits above the held item's name, which hangs over the hotbar
        float roomH = (row == 1 ? H - 2 * (mg + textH + gap) : H - 2 * mg - textH - gap) - lift;
        float cell = Math.max(0.2f, Math.min(want, Math.min(roomH / (Scene.H * 0.92f), (W - 2 * mg) / (float) Scene.W)));
        int sw = Math.round(Scene.W * cell), sh = Math.round(Scene.H * cell), vis = Math.round(Scene.H * cell * 0.92f);
        int half = Math.min((W - 2 * mg) / 2, Math.max(sw / 2, Math.round(110 * tsc)));
        int cx = col == 0 ? mg + half : col == 2 ? W - mg - half : W / 2;
        if (custom) cx = Math.max(mg + half, Math.min(W - mg - half, Math.round(c.farmX * W)));
        int oy, nameY;
        if (custom) {                                                             // placed by hand: the scene's centre goes where it was dropped
            int blockH = vis + gap + textH, cyc = Math.round(c.farmY * H);
            int top = Math.max(mg, Math.min(H - mg - blockH, below ? cyc - vis / 2 : cyc - blockH + vis / 2));
            if (below) { oy = top; nameY = top + vis + gap; } else { nameY = top; oy = top + textH + gap - Math.round(0.04f * sh); }
        } else if (row == 1) {                                                           // the scene stays in the middle, the text hangs off it
            oy = (H - sh) / 2;
            nameY = below ? oy + vis + gap : oy + Math.round(0.04f * sh) - textH - gap;
        } else {
            int blockH = vis + gap + textH, top = row == 0 ? mg : H - mg - lift - blockH;
            if (below) { oy = top; nameY = top + vis + gap; } else { nameY = top; oy = top + textH + gap - Math.round(0.04f * sh); }
        }
        int bx0 = cx - half, bx1 = cx + half, by0 = Math.min(oy, nameY), by1 = Math.max(oy + sh, nameY + textH);
        return new Layout(cx, cell, tsc, lineH, cx - sw / 2, oy, nameY, sw, sh, bx0, by0, bx1, by1);
    }

    // ------------------------------------------------------------------ queue

    private static final ArrayDeque<Anim> QUEUE = new ArrayDeque<>();
    private static Anim cur;
    private static final Canvas OUT = new Canvas(Scene.W, Scene.H), L3 = new Canvas(Scene.W, Scene.H);

    public static void play(Anim a) { QUEUE.add(a); }
    public static boolean isPlaying() { return cur != null || !QUEUE.isEmpty(); }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static int mobOf(String id) {
        String[] ids = {"minos_hunter", "siamese_lynxes", "gaia_construct", "minotaur", "minos_champion", "minos_inquisitor", "manticore", "king_minos", "stranded_nymph", "cretan_bull", "harpy", "sphinx"};
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(id)) return i;
        return Math.abs(id.hashCode()) % ids.length;
    }

    /** The settings key of an animation's length: nothing, default, rare, rng, dye, farm, helios, myth, mythrng. */
    static String typeKey(Anim a) {
        if (a.farm) return a.helios ? "helios" : "farm";
        if (a.isExp()) return a.tier == 2 ? "exprng" : "exp";
        if (a.isMyth()) return a.item == null ? "mythnothing" : a.tier == 2 ? "mythrng" : "myth";
        if (a.p != null && a.p.dye) return "dye";
        if (a.item == null) return "nothing";
        return a.tier == 2 ? "rng" : a.tier == 1 ? "rare" : "default";
    }
    /** The whole animation in scene seconds, text included. */
    static float naturalScene(Scene.Params p, boolean farm) { return farm ? Scene.splitTime(p) + 3.15f : Scene.length(p) + 1.8f; }
    /** {scene seconds per real second, when the words start in scene seconds, total real seconds}. The animation plays first; title and info follow after the text delay. */
    static float[] timing(Scene.Params p, boolean farm, String key, FancyConfig c) {
        float natural = naturalScene(p, farm), ts = Scene.splitTime(p), delay = farm ? 0f : c.textDelay(key);      // farming: the words follow the reveal by half a second
        Float want = c.animSecs.get(key);
        float total = Math.max(5f, Math.max(want != null ? Math.max(5f, Math.min(10f, want)) : natural, delay + 3.5f));
        float speed = natural / total;
        float textReal = farm ? (ts + 0.5f) / speed : Math.max(delay, (ts + 0.12f) / speed + 0.3f);
        return new float[]{speed, textReal * speed, total};
    }
    /** How long an animation lasts by default, in real seconds, for the length slider. */
    public static float naturalSecs(String key, FancyConfig c) {
        c.fixChops();
        int t = switch (key) { case "rng", "dye", "helios", "mythrng", "exprng" -> 2; case "rare" -> 1; default -> 0; };
        Scene.Params p = new Scene.Params(t, !key.endsWith("nothing"));
        p.chops = Math.max(1, Math.min(20, t == 2 ? c.chopsEpic : t == 1 ? c.chopsCool : c.chopsDefault));
        p.style = c.slayerStyle;
        boolean farm = false;
        switch (key) {
            case "dye" -> { p.dye = true; p.chops = 1; }
            case "farm", "helios" -> { p.farm = true; p.chops = 3; p.helios = key.equals("helios"); farm = true; }
            case "myth", "mythrng", "mythnothing" -> { p.myth = true; p.chops = key.equals("mythrng") ? c.mythStrikesRng : c.mythStrikes; }
            case "exp", "exprng" -> { p.exp = true; p.chops = key.equals("exprng") ? c.expFlipsRng : c.expFlips; }
            default -> { }
        }
        return Math.round(Math.max(5f, Math.max(naturalScene(p, farm), c.textDelay(key) + 3.5f)) * 10f) / 10f;
    }

    private static Scene.Params params(Anim a, Item it, int tier) {
        Scene.Params p = new Scene.Params(tier, it != null);
        FancyConfig c = FancyConfig.get();
        c.fixChops();
        p.chops = Math.max(1, Math.min(20, tier == 2 ? c.chopsEpic : tier == 1 ? c.chopsCool : c.chopsDefault));
        SlayerData.Cube cube = a.slayer.cube;
        p.base = SlayerData.hex(cube.base, p.base); p.dark = SlayerData.hex(cube.dark, p.dark); p.eye = SlayerData.hex(cube.eye, p.eye);
        p.style = c.slayerStyle;
        if (a.farm) { p.farm = true; p.chops = 3; p.helios = a.helios; }
        if (a.isExp()) { p.exp = true; p.chops = tier == 2 ? c.expFlipsRng : c.expFlips; }
        else if (a.isMyth()) { p.myth = true; p.chops = tier == 2 ? c.mythStrikesRng : c.mythStrikes; p.mob = mobOf(a.slayer.id); }
        p.seed = Math.abs(a.slayer.id.hashCode() % 97);                                   // the mob's look stays the same
        p.cutSeed = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);   // but the slashes are different every time
        p.theme = java.util.List.of("zombie", "spider", "wolf", "enderman", "blaze", "vampire").indexOf(a.slayer.id);
        if (p.theme < 0) p.theme = 6;
        if (it != null) { p.rarity = it.color(); p.item = ItemIcons.model(it.id, a.slayer.color()); }
        if (a.item2 != null && tier >= 0) { p.twin = true; p.item2 = ItemIcons.model(a.item2.id, a.slayer.color()); p.rarity2 = a.item2.color(); p.chops = Math.min(20, p.chops + 2); }
        if (isDye(it) && tier == 2) { p.dye = true; p.chops = 1; p.dyeCol = dyeColor(it.name, p.rarity); p.rarity = p.dyeCol | 0xFF000000; }
        return p;
    }

    // ------------------------------------------------------------------ text

    private static void outlined(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int color) {
        int o = Gfx.argb(color >>> 24, 0x000000);
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) if (dx != 0 || dy != 0) g.text(font, text, x + dx, y + dy, o, false);
        g.text(font, text, x, y, color, false);
    }

    // Text: every letter slams in from large, one after another; a light sweeps across it now and then; it fades out the same way.
    private static float waveIn = 1f, waveOut = 0f, waveT = 0f, bounceT = -1f;
    /** Menu previews: freezeT >= 0 holds the scene that many seconds after the words appear; loopTxt replays the words; textOnly leaves the scene out; fxOnly >= 0 shows just that text effect. */
    public static float freezeT = -1f; public static boolean loopTxt, textOnly; public static int fxOnly = -1;
    private static long textT0 = System.currentTimeMillis(); private static float textBoost = 1f;
    public static boolean itemOnly;                                   // the Item Changes preview: just the drop, nothing else
    /** The looping words start again from the first letter (an effect was hovered or pressed). */
    public static void restartText() { textT0 = System.currentTimeMillis(); }
    private static boolean fxOn(int i, FancyConfig c) { return fxOnly >= 0 ? fxOnly == i : c.fx(i); }
    private static float revealT = 9f, lineDelay = 0f;       // seconds since the words began to appear; the delay of the line being drawn (0 = with the title)

    /** One centred line made of 1-3 coloured pieces, drawn letter by letter. */
    private static void line(GuiGraphicsExtractor g, Font font, int cx, int y, float sc, int alpha, Object... parts) {
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
        float curX = -1f;
        float sweep = wi >= 1f && eShine ? ((waveT * 0.5f) % 2.2f) / 2.2f * (n + 12) - 6f : -99f;     // a bright band travelling along the letters
        boolean glitching = eGlitch && wi >= 1f && (waveT % 2.6f) < 0.22f;
        float breathe = ePulse && wi >= 1f ? 1f + 0.07f * (float) Math.sin(waveT * 3.2) : 1f;
        for (int i = 0; i < parts.length; i += 2) {
            Component part = (Component) parts[i];
            for (char ch : part.getString().toCharArray()) {
                float ain = eType ? ((fin - idx) > 0f ? 1f : 0f) : clamp((fin - idx) / 6f), aout = clamp((fout - idx) / 6f);
                if (eType && idx == (int) fin) curX = x;
                float a = ain * (1f - aout);
                Component c = Component.literal(String.valueOf(ch)).withStyle(ch == '\ue030' || ch == '\ue031' ? part.getStyle().withFont(EMOJI) : part.getStyle());
                int cw = font.width(c);
                if (a > 0.02f && ch != ' ') {
                    float pop = ePop ? 1f + (1f - ain) * (1f - ain) * 1.9f : 1f;                    // starts big, snaps into place
                    float shine = clamp(1f - Math.abs(idx - sweep) / 3f);
                    float s = pop * breathe * (eShrink ? 1f - aout : 1f - aout * 0.35f) * (1f + 0.1f * shine);
                    int col = Canvas.mix((Integer) parts[i + 1], 0xFFFFFFFF, shine * 0.8f);
                    if (eRain) col = Canvas.mix(col, 0xFF000000 | (java.awt.Color.HSBtoRGB((waveT * 0.35f + idx * 0.07f) % 1f, 0.75f, 1f) & 0xFFFFFF), 0.7f);
                    if (eFire) { float fl = 0.5f + 0.5f * (float) Math.sin(waveT * 7.0 + idx * 0.9); col = Canvas.mix(col, Canvas.mix(0xFFFF4A1C, 0xFFFFD84A, fl), 0.78f); }
                    if (eFrost) { col = Canvas.mix(col, 0xFF9FE8FF, 0.3f + 0.3f * (float) Math.sin(waveT * 2.0 + idx * 0.5)); if (((idx * 13) + (int) (waveT * 3f)) % 11 == 0) col = Canvas.mix(col, 0xFFFFFFFF, 0.85f); }
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
                    if (eScatter) { int hs = (idx * 2654435 + 12345) & 0xFFFF; jx += ((hs % 9) - 4) * aout * 3f; fall += (((hs / 9) % 9) - 4) * aout * 3f; }
                    Gfx.push(g); Gfx.translate(g, x + cw / 2f + jx, 4f + bob + fall); Gfx.scale(g, s);
                    if (eGlow) { int ga = Math.round(alpha * a * (0.18f + 0.14f * (float) Math.sin(waveT * 3.0 + idx * 0.3))); int gc = Gfx.argb(ga, col & 0xFFFFFF); for (int ox = -2; ox <= 2; ox += 2) for (int oy = -2; oy <= 2; oy += 2) if (ox != 0 || oy != 0) g.text(font, c, -cw / 2 + ox, -4 + oy, gc, false); }
                    if (glitching) {
                        int h = (idx * 7 + (int) (waveT * 40)) % 5 - 2;
                        g.text(font, c, -cw / 2 - 2 + h, -4, Gfx.argb((int) (alpha * a * 0.6f), 0x56D8FF), false);
                        g.text(font, c, -cw / 2 + 2 - h, -4 + 1, Gfx.argb((int) (alpha * a * 0.6f), 0xFF72C6), false);
                    }
                    outlined(g, font, c, -cw / 2 + (glitching && idx % 3 == 0 ? 1 : 0), -4, Gfx.argb((int) (alpha * a), col));
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

    private static MutableComponent lit(String s) { return Component.literal(s); }

    private static int rows(Anim a, FancyConfig c) {
        if (a.item2 != null) return 3 + (c.showMagicFind && (a.item.mf != null || a.item2.mf != null) ? 1 : 0);
        return rows(a.item, c);
    }

    private static int rows(Item it, FancyConfig c) {
        if (it == null) return 1;
        int n = 0;
        if (hasFacts(it, c)) n += 2;                                                   // the labelled columns: a small label line over the values
        if (c.showMagicFind && it.mf != null) n++;
        if (it.extra != null) n++;
        return n;
    }

    private static boolean hasFacts(Item it, FancyConfig c) {
        return (c.showOdds && (!it.odds.isEmpty() || !it.chance.isEmpty())) || (c.showPrice && !it.untradeable && it.price != -1);
    }
    /** Every coin, no K / M / B: 8010000 -> "8,010,000". */
    static String fullCoins(double v) { return String.format(java.util.Locale.ROOT, "%,d", Math.round(v)); }

    /** Odds, chance and value in their own labelled columns with dividers. They arrive one after another, 0.35 s apart, after the title. */
    private static void columns(GuiGraphicsExtractor g, Font font, int cx, int yLab, int yVal, float sc, Item it, FancyConfig c, int alpha) {
        java.util.List<Object[]> cols = new java.util.ArrayList<>();
        if (c.showOdds && !it.odds.isEmpty()) cols.add(new Object[]{"ODDS", it.odds, Ui.odds(it.odds), 0.35f});
        if (c.showOdds && !it.chance.isEmpty()) cols.add(new Object[]{"CHANCE", it.chance, Ui.SUB, 0.7f});
        if (c.showPrice && !it.untradeable && it.price != -1) {
            String lab = "VALUE" + (c.showPriceSource && it.price >= 0 && !it.source.isEmpty() ? "  ·  " + it.source.toUpperCase(java.util.Locale.ROOT) : "");
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

    /** One line of facts about a drop: how rare it is, then what it is worth. */
    private static void detail(GuiGraphicsExtractor g, Font font, Layout L, int y, Item it, FancyConfig c, int alpha) { detailAt(g, font, L.cx(), y, L.tsc() * c.subScale, it, c, alpha); }

    private static void detailAt(GuiGraphicsExtractor g, Font font, int cx, int y, float sc, Item it, FancyConfig c, int alpha) {
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
    /** For the animations: the number and our little Magic Find / Overbloom icon instead of the words. */
    static String mfAnim(String mf) {
        if (mf == null) return "";
        if (mf.startsWith("OB:")) return mf.substring(3).replaceFirst("^\\+", "") + " \ue031";
        return mf.replaceFirst("^\\+", "").replaceFirst("%$", "") + " \ue030";
    }
    /** "+250%" becomes "250 ✯ Magic Find". */
    static String mfText(String mf) {
        if (mf == null) return "";
        if (mf.startsWith("OB:")) return mf.substring(3).replaceFirst("^\\+", "") + " ✿ Overbloom";           // farming: Overbloom, not Magic Find
        return mf.replaceFirst("^\\+", "").replaceFirst("%$", "") + " ✯ Magic Find"; }

    /** The words live in the bottom black bar so they never cover the scene: title, facts and Magic Find. The top bar stays empty. */
    private static void barText(GuiGraphicsExtractor g, Font font, Anim a, FancyConfig c, int alpha, int W, int H, int bar) {
        float ts = c.textK() * uk(W, H) * textBoost, big = 1.5f * ts * c.titleScale, small = 1.1f * ts * c.subScale;
        String mf = a.item == null ? null : a.item.mf != null ? a.item.mf : a.item2 != null ? a.item2.mf : null;
        boolean hasMf = c.showMagicFind && mf != null;
        boolean facts = a.item != null && a.item2 == null && hasFacts(a.item, c);
        int total = Math.round(9 * big) + 4 + Math.round(9 * small * (facts ? 1.95f : 1f)) + (hasMf ? 3 + Math.round(9 * small) : 0);
        int ty = c.textPos == 0 ? Math.max(2, (bar - total) / 2) : Math.min(H - total - 2, H - bar + Math.max(2, (bar - total) / 2)), dy = ty + Math.round(9 * big) + 4;      // in the top or the bottom bar
        if (a.item == null) {
            line(g, font, W / 2, ty, big, alpha, lit("Nothing :(").withStyle(ChatFormatting.BOLD), 0xFFB9BFC9);
            line(g, font, W / 2, dy, small, alpha, lit("Better Luck Next Time"), 0xFF7C8494);
            return;
        }
        if (a.item2 != null) {
            float tb = big * 0.82f;
            line(g, font, Math.round(W * 0.27f), ty, tb, alpha, lit(a.item.title()).withStyle(ChatFormatting.BOLD), a.item.color());
            detailAt(g, font, Math.round(W * 0.27f), dy, small * 0.95f, a.item, c, alpha);
            line(g, font, Math.round(W * 0.73f), ty, tb, alpha, lit(a.item2.title()).withStyle(ChatFormatting.BOLD), a.item2.color());
            detailAt(g, font, Math.round(W * 0.73f), dy, small * 0.95f, a.item2, c, alpha);
        } else {
            line(g, font, W / 2, ty, big, alpha, lit(a.item.title()).withStyle(ChatFormatting.BOLD), a.item.color());
            if (facts) columns(g, font, W / 2, dy, dy + Math.round(9 * small * 0.9f), small, a.item, c, alpha);
            else detailAt(g, font, W / 2, dy, small, a.item, c, alpha);
        }
        if (hasMf) { lineDelay = 4f * c.subDelay; line(g, font, W / 2, dy + Math.round(9 * small * (facts ? 1.95f : 1f)) + 3, small, alpha, lit(mfText(mf)), mfColor(mf)); lineDelay = 0f; }
    }

    /** Two drops from one kill: both names and their facts, stacked. */
    private static void twinText(GuiGraphicsExtractor g, Font font, Layout L, Item a, Item b, FancyConfig c, int alpha) {
        line(g, font, L.cx(), L.nameY(), L.tsc(), alpha, lit(a.title()).withStyle(ChatFormatting.BOLD), a.color());
        detail(g, font, L, L.lineY(0), a, c, alpha);
        line(g, font, L.cx(), L.lineY(1), L.tsc(), alpha, lit(b.title()).withStyle(ChatFormatting.BOLD), b.color());
        detail(g, font, L, L.lineY(2), b, c, alpha);
        String mf = a.mf != null ? a.mf : b.mf;
        if (c.showMagicFind && mf != null) line(g, font, L.cx(), L.lineY(3), L.tsc(), alpha, lit(mfText(mf)), mfColor(mf));
    }

    /** Farming: a clean stack under the scene: the name, how rare, what it is worth, Overbloom. One line each, easy to read. */
    private static int farmRows(Item it, FancyConfig c) {
        if (it == null) return 1;
        int n = 0;
        if (c.showOdds && (!it.odds.isEmpty() || !it.chance.isEmpty())) n++;
        if (c.showPrice && !it.untradeable && it.price != -1) n++;
        if (it.extra != null) n++;
        if (c.showMagicFind && it.mf != null) n++;
        return n;
    }

    private static void farmText(GuiGraphicsExtractor g, Font font, Layout L, Item it, FancyConfig c, int alpha) {
        if (alpha <= 4) return;
        if (it == null) { text(g, font, L, null, c, alpha); return; }
        float tt = L.tsc() * 1.25f * c.titleScale, st = L.tsc() * 1.05f * c.subScale;
        line(g, font, L.cx(), L.nameY(), tt, alpha, lit(it.title()).withStyle(ChatFormatting.BOLD), it.color());
        int k = 0;
        if (c.showOdds && (!it.odds.isEmpty() || !it.chance.isEmpty())) {
            java.util.List<Object> parts = new java.util.ArrayList<>();
            if (!it.odds.isEmpty()) { parts.add(lit(it.odds)); parts.add(Ui.odds(it.odds)); }
            if (!it.chance.isEmpty()) { parts.add(lit((it.odds.isEmpty() ? "" : "   \u00B7   ") + it.chance)); parts.add(Ui.SUB); }
            lineDelay = (k + 1) * c.subDelay; line(g, font, L.cx(), L.lineY(k++), st, alpha, parts.toArray());
        }
        if (c.showPrice && !it.untradeable && it.price != -1) {
            String v = it.price < 0 ? "..." : fullCoins(it.price * Math.max(1, it.amount)) + " coins";
            java.util.List<Object> parts = new java.util.ArrayList<>(); parts.add(lit(v)); parts.add(Ui.PRICE);
            if (c.showPriceSource && it.price >= 0 && !it.source.isEmpty()) { parts.add(lit("   \u00B7   " + it.source)); parts.add(Ui.DIM); }
            lineDelay = (k + 1) * c.subDelay; line(g, font, L.cx(), L.lineY(k++), st, alpha, parts.toArray());
        }
        if (it.extra != null) { lineDelay = (k + 1) * c.subDelay; line(g, font, L.cx(), L.lineY(k++), st, alpha, lit(it.extra), it.extraCol); }
        if (c.showMagicFind && it.mf != null) { lineDelay = (k + 1) * c.subDelay; line(g, font, L.cx(), L.lineY(k), st, alpha, lit(mfText(it.mf)), mfColor(it.mf)); }
        lineDelay = 0f;
    }

    private static void text(GuiGraphicsExtractor g, Font font, Layout L, Item it, FancyConfig c, int alpha) {
        if (alpha <= 4) return;
        if (it == null) {
            line(g, font, L.cx(), L.nameY(), L.tsc(), alpha, lit("Nothing :(").withStyle(ChatFormatting.BOLD), 0xFFB9BFC9);
            line(g, font, L.cx(), L.lineY(0), L.tsc(), alpha, lit("Better Luck Next Time"), 0xFF7C8494);
            return;
        }
        float tt = L.tsc() * c.titleScale, st = L.tsc() * c.subScale;
        line(g, font, L.cx(), L.nameY(), tt, alpha, lit(it.title()).withStyle(ChatFormatting.BOLD), it.color());
        int k = 0;
        if (hasFacts(it, c)) { columns(g, font, L.cx(), L.lineY(0), L.lineY(0) + Math.round(L.lineH() * 0.8f), st, it, c, alpha); k = 2; }
        lineDelay = 4f * c.subDelay;
        if (it.extra != null) line(g, font, L.cx(), L.lineY(k++), st, alpha, lit(it.extra), it.extraCol);
        if (c.showMagicFind && it.mf != null) line(g, font, L.cx(), L.lineY(k), st, alpha, lit(mfText(it.mf)), mfColor(it.mf));
        lineDelay = 0f;
    }

    // ------------------------------------------------------------------ HUD editor preview

    private static int previewFrame = -1, previewStyle = -1;
    private static long previewT0;
    private static final Item PREVIEW = new Item();
    static { PREVIEW.name = "Example Drop"; PREVIEW.odds = "Pray RNGesus"; PREVIEW.chance = "1 in 735"; PREVIEW.price = 1_230_000; PREVIEW.source = "Bazaar"; PREVIEW.mf = "+250%"; PREVIEW.rank = 2; }

    private static final Item PREVIEW_EXP = new Item();       // Experiments: no Magic Find, Overbloom instead
    static { PREVIEW_EXP.name = "Example Drop"; PREVIEW_EXP.odds = ""; PREVIEW_EXP.chance = "1 in 89"; PREVIEW_EXP.price = 1_230_000; PREVIEW_EXP.source = "Bazaar"; PREVIEW_EXP.rank = 2; PREVIEW_EXP.mf = null; }
    private static final Item PREVIEW_FARM = new Item();      // farming has no Magic Find, so its preview has none either
    static { PREVIEW_FARM.name = "Example Crop"; PREVIEW_FARM.odds = ""; PREVIEW_FARM.chance = "1 in 18,000"; PREVIEW_FARM.price = 1_230_000; PREVIEW_FARM.source = "Bazaar"; PREVIEW_FARM.rank = 1; PREVIEW_FARM.mf = "OB:+25"; }

    private static final Anim PREVIEW_ANIM = new Anim(SlayerData.OTHER, PREVIEW, 1, null);
    private static final Anim PREVIEW_ANIM_EXP = new Anim(SlayerData.OTHER, PREVIEW_EXP, 1, null);      // no Magic Find in the Experiments bar text
    private static final Anim PREVIEW_ANIM_FARM = new Anim(SlayerData.OTHER, PREVIEW_FARM, 1, null);

    private static final Scene.Params[] PREVIEW_P = new Scene.Params[5];
    private static int previewCat = -1;
    private static String previewKeyLast = "";

    /** The name of the position the preview shows for a Drop Library category (0 Slayers, 1 Dye, 2 Farming, 3 Mythological). */
    public static String previewPosition(int cat, FancyConfig c) { return positionName(c, cat); }

    private static Scene.Params previewFor(int cat, FancyConfig c) {
        Scene.Params p = PREVIEW_P[cat];
        if (p == null) {
            int[] gem = ItemIcons.gem(0xFFB300);
            p = new Scene.Params(cat == 1 ? 2 : 1, true);
            p.rarity = cat == 1 ? 0xFFE0409A : Ui.R_CRAZY;
            p.item = new Scene.Model(PuffyItem.of(gem, 16, 16, true), gem);
            if (cat == 2) { p.farm = true; p.chops = 3; p.rarity = 0xFF7BE05A; }
            if (cat == 4) { p.exp = true; p.chops = 4; p.rarity = 0xFFB56BFF; }
            if (cat == 3) { p.myth = true; p.chops = 3; p.rarity = 0xFFFFC857; }          // (the live chop count is set below)
            PREVIEW_P[cat] = p;
        }
        if (cat == 0) { p.chops = Math.max(1, c.chopsCool); p.style = c.slayerStyle; }
        if (cat == 1) { p.dye = true; p.chops = 1; p.dyeCol = 0xFFE0409A; }
        if (cat == 3) p.chops = Math.max(1, c.mythStrikes);
        if (cat == 4) p.chops = Math.max(1, c.expFlips);
        return p;
    }

    /**
     * A looping example of the chosen drop type, drawn small inside the Settings and Animations pages (and full size while a size slider
     * is dragged). It plays the whole scene, in the layout the real one uses: cat 0 = Slayers, 1 = Dye (both with bars if they are on), 2 = Farming (never bars).
     */
    public static void drawPreview(GuiGraphicsExtractor g, int W, int H) { drawPreview(g, W, H, 0); }

    public static void drawPreview(GuiGraphicsExtractor g, int W, int H, int cat) { drawPreview(g, W, H, cat, null); }

    private static final java.util.Map<String, Scene.Params> KEY_P = new java.util.HashMap<>();

    /** The preview scene of one animation type (nothing, default, rare, rng, dye, farm, helios, myth, mythrng), built from the live settings every frame. */
    private static Scene.Params previewForKey(String key, FancyConfig c) {
        int cat = switch (key) { case "dye" -> 1; case "farm", "helios" -> 2; case "myth", "mythrng", "mythnothing" -> 3; case "exp", "exprng" -> 4; default -> 0; };
        Scene.Params base = previewFor(cat, c);
        int tier = switch (key) { case "rng", "dye", "helios", "mythrng", "exprng" -> 2; case "rare" -> 1; default -> 0; };
        Scene.Params p = KEY_P.get(key);
        if (p == null) {
            p = new Scene.Params(tier, !key.endsWith("nothing"));
            if (p.hasItem) { p.item = base.item; p.rarity = key.equals("default") ? Ui.R_CRAZY : base.rarity; }
            switch (key) {
                case "dye" -> { p.dye = true; p.dyeCol = 0xFFE0409A; }
                case "farm", "helios" -> { p.farm = true; p.helios = key.equals("helios"); p.rarity = 0xFF7BE05A; }
                case "myth", "mythrng", "mythnothing" -> { p.myth = true; p.rarity = 0xFFFFC857; }
                case "exp", "exprng" -> { p.exp = true; p.rarity = key.equals("exprng") ? 0xFFFF55FF : 0xFFB56BFF; }
                default -> { }
            }
            KEY_P.put(key, p);
        }
        c.fixChops();
        switch (key) {
            case "dye" -> p.chops = 1;
            case "farm", "helios" -> p.chops = 3;
            case "myth", "mythnothing" -> p.chops = Math.max(1, Math.min(10, c.mythStrikes));
            case "mythrng" -> p.chops = Math.max(1, Math.min(10, c.mythStrikesRng));
            case "exp" -> p.chops = Math.max(1, Math.min(12, c.expFlips));
            case "exprng" -> p.chops = Math.max(1, Math.min(12, c.expFlipsRng));
            default -> { p.chops = Math.max(1, Math.min(20, tier == 2 ? c.chopsEpic : tier == 1 ? c.chopsCool : c.chopsDefault)); p.style = c.slayerStyle; }
        }
        return p;
    }

    /** When each sound of one animation type fires, in real seconds: {total, chop count, chop times..., split, reveal, end}. Drives the sound waves in the menu. */
    /** When the linger sound starts: shortly after the reveal, but never later than the middle of what is left. */
    static float lingerAt(float ts, float len) { float a = ts + 0.7f, end = len - 0.9f; return Math.min(a, ts + 0.15f + Math.max(0.2f, (end - ts - 0.15f) * 0.45f)); }

    public static float[] soundTimes(String key, FancyConfig c) {
        Scene.Params p = previewForKey(key, c);
        float natural = naturalScene(p, p.farm);
        float secs = timing(p, p.farm, key, c)[2], k = secs / Math.max(0.1f, natural);
        int n = Math.max(0, Math.min(p.chops, 20));
        float[] r = new float[n + 6];
        r[0] = secs; r[1] = n;
        for (int i = 0; i < n; i++) r[2 + i] = Scene.impact(p, i) * k;
        float ts = Scene.splitTime(p);
        r[2 + n] = ts * k; r[3 + n] = (ts + 0.15f) * k; r[4 + n] = lingerAt(ts, natural) * k; r[5 + n] = Math.max(0f, natural - 0.9f) * k;
        return r;
    }

    /** >= 0: the preview shows this moment (real seconds) instead of looping on its own; the Sounds tab uses it to keep the scene and the sound waves in step. */
    public static float syncSec = -1f;

    /** Same as above, but for one animation type, at the length the user chose for it, so every edit shows at once. */
    public static void drawPreview(GuiGraphicsExtractor g, int W, int H, int cat, String key) {
        FancyConfig c = FancyConfig.get();
        cat = Math.max(0, Math.min(4, cat));
        if (c.slayerStyle != previewStyle) {                                                   // Sword <-> Spirit: build the scenes again and play the new one from the start
            previewStyle = c.slayerStyle; KEY_P.clear(); java.util.Arrays.fill(PREVIEW_P, null); previewFrame = -1; previewT0 = System.currentTimeMillis();
        }
        Scene.Params p = key == null ? previewFor(cat, c) : previewForKey(key, c);
        if (itemOnly) {                                                                         // just the item, big, in the middle
            float isec = ((System.currentTimeMillis() - textT0) % 4600L) / 1000f;
            Scene.drawItemOnly(OUT, L3, p, isec); SceneTexture.upload(OUT);
            float cell = H / 72f; int zw = Math.round(Scene.W * cell), zh = Math.round(Scene.H * cell);
            int dx = W / 2 - Math.round(56 * cell), dy = H / 2 - Math.round(32 * cell);
            if (!SceneTexture.draw(g, OUT, dx, dy, zw, zh, 1f)) OUT.flush(g::fill, dx, dy, Math.max(1, Math.round(cell)), 1f);
            return;
        }
        float[] tm = key == null ? null : timing(p, p.farm, key, c);
        float speed = tm == null ? 1f : tm[0];
        if (cat == 3) p.mob = (int) ((System.currentTimeMillis() / 1000L / 9L) % 12L);      // the preview calls a different creature each loop
        String kk = key == null ? "" : key;
        if (previewCat != cat || !kk.equals(previewKeyLast)) { previewCat = cat; previewKeyLast = kk; previewFrame = -1; }
        boolean farm = cat == 2, cine = c.cine(cat) && !farm;
        Font font = Minecraft.getInstance().font;
        float sec = syncSec >= 0f ? syncSec * speed : ((System.currentTimeMillis() - previewT0) % 600_000L) / 1000f * speed;
        float ts = Scene.splitTime(p), len = tm == null ? Scene.length(p) + 0.4f : naturalScene(p, p.farm), t = syncSec >= 0f ? Math.min(sec, len - 0.02f) : sec % len;
        float rv0 = tm == null ? ts + 0.15f : tm[1];
        if (freezeT >= 0f) { t = Math.min(len - 1.0f, rv0 + freezeT); sec = t; }                 // a still frame: the words are in, nothing moves
        else if (loopTxt) { t = Math.min(len - 1.0f, rv0 - 0.3f + ((System.currentTimeMillis() - textT0) % 4600L) / 1000f); sec = t; }       // the words play again and again
        int frame = (int) (t * 30f);
        if (frame != previewFrame) { previewFrame = frame; Scene.draw(OUT, L3, p, frame / 30f); if (cine) feather(OUT); SceneTexture.upload(OUT); }
        float rv = tm == null ? ts + 0.15f : tm[1];
        revealT = t - rv; waveIn = clamp((t - rv) / 0.6f); waveOut = clamp((t - (len - 0.8f)) / 0.6f); waveT = sec; bounceT = t - (rv + 0.6f);
        float ga = 1f - waveOut * 0f;
        int barFull = cine ? Math.round(H * c.barHeight / 100f) : 0;
        Item pi = cat == 4 ? PREVIEW_EXP : farm ? PREVIEW_FARM : PREVIEW;
        Layout L = layout(W, H, c, cat == 2 ? farmRows(pi, c) : rows(pi, c), cat);
        if (textOnly) {                                                                          // the Text Changes editor: just the words, big and centred, on nothing
            float lt = ((System.currentTimeMillis() - textT0) % 4600L) / 1000f;                  // the words come in, stay, and leave, again and again
            revealT = lt - 0.2f; waveIn = clamp(revealT / 0.6f); waveOut = clamp((lt - 3.5f) / 0.9f); waveT = lt;
            textBoost = 2.6f; try { barText(g, font, PREVIEW_ANIM, c, 255, W, H, H); } finally { textBoost = 1f; }
            return;
        }
        if (cine) {
            int side = Math.round(barFull * SIDE_K);
            bars(g, W, H, barFull, side, p.rarity | 0xFF000000, p.rarity | 0xFF000000);
            g.enableScissor(side, barFull, W - side, H - barFull);
            if (!textOnly && !SceneTexture.draw(g, OUT, L.ox(), L.oy(), L.sw(), L.sh(), 1f)) OUT.flush(g::fill, L.ox(), L.oy(), Math.max(1, Math.round(L.cell())), 1f);
            g.disableScissor();
            barText(g, font, cat == 4 ? PREVIEW_ANIM_EXP : farm ? PREVIEW_ANIM_FARM : PREVIEW_ANIM, c, 255, W, H, barFull);
            return;
        }
        if (!textOnly && !SceneTexture.draw(g, OUT, L.ox(), L.oy(), L.sw(), L.sh(), 1f)) OUT.flush(g::fill, L.ox(), L.oy(), Math.max(1, Math.round(L.cell())), 1f);
        if (cat == 2) farmText(g, font, L, pi, c, 255); else text(g, font, L, pi, c, 255);
    }

    /** Fades the scene's edges to see-through so it melts into the blurred world instead of sitting there as a hard box. */
    private static void feather(Canvas cv) {
        int fx = 18, fy = 16;
        for (int y = 0; y < cv.h; y++) {
            float ey = clamp(Math.min(y, cv.h - 1 - y) / (float) fy);
            for (int x = 0; x < cv.w; x++) {
                float f = Math.min(clamp(Math.min(x, cv.w - 1 - x) / (float) fx), ey);
                if (f >= 1f) continue;
                f = f * f * (3 - 2 * f);
                int i = y * cv.w + x, p = cv.px[i], al = p >>> 24;
                if (al != 0) cv.px[i] = ((int) (al * f) << 24) | (p & 0xFFFFFF);
            }
        }
    }

    /** The pillar bars are a little thinner than the top and bottom ones. */
    private static final float SIDE_K = 0.8f;

    /** Bar colour presets (Settings > Animations > Bar colour). Auto takes a deep shade of the drop's own colour. */
    public static final String[] BAR_NAMES = {"Auto (drop colour)", "Midnight", "Wine", "Forest", "Slate", "Black"};
    private static final int[] BAR_COLS = {0, 0x0B0E2A, 0x2A0B1C, 0x0B2418, 0x1A202C, 0x000000};

    private static int barFill(int tint, int style) {
        FancyConfig fc = FancyConfig.get();
        if (fc.barCustom) return 0xE6000000 | (fc.barColor & 0xFFFFFF);
        int i = Math.floorMod(style, BAR_NAMES.length);
        int rgb = i == 0 ? Canvas.mix(0xFF07070E, 0xFF000000 | (tint & 0xFFFFFF), 0.30f) & 0xFFFFFF : BAR_COLS[i];
        return 0xE6000000 | rgb;                                                   // a touch see-through so the blurred world glows behind it
    }

    private static void bars(GuiGraphicsExtractor g, int W, int H, int bar, int side, int edge) { bars(g, W, H, bar, side, edge, Ui.ACCENT); }

    /** The four bars: one clean tinted colour (not black), with no edge line. */
    private static void bars(GuiGraphicsExtractor g, int W, int H, int bar, int side, int edge, int tint) {
        if (bar <= 0) return;
        int f = barFill(tint, FancyConfig.get().barStyle);
        g.fill(0, 0, W, bar, f); g.fill(0, H - bar, W, H, f);
        g.fill(0, bar, side, H - bar, f); g.fill(W - side, bar, W, H - bar, f);
    }

    // ------------------------------------------------------------------ the animation

    /** The words stay this many real seconds after the scene is over. */
    private static final float TEXT_EXTRA = 2f;

    public static void render(GuiGraphicsExtractor g) {
        FancyConfig c = FancyConfig.get();
        if (cur == null) {
            Anim next = QUEUE.peek();
            if (next == null) return;
            if (next.waiting()) return;
            cur = QUEUE.poll();
            Anim a = cur;
            if (a.onStart != null) a.onStart.run();
            DropListener.announceNow(a.item); DropListener.announceNow(a.item2);          // the chat message goes out together with the scene
            a.tier = a.forcedTier >= 0 ? a.forcedTier : Math.max(tierOf(a.item), tierOf(a.item2));
            a.p = params(a, a.item, a.tier);
            float[] tm = timing(a.p, a.farm, typeKey(a), c);
            a.speed = tm[0]; a.textScene = tm[1];
            a.start = System.currentTimeMillis();
        }
        Anim a = cur;
        float t = (System.currentTimeMillis() - a.start) / 1000f * a.speed;                // a.speed stretches or squeezes the whole scene to the length the user chose
        float sLen = naturalScene(a.p, a.farm);     // the scene (picture, bars, shake) ends here
        float len = sLen + TEXT_EXTRA * a.speed;    // the words stay on screen 2 real seconds longer than the scene
        if (t >= len) { cur = null; SoundFx.stopPreviews(); return; }          // a sound longer than the animation ends with it
        if (a.p.item == null && a.item != null) a.p.item = ItemIcons.model(a.item.id, a.slayer.color());   // art finished loading late
        if (a.p.twin && a.p.item2 == null && a.item2 != null) a.p.item2 = ItemIcons.model(a.item2.id, a.slayer.color());

        float ga = Math.min(clamp(t / 0.2f), clamp((sLen - t) / 0.5f)), gt = Math.min(clamp(t / 0.2f), clamp((len - t) / 0.5f));      // scene fade, words fade
        boolean cine = c.cine(catOf(a)) && !a.farm;                              // bars are for Slayer and Dye scenes only
        float cin = cine ? easeIO(Math.min(clamp(t / 0.6f), clamp((sLen - t) / 0.6f))) : 0f;
        Font font = Minecraft.getInstance().font;
        Layout L = layout(g.guiWidth(), g.guiHeight(), c, catOf(a) == 2 ? farmRows(a.item, c) : rows(a, c), catOf(a));
        float ts = Scene.splitTime(a.p), reveal = a.textScene >= 0f ? a.textScene : ts + 0.27f;
        boolean epic = a.tier == 2;
        int W = g.guiWidth(), H = g.guiHeight(), barFull = Math.round(H * c.barHeight / 100f);
        boolean framed = cine;       // the scene fills the picture between the bars; the words sit in the bars
        float cellF = L.cell(); int sceneX = L.ox(), sceneY = L.oy();
        int sideFull = Math.round(barFull * SIDE_K);
        float scx = sceneX + Scene.W * cellF / 2f, scy = sceneY + Scene.H * cellF * 0.46f;
        if (cin > 0.02f) cinematic(g, cin, a, t, ts, scx, scy);

        if (epic && t > ts) g.fill(0, 0, W, H, Gfx.argb((int) (26 * clamp((t - ts) / 0.3f) * ga), 0x000010));

        if (t < sLen) {                                      // after the scene ends only the words remain
        int frame = (int) (t * 30f);                         // the scene is redrawn 30 times a second, not every frame
        if (frame != a.frame) { a.frame = frame; Scene.draw(OUT, L3, a.p, frame / 30f); if (cine) feather(OUT); a.sx = a.p.shakeX; a.sy = a.p.shakeY; SceneTexture.upload(OUT); }
        float since = t - ts, kick = since >= 0f && since < 0.45f ? (float) Math.sin(Math.PI * since / 0.45f) * (a.tier == 2 ? 0.07f : 0.045f) : 0f;   // the picture punches in at the reveal
        float zoom = (cine ? 1f + 0.05f * easeIO(clamp(t / sLen)) : 1f) + (cine ? kick : 0f);
        int zw = Math.round(Scene.W * cellF * zoom), zh = Math.round(Scene.H * cellF * zoom);
        int dx = sceneX + Math.round(a.sx * cellF) - (zw - Math.round(Scene.W * cellF)) / 2, dy = sceneY + Math.round(a.sy * cellF) - (zh - Math.round(Scene.H * cellF)) / 2;
        if (framed) g.enableScissor(sideFull, barFull, W - sideFull, H - barFull);          // a zoomed scene never spills into the bars
        if (!SceneTexture.draw(g, OUT, dx, dy, zw, zh, ga)) OUT.flush(g::fill, sceneX + Math.round(a.sx * cellF), sceneY + Math.round(a.sy * cellF), Math.max(1, Math.round(cellF)), ga);
        if (framed) g.disableScissor();
        }
        revealT = t - reveal; waveIn = clamp((t - reveal) / 0.6f); waveOut = clamp((t - (len - 1.0f)) / 0.7f); waveT = t; bounceT = t - (reveal + 0.6f);
        if (epic && t > ts) epicFront(g, a, t - ts, W, H, scx, scy, cin * ga);
        if (waveIn > 0f) {
            int al = (int) (255 * gt);
            if (framed) barText(g, font, a, c, al, W, H, barFull);
            else if (a.item2 != null) twinText(g, font, L, a.item, a.item2, c, al);
            else if (catOf(a) == 2) farmText(g, font, L, a.item, c, al);
            else text(g, font, L, a.item, c, al);
        }

        sounds(a, t, ts, sLen);
    }

    private static float easeIO(float x) { x = clamp(x); return x * x * (3 - 2 * x); }

    /** The cinematic frame: blur, a tint in the slayer's colour, black bars that kick on every slash, a flash and a light streak on the reveal, and a vignette. */
    private static void cinematic(GuiGraphicsExtractor g, float cin, Anim a, float t, float ts, float scx, float scy) {
        FancyConfig c = FancyConfig.get();
        int W = g.guiWidth(), H = g.guiHeight();
        if (c.blur(catOf(a)) && Minecraft.getInstance().gui.screen() == null) {      // a screen (pause menu...) already blurs the frame; never do it twice
            try {                                                              // the blur eases in and out with the bars instead of snapping off
                int real = Minecraft.getInstance().options.getMenuBackgroundBlurriness();
                int want = Math.round(real * cin);
                if (want > 0) {
                    try { Minecraft.getInstance().gameRenderer.gameRenderState().optionsRenderState.menuBackgroundBlurriness = want; } catch (Throwable ignored) {}
                    g.blurBeforeThisStratum(); g.nextStratum();
                }
            } catch (RuntimeException ignored) {}
        }
        int tint = a.slayer.color() & 0xFFFFFF;
        g.fill(0, 0, W, H, Gfx.argb((int) (40 * cin), Canvas.mix(0xFF02030A, 0xFF000000 | tint, 0.22f)));

        float punch = 0f;                                                     // how hard the last slash just hit (0..1)
        for (int k = 0; k < a.p.chops && k < 20; k++) {
            float d = t - Scene.impact(a.p, k);
            if (d >= 0f && d < 0.22f) punch = Math.max(punch, (1f - d / 0.22f) * (k == a.p.chops - 1 ? 1f : 0.45f));
        }
        if (t < ts && a.tier >= 1 && !a.p.dye && !a.p.myth && !a.p.exp) {                              // before the final slice the world dims and holds its breath
            float pre = clamp(1f - (ts - t) / (a.tier == 2 ? 1.1f : 0.8f));
            g.fill(0, 0, W, H, Gfx.argb((int) ((a.tier == 2 ? 70 : 42) * pre * pre * cin), 0x05020E));
        }
        float since = t - ts;                                                 // the reveal
        float flash = since >= 0f && since < 0.4f ? 1f - since / 0.4f : 0f;
        if (flash > 0f) { g.fill(0, 0, W, H, Gfx.argb((int) (120 * flash * (a.tier == 2 ? 1f : 0.6f)), 0xFFF2C0)); punch = Math.max(punch, flash); }
        if (punch > 0f) g.fill(0, 0, W, H, Gfx.argb((int) (34 * punch), 0xFFFFFF));

        // ---- the show: screen-wide effects behind the scene (the scene itself is drawn after this)
        int rc = a.p.rarity & 0xFFFFFF;
        float sx0 = scx, sy0 = scy;
        for (int k = 0; k < a.p.chops && k < 20; k++) {                      // every slash tears a bright line across the whole frame
            float d = t - Scene.impact(a.p, k);
            if (d < 0f || d > 0.2f) continue;
            float f = 1f - d / 0.2f, ang = (float) ((Tex.h(k, a.p.cutSeed) - 0.5f) * 1.7f + (k % 2 == 0 ? 0.55f : -0.55f));
            Gfx.push(g); Gfx.translate(g, sx0, sy0); Gfx.rotate(g, ang);
            g.fill(-W, -3, W, 3, Gfx.argb((int) (46 * f * cin), rc));
            g.fill(-W, -1, W, 1, Gfx.argb((int) (230 * f * cin), 0xFFFFFF));
            Gfx.pop(g);
        }
        if (since >= 0f) {
            float fade = cin * clamp((a.item == null ? 0.5f : 1f) * (1f - clamp((since - 1.8f) / 1.4f)));
            if (a.tier >= 1 && fade > 0.01f) {                                // slowly turning god rays in the drop's colour
                int n = a.tier == 2 ? 14 : 9;
                for (int i = 0; i < n; i++) {
                    float ang = (float) (i * Math.PI * 2 / n + since * 0.22f), wd = 5f + 5f * Tex.h(i, 3);
                    Gfx.push(g); Gfx.translate(g, sx0, sy0); Gfx.rotate(g, ang);
                    g.fill(8, (int) -wd, (int) (W * 0.9f), (int) wd, Gfx.argb((int) (20 * fade), rc));
                    g.fill(8, (int) (-wd * 0.4f), (int) (W * 0.9f), (int) (wd * 0.4f), Gfx.argb((int) (28 * fade), 0xFFFFFF));
                    Gfx.pop(g);
                }
            }
            if (since < 0.7f) {                                               // a burst of rays shooting out of the cut
                float f = since / 0.7f, r0 = W * 0.04f + W * 0.30f * easeIO(f), r1 = r0 + W * 0.14f * (1f - f) + 6f;
                for (int i = 0; i < 28; i++) {
                    float ang = (float) (i * Math.PI * 2 / 28 + Tex.h(i, 9) * 0.2f);
                    Gfx.push(g); Gfx.translate(g, sx0, sy0); Gfx.rotate(g, ang);
                    g.fill((int) r0, -1, (int) r1, 1, Gfx.argb((int) (190 * (1f - f) * cin), i % 3 == 0 ? 0xFFFFFF : rc));
                    Gfx.pop(g);
                }
            }
            int sparks = (a.tier == 2 ? 60 : a.tier == 1 ? 40 : 22) / (a.p.style == 1 ? 3 : 1);
            for (int i = 0; i < sparks; i++) {                                // sparks drift up through the frame
                float life = since - Tex.h(i, 21) * 0.8f;
                if (life < 0f || life > 2.2f) continue;
                float u = life / 2.2f, px = sx0 + (Tex.h(i, 22) - 0.5f) * W * 0.55f + (float) Math.sin(life * 3 + i) * 6f, py = sy0 + H * 0.12f - u * H * 0.5f * (0.6f + Tex.h(i, 23));
                int sz = Tex.h(i, 24) > 0.8f ? 3 : 2;
                g.fill((int) px, (int) py, (int) px + sz, (int) py + sz, Gfx.argb((int) (230 * Math.sin(u * Math.PI) * cin), i % 4 == 0 ? 0xFFFFFF : rc));
            }
            if (since < 1.0f) {                                               // a shockwave ring rolls out from the drop
                float f = since / 1.0f, rad = W * 0.04f + W * 0.42f * (1f - (1f - f) * (1f - f));
                for (int i = 0; i < 72; i++) {
                    float ang = (float) (i * Math.PI * 2 / 72);
                    int px = Math.round(sx0 + (float) Math.cos(ang) * rad), py = Math.round(sy0 + (float) Math.sin(ang) * rad * 0.55f);
                    g.fill(px - 1, py - 1, px + 1, py + 1, Gfx.argb((int) (170 * (1f - f) * cin), i % 2 == 0 ? 0xFFFFFF : rc));
                }
            }
        }

        if (a.tier == 2 && since >= 0f) epicBack(g, a, since, W, H, sx0, sy0, cin);
        int bar = Math.round(H * (c.barHeight / 100f) * cin + H * 0.012f * punch * cin);   // the bars flinch on every hit
        float rim = a.tier == 2 && since >= 0f ? 0.5f + 0.5f * (float) Math.sin(since * 7f) : 0f;      // RNG Drop: the bar edges pulse
        bars(g, W, H, bar, Math.round(bar * SIDE_K), Gfx.argb((int) (110 * cin + 120 * punch + 70 * rim * cin), a.p.rarity & 0xFFFFFF), tint);
        if (since >= 0f && since < 0.9f) {                                    // an anamorphic light streak across the middle of the scene
            float f = since / 0.9f, half = W * 0.5f * easeIO(Math.min(1f, f * 2.2f)), alpha = (1f - f) * cin;
            int yc = Math.round(scy);
            int col = a.p.rarity & 0xFFFFFF;
            g.fill(Math.round(W / 2f - half), yc - 1, Math.round(W / 2f + half), yc + 1, Gfx.argb((int) (200 * alpha), 0xFFFFFF));
            g.fill(Math.round(W / 2f - half), yc - 3, Math.round(W / 2f + half), yc + 3, Gfx.argb((int) (60 * alpha), col));
        }
        for (int i = 0; i < 7; i++) {                                         // vignette: the corners fall into shadow
            int s = (int) (W * 0.016f * (7 - i)) + 1;
            int al = (int) (cin * 9);
            g.fill(0, 0, s, H, Gfx.argb(al, 0x000000)); g.fill(W - s, 0, W, H, Gfx.argb(al, 0x000000));
        }
    }

    private static final int[] CONFETTI = {0xFFE9A0, 0xFFFFFF, 0xFF72C6, 0x56D8FF, 0xFFC857, 0xB56BFF};

    /** RNG Drop, behind the scene: a pulsing bloom, a prism sweep and two extra golden shockwaves. */
    private static void epicBack(GuiGraphicsExtractor g, Anim a, float since, int W, int H, float cx, float cy, float cin) {
        if (cin <= 0.02f) return;
        int rc = a.p.rarity & 0xFFFFFF;
        float fade = clamp(1f - (since - 2.6f) / 1.2f) * cin;
        float pulse = 0.65f + 0.35f * (float) Math.sin(since * 5.5f);
        for (int layer = 0; layer < 5; layer++) {                         // a soft bloom: stacked ellipses, bigger and fainter outwards
            float rx = W * (0.10f + layer * 0.075f) * (0.85f + 0.15f * pulse), ry = rx * 0.55f;
            int al = (int) ((14 - layer * 2) * fade * clamp(since / 0.25f)) + 1;
            for (int dy = (int) -ry; dy <= ry; dy += 2) {
                float w = rx * (float) Math.sqrt(1f - (dy * dy) / (ry * ry));
                g.fill(Math.round(cx - w), Math.round(cy + dy), Math.round(cx + w), Math.round(cy + dy) + 2, Gfx.argb(al, layer < 2 ? 0xFFFFFF : rc));
            }
        }
        for (int k = 0; k < 2; k++) {                                     // two more shockwave rings, thicker, gold and rarity
            float f = (since - 0.14f - k * 0.17f) / 1.1f;
            if (f < 0f || f > 1f) continue;
            float rad = W * 0.03f + W * 0.5f * (1f - (1f - f) * (1f - f));
            for (int i = 0; i < 96; i++) {
                float ang = (float) (i * Math.PI * 2 / 96);
                int px = Math.round(cx + (float) Math.cos(ang) * rad), py = Math.round(cy + (float) Math.sin(ang) * rad * 0.55f);
                g.fill(px - 2, py - 2, px + 2, py + 2, Gfx.argb((int) (150 * (1f - f) * cin), k == 0 ? 0xFFD36B : rc));
            }
        }
        if (since > 0.05f && since < 1.1f) {                              // a prism: a band of rainbow sweeps across, tilted
            float f = (since - 0.05f) / 1.05f, bx = -W * 0.2f + f * W * 1.4f;
            for (int i = 0; i < 7; i++) {
                int col = java.awt.Color.HSBtoRGB(i / 7f, 0.55f, 1f) & 0xFFFFFF;
                Gfx.push(g); Gfx.translate(g, bx + i * 9f, H * 0.5f); Gfx.rotate(g, 0.32f);
                g.fill(0, -H, 8, H, Gfx.argb((int) (34 * Math.sin(f * Math.PI) * cin), col));
                Gfx.pop(g);
            }
        }
    }

    /** RNG Drop, in front of the scene: confetti thrown out of the drop, falling across the whole screen, and twinkling stars. */
    private static void epicFront(GuiGraphicsExtractor g, Anim a, float since, int W, int H, float cx, float cy, float k) {
        if (k <= 0.02f) return;
        int rc = a.p.rarity & 0xFFFFFF;
        for (int i = 0; i < (a.p.style == 1 ? 34 : 110); i++) {
            float life = since - 0.05f - Tex.h(i, 41) * 0.25f;
            if (life < 0f || life > 3.4f) continue;
            float ang = (float) (Tex.h(i, 42) * Math.PI * 2), sp = W * (0.22f + 0.5f * Tex.h(i, 43)), grav = H * 0.95f;
            float px = cx + (float) Math.cos(ang) * sp * (1f - (float) Math.exp(-life * 2.2f)) / 2.2f * 1.6f + (float) Math.sin(life * 4f + i) * 4f;
            float py = cy + (float) Math.sin(ang) * sp * 0.6f * (1f - (float) Math.exp(-life * 2.2f)) / 2.2f * 1.6f + 0.5f * grav * life * life * 0.35f;
            if (py > H + 6 || px < -6 || px > W + 6) continue;
            float tw = 0.55f + 0.45f * (float) Math.sin(life * 14f + i * 2.1f);             // tumbling confetti flickers
            int col = i % 5 == 0 ? rc : CONFETTI[i % CONFETTI.length];
            int w = (i % 3 == 0) ? 3 : 2, h = tw > 0.7f ? 4 : 2;
            g.fill((int) px, (int) py, (int) px + w, (int) py + h, Gfx.argb((int) (235 * k * clamp((3.4f - life) / 0.8f) * (0.55f + 0.45f * tw)), col));
        }
        for (int i = 0; i < (a.p.style == 1 ? 4 : 9); i++) {                                     // four-point glints twinkling around the drop
            float ph = (since * 1.7f + Tex.h(i, 51)) % 1f; float tw = (float) Math.sin(ph * Math.PI);
            if (tw <= 0.05f || since > 3.6f) continue;
            float px = cx + (Tex.h(i, 52) - 0.5f) * W * 0.42f, py = cy + (Tex.h(i, 53) - 0.5f) * H * 0.42f;
            int len = 2 + Math.round(5 * tw), al = (int) (240 * tw * k);
            g.fill((int) px - len, (int) py, (int) px + len + 1, (int) py + 1, Gfx.argb(al, 0xFFFFFF));
            g.fill((int) px, (int) py - len, (int) px + 1, (int) py + len + 1, Gfx.argb(al, 0xFFFFFF));
            g.fill((int) px - 1, (int) py - 1, (int) px + 2, (int) py + 2, Gfx.argb(al / 2, rc));
        }
    }

    /** Pitch of slice k: a rising A-major pentatonic scale from an octave down to an octave up (the chop sounds are recorded on A). */
    private static final int[] SCALE = {-12, -10, -8, -5, -3, 0, 2, 4, 7, 9, 12};
    static float chopPitch(int k) {
        int semis = k < SCALE.length ? SCALE[k] : (k % 2 == 0 ? 12 : 9);
        return (float) Math.pow(2.0, semis / 12.0);
    }

    private static void sounds(Anim a, float t, float ts, float len) {
        boolean[] d = a.done;
        String sc = a.scene();
        for (int k = 0; k < a.p.chops && k < 20; k++) {
            if (!d[k] && t >= Scene.impact(a.p, k)) {
                d[k] = true;
                SoundFx.playSlot(sc, "chop", chopPitch(k));                                              // every slice steps up a musical scale, so the slices play a little tune
            }
        }
        float big = a.tier == 2 ? 0.82f : a.tier == 1 ? 0.92f : 1f;                           // same sounds in every scene, a bit deeper when bigger
        if (!d[20] && t >= ts) { d[20] = true; SoundFx.playSlot(sc, "split", big); }
        if (!d[21] && t >= ts + 0.15f) { d[21] = true; SoundFx.playSlot(sc, "reveal", 1f); }
        if (!d[23] && t >= lingerAt(ts, len)) { d[23] = true; SoundFx.playSlot(sc, "linger", 1f); }        // the long glow that fills the time between the reveal and the end
        if (!d[22] && t >= len - 0.9f) { d[22] = true; SoundFx.playSlot(sc, "end", 1f); }
    }

    private AnimationManager() {}
}
