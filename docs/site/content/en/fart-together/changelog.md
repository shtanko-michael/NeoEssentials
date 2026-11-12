All notable changes to Fart Together. Versions follow [Semantic Versioning](https://semver.org/).

## [0.1.0] — unreleased

First release.

- A toot key (G, rebindable): a pose, soft tinted puffs of gas behind the hips that drift up and fade, and one of eight sounds, the same for every player around (one seed per toot).
- **Hold to charge**: holding the key builds the toot up to the gas you have, letting go fires it; a quick tap is a small toot. The gas meter shows the build-up while you hold.
- **Gas from food**: beans, cabbage and a bowl of beans are the mod's own crops and food; vanilla foods have weights too, an enchanted golden apple fills the meter and makes the next toot golden; milk empties it. The weights are data (`data/<namespace>/gas_foods/*.json`).
- **Gas clouds** linger where you tooted and scare mobs away; effects inside the cloud are a server choice (none by default, other players only with PvP rules).
- **Jet jump**: a toot in mid-air pushes you up and softens the landing.
- **Fart Together**: players who toot close together within a moment make one big shared cloud, a chorus and a combo counter; advancements for 2, 4 and 8 players.
- **Under water** the toot is a stream of bubbles.
- Nine advancements in the Husbandry tab.
- Beans and cabbage seeds from grass, farmer villagers trading beans, cabbage and bowls of beans, beans in village, dungeon, mineshaft and shipwreck chests (1.20.1–26.2).
- **Your own style**, seen by your friends: cloud colour (presets, hex, gradients, random, rainbow), particle shape, size, amount, spread, duration, sound, pitch, volume and pose, with a "My style" tab and a live preview. The server clamps styles to its rules.
- Server rules in `farmstead_fart_together-server.toml` (presets `default`, `family`, `plain`, `cosmetic` — the toot is pure decoration, `chaos`), `/fart info|reload|charge|toggle`, LuckPerms nodes for every action and meta for cooldown, cloud radius and meter size.
- Streamer-safe mode (every sound becomes a soft "puff"), a particle limit for others' toots, 12 languages.
- Farmstead start-up line and a once-per-session update check against Modrinth.
- Minecraft 1.20.1 (Fabric, Forge); 1.21–1.21.5, 1.21.7–1.21.11, 26.1–26.3 (Fabric, NeoForge). Fabric needs Fabric API.
