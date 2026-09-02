package com.aiden.quickchestcapacity;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class QuickChestCapacitySettingsScreen extends Screen {
    private static final int RESIZE_HANDLE_SIZE = 9;
    private static final int MIN_HIT_SIZE = 14;

    private final QuickChestCapacityConfig config;

    private HudScaleSlider scaleSlider;
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
        int startY = Math.max(40, this.height / 2 - 68);

        // Replaces the old named HUD-size button with a precise 1-100% slider.
        this.scaleSlider = this.addRenderableWidget(new HudScaleSlider(
                centerX - 100,
                startY,
                200,
                20,
                config
        ));

        this.addRenderableWidget(Button.builder(positionText(), button -> {
            config.cyclePosition();
            button.setMessage(positionText());
        }).bounds(centerX - 100, startY + 28, 200, 20).build());

        // Moved up now that the old movement arrow buttons are gone.
        this.addRenderableWidget(Button.builder(Component.literal("Reset"), button -> {
            config.reset();
            this.minecraft.gui.setScreen(new QuickChestCapacitySettingsScreen(config));
        }).bounds(centerX - 100, startY + 58, 96, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds(centerX + 4, startY + 58, 96, 20).build());
    }

    private Component positionText() {
        return Component.literal("Position preset: " + config.hudPosition().displayName());
    }

    /** Returns true when the cursor is inside the HUD preview. Tiny scales get a larger invisible grab area. */
    private boolean isOverHud(double mouseX, double mouseY) {
        int panelWidth = config.panelWidth();
        int panelHeight = config.panelHeight();
        int hudX = config.calculateX(this.width, panelWidth);
        int hudY = config.calculateY(this.height, panelHeight);

        int hitWidth = Math.max(MIN_HIT_SIZE, panelWidth);
        int hitHeight = Math.max(MIN_HIT_SIZE, panelHeight);
        int hitX = hudX - (hitWidth - panelWidth) / 2;
        int hitY = hudY - (hitHeight - panelHeight) / 2;

        return mouseX >= hitX && mouseX < hitX + hitWidth
                && mouseY >= hitY && mouseY < hitY + hitHeight;
    }

    /** Bottom-right resize handle. */
    private int resizeHandleX() {
        int panelWidth = config.panelWidth();
        int hudX = config.calculateX(this.width, panelWidth);
        return hudX + panelWidth - RESIZE_HANDLE_SIZE;
    }

    private int resizeHandleY() {
        int panelHeight = config.panelHeight();
        int hudY = config.calculateY(this.height, panelHeight);
        return hudY + panelHeight - RESIZE_HANDLE_SIZE;
    }

    private boolean isOverResizeHandle(double mouseX, double mouseY) {
        int handleX = resizeHandleX();
        int handleY = resizeHandleY();
        return mouseX >= handleX && mouseX <= handleX + RESIZE_HANDLE_SIZE + 2
                && mouseY >= handleY && mouseY <= handleY + RESIZE_HANDLE_SIZE + 2;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Let sliders/buttons win if the preview happens to sit behind the settings controls.
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

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

        return false;
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
            if (scaleSlider != null) {
                scaleSlider.syncFromConfig();
            }
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
        // While moving/resizing, hide the entire settings GUI so the player can see the world
        // and the live HUD preview clearly. Releasing the mouse brings the menu straight back.
        if (draggingHud || resizingHud) {
            if (resizingHud) {
                drawResizeHandle(graphics, true);
            }
            return;
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String title = "QuickChestCapacity Settings";
        String dragHint = "Drag the HUD to move it • Drag the gold corner to resize it";
        String sizeHint = "HUD Size slider: 1% - 100%";
        String keyHint = "Press K in game to open this menu • Key can be changed in Controls";

        int startY = Math.max(40, this.height / 2 - 68);
        graphics.text(this.font, title, (this.width - this.font.width(title)) / 2, 16, 0xFFFFC400, true);
        graphics.text(this.font, dragHint, (this.width - this.font.width(dragHint)) / 2, startY + 88, 0xFFFFFFFF, true);
        graphics.text(this.font, sizeHint, (this.width - this.font.width(sizeHint)) / 2, startY + 104, 0xFFDDDDDD, true);
        graphics.text(this.font, keyHint, (this.width - this.font.width(keyHint)) / 2, startY + 120, 0xFFAAAAAA, true);

        drawResizeHandle(graphics, false);
    }

    private void drawResizeHandle(GuiGraphicsExtractor graphics, boolean active) {
        int handleX = resizeHandleX();
        int handleY = resizeHandleY();
        int handleColor = active ? 0xFFFFFFFF : 0xFFFFC400;
        graphics.fill(handleX, handleY, handleX + RESIZE_HANDLE_SIZE, handleY + RESIZE_HANDLE_SIZE, handleColor);
        graphics.outline(handleX, handleY, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE, 0xFF4B3513);
    }

    @Override
    public void onClose() {
        draggingHud = false;
        resizingHud = false;
        config.save();
        this.minecraft.gui.setScreen(null);
    }

    private static final class HudScaleSlider extends AbstractSliderButton {
        private final QuickChestCapacityConfig config;

        private HudScaleSlider(int x, int y, int width, int height, QuickChestCapacityConfig config) {
            super(x, y, width, height, Component.empty(), valueFromPercent(config.scalePercent()));
            this.config = config;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int percent = percentFromValue(this.value);
            this.setMessage(Component.literal("HUD Size: " + percent + "%"));
        }

        @Override
        protected void applyValue() {
            int percent = percentFromValue(this.value);
            config.setScalePercent(percent);
            updateMessage();
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            super.onRelease(event);
            config.save();
        }

        private void syncFromConfig() {
            this.value = valueFromPercent(config.scalePercent());
            updateMessage();
        }

        private static double valueFromPercent(int percent) {
            return (Math.max(1, Math.min(100, percent)) - 1) / 99.0;
        }

        private static int percentFromValue(double value) {
            return 1 + (int) Math.round(Math.max(0.0, Math.min(1.0, value)) * 99.0);
        }
    }
}
