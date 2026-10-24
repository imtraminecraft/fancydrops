package dev.fancydrops;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/** The door the name-tag mixin walks through (the mixin lives in another package, Cosmetics is package-private). */
public final class NameTagHook {
    /** True once the mixin has run: the head-name overlay is then not needed. */
    public static volatile boolean hooked;

    public static Component tag(Component base, Entity e) {
        hooked = true;
        try { return Cosmetics.nameTag(base, e); } catch (Throwable t) { return null; }
    }
    private NameTagHook() {}
}
