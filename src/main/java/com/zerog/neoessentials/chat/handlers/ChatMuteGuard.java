package com.zerog.neoessentials.chat.handlers;

import com.zerog.neoessentials.chat.MuteManager;
import com.zerog.neoessentials.moderation.BanManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enforces chat mutes at the earliest possible point in the {@link ServerChatEvent} pipeline.
 * <p>
 * This runs at {@link EventPriority#HIGHEST} — i.e. before any NORMAL-priority chat handler,
 * including NeoEssentials' own {@code ChatHandler} AND any third-party chat mod (e.g. chatter-mod)
 * that formats/re-broadcasts chat by cancelling the event.
 * <p>
 * <b>Why this exists as a separate HIGHEST listener rather than an inline check inside ChatHandler:</b>
 * the NeoForge event bus skips a {@code @SubscribeEvent} listener whose {@code receiveCanceled}
 * is false (the default) once any earlier listener has cancelled the event
 * (see {@code net.neoforged.bus.SubscribeEventListener#invoke}). If another chat mod's NORMAL-priority
 * listener runs first and cancels the event to re-broadcast the message itself, NeoEssentials'
 * own NORMAL-priority ChatHandler — and therefore its inline mute check — never executes, so a muted
 * player's message still reaches everyone. Cancelling here at HIGHEST guarantees the mute is applied
 * before any such handler gets the chance, and because we cancel the event the downstream formatter
 * is skipped and never re-broadcasts the muted message.
 */
@EventBusSubscriber(modid = "neoessentials")
public class ChatMuteGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatMuteGuard.class);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String playerName = player.getName().getString();

        if (!MuteManager.isMuted(player)) {
            return;
        }

        event.setCanceled(true);
        NeoLog.debug(LOGGER, LogCategory.CHAT, "ChatMuteGuard - blocked chat from muted player {}", playerName);

        player.sendSystemMessage(muteNotice(playerName));
    }

    /**
     * Builds the "you are muted" notice for a player: includes the remaining time when the mute is
     * temporary and the reason given to {@code /mute} when there is one.
     */
    public static Component muteNotice(String playerName) {
        long remainingMillis = MuteManager.getRemainingMillis(playerName);
        String reason = MuteManager.getReason(playerName);
        boolean hasReason = reason != null && !reason.isBlank();

        if (remainingMillis > 0) {
            String duration = BanManager.formatDuration(remainingMillis);
            return hasReason
                ? MessageUtil.error("commands.neoessentials.chat.muted_remaining_with_reason", duration, reason)
                : MessageUtil.error("commands.neoessentials.chat.muted_remaining", duration);
        }
        return hasReason
            ? MessageUtil.error("commands.neoessentials.chat.muted_with_reason", reason)
            : MessageUtil.error("commands.neoessentials.chat.muted");
    }
}
