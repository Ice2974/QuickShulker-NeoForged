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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Optional;

public final class NeoForgeHostSlotResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoForgeHostSlotResolver.class);
    private static final String CREATIVE_SLOT_WRAPPER_CLASS =
        "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper";
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

    public static void set(Player player, HostSlotRef slotRef, ItemStack stack) {
        Inventory inventory = player.getInventory();
        switch (slotRef.scope()) {
            case PLAYER_HOTBAR -> {
                if (isLogicalSlotInRange(slotRef.logicalSlotIndex(), PLAYER_HOTBAR_SIZE)) {
                    inventory.setItem(slotRef.logicalSlotIndex(), stack);
                }
            }
            case PLAYER_MAIN_INVENTORY -> {
                if (isLogicalSlotInRange(slotRef.logicalSlotIndex(), PLAYER_MAIN_INVENTORY_SIZE)) {
                    inventory.setItem(PLAYER_MAIN_INVENTORY_OFFSET + slotRef.logicalSlotIndex(), stack);
                }
            }
            case PLAYER_OFFHAND -> inventory.offhand.set(0, stack);
            default -> {
            }
        }
    }

    public static Optional<HostSlotRef> forPlayerInventorySlot(Player player, AbstractContainerMenu menu, Slot slot) {
        if (slot == null || menu == null) {
            return Optional.empty();
        }

        Slot effectiveSlot = unwrapSlot(slot);
        int menuSlotIndex = effectiveSlot.index;
        int containerSlot = effectiveSlot.getContainerSlot();
        boolean slotUsesPlayerInventory = effectiveSlot.container == player.getInventory();
        if (slotUsesPlayerInventory && containerSlot == Inventory.SLOT_OFFHAND) {
            return Optional.of(new HostSlotRef(HostStorageScope.PLAYER_OFFHAND, 0, menuSlotIndex));
        }
        if (slotUsesPlayerInventory && containerSlot >= 0 && containerSlot < PLAYER_HOTBAR_SIZE) {
            return Optional.of(new HostSlotRef(HostStorageScope.PLAYER_HOTBAR, containerSlot, menuSlotIndex));
        }
        if (slotUsesPlayerInventory
            && containerSlot >= PLAYER_MAIN_INVENTORY_OFFSET
            && containerSlot < PLAYER_MAIN_INVENTORY_OFFSET + PLAYER_MAIN_INVENTORY_SIZE) {
            return Optional.of(new HostSlotRef(
                HostStorageScope.PLAYER_MAIN_INVENTORY,
                containerSlot - PLAYER_MAIN_INVENTORY_OFFSET,
                menuSlotIndex
            ));
        }

        if (menu instanceof InventoryMenu && menuSlotIndex == InventoryMenu.SHIELD_SLOT) {
            LOGGER.debug(
                "Falling back to InventoryMenu shield-slot offhand mapping: menuClass={}, rawSlotIndex={}, rawContainerSlot={}, slotIndex={}, containerSlot={}, containerClass={}, usesPlayerInventory={}",
                menu.getClass().getName(),
                slot.index,
                slot.getContainerSlot(),
                menuSlotIndex,
                containerSlot,
                effectiveSlot.container == null ? "<null>" : effectiveSlot.container.getClass().getName(),
                slotUsesPlayerInventory
            );
            return Optional.of(new HostSlotRef(HostStorageScope.PLAYER_OFFHAND, 0, menuSlotIndex));
        }

        LOGGER.debug(
            "Unrecognized player inventory slot for quick open: menuClass={}, rawSlotIndex={}, rawContainerSlot={}, slotIndex={}, containerSlot={}, containerClass={}, usesPlayerInventory={}, inventoryMenu={}",
            menu.getClass().getName(),
            slot.index,
            slot.getContainerSlot(),
            menuSlotIndex,
            containerSlot,
            effectiveSlot.container == null ? "<null>" : effectiveSlot.container.getClass().getName(),
            slotUsesPlayerInventory,
            menu instanceof InventoryMenu
        );
        return Optional.empty();
    }

    private static Slot unwrapSlot(Slot slot) {
        if (!slot.getClass().getName().equals(CREATIVE_SLOT_WRAPPER_CLASS)) {
            return slot;
        }

        try {
            Field targetField = slot.getClass().getDeclaredField("target");
            targetField.setAccessible(true);
            Object target = targetField.get(slot);
            if (target instanceof Slot targetSlot) {
                return targetSlot;
            }
        } catch (ReflectiveOperationException exception) {
            LOGGER.debug("Failed to unwrap creative slot wrapper for quick open.", exception);
        }
        return slot;
    }

    private static boolean isLogicalSlotInRange(int logicalSlotIndex, int size) {
        return logicalSlotIndex >= 0 && logicalSlotIndex < size;
    }
}
