## Installation

Farmstead NeoEssentials runs **on the server only** — players do not need to install anything; a vanilla
client connects as usual.

1. Put `neoessentials-<version>.jar` into the server's `mods/` folder.
2. Start the server once and stop it. The mod creates `config/neoessentials/` with all
   the config and data files.
3. Open `config/neoessentials/config.json` and go through the `modules` section — that is where
   whole subsystems are turned on and off.

> The switches in `modules` and `commands` are read **once at startup**: a command is either
> registered or not. `/neoessentials reload` refreshes only setting values; it cannot
> add or remove a command that is already registered. After editing these two sections
> you need a server restart.

## Player name completion

In commands that take a player name, press <kbd>Tab</kbd> to pick a player from the online list. This works, among others, for private messages (`/me <player> <message>`, `/msg`, `/pm`), teleportation, moderation, mail, homes, shops and service commands.

Suggestions include only online players. If a command supports offline targets, such as `/seen`, `/tpoffline` or punishment history, you can still type a known offline player's name by hand.

## Server language

Out of the box the server replies in English (`"language": ""` means `en_us`). The language is set by a single key in `config.json`:

```json
"localization": {
  "language": "ru_ru"
}
```

After `/neoessentials reload` the server starts replying in Russian. Untranslated strings
automatically fall back to English, so a wrong language code cannot break output.
Translation coverage per language is on the [Localization](../localization/index.html) page.

## Granting the first permissions

By default almost everything is closed: `permissions.requireExplicitGrant: true` — a node works
only after an explicit grant (LuckPerms or the internal manager). `defaultValue=true` in
the registry does not grant access by itself. OP still passes with `opsBypassPermissions: true`.

A base set for a regular player and for a moderator looks like this (LuckPerms example):

```
/lp group default permission set neoessentials.use true
/lp group default permission set neoessentials.teleport.home true
/lp group default permission set neoessentials.teleport.home.set true
/lp group default permission set neoessentials.teleport.spawn true
/lp group default permission set neoessentials.economy.balance true
/lp group default permission set neoessentials.ah true
/lp group default permission set neoessentials.ah.sell true
/lp group default permission set neoessentials.chestshop true
/lp group default permission set neoessentials.chat.msg true
/lp group default permission set neoessentials.votifier.vote true

/lp group moderator permission set neoessentials.chat.mute true
/lp group moderator permission set neoessentials.moderation.kick true
/lp group moderator permission set neoessentials.moderation.jail true
/lp group helper permission set neoessentials.moderation.stafflog true
/lp group moderator permission set neoessentials.moderation.stafflog true
/lp group headmoderator permission set neoessentials.moderation.stafflog true
/lp group admin permission set neoessentials.moderation.stafflog true

/lp group admin permission set neoessentials.* true
```

The full list of nodes with descriptions is on the [Permissions](../permissions/index.html) page. Which permission
a specific command checks is shown in the "Permission" column on the
[All commands](../commands/index.html) page, and in game via `/help <command>`.

> `neoessentials.admin` and the wildcard `neoessentials.*` are not the same thing. The wildcard deliberately does **not**
> grant the graduated economy modifier nodes unless you enable
> `permissions.wildcardsGrantEconomyModifierPermissions`. This protects against accidentally granting the right
> to change other players' balances together with general access.

## Checking that everything is alive

| Command | What it confirms |
|---|---|
| `/neoessentials` | the mod loaded; shows the version and build number |
| `/help` | the command registry is built; shows only what is available to you |
| `/neoessentials reload` | configs are read without errors |
| `/balance` | the economy is enabled and initialized |

If a command is in this reference but does not work in game, first check whether its
subsystem is enabled in `modules`, then whether the command itself is disabled in the `commands` section of the config.

## Where to go next

The per-system sections in the menu on the left all follow the same layout: commands and working examples at the top, then
caveats, then known issues, and at the very end the original project's detailed guide
in English.
