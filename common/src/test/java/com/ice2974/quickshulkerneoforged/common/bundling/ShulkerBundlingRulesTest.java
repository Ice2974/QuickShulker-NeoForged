package com.ice2974.quickshulkerneoforged.common.bundling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShulkerBundlingRulesTest {
    private static final FakeStack EMPTY = new FakeStack("empty", 0, 64, false, true);
    private static final FakeStack STONE_16 = new FakeStack("stone", 16, 64, false, true);
    private static final FakeStack STONE_32 = new FakeStack("stone", 32, 64, false, true);
    private static final FakeStack STONE_48 = new FakeStack("stone", 48, 64, false, true);
    private static final FakeStack STONE_64 = new FakeStack("stone", 64, 64, false, true);
    private static final FakeStack DIRT_8 = new FakeStack("dirt", 8, 64, false, true);
    private static final FakeStack SHULKER_1 = new FakeStack("shulker", 1, 1, true, false);
    private static final ShulkerBundlingStackAdapter<FakeStack> ADAPTER = new FakeStackAdapter();

    @Test
    void insertIntoEmptyShulkerUsesFirstEmptySlot() {
        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = ShulkerBundlingRules.insertIntoContents(
            contents(EMPTY, EMPTY, EMPTY),
            STONE_16,
            ADAPTER
        );

        assertTrue(result.success());
        assertEquals(16, result.movedCount());
        assertTrue(result.updatedInputStack().orElseThrow().isEmpty());
        assertEquals(16, result.updatedContainerStack().orElseThrow().get(0).count());
    }

    @Test
    void insertMergesIntoExistingMatchingStackFirst() {
        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = ShulkerBundlingRules.insertIntoContents(
            contents(STONE_48, EMPTY, EMPTY),
            STONE_32,
            ADAPTER
        );

        assertTrue(result.success());
        assertEquals(32, result.movedCount());
        List<FakeStack> updated = result.updatedContainerStack().orElseThrow();
        assertEquals(64, updated.get(0).count());
        assertEquals(16, updated.get(1).count());
        assertTrue(result.updatedInputStack().orElseThrow().isEmpty());
    }

    @Test
    void insertIntoPartiallyFullShulkerOnlyMovesCapacity() {
        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = ShulkerBundlingRules.insertIntoContents(
            contents(STONE_64, DIRT_8, EMPTY),
            new FakeStack("stone", 20, 64, false, true),
            ADAPTER
        );

        assertTrue(result.success());
        assertEquals(20, result.movedCount());
        assertEquals(20, result.updatedContainerStack().orElseThrow().get(2).count());
        assertTrue(result.updatedInputStack().orElseThrow().isEmpty());
    }

    @Test
    void insertIntoFullShulkerFailsWithoutMutation() {
        List<FakeStack> original = contents(STONE_64, DIRT_8);

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = ShulkerBundlingRules.insertIntoContents(
            original,
            STONE_16,
            ADAPTER
        );

        assertFalse(result.success());
        assertFalse(result.changed());
        assertEquals(ShulkerBundlingFailure.NO_SPACE, result.failure());
        assertEquals(original, result.updatedContainerStack().orElseThrow());
        assertEquals(STONE_16, result.updatedInputStack().orElseThrow());
    }

    @Test
    void insertRejectsShulkerItems() {
        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = ShulkerBundlingRules.insertIntoContents(
            contents(EMPTY, EMPTY),
            SHULKER_1,
            ADAPTER
        );

        assertFalse(result.success());
        assertEquals(ShulkerBundlingFailure.WOULD_NEST_SHULKER, result.failure());
    }

    @Test
    void extractReturnsFirstNonEmptySlotStack() {
        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = ShulkerBundlingRules.extractFirstStack(
            contents(EMPTY, DIRT_8, STONE_16),
            ADAPTER
        );

        assertTrue(result.success());
        assertEquals(DIRT_8, result.extractedStack().orElseThrow());
        assertTrue(result.updatedContainerStack().orElseThrow().get(1).isEmpty());
    }

    @Test
    void extractFromEmptyShulkerFails() {
        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = ShulkerBundlingRules.extractFirstStack(
            contents(EMPTY, EMPTY),
            ADAPTER
        );

        assertFalse(result.success());
        assertEquals(ShulkerBundlingFailure.NO_ITEMS_TO_EXTRACT, result.failure());
    }

    @Test
    void transferIntoEmptyTargetMovesAllSupportedItems() {
        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = ShulkerBundlingRules.transferContents(
            contents(STONE_16, DIRT_8, EMPTY),
            contents(EMPTY, EMPTY, EMPTY),
            ADAPTER
        );

        assertTrue(result.success());
        assertEquals(24, result.movedCount());
        assertTrue(result.updatedSourceContainerStack().orElseThrow().get(0).isEmpty());
        assertTrue(result.updatedSourceContainerStack().orElseThrow().get(1).isEmpty());
        assertEquals(16, result.updatedTargetContainerStack().orElseThrow().get(0).count());
        assertEquals(8, result.updatedTargetContainerStack().orElseThrow().get(1).count());
    }

    @Test
    void transferIntoPartiallyFullTargetMergesAndSpillsToEmptySlot() {
        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = ShulkerBundlingRules.transferContents(
            contents(STONE_32, EMPTY, EMPTY),
            contents(STONE_48, EMPTY, EMPTY),
            ADAPTER
        );

        assertTrue(result.success());
        assertEquals(32, result.movedCount());
        assertTrue(result.updatedSourceContainerStack().orElseThrow().get(0).isEmpty());
        assertEquals(64, result.updatedTargetContainerStack().orElseThrow().get(0).count());
        assertEquals(16, result.updatedTargetContainerStack().orElseThrow().get(1).count());
    }

    @Test
    void transferOnlyMovesWhatTargetCanAccept() {
        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = ShulkerBundlingRules.transferContents(
            contents(STONE_32, EMPTY),
            contents(STONE_64, new FakeStack("stone", 60, 64, false, true)),
            ADAPTER
        );

        assertTrue(result.success());
        assertEquals(4, result.movedCount());
        assertEquals(28, result.updatedSourceContainerStack().orElseThrow().get(0).count());
        assertEquals(64, result.updatedTargetContainerStack().orElseThrow().get(1).count());
    }

    @Test
    void transferSkipsNestedShulkerItemsButStillMovesNormalItems() {
        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = ShulkerBundlingRules.transferContents(
            contents(SHULKER_1, DIRT_8, EMPTY),
            contents(EMPTY, EMPTY, EMPTY),
            ADAPTER
        );

        assertTrue(result.success());
        assertEquals(8, result.movedCount());
        assertEquals(SHULKER_1, result.updatedSourceContainerStack().orElseThrow().get(0));
        assertTrue(result.updatedSourceContainerStack().orElseThrow().get(1).isEmpty());
        assertEquals(8, result.updatedTargetContainerStack().orElseThrow().get(0).count());
    }

    @Test
    void transferWithoutSpaceReturnsFailure() {
        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = ShulkerBundlingRules.transferContents(
            contents(STONE_16, EMPTY),
            contents(STONE_64, DIRT_8),
            ADAPTER
        );

        assertFalse(result.success());
        assertEquals(ShulkerBundlingFailure.NO_SPACE, result.failure());
    }

    private static List<FakeStack> contents(FakeStack... stacks) {
        List<FakeStack> copies = new ArrayList<>(stacks.length);
        for (FakeStack stack : stacks) {
            copies.add(stack.copy());
        }
        return copies;
    }

    private record FakeStack(String key, int count, int maxStackSize, boolean shulker, boolean canFitInsideContainers) {
        private FakeStack {
            if (count < 0) {
                throw new IllegalArgumentException("count must be >= 0");
            }
        }

        private FakeStack copy() {
            return new FakeStack(key, count, maxStackSize, shulker, canFitInsideContainers);
        }

        private boolean isEmpty() {
            return count == 0;
        }
    }

    private static final class FakeStackAdapter implements ShulkerBundlingStackAdapter<FakeStack> {
        @Override
        public FakeStack empty() {
            return EMPTY;
        }

        @Override
        public FakeStack copy(FakeStack stack) {
            return stack.copy();
        }

        @Override
        public FakeStack copyWithCount(FakeStack stack, int count) {
            return new FakeStack(stack.key(), count, stack.maxStackSize(), stack.shulker(), stack.canFitInsideContainers());
        }

        @Override
        public boolean isEmpty(FakeStack stack) {
            return stack.isEmpty();
        }

        @Override
        public boolean isShulkerBox(FakeStack stack) {
            return stack.shulker();
        }

        @Override
        public boolean canInsertIntoShulker(FakeStack stack) {
            return !stack.isEmpty() && stack.canFitInsideContainers() && !stack.shulker();
        }

        @Override
        public boolean canStacksMerge(FakeStack existingStack, FakeStack incomingStack) {
            return existingStack.key().equals(incomingStack.key());
        }

        @Override
        public int getCount(FakeStack stack) {
            return stack.count();
        }

        @Override
        public int getMaxStackSize(FakeStack stack) {
            return stack.maxStackSize();
        }
    }
}
