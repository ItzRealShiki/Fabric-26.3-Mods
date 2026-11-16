package de.leon.coremod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CoreModClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("coremod", "main"));
    private static KeyMapping openMenuKey;
    @Override public void onInitializeClient() {
        CoreModConfig.load();
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.coremod.open_menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_M, CATEGORY));
        CoreModHud.register();
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen)) return;
            int center = scaledWidth / 2, y = scaledHeight / 2 + 78, start = center - 82;
            for (int i = 0; i < 4; i++) {
                final int slot = i;
                String label = CoreModText.smallCaps(CoreModConfig.quickCommands[i].replace("/", ""));
                Screens.getWidgets(screen).add(Button.builder(Component.literal(label), b -> runQuickCommand(slot)).bounds(start + i * 41, y, 39, 18).build());
            }
            int bx = CoreModConfig.quickButtonSide.equalsIgnoreCase("LEFT") ? center - 112 : center + 88;
            Screens.getWidgets(screen).add(Button.builder(Component.literal("CORE"), b -> client.gui.setScreen(new CoreModScreen())).bounds(bx, scaledHeight / 2 + 55, 32, 18).build());
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) if (client.player != null) client.gui.setScreen(new CoreModScreen());
        });
    }
    public static void runQuickCommand(int slot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.connection == null || slot < 0 || slot >= 4) return;
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