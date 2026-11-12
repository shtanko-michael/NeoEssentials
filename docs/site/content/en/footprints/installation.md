## Downloads and versions

| Minecraft | Fabric / Quilt | NeoForge | Forge | Java |
| --- | --- | --- | --- | --- |
| 26.1 – 26.3 | yes | yes | — | 25 |
| 1.21.7 – 1.21.11 | yes | yes | — | 21 |
| 1.21 – 1.21.5 | yes | yes | — | 21 |
| 1.20.1 | yes | — | yes | 17 |

On 26.x boat wakes and ripples are drawn as foam particles (the game draws custom geometry before the water there). 1.21.6 is not covered.

Get it from [Modrinth](https://modrinth.com/mod/farmstead-footprints), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/farmstead-footprints) or [GitHub Releases](https://github.com/shtanko-michael/farmstead-footprints/releases).

**Requirements: none.** No library, not even Fabric API on Fabric: the jar is all you need. [Mod Menu](https://modrinth.com/mod/modmenu) is optional and adds a settings button on Fabric.

**Server or client?** Client only. The server does not need it, and you can join any server with it. Installed on a dedicated server it only writes its start-up line to the log and checks for updates.

## Update check and privacy

When the game starts, Footprints writes one line to the log:

```
[Farmstead] Footprints 0.1.0 started (neoforge, Minecraft 1.21.1) - https://modrinth.com/mod/farmstead-footprints
```

Then, once per session and in the background, it asks the public Modrinth API whether a newer release exists for your loader and Minecraft version. If one does, the log says so with a download link. The request carries no information about you, your world or your server; nothing is downloaded or installed. Turn it off with `general.checkUpdates = false`, or for every Farmstead mod at once with the JVM flag `-Dfarmstead.updateCheck=false` or the environment variable `FARMSTEAD_UPDATE_CHECK=false`.
