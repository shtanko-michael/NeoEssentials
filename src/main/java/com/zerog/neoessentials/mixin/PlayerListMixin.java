package com.zerog.neoessentials.mixin;

import com.zerog.neoessentials.api.ChatAPI;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Suppresses Minecraft's hard-coded join broadcast when NeoEssentials owns it. */
@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Redirect(
        method = "placeNewPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
        )
    )
    private void neoessentials$broadcastJoinMessage(PlayerList playerList, Component message, boolean overlay) {
        if (!isVanillaJoinQuitMessageSuppressed()) {
            playerList.broadcastSystemMessage(message, overlay);
        }
    }

    private static boolean isVanillaJoinQuitMessageSuppressed() {
        var chatManager = ChatAPI.getChatManager();
        return chatManager != null && chatManager.suppressVanillaJoinQuitMessages();
    }
}
