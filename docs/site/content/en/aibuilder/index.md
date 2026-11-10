Farmstead AI Builder is a **Minecraft 1.21.1** mod for **NeoForge** that builds from a text prompt. A player types `/aibuild create a medieval tavern with a porch` — a couple of minutes later the building rises in front of them, block by block.

<p class="cta-row"><a class="cta" href="https://github.com/shtanko-michael/farmstead-aibuilder/releases/latest">⬇ Download the latest version</a> <a class="cta cta-soft" href="https://farmsteadminecraft.online/cabinet/ai-builder">Get a token</a> <a class="cta cta-soft" href="https://github.com/shtanko-michael/farmstead-aibuilder">GitHub repository</a></p>

The demo clip and the original README are in the [mod's repository](https://github.com/shtanko-michael/farmstead-aibuilder).

## What the mod does

**Text-to-build generation.** A player describes what they want in plain words — a house, a tower, a bridge, a farm, a whole plaza — and gets a finished structure. These are not pre-made schematics: every build is planned for the prompt it was given, so "a small fisherman's hut on stilts" and "a tall stone tower with a flag" come out as different buildings.

**A hologram before anything is placed.** Not a single block is written to the world until the player agrees. The structure first appears as a ghost under the crosshair: carry it further out, turn it with `R` so the right face meets the road, and only then confirm. Cancelling is one click.

**Built in front of you.** Blocks are laid in courses from the ground up, glowing dust trails the laying front, a beam comes down from the sky. The server does not stall for it — placement runs on a budget of a couple of milliseconds per tick.

**Generate into a selected region.** `/aibuild area` marks corners in the world, and the build is planned **for that volume** rather than cropped to it.

**Full undo.** The mod remembers what stood under every build, and `/aibuild undo` puts the ground back the way it was. Any row of `/aibuild placed` can be undone.

**A library per player.** `/aibuild list` shows your own builds, `/aibuild get <key>` places one again — somewhere else and for free.

**Spend control for the administrator.** Generation is paid for with tokens from your account; server and player daily caps, personal caps and bans are set in the cabinet — see [Limits and spend](limits/).

## How it works

1. **A player runs the command.** `/aibuild create <description>` — ordinary text, in English or Russian.
2. **Generation runs on the Farmstead service.** Your server sends the description to `ai.farmsteadminecraft.online` and receives a finished set of blocks. Your server needs neither a GPU nor any AI provider keys.
3. **The mod places the build.** Hologram, confirmation, layer-by-layer placement, and a record that makes it undoable.

The **token owner pays** — you, from your AI-token balance on [farmsteadminecraft.online](https://farmsteadminecraft.online). A build usually costs **200–600 tokens**.

## Supported builds

| Loader | Game version | Release file |
|---|---|---|
| NeoForge | 1.21.1 | `farmstead-aibuilder-neoforge-1.21.1.jar` |

Paper/Purpur, Fabric and Forge 1.20.1 are planned. The file name carries no version on purpose: an update **overwrites** the old jar instead of leaving a second one with the same mod id beside it.

## Where to start

1. [Install the mod and put in the token](installation/).
2. Look through the [commands](commands/) and decide who gets them — [permissions and LuckPerms](permissions/).
3. Set the [daily limits](limits/) in the cabinet.
4. If your server has claims, read about [land protection](land-protection/).

## What is sent to the service

With every request the service receives: the **build description**, the **player id** (UUID) and **their nickname** — so that the spend report can show you who spent what. Nothing else is read from your server. The snapshot of "what stood under the build" is stored as opaque bytes and exists only for `/aibuild undo`.

## License

Installing it on your own servers and **handing it to the players of your server** is allowed, including on paid servers. Publishing it on file-sharing sites and mod catalogues, decompiling and modifying it is not. Full text: LICENSE.md in the [repository](https://github.com/shtanko-michael/farmstead-aibuilder). If you found a way around the token, the limits or the permissions, follow SECURITY.md in the [repository](https://github.com/shtanko-michael/farmstead-aibuilder) — do not open a public issue.
