package com.ice2974.quickshulkerneoforged.neoforge.network;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.common.network.OpenHostItemIntent;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.HostStorageScope;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenTrigger;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NeoForgeOpenHostItemPayload(OpenHostItemIntent intent) implements CustomPacketPayload {
    public static final Type<NeoForgeOpenHostItemPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(QuickShulkerConstants.MOD_ID, OpenHostItemIntent.CHANNEL_ID));
    public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeOpenHostItemPayload> STREAM_CODEC =
        StreamCodec.ofMember(NeoForgeOpenHostItemPayload::encode, NeoForgeOpenHostItemPayload::decode);

    public static NeoForgeOpenHostItemPayload decode(RegistryFriendlyByteBuf buf) {
        return new NeoForgeOpenHostItemPayload(new OpenHostItemIntent(
            buf.readUtf(),
            new HostSlotRef(HostStorageScope.valueOf(buf.readUtf()), buf.readVarInt(), buf.readVarInt()),
            QuickOpenTrigger.valueOf(buf.readUtf())
        ));
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(intent.requestedTypeId());
        buf.writeUtf(intent.hostSlot().scope().name());
        buf.writeVarInt(intent.hostSlot().logicalSlotIndex());
        buf.writeVarInt(intent.hostSlot().menuSlotIndex());
        buf.writeUtf(intent.trigger().name());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
