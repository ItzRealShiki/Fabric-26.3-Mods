package de.itzrealshiki.hotbardisplay.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.itzrealshiki.hotbardisplay.client.HotbarCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class PlayerHotbarLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float ICON_SCALE = 0.18F;
    private static final float SLOT_SPACING = 0.22F;
    private static final float HEAD_GAP = 0.28F;

    public PlayerHotbarLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack ignoredPoseStack,
                       net.minecraft.client.renderer.SubmitNodeCollector collector,
                       int lightCoords,
                       AvatarRenderState state,
                       float yRot,
                       float xRot) {
        if (state.isInvisible || state.isSpectator) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        List<ItemStack> items;
        if (minecraft.player != null && state.id == minecraft.player.getId()) {
            if (!PlayerHotbarDisplayClient.isOwnHotbarVisible()) return;
            items = java.util.stream.IntStream.range(0, 9)
                    .mapToObj(slot -> minecraft.player.getInventory().getItem(slot))
                    .map(ItemStack::copy)
                    .toList();
        } else {
            items = HotbarCache.get(state.id);
        }
        if (items.isEmpty()) return;

        var camera = minecraft.gameRenderer.mainCamera();
        var cameraPosition = camera.position();

        PoseStack matrices = new PoseStack();
        matrices.translate(
                state.x - cameraPosition.x,
                state.y - cameraPosition.y + state.boundingBoxHeight + HEAD_GAP,
                state.z - cameraPosition.z
        );
        matrices.rotate(camera.rotation());
        matrices.rotateDegrees(com.mojang.math.Axis.YP, 180.0F);
        matrices.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);

        for (int slot = 0; slot < Math.min(9, items.size()); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;

            matrices.pushPose();
            matrices.translate((slot - 4) * SLOT_SPACING / ICON_SCALE, 0.0F, 0.0F);

            ItemStackRenderState itemState = new ItemStackRenderState();
            minecraft.getItemModelResolver().updateForTopItem(
                    itemState, stack, ItemDisplayContext.GUI, minecraft.level, null, state.id + slot
            );
            itemState.submit(matrices, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            matrices.popPose();
        }
    }
}
