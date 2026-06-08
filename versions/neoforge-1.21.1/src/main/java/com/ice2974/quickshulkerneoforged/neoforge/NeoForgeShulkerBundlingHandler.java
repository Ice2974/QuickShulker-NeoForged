package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.common.bundling.ShulkerBundlingResult;
import com.ice2974.quickshulkerneoforged.common.network.ShulkerBundlingIntent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class NeoForgeShulkerBundlingHandler {
    private static final NeoForgeShulkerBundlingHelper HELPER = new NeoForgeShulkerBundlingHelper();

    private NeoForgeShulkerBundlingHandler() {
    }

    public static void handle(ServerPlayer player, ShulkerBundlingIntent intent) {
        if (player == null
            || player.getAbilities().instabuild
            || player.containerMenu instanceof NeoForgeQuickOpenMenu) {
            return;
        }

        switch (intent.action()) {
            case INSERT -> handleInsert(player, intent);
            case PICKUP_INSERT -> handlePickupInsert(player, intent);
        }
    }

    private static void handleInsert(ServerPlayer player, ShulkerBundlingIntent intent) {
        if (!NeoForgeQuickShulkerConfig.view().supportsBundlingInsert()) {
            return;
        }

        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty() || isShulkerBox(carried)) {
            return;
        }

        ItemStack hostStack = NeoForgeHostSlotResolver.resolve(player, intent.hostSlot());
        ShulkerBundlingResult<ItemStack, ItemStack> result = HELPER.insertIntoShulker(hostStack, carried);
        if (!result.changed()) {
            return;
        }

        NeoForgeHostSlotResolver.set(player, intent.hostSlot(), result.updatedContainerStack().orElseThrow().copy());
        player.containerMenu.setCarried(result.updatedInputStack().orElseThrow().copy());
        syncPlayerInventory(player);
    }

    private static void handlePickupInsert(ServerPlayer player, ShulkerBundlingIntent intent) {
        if (!NeoForgeQuickShulkerConfig.view().supportsBundlingPickup()) {
            return;
        }

        ItemStack carried = player.containerMenu.getCarried();
        if (!isSingleShulkerBox(carried)) {
            return;
        }

        ItemStack hostStack = NeoForgeHostSlotResolver.resolve(player, intent.hostSlot());
        if (hostStack.isEmpty() || isShulkerBox(hostStack) || !hostStack.getItem().canFitInsideContainerItems()) {
            return;
        }

        ShulkerBundlingResult<ItemStack, ItemStack> result = HELPER.insertIntoShulker(carried, hostStack);
        if (!result.changed()) {
            return;
        }

        player.containerMenu.setCarried(result.updatedContainerStack().orElseThrow().copy());
        NeoForgeHostSlotResolver.set(player, intent.hostSlot(), result.updatedInputStack().orElseThrow().copy());
        syncPlayerInventory(player);
    }

    private static void syncPlayerInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static boolean isSingleShulkerBox(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() == 1 && isShulkerBox(stack);
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return !stack.isEmpty() && Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock;
    }
}
