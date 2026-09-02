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

    private HudSize hudSize = HudSize.NORMAL;
    private HudPosition hudPosition = HudPosition.BOTTOM_CENTER;
    private int offsetX = 0;
    private int offsetY = 0;

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

    public void cycleSize() {
        hudSize = hudSize.next();
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

    public void reset() {
        hudSize = HudSize.NORMAL;
        hudPosition = HudPosition.BOTTOM_CENTER;
        offsetX = 0;
        offsetY = 0;
        save();
    }

    public int panelWidth() {
        return switch (hudSize) {
            case SMALL -> 170;
            case NORMAL -> 194;
            case LARGE -> 230;
        };
    }

    public int panelHeight() {
        return switch (hudSize) {
            case SMALL -> 48;
            case NORMAL -> 54;
            case LARGE -> 62;
        };
    }

    public int barHeight() {
        return switch (hudSize) {
            case SMALL -> 8;
            case NORMAL -> 10;
            case LARGE -> 13;
        };
    }

    public int calculateX(int guiWidth, int panelWidth) {
        int margin = 8;
        int x = switch (hudPosition) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> margin;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> (guiWidth - panelWidth) / 2;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> guiWidth - panelWidth - margin;
        };
        return clamp(x + offsetX, 2, Math.max(2, guiWidth - panelWidth - 2));
    }

    public int calculateY(int guiHeight, int panelHeight) {
        int topMargin = 8;
        int bottomMargin = 44;
        int y = switch (hudPosition) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> topMargin;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> (guiHeight - panelHeight) / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> guiHeight - panelHeight - bottomMargin;
        };
        return clamp(y + offsetY, 2, Math.max(2, guiHeight - panelHeight - 2));
    }

    private static int parseInt(String value, int fallback, int min, int max) {
        if (value == null) return fallback;
        try {
            return clamp(Integer.parseInt(value), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum HudSize {
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
