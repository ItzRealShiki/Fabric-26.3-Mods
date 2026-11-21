package de.itzrealshiki.hotbardisplay.client;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HotbarCache {
    private static final Map<Integer, List<ItemStack>> HOTBARS = new ConcurrentHashMap<>();

    private HotbarCache() {}

    public static void update(int entityId, List<ItemStack> items) {
        HOTBARS.put(entityId, items.stream().map(ItemStack::copy).toList());
    }

    public static List<ItemStack> get(int entityId) {
        return HOTBARS.getOrDefault(entityId, List.of());
    }

    public static void remove(int entityId) {
        HOTBARS.remove(entityId);
    }

    public static void clear() {
        HOTBARS.clear();
    }
}
