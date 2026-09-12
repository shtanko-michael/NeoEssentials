## Установка

Farmstead NeoEssentials работает **только на сервере** — игрокам ничего ставить не нужно, ванильный
клиент подключается как обычно.

1. Положите `neoessentials-<версия>.jar` в папку `mods/` сервера.
2. Запустите сервер один раз и остановите. Мод создаст `config/neoessentials/` со всеми
   конфигурационными файлами и файлами данных.
3. Откройте `config/neoessentials/config.json` и пройдитесь по секции `modules` — там
   включаются и выключаются подсистемы целиком.

> Переключатели в `modules` и `commands` читаются **один раз при старте**: команда либо
> регистрируется, либо нет. `/neoessentials reload` обновляет только значения настроек, он не
> может добавить или убрать уже зарегистрированную команду. После правки этих двух секций
> нужен перезапуск сервера.

## Язык сервера

Язык задаётся одним ключом в `config.json`:

```json
"localization": {
  "language": "ru_ru"
}
```

После `/neoessentials reload` сервер начнёт отвечать по-русски. Непереведённые строки
автоматически откатываются на английский, так что сломать вывод неверным кодом языка нельзя.
Полнота перевода по языкам — на странице [Локализация](../localization/index.html).

## Первая выдача прав

По умолчанию почти всё закрыто. Базовый набор для обычного игрока и для модератора выглядит так
(пример для LuckPerms):

```
/lp group default permission set neoessentials.use true
/lp group default permission set neoessentials.teleport.home.home true
/lp group default permission set neoessentials.teleport.spawn true
/lp group default permission set neoessentials.economy.balance true

/lp group moderator permission set neoessentials.moderation.mute true
/lp group moderator permission set neoessentials.moderation.kick true
/lp group moderator permission set neoessentials.moderation.jail true

/lp group admin permission set neoessentials.admin true
```

Полный список узлов с описаниями — на странице [Права](../permissions/index.html). Какое право
проверяет конкретная команда, видно в колонке «Право» на странице
[Все команды](../commands/index.html), а в игре — по `/help <команда>`.

> `neoessentials.admin` и wildcard `neoessentials.*` — не одно и то же. Wildcard намеренно **не**
> выдаёт градуированные экономические узлы-модификаторы, если не включить
> `permissions.wildcardsGrantEconomyModifierPermissions`. Это защита от случайной выдачи права
> менять чужие балансы вместе с общим доступом.

## Проверка, что всё живо

| Команда | Что подтверждает |
|---|---|
| `/neoessentials` | мод загрузился, показывает версию и номер сборки |
| `/help` | реестр команд собрался, показывает только доступное вам |
| `/neoessentials reload` | конфиги читаются без ошибок |
| `/balance` | включена и проинициализирована экономика |

Если команда есть в этом справочнике, но не работает в игре — сначала проверьте, включена ли её
подсистема в `modules`, а затем не выключена ли сама команда в секции `commands` конфига.

## Куда дальше

Разделы по системам в меню слева устроены одинаково: сверху команды и рабочие примеры, ниже
тонкие места, затем известные проблемы, и в самом конце — подробное руководство оригинального
проекта на английском.
