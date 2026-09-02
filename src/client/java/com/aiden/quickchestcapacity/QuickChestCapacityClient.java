package com.aiden.quickchestcapacity;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
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
    private static final QuickChestCapacityConfig CONFIG = QuickChestCapacityConfig.load();
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "settings")
    );
    private static final KeyMapping OPEN_SETTINGS_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.quickchestcapacity.open_settings",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_K,
                    KEY_CATEGORY
            )
    );
    private static final Map<BlockPos, CapacityInfo> KNOWN_CHESTS = new HashMap<>();
    private static final int LIVE_REFRESH_TICKS = 4; // 5 updates per second at 20 TPS.

    private static BlockPos openChestPos;
    private static BlockPos lookedAtChestPos;
    private static CapacityInfo lookedAtInfo;
    private static int liveRefreshCounter;

    @Override
    public void onInitializeClient() {
        // Receive authoritative live values from the logical server.
        ClientPlayNetworking.registerGlobalReceiver(CapacityResponsePayload.TYPE, (payload, context) -> {
            CapacityInfo info = new CapacityInfo(payload.itemCount(), payload.maxItems(), payload.slots());
            rememberContainer(context.client(), payload.pos(), info);

            if (lookedAtChestPos != null && lookedAtChestPos.equals(payload.pos())) {
                lookedAtInfo = info;
            }
        });

        // Fallback: learn/update a chest while its inventory screen is open.
        // This keeps v1.3 useful even on multiplayer servers that do not have the mod installed.
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

        // Track the chest under the crosshair and request a fresh value while looking at it.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_SETTINGS_KEY.consumeClick()) {
                if (client.gui.screen() instanceof QuickChestCapacitySettingsScreen) {
                    client.gui.setScreen(null);
                } else if (client.gui.screen() == null) {
                    client.gui.setScreen(new QuickChestCapacitySettingsScreen(CONFIG));
                }
            }

            lookedAtChestPos = null;
            lookedAtInfo = null;

            if (client.player == null || client.level == null || client.gui.screen() != null) {
                liveRefreshCounter = 0;
                return;
            }

            BlockPos lookedAt = getLookedAtContainerPos(client);
            if (lookedAt == null) {
                liveRefreshCounter = 0;
                return;
            }

            lookedAtChestPos = lookedAt;
            lookedAtInfo = KNOWN_CHESTS.get(lookedAt);

            // If the server has QuickChestCapacity, request the real current inventory.
            // This catches hopper insertions/removals without opening the chest.
            liveRefreshCounter++;
            if (liveRefreshCounter >= LIVE_REFRESH_TICKS) {
                liveRefreshCounter = 0;
                if (ClientPlayNetworking.canSend(RequestCapacityPayload.TYPE)) {
                    ClientPlayNetworking.send(new RequestCapacityPayload(lookedAt.immutable()));
                }
            }
        });

        // Keep the original v1.3 bottom-centre capacity card.
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "capacity_display"),
                QuickChestCapacityClient::renderCapacityHud
        );
    }

    private static void renderCapacityHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean settingsOpen = minecraft.gui.screen() instanceof QuickChestCapacitySettingsScreen;
        boolean editingHud = settingsOpen
                && ((QuickChestCapacitySettingsScreen) minecraft.gui.screen()).isEditingHud();

        if (minecraft.player == null) {
            return;
        }
        if (!settingsOpen && (lookedAtChestPos == null || minecraft.gui.screen() != null)) {
            return;
        }

        float scale = CONFIG.scale();
        int scaledPanelWidth = CONFIG.panelWidth();
        int scaledPanelHeight = CONFIG.panelHeight();
        int screenX = CONFIG.calculateX(graphics.guiWidth(), scaledPanelWidth);
        int screenY = CONFIG.calculateY(graphics.guiHeight(), scaledPanelHeight);

        // Render one normal-sized 194x54 HUD, then scale the entire thing as a unit.
        // Text, numbers, bar, spacing and borders all shrink together, so even 1%
        // cannot cause the amount text to overlap the capacity bar.
        graphics.pose().pushMatrix();
        graphics.pose().translate(screenX, screenY);
        graphics.pose().scale(scale, scale);

        int panelWidth = QuickChestCapacityConfig.BASE_PANEL_WIDTH;
        int panelHeight = QuickChestCapacityConfig.BASE_PANEL_HEIGHT;
        int x = 0;
        int y = 0;

        // Use a fully opaque panel while moving/resizing so the preview stays crisp and
        // easy to place against any world background. Normal gameplay keeps the original
        // slightly transparent v1.3 look.
        int panelBackground = editingHud ? 0xFF12120F : 0xD912120F;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, panelBackground);
        graphics.outline(x, y, panelWidth, panelHeight, 0xFFF0C14B);
        graphics.outline(x + 2, y + 2, panelWidth - 4, panelHeight - 4, 0xFF5C421B);

        CapacityInfo displayInfo = settingsOpen
                ? new CapacityInfo(1728, 3456, 54)
                : lookedAtInfo;

        if (displayInfo == null) {
            String title = "CHEST INDICATOR";
            String hint = ClientPlayNetworking.canSend(RequestCapacityPayload.TYPE)
                    ? "READING CHEST..."
                    : "OPEN THIS CHEST ONCE TO SCAN";
            int tx = x + (panelWidth - minecraft.font.width(title)) / 2;
            int hx = x + (panelWidth - minecraft.font.width(hint)) / 2;
            graphics.text(minecraft.font, title, tx, y + 10, 0xFFFFC400, true);
            graphics.text(minecraft.font, hint, hx, y + 30, 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
            return;
        }

        CapacityInfo info = displayInfo;
        int percent = info.maxItems == 0 ? 0 : Math.min(100, Math.round((info.itemCount * 100.0f) / info.maxItems));
        String chestType = info.slots == 54 ? "DOUBLE CHEST" : "SINGLE CHEST";
        String amount = info.itemCount + " / " + info.maxItems;
        String status = statusText(percent);
        int statusColor = statusColor(percent);

        int sidePadding = 8;
        int chestTypeX = x + sidePadding;
        int amountX = x + panelWidth - sidePadding - minecraft.font.width(amount);
        graphics.text(minecraft.font, chestType, chestTypeX, y + 7, 0xFFFFC400, true);
        graphics.text(minecraft.font, amount, amountX, y + 7, 0xFFFFFFFF, true);

        int barWidth = panelWidth - 40;
        int barX = x + (panelWidth - barWidth) / 2;
        int barY = y + 24;
        int barHeight = QuickChestCapacityConfig.BASE_BAR_HEIGHT;
        int segments = 20;
        int gap = 2;
        int usableSegmentWidth = barWidth - gap * (segments - 1);
        int filled = Math.round(percent / 100.0f * segments);

        graphics.fill(barX - 2, barY - 2, barX + barWidth + 2, barY + barHeight + 2, 0xFF080808);

        for (int i = 0; i < segments; i++) {
            int sx = barX + i * gap + (i * usableSegmentWidth) / segments;
            int ex = barX + i * gap + ((i + 1) * usableSegmentWidth) / segments;
            int color;
            if (i >= filled) {
                color = 0xFF242424;
            } else {
                float point = (i + 1) / (float) segments;
                if (point <= 0.40f) {
                    color = 0xFF36D10B;
                } else if (point <= 0.70f) {
                    color = 0xFFFFC400;
                } else if (point <= 0.90f) {
                    color = 0xFFFF7A00;
                } else {
                    color = 0xFFE02B2B;
                }
            }
            graphics.fill(sx, barY, ex, barY + barHeight, color);
        }

        String percentText = percent + "%";
        graphics.text(minecraft.font, status, x + sidePadding, y + 40, statusColor, true);
        graphics.text(minecraft.font, percentText, x + panelWidth - sidePadding - minecraft.font.width(percentText), y + 40, statusColor, true);

        graphics.pose().popMatrix();
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
