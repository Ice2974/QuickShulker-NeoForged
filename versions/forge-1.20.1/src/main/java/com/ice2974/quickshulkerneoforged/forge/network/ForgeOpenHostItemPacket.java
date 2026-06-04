package com.ice2974.quickshulkerneoforged.forge.network;

import com.ice2974.quickshulkerneoforged.common.network.OpenHostItemIntent;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.HostStorageScope;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenTrigger;
import net.minecraft.network.FriendlyByteBuf;

public record ForgeOpenHostItemPacket(OpenHostItemIntent intent) {
    public static ForgeOpenHostItemPacket decode(FriendlyByteBuf buf) {
        return new ForgeOpenHostItemPacket(new OpenHostItemIntent(
            buf.readUtf(),
            new HostSlotRef(HostStorageScope.valueOf(buf.readUtf()), buf.readVarInt(), buf.readVarInt()),
            QuickOpenTrigger.valueOf(buf.readUtf())
        ));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(intent.requestedTypeId());
        buf.writeUtf(intent.hostSlot().scope().name());
        buf.writeVarInt(intent.hostSlot().logicalSlotIndex());
        buf.writeVarInt(intent.hostSlot().menuSlotIndex());
        buf.writeUtf(intent.trigger().name());
    }
}
