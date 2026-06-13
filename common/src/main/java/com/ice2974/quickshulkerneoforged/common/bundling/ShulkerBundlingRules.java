package com.ice2974.quickshulkerneoforged.common.bundling;

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
        S inputCopy = adapter.copy(inputStack);

        if (adapter.isEmpty(inputCopy)) {
            return ContainerBundlingRules.insertIntoContents(originalContents, inputCopy, adapter);
        }
        if (adapter.isShulkerBox(inputCopy)) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                ShulkerBundlingFailure.WOULD_NEST_SHULKER,
                0,
                Optional.of(ContainerBundlingRules.copyContents(originalContents, adapter)),
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
                Optional.of(ContainerBundlingRules.copyContents(originalContents, adapter)),
                Optional.empty(),
                Optional.empty(),
                Optional.of(inputCopy),
                Optional.empty(),
                "The input stack is not accepted by shulker bundling rules."
            );
        }
        return ContainerBundlingRules.insertIntoContents(originalContents, inputCopy, adapter);
    }

    // Stage 4A extracts the entire first non-empty slot stack to keep the writeback path explicit.
    public static <S> ShulkerBundlingResult<List<S>, S> extractFirstStack(
        List<S> originalContents,
        ShulkerBundlingStackAdapter<S> adapter
    ) {
        return ContainerBundlingRules.extractFirstStack(originalContents, adapter);
    }

    // Mouse dragged extract follows the original tail-first slot scan order.
    public static <S> ShulkerBundlingResult<List<S>, S> extractLastStack(
        List<S> originalContents,
        ShulkerBundlingStackAdapter<S> adapter
    ) {
        return ContainerBundlingRules.extractLastStack(originalContents, adapter);
    }

    public static <S> ShulkerBundlingResult<List<S>, S> transferContents(
        List<S> originalSourceContents,
        List<S> originalTargetContents,
        ShulkerBundlingStackAdapter<S> adapter
    ) {
        List<S> sourceContents = ContainerBundlingRules.copyContents(originalSourceContents, adapter);
        List<S> targetContents = ContainerBundlingRules.copyContents(originalTargetContents, adapter);

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

            targetContents = ContainerBundlingRules.copyContents(slotResult.updatedContainerStack().orElseThrow(), adapter);
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

}
