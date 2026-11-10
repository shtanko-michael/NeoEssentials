## How to use this page

Below are the known problems from all systems: the symptom as an admin sees it, then
the cause and what to do about it. Every entry is checked against the sources, `Issues_Discovered.md` or
the "Fixed" entries in the changelog — no guesses make it here.

Entries are grouped by system; the same entries are repeated at the end of each system's own page, next
to its commands.

## Check this first

Three causes cover most "the command does not work" reports:

1. **The subsystem is disabled.** The `modules` and `commands` sections of `config.json` are read once at
   server startup. If a command is disabled there, it simply is not in the registry — `/help` will not show it, and
   the server answers as if the command were unknown. `/neoessentials reload` does not help here; you need a
   restart.

2. **A missing permission, not a bug.** The build ships with `permissions.requireExplicitGrant: true`: the node from
   LuckPerms (or the internal manager) must be granted explicitly. `defaultValue=true` in the registry does not grant
   access by itself. Without the node the command is not shown in `/help` and behaves as if it did not exist. OP
   still passes with `opsBypassPermissions: true`. Check the actual node in the "Permission" column
   on the [All commands](../commands/index.html) page — it may not match the pattern
   `neoessentials.<command name>`. On denial the player sees only a no-permission message:
   the required node and its description are deliberately not printed to chat.

3. **The config edit was not reloaded.** `/neoessentials reload` refreshes values, but not the set of
   commands or GUIs that are already open. After editing `modules`, `commands`, or changing the
   structure of the config at all, you need a restart.

## If you need more detail

Turn on detailed logging for one specific subsystem — targeted, not globally:

```json
"logging": {
  "categories": {
    "TELEPORTATION": { "normal": true, "debug": true }
  }
}
```

Debug output goes to `logs/debug.log`. Categories and what they cover are on the
[Logging](../systems/logging/index.html) page.
