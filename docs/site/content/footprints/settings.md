Открыть: через Mod Menu (Fabric) или список модов (NeoForge, Forge), командой `/footprints config` или в файле `config/farmstead_footprints-client.toml`.

| Ключ | По умолчанию | Диапазон | Что делает |
| --- | --- | --- | --- |
| `preset` | `default` | `default`, `vanilla_plus`, `cinematic`, `performance` | Задаёт все значения ниже разом |
| `general.enabled` | `true` | | Главный выключатель |
| `general.checkUpdates` | `true` | | Один раз при запуске проверить новую версию на Modrinth (см. ниже) |
| `footprints.players` | `true` | | Следы игроков, включая ваши |
| `footprints.mobs` | `true` | | Следы животных и монстров |
| `footprints.maxPrints` | `2048` | 64 – 16384 | Сколько следов живёт одновременно; первыми исчезают старые |
| `footprints.renderDistance` | `48` | 8 – 128 | В скольких блоках от камеры следы появляются и рисуются |
| `footprints.lifetimeMultiplier` | `1.0` | 0.1 – 10.0 | Сколько держатся следы |
| `footprints.opacity` | `0.95` | 0.1 – 1.0 | Насколько заметны следы |
| `footprints.entityBlacklist` | летающие и безногие | id, #теги | Кто никогда не оставляет следов |
| `footprints.sizeMultiplier` | `1.0` | 0.25 – 4.0 | Все следы больше или меньше |
| `footprints.lift` | `0.0` | -0.05 – 0.5 | Приподнять следы над землёй (для шейдеров) |
| `footprints.style` | `DETAILED` | `DETAILED`, `PIXEL` | Гладкие следы или вдавленные пиксели самого блока |
| `footprints.sneaking` | `SHALLOW` | `SHALLOW`, `NONE` | Крадущийся оставляет мелкие следы или никаких |
| `footprints.invisible` | `false` | | Невидимые тоже оставляют следы |
| `footprints.profiles` | `[]` | строки | Следы по мобу или #тегу, см. [Свои следы](#свои-следы) |
| `effects.wetPrints` / `wetSeconds` / `wetOpacity` | `true` / `10` / `1.0` | | Мокрые следы на твёрдом после воды или дождя |
| `effects.splash` | `ALL` | `OFF`, `PLAYERS`, `ALL` | Брызги с мокрых ног |
| `effects.ripples` / `boatWake` / `swimBubbles` / `railSparks` | `true` | | Эффекты на воде и рельсах |
| `effects.removeWhenBroken` | `true` | | След исчезает вместе с блоком |
| `effects.decay` | `CRUMBLE` | `CRUMBLE`, `FADE` | Как исчезают старые следы |
| `effects.rainWear` | `3.0` | 1 – 20 | Во сколько раз быстрее дождь стирает следы |
| `effects.dust` | `ALL` | `OFF`, `PLAYERS`, `ALL` | Облачко из-под ног на рыхлом грунте |
| `surfaces.hardnessGate` | `0.7` | 0 – 5 | Следы оставляет любой блок мягче этого |
| `surfaces.include` / `exclude` | шерсть / листва, лёд, … | id, #теги | Блоки, которые всегда / никогда не принимают следы |

Пресеты: **vanilla_plus** — без следов мобов, вдвое короче и бледнее; **cinematic** — 8192 следа, 96 блоков, втрое дольше; **performance** — 512 следов, 24 блока, вдвое короче.

В файле ключи, которые следуют пресету, записаны закомментированными (`#maxPrints = 2048`). Уберите `#`, чтобы задать значение самому, — тогда оно важнее пресета. Ошибочное значение попадает в лог (и в чат после `/footprints reload`), а вместо него берётся значение пресета: из-за конфига игра не падает.
