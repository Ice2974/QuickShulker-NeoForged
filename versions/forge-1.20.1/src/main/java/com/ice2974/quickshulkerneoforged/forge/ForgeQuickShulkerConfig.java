package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.config.QuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.common.config.QuickShulkerConfigView;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ForgeQuickShulkerConfig {
    private static final QuickShulkerConfigView DEFAULTS = QuickShulkerConfig.defaults();
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue RIGHT_CLICK_TO_OPEN;
    private static final ForgeConfigSpec.BooleanValue KEYBIND_IN_HAND;
    private static final ForgeConfigSpec.BooleanValue KEYBIND_IN_INVENTORY;
    private static final ForgeConfigSpec.BooleanValue RIGHT_CLICK_IN_INVENTORY;
    private static final ForgeConfigSpec.BooleanValue RIGHT_CLICK_CLOSE;
    private static final ForgeConfigSpec.BooleanValue SUPPORTS_MOUSE_DRAGGED;
    private static final ForgeConfigSpec.BooleanValue QUICK_SHULKER_BOX;
    private static final QuickShulkerConfigView VIEW = new ConfigView();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
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
        RIGHT_CLICK_CLOSE = builder
            .comment("Reserved for future right-click-close behavior.")
            .define("rightClickClose", DEFAULTS.rightClickClose());
        builder.pop();

        builder.push("interaction");
        SUPPORTS_MOUSE_DRAGGED = builder
            .comment("Reserved for future dragged mouse interactions.")
            .define("supportsMouseDragged", DEFAULTS.supportsMouseDragged());
        builder.pop();

        builder.push("quickOpenables");
        QUICK_SHULKER_BOX = builder
            .comment("Enable quick-open behavior for shulker boxes.")
            .define("quickShulkerBox", DEFAULTS.quickShulkerBox());
        builder.pop();

        SPEC = builder.build();
    }

    private ForgeQuickShulkerConfig() {
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
        public boolean rightClickClose() {
            return RIGHT_CLICK_CLOSE.get();
        }

        @Override
        public boolean supportsBundlingInsert() {
            return DEFAULTS.supportsBundlingInsert();
        }

        @Override
        public boolean supportsBundlingPickup() {
            return DEFAULTS.supportsBundlingPickup();
        }

        @Override
        public boolean supportsBundlingTransfer() {
            return DEFAULTS.supportsBundlingTransfer();
        }

        @Override
        public boolean supportsBundlingExtract() {
            return DEFAULTS.supportsBundlingExtract();
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
            return DEFAULTS.quickCraftingTables();
        }

        @Override
        public boolean quickStonecutter() {
            return DEFAULTS.quickStonecutter();
        }

        @Override
        public boolean quickEnderChest() {
            return DEFAULTS.quickEnderChest();
        }

        @Override
        public boolean quickAnvil() {
            return DEFAULTS.quickAnvil();
        }
    }
}
