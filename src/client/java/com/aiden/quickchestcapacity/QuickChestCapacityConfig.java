package com.aiden.quickchestcapacity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class QuickChestCapacityConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("quickchestcapacity.properties");

    // Keeps custom resizing usable without making the text unreadable or the HUD enormous.
    private static final int MIN_PANEL_WIDTH = 118;
    private static final int MIN_PANEL_HEIGHT = 36;
    private static final int MAX_PANEL_WIDTH = 300;
    private static final int MAX_PANEL_HEIGHT = 82;

    private HudSize hudSize = HudSize.NORMAL;
    private HudPosition hudPosition = HudPosition.BOTTOM_CENTER;
    private int offsetX = 0;
    private int offsetY = 0;
    private float hudScale = 1.0f;

    public static QuickChestCapacityConfig load() {
        QuickChestCapacityConfig config = new QuickChestCapacityConfig();
        if (!Files.exists(CONFIG_PATH)) {
            return config;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
            properties.load(input);
            config.hudSize = HudSize.fromString(properties.getProperty("hudSize"));
            config.hudPosition = HudPosition.fromString(properties.getProperty("hudPosition"));
            config.offsetX = parseInt(properties.getProperty("offsetX"), 0, -1000, 1000);
            config.offsetY = parseInt(properties.getProperty("offsetY"), 0, -1000, 1000);
            config.hudScale = parseFloat(properties.getProperty("hudScale"), 1.0f, 0.4f, 2.5f);
            config.clampScaleToSizeLimits();
        } catch (IOException ignored) {
            // Use defaults if the config cannot be read.
        }
        return config;
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("hudSize", hudSize.name());
        properties.setProperty("hudPosition", hudPosition.name());
        properties.setProperty("offsetX", Integer.toString(offsetX));
        properties.setProperty("offsetY", Integer.toString(offsetY));
        properties.setProperty("hudScale", Float.toString(hudScale));

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(output, "QuickChestCapacity HUD settings");
            }
        } catch (IOException ignored) {
            // The mod still works even if the settings cannot be written.
        }
    }

    public HudSize hudSize() {
        return hudSize;
    }

    public HudPosition hudPosition() {
        return hudPosition;
    }

    public int offsetX() {
        return offsetX;
    }

    public int offsetY() {
        return offsetY;
    }

    public int scalePercent() {
        return Math.round(hudScale * 100.0f);
    }

    public boolean hasCustomScale() {
        return Math.abs(hudScale - 1.0f) > 0.005f;
    }

    public void cycleSize() {
        hudSize = hudSize.next();
        // Selecting a named size gives that preset its normal dimensions again.
        hudScale = 1.0f;
        save();
    }

    public void cyclePosition() {
        hudPosition = hudPosition.next();
        offsetX = 0;
        offsetY = 0;
        save();
    }

    public void move(int dx, int dy) {
        offsetX = clamp(offsetX + dx, -1000, 1000);
        offsetY = clamp(offsetY + dy, -1000, 1000);
        save();
    }

    /**
     * Moves the HUD to an exact top-left position while it is being dragged.
     * This updates the live preview without writing the config file every mouse frame.
     */
    public void setDraggedPosition(int guiWidth, int guiHeight, int panelWidth, int panelHeight, int desiredX, int desiredY) {
        int clampedX = clamp(desiredX, 2, Math.max(2, guiWidth - panelWidth - 2));
        int clampedY = clamp(desiredY, 2, Math.max(2, guiHeight - panelHeight - 2));

        int baseX = calculateBaseX(guiWidth, panelWidth);
        int baseY = calculateBaseY(guiHeight, panelHeight);
        offsetX = clamp(clampedX - baseX, -1000, 1000);
        offsetY = clamp(clampedY - baseY, -1000, 1000);
    }

    /**
     * Resizes the HUD from its bottom-right corner while keeping the top-left corner fixed.
     * Width and height are averaged so dragging diagonally preserves the HUD's proportions.
     */
    public void setResizedFromCorner(
            int guiWidth,
            int guiHeight,
            int fixedX,
            int fixedY,
            int desiredWidth,
            int desiredHeight
    ) {
        int baseWidth = basePanelWidth();
        int baseHeight = basePanelHeight();

        float widthScale = desiredWidth / (float) baseWidth;
        float heightScale = desiredHeight / (float) baseHeight;
        float desiredScale = (widthScale + heightScale) * 0.5f;

        float minScale = Math.max(
                MIN_PANEL_WIDTH / (float) baseWidth,
                MIN_PANEL_HEIGHT / (float) baseHeight
        );
        float maxScale = Math.min(
                MAX_PANEL_WIDTH / (float) baseWidth,
                MAX_PANEL_HEIGHT / (float) baseHeight
        );

        // Also keep the resized HUD inside the current GUI when possible.
        maxScale = Math.min(maxScale, Math.max(minScale, (guiWidth - 4) / (float) baseWidth));
        maxScale = Math.min(maxScale, Math.max(minScale, (guiHeight - 4) / (float) baseHeight));

        hudScale = clamp(desiredScale, minScale, maxScale);

        int newWidth = panelWidth();
        int newHeight = panelHeight();
        int clampedFixedX = clamp(fixedX, 2, Math.max(2, guiWidth - newWidth - 2));
        int clampedFixedY = clamp(fixedY, 2, Math.max(2, guiHeight - newHeight - 2));

        int baseX = calculateBaseX(guiWidth, newWidth);
        int baseY = calculateBaseY(guiHeight, newHeight);
        offsetX = clamp(clampedFixedX - baseX, -1000, 1000);
        offsetY = clamp(clampedFixedY - baseY, -1000, 1000);
    }

    public void finishDrag() {
        save();
    }

    public void finishResize() {
        save();
    }

    public void reset() {
        hudSize = HudSize.NORMAL;
        hudPosition = HudPosition.BOTTOM_CENTER;
        offsetX = 0;
        offsetY = 0;
        hudScale = 1.0f;
        save();
    }

    public int panelWidth() {
        return Math.round(basePanelWidth() * hudScale);
    }

    public int panelHeight() {
        return Math.round(basePanelHeight() * hudScale);
    }

    public int barHeight() {
        return Math.max(4, Math.round(baseBarHeight() * hudScale));
    }

    private int basePanelWidth() {
        return switch (hudSize) {
            case XXSMALL -> 142;
            case XSMALL -> 156;
            case SMALL -> 170;
            case NORMAL -> 194;
            case LARGE -> 230;
        };
    }

    private int basePanelHeight() {
        return switch (hudSize) {
            case XXSMALL -> 42;
            case XSMALL -> 45;
            case SMALL -> 48;
            case NORMAL -> 54;
            case LARGE -> 62;
        };
    }

    private int baseBarHeight() {
        return switch (hudSize) {
            case XXSMALL -> 6;
            case XSMALL -> 7;
            case SMALL -> 8;
            case NORMAL -> 10;
            case LARGE -> 13;
        };
    }

    private void clampScaleToSizeLimits() {
        int baseWidth = basePanelWidth();
        int baseHeight = basePanelHeight();
        float minScale = Math.max(
                MIN_PANEL_WIDTH / (float) baseWidth,
                MIN_PANEL_HEIGHT / (float) baseHeight
        );
        float maxScale = Math.min(
                MAX_PANEL_WIDTH / (float) baseWidth,
                MAX_PANEL_HEIGHT / (float) baseHeight
        );
        hudScale = clamp(hudScale, minScale, maxScale);
    }

    public int calculateX(int guiWidth, int panelWidth) {
        return clamp(calculateBaseX(guiWidth, panelWidth) + offsetX, 2, Math.max(2, guiWidth - panelWidth - 2));
    }

    public int calculateY(int guiHeight, int panelHeight) {
        return clamp(calculateBaseY(guiHeight, panelHeight) + offsetY, 2, Math.max(2, guiHeight - panelHeight - 2));
    }

    private int calculateBaseX(int guiWidth, int panelWidth) {
        int margin = 8;
        return switch (hudPosition) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> margin;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> (guiWidth - panelWidth) / 2;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> guiWidth - panelWidth - margin;
        };
    }

    private int calculateBaseY(int guiHeight, int panelHeight) {
        int topMargin = 8;
        int bottomMargin = 44;
        return switch (hudPosition) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> topMargin;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> (guiHeight - panelHeight) / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> guiHeight - panelHeight - bottomMargin;
        };
    }

    private static int parseInt(String value, int fallback, int min, int max) {
        if (value == null) return fallback;
        try {
            return clamp(Integer.parseInt(value), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback, float min, float max) {
        if (value == null) return fallback;
        try {
            return clamp(Float.parseFloat(value), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum HudSize {
        XXSMALL("XXSmall"),
        XSMALL("XSmall"),
        SMALL("Small"),
        NORMAL("Normal"),
        LARGE("Large");

        private final String displayName;

        HudSize(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public HudSize next() {
            HudSize[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static HudSize fromString(String value) {
            if (value == null) return NORMAL;
            try {
                return valueOf(value);
            } catch (IllegalArgumentException ignored) {
                return NORMAL;
            }
        }
    }

    public enum HudPosition {
        TOP_LEFT("Top Left"),
        TOP_CENTER("Top Centre"),
        TOP_RIGHT("Top Right"),
        CENTER_LEFT("Centre Left"),
        CENTER("Centre"),
        CENTER_RIGHT("Centre Right"),
        BOTTOM_LEFT("Bottom Left"),
        BOTTOM_CENTER("Bottom Centre"),
        BOTTOM_RIGHT("Bottom Right");

        private final String displayName;

        HudPosition(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public HudPosition next() {
            HudPosition[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public static HudPosition fromString(String value) {
            if (value == null) return BOTTOM_CENTER;
            try {
                return valueOf(value);
            } catch (IllegalArgumentException ignored) {
                return BOTTOM_CENTER;
            }
        }
    }
}
