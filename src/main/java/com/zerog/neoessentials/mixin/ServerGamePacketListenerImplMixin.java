package com.zerog.neoessentials.mixin;

import com.zerog.neoessentials.api.ChatAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Suppresses Minecraft's hard-coded leave broadcast alongside {@link PlayerListMixin}. */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Redirect(
        method = "removePlayerFromWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
        )
    )
    private void neoessentials$broadcastLeaveMessage(PlayerList playerList, Component message, boolean overlay) {
        var chatManager = ChatAPI.getChatManager();
        if (chatManager == null || !chatManager.suppressVanillaJoinQuitMessages()) {
            playerList.broadcastSystemMessage(message, overlay);
        }
    }
}
