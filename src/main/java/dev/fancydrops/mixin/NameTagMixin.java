package dev.fancydrops.mixin;

import dev.fancydrops.NameTagHook;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The name over a player's head: swaps in the cosmetic version (effect colours and suffix). If this Minecraft names the method differently the mixin is skipped and the mod falls back to its own overlay. */
@Mixin(EntityRenderer.class)
public abstract class NameTagMixin {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true, require = 0)
    private void fancydrops$tag(Entity entity, CallbackInfoReturnable<Component> cir) {
        Component c = NameTagHook.tag(cir.getReturnValue(), entity);
        if (c != null) cir.setReturnValue(c);
    }
}
