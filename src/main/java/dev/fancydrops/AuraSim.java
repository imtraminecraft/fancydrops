package dev.fancydrops;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The auras as little particle systems, one per player. Pure Java with no game objects: the world draws every particle as a real
 * Minecraft particle with our own glowing sprites (AuraWorld), and the cosmetics menu draws the very same simulation in 2D.
 *
 * Steps 20 times a second. A particle either rides along with the body (orbits, helixes, things that wander round you) or is left in
 * the world where it was born (the trail behind you while you run, embers that drift off).
 */
final class AuraSim {
    static final int GLOW = 0, STAR = 1, SPARK = 2, BUBBLE = 3, COIN = 4, RUNE = 5, EMBER = 6, COIN_EDGE = 7;
    static final String[] SPRITE = {"aura_glow", "aura_star", "aura_spark", "aura_bubble", "aura_coin", "aura_rune0", "aura_ember", "aura_coin_edge"};

    /** One particle. follow = its position is round the body; else it stays in the world. */
    static final class Part {
        boolean follow, orbit, dead;
        double x, y, z;                                 // offset from the feet (follow) or the world position
        double ox, oy, oz; boolean fresh = true;         // where it was last tick (drawing slides between the two)
        double vx, vy, vz;
        float ang, angV, rad, radV;                     // orbits: angle (radians), turn per tick, radius, its change per tick
        float size, sizeEnd, gravity, drag = 1f, twinkle, phase;
        int rgb, rgb2, sprite, rune, age, life;
        // what to draw this tick (worked out by step)
        float a, s; int col;
    }

    final String aura;
    final List<Part> parts = new ArrayList<>();
    final List<Part> born = new ArrayList<>();          // particles made in the last step (the world gives each one a Minecraft particle)
    private final Random rnd;
    private long tick;
    private double lx, ly, lz; private boolean first = true;
    private int stillTicks;
    private float moveK;                                // 0 standing, 1 running (smoothed)

    AuraSim(String aura, long seed) { this.aura = aura; this.rnd = new Random(seed); }

    private float r() { return rnd.nextFloat(); }
    private static float cl(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    static int mix(int a, int b, float k) {
        k = cl(k);
        return (Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k) << 16) | (Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k) << 8) | Math.round((a & 255) * (1 - k) + (b & 255) * k);
    }
    static int colour(String aura) {
        return switch (aura == null ? "" : aura) { case "glitchaura" -> 0xFF7A2E; case "stardust" -> 0x8FD0FF; case "patronaura" -> 0xFF9B2E; case "bubbleaura" -> 0x8FE0FF;
            case "fireflyaura" -> 0xE8FF70; case "coinaura" -> 0xFFD24A; case "runeaura" -> 0xC9A0FF; default -> 0xFFE680; };
    }

    private Part orbit(float ang, float angV, float rad, float y, float vy, int life, float size, int rgb, int sprite) {
        Part p = new Part(); p.follow = true; p.orbit = true; p.ang = ang; p.angV = angV; p.rad = rad; p.y = y; p.vy = vy; p.life = life; p.size = size; p.sizeEnd = size; p.rgb = p.rgb2 = rgb; p.sprite = sprite;
        return add(p);
    }
    private Part free(double x, double y, double z, double vx, double vy, double vz, int life, float size, int rgb, int sprite, boolean follow) {
        Part p = new Part(); p.follow = follow; p.x = x; p.y = y; p.z = z; p.vx = vx; p.vy = vy; p.vz = vz; p.life = life; p.size = size; p.sizeEnd = size; p.rgb = p.rgb2 = rgb; p.sprite = sprite;
        return add(p);
    }
    private Part add(Part p) { p.phase = r() * 6.28f; parts.add(p); born.add(p); return p; }

    /**
     * One step. bx/by/bz = where the feet are now. amount = 0..1 (fewer particles), speed = how fast they move, calm = slower and softer,
     * sizeK = size multiplier.
     */
    void step(double bx, double by, double bz, float amount, float speed, boolean calm, float sizeK) {
        born.clear(); tick++;
        if (first) { lx = bx; ly = by; lz = bz; first = false; }
        double mv = Math.hypot(bx - lx, bz - lz); lx = bx; ly = by; lz = bz;
        if (mv > 3) mv = 0;                                                                         // a teleport, not a run
        stillTicks = mv > 0.02 ? 0 : stillTicks + 1;
        moveK += ((float) Math.min(1.0, Math.max(0.0, (mv - 0.04) / 0.2)) - moveK) * 0.25f;
        float rate = Math.max(0.05f, amount) * (calm ? 0.55f : 1f), sp = Math.max(0.3f, speed) * (calm ? 0.6f : 1f);
        boolean idle = stillTicks > 100;
        emit(bx, by, bz, rate, sp, idle);
        if (moveK > 0.15f && chance(1.4f * rate * moveK)) {                                         // running: a short wake of light left behind you
            int col = colour(aura); float a0 = r() * 6.28f, rr = 0.25f + 0.2f * r();
            Part p = free(bx + Math.cos(a0) * rr, by + 0.1 + r() * 1.7, bz + Math.sin(a0) * rr, (r() - 0.5f) * 0.01, 0.01 + r() * 0.01, (r() - 0.5f) * 0.01, 12 + rnd.nextInt(8), 0.13f, r() < 0.3f ? mix(col, 0xFFFFFF, 0.6f) : col, r() < 0.25f ? STAR : GLOW, false);
            p.sizeEnd = 0.02f; p.drag = 0.9f;
        }
        for (Part p : parts) {
            p.ox = p.x; p.oy = p.y; p.oz = p.z; if (p.fresh) { p.fresh = false; }
            p.age++;
            if (p.age >= p.life) { p.dead = true; continue; }
            if (p.orbit) { p.ang += p.angV * sp; p.rad += p.radV * sp; p.y += p.vy * sp; p.x = Math.cos(p.ang) * p.rad; p.z = Math.sin(p.ang) * p.rad; }
            else { p.x += p.vx * sp; p.y += p.vy * sp; p.z += p.vz * sp; p.vy -= p.gravity; p.vx *= p.drag; p.vy *= p.drag; p.vz *= p.drag; }
            float u = p.age / (float) p.life;
            float in = cl(p.age / Math.min(8f, p.life * 0.15f)), out = cl((p.life - p.age) / Math.min(24f, p.life * 0.35f));
            float tw = p.twinkle <= 0 ? 1f : 1f - p.twinkle + p.twinkle * (0.5f + 0.5f * (float) Math.sin(p.age * 0.45f + p.phase));
            p.a = in * out * tw;
            p.s = (p.size + (p.sizeEnd - p.size) * u) * sizeK;
            p.col = p.rgb == p.rgb2 ? p.rgb : mix(p.rgb, p.rgb2, u);
        }
        parts.removeIf(p -> p.dead);
        if (parts.size() > 160) parts.subList(0, parts.size() - 160).forEach(p -> p.dead = true);
        parts.removeIf(p -> p.dead);
    }
    private boolean chance(float perTick) { float whole = (float) Math.floor(perTick); return r() < perTick - whole || whole >= 1f; }

    /** Each aura's own emitters. */
    private void emit(double bx, double by, double bz, float rate, float sp, boolean idle) {
        int col = colour(aura);
        switch (aura == null ? "" : aura) {
            case "sparkleaura" -> {                                                                  // Twinkle: stars pop in round you and twinkle, gold dust rises
                if (chance(0.5f * rate)) { float a = r() * 6.28f, rr = 0.4f + 0.3f * r(); Part p = free(Math.cos(a) * rr, 0.1 + r() * 1.9, Math.sin(a) * rr, 0, 0.002, 0, 22 + rnd.nextInt(14), 0.06f + 0.08f * r(), r() < 0.5f ? 0xFFE680 : 0xFFB3E6, STAR, true); p.twinkle = 0.6f; p.sizeEnd = 0.02f; }
                if (chance(0.06f * rate)) { float a = r() * 6.28f; Part p = free(Math.cos(a) * 0.55, 0.3 + r() * 1.5, Math.sin(a) * 0.55, 0, 0, 0, 14, 0.32f, 0xFFFFFF, STAR, true); p.sizeEnd = 0.04f; }
                if (chance(0.6f * rate)) { float a = r() * 6.28f, rr = 0.3f + 0.35f * r(); Part p = free(Math.cos(a) * rr, r() * 0.4, Math.sin(a) * rr, 0, 0.025 + 0.015 * r(), 0, 40 + rnd.nextInt(20), 0.05f, 0xFFF2C0, GLOW, true); p.sizeEnd = 0.015f; }
            }
            case "stardust" -> {                                                                     // two strands of light wind up the body, stars rise off them
                if (chance(0.7f * rate)) for (int s = 0; s < 2; s++) { Part p = orbit((float) (tick * 0.12 + s * Math.PI), 0.03f, 0.5f, 0.05f, 0.04f, 50, 0.1f, s == 0 ? 0x4DD6FF : 0x8FA8FF, GLOW); p.rgb2 = 0xE8FFFF; p.sizeEnd = 0.04f; }
                if (chance(0.12f * rate)) { float a = r() * 6.28f; Part p = free(Math.cos(a) * 0.6, 0.2 + r() * 1.2, Math.sin(a) * 0.6, 0, 0.03, 0, 30, 0.15f, 0xE8FFFF, STAR, true); p.sizeEnd = 0.02f; p.twinkle = 0.4f; }
            }
            case "glitchaura" -> {                                                                   // Alarm: red and amber sparks circle you, a pulse of light climbs, embers lift off
                if (tick % 3 == 0) { Part p = orbit(r() * 6.28f, 0.07f, 0.58f, 0.2f + r() * 1.6f, 0.002f, 26, 0.1f, r() < 0.5f ? 0xFF4A2E : 0xFFB02E, SPARK); p.twinkle = 0.7f; }
                if (tick % 56 == 0) for (int i = 0; i < 16; i++) { Part p = orbit(i * 6.2832f / 16, 0.03f, 0.62f, 0.0f, 0.045f, 46, 0.08f, 0xFFC03D, GLOW); p.rgb2 = 0xFF5A20; }
                if (chance(0.25f * rate)) { float a = r() * 6.28f; Part p = free(Math.cos(a) * 0.45, r() * 0.6, Math.sin(a) * 0.45, (r() - 0.5f) * 0.01, 0.03, (r() - 0.5f) * 0.01, 50, 0.08f, 0xFF5A36, EMBER, false); p.sizeEnd = 0.02f; p.x += bx; p.y += by; p.z += bz; }
            }
            case "patronaura" -> {                                                                   // Embers: warm embers drift up and away, now and then a glint
                if (chance(0.9f * rate)) { float a = r() * 6.28f, rr = 0.3f + 0.35f * r(); Part p = free(bx + Math.cos(a) * rr, by + r() * 0.5, bz + Math.sin(a) * rr, (r() - 0.5f) * 0.012, 0.025 + 0.02 * r(), (r() - 0.5f) * 0.012, 50 + rnd.nextInt(25), 0.1f, 0xFFD24A, EMBER, false); p.rgb2 = 0xFF5A1E; p.sizeEnd = 0.02f; p.twinkle = 0.35f; }
                if (chance(0.05f * rate)) { float a = r() * 6.28f; Part p = free(Math.cos(a) * 0.5, 0.4 + r() * 1.4, Math.sin(a) * 0.5, 0, 0, 0, 12, 0.22f, 0xFFF0C0, STAR, true); p.sizeEnd = 0.03f; }
            }
            case "bubbleaura" -> {                                                                   // Bubbles: wobble up round you and pop at the top
                if (chance(0.35f * rate)) { float a = r() * 6.28f, rr = 0.35f + 0.3f * r(); Part p = orbit(a, (r() - 0.5f) * 0.03f, rr, 0.05f, 0.035f + 0.01f * r(), 52, 0.07f + 0.05f * r(), 0x9AE0FF, BUBBLE); p.sizeEnd = p.size * 1.6f; p.rgb2 = 0xE8FFFF; }
                for (Part p : new ArrayList<>(parts)) if (p.sprite == BUBBLE && p.age == p.life - 1) { Part q = free(p.x, p.y, p.z, 0, 0, 0, 6, 0.16f, 0xFFFFFF, SPARK, true); q.sizeEnd = 0.02f; }
            }
            case "fireflyaura" -> {                                                                  // Fireflies: soft lights wander round you and blink
                long n = parts.stream().filter(p -> p.sprite == GLOW && p.follow).count();
                if (n < Math.round(7 * rate) + 2 && chance(0.2f)) { float a = r() * 6.28f; Part p = free(Math.cos(a) * 0.6, 0.3 + r() * 1.4, Math.sin(a) * 0.6, 0, 0, 0, 120 + rnd.nextInt(80), 0.13f, 0xE8FF70, GLOW, true); p.twinkle = 0.85f; p.rgb2 = 0xB8E860; }
                for (Part p : parts) if (p.sprite == GLOW && p.follow) {                            // they wander: a gentle steering towards a ring round you
                    double d = Math.hypot(p.x, p.z), want = 0.6;
                    p.vx += (r() - 0.5f) * 0.006 - p.x / Math.max(0.1, d) * (d - want) * 0.004; p.vz += (r() - 0.5f) * 0.006 - p.z / Math.max(0.1, d) * (d - want) * 0.004;
                    p.vy += (r() - 0.5f) * 0.004 - (p.y - 1.0) * 0.0015; p.drag = 0.96f;
                }
            }
            case "coinaura" -> {                                                                     // Coin Shower: coins tumble down round you and glint
                if (chance(0.3f * rate)) { float a = r() * 6.28f, rr = 0.4f + 0.25f * r(); Part p = free(Math.cos(a) * rr, 2.3, Math.sin(a) * rr, 0, -0.045 - 0.02 * r(), 0, 48, 0.12f, 0xFFFFFF, COIN, true); p.rune = rnd.nextInt(8); }
                for (Part p : new ArrayList<>(parts)) if (p.sprite == COIN || p.sprite == COIN_EDGE) { int fl = (p.age + p.rune) % 8; p.sprite = fl < 6 ? COIN : COIN_EDGE; if (fl == 0 && r() < 0.3f) { Part q = free(p.x, p.y, p.z, 0, 0, 0, 6, 0.16f, 0xFFF4C8, STAR, true); q.sizeEnd = 0.02f; } }
            }
            case "runeaura" -> {                                                                     // Runes: runes circle your waist on a tilted ring, one lights up at a time
                long n = parts.stream().filter(p -> p.sprite == RUNE).count();
                if (n < 8) { Part p = orbit((float) (n * Math.PI / 4), 0.025f, 0.65f, 1.0f, 0f, 1_000_000, 0.14f, n % 2 == 0 ? 0xB56BFF : 0x9AF0FF, RUNE); p.rune = (int) (n % 4); }
                int lit = (int) ((tick / 14) % 8), i = 0;
                for (Part p : parts) if (p.sprite == RUNE) { p.y = 1.0 + 0.16 * Math.sin(p.ang + tick * 0.03); p.rgb = p.rgb2 = i == lit ? 0xF4E8FF : (i % 2 == 0 ? 0xB56BFF : 0x9AF0FF); p.size = p.sizeEnd = i == lit ? 0.2f : 0.14f; p.age = Math.min(p.age, 20); i++; }
                if (chance(0.25f * rate)) { float a = r() * 6.28f; Part p = free(Math.cos(a) * 0.4, 0.4, Math.sin(a) * 0.4, 0, 0.025, 0, 40, 0.06f, 0xE6D2FF, GLOW, true); p.sizeEnd = 0.01f; }
            }
            default -> { }
        }
        if (idle && chance(0.5f * rate)) {                                                        // after 5 s standing still: two soft strands wind slowly round you
            for (int s = 0; s < 2; s++) { Part p = orbit((float) (tick * 0.07 + s * Math.PI), 0.01f, 0.6f, 0f, 0.025f, 90, 0.08f, s == 0 ? col : mix(col, 0xFFFFFF, 0.55f), s == 0 ? GLOW : STAR); p.sizeEnd = 0.03f; }
        }
    }

    /** The menu preview: one simulation per aura, stepped to the clock. */
    private static final java.util.Map<String, AuraSim> PREVIEW = new java.util.HashMap<>();
    private static final java.util.Map<String, Long> PREVIEW_T = new java.util.HashMap<>();
    static List<Part> preview(String aura, long now) {
        AuraSim s = PREVIEW.computeIfAbsent(aura, a -> new AuraSim(a, 7));
        long want = now / 50, have = PREVIEW_T.getOrDefault(aura, want - 60);
        if (want - have > 200) have = want - 60;                                                   // first look (or after a long time): start warm
        for (long k = have; k < want; k++) s.step(0, 0, 0, 1f, 1f, false, 1f);
        PREVIEW_T.put(aura, want);
        return s.parts;
    }
}
