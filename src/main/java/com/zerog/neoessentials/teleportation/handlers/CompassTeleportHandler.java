package com.zerog.neoessentials.teleportation.handlers;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.LevelCompat;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Staff compass teleport. Looking at a horizontal top face lands on that block;
 * looking at a vertical wall face lands in the first safe space behind the wall.
 */
@EventBusSubscriber(modid = "neoessentials")
public final class CompassTeleportHandler {
    private static final String PERMISSION = "neoessentials.teleport.compass";
    private static final double MAX_DISTANCE = 100.0D;
    private static final double STEP = 0.25D;
    private static final long DUPLICATE_CLICK_WINDOW_MS = 250L;
    private static final Map<UUID, Long> LAST_CLICK_AT = new ConcurrentHashMap<>();

    private CompassTeleportHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        handle(event, event.getItemStack().is(Items.COMPASS));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        handle(event, event.getItemStack().is(Items.COMPASS));
    }

    private static void handle(PlayerInteractEvent event, boolean isCompass) {
        if (!isCompass || !(event.getEntity() instanceof ServerPlayer player)
            || !PermissionAPI.hasPermission(player.getUUID(), PERMISSION)) {
            return;
        }

        // NeoForge can report the same physical click as both an item and a block
        // interaction. Treat those reports as one staff-tool activation.
        long now = System.currentTimeMillis();
        Long previous = LAST_CLICK_AT.put(player.getUUID(), now);
        if (previous != null && now - previous < DUPLICATE_CLICK_WINDOW_MS) {
            return;
        }

        if (event instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }

        ServerLevel level = LevelCompat.of(player);
        BlockPos destination = findDestination(player, level);
        if (destination == null) {
            player.displayClientMessage(MessageUtil.error("commands.neoessentials.teleport.compass.no_destination"), true);
            return;
        }

        player.teleportTo(level, destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D,
            player.getYRot(), player.getXRot());
        player.displayClientMessage(MessageUtil.success("commands.neoessentials.teleport.compass.success"), true);
    }

    private static BlockPos findDestination(ServerPlayer player, ServerLevel level) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(start, start.add(direction.scale(MAX_DISTANCE)),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }

        // A ground block is an explicit target: land directly on it. A wall is
        // intentionally different, because landing on top of it is not useful for
        // moderation; its side face instead activates the through-wall search below.
        if (hit.getDirection() == Direction.UP) {
            BlockPos onTarget = hit.getBlockPos().above();
            return hasRoomForPlayer(level, onTarget) ? onTarget : null;
        }

        boolean crossedObstacle = false;
        BlockPos previous = null;

        for (double distance = 0.0D; distance <= MAX_DISTANCE; distance += STEP) {
            BlockPos current = BlockPos.containing(start.add(direction.scale(distance)));
            if (current.equals(previous)) {
                continue;
            }
            previous = current;

            // Do not let a staff click synchronously load terrain outside the viewed world.
            if (!level.isLoaded(current)) {
                return null;
            }

            BlockState state = level.getBlockState(current);
            if (!crossedObstacle) {
                if (state.getCollisionShape(level, current).isEmpty()) {
                    continue;
                }
                crossedObstacle = true;
                continue;
            }

            // Keep the player's eye on the ray: its feet are one block below this
            // eye-height cell. We require empty room only, not a floor, so a staff
            // member reaches the actual empty point inside a building.
            BlockPos atFeetHeight = current.below();
            if (hasRoomForPlayer(level, atFeetHeight)) {
                return atFeetHeight;
            }
        }

        return null;
    }

    /** A staff compass may target air; it must never put the player inside a block. */
    private static boolean hasRoomForPlayer(ServerLevel level, BlockPos feet) {
        if (!level.isLoaded(feet) || !level.isLoaded(feet.above())) {
            return false;
        }
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
            && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }
}
