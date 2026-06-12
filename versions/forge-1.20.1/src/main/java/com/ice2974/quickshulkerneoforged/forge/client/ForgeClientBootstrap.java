package com.ice2974.quickshulkerneoforged.forge.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ForgeClientBootstrap {
    private ForgeClientBootstrap() {
    }

    public static void register(FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(ForgeQuickShulkerConfigScreen::new)
        );
    }
}
