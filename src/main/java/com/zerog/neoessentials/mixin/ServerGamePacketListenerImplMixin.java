package com.zerog.neoessentials.mixin;

import com.zerog.neoessentials.api.ChatAPI;
import com.zerog.neoessentials.commands.CommandPermissionRegistry;
import com.zerog.neoessentials.commands.CommandRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Locale;

/** Suppresses Minecraft's hard-coded leave broadcast alongside {@link PlayerListMixin}. */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    /**
     * Brigadier matches literal nodes case-sensitively. Normalize only the root of a
     * NeoEssentials command before the server parses it, leaving every argument
     * untouched (notably player names and message text).
     *
     * <p>This hook is shared by signed and unsigned player command packets, unlike
     * a command event which runs only after Brigadier has already parsed the input.</p>
     */
    @ModifyVariable(method = "parseCommand", at = @At("HEAD"), argsOnly = true)
    private String neoessentials$normalizeCommandRootCase(String command) {
        if (command == null || command.isEmpty()) {
            return command;
        }

        int rootStart = command.charAt(0) == '/' ? 1 : 0;
        int rootEnd = rootStart;
        while (rootEnd < command.length() && !Character.isWhitespace(command.charAt(rootEnd))) {
            rootEnd++;
        }

        if (rootStart == rootEnd) {
            return command;
        }

        String root = command.substring(rootStart, rootEnd);
        String normalizedRoot = root.toLowerCase(Locale.ROOT);
        if (root.equals(normalizedRoot) || !isNeoEssentialsCommand(normalizedRoot)) {
            return command;
        }

        return command.substring(0, rootStart) + normalizedRoot + command.substring(rootEnd);
    }

    private static boolean isNeoEssentialsCommand(String commandName) {
        // The two root commands are dispatcher-only. CommandPermissionRegistry is
        // generated from every registered NeoEssentials command and its aliases;
        // CommandRegistry covers any runtime-only registrations not in that reference.
        return commandName.equals("neoe")
            || commandName.equals("neoessentials")
            || CommandPermissionRegistry.getInstance().isKnown(commandName)
            || CommandRegistry.getInstance().isCommandRegistered(commandName);
    }

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
