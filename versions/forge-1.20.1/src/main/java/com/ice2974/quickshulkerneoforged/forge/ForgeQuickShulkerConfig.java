package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.config.QuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.common.config.QuickShulkerConfigView;

public final class ForgeQuickShulkerConfig {
    private static final QuickShulkerConfigView DEFAULTS = QuickShulkerConfig.defaults();

    private ForgeQuickShulkerConfig() {
    }

    public static QuickShulkerConfigView view() {
        return DEFAULTS;
    }
}
