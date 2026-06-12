package com.ice2974.quickshulkerneoforged;

import com.ice2974.quickshulkerneoforged.forge.ForgeQuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.forge.ForgeServices;
import com.ice2974.quickshulkerneoforged.forge.client.ForgeClientBootstrap;
import com.ice2974.quickshulkerneoforged.forge.network.ForgeQuickShulkerNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(QuickShulkerConstants.MOD_ID)
public final class QuickShulkerNeoForged {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuickShulkerNeoForged.class);

    public QuickShulkerNeoForged(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, ForgeQuickShulkerConfig.SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClientBootstrap.register(context));
        ForgeQuickShulkerNetwork.register(ForgeServices.SHULKER_SESSIONS);
        LOGGER.info(QuickShulkerCommon.bootstrapMessage("Forge", "1.20.1"));
    }
}
