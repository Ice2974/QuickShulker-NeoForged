package com.ice2974.quickshulkerneoforged.neoforge.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.bus.api.IEventBus;

public final class NeoForgeClientBootstrap {
    private NeoForgeClientBootstrap() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(NeoForgeKeyMappings::register);
        modContainer.registerExtensionPoint(
            IConfigScreenFactory.class,
            (IConfigScreenFactory) (container, parent) -> new NeoForgeQuickShulkerConfigScreen(parent)
        );
    }
}
