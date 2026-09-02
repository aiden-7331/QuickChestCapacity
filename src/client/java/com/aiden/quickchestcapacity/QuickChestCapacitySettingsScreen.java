package com.aiden.quickchestcapacity;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class QuickChestCapacitySettingsScreen extends Screen {
    private final QuickChestCapacityConfig config;

    public QuickChestCapacitySettingsScreen(QuickChestCapacityConfig config) {
        super(Component.literal("QuickChestCapacity Settings"));
        this.config = config;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = Math.max(38, this.height / 2 - 85);

        this.addRenderableWidget(Button.builder(sizeText(), button -> {
            config.cycleSize();
            button.setMessage(sizeText());
        }).bounds(centerX - 100, startY, 200, 20).build());

        this.addRenderableWidget(Button.builder(positionText(), button -> {
            config.cyclePosition();
            button.setMessage(positionText());
        }).bounds(centerX - 100, startY + 26, 200, 20).build());

        int moveY = startY + 78;
        this.addRenderableWidget(Button.builder(Component.literal("← Left"), button -> config.move(-5, 0))
                .bounds(centerX - 100, moveY, 64, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("↑ Up"), button -> config.move(0, -5))
                .bounds(centerX - 32, moveY, 64, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Right →"), button -> config.move(5, 0))
                .bounds(centerX + 36, moveY, 64, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("↓ Down"), button -> config.move(0, 5))
                .bounds(centerX - 32, moveY + 24, 64, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            config.reset();
            this.minecraft.gui.setScreen(new QuickChestCapacitySettingsScreen(config));
        }).bounds(centerX - 100, moveY + 54, 96, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds(centerX + 4, moveY + 54, 96, 20).build());
    }

    private Component sizeText() {
        return Component.literal("HUD Size: " + config.hudSize().displayName());
    }

    private Component positionText() {
        return Component.literal("Position: " + config.hudPosition().displayName());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String title = "QuickChestCapacity Settings";
        String moveLabel = "Fine position (5 pixels per click)";
        String hint = "Press K in game to open this menu • Key can be changed in Controls";

        graphics.text(this.font, title, (this.width - this.font.width(title)) / 2, 16, 0xFFFFC400, true);

        int startY = Math.max(38, this.height / 2 - 85);
        graphics.text(this.font, moveLabel, (this.width - this.font.width(moveLabel)) / 2, startY + 60, 0xFFFFFFFF, true);
        graphics.text(this.font, hint, (this.width - this.font.width(hint)) / 2, startY + 160, 0xFFAAAAAA, true);
    }

    @Override
    public void onClose() {
        config.save();
        this.minecraft.gui.setScreen(null);
    }
}
