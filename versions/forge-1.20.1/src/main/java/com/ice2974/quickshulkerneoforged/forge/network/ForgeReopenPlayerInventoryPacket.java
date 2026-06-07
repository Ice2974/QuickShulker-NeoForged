package com.ice2974.quickshulkerneoforged.forge.network;

import com.ice2974.quickshulkerneoforged.common.network.ReopenPlayerInventoryIntent;
import net.minecraft.network.FriendlyByteBuf;

public record ForgeReopenPlayerInventoryPacket(ReopenPlayerInventoryIntent intent) {
    private static final int MAX_TEXT_FIELD_LENGTH = 64;

    public static ForgeReopenPlayerInventoryPacket decode(FriendlyByteBuf buf) {
        return new ForgeReopenPlayerInventoryPacket(new ReopenPlayerInventoryIntent(buf.readUtf(MAX_TEXT_FIELD_LENGTH)));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(intent.sessionId());
    }
}
