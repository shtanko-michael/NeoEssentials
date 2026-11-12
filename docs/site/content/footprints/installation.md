## Загрузка и версии

| Minecraft | Fabric / Quilt | NeoForge | Forge | Java |
| --- | --- | --- | --- | --- |
| 26.1 – 26.3 | да | да | — | 25 |
| 1.21.7 – 1.21.11 | да | да | — | 21 |
| 1.21 – 1.21.5 | да | да | — | 21 |
| 1.20.1 | да | — | да | 17 |

На 26.x пена за лодкой и круги на воде — частицы (игра там рисует свою геометрию раньше воды). 1.21.6 не поддерживается.

Скачать: [Modrinth](https://modrinth.com/mod/farmstead-footprints), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/farmstead-footprints) или [релизы на GitHub](https://github.com/shtanko-michael/farmstead-footprints/releases).

**Что нужно: ничего.** Ни одной библиотеки, даже Fabric API на Fabric: достаточно самого jar. [Mod Menu](https://modrinth.com/mod/modmenu) не обязателен, он добавляет кнопку настроек на Fabric.

**Клиент или сервер?** Только клиент. Серверу мод не нужен, с ним можно заходить на любой сервер. На выделенном сервере он только пишет строку о запуске в лог и проверяет обновления.

## Проверка обновлений и приватность

При запуске игры Footprints пишет в лог одну строку:

```
[Farmstead] Footprints 0.1.0 started (neoforge, Minecraft 1.21.1) - https://modrinth.com/mod/farmstead-footprints
```

Затем один раз за сессию, в фоне, он спрашивает публичный API Modrinth, нет ли более новой версии для вашего загрузчика и версии Minecraft. Если есть — пишет об этом в лог со ссылкой на скачивание. В запросе нет никаких данных о вас, вашем мире или сервере; ничего не скачивается и не устанавливается. Выключить: `general.checkUpdates = false`, а для всех модов Farmstead сразу — флаг JVM `-Dfarmstead.updateCheck=false` или переменная окружения `FARMSTEAD_UPDATE_CHECK=false`.
