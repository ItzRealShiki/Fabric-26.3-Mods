package com.vextex.pvpmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vextex.pvpmod.PvPFeature;
import com.vextex.pvpmod.PvPModConfig;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(
            method = "submitArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void pvpmod$scaleCosmeticItems(
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState state,
            float partialTicks,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack stack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        if (hand != InteractionHand.MAIN_HAND && hand != InteractionHand.OFF_HAND) {
            return;
        }

        if (PvPModConfig.isEnabled(PvPFeature.TOTEM_SIZE) && stack.is(Items.TOTEM_OF_UNDYING)) {
            poseStack.scale(0.78F, 0.78F, 0.78F);
            return;
        }

        // Small Held Items intentionally applies only to utility and consumable items.
        // Weapons and shields are never modified by this feature.
        if (PvPModConfig.isEnabled(PvPFeature.SMALL_HELD_ITEMS)
                && (stack.getItem() instanceof PotionItem
                || stack.is(Items.MILK_BUCKET)
                || stack.is(Items.HONEY_BOTTLE)
                || stack.is(Items.GOLDEN_APPLE)
                || stack.is(Items.ENCHANTED_GOLDEN_APPLE)
                || stack.is(Items.APPLE)
                || stack.is(Items.BREAD)
                || stack.is(Items.COOKED_BEEF)
                || stack.is(Items.COOKED_PORKCHOP)
                || stack.is(Items.COOKED_CHICKEN)
                || stack.is(Items.COOKED_MUTTON)
                || stack.is(Items.BAKED_POTATO)
                || stack.is(Items.CARROT)
                || stack.is(Items.GOLDEN_CARROT)
                || stack.is(Items.PUMPKIN_PIE)
                || stack.is(Items.COOKIE))) {
            poseStack.scale(0.82F, 0.82F, 0.82F);
        }
    }
}
