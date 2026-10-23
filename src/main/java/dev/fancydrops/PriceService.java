package dev.fancydrops;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;

/**
 * Lowest-BIN prices from the Hypixel auctions API, matched by SkyBlock item id first and by item name second.
 * A community lowest-BIN feed is loaded alongside as a fallback for anything the auctions data can't resolve.
 */
public final class PriceService {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private static volatile Map<String, Double> bins = Map.of();        // auctions: by SkyBlock id
    private static volatile Map<String, Double> byName = Map.of();      // auctions: by normalised item name
    private static volatile Map<String, Double> feed = Map.of();        // fallback feed
    private static volatile Map<String, Double> bazaarBuy = Map.of();   // bazaar instant-buy price by product id
    private static volatile Map<String, Double> bazaarSell = Map.of();  // bazaar instant-sell price by product id
    private static volatile long lastRefresh = 0, lastAttempt = 0;
    private static volatile boolean loading = false, attempted = false;
    private static volatile String loadedKey = "";
    public static volatile String status = "Not loaded";

    private static String key() {
        FancyConfig c = FancyConfig.get();
        return c.priceSource + "|" + c.useBazaar + "|" + c.useAuctions;
    }

    private static boolean stale() {
        long now = System.currentTimeMillis();
        return (now - lastRefresh > 10 * 60_000L || !loadedKey.equals(key())) && now - lastAttempt > 30_000L;
    }

    /** Starts a background load only if nothing is loaded yet or the data is old. */
    public static void warm() { if (stale()) refreshAsync(); }

    public static void refreshAsync() {
        if (loading) return;
        loading = true; lastAttempt = System.currentTimeMillis();
        Thread t = new Thread(() -> {
            FancyConfig c = FancyConfig.get();
            String src = c.priceSource;
            StringBuilder st = new StringBuilder();
            status = "Loading...";
            if (c.useBazaar) {
                try {
                    fromBazaar();
                    st.append(bazaarBuy.size()).append(" bazaar items");
                } catch (Exception e) { st.append("Bazaar failed: ").append(e.getMessage()); }
            }
            if (c.useAuctions) {
                if (st.length() > 0) st.append(" + ");
                try {
                    Parsed p = fromAuctions();
                    bins = p.ids; byName = p.names;
                    st.append(p.ids.size()).append(" auction items");
                } catch (Exception e) { st.append("Auctions failed: ").append(e.getMessage()); }
                // Do not call the retired/unreliable community lowest-BIN feed. Hypixel auctions and Bazaar
                // are the supported price sources; a failed optional fallback should never poison status output.
            }
            if (!c.useBazaar && !c.useAuctions) st.append("No price sources enabled");
            if (!bins.isEmpty() || !feed.isEmpty() || !bazaarBuy.isEmpty()) { lastRefresh = System.currentTimeMillis(); loadedKey = key(); }
            status = st.toString();
            attempted = true; loading = false;
        }, "FancyDrops-Prices");
        t.setDaemon(true);
        t.start();
    }

    /** Price or -1. Bazaar first (if enabled), then the auction house. With wait==true this blocks while a load runs. */
    public static double lookup(String id, String name, boolean wait) {
        FancyConfig c = FancyConfig.get();
        warm();
        if (wait) {
            long end = System.currentTimeMillis() + 60_000;
            while (bins.isEmpty() && feed.isEmpty() && bazaarBuy.isEmpty() && (loading || !attempted) && System.currentTimeMillis() < end) {
                try { Thread.sleep(250); } catch (InterruptedException ignored) { break; }
            }
        }
        if (c.useBazaar) {
            Map<String, Double> bz = c.bazaarPrice.equals("SELL") ? bazaarSell : bazaarBuy;
            for (String cand : candidates(id)) { Double p = bz.get(cand); if (p != null) return p; }
            if (name != null) { Double p = bz.get(normalize(name)); if (p != null) return p; }
        }
        if (c.useAuctions) {
            for (String cand : candidates(id)) { Double p = bins.get(cand); if (p != null) return p; }
            if (name != null) { Double p = byName.get(normalize(name)); if (p != null) return p; }
            for (String cand : candidates(id)) { Double p = feed.get(cand); if (p != null) return p; }
        }
        return -1;
    }

    /** Where lookup() found the price: "Bazaar", "AH" (auction house), or "" (not found). Same order as lookup(). */
    public static String source(String id, String name) {
        FancyConfig c = FancyConfig.get();
        if (c.useBazaar) {
            Map<String, Double> bz = c.bazaarPrice.equals("SELL") ? bazaarSell : bazaarBuy;
            for (String cand : candidates(id)) if (bz.containsKey(cand)) return "Bazaar";
            if (name != null && bz.containsKey(normalize(name))) return "Bazaar";
        }
        if (c.useAuctions) {
            for (String cand : candidates(id)) if (bins.containsKey(cand) || feed.containsKey(cand)) return "AH";
            if (name != null && byName.containsKey(normalize(name))) return "AH";
        }
        return "";
    }

    private static void fromBazaar() throws Exception {
        JsonObject products = JsonParser.parseString(get("https://api.hypixel.net/v2/skyblock/bazaar")).getAsJsonObject().getAsJsonObject("products");
        Map<String, Double> buy = new HashMap<>(), sell = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : products.entrySet()) {
            JsonObject q = e.getValue().getAsJsonObject().getAsJsonObject("quick_status");
            // If instant buy / instant sell ever look swapped in-game, swap these two keys:
            buy.put(e.getKey(), q.get("buyPrice").getAsDouble());
            sell.put(e.getKey(), q.get("sellPrice").getAsDouble());
        }
        bazaarBuy = buy; bazaarSell = sell;
    }

    private static List<String> candidates(String id) {
        List<String> l = new ArrayList<>();
        l.add(id);
        Matcher m = Pattern.compile("^ENCHANTMENT_(.+)_(\\d+)$").matcher(id);
        if (m.matches()) l.add(m.group(1) + ";" + m.group(2));
        m = Pattern.compile("^(.+);(\\d+)$").matcher(id);
        if (m.matches() && !id.contains("RUNE")) l.add("ENCHANTMENT_" + m.group(1) + "_" + m.group(2));
        return l;
    }

    public static String normalize(String name) {
        return name.toUpperCase(Locale.ROOT).replace("'", "").replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
    }

    /** 194.8k / 5.55M / 1.20B - same style as the reference video. */
    public static String fmt(double v) {
        if (v >= 1e9) return String.format(Locale.ROOT, "%.2fB", v / 1e9);
        if (v >= 1e6) return String.format(Locale.ROOT, "%.2fM", v / 1e6);
        if (v >= 1e3) return String.format(Locale.ROOT, "%.1fk", v / 1e3);
        return String.valueOf(Math.round(v));
    }

    private static String get(String url) throws Exception {
        HttpRequest r = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("User-Agent", "FancyDrops/0.3").GET().build();
        HttpResponse<String> resp = HTTP.send(r, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new IOException("HTTP " + resp.statusCode());
        return resp.body();
    }

    private static final class Parsed { final Map<String, Double> ids = new HashMap<>(), names = new HashMap<>(); }

    private static Parsed fromAuctions() throws Exception {
        Parsed out = new Parsed();
        int page = 0, total = 1;
        while (page < total) {
            status = "Loading auctions " + (page + 1) + "/" + total;
            JsonObject o = JsonParser.parseString(get("https://api.hypixel.net/v2/skyblock/auctions?page=" + page)).getAsJsonObject();
            total = o.get("totalPages").getAsInt();
            for (JsonElement e : o.getAsJsonArray("auctions")) {
                JsonObject a = e.getAsJsonObject();
                if (!a.has("bin") || !a.get("bin").getAsBoolean()) continue;
                if (a.has("claimed") && a.get("claimed").getAsBoolean()) continue;
                double price = a.get("starting_bid").getAsDouble();
                String id = NbtMini.itemId(a.get("item_bytes").getAsString());
                if (id != null) out.ids.merge(id, price, Math::min);
                if (a.has("item_name")) {
                    String nm = a.get("item_name").getAsString().replaceAll("\u00a7.", "");
                    if (!nm.equals("Enchanted Book")) out.names.merge(normalize(nm), price, Math::min);
                }
            }
            page++;
            Thread.sleep(120);
        }
        return out;
    }

    /** Tiny NBT reader so we don't depend on Minecraft's (frequently changing) NBT API. */
    static final class NbtMini {
        @SuppressWarnings("unchecked")
        static String itemId(String b64) {
            try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(b64))))) {
                in.readByte(); in.readUTF();
                Map<String, Object> root = (Map<String, Object>) read(in, 10);
                List<Object> list = (List<Object>) root.get("i");
                Map<String, Object> item = (Map<String, Object>) list.get(0);
                Map<String, Object> ea = (Map<String, Object>) ((Map<String, Object>) item.get("tag")).get("ExtraAttributes");
                String id = (String) ea.get("id");
                if (id == null) return null;
                if (id.equals("ENCHANTED_BOOK") && ea.get("enchantments") instanceof Map<?, ?> en && !en.isEmpty()) {
                    Map.Entry<?, ?> e = en.entrySet().iterator().next();
                    return String.valueOf(e.getKey()).toUpperCase(Locale.ROOT) + ";" + ((Number) e.getValue()).intValue();
                }
                if ((id.equals("RUNE") || id.equals("UNIQUE_RUNE")) && ea.get("runes") instanceof Map<?, ?> ru && !ru.isEmpty()) {
                    Map.Entry<?, ?> e = ru.entrySet().iterator().next();
                    return String.valueOf(e.getKey()).toUpperCase(Locale.ROOT) + "_RUNE;" + ((Number) e.getValue()).intValue();
                }
                return id;
            } catch (Exception e) { return null; }
        }

        private static Object read(DataInputStream in, int t) throws IOException {
            switch (t) {
                case 1: return in.readByte();
                case 2: return in.readShort();
                case 3: return in.readInt();
                case 4: return in.readLong();
                case 5: return in.readFloat();
                case 6: return in.readDouble();
                case 7: in.skipNBytes(in.readInt()); return null;
                case 8: return in.readUTF();
                case 9: {
                    int et = in.readByte(), n = in.readInt();
                    List<Object> l = new ArrayList<>();
                    for (int i = 0; i < n; i++) l.add(read(in, et));
                    return l;
                }
                case 10: {
                    Map<String, Object> m = new LinkedHashMap<>();
                    while (true) {
                        int tt = in.readByte();
                        if (tt == 0) break;
                        String k = in.readUTF();
                        m.put(k, read(in, tt));
                    }
                    return m;
                }
                case 11: in.skipNBytes(4L * in.readInt()); return null;
                case 12: in.skipNBytes(8L * in.readInt()); return null;
                default: throw new IOException("bad tag " + t);
            }
        }
    }
    private PriceService() {}
}
