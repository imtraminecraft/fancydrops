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
    record P(float x, float y, float z, float size, int argb, int shape) {}

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
        switch (aura == null ? "" : aura) {
            case "stardust": {                                                       // two strands of cyan light wind up the body like a helix, with a glint where they cross
                for (int strand = 0; strand < 2; strand++) for (int i = 0; i < 12; i++) {
                    float ph = (float) (((t / 3200.0) + i / 12.0) % 1.0), ang = (float) (ph * Math.PI * 4 + strand * Math.PI + t / 1500.0), r = 0.52f;
                    float al = (float) Math.sin(Math.PI * ph);
                    o.add(new P((float) Math.cos(ang) * r, ph * 2.0f, (float) Math.sin(ang) * r, 0.045f, argb(0.25f + 0.75f * al, mix(0x4DD6FF, 0xE8FFFF, ph)), 0));
                }
                for (int i = 0; i < 4; i++) {
                    long off = i * 700L, step = (t + off) / 2800L; float ph = ((t + off) % 2800L) / 2800f;
                    float ang = hash(i, step) % 628 / 100f;
                    o.add(new P((float) Math.cos(ang) * 0.6f, 0.2f + ph * 1.6f, (float) Math.sin(ang) * 0.6f, 0.085f * (float) Math.sin(Math.PI * ph), argb(0.95f, 0xE8FFFF), 1));
                }
                break;
            }
            case "sparkleaura": {                                                    // gold and white stars twinkle round you, with a slow ring of light dots at your feet
                for (int i = 0; i < 10; i++) {
                    long off = i * 157L, step = (t + off) / 1600L; float ph = ((t + off) % 1600L) / 1600f;
                    float ang = hash(i, step) % 628 / 100f, r = 0.4f + (hash(i + 50, step) % 40) / 100f, y = 0.15f + (hash(i + 99, step) % 175) / 100f;
                    int col = new int[]{0xFFE680, 0xFFFFFF, 0xFFB3E6, 0xFFD27A}[hash(i, step + 3) % 4];
                    o.add(new P((float) Math.cos(ang) * r, y, (float) Math.sin(ang) * r, 0.09f * (float) Math.sin(Math.PI * ph), argb(0.95f, col), 1));
                }
                for (int i = 0; i < 12; i++) {
                    float ang = (float) (i * Math.PI * 2 / 12 + t / 2200.0);
                    float tw = 0.5f + 0.5f * (float) Math.sin(t / 300.0 + i * 1.7);
                    o.add(new P((float) Math.cos(ang) * 0.62f, 0.04f, (float) Math.sin(ang) * 0.62f, 0.035f, argb(0.25f + 0.6f * tw, 0xFFE9A8), 0));
                }
                for (int i = 0; i < 6; i++) {
                    float ph = (float) (((t / 3000.0) + i / 6.0) % 1.0), ang = i * 1.9f;
                    o.add(new P((float) Math.cos(ang) * 0.5f, ph * 2f, (float) Math.sin(ang) * 0.5f, 0.03f, argb(0.7f * (float) Math.sin(Math.PI * ph), 0xFFF2C0), 0));
                }
                break;
            }
            case "glitchaura": {                                                     // a scan line climbs the body, and torn pixels jump around with a red/cyan split
                float sp = (float) ((t % 1700L) / 1700.0);
                o.add(new P(0f, sp * 2.0f, 0.45f, 0.9f, argb(0.85f * (1f - sp * 0.4f), 0x3DFFB0), 2));
                o.add(new P(0f, Math.max(0f, sp * 2.0f - 0.12f), 0.44f, 0.9f, argb(0.4f, 0xFF3DDC), 2));
                o.add(new P(0f, sp * 2.0f, -0.45f, 0.9f, argb(0.6f * (1f - sp * 0.4f), 0x3DFFB0), 2));
                long step = t / 120L;
                for (int i = 0; i < 10; i++) {
                    long st = step / (1 + i % 3);
                    float ang = hash(i, st) % 628 / 100f, r = 0.42f + (hash(i + 20, st) % 30) / 100f, y = (hash(i + 40, st) % 200) / 100f;
                    float x = (float) Math.cos(ang) * r, z = (float) Math.sin(ang) * r;
                    float sz = 0.04f + (hash(i + 3, st) % 4) / 100f; float al = 0.55f + (hash(i + 9, st) % 45) / 100f;
                    o.add(new P(x - 0.03f, y, z, sz, argb(al, 0x3DFFF0), 0));          // the split: cyan one side, magenta the other
                    o.add(new P(x + 0.03f, y, z, sz, argb(al, 0xFF3DDC), 0));
                    if (hash(i + 77, st) % 5 == 0) o.add(new P(x, y, z, sz * 0.8f, argb(1f, 0xFFFFFF), 0));
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
    private static final Map<UUID, double[]> SMOOTH = new HashMap<>();
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
            for (var p : mc.level.players()) {
                Cosmetics.Look l = looks.get(p.getUUID());
                if (l == null || l.aura() == null) continue;
                if (p != mc.player && (mc.player.distanceTo(p) > 40 || !mc.player.hasLineOfSight(p))) continue;
                double[] sm = SMOOTH.computeIfAbsent(p.getUUID(), k -> new double[]{p.getX(), p.getY(), p.getZ()});
                sm[0] += (p.getX() - sm[0]) * 0.6; sm[1] += (p.getY() - sm[1]) * 0.6; sm[2] += (p.getZ() - sm[2]) * 0.6;
                for (P q : particles(l.aura(), now)) {
                    double dx = sm[0] + q.x() - cx, dy = sm[1] + q.y() - cy, dz = sm[2] + q.z() - cz;
                    double zc = dx * fx + dy * fy + dz * fz;
                    if (zc < 0.25) continue;
                    double xc = dx * rx + dy * ry + dz * rz, yc = dx * ux + dy * uy + dz * uz;
                    float sx = (float) (W / 2.0 * (1 + xc / (zc * th * asp))), sy = (float) (H / 2.0 * (1 - yc / (zc * th)));
                    float px = (float) (q.size() * (H / 2.0) / (zc * th));
                    draw(g, sx, sy, px, q);
                }
            }
            if (SMOOTH.size() > 64) SMOOTH.clear();
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
                int w = Math.max(4, Math.min(60, Math.round(px * 6f)));
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
