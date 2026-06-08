package com.ice2974.quickshulkerneoforged.common.network;

import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import java.util.Objects;

public record ShulkerBundlingIntent(
    ShulkerBundlingAction action,
    HostSlotRef hostSlot
) implements NetworkIntent {
    public static final String CHANNEL_ID = "shulker_bundling";

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
