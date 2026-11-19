package com.vextex.pvpmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public final class PvPModClient implements ClientModInitializer {
    private static boolean rightShiftWasDown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(PvPModClient::handleMenuKey);
    }

    private static void handleMenuKey(MinecraftClient client) {
        long window = client.getWindow().getHandle();
        boolean rightShiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        if (rightShiftDown && !rightShiftWasDown && client.currentScreen == null) {
            client.setScreen(new PvPModScreen(false));
        }

        rightShiftWasDown = rightShiftDown;
    }
}
