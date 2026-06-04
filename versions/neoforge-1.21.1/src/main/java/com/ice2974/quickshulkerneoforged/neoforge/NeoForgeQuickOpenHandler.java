package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.network.OpenHostItemIntent;
import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.HostItemReference;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenRequest;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class NeoForgeQuickOpenHandler {
    private NeoForgeQuickOpenHandler() {
    }

    public static QuickOpenRequest createRequest(HostSlotRef hostSlot) {
        return new QuickOpenRequest(
            BuiltinQuickOpenables.SHULKER_BOX.id(),
            hostSlot,
            QuickOpenTrigger.HAND_KEYBIND,
            true,
            false
        );
    }

    public static void handle(ServerPlayer player, OpenHostItemIntent intent, NeoForgeShulkerSessionManager sessionManager) {
        if (!NeoForgeQuickShulkerConfig.view().quickShulkerBox()) {
            return;
        }
        if (!BuiltinQuickOpenables.SHULKER_BOX.id().equals(intent.requestedTypeId())) {
            return;
        }

        ItemStack hostStack = NeoForgeHostSlotResolver.resolve(player, intent.hostSlot());
        if (hostStack.isEmpty() || hostStack.getCount() != 1) {
            return;
        }

        NeoForgeQuickOpenRegistry.registry()
            .findTypeForItem(NeoForgeItemSnapshots.snapshot(hostStack).itemKey())
            .filter(type -> type.id().equals(intent.requestedTypeId()))
            .ifPresent(type -> {
                HostItemReference hostItemReference = new HostItemReference(
                    type.id(),
                    intent.hostSlot(),
                    NeoForgeItemSnapshots.snapshot(hostStack.copy())
                );
                if (sessionManager.validateCurrentHost(player, hostItemReference).valid()) {
                    sessionManager.open(player, hostItemReference);
                }
            });
    }
}
