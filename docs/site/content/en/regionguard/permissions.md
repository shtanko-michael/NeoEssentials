Command permissions are checked through the NeoEssentials `PermissionAPI`, which passes the request to the installed adapter, usually LuckPerms. Without NeoEssentials or a permission plugin, management stays available to OP level 2 and above.

## Command permissions

| Node | Access |
|---|---|
| `regionguard.command` | Required root access to all `/rg` subcommands. |
| `regionguard.command.define` | `/rg define` and `/rg claim`. |
| `regionguard.command.redefine` | `/rg redefine`. |
| `regionguard.command.remove` | `/rg remove` and `/rg delete`. |
| `regionguard.command.list` | `/rg list`. |
| `regionguard.command.info` | `/rg info` and `/rg size` for your own regions and regions where you are a member. |
| `regionguard.command.tp` | `/rg tp` and the teleport button in lists. |
| `regionguard.command.members` | Adding, removing and clearing owners and members. |
| `regionguard.command.flag` | Entry to the `/rg flag` branch. |
| `regionguard.command.flag.<flag-id>` | Viewing and changing a specific flag. |

A regular player needs two layers at once: `regionguard.command` and the node of the subcommand. A flag needs a third layer — `regionguard.command.flag.<flag-id>`.

The safety and message flags use these exact nodes:

| Node | Flag |
|---|---|
| `regionguard.command.flag.mob-damage` | `mob-damage` |
| `regionguard.command.flag.invincibility` | `invincibility` |
| `regionguard.command.flag.passive-entity-damage` | `passive-entity-damage` |
| `regionguard.command.flag.fall-damage` | `fall-damage` |
| `regionguard.command.flag.item-drop` | `item-drop` |
| `regionguard.command.flag.potion-splash` | `potion-splash` |
| `regionguard.command.flag.greeting` | `greeting` |
| `regionguard.command.flag.farewell` | `farewell` |

## Recommended set for `default`

A region owner still cannot change someone else's region. For a regular role it makes sense to grant the base commands and the safe flags of their own claim, but not `invincibility`, mob spawning control, beacon or explosions:

```text
regionguard.command
regionguard.command.define
regionguard.command.redefine
regionguard.command.remove
regionguard.command.list
regionguard.command.info
regionguard.command.tp
regionguard.command.members
regionguard.command.flag
regionguard.command.flag.greeting
regionguard.command.flag.farewell
regionguard.command.flag.deny-message
regionguard.command.flag.weather
regionguard.command.flag.entry
regionguard.command.flag.interact
regionguard.command.flag.use
regionguard.command.flag.chest-access
regionguard.command.flag.pvp
regionguard.command.flag.passive-entity-damage
regionguard.command.flag.fall-damage
regionguard.command.flag.item-drop
regionguard.command.flag.potion-splash
```

A minimal set for viewing your own regions:

```text
regionguard.command
regionguard.command.list
regionguard.command.info
```

Full control over all flags:

```text
regionguard.command
regionguard.command.flag
regionguard.command.flag.*
```

`regionguard.command.*` does not include the `regionguard.command` node itself. If you need a full wildcard, use `regionguard.*` or grant the root node separately.

## Ownership matters more than the command node

A command node allows calling the command but does not give control over someone else's region. Only the owner or OP level 2 can change bounds, delete the region, or edit members and flags. The exception is a specially granted `regionguard.admin.*` node from the table below.

A member bypasses build protection but gets no management rights. The last owner cannot be removed with `/rg delowner`; delete the region deliberately or use `/rg clearowners`.

## Moderation without `/op`

The nodes in this section work only in RegionGuard: they do not grant vanilla operator commands and do not give access to other mods. Each node opens its own `/rg` branch, so `regionguard.command` and the regular command node are not needed alongside it.

| Node | Access to other players' claims |
|---|---|
| `regionguard.admin.access` | Entry, building, interactions and containers; bypass of `item-drop`, `potion-splash`, passive creature protection and `pvp deny`; `/bb` in someone else's claim. Environment rules — explosions, weather, spawning and the like — are not bypassed. |
| `regionguard.admin.members` | Add and remove owners/members, clear both lists. |
| `regionguard.admin.flags` | View and change all flags without separate `regionguard.command.flag.<flag-id>` nodes. |
| `regionguard.admin.remove` | Delete regions. |
| `regionguard.admin.redefine` | Change region bounds with `/rg redefine`. |
| `regionguard.admin.list` | See all regions in the current dimension via `/rg list`. |
| `regionguard.admin.info` | View the card of any region via `/rg info <id>`, show its grid via `/rg s <id>` and see owners/members with the stick. |
| `regionguard.admin.tp` | Teleport to any region and see the teleport buttons. |

The full set for a moderator group:

```text
/lp group moderators permission set regionguard.admin.* true
```

## Bypass permissions

Declared in the registry:

```text
regionguard.bypass.build
regionguard.bypass.interact
regionguard.bypass.entry
regionguard.bypass.pvp
regionguard.bypass.mob-spawning
regionguard.bypass.mob-entry
```

In the current version only `regionguard.bypass.pvp` is wired into the protection checks. It lets the attacker ignore `pvp deny`. The other nodes are kept but do not change protection behaviour yet: the corresponding bypasses are determined by owner or member status. For moderation without OP, use `regionguard.admin.access`, not these placeholder bypass nodes.

## Claim limit

`regionguard.regions` is a LuckPerms meta key, not a permission node. It sets the maximum number of regions a player can have in total across all dimensions:

```text
/lp group default meta set regionguard.regions 3
/lp group vip meta set regionguard.regions 10
/lp user Steve meta set regionguard.regions 25
```

If the meta is absent, `maxRegionsPerPlayer` from `regionguard-server.toml` is used. A value of `-1` removes the limit. Existing regions are not deleted when the limit is lowered.

## OP and the console

OP level 2 and above bypasses command checks, ownership restrictions and the region limit. Such an OP can also enter any region and bypasses denials on building, interactions, containers, damage to passive creatures, item drops and potions. For PvP, such an OP also bypasses `pvp deny`. Environment rules and zone protection effects (`invincibility`, `fall-damage`, `mob-damage`, explosions and so on) keep applying. The console and command blocks can run suitable commands, but commands that need a player object or their selection require an in-game context.
