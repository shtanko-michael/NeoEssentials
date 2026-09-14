package com.zerog.neoessentials.util;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.util.UUID;

/**
 * Centralized permission validation utility for NeoEssentials.
 * Provides consistent permission checking across all commands.
 */
public class PermissionValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionValidator.class);
    
    /**
     * Brigadier {@code .requires()} helper: console and other non-player sources pass;
     * players must hold {@code permission}. Prefer this over execute-time checks so the
     * command is hidden from tab-complete until the node is granted.
     */
    public static boolean allows(CommandSourceStack source, String permission) {
        if (source == null || permission == null || permission.isBlank()) {
            return false;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return true;
        }
        return PermissionAPI.hasPermission(player.getUUID(), permission);
    }

    /**
     * Like {@link #allows(CommandSourceStack, String)}, but passes if the player holds
     * any of the listed nodes (used for command trees whose subcommands have distinct nodes).
     */
    public static boolean allowsAny(CommandSourceStack source, String... permissions) {
        if (source == null || permissions == null || permissions.length == 0) {
            return false;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return true;
        }
        for (String permission : permissions) {
            if (permission != null && !permission.isBlank()
                    && PermissionAPI.hasPermission(player.getUUID(), permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates if a command source has the required permission.
     * Includes proper error messaging and logging.
     */
    public static PermissionResult validatePermission(CommandSourceStack source, String permission) {
        try {
            // Check if source is a player
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                // Console or other non-player source - allow if has admin level
                if (source.hasPermission(2)) {
                    return PermissionResult.success();
                }
                return PermissionResult.failure("commands.neoessentials.permission_check.players_or_op_only");
            }

            UUID playerUuid = player.getUUID();

            // Validate permission
            if (!PermissionAPI.hasPermission(playerUuid, permission)) {
                NeoLog.debug(LOGGER, LogCategory.GENERAL, "Permission denied for player {} ({}): {}",
                    player.getGameProfile().getName(), playerUuid, permission);
                return PermissionResult.failure(noPermissionMessage());
            }
            
            return PermissionResult.success(player);
            
        } catch (Exception e) {
            LOGGER.error("Error validating permission '{}' for source: {}", permission, e.getMessage(), e);
            return PermissionResult.failure("commands.neoessentials.permission_check.internal_error");
        }
    }
    
    /**
     * Validates if a command source has any of the required permissions.
     */
    public static PermissionResult validateAnyPermission(CommandSourceStack source, String... permissions) {
        try {
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                if (source.hasPermission(2)) {
                    return PermissionResult.success();
                }
                return PermissionResult.failure("commands.neoessentials.permission_check.players_or_op_only");
            }

            UUID playerUuid = player.getUUID();

            // Check if player has any of the required permissions
            for (String permission : permissions) {
                if (PermissionAPI.hasPermission(playerUuid, permission)) {
                    return PermissionResult.success(player);
                }
            }

            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Permission denied for player {} ({}): none of {}",
                player.getGameProfile().getName(), playerUuid, java.util.Arrays.toString(permissions));
            return PermissionResult.failure(noPermissionMessage());

        } catch (Exception e) {
            LOGGER.error("Error validating permissions {} for source: {}", 
                java.util.Arrays.toString(permissions), e.getMessage(), e);
            return PermissionResult.failure("commands.neoessentials.permission_check.internal_error");
        }
    }
    
    /**
     * Validates if a command source has admin-level permissions.
     */
    public static PermissionResult validateAdminPermission(CommandSourceStack source, String adminPermission) {
        try {
            // First check operator status
            if (source.hasPermission(2)) {
                return PermissionResult.success(source.getPlayer());
            }
            
            // Then check specific admin permission
            return validatePermission(source, adminPermission);
            
        } catch (Exception e) {
            LOGGER.error("Error validating admin permission '{}': {}", adminPermission, e.getMessage(), e);
            return PermissionResult.failure("commands.neoessentials.permission_check.internal_error");
        }
    }
    
    /**
     * Validates if a player can target another player (for admin commands).
     * Prevents lower-privilege players from targeting higher-privilege players.
     */
    public static PermissionResult validateTargetPermission(ServerPlayer executor, ServerPlayer target, String basePermission) {
        try {
            UUID executorUuid = executor.getUUID();
            UUID targetUuid = target.getUUID();
            
            // Self-targeting is usually not allowed for admin commands
            if (executorUuid.equals(targetUuid)) {
                return PermissionResult.failure("commands.neoessentials.permission_check.cannot_target_self");
            }

            // Check base permission
            if (!PermissionAPI.hasPermission(executorUuid, basePermission)) {
                return PermissionResult.failure(noPermissionMessage());
            }

            // Check if executor can target this player (prevent privilege escalation)
            String targetProtectionPerm = basePermission + ".exempt";
            if (PermissionAPI.hasPermission(targetUuid, targetProtectionPerm)) {
                // Check if executor has override permission
                String overridePerm = basePermission + ".override";
                if (!PermissionAPI.hasPermission(executorUuid, overridePerm)) {
                    return PermissionResult.failure("commands.neoessentials.permission_check.target_protected");
                }
            }
            
            return PermissionResult.success(executor);
            
        } catch (Exception e) {
            LOGGER.error("Error validating target permission: {}", e.getMessage(), e);
            return PermissionResult.failure("commands.neoessentials.permission_check.internal_error");
        }
    }
    
    /**
     * Resolve the generic denial message before command handlers pass it to
     * {@link MessageUtil#error(String, Object...)}. This avoids legacy custom translations of
     * detailed permission-check keys, whose {0} placeholder formerly held the permission node.
     */
    private static String noPermissionMessage() {
        return MessageUtil.localize("commands.neoessentials.no_permission");
    }

    /**
     * Result class for permission validation operations.
     */
    public static class PermissionResult {
        private final boolean hasPermission;
        private final String errorMessage;
        private final ServerPlayer player;
        
        private PermissionResult(boolean hasPermission, String errorMessage, ServerPlayer player) {
            this.hasPermission = hasPermission;
            this.errorMessage = errorMessage;
            this.player = player;
        }
        
        public static PermissionResult success() {
            return new PermissionResult(true, null, null);
        }
        
        public static PermissionResult success(ServerPlayer player) {
            return new PermissionResult(true, null, player);
        }
        
        public static PermissionResult failure(String errorMessage) {
            return new PermissionResult(false, errorMessage, null);
        }
        
        public boolean hasPermission() {
            return hasPermission;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public ServerPlayer getPlayer() {
            return player;
        }
    }
}
