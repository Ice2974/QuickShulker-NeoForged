package com.ice2974.quickshulkerneoforged.common.bundling;

import java.util.List;
import java.util.Optional;

public final class EnderChestBundlingRules {
    private EnderChestBundlingRules() {
    }

    public static <S> Optional<EnderChestBundlingOperation> resolveOperation(
        S carriedStack,
        S hoveredStack,
        EnderChestBundlingStackAdapter<S> adapter
    ) {
        if (adapter.isEmpty(hoveredStack) && adapter.isSingleEnderChest(carriedStack)) {
            return Optional.of(EnderChestBundlingOperation.EXTRACT);
        }
        if (adapter.isSingleEnderChest(hoveredStack) && !adapter.isEmpty(carriedStack)) {
            return Optional.of(EnderChestBundlingOperation.INSERT);
        }
        if (adapter.isSingleEnderChest(carriedStack) && !adapter.isEmpty(hoveredStack)) {
            return Optional.of(EnderChestBundlingOperation.PICKUP_INSERT);
        }
        return Optional.empty();
    }

    public static <S> ShulkerBundlingResult<List<S>, S> insertIntoPlayerEnderChest(
        List<S> originalContents,
        S inputStack,
        EnderChestBundlingStackAdapter<S> adapter
    ) {
        S inputCopy = adapter.copy(inputStack);
        if (adapter.isEmpty(inputCopy)) {
            return ContainerBundlingRules.insertIntoContents(originalContents, inputCopy, adapter);
        }
        if (!adapter.canInsertIntoEnderChest(inputCopy)) {
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
                "The input stack is not accepted by player ender chest bundling rules."
            );
        }
        return ContainerBundlingRules.insertIntoContents(originalContents, inputCopy, adapter);
    }

    public static <S> ShulkerBundlingResult<List<S>, S> pickupInsertIntoPlayerEnderChest(
        List<S> originalContents,
        S slotStack,
        EnderChestBundlingStackAdapter<S> adapter
    ) {
        return insertIntoPlayerEnderChest(originalContents, slotStack, adapter);
    }

    public static <S> ShulkerBundlingResult<List<S>, S> extractLastStackFromPlayerEnderChest(
        List<S> originalContents,
        EnderChestBundlingStackAdapter<S> adapter
    ) {
        return extractLastStackFromPlayerEnderChest(originalContents, adapter, false);
    }

    public static <S> ShulkerBundlingResult<List<S>, S> extractLastStackFromPlayerEnderChest(
        List<S> originalContents,
        EnderChestBundlingStackAdapter<S> adapter,
        boolean skipShulkerBoxes
    ) {
        if (skipShulkerBoxes) {
            return ContainerBundlingRules.extractLastStack(originalContents, adapter, stack -> !adapter.isShulkerBox(stack));
        }
        return ContainerBundlingRules.extractLastStack(originalContents, adapter);
    }
}
