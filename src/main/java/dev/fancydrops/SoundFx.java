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
    public static final String[] SCENES = {"default", "cool", "epic", "nothing", "dye", "farm", "mytho", "exp", "helios", "mythnothing", "pest", "treasure"};
    public static final String[] SLOTS = {"chop", "split", "reveal", "linger", "end"};

    private static final Map<String, Fx> ALL = new LinkedHashMap<>();
    private static Fx reg(String name, String label, SoundEvent vanilla, float vol) { Fx f = new Fx(name, label, vanilla, vol, true); ALL.put(name, f); return f; }
    /** A plain Minecraft sound by its id ("block.note_block.chime"): only offered while Custom sounds is off. */
    private static Fx mc(String name, String label, String id, float vol) {
        Fx f = new Fx(name, label, SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("minecraft", id)), vol, false);
        ALL.put(name, f); return f;
    }
    private static boolean isMc(String name) { return name != null && name.startsWith("mc_"); }

    // custom (ogg): a bank written for this mod, grouped by the moment it is meant for. Each ends in a soft tail, so nothing is cut off.
    // chop
    public static final Fx FLICK = reg("flick", "Tink", SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.8f);
    public static final Fx SATIN = reg("satin", "Tick", SoundEvents.PLAYER_ATTACK_SWEEP, 0.85f);
    public static final Fx MARIMBA = reg("marimba", "Marimba", SoundEvents.NOTE_BLOCK_XYLOPHONE.value(), 0.9f);
    public static final Fx PLUCK = reg("pluck", "Pluck", SoundEvents.NOTE_BLOCK_HARP.value(), 0.9f);
    public static final Fx DRIP = reg("drip", "Drip", SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.85f);
    public static final Fx BLIP = reg("blip", "Blip", SoundEvents.NOTE_BLOCK_BIT.value(), 0.6f);
    // split
    public static final Fx THUMP = reg("thump", "Kick", SoundEvents.ANVIL_LAND, 0.95f);
    public static final Fx BURST = reg("burst", "Burst", SoundEvents.GENERIC_EXPLODE.value(), 0.85f);
    public static final Fx SNAP = reg("snap", "Snap", SoundEvents.PLAYER_ATTACK_CRIT, 0.85f);
    public static final Fx SLAM = reg("slam", "Slam", SoundEvents.ANVIL_LAND, 0.8f);
    public static final Fx PLOP = reg("plop", "Plop", SoundEvents.SLIME_BLOCK_PLACE, 0.9f);
    public static final Fx ARC = reg("arc", "Arc", SoundEvents.LIGHTNING_BOLT_IMPACT, 0.75f);
    // reveal
    public static final Fx HALO = reg("halo", "Chimes", SoundEvents.BEACON_ACTIVATE, 0.8f);
    public static final Fx NECTAR = reg("nectar", "Pluck run", SoundEvents.NOTE_BLOCK_BELL.value(), 0.9f);
    public static final Fx CASCADE = reg("cascade", "Run", SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f);
    public static final Fx LIFT = reg("lift", "Rise", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f);
    public static final Fx SPARKLE = reg("sparkle", "Sparkle", SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f);
    public static final Fx FANFARE = reg("fanfare", "Fanfare", SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.75f);
    public static final Fx BLOOM = reg("bloom", "Bloom", SoundEvents.BEACON_ACTIVATE, 0.7f);
    public static final Fx SPROUT = reg("sprout", "Sprout", SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f);
    public static final Fx BEAM = reg("beam", "Beam", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f);
    public static final Fx FLARE = reg("flare", "Flare", SoundEvents.BEACON_ACTIVATE, 0.8f);
    public static final Fx GLOW = reg("glow", "Glow", SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f);
    public static final Fx PAGE = reg("page", "Page", SoundEvents.BOOK_PAGE_TURN, 0.9f);
    // linger
    public static final Fx LULL = reg("lull", "Lull", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f);
    public static final Fx EMBER = reg("ember", "Ember", SoundEvents.BEACON_AMBIENT, 0.7f);
    public static final Fx RIPPLE = reg("ripple", "Ripple", SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f);
    public static final Fx STORM = reg("storm", "Storm", SoundEvents.LIGHTNING_BOLT_THUNDER, 0.55f);
    public static final Fx CHANT = reg("chant", "Chant", SoundEvents.ENCHANTMENT_TABLE_USE, 0.7f);
    public static final Fx BREEZE = reg("breeze", "Breeze", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.75f);
    public static final Fx SHIMMER = reg("shimmer", "Shimmer", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f);
    public static final Fx HUM = reg("hum", "Hum", SoundEvents.BEACON_AMBIENT, 0.7f);
    // end
    public static final Fx DUSK = reg("dusk", "Whoosh", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f);
    public static final Fx SIGH = reg("sigh", "Sigh", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f);
    public static final Fx SETTLE = reg("settle", "Settle", SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f);
    public static final Fx FADE = reg("fade", "Fade", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6f);
    // one-shot
    public static final Fx REAP = reg("reap", "Reap", SoundEvents.PLAYER_ATTACK_SWEEP, 0.85f);
    public static final Fx RUSTLE = reg("rustle", "Rustle", SoundEvents.CROP_BREAK, 0.85f);
    public static final Fx VACUUM = reg("vacuum", "Vacuum", SoundEvents.PLAYER_ATTACK_SWEEP, 0.85f);
    public static final Fx SQUEAK = reg("squeak", "Squeak", SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.85f);
    // the Mythological bank, written new for the ritual scenes: every one fades to silence by itself. "_big" twins play in the RNG scene.
    static {
        reg("myth_bolt", "Bolt", SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8f);
        reg("myth_shatter", "Shatter", SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f);
        reg("myth_reveal", "Reveal", SoundEvents.AMETHYST_BLOCK_CHIME, 0.85f);
        reg("myth_reveal_big", "Reveal (big)", SoundEvents.BEACON_ACTIVATE, 0.9f);
        reg("myth_glow", "Glow", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f);
        reg("myth_glow_big", "Glow (big)", SoundEvents.BEACON_AMBIENT, 0.7f);
        reg("myth_end", "Farewell", SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f);
        reg("myth_dig", "Dig", SoundEvents.CROP_BREAK, 0.9f);
        reg("myth_chest", "Chest", SoundEvents.AMETHYST_BLOCK_CHIME, 0.85f);
        reg("myth_fizzle", "Fizzle", SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f);
    }

    // Minecraft's own sounds, picked to work as a set: bright notes for the stepping slices, a solid hit, a reward, a long glow, a soft close
    static {
        mc("mc_chime", "Chime", "block.note_block.chime", 0.9f);
        mc("mc_bell", "Bell", "block.note_block.bell", 0.9f);
        mc("mc_harp", "Harp", "block.note_block.harp", 0.9f);
        mc("mc_xylo", "Xylophone", "block.note_block.iron_xylophone", 0.9f);
        mc("mc_flute", "Flute", "block.note_block.flute", 0.9f);
        mc("mc_pling", "Pling", "block.note_block.pling", 0.7f);
        mc("mc_bit", "Bit", "block.note_block.bit", 0.7f);
        mc("mc_bass", "Bass", "block.note_block.bass", 1.0f);
        mc("mc_kick", "Kick", "block.note_block.basedrum", 1.0f);
        mc("mc_snare", "Snare", "block.note_block.snare", 0.8f);
        mc("mc_crystal", "Crystal", "block.amethyst_block.chime", 0.9f);
        mc("mc_shimmer", "Shimmer", "block.amethyst_block.resonate", 0.8f);
        mc("mc_swish", "Swish", "entity.player.attack.sweep", 0.8f);
        mc("mc_crit", "Crit", "entity.player.attack.crit", 0.8f);
        mc("mc_smack", "Smack", "entity.player.attack.strong", 0.8f);
        mc("mc_orb", "Orb", "entity.experience_orb.pickup", 0.8f);
        mc("mc_levelup", "Level up", "entity.player.levelup", 0.7f);
        mc("mc_fanfare", "Fanfare", "ui.toast.challenge_complete", 0.8f);
        mc("mc_totem", "Totem", "item.totem.use", 0.5f);
        mc("mc_thunder", "Thunder", "entity.lightning_bolt.thunder", 0.5f);
        mc("mc_bolt", "Lightning", "entity.lightning_bolt.impact", 0.6f);
        mc("mc_trident", "Trident", "item.trident.thunder", 0.5f);
        mc("mc_beacon", "Beacon", "block.beacon.activate", 0.7f);
        mc("mc_hum", "Beacon hum", "block.beacon.ambient", 0.7f);
        mc("mc_powerdown", "Power down", "block.beacon.deactivate", 0.7f);
        mc("mc_power", "Power", "block.beacon.power_select", 0.7f);
        mc("mc_conduit", "Conduit", "block.conduit.ambient", 0.7f);
        mc("mc_enchant", "Enchant", "block.enchantment_table.use", 0.8f);
        mc("mc_page", "Page", "item.book.page_turn", 0.9f);
        mc("mc_squish", "Squish", "entity.slime.squish", 0.8f);
        mc("mc_gift", "Gift", "entity.allay.item_given", 0.9f);
        mc("mc_fizzle", "Fizzle", "block.respawn_anchor.deplete", 0.7f);
        mc("mc_warp", "Warp", "entity.enderman.teleport", 0.6f);
        mc("mc_twinkle", "Twinkle", "entity.firework_rocket.twinkle", 0.8f);
        mc("mc_harvest", "Harvest", "block.sweet_berry_bush.pick_berries", 0.9f);
        mc("mc_anvil", "Anvil", "block.anvil.land", 0.5f);
        mc("mc_glass", "Glass", "block.glass.break", 0.6f);
        mc("mc_nope", "Nope", "entity.villager.no", 0.5f);
    }

    private static final Map<String, String> MC_DEFAULTS = new LinkedHashMap<>();
    static {
        String[][] d = {                                           // the same five moments as the custom set, built only from Minecraft sounds
            {"default", "mc_chime", "mc_crit", "mc_orb", "mc_shimmer", "mc_powerdown"},
            {"cool", "mc_bell", "mc_trident", "mc_levelup", "mc_hum", "mc_powerdown"},
            {"epic", "mc_xylo", "mc_bolt", "mc_fanfare", "mc_hum", "mc_powerdown"},
            {"nothing", "mc_bass", "mc_fizzle", "mc_nope", NONE, NONE},
            {"dye", "mc_squish", "mc_twinkle", "mc_gift", "mc_shimmer", "mc_powerdown"},
            {"farm", "mc_swish", "mc_harvest", "mc_orb", "mc_pling", "mc_orb"},
            {"mytho", "mc_bolt", "mc_thunder", "mc_power", "mc_hum", "mc_powerdown"},
            {"exp", "mc_page", "mc_enchant", "mc_levelup", "mc_shimmer", "mc_powerdown"},
            {"helios", "mc_swish", "mc_beacon", "mc_orb", "mc_pling", "mc_orb"},
            {"mythnothing", "mc_bolt", "mc_fizzle", NONE, NONE, NONE},
            {"pest", "mc_swish", "mc_crit", "mc_orb", "mc_pling", "mc_orb"},
            {"treasure", "mc_harvest", "mc_anvil", "mc_levelup", "mc_hum", "mc_powerdown"}};
        for (String[] r : d) for (int i = 0; i < SLOTS.length; i++) MC_DEFAULTS.put(r[0] + "." + SLOTS[i], r[i + 1]);
    }

    /** What each slot plays until the player changes it. */
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    static {
        String[][] d = {                                         // every animation plays from the same bank; the long ones are picked so they end with the scene
            {"default", "satin", "thump", "nectar", "lull", "dusk"},
            {"cool", "satin", "thump", "cascade", "ember", "dusk"},      // "cool" = Rare Drop
            {"epic", "flick", "burst", "halo", "ember", "dusk"},         // "epic" = RNG Drop
            {"nothing", "drip", "thump", "lift", "hum", "sigh"},
            {"dye", "plop", "burst", "cascade", "ripple", "dusk"},
            {"farm", "reap", "rustle", "sprout", "breeze", "settle"},
            {"mytho", "myth_bolt", "myth_shatter", "myth_reveal", "myth_glow", "myth_end"},
            {"exp", "page", "glow", "halo", "chant", "dusk"},
            {"helios", "beam", "flare", "sprout", "breeze", "settle"},
            {"mythnothing", "myth_bolt", "myth_fizzle", NONE, NONE, NONE},
            {"pest", "vacuum", "squeak", "sprout", "breeze", "settle"},
            {"treasure", "myth_dig", "myth_chest", "myth_reveal", "myth_glow", "myth_end"}};
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

    /** Every choice for a slot, "none" first: the mod's own sounds, or Minecraft's while Custom sounds is off. */
    public static List<String> choices() {
        boolean custom = FancyConfig.get().customSounds;
        List<String> l = new ArrayList<>(); l.add(NONE);
        for (String n : ALL.keySet()) if (isMc(n) != custom) l.add(n);
        return l;
    }
    public static String label(String name) { Fx f = byName(name); return f == null ? "None" : f.label; }

    private static Map<String, String> picks() { FancyConfig c = FancyConfig.get(); if (c.mcSounds == null) c.mcSounds = new LinkedHashMap<>(); return c.customSounds ? c.sounds : c.mcSounds; }

    /** The sound the player picked (or the default) for e.g. ("epic", "reveal"), in the current sound set. */
    public static String chosen(String scene, String slot) {
        String key = scene + "." + slot;
        boolean custom = FancyConfig.get().customSounds;
        String v = picks().get(key);
        if (v != null && (v.equals(NONE) || (ALL.containsKey(v) && isMc(v) != custom))) return v;
        return (custom ? DEFAULTS : MC_DEFAULTS).get(key);
    }
    public static void choose(String scene, String slot, String name) { picks().put(scene + "." + slot, name); }

    /** Master volume as a 0..1 factor (0 when sounds are off). */
    public static float master() {
        FancyConfig c = FancyConfig.get();
        if (!c.sound) return 0f;
        return Math.max(0f, Math.min(1f, c.volume / 100f));
    }

    /** Plays the sound chosen for a slot. */
    public static void playSlot(String scene, String slot, float pitch) { play(byName(chosen(scene, slot)), pitch, 1f); }
    /** The sound for a slot; in a big scene a sound with a "_big" twin plays the twin. One place decides it for playing, the sound waves and the timeline. */
    public static String pick(String scene, String slot, boolean big) {
        String n = chosen(scene, slot);
        return big && n != null && ALL.containsKey(n + "_big") && isMc(n) != true ? n + "_big" : n;
    }
    public static void playSlot(String scene, String slot, float pitch, boolean big) { play(byName(pick(scene, slot, big)), pitch, 1f); }

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
        String[] d = ("mc_chime 1.0;mc_bell 1.6;mc_harp 1.0;mc_xylo 0.8;mc_flute 1.0;mc_pling 0.6;mc_bit 0.5;mc_bass 0.8;mc_kick 0.5;mc_snare 0.4;mc_crystal 1.5;mc_shimmer 3.0;mc_swish 0.5;mc_crit 0.5;mc_smack 0.5;mc_orb 0.5;mc_levelup 1.3;mc_fanfare 2.8;mc_totem 3.0;mc_thunder 4.0;mc_bolt 2.0;mc_trident 2.5;mc_beacon 2.5;mc_hum 4.5;mc_powerdown 2.0;mc_power 1.6;mc_conduit 4.0;mc_enchant 1.5;mc_page 0.6;mc_squish 0.5;mc_gift 1.2;mc_fizzle 1.6;mc_warp 0.6;mc_twinkle 1.4;mc_harvest 0.7;mc_anvil 1.0;mc_glass 0.8;mc_nope 0.8").split(";");
        for (String e : d) { String[] p = e.split(" "); LEN.put(p[0], Float.parseFloat(p[1])); }
    }
    /** How long a sound lasts, in seconds (an estimate for the ones that are Minecraft's own). */
    public static float duration(String name) { float[] e = env(name); if (e != null) return e[0]; Float f = name == null ? null : LEN.get(name); return f != null ? f : 0.9f; }

    private static Map<String, float[]> ENV;
    /** The real loudness of each of our own sounds (measured from the audio): [0] = seconds, then 40 values a second, 0..1. */
    private static float[] env(String name) {
        if (name == null) return null;
        if (ENV == null) {
            ENV = new java.util.HashMap<>();
            try (var in = SoundFx.class.getResourceAsStream("/assets/fancydrops/envelopes.json")) {
                if (in != null) for (var en : com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().entrySet()) {
                    var o = en.getValue().getAsJsonObject(); var a = o.getAsJsonArray("e");
                    float[] r = new float[a.size() + 1]; r[0] = o.get("d").getAsFloat();
                    for (int i = 0; i < a.size(); i++) r[i + 1] = a.get(i).getAsInt() / 99f;
                    ENV.put(en.getKey(), r);
                }
            } catch (Exception ignored) {}
        }
        return ENV.get(name);
    }
    /** How loud a sound is u seconds after it starts (0..1). Minecraft's own sounds have no measurement, so they get a plain hit that dies away. */
    public static float level(String name, float u) {
        if (name == null || u < 0f) return 0f;
        float d = duration(name); if (u > d) return 0f;
        float[] e = env(name);
        if (e == null) return (1f - (float) Math.exp(-u / 0.01f)) * (float) Math.exp(-u / Math.max(0.05f, d * 0.3f));
        int n = e.length - 1; float pos = u / d * (n - 1); int i = Math.max(0, Math.min(n - 2, (int) pos)); float f = pos - i;
        float p0 = e[Math.max(1, i)], p1 = e[i + 1], p2 = e[i + 2], p3 = e[Math.min(n, i + 3)];
        float v = p1 + 0.5f * f * (p2 - p0 + f * (2f * p0 - 5f * p1 + 4f * p2 - p3 + f * (3f * (p1 - p2) + p3 - p0)));          // a smooth curve through the measurements, not straight lines
        return Math.max(0f, Math.min(1f, v));
    }

    private static final List<net.minecraft.client.resources.sounds.SoundInstance> PREVIEWS = new ArrayList<>();
    /** Silences every sound started from the menu (so a long one does not keep ringing after you switch or pause). */
    public static void stopPreviews() {
        var sm = Minecraft.getInstance().getSoundManager();
        for (var i : PREVIEWS) sm.stop(i);
        PREVIEWS.clear();
    }

    /** Plays a sound even if sounds are muted (menu / soundtest) - at the chosen volume, minimum 40%. */
    public static void preview(Fx fx) { preview(fx, 1f); }
    public static void preview(Fx fx, float pitch) {
        if (fx == null) return;
        FancyConfig c = FancyConfig.get();
        float vol = Math.max(0.4f, Math.min(1f, c.volume / 100f));
        SoundEvent e = fx.custom != null && c.customSounds ? fx.custom : fx.vanilla;
        var inst = SimpleSoundInstance.forUI(e, pitch, Math.min(1f, vol * fx.baseVolume));
        if (PREVIEWS.size() > 40) PREVIEWS.remove(0);
        PREVIEWS.add(inst);
        Minecraft.getInstance().getSoundManager().play(inst);
    }

    private SoundFx() {}
}
