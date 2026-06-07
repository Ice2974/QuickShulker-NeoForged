package com.ice2974.quickshulkerneoforged.forge.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record ForgeEnderChestFullSyncPacket(
    String sessionId,
    List<ItemStack> stacks
) {
    private static final int MAX_TEXT_FIELD_LENGTH = 64;

    public ForgeEnderChestFullSyncPacket {
        stacks = List.copyOf(stacks);
    }

    public static ForgeEnderChestFullSyncPacket decode(FriendlyByteBuf buf) {
        String sessionId = buf.readUtf(MAX_TEXT_FIELD_LENGTH);
        int slotCount = buf.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(slotCount);
        for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
            stacks.add(buf.readItem());
        }
        return new ForgeEnderChestFullSyncPacket(sessionId, stacks);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(sessionId);
        buf.writeVarInt(stacks.size());
        for (ItemStack stack : stacks) {
            buf.writeItem(stack);
        }
    }
}
