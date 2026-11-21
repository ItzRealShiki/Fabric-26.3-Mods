package de.itzrealshiki.hotbardisplay.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class PlayerHotbarDisplayClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("player_hotbar_display", "main")
    );
    private static KeyMapping toggleOwnHotbarKey;
    private static boolean ownHotbarVisible;

    public static boolean isOwnHotbarVisible() {
        return ownHotbarVisible;
    }

    @Override
    public void onInitializeClient() {
        toggleOwnHotbarKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.player_hotbar_display.toggle_own_hotbar",
                InputConstants.Type.KEYBOARD,
                GLFW.GLFW_KEY_LEFT_CONTROL,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleOwnHotbarKey.consumeClick()) {
                ownHotbarVisible = !ownHotbarVisible;
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(HotbarPayload.TYPE, (payload, context) ->
                HotbarCache.update(payload.entityId(), payload.items())
        );
        PlayerHotbarRenderRegistration.register();
    }
}
