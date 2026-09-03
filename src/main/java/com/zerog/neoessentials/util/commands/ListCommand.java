package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.neoforged.fml.ModList;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements the /list command - Shows online players with advanced formatting
 * Includes LuckPerms group integration, AFK status, vanish status, and interactive elements
 */
public class ListCommand {
    
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ListCommand.class);

    /**
     * Built-in group -> color defaults, kept in sync with the chat prefixes
     * (&5 Ender, &3 Wither, &6 Warden, &e Helper, &9 Moder). Used only when the
     * group has no chat prefix to read the color from; config can override.
     */
    private static final Map<String, ChatFormatting> DEFAULT_GROUP_COLORS = Map.of(
        "ender", ChatFormatting.DARK_PURPLE,
        "wither", ChatFormatting.DARK_AQUA,
        "warden", ChatFormatting.GOLD,
        "helper", ChatFormatting.YELLOW,
        "moder", ChatFormatting.BLUE,
        "default", ChatFormatting.GRAY
    );

    /** Color for any group not in the map (admins/service/unknown). */
    private static final ChatFormatting DEFAULT_UNKNOWN_COLOR = ChatFormatting.RED;

    /**
     * Register the /list command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        boolean enabled = ConfigManager.getInstance().isCommandEnabled("list");
        if (!enabled) {
            LOGGER.debug("Skipped registering 'list' and 'who' commands (disabled in config)");
            return;
        }

        dispatcher.register(
            Commands.literal("list")
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult =
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.list");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    return showOnlinePlayersList(ctx.getSource(), permResult.hasPermission() ? permResult.getPlayer() : null);
                })
        );

        dispatcher.register(
            Commands.literal("who")
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult =
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.list");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    return showOnlinePlayersList(ctx.getSource(), permResult.hasPermission() ? permResult.getPlayer() : null);
                })
        );

        // Register /online alias
        dispatcher.register(
            Commands.literal("online")
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult =
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.list");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    return showOnlinePlayersList(ctx.getSource(), permResult.hasPermission() ? permResult.getPlayer() : null);
                })
        );

        LOGGER.debug("Registered 'list', 'who', and 'online' commands");
    }

    /**
     * Show the online players list, grouped by LuckPerms group with per-group colors.
     * @return 1 (success) - Minecraft command convention requires returning 1 for successful execution
     */
    @SuppressWarnings("SameReturnValue")
    private static int showOnlinePlayersList(CommandSourceStack source, ServerPlayer viewer) {
        PlayerList playerList = source.getServer().getPlayerList();
        List<ServerPlayer> onlinePlayers = new ArrayList<>(playerList.getPlayers());

        // Check if viewer can see vanished players
        boolean canSeeVanished = viewer != null &&
            PermissionValidator.validatePermission(viewer.createCommandSourceStack(), "neoessentials.vanish.see").hasPermission();

        // Filter out vanished players if viewer can't see them
        if (!canSeeVanished) {
            onlinePlayers = onlinePlayers.stream()
                .filter(player -> !isVanished(player))
                .collect(Collectors.toList());
        }

        if (onlinePlayers.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.list.no_players"), false);
            return 1;
        }

        // Check if LuckPerms is available
        boolean useLuckPerms = ModList.get().isLoaded("luckperms") && isLuckPermsAvailable();

        if (useLuckPerms) {
            displayLuckPermsGroupedList(source, onlinePlayers);
        } else {
            displaySimpleGroupedList(source, onlinePlayers);
        }

        return 1;
    }

    /**
     * Display players grouped by LuckPerms primary group (sorted by weight desc), one line per group.
     */
    private static void displayLuckPermsGroupedList(CommandSourceStack source, List<ServerPlayer> players) {
        try {
            net.luckperms.api.LuckPerms luckPerms = net.luckperms.api.LuckPermsProvider.get();

            Map<String, GroupInfo> groupedPlayers = new LinkedHashMap<>();

            for (ServerPlayer player : players) {
                try {
                    net.luckperms.api.model.user.User lpUser = luckPerms.getUserManager().getUser(player.getUUID());
                    if (lpUser == null) {
                        groupedPlayers.computeIfAbsent("default", k -> new GroupInfo(0, defaultGroupLabel(), null)).players.add(player);
                        continue;
                    }

                    String primaryGroup = lpUser.getPrimaryGroup();
                    net.luckperms.api.model.group.Group lpGroup = luckPerms.getGroupManager().getGroup(primaryGroup);
                    int weight = (lpGroup != null) ? lpGroup.getWeight().orElse(0) : 0;
                    String label = computeGroupLabel(primaryGroup, lpGroup);
                    ChatFormatting prefixColor = groupPrefixColor(lpGroup);
                    groupedPlayers.computeIfAbsent(primaryGroup, k -> new GroupInfo(weight, label, prefixColor)).players.add(player);
                } catch (Exception e) {
                    LOGGER.warn("Error getting LuckPerms group for player {}: {}", player.getName().getString(), e.getMessage());
                    groupedPlayers.computeIfAbsent("default", k -> new GroupInfo(0, defaultGroupLabel(), null)).players.add(player);
                }
            }

            // Sort groups by weight (highest first); the "default" (regular players)
            // group always sorts last regardless of its configured weight.
            List<Map.Entry<String, GroupInfo>> sortedGroups = groupedPlayers.entrySet().stream()
                .sorted((a, b) -> Integer.compare(sortWeight(b.getKey(), b.getValue()), sortWeight(a.getKey(), a.getValue())))
                .toList();

            for (Map.Entry<String, GroupInfo> entry : sortedGroups) {
                source.sendSuccess(() -> renderGroupLine(entry.getKey(), entry.getValue()), false);
            }
        } catch (Exception e) {
            LOGGER.error("Error displaying LuckPerms grouped list: {}", e.getMessage(), e);
            displaySimpleGroupedList(source, players);
        }
    }

    /**
     * Fallback (LuckPerms not available): one gray "Players: ..." line.
     */
    private static void displaySimpleGroupedList(CommandSourceStack source, List<ServerPlayer> players) {
        List<ServerPlayer> sorted = new ArrayList<>(players);
        sorted.sort(Comparator.comparing(p -> p.getName().getString().toLowerCase(Locale.ROOT)));

        MutableComponent line = Component.literal(defaultGroupLabel() + ": ").withStyle(resolveGroupColor("default", null));
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
            line.append(Component.literal(sorted.get(i).getName().getString()).withStyle(ChatFormatting.WHITE));
        }
        source.sendSuccess(() -> line, false);
    }

    /**
     * Holds a group's weight, display label, the color of its chat prefix (null when the
     * group has none), and its players.
     */
    private static class GroupInfo {
        final int weight;
        final String label;
        final ChatFormatting prefixColor;
        final List<ServerPlayer> players = new ArrayList<>();

        GroupInfo(int weight, String label, ChatFormatting prefixColor) {
            this.weight = weight;
            this.label = label;
            this.prefixColor = prefixColor;
        }
    }

    /**
     * Render one group's line: "<colored label>: <white, gray-comma-separated names>".
     * Sorts the group's players alphabetically as a side effect.
     */
    private static Component renderGroupLine(String groupId, GroupInfo info) {
        List<ServerPlayer> groupPlayers = info.players;
        groupPlayers.sort(Comparator.comparing(p -> p.getName().getString().toLowerCase(Locale.ROOT)));

        ChatFormatting color = resolveGroupColor(groupId, info.prefixColor);
        MutableComponent line = Component.literal(info.label + ": ").withStyle(color);
        for (int i = 0; i < groupPlayers.size(); i++) {
            if (i > 0) {
                line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
            line.append(Component.literal(groupPlayers.get(i).getName().getString()).withStyle(ChatFormatting.WHITE));
        }
        return line;
    }

    /**
     * Sort key for group ordering: the "default" (regular players) group always
     * sorts last; every other group sorts by its LuckPerms weight (highest first).
     */
    private static int sortWeight(String groupId, GroupInfo info) {
        if ("default".equalsIgnoreCase(groupId)) {
            return Integer.MIN_VALUE;
        }
        return info.weight;
    }

    /**
     * Resolve a group's label color: config override -> the group's own chat-prefix color
     * (so /list matches the prefixes players see in chat) -> built-in default -> unknown color.
     */
    private static ChatFormatting resolveGroupColor(String groupId, ChatFormatting prefixColor) {
        String id = (groupId == null) ? "" : groupId.toLowerCase(Locale.ROOT);

        String override = ConfigManager.getListGroupColorName(id);
        if (override != null) {
            ChatFormatting c = ChatFormatting.getByName(override);
            if (c != null && c.isColor()) return c;
        }

        if (prefixColor != null) return prefixColor;

        ChatFormatting builtin = DEFAULT_GROUP_COLORS.get(id);
        if (builtin != null) return builtin;

        String unknownName = ConfigManager.getListUnknownGroupColor();
        ChatFormatting unknown = ChatFormatting.getByName(unknownName);
        return (unknown != null && unknown.isColor()) ? unknown : DEFAULT_UNKNOWN_COLOR;
    }

    /**
     * The color of a LuckPerms group's chat prefix (the same meta the chat formatter renders),
     * so a group renamed or recolored in LuckPerms needs no change here. Returns null when the
     * group has no prefix or its prefix carries no color code.
     */
    private static ChatFormatting groupPrefixColor(net.luckperms.api.model.group.Group lpGroup) {
        if (lpGroup == null) {
            return null;
        }
        try {
            String prefix = lpGroup.getCachedData()
                .getMetaData(net.luckperms.api.query.QueryOptions.defaultContextualOptions())
                .getPrefix();
            return firstColorCode(prefix);
        } catch (Exception e) {
            LOGGER.debug("Could not read prefix for LuckPerms group {}: {}", lpGroup.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * First color code of a formatting string, written either as '&amp;3' or '§3'.
     * Style-only codes (bold, italic, reset, ...) are skipped.
     */
    private static ChatFormatting firstColorCode(String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            return null;
        }
        for (int i = 0; i < formatted.length() - 1; i++) {
            char marker = formatted.charAt(i);
            if (marker != '&' && marker != '§') {
                continue;
            }
            ChatFormatting c = ChatFormatting.getByCode(Character.toLowerCase(formatted.charAt(i + 1)));
            if (c != null && c.isColor()) {
                return c;
            }
        }
        return null;
    }

    /**
     * Compute a group's display label: localized "Players" for the default group,
     * the LuckPerms friendly name if one is set, otherwise the capitalized group id.
     */
    private static String computeGroupLabel(String groupId, net.luckperms.api.model.group.Group lpGroup) {
        if ("default".equalsIgnoreCase(groupId)) {
            return defaultGroupLabel();
        }
        String friendly = (lpGroup != null) ? lpGroup.getFriendlyName() : groupId;
        if (friendly == null || friendly.isBlank() || friendly.equalsIgnoreCase(groupId)) {
            return capitalize(groupId);
        }
        return stripFormattingCodes(friendly);
    }

    private static String defaultGroupLabel() {
        return MessageUtil.localize("commands.neoessentials.list.group_label_default");
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return (s == null) ? "" : s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String stripFormattingCodes(String s) {
        return (s == null) ? "" : s.replaceAll("(?i)§[0-9A-FK-OR]", "").trim();
    }

    /**
     * Check if LuckPerms API is available
     */
    private static boolean isLuckPermsAvailable() {
        try {
            net.luckperms.api.LuckPermsProvider.get();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a player is vanished
     */
    private static boolean isVanished(ServerPlayer player) {
        if (!ConfigManager.getInstance().isVanishSystemEnabled()) {
            return false;
        }

        try {
            com.zerog.neoessentials.moderation.VanishManager vanishManager =
                com.zerog.neoessentials.moderation.VanishManager.getInstance();
            return vanishManager.isPlayerVanished(player.getUUID());
        } catch (Exception e) {
            return false;
        }
    }
}
