## Explicit grants (Farmstead build)

`config.json` ships with `permissions.requireExplicitGrant: true`. A player gets a command only
after the node is explicitly granted in LuckPerms (or in the internal manager when the external
adapter is off). `defaultValue: true` in `PermissionRegistry` does **not** open a node on its own.

OP level 2+ still passes while `opsBypassPermissions: true`. To make operators follow the same
rules as players, set `opsBypassPermissions: false`.

The old behaviour, "almost every gameplay command is open to everyone until you deny it", comes
back only one way: `requireExplicitGrant: false`.

The "Detailed guide" section below is the original upstream text. It still describes silent
grants via `defaultValue`. For this build, rely on the chain in this page's caveats and on
the [Quick start](../../quickstart/index.html).

To check the live result: `/permissions debug <player>` — the output includes `requireExplicitGrant`.
