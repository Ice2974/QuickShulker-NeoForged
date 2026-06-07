package com.ice2974.quickshulkerneoforged.common.config;

import java.util.Objects;

public record QuickShulkerConfig(
    KeyBindingSpec activationKey,
    KeyBindingSpec openSettingsKey,
    boolean rightClickToOpen,
    boolean keybindInHand,
    boolean keybindInInventory,
    boolean rightClickInInventory,
    boolean supportsBundlingInsert,
    boolean supportsBundlingPickup,
    boolean supportsBundlingTransfer,
    boolean supportsBundlingExtract,
    boolean supportsMouseDragged,
    boolean openSettingsKeyEnabled,
    boolean quickShulkerBox,
    boolean quickCraftingTables,
    boolean quickStonecutter,
    boolean quickEnderChest,
    boolean quickAnvil
) implements QuickShulkerConfigView {
    public static final KeyBindingSpec DEFAULT_ACTIVATION_KEY =
        new KeyBindingSpec("activation", "key.keyboard.k");
    public static final KeyBindingSpec DEFAULT_OPEN_SETTINGS_KEY =
        new KeyBindingSpec("open_settings", "key.keyboard.keypad.add");

    public QuickShulkerConfig {
        Objects.requireNonNull(activationKey, "activationKey");
        Objects.requireNonNull(openSettingsKey, "openSettingsKey");
    }

    public static QuickShulkerConfig defaults() {
        return new QuickShulkerConfig(
            DEFAULT_ACTIVATION_KEY,
            DEFAULT_OPEN_SETTINGS_KEY,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true
        );
    }
}
