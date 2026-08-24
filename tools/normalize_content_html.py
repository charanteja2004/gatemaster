#!/usr/bin/env python3
"""
Makes the bundled HTML articles readable on a phone.

None of the ~220 source documents declares a viewport, so WebView lays them out
at desktop width and scales down — tiny text, pinch-zoom on every page. They
also hardcode a light background, which makes dark mode impossible.

This rewrites each document's <head> to add, if missing:
  * <meta charset="utf-8">
  * <meta name="viewport" content="width=device-width, initial-scale=1">
  * <link rel="stylesheet" href="../reader.css">   (last, so it wins)

The link goes last on purpose: the per-file <style> blocks stay where they are,
and reader.css overrides them by cascade order rather than by deleting anything.

Safe to re-run — it detects its own marker and skips files already done.

Run:  python tools/normalize_content_html.py
      python tools/normalize_content_html.py --check   (report only, no writes)
"""

from __future__ import annotations

import io
import os
import re
import sys

ROOT = os.path.normpath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets")
)

MARKER = "gatemaster-reader"

VIEWPORT = '<meta name="viewport" content="width=device-width, initial-scale=1">'
CHARSET = '<meta charset="utf-8">'
STYLESHEET = f'<link rel="stylesheet" href="../reader.css" data-{MARKER}="1">'

HEAD_CLOSE_RE = re.compile(r"</head\s*>", re.I)
HEAD_OPEN_RE = re.compile(r"<head[^>]*>", re.I)
HTML_OPEN_RE = re.compile(r"<html[^>]*>", re.I)
BODY_OPEN_RE = re.compile(r"<body[^>]*>", re.I)
CHARSET_RE = re.compile(r"<meta[^>]+charset", re.I)
VIEWPORT_RE = re.compile(r'<meta[^>]+name\s*=\s*["\']viewport', re.I)


def read_text(path: str) -> str:
    raw = io.open(path, "rb").read()
    for enc in ("utf-8-sig", "utf-8", "cp1252", "latin-1"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            continue
    return raw.decode("utf-8", errors="replace")


def html_files() -> list[str]:
    out = []
    for entry in sorted(os.listdir(ROOT)):
        folder = os.path.join(ROOT, entry)
        if not os.path.isdir(folder) or entry in ("pdfs", "previousPapers"):
            continue
        for name in sorted(os.listdir(folder)):
            if name.lower().endswith(".html"):
                out.append(os.path.join(folder, name))
    return out


def normalize(source: str) -> tuple[str, list[str]]:
    """Returns (new_source, list of things added)."""
    added = []
    additions = []

    if not CHARSET_RE.search(source):
        additions.append(CHARSET)
        added.append("charset")
    if not VIEWPORT_RE.search(source):
        additions.append(VIEWPORT)
        added.append("viewport")
    if MARKER not in source:
        additions.append(STYLESHEET)
        added.append("stylesheet")

    if not additions:
        return source, []

    block = "\n" + "\n".join("    " + a for a in additions) + "\n"

    # Preferred: just before </head>, so our stylesheet is the last one in.
    m = HEAD_CLOSE_RE.search(source)
    if m:
        return source[: m.start()] + block + source[m.start():], added

    # No </head>: synthesise one after <html>, or before <body>.
    m = HEAD_OPEN_RE.search(source)
    if m:
        return source[: m.end()] + block + source[m.end():], added

    m = HTML_OPEN_RE.search(source) or BODY_OPEN_RE.search(source)
    insert_at = m.start() if m else 0
    return source[:insert_at] + "<head>" + block + "</head>\n" + source[insert_at:], added


def main(argv: list[str]) -> int:
    check_only = "--check" in argv

    if not os.path.isdir(ROOT):
        print("assets folder not found: %s" % ROOT, file=sys.stderr)
        return 1

    files = html_files()
    changed = skipped = 0
    counts: dict[str, int] = {}

    for path in files:
        source = read_text(path)
        new_source, added = normalize(source)
        if not added:
            skipped += 1
            continue
        for a in added:
            counts[a] = counts.get(a, 0) + 1
        changed += 1
        if not check_only:
            io.open(path, "w", encoding="utf-8", newline="\n").write(new_source)

    verb = "would update" if check_only else "updated"
    print("%d files scanned" % len(files))
    print("  %s %d, already normalized %d" % (verb, changed, skipped))
    for key in ("charset", "viewport", "stylesheet"):
        if key in counts:
            print("    +%-11s %d" % (key, counts[key]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
