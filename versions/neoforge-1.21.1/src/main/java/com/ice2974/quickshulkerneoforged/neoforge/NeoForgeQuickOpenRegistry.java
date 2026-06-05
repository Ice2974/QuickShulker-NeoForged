package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.QuickShulkerCommon;
import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class NeoForgeQuickOpenRegistry {
    private static final Item[] SHULKER_BOX_ITEMS = new Item[] {
        Items.SHULKER_BOX,
        Items.WHITE_SHULKER_BOX,
        Items.ORANGE_SHULKER_BOX,
        Items.MAGENTA_SHULKER_BOX,
        Items.LIGHT_BLUE_SHULKER_BOX,
        Items.YELLOW_SHULKER_BOX,
        Items.LIME_SHULKER_BOX,
        Items.PINK_SHULKER_BOX,
        Items.GRAY_SHULKER_BOX,
        Items.LIGHT_GRAY_SHULKER_BOX,
        Items.CYAN_SHULKER_BOX,
        Items.PURPLE_SHULKER_BOX,
        Items.BLUE_SHULKER_BOX,
        Items.BROWN_SHULKER_BOX,
        Items.GREEN_SHULKER_BOX,
        Items.RED_SHULKER_BOX,
        Items.BLACK_SHULKER_BOX
    };

    private static final QuickOpenableRegistry REGISTRY = createRegistry();

    private NeoForgeQuickOpenRegistry() {
    }

    public static QuickOpenableRegistry registry() {
        return REGISTRY;
    }

    private static QuickOpenableRegistry createRegistry() {
        QuickOpenableRegistry registry = QuickShulkerCommon.createDefaultQuickOpenableRegistry();
        for (Item item : SHULKER_BOX_ITEMS) {
            registry.bindItem(BuiltInRegistries.ITEM.getKey(item).toString(), BuiltinQuickOpenables.SHULKER_BOX.id());
        }
        registry.bindItem(BuiltInRegistries.ITEM.getKey(Items.ENDER_CHEST).toString(), BuiltinQuickOpenables.ENDER_CHEST.id());
        return registry;
    }
}
