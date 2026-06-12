package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent;

@Mod.EventBusSubscriber(modid = QuickShulkerConstants.MOD_ID)
public final class ForgeQuickShulkerEvents {
    private ForgeQuickShulkerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer) {
            ForgeServices.SHULKER_SESSIONS.tick(serverPlayer);
            ForgeShulkerBundlingHandler.tickDragSession(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ForgeShulkerBundlingHandler.clearDragSession(serverPlayer);
            ForgeServices.SHULKER_SESSIONS.finishSessionOnDisconnect(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ForgeShulkerBundlingHandler.clearDragSession(serverPlayer);
            ForgeServices.SHULKER_SESSIONS.finishSessionOnDeath(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ForgeShulkerBundlingHandler.clearDragSession(serverPlayer);
            ForgeServices.SHULKER_SESSIONS.finishSessionOnDimensionChange(serverPlayer);
        }
    }
}
