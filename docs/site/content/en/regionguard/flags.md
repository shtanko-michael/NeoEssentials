A flag is set with `/rg flag <id> <flag> <value>`. Boolean values accept `allow`, `deny`, `true` and `false`.

## Building and interaction

| Flag | Type | Default | What it controls |
|---|---|---:|---|
| `build` | bool | `true`* | Fallback value for build flags that are not set explicitly. |
| `block-break` | bool | `true` | Breaking blocks. |
| `block-place` | bool | `true` | Placing blocks, including a block from the item in hand. |
| `interact` | bool | `true` | Interaction with blocks and entities other than PvP: doors, buttons, armor stands, attacking entities and right-clicking a player. |
| `use` | bool | `true` | Using non-container blocks. |
| `chest-access` | bool | `true` | Opening vanilla and modded containers. |
| `interact-allow` | set | `[]` | Allow exceptions by block or entity ID. |
| `interact-deny` | set | `[]` | Deny exceptions by block or entity ID. |
| `chest-access-allow` | set | `[]` | Allow exceptions for containers. |
| `chest-access-deny` | set | `[]` | Deny exceptions for containers. |

`*` In an unclaimed region the permissive `build` default applies. In a region with an owner or member, build actions that are not set are denied to guests by default.

Owners and members bypass build flags regardless of their value.

## Players and entities

| Flag | Type | Default | What it controls |
|---|---|---:|---|
| `entry` | bool | `true` | A guest entering the region. When denied, the player is pushed back across the boundary. |
| `pvp` | bool | `true` | Damage to a player who is inside the region. |
| `mob-damage` | bool | `true` | Damage to a player from a mob, including indirect damage with a mob as the source. With `deny`, players in the region take none. |
| `invincibility` | bool | `false` | Full invulnerability for players inside the region. With `allow` / ON, all damage is cancelled, including PvP, mobs, falling and the environment. |
| `passive-entity-damage` | bool | `true` | Damage to passive creatures. |
| `fall-damage` | bool | `true` | Fall damage to players. |
| `item-drop` | bool | `true` | A player dropping items with Q or by dragging them out of the inventory. With `deny`, the item stays with the player and a localized message appears in the action bar. |
| `potion-splash` | bool | `true` | Splash and lingering potions bursting inside the region. With `deny`, the potion does not take effect, even if thrown from outside. |
| `mob-spawning` | bool | `true` | Natural mob spawning, except `/summon`. |
| `mob-entry` | bool | `true` | Mobs entering and staying in the region. |
| `mob-spawning-allow-only` | set | `[]` | If the list is not empty, allows spawning of the listed types only. |
| `mob-spawning-deny-only` | set | `[]` | Always denies spawning of the listed types. |

### PvP

`pvp deny` prevents dealing damage to any player who is in the region. The rule is symmetric: owners, members and guests cannot hit each other. The victim's position is checked, so a hit from outside into the region is blocked too.

The denial covers direct attacks and indirect damage whose source is a player, such as an arrow. Bypass is given by `regionguard.bypass.pvp`, `regionguard.admin.access` and OP level 2.

### Safe zone

`mob-damage deny` creates a PvE-safe zone: players inside take no damage whose source is a mob. `invincibility allow` (ON) is stronger — it cancels all damage to players in the region, with no exceptions for owners, members or OP. The flag does not make the player invisible and does not give the invisibility effect.

`item-drop deny` cancels the item drop, returns the already removed stack to the surviving player's inventory and shows the action bar message `protection.item_drop_deny`. `potion-splash deny` is checked at the impact point: you cannot throw a potion into the protected zone from outside, and a lingering cloud appearing there is cancelled too.

Example of a safe zone:

```text
/rg flag spawn mob-damage deny
/rg flag spawn item-drop deny
/rg flag spawn potion-splash deny
/rg flag spawn invincibility allow
```

## World and effects

| Flag | Type | Default | What it controls |
|---|---|---:|---|
| `fire-spread` | bool | `true` | Fire appearing and spreading. |
| `tnt` | bool | `true` | TNT explosions. |
| `creeper-explosion` | bool | `true` | Creeper explosions. |
| `other-explosion` | bool | `true` | All other explosions. |
| `weather` | string | `server` | Per-player weather: `server`, `sun`/`clear`, `rain`, `storm`/`thunder`. |
| `thunder` | bool | `true` | Lightning and thunderstorms. |
| `beacon` | set | `[]` | Effects in the `effect:level` format, maintained inside the region. |

Weather is sent to the player as packets and does not change the world's real weather. Beacon effects are refreshed once per second.

## Pixelmon

| Flag | Type | Default | What it controls |
|---|---|---:|---|
| `pixelmon-spawn` | bool | `true` | Wild Pokémon spawning. |
| `pixelmon-entry` | bool | `true` | Wild Pokémon entering and staying. |
| `pixelmon-player` | bool | `true` | Player-owned Pokémon entering. |

These flags work only with Pixelmon installed and `enablePixelmonCompat` enabled.

## Region messages

| Flag | Type | Default | What it controls |
|---|---|---:|---|
| `greeting` | string | `""` | Message on entry, shown in the action bar above the hotbar. |
| `farewell` | string | `""` | Message on exit, shown in the action bar above the hotbar. |
| `deny-message` | string | `""` | Message when an action is denied. |

Strings support `&` colour codes. An empty `deny-message` uses the mod's localized message.

For example:

```text
/rg flag spawn greeting &aWelcome to spawn!
/rg flag spawn farewell &eSee you soon!
```

## Flags of a new claim

On `/rg define` the mod explicitly writes two sets from the server config. By default `build`, `block-break`, `block-place`, `interact`, `chest-access`, `tnt` and `invincibility` are denied; all other boolean flags, including `mob-damage`, `item-drop` and `potion-splash`, are explicitly allowed.

`interact deny` protects doors, buttons and entity interactions from guests. Owners, members and OP level 2 bypass this build flag. For a specific region the value can be changed with `/rg flag <id> interact allow|deny`.
