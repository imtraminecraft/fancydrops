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
    public static final String DEFAULT_FARM_FORMAT = "RARE CROP! {item} ({price}) from {source}";

    public boolean enabled = true;       // legacy; kept equal to "animate or announce"
    public boolean masterAnimate = true;  // master: play any animation at all
    public boolean masterAnnounce = true; // master: send any chat announcement at all
    public String channel = "PARTY";    // PARTY | COOP | OFF
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
    public boolean show3d = true;             // dropped item is a real spinning 3D model instead of a flat icon
    public Boolean cineDye, cineMytho, cineExp, blurDye, blurMytho, blurExp;   // each scene can have its own bars and blur (null = same as Slayers)
    public float size = -1f;                  // animation size, 100% = 1.0 (older configs stored it as 80% = 1.0; converted on load)
    public boolean animBlur = true;           // blur the game behind a scene
    public int barHeight = 16;                // each black cinematic bar, as a percentage of the screen height
    public int slayerStyle = 0;               // 0 Sword (sword slices), 1 Spirit (soul slashes)
    public int barStyle = 0;                  // cinematic bar colour preset (AnimationManager.BAR_NAMES)
    public boolean cinematic = true;          // blur the game and close black bars from the top and bottom while a scene plays
    public boolean epicEffects = true;        // the RNG Drop scene for expensive drops
    public long epicThreshold = 20_000_000L;  // coins; drops worth this much play the RNG Drop scene AND are the cheapest drops announced in chat
    public float scale = 1f;                  // size of the animation scene
    public float textScale = 1f;              // size of the text under it (item name, price, odds)
    public float itemScale = 1f;              // size of the dropped item inside the scene
    public int chopsDefault = 2, chopsCool = 4, chopsEpic = 10;   // slices per animation (the nothing-dropped scene uses the default count)
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
    public java.util.Map<String, Float> animSecs = new java.util.HashMap<>();   // how long each animation lasts, in seconds (2..10); missing = its natural length
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
        instance.fixChops();
        if (!instance.enabled) { instance.masterAnimate = false; instance.masterAnnounce = false; instance.enabled = true; }   // older configs had one master switch
        instance.scale = Math.max(0.5f, Math.min(1.1f, instance.scale));
        if (instance.size < 0f) instance.size = instance.scale / 0.8f;
        instance.size = Math.max(0.6f, Math.min(1.4f, instance.size));
        instance.textScale = Math.max(0f, Math.min(1.1f, instance.textScale));
        instance.itemScale = Math.max(0.5f, Math.min(1.1f, instance.itemScale));
        instance.barHeight = Math.max(0, Math.min(25, instance.barHeight));
        instance.farmPos = Math.max(0, Math.min(9, instance.farmPos));
        instance.farmX = Math.max(0f, Math.min(1f, instance.farmX)); instance.farmY = Math.max(0f, Math.min(1f, instance.farmY));
        instance.mythPet = Math.max(0, Math.min(5, instance.mythPet));
        instance.mythStrikes = Math.max(1, Math.min(10, instance.mythStrikes)); instance.mythStrikesRng = Math.max(1, Math.min(10, instance.mythStrikesRng));
        if (instance.animSecs == null) instance.animSecs = new java.util.HashMap<>();
        instance.animSecs.replaceAll((k, v) -> v == null ? null : Math.max(2f, Math.min(10f, v)));
        instance.mythPosBars = Math.max(-1, Math.min(2, instance.mythPosBars)); instance.mythPos9 = Math.max(-1, Math.min(8, instance.mythPos9));
        instance.expPosBars = Math.max(-1, Math.min(2, instance.expPosBars)); instance.expPos9 = Math.max(-1, Math.min(8, instance.expPos9));
        instance.expFlips = Math.max(1, Math.min(12, instance.expFlips)); instance.expFlipsRng = Math.max(1, Math.min(12, instance.expFlipsRng));
        instance.dyePosBars = Math.max(-1, Math.min(2, instance.dyePosBars)); instance.dyePos9 = Math.max(-1, Math.min(8, instance.dyePos9));
        instance.farmScale = Math.max(0.5f, Math.min(1.1f, instance.farmScale <= 0 ? 1f : instance.farmScale));
        instance.slayerStyle = Math.max(0, Math.min(1, instance.slayerStyle));
        instance.menuTranslucency = Math.max(0, Math.min(70, instance.menuTranslucency));
        instance.previewAt = Math.max(0, Math.min(2, instance.previewAt));
        instance.posBars = Math.max(0, Math.min(2, instance.posBars)); instance.pos9 = Math.max(0, Math.min(8, instance.pos9)); instance.textPos = Math.max(0, Math.min(1, instance.textPos));
        if (instance.profitHudSlayer == null) instance.profitHudSlayer = "all";
        if (instance.formatVer < 3) { instance.format = DEFAULT_FORMAT; instance.formatDye = instance.formatFarm = instance.formatMyth = instance.formatExp = null; instance.formatVer = 3; }       // every message starts over with the new placeholders
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
