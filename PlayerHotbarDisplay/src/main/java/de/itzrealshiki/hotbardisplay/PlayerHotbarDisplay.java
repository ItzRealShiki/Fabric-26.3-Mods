package de.itzrealshiki.hotbardisplay;

import de.itzrealshiki.hotbardisplay.network.HotbarPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class PlayerHotbarDisplay implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(HotbarPayload.TYPE, HotbarPayload.CODEC);
    }
}
