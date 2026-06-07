package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.neoforge.network.NeoForgeQuickShulkerNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class NeoForgeEnderChestMenu extends ChestMenu implements NeoForgeQuickOpenMenu {
    private static final int ENDER_CHEST_SLOT_COUNT = 27;

    private final NeoForgeShulkerSessionManager sessionManager;
    private final Inventory playerInventory;
    private final Container enderChestInventory;
    private final HostSlotRef hostSlotRef;
    private final String sessionId;
    private final int lockedMenuSlotIndex;
    private final ItemStack[] lastSyncedContents;
    private boolean hostInvalidated;

    public NeoForgeEnderChestMenu(
        int containerId,
        Inventory inventory,
        Container enderChestInventory,
        NeoForgeShulkerSessionManager sessionManager,
        HostSlotRef hostSlotRef,
        String sessionId
    ) {
        super(MenuType.GENERIC_9x3, containerId, inventory, enderChestInventory, 3);
        this.sessionManager = sessionManager;
        this.playerInventory = inventory;
        this.enderChestInventory = enderChestInventory;
        this.hostSlotRef = hostSlotRef;
        this.sessionId = sessionId;
        this.lockedMenuSlotIndex = findLockedMenuSlotIndex();
        this.lastSyncedContents = captureCurrentContents();
    }

    @Override
    public boolean stillValid(Player player) {
        return !hostInvalidated && super.stillValid(player);
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
        if (isLockedMenuSlot(index)) {
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, index);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(playerInventory.player instanceof ServerPlayer serverPlayer) || playerInventory.player.level().isClientSide()) {
            return;
        }
        syncChangedSlots(serverPlayer);
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

    public void sendInitialSync(ServerPlayer player) {
        NeoForgeQuickShulkerNetwork.sendEnderChestFullSync(player, sessionId, captureCurrentContents());
    }

    private boolean isLockedMenuSlot(int slotId) {
        return lockedMenuSlotIndex >= 0 && slotId == lockedMenuSlotIndex;
    }

    private int findLockedMenuSlotIndex() {
        return NeoForgeHostLockedMenuSupport.findLockedMenuSlotIndex(slots, playerInventory, hostSlotRef);
    }

    private void syncChangedSlots(ServerPlayer player) {
        int limit = Math.min(ENDER_CHEST_SLOT_COUNT, enderChestInventory.getContainerSize());
        for (int slotIndex = 0; slotIndex < limit; slotIndex++) {
            ItemStack current = enderChestInventory.getItem(slotIndex);
            if (ItemStack.matches(lastSyncedContents[slotIndex], current)) {
                continue;
            }
            ItemStack syncedCopy = current.copy();
            lastSyncedContents[slotIndex] = syncedCopy;
            NeoForgeQuickShulkerNetwork.sendEnderChestSlotSync(player, sessionId, slotIndex, syncedCopy);
        }
    }

    private ItemStack[] captureCurrentContents() {
        ItemStack[] snapshot = new ItemStack[ENDER_CHEST_SLOT_COUNT];
        int limit = Math.min(ENDER_CHEST_SLOT_COUNT, enderChestInventory.getContainerSize());
        for (int slotIndex = 0; slotIndex < limit; slotIndex++) {
            snapshot[slotIndex] = enderChestInventory.getItem(slotIndex).copy();
        }
        for (int slotIndex = limit; slotIndex < ENDER_CHEST_SLOT_COUNT; slotIndex++) {
            snapshot[slotIndex] = ItemStack.EMPTY;
        }
        return snapshot;
    }
}
