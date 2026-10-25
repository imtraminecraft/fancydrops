package dev.fancydrops;

import dev.fancydrops.Render3D.Box;
import dev.fancydrops.Render3D.Obj;

import java.util.ArrayList;
import java.util.List;

/**
 * The drop scene, drawn as a pure function of time: a 3D model of the slayer's mob stands on a stone dais, a sword slices it
 * (the slices are cut into the picture itself, so the mob really falls apart along them), and the drop rises out of the pieces.
 *   tier 0 = default     (the plain sword, a small reveal - or just dust when nothing dropped)
 *   tier 1 = Rare Drop   (from slice 3 on: bigger glowing sword, trails, shockwaves, light beam, runes)
 *   tier 2 = RNG Drop    (from slice 3 on: huge sword, flashes, heavy shake, embers, a second shockwave)
 * The first two slices are identical in every tier.
 */
final class Scene {
    static final int W = 112, H = 96;
    static final double TWIN_DX = 34;                                                              // a twin drop's two items sit this far left / right of the middle
    static final double CX = 56, CY = 74, TILT = 0.52, GY = 0.62;
    private static final double CT = Math.cos(TILT), ST = Math.sin(TILT);
    static final int N = 12;                                   // texture size of the mob's faces
    private static final float SWEEP = 0.12f;                  // how long one slash of the sword takes
    private static final Canvas MOB = new Canvas(W, H), SL = new Canvas(W, H), SW = new Canvas(W, H), IT = new Canvas(W, H);

    /** A 3D item: either a few boxes (player heads) or a puffy sprite model. */
    static final class Model {
        final Obj[] parts; final double natural, pitchK; final PuffyItem puffy; final int[] sprite;   // sprite: flat 16x16 art of a non-skull item
        Model(Obj[] parts, double natural, double pitchK) { this.parts = parts; this.natural = natural; this.pitchK = pitchK; puffy = null; sprite = null; }
        Model(PuffyItem puffy, int[] sprite) { this.puffy = puffy; this.sprite = sprite; parts = null; natural = 16; pitchK = 1; }   // sprite == null: a skull we only have a picture of, spun as a thick 3D slab
    }

    static final class Params {
        int tier, chops; boolean hasItem, reveal = true; int rarity = 0xFFFF55FF;
        Model item, item2; int rarity2 = 0xFFFF55FF; boolean twin;      // twin = two drops from one kill, shown together
        int base = 0xFF5E9E52, dark = 0xFF3D6B36, eye = 0xFF10200E;
        boolean dye; int dyeCol = 0xFFB03A2E;     // dye drops get their own paint-splash scene instead of the mob
        boolean farm, helios, pest;                               // the small Farming scene (a growing crop)
        int mob;                                            // Mythological scene: which creature is summoned (0 Hunter, 1 Lynxes, 2 Gaia, 3 Minotaur, 4 Champion, 5 Inquisitor)
        boolean exp;                                        // Experimentation Table drops: the table's book opens, zooms in, flips its pages and shows the drop
        boolean treasure;                                   // a Mythological treasure drop (the digging scene) instead of a creature drop
        boolean myth;                                       // Mythological Ritual drops: three golden lightning strikes light a rune circle between two pillars
        boolean alt;                                // the second animation style of the Dye / Farming / Mythological / Experiment scenes
        int style;                                  // slayer animation style: 0 Sword (sword), 1 Spirit (soul slashes)
        int seed = 7, cutSeed = 7, theme = 6;       // theme: 0 zombie 1 spider 2 wolf 3 enderman 4 blaze 5 vampire 6 none
        float shakeX, shakeY;
        private int[] front, side, top, cloth, stone, stoneTop;
        Params(int tier, boolean hasItem) { this.tier = tier; this.hasItem = hasItem; chops = chopsFor(tier); }
    }

    static int chopsFor(int tier) { return tier == 2 ? 10 : tier == 1 ? 4 : 2; }
    /** Seconds per slice after the first two: the bigger scenes slice faster. */
    static float interval(int tier) { return tier == 2 ? 0.26f : tier == 1 ? 0.36f : 0.42f; }
    /** The first two slices are identical in every scene (0.42 s each); only the later ones differ. */
    static float chopStart(Params p, int k) { return 0.7f + (k < 2 ? k * 0.42f : 0.84f + (k - 2) * interval(p.tier)); }
    static float chopIv(Params p, int k) { return k < 2 ? 0.42f : interval(p.tier); }
    /** A Mythological drop in the Normal state: the small, see-through scene (a ritual circle for a creature, a dig for a treasure) that plays 3 seconds. */
    static boolean normalMyth(Params p) { return p.myth && p.tier != 2 && p.hasItem; }
    /** The rows of the 112 x 96 canvas the Normal Mythological scene really uses: the layout crops the empty rest so nothing floats above the action bar. */
    static final int CROP_T = 20, CROP_B = 80;

    static float impact(Params p, int k) { return p.exp ? (p.tier == 2 ? 1.45f + 0.3f * k - 0.008f * k * k : 1.45f + 0.36f * k) : p.myth ? (normalMyth(p) ? 0.4f + 0.5f * k : 0.55f + 0.5f * k) : p.farm ? (p.pest ? 1.15f : p.helios ? 0.5f + 0.35f * k : 0.12f + 0.18f * k) : p.dye ? 0.75f : chopStart(p, k) + 0.8095f * chopIv(p, k); }   // dye: when each paint blob lands
    static float splitTime(Params p) { return p.exp ? impact(p, Math.max(0, p.chops - 1)) + 0.45f : p.myth ? (normalMyth(p) ? (p.treasure ? 2.0f : 1.85f) : impact(p, Math.max(0, p.chops - 1)) + 0.6f) : p.farm ? impact(p, Math.max(0, p.chops - 1)) + (p.pest ? 0.32f : p.helios ? 0.7f : 0.25f) : p.dye ? impact(p, 0) + 1.0f : chopStart(p, p.chops) + 0.04f; }
    /** 0 = plain sword (first two slices of every scene); 1 = dramatic (Rare Drop); 2 = super dramatic (RNG Drop). */
    static int drama(Params p, int k) { return k < 2 ? 0 : p.tier; }
    static float length(Params p) { return p.exp ? splitTime(p) + (p.tier == 2 ? 4.6f : 3.4f) : p.myth ? splitTime(p) + (normalMyth(p) ? 1.15f : p.tier == 2 ? 5.4f : 3.9f) : p.farm ? splitTime(p) + (p.pest ? 1.0f : p.helios ? 1.9f : 0.77f) : p.dye ? splitTime(p) + 4.2f : splitTime(p) + (p.tier == 2 ? 4.6f : p.tier == 1 ? 3.7f : 3.4f); }

    private static float cl(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float easeOut(float x) { x = cl(x); return 1 - (1 - x) * (1 - x) * (1 - x); }
    private static float easeIn(float x) { x = cl(x); return x * x; }
    private static float easeIO(float x) { x = cl(x); return x * x * (3 - 2 * x); }
    private static float back(float x) { x = cl(x); float c1 = 1.70158f, c3 = c1 + 1; return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2); }
    private static float frac(double v) { return (float) (v - Math.floor(v)); }
    private static float h(int a, int b) { return Tex.h(a, b); }

    private static final int[][] GLYPH = {                    // thin rune strokes, not letters
        {0b100, 0b100, 0b111, 0b100, 0b100}, {0b111, 0b010, 0b010, 0b010, 0b000}, {0b001, 0b001, 0b111, 0b001, 0b001},
        {0b101, 0b101, 0b101, 0b010, 0b010}, {0b010, 0b101, 0b000, 0b101, 0b010}, {0b110, 0b000, 0b010, 0b000, 0b011}};

    // ------------------------------------------------------------------------------------------------ the cuts and the sword

    /** Slice k as a line on screen: {centre x, centre y, direction x, direction y}. The same for every tier. */
    private static float[] cut(Params p, int k) {
        double a = (h(k + p.cutSeed, 3) - 0.5) * 2.5 + (h(k, p.cutSeed + 1) > 0.5f ? 0 : Math.PI * 0.0);     // anywhere from steep to nearly flat, either way
        if (Math.abs(a) < 0.18) a += a < 0 ? -0.35 : 0.35;                                                    // never a perfectly flat or vertical-looking cut
        return new float[]{(float) CX + (h(k, p.cutSeed + 7) - 0.5f) * 30, (float) (CY - 24 * CT) + (h(k, p.cutSeed + 9) - 0.5f) * 30, (float) Math.cos(a), (float) Math.sin(a)};
    }
    /** Where the sword starts and ends slice k (it alternates left-to-right and right-to-left). */
    private static float[] ends(Params p, int k) {
        float[] c = cut(p, k); float d = h(k, p.cutSeed + 5) > 0.5f ? 1 : -1, L = 38;      // each slash may come from either side
        return new float[]{c[0] - c[2] * L * d, c[1] - c[3] * L * d, c[0] + c[2] * L * d, c[1] + c[3] * L * d};
    }
    private static double travelAngle(float[] e) { return Math.atan2(-(e[3] - e[1]) / CT, e[2] - e[0]); }
    private static double lerpAngle(double a, double b, double f) { double d = Math.atan2(Math.sin(b - a), Math.cos(b - a)); return a + d * f; }

    /** The sword at time t: {screen x, screen y, angle, current slice}, or null when it is out of sight. */
    private static double[] sword(Params p, float t) {
        float t0 = 0.3f, sw0 = impact(p, 0) - SWEEP;
        if (t < t0) return null;
        float[] e0 = ends(p, 0);
        if (t < sw0) {
            float f = easeOut((t - t0) / (sw0 - t0));
            double ang = travelAngle(e0);
            return new double[]{e0[0] - 30 * (1 - f) - 6 * (1 - f), e0[1] - 40 * (1 - f), ang + 0.9 * (1 - f), 0};
        }
        for (int k = 0; k < p.chops; k++) {
            float[] e = ends(p, k); float imp = impact(p, k), start = imp - SWEEP;
            double ang = travelAngle(e);
            if (t >= start && t < imp + 0.02f) {
                float f = cl((t - start) / SWEEP);
                return new double[]{e[0] + (e[2] - e[0]) * f, e[1] + (e[3] - e[1]) * f, ang, k};
            }
            if (k + 1 < p.chops) {
                float next = impact(p, k + 1) - SWEEP;
                if (t >= imp + 0.02f && t < next) {
                    float g = easeIO((t - imp - 0.02f) / (next - imp - 0.02f)); float[] n = ends(p, k + 1);
                    return new double[]{e[2] + (n[0] - e[2]) * g, e[3] + (n[1] - e[3]) * g - 16 * Math.sin(Math.PI * g), lerpAngle(ang, travelAngle(n), g), k};
                }
            } else if (t >= imp + 0.02f) {
                float g = (t - imp - 0.02f) / 0.4f;
                if (g > 1) return null;
                return new double[]{e[2] + (e[2] - e[0]) * 0.6 * g, e[3] + (e[3] - e[1]) * 0.6 * g - 34 * g * g, ang, k};
            }
        }
        return null;
    }

    private static Obj[] swordParts(Params p, double[] s) {
        int d = drama(p, (int) s[3]);
        double sc = d == 0 ? 1 : d == 1 ? 1.35 : 1.8;
        int edge = d == 2 ? 0xFFFFE9A0 : d == 1 ? 0xFFBFE6FF : 0xFFD6DCE6;
        Box blade = new Box(13 * sc, 2.4 * sc, 0.8 * sc).all(Tex.solid(2, edge), 2).face(4, Tex.steel(12), 12).face(5, Tex.steel(12), 12);
        Box guard = new Box(0.9 * sc, 4.8 * sc, 1.5 * sc).all(Tex.solid(2, d == 2 ? 0xFFFFC840 : 0xFFE9B84A), 2);
        Box grip = new Box(3.4 * sc, 1.1 * sc, 1.1 * sc).all(Tex.solid(2, d == 2 ? 0xFF8A1F1F : 0xFF5B3A22), 2);
        Box pommel = new Box(1.4 * sc, 1.4 * sc, 1.4 * sc).all(Tex.solid(2, 0xFFE9B84A), 2);
        double[] off = {0, -13 * sc, -17.4 * sc, -21.2 * sc};
        Box[] bs = {blade, guard, grip, pommel};
        double ca = Math.cos(s[2]), sa = Math.sin(s[2]);
        double x = s[0] - CX, z = 10, y = (CY - s[1] + z * ST) / CT;
        for (int i = 0; i < 4; i++) bs[i].at(x + off[i] * ca, y + off[i] * sa, z).rot(0, 0.25, s[2]);
        return bs;
    }

    // ------------------------------------------------------------------------------------------------ the mob

    private record Part(double x, double y, double z, double hx, double hy, double hz, int kind, double pitch, double roll, int col) {}
    private static Part pt(double x, double y, double z, double hx, double hy, double hz, int kind) { return new Part(x, y, z, hx, hy, hz, kind, 0, 0, 0); }

    /** The mob as boxes in its own space: feet at y = 0, facing +z. kind: 0 skin, 1 clothes, 2 head, 3 solid colour, 4 glowing. */
    private static List<Part> mobParts(Params p, float t) {
        List<Part> l = new ArrayList<>();
        double k = 1;
        switch (p.theme) {
            case 1 -> {                                                           // spider
                k = 1.9;
                l.add(pt(0, 6, -3, 6.5, 3.4, 6, 1)); l.add(pt(0, 6.5, 8, 4, 3.2, 3.5, 2));
                double[] zs = {-7, -2.5, 2, 6.5};
                for (int i = 0; i < 4; i++) for (int s = -1; s <= 1; s += 2)
                    l.add(new Part(s * 11.5, 6.5, zs[i], 7.5, 0.9, 0.9, 3, 0, -s * (0.6 + 0.05 * i), Canvas.shade(p.dark, 0.8f)));
            }
            case 2 -> {                                                           // wolf
                k = 1.7;
                l.add(pt(0, 11, 0, 4.5, 4.5, 9, 0)); l.add(pt(0, 15, 11.5, 3.6, 3.6, 3.6, 2)); l.add(pt(0, 13.3, 16.5, 2, 1.6, 2.5, 3));
                l.add(pt(-2.4, 19.5, 10.5, 0.9, 1.3, 0.7, 0)); l.add(pt(2.4, 19.5, 10.5, 0.9, 1.3, 0.7, 0));
                l.add(new Part(0, 14, -11.5, 1.3, 1.3, 4.5, 0, 0.7, 0, 0));
                for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) l.add(pt(sx * 2.7, 4.3, sz * 6.5, 1.5, 4.3, 1.5, 1));
            }
            case 3 -> {                                                           // enderman
                l.add(pt(-2.2, 12, 0, 1.4, 12, 1.4, 1)); l.add(pt(2.2, 12, 0, 1.4, 12, 1.4, 1));
                l.add(pt(0, 31, 0, 4, 6, 2.4, 1));
                l.add(pt(-6, 27, 0, 1.4, 11, 1.4, 1)); l.add(pt(6, 27, 0, 1.4, 11, 1.4, 1));
                l.add(pt(0, 42.5, 0, 4.4, 4.4, 4.4, 2));
            }
            case 4 -> {                                                           // blaze
                l.add(pt(0, 28, 0, 4.4, 4.4, 4.4, 2));
                for (int ring = 0; ring < 2; ring++) for (int i = 0; i < 4; i++) {
                    double a = t * (ring == 0 ? 2.2 : -1.6) + i * Math.PI / 2 + ring * 0.8, r = ring == 0 ? 9 : 12.5;
                    l.add(new Part(Math.cos(a) * r, ring == 0 ? 16 + Math.sin(t * 3 + i) * 1.5 : 7 + Math.sin(t * 3 + i * 2) * 1.5, Math.sin(a) * r, 1.2, 5, 1.2, 4, 0, 0, i % 2 == 0 ? 0xFFFFB02A : 0xFFFF7A1A));
                }
            }
            case 5 -> {                                                           // vampire
                humanoid(l, false);
                l.add(new Part(0, 21, -3.7, 6.8, 11, 0.7, 3, 0, 0, 0xFF6E0F22));
                l.add(new Part(0, 26.5, 3.4, 4.2, 1.2, 0.6, 3, 0, 0, 0xFF6E0F22));
            }
            case 0 -> humanoid(l, true);                                           // zombie: arms out in front
            default -> humanoid(l, false);
        }
        if (k != 1) {
            List<Part> o = new ArrayList<>();
            for (Part q : l) o.add(new Part(q.x * k, q.y * k, q.z * k, q.hx * k, q.hy * k, q.hz * k, q.kind, q.pitch, q.roll, q.col));
            return o;
        }
        return l;
    }

    private static void humanoid(List<Part> l, boolean armsOut) {
        l.add(pt(-3, 7, 0, 2.7, 7, 2.7, 1)); l.add(pt(3, 7, 0, 2.7, 7, 2.7, 1));
        l.add(pt(0, 20.5, 0, 5.6, 6.6, 3, 1));
        if (armsOut) { l.add(new Part(-8.4, 23, 5, 2.5, 6.5, 2.5, 0, -1.45, 0, 0)); l.add(new Part(8.4, 23, 5, 2.5, 6.5, 2.5, 0, -1.45, 0, 0)); }
        else { l.add(pt(-8.4, 21, 0, 2.5, 6.5, 2.5, 0)); l.add(pt(8.4, 21, 0, 2.5, 6.5, 2.5, 0)); }
        l.add(pt(0, 33.6, 0, 5.2, 5.2, 5.2, 2));
    }

    private static Box[] mobBoxes(Params p, float t, float lean, float yOff) {
        List<Part> parts = mobParts(p, t);
        Box[] out = new Box[parts.size()];
        double cg = Math.cos(GY), sg = Math.sin(GY), cl = Math.cos(lean), sl = Math.sin(lean);
        for (int i = 0; i < out.length; i++) {
            Part q = parts.get(i);
            Box b = new Box(q.hx, q.hy, q.hz);
            switch (q.kind) {
                case 0 -> b.all(p.side, N).face(2, p.top, N);
                case 1 -> b.all(p.cloth, N);
                case 2 -> b.all(p.side, N).face(2, p.top, N).face(4, p.front, N);
                case 4 -> b.all(Tex.solid(2, q.col), 2);
                default -> b.all(Tex.solid(2, q.col != 0 ? q.col : Canvas.shade(p.base, 0.9f)), 2);
            }
            if (q.kind == 4) b.bright = 1.35f;
            double x1 = q.x * cl - q.y * sl, y1 = q.x * sl + q.y * cl;
            b.at(x1 * cg + q.z * sg, y1 + yOff, -x1 * sg + q.z * cg).rot(GY, q.pitch, q.roll + lean);
            b.outerYaw = true;
            out[i] = b;
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ draw

    /** Draws the scene at t seconds into out (cleared first). l3 is scratch space of the same size. */
    static void draw(Canvas out, Canvas l3, Params p, float t) {
        out.clear(); l3.clear(); MOB.clear(); SL.clear(); SW.clear(); IT.clear();
        if (p.exp) { drawExperiment(out, p, t); return; }
        if (p.farm) { drawFarm(out, p, t); return; }
        if (normalMyth(p)) { if (p.treasure) drawTreasureN(out, p, t); else drawMobsN(out, p, t); return; }
        if (p.myth) { drawMytho(out, p, t); return; }
        if (p.dye) { drawDye(out, p, t); return; }
        if (p.front == null) {
            p.front = Tex.mobFront(N, p.base, p.dark, p.eye, p.seed);
            p.side = Tex.noise(N, p.base, p.dark, p.seed + 3);
            p.top = Tex.noise(N, Canvas.mix(p.base, 0xFFFFFFFF, 0.12f), p.base, p.seed + 5);
            p.cloth = Tex.noise(N, Canvas.mix(p.dark, p.base, 0.25f), Canvas.shade(p.dark, 0.7f), p.seed + 11);
            p.stone = Tex.floor(p.theme, false, p.seed + 2); p.stoneTop = Tex.floor(p.theme, true, p.seed + 2);
        }
        float ts = splitTime(p), us = Math.max(0f, t - ts - 0.12f);
        boolean split = t >= ts, epic = p.tier == 2, cool = p.tier >= 1;
        int ct = (int) Math.round(CY - 24 * CT);                           // screen y of the middle of the mob

        // hits: screen shake, and the mob leans away from the sword
        float shx = 0, shy = 0, lean = 0;
        for (int k = 0; k < p.chops; k++) {
            float a = t - impact(p, k);
            if (a < 0 || a > 0.4f) continue;
            int d = drama(p, k);
            if (a < 0.16f) { float amp = (d == 2 ? 3.6f : d == 1 ? 2.0f : 1.4f) * (1 - a / 0.16f); shx += (float) Math.sin(a * 110) * amp; shy += (float) Math.cos(a * 90) * amp * 0.6f; }
            lean += (k % 2 == 0 ? -1 : 1) * (d == 2 ? 0.16f : d == 1 ? 0.12f : 0.08f) * (float) (Math.exp(-a * 14) * Math.cos(a * 42));
        }
        if (split && t - ts < 0.25f) { float a = t - ts; float amp = (epic ? 3.4f : cool ? 2f : 1f) * (1 - a / 0.25f); shx += (float) Math.sin(a * 100) * amp; shy += (float) Math.cos(a * 80) * amp; }
        p.shakeX = shx; p.shakeY = shy;

        // ---- behind everything
        if (split && p.reveal) twinPass(out, p, t, ts, us, cool, epic, 0, false);
        if (p.style == 1) { spiritBackdrop(out, p, t, ts); } else ambient(out, p, t);

        // ---- the dais
        float be = back(t / 0.35f);
        if (p.style == 1) summonCircle(out, p, t, ts); else {
        Box dais = new Box(26, 2.5, 20).all(p.stone, 16).face(2, p.stoneTop, 16); dais.at(0, -2.5 - (1 - be) * 34, 0).rot(GY, 0, 0);
        Box rim = new Box(27.5, 0.8, 21.5).all(Tex.solid(2, Canvas.mix(p.base, 0xFF202028, 0.45f)), 2); rim.at(0, -5.6 - (1 - be) * 34, 0).rot(GY, 0, 0);
        Render3D.render(l3, new Obj[]{dais, rim}, CX, CY, 1.0, TILT);
        l3.softOutline(0.34f);
        out.draw(l3);
        if (be > 0.9f) runeRing(out, p, t);
        }

        // ---- the mob (drops in, idles, gets sliced)
        float td = t - 0.15f;
        if (td >= 0) {
            double yOff = td < 0.3f ? 57 * (1 - (td / 0.3) * (td / 0.3)) : 4.5 * Math.exp(-(td - 0.3) * 9) * Math.abs(Math.sin((td - 0.3) * 16));
            if (td >= 0.3f && !split) yOff += Math.sin(t * 3.2) * 0.5;
            Render3D.render(MOB, mobBoxes(p, t, lean, (float) yOff), CX, CY, 1.0, TILT);
            MOB.softOutline(0.34f);
            sliceInto(p, t, ts);
            out.draw(SL);
            if (p.style == 1) soulSlashes(out, p, t, ts, ct); else if (!split) cutGlow(out, p, t);
        }

        // ---- the sword
        double[] sw = p.style == 1 || (split && t > splitTime(p) + 0.5f) ? null : sword(p, t);
        if (sw != null) {
            Render3D.render(SW, swordParts(p, sw), CX, CY, 1.0, TILT);
            SW.softOutline(0.4f);
            out.draw(SW);
        }

        // ---- the drop
        if (!split && p.reveal && p.hasItem && cool) charge(out, p, t, ts, ct);
        if (split && p.reveal && p.hasItem && p.item != null) drawItem(out, p, t, ts, us);

        // ---- effects over the top
        for (int k = 0; k < p.chops; k++) {
            float th = impact(p, k), a = t - th;
            if (a < 0 || a > 0.6f) continue;
            for (int i = 0; i < 16; i++) {                                // debris chunks in the mob's colours
                float vx = (h(k, i) - 0.5f) * 80, vy = -(18 + h(i, k + 5) * 48);
                int x = (int) (CX + (h(i, k + 9) - 0.5f) * 18 + vx * a), y = (int) (ct + vy * a + 0.5f * 150 * a * a);
                int col = i % 3 == 0 ? p.dark : i % 3 == 1 ? p.base : 0xFF8E1B2B;
                int sz = i % 4 == 0 ? 2 : 1;
                out.rect(x, y, x + sz, y + sz, Canvas.rgb((int) (255 * (1 - a / 0.4f)), col));
            }
            if (drama(p, k) > 0) for (int i = 0; i < (drama(p, k) == 2 ? 22 : 12); i++) {          // sparks
                float vx = (h(k + 3, i) - 0.5f) * 110, vy = -(25 + h(i, k + 21) * 55);
                int x = (int) (CX + vx * a * 0.9f), y = (int) (ct - 2 + vy * a + 0.5f * 160 * a * a);
                if (i % 3 == 0) out.glint(x, y, 2, i % 2 == 0 ? 0xFFB347 : 0xFFE07A, Math.max(0f, 1 - a / 0.45f));
                else out.set(x, y, Canvas.rgb((int) (255 * (1 - a / 0.45f)), i % 2 == 0 ? 0xFFFFB347 : 0xFFFFE07A));
            }
        }
        if (p.style != 1) impactExtras(out, p, t, ct);
        if (split && p.reveal) twinPass(out, p, t, ts, us, cool, epic, ct, true);
    }

    // ------------------------------------------------------------------------------------------------ the dye scene

    private static void ell(Canvas c, int cx, int cy, int rx, int ry, int argb) {
        if (rx < 1 || ry < 1) return;
        for (int y = -ry; y <= ry; y++) {
            double yy = (double) y / ry;
            int hw = (int) Math.round(rx * Math.sqrt(Math.max(0, 1 - yy * yy)));
            for (int x = -hw; x <= hw; x++) c.set(cx + x, cy + y, argb);
        }
    }


    private static int hsv(float h, float sat, float v) { return 0xFF000000 | (java.awt.Color.HSBtoRGB(h - (float) Math.floor(h), Math.max(0f, Math.min(1f, sat)), Math.max(0f, Math.min(1f, v))) & 0xFFFFFF); }

    /** The dye's own colour first, then bright neighbours around the colour wheel, a complement and white. */
    private static int[] palette(int col) {
        float[] f = java.awt.Color.RGBtoHSB((col >> 16) & 255, (col >> 8) & 255, col & 255, null);
        float s = Math.max(0.65f, f[1]);
        return new int[]{col | 0xFF000000, hsv(f[0] + 0.07f, s, 1f), hsv(f[0] + 0.16f, s, 1f), hsv(f[0] - 0.08f, s, 1f), hsv(f[0] + 0.5f, 0.75f, 1f), hsv(f[0] + 0.33f, 0.7f, 1f), 0xFFFFFFFF};
    }

    private static void star(Canvas c, int x, int y, int r, int argb) {
        c.glint(x, y, Math.max(2, r), argb & 0xFFFFFF, (argb >>> 24) / 255f);
    }

    /** How far (0 = centre, 1 = edge) a point is inside the dye splat: a round body with uneven tendrils. */
    private static float splatE(float dx, float dy, float R0, float[] ang, float[] wid, float[] len) {
        float d = (float) Math.hypot(dx, dy), th = (float) Math.atan2(dy, dx), b = 1f + 0.07f * (float) Math.sin(th * 3 + 1.3) + 0.05f * (float) Math.sin(th * 7 + 0.4);
        for (int i = 0; i < ang.length; i++) {
            float dd = th - ang[i]; dd -= (float) (Math.PI * 2 * Math.round(dd / (Math.PI * 2)));
            b += len[i] * (float) Math.exp(-(dd / wid[i]) * (dd / wid[i]));
        }
        return d / Math.max(1f, R0 * b);
    }

    /** Dye drops: one blob of paint in the dye's own colour drops from above and bursts into a single giant splat that fills the screen,
     *  then the dye rises out of the middle of it in rays of the same colour. */
    private static void drawDye(Canvas out, Params p, float t) {
        float ts = splitTime(p), us = Math.max(0f, t - ts - 0.12f);
        boolean split = t >= ts;
        int[] pal = palette(p.dyeCol);
        int col = pal[0], dark = Canvas.shade(col, 0.55f), deep = Canvas.shade(col, 0.38f), lite = Canvas.mix(col, 0xFFFFFFFF, 0.5f);
        int cx = (int) CX, cy = 48, n = pal.length;
        float land = impact(p, 0), a = t - land;
        p.shakeX = a >= 0 && a < 0.3f ? (float) Math.sin(a * 120) * 4f * (1f - a / 0.3f) : 0f;
        p.shakeY = a >= 0 && a < 0.3f ? (float) Math.cos(a * 100) * 3f * (1f - a / 0.3f) : 0f;
        if (split && t - ts < 0.35f) { float f = 1 - (t - ts) / 0.35f; p.shakeX += (float) Math.sin((t - ts) * 110) * 3f * f; }
        int icy = itemScreenY(t, ts);

        if (split && p.hasItem && p.item != null) {
            twinPass(out, p, t, ts, us, true, false, 0, false);
            float in = cl(us / 0.15f);
            for (int i = 0; i < 16; i++) {                                // rays in the dye's own shades, slowly turning
                double ang = us * 0.7 + i * (Math.PI * 2 / 16); float L = 66 * (i % 2 == 0 ? 1f : 0.6f); int rc = i % 3 == 0 ? lite : i % 3 == 1 ? col : 0xFFFFFFFF;
                for (int s2 = 6; s2 < L; s2++) {
                    float f = s2 / L; int al = (int) (in * 120 * (1 - f));
                    int x = (int) (cx + Math.cos(ang) * s2), y = (int) (icy + Math.sin(ang) * s2 * 0.9);
                    out.rect(x, y, x + (f < 0.5f ? 2 : 1), y + (f < 0.5f ? 2 : 1), Canvas.rgb(al, rc));
                }
            }
        }

        // the blob falls in from the top
        if (p.alt) brushStrokes(out, p, t, ts, col, dark, lite);
        if (!p.alt && t < land) {
            float f = cl((t - 0.15f) / (land - 0.15f)), y = -8 + (cy + 8) * f * f, r = 3 + 3 * f;
            for (int i = 1; i <= 10; i++) out.disc(cx, (int) (y - i * 2.2f), Math.max(1, (int) (r * (1 - i / 12f))), Canvas.rgb(Math.round(150 * (1 - i / 11f) * f), col));
            out.disc(cx, (int) y, (int) r + 1, dark); out.disc(cx, (int) y, (int) r, col);
            out.set(cx - 2, (int) y - 2, 0xFFFFFFFF); out.set(cx - 1, (int) y - 3, lite);
        }

        // the splat: a round body with long, uneven tendrils, filling the whole frame
        if (!p.alt && a >= 0) {
            float g = easeOut(a / 0.3f) * (1f + 0.07f * (float) Math.sin(a * 16) * (float) Math.exp(-a * 4.5f)), creep = 1f + 0.03f * easeOut(a / 2f);
            float fade = split ? 1f - 0.4f * easeOut((t - ts) / 0.6f) : 1f;
            final int L = 11; float[] ang = new float[L], wid = new float[L], len = new float[L];
            for (int i = 0; i < L; i++) { ang[i] = (i + 0.6f * (h(i, 91) - 0.5f)) * (float) (Math.PI * 2 / L); wid[i] = 0.2f + 0.26f * h(i, 92); len[i] = 0.4f + 0.7f * h(i, 93); }
            final float R0 = 27f * g * creep;
            for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
                float dx = x + 0.5f - cx, dy = y + 0.5f - cy;
                if (Math.hypot(dx, dy) > 78f * g + 6f) continue;
                float e = splatE(dx, dy, R0, ang, wid, len);
                if (e > 1f) {                                                          // the splat's soft shadow on the ground
                    if (splatE(dx - 2.5f, dy - 3.5f, R0, ang, wid, len) <= 1f) out.set(x, y, Canvas.rgb((int) (95 * fade), 0xFF05030A));
                    continue;
                }
                float bound = Math.max(1f, (float) Math.hypot(dx, dy) / Math.max(0.001f, e)), d = (float) Math.hypot(dx, dy);
                float zz = (float) Math.sqrt(Math.max(0f, 1f - e * e)), nx = dx / bound * 0.9f, ny = dy / bound * 0.9f, nz = 0.30f + zz * 0.85f;
                float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz); nx /= nl; ny /= nl; nz /= nl;
                float diff = Math.max(0f, nx * -0.42f + ny * -0.52f + nz * 0.74f);
                float spec = (float) Math.pow(Math.max(0f, nx * -0.24f + ny * -0.30f + nz * 0.92f), 120);
                int c = Canvas.shade(col, 0.50f + 0.62f * diff);
                float ring = (float) Math.sin(d * 0.55f - a * 3f) * 0.5f + 0.5f;
                if (e < 0.6f) c = Canvas.mix(c, deep, 0.08f * ring);
                if (e > 0.92f) c = Canvas.mix(c, dark, (e - 0.92f) / 0.08f * 0.8f);
                c = Canvas.mix(c, 0xFFFFFFFF, Math.min(0.8f, spec * 0.8f));
                float rim = Math.max(0f, e - 0.8f) * 5f * Math.max(0f, nx * 0.5f + ny * 0.6f);      // a thin light on the far lower edge
                if (rim > 0f) c = Canvas.mix(c, lite, Math.min(0.5f, rim));
                if (h(x, y + 300) > 0.988f) c = Canvas.mix(c, lite, 0.6f);
                out.set(x, y, Canvas.rgb((int) (255 * fade * (e > 0.97f ? (1f - e) / 0.03f : 1f)), c));
            }
            for (int i = 0; i < 30; i++) {                                             // flung droplets
                float ph = cl(a / 0.3f - h(i, 94) * 0.12f); if (ph <= 0f) continue;
                double an = h(i, 95) * Math.PI * 2; float dist = (26 + 46 * h(i, 96)) * easeOut(ph) * creep; int rr = 1 + (int) (h(i, 97) * 2.4f);
                int x = (int) (cx + Math.cos(an) * dist), y = (int) (cy + Math.sin(an) * dist * 0.9f);
                out.disc(x, y, rr, Canvas.rgb((int) (255 * fade), i % 4 == 0 ? dark : col));
                if (rr > 1) out.set(x - 1, y - 1, Canvas.rgb((int) (230 * fade), 0xFFFFFFFF));
            }
            if (a < 0.14f) out.glow(cx, cy, 60, 0xFFFFFF, 0.55f * (1f - a / 0.14f));   // the burst of impact
        }

        // the reveal
        if (split) {
            float ra = t - ts;
            if (ra < 0.6f) for (int ring = 0; ring < 2; ring++) {
                float rr = ra - ring * 0.08f; if (rr < 0) continue;
                float r = 6 + 62 * easeOut(rr / 0.6f);
                for (int i = 0; i < 180; i++) { double an = i * Math.PI * 2 / 180; int x = (int) (cx + Math.cos(an) * r), y = (int) (icy + Math.sin(an) * r * 0.8f);
                    out.rect(x, y, x + 2, y + 1, Canvas.rgb((int) (230 * (1 - rr / 0.6f)), ring == 0 ? lite : 0xFFFFFFFF)); }
            }
            for (int i = 0; i < 22; i++) {                                              // twinkling stars
                float ph = frac(us * 0.9 + i * 0.211), tw = (float) Math.sin(ph * Math.PI);
                int x = (int) (cx + (h(i, 21) - 0.5f) * 96), y = (int) (cy + 36 - ph * 80);
                if (us > 0 && tw > 0.2f) star(out, x, y, tw > 0.7f ? 2 : 1, Canvas.rgb((int) (240 * tw), i % 3 == 0 ? 0xFFFFFFFF : lite));
            }
            float fl = cl(1f - ra / 0.26f);
            if (fl > 0) out.glow(cx, icy, 66, 0xFFFFFF, 0.8f * fl);
        }
        if (split && p.hasItem && p.item != null) {
            ribbons(out, us, icy, pal, false);
            drawItem(out, p, t, ts, us);
            ribbons(out, us, icy, pal, true);
        }
    }


    /** Dye, style 2: a wide brush sweeps four coats of the dye across the screen, one after the other, flinging flecks as it goes. */
    private static void brushStrokes(Canvas out, Params p, float t, float ts, int col, int dark, int lite) {
        float fade = t >= ts ? 1f - 0.45f * easeOut((t - ts) / 0.6f) : 1f;
        final int ST = 4, band = 26;
        for (int s = 0; s < ST; s++) {
            float t0 = 0.08f + s * 0.17f, f = cl((t - t0) / 0.3f);
            if (f <= 0f) continue;
            boolean ltr = s % 2 == 0;
            int y0 = 2 + s * 22;
            float edge = ltr ? -8 + (W + 16) * easeOut(f) : W + 8 - (W + 16) * easeOut(f);
            for (int y = Math.max(0, y0); y < Math.min(H, y0 + band); y++) {
                float yy = (y - y0) / (float) band, wob = (float) Math.sin(y * 0.8 + s * 2.3) * 1.6f;
                float rag = Math.min(yy, 1f - yy) * band;
                int streak = h(y, 55 + s) > 0.78f ? 1 : 0;
                for (int x = 0; x < W; x++) {
                    boolean in = ltr ? x <= edge + wob : x >= edge + wob;
                    if (!in) continue;
                    if (rag < 3.2f * h(x / 3 + s * 9, 400 + s)) continue;
                    float shade = 0.80f + 0.30f * h(y, 77 + s) + (yy > 0.18f && yy < 0.34f ? 0.16f : 0f) + streak * 0.18f;
                    int c = Canvas.shade(col, shade);
                    if (Math.abs(x - (edge + wob)) < 2.2f) c = Canvas.mix(c, lite, 0.55f);            // the wet leading edge
                    else if (rag < 5f) c = Canvas.mix(c, dark, 0.35f);
                    out.set(x, y, Canvas.rgb((int) (255 * fade), c));
                }
            }
            if (f < 1f) {                                                                             // the brush itself, and flecks off its tip
                int hx = Math.round(edge), hy = y0 + band / 2, dir = ltr ? 1 : -1;
                out.rect(hx - 5, hy - 9, hx + 5, hy + 9, Canvas.shade(col, 1.15f));
                out.rect(hx - 5, hy - 9, hx + 5, hy - 7, lite);
                int fx = hx - dir * 9;
                out.rect(Math.min(fx, fx - dir * 4), hy - 10, Math.max(fx, fx - dir * 4), hy + 10, 0xFFB8C0D0);
                out.rect(fx - dir * 4, hy - 3, fx - dir * 20, hy + 3, 0xFF8A5A2C);
                for (int i = 0; i < 7; i++) {
                    float ph = cl(f * 1.2f - h(i, 120 + s) * 0.3f); if (ph <= 0f) continue;
                    int px = hx + dir * (int) (4 + 20 * ph), py = hy + (int) ((h(i, 121 + s) - 0.5f) * 36 + 16 * ph * ph);
                    out.disc(px, py, 1 + (i % 2), Canvas.rgb((int) (230 * (1f - ph)), i % 3 == 0 ? dark : col));
                }
            }
        }
        float shine = cl((t - 0.9f) / 0.7f);                                                         // a wet gleam runs over the finished coat
        if (shine > 0f && shine < 1f) out.glow(-10 + (W + 20) * shine, 48, 40, 0xFFFFFF, 0.28f * (1f - Math.abs(shine - 0.5f) * 1.6f));
    }

    /** Farming, style 2: a rain cloud waters the field and every third of it bursts into sparkles as the rain reaches it. */
    private static void rainStorm(Canvas out, Params p, float t, float ts, int gy, int cx, int n, int NC) {
        float in = cl(t / 0.25f) * (t >= ts ? cl(1f - (t - ts) / 0.4f) : 1f);
        if (in > 0.02f) {
            for (int i = 0; i < 6; i++) {
                int x = cx - 40 + i * 16 + (int) (Math.sin(t * 1.2 + i) * 2), y = 8 + (i % 2) * 4;
                ell(out, x, y, 13, 6, Canvas.rgb((int) (230 * in), i % 2 == 0 ? 0x4A5878 : 0x5E6E92));
                ell(out, x - 2, y - 2, 8, 3, Canvas.rgb((int) (150 * in), 0x8FA3C8));
            }
            for (int i = 0; i < 64 && t < ts; i++) {
                float ph = frac(t * 2.6 + h(i, 140)), x0 = cx - 46 + h(i, 141) * 92;
                int y = (int) (14 + ph * (gy - 12));
                int al = (int) (170 * in * Math.min(1f, (1f - ph) * 4f));
                out.rect((int) x0, y, (int) x0 + 1, y + 3, Canvas.rgb(al, 0xA8D8FF));
            }
        }
        for (int k = 0; k < n; k++) {
            float a = t - impact(p, k); if (a < 0f || a > 0.7f) continue;
            float zx = cx - 35 + ((k * NC / n) + (NC / n - 1) / 2f + 0.5f) * 14 - 7;
            float r = 3 + 20 * easeOut(a / 0.5f);
            for (int i = 0; i < 40; i++) { double an = i * Math.PI * 2 / 40; out.set((int) (zx + Math.cos(an) * r), (int) (gy + 2 + Math.sin(an) * r * 0.3), Canvas.rgb((int) (210 * (1f - a / 0.7f)), 0xBFE6FF)); }
            if (a < 0.2f) out.glow(zx, gy - 8, 24, 0xDFFFE0, 0.55f * (1f - a / 0.2f));
            for (int i = 0; i < 12; i++) {
                double an = -Math.PI * (0.1 + 0.8 * h(i, 150 + k)); float sp = 18 + 26 * h(i, 160 + k);
                int px = (int) (zx + Math.cos(an) * sp * a), py = (int) (gy - 6 + Math.sin(an) * sp * a + 140 * a * a);
                out.rect(px, py, px + 2, py + 1, Canvas.rgb((int) (255 * (1f - a / 0.7f)), i % 3 == 0 ? 0xFFFFFFFF : i % 3 == 1 ? 0xFF7BE05A : 0xFFFFE680));
            }
        }
    }

    /** Pest, style 2: rain pours on the field, a puddle spreads round the pest, it is washed away and the drop rises out of the water. */
    private static void pestRain(Canvas out, Params p, float t, float ts, float hit, int gy, int cx, float a0) {
        rainStorm(out, p, t, ts, gy, cx, 1, 6);
        float pu = cl((t - 0.3f) / Math.max(0.1f, ts - 0.3f));
        ell(out, cx, gy + 1, (int) (4 + 20 * pu), (int) (1 + 3 * pu), Canvas.rgb((int) (230 * cl(pu * 4f)), 0xFF3E78B8));
        ell(out, cx, gy, (int) (3 + 15 * pu), (int) (1 + 2 * pu), Canvas.rgb((int) (230 * cl(pu * 4f)), 0xFF8CC4F0));
        if (t < hit) {
            float u = cl(t / hit), bx = cx - 30 + 30 * u * u, by = gy - 2 - Math.abs((float) Math.sin(t * 12)) * 3f * u;
            bug(out, bx, by, 1f - 0.4f * u * u, t, 1, u > 0.4f);
        }
        float g = t - hit;
        if (g >= 0f && g < 0.5f) {
            for (int i = 0; i < 8; i++) { double an = Math.PI * (0.1 + 0.8 * h(i, 11)); float sp = 14 + 26 * h(i, 12); out.glint(Math.round(cx + (float) Math.cos(an) * sp * g * 2f), Math.round(gy - 4 - (float) Math.sin(an) * sp * g * 2f), 1, 0xBFE6FF, 1f - g / 0.5f); }
        }
        if (t >= ts - 0.2f && t < ts) { float f = (t - (ts - 0.2f)) / 0.2f; out.glow(cx, gy - 2, 6 + 22 * f, 0xFFFFFF, 0.6f * f); }
        if (t >= ts) {
            float fl = cl(1f - a0 / 0.3f); out.glow(cx, gy - 4, 40, 0xFFFFFF, 0.7f * fl);
            for (int r = 0; r < 2; r++) {
                float ra = a0 - r * 0.07f, dur = 0.42f; if (ra < 0f || ra > dur) continue;
                float rad = 5 + 40 * easeOut(ra / dur);
                for (int i = 0; i < 100; i++) { double an = i * Math.PI * 2 / 100; out.set((int) (cx + Math.cos(an) * rad), (int) (gy + 1 + Math.sin(an) * rad * 0.3), Canvas.rgb((int) (200 * (1f - ra / dur)), r == 0 ? 0xFFBFE6FF : 0xFFFFFFFF)); }
            }
        }
    }

    /** Mythological, style 2: golden comets plunge out of a starry sky onto the rune circle instead of lightning. */
    private static void comet(Canvas out, float t, float land, float tx, int gy, int k) {
        float u = (t - (land - 0.3f)) / 0.3f;
        if (u <= 0f || u > 1.12f) return;
        float uu = Math.min(1f, u);
        float dx = (k % 2 == 0 ? -1 : 1) * 36f;
        for (int j = 0; j < 22; j++) {
            float uj = uu - j * 0.022f; if (uj < 0f) break;
            float x = tx - dx * (1f - uj), y = -6 + (gy + 4) * uj;
            int al = (int) (240 * (1f - j / 22f) * (u > 1f ? cl(1f - (u - 1f) / 0.12f) : 1f));
            int c = j < 3 ? 0xFFFFFFFF : j < 9 ? 0xFFFFE9A0 : 0xFFE8A030;
            int sz = j < 4 ? 2 : 1;
            out.rect((int) x, (int) y, (int) x + sz, (int) y + sz, Canvas.rgb(al, c));
        }
        if (uu < 1f) out.glow(tx - dx * (1f - uu), -6 + (gy + 4) * uu, 12, 0xFFE9A0, 0.6f);
    }

    /** Experiment, style 2: instead of a book, a rune orb hangs over the table; each flip lights a ring round it and sends a shockwave out. */
    private static void expOrb(Canvas out, Params p, float t, float ts, int bx, int by, float z, int landed, int K, boolean rng, boolean split, float open) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 5);
        int R0 = Math.max(6, Math.round((10 + 2 * open) * z));
        int core = rng ? 0xFFE9A0 : 0xFFB56BFF, ring = rng ? 0xFFFFC857 : 0xFF7AD8FF;
        float lit = landed / (float) K;
        if (!split) {
            for (int r = 0; r < 3; r++) {                                                              // orbit rings, one more lit with every flip
                int rx = Math.round((R0 + 7 + r * 6) * 1f), ry = Math.max(2, rx / 3);
                boolean on = r < Math.ceil(lit * 3);
                for (int i = 0; i < 96; i++) {
                    double an = i * Math.PI * 2 / 96 + t * (r % 2 == 0 ? 1.1 : -0.8) + r;
                    int x = (int) (bx + Math.cos(an) * rx), y = (int) (by + Math.sin(an) * ry * (r == 1 ? -1 : 1) + (r - 1) * 2);
                    out.set(x, y, Canvas.rgb(on ? 220 : 70, on ? ring : 0xFF5A4A8A));
                }
            }
        }
        for (int d = 0; d < R0; d++) {                                                                  // the orb: a lit sphere
            float f = d / (float) R0;
            out.disc(bx, by, R0 - d, Canvas.mix(Canvas.shade(core, 0.35f + 0.25f * pulse), Canvas.mix(core, 0xFFFFFFFF, 0.55f), f));
        }
        out.disc(bx - R0 / 3, by - R0 / 3, Math.max(1, R0 / 4), 0xCCFFFFFF);
        for (int i = 0; i < 6; i++) {                                                                   // runes drifting inside the orb
            double an = t * 1.6 + i * Math.PI / 3; int x = (int) (bx + Math.cos(an) * R0 * 0.55), y = (int) (by + Math.sin(an) * R0 * 0.35);
            out.rect(x, y, x + 2, y + 2, Canvas.rgb(200, i % 2 == 0 ? 0xFFFFFFFF : ring));
        }
        out.glow(bx, by, R0 * 3f, core, 0.28f + 0.2f * pulse * (0.4f + lit));
        for (int k = 0; k < K; k++) {                                                                   // a shockwave for every flip
            float a = t - impact(p, k); if (a < 0f || a > 0.5f) continue;
            float r = R0 + 4 + 40 * easeOut(a / 0.5f);
            for (int i = 0; i < 120; i++) { double an = i * Math.PI * 2 / 120; out.set((int) (bx + Math.cos(an) * r), (int) (by + Math.sin(an) * r * 0.6), Canvas.rgb((int) (210 * (1f - a / 0.5f)), k % 2 == 0 ? ring : 0xFFFFFFFF)); }
        }
    }

    private static void ribbons(Canvas out, float us, int cy, int[] pal, boolean front) {
        float in = cl(us / 0.3f);
        if (in <= 0) return;
        int n = pal.length - 1;
        for (int r = 0; r < 4; r++) for (int i = 0; i < 44; i++) {
            double an = us * 2.6 + i * 0.19 + r * (Math.PI / 2);
            if ((Math.sin(an) > 0) != front) continue;
            double rad = 17 + 4 * Math.sin(i * 0.3 + us * 2);
            int x = (int) (CX + Math.cos(an) * rad), y = (int) (cy + 28 - i * 1.4 + Math.sin(an) * rad * 0.3);
            int a = (int) (240 * Math.sin(i / 44.0 * Math.PI) * in);
            out.rect(x, y, x + 2, y + 2, Canvas.rgb(a, pal[(r + i / 11) % n]));
        }
        for (int i = 0; i < 6; i++) {                                                  // orbs circling the dye
            double an = us * 2.0 + i * Math.PI / 3;
            if ((Math.sin(an) > 0) != front) continue;
            int x = (int) (CX + Math.cos(an) * 27), y = (int) (cy + Math.sin(an) * 9);
            out.disc(x, y, 2, Canvas.rgb((int) (230 * in), pal[i % n]));
            out.set(x - 1, y - 1, Canvas.rgb((int) (255 * in), 0xFFFFFFFF));
        }
    }

    /** Cuts the rendered mob along every slice so far: the pieces open up a little, then fly apart on the final slice. */
    private static void sliceInto(Params p, float t, float ts) {
        int K = p.chops;
        float[][] c = new float[K][]; float[] hit = new float[K];
        int made = 0;
        for (int k = 0; k < K; k++) { c[k] = cut(p, k); hit[k] = impact(p, k); if (t >= hit[k]) made = k + 1; }
        boolean split = t >= ts;
        float big = split ? easeOut((t - ts) / 0.55f) * (15f / (float) Math.sqrt(Math.max(1, K))) : 0f;
        float fade = split ? cl((t - ts - 0.55f) / 0.7f) : 0f;
        int[] src = MOB.px;
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int col = src[y * W + x];
            if ((col >>> 24) == 0) continue;
            float dx = 0, dy = 0;
            for (int k = 0; k < made; k++) {
                float nx = -c[k][3], ny = c[k][2];
                float side = (x + 0.5f - c[k][0]) * nx + (y + 0.5f - c[k][1]) * ny, s = side >= 0 ? 1 : -1;
                float open = (split ? 0 : 1.3f * easeOut((t - hit[k]) / 0.14f)) + big;
                dx += s * nx * open; dy += s * ny * open;
            }
            if (split) dy += 10 * easeIn((t - ts - 0.2f) / 1.0f);
            int a = (int) ((col >>> 24) * (1 - fade));
            if (a > 3) SL.set(x + Math.round(dx), y + Math.round(dy), (a << 24) | (col & 0xFFFFFF));
        }
    }

    /** The glowing line each slice leaves behind. */
    private static void cutGlow(Canvas out, Params p, float t) {
        for (int k = 0; k < p.chops; k++) {
            float imp = impact(p, k), start = imp - SWEEP;
            if (t < start) break;
            float[] e = ends(p, k);
            float f = cl((t - start) / SWEEP), age = Math.max(0, t - imp);
            int d = drama(p, k);
            float core = Math.max(0f, 1f - age / 0.5f), hot = cl(1f - age / 0.12f);
            int rare = d == 2 ? 0xFFFFE9A0 : d == 1 ? p.rarity : 0xFFFFFFFF;
            int len = (int) Math.hypot(e[2] - e[0], e[3] - e[1]);
            for (int i = 0; i <= len * 2 * f; i++) {
                float u = i / (2f * len);
                int x = Math.round(e[0] + (e[2] - e[0]) * u), y = Math.round(e[1] + (e[3] - e[1]) * u);
                if (!hitsMob(x, y)) continue;
                out.set(x, y, Canvas.rgb((int) (110 + 145 * core), core > 0.05f ? 0xFFFFFFFF : 0xFFB02030));
                if (hot > 0 || d > 0) {
                    int wa = (int) ((d > 0 ? 160 : 120) * Math.max(hot, core * 0.5f));
                    out.set(x, y - 1, Canvas.rgb(wa, rare)); out.set(x, y + 1, Canvas.rgb(wa, rare));
                    if (d == 2 && hot > 0) { out.set(x - 1, y, Canvas.rgb(wa / 2, rare)); out.set(x + 1, y, Canvas.rgb(wa / 2, rare)); }
                }
            }
        }
    }
    private static boolean hitsMob(int x, int y) {
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            int xx = x + dx, yy = y + dy;
            if (xx >= 0 && yy >= 0 && xx < W && yy < H && (SL.px[yy * W + xx] >>> 24) > 100) return true;
        }
        return false;
    }

    private static void runeRing(Canvas out, Params p, float t) {
        int col = Canvas.mix(p.base, 0xFFFFFFFF, 0.4f);
        for (int i = 0; i < 20; i++) {
            double a = i * Math.PI * 2 / 20 + t * 0.6;
            float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3 + i);
            int x = (int) Math.round(CX + Math.cos(a) * 20), y = (int) Math.round(CY - 1 + Math.sin(a) * 20 * ST);
            if (Math.sin(a) < 0 && Math.abs(Math.cos(a)) < 0.7) continue;                 // hidden behind the mob
            out.set(x, y, Canvas.rgb((int) (110 + 120 * pulse), col));
            if (i % 5 == 0) out.set(x, y - 1, Canvas.rgb((int) (80 * pulse), col));
        }
    }

    /** Item Changes preview: nothing but the drop itself, flying up and then spinning. sec = seconds since it appeared. */
    static void drawItemOnly(Canvas out, Canvas l3, Params p, float sec) {
        out.clear(); l3.clear(); IT.clear();
        if (p.item == null) return;
        float ts = splitTime(p);
        drawOne(out, p, p.item, p.rarity, ts + 0.12f + sec, ts, Math.max(0f, sec), 0);
    }

    private static void drawItem(Canvas out, Params p, float t, float ts, float us) {
        if (p.twin && p.item2 != null) {
            float tr = ts + 0.12f, uu = (t - tr) / 0.35f;
            if (uu > 0 && uu < 1.6f) {                                              // both halves burst apart: a bright streak flashes across the cut
                float f = 1 - cl(uu / 1.6f); int w = (int) (6 + 46 * easeOut(uu / 1.6f)), cy = (int) (CY - 30);
                out.rect((int) CX - w, cy, (int) CX + w, cy + 1, Canvas.rgb((int) (200 * f), 0xFFFFFFFF));
                out.rect((int) CX - w / 2, cy - 1, (int) CX + w / 2, cy + 2, Canvas.rgb((int) (110 * f), p.rarity));
            }
            drawOne(out, p, p.item, p.rarity, t, ts, us, -1);
            drawOne(out, p, p.item2, p.rarity2, t, ts, us, 1);
        } else drawOne(out, p, p.item, p.rarity, t, ts, us, 0);
    }

    /** side: 0 = the usual single drop in the middle, -1 / +1 = the left / right drop of a twin scene. */
    private static void drawOne(Canvas out, Params p, Model m, int rar, float t, float ts, float us, int side) {
        float dl = side > 0 ? 0.3f : 0f;                                                   // twin: the right drop follows a moment after the left one
        float tr = ts + 0.12f + dl, uu = (t - tr) / 0.35f;
        us = Math.max(0f, us - dl);
        if (uu <= 0) return;
        boolean epic = p.tier == 2, cool = p.tier >= 1;
        float e = back(uu);
        boolean skull = m.parts != null || (m.puffy != null && m.sprite == null);       // heads are bulky: draw them smaller
        FancyConfig ic = FancyConfig.get();
        String rk = p.exp ? "exprng" : p.myth ? "mythrng" : "rng";
        float spinK = epic ? ic.rngOpt(rk, "spin", FancyConfig.rngSpinDef()) : 1f, popK = epic ? ic.rngOpt(rk, "pop", FancyConfig.rngPopDef()) : 1f;     // RNG drops: the item spins like a block and pops out bigger (both set in the animation's page)
        double size = (side != 0 ? (epic ? 26 : cool ? 23 : 21) : (epic ? 31 : cool ? 26 : 22)) * Math.max(0.01, e) * popK
                * FancyConfig.get().itemK() * (skull ? 0.72 : 1.0);
        double dx = side * TWIN_DX * easeOut(uu);
        double yy = itemY(uu) + (ic.itemBob ? 1.6 * ic.itemBobAmp : 0.0) * Math.sin(us * 2.6 + (side > 0 ? 1.7 : 0)) + (side != 0 ? 14 * Math.sin(Math.min(1f, Math.max(0f, uu)) * Math.PI) : 0);     // twin: each item flies out in an arc
        double spin = epic ? rngAngle(us, spinK, ic.itemSpin) * (ic.itemSpinRev ? -1 : 1) * (side == 0 ? 1 : -side) : 2.8 * us * ic.itemSpin * (ic.itemSpinRev ? -1 : 1) * (side == 0 ? 1 : -side);
        float sx = (float) (CX + dx), sy = (float) (CY - yy * CT);
        IT.clear();
        float pulseG = 0.75f + 0.25f * (float) Math.sin(us * 3.2);
        out.glow(sx, sy, (float) size * (side != 0 ? 1.25f : epic ? 1.7f : 1.4f), rar & 0xFFFFFF, Math.min(1f, uu) * (epic ? 0.62f : 0.5f) * pulseG * ic.itemGlow);     // the icons' soft halo
        if (side == 0) out.glow(sx, sy, (float) size * 0.8f, 0xFFFFFF, Math.min(1f, uu) * 0.22f * pulseG * ic.itemGlow);
        int rst = p.exp ? 2 : p.myth ? 1 : 0;
        if (epic && side == 0) rngAura(out, rst, sx, sy, (float) size, us, uu, rar, false);                                             // the RNG stage: floor light, orbits, speed lines
        if (m.sprite != null) {
            flatItem(out, p, m, rar, sx, sy, (float) size, us, uu, (float) FancyConfig.get().itemK() * (side != 0 ? 0.72f : 1f) * (epic ? popK : 1f), epic, (float) spin, rst);
        } else if (m.puffy != null) {
            m.puffy.draw(IT, sx, sy, (float) (size / 16.0), spin, 0.35);
        } else {
            double k = size / m.natural;
            Obj[] parts = m.parts;
            for (Obj o : parts) { o.scale = k; o.at(dx, yy, 6).rot(spin, -TILT * m.pitchK, 0); }
            Render3D.render(IT, parts, CX, CY, 1.0, TILT);
        }
        if (ic.itemOutline) IT.softOutline(0.42f);
        out.draw(IT);
        if (epic && side == 0) rngAura(out, rst, sx, sy, (float) size, us, uu, rar, true);
    }

    /** Non-skull items: coin-flip entrance, glowing halo, travelling shine and orbiting sparkles. */
    private static void flatItem(Canvas out, Params p, Model m, int rar, float sx, float sy, float size, float us, float uu, float sm, boolean slab, float ang, int style) {
        int[] spr = m.sprite;
        int col = rar & 0xFFFFFF;
        float pulse = 0.5f + 0.5f * (float) Math.sin(us * 3.2);
        if (!p.twin) for (int r = 4; r >= 1; r--) out.disc(Math.round(sx), Math.round(sy), Math.round(size * (0.42f + r * 0.1f)), Canvas.rgb((int) ((10 + 14 * pulse) * (5 - r) / 2f * Math.min(1f, uu) * FancyConfig.get().itemGlow), col));
        float k = Math.round(2f * sm * 2f) / 2f, half = 8f * k;                                   // whole-number scale so every sprite pixel stays square
        int al = (int) (255 * cl(uu * 2f));
        float shine = (us * 0.7f) % 1.6f - 0.3f;                                                      // a bright band sweeps across now and then
        if (slab) slabItem(spr, sx, sy, k, ang, al, shine, style, us, col);
        else {
        float flip = us < 0.8f ? (float) Math.cos(4 * Math.PI * (1 - Math.exp(-4.5 * us))) : 1f;      // two quick coin flips that settle
        float xs = Math.max(0.06f, Math.abs(flip));
        for (int y = 0; y < H; y++) {
            float v = (y - sy) / k + 8f;
            if (v < 0 || v >= 16) continue;
            for (int x = (int) (sx - half - 2); x <= sx + half + 2; x++) {
                float u = (x + 0.5f - sx) / (k * xs) + 8f;
                if (u < 0 || u >= 16) continue;
                int c = spr[(int) v * 16 + (int) u];
                if ((c >>> 24) < 40) continue;
                float d = (u + v) / 32f - shine;
                float f = flip < 0 ? 0.72f : 1f;                                                      // the back of the flip is darker
                if (Math.abs(d) < 0.06f) f += 0.55f * (1 - Math.abs(d) / 0.06f);
                IT.set(x, y, (Canvas.shade(c | 0xFF000000, f) & 0xFFFFFF) | (al << 24));
            }
        }
        }
        for (int i = 0; i < 6 && FancyConfig.get().itemGlints; i++) {                                  // orbiting glints
            double a = us * 1.6 + i * Math.PI / 3, rr = size * 0.72;
            int x = Math.round(sx + (float) Math.cos(a) * (float) rr), y = Math.round(sy + (float) Math.sin(a) * (float) rr * 0.55f);
            float tw = Math.min(1f, uu) * (0.45f + 0.55f * (float) Math.sin(us * 6 + i * 2) * (float) Math.sin(us * 6 + i * 2));
            out.glint(x, y, i % 2 == 0 ? 3 : 2, i % 2 == 0 ? 0xFFFFFF : col, tw);
        }
    }

    /** How far an RNG drop has turned: a fast spin that settles into a steady turn, like a block on a stand. */
    private static float rngAngle(float us, float spinK, float itemSpin) {
        if (spinK <= 0.01f) return 0f;
        return 7.5f * (1f - (float) Math.exp(-2.6 * us)) * Math.min(1f, spinK) + 3.3f * us * spinK * Math.max(0.2f, itemSpin);
    }

    /** A thick, turning item: the sprite is stacked in a few layers, so the side shows as it turns (the block in the lobby). */
    private static void slabItem(int[] spr, float sx, float sy, float k, float ang, int al, float shine, int style, float us, int col) {
        float cs = (float) Math.cos(ang), sn = (float) Math.sin(ang);
        if (Math.abs(cs) < 0.06f) cs = cs < 0 ? -0.06f : 0.06f;
        final int L = 5; final float thick = 2.4f;
        float half = 8f * k;
        for (int li = 0; li < L; li++) {
            int j = cs >= 0 ? li : L - 1 - li;                                   // far layers first
            float z = (j / (float) (L - 1) - 0.5f) * thick;
            boolean face = j == L - 1 || j == 0, near = cs >= 0 ? j == L - 1 : j == 0;
            float ox = z * sn * k;
            int tint = style == 2 ? hsv(us * 0.35f + j * 0.16f, 0.55f, 1f) & 0xFFFFFF : style == 1 ? 0xFFD36B : col;
            for (int y = 0; y < H; y++) {
                float v = (y - sy) / k + 8f;
                if (v < 0 || v >= 16) continue;
                for (int x = (int) (sx + ox - half - 2); x <= sx + ox + half + 2; x++) {
                    float u = (x + 0.5f - sx - ox) / (k * cs) + 8f;
                    if (u < 0 || u >= 16) continue;
                    int c = spr[(int) v * 16 + (int) u];
                    if ((c >>> 24) < 40) continue;
                    int px = c | 0xFF000000;
                    float f;
                    if (near) {
                        f = 0.8f + 0.2f * Math.abs(cs);
                        float d = (u + v) / 32f - shine;
                        if (Math.abs(d) < 0.06f) f += 0.55f * (1 - Math.abs(d) / 0.06f);
                    } else if (face) f = 0.5f;
                    else f = 0.62f + 0.12f * j;
                    px = Canvas.shade(px, f);
                    if (!near) px = Canvas.mix(px, 0xFF000000 | tint, face ? 0.18f : 0.38f);                 // the edge of the slab carries the drop's colour
                    IT.set(x, y, (px & 0xFFFFFF) | (al << 24));
                }
            }
        }
    }

    /** What makes the RNG drop grand, and different for each kind: floor light, orbits, speed lines. front = the part drawn over the item. */
    private static void rngAura(Canvas c, int style, float sx, float sy, float size, float us, float uu, int rar, boolean front) {
        float k = cl(uu) * cl(us / 0.25f + 0.2f);
        if (k <= 0.02f) return;
        int rc = rar & 0xFFFFFF, gold = 0xFFD36B, ix = Math.round(sx), fy = (int) CY - 3;
        if (style == 0) {                                                                              // slayer: a golden pool of light, rings sent out along the floor, speed lines, a lens star
            if (!front) {
                for (int i = 0; i < 3; i++) ell(c, ix, fy, 26 - i * 7, 8 - i * 2, Canvas.rgb((int) (34 * k), i == 2 ? 0xFFFFFF : gold));
                for (int r = 0; r < 2; r++) {
                    float rp = frac(us * 0.8f - r * 0.5f); int rx = (int) (8 + 36 * rp), n = Math.max(40, rx * 5);
                    for (int i = 0; i < n; i++) { double a = i * Math.PI * 2 / n; c.set((int) (ix + Math.cos(a) * rx), (int) (fy + Math.sin(a) * rx * 0.3), Canvas.rgb((int) (170 * k * (1 - rp)), r == 0 ? gold : 0xFFFFFF)); }
                }
            } else {
                if (us < 1.1f) for (int i = 0; i < 7; i++) {                                         // speed lines while it spins up
                    float yy = sy + (h(i, 41) - 0.5f) * size * 1.4f, len = size * (0.5f + h(i, 42)), off = ((us * 3f + h(i, 43)) % 1f) * size * 2.4f - size * 1.2f;
                    int a = (int) (150 * (1f - us / 1.1f));
                    for (int q = 0; q < len; q++) c.set((int) (sx + off + q - len / 2), (int) yy, Canvas.rgb((int) (a * (1f - q / len)), 0xFFFFFF));
                }
                float L = size * (0.9f + 0.25f * (float) Math.sin(us * 4)), ab = 0.5f * k;
                for (int q = -(int) L; q <= (int) L; q++) { float e = 1f - Math.abs(q) / (L + 1); c.set(ix + q, Math.round(sy), Canvas.rgb((int) (190 * e * ab), 0xFFFFFF)); c.set(ix, Math.round(sy) + q, Canvas.rgb((int) (190 * e * ab), 0xFFFFFF)); }
            }
        } else if (style == 1) {                                                                       // mythological: a rune circle on the floor and a golden spiral winding up around it
            if (!front) {
                for (int rr = 0; rr < 2; rr++) {
                    int rx = rr == 0 ? 26 : 17, n = rx * 6;
                    for (int i = 0; i < n; i++) { double a = i * Math.PI * 2 / n + us * (rr == 0 ? 0.6 : -0.9); boolean tick = i % 9 < 2; c.set((int) (ix + Math.cos(a) * rx), (int) (fy + Math.sin(a) * rx * 0.3), Canvas.rgb((int) ((tick ? 230 : 110) * k), tick ? 0xFFF3B8 : gold)); }
                }
                c.glow(ix, fy, 22, gold, 0.28f * k);
            }
            int n = 22;
            for (int i = 0; i < n; i++) {
                float t0 = frac(us * 0.42f + i / (float) n), a = us * 2.4f + i * 0.62f;
                boolean fr = Math.sin(a) > 0;
                if (fr != front) continue;
                float rad = 15f + size * 0.25f * (float) Math.sin(t0 * Math.PI);
                int x = (int) (sx + Math.cos(a) * rad), y = (int) (fy - t0 * (fy - sy + size * 0.8f) + Math.sin(a) * rad * 0.28f);
                c.glint(x, y, i % 4 == 0 ? 2 : 1, i % 3 == 0 ? 0xFFFFFF : gold, k * (float) Math.sin(t0 * Math.PI));
            }
        } else {                                                                                       // experiment: rainbow atom orbits around the drop and bubbles rising off the table
            if (!front) {
                for (int i = 0; i < 9; i++) {
                    float ph = frac(us * 0.45f + i * 0.123f), bx = sx + (h(i, 51) - 0.5f) * 44 + (float) Math.sin(ph * 8 + i) * 3, by = fy - ph * 62;
                    int cc = hsv(i * 0.11f + us * 0.2f, 0.5f, 1f) & 0xFFFFFF, br = 2 + i % 3, a = (int) (190 * k * Math.sin(ph * Math.PI));
                    for (int q = 0; q < 14; q++) { double an = q * Math.PI * 2 / 14; c.set((int) (bx + Math.cos(an) * br), (int) (by + Math.sin(an) * br), Canvas.rgb(a, cc)); }
                }
                c.glow(ix, fy, 20, 0xB56BFF, 0.22f * k);
            }
            for (int o = 0; o < 3; o++) {
                double tilt = o * Math.PI / 3 + us * 0.35; float R = size * 0.95f;
                for (int j = 0; j < 5; j++) {
                    double a = us * 3.2 + j * 1.2566 + o * 2.1;
                    boolean fr = Math.sin(a) > 0; if (fr != front) continue;
                    double lx = Math.cos(a) * R, ly = Math.sin(a) * R * 0.34;
                    int x = (int) (sx + lx * Math.cos(tilt) - ly * Math.sin(tilt)), y = (int) (sy + lx * Math.sin(tilt) * 0.55 + ly * Math.cos(tilt));
                    c.glint(x, y, fr ? (j == 0 ? 3 : 2) : 1, hsv(o * 0.33f + j * 0.07f + us * 0.2f, 0.5f, 1f) & 0xFFFFFF, k * (fr ? 1f : 0.5f));
                }
            }
        }
    }

    /** Sword trails and the flashes / shockwaves of the big slices. */
    private static void impactExtras(Canvas out, Params p, float t, int ct) {
        for (int j = 1; j <= 9; j++) {                                          // a fading trail behind the sword on the dramatic slices
            double[] s = sword(p, t - j * 0.012f);
            if (s == null) continue;
            int d = drama(p, (int) s[3]); if (d == 0) continue;
            float f = 1 - j / 10f;
            int col = d == 2 ? (j % 2 == 0 ? 0xFFFFFFFF : 0xFFFFD36B) : (j % 2 == 0 ? 0xFFFFFFFF : p.rarity);
            int x = (int) Math.round(s[0]), y = (int) Math.round(s[1]), w = d == 2 ? 3 : 2;
            out.rect(x - w, y - w, x + w, y + w, Canvas.rgb((int) ((d == 2 ? 170 : 120) * f), col));
        }
        for (int k = 2; k < p.chops; k++) {
            int d = drama(p, k); float a = t - impact(p, k);
            if (a < 0 || a > 0.3f) continue;
            if (d == 2 && a < 0.1f) {
                int fl = (int) (80 * (1 - a / 0.1f));
                for (int i = 0; i < 4; i++) out.disc((int) CX, ct, 10 + i * 12, Canvas.rgb(fl / 2, 0xFFFFFFFF));
            }
            float rp = a / 0.3f, r = 6 + (d == 2 ? 46 : 28) * easeOut(rp);
            int n = Math.max(40, (int) (r * 5));
            for (int i = 0; i < n; i++) {
                double an = i * Math.PI * 2 / n;
                out.set((int) (CX + Math.cos(an) * r), (int) (CY - 2 + Math.sin(an) * r * 0.35), Canvas.rgb((int) (190 * (1 - rp)), d == 2 ? 0xFFFFE0A0 : p.rarity));
            }
        }
    }

    // ---- little drawing helpers for the scenery
    private static void line(Canvas c, int x0, int y0, int x1, int y1, int argb) {
        int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)); if (n == 0) { c.set(x0, y0, argb); return; }
        for (int i = 0; i <= n; i++) c.set(x0 + Math.round((x1 - x0) * i / (float) n), y0 + Math.round((y1 - y0) * i / (float) n), argb);
    }
    private static void tri(Canvas c, int x0, int y0, int x1, int y1, int x2, int y2, int argb) {
        int minx = Math.min(x0, Math.min(x1, x2)), maxx = Math.max(x0, Math.max(x1, x2)), miny = Math.min(y0, Math.min(y1, y2)), maxy = Math.max(y0, Math.max(y1, y2));
        for (int y = miny; y <= maxy; y++) for (int x = minx; x <= maxx; x++) {
            float d1 = (x - x1) * (y0 - y1) - (x0 - x1) * (y - y1), d2 = (x - x2) * (y1 - y2) - (x1 - x2) * (y - y2), d3 = (x - x0) * (y2 - y0) - (x2 - x0) * (y - y0);
            boolean neg = d1 < 0 || d2 < 0 || d3 < 0, pos = d1 > 0 || d2 > 0 || d3 > 0;
            if (!(neg && pos)) c.set(x, y, argb);
        }
    }
    private static int al(float in, int a, int rgb) { return Canvas.rgb((int) (a * in), rgb); }

    /** Zombie: a moonlit graveyard - tombstones, a dead tree, an iron fence, drifting fog. */
    private static void graveyard(Canvas c, float t, float in) {
        ell(c, 92, 14, 17, 17, al(in, 14, 0xCFE6C0)); ell(c, 92, 14, 12, 12, al(in, 24, 0xCFE6C0));
        c.disc(92, 14, 7, al(in, 215, 0xE8F0D8)); c.disc(94, 12, 2, al(in, 60, 0x9AA890)); c.disc(90, 17, 1, al(in, 60, 0x9AA890));
        int fc = al(in, 200, 0x22232B);
        for (int i = 0; i < 11; i++) { int x = 62 + i * 5; line(c, x, 70, x, 54 + (i % 2), fc); c.set(x - 1, 55 + (i % 2), fc); c.set(x + 1, 55 + (i % 2), fc); }
        c.rect(61, 58, 118, 59, fc); c.rect(61, 65, 118, 66, fc);
        int tr = al(in, 215, 0x2A1F1A);
        c.rect(13, 34, 16, 74, tr);
        line(c, 14, 44, 4, 32, tr); line(c, 14, 40, 24, 28, tr); line(c, 15, 52, 25, 44, tr); line(c, 14, 36, 8, 22, tr); line(c, 4, 32, 2, 26, tr); line(c, 24, 28, 28, 24, tr); line(c, 24, 28, 25, 20, tr);
        int[][] gv = {{22, 62, 10, 14, 0}, {34, 66, 8, 11, 1}, {96, 63, 10, 14, 2}, {83, 67, 8, 10, 0}, {8, 68, 8, 10, 1}};
        for (int[] g : gv) {
            int x = g[0], y = g[1], w = g[2], hh = g[3];
            c.rect(x, y, x + w, y + hh, al(in, 225, 0x727A86));
            c.rect(x + 1, y - 1, x + w - 1, y, al(in, 225, 0x727A86)); c.rect(x + 2, y - 2, x + w - 2, y - 1, al(in, 225, 0x727A86));
            c.rect(x + w - 2, y, x + w, y + hh, al(in, 225, 0x4F5560));
            if (g[4] == 0) { c.rect(x + w / 2 - 1, y + 1, x + w / 2, y + 7, al(in, 200, 0xA9B1BC)); c.rect(x + w / 2 - 3, y + 3, x + w / 2 + 2, y + 4, al(in, 200, 0xA9B1BC)); }
            else if (g[4] == 1) { c.rect(x + 2, y + 3, x + w - 2, y + 4, al(in, 190, 0x4F5560)); c.rect(x + 2, y + 6, x + w - 3, y + 7, al(in, 190, 0x4F5560)); }
            else { c.rect(x + 2, y + 2, x + w - 2, y + 3, al(in, 190, 0x4F5560)); c.rect(x + 3, y + 5, x + w - 3, y + 6, al(in, 190, 0x4F5560)); }
            c.rect(x, y + hh - 2, x + 3, y + hh, al(in, 200, 0x3E6B34)); c.rect(x + w - 4, y + hh - 1, x + w, y + hh, al(in, 200, 0x3E6B34));
            c.rect(x - 2, y + hh, x + w + 2, y + hh + 2, al(in, 170, 0x3B2A1E));
        }
        for (int i = 0; i < 5; i++) { float ph = frac(t * 0.05 + i * 0.2); int x = (int) (ph * (W + 60)) - 30; ell(c, x, 72 + (i % 3) * 3, 26, 3, al(in, 34, 0xB8CCB8)); }
        int cx2 = (int) ((t * 14) % (W + 40)) - 20, cy2 = 18 + (int) (Math.sin(t * 2) * 3), fl = ((int) (t * 7)) % 2;
        int bk = al(in, 230, 0x0E0E12);
        c.rect(cx2, cy2, cx2 + 3, cy2 + 1, bk); c.set(cx2 - 2, cy2 - fl, bk); c.set(cx2 + 5, cy2 - fl, bk);
    }

    /** Sven: a sunny park - trees, a bench, a lamp post, grass, flowers and butterflies. */
    private static void park(Canvas c, float t, float in) {
        for (int k = 0; k < 2; k++) {
            int tx = k == 0 ? 13 : 100, th = k == 0 ? 30 : 22, r = k == 0 ? 12 : 9;
            c.rect(tx - 2, 72 - th, tx + 2, 74, al(in, 225, 0x5B3A22));
            c.disc(tx, 72 - th - 4, r, al(in, 220, 0x2F7A34)); c.disc(tx - r / 2, 72 - th, r - 3, al(in, 220, 0x3E8E3A)); c.disc(tx + r / 2, 72 - th, r - 3, al(in, 220, 0x3E8E3A));
            c.disc(tx - 3, 72 - th - 7, r / 2, al(in, 200, 0x5CB04C));
        }
        c.rect(78, 64, 98, 66, al(in, 230, 0x8A5A2E)); c.rect(78, 57, 98, 59, al(in, 230, 0x8A5A2E)); c.rect(78, 59, 79, 64, al(in, 230, 0x6B4423)); c.rect(97, 59, 98, 64, al(in, 230, 0x6B4423));
        c.rect(80, 66, 82, 72, al(in, 230, 0x3A2A1C)); c.rect(94, 66, 96, 72, al(in, 230, 0x3A2A1C));
        c.rect(31, 36, 33, 74, al(in, 230, 0x2C3038)); c.rect(28, 32, 36, 36, al(in, 230, 0x2C3038)); c.rect(29, 33, 35, 36, al(in, 240, 0xFFE9A0));
        float glow = 1f + 0.1f * (float) Math.sin(t * 3);
        ell(c, 32, 35, (int) (10 * glow), (int) (8 * glow), al(in, 26, 0xFFE9A0)); ell(c, 32, 35, 5, 4, al(in, 44, 0xFFE9A0));
        for (int x = 0; x < W; x += 3) {
            int bh = 3 + (int) (h(x, 5) * 4), sway = (int) Math.round(Math.sin(t * 2.2 + x * 0.4) * 1.2);
            int g = h(x, 6) > 0.5f ? 0x4FA043 : 0x3A8A38;
            line(c, x, 78, x + sway, 78 - bh, al(in, 220, g)); line(c, x + 1, 78, x + 1 + sway, 78 - bh + 1, al(in, 200, g));
        }
        int[] fcols = {0xFF6B8A, 0xFFE066, 0xFFFFFF, 0xB388FF};
        for (int i = 0; i < 14; i++) { int x = 3 + i * 8 + (int) (h(i, 9) * 4), y = 76 - (int) (h(i, 3) * 3), col = fcols[i % 4];
            c.set(x, y, al(in, 240, col)); c.set(x - 1, y, al(in, 200, col)); c.set(x + 1, y, al(in, 200, col)); c.set(x, y - 1, al(in, 200, col)); c.set(x, y + 1, al(in, 200, 0x3A8A38)); }
        for (int i = 0; i < 2; i++) {
            double an = t * 0.8 + i * 3; int x = (int) (56 + Math.cos(an) * 40), y = (int) (30 + Math.sin(an * 1.7) * 12 + i * 10), fl = ((int) (t * 9 + i)) % 2;
            int col = i == 0 ? 0xFFA8D0 : 0xFFE066;
            c.set(x, y, al(in, 255, 0x3A2A1C)); c.set(x - 1, y - fl, al(in, 235, col)); c.set(x + 1, y - fl, al(in, 235, col)); c.set(x - 1, y + 1 - fl, al(in, 200, col)); c.set(x + 1, y + 1 - fl, al(in, 200, col));
        }
        for (int i = 0; i < 7; i++) { float ph = frac(t * 0.12 + i * 0.143); int x = (int) (ph * W + Math.sin(ph * 12 + i) * 5), y = (int) (4 + ph * 66);
            c.rect(x, y, x + 2, y + 1, al(in, 200, i % 2 == 0 ? 0x6DBF4B : 0xE0A030)); }
    }

    /** Voidgloom: the End - stars, obsidian pillars with end crystals, floating islands, and the dragon passing over. */
    private static void theEnd(Canvas c, float t, float in) {
        ell(c, 10, 20, 26, 26, al(in, 16, 0xB060FF)); ell(c, 104, 70, 30, 24, al(in, 14, 0x8040E0));
        for (int i = 0; i < 46; i++) { int x = (int) (h(i, 1) * W), y = (int) (h(i, 2) * 70); float tw = 0.5f + 0.5f * (float) Math.sin(t * 2 + i * 1.7);
            int col = i % 3 == 0 ? 0xFFFFFF : i % 3 == 1 ? 0xD9B8FF : 0x9A78FF;
            c.set(x, y, al(in * tw, 230, col)); if (i % 7 == 0) { c.set(x - 1, y, al(in * tw, 140, col)); c.set(x + 1, y, al(in * tw, 140, col)); c.set(x, y - 1, al(in * tw, 140, col)); c.set(x, y + 1, al(in * tw, 140, col)); } }
        int[][] isl = {{78, 12, 12}, {24, 30, 9}, {100, 38, 7}};
        for (int[] s : isl) { int bob = (int) Math.round(Math.sin(t * 0.9 + s[0]) * 1.5); ell(c, s[0], s[1] + bob, s[2], 2, al(in, 170, 0xD8D49A));
            tri(c, s[0] - s[2] + 2, s[1] + bob + 1, s[0] + s[2] - 2, s[1] + bob + 1, s[0], s[1] + bob + s[2], al(in, 150, 0x8E8A58)); }
        for (int k = 0; k < 2; k++) {
            int px = k == 0 ? 6 : 96, top = k == 0 ? 24 : 36, w = 10;
            c.rect(px, top, px + w, 76, al(in, 235, 0x15101F)); c.rect(px, top, px + 2, 76, al(in, 235, 0x2A2140)); c.rect(px + w - 2, top, px + w, 76, al(in, 235, 0x0A0710));
            for (int y = top + 4; y < 76; y += 9) c.rect(px + 2, y, px + w - 2, y + 1, al(in, 120, 0x3A2C5C));
            int bob = (int) Math.round(Math.sin(t * 2 + k * 2) * 1.5), cx2 = px + w / 2, cy2 = top - 7 + bob;
            for (int i = 0; i < 3; i++) c.rect(cx2 - 4, cy2 - 4 + i * 4, cx2 + 5, cy2 - 3 + i * 4, al(in, 200, 0x5B5B66));
            ell(c, cx2, cy2, 9, 9, al(in, 30, 0xFF70E0));
            tri(c, cx2, cy2 - 4, cx2 - 3, cy2, cx2 + 3, cy2, al(in, 240, 0xFF8AF0)); tri(c, cx2, cy2 + 4, cx2 - 3, cy2, cx2 + 3, cy2, al(in, 240, 0xC040C0));
            c.set(cx2 - 1, cy2 - 1, al(in, 255, 0xFFFFFF));
        }
        float ph = (t * 0.1f) % 1.6f; int dx = (int) (ph * (W + 70)) - 40, dy = 14 + (int) (Math.sin(t * 1.3) * 3);
        int dk = al(in, 235, 0x1A1226); int flap = (int) Math.round(Math.sin(t * 5) * 5);
        c.rect(dx, dy, dx + 12, dy + 3, dk); c.rect(dx + 12, dy - 1, dx + 16, dy + 2, dk); c.set(dx + 14, dy, al(in, 255, 0xFF55FF)); line(c, dx, dy + 1, dx - 10, dy + 4, dk);
        tri(c, dx + 3, dy, dx + 9, dy, dx + 4, dy - 9 - flap, dk); tri(c, dx + 3, dy + 3, dx + 9, dy + 3, dx + 4, dy + 8 + flap, dk);
    }

    /** Inferno Demonlord: the Nether - netherrack ceiling with glowstone, lava falls and pools, a fortress bridge and a ghast. */
    private static void nether(Canvas c, float t, float in) {
        for (int i = 0; i < 6; i++) ell(c, 8 + i * 20, 98, 22, 12, al(in, 40, 0xFF5A00));
        for (int x = 0; x < W; x++) {
            int d = 4 + (int) (h(x / 3, 4) * 9 * (0.5 + 0.5 * Math.abs(Math.sin(x * 0.11))));
            c.rect(x, 0, x + 1, d, al(in, 235, h(x, 8) > 0.5f ? 0x6B2226 : 0x5A1C20));
        }
        int[][] gs = {{20, 4}, {58, 3}, {96, 5}};
        for (int[] g : gs) { ell(c, g[0], g[1] + 3, 10, 8, al(in, 30, 0xFFD060)); c.rect(g[0] - 3, g[1], g[0] + 4, g[1] + 6, al(in, 235, 0xE8B84A)); c.rect(g[0] - 1, g[1] + 6, g[0] + 2, g[1] + 9, al(in, 235, 0xF4D070)); c.set(g[0], g[1] + 2, al(in, 255, 0xFFF4B0)); }
        int br = al(in, 170, 0x2B0F14);
        c.rect(0, 56, W, 61, br);
        for (int x = 8; x < W; x += 28) { c.rect(x - 2, 61, x + 3, 76, br); for (int i = 0; i < 6; i++) { c.set(x - 6 + i, 61 + i / 2, br); c.set(x + 6 - i, 61 + i / 2, br); } }
        for (int x = 0; x < W; x += 6) c.rect(x, 53, x + 3, 56, br);
        for (int k = 0; k < 2; k++) {
            int fx = k == 0 ? 14 : 100;
            c.rect(fx, 8, fx + 3, 78, al(in, 215, 0xFF6A1A));
            for (int i = 0; i < 9; i++) { float ph = frac(t * 0.9 + i * 0.113 + k * 0.4); c.rect(fx, (int) (8 + ph * 70), fx + 2, (int) (8 + ph * 70) + 3, al(in, 230, 0xFFE070)); }
            ell(c, fx + 1, 79, 8, 2, al(in, 200, 0xFFA030));
        }
        ell(c, 10, 84, 22, 6, al(in, 215, 0xD94A0A)); ell(c, 102, 84, 24, 6, al(in, 215, 0xD94A0A)); ell(c, 10, 83, 16, 3, al(in, 200, 0xFF8A20)); ell(c, 102, 83, 18, 3, al(in, 200, 0xFF8A20));
        for (int i = 0; i < 8; i++) { float ph = frac(t * 0.8 + i * 0.19); int x = i < 4 ? 2 + i * 8 : 84 + (i - 4) * 8; if (ph < 0.5f) c.set(x, 82 - (int) (ph * 8), al(in * (1 - ph * 2), 255, 0xFFE070)); }
        int gx = 88 + (int) (Math.sin(t * 0.5) * 4), gy2 = 20 + (int) (Math.sin(t * 0.9) * 3);
        c.rect(gx, gy2, gx + 14, gy2 + 14, al(in, 235, 0xEDEDED)); c.rect(gx, gy2 + 12, gx + 14, gy2 + 14, al(in, 235, 0xD0D0D0));
        c.rect(gx + 3, gy2 + 5, gx + 5, gy2 + 6, al(in, 255, 0x3A3A3A)); c.rect(gx + 9, gy2 + 5, gx + 11, gy2 + 6, al(in, 255, 0x3A3A3A)); c.rect(gx + 5, gy2 + 9, gx + 9, gy2 + 10, al(in, 255, 0x3A3A3A));
        for (int i = 0; i < 5; i++) { int tx = gx + 1 + i * 3, ln = 5 + (int) (Math.sin(t * 3 + i) * 2); c.rect(tx, gy2 + 14, tx + 1, gy2 + 14 + ln, al(in, 235, 0xD0D0D0)); }
    }

    /** Riftstalker: a vampire movie - a huge blood moon, a gothic castle with lit windows, bats, mist and lightning. */
    private static void vampireMovie(Canvas c, float t, float in) {
        ell(c, 84, 22, 30, 30, al(in, 12, 0xFF3030)); ell(c, 84, 22, 22, 22, al(in, 20, 0xFF4040)); ell(c, 84, 22, 17, 17, al(in, 30, 0xFF5050));
        c.disc(84, 22, 14, al(in, 235, 0xF4E4D4)); c.disc(79, 18, 3, al(in, 60, 0xB89A88)); c.disc(90, 27, 4, al(in, 55, 0xB89A88)); c.disc(88, 14, 2, al(in, 50, 0xB89A88));
        int cs = al(in, 240, 0x0C0810);
        c.rect(10, 42, 36, 76, cs);
        c.rect(3, 30, 13, 76, cs); tri(c, 2, 30, 14, 30, 8, 14, cs);
        c.rect(32, 34, 44, 76, cs); tri(c, 31, 34, 45, 34, 38, 18, cs);
        c.rect(20, 34, 26, 42, cs); tri(c, 19, 34, 27, 34, 23, 24, cs);
        for (int x = 10; x < 36; x += 5) c.rect(x, 39, x + 3, 42, cs);
        float fl = 0.75f + 0.25f * (float) Math.sin(t * 7 + 1.3);
        int[][] win = {{6, 38}, {6, 50}, {36, 42}, {36, 54}, {16, 50}, {28, 50}, {22, 60}, {23, 28}};
        for (int[] w : win) { c.rect(w[0], w[1], w[0] + 3, w[1] + 5, al(in * fl, 255, 0xFFC040)); ell(c, w[0] + 1, w[1] + 2, 5, 5, al(in * fl, 24, 0xFFC040)); }
        for (int i = 0; i < 6; i++) { float ph = frac(t * 0.04 + i * 0.17); int x = (int) (ph * (W + 60)) - 30; ell(c, x, 70 + (i % 3) * 3, 28, 3, al(in, 36, 0xC8B0B8)); }
        for (int b = 0; b < 5; b++) {
            float ph = frac(t * 0.12 + b * 0.21); int x = (int) (ph * (W + 40)) - 20, y = 10 + b * 7 + (int) (Math.sin(ph * 12 + b) * 4), wf = (int) Math.round(Math.sin(t * 14 + b) * 2);
            int bc = al(in, 240, 0x120508);
            c.rect(x - 1, y, x + 2, y + 2, bc); c.set(x - 1, y - 1, bc); c.set(x + 1, y - 1, bc);
            line(c, x - 2, y, x - 7, y - 2 - wf, bc); line(c, x - 7, y - 2 - wf, x - 5, y + 1, bc); line(c, x + 2, y, x + 7, y - 2 - wf, bc); line(c, x + 7, y - 2 - wf, x + 5, y + 1, bc);
        }
        float lc = (t % 7f);
        if (lc > 6.5f && lc < 6.62f) {
            c.rect(0, 0, W, H, al(in, 36, 0xFFFFFF));
            int bx = 62; for (int y = 0; y < 44; y += 4) { int nx = bx + (int) ((h(y, 7) - 0.5f) * 10); line(c, bx, y, nx, y + 4, al(in, 255, 0xE8EEFF)); bx = nx; }
        }
        int vg = al(in, 24, 0xA00020);
        ell(c, 0, 0, 22, 18, vg); ell(c, W, 0, 22, 18, vg); ell(c, 0, H, 22, 18, vg); ell(c, W, H, 22, 18, vg);
    }

    /** Each slayer's own flavour: drifting spores, webs, claw marks, portal sparks, embers or mist. */
    private static void ambient(Canvas c, Params p, float t) {
        if (p.theme > 5) return;
        float in = cl(t / 0.6f);
        switch (p.theme) {
            case 0 -> { graveyard(c, t, in); for (int i = 0; i < 10; i++) { float ph = frac(t * 0.22 + i * 0.173); int x = (int) (CX + (frac(i * 0.61f) - 0.5f) * 96 + Math.sin(ph * 6 + i) * 4), y = (int) (CY - 4 - ph * 70);
                c.rect(x, y, x + 2, y + 2, Canvas.rgb((int) (130 * Math.sin(ph * Math.PI) * in), i % 2 == 0 ? 0xFF9BE564 : 0xFF4E8F3A)); } }
            case 1 -> { for (int sgn = -1; sgn <= 1; sgn += 2) { int ox = sgn < 0 ? 2 : W - 3;                                  // web strands from the top corners
                for (int j = 0; j < 5; j++) { double ang = (0.25 + j * 0.28); for (int s2 = 0; s2 < 34 + j * 4; s2++) c.set((int) (ox + sgn * -Math.cos(ang) * s2 * 1.0), (int) (2 + Math.sin(ang) * s2), Canvas.rgb((int) (70 * in * (1 - s2 / 46f)), 0xFFE0E0E8)); }
                for (int ring = 1; ring <= 3; ring++) for (int j = 0; j < 40; j++) { double ang = 0.25 + j * 0.0325 * 1.0; c.set((int) (ox - sgn * Math.cos(ang) * ring * 11), (int) (2 + Math.sin(ang) * ring * 11), Canvas.rgb((int) (55 * in), 0xFFE0E0E8)); } } }
            case 2 -> { park(c, t, in); for (int k = 0; k < p.chops; k++) { float a = t - impact(p, k); if (a < 0 || a > 0.35f) continue;           // triple claw marks on every chop
                float f = 1 - a / 0.35f; int ox = (int) CX + (k % 2 == 0 ? -22 : 14);
                for (int cl = 0; cl < 3; cl++) for (int s2 = 0; s2 < 30; s2++) c.set(ox + cl * 6 + s2 / 3, 18 + s2 + cl, Canvas.rgb((int) (190 * f * Math.min(1f, s2 / 8f)), 0xFFEAF4FF)); } }
            case 3 -> { theEnd(c, t, in); for (int i = 0; i < 22; i++) { float ph = frac(t * 0.3 + i * 0.137); double an = ph * 9 + i; int x = (int) (CX + Math.cos(an) * (10 + ph * 38)), y = (int) (CY - 8 - ph * 62 + Math.sin(an) * 6);
                c.rect(x, y, x + 2, y + 2, Canvas.rgb((int) (200 * Math.sin(ph * Math.PI) * in), i % 3 == 0 ? 0xFFFF55FF : 0xFFB060FF)); } }
            case 4 -> { nether(c, t, in); for (int i = 0; i < 26; i++) { float ph = frac(t * 0.55 + i * 0.211); int x = (int) (CX + (frac(i * 0.7548f) - 0.5f) * 80 + Math.sin(ph * 9 + i) * 3), y = (int) (CY - 2 - ph * 78);
                c.set(x, y, Canvas.rgb((int) (230 * Math.sin(ph * Math.PI) * in), i % 3 == 0 ? 0xFFFFE070 : 0xFFFF7A1A)); if (i % 4 == 0) c.set(x, y + 1, Canvas.rgb((int) (120 * Math.sin(ph * Math.PI) * in), 0xFFFF3A00)); } }
            case 5 -> { vampireMovie(c, t, in); for (int b = 0; b < 0; b++) { float ph = frac(t * 0.18 + b * 0.33); int x = (int) (ph * (W + 20)) - 10, y = 12 + b * 9 + (int) (Math.sin(ph * 14) * 3), fl = ((int) (t * 8) + b) % 2;   // little bats
                int col = Canvas.rgb((int) (170 * in), 0xFF3A0A14); c.set(x, y, col); c.set(x - 1, y - fl, col); c.set(x + 1, y - fl, col); c.set(x - 2, y - 1 + 2 * fl, col); c.set(x + 2, y - 1 + 2 * fl, col); }
                for (int i = 0; i < 14; i++) { float ph = frac(t * 0.4 + i * 0.29); int x = (int) (CX + (frac(i * 0.43f) - 0.5f) * 70), y = (int) (6 + ph * 62);
                    c.rect(x, y, x + 1, y + 2, Canvas.rgb((int) (150 * Math.sin(ph * Math.PI) * in), 0xFFC01830)); } }
            default -> {}
        }
    }

    private static double itemY(float uu) { return 18 + 30 * easeOut(uu); }      // the drop rises clear of the two halves

    private static int itemScreenY(float t, float ts) {
        float uu = (t - (ts + 0.12f)) / 0.35f;
        return (int) Math.round(CY - itemY(uu) * Math.cos(TILT));
    }

    // a two-drop scene gets one light circle per item, not one in the middle
    private static double bcx = CX; private static int brar; private static float bsc = 1f;
    private static void twinPass(Canvas c, Params p, float t, float ts, float us, boolean cool, boolean epic, int ct, boolean fore) {
        if (p.twin && p.item2 != null) {
            for (int s = -1; s <= 1; s += 2) {
                bcx = CX + s * TWIN_DX; brar = s < 0 ? p.rarity : p.rarity2; bsc = 0.6f;
                float u2 = s > 0 ? Math.max(0f, us - 0.3f) : us;
                if (fore) foreground(c, p, t, ts, u2, cool, epic, ct); else background(c, p, t, ts, u2, cool, epic);
            }
            bcx = CX; brar = 0; bsc = 1f;
        } else if (fore) foreground(c, p, t, ts, us, cool, epic, ct); else background(c, p, t, ts, us, cool, epic);
    }

    private static void background(Canvas c, Params p, float t, float ts, float us, boolean cool, boolean epic) {
        if (!p.hasItem) return;
        int cy = itemScreenY(t, ts), rar = brar != 0 ? brar : p.rarity, light = Canvas.mix(rar, 0xFFFFFFFF, 0.5f);
        float in = cl(us / 0.12f);
        if (!cool) {                                                       // the plain scene still gets a soft glow so the item reads clearly
            for (int i = 0; i < 3; i++) c.disc((int) bcx, cy, 18 - i * 5, Canvas.rgb((int) (in * (22 + i * 14)), i < 2 ? rar : light));
            return;
        }
        // light beam rising from the board
        for (int y = 0; y < CY; y++) {
            float f = 1f - y / (float) CY;
            int half = 3 + (int) (2 * f) + (epic ? 2 : 0);
            for (int x = -half; x <= half; x++) {
                float edge = 1f - Math.abs(x) / (float) (half + 1);
                c.set((int) bcx + x, y, Canvas.rgb((int) (in * (epic ? 120 : 90) * f * edge * (0.8f + 0.2f * Math.sin(us * 9 + y * 0.3))), x * x < 3 ? light : rar));
            }
        }
        // glow
        float pulse = 1f + 0.07f * (float) Math.sin(us * 9);
        int[] rr = {(int) (28 * pulse * bsc), (int) (20 * pulse * bsc), (int) (12 * pulse * bsc)};
        for (int i = 0; i < 3; i++) c.disc((int) bcx, cy, rr[i] + (epic ? 6 : 0), Canvas.rgb((int) (in * (28 + i * 22)), i < 2 ? rar : light));
        // rotating rays
        int rays = epic ? 14 : 10; float len = (epic ? 62 : 46) * bsc;
        for (int i = 0; i < rays; i++) {
            double ang = us * (epic ? 0.8 : 0.5) + i * (Math.PI * 2 / rays);
            float L = len * (i % 2 == 0 ? 1f : 0.62f);
            for (int s = 5; s < L; s++) {
                float f = s / L; int a = (int) (in * (epic ? 135 : 100) * (1 - f) * (1 - f * 0.4f));
                int w = Math.max(1, Math.round(3.2f * (1 - f)));
                int x = (int) (bcx + Math.cos(ang) * s), y = (int) (cy + Math.sin(ang) * s * 0.9);
                c.rect(x - w / 2, y - w / 2, x - w / 2 + w, y - w / 2 + w, Canvas.rgb(a, epic && i % 3 == 0 ? 0xFFE9A0 : light));
            }
        }
    }

    private static void foreground(Canvas c, Params p, float t, float ts, float us, boolean cool, boolean epic, int ct) {
        int rar = brar != 0 ? brar : p.rarity;
        if (!p.hasItem) {                                                  // nothing dropped: a small grey puff and that's it
            for (int i = 0; i < 9; i++) {
                float a = (t - ts) / 1.0f; if (a < 0 || a > 1) continue;
                int x = (int) (bcx + (h(i, 1) - 0.5f) * 26 + (h(i, 2) - 0.5f) * 10 * a), y = (int) (ct + 6 - 18 * a * (0.5f + h(i, 3)));
                c.rect(x, y, x + 2, y + 2, Canvas.rgb((int) (160 * (1 - a)), 0xB8B8C0));
            }
            return;
        }
        int cy = itemScreenY(t, ts);
        // burst of cream / rarity sparks
        float life = epic ? 0.7f : cool ? 0.5f : 0.38f, pt = us / life;
        if (pt < 1) for (int i = 0; i < (epic ? 70 : cool ? 38 : 16); i++) {
            double ang = i * 2.399963; float sp = (0.45f + 0.9f * frac(i * 0.618f)) * (epic ? 50 : 32) * bsc;
            int x = (int) (bcx + Math.cos(ang) * sp * easeOut(pt)), y = (int) (cy + Math.sin(ang) * sp * easeOut(pt) + 10 * pt * pt);
            int col = i % 3 == 0 ? rar : (epic && i % 3 == 1) ? 0xFFFFC840 : 0xFFFFF2C0;
            int sz = (epic && i % 7 == 0) ? 2 : 1;
            c.rect(x, y, x + sz, y + sz, Canvas.rgb((int) (255 * (1 - pt)), col));
        }
        if (cool) {
            for (int k = 0; k < (epic ? 2 : 1); k++) {                      // shockwave rings
                float rp = cl((us - 0.07f * k) / 0.5f); if (rp <= 0 || rp >= 1) continue;
                float r = 4 + (epic ? 56 : 42) * bsc * easeOut(rp);
                int n = Math.max(48, (int) (r * 5));
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n;
                    c.set((int) (bcx + Math.cos(a) * r), (int) (cy + Math.sin(a) * r * 0.9), Canvas.rgb((int) (210 * (1 - rp)), k == 0 ? 0xFFFFFF : rar));
                }
            }
            for (int i = 0; i < (epic ? 22 : 14); i++) {                    // floating runes
                float ph = frac(us * 0.55 + i * 0.137);
                int x = (int) (bcx + (h(i, 3) - 0.5f) * 64 * bsc), y = (int) (CY - 6 - ph * 68);
                int a = (int) (240 * Math.sin(ph * Math.PI) * cl(us / 0.2f));
                int col = i % 2 == 0 ? Canvas.mix(rar, 0xFFFFFFFF, 0.35f) : 0xFFC77DFF;
                int[] g = GLYPH[i % GLYPH.length];
                for (int gy = 0; gy < 5; gy++) for (int gx = 0; gx < 3; gx++) if ((g[gy] >> (2 - gx) & 1) != 0) c.set(x + gx, y + gy, Canvas.rgb(a, col));
            }
        }
        if (cool && bsc >= 1f) special(c, p, us, cy, epic);
        if (epic) {
            for (int i = 0; i < 30; i++) {                                  // embers
                float ph = frac(us * 0.8 + i * 0.381);
                int x = (int) (bcx + (frac(i * 0.7548) - 0.5f) * 70 * bsc + Math.sin(ph * 7 + i) * 3), y = (int) (CY - ph * 72);
                c.set(x, y, Canvas.rgb((int) (230 * Math.sin(ph * Math.PI)), i % 2 == 0 ? 0xFFD35A : rar));
            }
            float fl = cl(1f - (t - ts) / 0.26f);
            if (fl > 0) for (int i = 0; i < 5; i++) c.disc((int) bcx, cy, (int) ((14 + i * 11) * bsc), Canvas.rgb((int) (95 * fl), 0xFFFFFF));
        } else if (cool) {
            float fl = cl(1f - (t - ts) / 0.18f);
            if (fl > 0) for (int i = 0; i < 3; i++) c.disc((int) bcx, cy, (int) ((10 + i * 9) * bsc), Canvas.rgb((int) (60 * fl), 0xFFFFFF));
        }
    }

    /** Rare Drop / RNG Drop, just before the final slice: light is drawn in from the whole frame into the mob, a ring tightens and the glow swells. */
    private static void charge(Canvas out, Params p, float t, float ts, int ct) {
        boolean epic = p.tier == 2;
        float len = epic ? 1.1f : 0.8f, f = cl((t - (ts - len)) / len);
        if (f <= 0f) return;
        int rc = p.rarity & 0xFFFFFF, cy = ct;
        out.glow((float) CX, cy, 14 + 44 * f, rc, 0.20f + 0.5f * f);
        out.glow((float) CX, cy, 8 + 18 * f, 0xFFFFFF, 0.15f + 0.45f * f * f);
        int n = (epic ? 34 : 22) / (p.style == 1 ? 3 : 1);
        for (int i = 0; i < n; i++) {                                                  // sparks streaming in from every side
            float ph = frac(f * 1.6f + h(i, 77)), r = 64 * (1f - ph) * (0.5f + 0.5f * h(i, 78));
            double a = h(i, 79) * Math.PI * 2 + f * 2.2;
            int x = (int) (CX + Math.cos(a) * r * 1.2), y = (int) (cy + Math.sin(a) * r * 0.75);
            if (i % 3 == 0) out.glint(x, y, 2, i % 2 == 0 ? 0xFFFFFF : rc, f); else out.set(x, y, Canvas.rgb((int) (230 * f), i % 2 == 0 ? 0xFFFFFF : rc));
        }
        float rr = 52 * (1f - f * f) + 6;                                              // a ring closing in
        int m = Math.max(40, (int) (rr * 5));
        for (int i = 0; i < m; i++) { double a = i * Math.PI * 2 / m + t * 2; out.set((int) (CX + Math.cos(a) * rr), (int) (cy + Math.sin(a) * rr * 0.62), Canvas.rgb((int) (200 * f), i % 6 < 3 ? 0xFFFFFF : rc)); }
        if (epic) {                                                                    // a heartbeat that quickens
            float beat = (float) Math.pow(Math.max(0, Math.sin(t * (8 + 14 * f))), 6);
            out.glow((float) CX, cy, 30 + 24 * beat, 0xFFD36B, 0.25f * f * beat);
        }
    }

    /** What makes the Rare Drop and the RNG Drop reveals special: orbiting halo rings of glints, a column of light dust, a lens-flare streak,
     *  and for the RNG Drop a rotating crown of long light blades, a golden shower from above and prism-coloured rings. */
    private static void special(Canvas c, Params p, float us, int cy, boolean epic) {
        int rc = p.rarity & 0xFFFFFF, gold = 0xFFD36B;
        float in = cl(us / 0.25f), out_ = us > 3.4f ? cl(1f - (us - 3.4f) / 1.2f) : 1f, k = in * out_;
        if (k <= 0.02f) return;
        int rings = epic ? 3 : 2;
        for (int r = 0; r < rings; r++) {                                              // tilted orbit rings of glints
            int cnt = (epic ? 16 : 12) / (p.style == 1 ? 2 : 1); float rad = 24 + r * 9, tilt = 0.28f + 0.18f * r, sp = (r % 2 == 0 ? 1f : -1f) * (0.9f + 0.25f * r);
            for (int i = 0; i < cnt; i++) {
                double a = us * sp + i * Math.PI * 2 / cnt + r;
                int x = (int) (CX + Math.cos(a) * rad), y = (int) (cy + Math.sin(a) * rad * tilt);
                boolean front = Math.sin(a) > 0;
                int col = epic ? 0xFF000000 | (java.awt.Color.HSBtoRGB((i / (float) cnt + us * 0.15f) % 1f, 0.45f, 1f) & 0xFFFFFF) : (i % 2 == 0 ? 0xFFFFFF : rc);
                c.glint(x, y, front ? (i % 4 == 0 ? 3 : 2) : 1, col & 0xFFFFFF, k * (front ? 1f : 0.5f));
            }
        }
        for (int i = 0; i < (epic ? 18 : 10) / (p.style == 1 ? 3 : 1); i++) {                                   // light dust lifting off the dais
            float ph = frac(us * 0.45 + i * 0.173);
            int x = (int) (CX + (h(i, 91) - 0.5f) * 20 + Math.sin(ph * 6 + i) * 3), y = (int) (CY - 2 - ph * 74);
            c.glint(x, y, 1 + (i % 3 == 0 ? 1 : 0), i % 2 == 0 ? 0xFFFFFF : rc, k * (float) Math.sin(ph * Math.PI));
        }
        float fs = cl(us / 0.5f), fa = 1f - cl((us - 0.5f) / 0.7f);                   // a lens-flare streak through the drop
        if (fa > 0.01f) {
            int half = (int) (8 + 52 * easeOut(fs));
            for (int x = -half; x <= half; x++) { float e = 1f - Math.abs(x) / (float) (half + 1); c.set((int) CX + x, cy, Canvas.rgb((int) (230 * e * fa), 0xFFFFFF)); c.set((int) CX + x, cy - 1, Canvas.rgb((int) (80 * e * fa), rc)); c.set((int) CX + x, cy + 1, Canvas.rgb((int) (80 * e * fa), rc)); }
            c.glint((int) CX, cy, 6, 0xFFFFFF, fa);
        }
        if (!epic) return;
        for (int i = 0; i < 8; i++) {                                                  // the crown: long tapering light blades, slowly turning
            double a = -Math.PI / 2 + (i - 3.5) * 0.36 + Math.sin(us * 0.8) * 0.05; float L = 30 + 18 * (float) Math.cos((i - 3.5) * 0.3) + 4 * (float) Math.sin(us * 3 + i);
            for (int s2 = 8; s2 < L; s2++) {
                float f = s2 / L; int w = Math.max(0, Math.round(3f * (1 - f)));
                int x = (int) (CX + Math.cos(a) * s2 * 1.0), y = (int) (cy - 4 + Math.sin(a) * s2);
                c.rect(x - w, y, x + w + 1, y + 1, Canvas.rgb((int) (150 * k * (1 - f)), i % 2 == 0 ? gold : 0xFFFFFF));
            }
        }
        for (int i = 0; i < (p.style == 1 ? 7 : 26); i++) {                                                 // a golden shower from above
            float ph = frac(us * 0.7 + h(i, 93) * 3), x = 4 + h(i, 94) * (W - 8), y = ph * (H + 6) - 4;
            c.glint((int) (x + Math.sin(ph * 9 + i) * 2), (int) y, i % 5 == 0 ? 2 : 1, i % 3 == 0 ? 0xFFFFFF : gold, k * 0.9f);
        }
        float pulse = 0.5f + 0.5f * (float) Math.sin(us * 5);
        c.glow((float) CX, cy, 40, gold, 0.18f * k * (0.6f + 0.4f * pulse));
    }

    // ------------------------------------------------------------------------------------------------ the Spirit style

    /** A dark void with a soft glow in the boss's colour, and motes of light drifting up. */
    private static void spiritBackdrop(Canvas out, Params p, float t, float ts) {
        float in = cl(t / 0.5f);
        int tone = Canvas.mix(p.base, 0xFFFFFFFF, 0.25f) & 0xFFFFFF;
        out.glow((float) CX, (float) CY - 22, 58, tone, 0.34f * in);
        out.glow((float) CX, (float) CY - 30, 30, 0xFFFFFF, 0.08f * in);
        for (int i = 0; i < 6; i++) {
            float ph = frac(t * 0.16 + i * 0.137), x = (float) CX + (h(i, 61) - 0.5f) * 100 + (float) Math.sin(ph * 7 + i) * 4, y = (float) CY + 6 - ph * 86;
            out.glint((int) x, (int) y, i % 4 == 0 ? 2 : 1, i % 3 == 0 ? 0xFFFFFF : tone, in * 0.8f * (float) Math.sin(ph * Math.PI));
        }
    }

    /** The summoning circle on the floor: rings of light, turning ticks and a pulse on every slash. */
    private static void summonCircle(Canvas out, Params p, float t, float ts) {
        float in = easeOut(t / 0.5f);
        if (in <= 0f) return;
        float pulse = 0f;
        for (int k = 0; k < p.chops; k++) { float a = t - impact(p, k); if (a >= 0 && a < 0.4f) pulse = Math.max(pulse, (float) Math.exp(-a * 9)); }
        float fade = t > ts ? cl(1f - (t - ts - 0.9f) / 1.2f) : 1f;
        int col = Canvas.mix(p.base, 0xFFFFFFFF, 0.55f) & 0xFFFFFF, rc = p.rarity & 0xFFFFFF;
        float cx = (float) CX, cy = (float) CY - 1;
        out.glow(cx, cy, 46, col, (0.20f + 0.35f * pulse) * in * fade);
        int rings = p.tier == 2 ? 3 : 2;
        for (int r = 0; r < rings; r++) {
            float rad = 20 + r * 11; int n = (int) (rad * 5); double rot = t * (r % 2 == 0 ? 0.5 : -0.35);
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n + rot;
                boolean dash = (i / 3) % 2 == 0;
                if (!dash && r > 0) continue;
                int x = (int) Math.round(cx + Math.cos(a) * rad), y = (int) Math.round(cy + Math.sin(a) * rad * ST);
                out.set(x, y, Canvas.rgb((int) ((100 + 120 * pulse) * in * fade), r == rings - 1 && p.tier >= 1 ? rc : col));
            }
            int ticks = 4 + r;                                                      // glowing runes turning round the ring
            for (int i = 0; i < ticks; i++) {
                double a = i * Math.PI * 2 / ticks - rot * 1.6;
                int x = (int) Math.round(cx + Math.cos(a) * rad), y = (int) Math.round(cy + Math.sin(a) * rad * ST);
                out.glint(x, y, Math.sin(a) > 0 ? 2 : 1, 0xFFFFFF, in * fade * (0.45f + 0.55f * pulse + 0.2f));
            }
        }
    }

    /** One crescent of light: a curved stroke that sweeps in over `prog` and thins away with `fade`. */
    private static void arc(Canvas c, float cx, float cy, float dx, float dy, float len, float bend, float prog, float fade, int glowRgb, int coreRgb, float width) {
        float nx = -dy, ny = dx;
        int steps = 70;
        for (int i = 0; i <= steps; i++) {
            float u = -1f + 2f * i / steps;
            if (u > -1f + 2f * prog) break;
            float shape = 1f - u * u, w = width * (float) Math.pow(Math.max(0f, shape), 0.7f) * fade;
            float x = cx + dx * u * len + nx * bend * shape * len * 0.35f, y = cy + dy * u * len + ny * bend * shape * len * 0.35f;
            if (w < 0.4f) continue;
            c.glow(x, y, w + 3.2f, glowRgb, 0.35f * fade);
            c.disc(Math.round(x), Math.round(y), Math.max(0, Math.round(w * 0.5f)), Canvas.rgb((int) (255 * Math.min(1f, fade * 1.2f)), coreRgb));
        }
    }

    /** The slayer's blows: big crescents of light across the spirit (no sword), each leaving a glowing crack that flares when it shatters. */
    private static void soulSlashes(Canvas out, Params p, float t, float ts, int ct) {
        int rc = p.rarity & 0xFFFFFF, tone = Canvas.mix(p.base, 0xFFFFFFFF, 0.5f) & 0xFFFFFF;
        for (int k = 0; k < p.chops; k++) {
            float hit = impact(p, k), a = t - hit;
            float[] c = cut(p, k);
            float bend = (k % 2 == 0 ? 1f : -1f) * (0.9f + 0.5f * h(k, p.cutSeed + 4));
            int glowC = p.tier == 0 ? tone : (k % 2 == 0 ? rc : tone), core = p.tier == 2 && k >= 2 ? 0xFFF2C0 : 0xFFFFFF;
            float len = (p.tier == 2 ? 46 : p.tier == 1 ? 42 : 36) * (k >= 2 ? 1f + 0.05f * Math.min(8, k - 1) : 1f);
            float draw = cl((t - (hit - 0.1f)) / 0.1f);                                   // sweeps across in a tenth of a second
            if (draw > 0f && a < 0.4f) arc(out, c[0], c[1], c[2], c[3], len, bend, draw, a < 0f ? 1f : 1f - cl(a / 0.4f), glowC, core, p.tier == 2 ? 6.5f : 5f);
            if (a >= 0f && t < ts + 0.25f) {                                              // the crack it leaves
                float crack = 0.55f * cl(1f - a / 2.5f) + (t >= ts ? 1f - cl((t - ts) / 0.25f) : 0f);
                if (crack > 0.03f) arc(out, c[0], c[1], c[2], c[3], len * 0.9f, bend, 1f, 0.12f + 0.08f * Math.min(1f, crack), glowC, 0xFFFFFF, 2f);
            }
            if (a >= 0f && a < 0.18f) out.glint(Math.round(c[0] + c[2] * len * 0.25f * (k % 2 == 0 ? 1 : -1)), Math.round(c[1]), 3 + (int) (a * 30), 0xFFFFFF, 1f - a / 0.18f);
        }
        if (t >= ts) {                                                                    // the spirit bursts: souls spiral out, rings roll across the floor
            float a = t - ts;
            int n = p.tier == 2 ? 16 : p.tier == 1 ? 12 : 8;
            for (int i = 0; i < n; i++) {
                float life = a - h(i, 33) * 0.2f; if (life < 0f || life > 1.5f) continue;
                double ang = h(i, 34) * Math.PI * 2 + life * (1.5 + h(i, 35) * 2), r = 4 + (14 + 52 * h(i, 36)) * easeOut(life / 1.1f);
                int x = (int) (CX + Math.cos(ang) * r), y = (int) (ct + Math.sin(ang) * r * 0.7 - 12 * life);
                out.glint(x, y, i % 4 == 0 ? 3 : 2, i % 3 == 0 ? 0xFFFFFF : i % 2 == 0 ? tone : rc, 1f - life / 1.5f);
            }
            for (int r = 0; r < 2; r++) {
                float ra = a - r * 0.1f; if (ra < 0f || ra > 0.7f) continue;
                float rad = 6 + 58 * easeOut(ra / 0.7f);
                for (int i = 0; i < 160; i++) { double an = i * Math.PI * 2 / 160; out.set((int) (CX + Math.cos(an) * rad), (int) (ct + 8 + Math.sin(an) * rad * 0.4), Canvas.rgb((int) (210 * (1f - ra / 0.7f)), r == 0 ? 0xFFFFFF : rc)); }
            }
            float fl = cl(1f - a / 0.2f);
            if (fl > 0f) out.glow((float) CX, ct, 42, 0xFFFFFF, 0.8f * fl);
        }
    }

    // ------------------------------------------------------------------------------------------------ the Mythological scene

    /** A jagged golden lightning bolt from (x0, y0) down to (x1, y1): white core, pale glow, gold halo. */
    private static void bolt(Canvas c, float x0, float y0, float x1, float y1, int seed, float alpha) {
        int segs = 9; float px = x0, py = y0;
        for (int i = 1; i <= segs; i++) {
            float f = i / (float) segs, jx = i == segs ? 0 : (Tex.h(seed, i) - 0.5f) * 20f * (1f - f * 0.5f);
            float nx = x0 + (x1 - x0) * f + jx, ny = y0 + (y1 - y0) * f;
            for (int d = -2; d <= 2; d++) {
                int a = d == 0 ? 255 : Math.abs(d) == 1 ? 170 : 70;
                int col = d == 0 ? 0xFFFFFF : Math.abs(d) == 1 ? 0xFFF3B8 : 0xFFC857;
                lin(c, px + d, py, nx + d, ny, Canvas.rgb((int) (a * alpha), col));
            }
            if (i % 3 == 0) lin(c, nx, ny, nx + (Tex.h(seed, i + 20) - 0.5f) * 22f, ny + 9f, Canvas.rgb((int) (150 * alpha), 0xFFE08A));     // a fork
            px = nx; py = ny;
        }
    }

    /** A fluted marble pillar standing on the floor at x, h pixels tall. */
    private static void pillar(Canvas c, int x, int gy, int h, float glow) {
        if (h < 3) return;
        int top = gy - h;
        c.rect(x - 5, gy - 2, x + 6, gy + 1, 0xFFB9BECE);                                   // base
        for (int yy = top; yy < gy - 2; yy++) for (int xx = -3; xx <= 3; xx++)
            c.set(x + xx, yy, xx == -3 ? 0xFFF1F3FA : xx == -1 || xx == 1 ? 0xFFD7DBE8 : xx == 3 ? 0xFF8F95AA : 0xFFC4C9DA);   // lit from the left, with flutes
        c.rect(x - 5, top - 2, x + 6, top + 1, 0xFFE6E9F3); c.rect(x - 4, top + 1, x + 5, top + 2, 0xFF9AA0B5);   // capital
        if (glow > 0) { c.glint(x, top - 3, 3, 0xFFD36B, glow); c.rect(x - 4, top - 2, x + 5, top - 1, Canvas.rgb((int) (200 * glow), 0xFFE08A)); }
    }

    /** A wreath of laurel leaves circling the drop, split into the half behind it and the half in front. */
    private static void laurel(Canvas out, float us, int cy, boolean front) { laurel(out, us, cy, front, CX, 1f); }
    private static void laurel(Canvas out, float us, int cy, boolean front, double lcx, float k) {
        float in = cl(us / 0.3f);
        if (in <= 0) return;
        for (int r = 0; r < 2; r++) for (int i = 0; i < 18; i++) {
            double an = us * (r == 0 ? 1.7 : -1.3) + i * Math.PI * 2 / 18 + r;
            if ((Math.sin(an) > 0) != front) continue;
            int x = (int) (lcx + Math.cos(an) * (26 + r * 5) * k), y = (int) (cy + 3 + Math.sin(an) * (8 + r * 3) * k);
            int col = (i + r) % 3 == 0 ? 0xFFE9C25A : (i % 2 == 0 ? 0xFF7AD05A : 0xFF4FA84A);
            out.rect(x, y, x + 3, y + 2, Canvas.rgb((int) (235 * in), col)); out.set(x + 1, y - 1, Canvas.rgb((int) (200 * in), col));
        }
        for (int i = 0; i < 5; i++) {                                                    // golden motes
            double an = us * 2.1 + i * Math.PI * 2 / 5;
            if ((Math.sin(an) > 0) != front) continue;
            out.glint((int) (lcx + Math.cos(an) * 30 * k), (int) (cy + Math.sin(an) * 10), 2, 0xFFE08A, in);
        }
    }

    private static void sr(Canvas c, int cx, int gy, int x, int y, int w, int h, int col) { c.rect(cx + x, gy + y, cx + x + w, gy + y + h, col); }

    /** The creature the ritual calls up, as a dark shape inside the circle with burning eyes. Feet at gy, about 40 pixels tall. */
    private static void silhouette(Canvas c, int mob, int cx, int gy, float a, float t) {
        if (a <= 0.01f) return;
        // each creature gets its own colours: Hunter olive, Lynxes amber, Gaia stone grey, Minotaur crimson, Champion bronze, Inquisitor ember, Manticore rust, King Minos royal gold, Nymph sea green, Bull ox-blood, Harpy sky blue, Sphinx sand
        final int[] rimCols = {0xFF7A8A3A, 0xFFD89B3C, 0xFF8E98A8, 0xFFB8453A, 0xFFB98A44, 0xFFE0682D, 0xFFB5552A, 0xFFE8C04A, 0xFF3FB8A0, 0xFF8A3030, 0xFF7FB8E8, 0xFFD9BE7A};
        final int[] bodyCols = {0xFF141A0E, 0xFF1C140C, 0xFF14161C, 0xFF1E0E0E, 0xFF1A140C, 0xFF1E100A, 0xFF1C100A, 0xFF1A1608, 0xFF0A1C1A, 0xFF1A0A0A, 0xFF0E161E, 0xFF1C1810};
        int mi = Math.floorMod(mob, 12);
        int body = Canvas.rgb((int) (235 * a), bodyCols[mi]), edge = Canvas.rgb((int) (190 * a), rimCols[mi]);
        float br = (float) Math.sin(t * 2.2) * 0.8f; int bob = Math.round(br);
        int eyeCol = Canvas.rgb((int) (255 * a), mob == 5 ? 0xFFFF8A3D : mob == 3 || mob == 7 || mob == 9 ? 0xFFFF4040 : mob == 6 ? 0xFFFFD166 : 0xFFFFE08A);
        c.glow(cx, gy - 18, 26, rimCols[mi] & 0xFFFFFF, 0.26f * a);
        for (int pass = 0; pass < 2; pass++) {
            int col = pass == 0 ? edge : body, o = pass == 0 ? 1 : 0;        // a gold rim first (one pixel bigger on the left), then the dark body over it
            int dx = pass == 0 ? -1 : 0, y0 = bob;
            switch (mob) {
                case 0 -> {                                                          // the Hunter: hooded, a bow in hand
                    sr(c, cx, gy, -4 + dx, -22 + y0, 8 + o, 16, col); sr(c, cx, gy, -4 + dx, -6, 3 + o, 6, col); sr(c, cx, gy, 1 + dx, -6, 3 + o, 6, col);
                    sr(c, cx, gy, -3 + dx, -29 + y0, 6 + o, 7, col); sr(c, cx, gy, -4 + dx, -31 + y0, 8 + o, 3, col); sr(c, cx, gy, -2 + dx, -33 + y0, 4 + o, 2, col);
                    for (int j = -15; j <= 15; j++) { int bx = 11 + Math.round((float) (j * j) / 90f); sr(c, cx, gy, bx + dx, -20 + j + y0, 2, 1, col); }
                    sr(c, cx, gy, 6 + dx, -20 + y0, 6, 1, col);
                }
                case 1 -> {                                                          // two cats sitting side by side
                    for (int k = 0; k < 2; k++) {
                        int ox = k == 0 ? -11 : 11, yy = k == 0 ? y0 : -y0;
                        for (int j = -8; j <= 8; j++) { int w = (int) Math.round(6 * Math.sqrt(Math.max(0, 1 - (j / 8.5) * (j / 8.5)))); sr(c, cx, gy, ox - w + dx, -10 + j + yy, w * 2 + o, 1, col); }
                        for (int j = -4; j <= 4; j++) { int w = (int) Math.round(4.6 * Math.sqrt(Math.max(0, 1 - (j / 4.6) * (j / 4.6)))); sr(c, cx, gy, ox - w + dx, -23 + j + yy, w * 2 + o, 1, col); }
                        sr(c, cx, gy, ox - 5 + dx, -31 + yy, 3 + o, 4, col); sr(c, cx, gy, ox + 2 + dx, -31 + yy, 3 + o, 4, col);
                        sr(c, cx, gy, ox + (k == 0 ? 5 : -9) + dx, -12 + yy + (int) (Math.sin(t * 3 + k) * 2), 4 + o, 2, col);
                    }
                }
                case 2 -> {                                                          // Gaia: a huge block of stone
                    sr(c, cx, gy, -9 + dx, -27 + y0, 18 + o, 19, col); sr(c, cx, gy, -5 + dx, -35 + y0, 10 + o, 9, col);
                    sr(c, cx, gy, -15 + dx, -26 + y0, 6 + o, 20, col); sr(c, cx, gy, 9 + dx, -26 + y0, 6 + o, 20, col);
                    sr(c, cx, gy, -7 + dx, -8, 6 + o, 8, col); sr(c, cx, gy, 1 + dx, -8, 6 + o, 8, col);
                }
                case 3 -> {                                                          // the Minotaur: horned head, broad shoulders, an axe
                    sr(c, cx, gy, -8 + dx, -25 + y0, 16 + o, 17, col); sr(c, cx, gy, -4 + dx, -34 + y0, 8 + o, 9, col);
                    sr(c, cx, gy, -9 + dx, -36 + y0, 4 + o, 2, col); sr(c, cx, gy, -11 + dx, -40 + y0, 2 + o, 4, col); sr(c, cx, gy, 5 + dx, -36 + y0, 4 + o, 2, col); sr(c, cx, gy, 9 + dx, -40 + y0, 2 + o, 4, col);
                    sr(c, cx, gy, -6 + dx, -8, 5 + o, 8, col); sr(c, cx, gy, 1 + dx, -8, 5 + o, 8, col); sr(c, cx, gy, -13 + dx, -24 + y0, 5 + o, 13, col);
                    sr(c, cx, gy, 12 + dx, -36 + y0, 2 + o, 28, col); sr(c, cx, gy, 13 + dx, -37 + y0, 7 + o, 8, col);
                }
                case 4 -> {                                                          // the Champion: crested helmet, shield and spear
                    sr(c, cx, gy, -5 + dx, -23 + y0, 10 + o, 15, col); sr(c, cx, gy, -4 + dx, -8, 3 + o, 8, col); sr(c, cx, gy, 1 + dx, -8, 3 + o, 8, col);
                    sr(c, cx, gy, -3 + dx, -31 + y0, 6 + o, 8, col); sr(c, cx, gy, -1 + dx, -38 + y0, 2 + o, 7, col); sr(c, cx, gy, -3 + dx, -39 + y0, 5 + o, 2, col);
                    for (int j = -7; j <= 7; j++) { int w = (int) Math.round(6 * Math.sqrt(Math.max(0, 1 - (j / 7.5) * (j / 7.5)))); sr(c, cx, gy, -12 - w + dx, -17 + j + y0, w * 2 + o, 1, col); }
                    sr(c, cx, gy, 10 + dx, -44 + y0, 1 + o, 40, col); sr(c, cx, gy, 9 + dx, -47 + y0, 3 + o, 4, col);
                }
                case 6 -> {                                                          // the Manticore: a lion with bat wings and a curling scorpion tail
                    sr(c, cx, gy, -12 + dx, -20 + y0, 20 + o, 10, col); sr(c, cx, gy, -12 + dx, -10, 3 + o, 10, col); sr(c, cx, gy, 3 + dx, -10, 3 + o, 10, col);
                    sr(c, cx, gy, 4 + dx, -29 + y0, 8 + o, 10, col); sr(c, cx, gy, 3 + dx, -31 + y0, 3 + o, 3, col); sr(c, cx, gy, 10 + dx, -31 + y0, 3 + o, 3, col);
                    for (int j = 0; j < 14; j++) { int wy = -22 - j + y0 + Math.round(br), ww = 14 - j; sr(c, cx, gy, -8 - ww + dx, wy, ww + o, 1, col); }
                    for (int j = 0; j < 16; j++) { int tx = -13 - Math.round((float) Math.sin(j * 0.2) * 3f) - (j > 10 ? (j - 10) : 0) / 2; sr(c, cx, gy, tx - Math.round(j * 0.5f) + dx, -18 + y0 - j, 2 + o, 1, col); }
                    sr(c, cx, gy, -22 + dx, -36 + y0, 3 + o, 3, col); sr(c, cx, gy, -24 + dx, -34 + y0, 2 + o, 2, col);
                }
                case 7 -> {                                                          // King Minos: a huge crowned bull-king on a throne of shadow
                    sr(c, cx, gy, -11 + dx, -28 + y0, 22 + o, 20, col); sr(c, cx, gy, -6 + dx, -39 + y0, 12 + o, 12, col);
                    sr(c, cx, gy, -12 + dx, -42 + y0, 4 + o, 3, col); sr(c, cx, gy, -14 + dx, -47 + y0, 3 + o, 5, col); sr(c, cx, gy, 8 + dx, -42 + y0, 4 + o, 3, col); sr(c, cx, gy, 11 + dx, -47 + y0, 3 + o, 5, col);
                    sr(c, cx, gy, -7 + dx, -45 + y0, 14 + o, 3, col); for (int k = 0; k < 4; k++) sr(c, cx, gy, -7 + k * 4 + dx, -49 + y0, 2 + o, 4, pass == 0 ? col : Canvas.rgb((int) (235 * a), 0xFFFFC857));
                    sr(c, cx, gy, -8 + dx, -8, 6 + o, 8, col); sr(c, cx, gy, 2 + dx, -8, 6 + o, 8, col); sr(c, cx, gy, -17 + dx, -28 + y0, 6 + o, 18, col); sr(c, cx, gy, 11 + dx, -28 + y0, 6 + o, 18, col);
                    sr(c, cx, gy, 17 + dx, -48 + y0, 2 + o, 36, col);
                }
                case 8 -> {                                                          // the Stranded Nymph: a slender figure with long flowing hair
                    for (int j = 0; j < 24; j++) { int w = 2 + Math.round(j * 0.22f); sr(c, cx, gy, -w + dx, -30 + j + y0, w * 2 + o, 1, col); }
                    sr(c, cx, gy, -3 + dx, -38 + y0, 6 + o, 8, col); sr(c, cx, gy, -5 + dx, -38 + y0, 2 + o, 18 + Math.round(br), col); sr(c, cx, gy, 3 + dx, -38 + y0, 2 + o, 20 - Math.round(br), col);
                    sr(c, cx, gy, 7 + dx, -24 + y0, 1 + o, 8, col);
                }
                case 9 -> {                                                          // the Cretan Bull: a charging bull, head low
                    sr(c, cx, gy, -15 + dx, -23 + y0, 24 + o, 13, col); sr(c, cx, gy, -15 + dx, -10, 3 + o, 10, col); sr(c, cx, gy, -9 + dx, -10, 3 + o, 10, col); sr(c, cx, gy, 3 + dx, -10, 3 + o, 10, col); sr(c, cx, gy, 7 + dx, -10, 3 + o, 10, col);
                    sr(c, cx, gy, 8 + dx, -27 + y0, 10 + o, 10, col); sr(c, cx, gy, 8 + dx, -33 + y0, 2 + o, 6, col); sr(c, cx, gy, 16 + dx, -33 + y0, 2 + o, 6, col); sr(c, cx, gy, 18 + dx, -19 + y0, 3 + o, 3, col);
                    sr(c, cx, gy, -18 + dx, -22 + y0, 3 + o, 2, col); sr(c, cx, gy, -20 + dx, -20 + y0, 2 + o, 6, col);
                }
                case 10 -> {                                                         // the Harpy: a bird-woman with spread wings
                    sr(c, cx, gy, -3 + dx, -26 + y0, 6 + o, 14, col); sr(c, cx, gy, -3 + dx, -34 + y0, 6 + o, 8, col); sr(c, cx, gy, -4 + dx, -12, 2 + o, 12, col); sr(c, cx, gy, 2 + dx, -12, 2 + o, 12, col);
                    int fl = Math.round((float) Math.sin(t * 5f) * 2f);
                    for (int j = 0; j < 16; j++) { int w = 16 - j; sr(c, cx, gy, -4 - w + dx, -32 + j + y0 + (j < 8 ? -fl : 0), w + o, 1, col); sr(c, cx, gy, 4 + dx, -32 + j + y0 + (j < 8 ? -fl : 0), w + o, 1, col); }
                }
                case 11 -> {                                                         // the Sphinx: a lion couched, wings folded, a headdressed face
                    sr(c, cx, gy, -16 + dx, -16 + y0, 28 + o, 11, col); sr(c, cx, gy, 4 + dx, -8, 14 + o, 8, col); sr(c, cx, gy, -16 + dx, -8, 5 + o, 8, col);
                    sr(c, cx, gy, 6 + dx, -34 + y0, 10 + o, 12, col); sr(c, cx, gy, 4 + dx, -33 + y0, 2 + o, 14, col); sr(c, cx, gy, 16 + dx, -33 + y0, 2 + o, 14, col); sr(c, cx, gy, 5 + dx, -36 + y0, 12 + o, 3, col);
                    for (int j = 0; j < 12; j++) sr(c, cx, gy, -14 + j + dx, -28 + j + y0 - Math.round(br), 10 - j / 2 + o, 1, col);
                    sr(c, cx, gy, -19 + dx, -14 + y0, 3 + o, 2, col);
                }
                default -> {                                                         // the Inquisitor: a tall robed figure with a crown of flame
                    for (int j = 0; j < 34; j++) { int w = 3 + Math.round(j * 0.33f); sr(c, cx, gy, -w + dx, -36 + j + y0, w * 2 + o, 1, col); }
                    sr(c, cx, gy, -3 + dx, -43 + y0, 6 + o, 8, col);
                    for (int k = 0; k < 5; k++) { int fh = 4 + Math.round((float) (Math.sin(t * 9 + k * 1.7) * 0.5 + 0.5) * 4) + (k == 2 ? 3 : 0); sr(c, cx, gy, -6 + k * 3 + dx, -43 - fh + y0, 2 + o, fh, pass == 0 ? col : Canvas.rgb((int) (230 * a), 0xFFFF8A3D)); }
                }
            }
        }
        int ey = mob == 0 ? -27 : mob == 1 ? -23 : mob == 2 ? -31 : mob == 3 ? -30 : mob == 4 ? -28 : mob == 6 ? -26 : mob == 7 ? -35 : mob == 8 ? -34 : mob == 9 ? -23 : mob == 10 ? -31 : mob == 11 ? -29 : -39;
        if (mob == 1) { sr(c, cx, gy, -13, ey + bob, 2, 1, eyeCol); sr(c, cx, gy, -9, ey + bob, 2, 1, eyeCol); sr(c, cx, gy, 9, ey - bob, 2, 1, eyeCol); sr(c, cx, gy, 13, ey - bob, 2, 1, eyeCol); }
        else if (mob == 9 || mob == 11) { int ex = mob == 9 ? 12 : 8; sr(c, cx, gy, ex, ey + bob, 2, 1, eyeCol); sr(c, cx, gy, ex + 4, ey + bob, 2, 1, eyeCol); }
        else if (mob == 6) { sr(c, cx, gy, 6, ey + bob, 2, 1, eyeCol); sr(c, cx, gy, 9, ey + bob, 2, 1, eyeCol); }
        else { sr(c, cx, gy, -2, ey + bob, 2, 1, eyeCol); sr(c, cx, gy, 1, ey + bob, 2, 1, eyeCol); }
    }

    /**
     * The Mythological Ritual scene: storm clouds gather over a stone floor between two marble pillars. Three golden lightning bolts strike a
     * rune circle one after another, each lighting more of its glyphs and cracking the floor; then the circle erupts in a column of light
     * and the drop rises out of it wrapped in a wreath of laurel.
     */
    private static void drawMytho(Canvas out, Params p, float t) {
        final boolean rng = p.tier == 2;                                                   // the RNG Drop version: five strikes, a bigger eruption, god rays, a spinning halo and falling feathers
        final int K = Math.max(1, p.chops), GOLD = 0xFFC857, PALE = 0xFFF3B8;
        final float H = (K - 1) / 2f;
        float ts = splitTime(p), us = Math.max(0f, t - ts - 0.12f);
        boolean split = t >= ts;
        int cx = (int) CX, gy = (int) CY - 4;
        int landed = 0; for (int k = 0; k < K; k++) if (t >= impact(p, k)) landed = k + 1;
        float lit = split ? 1f : landed / (float) K;

        float shx = 0, shy = 0;
        for (int k = 0; k < K; k++) { float a = t - impact(p, k); if (a >= 0 && a < 0.25f) { float f = 1 - a / 0.25f; shx += (float) Math.sin(a * 140) * 2.2f * f; shy += (float) Math.cos(a * 120) * 1.6f * f; } }
        if (split && t - ts < 0.4f) { float f = 1 - (t - ts) / 0.4f; shx += (float) Math.sin((t - ts) * 110) * 4f * f; shy += (float) Math.cos((t - ts) * 95) * 4f * f; }
        p.shakeX = shx; p.shakeY = shy;

        int icy = itemScreenY(t, ts);
        if (split && p.hasItem && p.item != null) twinPass(out, p, t, ts, us, true, false, 0, false);

        // storm clouds roll in over the top, then part
        float cloud = cl(t / 0.35f) * (split ? cl(1f - (t - ts) / 0.6f) : 1f);
        if (p.alt && cloud > 0.02f) for (int i = 0; i < 40; i++) {
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 3 + i * 1.9);
            int x = (int) (h(i, 200) * 112), y = (int) (h(i, 201) * 40);
            out.set(x, y, Canvas.rgb((int) (200 * cloud * tw), i % 4 == 0 ? 0xFFFFE9A0 : 0xFFFFFFFF));
            if (i % 9 == 0) star(out, x, y, 1, Canvas.rgb((int) (160 * cloud * tw), 0xFFFFE9A0));
        }
        if (!p.alt && cloud > 0.02f) for (int i = 0; i < 8; i++) {
            int x = 6 + i * 14 + (int) (Math.sin(t * 0.9 + i) * 3), y = 6 + (i % 2) * 5 + (int) (Math.cos(t * 0.7 + i * 2) * 1.5);
            ell(out, x, y, 14, 6, Canvas.rgb((int) (200 * cloud), i % 2 == 0 ? 0x2A2F4E : 0x3A4166));
            ell(out, x - 2, y - 2, 9, 3, Canvas.rgb((int) (120 * cloud), 0x59618C));
        }

        // the stone floor
        float fl0 = cl(t / 0.4f);
        ell(out, cx, gy + 4, 46, 13, Canvas.rgb((int) (140 * fl0), 0xFF07060C));
        ell(out, cx, gy + 2, 42, 11, Canvas.rgb((int) (255 * fl0), 0xFF262938));
        ell(out, cx, gy + 1, 38, 9, Canvas.rgb((int) (255 * fl0), 0xFF303449));

        // pillars rise from the floor and catch each strike
        float rise = easeOut((t - 0.1f) / 0.5f);
        float flash = 0; for (int k = 0; k < K; k++) { float a = t - impact(p, k); if (a >= 0 && a < 0.5f) flash = Math.max(flash, 1f - a / 0.5f); }
        if (split) flash = Math.max(flash, 1f - cl((t - ts) / 1.2f));
        pillar(out, cx - 40, gy + 1, Math.round(44 * rise), flash);
        pillar(out, cx + 40, gy + 1, Math.round(44 * rise), flash);

        // the rune circle: dim until struck, then glowing gold
        for (int ring = 0; ring < 2; ring++) {
            int rx = ring == 0 ? 33 : 21, ry = ring == 0 ? 8 : 5;
            for (int i = 0; i < 140; i++) {
                double an = i * Math.PI * 2 / 140; float fr = i / 140f;
                boolean on = fr <= lit + (lit >= 1f ? 1 : 0.0f) || split;
                float pulse = 0.75f + 0.25f * (float) Math.sin(t * 8 - fr * 12);
                int a = on ? (int) ((split ? 255 : 220) * pulse) : 60;
                out.set((int) (cx + Math.cos(an) * rx), (int) (gy + 1 + Math.sin(an) * ry), Canvas.rgb(a, on ? (ring == 0 ? GOLD : PALE) : 0x8A7A4A));
            }
        }
        for (int i = 0; i < 12; i++) {                                                   // glyphs round the rim
            double an = i * Math.PI * 2 / 12 + 0.26; boolean on = split || i < 12 * lit;
            int x = (int) (cx + Math.cos(an) * 27), y = (int) (gy + 1 + Math.sin(an) * 6.5);
            int a = on ? (int) (255 * (0.7f + 0.3f * Math.sin(t * 9 + i))) : 70;
            out.rect(x, y - 1, x + 2, y + 2, Canvas.rgb(a, on ? PALE : 0x8A7A4A)); out.set(x + 1, y, Canvas.rgb(a, 0xFFFFFF));
        }
        float cg = lit * (split ? (p.hasItem ? 1f : 0.15f) : 0.8f);
        if (cg > 0) { out.glow(cx, gy + 1, 30, GOLD, 0.35f * cg); }

        // a hexagram turns inside the circle, brighter with every strike
        if (lit > 0f) {
            float rot = t * 0.9f; int hs = split ? 255 : (int) (130 + 110 * lit);
            for (int tri = 0; tri < 2; tri++) for (int k = 0; k < 3; k++) {
                double a1 = rot + tri * Math.PI / 3 + k * Math.PI * 2 / 3, a2 = rot + tri * Math.PI / 3 + (k + 1) * Math.PI * 2 / 3;
                lin(out, (float) (cx + Math.cos(a1) * 21), (float) (gy + 1 + Math.sin(a1) * 5), (float) (cx + Math.cos(a2) * 21), (float) (gy + 1 + Math.sin(a2) * 5), Canvas.rgb(hs, tri == 0 ? PALE : GOLD));
            }
        }
        // glyphs drift up off the circle while it charges
        if (lit > 0f && !split) for (int i = 0; i < 12; i++) {
            float ph = frac(t * 0.55 + i / 12.0), al = (float) Math.sin(ph * Math.PI) * lit;
            int gx = (int) (cx + Math.cos(i * 2.4 + t * 0.6) * 27), gyy = (int) (gy - 2 - ph * 46);
            out.rect(gx, gyy, gx + 2, gyy + 3, Canvas.rgb((int) (230 * al), PALE)); out.set(gx + 1, gyy + 1, Canvas.rgb((int) (255 * al), 0xFFFFFF));
        }
        // the creature the ritual calls: a shadow that grows clearer with every strike, then shatters
        float ghost = cl((t - 0.4f) / 0.5f) * (0.3f + 0.7f * lit) * (split ? cl(1f - (t - ts) / 0.12f) : 1f);
        silhouette(out, p.mob, cx, gy, ghost, t);

        // cracks left in the floor by every strike
        for (int k = 0; k < K; k++) {
            float a = t - impact(p, k); if (a < 0) continue;
            float g = easeOut(a / 0.3f);
            for (int i = 0; i < 5; i++) {
                double an = (i + Tex.h(k, i) * 0.8) * Math.PI * 2 / 5; float len = (8 + 16 * Tex.h(i, k + 3)) * g;
                float tx = cx + (k - H) * 12, ex = tx + (float) Math.cos(an) * len, ey = gy + 1 + (float) Math.sin(an) * len * 0.28f;
                lin(out, tx, gy + 1, ex, ey, Canvas.rgb((int) (240 * (split ? 0.5f : 1f)), 0xFF1A1620));
                lin(out, tx, gy, ex, ey - 1, Canvas.rgb((int) (190 * (1f - 0.5f * cl(a / 1.5f))), GOLD));
            }
        }

        // the three bolts
        for (int k = 0; k < K; k++) {
            float land = impact(p, k), a = t - land;
            float tx = cx + (k - H) * 12;
            float pre = land - t;
            if (p.alt) comet(out, t, land, tx, gy, k);
            if (!p.alt && pre > 0 && pre < 0.14f) bolt(out, tx + (k - H) * 4, 0, tx, gy, k * 11 + 3, 0.28f);                // a faint leader flickers first
            if (!p.alt && a >= 0 && a < 0.22f) bolt(out, tx + (k - H) * 4, 0, tx, gy, k * 11 + 5 + (int) (a * 40), 1f - a / 0.3f);
            if (rng && k == K - 1 && a >= 0 && a < 0.3f) {                                                       // the last strike is a thick double bolt that whitens the whole stage
                if (!p.alt) { bolt(out, tx - 5, 0, tx - 2, gy, 91 + (int) (a * 40), 1f - a / 0.3f); bolt(out, tx + 6, 0, tx + 2, gy, 77 + (int) (a * 40), 1f - a / 0.3f); }
                out.glow(cx, gy - 20, 110, 0xFFFFFF, 0.5f * (1 - a / 0.3f));
            }
            if (a >= 0 && a < 0.55f) {
                float r = 4 + 34 * easeOut(a / 0.55f);
                for (int i = 0; i < 90; i++) { double an = i * Math.PI * 2 / 90; out.set((int) (tx + Math.cos(an) * r), (int) (gy + 1 + Math.sin(an) * r * 0.25), Canvas.rgb((int) (230 * (1 - a / 0.55f)), PALE)); }
                for (int i = 0; i < 14; i++) {
                    double an = -Math.PI * (0.1 + 0.8 * Tex.h(i, k)); float sp = 30 + Tex.h(i, k + 6) * 40;
                    int x = (int) (tx + Math.cos(an) * sp * a), y = (int) (gy + Math.sin(an) * sp * a + 0.5f * 170 * a * a);
                    if (y > gy + 2) continue;
                    out.rect(x, y, x + (i % 3 == 0 ? 2 : 1), y + (i % 3 == 0 ? 2 : 1), Canvas.rgb((int) (255 * (1 - a / 0.55f)), i % 2 == 0 ? PALE : GOLD));
                }
            }
            if (a >= 0 && a < 0.12f) out.glow(tx, gy - 8, 58, 0xFFFFFF, 0.55f * (1 - a / 0.12f));                          // the strike lights up the whole stage
        }

        // lightning leaps across between the pillar tops with every strike
        for (int k = 0; k < K; k++) {
            float a = t - impact(p, k); if (a < 0 || a > 0.2f) continue;
            float px0 = cx - 40, py0 = gy - 46;
            for (int i = 1; i <= 10; i++) {
                float nx = cx - 40 + 80f * i / 10f, ny = gy - 46 + (i == 10 ? 0 : (Tex.h(k * 5 + i, 31) - 0.5f) * 12f);
                lin(out, px0, py0, nx, ny, Canvas.rgb((int) (250 * (1 - a / 0.2f)), 0xFFFFFF)); lin(out, px0, py0 + 1, nx, ny + 1, Canvas.rgb((int) (170 * (1 - a / 0.2f)), 0xFFC857));
                px0 = nx; py0 = ny;
            }
        }

        // the eruption
        if (split && p.hasItem) {
            float a = t - ts;
            if (a < 1.1f) for (int i = 0; i < 64; i++) {                                   // the shadow bursts into golden shards
                double an = h(i, 41) * Math.PI * 2; float sp = 14 + 70 * h(i, 42), x0 = cx + (h(i, 43) - 0.5f) * 16, y0 = gy - 4 - h(i, 44) * 34;
                int x = (int) (x0 + Math.cos(an) * sp * a), y = (int) (y0 + Math.sin(an) * sp * a * 0.8f + 60f * a * a);
                int sz = i % 4 == 0 ? 2 : 1;
                out.rect(x, y, x + sz, y + sz, Canvas.rgb((int) (255 * (1 - a / 1.1f)), i % 3 == 0 ? 0xFF2A1E3A : i % 3 == 1 ? PALE : GOLD));
            }
            if (a < 0.9f) {                                                              // a column of golden light
                float wv = 11f * (1 - a / 0.9f) + 2f; int top = gy - (int) ((gy + 2) * easeOut(a / 0.22f));
                for (int y = Math.max(0, top); y < gy; y++) {
                    float fy = (y - top) / (float) Math.max(1, gy - top), w = wv * (0.5f + 0.5f * fy);
                    for (int x = -(int) w; x <= (int) w; x++) out.set(cx + x, y, Canvas.rgb((int) (235 * (1 - a / 0.9f) * (0.8f + 0.2f * Math.sin(t * 40 + y * 0.4))), Math.abs(x) < w * 0.35f ? 0xFFFFFF : x % 2 == 0 ? PALE : GOLD));
                }
            }
            for (int ring = 0; ring < 3; ring++) {                                       // three shockwaves across the floor
                float ra = a - ring * 0.08f; if (ra < 0 || ra > 0.7f) continue;
                float r = 6 + 60 * easeOut(ra / 0.7f);
                for (int i = 0; i < 180; i++) { double an = i * Math.PI * 2 / 180; out.rect((int) (cx + Math.cos(an) * r), (int) (gy + 1 + Math.sin(an) * r * 0.27), (int) (cx + Math.cos(an) * r) + 2, (int) (gy + 1 + Math.sin(an) * r * 0.27) + 1, Canvas.rgb((int) (230 * (1 - ra / 0.7f)), ring == 1 ? 0xFFFFFF : ring == 0 ? PALE : 0x9BE87A)); }
            }
            if (a < 2.2f) for (int i = 0; i < 70; i++) {                                 // laurel leaves and gold dust whirling up out of the circle
                float ph = a * 0.5f + h(i, 2), sp = 0.5f + h(i, 4);
                double an = ph * 3.0 * sp + i; float rad = 6 + 28 * easeOut(a / 0.8f) * h(i, 6);
                int x = (int) (cx + Math.cos(an) * rad), y = (int) (gy - 2 - a * (22 + 40 * h(i, 8)) + Math.sin(an) * rad * 0.25f);
                int al = (int) (240 * (1 - a / 2.2f));
                if (i % 3 == 0) { out.rect(x, y, x + 3, y + 2, Canvas.rgb(al, i % 2 == 0 ? 0xFF7AD05A : 0xFF4FA84A)); out.set(x + 1, y - 1, Canvas.rgb(al, 0xFFA8F08A)); }
                else out.rect(x, y, x + (i % 5 == 0 ? 2 : 1), y + (i % 5 == 0 ? 2 : 1), Canvas.rgb(al, i % 2 == 0 ? PALE : GOLD));
            }
            for (int i = 0; i < 20; i++) {                                                // twinkling stars
                float ph = frac(us * 0.9 + i * 0.237), tw = (float) Math.sin(ph * Math.PI);
                int x = (int) (cx + (h(i, 21) - 0.5f) * 92), y = (int) (gy - 10 - ph * 66);
                if (us > 0 && tw > 0.2f) star(out, x, y, tw > 0.7f ? 2 : 1, Canvas.rgb((int) (240 * tw), i % 3 == 0 ? 0xFFFFFFFF : 0xFFF3B8));
            }
            float fl = cl(1f - a / 0.28f);
            if (fl > 0) for (int i = 0; i < 5; i++) out.disc(cx, icy, 12 + i * 12, Canvas.rgb((int) (95 * fl), i % 2 == 0 ? 0xFFFFFFFF : 0xFFFFE9A8));
        }

        if (split && rng && p.hasItem) {                                                     // ---- the RNG Drop finale
            float a = t - ts;
            float gr = cl(a / 0.5f) * cl((length(p) - t) / 0.8f);
            for (int i = 0; i < 12; i++) {                                                   // god rays turning slowly behind the drop
                double an = i * Math.PI / 6 + a * 0.35; float len = 70 * gr, w = 0.09f;
                for (int s = 0; s < 3; s++) { double a2 = an + (s - 1) * w * 0.5; lin(out, cx, icy, (float) (cx + Math.cos(a2) * len), (float) (icy + Math.sin(a2) * len * 0.8f), Canvas.rgb((int) ((s == 1 ? 70 : 30) * gr), i % 2 == 0 ? 0xFFF3B8 : 0xFFFFD27A)); }
            }
            for (int ring = 0; ring < 2; ring++) {                                           // two halos of runes circle the drop, one tilted each way
                int n = 26; float rr = 24 + ring * 6, tilt = ring == 0 ? 0.32f : 0.22f;
                for (int i = 0; i < n; i++) {
                    double an = i * Math.PI * 2 / n + a * (ring == 0 ? 1.6 : -1.2); int x = (int) (cx + Math.cos(an) * rr), y = (int) (icy + Math.sin(an) * rr * tilt);
                    boolean front = Math.sin(an) > 0, rune = i % 3 == 0;
                    out.rect(x, y, x + (rune ? 2 : 1), y + (rune ? 3 : 1), Canvas.rgb((int) ((front ? 255 : 120) * gr), rune ? 0xFFFFFFFF : ring == 0 ? PALE : GOLD));
                }
            }
            for (int ring = 0; ring < 4; ring++) {                                           // pulses of light keep rolling out from the drop
                float ph = frac(a * 0.8 + ring * 0.25), r = 10 + 56 * ph;
                for (int i = 0; i < 120; i++) { double an = i * Math.PI * 2 / 120; out.set((int) (cx + Math.cos(an) * r), (int) (icy + Math.sin(an) * r * 0.55f), Canvas.rgb((int) (150 * (1 - ph) * gr), 0xFFE9A8)); }
            }
            for (int i = 0; i < 34; i++) {                                                   // griffin feathers drift down the screen
                float sp = 0.5f + h(i, 61) * 0.7f, fy = (a * 18 * sp + h(i, 62) * 120) % 120 - 6;
                int fx = (int) (cx + (h(i, 63) - 0.5f) * 108 + Math.sin(a * 2 * sp + i) * 5), fyy = (int) fy;
                int al = (int) (230 * gr);
                out.set(fx, fyy, Canvas.rgb(al, 0xFFFFFFFF)); out.set(fx + 1, fyy + 1, Canvas.rgb(al, 0xFFFFF3B8)); out.set(fx + 2, fyy + 2, Canvas.rgb(al, 0xFFFFC857)); out.set(fx, fyy + 1, Canvas.rgb((int) (al * 0.6f), 0xFFFFFFFF));
            }
            float fl2 = cl(1f - a / 0.5f);
            if (fl2 > 0) out.glow(cx, icy, 120, 0xFFFFFF, 0.6f * fl2);
        }
        if (split && !p.hasItem) {                                                           // ---- nothing came: the circle goes dark and grey smoke curls up
            float a = t - ts, d = cl(a / 0.5f);
            ell(out, cx, gy + 1, 36, 9, Canvas.rgb((int) (215 * d), 0xFF262938));
            ell(out, cx, gy + 1, 22, 5, Canvas.rgb((int) (120 * d), 0xFF14141E));
            for (int i = 0; i < 28; i++) {
                float ph = cl(a * 0.45f - h(i, 5) * 0.7f); if (ph <= 0f || ph >= 1f) continue;
                int x = (int) (cx + (h(i, 6) - 0.5f) * 46 + Math.sin(a * 2.2 + i) * 4), y = (int) (gy - ph * 40);
                out.disc(x, y, 2 + (int) (ph * 5), Canvas.rgb((int) (140 * (1 - ph)), i % 2 == 0 ? 0xFF8A8F9E : 0xFF6A6F7E));
            }
            for (int k = 0; k < K; k++) {                                                    // the cracks the strikes left go cold
                float tx = cx + (k - H) * 12;
                lin(out, tx, gy + 1, tx + (h(k, 9) - 0.5f) * 14, gy + 3, Canvas.rgb((int) (160 * d), 0xFF0E0C14));
            }
        }
        if (split && p.hasItem && p.item != null) {
            laurel(out, us, icy, false);
            drawItem(out, p, t, ts, us);
            laurel(out, us, icy, true);
        }
    }

    // ------------------------------------------------------------------------------------------------ the Experimentation Table scene

    /**
     * The Experimentation Table scene: runes spiral into the table's floating book, it swings open and the view zooms in on it, its pages flip
     * (p.chops times) and then the drop bursts out of the book in a column of light. The RNG version (tier 2) flips more, faster, and ends
     * with god rays and a laurel.
     */
    private static void drawExperiment(Canvas out, Params p, float t) {
        final boolean rng = p.tier == 2;
        final int K = Math.max(1, p.chops), VIO = 0xB56BFF, AQUA = 0x9AF0FF, GOLD = 0xFFC857;
        final float ts = splitTime(p), us = Math.max(0f, t - ts - 0.12f), FD = rng ? 0.24f : 0.30f;
        final boolean split = t >= ts;
        final int cx = (int) CX, PW = 22, PH = 12;
        float zIn = easeIO((t - 0.5f) / 0.9f), zOut = easeIO((t - ts) / 0.7f);
        final float z = 1f + 1.05f * zIn - 0.5f * zOut;
        final float bcy = 38f + 14f * zIn + 24f * zOut;                                    // the book's centre on screen
        final float bob = (float) Math.sin(t * 2.1) * 1.4f * (1f - zIn);
        float open = easeOut((t - 0.25f) / 0.55f);
        int landed = 0; for (int k = 0; k < K; k++) if (t >= impact(p, k) - 0.1f + FD) landed = k + 1;
        float pulse = 0f; for (int k = 0; k < K; k++) { float a = t - impact(p, k); if (a >= 0 && a < 0.3f) pulse = Math.max(pulse, 1f - a / 0.3f); }
        p.shakeX = split && t - ts < 0.3f ? (float) Math.sin((t - ts) * 110) * 3f * (1f - (t - ts) / 0.3f) : 0f; p.shakeY = 0;
        final int bx = cx, by = Math.round(bcy + bob * z);

        // a dark soft backdrop so the scene has depth, then light behind everything, stronger on every flip
        out.glow(bx, by, 78, 0x07040F, 0.62f * cl(t / 0.4f));
        out.glow(bx, by, (50 + 14 * pulse) * z * 0.8f, rng ? 0xE0A8FF : VIO, (0.20f + 0.22f * pulse) * open + (split ? 0.25f * cl(1f - (t - ts) / 1.2f) : 0f));

        { int shy = Math.round(bcy + (66 - 38) * z) - 2; ell(out, cx, shy, Math.round(PW * z * 0.9f), Math.max(2, Math.round(3 * z)), Canvas.rgb((int) (120 * (1f - zIn * 0.7f)), 0xFF04020A)); }   // the book's shadow on the table
        // the table, seen below the floating book (it slides out of frame as we zoom in)
        {
            int ty = Math.round(bcy + (66 - 38) * z), tw = Math.round(22 * z), th = Math.round(5 * z), bh = Math.round(14 * z);
            ell(out, cx, ty + th + bh, Math.round(tw * 1.25f), Math.max(2, Math.round(4 * z)), Canvas.rgb(110, 0xFF07050C));
            out.rect(cx - tw, ty + th, cx + tw, ty + th + bh, 0xFF171221);
            for (int i = 0; i < 14; i++) { int sx = cx - tw + 2 + Math.round(h(i, 5) * (tw * 2 - 4)), sy = ty + th + 1 + Math.round(h(i, 6) * (bh - 2)); out.set(sx, sy, Canvas.rgb(150, 0xFF7A4BC2)); }
            out.rect(cx - tw, ty + th, cx + tw, ty + th + 1, 0xFF3A2F55);
            out.rect(cx - tw - 1, ty, cx + tw + 1, ty + th, 0xFFA11E2E);                     // the red cloth top
            out.rect(cx - tw - 1, ty, cx + tw + 1, ty + 1, 0xFFD6525F);
            out.rect(cx - tw / 3, ty + 1, cx + tw / 3, ty + th - 1, 0xFFC2303F);
        }

        // runes spiral from the table into the book
        float rate = 0.45f + (rng ? 0.35f : 0.18f) * landed;
        for (int i = 0; i < 18 && !split; i++) {
            float u = frac(t * rate + i / 18f + h(i, 3));
            double an = i * 2.4 + t * 1.4; float rr = (1f - u) * 40f * z;
            float gx = bx + (float) Math.cos(an) * rr, gy = by + 4 * z + (float) Math.sin(an) * rr * 0.35f - u * 14f * z + (1f - u) * 18f * z;
            int al = (int) (200 * Math.min(1f, u * 4f) * (1f - u * u));
            int gcol = i % 3 == 0 ? 0xFFFFFFFF : i % 3 == 1 ? 0xFF000000 | AQUA : 0xFF000000 | VIO;
            int bits = (int) (h(i, 9) * 511f);
            for (int b = 0; b < 9; b++) if ((bits >> b & 1) != 0 && (b % 3 + b / 3) != 4) out.set(Math.round(gx) + b % 3, Math.round(gy) + b / 3, Canvas.rgb(al, gcol));
        }

        if (p.alt) expOrb(out, p, t, ts, bx, by, z, landed, K, rng, split, open);
        else {
        // the book: leather cover, then the open pages
        final float hw = (3f + (PW - 3f) * open) * z, hh = PH * z;
        int L = Math.round(bx - hw), R = Math.round(bx + hw), T = Math.round(by - hh), B = Math.round(by + hh);
        int dep = Math.max(2, Math.round(3.2f * z));                                         // the cover's thickness under the pages
        for (int y = T - 2; y < B + 3 + dep; y++) {                                          // leather, lit from above
            float f = (y - (T - 2)) / (float) (B + 3 + dep - (T - 2));
            int lc = Canvas.mix(0xFF6B2E92, 0xFF2A1040, Math.min(1f, f * 1.15f));
            if (y >= B + 3) lc = Canvas.mix(0xFF4A1E66, 0xFF1C0A2C, (y - B - 3) / (float) dep);
            out.rect(L - 2, y, R + 2, y + 1, lc);
        }
        out.rect(L - 2, T - 2, R + 2, T - 1, 0xFFB57AE0);
        out.rect(L - 2, B + 2, R + 2, B + 3, 0xFFFFC857 & 0xFFC89A30 | 0xFF000000);           // a gold trim line under the pages
        out.rect(L - 2, B + 2 + dep, R + 2, B + 3 + dep, 0xFF12061C);
        out.rect(L - 2, T - 2, L - 1, B + 3 + dep, 0xFFD6A93C); out.rect(R + 1, T - 2, R + 2, B + 3 + dep, 0xFF8A6A22);   // gold corner edges
        for (int cxs : new int[]{L - 2, R + 1}) for (int cys : new int[]{T - 2, B + 2}) out.rect(cxs, cys, cxs + 2, cys + 2, 0xFFFFE08A);   // corner studs
        int stackR = Math.round((K - landed) / (float) K * 3f * z), stackL = Math.round(landed / (float) K * 3f * z);
        out.rect(L, B + 1, R, B + 2 + stackL / 2, 0xFFD9CBA0);
        for (int side = -1; side <= 1; side += 2) {                                         // static pages: left (side -1) and right (+1)
            int wpx = Math.round(hw) - 1;
            for (int dx = 1; dx <= wpx; dx++) {
                float u = dx / (float) Math.max(1, wpx);
                int x = side < 0 ? bx - dx : bx + dx - 1;
                int top = Math.round(by - hh + 1 + u * u * 2f * z), bot = Math.round(by + hh - 1 - u * 1.2f * z);
                float thick = side < 0 ? stackL : stackR;
                int col = Canvas.mix(0xFFF6EBCB, 0xFFD6C292, u * 0.5f);
                col = Canvas.shade(col, 0.58f + 0.42f * easeOut(u / 0.3f));                          // the page curves down into the gutter
                if (u > 0.9f) col = Canvas.mix(col, 0xFFFFFFFF, 0.25f);
                col = Canvas.mix(col, 0xFFE6D2FF, 0.18f * open * (0.5f + 0.5f * pulse));            // lit by the glow
                out.rect(x, top, x + 1, bot, col);
                if (thick > 0 && dx > wpx - Math.round(thick)) out.rect(x, top - 1, x + 1, top, 0xFFE8DCB8);
            }
            if (open > 0.6f) {                                                              // runes written on the page
                int lines = 6; float ink = cl((open - 0.6f) / 0.4f);
                for (int l = 0; l < lines; l++) {
                    int ly = Math.round(by - hh + (3 + l * 3.6f) * z); int len = Math.round((8 + h(l + (side > 0 ? 7 : 0), 11) * 10) * z);
                    int x0 = side < 0 ? bx - 3 - len : bx + 3;
                    out.rect(x0, ly, x0 + len, ly + Math.max(1, Math.round(z * 0.7f)), Canvas.rgb((int) (170 * ink), 0xFF7A4BC2));
                    if (l % 3 == 1) out.rect(x0, ly + 1, x0 + len / 2, ly + 2, Canvas.rgb((int) (90 * ink), 0xFF000000 | AQUA));
                }
            }
        }
        out.rect(bx - 1, T, bx + 1, B + 1, 0xFF2A1040);                                      // the gutter
        for (int k = 0; k < K; k++) {                                                        // a page turning over the spine
            float a = t - (impact(p, k) - 0.1f);
            if (a < 0f || a > FD) continue;
            float th = (float) Math.PI * easeIO(a / FD), cs = (float) Math.cos(th), sn = (float) Math.sin(th);
            int wpx = Math.round(hw) - 1, wd = Math.round(Math.abs(cs) * wpx);
            for (int dx = 0; dx <= wd; dx++) {
                float u = dx / (float) Math.max(1, wpx), lift = sn * 5f * z * (0.3f + u);
                int x = cs >= 0 ? bx + dx : bx - dx - 1;
                int top = Math.round(by - hh - lift), bot = Math.round(by + hh - lift * 0.35f);
                int col = Canvas.mix(0xFFFFF6DC, 0xFFB8A070, Math.min(1f, sn * 0.5f + u * 0.25f));
                out.rect(x, top, x + 1, bot, col);
                if (dx % 5 == 2 && sn > 0.15f) out.rect(x, top + 3, x + 1, top + 4, Canvas.rgb(120, 0xFF7A4BC2));
            }
            int ex = cs >= 0 ? bx + wd : bx - wd - 1, et = Math.round(by - hh - sn * 5f * z * 1.3f);
            out.rect(ex, et, ex + 1, Math.round(by + hh - sn * 5f * z * 0.46f), 0xFFFFFFFF);  // the bright turning edge
            for (int i = 0; i < 7; i++) {                                                   // sparks shake off the page
                float f = h(i, 40 + k), age = a / FD;
                out.set(ex + Math.round((f - 0.5f) * 14f), Math.round(et + 4 + (h(i, 41) - 0.5f) * hh * 1.2f - age * 10f), Canvas.rgb((int) (230 * (1f - age)), i % 2 == 0 ? 0xFFFFFFFF : 0xFF000000 | AQUA));
            }
        }
        if (rng && !split) {                                                                 // an RNG book hums gold between flips
            for (int i = 0; i < 10; i++) { double an = t * 1.8 + i * Math.PI / 5; out.set(Math.round(bx + (float) Math.cos(an) * (hw + 6)), Math.round(by + (float) Math.sin(an) * (hh + 5)), Canvas.rgb((int) (110 + 120 * cl(landed / (float) K)), 0xFF000000 | GOLD)); }
        }

        }

        // the reveal: a column of light out of the book, runes bursting, rings
        if (split) {
            float a = t - ts, fade = cl(1f - a / 3.2f) * 0.5f + 0.18f;
            int top = 0, icy = itemScreenY(t, ts);
            for (int y = top; y < by; y++) {
                float f = 1f - (by - y) / (float) by; int w = Math.round((4 + 7 * f) * cl(a / 0.25f));
                int al = Math.round(110 * fade * (0.3f + 0.7f * f)); out.rect(bx - w, y, bx + w, y + 1, Canvas.rgb(al, rng ? 0xFFF3D8FF : 0xFFE6D2FF));
                out.rect(bx - w / 3, y, bx + w / 3, y + 1, Canvas.rgb(Math.min(255, al * 2), 0xFFFFFFFF));
            }
            float fl = cl(1f - a / 0.35f);
            if (fl > 0) out.glow(bx, by, 80, 0xF0E4FF, 0.85f * fl);
            for (int i = 0; i < (rng ? 34 : 22); i++) {                                     // runes burst upward and fade
                float f = h(i, 70), sp = 24 + 40 * h(i, 71); double an = -Math.PI * (0.15 + 0.7 * f);
                float x = bx + (float) Math.cos(an) * sp * Math.min(a, 1.6f) * 0.9f, y = by + (float) Math.sin(an) * sp * Math.min(a, 1.6f) * 0.9f + 18f * a * a * 0.2f;
                int al = (int) (230 * cl(1f - a / 2.2f)); if (al <= 0) continue;
                int bits = (int) (h(i, 72) * 511f);
                for (int b = 0; b < 9; b++) if ((bits >> b & 1) != 0 && (b % 3 + b / 3) != 4) out.set(Math.round(x) + b % 3, Math.round(y) + b / 3, Canvas.rgb(al, i % 3 == 0 ? 0xFFFFFFFF : i % 3 == 1 ? 0xFF000000 | AQUA : 0xFF000000 | (rng ? GOLD : VIO)));
            }
            boolean tw = p.twin && p.item2 != null;
            int[] ctr = tw ? new int[]{bx - (int) TWIN_DX, bx + (int) TWIN_DX} : new int[]{bx};
            float kk = tw ? 0.6f : 1f;
            if (rng) {
                float gr = cl(a / 0.5f) * cl((length(p) - t) / 0.8f);
                for (int c0 : ctr) for (int i = 0; i < 12; i++) {                          // god rays turning slowly behind each drop
                    double an = i * Math.PI / 6 + a * 0.35; float len = 70 * gr * kk;
                    for (int s2 = 0; s2 < 3; s2++) { double a2 = an + (s2 - 1) * 0.045; lin(out, c0, icy, (float) (c0 + Math.cos(a2) * len), (float) (icy + Math.sin(a2) * len * 0.8f), Canvas.rgb((int) ((s2 == 1 ? 70 : 30) * gr), i % 2 == 0 ? 0xFFFFE9A8 : 0xFFE6D2FF)); }
                }
            }
            if (p.hasItem && p.item != null) {
                if (rng) for (int c0 : ctr) laurel(out, us, icy, false, c0, kk);
                drawItem(out, p, t, ts, us);
                if (rng) for (int c0 : ctr) laurel(out, us, icy, true, c0, kk);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ the Farming scene

    /** A small patch of farmland: a crop grows in three bursts, then bursts open and the drop pops out of it in a swirl of leaves and glints. */
    private static void lin(Canvas c, float x0, float y0, float x1, float y1, int col) {
        int n = Math.max(1, (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
        for (int i = 0; i <= n; i++) c.set(Math.round(x0 + (x1 - x0) * i / n), Math.round(y0 + (y1 - y0) * i / n), col);
    }

    /** A pest on the soil, drawn at (x, y) at scale k, facing right (dir 1) or left (-1). dead = belly up with crossed eyes. */
    private static void bug(Canvas c, float x, float y, float k, float t, int dir, boolean lifted) {
        int body = 0xFF6E8B2F, shell = 0xFF8FB13C, dark = 0xFF2E3B12, leg = 0xFF1E2A0C;
        int cx = Math.round(x), cy = Math.round(y), rx = Math.max(2, Math.round(6 * k)), ry = Math.max(2, Math.round(4 * k));
        float w = lifted ? 1.6f : 0.7f;
        for (int i = -1; i <= 1; i++) {                                                                // three pairs of legs, scrabbling
            float ph = (float) Math.sin(t * (lifted ? 38 : 18) + i * 2.1f) * w * k;
            lin(c, cx + i * 3 * k, cy + ry * 0.4f, cx + i * 3 * k + ph - 2 * k, cy + ry + 3 * k, leg);
            lin(c, cx + i * 3 * k, cy + ry * 0.4f, cx + i * 3 * k - ph + 2 * k, cy + ry + 3 * k, leg);
        }
        if (lifted) {                                                                                    // wings buzz when it is off the ground
            float fl = (float) Math.sin(t * 60) * 0.5f + 0.5f;
            ell(c, cx - Math.round(2 * k), cy - ry, Math.max(1, Math.round(5 * k)), Math.max(1, Math.round((2 + 2 * fl) * k)), Canvas.rgb(120, 0xD8F0FF));
        }
        ell(c, cx, cy, rx + 1, ry + 1, dark);                                                            // outline
        ell(c, cx, cy, rx, ry, body);
        ell(c, cx - dir * Math.round(1 * k), cy - Math.round(1 * k), Math.max(1, rx - 2), Math.max(1, ry - 2), shell);
        c.rect(cx - 1, cy - ry, cx, cy + ry, dark);                                                      // the shell seam
        for (int i = -1; i <= 1; i += 2) c.disc(cx + i * Math.round(2.5f * k), cy - Math.round(1 * k), Math.max(1, Math.round(1 * k)), dark);
        int hx = cx + dir * (rx + Math.round(1 * k)), hr = Math.max(1, Math.round(2.6f * k));            // the head
        c.disc(hx, cy, hr + 1, dark); c.disc(hx, cy, hr, body);
        c.set(hx + dir, cy - 1, 0xFFFFFFFF); c.set(hx + dir, cy - 1 + (hr > 2 ? 1 : 0), 0xFFFFFFFF); c.set(hx + dir * 2, cy - 1, 0xFF000000);
        lin(c, hx, cy - hr, hx + dir * 3 * k, cy - hr - 3 * k, leg); lin(c, hx, cy - hr + 1, hx + dir * 4 * k, cy - hr - 1 * k, leg);   // antennae
    }

    /**
     * The Pest scene: a pest scuttles over the soil under a hovering vacuum. The vacuum switches on, dust and leaves stream into its nozzle,
     * the pest is plucked off the ground, spins and shrinks into the mouth; the vacuum gulps, puffs out a cloud (the pest is gone for good),
     * and the drop shoots out of the nozzle in a burst of light.
     */
    private static void drawPest(Canvas out, Params p, float t) {
        float ts = splitTime(p), us = Math.max(0f, t - ts - 0.12f), hit = impact(p, 0);
        boolean split = t >= ts;
        int gy = (int) CY - 4, cx = (int) CX, nzY = 30;
        int soil = 0xFF6B4A2E, soilDk = 0xFF3F2B1A, soilLt = 0xFF8A6240;
        float in = cl(t / 0.3f), a0 = split ? t - ts : 0f, gulp = t >= hit && t < hit + 0.3f ? (t - hit) / 0.3f : -1f;
        float shake = gulp >= 0f ? (float) Math.sin(gulp * 90) * 2.0f * (1 - gulp) : 0f;
        if (split && a0 < 0.3f) shake += (float) Math.sin(a0 * 90) * 2.6f * (1 - a0 / 0.3f);
        p.shakeX = shake * 0.6f; p.shakeY = 0;

        out.glow(cx, gy - 26, 50, 0xFFE9A0, 0.18f * in);
        ell(out, cx, gy + 5, 50, 9, Canvas.rgb((int) (110 * in), 0xFF07050C));                           // the patch of soil
        ell(out, cx, gy + 3, 48, 10, soilDk);
        ell(out, cx, gy + 1, 46, 8, soil);
        ell(out, cx - 6, gy - 1, 30, 4, soilLt);
        for (int i = -5; i <= 5; i++) out.rect(cx + i * 8 - 3, gy + 2 + (i % 2 == 0 ? 0 : 2), cx + i * 8 + 3, gy + 3 + (i % 2 == 0 ? 0 : 2), soilDk);
        for (int j = 0; j < 5; j++) {                                                                      // a few sprouts that sway in the draught
            int x = cx - 38 + j * 19 + (j > 2 ? 4 : 0), sw = Math.round((float) Math.sin(t * 6 + j) * (t > 0.4f && t < hit ? 1.6f : 0.6f));
            if (Math.abs(x - cx) < 9) continue;
            for (int s2 = -1; s2 <= 1; s2 += 2) out.rect(x + s2 * 2 + sw, gy - 7, x + s2 * 2 + 1 + sw, gy, 0xFF5FA03F);
            out.rect(x - 1 + sw, gy - 9, x + 2 + sw, gy - 6, 0xFF7CCB52);
        }

        if (p.alt) {                                                                                       // style 2: a downpour instead of the vacuum
            pestRain(out, p, t, ts, hit, gy, cx, a0);
            if (split && p.hasItem && p.item != null) { twinPass(out, p, t, ts, us, false, false, 0, false); drawItem(out, p, t, ts, us); twinPass(out, p, t, ts, us, false, false, gy - 18, true); }
            return;
        }
        // the vacuum: a hose from the top corner and a flared nozzle that drops in over the middle
        float drop = easeOut(t / 0.35f), ny = -22 + (nzY + 22) * drop;
        int nx = cx + Math.round(shake * 0.5f);
        float hoseA = t < ts + 1.5f ? 1f : 0f;
        if (hoseA > 0f && t > 0.02f) {
            for (int i = 0; i <= 40; i++) {                                                                // the hose: a curve from the nozzle's neck out to the top right
                float u = i / 40f, hx = nx + 4 + (Scene.W + 8 - nx - 4) * u, hy = ny - 14 - 18 * (float) Math.sin(u * Math.PI * 0.9f) + 6 * u;
                int hxi = Math.round(hx), hyi = Math.round(hy);
                out.rect(hxi - 2, hyi - 2, hxi + 3, hyi + 3, 0xFF2B2F3A);
                out.rect(hxi - 1, hyi - 2, hxi + 2, hyi, i % 4 < 2 ? 0xFF6C7488 : 0xFF565D70);
            }
        }
        int ty0 = Math.round(ny) - 16, ty1 = Math.round(ny);
        for (int y = ty0; y <= ty1; y++) {                                                                   // the nozzle: wider towards the mouth
            float u = (y - ty0) / (float) Math.max(1, ty1 - ty0); int half = 5 + Math.round(u * u * 8);
            out.rect(nx - half - 1, y, nx + half + 2, y + 1, 0xFF1A1D26);
            out.rect(nx - half, y, nx + half + 1, y + 1, y % 6 < 1 ? 0xFF9AA3B8 : 0xFF7A8296);
            out.rect(nx - half, y, nx - half + 2, y + 1, 0xFFB9C2D6);
        }
        out.rect(nx - 14, ty1, nx + 15, ty1 + 3, 0xFF1A1D26); out.rect(nx - 13, ty1, nx + 14, ty1 + 2, 0xFFFFC857);      // the rim, with a glowing core
        float on = cl((t - 0.3f) / 0.15f) * (t < ts ? 1f : cl(1f - (t - ts) / 0.4f));
        if (on > 0f) { out.glow(nx, ty1 + 2, 16 + (gulp >= 0f ? 8 * (1 - gulp) : 0), 0x8FE8FF, 0.5f * on); out.rect(nx - 10, ty1 + 3, nx + 11, ty1 + 4, Canvas.rgb((int) (230 * on), 0xBFF4FF)); }

        // dust, leaves and light streaming in
        if (on > 0f) for (int i = 0; i < 22; i++) {
            float ph = (t * 1.5f + h(i, 7)) % 1f, ang = (float) Math.toRadians(20 + 140 * h(i, 8)), rad = 8 + 44 * h(i, 9);
            float f = 1f - ph * ph, sx = nx + (float) Math.cos(ang) * rad * f * 1.3f, sy = ty1 + 3 + (float) Math.sin(ang) * rad * f * 0.95f;
            if (sy > gy + 2) continue;
            float tx = (nx - sx) * 0.12f, ty = (ty1 + 3 - sy) * 0.12f; int col = i % 3 == 0 ? 0xFF8FD65A : i % 3 == 1 ? 0xFFD9C9A0 : 0xFFE8F8FF;
            lin(out, sx, sy, sx + tx, sy + ty, Canvas.rgb((int) (190 * on * Math.sin(ph * Math.PI)), col));
        }

        // the pest: scuttles about, is lifted, tumbles into the nozzle
        if (t < hit) {
            float sxp = cx - 28, syp = gy - 2;
            float u = cl((t - 0.45f) / (hit - 0.45f)), e = u * u;
            float hop = t < 0.45f ? Math.abs((float) Math.sin(t * 9)) * 3f : 0f, walk = t < 0.45f ? (float) Math.sin(t * 5) * 6f : 0f;
            float bx = sxp + walk + (nx - sxp - walk) * e, by = syp - hop + (ty1 + 6 - syp) * e - 12 * (float) Math.sin(u * Math.PI) * (1 - u * 0.4f);
            float k = 1f - 0.55f * e, wob = (float) Math.sin(t * 30) * 1.2f * u;
            if (u > 0f) out.glow(bx, by, 10 * k + 6, 0xBFF4FF, 0.3f * u);
            bug(out, bx + wob, by, k, t, nx > bx ? 1 : -1, u > 0.05f);
        }
        // the gulp: a cloud and a cross where the pest was, little stars flying out
        if (gulp >= 0f) {
            float f = 1f - gulp;
            for (int i = 0; i < 4; i++) { int r = Math.round(4 + 12 * easeOut(gulp) + i * 2); out.disc(nx - 6 + i * 4, ty1 + 6 + (i % 2) * 3, r, Canvas.rgb((int) (120 * f / (1 + i * 0.5f)), i % 2 == 0 ? 0xFFEAF2E0 : 0xFFB7C9A0)); }
            for (int i = 0; i < 3 && gulp < 0.7f; i++) { int xx = nx - 6 + i * 6, yy = ty1 + 12; lin(out, xx - 2, yy - 2, xx + 2, yy + 2, Canvas.rgb((int) (255 * f), 0xFFFFFFFF)); lin(out, xx - 2, yy + 2, xx + 2, yy - 2, Canvas.rgb((int) (255 * f), 0xFFFFFFFF)); }
            for (int i = 0; i < 8; i++) { double an = Math.PI * (0.1 + 0.8 * h(i, 11)); float sp = 14 + 26 * h(i, 12); out.glint(Math.round(nx + (float) Math.cos(an) * sp * gulp), Math.round(ty1 + 6 + (float) Math.sin(an) * sp * gulp), 2, i % 2 == 0 ? 0xFFFFFF : 0xFFE9A0, f); }
        }
        // the drop shoots out of the nozzle in a burst of light
        if (t >= ts - 0.2f && t < ts) { float f = (t - (ts - 0.2f)) / 0.2f; out.glow(nx, ty1 + 3, 6 + 22 * f, 0xFFFFFF, 0.6f * f); }
        if (split) {
            float fl = cl(1f - a0 / 0.3f);
            out.glow(nx, ty1 + 4, 38, 0xFFFFFF, 0.7f * fl);
            for (int r = 0; r < 2; r++) {
                float ra = a0 - r * 0.07f, dur = 0.42f; if (ra < 0f || ra > dur) continue;
                float rad = 5 + 38 * easeOut(ra / dur);
                for (int i = 0; i < 100; i++) { double an = i * Math.PI * 2 / 100; out.set((int) (nx + Math.cos(an) * rad), (int) (ty1 + 4 + Math.sin(an) * rad * 0.5), Canvas.rgb((int) (200 * (1f - ra / dur)), r == 0 ? 0xFFFFFF : 0xFFFFE8A0)); }
            }
            for (int i = 0; i < 14; i++) {
                float life = a0 - h(i, 41) * 0.2f; if (life < 0f || life > 1.1f) continue;
                int x = (int) (nx + (h(i, 42) - 0.5f) * 40), y = (int) (ty1 + 6 + life * (8 + 22 * h(i, 43)) - Math.sin(life * 3) * 8);
                out.rect(x, y, x + 1, y + 1, Canvas.rgb((int) (230 * (1 - life / 1.1f)), i % 3 == 0 ? 0xFFFFFF : 0xFFE8A030));
            }
        }
        if (split && p.hasItem && p.item != null) { twinPass(out, p, t, ts, us, false, false, 0, false); drawItem(out, p, t, ts, us); twinPass(out, p, t, ts, us, false, false, gy - 18, true); }
    }

    /**
     * The Farming scene: a row of ripe crops, cut down by three quick golden slashes that sweep across the field (left, middle, right),
     * then the soil heaves and the rare crop springs out in a column of light. Ray of Helios (p.helios, 1 in 500,000,000) gets a longer one:
     * a sun rises behind the field, its rays turn, and the reveal is a huge burst.
     */
    private static void drawFarm(Canvas out, Params p, float t) {
        if (p.pest) { drawPest(out, p, t); return; }
        boolean hel = p.helios;
        float ts = splitTime(p), us = Math.max(0f, t - ts - 0.12f);
        boolean split = t >= ts;
        int soil = 0xFF6B4A2E, soilDk = 0xFF3F2B1A, soilLt = 0xFF8A6240;
        int gy = (int) CY - 4, cx = (int) CX, n = Math.max(1, p.chops), NC = 6;
        int gold = hel ? 0xFFFFD25A : 0xFFFFF0A0;
        float in = cl(t / 0.3f), a0 = split ? t - ts : 0f;
        float shake = 0f;
        for (int k = 0; k < n; k++) { float a = t - impact(p, k); if (a >= 0 && a < 0.2f) shake += (float) Math.sin(a * 90) * (hel ? 1.6f : 1.0f) * (1 - a / 0.2f); }
        if (split && a0 < 0.35f) shake += (float) Math.sin(a0 * 90) * (hel ? 4.5f : 2.4f) * (1 - a0 / 0.35f);
        p.shakeX = shake * 0.6f; p.shakeY = 0;

        if (hel) {                                                                          // the sun rises behind the field and its rays turn
            float r = easeOut(t / 1.6f), sr = 6 + 24 * r, sy = gy + 6 - 40 * r;
            out.glow(cx, sy, 70, 0xFFC040, 0.45f * r);
            for (int i = 0; i < 18; i++) {
                double an = i * Math.PI * 2 / 18 + t * 0.5; float len = 10 + 12 * (float) Math.abs(Math.sin(t * 5 + i * 1.7)), a1 = sr + 3;
                lin(out, cx + (float) Math.cos(an) * a1, sy + (float) Math.sin(an) * a1, cx + (float) Math.cos(an) * (a1 + len * r), sy + (float) Math.sin(an) * (a1 + len * r), Canvas.rgb((int) (170 * r), 0xFFD25A));
            }
            for (int d = 0; d < 6; d++) out.disc(cx, (int) sy, (int) (sr * (1 - d * 0.14f)), Canvas.mix(0xFFE8841A, 0xFFFFFFE0, d / 5f));
        } else out.glow(cx, gy - 26, 50, 0xFFE9A0, 0.22f * in);
        ell(out, cx, gy + 5, 50, 9, Canvas.rgb((int) (110 * in), 0xFF07050C));
        ell(out, cx, gy + 3, 48, 10, soilDk);
        ell(out, cx, gy + 1, 46, 8, soil);
        ell(out, cx - 6, gy - 1, 30, 4, soilLt);
        for (int i = -5; i <= 5; i++) out.rect(cx + i * 8 - 3, gy + 2 + (i % 2 == 0 ? 0 : 2), cx + i * 8 + 3, gy + 3 + (i % 2 == 0 ? 0 : 2), soilDk);

        // the crops: wheat, carrots and pumpkins in a row; every slash cuts its own third of the field
        for (int j = 0; j < NC; j++) {
            int x = cx - 35 + j * 14, k = Math.min(n - 1, j * n / NC);
            float ct = impact(p, k) + 0.025f * (j % 2), a = t - ct;
            float gr = easeOut((t - 0.03f * j) / 0.18f);
            int h0 = Math.round(11 * gr), sway = Math.round((float) Math.sin(t * 3 + j) * 0.8f);
            if (a < 0f) {
                if (j % 3 == 0) {                                                           // wheat
                    for (int s2 = -1; s2 <= 1; s2 += 2) { int sx = x + s2 * 2; out.rect(sx, gy - h0, sx + 1, gy, 0xFF6FB04A); if (h0 > 6) { out.rect(sx - 1 + sway, gy - h0 - 4, sx + 2 + sway, gy - h0 + 1, 0xFFE6C34A); out.set(sx + sway, gy - h0 - 5, 0xFFF4DE7A); } }
                } else if (j % 3 == 1) {                                                    // carrot fronds
                    for (int i = -3; i <= 3; i++) { int hh = Math.max(1, Math.round((h0 - 2 - Math.abs(i)) * 0.9f)); out.rect(x + i + sway / 2, gy - hh, x + i + 1 + sway / 2, gy, i % 2 == 0 ? 0xFF4FB848 : 0xFF6FD05A); }
                    out.rect(x - 1, gy - 1, x + 2, gy + 1, 0xFFEE8A2E);
                } else {                                                                    // pumpkin
                    int r = Math.round(4 * gr); out.rect(x - r - 1, gy - 2 * r, x + r + 2, gy, 0xFFE8892B); out.rect(x - r, gy - 2 * r - 1, x + r + 1, gy, 0xFFF29A3A); out.rect(x, gy - 2 * r - 2, x + 1, gy - 2 * r, 0xFF4FB848);
                }
            } else if (a < 0.4f) {                                                          // cut: bits fly up and fall
                for (int i = 0; i < 8; i++) {
                    double an = -Math.PI * (0.1 + 0.8 * h(i, 21 + j)); float sp = 22 + 36 * h(i, 31 + j);
                    int bx = (int) (x + Math.cos(an) * sp * a), by = (int) (gy - 6 + Math.sin(an) * sp * a + 0.5f * 200 * a * a);
                    int col = i % 3 == 0 ? 0xFF6FB04A : j % 3 == 0 ? 0xFFE6C34A : j % 3 == 1 ? 0xFFEE8A2E : 0xFFF29A3A;
                    out.rect(bx, by, bx + 2, by + 1, Canvas.rgb((int) (255 * (1 - a / 0.4f)), col));
                }
                out.rect(x - 1, gy - 1, x + 2, gy, soilDk);
            } else out.rect(x - 1, gy - 1, x + 2, gy, soilDk);
        }

        // the slashes: a bright crescent sweeps over each third of the field, alternating direction
        if (p.alt) rainStorm(out, p, t, ts, gy, cx, n, NC);
        for (int k = 0; k < n && !p.alt; k++) {
            float a = t - impact(p, k) + 0.07f;
            if (a < 0f || a > 0.26f) continue;
            float e = easeOut(a / 0.12f), fade = cl(1f - (a - 0.1f) / 0.16f);
            float zx = cx - 35 + ((k * NC / n) + (NC / n - 1) / 2f + 0.5f) * 14 - 7, R = hel ? 22 : 18;
            for (int i = 0; i <= 100; i++) {
                float u = i / 100f; if (u > e) break;
                double an = Math.toRadians(k % 2 == 0 ? -165 + 150 * u : -15 - 150 * u);
                for (int w = 0; w < 5; w++) {
                    float rr = R - w * 1.0f, th = (float) Math.sin(Math.PI * u);                // thin at both ends, thick in the middle
                    if (w > 0 && w > th * 4.5f) continue;
                    out.set((int) Math.round(zx + Math.cos(an) * rr), (int) Math.round(gy - 7 + Math.sin(an) * rr * 0.6f), Canvas.rgb((int) (255 * fade), w <= 1 ? 0xFFFFFF : w == 2 ? gold : 0xFFE8A030));
                }
            }
            if (e < 1f) out.glow(zx + (float) Math.cos(Math.toRadians(k % 2 == 0 ? -165 + 150 * e : -15 - 150 * e)) * R, gy - 7 + (float) Math.sin(Math.toRadians(k % 2 == 0 ? -165 + 150 * e : -15 - 150 * e)) * R * 0.6f, 7, 0xFFFFFF, 0.8f);
        }

        // before the reveal the soil in the middle starts to glow
        if (t >= ts - (hel ? 0.45f : 0.2f) && t < ts) {
            float f = (t - (ts - (hel ? 0.45f : 0.2f))) / (hel ? 0.45f : 0.2f);
            out.glow(cx, gy - 2, 6 + 22 * f, hel ? 0xFFE08A : 0xFFFFFF, 0.65f * f);
            ell(out, cx, gy - 1, (int) (3 + 9 * f), (int) (1 + 2 * f), Canvas.rgb((int) (230 * f), gold));
        }
        if (split) {
            float a = a0, fl = cl(1f - a / (hel ? 0.5f : 0.25f));
            out.glow(cx, gy - 24, hel ? 70 : 36, 0xFFFFFF, (hel ? 0.95f : 0.7f) * fl);
            float beam = cl(1f - a / (hel ? 1.2f : 0.7f));                                   // a column of light where the drop springs out
            if (beam > 0f) for (int y = 0; y < gy; y++) { float w = (hel ? 9f : 6f) * (1f - y / (float) gy * 0.5f) * easeOut(a / 0.1f); out.rect(cx - (int) w, y, cx + (int) w + 1, y + 1, Canvas.rgb((int) ((hel ? 90 : 60) * beam * (0.4f + 0.6f * y / gy)), gold)); }
            int rings = hel ? 3 : 2;
            for (int r = 0; r < rings; r++) {
                float ra = a - r * 0.07f, dur = hel ? 0.7f : 0.42f; if (ra < 0f || ra > dur) continue;
                float rad = 5 + (hel ? 52 : 40) * easeOut(ra / dur);
                for (int i = 0; i < 120; i++) { double an = i * Math.PI * 2 / 120; out.set((int) (cx + Math.cos(an) * rad), (int) (gy + Math.sin(an) * rad * 0.28f), Canvas.rgb((int) (200 * (1f - ra / dur)), r == 0 ? 0xFFFFFF : r == 1 ? gold : p.rarity & 0xFFFFFF)); }
            }
            for (int i = 0; i < (hel ? 26 : 14); i++) {                                       // grains of light rising from the soil
                float life = a - h(i, 41) * 0.25f; if (life < 0f || life > 1.2f) continue;
                int x = (int) (cx + (h(i, 42) - 0.5f) * 44), y = (int) (gy - life * (22 + 34 * h(i, 43)));
                out.rect(x, y, x + 1, y + 1, Canvas.rgb((int) (230 * (1 - life / 1.2f)), i % 3 == 0 ? 0xFFFFFF : gold));
            }
            if (hel) for (int i = 0; i < 16; i++) {                                           // the sun's rays burst outward
                double an = i * Math.PI * 2 / 16 + 0.2; float rl = 70 * easeOut(a / 0.5f), fa = cl(1f - a / 0.9f);
                lin(out, cx + (float) Math.cos(an) * 14, gy - 22 + (float) Math.sin(an) * 14, cx + (float) Math.cos(an) * rl, gy - 22 + (float) Math.sin(an) * rl, Canvas.rgb((int) (190 * fa), 0xFFD25A));
            }
        }
        if (split && p.hasItem && p.item != null) { twinPass(out, p, t, ts, us, false, false, 0, false); drawItem(out, p, t, ts, us); twinPass(out, p, t, ts, us, false, false, gy - 18, true); }
    }

    // ------------------------------------------------------------------------------------------------ the Normal Mythological scenes

    private static final String[] MINO = {"h..............h", "hh............hh", ".hh..........hh.", ".ffffffffffffff.", "ffffffffffffffff", "ffefffffffffeff.", "ffeffddddfffeff.", "fffffdsssdffff..",
        ".fffddsnnsddff..", "..ffdssssssdf...", "...fddssssddf...", "...ffffffffff...", "..dffffffffffd..", ".dddffffffffddd.", "dd.dd......dd.dd"};
    private static int minoCol(char ch) { return switch (ch) { case 'h' -> 0xFFE8DCB4; case 'f' -> 0xFF7A4A2C; case 'd' -> 0xFF4A2A18; case 's' -> 0xFFB88A60; case 'n' -> 0xFFFFD24A; default -> 0xFF802020; }; }
    private static char minoAt(int i, int j) { String row = MINO[j]; return i < row.length() ? row.charAt(i) : '.'; }

    /** A jagged bolt from the top of the canvas down to the ground at x. */
    private static void bolt(Canvas out, int x, float t, int seed) {
        float px = x + (h(seed, 1) - 0.5f) * 6, py = -2; int step = (int) (t * 60);
        for (int k = 0; k < 8; k++) {
            float nx = x + (h(seed + k * 3, step) - 0.5f) * 12 * (1 - k / 9f), ny = py + (70 - py) / (8 - k);
            lin(out, px - 1, py, nx - 1, ny, Canvas.rgb(120, 0x8CBEFF)); lin(out, px + 1, py, nx + 1, ny, Canvas.rgb(120, 0x8CBEFF)); lin(out, px, py, nx, ny, 0xFFFFFFFF);
            px = nx; py = ny;
        }
    }

    /** The light that follows the reveal: a flash, a column, two rings and grains of light rising off the ground at cx. */
    private static void reveal(Canvas out, float t, float ts, int cx, int gy, int col) {
        float a = t - ts; if (a < 0f) return;
        out.glow(cx, gy - 24, 36, 0xFFFFFF, 0.7f * cl(1f - a / 0.25f));
        float beam = cl(1f - a / 0.7f);
        if (beam > 0f) for (int y = 0; y < gy; y++) { float w = 6f * (1f - y / (float) gy * 0.5f) * easeOut(a / 0.1f); out.rect(cx - (int) w, y, cx + (int) w + 1, y + 1, Canvas.rgb((int) (60 * beam * (1f - y / (float) gy * 0.4f)), col)); }
        for (int r = 0; r < 2; r++) {
            float ra = a - r * 0.07f, dur = 0.42f; if (ra < 0f || ra > dur) continue;
            float rad = 5 + 40 * easeOut(ra / dur);
            for (int i = 0; i < 120; i++) { double an = i * Math.PI * 2 / 120; out.set((int) (cx + Math.cos(an) * rad), (int) (gy + Math.sin(an) * rad * 0.28), Canvas.rgb((int) (200 * (1f - ra / dur)), r == 0 ? 0xFFFFFF : 0xFFE8A0)); }
        }
        for (int i = 0; i < 14; i++) {
            float life = a - h(i, 41) * 0.25f; if (life < 0f || life > 1.2f) continue;
            int x = (int) (cx + (h(i, 42) - 0.5f) * 44), y = (int) (gy - life * (22 + 34 * h(i, 43)));
            out.rect(x, y, x + 1, y + 1, Canvas.rgb((int) (230 * (1 - life / 1.2f)), i % 3 == 0 ? 0xFFFFFF : col));
        }
    }

    /**
     * Mythological creature, Normal state: a rune circle charges, three lightning strikes land, the first calls up a minotaur, the next two hit it
     * (it flashes white, its eyes flare) and the third shatters it. A column of light rises and the drop springs out of the circle.
     */
    private static void drawMobsN(Canvas out, Params p, float t) {
        final float TS = splitTime(p), us = Math.max(0f, t - TS - 0.12f);
        final boolean split = t >= TS;
        final int cx = 56, gy = 70;
        p.shakeX = 0; p.shakeY = 0;
        float inn = cl(t / 0.3f);
        out.glow(cx, gy - 20, 44, 0x9C90FF, 0.10f * inn);
        ell(out, cx, gy + 4, 42, 6, Canvas.rgb((int) (110 * inn), 0x07050C));
        ell(out, cx, gy + 2, 40, 7, Canvas.rgb((int) (235 * inn), 0x1A1828));
        ell(out, cx, gy + 1, 37, 6, Canvas.rgb((int) (235 * inn), 0x34304C));
        ell(out, cx - 5, gy, 24, 3, Canvas.rgb((int) (190 * inn), 0x565078));
        float ring = cl((t - 0.05f) / 0.4f), pul = t > 0.5f ? 0.5f + 0.5f * (float) Math.sin(t * 8) : 0f;
        for (int i = 0; i < 100; i++) {
            double an = i * Math.PI * 2 / 100 + t * 0.6;
            out.set((int) Math.round(cx + Math.cos(an) * 31 * ring), (int) Math.round(gy + 1 + Math.sin(an) * 4.5 * ring), Canvas.rgb((int) (255 * (0.35f + 0.5f * ring) * (0.75f + 0.25f * pul)), 0xFFD764));
        }
        for (int i = 0; i < 8; i++) {                                                                       // runes turning round the circle
            double an = i * Math.PI / 4 + t * 0.6; int gx = (int) Math.round(cx + Math.cos(an) * 25 * ring), gz = (int) Math.round(gy + 1 + Math.sin(an) * 3.4 * ring);
            out.rect(gx, gz - 2, gx + 1, gz + 1, Canvas.rgb((int) (210 * ring), 0xFFEB96));
        }
        // the minotaur: called up by strike 1, hit by 2 and 3, shattered at the reveal
        float s0 = impact(p, 0), em = easeOut((t - s0 - 0.05f) / 0.45f);
        if (t > s0 && t < TS) {
            float shake = 0, flash = 0;
            for (int k = 1; k < 3; k++) { float a = t - impact(p, k); if (a >= 0f && a < 0.22f) { shake += (float) Math.sin(a * 100) * 1.5f * (1 - a / 0.22f); flash = Math.max(flash, 1 - a / 0.14f); } }
            flash = cl(flash);
            boolean eyes = t > impact(p, 1);
            int hh = Math.max(1, Math.round(15 * em)), ox = cx - 16 + Math.round(shake), oy = gy - 26 + Math.round(8 * (1 - em));
            for (int j = 0; j < hh; j++) for (int i = 0; i < 16; i++) {
                char ch = minoAt(i, 15 - hh + j); if (ch == '.') continue;
                int col = minoCol(ch);
                if (ch == 'e') col = eyes ? (Math.sin(t * 20) > 0 ? 0xFFFF5030 : 0xFFFFD0A0) : 0xFF802020;
                if (flash > 0f) col = Canvas.mix(col, 0xFFFFFFFF, flash);
                out.rect(ox + i * 2, oy + j * 2, ox + i * 2 + 2, oy + j * 2 + 2, col);
            }
            if (eyes) { out.glow(ox + 6, oy + 11, 6, 0xFF3C28, 0.35f); out.glow(ox + 26, oy + 11, 6, 0xFF3C28, 0.35f); }
        }
        if (t >= TS && t < TS + 0.7f) {                                                                     // it breaks into pieces that fly apart
            float a = t - TS;
            for (int j = 0; j < 15; j++) for (int i = 0; i < 16; i++) {
                char ch = minoAt(i, j); if (ch == '.') continue;
                int n = i * 15 + j; float vx = (i - 8) * (2 + h(n, 1) * 5), vy = -12 - h(n, 3) * 30;
                int px = Math.round(cx - 16 + i * 2 + vx * a), py = Math.round(gy - 26 + j * 2 + vy * a + 110 * a * a);
                if (py < gy + 2) out.rect(px, py, px + 2, py + 2, Canvas.rgb((int) (255 * (1 - a / 0.7f)), minoCol(ch)));
            }
        }
        for (int k = 0; k < 3; k++) {                                                                       // the three strikes
            float a = t - impact(p, k); if (a < 0f || a > 0.22f) continue;
            int bx = cx + (k == 0 ? -16 : k == 1 ? 11 : 0);
            if (((int) (a * 40)) % 2 == 0) bolt(out, bx, t, k * 17 + 3);
            out.glow(bx, gy, 16, 0xAAC8FF, 0.5f * (1 - a / 0.22f));
            for (int i = 0; i < 6; i++) { double an = -Math.PI * (0.1 + 0.8 * h(i, k)); float sp = 14 + 22 * h(i, k + 4); out.set(Math.round(bx + (float) Math.cos(an) * sp * a), Math.round(gy + (float) Math.sin(an) * sp * a * 0.7f + 60 * a * a), Canvas.rgb((int) (255 * (1 - a / 0.22f)), 0xFFE28A)); }
        }
        if (t >= TS - 0.2f && t < TS) { float f = (t - (TS - 0.2f)) / 0.2f; out.glow(cx, gy - 2, 6 + 22 * f, 0xFFFFFF, 0.65f * f); }
        reveal(out, t, TS, cx, gy, 0xFFE28A);
        if (split && p.hasItem && p.item != null) { twinPass(out, p, t, TS, us, false, false, 0, false); drawItem(out, p, t, TS, us); twinPass(out, p, t, TS, us, false, false, gy - 18, true); }
    }

    private static void digger(Canvas out, int x, int y, int bob) {
        out.rect(x - 3, y - 14 + bob, x + 4, y - 12 + bob, 0xFF7A4A24); out.rect(x - 5, y - 12 + bob, x + 6, y - 11 + bob, 0xFF6A3E1C);       // hat
        out.rect(x - 2, y - 11 + bob, x + 3, y - 7 + bob, 0xFFE0B088); out.set(x + 1, y - 10 + bob, 0xFF222222);                                 // face
        out.rect(x - 3, y - 7 + bob, x + 4, y + bob, 0xFF2F8F8A);                                                                                // coat
        out.rect(x - 3, y - 1, x, y + 5, 0xFF3A3A5A); out.rect(x + 1, y - 1, x + 4, y + 5, 0xFF3A3A5A);                                         // legs
        out.rect(x - 3, y + 5, x, y + 6, 0xFF222222); out.rect(x + 1, y + 5, x + 4, y + 6, 0xFF222222);                                         // boots
    }

    /**
     * Mythological treasure, Normal state: an explorer digs three times with a rigid shovel (thrust, lift, throw the clod onto the pile), the hole widens,
     * glints show, then a chest bursts up, its lid opens and the drop springs out in a column of light.
     */
    private static void drawTreasureN(Canvas out, Params p, float t) {
        final float TS = splitTime(p), us = Math.max(0f, t - TS - 0.12f);
        final boolean split = t >= TS;
        final int gy = 70, hx = 56, pileX = 92;
        final float[] D = {0.34f, 0.89f, 1.44f};
        p.shakeX = 0; p.shakeY = 0;
        float inn = cl(t / 0.3f);
        out.glow(56, gy - 20, 44, 0xFFE9A0, 0.14f * inn);
        ell(out, 56, gy + 5, 48, 7, Canvas.rgb((int) (110 * inn), 0x07050C));
        ell(out, 56, gy + 3, 46, 8, 0xFF3F2B1A); ell(out, 56, gy + 1, 44, 7, 0xFF6B4A2E); ell(out, 50, gy - 1, 28, 3, 0xFF8A6240);
        for (int i = -5; i <= 5; i++) out.rect(56 + i * 7 - 3, gy + 2 + (i % 2 == 0 ? 0 : 2), 56 + i * 7 + 3, gy + 3 + (i % 2 == 0 ? 0 : 2), 0xFF3F2B1A);
        int holeR = 0, stage = 0;
        for (int k = 0; k < 3; k++) { if (t > D[k] + 0.04f) holeR = 6 + k * 4; if (t > D[k] + 0.3f) stage = k + 1; }
        if (holeR > 0) { ell(out, hx, gy + 1, holeR + 1, Math.max(2, Math.round(holeR * 0.35f)), 0xFF2A1A0E); ell(out, hx, gy + 1, holeR, Math.max(1, Math.round(holeR * 0.3f)), 0xFF140C06); }
        for (int k = 0; k < stage; k++) { ell(out, pileX + (k - 1) * 5, gy - 1 - k, 6 + k * 2, 3 + k, 0xFF7A5636); ell(out, pileX + (k - 1) * 5 - 1, gy - 2 - k, 4 + k, 2 + k, 0xFF9A7048); }
        // the explorer: the shovel is swung from the hands by an angle (0 = right, positive = down) and keeps its length
        final double deg = Math.PI / 180;
        final float[][] key = {{0f, -62f}, {0.25f, 50f}, {0.4f, 50f}, {0.62f, -18f}, {0.78f, -72f}, {1f, -62f}};
        double th = -62 * deg; int lean = 0, bob = 0; float[] clod = null;
        for (int k = 0; k < 3; k++) {
            float u = (t - (D[k] - 0.14f)) / 0.56f; if (u < 0f || u >= 1f) continue;
            int i = 0; while (i < key.length - 2 && u > key[i + 1][0]) i++;
            float f = (u - key[i][0]) / (key[i + 1][0] - key[i][0]), e = f * f * (3 - 2 * f);
            th = (key[i][1] + (key[i + 1][1] - key[i][1]) * e) * deg;
            lean = u > 0.12f && u < 0.55f ? 2 : 0; bob = u > 0.12f && u < 0.4f ? 1 : 0;
            if (u >= 0.4f && u < 0.78f) clod = new float[]{36 + lean + (float) Math.cos(th) * 25, gy - 14 + (float) Math.sin(th) * 25 - 1};
            if (u >= 0.78f) { float q = (u - 0.78f) / 0.22f; clod = new float[]{hx + 10 + (pileX - hx - 8) * q, gy - 16 - (float) Math.sin(q * Math.PI) * 14 + q * q * 16}; }
        }
        if (t >= TS - 0.1f) { th = -80 * deg; bob = -1; }
        int body = 30 + lean, hhx = 36 + lean, hhy = gy - 14 + bob;
        if (t >= TS - 0.1f) { float u = t - TS; int up = Math.round(easeOut((u + 0.1f) / 0.4f) * 6), lid = Math.round(easeOut((u + 0.1f) / 0.35f) * 5);       // the chest bursts out of the hole
            out.rect(hx - 9, gy - 8 - up, hx + 9, gy + 1 - up, 0xFF8A5A2A); out.rect(hx - 9, gy - 8 - up, hx + 9, gy - 6 - up, 0xFFB07A3A); out.rect(hx - 1, gy - 6 - up, hx + 2, gy - 3 - up, 0xFFFFD24A);
            out.rect(hx - 9, gy - 10 - up - lid, hx + 9, gy - 7 - up - lid, 0xFFA06A30); out.rect(hx - 9, gy - 10 - up - lid, hx + 9, gy - 9 - up - lid, 0xFFC8904A);
            if (lid > 1) out.glow(hx, gy - 8 - up, 18, 0xFFE278, 0.55f * cl(lid / 5f));
        }
        digger(out, body, gy, bob);
        out.rect(body + 3, hhy - 3, hhx + 2, hhy - 1, 0xFFE0B088);                                         // the arm
        double dx = Math.cos(th), dy = Math.sin(th), px = -dy, py = dx;
        lin(out, (float) (hhx - dx * 5), (float) (hhy - dy * 5), (float) (hhx + dx * 18), (float) (hhy + dy * 18), 0xFF8A5A2C);
        lin(out, (float) (hhx - dx * 5 + px * 2), (float) (hhy - dy * 5 + py * 2), (float) (hhx - dx * 5 - px * 2), (float) (hhy - dy * 5 - py * 2), 0xFF8A5A2C);
        for (int q = 0; q < 8; q++) { double bx = hhx + dx * (18 + q), by = hhy + dy * (18 + q); int w = q < 2 ? 3 : q < 5 ? 2 : 1; for (int o = -w; o <= w; o++) out.set((int) Math.round(bx + px * o), (int) Math.round(by + py * o), q < 3 ? 0xFFD4DCF2 : 0xFF98A2C4); }
        if (clod != null) out.rect(Math.round(clod[0]) - 1, Math.round(clod[1]) - 1, Math.round(clod[0]) + 3, Math.round(clod[1]) + 2, 0xFF7A5636);
        for (int k = 0; k < 3; k++) {                                                                       // dirt puffs at every thrust
            float a = t - D[k]; if (a < 0f || a > 0.45f) continue;
            for (int i = 0; i < 7; i++) { double an = -Math.PI * (0.15 + 0.7 * h(i, k)); float sp = 10 + 24 * h(i, k + 9); out.rect(Math.round(hx + (float) Math.cos(an) * sp * a), Math.round(gy - 2 + (float) Math.sin(an) * sp * a + 90 * a * a), Math.round(hx + (float) Math.cos(an) * sp * a) + 2, Math.round(gy - 2 + (float) Math.sin(an) * sp * a + 90 * a * a) + 1, Canvas.rgb((int) (255 * (1 - a / 0.45f)), i % 2 == 0 ? 0xC8AA78 : 0x8A6240)); }
        }
        if (t > D[2] + 0.3f && t < TS) { float f = cl((t - (D[2] + 0.3f)) / (TS - D[2] - 0.3f)); out.glow(hx, gy, 10 + 10 * f, 0xFFE278, 0.5f * f); if (((int) (t * 14)) % 2 == 0) { int gx = hx + (int) Math.round(Math.sin(t * 9) * 5); out.rect(gx, gy - 1 - Math.round(f * 3), gx + 2, gy - Math.round(f * 3), 0xFFFFFFFF); } }
        reveal(out, t, TS, hx, gy, 0xFFE28A);
        if (split && p.hasItem && p.item != null) { twinPass(out, p, t, TS, us, false, false, 0, false); drawItem(out, p, t, TS, us); twinPass(out, p, t, TS, us, false, false, gy - 18, true); }
    }
    private Scene() {}
}
