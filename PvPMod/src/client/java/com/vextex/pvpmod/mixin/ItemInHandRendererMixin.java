package com.vextex.pvpmod.mixin;

import com.vextex.pvpmod.PvPFeature;
import com.vextex.pvpmod.PvPModConfig;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void pvpmod$scaleCosmeticItems(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            CallbackInfo ci
    ) {
        if (displayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                && displayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return;
        }

        if (PvPModConfig.isEnabled(PvPFeature.TOTEM_SIZE) && stack.is(Items.TOTEM_OF_UNDYING)) {
            poseStack.scale(0.78F, 0.78F, 0.78F);
            return;
        }

        // Small Held Items intentionally applies only to safe utility/consumable items.
        // It does not alter weapons, shields, or combat equipment.
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
