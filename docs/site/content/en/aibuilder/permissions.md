## Two doors, either one is enough

Every subcommand can be passed in two ways:

1. **A vanilla permission level** from `config/farmstead_aibuilder.toml`, section `[permissions]`: `0` — everyone, `2` — operators (the `/give`, `/setblock` tier), `4` — owner/console.
2. **A node** `aibuilder.command.*`, granted by a permission manager.

Almost all levels default to `0`, so the commands are open to everyone: the real limiter is your daily caps in the cabinet, not permissions. The exception is `status_level = 2`: `/aibuild status` is the administrator's view.

> **Order matters.** A node changes nothing while `level = 0`: everyone clears level 0 and the node is never consulted. Raise the level first, then grant the node.

The mod declares its nodes to the NeoForge Permission API, so **LuckPerms sees them on its own** — nothing else to install.

## Generation for one group only

A typical "generation for donors only, the rest for everyone" setup:

```toml
# config/farmstead_aibuilder.toml
[permissions]
create_level = 4   # closed create and area to everyone but the console
area_level   = 4
```

```text
/lp creategroup builder
/lp group builder permission set aibuilder.command.create true
/lp group builder permission set aibuilder.command.area true
/lp user Steve parent add builder
```

For one player without a group: `/lp user Steve permission set aibuilder.command.create true`. To take it back: `/lp group builder permission unset aibuilder.command.create` — then the level decides again.

## All nodes and levels

| Node | Level key | Default | Subcommands |
|---|---|---|---|
| `aibuilder.command.create` | `create_level` | `0` | `create` |
| `aibuilder.command.area` | `area_level` | `0` | `area`, `area build`, `area pos1/pos2/clear` |
| `aibuilder.command.list` | `list_level` | `0` | `list` |
| `aibuilder.command.get` | `get_level` | `0` | `get` |
| `aibuilder.command.placed` | `placed_level` | `0` | `placed` |
| `aibuilder.command.undo` | `undo_level` | `0` | `undo`, `remove` |
| `aibuilder.command.upgrade` | `upgrade_level` | `0` | `upgrade` |
| `aibuilder.command.balance` | `balance_level` | `0` | `balance` |
| `aibuilder.command.topup` | `topup_level` | `0` | `topup` |
| `aibuilder.command.status` | `status_level` | `2` | `status` |

`undo` and `remove` share one node: it is one power over the same records.

## Without a permission manager

No node is ever granted and the config levels are the whole gate. Operators pass everything: `/op` grants level 4, which clears any level below it (see `op-permission-level` in `server.properties`).
