"""Brings the Modrinth project page in line with the repository: title, summary, description, categories, sides,
links, license, icon and gallery, all from listing/modrinth.json (paths there are relative to the repository root).

    MODRINTH_TOKEN=... MODRINTH_PROJECT_ID=... python3 tools/modrinth_listing.py [--dry-run]

Runs in CI (.github/workflows/listing.yml), where the token lives as a secret. The gallery is replaced as a whole:
old images are removed, the configured ones uploaded in order. Versions are not touched (farmstead-release.yml
adds them).

CurseForge has no API for the project page, so the same gallery is laid out for a hand upload:

    python3 tools/modrinth_listing.py --curseforge-gallery build/curseforge-gallery

copies the images as 01-..., 02-... in gallery order and writes CAPTIONS.md (title and description per file).
No token is needed for that.
"""
import json
import mimetypes
import os
import shutil
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://api.modrinth.com/v2"
ROOT = Path(__file__).resolve().parent.parent
CONFIG = ROOT / "listing" / "modrinth.json"
DRY = "--dry-run" in sys.argv
TOKEN = os.environ.get("MODRINTH_TOKEN", "")
PROJECT = os.environ.get("MODRINTH_PROJECT_ID", "")
UA = "farmstead-mods/listing-sync (github.com/shtanko-michael)"


def load_config():
    cfg = json.loads(CONFIG.read_text(encoding="utf-8"))
    missing = [p for p in [cfg["body"], cfg["icon"], *(g["file"] for g in cfg["gallery"])] if not (ROOT / p).is_file()]
    if missing:
        sys.exit("missing files: " + ", ".join(missing))
    return cfg


def call(method, path, body=None, data=None, content_type=None):
    url = API + path
    headers = {"Authorization": TOKEN, "User-Agent": UA}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        content_type = "application/json"
    if content_type:
        headers["Content-Type"] = content_type
    if DRY and method != "GET":
        print(f"  dry run: {method} {path.split('?')[0]} ({len(data or b'')} bytes)")
        return None
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            raw = r.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        sys.exit(f"{method} {path.split('?')[0]}: HTTP {e.code} {e.read().decode('utf-8', 'replace')[:500]}")


def curseforge_gallery(out):
    cfg = load_config()
    out = Path(out)
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)
    rows = []
    for i, g in enumerate(cfg["gallery"], 1):
        src = ROOT / g["file"]
        name = f"{i:02d}-{src.name}"
        shutil.copyfile(src, out / name)
        rows.append(f"| {i} | {name} | {g['title']} | {g.get('description', '')} |")
    (out / "CAPTIONS.md").write_text(
        "# CurseForge gallery: upload in this order (Images -> Upload)\n\n"
        "| # | File | Title | Description |\n| --- | --- | --- | --- |\n" + "\n".join(rows) + "\n",
        encoding="utf-8")
    print(f"{len(rows)} image(s) and CAPTIONS.md in {out}")


def sync_modrinth():
    if not TOKEN or not PROJECT:
        sys.exit("MODRINTH_TOKEN and MODRINTH_PROJECT_ID are required")
    cfg = load_config()
    project = call("GET", f"/project/{PROJECT}")
    print(f"project {project['slug']} ({project['status']}), gallery {len(project['gallery'])} image(s)")

    patch = {
        "title": cfg["title"],
        "description": cfg["summary"],
        "body": (ROOT / cfg["body"]).read_text(encoding="utf-8"),
        "categories": cfg["categories"],
        "additional_categories": cfg.get("additional_categories", []),
        "client_side": cfg["client_side"],
        "server_side": cfg["server_side"],
        "license_id": cfg["license_id"],
        "license_url": cfg.get("license_url"),
        "source_url": cfg.get("source_url"),
        "issues_url": cfg.get("issues_url"),
        "wiki_url": cfg.get("wiki_url"),
    }
    if "discord_url" in cfg:  # left alone unless configured, so a hand-set link survives
        patch["discord_url"] = cfg["discord_url"]
    for key in ("title", "description", "categories", "additional_categories", "client_side", "server_side",
                "license_id", "license_url", "source_url", "issues_url", "wiki_url", "discord_url"):
        if key not in patch:
            continue
        before = project.get(key)
        if key == "license_id":
            before = (project.get("license") or {}).get("id")
        elif key == "license_url":
            before = (project.get("license") or {}).get("url")
        mark = "=" if before == patch[key] else "~"
        print(f"  {mark} {key}: {before!r} -> {patch[key]!r}")
    print(f"  {'=' if project.get('body') == patch['body'] else '~'} body: {len(project.get('body') or '')} -> {len(patch['body'])} chars")
    call("PATCH", f"/project/{PROJECT}", body=patch)
    print("details and description " + ("would be updated" if DRY else "updated"))

    icon = ROOT / cfg["icon"]
    call("PATCH", f"/project/{PROJECT}/icon?ext={icon.suffix[1:]}", data=icon.read_bytes(),
         content_type=mimetypes.guess_type(icon.name)[0])
    print(f"icon {icon.name}")

    for img in project["gallery"]:
        call("DELETE", f"/project/{PROJECT}/gallery?url={urllib.parse.quote(img['url'], safe='')}")
    print(f"removed {len(project['gallery'])} old gallery image(s)")
    for i, g in enumerate(cfg["gallery"]):
        f = ROOT / g["file"]
        q = urllib.parse.urlencode({
            "ext": f.suffix[1:],
            "featured": str(bool(g.get("featured"))).lower(),
            "title": g["title"],
            "description": g.get("description", ""),
            "ordering": i,
        })
        call("POST", f"/project/{PROJECT}/gallery?{q}", data=f.read_bytes(), content_type=mimetypes.guess_type(f.name)[0])
        print(f"gallery {i + 1}: {f.name} ({f.stat().st_size // 1024} KB) - {g['title']}")
        if not DRY:
            time.sleep(1)  # be gentle with the rate limit
    print("done" + (" (dry run, nothing changed)" if DRY else ""))


if __name__ == "__main__":
    if "--curseforge-gallery" in sys.argv:
        i = sys.argv.index("--curseforge-gallery")
        curseforge_gallery(sys.argv[i + 1] if i + 1 < len(sys.argv) else ROOT / "build" / "curseforge-gallery")
    else:
        sync_modrinth()
