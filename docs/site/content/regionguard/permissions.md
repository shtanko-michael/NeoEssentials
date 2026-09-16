Права команд проверяются через NeoEssentials `PermissionAPI`, который передаёт запрос установленному адаптеру, обычно LuckPerms. Без NeoEssentials или permission-плагина управление остаётся доступно OP уровня 2 и выше.

## Командные права

| Нода | Доступ |
|---|---|
| `regionguard.command` | Обязательный корневой доступ ко всем подкомандам `/rg`. |
| `regionguard.command.define` | `/rg define` и `/rg claim`. |
| `regionguard.command.redefine` | `/rg redefine`. |
| `regionguard.command.remove` | `/rg remove` и `/rg delete`. |
| `regionguard.command.list` | `/rg list`. |
| `regionguard.command.info` | `/rg info`. |
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
| `regionguard.command.flag.item-drop` | `item-drop` |
| `regionguard.command.flag.potion-splash` | `potion-splash` |
| `regionguard.command.flag.greeting` | `greeting` |
| `regionguard.command.flag.farewell` | `farewell` |

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

Командная нода разрешает вызвать команду, но не даёт управление чужим регионом. Менять границы, удалять регион, редактировать участников и флаги может только владелец либо OP уровня 2.

Участник обходит строительную защиту, но не получает права управления. Последнего владельца нельзя удалить через `/rg delowner`; используйте осознанное удаление региона или `/rg clearowners`.

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

В текущей версии к проверке защиты подключён только `regionguard.bypass.pvp`. Он позволяет атакующему игнорировать `pvp deny`. Остальные ноды сохранены, но поведение защиты пока не меняют: соответствующие обходы определяются статусом владельца или участника.

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
