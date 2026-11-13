## Requirements

- A **NeoForge 1.21.1** server, Java 21.
- **The same jar on players' clients**, in their `mods/`: the client half draws the aiming hologram (`R` rotates, LMB/Enter places, RMB/Backspace cancels) and the whole build animation.
- An account on [farmsteadminecraft.online](https://farmsteadminecraft.online) with AI tokens on the balance.
- Outbound HTTPS from the server to `ai.farmsteadminecraft.online`.
- **LuckPerms is optional.** You only need it to hand commands out per group rather than by the permission levels in the config.

A player **without** the mod can join, but the build just appears in front of them — no aiming, no animation. That is a fallback, not a way to play.

## Installation

1. Download the [latest release](https://github.com/shtanko-michael/farmstead-aibuilder/releases/latest) and drop the jar into the server's `mods/`. Hand the same file to your players.
2. Start the server once — `config/farmstead_aibuilder.toml` appears.
3. In the cabinet at [farmsteadminecraft.online/cabinet/ai-builder](https://farmsteadminecraft.online/cabinet/ai-builder) press "Create token". The value is shown **once** — copy it straight away.
4. Put it into the config:

    ```toml
    [api]
    api_base_url = "https://ai.farmsteadminecraft.online"
    bearer_token = "fsm_..."
    ```

5. Restart the server.

## Checking the log

After the restart two lines appear in the log:

```text
[Farmstead] AI service OK: server='Main server' mode=external apiVersion=1 …
[regionguard] API available (ClaimsApi) — claims are enforced for non-operators
```

The first means the token was accepted. If you see `[Farmstead] the AI service rejected this server's token` instead, the token is wrong or revoked. `[Farmstead] /aibuild is not configured` means `api_base_url` or `bearer_token` is empty.

The second is about claims: whether Farmstead Claims or the original RegionGuard was found. See [Land protection](../land-protection/).

## Updating

A permanent link to the current build: [releases/latest/download/farmstead-aibuilder-neoforge-1.21.1.jar](https://github.com/shtanko-michael/farmstead-aibuilder/releases/latest/download/farmstead-aibuilder-neoforge-1.21.1.jar). The file name is the same in every version, so the new jar **replaces** the old one — FML would silently load one of two files with the same mod id. The version lives in the release title and in the jar manifest. Limits are stored on the service side; updating the mod does not reset them.

## Keeping the token safe

The token is a key to your AI-token wallet. Do not publish the config. If it leaks, revoke it in the cabinet and create a new one.
