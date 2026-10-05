package com.zerog.neoessentials.tablist;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API for other mods (Farmstead): attach a tab-list row to another player's row. An attached player — e.g. a player's
 * AI companion — is listed right under its parent, indented, with its own prefix instead of a permission prefix; that
 * prefix is also its above-head nametag prefix (one scoreboard team drives both). With the parent offline the row
 * sorts like anyone else's, still with that prefix.
 *
 * <p>Plain static calls; a mod that may run without NeoEssentials checks {@code ModList.isLoaded("neoessentials")}
 * before touching this class.
 */
public final class TabAnchors {
    /** {@code prefix} uses NeoEssentials text formatting (&amp; colour codes and tags), e.g. {@code "&b[ИИ]&r "}. */
    public record Anchor(UUID parent, String prefix) {}

    /** What goes in front of an attached row in the tab list: the indent that shows it belongs to the row above. */
    public static final String INDENT = "  ↳ ";

    private static final Map<UUID, Anchor> ANCHORS = new ConcurrentHashMap<>();

    private TabAnchors() {}

    public static void attach(UUID child, UUID parent, String prefix) {
        ANCHORS.put(child, new Anchor(parent, prefix == null ? "" : prefix));
    }

    public static void detach(UUID child) {
        ANCHORS.remove(child);
    }

    public static Anchor of(UUID child) {
        return ANCHORS.get(child);
    }

    /** Whether any player attached to {@code parent} is online now. */
    static boolean hasChildOnline(UUID parent, MinecraftServer server) {
        for (Map.Entry<UUID, Anchor> e : ANCHORS.entrySet()) {
            if (e.getValue().parent().equals(parent) && server.getPlayerList().getPlayer(e.getKey()) != null) return true;
        }
        return false;
    }

    /**
     * The scoreboard team that sorts a parent ({@code role} '0') and its children ('1') next to each other: the parent's
     * group weight, then its name, then the role — "ne_" + 4 + 8 + 1 = the 16 characters a team name may have.
     * Clients order the tab list by team name, so the child lands right after the parent's row.
     */
    static String teamName(int sortKey, ServerPlayer parent, char role) {
        StringBuilder key = new StringBuilder();
        for (char c : parent.getGameProfile().getName().toLowerCase(java.util.Locale.ROOT).toCharArray()) {
            if (key.length() == 8) break;
            key.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ? c : '_');
        }
        while (key.length() < 8) key.append('_');
        return String.format(java.util.Locale.ROOT, "ne_%04d%s%c", Math.max(0, Math.min(9999, sortKey)), key, role);
    }
}
