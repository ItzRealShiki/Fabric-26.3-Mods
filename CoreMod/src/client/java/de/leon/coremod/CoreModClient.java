package de.leon.coremod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CoreModClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("coremod", "main"));
    private static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        CoreModConfig.load();
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.coremod.open_menu", InputConstants.Type.KEYSYM, InputConstants.KEY_RIGHT_SHIFT, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.player != null) {
                    client.gui.setScreen(new CoreModScreen());
                }
            }
        });
    }

    public static void runQuickCommand(int slot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.connection == null) return;
        String command = CoreModConfig.quickCommands[slot].trim();
        if (command.isEmpty()) return;
        if (command.startsWith("/")) command = command.substring(1);
        client.player.connection.sendCommand(command);
    }

    public static void message(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) client.player.sendSystemMessage(Component.literal(CoreModText.smallCaps(text)));
    }
}
