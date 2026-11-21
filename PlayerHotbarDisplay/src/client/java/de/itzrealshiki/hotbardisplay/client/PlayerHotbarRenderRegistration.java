package de.itzrealshiki.hotbardisplay.client;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.AvatarRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public final class PlayerHotbarRenderRegistration {
    private PlayerHotbarRenderRegistration() {}

    @SuppressWarnings("unchecked")
    public static void register() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?>) {
                helper.register(new PlayerHotbarLayer(
                        (RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) renderer
                ));
            }
        });
    }
}
