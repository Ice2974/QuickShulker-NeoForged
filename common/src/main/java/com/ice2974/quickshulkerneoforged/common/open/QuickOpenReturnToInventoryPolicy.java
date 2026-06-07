package com.ice2974.quickshulkerneoforged.common.open;

public final class QuickOpenReturnToInventoryPolicy {
    private QuickOpenReturnToInventoryPolicy() {
    }

    public static boolean shouldReturnToPlayerInventory(QuickOpenableType type, QuickOpenTrigger trigger) {
        return isInventoryTrigger(trigger) && type.reopenPlayerInventoryAfterClose();
    }

    public static boolean isInventoryTrigger(QuickOpenTrigger trigger) {
        return trigger == QuickOpenTrigger.INVENTORY_KEYBIND
            || trigger == QuickOpenTrigger.INVENTORY_RIGHT_CLICK;
    }
}
