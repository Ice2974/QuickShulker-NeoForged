package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.HostStorageScope;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ForgeHostSlotResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeHostSlotResolver.class);
    private static final String CREATIVE_SLOT_WRAPPER_CLASS =
        "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper";
    private static final int PLAYER_HOTBAR_SIZE = 9;
    private static final int PLAYER_MAIN_INVENTORY_SIZE = 27;
    private static final int PLAYER_MAIN_INVENTORY_OFFSET = 9;
    private static final int PLAYER_OFFHAND_CONTAINER_SLOT = 40;

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

    public static boolean isPlayerInventorySlotRef(HostSlotRef slotRef) {
        return switch (slotRef.scope()) {
            case PLAYER_HOTBAR -> isLogicalSlotInRange(slotRef.logicalSlotIndex(), PLAYER_HOTBAR_SIZE);
            case PLAYER_MAIN_INVENTORY -> isLogicalSlotInRange(slotRef.logicalSlotIndex(), PLAYER_MAIN_INVENTORY_SIZE);
            case PLAYER_OFFHAND -> slotRef.logicalSlotIndex() == 0;
            default -> false;
        };
    }

    public static Optional<HostSlotRef> forPlayerInventorySlot(Player player, Slot slot, int menuSlotIndex) {
        if (slot == null) {
            return Optional.empty();
        }

        Slot effectiveSlot = unwrapSlot(slot);
        if (effectiveSlot.container != player.getInventory()) {
            return Optional.empty();
        }

        int containerSlot = effectiveSlot.getSlotIndex();
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
        if (containerSlot == PLAYER_OFFHAND_CONTAINER_SLOT) {
            return Optional.of(new HostSlotRef(HostStorageScope.PLAYER_OFFHAND, 0, menuSlotIndex));
        }
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
            LOGGER.debug("Failed to unwrap creative slot wrapper for shulker bundling.", exception);
        }
        return slot;
    }

    private static boolean isLogicalSlotInRange(int logicalSlotIndex, int size) {
        return logicalSlotIndex >= 0 && logicalSlotIndex < size;
    }
}
