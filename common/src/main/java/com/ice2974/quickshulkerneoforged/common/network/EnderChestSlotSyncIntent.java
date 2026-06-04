package com.ice2974.quickshulkerneoforged.common.network;

import com.ice2974.quickshulkerneoforged.common.content.ContainerSlotSnapshot;
import java.util.Objects;

public record EnderChestSlotSyncIntent(
    String sessionId,
    ContainerSlotSnapshot slot
) implements NetworkIntent {
    public static final String CHANNEL_ID = "ender_chest_slot_sync";

    public EnderChestSlotSyncIntent {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(slot, "slot");
    }

    @Override
    public String channelId() {
        return CHANNEL_ID;
    }

    @Override
    public NetworkDirection direction() {
        return NetworkDirection.SERVER_TO_CLIENT;
    }
}
