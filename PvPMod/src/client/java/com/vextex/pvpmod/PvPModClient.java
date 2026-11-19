package com.vextex.pvpmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;

public final class PvPModClient implements ClientModInitializer {
    private static KeyMapping menuKey;
    private static Boolean lastParticlesState;
    private static Boolean lastFullbrightState;
    private static Double originalGamma;

    @Override
    public void onInitializeClient() {
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pvpmod.open_menu",
                InputConstants.KEY_RSHIFT,
                KeyMapping.Category.MISC
        ));

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("pvpmod", "feature_hud"),
                PvPHud::render
        );

        ClientTickEvents.END_CLIENT_TICK.register(PvPModClient::handleMenuKey);
        ClientTickEvents.END_CLIENT_TICK.register(PvPModClient::applyVisualOptions);
    }

    private static void handleMenuKey(Minecraft client) {
        while (menuKey.consumeClick()) {
            if (client.gui.screen() == null) {
                client.gui.setScreen(new PvPModScreen(false));
            }
        }
    }

    private static void applyVisualOptions(Minecraft client) {
        if (client.options == null) return;

        boolean particlesEnabled = PvPModConfig.isEnabled(PvPFeature.PARTICLES);
        if (lastParticlesState == null || lastParticlesState != particlesEnabled) {
            client.options.particles().set(particlesEnabled ? ParticleStatus.ALL : ParticleStatus.MINIMAL);
            lastParticlesState = particlesEnabled;
        }

        boolean fullbrightEnabled = PvPModConfig.isEnabled(PvPFeature.FULLBRIGHT);
        if (lastFullbrightState == null) {
            lastFullbrightState = fullbrightEnabled;
            if (fullbrightEnabled) {
                originalGamma = client.options.gamma().get();
                client.options.gamma().set(16.0);
                client.options.save();
            }
        } else if (lastFullbrightState != fullbrightEnabled) {
            if (fullbrightEnabled) {
                originalGamma = client.options.gamma().get();
                client.options.gamma().set(16.0);
            } else {
                client.options.gamma().set(originalGamma == null ? 1.0 : originalGamma);
                originalGamma = null;
            }
            lastFullbrightState = fullbrightEnabled;
            client.options.save();
        }
    }
}
