package de.itzrealshiki.provisuals;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.EntityModel;

/** Registers the lightweight mirrored-model layer on living entity renderers. */
public final class GroundReflectionRenderer {
    private static boolean registered;

    private GroundReflectionRenderer() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void register() {
        if (registered) return;
        registered = true;
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            RenderLayerParent parent = (RenderLayerParent) renderer;
            helper.register(new GroundReflectionLayer(parent, (LivingEntityRenderer) renderer));
        });
        Diagnostics.info("Ground reflection render layers registered");
    }
}
