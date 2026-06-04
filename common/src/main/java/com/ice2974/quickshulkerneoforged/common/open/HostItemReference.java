package com.ice2974.quickshulkerneoforged.common.open;

import java.util.Objects;

public record HostItemReference(
    String quickOpenableTypeId,
    HostSlotRef slotRef,
    HostItemSnapshot initialSnapshot
) {
    public HostItemReference {
        Objects.requireNonNull(quickOpenableTypeId, "quickOpenableTypeId");
        Objects.requireNonNull(slotRef, "slotRef");
        Objects.requireNonNull(initialSnapshot, "initialSnapshot");
    }
}
