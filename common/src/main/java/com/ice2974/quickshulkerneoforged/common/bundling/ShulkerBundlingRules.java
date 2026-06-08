package com.ice2974.quickshulkerneoforged.common.bundling;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ShulkerBundlingRules {
    private ShulkerBundlingRules() {
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
                "Cannot insert an empty stack into shulker contents."
            );
        }
        if (adapter.isShulkerBox(inputCopy)) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                ShulkerBundlingFailure.WOULD_NEST_SHULKER,
                0,
                Optional.of(contents),
                Optional.empty(),
                Optional.empty(),
                Optional.of(inputCopy),
                Optional.empty(),
                "Refused to insert a shulker item into shulker contents."
            );
        }
        if (!adapter.canInsertIntoShulker(inputCopy)) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                ShulkerBundlingFailure.UNSUPPORTED_ITEM,
                0,
                Optional.of(contents),
                Optional.empty(),
                Optional.empty(),
                Optional.of(inputCopy),
                Optional.empty(),
                "The input stack is not accepted by shulker bundling rules."
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
                "Shulker contents had no room for the input stack."
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
            "Inserted items into shulker contents."
        );
    }

    // Stage 4A extracts the entire first non-empty slot stack to keep the writeback path explicit.
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
                "Extracted the first non-empty slot stack from shulker contents."
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
            "Shulker contents were empty."
        );
    }

    public static <S> ShulkerBundlingResult<List<S>, S> transferContents(
        List<S> originalSourceContents,
        List<S> originalTargetContents,
        ShulkerBundlingStackAdapter<S> adapter
    ) {
        List<S> sourceContents = copyContents(originalSourceContents, adapter);
        List<S> targetContents = copyContents(originalTargetContents, adapter);

        ShulkerBundlingFailure blockedReason = ShulkerBundlingFailure.NO_ITEMS_TO_EXTRACT;
        boolean sawTransferableItem = false;
        int movedCount = 0;

        for (int i = 0; i < sourceContents.size(); i++) {
            S sourceStack = sourceContents.get(i);
            if (adapter.isEmpty(sourceStack)) {
                continue;
            }
            if (adapter.isShulkerBox(sourceStack)) {
                if (!sawTransferableItem) {
                    blockedReason = ShulkerBundlingFailure.WOULD_NEST_SHULKER;
                }
                continue;
            }
            if (!adapter.canInsertIntoShulker(sourceStack)) {
                if (!sawTransferableItem) {
                    blockedReason = ShulkerBundlingFailure.UNSUPPORTED_ITEM;
                }
                continue;
            }

            sawTransferableItem = true;
            ShulkerBundlingResult<List<S>, S> slotResult = insertIntoContents(targetContents, sourceStack, adapter);
            if (!slotResult.changed()) {
                if (movedCount == 0) {
                    blockedReason = slotResult.failure();
                }
                continue;
            }

            targetContents = copyContents(slotResult.updatedContainerStack().orElseThrow(), adapter);
            S remainingSource = slotResult.updatedInputStack().orElseGet(adapter::empty);
            sourceContents.set(i, adapter.copy(remainingSource));
            movedCount += slotResult.movedCount();
        }

        if (movedCount == 0) {
            if (sawTransferableItem && blockedReason == ShulkerBundlingFailure.NO_ITEMS_TO_EXTRACT) {
                blockedReason = ShulkerBundlingFailure.NO_SPACE;
            }
            return new ShulkerBundlingResult<>(
                false,
                false,
                blockedReason,
                0,
                Optional.empty(),
                Optional.of(sourceContents),
                Optional.of(targetContents),
                Optional.empty(),
                Optional.empty(),
                "No source items were transferred into the target shulker contents."
            );
        }

        return new ShulkerBundlingResult<>(
            true,
            true,
            ShulkerBundlingFailure.NONE,
            movedCount,
            Optional.empty(),
            Optional.of(sourceContents),
            Optional.of(targetContents),
            Optional.empty(),
            Optional.empty(),
            "Transferred items between shulker contents."
        );
    }

    private static <S> List<S> copyContents(List<S> contents, ShulkerBundlingStackAdapter<S> adapter) {
        List<S> copies = new ArrayList<>(contents.size());
        for (S content : contents) {
            copies.add(adapter.copy(content));
        }
        return copies;
    }
}
