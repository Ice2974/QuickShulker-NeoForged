package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public final class NeoForgeAnvilMenu extends AnvilMenu implements NeoForgeQuickOpenMenu {
    private final NeoForgeShulkerSessionManager sessionManager;
    private final Inventory playerInventory;
    private final HostSlotRef hostSlotRef;
    private final HostBackedAnvilAccess anvilAccess;
    private final int lockedMenuSlotIndex;
    private boolean hostInvalidated;

    public NeoForgeAnvilMenu(int containerId, Inventory inventory, NeoForgeShulkerSessionManager sessionManager, HostSlotRef hostSlotRef) {
        this(containerId, inventory, sessionManager, hostSlotRef, new HostBackedAnvilAccess(inventory.player, hostSlotRef));
    }

    private NeoForgeAnvilMenu(
        int containerId,
        Inventory inventory,
        NeoForgeShulkerSessionManager sessionManager,
        HostSlotRef hostSlotRef,
        HostBackedAnvilAccess anvilAccess
    ) {
        super(containerId, inventory, anvilAccess);
        this.sessionManager = sessionManager;
        this.playerInventory = inventory;
        this.hostSlotRef = hostSlotRef;
        this.anvilAccess = anvilAccess;
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
        anvilAccess.beginRemoving();
        try {
            super.removed(player);
        } finally {
            anvilAccess.finishRemoving();
        }
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
        return BuiltinQuickOpenables.ANVIL.id();
    }

    private static final class HostBackedAnvilAccess implements ContainerLevelAccess {
        private final Player player;
        private final HostSlotRef hostSlotRef;
        private boolean removing;

        private HostBackedAnvilAccess(Player player, HostSlotRef hostSlotRef) {
            this.player = player;
            this.hostSlotRef = hostSlotRef;
        }

        @Override
        public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> function) {
            return Optional.of(function.apply(player.level(), player.blockPosition()));
        }

        @Override
        public void execute(BiConsumer<Level, BlockPos> consumer) {
            Level level = player.level();
            BlockPos pos = player.blockPosition();
            if (removing) {
                consumer.accept(level, pos);
                return;
            }

            ItemStack hostStack = NeoForgeHostSlotResolver.resolve(player, hostSlotRef);
            if (!player.isCreative() && hostStack.is(ItemTags.ANVIL) && player.getRandom().nextFloat() < 0.12F) {
                if (hostStack.is(Items.ANVIL)) {
                    NeoForgeHostSlotResolver.set(player, hostSlotRef, new ItemStack(Items.CHIPPED_ANVIL));
                    level.levelEvent(LevelEvent.SOUND_ANVIL_USED, pos, 0);
                    return;
                }
                if (hostStack.is(Items.CHIPPED_ANVIL)) {
                    NeoForgeHostSlotResolver.set(player, hostSlotRef, new ItemStack(Items.DAMAGED_ANVIL));
                    level.levelEvent(LevelEvent.SOUND_ANVIL_USED, pos, 0);
                    return;
                }
                NeoForgeHostSlotResolver.set(player, hostSlotRef, ItemStack.EMPTY);
                level.levelEvent(LevelEvent.SOUND_ANVIL_BROKEN, pos, 0);
                return;
            }
            level.levelEvent(LevelEvent.SOUND_ANVIL_USED, pos, 0);
        }

        private void beginRemoving() {
            this.removing = true;
        }

        private void finishRemoving() {
            this.removing = false;
        }
    }
}
