package com.zerog.neoessentials.util.handlers;

import com.zerog.neoessentials.chat.AfkManager;
import com.zerog.neoessentials.util.commands.PlayerStateCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Handles god mode damage cancellation and session tracking for playtime / god state cleanup.
 * Also handles AFK invulnerability — cancels all damage dealt to AFK players when the
 * {@code afk.invulnerableWhenAfk} config option is enabled.
 */
@EventBusSubscriber(modid = "neoessentials")
public class GodModeEventHandler {

    /** Cancel all incoming damage for players in god mode or (optionally) in AFK state. */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // God mode — always cancel
        if (PlayerStateCommands.isGodMode(player.getUUID())) {
            event.setNewDamage(0f);
            return;
        }

        // AFK invulnerability — cancel if the player is AFK and the feature is enabled
        AfkManager afk = AfkManager.getInstance();
        if (afk.isInvulnerableWhenAfk() && afk.isAfk(player.getUUID())) {
            event.setNewDamage(0f);
        }
    }

    /** Prevent mobs from acquiring players protected by /god as an AI target. */
    @SubscribeEvent
    public static void onMobChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob
                && event.getNewAboutToBeSetTarget() instanceof ServerPlayer player
                && PlayerStateCommands.isGodMode(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    /**
     * Stops nearby mobs that acquired this player before /god was enabled from continuing
     * their attack. Future target acquisition is blocked by {@link #onMobChangeTarget}.
     */
    public static void clearExistingMobTargets(ServerPlayer player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64.0))) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
    }

    /** Track the session and restore persisted fly/god state. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerStateCommands.onPlayerJoin(player);
            com.zerog.neoessentials.util.commands.UtilityCommands.onPlayerJoin(player);
        }
    }

    /** Clean up god/session state on logout. */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerStateCommands.onPlayerQuit(player.getUUID());
            com.zerog.neoessentials.util.commands.UtilityCommands.onPlayerQuit(player.getUUID());
        }
    }
}

