package com.ice2974.quickshulkerneoforged.neoforge.network;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record NeoForgeCreativeCursorSyncPayload(ItemStack stack) implements CustomPacketPayload {
    public static final Type<NeoForgeCreativeCursorSyncPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(QuickShulkerConstants.MOD_ID, "creative_cursor_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeCreativeCursorSyncPayload> STREAM_CODEC =
        StreamCodec.ofMember(NeoForgeCreativeCursorSyncPayload::encode, NeoForgeCreativeCursorSyncPayload::decode);

    public static NeoForgeCreativeCursorSyncPayload decode(RegistryFriendlyByteBuf buf) {
        return new NeoForgeCreativeCursorSyncPayload(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
