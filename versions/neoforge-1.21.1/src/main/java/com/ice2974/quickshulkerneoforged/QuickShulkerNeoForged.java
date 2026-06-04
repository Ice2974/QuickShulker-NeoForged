package com.ice2974.quickshulkerneoforged;

import com.ice2974.quickshulkerneoforged.neoforge.network.NeoForgeQuickShulkerNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(QuickShulkerConstants.MOD_ID)
public final class QuickShulkerNeoForged {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuickShulkerNeoForged.class);

    public QuickShulkerNeoForged(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeQuickShulkerNetwork::register);
        LOGGER.info(QuickShulkerCommon.bootstrapMessage("NeoForge", "1.21.1"));
    }
}
