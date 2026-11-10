## Две двери, достаточно любой

У каждой подкоманды два способа пройти проверку:

1. **Vanilla permission level** из `config/farmstead_aibuilder.toml`, секция `[permissions]`: `0` — все, `2` — операторы (уровень `/give`, `/setblock`), `4` — владелец/консоль.
2. **Нода** `aibuilder.command.*`, выданная менеджером прав.

По умолчанию почти все уровни — `0`, команды открыты всем: настоящий ограничитель — ваши дневные лимиты в кабинете, а не права. Исключение — `status_level = 2`: `/aibuild status` — экран администратора.

> **Порядок важен.** Нода при `level = 0` ничего не меняет: уровень 0 проходят все, и до проверки ноды дело не доходит. Сначала поднимите уровень, потом выдавайте ноду.

Мод объявляет свои ноды в NeoForge Permission API, поэтому **LuckPerms видит их сам** — ставить ничего дополнительно не нужно.

## Генерация только для группы

Типовой сценарий «генерация только донатерам, остальное всем»:

```toml
# config/farmstead_aibuilder.toml
[permissions]
create_level = 4   # закрыли create и area от всех, кроме консоли
area_level   = 4
```

```text
/lp creategroup builder
/lp group builder permission set aibuilder.command.create true
/lp group builder permission set aibuilder.command.area true
/lp user Steve parent add builder
```

Разово одному игроку: `/lp user Steve permission set aibuilder.command.create true`. Забрать обратно: `/lp group builder permission unset aibuilder.command.create` — тогда снова решает уровень.

## Все ноды и уровни

| Нода | Ключ уровня | По умолчанию | Подкоманды |
|---|---|---|---|
| `aibuilder.command.create` | `create_level` | `0` | `create` |
| `aibuilder.command.area` | `area_level` | `0` | `area`, `area build`, `area pos1/pos2/clear` |
| `aibuilder.command.list` | `list_level` | `0` | `list` |
| `aibuilder.command.get` | `get_level` | `0` | `get` |
| `aibuilder.command.placed` | `placed_level` | `0` | `placed` |
| `aibuilder.command.undo` | `undo_level` | `0` | `undo`, `remove` |
| `aibuilder.command.upgrade` | `upgrade_level` | `0` | `upgrade` |
| `aibuilder.command.balance` | `balance_level` | `0` | `balance` |
| `aibuilder.command.topup` | `topup_level` | `0` | `topup` |
| `aibuilder.command.status` | `status_level` | `2` | `status` |

`undo` и `remove` делят одну ноду: это одна власть над одними записями.

## Без менеджера прав

Ноды никто не выдаёт, и весь гейт — уровни из конфига. Операторы проходят всё: `/op` даёт уровень 4, а он перекрывает любой уровень ниже (см. `op-permission-level` в `server.properties`).
