package com.ice2974.quickshulkerneoforged.common.open;

import java.util.Objects;

public record QuickOpenRequest(
    String requestedTypeId,
    HostSlotRef hostSlot,
    QuickOpenTrigger trigger,
    boolean initiatedFromClient
) {
    public QuickOpenRequest {
        Objects.requireNonNull(requestedTypeId, "requestedTypeId");
        Objects.requireNonNull(hostSlot, "hostSlot");
        Objects.requireNonNull(trigger, "trigger");
    }
}
