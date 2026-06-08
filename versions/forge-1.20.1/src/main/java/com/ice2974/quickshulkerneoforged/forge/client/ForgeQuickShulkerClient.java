package com.ice2974.quickshulkerneoforged.forge.client;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.common.network.OpenHostItemIntent;
import com.ice2974.quickshulkerneoforged.common.network.ReopenPlayerInventoryIntent;
import com.ice2974.quickshulkerneoforged.common.network.ReopenPlayerInventoryQueue;
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
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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

import java.util.Optional;
import java.util.List;

@Mod.EventBusSubscriber(modid = QuickShulkerConstants.MOD_ID, value = Dist.CLIENT)
public final class ForgeQuickShulkerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeQuickShulkerClient.class);
    private static boolean suppressNextInventoryRightRelease;

    private ForgeQuickShulkerClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ForgeQuickOpenMouseRestore.onClientTick();
        processPendingInventoryReopen();

        if (!hasAnyEnabledQuickOpenable()
            || !ForgeQuickShulkerConfig.view().keybindInHand()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.screen != null) {
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

        if (trySendBundlingIntent(player, event.getScreen())) {
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
    public static void onScreenMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() != 1 || !suppressNextInventoryRightRelease) {
            return;
        }

        suppressNextInventoryRightRelease = false;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        suppressNextInventoryRightRelease = false;
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

    private static boolean trySendBundlingIntent(Player player, Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }

        Slot hoveredSlot = containerScreen.getSlotUnderMouse();
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return false;
        }

        Optional<HostSlotRef> hostSlot = ForgeHostSlotResolver.forPlayerInventorySlot(player, hoveredSlot, hoveredSlot.index);
        if (hostSlot.isEmpty()) {
            return false;
        }
        if (containerScreen.getMenu() instanceof ForgeQuickOpenMenu quickOpenMenu
            && HostIdentity.sameSlot(quickOpenMenu.hostSlotRef(), hostSlot.get())) {
            return false;
        }

        ItemStack carried = containerScreen.getMenu().getCarried();
        ItemStack hoveredStack = hoveredSlot.getItem();
        if (ForgeQuickShulkerConfig.view().supportsBundlingInsert()
            && !carried.isEmpty()
            && !isShulkerBox(carried)
            && isSingleShulkerBox(hoveredStack)) {
            sendBundlingIntent(new ShulkerBundlingIntent(ShulkerBundlingAction.INSERT, hostSlot.get()));
            return true;
        }

        if (ForgeQuickShulkerConfig.view().supportsBundlingPickup()
            && isSingleShulkerBox(carried)
            && !hoveredStack.isEmpty()
            && !isShulkerBox(hoveredStack)
            && hoveredStack.getItem().canFitInsideContainerItems()) {
            sendBundlingIntent(new ShulkerBundlingIntent(ShulkerBundlingAction.PICKUP_INSERT, hostSlot.get()));
            return true;
        }

        return false;
    }

    private static void sendBundlingIntent(ShulkerBundlingIntent intent) {
        ItemStack carried = Minecraft.getInstance().player == null
            ? ItemStack.EMPTY
            : Minecraft.getInstance().player.containerMenu.getCarried().copy();
        ForgeQuickShulkerNetwork.sendShulkerBundling(new ForgeShulkerBundlingPacket(intent, carried));
    }

    public static void schedulePendingInventoryReopenAndProcess(ReopenPlayerInventoryIntent intent) {
        ReopenPlayerInventoryQueue.schedule(intent);
        processPendingInventoryReopen();
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

    private static void processPendingInventoryReopen() {
        ReopenPlayerInventoryQueue.PendingReopen pendingReopen = ReopenPlayerInventoryQueue.pending();
        if (pendingReopen == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (pendingReopen.isExpired(System.nanoTime())) {
            LOGGER.debug("Dropped pending inventory reopen after timeout: sessionId={}", pendingReopen.sessionId());
            ReopenPlayerInventoryQueue.clear();
            return;
        }
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.screen instanceof InventoryScreen) {
            ReopenPlayerInventoryQueue.clear();
            return;
        }
        if (minecraft.screen != null) {
            return;
        }

        minecraft.setScreen(new InventoryScreen(minecraft.player));
        ReopenPlayerInventoryQueue.clear();
    }
}
