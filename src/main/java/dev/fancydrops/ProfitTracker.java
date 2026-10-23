package dev.fancydrops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Tracks what the player earns, per slayer and combined: overall (saved between sessions), this session, and per hour.
 * "Per hour" only counts time you were actually slaying: the time between slayer kills, ignoring breaks longer than 5 minutes.
 * Which slayer you are doing is read from Hypixel's quest messages and from the drops themselves.
 */
public final class ProfitTracker {
    public static final String ALL = "all";
    private static final long GAP = 5 * 60_000L;

    /** One kind of drop you tracked: how many, so the panel can price them live. */
    public static final class Item {
        public String name = "", id = "", icon = "nether_star"; public int amount, color = 0xFFFFFFFF, lastKill; public boolean untradeable, significant;   // significant: rare enough to be listed on the tracker
    }

    public static final class Stat {
        public double coins, costs;                 // coins = profit saved by older versions; costs = slayer quests paid for (auto-slayer)
        public int drops, kills; public long activeMs;
        public Map<String, Item> items = new LinkedHashMap<>();
        public String lastName = ""; public int lastBosses, lastColor = 0xFFFFAA00;     // the RNG meter line: "<item> in <n> bosses!"
        public Stat copy() { Stat s = new Stat(); s.coins = coins; s.costs = costs; s.drops = drops; s.kills = kills; s.activeMs = activeMs; return s; }
        Stat fix() { if (items == null) items = new LinkedHashMap<>(); return this; }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Stat> OVERALL = new LinkedHashMap<>(), SESSION = new LinkedHashMap<>();
    private static final Map<String, Long> LAST = new HashMap<>();     // last kill per slayer, this session
    private static String current;                                     // slayer id the player seems to be doing, or null

    // words that tell us which slayer a line is about
    private static final String[][] KEYWORDS = {
        {"zombie", "revenant", "zombie"}, {"spider", "tarantula", "spider"}, {"wolf", "wolves", "sven", "wolf"},
        {"enderman", "endermen", "voidgloom", "enderman"}, {"blaze", "blazes", "inferno", "demonlord", "blaze"},
        {"vampire", "bloodfiend", "riftstalker", "vampire"}};

    private static final java.util.regex.Pattern COST = java.util.regex.Pattern.compile("Took ([\\d.,]+)([kKmMbB])? coins from your bank for auto-slayer");

    private static Path file() { return DataFiles.dir().resolve("profit.json"); }

    public static void load() {
        try {
            if (Files.exists(file())) {
                Map<String, Stat> m = GSON.fromJson(Files.readString(file()), new TypeToken<Map<String, Stat>>() {}.getType());
                if (m != null) { OVERALL.clear(); OVERALL.putAll(m); }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void save() {
        try { Files.createDirectories(file().getParent()); Files.writeString(file(), GSON.toJson(OVERALL)); } catch (Exception e) { e.printStackTrace(); }
    }

    public static Stat overall(String id) { return OVERALL.computeIfAbsent(id, k -> new Stat()).fix(); }
    public static Stat session(String id) { return SESSION.computeIfAbsent(id, k -> new Stat()).fix(); }
    public static String current() { return current; }
    public static void setCurrent(String id) { if (id != null && !id.equals("other")) current = id; }

    public static void clearSession() { SESSION.clear(); LAST.clear(); }
    public static void clearOverall() { OVERALL.clear(); save(); }

    /** Every chat line: quest messages tell us which slayer the player is working on. */
    public static void noteLine(String line) {
        if (line == null) return;
        String l = line.toLowerCase(Locale.ROOT);
        java.util.regex.Matcher m = COST.matcher(line);
        if (m.find()) {                                           // "Took 68.4k coins from your bank for auto-slayer..."
            double v = Double.parseDouble(m.group(1).replace(",", ""));
            String u = m.group(2) == null ? "" : m.group(2).toLowerCase(Locale.ROOT);
            v *= u.equals("k") ? 1e3 : u.equals("m") ? 1e6 : u.equals("b") ? 1e9 : 1;
            for (String id : ids(current)) { session(id).costs += v; overall(id).costs += v; }
            save();
            return;
        }
        if (!(l.contains("slayer") || l.contains("combat xp worth") || l.contains("slay "))) return;
        for (String[] k : KEYWORDS) for (int i = 0; i < k.length - 1; i++) if (l.contains(k[i])) { current = k[k.length - 1]; return; }
    }

    /** A slayer kill / quest line. {@code counted} is true once per kill (the quest-complete line only keeps the clock honest). */
    public static void activity(long now, boolean counted) {
        for (String id : ids(current)) {
            Long last = LAST.get(id);
            long add = last != null && now - last <= GAP ? now - last : 0;
            LAST.put(id, now);
            for (Stat s : new Stat[]{session(id), overall(id)}) { s.activeMs += add; if (counted) s.kills++; }
        }
        if (counted) save();
    }

    /** A tracked drop. Nothing else is counted: only the drops you have switched on in the Drop Library. */
    public static void drop(String slayerId, String name, String itemId, String icon, int amount, int color, boolean untradeable, boolean meter, boolean significant) {
        setCurrent(slayerId);
        for (String id : ids(slayerId == null || slayerId.equals("other") ? null : slayerId)) {
            for (Stat s : new Stat[]{session(id), overall(id)}) {
                Item it = s.items.computeIfAbsent(name, k -> new Item());
                it.name = name; it.id = itemId; it.icon = icon; it.color = color; it.untradeable = untradeable; it.significant |= significant;
                it.amount += Math.max(1, amount); s.drops++;
                if (meter) { s.lastName = name; s.lastColor = color; s.lastBosses = Math.max(0, s.kills - it.lastKill); it.lastKill = s.kills; }
            }
        }
        save();
    }

    /** What one kind of drop is worth right now (priced live, so switching the price source updates the panel). */
    public static double value(Item it) {
        if (it.untradeable) return 0;
        double p = PriceService.lookup(it.id, it.name, false);
        return p > 0 ? p * it.amount : 0;
    }
    /** Gross: every tracked drop at today's prices, plus any profit saved by older versions. */
    public static double gross(Stat s) { double t = s.coins; for (Item it : s.items.values()) t += value(it); return t; }
    /** Net profit: gross minus quest costs. */
    public static double coins(Stat s) { return gross(s) - s.costs; }

    private static List<String> ids(String slayerId) { return slayerId == null ? List.of(ALL) : List.of(ALL, slayerId); }

    /** Coins per hour of real slaying this session, or -1 while there isn't enough data yet. */
    public static double perHour(String id, long now) { return perHour(session(id), activeMs(id, now)); }
    public static double perHour(Stat s, long activeMs) {
        if (activeMs < 60_000) return -1;
        return coins(s) / (activeMs / 3_600_000.0);
    }

    /** Session slaying time, including the stretch since the last kill if it is still recent. */
    public static long activeMs(String id, long now) {
        Long last = LAST.get(id);
        return session(id).activeMs + (last != null && now - last <= GAP ? now - last : 0);
    }

    /** "All slayers" or the boss name for a slayer id. */
    public static String label(String id) {
        if (ALL.equals(id)) return "All slayers";
        for (SlayerData.Slayer s : SlayerData.ALL) if (s.id.equals(id)) return s.boss;
        return "All slayers";
    }

    private ProfitTracker() {}
}
