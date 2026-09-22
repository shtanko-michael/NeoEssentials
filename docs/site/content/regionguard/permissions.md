Права команд проверяются через NeoEssentials `PermissionAPI`, который передаёт запрос установленному адаптеру, обычно LuckPerms. Без NeoEssentials или permission-плагина управление остаётся доступно OP уровня 2 и выше.

## Командные права

| Нода | Доступ |
|---|---|
| `regionguard.command` | Обязательный корневой доступ ко всем подкомандам `/rg`. |
| `regionguard.command.define` | `/rg define` и `/rg claim`. |
| `regionguard.command.redefine` | `/rg redefine`. |
| `regionguard.command.remove` | `/rg remove` и `/rg delete`. |
| `regionguard.command.list` | `/rg list`. |
| `regionguard.command.info` | `/rg info` и `/rg size` для собственных регионов и регионов, где игрок — участник. |
| `regionguard.command.tp` | `/rg tp` и кнопка телепортации в списках. |
| `regionguard.command.members` | Добавление, удаление и очистка владельцев и участников. |
| `regionguard.command.flag` | Вход в ветку `/rg flag`. |
| `regionguard.command.flag.<flag-id>` | Просмотр и изменение конкретного флага. |

Обычному игроку нужны сразу два слоя: `regionguard.command` и нода нужной подкоманды. Для флага нужен третий слой — `regionguard.command.flag.<flag-id>`.

Для флагов безопасности и сообщений используются следующие точные ноды:

| Нода | Флаг |
|---|---|
| `regionguard.command.flag.mob-damage` | `mob-damage` |
| `regionguard.command.flag.invincibility` | `invincibility` |
| `regionguard.command.flag.passive-entity-damage` | `passive-entity-damage` |
| `regionguard.command.flag.fall-damage` | `fall-damage` |
| `regionguard.command.flag.item-drop` | `item-drop` |
| `regionguard.command.flag.potion-splash` | `potion-splash` |
| `regionguard.command.flag.greeting` | `greeting` |
| `regionguard.command.flag.farewell` | `farewell` |

## Рекомендованный набор для `default`

Владелец региона всё равно не может менять чужой регион. Для обычной роли разумно выдать базовые команды и безопасные флаги своего привата, но не `invincibility`, управление спавном мобов, маяком и взрывами:

```text
regionguard.command
regionguard.command.define
regionguard.command.redefine
regionguard.command.remove
regionguard.command.list
regionguard.command.info
regionguard.command.tp
regionguard.command.members
regionguard.command.flag
regionguard.command.flag.greeting
regionguard.command.flag.farewell
regionguard.command.flag.deny-message
regionguard.command.flag.weather
regionguard.command.flag.entry
regionguard.command.flag.interact
regionguard.command.flag.use
regionguard.command.flag.chest-access
regionguard.command.flag.pvp
regionguard.command.flag.passive-entity-damage
regionguard.command.flag.fall-damage
regionguard.command.flag.item-drop
regionguard.command.flag.potion-splash
```

Пример минимального просмотра собственных регионов:

```text
regionguard.command
regionguard.command.list
regionguard.command.info
```

Полное управление всеми флагами:

```text
regionguard.command
regionguard.command.flag
regionguard.command.flag.*
```

`regionguard.command.*` не включает саму ноду `regionguard.command`. Если нужен полный wildcard, используйте `regionguard.*` либо выдавайте корневую ноду отдельно.

## Владение важнее командной ноды

Командная нода разрешает вызвать команду, но не даёт управление чужим регионом. Менять границы, удалять регион, редактировать участников и флаги может только владелец либо OP уровня 2. Исключение — специально выданная нода `regionguard.admin.*` из таблицы ниже.

Участник обходит строительную защиту, но не получает права управления. Последнего владельца нельзя удалить через `/rg delowner`; используйте осознанное удаление региона или `/rg clearowners`.

## Модерация без `/op`

Ноды этого раздела работают только в RegionGuard: они не выдают vanilla-операторские команды и не дают доступ к другим модам. Каждая нода сама открывает нужную ветку `/rg`, поэтому вместе с ней не требуются `regionguard.command` и обычная командная нода.

| Нода | Доступ к чужим приватам |
|---|---|
| `regionguard.admin.access` | Вход, строительство, взаимодействия и контейнеры; обход `item-drop`, `potion-splash`, защиты пассивных существ и `pvp deny`; `/bb` в чужом привате. Экологические правила — взрывы, погода, спавн и подобные — не обходятся. |
| `regionguard.admin.members` | Добавлять и удалять владельцев/участников, очищать оба списка. |
| `regionguard.admin.flags` | Смотреть и менять все флаги без отдельных `regionguard.command.flag.<flag-id>`. |
| `regionguard.admin.remove` | Удалять регионы. |
| `regionguard.admin.redefine` | Менять границы регионов через `/rg redefine`. |
| `regionguard.admin.list` | Видеть все регионы текущего измерения через `/rg list`. |
| `regionguard.admin.info` | Смотреть карточку любого региона через `/rg info <id>`, выводить его сетку через `/rg s <id>` и видеть владельцев/участников палкой. |
| `regionguard.admin.tp` | Телепортироваться в любой регион и видеть кнопки телепортации. |

Полный набор для группы модераторов:

```text
/lp group moderators permission set regionguard.admin.* true
```

## Права обхода

В реестре объявлены:

```text
regionguard.bypass.build
regionguard.bypass.interact
regionguard.bypass.entry
regionguard.bypass.pvp
regionguard.bypass.mob-spawning
regionguard.bypass.mob-entry
```

В текущей версии к проверке защиты подключён только `regionguard.bypass.pvp`. Он позволяет атакующему игнорировать `pvp deny`. Остальные ноды сохранены, но поведение защиты пока не меняют: соответствующие обходы определяются статусом владельца или участника. Для модерации без OP используйте `regionguard.admin.access`, а не эти заготовленные bypass-ноды.

## Лимит приватов

`regionguard.regions` — meta-ключ LuckPerms, а не permission-нода. Он задаёт максимальное число регионов игрока суммарно по всем измерениям:

```text
/lp group default meta set regionguard.regions 3
/lp group vip meta set regionguard.regions 10
/lp user Steve meta set regionguard.regions 25
```

Если meta отсутствует, используется `maxRegionsPerPlayer` из `regionguard-server.toml`. Значение `-1` снимает лимит. Существующие регионы при уменьшении лимита не удаляются.

## OP и консоль

OP уровня 2 и выше обходит командные проверки, ограничения владения и лимит регионов. Он также может войти в любой регион и обходит запреты на строительство, взаимодействия, контейнеры, урон пассивным существам, выбрасывание предметов и зелья. Для PvP такой OP также обходит `pvp deny`. Экологические правила и защитные эффекты зоны (`invincibility`, `fall-damage`, `mob-damage`, взрывы и т.п.) продолжают действовать. Консоль и командные блоки могут выполнять подходящие команды, но команды, которым нужен объект игрока или его выделение, требуют игрового контекста.
