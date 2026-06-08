package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.config.QuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.common.config.QuickShulkerConfigView;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoForgeQuickShulkerConfig {
    private static final QuickShulkerConfigView DEFAULTS = QuickShulkerConfig.defaults();
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue RIGHT_CLICK_TO_OPEN;
    private static final ModConfigSpec.BooleanValue KEYBIND_IN_HAND;
    private static final ModConfigSpec.BooleanValue KEYBIND_IN_INVENTORY;
    private static final ModConfigSpec.BooleanValue RIGHT_CLICK_IN_INVENTORY;
    private static final ModConfigSpec.BooleanValue SUPPORTS_BUNDLING_INSERT;
    private static final ModConfigSpec.BooleanValue SUPPORTS_BUNDLING_PICKUP;
    private static final ModConfigSpec.BooleanValue SUPPORTS_BUNDLING_TRANSFER;
    private static final ModConfigSpec.BooleanValue SUPPORTS_BUNDLING_EXTRACT;
    private static final ModConfigSpec.BooleanValue SUPPORTS_MOUSE_DRAGGED;
    private static final ModConfigSpec.BooleanValue QUICK_SHULKER_BOX;
    private static final ModConfigSpec.BooleanValue QUICK_CRAFTING_TABLE;
    private static final ModConfigSpec.BooleanValue QUICK_STONECUTTER;
    private static final ModConfigSpec.BooleanValue QUICK_ENDER_CHEST;
    private static final ModConfigSpec.BooleanValue QUICK_ANVIL;
    private static final QuickShulkerConfigView VIEW = new ConfigView();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("activation");
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
        builder.pop();

        builder.push("interaction");
        SUPPORTS_BUNDLING_INSERT = builder
            .comment("Allow right clicking a player-inventory shulker box with a carried item to insert the carried item.")
            .define("supportsBundlingInsert", DEFAULTS.supportsBundlingInsert());
        SUPPORTS_BUNDLING_PICKUP = builder
            .comment("Allow right clicking a player-inventory item with a carried shulker box to insert the hovered item.")
            .define("supportsBundlingPickup", DEFAULTS.supportsBundlingPickup());
        SUPPORTS_BUNDLING_TRANSFER = builder
            .comment("Reserved for a future shulker interaction where right clicking a shulker box with another shulker box transfers items.")
            .define("supportsBundlingTransfer", DEFAULTS.supportsBundlingTransfer());
        SUPPORTS_BUNDLING_EXTRACT = builder
            .comment("Reserved for a future shulker interaction where right clicking an empty slot with a shulker box extracts an item.")
            .define("supportsBundlingExtract", DEFAULTS.supportsBundlingExtract());
        SUPPORTS_MOUSE_DRAGGED = builder
            .comment("Reserved for a future shulker interaction where right clicking and dragging with a shulker box performs bulk interactions.")
            .define("supportsMouseDragged", DEFAULTS.supportsMouseDragged());
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

    private static final class ConfigView implements QuickShulkerConfigView {
        @Override
        public com.ice2974.quickshulkerneoforged.common.config.KeyBindingSpec activationKey() {
            return DEFAULTS.activationKey();
        }

        @Override
        public com.ice2974.quickshulkerneoforged.common.config.KeyBindingSpec openSettingsKey() {
            return DEFAULTS.openSettingsKey();
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
        public boolean openSettingsKeyEnabled() {
            return DEFAULTS.openSettingsKeyEnabled();
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
