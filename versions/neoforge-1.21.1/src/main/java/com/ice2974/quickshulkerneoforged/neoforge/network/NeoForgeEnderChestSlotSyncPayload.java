package com.ice2974.quickshulkerneoforged.neoforge.network;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.common.network.EnderChestSlotSyncIntent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record NeoForgeEnderChestSlotSyncPayload(
    String sessionId,
    int slotIndex,
    ItemStack stack
) implements CustomPacketPayload {
    private static final int MAX_TEXT_FIELD_LENGTH = 64;

    public static final Type<NeoForgeEnderChestSlotSyncPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(QuickShulkerConstants.MOD_ID, EnderChestSlotSyncIntent.CHANNEL_ID));
    public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeEnderChestSlotSyncPayload> STREAM_CODEC =
        StreamCodec.ofMember(NeoForgeEnderChestSlotSyncPayload::encode, NeoForgeEnderChestSlotSyncPayload::decode);

    public static NeoForgeEnderChestSlotSyncPayload decode(RegistryFriendlyByteBuf buf) {
        return new NeoForgeEnderChestSlotSyncPayload(
            buf.readUtf(MAX_TEXT_FIELD_LENGTH),
            buf.readVarInt(),
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        );
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(sessionId);
        buf.writeVarInt(slotIndex);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
