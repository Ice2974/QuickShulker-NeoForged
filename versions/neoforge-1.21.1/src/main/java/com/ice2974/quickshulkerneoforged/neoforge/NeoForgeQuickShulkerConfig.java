package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.config.QuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.common.config.QuickShulkerConfigView;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoForgeQuickShulkerConfig {
    private static final QuickShulkerConfigView DEFAULTS = QuickShulkerConfig.defaults();
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<String> ACTIVATION_KEY;
    private static final ModConfigSpec.ConfigValue<String> OPEN_SETTINGS_KEY;
    private static final ModConfigSpec.BooleanValue RIGHT_CLICK_TO_OPEN;
    private static final ModConfigSpec.BooleanValue KEYBIND_IN_HAND;
    private static final ModConfigSpec.BooleanValue KEYBIND_IN_INVENTORY;
    private static final ModConfigSpec.BooleanValue RIGHT_CLICK_IN_INVENTORY;
    private static final ModConfigSpec.BooleanValue SUPPORTS_BUNDLING_INSERT;
    private static final ModConfigSpec.BooleanValue SUPPORTS_BUNDLING_PICKUP;
    private static final ModConfigSpec.BooleanValue SUPPORTS_BUNDLING_TRANSFER;
    private static final ModConfigSpec.BooleanValue SUPPORTS_BUNDLING_EXTRACT;
    private static final ModConfigSpec.BooleanValue SUPPORTS_MOUSE_DRAGGED;
    private static final ModConfigSpec.BooleanValue ENDER_CHEST_BUNDLING_INSERT;
    private static final ModConfigSpec.BooleanValue ENDER_CHEST_BUNDLING_PICKUP;
    private static final ModConfigSpec.BooleanValue ENDER_CHEST_BUNDLING_EXTRACT;
    private static final ModConfigSpec.BooleanValue ENDER_CHEST_MOUSE_DRAGGED;
    private static final ModConfigSpec.BooleanValue OPEN_SETTINGS_KEY_ENABLED;
    private static final ModConfigSpec.BooleanValue QUICK_SHULKER_BOX;
    private static final ModConfigSpec.BooleanValue QUICK_CRAFTING_TABLE;
    private static final ModConfigSpec.BooleanValue QUICK_STONECUTTER;
    private static final ModConfigSpec.BooleanValue QUICK_ENDER_CHEST;
    private static final ModConfigSpec.BooleanValue QUICK_ANVIL;
    private static final QuickShulkerConfigView VIEW = new ConfigView();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("activation");
        ACTIVATION_KEY = builder
            .comment("Keyboard key used to quick-open a supported item.")
            .define("activationKey", DEFAULTS.activationKey().translationKey());
        OPEN_SETTINGS_KEY = builder
            .comment("Keyboard key used to open the QuickShulker config screen.")
            .define("openSettingsKey", DEFAULTS.openSettingsKey().translationKey());
        RIGHT_CLICK_TO_OPEN = builder
            .comment("Allow opening supported items with right click while held.")
            .define("rightClickToOpen", DEFAULTS.rightClickToOpen());
        KEYBIND_IN_HAND = builder
            .comment("Allow the activation key to open a supported held item.")
            .define("keybindInHand", DEFAULTS.keybindInHand());
        KEYBIND_IN_INVENTORY = builder
            .comment("Allow the activation key to open a supported item from a hovered player inventory slot.")
            .define("keybindInInventory", DEFAULTS.keybindInInventory());
        RIGHT_CLICK_IN_INVENTORY = builder
            .comment("Allow right click to open a supported item from a hovered player inventory slot.")
            .define("rightClickInInventory", DEFAULTS.rightClickInInventory());
        OPEN_SETTINGS_KEY_ENABLED = builder
            .comment("Allow the open settings keybinding to open the QuickShulker config screen.")
            .define("openSettingsKeyEnabled", DEFAULTS.openSettingsKeyEnabled());
        builder.pop();

        builder.push("interaction");
        SUPPORTS_BUNDLING_INSERT = builder
            .comment("Allow inserting carried items into shulker boxes.")
            .define("supportsBundlingInsert", DEFAULTS.supportsBundlingInsert());
        SUPPORTS_BUNDLING_PICKUP = builder
            .comment("Allow picking hovered items into carried shulker boxes.")
            .define("supportsBundlingPickup", DEFAULTS.supportsBundlingPickup());
        SUPPORTS_BUNDLING_TRANSFER = builder
            .comment("Allow transferring between shulker boxes.")
            .define("supportsBundlingTransfer", DEFAULTS.supportsBundlingTransfer());
        SUPPORTS_BUNDLING_EXTRACT = builder
            .comment("Allow extracting items from shulker boxes.")
            .define("supportsBundlingExtract", DEFAULTS.supportsBundlingExtract());
        SUPPORTS_MOUSE_DRAGGED = builder
            .comment("Allow shulker box mouse-drag batch interactions.")
            .define("supportsMouseDragged", DEFAULTS.supportsMouseDragged());
        ENDER_CHEST_BUNDLING_INSERT = builder
            .comment("Allow inserting carried items into ender chests.")
            .define("enderChestBundlingInsert", DEFAULTS.enderChestBundlingInsert());
        ENDER_CHEST_BUNDLING_PICKUP = builder
            .comment("Allow picking hovered items into carried ender chests.")
            .define("enderChestBundlingPickup", DEFAULTS.enderChestBundlingPickup());
        ENDER_CHEST_BUNDLING_EXTRACT = builder
            .comment("Allow extracting items from carried ender chests.")
            .define("enderChestBundlingExtract", DEFAULTS.enderChestBundlingExtract());
        ENDER_CHEST_MOUSE_DRAGGED = builder
            .comment("Allow ender chest mouse-drag batch interactions.")
            .define("enderChestMouseDragged", DEFAULTS.enderChestMouseDragged());
        builder.pop();

        builder.push("quickOpenables");
        QUICK_SHULKER_BOX = builder
            .comment("Enable quick-open behavior for shulker boxes.")
            .define("quickShulkerBox", DEFAULTS.quickShulkerBox());
        QUICK_CRAFTING_TABLE = builder
            .comment("Enable quick-open behavior for crafting tables.")
            .define("quickCraftingTable", DEFAULTS.quickCraftingTables());
        QUICK_STONECUTTER = builder
            .comment("Enable quick-open behavior for stonecutters.")
            .define("quickStonecutter", DEFAULTS.quickStonecutter());
        QUICK_ENDER_CHEST = builder
            .comment("Enable quick-open behavior for ender chests.")
            .define("quickEnderChest", DEFAULTS.quickEnderChest());
        QUICK_ANVIL = builder
            .comment("Enable quick-open behavior for anvils.")
            .define("quickAnvil", DEFAULTS.quickAnvil());
        builder.pop();

        SPEC = builder.build();
    }

    private NeoForgeQuickShulkerConfig() {
    }

    public static QuickShulkerConfigView view() {
        return VIEW;
    }

    public static QuickShulkerConfig snapshot() {
        return new QuickShulkerConfig(
            new com.ice2974.quickshulkerneoforged.common.config.KeyBindingSpec(
                DEFAULTS.activationKey().id(),
                sanitizeKey(ACTIVATION_KEY.get(), DEFAULTS.activationKey().translationKey())
            ),
            new com.ice2974.quickshulkerneoforged.common.config.KeyBindingSpec(
                DEFAULTS.openSettingsKey().id(),
                sanitizeKey(OPEN_SETTINGS_KEY.get(), DEFAULTS.openSettingsKey().translationKey())
            ),
            RIGHT_CLICK_TO_OPEN.get(),
            KEYBIND_IN_HAND.get(),
            KEYBIND_IN_INVENTORY.get(),
            RIGHT_CLICK_IN_INVENTORY.get(),
            SUPPORTS_BUNDLING_INSERT.get(),
            SUPPORTS_BUNDLING_PICKUP.get(),
            SUPPORTS_BUNDLING_TRANSFER.get(),
            SUPPORTS_BUNDLING_EXTRACT.get(),
            SUPPORTS_MOUSE_DRAGGED.get(),
            ENDER_CHEST_BUNDLING_INSERT.get(),
            ENDER_CHEST_BUNDLING_PICKUP.get(),
            ENDER_CHEST_BUNDLING_EXTRACT.get(),
            ENDER_CHEST_MOUSE_DRAGGED.get(),
            OPEN_SETTINGS_KEY_ENABLED.get(),
            QUICK_SHULKER_BOX.get(),
            QUICK_CRAFTING_TABLE.get(),
            QUICK_STONECUTTER.get(),
            QUICK_ENDER_CHEST.get(),
            QUICK_ANVIL.get()
        );
    }

    public static void apply(QuickShulkerConfig config) {
        ACTIVATION_KEY.set(sanitizeKey(config.activationKey().translationKey(), DEFAULTS.activationKey().translationKey()));
        OPEN_SETTINGS_KEY.set(sanitizeKey(config.openSettingsKey().translationKey(), DEFAULTS.openSettingsKey().translationKey()));
        RIGHT_CLICK_TO_OPEN.set(config.rightClickToOpen());
        KEYBIND_IN_HAND.set(config.keybindInHand());
        KEYBIND_IN_INVENTORY.set(config.keybindInInventory());
        RIGHT_CLICK_IN_INVENTORY.set(config.rightClickInInventory());
        SUPPORTS_BUNDLING_INSERT.set(config.supportsBundlingInsert());
        SUPPORTS_BUNDLING_PICKUP.set(config.supportsBundlingPickup());
        SUPPORTS_BUNDLING_TRANSFER.set(config.supportsBundlingTransfer());
        SUPPORTS_BUNDLING_EXTRACT.set(config.supportsBundlingExtract());
        SUPPORTS_MOUSE_DRAGGED.set(config.supportsMouseDragged());
        ENDER_CHEST_BUNDLING_INSERT.set(config.enderChestBundlingInsert());
        ENDER_CHEST_BUNDLING_PICKUP.set(config.enderChestBundlingPickup());
        ENDER_CHEST_BUNDLING_EXTRACT.set(config.enderChestBundlingExtract());
        ENDER_CHEST_MOUSE_DRAGGED.set(config.enderChestMouseDragged());
        OPEN_SETTINGS_KEY_ENABLED.set(config.openSettingsKeyEnabled());
        QUICK_SHULKER_BOX.set(config.quickShulkerBox());
        QUICK_CRAFTING_TABLE.set(config.quickCraftingTables());
        QUICK_STONECUTTER.set(config.quickStonecutter());
        QUICK_ENDER_CHEST.set(config.quickEnderChest());
        QUICK_ANVIL.set(config.quickAnvil());
        SPEC.save();
    }

    private static String sanitizeKey(String configuredKey, String fallbackKey) {
        return configuredKey == null || configuredKey.isBlank() ? fallbackKey : configuredKey;
    }

    private static final class ConfigView implements QuickShulkerConfigView {
        @Override
        public com.ice2974.quickshulkerneoforged.common.config.KeyBindingSpec activationKey() {
            return snapshot().activationKey();
        }

        @Override
        public com.ice2974.quickshulkerneoforged.common.config.KeyBindingSpec openSettingsKey() {
            return snapshot().openSettingsKey();
        }

        @Override
        public boolean rightClickToOpen() {
            return RIGHT_CLICK_TO_OPEN.get();
        }

        @Override
        public boolean keybindInHand() {
            return KEYBIND_IN_HAND.get();
        }

        @Override
        public boolean keybindInInventory() {
            return KEYBIND_IN_INVENTORY.get();
        }

        @Override
        public boolean rightClickInInventory() {
            return RIGHT_CLICK_IN_INVENTORY.get();
        }

        @Override
        public boolean supportsBundlingInsert() {
            return SUPPORTS_BUNDLING_INSERT.get();
        }

        @Override
        public boolean supportsBundlingPickup() {
            return SUPPORTS_BUNDLING_PICKUP.get();
        }

        @Override
        public boolean supportsBundlingTransfer() {
            return SUPPORTS_BUNDLING_TRANSFER.get();
        }

        @Override
        public boolean supportsBundlingExtract() {
            return SUPPORTS_BUNDLING_EXTRACT.get();
        }

        @Override
        public boolean supportsMouseDragged() {
            return SUPPORTS_MOUSE_DRAGGED.get();
        }

        @Override
        public boolean enderChestBundlingInsert() {
            return ENDER_CHEST_BUNDLING_INSERT.get();
        }

        @Override
        public boolean enderChestBundlingPickup() {
            return ENDER_CHEST_BUNDLING_PICKUP.get();
        }

        @Override
        public boolean enderChestBundlingExtract() {
            return ENDER_CHEST_BUNDLING_EXTRACT.get();
        }

        @Override
        public boolean enderChestMouseDragged() {
            return ENDER_CHEST_MOUSE_DRAGGED.get();
        }

        @Override
        public boolean openSettingsKeyEnabled() {
            return OPEN_SETTINGS_KEY_ENABLED.get();
        }

        @Override
        public boolean quickShulkerBox() {
            return QUICK_SHULKER_BOX.get();
        }

        @Override
        public boolean quickCraftingTables() {
            return QUICK_CRAFTING_TABLE.get();
        }

        @Override
        public boolean quickStonecutter() {
            return QUICK_STONECUTTER.get();
        }

        @Override
        public boolean quickEnderChest() {
            return QUICK_ENDER_CHEST.get();
        }

        @Override
        public boolean quickAnvil() {
            return QUICK_ANVIL.get();
        }
    }
}
