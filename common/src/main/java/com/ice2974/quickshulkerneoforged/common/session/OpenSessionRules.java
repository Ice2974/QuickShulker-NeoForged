package com.ice2974.quickshulkerneoforged.common.session;

import com.ice2974.quickshulkerneoforged.common.open.HostValidationResult;
import java.util.Objects;

public final class OpenSessionRules {
    private OpenSessionRules() {
    }

    public static SaveDisposition decideSaveDisposition(
        OpenSession session,
        HostValidationResult validationResult,
        CloseReason closeReason
    ) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(validationResult, "validationResult");
        Objects.requireNonNull(closeReason, "closeReason");

        if (!session.dirty()) {
            return SaveDisposition.NO_CHANGES;
        }
        if (!validationResult.valid() && session.safetyPolicy().discardChangesIfHostInvalid()) {
            return SaveDisposition.DISCARD_CHANGES;
        }
        if (closeReason == CloseReason.HOST_INVALIDATED || closeReason == CloseReason.VALIDATION_REJECTED) {
            return SaveDisposition.DISCARD_CHANGES;
        }
        return SaveDisposition.SAVE_TO_HOST;
    }
}
