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
 * The hand renderer uses a separate feature-render pass from the world renderer.
 * Keep the normal item submit intact and add a slightly enlarged, texture-colored
 * outline submit behind it. FirstPersonHeldItemOutlinePhaseMixin drains that pass.
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
        if (!ProVisualsConfig.isEnabled("held_item_glow")) {
            itemState.submit(poseStack, collector, lightCoords, overlayCoords, originalOutlineColor);
            return;
        }

        // Preserve the textured item in the normal hand-render phase.
        itemState.submit(poseStack, collector, lightCoords, overlayCoords, 0);

        // Add a texture-matched silhouette slightly behind the original item.
        int sampled = HeldItemGlowRenderer.outlineColor(itemState);
        int glowColor = (sampled & 0x00FFFFFF) | 0xC0000000;
        poseStack.pushPose();
        poseStack.scale(1.09F, 1.09F, 1.09F);
        itemState.submit(poseStack, collector, lightCoords, overlayCoords, glowColor);
        poseStack.popPose();
    }
}