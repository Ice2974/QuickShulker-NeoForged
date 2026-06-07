package com.ice2974.quickshulkerneoforged.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record ForgeEnderChestSlotSyncPacket(
    String sessionId,
    int slotIndex,
    ItemStack stack
) {
    private static final int MAX_TEXT_FIELD_LENGTH = 64;

    public static ForgeEnderChestSlotSyncPacket decode(FriendlyByteBuf buf) {
        return new ForgeEnderChestSlotSyncPacket(
            buf.readUtf(MAX_TEXT_FIELD_LENGTH),
            buf.readVarInt(),
            buf.readItem()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(sessionId);
        buf.writeVarInt(slotIndex);
        buf.writeItem(stack);
    }
}
