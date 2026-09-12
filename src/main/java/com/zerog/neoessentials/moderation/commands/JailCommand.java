package com.zerog.neoessentials.moderation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.moderation.JailManager;
import com.zerog.neoessentials.moderation.BanManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.util.InputValidator;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

/**
 * Jail commands: /jail, /unjail, /setjail, /jaillist, /jailinfo
 */
public class JailCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(JailCommand.class);
    // Same leading-duration syntax as /mute: 30m, 1h, 1d, etc.
    private static final Pattern DURATION_TOKEN = Pattern.compile(
        "(?i)^\\d+(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days|w|week|weeks)$");
    
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_JAILED_PLAYERS = (ctx, builder) -> {
        JailManager jailManager = JailManager.getInstance();
        return SharedSuggestionProvider.suggest(
            jailManager.getAllJailedPlayers().stream()
                .map(jail -> jail.playerName)
                .toList(),
            builder
        );
    };
    
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_JAIL_NAMES = (ctx, builder) -> {
        JailManager jailManager = JailManager.getInstance();
        return SharedSuggestionProvider.suggest(
            jailManager.getAllJailLocations().stream()
                .map(jail -> jail.name)
                .toList(),
            builder
        );
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Enforce moderationEnabled and jailSystemEnabled config
        if (!com.zerog.neoessentials.config.ConfigManager.isModerationEnabled()
            || !com.zerog.neoessentials.moderation.JailManager.isJailSystemEnabled()) {
            return;
        }
        // /jail <player> <jail> [duration] [reason]
        // /jail <player> <jail> [reason]
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("jail")) {
        dispatcher.register(Commands.literal("jail")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.jail").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .then(Commands.argument("jail", StringArgumentType.word())
                    .suggests(SUGGEST_JAIL_NAMES)
                    .executes(ctx -> executeJail(ctx,
                            StringArgumentType.getString(ctx, "player"),
                            StringArgumentType.getString(ctx, "jail"),
                            getDefaultJailReason(), 0L))
                    .then(Commands.argument("duration_or_reason", StringArgumentType.greedyString())
                        .executes(ctx -> executeJailWithDurationOrReason(ctx,
                            StringArgumentType.getString(ctx, "player"),
                            StringArgumentType.getString(ctx, "jail"),
                            StringArgumentType.getString(ctx, "duration_or_reason")))
                    )
                )
            )
        );
        }

        // /jailfor <player> <jail> <duration> [reason]  — timed jail (Essentials: sendtemp pattern)
        // A timed jail is a /jail with a duration, so whoever may jail may also jail for a while:
        // neoessentials.moderation.jail grants it too. The dedicated .timed node stays for setups
        // that hand out only the timed form. (LuckPerms treats the two as unrelated nodes: granting
        // the parent neoessentials.moderation.jail does NOT imply the .timed child.)
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("jailfor")) {
        dispatcher.register(Commands.literal("jailfor")
            .requires(source -> PermissionValidator.validateAnyPermission(source,
                "neoessentials.moderation.jail.timed", "neoessentials.moderation.jail").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .then(Commands.argument("jail", StringArgumentType.word())
                    .suggests(SUGGEST_JAIL_NAMES)
                    .then(Commands.argument("duration", StringArgumentType.word())
                        .executes(ctx -> executeTimedJail(ctx,
                            StringArgumentType.getString(ctx, "player"),
                            StringArgumentType.getString(ctx, "jail"),
                            StringArgumentType.getString(ctx, "duration"),
                            getDefaultJailReason()))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                            .executes(ctx -> executeTimedJail(ctx,
                                StringArgumentType.getString(ctx, "player"),
                                StringArgumentType.getString(ctx, "jail"),
                                StringArgumentType.getString(ctx, "duration"),
                                StringArgumentType.getString(ctx, "reason")))
                        )
                    )
                )
            )
        );
        }

        // /unjail <player>
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("unjail")) {
        dispatcher.register(Commands.literal("unjail")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.unjail").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests(SUGGEST_JAILED_PLAYERS)
                .executes(ctx -> executeUnjail(ctx, StringArgumentType.getString(ctx, "player"))))
        );
        }

        // /setjail <name>                    — auto-detect: wand cuboid selection, else
        //                                       WorldEdit selection, else sphere at current
        //                                       position with the configured default radius
        // /setjail <name> sphere <radius>     — sphere at current position, explicit radius
        // /setjail <name> cuboid              — cuboid from the wand/WorldEdit selection
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("setjail")) {
        dispatcher.register(Commands.literal("setjail")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.setjail").hasPermission())
            .then(Commands.argument("name", StringArgumentType.word())
                .executes(ctx -> executeSetJailAuto(ctx, StringArgumentType.getString(ctx, "name")))
                .then(Commands.literal("sphere")
                    .then(Commands.argument("radius", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(1.0))
                        .executes(ctx -> executeSetJailSphere(ctx,
                            StringArgumentType.getString(ctx, "name"),
                            com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(ctx, "radius")))))
                .then(Commands.literal("cuboid")
                    .executes(ctx -> executeSetJailCuboid(ctx, StringArgumentType.getString(ctx, "name"))))
            )
        );
        }

        // /jailwand — give the jail-region selection wand (item configurable via
        // moderation.jailSettings.wandItem). Right-click = corner 1, left-click = corner 2.
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("jailwand")) {
        dispatcher.register(Commands.literal("jailwand")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.jail.wand").hasPermission())
            .executes(JailCommand::executeJailWand)
        );
        }

        // /jaillist
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("jaillist")) {
        dispatcher.register(Commands.literal("jaillist")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.jaillist").hasPermission())
            .executes(ctx -> executeJailList(ctx))
        );
        }

        // /jailinfo [jail]
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("jailinfo")) {
        dispatcher.register(Commands.literal("jailinfo")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.jailinfo").hasPermission())
            .executes(ctx -> executeJailInfo(ctx, null))
            .then(Commands.argument("jail", StringArgumentType.word())
                .suggests(SUGGEST_JAIL_NAMES)
                .executes(ctx -> executeJailInfo(ctx, StringArgumentType.getString(ctx, "jail"))))
        );
        }

        // /deljail <name>  — Essentials: Commanddeljail
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("deljail")) {
        dispatcher.register(Commands.literal("deljail")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.setjail").hasPermission())
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(SUGGEST_JAIL_NAMES)
                .executes(ctx -> executeDelJail(ctx, StringArgumentType.getString(ctx, "name"))))
        );
        }

        // /jails — alias for /jaillist (Essentials: Commandjails)
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("jails")) {
        dispatcher.register(Commands.literal("jails")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.jaillist").hasPermission())
            .executes(ctx -> executeJailList(ctx))
        );
        }

        // /togglejail <player> — toggle a player's jail state (Essentials: Commandtogglejail)
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("togglejail")) {
        dispatcher.register(Commands.literal("togglejail")
            .requires(source -> PermissionValidator.validatePermission(source, "neoessentials.moderation.jail").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), builder))
                .executes(ctx -> executeToggleJail(ctx, StringArgumentType.getString(ctx, "player"))))
        );
        }
    }

    /**
     * Parses the optional tail of /jail exactly as /mute does: a leading duration token is
     * consumed and all following text remains the reason. Without a duration, the complete
     * tail is the reason for an indefinite jail.
     */
    private static int executeJailWithDurationOrReason(CommandContext<CommandSourceStack> ctx,
                                                        String playerName, String jailName, String args) {
        String durationToken = null;
        String reason = args;
        int separator = args.indexOf(' ');
        String firstWord = separator >= 0 ? args.substring(0, separator) : args;
        if (DURATION_TOKEN.matcher(firstWord).matches()) {
            durationToken = firstWord;
            reason = separator >= 0 ? args.substring(separator + 1).trim() : "";
        }

        long durationMillis = durationToken != null ? BanManager.parseDuration(durationToken) : 0L;
        if (durationToken != null && durationMillis <= 0) {
            ctx.getSource().sendFailure(MessageUtil.error(
                "commands.neoessentials.jail.invalid_duration", durationToken));
            return 0;
        }
        if (reason.isEmpty()) {
            reason = getDefaultJailReason();
        }
        return executeJail(ctx, playerName, jailName, reason, durationMillis);
    }

    private static int executeTimedJail(CommandContext<CommandSourceStack> ctx,
                                        String playerName, String jailName,
                                        String durationToken, String reason) {
        if (!DURATION_TOKEN.matcher(durationToken).matches()) {
            ctx.getSource().sendFailure(MessageUtil.error(
                "commands.neoessentials.jail.invalid_duration", durationToken));
            return 0;
        }

        long durationMillis = BanManager.parseDuration(durationToken);
        if (durationMillis <= 0) {
            ctx.getSource().sendFailure(MessageUtil.error(
                "commands.neoessentials.jail.invalid_duration", durationToken));
            return 0;
        }
        return executeJail(ctx, playerName, jailName,
            reason == null || reason.isBlank() ? getDefaultJailReason() : reason,
            durationMillis);
    }

    private static String getDefaultJailReason() {
        var config = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("config.json");
        if (config.has("moderation")) {
            var moderation = config.getAsJsonObject("moderation");
            if (moderation.has("jailSettings")) {
                var jailSettings = moderation.getAsJsonObject("jailSettings");
                if (jailSettings.has("defaultJailReason")) {
                    return jailSettings.get("defaultJailReason").getAsString();
                }
            }
        }
        return "Jailed by an operator";
    }

    private static int executeToggleJail(CommandContext<CommandSourceStack> ctx, String playerName) {
        CommandSourceStack source = ctx.getSource();
        JailManager jailManager = JailManager.getInstance();
        MinecraftServer server = source.getServer();

        // Resolve player
        UUID playerId = null;
        String resolvedName = playerName;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getName().getString().equalsIgnoreCase(playerName)) {
                playerId = p.getUUID();
                resolvedName = p.getName().getString();
                break;
            }
        }
        if (playerId == null) {
            source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
            return 0;
        }

        boolean isJailed = jailManager.isPlayerJailed(playerId);
        if (isJailed) {
            boolean ok = jailManager.unjailPlayer(playerId);
            if (ok) {
                final String name = resolvedName;
                source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.jail.unjail_success", name), true);
                return 1;
            }
        } else {
            List<JailManager.JailLocation> locations = jailManager.getAllJailLocations();
            if (locations.isEmpty()) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.jail.no_locations"));
                return 0;
            }
            String jailName = locations.getFirst().name;
            boolean ok = jailManager.jailPlayer(resolvedName, playerId, "Toggled by staff", getCommandSender(source), jailName, 0L);
            if (ok) {
                final String name = resolvedName;
                source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.jail.jail_success", name, jailName), true);
                return 1;
            }
        }
        source.sendFailure(MessageUtil.error("commands.neoessentials.jail.toggle_failed", resolvedName));
        return 0;
    }


    private static int executeJail(CommandContext<CommandSourceStack> ctx, String playerName, String jailName, String reason, long durationMillis) {
        CommandSourceStack source = ctx.getSource();
        String jailedBy = getCommandSender(source);
        try {
            // Validate reason length and content
            InputValidator.ValidationResult reasonResult = InputValidator.validateReason(reason);
            if (!reasonResult.isValid()) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.invalid_reason", reasonResult.getErrorMessage()));
                return 0;
            }
            reason = (String) reasonResult.getValue();

            JailManager jailManager = JailManager.getInstance();
            MinecraftServer server = source.getServer();

            // Enforce requireJailLocation config: must have at least one jail location set
            boolean requireJailLocation = com.zerog.neoessentials.config.ConfigManager.isRequireJailLocationEnabled();
            if (requireJailLocation && jailManager.getAllJailLocations().isEmpty()) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.no_jail_locations"));
                return 0;
            }
            // Check if jail exists
            if (jailManager.getJailLocation(jailName) == null) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.jail_not_found", jailName));
                return 0;
            }

            // Resolve player UUID
            UUID playerId = null;
            String resolvedName = playerName;

            // Try to find online player first
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getName().getString().equalsIgnoreCase(playerName)) {
                    playerId = player.getUUID();
                    resolvedName = player.getName().getString();
                    break;
                }
            }

            // If not online, try to get from player cache
            if (playerId == null) {
                var profile = server.getProfileCache().get(playerName);
                if (profile.isPresent()) {
                    playerId = profile.get().getId();
                    resolvedName = profile.get().getName();
                }
            }

            if (playerId == null) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
                return 0;
            }

            // Jail the player
            boolean success = jailManager.jailPlayer(resolvedName, playerId, reason, jailedBy, jailName, durationMillis);

            if (success) {
                // Our fork's jail_success/jail_broadcast carry the jail name (and, for a timed
                // sentence, the duration), so the argument lists below match those richer
                // strings — upstream's single-placeholder trim doesn't apply here. What we do
                // take from upstream is the display fix: coloredText() instead of success(),
                // so the §-codes baked into the strings survive.
                String confirmMessage = durationMillis > 0
                    ? MessageUtil.localize("neoessentials.moderation.jail_success_timed",
                        resolvedName, jailName, BanManager.formatDuration(durationMillis), reason)
                    : MessageUtil.localize("neoessentials.moderation.jail_success",
                        resolvedName, jailName, reason);
                // `false` here — NOT broadcasting this personal confirmation to ops, since
                // broadcastToStaff() right below already sends every staff member a
                // near-identical message via a separate permission node. With both set to
                // broadcast, anyone who is both an op AND has
                // neoessentials.moderation.notifications saw the same thing twice.
                source.sendSuccess(() -> MessageUtil.coloredText(confirmMessage), false);

                // Broadcast jail to all online staff, excluding the sender (they already got
                // confirmMessage above) to avoid a second, slightly-differently-worded copy.
                String broadcastMessage = durationMillis > 0
                    ? MessageUtil.localize("neoessentials.moderation.jail_broadcast_timed",
                        resolvedName, jailName, jailedBy, BanManager.formatDuration(durationMillis), reason)
                    : MessageUtil.localize("neoessentials.moderation.jail_broadcast",
                        resolvedName, jailName, jailedBy, reason);
                broadcastToStaff(server, broadcastMessage, senderId(source));

                NeoLog.info(LOGGER, LogCategory.MODERATION, "Player {} jailed by {} in {} for {}. Reason: {}",
                    resolvedName, jailedBy, jailName,
                    durationMillis > 0 ? BanManager.formatDuration(durationMillis) : "permanent",
                    reason);
                return 1;
            } else {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.jail_failed", resolvedName));
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Error executing jail command", e);
            source.sendFailure(MessageUtil.error("neoessentials.moderation.jail_error"));
            return 0;
        }
    }
    
    private static int executeUnjail(CommandContext<CommandSourceStack> ctx, String playerName) {
        CommandSourceStack source = ctx.getSource();
        String unjailedBy = getCommandSender(source);
        
        try {
            JailManager jailManager = JailManager.getInstance();
            MinecraftServer server = source.getServer();
            
            // Try to find the player's UUID
            UUID playerId = null;
            String resolvedName = playerName;
            
            // First, try online players
            ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(playerName);
            if (onlinePlayer != null) {
                playerId = onlinePlayer.getUUID();
                resolvedName = onlinePlayer.getName().getString();
            } else {
                // Try player cache
                var profile = server.getProfileCache().get(playerName);
                if (profile.isPresent()) {
                    playerId = profile.get().getId();
                    resolvedName = profile.get().getName();
                }
            }

            if (playerId == null) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
                return 0;
            }

            // Check if player is actually jailed
            if (!jailManager.isPlayerJailed(playerId)) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_jailed", resolvedName));
                return 0;
            }

            // Unjail the player
            boolean success = jailManager.unjailPlayer(playerId);

            if (success) {
                String confirmMessage = MessageUtil.localize("neoessentials.moderation.unjail_success", resolvedName, unjailedBy);
                // See the matching comment in executeJail — `false` avoids double-showing this
                // to anyone who is both an op and has neoessentials.moderation.notifications.
                source.sendSuccess(() -> MessageUtil.coloredText(confirmMessage), false);

                // Broadcast unjail to all online staff (excluding the sender, already notified above)
                broadcastToStaff(server, MessageUtil.localize("neoessentials.moderation.unjail_broadcast",
                    resolvedName, unjailedBy), senderId(source));

                NeoLog.info(LOGGER, LogCategory.MODERATION, "Player {} unjailed by {}", resolvedName, unjailedBy);
                return 1;
            } else {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.unjail_failed", resolvedName));
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Error executing unjail command", e);
            source.sendFailure(MessageUtil.error("neoessentials.moderation.unjail_error"));
            return 0;
        }
    }
    
    /**
     * {@code /setjail <name>} — auto-detects the source: a completed NeoEssentials wand
     * selection takes priority, then a WorldEdit cuboid selection (best-effort — see
     * {@link com.zerog.neoessentials.moderation.WorldEditIntegration}), and only falls back to
     * a sphere at the player's current position (configured default radius) if neither is
     * present, preserving the exact old point-only behavior for anyone not using the wand.
     */
    private static int executeSetJailAuto(CommandContext<CommandSourceStack> ctx, String jailName) {
        CommandSourceStack source = ctx.getSource();
        try {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_only_command"));
                return 0;
            }

            JailManager jailManager = JailManager.getInstance();
            String createdBy = player.getName().getString();
            var selectionManager = com.zerog.neoessentials.moderation.JailSelectionManager.getInstance();

            if (selectionManager.hasFullSelection(player.getUUID())) {
                BlockPos pos1 = selectionManager.getPos1(player.getUUID());
                BlockPos pos2 = selectionManager.getPos2(player.getUUID());
                String dimension = selectionManager.getDimension(player.getUUID());
                boolean success = jailManager.setJailLocationCuboid(jailName, pos1, pos2, dimension, createdBy);
                selectionManager.clear(player.getUUID());
                return reportSetJailResult(source, jailName, success);
            }

            var weSelection = com.zerog.neoessentials.moderation.WorldEditIntegration.getSelection(player);
            if (weSelection != null) {
                String dimension = player.level().dimension().location().toString();
                boolean success = jailManager.setJailLocationCuboid(jailName, weSelection.min(), weSelection.max(), dimension, createdBy);
                return reportSetJailResult(source, jailName, success);
            }

            BlockPos position = player.blockPosition();
            String dimension = player.level().dimension().location().toString();
            double defaultRadius = com.zerog.neoessentials.config.ConfigManager.getDefaultJailSphereRadius();
            boolean success = jailManager.setJailLocationSphere(jailName, position, defaultRadius, dimension, createdBy);
            return reportSetJailResult(source, jailName, success);
        } catch (Exception e) {
            LOGGER.error("Error executing setjail command", e);
            source.sendFailure(MessageUtil.error("neoessentials.moderation.setjail_error"));
            return 0;
        }
    }

    /** {@code /setjail <name> sphere <radius>} — sphere at the player's current position. */
    private static int executeSetJailSphere(CommandContext<CommandSourceStack> ctx, String jailName, double radius) {
        CommandSourceStack source = ctx.getSource();
        try {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_only_command"));
                return 0;
            }
            BlockPos position = player.blockPosition();
            String dimension = player.level().dimension().location().toString();
            boolean success = JailManager.getInstance().setJailLocationSphere(
                jailName, position, radius, dimension, player.getName().getString());
            return reportSetJailResult(source, jailName, success);
        } catch (Exception e) {
            LOGGER.error("Error executing setjail sphere command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the setjail command."));
            return 0;
        }
    }

    /** {@code /setjail <name> cuboid} — cuboid from the NeoEssentials wand or WorldEdit selection. */
    private static int executeSetJailCuboid(CommandContext<CommandSourceStack> ctx, String jailName) {
        CommandSourceStack source = ctx.getSource();
        try {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_only_command"));
                return 0;
            }

            var selectionManager = com.zerog.neoessentials.moderation.JailSelectionManager.getInstance();
            if (selectionManager.hasFullSelection(player.getUUID())) {
                BlockPos pos1 = selectionManager.getPos1(player.getUUID());
                BlockPos pos2 = selectionManager.getPos2(player.getUUID());
                String dimension = selectionManager.getDimension(player.getUUID());
                boolean success = JailManager.getInstance().setJailLocationCuboid(
                    jailName, pos1, pos2, dimension, player.getName().getString());
                selectionManager.clear(player.getUUID());
                return reportSetJailResult(source, jailName, success);
            }

            var weSelection = com.zerog.neoessentials.moderation.WorldEditIntegration.getSelection(player);
            if (weSelection != null) {
                String dimension = player.level().dimension().location().toString();
                boolean success = JailManager.getInstance().setJailLocationCuboid(
                    jailName, weSelection.min(), weSelection.max(), dimension, player.getName().getString());
                return reportSetJailResult(source, jailName, success);
            }

            source.sendFailure(MessageUtil.error("commands.neoessentials.jail.wand.no_selection"));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error executing setjail cuboid command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the setjail command."));
            return 0;
        }
    }

    private static int reportSetJailResult(CommandSourceStack source, String jailName, boolean success) {
        if (success) {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.jail.setjail_created", jailName), true);
            return 1;
        } else {
            source.sendFailure(MessageUtil.error("neoessentials.moderation.setjail_failed", jailName));
            return 0;
        }
    }

    /** {@code /jailwand} — gives the player the configured jail-region selection wand item. */
    private static int executeJailWand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        try {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.player_only_command"));
                return 0;
            }
            String wandItemId = com.zerog.neoessentials.config.ConfigManager.getJailWandItem();
            var itemId = com.zerog.neoessentials.util.ResourceLocationHelper.parse(wandItemId);
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            if (item == null) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.jail.wand.invalid_item", wandItemId));
                return 0;
            }
            net.minecraft.world.item.ItemStack wandStack = new net.minecraft.world.item.ItemStack(item);
            if (!player.getInventory().add(wandStack)) {
                player.drop(wandStack, false);
            }
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.jail.wand.given"), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing jailwand command", e);
            source.sendFailure(MessageUtil.error("An error occurred while executing the jailwand command."));
            return 0;
        }
    }
    
    private static int executeJailList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            JailManager jailManager = JailManager.getInstance();
            var jailedPlayers = jailManager.getAllJailedPlayers();
            
            if (jailedPlayers.isEmpty()) {
                String message = MessageUtil.localize("neoessentials.moderation.jaillist_empty");
                source.sendSuccess(() -> MessageUtil.coloredText(message), false);
                return 1;
            }
            
            String header = MessageUtil.localize("neoessentials.moderation.jaillist_header", jailedPlayers.size());
            source.sendSuccess(() -> MessageUtil.coloredText(header), false);
            
            for (JailManager.JailEntry jail : jailedPlayers) {
                // reason/jailedBy were swapped relative to the template ("jailed by {2}") —
                // staff saw the jail REASON where the jailing staff member's name belonged.
                String jailInfo = MessageUtil.localize("neoessentials.moderation.jaillist_entry",
                    jail.playerName, jail.jailName, jail.jailedBy, jail.reason, jail.getFormattedJailTime());
                source.sendSuccess(() -> MessageUtil.coloredText(jailInfo), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error executing jaillist command", e);
            source.sendFailure(MessageUtil.error("neoessentials.moderation.jaillist_error"));
            return 0;
        }
    }
    
    private static int executeJailInfo(CommandContext<CommandSourceStack> ctx, String jailName) {
        CommandSourceStack source = ctx.getSource();
        
        try {
            JailManager jailManager = JailManager.getInstance();
            
            if (jailName == null) {
                // Show all jail locations
                var jailLocations = jailManager.getAllJailLocations();
                
                if (jailLocations.isEmpty()) {
                    String message = MessageUtil.localize("neoessentials.moderation.jailinfo_no_jails");
                    source.sendSuccess(() -> MessageUtil.coloredText(message), false);
                    return 1;
                }
                
                String message = MessageUtil.localize("neoessentials.moderation.jailinfo_all_header");
                source.sendSuccess(() -> MessageUtil.coloredText(message), false);
                
                for (JailManager.JailLocation jail : jailLocations) {
                    String locationInfo = MessageUtil.localize("neoessentials.moderation.jailinfo_location",
                        jail.name, jail.position.getX(), jail.position.getY(), jail.position.getZ(), 
                        jail.dimension, jail.createdBy, jail.getFormattedCreatedTime());
                    source.sendSuccess(() -> MessageUtil.coloredText(locationInfo), false);
                }
                
                String countInfo = MessageUtil.localize("neoessentials.moderation.jailinfo_count", jailLocations.size());
                source.sendSuccess(() -> MessageUtil.coloredText(countInfo), false);
                
            } else {
                // Show specific jail info
                JailManager.JailLocation jail = jailManager.getJailLocation(jailName);
                
                if (jail == null) {
                    source.sendFailure(MessageUtil.error("neoessentials.moderation.jail_not_found", jailName));
                    return 0;
                }
                
                String locationInfo = MessageUtil.localize("neoessentials.moderation.jailinfo_specific",
                    jail.name, jail.position.getX(), jail.position.getY(), jail.position.getZ(), 
                    jail.dimension, jail.createdBy, jail.getFormattedCreatedTime());
                source.sendSuccess(() -> MessageUtil.coloredText(locationInfo), false);
                
                // Show how many players are in this jail
                long playersInJail = jailManager.getAllJailedPlayers().stream()
                    .filter(j -> j.jailName.equals(jailName))
                    .count();
                
                if (playersInJail > 0) {
                    String playerInfo = MessageUtil.localize("neoessentials.moderation.jailinfo_players", playersInJail);
                    source.sendSuccess(() -> MessageUtil.coloredText(playerInfo), false);
                }
            }
            
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error executing jailinfo command", e);
            source.sendFailure(MessageUtil.error("neoessentials.moderation.jailinfo_error"));
            return 0;
        }
    }
    
    private static void broadcastToStaff(MinecraftServer server, String message) {
        broadcastToStaff(server, message, null);
    }

    /**
     * @param excludeId skipped if non-null — used so the command sender, who already got their
     *                  own personal confirmation message, doesn't also get this near-duplicate
     *                  staff-wide broadcast just because they also qualify for it.
     */
    private static void broadcastToStaff(MinecraftServer server, String message, UUID excludeId) {
        // `message` here is already fully-localized, resolved text (callers pass the result of
        // MessageUtil.localize(key, args...)) — coloredText() applies its embedded §-codes
        // without re-running it through localize() as if it were a translation key itself.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (excludeId != null && player.getUUID().equals(excludeId)) continue;
            if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                    player.getUUID(), "neoessentials.moderation.notifications")) {
                player.sendSystemMessage(MessageUtil.coloredText(message));
            }
        }
    }

    /** The command sender's player UUID, or {@code null} if run from console/command block. */
    private static UUID senderId(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player ? player.getUUID() : null;
    }
    
    private static int executeDelJail(CommandContext<CommandSourceStack> ctx, String jailName) {
        CommandSourceStack source = ctx.getSource();
        try {
            JailManager jailManager = JailManager.getInstance();
            if (jailManager.getJailLocation(jailName) == null) {
                source.sendFailure(MessageUtil.error("neoessentials.moderation.jail_not_found", jailName));
                return 0;
            }
            // Check if any players are currently in this jail
            long inmates = jailManager.getAllJailedPlayers().stream()
                .filter(j -> j.jailName.equals(jailName)).count();
            jailManager.removeJailLocation(jailName);
            String msg = MessageUtil.localize("commands.neoessentials.jail.deljail_success", jailName);
            source.sendSuccess(() -> MessageUtil.coloredText(msg), true);
            if (inmates > 0) {
                String warn = MessageUtil.localize("commands.neoessentials.jail.deljail_had_inmates", inmates);
                source.sendSuccess(() -> MessageUtil.coloredText(warn), false);
            }
            NeoLog.info(LOGGER, LogCategory.MODERATION, "Jail location '{}' deleted by {}", jailName, getCommandSender(source));
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing deljail command", e);
            source.sendFailure(MessageUtil.error("neoessentials.moderation.deljail_error"));
            return 0;
        }
    }

    private static String getCommandSender(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getName().getString();
        }
        return "Console";
    }
    
    private static UUID getPlayerUUID(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getUUID();
        }
        return null; // Console
    }
}
