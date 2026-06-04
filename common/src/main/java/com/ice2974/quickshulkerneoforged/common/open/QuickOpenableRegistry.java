package com.ice2974.quickshulkerneoforged.common.open;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class QuickOpenableRegistry {
    private final Map<String, QuickOpenableType> typesById = new LinkedHashMap<>();
    private final Map<String, String> itemBindings = new LinkedHashMap<>();

    public QuickOpenableRegistry registerType(QuickOpenableType type) {
        Objects.requireNonNull(type, "type");
        QuickOpenableType previous = typesById.putIfAbsent(type.id(), type);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate quick-openable type id: " + type.id());
        }
        return this;
    }

    public QuickOpenableRegistry bindItem(String itemKey, String typeId) {
        Objects.requireNonNull(itemKey, "itemKey");
        Objects.requireNonNull(typeId, "typeId");
        if (!typesById.containsKey(typeId)) {
            throw new IllegalArgumentException("Unknown quick-openable type id: " + typeId);
        }
        String previous = itemBindings.putIfAbsent(itemKey, typeId);
        if (previous != null && !previous.equals(typeId)) {
            throw new IllegalArgumentException("Item key is already bound: " + itemKey);
        }
        return this;
    }

    public Optional<QuickOpenableType> findType(String typeId) {
        return Optional.ofNullable(typesById.get(typeId));
    }

    public Optional<QuickOpenableType> findTypeForItem(String itemKey) {
        String typeId = itemBindings.get(itemKey);
        if (typeId == null) {
            return Optional.empty();
        }
        return findType(typeId);
    }

    public Collection<QuickOpenableType> types() {
        return Collections.unmodifiableCollection(typesById.values());
    }

    public Map<String, String> itemBindings() {
        return Collections.unmodifiableMap(itemBindings);
    }
}
