package com.zerog.neoessentials.shop.handlers;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.shop.ShopManager;
import com.zerog.neoessentials.shop.ShopParser;
import com.zerog.neoessentials.shop.ShopTransaction;
import com.zerog.neoessentials.shop.ShopTransaction.TransactionResult;
import com.zerog.neoessentials.shop.model.ShopData;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;


/**
 * Handles player interactions with ChestShop signs.
 * <ul>
 *   <li>Right-click sign → BUY</li>
 *   <li>Left-click sign  → SELL</li>
 *   <li>Break sign/chest → remove shop</li>
 * </ul>
 */
@EventBusSubscriber(modid = "neoessentials")
public class ShopInteractHandler {

    // ── Right-click = BUY ─────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity)) return;

        String dimension = level.dimension().location().toString();
        ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
        if (shop == null) return;

        event.setCanceled(true);

        // ── Item autofill: owner right-clicks a pending "?" shop with item in hand ──
        if (shop.itemPending) {
            if (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID())) {
                net.minecraft.world.item.ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (held.isEmpty()) {
                    player.sendSystemMessage(Component.literal(
                        MessageUtil.localize("commands.neoessentials.shop.autofill_hold_item")));
                } else {
                    // Assign the held item
                    shop.itemId      = com.zerog.neoessentials.economy.worth.WorthManager.getItemId(held);
                    shop.itemPending = false;
                    ShopManager.getInstance().registerShop(shop); // re-save with updated data
                    ShopSignHandler.writeSignLines(level, pos, ShopParser.formatSignLines(shop));
                    String currency = EconomyManager.getInstance().getCurrencySymbol();
                    player.sendSystemMessage(Component.literal(
                        MessageUtil.localize("commands.neoessentials.shop.item_set", ShopParser.buildItemDisplayName(shop.itemId))));
                    if (shop.buyPrice  != null) player.sendSystemMessage(Component.literal(
                        MessageUtil.localize("commands.neoessentials.shop.buy_price", currency + shop.buyPrice.toPlainString())));
                    if (shop.sellPrice != null) player.sendSystemMessage(Component.literal(
                        MessageUtil.localize("commands.neoessentials.shop.sell_price", currency + shop.sellPrice.toPlainString())));
                    player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.now_active")));
                }
            } else {
                player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.not_ready")));
            }
            return;
        }

        // ── Normal right-click = BUY ──────────────────────────────────────────
        // Owner right-clicks their own active sign → show info instead of buying
        if (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID())) {
            sendShopInfo(player, shop);
            return;
        }

        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.use")) {
            player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.no_permission_use")));
            return;
        }

        if (!shop.canBuy()) {
            player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.does_not_sell")));
            return;
        }

        TransactionResult result = ShopTransaction.executeBuy(player, shop, level);
        sendTransactionResult(player, result, shop, true);
    }

    // ── Left-click = SELL ─────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity)) return;

        String dimension = level.dimension().location().toString();
        ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
        if (shop == null) return;

        // Owner left-clicks → show info only
        if (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID())) {
            event.setCanceled(true);
            sendShopInfo(player, shop);
            return;
        }

        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.use")) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.no_permission_use")));
            return;
        }

        if (!shop.canSell()) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.does_not_buy")));
            return;
        }

        event.setCanceled(true);
        TransactionResult result = ShopTransaction.executeSell(player, shop, level);
        sendTransactionResult(player, result, shop, false);
    }

    // ── Block break → remove shop ─────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        String dimension = level.dimension().location().toString();

        // Check if it's a shop sign
        ShopData shop = ShopManager.getInstance().getShopBySign(dimension, pos);
        if (shop == null) {
            // Maybe it's the linked chest
            shop = ShopManager.getInstance().getShopByChest(dimension, pos);
        }
        if (shop == null) return;

        boolean isOwner = shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID());
        boolean isAdmin = PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.admin.remove");

        if (!isOwner && !isAdmin) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.cannot_break_other")));
            return;
        }

        ShopManager.getInstance().removeShop(dimension, shop.getSignPos());
        player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.removed")));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void sendTransactionResult(ServerPlayer player, TransactionResult result,
                                              ShopData shop, boolean buying) {
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        // Use buildItemDisplayName for readable modded item names (spaces, no namespace for vanilla)
        String itemDisplay = ShopParser.buildItemDisplayName(shop.itemId);
        switch (result.type) {
            case SUCCESS -> {
                if (buying) {
                    player.sendSystemMessage(Component.literal(MessageUtil.localize(
                        "commands.neoessentials.shop.bought",
                        result.quantity, itemDisplay,
                        currency, result.price.toPlainString(),
                        shop.ownerName)));
                } else {
                    player.sendSystemMessage(Component.literal(MessageUtil.localize(
                        "commands.neoessentials.shop.sold",
                        result.quantity, itemDisplay,
                        currency, result.price.toPlainString())));
                }
            }
            case NOT_ENOUGH_MONEY ->
                player.sendSystemMessage(Component.literal(buying
                    ? MessageUtil.localize("commands.neoessentials.shop.not_enough_money_buy")
                    : MessageUtil.localize("commands.neoessentials.shop.owner_cannot_afford")));
            case NOT_ENOUGH_STOCK ->
                player.sendSystemMessage(Component.literal(buying
                    ? MessageUtil.localize("commands.neoessentials.shop.out_of_stock")
                    : MessageUtil.localize("commands.neoessentials.shop.not_enough_item")));
            case NO_SPACE ->
                player.sendSystemMessage(Component.literal(buying
                    ? MessageUtil.localize("commands.neoessentials.shop.inventory_full")
                    : MessageUtil.localize("commands.neoessentials.shop.chest_full")));
            case NO_CHEST ->
                player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.no_linked_chest")));
            case SHOP_DISABLED ->
                player.sendSystemMessage(Component.literal(buying
                    ? MessageUtil.localize("commands.neoessentials.shop.disabled_no_sell")
                    : MessageUtil.localize("commands.neoessentials.shop.disabled_no_buy")));
            default ->
                player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.transaction_failed")));
        }
    }

    private static void sendShopInfo(ServerPlayer player, ShopData shop) {
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        String itemDisplay = ShopParser.buildItemDisplayName(shop.itemId);
        player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.info_header")));
        player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.info_owner", shop.ownerName)));
        player.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.shop.info_item", shop.quantity, itemDisplay)));
        if (shop.buyPrice  != null) player.sendSystemMessage(Component.literal(
            MessageUtil.localize("commands.neoessentials.shop.info_buy", currency + shop.buyPrice.toPlainString())));
        if (shop.sellPrice != null) player.sendSystemMessage(Component.literal(
            MessageUtil.localize("commands.neoessentials.shop.info_sell", currency + shop.sellPrice.toPlainString())));
        if (!shop.isAdminShop() && shop.hasChest) {
            // Show stock count
            player.sendSystemMessage(Component.literal(
                MessageUtil.localize("commands.neoessentials.shop.info_chest", shop.chestX, shop.chestY, shop.chestZ)));
        }
    }
}

