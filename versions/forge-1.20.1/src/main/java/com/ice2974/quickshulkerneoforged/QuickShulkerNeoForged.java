package com.ice2974.quickshulkerneoforged;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(QuickShulkerConstants.MOD_ID)
public final class QuickShulkerNeoForged {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuickShulkerNeoForged.class);

    public QuickShulkerNeoForged() {
        LOGGER.info(QuickShulkerCommon.bootstrapMessage("Forge", "1.20.1"));
    }
}
