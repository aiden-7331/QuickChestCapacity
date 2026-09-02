package com.aiden.quickchestcapacity;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class QuickChestCapacityClient implements ClientModInitializer {
    private static final Map<BlockPos, CapacityInfo> KNOWN_CHESTS = new HashMap<>();

    private static BlockPos openChestPos;
    private static ChestDisplayState lookedAtDisplay;

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

            openChestPos = getLookedAtChestPos(client);
            if (openChestPos == null) {
                return;
            }

            ScreenEvents.afterForeground(screen).register((screen1, graphics, mouseX, mouseY, tickProgress) -> {
                int slots = chestMenu.getRowCount() * 9;
                int maxItems = slots * 64;
                int itemCount = countItems(chestMenu, slots);
                CapacityInfo info = new CapacityInfo(itemCount, maxItems, slots);
                rememberChest(client, openChestPos, info);
            });
        });

        // Work out which chest the crosshair is pointing at and prepare only the
        // simple data needed by the render callback.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            lookedAtDisplay = null;

            if (client.player == null || client.level == null || client.gui.screen() != null) {
                return;
            }

            BlockPos lookedAt = getLookedAtChestPos(client);
            if (lookedAt == null) {
                return;
            }

            lookedAtDisplay = createDisplayState(client, lookedAt, KNOWN_CHESTS.get(lookedAt));
        });

        // Draw the indicator in the 3D world, physically attached to the front
        // of the chest instead of drawing a HUD panel on the player's screen.
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            ChestDisplayState display = lookedAtDisplay;
            if (display == null) {
                return;
            }

            renderChestIndicator(
                    context.submitNodeCollector(),
                    context.levelState().cameraRenderState,
                    display
            );
        });
    }

    private static ChestDisplayState createDisplayState(Minecraft client, BlockPos pos, CapacityInfo info) {
        if (client.level == null) {
            return null;
        }

        BlockState state = client.level.getBlockState(pos);
        if (!isSupportedChest(state)) {
            return null;
        }

        Direction facing = state.getValue(ChestBlock.FACING);
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        boolean doubleChest = info != null && info.slots == 54;

        // Centre a 54-slot display across both halves of the double chest.
        if (doubleChest && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos partner = pos.relative(ChestBlock.getConnectedDirection(state));
            centerX = (centerX + partner.getX() + 0.5) / 2.0;
            centerZ = (centerZ + partner.getZ() + 0.5) / 2.0;
        }

        return new ChestDisplayState(centerX, centerY, centerZ, facing, doubleChest, info);
    }

    private static void renderChestIndicator(
            net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector,
            net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState,
            ChestDisplayState display
    ) {
        DrawableGizmoPrimitives primitives = new DrawableGizmoPrimitives();

        Vec3 normal = directionVector(display.facing);
        Vec3 right = new Vec3(-normal.z, 0.0, normal.x);
        Vec3 up = new Vec3(0.0, 1.0, 0.0);

        // A chest model sits slightly inside its full block. This puts the plate
        // just in front of the visible chest face so it looks mounted to it.
        Vec3 baseCenter = new Vec3(display.centerX, display.centerY - 0.04, display.centerZ)
                .add(normal.scale(0.452));

        double plateWidth = display.doubleChest ? 1.64 : 0.80;
        double plateHeight = 0.25;

        // Gold outer frame, dark inner plate: same visual idea as the original HUD.
        addRect(primitives, baseCenter, right, up, plateWidth, plateHeight, 0xFFF0C14B);
        addRect(primitives, baseCenter.add(normal.scale(0.0015)), right, up,
                plateWidth - 0.035, plateHeight - 0.035, 0xFF17140E);

        if (display.info == null) {
            primitives.addText(
                    baseCenter.add(normal.scale(0.012)).add(up.scale(0.005)),
                    "OPEN ONCE TO SCAN",
                    TextGizmo.Style.forColorAndCentered(0xFFFFC400)
            );
            primitives.submit(submitNodeCollector, cameraRenderState, false);
            return;
        }

        CapacityInfo info = display.info;
        int percent = info.maxItems == 0
                ? 0
                : Math.min(100, Math.round((info.itemCount * 100.0f) / info.maxItems));

        // Count text sits directly above the bar on the chest.
        String countText = info.itemCount + " / " + info.maxItems;
        primitives.addText(
                baseCenter.add(normal.scale(0.012)).add(up.scale(0.075)),
                countText,
                TextGizmo.Style.whiteAndCentered()
        );

        // Segmented bar, mounted directly on the chest face.
        int segments = 20;
        int filled = Math.round(percent / 100.0f * segments);
        double barWidth = plateWidth - 0.10;
        double barHeight = 0.075;
        double gap = display.doubleChest ? 0.014 : 0.008;
        double segmentWidth = (barWidth - gap * (segments - 1)) / segments;
        double leftEdge = -barWidth / 2.0;
        Vec3 barCenter = baseCenter.add(normal.scale(0.004)).add(up.scale(-0.045));

        // Dark trough behind all segments.
        addRect(primitives, barCenter, right, up, barWidth + 0.025, barHeight + 0.025, 0xFF070707);

        for (int i = 0; i < segments; i++) {
            double segmentCenterOffset = leftEdge + segmentWidth / 2.0 + i * (segmentWidth + gap);
            Vec3 segmentCenter = barCenter
                    .add(right.scale(segmentCenterOffset))
                    .add(normal.scale(0.002));

            int color = i < filled ? gradientColor(i, segments) : 0xFF242424;
            addRect(primitives, segmentCenter, right, up, segmentWidth, barHeight, color);
        }

        String status = statusText(percent) + "  " + percent + "%";
        primitives.addText(
                baseCenter.add(normal.scale(0.012)).add(up.scale(-0.105)),
                status,
                TextGizmo.Style.forColorAndCentered(statusColor(percent))
        );

        // false = normal depth testing, so the plate behaves like something in the world
        // rather than showing through walls like a screen overlay.
        primitives.submit(submitNodeCollector, cameraRenderState, false);
    }

    private static void addRect(
            DrawableGizmoPrimitives primitives,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            double width,
            double height,
            int color
    ) {
        Vec3 halfRight = right.scale(width / 2.0);
        Vec3 halfUp = up.scale(height / 2.0);

        Vec3 bottomLeft = center.subtract(halfRight).subtract(halfUp);
        Vec3 bottomRight = center.add(halfRight).subtract(halfUp);
        Vec3 topRight = center.add(halfRight).add(halfUp);
        Vec3 topLeft = center.subtract(halfRight).add(halfUp);

        primitives.addQuad(bottomLeft, bottomRight, topRight, topLeft, color);
    }

    private static Vec3 directionVector(Direction direction) {
        return switch (direction) {
            case NORTH -> new Vec3(0.0, 0.0, -1.0);
            case SOUTH -> new Vec3(0.0, 0.0, 1.0);
            case WEST -> new Vec3(-1.0, 0.0, 0.0);
            case EAST -> new Vec3(1.0, 0.0, 0.0);
            default -> new Vec3(0.0, 0.0, 1.0);
        };
    }

    private static int gradientColor(int segment, int totalSegments) {
        float point = (segment + 1) / (float) totalSegments;
        if (point <= 0.40f) {
            return 0xFF36D10B; // green
        }
        if (point <= 0.70f) {
            return 0xFFFFC400; // yellow
        }
        if (point <= 0.90f) {
            return 0xFFFF7A00; // orange
        }
        return 0xFFE02B2B; // red
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
        return 0xFFDDDDDD;
    }

    private static void rememberChest(Minecraft client, BlockPos pos, CapacityInfo info) {
        KNOWN_CHESTS.put(pos.immutable(), info);

        // Save a 54-slot reading to the exact connected half as well, so looking
        // at either side of the double chest gives the same total.
        if (info.slots != 54 || client.level == null) {
            return;
        }

        BlockState state = client.level.getBlockState(pos);
        if (!isSupportedChest(state) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return;
        }

        BlockPos partner = pos.relative(ChestBlock.getConnectedDirection(state));
        KNOWN_CHESTS.put(partner.immutable(), info);
    }

    private static BlockPos getLookedAtChestPos(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit) || client.level == null) {
            return null;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = client.level.getBlockState(pos);
        return isSupportedChest(state) ? pos : null;
    }

    private static boolean isSupportedChest(BlockState state) {
        return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST);
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

    private record ChestDisplayState(
            double centerX,
            double centerY,
            double centerZ,
            Direction facing,
            boolean doubleChest,
            CapacityInfo info
    ) {
    }
}
