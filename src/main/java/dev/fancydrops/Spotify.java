package dev.fancydrops;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Spotify: log in once (in your browser, with your own Spotify app's Client ID), then the mod asks Spotify every few seconds what is playing
 * and how far into it you are. Only what the Lyrics need: the track, its artists, album, length and position.
 *
 * Login is the PKCE flow, so no secret is ever stored: the browser comes back to http://127.0.0.1:43821/callback (add that exact address
 * to your Spotify app's Redirect URIs), we swap the code for tokens, and keep the refresh token in config/fancydrops.json.
 */
final class Spotify {
    private Spotify() {}

    static final int PORT = 43821;
    static final String REDIRECT = "http://127.0.0.1:" + PORT + "/callback";
    private static final String SCOPES = "user-read-currently-playing user-read-playback-state";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(8)).build();

    /** What is playing right now. */
    record Track(String id, String name, String artists, String album, long durationMs) {}
    static volatile Track track;
    static volatile boolean playing;
    private static volatile long progressMs, progressAt;                 // the position Spotify gave us, and when
    static volatile String status = "";
    private static volatile String access = ""; private static volatile long expires;
    private static volatile boolean busy;
    private static long lastPoll;

    static boolean linked() { return !FancyConfig.get().spotifyRefresh.isBlank(); }
    /** Where the song is now, in ms (moves on between polls while it plays). */
    static long position() { long p = progressMs + (playing ? System.currentTimeMillis() - progressAt : 0); return track == null ? p : Math.min(p, track.durationMs()); }

    // ------------------------------------------------------------------ login
    static void login(java.util.function.Consumer<String> say) {
        String id = FancyConfig.get().spotifyClient.trim();
        if (id.isEmpty()) { say.accept("Paste your Spotify app's Client ID first."); return; }
        if (busy) { say.accept("Already waiting for Spotify in your browser."); return; }
        busy = true; status = "Waiting for you in the browser...";
        Thread t = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(PORT, 1, InetAddress.getByName("127.0.0.1"))) {
                server.setSoTimeout(180_000);
                String verifier = random(64), state = random(16);
                String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
                String url = "https://accounts.spotify.com/authorize?client_id=" + enc(id) + "&response_type=code&redirect_uri=" + enc(REDIRECT)
                    + "&code_challenge_method=S256&code_challenge=" + challenge + "&state=" + state + "&scope=" + enc(SCOPES);
                net.minecraft.util.Util.getPlatform().openUri(URI.create(url));
                String code = null, err = null;
                while (code == null && err == null) {
                    try (Socket s = server.accept()) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                        String first = in.readLine(); if (first == null) continue;
                        String path = first.split(" ").length > 1 ? first.split(" ")[1] : "";
                        if (!path.startsWith("/callback")) { reply(s, 404, "Not here."); continue; }
                        String q = path.contains("?") ? path.substring(path.indexOf('?') + 1) : "";
                        String gotState = param(q, "state"); code = param(q, "code"); err = param(q, "error");
                        if (code != null && !state.equals(gotState)) { code = null; err = "the login did not match (try again)"; }
                        reply(s, 200, code != null ? "<h2>FancyDrops is linked to Spotify.</h2><p>You can close this tab and go back to Minecraft.</p>" : "<h2>Spotify said no: " + err + "</h2>");
                    }
                }
                if (code == null) throw new IOException(err);
                JsonObject tok = post("grant_type=authorization_code&code=" + enc(code) + "&redirect_uri=" + enc(REDIRECT) + "&client_id=" + enc(id) + "&code_verifier=" + verifier);
                keep(tok);
                status = "Linked."; say.accept("Spotify is linked. Lyrics will show while a song plays.");
            } catch (SocketTimeoutException e) { status = "The login timed out."; say.accept("Spotify login timed out. Try again."); }
            catch (BindException e) { status = "Port " + PORT + " is busy."; say.accept("Couldn't listen on port " + PORT + ". Close whatever uses it and try again."); }
            catch (Throwable e) { status = "Login failed: " + e.getMessage(); say.accept("Spotify login failed: " + e.getMessage()); }
            finally { busy = false; }
        }, "FancyDrops Spotify login");
        t.setDaemon(true); t.start();
    }
    static void logout() { FancyConfig c = FancyConfig.get(); c.spotifyRefresh = ""; FancyConfig.save(); access = ""; expires = 0; track = null; playing = false; status = "Not linked."; }

    private static void keep(JsonObject tok) {
        if (!tok.has("access_token")) throw new IllegalStateException(tok.has("error_description") ? tok.get("error_description").getAsString() : "no token");
        access = tok.get("access_token").getAsString(); expires = System.currentTimeMillis() + (tok.has("expires_in") ? tok.get("expires_in").getAsLong() : 3600) * 1000L - 30_000L;
        if (tok.has("refresh_token")) { FancyConfig.get().spotifyRefresh = tok.get("refresh_token").getAsString(); FancyConfig.save(); }
    }
    private static boolean fresh() {
        if (!access.isEmpty() && System.currentTimeMillis() < expires) return true;
        FancyConfig c = FancyConfig.get(); if (c.spotifyRefresh.isBlank() || c.spotifyClient.isBlank()) return false;
        try { keep(post("grant_type=refresh_token&refresh_token=" + enc(c.spotifyRefresh) + "&client_id=" + enc(c.spotifyClient.trim()))); return true; }
        catch (Throwable e) { status = "Couldn't refresh the login: " + e.getMessage(); return false; }
    }

    // ------------------------------------------------------------------ what is playing (asked every 3 s from a worker thread, only while lyrics are on)
    static void tick() {
        FancyConfig c = FancyConfig.get();
        if (!c.lyricsOn || !linked() || busy) return;
        long now = System.currentTimeMillis(); if (now - lastPoll < 3000) return; lastPoll = now;
        Thread t = new Thread(Spotify::poll, "FancyDrops Spotify"); t.setDaemon(true); t.start();
    }
    private static void poll() {
        try {
            if (!fresh()) return;
            HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/currently-playing")).header("Authorization", "Bearer " + access).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 204 || r.body().isBlank()) { playing = false; status = "Nothing playing."; return; }
            if (r.statusCode() == 401) { access = ""; return; }
            if (r.statusCode() != 200) { status = "Spotify answered " + r.statusCode(); return; }
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            if (!o.has("item") || o.get("item").isJsonNull()) { playing = false; return; }
            JsonObject it = o.getAsJsonObject("item");
            StringBuilder ar = new StringBuilder();
            if (it.has("artists")) for (var a : it.getAsJsonArray("artists")) { if (ar.length() > 0) ar.append(", "); ar.append(a.getAsJsonObject().get("name").getAsString()); }
            String album = it.has("album") && it.get("album").isJsonObject() ? it.getAsJsonObject("album").get("name").getAsString() : "";
            Track tr = new Track(it.has("id") && !it.get("id").isJsonNull() ? it.get("id").getAsString() : it.get("name").getAsString(), it.get("name").getAsString(), ar.toString(), album, it.has("duration_ms") ? it.get("duration_ms").getAsLong() : 0);
            long prog = o.has("progress_ms") && !o.get("progress_ms").isJsonNull() ? o.get("progress_ms").getAsLong() : 0;
            boolean pl = o.has("is_playing") && o.get("is_playing").getAsBoolean();
            progressMs = prog; progressAt = System.currentTimeMillis(); playing = pl;
            if (track == null || !track.id().equals(tr.id())) { track = tr; Lyrics.load(tr); }
            status = (pl ? "Playing: " : "Paused: ") + tr.name() + " - " + tr.artists();
        } catch (Throwable e) { status = "Couldn't reach Spotify."; }
    }

    // ------------------------------------------------------------------ bits
    private static JsonObject post(String form) throws Exception {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token")).header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(r.body()).getAsJsonObject();
    }
    private static void reply(Socket s, int code, String html) throws IOException {
        byte[] b = ("<!doctype html><meta charset=utf-8><title>FancyDrops</title><body style='font-family:sans-serif;background:#111;color:#eee;padding:40px'>" + html).getBytes(StandardCharsets.UTF_8);
        OutputStream o = s.getOutputStream();
        o.write(("HTTP/1.1 " + code + " OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: " + b.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII)); o.write(b); o.flush();
    }
    private static String param(String q, String k) {
        for (String kv : q.split("&")) { int e = kv.indexOf('='); if (e > 0 && kv.substring(0, e).equals(k)) return URLDecoder.decode(kv.substring(e + 1), StandardCharsets.UTF_8); }
        return null;
    }
    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String random(int n) {
        String ab = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"; SecureRandom r = new SecureRandom(); StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(ab.charAt(r.nextInt(ab.length())));
        return b.toString();
    }
}
