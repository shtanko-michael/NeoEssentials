Versions follow [semantic versioning](https://semver.org/). The current build is on the [releases page](https://github.com/shtanko-michael/farmstead-aibuilder/releases/latest).

## v1.1.0

- The mod speaks English and Russian: chat follows each player's game language, `[language] language` in the config pins one.
- Cancel the hologram and leave `/aibuild area` with RMB or **Backspace** (Esc was taken by the pause menu).
- Chat hints with clickable commands after placing, removing and generating.
- New generation stages in chat; a stage repeated back to back is said once.
- A server console line for every successful generation.
- A refusal outside a claim (with RegionGuard) shows the size, the corners and the commands to claim it.
- Chat lines signed `[Farmstead AI]`; finished builds announced (`[chat] announce_builds`).
- `/aibuild`, `help`, `about`, `status` (operators).
- Clients without the mod can join again.
- A failed print no longer crashes the server; heightmaps after a print; own HTTP pool; handshake retried.
- Cabinet: pause, cooldown, limit auto-extension, emails, event feed.

## v1.0.0 — 2026-09-13

First public release.

- `/aibuild create <description>` — a build from text: hologram preview, `R` to rotate, confirm, then laid block by block in front of you.
- `/aibuild area` — mark a region in the world and generate a build to fit that volume.
- `/aibuild list` and `/aibuild get <key>` — a personal build library; placing again is free.
- `/aibuild placed`, `/aibuild undo`, `/aibuild remove <n>` — take a build down and restore what was under it.
- `/aibuild balance` — what is left of the player's daily cap.
- Permissions: a config level **or** an `aibuilder.command.*` node; the nodes are declared to the NeoForge Permission API, so LuckPerms reads them with no extra setup.
- RegionGuard is optional; when present, claims are enforced and the startup log says so.
- Daily caps for the server and per player, personal caps and bans — in the web cabinet.

Requirements: NeoForge 1.21.1, Java 21. The same jar goes on the server and on players' clients.
