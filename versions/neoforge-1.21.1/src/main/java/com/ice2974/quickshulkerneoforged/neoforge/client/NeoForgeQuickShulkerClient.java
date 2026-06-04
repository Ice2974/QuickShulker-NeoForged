package com.ice2974.quickshulkerneoforged.neoforge.client;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.common.network.OpenHostItemIntent;
import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenTrigger;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeHostSlotResolver;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeItemSnapshots;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeQuickOpenRegistry;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeQuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.neoforge.network.NeoForgeOpenHostItemPayload;
import com.ice2974.quickshulkerneoforged.neoforge.network.NeoForgeQuickShulkerNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = QuickShulkerConstants.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeQuickShulkerClient {
    private NeoForgeQuickShulkerClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!NeoForgeQuickShulkerConfig.view().keybindInHand()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            return;
        }

        while (NeoForgeKeyMappings.OPEN_HELD_SHULKER.consumeClick()) {
            if (trySend(player, InteractionHand.MAIN_HAND) || trySend(player, InteractionHand.OFF_HAND)) {
                return;
            }
        }
    }

    private static boolean trySend(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || stack.getCount() != 1) {
            return false;
        }

        return NeoForgeQuickOpenRegistry.registry()
            .findTypeForItem(NeoForgeItemSnapshots.snapshot(stack).itemKey())
            .filter(type -> type.id().equals(BuiltinQuickOpenables.SHULKER_BOX.id()))
            .map(type -> {
                NeoForgeQuickShulkerNetwork.sendOpenHostItem(new NeoForgeOpenHostItemPayload(new OpenHostItemIntent(
                    type.id(),
                    NeoForgeHostSlotResolver.forHand(player, hand),
                    QuickOpenTrigger.HAND_KEYBIND
                )));
                return true;
            })
            .orElse(false);
    }
}
