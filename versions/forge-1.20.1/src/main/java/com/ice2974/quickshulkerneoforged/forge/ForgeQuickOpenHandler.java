package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.network.OpenHostItemIntent;
import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.HostItemReference;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenRequest;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenTrigger;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenableType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ForgeQuickOpenHandler {
    private ForgeQuickOpenHandler() {
    }

    public static QuickOpenRequest createRequest(String requestedTypeId, HostSlotRef hostSlot, QuickOpenTrigger trigger) {
        return new QuickOpenRequest(
            requestedTypeId,
            hostSlot,
            trigger,
            true,
            false
        );
    }

    public static void handle(ServerPlayer player, OpenHostItemIntent intent, ForgeShulkerSessionManager sessionManager) {
        if (!ForgeQuickShulkerConfig.view().allowsTrigger(intent.trigger())) {
            return;
        }

        QuickOpenableType requestedType = ForgeQuickOpenRegistry.registry()
            .findType(intent.requestedTypeId())
            .filter(type -> isSupportedType(type.id()))
            .orElse(null);
        if (requestedType == null || !ForgeQuickShulkerConfig.view().isEnabled(requestedType)) {
            return;
        }

        ItemStack hostStack = ForgeHostSlotResolver.resolve(player, intent.hostSlot());
        if (hostStack.isEmpty() || hostStack.getCount() != 1) {
            return;
        }

        ForgeQuickOpenRegistry.registry()
            .findTypeForItem(ForgeItemSnapshots.snapshot(hostStack).itemKey())
            .filter(type -> isSupportedType(type.id()))
            .filter(type -> type.id().equals(requestedType.id()))
            .ifPresent(type -> {
                HostItemReference hostItemReference = new HostItemReference(
                    type.id(),
                    intent.hostSlot(),
                    ForgeItemSnapshots.snapshot(hostStack.copy())
                );
                if (sessionManager.validateCurrentHost(player, hostItemReference).valid()) {
                    sessionManager.open(player, hostItemReference, intent.trigger());
                }
            });
    }

    private static boolean isSupportedType(String typeId) {
        return BuiltinQuickOpenables.SHULKER_BOX.id().equals(typeId)
            || BuiltinQuickOpenables.ENDER_CHEST.id().equals(typeId);
    }
}
