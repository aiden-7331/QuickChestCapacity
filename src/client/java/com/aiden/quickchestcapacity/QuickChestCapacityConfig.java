package com.aiden.quickchestcapacity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class QuickChestCapacityConfig {
    public static final int BASE_PANEL_WIDTH = 194;
    public static final int BASE_PANEL_HEIGHT = 54;
    public static final int BASE_BAR_HEIGHT = 10;

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("quickchestcapacity.properties");

    private static final float MIN_SCALE = 0.01f;
    private static final float MAX_SCALE = 1.00f;

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
            config.hudPosition = HudPosition.fromString(properties.getProperty("hudPosition"));
            config.offsetX = parseInt(properties.getProperty("offsetX"), 0, -1000, 1000);
            config.offsetY = parseInt(properties.getProperty("offsetY"), 0, -1000, 1000);

            // v1.3.8+ stores one simple 1-100% scale. If this is an older config,
            // convert the previous named-size + custom-scale combination automatically.
            String savedPercent = properties.getProperty("scalePercent");
            if (savedPercent != null) {
                int percent = parseInt(savedPercent, 100, 1, 100);
                config.hudScale = percent / 100.0f;
            } else {
                float legacyScale = parseFloat(properties.getProperty("hudScale"), 1.0f, 0.01f, 2.5f);
                float legacyPresetFactor = legacyPresetFactor(properties.getProperty("hudSize"));
                config.hudScale = clamp(legacyScale * legacyPresetFactor, MIN_SCALE, MAX_SCALE);
            }
        } catch (IOException ignored) {
            // Use defaults if the config cannot be read.
        }
        return config;
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("configVersion", "2");
        properties.setProperty("hudPosition", hudPosition.name());
        properties.setProperty("offsetX", Integer.toString(offsetX));
        properties.setProperty("offsetY", Integer.toString(offsetY));
        properties.setProperty("scalePercent", Integer.toString(scalePercent()));

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
                properties.store(output, "QuickChestCapacity HUD settings");
            }
        } catch (IOException ignored) {
            // The mod still works even if the settings cannot be written.
        }
    }

    public HudPosition hudPosition() {
        return hudPosition;
    }

    public int scalePercent() {
        return Math.max(1, Math.min(100, Math.round(hudScale * 100.0f)));
    }

    public float scale() {
        return hudScale;
    }

    public void setScalePercent(int percent) {
        hudScale = clamp(percent / 100.0f, MIN_SCALE, MAX_SCALE);
    }

    public void cyclePosition() {
        hudPosition = hudPosition.next();
        offsetX = 0;
        offsetY = 0;
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
     * Everything is rendered at one proportional scale, so text can never collide with the bar.
     */
    public void setResizedFromCorner(
            int guiWidth,
            int guiHeight,
            int fixedX,
            int fixedY,
            int desiredWidth,
            int desiredHeight
    ) {
        float widthScale = desiredWidth / (float) BASE_PANEL_WIDTH;
        float heightScale = desiredHeight / (float) BASE_PANEL_HEIGHT;
        float desiredScale = (widthScale + heightScale) * 0.5f;
        hudScale = clamp(desiredScale, MIN_SCALE, MAX_SCALE);

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
        hudPosition = HudPosition.BOTTOM_CENTER;
        offsetX = 0;
        offsetY = 0;
        hudScale = 1.0f;
        save();
    }

    public int panelWidth() {
        return Math.max(1, Math.round(BASE_PANEL_WIDTH * hudScale));
    }

    public int panelHeight() {
        return Math.max(1, Math.round(BASE_PANEL_HEIGHT * hudScale));
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

    private static float legacyPresetFactor(String value) {
        if (value == null) return 1.0f;
        return switch (value) {
            case "XXSMALL" -> 142.0f / BASE_PANEL_WIDTH;
            case "XSMALL" -> 156.0f / BASE_PANEL_WIDTH;
            case "SMALL" -> 170.0f / BASE_PANEL_WIDTH;
            case "LARGE" -> 230.0f / BASE_PANEL_WIDTH;
            default -> 1.0f;
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
