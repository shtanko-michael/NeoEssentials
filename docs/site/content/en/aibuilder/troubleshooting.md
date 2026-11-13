## Log lines and chat messages

| In the log or in chat | What it means |
|---|---|
| `[Farmstead] … rejected this server's token` | The token is wrong or revoked — check `bearer_token` |
| `[Farmstead] /aibuild is not configured` | `api_base_url` or `bearer_token` is empty in the config |
| `Your limit for today is used up` | The player's daily cap — raise it in the cabinet or wait for midnight UTC |
| `The server's daily limit is used up` | The server's daily cap — same |
| `The server administrator has turned off build generation for you` | The player is on the deny list |
| `The service is busy` | Too many generations at once — try again in a minute |
| `Not enough AI tokens` | Your AI wallet is below the minimum — top it up in the cabinet |
| `[regionguard] claim check failed` | A claim mod is installed but its call failed; placements are refused until that is fixed |

## A command with non-Latin text "does not go through"

Some mod filters characters in commands. In NeoEssentials it is the `security.allowUnsafeCommands` key — see the [NeoEssentials configuration](../../config/index.html).

## No hologram and no animation

The player has no mod on the client. They can still use the command, but the build just appears in front of them. Hand players the same jar the server runs.

## Builds go anywhere despite claims

The log says `[regionguard] API not available` — no claim mod was found. See [Land protection](../land-protection/).

## Bugs and suggestions

Use the mod repository's [Issues](https://github.com/shtanko-michael/farmstead-aibuilder/issues).
