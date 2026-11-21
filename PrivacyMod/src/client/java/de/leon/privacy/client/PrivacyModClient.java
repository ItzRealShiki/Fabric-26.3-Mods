package de.leon.privacy.client;

import de.leon.privacy.api.PrivacyEntry;
import de.leon.privacy.api.PrivacyManager;
import de.leon.privacy.api.PrivacyVisibility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class PrivacyModClient implements ClientModInitializer {
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("privacymod", "main")
    );
    private static KeyMapping openLibraryKey;

    @Override
    public void onInitializeClient() {
        PrivacyManager.registerProvider(() -> List.of(
            new PrivacyEntry("network.ip", "Connection IP",
                "The server endpoint receives the public source IP used by this connection. A client mod cannot hide it.",
                PrivacyVisibility.EXPOSED),
            new PrivacyEntry("client.version", "Client / protocol version",
                "The server can identify or infer protocol information. This mod does not spoof it.",
                PrivacyVisibility.EXPOSED),
            new PrivacyEntry("mods.handshake", "Installed mods",
                "Only mod information actually transmitted or otherwise exposed is available to the server; this is not a complete local mod-list audit.",
                PrivacyVisibility.PARTIALLY_EXPOSED),
            new PrivacyEntry("hardware", "Hardware details",
                "Ordinary Minecraft server connections do not provide a complete hardware inventory.",
                PrivacyVisibility.PRIVATE),
            new PrivacyEntry("local.settings", "Local settings",
                "Local options are not automatically sent to a Minecraft server.",
                PrivacyVisibility.PRIVATE)
        ));

        openLibraryKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.privacymod.open_library",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            KEY_CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openLibraryKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new PrivacyScreen());
                } else if (client.screen instanceof PrivacyScreen) {
                    client.setScreen(null);
                }
            }
        });
    }
}
