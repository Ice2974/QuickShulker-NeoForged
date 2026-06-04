package com.ice2974.quickshulkerneoforged.common.open;

import java.util.Objects;

public record HostItemSnapshot(String itemKey, int count, String contentFingerprint) {
    public HostItemSnapshot {
        Objects.requireNonNull(itemKey, "itemKey");
        Objects.requireNonNull(contentFingerprint, "contentFingerprint");
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
    }

    public boolean isEmpty() {
        return count == 0;
    }
}
