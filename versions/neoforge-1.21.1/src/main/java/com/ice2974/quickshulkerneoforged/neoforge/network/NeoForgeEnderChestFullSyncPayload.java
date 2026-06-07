package com.ice2974.quickshulkerneoforged.neoforge.network;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.common.network.EnderChestFullSyncIntent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record NeoForgeEnderChestFullSyncPayload(
    String sessionId,
    List<ItemStack> stacks
) implements CustomPacketPayload {
    private static final int MAX_TEXT_FIELD_LENGTH = 64;

    public static final Type<NeoForgeEnderChestFullSyncPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(QuickShulkerConstants.MOD_ID, EnderChestFullSyncIntent.CHANNEL_ID));
    public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeEnderChestFullSyncPayload> STREAM_CODEC =
        StreamCodec.ofMember(NeoForgeEnderChestFullSyncPayload::encode, NeoForgeEnderChestFullSyncPayload::decode);

    public NeoForgeEnderChestFullSyncPayload {
        stacks = List.copyOf(stacks);
    }

    public static NeoForgeEnderChestFullSyncPayload decode(RegistryFriendlyByteBuf buf) {
        String sessionId = buf.readUtf(MAX_TEXT_FIELD_LENGTH);
        int slotCount = buf.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(slotCount);
        for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
            stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return new NeoForgeEnderChestFullSyncPayload(sessionId, stacks);
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(sessionId);
        buf.writeVarInt(stacks.size());
        for (ItemStack stack : stacks) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
