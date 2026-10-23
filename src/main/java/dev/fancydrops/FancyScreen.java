package dev.fancydrops;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.*;

/**
 * /fancydrops menu: a sidebar with six pages, and cards on the right.
 * Every page is built from rows (title + optional second line + controls on the right); sections can be folded open like dropdowns.
 */
public class FancyScreen extends Screen {
    private enum Tab {
        HOME("Home", "nether_star", "What the mod does"),
        DROPS("Drop Library", "chest", "Choose what animates and announces"),
        ANIMATIONS("Animations", "firework_rocket", "Scenes, sounds and looks"),
        SOUNDS("Sounds", "note_block", "Volume and sound packs"),
        CHAT("Chat", "writable_book", "Tell your party about drops"),
        GENERAL("Settings", "comparator", "Menu, prices and data"),
        STATS("Stats", "map", "Profit, kills and drops"),
        COSMETICS("Cosmetics", "nether_star", "Suffixes and looks"),
        RESOURCES("Resources", "book", "Credits and data sources");
        final String label, icon, sub;
        Tab(String l, String i, String s) { label = l; icon = i; sub = s; }
    }

    /** One colour for each main page; its sub pages use a lighter shade of it. */
    private static final int[] TAB_COL = {0xFFFFA45C, 0xFFFFC857, 0xFFFF72C6, 0xFF56D8FF, 0xFF7BE05A, 0xFFB0B6C2, 0xFFB56BFF, 0xFF6C86FF, 0xFFFF6B8A};
    private int tabAcc;
    private static int tabCol(Tab t) { return TAB_COL[t.ordinal()]; }
    private static int subCol(Tab t) { return Canvas.mix(tabCol(t), 0xFFFFFFFF, 0.5f); }
    private static final long[] EPIC = {500_000, 1_000_000, 2_000_000, 5_000_000, 10_000_000, 20_000_000, 50_000_000, 100_000_000, 500_000_000};
    private static final String[] CHANNELS = {"PARTY", "COOP", "ALL", "OFF"};
    private static final String[] STYLE_NAMES = {"Sword", "Spirit"};
    private static final String[] FARM_SLOT_NAMES = {"Swing", "Harvest", "Reveal", "Linger", "End"};
    private static final String[] EXP_SLOT_NAMES = {"Page flip", "Glow", "Reveal", "Linger", "End"};
    private static final String[] EXP_SLOT_SUBS = {"Every page the book turns", "The book bursts into light", "The drop appears", "The glow after it", "The scene fades out"};
    private static final String[] FARM_SLOT_SUBS = {"Every swing of the scythe", "The last crop opens", "The drop appears", "The glow after it", "The scene fades out"};
    private static final String[] SLOT_NAMES = {"Chop", "Split", "Reveal", "Linger", "End"};
    private static final String[] MYTH_SLOT_NAMES = {"Lightning", "Eruption", "Reveal", "Linger", "End"};
    private static final String[] MYTH_SLOT_SUBS = {"Every lightning strike on the rune circle", "The circle erupts in light", "The drop appears", "The glow after it", "The scene fades out"};
    private static final String[] DYE_SLOT_NAMES = {"Splash", "Burst", "Reveal", "Linger", "End"};
    private static final String[] DYE_SLOT_SUBS = {"Every paint splash landing", "The paint bursts open", "The dye appears", "The glow after it", "The scene fades out"};
    private static final String[] SLOT_SUBS = {"Every slice of the cleaver", "The cube splits open", "The drop appears", "The glow after it", "The scene fades out"};
    private static final int GREEN = 0xFF2D7D46;
    private static final int CELL = 54;          // width of each Play / Animate / Announce column

    // ------------------------------------------------------------------ rows and controls

    private abstract class Ctl {
        int x0, x1;
        String key = "";
        abstract int width();
        abstract void draw(GuiGraphicsExtractor g, int y, int h, boolean hover);
        boolean click(double mx, int button) { return false; }
        boolean drag(double mx) { return false; }
    }

    private class Switch extends Ctl {
        final BooleanSupplier get; final Consumer<Boolean> set;
        Switch(BooleanSupplier g, Consumer<Boolean> s) { get = g; set = s; }
        int width() { return 24; }
        void draw(GuiGraphicsExtractor g, int y, int h, boolean hover) {
            boolean on = get.getAsBoolean();
            int ty = y + (h - 12) / 2;
            float v = anim("sw|" + key, on ? 1f : 0f, 36f);
            pill(g, x0, ty, 24, 12, Canvas.mix(Ui.OFF, Ui.ACCENT, v));
            pill(g, x0 + 1 + Math.round(12 * v), ty + 1, 10, 10, hover ? 0xFFFFFFFF : 0xFFE6E6EC);
        }
        boolean click(double mx, int button) { set.accept(!get.getAsBoolean()); return true; }
    }

    /** An Animate / Announce button: glows in its own colour with a pulsing light when ON, goes dark when OFF. */
    private class Tog extends Ctl {
        final BooleanSupplier get; final Consumer<Boolean> set; final int col;
        Tog(BooleanSupplier g, Consumer<Boolean> s, int col) { get = g; set = s; this.col = col; }
        int width() { return 40; }
        void draw(GuiGraphicsExtractor g, int y, int h, boolean hover) {
            boolean on = get.getAsBoolean();
            int ty = y + (h - 14) / 2, w = x1 - x0;
            float v = anim("tg|" + key, on ? 1f : 0f, 30f), hv = anim("th|" + key, hover ? 1f : 0f, 48f);
            int base = Canvas.mix(0xFF2B2E3B, col, v);
            pill(g, x0, ty, w, 14, Canvas.mix(base, 0xFFFFFFFF, hv * 0.18f));
            g.fill(x0 + 2, ty + 1, x1 - 2, ty + 2, Gfx.argb(Math.round(70 * v + 25 * hv), 0xFFFFFF));          // top shine
            g.fill(x0 + 2, ty + 12, x1 - 2, ty + 13, Gfx.argb(Math.round(70 * (1f - v)), 0x000000));           // shadow while off
            float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 380.0 + x0 * 0.05);
            if (v > 0.02f) g.outline(x0, ty, w, 14, Gfx.argb(Math.round((50 + 70 * pulse) * v), 0xFFFFFF));      // breathing edge
            int led = on ? Canvas.mix(col, 0xFFFFFFFF, 0.55f + 0.45f * pulse) : 0xFF4A4D5C;
            g.fill(x0 + 5, ty + 5, x0 + 9, ty + 9, led);
            if (on) g.fill(x0 + 4, ty + 4, x0 + 10, ty + 10, Gfx.argb(Math.round(60 * pulse), 0xFFFFFF));
            Gfx.text(g, font, on ? "ON" : "OFF", x0 + 13 + (on ? 3 : 0), ty + 3, on ? 0xFFFFFFFF : 0xFF8A8D9C);
        }
        boolean click(double mx, int button) { if (button == 0) set.accept(!get.getAsBoolean()); return true; }
    }

    private class Btn extends Ctl {
        final String label; final int color; final Runnable run;
        Btn(String label, int color, Runnable run) { this.label = label; this.color = color; this.run = run; }
        int width() { return Math.max(22, font.width(label) + 14); }
        void draw(GuiGraphicsExtractor g, int y, int h, boolean hover) {
            int ty = y + (h - 14) / 2;
            pill(g, x0, ty, x1 - x0, 14, hover ? Canvas.mix(color, 0xFFFFFFFF, 0.18f) : color);
            Gfx.textCentered(g, font, label, (x0 + x1) / 2, ty + 3, 0xFFFFFFFF);
        }
        boolean click(double mx, int button) { if (button == 0) run.run(); return true; }
    }

    private class Step extends Ctl {
        final Supplier<String> value; final IntConsumer step; final int w;
        Step(Supplier<String> v, IntConsumer s, int w) { value = v; step = s; this.w = w; }
        int width() { return w; }
        void draw(GuiGraphicsExtractor g, int y, int h, boolean hover) {
            int ty = y + (h - 14) / 2;
            pill(g, x0, ty, x1 - x0, 14, Ui.PANEL2);
            chevron(g, x0 + 7, ty + 7, 3, hover ? Ui.ACCENT : Ui.DIM);
            chevron(g, x1 - 8, ty + 7, 0, hover ? Ui.ACCENT : Ui.DIM);
            Gfx.textCentered(g, font, font.plainSubstrByWidth(value.get(), x1 - x0 - 24), (x0 + x1) / 2, ty + 3, Ui.TEXT);
        }
        boolean click(double mx, int button) { step.accept(button == 1 ? -1 : mx < x0 + 12 ? -1 : 1); return true; }
    }

    private class Slider extends Ctl {
        final double min, max, stp; final DoubleSupplier get; final DoubleConsumer set; final Supplier<String> text;
        Slider(double min, double max, double stp, DoubleSupplier g, DoubleConsumer s, Supplier<String> t) { this.min = min; this.max = max; this.stp = stp; get = g; set = s; text = t; }
        boolean live;                                    // size sliders: while you drag, the menu steps aside and the real scene loops so you see the change
        Slider live() { live = true; return this; }
        final int id = sliderSeq++;                      // rows are rebuilt every frame, so a slider is told apart by its order
        double factor = 0;                               // > 0: the number is clickable and can be typed (shown value = raw * factor)
        Slider typed(double f) { factor = f; return this; }
        int width() { return 140; }
        int trackX1() { return x1 - 38; }
        void draw(GuiGraphicsExtractor g, int y, int h, boolean hover) {
            double f = Math.max(0, Math.min(1, (get.getAsDouble() - min) / (max - min)));
            int ty = y + h / 2, kx = x0 + (int) (f * (trackX1() - x0));
            g.fill(x0, ty - 1, trackX1(), ty + 1, Ui.OFF);
            g.fill(x0, ty - 1, kx, ty + 1, Ui.ACCENT);
            pill(g, kx - 3, ty - 5, 7, 10, hover ? 0xFFFFFFFF : 0xFFE6E6EC);
            if (typingId == id) {
                numField.setX(trackX1() + 2); numField.setY(ty - 8); numField.setWidth(x1 - trackX1() - 2); numShow = true;
            } else {
                String tx = text.get(); int txw = font.width(tx);
                if (factor > 0) {
                    boolean on = hover && lastMx >= trackX1();
                    g.fill(x1 - txw - 3, ty - 6, x1 + 1, ty + 6, on ? 0xFF2E3446 : 0xFF222838);
                    g.outline(x1 - txw - 3, ty - 6, txw + 4, 12, on ? Ui.ACCENT : Ui.BORDER);
                }
                Gfx.text(g, font, tx, x1 - txw, ty - 4, Ui.ACCENT);
            }
        }
        boolean click(double mx, int button) {
            if (factor > 0 && button == 0 && mx >= trackX1()) { beginTyping(this); return false; }
            return drag(mx);
        }
        void commit(String v) {
            try {
                double d = Double.parseDouble(v.trim().replace("%", "")) / (factor > 0 ? factor : 1);
                set.accept(Math.max(min, Math.min(max, Math.round(d / stp) * stp)));
            } catch (NumberFormatException ignored) {}
        }
        String current() { return String.valueOf(Math.round(get.getAsDouble() * factor)); }
        boolean drag(double mx) {
            double f = Math.max(0, Math.min(1, (mx - x0) / (double) (trackX1() - x0)));
            double v = Math.round((min + f * (max - min)) / stp) * stp;
            set.accept(Math.max(min, Math.min(max, v))); return true;
        }
    }

    /** A dropdown button: shows the current choice, click to open or close the list under its row. */
    private class Dd extends Ctl {
        final Supplier<String> value; final String key; final int w;
        Dd(Supplier<String> v, String key, int w) { value = v; this.key = key; this.w = w; }
        int width() { return w; }
        void draw(GuiGraphicsExtractor g, int y, int h, boolean hover) {
            int ty = y + (h - 14) / 2; boolean o = open.contains(key);
            pill(g, x0, ty, x1 - x0, 14, o ? Ui.ROW_HOVER : Ui.PANEL2);
            Gfx.text(g, font, font.plainSubstrByWidth(value.get(), x1 - x0 - 20), x0 + 5, ty + 3, hover || o ? Ui.ACCENT : Ui.TEXT);
            chevron(g, x1 - 9, ty + 7, o ? 2 : 1, hover || o ? Ui.ACCENT : Ui.DIM);
        }
        boolean click(double mx, int button) {
            boolean was = open.contains(key);
            open.removeIf(k -> k.startsWith("dd_"));              // one list open at a time
            if (!was) open.add(key);
            return true;
        }
    }

    /** A read-only value in a little pill (stats). */
    private class Val extends Ctl {
        final Supplier<String> text; final int w;
        Val(Supplier<String> t, int w) { text = t; this.w = w; }
        int width() { return w; }
        void draw(GuiGraphicsExtractor g, int y, int h, boolean hover) {
            int ty = y + (h - 14) / 2;
            pill(g, x0, ty, x1 - x0, 14, Ui.PANEL2);
            Gfx.textCentered(g, font, font.plainSubstrByWidth(text.get(), x1 - x0 - 8), (x0 + x1) / 2, ty + 3, Ui.ACCENT);
        }
    }

    private static final int NORMAL = 0, SECTION = 1, BANNER = 2, NOTE = 3, PREVIEW = 4, TEXT = 5, PANEL = 6, HERO = 7, TIP = 8, CHATPREV = 9, PICKER = 11, COSPREV = 12, CAT = 13;
    private class Row {
        String fkey;                                                      // set on folding rows: lets the Show > All tile open or close them all

        int kind = NORMAL, indent, accent = Ui.ACCENT, h = 28, x, y, w;
        String icon, iconId, title = "", sub = "";
        FancyConfig.DropCfg dcfg;      // drop rows: the switches, so the Drop Library "Show" filter can hide rows
        String spriteId;               // a creature picture that replaces the drawn icon once it has downloaded
        int pbw;                       // PREVIEW rows: draw at this width, left aligned (the dock)
        boolean nolabel;               // PREVIEW rows: the caller writes its own caption
        String pkey = null;            // PREVIEW rows: the animation type to play (null = the category's default)
        int pcat = -1;                 // PREVIEW rows: which Drop Library category to show (-1 = the one chosen in Settings)
        int subW;                      // wrapped description width (0 = default)
        int subColor = Ui.SUB;
        List<Ctl> ctls = new ArrayList<>();
        Runnable click;                // clicking the row body
        boolean arrow, open;
        EditBox field; String fieldBtn; int btnX0, btnX1;
        boolean dropCols;              // section header that labels the Play / Animate / Announce columns
        int cell;                      // > 0: every control sits centred in its own column of this width
        String[] pLabels, pIcons, pSubs; int pTh = 27, pColsForce, pTop = 22; int[] pColors; boolean[] pWip; int pCols; IntSupplier pSel; IntConsumer pSet; java.util.function.IntPredicate pOn; boolean[] pLock; String[] pTips, pFx, pIds; int[] pRar, pBg; Runnable[] pAct;      // PICKER rows: the tiles (pOn: which are switched on, pLock: which are locked)
        boolean locked;               // greyed out, not clickable (coming soon)
        boolean wrapSub;              // the second line is a paragraph, wrapped over several lines
        boolean wip;                  // gets the slanted WORK IN PROGRESS tape across it
        boolean noSearch;             // never shown in search results
        List<String> pickLabels; int pickSel; IntConsumer onPick; Ctl anchor; Row owner;   // small drop-down list under a Dd button
    }

    // ------------------------------------------------------------------ state

    private static Tab tab = Tab.HOME;
    private static final Set<String> open = new HashSet<>();
    private static int scroll;                 // kept when the menu reopens after a preview, so you land where you were
    private int totalH;
    private static String query = "";
    private boolean searching;
    private EditBox searchField;
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("fancydrops", "textures/gui/icon.png");
    private final List<Row> rows = new ArrayList<>();
    private EditBox formatField, addField, numField, hexField;
    private static int sliderSeq; private int typingId = -1;
    private Slider typing; private boolean numShow; private double lastMx;
    private String formatError;                                  // why the typed message was refused, or null
    private static String chatStatus = ""; private static int chatStatusColor = 0xFFFF7777;   // result of the last "Test" press
    static { open.add("cat_slayers"); }
    private Ctl dragCtl;
    private int px, py, pw, ph, sw = 132, cx, cw, lt, lb;
    private static final int RS = 0;

    private static boolean skipIntro;                       // true when coming back from a preview: no fly-in again
    private static long tabChanged, seenTabChange;
    private static float pageK = 1f;
    private static boolean STILL;                                             // menu animations off: no waving, no glints
    private static float selY = Float.NaN;
    private final long born;
    private long closeAt;                                    // set when Esc is pressed: the menu fades out, then closes

    /** Set by /fd stats and /fd <search>: the next menu opens on the Stats page, or with this text in the search box. */
    static boolean pendingStats; static String pendingSearch;

    public FancyScreen() {
        super(Component.literal("FancyDrops"));
        if (!skipIntro) { tab = Tab.HOME; scroll = 0; query = ""; }
        if (pendingStats) { pendingStats = false; tab = Tab.STATS; subSel[Tab.STATS.ordinal()] = -1; scroll = 0; }
        if (pendingSearch != null) { query = pendingSearch; pendingSearch = null; scroll = 0; }          // a fresh /fd opens on Home; coming back from a preview keeps your page
        born = skipIntro ? 0L : System.currentTimeMillis();
        skipIntro = false;
    }

    @Override protected void init() {
        PriceService.warm();
        pw = Math.min(560, width - 76); ph = menuHeight(height);
        px = (width - pw) / 2; py = (height - ph) / 2;
        cx = px + sw + 10; cw = pw - sw - 20 - RS; lt = py + 46; lb = py + ph - 10;

        formatField = new EditBox(font, 0, 0, 100, 16, Component.literal("format"));
        formatField.setMaxLength(ChatFilter.MAX);
        formatField.setValue(FancyConfig.get().formatFor(Math.min(4, chatEv)));
        formatField.setResponder(v -> {                    // only a clean message is ever saved
            formatError = ChatFilter.check(v);
            if (formatError == null && chatEv <= 4) FancyConfig.get().setFormat(chatEv, v);
        });
        addRenderableWidget(formatField);

        addField = new EditBox(font, 0, 0, 100, 16, Component.literal("drop name"));
        addField.setMaxLength(60);
        addField.setHint(Component.literal("e.g. Dark Claymore"));
        addRenderableWidget(addField);
        searchField = new EditBox(font, 0, 0, 120, 14, Component.literal("search"));
        searchField.setMaxLength(30);
        searchField.setHint(Component.literal("Search..."));
        searchField.setValue(query);
        searchField.setResponder(v -> { query = v; scroll = 0; sideScroll = 0; });
        addRenderableWidget(searchField);
        numField = new EditBox(font, 0, 0, 40, 14, Component.literal("value"));
        numField.setMaxLength(4);
        numField.setResponder(v -> { String f = v.replaceAll("[^0-9.]", ""); if (!f.equals(v)) numField.setValue(f); });
        addRenderableWidget(numField);
        numField.setVisible(false);
        hexField = new EditBox(font, 0, 0, 64, 14, Component.literal("hex"));
        hexField.setMaxLength(7);
        hexField.setResponder(this::hexTyped);
        addRenderableWidget(hexField);
        hexField.setVisible(false);
        prevBoxes.clear();
        formatField.setVisible(false); addField.setVisible(false);
    }

    private void beginTyping(Slider s) {
        typing = s; typingId = s.id; numField.setValue(s.current()); numField.setVisible(true); setFocused(numField); numField.setFocused(true);
        numField.moveCursorToEnd(false); numField.setHighlightPos(0);
    }
    private void endTyping(boolean apply) {
        if (typing == null) return;
        if (apply) typing.commit(numField.getValue());
        typing = null; typingId = -1; numField.setFocused(false); numField.setVisible(false);
    }

    @Override public void removed() { FancyConfig.save(); SoundFx.stopPreviews(); }
    @Override public boolean isPauseScreen() { return false; }

    // ------------------------------------------------------------------ drawing helpers

    private static float TR = 1f;                    // opacity of the panel colours this frame (1 = solid), from General > Menu translucency
    private static final Map<String, Float> ANIM = new HashMap<>();
    private static long lastFrame;
    private static float dt = 0.016f;                // seconds since the previous frame

    /** Moves a stored value toward target (smooth, frame-rate independent); returns it. With animations off it jumps. */
    private static float anim(String key, float target, float speed) {
        if (!FancyConfig.get().menuAnimated) { ANIM.put(key, target); return target; }
        float v = ANIM.getOrDefault(key, target);
        v += (target - v) * (1f - (float) Math.exp(-dt * speed));
        if (Math.abs(target - v) < 0.004f) v = target;
        ANIM.put(key, v);
        return v;
    }
    /** A slim chevron: dir 0 = right, 1 = down, 2 = up, 3 = left. */
    private static void chevron(GuiGraphicsExtractor g, int cx, int cy, int dir, int color) {
        for (int i = 0; i < 4; i++) {
            switch (dir) {
                case 0 -> { g.fill(cx - 2 + i, cy - 3 + i, cx - 1 + i, cy - 2 + i, color); g.fill(cx - 2 + i, cy + 3 - i, cx - 1 + i, cy + 4 - i, color); }
                case 1 -> { g.fill(cx - 3 + i, cy - 2 + i, cx - 2 + i, cy - 1 + i, color); g.fill(cx + 3 - i, cy - 2 + i, cx + 4 - i, cy - 1 + i, color); }
                case 2 -> { g.fill(cx - 3 + i, cy + 2 - i, cx - 2 + i, cy + 3 - i, color); g.fill(cx + 3 - i, cy + 2 - i, cx + 4 - i, cy + 3 - i, color); }
                default -> { g.fill(cx + 2 - i, cy - 3 + i, cx + 3 - i, cy - 2 + i, color); g.fill(cx + 2 - i, cy + 3 - i, cx + 3 - i, cy + 4 - i, color); }
            }
        }
    }

    /** Text that is too wide for its space slides left, pauses, then slides back right, over and over. */
    private void marquee(GuiGraphicsExtractor g, String s, int x, int y, int limit, int color) {
        int w = font.width(s);
        if (w <= limit) { Gfx.text(g, font, s, x, y, color); return; }
        float over = w - limit, speed = 26f, move = over / speed, pause = 1.0f, cyc = 2 * (move + pause);
        float tt = (System.currentTimeMillis() % (long) (cyc * 1000)) / 1000f, off;
        if (tt < pause) off = 0; else if (tt < pause + move) off = (tt - pause) * speed;
        else if (tt < 2 * pause + move) off = over; else off = over - (tt - 2 * pause - move) * speed;
        g.enableScissor(x, y - 2, x + limit, y + 11);
        Gfx.text(g, font, s, x - Math.round(off), y, color);
        g.disableScissor();
    }

    private static final long OPEN_MS = 280L, CLOSE_MS = 210L;
    private static float easeIn(float t) { t = Math.max(0f, Math.min(1f, t)); return t * t * (3f - 2f * t); }
    private static float ease(float t) { t = Math.max(0f, Math.min(1f, t)); return 1f - (1f - t) * (1f - t) * (1f - t); }

    /** The menu's own panel colours get the translucency; everything else (buttons, text, accents) stays solid. */
    private static int soft(int c) {
        if (TR >= 0.999f) return c;
        if (c == Ui.PANEL || c == Ui.PANEL2 || c == Ui.ROW || c == Ui.ROW_HOVER || c == 0xFF17181F)
            return (Math.round(0xFF * TR) << 24) | (c & 0xFFFFFF);
        return c;
    }

    /** Rectangle with softly cut corners. */
    /** A soft round backdrop in the mod's blue, so an icon never floats on the bare panel. */
    private static void iconBack(GuiGraphicsExtractor g, int cx, int cy, int r, boolean lit) { iconBack(g, cx, cy, r, lit, Ui.ACCENT); }
    private static void iconBack(GuiGraphicsExtractor g, int cx, int cy, int r, boolean lit, int tint) {
        int col = lit ? Gfx.argb(110, tint & 0xFFFFFF) : Gfx.argb(60, tint & 0xFFFFFF);
        for (int dy = -r; dy <= r; dy++) { int hw = (int) Math.round(Math.sqrt(Math.max(0, r * r + 0.25 - dy * dy))); g.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, col); }
    }
    private static void pill(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        c = soft(c);
        g.fill(x + 1, y, x + w - 1, y + h, c);
        g.fill(x, y + 1, x + 1, y + h - 1, c);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    private static int idx(Object[] arr, Object v) { for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i; return 0; }
    private static int idx(long[] arr, long v) { int b = 0; for (int i = 0; i < arr.length; i++) if (arr[i] <= v) b = i; return b; }
    private static int wrap(int i, int n) { return ((i % n) + n) % n; }
    private static int sliceOf(FancyConfig c, int k) { return k == 0 ? c.chopsDefault : k == 1 ? c.chopsCool : c.chopsEpic; }
    private static void setSlice(FancyConfig c, int k, int v) { if (k == 0) c.chopsDefault = v; else if (k == 1) c.chopsCool = v; else c.chopsEpic = v; }
    private static int clampN(int n) { return Math.max(1, Math.min(20, n)); }

    // ------------------------------------------------------------------ building the pages

    /** A little screen: the real scene (looping) where it will appear, with the cinematic bars as they will close in. */
    private void drawPositionPreview(GuiGraphicsExtractor g, Row r) {
        FancyConfig c = FancyConfig.get();
        int cat = r.pcat >= 0 ? r.pcat : previewSel;
        boolean wip = cat > 4;
        int bw = r.pbw > 0 ? r.pbw : Math.min(r.w - 24, 214), bh = previewH(bw), bx = r.pbw > 0 ? r.x : r.x + (r.w - bw) / 2, by = r.pbw > 0 ? r.y : r.y + 2;
        pill(g, bx - 2, by - 2, bw + 4, bh + 4, Ui.PANEL2);
        if (!AnimationManager.textOnly && !AnimationManager.itemOnly) drawBackdrop(g, bx, by, bw, bh, cat <= 4 && c.blur(cat));
        if (!wip && cat == 2) prevBoxes.add(new int[]{bx, by, bw, bh, cat});
        if (wip) {
            Gfx.textCentered(g, font, font.plainSubstrByWidth(CATS[Math.min(cat, CATS.length - 1)], bw - 8), bx + bw / 2, by + bh / 2 - 14, Ui.TEXT);
            Gfx.textCentered(g, font, font.plainSubstrByWidth("Coming soon", bw - 8), bx + bw / 2, by + bh / 2 - 2, Ui.SUB);
            Gfx.textCentered(g, font, "WIP", bx + bw / 2, by + bh / 2 + 10, 0xFFB08A20);
            if (!r.nolabel) Gfx.textCentered(g, font, font.plainSubstrByWidth("Preview  ·  " + CATS[Math.min(cat, CATS.length - 1)], r.w - 8), r.x + r.w / 2, by + bh + 5, Ui.SUB);
            return;
        }
        float s = bw / (float) Math.max(1, width);
        g.enableScissor(bx, by, bx + bw, by + bh);
        Gfx.push(g); Gfx.translate(g, bx, by); Gfx.scale(g, s);
        AnimationManager.drawPreview(g, width, height, cat, r.pkey);
        Gfx.pop(g);
        g.disableScissor();
        if (!r.nolabel) Gfx.textCentered(g, font, font.plainSubstrByWidth("Preview  ·  " + CATS[cat] + "  ·  " + AnimationManager.previewPosition(cat, c), r.w - 8), r.x + r.w / 2, by + bh + 5, Ui.SUB);
    }


    // ------------------------------------------------------------------ the live preview dock

    private static final int DOCK_H = 128, DOCK_GAP = 6, COS_H = 124, AURA_W = 118;
    private float cosK;
    private static final int[] SLOT_COL = {0xFF56D8FF, 0xFFFF72C6, 0xFFFFC857, 0xFF7CE06A, 0xFFB56BFF};     // chop, split, reveal, linger, end
    private static final long[] pulseAt = new long[5];                                            // when each sound slot was last played by hand
    private final int[] dockBtn = {-1, -1, -1, -1};
    private float dockK;                                                                          // 0..1: how far the dock has slid open
    private static int soundCat;                                                                  // Sounds > Animation Sounds: which animation page is open

    private boolean dockPossible() { return height >= 390 && FancyConfig.get().previewAt != 2; }
    private boolean dockWanted() {
        if (!dockPossible() || !query.isBlank()) return false;
        if (tab == Tab.ANIMATIONS) return !onLanding();                      // only inside a scene page
        return tab == Tab.SOUNDS && FancyConfig.get().sound && !onLanding() && curSub() < 5;
    }
    private static int menuHeight(int screenH) { return Math.min(350, screenH - 16 - (screenH >= 390 && FancyConfig.get().previewAt != 2 ? DOCK_H + DOCK_GAP : 0)); }

    private static String slotName(int sc, int k) { return sc == 7 ? EXP_SLOT_NAMES[k] : (sc == 6 || sc == 9) ? MYTH_SLOT_NAMES[k] : (sc == 5 || sc == 8) ? FARM_SLOT_NAMES[k] : sc == 4 ? DYE_SLOT_NAMES[k] : SLOT_NAMES[k]; }

    /** The live preview: the real scene on the left, and on the right either every setting of the animation (Animations) or animated sound waves (Sounds). Sits above or below the menu. */
    private void drawDock(GuiGraphicsExtractor g, int mx, int my, float intro, long now) {
        dockBtn[0] = dockBtn[1] = dockBtn[2] = dockBtn[3] = -1;
        if (dockK < 0.97f) return;
        FancyConfig c = FancyConfig.get();
        boolean top = c.previewAt == 0, sounds = tab == Tab.SOUNDS;
        int x0 = px, w = pw, y0 = top ? py - DOCK_H - DOCK_GAP : py + ph + DOCK_GAP;
        int cat = Math.max(0, Math.min(CATS.length - 1, sounds ? soundCat : ORD[Math.min(CATS.length - 1, curSub())]));
        String key = null, tname = CATS[cat]; int sc = 0;
        if (cat < ATYPES.length) { String[] t = ATYPES[cat][Math.max(0, Math.min(ATYPES[cat].length - 1, animType[cat]))]; key = t[1]; tname = t[0]; sc = Integer.parseInt(t[2]); }
        pill(g, x0, y0, w, DOCK_H, Ui.PANEL);
        g.outline(x0, y0, w, DOCK_H, Ui.BORDER);
        Gfx.text(g, font, sounds ? "Live sound preview" : "Live animation preview", x0 + 8, y0 + 5, Ui.TEXT);
        String lab = top ? "▼ Move to bottom" : "▲ Move to top";
        int bwid = font.width(lab) + 12, bx2 = x0 + w - bwid - 6, by2 = y0 + 3;
        boolean hv = mx >= bx2 && mx < bx2 + bwid && my >= by2 && my < by2 + 13;
        pill(g, bx2, by2, bwid, 13, hv ? Ui.ROW_HOVER : Ui.PANEL2);
        Gfx.text(g, font, lab, bx2 + 6, by2 + 3, hv ? Ui.TEXT : Ui.SUB);
        dockBtn[0] = bx2; dockBtn[1] = by2; dockBtn[2] = bx2 + bwid; dockBtn[3] = by2 + 13;

        int ph0 = DOCK_H - 22, bw = Math.min(w / 2 - 10, Math.round(ph0 * width / (float) Math.max(1, height)));
        Row syn = new Row(); syn.x = x0 + 8; syn.y = y0 + 19; syn.w = bw; syn.pcat = cat; syn.pkey = key; syn.nolabel = true; syn.pbw = bw;
        if (sounds && waveKey != null && key != null && waveKey.equals(key) && now - waveNow < 250) AnimationManager.syncSec = Math.max(0f, waveT);      // the scene follows the sound waves
        drawPositionPreview(g, syn);
        AnimationManager.syncSec = -1f;
        int rx = x0 + 8 + bw + 14, rw = x0 + w - 8 - rx;
        if (sounds) drawWaves(g, rx, y0 + 19, rw, ph0, sc, key, now);
        else drawDetails(g, rx, y0 + 19, rw, cat, key, tname, sc, c);
        if (intro < 1f) pill(g, x0, y0, w, DOCK_H, Gfx.argb(Math.round((1f - intro) * 150), 0x0C0E14));
    }

    /** Everything about the open animation, in two columns, plus its four sounds. */
    private void drawDetails(GuiGraphicsExtractor g, int x, int y, int w, int cat, String key, String tname, int sc, FancyConfig c) {
        List<String[]> d = new ArrayList<>();
        d.add(new String[]{"Animation", tname});
        if (key == null) { d.add(new String[]{"Status", "Coming soon"}); }
        else {
            d.add(new String[]{"Length", String.format(java.util.Locale.ROOT, "%.1f s", secsOf(c, key))});
            switch (key) {
                case "default", "rare", "rng" -> { int n = clampN(sliceOf(c, key.equals("default") ? 0 : key.equals("rare") ? 1 : 2)); d.add(new String[]{"Slices", n + ""}); d.add(new String[]{"Style", STYLE_NAMES[Math.floorMod(c.slayerStyle, STYLE_NAMES.length)]}); }
                case "dye" -> d.add(new String[]{"Splat", "1, in the dye's colour"});
                case "myth" -> d.add(new String[]{"Strikes", Math.max(1, Math.min(10, c.mythStrikes)) + ""});
                case "mythrng" -> d.add(new String[]{"Strikes", Math.max(1, Math.min(10, c.mythStrikesRng)) + ""});
                case "mythnothing" -> d.add(new String[]{"Strikes", Math.max(1, Math.min(10, c.mythStrikes)) + ""});
                case "exp" -> d.add(new String[]{"Flips", Math.max(1, Math.min(12, c.expFlips)) + ""});
                case "exprng" -> d.add(new String[]{"Flips", Math.max(1, Math.min(12, c.expFlipsRng)) + ""});
                case "nothing" -> d.add(new String[]{"Style", STYLE_NAMES[Math.floorMod(c.slayerStyle, STYLE_NAMES.length)]});
                default -> { }
            }
            d.add(new String[]{"Position", AnimationManager.previewPosition(cat, c)});
            if (AnimationManager.textChoice(c, cat)) d.add(new String[]{"Text", AnimationManager.TEXT_POS_NAMES[Math.floorMod(c.textPos, 2)]});
            if (cat != 2) {
                d.add(new String[]{"Bars", c.cine(cat) ? "On  ·  " + c.barPx + " px" : "Off"});
                d.add(new String[]{"Blur", c.blur(cat) ? "On" : "Off"});
                d.add(new String[]{"Size", Math.round((c.cine(cat) ? c.sizeOf(cat) : Math.max(1f, c.sizeOf(cat))) * 100) + "%"});
            } else d.add(new String[]{"Size", Math.round(c.farmScale * 100) + "%"});
            d.add(new String[]{"Item size", Math.round(c.itemScale * 100) + "%"});
        }
        final float k0 = 0.75f; int colW = w / 2, per = 8, lh = 12;
        for (int i = 0; i < d.size() && i < per * 2; i++) {
            int cx0 = x + (i / per) * colW, cy0 = y + (i % per) * lh;
            small(g, d.get(i)[0], cx0, cy0, Ui.DIM, k0);
            small(g, font.plainSubstrByWidth(d.get(i)[1], Math.round((colW - 46) / k0)), cx0 + 44, cy0, Ui.TEXT, k0);
        }
        if (key != null) {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < SoundFx.SLOTS.length; k++) sb.append(k == 0 ? "" : "  \u00B7  ").append(SoundFx.label(SoundFx.chosen(SoundFx.SCENES[sc], SoundFx.SLOTS[k])));
            small(g, "Sounds", x, y + 8 * lh + 2, Ui.DIM, k0);
            small(g, font.plainSubstrByWidth(sb.toString(), Math.round((w - 46) / k0)), x + 44, y + 8 * lh + 2, Ui.ACCENT, k0);
        }
    }

    private void small(GuiGraphicsExtractor g, String t, int x, int y, int col, float k) {
        Gfx.push(g); Gfx.translate(g, x, y); Gfx.scale(g, k); Gfx.text(g, font, t, 0, 0, col); Gfx.pop(g);
    }

    /** Sound waves of one animation: every sound is drawn at its real length (a pad swells, a tick fades fast) on a time line as long as the animation. */
    private void drawWaves(GuiGraphicsExtractor g, int x, int y, int w, int h, int sc, String key, long now) {
        FancyConfig c = FancyConfig.get();
        pill(g, x, y, w, h, Ui.PANEL2);
        if (key == null) { Gfx.textCentered(g, font, "Coming soon", x + w / 2, y + h / 2 - 4, Ui.SUB); return; }
        float[] st = AnimationManager.soundTimes(key, c);
        float total = Math.max(0.5f, st[0]); int n = (int) st[1], ne = n + 4;
        float[] te = new float[ne], du = new float[ne]; int[] slot = new int[ne];
        for (int i = 0; i < n; i++) { te[i] = st[2 + i]; slot[i] = 0; }
        te[n] = st[2 + n]; slot[n] = 1; te[n + 1] = st[3 + n]; slot[n + 1] = 2; te[n + 2] = st[4 + n]; slot[n + 2] = 3; te[n + 3] = st[5 + n]; slot[n + 3] = 4;
        float span = total;
        for (int i = 0; i < ne; i++) {
            String nm = SoundFx.chosen(SoundFx.SCENES[sc], SoundFx.SLOTS[slot[i]]);
            du[i] = SoundFx.NONE.equals(nm) ? 0f : SoundFx.duration(nm);
        }
        long loopMs = Math.round((span + 0.6f) * 1000f);
        boolean fresh = waveKey == null || !waveKey.equals(key) || now - waveNow > 250;
        if (fresh) { waveEl = 0; waveT = -1f; SoundFx.stopPreviews(); }                           // another animation (or the tab just opened): start again, and let the old sound go quiet
        else if (!wavePaused) waveEl += now - waveNow;
        float t = (waveEl % loopMs) / 1000f, sec = now / 1000f;
        if (t < waveT) waveCut = false;
        if (t >= span && !waveCut && !fresh) { waveCut = true; SoundFx.stopPreviews(); }                    // sounds end with the animation, like in the game
        if (!fresh && !wavePaused) {                                                              // the playhead just passed a sound: play it
            if (waveWasPaused) {                                                                  // resumed: sounds that were mid-play start again, so they carry on
                for (int i = 0; i < ne; i++) if (du[i] > 0f && te[i] <= t && t < te[i] + du[i] && t < span) {
                    SoundFx.Fx fx = SoundFx.byName(SoundFx.chosen(SoundFx.SCENES[sc], SoundFx.SLOTS[slot[i]]));
                    if (fx != null) SoundFx.preview(fx);
                }
            }
            if (t < waveT) SoundFx.stopPreviews();                                                // looped round: cut the tail of the last run
            float from = t >= waveT ? waveT : -1f;
            for (int i = 0; i < ne; i++) if (te[i] > from && te[i] <= t) {
                pulseAt[slot[i]] = now;
                SoundFx.Fx fx = SoundFx.byName(SoundFx.chosen(SoundFx.SCENES[sc], SoundFx.SLOTS[slot[i]]));
                if (fx != null) SoundFx.preview(fx);
            }
        }
        waveT = t; waveNow = now; waveKey = key; waveWasPaused = wavePaused;
        float[] power = {0.5f, 0.85f, 1f, 0.55f, 0.65f};
        int wy = y + 15, wh = h - 50, cy = wy + wh / 2; float amp = wh / 2f - 1f;               // the waves keep to their own band: a time strip above, the legend below
        String lenTxt = String.format(Locale.ROOT, "%.1f / %.1f s", Math.min(waveT < 0f ? 0f : t, span), span);
        Gfx.text(g, font, lenTxt, x + 6, y + 3, Ui.DIM);
        g.fill(x + 4, y + 12, x + w - 4, y + 13, 0x14FFFFFF);
        g.fill(x + 4, y + h - 31, x + w - 4, y + h - 30, 0x14FFFFFF);
        g.enableScissor(x + 1, y + 13, x + w - 1, y + h - 31);
        g.fill(x + 2, cy, x + w - 2, cy + 1, 0x22FFFFFF);                                          // the centre line
        for (int xx = x + 4; xx < x + w - 4; xx += 3) {
            float tx = (xx - x - 4) / (float) (w - 8) * span;
            float e = 0.03f; int dom = -1; float best = 0.05f;
            for (int i = 0; i < ne; i++) {
                float u = tx - te[i];
                if (u < 0f || u > du[i]) continue;
                float rel = u / du[i], shape;
                if (du[i] >= 2.4f) shape = (float) Math.pow(Math.sin(Math.PI * Math.min(1f, rel * 0.9f + 0.05f)), 0.7) * (0.55f + 0.45f * (float) Math.exp(-rel * 2f));   // a long pad swells and fades
                else shape = (1f - (float) Math.exp(-u / 0.012f)) * (float) Math.exp(-rel * 4.2f);                                                                         // a hit: sharp, then it dies away
                long since = now - pulseAt[slot[i]];
                float v = power[slot[i]] * shape;
                if (since >= 0 && since < 900) v += 0.35f * shape * (float) Math.exp(-since / 260.0);       // played by hand
                e += v;
                if (v > best) { best = v; dom = slot[i]; }
            }
            e = Math.min(1f, e);
            float jit = 0.7f + 0.3f * Math.abs((float) Math.sin(xx * 0.77 + sec * (tx <= t ? 9.0 : 0.0) + (xx % 5)));
            int hh = Math.max(1, Math.round(amp * e * jit));
            boolean played = tx <= t;
            int rgb = dom >= 0 ? SLOT_COL[dom] & 0xFFFFFF : 0x5A6C9A;
            if (e > 0.1f && played) g.fill(xx - 1, cy - hh - 1, xx + 3, cy + hh + 2, Gfx.argb(Math.round(10 + 45 * e), rgb));      // glow
            g.fill(xx, cy - hh, xx + 2, cy + hh + 1, Gfx.argb(dom < 0 ? 120 : played ? 235 : 120, rgb));                          // bar: dimmer until the playhead has passed
            if (e > 0.5f && played) g.fill(xx, cy - hh, xx + 2, cy - hh + 1, Gfx.argb(230, 0xFFFFFF));
        }
        int phx = x + 4 + Math.min(w - 8, Math.round(Math.min(t, span) / span * (w - 8)));
        g.fill(phx, wy - 2, phx + 1, wy + wh + 2, 0xAAFFFFFF);                                       // the playhead
        for (int i = 0; i < ne; i++) {                                                              // rings spreading from every hit
            float age = t - te[i];
            if (age < 0f || age > 0.8f) continue;
            ring(g, x + 4 + te[i] / span * (w - 8), cy, age / 0.8f * wh * 0.62f, 1.6f, Math.round((1f - age / 0.8f) * 170), SLOT_COL[slot[i]]);
        }
        for (int k = 0; k < 5; k++) {                                                               // rings from a hand-played sound
            long since = now - pulseAt[k];
            if (since < 0 || since > 800) continue;
            int first = 0; for (int i = 0; i < ne; i++) if (slot[i] == k) { first = i; break; }
            float u = since / 800f;
            ring(g, x + 4 + te[first] / span * (w - 8), cy, u * wh * 0.75f, 1.6f, Math.round((1f - u) * 220), SLOT_COL[k]);
        }
        g.disableScissor();
        int chipW = w / 5, cy2 = y + h - 25;                                                        // what each slot plays: its moment, then its sound
        for (int k = 0; k < 5; k++) {
            int cx0 = x + k * chipW + 5;
            boolean fired = false; for (int i = 0; i < ne; i++) if (slot[i] == k && t >= te[i] && t - te[i] < 0.35f) fired = true;
            if (now - pulseAt[k] < 400) fired = true;
            g.fill(cx0, cy2 + 1, cx0 + 5, cy2 + 6, fired ? 0xFFFFFFFF : SLOT_COL[k]);
            Gfx.text(g, font, font.plainSubstrByWidth(slotName(sc, k), chipW - 16), cx0 + 9, cy2, fired ? Ui.TEXT : Ui.SUB);
            Gfx.text(g, font, font.plainSubstrByWidth(SoundFx.label(SoundFx.chosen(SoundFx.SCENES[sc], SoundFx.SLOTS[k])), chipW - 16), cx0 + 9, cy2 + 10, fired ? SLOT_COL[k] : Ui.DIM);
        }
    }

    private static float waveT; private static long waveNow, waveEl; private static boolean wavePaused, waveCut, waveWasPaused; private static String waveKey;

    private static void ring(GuiGraphicsExtractor g, float cxf, float cyf, float r, float aspect, int alpha, int col) {
        if (alpha <= 4 || r < 1f) return;
        for (int i = 0; i < 36; i++) {
            double an = i * Math.PI * 2 / 36;
            int px0 = Math.round(cxf + (float) Math.cos(an) * r * aspect), py0 = Math.round(cyf + (float) Math.sin(an) * r * 0.7f);
            g.fill(px0, py0, px0 + 1, py0 + 1, Gfx.argb(alpha, col & 0xFFFFFF));
        }
    }

    /** The Home tip card: an indigo-to-magenta ribbon with a floating bulb, a light sweep, the tip, and dots that follow the 3-second rotation. */
    private void drawTip(GuiGraphicsExtractor g, Row r, int mx, int my) {
        long now = System.currentTimeMillis(); float sec = (now % 3600000L) / 1000f;
        boolean hv = mx >= r.x && mx < r.x + r.w && my >= r.y && my < r.y + r.h;
        pill(g, r.x, r.y, r.w, r.h, 0xFF1B1840);
        g.enableScissor(r.x + 1, r.y + 1, r.x + r.w - 1, r.y + r.h - 1);
        for (int x = 0; x < r.w - 2; x += 2) {                                                     // a colour wash, left to right
            float u = x / (float) r.w; int col = Canvas.mix(0xFF4B3FD8, 0xFFD84BB4, u);
            g.fill(r.x + 1 + x, r.y + 1, r.x + 3 + x, r.y + r.h - 1, Gfx.argb(hv ? 120 : 92, col & 0xFFFFFF));
        }
        int sweep = r.x + (int) (((sec * 0.4f) % 1.6f - 0.3f) * r.w);
        for (int i = 0; i < 16; i++) g.fill(sweep + i, r.y, sweep + i + 1, r.y + r.h, Gfx.argb(Math.round(30 * (1f - Math.abs(i - 8) / 8f)), 0xFFFFFF));
        g.disableScissor();
        g.outline(r.x, r.y, r.w, r.h, Gfx.argb(hv ? 220 : 140, 0xC9A8FF));
        int bx = r.x + 10, by = r.y + r.h / 2 - 8 + Math.round((float) Math.sin(sec * 2.4f) * 1.5f);
        if (Icons.has(r.icon)) Icons.draw(g, r.icon, bx, by, now);
        int tx = r.x + 34, tw = r.w - 34 - 10;
        Gfx.text(g, font, "TIP", tx, r.y + 6, 0xFFFFE08A);
        List<String> ln = wrap(r.title, tw);
        for (int i = 0; i < ln.size() && i < 2; i++) Gfx.text(g, font, ln.get(i), tx, r.y + 17 + i * 10, 0xFFFFFFFF);
        int cur = tipNow(), dx0 = r.x + r.w - (TIPS.length * 6) - 8, dy0 = r.y + 6;
        for (int i = 0; i < TIPS.length; i++) g.fill(dx0 + i * 6, dy0, dx0 + i * 6 + (i == cur ? 4 : 2), dy0 + 2, i == cur ? 0xFFFFE08A : 0x66FFFFFF);
    }

    private static final int[] PICK_PALETTE = {0xFF6C86FF, 0xFFFF72C6, 0xFF7BE05A, 0xFFFFC857, 0xFFB56BFF, 0xFF56D8FF};
    private static final String[] PICK_ICONS = {"fd:slayers", "fd:dye", "fd:farm", "fd:mytho", "fd:experiments", "fd:mining", "fd:fishing"};
    private static final int[] PICK_COLS = {0xFF6C86FF, 0xFFFF72C6, 0xFF7BE05A, 0xFFFFC857, 0xFFB56BFF, 0xFFB0B6C2, 0xFF56D8FF};

    /** A box of tiles to pick one thing from: a title, then one tile per choice (icon and name), the chosen one lit in its own colour. */
    private Row picker(String title, String sub, String[] labels, String[] icons, int[] colors, boolean[] wip, IntSupplier sel, IntConsumer set) {
        Row r = new Row(); r.kind = PICKER; r.noSearch = true; r.title = title; r.sub = sub;
        r.pLabels = labels; r.pIcons = icons; r.pColors = colors; r.pWip = wip; r.pSel = sel; r.pSet = set;
        r.pCols = labels.length <= 4 ? labels.length : labels.length <= 6 ? 3 : 4; r.pTop = title.isEmpty() ? 6 : 22;
        return fit(r);
    }
    private Row fit(Row r) {
        if (r.pColsForce > 0) r.pCols = r.pColsForce;
        int nr = (r.pLabels.length + r.pCols - 1) / r.pCols;
        r.h = r.pTop + nr * r.pTh + (nr - 1) * 4 + 8;
        return r;
    }
    /** Tiles with a second line under each name (and an optional fixed column count). */
    private Row picker(String title, String sub, String[] labels, String[] tileSubs, String[] icons, int[] colors, boolean[] wip, int cols, IntSupplier sel, IntConsumer set) {
        Row r = picker(title, sub, labels, icons, colors, wip, sel, set);
        r.pSubs = tileSubs; r.pTh = 32; r.pColsForce = cols;
        return fit(r);
    }
    private Row picker(String title, String sub, String[] labels, String[] icons, IntSupplier sel, IntConsumer set) {
        int[] cols = new int[labels.length]; for (int i = 0; i < cols.length; i++) cols[i] = PICK_PALETTE[i % PICK_PALETTE.length];
        return picker(title, sub, labels, icons, cols, null, sel, set);
    }
    private int[] pickBox(Row r, int i) {
        int cols = r.pCols, gap = 4, tw = (r.w - 16 - gap * (cols - 1)) / cols, th = r.pTh;
        int n = r.pLabels.length, nr = (n + cols - 1) / cols, row = i / cols, last = n - (nr - 1) * cols, ox = 0;
        if (r.pColsForce > 0 && row == nr - 1 && last < cols) ox = (cols - last) * (tw + gap) / 2;          // a lone tile on the last row sits in the middle, in line with the two above
        return new int[]{r.x + 8 + ox + (i % cols) * (tw + gap), r.y + r.pTop + row * (th + gap), tw, th};
    }
    private void drawPicker(GuiGraphicsExtractor g, Row r, int mx, int my) {
        long now = System.currentTimeMillis();
        pill(g, r.x, r.y, r.w, r.h, Ui.ROW);
        g.fill(r.x + 1, r.y + 3, r.x + 3, r.y + r.h - 3, Ui.ACCENT);
        if (!r.title.isEmpty()) {
            Gfx.text(g, font, r.title, r.x + 10, r.y + 6, Ui.TEXT);
            String hint = font.plainSubstrByWidth(r.sub, r.w - 20 - font.width(r.title) - 10);
            Gfx.text(g, font, hint, r.x + r.w - 8 - font.width(hint), r.y + 6, Ui.DIM);
        }
        int sel = Math.min(r.pLabels.length - 1, r.pSel.getAsInt());
        for (int i = 0; i < r.pLabels.length; i++) {
            int[] b = pickBox(r, i); boolean on = r.pOn != null ? r.pOn.test(i) : i == sel, hv = mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3], wip = r.pWip != null && r.pWip[i];
            int col = r.pColors[i];
            boolean lk = r.pLock != null && r.pLock[i];
            Long shk = lk ? lockShake.get(r.pLabels[i]) : null;
            if (shk != null) { long ag = now - shk; if (ag < 420) b[0] += Math.round((float) Math.sin(ag / 24.0) * 4f * (1f - ag / 420f)); }          // a locked tile shakes when you press it
            float k = anim("pick|" + r.title + "|" + r.pLabels[i], on ? 1f : hv ? 0.4f : 0f, 40f);
            if (hv && r.pTips != null) { tipText = r.pTips[i]; tipCol = r.pBg != null ? r.pBg[i] : col; }
            if (hv && r.pIds != null) hoverCos = r.pIds[i];
            int rar = r.pRar == null ? -1 : r.pRar[i];                                                  // rarity: 0 common (grey), 1 beta (green), 2 special (the colours of its set)
            int sch = r.pBg != null ? r.pBg[i] : rar == 1 ? 0xFF3FD16A : rar == 0 ? 0xFF9AA3B5 : col;     // the colour scheme of the tile: every tile of a set shares it
            int bgBase = rar < 0 ? Ui.PANEL2 : Canvas.mix(Ui.PANEL2, sch, rar == 2 ? 0.10f : rar == 1 ? 0.13f : 0.07f);
            pill(g, b[0], b[1], b[2], b[3], Canvas.mix(bgBase, Canvas.mix(Ui.ROW_HOVER, sch, 0.22f), k));
            if (rar == 2) {                                                                              // special: a still gradient in the set's colour, with one light slowly passing over
                g.enableScissor(b[0] + 1, b[1] + 1, b[0] + b[2] - 1, b[1] + b[3] - 1);
                int seg = 8, sw2 = b[2] / seg + 1;
                for (int q = 0; q < seg; q++) g.fill(b[0] + q * b[2] / seg, b[1] + 1, b[0] + q * b[2] / seg + sw2, b[1] + b[3] - 1, Gfx.argb(Math.round(52 * (1f - q / (float) seg) + 6), Canvas.mix(sch, 0xFFFFFFFF, 0.25f * (1f - q / (float) seg)) & 0xFFFFFF));
                int shx = b[0] + (int) ((now / 18 + i * 37L) % (b[2] + 40)) - 20; g.fill(shx, b[1] + 1, shx + 5, b[1] + b[3] - 1, Gfx.argb(hv ? 34 : 18, 0xFFFFFF));
                g.disableScissor();
            }
            if (on) {
                int pulse = r.pOn != null ? Math.round(30 * (0.5f + 0.5f * (float) Math.sin(now / 500.0))) : 0;                    // a worn cosmetic glows softly
                g.outline(b[0], b[1], b[2], b[3], Gfx.argb(200 + pulse, sch & 0xFFFFFF)); g.fill(b[0] + 3, b[1] + b[3] - 3, b[0] + b[2] - 3, b[1] + b[3] - 2, sch);
                if (r.pOn != null) { int cx2 = b[0] + b[2] - 12, cy2 = b[1] + 6; g.fill(cx2 + 4, cy2 + 5, cx2 + 6, cy2 + 7, sch); g.fill(cx2 + 2, cy2 + 3, cx2 + 4, cy2 + 5, sch); g.fill(cx2 + 6, cy2 + 1, cx2 + 8, cy2 + 5, sch); g.fill(cx2 + 7, cy2 - 1, cx2 + 9, cy2 + 1, sch); }
            }
            else g.outline(b[0], b[1], b[2], b[3], Gfx.argb(hv ? 210 : rar == 2 ? 130 : rar >= 0 ? 90 : 50, (rar >= 0 ? sch : col) & 0xFFFFFF));
            int tx = b[0] + 6, iy = b[1] + (b[3] - 16) / 2;
            if (r.pIcons[i] != null) { Icons.draw(g, r.pIcons[i], b[0] + 5, iy, r.pOn != null ? 2400L : on || hv ? now : 2400L); tx = b[0] + 25; }          // cosmetic icons stand still, in the middle
            String nm = font.plainSubstrByWidth(r.pLabels[i], b[2] - (tx - b[0]) - 4 - (wip ? 22 : 0));
            if (r.pSubs != null) {
                if (r.pFx != null && r.pFx[i] != null) { int lx = tx; for (int ci = 0; ci < nm.length(); ci++) { String chs = String.valueOf(nm.charAt(ci)); Gfx.text(g, font, chs, lx, b[1] + 7, 0xFF000000 | NameFx.color(r.pFx[i], ci, nm.length(), col & 0xFFFFFF, lk ? 2400L : now)); lx += font.width(chs); } }
                else Gfx.text(g, font, nm, tx, b[1] + 7, on ? Ui.TEXT : wip ? Ui.DIM : Canvas.mix(Ui.SUB, Ui.TEXT, 0.5f));
                Gfx.text(g, font, font.plainSubstrByWidth(r.pSubs[i], b[2] - (tx - b[0]) - 6), tx, b[1] + 18, on ? Canvas.mix(Ui.SUB, col, 0.45f) : Ui.DIM);
            } else if (r.pFx != null && r.pFx[i] != null) {                                           // a name effect: the name itself, wearing the effect
                int lx = tx; for (int ci = 0; ci < nm.length(); ci++) { String chs = String.valueOf(nm.charAt(ci)); Gfx.text(g, font, chs, lx, b[1] + (b[3] - 8) / 2 - 1, 0xFF000000 | NameFx.color(r.pFx[i], ci, nm.length(), col & 0xFFFFFF, lk ? 2400L : now)); lx += font.width(chs); }
            } else Gfx.text(g, font, nm, tx, b[1] + (b[3] - 8) / 2 - 1, on ? Ui.TEXT : wip ? Ui.DIM : Ui.SUB);
            if (wip) { pill(g, b[0] + b[2] - 22, b[1] + 5, 18, 10, 0xFF3A3220); Gfx.text(g, font, "WIP", b[0] + b[2] - 20, b[1] + 6, 0xFFFFC857); }
            if (lk) {                                                                                  // locked: dimmed, with a padlock
                g.fill(b[0] + 1, b[1] + 1, b[0] + b[2] - 1, b[1] + b[3] - 1, 0x99080A10);
                int lx = b[0] + b[2] - 16, ly = b[1] + 5, lc = shk != null && now - shk < 420 ? 0xFFFF6B6B : 0xFFB9C0D6;
                g.fill(lx + 2, ly, lx + 3, ly + 4, lc); g.fill(lx + 7, ly, lx + 8, ly + 4, lc); g.fill(lx + 2, ly, lx + 8, ly + 1, lc);
                g.fill(lx, ly + 4, lx + 10, ly + 10, lc); g.fill(lx + 4, ly + 6, lx + 6, ly + 8, 0xFF1A1C26);
            }
        }
    }
    private static final Map<String, Long> lockShake = new HashMap<>();
    private static String tipText, hoverCos; private static int tipCol;                      // the hover card of the tile under the mouse (drawn last, on top of everything)
    private static final int[] RAR_COL = {0xFF9AA3B5, 0xFF3FD16A, 0xFFC77DFF};
    /** The little rarity pin: a tiny gem on a pin, in the colour of the rarity. */
    private static void rarityPin(GuiGraphicsExtractor g, int x, int y, int rar) {
        int c = RAR_COL[Math.max(0, Math.min(2, rar))], lt = Canvas.mix(c, 0xFFFFFFFF, 0.55f), dk = Canvas.mix(c, 0xFF000000, 0.45f);
        g.fill(x + 2, y, x + 5, y + 1, lt); g.fill(x + 1, y + 1, x + 6, y + 3, c); g.fill(x, y + 3, x + 7, y + 4, c);
        g.fill(x + 1, y + 4, x + 6, y + 5, dk); g.fill(x + 2, y + 5, x + 5, y + 6, dk); g.fill(x + 3, y + 6, x + 4, y + 7, dk);
        g.fill(x + 2, y + 1, x + 3, y + 2, 0xFFFFFFFF);
        if (rar == 2) { g.fill(x + 6, y - 1, x + 7, y, 0xFFFFFFFF); g.fill(x - 1, y + 5, x, y + 6, lt); }
    }
    /** The hover card. tipText lines: name | what it is | rarity (0, 1, 2) | 1 yours / 0 locked | extra line (may be empty) | how to get it. */
    private void drawTileTip(GuiGraphicsExtractor g, int mx, int my) {
        String t = tipText; tipText = null;
        if (t == null || t.isEmpty()) return;
        String[] ln = t.split("\n", -1); if (ln.length < 6) return;
        int rar = 0; try { rar = Integer.parseInt(ln[2]); } catch (NumberFormatException ignored) { }
        boolean yours = "1".equals(ln[3]); int rc = RAR_COL[Math.max(0, Math.min(2, rar))];
        List<String> how = new ArrayList<>(); String cur = "";
        for (String wd : ln[5].split(" ")) { if (!cur.isEmpty() && font.width(cur + " " + wd) > 150) { how.add(cur); cur = wd; } else cur = cur.isEmpty() ? wd : cur + " " + wd; }
        if (!cur.isEmpty()) how.add(cur);
        int w = Math.max(Math.max(font.width(ln[0]) + font.width(ln[1]) + 10, 80 + 30), 150); w += 16;
        int h = 8 + 12 + 12 + (ln[4].isEmpty() ? 0 : 11) + 3 + how.size() * 10 + 2;
        int x = Math.min(mx + 10, width - w - 2), y = Math.min(my + 12, height - h - 2);
        pill(g, x, y, w, h, 0xF4101420); g.outline(x, y, w, h, Gfx.argb(200, rc & 0xFFFFFF)); g.fill(x, y + 2, x + 2, y + h - 2, tipCol);
        Gfx.text(g, font, ln[0], x + 8, y + 5, Canvas.mix(tipCol, 0xFFFFFFFF, 0.5f)); Gfx.text(g, font, ln[1], x + 8 + font.width(ln[0]) + 6, y + 5, Ui.DIM);
        int yy = y + 17; rarityPin(g, x + 8, yy + 1, rar); Gfx.text(g, font, CosmeticData.rarityName(rar), x + 20, yy, Canvas.mix(rc, 0xFFFFFFFF, 0.25f));
        String st = yours ? "Unlocked" : "Locked"; Gfx.text(g, font, st, x + w - 8 - font.width(st), yy, yours ? 0xFF62E6A8 : 0xFFFF7B7B);
        yy += 12; if (!ln[4].isEmpty()) { Gfx.text(g, font, ln[4], x + 8, yy, Ui.SUB); yy += 11; }
        g.fill(x + 8, yy, x + w - 8, yy + 1, 0x22FFFFFF); yy += 4;
        for (String hl : how) { Gfx.text(g, font, hl, x + 8, yy, Ui.DIM); yy += 10; }
    }

    /** A category banner: a gradient with a light sweeping over it, the name in shimmering letters, a line under it and a counter on the right. */
    private void drawCat(GuiGraphicsExtractor g, Row r) {
        long now = System.currentTimeMillis(); int col = r.accent;
        pill(g, r.x, r.y, r.w, r.h, Canvas.mix(Ui.ROW, col, 0.12f));
        g.enableScissor(r.x, r.y, r.x + r.w, r.y + r.h);
        for (int i = 0; i < r.w; i += 2) {                                                              // colour fading out to the right
            float u = i / (float) r.w; g.fill(r.x + i, r.y, r.x + i + 2, r.y + r.h, Gfx.argb(Math.round(70 * (1f - u) * (1f - u)), col & 0xFFFFFF));
        }
        int sw = Math.round(((now % 3200L) / 3200f) * (r.w + 60)) - 30;                                  // a slanted light sweeping across
        for (int j = 0; j < r.h; j++) g.fill(r.x + sw - j / 2, r.y + j, r.x + sw - j / 2 + 10, r.y + j + 1, 0x14FFFFFF);
        g.disableScissor();
        g.fill(r.x, r.y + 2, r.x + 3, r.y + r.h - 2, col);
        int cx = r.x + 12;
        for (int i = 0; i < r.title.length(); i++) {
            String ch = String.valueOf(r.title.charAt(i));
            float sh = 0.5f + 0.5f * (float) Math.sin(now / 420.0 - i * 0.45);
            Gfx.text(g, font, ch, cx, r.y + 6 - Math.round(sh), Canvas.mix(col, 0xFFFFFFFF, 0.25f + 0.65f * sh));
            cx += font.width(ch) + 1;
        }
        for (int k = 0; k < 3; k++) {                                                                    // little stars twinkling beside the name
            float tw2 = 0.5f + 0.5f * (float) Math.sin(now / 300.0 + k * 2.1); int sx = cx + 6 + k * 9, sy = r.y + 7 + (k % 2) * 3;
            g.fill(sx, sy, sx + 2, sy + 2, Gfx.argb(Math.round(90 + 150 * tw2), 0xFFFFFF));
        }
        Gfx.text(g, font, r.sub, r.x + 12, r.y + 20, Ui.SUB);
        String badge = r.pSubs != null && r.pSubs.length > 0 ? r.pSubs[0] : "";
        if (!badge.isEmpty()) { int bw = font.width(badge) + 10; pill(g, r.x + r.w - bw - 8, r.y + 8, bw, 16, Canvas.mix(Ui.PANEL2, col, 0.25f)); g.outline(r.x + r.w - bw - 8, r.y + 8, bw, 16, Gfx.argb(160, col & 0xFFFFFF)); Gfx.text(g, font, badge, r.x + r.w - bw - 3, r.y + 12, Canvas.mix(col, 0xFFFFFFFF, 0.6f)); }
    }
    private Row catHeader(String title, String sub, String badge, int col) {
        Row r = new Row(); r.kind = CAT; r.noSearch = true; r.title = title; r.sub = sub; r.accent = col; r.h = 34; r.pSubs = new String[]{badge}; return r;
    }


    // ------------------------------------------------------------------ the Cosmetics page (Special / Beta / Earned)
    /** The previews get their own menus round the window (which stays in the middle) when the screen has the room for them. */
    private boolean cosPanels() { return tab == Tab.COSMETICS && query.isBlank() && height - 2 * (COS_H + DOCK_GAP + 6) >= 210 && width - 2 * (AURA_W + DOCK_GAP + 6) >= 430; }

    private void clickDef(CosmeticData.Def d, String label) {
        if (CosmeticData.editMode) {                                                           // test menu: add or remove it on yourself
            boolean had = CosmeticData.unlocked(d); CosmeticData.force.put(d.id(), !had); CosmeticData.changed();
            toast((had ? "Removed " : "Added ") + d.name() + "\n(test only, nothing is saved)", had ? 0xFFFF8A5A : 0xFF62E6A8); return;
        }
        if (!CosmeticData.unlocked(d)) { lockShake.put(label, System.currentTimeMillis()); lockedToast(d); }
        else CosmeticData.toggle(d);
    }
    /** The colour scheme every tile of a set shares (beta green, common grey for the rest). */
    private static int schemeOf(CosmeticData.Def d) {
        CosmeticData.SetDef sd = d.set() == null ? null : CosmeticData.setById(d.set());
        return sd != null ? sd.color() : d.line() == CosmeticData.Line.BETA ? 0xFF3FD16A : 0xFF9AA3B5;
    }
    private static String tipLines(String name, String what, int rar, boolean yours, String extra, String how) {
        return name + "\n" + what + "\n" + rar + "\n" + (yours ? "1" : "0") + "\n" + extra + "\n" + how;
    }

    /** One grid of tiles: sets (SetDef) or single cosmetics (Def). */
    private Row grid(List<Object> items, int cols) {
        int n = items.size();
        String[] lb = new String[n], sb = new String[n], ic = new String[n], tp = new String[n], fxs = new String[n], ids = new String[n]; int[] cl = new int[n], rar = new int[n], bg = new int[n]; boolean[] lk = new boolean[n], eq = new boolean[n]; boolean sets = false;
        for (int k = 0; k < n; k++) {
            Object it = items.get(k);
            if (it instanceof CosmeticData.SetDef sd) {
                sets = true;
                List<CosmeticData.Def> ms = CosmeticData.members(sd);
                boolean owned = CosmeticData.unlocked(ms.get(0)), wearing = CosmeticData.wearingSet(sd);
                lb[k] = sd.name(); sb[k] = !owned ? "Locked" : wearing ? "Wearing" : "Wear set"; ic[k] = "fd:suf_" + sd.suffix(); cl[k] = sd.color(); bg[k] = sd.color(); rar[k] = sd.rarity(); lk[k] = !owned; eq[k] = owned && wearing; fxs[k] = sd.fx();
                tp[k] = tipLines(sd.name(), "Set", sd.rarity(), owned, "Items " + CosmeticData.owned(sd) + " / " + ms.size(), sd.how());
            } else {
                CosmeticData.Def d = (CosmeticData.Def) it; boolean open2 = CosmeticData.unlocked(d);
                lb[k] = !open2 && d.id().equals("secret") ? "???" : d.name(); cl[k] = d.color(); bg[k] = schemeOf(d); rar[k] = CosmeticData.rarityOf(d); lk[k] = !open2; eq[k] = open2 && CosmeticData.isEquipped(d); ids[k] = d.id();
                ic[k] = d.kind() == CosmeticData.Kind.NAME ? null : d.kind() == CosmeticData.Kind.SUFFIX ? "fd:suf_" + d.id() : "fd:cx_" + d.id();
                fxs[k] = d.kind() == CosmeticData.Kind.NAME ? d.id() : null;
                CosmeticData.SetDef sd = d.set() == null ? null : CosmeticData.setById(d.set());
                boolean hid = !open2 && d.id().equals("secret");
                tp[k] = tipLines(hid ? "???" : d.name(), CosmeticData.kindLabel(d.kind()), rar[k], open2, sd != null ? sd.name() + " set" : "", open2 && d.line() != CosmeticData.Line.SPECIAL ? d.how() : CosmeticData.lockText(d));
            }
        }
        Row pk = sets ? picker("", "", lb, sb, ic, cl, null, cols, () -> -1, i -> {
            CosmeticData.SetDef sd = (CosmeticData.SetDef) items.get(i);
            List<CosmeticData.Def> ds = CosmeticData.members(sd); boolean owned = CosmeticData.unlocked(ds.get(0));
            if (CosmeticData.editMode) { for (CosmeticData.Def d : ds) CosmeticData.force.put(d.id(), !owned); CosmeticData.changed(); toast((owned ? "Removed " : "Added ") + sd.name() + " set\n(test only, nothing is saved)", owned ? 0xFFFF8A5A : 0xFF62E6A8); return; }
            if (!owned) { lockShake.put(lb[i], System.currentTimeMillis()); toast("LOCKED!\n" + sd.how(), 0xFFFF5A5A); toastShake = true; } else CosmeticData.toggleSet(sd);
        }) : picker("", "", lb, ic, cl, null, () -> -1, i -> clickDef((CosmeticData.Def) items.get(i), lb[i]));
        pk.pCols = cols; pk.pColsForce = 0; pk.pTop = 6; fit(pk);
        pk.pOn = i -> eq[i]; pk.pLock = lk; pk.pTips = tp; pk.pFx = fxs; pk.pIds = ids; pk.pRar = rar; pk.pBg = bg;
        return pk;
    }

    private void cosmeticsPage() {
        if (!cosPanels() && query.isBlank()) { Row cp = new Row(); cp.kind = COSPREV; cp.noSearch = true; cp.h = 150; rows.add(cp); }          // small screens keep the previews inside the page
        CosmeticData.Kind[] kinds = {CosmeticData.Kind.SUFFIX, CosmeticData.Kind.NAME, CosmeticData.Kind.AURA};
        String[] heads = {"SUFFIX", "NAME EFFECT", "AURA"}, subs = {"A little icon after your name.", "Your name in moving colours.", "Particles around your character."};
        int[] accents = {0xFFFFC857, 0xFFFF9AC8, 0xFF8FD0FF};
        // --- sets: the rarest first
        int ownedSets = 0; for (CosmeticData.SetDef sd : CosmeticData.SETS) if (CosmeticData.unlocked(CosmeticData.members(sd).get(0))) ownedSets++;
        rows.add(catHeader("SETS", "A suffix, a name effect and an aura that belong together.", ownedSets + " / " + CosmeticData.SETS.size(), 0xFFC77DFF));
        List<Object> st = new ArrayList<>(); for (int r2 = 2; r2 >= 0; r2--) for (CosmeticData.SetDef sd : CosmeticData.SETS) if (sd.rarity() == r2) st.add(sd);
        rows.add(grid(st, 3));
        // --- one section per kind, ordered by rarity (special, then beta, then common); a set's items keep the set's colours
        for (int ki = 0; ki < kinds.length; ki++) {
            List<Object> li = new ArrayList<>(); int have = 0;
            for (int r2 = 2; r2 >= 0; r2--) for (CosmeticData.SetDef sd : CosmeticData.SETS) if (sd.rarity() == r2)
                for (CosmeticData.Def d : CosmeticData.members(sd)) if (d.kind() == kinds[ki]) li.add(d);
            for (int r2 = 1; r2 >= 0; r2--) for (CosmeticData.Def d : CosmeticData.ALL) if (d.kind() == kinds[ki] && d.set() == null && CosmeticData.rarityOf(d) == r2) li.add(d);
            for (Object o : li) if (CosmeticData.unlocked((CosmeticData.Def) o)) have++;
            rows.add(catHeader(heads[ki], subs[ki], have + " / " + li.size(), accents[ki]));
            rows.add(grid(li, 4));
        }
        rows.add(note("Click to wear, click again to take off. Hover a tile to see its rarity and how to get it.", Ui.DIM));
        Row rf = row(0, "fd:sparkle", "Cosmetics list", Cosmetics.refreshNote(), Ui.ACCENT); rf.noSearch = true;
        rf.ctls.add(new Btn("Refresh", 0xFF2F8F5A, () -> { Cosmetics.forceRefresh(); build(); }));
        rf.ctls.add(new Btn("Copy ID", 0xFF3A4FA0, () -> { try { net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(Cosmetics.code(Cosmetics.me())); } catch (Throwable ignored) { } }));
        rows.add(rf);
    }
    private void lockedToast(CosmeticData.Def d) {
        toast("LOCKED!\n" + CosmeticData.lockText(d), 0xFFFF5A5A);
        toastShake = true;
    }
    private static boolean toastShake;

    // ------------------------------------------------------------------ the small test menu under the main menu (remove once the new page is tested)
    private static final boolean TEST_STRIP = true;
    private int[] stripBox(int i) {
        String[] lbl = stripLabels(); int x = px + 30, w;
        for (int k = 0; k < i; k++) x += font.width(lbl[k]) + 14;
        w = font.width(lbl[i]) + 10;
        return new int[]{x, py + ph + 4, x + w, py + ph + 18};
    }
    private String[] stripLabels() {
        return new String[]{"Add / remove mine: " + (CosmeticData.editMode ? "ON" : "OFF"), "Give me all", "Remove all from me", "Reset test + secret + drops"};
    }
    // ------------------------------------------------------------------ the two Cosmetics side menus: chat + tab list above the window, the aura preview (your skin) to its left
    private static float figYaw = 0.5f;
    private boolean dragFig;
    private final int[] auraBox = {-1, -1, -1, -1};
    private void drawCosPanels(GuiGraphicsExtractor g, int mx, int my, float intro) {
        auraBox[0] = auraBox[1] = auraBox[2] = auraBox[3] = -1;
        if (cosK < 0.97f) { hoverCos = null; return; }
        long now = System.currentTimeMillis();
        Row tr = new Row(); tr.x = px; tr.y = py - COS_H - DOCK_GAP; tr.w = pw; tr.h = COS_H;
        drawCosPreview(g, tr);
        int x = px - AURA_W - DOCK_GAP, y = py, w = AURA_W, h = ph;
        pill(g, x, y, w, h, Ui.PANEL); g.outline(x, y, w, h, Ui.BORDER);
        Gfx.text(g, font, "Aura preview", x + 8, y + 6, Ui.TEXT);
        String aura = null; CosmeticData.Def hd = hoverCos == null ? null : CosmeticData.byId(hoverCos); hoverCos = null;
        FancyConfig c = FancyConfig.get();
        if (hd != null && hd.kind() == CosmeticData.Kind.AURA) aura = hd.id(); else if (CosmeticData.unlockedId(c.aura)) aura = c.aura;
        CosmeticData.Def ad = aura == null ? null : CosmeticData.byId(aura);
        Gfx.text(g, font, ad == null ? "No aura worn" : ad.name(), x + 8, y + 17, ad == null ? Ui.DIM : Canvas.mix(Ui.SUB, ad.color(), 0.6f));
        int fx = x + w / 2, feet = y + h - 22; float ppb = SkinMock.height() / 1.8f;
        auraBox[0] = x; auraBox[1] = y + 28; auraBox[2] = x + w; auraBox[3] = y + h;
        g.enableScissor(x + 1, y + 28, x + w - 1, y + h - 1);
        g.fill(fx - 13, feet + 1, fx + 13, feet + 3, 0x40000000);
        List<AuraFx.P> parts = ad == null ? List.of() : AuraFx.particles(aura, now);
        for (int pass = 0; pass < 2; pass++) {
            if (pass == 1) SkinMock.draw(g, fx, feet, figYaw);
            for (AuraFx.P q : parts) if ((q.z() < 0) == (pass == 0)) AuraFx.draw(g, fx + q.x() * ppb, feet - q.y() * ppb, Math.max(1f, q.size() * ppb * 1.3f), q);
        }
        g.disableScissor();
        Gfx.textCentered(g, font, "Drag to turn", x + w / 2, y + h - 11, Ui.DIM);
        if (intro < 1f) { int al = Math.round((1f - intro) * 150); pill(g, tr.x, tr.y, tr.w, tr.h, Gfx.argb(al, 0x0C0E14)); pill(g, x, y, w, h, Gfx.argb(al, 0x0C0E14)); }
    }
    private boolean inAuraPanel(double mx, double my) { return auraBox[0] >= 0 && mx >= auraBox[0] && mx < auraBox[2] && my >= auraBox[1] && my < auraBox[3]; }

    private void drawStrip(GuiGraphicsExtractor g, int mx, int my) {
        if (!TEST_STRIP) return;
        String[] lbl = stripLabels(); boolean[] on = {CosmeticData.editMode, false, false, false};
        Gfx.text(g, font, "TEST", px + 2, py + ph + 8, 0xFF6B7390);
        for (int i = 0; i < lbl.length; i++) {
            int[] b = stripBox(i);
            boolean hv = mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3];
            pill(g, b[0], b[1], b[2] - b[0], b[3] - b[1], on[i] ? 0xFF2A5A44 : hv ? 0xFF2A2F44 : 0xFF1A1E2E);
            g.outline(b[0], b[1], b[2] - b[0], b[3] - b[1], on[i] ? 0xFF62E6A8 : 0x55FFFFFF);
            Gfx.text(g, font, lbl[i], b[0] + 5, b[1] + 3, on[i] ? 0xFFB8FFD9 : Ui.SUB);
        }
    }
    private boolean stripClick(double mx, double my) {
        if (!TEST_STRIP) return false;
        for (int i = 0; i < 4; i++) {
            int[] b = stripBox(i);
            if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
                FancyConfig c = FancyConfig.get();
                switch (i) {
                    case 0 -> { CosmeticData.editMode = !CosmeticData.editMode; toast(CosmeticData.editMode ? "Test mode ON\nClick a tile to add or remove it." : "Test mode OFF", 0xFFFFB86B); }
                    case 1 -> { for (CosmeticData.Def d : CosmeticData.ALL) CosmeticData.force.put(d.id(), true); CosmeticData.changed(); toast("Every cosmetic added (test only).", 0xFF62E6A8); }
                    case 2 -> { for (CosmeticData.Def d : CosmeticData.ALL) CosmeticData.force.put(d.id(), false); CosmeticData.changed(); toast("Every cosmetic removed (test only).", 0xFFFF8A5A); }
                    default -> { CosmeticData.force.clear(); CosmeticData.editMode = false; CosmeticData.changed(); c.secretFound = false; c.discordJoined = false; c.dropsSeen = 0; FancyConfig.save(); toast("Test changes, secret, Discord and drop counter reset.", 0xFFFFB86B); }
                }
                return true;
            }
        }
        return false;
    }

    // ---- Chat > the typing preview
    private static String prevKey = ""; private static long prevAt;
    private void drawChatPrev(GuiGraphicsExtractor g, Row r) {
        FancyConfig c = FancyConfig.get();
        String user = net.minecraft.client.Minecraft.getInstance().getUser().getName();
        String msg = r.sub, key = r.title + "|" + msg;
        long now = System.currentTimeMillis();
        if (!key.equals(prevKey)) { prevKey = key; prevAt = now; }
        g.fill(r.x, r.y, r.x + r.w, r.y + r.h, 0xB0000000);
        g.outline(r.x, r.y, r.w, r.h, 0x33FFFFFF);
        // the prefix, one colour per piece: Hypixel's own look
        String chan = c.channel;
        java.util.List<Object[]> parts = new ArrayList<>();
        if (chan.equals("PARTY")) { parts.add(new Object[]{"Party > ", 0xFF5555FF}); }
        else if (chan.equals("COOP")) { parts.add(new Object[]{"Co-op > ", 0xFF55FFFF}); }
        parts.add(new Object[]{"[MVP", 0xFF55FFFF}); parts.add(new Object[]{"+", 0xFFFF5555}); parts.add(new Object[]{"] " + user, 0xFF55FFFF}); parts.add(new Object[]{": ", 0xFFFFFFFF});
        StringBuilder sb = new StringBuilder(); java.util.List<Integer> cols = new ArrayList<>();
        for (Object[] o : parts) { String t = (String) o[0]; for (int i = 0; i < t.length(); i++) { sb.append(t.charAt(i)); cols.add((Integer) o[1]); } }
        int preLen = sb.length();
        sb.append(msg); for (int i = 0; i < msg.length(); i++) cols.add(0xFFFFFFFF);
        String full = sb.toString();
        double cps = 26, typeSecs = msg.length() / cps, cycle = typeSecs + 2.8;
        double t = ((now - prevAt) / 1000.0) % cycle;
        int shown = preLen + (int) Math.min(msg.length(), t * cps);
        boolean typing = t < typeSecs;
        // wrap on spaces
        List<int[]> lines = new ArrayList<>(); int ls = 0, lastSp = -1, maxW = r.w - 16;
        for (int i = 0; i < full.length(); i++) {
            if (full.charAt(i) == ' ') lastSp = i;
            if (font.width(full.substring(ls, i + 1)) > maxW) {
                int cut = lastSp > ls ? lastSp + 1 : i; lines.add(new int[]{ls, cut}); ls = cut; lastSp = -1;
            }
        }
        lines.add(new int[]{ls, full.length()});
        int y = r.y + 6;
        for (int[] ln : lines) {
            int x = r.x + 8, i = ln[0];
            while (i < ln[1] && i < shown) {
                int j = i; int col = cols.get(i);
                while (j < ln[1] && j < shown && cols.get(j) == col) j++;
                String seg = full.substring(i, j); Gfx.text(g, font, seg, x, y, col); x += font.width(seg); i = j;
            }
            if (shown >= ln[0] && shown <= ln[1] && (shown < full.length() || ln == lines.get(lines.size() - 1)) && (now / 450) % 2 == 0 && (typing || t < typeSecs + 1.2)) {
                if (shown < ln[1] || ln == lines.get(lines.size() - 1)) g.fill(x + 1, y, x + 2, y + 8, 0xFFFFFFFF);
            }
            y += 11;
        }
    }
    private static final String[] COMM = {"discord", "patreon", "github"}, COMM_NAME = {"Discord", "Patreon", "GitHub"};
    private int[] commBox(int i) { int x = Math.min(px + pw + 6, width - 28), y = py + 12 + i * 28; return new int[]{x, y, x + 22, y + 22}; }
    /** The community icons: a narrow column on the right of the menu. */
    private void drawCommunity(GuiGraphicsExtractor g, int mx, int my) {
        String tipText = null; int tipX = 0, tipY = 0;
        for (int i = 0; i < 3; i++) {
            int[] b = commBox(i); boolean hv = mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3];
            float k = anim("comm" + i, hv ? 1f : 0f, 40f);
            pill(g, b[0], b[1], 22, 22, Canvas.mix(Ui.ROW, Ui.ROW_HOVER, k));
            Icons.draw(g, "fd:" + COMM[i], b[0] + 3, b[1] + 3 - Math.round(k), System.currentTimeMillis());
            if (hv) { tipText = COMM_NAME[i]; tipX = b[0]; tipY = b[3] + 3; }
        }
        if (tipText != null) { int w = font.width(tipText) + 8; int x0 = Math.max(px + 2, tipX + 22 - w); pill(g, x0, tipY, w, 13, 0xFF0D0F16); g.outline(x0, tipY, w, 13, Ui.BORDER); Gfx.text(g, font, tipText, x0 + 4, tipY + 3, Ui.TEXT); }
    }

    /** A little world for the previews to sit in: dusk sky, hills, trees. With background blur on it is soft and hazy; off, it is sharp and detailed. */
    private void drawBackdrop(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean blur) {
        int hor = y + h * 56 / 100;
        for (int j = 0; j < hor - y; j += 2) {
            float f = j / (float) Math.max(1, hor - y);
            int c = f < 0.5f ? Canvas.mix(0xFF141C3A, 0xFF5B4B88, f * 2f) : Canvas.mix(0xFF5B4B88, 0xFFE58F62, (f - 0.5f) * 2f);
            g.fill(x, y + j, x + w, Math.min(hor, y + j + 2), c);
        }
        if (!blur) for (int i = 0; i < 26; i++) { int sx = x + (i * 97 + 13) % Math.max(1, w), sy = y + (i * 53 + 7) % Math.max(1, (hor - y) / 2); g.fill(sx, sy, sx + 1, sy + 1, 0xCCFFFFFF); }
        int sunX = x + w * 72 / 100, sunY = hor - h / 14, sr = Math.max(4, h / 9);
        for (int j = -sr; j <= sr; j++) { int half = (int) Math.sqrt(Math.max(0, sr * sr - j * j)); if (sunY + j < hor) g.fill(sunX - half, sunY + j, sunX + half, sunY + j + 1, blur ? 0x66FFD9A0 : 0xFFFFD9A0); }
        for (int i = 0; i < w; i++) {                                                                  // two ranges of hills
            int h1 = (int) (h * 0.10 + Math.sin(i * 0.045 + 1.0) * h * 0.05 + Math.sin(i * 0.11) * h * 0.02);
            g.fill(x + i, hor - h1, x + i + 1, hor, blur ? 0xFF3A3461 : 0xFF2C2850);
            int h2 = (int) (h * 0.05 + Math.sin(i * 0.07 + 3.0) * h * 0.03);
            g.fill(x + i, hor - h2, x + i + 1, hor + 1, blur ? 0xFF29433A : 0xFF1D3A2E);
        }
        for (int j = hor; j < y + h; j += 2) g.fill(x, j, x + w, Math.min(y + h, j + 2), Canvas.mix(0xFF2A5230, 0xFF14281A, (j - hor) / (float) Math.max(1, y + h - hor)));
        if (!blur) {
            for (int i = 0; i < 7; i++) {                                                              // pine trees
                int tx = x + (i * 61 + 20) % Math.max(1, w - 10) + 4, ty = hor + 1 + (i % 3) * (h / 14), th = Math.max(8, h / 6);
                g.fill(tx, ty - 1, tx + 2, ty + 2, 0xFF3B2A1A);
                for (int k = 0; k < 4; k++) g.fill(tx - 3 + k, ty - 2 - k * th / 5, tx + 5 - k, ty - 1 - k * th / 5, k % 2 == 0 ? 0xFF1E4A2C : 0xFF245A34);
            }
            for (int i = 0; i < 24; i++) { int gx = x + (i * 37 + 5) % Math.max(1, w), gy = hor + 3 + (i * 19) % Math.max(1, y + h - hor - 4); g.fill(gx, gy, gx + 1, gy + 2, 0x663E7A44); }
            g.fill(x, hor, x + w, hor + 1, 0x40FFFFFF);
        } else {
            g.fill(x, y, x + w, y + h, 0x22C8D6FF);                                                    // haze: the game behind the scene is blurred
            g.fill(x, hor - h / 12, x + w, hor + h / 12, 0x1AFFFFFF);
        }
    }

    private int previewH(int bw) { return Math.round(bw * height / (float) Math.max(1, width)); }

    private static boolean isDyeDrop(SlayerData.Drop d) { return d.name.toLowerCase(Locale.ROOT).endsWith(" dye"); }
    private static List<SlayerData.Drop> nonDye(SlayerData.Slayer s) { List<SlayerData.Drop> l = new ArrayList<>(); for (SlayerData.Drop d : s.drops) if (!isDyeDrop(d)) l.add(d); return l; }
    /** The four sound slots (chop, split, reveal, end) of one animation, as dropdown rows. */
    private void soundRows(int sceneIdx, FancyConfig c) {
        final String scene = SoundFx.SCENES[sceneIdx];
        List<String> choices = SoundFx.choices();
        int col = sceneIdx == 3 ? Ui.DIM : sceneIdx == 0 ? Ui.SUB : sceneIdx == 1 ? Ui.R_CRAZY : (sceneIdx == 5 || sceneIdx == 8) ? 0xFF7BE05A : (sceneIdx == 6 || sceneIdx == 9) ? 0xFFFFC857 : sceneIdx == 7 ? 0xFFB56BFF : Ui.ACCENT;
        for (int k = 0; k < SoundFx.SLOTS.length; k++) {
            final String slot = SoundFx.SLOTS[k];
            Row sr = row(1, "fd:slot_" + slot + (k <= 1 ? (sceneIdx == 4 ? "_dye" : (sceneIdx == 5 || sceneIdx == 8) ? "_farm" : (sceneIdx == 6 || sceneIdx == 9) ? "_myth" : sceneIdx == 7 ? "_exp" : "") : ""), sceneIdx == 7 ? EXP_SLOT_NAMES[k] : (sceneIdx == 6 || sceneIdx == 9) ? MYTH_SLOT_NAMES[k] : (sceneIdx == 5 || sceneIdx == 8) ? FARM_SLOT_NAMES[k] : sceneIdx == 4 ? DYE_SLOT_NAMES[k] : SLOT_NAMES[k], sceneIdx == 7 ? EXP_SLOT_SUBS[k] : (sceneIdx == 6 || sceneIdx == 9) ? MYTH_SLOT_SUBS[k] : (sceneIdx == 5 || sceneIdx == 8) ? FARM_SLOT_SUBS[k] : sceneIdx == 4 ? DYE_SLOT_SUBS[k] : SLOT_SUBS[k], col);
            final int slotIdx = k;
            sr.ctls.add(new Btn("▶", GREEN, () -> { pulseAt[slotIdx] = System.currentTimeMillis(); SoundFx.Fx fx = SoundFx.byName(SoundFx.chosen(scene, slot)); if (fx != null) SoundFx.preview(fx); }));
            final String ddKey = "dd_" + scene + "_" + slot;
            Dd sdd = new Dd(() -> SoundFx.label(SoundFx.chosen(scene, slot)), ddKey, 128);
            sr.ctls.add(sdd);
            rows.add(sr);
            if (!open.contains(ddKey) || searching) continue;
            List<String> labels = new ArrayList<>();
            for (String ch : choices) labels.add(SoundFx.label(ch));
            rows.add(miniList(sr, sdd, labels, choices.indexOf(SoundFx.chosen(scene, slot)), pi -> {
                String ch = choices.get(pi);
                SoundFx.choose(scene, slot, ch); open.remove(ddKey);
                SoundFx.Fx fx = SoundFx.byName(ch); if (fx != null) SoundFx.preview(fx);
            }));
        }
    }

    /** Every sidebar icon moves a little: a twinkling star, a rocket with sparks, a note floating off the note block, typing dots, a spinning gear, rising bars. */
    /** Every sidebar icon is its own little animation (see Icons). */
    /** While searching the sidebar lists only the pages that have a match, each with the sub pages that match (all of them when the page's name matches); a click opens that page and clears the search. */
    private void drawSearchSide(GuiGraphicsExtractor g, int mx, int my, long now) {
        sideScroll = Math.max(0, Math.min(sideMax(), sideScroll));
        searchSide.clear();
        g.enableScissor(px + 4, py + 66, px + sw, py + ph - 4);
        String q = query.trim().toLowerCase(Locale.ROOT);
        int y0 = py + 70 - sideScroll, y = y0, rowH = Math.max(12, Math.min(SUBH, 15));
        boolean any = false;
        for (Tab t : Tab.values()) {
            int ti = t.ordinal();
            if (!searchVis[ti]) continue;
            any = true;
            int[] b = {px + 8, y, px + sw - 6, y + 22};
            boolean hov = inSide(mx, my) && mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3], hit = t.label.toLowerCase(Locale.ROOT).contains(q);
            float hv = anim("stab|" + ti, hov ? 1f : 0f, 48f);
            if (hv > 0.01f) pill(g, b[0], b[1], b[2] - b[0], b[3] - b[1], Canvas.mix(Ui.PANEL2, 0xFF1D1E27, hv));
            iconBack(g, b[0] + 14, b[1] + 11, 9, hit || hov, tabCol(t));
            tabIcon(g, t, b[0] + 6, b[1] + 3, hit || hov ? now : 2400L, hit || hov);
            Gfx.text(g, font, t.label, b[0] + 28, b[1] + 7, hit ? Canvas.mix(tabCol(t), 0xFFFFFFFF, 0.35f) : Canvas.mix(Canvas.mix(0xFF8A8A96, tabCol(t), 0.45f), 0xFFFFFFFF, hv * 0.4f));
            searchSide.add(new int[]{ti, -1, b[0], b[1], b[2], b[3]});
            y += 24;
            String[] sn = SUBS[ti];
            for (int k = 0; k < sn.length; k++) {
                if (!hit && !sn[k].toLowerCase(Locale.ROOT).contains(q)) continue;      // only the sub pages that match (all of them when the page's own name matches)
                int[] sb = {px + 16, y, px + sw - 6, y + rowH};
                boolean sh = inSide(mx, my) && mx >= sb[0] && mx < sb[2] && my >= sb[1] && my < sb[3], sHit = sn[k].toLowerCase(Locale.ROOT).contains(q);
                float shv = anim("ssub|" + ti + "|" + k, sh ? 1f : 0f, 48f);
                int sc2 = subCol(t);
                if (sHit) { pill(g, sb[0], sb[1], sb[2] - sb[0], sb[3] - sb[1], Canvas.mix(Ui.ROW, sc2, 0.22f)); g.fill(sb[0], sb[1] + 2, sb[0] + 2, sb[3] - 2, sc2); }
                else if (shv > 0.01f) pill(g, sb[0], sb[1], sb[2] - sb[0], sb[3] - sb[1], Canvas.mix(Ui.PANEL2, 0xFF1D1E27, shv));
                String[] d = LANDING[ti];
                String ic = d.length > 3 + k ? d[3 + k].split("\\|")[0] : "fd:note";
                iconBack(g, sb[0] + 10, sb[1] + rowH / 2, 6, sHit, sc2);
                Gfx.push(g); Gfx.translate(g, sb[0] + 5, sb[1] + (rowH - 13) / 2); Gfx.scale(g, 0.7f); Icons.draw(g, ic, 0, 0, sHit || sh ? now : 2400L); Gfx.pop(g);
                Gfx.text(g, font, sn[k], sb[0] + 21, sb[1] + (rowH - 8) / 2, sHit ? Ui.TEXT : Canvas.mix(Canvas.mix(0xFF8A8A96, sc2, 0.4f), 0xFFFFFFFF, shv * 0.4f));
                searchSide.add(new int[]{ti, k, sb[0], sb[1], sb[2], sb[3]});
                y += rowH + 1;
            }
            y += 2;
        }
        if (!any) Gfx.text(g, font, "No page matches", px + 14, y + 4, Ui.DIM);
        searchSideH = y - y0 + 4;
        g.disableScissor();
    }

    private void tabIcon(GuiGraphicsExtractor g, Tab t, int x, int y, long now, boolean lively) {
        switch (t) {
            case DROPS -> Icons.draw(g, "library", x, y, now);
            case HOME -> Icons.draw(g, "home", x, y, now);
            case ANIMATIONS -> Icons.draw(g, "animations", x, y, now);
            case SOUNDS -> Icons.draw(g, "sounds", x, y, now);
            case CHAT -> Icons.draw(g, "chat", x, y, now);
            case GENERAL -> Icons.draw(g, "settings", x, y, now);
            case STATS -> Icons.draw(g, "stats", x, y, now);
            case COSMETICS -> Icons.draw(g, "sparkle", x, y, now);
            case RESOURCES -> Icons.draw(g, "res_heart", x, y, now);
            default -> Gfx.item(g, t.icon, x, y);
        }
    }

    /** The main switch tile under the title: {x0, y0, x1, y1}. */
    private int[] masterBox() { return new int[]{px + 8, py + 36, px + sw - 6, py + 62}; }

    /** The header line: the tab, and the page inside it ("Animations  >  Slayers"). */
    private String pageTitle() {
        String[] n = subs();
        if (!query.isBlank()) return "Search";
        if (n.length == 0 || onLanding()) return tab.label;
        return tab.label + "  >  " + n[curSub()] + (tab == Tab.DROPS && dropSrc != null && dropSrcName != null ? "  >  " + dropSrcName : "");
    }

    /** The line under the header: what the open page is for. */
    private String pageSub() {
        if (!query.isBlank()) return "Whole pages that match, then single settings";
        int k = curSub();
        if (onLanding()) return tab.sub;
        String[][] t = {
            {tab.sub},
            {"Pick what animates and announces", "Every RNG dye, by rarity", "Crops, pests and feasts", "Ritual creatures and their drops", "Experimentation Table drops", "Coming soon", "Coming soon"},
            {"Boss scenes, sounds and looks", "Paint scene, sounds and looks", "Scythe scene, sounds and looks", "Lightning scene, sounds and looks", "Book scene, sounds and looks", "Coming soon", "Coming soon"},
            {"Pick the sound of each moment, and watch the waves"}, {tab.sub},
            {"How the menu looks", "Where prices come from", "Reload the drop data"},
            {"Profit, kills and drops", "Slayer profit and drops", "Dye profit and drops", "Farming profit and drops", "Mythological profit and drops", "Everything you dropped, newest first"},
            {tab.sub}, {tab.sub}};
        String[] a = t[tab.ordinal()];
        return a[Math.min(k, a.length - 1)];
    }

    private static String hex(int argb) { return String.format("%06X", argb & 0xFFFFFF); }
    /** Our own drawn head for the six slayer bosses; a custom slayer keeps its item. */
    private static String bossIcon(SlayerData.Slayer s) {
        return switch (s.id) { case "zombie", "spider", "wolf", "enderman", "blaze", "vampire" -> "fd:boss_" + s.id; default -> s.icon; };
    }

    /** Chat > Rules: the drops of the chosen event that are being sent to chat right now, grouped by boss / source, so nobody has to open the Drop Library to check. */
    private void chatSent(List<Row> rows, FancyConfig c) {
        record Grp(String key, String icon, String title, int col, int[] total, List<Row> on) {}
        List<Grp> gs = new ArrayList<>();
        int ev = Math.max(0, Math.min(4, chatEv));
        if (ev == 1) {
            for (int rar = 0; rar < DyeData.RARITY.length; rar++) {
                Grp g = new Grp("dye" + rar, "fd:dyebottle#" + hex(DyeData.RARITY_COL[rar]), DyeData.RARITY[rar], DyeData.RARITY_COL[rar], new int[1], new ArrayList<>());
                for (DyeData.Dye d : DyeData.ALL) {
                    if (d.rarity() != rar) continue;
                    g.total()[0]++;
                    FancyConfig.DropCfg dc = c.drop(d.owner(), d.name());
                    if (!dc.announce) continue;
                    Row r = row(2, "fd:dyebottle#" + hex(AnimationManager.dyeColor(d.name(), DyeData.RARITY_COL[rar])), d.name(), d.src().split("  ·  ")[0], DyeData.RARITY_COL[rar]);
                    r.subColor = Ui.SUB; r.cell = CELL;
                    r.ctls.add(new Btn("Stop", 0xFF8A3030, () -> dc.announce = false));
                    g.on().add(r);
                }
                gs.add(g);
            }
        } else {
            List<SlayerData.Slayer> src = ev == 0 ? SlayerData.ALL : ev == 2 ? FarmData.ALL : ev == 4 ? List.of(ExperimentData.slayer()) : MythData.spawnable(c.mythPet);
            for (SlayerData.Slayer s : src) {
                String icon = ev == 0 ? bossIcon(s) : ev == 2 ? "fd:farm_" + s.id : ev == 4 ? "fd:experiments" : "fd:myth_" + s.id;
                Grp g = new Grp((ev == 0 ? "s" : ev == 2 ? "f" : ev == 4 ? "x" : "m") + s.id, icon, s.boss, s.color(), new int[1], new ArrayList<>());
                FancyConfig.SlayerCfg sc = c.slayer(s.id);
                for (SlayerData.Drop d : ev == 0 ? nonDye(s) : s.drops) {
                    g.total()[0]++;
                    FancyConfig.DropCfg dc = ev == 3 ? MythData.cfg(c, s, d) : c.drop(s.id, d.name);
                    if (!(sc.enabled && sc.announce && dc.announce)) continue;
                    Row r = row(2, d.icon, d.name, d.chance.isEmpty() ? "chance unknown" : d.chance, s.color());
                    r.iconId = d.id; r.subColor = d.chance.isEmpty() ? Ui.DIM : Ui.odds(d.odds); r.cell = CELL;
                    r.ctls.add(new Btn("Stop", 0xFF8A3030, () -> dc.announce = false));
                    g.on().add(r);
                }
                gs.add(g);
            }
        }
        int on = 0, all = 0; for (Grp g : gs) { on += g.on().size(); all += g.total()[0]; }
        if (!c.masterAnnounce) rows.add(note("The chat master switch in the Drop Library is off, so nothing is sent.", 0xFFFF7777));
        else if (c.channel.equals("OFF")) rows.add(note("Announcements are off. Pick a channel at the top of this page.", 0xFFFF7777));
        Row sum = row(0, "fd:megaphone", on + " of " + all + " drops are sent", on == 0 ? "Nothing reaches chat. Turn on a drop's chat switch in the Drop Library." : "Open a group to see them. Stop keeps the animation but stops the chat message.", on == 0 ? Ui.DIM : GREEN);
        sum.noSearch = true; rows.add(sum);
        for (Grp g : gs) {
            Row f = fold("chat_" + g.key(), g.icon(), g.title(), g.on().size() + " of " + g.total()[0] + " sent", g.on().isEmpty() ? Ui.DIM : g.col());
            f.indent = 1; f.noSearch = true; rows.add(f);
            if (f.open) { if (g.on().isEmpty()) rows.add(note("None of these are sent.", Ui.DIM)); else rows.addAll(g.on()); }
        }
    }

    private Row dyeRow(DyeData.Dye d, FancyConfig c) {
        FancyConfig.DropCfg dc = c.drop(d.owner(), d.name());
        Row dr = row(1, "fd:dyebottle#" + hex(AnimationManager.dyeColor(d.name(), DyeData.RARITY_COL[d.rarity()])), d.name(), d.src().split("  ·  ")[0], DyeData.RARITY_COL[d.rarity()]);
        dr.subColor = Ui.SUB; dr.cell = CELL;                                // a bottle drawn in the dye's own colour
        dr.ctls.add(new Btn("▶", GREEN, () -> play(() -> {
            SlayerData.Slayer own = d.slayer() == null ? SlayerData.OTHER : slayerById(d.slayer());
            SlayerData.Drop dd = new SlayerData.Drop();
            dd.name = d.name(); dd.id = d.id(); dd.icon = DyeData.icon(d.name()); dd.odds = "RNGesus Incarnate"; dd.chance = d.src();
            DropListener.playDrop(own == null ? SlayerData.OTHER : own, dd, "+250%");
        })));
        dr.ctls.add(tog(() -> dc.animate, v -> dc.animate = v, 0xFF4A6CF0));
        dr.ctls.add(tog(() -> dc.announce, v -> dc.announce = v, 0xFFD98A1F));
        dr.dcfg = dc;
        return dr;
    }
    private boolean buildAll;                                                 // Drop Library: every source is built, then sorted into tiles / one source page
    private static String dropSrc; private static int dropSrcCat = -1;     // the boss / source page that is open (null = the tiles)
    private static int dropShow = -1;                                           // Drop Library filter: 0 all, 1 animated, 2 announced, 3 muted
    private Row dropRow(SlayerData.Slayer s, SlayerData.Drop d, FancyConfig c) {
        FancyConfig.DropCfg dc = MythData.ALL.contains(s) ? MythData.cfg(c, s, d) : c.drop(s.id, d.name);
        Row dr = row(2, d.icon, d.name, d.chance.isEmpty() ? "chance unknown" : d.chance, s.color());
        dr.iconId = d.id; dr.subColor = d.chance.isEmpty() ? Ui.DIM : Ui.odds(d.odds);
        dr.cell = CELL;
        dr.ctls.add(new Btn("▶", GREEN, () -> play(() -> { if (FarmData.ALL.contains(s)) DropListener.playFarmDrop(s, d); else if (MythData.ALL.contains(s)) DropListener.playMythDrop(s, d); else DropListener.playDrop(s, d, "+250%"); })));
        dr.ctls.add(tog(() -> dc.animate, v -> dc.animate = v, 0xFF4A6CF0));
        dr.ctls.add(tog(() -> dc.announce, v -> dc.announce = v, 0xFFD98A1F));
        dr.dcfg = dc;
        return dr;
    }

    /** The Drop Library "Show" tiles: picks which drops stay in the list. */
    private static boolean dropKeep(FancyConfig.DropCfg d) {
        return dropShow <= 0 || (dropShow == 1 ? d.animate : dropShow == 2 ? d.announce : (!d.animate || !d.announce));
    }
    private static final String[] SHOW_L = {"Enabled", "Animated", "Chat Alert", "Disabled"}, SHOW_I = {"fd:library", "fd:animations", "fd:chat", "fd:nothing"};
    private static final int[] SHOW_C = {0xFF6C86FF, 0xFFFF72C6, 0xFF7BE05A, 0xFFE5484D};
    private boolean showBarOn() { return tab == Tab.DROPS && !onLanding() && query.isBlank() && ORD[Math.max(0, Math.min(CATS.length - 1, curSub()))] <= 4; }
    /** The Show filter lives in a strip above the menu (like the community buttons), and only while the Drop Library is open. */
    private int[] showBtn(int i) {
        int h = 20, y = py - h - 5; if (y < 2) y = py + ph + 5;
        int w0 = (pw - 12) / 4;
        return new int[]{px + 6 + i * w0, y, px + 6 + (i + 1) * w0 - 3, y + h};
    }
    private void drawShowBar(GuiGraphicsExtractor g, int mx, int my) {
        if (!showBarOn()) return;
        int[] b0 = showBtn(0);
        pill(g, px, b0[1], pw, 20, Ui.PANEL); g.outline(px, b0[1], pw, 20, Ui.BORDER);
        for (int i = 0; i < 4; i++) {
            int[] b = showBtn(i); boolean act = dropShow == i, hov = mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3];
            float hv = anim("show|" + i, hov ? 1f : 0f, 48f);
            pill(g, b[0], b[1] + 2, b[2] - b[0], 16, act ? Canvas.mix(Ui.ROW, SHOW_C[i], 0.35f) : Canvas.mix(Ui.PANEL2, 0xFF23283A, hv));
            if (act) g.fill(b[0], b[1] + 4, b[0] + 2, b[1] + 16, SHOW_C[i]);
            Gfx.push(g); Gfx.translate(g, b[0] + 5, b[1] + 3); Gfx.scale(g, 0.8f); Icons.draw(g, SHOW_I[i], 0, 0, act ? System.currentTimeMillis() : 2400L); Gfx.pop(g);
            Gfx.text(g, font, SHOW_L[i], b[0] + 21, b[1] + 6, act ? Ui.TEXT : Canvas.mix(0xFF8A8A96, 0xFFE0E0E8, hv));
        }
    }
    private boolean showBarClick(double mx, double my) {
        if (!showBarOn()) return false;
        for (int i = 0; i < 4; i++) { int[] b = showBtn(i); if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) { dropShow = dropShow == i ? -1 : i; return true; } }
        return false;
    }
    /** The Drop Library page: Show tiles on top, then one tile per boss / source. A tile opens the page with that source's drops, filtered the same way. */
    private void dropFinish(List<Row> rows, int cat) {
        if (searching) return;
        if (dropSrcCat != cat) { dropSrc = null; dropSrcCat = cat; }
        List<Row> head = new ArrayList<>(), folds = new ArrayList<>(); List<List<Row>> segs = new ArrayList<>();
        List<Row> cur = null;
        for (Row r : rows) {
            if (r.fkey != null) { folds.add(r); cur = new ArrayList<>(); segs.add(cur); }
            else if (cur != null) cur.add(r); else head.add(r);
        }
        if (folds.isEmpty()) return;                                              // Mining / Fishing: nothing to sort yet
        srcList.clear(); for (Row f : folds) srcList.add(new String[]{f.fkey, f.title, f.icon, String.valueOf(f.accent)});
        rows.clear();
        int open = -1;
        if (dropSrc != null) { for (int i = 0; i < folds.size(); i++) if (folds.get(i).fkey.equals(dropSrc)) open = i; if (open < 0) dropSrc = null; }
        if (open >= 0) {                                                          // one boss / source: its drops (through the Show filter, if one is on)
            for (Row r : head) if (r.kind == HERO) rows.add(r);
            Row f = folds.get(open); dropSrcName = f.title;
            Row back = row(0, f.icon, "\u2039  " + f.title, "Click to go back to all of them", f.accent);
            back.noSearch = true; back.click = () -> { dropSrc = null; scroll = 0; tabChanged = System.currentTimeMillis(); };
            rows.add(back);
            boolean any = false; for (Row r : segs.get(open)) if (r.dcfg != null && dropKeep(r.dcfg)) { any = true; break; }
            if (any) for (Row r : segs.get(open)) { if (r.dcfg != null && !dropKeep(r.dcfg)) continue; rows.add(r); }
        } else if (dropShow >= 0) {                                               // a Show filter is on: every matching drop of the page, in one scrollable list
            rows.addAll(head);
            for (int i = 0; i < folds.size(); i++) {
                List<Row> keep = new ArrayList<>(); Row cols = null;
                for (Row r : segs.get(i)) { if (r.dcfg != null && dropKeep(r.dcfg)) keep.add(r); else if (r.dropCols) cols = r; }
                if (keep.isEmpty()) continue;
                Row hs = section(folds.get(i).title + "  \u00B7  " + keep.size()); hs.indent = 1; hs.noSearch = true; rows.add(hs);
                if (cols != null) rows.add(cols);
                rows.addAll(keep);
            }
        } else {                                                                  // nothing picked: one button per boss / source
            rows.addAll(head);
            int n = folds.size(); String[] lb = new String[n], ls = new String[n], li = new String[n]; int[] lc = new int[n]; boolean[] lw = new boolean[n]; String[] keys = new String[n];
            for (int i = 0; i < n; i++) { Row f = folds.get(i); lb[i] = f.title; li[i] = f.icon; lc[i] = f.accent; keys[i] = f.fkey; ls[i] = f.sub; }
            rows.add(picker("", "", lb, ls, li, lc, lw, 2, () -> -1, q -> { dropSrc = keys[q]; scroll = 0; tabChanged = System.currentTimeMillis(); }));
        }
    }
    private static String dropSrcName;
    private final List<String[]> srcList = new ArrayList<>();                // {key, title, icon, colour} of every source on the open Drop Library page: the sidebar lists them under the page
    private float srcExp;

    private Row row(int indent, String icon, String title, String sub, int accent) {
        Row r = new Row(); r.indent = indent; r.icon = icon; r.title = title; r.sub = sub; r.accent = accent; return r;
    }
    private Row note(String text, int color) { Row r = new Row(); r.kind = NOTE; r.title = text; r.subColor = color; r.indent = 0; r.h = 8 + 11 * Math.max(1, wrap(text, cw - 20).size()); return r; }
    private List<String> wrap(String text, int w) {
        List<String> out = new ArrayList<>(); String cur = "";
        for (String word : text.split(" ")) {
            String t = cur.isEmpty() ? word : cur + " " + word;
            if (cur.isEmpty() || font.width(t) <= w) cur = t; else { out.add(cur); cur = word; }
        }
        if (!cur.isEmpty()) out.add(cur);
        return out;
    }
    /** The small list that opens under a drop-down button: compact entries lined up with the button. */
    private Row miniList(Row owner, Ctl anchor, List<String> labels, int sel, IntConsumer onPick) {
        Row r = new Row(); r.pickLabels = labels; r.pickSel = sel; r.onPick = onPick; r.anchor = anchor; r.owner = owner; r.noSearch = true;
        r.h = 0; return r;          // takes no room: it floats over the rows below
    }
    private Row section(String title) { Row r = new Row(); r.kind = SECTION; r.h = 20; r.title = title; return r; }
    private Row fold(String key, String icon, String title, String sub, int accent) {
        Row r = row(0, icon, title, sub, accent);
        r.arrow = true; r.fkey = key; r.open = searching || buildAll || open.contains(key);
        r.click = () -> { if (!open.remove(key)) open.add(key); };
        return r;
    }
    private Tog tog(BooleanSupplier g, Consumer<Boolean> s, int col) { return new Tog(g, s, col); }
    private Switch sw(BooleanSupplier g, Consumer<Boolean> s) { return new Switch(g, s); }
    private Row toggle(String icon, String title, String sub, BooleanSupplier g, Consumer<Boolean> s) {
        Row r = row(0, icon, title, sub, Ui.ACCENT); r.ctls.add(sw(g, s)); r.click = () -> s.accept(!g.getAsBoolean()); return r;
    }
    private Row stepper(String icon, String title, String sub, Supplier<String> value, IntConsumer step, int w) {
        Row r = row(0, icon, title, sub, Ui.ACCENT); r.ctls.add(new Step(value, step, w)); return r;
    }

    private void play(Runnable r) { r.run(); FancyDropsMod.reopenMenu = true; skipIntro = true; onClose(); }      // scroll and open folders are static, so the menu comes back to the same spot

    private void build() {
        rows.clear(); sliderSeq = 0;
        searching = false;
        if (query.isBlank()) { if (onLanding()) buildLanding(tab); else { buildTab(tab); filterSub(); } }
        else {
            String q = query.trim().toLowerCase(Locale.ROOT);
            Tab keepTab = tab; int[] keepSub = subSel.clone();
            java.util.Arrays.fill(searchVis, false);
            List<Row> pages = new ArrayList<>(), res = new ArrayList<>();
            java.util.Set<String> shown = new java.util.HashSet<>();
            int pageCount = 0;
            for (Tab t : Tab.values()) {                                    // a page whose name matches opens whole: "cosmetics", "mythological", "stats"...
                int ti = t.ordinal(); String[] sn = SUBS[ti];
                boolean tabHit = t.label.toLowerCase(Locale.ROOT).contains(q);
                List<Integer> want = new ArrayList<>();
                if (tabHit) want.add(-1); else for (int k = 0; k < sn.length; k++) if (sn[k].toLowerCase(Locale.ROOT).contains(q)) want.add(k);
                for (int k : want) {
                    searchVis[ti] = true;
                    searching = false; tab = t; subSel[ti] = k;
                    rows.clear();
                    if (k < 0 && sn.length > 0) buildLanding(t); else { if (k < 0) subSel[ti] = -1; buildTab(t); filterSub(); }
                    Row head = section(t.label + (k >= 0 ? "  >  " + sn[k] : "")); head.noSearch = true;
                    pages.add(head);
                    for (Row r : rows) { pages.add(r); if (r.kind == NORMAL) shown.add(r.title); }
                    pageCount++;
                }
            }
            tab = keepTab; System.arraycopy(keepSub, 0, subSel, 0, keepSub.length);
            searching = true;
            for (Tab t : Tab.values()) {                                    // then single settings and drops anywhere that match
                rows.clear(); buildTab(t);
                for (Row r : rows) {
                    if (r.kind != NORMAL || r.field != null || r.noSearch || r.locked) continue;
                    if (!(r.title + " " + r.sub).toLowerCase(Locale.ROOT).contains(q)) continue;
                    searchVis[t.ordinal()] = true;
                    if (shown.contains(r.title)) continue;
                    r.indent = 0; if (r.arrow) { r.arrow = false; r.click = null; }
                    r.sub = (r.sub.isEmpty() ? "" : r.sub + "  ·  ") + t.label;
                    res.add(r);
                }
            }
            rows.clear();
            rows.add(section((pageCount + res.size()) + " result" + (pageCount + res.size() == 1 ? "" : "s") + " for \"" + query.trim() + "\""));
            rows.addAll(pages);
            if (!pages.isEmpty() && !res.isEmpty()) rows.add(section("Also matching"));
            rows.addAll(res);
            if (pages.isEmpty() && res.isEmpty()) rows.add(note("Nothing matches. Try a drop name, a slayer, or a setting like \"volume\".", 0xFF9AA6FF));
            searching = false;
        }
        for (Row r : rows) {                                           // a description too long for its row wraps onto more lines instead of scrolling
            if (r.kind != NORMAL || r.sub.isEmpty() || r.wrapSub || r.field != null || r.pickLabels != null) continue;
            int avail = cw - r.indent * 12 - 8 - (r.arrow ? 14 : 0) - 14 - 32;
            if (r.cell > 0) avail -= r.cell * r.ctls.size(); else for (Ctl c : r.ctls) avail -= c.width() + 8;
            if (avail > 70 && font.width(r.sub) > avail) { r.wrapSub = true; r.subW = avail; r.h = Math.max(r.h, 19 + 11 * wrap(r.sub, avail).size()); }
        }
        for (Row r : rows) if (r.kind == HERO) r.h = Math.max(r.h, 76 + 11 * wrap(r.sub, cw - 60).size());      // the words always fit inside the card
        int y = lt - scroll;
        for (Row r : rows) { r.y = y; y += r.h + (r.kind == SECTION ? 1 : r.pickLabels != null ? 0 : 3); layout(r); }
        totalH = y + scroll - lt;
        if (jumpTo != null) {                                          // e.g. Chat > Minimum price > Change lands on the RNG Drop from row
            String want = jumpTo; jumpTo = null;
            for (Row r : rows) if (r.title.equals(want)) { scroll = Math.max(0, r.y - lt + scroll - 30); build(); break; }
        }
    }

    private void buildTab(Tab which) {
        FancyConfig c = FancyConfig.get();
        switch (which) {
            case HOME -> {
                if (c.showTips) { Row tip = new Row(); tip.kind = TIP; tip.h = 46; tip.noSearch = true; tip.icon = "fd:rng"; tip.title = TIPS[tipNow()]; tip.sub = "";
                tip.click = () -> { tipIdx = tipNow() + 1; tipAt = System.currentTimeMillis(); };
                rows.add(tip); }
                Row hero = row(0, "fd:sparkle", "FancyDrops", "Every rare drop becomes a show. Pick what animates, what gets announced, and watch your profit grow.", Ui.ACCENT);
                hero.kind = HERO; hero.h = 84; hero.noSearch = true; rows.add(hero);
                rows.add(section("Jump to"));
                Object[][] pts = {
                    {"fd:library", "Drop Library", "Choose which drops animate and announce", Tab.DROPS},
                    {"fd:animations", "Animations", "Scenes, sounds and looks", Tab.ANIMATIONS},
                    {"fd:megaphone", "Chat", "Tell everyone about your drop, in the channel you set", Tab.CHAT},
                    {"fd:profit", "Stats", "Profit, kills and drops", Tab.STATS}};
                for (Object[] p : pts) {
                    Row pr = row(0, (String) p[0], (String) p[1], (String) p[2], Ui.ACCENT); pr.noSearch = true;
                    final Tab target = (Tab) p[3];
                    pr.ctls.add(new Btn("Open", 0xFF3A4FA0, () -> { tab = target; subSel[target.ordinal()] = -1; scroll = 0; tabChanged = System.currentTimeMillis(); open.removeIf(o -> o.startsWith("dd_")); }));
                    rows.add(pr);
                }
            }
            case DROPS -> {
                buildAll = true; srcList.clear();
                int cat = searching ? 0 : ORD[Math.max(0, Math.min(CATS.length - 1, subSel[Tab.DROPS.ordinal()]))];
                if (cat == 0) {
                    int total = 0; for (SlayerData.Slayer s : SlayerData.ALL) total += nonDye(s).size();
                    Row head = row(0, "fd:slayers", "Slayers", SlayerData.ALL.size() + " bosses  ·  " + total + " drops", Ui.ACCENT);
                    head.kind = HERO; head.h = 84; head.noSearch = true; rows.add(head);
                    if (searching) for (DyeData.Dye d : DyeData.ALL) rows.add(dyeRow(d, c));
                    for (SlayerData.Slayer s : SlayerData.ALL) {
                        Row boss = fold("boss_" + s.id, bossIcon(s), s.boss, nonDye(s).size() + (nonDye(s).size() == 1 ? " drop" : " drops"), s.color());
                        boss.indent = 1;
                        rows.add(boss);
                        if (!boss.open) continue;
                        FancyConfig.SlayerCfg sc = c.slayer(s.id);
                        Row cols = section(""); cols.dropCols = true; cols.indent = 2; cols.h = 14; rows.add(cols);
                        rows.add(masterRow(sc, s, c));
                        for (SlayerData.Drop d : nonDye(s)) rows.add(dropRow(s, d, c));
                    }
                } else if (cat == 1) {
                    int n = DyeData.ALL.size();
                    Row head = row(0, "fd:dye", "Dyes", n + (n == 1 ? " dye" : " dyes"), 0xFFFF72C6);
                    head.kind = HERO; head.h = 84; head.noSearch = true; rows.add(head);
                    for (int gi = 0; gi < DyeData.GROUPS.length; gi++) {
                        int cnt = 0; for (DyeData.Dye d : DyeData.ALL) if (DyeData.group(d.name()) == gi) cnt++;
                        Row fr = fold("dyeg_" + gi, "fd:dyebottle#" + hex(DyeData.GROUP_COL[gi]), DyeData.GROUPS[gi], cnt + (cnt == 1 ? " dye" : " dyes"), DyeData.GROUP_COL[gi]);
                        fr.indent = 1; rows.add(fr);
                        if (!fr.open) continue;
                        Row cols = section(""); cols.dropCols = true; cols.indent = 2; cols.h = 14; rows.add(cols);
                        List<DyeData.Dye> inG = new ArrayList<>();
                        for (DyeData.Dye d : DyeData.ALL) if (DyeData.group(d.name()) == gi) inG.add(d);
                        inG.sort(java.util.Comparator.comparingInt(DyeData.Dye::rarity).thenComparing(DyeData.Dye::name));       // rarest first
                        for (DyeData.Dye d : inG) { Row dr = dyeRow(d, c); dr.indent = 2; rows.add(dr); }
                    }
                    for (int rar = 0; rar < DyeData.RARITY.length; rar++) {                       // dyes no group claims stay on their rarity page
                        List<DyeData.Dye> left = new ArrayList<>();
                        for (DyeData.Dye d : DyeData.ALL) if (DyeData.group(d.name()) < 0 && d.rarity() == rar) left.add(d);
                        if (left.isEmpty()) continue;
                        Row fr = fold("dyer_" + rar, "fd:dyebottle#" + hex(DyeData.RARITY_COL[rar]), DyeData.RARITY[rar], left.size() + (left.size() == 1 ? " dye" : " dyes"), DyeData.RARITY_COL[rar]);
                        fr.indent = 1; rows.add(fr);
                        if (!fr.open) continue;
                        Row cols = section(""); cols.dropCols = true; cols.indent = 2; cols.h = 14; rows.add(cols);
                        left.sort(java.util.Comparator.comparing(DyeData.Dye::name));
                        for (DyeData.Dye d : left) { Row dr = dyeRow(d, c); dr.indent = 2; rows.add(dr); }
                    }
                } else if (cat == 2) {
                    int total = 0; for (SlayerData.Slayer s : FarmData.ALL) total += s.drops.size();
                    Row head = row(0, "fd:farm", "Farming", FarmData.ALL.size() + " sources  ·  " + total + " drops", 0xFF7BE05A);
                    head.kind = HERO; head.h = 84; head.noSearch = true; rows.add(head);
                    for (SlayerData.Slayer s : FarmData.ALL) {
                        Row src = fold("farm_" + s.id, "fd:farm_" + s.id, s.boss, s.drops.size() + (s.drops.size() == 1 ? " drop" : " drops"), s.color());
                        src.indent = 1; rows.add(src);
                        if (!src.open) continue;
                        FancyConfig.SlayerCfg sc = c.slayer(s.id);
                        Row cols = section(""); cols.dropCols = true; cols.indent = 2; cols.h = 14; rows.add(cols);
                        rows.add(masterRow(sc, s, c));
                        for (SlayerData.Drop d : s.drops) rows.add(dropRow(s, d, c));
                    }
                } else if (cat == 4) {
                    Row head = row(0, "fd:experiments", "Experimentation Table", ExperimentData.ALL.length + " drops", 0xFFB56BFF);
                    head.kind = HERO; head.h = 84; head.noSearch = true; rows.add(head);
                    String[] grp = {"Rare Items", "RNG Items", "Enchantment Books"};
                    String[] gic = {"fd:exp_rare", "fd:dice", "fd:exp_book"};
                    int[] gcol = {0xFFC77DFF, 0xFFFF72C6, 0xFF8FA8FF};
                    for (int gi = 0; gi < 3; gi++) {
                        List<String[]> its = new ArrayList<>();
                        for (String[] e : ExperimentData.ALL) if (expGroup(e) == gi) its.add(e);
                        its.sort(java.util.Comparator.comparingLong((String[] e) -> Long.parseLong(e[1].replace(",", ""))).thenComparing(e -> e[0]));
                        Row fr = fold("exp_g" + gi, gic[gi], grp[gi], its.size() + (its.size() == 1 ? " item" : " items"), gcol[gi]);
                        fr.indent = 1; rows.add(fr);
                        if (!fr.open) continue;
                        if (gi == 2) its.sort(java.util.Comparator.comparingDouble((String[] e) -> -pctOf(e[4])).thenComparing(e -> e[0]));       // books: the likeliest first, then A to Z
                        SlayerData.Slayer gs = new SlayerData.Slayer();                                                                           // this group as a little "boss": its own Preview / Animation / Chat Alert headings and switch-all row
                        gs.id = ExperimentData.slayer().id; gs.name = grp[gi]; gs.boss = grp[gi]; gs.icon = ExperimentData.slayer().icon; gs.color = String.format("#%06X", gcol[gi] & 0xFFFFFF);
                        for (String[] e : its) for (SlayerData.Drop q : ExperimentData.slayer().drops) if (q.name.equals(e[0])) gs.drops.add(q);
                        Row cols = section(""); cols.dropCols = true; cols.indent = 2; cols.h = 14; rows.add(cols);
                        rows.add(masterRow(c.slayer(gs.id), gs, c));
                        String last = "";
                        for (String[] e : its) {
                            if (gi == 2 && !e[4].equals(last)) { last = e[4]; Row hs = section(e[3] + "  ·  " + e[4]); hs.indent = 2; rows.add(hs); }
                            SlayerData.Drop xd = null; for (SlayerData.Drop q : ExperimentData.slayer().drops) if (q.name.equals(e[0])) { xd = q; break; }
                            Row ir = dropRow(ExperimentData.slayer(), xd, c); ir.indent = 2; ir.accent = gcol[gi]; ir.sub = e[3] + "  ·  " + e[4] + "  ·  " + e[1] + " XP"; ir.subColor = Ui.odds(e[3]); rows.add(ir);
                        }
                    }
                } else if (cat == 5 || cat == 6) {
                    boolean mine = cat == 5;
                    Row head = row(0, mine ? "fd:mining" : "fd:fishing", mine ? "Mining" : "Fishing", "Coming soon: every " + (mine ? "mining" : "fishing") + " drop, with the same switches as slayers.", Ui.DIM);
                    head.kind = PANEL; head.h = 150; head.noSearch = true; rows.add(head);
                } else {
                    int total = 0; for (SlayerData.Slayer s : MythData.spawnable(c.mythPet)) total += s.drops.size();
                    Row head = row(0, "fd:mytho", "Mythological Ritual", MythData.spawnable(c.mythPet).size() + " of " + MythData.ALL.size() + " creatures  ·  " + total + " drops", 0xFF55D8FF);
                    head.kind = HERO; head.h = 84; head.noSearch = true; rows.add(head);
                    Row pet = row(0, "fd:mytho", "Griffin pet", "Chances for this pet  ·  Mythic is best  ·  creatures it can't call are hidden", 0xFFFFC857);
                    Dd petDd = new Dd(() -> MythData.PETS[c.mythPet], "dd_pet", 90);
                    pet.ctls.add(petDd); rows.add(pet);
                    if (open.contains("dd_pet") && !searching) rows.add(miniList(pet, petDd, java.util.Arrays.asList(MythData.PETS), c.mythPet, i -> { c.mythPet = i; MythData.applyPet(i); open.remove("dd_pet"); }));
                    for (SlayerData.Slayer s : MythData.spawnable(c.mythPet)) {
                        Row src = fold("myth_" + s.id, "fd:myth_" + s.id, s.boss, s.drops.size() + (s.drops.size() == 1 ? " drop" : " drops"), s.color());
                        src.indent = 1; rows.add(src);
                        if (!src.open) continue;
                        FancyConfig.SlayerCfg sc = c.slayer(s.id);
                        Row cols = section(""); cols.dropCols = true; cols.indent = 2; cols.h = 14; rows.add(cols);
                        rows.add(masterRow(sc, s, c));
                        for (SlayerData.Drop d : s.drops) rows.add(dropRow(s, d, c));
                    }
                }
                dropFinish(rows, cat);
                buildAll = false;
            }
            case ANIMATIONS -> {
                boolean all = searching; int sub = curSub();
                if (!searching) {
                    Row back = row(0, "fd:animations", "\u2039  Animations", "Back to all the scenes", Ui.ACCENT);
                    back.noSearch = true; back.click = () -> { subSel[Tab.ANIMATIONS.ordinal()] = -1; scroll = 0; tabChanged = System.currentTimeMillis(); };
                    rows.add(back);
                }
                for (int di = 0; di < CATS.length; di++) {
                    int cat = ORD[di];
                    if (!(all || sub == di)) continue;
                    if (cat >= 5) {
                        String nm = CATS[cat].toLowerCase(Locale.ROOT);
                        Row wp = row(0, cat == 5 ? "fd:mining" : "fd:fishing", CATS[cat] + " animations", "Coming soon: scenes and sounds for " + nm + " drops.", Ui.DIM);
                        wp.kind = PANEL; wp.h = 130; wp.noSearch = true; rows.add(wp);
                    } else animCategory(rows, c, cat, all);
                }
                if (all) animChanges(rows, c);
            }
            case SOUNDS -> {
                if (searching) { soundMaster(rows, c); soundCat = 0; soundsPage(rows, c); }
                else {
                    Row back = row(0, "fd:sounds", "\u2039  Sounds", "Back to the volume and all the animation sounds", Ui.ACCENT);
                    back.noSearch = true; back.click = () -> { subSel[Tab.SOUNDS.ordinal()] = -1; waveKey = null; SoundFx.stopPreviews(); scroll = 0; tabChanged = System.currentTimeMillis(); };
                    rows.add(back);
                    if (curSub() >= 5) {
                        int wc = ORD[Math.min(6, curSub())]; String nm = CATS[wc].toLowerCase(Locale.ROOT);
                        Row wp = row(0, wc == 5 ? "fd:mining" : "fd:fishing", CATS[wc] + " sounds", "Coming soon: sounds for " + nm + " drops.", Ui.DIM);
                        wp.kind = PANEL; wp.h = 130; wp.noSearch = true; rows.add(wp); break;
                    }
                    if (!c.sound) { rows.add(note("Sounds are off. Turn them on from the Sounds page.", Ui.DIM)); break; }
                    soundCat = ORD[Math.max(0, Math.min(4, curSub()))];
                    soundsPage(rows, c);
                }
            }
            case CHAT -> {
                rows.add(section("Announce"));
                rows.add(picker("Announce rare drops in", "Hypixel only  ·  /pc, /cc or /ac", new String[]{"Party", "Co-op", "Public", "Off"}, new String[]{"fd:megaphone", "fd:farm", "fd:chat", "fd:nothing"},
                    new int[]{0xFF6C86FF, 0xFF7BE05A, 0xFFFFC857, 0xFF8A8F9E}, null, () -> idx(CHANNELS, c.channel), i -> c.channel = CHANNELS[i]));
                if (c.channel.equals("OFF") && !searching) break;                    // announcements off: nothing else to set up
                int nev = CHAT_EVENTS.length; String[] evL = new String[nev], evI = new String[nev]; int[] evC = new int[nev]; boolean[] evWip = new boolean[nev];
                for (int i = 0; i < nev; i++) { evL[i] = CHAT_EVENTS[ORD[i]]; evI[i] = PICK_ICONS[ORD[i]]; evC[i] = PICK_COLS[ORD[i]]; evWip[i] = isWip(evL[i]); }
                rows.add(picker("Message for", "Every drop page has its own message", evL, evI, evC, evWip, () -> ordPos(chatEv),
                    i -> { chatEv = ORD[i]; if (ORD[i] <= 4) formatField.setValue(c.formatFor(ORD[i])); }));
                if (chatEv >= 5) {                                                 // Mining and Fishing: not built yet, so the whole editor sits behind a WIP overlay
                    boolean mine = chatEv == 5;
                    Row wp = row(0, mine ? "fd:mining" : "fd:fishing", CHAT_EVENTS[chatEv] + " messages", "Coming soon with the " + (mine ? "mining" : "fishing") + " drops.", Ui.DIM);
                    wp.kind = PANEL; wp.h = 150; wp.noSearch = true; rows.add(wp);
                    break;
                }
                Row fm = row(0, "fd:format", "Message format", "Type your own message. Add placeholders from the list below", Ui.ACCENT);
                fm.h = 44; fm.field = formatField; rows.add(fm);
                if (formatError != null) rows.add(note("Not saved: " + formatError + ". Your last good message is still used.", 0xFFFF7777));
                else {
                    rows.add(section("Preview"));
                    Row cp = new Row(); cp.kind = CHATPREV; cp.noSearch = true; cp.title = c.channel + chatEv; cp.sub = DropListener.sample(c.formatFor(chatEv), chatEv);
                    String uname = net.minecraft.client.Minecraft.getInstance().getUser().getName();
                    int pre = font.width((c.channel.equals("PARTY") ? "Party > " : c.channel.equals("COOP") ? "Co-op > " : "") + "[MVP+] " + uname + ": ");
                    int nl = 1, wl = pre; for (String wd : cp.sub.split(" ")) { int ww = font.width(wd + " "); if (wl + ww > cw - 16 && wl > pre - 1 && nl >= 1 && wl > 0) { nl++; wl = ww; } else wl += ww; }
                    cp.h = 12 + 11 * nl; rows.add(cp);
                }
                rows.add(section("Placeholders"));
                String[][] ph = {
                    {"{item}", "The drop's name", chatEv == 1 ? "Necron Dye" : chatEv == 2 ? "Cropie" : chatEv == 3 ? "Daedalus Stick" : chatEv == 4 ? "Titanic Experience Bottle" : "Warden Heart"},
                    {"{price}", "What it's worth, with its unit", "12.5M coins   (or: untradeable / price unknown)"},
                    {chatEv == 2 ? "{overbloom}" : "{mf}", chatEv == 2 ? "Your Overbloom on that drop (only when Hypixel shows it)" : "Your Magic Find on that drop", chatEv == 2 ? "25 Overbloom" : "250 Magic Find"},
                    {"{source}", chatEv == 3 ? "The creature it dropped from" : chatEv == 4 ? "Where it came from" : chatEv == 1 ? "What it came from (a drop)" : "The boss it dropped from", chatEv == 3 ? "Minotaur" : chatEv == 4 ? "Experimentation Table" : chatEv == 2 ? "Wheat" : chatEv == 1 ? "a drop" : "Voidgloom Seraph"}};
                if (chatEv == 4) ph = new String[][]{ph[0], ph[1], ph[3]};
                else if (chatEv == 1 || chatEv == 2) ph = new String[][]{ph[0], ph[1], ph[2]};
                String[] pic = chatEv == 4 ? new String[]{"fd:itemsize", "fd:coin", "fd:slayers"} : new String[]{"fd:itemsize", "fd:coin", "fd:mf", "fd:slayers"};
                int[] pcol = chatEv == 4 ? new int[]{0xFF56D8FF, 0xFFFFC857, 0xFFFF72C6} : new int[]{0xFF56D8FF, 0xFFFFC857, 0xFF55E6FF, 0xFFFF72C6};
                for (int pi = 0; pi < ph.length; pi++) {
                    String[] p = ph[pi];
                    Row pr = row(0, pic[pi], p[0], p[1] + "  ->  " + p[2], pcol[pi]);
                    pr.click = () -> insertPlaceholder(p[0]);
                    rows.add(pr);
                }
                rows.add(section("Test"));
                Row tr = row(0, "fd:send", "Send a test message", "Posts the preview above to your " + (c.channel.equals("COOP") ? "co-op" : c.channel.equals("ALL") ? "public" : "party") + " chat", Ui.ACCENT);
                tr.ctls.add(new Btn("Test", GREEN, () -> DropListener.testChat(msg -> { chatStatus = msg; chatStatusColor = msg.startsWith("Sent") ? 0xFF62E6A8 : 0xFFFF7777; toast(msg, msg.startsWith("Sent") ? 0xFF62E6A8 : DropListener.NOT_ON_HYPIXEL.equals(msg) ? 0xFFFFB86B : 0xFFFF7777); }, chatEv)));
                rows.add(tr);
                if (!chatStatus.isEmpty()) rows.add(note(chatStatus, chatStatusColor));
                rows.add(section("Rules"));
                Row mp = row(0, "fd:coin", "Minimum price", "Same as RNG Drop: " + PriceService.fmt(c.epicThreshold) + "+  (unknown prices still send)", Ui.ACCENT);
                mp.ctls.add(new Btn("Change", Ui.ROW_HOVER, () -> { tab = Tab.GENERAL; subSel[Tab.GENERAL.ordinal()] = 1; scroll = 0; jumpTo = "RNG Drop from"; tabChanged = System.currentTimeMillis(); }));
                rows.add(mp);
                rows.add(section("Sent in chat  ·  " + CHAT_EVENTS[Math.max(0, Math.min(CHAT_EVENTS.length - 1, chatEv))]));
                chatSent(rows, c);
                rows.add(section("Custom drops"));
                Row add = row(0, "fd:plus", "Add a drop", "Exact in-game name, then Enter or Add", 0xFF55FF55);
                add.h = 44; add.field = addField; add.fieldBtn = "Add"; rows.add(add);
                for (FancyConfig.CustomDrop cd : new ArrayList<>(c.custom)) {
                    Row r = row(0, "fd:rng", cd.name, (cd.id.isBlank() ? "Custom drop" : cd.id) + "  ·  animate / announce", 0xFF55FF55);
                    r.ctls.add(new Btn("Remove", 0xFF8A3030, () -> c.custom.remove(cd)));
                    r.ctls.add(tog(() -> cd.animate, v -> cd.animate = v, 0xFF4A6CF0));
                    r.ctls.add(tog(() -> cd.announce, v -> cd.announce = v, 0xFFD98A1F));
                    rows.add(r);
                }
            }
            case GENERAL -> {
                rows.add(section("Menu"));
                Row tr = row(0, "fd:glass", "Menu translucency", "0% is solid, higher lets the game show through", Ui.ACCENT);
                tr.ctls.add(new Slider(0, 70, 5, () -> c.menuTranslucency, v -> c.menuTranslucency = (int) v, () -> c.menuTranslucency + "%").typed(1));
                rows.add(tr);
                rows.add(picker("Live animation preview", "Where it sits on the Animations and Sounds pages", new String[]{"Top", "Bottom", "Off"},
                    new String[]{"Above the menu", "Below the menu", "Hidden"}, new String[]{"fd:dock_top", "fd:dock_bottom", "fd:dock_off"},
                    new int[]{0xFF6C86FF, 0xFFFF72C6, 0xFF8A8F9E}, null, 3, () -> c.previewAt, i -> c.previewAt = i));
                rows.add(toggle("fd:sparkle", "Animated icons", "Icons move on the page you have open", () -> c.menuIcons, v -> c.menuIcons = v));
                rows.add(toggle("fd:sparkle", "Animated menu", "Fly-in, page transitions and glowing hovers", () -> c.menuAnimated, v -> c.menuAnimated = v));
                rows.add(toggle("fd:rng", "Tip card", "The rotating tip at the top of Home", () -> c.showTips, v -> c.showTips = v));
                rows.add(toggle("fd:discord", "Community buttons", "Discord, Patreon and GitHub beside the menu", () -> c.showCommunity, v -> c.showCommunity = v));
                rows.add(toggle("fd:bell", "Pop-up notices", "Small notices under the menu (saved, discarded, links) instead of chat messages", () -> c.popups, v -> c.popups = v));
                rows.add(section("Prices"));
                Row ps = fold("__price", "fd:coin", "Price source", "Bazaar for Bazaar items, Auction House for the rest", Ui.ACCENT);
                rows.add(ps);
                if (ps.open) {
                    Row bz = toggle("fd:bazaar", "Bazaar", "Items sold on the Bazaar", () -> c.useBazaar, v -> { c.useBazaar = v; PriceService.refreshAsync(); });
                    bz.indent = 1; rows.add(bz);
                    Row bp = stepper("fd:coin", "Bazaar price", "Sell order or instant sell", () -> c.bazaarPrice.equals("SELL") ? "Insta Sell" : "Sell Order",
                            d -> c.bazaarPrice = c.bazaarPrice.equals("SELL") ? "BUY" : "SELL", 90);
                    bp.indent = 1; rows.add(bp);
                    Row ah = toggle("fd:hammer", "Auction House", "Items sold by players (lowest BIN)", () -> c.useAuctions, v -> { c.useAuctions = v; PriceService.refreshAsync(); });
                    ah.indent = 1; rows.add(ah);
                }
                Row rf = row(0, "fd:refresh", "Refresh prices now", "Status: " + PriceService.status, Ui.ACCENT);
                rf.ctls.add(new Btn("Refresh", Ui.ROW_HOVER, PriceService::refreshAsync));
                rows.add(rf);
                rows.add(toggle("fd:rng", "RNG Drop scene", "The big scene for expensive drops", () -> c.epicEffects, v -> c.epicEffects = v));
                rows.add(stepper("fd:coin", "RNG Drop from", "Drops worth this much get the big scene and a chat alert",
                        () -> PriceService.fmt(c.epicThreshold) + " coins",
                        d -> c.epicThreshold = EPIC[wrap(idx(EPIC, c.epicThreshold) + d, EPIC.length)], 90));
                            rows.add(section("Drop Data"));
                Row rl = row(0, "fd:refresh", "Reload drop data", "Reloads the drop data.  " + DataFiles.status, Ui.ACCENT);
                rl.ctls.add(new Btn("Reload", Ui.ROW_HOVER, DataFiles::reloadAll));
                rows.add(rl);
            }
            case COSMETICS -> cosmeticsPage();
            case RESOURCES -> resourcesPage();
            case STATS -> {
                long now = System.currentTimeMillis();
                int sub = searching ? 0 : curSub();
                switch (sub) {
                    case 0 -> statsOverview(rows, now);
                    case 1 -> statsSlayers(rows, c, now);
                    case 2 -> statsCategory(rows, 3, now);
                    case 3 -> statsCategory(rows, 1, now);
                    case 4 -> statsCategory(rows, 2, now);
                    default -> statsHistory(rows);
                }
                statsFooter(rows);
            }
        }
    }

    // ------------------------------------------------------------------ stats

    private static final String[] STAT_CATS = {"Slayers", "Dye", "Farming", "Mythological"};
    private static final String[] STAT_ICON = {"fd:slayers", "fd:dye", "fd:farm", "fd:mytho"};
    private static final int[] STAT_COL = {0xFF8FA8FF, 0xFFFF72C6, 0xFF7BE05A, 0xFF55D8FF};

    /** Which Drop Library page a tracked drop belongs to: 0 Slayers, 1 Dye, 2 Farming. */
    private static int statCat(String name) {
        if (name == null) return 0;
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(" dye")) return 1;
        if (MythData.find(name) != null) {
            for (SlayerData.Slayer s : SlayerData.ALL) for (SlayerData.Drop d : s.drops) if (d.name.equalsIgnoreCase(name)) return 0;
            return 3;
        }
        if (FarmData.sourceOf(name) != null) {
            for (SlayerData.Slayer s : SlayerData.ALL) for (SlayerData.Drop d : s.drops) if (d.name.equalsIgnoreCase(name)) return 0;
            return 2;
        }
        return 0;
    }
    private static List<ProfitTracker.Item> catItems(ProfitTracker.Stat st, int cat) {
        List<ProfitTracker.Item> l = new ArrayList<>();
        for (ProfitTracker.Item it : st.items.values()) if (it.amount > 0 && (cat < 0 || statCat(it.name) == cat)) l.add(it);
        l.sort((x, y) -> Double.compare(ProfitTracker.value(y), ProfitTracker.value(x)));
        return l;
    }
    private static int amountOf(List<ProfitTracker.Item> l) { int n = 0; for (ProfitTracker.Item it : l) n += it.amount; return n; }
    private static double valueOf(List<ProfitTracker.Item> l) { double v = 0; for (ProfitTracker.Item it : l) v += ProfitTracker.value(it); return v; }
    private static String coinsText(double v) { return PriceService.fmt(v) + " coins"; }

    private void goStats(int sub) { subSel[Tab.STATS.ordinal()] = sub; scroll = 0; tabChanged = System.currentTimeMillis(); }

    private static long resetArm;                                   // when "Reset All" was first clicked (a second click within 3 s confirms)

    private static String perHourText(double ph) { return ph < 0 ? "-" : PriceService.fmt(ph) + " / hr"; }
    private static void clearSessionStats() { ProfitTracker.clearSession(); DropListener.LEDGER.clear(); }

    /** The Profit block every stats page starts with: total, per hour and this session (with its own Reset Session). */
    private void statsProfit(List<Row> rows, double total, double session, double perHour, String sessionNote, int col) {
        rows.add(section("Profit"));
        Row t = row(0, "fd:coin", "Total profit", "Every drop at today's prices  ·  saved between sessions", col);
        t.ctls.add(new Val(() -> coinsText(total), 120)); rows.add(t);
        Row h = row(0, "fd:speed", "Profit / hour", perHour < 0 ? "Needs about a minute of play to work out" : "How much you earn in an hour at this pace", col);
        h.ctls.add(new Val(() -> perHourText(perHour), 120)); rows.add(h);
        Row s = row(0, "fd:clock", "Profit this session", sessionNote, col);
        s.ctls.add(new Val(() -> coinsText(session), 120));
        s.ctls.add(new Btn("Reset Session", 0xFF8A3030, FancyScreen::clearSessionStats));
        rows.add(s);
    }

    /** The very bottom of every stats page. */
    private void statsFooter(List<Row> rows) {
        rows.add(section("Reset"));
        boolean armed = System.currentTimeMillis() - resetArm < 3000;
        Row r = row(0, "fd:tnt", "Reset all stats", armed ? "Click again to wipe everything. This can't be undone" : "Wipes all-time and session profit, kills and drops", 0xFFFF5555);
        r.ctls.add(new Btn(armed ? "Sure? Click again" : "Reset All", armed ? 0xFFC03030 : 0xFF8A3030, () -> {
            if (System.currentTimeMillis() - resetArm < 3000) { ProfitTracker.clearOverall(); clearSessionStats(); resetArm = 0; }
            else resetArm = System.currentTimeMillis();
        }));
        rows.add(r);
    }

    private static final int[] animType = {1, 0, 0, 1, 0};                      // Animations: which animation type of each page is open
    private static final String[][][] ATYPES = {
        {{"Nothing Happens", "nothing", "3", "fd:nothing", "When a kill drops nothing.", "Nothing"},
         {"Default", "default", "0", "fd:chop", "When a kill drops something decent.", "Default"},
         {"Rare Drop", "rare", "1", "fd:rare", "When a kill drops something rare.", "Rare"},
         {"RNG Drop", "rng", "2", "fd:rng", "When a drop is worth a celebration.", "RNG"}},
        {{"Dye Drop", "dye", "4", "fd:dye", "One big splat in the dye's colour fills the screen, then the dye rises out.", "Dye"}},
        {{"Farming Drop", "farm", "5", "fd:farm", "A scythe cuts a row of crops and the last one opens. For crops, feasts and pests.", "Farming"},
         {"Ray of Helios", "helios", "8", "fd:sun", "A longer sunrise, only for the Ray of Helios.", "Helios"}},
        {{"Nothing Happens", "mythnothing", "9", "fd:nothing", "When a ritual drops nothing: the circle goes dark.", "Nothing"},
         {"Mythological Drop", "myth", "6", "fd:mytho", "Golden lightning lights a rune circle, which lifts the drop out in laurel.", "Normal"},
         {"RNG Drop", "mythrng", "6", "fd:rng", "For RNG drops: a bigger last bolt, god rays, rune halos and griffin feathers.", "RNG"}},
        {{"Experiment Drop", "exp", "7", "fd:experiments", "The table's book opens, the view zooms in, the pages flip and the drop bursts out.", "Normal"},
         {"RNG Drop", "exprng", "7", "fd:rng", "For RNG items: the book flips more and faster, then god rays and a laurel crown the drop.", "RNG"}}};

    private static float secsOf(FancyConfig c, String key) {
        Float v = c.animSecs.get(key);
        return Math.max(5f, Math.min(10f, v != null ? Math.max(v, c.textDelay(key) + 3.5f) : AnimationManager.naturalSecs(key, c)));
    }

    /** One Animations page: the preview, the animation type (a list when there is more than one), that type's settings and sounds, then the frame and text. */
    private void animCategory(List<Row> rows, FancyConfig c, int cat, boolean all) {
        if (!all) previewSel = cat;                                            // the live preview floats beside the menu (drawSidePreview)
        String[][] types = ATYPES[cat];
        rows.add(section(all ? CATS[cat] + " animation" : "Animation"));
        if (cat == 0) {
            rows.add(picker("Animation style", "Sword slices the boss, Spirit cuts it with light", STYLE_NAMES, new String[]{"fd:slayers", "fd:sparkle"}, () -> Math.floorMod(c.slayerStyle, STYLE_NAMES.length), i -> c.slayerStyle = i));
            rows.add(toggle("fd:nothing", "Animate empty kills", "Play the empty scene when nothing drops", () -> c.animateNothing, v -> c.animateNothing = v));
        }
        int sel = Math.max(0, Math.min(types.length - 1, animType[cat]));
        if (types.length > 1 && !all) {
            String[] names = new String[types.length], tic = new String[types.length]; for (int i = 0; i < names.length; i++) { names[i] = types[i][5]; tic[i] = types[i][3]; }
            rows.add(picker("Animation type", "Pick the one to edit", names, tic, () -> sel, i -> animType[cat] = i));
        }
        for (int ti = 0; ti < types.length; ti++) if (all || ti == sel) animType(rows, c, types[ti]);
        sceneSettings(rows, c, cat, all);
    }

    /** What the Play button of one animation type does. */
    private Runnable playerOf(String key) {
        return switch (key) {
            case "nothing" -> () -> DropListener.playTest(3, true);
            case "default" -> () -> DropListener.playTest(0, false);
            case "rare" -> () -> DropListener.playTest(1, false);
            case "rng" -> () -> DropListener.playTest(2, false);
            case "dye" -> () -> {
                DyeData.Dye d = DyeData.ALL.get(0);
                SlayerData.Drop dd = new SlayerData.Drop();
                dd.name = d.name(); dd.id = d.id(); dd.icon = DyeData.icon(d.name()); dd.odds = "RNGesus Incarnate"; dd.chance = d.src();
                DropListener.playDrop(SlayerData.OTHER, dd, "+250%");
            };
            case "farm" -> DropListener::playFarmTest;
            case "helios" -> DropListener::playHeliosTest;
            case "myth" -> () -> DropListener.playMythTest(false);
            case "mythnothing" -> DropListener::playMythNothing;
            case "exp" -> () -> DropListener.playExpTest(false);
            case "exprng" -> () -> DropListener.playExpTest(true);
            default -> () -> DropListener.playMythTest(true);
        };
    }

    /** The settings of one animation type: Play, how many slices / drips / strikes, how long it lasts, and its sounds. */
    private void animType(List<Row> rows, FancyConfig c, String[] t) {
        final String key = t[1]; final int sc = Integer.parseInt(t[2]);
        int col = switch (key) { case "nothing" -> Ui.DIM; case "default" -> Ui.SUB; case "rare" -> Ui.R_CRAZY; case "dye" -> 0xFFFF72C6; case "farm" -> 0xFF7BE05A; case "myth", "mythrng", "mythnothing" -> 0xFFFFC857; case "exp", "exprng" -> 0xFFB56BFF; default -> Ui.ACCENT; };
        Row head = row(0, t[3], t[0], t[4], col);
        Runnable go = playerOf(key);
        head.ctls.add(new Btn("▶ Play", GREEN, () -> play(go)));
        rows.add(head);
        if (key.equals("default") || key.equals("rare") || key.equals("rng")) {
            List<String> counts = new ArrayList<>(); for (int n = 1; n <= 20; n++) counts.add(n + (n == 1 ? " slice" : " slices"));
            Row cr = row(0, "fd:chop", "Slices", "Cuts before the drop shows", col);
            final String dk = "dd_slice" + sc;
            Dd sd = new Dd(() -> counts.get(clampN(sliceOf(c, sc)) - 1), dk, 82);
            cr.ctls.add(sd); rows.add(cr);
            if (open.contains(dk) && !searching) rows.add(miniList(cr, sd, counts, clampN(sliceOf(c, sc)) - 1, i -> { setSlice(c, sc, i + 1); c.fixChops(); open.remove(dk); }));
        } else if (key.equals("dye")) {
            // one splat in the dye's own colour: nothing to set here but the length
        } else if (key.equals("myth") || key.equals("mythrng") || key.equals("mythnothing")) {
            boolean r = key.equals("mythrng");
            List<String> hits = new ArrayList<>(); for (int n = 1; n <= 10; n++) hits.add(n + (n == 1 ? " strike" : " strikes"));
            Row cr = row(0, "fd:mytho", "Lightning strikes", "Bolts before the drop shows", col);
            final String dk = "dd_strike" + key;
            Dd sd = new Dd(() -> hits.get(Math.max(1, Math.min(10, r ? c.mythStrikesRng : c.mythStrikes)) - 1), dk, 90);
            cr.ctls.add(sd); rows.add(cr);
            if (open.contains(dk) && !searching) rows.add(miniList(cr, sd, hits, Math.max(1, Math.min(10, r ? c.mythStrikesRng : c.mythStrikes)) - 1, i -> { if (r) c.mythStrikesRng = i + 1; else c.mythStrikes = i + 1; open.remove(dk); }));
        } else if (key.equals("exp") || key.equals("exprng")) {
            boolean r = key.equals("exprng");
            List<String> flips = new ArrayList<>(); for (int n = 1; n <= 12; n++) flips.add(n + (n == 1 ? " flip" : " flips"));
            Row cr = row(0, "fd:experiments", "Page flips", "Pages the book turns before the drop shows", col);
            final String dk = "dd_flip" + key;
            Dd sd = new Dd(() -> flips.get(Math.max(1, Math.min(12, r ? c.expFlipsRng : c.expFlips)) - 1), dk, 82);
            cr.ctls.add(sd); rows.add(cr);
            if (open.contains(dk) && !searching) rows.add(miniList(cr, sd, flips, Math.max(1, Math.min(12, r ? c.expFlipsRng : c.expFlips)) - 1, i -> { if (r) c.expFlipsRng = i + 1; else c.expFlips = i + 1; open.remove(dk); }));
        }
        Row dl = row(0, "fd:clock", "Animation length", "Total time, 5 to 10 s  ·  click the number to type", col);
        dl.ctls.add(new Btn("Default", Ui.ROW_HOVER, () -> c.animSecs.remove(key)));
        dl.ctls.add(new Slider(5, 10, 0.1, () -> secsOf(c, key), v -> c.animSecs.put(key, (float) (Math.round(v * 10) / 10.0)), () -> String.format(java.util.Locale.ROOT, "%.1f s", secsOf(c, key))).typed(1));
        rows.add(dl);
        Row ss = row(0, "fd:sounds", "Sounds", "What every moment of this animation plays", col);
        ss.ctls.add(new Btn("Edit sounds", 0xFF3A4FA0, () -> {
            for (int ci = 0; ci < ATYPES.length; ci++) for (int ti = 0; ti < ATYPES[ci].length; ti++) if (ATYPES[ci][ti][1].equals(key)) { soundCat = ci; animType[ci] = ti; }
            tab = Tab.SOUNDS; subSel[Tab.SOUNDS.ordinal()] = ordPos(soundCat); waveKey = null; scroll = 0; tabChanged = System.currentTimeMillis(); open.removeIf(o -> o.startsWith("dd_"));
        }));
        rows.add(ss);
    }

    /** Sounds > Animation Sounds: pick an animation, then the sound of each of its four moments. */
    /** The Sounds main page: the switch, the volume and the custom-sounds toggle. Returns false when sounds are off (nothing else is shown then). */
    private boolean soundMaster(List<Row> rows, FancyConfig c) {
        rows.add(section("Master"));
        rows.add(toggle("fd:sounds", "Sounds", "Master switch (also /fd mute)", () -> c.sound, v -> { c.sound = v; SoundFx.stopPreviews(); waveKey = null; }));
        if (!c.sound && !searching) return false;
        Row vo = row(0, "fd:bell", "Volume", "Applies to every FancyDrops sound", Ui.ACCENT);
        vo.ctls.add(new Slider(0, 100, 1, () -> c.volume, v -> c.volume = (int) Math.round(v), () -> c.volume + "%").typed(1));
        rows.add(vo);
        rows.add(toggle("fd:disc", "Custom sounds", "FancyDrops' own sounds  \u00B7  off = Minecraft's own sounds, picked separately on each page", () -> c.customSounds, v -> { c.customSounds = v; SoundFx.stopPreviews(); waveKey = null; wavePaused = false; }));
        return true;
    }
    private void soundsPage(List<Row> rows, FancyConfig c) {
        if (!c.customSounds) rows.add(note("Custom sounds are off: every animation uses Minecraft's own sounds. Your picks for this set are saved separately.", Ui.DIM));
        final int cat = Math.max(0, Math.min(4, soundCat));
        String[][] types = ATYPES[cat];
        final int sel = Math.max(0, Math.min(types.length - 1, animType[cat]));
        if (types.length > 1) {
            String[] names = new String[types.length], tic = new String[types.length]; for (int i = 0; i < names.length; i++) { names[i] = types[i][5]; tic[i] = types[i][3]; }
            rows.add(picker("Animation type", "Pick the one to edit", names, tic, () -> sel, i -> { animType[cat] = i; waveKey = null; }));
        }
        String[] t = types[sel];
        Row head = row(0, t[3], t[0], t[4], Ui.ACCENT);
        head.ctls.add(new Btn("▶ Play", GREEN, () -> { waveKey = null; wavePaused = false; }));        // only the sounds, in the live sound preview
        head.ctls.add(new Btn(wavePaused ? "Resume" : "Pause", 0xFF3A3F52, () -> { wavePaused = !wavePaused; if (wavePaused) SoundFx.stopPreviews(); }));
        rows.add(head);
        rows.add(section("Sounds"));
        soundRows(Integer.parseInt(t[2]), c);
    }

    /** Position, bars, size and text of one animation page (these used to live in Settings > Scene Settings). */
    /** Animation Changes: the look every animation shares (bar colour and height, item size). Per-animation pages only keep what is theirs. */
    private void animChanges(List<Row> rows, FancyConfig c) {
        rows.add(section("Changes"));
        Row bc = row(0, "fd:bars", "Bar Changes", c.barPx + " px tall  \u00B7  " + (c.barCustom ? "your own colour" : "colour follows the drop"), Ui.ACCENT);
        bc.ctls.add(new Btn("Edit", 0xFF3A4FA0, () -> openEditor(1)));
        rows.add(bc);
        int fxOn = (c.fxPop ? 1 : 0) + (c.fxShine ? 1 : 0) + (c.fxWave ? 1 : 0);
        Row tc = row(0, "fd:text", "Text Changes", String.format(java.util.Locale.ROOT, "Words after %.1f s  \u00B7  %d of 3 effects on", c.textDelayAll, fxOn), Ui.ACCENT);
        tc.ctls.add(new Btn("Edit", 0xFF3A4FA0, () -> openEditor(2)));
        rows.add(tc);
        Row is = row(0, "fd:itemsize", "Item Changes", Math.round(c.itemScale * 100) + "% size  \u00B7  " + (c.show3d ? "3D items" : "flat items"), Ui.ACCENT);
        is.ctls.add(new Btn("Edit", 0xFF3A4FA0, () -> openEditor(3)));
        rows.add(is);
    }

    // ------------------------------------------------------------------ the Bar / Text editors
    private int editor;                                         // 0 none, 1 Bar Changes, 2 Text Changes
    private boolean editWarn; private int lastHoverFx = -2; private long hoverFxAt;
    private int eBarPx, eColor, eDrag, hoverFx = -1, eTextPos; private boolean eCustom; private final boolean[] eFx = new boolean[19];
    private float eSpin, eGlow; private boolean eBob, eGlints, eOutline; private float eItem, eHue, eSat, eVal = 1f, eAll, eTitle, eSub, eSubDelay;
    private boolean eShowOdds, eShowPrice, eShowSrc, eShowMf, eShow3d;
    private Slider slSpin, slGlow, slItem, slBar, slAll, slTitle, slSub, slDelay, eSl; private boolean hexSync;
    private static final int[] PRESETS = {0xFF0B0E2A, 0xFF2A0B1C, 0xFF0B2418, 0xFF1A202C, 0xFF000000, 0xFF5A0F1B, 0xFF14206B, 0xFF0B3A3F, 0xFF2B1450, 0xFF4A3208, 0xFF5A1F3D, 0xFF2F3A4C};
    private static final String[] FX_NAMES = {"Slam in", "Light sweep", "Wave", "Rainbow", "Drop in", "Pulse", "Glitch", "Neon glow", "Bounce in", "Typewriter", "Float", "Shake", "Fire", "Frost", "Twinkle", "Fade away", "Fly up", "Shrink", "Scatter"};
    private static final String[] FX_DESC = {"Letters start huge, then snap into place.", "A bright band runs along the words.", "Letters bob up and down like a wave.", "Colours flow along the letters.",
        "Letters fall in from above.", "The words breathe in and out.", "A quick digital flicker, now and then.", "A soft glow pulses round each letter.",
        "Letters drop in and bounce to a stop.", "Letters are typed out, with a blinking cursor.", "The words drift gently, like on water.", "The letters tremble, like a held breath.",
        "Flames flicker through the letters.", "A cold shimmer, with ice glints.", "Random letters flash gold, like stars.",
        "The letters fade out one after another.", "The letters float up as they fade.", "The letters shrink away to nothing.", "The letters drift apart and vanish."};
    private static final String[] FX_GROUP = {"Entrance  \u00B7  pick 1 (needed)", "Motion  \u00B7  pick up to 1", "Colour  \u00B7  pick up to 2", "Exit  \u00B7  pick up to 1"};
    private static final int[][] FX_ORDER = {{0, 4, 8, 9}, {2, 5, 10, 11, 6}, {3, 1, 7, 12, 13, 14}, {15, 16, 17, 18}};
    private static final int FX_N = 19;
    private void fxToggle(int i, FancyConfig c) {
        int gi = 0; for (int q = 0; q < FX_ORDER.length; q++) if (contains(FX_ORDER[q], i)) gi = q;
        boolean was = c.fx(i);
        if (!was) {
            if (gi != 2) { for (int k : FX_ORDER[gi]) if (k != i) c.setFx(k, false); }                  // one at a time
            else { int on = 0; for (int k : FX_ORDER[2]) if (c.fx(k)) on++; if (on >= 2) { toast("Up to 2 colour effects at once.\nTurn one off first.", 0xFFFFB86B); return; } }
        }
        c.setFx(i, !was); AnimationManager.restartText();
    }
    private int[] e3Box(int i) { int w = (ePrevW() - 4) / 2; return new int[]{eX() + 10 + (i % 2) * (w + 4), eY() + 38 + previewH(ePrevW()) + 20 + (i / 2) * 19, w, 16}; }
    private static boolean contains(int[] a, int v) { for (int x : a) if (x == v) return true; return false; }

    private int eW() { return Math.min(pw - 6, 540); }
    private int eHt() { return Math.min(ph - 6, 348); }
    private int eX() { return px + (pw - eW()) / 2; }
    private int eY() { return py + (ph - eHt()) / 2; }
    private int ePrevW() {
        return Math.max(200, Math.min(Math.min(272, eW() * 48 / 100), Math.round(150f * width / Math.max(1, height))));      // every Changes menu: a big preview on the left, the controls on the right
    }
    private int eRx() { return eX() + 10 + ePrevW() + 14; }
    private int eRw() { return eX() + eW() - 10 - eRx(); }
    private int[] eBtn(int i) { return new int[]{eX() + eW() - 10 - 64 * (i + 1) - 4 * i, eY() + eHt() - 24, 64, 16}; }         // 0 Save, 1 Cancel
    private int[] eWheel() { return new int[]{eRx() + 42, eY() + 120, 38}; }
    private int[] eStrip() { return new int[]{eRx() + 90, eY() + 82, 14, 76}; }
    private int[] eHexBox() { return new int[]{eRx(), eY() + 168, 64, 14}; }
    private int[] eAuto() { return new int[]{eRx() + 68, eY() + 168, Math.min(80, eRw() - 68), 14}; }
    private int[] ePreset(int i) { return new int[]{eX() + 10 + i * 21, eY() + 38 + previewH(ePrevW()) + 22, 18, 18}; }
    private int eFxY0() { return eY() + 38; }
    private int eFxHead(int gi) { int y = eFxY0(); for (int k = 0; k < gi; k++) y += 10 + ((FX_ORDER[k].length + 2) / 3) * 18; return y; }
    private int[] eFxBox(int i) {
        int w = (eRw() - 6) / 3;
        for (int gi = 0; gi < FX_ORDER.length; gi++) for (int k = 0; k < FX_ORDER[gi].length; k++) if (FX_ORDER[gi][k] == i) return new int[]{eRx() + (k % 3) * (w + 3), eFxHead(gi) + 10 + (k / 3) * 18, w, 16};
        return new int[]{0, 0, 0, 0};
    }
    private int[] eSliderBox(int i) { return editor == 2 ? new int[]{eX() + 10, eY() + 38 + previewH(ePrevW()) + 34 + i * 29, ePrevW(), 14} : new int[]{eRx(), eY() + 48 + i * 34, eRw(), 14}; }
    private int eRest() { return eFxHead(FX_ORDER.length); }                                                  // where the effect groups end on the right
    private int[] eSegBox(int i) { int w = (eRw() - 4) / 2; return new int[]{eRx() + i * (w + 4), eRest() + 16, w, 16}; }
    private int[] eTogBox(int i) { int w = (eRw() - 4) / 2; return new int[]{eRx() + (i % 2) * (w + 4), eRest() + 50 + (i / 2) * 19, w, 16}; }
    private static boolean in(double mx, double my, int[] b) { return mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]; }

    private int[] eClose() { return new int[]{eX() + eW() - 25, eY() + 6, 17, 17}; }
    private int[] mainClose() { return new int[]{px + pw - 12, Math.max(2, py - 10), 17, 17}; }
    /** A round close button on a 17 px square, so the cross sits exactly on the middle pixel: dark with a top shine, a red glow and a bolder cross on hover. */
    private void drawX(GuiGraphicsExtractor g, int[] b, boolean hv, String key) {
        float k = anim(key, hv ? 1f : 0f, 40f);
        int cx = b[0] + 8, cy = b[1] + 8;
        if (k > 0.02f) { g.fill(b[0] - 2, b[1] - 2, b[0] + 19, b[1] + 19, Gfx.argb(Math.round(40 * k), 0xFF4A4A)); g.fill(b[0] - 1, b[1] - 1, b[0] + 18, b[1] + 18, Gfx.argb(Math.round(60 * k), 0xFF4A4A)); }
        pill(g, b[0], b[1], 17, 17, Canvas.mix(0xFF272C3C, 0xFFC93636, k));
        g.fill(b[0] + 3, b[1] + 1, b[0] + 14, b[1] + 2, Gfx.argb(Math.round(40 + 50 * k), 0xFFFFFF));          // a thin shine along the top
        g.outline(b[0], b[1], 17, 17, Canvas.mix(0xFF444B63, 0xFFFFA0A0, k));
        int core = Canvas.mix(0xFFC4CBDC, 0xFFFFFFFF, k), soft = Gfx.argb(Math.round(70 + 60 * k), Canvas.mix(0xFFC4CBDC, 0xFFFFFFFF, k) & 0xFFFFFF);
        int n = 4 + Math.round(k);
        for (int d = -n; d <= n; d++) {                                         // two diagonals through the middle pixel, each with a soft shoulder, so every reflection matches
            for (int sg = -1; sg <= 1; sg += 2) {
                int x = cx + d, y = cy + sg * d;
                g.fill(x - 1, y, x, y + 1, soft); g.fill(x + 1, y, x + 2, y + 1, soft);
                g.fill(x, y - 1, x + 1, y, soft); g.fill(x, y + 1, x + 1, y + 2, soft);
            }
        }
        for (int d = -n; d <= n; d++) { g.fill(cx + d, cy + d, cx + d + 1, cy + d + 1, core); g.fill(cx + d, cy - d, cx + d + 1, cy - d + 1, core); }
    }
    private void openEditor(int which) {
        FancyConfig c = FancyConfig.get();
        editor = which; editWarn = false; eDrag = 0; eSl = null; hoverFx = -1;
        eBarPx = c.barPx; eCustom = c.barCustom; eColor = c.barColor;
        for (int i = 0; i < FX_N; i++) eFx[i] = c.fx(i);
        for (int gi = 0; gi < FX_ORDER.length; gi++) { int lim = gi == 2 ? 2 : 1, on = 0; for (int k : FX_ORDER[gi]) if (c.fx(k)) { if (++on > lim) c.setFx(k, false); } }      // 1 entrance, 1 motion, 2 colour, 1 exit
        eAll = c.textScale; eTitle = c.titleScale; eSub = c.subScale; eSubDelay = c.subDelay; eTextPos = c.textPos;
        eShowOdds = c.showOdds; eShowPrice = c.showPrice; eShowSrc = c.showPriceSource; eShowMf = c.showMagicFind; eShow3d = c.show3d; eItem = c.itemScale; eSpin = c.itemSpin; eGlow = c.itemGlow; eBob = c.itemBob; eGlints = c.itemGlints; eOutline = c.itemOutline;
        int col = c.barCustom ? c.barColor : 0xFF0B0E2A;
        float[] hsv = rgbToHsv(col); eHue = hsv[0]; eSat = hsv[1]; eVal = Math.max(0.05f, hsv[2]);
        int sv = sliderSeq; sliderSeq = 1000;                                                 // typed sliders, told apart from the menu's own
        slBar = new Slider(5, 15, 1, () -> c.barPx, v -> c.setBarPx((int) Math.round(v)), () -> c.barPx + " px").typed(1);
        slItem = new Slider(0.5, 1.1, 0.05, () -> c.itemScale, v -> c.itemScale = (float) v, () -> Math.round(c.itemScale * 100) + "%").typed(100);
        slSpin = new Slider(0, 2, 0.1, () -> c.itemSpin, v -> c.itemSpin = (float) v, () -> Math.round(c.itemSpin * 100) + "%").typed(100);
        slGlow = new Slider(0, 1.5, 0.1, () -> c.itemGlow, v -> c.itemGlow = (float) v, () -> Math.round(c.itemGlow * 100) + "%").typed(100);
        slAll = new Slider(0, 1.1, 0.05, () -> c.textScale, v -> c.textScale = (float) v, () -> Math.round(c.textScale * 100) + "%").typed(100);
        slTitle = new Slider(0.5, 1.5, 0.05, () -> c.titleScale, v -> c.titleScale = (float) v, () -> Math.round(c.titleScale * 100) + "%").typed(100);
        slSub = new Slider(0.5, 1.5, 0.05, () -> c.subScale, v -> c.subScale = (float) v, () -> Math.round(c.subScale * 100) + "%").typed(100);
        slDelay = new Slider(0, 3, 0.05, () -> c.subDelay, v -> c.subDelay = (float) v, () -> String.format(java.util.Locale.ROOT, "%.2f s", c.subDelay)).typed(1);
        sliderSeq = sv;
    }
    private void closeEditor(boolean save) {
        FancyConfig c = FancyConfig.get();
        if (typing != null) endTyping(true);
        if (save) { FancyConfig.save(); toast("Changes saved successfully.", 0xFF62E6A8); }
        else {
            c.setBarPx(eBarPx); c.barCustom = eCustom; c.barColor = eColor;
            for (int i = 0; i < FX_N; i++) c.setFx(i, eFx[i]);
            c.textScale = eAll; c.titleScale = eTitle; c.subScale = eSub; c.subDelay = eSubDelay; c.textPos = eTextPos;
            c.showOdds = eShowOdds; c.showPrice = eShowPrice; c.showPriceSource = eShowSrc; c.showMagicFind = eShowMf; c.show3d = eShow3d; c.itemScale = eItem; c.itemSpin = eSpin; c.itemGlow = eGlow; c.itemBob = eBob; c.itemGlints = eGlints; c.itemOutline = eOutline;
        }
        editor = 0; eDrag = 0; eSl = null; editWarn = false; hexField.setFocused(false); hexField.setVisible(false);
    }
    private static float[] rgbToHsv(int argb) {
        float r = ((argb >> 16) & 255) / 255f, g = ((argb >> 8) & 255) / 255f, b = (argb & 255) / 255f;
        float mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b)), d = mx - mn, h = 0f;
        if (d > 1e-5f) { if (mx == r) h = ((g - b) / d) % 6f; else if (mx == g) h = (b - r) / d + 2f; else h = (r - g) / d + 4f; h /= 6f; if (h < 0) h += 1f; }
        return new float[]{h, mx <= 0f ? 0f : d / mx, mx};
    }
    private static int hsvToRgb(float h, float sat, float v) {
        float c = v * sat, hh = (h * 6f) % 6f, x = c * (1f - Math.abs(hh % 2f - 1f)), m = v - c, r, g, b;
        if (hh < 1) { r = c; g = x; b = 0; } else if (hh < 2) { r = x; g = c; b = 0; } else if (hh < 3) { r = 0; g = c; b = x; }
        else if (hh < 4) { r = 0; g = x; b = c; } else if (hh < 5) { r = x; g = 0; b = c; } else { r = c; g = 0; b = x; }
        return 0xFF000000 | (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }
    private void hexTyped(String v) {
        if (hexSync) return;
        String f = v.replaceAll("[^#0-9a-fA-F]", ""); if (!f.equals(v)) { hexSync = true; hexField.setValue(f); hexSync = false; return; }
        String h = f.startsWith("#") ? f.substring(1) : f;
        if (h.length() != 6) return;
        try {
            int rgb = Integer.parseInt(h, 16); FancyConfig c = FancyConfig.get();
            c.barCustom = true; c.barColor = 0xFF000000 | rgb;
            float[] hsv = rgbToHsv(c.barColor); eHue = hsv[0]; eSat = hsv[1]; eVal = Math.max(0.05f, hsv[2]);
        } catch (NumberFormatException ignored) { }
    }
    private void eSetColor(int rgb) {
        FancyConfig c = FancyConfig.get();
        c.barCustom = true; c.barColor = 0xFF000000 | (rgb & 0xFFFFFF);
        float[] hsv = rgbToHsv(c.barColor); eHue = hsv[0]; eSat = hsv[1]; eVal = Math.max(0.05f, hsv[2]);
    }
    private void eSet(double mx, double my) {
        FancyConfig c = FancyConfig.get();
        if (eDrag == 1 && eSl != null) eSl.drag(mx);
        else if (eDrag == 2) {
            int[] w = eWheel(); double dx = mx - w[0], dy = my - w[1];
            eHue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1.0) % 1.0); eSat = (float) Math.min(1.0, Math.sqrt(dx * dx + dy * dy) / w[2]);
            c.barCustom = true; c.barColor = hsvToRgb(eHue, eSat, eVal);
        } else if (eDrag == 3) {
            int[] st = eStrip(); eVal = Math.max(0.05f, 1f - (float) Math.max(0, Math.min(1, (my - st[1]) / st[3])));
            c.barCustom = true; c.barColor = hsvToRgb(eHue, eSat, eVal);
        }
    }
    private Slider[] eSliders() { return editor == 1 ? new Slider[]{slBar} : editor == 3 ? new Slider[]{slItem, slSpin, slGlow} : new Slider[]{slAll, slTitle, slSub, slDelay}; }
    private void editorClick(double mx, double my, int button) {
        FancyConfig c = FancyConfig.get();
        if (button != 0) return;
        if (in(mx, my, eClose())) { closeEditor(false); toast("Changes discarded.", 0xFFFFB86B); return; }          // the X skips the Esc-twice warning
        if (editor == 1) {
            int[] hb = eHexBox();
            if (in(mx, my, hb)) { hexField.setFocused(true); setFocused(hexField); return; }
            else if (hexField.isFocused()) hexField.setFocused(false);
        }
        if (in(mx, my, eBtn(0))) {
            if (editor == 2) { boolean has = false; for (int k : FX_ORDER[0]) if (c.fx(k)) has = true; if (!has) { toast("Pick an entrance effect first.\nThe words need a way to arrive.", 0xFFFF8A5C); return; } }
            closeEditor(true); return;
        }
        if (in(mx, my, eBtn(1))) { closeEditor(false); toast("Changes discarded.", 0xFFFFB86B); return; }
        Slider[] ss = eSliders();
        for (int i = 0; i < ss.length; i++) {
            int[] b = editor == 1 ? new int[]{eRx(), eY() + 48, eRw(), 14} : eSliderBox(i);
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] - 3 && my < b[1] + b[3] + 3) {
                ss[i].x0 = b[0]; ss[i].x1 = b[0] + b[2];
                if (ss[i].click(mx, 0)) { eSl = ss[i]; eDrag = 1; }
                return;
            }
        }
        if (editor == 1) {
            int[] w = eWheel(); double dx = mx - w[0], dy = my - w[1];
            if (dx * dx + dy * dy <= (w[2] + 2) * (w[2] + 2)) { eDrag = 2; eSet(mx, my); return; }
            if (in(mx, my, eStrip())) { eDrag = 3; eSet(mx, my); return; }
            if (in(mx, my, eAuto())) { c.barCustom = false; return; }
            for (int i = 0; i < PRESETS.length; i++) if (in(mx, my, ePreset(i))) { eSetColor(PRESETS[i]); return; }
        } else if (editor == 3) {
            for (int k = 0; k < 4; k++) if (in(mx, my, e3Box(k))) {
                if (k == 0) c.show3d = !c.show3d; else if (k == 1) c.itemBob = !c.itemBob; else if (k == 2) c.itemGlints = !c.itemGlints; else c.itemOutline = !c.itemOutline;
                AnimationManager.restartText();
            }
        } else {
            for (int i = 0; i < FX_N; i++) if (in(mx, my, eFxBox(i))) { fxToggle(i, c); return; }
            for (int i = 0; i < 2; i++) if (in(mx, my, eSegBox(i))) { c.textPos = i == 0 ? 0 : 1; return; }
            if (in(mx, my, eTogBox(0))) c.showOdds = !c.showOdds; else if (in(mx, my, eTogBox(1))) c.showPrice = !c.showPrice;
            else if (in(mx, my, eTogBox(2))) c.showPriceSource = !c.showPriceSource; else if (in(mx, my, eTogBox(3))) c.showMagicFind = !c.showMagicFind;
        }
    }
    private void drawEditor(GuiGraphicsExtractor g, int mx, int my) {
        if (editor == 0) { hexField.setVisible(false); return; }
        FancyConfig c = FancyConfig.get();
        int ex = eX(), ey = eY(), ew = eW(), eh = eHt();
        g.fill(px, py, px + pw, py + ph, 0xCC0A0C12);
        pill(g, ex, ey, ew, eh, Ui.PANEL); g.outline(ex, ey, ew, eh, Ui.BORDER);
        g.fill(ex + 1, ey + 1, ex + ew - 1, ey + 2, Gfx.argb(160, Ui.ACCENT & 0xFFFFFF));
        Gfx.text(g, font, editor == 1 ? "Bar Changes" : editor == 3 ? "Item Changes" : "Text Changes", ex + 10, ey + 9, Ui.TEXT);
        Gfx.text(g, font, editor == 1 ? "Height and colour of the cinematic bars" : editor == 3 ? "Size, spin, glow and extras for the dropped item" : "Sizes, effects and what the words show", ex + 10, ey + 21, Ui.DIM);
        drawX(g, eClose(), in(mx, my, eClose()), "edx");
        int bw = ePrevW(), bh = previewH(bw), bx = ex + 10, by = ey + 38;
        hoverFx = -1;
        if (editor == 2) for (int i = 0; i < FX_N; i++) if (in(mx, my, eFxBox(i))) hoverFx = i;
        Row syn = new Row(); syn.x = bx; syn.y = by; syn.w = bw; syn.pcat = 0; syn.pkey = "default"; syn.nolabel = true; syn.pbw = bw;
        AnimationManager.itemOnly = editor == 3; AnimationManager.textOnly = editor == 2; AnimationManager.fxOnly = editor == 2 ? hoverFx : -1;
        if (hoverFx != lastHoverFx) { lastHoverFx = hoverFx; hoverFxAt = System.currentTimeMillis(); AnimationManager.restartText(); }
        AnimationManager.loopTxt = editor == 2; AnimationManager.freezeT = AnimationManager.loopTxt || editor == 3 ? -1f : 2.4f;
        drawPositionPreview(g, syn);
        AnimationManager.textOnly = false; AnimationManager.itemOnly = false; AnimationManager.fxOnly = -1; AnimationManager.loopTxt = false; AnimationManager.freezeT = -1f;
        if (editor == 1) {
            Gfx.text(g, font, "A still frame: the item and the words don't move", bx, by + bh + 6, Ui.DIM);
            for (int i = 0; i < PRESETS.length; i++) {
                int[] b = ePreset(i); boolean cur = c.barCustom && (c.barColor & 0xFFFFFF) == (PRESETS[i] & 0xFFFFFF), hv = in(mx, my, b);
                g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], PRESETS[i]);
                g.outline(b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, cur ? 0xFFFFFFFF : hv ? 0xFF8A93A8 : 0xFF2A3044);
            }
            Gfx.text(g, font, "Presets", bx, by + bh + 17 + 24 + 10, Ui.DIM);
        } else if (editor == 3) {
            Gfx.text(g, font, "Extras", bx, by + bh + 6, Ui.SUB);
            String[] tl = {"3D items", "Floating", "Glints", "Outline"}; String[] ti3 = {"fd:cube", "fd:speed", "fd:sparkle", "fd:size"}; boolean[] tv = {c.show3d, c.itemBob, c.itemGlints, c.itemOutline};
            for (int i = 0; i < 4; i++) {
                int[] b = e3Box(i); boolean on = tv[i], hv = in(mx, my, b);
                pill(g, b[0], b[1], b[2], b[3], on ? Canvas.mix(Ui.ROW, Ui.ACCENT, 0.3f) : hv ? Ui.ROW_HOVER : Ui.PANEL2);
                if (on) g.fill(b[0], b[1] + 3, b[0] + 2, b[1] + b[3] - 3, Ui.ACCENT);
                Gfx.push(g); Gfx.translate(g, b[0] + 4, b[1] + 2); Gfx.scale(g, 0.75f); Icons.draw(g, ti3[i], 0, 0, hv || on ? System.currentTimeMillis() : 2400L); Gfx.pop(g);
                Gfx.text(g, font, tl[i], b[0] + 18, b[1] + 4, on ? Ui.TEXT : Ui.SUB);
            }
            String[] hint = {"Thick spinning models, or flat pictures", "The item floats gently up and down", "Little glints orbit flat items", "A soft dark edge round the item"};
            int hh = -1; for (int i = 0; i < 4; i++) if (in(mx, my, e3Box(i))) hh = i;
            Gfx.text(g, font, font.plainSubstrByWidth(hh >= 0 ? hint[hh] : "Hover an extra to see what it does.", Math.max(bw, 200)), bx, by + bh + 20 + 2 * 19 + 4, hh >= 0 ? Ui.TEXT : Ui.DIM);
        } else {
            String d = hoverFx >= 0 ? FX_NAMES[hoverFx] + ": " + FX_DESC[hoverFx] : "Hover an effect to see it play.";
            Gfx.text(g, font, font.plainSubstrByWidth(d, Math.max(bw, 200)), bx, by + bh + 6, hoverFx >= 0 ? Ui.TEXT : Ui.DIM);
            for (int gi = 0; gi < FX_ORDER.length; gi++) {
                int hy = eFxHead(gi); Gfx.text(g, font, FX_GROUP[gi], eRx(), hy, Canvas.mix(Ui.DIM, Ui.ACCENT, 0.5f));
            }
            for (int i = 0; i < FX_N; i++) {
                int[] b = eFxBox(i); boolean on = c.fx(i), hv = i == hoverFx;
                pill(g, b[0], b[1], b[2], b[3], on ? Canvas.mix(Ui.ROW, Ui.ACCENT, 0.3f) : hv ? Ui.ROW_HOVER : Ui.PANEL2);
                if (on) g.fill(b[0], b[1] + 3, b[0] + 2, b[1] + b[3] - 3, Ui.ACCENT);
                Gfx.push(g); Gfx.translate(g, b[0] + 3, b[1] + 2); Gfx.scale(g, 0.75f); Icons.draw(g, "fd:fx" + i, 0, 0, hv || on ? System.currentTimeMillis() : 2400L); Gfx.pop(g);
                int avail = b[2] - 24, tw = font.width(FX_NAMES[i]), tcol = on ? Ui.TEXT : Ui.SUB;
                float k = tw > avail ? avail / (float) tw : 1f;
                if (i == hoverFx) {                                                                      // hovered: the name plays its own effect
                    g.enableScissor(b[0] + 15, b[1], b[0] + b[2] - 1, b[1] + b[3]);
                    AnimationManager.effectLabel(g, font, FX_NAMES[i], b[0] + 17 + Math.round(Math.min(tw, avail) / 2f), b[1] + 4 + Math.round((1f - k) * 4f), k, Ui.TEXT, i, ((System.currentTimeMillis() - hoverFxAt) % 4400L) / 1000f);
                    g.disableScissor();
                } else { Gfx.push(g); Gfx.translate(g, b[0] + 17, b[1] + 4 + (1f - k) * 4f); Gfx.scale(g, k); Gfx.text(g, font, FX_NAMES[i], 0, 0, tcol); Gfx.pop(g); }
                if (on) g.fill(b[0] + b[2] - 5, b[1] + 6, b[0] + b[2] - 2, b[1] + 9, 0xFF62E6A8);
            }
        }
        // right column
        numShow = false;
        if (editor == 1) {
            Gfx.text(g, font, "Bar height", eRx(), ey + 38, Ui.SUB);
            slBar.x0 = eRx(); slBar.x1 = eRx() + eRw(); slBar.draw(g, ey + 48, 14, in(mx, my, new int[]{eRx(), ey + 45, eRw(), 20}));
            int[] w = eWheel(); int cell = 2;
            for (int yy = -w[2]; yy < w[2]; yy += cell) for (int xx = -w[2]; xx < w[2]; xx += cell) {
                double dx = xx + cell / 2.0, dy = yy + cell / 2.0, d = Math.sqrt(dx * dx + dy * dy);
                if (d > w[2]) continue;
                g.fill(w[0] + xx, w[1] + yy, w[0] + xx + cell, w[1] + yy + cell, hsvToRgb((float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1.0) % 1.0), (float) (d / w[2]), eVal));
            }
            int mxp = w[0] + Math.round((float) Math.cos(eHue * 2 * Math.PI) * eSat * w[2]), myp = w[1] + Math.round((float) Math.sin(eHue * 2 * Math.PI) * eSat * w[2]);
            g.outline(mxp - 3, myp - 3, 7, 7, 0xFF000000); g.outline(mxp - 2, myp - 2, 5, 5, 0xFFFFFFFF);
            int[] st = eStrip();
            for (int yy = 0; yy < st[3]; yy += 2) g.fill(st[0], st[1] + yy, st[0] + st[2], st[1] + yy + 2, hsvToRgb(eHue, eSat, Math.max(0.05f, 1f - yy / (float) st[3])));
            int ky = st[1] + Math.round((1f - eVal) * st[3]); g.outline(st[0] - 1, ky - 1, st[2] + 2, 3, 0xFFFFFFFF);
            int cur = c.barCustom ? c.barColor : 0xFF3A4254;
            int[] hb = eHexBox(), au = eAuto();
            if (!hexField.isFocused()) { String t = String.format("#%06X", (c.barCustom ? c.barColor : 0xFF0B0E2A) & 0xFFFFFF); if (!hexField.getValue().equalsIgnoreCase(t)) { hexSync = true; hexField.setValue(t); hexSync = false; } }
            hexField.setX(hb[0]); hexField.setY(hb[1]); hexField.setWidth(hb[2]);
            boolean ah = in(mx, my, au);
            pill(g, au[0], au[1], au[2], au[3], !c.barCustom ? Canvas.mix(Ui.ROW, Ui.ACCENT, 0.5f) : ah ? Ui.ROW_HOVER : Ui.PANEL2);
            Gfx.textCentered(g, font, "Auto colour", au[0] + au[2] / 2, au[1] + 3, Ui.TEXT);
            Gfx.text(g, font, "Hex code", eRx(), ey + 158, Ui.DIM);
            g.fill(eRx(), ey + 188, eRx() + 14, ey + 200, cur); g.outline(eRx(), ey + 188, 14, 12, Ui.BORDER);
            Gfx.text(g, font, c.barCustom ? "Your colour" : "Follows the drop", eRx() + 20, ey + 190, Ui.DIM);
        } else if (editor == 3) {
            String[] il = {"Item size", "Spin speed", "Glow"}; Slider[] ss3 = eSliders();
            for (int i = 0; i < 3; i++) {
                int[] b = eSliderBox(i);
                Gfx.text(g, font, il[i], b[0], b[1] - 11, Ui.SUB);
                ss3[i].x0 = b[0]; ss3[i].x1 = b[0] + b[2]; ss3[i].draw(g, b[1], b[3], in(mx, my, new int[]{b[0], b[1] - 2, b[2], 18}));
            }
        } else {
            String[] lb = {"Overall text size", "Title size", "Sub Title size", "Sub Title delay"};
            Slider[] ss = eSliders();
            for (int i = 0; i < 4; i++) {
                int[] b = eSliderBox(i);
                Gfx.text(g, font, lb[i], b[0], b[1] - 11, Ui.SUB);
                ss[i].x0 = b[0]; ss[i].x1 = b[0] + b[2]; ss[i].draw(g, b[1], b[3], in(mx, my, new int[]{b[0], b[1] - 2, b[2], 18}));
            }
            Gfx.text(g, font, "Text position", eRx(), eRest() + 4, Ui.SUB);
            for (int i = 0; i < 2; i++) {
                int[] b = eSegBox(i); boolean on = Math.floorMod(c.textPos, 2) == i, hv = in(mx, my, b);
                pill(g, b[0], b[1], b[2], b[3], on ? Canvas.mix(Ui.ROW, Ui.ACCENT, 0.3f) : hv ? Ui.ROW_HOVER : Ui.PANEL2);
                if (on) g.fill(b[0], b[1] + 3, b[0] + 2, b[1] + b[3] - 3, Ui.ACCENT);
                Gfx.textCentered(g, font, i == 0 ? "Top" : "Bottom", b[0] + b[2] / 2, b[1] + 4, on ? Ui.TEXT : Ui.SUB);
            }
            Gfx.text(g, font, "Show", eRx(), eRest() + 38, Ui.SUB);
            String[] tn = {"Drop chance", "Price", "Price source", null}; String[] ti = {"fd:dice", "fd:coin", "fd:bazaar", null}; boolean[] to = {c.showOdds, c.showPrice, c.showPriceSource, c.showMagicFind};
            for (int i = 0; i < 4; i++) {
                int[] b = eTogBox(i); boolean on = to[i], hv = in(mx, my, b);
                pill(g, b[0], b[1], b[2], b[3], on ? Canvas.mix(Ui.ROW, Ui.ACCENT, 0.3f) : hv ? Ui.ROW_HOVER : Ui.PANEL2);
                if (on) g.fill(b[0], b[1] + 3, b[0] + 2, b[1] + b[3] - 3, Ui.ACCENT);
                int tcol = on ? Ui.TEXT : Ui.SUB;
                if (ti[i] != null) { Gfx.push(g); Gfx.translate(g, b[0] + 4, b[1] + 2); Gfx.scale(g, 0.75f); Icons.draw(g, ti[i], 0, 0, hv || on ? System.currentTimeMillis() : 2400L); Gfx.pop(g); }
                if (tn[i] != null) {
                    int avail = b[2] - 20, tw = font.width(tn[i]);
                    if (tw <= avail) Gfx.text(g, font, tn[i], b[0] + 18, b[1] + 4, tcol);
                    else { float k = avail / (float) tw; Gfx.push(g); Gfx.translate(g, b[0] + 18, b[1] + 4 + (1f - k) * 4f); Gfx.scale(g, k); Gfx.text(g, font, tn[i], 0, 0, tcol); Gfx.pop(g); }       // long names shrink to fit
                } else {                                                                                   // Magic Find and Overbloom: their little icons, no words
                    net.minecraft.network.chat.Style es = net.minecraft.network.chat.Style.EMPTY.withFont(AnimationManager.EMOJI);
                    Component lab = Component.empty().append(Component.literal("\ue030").withStyle(es.withColor(on ? Ui.MAGIC_FIND & 0xFFFFFF : 0x7A8494))).append(Component.literal("  /  ").withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(tcol & 0xFFFFFF))).append(Component.literal("\ue031").withStyle(es.withColor(on ? 0xFFFF55 : 0x7A8494)));
                    g.text(font, lab, b[0] + 8, b[1] + 4, tcol, false);
                }
            }
        }
        if (editWarn && !c.popups) {
            Gfx.text(g, font, "Changes won't be saved.", ex + 10, ey + eh - 34, 0xFFFF6B6B);
            Gfx.text(g, font, "Esc again to discard  ·  Save to keep", ex + 10, ey + eh - 22, 0xFFFFB86B);
        }
        for (int i = 0; i < 2; i++) {
            int[] b = eBtn(i); boolean hv = in(mx, my, b);
            int col = i == 0 ? 0xFF2D7D46 : 0xFF3A3F52;
            pill(g, b[0], b[1], b[2], b[3], hv ? Canvas.mix(col, 0xFFFFFFFF, 0.18f) : col);
            Gfx.textCentered(g, font, i == 0 ? "Save" : "Cancel", b[0] + b[2] / 2, b[1] + 4, 0xFFFFFFFF);
        }
        numField.setVisible(typing != null && numShow);
        if (numField.visible) numField.extractRenderState(g, mx, my, 0f);
        hexField.setVisible(editor == 1);
        if (editor == 1) hexField.extractRenderState(g, mx, my, 0f);
    }

    // ------------------------------------------------------------------ pop-up notices (a small card under the menu instead of a chat line)
    private static String toastText = ""; private static int toastCol; private static long toastAt, toastDur;
    static void toast(String text, int col) {
        if (!FancyConfig.get().popups) { try { var pl = net.minecraft.client.Minecraft.getInstance().player; if (pl != null) pl.sendSystemMessage(Chat.msg(text.replace("\n", " "))); } catch (Throwable ignored) { } return; }
        toastText = text; toastCol = col; toastAt = System.currentTimeMillis(); toastDur = 3400; toastShake = false;
    }
    private void drawToast(GuiGraphicsExtractor g, long now) {
        if (toastText.isEmpty()) return;
        long age = now - toastAt; if (age > toastDur) { toastText = ""; return; }
        float in = ease(age / 240f), out = age > toastDur - 450 ? (toastDur - age) / 450f : 1f;
        String[] ln = toastText.split("\n"); int tw = 0; for (String s : ln) tw = Math.max(tw, font.width(s));
        int w = tw + 46, h = 10 + ln.length * 11 + 6, x = px + (pw - w) / 2;
        if (toastShake && age < 600) x += Math.round((float) Math.sin(age / 22.0) * 6f * (1f - age / 600f));          // LOCKED! rattles
        int y = py + ph + 8 + (TEST_STRIP ? 22 : 0) + (dockK > 0.97f && FancyConfig.get().previewAt != 0 && dockWanted() ? DOCK_H + DOCK_GAP : 0) + Math.round((1f - in) * 10f);
        if (y + h > height - 2) y = Math.max(2, height - h - 2);
        int al = Math.round(255 * in * out);
        if (al < 6) return;
        pill(g, x, y, w, h, Gfx.argb(Math.min(245, al), 0x10141D));
        for (int i = 0; i < w; i++) {                                                                  // a colour that glides along the border
            float u = ((i / (float) w) + now / 1800f) % 1f; int bc = Canvas.mix(toastCol, 0xFFFFFFFF, 0.25f + 0.5f * (float) Math.abs(Math.sin(u * Math.PI * 2)));
            g.fill(x + i, y, x + i + 1, y + 1, Gfx.argb(al, bc & 0xFFFFFF)); g.fill(x + i, y + h - 1, x + i + 1, y + h, Gfx.argb(al, bc & 0xFFFFFF));
        }
        g.fill(x, y + 1, x + 1, y + h - 1, Gfx.argb(al, toastCol & 0xFFFFFF)); g.fill(x + w - 1, y + 1, x + w, y + h - 1, Gfx.argb(al, toastCol & 0xFFFFFF));
        Gfx.push(g); Gfx.translate(g, x + 5, y + (h - 12) / 2f + (float) Math.sin(now / 260.0) * 0.8f); Gfx.scale(g, 0.75f); Icons.draw(g, "fd:sparkle", 0, 0, now); Gfx.pop(g);
        for (int l = 0; l < ln.length; l++) {
            int cx = x + 24 + (w - 46 - font.width(ln[l])) / 2, ty = y + 6 + l * 11;
            for (int i = 0; i < ln[l].length(); i++) {
                String ch = String.valueOf(ln[l].charAt(i));
                float sh = 0.5f + 0.5f * (float) Math.sin(now / 240.0 - i * 0.5);
                int col = Canvas.mix(toastCol, 0xFFFFFFFF, 0.2f + 0.6f * sh);
                Gfx.text(g, font, ch, cx, ty - Math.round(sh * 1f), Gfx.argb(al, col & 0xFFFFFF) | 0);
                cx += font.width(ch);
            }
        }
    }

    private void sceneSettings(List<Row> rows, FancyConfig c, int cat, boolean all) {
        final int sel = cat, eff = cat;
        final boolean cineCat = cat == 0 || cat == 1 || cat == 3 || cat == 4;
        rows.add(section(all ? CATS[cat] + " frame and size" : "Frame and size"));
        String[] posNames = eff == 2 ? AnimationManager.FARM_POS_NAMES : c.cine(eff) ? AnimationManager.POS_BAR_NAMES : AnimationManager.POS9_NAMES;
        Row pos = row(0, "fd:position", CATS[eff] + " position", eff == 2 ? "3 x 3 grid, or Custom: drag the preview or use the arrow keys"
                : c.cine(eff) ? "Top, middle or bottom between the bars" : "3 x 3 grid  ·  text goes where there is room", Ui.ACCENT);
        Dd posDd = new Dd(() -> AnimationManager.positionName(c, eff), "dd_pos", 110);
        pos.ctls.add(posDd);
        rows.add(pos);
        if (open.contains("dd_pos") && !searching)
            rows.add(miniList(pos, posDd, java.util.Arrays.asList(posNames), eff == 2 ? c.farmPos : c.cine(eff) ? AnimationManager.posBarsOf(c, eff) : AnimationManager.pos9Of(c, eff), i2 -> {
                if (eff == 2) c.farmPos = i2;
                else if (eff == 1) { if (c.cine(1)) c.dyePosBars = i2; else c.dyePos9 = i2; }
                else if (eff == 3) { if (c.cine(3)) c.mythPosBars = i2; else c.mythPos9 = i2; }
                else if (eff == 4) { if (c.cine(4)) c.expPosBars = i2; else c.expPos9 = i2; }
                else if (c.cine(0)) c.posBars = i2; else c.pos9 = i2;
                open.remove("dd_pos"); }));
        if (cineCat) {
            rows.add(toggle("fd:bars", CATS[sel] + " cinematic bars", "Bars close in around the scene", () -> c.cine(sel), v -> c.setCine(sel, v)));
            rows.add(toggle("fd:blur", CATS[sel] + " background blur", "Blurs the game behind (needs Blurriness above 0 in Video Settings)", () -> c.blur(sel), v -> c.setBlur(sel, v)));
        }
        if (eff == 2) {
            Row fz = row(0, "fd:size", "Farming size", "Size of the Farming scene", 0xFF7BE05A);
            fz.ctls.add(new Slider(0.5, 1.1, 0.05, () -> c.farmScale, v -> c.farmScale = (float) v, () -> Math.round(c.farmScale * 100) + "%").typed(100).live());
            rows.add(fz);
        } else {
            Row as = row(0, "fd:size", CATS[eff] + " size", c.cine(eff) ? "Scene size inside the frame  ·  each animation has its own" : "Scene size, 100% to 140%  ·  each animation has its own", Ui.ACCENT);
            as.ctls.add(new Slider(c.cine(eff) ? 0.6 : 1.0, 1.4, 0.05, () -> c.cine(eff) ? c.sizeOf(eff) : Math.max(1f, c.sizeOf(eff)), v -> c.setSizeOf(eff, (float) v), () -> Math.round((c.cine(eff) ? c.sizeOf(eff) : Math.max(1f, c.sizeOf(eff))) * 100) + "%").typed(100).live());
            rows.add(as);
        }
    }

    private static int tipIdx = 0;
    private static long tipAt = System.currentTimeMillis();
    /** The tip shown now: moves on by itself every 3 seconds, and a click skips ahead (restarting the 3 seconds). */
    private static int tipNow() { return Math.floorMod(tipIdx + (int) ((System.currentTimeMillis() - tipAt) / 3000L), TIPS.length); }
    private static final String[] TIPS = {
        "Type /fd sim to try any animation without touching your stats.",
        "Animations > Changes lets you edit the Bar, the Text and the Item.",
        "Text Changes: hover an effect to see it on the preview first.",
        "Every text needs one Entrance effect. Then add a Motion, Colors and an Exit.",
        "Drop Library > Show hides the drops you do not care about.",
        "Sounds: pick a sound for every moment, then Pause and Resume to listen.",
        "Each drop has two switches: one for the animation, one for chat.",
        "Set a minimum price in Chat to only share drops worth bragging about.",
        "Item Changes: spin, glow and bob your drop. Extras restart the preview.",
        "Animation length runs from 5 to 10 seconds. Tune it to your farming speed.",
        "The live preview can sit on top, at the bottom, or be hidden.",
        "Esc twice discards what you changed in an editor.",
        "/fd mute silences the mod whenever you need quiet.",
        "Cosmetics: pick a suffix and see it in chat and the tab list."};

    /** A little fake player head: skin, hair and two eyes. */
    private static void head(GuiGraphicsExtractor g, int x, int y, int skin, int hair) {
        g.fill(x, y, x + 8, y + 8, skin); g.fill(x, y, x + 8, y + 3, hair);
        g.fill(x + 2, y + 4, x + 3, y + 5, 0xFF1C1C28); g.fill(x + 5, y + 4, x + 6, y + 5, 0xFF1C1C28);
    }

    private void drawCosPreview(GuiGraphicsExtractor g, Row r) {
        long now = System.currentTimeMillis(); float sec = (now % 3600000L) / 1000f;
        Cosmetics.Suffix cs = Cosmetics.chosen();
        FancyConfig pc = FancyConfig.get(); String fx = CosmeticData.unlockedId(pc.nameFx) ? pc.nameFx : null;
        String user = net.minecraft.client.Minecraft.getInstance().getUser().getName();
        net.minecraft.network.chat.Style plain = net.minecraft.network.chat.Style.EMPTY;
        pill(g, r.x, r.y, r.w, r.h, Ui.ROW);
        int x0 = r.x + 8, w0 = r.w - 16;
        // --- in chat
        boolean compact = r.h < 140;
        Gfx.text(g, font, "In chat", x0, r.y + (compact ? 3 : 6), Ui.SUB);
        if (cs == null && fx == null) Gfx.text(g, font, "Nothing picked", x0 + w0 - font.width("Nothing picked"), r.y + 6, Ui.DIM);
        int cy = r.y + (compact ? 13 : 17);
        g.fill(x0, cy, x0 + w0, cy + 29, 0xB0000000); g.outline(x0, cy, w0, 29, 0x33FFFFFF);
        net.minecraft.network.chat.MutableComponent l1 = Component.empty();
        l1.append(Component.literal("[MVP+] ").withStyle(plain.withColor(0x55FFFF))).append(Cosmetics.nameComp(user, 0x55FFFF, fx, true, now));
        if (cs != null) { l1.append(Component.literal(" ")); l1.append(Cosmetics.chatGlyph(cs).copy().withStyle(plain.withColor(0xFFFFFF))); l1.append(Component.literal(" ")); }          // the very same animated picture the chat shows
        l1.append(Component.literal(": that drop was insane!").withStyle(plain.withColor(0xFFFFFF)));
        g.text(font, Component.literal("RARE DROP! ").withStyle(plain.withColor(0xFFAA00).withBold(true)).append(Component.literal("Necron Dye").withStyle(plain.withColor(0xFF55FF).withBold(false))), x0 + 6, cy + 5, 0xFFFFFFFF, true);
        g.text(font, l1, x0 + 6, cy + 17, 0xFFFFFFFF, true);
        // --- in the tab list: a mock of SkyBlock's player list, with you lit up and the ping bars moving
        int ty = cy + (compact ? 42 : 41);
        Gfx.text(g, font, "In the tab list", x0, ty - 10, Ui.SUB);
        int tw = Math.min(w0, 372), tx = r.x + (r.w - tw) / 2, th = r.y + r.h - (compact ? 4 : 6) - ty;
        g.fill(tx, ty, tx + tw, ty + th, 0xC00C0E14); g.outline(tx, ty, tw, th, 0x44FFFFFF);
        String head = "You are playing on MC.HYPIXEL.NET";
        g.text(font, head, tx + (tw - font.width(head)) / 2, ty + 3, 0xFFFFAA00, true);
        g.fill(tx + 4, ty + 14, tx + tw - 4, ty + 15, 0x33FFFFFF);
        String[] names = {"Dirt_Mage", "xX_Farmer_Xx", "CoinCollector", "Pickaxe_Pro", null, "BazaarBob", "Slayer_Sam", "NetherNinja", "DyeDealer", "Mythic_Mel", "MinerMike", "Skyblocker99"};
        String[] rank = {"", "[VIP] ", "[MVP+] ", "[MVP] ", "[MVP+] ", "[VIP+] ", "", "[MVP++] ", "[VIP] ", "[MVP+] ", "[MVP] ", ""};
        int[] rc = {0xAAAAAA, 0x55FF55, 0x55FFFF, 0x55FFFF, 0x55FFFF, 0x55FF55, 0xAAAAAA, 0xFFAA00, 0x55FF55, 0x55FFFF, 0x55FFFF, 0xAAAAAA};
        int[] skin = {0xFFC99A6B, 0xFFE0B48A, 0xFFB5835A, 0xFFD9A57A, 0xFFEBC7A0, 0xFFC08A5E}, hair = {0xFF4A2F1B, 0xFF222222, 0xFF6B3F1D, 0xFF8A5A2C, 0xFF111111, 0xFFC9A227};
        int cols = 3, colW = (tw - 12) / cols, rowH = 11;
        for (int k = 0; k < names.length; k++) {
            int ex = tx + 6 + (k / 4) * colW, ey2 = ty + 18 + (k % 4) * rowH; boolean me = names[k] == null;
            if (me) {                                                                               // you: a glowing row with a sweeping shine
                float pu = 0.5f + 0.5f * (float) Math.sin(sec * 3f);
                g.fill(ex - 1, ey2 - 1, ex + colW - 3, ey2 + 9, Gfx.argb(Math.round(50 + 40 * pu), Ui.ACCENT & 0xFFFFFF)); g.outline(ex - 1, ey2 - 1, colW - 2, 10, Gfx.argb(Math.round(130 + 100 * pu), Ui.ACCENT & 0xFFFFFF));
                g.enableScissor(ex - 1, ey2 - 1, ex + colW - 3, ey2 + 9);
                int sx = ex - 14 + Math.round(((sec * 0.7f) % 1.6f) * (colW + 10)); for (int j = 0; j < 10; j++) g.fill(sx + j / 2, ey2 - 1 + j, sx + j / 2 + 3, ey2 + j, 0x55FFFFFF);
                g.disableScissor();
            }
            head(g, ex, ey2, me ? 0xFFD9A57A : skin[k % 6], me ? 0xFF2F6BFF : hair[k % 6]);
            net.minecraft.network.chat.MutableComponent nm = Component.literal(me ? "[MVP+] " : rank[k]).withStyle(plain.withColor(me ? 0x55FFFF : rc[k]));
            nm.append(me ? Cosmetics.nameComp(user, 0x55FFFF, fx, true, now) : Component.literal(names[k]).withStyle(plain.withColor(rc[k])));
            if (me && cs != null) { nm.append(Component.literal(" ")); nm.append(Cosmetics.chatGlyph(cs).copy().withStyle(plain.withColor(0xFFFFFF))); nm.append(Component.literal(" ")); }
            g.text(font, nm, ex + 11, ey2, 0xFFFFFFFF, true);
            float wob = (float) Math.sin(sec * 1.3f + k * 1.9f) + 0.6f * (float) Math.sin(sec * 3.1f + k * 0.7f);            // ping: the bars rise and fall like a real connection
            int lit = Math.max(1, Math.min(5, 4 + Math.round(wob * 1.2f)));
            int bc = lit >= 4 ? 0xFF55FF55 : lit >= 3 ? 0xFFFFFF55 : lit >= 2 ? 0xFFFFAA00 : 0xFFFF5555;
            for (int b = 0; b < 5; b++) g.fill(ex + colW - 20 + b * 3, ey2 + 8 - (2 + b), ex + colW - 18 + b * 3, ey2 + 8, b < lit ? bc : 0xFF3A4254);
        }
        String foot = "Ranks, Boosters & MORE! STORE.HYPIXEL.NET";
        if (!compact) g.text(font, foot, tx + (tw - font.width(foot)) / 2, ty + th - 11, 0xFFFF55FF, true);
    }

    private void resourcesPage() {
        Row hero = row(0, "fd:res_heart", "Resources used", "FancyDrops stands on the work of these projects and communities. Thank you!", Ui.ACCENT);
        hero.kind = HERO; hero.h = 84; hero.noSearch = true; rows.add(hero);
        rows.add(section("Credits"));
        String[][] res = {
            {"fd:res_wiki", "Hypixel SkyBlock Wiki", "Drop odds, pity and RNG meters, and the Experimentation Table chances", "Website", "https://hypixelskyblock.minecraft.wiki"},
            {"fd:res_neu", "NotEnoughUpdates-REPO", "Item ids, names and the skull textures behind every icon", "GitHub", "https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO"},
            {"fd:res_hypixel", "Hypixel Public API", "Bazaar and lowest BIN prices for drop values", "Website", "https://api.hypixel.net"},
            {"fd:res_fabric", "FabricMC", "The loader, API and Loom this mod is built on", "GitHub", "https://github.com/FabricMC"},
            {"fd:res_mc", "Minecraft (Mojang Studios)", "The game itself, and the vanilla sounds some scenes use", "Website", "https://www.minecraft.net"}};
        for (String[] rs : res) {
            Row rr = row(0, rs[0], rs[1], rs[2], Ui.ACCENT); rr.noSearch = true;
            final String url = rs[4];
            Runnable go = () -> { try { net.minecraft.client.gui.screens.ConfirmLinkScreen.confirmLinkNow(this, java.net.URI.create(url)); } catch (Throwable ignored) {} };
            rr.click = go; rr.ctls.add(new Btn(rs[3], 0xFF3A4FA0, go));
            rows.add(rr);
        }
    }

    private static final String[][] LANDING = {
        {},
        {"fd:library", "Drop Library", "Every drop the mod knows about, grouped by where it comes from. Open a page to choose which drops get an animation and which get a chat alert.",
            "fd:slayers|Slayers|Slayer boss drops", "fd:mytho|Mythological|Ritual creatures", "fd:dye|Dye|RNG dyes", "fd:farm|Farming|Crops and pests", "fd:experiments|Experiments|Experimentation Table", "fd:mining|Mining|Coming soon", "fd:fishing|Fishing|Coming soon"},
        {"fd:animations", "Animations", "Pick the scene, sounds and look of the animation for each kind of drop, and try them out with the play buttons.",
            "fd:slayers|Slayers|Boss scenes", "fd:mytho|Mythological|Lightning ritual", "fd:dye|Dye|Paint scene", "fd:farm|Farming|Scythe and sunrise", "fd:experiments|Experiments|Book scene", "fd:mining|Mining|Coming soon", "fd:fishing|Fishing|Coming soon"},
        {"fd:sounds", "Sounds", "Volume, and the sound of every moment of every animation, with a live wave preview.",
            "fd:slayers|Slayers|Boss sounds", "fd:mytho|Mythological|Ritual sounds", "fd:dye|Dye|Paint sounds", "fd:farm|Farming|Scythe sounds", "fd:experiments|Experiments|Book sounds", "fd:mining|Mining|Coming soon", "fd:fishing|Fishing|Coming soon"},
        {},
        {"fd:settings", "Settings", "How the menu looks, where prices come from and how drop data is loaded. Position, size and text for each animation now live under Animations.",
            "fd:glass|Menu|Look and feel", "fd:coin|Prices|Bazaar and Auction House", "fd:refresh|Drop Data|Reload data"},
        {"fd:stats", "Stats", "Your profit, kills and drops: totals, profit per hour, this session and what you've found in every category.",
            "fd:stats|Overview|Totals", "fd:slayers|Slayers|Per boss", "fd:mytho|Mythological|Found drops", "fd:dye|Dye|Found dyes", "fd:farm|Farming|Found drops", "fd:clock|History|This session"},
        {},
        {}};

    private String subIcon(int k) { String[] d = LANDING[tab.ordinal()]; return d.length > 3 + k ? d[3 + k].split("\\|")[0] : "fd:note"; }

    /** The first page of a tab that has sub pages: what the tab is for, and one big button for each sub page. */
    private void buildLanding(Tab t) {
        String[] d = LANDING[t.ordinal()];
        Row hero = row(0, d[0], d[1], d[2], Ui.ACCENT);
        hero.kind = HERO; hero.h = 96; hero.noSearch = true; rows.add(hero);
        String[] names = SUBS[t.ordinal()];
        String[] lb = new String[names.length], ls = new String[names.length], li = new String[names.length]; int[] lc = new int[names.length]; boolean[] lw = new boolean[names.length];
        for (int k = 0; k < names.length; k++) {
            String[] p = d[3 + k].split("\\|");
            li[k] = p[0]; lb[k] = p[1]; ls[k] = p[2];
            boolean cat = t == Tab.DROPS || t == Tab.ANIMATIONS || t == Tab.SOUNDS;
            lw[k] = cat && isWip(names[k]);
            lc[k] = cat ? catCol(names[k]) : PICK_PALETTE[k % PICK_PALETTE.length];
        }
        if (t == Tab.ANIMATIONS) animChanges(rows, FancyConfig.get());                        // the look every animation shares sits at the top of the main page
        if (t == Tab.SOUNDS && !soundMaster(rows, FancyConfig.get())) return;      // sounds off: only the switch
        rows.add(picker(landingTitle(t), landingHint(t, names.length), lb, ls, li, lc, lw, 2, () -> -1, k -> { dropSrc = null; subSel[t.ordinal()] = k; scroll = 0; tabChanged = System.currentTimeMillis(); open.removeIf(o -> o.startsWith("dd_")); }));
    }

    private static String landingTitle(Tab t) {
        return switch (t) { case DROPS -> "Drop categories"; case ANIMATIONS -> "Animation scenes"; case STATS -> "Stats pages"; case GENERAL -> "Settings pages"; default -> "Pages"; };
    }
    /** The line on the right of a landing's tile box: a live fact about that tab. */
    private static String landingHint(Tab t, int pages) {
        FancyConfig c = FancyConfig.get();
        switch (t) {
            case DROPS -> {
                int n = 0;
                for (SlayerData.Slayer s : SlayerData.ALL) n += s.drops.size();
                for (SlayerData.Slayer s : FarmData.ALL) n += s.drops.size();
                for (SlayerData.Slayer s : MythData.ALL) n += s.drops.size();
                n += DyeData.ALL.size() + ExperimentData.ALL.length;
                return n + " drops tracked";
            }
            case ANIMATIONS -> { int n = 0; for (String[][] a : ATYPES) n += a.length; return n + " scenes  ·  animations " + (c.masterAnimate ? "on" : "OFF"); }
            case STATS -> { try { return coinsText(ProfitTracker.coins(ProfitTracker.overall(ProfitTracker.ALL))) + " profit"; } catch (Throwable e) { return pages + " pages"; } }
            default -> { return pages + " pages"; }
        }
    }

    private void statsOverview(List<Row> rows, long now) {
        String all = ProfitTracker.ALL;
        ProfitTracker.Stat ss = ProfitTracker.session(all), os = ProfitTracker.overall(all);
        double total = ProfitTracker.coins(os), sv = ProfitTracker.coins(ss);
        Row hero = row(0, "fd:coin", coinsText(total), "Total profit  ·  " + os.drops + " drops", Ui.ACCENT);
        hero.kind = HERO; hero.h = 84; hero.noSearch = true; rows.add(hero);
        long mins = ProfitTracker.activeMs(all, now) / 60_000;
        statsProfit(rows, total, sv, ProfitTracker.perHour(all, now), mins < 1 ? "Since you started the game" : "Since you started the game  ·  " + (mins >= 60 ? (mins / 60) + "h " + (mins % 60) + "m" : mins + " min") + " played", Ui.ACCENT);

        rows.add(section("Categories"));
        String[] lb = new String[4], ts = new String[4], ic = new String[4]; int[] cl = new int[4], goTo = new int[4];
        for (int di = 0; di < 4; di++) {                                                       // one tile per category: click to open its page
            final int ci = new int[]{0, 3, 1, 2}[di];
            List<ProfitTracker.Item> so = catItems(os, ci);
            lb[di] = STAT_CATS[ci]; ic[di] = STAT_ICON[ci]; cl[di] = STAT_COL[ci];
            ts[di] = amountOf(so) + " drops  ·  " + coinsText(valueOf(so));
            goTo[di] = ci == 0 ? 1 : ci == 3 ? 2 : ci == 1 ? 3 : 4;
        }
        rows.add(picker("Open a category", "Your drops and what they are worth", lb, ts, ic, cl, null, 4, () -> -1, i2 -> goStats(goTo[i2])));
        rows.add(section("Drops"));
        statsDropList(rows, os, ss, -1);
    }

    private void statsDropList(List<Row> rows, ProfitTracker.Stat os, ProfitTracker.Stat ss, int cat) {
        List<ProfitTracker.Item> items = catItems(os, cat);
        for (ProfitTracker.Item it : items) {
            ProfitTracker.Item si = ss.items.get(it.name);
            Row x = row(0, it.icon, it.name, it.amount + " in total  ·  " + (si == null ? 0 : si.amount) + " this session", it.color);
            x.iconId = it.id; x.cell = CELL;
            double v = ProfitTracker.value(it);
            x.ctls.add(new Val(() -> it.untradeable ? "untradeable" : v > 0 ? PriceService.fmt(v) : "?", 80));
            rows.add(x);
        }
        if (items.isEmpty()) rows.add(row(0, "fd:nothing", "Nothing To Show :(", "Drops you get will be listed here with what they're worth", Ui.SUB));
    }

    private void statsSlayers(List<Row> rows, FancyConfig c, long now) {
        String id = c.profitHudSlayer == null || c.profitHudSlayer.isEmpty() ? ProfitTracker.ALL : c.profitHudSlayer;
        boolean every = id.equals(ProfitTracker.ALL);
        List<String> ids = new ArrayList<>(); ids.add(ProfitTracker.ALL);
        for (SlayerData.Slayer sl : SlayerData.ALL) ids.add(sl.id);
        ProfitTracker.Stat ss = ProfitTracker.session(id), os = ProfitTracker.overall(id);
        SlayerData.Slayer sel = slayerById(id);
        int col = sel == null ? STAT_COL[0] : sel.color();
        String cur = ProfitTracker.current();
        List<ProfitTracker.Item> so = catItems(os, 0), ssn = catItems(ss, 0);
        double ov = valueOf(so) - os.costs, sv = valueOf(ssn) - ss.costs;
        Row hero = row(0, sel == null ? "fd:slayers" : bossIcon(sel), sel == null ? "All slayers" : sel.boss, coinsText(ov) + " all time  ·  " + os.kills + " kills  ·  " + amountOf(so) + " drops", col);
        hero.kind = HERO; hero.h = 84; hero.noSearch = true; rows.add(hero);

        Row pick = row(0, sel == null ? "fd:slayers" : bossIcon(sel), "Slayer", cur == null ? "Which slayer these numbers are for" : "Which slayer these numbers are for  ·  you're on " + slayerName(cur), col);
        Dd pdd = new Dd(() -> ProfitTracker.label(id), "dd_profit", 124);
        pick.ctls.add(pdd); rows.add(pick);
        if (open.contains("dd_profit") && !searching) {
            List<String> labels = new ArrayList<>();
            for (String pid : ids) labels.add(ProfitTracker.label(pid));
            rows.add(miniList(pick, pdd, labels, every ? 0 : ids.indexOf(id), i -> { c.profitHudSlayer = ids.get(i); open.remove("dd_profit"); }));
        }
        long mins = ProfitTracker.activeMs(id, now) / 60_000;
        statsProfit(rows, ov, sv, ProfitTracker.perHour(id, now), "Slayer drops at today's prices" + (ss.costs > 0 ? ", minus " + PriceService.fmt(ss.costs) + " for quests" : "") + (mins > 0 ? "  ·  " + mins + " min of slaying" : ""), col);

        rows.add(section("Kills"));
        Row kl = row(0, "fd:slayers", "Kills", "Bosses killed this session  ·  " + os.kills + " in total", col);
        kl.ctls.add(new Val(() -> ss.kills + " this session", 120)); rows.add(kl);
        Row dr = row(0, "fd:dice", "Drops", "Tracked drops this session  ·  " + amountOf(so) + " in total", col);
        dr.ctls.add(new Val(() -> amountOf(ssn) + " this session", 120)); rows.add(dr);
        if (os.kills > 0 && !so.isEmpty()) {
            Row ev = row(0, "fd:rng", "Coins per kill", "Average over every kill you've done", col);
            double per = ov / os.kills;
            ev.ctls.add(new Val(() -> PriceService.fmt(per), 120)); rows.add(ev);
        }
        if (every) {
            rows.add(section("By slayer"));
            for (SlayerData.Slayer sl : SlayerData.ALL) {
                ProfitTracker.Stat st = ProfitTracker.overall(sl.id), sst = ProfitTracker.session(sl.id);
                Row r = row(0, bossIcon(sl), sl.boss, st.kills + " kills  ·  " + sst.kills + " this session  ·  " + amountOf(catItems(st, 0)) + " drops", sl.color());
                double v = ProfitTracker.coins(st);
                r.ctls.add(new Val(() -> coinsText(v), 120));
                r.click = () -> c.profitHudSlayer = sl.id;
                rows.add(r);
            }
        }
        rows.add(section("Drops"));
        statsDropList(rows, os, ss, 0);
    }

    private void statsCategory(List<Row> rows, int cat, long now) {
        ProfitTracker.Stat ss = ProfitTracker.session(ProfitTracker.ALL), os = ProfitTracker.overall(ProfitTracker.ALL);
        List<ProfitTracker.Item> so = catItems(os, cat), ssn = catItems(ss, cat);
        int total = 0;
        if (cat == 1) total = DyeData.ALL.size(); else for (SlayerData.Slayer s : cat == 3 ? MythData.ALL : FarmData.ALL) total += s.drops.size();
        String name = STAT_CATS[cat];
        double val = valueOf(so), sv = valueOf(ssn);
        Row hero = row(0, STAT_ICON[cat], name, coinsText(val) + " all time  ·  " + amountOf(so) + " drops  ·  " + so.size() + " of " + total + (cat == 1 ? " dyes" : cat == 3 ? " drops" : " items") + " found", STAT_COL[cat]);
        hero.kind = HERO; hero.h = 84; hero.noSearch = true; rows.add(hero);
        double hrs = ProfitTracker.activeMs(ProfitTracker.ALL, now) / 3_600_000.0;
        statsProfit(rows, val, sv, hrs * 60 >= 1 ? sv / hrs : -1, "Worth of the " + name.toLowerCase(java.util.Locale.ROOT) + " drops you got since you started the game", STAT_COL[cat]);

        rows.add(section("Collection"));
        Row d = row(0, "fd:dice", "Drops", amountOf(ssn) + " this session  ·  " + amountOf(so) + " in total", STAT_COL[cat]);
        int tot = amountOf(so);
        d.ctls.add(new Val(() -> tot + " total", 120)); rows.add(d);
        int found = so.size(), all = total;
        Row cl = row(0, "fd:cube", "Found", cat == 1 ? "Different dyes you've found" : cat == 3 ? "Different mythological drops you've found" : "Different farming drops you've found", STAT_COL[cat]);
        cl.ctls.add(new Val(() -> found + " / " + all + (all > 0 ? "  (" + Math.round(found * 100f / all) + "%)" : ""), 120)); rows.add(cl);

        if (cat == 2 || cat == 3) {
            rows.add(section(cat == 3 ? "Creatures" : "Sources"));
            for (SlayerData.Slayer s : cat == 3 ? MythData.ALL : FarmData.ALL) {
                ProfitTracker.Stat st = ProfitTracker.overall(s.id), sst = ProfitTracker.session(s.id);
                List<ProfitTracker.Item> l = catItems(st, -1);
                Row r = row(0, cat == 3 ? "fd:myth_" + s.id : "fd:farm_" + s.id, s.boss, sst.drops + " this session  ·  " + st.drops + " in total  ·  " + l.size() + " of " + s.drops.size() + " found", s.color());
                double v = valueOf(l);
                r.ctls.add(new Val(() -> coinsText(v), 120)); rows.add(r);
            }
        }
        rows.add(section("Drops"));
        statsDropList(rows, os, ss, cat);
    }

    private void statsHistory(List<Row> rows) {
        List<DropListener.Rec> l = new ArrayList<>(DropListener.LEDGER); Collections.reverse(l);
        rows.add(section("This session, newest first"));
        if (l.isEmpty()) { rows.add(row(0, "fd:nothing", "Nothing To Show :(", "Every drop you get this session shows up here as it happens", Ui.SUB)); return; }
        long now = System.currentTimeMillis();
        for (DropListener.Rec r : l) {
            int ci = statCat(r.item());
            long mins = Math.max(0, (now - r.time()) / 60_000);
            String ago = mins < 1 ? "just now" : mins < 60 ? mins + " min ago" : (mins / 60) + "h " + (mins % 60) + "m ago";
            Row x = row(0, STAT_ICON[ci], r.item(), r.slayer() + "  ·  " + ago, STAT_COL[ci]);
            x.ctls.add(new Val(() -> r.price() >= 0 ? PriceService.fmt(r.price()) : "?", 80));
            rows.add(x);
        }
    }

    private static String slayerName(String id) { SlayerData.Slayer s = slayerById(id); return s == null ? id : s.boss; }
    private static SlayerData.Slayer slayerById(String id) { for (SlayerData.Slayer s : SlayerData.ALL) if (s.id.equals(id)) return s; return null; }

    /** The "all at once" row on top of a boss / source: one switch for every Animate, one for every Announce, lined up with the columns below. */
    private Row masterRow(FancyConfig.SlayerCfg sc, SlayerData.Slayer s, FancyConfig c) {
        Row all = row(2, "fd:switch", "Every " + s.boss + " drop", "Animation and chat switches for every drop below", s.color());
        all.cell = CELL;
        all.ctls.add(tog(() -> anyAnimate(s, c), v -> setEvery(sc, s, c, v, null), 0xFF4A6CF0));
        all.ctls.add(tog(() -> anyAnnounce(s, c), v -> setEvery(sc, s, c, null, v), 0xFFD98A1F));
        return all;
    }
    private static FancyConfig.DropCfg cfgOf(SlayerData.Slayer s, SlayerData.Drop d, FancyConfig c) { return MythData.ALL.contains(s) ? MythData.cfg(c, s, d) : c.drop(s.id, d.name); }
    private static boolean anyAnimate(SlayerData.Slayer s, FancyConfig c) { for (SlayerData.Drop d : s.drops) if (cfgOf(s, d, c).animate) return true; return false; }
    private static boolean anyAnnounce(SlayerData.Slayer s, FancyConfig c) { for (SlayerData.Drop d : s.drops) if (cfgOf(s, d, c).announce) return true; return false; }
    private static void setEvery(FancyConfig.SlayerCfg sc, SlayerData.Slayer s, FancyConfig c, Boolean animate, Boolean announce) {
        sc.enabled = true; sc.animate = true; sc.announce = true;      // per-drop switches decide; the slayer-level ones stay out of the way
        for (SlayerData.Drop d : s.drops) { FancyConfig.DropCfg dc = cfgOf(s, d, c); if (animate != null) dc.animate = animate; if (announce != null) dc.announce = announce; }
    }

    private void insertPlaceholder(String token) {
        String v = formatField.getValue();
        if (v.length() + token.length() + 1 > ChatFilter.MAX) return;
        formatField.setValue(v.isEmpty() || v.endsWith(" ") ? v + token : v + " " + token);
    }

    private void layout(Row r) {
        r.x = cx + r.indent * 12; r.w = cw - r.indent * 12;
        int edge = r.x + r.w - 8;
        if (r.arrow) edge -= 14;
        for (int i = r.ctls.size() - 1; i >= 0; i--) {
            Ctl c = r.ctls.get(i);
            if (r.cell > 0) {                      // fixed column, control centred in it
                int mid = edge - r.cell / 2;
                c.x0 = mid - c.width() / 2; c.x1 = c.x0 + c.width();
                edge -= r.cell;
            } else { c.x1 = edge; c.x0 = edge - c.width(); edge = c.x0 - 8; }
        }
        if (r.fieldBtn != null) { r.btnX1 = r.x + r.w - 8; r.btnX0 = r.btnX1 - 40; }
    }

    // ------------------------------------------------------------------ render

    /** Menu order of the categories: Slayers, Mythological, Dye, Farming, Experiments, Mining, Fishing (the numbers inside the mod stay as they are). */
    private static final int[] ORD = {0, 3, 1, 2, 4, 5, 6};
    private static int ordPos(int cat) { for (int i = 0; i < ORD.length; i++) if (ORD[i] == cat) return i; return 0; }
    private static final String[] CATS = {"Slayers", "Dye", "Farming", "Mythological", "Experiments", "Mining", "Fishing"};
    /** Pages that are still being built show a WIP label in the sidebar. */
    private static boolean isWip(String name) { return name.equals("Mining") || name.equals("Fishing"); }
    private static double pctOf(String chance) { var m = java.util.regex.Pattern.compile("\\(([0-9.]+)%\\)").matcher(chance); return m.find() ? Double.parseDouble(m.group(1)) : 0; }
    /** 0 Rare Items (the Extraordinary ones), 1 RNG Items (everything else that isn't a book), 2 Enchantment Books. */
    private static int expGroup(String[] e) {
        if (e[0].matches(".* [IVX]+$")) return 2;
        return e[3].equals("Extraordinary") ? 0 : 1;
    }
    private static int catCol(String name) {
        return switch (name) { case "Slayers" -> 0xFF8FA8FF; case "Dye" -> 0xFFFF72C6; case "Farming" -> 0xFF7BE05A; case "Mythological" -> 0xFF55D8FF;
            case "Mining" -> 0xFFB0B6C2; case "Fishing" -> 0xFF4FA8FF; case "Experiments" -> 0xFFB56BFF; default -> Ui.ACCENT; };
    }
    private static final int SUBH = 17;
    /** Sub page row height: shrinks a little when the open page has so many that they would run off the bottom of the menu. */
    private int subH() { int n = Math.max(1, subs().length); int room = py + ph - 6 - (py + 70 + tab.ordinal() * 24 + 25); return Math.max(12, Math.min(SUBH, room / n)); }
    private final List<int[]> prevBoxes = new ArrayList<>();           // {x, y, w, h, category} of every preview drawn this frame (for dragging a custom Farming spot)
    private boolean dragFarm;
    private static final String[] CHAT_EVENTS = {"Slayers", "Dye", "Farming", "Mythological", "Experiments", "Mining", "Fishing"};
    private static int chatEv;                                    // Chat > Message format: which event's message is being edited
    private static int previewSel;                               // Settings > Scene Settings > Preview: the chosen Drop Library category
    private static String jumpTo;                                 // a row title to scroll to after the next build
    private static final int[] subSel = new int[Tab.values().length];
    static { java.util.Arrays.fill(subSel, -1); }   // the chosen sub page of every tab
    private final boolean[] searchVis = new boolean[Tab.values().length];      // while searching: which pages have something that matches
    private final List<int[]> searchSide = new ArrayList<>();               // the sidebar entries of the last frame while searching: {tab, sub (-1 = the page itself), x0, y0, x1, y1}
    private int searchSideH;
    private float subExp;                                         // 0..1: how far the current tab's list has opened in the sidebar
    private static final String[][] SUBS = {
        {},                                                        // Home
        {"Slayers", "Mythological", "Dye", "Farming", "Experiments", "Mining", "Fishing"},   // Drop Library
        {"Slayers", "Mythological", "Dye", "Farming", "Experiments", "Mining", "Fishing"},   // Animations
        {"Slayers", "Mythological", "Dye", "Farming", "Experiments", "Mining", "Fishing"},   // Sounds: the main page holds the volume, then one sound page per animation
        {},                                                        // Chat (one page)
        {"Menu", "Prices", "Drop Data"},     // Settings
        {"Overview", "Slayers", "Mythological", "Dye", "Farming", "History"},
        {},                                                        // Cosmetics (one page)
        {}};                                                        // Resources                                      // Stats
    private String[] subs() { return SUBS[tab.ordinal()]; }
    private boolean onLanding() { return subs().length > 0 && subSel[tab.ordinal()] < 0 && query.isBlank(); }
    private int curSub() { String[] n = subs(); return n.length == 0 ? 0 : Math.max(0, Math.min(n.length - 1, subSel[tab.ordinal()])); }

    private int sideScroll;                                   // the sidebar scrolls when the tabs and sub pages are taller than the menu
    private int srcExtra() { return tab == Tab.DROPS ? Math.round(srcExp * SRCH * srcList.size()) : 0; }
    private static final int SRCH = 14;
    private int sideMax() { if (!query.isBlank()) return Math.max(0, searchSideH - (ph - 70 - 8)); return Math.max(0, Tab.values().length * 24 + 4 + srcExtra() + Math.round(subExp * subH() * subs().length) - (ph - 70 - 8)); }
    private boolean inSide(double mx, double my) { return mx >= px && mx < px + sw && my >= py + 66 && my < py + ph - 4; }
    private int[] tabBox(int i) {
        int y = py + 70 - sideScroll + i * 24 + (i > tab.ordinal() ? Math.round(subExp * subH() * subs().length) + srcExtra() : 0);
        return new int[]{px + 8, y, px + sw - 6, y + 22};
    }
    private int[] srcBox(int j) { int[] b = subBox(curSub()); int y = b[1] + subH() + j * SRCH; return new int[]{px + 26, y, px + sw - 6, y + SRCH - 1}; }
    private int[] subBox(int k) { int y = tabBox(tab.ordinal())[1] + 25 + k * subH() + (tab == Tab.DROPS && k > curSub() ? srcExtra() : 0); return new int[]{px + 16, y, px + sw - 6, y + subH() - 1}; }

    /** Keeps only the rows under the chosen sub heading (the headings themselves become the sidebar list). */
    private void filterSub() {
        String[] names = subs();
        if (names.length == 0 || tab == Tab.DROPS || tab == Tab.ANIMATIONS || tab == Tab.SOUNDS || tab == Tab.STATS) return;
        int sel = curSub(), cur = -1;
        List<Row> keep = new ArrayList<>();
        for (Row r : rows) {
            if (r.kind == SECTION) {
                int m = -1;
                for (int k = 0; k < names.length; k++) if (r.title.startsWith(names[k])) { m = k; break; }
                if (m >= 0) { cur = m; continue; }
            }
            if (cur == sel) keep.add(r);
        }
        rows.clear(); rows.addAll(keep);
    }

    /** While an animation plays the world behind the menu stays sharp and undimmed, so the menu sits on top of the animation. */
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (AnimationManager.isPlaying()) return;
        super.extractBackground(g, mx, my, delta);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx0, int my0, float delta) {
        final int mx = editor != 0 ? -9999 : mx0, my = editor != 0 ? -9999 : my0;       // an open editor sits on top: nothing behind it lights up
        formatField.setVisible(false); addField.setVisible(false);
        searchField.setVisible(false); numShow = false; lastMx = mx;
        if (dragCtl instanceof Slider ls && ls.live) {   // dragging a size slider: show the scene resizing live
            g.fill(0, 0, width, height, 0x55000000);
            AnimationManager.drawPreview(g, width, height, Math.min(previewSel, 4));
            return;
        }

        FancyConfig cfg = FancyConfig.get();
        long now = System.currentTimeMillis();
        dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f); lastFrame = now;
        Icons.animated = cfg.menuIcons;
        boolean animOn = cfg.menuAnimated;
        if (closeAt > 0 && now - closeAt >= CLOSE_MS) { closeAt = -1; onClose(); return; }
        float intro = animOn ? ease((now - born) / (float) OPEN_MS) : 1f;
        if (closeAt > 0) intro = Math.min(intro, 1f - easeIn((now - closeAt) / (float) CLOSE_MS));      // Esc: fade out
        STILL = !animOn;
        if (tabChanged != seenTabChange) { seenTabChange = tabChanged; pageK = Math.min(pageK, 0.55f); }          // a new page only dips part-way, so fast clicking never snaps back to nothing
        pageK = animOn ? Math.min(1f, pageK + dt * 6.5f) : 1f;
        float pageIn = ease(pageK);
        TR = 1f - cfg.menuTranslucency / 100f;
        cosK = anim("cosK", cosPanels() ? 1f : 0f, 14f);
        int basePw = Math.min(560, width - 76);
        pw = Math.round(basePw + (Math.min(basePw, width - 2 * (AURA_W + DOCK_GAP + 6)) - basePw) * cosK);       // the window stays in the middle; it only gets a little smaller to leave room for the side menus
        px = (width - pw) / 2; cx = px + sw + 10; cw = pw - sw - 20 - RS;
        ph = Math.round(menuHeight(height) + (Math.min(350, height - 2 * (COS_H + DOCK_GAP + 6)) - menuHeight(height)) * cosK);
        dockK = anim("dock", dockWanted() ? 1f : 0f, 14f);
        int dockExtra = Math.round((DOCK_H + DOCK_GAP) * dockK);
        py = (height - ph - dockExtra) / 2 + (FancyConfig.get().previewAt == 0 ? dockExtra : 0) + Math.round((1f - intro) * 10f);        // the panel glides up into place; the live preview takes the room above or below it
        lt = py + 46; lb = py + ph - 10;

        int viewH = lb - lt;
        build();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, totalH - viewH)));
        build();

        g.fill(0, 0, width, height, Gfx.argb(Math.round((0x28 + 0x78 * TR) * intro), 0x000000));
        float zoom = 0.94f + 0.06f * intro;                                    // the whole menu grows into place (and shrinks away on Esc)
        Gfx.push(g); Gfx.translate(g, px + pw / 2f, py + ph / 2f); Gfx.scale(g, zoom); Gfx.translate(g, -(px + pw / 2f), -(py + ph / 2f));
        pill(g, px, py, pw, ph, Ui.PANEL);
        g.fill(px + 1, py + 2, px + sw, py + ph - 1, soft(Ui.PANEL2));
        g.fill(px + sw, py + 4, px + sw + 1, py + ph - 4, Ui.BORDER);
        int[] grad = {0xFF42D9FF, 0xFF6578FF, 0xFFB66CFF, 0xFFFF6EB5};
        float flow = animOn ? (now % 5000L) / 5000f : 0f;                    // the colours slowly flow along the top edge
        for (int x = px + 1; x < px + pw - 1; x += 3) {
            float u = (((x - px) / (float) pw + flow) % 1f) * grad.length;
            int i0 = (int) u % grad.length;
            g.fill(x, py, Math.min(px + pw - 1, x + 3), py + 2, Canvas.mix(grad[i0], grad[(i0 + 1) % grad.length], u - (int) u));
        }
        g.outline(px, py, pw, ph, Ui.BORDER);

        // brand
        g.blit(RenderPipelines.GUI_TEXTURED, ICON, px + 8, py + 8, 0f, 0f, 26, 26, 128, 128, 128, 128, 0xFFFFFFFF);
        // the name drops in letter by letter, bounces once, and stays still
        {
            float age = born == 0L || !animOn ? 99f : (now - born) / 1000f;
            int[] pal = {0xFF56D8FF, 0xFF6885FF, 0xFFB56BFF, 0xFFFF72C6, 0xFFFFB86B};
            String title = "FancyDrops"; int tx = px + 40;
            for (int i = 0; i < title.length(); i++) {
                String ch = String.valueOf(title.charAt(i)); int cwid = font.width(ch);
                float lt0 = age - 0.12f - i * 0.05f;                                  // each letter lands a moment after the one before
                if (lt0 >= 0f) {
                    float off = lt0 < 1.2f ? -22f * (float) (Math.exp(-4.5 * lt0) * Math.abs(Math.cos(lt0 * 9.5))) : 0f;
                    float sweep = STILL ? -99f : ((now % 1700L) / 1700f) * (title.length() + 10) - 5f;           // a light blinks along the name, over and over
                    float shine = Math.max(0f, 1f - Math.abs(i - sweep) / 2.6f);
                    int col = Canvas.mix(pal[i % pal.length], 0xFFFFFFFF, shine * 0.85f);
                    Gfx.text(g, font, ch, tx, Math.round(py + 10 + off - shine * 1.2f), col);
                }
                tx += cwid;
            }
        }
        Gfx.text(g, font, "v" + FancyDropsMod.VERSION + "  \u00B7  " + FancyDropsMod.CHANNEL, px + 40, py + 22, Ui.DIM);

        if (!query.isBlank()) drawSearchSide(g, mx, my, now);
        else {
        // sidebar tabs: the highlight glides from tab to tab
        sideScroll = Math.max(0, Math.min(sideMax(), sideScroll));
        g.enableScissor(px + 4, py + 66, px + sw, py + ph - 4);
        float want = tabBox(tab.ordinal())[1];
        selY = Float.isNaN(selY) || !animOn ? want : selY + (want - selY) * (1f - (float) Math.exp(-dt * 16f));
        if (Math.abs(want - selY) < 0.3f) selY = want;
        int sy = Math.round(selY);
        pill(g, px + 8, sy, sw - 14, 22, Ui.ROW);
        tabAcc = Canvas.mix(tabAcc == 0 ? tabCol(tab) : tabAcc, tabCol(tab), animOn ? 0.2f : 1f);
        g.fill(px + 8, sy + 3, px + 10, sy + 19, tabAcc);
        for (int i = 0; i < Tab.values().length; i++) {
            Tab t = Tab.values()[i]; int[] b = tabBox(i);
            boolean act = t == tab, hov = mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3];
            float hv = anim("tab|" + i, hov && !act ? 1f : 0f, 48f);
            if (hv > 0.01f) pill(g, b[0], b[1], b[2] - b[0], b[3] - b[1], Canvas.mix(Ui.PANEL2, 0xFF1D1E27, hv));
            int dx = 0;
            iconBack(g, b[0] + 14 + dx, b[1] + 11, 9, act, tabCol(t));
            tabIcon(g, t, b[0] + 6 + dx, b[1] + 3, act && cfg.menuIcons ? now : 2400L, act);   // only the open page's icon moves
            Gfx.text(g, font, t.label, b[0] + 28 + dx, b[1] + 7, act ? Canvas.mix(tabCol(t), 0xFFFFFFFF, 0.35f) : Canvas.mix(Canvas.mix(0xFF8A8A96, tabCol(t), 0.45f), 0xFFFFFFFF, hv * 0.4f));
        }

        subExp = anim("subExp", subs().length > 0 && query.isBlank() ? 1f : 0f, 40f);
        srcExp = anim("srcExp", tab == Tab.DROPS && !onLanding() && !srcList.isEmpty() && query.isBlank() ? 1f : 0f, 30f);
        String[] sn = subs();
        if (subExp > 0.02f && sn.length > 0) {                                      // the open tab lists its own sub pages under it
            int top = tabBox(tab.ordinal())[1] + 24;
            g.enableScissor(px + 8, top, px + sw - 6, top + Math.round(subExp * (subH() * sn.length + srcExtra())));
            g.fill(px + 12, top, px + 13, top + subH() * sn.length + srcExtra() - 2, Canvas.mix(Ui.BORDER, subCol(tab), 0.45f));
            for (int k = 0; k < sn.length; k++) {
                int[] b = subBox(k);
                boolean wip = (tab == Tab.DROPS || tab == Tab.ANIMATIONS || tab == Tab.SOUNDS) && isWip(sn[k]); int dot = (tab == Tab.DROPS || tab == Tab.ANIMATIONS || tab == Tab.SOUNDS) ? catCol(sn[k]) : Ui.ACCENT;
                boolean act = !onLanding() && curSub() == k, hov = mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3];
                float hv = anim("sub|" + k, hov && !act ? 1f : 0f, 48f);
                int sc2 = subCol(tab);
                if (act) { pill(g, b[0], b[1], b[2] - b[0], b[3] - b[1], Canvas.mix(Ui.ROW, sc2, 0.22f)); g.fill(b[0], b[1] + 2, b[0] + 2, b[3] - 2, sc2); }
                else if (hv > 0.01f) pill(g, b[0], b[1], b[2] - b[0], b[3] - b[1], Canvas.mix(Ui.PANEL2, 0xFF1D1E27, hv));
                int dx = 0;
                iconBack(g, b[0] + 10 + dx, b[1] + 8, 7, act, sc2);
                Gfx.push(g); Gfx.translate(g, b[0] + 4 + dx, b[1] + 2); Gfx.scale(g, 0.8f);
                Icons.draw(g, subIcon(k), 0, 0, act && cfg.menuIcons ? now : 2400L);
                Gfx.pop(g);
                int availW = b[2] - b[0] - 26 - (wip ? 18 : 0), lw = font.width(sn[k]);
                float fit = lw > availW ? Math.max(0.6f, availW / (float) lw) : 1f;                  // a long name shrinks to fit instead of being cut off
                Gfx.push(g); Gfx.translate(g, b[0] + 21 + dx, b[1] + 5 + (fit < 1f ? (1f - fit) * 4f : 0f)); Gfx.scale(g, fit);
                Gfx.text(g, font, sn[k], 0, 0, act ? Ui.TEXT : Canvas.mix(Canvas.mix(0xFF8A8A96, sc2, 0.4f), 0xFFFFFFFF, hv * 0.4f));
                Gfx.pop(g);
                if (wip) { Gfx.push(g); Gfx.translate(g, b[2] - 17, b[1] + 6); Gfx.scale(g, 0.75f); Gfx.text(g, font, "WIP", 0, 0, 0xFFB08A20); Gfx.pop(g); }
            }
            if (tab == Tab.DROPS && srcExp > 0.02f) {                                  // the open page lists its bosses / sources, the open one lit
                g.enableScissor(px + 8, srcBox(0)[1], px + sw - 6, srcBox(0)[1] + srcExtra());
                for (int j = 0; j < srcList.size(); j++) {
                    String[] e = srcList.get(j); int[] b = srcBox(j);
                    boolean act = e[0].equals(dropSrc), hov = mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3];
                    float hv = anim("src|" + j, hov && !act ? 1f : 0f, 48f);
                    int col = Integer.parseInt(e[3]);
                    if (act) { pill(g, b[0], b[1], b[2] - b[0], b[3] - b[1], Canvas.mix(Ui.ROW, Ui.ACCENT, 0.22f)); g.fill(b[0], b[1] + 2, b[0] + 2, b[3] - 2, Ui.ACCENT); }
                    else if (hv > 0.01f) pill(g, b[0], b[1], b[2] - b[0], b[3] - b[1], Canvas.mix(Ui.PANEL2, 0xFF1D1E27, hv));
                    int availW = b[2] - b[0] - 8, lw = font.width(e[1]); float fit = lw > availW ? Math.max(0.55f, availW / (float) lw) : 1f;
                    if (e[2] != null) { Gfx.push(g); Gfx.translate(g, b[0] + 2, b[1] + 1); Gfx.scale(g, 0.75f); if (Icons.has(e[2])) Icons.draw(g, e[2], 0, 0, act || hv > 0.1f ? now : 2400L); else Gfx.item(g, e[2], 0, 0); Gfx.pop(g); }
                    availW -= 12; fit = lw > availW ? Math.max(0.55f, availW / (float) lw) : 1f;
                    Gfx.push(g); Gfx.translate(g, b[0] + 17, b[1] + 3 + (fit < 1f ? (1f - fit) * 3f : 0f)); Gfx.scale(g, 0.9f * fit);
                    Gfx.text(g, font, e[1], 0, 0, act ? Ui.TEXT : Canvas.mix(0xFF8A8A96, 0xFFE0E0E8, hv));
                    Gfx.pop(g);
                }
                g.disableScissor();                                                 // closes the boss list's own clip
            }
            g.disableScissor();
        }
        g.disableScissor();
        }
        if (sideMax() > 0) {                                                       // a slim scroll bar shows there is more below
            int trk = ph - 76, bar = Math.max(14, trk * trk / (trk + sideMax())), by0 = py + 68 + Math.round((trk - bar) * (sideScroll / (float) sideMax()));
            g.fill(px + sw - 3, py + 68, px + sw - 1, py + 68 + trk, 0x22FFFFFF); g.fill(px + sw - 3, by0, px + sw - 1, by0 + bar, 0x99B0B8D0);
        }

        // the main switch sits right under the title: one wide glowing tile that turns the whole mod on or off
        {
            FancyConfig mc = FancyConfig.get();
            boolean on = mc.masterAnimate || mc.masterAnnounce;
            int[] b = masterBox();
            boolean mh = mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3];
            float mv = anim("masterSw", on ? 1f : 0f, 36f), hv = anim("masterHv", mh ? 1f : 0f, 48f);
            int w = b[2] - b[0], h = b[3] - b[1];
            float pulse = STILL ? 0.5f : 0.5f + 0.5f * (float) Math.sin(now / 420.0);
            int tbg = Canvas.mix(Canvas.mix(Ui.PANEL2, 0xFF1D2338, mv), 0xFF23283A, hv * 0.8f);
            pill(g, b[0], b[1], w, h, tbg);
            if (mv > 0.02f) {                                                              // a soft glow breathing round the tile while the mod is on
                g.outline(b[0], b[1], w, h, Gfx.argb(Math.round((60 + 70 * pulse) * mv), 0x62E6A8));
                g.fill(b[0] + 2, b[1] + h - 2, b[2] - 2, b[1] + h - 1, Gfx.argb(Math.round((120 + 100 * pulse) * mv), 0x62E6A8));
                gleam(g, b[0], b[1], w, h, now);
            } else g.fill(b[0] + 2, b[1] + h - 2, b[2] - 2, b[1] + h - 1, Gfx.argb(150, 0xE5484D));
            Icons.draw(g, "fd:sparkle", b[0] + 4, b[1] + 3 - (STILL ? 0 : Math.round(mv * (float) Math.sin(now / 300.0) * 0.9f)), on ? now : 2400L);
            if (mv < 0.98f) g.fill(b[0] + 4, b[1] + 3, b[0] + 20, b[1] + 19, Gfx.argb(Math.round(170 * (1f - mv)), tbg & 0xFFFFFF));      // dims the icon with the tile's own colour, so no dark box shows
            Gfx.text(g, font, "Active", b[0] + 24, b[1] + 4, Canvas.mix(0xFF7A7A86, Ui.TEXT, mv));
            Gfx.text(g, font, on ? "ON" : "OFF", b[0] + 24, b[1] + 14, Canvas.mix(0xFFE5484D, 0xFF62E6A8, mv));
            // a battery instead of a switch: full and charging while the mod is on, drained (and blinking red) while it is off
            int bx0 = b[2] - 38, by0 = b[1] + 6, bw0 = 30, bh0 = 14;
            int rim = Canvas.mix(0xFF6E7488, 0xFFD6DAE6, mv * 0.7f + hv * 0.3f);
            g.fill(bx0 + bw0, by0 + 4, bx0 + bw0 + 2, by0 + bh0 - 4, rim);                         // the little terminal on the right
            g.fill(bx0, by0, bx0 + bw0, by0 + bh0, 0xFF0C0E14); g.outline(bx0, by0, bw0, bh0, rim);
            int cells = 5, cellW = (bw0 - 4) / cells;
            float charge = mv * cells;
            for (int k = 0; k < cells; k++) {
                float f = Math.max(0f, Math.min(1f, charge - k));
                int cxx = bx0 + 2 + k * cellW;
                if (f > 0.02f) {
                    int cc = Canvas.mix(0xFF3FBF7F, 0xFF7CF0B0, (float) k / cells);
                    if (mv > 0.98f && !STILL) cc = Canvas.mix(cc, 0xFFFFFFFF, 0.25f * Math.max(0f, (float) Math.sin(now / 260.0 - k * 0.8)));      // a ripple of charge runs along a full battery
                    g.fill(cxx, by0 + 2, cxx + cellW - 1, by0 + bh0 - 2, Gfx.argb(Math.round(255 * f), cc & 0xFFFFFF));
                }
            }
            if (mv < 0.5f) {                                                                         // drained: one red sliver that blinks
                float lowBlink = STILL ? 1f : 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(now / 300.0));
                g.fill(bx0 + 2, by0 + 2, bx0 + 2 + Math.max(2, cellW - 3), by0 + bh0 - 2, Gfx.argb(Math.round(230 * lowBlink * (1f - mv * 2f)), 0xE5484D));
            }
        }

        // page header
        Gfx.text(g, font, font.plainSubstrByWidth(pageTitle(), cw - 134), cx, py + 12, Ui.TEXT);
        Gfx.text(g, font, font.plainSubstrByWidth(query.isBlank() ? pageSub() : "Searching every page", cw - 134), cx, py + 25, Ui.SUB);
        g.fill(cx, py + 39, cx + cw, py + 40, Ui.BORDER);

        // rows
        popup = null;
        g.enableScissor(cx - 2, lt, cx + cw + 2, lb);
        Gfx.push(g);
        Gfx.translate(g, Math.round((1f - pageIn) * 8f), 0);
        for (Row r : rows) {
            if (r.y + r.h < lt || r.y > lb) continue;
            if (r.kind == SECTION) { drawSection(g, r); continue; }
            if (r.kind == PREVIEW) { drawPositionPreview(g, r); continue; }
            if (r.kind == TIP) { drawTip(g, r, mx, my); continue; }
            if (r.kind == PICKER) { drawPicker(g, r, mx, my); continue; }
            if (r.kind == CAT) { drawCat(g, r); continue; }
            if (r.kind == CHATPREV) { drawChatPrev(g, r); continue; }
            if (r.kind == COSPREV) { drawCosPreview(g, r); continue; }
            if (r.kind == PANEL || r.kind == HERO) {                                // a centred banner (HERO: just icon, title, words) or a "coming soon" page: icon, title, words and a hazard strip, all centred
                pill(g, r.x, r.y, r.w, r.h, Ui.ROW);
                int mid = r.x + r.w / 2; long nw = System.currentTimeMillis(); float sec = (nw % 3600000L) / 1000f;
                int bob = Math.round((float) Math.sin(sec * 2.4f) * 2f), cyi = r.y + 30 + bob;
                for (int i = 0; i < 5; i++) {                                           // sparkles orbiting the icon
                    double an = sec * 1.3 + i * Math.PI * 2 / 5; int sx2 = mid + (int) (Math.cos(an) * 30), sy2 = cyi + (int) (Math.sin(an) * 12);
                    int al = 120 + (int) (120 * Math.sin(sec * 5 + i)); g.fill(sx2, sy2, sx2 + 2, sy2 + 2, Gfx.argb(Math.max(0, Math.min(255, al)), i % 2 == 0 ? 0xFFC857 : 0x9DB2CC));
                }
                if (r.iconId != null) ItemIcons.draw(g, r.iconId, r.icon, mid, cyi, 1.8f); else if (Icons.has(r.icon)) { Gfx.push(g); Gfx.translate(g, mid - 16, cyi - 16); Gfx.scale(g, 2f); Icons.draw(g, r.icon, 0, 0, System.currentTimeMillis()); Gfx.pop(g); } else if (r.icon != null) { Gfx.push(g); Gfx.translate(g, mid - 16, cyi - 16); Gfx.scale(g, 2f); Gfx.item(g, r.icon, 0, 0); Gfx.pop(g); }
                Gfx.textCentered(g, font, r.title, mid, r.y + 56, Ui.TEXT);
                int wy = r.y + 70; for (String ln : wrap(r.sub, r.w - 60)) { Gfx.textCentered(g, font, ln, mid, wy, Ui.SUB); wy += 11; }
                if (r.kind == HERO) continue;
                // the tape: stripes crawling along, with a glowing label plate on top
                int bw2 = 168, bh2 = 14, hx = mid - bw2 / 2, hy = r.y + r.h - 26, scr = (int) (sec * 34) % 16;
                g.fill(hx - 1, hy - 1, hx + bw2 + 1, hy + bh2 + 1, 0xFF000000);
                g.enableScissor(hx, hy, hx + bw2, hy + bh2);
                for (int row2 = 0; row2 < bh2; row2++) {
                    for (int k = -2; k < bw2 / 16 + 3; k++) {
                        int x0 = hx + k * 16 + scr - row2; g.fill(x0, hy + row2, x0 + 8, hy + row2 + 1, 0xFFE0A21B);
                        g.fill(x0 + 8, hy + row2, x0 + 16, hy + row2 + 1, 0xFF17130A);
                    }
                }
                g.disableScissor();
                String wt = "WORK IN PROGRESS"; int wtw = font.width(wt), pl = mid - wtw / 2 - 8;
                g.fill(pl, hy + 2, pl + wtw + 16, hy + bh2 - 2, 0xFF0D0F16);
                int gl = 170 + (int) (85 * Math.sin(sec * 4)); g.outline(pl, hy + 2, wtw + 16, bh2 - 4, Gfx.argb(gl, 0xFFC857));
                Gfx.textCentered(g, font, wt, mid, hy + 3, Gfx.argb(gl, 0xFFC857));
                gleam(g, hx, hy, bw2, bh2, nw);
                Icons.draw(g, "fd:wip", hx - 20, hy - 1, nw);
                Icons.draw(g, "fd:wip", hx + bw2 + 4, hy - 1, nw);
                continue;
            }
            if (r.kind == TEXT) {                                                   // plain text: a heading and its paragraph, no card
                int tx = r.x + 8;
                if (Icons.has(r.icon)) { Icons.draw(g, r.icon, r.x + 8, r.y + 4, System.currentTimeMillis()); tx = r.x + 30; }
                Gfx.text(g, font, r.title, tx, r.y + 3, Ui.TEXT);
                int wy = r.y + 16; for (String ln : wrap(r.sub, cw - (tx - r.x) - 20)) { Gfx.text(g, font, ln, tx, wy, Ui.SUB); wy += 11; }
                g.fill(r.x + 8, r.y + r.h - 3, r.x + r.w - 8, r.y + r.h - 2, 0x22FFFFFF);
                continue;
            }
            if (r.pickLabels != null) { popup = r; continue; }
            if (r.kind == NOTE) { int ny = r.y + 4; for (String ln : wrap(r.title, cw - 20)) { Gfx.text(g, font, ln, r.x + 10, ny, r.subColor); ny += 11; } continue; }
            boolean hov = !r.locked && mx >= r.x && mx < r.x + r.w && my >= r.y && my < r.y + r.h && my >= lt && my < lb;
            float hv = anim("row|" + tab + r.indent + r.title, hov && r.field == null && (r.click != null || !r.ctls.isEmpty()) ? 1f : 0f, 48f);
            pill(g, r.x, r.y, r.w, r.h, hv > 0.02f ? Canvas.mix(Ui.ROW, Ui.ROW_HOVER, hv) : Ui.ROW);
            g.fill(r.x + 1, r.y + 3, r.x + 3 + Math.round(hv * 2), r.y + r.h - 3, r.accent);

            int ix = r.x + 17, iy = r.y + (r.field != null ? 14 : r.h / 2);
            if (r.spriteId != null && ItemIcons.hasPixels(r.spriteId)) ItemIcons.draw(g, r.spriteId, null, ix, iy, 1f);
            else if (r.iconId != null) ItemIcons.draw(g, r.iconId, r.icon, ix, iy, 1f);
            else if (Icons.has(r.icon)) Icons.draw(g, r.icon, ix - 8, iy - 8, System.currentTimeMillis());
            else if (r.icon != null) Gfx.item(g, r.icon, ix - 8, iy - 8);
            int tx2 = r.x + 32;
            int limit = (r.ctls.isEmpty() ? r.x + r.w - 24 : r.ctls.get(0).x0 - 8) - tx2;
            boolean two = !r.sub.isEmpty();
            marquee(g, r.title, tx2, r.y + (two ? 5 : (r.h - 8) / 2), Math.max(40, limit), r.locked ? 0xFF8A8A96 : Ui.TEXT);
            if (two && r.wrapSub) { int wy = r.y + 17; for (String ln : wrap(r.sub, r.subW > 0 ? r.subW : r.w - 52)) { Gfx.text(g, font, ln, tx2, wy, r.subColor); wy += 11; } }
            else if (two) marquee(g, r.sub, tx2, r.y + 16, Math.max(40, limit), r.subColor);

            int ci = 0;
            for (Ctl c : r.ctls) { c.key = tab + r.title + (ci++); c.draw(g, r.y, r.wrapSub && r.subW > 0 ? r.h : Math.min(r.h, 28), mx >= c.x0 && mx < c.x1 && my >= r.y && my < r.y + r.h); }
            if (r.wip) drawWip(g, r);
            if (r.arrow) chevron(g, r.x + r.w - 12 + Math.round(hv * (r.open ? 0 : 1.5f)), r.y + r.h / 2, r.open ? 1 : 0, Canvas.mix(0xFF7C8394, Ui.ACCENT, Math.max(hv, r.open ? 0.7f : 0f)));

            if (r.field != null) {
                r.field.setVisible(r.y >= lt && r.y + r.h <= lb);
                r.field.setX(r.x + 32); r.field.setY(r.y + 24);
                int fw = r.w - 44 - (r.fieldBtn != null ? 46 : 0);
                if (r.field.getWidth() != fw) { r.field.setWidth(fw); r.field.moveCursorToStart(false); r.field.moveCursorToEnd(false); }   // a widened box shows its text from the start again
                if (r.fieldBtn != null) {
                    pill(g, r.btnX0, r.y + 24, r.btnX1 - r.btnX0, 16, GREEN);
                    Gfx.textCentered(g, font, r.fieldBtn, (r.btnX0 + r.btnX1) / 2, r.y + 28, Ui.TEXT);
                }
            }
        }
        for (Row r : rows) if (r.field != null && r.field.visible) r.field.extractRenderState(g, mx, my, delta);
        if (editor == 0) {
            if (typing != null && !numShow) endTyping(false);      // its row scrolled away or the page changed
            numField.setVisible(typing != null && numShow);
            if (numField.visible) numField.extractRenderState(g, mx, my, delta);
        }
        Gfx.pop(g);
        if (pageIn < 1f) g.fill(cx - 2, lt, cx + cw + 2, lb, Gfx.argb(Math.round((1f - pageIn) * 90), 0x121218));
        g.disableScissor();
        placePopup();
        drawPopup(g, mx, my);

        searchField.setVisible(true); searchField.setX(cx + cw - 124); searchField.setY(py + 12); searchField.setWidth(124);
        searchField.extractRenderState(g, mx, my, delta);

        if (totalH > viewH) {
            int bh = Math.max(16, viewH * viewH / totalH);
            int by = lt + (int) ((viewH - bh) * (scroll / (float) (totalH - viewH)));
            g.fill(cx + cw + 3, by, cx + cw + 5, by + bh, 0xFF4A4B58);
        }
        if (FancyConfig.get().showCommunity) drawCommunity(g, mx, my);
        if (intro < 1f) pill(g, px, py, pw, ph, Gfx.argb(Math.round((1f - intro) * 150), 0x0C0E14));
        Gfx.pop(g);
        drawDock(g, mx, my, intro, now);
        drawShowBar(g, mx, my);
        drawCosPanels(g, mx, my, intro);
        drawStrip(g, mx, my);
        drawToast(g, now);
        drawTileTip(g, mx, my);
        if (editor == 0) drawX(g, mainClose(), in(mx0, my0, mainClose()), "mx");
        drawEditor(g, mx0, my0);
    }

    // the open drop-down list, drawn over everything else
    private Row popup;
    private int popX0, popX1, popY, popVis, popScroll;
    private String popKey = "";
    private static final int POP_ROWS = 8, POP_H = 12;

    /** Works out where the open list goes: under its button, or above it when there is no room; shows 8 entries and scrolls. */
    private void placePopup() {
        if (popup == null) return;
        Row r = popup;
        popVis = Math.min(POP_ROWS, r.pickLabels.size());
        int hgt = popVis * POP_H + 4, below = r.owner.y + Math.min(r.owner.h, 28) - 6;
        popY = below + hgt <= py + ph - 4 ? below : Math.max(py + 44, r.owner.y + 6 - hgt);
        popX0 = r.anchor.x0; popX1 = r.anchor.x1;
        String key = open.stream().filter(k -> k.startsWith("dd_")).findFirst().orElse("");
        if (!key.equals(popKey)) { popKey = key; popScroll = Math.max(0, Math.min(r.pickLabels.size() - popVis, r.pickSel - popVis / 2)); }
        popScroll = Math.max(0, Math.min(r.pickLabels.size() - popVis, popScroll));
    }

    private void drawPopup(GuiGraphicsExtractor g, int mx, int my) {
        if (popup == null) return;
        Row r = popup; int hgt = popVis * POP_H + 4;
        g.fill(popX0, popY, popX1, popY + hgt, 0xFF0C0C11);
        g.outline(popX0, popY, popX1 - popX0, hgt, Ui.ACCENT);
        for (int i = 0; i < popVis; i++) {
            int idx = i + popScroll, ey = popY + 2 + i * POP_H;
            boolean hov = mx >= popX0 && mx < popX1 && my >= ey && my < ey + POP_H;
            boolean sel = idx == r.pickSel;
            if (hov) g.fill(popX0 + 1, ey, popX1 - 1, ey + POP_H, Ui.ROW_HOVER);
            if (sel) g.fill(popX0 + 1, ey + 2, popX0 + 3, ey + POP_H - 2, Ui.ACCENT);
            Gfx.text(g, font, font.plainSubstrByWidth(r.pickLabels.get(idx), popX1 - popX0 - 16), popX0 + 7, ey + 2, sel ? Ui.ACCENT : hov ? Ui.TEXT : Ui.SUB);
        }
        int n = r.pickLabels.size();
        if (n > popVis) {                                       // little scroll bar
            int bh = Math.max(8, (hgt - 4) * popVis / n), by = popY + 2 + (int) ((hgt - 4 - bh) * (popScroll / (float) (n - popVis)));
            g.fill(popX1 - 3, by, popX1 - 1, by + bh, 0xFF4A4B58);
        }
    }

    private void drawSection(GuiGraphicsExtractor g, Row r) {
        if (r.dropCols) {
            // labels over the Preview / Animation / Chat Alert columns of the drop rows below: same right-to-left columns, each CELL wide
            int edge = cx + cw - 8;
            String[] labels = {"Chat Alert", "Animation", "Preview"};
            for (String label : labels) {
                Gfx.textCentered(g, font, label, edge - CELL / 2, r.y + 3, Ui.DIM);
                edge -= CELL;
            }
            return;
        }
        Gfx.text(g, font, font.plainSubstrByWidth(r.title, cw), cx, r.y + 8, Ui.DIM);
    }

    /** One strip of hazard tape on the right of the row: stripes march along it, edges pulse. */
    private void drawWip(GuiGraphicsExtractor g, Row r) {
        long now = System.currentTimeMillis();
        int w = Math.min(r.w / 2, 170), h = 16;
        int x0 = r.x + r.w - w - 10, y0 = r.y + (r.h - h) / 2;
        int shift = (int) ((now / 28L) % 16);
        g.fill(x0, y0, x0 + w, y0 + h, 0xFF15130C);
        for (int by = 0; by < h; by += 2) {
            int off = Math.floorMod(by + shift, 16);
            for (int sx = x0 - 16 + off; sx < x0 + w; sx += 16) {
                int a = Math.max(x0, sx), b = Math.min(x0 + w, sx + 8);
                if (b > a) g.fill(a, y0 + by, b, y0 + by + 2, 0xFFC98C00);
            }
        }
        int glow = Math.round(150 + 100 * (float) Math.sin(now / 300.0));
        int edge = Gfx.argb(Math.min(255, glow), 0xFFD36B);
        g.outline(x0, y0, w, h, edge);
        String t = "WORK IN PROGRESS"; int tw = font.width(t), cxm = x0 + w / 2;
        g.fill(cxm - tw / 2 - 6, y0 + 2, cxm + tw / 2 + 6, y0 + h - 2, 0xFF15130C);
        Gfx.textCentered(g, font, t, cxm, y0 + 4, Ui.ACCENT);
        gleam(g, x0, y0, w, h, now);
    }

    /** A slanted white gleam that sweeps across a strip now and then. */
    private static void gleam(GuiGraphicsExtractor g, int x0, int y0, int w, int h, long now) {
        if (STILL) return;
        float u = (now % 2400L) / 2400f; int gx = x0 - 14 + Math.round(u * (w + 40));
        g.enableScissor(x0, y0, x0 + w, y0 + h);
        for (int j = 0; j < h; j++) g.fill(gx - j / 2, y0 + j, gx - j / 2 + 7, y0 + j + 1, 0x40FFFFFF);
        g.disableScissor();
    }

    // ------------------------------------------------------------------ input

    private void addCustom() {
        String n = addField.getValue().trim();
        if (n.isEmpty()) return;
        FancyConfig c = FancyConfig.get();
        if (c.custom.stream().noneMatch(d -> d.name.equalsIgnoreCase(n))) {
            FancyConfig.CustomDrop d = new FancyConfig.CustomDrop(); d.name = n; c.custom.add(d);
        }
        addField.setValue("");
    }

    /** Click or drag inside a Farming preview: places the scene there, but only when Farming's position is Custom. */
    private boolean farmDrag(double mx, double my, boolean start) {
        for (int[] b : prevBoxes) {
            boolean inside = mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3];
            if (start && !inside) continue;
            if (!start && !dragFarm) return false;
            FancyConfig c = FancyConfig.get();
            if (c.farmPos != 9) return false;                         // the preview only moves the scene while Farming is set to Custom
            c.farmX = (float) Math.max(0, Math.min(1, (mx - b[0]) / b[2]));
            c.farmY = (float) Math.max(0, Math.min(1, (my - b[1]) / b[3]));
            return true;
        }
        return false;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (typing != null) {
            boolean inBox = event.x() >= numField.getX() && event.x() < numField.getX() + numField.getWidth() && event.y() >= numField.getY() && event.y() < numField.getY() + 14;
            if (!inBox) endTyping(true);
        }
        if (editor != 0) { editorClick(event.x(), event.y(), event.button()); return true; }
        {   // an open drop-down list sits on top of everything, so it gets the click before the boxes underneath it
            double px0 = event.x(), py0 = event.y();
            if (popup != null && px0 >= popX0 && px0 < popX1 && py0 >= popY && py0 < popY + popVis * POP_H + 4) {
                int i = (int) ((py0 - popY - 2) / POP_H) + popScroll;
                if (event.button() == 0 && i >= popScroll && i < Math.min(popup.pickLabels.size(), popScroll + popVis)) popup.onPick.accept(i);
                return true;
            }
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        double mx = event.x(), my = event.y(); int button = event.button();
        if (button == 0 && stripClick(mx, my)) return true;
        if (button == 0 && inAuraPanel(mx, my)) { dragFig = true; return true; }
        if (button == 0 && mx >= px && mx < px + 14 && my >= py && my < py + 12) {                  // the hidden button, in the very corner of the menu
            FancyConfig sc = FancyConfig.get();
            if (!sc.secretFound) { sc.secretFound = true; FancyConfig.save(); toast("You found it!\nThe Secret cosmetic is yours.", 0xFFC08BFF); }
            return true;
        }
        if (button == 0 && farmDrag(mx, my, true)) { dragFarm = true; return true; }
        if (button == 0 && in(mx, my, mainClose())) { if (FancyConfig.get().menuAnimated) { if (closeAt == 0L) closeAt = System.currentTimeMillis(); } else onClose(); return true; }
        if (button == 0 && showBarClick(mx, my)) return true;
        if (button == 0 && dockBtn[0] >= 0 && mx >= dockBtn[0] && mx < dockBtn[2] && my >= dockBtn[1] && my < dockBtn[3]) {        // the dock's own "move" button
            FancyConfig dc = FancyConfig.get(); dc.previewAt = dc.previewAt == 0 ? 1 : 0; FancyConfig.save();
            return true;
        }

        if (button == 0 && mx >= masterBox()[0] && mx < masterBox()[2] && my >= masterBox()[1] && my < masterBox()[3]) {
            FancyConfig mc = FancyConfig.get(); boolean on = !(mc.masterAnimate || mc.masterAnnounce);
            mc.setMaster(on, on); FancyConfig.save();
            return true;
        }
        if (!query.isBlank()) {
            if (button == 0 && inSide(mx, my)) for (int[] e : searchSide) if (mx >= e[2] && mx < e[4] && my >= e[3] && my < e[5]) {
                dropSrc = null; waveKey = null; SoundFx.stopPreviews();
                tab = Tab.values()[e[0]]; subSel[e[0]] = e[1]; query = ""; searchField.setValue(""); scroll = 0; sideScroll = 0; tabChanged = System.currentTimeMillis();
                open.removeIf(o -> o.startsWith("dd_"));
                return true;
            }
        } else
        for (int i = 0; i < Tab.values().length; i++) {
            int[] b = tabBox(i);
            if (inSide(mx, my) && mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) { dropSrc = null; if (tab != Tab.values()[i]) { tabChanged = System.currentTimeMillis(); } tab = Tab.values()[i]; subSel[tab.ordinal()] = -1; scroll = 0; open.removeIf(o -> o.startsWith("dd_")); return true; }
        }
        if (tab == Tab.DROPS && srcExp > 0.9f && query.isBlank()) {
            for (int j = 0; j < srcList.size(); j++) {
                int[] b = srcBox(j);
                if (inSide(mx, my) && mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) { dropSrc = srcList.get(j)[0]; scroll = 0; tabChanged = System.currentTimeMillis(); return true; }
            }
        }
        if (subExp > 0.9f && query.isBlank()) {
            for (int k = 0; k < subs().length; k++) {
                int[] b = subBox(k);
                if (inSide(mx, my) && mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) { dropSrc = null; waveKey = null; SoundFx.stopPreviews(); if (curSub() != k) tabChanged = System.currentTimeMillis(); subSel[tab.ordinal()] = k; scroll = 0; open.removeIf(o -> o.startsWith("dd_")); return true; }
            }
        }
        if (popup != null) {
            if (mx >= popX0 && mx < popX1 && my >= popY && my < popY + popVis * POP_H + 4) {
                int i = (int) ((my - popY - 2) / POP_H) + popScroll;
                if (button == 0 && i >= popScroll && i < Math.min(popup.pickLabels.size(), popScroll + popVis)) popup.onPick.accept(i);
                return true;
            }
            Row ow = popup.owner;       // clicking anywhere else closes it (its own button toggles it by itself)
            boolean onBtn = mx >= popup.anchor.x0 && mx < popup.anchor.x1 && my >= ow.y && my < ow.y + 28;
            if (!onBtn) open.removeIf(k -> k.startsWith("dd_"));
        }
        if (button == 0 && FancyConfig.get().showCommunity) for (int i = 0; i < 3; i++) {
            int[] b = commBox(i);
            if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {             // no links yet: the chat says so
                if (i == 0) { FancyConfig dc0 = FancyConfig.get(); if (!dc0.discordJoined) { dc0.discordJoined = true; FancyConfig.save(); }      // pressing it unlocks the Discord cosmetic
                    try { net.minecraft.client.gui.screens.ConfirmLinkScreen.confirmLinkNow(this, java.net.URI.create(FancyDropsMod.DISCORD)); } catch (Throwable ignored) {} return true; }      // Discord has a link; the others say so in chat
                if (FancyConfig.get().popups) { toast(COMM_NAME[i] + " link coming soon", i == 1 ? 0xFFFF7A59 : 0xFF8FA8FF); return true; }
                var mcl = net.minecraft.client.Minecraft.getInstance(); if (mcl.player != null) mcl.player.connection.sendCommand("fd " + COMM[i]);
                return true;
            }
        }
        if (mx < cx || mx > cx + cw || my < lt || my > lb) return false;

        build();
        for (Row r : rows) {
            if (r.pickLabels != null) continue;
            if (r.kind == PICKER && button == 0 && mx >= r.x && mx < r.x + r.w && my >= r.y && my < r.y + r.h) {
                for (int i = 0; i < r.pLabels.length; i++) {
                    int[] pb = pickBox(r, i);
                    if (mx >= pb[0] && mx < pb[0] + pb[2] && my >= pb[1] && my < pb[1] + pb[3]) { r.pSet.accept(i); return true; }
                }
                return true;
            }
            if ((r.kind != NORMAL && r.kind != TIP) || r.locked || my < r.y || my >= r.y + r.h || mx < r.x || mx >= r.x + r.w) continue;
            for (Ctl c : r.ctls) if (mx >= c.x0 && mx <= c.x1) {
                if (c.click(mx, button)) { if (c instanceof Slider) dragCtl = c; return true; }
                if (c instanceof Slider ts && typingId == ts.id) return true;
            }
            if (r.fieldBtn != null && button == 0 && mx >= r.btnX0 && mx <= r.btnX1 && my >= r.y + 24) { addCustom(); return true; }
            if (button == 0 && r.click != null) { r.click.run(); return true; }
            return true;
        }
        return false;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (editor != 0) { if (eDrag != 0) eSet(event.x(), event.y()); return true; }
        if (dragFig) { figYaw += (float) dx * 0.03f; return true; }
        if (dragFarm) { farmDrag(event.x(), event.y(), false); return true; }
        if (dragCtl != null) return dragCtl.drag(event.x());
        return super.mouseDragged(event, dx, dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        eDrag = 0;
        dragCtl = null; dragFarm = false; dragFig = false;
        return super.mouseReleased(event);
    }

    @Override public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (editor != 0) return true;
        if (popup != null && mx >= popX0 && mx < popX1 && my >= popY && my < popY + popVis * POP_H + 4 && popup.pickLabels.size() > popVis) {
            popScroll -= (int) Math.signum(scrollY) * 2;
            return true;
        }
        if (inSide(mx, my) && sideMax() > 0) { sideScroll = Math.max(0, Math.min(sideMax(), sideScroll - (int) (scrollY * 22))); return true; }
        scroll = Math.max(0, scroll - (int) (scrollY * 22));
        return true;
    }

    @Override public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (typing != null && numField.isFocused()) {
            if (key == 257 || key == 335) { endTyping(true); return true; }
            if (key == 256) { endTyping(false); return true; }
        }
        if (editor != 0) {                                                                  // Esc warns first, a second Esc throws the edits away
            if (hexField.isFocused()) { if (key == 256 || key == 257 || key == 335) { hexField.setFocused(false); return true; } return super.keyPressed(event); }
            if (key == 256) {
                if (!editWarn) { editWarn = true; toast("Changes won't be saved.\nEsc again to discard", 0xFFFF8A5C); }
                else { closeEditor(false); toast("Changes discarded.", 0xFFFFB86B); }
            }
            return true;
        }
        if (addField.isFocused() && addField.visible && (key == 257 || key == 335)) { addCustom(); return true; }
        FancyConfig fc = FancyConfig.get();
        if (key == 256 && fc.menuAnimated) { if (closeAt == 0L) closeAt = System.currentTimeMillis(); return true; }      // Esc fades the menu out first
        if (key >= 262 && key <= 265 && fc.farmPos == 9 && !prevBoxes.isEmpty() && typing == null && !searchField.isFocused() && !formatField.isFocused() && !addField.isFocused()) {
            float dx = key == 262 ? 1f : key == 263 ? -1f : 0f, dy = key == 264 ? 1f : key == 265 ? -1f : 0f;     // one GUI pixel per press
            fc.farmX = Math.max(0f, Math.min(1f, fc.farmX + dx / Math.max(1, width)));
            fc.farmY = Math.max(0f, Math.min(1f, fc.farmY + dy / Math.max(1, height)));
            return true;
        }
        return super.keyPressed(event);
    }
}
