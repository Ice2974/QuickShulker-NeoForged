package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.QuickShulkerCommon;
import com.ice2974.quickshulkerneoforged.common.config.QuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.common.config.QuickShulkerConfigView;

public final class NeoForgeQuickShulkerConfig {
    private static final QuickShulkerConfig CONFIG = QuickShulkerCommon.defaultConfig();

    private NeoForgeQuickShulkerConfig() {
    }

    public static QuickShulkerConfigView view() {
        return CONFIG;
    }
}
