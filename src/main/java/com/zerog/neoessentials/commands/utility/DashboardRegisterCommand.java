package com.zerog.neoessentials.commands.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.webdashboard.security.DashboardRegistrationManager;
import com.zerog.neoessentials.webdashboard.security.DashboardAccountRegistration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command for players to register dashboard accounts
 * Usage:
 * - /dashboardregister start - Start registration process
 * - /dashboardregister complete <username> <password> - Complete registration
 * - /dashboardregister status - Check registration status
 */
public class DashboardRegisterCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dashboardregister")
            .requires(source -> {
                // Allow console to use the command too for testing
                if (!source.isPlayer()) {
                    return source.hasPermission(2); // Op level 2
                }
                // For players, check the dashboard access permission
                return PermissionValidator.validatePermission(source,
                    "neoessentials.dashboard.access").hasPermission();
            })
            .executes(context -> {
                // Default action when just /dashboardregister is used - show help
                CommandSourceStack source = context.getSource();
                source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
                source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.help.title")), false);
                source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.help.available")), false);
                source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.help.start")), false);
                source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.help.complete")), false);
                source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.help.status")), false);
                source.sendSuccess(() -> Component.literal(""), false);
                source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
                return 1;
            })
            .then(Commands.literal("start")
                .executes(DashboardRegisterCommand::startRegistration))
            .then(Commands.literal("complete")
                .then(Commands.argument("username", StringArgumentType.word())
                    .then(Commands.argument("password", StringArgumentType.greedyString())
                        .executes(DashboardRegisterCommand::completeRegistration))))
            .then(Commands.literal("status")
                .executes(DashboardRegisterCommand::checkStatus))
        );
    }

    private static int startRegistration(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.general.player_only")), false);
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        // Debug logging
        System.out.println("[DashboardRegister] Player " + player.getName().getString() + " (" + player.getUUID() + ") attempting registration");

        // Check if already registered
        if (manager.isRegistered(player.getUUID())) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.already_registered_info")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.use_credentials")), false);
            System.out.println("[DashboardRegister] Player already registered");
            return 0;
        }

        // Start registration
        String token = manager.startRegistration(player.getUUID(), player.getName().getString());

        System.out.println("[DashboardRegister] Registration token generated: " + (token != null ? "SUCCESS" : "FAILED"));

        if (token == null) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.start_failed")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.contact_admin")), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.started_title")), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.your_token", token)), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.token_expires")), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.to_complete")), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.complete_syntax")), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.example_label")), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.example_usage")), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.password_warning")), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);

        return 1;
    }

    private static int completeRegistration(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.general.player_only")), false);
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        String username = StringArgumentType.getString(context, "username");
        String password = StringArgumentType.getString(context, "password");

        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        // Check if already registered
        if (manager.isRegistered(player.getUUID())) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.already_registered_error")), false);
            return 0;
        }

        // Validate username
        if (username.length() < 3 || username.length() > 20) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.username_length")), false);
            return 0;
        }

        // Validate password
        if (password.length() < 8) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.password_length")), false);
            return 0;
        }

        // For security, we need to get the token from the pending registration
        // Since we can't pass it securely, we'll lookup by UUID
        // This requires a small modification to complete registration

        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.processing")), false);

        // Try to complete registration
        DashboardAccountRegistration registration = completeRegistrationByUuid(
            player.getUUID(), username, password);

        if (registration == null) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.failed")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.possible_reasons")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.reason_expired")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.reason_taken")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.reason_not_started")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.use_start_to_begin")), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.success_title")), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.dashboard_username", registration.getDashboardUsername())), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.linked_to", player.getName().getString())), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.login_at")), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.dashboard_url")), false);
        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.login_instructions")), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);

        return 1;
    }

    private static int checkStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!source.isPlayer()) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.general.player_only")), false);
            return 0;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();
        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.status_title")), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);
        source.sendSuccess(() -> Component.literal(""), false);

        if (manager.isRegistered(player.getUUID())) {
            DashboardAccountRegistration reg = manager.getRegistration(player.getUUID());
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.status_registered")), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.dashboard_username", reg.getDashboardUsername())), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.minecraft_account", reg.getMinecraftUsername())), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.registered_at", formatTimestamp(reg.getRegisteredAt()))), false);

            if (reg.isDiscordLinked()) {
                source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.discord_linked", reg.getDiscordUsername())), false);
            } else {
                source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.discord_not_linked")), false);
            }
        } else {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.status_not_registered")), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboardregister.use_start_to_register")), false);
        }

        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);

        return 1;
    }

    /**
     * Helper method to complete registration by UUID
     * This allows us to avoid passing the token through chat
     */
    private static DashboardAccountRegistration completeRegistrationByUuid(
            java.util.UUID playerUuid, String username, String password) {

        DashboardRegistrationManager manager = DashboardRegistrationManager.getInstance();

        // Find pending registration by UUID
        // We need to add this method to DashboardRegistrationManager
        return manager.completeRegistrationByUuid(playerUuid, username, password);
    }

    /**
     * Format timestamp for display
     */
    private static String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }
}
