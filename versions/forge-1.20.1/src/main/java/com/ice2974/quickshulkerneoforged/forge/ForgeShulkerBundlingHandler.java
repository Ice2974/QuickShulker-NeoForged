package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.bundling.ShulkerBundlingResult;
import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingAction;
import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingIntent;
import com.ice2974.quickshulkerneoforged.common.open.HostIdentity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ForgeShulkerBundlingHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeShulkerBundlingHandler.class);
    private static final ForgeShulkerBundlingHelper HELPER = new ForgeShulkerBundlingHelper();

    private ForgeShulkerBundlingHandler() {
    }

    public static void handle(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        if (player == null) {
            return;
        }

        switch (intent.action()) {
            case INSERT -> handleInsert(player, intent, cursorStack);
            case PICKUP_INSERT -> handlePickupInsert(player, intent, cursorStack);
        }
    }

    private static void handleInsert(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        if (!ForgeQuickShulkerConfig.view().supportsBundlingInsert()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }

        ItemStack carried = resolvedCarried(player, cursorStack);
        if (carried.isEmpty() || isShulkerBox(carried)) {
            LOGGER.debug("Rejected Forge creative/player bundling insert due to invalid carried stack: creative={}, empty={}, shulker={}",
                player.getAbilities().instabuild, carried.isEmpty(), isShulkerBox(carried));
            return;
        }

        ItemStack hostStack = ForgeHostSlotResolver.resolve(player, intent.hostSlot());
        ShulkerBundlingResult<ItemStack, ItemStack> result = HELPER.insertIntoShulker(hostStack, carried);
        if (!result.changed()) {
            LOGGER.debug("Rejected Forge bundling insert after helper validation: failure={}, detail={}", result.failure(), result.detail());
            return;
        }

        ForgeHostSlotResolver.set(player, intent.hostSlot(), result.updatedContainerStack().orElseThrow().copy());
        player.containerMenu.setCarried(result.updatedInputStack().orElseThrow().copy());
        syncPlayerInventory(player);
    }

    private static void handlePickupInsert(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        if (!ForgeQuickShulkerConfig.view().supportsBundlingPickup()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }

        ItemStack carried = resolvedCarried(player, cursorStack);
        if (!isSingleShulkerBox(carried)) {
            LOGGER.debug("Rejected Forge pickup insert due to non-single-shulker carried stack: creative={}, count={}, empty={}",
                player.getAbilities().instabuild, carried.getCount(), carried.isEmpty());
            return;
        }

        ItemStack hostStack = ForgeHostSlotResolver.resolve(player, intent.hostSlot());
        if (hostStack.isEmpty() || isShulkerBox(hostStack) || !hostStack.getItem().canFitInsideContainerItems()) {
            return;
        }

        ShulkerBundlingResult<ItemStack, ItemStack> result = HELPER.insertIntoShulker(carried, hostStack);
        if (!result.changed()) {
            LOGGER.debug("Rejected Forge pickup insert after helper validation: failure={}, detail={}", result.failure(), result.detail());
            return;
        }

        player.containerMenu.setCarried(result.updatedContainerStack().orElseThrow().copy());
        ForgeHostSlotResolver.set(player, intent.hostSlot(), result.updatedInputStack().orElseThrow().copy());
        syncPlayerInventory(player);
    }

    private static void syncPlayerInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        if (player.getAbilities().instabuild) {
            player.containerMenu.broadcastFullState();
            player.inventoryMenu.sendAllDataToRemote();
        }
    }

    private static ItemStack resolvedCarried(ServerPlayer player, ItemStack cursorStack) {
        if (player.getAbilities().instabuild) {
            return cursorStack == null ? ItemStack.EMPTY : cursorStack.copy();
        }
        return player.containerMenu.getCarried().copy();
    }

    private static boolean isSingleShulkerBox(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() == 1 && isShulkerBox(stack);
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return !stack.isEmpty() && Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock;
    }

    private static boolean isCurrentQuickOpenHost(ServerPlayer player, ShulkerBundlingIntent intent) {
        return player.containerMenu instanceof ForgeQuickOpenMenu quickOpenMenu
            && HostIdentity.sameSlot(quickOpenMenu.hostSlotRef(), intent.hostSlot());
    }
}
