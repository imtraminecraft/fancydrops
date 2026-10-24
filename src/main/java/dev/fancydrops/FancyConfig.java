package dev.fancydrops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Player settings (toggled in /fancydrops). Saved to config/fancydrops.json. */
public class FancyConfig {
    public static class DropCfg { public boolean animate = true, announce = true; }

    public static class SlayerCfg {
        public boolean enabled = true, animate = true, announce = true;
        public Map<String, DropCfg> drops = new LinkedHashMap<>();
    }

    public static class CustomDrop {
        public String name = "";
        public String id = "";          // optional SkyBlock id for price lookup (defaults to NAME_WITH_UNDERSCORES)
        public boolean animate = true, announce = true;
    }

    public static final String DEFAULT_FORMAT = "RARE DROP! {item} ({price}) from {source}";
    public static final String DEFAULT_DYE_FORMAT = "RARE DYE! {item} ({price})";
    public static final String DEFAULT_MYTH_FORMAT = "RARE DROP! {item} ({price}) from {source}";
    public static final String DEFAULT_EXP_FORMAT = "RARE DROP! {item} ({price}) from {source}";
    public static final String DEFAULT_FARM_FORMAT = "RARE CROP! {item} ({price})";

    public boolean enabled = true;       // legacy; kept equal to "animate or announce"
    public boolean masterAnimate = true;  // master: play any animation at all
    public boolean masterAnnounce = true; // master: send any chat announcement at all
    public String channel = "PARTY";    // PARTY | COOP | OFF
    public boolean alsoAll = false;       // RNG drops are also announced in public chat, with "[FancyDrops] | " in front
    public int formatVer;                 // 2 = the message formats were reset to the {source} ones
    public String formatDye, formatFarm, formatMyth, formatExp;      // the message for Dye and Farming drops (null = Dye follows the Slayers one; Farming has its own default)
    public String format = DEFAULT_FORMAT;   // placeholders: {item} {drop} {price} {mf} {slayer}
    public boolean announceUnknown = true, animateUnknown = true;
    public boolean showPrice = true, sound = true;
    public boolean showPriceSource = true;    // "at Bazaar" / "at Auction House" after the price, like the reference video's "at George"
    public int volume = 100;                  // 0..100 %, master volume of every FancyDrops sound (/fancydrops volume)
    public boolean customSounds = true;       // the mod's own .ogg sound effects (off = Minecraft's sounds instead)
    public boolean animateNothing = true;     // play the (boring) scene on slayer kills that drop nothing you ticked in /fancydrops
    public boolean showOdds = true;           // "1 in 735" under the drop name
    public boolean showMagicFind = true;      // "Magic Find: +X%" line, taken from the Hypixel drop message
    public String suffix = "";                   // Cosmetics: the chat / tab list suffix you picked ("" = none)
    public String nameFx = "", aura = "";        // Cosmetics: the name effect and the aura you picked
    public String firstVersion = "";             // the first version of the mod this config saw: decides who gets the Beta cosmetics
    public boolean discordJoined, secretFound;   // Cosmetics: unlocked by pressing the Discord button / the hidden button
    public int dropsSeen;                        // rare drops the mod has heard (First Drop, Collector)
    public boolean show3d = true;             // dropped item is a real spinning 3D model instead of a flat icon
    public Boolean cineDye, cineMytho, cineExp, blurDye, blurMytho, blurExp;   // each scene can have its own bars and blur (null = same as Slayers)
    public float size = -1f;                  // animation size, 100% = 1.0 (older configs stored it as 80% = 1.0; converted on load)
    public boolean animBlur = true;           // blur the game behind a scene
    public int barHeight = 16;                // each black cinematic bar, as a percentage of the screen height
    public int dyeStyle = 0, farmStyle = 0, mythStyle = 0, expStyle = 0;      // 0 = the first look, 1 = the second look of that page
    /** The look a page of animations is using: 0 or 1 (cat: 0 slayers, 1 dye, 2 farming, 3 mythological, 4 experiments). */
    public int styleOf(int cat) { return switch (cat) { case 1 -> dyeStyle; case 2 -> farmStyle; case 3 -> mythStyle; case 4 -> expStyle; default -> slayerStyle; }; }
    public int slayerStyle = 0;               // 0 Sword (sword slices), 1 Spirit (soul slashes)
    public int barPx = 15;                    // bar height in pixels of the 96-pixel-tall scene (5..15); barHeight (a percentage) follows it
    public int barOpacity = 90;                 // Bar Changes: how solid the bars are (%)
    public boolean barPillars = true, barLine = false;       // Bar Changes: the side pillars (barLine is retired and unused)
    public int barCurve = 0;                   // Bar Changes: how round the window's corners are (0..100, a share of the bar height)
    public boolean barSoft = false;            // Bar Changes: the bars fade out softly along their inner edge
    public float itemBobAmp = 1f;               // Item Changes: how far the item floats (a multiple)
    public boolean itemSpinRev = false, textBold = false, textOutline = true;
    public boolean barCustom = false;         // true: the bars use barColor instead of the drop's colour
    public int barColor = 0xFF0B0E2A;
    public float textDelayAll = 4f;           // seconds after an animation starts until its words appear (3..10)
    public boolean fxPop = true, fxShine = true, fxWave = false, fxRainbow = false, fxDrop = false, fxPulse = false, fxGlitch = false, fxGlow = false, fxBounce = false, fxType = false, fxFloat = false, fxShake = false, fxFire = false, fxFrost = false, fxTwinkle = false, fxFade = true, fxRise = false, fxShrink = false, fxScatter = false, fxSlide = false, fxFlip = false, fxSwing = false, fxGold = false, fxSlideOut = false, fxMelt = false;
    public float itemSpin = 1f, itemGlow = 1f;                   // Item Changes: how fast the item spins and how strong its glow is (a multiple)
    public boolean itemBob = true, itemGlints = true, itemOutline = true;   // Item Changes: floats up and down, little glints round flat items, soft outline   // text effects (Animations > Text Changes)
    public float titleScale = 1f, subScale = 1f;   // title and sub title size, as a multiple
    public float subDelay = 0.35f;            // seconds between the title and the sub title coming in
    public boolean popups = true;             // notices pop up under the menu instead of going to chat
    public boolean fx(int i) { return switch (i) { case 0 -> fxPop; case 1 -> fxShine; case 2 -> fxWave; case 3 -> fxRainbow; case 4 -> fxDrop; case 5 -> fxPulse; case 6 -> fxGlitch; case 7 -> fxGlow; case 8 -> fxBounce; case 9 -> fxType; case 10 -> fxFloat; case 11 -> fxShake; case 12 -> fxFire; case 13 -> fxFrost; case 14 -> fxTwinkle; case 15 -> fxFade; case 16 -> fxRise; case 17 -> fxShrink; case 18 -> fxScatter; case 19 -> fxSlide; case 20 -> fxFlip; case 21 -> fxSwing; case 22 -> fxGold; case 23 -> fxSlideOut; default -> fxMelt; }; }
    public void setFx(int i, boolean v) { switch (i) { case 0 -> fxPop = v; case 1 -> fxShine = v; case 2 -> fxWave = v; case 3 -> fxRainbow = v; case 4 -> fxDrop = v; case 5 -> fxPulse = v; case 6 -> fxGlitch = v; case 7 -> fxGlow = v; case 8 -> fxBounce = v; case 9 -> fxType = v; case 10 -> fxFloat = v; case 11 -> fxShake = v; case 12 -> fxFire = v; case 13 -> fxFrost = v; case 14 -> fxTwinkle = v; case 15 -> fxFade = v; case 16 -> fxRise = v; case 17 -> fxShrink = v; case 18 -> fxScatter = v; case 19 -> fxSlide = v; case 20 -> fxFlip = v; case 21 -> fxSwing = v; case 22 -> fxGold = v; case 23 -> fxSlideOut = v; default -> fxMelt = v; } }
    public int barStyle = 0;                  // cinematic bar colour preset (AnimationManager.BAR_NAMES)
    public boolean cinematic = true;          // blur the game and close black bars from the top and bottom while a scene plays
    public boolean epicEffects = true;        // the RNG Drop scene for expensive drops
    public long epicThreshold = 20_000_000L;  // coins; drops worth this much play the RNG Drop scene AND are the cheapest drops announced in chat
    public float scale = 1f;                  // size of the animation scene
    public float textScale = 1f;              // size of the text under it (item name, price, odds)
    public float dyeSize = -1f, mythSize = -1f, expSize = -1f;   // animation size of Dye / Mythological / Experiments (-1 = same as Slayers)
    /** Animation size of one category (0 Slayers, 1 Dye, 2 Farming = its own farmScale, 3 Mythological, 4 Experiments). */
    public float sizeOf(int cat) {
        float v = switch (cat) { case 1 -> dyeSize; case 3 -> mythSize; case 4 -> expSize; default -> -1f; };
        return v < 0f ? size : Math.max(0.6f, Math.min(1.4f, v));
    }
    public void setSizeOf(int cat, float v) { switch (cat) { case 1 -> dyeSize = v; case 3 -> mythSize = v; case 4 -> expSize = v; default -> size = v; } }
    public float itemScale = 1f;              // size of the dropped item inside the scene
    public int chopsDefault = 2, chopsCool = 4, chopsEpic = 10;   // slices per animation (the nothing-dropped scene uses the default count)
    public Map<String, String> mcSounds = new LinkedHashMap<>();   // the same slots, but for the Minecraft sound set (used while Custom sounds is off)
    public Map<String, String> sounds = new LinkedHashMap<>();   // "<scene>.<slot>" -> sound name, see SoundFx
    /** The chat message for an event: 0 Slayers, 1 Dye, 2 Farming. */
    public String formatFor(int ev) {
        if (ev == 4) return formatExp != null ? formatExp : DEFAULT_EXP_FORMAT;
        return ev == 3 ? (formatMyth != null ? formatMyth : DEFAULT_MYTH_FORMAT) : ev == 2 ? (formatFarm != null ? formatFarm : DEFAULT_FARM_FORMAT) : ev == 1 ? (formatDye != null ? formatDye : DEFAULT_DYE_FORMAT) : format;
    }
    public void setFormat(int ev, String v) { if (ev == 4) formatExp = v; else if (ev == 3) formatMyth = v; else if (ev == 2) formatFarm = v; else if (ev == 1) formatDye = v; else format = v; }

    /** Cinematic bars for one scene: 0 Slayers, 1 Dye, 3 Mythological. Farming (2), Mining and Fishing never use them. */
    public boolean cine(int cat) {
        return switch (cat) { case 0 -> cinematic; case 1 -> cineDye != null ? cineDye : cinematic; case 3 -> cineMytho != null ? cineMytho : cinematic; case 4 -> cineExp != null ? cineExp : cinematic; default -> false; };
    }
    public void setCine(int cat, boolean v) { switch (cat) { case 0 -> cinematic = v; case 1 -> cineDye = v; case 3 -> cineMytho = v; case 4 -> cineExp = v; default -> { } } }
    /** Background blur for one scene (same categories as {@link #cine}). */
    public boolean blur(int cat) {
        return switch (cat) { case 0 -> animBlur; case 1 -> blurDye != null ? blurDye : animBlur; case 3 -> blurMytho != null ? blurMytho : animBlur; case 4 -> blurExp != null ? blurExp : animBlur; default -> false; };
    }
    public void setBlur(int cat, boolean v) { switch (cat) { case 0 -> animBlur = v; case 1 -> blurDye = v; case 3 -> blurMytho = v; case 4 -> blurExp = v; default -> { } } }

    public int posBars = 2;             // with cinematic bars: 0 top, 1 middle, 2 bottom centre
    public int pos9 = 7;                // without bars: 3 x 3 grid, row by row (7 = bottom centre)
    public int mythStrikes = 3, mythStrikesRng = 5;   // lightning strikes before the Mythological drop shows (1..10)
    public java.util.Map<String, Float> animSecs = new java.util.HashMap<>();   // how long each animation lasts, in seconds (5..10); missing = its natural length
    public java.util.Map<String, Float> rngOpts = new java.util.HashMap<>();    // RNG Drop items, per animation ("rng", "mythrng", "exprng"): "<key>.spin" turns per speed (0 = off, 0..3), "<key>.pop" size (1..2)
    public float rngOpt(String key, String what, float def) { Float v = rngOpts == null ? null : rngOpts.get(key + "." + what); return v == null ? def : v; }
    public static float rngSpinDef() { return 1f; }
    public static float rngPopDef() { return 1.5f; }
    public java.util.Map<String, Float> textSecs = new java.util.HashMap<>();   // seconds after the animation starts until the title and info appear (3..10); missing = 4
    public float textDelay(String key) { return "farm".equals(key) || "helios".equals(key) || "pest".equals(key) ? 0f : Math.max(3f, Math.min(10f, textDelayAll)); }       // Farming's words follow the reveal by themselves
    public void setBarPx(int px) { barPx = Math.max(5, Math.min(15, px)); barHeight = Math.round(barPx * 100f / 96f); }
    public int mythPet = 5;                       // Mythological: the Griffin pet rarity drop chances are shown for (0 Common .. 5 Mythic)
    public int mythPosBars = -1, mythPos9 = -1;   // Mythological scene positions (-1 = same as Slayers)
    public int expPosBars = -1, expPos9 = -1;     // Experimentation Table scene positions (-1 = same as Slayers)
    public int expFlips = 4, expFlipsRng = 8;     // pages the book flips before the Experiments drop shows (1..12)
    public int dyePosBars = -1, dyePos9 = -1;   // Dye scene positions (-1 = same as Slayers)
    public int farmPos = 1;             // Farming scene: 3 x 3 grid, row by row (1 = top centre), 9 = custom spot
    public float farmX = 0.5f, farmY = 0.2f;     // the custom spot: where the scene's centre sits, as a share of the screen
    public float farmScale = 1f;        // Farming scene size, 50 to 110 %
    public int textPos = 1;             // where the text goes when there is a choice: 0 top, 1 bottom
    public int offsetX = 0, offsetY = 0; // fine tuning for the presets
    public int previewAt = 1;                // live preview dock on the Animations and Sounds pages: 0 top, 1 bottom, 2 off
    public int menuTranslucency = 25;        // 0 = solid menu .. 70 = see-through (General settings)
    public boolean menuIcons = true;          // sidebar and row icons move
    public boolean menuAnimated = true;       // menu slides in, pages fade, accent bar flows
    public int uiFont = 0, textFont = 0;                       // Fonts: the menu (Settings > Menu) and the drop texts (Text Changes)
    public boolean othersSuffix = true, othersName = true, othersAura = true;      // see other players' cosmetics: suffixes, name effects, auras
    public boolean showTips = true;           // the rotating tip card on the Home page
    public String adminToken = "";                  // Admin Panel: a GitHub token that lets Save write cosmetics.json
    public boolean selfSuffix = true, selfName = true, selfAura = true;           // show your own cosmetics
    public String profitHudSlayer = "all";    // which slayer the Stats page shows ("all" = combined)
    public boolean useBazaar = true, useAuctions = true;
    public String bazaarPrice = "BUY";  // BUY = instant buy, SELL = instant sell
    public String priceSource = "HYPIXEL_AUCTIONS"; // auction source: HYPIXEL_AUCTIONS or LOWEST_BIN_FEED
    public Map<String, SlayerCfg> slayers = new LinkedHashMap<>();
    public List<CustomDrop> custom = new ArrayList<>();

    private static FancyConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("fancydrops.json"); }

    public static FancyConfig get() { if (instance == null) load(); return instance; }

    public static void load() {
        try {
            Path p = path();
            if (!Files.exists(p)) {          // coming from RNGDrops: take the old settings with us
                Path old = FabricLoader.getInstance().getConfigDir().resolve("rngdrops.json");
                if (Files.exists(old)) p = old;
            }
            if (Files.exists(p)) instance = GSON.fromJson(Files.readString(p), FancyConfig.class);
        } catch (Exception e) { e.printStackTrace(); }
        if (instance == null) instance = new FancyConfig();
        if (instance.firstVersion == null || instance.firstVersion.isEmpty()) { instance.firstVersion = FancyDropsMod.VERSION; save(); }
        if (instance.nameFx == null) instance.nameFx = "";
        if (instance.aura == null) instance.aura = "";
        instance.fixChops();
        if (!instance.enabled) { instance.masterAnimate = false; instance.masterAnnounce = false; instance.enabled = true; }   // older configs had one master switch
        instance.scale = Math.max(0.5f, Math.min(1.1f, instance.scale));
        if (instance.size < 0f) instance.size = instance.scale / 0.8f;
        instance.size = Math.max(0.6f, Math.min(1.4f, instance.size));
        instance.textScale = Math.max(0f, Math.min(1.1f, instance.textScale));
        instance.itemScale = Math.max(0.5f, Math.min(1.1f, instance.itemScale));
        instance.barHeight = Math.max(0, Math.min(25, instance.barHeight));
        instance.barPx = Math.max(5, Math.min(15, Math.round(instance.barHeight * 96f / 100f)));
        instance.textDelayAll = Math.max(3f, Math.min(10f, instance.textDelayAll));
        instance.titleScale = Math.max(0.5f, Math.min(1.5f, instance.titleScale)); instance.subScale = Math.max(0.5f, Math.min(1.5f, instance.subScale)); instance.subDelay = Math.max(0f, Math.min(3f, instance.subDelay));
        instance.farmPos = Math.max(0, Math.min(9, instance.farmPos));
        instance.farmX = Math.max(0f, Math.min(1f, instance.farmX)); instance.farmY = Math.max(0f, Math.min(1f, instance.farmY));
        instance.mythPet = Math.max(0, Math.min(5, instance.mythPet));
        instance.mythStrikes = Math.max(1, Math.min(10, instance.mythStrikes)); instance.mythStrikesRng = Math.max(1, Math.min(10, instance.mythStrikesRng));
        if (instance.textSecs == null) instance.textSecs = new java.util.HashMap<>();
        instance.textSecs.replaceAll((k, v) -> v == null ? 4f : Math.max(3f, Math.min(10f, v)));
        if (instance.animSecs == null) instance.animSecs = new java.util.HashMap<>();
        if (instance.rngOpts == null) instance.rngOpts = new java.util.HashMap<>();
        instance.rngOpts.replaceAll((k, v) -> v == null ? 1f : k.endsWith(".pop") ? Math.max(1f, Math.min(2.5f, v)) : Math.max(0f, Math.min(3f, v)));
        instance.itemSpin = Math.max(0f, Math.min(2f, instance.itemSpin)); instance.itemGlow = Math.max(0f, Math.min(1.5f, instance.itemGlow));
        instance.animSecs.replaceAll((k, v) -> v == null ? null : Math.max(5f, Math.min(10f, v)));
        instance.mythPosBars = Math.max(-1, Math.min(2, instance.mythPosBars)); instance.mythPos9 = Math.max(-1, Math.min(8, instance.mythPos9));
        instance.expPosBars = Math.max(-1, Math.min(2, instance.expPosBars)); instance.expPos9 = Math.max(-1, Math.min(8, instance.expPos9));
        instance.expFlips = Math.max(1, Math.min(12, instance.expFlips)); instance.expFlipsRng = Math.max(1, Math.min(12, instance.expFlipsRng));
        instance.dyePosBars = Math.max(-1, Math.min(2, instance.dyePosBars)); instance.dyePos9 = Math.max(-1, Math.min(8, instance.dyePos9));
        instance.farmScale = Math.max(0.5f, Math.min(1.1f, instance.farmScale <= 0 ? 1f : instance.farmScale));
        instance.slayerStyle = Math.max(0, Math.min(1, instance.slayerStyle));
        instance.dyeStyle = Math.max(0, Math.min(1, instance.dyeStyle)); instance.farmStyle = Math.max(0, Math.min(1, instance.farmStyle));
        instance.mythStyle = Math.max(0, Math.min(1, instance.mythStyle)); instance.expStyle = Math.max(0, Math.min(1, instance.expStyle));
        instance.menuTranslucency = Math.max(0, Math.min(70, instance.menuTranslucency));
        instance.previewAt = Math.max(0, Math.min(2, instance.previewAt));
        instance.posBars = Math.max(0, Math.min(2, instance.posBars)); instance.pos9 = Math.max(0, Math.min(8, instance.pos9)); instance.textPos = Math.max(0, Math.min(1, instance.textPos));
        if (instance.profitHudSlayer == null) instance.profitHudSlayer = "all";
        if (instance.formatVer < 3) { instance.format = DEFAULT_FORMAT; instance.formatDye = instance.formatFarm = instance.formatMyth = instance.formatExp = null; instance.formatVer = 3; }       // every message starts over with the new placeholders
        if (instance.formatVer < 4) {                                                                 // Dye and Farming messages have no {source} any more
            if (instance.formatFarm != null) instance.formatFarm = instance.formatFarm.replaceAll("\\s*(?:from\\s+)?\\{source}", "").trim();
            if (instance.formatDye != null) instance.formatDye = instance.formatDye.replaceAll("\\s*(?:from\\s+)?\\{source}", "").trim();
            instance.formatVer = 4;
        }
        if (instance.format == null || instance.format.isBlank() || !ChatFilter.ok(instance.format)) instance.format = DEFAULT_FORMAT;
        if (instance.formatDye != null && !ChatFilter.ok(instance.formatDye)) instance.formatDye = null;
        if (instance.formatFarm != null && !ChatFilter.ok(instance.formatFarm)) instance.formatFarm = null;
        if (instance.formatMyth != null && !ChatFilter.ok(instance.formatMyth)) instance.formatMyth = null;
        if (instance.formatExp != null && !ChatFilter.ok(instance.formatExp)) instance.formatExp = null;
    }

    /** Sets the two master switches (null = leave as is). */
    public void setMaster(Boolean animate, Boolean announce) {
        if (animate != null) masterAnimate = animate; if (announce != null) masterAnnounce = announce;
        enabled = masterAnimate || masterAnnounce;
    }

    public static void save() {
        try { Files.writeString(path(), GSON.toJson(get())); } catch (Exception e) { e.printStackTrace(); }
    }

    /** Default is the fewest slices, Rare Drop has more than Default, RNG Drop has more than Rare Drop. */
    public void fixChops() {
        chopsDefault = Math.max(1, Math.min(18, chopsDefault));
        chopsCool = Math.max(chopsDefault + 1, Math.min(19, chopsCool));
        chopsEpic = Math.max(chopsCool + 1, Math.min(20, chopsEpic));
    }

    /** The sliders show 0-110%; these turn them into the real multipliers (100% of Animation size = 40% of the frame, 50% of Text size = x0.8). */
    public float sceneK() { return scale * 0.4f; }
    public float textK() { return 0.6f + 0.4f * textScale; }
    public float itemK() { return itemScale; }

    public SlayerCfg slayer(String id) { return slayers.computeIfAbsent(id, k -> new SlayerCfg()); }
    public DropCfg drop(String slayerId, String name) { return slayer(slayerId).drops.computeIfAbsent(name, k -> new DropCfg()); }
}
