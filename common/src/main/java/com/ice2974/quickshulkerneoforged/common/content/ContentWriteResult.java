package com.ice2974.quickshulkerneoforged.common.content;

public record ContentWriteResult(boolean applied, String detail) {
    public static ContentWriteResult applied(String detail) {
        return new ContentWriteResult(true, detail);
    }

    public static ContentWriteResult rejected(String detail) {
        return new ContentWriteResult(false, detail);
    }
}
