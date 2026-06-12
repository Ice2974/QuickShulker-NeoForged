package com.ice2974.quickshulkerneoforged.forge.client;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.common.network.OpenHostItemIntent;
import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingAction;
import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingIntent;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.HostStorageScope;
import com.ice2974.quickshulkerneoforged.common.open.HostIdentity;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenTrigger;
import com.ice2974.quickshulkerneoforged.forge.ForgeHostSlotResolver;
import com.ice2974.quickshulkerneoforged.forge.ForgeItemSnapshots;
import com.ice2974.quickshulkerneoforged.forge.ForgeQuickOpenMenu;
import com.ice2974.quickshulkerneoforged.forge.ForgeQuickOpenRegistry;
import com.ice2974.quickshulkerneoforged.forge.ForgeQuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.forge.network.ForgeOpenHostItemPacket;
import com.ice2974.quickshulkerneoforged.forge.network.ForgeQuickShulkerNetwork;
import com.ice2974.quickshulkerneoforged.forge.network.ForgeShulkerBundlingPacket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid = QuickShulkerConstants.MOD_ID, value = Dist.CLIENT)
public final class ForgeQuickShulkerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeQuickShulkerClient.class);
    private static boolean suppressNextInventoryRightRelease;
    private static DragMode dragMode = DragMode.NONE;
    private static final Set<HostSlotRef> DRAGGED_HOST_SLOTS = new HashSet<>();
    private static long currentDragId;
    private static int dragContainerId = -1;

    private ForgeQuickShulkerClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ForgeQuickOpenMouseRestore.onClientTick();

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            clearMouseDrag();
            return;
        }
        if (dragMode != DragMode.NONE
            && (!(minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)
            || containerScreen.getMenu().containerId != dragContainerId)) {
            clearMouseDrag();
        }

        if (minecraft.screen == null
            && ForgeQuickShulkerConfig.view().openSettingsKeyEnabled()) {
            while (ForgeKeyMappings.OPEN_SETTINGS_SCREEN.consumeClick()) {
                minecraft.setScreen(new ForgeQuickShulkerConfigScreen(null));
                return;
            }
        }

        if (!hasAnyEnabledQuickOpenable()
            || !ForgeQuickShulkerConfig.view().keybindInHand()) {
            return;
        }

        if (minecraft.screen != null) {
            return;
        }

        while (ForgeKeyMappings.OPEN_HELD_SHULKER.consumeClick()) {
            if (trySendHeld(player, InteractionHand.MAIN_HAND, QuickOpenTrigger.HAND_KEYBIND)
                || trySendHeld(player, InteractionHand.OFF_HAND, QuickOpenTrigger.HAND_KEYBIND)) {
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!hasAnyEnabledQuickOpenable()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        if (shouldBlockOffhandSwap(event.getScreen(), minecraft, event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true);
            return;
        }

        if (!ForgeQuickShulkerConfig.view().keybindInInventory()) {
            return;
        }

        if (ForgeKeyMappings.OPEN_HELD_SHULKER.matches(event.getKeyCode(), event.getScanCode())) {
            if (trySendHovered(minecraft.player, event.getScreen(), QuickOpenTrigger.INVENTORY_KEYBIND)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || event.getButton() != 1) {
            return;
        }

        Optional<ShulkerBundlingIntent> bundlingIntent = determineBundlingIntent(player, event.getScreen());
        if (bundlingIntent.isPresent()) {
            AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) event.getScreen();
            ShulkerBundlingIntent preparedIntent = prepareBundlingIntent(containerScreen, bundlingIntent.get());
            sendBundlingIntent(containerScreen, preparedIntent);
            beginMouseDrag(player, preparedIntent);
            suppressNextInventoryRightRelease = true;
            event.setCanceled(true);
            return;
        }

        if (!hasAnyEnabledQuickOpenable()
            || !ForgeQuickShulkerConfig.view().rightClickInInventory()
            || !ForgeQuickShulkerConfig.view().rightClickToOpen()) {
            return;
        }

        if (trySendHovered(player, event.getScreen(), QuickOpenTrigger.INVENTORY_RIGHT_CLICK)) {
            suppressNextInventoryRightRelease = true;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || event.getMouseButton() != 1) {
            clearMouseDrag();
            return;
        }

        if (trySendMouseDraggedBundlingIntent(player, event.getScreen())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() == 1) {
            clearMouseDrag();
        }
        if (event.getButton() != 1 || !suppressNextInventoryRightRelease) {
            return;
        }

        suppressNextInventoryRightRelease = false;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        suppressNextInventoryRightRelease = false;
        clearMouseDrag();
        ForgeQuickOpenMouseRestore.onScreenInit(event.getScreen());
    }

    @SubscribeEvent
    public static void onHandRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!hasAnyEnabledQuickOpenable()
            || !ForgeQuickShulkerConfig.view().rightClickToOpen()) {
            return;
        }

        Player player = event.getEntity();
        if (player == null || !player.level().isClientSide()) {
            return;
        }

        if (trySendHeld(player, event.getHand(), QuickOpenTrigger.HAND_RIGHT_CLICK)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    private static boolean trySendHeld(Player player, InteractionHand hand, QuickOpenTrigger trigger) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return false;
        }

        return resolveTypeId(stack)
            .map(type -> {
                if (type.requiresSingleHostStack() && stack.getCount() != 1) {
                    return false;
                }
                sendIntent(new OpenHostItemIntent(
                    type.id(),
                    ForgeHostSlotResolver.forHand(player, hand),
                    trigger
                ));
                return true;
            })
            .orElse(false);
    }

    private static boolean trySendHovered(Player player, Screen screen, QuickOpenTrigger trigger) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        if (!containerScreen.getMenu().getCarried().isEmpty()) {
            return false;
        }

        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return false;
        }

        ItemStack stack = hoveredSlot.getItem();
        Optional<HostSlotRef> hostSlot = ForgeHostSlotResolver.forPlayerInventorySlot(player, hoveredSlot, hoveredSlot.index);
        if (hostSlot.isEmpty()) {
            return false;
        }

        HostSlotRef requestedHostSlot = hostSlot.get();
        return resolveTypeId(stack)
            .map(type -> {
                if (type.requiresSingleHostStack() && stack.getCount() != 1) {
                    return false;
                }
                if (containerScreen.getMenu() instanceof ForgeQuickOpenMenu quickOpenMenu
                    && quickOpenMenu.isSameHost(type.id(), requestedHostSlot)) {
                    return false;
                }
                sendIntent(new OpenHostItemIntent(type.id(), requestedHostSlot, trigger));
                return true;
            })
            .orElse(false);
    }

    private static Optional<com.ice2974.quickshulkerneoforged.common.open.QuickOpenableType> resolveTypeId(ItemStack stack) {
        return ForgeQuickOpenRegistry.registry()
            .findTypeForItem(ForgeItemSnapshots.snapshot(stack).itemKey())
            .filter(type -> ForgeQuickShulkerConfig.view().isEnabled(type));
    }

    private static void sendIntent(OpenHostItemIntent intent) {
        ForgeQuickOpenMouseRestore.capture(Minecraft.getInstance().screen, intent.requestedTypeId());
        ForgeQuickShulkerNetwork.sendOpenHostItem(new ForgeOpenHostItemPacket(intent));
    }

    private static Optional<ShulkerBundlingIntent> determineBundlingIntent(Player player, Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return Optional.empty();
        }

        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot == null) {
            return Optional.empty();
        }

        Optional<HostSlotRef> hostSlot = ForgeHostSlotResolver.forBundlingSlot(player, hoveredSlot, hoveredSlot.index);
        if (hostSlot.isEmpty()) {
            return Optional.empty();
        }
        if (containerScreen.getMenu() instanceof ForgeQuickOpenMenu quickOpenMenu
            && HostIdentity.sameSlot(quickOpenMenu.hostSlotRef(), hostSlot.get())) {
            return Optional.empty();
        }

        ItemStack carried = containerScreen.getMenu().getCarried();
        ItemStack hoveredStack = hoveredSlot.getItem();
        if (ForgeQuickShulkerConfig.view().supportsBundlingExtract()
            && hoveredStack.isEmpty()
            && isSingleShulkerBox(carried)) {
            return Optional.of(new ShulkerBundlingIntent(ShulkerBundlingAction.EXTRACT, hostSlot.get()));
        }

        if (ForgeQuickShulkerConfig.view().supportsBundlingTransfer()
            && isSingleShulkerBox(carried)
            && isSingleShulkerBox(hoveredStack)) {
            return Optional.of(new ShulkerBundlingIntent(ShulkerBundlingAction.TRANSFER, hostSlot.get()));
        }

        if (ForgeQuickShulkerConfig.view().supportsBundlingInsert()
            && !carried.isEmpty()
            && !isShulkerBox(carried)
            && isSingleShulkerBox(hoveredStack)) {
            return Optional.of(new ShulkerBundlingIntent(ShulkerBundlingAction.INSERT, hostSlot.get()));
        }

        if (ForgeQuickShulkerConfig.view().supportsBundlingPickup()
            && isSingleShulkerBox(carried)
            && !hoveredStack.isEmpty()
            && !isShulkerBox(hoveredStack)
            && hoveredStack.getItem().canFitInsideContainerItems()) {
            return Optional.of(new ShulkerBundlingIntent(ShulkerBundlingAction.PICKUP_INSERT, hostSlot.get()));
        }

        return Optional.empty();
    }

    private static void beginMouseDrag(Player player, ShulkerBundlingIntent intent) {
        dragMode = DragMode.NONE;
        DRAGGED_HOST_SLOTS.clear();
        if (!ForgeQuickShulkerConfig.view().supportsMouseDragged()) {
            currentDragId = 0L;
            dragContainerId = -1;
            return;
        }
        if (intent.action() == ShulkerBundlingAction.PICKUP_INSERT) {
            dragMode = DragMode.PICKUP_INTO_CARRIED_SHULKER;
        } else if (intent.action() == ShulkerBundlingAction.EXTRACT
            && ForgeQuickShulkerConfig.view().supportsBundlingExtract()) {
            dragMode = DragMode.EXTRACT_FROM_CARRIED_SHULKER;
        } else {
            currentDragId = 0L;
            dragContainerId = -1;
            return;
        }
        DRAGGED_HOST_SLOTS.add(intent.hostSlot());
    }

    private static boolean trySendMouseDraggedBundlingIntent(Player player, Screen screen) {
        if (dragMode == DragMode.NONE) {
            return false;
        }
        if (!ForgeQuickShulkerConfig.view().supportsMouseDragged()) {
            clearMouseDrag();
            return false;
        }
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            clearMouseDrag();
            return false;
        }

        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot == null) {
            return true;
        }

        Optional<HostSlotRef> hostSlot = ForgeHostSlotResolver.forBundlingSlot(player, hoveredSlot, hoveredSlot.index);
        if (hostSlot.isEmpty()) {
            return true;
        }
        if (DRAGGED_HOST_SLOTS.contains(hostSlot.get())) {
            return true;
        }
        if (containerScreen.getMenu() instanceof ForgeQuickOpenMenu quickOpenMenu
            && HostIdentity.sameSlot(quickOpenMenu.hostSlotRef(), hostSlot.get())) {
            return true;
        }

        ItemStack carried = containerScreen.getMenu().getCarried();
        ItemStack hoveredStack = hoveredSlot.getItem();
        ShulkerBundlingAction action;
        if (dragMode == DragMode.PICKUP_INTO_CARRIED_SHULKER
            && ForgeQuickShulkerConfig.view().supportsBundlingPickup()
            && isSingleShulkerBox(carried)
            && !hoveredStack.isEmpty()
            && !isShulkerBox(hoveredStack)
            && hoveredStack.getItem().canFitInsideContainerItems()) {
            action = ShulkerBundlingAction.MOUSE_DRAG_PICKUP_INSERT;
        } else if (dragMode == DragMode.EXTRACT_FROM_CARRIED_SHULKER
            && ForgeQuickShulkerConfig.view().supportsBundlingExtract()
            && hoveredStack.isEmpty()
            && isSingleShulkerBox(carried)) {
            action = ShulkerBundlingAction.MOUSE_DRAG_EXTRACT;
        } else {
            return true;
        }

        DRAGGED_HOST_SLOTS.add(hostSlot.get());
        sendBundlingIntent(containerScreen, new ShulkerBundlingIntent(
            action,
            hostSlot.get(),
            containerScreen.getMenu().containerId,
            currentDragId
        ));
        return true;
    }

    private static void clearMouseDrag() {
        dragMode = DragMode.NONE;
        DRAGGED_HOST_SLOTS.clear();
        currentDragId = 0L;
        dragContainerId = -1;
    }

    private static ShulkerBundlingIntent prepareBundlingIntent(
        AbstractContainerScreen<?> containerScreen,
        ShulkerBundlingIntent intent
    ) {
        int containerId = containerScreen.getMenu().containerId;
        if (!ForgeQuickShulkerConfig.view().supportsMouseDragged()
            || (intent.action() != ShulkerBundlingAction.PICKUP_INSERT
            && intent.action() != ShulkerBundlingAction.EXTRACT)) {
            return new ShulkerBundlingIntent(intent.action(), intent.hostSlot(), containerId, 0L);
        }

        currentDragId = ThreadLocalRandom.current().nextLong();
        if (currentDragId == 0L) {
            currentDragId = 1L;
        }
        dragContainerId = containerId;
        return new ShulkerBundlingIntent(intent.action(), intent.hostSlot(), containerId, currentDragId);
    }

    private static void sendBundlingIntent(AbstractContainerScreen<?> containerScreen, ShulkerBundlingIntent intent) {
        Player player = Minecraft.getInstance().player;
        ItemStack carried = containerScreen.getMenu().getCarried().copy();
        if (player != null && player.getAbilities().instabuild && LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "Sending Forge creative bundling intent: action={}, hostSlot={}, screenMenuClass={}, screenCarried={}, playerContainerMenuClass={}, playerContainerCarried={}",
                intent.action(),
                intent.hostSlot(),
                containerScreen.getMenu().getClass().getName(),
                describeStack(carried),
                player.containerMenu.getClass().getName(),
                describeStack(player.containerMenu.getCarried())
            );
        }
        ForgeQuickShulkerNetwork.sendShulkerBundling(new ForgeShulkerBundlingPacket(intent, carried));
    }

    public static void applyEnderChestFullSync(String sessionId, List<ItemStack> stacks) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            LOGGER.debug("Ignored ender chest full sync because local player is unavailable: sessionId={}", sessionId);
            return;
        }

        Container enderChest = player.getEnderChestInventory();
        int containerSize = enderChest.getContainerSize();
        for (int slotIndex = 0; slotIndex < containerSize; slotIndex++) {
            ItemStack syncedStack = slotIndex < stacks.size() ? stacks.get(slotIndex) : ItemStack.EMPTY;
            enderChest.setItem(slotIndex, syncedStack.copy());
        }
        enderChest.setChanged();
        LOGGER.debug("Applied ender chest full sync: sessionId={}, slotCount={}", sessionId, stacks.size());
    }

    public static void applyEnderChestSlotSync(String sessionId, int slotIndex, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            LOGGER.debug(
                "Ignored ender chest slot sync because local player is unavailable: sessionId={}, slotIndex={}",
                sessionId,
                slotIndex
            );
            return;
        }

        Container enderChest = player.getEnderChestInventory();
        if (slotIndex < 0 || slotIndex >= enderChest.getContainerSize()) {
            LOGGER.debug(
                "Ignored ender chest slot sync with invalid slot index: sessionId={}, slotIndex={}, containerSize={}",
                sessionId,
                slotIndex,
                enderChest.getContainerSize()
            );
            return;
        }

        enderChest.setItem(slotIndex, stack.copy());
        enderChest.setChanged();
        LOGGER.debug("Applied ender chest slot sync: sessionId={}, slotIndex={}", sessionId, slotIndex);
    }

    private static boolean shouldBlockOffhandSwap(Screen screen, Minecraft minecraft, int keyCode, int scanCode) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        if (!(containerScreen.getMenu() instanceof ForgeQuickOpenMenu quickOpenMenu)) {
            return false;
        }
        if (!minecraft.options.keySwapOffhand.matches(keyCode, scanCode)) {
            return false;
        }

        HostSlotRef hostSlotRef = quickOpenMenu.hostSlotRef();
        if (hostSlotRef.scope() == HostStorageScope.PLAYER_OFFHAND) {
            return true;
        }

        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot == null) {
            return false;
        }

        return ForgeHostSlotResolver.forPlayerInventorySlot(minecraft.player, hoveredSlot, hoveredSlot.index)
            .map(hoveredHostSlot -> hoveredHostSlot.scope() == hostSlotRef.scope()
                && hoveredHostSlot.logicalSlotIndex() == hostSlotRef.logicalSlotIndex())
            .orElse(false);
    }

    private static boolean hasAnyEnabledQuickOpenable() {
        return ForgeQuickShulkerConfig.view().quickShulkerBox()
            || ForgeQuickShulkerConfig.view().quickCraftingTables()
            || ForgeQuickShulkerConfig.view().quickStonecutter()
            || ForgeQuickShulkerConfig.view().quickEnderChest()
            || ForgeQuickShulkerConfig.view().quickAnvil();
    }

    private static boolean isSingleShulkerBox(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() == 1 && isShulkerBox(stack);
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return !stack.isEmpty() && Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock;
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        return ForgeItemSnapshots.snapshot(stack).itemKey() + " x" + stack.getCount();
    }

    private enum DragMode {
        NONE,
        PICKUP_INTO_CARRIED_SHULKER,
        EXTRACT_FROM_CARRIED_SHULKER
    }
}
