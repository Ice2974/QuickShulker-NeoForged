package com.ice2974.quickshulkerneoforged.common.open;

public record HostValidationResult(boolean valid, HostValidationFailure failure, String detail) {
    public static HostValidationResult success() {
        return new HostValidationResult(true, HostValidationFailure.NONE, "");
    }

    public static HostValidationResult invalid(HostValidationFailure failure, String detail) {
        return new HostValidationResult(false, failure, detail);
    }
}
