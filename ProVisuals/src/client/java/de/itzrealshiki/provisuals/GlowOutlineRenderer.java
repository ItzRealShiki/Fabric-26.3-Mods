package de.itzrealshiki.provisuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Client-side outline render-state hooks for dropped items, friends and ore blocks. */
public final class GlowOutlineRenderer {
    public static final int VIOLET = 0xFF9A6BFF;
    public static final int FRIEND_COLOR = 0xFFFF83C7;
    public static final int TRIM_COLOR = 0xFFFFD17A;
    private static boolean registered;

    private GlowOutlineRenderer() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        LevelExtractionEvents.END_EXTRACTION.register(
                context -> applyEntityOutlines(context.levelState()));
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(GlowOutlineRenderer::renderOreOutline);
        Diagnostics.info("Glow render-state hooks registered");
    }

    /**
     * Assigns entity outlines during 26.3 render-state extraction.
     * Armor is deliberately excluded here: its four slots are handled separately
     * by HumanoidArmorLayerMixin so one equipped piece cannot outline the whole body.
     */
    private static void applyEntityOutlines(LevelRenderState levelState) {
        boolean itemGlow = ProVisualsConfig.isEnabled("item_outline_glow");
        boolean friendsGlow = ProVisualsConfig.isEnabled("visual_friends");
        if (!itemGlow && !friendsGlow) return;

        for (EntityRenderState state : levelState.entityRenderStates) {
            if (itemGlow && state instanceof ItemEntityRenderState) {
                state.outlineColor = VIOLET;
                continue;
            }

            if (friendsGlow && state.nameTag != null
                    && FriendManager.isFriend(state.nameTag.getString())) {
                state.outlineColor = FRIEND_COLOR;
            }
        }
    }

    /** Outline color supplied to the individual equipment piece render call. */
    public static int armorPieceOutline(ItemStack stack, int original) {
        return ProVisualsConfig.isEnabled("armor_glow") && stack != null && !stack.isEmpty()
                ? VIOLET : original;
    }

    /** A trim is a separate submitted model, so it receives its own independent color. */
    public static int trimOutline(int original) {
        return ProVisualsConfig.isEnabled("glowing_armor_trims") ? TRIM_COLOR : 0;
    }

    private static boolean renderOreOutline(LevelRenderContext context, BlockOutlineRenderState outline) {
        if (!ProVisualsConfig.isEnabled("glowing_ores")) return true;
        if (!ProVisualsConfig.oreOutlineGlow && !ProVisualsConfig.oreInteriorGlow) return true;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return true;

        BlockPos pos = outline.pos();
        String path = BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(pos).getBlock()).getPath();
        if (!path.endsWith("_ore") && !path.contains("_ore_")) return true;

        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

        if (ProVisualsConfig.oreInteriorGlow) {
            context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(),
                    (pose, buffer) -> drawFilledBox(pose, buffer,
                            new AABB(0.025, 0.025, 0.025, 0.975, 0.975, 0.975), 0x269A6BFF));
        }
        if (ProVisualsConfig.oreOutlineGlow) {
            submitEdgeLayer(context, poseStack, 0.105, 0x559A6BFF);
            submitEdgeLayer(context, poseStack, 0.032, 0xFFE4D7FF);
        }

        poseStack.popPose();
        return true;
    }

    private static void submitEdgeLayer(LevelRenderContext context, PoseStack poseStack,
                                        double thickness, int color) {
        context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(),
                (pose, buffer) -> {
                    double half = thickness / 2.0;
                    for (int y = 0; y <= 1; y++) {
                        for (int z = 0; z <= 1; z++) {
                            drawFilledBox(pose, buffer, new AABB(
                                    -half, y - half, z - half, 1.0 + half, y + half, z + half
                            ), color);
                        }
                    }
                    for (int x = 0; x <= 1; x++) {
                        for (int z = 0; z <= 1; z++) {
                            drawFilledBox(pose, buffer, new AABB(
                                    x - half, -half, z - half, x + half, 1.0 + half, z + half
                            ), color);
                        }
                    }
                    for (int x = 0; x <= 1; x++) {
                        for (int y = 0; y <= 1; y++) {
                            drawFilledBox(pose, buffer, new AABB(
                                    x - half, y - half, -half, x + half, y + half, 1.0 + half
                            ), color);
                        }
                    }
                });
    }

    private static void drawFilledBox(PoseStack.Pose pose, VertexConsumer out, AABB box, int color) {
        out.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color);
        out.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.minZ).setColor(color);
        out.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.minZ).setColor(color);
        out.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.minZ).setColor(color);

        out.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.maxZ).setColor(color);
        out.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.maxZ).setColor(color);
        out.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.maxZ).setColor(color);
        out.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.maxZ).setColor(color);

        out.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.maxZ).setColor(color);
        out.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color);
        out.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.minZ).setColor(color);
        out.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.maxZ).setColor(color);

        out.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.minZ).setColor(color);
        out.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.maxZ).setColor(color);
        out.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.maxZ).setColor(color);
        out.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.minZ).setColor(color);

        out.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.minZ).setColor(color);
        out.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.minZ).setColor(color);
        out.addVertex(pose, (float) box.maxX, (float) box.maxY, (float) box.maxZ).setColor(color);
        out.addVertex(pose, (float) box.minX, (float) box.maxY, (float) box.maxZ).setColor(color);

        out.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.maxZ).setColor(color);
        out.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.maxZ).setColor(color);
        out.addVertex(pose, (float) box.maxX, (float) box.minY, (float) box.minZ).setColor(color);
        out.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color);
    }
}
