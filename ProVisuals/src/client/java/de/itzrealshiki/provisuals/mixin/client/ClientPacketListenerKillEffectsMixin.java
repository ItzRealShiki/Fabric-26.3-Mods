package de.itzrealshiki.provisuals.mixin.client;

import de.itzrealshiki.provisuals.KillEffectsRenderer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes broadcast chat death messages so kill effects only play on confirmed kills. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerKillEffectsMixin {
    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void provisuals$checkPlayerKill(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        Minecraft.getInstance().execute(() -> KillEffectsRenderer.onDeathMessage(packet.content()));
    }
}
