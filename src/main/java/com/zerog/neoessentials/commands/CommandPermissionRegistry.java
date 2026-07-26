package com.zerog.neoessentials.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Read-only index mapping every NeoEssentials command (and its aliases) to its
 * base permission node, loaded once from the bundled
 * {@code data/neoessentials/command_permissions.json} reference.
 *
 * <p>This is the source of truth {@link CommandRegistry} lacks: CommandInfo stores
 * only name/description/aliases, so {@code HelpCommand} used to fabricate
 * {@code "neoessentials." + name} and hid every command that uses a categorized
 * node ({@code neoessentials.<category>.<command>}). Used by /help only — a
 * display concern; command execution enforces permissions independently.
 */
public final class CommandPermissionRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandPermissionRegistry.class);
    private static final String RESOURCE = "data/neoessentials/command_permissions.json";

    private static volatile CommandPermissionRegistry instance;

    /** command-or-alias (lowercase) -> permission node; only non-null nodes stored. */
    private final Map<String, String> permissionByName = new HashMap<>();
    /** every command-or-alias (lowercase) present in the reference, incl. null-permission ones. */
    private final Set<String> knownNames = new HashSet<>();
    /** false if the reference could not be loaded/parsed — callers must degrade gracefully. */
    private boolean ready;

    private CommandPermissionRegistry() {
        load();
    }

    public static CommandPermissionRegistry getInstance() {
        CommandPermissionRegistry local = instance;
        if (local == null) {
            synchronized (CommandPermissionRegistry.class) {
                local = instance;
                if (local == null) {
                    local = new CommandPermissionRegistry();
                    instance = local;
                }
            }
        }
        return local;
    }

    private void load() {
        try (InputStream in = CommandPermissionRegistry.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                LOGGER.error("{} not found on classpath — /help will fall back to showing all commands", RESOURCE);
                return;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject commands = root.getAsJsonObject("commands");
            for (Map.Entry<String, JsonElement> entry : commands.entrySet()) {
                JsonObject cmd = entry.getValue().getAsJsonObject();
                JsonElement perm = cmd.get("permission");
                String node = (perm != null && !perm.isJsonNull()) ? perm.getAsString() : null;
                register(entry.getKey(), node);
                JsonElement aliases = cmd.get("aliases");
                if (aliases != null && aliases.isJsonArray()) {
                    for (JsonElement a : aliases.getAsJsonArray()) {
                        register(a.getAsString(), node);
                    }
                }

                // A command may expose several independently permission-gated syntaxes.
                // Index them as complete display names so /help can list each one rather
                // than collapsing a command tree (such as RegionGuard's /rg) into its root.
                JsonElement subPermissions = cmd.get("sub_permissions");
                if (subPermissions != null && subPermissions.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> sub : subPermissions.getAsJsonObject().entrySet()) {
                        register(entry.getKey() + " " + sub.getKey(), basePermission(sub.getValue().getAsString()));
                    }
                }
            }
            ready = true;
            LOGGER.info("Loaded command permission index: {} names, {} with a permission node",
                knownNames.size(), permissionByName.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load {} — /help will fall back to showing all commands", RESOURCE, e);
        }
    }

    private void register(String name, String node) {
        String key = name.toLowerCase();
        knownNames.add(key);
        if (node != null) {
            permissionByName.put(key, node);
        }
    }

    /**
     * Some documentation entries describe multiple checks with " + ". For help-list
     * visibility, the first node is the Brigadier gate for the syntax; finer-grained
     * checks still run when the player executes the command.
     */
    private static String basePermission(String node) {
        int separator = node.indexOf(" + ");
        return separator >= 0 ? node.substring(0, separator).trim() : node;
    }

    /** true once the reference parsed successfully; when false, callers should not hide commands. */
    public boolean isReady() {
        return ready;
    }

    /** true if the command/alias appears in the reference (even with a null permission). */
    public boolean isKnown(String command) {
        return knownNames.contains(command.toLowerCase());
    }

    /** base permission node for a command/alias, or null if it has none (or is unknown). */
    public String permissionFor(String command) {
        return permissionByName.get(command.toLowerCase());
    }
}
