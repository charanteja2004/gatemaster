#!/usr/bin/env python3
"""
Audits the bundled study material for defects that can be found mechanically.

This does NOT verify that the physics is right -- that needs a human who knows
the subject. What it does catch is the large class of problems that make notes
useless regardless of whether the prose is correct: links that go nowhere,
images that were never copied across, articles that stop mid-sentence,
placeholder text that was never replaced, and duplicated bodies where the same
file was pasted twice under different names.

Run:  python tools/audit_content.py
      python tools/audit_content.py --json report.json
"""

from __future__ import annotations

import argparse
import hashlib
import html as html_lib
import io
import json
import os
import re
import sys
from collections import defaultdict

ASSETS = os.path.normpath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets")
)

SKIP_DIRS = {"pdfs", "previousPapers", "tests"}


TAG_RE = re.compile(r"<[^>]+>")
SCRIPT_STYLE_RE = re.compile(r"<(script|style)[^>]*>.*?</\1>", re.S | re.I)
HREF_RE = re.compile(r'<a[^>]+href\s*=\s*["\']([^"\']+)["\']', re.I)
SRC_RE = re.compile(r'<(?:img|iframe)[^>]+src\s*=\s*["\']([^"\']+)["\']', re.I)
HEADING_RE = re.compile(r"<h[1-3][^>]*>(.*?)</h[1-3]>", re.S | re.I)

PLACEHOLDER_PATTERNS = [
    (re.compile(r"\blorem ipsum\b", re.I), "lorem ipsum"),
    (re.compile(r"\bTODO\b"), "TODO marker"),
    (re.compile(r"\bFIXME\b"), "FIXME marker"),
    (re.compile(r"\bcoming soon\b", re.I), "'coming soon'"),
    (re.compile(r"\byour (?:text|content) here\b", re.I), "'your text here'"),
    (re.compile(r"capital of France", re.I), "sample-data question"),
]

# Sentences that end without terminal punctuation suggest a truncated paste.
# A real truncation is a *final* paragraph that stops on a word or comma.
# A paragraph ending in a colon is a lead-in to the list or table that follows,
# and a very short one is a caption -- neither is a defect.
TRUNCATION_TAIL = re.compile(r"[A-Za-z,;]\s*$")
LAST_BLOCK_RE = re.compile(
    r"<(p|ul|ol|table|pre|blockquote|h[1-6]|div)[^>]*>(.*?)</>", re.S | re.I
)
MIN_TAIL_WORDS = 8

MIN_WORDS = 60


def read_text(path: str) -> str:
    raw = io.open(path, "rb").read()
    for enc in ("utf-8-sig", "utf-8", "cp1252", "latin-1"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            continue
    return raw.decode("utf-8", errors="replace")


def visible_text(html: str) -> str:
    body = SCRIPT_STYLE_RE.sub(" ", html)
    body = TAG_RE.sub(" ", body)
    body = body.replace("&nbsp;", " ")
    return " ".join(body.split())


def content_files() -> list[str]:
    out = []
    for entry in sorted(os.listdir(ASSETS)):
        folder = os.path.join(ASSETS, entry)
        if not os.path.isdir(folder) or entry in SKIP_DIRS:
            continue
        for name in sorted(os.listdir(folder)):
            if name.lower().endswith(".html"):
                out.append(os.path.join(folder, name))
    return out


def truncated_tail(html: str) -> str | None:
    """Returns the offending text when the document's final block is a cut-off paragraph."""
    blocks = LAST_BLOCK_RE.findall(SCRIPT_STYLE_RE.sub(" ", html))
    if not blocks:
        return None
    tag, inner = blocks[-1]
    if tag.lower() != "p":
        return None
    tail = visible_text(inner)
    if len(tail.split()) < MIN_TAIL_WORDS:
        return None
    if tail.endswith(("...", "…")):
        return None
    return tail if TRUNCATION_TAIL.search(tail[-40:]) else None


def is_external(url: str) -> bool:
    return url.startswith(("http://", "https://", "mailto:", "tel:", "//"))


def audit() -> dict:
    files = content_files()
    findings: dict[str, list] = defaultdict(list)
    body_hashes: dict[str, list[str]] = defaultdict(list)

    for path in files:
        rel = os.path.relpath(path, ASSETS).replace("\\", "/")
        folder = os.path.dirname(path)
        html = read_text(path)
        text = visible_text(html)
        words = text.split()

        # --- substance ---------------------------------------------------
        if len(words) < MIN_WORDS:
            findings["thin"].append({
                "file": rel, "words": len(words),
                "detail": "only %d words of visible text" % len(words),
            })

        tail = truncated_tail(html)
        if tail:
            findings["truncated"].append({
                "file": rel,
                "detail": "ends mid-sentence: ...%s" % tail[-60:].strip(),
            })

        for pattern, label in PLACEHOLDER_PATTERNS:
            if pattern.search(text):
                findings["placeholder"].append({"file": rel, "detail": label})

        if "�" in html:
            findings["encoding"].append({
                "file": rel,
                "detail": "%d replacement characters" % html.count("�"),
            })

        # --- structure ---------------------------------------------------
        if not HEADING_RE.search(html):
            findings["no_heading"].append({"file": rel, "detail": "no h1/h2/h3"})

        # --- links ---------------------------------------------------------
        for href in HREF_RE.findall(html):
            # hrefs are HTML-escaped in source; "divide&amp;conq.html" is a
            # link to the file "divide&conq.html".
            href = html_lib.unescape(href).strip()
            if not href or href.startswith("#") or is_external(href):
                continue
            target = href.split("#")[0].split("?")[0]
            if not target:
                continue
            resolved = os.path.normpath(os.path.join(folder, target))
            if not os.path.isfile(resolved):
                findings["broken_link"].append({
                    "file": rel, "detail": "-> %s" % href,
                })

        # --- media ---------------------------------------------------------
        for src in SRC_RE.findall(html):
            src = src.strip()
            if not src or is_external(src) or src.startswith("data:"):
                continue
            resolved = os.path.normpath(
                os.path.join(folder, html_lib.unescape(src).split("?")[0])
            )
            if not os.path.isfile(resolved):
                findings["missing_image"].append({
                    "file": rel, "detail": "-> %s" % src,
                })

        # --- duplicates ------------------------------------------------------
        if len(words) >= MIN_WORDS:
            digest = hashlib.sha1(" ".join(words[:400]).lower().encode()).hexdigest()
            body_hashes[digest].append(rel)

    for digest, group in body_hashes.items():
        if len(group) > 1:
            findings["duplicate"].append({
                "file": group[0],
                "detail": "same body as: %s" % ", ".join(group[1:]),
            })

    return {"fileCount": len(files), "findings": dict(findings)}


ORDER = [
    ("placeholder", "Placeholder / sample text left in"),
    ("missing_image", "Image referenced but not present"),
    ("broken_link", "Internal link points at a missing file"),
    ("thin", "Barely any content"),
    ("truncated", "Ends mid-sentence"),
    ("encoding", "Mojibake / replacement characters"),
    ("no_heading", "No heading at all"),
    ("duplicate", "Duplicate body"),
]


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", help="also write the report to this path")
    parser.add_argument("--limit", type=int, default=12, help="rows to print per category")
    args = parser.parse_args(argv)

    if not os.path.isdir(ASSETS):
        print("assets folder not found: %s" % ASSETS, file=sys.stderr)
        return 1

    report = audit()
    findings = report["findings"]

    print("Audited %d HTML articles\n" % report["fileCount"])
    total = 0
    for key, title in ORDER:
        rows = findings.get(key, [])
        total += len(rows)
        if not rows:
            print("  OK   %-42s 0" % title)
            continue
        print("  !!   %-42s %d" % (title, len(rows)))
    print("\n%d findings total\n" % total)

    for key, title in ORDER:
        rows = findings.get(key, [])
        if not rows:
            continue
        print("--- %s (%d) ---" % (title, len(rows)))
        for row in rows[: args.limit]:
            print("  %-34s %s" % (row["file"], row["detail"][:90]))
        if len(rows) > args.limit:
            print("  ... and %d more" % (len(rows) - args.limit))
        print()

    if args.json:
        with io.open(args.json, "w", encoding="utf-8", newline="\n") as fh:
            json.dump(report, fh, ensure_ascii=False, indent=2)
        print("report written to %s" % args.json)

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
