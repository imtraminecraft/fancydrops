package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.*;

/**
 * The auras. Every particle is our own: a little square or star that we place and draw ourselves (nothing from Minecraft's particle system).
 * The same recipe draws them in the world (projected onto the screen round each player) and in the menu preview.
 */
final class AuraFx {
    private AuraFx() {}

    /** One particle: x/z in blocks round the player, y up from the feet, size in blocks, colour with alpha, shape 0 = square, 1 = star, 2 = flat slice. */
    record P(float x, float y, float z, float size, int argb, int shape, float lag) {
        P(float x, float y, float z, float size, int argb, int shape) { this(x, y, z, size, argb, shape, 0f); }
    }
    // lag: seconds the particle stays behind in the world while you move (0 = it follows you)

    private static int hash(int a, long b) {
        int h = a * 73856093 ^ (int) b * 19349663; h ^= h >>> 13; h *= 0x5bd1e995; h ^= h >>> 15;
        return Math.abs(h);
    }
    private static int mix(int a, int b, float k) {
        k = Math.max(0f, Math.min(1f, k));
        int r = Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k), g = Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k), bl = Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }
    private static int argb(float alpha, int rgb) { return (Math.max(0, Math.min(255, Math.round(alpha * 255))) << 24) | (rgb & 0xFFFFFF); }

    static List<P> particles(String aura, long t) {
        List<P> o = new ArrayList<>();
        t %= 1_000_000L;                                                         // keeps the angles small: a float can not hold the full clock without losing the turn
        switch (aura == null ? "" : aura) {
            case "stardust": {                                                       // two strands of light wind up the body, a thin halo ring circles above the head, and stars rise off the strands
                for (int strand = 0; strand < 2; strand++) for (int i = 0; i < 14; i++) {
                    float ph = (float) (((t / 3600.0) + i / 14.0) % 1.0), ang = (float) (ph * Math.PI * 3 + strand * Math.PI + t / 1700.0), r = 0.5f + 0.06f * (float) Math.sin(ph * 9);
                    float al = (float) Math.sin(Math.PI * ph);
                    o.add(new P((float) Math.cos(ang) * r, 0.05f + ph * 1.95f, (float) Math.sin(ang) * r, 0.04f + 0.012f * al, argb(0.3f + 0.7f * al, mix(strand == 0 ? 0x4DD6FF : 0x8FA8FF, 0xE8FFFF, ph)), 0, ph * 0.7f));
                }
                for (int i = 0; i < 10; i++) {                                         // the halo
                    float ang = (float) (i * Math.PI * 2 / 10 - t / 1300.0), tw = 0.5f + 0.5f * (float) Math.sin(t / 260.0 + i * 1.3);
                    o.add(new P((float) Math.cos(ang) * 0.32f, 2.08f, (float) Math.sin(ang) * 0.32f, 0.03f, argb(0.35f + 0.6f * tw, 0xBFF0FF), 0));
                }
                for (int i = 0; i < 5; i++) {                                          // stars that rise and fade
                    long off = i * 560L, step = (t + off) / 2800L; float ph = ((t + off) % 2800L) / 2800f;
                    float ang = hash(i, step) % 628 / 100f;
                    o.add(new P((float) Math.cos(ang) * 0.62f, 0.2f + ph * 1.6f, (float) Math.sin(ang) * 0.62f, 0.08f * (float) Math.sin(Math.PI * ph), argb(0.95f, 0xE8FFFF), 1, ph * 0.9f));
                }
                break;
            }
            case "sparkleaura": {                                                    // two slow rings of stars (one gold, one pink) turn around you; bright bursts pop up now and then; light dust rises
                for (int ring = 0; ring < 2; ring++) for (int i = 0; i < 6; i++) {
                    float ang = (float) (i * Math.PI * 2 / 6 + (ring == 0 ? 1 : -1) * t / 2300.0 + ring * 0.5), y = ring == 0 ? 0.55f + 0.1f * (float) Math.sin(t / 700.0 + i) : 1.35f + 0.1f * (float) Math.sin(t / 800.0 + i * 2);
                    float tw = 0.5f + 0.5f * (float) Math.sin(t / 240.0 + i * 1.9 + ring * 2);
                    o.add(new P((float) Math.cos(ang) * 0.6f, y, (float) Math.sin(ang) * 0.6f, 0.035f + 0.04f * tw, argb(0.45f + 0.5f * tw, ring == 0 ? 0xFFE680 : 0xFFB3E6), 1));
                }
                for (int i = 0; i < 5; i++) {                                          // bursts: a star that swells and dies
                    long off = i * 330L, step = (t + off) / 1500L; float ph = ((t + off) % 1500L) / 1500f;
                    float ang = hash(i, step) % 628 / 100f, r = 0.35f + (hash(i + 50, step) % 45) / 100f, y = 0.1f + (hash(i + 99, step) % 180) / 100f;
                    o.add(new P((float) Math.cos(ang) * r, y, (float) Math.sin(ang) * r, 0.13f * (float) Math.sin(Math.PI * ph), argb(1f, 0xFFFFFF), 1, ph * 0.8f));
                }
                for (int i = 0; i < 12; i++) {                                         // a ring of light at your feet
                    float ang = (float) (i * Math.PI * 2 / 12 + t / 2200.0), tw = 0.5f + 0.5f * (float) Math.sin(t / 300.0 + i * 1.7);
                    o.add(new P((float) Math.cos(ang) * 0.62f, 0.04f, (float) Math.sin(ang) * 0.62f, 0.035f, argb(0.25f + 0.6f * tw, 0xFFE9A8), 0));
                }
                for (int i = 0; i < 8; i++) {
                    float ph = (float) (((t / 3000.0) + i / 8.0) % 1.0), ang = i * 1.9f;
                    o.add(new P((float) Math.cos(ang) * 0.5f, ph * 2f, (float) Math.sin(ang) * 0.5f, 0.03f, argb(0.7f * (float) Math.sin(Math.PI * ph), 0xFFF2C0), 0, ph * 0.9f));
                }
                break;
            }
            case "glitchaura": {                                                     // Alarm: red and amber sparks circle you, a thin ring of amber light climbs from your feet to your head and makes the sparks flare white as it passes
                float ringY = ((t % 2800L) / 2800f) * 2.3f - 0.1f;
                for (int i = 0; i < 10; i++) {
                    float ang = (float) (i * Math.PI * 2 / 10 + t / 1500.0), y = 0.25f + 0.17f * i + 0.1f * (float) Math.sin(t / 600.0 + i);
                    boolean hit = Math.abs(y - ringY) < 0.14f;
                    o.add(new P((float) Math.cos(ang) * 0.58f, y, (float) Math.sin(ang) * 0.58f, hit ? 0.075f : 0.045f, hit ? argb(1f, 0xFFFFFF) : argb(0.9f, i % 2 == 0 ? 0xFF4A2E : 0xFFB02E), hit ? 1 : 0, 0.35f));
                }
                if (ringY > 0f && ringY < 2.1f) for (int i = 0; i < 16; i++) {
                    float ang = (float) (i * Math.PI * 2 / 16);
                    o.add(new P((float) Math.cos(ang) * 0.6f, ringY, (float) Math.sin(ang) * 0.6f, 0.03f, argb(0.75f * (1f - ringY / 2.3f), 0xFFC03D), 0));
                }
                for (int i = 0; i < 6; i++) {                                         // slow red embers lifting off you
                    float ph = (float) (((t / 3400.0) + i / 6.0) % 1.0), ang = i * 1.7f;
                    o.add(new P((float) Math.cos(ang) * 0.5f, ph * 2f, (float) Math.sin(ang) * 0.5f, 0.032f, argb(0.7f * (float) Math.sin(Math.PI * ph), 0xFF5A36), 0, ph * 0.8f));
                }
                break;
            }
            case "patronaura": {                                                     // gold embers drift slowly up, now and then a glint
                for (int i = 0; i < 16; i++) {
                    float ph = (float) (((t / 3800.0) + i / 16.0) % 1.0), ang = (float) (i * 2.1 + Math.sin(t / 1300.0 + i) * 0.6), r = 0.45f + 0.2f * (float) Math.sin(i * 1.7 + t / 900.0);
                    float al = (float) Math.sin(Math.PI * ph);
                    o.add(new P((float) Math.cos(ang) * r, ph * 2.1f, (float) Math.sin(ang) * r, 0.04f * (1f - 0.5f * ph) + 0.018f, argb(al, mix(0xFFD24A, 0xFF7A1E, ph)), i % 6 == 0 ? 1 : 0));
                }
                break;
            }
            default: break;
        }
        return o;
    }

    // ------------------------------------------------------------------ in the world: our particles projected onto the screen
    private static final Map<UUID, ArrayDeque<double[]>> HIST = new HashMap<>();
    /** The player's position at time t (ms), from the recorded history. */
    private static double[] at(ArrayDeque<double[]> h, long t) {
        double[] prev = null;
        for (double[] e : h) {
            if (e[0] >= t) { if (prev == null) return new double[]{e[1], e[2], e[3]}; double k = (t - prev[0]) / Math.max(1.0, e[0] - prev[0]); return new double[]{prev[1] + (e[1] - prev[1]) * k, prev[2] + (e[2] - prev[2]) * k, prev[3] + (e[3] - prev[3]) * k}; }
            prev = e;
        }
        return new double[]{prev[1], prev[2], prev[3]};
    }
    private static Object call(Object o, String... names) {
        if (o == null) return null;
        for (String n : names) { try { var m = o.getClass().getMethod(n); m.setAccessible(true); return m.invoke(o); } catch (Throwable ignored) { } }
        return null;
    }
    private static double fieldD(Object o, String f) { try { var fl = o.getClass().getField(f); return fl.getDouble(o); } catch (Throwable t) { Object r = call(o, f); return r instanceof Number nn ? nn.doubleValue() : 0; } }

    private static java.lang.reflect.Field camPos, camX, camY; private static Object camObj; private static boolean camTried;
    /** The camera the world is drawn from: x, y, z, yaw, pitch. Found by field shape; if that fails, worked out from the player and the F5 view. */
    private static double[] camera(Minecraft mc) {
        try {
            if (!camTried) {
                camTried = true;
                for (var f : mc.gameRenderer.getClass().getDeclaredFields()) {
                    if (!f.getType().getSimpleName().equals("Camera")) continue;
                    f.setAccessible(true); camObj = f.get(mc.gameRenderer);
                    for (var cf : f.getType().getDeclaredFields()) {
                        cf.setAccessible(true);
                        if (cf.getType().getSimpleName().equals("Vec3") && camPos == null) camPos = cf;
                        else if (cf.getType() == float.class && cf.getName().toLowerCase().startsWith("x") && camX == null) camX = cf;
                        else if (cf.getType() == float.class && cf.getName().toLowerCase().startsWith("y") && camY == null) camY = cf;
                    }
                    break;
                }
                System.err.println("[FancyDrops] aura camera: " + (camObj != null && camPos != null && camX != null && camY != null ? "found" : "not found, using the player view"));
            }
            if (camObj != null && camPos != null && camX != null && camY != null) {
                Object v = camPos.get(camObj);
                if (v != null) return new double[]{fieldD(v, "x"), fieldD(v, "y"), fieldD(v, "z"), camY.getFloat(camObj), camX.getFloat(camObj)};
            }
        } catch (Throwable ignored) { }
        double x = mc.player.getX(), y = mc.player.getEyeY(), z = mc.player.getZ(); float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
        try {                                                                                    // F5: the camera floats 4 blocks behind (or in front of) the player
            Object ct = call(mc.options, "getCameraType");
            boolean first = ct == null || Boolean.TRUE.equals(call(ct, "isFirstPerson"));
            if (!first) {
                boolean front = Boolean.TRUE.equals(call(ct, "isMirrored"));
                double yr = Math.toRadians(yaw), xr = Math.toRadians(pitch), d = front ? -4.0 : 4.0;
                x += Math.sin(yr) * Math.cos(xr) * d; y += Math.sin(xr) * d; z -= Math.cos(yr) * Math.cos(xr) * d;
                if (front) { yaw += 180f; pitch = -pitch; }
            }
        } catch (Throwable ignored) { }
        return new double[]{x, y, z, yaw, pitch};
    }

    /** Called every frame by the HUD: draws the aura particles round every player who wears one. */
    static void render(GuiGraphicsExtractor g) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            Map<UUID, Cosmetics.Look> looks = Cosmetics.looks();
            if (looks.isEmpty()) return;
            double[] cam = camera(mc);
            double cx = cam[0], cy = cam[1], cz = cam[2]; float yaw = (float) cam[3], pitch = (float) cam[4];
            double yr2 = Math.toRadians(yaw), xr2 = Math.toRadians(pitch);
            double fx = -Math.sin(yr2) * Math.cos(xr2), fy = -Math.sin(xr2), fz = Math.cos(yr2) * Math.cos(xr2);          // forward
            double rx = -Math.cos(yr2), ry = 0, rz = -Math.sin(yr2);                                                      // right
            double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;                                // up = right x forward
            int W = mc.getWindow().getGuiScaledWidth(), H = mc.getWindow().getGuiScaledHeight();
            double fov = Math.toRadians(mc.options.fov().get()), th = Math.tan(fov / 2), asp = W / (double) H;
            long now = System.currentTimeMillis();
            float pt = 1f; try { pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false); } catch (Throwable ignored) { }
            for (var p : mc.level.players()) {
                Cosmetics.Look l = looks.get(p.getUUID());
                if (l == null || l.aura() == null) continue;
                if (p != mc.player && (mc.player.distanceTo(p) > 40 || !mc.player.hasLineOfSight(p))) continue;
                double ox = fieldD(p, "xOld"), oy = fieldD(p, "yOld"), oz = fieldD(p, "zOld");           // where the player is drawn this frame: between the last tick and this one
                if (ox == 0 && oy == 0 && oz == 0) { ox = p.getX(); oy = p.getY(); oz = p.getZ(); }
                double ex = ox + (p.getX() - ox) * pt, ey = oy + (p.getY() - oy) * pt, ez = oz + (p.getZ() - oz) * pt;
                ArrayDeque<double[]> hist = HIST.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());       // the last two seconds of where the player was
                if (hist.isEmpty() || now - (long) hist.peekLast()[0] >= 8) hist.addLast(new double[]{now, ex, ey, ez});
                while (hist.size() > 1 && now - (long) hist.peekFirst()[0] > 2000) hist.pollFirst();
                for (P q : particles(l.aura(), now)) {
                    double bx = ex, by = ey, bz = ez;
                    if (q.lag() > 0f && hist.size() > 1) {                                                   // it was let go a moment ago: it stays where you were then
                        double[] at = at(hist, now - (long) (q.lag() * 1000f)); bx = at[0]; by = at[1]; bz = at[2];
                    }
                    double dx = bx + q.x() - cx, dy = by + q.y() - cy, dz = bz + q.z() - cz;
                    double zc = dx * fx + dy * fy + dz * fz;
                    if (zc < 0.25) continue;
                    double xc = dx * rx + dy * ry + dz * rz, yc = dx * ux + dy * uy + dz * uz;
                    float sx = (float) (W / 2.0 * (1 + xc / (zc * th * asp))), sy = (float) (H / 2.0 * (1 - yc / (zc * th)));
                    float px = (float) (q.size() * (H / 2.0) / (zc * th));
                    draw(g, sx, sy, px, q);
                }
            }
            if (HIST.size() > 64) HIST.clear();
        } catch (Throwable ignored) { }
    }

    /** One particle at a screen position, sx/sy its centre and px its size in GUI pixels. Small and soft: a core with a faint halo. */
    static void draw(GuiGraphicsExtractor g, float sx, float sy, float px, P q) {
        int a = q.argb(), al = a >>> 24, rgb = a & 0xFFFFFF; if (al < 6) return;
        px = Math.max(1f, Math.min(px, 7f));
        int x = Math.round(sx), y = Math.round(sy);
        switch (q.shape()) {
            case 1: {                                                                  // a four-point star: thin arms that fade out, a bright centre
                int arm = Math.max(2, Math.round(px * 1.3f));
                for (int d = 1; d <= arm; d++) {
                    int ad = Math.round(al * (1f - d / (arm + 1f)));
                    int c = (ad << 24) | rgb;
                    g.fill(x + d, y, x + d + 1, y + 1, c); g.fill(x - d, y, x - d + 1, y + 1, c); g.fill(x, y + d, x + 1, y + d + 1, c); g.fill(x, y - d, x + 1, y - d + 1, c);
                }
                g.fill(x - 1, y - 1, x + 2, y + 2, ((al / 3) << 24) | rgb);
                g.fill(x, y, x + 1, y + 1, (Math.min(255, al + 60) << 24) | 0xFFFFFF);
                break;
            }
            case 2: {                                                                  // a flat slice of light
                int w = Math.max(4, Math.min(18, Math.round(px * 3f)));
                g.fill(x - w / 2, y, x + w / 2, y + 1, a);
                g.fill(x - w / 2, y - 1, x + w / 2, y, ((al / 4) << 24) | rgb); g.fill(x - w / 2, y + 1, x + w / 2, y + 2, ((al / 4) << 24) | rgb);
                break;
            }
            default: {
                int s = Math.max(1, Math.round(px));
                if (s >= 2) g.fill(x - s / 2 - 1, y - s / 2 - 1, x - s / 2 + s + 1, y - s / 2 + s + 1, ((al / 4) << 24) | rgb);
                g.fill(x - s / 2, y - s / 2, x - s / 2 + s, y - s / 2 + s, a);
            }
        }
    }
}
