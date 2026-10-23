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
 * Reads Hypixel chat: "Your Slayer Kill gave you ..." marks a kill, "RARE DROP! ..." lines are its drops (KillTracker pairs them).
 * Each kill plays one scene: the drop (tier by rarity / value) or, when nothing you ticked dropped, the "nothing" scene.
 * Drops are also logged and announced in party / co-op chat.
 */
public final class DropListener {
    public record Rec(String item, String slayer, double price, long time) {}
    public static final List<Rec> LEDGER = new ArrayList<>();

    private static final ArrayDeque<String> OUT = new ArrayDeque<>();
    private static int cooldown = 0;
    private static long cocoonAt = 0;
    private static final java.util.regex.Pattern COCOON = java.util.regex.Pattern.compile("cocooned your slayer boss", java.util.regex.Pattern.CASE_INSENSITIVE);
    /** True once after the boss was cocooned: that scene plays faster. */
    static boolean takeCocoon() { boolean b = System.currentTimeMillis() - cocoonAt < 90_000; cocoonAt = 0; return b; }
    private static SlayerData.Slayer lastSlayer;
    private static final KillTracker<ChatParser.Drop> TRACKER = new KillTracker<>(DropListener::onSession);

    // ------------------------------------------------------------------ chat in

    /** Called for every game chat line. Returns true when the line is a rare-drop line that our animation replaces. */
    public static boolean onChat(Component message) {
        if (message == null || !(FancyConfig.get().masterAnimate || FancyConfig.get().masterAnnounce)) return false;
        String raw = ChatFormatting.stripFormatting(message.getString());
        if (raw == null) return false;
        long now = System.currentTimeMillis();
        boolean drop = false;
        for (String line : raw.split("\\R")) {
            ProfitTracker.noteLine(line);
            noteBoss(line);
            if (COCOON.matcher(line).find()) { cocoonAt = now; dlog("boss cocooned"); }
            if (expLine(line, now)) continue;
            if (ChatParser.isKill(line) || ChatParser.isQuestComplete(line)) {
                dlog("kill line: " + line.trim());
                ProfitTracker.activity(now, ChatParser.isKill(line));
                TRACKER.kill(now);
            } else {
                ChatParser.Drop d = ChatParser.parseDrop(line);
                if (d != null) {
                    dlog("heard: " + line.trim() + "  ->  " + d.kind() + " / " + d.name() + " x" + d.amount());
                    if (FarmData.isFarm(d)) { if (playFarm(d)) drop = true; }          // farming drops have no kill: they play at once
                    else if (MythData.isMyth(d)) { if (playMyth(d)) drop = true; }     // so do mythological creature drops
                    else { TRACKER.drop(d, now); if (find(d.name()) != null || isCustom(d.name())) drop = true; else dlog("left in chat (unlisted): " + d.name()); }      // only a drop the mod knows is replaced by its scene
                }
            }
        }
        return drop;
    }

    // ------------------------------------------------------------------ Experimentation Table (Superpairs) rewards

    private static final List<String> EXP_NAMES = new ArrayList<>();
    private static long expUntil, expFlushAt;

    /** "You claimed the Superpairs rewards!" is followed by one line per reward ("+400k Enchanting Exp", "Power VII", ...): the books and items among them are the drops. */
    private static boolean expLine(String line, long now) {
        String t = line.trim();
        if (t.startsWith("You claimed the") && t.endsWith("rewards!")) { EXP_NAMES.clear(); expUntil = now + 3000; expFlushAt = Long.MAX_VALUE; return true; }
        if (now > expUntil || t.isEmpty()) return false;
        String n = t.replaceFirst("^\\+\\s*", "").replaceFirst("^\\d+\\s*[x×]\\s*", "").replaceFirst("\\s*[x×]\\s*\\d+$", "").trim();
        for (String[] e : ExperimentData.ALL) if (e[0].equalsIgnoreCase(n)) {
            EXP_NAMES.add(e[0]); expUntil = now + 1500; expFlushAt = now + 700;
            return true;
        }
        return false;
    }

    /** Once the reward lines stop coming, plays them: two drops share one book scene, more play one scene after another. */
    private static void flushExp(long now) {
        if (EXP_NAMES.isEmpty() || now < expFlushAt) return;
        List<String> names = new ArrayList<>(EXP_NAMES); EXP_NAMES.clear(); expFlushAt = Long.MAX_VALUE;
        FancyConfig c = FancyConfig.get();
        if (!(c.masterAnimate || c.masterAnnounce)) return;
        SlayerData.Slayer s = ExperimentData.slayer();
        FancyConfig.SlayerCfg sc = c.slayer(s.id);
        List<Res> found = new ArrayList<>();
        for (String nm : names) for (SlayerData.Drop dr : s.drops) if (dr.name.equalsIgnoreCase(nm)) {
            Res r = new Res(); Item it = r.item; FancyConfig.DropCfg dc = c.drop(s.id, dr.name);
            it.slayer = s; it.name = dr.name; it.id = dr.id; it.icon = dr.icon; it.odds = dr.odds; it.chance = dr.chance;
            it.untradeable = SlayerData.untradeable(dr.id); it.rank = ExperimentData.isRng(dr.name) ? 3 : 2;
            r.animate = sc.enabled && sc.animate && dc.animate; r.announce = sc.enabled && sc.announce && dc.announce; r.known = true;
            found.add(r);
        }
        if (!found.isEmpty()) playDrops(found, null, false);
    }

    /** "SLAYER BOSS! The Inferno Demonlord IV spawned!" says which slayer you are fighting, so the scene uses that mob and backdrop. */
    private static void noteBoss(String line) {
        int i = line.indexOf("SLAYER BOSS!");
        if (i < 0) return;
        String rest = line.substring(i + 12).toLowerCase(Locale.ROOT);
        for (SlayerData.Slayer s : SlayerData.ALL)
            if (!s.boss.isBlank() && rest.contains(s.boss.toLowerCase(Locale.ROOT))) { lastSlayer = s; return; }
    }

    /** Test hook: feed a chat line as if Hypixel had sent it (used by /fancydrops simulate). */
    public static void simulate(String line) { onChat(Component.literal(line)); }

    // ------------------------------------------------------------------ one kill

    /** A few lines about what was heard and what happened to it: config/fancydrops/drop-log.txt, so a missing animation can be traced. */
    static void dlog(String s) {
        try {
            java.nio.file.Path f = DataFiles.dir().resolve("drop-log.txt");
            if (java.nio.file.Files.exists(f) && java.nio.file.Files.size(f) > 200_000) java.nio.file.Files.delete(f);
            java.nio.file.Files.createDirectories(f.getParent());
            java.nio.file.Files.writeString(f, java.time.LocalTime.now().withNano(0) + "  " + s + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Throwable ignored) { }
    }

    /** Names compared without case, spaces and symbols, so "Flawed Opal Gemstone", "flawed opal gemstone!" and "✯ Flawed Opal Gemstone" are all the same drop. */
    private static String norm(String n) { return n == null ? "" : n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }

    /** The listed drop a chat name belongs to: {slayer, drop}, or null. Exact name first, then the same name ignoring symbols, then a listed name found inside the chat name (or the other way round). The slayer you are fighting wins a tie. */
    private static Object[] find(String chatName) {
        List<SlayerData.Slayer> order = new ArrayList<>(SlayerData.ALL);
        if (lastSlayer != null && order.remove(lastSlayer)) order.add(0, lastSlayer);
        for (SlayerData.Slayer s : order) for (SlayerData.Drop dr : s.drops) if (dr.name.equalsIgnoreCase(chatName)) return new Object[]{s, dr};
        String n = norm(chatName);
        if (n.isEmpty()) return null;
        for (SlayerData.Slayer s : order) for (SlayerData.Drop dr : s.drops) if (norm(dr.name).equals(n)) return new Object[]{s, dr};
        Object[] best = null; int bestLen = 0;
        for (SlayerData.Slayer s : order) for (SlayerData.Drop dr : s.drops) {
            String dn = norm(dr.name);
            if (dn.length() < 6) continue;
            if ((n.contains(dn) || (n.length() >= 8 && dn.contains(n))) && dn.length() > bestLen) { best = new Object[]{s, dr}; bestLen = dn.length(); }
        }
        return best;
    }

    private static boolean isCustom(String name) { for (FancyConfig.CustomDrop cd : FancyConfig.get().custom) if (cd.name.equalsIgnoreCase(name)) return true; return false; }

    private static final class Res { final Item item = new Item(); boolean animate, announce, known, unlisted; }

    private static Res resolve(ChatParser.Drop d) {
        FancyConfig c = FancyConfig.get();
        Res r = new Res();
        Item it = r.item;
        it.rank = d.rank(); it.mf = d.mf(); it.name = d.name(); it.sim = d.sim();
        Object[] hit = find(d.name());
        if (hit != null) {
            SlayerData.Slayer s = (SlayerData.Slayer) hit[0]; SlayerData.Drop dr = (SlayerData.Drop) hit[1];
            FancyConfig.SlayerCfg sc = c.slayer(s.id);
            FancyConfig.DropCfg dc = c.drop(s.id, dr.name);
            it.slayer = s; it.name = dr.name; it.id = dr.id; it.icon = dr.icon; it.odds = dr.odds; it.chance = dr.chance;
            it.amount = dr.amount ? d.amount() : 1;
            it.untradeable = SlayerData.untradeable(dr.id);
            r.animate = sc.enabled && sc.animate && dc.animate;
            r.announce = sc.enabled && sc.announce && dc.announce;
            r.known = true;
            lastSlayer = s;
            return r;
        }
        dlog("not one of the listed drops: \"" + d.name() + "\"");
        it.id = PriceService.normalize(d.name());
        it.untradeable = SlayerData.untradeable(it.id);
        for (FancyConfig.CustomDrop cd : c.custom) if (cd.name.equalsIgnoreCase(d.name())) {
            if (!cd.id.isBlank()) it.id = cd.id;
            r.animate = cd.animate; r.announce = cd.announce; r.known = true;
            return r;
        }
        r.animate = false; r.announce = false; r.known = true; r.unlisted = true;     // a RARE DROP this mod doesn't list: no scene, no message, but it is still tracked
        if (lastSlayer != null) it.slayer = lastSlayer;
        return r;
    }

    private static void onSession(KillTracker.Session<ChatParser.Drop> s) {
        FancyConfig c = FancyConfig.get();
        if (!(c.masterAnimate || c.masterAnnounce)) { s.close(); return; }
        List<Res> found = new ArrayList<>();
        for (ChatParser.Drop d : s.drops) found.add(resolve(d));
        for (Res r : found) dlog("scene for " + r.item.name + ": known=" + r.known + " animate=" + r.animate + " announce=" + r.announce + (s.fromKill ? " (kill)" : " (no kill line)"));
        if (found.isEmpty()) dlog("kill with no drops: the 'nothing' scene");
        final int seen = s.drops.size();
        final Runnable start = () -> {                       // the scene is starting: close the session, and treat stragglers as their own drops
            s.close();
            for (int i = seen; i < s.drops.size(); i++) playDrops(List.of(resolve(s.drops.get(i))), null, false);
        };
        playDrops(found, start, s.fromKill);
    }

    private static void playDrops(List<Res> found, Runnable onStart, boolean fromKill) {
        FancyConfig c = FancyConfig.get();
        List<Res> shown = new ArrayList<>();
        boolean any = false, unl = false;
        for (Res r : found) {
            if (r.unlisted) unl = true;
            if (!r.known) continue;                                  // not one of ours: no scene, no message, no profit entry
            any = true;
            boolean an = r.animate && c.masterAnimate, ac = r.announce && c.masterAnnounce;      // the two master switches in the menu
            r.item.announce = ac && an;                              // animated drops announce themselves when their scene starts
            record(r.item, ac && !an, r.animate || r.announce || r.unlisted);      // tracked = switched on in the Drop Library
            if (an) shown.add(r);
        }
        shown.sort(Comparator.comparingInt((Res r) -> -r.item.rank));
        if (shown.isEmpty()) {
            if ((!any || unl) && !found.isEmpty()) { if (onStart != null) onStart.run(); return; }      // a drop this mod doesn't list happened: it is tracked, and no 'nothing' scene is played over it
            if (fromKill && c.animateNothing && c.masterAnimate && lastSlayer != null) AnimationManager.play(new Anim(lastSlayer, null, -1, onStart));
            else if (onStart != null) onStart.run();
            return;
        }
        boolean first = true;
        for (int i = 0; i < shown.size(); i++) {                    // two drops from one kill share one scene (dyes keep their own)
            Item it = shown.get(i).item;
            Item it2 = null;
            if (!AnimationManager.isDye(it) && i + 1 < shown.size() && !AnimationManager.isDye(shown.get(i + 1).item)) it2 = shown.get(++i).item;
            boolean xp = it.slayer == ExperimentData.slayer();                                      // the book scene flips more for RNG items
            int ft = xp ? (ExperimentData.isRng(it.name) || it2 != null && ExperimentData.isRng(it2.name) ? 2 : 0) : -1;
            Anim a = new Anim(it.slayer, it, ft, first ? onStart : null);
            first = false;
            a.item2 = it2;
            AnimationManager.play(a);
        }
    }

    /** Sends the party / co-op message for a drop whose scene just started. */
    /** The slash command (without the slash) for the chosen channel. */
    static String chan(FancyConfig c) { return c.channel.equals("COOP") ? "cc " : c.channel.equals("ALL") ? "ac " : "pc "; }

    public static void announceNow(Item it) {
        if (it == null || !it.announce) return;
        it.announce = false;
        double total = it.price < 0 ? it.price : it.price * Math.max(1, it.amount);
        queueAnnounce(it, total);
    }

    private static SlayerData.Slayer nothingSlayer() {
        if (lastSlayer != null) return lastSlayer;
        return SlayerData.ALL.isEmpty() ? SlayerData.OTHER : SlayerData.ALL.get(0);
    }

    /** Looks the price up (cached = instant), then logs + announces. The scene waits briefly for the same lookup. */
    private static void record(Item it, boolean announce, boolean tracked) {
        FancyConfig c = FancyConfig.get();
        ItemIcons.prefetch(it.id);
        if (it.untradeable) it.price = -1;
        else {
            double quick = PriceService.lookup(it.id, it.name, false);
            if (quick >= 0) { it.price = quick; it.source = PriceService.source(it.id, it.name); }
        }
        final String slayerName = it.slayer.name;
        if (!it.sim && tracked) {                                             // counted the moment it drops: never waits for a price, and a failed price lookup can't lose it
            try {
                boolean book = "enchanted_book".equals(it.icon);
                int col = book ? 0xFFFFFFFF : it.odds.isEmpty() ? 0xFF55FF55 : Ui.odds(it.odds);
                boolean meter = it.odds.equals("Rare") || it.odds.equals("Extraordinary") || Ui.significant(it.odds);
                ProfitTracker.drop(it.slayer.id, it.name, it.id, it.icon, it.amount, col, it.untradeable, meter, meter || it.rank >= 1);   // only significant drops fill the tracker list
                dlog("tracked " + it.name + " x" + it.amount + " (" + it.slayer.id + ")");
            } catch (Throwable t) { dlog("tracking failed for " + it.name + ": " + t); }
        }
        CompletableFuture.supplyAsync(() -> it.untradeable ? -1.0 : PriceService.lookup(it.id, it.name, true)).exceptionally(ex -> -1.0).thenAccept(price ->
            Minecraft.getInstance().execute(() -> {
                it.price = price;
                if (price >= 0) it.source = PriceService.source(it.id, it.name);
                double total = price < 0 ? price : price * Math.max(1, it.amount);        // a stack of 40 is worth 40 of them
                if (!it.sim) LEDGER.add(new Rec(it.name, slayerName, total, System.currentTimeMillis()));     // simulated drops never touch the ledger or the profit tracker
                if (announce) queueAnnounce(it, total);
            }));
    }

    private static void queueAnnounce(Item it, double price) {
        FancyConfig c = FancyConfig.get();
        if (c.channel.equals("OFF") || !onHypixel()) return;               // never talks outside Hypixel
        if (price >= 0 && price < c.epicThreshold) return;                  // only RNG Drop class drops: same price as the Animations page ("RNG Drop from")
        OUT.add(chan(c) + message(it, price, it.mf != null ? it.mf : "0%"));
    }

    /** Which message a drop uses: 0 Slayers, 1 Dye, 2 Farming. */
    static int event(Item it) {
        if (it.slayer != null && it.slayer == ExperimentData.slayer()) return 4;
        if (it.slayer != null && FarmData.ALL.contains(it.slayer)) return 2;
        if (AnimationManager.isDye(it)) return 1;
        if (it.slayer != null && MythData.ALL.contains(it.slayer)) return 3;
        return 0;
    }

    private static String message(Item it, double price, String mf) { return message(it, price, mf, null); }

    private static String message(Item it, double price, String mf, String fmt) {
        FancyConfig c = FancyConfig.get();
        int ev = event(it);
        String priceText = it.untradeable ? "untradeable" : price >= 0 ? PriceService.fmt(price) + " coins" : "price unknown";
        String format = fmt != null ? fmt : c.formatFor(ev);
        if (!ChatFilter.ok(format)) format = ev == 4 ? FancyConfig.DEFAULT_EXP_FORMAT : ev == 3 ? FancyConfig.DEFAULT_MYTH_FORMAT : ev == 2 ? FancyConfig.DEFAULT_FARM_FORMAT : ev == 1 ? FancyConfig.DEFAULT_DYE_FORMAT : FancyConfig.DEFAULT_FORMAT;
        String from = it.slayer.id.equals("other") ? "a drop" : it.slayer.boss;
        boolean ob = mf != null && mf.startsWith("OB:");
        String obText = ob ? AnimationManager.mfText(mf) : "";
        String mfPart = ev == 4 ? "" : ev == 2 ? obText : AnimationManager.mfText(mf);                       // farming has Overbloom where the others have Magic Find
        String msg = format.replace("{item}", it.title()).replace("{price}", priceText)
                .replace("{mf}", mfPart).replace("{overbloom}", obText).replace("{source}", from)
                .replaceAll("\\s{2,}", " ").trim();
        return msg.length() > 250 ? msg.substring(0, 250) : msg;
    }

    /** If the text starts with a boss name (or slayer id), returns its length and puts the slayer in out[0]; else 0. */
    public static int bossPrefix(String raw, SlayerData.Slayer[] out) {
        int best = 0; out[0] = null;
        for (SlayerData.Slayer s : SlayerData.ALL) for (String n : new String[]{s.boss, s.id}) {
            if (n.isBlank() || n.length() <= best) continue;
            if (raw.regionMatches(true, 0, n, 0, n.length()) && (raw.length() == n.length() || Character.isWhitespace(raw.charAt(n.length())))) { best = n.length(); out[0] = s; }
        }
        return best;
    }
    public static List<String> dropNames(SlayerData.Slayer s) { List<String> l = new ArrayList<>(); for (SlayerData.Drop d : s.drops) l.add(d.name); return l; }

    /** Length of the longest of this boss's drop names at the start of the text (followed by a space or the end), or 0. */
    public static int dropPrefix(SlayerData.Slayer boss, String rest) {
        int best = 0;
        for (String cand : dropNames(boss))
            if (cand.length() > best && rest.regionMatches(true, 0, cand, 0, cand.length()) && (rest.length() == cand.length() || Character.isWhitespace(rest.charAt(cand.length())))) best = cand.length();
        return best;
    }

    /** All the dye drops of every slayer, as {slayer, drop} pairs. */
    public static List<Object[]> allDyes() {
        List<Object[]> l = new ArrayList<>();
        for (SlayerData.Slayer s : SlayerData.ALL) for (SlayerData.Drop d : s.drops) if (d.name.toLowerCase(Locale.ROOT).endsWith(" dye")) l.add(new Object[]{s, d});
        return l;
    }

    /** /fd sim <mf> dye [name]: a pretend dye drop (a random dye when none is named). Not counted in profit. */
    static void simulateDye(int mf, String name, Consumer<String> reply) {
        List<Object[]> all = allDyes();
        if (all.isEmpty()) { reply.accept("No dyes are in the data files."); return; }
        Object[] pick = null;
        if (name.isEmpty()) pick = all.get(new Random().nextInt(all.size()));
        else for (Object[] o : all) if (((SlayerData.Drop) o[1]).name.equalsIgnoreCase(name)) pick = o;
        if (pick == null) { reply.accept("\"" + name + "\" isn't a dye. Press Tab to see them."); return; }
        SlayerData.Slayer s = (SlayerData.Slayer) pick[0]; SlayerData.Drop d = (SlayerData.Drop) pick[1];
        reply.accept("Simulating " + d.name + " at " + mf + " Magic Find");
        long now = System.currentTimeMillis();
        lastSlayer = s;
        TRACKER.kill(now);
        TRACKER.drop(new ChatParser.Drop("RARE", d.name, 1, "+" + mf + "%", true), now);
    }

    /** /fd sim|simulation <mf> <Boss> [nothing | Drop 1 [Drop 2]]: a pretend kill of that boss. Nothing here is counted in the profit tracker. */
    public static void simulateKill(int mf, String items, Consumer<String> reply) {
        String raw = items == null ? "" : items.trim();
        int colon = raw.indexOf(':');
        if (colon >= 0) {                                              // /fd sim <mf> <boss:item[,item]> or <dye:item>
            String boss0 = raw.substring(0, colon).trim();
            String list = String.join(" ", java.util.Arrays.stream(raw.substring(colon + 1).split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList());
            raw = (boss0 + " " + list).trim();
        }
        String low = raw.toLowerCase(Locale.ROOT);
        if (low.equals("mytho") || low.startsWith("mytho ") || low.startsWith("mythological")) { simulateMyth(mf, raw.replaceFirst("(?i)^mytho(logical)?", "").trim(), reply); return; }
        if (low.equals("exp") || low.startsWith("exp ") || low.startsWith("experiment")) { simulateExp(mf, raw.replaceFirst("(?i)^exp(eriment(ation)?s?)?( table)?", "").trim(), reply); return; }
        if (low.equals("farm") || low.startsWith("farm ") || low.startsWith("farming")) { simulateFarm(mf, raw.replaceFirst("(?i)^farm(ing)?", "").trim(), reply); return; }
        if (low.equals("dye") || low.startsWith("dye ")) { simulateDye(mf, raw.substring(3).trim(), reply); return; }
        SlayerData.Slayer[] hit = new SlayerData.Slayer[1];
        int n = bossPrefix(raw, hit);
        SlayerData.Slayer boss = hit[0];
        if (boss == null) { reply.accept("Pick a boss, dye, farm, mytho or exp first: " + String.join(", ", SlayerData.ALL.stream().map(s -> s.boss).toList()) + ", dye, farm, mytho, exp  (Tab lists them)"); return; }
        String rest = raw.substring(n).trim();
        List<String> parsed = new ArrayList<>();
        if (rest.isEmpty()) { if (!boss.drops.isEmpty()) parsed.add(boss.drops.get(new Random().nextInt(boss.drops.size())).name); }
        else if (!rest.equalsIgnoreCase("nothing")) {
            while (!rest.isBlank()) {
                int len = dropPrefix(boss, rest);
                if (len == 0) { reply.accept("\"" + rest + "\" isn't a " + boss.boss + " drop. Press Tab to see them."); return; }
                final String cur = rest;
                parsed.add(dropNames(boss).stream().filter(x -> x.length() == len && cur.regionMatches(true, 0, x, 0, len)).findFirst().get());
                rest = rest.substring(len).trim();
            }
        }
        if (parsed.size() > 2) { reply.accept("A kill can drop at most two items here: /fd sim <magic find> <Boss> <Drop 1> [Drop 2]"); return; }
        reply.accept("Simulating " + (parsed.isEmpty() ? "nothing" : String.join(", ", parsed)) + " at " + mf + " Magic Find");
        long now = System.currentTimeMillis();
        lastSlayer = boss;
        TRACKER.kill(now);
        for (String name : parsed) TRACKER.drop(new ChatParser.Drop("RARE", name, 1, "+" + mf + "%", true), now);
    }

    /** True when connected to Hypixel. Chat announcements (and their tests) only happen there. */
    public static boolean onHypixel() {
        var server = Minecraft.getInstance().getCurrentServer();
        return server != null && server.ip != null && server.ip.toLowerCase(java.util.Locale.ROOT).contains("hypixel");
    }

    public static final String NOT_ON_HYPIXEL = "The chat message can't be tested because you're not on Hypixel.";

    /** What the message would look like for a sample drop (the menu shows this live under the format box). */
    public static String sample(String format) { return sample(format, 0); }

    /** The same for the Slayers (0), Dye (1), Farming (2) or Mythological (3) message. */
    public static String sample(String format, int ev) {
        Item it = new Item();
        if (ev == 4) { it.name = "Titanic Experience Bottle"; it.slayer = ExperimentData.slayer(); return message(it, 8_500_000, null, format); }
        if (ev == 3) { it.name = "Daedalus Stick"; it.slayer = MythData.ALL.isEmpty() ? SlayerData.OTHER : MythData.ALL.get(0); return message(it, 21_000_000, "+250%", format); }
        if (ev == 2) { it.name = "Cropie"; it.slayer = FarmData.ALL.isEmpty() ? SlayerData.OTHER : FarmData.ALL.get(0); return message(it, 4_500_000, "OB:+25", format); }
        if (ev == 1) { it.name = "Necron Dye"; it.slayer = SlayerData.OTHER; return message(it, 250_000_000, "+250%", format); }
        it.name = "Warden Heart"; it.slayer = SlayerData.ALL.isEmpty() ? SlayerData.OTHER : SlayerData.ALL.get(0);
        return message(it, 12_500_000, "+250%", format);
    }

    /** Menu "Test" button: sends the message to your party / co-op chat, but only on Hypixel. */
    public static void testChat(Consumer<String> reply) { testChat(reply, 0); }

    public static void testChat(Consumer<String> reply, int ev) {
        FancyConfig c = FancyConfig.get();
        if (!onHypixel()) { reply.accept(NOT_ON_HYPIXEL); return; }
        if (c.channel.equals("OFF")) { reply.accept("Chat announcements are off - pick Party or Co-op first."); return; }
        String cmd = chan(c);
        String msg = sample(c.formatFor(ev), ev);
        OUT.add(cmd + msg);
        reply.accept("Sent to /" + cmd.trim() + ": " + msg);
    }

    // ------------------------------------------------------------------ menu / preview plays

    /** Plays a drop's own scene (menu "play" button). The tier follows the drop's real odds and price. */
    public static void playDrop(SlayerData.Slayer sl, SlayerData.Drop dr, String mf) {
        if (sl == ExperimentData.slayer()) { playExpDrop(dr); return; }
        if (MythData.ALL.contains(sl)) { playMythDrop(sl, dr); return; }
        if (FarmData.ALL.contains(sl)) { playFarmDrop(sl, dr); return; }
        Item it = new Item();
        it.slayer = sl; it.name = dr.name; it.id = dr.id; it.icon = dr.icon; it.odds = dr.odds; it.chance = dr.chance; it.mf = mf;
        it.untradeable = SlayerData.untradeable(dr.id);
        it.amount = dr.amount ? 8 : 1;                           // stacked drops show a sample amount in the preview
        it.rank = Ui.significant(dr.odds) ? 2 : 1;
        previewPrice(it);
        AnimationManager.play(new Anim(sl, it, -1, null));
    }

        // ------------------------------------------------------------------ farming

    /** A RARE CROP! / pest RARE DROP! line: the small farming scene plays right away. Returns true when the line is replaced by the scene. */
    private static boolean playFarm(ChatParser.Drop d) {
        FancyConfig c = FancyConfig.get();
        SlayerData.Slayer src = FarmData.sourceOf(d.name());
        if (src == null) return false;                                         // not in the Farming library: Hypixel's own line stays
        SlayerData.Drop dr = null;
        String bare = FarmData.stripRarity(d.name());
        for (SlayerData.Drop x : src.drops) if (x.name.equalsIgnoreCase(bare)) dr = x;
        FancyConfig.SlayerCfg sc = c.slayer(src.id);
        FancyConfig.DropCfg dc = c.drop(src.id, dr.name);
        boolean animate = sc.enabled && sc.animate && dc.animate, announce = sc.enabled && sc.announce && dc.announce;
        if (!animate && !announce) return false;
        Item it = farmItem(src, dr, d.mf() != null && d.mf().startsWith("OB:") ? d.mf() : null, d.sim(), FarmData.rarityOf(d.name()));        // farming has Overbloom, not Magic Find
        it.rank = dr.rank >= 0 ? dr.rank : d.rank();
        it.amount = Math.max(1, d.amount());
        it.announce = announce && animate;
        record(it, announce && !animate, true);
        if (animate) { boolean hel = dr.id.equals("RAY_OF_HELIOS"); Anim a = new Anim(src, it, hel ? 2 : 0, null); a.farm = true; a.helios = hel; AnimationManager.play(a); }
        return animate;
    }

    private static Item farmItem(SlayerData.Slayer src, SlayerData.Drop dr, String mf, boolean sim) { return farmItem(src, dr, mf, sim, ""); }

    private static Item farmItem(SlayerData.Slayer src, SlayerData.Drop dr, String mf, boolean sim, String rarity) {
        Item it = new Item();
        it.slayer = src; it.name = dr.name; it.id = dr.id; it.icon = dr.icon; it.odds = dr.odds; it.chance = dr.chance; it.mf = mf; it.sim = sim;
        it.untradeable = dr.untradeable || SlayerData.untradeable(dr.id);
        it.rank = Math.max(0, dr.rank);
        if (dr.george != null && !dr.george.isEmpty()) {                                  // a pet: its rarity and what George pays for it
            String r = rarity.isEmpty() ? dr.rarity.toUpperCase(java.util.Locale.ROOT) : rarity;
            Long gp = dr.george.get(r);
            if (gp == null && !dr.george.isEmpty()) { r = dr.george.keySet().iterator().next(); gp = dr.george.get(r); }
            String nice = r.charAt(0) + r.substring(1).toLowerCase(java.util.Locale.ROOT);
            it.extra = nice + " Pet  ·  George pays " + PriceService.fmt(gp) + " coins";
            it.extraCol = switch (r) { case "COMMON" -> 0xFFFFFFFF; case "UNCOMMON" -> 0xFF55FF55; case "RARE" -> 0xFF5555FF; case "EPIC" -> 0xFFAA00AA; case "LEGENDARY" -> 0xFFFFAA00; default -> 0xFFFF55FF; };
        }
        return it;
    }

    /** Menu "play" button of a Farming drop. */
    public static void playFarmDrop(SlayerData.Slayer src, SlayerData.Drop dr) {
        Item it = farmItem(src, dr, "OB:+25", true);
        previewPrice(it);
        boolean hel = dr.id.equals("RAY_OF_HELIOS");
        Anim a = new Anim(src, it, hel ? 2 : 0, null); a.farm = true; a.helios = hel;
        AnimationManager.play(a);
    }

    /** Animations > Farming "Play": a sample rare crop. */
    public static void playFarmTest() {
        if (FarmData.ALL.isEmpty() || FarmData.ALL.get(0).drops.isEmpty()) return;
        playFarmDrop(FarmData.ALL.get(0), FarmData.ALL.get(0).drops.get(0));
    }

    // ------------------------------------------------------------------ mythological

    /** A mythological creature's rare drop line: the lightning scene plays right away. Returns true when the line is replaced by the scene. */
    private static boolean playMyth(ChatParser.Drop d) {
        FancyConfig c = FancyConfig.get();
        SlayerData.Drop dr = MythData.find(d.name());
        SlayerData.Slayer src = MythData.sourceOf(d.name());
        if (dr == null || src == null) return false;
        boolean dye = MythData.isDye(dr);                                        // the Mythological Dye is also on the Dye page: one switch for both, and it gets the paint scene
        FancyConfig.SlayerCfg sc = c.slayer(dye ? "dyes" : src.id);
        FancyConfig.DropCfg dc = MythData.cfg(c, src, dr);
        boolean animate = c.masterAnimate && sc.enabled && sc.animate && dc.animate, announce = c.masterAnnounce && sc.enabled && sc.announce && dc.announce;
        boolean tracked = sc.enabled && (dc.animate || dc.announce);
        if (!animate && !announce && !tracked) return false;
        Item it = mythItem(dye ? SlayerData.OTHER : src, dr, d.mf(), d.sim());
        it.amount = Math.max(1, d.amount());
        it.announce = announce && animate;
        record(it, announce && !animate, tracked);
        if (animate) { Anim a = new Anim(dye ? SlayerData.OTHER : src, it, -1, null); a.myth = !dye; AnimationManager.play(a); }       // costly drops (the RNG Drop price) get the bigger version
        return animate;
    }

    private static Item mythItem(SlayerData.Slayer src, SlayerData.Drop dr, String mf, boolean sim) {
        Item it = new Item();
        it.slayer = src; it.name = dr.name; it.id = dr.id; it.icon = dr.icon; it.odds = dr.odds; it.chance = dr.chance; it.mf = mf; it.sim = sim;
        it.untradeable = dr.untradeable || SlayerData.untradeable(dr.id);
        it.rank = Math.max(0, dr.rank);
        return it;
    }

    /** Menu "play" button of a Mythological drop. */
    public static void playMythDrop(SlayerData.Slayer src, SlayerData.Drop dr) {
        if (MythData.isDye(dr)) { SlayerData.Drop dd = new SlayerData.Drop(); dd.name = dr.name; dd.id = dr.id; dd.icon = DyeData.icon(dr.name); dd.odds = "RNGesus Incarnate"; dd.chance = dr.chance; playDrop(SlayerData.OTHER, dd, "+250%"); return; }
        Item it = mythItem(src, dr, "+250%", true);
        previewPrice(it);
        Anim a = new Anim(src, it, -1, null); a.myth = true;
        AnimationManager.play(a);
    }

    /** A ritual that dropped nothing: the circle goes dark. */
    public static void playMythNothing() {
        if (MythData.ALL.isEmpty()) return;
        Anim a = new Anim(MythData.ALL.get(Math.min(MythData.ALL.size() - 1, 6)), null, 0, null); a.myth = true;
        AnimationManager.play(a);
    }

    /** Animations > Mythological "Play": the normal scene (rng false) or the RNG Drop version (rng true) with a sample drop. */
    public static void playMythTest(boolean rng) {
        if (MythData.ALL.isEmpty()) return;
        SlayerData.Slayer s = MythData.ALL.get(Math.min(MythData.ALL.size() - 1, 6));
        if (s.drops.isEmpty()) return;
        SlayerData.Drop dr = s.drops.get(0);
        for (SlayerData.Drop x : s.drops) if (x.pct != null) { dr = x; break; }          // the creature's signature drop
        Item it = mythItem(s, dr, "+250%", true);
        previewPrice(it);
        Anim a = new Anim(s, it, rng ? 2 : 0, null); a.myth = true;
        AnimationManager.play(a);
    }

    /** An Experimentation Table drop: the book scene (RNG items flip more). */
    public static void playExpDrop(SlayerData.Drop dr) {
        SlayerData.Slayer s = ExperimentData.slayer();
        Item it = new Item();
        it.slayer = s; it.name = dr.name; it.id = dr.id; it.icon = dr.icon; it.odds = dr.odds; it.chance = dr.chance; it.mf = null;          // the Experimentation Table has no Magic Find or Overbloom
        it.untradeable = SlayerData.untradeable(dr.id);
        it.rank = ExperimentData.isRng(dr.name) ? 3 : 2;
        previewPrice(it);
        Anim a = new Anim(s, it, ExperimentData.isRng(dr.name) ? 2 : 0, null); a.exp = true;
        AnimationManager.play(a);
    }

    /** Animations > Experiments "Play": the normal book (rng false) or the RNG Drop version (rng true) with a sample drop. */
    public static void playExpTest(boolean rng) {
        SlayerData.Slayer s = ExperimentData.slayer();
        SlayerData.Drop dr = s.drops.get(0);
        for (SlayerData.Drop x : s.drops) if (ExperimentData.isRng(x.name) == rng) { dr = x; break; }
        playExpDrop(dr);
    }

    /** Animations > Farming > Ray of Helios "Play". */
    public static void playHeliosTest() {
        for (SlayerData.Slayer s : FarmData.ALL) for (SlayerData.Drop d : s.drops) if (d.id.equals("RAY_OF_HELIOS")) { playFarmDrop(s, d); return; }
        playFarmTest();
    }


    /** Every farming drop with its source, for /fd sim farm. */
    static List<Object[]> allFarmDrops() {
        List<Object[]> l = new ArrayList<>();
        for (SlayerData.Slayer s : FarmData.ALL) for (SlayerData.Drop d : s.drops) l.add(new Object[]{s, d});
        return l;
    }

    /** /fd sim <mf> farm [drop]: a pretend farming drop (a random one when none is named). Not counted in profit. */
    static void simulateFarm(int mf, String name, Consumer<String> reply) {
        List<Object[]> all = allFarmDrops();
        if (all.isEmpty()) { reply.accept("No farming drops are in the data files."); return; }
        Object[] pick = null;
        if (name.isEmpty()) pick = all.get(new Random().nextInt(all.size()));
        else for (Object[] o : all) if (((SlayerData.Drop) o[1]).name.equalsIgnoreCase(name)) pick = o;
        if (pick == null) { reply.accept("\"" + name + "\" isn't a farming drop. Press Tab to see them."); return; }
        SlayerData.Drop d = (SlayerData.Drop) pick[1];
        reply.accept("Simulating " + d.name);
        playFarmDrop((SlayerData.Slayer) pick[0], d);
    }

    /** Up to {max} drop names from the start of the text, longest match first ("Power VII Protection VI"); null (and a reply) when something doesn't match. */
    private static List<SlayerData.Drop> pickDrops(List<SlayerData.Drop> pool, String text, int max, String what, Consumer<String> reply) {
        List<SlayerData.Drop> out = new ArrayList<>();
        String rest = text.replace(",", " ").trim();
        while (!rest.isBlank()) {
            SlayerData.Drop best = null;
            for (SlayerData.Drop d : pool) {
                int n = d.name.length();
                if (rest.regionMatches(true, 0, d.name, 0, n) && (rest.length() == n || Character.isWhitespace(rest.charAt(n))) && (best == null || n > best.name.length())) best = d;
            }
            if (best == null) { reply.accept("\"" + rest + "\" isn't " + what + ". Press Tab to see them."); return null; }
            out.add(best); rest = rest.substring(best.name.length()).trim();
        }
        if (out.size() > max) { reply.accept("At most " + max + " drop" + (max == 1 ? "" : "s") + " here."); return null; }
        return out;
    }

    private static Item expItem(SlayerData.Drop dr) {
        Item it = new Item();
        it.slayer = ExperimentData.slayer(); it.name = dr.name; it.id = dr.id; it.icon = dr.icon; it.odds = dr.odds; it.chance = dr.chance; it.mf = null;
        it.untradeable = SlayerData.untradeable(dr.id);
        it.rank = ExperimentData.isRng(dr.name) ? 3 : 2;
        previewPrice(it);
        return it;
    }

    /** /fd sim Exp [Drop 1] [Drop 2] [mf]: a pretend Experimentation Table reward with one or two items (random when none is named). Not counted in profit. */
    static void simulateExp(int mf, String name, Consumer<String> reply) {
        SlayerData.Slayer s = ExperimentData.slayer();
        List<SlayerData.Drop> picks = name.isBlank() ? List.of(s.drops.get(new Random().nextInt(s.drops.size()))) : pickDrops(s.drops, name, 2, "an Experimentation Table item", reply);
        if (picks == null) return;
        reply.accept("Simulating " + String.join(" + ", picks.stream().map(d -> d.name).toList()) + " from the Experimentation Table");
        Item a = expItem(picks.get(0)), b = picks.size() > 1 ? expItem(picks.get(1)) : null;
        boolean rng = ExperimentData.isRng(picks.get(0).name) || picks.size() > 1 && ExperimentData.isRng(picks.get(1).name);
        Anim an = new Anim(s, a, rng ? 2 : 0, null); an.exp = true; an.item2 = b;
        AnimationManager.play(an);
    }

    /** /fd sim Mytho [Creature] [Drop 1] [Drop 2] [mf]: a pretend Mythological drop (or two), from a named creature or any (not counted in profit). */
    static void simulateMyth(int mf, String name, Consumer<String> reply) {
        SlayerData.Slayer from = null;
        for (SlayerData.Slayer s : MythData.ALL) {
            int n = s.boss.length();
            if (name.regionMatches(true, 0, s.boss, 0, n) && (name.length() == n || name.charAt(n) == ' ')) { from = s; name = name.substring(n).trim(); break; }
        }
        if (name.equalsIgnoreCase("nothing")) { reply.accept("Simulating a ritual that dropped nothing"); playMythNothing(); return; }
        List<SlayerData.Drop> pool = new ArrayList<>();
        if (from != null) pool.addAll(from.drops); else for (SlayerData.Slayer s : MythData.ALL) pool.addAll(s.drops);
        if (pool.isEmpty()) { reply.accept("No mythological drops loaded."); return; }
        List<SlayerData.Drop> picks = name.isBlank() ? List.of(pool.get(new Random().nextInt(pool.size()))) : pickDrops(pool, name, 2, from == null ? "a mythological drop" : "a " + from.boss + " drop", reply);
        if (picks == null) return;
        reply.accept("Simulating " + String.join(" + ", picks.stream().map(d -> d.name).toList()) + (from == null ? "" : " from " + from.boss) + " at " + mf + " Magic Find");
        if (picks.size() == 1 && from == null) { playMyth(new ChatParser.Drop("RARE", picks.get(0).name, 1, "+" + mf + "%", true)); return; }
        SlayerData.Slayer src = from != null ? from : MythData.sourceOf(picks.get(0).name);
        if (src == null) return;
        Item a = mythItem(src, picks.get(0), "+" + mf + "%", true); previewPrice(a);
        Item b = null;
        if (picks.size() > 1) { SlayerData.Slayer s2 = from != null ? from : MythData.sourceOf(picks.get(1).name); b = mythItem(s2 == null ? src : s2, picks.get(1), "+" + mf + "%", true); previewPrice(b); }
        Anim an = new Anim(src, a, -1, null); an.myth = true; an.item2 = b;
        AnimationManager.play(an);
    }

    /** Plays one of the four scenes: tier 0 / 1 / 2 with a sample drop, or the "nothing" scene. */
    public static void playTest(int tier, boolean nothing) {
        SlayerData.Slayer sl = nothingSlayer();
        if (nothing) { AnimationManager.play(new Anim(sl, null, 0, null)); return; }
        Item it = new Item();
        it.slayer = sl;
        SlayerData.Drop dr = sl.drops.isEmpty() ? null : sl.drops.get(Math.min(sl.drops.size() - 1, tier * 2));
        if (dr != null) { it.name = dr.name; it.id = dr.id; it.icon = dr.icon; it.odds = dr.odds; it.chance = dr.chance; }
        else { it.name = "Test Drop"; it.id = "TEST"; }
        it.rank = tier == 0 ? 0 : 2;
        it.mf = "+250%";
        it.price = tier == 2 ? Math.max(FancyConfig.get().epicThreshold, 150_000_000L) : tier == 1 ? 3_200_000 : 640_000;
        it.source = "Bazaar";
        ItemIcons.prefetch(it.id);
        AnimationManager.play(new Anim(sl, it, tier, null));
    }

    private static void previewPrice(Item it) {
        ItemIcons.prefetch(it.id);
        if (it.untradeable) { it.price = -1; return; }
        double quick = PriceService.lookup(it.id, it.name, false);
        if (quick >= 0) { it.price = quick; it.source = PriceService.source(it.id, it.name); }
        else CompletableFuture.supplyAsync(() -> PriceService.lookup(it.id, it.name, true)).thenAccept(p -> {
            it.price = p; if (p >= 0) it.source = PriceService.source(it.id, it.name);
        });
    }

    /** Every drop name the user could test: built-in slayer drops + their custom drops. */
    public static List<String> allDropNames() {
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (SlayerData.Slayer s : SlayerData.ALL) for (SlayerData.Drop d : s.drops) names.add(d.name);
        for (FancyConfig.CustomDrop cd : FancyConfig.get().custom) if (!cd.name.isBlank()) names.add(cd.name);
        return new ArrayList<>(names);
    }

    /** /fancydrops preview <animation|chat> <mf> <item 1> <item 2> ... ; chat previews never send a real message unless a channel is set. */
    public static void preview(String mode, String input, Consumer<String> reply) {
        String raw = input == null ? "" : input.trim();
        if (raw.isBlank()) { reply.accept("FancyDrops » Usage: /fancydrops preview <animation|chat> <magic find> <item 1> <item 2> ..."); return; }
        String[] first = raw.split("\\s+", 2);
        String mfToken = first[0].replaceFirst("%$", "").replace(',', '.');
        if (!mfToken.matches("\\d+(?:\\.\\d+)?")) { reply.accept("FancyDrops » Enter Magic Find after the mode. Example: /fancydrops preview animation 250 Archfiend Dice Warden Heart"); return; }
        if (first.length < 2 || first[1].isBlank()) { reply.accept("FancyDrops » Add one or more listed item names after the Magic Find value."); return; }

        String remaining = first[1].trim();
        List<String> validNames = allDropNames();
        validNames.sort((a, b) -> Integer.compare(b.length(), a.length()));
        List<String> parsed = new ArrayList<>();
        while (!remaining.isBlank()) {
            String match = null;
            for (String cand : validNames) {
                if (remaining.equalsIgnoreCase(cand) || (remaining.length() > cand.length() && remaining.regionMatches(true, 0, cand, 0, cand.length())
                        && Character.isWhitespace(remaining.charAt(cand.length())))) { match = cand; break; }
            }
            if (match == null) { reply.accept("FancyDrops » Unknown or unlisted item near: \"" + remaining + "\". Use Tab to choose listed items."); return; }
            parsed.add(match);
            remaining = remaining.substring(match.length()).trim();
        }
        String mf = "+" + mfToken + "%";
        if (mode.equalsIgnoreCase("chat") && !onHypixel()) { reply.accept("FancyDrops » " + NOT_ON_HYPIXEL); return; }
        reply.accept("FancyDrops » Previewing " + parsed.size() + " item(s) in " + mode + " mode at " + mf + " Magic Find.");
        PriceService.warm();
        for (String name : parsed) previewOne(name, mf, mode, reply);
    }

    private static void previewOne(String name, String mf, String mode, Consumer<String> reply) {
        SlayerData.Slayer sl = null; SlayerData.Drop dr = null;
        outer:
        for (SlayerData.Slayer s : SlayerData.ALL) for (SlayerData.Drop d : s.drops) if (d.name.equalsIgnoreCase(name)) { sl = s; dr = d; break outer; }
        Item it = new Item();
        it.mf = mf;
        if (dr != null) {
            it.slayer = sl; it.name = dr.name; it.id = dr.id; it.icon = dr.icon; it.odds = dr.odds; it.chance = dr.chance;
            it.untradeable = SlayerData.untradeable(dr.id);
            it.rank = Ui.significant(dr.odds) ? 2 : 1;
        } else {
            it.name = name; it.id = PriceService.normalize(name);
            for (FancyConfig.CustomDrop cd : FancyConfig.get().custom) if (cd.name.equalsIgnoreCase(name) && !cd.id.isBlank()) it.id = cd.id;
        }
        final Item fi = it;
        if (mode.equalsIgnoreCase("animation")) { previewPrice(fi); AnimationManager.play(new Anim(it.slayer, fi, -1, null)); }
        CompletableFuture.supplyAsync(() -> fi.untradeable ? -1.0 : PriceService.lookup(fi.id, fi.name, true)).thenAccept(p ->
            Minecraft.getInstance().execute(() -> {
                fi.price = p;
                if (mode.equalsIgnoreCase("animation")) {
                    reply.accept("FancyDrops » " + fi.name + " — " + (fi.untradeable ? "Untradeable" : p >= 0 ? PriceService.fmt(p) + " coins" : "Price unavailable"));
                    return;
                }
                FancyConfig c = FancyConfig.get();
                String message = message(fi, p, mf);
                if (!onHypixel()) { reply.accept("FancyDrops » " + NOT_ON_HYPIXEL); return; }
                if (c.channel.equals("OFF")) { reply.accept("FancyDrops » Chat preview (chat disabled): " + message); return; }
                String cmd = chan(c);
                OUT.add(cmd + message);
                reply.accept("FancyDrops » Chat preview queued for /" + cmd.trim() + ": " + message);
            }));
    }

    // ------------------------------------------------------------------ tick

    /** Called every client tick: drives the kill tracker and sends at most one chat message per 1.5 s. */
    public static void tick(Minecraft mc) {
        TRACKER.tick(System.currentTimeMillis());
        flushExp(System.currentTimeMillis());
        if (cooldown > 0) { cooldown--; return; }
        if (mc.player == null || OUT.isEmpty()) return;
        mc.player.connection.sendCommand(OUT.poll());
        cooldown = 30;
    }

    public static int kills() { return TRACKER.kills; }
    private DropListener() {}
}
