package de.itzrealshiki.provisuals.mixin.client;

import com.mojang.renderpearl.api.commands.RenderPass;
import de.itzrealshiki.provisuals.ProVisualsConfig;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The dedicated first-person hand/screen feature pass omits executeOutline().
 * Drain the outline phase before the normal item phase so the enlarged held-item
 * silhouette is behind the textured item instead of replacing it.
 */
@Mixin(FeatureRenderDispatcher.class)
public abstract class FirstPersonHeldItemOutlinePhaseMixin {
    @Inject(method = "renderAllFeatures", at = @At("HEAD"))
    private static void provisuals$renderFirstPersonOutlinePhase(
            RenderPass renderPass,
            FeatureRenderDispatcher.PreparedFrame frame,
            CallbackInfo ci
    ) {
        if (!ProVisualsConfig.isEnabled("held_item_glow") || !frame.hasAnyOutline()) return;
        frame.executeOutline(renderPass);
    }
}
