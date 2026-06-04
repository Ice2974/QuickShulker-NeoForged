package com.ice2974.quickshulkerneoforged.common.network;

import com.ice2974.quickshulkerneoforged.common.content.ContainerContentSnapshot;
import java.util.Objects;

public record EnderChestFullSyncIntent(
    String sessionId,
    ContainerContentSnapshot contents
) implements NetworkIntent {
    public static final String CHANNEL_ID = "ender_chest_full_sync";

    public EnderChestFullSyncIntent {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(contents, "contents");
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
