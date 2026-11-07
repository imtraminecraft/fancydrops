package dev.fancydrops;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * Name tags over players (their name effect and suffix), and the menu preview of an aura. The auras themselves are AuraSim
 * (the particles) drawn in the world by AuraWorld.
 */
final class AuraFx {
    private AuraFx() {}

    /** One particle: x/z in blocks round the player, y up from the feet, size in blocks, colour with alpha, shape 0 = square, 1 = star, 2 = flat slice. */
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
    private static boolean warned;
    /** End of the game's extraction step: style the name tags about to be drawn. */
    static void extract(net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext ctx) {
        try { styleNameTags(ctx.levelState()); }
        catch (Throwable t) { if (!warned) { warned = true; System.err.println("[FancyDrops] name tags failed: " + t); } }
    }

    /** Every name tag about to be drawn this frame: a player's own tag, or the floating text a server (Hypixel) hangs over them, gets that player's name effect and icon.
     *  Done on the render states, so it works for every player, you included in F5, whatever draws the name. */
    private static void styleNameTags(net.minecraft.client.renderer.state.level.LevelRenderState ls) {
        Minecraft mc = Minecraft.getInstance(); if (mc.level == null) return;
        Map<UUID, Cosmetics.Look> looks = Cosmetics.looks(); if (looks.isEmpty()) return;
        FancyConfig cf = FancyConfig.get();
        List<net.minecraft.world.entity.player.Player> worn = new ArrayList<>();
        for (var p : mc.level.players()) { Cosmetics.Look l = looks.get(p.getUUID()); if (l != null && (l.fx() != null || l.suffix() != null)) worn.add(p); }
        if (worn.isEmpty()) return;
        Cosmetics.nameTagHooked = true;
        for (var st : ls.entityRenderStates) {
            if (st.nameTag == null) continue;
            net.minecraft.world.entity.player.Player owner = null;
            if (st instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState av) { for (var p : worn) if (p.getId() == av.id) { owner = p; break; } }
            else for (var p : worn) {                                                           // floating text: it belongs to the player right under it
                if (Math.abs(st.x - p.getX()) < 1.2 && Math.abs(st.z - p.getZ()) < 1.2 && st.y > p.getY() - 0.5 && st.y < p.getY() + 4.5) { owner = p; break; }
            }
            if (owner == null) continue;
            if (owner != mc.player && owner.distanceTo(mc.player) > cf.cosRange) continue;
            Component c = Cosmetics.styleTag(st.nameTag, owner.getUUID(), owner.getName().getString());
            if (c != null) st.nameTag = c;
        }
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

    /** One aura particle in the menu preview: its own sprite, tinted, centred on sx/sy, px pixels across. */
    static void drawPart(GuiGraphicsExtractor g, float sx, float sy, float px, AuraSim.Part q) { drawPart(g, sx, sy, px, q, 1f); }
    static void drawPart(GuiGraphicsExtractor g, float sx, float sy, float px, AuraSim.Part q, float k) {
        float a = q.a * k; if (a < 0.03f || px < 0.4f) return;
        int argb = (Math.round(Math.min(1f, a) * 255) << 24) | (q.col & 0xFFFFFF);
        Identifier id = Identifier.fromNamespaceAndPath("fancydrops", "textures/particle/" + AuraWorld.spriteName(q) + ".png");
        Gfx.push(g); Gfx.translate(g, sx - px / 2f, sy - px / 2f); g.pose().scale(px / 32f, px / 32f);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id, 0, 0, 0f, 0f, 32, 32, 32, 32, 32, 32, argb);
        Gfx.pop(g);
    }
}
