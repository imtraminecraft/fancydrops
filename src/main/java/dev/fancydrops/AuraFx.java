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
            case "stardust": {                                                       // cool cyan motes spiral up the body and fade away at the top
                for (int i = 0; i < 22; i++) {
                    float ph = (float) (((t / 2800.0) + i / 22.0) % 1.0), ang = (float) (i * 2.399 + t / 850.0), r = 0.5f + 0.12f * (float) Math.sin(i + t / 500.0);
                    float al = (float) Math.pow(Math.sin(Math.PI * ph), 0.7);
                    int col = mix(0x66E0FF, 0xFFFFFF, 0.5f + 0.5f * (float) Math.sin(i * 1.3 + t / 400.0));
                    o.add(new P((float) Math.cos(ang) * r, ph * 2.0f, (float) Math.sin(ang) * r, 0.05f + 0.035f * al, argb(al, col), i % 4 == 0 ? 1 : 0));
                }
                break;
            }
            case "sparkleaura": {                                                    // twinkling four-point stars that pop up round you and fade
                for (int i = 0; i < 14; i++) {
                    long off = i * 107L, step = (t + off) / 1500L; float ph = ((t + off) % 1500L) / 1500f;
                    float ang = hash(i, step) % 628 / 100f, r = 0.35f + (hash(i + 50, step) % 45) / 100f, y = 0.1f + (hash(i + 99, step) % 185) / 100f;
                    float sz = 0.16f * (float) Math.sin(Math.PI * ph);
                    int col = new int[]{0xFFE680, 0xFFFFFF, 0xFFB3E6, 0xBFE8FF}[hash(i, step + 3) % 4];
                    o.add(new P((float) Math.cos(ang) * r, y, (float) Math.sin(ang) * r, sz, argb(0.35f + 0.65f * (float) Math.sin(Math.PI * ph), col), 1));
                }
                break;
            }
            case "glitchaura": {                                                     // torn pixels of magenta, cyan and green jumping round you, and now and then a flat slice of light
                long step = t / 110L;
                for (int i = 0; i < 18; i++) {
                    long st = step / (1 + i % 2);
                    float ang = hash(i, st) % 628 / 100f, r = 0.4f + (hash(i + 20, st) % 40) / 100f, y = (hash(i + 40, st) % 200) / 100f;
                    int col = new int[]{0xFF3DDC, 0x3DFFF0, 0x6BFF6B, 0xFFFFFF}[hash(i + 7, st) % 4];
                    o.add(new P((float) Math.cos(ang) * r, y, (float) Math.sin(ang) * r, 0.04f + (hash(i + 3, st) % 6) / 100f, argb(0.55f + (hash(i + 9, st) % 45) / 100f, col), 0));
                }
                if (hash(1, step) % 3 == 0) o.add(new P(0f, (hash(2, step) % 190) / 100f, 0.5f, 0.55f, argb(0.55f, 0x55FFFF), 2));
                if (hash(5, step) % 4 == 0) o.add(new P(0f, (hash(6, step) % 190) / 100f, -0.5f, 0.45f, argb(0.5f, 0xFF55FF), 2));
                break;
            }
            case "patronaura": {                                                     // gold embers drift slowly up, now and then a glint
                for (int i = 0; i < 18; i++) {
                    float ph = (float) (((t / 3800.0) + i / 18.0) % 1.0), ang = (float) (i * 2.1 + Math.sin(t / 1300.0 + i) * 0.6), r = 0.45f + 0.2f * (float) Math.sin(i * 1.7 + t / 900.0);
                    float al = (float) Math.sin(Math.PI * ph);
                    o.add(new P((float) Math.cos(ang) * r, ph * 2.1f, (float) Math.sin(ang) * r, 0.045f * (1f - 0.5f * ph) + 0.02f, argb(al, mix(0xFFD24A, 0xFF7A1E, ph)), i % 6 == 0 ? 1 : 0));
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

    /** Called every frame by the HUD: draws the aura particles round every player who wears one. */
    static void render(GuiGraphicsExtractor g) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            Map<UUID, Cosmetics.Look> looks = Cosmetics.looks();
            if (looks.isEmpty()) return;
            double cx, cy, cz; float yaw, pitch;
            Object cam = call(mc.gameRenderer, "getMainCamera");
            Object pos = call(cam, "position", "getPosition");
            Object yr = call(cam, "yRot", "getYRot"), xr = call(cam, "xRot", "getXRot");
            if (pos != null && yr instanceof Number yn && xr instanceof Number xn) { cx = fieldD(pos, "x"); cy = fieldD(pos, "y"); cz = fieldD(pos, "z"); yaw = yn.floatValue(); pitch = xn.floatValue(); }
            else { cx = mc.player.getX(); cy = mc.player.getEyeY(); cz = mc.player.getZ(); yaw = mc.player.getYRot(); pitch = mc.player.getXRot(); }
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

    /** One particle at a screen position, sx/sy its centre and px its size in GUI pixels. */
    static void draw(GuiGraphicsExtractor g, float sx, float sy, float px, P q) {
        int a = q.argb(); if ((a >>> 24) < 4) return;
        int s = Math.max(1, Math.round(px));
        int x = Math.round(sx), y = Math.round(sy);
        switch (q.shape()) {
            case 1: {                                                                  // a four-point star
                int arm = Math.max(1, Math.round(px * 1.4f)), t = Math.max(1, s / 3);
                g.fill(x - arm, y - t / 2, x + arm + 1, y - t / 2 + t, a); g.fill(x - t / 2, y - arm, x - t / 2 + t, y + arm + 1, a);
                int core = ((Math.min(255, (a >>> 24) + 60)) << 24) | 0xFFFFFF;
                g.fill(x - t / 2, y - t / 2, x - t / 2 + t, y - t / 2 + t, core);
                break;
            }
            case 2: {                                                                  // a flat slice of light
                int w = Math.max(3, Math.round(px)), h = Math.max(1, s / 6);
                g.fill(x - w / 2, y, x + w / 2, y + h, a);
                break;
            }
            default: g.fill(x - s / 2, y - s / 2, x - s / 2 + s, y - s / 2 + s, a);
        }
    }
}
