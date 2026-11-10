#!/usr/bin/env python3
"""Checks that every shipped language file is complete and safe to load.

en_us.json is the reference. For every other data/lang/*.json:

  missing      a key en_us has (non-empty string, not a "_comment") is absent or empty
  extra        a key en_us does not have (stale after a rename/removal)
  placeholder  {0} / {name} / %s placeholders differ from en_us (as a multiset). MessageUtil
               substitutes by plain replace ('' becomes ', %s is the first argument), so quotes
               are harmless — but a missing placeholder silently drops data
  colour       the sequence of §x / &x formatting codes differs from en_us (a warning only: word
               order legitimately moves colours around — read it, don't blindly copy en_us)
  tags         <gradient:…> / <rainbow> / </…>-style tags differ from en_us
  newline      the number of literal \\n differs from en_us
  untranslated the value is identical to en_us although it has real words in it (reported only
               with --untranslated; brand names, commands and permission lists are legitimately
               identical)

Exit code 1 when anything but "colour" or "untranslated" is found, so the docs build (--strict) and CI fail
on an incomplete translation.

Usage:  python tools/lang_check.py [--untranslated] [--lang ru_ru] [--quiet]
"""

import argparse
import json
import os
import re
import sys
from collections import Counter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LANG_DIR = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'lang')

RE_PLACEHOLDER = re.compile(r'\{[^{}\s]*\}|%s')
RE_COLOUR = re.compile(r'[§&]([0-9a-fk-orA-FK-OR]|#[0-9a-fA-F]{6})')
RE_TAG = re.compile(r'</?(?:gradient|rainbow|color|bold|italic|underline|reset)[^>]*>')
RE_WORDS = re.compile(r'[A-Za-z]{4,}')
SOFT = ('colour', 'untranslated')


def load(path):
    with open(path, encoding='utf-8') as fh:
        return json.load(fh)


def messages(data):
    return {k: v for k, v in data.items()
            if not k.startswith('_') and isinstance(v, str) and v.strip()}


def words(text):
    stripped = RE_COLOUR.sub('', RE_PLACEHOLDER.sub('', text))
    stripped = re.sub(r'/\S+|\S+\.\S+', '', stripped)  # commands, nodes, keys
    return RE_WORDS.findall(stripped)


def check(code, data, en, untranslated=False):
    """Returns a list of (kind, key, detail)."""
    out = []
    msgs = messages(data)
    for key, ref in en.items():
        val = msgs.get(key)
        if val is None:
            out.append(('missing', key, ''))
            continue
        if Counter(RE_PLACEHOLDER.findall(val)) != Counter(RE_PLACEHOLDER.findall(ref)):
            out.append(('placeholder', key, '%s vs %s' % (RE_PLACEHOLDER.findall(val), RE_PLACEHOLDER.findall(ref))))
        if RE_COLOUR.findall(val) != RE_COLOUR.findall(ref):
            out.append(('colour', key, ''))
        if Counter(RE_TAG.findall(val)) != Counter(RE_TAG.findall(ref)):
            out.append(('tags', key, ''))
        if val.count('\\n') != ref.count('\\n') or val.count('\n') != ref.count('\n'):
            out.append(('newline', key, ''))
        if untranslated and val == ref and words(ref):
            out.append(('untranslated', key, ref[:80]))
    for key in msgs:
        if key not in en:
            out.append(('extra', key, ''))
    return out


def run(only=None, untranslated=False, quiet=False):
    en = messages(load(os.path.join(LANG_DIR, 'en_us.json')))
    failed = False
    report = {}
    for name in sorted(os.listdir(LANG_DIR)):
        if not name.endswith('.json') or name == 'en_us.json':
            continue
        code = name[:-5]
        if only and code != only:
            continue
        problems = check(code, load(os.path.join(LANG_DIR, name)), en, untranslated)
        report[code] = problems
        kinds = Counter(k for k, _, _ in problems)
        hard = {k: n for k, n in kinds.items() if k not in SOFT}
        if hard:
            failed = True
        if not quiet:
            summary = ', '.join('%s %d' % (k, n) for k, n in sorted(kinds.items())) or 'ok'
            print('%-6s %s' % (code, summary))
            for kind, key, detail in problems[:40]:
                print('   %-12s %s %s' % (kind, key, detail))
            if len(problems) > 40:
                print('   … %d more' % (len(problems) - 40))
    return failed, report


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--lang', help='check one language, e.g. de_de')
    ap.add_argument('--untranslated', action='store_true', help='also list values identical to en_us')
    ap.add_argument('--quiet', action='store_true')
    a = ap.parse_args()
    failed, _ = run(a.lang, a.untranslated, a.quiet)
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main())
