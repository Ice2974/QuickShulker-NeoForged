package com.ice2974.quickshulkerneoforged.common.bundling;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ContainerBundlingRules {
    private ContainerBundlingRules() {
    }

    public static <S> ShulkerBundlingResult<List<S>, S> insertIntoContents(
        List<S> originalContents,
        S inputStack,
        ShulkerBundlingStackAdapter<S> adapter
    ) {
        List<S> contents = copyContents(originalContents, adapter);
        S inputCopy = adapter.copy(inputStack);

        if (adapter.isEmpty(inputCopy)) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                ShulkerBundlingFailure.SOURCE_EMPTY,
                0,
                Optional.of(contents),
                Optional.empty(),
                Optional.empty(),
                Optional.of(inputCopy),
                Optional.empty(),
                "Cannot insert an empty stack into container contents."
            );
        }

        int remaining = adapter.getCount(inputCopy);
        for (int i = 0; i < contents.size() && remaining > 0; i++) {
            S existing = contents.get(i);
            if (adapter.isEmpty(existing) || !adapter.canStacksMerge(existing, inputCopy)) {
                continue;
            }
            int limit = Math.min(adapter.getMaxStackSize(existing), adapter.getMaxStackSize(inputCopy));
            int space = limit - adapter.getCount(existing);
            if (space <= 0) {
                continue;
            }
            int moved = Math.min(space, remaining);
            contents.set(i, adapter.copyWithCount(existing, adapter.getCount(existing) + moved));
            remaining -= moved;
        }

        for (int i = 0; i < contents.size() && remaining > 0; i++) {
            S existing = contents.get(i);
            if (!adapter.isEmpty(existing)) {
                continue;
            }
            int moved = Math.min(adapter.getMaxStackSize(inputCopy), remaining);
            contents.set(i, adapter.copyWithCount(inputCopy, moved));
            remaining -= moved;
        }

        int movedCount = adapter.getCount(inputCopy) - remaining;
        S updatedInput = remaining == 0 ? adapter.empty() : adapter.copyWithCount(inputCopy, remaining);
        if (movedCount == 0) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                ShulkerBundlingFailure.NO_SPACE,
                0,
                Optional.of(copyContents(originalContents, adapter)),
                Optional.empty(),
                Optional.empty(),
                Optional.of(inputCopy),
                Optional.empty(),
                "Container contents had no room for the input stack."
            );
        }

        return new ShulkerBundlingResult<>(
            true,
            true,
            ShulkerBundlingFailure.NONE,
            movedCount,
            Optional.of(contents),
            Optional.empty(),
            Optional.empty(),
            Optional.of(updatedInput),
            Optional.empty(),
            "Inserted items into container contents."
        );
    }

    public static <S> ShulkerBundlingResult<List<S>, S> extractFirstStack(
        List<S> originalContents,
        ShulkerBundlingStackAdapter<S> adapter
    ) {
        List<S> contents = copyContents(originalContents, adapter);
        for (int i = 0; i < contents.size(); i++) {
            S existing = contents.get(i);
            if (adapter.isEmpty(existing)) {
                continue;
            }
            S extracted = adapter.copy(existing);
            contents.set(i, adapter.empty());
            return new ShulkerBundlingResult<>(
                true,
                true,
                ShulkerBundlingFailure.NONE,
                adapter.getCount(extracted),
                Optional.of(contents),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(extracted),
                "Extracted the first non-empty slot stack from container contents."
            );
        }

        return new ShulkerBundlingResult<>(
            false,
            false,
            ShulkerBundlingFailure.NO_ITEMS_TO_EXTRACT,
            0,
            Optional.of(contents),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            "Container contents were empty."
        );
    }

    public static <S> ShulkerBundlingResult<List<S>, S> extractLastStack(
        List<S> originalContents,
        ShulkerBundlingStackAdapter<S> adapter
    ) {
        List<S> contents = copyContents(originalContents, adapter);
        for (int i = contents.size() - 1; i >= 0; i--) {
            S existing = contents.get(i);
            if (adapter.isEmpty(existing)) {
                continue;
            }
            S extracted = adapter.copy(existing);
            contents.set(i, adapter.empty());
            return new ShulkerBundlingResult<>(
                true,
                true,
                ShulkerBundlingFailure.NONE,
                adapter.getCount(extracted),
                Optional.of(contents),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(extracted),
                "Extracted the last non-empty slot stack from container contents."
            );
        }

        return new ShulkerBundlingResult<>(
            false,
            false,
            ShulkerBundlingFailure.NO_ITEMS_TO_EXTRACT,
            0,
            Optional.of(contents),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            "Container contents were empty."
        );
    }

    public static <S> List<S> copyContents(List<S> contents, ShulkerBundlingStackAdapter<S> adapter) {
        List<S> copies = new ArrayList<>(contents.size());
        for (S content : contents) {
            copies.add(adapter.copy(content));
        }
        return copies;
    }
}
