## Where limits are set

All of it in the cabinet, on the same page where the token was created — [farmsteadminecraft.online/cabinet/ai-builder](https://farmsteadminecraft.online/cabinet/ai-builder):

- **Daily limit for the server** — how many tokens the server may spend per day (UTC).
- **Daily limit per player** — the default for everyone; in the player table you can set a personal one or **ban** a player from generating.
- **Report** — who spent what over 7 / 30 days, per player and per day.

Limits are enforced on the service side, not in the mod: they take effect immediately, no restart is needed, and updating the mod does not reset them.

## What a build costs

A build usually costs **200–600 tokens** from the token owner's balance. Only `/aibuild create` and `/aibuild area build` spend; placing again with `/aibuild get`, undo and the lists are free.

## What the player and the administrator see

- `/aibuild balance` — the player sees what is left for today.
- `/aibuild topup` — explains to the player that the server administrator pays for generations.
- `/aibuild status` — operators see the server's day: spend against the daily limit, auto-extensions used and whether the wallet is healthy.

When a limit is used up, the player gets a clear refusal — see [Troubleshooting](../troubleshooting/).
