package de.itzrealshiki.provisuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Low-cost client-side block, armor-wearer and dropped-item outlines. */
public final class GlowOutlineRenderer {
    private static final Map<Entity, Boolean> ORIGINAL_GLOW = new WeakHashMap<>();
    private static boolean registered;

    private GlowOutlineRenderer() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(GlowOutlineRenderer::renderOreOutline);
    }

    public static void tick(Minecraft client) {
        Set<Entity> wanted = Collections.newSetFromMap(new IdentityHashMap<>());
        if (client.level != null) {
            boolean armorGlow = ProVisualsConfig.isEnabled("armor_glow");
            boolean itemGlow = ProVisualsConfig.isEnabled("item_outline_glow");
            if (armorGlow || itemGlow) {
                for (Entity entity : client.level.entitiesForRendering()) {
                    boolean shouldGlow = itemGlow && entity instanceof ItemEntity;
                    if (!shouldGlow && armorGlow && entity instanceof LivingEntity living) {
                        shouldGlow = hasArmor(living);
                    }
                    if (!shouldGlow) continue;

                    ORIGINAL_GLOW.putIfAbsent(entity, entity.hasGlowingTag());
                    entity.setGlowingTag(true);
                    wanted.add(entity);
                }
            }
        }

        Iterator<Map.Entry<Entity, Boolean>> iterator = ORIGINAL_GLOW.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Entity, Boolean> entry = iterator.next();
            if (!wanted.contains(entry.getKey())) {
                entry.getKey().setGlowingTag(entry.getValue());
                iterator.remove();
            }
        }
    }

    private static boolean hasArmor(LivingEntity living) {
        return !living.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                || !living.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                || !living.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                || !living.getItemBySlot(EquipmentSlot.FEET).isEmpty();
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
