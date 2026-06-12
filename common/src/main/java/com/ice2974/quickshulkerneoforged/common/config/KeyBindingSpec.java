package com.ice2974.quickshulkerneoforged.common.config;

import java.util.Objects;

public record KeyBindingSpec(String id, String defaultTranslationKey) {
    public KeyBindingSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(defaultTranslationKey, "defaultTranslationKey");
    }

    public String translationKey() {
        return defaultTranslationKey;
    }
}
