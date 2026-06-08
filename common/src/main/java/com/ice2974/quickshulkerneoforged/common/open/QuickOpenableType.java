package com.ice2974.quickshulkerneoforged.common.open;

import java.util.Objects;

public record QuickOpenableType(
    String id,
    QuickOpenableCategory category,
    QuickOpenMenuKind menuKind,
    QuickOpenConfigGate configGate,
    boolean requiresSingleHostStack,
    boolean supportsBundlingOperations,
    boolean canOpenInHand,
    boolean lockHostSlotWhileOpen
) {
    public QuickOpenableType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(menuKind, "menuKind");
        Objects.requireNonNull(configGate, "configGate");
    }
}
