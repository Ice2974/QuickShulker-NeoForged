package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ForgeEnderChestMenu extends ChestMenu implements ForgeQuickOpenMenu {
    private final ForgeShulkerSessionManager sessionManager;
    private final Inventory playerInventory;
    private final HostSlotRef hostSlotRef;
    private final int lockedMenuSlotIndex;
    private boolean hostInvalidated;

    public ForgeEnderChestMenu(
        int containerId,
        Inventory inventory,
        Container enderChestInventory,
        ForgeShulkerSessionManager sessionManager,
        HostSlotRef hostSlotRef
    ) {
        super(MenuType.GENERIC_9x3, containerId, inventory, enderChestInventory, 3);
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
        if (ForgeHostLockedMenuSupport.shouldBlockHostSlotClick(slotId, button, clickType, lockedMenuSlotIndex, hostSlotRef)) {
            ForgeHostLockedMenuSupport.syncBlockedClickState(player, this::broadcastChanges);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (ForgeHostLockedMenuSupport.isLockedHostSlot(slot, playerInventory, hostSlotRef)) {
            return false;
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        if (ForgeHostLockedMenuSupport.isLockedHostSlot(slot, playerInventory, hostSlotRef)) {
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
        return BuiltinQuickOpenables.ENDER_CHEST.id();
    }

    private boolean isLockedMenuSlot(int slotId) {
        return lockedMenuSlotIndex >= 0 && slotId == lockedMenuSlotIndex;
    }

    private int findLockedMenuSlotIndex() {
        return ForgeHostLockedMenuSupport.findLockedMenuSlotIndex(slots, playerInventory, hostSlotRef);
    }
}
