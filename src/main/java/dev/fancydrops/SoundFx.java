package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every sound the mod can play. Custom sounds are .ogg files in assets/fancydrops/sounds/ (listed in sounds.json); each has a vanilla
 * stand-in for when "custom sounds" is off. A few plain Minecraft sounds are offered as choices too.
 * Each animation has four sound slots (chop, split, reveal, end) and the player picks the sound for every slot in the Sounds tab.
 */
public final class SoundFx {
    public static final class Fx {
        public final String name, label; final SoundEvent custom, vanilla; final float baseVolume;
        Fx(String name, String label, SoundEvent vanilla, float baseVolume, boolean hasOgg) {
            this.name = name; this.label = label; this.vanilla = vanilla; this.baseVolume = baseVolume;
            this.custom = hasOgg ? SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("fancydrops", name)) : null;
        }
    }

    public static final String NONE = "none";
    public static final String[] SCENES = {"default", "cool", "epic", "nothing", "dye", "farm", "mytho", "exp", "helios"};
    public static final String[] SLOTS = {"chop", "split", "reveal", "linger", "end"};

    private static final Map<String, Fx> ALL = new LinkedHashMap<>();
    private static Fx reg(String name, String label, SoundEvent vanilla, float vol) { Fx f = new Fx(name, label, vanilla, vol, true); ALL.put(name, f); return f; }
    private static Fx vanilla(String name, String label, SoundEvent e, float vol) { Fx f = new Fx(name, label, e, vol, false); ALL.put(name, f); return f; }

    // custom (ogg)
    public static final Fx SLICE = reg("slice", "Slice", SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f);
    public static final Fx CLEAVE = reg("cleave", "Cleave", SoundEvents.PLAYER_ATTACK_STRONG, 1.0f);
    public static final Fx CRACK = reg("crack", "Crack", SoundEvents.PLAYER_ATTACK_CRIT, 0.9f);
    public static final Fx THUNK = reg("thunk", "Thunk", SoundEvents.ANVIL_LAND, 0.7f);
    public static final Fx WHOOSH = reg("whoosh", "Whoosh", SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f);
    public static final Fx POP = reg("pop", "Tink", SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.8f);
    public static final Fx COIN = reg("coin", "Coin", SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9f);
    public static final Fx REVEAL = reg("reveal", "Sparkle", SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f);
    public static final Fx SHIMMER = reg("shimmer", "Shimmer", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9f);
    public static final Fx CHOIR = reg("choir", "Choir", SoundEvents.BEACON_ACTIVATE, 0.8f);
    public static final Fx BOOM = reg("boom", "Boom", SoundEvents.GENERIC_EXPLODE.value(), 0.7f);
    public static final Fx EPIC = reg("epic", "Fanfare", SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9f);
    public static final Fx SAD = reg("sad", "Trombone", SoundEvents.VILLAGER_NO, 0.9f);
    public static final Fx FADE = reg("fade", "Fade", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f);
    public static final Fx KATANA = reg("katana", "Katana", SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f);
    public static final Fx ZAP = reg("zap", "Zap", SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7f);
    public static final Fx TAIKO = reg("taiko", "Taiko", SoundEvents.ANVIL_LAND, 0.9f);
    public static final Fx GONG = reg("gong", "Gong", SoundEvents.BELL_BLOCK, 0.8f);
    public static final Fx HARP = reg("harp", "Harp", SoundEvents.NOTE_BLOCK_HARP.value(), 0.8f);
    public static final Fx MAGIC = reg("magic", "Magic", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f);
    public static final Fx GLASS = reg("glass", "Glass", SoundEvents.GLASS_BREAK, 0.7f);
    public static final Fx BELL = reg("bell", "Bell", SoundEvents.BELL_RESONATE, 0.8f);
    // the studio set (synthesised): clean, layered and reverbed
    public static final Fx SILK = reg("silk", "Silk", SoundEvents.PLAYER_ATTACK_SWEEP, 0.85f);
    public static final Fx IMPACT = reg("impact", "Impact", SoundEvents.ANVIL_LAND, 0.9f);
    public static final Fx CRYSTAL = reg("crystal", "Crystal", SoundEvents.AMETHYST_BLOCK_CHIME, 0.85f);
    public static final Fx RISE = reg("rise", "Riser", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f);
    public static final Fx GLIMMER = reg("glimmer", "Glimmer", SoundEvents.AMETHYST_BLOCK_CHIME, 0.85f);
    public static final Fx BLOOM = reg("bloom", "Bloom", SoundEvents.BEACON_ACTIVATE, 0.8f);
    public static final Fx DROP808 = reg("drop808", "Sub", SoundEvents.GENERIC_EXPLODE.value(), 0.8f);
    public static final Fx TWINKLE = reg("twinkle", "Twinkle", SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f);
    public static final Fx VELVET = reg("velvet", "Velvet", SoundEvents.NOTE_BLOCK_BELL.value(), 0.8f);
    public static final Fx SWOOSH = reg("swoosh", "Swoosh", SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f);
    public static final Fx SPLAT = reg("splat", "Splat", SoundEvents.SLIME_BLOCK_PLACE, 0.85f);
    // the sleek set: smooth, warm and glossy
    public static final Fx SATIN = reg("satin", "Tick", SoundEvents.PLAYER_ATTACK_SWEEP, 0.85f);
    public static final Fx THUMP = reg("thump", "Kick", SoundEvents.ANVIL_LAND, 0.9f);
    public static final Fx HALO = reg("halo", "Chimes", SoundEvents.BEACON_ACTIVATE, 0.8f);
    public static final Fx CASCADE = reg("cascade", "Run", SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f);
    public static final Fx LIFT = reg("lift", "Rise", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f);
    public static final Fx AURORA = reg("aurora", "Pair", SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f);
    public static final Fx NECTAR = reg("nectar", "Pluck", SoundEvents.NOTE_BLOCK_BELL.value(), 0.85f);
    public static final Fx LULL = reg("lull", "Lull", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f);
    public static final Fx EMBER = reg("ember", "Ember", SoundEvents.BEACON_AMBIENT, 0.7f);
    public static final Fx SIGH = reg("sigh", "Sigh", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f);
    public static final Fx RIPPLE = reg("ripple", "Ripple", SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f);
    public static final Fx MEADOW = reg("meadow", "Meadow", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f);
    public static final Fx STORM = reg("storm", "Storm", SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f);
    public static final Fx CHANT = reg("chant", "Chant", SoundEvents.ENCHANTMENT_TABLE_USE, 0.7f);
    public static final Fx DUSK = reg("dusk", "Whoosh", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f);
    public static final Fx PLOP = reg("plop", "Plop", SoundEvents.SLIME_BLOCK_PLACE, 0.85f);
    public static final Fx BURST = reg("burst", "Burst", SoundEvents.ANVIL_LAND, 0.85f);
    public static final Fx ARC = reg("arc", "Arc", SoundEvents.AMETHYST_BLOCK_CHIME, 0.85f);
    public static final Fx SCYTHE = reg("scythe", "Scythe", SoundEvents.PLAYER_ATTACK_SWEEP, 0.85f);
    public static final Fx PAGE = reg("page", "Page", SoundEvents.BOOK_PAGE_TURN, 0.9f);
    public static final Fx GLOW = reg("glow", "Glow", SoundEvents.AMETHYST_BLOCK_CHIME, 0.85f);
    public static final Fx FLICK = reg("flick", "Tink", SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f);
    // plain Minecraft sounds
    static {
        vanilla("v_hit", "MC Hit", SoundEvents.PLAYER_ATTACK_STRONG, 0.8f);
        vanilla("v_anvil", "MC Anvil", SoundEvents.ANVIL_LAND, 0.6f);
        vanilla("v_levelup", "MC Level", SoundEvents.PLAYER_LEVELUP, 0.7f);
        vanilla("v_totem", "MC Totem", SoundEvents.TOTEM_USE, 0.6f);
        vanilla("v_firework", "MC Firework", SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f);
        vanilla("v_pageturn", "MC Page", SoundEvents.BOOK_PAGE_TURN, 1f);
        vanilla("v_enchant", "MC Enchant", SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f);
        vanilla("v_thunder", "MC Thunder", SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f);
    }

    /** What each slot plays until the player changes it. */
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    static {
        String[][] d = {                                         // every animation plays from the same soft set as the default scenes
            {"default", "satin", "thump", "nectar", "lull", "dusk"},
            {"cool", "satin", "thump", "cascade", "lull", "dusk"},      // "cool" = Rare Drop
            {"epic", "flick", "thump", "halo", "ember", "dusk"},         // "epic" = RNG Drop
            {"nothing", "flick", "thump", "lift", "sigh", "dusk"},
            {"dye", "plop", "burst", "cascade", "ripple", "dusk"},
            {"farm", "scythe", "nectar", "cascade", "meadow", "dusk"},
            {"mytho", "arc", "thump", "halo", "storm", "dusk"},
            {"exp", "page", "glow", "halo", "chant", "dusk"},
            {"helios", "scythe", "nectar", "halo", "ember", "dusk"}};
        for (String[] r : d) for (int i = 0; i < SLOTS.length; i++) DEFAULTS.put(r[0] + "." + SLOTS[i], r[i + 1]);
    }

    /** Register sound event IDs before any playback; sounds.json alone only supplies the audio resource. */
    public static void register() {
        for (Fx fx : ALL.values()) {
            if (fx.custom == null) continue;
            Identifier id = Identifier.fromNamespaceAndPath("fancydrops", fx.name);
            if (!BuiltInRegistries.SOUND_EVENT.containsKey(id)) Registry.register(BuiltInRegistries.SOUND_EVENT, id, fx.custom);
        }
    }

    public static Iterable<String> names() { return ALL.keySet(); }
    public static Fx byName(String n) { return n == null ? null : ALL.get(n.toLowerCase(Locale.ROOT)); }

    /** Every choice for a slot, "none" first. */
    public static List<String> choices() { List<String> l = new ArrayList<>(); l.add(NONE); l.addAll(ALL.keySet()); return l; }
    public static String label(String name) { Fx f = byName(name); return f == null ? "None" : f.label; }

    /** The sound the player picked (or the default) for e.g. ("epic", "reveal"). */
    public static String chosen(String scene, String slot) {
        String key = scene + "." + slot;
        String v = FancyConfig.get().sounds.get(key);
        return v != null && (v.equals(NONE) || ALL.containsKey(v)) ? v : DEFAULTS.get(key);
    }
    public static void choose(String scene, String slot, String name) { FancyConfig.get().sounds.put(scene + "." + slot, name); }

    /** Master volume as a 0..1 factor (0 when sounds are off). */
    public static float master() {
        FancyConfig c = FancyConfig.get();
        if (!c.sound) return 0f;
        return Math.max(0f, Math.min(1f, c.volume / 100f));
    }

    /** Plays the sound chosen for a slot. */
    public static void playSlot(String scene, String slot, float pitch) { play(byName(chosen(scene, slot)), pitch, 1f); }

    public static void play(Fx fx, float pitch, float gain) {
        if (fx == null) return;
        float v = master() * fx.baseVolume * gain;
        if (v <= 0.001f) return;
        SoundEvent e = fx.custom != null && FancyConfig.get().customSounds ? fx.custom : fx.vanilla;
        var inst = SimpleSoundInstance.forUI(e, pitch, Math.min(1f, v));
        if (PREVIEWS.size() > 40) PREVIEWS.remove(0);
        PREVIEWS.add(inst);
        Minecraft.getInstance().getSoundManager().play(inst);
    }
    public static void play(Fx fx, float pitch) { play(fx, pitch, 1f); }

    private static final Map<String, Float> LEN = new java.util.HashMap<>();
    static {
        String[] d = ("arc 0.6;aurora 1.3;bloom 5.6;burst 0.9;cascade 1.3;chant 5.0;crystal 3.4;drop808 3.0;dusk 1.9;ember 4.7;flick 0.2;glimmer 4.8;glow 2.1;halo 1.8;impact 1.2;lift 0.9;lull 4.6;meadow 4.4;nectar 1.0;page 0.6;plop 0.6;ripple 5.0;rise 2.3;satin 0.7;scythe 0.8;sigh 3.2;silk 0.9;splat 1.1;storm 4.6;swoosh 1.2;thump 0.6;twinkle 1.8;velvet 3.8").split(";");
        for (String e : d) { String[] p = e.split(" "); LEN.put(p[0], Float.parseFloat(p[1])); }
    }
    /** How long a sound lasts, in seconds (an estimate for the ones that are Minecraft's own). */
    public static float duration(String name) { Float f = name == null ? null : LEN.get(name); return f != null ? f : 0.9f; }

    private static final List<net.minecraft.client.resources.sounds.SoundInstance> PREVIEWS = new ArrayList<>();
    /** Silences every sound started from the menu (so a long one does not keep ringing after you switch or pause). */
    public static void stopPreviews() {
        var sm = Minecraft.getInstance().getSoundManager();
        for (var i : PREVIEWS) sm.stop(i);
        PREVIEWS.clear();
    }

    /** Plays a sound even if sounds are muted (menu / soundtest) - at the chosen volume, minimum 40%. */
    public static void preview(Fx fx) {
        if (fx == null) return;
        FancyConfig c = FancyConfig.get();
        float vol = Math.max(0.4f, Math.min(1f, c.volume / 100f));
        SoundEvent e = fx.custom != null && c.customSounds ? fx.custom : fx.vanilla;
        var inst = SimpleSoundInstance.forUI(e, 1f, Math.min(1f, vol * fx.baseVolume));
        if (PREVIEWS.size() > 40) PREVIEWS.remove(0);
        PREVIEWS.add(inst);
        Minecraft.getInstance().getSoundManager().play(inst);
    }

    private SoundFx() {}
}
