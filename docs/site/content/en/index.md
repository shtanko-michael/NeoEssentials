## What this site is

The reference for **Farmstead NeoEssentials** — a fork of the server-side "essentials" mod for Minecraft 1.21.1
on NeoForge, running on [Farmstead Minecraft](https://farmsteadminecraft.online). This is not
the ZeroG upstream documentation: some commands, permissions and behaviour here are our own.

It collects everything an admin and a player need: the full command list, permission nodes, every
config key, walkthroughs of common scenarios and the list of known issues.

In this build commands are not open "by default": `permissions.requireExplicitGrant: true`.
The "In registry" column on the [Permissions](permissions/index.html) page shows the node's `defaultValue`,
not a live grant. The node has to be granted to the player (LuckPerms or the internal manager). OP passes
with `opsBypassPermissions: true`.

The key difference from a regular wiki: **the command, permission and config tables are not written by hand**. They
are extracted from the sources on every site build — from command registrations, from the permission registry and
from the very JSON files the mod puts on the server. So the "Permission" column shows the node
the mod actually checks, and "Default" in the config is the value that will really
arrive in your file. These pages cannot drift from the code: if a command is renamed and
the site is not updated, the site build simply fails.

What is written by hand is what you cannot derive from code: examples of real tasks, caveats where
systems meet, and known issues with workarounds.

## Where to start

| If you… | Go here |
|---|---|
| are installing the mod for the first time | [Quick start](quickstart/index.html) |
| are looking for a specific command | [All commands](commands/index.html) — search by text or press <kbd>/</kbd> anywhere on the site |
| are granting permissions to groups | [Permissions](permissions/index.html) |
| are tuning behaviour | [Configuration](config/index.html) |
| ran into strange behaviour | [Known issues](troubleshooting/index.html) |

Per-system sections — economy, teleportation, moderation and the rest — are in the menu on the left. Each
of those pages starts with the system's commands and worked examples, then caveats and
issues, and at the very bottom the detailed guide from the project's original documentation.

## About versions

The site is built from the `farmstead-monorepo-build` branch of the Farmstead fork — Minecraft **1.21.1** and
NeoForge **21.1.179**. The original ZeroG project develops two more lines, `26.1.x` and `26.2.x`,
for newer game versions; their commands and config keys differ in places, so
rely on this site only for the Farmstead build.
