# Публикация Farmstead NeoEssentials на Modrinth и CurseForge

Всё, что нужно ввести руками при создании страниц мода, и что потом делает CI сам.
Тексты страницы — [description.md](description.md), заметки к версии —
[releases/](releases/) (`<версия>.md`, CI без них не выпускает).

## Поля страницы

| Поле | Значение |
|---|---|
| Название | `Farmstead NeoEssentials` |
| Slug / URL | `farmstead-neoessentials` (на Modrinth свободен; `neoessentials` занят оригиналом) |
| Краткое описание (summary) | `Essentials for NeoForge 1.21.1 servers: homes, warps, TPA, economy, kits, moderation, chat, tablist. A maintained fork of NeoEssentials with full Russian localization.` |
| Описание | содержимое [description.md](description.md) целиком |
| Категории Modrinth | `management`, `utility`, `economy` (дополнительно: `social`) |
| Категории CurseForge | Server Utility, Miscellaneous |
| Клиент / сервер | клиент: **не нужен** (unsupported), сервер: **обязателен** |
| Загрузчик / версия MC | NeoForge, 1.21.1 |
| Лицензия | MIT, ссылка на `LICENSE` в репозитории |
| Исходники | https://github.com/shtanko-michael/NeoEssentials |
| Баг-трекер | https://github.com/shtanko-michael/NeoEssentials/issues |
| Вики | https://shtanko-michael.github.io/NeoEssentials/ |
| Discord | свой, если есть; **не** Discord ZeroG |
| Владелец | организация **Farmstead** на Modrinth (создать один раз, туда же — AI Builder) |

### Раскрытие ИИ (Modrinth, правила §6)

Код форка в значительной части написан с ИИ-ассистентом, поэтому при создании проекта ставим
отметку **«Contains AI-generated content»** (§6.1: «a substantial portion of the project's code is
a product of AI output»). Не отметить — повод для снятия проекта.

### Иконка и галерея

Готовые, в [images/](images/): `icon.png` (512×512), галерея 1920×1080 — `gallery-1-hero`,
`gallery-2-chat` (английский и русский чат — строки из `data/lang`), `gallery-3-tablist`,
`gallery-4-docs` (снимок справочника), `gallery-5-fork` — и `social.png` (1280×640, превью
репозитория: Settings → Social preview). Фоны — кадры сервера Farmstead из `video/public/footage/`
монорепо. Рабочий вариант для запуска, владелец заменит своими.

Пересобрать после правки `images/src/slides.html`: каждая `<section data-out>` — отдельная картинка,
снимок элемента Playwright'ом. Шрифты берутся из `client/landing/public/fonts` монорепо по
относительному пути — в отдельном чекауте форка их нет.

## Первый выпуск — по шагам

1. Создать организацию Farmstead на Modrinth и проект по таблице выше (статус Draft).
   Загрузить первую версию CI не может, пока проекта нет, — поэтому порядок именно такой.
2. В репозитории `shtanko-michael/NeoEssentials` → Settings → Secrets and variables → Actions:
   - секрет `MODRINTH_TOKEN` — PAT Modrinth со scope *Create versions*;
   - переменная `MODRINTH_PROJECT_ID` — ID проекта (Settings проекта на Modrinth);
   - то же для CurseForge: `CURSEFORGE_TOKEN` (API token из профиля) и `CURSEFORGE_PROJECT_ID`.
   Без пары «секрет + переменная» соответствующая площадка просто пропускается.
3. Проверить `mod_version` в `gradle.properties` и написать `docs/publish/releases/<версия>.md`.
4. Поставить тег на `farmstead-monorepo-build` и запушить:
   `git tag farmstead-v1.0.6-fs1 && git push origin farmstead-v1.0.6-fs1`.
5. Workflow `Release Farmstead NeoEssentials` соберёт `-all` jar, создаст GitHub-релиз и загрузит
   версию на Modrinth/CurseForge. После — отправить проект Modrinth на модерацию (Submit for review).

## Версии

`<mod_version апстрима>-fs<N>`: `1.0.6-fs1`, `1.0.6-fs2`… После синка с апстримом, поднявшего
`mod_version` до 1.0.7, следующий выпуск — `1.0.7-fs1`. Workflow отказывается собирать тег, чья база
не совпадает с `mod_version`.

## Чего на публичной странице быть не должно

- Ссылок на Discord / Ko-fi ZeroG как на «нашу поддержку» — только в разделе Credits.
- Раздела справочника про RegionGuard как части мода: сам RegionGuard в этот выпуск не входит.
