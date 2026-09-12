
package com.zerog.neoessentials.chat.command;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Handles the /mutelist command for listing all muted players.
 */
public class MuteListCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Check if chat module is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.isChatEnabled()) {
            return;
        }

        // Check if the mutelist command is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("mutelist")) {
            return;
        }

        dispatcher.register(Commands.literal("mutelist")
            .requires(src -> PermissionValidator.allows(src, "neoessentials.chat.mute"))
            .executes(ctx -> {
                CommandSourceStack source = ctx.getSource();

                // Sender may be console/RCON — allowed, same as every other moderation command.
                net.minecraft.server.level.ServerPlayer sender = source.getPlayer();

                // Check if command is enabled
                ChatManager chatManager = com.zerog.neoessentials.api.ChatAPI.getChatManager();
                if (chatManager != null && !chatManager.isMuteListEnabled()) {
                    source.sendFailure(MessageUtil.error("commands.neoessentials.mutelist.disabled"));
                    return 0;
                }

                // Check permissions (console always passes)
                if (sender != null && !com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(sender.getUUID(), "neoessentials.chat.mute")) {
                    source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
                    return 0;
                }
                
                java.util.List<String> muted = new java.util.ArrayList<>(com.zerog.neoessentials.chat.MuteManager.getMutedPlayers());
                // isMuted() lazily expires stale entries, so filter through it to get the live list
                java.util.List<String> entries = new java.util.ArrayList<>();
                for (String name : muted) {
                    if (!com.zerog.neoessentials.chat.MuteManager.isMuted(name)) continue;
                    long remaining = com.zerog.neoessentials.chat.MuteManager.getRemainingMillis(name);
                    entries.add(remaining > 0
                        ? MessageUtil.localize("commands.neoessentials.mutelist.entry_temp", name, com.zerog.neoessentials.moderation.BanManager.formatDuration(remaining))
                        : MessageUtil.localize("commands.neoessentials.mutelist.entry_permanent", name));
                }
                if (entries.isEmpty()) {
                    source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.mutelist.empty"), false);
                } else {
                    String mutedList = String.join(", ", entries);
                    source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.mutelist.list", mutedList), false);
                }
                return 1;
            })
        );
    }
}
