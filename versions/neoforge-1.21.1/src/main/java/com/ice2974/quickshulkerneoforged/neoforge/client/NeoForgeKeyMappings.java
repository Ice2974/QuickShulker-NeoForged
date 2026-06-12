package com.ice2974.quickshulkerneoforged.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeQuickShulkerConfig;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class NeoForgeKeyMappings {
    public static final String CATEGORY = "key.categories.quickshulker";
    public static final KeyMapping OPEN_HELD_SHULKER = new KeyMapping(
        "key.quickshulker_neoforged.open_held_shulker",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_K,
        CATEGORY
    );
    public static final KeyMapping OPEN_SETTINGS_SCREEN = new KeyMapping(
        "key.quickshulker_neoforged.open_settings",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD,
        CATEGORY
    );

    private NeoForgeKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        syncFromConfig();
        event.register(OPEN_HELD_SHULKER);
        event.register(OPEN_SETTINGS_SCREEN);
    }

    public static void syncFromConfig() {
        OPEN_HELD_SHULKER.setKey(resolveKey(
            NeoForgeQuickShulkerConfig.snapshot().activationKey().translationKey(),
            InputConstants.KEY_K
        ));
        OPEN_SETTINGS_SCREEN.setKey(resolveKey(
            NeoForgeQuickShulkerConfig.snapshot().openSettingsKey().translationKey(),
            org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD
        ));
        KeyMapping.resetMapping();
    }

    private static InputConstants.Key resolveKey(String configuredKey, int fallbackKeyCode) {
        try {
            return InputConstants.getKey(configuredKey);
        } catch (RuntimeException exception) {
            return InputConstants.Type.KEYSYM.getOrCreate(fallbackKeyCode);
        }
    }
}
