#!/usr/bin/env python3
"""Build the NeoEssentials documentation site.

Everything mechanical — the command list, the permission nodes, the config keys — is read
straight out of the source tree at build time, so a page can never quietly disagree with the
code the way a hand-maintained wiki does. Curated prose (guides, worked examples, pitfalls,
known issues) lives alongside it in Markdown and is merged in per system.

The generator lives in docs/site/ on purpose. docs/Wiki/ is upstream's GitHub-wiki dump;
on Windows those two names are the same folder, so mixing them would pollute the next
upstream sync. Never put generated or curated files back under docs/Wiki/.

Sources
  src/main/java/**.java ................ command registrations, permission-node registry
  src/main/resources/data/config/ ...... config keys, defaults, and their comments
  src/main/resources/data/lang/ ........ message catalogue coverage
  docs/Wiki/*.md ....................... upstream narrative pages (never edited here, so
                                         upstream syncs stay conflict-free)
  docs/site/content/*.md ............... this fork's added guides / pitfalls / known issues
  docs/site/content/systems/*.json ..... verified syntax + examples per command
  docs/site/content/_meta.json ......... system taxonomy and navigation
  docs/site/content/claims/ ............ curated Farmstead Claims reference pages
                                         (old regionguard/** URLs get redirect stubs)

Usage:  python docs/site/generate.py [--out docs/site/_site]
"""

import argparse
import html
import io
import json
import os
import re
import shutil
import sys
from urllib.parse import quote

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
JAVA_SRC = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'zerog', 'neoessentials')
CONFIG_DIR = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'config', 'neoessentials')
LANG_DIR = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'lang')
UPSTREAM_WIKI = os.path.join(ROOT, 'docs', 'Wiki')
CONTENT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'content')
CLAIMS_CONTENT_DIR = os.path.join(CONTENT_DIR, 'claims')
ASSETS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'assets')
PUBLIC_SITE = 'https://shtanko-michael.github.io/NeoEssentials'

WARNINGS = []


def warn(msg):
    WARNINGS.append(msg)
    print('  ! ' + msg)


def read(path):
    with io.open(path, encoding='utf-8', errors='replace') as fh:
        return fh.read()


def read_json(path, default=None):
    if not os.path.exists(path):
        return default
    return json.loads(read(path))


# ─────────────────────────────────────────────────────────────────────────────
# Languages
#
# The site is built twice: Russian at the root, English under /en/. Curated content lives in
# content/ (Russian, the reference) and content/en/ (English, same relative paths). An English
# JSON file is deep-merged over its Russian twin — structure (command lists, permission
# prefixes, config files) comes from the Russian file, text from the English one — so the
# translation can never drift structurally. A missing English file falls back to Russian with a
# warning, which --strict turns into a failed build.
# ─────────────────────────────────────────────────────────────────────────────

LANGS = ('ru', 'en')
LANG = 'ru'  # the language being built; set by build()

UI = {
    'anchor_label': ('Ссылка на раздел', 'Link to this section'),
    'on_this_page': ('На этой странице', 'On this page'),
    'product_switch': ('Выбор мода', 'Choose the mod'),
    'skip': ('К содержимому', 'Skip to content'),
    'search_ph': ('Поиск по командам, правам и конфигу…', 'Search commands, permissions and config…'),
    'theme': ('Переключить тему', 'Toggle theme'),
    'menu': ('Меню', 'Menu'),
    'lang_label': ('Read in English', 'Читать на русском'),
    'sections': ('Разделы', 'Sections'),
    'footer': ('— документация мода для сервера', '— mod documentation for the'),
    'footer_server': ('', ' server'),
    'footer_upstream': (' Не путать с апстримом ', ' Not to be confused with upstream '),
    'no_node': ('нет узла', 'no node'),
    'inferred_title': ('Узел не объявлен явно — выведен по соглашению реестра',
                       'The node is not declared explicitly — inferred from the registry convention'),
    'inferred': ('по соглашению', 'by convention'),
    'help_only_title': ('Запись в каталоге /help, не отдельная команда мода',
                        'An entry in the /help catalogue, not a separate mod command'),
    'help_only': ('каталог /help', '/help catalogue'),
    'th_command': ('Команда', 'Command'), 'th_syntax': ('Синтаксис', 'Syntax'),
    'th_aliases': ('Алиасы', 'Aliases'), 'th_perm': ('Право', 'Permission'),
    'th_desc': ('Описание', 'Description'), 'th_node': ('Узел', 'Node'),
    'th_registry': ('В реестре', 'In registry'), 'th_grants': ('Что даёт', 'What it grants'),
    'th_key': ('Ключ', 'Key'), 'th_default': ('По умолчанию', 'Default'),
    'th_lang': ('Язык', 'Language'), 'th_translated': ('Переведено', 'Translated'),
    'th_total': ('Всего ключей', 'Total keys'), 'th_coverage': ('Покрытие', 'Coverage'),
    'reg_yes_title': ('defaultValue=true в PermissionRegistry; при requireExplicitGrant это не грант',
                      'defaultValue=true in PermissionRegistry; with requireExplicitGrant this is not a grant'),
    'reg_yes': ('реестр: да', 'registry: yes'),
    'reg_no_title': ('defaultValue=false — без выдачи отказано', 'defaultValue=false — denied unless granted'),
    'reg_no': ('реестр: нет', 'registry: no'),
    'fix': ('Что делать:', 'What to do:'),
    'examples': ('Примеры', 'Examples'),
    'edge_cases': ('Тонкие места', 'Edge cases'),
    'known_issues': ('Известные проблемы', 'Known issues'),
    'nav_start': ('Начало', 'Start'), 'nav_overview': ('Обзор', 'Overview'),
    'nav_quickstart': ('Быстрый старт', 'Quick start'), 'nav_reference': ('Справочник', 'Reference'),
    'nav_commands': ('Все команды', 'All commands'), 'nav_permissions': ('Права', 'Permissions'),
    'nav_config': ('Конфигурация', 'Configuration'), 'nav_lang': ('Локализация', 'Localization'),
    'nav_systems': ('Системы', 'Systems'), 'nav_help': ('Помощь', 'Help'),
    'tile_cmds': ('Команд', 'Commands'), 'tile_cmds_s': ('объявлено в реестре мода', 'declared in the mod registry'),
    'tile_nodes': ('Узлов прав', 'Permission nodes'),
    'tile_nodes_s': ('с описанием и defaultValue в реестре', 'with a description and defaultValue in the registry'),
    'tile_keys': ('Ключей конфига', 'Config keys'), 'tile_keys_s': ('в %d файлах', 'in %d files'),
    'tile_langs': ('Языков', 'Languages'), 'tile_langs_s': ('русский — %d%%', 'Russian — %d%%'),
    'index_lede': ('Справочник форка для сервера Farmstead Minecraft: команды, права, конфигурация и разбор проблем.',
                   'Reference for the Farmstead Minecraft fork: commands, permissions, configuration and troubleshooting.'),
    'qs_lede': ('Установка, первые настройки и выдача прав.', 'Installation, first settings and granting permissions.'),
    'other': ('Прочее', 'Other'), 'all': ('Все', 'All'),
    'k_command': ('команда', 'command'), 'k_perm': ('право', 'permission'),
    'k_config': ('конфиг', 'config'), 'k_system': ('система', 'system'),
    'cmds_note': ('Таблица собирается из регистраций команд в исходниках при каждой сборке сайта, поэтому '
                  'колонка «Право» показывает узел, который мод проверяет на самом деле. Синтаксис и '
                  'примечания — выверенная вручную часть.',
                  'The table is built from the command registrations in the source on every site build, so '
                  'the Permission column shows the node the mod actually checks. Syntax and notes are the '
                  'hand-verified part.'),
    'cmds_lede': ('%d команд: синтаксис, алиасы, право доступа и описание.',
                  '%d commands: syntax, aliases, permission and description.'),
    'filter_table': ('Фильтр по таблице…', 'Filter the table…'),
    'perm_note': ('Колонка «В реестре» — это <code>defaultValue</code> узла в <code>PermissionRegistry</code>. '
                  'В поставке <code>permissions.requireExplicitGrant: true</code>: даже «реестр: да» само по себе '
                  'доступ не даёт — узел нужно выдать в LuckPerms или внутренним менеджером. OP проходит при '
                  '<code>opsBypassPermissions: true</code>.',
                  'The In registry column is the node\'s <code>defaultValue</code> in <code>PermissionRegistry</code>. '
                  'The shipped config has <code>permissions.requireExplicitGrant: true</code>: even "registry: yes" '
                  'grants nothing by itself — the node has to be granted in LuckPerms or the built-in manager. '
                  'OPs pass with <code>opsBypassPermissions: true</code>.'),
    'perm_lede': ('%d узлов из реестра — что даёт каждый. defaultValue при requireExplicitGrant не грант.',
                  '%d nodes from the registry — what each one grants. With requireExplicitGrant, defaultValue is not a grant.'),
    'filter_nodes': ('Фильтр по узлам…', 'Filter nodes…'),
    'cfg_lede': ('Каждый ключ, его значение по умолчанию и пояснение — прямо из файлов, которые ставит мод.',
                 'Every key, its default and its explanation — straight from the files the mod installs.'),
    'filter_keys': ('Фильтр по ключам…', 'Filter keys…'),
    'loc_lede': ('Какие языки поставляются и насколько полно переведены.',
                 'Which languages ship with the mod and how complete they are.'),
    'h_commands': ('Команды', 'Commands'), 'h_config': ('Конфигурация', 'Configuration'),
    'h_perms': ('Права', 'Permissions'), 'h_guide': ('Подробное руководство', 'Detailed guide'),
    'upstream_note': ('Раздел ниже — исходная документация проекта (<code>docs/Wiki/%s</code>). Ссылки на '
                      'другие страницы вики переписаны на этот сайт; текст самих руководств не редактируется, '
                      'чтобы синк с апстримом не давал конфликтов.',
                      'The section below is the project\'s original documentation (<code>docs/Wiki/%s</code>). '
                      'Links to other wiki pages are rewritten to this site; the guides themselves are not '
                      'edited, so upstream syncs stay conflict-free.'),
    'ts_title': ('Известные проблемы', 'Known issues'),
    'ts_lede': ('Симптом, причина и что с этим делать — по всем системам.',
                'Symptom, cause and what to do about it — across all systems.'),
}


def t(key, *args):
    """UI text in the language being built."""
    text = UI[key][LANGS.index(LANG)]
    return text % args if args else text


def content_path(rel):
    """The curated file for the language being built: content/en/<rel> for English (falling back
    to the Russian original, with a warning, when it has not been translated yet)."""
    ru = os.path.join(CONTENT_DIR, rel)
    if LANG == 'ru':
        return ru
    en = os.path.join(CONTENT_DIR, 'en', rel)
    if os.path.exists(en):
        return en
    if os.path.exists(ru):
        warn('no English translation of content/%s — using the Russian text' % rel.replace(os.sep, '/'))
    return ru


def overlay(base, top):
    """Deep-merge the translation over the reference: dicts by key, lists by index."""
    if isinstance(base, dict) and isinstance(top, dict):
        out = dict(base)
        for k, v in top.items():
            out[k] = overlay(base[k], v) if k in base else v
        return out
    if isinstance(base, list) and isinstance(top, list):
        return [overlay(b, top[i]) if i < len(top) else b for i, b in enumerate(base)]
    return top


def read_content_json_file(rel, default=None):
    """A curated JSON file in the language being built (English text over Russian structure)."""
    base = read_json(os.path.join(CONTENT_DIR, rel), None)
    if base is None:
        return default
    if LANG == 'ru':
        return base
    path = content_path(rel)
    if path == os.path.join(CONTENT_DIR, rel):
        return base
    return overlay(base, read_json(path, {}) or {})


# ─────────────────────────────────────────────────────────────────────────────
# Extraction
# ─────────────────────────────────────────────────────────────────────────────

STR = r'"((?:[^"\\]|\\.)*)"'
RE_REGISTER_PERM = re.compile(
    r'registerCommandWithPermission\(\s*' + STR + r'\s*,\s*' + STR +
    r'\s*,\s*(null|' + STR + r')((?:\s*,\s*"(?:[^"\\]|\\.)*")*)\s*\)', re.S)
RE_REGISTER_PLAIN = re.compile(
    r'(?<!WithPermission)registerCommand\(\s*' + STR + r'\s*,\s*' + STR +
    r'((?:\s*,\s*"(?:[^"\\]|\\.)*")*)\s*\)', re.S)
RE_NODE = re.compile(
    r'\bregister\(\s*' + STR + r'\s*,\s*' + STR +
    r'\s*,\s*PermissionCategory\.(\w+)\s*,\s*(true|false)\s*\)', re.S)
RE_STRLIT = re.compile(STR)
RE_REAL_CMD = re.compile(r'^[A-Za-z0-9?]+$')


def unescape(s):
    return s.replace('\\"', '"').replace('\\n', '\n').replace('\\\\', '\\')


def java_files():
    for base, _, names in os.walk(JAVA_SRC):
        for n in sorted(names):
            if n.endswith('.java'):
                yield os.path.join(base, n)


def extract_commands():
    """Every command the mod declares to its own registry, with its real permission node.

    Some registerCommand() calls pass a full syntax string (``rg define <id>``, ``neoe tpr``)
    so /help can show subcommands of another mod or of /neoe. Those are not slash-commands of
    their own — they are folded onto the first token.
    """
    blobs = {p: read(p) for p in java_files()}
    commands = {}
    extras = []  # (full_name, desc, rel)

    def add(name, desc, permission, inferred, aliases, rel):
        commands[name.lower()] = {
            'name': name,
            'description': unescape(desc),
            'permission': permission,
            'permissionInferred': inferred,
            'aliases': aliases,
            'declaredIn': rel,
        }

    for path, blob in sorted(blobs.items()):
        rel = os.path.relpath(path, JAVA_SRC).replace(os.sep, '/')
        for m in RE_REGISTER_PERM.finditer(blob):
            name, desc, node_raw, node_str, tail = m.groups()
            aliases = [unescape(a) for a in RE_STRLIT.findall(tail or '')]
            perm = None if node_raw == 'null' else unescape(node_str or '')
            if not RE_REAL_CMD.match(name):
                extras.append((name, unescape(desc), rel))
                continue
            add(name, desc, perm, False, aliases, rel)
        for m in RE_REGISTER_PLAIN.finditer(blob):
            name, desc, tail = m.groups()
            if name.lower() in commands:
                continue
            aliases = [unescape(a) for a in RE_STRLIT.findall(tail or '')]
            if not RE_REAL_CMD.match(name):
                extras.append((name, unescape(desc), rel))
                continue
            add(name, desc, 'neoessentials.' + name.lower(), True, aliases, rel)

    for full, desc, rel in extras:
        root = full.split()[0]
        key = root.lower()
        if key not in commands:
            add(root, desc, 'neoessentials.' + key, True, [], rel)
            commands[key]['helpCatalogOnly'] = True
        commands[key].setdefault('syntaxVariants', []).append('/' + full)
        if desc and commands[key]['description'] in (
                'NeoEssentials command', commands[key]['description']):
            # Keep the first real description; variants still carry their own line in syntaxVariants.
            pass

    # Which file actually builds each command's Brigadier tree.
    for key, cmd in commands.items():
        literal = 'Commands.literal("%s")' % cmd['name']
        root_re = re.compile(
            r'(?:dispatcher\.)?register\(\s*(?:root\s*=\s*)?Commands\.literal\("%s"\)'
            % re.escape(cmd['name']))
        strong, weak = [], []
        for path, blob in blobs.items():
            if literal not in blob:
                continue
            rel = os.path.relpath(path, JAVA_SRC).replace(os.sep, '/')
            if root_re.search(blob):
                strong.append(rel)
            else:
                weak.append(rel)
        if strong:
            cmd['implementedIn'] = sorted(strong, key=len)[0]
        elif weak:
            named = [p for p in weak if cmd['name'].lower() in p.lower()]
            cmd['implementedIn'] = (named or sorted(weak, key=len))[0]
    return commands


def extract_permissions():
    path = os.path.join(JAVA_SRC, 'api', 'permissions', 'PermissionRegistry.java')
    if not os.path.exists(path):
        warn('PermissionRegistry.java not found — permission reference will be empty')
        return {}
    nodes = {}
    for m in RE_NODE.finditer(read(path)):
        node, desc, cat, default = m.groups()
        nodes[node] = {
            'node': node,
            'description': unescape(desc),
            'category': cat,
            'default': default == 'true',
        }
    return nodes


RE_KEY_LINE = re.compile(r'^\s*"((?:[^"\\]|\\.)*)"\s*:\s*(.*?)\s*,?\s*$')
RE_LINE_COMMENT = re.compile(r'^\s*//\s?(.*)$')


def extract_config(filename):
    """Key / default / description for one config file.

    The shipped configs are JSON with `//` and `/* */` comments, where the comment above a key
    IS its documentation. Scanned line-by-line so that authorship stays in the config file
    itself — the reference can never drift from the defaults the mod actually ships.
    """
    path = os.path.join(CONFIG_DIR, filename)
    if not os.path.exists(path):
        return []
    entries = []
    stack = []
    pending = []
    in_block = False
    section = ''

    for raw in read(path).split('\n'):
        line = raw.rstrip()
        stripped = line.strip()

        if in_block:
            if '*/' in stripped:
                in_block = False
                tail = stripped.split('*/')[0].strip(' *')
                if tail:
                    section = (section + ' ' + tail).strip()
            else:
                section = (section + ' ' + stripped.lstrip(' *')).strip()
            continue
        if stripped.startswith('/*'):
            if '*/' in stripped:
                section = stripped[2:stripped.index('*/')].strip(' *')
            else:
                in_block = True
                section = stripped[2:].strip(' *')
            pending = []
            continue

        cm = RE_LINE_COMMENT.match(line)
        if cm:
            pending.append(cm.group(1).strip())
            continue

        km = RE_KEY_LINE.match(line)
        if km:
            key, value = km.group(1), km.group(2)
            if not key.startswith('_'):
                path_parts = stack + [key]
                if value in ('{', '['):
                    kind = 'object' if value == '{' else 'array'
                    entries.append({'key': '.'.join(path_parts), 'default': kind,
                                    'description': ' '.join(pending), 'section': section,
                                    'container': True})
                else:
                    entries.append({'key': '.'.join(path_parts),
                                    'default': value.rstrip(','),
                                    'description': ' '.join(pending), 'section': section,
                                    'container': False})
            if value in ('{', '['):
                stack.append(key)
            pending = []
            continue

        # Structural braces with no key on the line.
        opens = stripped.count('{') + stripped.count('[')
        closes = stripped.count('}') + stripped.count(']')
        if closes > opens:
            for _ in range(closes - opens):
                if stack:
                    stack.pop()
        if stripped:
            pending = []

    return entries


def extract_lang_stats():
    stats = []
    if not os.path.isdir(LANG_DIR):
        return stats
    en_path = os.path.join(LANG_DIR, 'en_us.json')
    en = read_json(en_path, {}) or {}
    base = {k for k, v in en.items() if not k.startswith('_') and isinstance(v, str)}
    for name in sorted(os.listdir(LANG_DIR)):
        if not name.endswith('.json'):
            continue
        code = name[:-5]
        data = read_json(os.path.join(LANG_DIR, name), {}) or {}
        have = {k for k, v in data.items()
                if not k.startswith('_') and isinstance(v, str) and v.strip()}
        covered = len(have & base) if code != 'en_us' else len(base)
        stats.append({'code': code, 'keys': len(have), 'covered': covered,
                      'total': len(base),
                      'percent': round(100.0 * covered / max(len(base), 1))})
    return stats


# ─────────────────────────────────────────────────────────────────────────────
# Markdown
# ─────────────────────────────────────────────────────────────────────────────

try:
    import markdown as _markdown

    def md_to_html(text):
        return _markdown.markdown(
            text, extensions=['tables', 'fenced_code', 'sane_lists', 'attr_list', 'toc'])
except ImportError:  # pragma: no cover - CI installs it; keep the build alive locally
    def md_to_html(text):
        warn('python-markdown not installed — rendering Markdown as preformatted text')
        return '<pre class="md-fallback">%s</pre>' % html.escape(text)


RE_H = re.compile(r'<h([23])[^>]*>(.*?)</h\1>', re.S)
RE_TAGS = re.compile(r'<[^>]+>')
RE_COUNT_BADGE = re.compile(r'<span class="count">.*?</span>', re.S)
RE_KEEP_ID = re.compile(r'\bid="([^"]+)"')


TRANSLIT = {
    'а': 'a', 'б': 'b', 'в': 'v', 'г': 'g', 'д': 'd', 'е': 'e', 'ё': 'e', 'ж': 'zh',
    'з': 'z', 'и': 'i', 'й': 'y', 'к': 'k', 'л': 'l', 'м': 'm', 'н': 'n', 'о': 'o',
    'п': 'p', 'р': 'r', 'с': 's', 'т': 't', 'у': 'u', 'ф': 'f', 'х': 'h', 'ц': 'ts',
    'ч': 'ch', 'ш': 'sh', 'щ': 'sch', 'ъ': '', 'ы': 'y', 'ь': '', 'э': 'e', 'ю': 'yu',
    'я': 'ya',
}


GITHUB_BLOB = 'https://github.com/shtanko-michael/NeoEssentials/blob/farmstead-monorepo-build/'

# Upstream GitHub-wiki pages that are not a system in _meta.json.
WIKI_SPECIAL = {
    'Home': '../../index.html',
    'CommandsReference': '../../commands/index.html',
    'FAQ': '../../index.html',
}

WIKI_REPO_DOCS = {
    'API': 'docs/API.md',
    'VaultAPI': 'docs/VaultAPI.md',
    'CUSTOM_LANGUAGES': 'docs/CUSTOM_LANGUAGES.md',
}

RE_MD_LINK = re.compile(r'\[([^\]]+)\]\(([^)]+)\)')


def wiki_page_map(systems):
    """Map an upstream wiki page name (EconomySystem, neoecrates, …) to our system slug."""
    out = {}
    for s in systems:
        doc = s.get('upstreamDoc')
        if not doc:
            continue
        out[os.path.splitext(doc)[0]] = s['slug']
    return out


def rewrite_upstream_links(text, page_map):
    """GitHub wiki links like [Tablist](TablistSystem) only resolve on github.com/wiki.

    The original English pages are embedded into /systems/<slug>/, so rewrite those
    targets to sibling site pages (or to the blob on GitHub for docs that are not a
    wiki page). Same-page #anchors and already-absolute URLs are left alone.
    """
    unmatched = set()

    def repl(m):
        label, target = m.group(1), m.group(2).strip()
        if not target or target.startswith(('#', 'http://', 'https://', 'mailto:', '/')):
            return m.group(0)
        path, hashsep, frag = target.partition('#')
        path = path.strip()
        if not path or path.endswith('.html'):
            return m.group(0)
        name = os.path.splitext(os.path.basename(path))[0]
        tail = ('#' + frag) if hashsep else ''
        if name in page_map:
            return '[%s](../%s/index.html%s)' % (label, page_map[name], tail)
        if name in WIKI_SPECIAL:
            return '[%s](%s%s)' % (label, WIKI_SPECIAL[name], tail)
        if name in WIKI_REPO_DOCS:
            return '[%s](%s%s%s)' % (label, GITHUB_BLOB, WIKI_REPO_DOCS[name], tail)
        unmatched.add(target)
        return m.group(0)

    rewritten = RE_MD_LINK.sub(repl, text)
    for t in sorted(unmatched):
        warn('unresolved upstream wiki link: (%s)' % t)
    return rewritten


def slugify(text):
    """Stable ASCII anchor. Cyrillic is transliterated rather than stripped — dropping it
    collapsed every Russian heading to the same id, which broke both deep links and the
    on-this-page rail."""
    plain = RE_TAGS.sub('', text).lower()
    plain = ''.join(TRANSLIT.get(ch, ch) for ch in plain)
    s = re.sub(r'[^a-z0-9]+', '-', plain).strip('-')
    return s or 'section'


def add_heading_ids(body):
    """Give h2/h3 stable ids and collect them for the on-this-page rail."""
    toc = []
    seen = {}

    def repl(m):
        level, inner = m.group(1), m.group(2)
        # The "N" badge is decoration and changes whenever the code does — keeping it out of
        # the anchor is what stops every deep link breaking when a command is added.
        label = RE_COUNT_BADGE.sub('', inner)
        # A generated section heading carries a fixed id (data-keep) so its anchor is the same
        # on the Russian and the English site — deep links survive the language switch.
        kept = RE_KEEP_ID.search(m.group(0)) if 'data-keep' in m.group(0) else None
        base = kept.group(1) if kept else slugify(label)
        seen[base] = seen.get(base, 0) + 1
        anchor = base if seen[base] == 1 else '%s-%d' % (base, seen[base])
        toc.append({'level': int(level), 'text': html.unescape(RE_TAGS.sub('', label)).strip(), 'id': anchor})
        return '<h%s id="%s">%s<a class="anchor" href="#%s" aria-label="%s">#</a></h%s>' % (
            level, anchor, inner, anchor, t('anchor_label'), level)

    return RE_H.sub(repl, body), toc


# ─────────────────────────────────────────────────────────────────────────────
# Page shell
# ─────────────────────────────────────────────────────────────────────────────

def cmd_anchor(name):
    """HTML id / hash for a command. Quote so names like '?' don't become a query string."""
    return 'cmd-' + quote(name, safe='')


def e(v):
    return html.escape('' if v is None else str(v), quote=True)


def code(v):
    return '<code>%s</code>' % e(v)


NAV = None  # filled in build()


def page(rel, title, subtitle, body, toc=None, active='', wide=False, extra_head='',
         product='neoessentials', nav=None):
    up = '../' * rel
    nav_html = []
    for group in nav or NAV:
        links = []
        for item in group['items']:
            cls = ' class="on"' if item['id'] == active else ''
            links.append('<li><a href="%s%s"%s>%s</a></li>' % (up, item['href'], cls, e(item['title'])))
        nav_html.append('<div class="nav-group"><p class="nav-title">%s</p><ul>%s</ul></div>'
                        % (e(group['title']), ''.join(links)))

    toc_html = ''
    if toc:
        items = ''.join('<li class="lv%d"><a href="#%s">%s</a></li>' % (t['level'], t['id'], e(t['text']))
                        for t in toc if t['level'] <= 3)
        if items:
            toc_html = ('<details class="toc"><summary class="toc-title">%s</summary>'
                        '<ul>%s</ul></details>' % (t('on_this_page'), items))

    is_claims = product == 'claims'
    product_name = 'Claims' if is_claims else 'NeoEssentials'
    product_home = 'claims/index.html' if is_claims else 'index.html'
    site_name = 'Farmstead ' + product_name
    doc_title = title if title == site_name else '%s · %s' % (title, site_name)

    product_switch = (
        '<nav class="product-switch" aria-label="%s">'
        '<a href="%sindex.html"%s>NeoEssentials</a>'
        '<a href="%sclaims/index.html"%s>Farmstead Claims</a>'
        '</nav>' % (
            t('product_switch'), up, '' if is_claims else ' class="on" aria-current="page"',
            up, ' class="on" aria-current="page"' if is_claims else ''))

    return """<!doctype html>
<html lang="{lang}" data-base="{up}" data-alt="{alt}">
<head>
<meta charset="utf-8">
<script>{early}</script>
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{doc_title}</title>
<meta name="description" content="{subtitle}">
<meta name="theme-color" content="#16352c">
<meta property="og:title" content="{doc_title}">
<meta property="og:description" content="{subtitle}">
<meta property="og:image" content="https://shtanko-michael.github.io/NeoEssentials/assets/farmstead.png">
<link rel="icon" type="image/png" href="{up}assets/farmstead.png">
<link rel="apple-touch-icon" href="{up}assets/farmstead.png">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Archivo:wght@500;600;700&family=JetBrains+Mono:wght@400;500;700&family=Source+Sans+3:ital,wght@0,400;0,600;1,400&display=swap">
<link rel="alternate" hreflang="{alt_lang}" href="{alt}">
<link rel="stylesheet" href="{up}assets/site.css?v=4">
{extra_head}
</head>
<body{bodycls}>
<a class="skip" href="#main">{t_skip}</a>
<header class="topbar">
  <button id="menu" type="button" aria-label="{t_menu}" aria-controls="sidenav" aria-expanded="false"><span></span></button>
  <a class="brand" href="{up}{product_home}" aria-label="{site_name}">
    <img class="brand-logo" src="{up}assets/farmstead.png" alt="" width="36" height="36">
    <span class="brand-text">
      <span class="brand-kicker">Farmstead</span>
      <span class="brand-name">{product_name}</span>
    </span>
  </a>
  {product_switch}
  <div class="search-wrap">
    <input id="q" type="search" placeholder="{t_search}" autocomplete="off" spellcheck="false">
    <div id="results" hidden></div>
  </div>
  <a id="lang" href="{alt}" hreflang="{alt_lang}" lang="{alt_lang}" data-lang="{alt_lang}" title="{t_lang}" aria-label="{t_lang}">{alt_code}</a>
  <button id="theme" type="button" aria-label="{t_theme}">◐</button>
</header>
<div class="shell">
  <nav class="sidenav" id="sidenav" aria-label="{t_sections}">{nav}</nav>
  <div class="scrim" hidden></div>
  <main id="main">
    <div class="page-head">
      <h1>{title}</h1>
      <p class="lede">{subtitle}</p>
    </div>
    {toc}
    <div class="prose">{body}</div>
  </main>
</div>
<footer class="site-foot">
  <p><strong>{site_name}</strong> {t_footer}
  <a href="https://farmsteadminecraft.online">Farmstead Minecraft</a>{t_footer_server}.{footer_tail}</p>
</footer>
<script src="{up}assets/site.js?v=4" defer></script>
</body>
</html>
""".format(up=up, title=e(title), doc_title=e(doc_title), subtitle=e(subtitle),
           body=body, nav=''.join(nav_html),
           toc=toc_html, extra_head=extra_head,
           bodycls=' class="wide"' if wide else '', product_home=product_home,
           product_name=product_name, site_name=site_name, product_switch=product_switch,
           lang=LANG, alt=ALT_HREF, alt_lang=('en' if LANG == 'ru' else 'ru'),
           alt_code=('EN' if LANG == 'ru' else 'RU'), early=EARLY_LANG_JS,
           t_menu=t('menu'), t_skip=t('skip'), t_search=t('search_ph'), t_theme=t('theme'), t_lang=t('lang_label'),
           t_sections=t('sections'), t_footer=t('footer'), t_footer_server=t('footer_server'),
           footer_tail=(t('footer_upstream') +
                        '<a href="https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials">'
                        'ZeroG NeoEssentials</a>.' if not is_claims else ''))


ALT_HREF = '__ALT_HREF__'  # the same page in the other language; resolved per file in write()

# Runs before anything renders: a reader who picked a language (the switch stores it, like the
# theme) is sent to that language's copy of the page; a first-time reader gets the language of
# their browser (Russian for ru/uk/be/kk, English otherwise).
EARLY_LANG_JS = (
    "(function(){try{var d=document.documentElement,l=d.lang,"
    "s=localStorage.getItem('ne-docs-lang');"
    "if(s!=='ru'&&s!=='en'){var n=((navigator.languages&&navigator.languages[0])||navigator.language||'')"
    ".toLowerCase();s=/^(ru|uk|be|kk)/.test(n)?'ru':'en';}"
    "if(s!==l){location.replace(d.getAttribute('data-alt')+location.hash);}}catch(e){}})();"
)


def redirect_page(target_rel, href, title):
    """A stub kept at an old URL that sends the reader to the new home of that page."""
    canonical = PUBLIC_SITE + ('/en/' if LANG == 'en' else '/') + target_rel.replace('index.html', '')
    return (
        '<!doctype html>\n<html lang="%s">\n<head>\n<meta charset="utf-8">\n'
        '<title>%s</title>\n<meta name="robots" content="noindex">\n'
        '<link rel="canonical" href="%s">\n<meta http-equiv="refresh" content="0; url=%s">\n'
        '<script>location.replace(%s + location.hash)</script>\n</head>\n'
        '<body><p><a href="%s">%s</a></p></body>\n</html>\n'
        % (LANG, e(title), e(canonical), e(href), json.dumps(href), e(href), e(title)))


def write(out_dir, rel_path, content):
    if ALT_HREF in content:
        up = '../' * rel_path.count('/')
        alt = up + ('en/' if LANG == 'ru' else '../') + rel_path
        content = content.replace(ALT_HREF, alt)
    dest = os.path.join(out_dir, rel_path)
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    with io.open(dest, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write(content)


# ─────────────────────────────────────────────────────────────────────────────
# Renderers
# ─────────────────────────────────────────────────────────────────────────────

CATEGORY_RU = {
    'CORE': 'Базовые', 'ADMIN': 'Администрирование', 'ECONOMY': 'Экономика',
    'PLAYER': 'Игрок', 'MISC': 'Разное', 'CHAT': 'Чат', 'TELEPORT': 'Телепортация',
    'ITEMS': 'Предметы', 'KITS': 'Киты', 'MODERATION': 'Модерация',
}
CATEGORY_EN = {
    'CORE': 'Core', 'ADMIN': 'Administration', 'ECONOMY': 'Economy',
    'PLAYER': 'Player', 'MISC': 'Miscellaneous', 'CHAT': 'Chat', 'TELEPORT': 'Teleportation',
    'ITEMS': 'Items', 'KITS': 'Kits', 'MODERATION': 'Moderation',
}


def category_name(category):
    return (CATEGORY_RU if LANG == 'ru' else CATEGORY_EN).get(category, category)


def command_row(cmd, curated):
    extra = curated.get(cmd['name'].lower(), {})
    syntax = extra.get('syntax')
    if not syntax:
        variants = cmd.get('syntaxVariants') or []
        syntax = ' | '.join(variants) if variants else ('/' + cmd['name'])
    perm = cmd.get('permission')
    perm_cell = code(perm) if perm else '<span class="muted">%s</span>' % t('no_node')
    if cmd.get('permissionInferred'):
        perm_cell += ' <span class="tag tag-soft" title="%s">%s</span>' % (t('inferred_title'), t('inferred'))
    if cmd.get('helpCatalogOnly'):
        perm_cell += ' <span class="tag tag-soft" title="%s">%s</span>' % (t('help_only_title'), t('help_only'))
    aliases = ', '.join(code(a) for a in cmd.get('aliases', [])) or '<span class="muted">—</span>'
    desc = e(extra.get('description') or cmd['description'])
    notes = extra.get('notes')
    if notes:
        desc += '<span class="row-note">%s</span>' % e(notes)
    return ('<tr id="%s" data-search="%s">'
            '<td class="c-name"><code>/%s</code></td>'
            '<td class="c-syntax"><code>%s</code></td>'
            '<td class="c-alias" data-label="%s">%s</td>'
            '<td class="c-perm" data-label="%s">%s</td>'
            '<td class="c-desc">%s</td>'
            '</tr>') % (
        cmd_anchor(cmd['name']),
        e(' '.join([cmd['name'], syntax, perm or '', ' '.join(cmd.get('aliases', [])),
                    extra.get('description') or cmd['description']]).lower()),
        e(cmd['name']), e(syntax), t('th_aliases'), aliases, t('th_perm'), perm_cell, desc)


def cmd_thead():
    return ('<thead><tr><th>%s</th><th>%s</th><th>%s</th><th>%s</th><th>%s</th></tr></thead>'
            % (t('th_command'), t('th_syntax'), t('th_aliases'), t('th_perm'), t('th_desc')))


def commands_table(cmds, curated):
    rows = ''.join(command_row(c, curated) for c in cmds)
    return ('<div class="scroller"><table class="grid cmd-table">%s'
            '<tbody>%s</tbody></table></div>') % (cmd_thead(), rows)


def permissions_table(nodes):
    rows = []
    for n in nodes:
        default = ('<span class="tag tag-ok" title="%s">%s</span>' % (t('reg_yes_title'), t('reg_yes'))
                   if n['default']
                   else '<span class="tag tag-warn" title="%s">%s</span>' % (t('reg_no_title'), t('reg_no')))
        rows.append('<tr data-search="%s"><td class="c-key"><code>%s</code></td><td class="c-meta">%s</td><td>%s</td></tr>'
                    % (e((n['node'] + ' ' + n['description']).lower()), e(n['node']), default,
                       e(n['description'])))
    return ('<div class="scroller"><table class="grid"><thead><tr><th>%s</th>'
            '<th>%s</th><th>%s</th></tr></thead><tbody>%s</tbody></table></div>'
            % (t('th_node'), t('th_registry'), t('th_grants'), ''.join(rows)))


def config_table(entries):
    rows = []
    for c in entries:
        if c['container']:
            rows.append('<tr class="grp"><td colspan="3"><code>%s</code>%s</td></tr>'
                        % (e(c['key']),
                           (' <span class="muted">— %s</span>' % e(c['description']))
                           if c['description'] else ''))
            continue
        rows.append('<tr data-search="%s"><td class="c-key"><code>%s</code></td>'
                    '<td class="c-meta" data-label="%s"><code>%s</code></td>'
                    '<td>%s</td></tr>'
                    % (e((c['key'] + ' ' + c['description']).lower()), e(c['key']),
                       t('th_default'), e(c['default']), e(c['description']) or '<span class="muted">—</span>'))
    return ('<div class="scroller"><table class="grid"><thead><tr><th>%s</th>'
            '<th>%s</th><th>%s</th></tr></thead><tbody>%s</tbody></table></div>'
            % (t('th_key'), t('th_default'), t('th_desc'), ''.join(rows)))


def callouts(items, kind, title, anchor):
    if not items:
        return ''
    out = ['<section class="callouts %s"><h2 id="%s" data-keep>%s</h2>' % (kind, anchor, e(title))]
    for it in items:
        out.append('<article class="callout"><h3>%s</h3>%s%s</article>' % (
            e(it.get('title', '')),
            md_to_html(it.get('body', '')),
            ('<p class="callout-fix"><strong>%s</strong> %s</p>' % (t('fix'), md_to_html(it['fix']).replace('<p>', '').replace('</p>', '')))
            if it.get('fix') else ''))
    out.append('</section>')
    return ''.join(out)


def examples_block(items):
    if not items:
        return ''
    out = ['<section class="examples"><h2 id="primery" data-keep>%s</h2>' % t('examples')]
    for ex in items:
        out.append('<article class="example"><h3>%s</h3>%s<pre class="cmd"><code>%s</code></pre>%s</article>'
                   % (e(ex.get('title', '')),
                      md_to_html(ex.get('goal', '')) if ex.get('goal') else '',
                      e('\n'.join(ex.get('commands', []))),
                      md_to_html(ex.get('result', '')) if ex.get('result') else ''))
    out.append('</section>')
    return ''.join(out)


# ─────────────────────────────────────────────────────────────────────────────
# Catalog (shared by the HTML site and the GitHub wiki)
# ─────────────────────────────────────────────────────────────────────────────

def load_catalog():
    """Extract commands, permissions, config and attach curated system membership."""
    print('Extracting from source… (%s)' % LANG)
    commands = extract_commands()
    nodes = extract_permissions()
    lang_stats = extract_lang_stats()
    config_files = sorted(f for f in os.listdir(CONFIG_DIR) if f.endswith('.json')) \
        if os.path.isdir(CONFIG_DIR) else []
    configs = [(f, extract_config(f)) for f in config_files]
    print('  %d commands, %d permission nodes, %d config files, %d languages'
          % (len(commands), len(nodes), len(configs), len(lang_stats)))

    meta = read_content_json_file('_meta.json', None)
    if meta is None:
        warn('docs/site/content/_meta.json missing — falling back to permission-prefix grouping')
        meta = {'systems': []}
    curated_cmds = read_content_json_file('commands.json', {}) or {}
    systems = meta.get('systems', [])
    wiki_pages = wiki_page_map(systems)
    for sysdef in systems:
        for name, detail in (read_content_json(sysdef['slug']).get('commandDetails') or {}).items():
            curated_cmds.setdefault(name.lstrip('/').lower(), detail)

    assigned = set()
    for sysdef in systems:
        sysdef['_commands'] = []
        sysdef['_content'] = read_content_json(sysdef['slug'])
        declared = list(sysdef.get('commands', [])) + list(sysdef['_content'].get('commands', []))
        for name in declared:
            key = name.lstrip('/').lower()
            if key in assigned:
                continue
            cmd = commands.get(key)
            if cmd:
                sysdef['_commands'].append(cmd)
                assigned.add(key)
            else:
                warn('system "%s" lists unknown command /%s' % (sysdef['slug'], name))
        prefixes = tuple(sysdef.get('permissionPrefixes', []))
        if prefixes:
            for key, cmd in sorted(commands.items()):
                if key in assigned:
                    continue
                if cmd.get('permission') and cmd['permission'].startswith(prefixes):
                    sysdef['_commands'].append(cmd)
                    assigned.add(key)
        sysdef['_nodes'] = [n for n in sorted(nodes.values(), key=lambda x: x['node'])
                            if prefixes and n['node'].startswith(prefixes)]
        sysdef['_commands'].sort(key=lambda c: c['name'])
        sysdef['_wiki'] = wiki_page_name(sysdef)

    orphans = sorted(set(commands) - assigned)
    if orphans:
        print('  %d commands not claimed by any system (listed under «Прочее»)' % len(orphans))

    return {
        'commands': commands,
        'nodes': nodes,
        'lang_stats': lang_stats,
        'configs': configs,
        'systems': systems,
        'curated_cmds': curated_cmds,
        'orphans': orphans,
        'wiki_pages': wiki_pages,
    }


def wiki_page_name(sysdef):
    """GitHub wiki page title. Prefer the upstream wiki filename so URLs match ZeroG."""
    doc = sysdef.get('upstreamDoc')
    if doc:
        return os.path.splitext(doc)[0]
    return ''.join(part.capitalize() for part in sysdef['slug'].split('-'))


# ─────────────────────────────────────────────────────────────────────────────
# Build
# ─────────────────────────────────────────────────────────────────────────────

def check_languages():
    """Every shipped language must be complete — tools/lang_check.py decides; --strict fails on it."""
    sys.path.insert(0, os.path.join(ROOT, 'tools'))
    try:
        import lang_check
    finally:
        sys.path.pop(0)
    _, report = lang_check.run(quiet=True)
    for code, problems in sorted(report.items()):
        kinds = {}
        for kind, _, _ in problems:
            if kind not in lang_check.SOFT:
                kinds[kind] = kinds.get(kind, 0) + 1
        if kinds:
            warn('data/lang/%s.json is not complete (%s) — run python tools/lang_check.py --lang %s'
                 % (code, ', '.join('%s %d' % kv for kv in sorted(kinds.items())), code))


def build(out_dir, base, wiki_out=None, lang='ru'):
    global NAV, LANG
    LANG = lang
    if lang == 'ru':
        check_languages()
    cat = load_catalog()
    commands = cat['commands']
    nodes = cat['nodes']
    lang_stats = cat['lang_stats']
    configs = cat['configs']
    systems = cat['systems']
    curated_cmds = cat['curated_cmds']
    orphans = cat['orphans']
    wiki_pages = cat['wiki_pages']

    NAV = [
        {'title': t('nav_start'), 'items': [
            {'id': 'index', 'title': t('nav_overview'), 'href': 'index.html'},
            {'id': 'quickstart', 'title': t('nav_quickstart'), 'href': 'quickstart/index.html'},
        ]},
        {'title': t('nav_reference'), 'items': [
            {'id': 'commands', 'title': t('nav_commands'), 'href': 'commands/index.html'},
            {'id': 'permissions', 'title': t('nav_permissions'), 'href': 'permissions/index.html'},
            {'id': 'config', 'title': t('nav_config'), 'href': 'config/index.html'},
            {'id': 'lang', 'title': t('nav_lang'), 'href': 'localization/index.html'},
        ]},
        {'title': t('nav_systems'), 'items': [
            {'id': 'sys-' + s['slug'], 'title': s['title'], 'href': 'systems/%s/index.html' % s['slug']}
            for s in systems
        ]},
        {'title': t('nav_help'), 'items': [
            {'id': 'troubleshooting', 'title': t('known_issues'), 'href': 'troubleshooting/index.html'},
        ]},
    ]

    search = []

    # ---- index -------------------------------------------------------------
    ru = next((l for l in lang_stats if l['code'] == 'ru_ru'), None)
    stat_tiles = [
        (t('tile_cmds'), len(commands), t('tile_cmds_s')),
        (t('tile_nodes'), len(nodes), t('tile_nodes_s')),
        (t('tile_keys'), sum(len([c for c in ent if not c['container']]) for _, ent in configs),
         t('tile_keys_s', len(configs))),
        (t('tile_langs'), len(lang_stats), t('tile_langs_s', ru['percent'] if ru else 0)),
    ]
    tiles = ''.join('<div class="tile"><p class="tile-n">%s</p><p class="tile-k">%s</p>'
                    '<p class="tile-s">%s</p></div>' % (v, e(k), e(s)) for k, v, s in stat_tiles)

    intro = read_md_content('index.md')
    body = ('<section class="tiles">%s</section>' % tiles) + intro
    write(out_dir, 'index.html', page(0, 'Farmstead NeoEssentials',
                                      t('index_lede'),
                                      body, active='index'))

    # ---- quick start -------------------------------------------------------
    qs = read_md_content('quickstart.md')
    if qs:
        b, toc = add_heading_ids(qs)
        write(out_dir, 'quickstart/index.html',
              page(1, t('nav_quickstart'), t('qs_lede'), b, toc,
                   active='quickstart'))

    # ---- commands ----------------------------------------------------------
    all_cmds = sorted(commands.values(), key=lambda c: c['name'])
    filters = ''.join(
        '<button type="button" class="chip" data-filter="%s">%s <span class="count">%d</span></button>'
        % (e(s['slug']), e(s['title']), len(s['_commands']))
        for s in systems if s['_commands'])
    if orphans:
        # Commands no system claims stay reachable instead of being quietly invisible.
        filters += ('<button type="button" class="chip" data-filter="other">%s '
                    '<span class="count">%d</span></button>' % (t('other'), len(orphans)))
    sys_of = {}
    for s in systems:
        for c in s['_commands']:
            sys_of[c['name'].lower()] = s['slug']
    rows = []
    for c in all_cmds:
        row = command_row(c, curated_cmds)
        rows.append(row.replace('<tr ', '<tr data-sys="%s" ' % e(sys_of.get(c['name'].lower(), 'other')), 1))
        extra = curated_cmds.get(c['name'].lower(), {})
        search.append({'t': '/' + c['name'], 'd': extra.get('description') or c['description'],
                       'u': 'commands/index.html#' + cmd_anchor(c['name']), 'k': t('k_command')})
    table = ('<div class="scroller"><table class="grid cmd-table">%s'
             '<tbody>%s</tbody></table></div>' % (cmd_thead(), ''.join(rows)))
    note = '<div class="note"><p>%s</p></div>' % t('cmds_note')
    write(out_dir, 'commands/index.html',
          page(1, t('nav_commands'), t('cmds_lede', len(all_cmds)),
               note + '<div class="filters"><button type="button" class="chip on" data-filter="*">%s</button>' % t('all') +
               filters + '</div><div class="tablefilter"><input id="tq" type="search" '
               'placeholder="%s" autocomplete="off"></div>' % t('filter_table') + table,
               active='commands', wide=True))

    # ---- permissions -------------------------------------------------------
    by_cat = {}
    for n in nodes.values():
        by_cat.setdefault(n['category'], []).append(n)
    parts = []
    toc = []
    for category in sorted(by_cat, key=lambda c: (-len(by_cat[c]), c)):
        title = '%s (%s)' % (category_name(category), category)
        anchor = slugify(category)
        toc.append({'level': 2, 'text': title, 'id': anchor})
        parts.append('<h2 id="%s">%s <span class="count">%d</span></h2>'
                     % (anchor, e(title), len(by_cat[category])))
        parts.append(permissions_table(sorted(by_cat[category], key=lambda x: x['node'])))
    for n in nodes.values():
        search.append({'t': n['node'], 'd': n['description'], 'u': 'permissions/index.html', 'k': t('k_perm')})
    perm_note = '<div class="note"><p>%s</p></div>' % t('perm_note')
    write(out_dir, 'permissions/index.html',
          page(1, t('nav_permissions'), t('perm_lede', len(nodes)),
               perm_note +
               '<div class="tablefilter"><input id="tq" type="search" placeholder="%s" '
               'autocomplete="off"></div>' % t('filter_nodes') + ''.join(parts), toc, active='permissions', wide=True))

    # ---- config ------------------------------------------------------------
    parts = []
    toc = []
    for fname, entries in configs:
        anchor = slugify(fname)
        toc.append({'level': 2, 'text': fname, 'id': anchor})
        real = [c for c in entries if not c['container']]
        parts.append('<h2 id="%s"><code>%s</code> <span class="count">%d</span></h2>'
                     % (anchor, e(fname), len(real)))
        parts.append(config_table(entries))
        for c in real:
            search.append({'t': c['key'], 'd': c['description'] or fname,
                           'u': 'config/index.html#' + anchor, 'k': t('k_config')})
    write(out_dir, 'config/index.html',
          page(1, t('nav_config'), t('cfg_lede'),
               '<div class="tablefilter"><input id="tq" type="search" placeholder="%s" '
               'autocomplete="off"></div>' % t('filter_keys') + ''.join(parts), toc, active='config', wide=True))

    # ---- localization ------------------------------------------------------
    rows = ''.join(
        '<tr><td><code>%s</code></td><td class="num">%d</td><td class="num">%d</td>'
        '<td><div class="bar"><span style="width:%d%%"></span></div><span class="pct">%d%%</span></td></tr>'
        % (e(l['code']), l['covered'], l['total'], l['percent'], l['percent']) for l in lang_stats)
    loc_md = read_md_content('localization.md')
    write(out_dir, 'localization/index.html',
          page(1, t('nav_lang'), t('loc_lede'),
               '<div class="scroller"><table class="grid"><thead><tr><th>%s</th><th>%s</th>'
               '<th>%s</th><th>%s</th></tr></thead><tbody>%s</tbody></table></div>%s'
               % (t('th_lang'), t('th_translated'), t('th_total'), t('th_coverage'), rows, loc_md), active='lang'))

    # ---- systems -----------------------------------------------------------
    for sysdef in systems:
        slug = sysdef['slug']
        chunks = []
        extra = sysdef['_content']

        if sysdef.get('summary'):
            chunks.append('<div class="note"><p>%s</p></div>' % e(sysdef['summary']))

        if sysdef['_commands']:
            chunks.append('<h2 id="komandy" data-keep>%s <span class="count">%d</span></h2>'
                          % (t('h_commands'), len(sysdef['_commands'])))
            chunks.append(commands_table(sysdef['_commands'], curated_cmds))

        own_md = read_md_content('systems/%s.md' % slug)
        if own_md:
            chunks.append(own_md)

        chunks.append(examples_block(extra.get('examples', [])))
        chunks.append(callouts(extra.get('edgeCases', []), 'edge', t('edge_cases'), 'tonkie-mesta'))
        chunks.append(callouts(extra.get('knownIssues', []), 'issue', t('known_issues'), 'izvestnye-problemy'))

        cfg_keys = sysdef.get('configFiles', [])
        cfg_chunks = []
        for fname in cfg_keys:
            entries = dict(configs).get(fname)
            if entries:
                cfg_chunks.append('<h3><code>%s</code></h3>%s' % (e(fname), config_table(entries)))
        if cfg_chunks:
            chunks.append('<h2 id="konfiguraciya" data-keep>%s</h2>' % t('h_config') + ''.join(cfg_chunks))

        if sysdef['_nodes']:
            chunks.append('<h2 id="prava" data-keep>%s <span class="count">%d</span></h2>'
                          % (t('h_perms'), len(sysdef['_nodes'])))
            chunks.append(permissions_table(sysdef['_nodes']))

        upstream = sysdef.get('upstreamDoc')
        if upstream:
            path = os.path.join(UPSTREAM_WIKI, upstream)
            if os.path.exists(path):
                text = read(path)
                text = re.sub(r'^#\s+.*\n', '', text, count=1)
                text = rewrite_upstream_links(text, wiki_pages)
                chunks.append('<h2 id="podrobnoe-rukovodstvo" data-keep>%s</h2>'
                              '<div class="upstream-note">%s</div>%s'
                              % (t('h_guide'), t('upstream_note', e(upstream)), md_to_html(text)))
            else:
                warn('system "%s" points at missing upstream doc %s' % (slug, upstream))

        body, toc = add_heading_ids(''.join(chunks))
        write(out_dir, 'systems/%s/index.html' % slug,
              page(2, sysdef['title'], sysdef.get('lede', ''), body, toc,
                   active='sys-' + slug, wide=True))
        search.append({'t': sysdef['title'], 'd': sysdef.get('lede', ''),
                       'u': 'systems/%s/index.html' % slug, 'k': t('k_system')})

    # ---- troubleshooting ---------------------------------------------------
    all_issues = []
    for sysdef in systems:
        for it in read_content_json(sysdef['slug']).get('knownIssues', []):
            it = dict(it)
            it['system'] = sysdef['title']
            it['slug'] = sysdef['slug']
            all_issues.append(it)
    ts_md = read_md_content('troubleshooting.md')
    blocks = []
    for it in all_issues:
        blocks.append('<article class="callout"><h3>%s</h3>'
                      '<p class="callout-sys"><a href="../systems/%s/index.html">%s</a></p>%s%s</article>'
                      % (e(it.get('title', '')), e(it['slug']), e(it['system']),
                         md_to_html(it.get('body', '')),
                         ('<p class="callout-fix"><strong>%s</strong> %s</p>'
                          % (t('fix'), md_to_html(it['fix']).replace('<p>', '').replace('</p>', '')))
                         if it.get('fix') else ''))
    body = ts_md + ('<section class="callouts issue">%s</section>' % ''.join(blocks) if blocks else '')
    b, toc = add_heading_ids(body)
    write(out_dir, 'troubleshooting/index.html',
          page(1, t('ts_title'), t('ts_lede'),
               b, toc, active='troubleshooting'))

    # ---- Farmstead Claims --------------------------------------------------
    # Farmstead Claims (game/claims-mod, the drop-in replacement for RegionGuard) is maintained in
    # the parent Farmstead monorepo, while this site is built from the standalone NeoEssentials
    # repository on GitHub Actions. Keep its curated documentation here so Pages builds are
    # self-contained and do not depend on a sibling checkout. The section used to live at
    # regionguard/; every old page gets a redirect stub to its claims/ twin.
    rg_meta = read_content_json_file('claims/_meta.json', {}) or {}
    rg_pages = rg_meta.get('pages', [])
    rg_groups = []
    for group_name in rg_meta.get('groups', []):
        items = []
        for item in rg_pages:
            if item.get('group') != group_name:
                continue
            slug = item.get('slug', '')
            items.append({
                'id': 'rg-' + (slug or 'index'),
                'title': item.get('navTitle') or item['title'],
                'href': 'claims/%s' % (('%s/index.html' % slug) if slug else 'index.html'),
            })
        if items:
            rg_groups.append({'title': group_name, 'items': items})

    for item in rg_pages:
        slug = item.get('slug', '')
        source = item.get('source') or ((slug or 'index') + '.md')
        source_path = content_path('claims/' + source)
        if not os.path.exists(source_path):
            warn('Farmstead Claims page "%s" points at missing content %s' % (slug or 'index', source))
            continue
        body, toc = add_heading_ids(md_to_html(read(source_path)))
        out_path = 'claims/%s' % (('%s/index.html' % slug) if slug else 'index.html')
        rel = 2 if slug else 1
        write(out_dir, 'regionguard/%s' % (('%s/index.html' % slug) if slug else 'index.html'),
              redirect_page(out_path, '../' * rel + out_path, item['title']))
        write(out_dir, out_path,
              page(rel, item['title'], item.get('lede', ''), body, toc,
                   active='rg-' + (slug or 'index'), wide=item.get('wide', False),
                   product='claims', nav=rg_groups))
        search.append({'t': item['title'], 'd': item.get('lede', ''), 'u': out_path,
                       'k': 'Claims'})
        for keyword in item.get('keywords', []):
            search.append({'t': keyword, 'd': item.get('lede', ''), 'u': out_path,
                           'k': 'Claims'})

    # ---- assets & search ---------------------------------------------------
    os.makedirs(os.path.join(out_dir, 'assets'), exist_ok=True)
    for name in os.listdir(ASSETS_DIR):
        src = os.path.join(ASSETS_DIR, name)
        if not os.path.isfile(src) or name.startswith('.'):
            continue
        dest = os.path.join(out_dir, 'assets', name)
        if name.lower().endswith(('.png', '.ico', '.jpg', '.jpeg', '.webp', '.svg', '.gif')):
            shutil.copy2(src, dest)
        else:
            write(out_dir, 'assets/' + name, read(src))
    write(out_dir, 'search.json', json.dumps(search, ensure_ascii=False))
    write(out_dir, '.nojekyll', '')

    if wiki_out:
        write_github_wiki(wiki_out, cat)

    print('Wrote %d search entries to %s' % (len(search), out_dir))
    if WARNINGS:
        print('\n%d warning(s):' % len(WARNINGS))
        for w in WARNINGS:
            print('  - ' + w)
    return 0


def read_md_content(rel):
    path = content_path(rel)
    if not os.path.exists(path):
        return ''
    return md_to_html(read(path))


def read_content_json(slug):
    return read_content_json_file('systems/%s.json' % slug, {}) or {}


def doc_url(rel=''):
    rel = rel.lstrip('/')
    if not rel:
        return PUBLIC_SITE + '/'
    return PUBLIC_SITE + '/' + rel


def _wiki_link(title, rel, lede):
    return '- **[%s](%s)** — %s' % (title, doc_url(rel), lede)


def write_github_wiki(out_dir, cat):
    """GitHub Wiki tab: a generated index that points at the canonical Pages site."""
    systems = cat['systems']
    n_cmd = len(cat['commands'])
    n_nodes = len(cat['nodes'])
    n_keys = sum(len([c for c in ent if not c['container']]) for _, ent in cat['configs'])
    n_files = len(cat['configs'])
    n_lang = len(cat['lang_stats'])

    start = [
        _wiki_link('Обзор', '',
                   'что это за форк и чем справочник отличается от апстрима ZeroG.'),
        _wiki_link('Быстрый старт', 'quickstart/',
                   'установка, первые настройки и выдача прав.'),
    ]
    updates = [
        _wiki_link('Защита платных зон', 'systems/teleportation/',
                   'если в Farmstead Claims (или исходном RegionGuard) у региона стоит `teleport-escape deny`, NeoEssentials не создаёт там `/sethome`, не выполняет TPA с посадкой в зоне и не даёт `/back` вернуться в неё. Настройка флага доступна только OP или с `regionguard.admin.flag.teleport-escape`.'),
        _wiki_link('Команды без учёта регистра', 'systems/utility/',
                   'корневые команды NeoEssentials и их алиасы можно вводить как /HOME, /Home или /home; аргументы и подкоманды не меняются.'),
        _wiki_link('Русская справка Farmstead Claims', 'systems/utility/',
                   'в /help каждая из 17 веток /rg показывает своё русское описание, включая claim, list и flag clear.'),
        _wiki_link('Личные сообщения и TAB', 'quickstart/',
                   '/me — алиас личного сообщения; ники в базовых командах подсказываются по Tab.'),
        _wiki_link('Сохранение /fly и /god', 'systems/utility/',
                   'включённые состояния сохраняются по UUID и возвращаются после входа при сохранённом праве.'),
    ]
    reference = [
        _wiki_link('Все команды', 'commands/',
                   'синтаксис, алиасы и узел права, который мод проверяет на самом деле.'),
        _wiki_link('Права', 'permissions/',
                   'каждый узел из реестра; при requireExplicitGrant defaultValue сам не грант.'),
        _wiki_link('Конфигурация', 'config/',
                   'каждый ключ из JSON-файлов, которые мод кладёт на сервер.'),
        _wiki_link('Локализация', 'localization/',
                   'покрытие встроенных языков и как задать язык сервера.'),
    ]
    help_items = [
        _wiki_link('Известные проблемы', 'troubleshooting/',
                   'симптом, причина и что с этим делать — по всем системам.'),
    ]
    system_items = [
        _wiki_link(s['title'], 'systems/%s/' % s['slug'], s.get('lede') or s['title'])
        for s in systems
    ]

    home = '\n'.join([
        '# Farmstead NeoEssentials',
        '',
        'Каноническая документация — сайт с поиском и таблицами, собранными из исходников:',
        '',
        '**[%s](%s)**' % (PUBLIC_SITE.replace('https://', ''), doc_url()),
        '',
        'Эта вкладка Wiki — только оглавление. Таблицы команд, прав и конфигов сюда не копируются, '
        'чтобы не разъехаться с кодом.',
        '',
        'В справочнике сейчас: **%d** команд, **%d** узлов прав, **%d** ключей конфига в %d файлах, '
        '**%d** языков. На сайте клавиша `/` открывает поиск.'
        % (n_cmd, n_nodes, n_keys, n_files, n_lang),
        '',
        '## Начало',
        '',
        '\n'.join(start),
        '',
        '## Последние изменения',
        '',
        '\n'.join(updates),
        '',
        '## Справочник',
        '',
        '\n'.join(reference),
        '',
        '## Системы',
        '',
        '\n'.join(system_items),
        '',
        '## Помощь',
        '',
        '\n'.join(help_items),
        '',
        '---',
        '',
        'Форк для [%s](%s). Не путать с апстримом [%s](%s).'
        % ('Farmstead Minecraft', 'https://farmsteadminecraft.online',
           'ZeroG NeoEssentials', 'https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials'),
        '',
    ])

    def side_link(title, rel):
        return '* [%s](%s)' % (title, doc_url(rel))

    sidebar_systems = '\n'.join(side_link(s['title'], 'systems/%s/' % s['slug']) for s in systems)
    sidebar = '\n'.join([
        '**[Farmstead NeoEssentials](%s)**' % doc_url(),
        '',
        'Полный справочник на сайте, не в этой вкладке.',
        '',
        '### Начало',
        side_link('Обзор', ''),
        side_link('Быстрый старт', 'quickstart/'),
        '',
        '### Справочник',
        side_link('Все команды', 'commands/'),
        side_link('Права', 'permissions/'),
        side_link('Конфигурация', 'config/'),
        side_link('Локализация', 'localization/'),
        '',
        '### Системы',
        sidebar_systems,
        '',
        '### Помощь',
        side_link('Известные проблемы', 'troubleshooting/'),
        '',
    ])

    footer = (
        '**Farmstead NeoEssentials** — форк для [Farmstead Minecraft](https://farmsteadminecraft.online). '
        'Канон: [%s](%s).'
        % (PUBLIC_SITE.replace('https://', ''), doc_url())
    )

    write(out_dir, 'Home.md', home)
    write(out_dir, '_Sidebar.md', sidebar)
    write(out_dir, '_Footer.md', footer + '\n')
    print('Wrote GitHub wiki index to %s' % out_dir)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default=os.path.join(os.path.dirname(os.path.abspath(__file__)), '_site'))
    ap.add_argument('--base', default='/')
    ap.add_argument('--wiki-out', default=None,
                    help='also write GitHub Wiki index (Home.md, _Sidebar.md) to this directory')
    ap.add_argument('--strict', action='store_true', help='exit non-zero if anything was warned about')
    a = ap.parse_args()
    WARNINGS.clear()
    rc = build(a.out, a.base, wiki_out=a.wiki_out, lang='ru')
    rc = build(os.path.join(a.out, 'en'), a.base, lang='en') or rc
    if a.strict and WARNINGS:
        return 1
    return rc


if __name__ == '__main__':
    sys.exit(main())
