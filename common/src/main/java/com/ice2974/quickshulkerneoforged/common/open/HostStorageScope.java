package com.ice2974.quickshulkerneoforged.common.open;

public enum HostStorageScope {
    PLAYER_MAIN_INVENTORY,
    PLAYER_HOTBAR,
    PLAYER_OFFHAND,
    PLAYER_CONTAINER_MENU,
    CREATIVE_INVENTORY,
    ENDER_CHEST_PROXY,
    UNKNOWN;

    public static HostStorageScope fromSerializedName(String value) {
        if (value == null) {
            return UNKNOWN;
        }

        try {
            return HostStorageScope.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}
