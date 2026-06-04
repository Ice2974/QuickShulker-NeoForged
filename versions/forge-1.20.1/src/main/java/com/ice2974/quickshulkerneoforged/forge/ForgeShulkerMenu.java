package com.ice2974.quickshulkerneoforged.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxMenu;

public final class ForgeShulkerMenu extends ShulkerBoxMenu {
    private final ForgeShulkerSessionManager sessionManager;
    private boolean hostInvalidated;

    public ForgeShulkerMenu(int containerId, Inventory inventory, ItemBackedShulkerContainer container, ForgeShulkerSessionManager sessionManager) {
        super(containerId, inventory, container);
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean stillValid(Player player) {
        return !hostInvalidated && super.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            sessionManager.finishSession(serverPlayer, this);
        }
    }

    public void markHostInvalidated() {
        this.hostInvalidated = true;
    }
}
