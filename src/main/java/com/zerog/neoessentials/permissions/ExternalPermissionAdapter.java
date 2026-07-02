package com.zerog.neoessentials.permissions;

import java.util.UUID;

/**
 * Interface for external permission adapters (LuckPerms, FTB Ranks, etc).
 */
public interface ExternalPermissionAdapter {
    /**
     * Check if the user has the given permission.
     * @param uuid The UUID of the user.
     * @param permission The permission node.
     * @return true if the user has the permission, false otherwise.
     */
    boolean hasPermission(UUID uuid, String permission);

    /**
     * Get the prefix for the user (if supported).
     * @param uuid The UUID of the user.
     * @return The prefix string, or null if not supported.
     */
    String getPrefix(UUID uuid);

    /**
     * Get the suffix for the user (if supported).
     * @param uuid The UUID of the user.
     * @return The suffix string, or null if not supported.
     */
    String getSuffix(UUID uuid);

    /**
     * Read an integer meta/option value for the user (e.g. LuckPerms meta,
     * set via {@code /lp user <name> meta set <key> <value>}).
     *
     * @param uuid The UUID of the user.
     * @param key The meta key (e.g. "neoessentials.homes").
     * @return The parsed value, or null if the backend has no meta concept,
     *         the key is not set for the user, or the value is not an integer.
     */
    default Integer getMetaInt(UUID uuid, String key) {
        return null;
    }

    /**
     * Reload the external permission data (if supported).
     */
    void reload();

    /**
     * @return The name of the external system (e.g., "LuckPerms").
     */
    String getName();
    
    /**
     * Check if this adapter is properly loaded and available for use.
     * @return true if the external system is available, false otherwise.
     */
    boolean isAvailable();
}
