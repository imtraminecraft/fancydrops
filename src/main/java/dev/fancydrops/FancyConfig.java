package dev.fancydrops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Player settings (toggled in /fancydrops). Saved to config/fancydrops.json. */
public class FancyConfig {
    // ------------------------------------------------------------------ the shapes

    /** One drop in the Drop Library. anim: 0 = by rarity (rare drops play RNG, the rest Normal), 1 = Normal, 2 = RNG, 3 = no animation. */
    public static class DropCfg { public int anim = 0; public boolean announce = false; public boolean init; }
    /** One source (a creature, the treasure): its master switch and its drops. */
    public static class SourceCfg { public boolean enabled = true; public Map<String, DropCfg> drops = new LinkedHashMap<>(); }
    /** The settings of one animation (key like "myth.rng"): how long, where, how big, and for RNG the bars and the blur. */
    public static class AnimCfg {
        public float secs = 6f;          // 5..7
        public int pos = 1;              // 0 Top Center, 1 Center, 2 Bottom Center
        public float size = 1f;          // 0.5 .. 2
        public boolean bars = true, blur = true;   // RNG only
    }

    public static final String DEFAULT_FORMAT = "RARE DROP! {item} ({price}) from {source}";

    // ------------------------------------------------------------------ general
    public boolean masterAnimate = true;     // master: play any animation at all (the Active button)
    public boolean masterAnnounce = true;    // master: send any chat alert at all
    public String channel = "PARTY";         // PARTY | COOP | ALL | OFF
    public boolean alsoAll = false;          // RNG drops are also announced in public chat, with "[FancyDrops] | " in front
    public boolean sound = true;
    public int volume = 100;                 // 0..100 %, master volume of every FancyDrops sound
    public boolean customSounds = true;      // the mod's own sound effects (off = Minecraft's sounds instead)
    public Map<String, String> sounds = new LinkedHashMap<>();      // "<scene>.<slot>" -> sound name, see SoundFx
    public Map<String, String> mcSounds = new LinkedHashMap<>();    // the same slots for the Minecraft sound set
    public boolean show3d = true;            // the dropped item is a real spinning 3D model instead of a flat icon
    public boolean epicEffects = true;       // a drop worth epicThreshold or more plays RNG even when its rarity says Normal
    public long epicThreshold = 20_000_000L; // coins
    public boolean popups = true;            // notices pop up under the menu instead of going to chat
    public boolean showTips = true;          // the rotating tip card on the Home page

    // ------------------------------------------------------------------ prices
    public boolean useBazaar = true, useAuctions = true;
    public String bazaarPrice = "BUY";                  // BUY = instant buy, SELL = instant sell
    public String priceSource = "HYPIXEL_AUCTIONS";     // HYPIXEL_AUCTIONS or LOWEST_BIN_FEED

    // ------------------------------------------------------------------ animations (the Animations page)
    public Map<String, AnimCfg> anims = new LinkedHashMap<>();
    public AnimCfg anim(String key) {
        AnimCfg a = anims.get(key);
        if (a == null) { a = new AnimCfg(); anims.put(key, a); }
        return a;
    }
    // Bar changes
    public int barPx = 15;               // bar height in pixels of the 96-pixel-tall scene (5..15)
    public int barOpacity = 90;          // how solid the bars are (%)
    public boolean barPillars = true;    // the side pillars
    public int barCurve = 0;             // how round the window's corners are (0..100)
    public boolean barLine = true;       // a thin bright line along the inner edge of the bars
    public void setBarPx(int v) { barPx = Math.max(5, Math.min(15, v)); }
    public boolean barSoft = false;      // the bars fade out softly along their inner edge
    public boolean barCustom = false;    // true: the bars use barColor instead of the drop's colour
    public int barColor = 0xFF0B0E2A;
    public int barStyle = 0;             // bar colour preset
    // Text changes
    public float textScale = 1f;         // size of the text (name, price, odds)
    public float titleScale = 1f, subScale = 1f;
    public float subDelay = 0.35f;       // seconds between the title and the sub title coming in
    public boolean textBold = false, textOutline = true;
    public boolean showPrice = true, showPriceSource = true, showOdds = true, showMagicFind = true;
    public boolean fxPop = true, fxShine = true, fxWave = false, fxRainbow = false, fxDrop = false, fxPulse = false, fxGlitch = false, fxGlow = false, fxBounce = false, fxType = false, fxFloat = false, fxShake = false, fxFire = false, fxFrost = false;
    public boolean fx(int i) { return switch (i) { case 0 -> fxPop; case 1 -> fxShine; case 2 -> fxWave; case 3 -> fxRainbow; case 4 -> fxDrop; case 5 -> fxPulse; case 6 -> fxGlitch; case 7 -> fxGlow; case 8 -> fxBounce; case 9 -> fxType; case 10 -> fxFloat; case 11 -> fxShake; case 12 -> fxFire; case 13 -> fxFrost; default -> false; }; }
    public void setFx(int i, boolean v) { switch (i) { case 0 -> fxPop = v; case 1 -> fxShine = v; case 2 -> fxWave = v; case 3 -> fxRainbow = v; case 4 -> fxDrop = v; case 5 -> fxPulse = v; case 6 -> fxGlitch = v; case 7 -> fxGlow = v; case 8 -> fxBounce = v; case 9 -> fxType = v; case 10 -> fxFloat = v; case 11 -> fxShake = v; case 12 -> fxFire = v; case 13 -> fxFrost = v; default -> { } } }
    // Item changes
    public float itemScale = 1f, itemSpin = 1f, itemGlow = 1f, itemBobAmp = 1f;
    public boolean itemBob = true, itemGlints = true, itemOutline = true, itemSpinRev = false;

    // ------------------------------------------------------------------ chat
    public Map<String, String> formats = new LinkedHashMap<>();     // category id -> the message
    public Map<String, Boolean> chatOn = new LinkedHashMap<>();     // category id -> chat alerts on (missing = on)
    public String formatFor(String cat) { String f = formats.get(cat); return f == null || !ChatFilter.ok(f) ? DEFAULT_FORMAT : f; }
    public void setFormat(String cat, String v) { formats.put(cat, v); }
    public boolean chatEnabled(String cat) { Boolean b = chatOn.get(cat); return b == null || b; }

    // ------------------------------------------------------------------ the Drop Library
    public Map<String, SourceCfg> sources = new LinkedHashMap<>();
    public SourceCfg source(String id) { return sources.computeIfAbsent(id, k -> new SourceCfg()); }
    public DropCfg drop(String sourceId, String name) { return source(sourceId).drops.computeIfAbsent(name, k -> new DropCfg()); }
    /** A drop that was never touched starts from its rarity: rare drops (rank 2 and up) get a chat alert, the rest stay quiet. */
    public static void initDrop(DropCfg dc, int rank) { if (dc.init) return; dc.init = true; dc.announce = rank >= 2; }
    public int mythPet = 5;                  // Mythological: the Griffin pet rarity drop chances are shown for (0 Common .. 5 Mythic)
    public int mythSpade = 2;                // Mythological treasure: the spade the chances are shown for (0 Ancestral, 1 Archaic, 2 Deific)
    public boolean mythErudite = false;      // ... with the Erudite reforge (2 more burrows in a chain)

    // ------------------------------------------------------------------ the menu
    public int previewAt = 1;                // live preview dock: 0 top, 1 bottom, 2 off
    public int menuTranslucency = 25;        // 0 = solid menu .. 70 = see-through
    public boolean menuIcons = true;         // sidebar and row icons move
    public boolean menuAnimated = true;      // menu slides in, pages fade, accent bar flows
    public int uiFont = 0, textFont = 0;

    // ------------------------------------------------------------------ cosmetics
    public String suffix = "", nameFx = "", aura = "";        // what you picked ("" = none)
    public String firstVersion = "";                           // the first version this config saw: decides who gets the Beta cosmetics
    public boolean discordJoined, secretFound;                 // unlocked by pressing the Discord button / the hidden button
    public int dropsSeen;                                      // rare drops the mod has heard
    public boolean othersSuffix = true, othersName = true, othersAura = true;      // see other players' cosmetics
    public boolean selfSuffix = true, selfName = true, selfAura = true;            // show your own cosmetics
    public int cosRange = 32, cosMax = 8, cosSpeed = 100, cosAmount = 100, cosSize = 100;
    public boolean cosHideFirst = false, cosCalm = false;
    public String adminToken = "";                             // Admin Panel: a GitHub token that lets Save write cosmetics.json

    public int ver;                                            // 3 = the rewrite: animation and drop settings start fresh

    // ------------------------------------------------------------------ load / save
    private static FancyConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("fancydrops.json"); }

    public static FancyConfig get() { if (instance == null) load(); return instance; }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) instance = GSON.fromJson(Files.readString(p), FancyConfig.class);
        } catch (Exception e) { e.printStackTrace(); }
        if (instance == null) instance = new FancyConfig();
        FancyConfig c = instance;
        if (c.firstVersion == null || c.firstVersion.isEmpty()) { c.firstVersion = FancyDropsMod.VERSION; save(); }
        if (c.nameFx == null) c.nameFx = "";
        if (c.aura == null) c.aura = "";
        if (c.suffix == null) c.suffix = "";
        if (c.anims == null) c.anims = new LinkedHashMap<>();
        if (c.sources == null) c.sources = new LinkedHashMap<>();
        if (c.formats == null) c.formats = new LinkedHashMap<>();
        if (c.chatOn == null) c.chatOn = new LinkedHashMap<>();
        if (c.sounds == null) c.sounds = new LinkedHashMap<>();
        if (c.mcSounds == null) c.mcSounds = new LinkedHashMap<>();
        if (c.ver < 3) { c.ver = 3; c.sounds.clear(); c.mcSounds.clear(); save(); }          // the rewrite: old sound picks refer to sounds that no longer exist
        c.textScale = clamp(c.textScale, 0f, 1.1f); c.itemScale = clamp(c.itemScale, 0.5f, 1.1f);
        c.titleScale = clamp(c.titleScale, 0.5f, 1.5f); c.subScale = clamp(c.subScale, 0.5f, 1.5f); c.subDelay = clamp(c.subDelay, 0f, 3f);
        c.itemSpin = clamp(c.itemSpin, 0f, 2f); c.itemGlow = clamp(c.itemGlow, 0f, 1.5f);
        c.barPx = Math.max(5, Math.min(15, c.barPx));
        c.mythPet = Math.max(0, Math.min(5, c.mythPet)); c.mythSpade = Math.max(0, Math.min(2, c.mythSpade));
        c.menuTranslucency = Math.max(0, Math.min(70, c.menuTranslucency)); c.previewAt = Math.max(0, Math.min(2, c.previewAt));
        for (AnimCfg a : c.anims.values()) { a.secs = clamp(a.secs, 5f, 7f); a.size = clamp(a.size, 0.5f, 2f); a.pos = Math.max(0, Math.min(2, a.pos)); }
    }
    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    /** Sets the two master switches (null = leave as is). */
    public void setMaster(Boolean animate, Boolean announce) {
        if (animate != null) masterAnimate = animate;
        if (announce != null) masterAnnounce = announce;
    }

    public static void save() {
        try { Files.writeString(path(), GSON.toJson(get())); } catch (Exception e) { e.printStackTrace(); }
    }

    public float textK() { return 0.6f + 0.4f * textScale; }
    public float itemK() { return itemScale; }
}
