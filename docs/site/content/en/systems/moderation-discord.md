## How the integration works

NeoEssentials **does not talk to Discord as a bot itself**: it has no token and no gateway of its own. There are four independent pieces:

1. **Chat and event bridges** — the SDLink, Mc2Discord and DCIntegration adapters. They take the already linked account from the bridge and send chat / join / leave / mute / AFK / advancement / private messages to Discord.
2. **Webhooks** — `WebhookAdapter`: a plain HTTPS POST to a channel's webhook URL. No bridge needed. Minecraft → Discord only.
3. **Discord roles → Minecraft group** — `discordrolesync.json` and DCIntegration. Writes to the NeoEssentials internal permission manager.
4. **Web panel login via Discord** — `discord_auth.json` (`roleMapping` to ADMIN/MODERATOR/VIEWER). These are panel roles, not the player's roles on the server. Panel commands (`/dashboardregister discord`, `/linkaccount`) are not covered on this page.

NeoEssentials has no account-linking commands. A player links Minecraft and Discord with the bridge's commands.

## Bridges

Registration order: SDLink → Mc2Discord → DCIntegration → Generic Webhook. An event is sent to **every** ready adapter.

| Bridge | Chat and events | Account link | Discord role list |
|---|---|---|---|
| Simple Discord Link (SDLink) | yes | yes | no (the public API does not expose it) |
| Mc2Discord | yes | yes | no |
| DCIntegration | yes, mostly with an explicit `channelId` | yes | yes |
| Generic Webhook | yes, if `webhookUrl` is set | no | no |

Chat is configured **inside a NeoEssentials channel**: `chat.channels.<name>.discord.enabled` / `channelId` / `webhookUrl`. Non-chat events go in `discordEventChannels` at the top of `config.json`. Channel commands (`/g`, `/staff`, …) belong to the chat channels system, not this page.

`modules.discordIntegrationEnabled: false` skips `ChatIntegrationManager.initialize()` at startup — there will be no bridges and no webhooks until you enable the flag and restart.

## Webhooks

The URL comes from the same `webhookUrl` next to `channelId`. A webhook added via `/neoe reload` starts working without a restart — the adapter does not cache the URL. Installing the bridge mod itself still needs a restart.

There is no reverse direction, no roles and no linking, and a webhook cannot do `sendToChannel` by snowflake id (the dashboard's "test message" will not go through it).

## Double sending

SDLink and Mc2Discord have **their own** relay running in parallel with NeoEssentials. At startup NeoEssentials reads their toml and silences its own duplicates for the default route, logging a WARN. An explicit `channelId` is not silenced. The detector does not re-read the toml if you change it without a restart.

## Roles → groups

File `discordrolesync.json`, task `DiscordRoleSyncTask`. Role snowflake → group name from `permissions.json`. With several roles, the group with the higher `priority` wins. Promotion only. Internal PermissionManager only. DCIntegration only as the role source. Runs on a timer, plus immediately on join.
