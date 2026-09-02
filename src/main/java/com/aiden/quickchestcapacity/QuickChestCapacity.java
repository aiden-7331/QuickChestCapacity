package com.aiden.quickchestcapacity;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class QuickChestCapacity implements ModInitializer {
    public static final String MOD_ID = "quickchestcapacity";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(RequestCapacityPayload.TYPE, RequestCapacityPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CapacityResponsePayload.TYPE, CapacityResponsePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestCapacityPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ServerLevel level = player.level();
            BlockPos pos = payload.pos();

            // Only answer nearby requests for loaded blocks.
            double dx = player.getX() - (pos.getX() + 0.5D);
            double dy = player.getY() - (pos.getY() + 0.5D);
            double dz = player.getZ() - (pos.getZ() + 0.5D);
            if ((dx * dx + dy * dy + dz * dz) > 100.0D || !level.hasChunkAt(pos)) {
                return;
            }

            CapacitySnapshot snapshot = readCapacity(level, pos);
            if (snapshot == null) {
                return;
            }

            ServerPlayNetworking.send(player, new CapacityResponsePayload(
                    pos.immutable(),
                    snapshot.itemCount(),
                    snapshot.maxItems(),
                    snapshot.slots()
            ));
        });
    }

    private static CapacitySnapshot readCapacity(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Container container = null;

        // ChestBlock#getContainer merges both halves of a double chest into one 54-slot container.
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            container = ChestBlock.getContainer(chestBlock, state, level, pos, true);
        }

        // Also keeps the v1.3 barrel support.
        if (container == null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container blockContainer) {
                container = blockContainer;
            }
        }

        if (container == null) {
            return null;
        }

        int slots = container.getContainerSize();
        int itemCount = 0;

        for (int i = 0; i < slots; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                itemCount += stack.getCount();
            }
        }

        return new CapacitySnapshot(itemCount, slots * 64, slots);
    }

    private record CapacitySnapshot(int itemCount, int maxItems, int slots) {
    }
}
