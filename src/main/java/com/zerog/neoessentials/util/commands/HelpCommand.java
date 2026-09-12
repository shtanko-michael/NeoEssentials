package com.zerog.neoessentials.util.commands;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.commands.CommandPermissionRegistry;
import com.zerog.neoessentials.commands.CommandRegistry;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.lang.reflect.Field;
import java.util.*;

/**
 * /help [page|command] — Paginated help system ported from EssentialsX Commandhelp.
 *
 * Displays all commands the player has permission to use, paginated.
 * /help <command> shows detailed info about a specific command.
 */
public class HelpCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(HelpCommand.class);
    private static final int CMDS_PER_PAGE = 10;
    private static final String PERMISSION = "neoessentials.help";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("help")) return;

        // Vanilla's /help node shadows pagination (/help 2) in this Brigadier build.
        // We remove the existing root nodes via reflection before registering ours.
        removeRootLiteral(dispatcher, "help");
        removeRootLiteral(dispatcher, "?");

        // NOTE: Vanilla Minecraft registers /help <command:string> before any mod.
        // Brigadier matches children in insertion order, so a separate int-argument branch
        // would never be reached (the vanilla string branch grabs the number first).
        // Solution: use a single optional string argument and detect page numbers inside.
        dispatcher.register(Commands.literal("help")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), PERMISSION);
            })
            // /help
            .executes(ctx -> executeHelp(ctx, null, 1))
            // /help <page_or_command>  — handles both "/help 2" and "/help warp"
            .then(Commands.argument("page_or_command", StringArgumentType.word())
                .executes(ctx -> {
                    String arg = StringArgumentType.getString(ctx, "page_or_command");
                    try {
                        int pageNum = Integer.parseInt(arg);
                        if (pageNum >= 1) return executeHelp(ctx, null, pageNum);
                    } catch (NumberFormatException e) {
                        NeoLog.debug(LOGGER, com.zerog.neoessentials.logging.LogCategory.COMMANDS,
                            "'{}' isn't a page number, treating as a command/topic search", arg, e);
                    }
                    return executeHelp(ctx, arg, 1);
                })
                // /help <command> <page>
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeHelp(ctx,
                        StringArgumentType.getString(ctx, "page_or_command"),
                        IntegerArgumentType.getInteger(ctx, "page")))
                )
            )
        );
        // /? alias
        dispatcher.register(Commands.literal("?")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), PERMISSION);
            })
            .executes(ctx -> executeHelp(ctx, null, 1))
            .then(Commands.argument("page_or_command", StringArgumentType.word())
                .executes(ctx -> {
                    String arg = StringArgumentType.getString(ctx, "page_or_command");
                    try {
                        int pageNum = Integer.parseInt(arg);
                        if (pageNum >= 1) return executeHelp(ctx, null, pageNum);
                    } catch (NumberFormatException e) {
                        NeoLog.debug(LOGGER, com.zerog.neoessentials.logging.LogCategory.COMMANDS,
                            "'{}' isn't a page number, treating as a command/topic search", arg, e);
                    }
                    return executeHelp(ctx, arg, 1);
                })
            )
        );
    }

    /**
     * Dispatch /help &lt;target&gt; [page]: a purely-numeric target is page navigation
     * (/help 2 -> page 2); anything else is a command-name lookup (/help warp).
     * Using one word argument avoids Brigadier treating "/help 2" as a command search.
     */
    private static int executeHelpTarget(CommandContext<CommandSourceStack> ctx, String target, int page) {
        if (target != null && target.matches("\\d+")) {
            try {
                return executeHelp(ctx, null, Math.max(1, Integer.parseInt(target)));
            } catch (NumberFormatException ignored) {
                // huge number -> fall through to command lookup
            }
        }
        return executeHelp(ctx, target, page);
    }

    private static int executeHelp(CommandContext<CommandSourceStack> ctx, String search, int page) {
        var src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        UUID uuid = player != null ? player.getUUID() : null;

        // Get all registered commands
        CommandRegistry registry = CommandRegistry.getInstance();
        List<CommandRegistry.CommandInfo> allCommands = registry.getAllCommandsSorted();

        // Build the list of commands visible to this player. Visibility is resolved from
        // the bundled command->permission reference (CommandPermissionRegistry): each command
        // is shown only if the player holds its real permission node. This replaces the old
        // fabricated "neoessentials.<name>" check, which hid every command using a categorized
        // node (neoessentials.<category>.<command>). Display-only: execution is still gated by
        // each command's own permission check.
        CommandPermissionRegistry perms = CommandPermissionRegistry.getInstance();
        List<CommandRegistry.CommandInfo> accessible = allCommands.stream()
            .filter(cmd -> {
                // Console sees everything.
                if (uuid == null) return true;
                // Admins / wildcard holders see the full list regardless of per-command nodes.
                if (PermissionAPI.hasPermission(uuid, "neoessentials.admin")) return true;
                if (PermissionAPI.hasPermission(uuid, "neoessentials.*")) return true;
                // Safety net: if the reference failed to load, do not blank the help list —
                // fall back to showing every command.
                if (!perms.isReady()) return true;

                String node = perms.permissionFor(cmd.getName());
                if (node != null) {
                    return PermissionAPI.hasPermission(uuid, node);
                }
                // node == null covers two cases:
                //   1. The command IS in the reference with "permission": null — open,
                //      config-driven, or op-only (e.g. chat channels, /permissions root).
                //      Project decision: do not show these to regular players.
                //   2. The command is NOT in the reference at all — a phantom registry entry
                //      (see _phantom_registry_entries in command_permissions.json) OR any
                //      command added upstream after our reference snapshot. Hiding case 2
                //      outright would silently drop every newly synced upstream command from
                //      /help, so fall back to the registry's own node resolution there.
                if (!perms.isKnown(cmd.getName())) {
                    NeoLog.debug(LOGGER, LogCategory.COMMANDS,
                        "/help: '{}' has no entry in command_permissions.json — falling back to the registry's own permission node",
                        cmd.getName());
                    return PermissionAPI.hasPermission(uuid, resolvePermissionNode(cmd));
                }
                return false;
            })
            .sorted(Comparator.comparing(CommandRegistry.CommandInfo::getName))
            .toList();

        // If searching for a specific command
        if (search != null && !search.isEmpty()) {
            final String query = search.toLowerCase();
            // Try exact match first
            Optional<CommandRegistry.CommandInfo> exact = accessible.stream()
                .filter(c -> c.getName().equalsIgnoreCase(query))
                .findFirst();
            if (exact.isPresent()) {
                showCommandDetail(src, exact.get());
                return 1;
            }
            // Filter by search term
            accessible = accessible.stream()
                .filter(c -> c.getName().toLowerCase().contains(query)
                    || (c.getDescription() != null && c.getDescription().toLowerCase().contains(query)))
                .toList();
            if (accessible.isEmpty()) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.help.not_found", search));
                return 0;
            }
        }

        int totalPages = Math.max(1, (int) Math.ceil(accessible.size() / (double) CMDS_PER_PAGE));
        int p = Math.max(1, Math.min(page, totalPages));
        int start = (p - 1) * CMDS_PER_PAGE;
        int end = Math.min(start + CMDS_PER_PAGE, accessible.size());

        // Header
        final int pFinal = p;
        final int totalPagesFinal = totalPages;
        src.sendSuccess(() -> MessageUtil.component(
            "commands.neoessentials.help.header", pFinal, totalPagesFinal), false);

        // List commands
        for (int i = start; i < end; i++) {
            CommandRegistry.CommandInfo cmd = accessible.get(i);
            String desc = getLocalizedDescription(cmd);
            src.sendSuccess(() -> MessageUtil.component(
                "commands.neoessentials.help.entry", cmd.getName(), desc), false);
        }

        // Footer
        if (totalPages > 1) {
            final int nextPage = p < totalPages ? (p + 1) : 1;
            src.sendSuccess(() -> MessageUtil.component(
                "commands.neoessentials.help.footer_next", nextPage), false);
        } else {
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.help.footer"), false);
        }
        return 1;
    }

    private static void showCommandDetail(CommandSourceStack src, CommandRegistry.CommandInfo cmd) {
        src.sendSuccess(() -> MessageUtil.component(
            "commands.neoessentials.help.detail_header", cmd.getName()), false);
        String desc = getLocalizedDescription(cmd);
        src.sendSuccess(() -> Component.literal("§7" + desc), false);
        String permDisplay = resolvePermissionNode(cmd);
        src.sendSuccess(() -> MessageUtil.component(
            "commands.neoessentials.help.detail_permission", permDisplay), false);
        List<String> aliases = cmd.getAliases();
        if (aliases != null && !aliases.isEmpty()) {
            src.sendSuccess(() -> MessageUtil.component(
                "commands.neoessentials.help.detail_aliases", String.join("§7, §e", aliases)), false);
        }
    }

    /**
     * The permission node to check/display for a command: its explicit override if one was
     * registered via {@link CommandRegistry#registerCommandWithPermission}, else the legacy
     * {@code "neoessentials." + name} guess (correct for most simple commands, but not all —
     * see that method's own doc for why an explicit override is sometimes needed).
     */
    private static String resolvePermissionNode(CommandRegistry.CommandInfo cmd) {
        if (cmd.hasPermissionOverride()) {
            String override = cmd.getPermissionNodeOverride();
            if (override != null && !override.isEmpty()) {
                return override;
            }
        }
        return "neoessentials." + cmd.getName().toLowerCase();
    }

    /**
     * Get a localized description for a command.
     * Checks for a translation key "commands.neoessentials.cmd.NAME.description" first;
     * falls back to the registered English description if not found.
     */
    private static String getLocalizedDescription(CommandRegistry.CommandInfo cmd) {
        String name = cmd.getName().toLowerCase();
        String descKey = "commands.neoessentials.cmd." + name + ".description";
        if (MessageUtil.hasTranslation(descKey)) {
            return MessageUtil.localize(descKey);
        }
        String fallback = cmd.getDescription();
        return (fallback != null && !fallback.isEmpty())
            ? fallback
            : MessageUtil.localize("commands.neoessentials.help.no_description");
    }

    @SuppressWarnings("unchecked")
    private static void removeRootLiteral(CommandDispatcher<CommandSourceStack> dispatcher, String literal) {
        try {
            CommandNode<CommandSourceStack> root = dispatcher.getRoot();
            Field childrenField = CommandNode.class.getDeclaredField("children");
            Field literalsField = CommandNode.class.getDeclaredField("literals");
            childrenField.setAccessible(true);
            literalsField.setAccessible(true);

            Map<String, CommandNode<CommandSourceStack>> children =
                (Map<String, CommandNode<CommandSourceStack>>) childrenField.get(root);
            Map<String, CommandNode<CommandSourceStack>> literals =
                (Map<String, CommandNode<CommandSourceStack>>) literalsField.get(root);

            children.remove(literal);
            literals.remove(literal);
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Could not remove existing '{}' command node before registering NeoEssentials help", literal, e);
        }
    }

    /**
     * Resolve a command's help description: prefer the localized key
     * commands.neoessentials.&lt;name&gt;.description, falling back to the description
     * registered in code, then to a generic "no description" message.
     */
    private static String resolveDescription(CommandRegistry.CommandInfo cmd) {
        String key = "commands.neoessentials." + cmd.getName().toLowerCase() + ".description";
        if (MessageUtil.hasTranslation(key)) {
            return MessageUtil.localize(key);
        }
        return cmd.getDescription() != null
            ? cmd.getDescription()
            : MessageUtil.localize("commands.neoessentials.help.no_description");
    }
}
