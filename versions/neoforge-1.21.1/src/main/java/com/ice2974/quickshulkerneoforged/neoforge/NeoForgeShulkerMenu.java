package com.ice2974.quickshulkerneoforged.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxMenu;

public final class NeoForgeShulkerMenu extends ShulkerBoxMenu {
    private final NeoForgeShulkerSessionManager sessionManager;
    private boolean hostInvalidated;

    public NeoForgeShulkerMenu(
        int containerId,
        Inventory inventory,
        ItemBackedShulkerContainer container,
        NeoForgeShulkerSessionManager sessionManager
    ) {
        super(containerId, inventory, container);
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean stillValid(Player player) {
        return !hostInvalidated && super.stillValid(player);
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
}
