package com.zerog.neoessentials.util;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Locale;

/**
 * Farmstead fork: messages in the language of the player who reads them.
 *
 * <p>NeoEssentials is server-only — clients never have its language files — so it resolves every message
 * to text on the server, and upstream picks ONE language for everybody ({@code localization.language}).
 * A server with Russian and English players had to choose whom to leave unable to read.</p>
 *
 * <p>Messages are built deep inside command handlers with no recipient in sight
 * ({@code MessageUtil.localize(key, args)} — hundreds of call sites), so the reader is not passed down: it
 * is remembered for the duration of the work done on their behalf. A player's command and a player's
 * login set the reader before any handler runs (lowest priority, after other mods could cancel), and the
 * end of the server tick forgets it. {@link MessageUtil} then answers in that player's client language.</p>
 *
 * <p>The cost of not threading a recipient through every call: a message one player's command sends to
 * ANOTHER player (a /tpa request, a /pay notice) is written in the sender's language, and a message sent
 * later from a scheduled task (a delayed teleport) in the server's. Both are honest fallbacks — every
 * one of them was in the server's language before.</p>
 *
 * <p>Off with {@code localization.perPlayerLanguage: false}: then everything is in the server language,
 * as upstream.</p>
 */
@EventBusSubscriber(modid = "neoessentials")
public final class ReaderLanguage {

    private static final ThreadLocal<String> READER = new ThreadLocal<>();

    private ReaderLanguage() {}

    /** The language code of the player this work is for, or null — then the server language applies. */
    static String current() {
        return READER.get();
    }

    /** Reads the player's client language from now until the end of the tick (or {@link #clear()}). */
    public static void set(ServerPlayer player) {
        String language = languageOf(player);
        if (language == null) {
            READER.remove();
        } else {
            READER.set(language);
        }
    }

    public static void clear() {
        READER.remove();
    }

    /**
     * The bundled language a client language maps to: the same code when the jar has it (de_de, fr_fr…),
     * Russian for Ukrainian, Belarusian and Kazakh clients (they read the Russian server far better than
     * English), English otherwise. Null when the feature is off or the client has not told us.
     */
    static String languageOf(ServerPlayer player) {
        if (player == null || !com.zerog.neoessentials.config.ConfigManager.isPerPlayerLanguageEnabled()) return null;
        String client;
        try {
            client = player.clientInformation().language();
        } catch (RuntimeException e) {
            return null;
        }
        if (client == null || client.isBlank()) return null;
        String code = client.trim().toLowerCase(Locale.ROOT);
        if (MessageUtil.hasBundledLanguage(code)) return code;
        if (code.startsWith("ru") || code.startsWith("uk") || code.startsWith("be") || code.startsWith("kk")) {
            return "ru_ru";
        }
        if (code.startsWith("pt")) return "pt_br";
        if (code.startsWith("zh")) return "zh_cn";
        for (String bundled : new String[] {"de_de", "es_es", "fr_fr", "nl_nl", "pl_pl"}) {
            if (code.startsWith(bundled.substring(0, 2))) return bundled;
        }
        return "en_us";
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCommand(CommandEvent event) {
        if (event.isCanceled()) return;
        if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player) {
            set(player);
        } else {
            clear();   // the console or a command block: the server language
        }
    }

    /** Welcome lines, MOTD and the rest of what a joining player is told. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) set(player);
    }

    @SubscribeEvent
    public static void onTickEnd(ServerTickEvent.Post event) {
        clear();
    }
}
