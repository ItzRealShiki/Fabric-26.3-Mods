package de.leon.packmanager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class PackManagerClient implements ClientModInitializer {
    private static KeyBinding openManagerKey;

    @Override
    public void onInitializeClient() {
        openManagerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.packmanager.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openManagerKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new PackManagerScreen(null));
                }
            }
        });
    }
}
