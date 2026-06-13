package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.bundling.ShulkerBundlingResult;
import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingAction;
import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingIntent;
import com.ice2974.quickshulkerneoforged.common.open.HostIdentity;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.neoforge.network.NeoForgeQuickShulkerNetwork;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NeoForgeShulkerBundlingHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoForgeShulkerBundlingHandler.class);
    private static final NeoForgeShulkerBundlingHelper HELPER = new NeoForgeShulkerBundlingHelper();
    private static final int DRAG_SESSION_TIMEOUT_TICKS = 200;
    private static final Map<UUID, DragSession> DRAG_SESSIONS = new HashMap<>();

    private NeoForgeShulkerBundlingHandler() {
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
        if (!matchesCurrentContainer(player, intent)) {
            LOGGER.debug(
                "Rejected NeoForge bundling intent for stale container: action={}, packetContainerId={}, currentContainerId={}, hostSlot={}",
                intent.action(),
                intent.containerId(),
                player.containerMenu.containerId,
                intent.hostSlot()
            );
            return;
        }

        DragSession dragSession = null;
        if (isDragSessionIntent(intent)) {
            dragSession = resolveDragSession(player, intent, cursorStack);
            if (dragSession == null || !dragSession.markProcessed(intent.action(), intent.hostSlot())) {
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
            case UNKNOWN -> LOGGER.debug("Rejected NeoForge bundling intent with unknown action: hostSlot={}", intent.hostSlot());
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
        if (!NeoForgeQuickShulkerConfig.view().supportsBundlingInsert()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }
        if (!NeoForgeHostSlotResolver.canSafelyReadAndShrink(player, intent.hostSlot())) {
            LOGGER.debug("Rejected NeoForge bundling insert due to unsafe host slot: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack carried = resolvedCarried(player, cursorStack);
        logCreativeResolvedCarried(player, intent, cursorStack, carried, "insert");
        if (carried.isEmpty() || isShulkerBox(carried)) {
            LOGGER.debug("Rejected NeoForge creative/player bundling insert due to invalid carried stack: creative={}, empty={}, shulker={}",
                player.getAbilities().instabuild, carried.isEmpty(), isShulkerBox(carried));
            return;
        }

        ItemStack hostStack = NeoForgeHostSlotResolver.resolve(player, intent.hostSlot());
        ShulkerBundlingResult<ItemStack, ItemStack> result = HELPER.insertIntoShulker(hostStack, carried);
        if (!result.changed()) {
            LOGGER.debug("Rejected NeoForge bundling insert after helper validation: failure={}, detail={}", result.failure(), result.detail());
            return;
        }
        ItemStack updatedHostStack = result.updatedContainerStack().orElseThrow().copy();
        if (!NeoForgeHostSlotResolver.canSafelyReplace(player, intent.hostSlot(), updatedHostStack)) {
            LOGGER.debug("Rejected NeoForge bundling insert due to unsafe host slot writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack updatedCarried = result.updatedInputStack().orElseThrow().copy();
        NeoForgeHostSlotResolver.set(player, intent.hostSlot(), updatedHostStack);
        writeCarried(player, updatedCarried, null);
        logCreativeSetCarried(player, intent, "insert");
        syncPlayerInventory(player);
        syncCreativeCursor(player, updatedCarried);
        finishCreativeServerCarriedAfterSync(player, intent, "insert");
    }

    private static void handlePickupInsert(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack, DragSession dragSession) {
        if (!NeoForgeQuickShulkerConfig.view().supportsBundlingPickup()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }
        if (!NeoForgeHostSlotResolver.canSafelyReadAndShrink(player, intent.hostSlot())) {
            LOGGER.debug("Rejected NeoForge pickup insert due to unsafe host slot: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack carried = resolvedCarried(player, cursorStack, dragSession);
        logCreativeResolvedCarried(player, intent, cursorStack, carried, "pickup_insert");
        if (!isSingleShulkerBox(carried)) {
            LOGGER.debug("Rejected NeoForge pickup insert due to non-single-shulker carried stack: creative={}, count={}, empty={}",
                player.getAbilities().instabuild, carried.getCount(), carried.isEmpty());
            return;
        }

        ItemStack hostStack = NeoForgeHostSlotResolver.resolve(player, intent.hostSlot()).copy();
        if (hostStack.isEmpty() || isShulkerBox(hostStack) || !hostStack.getItem().canFitInsideContainerItems()) {
            return;
        }
        int targetBefore = hostStack.getCount();
        int shulkerBefore = countShulkerItems(carried);

        ShulkerBundlingResult<ItemStack, ItemStack> result = HELPER.insertIntoShulker(carried, hostStack);
        if (!result.changed()) {
            LOGGER.debug("Rejected NeoForge pickup insert after helper validation: failure={}, detail={}", result.failure(), result.detail());
            return;
        }
        ItemStack updatedHostStack = result.updatedInputStack().orElseThrow().copy();
        ItemStack updatedCarried = result.updatedContainerStack().orElseThrow().copy();
        if (!isConservedPickupInsert(targetBefore, shulkerBefore, updatedHostStack, updatedCarried)) {
            LOGGER.warn("Rejected NeoForge pickup insert due to conservation check failure: action={}, hostSlot={}", intent.action(), intent.hostSlot());
            return;
        }
        if (!ItemStack.matches(hostStack, NeoForgeHostSlotResolver.resolve(player, intent.hostSlot()))) {
            LOGGER.debug("Rejected NeoForge pickup insert because target slot changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!carriedStillMatches(player, carried, dragSession)) {
            LOGGER.debug("Rejected NeoForge pickup insert because carried stack changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!NeoForgeHostSlotResolver.canSafelyReplace(player, intent.hostSlot(), updatedHostStack)) {
            LOGGER.debug("Rejected NeoForge pickup insert due to unsafe host slot writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        writeCarried(player, updatedCarried, dragSession);
        logCreativeSetCarried(player, intent, "pickup_insert");
        NeoForgeHostSlotResolver.set(player, intent.hostSlot(), updatedHostStack);
        syncPlayerInventory(player);
        syncCreativeCursor(player, updatedCarried);
        finishCreativeServerCarriedAfterSync(player, intent, "pickup_insert");
    }

    private static void handleMouseDragInsert(ServerPlayer player, ShulkerBundlingIntent intent) {
        if (!NeoForgeQuickShulkerConfig.view().supportsMouseDragged()
            || !NeoForgeQuickShulkerConfig.view().supportsBundlingInsert()) {
            return;
        }
        LOGGER.debug("Rejected NeoForge mouse dragged insert because ordinary-item drag insertion is disabled: hostSlot={}", intent.hostSlot());
    }

    private static void handleMouseDragPickupInsert(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack, DragSession dragSession) {
        if (!NeoForgeQuickShulkerConfig.view().supportsMouseDragged()
            || !NeoForgeQuickShulkerConfig.view().supportsBundlingPickup()) {
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
        if (!NeoForgeQuickShulkerConfig.view().supportsMouseDragged()) {
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
        if (!NeoForgeQuickShulkerConfig.view().supportsBundlingExtract()) {
            return;
        }
        if (mouseDragged && !NeoForgeQuickShulkerConfig.view().supportsMouseDragged()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }
        ItemStack targetStack = NeoForgeHostSlotResolver.resolve(player, intent.hostSlot()).copy();
        if (!targetStack.isEmpty()) {
            LOGGER.debug("Rejected NeoForge extract because target slot was not empty: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack carried = resolvedCarried(player, cursorStack, dragSession);
        logCreativeResolvedCarried(player, intent, cursorStack, carried, "extract");
        if (!isSingleShulkerBox(carried)) {
            LOGGER.debug("Rejected NeoForge extract due to non-single-shulker carried stack: creative={}, count={}, empty={}",
                player.getAbilities().instabuild, carried.getCount(), carried.isEmpty());
            return;
        }
        int shulkerBefore = countShulkerItems(carried);

        ShulkerBundlingResult<ItemStack, ItemStack> result = HELPER.extractFirstStack(carried);
        if (!result.changed() || result.updatedContainerStack().isEmpty() || result.extractedStack().isEmpty()) {
            LOGGER.debug("Rejected NeoForge extract after helper validation: failure={}, detail={}", result.failure(), result.detail());
            return;
        }

        ItemStack extractedStack = result.extractedStack().orElseThrow().copy();
        ItemStack updatedCarried = result.updatedContainerStack().orElseThrow().copy();
        if (!isConservedExtract(shulkerBefore, updatedCarried, extractedStack)) {
            LOGGER.warn("Rejected NeoForge extract due to conservation check failure: action={}, hostSlot={}", intent.action(), intent.hostSlot());
            return;
        }
        if (!NeoForgeHostSlotResolver.canSafelyReplace(player, intent.hostSlot(), extractedStack)) {
            LOGGER.debug("Rejected NeoForge extract because target slot is unsafe for extracted stack writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!NeoForgeHostSlotResolver.resolve(player, intent.hostSlot()).isEmpty()) {
            LOGGER.debug("Rejected NeoForge extract because target slot changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }
        if (!carriedStillMatches(player, carried, dragSession)) {
            LOGGER.debug("Rejected NeoForge extract because carried stack changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        writeCarried(player, updatedCarried, dragSession);
        logCreativeSetCarried(player, intent, "extract");
        NeoForgeHostSlotResolver.set(player, intent.hostSlot(), extractedStack);
        syncPlayerInventory(player);
        syncCreativeCursor(player, updatedCarried);
        finishCreativeServerCarriedAfterSync(player, intent, "extract");
    }

    private static void handleTransfer(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        if (!NeoForgeQuickShulkerConfig.view().supportsBundlingTransfer()) {
            return;
        }
        if (isCurrentQuickOpenHost(player, intent)) {
            return;
        }
        if (!NeoForgeHostSlotResolver.canSafelyReadAndShrink(player, intent.hostSlot())) {
            LOGGER.debug("Rejected NeoForge transfer due to unsafe host slot: hostSlot={}", intent.hostSlot());
            return;
        }
        ItemStack targetShulker = NeoForgeHostSlotResolver.resolve(player, intent.hostSlot());
        if (!isSingleShulkerBox(targetShulker)) {
            LOGGER.debug("Rejected NeoForge transfer due to non-single-shulker target stack: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack sourceShulker = resolvedCarried(player, cursorStack);
        logCreativeResolvedCarried(player, intent, cursorStack, sourceShulker, "transfer");
        if (!isSingleShulkerBox(sourceShulker)) {
            LOGGER.debug("Rejected NeoForge transfer due to non-single-shulker carried stack: creative={}, count={}, empty={}",
                player.getAbilities().instabuild, sourceShulker.getCount(), sourceShulker.isEmpty());
            return;
        }

        ShulkerBundlingResult<ItemStack, ItemStack> result = HELPER.transferBetweenShulkers(sourceShulker, targetShulker);
        if (!result.changed()
            || result.updatedSourceContainerStack().isEmpty()
            || result.updatedTargetContainerStack().isEmpty()) {
            LOGGER.debug("Rejected NeoForge transfer after helper validation: failure={}, detail={}", result.failure(), result.detail());
            return;
        }
        ItemStack updatedTargetStack = result.updatedTargetContainerStack().orElseThrow().copy();
        if (!NeoForgeHostSlotResolver.canSafelyReplace(player, intent.hostSlot(), updatedTargetStack)) {
            LOGGER.debug("Rejected NeoForge transfer due to unsafe host slot writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        if (!isSingleShulkerBox(NeoForgeHostSlotResolver.resolve(player, intent.hostSlot()))) {
            LOGGER.debug("Rejected NeoForge transfer because target slot changed before writeback: hostSlot={}", intent.hostSlot());
            return;
        }

        ItemStack updatedCarried = result.updatedSourceContainerStack().orElseThrow().copy();
        NeoForgeHostSlotResolver.set(player, intent.hostSlot(), updatedTargetStack);
        writeCarried(player, updatedCarried, null);
        logCreativeSetCarried(player, intent, "transfer");
        syncPlayerInventory(player);
        syncCreativeCursor(player, updatedCarried);
        finishCreativeServerCarriedAfterSync(player, intent, "transfer");
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
            return;
        }
        player.containerMenu.setCarried(copy);
    }

    private static void syncCreativeCursor(ServerPlayer player, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            NeoForgeQuickShulkerNetwork.sendCreativeCursorSync(player, stack.copy());
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
                || intent.action() == ShulkerBundlingAction.MOUSE_DRAG_EXTRACT);
    }

    private static DragSession resolveDragSession(ServerPlayer player, ShulkerBundlingIntent intent, ItemStack cursorStack) {
        ItemStack currentCarried = player.getAbilities().instabuild
            ? cursorStack.copy()
            : player.containerMenu.getCarried().copy();
        if (!isSingleShulkerBox(currentCarried)) {
            LOGGER.debug("Rejected NeoForge mouse drag session due to non-single-shulker carried stack: action={}, hostSlot={}",
                intent.action(), intent.hostSlot());
            return null;
        }

        long now = currentGameTime(player);
        DragSession session = DRAG_SESSIONS.get(player.getUUID());
        if (session == null || !session.matches(intent.containerId(), intent.dragId())) {
            if (intent.action() == ShulkerBundlingAction.MOUSE_DRAG_PICKUP_INSERT
                || intent.action() == ShulkerBundlingAction.MOUSE_DRAG_EXTRACT) {
                LOGGER.debug("Rejected NeoForge mouse drag continuation without active session: action={}, hostSlot={}",
                    intent.action(), intent.hostSlot());
                return null;
            }
            session = new DragSession(intent.containerId(), intent.dragId(), now);
            DRAG_SESSIONS.put(player.getUUID(), session);
            return session;
        }
        session.refresh(now);
        return session;
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

    private static boolean isShulkerBox(ItemStack stack) {
        return !stack.isEmpty() && Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock;
    }

    private static boolean isCurrentQuickOpenHost(ServerPlayer player, ShulkerBundlingIntent intent) {
        return player.containerMenu instanceof NeoForgeQuickOpenMenu quickOpenMenu
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
            "NeoForge creative bundling {} before apply: action={}, hostSlot={}, payloadCursor={}, resolvedCarried={}, menuCarried={}",
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
            "NeoForge creative bundling {} after setCarried: action={}, hostSlot={}, menuCarried={}",
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
                "NeoForge creative bundling {} kept server carried after sync until drag end: action={}, hostSlot={}, menuClass={}, menuCarried={}",
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
                "NeoForge creative bundling {} preserved carried after session end: action={}, hostSlot={}, menuClass={}, menuCarried={}",
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
            this.serverCarried = true;
        }

        private ItemStack creativeCursor = ItemStack.EMPTY;
    }

    private record ProcessedDragSlot(ShulkerBundlingAction action, HostSlotRef hostSlot) {
    }
}
