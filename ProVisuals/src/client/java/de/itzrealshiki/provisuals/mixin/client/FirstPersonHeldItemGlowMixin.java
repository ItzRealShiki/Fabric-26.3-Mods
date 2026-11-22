package de.itzrealshiki.provisuals.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.itzrealshiki.provisuals.HeldItemGlowRenderer;
import de.itzrealshiki.provisuals.ProVisualsConfig;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * First-person hand rendering supplies an outline color of zero at each held-item submit.
 * Redirect those exact submits so the mainhand and offhand receive the same texture-derived
 * outline as third-person held items, without depending on an item display-context shadow.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHeldItemGlowMixin {
    @Redirect(
            method = "submitArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
            )
    )
    private void provisuals$addFirstPersonHeldItemOutline(
            ItemStackRenderState itemState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            int originalOutlineColor
    ) {
        int outlineColor = ProVisualsConfig.isEnabled("held_item_glow")
                ? HeldItemGlowRenderer.outlineColor(itemState)
                : originalOutlineColor;
        itemState.submit(poseStack, collector, lightCoords, overlayCoords, outlineColor);
    }
}
