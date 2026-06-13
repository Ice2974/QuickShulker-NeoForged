package com.ice2974.quickshulkerneoforged.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record ForgeCreativeCursorSyncPacket(ItemStack stack) {
    public static ForgeCreativeCursorSyncPacket decode(FriendlyByteBuf buf) {
        return new ForgeCreativeCursorSyncPacket(buf.readItem());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeItem(stack);
    }
}
