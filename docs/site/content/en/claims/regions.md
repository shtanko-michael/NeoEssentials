## Geometry and dimensions

A region is a rectangular box with inclusive minimum and maximum coordinates. The ID is unique within a dimension; the same name can exist in different worlds as an independent region. The number of claimed blocks is calculated as `(maxX − minX + 1) × (maxY − minY + 1) × (maxZ − minZ + 1)` and shown in `/rg info` and `/rg size`.

For `/rg define` and `/rg redefine`, the sum of the X, Y and Z sizes must not exceed 384 blocks. For example, `128 × 128 × 128` is allowed, while `129 × 128 × 128` already exceeds the limit.

## Overlaps and priority

If a point belongs to several regions, the single region with the highest numeric priority applies. There is no command to change priority yet; the field is edited in JSON while the server is stopped.

Flags of different overlapping regions are not merged. Once a region is chosen, only its value is checked, so a nested zone with a higher priority fully determines behaviour within its area.

## Global region

`__global__` is created automatically in every dimension and covers the world's valid range. Its priority is `Integer.MIN_VALUE`, so any regular region sits above it.

Commands do not allow removing or redefining `__global__`. It is not shown as an owner's regular claim and is used for the base rules outside created regions.

## Owners

The player who creates a region automatically becomes its owner. An owner can:

- redefine the bounds and remove the region;
- add and remove owners and members;
- view and change the flags they are allowed to;
- build and interact, bypassing build flags.

Changing management data still requires the matching command permission node. Ownership does not replace `regionguard.command.*`.

## Members

A member is treated as a trusted user of the area and bypasses `build`, `block-break`, `block-place`, `interact`, `use`, `chest-access`, `entry` and `passive-entity-damage`.

A member cannot change the region's bounds, flags or membership and does not bypass `pvp deny`. The `/rg info` card hides the management buttons and the flag summary from members.

## Guests

All flags of the chosen region apply to a guest. If a build flag was not set explicitly in a claimed region, the action is denied by default.

`entry deny` pushes the guest back across the region boundary. The denial is shown on the action bar so it does not flood chat with a message every tick.

## OP access

OP level 2 can enter any region, build, interact with blocks and containers, drop items and use potions, even without being added as an owner or member. The PvP denial is already bypassed for OP. Environment rules and zone safety effects — for example `invincibility`, `fall-damage`, `mob-damage` and explosions — stay active for OP too.

## Moderator access without OP

`regionguard.admin.access` gives the same access to player actions inside any claim, including bypassing `pvp deny`, but does not open vanilla commands and does not disable environment rules. To manage other players' regions, separate nodes are granted: `regionguard.admin.members`, `regionguard.admin.flags`, `regionguard.admin.remove` and `regionguard.admin.redefine`. The full list and a ready-made LuckPerms command are in "Farmstead Claims permissions".

## Redefining a region

`/rg redefine <id>` uses the new selection but carries over the old owners, members and flags. If the selection size is invalid or there is no selection, the existing region stays unchanged.

## Teleportation

`/rg tp <id>` looks for a safe landing spot near the centre. A regular player must be an owner or member and have `regionguard.command.tp`; teleporting to `__global__` is not allowed.
