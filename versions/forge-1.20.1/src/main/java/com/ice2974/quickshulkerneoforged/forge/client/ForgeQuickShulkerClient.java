package com.ice2974.quickshulkerneoforged.forge.client;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import com.ice2974.quickshulkerneoforged.common.network.OpenHostItemIntent;
import com.ice2974.quickshulkerneoforged.common.open.BuiltinQuickOpenables;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenTrigger;
import com.ice2974.quickshulkerneoforged.forge.ForgeHostSlotResolver;
import com.ice2974.quickshulkerneoforged.forge.ForgeItemSnapshots;
import com.ice2974.quickshulkerneoforged.forge.ForgeQuickOpenRegistry;
import com.ice2974.quickshulkerneoforged.forge.ForgeQuickShulkerConfig;
import com.ice2974.quickshulkerneoforged.forge.network.ForgeOpenHostItemPacket;
import com.ice2974.quickshulkerneoforged.forge.network.ForgeQuickShulkerNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = QuickShulkerConstants.MOD_ID, value = Dist.CLIENT)
public final class ForgeQuickShulkerClient {
    private ForgeQuickShulkerClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ForgeQuickShulkerConfig.view().keybindInHand()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            return;
        }

        while (ForgeKeyMappings.OPEN_HELD_SHULKER.consumeClick()) {
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

        return ForgeQuickOpenRegistry.registry()
            .findTypeForItem(ForgeItemSnapshots.snapshot(stack).itemKey())
            .filter(type -> type.id().equals(BuiltinQuickOpenables.SHULKER_BOX.id()))
            .map(type -> {
                ForgeQuickShulkerNetwork.sendOpenHostItem(new ForgeOpenHostItemPacket(new OpenHostItemIntent(
                    type.id(),
                    ForgeHostSlotResolver.forHand(player, hand),
                    QuickOpenTrigger.HAND_KEYBIND
                )));
                return true;
            })
            .orElse(false);
    }
}
