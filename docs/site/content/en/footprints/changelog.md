All notable changes to Footprints. Versions follow [Semantic Versioning](https://semver.org/).

## [0.1.0] — unreleased

First release.

- Footprints for players and 47 mobs, each with its own foot shape; children and baby zombies leave smaller prints.
- Four-legged animals leave real track patterns: one zig-zag line as wide as the animal's stance (hip positions from each vanilla model), the hind foot landing in the front print of the same side; bears overstep slightly and walk pigeon-toed; sniffers put all three feet of a side in one print. Steps lengthen with speed.
- Soft surfaces: snow, powder snow, sand, red sand, mud, muddy mangrove roots, gravel, dirt, coarse dirt, rooted dirt, podzol, mycelium, farmland, clay, soul sand, soul soil. Prints sit on top of thin snow layers.
- Two-layer prints (hollow and lip) coloured from the ground they sit on; they fade after 1.5–5 minutes depending on the surface.
- Works with Sodium and Iris shader packs.
- Settings: presets (`default`, `vanilla_plus`, `cinematic`, `performance`), in-game settings screen (Mod Menu on Fabric, mod list on NeoForge/Forge), commented config `farmstead_footprints-client.toml`, commands `/footprints config|reload|stats`.
- 12 languages.
- Farmstead start-up line in the log and a once-per-session update check against Modrinth (off with `general.checkUpdates` or `-Dfarmstead.updateCheck=false`).
- Two looks: detailed (hollow and lip) and pixel (the block's own texture pixels pressed in).
- Prints crumble from the edges as they age; rain wears exposed prints; prints go with a broken block.
- Modded mobs get feet from their model; any soft modded block takes prints (hardness gate, id/#tag lists).
- Water: boat wakes, wading rings, swim bubbles, damp prints on hard ground, droplets; minecart sparks.
- Own prints from PNG files, random variants, resource-pack shapes, base textures and rules; F3+T reload.
- Settings screen with tabs, block/mob pickers, an About tab and "Try it".
- No dependencies on any loader, not even Fabric API.
- Minecraft 1.20.1 (Fabric, Forge); 1.21–1.21.5, 1.21.7–1.21.11, 26.1–26.3 (Fabric, NeoForge).
