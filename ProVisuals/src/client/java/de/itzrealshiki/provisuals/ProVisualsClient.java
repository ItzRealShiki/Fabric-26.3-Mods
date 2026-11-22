package de.itzrealshiki.provisuals;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ProVisualsClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("provisuals", "main"));
    private static KeyMapping openMenuKey;
    private static double previousGamma = 1.0;
    private static boolean gammaCaptured;
    private static boolean skyLyricsSettingsKeyUnbound;
    private static boolean skyLyricsHudEnabled;

    @Override
    public void onInitializeClient() {
        Diagnostics.loadPreviousEntries();
        ProVisualsConfig.load();
        FriendManager.load();
        GlowOutlineRenderer.register();
        EnergyWaveRenderer.register();
        KillEffectsRenderer.register();
        // GroundReflectionRenderer is intentionally not registered: its old layer
        // mirrored every living entity, causing white ghost models over players and mobs.
        skyLyricsSettingsKeyUnbound = MusicLyricsBridge.disableSeparateSettingsKey();
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.provisuals.open_menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT, CATEGORY));

        // Run after every client mod initializer so Nuit cannot overwrite this preference during its own startup.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            migrateLegacyMenuHotkey(client);
            skyLyricsSettingsKeyUnbound = MusicLyricsBridge.disableSeparateSettingsKey();
            skyLyricsHudEnabled = MusicLyricsBridge.setNowPlayingHudEnabled(true);
            CustomSkyBridge.ensureAllWorldCoverage();
            boolean customSkyEnabled = ProVisualsConfig.isEnabled("custom_sky");
            if (CustomSkyBridge.setEnabled(customSkyEnabled)) {
                Diagnostics.info(CustomSkyBridge.statusText(customSkyEnabled));
            } else {
                Diagnostics.warn(CustomSkyBridge.statusText(customSkyEnabled));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!skyLyricsSettingsKeyUnbound) {
                skyLyricsSettingsKeyUnbound = MusicLyricsBridge.disableSeparateSettingsKey();
            }
            if (!skyLyricsHudEnabled) {
                skyLyricsHudEnabled = MusicLyricsBridge.setNowPlayingHudEnabled(true);
            }
            CustomSkyBridge.ensureAllWorldCoverage();
            FriendManager.tickPresence(client);
            KillEffectsRenderer.tickTracking(client);
            while (openMenuKey.consumeClick()) {
                if (client.player != null) client.gui.setScreen(new ProVisualsScreen());
            }
            applyFullbright(client);
        });
        Diagnostics.info("ProVisuals client initialized");
    }

    /**
     * Older ProVisuals builds used M as the menu default. Minecraft persists key mappings
     * in options.txt, so changing the constructor default alone would leave existing installs
     * bound to M. Migrate that legacy default once, while keeping any other custom binding.
     */
    private static void migrateLegacyMenuHotkey(Minecraft client) {
        if (ProVisualsConfig.hotkeyMigrationDone) return;

        InputConstants.Key legacyM = InputConstants.Type.KEYBOARD.getOrCreate(77);
        if (openMenuKey.matches(legacyM)) {
            openMenuKey.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_RSHIFT));
            try {
                var reset = KeyMapping.class.getDeclaredMethod("resetMapping");
                reset.setAccessible(true);
                reset.invoke(null);
            } catch (ReflectiveOperationException exception) {
                Diagnostics.warn("Could not refresh key mappings after the Right Shift migration: "
                        + exception.getMessage());
            }
            client.options.save();
            Diagnostics.info("Migrated ProVisuals menu hotkey from M to Right Shift");
        }

        ProVisualsConfig.hotkeyMigrationDone = true;
        ProVisualsConfig.save();
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