package com.ice2974.quickshulkerneoforged.common.content;

import java.util.List;
import java.util.Objects;

public record ContainerContentSnapshot(int slotCount, List<ContainerSlotSnapshot> slots) {
    public ContainerContentSnapshot {
        if (slotCount < 0) {
            throw new IllegalArgumentException("slotCount must be >= 0");
        }
        Objects.requireNonNull(slots, "slots");
        slots = List.copyOf(slots);
    }

    public static ContainerContentSnapshot empty(int slotCount) {
        return new ContainerContentSnapshot(slotCount, List.of());
    }
}
