package com.ice2974.quickshulkerneoforged.forge.client;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.forge.ForgeQuickShulkerConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = QuickShulkerConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeKeyMappings {
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

    private ForgeKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        syncFromConfig();
        event.register(OPEN_HELD_SHULKER);
        event.register(OPEN_SETTINGS_SCREEN);
    }

    public static void syncFromConfig() {
        OPEN_HELD_SHULKER.setKey(resolveKey(
            ForgeQuickShulkerConfig.snapshot().activationKey().translationKey(),
            InputConstants.KEY_K
        ));
        OPEN_SETTINGS_SCREEN.setKey(resolveKey(
            ForgeQuickShulkerConfig.snapshot().openSettingsKey().translationKey(),
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
