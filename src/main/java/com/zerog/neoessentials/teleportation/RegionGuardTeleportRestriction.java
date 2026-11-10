package com.zerog.neoessentials.teleportation;

import java.lang.reflect.Method;

import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Optional RegionGuard bridge for anti-teleport-escape regions. */
public final class RegionGuardTeleportRestriction {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionGuardTeleportRestriction.class);
    /** Farmstead Claims first, then the original RegionGuard — both expose the same static check. */
    private static final String[] API_CLASSES = {
        "online.farmsteadminecraft.claims.api.ClaimsApi",
        "com.banerokid.regionguard.api.RegionGuardApi",
    };

    private RegionGuardTeleportRestriction() {}

    public static boolean allowsLandingAt(ServerPlayer player) {
        return allowsLandingAt(player.serverLevel(), player.blockPosition());
    }

    public static boolean allowsLandingAt(TeleportLocation location) {
        ServerLevel level = location.getLevel();
        return level == null || allowsLandingAt(level, BlockPos.containing(location.getX(), location.getY(), location.getZ()));
    }

    private static boolean allowsLandingAt(ServerLevel level, BlockPos pos) {
        for (String apiClass : API_CLASSES) {
            try {
                Class<?> api = Class.forName(apiClass);
                Method check = api.getMethod("isTeleportEscapeAllowed", ServerLevel.class, BlockPos.class);
                return (Boolean) check.invoke(null, level, pos);
            } catch (ClassNotFoundException ignored) {
                // Not installed — try the next claims mod; none at all means no restriction.
            } catch (ReflectiveOperationException | LinkageError exception) {
                NeoLog.warn(LOGGER, LogCategory.TELEPORTATION,
                    "Could not query teleport-escape at {}; allowing teleport: {}", pos, exception.toString());
                return true;
            }
        }
        return true;
    }
}
