package com.ice2974.quickshulkerneoforged.forge.network;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.common.network.ReopenPlayerInventoryIntent;
import com.ice2974.quickshulkerneoforged.forge.ForgeQuickOpenHandler;
import com.ice2974.quickshulkerneoforged.forge.ForgeShulkerSessionManager;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ForgeQuickShulkerNetwork {
    private static final String CLIENT_HANDLER_CLASS =
        "com.ice2974.quickshulkerneoforged.forge.client.ForgeQuickShulkerClient";
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(ResourceLocation.fromNamespaceAndPath(QuickShulkerConstants.MOD_ID, "main"))
        .networkProtocolVersion(() -> PROTOCOL_VERSION)
        .clientAcceptedVersions(PROTOCOL_VERSION::equals)
        .serverAcceptedVersions(PROTOCOL_VERSION::equals)
        .simpleChannel();

    private ForgeQuickShulkerNetwork() {
    }

    public static void register(ForgeShulkerSessionManager sessionManager) {
        CHANNEL.messageBuilder(ForgeOpenHostItemPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ForgeOpenHostItemPacket::encode)
            .decoder(ForgeOpenHostItemPacket::decode)
            .consumerMainThread((packet, contextSupplier) -> handleOpenHostItem(packet, contextSupplier, sessionManager))
            .add();
        CHANNEL.messageBuilder(ForgeReopenPlayerInventoryPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ForgeReopenPlayerInventoryPacket::encode)
            .decoder(ForgeReopenPlayerInventoryPacket::decode)
            .consumerMainThread(ForgeQuickShulkerNetwork::handleReopenPlayerInventory)
            .add();
        CHANNEL.messageBuilder(ForgeEnderChestFullSyncPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ForgeEnderChestFullSyncPacket::encode)
            .decoder(ForgeEnderChestFullSyncPacket::decode)
            .consumerMainThread(ForgeQuickShulkerNetwork::handleEnderChestFullSync)
            .add();
        CHANNEL.messageBuilder(ForgeEnderChestSlotSyncPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ForgeEnderChestSlotSyncPacket::encode)
            .decoder(ForgeEnderChestSlotSyncPacket::decode)
            .consumerMainThread(ForgeQuickShulkerNetwork::handleEnderChestSlotSync)
            .add();
    }

    private static void handleOpenHostItem(
        ForgeOpenHostItemPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier,
        ForgeShulkerSessionManager sessionManager
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            ForgeQuickOpenHandler.handle(player, packet.intent(), sessionManager);
        }
        context.setPacketHandled(true);
    }

    public static void sendOpenHostItem(ForgeOpenHostItemPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendReopenPlayerInventory(ServerPlayer player, ReopenPlayerInventoryIntent intent) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ForgeReopenPlayerInventoryPacket(intent));
    }

    public static void sendEnderChestFullSync(ServerPlayer player, String sessionId, ItemStack[] stacks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ForgeEnderChestFullSyncPacket(sessionId, Arrays.asList(stacks)));
    }

    public static void sendEnderChestSlotSync(ServerPlayer player, String sessionId, int slotIndex, ItemStack stack) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ForgeEnderChestSlotSyncPacket(sessionId, slotIndex, stack));
    }

    private static void handleReopenPlayerInventory(
        ForgeReopenPlayerInventoryPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        invokeClientHandler(
            "schedulePendingInventoryReopenAndProcess",
            new Class<?>[]{ReopenPlayerInventoryIntent.class},
            packet.intent()
        );
        contextSupplier.get().setPacketHandled(true);
    }

    private static void handleEnderChestFullSync(
        ForgeEnderChestFullSyncPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        invokeClientHandler(
            "applyEnderChestFullSync",
            new Class<?>[]{String.class, List.class},
            packet.sessionId(),
            packet.stacks()
        );
        contextSupplier.get().setPacketHandled(true);
    }

    private static void handleEnderChestSlotSync(
        ForgeEnderChestSlotSyncPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        invokeClientHandler(
            "applyEnderChestSlotSync",
            new Class<?>[]{String.class, int.class, ItemStack.class},
            packet.sessionId(),
            packet.slotIndex(),
            packet.stack()
        );
        contextSupplier.get().setPacketHandled(true);
    }

    private static void invokeClientHandler(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> clientClass = Class.forName(CLIENT_HANDLER_CLASS);
            clientClass.getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException("Failed to access Forge client network handler: " + methodName, exception);
        } catch (InvocationTargetException exception) {
            throw new RuntimeException("Forge client network handler threw for method: " + methodName, exception.getCause());
        }
    }
}
