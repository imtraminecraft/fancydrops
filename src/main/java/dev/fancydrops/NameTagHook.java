package dev.fancydrops;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/** The door the name-tag mixin walks through (the mixin lives in another package, Cosmetics is package-private). */
public final class NameTagHook {

    public static Component tag(Component base, Entity e) {
        Cosmetics.nameTagHooked = true;
        try { return Cosmetics.nameTag(base, e); } catch (Throwable t) { return null; }
    }
    private NameTagHook() {}
}
