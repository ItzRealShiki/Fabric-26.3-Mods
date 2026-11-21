package de.itzrealshiki.hotbardisplay;

import de.itzrealshiki.hotbardisplay.network.HotbarPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PlayerHotbarDisplay implements ModInitializer {
    private static final int SYNC_INTERVAL_TICKS = 5;

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(HotbarPayload.TYPE, HotbarPayload.CODEC);
        ServerTickEvents.END_SERVER_TICK.register(this::syncHotbars);
    }

    private void syncHotbars(MinecraftServer server) {
        if (server.getTickCount() % SYNC_INTERVAL_TICKS != 0) return;

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.size() < 2) return;

        for (ServerPlayer viewer : players) {
            if (!ServerPlayNetworking.canSend(viewer, HotbarPayload.TYPE)) continue;

            for (ServerPlayer target : players) {
                if (viewer == target || viewer.level() != target.level()) continue;

                List<ItemStack> hotbar = new ArrayList<>(9);
                for (int slot = 0; slot < 9; slot++) {
                    hotbar.add(target.getInventory().getItem(slot).copy());
                }
                ServerPlayNetworking.send(viewer, new HotbarPayload(target.getId(), hotbar));
            }
        }
    }
}
