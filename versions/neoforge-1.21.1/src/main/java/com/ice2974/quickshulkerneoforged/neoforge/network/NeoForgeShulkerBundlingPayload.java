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

public record NeoForgeShulkerBundlingPayload(ShulkerBundlingIntent intent) implements CustomPacketPayload {
    private static final int MAX_TEXT_FIELD_LENGTH = 64;

    public static final Type<NeoForgeShulkerBundlingPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(QuickShulkerConstants.MOD_ID, ShulkerBundlingIntent.CHANNEL_ID));
    public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeShulkerBundlingPayload> STREAM_CODEC =
        StreamCodec.ofMember(NeoForgeShulkerBundlingPayload::encode, NeoForgeShulkerBundlingPayload::decode);

    public static NeoForgeShulkerBundlingPayload decode(RegistryFriendlyByteBuf buf) {
        return new NeoForgeShulkerBundlingPayload(new ShulkerBundlingIntent(
            ShulkerBundlingAction.valueOf(buf.readUtf(MAX_TEXT_FIELD_LENGTH)),
            new HostSlotRef(HostStorageScope.valueOf(buf.readUtf(MAX_TEXT_FIELD_LENGTH)), buf.readVarInt(), buf.readVarInt())
        ));
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(intent.action().name());
        buf.writeUtf(intent.hostSlot().scope().name());
        buf.writeVarInt(intent.hostSlot().logicalSlotIndex());
        buf.writeVarInt(intent.hostSlot().menuSlotIndex());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
