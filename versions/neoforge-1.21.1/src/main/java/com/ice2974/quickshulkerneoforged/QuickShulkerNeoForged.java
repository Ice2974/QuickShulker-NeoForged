package com.ice2974.quickshulkerneoforged;

import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeQuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.neoforge.network.NeoForgeQuickShulkerNetwork;
import java.lang.reflect.InvocationTargetException;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(QuickShulkerConstants.MOD_ID)
public final class QuickShulkerNeoForged {
    private static final String CLIENT_BOOTSTRAP_CLASS =
        "com.ice2974.quickshulkerneoforged.neoforge.client.NeoForgeClientBootstrap";
    private static final Logger LOGGER = LoggerFactory.getLogger(QuickShulkerNeoForged.class);

    public QuickShulkerNeoForged(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, NeoForgeQuickShulkerConfig.SPEC);
        modEventBus.addListener(NeoForgeQuickShulkerNetwork::register);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registerClientBootstrap(modEventBus, modContainer);
        }
        LOGGER.info(QuickShulkerCommon.bootstrapMessage("NeoForge", "1.21.1"));
    }

    private static void registerClientBootstrap(IEventBus modEventBus, ModContainer modContainer) {
        try {
            Class<?> bootstrapClass = Class.forName(CLIENT_BOOTSTRAP_CLASS);
            bootstrapClass.getMethod("register", IEventBus.class, ModContainer.class).invoke(null, modEventBus, modContainer);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException("Failed to access NeoForge client bootstrap", exception);
        } catch (InvocationTargetException exception) {
            throw new RuntimeException("NeoForge client bootstrap threw during registration", exception.getCause());
        }
    }
}
