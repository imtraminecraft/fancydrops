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
        long start; float lastT = -1f;
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
        float t = (now - cur.start) / 1000f;
        cues(cur, cur.lastT, t); cur.lastT = t;
        draw(g, mc.font, cur, t, g.guiWidth(), g.guiHeight());
    }

    /** The moments (seconds) each sound slot (chop, split, reveal, linger, end) plays in a scene of this length. */
    static float[][] cueTimes(String scene, float secs) {
        float S = Math.max(3.6f, secs), O = S - Ritual.TAIL; boolean tre = scene.startsWith("treasure"), nod = scene.endsWith("nodrops");
        float[] beats = tre ? new float[]{0.215f * O, 0.505f * O, 0.795f * O} : new float[]{0.34f * O, 0.58f * O, 0.80f * O};
        return new float[][]{beats, {O}, nod ? new float[0] : new float[]{O + 0.25f}, {O + 1.0f}, {S - 1.4f}};
    }

    /** The sounds of the timeline: a beat for every strike or dig, the burst when the animation pivots, then the reveal, the linger and the end. */
    private static void cues(Anim a, float prev, float t) { cues(a.key(), a.secs(), prev, t); }
    private static void cues(String scene, float secs, float prev, float t) {
        boolean big = scene.endsWith("rng"), nod = scene.endsWith("nodrops");
        float[][] tm = cueTimes(scene, secs);
        for (int k = 0; k < tm[0].length; k++) if (prev < tm[0][k] && t >= tm[0][k]) SoundFx.playSlot(scene, "chop", 0.9f + 0.12f * k, big);
        if (prev < tm[1][0] && t >= tm[1][0]) SoundFx.playSlot(scene, "split", nod ? 0.8f : 1f, big);
        if (tm[2].length > 0 && prev < tm[2][0] && t >= tm[2][0]) SoundFx.playSlot(scene, "reveal", 1f, big);
        if (prev < tm[3][0] && t >= tm[3][0]) SoundFx.playSlot(scene, "linger", nod ? 0.85f : 1f, big);
        if (prev < tm[4][0] && t >= tm[4][0]) SoundFx.playSlot(scene, "end", 1f, big);
    }

    // ---- the Play button of the Sounds page: just the sounds, in time, no picture
    private static String sndKey; private static long sndT0; private static float sndLast; private static boolean sndOn;
    /** Plays the sounds of a scene ("myth.rng") on their real timeline. */
    public static void playSoundsOnly(String key) { SoundFx.stopPreviews(); sndKey = key; sndT0 = System.currentTimeMillis(); sndLast = -1f; sndOn = true; }
    /** Call every frame while the menu is open: fires the due sounds of a sounds-only playback. */
    public static void tickSounds() {
        if (!sndOn) return;
        float t = (System.currentTimeMillis() - sndT0) / 1000f, secs = FancyConfig.get().anim(sndKey).secs;
        cues(sndKey, secs, sndLast, t); sndLast = t;
        if (t > Math.max(3.6f, secs) + 0.4f) sndOn = false;
    }
    /** Seconds into the playback of this scene (a sounds-only playback or the real animation), or -1 when nothing of it is playing. */
    public static float playingT(String key) {
        long now = System.currentTimeMillis();
        if (sndOn && key.equals(sndKey)) return (now - sndT0) / 1000f;
        if (cur != null && key.equals(cur.key())) return (now - cur.start) / 1000f;
        return -1f;
    }

    /** Draws one animation at time t (seconds) inside a W x H screen. */
    static void draw(GuiGraphicsExtractor g, Font font, Anim a, float t, int W, int H) { Ritual.draw(g, font, a, t, W, H); }

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
        float pivot = Math.max(3.6f, secs) - Ritual.TAIL;
        if (TextFx.freezeT >= 0) t = pivot + TextFx.freezeT;                       // held that many seconds after the drop appears
        else if (TextFx.itemOnly) t = pivot + 1.6f;                                // the Item editor shows the scene held, with the item living on its own clock
        else if (TextFx.textOnly) t = pivot + 1.6f;                                // the Text editor draws the words alone (they loop on the text clock)
        if (t > secs + 0.01f) return;
        draw(g, Minecraft.getInstance().font, a, t, W, H);
    }

    private AnimationManager() {}
}
