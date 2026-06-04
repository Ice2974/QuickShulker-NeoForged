package com.ice2974.quickshulkerneoforged;

import com.ice2974.quickshulkerneoforged.forge.ForgeQuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.forge.ForgeServices;
import com.ice2974.quickshulkerneoforged.forge.network.ForgeQuickShulkerNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(QuickShulkerConstants.MOD_ID)
public final class QuickShulkerNeoForged {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuickShulkerNeoForged.class);

    public QuickShulkerNeoForged() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ForgeQuickShulkerConfig.SPEC);
        ForgeQuickShulkerNetwork.register(ForgeServices.SHULKER_SESSIONS);
        LOGGER.info(QuickShulkerCommon.bootstrapMessage("Forge", "1.20.1"));
    }
}
