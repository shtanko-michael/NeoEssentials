`config/farmstead_aibuilder.toml` is created on the first server start. The comment above every key in the file itself explains it in more detail; below are the main keys and their defaults.

## Language

The mod speaks English and Russian. By default every player reads chat in their own game language (Russian for Russian, Ukrainian, Belarusian and Kazakh clients, English for everyone else). The hologram hints on screen always follow the client. To pin one language for the whole server:

```toml
[language]
language = "en_us"   # auto | en_us | ru_ru
```

| Value | Meaning |
|---|---|
| `auto` | The default: every player reads their own client's language |
| `en_us` | English for everyone |
| `ru_ru` | Russian for everyone |

## `[api]` — the generation service

| Key | Default | What it sets |
|---|---|---|
| `api_base_url` | `"https://ai.farmsteadminecraft.online"` | The generation service address |
| `bearer_token` | `""` | The server token from the [cabinet](https://farmsteadminecraft.online/cabinet/ai-builder) |
| `timeout_seconds` | `60` | Timeout for a plain request (`/aibuild get`), 10–300 s |
| `generate_timeout_seconds` | `600` | Timeout for a generation, 60–1800 s |

## `[links]`

| Key | Default | What it sets |
|---|---|---|
| `topup_url` | `"https://farmsteadminecraft.online/cabinet/wallet?currency=ai"` | The top-up link the mod hands out. Blank turns the command into an honest refusal instead of a dead link |

## `[permissions]`

One `*_level` per subcommand (`0` — everyone, `2` — operators, `4` — console). The full table is in [Permissions and LuckPerms](../permissions/).

## `[chat]`

| Key | Default | What it sets |
|---|---|---|
| `announce_builds` | `true` | Tell the players online that somebody's build has finished, and how to make their own |
| `announce_min_blocks` | `200` | Smallest build worth announcing, in blocks |
| `notify_updates` | `true` | Tell operators once, on login, that a newer release is out; the `[Farmstead]` log line is always written |

## `[region]` — the `/aibuild area` selection

| Key | Default | What it sets |
|---|---|---|
| `max_region_axis` | `100` | Longest side of a region, in blocks (1–100, the service's limit) |
| `max_region_distance` | `192` | How far from the player a corner may be set — a guard against modified clients |

## `[placement]` — laying blocks

| Key | Default | What it sets |
|---|---|---|
| `budget_ms` | `2` | Placement time budget per server tick, 1–5 ms |
| `max_blocks` | `160000` | Maximum blocks in one build (absolute limit 262144) |
| `default_strategy` | `"BOTTOM_UP"` | Placement order: `BOTTOM_UP`, `TOP_DOWN`, `CENTER_OUT`, `LAYER_BY_LAYER` |

## `[preview]` — the hologram

| Key | Default | What it sets |
|---|---|---|
| `holo_max_blocks` | `20000` | Above this the client is shown only the bounding box |
| `aim_reach` | `128` | How far the ghost can be aimed, in blocks |
| `drop_depth` | `160` | When the aim ray hits nothing — how far down to look for ground |
| `sky_distance` | `16` | Area selection only: where a corner in mid-air is placed |
| `ttl_seconds` | `300` | How long an unconfirmed offer is held on the server |

## `[animation]` — watching it go up

`enabled = true` turns on the layer-by-layer reveal; `false` makes the build appear in one go at the end. The other keys of the section (`blocks_per_tick`, `min_duration_ticks`, `max_duration_ticks`, `effects`, `ignition`, `sky_beam`, `snake_order`, `per_block_dissolve`, `landing_flash`, `hover_blocks`, `finale`, `build_sounds`) fine-tune the pace and the effects; each one is described in its comment in the file.
