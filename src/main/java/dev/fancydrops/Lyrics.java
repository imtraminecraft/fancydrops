package dev.fancydrops;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Time-synced lyrics for the song Spotify is playing, from LRCLIB (lrclib.net, a free public lyrics library). Nothing is stored in the mod:
 * each song's lines are fetched when it starts and kept in memory for the session.
 */
final class Lyrics {
    private Lyrics() {}

    record Line(long atMs, String text) {}
    private static final Map<String, List<Line>> CACHE = new HashMap<>();
    static volatile List<Line> lines = List.of();
    static volatile String forTrack = "", status = "";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Pattern TAG = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]");

    static void load(Spotify.Track t) {
        forTrack = t.id(); lines = List.of();
        synchronized (CACHE) { if (CACHE.containsKey(t.id())) { lines = CACHE.get(t.id()); status = lines.isEmpty() ? "No synced lyrics for this song." : ""; return; } }
        status = "Looking for the lyrics...";
        Thread th = new Thread(() -> {
            List<Line> got = List.of();
            try {
                String q = "track_name=" + Spotify.enc(t.name()) + "&artist_name=" + Spotify.enc(firstArtist(t.artists())) + "&album_name=" + Spotify.enc(t.album()) + "&duration=" + Math.round(t.durationMs() / 1000.0);
                JsonElement e = get("https://lrclib.net/api/get?" + q);
                if (e != null && e.isJsonObject()) got = parse(e.getAsJsonObject());
                if (got.isEmpty()) {                                                          // not found exactly: search, and take the closest length that has synced lines
                    JsonElement s = get("https://lrclib.net/api/search?track_name=" + Spotify.enc(t.name()) + "&artist_name=" + Spotify.enc(firstArtist(t.artists())));
                    if (s != null && s.isJsonArray()) {
                        JsonArray arr = s.getAsJsonArray(); double best = Double.MAX_VALUE;
                        for (JsonElement x : arr) {
                            JsonObject o = x.getAsJsonObject(); List<Line> l = parse(o); if (l.isEmpty()) continue;
                            double d = Math.abs((o.has("duration") && !o.get("duration").isJsonNull() ? o.get("duration").getAsDouble() : 0) - t.durationMs() / 1000.0);
                            if (d < best) { best = d; got = l; }
                        }
                    }
                }
            } catch (Throwable ignored) { }
            synchronized (CACHE) { CACHE.put(t.id(), got); }
            if (forTrack.equals(t.id())) { lines = got; status = got.isEmpty() ? "No synced lyrics for this song." : ""; }
        }, "FancyDrops lyrics");
        th.setDaemon(true); th.start();
    }
    private static String firstArtist(String a) { int c = a.indexOf(','); return c < 0 ? a : a.substring(0, c); }
    private static JsonElement get(String url) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "FancyDrops (github.com/imtraminecraft/fancydrops)").GET().build(), HttpResponse.BodyHandlers.ofString());
        return r.statusCode() == 200 ? JsonParser.parseString(r.body()) : null;
    }
    /** "[01:02.34] some words" lines, in time order; empty lines become pauses. */
    static List<Line> parse(JsonObject o) {
        if (!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) return List.of();
        List<Line> out = new ArrayList<>();
        for (String raw : o.get("syncedLyrics").getAsString().split("\\R")) {
            Matcher m = TAG.matcher(raw); List<Long> at = new ArrayList<>(); int end = 0;
            while (m.find()) { at.add(Long.parseLong(m.group(1)) * 60000L + Math.round(Double.parseDouble(m.group(2)) * 1000)); end = m.end(); }
            String text = raw.substring(end).trim();
            for (long a : at) out.add(new Line(a, text));
        }
        out.sort(Comparator.comparingLong(Line::atMs));
        return out;
    }
    /** The line sung at ms (-1 before the first). */
    static int at(long ms) { List<Line> l = lines; int i = -1; for (int k = 0; k < l.size(); k++) { if (l.get(k).atMs() <= ms) i = k; else break; } return i; }
}
