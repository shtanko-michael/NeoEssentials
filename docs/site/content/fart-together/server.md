## Серверный конфиг

`config/farmstead_fart_together-server.toml`, пресеты `default`, `family` (кулдаун длиннее, без эффектов), `plain` (без личных стилей), `cosmetic` (только украшение: газ не нужен, мобы не пугаются, у облака нет эффектов, реактивный прыжок не подбрасывает, комбо без подсветки — залп только выглядит и звучит), `chaos` (частые залпы, большие облака с тошнотой, которые достают других игроков, сильный реактивный прыжок, окно комбо шире).

| Секция | Ключи |
| --- | --- |
| `toot` | `cooldownSeconds` (3), `fullChargeSeconds` (1,5: за сколько удержание выпускает весь газ) |
| `charge` | `required`, `max` (100), `minToToot` (10: столько же выпускает короткое нажатие), `passivePerMinute`, `unlistedFood`, `goldenApple` |
| `cloud` | `enabled`, `durationSeconds`, `radius`, `scareMobs`, `scareRange`, `scaredMobs` (id или `#теги`), `effects` (`"minecraft:nausea 4 1"`), `affectsPlayers` (выкл.: без PvP), `respectServerPvp` |
| `jetJump` | `enabled`, `strength` (0,6), `slowFallSeconds` (1,5) |
| `together` | `enabled`, `windowSeconds` (1,5), `radius` (6), `glow` |
| `items` | `enabled` (выделенный сервер, перезапуск), `beansFromGrass`, `cabbageSeedsFromGrass`, `beansInVillagerTrades`, `beansInLoot` |
| `style` | `allowCustom`, `maxSize`, `maxAmount`, `maxDuration`, `allowedParticles`, `allowedSounds`, `updateCooldownSeconds` |

Веса газа — в `data/<namespace>/gas_foods/*.json`, например `{"foods": {"minecraft:beetroot": 10, "#c:foods/vegetable": 4}}`. Датапак меняет наши, положив файл по тому же пути (`data/farmstead_fart_together/gas_foods/vanilla.json`), или добавляет свой; применяется `/reload` или `/fart reload`.

## Права

LuckPerms или любой мод прав лоадера. Проверено вживую с LuckPerms на Fabric и NeoForge. На NeoForge 1.21.1 ставьте LuckPerms 5.4.150 или новее с [luckperms.net](https://luckperms.net/download): 5.4.140, последняя сборка для NeoForge на Modrinth, выкидывает каждого игрока при входе («Invalid player data») (ошибка LuckPerms, исправлена в более новых сборках).

| Узел | По умолчанию | Что даёт |
| --- | --- | --- |
| `farmstead_fart_together.use` | все | пускать газы |
| `farmstead_fart_together.jetjump` | все | залп в воздухе подбрасывает |
| `farmstead_fart_together.together` | все | участвовать в комбо Fart Together |
| `farmstead_fart_together.style.custom` | все | личный стиль (иначе простое зелёное облачко) |
| `farmstead_fart_together.style.rainbow` | никто | градиенты и радуга |
| `farmstead_fart_together.style.huge` | никто | потолки размера и длительности ×1,5 |
| `farmstead_fart_together.style.sounds.rare` | никто | редкие звуки по выбору (пока нет) |
| `farmstead_fart_together.cloud.bypass` | никто | эффекты облака не действуют |
| `farmstead_fart_together.command.info` | все | `/fart info` |
| `farmstead_fart_together.command.reload` | операторы | `/fart reload` |
| `farmstead_fart_together.command.charge` | операторы | `/fart charge` |
| `farmstead_fart_together.command.toggle` | операторы | `/fart toggle` |

| Meta (число) | По умолчанию | Что задаёт |
| --- | --- | --- |
| `farmstead_fart_together.cooldown` | `toot.cooldownSeconds` | секунд между залпами |
| `farmstead_fart_together.cloud.radius` | `cloud.radius` | радиус облака полного залпа |
| `farmstead_fart_together.charge.max` | `charge.max` | размер шкалы газа |

Пример: `/lp group vip meta set farmstead_fart_together.cooldown 1`. На NeoForge и Forge meta читается через API прав лоадера (строковые узлы), на Fabric — через options из fabric-permissions-api.
