package com.aiden.quickchestcapacity;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class QuickChestCapacityClient implements ClientModInitializer {
    private static final String MOD_ID = "quickchestcapacity";
    private static final Map<BlockPos, CapacityInfo> KNOWN_CHESTS = new HashMap<>();

    private static BlockPos openChestPos;
    private static BlockPos lookedAtChestPos;
    private static CapacityInfo lookedAtInfo;

    @Override
    public void onInitializeClient() {
        // Learn/update a chest while its inventory screen is open.
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof MenuAccess<?> menuAccess)) {
                return;
            }

            AbstractContainerMenu menu = menuAccess.getMenu();
            if (!(menu instanceof ChestMenu chestMenu)) {
                return;
            }

            openChestPos = getLookedAtContainerPos(client);
            if (openChestPos == null) {
                return;
            }

            ScreenEvents.afterForeground(screen).register((screen1, graphics, mouseX, mouseY, tickProgress) -> {
                int slots = chestMenu.getRowCount() * 9;
                int maxItems = slots * 64;
                int itemCount = countItems(chestMenu, slots);
                CapacityInfo info = new CapacityInfo(itemCount, maxItems, slots);
                rememberContainer(client, openChestPos, info);
            });
        });

        // Keep track of the chest the crosshair is currently pointing at.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            lookedAtChestPos = null;
            lookedAtInfo = null;

            if (client.player == null || client.level == null || client.screen != null) {
                return;
            }

            BlockPos lookedAt = getLookedAtContainerPos(client);
            if (lookedAt == null) {
                return;
            }

            lookedAtChestPos = lookedAt;
            lookedAtInfo = KNOWN_CHESTS.get(lookedAt);
        });

        // Image-style capacity card shown while looking at a chest.
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "capacity_display"),
                QuickChestCapacityClient::renderCapacityHud
        );
    }

    private static void renderCapacityHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || lookedAtChestPos == null || minecraft.screen != null) {
            return;
        }

        int panelWidth = 210;
        int panelHeight = 58;
        int x = (graphics.guiWidth() - panelWidth) / 2;
        int y = graphics.guiHeight() - 118;

        // Dark, Minecraft-like plate inspired by the reference image.
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xD912120F);
        graphics.outline(x, y, panelWidth, panelHeight, 0xFFF0C14B);
        graphics.outline(x + 2, y + 2, panelWidth - 4, panelHeight - 4, 0xFF5C421B);

        if (lookedAtInfo == null) {
            String title = "CHEST INDICATOR";
            String hint = "OPEN THIS CHEST ONCE TO SCAN";
            int tx = x + (panelWidth - minecraft.font.width(title)) / 2;
            int hx = x + (panelWidth - minecraft.font.width(hint)) / 2;
            graphics.text(minecraft.font, title, tx, y + 12, 0xFFFFC400, true);
            graphics.text(minecraft.font, hint, hx, y + 34, 0xFFFFFFFF, true);
            return;
        }

        CapacityInfo info = lookedAtInfo;
        int percent = info.maxItems == 0 ? 0 : Math.min(100, Math.round((info.itemCount * 100.0f) / info.maxItems));
        String chestType = info.slots == 54 ? "DOUBLE CHEST" : "SINGLE CHEST";
        String amount = info.itemCount + " / " + info.maxItems;
        String status = statusText(percent);
        int statusColor = statusColor(percent);

        int chestTypeX = x + 10;
        int amountX = x + panelWidth - 10 - minecraft.font.width(amount);
        graphics.text(minecraft.font, chestType, chestTypeX, y + 8, 0xFFFFC400, true);
        graphics.text(minecraft.font, amount, amountX, y + 8, 0xFFFFFFFF, true);

        // Segmented capacity bar like the image.
        int barX = x + 10;
        int barY = y + 25;
        int barWidth = panelWidth - 20;
        int barHeight = 13;
        int segments = 20;
        int gap = 2;
        int segmentWidth = (barWidth - gap * (segments - 1)) / segments;
        int filled = Math.round(percent / 100.0f * segments);

        graphics.fill(barX - 2, barY - 2, barX + barWidth + 2, barY + barHeight + 2, 0xFF080808);

        for (int i = 0; i < segments; i++) {
            int sx = barX + i * (segmentWidth + gap);
            int color;
            if (i >= filled) {
                color = 0xFF242424;
            } else {
                float point = (i + 1) / (float) segments;
                if (point <= 0.40f) {
                    color = 0xFF36D10B; // green
                } else if (point <= 0.70f) {
                    color = 0xFFFFC400; // yellow
                } else if (point <= 0.90f) {
                    color = 0xFFFF7A00; // orange
                } else {
                    color = 0xFFE02B2B; // red
                }
            }
            graphics.fill(sx, barY, sx + segmentWidth, barY + barHeight, color);
        }

        String percentText = percent + "%";
        graphics.text(minecraft.font, status, x + 10, y + 44, statusColor, true);
        graphics.text(minecraft.font, percentText, x + panelWidth - 10 - minecraft.font.width(percentText), y + 44, statusColor, true);
    }

    private static String statusText(int percent) {
        if (percent == 0) return "EMPTY";
        if (percent <= 70) return "PARTIALLY FULL";
        if (percent < 100) return "NEARLY FULL";
        return "FULL";
    }

    private static int statusColor(int percent) {
        if (percent == 0) return 0xFF46E01C;
        if (percent <= 70) return 0xFFFFC400;
        if (percent < 100) return 0xFFE33A3A;
        return 0xFFAAAAAA;
    }

    private static void rememberContainer(Minecraft client, BlockPos pos, CapacityInfo info) {
        KNOWN_CHESTS.put(pos.immutable(), info);

        // Double chest = 54 slots. Save the same reading to the connected half,
        // so pointing at either block shows the same total.
        if (info.slots != 54 || client.level == null) {
            return;
        }

        BlockState original = client.level.getBlockState(pos);
        BlockPos[] neighbours = { pos.north(), pos.south(), pos.east(), pos.west() };

        for (BlockPos neighbour : neighbours) {
            BlockState neighbourState = client.level.getBlockState(neighbour);
            boolean sameChestType =
                    (original.is(Blocks.CHEST) && neighbourState.is(Blocks.CHEST)) ||
                    (original.is(Blocks.TRAPPED_CHEST) && neighbourState.is(Blocks.TRAPPED_CHEST));

            if (sameChestType) {
                KNOWN_CHESTS.put(neighbour.immutable(), info);
            }
        }
    }

    private static BlockPos getLookedAtContainerPos(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit) || client.level == null) {
            return null;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = client.level.getBlockState(pos);

        if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL)) {
            return pos;
        }

        return null;
    }

    private static int countItems(ChestMenu menu, int containerSlots) {
        int items = 0;

        for (int i = 0; i < containerSlots && i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.hasItem()) {
                items += slot.getItem().getCount();
            }
        }

        return items;
    }

    private record CapacityInfo(int itemCount, int maxItems, int slots) {
    }
}
