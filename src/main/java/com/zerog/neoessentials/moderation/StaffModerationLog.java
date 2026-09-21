package com.zerog.neoessentials.moderation;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * A private, in-game moderation feed.  It deliberately lives beside the managers rather than
 * individual commands, so dashboard actions and automatic expirations are visible as well.
 */
public final class StaffModerationLog {
    public static final String PERMISSION = "neoessentials.moderation.stafflog";
    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd.MM")
        .withZone(ZoneId.systemDefault());

    private StaffModerationLog() { }

    public static void mute(String target, String actor, String reason, long expireAt) {
        announce("neoessentials.moderation.stafflog.mute", target, staff(actor), duration(expireAt), reason(reason));
    }

    public static void unmute(String target, String actor) {
        announce("neoessentials.moderation.stafflog.unmute", target, staff(actor));
    }

    public static void ban(String target, String actor, String reason, long expireAt) {
        announce("neoessentials.moderation.stafflog.ban", target, staff(actor), duration(expireAt), reason(reason));
    }

    public static void unban(String target, String actor) {
        announce("neoessentials.moderation.stafflog.unban", target, staff(actor));
    }

    public static void jail(String target, String actor, String jail, String reason, long expireAt) {
        announce("neoessentials.moderation.stafflog.jail", target, staff(actor), jail, duration(expireAt), reason(reason));
    }

    public static void unjail(String target, String actor, boolean expired) {
        announce(expired ? "neoessentials.moderation.stafflog.jail_expired" : "neoessentials.moderation.stafflog.unjail",
            target, staff(actor));
    }

    public static void expired(String target, String punishmentKey) {
        announce("neoessentials.moderation.stafflog.expired", target, MessageUtil.localize(punishmentKey));
    }

    private static void announce(String key, Object... args) {
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        // Expiry cleanup can run on a scheduler. Sending packets is main-thread work.
        server.execute(() -> {
            var message = MessageUtil.coloredText(MessageUtil.localize(key, args));
            for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
                if (recipient.hasPermissions(2) || PermissionAPI.hasPermission(recipient.getUUID(), PERMISSION)) {
                    recipient.sendSystemMessage(message);
                }
            }
        });
    }

    private static String staff(String actor) {
        return actor == null || actor.isBlank() ? "Console" : actor;
    }

    private static String reason(String reason) {
        return reason == null || reason.isBlank() ? "—" : reason;
    }

    private static String duration(long expireAt) {
        return expireAt <= 0 ? MessageUtil.localize("neoessentials.moderation.stafflog.permanent")
            : MessageUtil.localize("neoessentials.moderation.stafflog.until",
                EXPIRY_FORMAT.format(Instant.ofEpochMilli(expireAt)));
    }
}
