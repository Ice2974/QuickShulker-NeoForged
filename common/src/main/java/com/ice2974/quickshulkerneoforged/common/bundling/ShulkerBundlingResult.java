package com.ice2974.quickshulkerneoforged.common.bundling;

import java.util.Objects;
import java.util.Optional;

public record ShulkerBundlingResult<H, S>(
    boolean success,
    boolean changed,
    ShulkerBundlingFailure failure,
    int movedCount,
    Optional<H> updatedContainerStack,
    Optional<H> updatedSourceContainerStack,
    Optional<H> updatedTargetContainerStack,
    Optional<S> updatedInputStack,
    Optional<S> extractedStack,
    String detail
) {
    public ShulkerBundlingResult {
        Objects.requireNonNull(failure, "failure");
        Objects.requireNonNull(updatedContainerStack, "updatedContainerStack");
        Objects.requireNonNull(updatedSourceContainerStack, "updatedSourceContainerStack");
        Objects.requireNonNull(updatedTargetContainerStack, "updatedTargetContainerStack");
        Objects.requireNonNull(updatedInputStack, "updatedInputStack");
        Objects.requireNonNull(extractedStack, "extractedStack");
        Objects.requireNonNull(detail, "detail");
        if (movedCount < 0) {
            throw new IllegalArgumentException("movedCount must be >= 0");
        }
    }
}
