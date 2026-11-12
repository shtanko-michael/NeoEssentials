Открываются из Mod Menu (Fabric) или списка модов (NeoForge, Forge), командой `/farttogether config` или в файле `config/farmstead_fart_together-client.toml`. Пресеты: `default`, `family`, `performance`.

| Ключ | По умолчанию | Что делает |
| --- | --- | --- |
| `general.poses` | `true` | Показывать позу |
| `general.gasMeter` | `true` | Шкала газа над едой |
| `general.streamerSafe` | `false` | Все звуки — мягкий «пуф» |
| `general.checkUpdates` | `true` | Раз за запуск спросить Modrinth о новой версии (см. ниже) |
| `sounds.volume` | `1.0` | Громкость залпов (0–2) |
| `sounds.mutedForOthers` | `false` | Не слышать чужие залпы |
| `particles.amount` | `1.0` | Количество частиц во всём, что вы видите (0–3) |
| `particles.lingeringClouds` | `true` | Рисовать остающиеся облака |
| `particles.maxPerToot` | `200` | Потолок частиц чужого залпа |
| `style.others` | `theirs` | Чужие залпы: `theirs` (их стиль), `simple`, `mine` (ваш стиль) |
| `style.color` | `swamp` | Имя пресета, `#RRGGBB`, `gradient:#A,#B`, `random` или `rainbow` |
| `style.particle` | `puff` | `puff`, `bubbles`, `sparks`, `leaves`, `notes`, `hearts`, `snow` |
| `style.size` / `amount` / `spread` / `duration` | `1.0` | Множители (сервер их ограничивает) |
| `style.sound` | `random` | `random` (по силе) или `short`, `squeaky`, `bubbly`, `long`, `trumpet`, `tiny`, `rumble`, `puff` |
| `style.pitch` / `style.volume` | `1.0` | Тон 0,5–2, громкость 0–1 |
| `style.pose` | `classic` | `classic`, `hero`, `none` |

Клавиша — в «Управление» → «Разное» («Залп (держите — сильнее)»).
