package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ForgeShulkerMenu extends ShulkerBoxMenu implements ForgeQuickOpenMenu {
    private static final int PLAYER_MAIN_INVENTORY_OFFSET = 9;
    private static final int PLAYER_OFFHAND_CONTAINER_SLOT = 40;

    private final ForgeShulkerSessionManager sessionManager;
    private final Inventory playerInventory;
    private final HostSlotRef hostSlotRef;
    private final int lockedMenuSlotIndex;
    private boolean hostInvalidated;

    public ForgeShulkerMenu(
        int containerId,
        Inventory inventory,
        ItemBackedShulkerContainer container,
        ForgeShulkerSessionManager sessionManager,
        HostSlotRef hostSlotRef
    ) {
        super(containerId, inventory, container);
        this.sessionManager = sessionManager;
        this.playerInventory = inventory;
        this.hostSlotRef = hostSlotRef;
        this.lockedMenuSlotIndex = findLockedMenuSlotIndex();
    }

    @Override
    public boolean stillValid(Player player) {
        return !hostInvalidated && super.stillValid(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (shouldBlockHostSlotClick(slotId, button, clickType)) {
            syncBlockedClickState(player);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (isLockedHostSlot(slot)) {
            return false;
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        if (isLockedHostSlot(slot)) {
            return false;
        }
        return super.canDragTo(slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (isLockedMenuSlot(index)) {
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, index);
    }

    @Override
    public void removed(Player player) {
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            sessionManager.finishSession(serverPlayer, this);
        }
        super.removed(player);
    }

    public void markHostInvalidated() {
        this.hostInvalidated = true;
    }

    @Override
    public HostSlotRef hostSlotRef() {
        return hostSlotRef;
    }

    @Override
    public String quickOpenableTypeId() {
        return BuiltinQuickOpenables.SHULKER_BOX.id();
    }

    private boolean shouldBlockHostSlotClick(int slotId, int button, ClickType clickType) {
        if (isLockedMenuSlot(slotId)) {
            return true;
        }
        if (clickType == ClickType.SWAP && isOffhandSwapButton(button)) {
            return true;
        }
        return clickType == ClickType.SWAP && targetsLockedSwapButton(button);
    }

    private boolean isLockedMenuSlot(int slotId) {
        return lockedMenuSlotIndex >= 0 && slotId == lockedMenuSlotIndex;
    }

    private boolean isLockedHostSlot(Slot slot) {
        if (slot == null || slot.container != playerInventory) {
            return false;
        }

        int containerSlot = slot.getSlotIndex();
        return switch (hostSlotRef.scope()) {
            case PLAYER_HOTBAR -> containerSlot == hostSlotRef.logicalSlotIndex();
            case PLAYER_MAIN_INVENTORY -> containerSlot == PLAYER_MAIN_INVENTORY_OFFSET + hostSlotRef.logicalSlotIndex();
            case PLAYER_OFFHAND -> containerSlot == PLAYER_OFFHAND_CONTAINER_SLOT;
            default -> false;
        };
    }

    private boolean targetsLockedSwapButton(int button) {
        return switch (hostSlotRef.scope()) {
            case PLAYER_HOTBAR -> button == hostSlotRef.logicalSlotIndex();
            case PLAYER_OFFHAND -> button == PLAYER_OFFHAND_CONTAINER_SLOT;
            default -> false;
        };
    }

    private static boolean isOffhandSwapButton(int button) {
        return button == PLAYER_OFFHAND_CONTAINER_SLOT;
    }

    private void syncBlockedClickState(Player player) {
        broadcastChanges();
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.sendAllDataToRemote();
        }
    }

    private int findLockedMenuSlotIndex() {
        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            if (isLockedHostSlot(slots.get(slotIndex))) {
                return slotIndex;
            }
        }
        return -1;
    }
}
