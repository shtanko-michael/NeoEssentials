
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.moderation.BanManager;

import java.util.regex.Pattern;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the /mute command for muting a player.
 */
public class MuteCommand {
    // Recognizes a leading duration token ("1h", "30m", "1d", "2w", ...) so it can be told
    // apart from the start of a plain-text reason (e.g. "spamming").
    private static final Pattern DURATION_TOKEN = Pattern.compile(
        "(?i)^\\d+(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days|w|week|weeks)$");

    private static final Logger LOGGER = LoggerFactory.getLogger(MuteCommand.class);
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerMuteCommand(dispatcher, "mute");
        registerMuteCommand(dispatcher, "silence");
    }

    private static void registerMuteCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(Commands.literal(commandName)
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> executeMute(ctx, ""))
                .then(Commands.argument("duration_or_reason", StringArgumentType.greedyString())
                    .executes(ctx -> executeMute(ctx, StringArgumentType.getString(ctx, "duration_or_reason")))
                )
            )
        );
    }

    private static int executeMute(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String args) {
        CommandSourceStack source = ctx.getSource();

        // Check if chat module is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.isChatEnabled()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mute.disabled"));
            return 0;
        }

        // Check if individual mute command is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("mute")) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mute.disabled"));
            return 0;
        }

        net.minecraft.server.level.ServerPlayer targetPlayer;
        try {
            targetPlayer = EntityArgument.getPlayer(ctx, "target");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mute.player_not_found"));
            return 0;
        }
        String targetName = targetPlayer.getName().getString();

        // Sender may be console/RCON (source.getPlayer() == null) — every other moderation
        // command (ban, kick, freeze, jail, warn, ...) already allows that, so mute/unmute/
        // mutelist should too rather than rejecting with a "server context" error that doesn't
        // even describe the real condition being checked.
        net.minecraft.server.level.ServerPlayer sender = source.getPlayer();
        String senderName = sender != null ? sender.getName().getString() : "Console";

        // Check if command is enabled
        ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
        if (chatManager != null && !chatManager.isMuteEnabled()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mute.disabled"));
            return 0;
        }

        // Check permissions (console always passes, same as every other moderation command)
        if (sender != null && !com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.mute")) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }

        // Check if trying to mute self
        if (sender != null && senderName.equalsIgnoreCase(targetName)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mute.self"));
            return 0;
        }

        // Check if target has the exempt permission. Uses the ops-bypass-free check: exempt is a
        // protection flag on the TARGET, not an authority granted to the acting player, so an
        // operator target must not become automatically immune just because ops bypass permission
        // checks in general (see PermissionAPI#hasPermissionExplicit).
        if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermissionExplicit(targetPlayer.getUUID(), "neoessentials.chat.mute.exempt")) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mute.exempt", targetName));
            return 0;
        }

        // Split a leading duration token ("1h", "30m", "1d", ...) off the trailing text; whatever
        // remains (or the whole string, if it isn't a duration) is the reason. This keeps
        // "/mute <target> <reason...>" working exactly as before when no duration is given.
        String durationToken = null;
        String reason = args;
        if (!args.isEmpty()) {
            int sp = args.indexOf(' ');
            String firstWord = sp >= 0 ? args.substring(0, sp) : args;
            if (DURATION_TOKEN.matcher(firstWord).matches()) {
                durationToken = firstWord;
                reason = sp >= 0 ? args.substring(sp + 1).trim() : "";
            }
        }
        long durationMillis = durationToken != null ? BanManager.parseDuration(durationToken) : 0L;
        final String finalReason = reason;

        com.zerog.neoessentials.chat.MuteManager.mute(
            targetName, finalReason.isEmpty() ? null : finalReason, senderName, durationMillis);
        // The mute state is otherwise only noticed when the player next tries to chat. Notify
        // the online target immediately with the same information the moderator supplied.
        String notificationDuration = durationMillis > 0
            ? BanManager.formatDuration(durationMillis)
            : MessageUtil.localize("commands.neoessentials.mute.duration_permanent");
        String notificationReason = finalReason.isEmpty()
            ? MessageUtil.localize("commands.neoessentials.mute.reason_unspecified")
            : finalReason;
        targetPlayer.sendSystemMessage(MessageUtil.warning(
            "commands.neoessentials.mute.target_notification",
            senderName, notificationDuration, notificationReason));

        // Notify Discord integrations (fold duration into the relayed reason text since the
        // integration API has no separate duration field)
        try {
            String discordReason = finalReason.isEmpty() ? "No reason given" : finalReason;
            if (durationMillis > 0) {
                discordReason = "[" + BanManager.formatDuration(durationMillis) + "] " + discordReason;
            }
            com.zerog.neoessentials.integrations.ChatIntegrationManager.broadcastMuteEvent(targetPlayer, discordReason, true);
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.CHAT, "Failed to broadcast mute event to integrations for " + targetName, e);
        }

        if (durationMillis > 0) {
            String formattedDuration = BanManager.formatDuration(durationMillis);
            if (finalReason.isEmpty()) {
                source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.mute.success_temp", targetName, formattedDuration), false);
            } else {
                source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.mute.success_temp_with_reason", targetName, formattedDuration, finalReason), false);
            }
        } else if (finalReason.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.mute.success", targetName), false);
        } else {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.mute.success_with_reason", targetName, finalReason), false);
        }
        return 1;
    }
}
