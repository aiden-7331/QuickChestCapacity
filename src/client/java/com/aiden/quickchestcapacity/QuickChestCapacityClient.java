package com.aiden.quickchestcapacity;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;

public final class QuickChestCapacityClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof MenuAccess<?> menuAccess)) {
                return;
            }

            AbstractContainerMenu menu = menuAccess.getMenu();
            if (!(menu instanceof ChestMenu chestMenu)) {
                return;
            }

            ScreenEvents.afterForeground(screen).register((screen1, graphics, mouseX, mouseY, tickProgress) -> {
                int capacity = chestMenu.getRowCount() * 9;
                int usedSlots = countUsedContainerSlots(chestMenu, capacity);
                int freeSlots = capacity - usedSlots;

                String capacityText = "Chest Capacity: " + usedSlots + "/" + capacity;
                String freeText = "Free Slots: " + freeSlots;

                Minecraft minecraft = Minecraft.getInstance();

                int x = 8;
                int y = 8;

                graphics.fill(x - 4, y - 4, x + Math.max(
                        minecraft.font.width(capacityText),
                        minecraft.font.width(freeText)
                ) + 4, y + 24, 0xB0000000);

                graphics.text(minecraft.font, Component.literal(capacityText), x, y, 0xFFFFFF, true);
                graphics.text(minecraft.font, Component.literal(freeText), x, y + 11, 0xB7FFB7, true);
            });
        });
    }

    private static int countUsedContainerSlots(ChestMenu menu, int capacity) {
        int used = 0;

        for (int i = 0; i < capacity && i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.hasItem()) {
                used++;
            }
        }

        return used;
    }
}
