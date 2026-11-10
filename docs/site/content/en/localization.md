## How the mod picks a language

The server language is set by the `localization.language` key in `config.json` (for example `ru_ru`). The mod
loads `data/lang/<code>.json` from the jar, applies the admin's edits from
`config/neoessentials/languages/custom/<code>.json` on top, and takes anything missing in the chosen language from
`en_us`. So an incomplete translation does not break output — some lines simply stay in English.

The percentages in the table above are the share of `en_us` keys that have a translation; they are computed when
the site is built, from the same files that ship in the jar.

## Your own translations

To fix individual lines without touching the mod's files:

```
/language template ru_ru        create a template in languages/custom/
/language override set <key> <text>   override one line on the fly
/language override list         list all overrides
/language reload                reload the language files
```

A file in `languages/custom/` is an **overlay**, not a replacement: keys it does not contain keep
coming from the translation in the jar. Because of this, a partial or outdated custom file
cannot hide a complete translation that arrived with a mod update.

## What you must not change in strings

A translation must keep the service fragments, otherwise chat output breaks:

| Fragment | What it is | Rule |
|---|---|---|
| `§a`, `§c`, `§l` | Minecraft colour and style codes | keep all of them, in the same order and position |
| `{0}`, `{1}` | argument substitution | keep all of them, with the same numbers |
| `{player}`, `{server_name}` | named placeholders | do not translate |
| `<gradient:…>`, `<rainbow>` | formatting tags | do not translate |
| `''` | escaped MessageFormat quote | keep it doubled |

A single quote `'` in a string that contains `{0}` swallows the substitution — this is the most common reason
"why `{0}` is printed instead of the player's name".
