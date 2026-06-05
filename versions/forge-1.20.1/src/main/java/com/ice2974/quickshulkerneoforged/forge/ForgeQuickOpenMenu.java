package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.open.HostIdentity;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;

public interface ForgeQuickOpenMenu {
    void markHostInvalidated();

    HostSlotRef hostSlotRef();

    String quickOpenableTypeId();

    default boolean isSameHost(String requestedTypeId, HostSlotRef requestedHostSlot) {
        return HostIdentity.sameHost(quickOpenableTypeId(), hostSlotRef(), requestedTypeId, requestedHostSlot);
    }
}
