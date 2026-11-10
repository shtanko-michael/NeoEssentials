## Requirements

- Minecraft `1.21.1`;
- NeoForge `21.1.230` or newer on the `21.1.x` line;
- Java 21;
- WorldEdit for creating and redefining regions;
- optional: NeoEssentials and LuckPerms to grant permissions without OP.

Put the RegionGuard JAR into the server's `mods` folder and restart the server. On first start the mod creates its config, language files and a global region for every loaded dimension.

## Permissions for a regular player

The minimum set for creating and managing your own claim. The safe home flags are listed below; granting `regionguard.command.flag.*` instead also gives the dangerous balance-affecting flags, including `invincibility`.

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

OP level 2 and above passes command checks without these nodes. Details and safe permission sets are in [Permissions](../permissions/).

## Your first claim

Select two opposite corners with the WorldEdit wooden axe or with commands:

```text
//pos1
//pos2
```

Create the region:

```text
/rg define home
```

`/rg claim home` does the same. The player who creates it becomes the owner, and the standard protection flags are written explicitly from `defaultDeniedFlagsOnClaim` and `defaultAllowedFlagsOnClaim`.

Check the result:

```text
/rg info home
/rg flag home list
```

## Members and owners

Add a player who should build in and use the area:

```text
/rg addmember home PlayerName
```

A new owner can also change the region's flags and membership:

```text
/rg addowner home PlayerName
```

## Typical protection settings

Deny PvP completely inside the region:

```text
/rg flag home pvp deny
```

New claims are already created with guests denied interaction with doors, buttons and entities. For an older region, turn it on by hand:

```text
/rg flag home interact deny
```

Deny entry to outsiders and explosions:

```text
/rg flag home entry deny
/rg flag home tnt deny
/rg flag home creeper-explosion deny
/rg flag home other-explosion deny
```

With `pvp deny`, owner or member status gives no bypass. Only an admin with `regionguard.bypass.pvp`, `regionguard.admin.access` or OP level 2 can hit a player in a protected region.

A safe zone where players do not lose items and take no damage from mobs:

```text
/rg flag home mob-damage deny
/rg flag home item-drop deny
/rg flag home potion-splash deny
```

## Changing bounds and removing

Make a new WorldEdit selection and run:

```text
/rg redefine home
```

Owners, members and flags are kept. To delete the region completely, use `/rg remove home`.
