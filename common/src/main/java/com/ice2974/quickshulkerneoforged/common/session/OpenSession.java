package com.ice2974.quickshulkerneoforged.common.session;

import com.ice2974.quickshulkerneoforged.common.open.HostItemReference;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenRequest;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OpenSession(
    String sessionId,
    QuickOpenRequest request,
    HostItemReference hostItem,
    MenuOpenIntent menuIntent,
    OpenSessionSafetyPolicy safetyPolicy,
    OpenSessionState state,
    boolean dirty,
    Instant createdAt
) {
    public OpenSession {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(hostItem, "hostItem");
        Objects.requireNonNull(menuIntent, "menuIntent");
        Objects.requireNonNull(safetyPolicy, "safetyPolicy");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static OpenSession create(
        QuickOpenRequest request,
        HostItemReference hostItem,
        MenuOpenIntent menuIntent,
        OpenSessionSafetyPolicy safetyPolicy
    ) {
        return new OpenSession(
            UUID.randomUUID().toString(),
            request,
            hostItem,
            menuIntent,
            safetyPolicy,
            OpenSessionState.REQUESTED,
            false,
            Instant.now()
        );
    }

    public OpenSession withState(OpenSessionState nextState) {
        return new OpenSession(sessionId, request, hostItem, menuIntent, safetyPolicy, nextState, dirty, createdAt);
    }

    public OpenSession markDirty() {
        return new OpenSession(sessionId, request, hostItem, menuIntent, safetyPolicy, OpenSessionState.DIRTY, true, createdAt);
    }
}
