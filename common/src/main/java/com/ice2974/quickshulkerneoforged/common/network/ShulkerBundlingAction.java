package com.ice2974.quickshulkerneoforged.common.network;

public enum ShulkerBundlingAction {
    INSERT,
    PICKUP_INSERT,
    MOUSE_DRAG_INSERT,
    MOUSE_DRAG_PICKUP_INSERT,
    EXTRACT,
    TRANSFER,
    UNKNOWN;

    public static ShulkerBundlingAction fromSerializedName(String value) {
        if (value == null) {
            return UNKNOWN;
        }

        try {
            return ShulkerBundlingAction.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}
