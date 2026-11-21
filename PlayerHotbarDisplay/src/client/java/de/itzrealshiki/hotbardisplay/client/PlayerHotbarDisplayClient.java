package de.itzrealshiki.hotbardisplay.client;

import de.itzrealshiki.hotbardisplay.network.HotbarPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PlayerHotbarDisplayClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.clientboundPlay().register(HotbarPayload.TYPE, HotbarPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(HotbarPayload.TYPE, (payload, context) ->
                HotbarCache.update(payload.entityId(), payload.items())
        );
        PlayerHotbarRenderRegistration.register();
    }
}
