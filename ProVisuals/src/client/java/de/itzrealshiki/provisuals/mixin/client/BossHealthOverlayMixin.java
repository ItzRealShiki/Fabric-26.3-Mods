package de.itzrealshiki.provisuals.mixin.client;

import de.itzrealshiki.provisuals.BossBarLayoutBridge;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Places vanilla boss bars underneath the current SkyLyrics Music Island. */
@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
    @ModifyConstant(
            method = "extractRenderState",
            constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 12)
    )
    private int provisuals$placeBossBarsUnderSkyLyrics(int vanillaY) {
        return BossBarLayoutBridge.bossBarTop(vanillaY);
    }
}
