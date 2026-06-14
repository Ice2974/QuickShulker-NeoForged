package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.bundling.EnderChestBundlingStackAdapter;
import com.ice2974.quickshulkerneoforged.common.bundling.EnderChestBundlingRules;
import com.ice2974.quickshulkerneoforged.common.bundling.PlayerEnderChestBundlingService;
import com.ice2974.quickshulkerneoforged.common.bundling.ShulkerBundlingResult;
import com.ice2974.quickshulkerneoforged.common.content.ContentWriteResult;
import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingAction;
import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingIntent;
import com.ice2974.quickshulkerneoforged.common.open.HostIdentity;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.HostStorageScope;
import com.ice2974.quickshulkerneoforged.forge.network.ForgeQuickShulkerNetwork;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ForgeShulkerBundlingHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeShulkerBundlingHandler.class);
    private static final ForgeShulkerBundlingHelper HELPER = new ForgeShulkerBundlingHelper();
    private static final ForgePlayerEnderChestContentAccess ENDER_CHEST_ACCESS = new ForgePlayerEnderChestContentAccess();
    private static final EnderChestBundlingStackAdapter<ItemStack> ENDER_CHEST_ADAPTER = new EnderChestItemStackAdapter();
    private static final PlayerEnderChestBundlingService<net.minecraft.world.entity.player.Player, ItemStack>
        ENDER_CHEST_SERVICE = new PlayerEnderChestBundlingService<>(
            ENDER_CHEST_ACCESS,
            ENDER_CHEST_ADAPTER
        );
    private static final int DRAG_SESSION_TIMEOUT_TICKS = 200;
    private static final Map<UUID, DragSession> DRAG_SESSIONS = new HashMap<>();

    private ForgeShulkerBundlingHandler() {
    }

    public static void handle(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        if (player == null) {
            return;
        }
        pruneDragSessions(player);
        if (intent.action() == ShulkerBundlingAction.END_MOUSE_DRAG) {
            clearMatchingDragSession(player, intent);
            logCreativeCarriedPreserved(player, intent, "end_mouse_drag");
            return;
        }
        if (intent.action() == ShulkerBundlingAction.MOUSE_DRAG_INSERT) {
            LOGGER.debug(
                "Rejected Forge mouse dragged insert because Forge 1.20.1 does not support dragging ordinary carried stacks into shulkers: action={}, hostSlot={}, packetContainerId={}",
                intent.action(),
                intent.hostSlot(),
                intent.containerId()
            );
            return;
        }
        if (!matchesCurrentContainer(player, intent)) {
            LOGGER.debug(
                "Rejected Forge bundling intent for stale container: action={}, packetContainerId={}, currentContainerId={}, hostSlot={}",
                intent.action(),
                intent.containerId(),
                player.containerMenu.containerId,
                intent.hostSlot()
            );
            return;
        }
        if (isEnderChestBundlingAction(intent.action()) && !isEnderChestDragAction(intent.action())) {
            DRAG_SESSIONS.remove(player.getUUID());
            handleEnderChestBundling(player, intent, cursorStack);
            return;
        }

        DragSession dragSession = null;
        if (isDragSessionIntent(intent)) {
            dragSession = resolveDragSession(player, intent, cursorStack);
            if (dragSession == null) {
                return;
            }
            if (!dragSession.markProcessed(intent.action(), intent.hostSlot())) {
                LOGGER.debug(
                    "Rejected duplicate Forge mouse dragged bundling slot: action={}, hostSlot={}, packetContainerId={}, dragId={}",
                    intent.action(),
                    intent.hostSlot(),
                    intent.containerId(),
                    intent.dragId()
                );
                return;
            }
        } else {
            DRAG_SESSIONS.remove(player.getUUID());
        }

        switch (intent.action()) {
            case INSERT -> handleInsert(player, intent, cursorStack);
            case PICKUP_INSERT -> handlePickupInsert(player, intent, cursorStack, dragSession);
            case MOUSE_DRAG_INSERT -> handleMouseDragInsert(player, intent);
            case MOUSE_DRAG_PICKUP_INSERT -> handleMouseDragPickupInsert(player, intent, cursorStack, dragSession);
            case EXTRACT -> handleExtract(player, intent, cursorStack, dragSession);
            case MOUSE_DRAG_EXTRACT -> handleMouseDragExtract(player, intent, cursorStack, dragSession);
            case END_MOUSE_DRAG -> {
            }
            case TRANSFER -> handleTransfer(player, intent, cursorStack);
            case ENDER_CHEST_INSERT, ENDER_CHEST_PICKUP_INSERT, ENDER_CHEST_EXTRACT -> handleEnderChestBundling(player, intent, cursorStack, dragSession);
            case MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT -> handleMouseDragEnderChestPickupInsert(player, intent, cursorStack, dragSession);
            case MOUSE_DRAG_ENDER_CHEST_EXTRACT -> handleMouseDragEnderChestExtract(player, intent, cursorStack, dragSession);
            case UNKNOWN -> LOGGER.debug("Rejected Forge bundling intent with unknown action: hostSlot={}", intent.hostSlot());
        }
    }

    public static void tickDragSession(ServerPlayer player) {
        if (player != null) {
            pruneDragSessions(player);
        }
    }

    public static void clearDragSession(ServerPlayer player) {
        if (player != null) {
            DRAG_SESSIONS.remove(player.getUUID());
            logCreativeCarriedPreserved(player, null, "clear_drag_session");
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

        ItemStack updatedCarried = result.updatedInputStack().orElseThrow().copy();
        ForgeHostSlotResolver.set(player, intent.hostSlot(), updatedHostStack);
        writeCarried(player, updatedCarried, null);
        logCreativeSetCarried(player, intent, "insert");
        syncPlayerInventory(player);
        syncCreativeCursor(player, updatedCarried);
        finishCreativeServerCarriedAfterSync(player, intent, "insert");
    }

    private static void handlePickupInsert(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack, DragSession dragSession) {
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

        ItemStack carried = resolvedCarried(player, cursorStack, dragSession);
        logCreativeResolvedCarried(player, intent, cursorStack, carried, "pickup_insert");
        if (!isSingleShulkerBox(carried)) {
            LOGGER.debug("Rejected Forge pickup insert due to non-single-shulker carried stack: creative={}, count={}, empty={}",
                player.getAbilities().instabuild, carried.getCount(), carried.isEmpty());
            return;
        }

        ItemStack hostStack = ForgeHostSlotResolver.resolve(player, intent.hostSlot()).copy();
        if (hostStack.isEmpty() || isShulkerBox(hostStack) || !hostStack.getItem().canFitInsideContainerItems()) {
            return;
        }
        int targetBefore = hostStack.getCount();
        int shulkerBefore = countShulkerItems(carried);

        ShulkerBundlingResult<ItemStack, ItemStack> result = HELPER.insertIntoShulker(carried, hostStack);
        if (!result.changed()) {
            LOGGER.debug("Rejected Forge pickup insert after helper validation: failure={}, detail={}", result.failure(), result.detail());
            return;
        }
        ItemStack updatedHostStack = result.updatedInputStack().orElseThrow().copy();
        ItemStack updatedCarried = result.updatedContainerStack().orElseThrow().copy();
        if (!isConservedPickupInsert(targetBefore, shulkerBefore, updatedHostStack, updatedCarried)) {
            LOGGER.warn("Rejected Forge pickup insert due to conservation check failure: action={}, hostSlot={}", intent.action(), intent.hostSlot());
            return;
        }
        if (!ItemStack.matches(hostStack, ForgeHostSlotResolver.resolve(player, intent.hostSlot()))) {
            LOGGER.debug("Rejected Forge pickup insert because target slot changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!carriedStillMatches(player, carried, dragSession)) {
            LOGGER.debug("Rejected Forge pickup insert because carried stack changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!ForgeHostSlotResolver.canSafelyReplace(player, intent.hostSlot(), updatedHostStack)) {
            LOGGER.debug("Rejected Forge pickup insert due to unsafe host slot writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        writeCarried(player, updatedCarried, dragSession);
        logCreativeSetCarried(player, intent, "pickup_insert");
        ForgeHostSlotResolver.set(player, intent.hostSlot(), updatedHostStack);
        syncPlayerInventory(player);
        syncCreativeCursor(player, updatedCarried);
        finishCreativeServerCarriedAfterSync(player, intent, "pickup_insert");
    }

    private static void handleMouseDragInsert(ServerPlayer player, ShulkerBundlingIntent intent) {
        if (!ForgeQuickShulkerConfig.view().supportsMouseDragged()
            || !ForgeQuickShulkerConfig.view().supportsBundlingInsert()) {
            return;
        }
        LOGGER.debug("Rejected Forge mouse dragged insert because ordinary-item drag insertion is disabled: hostSlot={}", intent.hostSlot());
    }

    private static void handleMouseDragPickupInsert(
        ServerPlayer player,
        ShulkerBundlingIntent intent,
        ItemStack cursorStack,
        DragSession dragSession
    ) {
        if (!ForgeQuickShulkerConfig.view().supportsMouseDragged()
            || !ForgeQuickShulkerConfig.view().supportsBundlingPickup()) {
            return;
        }
        handlePickupInsert(player, new ShulkerBundlingIntent(
            ShulkerBundlingAction.PICKUP_INSERT,
            intent.hostSlot(),
            intent.containerId(),
            intent.dragId()
        ), cursorStack, dragSession);
    }

    private static void handleExtract(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack, DragSession dragSession) {
        handleExtractCore(player, intent, cursorStack, false, dragSession);
    }

    private static void handleMouseDragExtract(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack, DragSession dragSession) {
        if (!ForgeQuickShulkerConfig.view().supportsMouseDragged()) {
            return;
        }
        handleExtractCore(player, intent, cursorStack, true, dragSession);
    }

    private static void handleExtractCore(
        ServerPlayer player,
        ShulkerBundlingIntent intent,
        ItemStack cursorStack,
        boolean mouseDragged,
        DragSession dragSession
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
        ItemStack targetStack = ForgeHostSlotResolver.resolve(player, intent.hostSlot()).copy();
        if (!targetStack.isEmpty()) {
            LOGGER.debug("Rejected Forge extract because target slot was not empty: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack carried = resolvedCarried(player, cursorStack, dragSession);
        logCreativeResolvedCarried(player, intent, cursorStack, carried, "extract");
        if (!isSingleShulkerBox(carried)) {
            LOGGER.debug("Rejected Forge extract due to non-single-shulker carried stack: creative={}, count={}, empty={}",
                player.getAbilities().instabuild, carried.getCount(), carried.isEmpty());
            return;
        }
        int shulkerBefore = countShulkerItems(carried);

        ShulkerBundlingResult<ItemStack, ItemStack> result = HELPER.extractLastStack(carried);
        if (!result.changed() || result.updatedContainerStack().isEmpty() || result.extractedStack().isEmpty()) {
            LOGGER.debug("Rejected Forge extract after helper validation: failure={}, detail={}", result.failure(), result.detail());
            return;
        }

        ItemStack extractedStack = result.extractedStack().orElseThrow().copy();
        ItemStack updatedCarried = result.updatedContainerStack().orElseThrow().copy();
        if (!isConservedExtract(shulkerBefore, updatedCarried, extractedStack)) {
            LOGGER.warn("Rejected Forge extract due to conservation check failure: action={}, hostSlot={}", intent.action(), intent.hostSlot());
            return;
        }
        if (!ForgeHostSlotResolver.canSafelyReplace(player, intent.hostSlot(), extractedStack)) {
            LOGGER.debug("Rejected Forge extract because target slot is unsafe for extracted stack writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!ForgeHostSlotResolver.resolve(player, intent.hostSlot()).isEmpty()) {
            LOGGER.debug("Rejected Forge extract because target slot changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!carriedStillMatches(player, carried, dragSession)) {
            LOGGER.debug("Rejected Forge extract because carried stack changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        writeCarried(player, updatedCarried, dragSession);
        logCreativeSetCarried(player, intent, "extract");
        ForgeHostSlotResolver.set(player, intent.hostSlot(), extractedStack);
        syncPlayerInventory(player);
        syncCreativeCursor(player, updatedCarried);
        finishCreativeServerCarriedAfterSync(player, intent, "extract");
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

        ItemStack updatedCarried = result.updatedSourceContainerStack().orElseThrow().copy();
        ForgeHostSlotResolver.set(player, intent.hostSlot(), updatedTargetStack);
        writeCarried(player, updatedCarried, null);
        logCreativeSetCarried(player, intent, "transfer");
        syncPlayerInventory(player);
        syncCreativeCursor(player, updatedCarried);
        finishCreativeServerCarriedAfterSync(player, intent, "transfer");
    }

    private static void handleEnderChestBundling(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        handleEnderChestBundling(player, intent, cursorStack, null);
    }

    private static void handleEnderChestBundling(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack, DragSession dragSession) {
        if (!supportsEnderChestBundling()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }

        switch (intent.action()) {
            case ENDER_CHEST_INSERT -> handleEnderChestInsert(player, intent, cursorStack);
            case ENDER_CHEST_PICKUP_INSERT -> handleEnderChestPickupInsert(player, intent, cursorStack, dragSession);
            case ENDER_CHEST_EXTRACT -> handleEnderChestExtract(player, intent, cursorStack, dragSession);
            default -> {
            }
        }
    }

    private static boolean supportsEnderChestBundling() {
        return ForgeQuickShulkerConfig.view().enderChestBundlingInsert()
            || ForgeQuickShulkerConfig.view().enderChestBundlingPickup()
            || ForgeQuickShulkerConfig.view().enderChestBundlingExtract()
            || ForgeQuickShulkerConfig.view().enderChestMouseDragged();
    }

    private static void handleEnderChestInsert(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        if (!supportsEnderChestBundling()) {
            return;
        }
        if (!ForgeQuickShulkerConfig.view().enderChestBundlingInsert()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }
        if (!ForgeHostSlotResolver.canSafelyReadAndShrink(player, intent.hostSlot())) {
            LOGGER.debug("Rejected Forge ender chest insert due to unsafe target slot: hostSlot={}", intent.hostSlot());
            return;
        }
        ItemStack enderChestStack = ForgeHostSlotResolver.resolve(player, intent.hostSlot()).copy();
        if (!isSingleEnderChest(enderChestStack)) {
            LOGGER.debug("Rejected Forge ender chest insert because hovered slot is not a single ender chest: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack carried = resolvedCarried(player, cursorStack);
        if (isEnderChest(carried)) {
            LOGGER.debug("Rejected Forge ender chest insert because carried ender chest cannot be inserted into ender chest: carried={}", describeStack(carried));
            return;
        }
        if (carried.isEmpty() || !canInsertIntoEnderChest(carried)) {
            LOGGER.debug("Rejected Forge ender chest insert due to invalid carried stack: carried={}", describeStack(carried));
            return;
        }

        ShulkerBundlingResult<List<ItemStack>, ItemStack> result = ENDER_CHEST_SERVICE.insert(player, carried);
        if (!result.changed()) {
            LOGGER.debug("Rejected Forge ender chest insert after service validation: failure={}, detail={}", result.failure(), result.detail());
            return;
        }
        if (!isSingleEnderChest(ForgeHostSlotResolver.resolve(player, intent.hostSlot()))) {
            LOGGER.debug("Rejected Forge ender chest insert because hovered slot changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!carriedStillMatches(player, carried, null)) {
            LOGGER.debug("Rejected Forge ender chest insert because carried stack changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack updatedCarried = result.updatedInputStack().orElseThrow().copy();
        writeCarried(player, updatedCarried, null);
        syncPlayerInventory(player);
        syncCreativeCursor(player, updatedCarried);
        finishCreativeServerCarriedAfterSync(player, intent, "ender_chest_insert");
    }

    private static void handleEnderChestPickupInsert(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        handleEnderChestPickupInsert(player, intent, cursorStack, null, true);
    }

    private static void handleMouseDragEnderChestPickupInsert(
        ServerPlayer player,
        ShulkerBundlingIntent intent,
        ItemStack cursorStack,
        DragSession dragSession
    ) {
        if (!ForgeQuickShulkerConfig.view().enderChestMouseDragged()) {
            return;
        }
        handleEnderChestPickupInsert(player, new ShulkerBundlingIntent(
            ShulkerBundlingAction.ENDER_CHEST_PICKUP_INSERT,
            intent.hostSlot(),
            intent.containerId(),
            intent.dragId()
        ), cursorStack, dragSession, false);
    }

    private static void handleEnderChestPickupInsert(
        ServerPlayer player,
        ShulkerBundlingIntent intent,
        ItemStack cursorStack,
        DragSession dragSession
    ) {
        handleEnderChestPickupInsert(player, intent, cursorStack, dragSession, true);
    }

    private static void handleEnderChestPickupInsert(
        ServerPlayer player,
        ShulkerBundlingIntent intent,
        ItemStack cursorStack,
        DragSession dragSession,
        boolean requireActionConfig
    ) {
        if (!supportsEnderChestBundling()) {
            return;
        }
        if (requireActionConfig && !ForgeQuickShulkerConfig.view().enderChestBundlingPickup()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }
        if (!ForgeHostSlotResolver.canSafelyReadAndShrink(player, intent.hostSlot())) {
            LOGGER.debug("Rejected Forge ender chest pickup insert due to unsafe target slot: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack carried = resolvedCarried(player, cursorStack, dragSession);
        if (!isSingleEnderChest(carried)) {
            LOGGER.debug("Rejected Forge ender chest pickup insert because carried stack is not a single ender chest: carried={}", describeStack(carried));
            return;
        }
        ItemStack targetStack = ForgeHostSlotResolver.resolve(player, intent.hostSlot()).copy();
        if (isEnderChest(targetStack)) {
            LOGGER.debug("Rejected Forge ender chest pickup insert because target ender chest cannot be inserted into ender chest: hostSlot={}, target={}",
                intent.hostSlot(), describeStack(targetStack));
            return;
        }
        if (targetStack.isEmpty() || !canInsertIntoEnderChest(targetStack)) {
            LOGGER.debug("Rejected Forge ender chest pickup insert due to invalid target stack: hostSlot={}, target={}",
                intent.hostSlot(), describeStack(targetStack));
            return;
        }

        ShulkerBundlingResult<List<ItemStack>, ItemStack> result = ENDER_CHEST_SERVICE.pickupInsert(player, targetStack);
        if (!result.changed()) {
            LOGGER.debug("Rejected Forge ender chest pickup insert after service validation: failure={}, detail={}", result.failure(), result.detail());
            return;
        }
        ItemStack updatedTargetStack = result.updatedInputStack().orElseThrow().copy();
        if (!ItemStack.matches(targetStack, ForgeHostSlotResolver.resolve(player, intent.hostSlot()))) {
            LOGGER.debug("Rejected Forge ender chest pickup insert because target slot changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!carriedStillMatches(player, carried, dragSession)) {
            LOGGER.debug("Rejected Forge ender chest pickup insert because carried stack changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!ForgeHostSlotResolver.canSafelyReplace(player, intent.hostSlot(), updatedTargetStack)) {
            LOGGER.debug("Rejected Forge ender chest pickup insert due to unsafe target slot writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        ForgeHostSlotResolver.set(player, intent.hostSlot(), updatedTargetStack);
        syncPlayerInventory(player);
        syncCreativeCursor(player, carried);
        finishCreativeServerCarriedAfterSync(player, intent, "ender_chest_pickup_insert");
    }

    private static void handleEnderChestExtract(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        handleEnderChestExtract(player, intent, cursorStack, null, true);
    }

    private static void handleMouseDragEnderChestExtract(
        ServerPlayer player,
        ShulkerBundlingIntent intent,
        ItemStack cursorStack,
        DragSession dragSession
    ) {
        if (!ForgeQuickShulkerConfig.view().enderChestMouseDragged()) {
            return;
        }
        handleEnderChestExtract(player, new ShulkerBundlingIntent(
            ShulkerBundlingAction.ENDER_CHEST_EXTRACT,
            intent.hostSlot(),
            intent.containerId(),
            intent.dragId()
        ), cursorStack, dragSession, false);
    }

    private static void handleEnderChestExtract(
        ServerPlayer player,
        ShulkerBundlingIntent intent,
        ItemStack cursorStack,
        DragSession dragSession
    ) {
        handleEnderChestExtract(player, intent, cursorStack, dragSession, true);
    }

    private static void handleEnderChestExtract(
        ServerPlayer player,
        ShulkerBundlingIntent intent,
        ItemStack cursorStack,
        DragSession dragSession,
        boolean requireActionConfig
    ) {
        if (!supportsEnderChestBundling()) {
            return;
        }
        if (requireActionConfig && !ForgeQuickShulkerConfig.view().enderChestBundlingExtract()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }
        ItemStack targetStack = ForgeHostSlotResolver.resolve(player, intent.hostSlot()).copy();
        if (!targetStack.isEmpty()) {
            LOGGER.debug("Rejected Forge ender chest extract because target slot was not empty: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack carried = resolvedCarried(player, cursorStack, dragSession);
        if (!isSingleEnderChest(carried)) {
            LOGGER.debug("Rejected Forge ender chest extract because carried stack is not a single ender chest: carried={}", describeStack(carried));
            return;
        }

        boolean targetIsShulkerContentSlot = isShulkerMenuContainerSlot(player, intent.hostSlot());
        ShulkerBundlingResult<List<ItemStack>, ItemStack> result =
            EnderChestBundlingRules.extractLastStackFromPlayerEnderChest(
                ENDER_CHEST_ACCESS.readPlayerEnderChestContents(player),
                ENDER_CHEST_ADAPTER,
                targetIsShulkerContentSlot
            );
        if (!result.changed() || result.updatedContainerStack().isEmpty() || result.extractedStack().isEmpty()) {
            LOGGER.debug("Rejected Forge ender chest extract after service validation: failure={}, detail={}, targetIsShulkerContentSlot={}", result.failure(), result.detail(), targetIsShulkerContentSlot);
            return;
        }
        ItemStack extractedStack = result.extractedStack().orElseThrow().copy();
        if (!ForgeHostSlotResolver.canSafelyReplace(player, intent.hostSlot(), extractedStack)) {
            LOGGER.debug("Rejected Forge ender chest extract because target slot is unsafe for writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!ForgeHostSlotResolver.resolve(player, intent.hostSlot()).isEmpty()) {
            LOGGER.debug("Rejected Forge ender chest extract because target slot changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!carriedStillMatches(player, carried, dragSession)) {
            LOGGER.debug("Rejected Forge ender chest extract because carried stack changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        ContentWriteResult writeResult = ENDER_CHEST_ACCESS.writePlayerEnderChestContents(
            player,
            result.updatedContainerStack().orElseThrow()
        );
        if (!writeResult.applied()) {
            LOGGER.debug("Rejected Forge ender chest extract because ender chest writeback failed: detail={}", writeResult.detail());
            return;
        }

        ForgeHostSlotResolver.set(player, intent.hostSlot(), extractedStack);
        syncPlayerInventory(player);
        syncCreativeCursor(player, carried);
        finishCreativeServerCarriedAfterSync(player, intent, "ender_chest_extract");
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
        return resolvedCarried(player, cursorStack, null);
    }

    private static ItemStack resolvedCarried(ServerPlayer player, ItemStack cursorStack, DragSession dragSession) {
        if (player.getAbilities().instabuild) {
            if (dragSession != null && dragSession.hasCreativeCursor()) {
                return dragSession.creativeCursor();
            }
            return cursorStack == null ? ItemStack.EMPTY : cursorStack.copy();
        }
        return player.containerMenu.getCarried().copy();
    }

    private static void writeCarried(ServerPlayer player, ItemStack stack, DragSession dragSession) {
        ItemStack copy = stack.copy();
        if (player.getAbilities().instabuild) {
            if (dragSession != null) {
                dragSession.setCreativeCursor(copy);
            }
            // Creative mode keeps the authoritative carried in the QuickShulker drag
            // session and pushes the updated stack to the client via syncCreativeCursor.
            // Persisting it onto the server menu leaks the stack across menu
            // close/reopen and reintroduces the creative shulker-box dupe
            // (stage 3.5 intended this; stage 8.1 completes the removal).
            return;
        }
        player.containerMenu.setCarried(copy);
    }

    private static void syncCreativeCursor(ServerPlayer player, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            ForgeQuickShulkerNetwork.sendCreativeCursorSync(player, stack.copy());
        }
    }

    private static boolean matchesCurrentContainer(ServerPlayer player, ShulkerBundlingIntent intent) {
        AbstractContainerMenu menu = player.containerMenu;
        return menu != null && intent.containerId() == menu.containerId;
    }

    private static boolean isDragSessionIntent(ShulkerBundlingIntent intent) {
        return intent.dragId() != 0L
            && (intent.action() == ShulkerBundlingAction.PICKUP_INSERT
                || intent.action() == ShulkerBundlingAction.MOUSE_DRAG_PICKUP_INSERT
                || intent.action() == ShulkerBundlingAction.EXTRACT
                || intent.action() == ShulkerBundlingAction.MOUSE_DRAG_EXTRACT
                || intent.action() == ShulkerBundlingAction.ENDER_CHEST_PICKUP_INSERT
                || intent.action() == ShulkerBundlingAction.ENDER_CHEST_EXTRACT
                || intent.action() == ShulkerBundlingAction.MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT
                || intent.action() == ShulkerBundlingAction.MOUSE_DRAG_ENDER_CHEST_EXTRACT);
    }

    private static DragSession resolveDragSession(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        boolean enderChestDrag = isEnderChestDragAction(intent.action());
        long now = currentGameTime(player);
        DragSession session = DRAG_SESSIONS.get(player.getUUID());
        if (session == null || !session.matches(intent.containerId(), intent.dragId())) {
            ItemStack initialCarried = player.getAbilities().instabuild
                ? (cursorStack == null ? ItemStack.EMPTY : cursorStack.copy())
                : player.containerMenu.getCarried().copy();
            if (!isValidDragCarried(initialCarried, enderChestDrag)) {
                LOGGER.debug("Rejected Forge mouse drag session due to invalid carried stack: action={}, hostSlot={}",
                    intent.action(), intent.hostSlot());
                return null;
            }
            if (intent.action() == ShulkerBundlingAction.MOUSE_DRAG_PICKUP_INSERT
                || intent.action() == ShulkerBundlingAction.MOUSE_DRAG_EXTRACT
                || intent.action() == ShulkerBundlingAction.MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT
                || intent.action() == ShulkerBundlingAction.MOUSE_DRAG_ENDER_CHEST_EXTRACT) {
                LOGGER.debug("Rejected Forge mouse drag continuation without active session: action={}, hostSlot={}",
                    intent.action(), intent.hostSlot());
                return null;
            }
            session = new DragSession(intent.containerId(), intent.dragId(), now);
            DRAG_SESSIONS.put(player.getUUID(), session);
            return session;
        }
        ItemStack currentCarried = player.getAbilities().instabuild
            ? (session.hasCreativeCursor() ? session.creativeCursor() : (cursorStack == null ? ItemStack.EMPTY : cursorStack.copy()))
            : player.containerMenu.getCarried().copy();
        if (!isValidDragCarried(currentCarried, enderChestDrag)) {
            LOGGER.debug("Rejected Forge mouse drag session due to invalid server carried stack: action={}, hostSlot={}",
                intent.action(), intent.hostSlot());
            return null;
        }
        session.refresh(now);
        return session;
    }

    private static boolean isValidDragCarried(ItemStack carried, boolean enderChestDrag) {
        return enderChestDrag ? isSingleEnderChest(carried) : isSingleShulkerBox(carried);
    }

    private static boolean isEnderChestDragAction(ShulkerBundlingAction action) {
        return action == ShulkerBundlingAction.ENDER_CHEST_PICKUP_INSERT
            || action == ShulkerBundlingAction.ENDER_CHEST_EXTRACT
            || action == ShulkerBundlingAction.MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT
            || action == ShulkerBundlingAction.MOUSE_DRAG_ENDER_CHEST_EXTRACT;
    }

    private static void pruneDragSessions(ServerPlayer player) {
        DragSession session = DRAG_SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        long now = currentGameTime(player);
        if (menu == null
            || session.containerId != menu.containerId
            || now - session.lastSeenGameTime > DRAG_SESSION_TIMEOUT_TICKS) {
            DRAG_SESSIONS.remove(player.getUUID());
        }
    }

    private static long currentGameTime(ServerPlayer player) {
        return player.serverLevel().getGameTime();
    }

    private static boolean carriedStillMatches(ServerPlayer player, ItemStack expected, DragSession dragSession) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        return ItemStack.matches(expected, player.containerMenu.getCarried());
    }

    private static DragSession clearMatchingDragSession(ServerPlayer player, ShulkerBundlingIntent intent) {
        DragSession session = DRAG_SESSIONS.get(player.getUUID());
        if (session != null && session.matches(intent.containerId(), intent.dragId())) {
            DRAG_SESSIONS.remove(player.getUUID());
            return session;
        }
        return null;
    }

    private static boolean isConservedPickupInsert(
        int targetBefore,
        int shulkerBefore,
        ItemStack updatedTarget,
        ItemStack updatedCarried
    ) {
        int moved = targetBefore - updatedTarget.getCount();
        return moved >= 0 && countShulkerItems(updatedCarried) - shulkerBefore == moved;
    }

    private static boolean isConservedExtract(int shulkerBefore, ItemStack updatedCarried, ItemStack extractedStack) {
        int moved = extractedStack.getCount();
        return moved >= 0 && shulkerBefore - countShulkerItems(updatedCarried) == moved;
    }

    private static int countShulkerItems(ItemStack shulkerStack) {
        int total = 0;
        for (ItemStack stack : HELPER.readItemStacksCopy(shulkerStack)) {
            total += stack.getCount();
        }
        return total;
    }

    private static boolean isSingleShulkerBox(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() == 1 && isShulkerBox(stack);
    }

    private static boolean isSingleEnderChest(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() == 1 && stack.is(Items.ENDER_CHEST);
    }

    private static boolean isEnderChest(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.ENDER_CHEST);
    }

    private static boolean canInsertIntoEnderChest(ItemStack stack) {
        return !stack.isEmpty() && !isEnderChest(stack);
    }

    private static boolean isEnderChestBundlingAction(ShulkerBundlingAction action) {
        return action == ShulkerBundlingAction.ENDER_CHEST_INSERT
            || action == ShulkerBundlingAction.ENDER_CHEST_PICKUP_INSERT
            || action == ShulkerBundlingAction.ENDER_CHEST_EXTRACT;
    }

    private static boolean isShulkerMenuContainerSlot(ServerPlayer player, HostSlotRef slotRef) {
        return slotRef.scope() == HostStorageScope.PLAYER_CONTAINER_MENU
            && player.containerMenu instanceof ShulkerBoxMenu;
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

    private static void finishCreativeServerCarriedAfterSync(ServerPlayer player, ShulkerBundlingIntent intent, String phase) {
        if (!player.getAbilities().instabuild) {
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "Forge creative bundling {} kept server carried after sync until drag end: action={}, hostSlot={}, menuClass={}, menuCarried={}",
                phase,
                intent.action(),
                intent.hostSlot(),
                player.containerMenu.getClass().getName(),
                "<client-synced>"
            );
        }
    }

    private static void logCreativeCarriedPreserved(ServerPlayer player, ShulkerBundlingIntent intent, String phase) {
        if (!player.getAbilities().instabuild) {
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "Forge creative bundling {} preserved carried after session end: action={}, hostSlot={}, menuClass={}, menuCarried={}",
                phase,
                intent == null ? ShulkerBundlingAction.UNKNOWN : intent.action(),
                intent == null ? null : intent.hostSlot(),
                player.containerMenu.getClass().getName(),
                "<client-synced>"
            );
        }
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        return stack.getItem().toString() + " x" + stack.getCount();
    }

    private static final class DragSession {
        private final int containerId;
        private final long dragId;
        private final Set<ProcessedDragSlot> processedSlots = new HashSet<>();
        private long lastSeenGameTime;
        private boolean serverCarried;

        private DragSession(int containerId, long dragId, long lastSeenGameTime) {
            this.containerId = containerId;
            this.dragId = dragId;
            this.lastSeenGameTime = lastSeenGameTime;
        }

        private boolean matches(int containerId, long dragId) {
            return this.containerId == containerId && this.dragId == dragId;
        }

        private void refresh(long now) {
            this.lastSeenGameTime = now;
        }

        private boolean markProcessed(ShulkerBundlingAction action, HostSlotRef hostSlot) {
            return processedSlots.add(new ProcessedDragSlot(action, hostSlot));
        }

        private boolean hasCreativeCursor() {
            return serverCarried;
        }

        private ItemStack creativeCursor() {
            return creativeCursor.copy();
        }

        private void setCreativeCursor(ItemStack stack) {
            this.creativeCursor = stack.copy();
            serverCarried = true;
        }

        private ItemStack creativeCursor = ItemStack.EMPTY;
    }

    private record ProcessedDragSlot(ShulkerBundlingAction action, HostSlotRef hostSlot) {
    }

    private static final class EnderChestItemStackAdapter implements EnderChestBundlingStackAdapter<ItemStack> {
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
            return ForgeShulkerBundlingHandler.isShulkerBox(stack);
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

        @Override
        public boolean isEnderChest(ItemStack stack) {
            return ForgeShulkerBundlingHandler.isEnderChest(stack);
        }

        @Override
        public boolean canInsertIntoEnderChest(ItemStack stack) {
            return ForgeShulkerBundlingHandler.canInsertIntoEnderChest(stack);
        }
    }
}
