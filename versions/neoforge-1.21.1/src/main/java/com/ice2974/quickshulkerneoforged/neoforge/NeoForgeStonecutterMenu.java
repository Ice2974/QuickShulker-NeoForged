package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;

public final class NeoForgeStonecutterMenu extends StonecutterMenu implements NeoForgeQuickOpenMenu {
    private final NeoForgeShulkerSessionManager sessionManager;
    private final Inventory playerInventory;
    private final HostSlotRef hostSlotRef;
    private final int lockedMenuSlotIndex;
    private boolean hostInvalidated;

    public NeoForgeStonecutterMenu(int containerId, Inventory inventory, NeoForgeShulkerSessionManager sessionManager, HostSlotRef hostSlotRef) {
        super(containerId, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
        this.sessionManager = sessionManager;
        this.playerInventory = inventory;
        this.hostSlotRef = hostSlotRef;
        this.lockedMenuSlotIndex = NeoForgeHostLockedMenuSupport.findLockedMenuSlotIndex(slots, playerInventory, hostSlotRef);
    }

    @Override
    public boolean stillValid(Player player) {
        return !hostInvalidated;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (NeoForgeHostLockedMenuSupport.shouldBlockHostSlotClick(slotId, button, clickType, lockedMenuSlotIndex, hostSlotRef)) {
            NeoForgeHostLockedMenuSupport.syncBlockedClickState(player, this::broadcastChanges);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (NeoForgeHostLockedMenuSupport.isLockedHostSlot(slot, playerInventory, hostSlotRef)) {
            return false;
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        if (NeoForgeHostLockedMenuSupport.isLockedHostSlot(slot, playerInventory, hostSlotRef)) {
            return false;
        }
        return super.canDragTo(slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (lockedMenuSlotIndex >= 0 && index == lockedMenuSlotIndex) {
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

    @Override
    public void markHostInvalidated() {
        this.hostInvalidated = true;
    }

    @Override
    public HostSlotRef hostSlotRef() {
        return hostSlotRef;
    }

    @Override
    public String quickOpenableTypeId() {
        return BuiltinQuickOpenables.STONECUTTER.id();
    }
}
