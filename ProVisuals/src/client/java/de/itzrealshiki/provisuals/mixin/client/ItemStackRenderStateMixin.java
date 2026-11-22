package de.itzrealshiki.provisuals.mixin.client;

import de.itzrealshiki.provisuals.HeldItemGlowRenderer;
import de.itzrealshiki.provisuals.ProVisualsConfig;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Adds texture-derived outlines to third-person held items only. */
@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin {
    @Shadow ItemDisplayContext displayContext;

    @ModifyVariable(method = "submit", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int provisuals$textureMatchedHandOutline(int original) {
        if (!ProVisualsConfig.isEnabled("held_item_glow")) return original;

        // First-person items receive a separate normal submit plus a glow submit
        // from FirstPersonHeldItemGlowMixin. Do not turn the normal copy into an outline.
        if (displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return original;
        }

        if (!HeldItemGlowRenderer.isHandContext(displayContext)) return original;
        return HeldItemGlowRenderer.outlineColor((ItemStackRenderState) (Object) this);
    }
}