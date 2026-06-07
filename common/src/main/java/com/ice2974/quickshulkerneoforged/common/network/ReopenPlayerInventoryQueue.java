package com.ice2974.quickshulkerneoforged.common.network;

import java.util.Objects;

public final class ReopenPlayerInventoryQueue {
    private static final long DEFAULT_TTL_NANOS = 500_000_000L;

    private static PendingReopen pendingReopen;

    private ReopenPlayerInventoryQueue() {
    }

    public static synchronized void schedule(ReopenPlayerInventoryIntent intent) {
        Objects.requireNonNull(intent, "intent");
        pendingReopen = new PendingReopen(intent.sessionId(), System.nanoTime() + DEFAULT_TTL_NANOS);
    }

    public static synchronized PendingReopen pending() {
        return pendingReopen;
    }

    public static synchronized void clear() {
        pendingReopen = null;
    }

    public record PendingReopen(String sessionId, long expiresAtNanoTime) {
        public PendingReopen {
            Objects.requireNonNull(sessionId, "sessionId");
        }

        public boolean isExpired(long now) {
            return now > expiresAtNanoTime;
        }
    }
}
