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
ASSETS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'assets')

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
        base = slugify(label)
        seen[base] = seen.get(base, 0) + 1
        anchor = base if seen[base] == 1 else '%s-%d' % (base, seen[base])
        toc.append({'level': int(level), 'text': RE_TAGS.sub('', label).strip(), 'id': anchor})
        return '<h%s id="%s">%s<a class="anchor" href="#%s" aria-label="Ссылка на раздел">#</a></h%s>' % (
            level, anchor, inner, anchor, level)

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


def page(rel, title, subtitle, body, toc=None, active='', wide=False, extra_head=''):
    up = '../' * rel
    nav_html = []
    for group in NAV:
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
            toc_html = ('<aside class="toc"><p class="toc-title">На этой странице</p>'
                        '<ul>%s</ul></aside>' % items)

    site_name = 'Farmstead NeoEssentials'
    doc_title = title if title == site_name else '%s · %s' % (title, site_name)

    return """<!doctype html>
<html lang="ru" data-base="{up}">
<head>
<meta charset="utf-8">
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
<link rel="stylesheet" href="{up}assets/site.css">
{extra_head}
</head>
<body{bodycls}>
<a class="skip" href="#main">К содержимому</a>
<header class="topbar">
  <a class="brand" href="{up}index.html" aria-label="Farmstead NeoEssentials">
    <img class="brand-logo" src="{up}assets/farmstead.png" alt="" width="36" height="36">
    <span class="brand-text">
      <span class="brand-kicker">Farmstead</span>
      <span class="brand-name">NeoEssentials</span>
    </span>
  </a>
  <div class="search-wrap">
    <input id="q" type="search" placeholder="Поиск по командам, правам и конфигу…" autocomplete="off" spellcheck="false">
    <div id="results" hidden></div>
  </div>
  <button id="theme" type="button" aria-label="Переключить тему">◐</button>
</header>
<div class="shell">
  <nav class="sidenav" aria-label="Разделы">{nav}</nav>
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
  <p><strong>Farmstead NeoEssentials</strong> — форк мода для сервера
  <a href="https://farmsteadminecraft.online">Farmstead Minecraft</a>.
  Не путать с апстримом
  <a href="https://github.com/ZeroG-Network-PTY-LTD/NeoEssentials">ZeroG NeoEssentials</a>.</p>
</footer>
<script src="{up}assets/site.js" defer></script>
</body>
</html>
""".format(up=up, title=e(title), doc_title=e(doc_title), subtitle=e(subtitle),
           body=body, nav=''.join(nav_html),
           toc=toc_html, extra_head=extra_head,
           bodycls=' class="wide"' if wide else '')


def write(out_dir, rel_path, content):
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


def command_row(cmd, curated):
    extra = curated.get(cmd['name'].lower(), {})
    syntax = extra.get('syntax')
    if not syntax:
        variants = cmd.get('syntaxVariants') or []
        syntax = ' | '.join(variants) if variants else ('/' + cmd['name'])
    perm = cmd.get('permission')
    perm_cell = code(perm) if perm else '<span class="muted">нет узла</span>'
    if cmd.get('permissionInferred'):
        perm_cell += ' <span class="tag tag-soft" title="Узел не объявлен явно — выведен по соглашению реестра">по соглашению</span>'
    if cmd.get('helpCatalogOnly'):
        perm_cell += ' <span class="tag tag-soft" title="Запись в каталоге /help, не отдельная команда мода">каталог /help</span>'
    aliases = ', '.join(code(a) for a in cmd.get('aliases', [])) or '<span class="muted">—</span>'
    desc = e(extra.get('description') or cmd['description'])
    notes = extra.get('notes')
    if notes:
        desc += '<span class="row-note">%s</span>' % e(notes)
    return ('<tr id="%s" data-search="%s">'
            '<td class="c-name"><code>/%s</code></td>'
            '<td class="c-syntax"><code>%s</code></td>'
            '<td class="c-alias">%s</td>'
            '<td class="c-perm">%s</td>'
            '<td class="c-desc">%s</td>'
            '</tr>') % (
        cmd_anchor(cmd['name']),
        e(' '.join([cmd['name'], syntax, perm or '', ' '.join(cmd.get('aliases', [])),
                    extra.get('description') or cmd['description']]).lower()),
        e(cmd['name']), e(syntax), aliases, perm_cell, desc)


def commands_table(cmds, curated):
    rows = ''.join(command_row(c, curated) for c in cmds)
    return ('<div class="scroller"><table class="grid cmd-table">'
            '<thead><tr><th>Команда</th><th>Синтаксис</th><th>Алиасы</th>'
            '<th>Право</th><th>Описание</th></tr></thead>'
            '<tbody>%s</tbody></table></div>') % rows


def permissions_table(nodes):
    rows = []
    for n in nodes:
        default = ('<span class="tag tag-ok">всем</span>' if n['default']
                   else '<span class="tag tag-warn">по выдаче</span>')
        rows.append('<tr data-search="%s"><td><code>%s</code></td><td>%s</td><td>%s</td></tr>'
                    % (e((n['node'] + ' ' + n['description']).lower()), e(n['node']), default,
                       e(n['description'])))
    return ('<div class="scroller"><table class="grid"><thead><tr><th>Узел</th>'
            '<th>По умолчанию</th><th>Что даёт</th></tr></thead><tbody>%s</tbody></table></div>'
            % ''.join(rows))


def config_table(entries):
    rows = []
    for c in entries:
        if c['container']:
            rows.append('<tr class="grp"><td colspan="3"><code>%s</code>%s</td></tr>'
                        % (e(c['key']),
                           (' <span class="muted">— %s</span>' % e(c['description']))
                           if c['description'] else ''))
            continue
        rows.append('<tr data-search="%s"><td><code>%s</code></td><td><code>%s</code></td>'
                    '<td>%s</td></tr>'
                    % (e((c['key'] + ' ' + c['description']).lower()), e(c['key']),
                       e(c['default']), e(c['description']) or '<span class="muted">—</span>'))
    return ('<div class="scroller"><table class="grid"><thead><tr><th>Ключ</th>'
            '<th>По умолчанию</th><th>Описание</th></tr></thead><tbody>%s</tbody></table></div>'
            % ''.join(rows))


def callouts(items, kind, title):
    if not items:
        return ''
    out = ['<section class="callouts %s"><h2 id="%s">%s</h2>' % (kind, slugify(title), e(title))]
    for it in items:
        out.append('<article class="callout"><h3>%s</h3>%s%s</article>' % (
            e(it.get('title', '')),
            md_to_html(it.get('body', '')),
            ('<p class="callout-fix"><strong>Что делать:</strong> %s</p>' % md_to_html(it['fix']).replace('<p>', '').replace('</p>', ''))
            if it.get('fix') else ''))
    out.append('</section>')
    return ''.join(out)


def examples_block(items):
    if not items:
        return ''
    out = ['<section class="examples"><h2 id="primery">Примеры</h2>']
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
    WARNINGS.clear()
    print('Extracting from source…')
    commands = extract_commands()
    nodes = extract_permissions()
    lang_stats = extract_lang_stats()
    config_files = sorted(f for f in os.listdir(CONFIG_DIR) if f.endswith('.json')) \
        if os.path.isdir(CONFIG_DIR) else []
    configs = [(f, extract_config(f)) for f in config_files]
    print('  %d commands, %d permission nodes, %d config files, %d languages'
          % (len(commands), len(nodes), len(configs), len(lang_stats)))

    meta = read_json(os.path.join(CONTENT_DIR, '_meta.json'), None)
    if meta is None:
        warn('docs/site/content/_meta.json missing — falling back to permission-prefix grouping')
        meta = {'systems': []}
    curated_cmds = read_json(os.path.join(CONTENT_DIR, 'commands.json'), {}) or {}
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

def build(out_dir, base):
    global NAV
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
        {'title': 'Начало', 'items': [
            {'id': 'index', 'title': 'Обзор', 'href': 'index.html'},
            {'id': 'quickstart', 'title': 'Быстрый старт', 'href': 'quickstart/index.html'},
        ]},
        {'title': 'Справочник', 'items': [
            {'id': 'commands', 'title': 'Все команды', 'href': 'commands/index.html'},
            {'id': 'permissions', 'title': 'Права', 'href': 'permissions/index.html'},
            {'id': 'config', 'title': 'Конфигурация', 'href': 'config/index.html'},
            {'id': 'lang', 'title': 'Локализация', 'href': 'localization/index.html'},
        ]},
        {'title': 'Системы', 'items': [
            {'id': 'sys-' + s['slug'], 'title': s['title'], 'href': 'systems/%s/index.html' % s['slug']}
            for s in systems
        ]},
        {'title': 'Помощь', 'items': [
            {'id': 'troubleshooting', 'title': 'Известные проблемы', 'href': 'troubleshooting/index.html'},
        ]},
    ]

    search = []

    # ---- index -------------------------------------------------------------
    ru = next((l for l in lang_stats if l['code'] == 'ru_ru'), None)
    stat_tiles = [
        ('Команд', len(commands), 'объявлено в реестре мода'),
        ('Узлов прав', len(nodes), 'с описанием и значением по умолчанию'),
        ('Ключей конфига', sum(len([c for c in ent if not c['container']]) for _, ent in configs),
         'в %d файлах' % len(configs)),
        ('Языков', len(lang_stats), 'русский — %d%%' % (ru['percent'] if ru else 0)),
    ]
    tiles = ''.join('<div class="tile"><p class="tile-n">%s</p><p class="tile-k">%s</p>'
                    '<p class="tile-s">%s</p></div>' % (v, e(k), e(s)) for k, v, s in stat_tiles)

    intro = read_md_content('index.md')
    body = ('<section class="tiles">%s</section>' % tiles) + intro
    write(out_dir, 'index.html', page(0, 'Farmstead NeoEssentials',
                                      'Справочник форка для сервера Farmstead Minecraft: команды, права, конфигурация и разбор проблем.',
                                      body, active='index'))

    # ---- quick start -------------------------------------------------------
    qs = read_md_content('quickstart.md')
    if qs:
        b, toc = add_heading_ids(qs)
        write(out_dir, 'quickstart/index.html',
              page(1, 'Быстрый старт', 'Установка, первые настройки и выдача прав.', b, toc,
                   active='quickstart'))

    # ---- commands ----------------------------------------------------------
    all_cmds = sorted(commands.values(), key=lambda c: c['name'])
    filters = ''.join(
        '<button type="button" class="chip" data-filter="%s">%s <span class="count">%d</span></button>'
        % (e(s['slug']), e(s['title']), len(s['_commands']))
        for s in systems if s['_commands'])
    if orphans:
        # Commands no system claims stay reachable instead of being quietly invisible.
        filters += ('<button type="button" class="chip" data-filter="other">Прочее '
                    '<span class="count">%d</span></button>' % len(orphans))
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
                       'u': 'commands/index.html#' + cmd_anchor(c['name']), 'k': 'команда'})
    table = ('<div class="scroller"><table class="grid cmd-table"><thead><tr><th>Команда</th>'
             '<th>Синтаксис</th><th>Алиасы</th><th>Право</th><th>Описание</th></tr></thead>'
             '<tbody>%s</tbody></table></div>' % ''.join(rows))
    note = ('<div class="note"><p>Таблица собирается из регистраций команд в исходниках при каждой '
            'сборке сайта, поэтому колонка «Право» показывает узел, который мод проверяет на самом '
            'деле. Синтаксис и примечания — выверенная вручную часть.</p></div>')
    write(out_dir, 'commands/index.html',
          page(1, 'Все команды', '%d команд: синтаксис, алиасы, право доступа и описание.' % len(all_cmds),
               note + '<div class="filters"><button type="button" class="chip on" data-filter="*">Все</button>' +
               filters + '</div><div class="tablefilter"><input id="tq" type="search" '
               'placeholder="Фильтр по таблице…" autocomplete="off"></div>' + table,
               active='commands', wide=True))

    # ---- permissions -------------------------------------------------------
    by_cat = {}
    for n in nodes.values():
        by_cat.setdefault(n['category'], []).append(n)
    parts = []
    toc = []
    for cat in sorted(by_cat, key=lambda c: (-len(by_cat[c]), c)):
        title = '%s (%s)' % (CATEGORY_RU.get(cat, cat), cat)
        anchor = slugify(cat)
        toc.append({'level': 2, 'text': title, 'id': anchor})
        parts.append('<h2 id="%s">%s <span class="count">%d</span></h2>' % (anchor, e(title), len(by_cat[cat])))
        parts.append(permissions_table(sorted(by_cat[cat], key=lambda x: x['node'])))
    for n in nodes.values():
        search.append({'t': n['node'], 'd': n['description'], 'u': 'permissions/index.html', 'k': 'право'})
    write(out_dir, 'permissions/index.html',
          page(1, 'Права', '%d узлов из реестра прав — что даёт каждый и кому доступен по умолчанию.' % len(nodes),
               '<div class="tablefilter"><input id="tq" type="search" placeholder="Фильтр по узлам…" '
               'autocomplete="off"></div>' + ''.join(parts), toc, active='permissions', wide=True))

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
                           'u': 'config/index.html#' + anchor, 'k': 'конфиг'})
    write(out_dir, 'config/index.html',
          page(1, 'Конфигурация', 'Каждый ключ, его значение по умолчанию и пояснение — прямо из файлов, которые ставит мод.',
               '<div class="tablefilter"><input id="tq" type="search" placeholder="Фильтр по ключам…" '
               'autocomplete="off"></div>' + ''.join(parts), toc, active='config', wide=True))

    # ---- localization ------------------------------------------------------
    rows = ''.join(
        '<tr><td><code>%s</code></td><td class="num">%d</td><td class="num">%d</td>'
        '<td><div class="bar"><span style="width:%d%%"></span></div><span class="pct">%d%%</span></td></tr>'
        % (e(l['code']), l['covered'], l['total'], l['percent'], l['percent']) for l in lang_stats)
    loc_md = read_md_content('localization.md')
    write(out_dir, 'localization/index.html',
          page(1, 'Локализация', 'Какие языки поставляются и насколько полно переведены.',
               '<div class="scroller"><table class="grid"><thead><tr><th>Язык</th><th>Переведено</th>'
               '<th>Всего ключей</th><th>Покрытие</th></tr></thead><tbody>%s</tbody></table></div>%s'
               % (rows, loc_md), active='lang'))

    # ---- systems -----------------------------------------------------------
    for sysdef in systems:
        slug = sysdef['slug']
        chunks = []
        extra = sysdef['_content']

        if sysdef.get('summary'):
            chunks.append('<div class="note"><p>%s</p></div>' % e(sysdef['summary']))

        if sysdef['_commands']:
            chunks.append('<h2 id="komandy">Команды <span class="count">%d</span></h2>'
                          % len(sysdef['_commands']))
            chunks.append(commands_table(sysdef['_commands'], curated_cmds))

        own_md = read_md_content('systems/%s.md' % slug)
        if own_md:
            chunks.append(own_md)

        chunks.append(examples_block(extra.get('examples', [])))
        chunks.append(callouts(extra.get('edgeCases', []), 'edge', 'Тонкие места'))
        chunks.append(callouts(extra.get('knownIssues', []), 'issue', 'Известные проблемы'))

        cfg_keys = sysdef.get('configFiles', [])
        cfg_chunks = []
        for fname in cfg_keys:
            entries = dict(configs).get(fname)
            if entries:
                cfg_chunks.append('<h3><code>%s</code></h3>%s' % (e(fname), config_table(entries)))
        if cfg_chunks:
            chunks.append('<h2 id="konfiguraciya">Конфигурация</h2>' + ''.join(cfg_chunks))

        if sysdef['_nodes']:
            chunks.append('<h2 id="prava">Права <span class="count">%d</span></h2>' % len(sysdef['_nodes']))
            chunks.append(permissions_table(sysdef['_nodes']))

        upstream = sysdef.get('upstreamDoc')
        if upstream:
            path = os.path.join(UPSTREAM_WIKI, upstream)
            if os.path.exists(path):
                text = read(path)
                text = re.sub(r'^#\s+.*\n', '', text, count=1)
                text = rewrite_upstream_links(text, wiki_pages)
                chunks.append('<h2 id="podrobnoe-rukovodstvo">Подробное руководство</h2>'
                              '<div class="upstream-note">Раздел ниже — исходная документация '
                              'проекта (<code>docs/Wiki/%s</code>). Ссылки на другие страницы '
                              'вики переписаны на этот сайт; текст самих руководств не '
                              'редактируется, чтобы синк с апстримом не давал конфликтов.</div>%s'
                              % (e(upstream), md_to_html(text)))
            else:
                warn('system "%s" points at missing upstream doc %s' % (slug, upstream))

        body, toc = add_heading_ids(''.join(chunks))
        write(out_dir, 'systems/%s/index.html' % slug,
              page(2, sysdef['title'], sysdef.get('lede', ''), body, toc,
                   active='sys-' + slug, wide=True))
        search.append({'t': sysdef['title'], 'd': sysdef.get('lede', ''),
                       'u': 'systems/%s/index.html' % slug, 'k': 'система'})

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
                         ('<p class="callout-fix"><strong>Что делать:</strong> %s</p>'
                          % md_to_html(it['fix']).replace('<p>', '').replace('</p>', ''))
                         if it.get('fix') else ''))
    body = ts_md + ('<section class="callouts issue">%s</section>' % ''.join(blocks) if blocks else '')
    b, toc = add_heading_ids(body)
    write(out_dir, 'troubleshooting/index.html',
          page(1, 'Известные проблемы', 'Симптом, причина и что с этим делать — по всем системам.',
               b, toc, active='troubleshooting'))

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

    print('Wrote %d search entries to %s' % (len(search), out_dir))
    if WARNINGS:
        print('\n%d warning(s):' % len(WARNINGS))
        for w in WARNINGS:
            print('  - ' + w)
    return 0


def read_md_content(rel):
    path = os.path.join(CONTENT_DIR, rel)
    if not os.path.exists(path):
        return ''
    return md_to_html(read(path))


def read_content_json(slug):
    return read_json(os.path.join(CONTENT_DIR, 'systems', slug + '.json'), {}) or {}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default=os.path.join(os.path.dirname(os.path.abspath(__file__)), '_site'))
    ap.add_argument('--base', default='/')
    ap.add_argument('--strict', action='store_true', help='exit non-zero if anything was warned about')
    a = ap.parse_args()
    rc = build(a.out, a.base)
    if a.strict and WARNINGS:
        return 1
    return rc


if __name__ == '__main__':
    sys.exit(main())
