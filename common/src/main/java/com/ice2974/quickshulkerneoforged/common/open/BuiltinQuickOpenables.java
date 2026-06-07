package com.ice2974.quickshulkerneoforged.common.open;

public final class BuiltinQuickOpenables {
    public static final QuickOpenableType SHULKER_BOX = new QuickOpenableType(
        "shulker_box",
        QuickOpenableCategory.SHULKER_BOX,
        QuickOpenMenuKind.SHULKER_BOX,
        QuickOpenConfigGate.QUICK_SHULKER_BOX,
        true,
        true,
        true,
        true,
        true
    );

    public static final QuickOpenableType ENDER_CHEST = new QuickOpenableType(
        "ender_chest",
        QuickOpenableCategory.ENDER_CHEST,
        QuickOpenMenuKind.ENDER_CHEST,
        QuickOpenConfigGate.QUICK_ENDER_CHEST,
        true,
        true,
        true,
        true,
        true
    );

    public static final QuickOpenableType CRAFTING_TABLE = new QuickOpenableType(
        "crafting_table",
        QuickOpenableCategory.CRAFTING_TABLE,
        QuickOpenMenuKind.CRAFTING_TABLE,
        QuickOpenConfigGate.QUICK_CRAFTING_TABLE,
        true,
        false,
        true,
        true,
        true
    );

    public static final QuickOpenableType STONECUTTER = new QuickOpenableType(
        "stonecutter",
        QuickOpenableCategory.STONECUTTER,
        QuickOpenMenuKind.STONECUTTER,
        QuickOpenConfigGate.QUICK_STONECUTTER,
        true,
        false,
        true,
        true,
        true
    );

    public static final QuickOpenableType ANVIL = new QuickOpenableType(
        "anvil",
        QuickOpenableCategory.ANVIL,
        QuickOpenMenuKind.ANVIL,
        QuickOpenConfigGate.QUICK_ANVIL,
        true,
        false,
        true,
        true,
        true
    );

    private BuiltinQuickOpenables() {
    }

    public static QuickOpenableRegistry createDefaultRegistry() {
        return new QuickOpenableRegistry()
            .registerType(SHULKER_BOX)
            .registerType(ENDER_CHEST)
            .registerType(CRAFTING_TABLE)
            .registerType(STONECUTTER)
            .registerType(ANVIL);
    }
}
