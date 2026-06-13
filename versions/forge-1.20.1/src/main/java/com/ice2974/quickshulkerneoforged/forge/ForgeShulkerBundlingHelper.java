package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.bundling.ShulkerBundlingFailure;
import com.ice2974.quickshulkerneoforged.common.bundling.ShulkerBundlingResult;
import com.ice2974.quickshulkerneoforged.common.bundling.ShulkerBundlingRules;
import com.ice2974.quickshulkerneoforged.common.bundling.ShulkerBundlingStackAdapter;
import com.ice2974.quickshulkerneoforged.common.content.ContentWriteResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class ForgeShulkerBundlingHelper {
    private static final ShulkerBundlingStackAdapter<ItemStack> STACK_ADAPTER = new ItemStackAdapter();

    private final ForgeShulkerContentAccess contentAccess;

    public ForgeShulkerBundlingHelper() {
        this(new ForgeShulkerContentAccess());
    }

    public ForgeShulkerBundlingHelper(ForgeShulkerContentAccess contentAccess) {
        this.contentAccess = contentAccess;
    }

    public NonNullList<ItemStack> readItemStacksCopy(ItemStack shulkerStack) {
        return copyToNonNullList(contentAccess.readItemStacks(shulkerStack));
    }

    public ContentWriteResult writeItemStacksCopy(ItemStack shulkerStack, NonNullList<ItemStack> items) {
        return contentAccess.writeItemStacks(shulkerStack, copyToNonNullList(items));
    }

    public ShulkerBundlingResult<ItemStack, ItemStack> insertIntoShulker(ItemStack shulkerStack, ItemStack inputStack) {
        ShulkerBundlingResult<ItemStack, ItemStack> validationFailure =
            validateSingleShulker(shulkerStack, false, "Cannot insert into a non-single shulker host.");
        if (validationFailure != null) {
            return validationFailure;
        }

        ItemStack updatedShulker = shulkerStack.copy();
        ShulkerBundlingResult<List<ItemStack>, ItemStack> ruleResult = ShulkerBundlingRules.insertIntoContents(
            copyToList(contentAccess.readItemStacks(updatedShulker)),
            inputStack.copy(),
            STACK_ADAPTER
        );
        if (!ruleResult.changed()) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                ruleResult.failure(),
                0,
                Optional.of(updatedShulker),
                Optional.empty(),
                Optional.empty(),
                ruleResult.updatedInputStack(),
                Optional.empty(),
                ruleResult.detail()
            );
        }

        ContentWriteResult writeResult = contentAccess.writeItemStacks(
            updatedShulker,
            toNonNullList(ruleResult.updatedContainerStack().orElseThrow())
        );
        return mapSingleContainerResult(ruleResult, updatedShulker, writeResult);
    }

    public ShulkerBundlingResult<ItemStack, ItemStack> extractFirstStack(ItemStack shulkerStack) {
        ShulkerBundlingResult<ItemStack, ItemStack> validationFailure =
            validateSingleShulker(shulkerStack, true, "Cannot extract from a non-single shulker host.");
        if (validationFailure != null) {
            return validationFailure;
        }

        ItemStack updatedShulker = shulkerStack.copy();
        ShulkerBundlingResult<List<ItemStack>, ItemStack> ruleResult = ShulkerBundlingRules.extractFirstStack(
            copyToList(contentAccess.readItemStacks(updatedShulker)),
            STACK_ADAPTER
        );
        if (!ruleResult.changed()) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                ruleResult.failure(),
                0,
                Optional.of(updatedShulker),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ruleResult.detail()
            );
        }

        ContentWriteResult writeResult = contentAccess.writeItemStacks(
            updatedShulker,
            toNonNullList(ruleResult.updatedContainerStack().orElseThrow())
        );
        return mapSingleContainerResult(ruleResult, updatedShulker, writeResult);
    }

    public ShulkerBundlingResult<ItemStack, ItemStack> extractLastStack(ItemStack shulkerStack) {
        ShulkerBundlingResult<ItemStack, ItemStack> validationFailure =
            validateSingleShulker(shulkerStack, true, "Cannot extract from a non-single shulker host.");
        if (validationFailure != null) {
            return validationFailure;
        }

        ItemStack updatedShulker = shulkerStack.copy();
        ShulkerBundlingResult<List<ItemStack>, ItemStack> ruleResult = ShulkerBundlingRules.extractLastStack(
            copyToList(contentAccess.readItemStacks(updatedShulker)),
            STACK_ADAPTER
        );
        if (!ruleResult.changed()) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                ruleResult.failure(),
                0,
                Optional.of(updatedShulker),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ruleResult.detail()
            );
        }

        ContentWriteResult writeResult = contentAccess.writeItemStacks(
            updatedShulker,
            toNonNullList(ruleResult.updatedContainerStack().orElseThrow())
        );
        return mapSingleContainerResult(ruleResult, updatedShulker, writeResult);
    }

    // Callers must ensure source and target do not point at the same live host slot before applying results.
    public ShulkerBundlingResult<ItemStack, ItemStack> transferBetweenShulkers(ItemStack sourceShulker, ItemStack targetShulker) {
        ShulkerBundlingResult<ItemStack, ItemStack> sourceFailure =
            validateSingleShulker(sourceShulker, true, "Cannot transfer from a non-single shulker host.");
        if (sourceFailure != null) {
            return sourceFailure;
        }
        ShulkerBundlingResult<ItemStack, ItemStack> targetFailure =
            validateSingleShulker(targetShulker, false, "Cannot transfer into a non-single shulker host.");
        if (targetFailure != null) {
            return targetFailure;
        }

        ItemStack updatedSource = sourceShulker.copy();
        ItemStack updatedTarget = targetShulker.copy();
        ShulkerBundlingResult<List<ItemStack>, ItemStack> ruleResult = ShulkerBundlingRules.transferContents(
            copyToList(contentAccess.readItemStacks(updatedSource)),
            copyToList(contentAccess.readItemStacks(updatedTarget)),
            STACK_ADAPTER
        );
        if (!ruleResult.changed()) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                ruleResult.failure(),
                0,
                Optional.empty(),
                Optional.of(updatedSource),
                Optional.of(updatedTarget),
                Optional.empty(),
                Optional.empty(),
                ruleResult.detail()
            );
        }

        ContentWriteResult sourceWrite = contentAccess.writeItemStacks(
            updatedSource,
            toNonNullList(ruleResult.updatedSourceContainerStack().orElseThrow())
        );
        if (!sourceWrite.applied()) {
            return writeRejectedTransfer(updatedSource, updatedTarget, sourceWrite.detail());
        }

        ContentWriteResult targetWrite = contentAccess.writeItemStacks(
            updatedTarget,
            toNonNullList(ruleResult.updatedTargetContainerStack().orElseThrow())
        );
        if (!targetWrite.applied()) {
            return writeRejectedTransfer(updatedSource, updatedTarget, targetWrite.detail());
        }

        return new ShulkerBundlingResult<>(
            true,
            true,
            ShulkerBundlingFailure.NONE,
            ruleResult.movedCount(),
            Optional.empty(),
            Optional.of(updatedSource),
            Optional.of(updatedTarget),
            Optional.empty(),
            Optional.empty(),
            ruleResult.detail()
        );
    }

    private ShulkerBundlingResult<ItemStack, ItemStack> mapSingleContainerResult(
        ShulkerBundlingResult<List<ItemStack>, ItemStack> ruleResult,
        ItemStack updatedShulker,
        ContentWriteResult writeResult
    ) {
        if (!writeResult.applied()) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                ShulkerBundlingFailure.WRITE_REJECTED,
                0,
                Optional.of(updatedShulker),
                Optional.empty(),
                Optional.empty(),
                ruleResult.updatedInputStack(),
                ruleResult.extractedStack(),
                writeResult.detail()
            );
        }

        return new ShulkerBundlingResult<>(
            true,
            true,
            ShulkerBundlingFailure.NONE,
            ruleResult.movedCount(),
            Optional.of(updatedShulker),
            Optional.empty(),
            Optional.empty(),
            ruleResult.updatedInputStack(),
            ruleResult.extractedStack(),
            writeResult.detail()
        );
    }

    private ShulkerBundlingResult<ItemStack, ItemStack> validateSingleShulker(
        ItemStack shulkerStack,
        boolean source,
        String detail
    ) {
        if (shulkerStack.isEmpty()) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                source ? ShulkerBundlingFailure.SOURCE_EMPTY : ShulkerBundlingFailure.TARGET_EMPTY,
                0,
                Optional.of(ItemStack.EMPTY),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                detail
            );
        }
        if (!(Block.byItem(shulkerStack.getItem()) instanceof ShulkerBoxBlock)) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                source ? ShulkerBundlingFailure.SOURCE_NOT_SHULKER : ShulkerBundlingFailure.TARGET_NOT_SHULKER,
                0,
                Optional.of(shulkerStack.copy()),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                detail
            );
        }
        if (shulkerStack.getCount() != 1) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                source ? ShulkerBundlingFailure.SOURCE_STACK_NOT_SINGLE : ShulkerBundlingFailure.TARGET_STACK_NOT_SINGLE,
                0,
                Optional.of(shulkerStack.copy()),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                detail
            );
        }
        return null;
    }

    private ShulkerBundlingResult<ItemStack, ItemStack> writeRejectedTransfer(
        ItemStack source,
        ItemStack target,
        String detail
    ) {
        return new ShulkerBundlingResult<>(
            false,
            false,
            ShulkerBundlingFailure.WRITE_REJECTED,
            0,
            Optional.empty(),
            Optional.of(source),
            Optional.of(target),
            Optional.empty(),
            Optional.empty(),
            detail
        );
    }

    private static List<ItemStack> copyToList(NonNullList<ItemStack> items) {
        List<ItemStack> copies = new ArrayList<>(items.size());
        for (ItemStack item : items) {
            copies.add(item.copy());
        }
        return copies;
    }

    private static NonNullList<ItemStack> toNonNullList(List<ItemStack> items) {
        NonNullList<ItemStack> copies = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            copies.set(i, items.get(i).copy());
        }
        return copies;
    }

    private static NonNullList<ItemStack> copyToNonNullList(NonNullList<ItemStack> items) {
        return toNonNullList(copyToList(items));
    }

    private static final class ItemStackAdapter implements ShulkerBundlingStackAdapter<ItemStack> {
        @Override
        public ItemStack empty() {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack copy(ItemStack stack) {
            return stack.copy();
        }

        @Override
        public ItemStack copyWithCount(ItemStack stack, int count) {
            ItemStack copy = stack.copy();
            copy.setCount(count);
            return copy;
        }

        @Override
        public boolean isEmpty(ItemStack stack) {
            return stack.isEmpty();
        }

        @Override
        public boolean isShulkerBox(ItemStack stack) {
            return Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock;
        }

        @Override
        public boolean canInsertIntoShulker(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem().canFitInsideContainerItems() && !isShulkerBox(stack);
        }

        @Override
        public boolean canStacksMerge(ItemStack existingStack, ItemStack incomingStack) {
            return ItemStack.isSameItemSameTags(existingStack, incomingStack);
        }

        @Override
        public int getCount(ItemStack stack) {
            return stack.getCount();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return stack.getMaxStackSize();
        }
    }
}
