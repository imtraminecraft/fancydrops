package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayDeque;

/**
 * Plays the drop animations, one at a time (the rest wait in a queue). An animation is a group ("myth", "treasure") and a type
 * (No Drops, Normal, RNG); its settings are FancyConfig.anim("myth.rng"). Categories.java lists every group.
 */
public final class AnimationManager {
    /** Everything the animation and its text need about one drop. */
    public static final class Item {
        public DropData.Source source;
        public String name = "", id = "", icon = "nether_star", odds = "", chance = "", mf, stateKey;     // stateKey: the library row this drop follows
        public int rank, amount = 1;
        public boolean untradeable, announce, sim;
        public volatile double price = -2;       // -2 = still loading, -1 = no price
        public volatile String priceSource = "";
        public String extra; public int extraCol = Ui.SUB;     // one more line under the price
        public int color() {
            return switch (rank) { case 0 -> Ui.R_RARE; case 1 -> Ui.R_VERY; case 2 -> Ui.R_CRAZY; default -> Ui.R_LEGEND; };
        }
        public String title() { return amount > 1 ? amount + " " + name : name; }
    }

    /** The three types every group has. */
    public static final int NO_DROPS = 0, NORMAL = 1, RNG = 2;
    static final String[] TYPE_IDS = {"nodrops", "normal", "rng"};

    public static final class Anim {
        final Item item;                         // null = nothing dropped
        final String group;                      // "myth" or "treasure"
        final int type;                          // NO_DROPS, NORMAL or RNG
        final long created = System.currentTimeMillis();
        long start;
        Anim(String group, int type, Item item) { this.group = group; this.type = type; this.item = type == NO_DROPS ? null : item; }
        String key() { return group + "." + TYPE_IDS[type]; }
        FancyConfig.AnimCfg cfg() { return FancyConfig.get().anim(key()); }
        float secs() { return cfg().secs; }
    }

    private static final ArrayDeque<Anim> QUEUE = new ArrayDeque<>();
    private static Anim cur;

    public static void play(Anim a) { if (a != null) QUEUE.add(a); }
    public static boolean isPlaying() { return cur != null || !QUEUE.isEmpty(); }
    /** True while the hotbar and hearts should slide away (a scene with cinematic bars). */
    public static boolean hidesHud() { return cur != null && cur.type == RNG && cur.cfg().bars; }

    /** Which type a drop plays: its own pick in the Drop Library, else RNG for rare drops (rank 2 up, or worth the RNG price), else Normal. -1 = it has no animation. */
    public static int typeOf(Item it, FancyConfig.DropCfg dc) {
        FancyConfig c = FancyConfig.get();
        int pick = dc == null ? 0 : dc.anim;
        if (pick == 3) return -1;
        if (pick == 1) return NORMAL;
        if (pick == 2) return RNG;
        return it.rank >= 2 ? RNG : NORMAL;
    }

    /** Called every frame from the HUD. */
    public static void render(GuiGraphicsExtractor g) {
        long now = System.currentTimeMillis();
        if (cur != null && (now - cur.start) / 1000f > cur.secs()) cur = null;
        if (cur == null && !QUEUE.isEmpty()) { cur = QUEUE.poll(); cur.start = now; DropListener.announceNow(cur.item); }          // the chat alert goes out together with the animation
        if (cur == null) return;
        Minecraft mc = Minecraft.getInstance();
        draw(g, mc.font, cur, (now - cur.start) / 1000f, g.guiWidth(), g.guiHeight());
    }

    /** Draws one animation at time t (seconds) inside a W x H screen. The new animations replace this holding page. */
    static void draw(GuiGraphicsExtractor g, Font font, Anim a, float t, int W, int H) {
        FancyConfig c = FancyConfig.get();
        float secs = a.secs();
        float in = Math.min(1f, t / 0.4f), out = Math.min(1f, Math.max(0f, (secs - t) / 0.5f));
        int alpha = Math.round(255 * Math.min(in, out));
        TextFx.revealT = t; TextFx.waveT = t; TextFx.waveOut = 0f; TextFx.lineDelay = 0f;
        float sc = c.textK() * Math.max(0.55f, Math.min(W, H) / 540f) * a.cfg().size;
        int y = a.cfg().pos == 0 ? Math.round(H * 0.28f) : a.cfg().pos == 1 ? Math.round(H * 0.45f) : Math.round(H * 0.62f);
        if (a.item == null) { Gfx.textCentered(g, font, "Nothing :(", W / 2, y, Gfx.argb(alpha, 0xB8C0D4)); Gfx.textCentered(g, font, "Better luck next time", W / 2, y + 12, Gfx.argb(alpha, 0x8A93B0)); return; }
        TextFx.block(g, font, W / 2, y, sc, a.item, c, alpha);
    }


    // ------------------------------------------------------------------ the menu preview

    private static Item SAMPLE;
    private static Item sample() {
        if (SAMPLE == null) {
            Item it = new Item(); it.name = "Daedalus Stick"; it.id = "DAEDALUS_STICK"; it.icon = "stick"; it.rank = 2; it.price = 21_000_000; it.priceSource = "Auction House"; it.mf = "+250%"; it.chance = "1 in 1,800"; it.odds = "1 in 1,800";
            SAMPLE = it;
        }
        return SAMPLE;
    }
    private static String previewKey; private static long previewT0;
    /** Seconds into the animation the menu preview shows, or -1 to loop on its own clock. */
    public static float syncSec = -1f;
    /** The menu's live preview: loops the animation named by key ("myth.rng") inside a W x H screen. */
    public static void drawPreview(GuiGraphicsExtractor g, int W, int H, String key) {
        if (key == null || !key.contains(".")) key = "myth.normal";
        String group = key.substring(0, key.indexOf('.')), tid = key.substring(key.indexOf('.') + 1);
        int type = NORMAL; for (int i = 0; i < TYPE_IDS.length; i++) if (TYPE_IDS[i].equals(tid)) type = i;
        Anim a = new Anim(group, type, sample());
        long now = System.currentTimeMillis();
        if (!key.equals(previewKey)) { previewKey = key; previewT0 = now; }
        float secs = a.secs();
        float t = syncSec >= 0 ? syncSec : ((now - previewT0) / 1000f) % (secs + 1f);
        if (TextFx.freezeT >= 0) t = TextFx.freezeT;
        if (t > secs) return;
        draw(g, Minecraft.getInstance().font, a, t, W, H);
    }

    private AnimationManager() {}
}
