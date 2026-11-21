package de.leon.privacy.client;

import de.leon.privacy.api.PrivacyEntry;
import de.leon.privacy.api.PrivacyManager;
import de.leon.privacy.api.PrivacyVisibility;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import java.util.List;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public final class PrivacyModClient implements ClientModInitializer {
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

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("privacy").executes(context -> {
                Minecraft.getInstance().gui.setScreen(new PrivacyScreen());
                return 1;
            }))
        );
    }
}
