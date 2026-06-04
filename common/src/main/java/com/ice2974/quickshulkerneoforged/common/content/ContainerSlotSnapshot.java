package com.ice2974.quickshulkerneoforged.common.content;

import java.util.Objects;

public record ContainerSlotSnapshot(int slotIndex, String itemKey, int count, String contentFingerprint) {
    public ContainerSlotSnapshot {
        Objects.requireNonNull(itemKey, "itemKey");
        Objects.requireNonNull(contentFingerprint, "contentFingerprint");
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex must be >= 0");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
    }
}
