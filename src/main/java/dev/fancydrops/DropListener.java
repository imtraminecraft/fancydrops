package dev.fancydrops;

import dev.fancydrops.AnimationManager.Anim;
import dev.fancydrops.AnimationManager.Item;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Reads Hypixel chat: a "RARE DROP!" line of a Mythological creature, or a "You dug out ..." burrow line, becomes an animation
 * (and, when the drop has a Chat Alert, a message in party / co-op chat). The line itself is replaced by the animation.
 */
public final class DropListener {
    private static final ArrayDeque<String> OUT = new ArrayDeque<>();
    private static int cooldown = 0;

    // ------------------------------------------------------------------ chat in

    /** Called for every game chat line. Returns true when the line is a drop line that our animation replaces. */
    public static boolean onChat(Component message) {
        if (message == null || !(FancyConfig.get().masterAnimate || FancyConfig.get().masterAnnounce)) return false;
        String raw = ChatFormatting.stripFormatting(message.getString());
        if (raw == null) return false;
        long now = System.currentTimeMillis();
        if (echoHold(message, raw, now)) return true;                      // your own drop message waits until the animation is over
        boolean drop = false;
        for (String line : raw.split("\\R")) {
            if (ChatParser.isPlayerChat(line)) { if (line.toUpperCase(java.util.Locale.ROOT).contains("DROP!")) dlog("ignored (looks like player chat): " + line.trim()); continue; }                   // a player pasting a drop line is just chat
            if (expLine(line, now)) { drop = true; continue; }             // the Superpairs reward lines would show the items early
            if (playTreasureLine(line)) { drop = true; continue; }         // a treasure from a Mythological burrow
            ChatParser.Drop d = ChatParser.parseDrop(line);
            if (d == null) { if (line.toUpperCase(java.util.Locale.ROOT).contains("DROP!")) dlog("not a drop line I can read: " + line.trim()); continue; }
            { FancyConfig fc = FancyConfig.get(); if (fc.dropsSeen < 100000) { fc.dropsSeen++; FancyConfig.save(); } }          // counts toward the cosmetics that unlock by drops
            dlog("heard: " + line.trim() + "  ->  " + d.kind() + " / " + d.name() + " x" + d.amount());
            if (FarmData.isFarm(d) && playFarm(d)) drop = true;             // farming drops have no kill: they play at once
            else if (MythData.isMyth(d) && playMyth(d)) drop = true;
            else if (SlayerData.isSlayer(d) && queueSlayer(d, now)) drop = true;
            else dlog("left in chat (not a listed drop): " + d.name());
        }
        return drop;
    }

    /** Test hook: feed a chat line as if Hypixel had sent it. */
    public static void simulate(String line) { onChat(Component.literal(line)); }

    // ------------------------------------------------------------------ your own drop message

    private static final List<String[]> SENT = new ArrayList<>();            // {text, sentAt}
    private static final List<Component> HELD = new ArrayList<>();
    private static long heldAt;

    /** Hypixel echoes your /pc, /cc or /ac message back to you: it would show the drop before the animation ends, so it is kept back until then. */
    private static boolean echoHold(Component message, String raw, long now) {
        SENT.removeIf(x -> now - Long.parseLong(x[1]) > 20_000);
        if (SENT.isEmpty()) return false;
        for (String[] x : SENT) if (raw.contains(x[0])) {
            SENT.remove(x);
            if (!AnimationManager.isPlaying() && OUT.isEmpty()) return false;
            if (HELD.isEmpty()) heldAt = now;
            HELD.add(message);
            return true;
        }
        return false;
    }

    private static void releaseHeld(Minecraft mc, long now) {
        if (HELD.isEmpty() || mc.player == null) return;
        if ((AnimationManager.isPlaying() || !OUT.isEmpty()) && now - heldAt < 30_000) return;
        List<Component> l = new ArrayList<>(HELD); HELD.clear();
        for (Component c : l) mc.player.sendSystemMessage(c);
    }

    /** A few lines about what was heard and what happened to it: config/fancydrops/drop-log.txt, so a missing animation can be traced. */
    static void dlog(String s) {
        try {
            java.nio.file.Path f = DataFiles.dir().resolve("drop-log.txt");
            if (java.nio.file.Files.exists(f) && java.nio.file.Files.size(f) > 200_000) java.nio.file.Files.delete(f);
            java.nio.file.Files.createDirectories(f.getParent());
            java.nio.file.Files.writeString(f, java.time.LocalTime.now().withNano(0) + "  " + s + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Throwable ignored) { }
    }

    // ------------------------------------------------------------------ Mythological creatures

    /** A mythological creature's rare drop line: the animation plays right away. Returns true when the line is replaced by it. */
    private static boolean playMyth(ChatParser.Drop d) {
        FancyConfig c = FancyConfig.get();
        DropData.Drop dr = MythData.find(d.name());
        DropData.Source src = MythData.sourceOf(d.name());
        if (dr == null || src == null) return false;
        FancyConfig.SourceCfg sc = c.source(src.id); FancyConfig.DropCfg dc = c.drop(src.id, dr.name);
        FancyConfig.initDrop(dc, dr.rank);
        boolean animate = c.masterAnimate && sc.enabled && dc.anim != 3, announce = c.masterAnnounce && c.chatEnabled("mythological") && sc.enabled && dc.announce;
        dlog("mythological: " + dr.name + " from " + src.id + "  animate=" + animate + " announce=" + announce);
        if (!animate && !announce) return false;
        Item it = mythItem(src, dr, d.mf(), d.sim());
        it.amount = Math.max(1, d.amount());
        it.announce = announce;
        lookupPrice(it, announce && !animate);
        if (animate) AnimationManager.play(new Anim("myth", AnimationManager.typeOf(it, dc), it));
        return animate;
    }

    /** The Item an animation shows for a drop of any library. */
    static Item dropItem(DropData.Source src, DropData.Drop dr, String mf, boolean sim) {
        Item it = new Item();
        it.source = src; it.name = dr.name; it.id = dr.id; it.icon = dr.icon; it.odds = dr.odds; it.chance = TreasureData.both(dr.chance); it.mf = mf; it.sim = sim;
        it.untradeable = dr.untradeable || DropData.untradeable(dr.id);
        it.rank = Math.max(0, dr.rank);
        return it;
    }
    private static Item mythItem(DropData.Source src, DropData.Drop dr, String mf, boolean sim) { return dropItem(src, dr, mf, sim); }

    /** A drop that is ready to play: its item, whether it animates, whether it is announced, and the animation type (-1 when it has none). */
    private record Plan(Item it, boolean animate, boolean announce, int type) {}

    /** Reads the settings of a drop (its source, its animation pick, the chat switches) and makes its plan, or null when it neither animates nor announces. */
    private static Plan plan(DropData.Source src, DropData.Drop dr, Item it, String chatCat, String what) {
        FancyConfig c = FancyConfig.get();
        FancyConfig.SourceCfg sc = c.source(src.id); FancyConfig.DropCfg dc = c.drop(src.id, dr.name);
        FancyConfig.initDrop(dc, dr.rank);
        boolean animate = c.masterAnimate && sc.enabled && dc.anim != 3, announce = c.masterAnnounce && c.chatEnabled(chatCat) && sc.enabled && dc.announce;
        dlog(what + ": " + dr.name + " from " + src.id + "  animate=" + animate + " announce=" + announce);
        if (!animate && !announce) return null;
        it.announce = announce;
        return new Plan(it, animate, announce, animate ? AnimationManager.typeOf(it, dc) : -1);
    }

    /** Looks the prices up and plays one or two planned drops as one animation. Returns true when an animation plays (the chat line is then replaced). */
    private static boolean show(String group, Plan a, Plan b, int kind, boolean helios) {
        List<Plan> anim = new ArrayList<>();
        for (Plan p : new Plan[]{a, b}) if (p != null) { lookupPrice(p.it(), p.announce() && !p.animate()); if (p.animate()) anim.add(p); }
        if (anim.isEmpty()) return false;
        boolean rng = false; for (Plan p : anim) if (p.type() == AnimationManager.RNG) rng = true;          // two drops play the bigger of their two animations
        Anim an = new Anim(group, rng ? AnimationManager.RNG : AnimationManager.NORMAL, anim.get(0).it());
        an.kind = kind; an.helios = helios;
        if (anim.size() > 1) an.item2 = anim.get(1).it();
        AnimationManager.play(an);
        return true;
    }

    /** The animation type a menu or command play uses: the drop's own pick, and for an "Off" drop the one its rank would get. */
    private static int previewType(Item it, DropData.Source src, DropData.Drop dr) {
        int ty = AnimationManager.typeOf(it, FancyConfig.get().drop(src.id, dr.name));
        return ty >= 0 ? ty : it.rank >= 2 ? AnimationManager.RNG : AnimationManager.NORMAL;
    }

    // ------------------------------------------------------------------ Slayer bosses

    private static ChatParser.Drop pendSlayer; private static long pendAt;

    /** A slayer drop waits a moment for its twin: two drops of one kill play as one animation. True when the line is taken (it plays now or in a moment). */
    private static boolean queueSlayer(ChatParser.Drop d, long now) {
        if (pendSlayer != null) { ChatParser.Drop first = pendSlayer; pendSlayer = null; return playSlayer(first, d); }
        FancyConfig c = FancyConfig.get();
        DropData.Drop dr = SlayerData.find(d.name()); DropData.Source src = SlayerData.sourceOf(d.name());
        if (dr == null || src == null) return false;
        FancyConfig.SourceCfg sc = c.source(src.id); FancyConfig.DropCfg dc = c.drop(src.id, dr.name); FancyConfig.initDrop(dc, dr.rank);
        boolean wanted = c.masterAnimate && sc.enabled && dc.anim != 3 || c.masterAnnounce && c.chatEnabled("bosses") && sc.enabled && dc.announce;
        if (!wanted) { dlog("slayer: " + dr.name + " left in chat (animation and alert are off)"); return false; }
        pendSlayer = d; pendAt = now;
        return true;
    }
    /** Plays a slayer drop that waited and found no twin. */
    private static void flushSlayer(long now) {
        if (pendSlayer == null || now - pendAt < 450) return;
        ChatParser.Drop d = pendSlayer; pendSlayer = null;
        playSlayer(d, null);
    }

    /** One or two drops of one kill: a single animation (the boss decides the scene), the item or both springing out of it. */
    private static boolean playSlayer(ChatParser.Drop d, ChatParser.Drop d2) {
        Plan a = slayerPlan(d), b = d2 == null ? null : slayerPlan(d2);
        DropData.Source src = SlayerData.sourceOf(d.name());
        if (a == null && b != null) { a = b; b = null; src = SlayerData.sourceOf(d2.name()); }          // the first one has no animation or alert, the second does
        if (a == null || src == null) return false;
        return show("slayer", a, b, SlayerData.theme(src.id), false);
    }
    private static Plan slayerPlan(ChatParser.Drop d) {
        DropData.Drop dr = SlayerData.find(d.name()); DropData.Source src = SlayerData.sourceOf(d.name());
        if (dr == null || src == null) return null;
        Item it = dropItem(src, dr, d.mf(), d.sim());
        it.chance = dr.chance; it.amount = Math.max(1, d.amount());
        return plan(src, dr, it, "bosses", "slayer");
    }

    static void launchSlayer(DropData.Source src, DropData.Drop dr, DropData.Source src2, DropData.Drop dr2, String mf, int type) {
        Item it = dropItem(src, dr, mf, true); it.chance = dr.chance; previewPrice(it);
        Item it2 = null;
        if (dr2 != null) { it2 = dropItem(src2, dr2, mf, true); it2.chance = dr2.chance; previewPrice(it2); }
        int ty = type >= 0 ? type : Math.max(previewType(it, src, dr), it2 == null ? 0 : previewType(it2, src2, dr2));
        Anim a = new Anim("slayer", ty, it); a.kind = SlayerData.theme(src.id); a.item2 = it2;
        AnimationManager.play(a);
    }
    /** Menu "play" button of a slayer drop (the Drop Library). */
    public static void playSlayerDrop(DropData.Source src, DropData.Drop dr) { launchSlayer(src, dr, null, null, "+250%", -1); }

    /** /fd simulate Slayer [Boss] [Drop | Nothing] [Drop 2] [mf]: a pretend slayer drop (or two), from a named boss or any. */
    static void simulateSlayer(int mf, String name, Consumer<String> reply) {
        DropData.Source from = null;
        for (DropData.Source s : SlayerData.ALL) {
            int n = s.boss.length();
            if (name.regionMatches(true, 0, s.boss, 0, n) && (name.length() == n || name.charAt(n) == ' ')) { from = s; name = name.substring(n).trim(); break; }
        }
        if (name.equalsIgnoreCase("nothing")) {
            reply.accept("Simulating a slayer that dropped nothing");
            Anim a = new Anim("slayer", AnimationManager.NO_DROPS, null); a.kind = from != null ? SlayerData.theme(from.id) : new Random().nextInt(6); AnimationManager.play(a); return;
        }
        List<DropData.Drop> pool = new ArrayList<>();
        if (from != null) pool.addAll(from.drops); else for (DropData.Source s : SlayerData.ALL) pool.addAll(s.drops);
        if (pool.isEmpty()) { reply.accept("No slayer drops loaded."); return; }
        List<DropData.Drop> picks = name.isBlank() ? List.of(pool.get(new Random().nextInt(pool.size()))) : pickDrops(pool, name, 2, from == null ? "a slayer drop" : "a " + from.boss + " drop", reply);
        if (picks == null || picks.isEmpty()) return;
        DropData.Source src = from != null ? from : SlayerData.sourceOf(picks.get(0).name);
        if (src == null) return;
        DropData.Drop second = picks.size() > 1 ? picks.get(1) : null;
        DropData.Source src2 = second == null ? null : from != null ? from : SlayerData.sourceOf(second.name);
        reply.accept("Simulating " + picks.get(0).name + (second == null ? "" : " + " + second.name) + " from " + src.boss + " at " + mf + " Magic Find");
        launchSlayer(src, picks.get(0), src2, second, "+" + mf + "%", -1);
    }

    // ------------------------------------------------------------------ Farming

    /** A "RARE CROP!" line, or a "RARE DROP!" line of a pest: the field scene plays right away. Returns true when the line is replaced by it. */
    private static boolean playFarm(ChatParser.Drop d) {
        DropData.Drop dr = FarmData.find(d.name()); DropData.Source src = FarmData.sourceOf(d.name());
        if (dr == null || src == null) return false;                       // not in the Farming library: Hypixel's own line stays
        Item it = farmItem(src, dr, d.mf() != null && d.mf().startsWith("OB:") ? d.mf() : null, d.sim(), FarmData.rarityOf(d.name()));        // farming has Overbloom, not Magic Find
        it.amount = Math.max(1, d.amount());
        Plan pl = plan(src, dr, it, "farming", "farming");
        return pl != null && show(src.id, pl, null, 0, dr.id.equals("RAY_OF_HELIOS"));
    }

    /** A farming drop's item: a pet carries its rarity and what George pays for it. */
    private static Item farmItem(DropData.Source src, DropData.Drop dr, String mf, boolean sim, String rarity) {
        Item it = dropItem(src, dr, mf, sim);
        it.chance = dr.chance;
        if (dr.george != null && !dr.george.isEmpty()) {
            String r = rarity.isEmpty() ? dr.rarity.toUpperCase(Locale.ROOT) : rarity;
            Long gp = dr.george.get(r);
            if (gp == null) { r = dr.george.keySet().iterator().next(); gp = dr.george.get(r); }
            it.extra = r.charAt(0) + r.substring(1).toLowerCase(Locale.ROOT) + " Pet  \u00B7  George pays " + PriceService.fmt(gp) + " coins";
            it.extraCol = switch (r) { case "COMMON" -> 0xFFFFFFFF; case "UNCOMMON" -> 0xFF55FF55; case "RARE" -> 0xFF5555FF; case "EPIC" -> 0xFFAA00AA; case "LEGENDARY" -> 0xFFFFAA00; default -> 0xFFFF55FF; };
        }
        return it;
    }

    static void launchFarm(DropData.Source src, DropData.Drop dr, int ob, int type) {
        Item it = farmItem(src, dr, "OB:+" + ob, true, ""); previewPrice(it);
        Anim a = new Anim(src.id, type >= 0 ? type : previewType(it, src, dr), it);
        a.helios = dr.id.equals("RAY_OF_HELIOS");
        AnimationManager.play(a);
    }

    /** /fd simulate Farming [Rare Crops|Harvest Feast|Pest Drops] [Drop] [overbloom]: a pretend farming drop (a random one when none is named). */
    static void simulateFarm(int ob, String name, Consumer<String> reply) {
        DropData.Source from = null;
        for (DropData.Source s : FarmData.ALL) {
            int n = s.boss.length();
            if (name.regionMatches(true, 0, s.boss, 0, n) && (name.length() == n || name.charAt(n) == ' ')) { from = s; name = name.substring(n).trim(); break; }
        }
        List<DropData.Drop> pool = new ArrayList<>();
        if (from != null) pool.addAll(from.drops); else for (DropData.Source s : FarmData.ALL) pool.addAll(s.drops);
        if (pool.isEmpty()) { reply.accept("No farming drops loaded."); return; }
        List<DropData.Drop> picks = name.isBlank() ? List.of(pool.get(new Random().nextInt(pool.size()))) : pickDrops(pool, name, 1, from == null ? "a farming drop" : "a " + from.boss + " drop", reply);
        if (picks == null || picks.isEmpty()) return;
        DropData.Source src = from != null ? from : FarmData.sourceOf(picks.get(0).name);
        if (src == null) return;
        reply.accept("Simulating " + picks.get(0).name + " from " + src.boss + " at " + ob + " Overbloom");
        launchFarm(src, picks.get(0), ob, -1);
    }

    // ------------------------------------------------------------------ Experimentation Table (Superpairs) rewards

    private static final List<String> EXP_NAMES = new ArrayList<>();
    private static long expUntil, expFlushAt = Long.MAX_VALUE;

    /** "You claimed the Superpairs rewards!" is followed by one line per reward ("+400k Enchanting Exp", "Power VII", ...): the books and items among them are the drops. */
    private static boolean expLine(String line, long now) {
        String t = line.trim();
        if (t.startsWith("You claimed the") && t.endsWith("rewards!")) { EXP_NAMES.clear(); expUntil = now + 3000; expFlushAt = Long.MAX_VALUE; return true; }
        if (now > expUntil || t.isEmpty()) return false;
        if (t.startsWith("+") || t.length() < 40 && !t.contains(">")) expUntil = Math.max(expUntil, now + 1200);          // every reward line, listed or not, stays out of chat
        String n = t.replaceFirst("^\\+\\s*", "").replaceFirst("^\\d+\\s*[x\u00D7]\\s*", "").replaceFirst("\\s*[x\u00D7]\\s*\\d+$", "").trim();
        DropData.Drop dr = ExperimentData.find(n);
        if (dr != null) { EXP_NAMES.add(dr.name); expUntil = now + 1500; expFlushAt = now + 700; return true; }
        return t.startsWith("+") || t.matches("[A-Za-z' ]+( [IVX]+)?( x\\d+)?");      // "+400k Enchanting Exp", "Power VII": hidden too
    }

    /** Once the reward lines stop coming, plays them: two items share one book scene, more play one scene after another. */
    private static void flushExp(long now) {
        if (EXP_NAMES.isEmpty() || now < expFlushAt) return;
        List<String> names = new ArrayList<>(EXP_NAMES); EXP_NAMES.clear(); expFlushAt = Long.MAX_VALUE;
        if (!(FancyConfig.get().masterAnimate || FancyConfig.get().masterAnnounce)) return;
        DropData.Source s = ExperimentData.SOURCE;
        List<Plan> plans = new ArrayList<>();
        for (String nm : names) {
            DropData.Drop dr = ExperimentData.find(nm); if (dr == null) continue;
            Plan pl = plan(s, dr, expItem(dr, false), "experiments", "experiment");
            if (pl != null) plans.add(pl);
        }
        for (int i = 0; i < plans.size(); i += 2) show("exp", plans.get(i), i + 1 < plans.size() ? plans.get(i + 1) : null, 0, false);
    }

    private static Item expItem(DropData.Drop dr, boolean preview) {
        Item it = dropItem(ExperimentData.SOURCE, dr, null, preview);       // the Table has no Magic Find or Overbloom
        it.chance = dr.chance;
        if (preview) previewPrice(it);
        return it;
    }

    /** Menu "play" button of a Table item. */
    public static void playExpDrop(DropData.Drop dr) { launchExp(dr, null, -1); }
    static void launchExp(DropData.Drop a, DropData.Drop b, int type) {
        Item it = expItem(a, true), it2 = b == null ? null : expItem(b, true);
        DropData.Source s = ExperimentData.SOURCE;
        int ty = type >= 0 ? type : Math.max(previewType(it, s, a), it2 == null ? 0 : previewType(it2, s, b));
        Anim an = new Anim("exp", ty, it); an.item2 = it2;
        AnimationManager.play(an);
    }

    /** /fd simulate Experiments [Drop 1] [Drop 2]: a pretend Table reward with one or two items (random when none is named). */
    static void simulateExp(String name, Consumer<String> reply) {
        List<DropData.Drop> pool = ExperimentData.SOURCE.drops;
        if (pool.isEmpty()) { reply.accept("No Experimentation Table drops loaded."); return; }
        List<DropData.Drop> picks = name.isBlank() ? List.of(pool.get(new Random().nextInt(pool.size()))) : pickDrops(pool, name, 2, "an Experimentation Table item", reply);
        if (picks == null || picks.isEmpty()) return;
        reply.accept("Simulating " + String.join(" + ", picks.stream().map(d -> d.name).toList()) + " from the Experimentation Table");
        launchExp(picks.get(0), picks.size() > 1 ? picks.get(1) : null, -1);
    }

    // ------------------------------------------------------------------ Mythological treasure (a burrow that holds loot instead of a creature)

    private static final java.util.regex.Pattern TREASURE = java.util.regex.Pattern.compile("You dug out\\s+(?:an?\\s+|some\\s+)?(.+?)\\s*[!.]*\\s*$", java.util.regex.Pattern.CASE_INSENSITIVE);

    private static Item treasureItem(DropData.Drop dr) {
        Item it = new Item();
        it.source = TreasureData.SOURCE; it.name = dr.name; it.id = dr.id; it.icon = dr.icon; it.chance = dr.chance; it.sim = false; it.rank = Math.max(0, dr.rank); it.stateKey = dr.name;
        long coins = TreasureData.coins(dr.name);
        if (coins > 0) { it.price = coins; it.priceSource = ""; }                             // coins: the coins are the price, always the exact number
        return it;
    }

    /** A "You dug out ..." line that names a treasure. True when it was one and the animation is playing (the chat line is then held back). */
    private static boolean playTreasureLine(String line) {
        java.util.regex.Matcher m = TREASURE.matcher(line.trim());
        if (!m.find()) return false;
        FancyConfig c = FancyConfig.get();
        DropData.Drop dr = TreasureData.find(m.group(1).trim(), c.mythSpade);
        if (dr == null) return false;
        String cfgName = dr.name; long cn = TreasureData.coins(dr.name);
        if (cn > 0) { boolean listed = false; for (DropData.Drop x : TreasureData.drops(c.mythSpade)) if (x.name.equalsIgnoreCase(dr.name)) listed = true; if (!listed) { String nn = TreasureData.nearestCoins(cn, c.mythSpade); if (nn != null) cfgName = nn; } }
        FancyConfig.SourceCfg sc = c.source(TreasureData.SOURCE.id); FancyConfig.DropCfg dc = c.drop(TreasureData.SOURCE.id, cfgName);
        FancyConfig.initDrop(dc, dr.rank);
        boolean animate = c.masterAnimate && sc.enabled && dc.anim != 3, announce = c.masterAnnounce && c.chatEnabled("mythological") && sc.enabled && dc.announce;
        dlog("treasure: " + dr.name + "  animate=" + animate + " announce=" + announce);
        if (!animate && !announce) return false;
        Item it = treasureItem(dr);
        it.stateKey = cfgName;
        it.announce = announce;
        lookupPrice(it, announce && !animate);
        if (animate) AnimationManager.play(new Anim("treasure", AnimationManager.typeOf(it, dc), it));
        return animate;
    }

    // ------------------------------------------------------------------ prices and chat alerts

    /** Looks the price up (a cached one is instant), and sends the alert once it is known when announceNow is set. An animated drop sends its own alert when its animation starts. */
    private static void lookupPrice(Item it, boolean announceNow) {
        ItemIcons.prefetch(it.id);
        if (it.untradeable) it.price = -1;
        else if (TreasureData.coins(it.name) == 0) {
            double quick = PriceService.lookup(it.id, it.name, false);
            if (quick >= 0) { it.price = quick; it.priceSource = PriceService.source(it.id, it.name); }
        }
        if (TreasureData.coins(it.name) > 0 && !it.untradeable) { if (announceNow) queueAnnounce(it, it.price); return; }
        CompletableFuture.supplyAsync(() -> it.untradeable ? -1.0 : PriceService.lookup(it.id, it.name, true)).exceptionally(ex -> -1.0).thenAccept(price ->
            Minecraft.getInstance().execute(() -> {
                it.price = price;
                if (price >= 0) it.priceSource = PriceService.source(it.id, it.name);
                if (announceNow) queueAnnounce(it, price < 0 ? price : price * Math.max(1, it.amount));
            }));
    }

    /** The animation just started: the chat alert goes out with it. */
    public static void announceNow(Item it) {
        if (it == null || !it.announce) return;
        it.announce = false;
        queueAnnounce(it, it.price < 0 ? it.price : it.price * Math.max(1, it.amount));
    }

    /** The slash command (without the slash) for the chosen channel. */
    static String chan(FancyConfig c) { return c.channel.equals("COOP") ? "cc " : c.channel.equals("ALL") ? "ac " : "pc "; }

    private static void queueAnnounce(Item it, double price) {
        FancyConfig c = FancyConfig.get();
        if (c.channel.equals("OFF") || !onHypixel()) return;               // never talks outside Hypixel
        String m = message(it, price, it.mf != null ? it.mf : "0%", null);
        OUT.add(chan(c) + (c.channel.equals("ALL") ? adv(m) : m));
        if (c.alsoAll && !c.channel.equals("ALL") && it.rank >= 2) OUT.add("ac " + adv(m));      // RNG drops also go to public chat, below the other one
    }

    static final String ADV = "[FancyDrops] | ";
    /** Public chat carries the mod's name in front, cut to Hypixel's 256 characters. */
    static String adv(String m) { String t = ADV + m; return t.length() > 250 ? t.substring(0, 250) : t; }

    /** The category (Categories.Cat id) a drop's chat message follows. */
    static String catOf(Item it) {
        if (it.source == null) return "mythological";
        if (SlayerData.byId(it.source.id) == it.source) return "bosses";
        if (FarmData.byId(it.source.id) == it.source) return "farming";
        if (it.source == ExperimentData.SOURCE) return "experiments";
        return "mythological";
    }

    static String message(Item it, double price, String mf, String fmt) {
        FancyConfig c = FancyConfig.get();
        String priceText = it.untradeable ? "untradeable" : price >= 0 ? PriceService.fmt(price) + " coins" : "price unknown";
        String format = fmt != null ? fmt : c.formatFor(catOf(it));
        if (!ChatFilter.ok(format)) format = FancyConfig.DEFAULT_FORMAT;
        String from = it.source == null ? "a drop" : it.source.boss;
        String msg = format.replace("{item}", it.title()).replace("{price}", priceText)
                .replace("{mf}", TextFx.mfText(mf)).replace("{source}", from)
                .replaceAll("\\s{2,}", " ").trim();
        return msg.length() > 250 ? msg.substring(0, 250) : msg;
    }

    /** True when connected to Hypixel. Chat alerts (and their tests) only happen there. */
    public static boolean onHypixel() {
        var server = Minecraft.getInstance().getCurrentServer();
        return server != null && server.ip != null && server.ip.toLowerCase(java.util.Locale.ROOT).contains("hypixel");
    }

    public static final String NOT_ON_HYPIXEL = "The chat message can't be tested because you're not on Hypixel.";

    /** What the message would look like for a sample drop (the Chat page shows this live under the format box). */
    public static String sample(String format, String cat) {
        Item it = new Item();
        if ("bosses".equals(cat) && !SlayerData.ALL.isEmpty()) {
            DropData.Source b = SlayerData.ALL.get(0); it.source = b; it.name = b.drops.isEmpty() ? "Scythe Blade" : b.drops.get(b.drops.size() - 1).name;
            return message(it, 21_000_000, "+250%", format);
        }
        if ("farming".equals(cat) && !FarmData.ALL.isEmpty()) {
            DropData.Source f = FarmData.ALL.get(0); it.source = f; it.name = f.drops.isEmpty() ? "Cropie" : f.drops.get(f.drops.size() - 1).name;
            return message(it, 4_500_000, "OB:+25", format);
        }
        if ("experiments".equals(cat) && !ExperimentData.SOURCE.drops.isEmpty()) {
            it.source = ExperimentData.SOURCE; it.name = ExperimentData.SOURCE.drops.get(0).name;
            return message(it, 8_500_000, null, format);
        }
        it.name = "Daedalus Stick"; it.source = MythData.ALL.isEmpty() ? null : MythData.ALL.get(0);
        return message(it, 21_000_000, "+250%", format);
    }

    /** The Chat page's Test button: sends the message to your party / co-op chat, but only on Hypixel. */
    public static void testChat(Consumer<String> reply, String cat) {
        FancyConfig c = FancyConfig.get();
        if (!onHypixel()) { reply.accept(NOT_ON_HYPIXEL); return; }
        if (c.channel.equals("OFF")) { reply.accept("Chat alerts are off - pick Party or Co-op first."); return; }
        String cmd = chan(c);
        String msg = sample(c.formatFor(cat), cat);
        OUT.add(cmd + msg);
        reply.accept("Sent to /" + cmd.trim() + ": " + msg);
    }

    // ------------------------------------------------------------------ menu plays and /fd simulate

    private static void previewPrice(Item it) {
        ItemIcons.prefetch(it.id);
        if (it.untradeable) { it.price = -1; return; }
        if (TreasureData.coins(it.name) > 0) return;
        double quick = PriceService.lookup(it.id, it.name, false);
        if (quick >= 0) { it.price = quick; it.priceSource = PriceService.source(it.id, it.name); }
        else CompletableFuture.supplyAsync(() -> PriceService.lookup(it.id, it.name, true)).thenAccept(p -> {
            it.price = p; if (p >= 0) it.priceSource = PriceService.source(it.id, it.name);
        });
    }

    /** The one way a drop reaches the screen from the menu and the commands. type -1 = the one its own Drop Library pick says. */
    static void launchMyth(DropData.Source src, DropData.Drop dr, String mf, int type) {
        Item it = mythItem(src, dr, mf, true); previewPrice(it);
        AnimationManager.play(new Anim("myth", type >= 0 ? type : previewType(it, src, dr), it));
    }
    static void launchTreasure(DropData.Drop dr, int type) {
        Item it = treasureItem(dr); it.sim = true; previewPrice(it);
        AnimationManager.play(new Anim("treasure", type >= 0 ? type : previewType(it, TreasureData.SOURCE, dr), it));
    }

    /** Menu "play" button of any drop (the Drop Library): the scene of the library it belongs to. */
    public static void playDrop(DropData.Source s, DropData.Drop d) {
        if (s == TreasureData.SOURCE) playTreasureDrop(d);
        else if (SlayerData.byId(s.id) == s) playSlayerDrop(s, d);
        else if (FarmData.byId(s.id) == s) launchFarm(s, d, 25, -1);
        else if (s == ExperimentData.SOURCE) playExpDrop(d);
        else playMythDrop(s, d);
    }
    /** Menu "play" button of a Mythological drop (the Drop Library). */
    public static void playMythDrop(DropData.Source src, DropData.Drop dr) { launchMyth(src, dr, "+250%", -1); }
    /** Menu "play" button of a treasure drop. */
    public static void playTreasureDrop(DropData.Drop dr) { launchTreasure(dr, -1); }

    /** The Animation Editor's Play button: a sample drop of this group in the given type (No Drops, Normal or RNG). two = a pair of drops (Slayers and the Table). */
    public static void playSample(String group, int type, boolean two) {
        switch (group) {
            case "treasure" -> {
                DropData.Drop dr = null;
                for (DropData.Drop d : TreasureData.drops(FancyConfig.get().mythSpade)) if (d.name.equals(type == AnimationManager.RNG ? "Myth the Fish" : "Griffin Feather")) dr = d;
                if (dr != null) launchTreasure(dr, type);
            }
            case "slayer" -> {
                if (SlayerData.ALL.isEmpty()) return;
                DropData.Source s = SlayerData.ALL.get(0);
                if (s.drops.isEmpty()) return;
                DropData.Drop dr = s.drops.get(s.drops.size() - 1), dr2 = two && s.drops.size() > 1 ? s.drops.get(s.drops.size() - 2) : null;      // the boss's rarest drops
                launchSlayer(s, dr, s, dr2, "+250%", type);
            }
            case "crops", "feast", "bounty", "pests" -> {
                DropData.Source s = FarmData.byId(group);
                if (s == null || s.drops.isEmpty()) return;
                DropData.Drop dr = type == AnimationManager.RNG ? s.drops.get(s.drops.size() - 1) : s.drops.get(s.drops.size() / 3);
                launchFarm(s, dr, 25, type);
            }
            case "exp" -> {
                List<DropData.Drop> l = ExperimentData.SOURCE.drops;
                if (l.isEmpty()) return;
                DropData.Drop dr = l.get(type == AnimationManager.RNG ? l.size() - 1 : l.size() / 3), dr2 = two ? l.get(Math.max(0, l.size() / 3 - 2)) : null;
                launchExp(dr, dr2, type);
            }
            default -> {
                if (MythData.ALL.isEmpty()) return;
                DropData.Source s = MythData.ALL.get(Math.min(MythData.ALL.size() - 1, 6));
                if (s.drops.isEmpty()) return;
                DropData.Drop dr = s.drops.get(0);
                for (DropData.Drop x : s.drops) if (x.pct != null) { dr = x; break; }          // the creature's signature drop
                launchMyth(s, dr, "+250%", type);
            }
        }
    }

    private static List<DropData.Drop> pickDrops(List<DropData.Drop> pool, String text, int max, String what, Consumer<String> reply) {
        List<DropData.Drop> out = new ArrayList<>();
        String rest = text.replace(",", " ").trim();
        while (!rest.isBlank()) {
            DropData.Drop best = null;
            for (DropData.Drop d : pool) {
                int n = d.name.length();
                if (rest.regionMatches(true, 0, d.name, 0, n) && (rest.length() == n || Character.isWhitespace(rest.charAt(n))) && (best == null || n > best.name.length())) best = d;
            }
            if (best == null) { reply.accept("\"" + rest + "\" isn't " + what + ". Press Tab to see them."); return null; }
            out.add(best); rest = rest.substring(best.name.length()).trim();
        }
        if (out.size() > max) { reply.accept("At most " + max + " drop" + (max == 1 ? "" : "s") + " here."); return null; }
        return out;
    }

    /** /fd simulate Mythological [Creature] [Drop | Nothing] [mf]: a pretend Mythological drop, from a named creature or any. */
    static void simulateMyth(int mf, String name, Consumer<String> reply) {
        DropData.Source from = null;
        for (DropData.Source s : MythData.ALL) {
            int n = s.boss.length();
            if (name.regionMatches(true, 0, s.boss, 0, n) && (name.length() == n || name.charAt(n) == ' ')) { from = s; name = name.substring(n).trim(); break; }
        }
        if (name.equalsIgnoreCase("nothing")) { reply.accept("Simulating a ritual that dropped nothing"); AnimationManager.play(new Anim("myth", AnimationManager.NO_DROPS, null)); return; }
        List<DropData.Drop> pool = new ArrayList<>();
        if (from != null) pool.addAll(from.drops); else for (DropData.Source s : MythData.ALL) pool.addAll(s.drops);
        if (pool.isEmpty()) { reply.accept("No mythological drops loaded."); return; }
        List<DropData.Drop> picks = name.isBlank() ? List.of(pool.get(new Random().nextInt(pool.size()))) : pickDrops(pool, name, 1, from == null ? "a mythological drop" : "a " + from.boss + " drop", reply);
        if (picks == null || picks.isEmpty()) return;
        DropData.Source src = from != null ? from : MythData.sourceOf(picks.get(0).name);
        if (src == null) return;
        reply.accept("Simulating " + picks.get(0).name + " from " + src.boss + " at " + mf + " Magic Find");
        launchMyth(src, picks.get(0), "+" + mf + "%", -1);
    }

    /** /fd simulate Treasure [Drop]: a pretend Griffin burrow treasure (a random one when no name is given). */
    static void simulateTreasure(String name, int mf, Consumer<String> reply) {
        List<DropData.Drop> pool = new ArrayList<>(); java.util.Set<String> seen = new java.util.HashSet<>();
        for (int sp = 0; sp < 3; sp++) for (DropData.Drop d : TreasureData.drops(sp)) if (seen.add(d.name)) pool.add(d);
        List<DropData.Drop> picks = name.isBlank() ? List.of(pool.get(new Random().nextInt(pool.size()))) : pickDrops(pool, name, 1, "a treasure drop", reply);
        if (picks == null || picks.isEmpty()) return;
        reply.accept("Simulating " + picks.get(0).name + " from a burrow at " + mf + " Magic Find");
        launchTreasure(picks.get(0), -1);
    }


    // ------------------------------------------------------------------ tick

    /** Called every client tick: sends at most one chat message per 1.5 s. */
    public static void tick(Minecraft mc) {
        long now = System.currentTimeMillis();
        releaseHeld(mc, now);
        flushSlayer(now); flushExp(now);
        if (cooldown > 0) { cooldown--; return; }
        if (mc.player == null || OUT.isEmpty()) return;
        String out = OUT.poll();
        int sp = out.indexOf(' ');
        if (sp > 0) SENT.add(new String[]{out.substring(sp + 1).trim(), String.valueOf(System.currentTimeMillis())});
        mc.player.connection.sendCommand(out);
        cooldown = 30;
    }

    private DropListener() {}
}
