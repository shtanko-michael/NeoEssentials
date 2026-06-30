package com.zerog.neoessentials.commands.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.webdashboard.DashboardAPI;
import com.zerog.neoessentials.webdashboard.DashboardFileManager;
import com.zerog.neoessentials.webdashboard.DashboardLifecycleManager;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Command to manage the Dashboard API server
 * Usage:
 * - /dashboard - Show dashboard status
 * - /dashboard start - Start the dashboard
 * - /dashboard stop - Stop the dashboard
 * - /dashboard restart - Restart the dashboard
 * - /dashboard url - Show dashboard URL
 */
public class DashboardCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dashboard")
            .requires(source -> PermissionValidator.validateAdminPermission(source, "neoessentials.admin.dashboard").hasPermission())
            .executes(DashboardCommand::showStatus)
            .then(Commands.literal("start")
                .executes(DashboardCommand::startDashboard))
            .then(Commands.literal("stop")
                .executes(DashboardCommand::stopDashboard))
            .then(Commands.literal("restart")
                .executes(DashboardCommand::restartDashboard))
            .then(Commands.literal("status")
                .executes(DashboardCommand::showStatus))
            .then(Commands.literal("url")
                .executes(DashboardCommand::showUrl))
            .then(Commands.literal("update")
                .executes(DashboardCommand::updateDashboardFiles))
        );
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.separator"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.title"), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.dashboard.separator"), false);
        source.sendSuccess(() -> Component.literal(""), false);
        
        // Running status
        String runningStatus = status.running ? MessageUtil.localize("commands.neoessentials.dashboard.status_online") : MessageUtil.localize("commands.neoessentials.dashboard.status_offline");
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.status_line", runningStatus)), false);

        // Config status
        String configStatus = status.configEnabled ? MessageUtil.localize("commands.neoessentials.dashboard.config_enabled") : MessageUtil.localize("commands.neoessentials.dashboard.config_disabled");
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.config_line", configStatus)), false);

        // Manual override
        if (status.manuallyDisabled) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.override_manually_disabled")), false);
        }

        // URL
        if (status.running) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.url_line", status.url)), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.api_line", status.url)), false);
        }

        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("§6§l═══════════════════════════════════"), false);

        // Show available commands
        if (!status.running) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.hint_start")), false);
        } else {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.hint_stop")), false);
        }
        
        return 1;
    }
    
    private static int startDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!ConfigManager.isWebDashboardEnabled()) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.error_disabled_config")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.enable_in_config")), false);
            return 0;
        }

        if (DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.warning_already_running")), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.starting")), false);

        boolean success = DashboardLifecycleManager.startDashboard(source.getServer());

        if (success) {
            DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.started_success")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.url_line", status.url)), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.api_line", status.url)), false);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.start_failed")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.check_logs")), false);
            return 0;
        }
    }
    
    private static int stopDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.warning_not_running")), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.stopping")), false);

        boolean success = DashboardLifecycleManager.stopDashboard();

        if (success) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.stopped_success")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.hint_restart_start")), false);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.stop_failed")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.check_logs")), false);
            return 0;
        }
    }
    
    private static int restartDashboard(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.warning_not_running")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.hint_start_instead")), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.restarting")), false);

        // Stop
        boolean stopSuccess = DashboardLifecycleManager.stopDashboard();
        if (!stopSuccess) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.stop_failed")), false);
            return 0;
        }
        
        // Wait a moment
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Start
        boolean startSuccess = DashboardLifecycleManager.startDashboard(source.getServer());
        
        if (startSuccess) {
            DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.restarted_success")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.url_line", status.url)), false);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.restart_failed")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.check_logs")), false);
            return 0;
        }
    }
    
    private static int showUrl(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!DashboardAPI.getInstance().isRunning()) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.error_not_running")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.hint_start_it")), false);
            return 0;
        }

        DashboardLifecycleManager.DashboardStatus status = DashboardLifecycleManager.getStatus();

        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.urls_title")), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.frontend_line", status.url)), false);
        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.api_line", status.url)), false);
        
        return 1;
    }

    private static int updateDashboardFiles(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.updating_files")), false);

        try {
            DashboardFileManager.forceUpdateDashboardFiles();

            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.files_updated_success")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.files_extracted_to")), false);

            // Recommend restart if dashboard is running
            if (DashboardAPI.getInstance().isRunning()) {
                source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.restart_to_apply")), false);
            }

            return 1;
        } catch (Exception e) {
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.files_update_failed")), false);
            source.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.dashboard.error_detail", e.getMessage())), false);
            return 0;
        }
    }
}
