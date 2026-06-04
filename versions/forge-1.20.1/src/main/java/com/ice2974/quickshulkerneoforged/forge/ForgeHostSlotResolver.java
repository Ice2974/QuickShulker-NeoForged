package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.HostStorageScope;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ForgeHostSlotResolver {
    private ForgeHostSlotResolver() {
    }

    public static HostSlotRef forHand(Player player, InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> new HostSlotRef(HostStorageScope.PLAYER_HOTBAR, player.getInventory().selected, -1);
            case OFF_HAND -> new HostSlotRef(HostStorageScope.PLAYER_OFFHAND, 0, -1);
        };
    }

    public static ItemStack resolve(Player player, HostSlotRef slotRef) {
        Inventory inventory = player.getInventory();
        return switch (slotRef.scope()) {
            case PLAYER_HOTBAR -> inventory.getItem(slotRef.logicalSlotIndex());
            case PLAYER_MAIN_INVENTORY -> inventory.getItem(9 + slotRef.logicalSlotIndex());
            case PLAYER_OFFHAND -> inventory.offhand.get(0);
            default -> ItemStack.EMPTY;
        };
    }
}
