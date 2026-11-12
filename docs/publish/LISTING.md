# Публикация Farmstead NeoEssentials на Modrinth и CurseForge

Всё, что вводится руками на страницах мода, и что делает CI сам. Тексты и настройки страниц —
в [`listing/`](../../listing/): [modrinth.json](../../listing/modrinth.json) (summary, категории,
стороны, ссылки, лицензия, иконка, галерея с подписями), [modrinth.md](../../listing/modrinth.md) и
[curseforge.md](../../listing/curseforge.md) (описание, английский Markdown: баннер, картинки
таблицей в две колонки с жирной подписью под каждой). Заметки к версии — [releases/](releases/)
(`<версия>.md`, CI без них не выпускает).

## Поля страницы

| Поле | Значение |
|---|---|
| Название | `Farmstead NeoEssentials` |
| Slug / URL | `farmstead-neoessentials` (на Modrinth свободен; `neoessentials` занят оригиналом) |
| Краткое описание (summary) | `Essentials for NeoForge 1.21.1 servers: homes, warps, TPA, economy, kits, moderation, chat and an animated tablist. A maintained fork of NeoEssentials with complete Russian localization and nine languages. Server-side only.` (как в [modrinth.json](../../listing/modrinth.json); CurseForge — тот же текст) |
| Описание | Modrinth — ставит workflow `Listing`; CurseForge — вставить [curseforge.md](../../listing/curseforge.md) руками (тип описания Markdown) |
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

Готовые, в [images/](images/): `icon.png` (512×512), галерея 1920×1080 — `gallery-1-hero`
(он же баннер в начале описаний), `gallery-2-chat` (английский и русский чат — строки из
`data/lang`), `gallery-3-tablist`, `gallery-4-docs` (снимок английского справочника, переснят
2026-11-12), `gallery-5-fork` — и `social.png` (1280×640, превью репозитория: Settings → Social
preview). Фоны — кадры сервера Farmstead из `video/public/footage/` монорепо. Это свёрстанные
слайды, а не игровые скриншоты: живых кадров чата, таба, магазинов и GUI пока нет.

Пересобрать после правки `images/src/slides.html`: каждая `<section data-out>` — отдельная картинка,
снимок элемента Playwright'ом. Шрифты берутся из `client/landing/public/fonts` монорепо по
относительному пути — в отдельном чекауте форка их нет.

### Страница Modrinth — workflow `Listing`

Actions → **Listing** → Run workflow. По умолчанию `dry_run` включён: скрипт
[`tools/modrinth_listing.py`](../../tools/modrinth_listing.py) читает живую страницу и печатает, что
поменяет (`=` совпадает, `~` изменится). Без `dry_run` — ставит название, summary, описание,
категории, стороны, ссылки, лицензию, иконку и заменяет галерею целиком (первая картинка —
Featured). Версии не трогает.

### Страница CurseForge — руками

У CurseForge нет API для страницы проекта.

1. Описание: тип **Markdown**, вставить [curseforge.md](../../listing/curseforge.md) целиком.
   Картинки берутся с `raw.githubusercontent.com` ветки `farmstead-monorepo-build` — PNG, их
   CurseForge показывает (WebP — нет).
2. Галерея: `python tools/modrinth_listing.py --curseforge-gallery build/curseforge-gallery` —
   кладёт картинки в порядке `modrinth.json` как `01-…`, `02-…` и пишет `CAPTIONS.md` (заголовок и
   описание каждой). Images → Upload по порядку, первую отметить Featured.
3. Main category **Server Utility**, дополнительная — Miscellaneous; Source / Issues / Wiki — как в
   таблице выше; иконка — `images/icon.png`.

## Первый выпуск — по шагам

1. Создать организацию Farmstead на Modrinth и проект по таблице выше (статус Draft).
   Загрузить первую версию CI не может, пока проекта нет, — поэтому порядок именно такой.
2. В репозитории `shtanko-michael/NeoEssentials` → Settings → Secrets and variables → Actions:
   - секрет `MODRINTH_TOKEN` — PAT Modrinth со scope *Create versions*;
   - переменная `MODRINTH_PROJECT_ID` — ID проекта (Settings проекта на Modrinth);
   - то же для CurseForge: `CURSEFORGE_TOKEN` (API token из профиля) и `CURSEFORGE_PROJECT_ID`.
   Без пары «секрет + переменная» соответствующая площадка просто пропускается.
3. Проверить `mod_version` в `gradle.properties` и написать `docs/publish/releases/<версия>.md`.
4. Сухой прогон: Actions → **Release Farmstead NeoEssentials** → Run workflow, `version` =
   `1.0.6-fs1`, `dry_run` включён (по умолчанию). Соберёт jar, проверит версию и заметки, напечатает
   план загрузки; jar лежит в артефакте `release` прогона. Ничего не публикует.
5. Поставить тег на `farmstead-monorepo-build` и запушить:
   `git tag farmstead-v1.0.6-fs1 && git push origin farmstead-v1.0.6-fs1`.
6. Workflow соберёт `-all` jar, создаст GitHub-релиз и загрузит версию на Modrinth и CurseForge
   (NeoForge, игровые версии из `GAME_VERSIONS` в workflow — по одной на строку). После — отправить
   проект Modrinth на модерацию (Submit for review).
7. Упала одна площадка — Run workflow с `dry_run` выключенным, `catalogues_only` включённым и
   `platforms` = `modrinth` или `curseforge`: GitHub-релиз второй раз не создаётся.

## Версии

`<mod_version апстрима>-fs<N>`: `1.0.6-fs1`, `1.0.6-fs2`… После синка с апстримом, поднявшего
`mod_version` до 1.0.7, следующий выпуск — `1.0.7-fs1`. Workflow отказывается собирать тег, чья база
не совпадает с `mod_version`.

## Чего на публичной странице быть не должно

- Ссылок на Discord / Ko-fi ZeroG как на «нашу поддержку» — только в разделе Credits.
- Раздела справочника про RegionGuard / Farmstead Claims как части мода: Claims — отдельный мод,
  здесь только совместимость (телепорты не сажают игрока в регион, куда вход запрещён).
