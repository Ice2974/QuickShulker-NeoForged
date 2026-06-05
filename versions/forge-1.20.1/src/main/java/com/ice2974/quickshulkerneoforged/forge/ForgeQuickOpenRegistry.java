package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenableRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgeQuickOpenRegistry {
    private static final QuickOpenableRegistry REGISTRY = createRegistry();

    private ForgeQuickOpenRegistry() {
    }

    private static QuickOpenableRegistry createRegistry() {
        QuickOpenableRegistry registry = BuiltinQuickOpenables.createDefaultRegistry();
        bind(registry, Items.SHULKER_BOX);
        bind(registry, Items.WHITE_SHULKER_BOX);
        bind(registry, Items.ORANGE_SHULKER_BOX);
        bind(registry, Items.MAGENTA_SHULKER_BOX);
        bind(registry, Items.LIGHT_BLUE_SHULKER_BOX);
        bind(registry, Items.YELLOW_SHULKER_BOX);
        bind(registry, Items.LIME_SHULKER_BOX);
        bind(registry, Items.PINK_SHULKER_BOX);
        bind(registry, Items.GRAY_SHULKER_BOX);
        bind(registry, Items.LIGHT_GRAY_SHULKER_BOX);
        bind(registry, Items.CYAN_SHULKER_BOX);
        bind(registry, Items.PURPLE_SHULKER_BOX);
        bind(registry, Items.BLUE_SHULKER_BOX);
        bind(registry, Items.BROWN_SHULKER_BOX);
        bind(registry, Items.GREEN_SHULKER_BOX);
        bind(registry, Items.RED_SHULKER_BOX);
        bind(registry, Items.BLACK_SHULKER_BOX);
        bind(registry, Items.ENDER_CHEST, BuiltinQuickOpenables.ENDER_CHEST.id());
        bind(registry, Items.CRAFTING_TABLE, BuiltinQuickOpenables.CRAFTING_TABLE.id());
        bind(registry, Items.STONECUTTER, BuiltinQuickOpenables.STONECUTTER.id());
        bind(registry, Items.ANVIL, BuiltinQuickOpenables.ANVIL.id());
        bind(registry, Items.CHIPPED_ANVIL, BuiltinQuickOpenables.ANVIL.id());
        bind(registry, Items.DAMAGED_ANVIL, BuiltinQuickOpenables.ANVIL.id());
        return registry;
    }

    private static void bind(QuickOpenableRegistry registry, Item item) {
        bind(registry, item, BuiltinQuickOpenables.SHULKER_BOX.id());
    }

    private static void bind(QuickOpenableRegistry registry, Item item, String typeId) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id != null) {
            registry.bindItem(id.toString(), typeId);
        }
    }

    public static QuickOpenableRegistry registry() {
        return REGISTRY;
    }
}
