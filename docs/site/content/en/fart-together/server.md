## Server file

`config/farmstead_fart_together-server.toml`, presets `default`, `family` (longer cooldown, no effects), `plain` (no personal styles), `cosmetic` (pure decoration: no gas needed, mobs are not scared, no cloud effects, no jet push, no combo glow; the toot only looks and sounds), `chaos` (fast toots, big clouds with nausea that reach other players, stronger jet jumps, a wider combo window).

| Section | Keys |
| --- | --- |
| `toot` | `cooldownSeconds` (3), `fullChargeSeconds` (1.5: how long a hold takes to let out all the gas) |
| `charge` | `required`, `max` (100), `minToToot` (10: also what a tap lets out), `passivePerMinute`, `unlistedFood`, `goldenApple` |
| `cloud` | `enabled`, `durationSeconds`, `radius`, `scareMobs`, `scareRange`, `scaredMobs` (ids or `#tags`), `effects` (`"minecraft:nausea 4 1"`), `affectsPlayers` (off: no PvP), `respectServerPvp` |
| `jetJump` | `enabled`, `strength` (0.6), `slowFallSeconds` (1.5) |
| `together` | `enabled`, `windowSeconds` (1.5), `radius` (6), `glow` |
| `items` | `enabled` (dedicated servers, restart), `beansFromGrass`, `cabbageSeedsFromGrass`, `beansInVillagerTrades`, `beansInLoot` |
| `style` | `allowCustom`, `maxSize`, `maxAmount`, `maxDuration`, `allowedParticles`, `allowedSounds`, `updateCooldownSeconds` |

Gas weights live in `data/<namespace>/gas_foods/*.json`, for example `{"foods": {"minecraft:beetroot": 10, "#c:foods/vegetable": 4}}`. A datapack changes ours by shipping the same path (`data/farmstead_fart_together/gas_foods/vanilla.json`) or adds its own file; `/reload` or `/fart reload` applies it.

## Permissions

LuckPerms or any permission mod of the loader. Checked live with LuckPerms on Fabric and NeoForge. On NeoForge 1.21.1 use LuckPerms 5.4.150 or newer from [luckperms.net](https://luckperms.net/download): 5.4.140, the newest NeoForge build on Modrinth, kicks every player on join ("Invalid player data") (a LuckPerms bug, fixed in later builds).

| Node | Default | What it allows |
| --- | --- | --- |
| `farmstead_fart_together.use` | everyone | toot |
| `farmstead_fart_together.jetjump` | everyone | a toot in mid-air pushes you up |
| `farmstead_fart_together.together` | everyone | count in Fart Together combos |
| `farmstead_fart_together.style.custom` | everyone | a personal style (otherwise the plain green puff) |
| `farmstead_fart_together.style.rainbow` | nobody | gradient and rainbow colours |
| `farmstead_fart_together.style.huge` | nobody | size and duration caps x1.5 |
| `farmstead_fart_together.style.sounds.rare` | nobody | rare sounds by name (none yet) |
| `farmstead_fart_together.cloud.bypass` | nobody | cloud effects never reach this player |
| `farmstead_fart_together.command.info` | everyone | `/fart info` |
| `farmstead_fart_together.command.reload` | operators | `/fart reload` |
| `farmstead_fart_together.command.charge` | operators | `/fart charge` |
| `farmstead_fart_together.command.toggle` | operators | `/fart toggle` |

| Meta (number) | Default | What it sets |
| --- | --- | --- |
| `farmstead_fart_together.cooldown` | `toot.cooldownSeconds` | seconds between toots |
| `farmstead_fart_together.cloud.radius` | `cloud.radius` | radius of a full-strength cloud |
| `farmstead_fart_together.charge.max` | `charge.max` | size of the gas meter |

Example: `/lp group vip meta set farmstead_fart_together.cooldown 1`. On NeoForge and Forge the meta is read through the loader's permission API (string nodes), on Fabric through fabric-permissions-api's options.
