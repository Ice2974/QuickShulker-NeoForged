package com.ice2974.quickshulkerneoforged.neoforge.network;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingAction;
import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingIntent;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.HostStorageScope;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record NeoForgeShulkerBundlingPayload(
    ShulkerBundlingIntent intent,
    ItemStack cursorStack
) implements CustomPacketPayload {
    private static final int MAX_TEXT_FIELD_LENGTH = 64;

    public static final Type<NeoForgeShulkerBundlingPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(QuickShulkerConstants.MOD_ID, ShulkerBundlingIntent.CHANNEL_ID));
    public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeShulkerBundlingPayload> STREAM_CODEC =
        StreamCodec.ofMember(NeoForgeShulkerBundlingPayload::encode, NeoForgeShulkerBundlingPayload::decode);

    public static NeoForgeShulkerBundlingPayload decode(RegistryFriendlyByteBuf buf) {
        return new NeoForgeShulkerBundlingPayload(new ShulkerBundlingIntent(
            ShulkerBundlingAction.fromSerializedName(buf.readUtf(MAX_TEXT_FIELD_LENGTH)),
            new HostSlotRef(HostStorageScope.fromSerializedName(buf.readUtf(MAX_TEXT_FIELD_LENGTH)), buf.readVarInt(), buf.readVarInt()),
            buf.readVarInt(),
            buf.readLong()
        ), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(intent.action().name());
        buf.writeUtf(intent.hostSlot().scope().name());
        buf.writeVarInt(intent.hostSlot().logicalSlotIndex());
        buf.writeVarInt(intent.hostSlot().menuSlotIndex());
        buf.writeVarInt(intent.containerId());
        buf.writeLong(intent.dragId());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, cursorStack);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
