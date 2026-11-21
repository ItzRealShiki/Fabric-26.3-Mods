package de.leon.privacy.api;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public integration point for other client mods.
 *
 * Example:
 * PrivacyManager.registerProvider(() -> List.of(
 *     new PrivacyEntry("example.setting", "Example setting",
 *         "This mod keeps this setting local.", PrivacyVisibility.PRIVATE)
 * ));
 */
public final class PrivacyManager {
    private static final List<PrivacyProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static volatile HudController HUD_CONTROLLER;

    /** Optional controls exposed by HUD integrations to the Privacy Library UI. */
    public interface HudController {
        boolean isEnabled();
        void setEnabled(boolean enabled);
        int getSizePercent();
        void setSizePercent(int percent);
    }

    public static void registerHudController(HudController controller) {
        HUD_CONTROLLER = controller;
    }

    public static HudController getHudController() {
        return HUD_CONTROLLER;
    }

    private PrivacyManager() {}

    public static void registerProvider(PrivacyProvider provider) {
        if (provider != null) {
            PROVIDERS.add(provider);
        }
    }

    public static void unregisterProvider(PrivacyProvider provider) {
        PROVIDERS.remove(provider);
    }

    public static List<PrivacyEntry> getEntries() {
        return PROVIDERS.stream()
                .map(PrivacyProvider::getEntries)
                .flatMap(Collection::stream)
                .toList();
    }
}
