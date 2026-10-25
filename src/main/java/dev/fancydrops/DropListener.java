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
            if (ChatParser.isPlayerChat(line)) continue;                   // a player pasting a drop line is just chat
            if (playTreasureLine(line)) { drop = true; continue; }         // a treasure from a Mythological burrow
            ChatParser.Drop d = ChatParser.parseDrop(line);
            if (d == null) continue;
            { FancyConfig fc = FancyConfig.get(); if (fc.dropsSeen < 100000) { fc.dropsSeen++; FancyConfig.save(); } }          // counts toward the cosmetics that unlock by drops
            dlog("heard: " + line.trim() + "  ->  " + d.kind() + " / " + d.name() + " x" + d.amount());
            if (MythData.isMyth(d) && playMyth(d)) drop = true;
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

    private static Item mythItem(DropData.Source src, DropData.Drop dr, String mf, boolean sim) {
        Item it = new Item();
        it.source = src; it.name = dr.name; it.id = dr.id; it.icon = dr.icon; it.odds = dr.odds; it.chance = TreasureData.both(dr.chance); it.mf = mf; it.sim = sim;
        it.untradeable = dr.untradeable || DropData.untradeable(dr.id);
        it.rank = Math.max(0, dr.rank);
        return it;
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
    static String catOf(Item it) { return "mythological"; }

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
        AnimationManager.play(new Anim("myth", type >= 0 ? type : AnimationManager.typeOf(it, FancyConfig.get().drop(src.id, dr.name)), it));
    }
    static void launchTreasure(DropData.Drop dr, int type) {
        Item it = treasureItem(dr); it.sim = true; previewPrice(it);
        AnimationManager.play(new Anim("treasure", type >= 0 ? type : AnimationManager.typeOf(it, FancyConfig.get().drop(TreasureData.SOURCE.id, dr.name)), it));
    }

    /** Menu "play" button of a Mythological drop (the Drop Library). */
    public static void playMythDrop(DropData.Source src, DropData.Drop dr) { launchMyth(src, dr, "+250%", -1); }
    /** Menu "play" button of a treasure drop. */
    public static void playTreasureDrop(DropData.Drop dr) { launchTreasure(dr, -1); }

    /** The Animation Editor's Play button: a sample drop of this group in the given type (No Drops, Normal or RNG). */
    public static void playSample(String group, int type) {
        if (group.equals("treasure")) {
            DropData.Drop dr = null;
            for (DropData.Drop d : TreasureData.drops(FancyConfig.get().mythSpade)) if (d.name.equals(type == AnimationManager.RNG ? "Myth the Fish" : "Griffin Feather")) dr = d;
            if (dr == null) return;
            launchTreasure(dr, type);
        } else {
            if (MythData.ALL.isEmpty()) return;
            DropData.Source s = MythData.ALL.get(Math.min(MythData.ALL.size() - 1, 6));
            if (s.drops.isEmpty()) return;
            DropData.Drop dr = s.drops.get(0);
            for (DropData.Drop x : s.drops) if (x.pct != null) { dr = x; break; }          // the creature's signature drop
            launchMyth(s, dr, "+250%", type);
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

    /** Every drop name the user could test. */
    public static List<String> allDropNames() {
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (DropData.Source s : MythData.ALL) for (DropData.Drop d : s.drops) names.add(d.name);
        for (int sp = 0; sp < 3; sp++) for (DropData.Drop d : TreasureData.drops(sp)) names.add(d.name);
        return new ArrayList<>(names);
    }

    // ------------------------------------------------------------------ tick

    /** Called every client tick: sends at most one chat message per 1.5 s. */
    public static void tick(Minecraft mc) {
        releaseHeld(mc, System.currentTimeMillis());
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
