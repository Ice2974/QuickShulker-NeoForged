package com.ice2974.quickshulkerneoforged.forge.network;

import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingAction;
import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingIntent;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.HostStorageScope;
import net.minecraft.network.FriendlyByteBuf;

public record ForgeShulkerBundlingPacket(ShulkerBundlingIntent intent) {
    private static final int MAX_TEXT_FIELD_LENGTH = 64;

    public static ForgeShulkerBundlingPacket decode(FriendlyByteBuf buf) {
        return new ForgeShulkerBundlingPacket(new ShulkerBundlingIntent(
            ShulkerBundlingAction.valueOf(buf.readUtf(MAX_TEXT_FIELD_LENGTH)),
            new HostSlotRef(HostStorageScope.valueOf(buf.readUtf(MAX_TEXT_FIELD_LENGTH)), buf.readVarInt(), buf.readVarInt())
        ));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(intent.action().name());
        buf.writeUtf(intent.hostSlot().scope().name());
        buf.writeVarInt(intent.hostSlot().logicalSlotIndex());
        buf.writeVarInt(intent.hostSlot().menuSlotIndex());
    }
}
