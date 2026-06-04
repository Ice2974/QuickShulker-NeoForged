package com.ice2974.quickshulkerneoforged.common.network;

import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenTrigger;
import java.util.Objects;

public record OpenHostItemIntent(
    String requestedTypeId,
    HostSlotRef hostSlot,
    QuickOpenTrigger trigger
) implements NetworkIntent {
    public static final String CHANNEL_ID = "open_host_item";

    public OpenHostItemIntent {
        Objects.requireNonNull(requestedTypeId, "requestedTypeId");
        Objects.requireNonNull(hostSlot, "hostSlot");
        Objects.requireNonNull(trigger, "trigger");
    }

    @Override
    public String channelId() {
        return CHANNEL_ID;
    }

    @Override
    public NetworkDirection direction() {
        return NetworkDirection.CLIENT_TO_SERVER;
    }
}
