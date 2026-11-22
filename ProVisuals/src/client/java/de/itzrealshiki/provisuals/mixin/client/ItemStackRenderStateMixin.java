package de.itzrealshiki.provisuals.mixin.client;

import de.itzrealshiki.provisuals.HeldItemGlowRenderer;
import de.itzrealshiki.provisuals.ProVisualsConfig;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Routes only hand display contexts into the item-texture-derived outline pass. */
@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin {
    @Shadow ItemDisplayContext displayContext;

    @ModifyVariable(method = "submit", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int provisuals$textureMatchedHandOutline(int original) {
        if (!ProVisualsConfig.isEnabled("held_item_glow")) return original;
        if (!HeldItemGlowRenderer.isHandContext(displayContext)) return original;
        return HeldItemGlowRenderer.outlineColor((ItemStackRenderState) (Object) this);
    }
}
