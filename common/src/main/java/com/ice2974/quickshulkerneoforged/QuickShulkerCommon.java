package com.ice2974.quickshulkerneoforged;

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
}
