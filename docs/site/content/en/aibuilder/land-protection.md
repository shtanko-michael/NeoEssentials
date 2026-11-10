## Which claim mods it works with

AI Builder checks claims when the server runs one of two mods:

- **[Farmstead Claims](../../claims/)** (`farmstead_claims`) — Farmstead's own mod, a drop-in replacement for RegionGuard;
- the original **RegionGuard**.

The mod asks Farmstead Claims first, then RegionGuard. It knows no other claim systems.

## What the log shows

| Log line | What it means |
|---|---|
| `[regionguard] API available (ClaimsApi) — claims are enforced for non-operators` | Farmstead Claims found: the whole build must fit inside the player's region |
| `[regionguard] API available (RegionGuardApi) — claims are enforced for non-operators` | The same with the original RegionGuard |
| `[regionguard] API not available — claim checks are skipped; …` | No claim mod, AI Builder checks **no land** |

Operators are not subject to the claim check.

## Without a claim mod

A player will place a build anywhere the command is allowed. Options: raise `create_level`/`area_level` and grant the node to people you trust (see [Permissions and LuckPerms](../permissions/)), cap the daily limit — or install Farmstead Claims.

## Refusal outside a claim

If a build reaches outside the player's region, the mod refuses and shows its size, its corners and the commands to claim the land it needs.

## When the check fails

If a claim mod is installed but its call fails, AI Builder **refuses** the placement and logs `[regionguard] claim check failed` — a protection check that errors must not read as "permitted". The same goes for `/aibuild undo`: it is allowed only when the player may build on that spot right now.
