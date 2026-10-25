package dev.fancydrops;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Item art for the drops, loaded in the background and cached in config/fancydrops/icon-cache/:
 *  - head items  -> the player's skin (all six faces + hat layer), drawn as a real 3D cube by the ray-caster
 *  - other items -> the PNG from the SkyBlock wiki (hypixelskyblock.minecraft.wiki), extruded into a thin 3D slab
 *  - no art      -> a small generated gem, so there is always something to show
 */
public final class ItemIcons {
    /** heads: six 8x8 faces in Box order (+x, -x, +y, -y, +z, -z). */
    private static final class Cached { String legacy, headTexture; int[] face, wikiPixels; int[][] heads; int wikiVersion; transient int[] iso; transient int[][] spin; transient int[][] faces6; }

    public static final class Icon {
        volatile boolean ready;
        volatile Cached data;
    }

    private static final Map<String, Icon> CACHE = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "FancyDrops-Icons"); t.setDaemon(true); return t;
    });
    private static final Pattern VALUE = Pattern.compile("Value:\"([A-Za-z0-9+/=]+)\"");
    private static final String REPO = "https://raw.githubusercontent.com/NotEnoughUpdates/NotEnoughUpdates-REPO/master/items/";

    public static Icon get(String id) {
        Icon ic = CACHE.get(id);
        if (ic == null) {
            ic = new Icon();
            Icon prev = CACHE.putIfAbsent(id, ic);
            if (prev != null) return prev;
            final Icon fi = ic;
            POOL.execute(() -> load(id, fi));
        }
        return ic;
    }

    private static Path cacheFile(String id) {
        return DataFiles.dir().resolve("icon-cache").resolve(id.replaceAll("[^A-Za-z0-9_]", "_") + ".json");
    }

    private static void load(String id, Icon ic) {
        if (isBook(id)) {                                          // every enchanted book / bundle: a proper book, never a head or a wiki render
            Cached b = new Cached(); b.wikiVersion = 11; b.wikiPixels = bookSprite();
            ic.data = b; ic.ready = true; return;
        }
        if ("@COIN".equals(id)) { Cached b = new Cached(); b.wikiVersion = 11; b.wikiPixels = coinSprite(); ic.data = b; ic.ready = true; return; }
        if ("@seed".equals(FarmData.imgOf(id))) {                  // Seasoning is not a real item: a few seeds
            Cached b = new Cached(); b.wikiVersion = 11; b.wikiPixels = seedSprite();
            ic.data = b; ic.ready = true; return;
        }
        Cached c = new Cached();
        try {
            Path cf = cacheFile(id);
            if (Files.exists(cf)) {
                Cached old = GSON.fromJson(Files.readString(cf), Cached.class);
                if (old != null && old.wikiVersion >= 11 && (old.wikiPixels != null || old.face != null) && (old.heads != null || !skullFlag(id))) { ic.data = old; ic.ready = true; return; }
            }
        } catch (Exception ignored) {}

        if (FarmData.hasId(id)) {                                  // farming items: the page's own picture file (/images/Name.png, as on the Rare Crops page) comes first
            String img = FarmData.imgOf(id);
            java.util.List<String> files = new java.util.ArrayList<>(); if (img != null) files.add(img); files.addAll(wikiPages(id));
            for (String page : files) {
                try { c.wikiPixels = downloadWikiPixels("https://hypixelskyblock.minecraft.wiki/images/" + URLEncoder.encode(page.trim().replace(' ', '_'), StandardCharsets.UTF_8).replace("+", "%20").replace("%27", "'") + ".png"); if (c.wikiPixels != null) break; } catch (Exception ignored) {}
            }
        }
        if (c.wikiPixels == null && FarmData.hasId(id)) for (String page : wikiPages(id)) {      // farming items: the picture in the page's own info box (its og:image), not the "lead image" guess that can pick a pest
            try { c.wikiPixels = ogImagePixels(page); if (c.wikiPixels != null) break; } catch (Exception ignored) {}
        }
        if (c.wikiPixels == null && !FarmData.hasId(id)) for (String page : wikiPages(id)) {                       // try the likely page names until one has a picture
            try { c.wikiPixels = fetchWikiPixels(page); if (c.wikiPixels != null) break; } catch (Exception ignored) {}
        }
        if (c.wikiPixels == null && !FarmData.hasId(id)) try { c.wikiPixels = fetchWikiPixels(searchWiki(wikiPageName(id))); } catch (Exception ignored) {}   // a search can land on the wrong page (a pest for a crop), so farming items go without
        c.wikiVersion = 11;
        boolean flatFarm = FarmData.hasId(id) && !skullFlag(id) && c.wikiPixels != null;      // a plain farming item (Cornucopia...) keeps the wiki's own picture, never a head cube
        if (!flatFarm) for (String cand : neuIds(id)) {
            if (c.heads != null) break;
            try {
            JsonObject j = JsonParser.parseString(httpString(REPO + URLEncoder.encode(cand, StandardCharsets.UTF_8) + ".json")).getAsJsonObject();
            c.legacy = j.has("itemid") ? j.get("itemid").getAsString().replace("minecraft:", "") : null;
            String nbt = j.has("nbttag") ? j.get("nbttag").getAsString() : "";
            Matcher vm = VALUE.matcher(nbt);
            if (("skull".equals(c.legacy) || "player_head".equals(c.legacy)) && vm.find()) {
                c.headTexture = vm.group(1);
                c.heads = fetchHead(vm.group(1));
                c.face = c.heads[4];
            }
            } catch (Exception e) {
                if (skullFlag(id)) log(id + ": NEU '" + cand + "' failed: " + e);        // the next candidate id, the Hypixel item list, or finally the wiki PNG / gem; the log says why a head did not load
            }
        }
        if (c.heads == null && !flatFarm) {                       // second source: Hypixel's own item list knows every skull's skin
            try {
                String skin = apiSkin(id);
                if (skin != null) { c.heads = fetchHead(skin); c.face = c.heads[4]; c.headTexture = skin; }
                else if (skullFlag(id)) log(id + ": no skin in NEU repo or Hypixel item list (list " + (API_ID == null ? "NOT loaded" : "loaded, " + API_ID.size() + " skins") + ")");
            } catch (Exception e) { log(id + ": head failed: " + e); }
        }
        if (c.heads != null || !skullFlag(id)) try {             // a skull that could not be found is not cached, so it is tried again next time
            Path cf = cacheFile(id);
            Files.createDirectories(cf.getParent());
            Files.writeString(cf, GSON.toJson(c));
        } catch (Exception ignored) {}
        ic.data = c;
        ic.ready = true;
    }

    private static boolean isBook(String id) {
        for (SlayerData.Slayer s : SlayerData.ALL) for (SlayerData.Drop d : s.drops) if (d.id.equalsIgnoreCase(id) && "enchanted_book".equals(d.icon)) return true;
        for (SlayerData.Slayer s : FarmData.ALL) for (SlayerData.Drop d : s.drops) if (d.id.equalsIgnoreCase(id) && "enchanted_book".equals(d.icon)) return true;
        for (SlayerData.Slayer s : MythData.ALL) for (SlayerData.Drop d : s.drops) if (d.id.equalsIgnoreCase(id) && "enchanted_book".equals(d.icon)) return true;
        return false;
    }

    private static final String[] BOOK = {
        "................",
        "..oooooooooooo..",
        ".oPPPPPPPPPPPPo.",
        ".oPhhhhhhhhhhPo.",
        ".oPhPPPPPPPPhPo.",
        ".oPhPPPgPPPPhPo.",
        ".oPhPPgGgPPPhPo.",
        ".oPhPPPgPPPPhPo.",
        ".oPhPPPPPPPPhPo.",
        ".oPhhhhhhhhhhPo.",
        ".oPPPPPPPPPPPPo.",
        ".oooooooooooooo.",
        ".owwwwwwwwwwwwo.",
        "..oooooooooooo..",
        "................",
        "................"};

    /** A purple enchanted book with a gold glint, 16x16. */
    /** A small pile of wheat seeds. */
    static int[] seedSprite() {
        String[] rows = {"................", "................", "................", "................", "................", ".....ss..ss.....", "....sSSs.sSSs...", "....sSSs.sSSs...", ".....ss..ss.....", "..ss...ss...ss..", ".sSSs.sSSs.sSSs.", ".sSSs.sSSs.sSSs.", "..ss...ss...ss..", "................", "................", "................"};
        int[] px = new int[256];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            char ch = rows[y].charAt(x);
            px[y * 16 + x] = ch == 's' ? 0xFF6B5A1E : ch == 'S' ? 0xFFC9B24A : 0;
        }
        return px;
    }

    /** The Total profit coin as a picture: a gold disc with a darker inner ring and a light bar. */
    static int[] coinSprite() {
        int[] px = new int[256];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            double d = Math.hypot(x - 7.5, y - 7.5);
            int c = 0;
            if (d <= 5.6) c = d > 4.0 ? 0xFFFFC857 : (x >= 7 && x <= 8 && y >= 5 && y <= 10) ? 0xFFFFE9A0 : 0xFFE0A21B;
            else if (d <= 6.6) c = 0xFFA86E12;
            if (c != 0 && d <= 5.6 && x + y <= 9 && d > 4.0) c = 0xFFFFE08A;
            px[y * 16 + x] = c;
        }
        return px;
    }

    static int[] bookSprite() {
        int[] p = new int[256];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int c = switch (BOOK[y].charAt(x)) {
                case 'o' -> 0xFF22103A; case 'P' -> 0xFF5B2A8C; case 'h' -> 0xFF8E57C9; case 'g' -> 0xFFE9B84A; case 'G' -> 0xFFFFF0A0; case 'w' -> 0xFFE8DCC0;
                default -> 0;
            };
            p[y * 16 + x] = c;
        }
        return p;
    }

    private static void log(String s) {
        try { Files.writeString(DataFiles.dir().resolve("icon-log.txt"), s + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception ignored) {}
    }

    private static boolean skullFlag(String id) {
        if (id != null && id.toUpperCase(java.util.Locale.ROOT).endsWith("_DYE")) return true;          // every SkyBlock dye is a player head
        if (id != null && id.toUpperCase(java.util.Locale.ROOT).startsWith("DYE_")) return true;
        if (TreasureData.headArt(id)) return true;
        for (SlayerData.Slayer s : SlayerData.ALL) for (SlayerData.Drop d : s.drops) if (d.skull && d.id.equalsIgnoreCase(id)) return true;
        for (SlayerData.Slayer s : MythData.ALL) for (SlayerData.Drop d : s.drops) if (d.skull && d.id.equalsIgnoreCase(id)) return true;
        return false;
    }

    /** Ids to try in the NEU item repo: the drop's id, with ";1" for runes, and the one built from its display name. */
    private static java.util.List<String> neuIds(String id) {
        java.util.LinkedHashSet<String> l = new java.util.LinkedHashSet<>();
        l.add(id);
        if (!id.contains(";") && id.endsWith("_RUNE")) l.add(id + ";1");
        if (id.endsWith(";1") && id.endsWith("_RUNE;1") == false) l.add(id.replace(";1", "_RUNE;1"));
        l.add(wikiPageName(id).toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", ""));
        return new java.util.ArrayList<>(l);
    }

    // ---- Hypixel's public item list (api.hypixel.net/v2/resources/skyblock/items): id and name -> skull skin
    private static volatile java.util.Map<String, String> API_ID, API_NAME;
    private static long apiTried;

    private static synchronized void loadApi() {
        if (API_ID != null) return;
        long now = System.currentTimeMillis();
        if (apiTried != 0 && now - apiTried < 60_000) return;      // after a failure, try again at most once a minute
        apiTried = now;
        try {
            Path f = DataFiles.dir().resolve("hypixel-items.json");
            String body;
            if (Files.exists(f) && now - Files.getLastModifiedTime(f).toMillis() < 7L * 86_400_000L) body = Files.readString(f);
            else { body = httpString("https://api.hypixel.net/v2/resources/skyblock/items"); Files.createDirectories(f.getParent()); Files.writeString(f, body); }
            var byId = new java.util.HashMap<String, String>(); var byName = new java.util.HashMap<String, String>();
            for (var e : JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("items")) {
                JsonObject o = e.getAsJsonObject();
                if (!o.has("skin") || o.get("skin").isJsonNull()) continue;
                var s = o.get("skin");
                String v = s.isJsonObject() ? (s.getAsJsonObject().has("value") ? s.getAsJsonObject().get("value").getAsString() : null) : s.getAsString();
                if (v == null || v.isBlank()) continue;
                if (o.has("id")) byId.put(o.get("id").getAsString().toUpperCase(java.util.Locale.ROOT), v);
                if (o.has("name")) byName.put(o.get("name").getAsString().toLowerCase(java.util.Locale.ROOT).replaceAll("§.", "").trim(), v);
            }
            API_NAME = byName; API_ID = byId;
        } catch (Exception ignored) {}
    }

    private static String apiSkin(String id) {
        loadApi();
        if (API_ID == null) return null;
        for (String cand : neuIds(id)) { String v = API_ID.get(cand.toUpperCase(java.util.Locale.ROOT)); if (v != null) return v; }
        String name = wikiPageName(id).toLowerCase(java.util.Locale.ROOT).trim();
        String v = API_NAME.get(name);
        if (v == null) v = API_NAME.get(name.replaceAll("\\s*\\(.*\\)$", ""));
        if (v == null) {                                         // "Pestilence Rune I" -> any "pestilence rune ..." entry; "Revenant Shard" -> "revenant shard ..."
            String base = name.replaceAll("\\s+(i{1,3}|iv|v|vi{0,3}|ix|x)$", "").trim();
            if (base.length() >= 6) for (var e : API_NAME.entrySet()) if (e.getKey().equals(base) || e.getKey().startsWith(base + " ") || e.getKey().endsWith(" " + base)) { v = e.getValue(); break; }
        }
        return v;
    }

    /** Use the configured display name as the Wiki page title, not the internal SkyBlock id. */
    private static String wikiPageName(String id) {
        if ("MYTHOLOGICAL_RITUAL".equals(id)) return "Mythological Ritual";
        for (SlayerData.Slayer slayer : SlayerData.ALL) for (SlayerData.Drop drop : slayer.drops)
            if (drop.id.equalsIgnoreCase(id)) return drop.name;
        for (SlayerData.Slayer slayer : FarmData.ALL) for (SlayerData.Drop drop : slayer.drops)
            if (drop.id.equalsIgnoreCase(id)) return drop.name;
        for (SlayerData.Slayer slayer : MythData.ALL) for (SlayerData.Drop drop : slayer.drops)
            if (drop.id.equalsIgnoreCase(id)) return drop.name;
        return id.replace('_', ' ');
    }

    /** Page names to try: the drop name, without a trailing roman numeral ("Pestilence Rune I" -> "Pestilence Rune"), then the id. */
    private static java.util.List<String> wikiPages(String id) {
        String n = wikiPageName(id);
        java.util.LinkedHashSet<String> l = new java.util.LinkedHashSet<>();
        l.add(n);
        l.add(n.replaceAll("\\s+(I{1,3}|IV|V|VI{0,3}|IX|X)$", ""));
        l.add(id.replace(';', ' ').replace('_', ' ').toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+\\d+$", ""));
        return new java.util.ArrayList<>(l);
    }

    /** Last resort: ask the wiki's search for the closest page title. */
    private static String searchWiki(String name) throws Exception {
        String api = "https://hypixelskyblock.minecraft.wiki/api.php?action=query&format=json&list=search&srlimit=1&srsearch=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        var hits = JsonParser.parseString(httpString(api)).getAsJsonObject().getAsJsonObject("query").getAsJsonArray("search");
        if (hits.isEmpty()) throw new java.io.IOException("no page");
        return hits.get(0).getAsJsonObject().get("title").getAsString();
    }

    /** Fetch a Wiki item PNG. Try pageimages first, then the page's own image files (many item pages do not expose a pageimage). */
    /** The picture the wiki page itself advertises (og:image = the item's info-box icon). */
    private static int[] ogImagePixels(String pageName) throws Exception {
        String html = httpString("https://hypixelskyblock.minecraft.wiki/w/" + URLEncoder.encode(pageName.replace(' ', '_'), StandardCharsets.UTF_8).replace("+", "%20"));
        Matcher m = Pattern.compile("<meta[^>]+property=\"og:image\"[^>]+content=\"([^\"]+)\"").matcher(html);
        if (!m.find()) m = Pattern.compile("<meta[^>]+content=\"([^\"]+)\"[^>]+property=\"og:image\"").matcher(html);
        if (!m.find()) return null;
        String url = m.group(1).replace("&amp;", "&");
        if (url.startsWith("//")) url = "https:" + url; else if (url.startsWith("/")) url = "https://hypixelskyblock.minecraft.wiki" + url;
        return downloadWikiPixels(url);
    }

    private static int[] fetchWikiPixels(String pageName) throws Exception {
        String title = URLEncoder.encode(pageName.replace(' ', '_'), StandardCharsets.UTF_8);
        String api = "https://hypixelskyblock.minecraft.wiki/api.php?action=query&format=json&redirects=1&prop=pageimages&piprop=thumbnail&pithumbsize=128&titles=" + title;
        JsonObject root = JsonParser.parseString(httpString(api)).getAsJsonObject();
        JsonObject pages = root.getAsJsonObject("query").getAsJsonObject("pages");
        for (String key : pages.keySet()) {
            JsonObject page = pages.getAsJsonObject(key);
            if (page.has("thumbnail")) {
                String imageUrl = page.getAsJsonObject("thumbnail").get("source").getAsString();
                try { return downloadWikiPixels(imageUrl); } catch (Exception ignored) {}
            }
        }

        // Find an actual image file associated with the article, prioritising an item render matching its name.
        String imageApi = "https://hypixelskyblock.minecraft.wiki/api.php?action=query&format=json&redirects=1&prop=images&imlimit=max&titles=" + title;
        JsonObject imageRoot = JsonParser.parseString(httpString(imageApi)).getAsJsonObject();
        JsonObject imagePages = imageRoot.getAsJsonObject("query").getAsJsonObject("pages");
        java.util.List<String> files = new java.util.ArrayList<>();
        for (String key : imagePages.keySet()) {
            JsonObject page = imagePages.getAsJsonObject(key);
            if (!page.has("images")) continue;
            for (var e : page.getAsJsonArray("images")) {
                String file = e.getAsJsonObject().get("title").getAsString();
                String norm = file.toLowerCase(java.util.Locale.ROOT);
                if (norm.endsWith(".png") && !norm.contains("logo") && !norm.contains("icon_")) files.add(file);
            }
        }
        files.sort((a,b) -> {
            String an=a.toLowerCase(java.util.Locale.ROOT), bn=b.toLowerCase(java.util.Locale.ROOT);
            boolean am=an.contains(pageName.toLowerCase(java.util.Locale.ROOT)), bm=bn.contains(pageName.toLowerCase(java.util.Locale.ROOT));
            if (am != bm) return am ? -1 : 1;
            boolean ai=an.contains("item") || an.contains("render") || an.contains("sprite"), bi=bn.contains("item") || bn.contains("render") || bn.contains("sprite");
            return ai == bi ? a.compareToIgnoreCase(b) : (ai ? -1 : 1);
        });
        for (String file : files) {
            String infoApi = "https://hypixelskyblock.minecraft.wiki/api.php?action=query&format=json&prop=imageinfo&iiprop=url&iiurlwidth=128&titles=" + URLEncoder.encode(file, StandardCharsets.UTF_8).replace("+", "%20");
            try {
                JsonObject infoRoot = JsonParser.parseString(httpString(infoApi)).getAsJsonObject();
                JsonObject infoPages = infoRoot.getAsJsonObject("query").getAsJsonObject("pages");
                for (String key : infoPages.keySet()) {
                    JsonObject page = infoPages.getAsJsonObject(key);
                    if (!page.has("imageinfo") || page.getAsJsonArray("imageinfo").isEmpty()) continue;
                    JsonObject info = page.getAsJsonArray("imageinfo").get(0).getAsJsonObject();
                    String imageUrl = info.get("url").getAsString();
                    if (!imageUrl.toLowerCase(java.util.Locale.ROOT).contains(".png")) continue;
                    return downloadWikiPixels(imageUrl);
                }
            } catch (Exception ignored) {}
        }
        throw new java.io.IOException("No Wiki PNG found for " + pageName);
    }

    private static int[] downloadWikiPixels(String imageUrl) throws Exception {
        HttpResponse<byte[]> r = HTTP.send(HttpRequest.newBuilder(URI.create(imageUrl)).timeout(Duration.ofSeconds(20))
                .header("User-Agent", "FancyDrops/1.0.1 (SkyBlock item icon lookup)").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() != 200) throw new java.io.IOException("Wiki image HTTP " + r.statusCode());
        var img = ImageIO.read(new ByteArrayInputStream(r.body()));
        if (img == null) throw new java.io.IOException("Unsupported Wiki image");
        var scaled = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var graphics = scaled.createGraphics();
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        // Preserve transparent backgrounds; keep the complete image centred in a square instead of stretching it.
        double factor = Math.min(16.0 / img.getWidth(), 16.0 / img.getHeight());
        int w = Math.max(1, (int)Math.round(img.getWidth() * factor));
        int h = Math.max(1, (int)Math.round(img.getHeight() * factor));
        graphics.drawImage(img, (16-w)/2, (16-h)/2, w, h, null);
        graphics.dispose();
        int[] pixels = new int[256];
        scaled.getRGB(0, 0, 16, 16, pixels, 0, 16);
        return pixels;
    }

    private static String httpString(String url) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("User-Agent", "FancyDrops/1.0.1").GET().build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new java.io.IOException("HTTP " + r.statusCode());
        return r.body();
    }

    /**
     * Skin texture -> the six faces of the head (hat layer merged on top), in Box order: +x, -x, +y, -y, +z, -z.
     * Viewed from the front, the character's left side is on the right (+x).
     */
    private static int[][] fetchHead(String base64) throws Exception {
        String raw = base64.trim(), url;
        if (raw.startsWith("http")) url = raw;
        else if (raw.matches("[0-9a-fA-F]{40,80}")) url = "https://textures.minecraft.net/texture/" + raw;
        else {
            String json = raw.startsWith("{") ? raw : new String(Base64.getDecoder().decode(raw), StandardCharsets.UTF_8);
            JsonObject tex = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN");
            url = tex.get("url").getAsString();
        }
        url = url.replace("http://", "https://");
        HttpResponse<byte[]> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("User-Agent", "FancyDrops").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() != 200) throw new java.io.IOException("HTTP " + r.statusCode());
        var img = ImageIO.read(new ByteArrayInputStream(r.body()));
        boolean hat = img.getWidth() >= 64 && img.getHeight() >= 16;
        int[][] src = {{16, 8}, {0, 8}, {8, 0}, {16, 0}, {8, 8}, {24, 8}};    // +x, -x, +y, -y, +z, -z (x, y of each 8x8 region)
        int[][] faces = new int[6][64];
        for (int f = 0; f < 6; f++) for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            int base = img.getRGB(src[f][0] + x, src[f][1] + y) | 0xFF000000;
            int over = hat ? img.getRGB(src[f][0] + 32 + x, src[f][1] + y) : 0;
            faces[f][y * 8 + x] = (over >>> 24) > 16 ? (over | 0xFF000000) : base;
        }
        return faces;
    }

    /** Starts loading the item art now so it is ready by the time the item pops up. */
    public static void prefetch(String id) { get(id); }
    public static boolean ready(String id) { return get(id).ready; }
    /** True once the picture for this id has downloaded and has real pixels (a wiki picture, or a head). */
    public static boolean hasPixels(String id) { Icon ic = get(id); Cached d = ic.data; return ic.ready && d != null && (d.wikiPixels != null || d.face != null || d.heads != null); }

    /** A little gem, used when no art could be found. Tinted with the slayer's colour. */
    static int[] gem(int rgb) {
        int[] p = new int[256];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            double d = Math.abs(x - 7.5) + Math.abs(y - 7.5) * (y < 7 ? 1.25 : 0.8);
            if (d > 7.2) continue;
            float lit = (x < 8 ? 1.1f : 0.8f) * (y < 6 ? 1.12f : 1f);
            if (d > 6.2) lit *= 0.6f;
            p[y * 16 + x] = Canvas.shade(0xFF000000 | rgb, lit);
        }
        return p;
    }

    /** The 3D model of this item for the scene (null while the art is still loading). */
    static Scene.Model model(String id, int tint) {
        Icon ic = get(id);
        if (!ic.ready) return null;
        Cached d = ic.data;
        if (d != null && d.heads != null && FancyConfig.get().show3d) {
            Render3D.Box b = new Render3D.Box(4, 4, 4);
            for (int f = 0; f < 6; f++) b.face(f, d.heads[f], 8);
            return new Scene.Model(new Render3D.Obj[]{b}, 8, 0.6);
        }
        if (d != null && d.heads == null && d.wikiPixels != null && skullFlag(id) && FancyConfig.get().show3d && !isBook(id)) {   // no skin found: rebuild the cube from the head's picture so it still spins as a real block
            int[][] cube = isoHead(d.wikiPixels);
            if (cube != null) {
                Render3D.Box b = new Render3D.Box(4, 4, 4);
                for (int f = 0; f < 6; f++) b.face(f, cube[f], 8);
                return new Scene.Model(new Render3D.Obj[]{b}, 8, 0.6);
            }
        }
        int[] px = d != null && d.wikiPixels != null ? d.wikiPixels : d != null && d.face != null ? upscale(d.face) : gem(tint & 0xFFFFFF);
        if (skullFlag(id) && FancyConfig.get().show3d) return new Scene.Model(PuffyItem.of(px, 16, 16, true, 2.2f), null);   // a skull whose skin we could not find still spins in 3D
        return new Scene.Model(PuffyItem.of(px, 16, 16, true), px);
    }

    /**
     * A head's flat picture is an isometric cube (top, front-left and front-right faces). Sampling each of those three faces gives
     * the six faces of a real block, in Box order +x, -x, +y, -y, +z, -z (the hidden sides reuse the visible ones, a little darker).
     * Returns null when the picture does not look like a cube.
     */
    private static int[][] isoHead(int[] px) {
        int solid = 0; for (int p : px) if ((p >>> 24) > 40) solid++;
        if (solid < 90) return null;
        float[] T = {8f, 1f}, L = {1f, 4.5f}, R = {15f, 4.5f}, C = {8f, 8f}, LB = {1f, 11.5f}, B = {8f, 15f};
        int[] top = new int[64], left = new int[64], right = new int[64];
        for (int v = 0; v < 8; v++) for (int u = 0; u < 8; u++) {
            float a = (u + 0.5f) / 8f, b = (v + 0.5f) / 8f;
            top[v * 8 + u] = sampleIso(px, T[0] + a * (R[0] - T[0]) + b * (L[0] - T[0]), T[1] + a * (R[1] - T[1]) + b * (L[1] - T[1]));
            left[v * 8 + u] = sampleIso(px, L[0] + a * (C[0] - L[0]) + b * (LB[0] - L[0]), L[1] + a * (C[1] - L[1]) + b * (LB[1] - L[1]));
            right[v * 8 + u] = sampleIso(px, C[0] + a * (R[0] - C[0]) + b * (B[0] - C[0]), C[1] + a * (R[1] - C[1]) + b * (B[1] - C[1]));
        }
        int[][] f = new int[6][];
        f[0] = right;                                   // +x
        f[1] = shadeFace(mirror(right), 0.8f);          // -x
        f[2] = top;                                     // +y
        f[3] = shadeFace(top, 0.55f);                   // -y
        f[4] = left;                                    // +z (front)
        f[5] = shadeFace(mirror(left), 0.75f);          // -z
        return f;
    }

    private static int sampleIso(int[] px, float x, float y) {
        int ix = Math.max(0, Math.min(15, (int) Math.floor(x))), iy = Math.max(0, Math.min(15, (int) Math.floor(y)));
        int c = px[iy * 16 + ix];
        if ((c >>> 24) < 40) {                          // an edge pixel: take the nearest solid neighbour
            for (int r = 1; r <= 2; r++) for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++) {
                int nx = ix + dx, ny = iy + dy; if (nx < 0 || ny < 0 || nx > 15 || ny > 15) continue;
                int n = px[ny * 16 + nx]; if ((n >>> 24) >= 40) return n | 0xFF000000;
            }
            return 0xFF23202B;
        }
        return c | 0xFF000000;
    }

    private static int[] mirror(int[] f) { int[] o = new int[64]; for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) o[y * 8 + x] = f[y * 8 + 7 - x]; return o; }
    private static int[] shadeFace(int[] f, float k) { int[] o = new int[64]; for (int i = 0; i < 64; i++) o[i] = Canvas.shade(f[i], k); return o; }

    private static int[] upscale(int[] face8) {
        int[] o = new int[256];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) o[y * 16 + x] = face8[(y / 2) * 8 + x / 2];
        return o;
    }

    /** Draws the flat icon centred on (cx, cy). scale 1.0 = a normal 16px item. Used by the menu. */
    public static void draw(GuiGraphicsExtractor g, String id, String fallback, int cx, int cy, float scale) {
        if (scale <= 0.01f) return;
        Icon ic = get(id);
        Cached d = ic.ready ? ic.data : null;
        Gfx.push(g); Gfx.translate(g, cx, cy);
        int[] spun = null;
        boolean head = d != null && d.heads != null;                                   // any item that is a skull (NEU says so) shows a 3D head block, never the flat wiki picture (Myth the Fish, pets, fragments...)
        if (d != null && d.heads == null && d.wikiPixels != null && skullFlag(id) && !isBook(id)) {         // no skin found: the icon is still a real block, rebuilt from the wiki picture of the head
            if (d.iso == null) { int[][] cube = isoHead(d.wikiPixels); if (cube != null) d.iso = isoCube(cube); }
        }
        int bobY = 0;
        if (d != null && (d.heads != null || (d.iso != null && skullFlag(id))) && !Icons.STILL_ICONS) {                       // a skull turns and bobs like an item dropped in the world
            if (d.faces6 == null) d.faces6 = d.heads != null ? d.heads : isoHead(d.wikiPixels);
            if (d.faces6 != null) {
                if (d.spin == null) d.spin = new int[SPIN_FRAMES][];
                long tm = System.currentTimeMillis(); float ph0 = (id.hashCode() & 255) / 255f, ang = (float) ((tm / 1000.0 * 1.15 + ph0 * 6.28) % (Math.PI * 2));
                int fr = Math.round(ang / (float) (Math.PI * 2) * SPIN_FRAMES) % SPIN_FRAMES;
                if (d.spin[fr] == null) d.spin[fr] = spinCube(d.faces6, fr * (float) (Math.PI * 2) / SPIN_FRAMES);
                bobY = Math.round((float) Math.sin(tm / 380.0 + ph0 * 6.28) * 1.2f);
                spun = d.spin[fr];
            }
        }
        int[] pix = spun != null ? spun : d == null ? null : d.iso != null && d.heads == null && skullFlag(id) ? d.iso : head ? (d.iso != null ? d.iso : (d.iso = isoCube(d.heads))) : d.wikiPixels != null ? d.wikiPixels : d.heads != null ? (d.iso != null ? d.iso : (d.iso = isoCube(d.heads))) : null;
        if (pix != null) {
            Gfx.scale(g, scale); if (bobY != 0) Gfx.translate(g, 0, bobY);
            for (int y = 0; y < 16; y++) {                         // one fill per run of the same colour, not per pixel (far fewer draw calls)
                int x = 0;
                while (x < 16) {
                    int c = pix[y * 16 + x];
                    if ((c >>> 24) == 0) { x++; continue; }
                    int x2 = x + 1;
                    while (x2 < 16 && pix[y * 16 + x2] == c) x2++;
                    g.fill(x - 8, y - 8, x2 - 8, y - 7, c);
                    x = x2;
                }
            }
        } else if (d != null && d.face != null) {
            Gfx.scale(g, scale * 2f);
            for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
                int c = d.face[y * 8 + x];
                if ((c >>> 24) != 0) g.fill(x - 4, y - 4, x - 3, y - 3, c);
            }
        } else if (fallback != null && !fallback.isEmpty() && !Icons.has(fallback)) {      // no art (yet): the plain Minecraft item, never a grey square
            Gfx.scale(g, scale);
            Gfx.item(g, fallback, -8, -8);
        } else if (ic.ready) {                                        // no art found: a little gem, never a Steve head
            Gfx.scale(g, scale);
            int[] gem = gem(0xC9A227);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) if ((gem[y * 16 + x] >>> 24) != 0) g.fill(x - 8, y - 8, x - 7, y - 7, gem[y * 16 + x]);
        }
        Gfx.pop(g);
    }
    private static final int SPIN_FRAMES = 36;
    /** A real little block turning on its own axis, one picture per angle (cached): each pixel is a ray through a unit cube, so every face is textured, shaded by a fixed light and outlined. */
    private static int[] spinCube(int[][] f, float yaw) {
        int[] out = new int[256]; float pitch = 0.52f, cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw), cp = (float) Math.cos(pitch), sp = (float) Math.sin(pitch), k = 5.1f;
        float lx = -0.45f, ly = 0.8f, lz = 0.55f, ll = (float) Math.sqrt(lx * lx + ly * ly + lz * lz); lx /= ll; ly /= ll; lz /= ll;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            float vx = (x + 0.5f - 8f) / k, vy = -(y + 0.5f - 8.4f) / k, vz = 10f;                       // the ray starts in front of the block and goes straight back
            float ry = vy * cp + vz * sp, rz = -vy * sp + vz * cp;                                       // undo the tilt, then the turn
            float ox = vx * cy - rz * sy, oz = vx * sy + rz * cy, oy = ry;
            float wdx = cp * sy, wdy = -sp, wdz = -cp * cy;                                              // the view direction (0,0,-1) in the block's own space
            float t0 = -1e9f, t1 = 1e9f; int face = -1; float[] o3 = {ox, oy, oz}, d3 = {wdx, wdy, wdz};
            boolean miss = false;
            for (int ax = 0; ax < 3 && !miss; ax++) {
                if (Math.abs(d3[ax]) < 1e-6f) { if (Math.abs(o3[ax]) > 1f) miss = true; continue; }
                float ta = (-1f - o3[ax]) / d3[ax], tb = (1f - o3[ax]) / d3[ax];
                if (ta > tb) { float tmp = ta; ta = tb; tb = tmp; }
                if (ta > t0) { t0 = ta; face = d3[ax] > 0 ? ax * 2 + 1 : ax * 2; }
                if (tb < t1) t1 = tb;
            }
            if (miss || t0 > t1 || face < 0) continue;
            float hx = ox + wdx * t0, hy = oy + wdy * t0, hz = oz + wdz * t0;
            // face ids from the axis: x -> 0 (+x) / 1 (-x), y -> 2 / 3, z -> 4 / 5
            int fi = face; float u, v;
            switch (fi) {
                case 0 -> { u = (1f - hz) / 2f; v = (1f - hy) / 2f; }
                case 1 -> { u = (hz + 1f) / 2f; v = (1f - hy) / 2f; }
                case 2 -> { u = (hx + 1f) / 2f; v = (hz + 1f) / 2f; }
                case 3 -> { u = (hx + 1f) / 2f; v = (1f - hz) / 2f; }
                case 4 -> { u = (hx + 1f) / 2f; v = (1f - hy) / 2f; }
                default -> { u = (1f - hx) / 2f; v = (1f - hy) / 2f; }
            }
            int c = f[fi][Math.min(7, Math.max(0, (int) (v * 8))) * 8 + Math.min(7, Math.max(0, (int) (u * 8)))];
            float nx = fi == 0 ? 1 : fi == 1 ? -1 : 0, ny = fi == 2 ? 1 : fi == 3 ? -1 : 0, nz = fi == 4 ? 1 : fi == 5 ? -1 : 0;     // the face's normal in view space
            float wx = nx * cy + nz * sy, wz = -nx * sy + nz * cy, wy = ny;
            float vyv = wy * cp - wz * sp, vzv = wy * sp + wz * cp, vxv = wx;
            float lit = 0.5f + 0.62f * Math.max(0f, vxv * lx + vyv * ly + vzv * lz);
            out[y * 16 + x] = Canvas.shade(c | 0xFF000000, lit);
        }
        int[] res = out.clone();
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            if ((out[y * 16 + x] >>> 24) == 0) continue;
            boolean edge = x == 0 || y == 0 || x == 15 || y == 15 || (out[y * 16 + x - 1] >>> 24) == 0 || (out[y * 16 + x + 1] >>> 24) == 0 || (out[(y - 1) * 16 + x] >>> 24) == 0 || (out[(y + 1) * 16 + x] >>> 24) == 0;
            if (edge) res[y * 16 + x] = Canvas.shade(out[y * 16 + x], 0.5f);
        }
        return res;
    }

    /** A small 3D block (top, front and side) made from a head's faces, for lists that show a flat 16 x 16 picture. */
    private static int[] isoCube(int[][] heads) {
        int[] out = new int[256];
        int[] top = shadeFace(heads[2], 1.22f), front = shadeFace(heads[4], 0.96f), side = shadeFace(heads[0], 0.62f);
        float[][] faces = {{8f, 1f, 7f, 3.5f, -7f, 3.5f}, {1f, 4.5f, 7f, 3.5f, 0f, 7f}, {8f, 8f, 7f, -3.5f, 0f, 7f}};      // origin, edge u, edge v of each visible face
        int[][] tex = {top, front, side};
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            for (int k = 0; k < 3; k++) {
                float[] f = faces[k];
                float px = x + 0.5f - f[0], py = y + 0.5f - f[1], det = f[2] * f[5] - f[3] * f[4];
                float a = (px * f[5] - py * f[4]) / det, b = (f[2] * py - f[3] * px) / det;
                if (a < 0f || a >= 1f || b < 0f || b >= 1f) continue;
                out[y * 16 + x] = tex[k][Math.min(7, (int) (b * 8)) * 8 + Math.min(7, (int) (a * 8))] | 0xFF000000;
                break;
            }
        }
        int[] res = out.clone();                                                  // depth: a dark outline round the block, a bright edge where the top meets the front
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            if ((out[y * 16 + x] >>> 24) == 0) continue;
            boolean edge = x == 0 || y == 0 || x == 15 || y == 15 || (out[y * 16 + x - 1] >>> 24) == 0 || (out[y * 16 + x + 1] >>> 24) == 0 || (out[(y - 1) * 16 + x] >>> 24) == 0 || (out[(y + 1) * 16 + x] >>> 24) == 0;
            if (edge) res[y * 16 + x] = Canvas.shade(out[y * 16 + x], 0.5f);
        }
        return res;
    }

    private ItemIcons() {}
}
