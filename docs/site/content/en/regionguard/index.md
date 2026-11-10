RegionGuard is a server-side, WorldGuard-style territory protection mod for **Minecraft 1.21.1** and **NeoForge 21.1.230+**. It stores cuboid regions, manages owners and members, and applies flags to building, PvP, mobs, explosions, weather and Pixelmon.

## What the mod does

- creates a claim from a WorldEdit selection;
- protects blocks and containers from outsiders;
- manages owners and members separately;
- denies PvP, entry, mob damage, item drops, potions, fire and various kinds of explosions;
- creates a fully safe zone via `invincibility` and shows a greeting/farewell above the hotbar;
- sets per-player weather and beacon effects inside a region;
- supports LuckPerms through NeoEssentials and separate Pixelmon rules;
- stores regions separately for each dimension.

## Getting started

1. Install RegionGuard and WorldEdit.
2. Grant the player the base and command permissions.
3. Select an area with `//pos1` and `//pos2`.
4. Run `/rg define home`.
5. Check the region with `/rg info home`.

The step-by-step walkthrough is in [Quick start](quickstart/). The full syntax is in the [command reference](commands/), and protection behaviour is in the [flag reference](flags/).

## Key concepts

An **owner** manages the region's bounds, membership and flags. A **member** can use the claimed area and bypasses build restrictions, but cannot change the region itself. A **guest** is subject to the flags that are set. A moderator can be given separate `regionguard.admin.*` nodes to access other players' claims without `/op`.

Every dimension has a system region `__global__`. It covers the whole world at the lowest priority, so a regular region always overrides it.

## Compatibility

WorldEdit is used to select areas. NeoEssentials connects RegionGuard permissions to LuckPerms and lets you set the claim limit through a meta key. Pixelmon is optional: its integration turns on only when the mod is present and the matching setting is enabled.

The RegionGuard source code lives in the [Farmstead monorepo](https://github.com/shtanko-michael/minecraft-api/tree/main/game/regionguard-mod).
