## Server config

The main settings are in `config/regionguard-server.toml`:

```toml
[general]
allowFallbackSelection = true
enablePixelmonCompat = true
language = "ru_ru"
maxRegionsPerPlayer = 3
defaultDeniedFlagsOnClaim = ["build", "block-break", "block-place", "interact", "chest-access", "tnt", "invincibility"]
defaultAllowedFlagsOnClaim = ["use", "entry", "pvp", "mob-damage", "mob-spawning", "mob-entry", "passive-entity-damage", "fall-damage", "item-drop", "potion-splash", "fire-spread", "creeper-explosion", "other-explosion", "thunder", "pixelmon-spawn", "pixelmon-entry", "pixelmon-player"]
```

| Key | Default | Purpose |
|---|---:|---|
| `allowFallbackSelection` | `true` | Allow the internal selection store when WorldEdit is absent. The current version has no command that fills this store. |
| `enablePixelmonCompat` | `true` | Enable the Pixelmon bridge if Pixelmon itself is installed. |
| `language` | `ru_ru` | Message language: `ru_ru` or `en_us`. An unknown value falls back to English. |
| `maxRegionsPerPlayer` | `3` | Per-owner region limit; `-1` means unlimited. Can be overridden by a LuckPerms meta key. |
| `defaultDeniedFlagsOnClaim` | list | Boolean flags written as `false` when a region is created. |
| `defaultAllowedFlagsOnClaim` | list | Boolean flags written as `true` after the deny list. On a repeat, allow wins. |

Unknown IDs and non-boolean flags in the lists are skipped with a warning. IDs are normalized: case does not matter, `_` becomes `-`.

A new claim does not make players invulnerable: `invincibility` is explicitly written as `deny` (`false`). `interact` is also written as `deny` by default: guests cannot open doors and buttons, interact with armor stands, hit entities, or open another player's worn backpack through normal interaction. The new flags `mob-damage`, `item-drop` and `potion-splash` are allowed by default. Changing the list affects only claims created after the TOML change; existing regions are not rewritten.

## Additional JSON

`config/RegionGuard/config.json` holds fields kept for compatibility with an early version. Only `enablePixelmonCompat` is actually used, duplicating the server TOML. The fields `createFallbackSelection`, `enableWorldEditBridge` and `localeOverridesDenyMessage` are currently not read by any handler.

## Localization

Editable language files are created on first start:

```text
config/RegionGuard/lang/ru_ru.json
config/RegionGuard/lang/en_us.json
```

Translation lookup goes from the file on disk to the built-in selected language, then to the built-in `en_us`, and finally to the key itself. Changes apply after a restart or a NeoForge config reload.

The string flags `greeting`, `farewell` and `deny-message` belong to a specific region and are not keys of the language catalogue.

## Region storage

A separate JSON file is created for each dimension:

```text
<world>/regionguard/regions/minecraft_overworld.json
<world>/regionguard/regions/minecraft_the_nether.json
```

The file holds a flat array of regions: ID, minimum and maximum points, priority, parent, owner and member UUIDs, and the flags that are set.

Before editing by hand, stop the server and make a backup. Commands cannot change priority and parent, so those fields are only available through JSON.

## Integrations

**WorldEdit** is detected without a hard compile-time dependency and provides the selection for `/rg define` and `/rg redefine`.

**NeoEssentials** is an optional bridge to PermissionAPI and LuckPerms. Without it, external node checks safely return a denial, and OP access keeps working.

**Pixelmon** is detected at startup. The `pixelmon-*` flags apply only when the integration is enabled and the Pixelmon classes are available.
