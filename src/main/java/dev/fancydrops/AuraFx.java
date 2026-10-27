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
                for (int i = 0; i < 5; i++) {                                          // stars that rise and fade
                    long off = i * 560L, step = (t + off) / 2800L; float ph = ((t + off) % 2800L) / 2800f;
                    float ang = hash(i, step) % 628 / 100f;
                    o.add(new P((float) Math.cos(ang) * 0.62f, 0.2f + ph * 1.6f, (float) Math.sin(ang) * 0.62f, 0.08f * (float) Math.sin(Math.PI * ph), argb(0.95f, 0xE8FFFF), 1, ph * 0.9f));
                }
                break;
            }
            case "sparkleaura": {                                                    // two slow rings of stars (one gold, one pink) turn around you; bright bursts pop up now and then; light dust rises
                for (int i = 0; i < 10; i++) {                                         // stars twinkle here and there round you, never in a ring
                    long step = (t + i * 410L) / 2100L; float ph = ((t + i * 410L) % 2100L) / 2100f, tw = (float) Math.sin(Math.PI * ph);
                    float ang = hash(i + 7, step) % 628 / 100f, r = 0.35f + (hash(i + 31, step) % 40) / 100f, y = 0.15f + (hash(i + 63, step) % 185) / 100f;
                    o.add(new P((float) Math.cos(ang) * r, y, (float) Math.sin(ang) * r, 0.035f + 0.04f * tw, argb(0.35f + 0.6f * tw, i % 2 == 0 ? 0xFFE680 : 0xFFB3E6), 1));
                }
                for (int i = 0; i < 5; i++) {                                          // bursts: a star that swells and dies
                    long off = i * 330L, step = (t + off) / 1500L; float ph = ((t + off) % 1500L) / 1500f;
                    float ang = hash(i, step) % 628 / 100f, r = 0.35f + (hash(i + 50, step) % 45) / 100f, y = 0.1f + (hash(i + 99, step) % 180) / 100f;
                    o.add(new P((float) Math.cos(ang) * r, y, (float) Math.sin(ang) * r, 0.13f * (float) Math.sin(Math.PI * ph), argb(1f, 0xFFFFFF), 1, ph * 0.8f));
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
                if (ringY > 0f && ringY < 2.1f) o.add(new P(0f, ringY, 0f, 0.05f, argb(0.75f * (1f - ringY / 2.3f), 0xFFC03D), 2));          // a thin slice of amber light climbs, not a ring
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
    private static final Map<UUID, double[]> STATE = new HashMap<>();
    private static final Map<UUID, ArrayDeque<double[]>> HIST = new HashMap<>();
    private static int auraRgb(String aura) {
        return switch (aura == null ? "" : aura) { case "glitchaura" -> 0xFF7A2E; case "stardust" -> 0x8FD0FF; case "patronaura" -> 0xFF9B2E; default -> 0xFFE680; };
    }
    /** The resting animation (after 5 seconds without moving): two strands of light slowly wind up from the feet past the head, and a soft ring breathes round the waist. */
    static List<P> idleParticles(String aura, long now, float k) {
        List<P> o = new ArrayList<>(); int col = auraRgb(aura), pale = mix(col, 0xFFFFFF, 0.55f);
        float u = (now % 6000L) / 6000f;
        for (int strand = 0; strand < 2; strand++) for (int i = 0; i < 9; i++) {
            float ph = (u + i / 9f) % 1f, al = (float) Math.sin(ph * Math.PI) * k;
            double an = ph * Math.PI * 3.0 + strand * Math.PI + (now % 9000L) / 9000.0 * Math.PI * 2;
            float r = 0.55f + 0.08f * (float) Math.sin(ph * 9 + strand);
            o.add(new P((float) Math.cos(an) * r, ph * 2.2f, (float) Math.sin(an) * r, 0.045f, argb(al * 0.95f, i % 3 == 0 ? pale : col), i % 2 == 0 ? 1 : 0));
        }
        float br = 0.5f + 0.5f * (float) Math.sin(now / 900.0);
        for (int i = 0; i < 12; i++) {
            double an = i * Math.PI / 6 + (now % 12000L) / 12000.0 * Math.PI * 2;
            o.add(new P((float) Math.cos(an) * (0.62f + 0.06f * br), 0.9f + 0.05f * br, (float) Math.sin(an) * (0.62f + 0.06f * br), 0.035f, argb((0.35f + 0.45f * br) * k, col), 0));
        }
        return o;
    }
    /** Every particle's alpha times k. */
    private static List<P> fade(List<P> in, float k) {
        if (k >= 0.999f) return in;
        List<P> o = new ArrayList<>(in.size());
        for (P q : in) { int al = Math.round((q.argb() >>> 24) * k); if (al > 0) o.add(new P(q.x(), q.y(), q.z(), q.size(), (al << 24) | (q.argb() & 0xFFFFFF), q.shape(), q.lag())); }
        return o;
    }
    /** The moving animation: a short tail of light follows right behind you along the path you just walked. Slower steps, shorter and fainter tail. */
    static List<P> trail(String aura, ArrayDeque<double[]> hist, long now, double ex, double ey, double ez, double turn, float k) {
        List<P> o = new ArrayList<>(); int col = auraRgb(aura), pale = mix(col, 0xFFFFFF, 0.55f);
        int n = 18;
        for (int i = 0; i < n; i++) {
            float u = i / (float) n;
            double[] h = at(hist, now - (long) (u * 400f));                                                  // where you were u * 0.4 s ago: a short tail that stays close
            float hgt = 0.1f + 1.9f * (float) ((i * 0.618034) % 1.0);                                        // each particle keeps its own height: together they make a curtain
            double an = turn * 3.0 + i * 0.75; float r = 0.3f * (1f - 0.4f * u);
            float fl = (1f - u) * (1f - u * 0.4f);
            float tail = 0.25f * u * (float) Math.sin(turn * 2 + i);
            o.add(new P((float) (h[0] - ex + Math.cos(an) * r), hgt + tail, (float) (h[2] - ez + Math.sin(an) * r), 0.05f * (1f - 0.55f * u) + 0.012f, argb(fl * k * 0.95f, i % 3 == 0 ? pale : col), i % 4 == 0 ? 1 : 0));
        }
        return o;
    }
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
    private static final Map<String, java.lang.reflect.Field> FD = new java.util.HashMap<>();
    private static final Map<String, java.lang.reflect.Method> FM = new java.util.HashMap<>();
    /** A number from a field or, failing that, a method of the same name. Both lookups are found once per class, not every frame. */
    private static double fieldD(Object o, String f) {
        String key = o.getClass().getName() + "#" + f;
        try {
            java.lang.reflect.Field fl;
            if (FD.containsKey(key)) fl = FD.get(key); else { try { fl = o.getClass().getField(f); } catch (Throwable t) { fl = null; } FD.put(key, fl); }
            if (fl != null) return fl.getDouble(o);
            java.lang.reflect.Method m;
            if (FM.containsKey(key)) m = FM.get(key); else { try { m = o.getClass().getMethod(f); m.setAccessible(true); } catch (Throwable t) { m = null; } FM.put(key, m); }
            if (m != null && m.invoke(o) instanceof Number nn) return nn.doubleValue();
        } catch (Throwable ignored) { }
        return 0;
    }

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

    // ------------------------------------------------------------------ in the world: real 3D particles, drawn in the level pass so blocks and bodies hide them
    /** Where each player's body is drawn this frame (entity id -> x, y, z), read from the game's own render states, so the aura sits exactly on the body. */
    private static final Map<Integer, double[]> BODY = new HashMap<>();
    private static double wcX, wcY, wcZ; private static final org.joml.Quaternionf CAM_ROT = new org.joml.Quaternionf();
    private static boolean worldOk;

    /** End of the game's extraction step: remember where every player body and the camera are this frame. */
    static void extract(net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext ctx) {
        try {
            BODY.clear();
            var ls = ctx.levelState(); var cs = ls.cameraRenderState;
            wcX = cs.pos.x(); wcY = cs.pos.y(); wcZ = cs.pos.z(); CAM_ROT.set(cs.orientation);
            for (var st : ls.entityRenderStates) if (st instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState av && !av.isInvisible) BODY.put(av.id, new double[]{av.x, av.y, av.z});
        } catch (Throwable t) { if (!worldOk) System.err.println("[FancyDrops] aura extract failed: " + t); }
    }

    /** The level is collecting what to draw: add the aura particles of every player who wears one, as glowing camera-facing quads in the world. */
    static void submit(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            Map<UUID, Cosmetics.Look> looks = Cosmetics.looks();
            if (looks.isEmpty()) return;
            FancyConfig cf = FancyConfig.get();
            long now = System.currentTimeMillis();
            java.util.Set<UUID> allowed = null;                                                      // "most players with an aura": only the nearest few get drawn
            if (cf.cosMax > 0) {
                List<net.minecraft.world.entity.player.Player> near = new ArrayList<>();
                for (var p : mc.level.players()) if (p != mc.player) { Cosmetics.Look l0 = looks.get(p.getUUID()); if (l0 != null && l0.aura() != null) near.add(p); }
                near.sort(Comparator.comparingDouble(q -> mc.player.distanceToSqr(q)));
                allowed = new HashSet<>(); for (int i = 0; i < Math.min(cf.cosMax, near.size()); i++) allowed.add(near.get(i).getUUID());
            }
            org.joml.Vector3f right = CAM_ROT.transform(new org.joml.Vector3f(1, 0, 0)), up = CAM_ROT.transform(new org.joml.Vector3f(0, 1, 0));
            float pt = 1f; try { pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false); } catch (Throwable ignored) { }
            for (var p : mc.level.players()) {
                Cosmetics.Look l = looks.get(p.getUUID());
                if (l == null || l.aura() == null) continue;
                if (p != mc.player && (mc.player.distanceTo(p) > cf.cosRange || (allowed != null && !allowed.contains(p.getUUID())))) continue;
                double[] body = BODY.get(p.getId());
                if (body == null) {                                                                 // not drawn this frame (first person, invisible, out of view)
                    if (p != mc.player || cf.cosHideFirst) continue;
                    var v = p.getPosition(pt); body = new double[]{v.x, v.y, v.z};                // first person: your own aura still round you, if you want it
                }
                List<P> ps = frameParticles(p.getUUID(), l.aura(), body[0], body[1], body[2], now, cf);
                if (ps.isEmpty()) continue;
                final float ox = (float) (body[0] - wcX), oy = (float) (body[1] - wcY), oz = (float) (body[2] - wcZ);
                final float k = cf.cosSize / 100f;
                ctx.submitNodeCollector().submitCustomGeometry(ctx.poseStack(), net.minecraft.client.renderer.rendertype.RenderTypes.lightning(), (pose, buf) -> {
                    for (P q : ps) quad3d(pose, buf, ox + q.x(), oy + q.y(), oz + q.z(), q.size() * k, q, right, up);
                });
            }
            worldOk = true;
        } catch (Throwable t) { if (!worldOk) { worldOk = true; System.err.println("[FancyDrops] aura draw failed: " + t); } }
    }

    /** This frame's particles round one player: the resting aura, the idle swirl after 5 s still, and a short trail while moving. Positions are relative to the feet. */
    private static List<P> frameParticles(UUID id, String aura, double ex, double ey, double ez, long now, FancyConfig cf) {
        double[] st = STATE.get(id); if (st == null) { st = new double[]{0, now, ex, ey, ez, now, 1, 0, 0}; STATE.put(id, st); }     // phase ms, last frame, last position, last time it moved, speed, swirl
        double dtf = Math.max(1, Math.min(100, now - (long) st[1]));
        double moved = Math.abs(ex - st[2]) + Math.abs(ey - st[3]) + Math.abs(ez - st[4]);
        if (moved > 0.004) st[5] = now;
        double sp = Math.sqrt((ex - st[2]) * (ex - st[2]) + (ez - st[4]) * (ez - st[4])) / (dtf / 1000.0);      // blocks per second
        if (sp > 60) sp = 0;                                                                                     // a teleport, not a run
        st[7] += (Math.min(sp, 12.0) - st[7]) * Math.min(1.0, dtf / 160.0);
        st[2] = ex; st[3] = ey; st[4] = ez;
        ArrayDeque<double[]> hist = HIST.computeIfAbsent(id, kk -> new ArrayDeque<>());
        if (hist.isEmpty() || now - (long) hist.peekLast()[0] >= 25) hist.addLast(new double[]{now, ex, ey, ez});
        while (hist.size() > 2 && now - (long) hist.peekFirst()[0] > 800) hist.removeFirst();
        float moveK = (float) Math.max(0.0, Math.min(1.0, (st[7] - 0.35) / 3.6)); moveK = moveK * moveK * (3f - 2f * moveK);
        boolean idle = now - (long) st[5] > 5000, going = now - (long) st[5] < 250;
        double want = going ? 1.6 : idle ? 0.55 : 1.0;
        if (cf.cosCalm) want *= 0.4;
        double dtm = Math.min(100, now - st[1]); st[1] = now;
        st[6] += (want - st[6]) * Math.min(1.0, dtm / 250.0);
        st[0] += dtm * st[6] * Math.max(0.2, cf.cosSpeed / 100.0);
        float idleK = idle ? (float) Math.min(1.0, (now - st[5] - 5000) / 1200.0) : 0f;
        st[8] += dtm * (0.25 + st[7] * 0.35) / 1000.0;
        List<P> ps = fade(particles(aura, (long) st[0]), 1f - 0.6f * moveK);                                     // the aura stays on you while you run, a little fainter
        if (idleK > 0f) ps.addAll(idleParticles(aura, now, idleK * (1f - moveK)));
        if (moveK > 0.01f) ps.addAll(trail(aura, hist, now, ex, ey, ez, st[8], moveK));
        int keep = Math.min(100, cf.cosAmount);
        if (keep < 100) { List<P> o = new ArrayList<>(); for (int i = 0; i < ps.size(); i++) if ((i + 1) * keep / 100 != i * keep / 100) o.add(ps.get(i)); ps = o; }
        if (STATE.size() > 64) { STATE.clear(); HIST.clear(); }
        return ps;
    }

    /** One particle as glowing quads facing the camera: a soft halo, then the shape (a square, a four-point star, or a ring round the body). */
    private static void quad3d(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buf, float x, float y, float z, float size, P q, org.joml.Vector3f r, org.joml.Vector3f u) {
        int a = q.argb() >>> 24, rgb = q.argb() & 0xFFFFFF; if (a < 6 || size <= 0.001f) return;
        int cr = (rgb >> 16) & 255, cg = (rgb >> 8) & 255, cb = rgb & 255;
        if (q.shape() == 2) {                                                                    // a thin ring of light round the body at this height
            float rad = 0.6f; int seg = 28;
            for (int i = 0; i < seg; i++) {
                double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
                float x0 = x + (float) Math.cos(a0) * rad - q.x(), z0 = z + (float) Math.sin(a0) * rad - q.z(), x1 = x + (float) Math.cos(a1) * rad - q.x(), z1 = z + (float) Math.sin(a1) * rad - q.z();
                float h = 0.025f;
                buf.addVertex(pose, x0, y - h, z0).setColor(cr, cg, cb, a); buf.addVertex(pose, x1, y - h, z1).setColor(cr, cg, cb, a);
                buf.addVertex(pose, x1, y + h, z1).setColor(cr, cg, cb, a); buf.addVertex(pose, x0, y + h, z0).setColor(cr, cg, cb, a);
                buf.addVertex(pose, x0, y + h, z0).setColor(cr, cg, cb, a); buf.addVertex(pose, x1, y + h, z1).setColor(cr, cg, cb, a);       // the back face too, so it shows from any side
                buf.addVertex(pose, x1, y - h, z1).setColor(cr, cg, cb, a); buf.addVertex(pose, x0, y - h, z0).setColor(cr, cg, cb, a);
            }
            return;
        }
        bill(pose, buf, x, y, z, size * 1.9f, size * 1.9f, r, u, cr, cg, cb, Math.round(a * 0.22f));     // soft halo
        if (q.shape() == 1) {                                                                    // a four-point star: two thin arms and a bright core
            bill(pose, buf, x, y, z, size * 1.5f, size * 0.16f, r, u, cr, cg, cb, a);
            bill(pose, buf, x, y, z, size * 0.16f, size * 1.5f, r, u, cr, cg, cb, a);
            bill(pose, buf, x, y, z, size * 0.3f, size * 0.3f, r, u, 255, 255, 255, Math.min(255, a + 40));
        } else bill(pose, buf, x, y, z, size * 0.55f, size * 0.55f, r, u, cr, cg, cb, a);
    }
    /** A camera-facing rectangle, hw/hh its half width and height in blocks. */
    private static void bill(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buf, float x, float y, float z, float hw, float hh, org.joml.Vector3f r, org.joml.Vector3f u, int cr, int cg, int cb, int a) {
        if (a < 4) return;
        float rx = r.x * hw, ry = r.y * hw, rz = r.z * hw, ux = u.x * hh, uy = u.y * hh, uz = u.z * hh;
        buf.addVertex(pose, x - rx - ux, y - ry - uy, z - rz - uz).setColor(cr, cg, cb, a);
        buf.addVertex(pose, x + rx - ux, y + ry - uy, z + rz - uz).setColor(cr, cg, cb, a);
        buf.addVertex(pose, x + rx + ux, y + ry + uy, z + rz + uz).setColor(cr, cg, cb, a);
        buf.addVertex(pose, x - rx + ux, y - ry + uy, z - rz + uz).setColor(cr, cg, cb, a);
    }

    /** Called every frame by the HUD: only the fallback name over other players' heads (the auras are drawn in the world, see submit). */
    static void render(GuiGraphicsExtractor g) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || Cosmetics.nameTagHooked) return;
            Map<UUID, Cosmetics.Look> looks = Cosmetics.looks();
            if (looks.isEmpty()) return;
            FancyConfig cf = FancyConfig.get();
            long now = System.currentTimeMillis();
            double[] cam = camera(mc);
            double cx = cam[0], cy = cam[1], cz = cam[2]; float yaw = (float) cam[3], pitch = (float) cam[4];
            double yr2 = Math.toRadians(yaw), xr2 = Math.toRadians(pitch);
            double fx = -Math.sin(yr2) * Math.cos(xr2), fy = -Math.sin(xr2), fz = Math.cos(yr2) * Math.cos(xr2);
            double rx = -Math.cos(yr2), ry = 0, rz = -Math.sin(yr2);
            double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;
            int W = mc.getWindow().getGuiScaledWidth(), H = mc.getWindow().getGuiScaledHeight();
            double fov = Math.toRadians(mc.options.fov().get()), th = Math.tan(fov / 2), asp = W / (double) H;
            if (!Cosmetics.nameTagHooked) for (var p : mc.level.players()) {                                // fallback (the name-tag mixin did not apply): the name cosmetic and suffix over other players' heads
                if (p == mc.player) continue;
                Cosmetics.Look l = looks.get(p.getUUID());
                if (l == null || (l.fx() == null && l.suffix() == null) || mc.player.distanceTo(p) > Math.min(24, cf.cosRange)) continue;
                double ex = p.getX(), ey = p.getY() + p.getBbHeight() + 0.95, ez = p.getZ();
                double dx = ex - cx, dy = ey - cy, dz = ez - cz, zc = dx * fx + dy * fy + dz * fz;
                if (zc < 0.5) continue;
                double xc = dx * rx + dy * ry + dz * rz, yc = dx * ux + dy * uy + dz * uz;
                float sx = (float) (W / 2.0 * (1 + xc / (zc * th * asp))), sy = (float) (H / 2.0 * (1 - yc / (zc * th)));
                var font = mc.font; String nm = p.getName().getString();
                net.minecraft.network.chat.MutableComponent c = Cosmetics.nameComp(nm, 0xFFFFFF, l.fx(), true, now);
                if (l.suffix() != null) c.append(" ").append(Cosmetics.chatGlyph(l.suffix()));
                int tw = font.width(c);
                g.fill(Math.round(sx - tw / 2f - 2), Math.round(sy - 1), Math.round(sx + tw / 2f + 2), Math.round(sy + 9), 0x40000000);
                g.text(font, c, Math.round(sx - tw / 2f), Math.round(sy), 0xFFFFFFFF, true);
            }
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
                g.fill(x, y, x + 1, y + 1, (Math.min(255, al + 60) << 24) | 0xFFFFFF);
                break;
            }
            case 2: {                                                                  // a flat slice of light
                int w = Math.max(4, Math.min(18, Math.round(px * 3f)));
                g.fill(x - w / 2, y, x + w / 2, y + 1, a);
                break;
            }
            default: {
                int s = Math.max(1, Math.round(px));
                g.fill(x - s / 2, y - s / 2, x - s / 2 + s, y - s / 2 + s, a);
            }
        }
    }
}
