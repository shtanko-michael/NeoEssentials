A server-side mod (`farmstead_tradechat`) for **NeoForge 21.1.230+ / Minecraft 1.21.1**. Players do not need it on the client.

## How to use it

A message starting with `@` goes to the trade channel — to every player on the server:

```text
@Selling a Luck III book for 4000 coins
```

becomes

```text
[Торговля] PlayerName: Selling a Luck III book for 4000 coins
```

- `@` must be the **first** character of the message; it is not shown in the channel, and the spaces around the text are trimmed.
- Messages without `@` stay in the normal chat.
- An empty trade message (`@` and nothing else) is cancelled, and the sender gets a hint: `После @ напишите торговое сообщение.` ("Write a trade message after @.")
- A cooldown applies between one player's trade messages (`cooldownSeconds`, 30 seconds by default). A message sent too soon is not posted: `Подождите ещё N сек. перед следующим торговым сообщением.` ("Wait N more seconds…")
- Trade messages are also written to the server console and log.
- The mod has no commands and no permission nodes.

## Config

After the first start `config/farmstead_tradechat-server.toml` appears.

| Key | Default | What it sets |
|---|---|---|
| `channelPrefix` | `"[Торговля] "` | The full channel prefix. Brackets and spaces are written explicitly |
| `cooldownSeconds` | `30` | Cooldown between one player's trade messages, 0–86400 s; `0` turns it off |
| `showGroupPrefix` | `true` | Show the LuckPerms group prefix before the player's name |
| `tagColor` | `"gold"` | Colour of the channel tag |
| `nameColor` | `"white"` | Fallback name colour for a player with no nickname colour of their own |
| `messageColor` | `"white"` | Colour of the message text |

Colours are the English names of the standard Minecraft colours: `gold`, `yellow`, `white`, `green`, `aqua` and so on. The config rejects an unknown name.

For an English-speaking server set, for example, `channelPrefix = "&6[Trade]&r "`.

## Colours and prefixes

`channelPrefix` and the LuckPerms group prefix accept legacy colour and format codes: `&0`…`&f`, `&l`, `&o`, `&n`, `&m`, `&k`, `&r`. For example, `channelPrefix = "&6[Trade]&r "` and the group prefix `&4[Warden]&r ` produce `[Trade] [Warden] KredoO: text` in those colours.

The prefix and the nickname are coloured **separately**: the colour a group prefix ends on does not carry over to the name. The name colour comes from the LuckPerms meta key `farmstead-namecolor` (one Minecraft colour code digit or letter, e.g. `c` for red) and, without it, from `nameColor`. When the prefix already ends in visible whitespace (for example `[Warden] &f`), no second space is added.

LuckPerms is an optional dependency: without it no group prefix is shown and the name uses `nameColor`.

## Language

The empty-message hint and the cooldown notice are translated into Russian and English and follow the client's language when the client has the translation. Since the mod is server-only, a regular client has none, so players see the Russian text.
