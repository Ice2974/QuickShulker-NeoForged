package com.ice2974.quickshulkerneoforged.neoforge.client;

import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeQuickOpenMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

final class NeoForgeQuickOpenMouseRestore {
    private static final int MAX_PENDING_TICKS = 40;
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoForgeQuickOpenMouseRestore.class);

    private static PendingRestore pendingRestore;

    private NeoForgeQuickOpenMouseRestore() {
    }

    static void capture(Screen sourceScreen, String requestedTypeId) {
        if (!(sourceScreen instanceof AbstractContainerScreen<?>)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        pendingRestore = new PendingRestore(
            minecraft.mouseHandler.xpos(),
            minecraft.mouseHandler.ypos(),
            sourceScreen,
            requestedTypeId,
            MAX_PENDING_TICKS
        );
        LOGGER.debug(
            "Captured quick-open mouse position: screenClass={}, requestedType={}, mouseX={}, mouseY={}",
            sourceScreen.getClass().getName(),
            requestedTypeId,
            pendingRestore.mouseX(),
            pendingRestore.mouseY()
        );
    }

    static void onClientTick() {
        PendingRestore pending = pendingRestore;
        if (pending == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Screen currentScreen = minecraft.screen;
        if (tryRestore(minecraft, currentScreen, pending, "client_tick")) {
            return;
        }

        if (pending.remainingTicks() <= 1) {
            LOGGER.debug(
                "Discarded pending quick-open mouse restore after timeout: sourceScreenClass={}, requestedType={}",
                pending.sourceScreen().getClass().getName(),
                pending.requestedTypeId()
            );
            pendingRestore = null;
            return;
        }

        pendingRestore = pending.tickDown();
    }

    static void onScreenInit(Screen screen) {
        PendingRestore pending = pendingRestore;
        if (pending == null) {
            return;
        }

        tryRestore(Minecraft.getInstance(), screen, pending, "screen_init");
    }

    private static boolean tryRestore(Minecraft minecraft, Screen currentScreen, PendingRestore pending, String source) {
        if (currentScreen == pending.sourceScreen() || !isExpectedQuickOpenScreen(currentScreen, pending.requestedTypeId())) {
            return false;
        }

        GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), pending.mouseX(), pending.mouseY());
        LOGGER.debug(
            "Restored quick-open mouse position via {}: screenClass={}, requestedType={}, mouseX={}, mouseY={}",
            source,
            currentScreen.getClass().getName(),
            pending.requestedTypeId(),
            pending.mouseX(),
            pending.mouseY()
        );
        pendingRestore = null;
        return true;
    }

    private static boolean isExpectedQuickOpenScreen(Screen screen, String requestedTypeId) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        if (!(containerScreen.getMenu() instanceof NeoForgeQuickOpenMenu quickOpenMenu)) {
            return false;
        }
        return requestedTypeId.equals(quickOpenMenu.quickOpenableTypeId());
    }

    private record PendingRestore(double mouseX, double mouseY, Screen sourceScreen, String requestedTypeId, int remainingTicks) {
        private PendingRestore tickDown() {
            return new PendingRestore(mouseX, mouseY, sourceScreen, requestedTypeId, remainingTicks - 1);
        }
    }
}
