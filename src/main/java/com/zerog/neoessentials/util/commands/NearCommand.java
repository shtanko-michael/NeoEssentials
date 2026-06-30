package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.CommandSourceHelper;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.util.commands.CommandUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implements the /near command - Shows nearby players within a certain radius
 * Includes distance, direction, and world information
 */
public class NearCommand {
    private static final int DEFAULT_RADIUS = 100;
    private static final int MAX_RADIUS = 500;
    
    /**
     * Register the /near command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("near")) return;
        
        dispatcher.register(
            Commands.literal("near")
                // /near - Show nearby players with default radius (requires player)
                .executes(ctx -> {
                    ServerPlayer player = CommandSourceHelper.requirePlayer(ctx.getSource(), "commands.neoessentials.near.player_only");
                    if (player == null) return 0;

                    PermissionValidator.PermissionResult permResult =
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.near");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    return showNearbyPlayers(player, DEFAULT_RADIUS);
                })
                // /near <radius> - Show nearby players with custom radius
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
                    .executes(ctx -> {
                        ServerPlayer player = CommandSourceHelper.requirePlayer(ctx.getSource(), "commands.neoessentials.near.player_only");
                        if (player == null) return 0;

                        PermissionValidator.PermissionResult permResult =
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.near");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        int radius = IntegerArgumentType.getInteger(ctx, "radius");
                        return showNearbyPlayers(player, radius);
                    })
                )
        );
        
        // Also register /nearby alias
        dispatcher.register(
            Commands.literal("nearby")
                .executes(ctx -> {
                    ServerPlayer player = CommandSourceHelper.requirePlayer(ctx.getSource(), "commands.neoessentials.near.player_only");
                    if (player == null) return 0;

                    PermissionValidator.PermissionResult permResult =
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.near");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    return showNearbyPlayers(player, DEFAULT_RADIUS);
                })
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
                    .executes(ctx -> {
                        ServerPlayer player = CommandSourceHelper.requirePlayer(ctx.getSource(), "commands.neoessentials.near.player_only");
                        if (player == null) return 0;

                        PermissionValidator.PermissionResult permResult =
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.near");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        int radius = IntegerArgumentType.getInteger(ctx, "radius");
                        return showNearbyPlayers(player, radius);
                    })
                )
        );
    }
    
    /**
     * Show nearby players within the specified radius
     */
    private static int showNearbyPlayers(ServerPlayer player, int radius) {
        Vec3 playerPos = player.position();
        
        // Null safety check for server
        if (player.getServer() == null) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.near.server_error"));
            return 0;
        }

        // Get all nearby players (exclude self)
        List<NearbyPlayerInfo> nearbyPlayers = player.getServer().getPlayerList().getPlayers().stream()
            .filter(p -> !p.equals(player))
            .filter(p -> p.level() == player.level()) // Same dimension
            .filter(p -> !isVanished(p) || canSeeVanished(player)) // Vanish check
            .map(p -> new NearbyPlayerInfo(p, playerPos))
            .filter(info -> info.distance <= radius)
            .sorted(Comparator.comparingDouble(info -> info.distance))
            .toList(); // Java 16+ optimized collection

        // Header
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.near.header", 
            nearbyPlayers.size(), radius));
        
        if (nearbyPlayers.isEmpty()) {
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.near.no_players"));
            return 1;
        }
        
        // Show nearby players
        for (NearbyPlayerInfo info : nearbyPlayers) {
            MutableComponent message = createPlayerEntry(info, player);
            player.sendSystemMessage(message);
        }
        
        // Footer with statistics
        if (nearbyPlayers.size() > 1) {
            NearbyPlayerInfo closest = nearbyPlayers.getFirst(); // Java 21+
            NearbyPlayerInfo farthest = nearbyPlayers.getLast(); // Java 21+

            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.near.stats",
                closest.player.getName().getString(), String.format("%.1f", closest.distance),
                farthest.player.getName().getString(), String.format("%.1f", farthest.distance)));
        }
        
        return 1;
    }
    
    /**
     * Create a formatted entry for a nearby player
     */
    private static MutableComponent createPlayerEntry(NearbyPlayerInfo info, ServerPlayer viewer) {
        String distanceStr = CommandUtil.formatDistance(info.distance, 1);
        String direction = CommandUtil.getSimpleDirection(info.relativePos.x, info.relativePos.z);
        
        // Base message with distance and direction
        MutableComponent message = Component.literal(MessageUtil.localize("commands.neoessentials.near.entry",
            info.player.getName().getString(), distanceStr, direction));
        
        // Add status indicators
        List<String> statusList = new ArrayList<>();
        
        if (isAfk(info.player)) {
            statusList.add(MessageUtil.localize("commands.neoessentials.near.status_afk"));
        }

        if (isVanished(info.player)) {
            statusList.add(MessageUtil.localize("commands.neoessentials.near.status_vanished"));
        }

        if (info.player.hasPermissions(4)) {
            statusList.add(MessageUtil.localize("commands.neoessentials.near.status_op"));
        }

        if (!statusList.isEmpty()) {
            message.append(Component.literal(MessageUtil.localize("commands.neoessentials.near.status_wrap", String.join("§7,", statusList))));
        }
        
        // Create hover text with detailed info
        MutableComponent hoverText = Component.literal("")
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.near.hover_player", info.player.getName().getString()) + "\n"))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.near.hover_distance", distanceStr) + "\n"))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.near.hover_direction", direction) + "\n"))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.near.hover_world", info.player.level().dimension().location()) + "\n"))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.near.hover_coordinates",
                (int)info.player.getX(), (int)info.player.getY(), (int)info.player.getZ()) + "\n"))
            .append(Component.literal(MessageUtil.localize("commands.neoessentials.near.hover_health",
                String.format("%.1f", info.player.getHealth()), String.format("%.1f", info.player.getMaxHealth())) + "\n"));

        if (isAfk(info.player)) {
            hoverText.append(Component.literal(MessageUtil.localize("commands.neoessentials.near.hover_afk") + "\n"));
        }

        hoverText.append(Component.literal("\n" + MessageUtil.localize("commands.neoessentials.near.hover_click_teleport")));
        
        // Add click event for teleportation (if has permission)
        PermissionValidator.PermissionResult tpResult = 
            PermissionValidator.validatePermission(viewer.createCommandSourceStack(), "neoessentials.teleport.tp");
        
        if (tpResult.hasPermission()) {
            message = message.withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
                    "/tp " + info.player.getName().getString()))
            );
        } else {
            // Just hover text, no click event
            message = message.withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
            );
        }
        
        return message;
    }
    
    // Removed getDirection - now using CommandUtil.getSimpleDirection
    
    /**
     * Check if a player is vanished
     * Integrates with VanishManager for actual vanish state
     */
    private static boolean isVanished(ServerPlayer player) {
        // If vanish system is disabled, always return false
        if (!ConfigManager.getInstance().isVanishSystemEnabled()) {
            return false;
        }
        // Use VanishManager to check actual vanish state
        try {
            com.zerog.neoessentials.moderation.VanishManager vanishManager = 
                com.zerog.neoessentials.moderation.VanishManager.getInstance();
            return vanishManager.isPlayerVanished(player.getUUID());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if viewer can see vanished players
     * Integrates with VanishManager for actual permission state
     */
    private static boolean canSeeVanished(ServerPlayer viewer) {
        // If vanish system is disabled, always return false
        if (!ConfigManager.getInstance().isVanishSystemEnabled()) {
            return false;
        }
        // Use VanishManager to check if viewer can see vanished players
        try {
            com.zerog.neoessentials.moderation.VanishManager vanishManager = 
                com.zerog.neoessentials.moderation.VanishManager.getInstance();
            return vanishManager.canPlayerSeeVanished(viewer.getUUID());
        } catch (Exception e) {
            return PermissionValidator.validatePermission(viewer.createCommandSourceStack(), "neoessentials.vanish.see").hasPermission();
        }
    }
    
    /**
     * Check if a player is AFK
     * Integrates with AfkManager for actual AFK state
     */
    private static boolean isAfk(ServerPlayer player) {
        // Check if chat module is enabled
        if (!ConfigManager.isChatEnabled()) {
            return false;
        }
        // Use AfkManager to check actual AFK state
        try {
            com.zerog.neoessentials.chat.AfkManager afkManager = 
                com.zerog.neoessentials.chat.AfkManager.getInstance();
            return afkManager.isAfk(player);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Data class for nearby player information
     */
    private static class NearbyPlayerInfo {
        final ServerPlayer player;
        final double distance;
        final Vec3 relativePos;
        
        NearbyPlayerInfo(ServerPlayer player, Vec3 viewerPos) {
            this.player = player;
            Vec3 playerPos = player.position();
            this.relativePos = playerPos.subtract(viewerPos);
            this.distance = viewerPos.distanceTo(playerPos);
        }
    }
}


