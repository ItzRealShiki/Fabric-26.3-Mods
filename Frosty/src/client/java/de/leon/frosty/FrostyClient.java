package de.leon.frosty;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class FrostyClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("frosty", "main"));
    private static KeyMapping openMenuKey;

    @Override public void onInitializeClient() {
        FrostyConfig.load();
        FrostyLog.install();
        FrostyHud.register();
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.frosty.open_menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT, CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.player != null) client.gui.setScreen(new FrostyScreen());
            }
        });
    }
}
