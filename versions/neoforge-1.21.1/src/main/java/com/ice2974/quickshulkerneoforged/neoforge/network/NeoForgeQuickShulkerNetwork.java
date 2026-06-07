package com.ice2974.quickshulkerneoforged.neoforge.network;

import com.ice2974.quickshulkerneoforged.common.network.ReopenPlayerInventoryIntent;
import com.ice2974.quickshulkerneoforged.common.network.ReopenPlayerInventoryQueue;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeQuickOpenHandler;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeServices;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NeoForgeQuickShulkerNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private NeoForgeQuickShulkerNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION).playToServer(
            NeoForgeOpenHostItemPayload.TYPE,
            NeoForgeOpenHostItemPayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    NeoForgeQuickOpenHandler.handle(serverPlayer, payload.intent(), NeoForgeServices.SHULKER_SESSIONS);
                }
            }
        );
        event.registrar(PROTOCOL_VERSION).playToClient(
            NeoForgeReopenPlayerInventoryPayload.TYPE,
            NeoForgeReopenPlayerInventoryPayload.STREAM_CODEC,
            (payload, context) -> ReopenPlayerInventoryQueue.schedule(payload.intent())
        );
    }

    public static void sendOpenHostItem(NeoForgeOpenHostItemPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendReopenPlayerInventory(ServerPlayer player, ReopenPlayerInventoryIntent intent) {
        PacketDistributor.sendToPlayer(player, new NeoForgeReopenPlayerInventoryPayload(intent));
    }
}
