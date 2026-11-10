All commands start with `/rg`. A non-operator usually needs the base node `regionguard.command` together with the node of the specific subcommand. The `regionguard.admin.*` moderation nodes do not need it: each one opens only its own operation.

## Sending a guest back to spawn

`/bb <player>` is available to an owner or member of the claim they are currently standing in, and to a moderator with `regionguard.admin.access`. The named online player must also be in the same claim. If they are neither an owner nor a member of that claim, Farmstead Claims immediately sends them to the configured server spawn and shows them the message "You were removed from the region." The usual warmup and `/spawn` messages are not used. For an owner or member of the claim the command does nothing. In the NeoEssentials `/help` catalogue it is listed with the description "Return a guest of the current claim to spawn".

## Managing regions

| Command | Purpose |
|---|---|
| `/rg define <id>` | Create a region from the current selection. The player becomes the owner. Alias: `/rg claim <id>`. |
| `/rg redefine <id>` | Move an existing region's bounds to a new selection, keeping owners, members and flags. |
| `/rg remove <id>` | Delete a region. Alias: `/rg delete <id>`. |
| `/rg list` | Show the regions in the current dimension that the player owns. OP, the console and `regionguard.admin.list` see all regions. |
| `/rg info [id]` | Without `id`, open the detailed card of the region the player is standing in; with `id`, open the card of that region from anywhere. The card shows owners, members, available flags, the number of claimed blocks and the coordinates. The `[Flags]` button is highlighted in purple. |
| `/rg size [id]` | Show the line `Claimed blocks: <number>` for the region under the player; with `id`, for that region. |
| `/rg tp <id>` | Teleport safely to the centre of the region. |
| `/rg s [id]` | Show the region's bounds as a WorldEdit grid. Without `id`, shows the region under the player. |

`define`, `redefine` and `remove` are not available for the system region `__global__`. The size of a new selection is checked before it is written: the sum of the X, Y and Z lengths must not exceed 384 blocks.

Without an argument, `/rg info` and `/rg size` pick the region with the highest priority at the player's position. Outside a regular claim (including when only in `__global__`) they report that there is no claim at that point. `/rg list` does not depend on position: a regular player sees only regions in the current dimension that they own; OP, the console and `regionguard.admin.list` see all regions.

For `/rg info <id>` and `/rg size <id>`, a regular player can name only a region where they are an owner or member. The same regions are offered in command suggestions; typing another player's ID directly gets the same reply as an unknown region and does not reveal that it exists. OP and a player with `regionguard.admin.info` can view any region.

## Viewing bounds and quick checks

`/rg s [id]` restores the claim's saved bounds as a cuboid WorldEdit selection. It can be used by an owner, a member, or a moderator with `regionguard.admin.info`; for another player's region you must give the `id` explicitly. The command replaces the current WorldEdit selection, and `//sel` clears the grid.

Right-clicking a block with a plain stick shows brief info about the claim at the clicked point. An owner sees that the region belongs to them. A moderator with `regionguard.admin.info` sees the region ID, owners and members. Farmstead Claims does not reveal this information to outsiders.

## Owners and members

| Command | Purpose |
|---|---|
| `/rg addowner <id> <player>` | Add an owner by name, UUID or the profile cache. |
| `/rg delowner <id> <player>` | Remove an owner. This command cannot remove the last owner. |
| `/rg clearowners <id>` | Remove all owners. |
| `/rg addmember <id> <player>` | Add a member. |
| `/rg delmember <id> <player>` | Remove a member. |
| `/rg clearmembers <id>` | Remove all members. |

Only the region's owner, OP level 2, or a player with `regionguard.admin.members` can change its membership. A member can use the claimed area but cannot manage it.

## Flags

| Command | Purpose |
|---|---|
| `/rg flag <id> list` | Open the interactive panel of available boolean flags. |
| `/rg flag <id> <flag> <value>` | Set a flag's value. |
| `/rg flag <id> clear <flag>` | Reset a flag entirely to its default value. |
| `/rg flag <id> clear <flag> <value>` | Remove one value from a set flag. |

The `list` panel shows only boolean flags for which `regionguard.command.flag.<flag-id>` is granted. A player with `regionguard.admin.flags` sees and changes all flags. String and set flags are set with the direct command.

## Values of complex flags

`mobspawning` is a command alias of the `mob-spawning` flag with a special syntax:

```text
/rg flag <id> mobspawning allow-only <entity_id>
/rg flag <id> mobspawning deny-only <entity_id>
/rg flag <id> mobspawning allow <entity_id>
/rg flag <id> mobspawning deny <entity_id>
```

A beacon effect is added as an effect and a level from 1 to 5:

```text
/rg flag spawn beacon speed 3
/rg flag spawn beacon resistance 2
```

For `interact-allow`, `interact-deny`, `chest-access-allow` and `chest-access-deny`, pass a block or entity ID, for example `minecraft:chest` or `minecraft:villager`.

## Autocompletion

Region and flag arguments support Brigadier suggestions. Values get matching options: `allow`/`deny`, the weather mode, beacon effects and known identifiers.
