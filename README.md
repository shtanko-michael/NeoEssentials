# NeoEssentials

[![Documentation](https://img.shields.io/badge/%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%82%D0%B0%D1%86%D0%B8%D1%8F-shtanko--michael.github.io-0f7b5f?style=for-the-badge&logo=readthedocs&logoColor=white)](https://shtanko-michael.github.io/NeoEssentials/)

> **📖 [Полный справочник: команды, права, конфигурация и разбор проблем](https://shtanko-michael.github.io/NeoEssentials/)**  
> Таблицы команд, узлов прав и ключей конфигурации генерируются из исходников при каждой
> сборке, поэтому не расходятся с кодом. Поиск по всему справочнику — клавиша `/`.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://www.minecraft.net/) [![NeoForge](https://img.shields.io/badge/NeoForge-orange.svg)](https://neoforged.net/) [![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](https://opensource.org/licenses/MIT) [![Version](https://img.shields.io/github/v/release/ZeroG-Network-PTY-LTD/NeoEssentials?label=Version)](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/releases) [![Discord](https://img.shields.io/discord/709351422088708196?color=7289da&label=Discord&logo=discord&logoColor=white)](https://discord.gg/dUGAQF2Mga)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Me-F16061?logo=ko-fi&logoColor=white)](https://ko-fi.com/mrwhiteflamesyt)

> NeoEssentials is a comprehensive, config-driven essentials mod for Minecraft NeoForge 1.21.1 servers (this is the primary development branch — see [`26.1.x`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/tree/26.1.x) for the pinned Minecraft 26.1-26.1.2 build, or [`26.2.x`](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/tree/26.2.x) for the pinned Minecraft 26.2 build). It provides 100+ commands, GUI tools, advanced administration, a real-time web dashboard, animated tablist, full localization support, and a PlaceholderAPI system — all managed by modular JSON config files.

## 🌟 Overview

NeoEssentials brings essential server management, player utilities, and advanced admin features to NeoForge servers. All features are strictly documented and driven by config files for reliability and transparency.

**Server-Side Only**: No client install required — works with vanilla clients.  
**100+ Commands**: Covers all major server functions, utilities, and moderation.  
**Modern UI**: GUI commands, color code support, animated tablist, and web dashboard.

## ✨ Core Systems & Features

- **Economy System**: Player balances, payments, baltop, shop support, and Vault-compatible API.
- **Chat & Messaging**: Format templates, per-group/per-world formats, channels, private messages, social spy, AFK broadcasts, mentions, rich text (gradients/rainbow).
- **Moderation**: Ban, tempban, IP ban, kick, mute, jail, vanish, freeze, sudo, player data.
- **Teleportation**: Homes, warps, spawn, TPA, random teleport, /back, safe teleport.
- **Kit Management**: Configurable item kits with cooldowns, previews, and economy costs.
- **Web Dashboard**: Real-time server monitoring, config editing, API endpoints, and Discord auth integration.
- **Permission System**: LuckPerms, FTB Ranks, and built-in group-based permission support.
- **Item Management**: Item spawning, repair, enchant, oversized stacks, clearinventory, powertool, condense.
- **Utility Systems**: Nicknames, MOTD, rules, near, ping, depth, helpop, getpos, playtime, etc.
- **API & Placeholder System**: `{neoessentials_*}` PlaceholderAPI tokens, stat placeholders (deaths, kills, playtime), custom expansion API, and REST API endpoints.
- **Tablist System**: Animated header/footer, per-group/per-player overrides, hex/gradient/rainbow colors, fake player entries, layout config, and full PlaceholderAPI support.
- **Sidebar Scoreboard System**: Config-driven named boards with conditions, animation, per-group/per-player overrides, a persisted toggle, and true per-viewer rendering.
- **Leaderboard System**: `/leaderboard` (`/lb`) ranks money, any vanilla-tracked stat, or custom admin-defined boards — config-driven, with a `LeaderboardAPI` for other mods and a hologram generator.
- **Localization System**: Set the server language with one config line (`"language": "fr_fr"`). Supports 9 built-in languages with `en_us` fallback for missing keys.

## 📖 Documentation

**Canonical reference for this fork:** [shtanko-michael.github.io/NeoEssentials](https://shtanko-michael.github.io/NeoEssentials/)

Command, permission and config tables are generated from the source on every push to `farmstead-monorepo-build`, so they cannot drift from the code. Worked examples, edge cases and known issues are curated in Russian. Press `/` anywhere on the site to search.

| Page | What it covers |
|---|---|
| [Quick start](https://shtanko-michael.github.io/NeoEssentials/quickstart/) | Install, first permissions, reload vs restart |
| [All commands](https://shtanko-michael.github.io/NeoEssentials/commands/) | Syntax, aliases, real permission node |
| [Permissions](https://shtanko-michael.github.io/NeoEssentials/permissions/) | Every node from the registry |
| [Configuration](https://shtanko-michael.github.io/NeoEssentials/config/) | Every shipped key and its default |
| [Economy](https://shtanko-michael.github.io/NeoEssentials/systems/economy/) | Balances, pay, baltop, ChestShop, Vault |
| [Teleportation](https://shtanko-michael.github.io/NeoEssentials/systems/teleportation/) | Homes, warps, TPA, RTP, spawn |
| [Moderation](https://shtanko-michael.github.io/NeoEssentials/systems/moderation/) | Ban, mute, jail, freeze, vanish |
| [Chat](https://shtanko-michael.github.io/NeoEssentials/systems/chat/) | Formatting, channels, mail, rich text |
| [Kits](https://shtanko-michael.github.io/NeoEssentials/systems/kits/) | Kits, cooldowns, preview |
| [Web dashboard](https://shtanko-michael.github.io/NeoEssentials/systems/dashboard/) | Setup, login, REST API |
| [Permissions system](https://shtanko-michael.github.io/NeoEssentials/systems/permissions/) | Groups, wildcards, LuckPerms, FTB Ranks |
| [Known issues](https://shtanko-michael.github.io/NeoEssentials/troubleshooting/) | Symptom, cause, what to do |

The original English upstream pages are embedded at the bottom of each system page so they keep merging without conflicts. The upstream GitHub wiki itself is **not** kept in sync and may be stale (its own CommandsReference banner says so).

## 🚀 Quick Start

### Installation
1. Download the latest release [![Version](https://img.shields.io/github/v/release/ZeroG-Network-PTY-LTD/NeoEssentials?label=Version)](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/releases)
2. Place the JAR file in your server's `mods` folder
3. Start your server — config files are auto-generated
4. Configure permissions and features as needed
5. Reload with `/neoessentials reload` to apply changes without restarting

### Essential Configuration Files (Split Config Mode — Recommended)
```
config/neoessentials/
├── main.json           # Modules, logging, localization, permissions settings, kits settings, economy settings
├── commands.json       # Enable/disable individual commands
├── chat.json           # Chat formatting, channels, anti-spam, badges
├── teleportation.json  # Homes, warps, spawn, TPA, random TP
├── moderation.json     # Ban, jail, vanish, freeze, kick settings
├── webdashboard.json   # Web dashboard port, auth, UI settings
├── items.json          # Item spawn, enchantments, stack sizes
├── afk.json            # AFK timeout, kick, broadcast messages
├── security.json       # Input validation, unsafe command filter
├── tablist.json        # Animated tablist header/footer/format
├── scoreboard.json     # Sidebar scoreboard boards, conditions, overrides
├── leaderboard.json    # Ranked leaderboard boards (economy/vanilla stat/custom)
├── kits.json           # Kit definitions
├── permissions.json    # Permission groups and nodes
└── .split_configs      # Marker file that activates split mode

neoessentials/
├── languages/custom/   # Active language files (e.g. fr_fr.json)
├── languages/overrides.json  # Admin message overrides
├── playerdata/         # Per-player data
└── ...                 # Homes, warps, moderation data
```

> Run `/neoe config split` to automatically migrate from a single `config.json` to split mode.

### Quick Permission Setup
For LuckPerms users:
```
/lp group admin permission set neoessentials.admin true
/lp group moderator permission set neoessentials.moderator true
/lp group default permission set neoessentials.player true
```

### Setting a Custom Language
In `config/neoessentials/main.json` (or `config.json`):
```json
"localization": {
  "language": "fr_fr"
}
```
Then run `/neoessentials reload`. See [LocalizationSystem](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/LocalizationSystem) for available language codes.

## 🎮 Command Reference

See [CommandsReference](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/CommandsReference) for the full list of 100+ commands with syntax, permissions, and aliases.

Timed jail sentences can be set directly with `/jail <player> <jail> <duration> [reason]`,
for example `/jail Steve spawn-jail 2h griefing`. Supported units are `s`, `m`, `h`, `d`,
and `w`; omitting the duration creates a permanent sentence.

## 🔗 API Integration for Modders

See [APISystem](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/APISystem) for full API and PlaceholderAPI documentation, including:
- Registering custom `{prefix_identifier}` placeholders and expansion groups
- Stat placeholders: `{neoessentials_deaths}`, `{neoessentials_player_kills}`, `{neoessentials_mob_kills}`, `{neoessentials_play_time}`
- Using REST API endpoints for server status, player info, logs, config, and statistics
- Economy API and Permissions API for mod integration

## 🔗 Integration & Compatibility

- **LuckPerms** and **FTB Ranks** supported for permission management
- **Server-Side Only** — no client mod required
- **Vanilla Client Support**
- **Performance Optimized** — dirty-check caching for tablist team packets, configurable refresh intervals

## 🛠️ Development

### IDE Setup
This project uses **IntelliJ IDEA Community Edition** with Gradle.

**Setup Instructions:**
1. Clone the repository
2. Open the project in IntelliJ IDEA
3. Import as a Gradle project
4. Wait for Gradle to sync and download dependencies
5. The IDE is configured to automatically download sources and javadocs

**Running the Mod:**
- Use Gradle run configurations provided by NeoForge ModDev
- `runClient` - Test in client mode
- `runServer` - Test in server mode
- `runData` - Generate data resources

**Building:**
```bash
./gradlew build
```
Output JAR: `build/libs/neoessentials-VERSION+build.XXX.jar`

### Project Structure
- **Java 21** - Target JDK version
- **Gradle** - Build system with auto-incrementing build numbers
- **NeoForge ModDev 2.0.107** - Modern mod development plugin
- **Parchment Mappings** - Better parameter names and javadocs

## 🤝 Support & Community

- **Discord**: [Join our Discord server](https://discord.gg/dUGAQF2Mga) for support and community discussion
- **Bug Reports**: Report issues and bugs through GitHub or Discord
- **Feature Requests**: Suggest new features and improvements
- **Documentation**: See [Wiki Home](https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials/wiki/Home)

## 📄 License

NeoEssentials is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

**🌟 Ready to enhance your server? Download NeoEssentials and give your players the essential tools they need!**

*Made with ❤️ for the Minecraft community*
