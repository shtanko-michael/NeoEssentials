
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

/**
 * Handles the /mute command for muting a player.
 */
public class MuteCommand {
    // Recognizes a leading duration token ("1h", "30m", "1d", "2w", ...) so it can be told
    // apart from the start of a plain-text reason (e.g. "spamming").
    private static final Pattern DURATION_TOKEN = Pattern.compile(
        "(?i)^\\d+(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days|w|week|weeks)$");

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

        // Validate sender
        net.minecraft.server.level.ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            source.sendFailure(MessageUtil.error("neoessentials.error.no_server"));
            return 0;
        }

        // Check if command is enabled
        ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
        if (chatManager != null && !chatManager.isMuteEnabled()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.mute.disabled"));
            return 0;
        }

        // Check permissions
        if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.mute")) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }

        // Check if trying to mute self
        if (sender.getName().getString().equalsIgnoreCase(targetName)) {
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

        com.zerog.neoessentials.chat.MuteManager.mute(sender, targetName, durationMillis, finalReason);
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
            sender.getName().getString(), notificationDuration, notificationReason));

        // Notify Discord integrations (fold duration into the relayed reason text since the
        // integration API has no separate duration field)
        try {
            String discordReason = reason.isEmpty() ? "No reason given" : reason;
            if (durationMillis > 0) {
                discordReason = "[" + BanManager.formatDuration(durationMillis) + "] " + discordReason;
            }
            com.zerog.neoessentials.integrations.ChatIntegrationManager.broadcastMuteEvent(targetPlayer, discordReason, true);
        } catch (Exception ignored) {}

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
