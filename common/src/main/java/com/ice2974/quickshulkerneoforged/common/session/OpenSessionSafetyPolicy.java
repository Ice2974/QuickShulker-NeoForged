package com.ice2974.quickshulkerneoforged.common.session;

public record OpenSessionSafetyPolicy(
    boolean revalidateBeforeOpen,
    boolean revalidateWhileOpen,
    boolean lockHostSlotWhileOpen,
    boolean discardChangesIfHostInvalid,
    boolean forceCloseIfHostChanges
) {
    public static OpenSessionSafetyPolicy strict() {
        return new OpenSessionSafetyPolicy(true, true, true, true, true);
    }
}
