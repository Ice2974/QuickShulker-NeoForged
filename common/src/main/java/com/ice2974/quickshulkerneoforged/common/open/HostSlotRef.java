package com.ice2974.quickshulkerneoforged.common.open;

public record HostSlotRef(HostStorageScope scope, int logicalSlotIndex, int menuSlotIndex) {
    public HostSlotRef {
        if (scope == null) {
            throw new NullPointerException("scope");
        }
        if (logicalSlotIndex < -1) {
            throw new IllegalArgumentException("logicalSlotIndex must be >= -1");
        }
        if (menuSlotIndex < -1) {
            throw new IllegalArgumentException("menuSlotIndex must be >= -1");
        }
    }

    public static HostSlotRef unknown() {
        return new HostSlotRef(HostStorageScope.UNKNOWN, -1, -1);
    }
}
