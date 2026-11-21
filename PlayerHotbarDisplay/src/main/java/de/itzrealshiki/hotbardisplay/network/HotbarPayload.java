package de.itzrealshiki.hotbardisplay.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record HotbarPayload(int entityId, List<ItemStack> items) implements CustomPacketPayload {
    public static final Type<HotbarPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("player_hotbar_display", "hotbar"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HotbarPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, HotbarPayload::entityId,
                    ByteBufCodecs.collection(ArrayList::new, ItemStack.STREAM_CODEC), HotbarPayload::items,
                    HotbarPayload::new
            );

    public HotbarPayload {
        items = List.copyOf(items);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
