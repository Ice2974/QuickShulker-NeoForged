package com.ice2974.quickshulkerneoforged.common.open;

public interface HostItemValidator {
    HostValidationResult validate(
        HostItemReference expectedHost,
        HostItemSnapshot currentSnapshot,
        HostValidationMode mode,
        boolean requiresSingleHostStack
    );
}
