package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.HostStorageScope;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class NeoForgeHostSlotResolver {
    private NeoForgeHostSlotResolver() {
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
            case PLAYER_HOTBAR -> isLogicalSlotInRange(slotRef.logicalSlotIndex(), 9)
                ? inventory.getItem(slotRef.logicalSlotIndex())
                : ItemStack.EMPTY;
            case PLAYER_MAIN_INVENTORY -> isLogicalSlotInRange(slotRef.logicalSlotIndex(), 27)
                ? inventory.getItem(9 + slotRef.logicalSlotIndex())
                : ItemStack.EMPTY;
            case PLAYER_OFFHAND -> inventory.offhand.get(0);
            default -> ItemStack.EMPTY;
        };
    }

    private static boolean isLogicalSlotInRange(int logicalSlotIndex, int size) {
        return logicalSlotIndex >= 0 && logicalSlotIndex < size;
    }
}
