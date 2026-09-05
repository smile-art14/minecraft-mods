package com.shenmi.xray;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class XRayClient implements ClientModInitializer {
    private static final float STEP = 0.1F;
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("x-ray.properties");

    private static boolean enabled;
    private static float strength = 1.0F;
    private static boolean toggleWasDown;
    private static boolean decreaseWasDown;
    private static boolean increaseWasDown;
    private static Object lastLevel;
    private static int lastPlayerId = Integer.MIN_VALUE;

    @Override
    public void onInitializeClient() {
        loadConfig();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static float getStrength() {
        return strength;
    }

    public static void tick(Minecraft minecraft) {
        refreshConnectionState(minecraft);

        // Keep the client lightmap authoritative while X-Ray is enabled. This does
        // not touch the player's MobEffect map, so a remote server cannot overwrite
        // the visual state when it synchronizes potion effects.
        if (enabled && minecraft.level != null && minecraft.player != null) {
            minecraft.gameRenderer.lightTexture().tick();
        }

        long window = minecraft.getWindow().getWindow();
        boolean toggleDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_X);
        boolean decreaseDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_BRACKET);
        boolean increaseDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_BRACKET);
        boolean changed = false;

        if (minecraft.screen == null && toggleDown && !toggleWasDown) {
            enabled = !enabled;
            changed = true;
            if (enabled) {
                showMessage(minecraft, "message.xray.enabled", strengthPercent());
            } else {
                showMessage(minecraft, "message.xray.disabled");
            }
        }

        if (minecraft.screen == null && decreaseDown && !decreaseWasDown) {
            strength = clampAndRound(strength - STEP);
            changed = true;
            showMessage(minecraft, "message.xray.strength", strengthPercent());
        }

        if (minecraft.screen == null && increaseDown && !increaseWasDown) {
            strength = clampAndRound(strength + STEP);
            changed = true;
            showMessage(minecraft, "message.xray.strength", strengthPercent());
        }

        toggleWasDown = toggleDown;
        decreaseWasDown = decreaseDown;
        increaseWasDown = increaseDown;

        if (changed) {
            saveConfig();
            minecraft.gameRenderer.lightTexture().tick();
        }
    }

    private static void refreshConnectionState(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            lastLevel = null;
            lastPlayerId = Integer.MIN_VALUE;
            return;
        }

        Object currentLevel = minecraft.level;
        int currentPlayerId = minecraft.player.getId();
        if (currentLevel != lastLevel || currentPlayerId != lastPlayerId) {
            lastLevel = currentLevel;
            lastPlayerId = currentPlayerId;
            // Joining a remote server, respawning, or changing dimensions replaces
            // the LocalPlayer/ClientLevel. Force a fresh lightmap immediately so a
            // persisted enabled state works for every client, not only the host.
            minecraft.gameRenderer.lightTexture().tick();
        }
    }

    private static void showMessage(Minecraft minecraft, String translationKey, Object... args) {
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(translationKey, args), true);
        }
    }

    private static int strengthPercent() {
        return Math.round(strength * 100.0F);
    }

    private static float clampAndRound(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return Math.round(clamped * 10.0F) / 10.0F;
    }

    private static void loadConfig() {
        if (!Files.isRegularFile(CONFIG_PATH)) {
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            properties.load(reader);
            enabled = Boolean.parseBoolean(properties.getProperty("enabled", "false"));
            strength = clampAndRound(Float.parseFloat(
                    properties.getProperty("strength", "1.0")
            ));
        } catch (IOException | NumberFormatException exception) {
            System.err.println("[X-Ray] Unable to read config: " + exception.getMessage());
        }
    }

    private static void saveConfig() {
        Properties properties = new Properties();
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty("strength", Float.toString(strength));

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                properties.store(writer, "X-Ray client settings");
            }
        } catch (IOException exception) {
            System.err.println("[X-Ray] Unable to save config: " + exception.getMessage());
        }
    }
}
