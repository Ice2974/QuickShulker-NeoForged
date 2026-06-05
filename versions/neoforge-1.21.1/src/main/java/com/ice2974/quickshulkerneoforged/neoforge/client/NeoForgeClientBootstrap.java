package com.ice2974.quickshulkerneoforged.neoforge.client;

import net.neoforged.bus.api.IEventBus;

public final class NeoForgeClientBootstrap {
    private NeoForgeClientBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeKeyMappings::register);
    }
}
