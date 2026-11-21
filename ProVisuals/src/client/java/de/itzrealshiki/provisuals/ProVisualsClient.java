package de.itzrealshiki.provisuals;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ProVisualsClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("provisuals", "main"));
    private static KeyMapping openMenuKey;
    private static double previousGamma = 1.0;
    private static boolean gammaCaptured;

    @Override
    public void onInitializeClient() {
        Diagnostics.loadPreviousEntries();
        ProVisualsConfig.load();
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.provisuals.open_menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_M, CATEGORY));

        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("provisuals", "music_island"), new MusicIslandHud());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.player != null) client.gui.setScreen(new ProVisualsScreen());
            }
            applyFullbright(client);
        });
        Diagnostics.info("ProVisuals client initialized");
    }

    private static void applyFullbright(Minecraft client) {
        if (client.options == null) return;
        if (ProVisualsConfig.isEnabled("fullbright")) {
            if (!gammaCaptured) { previousGamma = client.options.gamma().get(); gammaCaptured = true; }
            if (client.options.gamma().get() < 15.9) client.options.gamma().set(16.0);
        } else if (gammaCaptured) {
            client.options.gamma().set(previousGamma);
            gammaCaptured = false;
        }
    }
}
