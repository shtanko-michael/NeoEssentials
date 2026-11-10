Only `create` and `area build` spend tokens. Everything else is free. By default all of them are open to everyone (except `status` — operators only); how to change that is in [Permissions and LuckPerms](../permissions/).

## Generation

| Command | What it does | Tokens | Node |
|---|---|---|---|
| `/aibuild create <description>` | Generate a build and place it through the hologram | **yes** | `aibuilder.command.create` |
| `/aibuild area` | Selection mode: LMB sets corner 1, RMB corner 2, Backspace exits | — | `aibuilder.command.area` |
| `/aibuild area build <description>` | Generate **into the selected region** | **yes** | `aibuilder.command.area` |
| `/aibuild area pos1` / `pos2` | Set a corner at the player's position (for clients without the mod) | — | `aibuilder.command.area` |
| `/aibuild area clear` | Reset the selection | — | `aibuilder.command.area` |

## Library and placed builds

| Command | What it does | Tokens | Node |
|---|---|---|---|
| `/aibuild list [page]` | Your own builds as a list; a row is clickable | — | `aibuilder.command.list` |
| `/aibuild get <key>` | Place one of your builds again | — | `aibuilder.command.get` |
| `/aibuild placed [page]` | Which of your builds stand in this world | — | `aibuilder.command.placed` |
| `/aibuild undo` | Take the last build down and restore what was under it | — | `aibuilder.command.undo` |
| `/aibuild remove <n>` | The same for any row of `placed` | — | `aibuilder.command.undo` |
| `/aibuild upgrade <n>` | Apply a build's current version to a copy already standing in the world | — | `aibuilder.command.upgrade` |

`undo` is allowed only when the player may build on that spot right now.

## Balance and help

| Command | What it does | Node | Default level |
|---|---|---|---|
| `/aibuild balance` | How many tokens the player has left today | `aibuilder.command.balance` | `balance_level = 0` |
| `/aibuild topup` | Explains that the administrator pays for generations | `aibuilder.command.topup` | `topup_level = 0` |
| `/aibuild status` | The server's day: spend against the daily limit, auto-extensions, wallet health | `aibuilder.command.status` | `status_level = 2` (operators) |
| `/aibuild`, `/aibuild help` | The mod's card: only the commands this player may run | — | everyone |
| `/aibuild about` | What the mod is and whose it is | — | everyone |

## Hologram and selection

| Key | Action |
|---|---|
| `R` | Rotate the hologram |
| LMB or Enter | Place the build |
| RMB or Backspace | Cancel the hologram or leave `/aibuild area` |

Esc does not cancel — the pause menu takes it.

Builds made by players on your server are visible only there: they have no access to the Farmstead launcher or marketplace, and your own cabinet does not show them either — only the spend.
