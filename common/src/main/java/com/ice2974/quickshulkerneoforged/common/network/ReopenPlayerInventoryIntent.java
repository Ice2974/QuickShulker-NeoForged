package com.ice2974.quickshulkerneoforged.common.network;

import java.util.Objects;

public record ReopenPlayerInventoryIntent(String sessionId) implements NetworkIntent {
    public static final String CHANNEL_ID = "reopen_player_inventory";

    public ReopenPlayerInventoryIntent {
        Objects.requireNonNull(sessionId, "sessionId");
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
