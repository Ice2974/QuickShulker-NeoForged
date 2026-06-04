package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.HostStorageScope;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class NeoForgeHostSlotResolver {
    private static final int PLAYER_HOTBAR_SIZE = 9;
    private static final int PLAYER_MAIN_INVENTORY_SIZE = 27;
    private static final int PLAYER_MAIN_INVENTORY_OFFSET = 9;

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
            case PLAYER_HOTBAR -> isLogicalSlotInRange(slotRef.logicalSlotIndex(), PLAYER_HOTBAR_SIZE)
                ? inventory.getItem(slotRef.logicalSlotIndex())
                : ItemStack.EMPTY;
            case PLAYER_MAIN_INVENTORY -> isLogicalSlotInRange(slotRef.logicalSlotIndex(), PLAYER_MAIN_INVENTORY_SIZE)
                ? inventory.getItem(PLAYER_MAIN_INVENTORY_OFFSET + slotRef.logicalSlotIndex())
                : ItemStack.EMPTY;
            case PLAYER_OFFHAND -> inventory.offhand.get(0);
            default -> ItemStack.EMPTY;
        };
    }

    public static Optional<HostSlotRef> forPlayerInventorySlot(Player player, AbstractContainerMenu menu, Slot slot) {
        if (slot == null || menu == null) {
            return Optional.empty();
        }

        int menuSlotIndex = slot.index;
        int containerSlot = slot.getContainerSlot();
        if (menu instanceof InventoryMenu) {
            if (menuSlotIndex == InventoryMenu.SHIELD_SLOT && containerSlot == Inventory.SLOT_OFFHAND) {
                return Optional.of(new HostSlotRef(HostStorageScope.PLAYER_OFFHAND, 0, menuSlotIndex));
            }
            if (menuSlotIndex >= InventoryMenu.USE_ROW_SLOT_START
                && menuSlotIndex < InventoryMenu.USE_ROW_SLOT_END
                && containerSlot >= 0
                && containerSlot < PLAYER_HOTBAR_SIZE) {
                return Optional.of(new HostSlotRef(HostStorageScope.PLAYER_HOTBAR, containerSlot, menuSlotIndex));
            }
            if (menuSlotIndex >= InventoryMenu.INV_SLOT_START
                && menuSlotIndex < InventoryMenu.INV_SLOT_END
                && containerSlot >= PLAYER_MAIN_INVENTORY_OFFSET
                && containerSlot < PLAYER_MAIN_INVENTORY_OFFSET + PLAYER_MAIN_INVENTORY_SIZE) {
                return Optional.of(new HostSlotRef(
                    HostStorageScope.PLAYER_MAIN_INVENTORY,
                    containerSlot - PLAYER_MAIN_INVENTORY_OFFSET,
                    menuSlotIndex
                ));
            }
            return Optional.empty();
        }

        if (slot.container != player.getInventory()) {
            return Optional.empty();
        }

        if (containerSlot >= 0 && containerSlot < PLAYER_HOTBAR_SIZE) {
            return Optional.of(new HostSlotRef(HostStorageScope.PLAYER_HOTBAR, containerSlot, menuSlotIndex));
        }
        if (containerSlot >= PLAYER_MAIN_INVENTORY_OFFSET
            && containerSlot < PLAYER_MAIN_INVENTORY_OFFSET + PLAYER_MAIN_INVENTORY_SIZE) {
            return Optional.of(new HostSlotRef(
                HostStorageScope.PLAYER_MAIN_INVENTORY,
                containerSlot - PLAYER_MAIN_INVENTORY_OFFSET,
                menuSlotIndex
            ));
        }
        if (containerSlot == Inventory.SLOT_OFFHAND) {
            return Optional.of(new HostSlotRef(HostStorageScope.PLAYER_OFFHAND, 0, menuSlotIndex));
        }
        return Optional.empty();
    }

    private static boolean isLogicalSlotInRange(int logicalSlotIndex, int size) {
        return logicalSlotIndex >= 0 && logicalSlotIndex < size;
    }
}
