package com.ice2974.quickshulkerneoforged.forge.network;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.forge.ForgeQuickOpenHandler;
import com.ice2974.quickshulkerneoforged.forge.ForgeShulkerSessionManager;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ForgeQuickShulkerNetwork {
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
}
