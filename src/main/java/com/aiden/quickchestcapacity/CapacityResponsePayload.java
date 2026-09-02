package com.aiden.quickchestcapacity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CapacityResponsePayload(BlockPos pos, int itemCount, int maxItems, int slots) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(QuickChestCapacity.MOD_ID, "capacity_response");
    public static final Type<CapacityResponsePayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, CapacityResponsePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CapacityResponsePayload::pos,
            ByteBufCodecs.INT, CapacityResponsePayload::itemCount,
            ByteBufCodecs.INT, CapacityResponsePayload::maxItems,
            ByteBufCodecs.INT, CapacityResponsePayload::slots,
            CapacityResponsePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
