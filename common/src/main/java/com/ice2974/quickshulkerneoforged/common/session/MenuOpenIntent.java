package com.ice2974.quickshulkerneoforged.common.session;

import com.ice2974.quickshulkerneoforged.common.open.HostItemReference;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenMenuKind;
import java.util.Objects;

public record MenuOpenIntent(
    String sessionId,
    String quickOpenableTypeId,
    QuickOpenMenuKind menuKind,
    HostItemReference hostItem,
    boolean lockHostSlotWhileOpen
) {
    public MenuOpenIntent {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(quickOpenableTypeId, "quickOpenableTypeId");
        Objects.requireNonNull(menuKind, "menuKind");
        Objects.requireNonNull(hostItem, "hostItem");
    }
}
