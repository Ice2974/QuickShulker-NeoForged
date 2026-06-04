package com.ice2974.quickshulkerneoforged.forge.client;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = QuickShulkerConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeKeyMappings {
    public static final String CATEGORY = "key.categories.quickshulker";
    public static final KeyMapping OPEN_HELD_SHULKER = new KeyMapping(
        "key.quickshulker_neoforged.open_held_shulker",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_K,
        CATEGORY
    );

    private ForgeKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_HELD_SHULKER);
    }
}
