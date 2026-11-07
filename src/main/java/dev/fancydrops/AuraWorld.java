package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.*;

/**
 * Auras in the world. The particles (AuraSim) step 20 times a second; every frame each one is placed between where it was and where it is
 * now, projected to the screen and drawn there with its own glowing sprite. Nothing is handed to the game's world rendering at all.
 *
 * Seeing through walls is avoided by looking from the camera to the wearer: when a block is in the way the aura fades out. Particles
 * behind the wearer (further from you than their body) are drawn fainter, as if the body hid part of them.
 */
final class AuraWorld {
    private AuraWorld() {}

    private record Sim(AuraSim sim, String aura) {}
    private static final Map<UUID, Sim> SIMS = new HashMap<>();
    private static final Map<UUID, Float> SEEN = new HashMap<>();        // 0..1: how visible each wearer is (fades when a wall is in the way)
    private static ClientLevel lastLevel;
    private static boolean warned;

    /** Every client tick: step the aura of every player near you who wears one. */
    static void tick(Minecraft mc) {
        try {
            if (mc.level == null || mc.player == null || mc.isPaused()) { if (mc.level == null) SIMS.clear(); return; }
            if (mc.level != lastLevel) { SIMS.clear(); lastLevel = mc.level; }
            Map<UUID, Cosmetics.Look> looks = Cosmetics.looks();
            FancyConfig cf = FancyConfig.get();
            Set<UUID> allowed = null;
            if (cf.cosMax > 0) {                                                                   // only the nearest few other players
                List<Player> near = new ArrayList<>();
                for (Player p : mc.level.players()) if (p != mc.player) { Cosmetics.Look l = looks.get(p.getUUID()); if (l != null && l.aura() != null) near.add(p); }
                near.sort(Comparator.comparingDouble(q -> mc.player.distanceToSqr(q)));
                allowed = new HashSet<>(); for (int i = 0; i < Math.min(cf.cosMax, near.size()); i++) allowed.add(near.get(i).getUUID());
            }
            Set<UUID> seen = new HashSet<>();
            for (Player p : mc.level.players()) {
                Cosmetics.Look l = looks.get(p.getUUID());
                if (l == null || l.aura() == null || p.isInvisible() || p.isSpectator()) continue;
                if (p != mc.player && (mc.player.distanceTo(p) > cf.cosRange || (allowed != null && !allowed.contains(p.getUUID())))) continue;
                seen.add(p.getUUID());
                Sim s = SIMS.get(p.getUUID());
                if (s == null || !s.aura().equals(l.aura())) { s = new Sim(new AuraSim(l.aura(), p.getUUID().hashCode()), l.aura()); SIMS.put(p.getUUID(), s); }
                s.sim().step(p.getX(), p.getY(), p.getZ(), Math.min(100, cf.cosAmount) / 100f, cf.cosSpeed / 100f, cf.cosCalm, cf.cosSize / 100f);
            }
            SIMS.keySet().retainAll(seen);
        } catch (Throwable t) { if (!warned) { warned = true; System.err.println("[FancyDrops] aura tick failed: " + t); } }
    }

    static String spriteName(AuraSim.Part p) { return p.sprite == AuraSim.RUNE ? "aura_rune" + (p.rune & 3) : AuraSim.SPRITE[p.sprite]; }

    /** Every frame (HUD): draw every aura particle at its place on the screen. */
    static void render(GuiGraphicsExtractor g, float partial) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || SIMS.isEmpty()) return;
            FancyConfig cf = FancyConfig.get();
            var cam = mc.gameRenderer.mainCamera(); Vec3 cp = cam.position();
            Matrix4f vp = cam.getViewRotationProjectionMatrix(new Matrix4f());
            int W = g.guiWidth(), H = g.guiHeight();
            double ppb = H / (2 * Math.tan(Math.toRadians(mc.options.fov().get()) / 2));          // screen pixels per block at one block away
            List<float[]> draw = new ArrayList<>(); List<AuraSim.Part> who = new ArrayList<>();
            for (Player p : mc.level.players()) {
                Sim s = SIMS.get(p.getUUID()); if (s == null) continue;
                if (p == mc.player && !cam.isDetached()) continue;                                 // your own aura is for others (and for you in F5)
                Vec3 feet = p.getPosition(partial), chest = feet.add(0, 1.1, 0);
                boolean blocked = p != mc.player && mc.level.clip(new ClipContext(cp, chest, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.BLOCK;
                float vis = SEEN.getOrDefault(p.getUUID(), 0f); vis += ((blocked ? 0f : 1f) - vis) * 0.15f; SEEN.put(p.getUUID(), vis);
                if (vis < 0.02f) continue;
                Vector4f bc = new Vector4f((float) (chest.x - cp.x), (float) (chest.y - cp.y), (float) (chest.z - cp.z), 1f); vp.transform(bc);
                float bodyDepth = bc.w;
                for (AuraSim.Part q : s.sim().parts) {
                    if (q.a < 0.03f) continue;
                    double lx = q.ox + (q.x - q.ox) * partial, ly = q.oy + (q.y - q.oy) * partial, lz = q.oz + (q.z - q.oz) * partial;
                    double wx = q.follow ? feet.x + lx : lx, wy = q.follow ? feet.y + ly : ly, wz = q.follow ? feet.z + lz : lz;
                    Vector4f v = new Vector4f((float) (wx - cp.x), (float) (wy - cp.y), (float) (wz - cp.z), 1f); vp.transform(v);
                    if (v.w < 0.6f) continue;                                                      // at or behind the eye: never drawn
                    float sx = (v.x / v.w * 0.5f + 0.5f) * W, sy = (1f - (v.y / v.w * 0.5f + 0.5f)) * H, px = (float) (q.s * ppb / v.w) * 1.3f;
                    if (sx < -40 || sx > W + 40 || sy < -40 || sy > H + 40) continue;
                    if (px < 0.7f) continue;                                                       // too small to see: skip
                    float a = q.a * vis * (v.w > bodyDepth + 0.15f ? 0.4f : 1f);                    // behind the body: fainter
                    draw.add(new float[]{sx, sy, px, a, v.w}); who.add(q);
                }
            }
            if (draw.size() > 700) { draw.subList(700, draw.size()).clear(); who.subList(700, who.size()).clear(); }   // a crowd of auras: a fixed budget of sprites
            Integer[] order = new Integer[draw.size()]; for (int i = 0; i < order.length; i++) order[i] = i;
            Arrays.sort(order, (a, b) -> Float.compare(draw.get(b)[4], draw.get(a)[4]));           // far ones first
            for (int i : order) { float[] d = draw.get(i); AuraFx.drawPart(g, d[0], d[1], Math.min(48f, d[2]), who.get(i), d[3]); }
        } catch (Throwable t) { if (!warned) { warned = true; System.err.println("[FancyDrops] aura draw failed: " + t); } }
    }
}
