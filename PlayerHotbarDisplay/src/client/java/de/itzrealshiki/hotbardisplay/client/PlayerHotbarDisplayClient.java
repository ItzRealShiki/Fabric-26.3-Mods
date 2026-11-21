package de.itzrealshiki.hotbardisplay.client;

import de.itzrealshiki.hotbardisplay.network.HotbarPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PlayerHotbarDisplayClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(HotbarPayload.TYPE, (payload, context) ->
                context.client().execute(() ->
                        HotbarCache.update(payload.entityId(), payload.items())
                )
        );
    }
}
