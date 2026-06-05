package com.ice2974.quickshulkerneoforged.neoforge.client;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.common.network.OpenHostItemIntent;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenTrigger;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeHostSlotResolver;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeItemSnapshots;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeQuickOpenMenu;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeQuickOpenRegistry;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeQuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.neoforge.network.NeoForgeOpenHostItemPayload;
import com.ice2974.quickshulkerneoforged.neoforge.network.NeoForgeQuickShulkerNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@EventBusSubscriber(modid = QuickShulkerConstants.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeQuickShulkerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoForgeQuickShulkerClient.class);

    private NeoForgeQuickShulkerClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        NeoForgeQuickOpenMouseRestore.onClientTick();

        if (!hasAnyEnabledQuickOpenable()
            || !NeoForgeQuickShulkerConfig.view().keybindInHand()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            return;
        }

        while (NeoForgeKeyMappings.OPEN_HELD_SHULKER.consumeClick()) {
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
            LOGGER.debug("Blocked offhand swap key while QuickShulker menu is open");
            event.setCanceled(true);
            return;
        }

        if (!NeoForgeQuickShulkerConfig.view().keybindInInventory()) {
            return;
        }

        if (NeoForgeKeyMappings.OPEN_HELD_SHULKER.matches(event.getKeyCode(), event.getScanCode())) {
            if (trySendHovered(minecraft.player, event.getScreen(), QuickOpenTrigger.INVENTORY_KEYBIND)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!hasAnyEnabledQuickOpenable()
            || !NeoForgeQuickShulkerConfig.view().rightClickInInventory()
            || !NeoForgeQuickShulkerConfig.view().rightClickToOpen()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || event.getButton() != 1) {
            return;
        }

        if (trySendHovered(player, event.getScreen(), QuickOpenTrigger.INVENTORY_RIGHT_CLICK)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        NeoForgeQuickOpenMouseRestore.onScreenInit(event.getScreen());
    }

    @SubscribeEvent
    public static void onHandRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!hasAnyEnabledQuickOpenable()
            || !NeoForgeQuickShulkerConfig.view().rightClickToOpen()) {
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
                    NeoForgeHostSlotResolver.forHand(player, hand),
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
        Optional<HostSlotRef> hostSlot = NeoForgeHostSlotResolver.forPlayerInventorySlot(player, containerScreen.getMenu(), hoveredSlot);
        if (hostSlot.isEmpty()) {
            LOGGER.debug(
                "Failed to resolve hovered quick-open slot: screenClass={}, menuClass={}, slotIndex={}, containerSlot={}, containerClass={}, usesPlayerInventory={}, inventoryMenu={}, hoveredItemKey={}, trigger={}",
                screen.getClass().getName(),
                containerScreen.getMenu().getClass().getName(),
                hoveredSlot.index,
                hoveredSlot.getContainerSlot(),
                hoveredSlot.container == null ? "<null>" : hoveredSlot.container.getClass().getName(),
                hoveredSlot.container == player.getInventory(),
                containerScreen.getMenu() instanceof net.minecraft.world.inventory.InventoryMenu,
                NeoForgeItemSnapshots.snapshot(stack).itemKey(),
                trigger
            );
            return false;
        }

        HostSlotRef requestedHostSlot = hostSlot.get();
        return resolveTypeId(stack)
            .map(type -> {
                if (type.requiresSingleHostStack() && stack.getCount() != 1) {
                    return false;
                }
                if (containerScreen.getMenu() instanceof NeoForgeQuickOpenMenu quickOpenMenu
                    && quickOpenMenu.isSameHost(type.id(), requestedHostSlot)) {
                    return false;
                }
                sendIntent(new OpenHostItemIntent(type.id(), requestedHostSlot, trigger));
                return true;
            })
            .orElse(false);
    }

    private static Optional<com.ice2974.quickshulkerneoforged.common.open.QuickOpenableType> resolveTypeId(ItemStack stack) {
        return NeoForgeQuickOpenRegistry.registry()
            .findTypeForItem(NeoForgeItemSnapshots.snapshot(stack).itemKey())
            .filter(type -> NeoForgeQuickShulkerConfig.view().isEnabled(type));
    }

    private static void sendIntent(OpenHostItemIntent intent) {
        NeoForgeQuickOpenMouseRestore.capture(Minecraft.getInstance().screen, intent.requestedTypeId());
        NeoForgeQuickShulkerNetwork.sendOpenHostItem(new NeoForgeOpenHostItemPayload(intent));
    }

    private static boolean shouldBlockOffhandSwap(Screen screen, Minecraft minecraft, int keyCode, int scanCode) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        if (!(containerScreen.getMenu() instanceof NeoForgeQuickOpenMenu)) {
            return false;
        }
        return minecraft.options.keySwapOffhand.matches(keyCode, scanCode);
    }

    private static boolean hasAnyEnabledQuickOpenable() {
        return NeoForgeQuickShulkerConfig.view().quickShulkerBox()
            || NeoForgeQuickShulkerConfig.view().quickCraftingTables()
            || NeoForgeQuickShulkerConfig.view().quickStonecutter()
            || NeoForgeQuickShulkerConfig.view().quickEnderChest()
            || NeoForgeQuickShulkerConfig.view().quickAnvil();
    }
}
