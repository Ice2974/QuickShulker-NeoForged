package com.ice2974.quickshulkerneoforged.common.open;

import java.util.Objects;

public final class HostIdentity {
    private HostIdentity() {
    }

    public static boolean sameHost(HostItemReference left, HostItemReference right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return sameHost(left.quickOpenableTypeId(), left.slotRef(), right.quickOpenableTypeId(), right.slotRef());
    }

    public static boolean sameHost(String leftTypeId, HostSlotRef leftSlotRef, String rightTypeId, HostSlotRef rightSlotRef) {
        Objects.requireNonNull(leftTypeId, "leftTypeId");
        Objects.requireNonNull(leftSlotRef, "leftSlotRef");
        Objects.requireNonNull(rightTypeId, "rightTypeId");
        Objects.requireNonNull(rightSlotRef, "rightSlotRef");
        return leftTypeId.equals(rightTypeId) && sameSlot(leftSlotRef, rightSlotRef);
    }

    public static boolean sameSlot(HostSlotRef leftSlotRef, HostSlotRef rightSlotRef) {
        Objects.requireNonNull(leftSlotRef, "leftSlotRef");
        Objects.requireNonNull(rightSlotRef, "rightSlotRef");
        if (leftSlotRef.scope() != rightSlotRef.scope()) {
            return false;
        }
        return leftSlotRef.logicalSlotIndex() == rightSlotRef.logicalSlotIndex();
    }
}
