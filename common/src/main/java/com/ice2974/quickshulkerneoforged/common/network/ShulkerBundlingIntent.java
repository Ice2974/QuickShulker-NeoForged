package com.ice2974.quickshulkerneoforged.common.network;

import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import java.util.Objects;

public record ShulkerBundlingIntent(
    ShulkerBundlingAction action,
    HostSlotRef hostSlot,
    int containerId,
    long dragId
) implements NetworkIntent {
    public static final String CHANNEL_ID = "shulker_bundling";

    public ShulkerBundlingIntent(ShulkerBundlingAction action, HostSlotRef hostSlot) {
        this(action, hostSlot, -1, 0L);
    }

    public ShulkerBundlingIntent {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(hostSlot, "hostSlot");
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
