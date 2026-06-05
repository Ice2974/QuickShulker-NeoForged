package com.ice2974.quickshulkerneoforged.forge.client;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.common.network.OpenHostItemIntent;
import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenTrigger;
import com.ice2974.quickshulkerneoforged.forge.ForgeHostSlotResolver;
import com.ice2974.quickshulkerneoforged.forge.ForgeItemSnapshots;
import com.ice2974.quickshulkerneoforged.forge.ForgeQuickOpenRegistry;
import com.ice2974.quickshulkerneoforged.forge.ForgeQuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.forge.ForgeShulkerMenu;
import com.ice2974.quickshulkerneoforged.forge.network.ForgeOpenHostItemPacket;
import com.ice2974.quickshulkerneoforged.forge.network.ForgeQuickShulkerNetwork;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = QuickShulkerConstants.MOD_ID, value = Dist.CLIENT)
public final class ForgeQuickShulkerClient {
    private ForgeQuickShulkerClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END
            || !ForgeQuickShulkerConfig.view().quickShulkerBox()
            || !ForgeQuickShulkerConfig.view().keybindInHand()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            return;
        }

        while (ForgeKeyMappings.OPEN_HELD_SHULKER.consumeClick()) {
            if (trySendHeld(player, InteractionHand.MAIN_HAND) || trySendHeld(player, InteractionHand.OFF_HAND)) {
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!ForgeQuickShulkerConfig.view().quickShulkerBox()
            || !ForgeQuickShulkerConfig.view().keybindInInventory()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
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
        if (!ForgeQuickShulkerConfig.view().quickShulkerBox()
            || !ForgeQuickShulkerConfig.view().rightClickInInventory()
            || !ForgeQuickShulkerConfig.view().rightClickToOpen()) {
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

    private static boolean trySendHeld(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || stack.getCount() != 1) {
            return false;
        }

        return resolveTypeId(stack)
            .map(typeId -> {
                sendIntent(new OpenHostItemIntent(
                    typeId,
                    ForgeHostSlotResolver.forHand(player, hand),
                    QuickOpenTrigger.HAND_KEYBIND
                ));
                return true;
            })
            .orElse(false);
    }

    private static boolean trySendHovered(Player player, Screen screen, QuickOpenTrigger trigger) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        if (containerScreen.getMenu() instanceof ForgeShulkerMenu) {
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
        if (stack.getCount() != 1) {
            return false;
        }

        Optional<HostSlotRef> hostSlot = ForgeHostSlotResolver.forPlayerInventorySlot(player, hoveredSlot, hoveredSlot.index);
        if (hostSlot.isEmpty()) {
            return false;
        }

        return resolveTypeId(stack)
            .map(typeId -> {
                sendIntent(new OpenHostItemIntent(typeId, hostSlot.get(), trigger));
                return true;
            })
            .orElse(false);
    }

    private static Optional<String> resolveTypeId(ItemStack stack) {
        return ForgeQuickOpenRegistry.registry()
            .findTypeForItem(ForgeItemSnapshots.snapshot(stack).itemKey())
            .filter(type -> ForgeQuickShulkerConfig.view().isEnabled(type))
            .filter(type -> type.id().equals(BuiltinQuickOpenables.SHULKER_BOX.id()))
            .map(type -> type.id());
    }

    private static void sendIntent(OpenHostItemIntent intent) {
        ForgeQuickShulkerNetwork.sendOpenHostItem(new ForgeOpenHostItemPacket(intent));
    }
}
