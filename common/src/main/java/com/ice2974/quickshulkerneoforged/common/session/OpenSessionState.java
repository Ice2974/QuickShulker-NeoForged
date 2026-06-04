package com.ice2974.quickshulkerneoforged.common.session;

public enum OpenSessionState {
    REQUESTED,
    VALIDATED,
    OPEN,
    DIRTY,
    SAVE_PENDING,
    CLOSED,
    ABORTED
}
