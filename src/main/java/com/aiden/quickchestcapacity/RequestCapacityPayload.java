package com.aiden.quickchestcapacity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestCapacityPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(QuickChestCapacity.MOD_ID, "request_capacity");
    public static final Type<RequestCapacityPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestCapacityPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RequestCapacityPayload::pos,
            RequestCapacityPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
