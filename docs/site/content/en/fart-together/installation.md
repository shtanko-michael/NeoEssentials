## Downloads and versions

| Minecraft | Fabric / Quilt | NeoForge | Forge | Java |
| --- | --- | --- | --- | --- |
| 26.1 – 26.3 | yes | yes | — | 25 |
| 1.21.7 – 1.21.11 | yes | yes | — | 21 |
| 1.21 – 1.21.5 | yes | yes | — | 21 |
| 1.20.1 | yes | — | yes | 17 |

1.21.6 is not covered.

Get it from [Modrinth](https://modrinth.com/mod/farmstead-fart-together), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/farmstead-fart-together) or [GitHub Releases](https://github.com/shtanko-michael/farmstead-fart-together/releases).

**Requirements:** nothing on NeoForge and Forge. **On Fabric the mod needs [Fabric API](https://modrinth.com/mod/fabric-api).** [Mod Menu](https://modrinth.com/mod/modmenu) is optional and adds a settings button on Fabric.

**Server or client?** Both. Install it on the server and on every client. A client on a server without the mod still toots, but only for itself. Players without the mod see nothing of it; they can still log in, but the beans and cabbage are content their game does not know: a server with vanilla players can set `items.enabled = false` (restart) and the mod adds nothing to the registries.

Chest loot is not added on 26.3 yet (26.3 rewrote the loot builders). On 26.x villager trades come from data and cannot be switched off with `items.beansInVillagerTrades`; a datapack that empties `data/minecraft/tags/villager_trade/farmer/level_1.json` and `level_2.json` entries does.

## Update check

Once per start the mod asks Modrinth whether a newer version exists and writes one line to the log (the About tab of the settings shows it too). Nothing else is sent. Turn it off with `general.checkUpdates = false` or `-Dfarmstead.updateCheck=false` (servers).
