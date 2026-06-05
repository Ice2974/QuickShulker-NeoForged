package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import java.util.List;

final class NeoForgeHostLockedMenuSupport {
    private static final int PLAYER_MAIN_INVENTORY_OFFSET = 9;
    private static final int PLAYER_OFFHAND_CONTAINER_SLOT = Inventory.SLOT_OFFHAND;

    private NeoForgeHostLockedMenuSupport() {
    }

    static int findLockedMenuSlotIndex(List<Slot> slots, Inventory playerInventory, HostSlotRef hostSlotRef) {
        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            if (isLockedHostSlot(slots.get(slotIndex), playerInventory, hostSlotRef)) {
                return slotIndex;
            }
        }
        return -1;
    }

    static boolean shouldBlockHostSlotClick(int slotId, int button, ClickType clickType, int lockedMenuSlotIndex, HostSlotRef hostSlotRef) {
        if (lockedMenuSlotIndex >= 0 && slotId == lockedMenuSlotIndex) {
            return true;
        }
        if (clickType == ClickType.SWAP && button == PLAYER_OFFHAND_CONTAINER_SLOT) {
            return true;
        }
        return clickType == ClickType.SWAP && targetsLockedSwapButton(button, hostSlotRef);
    }

    static boolean isLockedHostSlot(Slot slot, Inventory playerInventory, HostSlotRef hostSlotRef) {
        if (slot == null || slot.container != playerInventory) {
            return false;
        }

        int containerSlot = slot.getContainerSlot();
        return switch (hostSlotRef.scope()) {
            case PLAYER_HOTBAR -> containerSlot == hostSlotRef.logicalSlotIndex();
            case PLAYER_MAIN_INVENTORY -> containerSlot == PLAYER_MAIN_INVENTORY_OFFSET + hostSlotRef.logicalSlotIndex();
            case PLAYER_OFFHAND -> containerSlot == PLAYER_OFFHAND_CONTAINER_SLOT;
            default -> false;
        };
    }

    static void syncBlockedClickState(Player player, Runnable broadcastChanges) {
        broadcastChanges.run();
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.sendAllDataToRemote();
        }
    }

    private static boolean targetsLockedSwapButton(int button, HostSlotRef hostSlotRef) {
        return switch (hostSlotRef.scope()) {
            case PLAYER_HOTBAR -> button == hostSlotRef.logicalSlotIndex();
            case PLAYER_OFFHAND -> button == PLAYER_OFFHAND_CONTAINER_SLOT;
            default -> false;
        };
    }
}
