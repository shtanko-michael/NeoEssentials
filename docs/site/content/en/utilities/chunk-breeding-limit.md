A server-side mod (`farmstead_chunkbreeding`) for **NeoForge 21.1.230+ / Minecraft 1.21.1**: players do not install it. It stops a new baby from appearing once the farm's area has reached its animal cap. Existing animals are never removed.

## Counting area: region or chunk

The default is `scope = "region"`. When **[Farmstead Claims](../../claims/)** is installed and the parents stand inside an ordinary region, the mod counts animals across that whole region, even when the farm spans several chunks. Building one pen across a chunk border does not get around the cap.

Two neighbouring regions are independent: animals in player A's region do not use up player B's cap. The technical global region `__global__` is not treated as a farm. When the animals stand outside a region, or Farmstead Claims is not installed (it is an optional dependency), the mod falls back to one 16×16 chunk over the full world height.

Regions come from Farmstead Claims only: this mod does not ask the original RegionGuard — with it the chunk rule applies.

## What is limited

- **Breeding.** No new baby appears when the area already holds the cap or more. The player who fed the animals sees above the hotbar: `Лимит животных в регионе 'cows' уже достигнут: 15/15.` ("The animal limit in region 'cows' is reached: 15/15.")
- **Spawn eggs** (`spawnEggsRespectLimit = true`). A passive mob's egg used on a block or on an animal does not create an animal over the cap. Spawners configured with an egg are covered too: the extra animal from such a spawner does not appear. Hostile mob eggs and dispensers are not affected.
- Adults and babies of every `Animal` mob count.

## Operator commands

Every command needs permission level **2** (operator). Values set through the commands apply at once and are written to the config, so they survive a restart.

| Command | What it does |
|---|---|
| `/chunkbreedlimit get` | Shows the cap and the active counting area |
| `/chunkbreedlimit set <0-10000>` | Sets the total cap at once and saves it |
| `/chunkbreedlimit scope region` | Count per Farmstead Claims region; outside a region, per chunk |
| `/chunkbreedlimit scope chunk` | Always count in one chunk only |
| `/chunkbreedlimit count <animal>` | The number of animals of that type and the cap in the selected animal's area |

Example: `/chunkbreedlimit count @e[type=minecraft:cow,limit=1,sort=nearest]` — how many cows are in the nearest cow's area and what their cap is. When the type has no cap of its own in `entityLimits`, every animal in the area is counted.

## Config

After the first server start `config/farmstead-chunk-breeding.toml` appears:

```toml
enabled = true
spawnEggsRespectLimit = true
operatorsBypassSpawnEggLimit = true
scope = "region"
maxAnimalsPerChunk = 24
entityLimits = ["minecraft:cow=12", "minecraft:chicken=32"]
```

| Key | Default | What it sets |
|---|---|---|
| `enabled` | `true` | Turns the limits on |
| `spawnEggsRespectLimit` | `true` | Passive mob spawn eggs and egg-configured spawners obey the same cap |
| `operatorsBypassSpawnEggLimit` | `true` | Operators (level 2) bypass the cap for eggs; `false` — the cap applies to them too |
| `scope` | `"region"` | `region` — the Farmstead Claims region, a chunk outside it; `chunk` — always a chunk |
| `maxAnimalsPerChunk` | `24` | Total animal cap in the area (0–10000; `0` stops all breeding) |
| `entityLimits` | `[]` | Separate caps per type, as `mob_id=number` |

An `entityLimits` entry replaces the total cap for its type: with `maxAnimalsPerChunk = 15` and `minecraft:cow=12` there will be at most twelve cows, and only cows are counted for it. Invalid entries are ignored.

After editing the TOML by hand, restart the server or reload the config. The commands are more convenient for day-to-day changes: they apply and save the value at once.

## Setting up independent farms

1. Claim the first farm with an ordinary Farmstead Claims command, for example `/rg define cows` after selecting two corners.
2. Make sure the whole pen is inside that region: `/rg info cows`.
3. Keep the `region` mode and set the cap: `/chunkbreedlimit set 15`.
4. For the next farm create a separate, non-overlapping region. Its cap is independent of the first.

If you do not use regions, switch to `/chunkbreedlimit scope chunk`; players should then build each pen inside one chunk (`F3 + G` shows chunk borders).

## Language

The mod's messages — above the hotbar and in command replies — are in Russian only.
