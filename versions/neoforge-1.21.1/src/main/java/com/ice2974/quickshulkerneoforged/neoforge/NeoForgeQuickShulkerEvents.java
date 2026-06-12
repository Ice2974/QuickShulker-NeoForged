package com.ice2974.quickshulkerneoforged.neoforge;

import com.ice2974.quickshulkerneoforged.QuickShulkerConstants;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = QuickShulkerConstants.MOD_ID)
public final class NeoForgeQuickShulkerEvents {
    private NeoForgeQuickShulkerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer serverPlayer) {
            NeoForgeServices.SHULKER_SESSIONS.tick(serverPlayer);
            NeoForgeShulkerBundlingHandler.tickDragSession(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            NeoForgeShulkerBundlingHandler.clearDragSession(serverPlayer);
            NeoForgeServices.SHULKER_SESSIONS.finishSessionOnDisconnect(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            NeoForgeShulkerBundlingHandler.clearDragSession(serverPlayer);
            NeoForgeServices.SHULKER_SESSIONS.finishSessionOnDeath(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            NeoForgeShulkerBundlingHandler.clearDragSession(serverPlayer);
            NeoForgeServices.SHULKER_SESSIONS.finishSessionOnDimensionChange(serverPlayer);
        }
    }
}
