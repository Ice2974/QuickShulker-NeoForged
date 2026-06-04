package com.ice2974.quickshulkerneoforged;

import com.ice2974.quickshulkerneoforged.common.config.QuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenableRegistry;

public final class QuickShulkerCommon {
    private QuickShulkerCommon() {
    }

    public static String bootstrapMessage(String platformName, String targetVersion) {
        return QuickShulkerConstants.MOD_NAME
            + " skeleton bootstrap on "
            + platformName
            + " for Minecraft "
            + targetVersion;
    }

    public static QuickOpenableRegistry createDefaultQuickOpenableRegistry() {
        return BuiltinQuickOpenables.createDefaultRegistry();
    }

    public static QuickShulkerConfig defaultConfig() {
        return QuickShulkerConfig.defaults();
    }
}
