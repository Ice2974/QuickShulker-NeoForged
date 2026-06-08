package com.ice2974.quickshulkerneoforged.neoforge.network;

import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeQuickOpenHandler;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeShulkerBundlingHandler;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeServices;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NeoForgeQuickShulkerNetwork {
    private static final String CLIENT_HANDLER_CLASS =
        "com.ice2974.quickshulkerneoforged.neoforge.client.NeoForgeQuickShulkerClient";
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
        event.registrar(PROTOCOL_VERSION).playToServer(
            NeoForgeShulkerBundlingPayload.TYPE,
            NeoForgeShulkerBundlingPayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    NeoForgeShulkerBundlingHandler.handle(serverPlayer, payload.intent(), payload.cursorStack());
                }
            }
        );
        event.registrar(PROTOCOL_VERSION).playToClient(
            NeoForgeEnderChestFullSyncPayload.TYPE,
            NeoForgeEnderChestFullSyncPayload.STREAM_CODEC,
            (payload, context) -> invokeClientHandler(
                "applyEnderChestFullSync",
                new Class<?>[]{String.class, List.class},
                payload.sessionId(),
                payload.stacks()
            )
        );
        event.registrar(PROTOCOL_VERSION).playToClient(
            NeoForgeEnderChestSlotSyncPayload.TYPE,
            NeoForgeEnderChestSlotSyncPayload.STREAM_CODEC,
            (payload, context) -> invokeClientHandler(
                "applyEnderChestSlotSync",
                new Class<?>[]{String.class, int.class, ItemStack.class},
                payload.sessionId(),
                payload.slotIndex(),
                payload.stack()
            )
        );
    }

    public static void sendOpenHostItem(NeoForgeOpenHostItemPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendShulkerBundling(NeoForgeShulkerBundlingPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendEnderChestFullSync(ServerPlayer player, String sessionId, ItemStack[] stacks) {
        PacketDistributor.sendToPlayer(player, new NeoForgeEnderChestFullSyncPayload(sessionId, Arrays.asList(stacks)));
    }

    public static void sendEnderChestSlotSync(ServerPlayer player, String sessionId, int slotIndex, ItemStack stack) {
        PacketDistributor.sendToPlayer(player, new NeoForgeEnderChestSlotSyncPayload(sessionId, slotIndex, stack));
    }

    private static void invokeClientHandler(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> clientClass = Class.forName(CLIENT_HANDLER_CLASS);
            clientClass.getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException("Failed to access NeoForge client network handler: " + methodName, exception);
        } catch (InvocationTargetException exception) {
            throw new RuntimeException("NeoForge client network handler threw for method: " + methodName, exception.getCause());
        }
    }
}
