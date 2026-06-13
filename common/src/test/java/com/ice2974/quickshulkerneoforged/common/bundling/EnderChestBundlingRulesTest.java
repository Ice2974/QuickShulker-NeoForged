package com.ice2974.quickshulkerneoforged.common.bundling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ice2974.quickshulkerneoforged.common.content.ContentWriteResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnderChestBundlingRulesTest {
    private static final FakeStack EMPTY = new FakeStack("empty", 0, 64, Kind.NORMAL, false);
    private static final FakeStack STONE_16 = new FakeStack("stone", 16, 64, Kind.NORMAL, true);
    private static final FakeStack STONE_32 = new FakeStack("stone", 32, 64, Kind.NORMAL, true);
    private static final FakeStack STONE_64 = new FakeStack("stone", 64, 64, Kind.NORMAL, true);
    private static final FakeStack DIRT_8 = new FakeStack("dirt", 8, 64, Kind.NORMAL, true);
    private static final FakeStack SHULKER_1 = new FakeStack("shulker", 1, 1, Kind.SHULKER, true);
    private static final FakeStack ENDER_CHEST_1 = new FakeStack("ender_chest", 1, 64, Kind.ENDER_CHEST, true);
    private static final FakeStack ENDER_CHEST_2 = new FakeStack("ender_chest", 2, 64, Kind.ENDER_CHEST, true);
    private static final EnderChestBundlingStackAdapter<FakeStack> ADAPTER = new FakeStackAdapter();

    @Test
    void insertOrdinaryItemIntoPlayerEnderChest() {
        FakePlayer player = new FakePlayer(contents(EMPTY, EMPTY, EMPTY));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.insert(player, STONE_16);

        assertTrue(result.success());
        assertEquals(16, result.movedCount());
        assertTrue(result.updatedInputStack().orElseThrow().isEmpty());
        assertEquals(STONE_16, player.enderChestContents().get(0));
    }

    @Test
    void insertShulkerIntoPlayerEnderChest() {
        FakePlayer player = new FakePlayer(contents(EMPTY, EMPTY, EMPTY));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.insert(player, SHULKER_1);

        assertTrue(result.success());
        assertEquals(1, result.movedCount());
        assertEquals(SHULKER_1, player.enderChestContents().get(0));
    }

    @Test
    void carriedShulkerHoveringEnderChestResolvesToEnderChestInsert() {
        Optional<EnderChestBundlingOperation> operation = EnderChestBundlingRules.resolveOperation(
            SHULKER_1,
            ENDER_CHEST_1,
            ADAPTER
        );

        assertEquals(Optional.of(EnderChestBundlingOperation.INSERT), operation);
    }

    @Test
    void carriedEnderChestHoveringShulkerResolvesToPickupInsertIntoEnderChest() {
        Optional<EnderChestBundlingOperation> operation = EnderChestBundlingRules.resolveOperation(
            ENDER_CHEST_1,
            SHULKER_1,
            ADAPTER
        );

        assertEquals(Optional.of(EnderChestBundlingOperation.PICKUP_INSERT), operation);
    }

    @Test
    void carriedSingleEnderChestHoveringEmptySlotResolvesToExtract() {
        Optional<EnderChestBundlingOperation> operation = EnderChestBundlingRules.resolveOperation(
            ENDER_CHEST_1,
            EMPTY,
            ADAPTER
        );

        assertEquals(Optional.of(EnderChestBundlingOperation.EXTRACT), operation);
    }

    @Test
    void stackedEnderChestDoesNotResolveToExtract() {
        Optional<EnderChestBundlingOperation> operation = EnderChestBundlingRules.resolveOperation(
            ENDER_CHEST_2,
            EMPTY,
            ADAPTER
        );

        assertEquals(Optional.empty(), operation);
    }

    @Test
    void fullEnderChestRejectsInsertWithoutFallingBackToShulkerBundling() {
        FakePlayer player = new FakePlayer(contents(STONE_64, DIRT_8));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        Optional<EnderChestBundlingOperation> operation = EnderChestBundlingRules.resolveOperation(
            ENDER_CHEST_1,
            SHULKER_1,
            ADAPTER
        );
        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.pickupInsert(player, SHULKER_1);

        assertEquals(Optional.of(EnderChestBundlingOperation.PICKUP_INSERT), operation);
        assertFalse(result.success());
        assertFalse(result.changed());
        assertEquals(ShulkerBundlingFailure.NO_SPACE, result.failure());
        assertEquals(SHULKER_1, result.updatedInputStack().orElseThrow());
        assertEquals(contents(STONE_64, DIRT_8), player.enderChestContents());
    }

    @Test
    void extractFromEmptyEnderChestFailsSafely() {
        FakePlayer player = new FakePlayer(contents(EMPTY, EMPTY, EMPTY));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.extractLastStack(player);

        assertFalse(result.success());
        assertEquals(ShulkerBundlingFailure.NO_ITEMS_TO_EXTRACT, result.failure());
    }

    @Test
    void extractReturnsLastStackToEmptySlotSemantic() {
        FakePlayer player = new FakePlayer(contents(EMPTY, DIRT_8, STONE_16));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.extractLastStack(player);

        assertTrue(result.success());
        assertEquals(STONE_16, result.extractedStack().orElseThrow());
        assertTrue(player.enderChestContents().get(2).isEmpty());
    }

    @Test
    void extractCanReturnShulkerAsPlainEnderChestInventoryItem() {
        FakePlayer player = new FakePlayer(contents(EMPTY, STONE_16, SHULKER_1));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.extractLastStack(player);

        assertTrue(result.success());
        assertEquals(SHULKER_1, result.extractedStack().orElseThrow());
        assertTrue(player.enderChestContents().get(2).isEmpty());
    }

    @Test
    void totalItemCountIsConservedAcrossInsertAndExtract() {
        FakePlayer player = new FakePlayer(contents(STONE_32, EMPTY, DIRT_8));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        int beforeInsert = totalCount(player.enderChestContents()) + SHULKER_1.count();
        ShulkerBundlingResult<List<FakeStack>, FakeStack> insertResult = service.insert(player, SHULKER_1);
        int afterInsert = totalCount(player.enderChestContents()) + insertResult.updatedInputStack().orElseThrow().count();

        assertTrue(insertResult.success());
        assertEquals(beforeInsert, afterInsert);

        int beforeExtract = totalCount(player.enderChestContents());
        ShulkerBundlingResult<List<FakeStack>, FakeStack> extractResult = service.extractLastStack(player);
        int afterExtract = totalCount(player.enderChestContents()) + extractResult.extractedStack().orElseThrow().count();

        assertTrue(extractResult.success());
        assertEquals(beforeExtract, afterExtract);
    }

    @Test
    void extractSkipShulkerPicksOrdinaryItemBeforeShulkerAtTail() {
        FakePlayer player = new FakePlayer(contents(EMPTY, STONE_16, SHULKER_1));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.extractLastStack(player, true);

        assertTrue(result.success());
        assertEquals(STONE_16, result.extractedStack().orElseThrow());
        assertEquals(SHULKER_1, player.enderChestContents().get(2));
        assertTrue(player.enderChestContents().get(1).isEmpty());
    }

    @Test
    void extractSkipShulkerFailsWhenOnlyShulkersRemainAndLeavesContentsUnchanged() {
        FakePlayer player = new FakePlayer(contents(EMPTY, SHULKER_1, SHULKER_1));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();
        List<FakeStack> before = contents(EMPTY, SHULKER_1, SHULKER_1);

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.extractLastStack(player, true);

        assertFalse(result.success());
        assertFalse(result.changed());
        assertEquals(ShulkerBundlingFailure.NO_ITEMS_TO_EXTRACT, result.failure());
        assertTrue(result.extractedStack().isEmpty());
        assertEquals(before, player.enderChestContents());
    }

    @Test
    void extractWithoutSkipStillReturnsLastShulkerForPlainSlot() {
        FakePlayer player = new FakePlayer(contents(EMPTY, STONE_16, SHULKER_1));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.extractLastStack(player, false);

        assertTrue(result.success());
        assertEquals(SHULKER_1, result.extractedStack().orElseThrow());
        assertTrue(player.enderChestContents().get(2).isEmpty());
        assertEquals(STONE_16, player.enderChestContents().get(1));
    }

    @Test
    void dragPickupInsertAcceptsShulkerBoxIntoEnderChest() {
        FakePlayer player = new FakePlayer(contents(EMPTY, EMPTY, EMPTY));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.pickupInsert(player, SHULKER_1);

        assertTrue(result.success());
        assertEquals(SHULKER_1, player.enderChestContents().get(0));
        assertTrue(result.updatedInputStack().orElseThrow().isEmpty());
    }

    @Test
    void dragPickupInsertFailsWhenEnderChestFullAndLeavesContentsUnchanged() {
        FakePlayer player = new FakePlayer(contents(STONE_64, DIRT_8));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();
        List<FakeStack> before = contents(STONE_64, DIRT_8);

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.pickupInsert(player, SHULKER_1);

        assertFalse(result.success());
        assertFalse(result.changed());
        assertEquals(ShulkerBundlingFailure.NO_SPACE, result.failure());
        assertEquals(before, player.enderChestContents());
    }

    @Test
    void dragExtractOrderIsBackToFront() {
        FakePlayer player = new FakePlayer(contents(EMPTY, EMPTY, STONE_16, DIRT_8));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        ShulkerBundlingResult<List<FakeStack>, FakeStack> first = service.extractLastStack(player, false);
        assertTrue(first.success());
        assertEquals(DIRT_8, first.extractedStack().orElseThrow());

        ShulkerBundlingResult<List<FakeStack>, FakeStack> second = service.extractLastStack(player, false);
        assertTrue(second.success());
        assertEquals(STONE_16, second.extractedStack().orElseThrow());
    }

    @Test
    void dragExtractSkipShulkerKeepsShulkerInPlaceWhenExtractingOrdinary() {
        FakePlayer player = new FakePlayer(contents(EMPTY, STONE_16, SHULKER_1, DIRT_8));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.extractLastStack(player, true);

        assertTrue(result.success());
        assertEquals(DIRT_8, result.extractedStack().orElseThrow());
        assertEquals(SHULKER_1, player.enderChestContents().get(2));
        assertEquals(STONE_16, player.enderChestContents().get(1));
    }

    @Test
    void dragInsertDoesNotReorderExistingStacks() {
        FakePlayer player = new FakePlayer(contents(STONE_16, EMPTY, DIRT_8));
        PlayerEnderChestBundlingService<FakePlayer, FakeStack> service = service();

        ShulkerBundlingResult<List<FakeStack>, FakeStack> result = service.insert(player, STONE_16);

        assertTrue(result.success());
        assertEquals(STONE_32, player.enderChestContents().get(0));
        assertEquals(DIRT_8, player.enderChestContents().get(2));
    }

    private static PlayerEnderChestBundlingService<FakePlayer, FakeStack> service() {
        return new PlayerEnderChestBundlingService<>(new FakeEnderChestAccess(), ADAPTER);
    }

    private static int totalCount(List<FakeStack> stacks) {
        int total = 0;
        for (FakeStack stack : stacks) {
            total += stack.count();
        }
        return total;
    }

    private static List<FakeStack> contents(FakeStack... stacks) {
        List<FakeStack> copies = new ArrayList<>(stacks.length);
        for (FakeStack stack : stacks) {
            copies.add(stack.copy());
        }
        return copies;
    }

    private enum Kind {
        NORMAL,
        SHULKER,
        ENDER_CHEST
    }

    private record FakeStack(String key, int count, int maxStackSize, Kind kind, boolean canFitInsideContainers) {
        private FakeStack {
            if (count < 0) {
                throw new IllegalArgumentException("count must be >= 0");
            }
        }

        private FakeStack copy() {
            return new FakeStack(key, count, maxStackSize, kind, canFitInsideContainers);
        }

        private boolean isEmpty() {
            return count == 0;
        }
    }

    private record FakePlayer(List<FakeStack> enderChestContents) {
    }

    private static final class FakeEnderChestAccess implements PlayerEnderChestContentAccess<FakePlayer, FakeStack> {
        @Override
        public List<FakeStack> readPlayerEnderChestContents(FakePlayer playerHandle) {
            return contents(playerHandle.enderChestContents().toArray(FakeStack[]::new));
        }

        @Override
        public ContentWriteResult writePlayerEnderChestContents(FakePlayer playerHandle, List<FakeStack> contents) {
            playerHandle.enderChestContents().clear();
            for (FakeStack stack : contents) {
                playerHandle.enderChestContents().add(stack.copy());
            }
            return ContentWriteResult.applied("Wrote player-scoped ender chest contents.");
        }
    }

    private static final class FakeStackAdapter implements EnderChestBundlingStackAdapter<FakeStack> {
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
            return new FakeStack(stack.key(), count, stack.maxStackSize(), stack.kind(), stack.canFitInsideContainers());
        }

        @Override
        public boolean isEmpty(FakeStack stack) {
            return stack.isEmpty();
        }

        @Override
        public boolean isShulkerBox(FakeStack stack) {
            return stack.kind() == Kind.SHULKER;
        }

        @Override
        public boolean canInsertIntoShulker(FakeStack stack) {
            return !stack.isEmpty() && stack.canFitInsideContainers() && stack.kind() == Kind.NORMAL;
        }

        @Override
        public boolean canStacksMerge(FakeStack existingStack, FakeStack incomingStack) {
            return existingStack.key().equals(incomingStack.key()) && existingStack.kind() == incomingStack.kind();
        }

        @Override
        public int getCount(FakeStack stack) {
            return stack.count();
        }

        @Override
        public int getMaxStackSize(FakeStack stack) {
            return stack.maxStackSize();
        }

        @Override
        public boolean isEnderChest(FakeStack stack) {
            return stack.kind() == Kind.ENDER_CHEST;
        }

        @Override
        public boolean canInsertIntoEnderChest(FakeStack stack) {
            return !stack.isEmpty() && stack.canFitInsideContainers();
        }
    }
}
