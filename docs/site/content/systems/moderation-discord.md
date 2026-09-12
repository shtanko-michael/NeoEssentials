## Как устроена интеграция

NeoEssentials **сам в Discord не ходит** за ботом: нет своего токена и gateway. Есть четыре независимых куска:

1. **Мосты чата и событий** — адаптеры SDLink, Mc2Discord, DCIntegration. Забирают уже связанный аккаунт у моста и шлют чат / join / leave / mute / AFK / advancement / личку в Discord.
2. **Вебхуки** — `WebhookAdapter`: обычный HTTPS POST на URL вебхука канала. Мост не нужен. Только Minecraft → Discord.
3. **Роли Discord → группа Minecraft** — `discordrolesync.json` и DCIntegration. Пишет во внутренний менеджер прав NeoEssentials.
4. **Вход в веб-панель по Discord** — `discord_auth.json` (`roleMapping` на ADMIN/MODERATOR/VIEWER). Это роли панели, не игрока на сервере. Команды панели (`/dashboardregister discord`, `/linkaccount`) на этой странице не разбираются.

Команд линковки у NeoEssentials нет. Игрок связывает Minecraft и Discord командами моста.

## Мосты

Порядок регистрации: SDLink → Mc2Discord → DCIntegration → Generic Webhook. Событие рассылается **всем** готовым адаптерам.

| Мост | Чат и события | Линк аккаунта | Список ролей Discord |
|---|---|---|---|
| Simple Discord Link (SDLink) | да | да | нет (публичное API не отдаёт) |
| Mc2Discord | да | да | нет |
| DCIntegration | да, в основном явный `channelId` | да | да |
| Generic Webhook | да, если задан `webhookUrl` | нет | нет |

Чат настраивается **внутри канала** NeoEssentials: `chat.channels.<имя>.discord.enabled` / `channelId` / `webhookUrl`. Не-чат — сверху `config.json` в `discordEventChannels`. Команды каналов (`/g`, `/staff`, …) принадлежат системе каналов чата, не этой странице.

`modules.discordIntegrationEnabled: false` на старте пропускает `ChatIntegrationManager.initialize()` — не будет ни мостов, ни вебхуков, пока не включите флаг и не перезапустите.

## Вебхуки

URL берётся из того же `webhookUrl` рядом с `channelId`. Вебхук, добавленный через `/neoe reload`, начинает работать без рестарта — адаптер не кеширует URL. Поставить сам мод-мост по-прежнему можно только рестартом.

Обратного направления нет, ролей и линка нет, `sendToChannel` по snowflake id вебхук не умеет (дашбордный «test message» через него не уйдёт).

## Двойная отправка

У SDLink и Mc2Discord есть **свой** релей, параллельный NeoEssentials. На старте NeoEssentials читает их toml и для default-route глушит свои дубли, оставляя WARN. Явный `channelId` не глушится. Смена toml без рестарта детектор не перечитывает.

## Роли → группы

Файл `discordrolesync.json`, задача `DiscordRoleSyncTask`. Snowflake роли → имя группы из `permissions.json`. Несколько ролей — побеждает группа с большим `priority`. Только повышение. Только внутренний PermissionManager. Только DCIntegration как источник ролей. Обход по таймеру + сразу на join.
