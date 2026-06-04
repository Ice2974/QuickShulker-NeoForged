package com.ice2974.quickshulkerneoforged.forge;

import com.ice2974.quickshulkerneoforged.common.open.DefaultHostItemValidator;
import com.ice2974.quickshulkerneoforged.common.open.HostItemReference;
import com.ice2974.quickshulkerneoforged.common.open.HostValidationMode;
import com.ice2974.quickshulkerneoforged.common.open.HostValidationResult;
import com.ice2974.quickshulkerneoforged.common.session.CloseReason;
import com.ice2974.quickshulkerneoforged.common.session.MenuOpenIntent;
import com.ice2974.quickshulkerneoforged.common.session.OpenSession;
import com.ice2974.quickshulkerneoforged.common.session.OpenSessionRules;
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
                ForgeShulkerMenu menu = new ForgeShulkerMenu(containerId, inventory, container, this);
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
        if (session == null || player.containerMenu != session.menu()) {
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
        ActiveSession session = sessions.remove(player.getUUID());
        if (session == null || session.menu() != menu) {
            return;
        }

        HostValidationResult validation = validateCurrentHost(player, session.hostItem());
        if (!validation.valid() && session.closeReason() == CloseReason.PLAYER_CLOSED) {
            session = session.withCloseReason(CloseReason.HOST_INVALIDATED);
        }

        OpenSession evaluatedSession = session.container().isDirty() ? session.openSession().markDirty() : session.openSession();
        SaveDisposition disposition = OpenSessionRules.decideSaveDisposition(
            evaluatedSession,
            validation,
            session.closeReason()
        );

        if (disposition == SaveDisposition.SAVE_TO_HOST) {
            ItemStack hostStack = ForgeHostSlotResolver.resolve(player, session.hostItem().slotRef());
            contentAccess.writeItemStacks(hostStack, session.container().copyContents());
        } else {
            LOGGER.debug("Discarded quick shulker changes: {}", disposition);
        }
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
