package com.zerog.neoessentials.shop.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.shop.ShopManager;
import com.zerog.neoessentials.shop.handlers.ShopSignHandler;
import com.zerog.neoessentials.shop.model.ShopData;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.UUID;

/**
 * /chestshop (alias /cshop, /shop) — admin and player shop management commands.
 */
public class ShopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var node = Commands.literal("chestshop")
            .then(Commands.literal("list")
                .executes(ctx -> executeList(ctx.getSource(), null))
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(ctx -> executeList(ctx.getSource(),
                        StringArgumentType.getString(ctx, "player")))))
            .then(Commands.literal("info")
                .executes(ctx -> executeInfo(ctx.getSource())))
            .then(Commands.literal("convert")
                .executes(ctx -> executeConvert(ctx.getSource())))
            .then(Commands.literal("remove")
                .then(Commands.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                    .then(Commands.argument("y", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                        .then(Commands.argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                            .executes(ctx -> executeRemove(ctx.getSource(),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "x"),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "y"),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "z")))))))
            .then(Commands.literal("reload")
                .requires(src -> src.hasPermission(3) ||
                    (src.getEntity() != null &&
                     PermissionAPI.hasPermission(src.getEntity().getUUID(), "neoessentials.shop.admin.reload")))
                .executes(ctx -> executeReload(ctx.getSource())))
            .executes(ctx -> executeHelp(ctx.getSource()));

        dispatcher.register(node);

        // Aliases
        dispatcher.register(Commands.literal("cshop").redirect(dispatcher.getRoot().getChild("chestshop")));
    }

    // ── /chestshop list [player] ──────────────────────────────────────────────

    private static int executeList(CommandSourceStack src, String targetName) {
        try {
            UUID uuid;
            String displayName;

            if (targetName == null) {
                ServerPlayer self = src.getPlayerOrException();
                uuid = self.getUUID();
                displayName = self.getName().getString();
            } else {
                boolean canListOthers = src.hasPermission(3) ||
                    (src.getEntity() != null &&
                     PermissionAPI.hasPermission(src.getEntity().getUUID(), "neoessentials.shop.list.others"));
                if (!canListOthers) {
                    src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.shop.list_no_permission_others")));
                    return 0;
                }
                // Resolve UUID by online player name
                var server = src.getServer();
                ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
                if (target == null) {
                    src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.general.player_not_found", targetName)));
                    return 0;
                }
                uuid = target.getUUID();
                displayName = targetName;
            }

            List<ShopData> shops = ShopManager.getInstance().getShopsByOwner(uuid);
            src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.list_header", displayName, shops.size())), false);
            if (shops.isEmpty()) {
                src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.list_empty")), false);
            } else {
                String currency = EconomyManager.getInstance().getCurrencySymbol();
                for (ShopData s : shops) {
                    src.sendSuccess(() -> Component.literal(MessageUtil.localize(
                        "commands.neoessentials.shop.list_entry",
                        s.signDimension.replace("minecraft:", ""),
                        s.signX, s.signY, s.signZ,
                        s.quantity,
                        s.itemId.replace("minecraft:", ""),
                        s.buyPrice  != null ? currency + s.buyPrice.toPlainString()  : "§7—",
                        s.sellPrice != null ? currency + s.sellPrice.toPlainString() : "§7—"
                    )), false);
                }
            }
            return shops.size();
        } catch (Exception e) {
            src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.general.error", e.getMessage())));
            return 0;
        }
    }

    // ── /chestshop info ───────────────────────────────────────────────────────

    private static int executeInfo(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            HitResult hit = player.pick(5.0, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) {
                src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.shop.look_at_shop_sign")));
                return 0;
            }
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            ServerLevel level = player.serverLevel();
            String dimension = level.dimension().location().toString();

            ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
            if (shop == null) {
                src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.shop.no_shop_at_sign")));
                return 0;
            }

            String currency = EconomyManager.getInstance().getCurrencySymbol();
            src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.info_header")), false);
            src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.info_owner",
                shop.ownerName + (shop.isAdminShop() ? " §2[Admin]" : ""))), false);
            src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.info_item",
                shop.quantity, shop.itemId.replace("minecraft:", ""))), false);
            if (shop.buyPrice  != null) src.sendSuccess(() -> Component.literal(
                MessageUtil.localize("commands.neoessentials.shop.info_buy", currency + shop.buyPrice.toPlainString())), false);
            if (shop.sellPrice != null) src.sendSuccess(() -> Component.literal(
                MessageUtil.localize("commands.neoessentials.shop.info_sell", currency + shop.sellPrice.toPlainString())), false);
            src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.info_sign",
                shop.signX, shop.signY, shop.signZ)), false);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.general.error", e.getMessage())));
            return 0;
        }
    }

    // ── /chestshop convert ────────────────────────────────────────────────────
    /** Register the sign the player is looking at as a shop (for pre-existing signs). */
    private static int executeConvert(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.create")) {
                src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.general.no_permission")));
                return 0;
            }
            HitResult hit = player.pick(5.0, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) {
                src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.shop.look_at_sign")));
                return 0;
            }
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof SignBlockEntity sign)) {
                src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.shop.not_a_sign")));
                return 0;
            }
            String dimension = level.dimension().location().toString();
            String[] lines = ShopSignHandler.readSignLines(sign);
            ShopSignHandler.tryRegisterShop(player, lines, pos, dimension, level);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.general.error", e.getMessage())));
            return 0;
        }
    }

    // ── /chestshop remove <x> <y> <z> ────────────────────────────────────────

    private static int executeRemove(CommandSourceStack src, int x, int y, int z) {
        boolean isAdmin = src.hasPermission(3) ||
            (src.getEntity() != null &&
             PermissionAPI.hasPermission(src.getEntity().getUUID(), "neoessentials.shop.admin.remove"));
        if (!isAdmin) {
            src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.general.no_permission")));
            return 0;
        }
        try {
            ServerPlayer player = src.getPlayerOrException();
            String dimension = player.serverLevel().dimension().location().toString();
            BlockPos pos = new BlockPos(x, y, z);
            ShopData removed = ShopManager.getInstance().removeShop(dimension, pos);
            if (removed == null) {
                src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.shop.no_shop_found_at", x, y, z)));
                return 0;
            }
            src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.removed_detail", removed.ownerName, x, y, z)), true);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal(MessageUtil.localize("commands.neoessentials.general.error", e.getMessage())));
            return 0;
        }
    }

    // ── /chestshop reload ─────────────────────────────────────────────────────

    private static int executeReload(CommandSourceStack src) {
        ShopManager.getInstance().reload();
        src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.reloaded", ShopManager.getInstance().getShopCount())), true);
        return 1;
    }

    // ── /chestshop (help) ─────────────────────────────────────────────────────

    private static int executeHelp(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.help_header")), false);
        src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.help_list")), false);
        src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.help_info")), false);
        src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.help_convert")), false);
        src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.help_remove")), false);
        src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.help_reload")), false);
        src.sendSuccess(() -> Component.literal(MessageUtil.localize("commands.neoessentials.shop.help_signs")), false);
        return 1;
    }
}

