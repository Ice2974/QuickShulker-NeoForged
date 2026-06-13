package com.ice2974.quickshulkerneoforged.common.bundling;

import com.ice2974.quickshulkerneoforged.common.content.ContentWriteResult;
import java.util.List;
import java.util.Optional;

public final class PlayerEnderChestBundlingService<P, S> {
    private final PlayerEnderChestContentAccess<P, S> contentAccess;
    private final EnderChestBundlingStackAdapter<S> adapter;

    public PlayerEnderChestBundlingService(
        PlayerEnderChestContentAccess<P, S> contentAccess,
        EnderChestBundlingStackAdapter<S> adapter
    ) {
        this.contentAccess = contentAccess;
        this.adapter = adapter;
    }

    public ShulkerBundlingResult<List<S>, S> insert(P playerHandle, S inputStack) {
        ShulkerBundlingResult<List<S>, S> ruleResult = EnderChestBundlingRules.insertIntoPlayerEnderChest(
            contentAccess.readPlayerEnderChestContents(playerHandle),
            inputStack,
            adapter
        );
        return applyWriteback(playerHandle, ruleResult);
    }

    public ShulkerBundlingResult<List<S>, S> pickupInsert(P playerHandle, S slotStack) {
        ShulkerBundlingResult<List<S>, S> ruleResult = EnderChestBundlingRules.pickupInsertIntoPlayerEnderChest(
            contentAccess.readPlayerEnderChestContents(playerHandle),
            slotStack,
            adapter
        );
        return applyWriteback(playerHandle, ruleResult);
    }

    public ShulkerBundlingResult<List<S>, S> extractFirstStack(P playerHandle) {
        ShulkerBundlingResult<List<S>, S> ruleResult = EnderChestBundlingRules.extractFirstStackFromPlayerEnderChest(
            contentAccess.readPlayerEnderChestContents(playerHandle),
            adapter
        );
        return applyWriteback(playerHandle, ruleResult);
    }

    private ShulkerBundlingResult<List<S>, S> applyWriteback(
        P playerHandle,
        ShulkerBundlingResult<List<S>, S> ruleResult
    ) {
        if (!ruleResult.changed()) {
            return ruleResult;
        }

        ContentWriteResult writeResult = contentAccess.writePlayerEnderChestContents(
            playerHandle,
            ruleResult.updatedContainerStack().orElseThrow()
        );
        if (!writeResult.applied()) {
            return new ShulkerBundlingResult<>(
                false,
                false,
                ShulkerBundlingFailure.WRITE_REJECTED,
                0,
                ruleResult.updatedContainerStack(),
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
            ruleResult.updatedContainerStack(),
            Optional.empty(),
            Optional.empty(),
            ruleResult.updatedInputStack(),
            ruleResult.extractedStack(),
            writeResult.detail()
        );
    }
}
