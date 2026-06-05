package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.open.DefaultHostItemValidator;
import com.ice2974.quickshulkerneoforged.common.open.HostItemReference;
import com.ice2974.quickshulkerneoforged.common.open.HostValidationMode;
import com.ice2974.quickshulkerneoforged.common.open.HostValidationResult;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenMenuKind;
import com.ice2974.quickshulkerneoforged.common.open.QuickOpenTrigger;
import com.ice2974.quickshulkerneoforged.common.session.CloseReason;
import com.ice2974.quickshulkerneoforged.common.session.MenuOpenIntent;
import com.ice2974.quickshulkerneoforged.common.session.OpenSession;
import com.ice2974.quickshulkerneoforged.common.session.OpenSessionSafetyPolicy;
import com.ice2974.quickshulkerneoforged.common.session.SaveDisposition;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ForgeShulkerSessionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeShulkerSessionManager.class);

    private final ForgeShulkerContentAccess contentAccess = new ForgeShulkerContentAccess();
    private final DefaultHostItemValidator validator = new DefaultHostItemValidator();
    private final Map<UUID, ActiveSession> sessions = new ConcurrentHashMap<>();

    public void open(ServerPlayer player, HostItemReference hostItemReference, QuickOpenTrigger trigger) {
        ActiveSession existingSession = sessions.get(player.getUUID());
        if (existingSession != null) {
            LOGGER.debug(
                "Rejected quick-open request because an active session already exists: player={}, requestedHostSlot={}, activeHostSlot={}",
                player.getScoreboardName(),
                hostItemReference.slotRef(),
                existingSession.hostItem().slotRef()
            );
            return;
        }

        ActiveSession session = switch (hostItemReference.quickOpenableTypeId()) {
            case "shulker_box" -> openShulkerSession(player, hostItemReference, trigger);
            case "ender_chest" -> openEnderChestSession(player, hostItemReference, trigger);
            default -> null;
        };
        if (session != null) {
            sessions.put(player.getUUID(), session);
        }
    }

    public void tick(ServerPlayer player) {
        ActiveSession session = sessions.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (player.containerMenu != session.menu()) {
            finishSession(player, null, CloseReason.PLAYER_CLOSED, "tick_menu_mismatch");
            return;
        }
        HostValidationResult validation = validateCurrentHost(player, session.hostItem());
        if (!validation.valid()) {
            if (session.menu() instanceof ForgeQuickOpenMenu quickOpenMenu) {
                quickOpenMenu.markHostInvalidated();
            }
            sessions.put(player.getUUID(), session.withCloseReason(CloseReason.HOST_INVALIDATED));
            LOGGER.debug("Closing quick-open menu because host became invalid: {}", validation.failure());
            player.closeContainer();
        }
    }

    public void finishSession(ServerPlayer player, AbstractContainerMenu menu) {
        finishSession(player, menu, CloseReason.PLAYER_CLOSED, "menu_removed");
    }

    public void finishSession(ServerPlayer player, AbstractContainerMenu menu, CloseReason closeReason) {
        finishSession(player, menu, closeReason, "unspecified");
    }

    public void finishSession(ServerPlayer player, AbstractContainerMenu menu, CloseReason closeReason, String source) {
        ActiveSession session = sessions.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (menu != null && session.menu() != menu) {
            return;
        }
        sessions.remove(player.getUUID(), session);
        if (closeReason != null && session.closeReason() != closeReason) {
            session = session.withCloseReason(closeReason);
        }

        HostValidationResult validation = validateCurrentHost(player, session.hostItem());
        if (!validation.valid() && isNormalCloseReason(session.closeReason())) {
            session = session.withCloseReason(CloseReason.HOST_INVALIDATED);
        }

        OpenSession evaluatedSession = session.isDirty() ? session.openSession().markDirty() : session.openSession();
        SaveDisposition disposition = decideSaveDisposition(validation, session.closeReason());
        boolean wroteContents = false;

        if (disposition == SaveDisposition.SAVE_TO_HOST && session.shouldWriteBackToHost()) {
            ItemStack hostStack = ForgeHostSlotResolver.resolve(player, session.hostItem().slotRef());
            contentAccess.writeItemStacks(hostStack, session.shulkerContainer().copyContents());
            wroteContents = true;
        } else if (disposition != SaveDisposition.SAVE_TO_HOST) {
            LOGGER.debug("Discarded quick shulker changes: {}", disposition);
        }

        LOGGER.debug(
            "Finished quick-open session via source={}, type={}, closeReason={}, valid={}, dirty={}, disposition={}, wroteContents={}, sessionState={}",
            source,
            session.hostItem().quickOpenableTypeId(),
            session.closeReason(),
            validation.valid(),
            session.isDirty(),
            disposition,
            wroteContents,
            evaluatedSession.state()
        );
    }

    public void finishSessionOnDisconnect(ServerPlayer player) {
        finishSession(player, null, CloseReason.PLAYER_DISCONNECTED, "player_logged_out");
    }

    public void finishSessionOnDeath(ServerPlayer player) {
        finishSession(player, null, CloseReason.PLAYER_DIED, "player_respawned");
    }

    public void finishSessionOnDimensionChange(ServerPlayer player) {
        finishSession(player, null, CloseReason.DIMENSION_CHANGED, "player_changed_dimension");
    }

    public HostValidationResult validateCurrentHost(ServerPlayer player, HostItemReference hostItemReference) {
        ItemStack currentStack = ForgeHostSlotResolver.resolve(player, hostItemReference.slotRef());
        return validator.validate(
            hostItemReference,
            ForgeItemSnapshots.snapshot(currentStack),
            HostValidationMode.SAME_ITEM_TYPE_AND_SINGLE_COUNT,
            true
        );
    }

    private static boolean isNormalCloseReason(CloseReason closeReason) {
        return closeReason == CloseReason.PLAYER_CLOSED
            || closeReason == CloseReason.PLAYER_DISCONNECTED
            || closeReason == CloseReason.PLAYER_DIED
            || closeReason == CloseReason.DIMENSION_CHANGED;
    }

    private static SaveDisposition decideSaveDisposition(HostValidationResult validation, CloseReason closeReason) {
        if (!validation.valid()) {
            return SaveDisposition.DISCARD_CHANGES;
        }
        if (closeReason == CloseReason.HOST_INVALIDATED || closeReason == CloseReason.VALIDATION_REJECTED) {
            return SaveDisposition.DISCARD_CHANGES;
        }
        if (isNormalCloseReason(closeReason)) {
            return SaveDisposition.SAVE_TO_HOST;
        }
        return SaveDisposition.DISCARD_CHANGES;
    }

    private ActiveSession openShulkerSession(ServerPlayer player, HostItemReference hostItemReference, QuickOpenTrigger trigger) {
        ItemStack hostStack = ForgeHostSlotResolver.resolve(player, hostItemReference.slotRef());
        ItemBackedShulkerContainer container = new ItemBackedShulkerContainer(contentAccess, hostStack);
        OpenSession openSession = createOpenSession(hostItemReference, trigger, QuickOpenMenuKind.SHULKER_BOX);

        final ForgeShulkerMenu[] holder = new ForgeShulkerMenu[1];
        player.openMenu(new SimpleMenuProvider(
            (containerId, inventory, serverPlayer) -> {
                ForgeShulkerMenu menu = new ForgeShulkerMenu(containerId, inventory, container, this, hostItemReference.slotRef());
                holder[0] = menu;
                return menu;
            },
            hostStack.hasCustomHoverName() ? hostStack.getHoverName() : Component.translatable("container.shulkerBox")
        ));

        if (holder[0] == null) {
            return null;
        }
        return ActiveSession.forShulker(openSession, hostItemReference, holder[0], container, CloseReason.PLAYER_CLOSED);
    }

    private ActiveSession openEnderChestSession(ServerPlayer player, HostItemReference hostItemReference, QuickOpenTrigger trigger) {
        OpenSession openSession = createOpenSession(hostItemReference, trigger, QuickOpenMenuKind.ENDER_CHEST);

        final ForgeEnderChestMenu[] holder = new ForgeEnderChestMenu[1];
        player.openMenu(new SimpleMenuProvider(
            (containerId, inventory, serverPlayer) -> {
                ForgeEnderChestMenu menu = new ForgeEnderChestMenu(
                    containerId,
                    inventory,
                    serverPlayer.getEnderChestInventory(),
                    this,
                    hostItemReference.slotRef()
                );
                holder[0] = menu;
                return menu;
            },
            Component.translatable("container.enderchest")
        ));

        if (holder[0] == null) {
            return null;
        }
        return ActiveSession.forTransient(openSession, hostItemReference, holder[0], CloseReason.PLAYER_CLOSED);
    }

    private static OpenSession createOpenSession(
        HostItemReference hostItemReference,
        QuickOpenTrigger trigger,
        QuickOpenMenuKind menuKind
    ) {
        return OpenSession.create(
            ForgeQuickOpenHandler.createRequest(hostItemReference.quickOpenableTypeId(), hostItemReference.slotRef(), trigger),
            hostItemReference,
            new MenuOpenIntent(
                "pending",
                hostItemReference.quickOpenableTypeId(),
                menuKind,
                hostItemReference,
                true,
                false
            ),
            OpenSessionSafetyPolicy.strict()
        );
    }

    private record ActiveSession(
        OpenSession openSession,
        HostItemReference hostItem,
        AbstractContainerMenu menu,
        ItemBackedShulkerContainer shulkerContainer,
        boolean writeBackToHost,
        CloseReason closeReason
    ) {
        private static ActiveSession forShulker(
            OpenSession openSession,
            HostItemReference hostItem,
            AbstractContainerMenu menu,
            ItemBackedShulkerContainer shulkerContainer,
            CloseReason closeReason
        ) {
            return new ActiveSession(openSession, hostItem, menu, shulkerContainer, true, closeReason);
        }

        private static ActiveSession forTransient(
            OpenSession openSession,
            HostItemReference hostItem,
            AbstractContainerMenu menu,
            CloseReason closeReason
        ) {
            return new ActiveSession(openSession, hostItem, menu, null, false, closeReason);
        }

        private ActiveSession withCloseReason(CloseReason nextReason) {
            return new ActiveSession(openSession, hostItem, menu, shulkerContainer, writeBackToHost, nextReason);
        }

        private boolean shouldWriteBackToHost() {
            return writeBackToHost && shulkerContainer != null;
        }

        private boolean isDirty() {
            return shulkerContainer != null && shulkerContainer.isDirty();
        }
    }
}
