package com.ice2974.quickshulkerneoforged.common.config;

import com.ice2974.quickshulkerneoforged.common.open.QuickOpenConfigGate;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenableType;

public interface QuickShulkerConfigView {
    KeyBindingSpec activationKey();

    KeyBindingSpec openSettingsKey();

    boolean rightClickToOpen();

    boolean keybindInHand();

    boolean keybindInInventory();

    boolean rightClickInInventory();

    boolean rightClickClose();

    boolean supportsBundlingInsert();

    boolean supportsBundlingPickup();

    boolean supportsBundlingTransfer();

    boolean supportsBundlingExtract();

    boolean supportsMouseDragged();

    boolean openSettingsKeyEnabled();

    boolean quickShulkerBox();

    boolean quickCraftingTables();

    boolean quickStonecutter();

    boolean quickEnderChest();

    boolean quickAnvil();

    default boolean isEnabled(QuickOpenConfigGate gate) {
        return switch (gate) {
            case QUICK_SHULKER_BOX -> quickShulkerBox();
            case QUICK_ENDER_CHEST -> quickEnderChest();
            case QUICK_CRAFTING_TABLE -> quickCraftingTables();
            case QUICK_STONECUTTER -> quickStonecutter();
            case QUICK_ANVIL -> quickAnvil();
        };
    }

    default boolean isEnabled(QuickOpenableType type) {
        return isEnabled(type.configGate());
    }
}
