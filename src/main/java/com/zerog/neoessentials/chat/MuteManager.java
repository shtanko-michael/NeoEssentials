
package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.zerog.neoessentials.util.ChatDebugUtil;

/**
 * Thread-safe manager for muted players.
 */
public class MuteManager {
    // Use thread-safe Set
    private static final Set<String> mutedPlayers = ConcurrentHashMap.newKeySet();
    // Lowercase player name -> epoch millis when the mute expires. Absent entry = permanent mute.
    private static final Map<String, Long> muteExpiry = new ConcurrentHashMap<>();
    // Lowercase player name -> reason given to /mute. Absent entry = muted without a reason.
    private static final Map<String, String> muteReason = new ConcurrentHashMap<>();

    /**
     * Returns a snapshot of all muted player names (lowercase). Includes names whose mute
     * has expired but hasn't been lazily purged yet — use {@link #isMuted(String)} to check
     * a specific name's current state.
     */
    public static Set<String> getMutedPlayers() {
        return new HashSet<>(mutedPlayers);
    }

    /**
     * Mutes a player permanently.
     */
    public static void mute(ServerPlayer sender, String targetName) {
        mute(sender, targetName, 0L);
    }

    /**
     * Mutes a player for the given duration, without a reason.
     * @param durationMillis how long the mute lasts; 0 or negative means permanent.
     */
    public static void mute(ServerPlayer sender, String targetName, long durationMillis) {
        mute(sender, targetName, durationMillis, null);
    }

    /**
     * Mutes a player for the given duration with an optional reason.
     * @param durationMillis how long the mute lasts; 0 or negative means permanent.
     * @param reason reason shown to the muted player when their chat is blocked; null/blank = none.
     */
    public static void mute(ServerPlayer sender, String targetName, long durationMillis, String reason) {
        String key = targetName.toLowerCase();
        mutedPlayers.add(key);
        if (durationMillis > 0) {
            muteExpiry.put(key, System.currentTimeMillis() + durationMillis);
        } else {
            muteExpiry.remove(key);
        }
        if (reason != null && !reason.isBlank()) {
            muteReason.put(key, reason.trim());
        } else {
            muteReason.remove(key);
        }
        ChatDebugUtil.debug("Muted player %s (duration=%s, reason=%s). Muted players now: %s",
            targetName, durationMillis > 0 ? durationMillis + "ms" : "permanent",
            muteReason.getOrDefault(key, "none"), mutedPlayers);
    }

    public static void unmute(ServerPlayer sender, String targetName) {
        String key = targetName.toLowerCase();
        mutedPlayers.remove(key);
        muteExpiry.remove(key);
        muteReason.remove(key);
        ChatDebugUtil.debug("Unmuted player %s. Muted players now: %s", targetName, mutedPlayers);
    }

    public static boolean isMuted(ServerPlayer player) {
        return isMuted(player.getName().getString());
    }

    /**
     * Checks whether a player (by name) is currently muted, lazily expiring the mute
     * (removing it from the underlying set) if its duration has elapsed.
     */
    public static boolean isMuted(String playerName) {
        String key = playerName.toLowerCase();
        boolean result = mutedPlayers.contains(key);
        if (result) {
            Long expiry = muteExpiry.get(key);
            if (expiry != null && System.currentTimeMillis() >= expiry) {
                mutedPlayers.remove(key);
                muteExpiry.remove(key);
                muteReason.remove(key);
                ChatDebugUtil.debug("Mute for %s expired, auto-unmuting", playerName);
                result = false;
            }
        }
        ChatDebugUtil.debug("Checking if %s is muted: %s (mutedPlayers contains: %s)", playerName, result, mutedPlayers);
        return result;
    }

    /**
     * Returns the remaining mute duration in milliseconds, or 0 if the player is not
     * muted or is muted permanently.
     */
    public static long getRemainingMillis(String playerName) {
        String key = playerName.toLowerCase();
        if (!mutedPlayers.contains(key)) {
            return 0;
        }
        Long expiry = muteExpiry.get(key);
        return expiry == null ? 0 : Math.max(0, expiry - System.currentTimeMillis());
    }

    /**
     * Returns the reason the player was muted for, or {@code null} if they are not muted or
     * were muted without a reason.
     */
    public static String getReason(String playerName) {
        String key = playerName.toLowerCase();
        if (!mutedPlayers.contains(key)) {
            return null;
        }
        return muteReason.get(key);
    }

    /**
     * Returns true if the player is muted with no expiry (mutedPlayers contains them and
     * there's no entry in muteExpiry). Returns false if not muted at all.
     */
    public static boolean isPermanentlyMuted(String playerName) {
        String key = playerName.toLowerCase();
        return mutedPlayers.contains(key) && !muteExpiry.containsKey(key);
    }
}
