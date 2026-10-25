package dev.fancydrops;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Asks GitHub whether the mod has a newer release. One quiet request, in the background, at most once in a while (or when you press Check). */
final class UpdateCheck {
    static final String REPO = "imtraminecraft/fancydrops";
    static final String PAGE = "https://github.com/" + REPO + "/releases";
    /** 0 = not asked yet, 1 = asking, 2 = done, 3 = could not ask. */
    static volatile int state = 0;
    static volatile String latest = "", latestName = "", error = "";
    static volatile long checkedAt = 0L, minShowUntil = 0L;
    /** True while a check runs, and for a moment after a button press so the wording does not flicker. */
    static boolean busy() { return state == 1 || state == 0 || System.currentTimeMillis() < minShowUntil; }
    private static final Pattern VER = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    /** Starts a check unless one is running or the last one is recent (force = the Check button). */
    static void check(boolean force) {
        long now = System.currentTimeMillis();
        if (state == 1) return;
        if (force) minShowUntil = now + 900L;
        if (!force && state == 2 && now - checkedAt < 5 * 60_000L) return;
        if (!force && state == 3 && now - checkedAt < 60_000L) return;
        state = 1;
        Thread t = new Thread(() -> {
            try {
                HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + REPO + "/releases?per_page=8"))
                        .timeout(Duration.ofSeconds(10)).header("User-Agent", "FancyDrops").header("Accept", "application/vnd.github+json").GET().build(), HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) throw new IllegalStateException("GitHub said " + res.statusCode());
                JsonArray a = JsonParser.parseString(res.body()).getAsJsonArray();
                int[] best = null; String bestTag = "", bestName = "";
                for (JsonElement e : a) {
                    JsonObject o = e.getAsJsonObject();
                    if (o.has("draft") && o.get("draft").getAsBoolean()) continue;
                    String tag = o.has("tag_name") && !o.get("tag_name").isJsonNull() ? o.get("tag_name").getAsString() : "";
                    int[] v = parse(tag); if (v == null) continue;
                    best = v; bestTag = tag; bestName = best[0] + "." + best[1] + "." + best[2]; break;          // GitHub lists the newest release first: the version was restarted at 0.4.x, so the highest number is not the newest release
                }
                latest = bestTag; latestName = bestName; error = ""; state = best == null ? 3 : 2;
                if (best == null) error = "No releases yet";
            } catch (Throwable e) { error = e.getMessage() == null ? "No connection" : e.getMessage(); state = 3; }
            checkedAt = System.currentTimeMillis();
        }, "FancyDrops-update");
        t.setDaemon(true); t.start();
    }

    private static int[] parse(String s) {
        if (s == null) return null;
        Matcher m = VER.matcher(s); if (!m.find()) return null;
        return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))};
    }
    private static boolean newer(int[] a, int[] b) { for (int i = 0; i < 3; i++) if (a[i] != b[i]) return a[i] > b[i]; return false; }

    /** True when the newest release on GitHub is newer than the version running now. */
    static boolean updateAvailable() {
        if (state != 2) return false;
        int[] cur = parse(FancyDropsMod.VERSION), lat = parse(latest);
        return cur != null && lat != null && newer(lat, cur);
    }
}
