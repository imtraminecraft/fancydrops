package dev.fancydrops;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.CRC32;

/**
 * The animation frames as real pictures, in config/fancydrops/art/animations/.
 * The scenes are drawn by code, so on the first menu open this writes every scene as 320x180 PNG frames (8 per second), one folder per scene
 * (myth_rng_minotaur, myth_normal_sphinx, treasure_rng...). Edit a frame in any paint program and save it: the mod plays YOUR frame in place of the drawn one
 * (a frame you have not touched keeps being drawn by the code, so colours still follow the item). The items, bars and words are drawn on top as usual.
 */
final class AnimFrames {
    static final int FPS = 8, W = 320, H = 180;
    private static final String[] TYPES = {"nodrops", "normal", "rng"};
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "fancydrops-frames"); t.setDaemon(true); t.setPriority(Thread.MIN_PRIORITY); return t; });

    // ---------------------------------------------------------------- the export (a few frames per screen tick, the files are written off-thread)
    private static final class Job { String group, name; int type, kind, frames; }
    private static List<Job> jobs; private static int jobIdx, frameIdx; private static boolean done;
    private static Map<String, Long> manifest;

    static String name(String group, int type, int kind) { return group + "_" + TYPES[Math.max(0, Math.min(2, type))] + (group.equals("myth") ? "_" + Ritual.KINDS[Math.max(0, Math.min(Ritual.KINDS.length - 1, kind))] : ""); }

    /** Call once per rendered menu frame: writes the frames the first time (and again if exported_v1.txt is deleted). */
    static void tick() {
        if (done) return;
        try {
            Path root = DataFiles.art("animations"), marker = root.resolve("exported_v1.txt");
            if (jobs == null) {
                if (Files.exists(marker)) { done = true; return; }
                jobs = new ArrayList<>();
                for (int ty = 0; ty < 3; ty++) {
                    Job j = new Job(); j.group = "treasure"; j.type = ty; j.kind = 0; j.name = name("treasure", ty, 0); jobs.add(j);
                    for (int k = 0; k < Ritual.KINDS.length; k++) { Job m = new Job(); m.group = "myth"; m.type = ty; m.kind = k; m.name = name("myth", ty, k); jobs.add(m); }
                }
                Files.writeString(root.resolve("README.txt"), "ANIMATION FRAMES\n\nEvery scene is written here as 320x180 PNG frames, 8 per second (frame_000.png, frame_001.png ...), one folder per scene:\n  myth_<type>_<creature>   e.g. myth_rng_minotaur\n  treasure_<type>          e.g. treasure_normal\n(types: nodrops, normal, rng)\n\nEdit a frame in any paint program, keep the size and the transparent background, save it, and the mod plays your frame in the scene (no restart).\nFrames you have not changed are still drawn by the mod, so colours keep following the item.\nThe item, the bars and the words are drawn on top, they are not in these pictures.\nDelete a frame's changes by writing the original back, or delete exported_v1.txt to make the mod write every frame again.\n");
            }
            int budget = 3;
            while (budget-- > 0 && jobIdx < jobs.size()) {
                Job j = jobs.get(jobIdx);
                AnimationManager.Anim a = new AnimationManager.Anim(j.group, j.type, AnimationManager.sampleItem()); a.kind = j.kind;
                if (frameIdx == 0) { j.frames = (int) Math.ceil(Math.max(3.6f, a.cfg().secs) * FPS) + 1; manifest = new HashMap<>(); Files.createDirectories(root.resolve(j.name)); }
                int[] px = Ritual.snapshot(a, frameIdx / (float) FPS);
                final Path dir = root.resolve(j.name); final int fi = frameIdx; final Map<String, Long> man = manifest;
                IO.execute(() -> write(dir, fi, px, man));
                frameIdx++;
                if (frameIdx >= j.frames) {
                    final Map<String, Long> man2 = manifest; final boolean last = jobIdx == jobs.size() - 1;
                    IO.execute(() -> { writeManifest(dir, man2); if (last) try { Files.writeString(marker, "The animation frames were written. Delete this file to write them again.\n"); } catch (Exception ignored) {} });
                    jobIdx++; frameIdx = 0;
                }
            }
            if (jobIdx >= jobs.size()) done = true;
        } catch (Throwable t) { t.printStackTrace(); done = true; }
    }

    private static void write(Path dir, int fi, int[] px, Map<String, Long> man) {
        try {
            BufferedImage bi = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB); bi.setRGB(0, 0, W, H, px, 0, W);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(bi, "png", bo);
            byte[] b = bo.toByteArray(); String fn = String.format("frame_%03d.png", fi);
            Files.write(dir.resolve(fn), b);
            CRC32 c = new CRC32(); c.update(b); synchronized (man) { man.put(fn, c.getValue()); }
        } catch (Exception e) { e.printStackTrace(); }
    }
    private static void writeManifest(Path dir, Map<String, Long> man) {
        try { StringBuilder sb = new StringBuilder(); synchronized (man) { for (Map.Entry<String, Long> e : new java.util.TreeMap<>(man).entrySet()) sb.append(e.getKey()).append('=').append(e.getValue()).append('\n'); } Files.writeString(dir.resolve("original.txt"), sb.toString()); } catch (Exception e) { e.printStackTrace(); }
    }

    // ---------------------------------------------------------------- playing edited frames
    private static final class Folder { Path dir; long next, stamp; int n; Map<String, Long> orig = new HashMap<>(); int[][] data; boolean[] tried; }
    private static final Map<String, Folder> CACHE = new HashMap<>();

    /** The edited picture for this moment of the scene (320x180 ARGB), or null to let the code draw it. */
    static int[] override(AnimationManager.Anim a, float t) {
        try {
            if (!done) return null;                                                   // not while the frames are still being written
            String nm = name(a.group, a.type, Math.max(0, a.kind));
            long now = System.currentTimeMillis();
            Folder f = CACHE.computeIfAbsent(nm, k -> { Folder x = new Folder(); x.dir = DataFiles.art("animations").resolve(k); return x; });
            if (now >= f.next) {                                                      // look again once a second: a saved edit shows up at once
                f.next = now + 1000L;
                long st = 0; int n = 0;
                if (Files.isDirectory(f.dir)) try (var s = Files.list(f.dir)) { for (Path p : (Iterable<Path>) s::iterator) { String fn = p.getFileName().toString(); if (fn.startsWith("frame_") && fn.endsWith(".png")) { n++; st += Files.getLastModifiedTime(p).toMillis() + Files.size(p); } } }
                if (st != f.stamp || n != f.n) {
                    f.stamp = st; f.n = n; f.data = new int[Math.max(1, n)][]; f.tried = new boolean[Math.max(1, n)]; f.orig.clear();
                    Path man = f.dir.resolve("original.txt");
                    if (Files.exists(man)) for (String ln : Files.readAllLines(man)) { int q = ln.indexOf('='); if (q > 0) try { f.orig.put(ln.substring(0, q), Long.parseLong(ln.substring(q + 1).trim())); } catch (NumberFormatException ignored) {} }
                }
            }
            if (f.n == 0 || f.orig.isEmpty()) return null;
            int i = Math.max(0, Math.min(f.n - 1, Math.round(t * FPS)));
            if (!f.tried[i]) {
                f.tried[i] = true;
                String fn = String.format("frame_%03d.png", i); Path p = f.dir.resolve(fn);
                if (Files.exists(p)) {
                    byte[] b = Files.readAllBytes(p); CRC32 c = new CRC32(); c.update(b);
                    Long was = f.orig.get(fn);
                    if (was == null || was != c.getValue()) {                         // changed by you
                        BufferedImage bi = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(b));
                        if (bi != null && bi.getWidth() == W && bi.getHeight() == H) { int[] px = new int[W * H]; bi.getRGB(0, 0, W, H, px, 0, W); f.data[i] = px; }
                        else System.out.println("[FancyDrops] " + nm + "/" + fn + " is not 320x180, so it is ignored.");
                    }
                }
            }
            return f.data[i];
        } catch (Throwable ex) { return null; }
    }

    private AnimFrames() {}
}
