package com.zerog.neoessentials.util;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.api.permissions.PermissionRegistry;
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
                // Pre-localized here (not just a key) because the required-permission node is a
                // runtime argument that the caller's MessageUtil.error(getErrorMessage()) can't supply.
                String msg = MessageUtil.localize(
                    "commands.neoessentials.permission_check.no_permission_required", permission);
                // Append human-friendly description from PermissionRegistry if available
                try {
                    PermissionRegistry.PermissionInfo info =
                        PermissionRegistry.getInstance().getPermissionInfo(permission);
                    if (info != null && info.getDescription() != null && !info.getDescription().isEmpty()) {
                        msg += "\n§8(" + info.getDescription() + ")";
                    }
                } catch (Exception e) {
                    NeoLog.debug(LOGGER, LogCategory.GENERAL,
                        "Failed to look up description for permission '{}'", permission, e);
                }
                return PermissionResult.failure(msg);
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
            // Pre-localized here (not just a key) because the required-permission list is a
            // runtime argument that the caller's MessageUtil.error(getErrorMessage()) can't supply.
            String orSeparator = MessageUtil.localize("commands.neoessentials.permission_check.or_separator");
            StringBuilder msg = new StringBuilder(MessageUtil.localize(
                "commands.neoessentials.permission_check.no_permission_required_any",
                String.join(orSeparator, permissions)));
            // Append descriptions for each node from PermissionRegistry
            try {
                for (String perm : permissions) {
                    PermissionRegistry.PermissionInfo info =
                        PermissionRegistry.getInstance().getPermissionInfo(perm);
                    if (info != null && info.getDescription() != null && !info.getDescription().isEmpty()) {
                        msg.append("\n§8  ").append(perm).append(" — ").append(info.getDescription());
                    }
                }
            } catch (Exception e) {
                NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to look up permission descriptions", e);
            }
            return PermissionResult.failure(msg.toString());

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
                // Pre-localized: the required permission node is a runtime argument.
                return PermissionResult.failure(MessageUtil.localize(
                    "commands.neoessentials.permission_check.no_permission_required", basePermission));
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