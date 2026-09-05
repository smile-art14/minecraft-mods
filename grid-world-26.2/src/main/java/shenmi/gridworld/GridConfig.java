package shenmi.gridworld;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class GridConfig {
    private static final int DEFAULT_SPACING = 5;
    private static final int DEFAULT_THICKNESS = 1;

    private static int spacing = DEFAULT_SPACING;
    private static int thickness = DEFAULT_THICKNESS;

    private GridConfig() {
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("shenmi-grid-world.properties");
        Properties properties = new Properties();

        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException exception) {
                ShenmiGridWorld.LOGGER.warn("Could not read {}, using defaults", path, exception);
            }
        }

        spacing = readInt(properties, "gridSpacing", DEFAULT_SPACING, 2, 64);
        thickness = readInt(properties, "gridThickness", DEFAULT_THICKNESS, 1, spacing - 1);

        properties.setProperty("gridSpacing", Integer.toString(spacing));
        properties.setProperty("gridThickness", Integer.toString(thickness));

        try {
            Files.createDirectories(path.getParent());
            try (OutputStream output = Files.newOutputStream(path)) {
                properties.store(output, "Shenmi Grid World - restart Minecraft after changing these values");
            }
        } catch (IOException exception) {
            ShenmiGridWorld.LOGGER.warn("Could not write {}", path, exception);
        }
    }

    public static int spacing() {
        return spacing;
    }

    public static int thickness() {
        return thickness;
    }

    private static int readInt(Properties properties, String key, int fallback, int minimum, int maximum) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            return fallback;
        }

        try {
            int value = Integer.parseInt(raw.trim());
            if (value >= minimum && value <= maximum) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }

        ShenmiGridWorld.LOGGER.warn(
                "Invalid {}={}, expected {}..{}; using {}",
                key,
                raw,
                minimum,
                maximum,
                fallback);
        return fallback;
    }
}
