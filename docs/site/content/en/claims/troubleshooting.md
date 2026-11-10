## `/rg define` does not see the selection

Install a compatible WorldEdit build and repeat `//pos1` and `//pos2` in the same dimension. The internal fallback store exists, but the current version of Farmstead Claims has no command or tool that writes points into it.

## The player has the subcommand permission, but the command is unavailable

Check the base node `regionguard.command`. For example, `regionguard.command.list` alone is not enough — both nodes are needed.

If you use LuckPerms, NeoEssentials must be installed: its PermissionAPI is what connects Farmstead Claims to the external adapter. Without the bridge a non-OP player is denied.

The `regionguard.admin.*` nodes work differently: they do not need a separate base `regionguard.command`. Check that the node granted is exactly the one for the operation you need, for example `regionguard.admin.members` for region membership or `regionguard.admin.access` for entry and building.

## The owner can change a flag, but a member cannot

This is the intended split of roles. A member bypasses build protection, but only an owner, OP level 2, or a moderator with `regionguard.admin.members` / `regionguard.admin.flags` can manage membership and flags.

To change a specific flag, the owner also needs:

```text
regionguard.command
regionguard.command.flag
regionguard.command.flag.<flag-id>
```

## The flag panel looks empty

`/rg flag <id> list` shows only the boolean flags for which the `regionguard.command.flag.<flag-id>` node is granted. Grant the specific nodes or `regionguard.command.flag.*`.

String and set flags are deliberately not shown in the panel. Use the direct command `/rg flag <id> <flag> <value>`.

## Guests open doors, touch armor stands or another player's worn backpack

New claims are created with `interact deny`, so guests cannot interact with blocks and entities. This covers doors, buttons, armor stands, attacking entities and the standard right-click on a player.

Close an existing region created before this setting with:

```text
/rg flag home interact deny
```

If the server has already created `config/regionguard-server.toml` with the old lists, add `interact` to `defaultDeniedFlagsOnClaim` and remove it from `defaultAllowedFlagsOnClaim`, then restart the server. The change affects only future claims.

Sophisticated Backpacks also has its own server setting `allowOpeningOtherPlayerBackpacks`. Set it to `false` if opening other players' worn backpacks should be disabled across the whole server, including unclaimed land.

## `item-drop deny` shows a message, but the item does not come back

Update Farmstead Claims to the current JAR and restart the server. The flag cancels the drop, then returns the stack on the next server tick; test in survival, not creative. In creative the item is not consumed when dropped in the first place, so that mode is no good for testing the return.

## `pvp deny` blocks only one side

In the current version this is considered a bug. `pvp deny` is checked at the victim's position and must symmetrically block attacks from the owner, members and guests, including a player's arrows.

If one player still deals damage, check their OP level, `regionguard.bypass.pvp` and `regionguard.admin.access`. Also make sure the server actually loaded the new JAR and there is no old `regionguard-*.jar` in `mods`.

## A flag has no effect where regions overlap

Check `/rg info` and the priorities in JSON. At each point the region with the highest priority is chosen; flags of overlapping regions do not add up.

## A language file change did not apply

Restart the server or reload the NeoForge config. Check that you are editing the file for the selected language in `config/RegionGuard/lang/`, and that `language` in `regionguard-server.toml` is `ru_ru` or `en_us`.

## Pixelmon flags change nothing

Make sure Pixelmon is actually loaded and `enablePixelmonCompat = true`. After changing this setting it is safer to restart the server so the integration bridge and handlers load in the expected state.
