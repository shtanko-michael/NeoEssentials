Open them from Mod Menu (Fabric) or the mod list (NeoForge, Forge), type `/footprints config`, or edit `config/farmstead_footprints-client.toml`.

| Key | Default | Range | What it does |
| --- | --- | --- | --- |
| `preset` | `default` | `default`, `vanilla_plus`, `cinematic`, `performance` | Sets every value below at once |
| `general.enabled` | `true` | | Master switch |
| `general.checkUpdates` | `true` | | Look for a newer version on Modrinth once at start-up (see below) |
| `footprints.players` | `true` | | Prints of players, yourself included |
| `footprints.mobs` | `true` | | Prints of animals and monsters |
| `footprints.maxPrints` | `2048` | 64 – 16384 | Prints alive at once; the oldest go first |
| `footprints.renderDistance` | `48` | 8 – 128 | Blocks from the camera where prints are made and drawn |
| `footprints.lifetimeMultiplier` | `1.0` | 0.1 – 10.0 | How long prints stay |
| `footprints.opacity` | `0.95` | 0.1 – 1.0 | How strongly prints show |
| `footprints.entityBlacklist` | flying and footless mobs | ids, #tags | Entities that never leave prints |
| `footprints.sizeMultiplier` | `1.0` | 0.25 – 4.0 | Makes every print bigger or smaller |
| `footprints.lift` | `0.0` | -0.05 – 0.5 | Raises prints above the ground (for shader packs) |
| `footprints.style` | `DETAILED` | `DETAILED`, `PIXEL` | Smooth prints, or the block's own pixels pressed in |
| `footprints.sneaking` | `SHALLOW` | `SHALLOW`, `NONE` | Sneaking leaves shallow prints or none |
| `footprints.invisible` | `false` | | Invisible players and mobs leave prints too |
| `footprints.profiles` | `[]` | lines | Prints per mob or #tag, see [Your own prints](#your-own-prints) |
| `effects.wetPrints` / `wetSeconds` / `wetOpacity` | `true` / `10` / `1.0` | | Damp prints on hard ground after water or rain |
| `effects.splash` | `ALL` | `OFF`, `PLAYERS`, `ALL` | Droplets from wet feet |
| `effects.ripples` / `boatWake` / `swimBubbles` / `railSparks` | `true` | | Water and rail effects |
| `effects.removeWhenBroken` | `true` | | A print goes with its block |
| `effects.decay` | `CRUMBLE` | `CRUMBLE`, `FADE` | How old prints go |
| `effects.rainWear` | `3.0` | 1 – 20 | How much faster rain wears exposed prints |
| `effects.dust` | `ALL` | `OFF`, `PLAYERS`, `ALL` | A puff from under the feet on loose ground |
| `surfaces.hardnessGate` | `0.7` | 0 – 5 | Any block softer than this takes prints |
| `surfaces.include` / `exclude` | wool / leaves, ice, … | ids, #tags | Blocks that always / never take prints |

Presets: **vanilla_plus** — no mob prints, half the lifetime, lighter; **cinematic** — 8192 prints, 96 blocks, three times longer; **performance** — 512 prints, 24 blocks, half the lifetime.

In the file, keys that follow the preset are written commented out (`#maxPrints = 2048`). Remove the `#` to set a value yourself; your value then wins over the preset. A wrong value is reported in the log, and in chat after `/footprints reload`, and the preset value is used instead: the game never crashes over the config.
