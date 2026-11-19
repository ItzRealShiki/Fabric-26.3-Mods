package com.vextex.pvpmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class PvPModClient implements ClientModInitializer {
    private static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pvpmod.open_menu",
                InputConstants.KEY_RSHIFT,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(PvPModClient::handleMenuKey);
    }

    private static void handleMenuKey(Minecraft client) {
        while (menuKey.consumeClick()) {
            if (client.gui.screen() == null) {
                client.gui.setScreen(new PvPModScreen(false));
            }
        }
    }
}
