package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.commands.CommandRegistry;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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

        dispatcher.register(Commands.literal("help")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), PERMISSION);
            })
            // /help
            .executes(ctx -> executeHelp(ctx, null, 1))
            // /help <page|command> — ONE word argument, disambiguated in code below.
            // Two sibling arguments (int page + word command) make Brigadier resolve
            // "/help 2" to a command search for "2" instead of page navigation.
            .then(Commands.argument("target", StringArgumentType.word())
                .executes(ctx -> executeHelpTarget(ctx, StringArgumentType.getString(ctx, "target"), 1))
                // /help <command> <page>
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeHelpTarget(ctx,
                        StringArgumentType.getString(ctx, "target"),
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
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> executeHelp(ctx, null, IntegerArgumentType.getInteger(ctx, "page")))
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

        // Build list of commands accessible to this player
        // Show all registered commands; individual commands handle their own permission checks
        List<CommandRegistry.CommandInfo> accessible = allCommands.stream()
            .filter(cmd -> {
                // Console can see everything; for players check admin or generic perm
                if (uuid == null) return true;
                String perm = "neoessentials." + cmd.getName().toLowerCase();
                // Admin can see all
                if (PermissionAPI.hasPermission(uuid, "neoessentials.admin")) return true;
                // Try the command-specific permission; if not explicitly denied, show it
                return PermissionAPI.hasPermission(uuid, perm)
                    || PermissionAPI.hasPermission(uuid, "neoessentials.*");
            })
            .sorted(Comparator.comparing(CommandRegistry.CommandInfo::getName))
            .collect(Collectors.toList());

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
                .collect(Collectors.toList());
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
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.help.header", p, totalPages), false);

        // List commands
        for (int i = start; i < end; i++) {
            CommandRegistry.CommandInfo cmd = accessible.get(i);
            String desc = resolveDescription(cmd);
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.help.entry", cmd.getName(), desc), false);
        }

        // Footer
        if (totalPages > 1) {
            int nextPage = (p < totalPages) ? (p + 1) : 1;
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.help.footer_paged", nextPage), false);
        } else {
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.help.footer_single"), false);
        }
        return 1;
    }

    private static void showCommandDetail(CommandSourceStack src, CommandRegistry.CommandInfo cmd) {
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.help.detail_header", cmd.getName()), false);
        String desc = resolveDescription(cmd);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.help.detail_description", desc), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.help.detail_permission", cmd.getName()), false);
        List<String> aliases = cmd.getAliases();
        if (aliases != null && !aliases.isEmpty()) {
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.help.detail_aliases", String.join("§7, §e", aliases)), false);
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



