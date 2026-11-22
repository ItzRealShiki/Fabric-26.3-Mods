package de.itzrealshiki.provisuals;

import de.itzrealshiki.provisuals.mixin.client.EntityRendererTextureAccessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/** A low-cost mirrored model submit for entities standing on configured reflective surfaces. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class GroundReflectionLayer extends RenderLayer<LivingEntityRenderState, EntityModel<LivingEntityRenderState>> {
    private final LivingEntityRenderer renderer;

    public GroundReflectionLayer(RenderLayerParent parent, LivingEntityRenderer renderer) {
        super(parent);
        this.renderer = renderer;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       LivingEntityRenderState state, float yRot, float xRot) {
        if (!ProVisualsConfig.isEnabled("ground_reflections") || state.isInvisible
                || state.distanceToCameraSq > 48.0 * 48.0) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        BlockPos floor = BlockPos.containing(state.x, state.y - 0.05, state.z);
        BlockState block = client.level.getBlockState(floor);
        boolean water = client.level.getFluidState(floor).is(FluidTags.WATER);
        if (!isReflectiveSurface(block, floor, water)) return;

        double surfaceY = water ? floor.getY() + 0.88 : floor.getY() + 1.0;
        double distanceFromSurface = Math.abs(state.y - surfaceY);
        if (distanceFromSurface > (water ? 0.22 : 0.14)) return;

        Identifier texture = textureFor(state);
        if (texture == null) return;

        float intensity = Math.max(0.0f, Math.min(1.0f, ProVisualsConfig.reflectionIntensity));
        float quality = switch (ProVisualsConfig.reflectionQuality) {
            case "LOW" -> 0.68f;
            case "HIGH" -> 1.0f;
            default -> 0.84f;
        };
        int alpha = Math.max(0, Math.min(150, Math.round(150.0f * intensity * quality)));
        if (alpha < 2) return;

        poseStack.pushPose();
        poseStack.scale(1.0f, -1.0f, 1.0f);
        int tint = (alpha << 24) | 0x00FFFFFF;
        collector.submitModel(getParentModel(), state, poseStack,
                RenderTypes.entityTranslucent(texture), lightCoords, OverlayTexture.NO_OVERLAY,
                tint, null, 0);
        poseStack.popPose();
    }

    private static boolean isReflectiveSurface(BlockState block, BlockPos pos, boolean water) {
        if (water) return true;
        return switch (ProVisualsConfig.reflectiveSurfaces) {
            case "WATER_ONLY" -> false;
            case "SELECTED" -> {
                String id = BuiltInRegistries.BLOCK.getKey(block.getBlock()).getPath();
                yield id.contains("glass") || id.contains("ice") || id.contains("copper")
                        || id.contains("iron") || id.contains("gold") || id.contains("diamond")
                        || id.contains("amethyst") || id.contains("prismarine") || id.contains("quartz");
            }
            default -> block.isFaceSturdy(Minecraft.getInstance().level, pos, Direction.UP);
        };
    }

    private Identifier textureFor(EntityRenderState state) {
        try {
            return ((EntityRendererTextureAccessor) renderer).provisuals$getTextureLocation(state);
        } catch (Throwable throwable) {
            return null;
        }
    }
}
