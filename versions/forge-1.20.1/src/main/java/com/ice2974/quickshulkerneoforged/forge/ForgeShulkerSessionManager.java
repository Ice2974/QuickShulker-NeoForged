package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.open.DefaultHostItemValidator;
import com.ice2974.quickshulkerneoforged.common.open.HostItemReference;
import com.ice2974.quickshulkerneoforged.common.open.HostValidationMode;
import com.ice2974.quickshulkerneoforged.common.open.HostValidationResult;
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
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ForgeShulkerSessionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeShulkerSessionManager.class);

    private final ForgeShulkerContentAccess contentAccess = new ForgeShulkerContentAccess();
    private final DefaultHostItemValidator validator = new DefaultHostItemValidator();
    private final Map<UUID, ActiveSession> sessions = new ConcurrentHashMap<>();

    public void open(ServerPlayer player, HostItemReference hostItemReference) {
        ActiveSession existingSession = sessions.get(player.getUUID());
        if (existingSession != null) {
            LOGGER.debug(
                "Rejected quick shulker open because an active session already exists: player={}, requestedHostSlot={}, activeHostSlot={}",
                player.getScoreboardName(),
                hostItemReference.slotRef(),
                existingSession.hostItem().slotRef()
            );
            return;
        }

        ItemStack hostStack = ForgeHostSlotResolver.resolve(player, hostItemReference.slotRef());
        ItemBackedShulkerContainer container = new ItemBackedShulkerContainer(contentAccess, hostStack);
        OpenSession openSession = OpenSession.create(
            ForgeQuickOpenHandler.createRequest(hostItemReference.slotRef()),
            hostItemReference,
            new MenuOpenIntent(
                "pending",
                hostItemReference.quickOpenableTypeId(),
                com.ice2974.quickshulkerneoforged.common.open.QuickOpenMenuKind.SHULKER_BOX,
                hostItemReference,
                true,
                false
            ),
            OpenSessionSafetyPolicy.strict()
        );

        final ForgeShulkerMenu[] holder = new ForgeShulkerMenu[1];
        player.openMenu(new SimpleMenuProvider(
            (containerId, inventory, serverPlayer) -> {
                ForgeShulkerMenu menu = new ForgeShulkerMenu(containerId, inventory, container, this, hostItemReference.slotRef());
                holder[0] = menu;
                return menu;
            },
            hostStack.hasCustomHoverName() ? hostStack.getHoverName() : Component.translatable("container.shulkerBox")
        ));

        if (holder[0] != null) {
            sessions.put(player.getUUID(), new ActiveSession(openSession, hostItemReference, holder[0], container, CloseReason.PLAYER_CLOSED));
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
            session.menu().markHostInvalidated();
            sessions.put(player.getUUID(), session.withCloseReason(CloseReason.HOST_INVALIDATED));
            LOGGER.debug("Closing quick shulker menu because host became invalid: {}", validation.failure());
            player.closeContainer();
        }
    }

    public void finishSession(ServerPlayer player, ForgeShulkerMenu menu) {
        finishSession(player, menu, CloseReason.PLAYER_CLOSED, "menu_removed");
    }

    public void finishSession(ServerPlayer player, ForgeShulkerMenu menu, CloseReason closeReason) {
        finishSession(player, menu, closeReason, "unspecified");
    }

    public void finishSession(ServerPlayer player, ForgeShulkerMenu menu, CloseReason closeReason, String source) {
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

        OpenSession evaluatedSession = session.container().isDirty() ? session.openSession().markDirty() : session.openSession();
        SaveDisposition disposition = decideSaveDisposition(validation, session.closeReason());
        boolean wroteContents = false;

        if (disposition == SaveDisposition.SAVE_TO_HOST) {
            ItemStack hostStack = ForgeHostSlotResolver.resolve(player, session.hostItem().slotRef());
            contentAccess.writeItemStacks(hostStack, session.container().copyContents());
            wroteContents = true;
        } else {
            LOGGER.debug("Discarded quick shulker changes: {}", disposition);
        }

        LOGGER.debug(
            "Finished quick shulker session via source={}, closeReason={}, valid={}, dirty={}, disposition={}, wroteContents={}",
            source,
            session.closeReason(),
            validation.valid(),
            session.container().isDirty(),
            disposition,
            wroteContents
        );
    }

    public void finishSessionOnDisconnect(ServerPlayer player) {
        finishSession(player, null, CloseReason.PLAYER_DISCONNECTED, "player_logged_out");
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

    private record ActiveSession(
        OpenSession openSession,
        HostItemReference hostItem,
        ForgeShulkerMenu menu,
        ItemBackedShulkerContainer container,
        CloseReason closeReason
    ) {
        private ActiveSession withCloseReason(CloseReason nextReason) {
            return new ActiveSession(openSession, hostItem, menu, container, nextReason);
        }
    }
}
