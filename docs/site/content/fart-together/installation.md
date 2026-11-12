## Загрузки и версии

| Minecraft | Fabric / Quilt | NeoForge | Forge | Java |
| --- | --- | --- | --- | --- |
| 26.1 – 26.3 | да | да | — | 25 |
| 1.21.7 – 1.21.11 | да | да | — | 21 |
| 1.21 – 1.21.5 | да | да | — | 21 |
| 1.20.1 | да | — | да | 17 |

1.21.6 не поддерживается.

Скачать: [Modrinth](https://modrinth.com/mod/farmstead-fart-together), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/farmstead-fart-together), [GitHub Releases](https://github.com/shtanko-michael/farmstead-fart-together/releases).

**Что нужно:** на NeoForge и Forge — ничего. **На Fabric мод требует [Fabric API](https://modrinth.com/mod/fabric-api).** [Mod Menu](https://modrinth.com/mod/modmenu) — по желанию, добавляет кнопку настроек на Fabric.

**Сервер или клиент?** Оба. Ставьте на сервер и на каждый клиент. Клиент на сервере без мода всё равно пускает газы, но только для себя. Игроки без мода ничего не видят; войти они могут, но бобы и капуста — незнакомый их игре контент: серверу с ванильными игроками стоит поставить `items.enabled = false` (перезапуск) — тогда мод ничего не добавляет в реестры.

На 26.3 бобы в сундуки пока не добавляются (26.3 переписал построители лута). На 26.x торги жителей — это данные, и `items.beansInVillagerTrades` их не выключает; выключает датапак, который убирает наши записи из `data/minecraft/tags/villager_trade/farmer/level_1.json` и `level_2.json`.

## Проверка обновлений

Раз за запуск мод спрашивает Modrinth, нет ли новой версии, и пишет одну строку в лог (её же показывает вкладка «О моде»). Больше ничего не отправляется. Выключается `general.checkUpdates = false` или `-Dfarmstead.updateCheck=false` (сервер).
