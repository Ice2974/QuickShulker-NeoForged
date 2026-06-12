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
            case MOUSE_DRAG_INSERT -> handleMouseDragInsert(player, intent);
            case MOUSE_DRAG_PICKUP_INSERT -> handleMouseDragPickupInsert(player, intent, cursorStack);
            case EXTRACT -> handleExtract(player, intent, cursorStack);
            case MOUSE_DRAG_EXTRACT -> handleMouseDragExtract(player, intent, cursorStack);
            case TRANSFER -> handleTransfer(player, intent, cursorStack);
            case UNKNOWN -> LOGGER.debug("Rejected Forge bundling intent with unknown action: hostSlot={}", intent.hostSlot());
        }
    }

    private static void handleInsert(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        if (!ForgeQuickShulkerConfig.view().supportsBundlingInsert()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }
        if (!ForgeHostSlotResolver.canSafelyReadAndShrink(player, intent.hostSlot())) {
            LOGGER.debug("Rejected Forge bundling insert due to unsafe host slot: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack carried = resolvedCarried(player, cursorStack);
        logCreativeResolvedCarried(player, intent, cursorStack, carried, "insert");
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
        ItemStack updatedHostStack = result.updatedContainerStack().orElseThrow().copy();
        if (!ForgeHostSlotResolver.canSafelyReplace(player, intent.hostSlot(), updatedHostStack)) {
            LOGGER.debug("Rejected Forge bundling insert due to unsafe host slot writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        ForgeHostSlotResolver.set(player, intent.hostSlot(), updatedHostStack);
        player.containerMenu.setCarried(result.updatedInputStack().orElseThrow().copy());
        logCreativeSetCarried(player, intent, "insert");
        syncPlayerInventory(player);
        clearCreativeServerCarriedAfterSync(player, intent, "insert");
    }

    private static void handlePickupInsert(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        if (!ForgeQuickShulkerConfig.view().supportsBundlingPickup()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }
        if (!ForgeHostSlotResolver.canSafelyReadAndShrink(player, intent.hostSlot())) {
            LOGGER.debug("Rejected Forge pickup insert due to unsafe host slot: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack carried = resolvedCarried(player, cursorStack);
        logCreativeResolvedCarried(player, intent, cursorStack, carried, "pickup_insert");
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
        ItemStack updatedHostStack = result.updatedInputStack().orElseThrow().copy();
        if (!ForgeHostSlotResolver.canSafelyReplace(player, intent.hostSlot(), updatedHostStack)) {
            LOGGER.debug("Rejected Forge pickup insert due to unsafe host slot writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        player.containerMenu.setCarried(result.updatedContainerStack().orElseThrow().copy());
        logCreativeSetCarried(player, intent, "pickup_insert");
        ForgeHostSlotResolver.set(player, intent.hostSlot(), updatedHostStack);
        syncPlayerInventory(player);
        clearCreativeServerCarriedAfterSync(player, intent, "pickup_insert");
    }

    private static void handleMouseDragInsert(ServerPlayer player, ShulkerBundlingIntent intent) {
        if (!ForgeQuickShulkerConfig.view().supportsMouseDragged()
            || !ForgeQuickShulkerConfig.view().supportsBundlingInsert()) {
            return;
        }
        LOGGER.debug("Rejected Forge mouse dragged insert because ordinary-item drag insertion is disabled: hostSlot={}", intent.hostSlot());
    }

    private static void handleMouseDragPickupInsert(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        if (!ForgeQuickShulkerConfig.view().supportsMouseDragged()
            || !ForgeQuickShulkerConfig.view().supportsBundlingPickup()) {
            return;
        }
        handlePickupInsert(player, new ShulkerBundlingIntent(ShulkerBundlingAction.PICKUP_INSERT, intent.hostSlot()), cursorStack);
    }

    private static void handleExtract(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        handleExtractCore(player, intent, cursorStack, false);
    }

    private static void handleMouseDragExtract(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        if (!ForgeQuickShulkerConfig.view().supportsMouseDragged()) {
            return;
        }
        handleExtractCore(player, intent, cursorStack, true);
    }

    private static void handleExtractCore(
        ServerPlayer player,
        ShulkerBundlingIntent intent,
        ItemStack cursorStack,
        boolean mouseDragged
    ) {
        if (!ForgeQuickShulkerConfig.view().supportsBundlingExtract()) {
            return;
        }
        if (mouseDragged && !ForgeQuickShulkerConfig.view().supportsMouseDragged()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }
        ItemStack targetStack = ForgeHostSlotResolver.resolve(player, intent.hostSlot());
        if (!targetStack.isEmpty()) {
            LOGGER.debug("Rejected Forge extract because target slot was not empty: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack carried = resolvedCarried(player, cursorStack);
        logCreativeResolvedCarried(player, intent, cursorStack, carried, "extract");
        if (!isSingleShulkerBox(carried)) {
            LOGGER.debug("Rejected Forge extract due to non-single-shulker carried stack: creative={}, count={}, empty={}",
                player.getAbilities().instabuild, carried.getCount(), carried.isEmpty());
            return;
        }

        ShulkerBundlingResult<ItemStack, ItemStack> result = HELPER.extractFirstStack(carried);
        if (!result.changed() || result.updatedContainerStack().isEmpty() || result.extractedStack().isEmpty()) {
            LOGGER.debug("Rejected Forge extract after helper validation: failure={}, detail={}", result.failure(), result.detail());
            return;
        }

        ItemStack extractedStack = result.extractedStack().orElseThrow().copy();
        if (!ForgeHostSlotResolver.canSafelyReplace(player, intent.hostSlot(), extractedStack)) {
            LOGGER.debug("Rejected Forge extract because target slot is unsafe for extracted stack writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!ForgeHostSlotResolver.resolve(player, intent.hostSlot()).isEmpty()) {
            LOGGER.debug("Rejected Forge extract because target slot changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        player.containerMenu.setCarried(result.updatedContainerStack().orElseThrow().copy());
        logCreativeSetCarried(player, intent, "extract");
        ForgeHostSlotResolver.set(player, intent.hostSlot(), extractedStack);
        syncPlayerInventory(player);
        clearCreativeServerCarriedAfterSync(player, intent, "extract");
    }

    private static void handleTransfer(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        if (!ForgeQuickShulkerConfig.view().supportsBundlingTransfer()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }
        if (!ForgeHostSlotResolver.canSafelyReadAndShrink(player, intent.hostSlot())) {
            LOGGER.debug("Rejected Forge transfer due to unsafe host slot: hostSlot={}", intent.hostSlot());
            return;
        }
        ItemStack targetShulker = ForgeHostSlotResolver.resolve(player, intent.hostSlot());
        if (!isSingleShulkerBox(targetShulker)) {
            LOGGER.debug("Rejected Forge transfer due to non-single-shulker target stack: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack sourceShulker = resolvedCarried(player, cursorStack);
        logCreativeResolvedCarried(player, intent, cursorStack, sourceShulker, "transfer");
        if (!isSingleShulkerBox(sourceShulker)) {
            LOGGER.debug("Rejected Forge transfer due to non-single-shulker carried stack: creative={}, count={}, empty={}",
                player.getAbilities().instabuild, sourceShulker.getCount(), sourceShulker.isEmpty());
            return;
        }

        ShulkerBundlingResult<ItemStack, ItemStack> result = HELPER.transferBetweenShulkers(sourceShulker, targetShulker);
        if (!result.changed()
            || result.updatedSourceContainerStack().isEmpty()
            || result.updatedTargetContainerStack().isEmpty()) {
            LOGGER.debug("Rejected Forge transfer after helper validation: failure={}, detail={}", result.failure(), result.detail());
            return;
        }
        ItemStack updatedTargetStack = result.updatedTargetContainerStack().orElseThrow().copy();
        if (!ForgeHostSlotResolver.canSafelyReplace(player, intent.hostSlot(), updatedTargetStack)) {
            LOGGER.debug("Rejected Forge transfer due to unsafe host slot writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        if (!isSingleShulkerBox(ForgeHostSlotResolver.resolve(player, intent.hostSlot()))) {
            LOGGER.debug("Rejected Forge transfer because target slot changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        ForgeHostSlotResolver.set(player, intent.hostSlot(), updatedTargetStack);
        player.containerMenu.setCarried(result.updatedSourceContainerStack().orElseThrow().copy());
        logCreativeSetCarried(player, intent, "transfer");
        syncPlayerInventory(player);
        clearCreativeServerCarriedAfterSync(player, intent, "transfer");
    }

    private static void syncPlayerInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        if (player.getAbilities().instabuild) {
            // Creative bundling mutates the currently open menu's carried stack. Re-sending the
            // separate inventoryMenu state here can reintroduce the pre-mutation shulker copy
            // when the creative screen closes.
            player.containerMenu.broadcastFullState();
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

    private static void logCreativeResolvedCarried(
        ServerPlayer player,
        ShulkerBundlingIntent intent,
        ItemStack cursorStack,
        ItemStack resolvedCarried,
        String phase
    ) {
        if (!player.getAbilities().instabuild || !LOGGER.isDebugEnabled()) {
            return;
        }
        LOGGER.debug(
            "Forge creative bundling {} before apply: action={}, hostSlot={}, payloadCursor={}, resolvedCarried={}, menuCarried={}",
            phase,
            intent.action(),
            intent.hostSlot(),
            describeStack(cursorStack),
            describeStack(resolvedCarried),
            describeStack(player.containerMenu.getCarried())
        );
    }

    private static void logCreativeSetCarried(ServerPlayer player, ShulkerBundlingIntent intent, String phase) {
        if (!player.getAbilities().instabuild || !LOGGER.isDebugEnabled()) {
            return;
        }
        LOGGER.debug(
            "Forge creative bundling {} after setCarried: action={}, hostSlot={}, menuCarried={}",
            phase,
            intent.action(),
            intent.hostSlot(),
            describeStack(player.containerMenu.getCarried())
        );
    }

    private static void clearCreativeServerCarriedAfterSync(ServerPlayer player, ShulkerBundlingIntent intent, String phase) {
        if (!player.getAbilities().instabuild) {
            return;
        }
        if (player.containerMenu != player.inventoryMenu) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "Forge creative bundling {} kept server carried for non-inventory menu: action={}, hostSlot={}, menuClass={}, menuCarried={}",
                    phase,
                    intent.action(),
                    intent.hostSlot(),
                    player.containerMenu.getClass().getName(),
                    describeStack(player.containerMenu.getCarried())
                );
            }
            return;
        }
        player.containerMenu.setCarried(ItemStack.EMPTY);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "Forge creative bundling {} cleared server carried after sync: action={}, hostSlot={}, menuCarried={}",
                phase,
                intent.action(),
                intent.hostSlot(),
                describeStack(player.containerMenu.getCarried())
            );
        }
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        return stack.getItem().toString() + " x" + stack.getCount();
    }
}
