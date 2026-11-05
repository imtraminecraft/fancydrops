package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * Auras in the world, drawn by Minecraft's own particle engine: every AuraSim particle gets a real particle with one of our glowing
 * sprites (textures/particle/aura_*.png), lit at full brightness so it glows at night. Nothing is drawn by hand any more, so nothing can
 * flash over the screen. Particles very close to the camera fade out (they would otherwise fill the view in first person).
 */
final class AuraWorld {
    private AuraWorld() {}

    private record Sim(AuraSim sim, String aura) {}
    private static final Map<UUID, Sim> SIMS = new HashMap<>();
    private static ClientLevel lastLevel;

    /** Every client tick: step the aura of every player near you who wears one, and give new particles to the engine. */
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
                boolean firstPerson = p == mc.player && !mc.gameRenderer.mainCamera().isDetached();
                if (firstPerson && cf.cosHideFirst) continue;
                seen.add(p.getUUID());
                Sim s = SIMS.get(p.getUUID());
                if (s == null || !s.aura().equals(l.aura())) { if (s != null) kill(s.sim()); s = new Sim(new AuraSim(l.aura(), p.getUUID().hashCode()), l.aura()); SIMS.put(p.getUUID(), s); }
                s.sim().step(p.getX(), p.getY(), p.getZ(), Math.min(100, cf.cosAmount) / 100f, cf.cosSpeed / 100f, cf.cosCalm, cf.cosSize / 100f);
                for (AuraSim.Part part : s.sim().born) mc.particleEngine.add(new Glow(mc.level, p, part));
            }
            for (Iterator<Map.Entry<UUID, Sim>> it = SIMS.entrySet().iterator(); it.hasNext(); ) { var e = it.next(); if (!seen.contains(e.getKey())) { kill(e.getValue().sim()); it.remove(); } }
        } catch (Throwable t) { if (!warned) { warned = true; System.err.println("[FancyDrops] aura tick failed: " + t); } }
    }
    private static boolean warned;
    private static void kill(AuraSim s) { for (AuraSim.Part p : s.parts) p.dead = true; }

    private static final Map<String, TextureAtlasSprite> SPRITES = new HashMap<>();
    static TextureAtlasSprite sprite(String name) {
        return SPRITES.computeIfAbsent(name, n -> Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(Identifier.fromNamespaceAndPath("fancydrops", n)));
    }
    static void reloaded() { SPRITES.clear(); }
    static String spriteName(AuraSim.Part p) { return p.sprite == AuraSim.RUNE ? "aura_rune" + (p.rune & 3) : AuraSim.SPRITE[p.sprite]; }

    /** One aura particle: it reads its simulated particle every tick and follows the body when it should. */
    static final class Glow extends SingleQuadParticle {
        private final Player owner; private final AuraSim.Part part; private String sp;

        Glow(ClientLevel level, Player owner, AuraSim.Part part) {
            super(level, 0, 0, 0, sprite(spriteName(part)));
            this.owner = owner; this.part = part; this.sp = spriteName(part);
            this.hasPhysics = false; this.gravity = 0f; this.lifetime = Integer.MAX_VALUE;
            place(); this.xo = this.x; this.yo = this.y; this.zo = this.z;
            look();
        }
        private void place() {
            if (part.follow) setPos(owner.getX() + part.x, owner.getY() + part.y, owner.getZ() + part.z);
            else setPos(part.x, part.y, part.z);
        }
        private void look() {
            float a = part.a;
            var cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
            double d = Math.sqrt(cam.distanceToSqr(this.x, this.y, this.z));
            float near = (float) ((d - 0.6 - part.s) / 0.8);                                      // gone right at the eye, full a block or so away
            a *= near <= 0 ? 0f : near >= 1 ? 1f : near * near;
            int c = part.col;
            this.rCol = ((c >> 16) & 255) / 255f; this.gCol = ((c >> 8) & 255) / 255f; this.bCol = (c & 255) / 255f;
            this.alpha = Math.max(0f, Math.min(1f, a));
            this.quadSize = Math.max(0.001f, part.s * 0.65f);                                      // a quad reaches quadSize each way from its middle
        }
        @Override public void tick() {
            this.xo = this.x; this.yo = this.y; this.zo = this.z;
            if (part.dead || owner.isRemoved()) { remove(); return; }
            place(); look();
            String n = spriteName(part);
            if (!n.equals(sp)) { sp = n; setSprite(sprite(n)); }
        }
        @Override public void move(double dx, double dy, double dz) { }
        @Override protected int getLightCoords(float partial) { return 0xF000F0; }
        @Override protected Layer getLayer() { return Layer.TRANSLUCENT; }
    }
}
