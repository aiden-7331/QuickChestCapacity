package com.aiden.quickchestcapacity;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class QuickChestCapacitySettingsScreen extends Screen {
    private static final int RESIZE_HANDLE_SIZE = 10;

    private final QuickChestCapacityConfig config;

    private boolean draggingHud;
    private boolean resizingHud;
    private double dragGrabX;
    private double dragGrabY;
    private int resizeFixedX;
    private int resizeFixedY;

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
        String text = "HUD Size: " + config.hudSize().displayName();
        if (config.hasCustomScale()) {
            text += " • Custom " + config.scalePercent() + "%";
        }
        return Component.literal(text);
    }

    private Component positionText() {
        return Component.literal("Position: " + config.hudPosition().displayName());
    }

    /** Returns true when the cursor is inside the live HUD preview. */
    private boolean isOverHud(double mouseX, double mouseY) {
        int panelWidth = config.panelWidth();
        int panelHeight = config.panelHeight();
        int hudX = config.calculateX(this.width, panelWidth);
        int hudY = config.calculateY(this.height, panelHeight);
        return mouseX >= hudX && mouseX < hudX + panelWidth
                && mouseY >= hudY && mouseY < hudY + panelHeight;
    }

    /** Bottom-right resize handle, drawn slightly inside the HUD so it is easy to grab. */
    private boolean isOverResizeHandle(double mouseX, double mouseY) {
        int panelWidth = config.panelWidth();
        int panelHeight = config.panelHeight();
        int hudX = config.calculateX(this.width, panelWidth);
        int hudY = config.calculateY(this.height, panelHeight);
        int handleX = hudX + panelWidth - RESIZE_HANDLE_SIZE;
        int handleY = hudY + panelHeight - RESIZE_HANDLE_SIZE;

        return mouseX >= handleX && mouseX <= hudX + panelWidth + 2
                && mouseY >= handleY && mouseY <= hudY + panelHeight + 2;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isOverResizeHandle(event.x(), event.y())) {
            int panelWidth = config.panelWidth();
            int panelHeight = config.panelHeight();
            resizeFixedX = config.calculateX(this.width, panelWidth);
            resizeFixedY = config.calculateY(this.height, panelHeight);
            resizingHud = true;
            draggingHud = false;
            return true;
        }

        if (event.button() == 0 && isOverHud(event.x(), event.y())) {
            int panelWidth = config.panelWidth();
            int panelHeight = config.panelHeight();
            int hudX = config.calculateX(this.width, panelWidth);
            int hudY = config.calculateY(this.height, panelHeight);

            draggingHud = true;
            resizingHud = false;
            dragGrabX = event.x() - hudX;
            dragGrabY = event.y() - hudY;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (resizingHud && event.button() == 0) {
            int desiredWidth = (int) Math.round(event.x() - resizeFixedX);
            int desiredHeight = (int) Math.round(event.y() - resizeFixedY);
            config.setResizedFromCorner(
                    this.width,
                    this.height,
                    resizeFixedX,
                    resizeFixedY,
                    desiredWidth,
                    desiredHeight
            );
            return true;
        }

        if (draggingHud && event.button() == 0) {
            int panelWidth = config.panelWidth();
            int panelHeight = config.panelHeight();
            int desiredX = (int) Math.round(event.x() - dragGrabX);
            int desiredY = (int) Math.round(event.y() - dragGrabY);

            config.setDraggedPosition(this.width, this.height, panelWidth, panelHeight, desiredX, desiredY);
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (resizingHud && event.button() == 0) {
            resizingHud = false;
            config.finishResize();
            return true;
        }

        if (draggingHud && event.button() == 0) {
            draggingHud = false;
            config.finishDrag();
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String title = "QuickChestCapacity Settings";
        String moveLabel = "Drag the HUD to move it • Drag the gold corner to resize it";
        String dragHint;
        if (resizingHud) {
            dragHint = "Resizing HUD — " + config.scalePercent() + "% — release to save";
        } else if (draggingHud) {
            dragHint = "Dragging HUD — release to save";
        } else {
            dragHint = "Move: grab the card   Resize: grab the bottom-right gold square";
        }
        String hint = "Press K in game to open this menu • Key can be changed in Controls";

        graphics.text(this.font, title, (this.width - this.font.width(title)) / 2, 16, 0xFFFFC400, true);

        int startY = Math.max(38, this.height / 2 - 85);
        graphics.text(this.font, moveLabel, (this.width - this.font.width(moveLabel)) / 2, startY + 60, 0xFFFFFFFF, true);
        graphics.text(this.font, dragHint, (this.width - this.font.width(dragHint)) / 2, startY + 142,
                (draggingHud || resizingHud) ? 0xFFFFC400 : 0xFFDDDDDD, true);
        graphics.text(this.font, hint, (this.width - this.font.width(hint)) / 2, startY + 160, 0xFFAAAAAA, true);

        // Visible resize handle on the live HUD preview.
        int panelWidth = config.panelWidth();
        int panelHeight = config.panelHeight();
        int hudX = config.calculateX(this.width, panelWidth);
        int hudY = config.calculateY(this.height, panelHeight);
        int handleX = hudX + panelWidth - RESIZE_HANDLE_SIZE;
        int handleY = hudY + panelHeight - RESIZE_HANDLE_SIZE;
        int handleColor = resizingHud ? 0xFFFFFFFF : 0xFFFFC400;
        graphics.fill(handleX, handleY, hudX + panelWidth, hudY + panelHeight, handleColor);
        graphics.outline(handleX, handleY, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE, 0xFF4B3513);
    }

    @Override
    public void onClose() {
        draggingHud = false;
        resizingHud = false;
        config.save();
        this.minecraft.gui.setScreen(null);
    }
}
