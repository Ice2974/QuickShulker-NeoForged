package com.ice2974.quickshulkerneoforged.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class NeoForgeKeyMappings {
    public static final String CATEGORY = "key.categories.quickshulker";
    public static final KeyMapping OPEN_HELD_SHULKER = new KeyMapping(
        "key.quickshulker_neoforged.open_held_shulker",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_K,
        CATEGORY
    );

    private NeoForgeKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_HELD_SHULKER);
    }
}
