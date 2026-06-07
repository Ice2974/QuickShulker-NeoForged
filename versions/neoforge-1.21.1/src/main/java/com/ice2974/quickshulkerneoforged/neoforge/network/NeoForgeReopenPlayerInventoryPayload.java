package com.ice2974.quickshulkerneoforged.neoforge.network;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.common.network.ReopenPlayerInventoryIntent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NeoForgeReopenPlayerInventoryPayload(ReopenPlayerInventoryIntent intent) implements CustomPacketPayload {
    private static final int MAX_TEXT_FIELD_LENGTH = 64;

    public static final Type<NeoForgeReopenPlayerInventoryPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(QuickShulkerConstants.MOD_ID, ReopenPlayerInventoryIntent.CHANNEL_ID));
    public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeReopenPlayerInventoryPayload> STREAM_CODEC =
        StreamCodec.ofMember(NeoForgeReopenPlayerInventoryPayload::encode, NeoForgeReopenPlayerInventoryPayload::decode);

    public static NeoForgeReopenPlayerInventoryPayload decode(RegistryFriendlyByteBuf buf) {
        return new NeoForgeReopenPlayerInventoryPayload(new ReopenPlayerInventoryIntent(buf.readUtf(MAX_TEXT_FIELD_LENGTH)));
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(intent.sessionId());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
