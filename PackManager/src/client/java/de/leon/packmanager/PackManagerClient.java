package de.leon.packmanager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Adds PackManager controls directly to Minecraft's vanilla resource-pack screen. */
public final class PackManagerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof PackSelectionScreen)) return;

            Button infoButton = Button.builder(Component.translatable("screen.packmanager.info"), button -> {
                List<PackManagerService.PackInfo> packs = PackManagerService.scanInstalledPacks(client);
                if (packs.isEmpty()) {
                    button.setMessage(Component.translatable("screen.packmanager.info_empty"));
                    return;
                }
                int next = PackManagerService.nextInfoIndex();
                PackManagerService.PackInfo pack = packs.get(next % packs.size());
                String version = pack.version() == null ? "version unknown" : "v" + pack.version();
                button.setMessage(Component.literal(pack.displayName() + " (" + version + ")"));
            }).bounds(Math.max(4, scaledWidth - 222), Math.max(4, scaledHeight - 58), 108, 20).build();

            Button updateButton = Button.builder(Component.translatable("screen.packmanager.check_updates"), button -> {
                button.setMessage(Component.translatable("screen.packmanager.checking"));
                PackManagerService.checkUpdates(client,
                        result -> button.setMessage(Component.translatable(result.translationKey(), result.count())));
            }).bounds(Math.max(4, scaledWidth - 112), Math.max(4, scaledHeight - 58), 108, 20).build();

            Screens.getWidgets(screen).add(infoButton);
            Screens.getWidgets(screen).add(updateButton);
        });
    }
}
