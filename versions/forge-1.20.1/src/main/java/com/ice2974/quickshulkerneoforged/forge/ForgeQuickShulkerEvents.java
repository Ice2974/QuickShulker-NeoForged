package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = QuickShulkerConstants.MOD_ID)
public final class ForgeQuickShulkerEvents {
    private ForgeQuickShulkerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer) {
            ForgeServices.SHULKER_SESSIONS.tick(serverPlayer);
        }
    }
}
