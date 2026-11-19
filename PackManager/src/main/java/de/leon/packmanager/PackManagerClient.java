package de.leon.packmanager;

import net.fabricmc.api.ClientModInitializer;

/**
 * PackManager extends the vanilla resource pack screen.
 * It intentionally does not register a separate key binding or standalone screen.
 */
public final class PackManagerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Vanilla resource pack screen integration is initialized here.
    }
}
