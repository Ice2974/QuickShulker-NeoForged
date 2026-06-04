package com.ice2974.quickshulkerneoforged.common.open;

import java.util.Objects;

public final class DefaultHostItemValidator implements HostItemValidator {
    @Override
    public HostValidationResult validate(
        HostItemReference expectedHost,
        HostItemSnapshot currentSnapshot,
        HostValidationMode mode,
        boolean requiresSingleHostStack
    ) {
        Objects.requireNonNull(expectedHost, "expectedHost");
        Objects.requireNonNull(currentSnapshot, "currentSnapshot");
        Objects.requireNonNull(mode, "mode");

        HostItemSnapshot initial = expectedHost.initialSnapshot();
        if (currentSnapshot.isEmpty()) {
            return HostValidationResult.invalid(HostValidationFailure.HOST_MISSING, "Host item is no longer present.");
        }
        if (!initial.itemKey().equals(currentSnapshot.itemKey())) {
            return HostValidationResult.invalid(HostValidationFailure.ITEM_TYPE_CHANGED, "Host item type changed.");
        }
        if (requiresSingleHostStack && currentSnapshot.count() != 1) {
            return HostValidationResult.invalid(HostValidationFailure.STACK_COUNT_CHANGED, "Host stack count is no longer singular.");
        }
        if (mode == HostValidationMode.EXACT && initial.count() != currentSnapshot.count()) {
            return HostValidationResult.invalid(HostValidationFailure.STACK_COUNT_CHANGED, "Host stack count changed.");
        }
        if (mode == HostValidationMode.EXACT
            && !initial.contentFingerprint().equals(currentSnapshot.contentFingerprint())) {
            return HostValidationResult.invalid(HostValidationFailure.CONTENT_FINGERPRINT_CHANGED, "Host content fingerprint changed.");
        }
        return HostValidationResult.success();
    }
}
