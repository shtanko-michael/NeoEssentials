## Серверный конфиг

Главные настройки находятся в `config/regionguard-server.toml`:

```toml
[general]
allowFallbackSelection = true
enablePixelmonCompat = true
language = "ru_ru"
maxRegionsPerPlayer = 3
defaultDeniedFlagsOnClaim = ["build", "block-break", "block-place", "chest-access", "tnt"]
defaultAllowedFlagsOnClaim = ["interact", "use", "entry", "pvp", "mob-spawning", "mob-entry", "passive-entity-damage", "fall-damage", "fire-spread", "creeper-explosion", "other-explosion", "thunder", "pixelmon-spawn", "pixelmon-entry", "pixelmon-player"]
```

| Ключ | По умолчанию | Назначение |
|---|---:|---|
| `allowFallbackSelection` | `true` | Разрешить внутреннее хранилище выделения, если WorldEdit отсутствует. В текущей версии нет команды, заполняющей это хранилище. |
| `enablePixelmonCompat` | `true` | Включить мост Pixelmon, если сам Pixelmon установлен. |
| `language` | `ru_ru` | Язык сообщений: `ru_ru` или `en_us`. Неизвестное значение откатывается на английский. |
| `maxRegionsPerPlayer` | `3` | Лимит регионов владельца; `-1` означает без лимита. Может быть переопределён meta-ключом LuckPerms. |
| `defaultDeniedFlagsOnClaim` | список | Булевы флаги, записываемые как `false` при создании региона. |
| `defaultAllowedFlagsOnClaim` | список | Булевы флаги, записываемые как `true` после deny-списка. При повторе побеждает allow. |

Неизвестные ID и небулевы флаги в списках пропускаются с предупреждением. ID нормализуются: регистр не важен, `_` превращается в `-`.

## Дополнительный JSON

`config/RegionGuard/config.json` содержит совместимые с ранней версией поля. Реально используется `enablePixelmonCompat`, дублирующий серверный TOML. Поля `createFallbackSelection`, `enableWorldEditBridge` и `localeOverridesDenyMessage` сейчас не читаются обработчиками.

## Локализация

Редактируемые языковые файлы создаются при первом запуске:

```text
config/RegionGuard/lang/ru_ru.json
config/RegionGuard/lang/en_us.json
```

Поиск перевода идёт от дискового файла к встроенному выбранному языку, затем к встроенному `en_us` и, наконец, к самому ключу. Изменения применяются после рестарта или перезагрузки конфигурации NeoForge.

Строковые флаги `greeting`, `farewell` и `deny-message` относятся к конкретному региону и не являются ключами языкового каталога.

## Хранение регионов

Для каждого измерения создаётся отдельный JSON:

```text
<world>/regionguard/regions/minecraft_overworld.json
<world>/regionguard/regions/minecraft_the_nether.json
```

Файл содержит плоский массив регионов: ID, минимальную и максимальную точки, приоритет, parent, UUID владельцев и участников и установленные флаги.

Перед ручным редактированием остановите сервер и сделайте резервную копию. Команды не позволяют менять приоритет и parent, поэтому эти поля доступны только через JSON.

## Интеграции

**WorldEdit** обнаруживается без жёсткой compile-time зависимости и предоставляет выделение для `/rg define` и `/rg redefine`.

**NeoEssentials** является необязательным мостом к PermissionAPI и LuckPerms. При его отсутствии проверка внешних нод безопасно возвращает отказ, а OP-доступ продолжает работать.

**Pixelmon** обнаруживается во время запуска. Флаги `pixelmon-*` применяются только когда интеграция включена и классы Pixelmon доступны.
