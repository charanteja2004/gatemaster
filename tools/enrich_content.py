#!/usr/bin/env python3
"""
Adds semantic structure to the bundled articles so the reader can style them.

The source documents are undifferentiated prose: a note, a worked example, a
formula and a complexity result all look identical. That is what makes them
tiring to read on a phone. This pass finds those things by the conventions the
authors already used and tags them, so assets/reader.css can give each one a
distinct, skimmable form.

What it does:
  * groups runs of SyntaxHighlighter ".line" divs into one code panel
  * wraps <table> in a scroll container so wide tables stop stretching the page
  * wraps <img> in a <figure> with its alt text as a caption, marking remote
    images so a missing diagram reads as a labelled placeholder offline
  * promotes lead-in paragraphs to callouts: Note, Example, Syntax, Solution,
    Definition, Time/Space Complexity, Step N
  * promotes short centred formula lines to a formula plaque

Everything is idempotent and marker-guarded: re-running changes nothing.

Run:  python tools/enrich_content.py [--check]
"""

from __future__ import annotations

import io
import os
import re
import sys
from collections import Counter

ASSETS = os.path.normpath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets")
)
SKIP_DIRS = {"pdfs", "previousPapers", "tests"}
MARKER = "gm-enriched"

# --------------------------------------------------------------------------
# Callout detection. Ordered: the first pattern that matches a paragraph's
# opening wins, so "Time Complexity" is checked before the generic cases.
# --------------------------------------------------------------------------
CALLOUTS = [
    (r"time\s+complexit(?:y|ies)", "gm-complexity", "Time complexity"),
    (r"space\s+complexit(?:y|ies)", "gm-complexity", "Space complexity"),
    (r"auxiliary\s+space", "gm-complexity", "Auxiliary space"),
    (r"notes?", "gm-note", "Note"),
    (r"important", "gm-key", "Important"),
    (r"key\s+points?", "gm-key", "Key point"),
    (r"remember", "gm-key", "Remember"),
    (r"(?:for\s+)?examples?\s*\d*", "gm-example", "Example"),
    (r"solutions?", "gm-solution", "Solution"),
    (r"definitions?", "gm-definition", "Definition"),
    (r"syntax", "gm-syntax", "Syntax"),
    (r"inputs?", "gm-syntax", "Input"),
    (r"outputs?", "gm-syntax", "Output"),
    (r"advantages?", "gm-note", "Advantages"),
    (r"disadvantages?", "gm-warn", "Disadvantages"),
    (r"limitations?", "gm-warn", "Limitations"),
    (r"applications?", "gm-note", "Applications"),
]

CALLOUT_RES = [
    (re.compile(r"^\s*(?:<(?:strong|b|em|u|span)[^>]*>\s*)*%s\s*:?\s*(?:</(?:strong|b|em|u|span)>\s*)*[:\-–]" % pat,
                re.I), cls, label)
    for pat, cls, label in CALLOUTS
]

PARA_RE = re.compile(r"<p\b([^>]*)>(.*?)</p>", re.S | re.I)
TABLE_RE = re.compile(r"<table\b.*?</table>", re.S | re.I)
IMG_RE = re.compile(r"<img\b[^>]*>", re.I)
LINE_RUN_RE = re.compile(
    r"(?:\s*<div class=\"line[^\"]*\">.*?</div>)+", re.S
)
BODY_RE = re.compile(r"(<body[^>]*>)(.*)(</body>)", re.S | re.I)
TAG_RE = re.compile(r"<[^>]+>")

# A formula states a relation. Symbols alone are not enough: "left-to-right"
# and "enqueue()" are symbol-dense and are not formulas.
RELATION_RE = re.compile(r"(?:=|≤|≥|≠|≈|⇒|→|&lt;=|&gt;=|:=)")
FORMULA_CHARS = set("=+-*/^<>()[]{}|Σ∑∫√≤≥≠≈×÷·")
PROSE_OPENERS = re.compile(
    r"^(?:similarly|for example|consider|note that|suppose|assume|in the above|"
    r"here|where|let us|we |this |that |these |it )", re.I
)


def read_text(path: str) -> str:
    raw = io.open(path, "rb").read()
    for enc in ("utf-8-sig", "utf-8", "cp1252", "latin-1"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            continue
    return raw.decode("utf-8", errors="replace")


def visible(html: str) -> str:
    return " ".join(TAG_RE.sub(" ", html).replace("&nbsp;", " ").split())


def attr(tag: str, name: str) -> str:
    m = re.search(r'%s\s*=\s*["\']([^"\']*)["\']' % name, tag, re.I)
    return m.group(1) if m else ""


def is_remote(src: str) -> bool:
    return src.startswith(("http://", "https://", "//"))


# --------------------------------------------------------------------------

def wrap_code_runs(body: str, stats: Counter) -> str:
    def repl(m: re.Match) -> str:
        run = m.group(0)
        if run.count('class="line') < 2:
            return run
        stats["code"] += 1
        return (
            '\n<div class="gm-code" data-lang="code"><div class="gm-code-body">'
            + run.strip()
            + "</div></div>\n"
        )

    return LINE_RUN_RE.sub(repl, body)


def wrap_tables(body: str, stats: Counter) -> str:
    def repl(m: re.Match) -> str:
        stats["table"] += 1
        return '<div class="gm-table">%s</div>' % m.group(0)

    return TABLE_RE.sub(repl, body)


def wrap_images(body: str, stats: Counter) -> str:
    def repl(m: re.Match) -> str:
        tag = m.group(0)
        src = attr(tag, "src")
        if not src:
            return tag
        alt = attr(tag, "alt").strip()
        remote = is_remote(src)
        stats["figure_remote" if remote else "figure"] += 1
        cls = "gm-figure gm-remote" if remote else "gm-figure"
        caption = "<figcaption>%s</figcaption>" % alt if alt else ""
        return '<figure class="%s">%s%s</figure>' % (cls, tag, caption)

    return IMG_RE.sub(repl, body)


def promote_paragraphs(body: str, stats: Counter) -> str:
    def repl(m: re.Match) -> str:
        attrs, inner = m.group(1), m.group(2)
        text = visible(inner)
        if not text:
            return m.group(0)

        for rx, cls, label in CALLOUT_RES:
            if not (rx.match(inner) or rx.match(text)):
                continue

            # "Example:" on its own line is a heading for the list that
            # follows, not a callout with a body. Boxing a bare label just
            # repeats the word inside a frame, so those become a compact
            # section label instead.
            if is_label_only(text, label):
                stats["lead"] += 1
                return '<p%s class="gm-lead %s-lead">%s</p>' % (attrs, cls, inner)

            stats[cls] += 1
            if cls == "gm-complexity":
                return '<p%s class="gm-complexity" data-label="%s">%s</p>' % (
                    attrs, label, inner,
                )
            return '<div class="gm-callout %s" data-label="%s"><p%s>%s</p></div>' % (
                cls, label, attrs, inner,
            )

        if re.match(r"^\s*step\s*\d+\s*[:\-–]", text, re.I):
            stats["step"] += 1
            return '<p%s class="gm-step">%s</p>' % (attrs, inner)

        if looks_like_formula(text, inner):
            stats["formula"] += 1
            return '<div class="gm-formula"><p%s>%s</p></div>' % (attrs, inner)

        return m.group(0)

    return PARA_RE.sub(repl, body)


def is_label_only(text: str, label: str) -> bool:
    """True when the paragraph is just "Example:" with nothing after it."""
    stripped = text.strip().rstrip(":-– ").strip()
    return len(stripped) <= len(label) + 4


def looks_like_formula(text: str, inner: str) -> bool:
    """
    A formula states a relation, briefly.

    Deliberately conservative: a paragraph wrongly promoted to a formula plaque
    looks far worse than one left as plain prose, so every rule here is a reason
    to reject.
    """
    if not (6 <= len(text) <= 110):
        return False
    if not RELATION_RE.search(text):
        return False
    words = text.split()
    if not (2 <= len(words) <= 14):
        return False
    if PROSE_OPENERS.match(text):
        return False
    # Comma-heavy lines are parameter lists and worked inputs, not formulas.
    if text.count(",") > 2:
        return False
    # Semicolons and braces mean this is a line of code, not mathematics.
    if any(ch in text for ch in ";{}"):
        return False
    # Bare operator lists like "*= , /=" carry no statement.
    if sum(1 for c in text if c.isalpha()) < 3:
        return False
    # A finished sentence is prose even when it contains an equals sign.
    if text.endswith(".") and len(words) > 6:
        return False
    symbols = sum(1 for c in text if c in FORMULA_CHARS)
    letters = sum(1 for c in text if c.isalpha())
    return symbols >= 2 and (not letters or symbols / letters >= 0.15)


def enrich(source: str) -> tuple[str, Counter]:
    stats: Counter = Counter()
    m = BODY_RE.search(source)
    if not m:
        return source, stats

    head, body, tail = m.group(1), m.group(2), m.group(3)

    body = wrap_code_runs(body, stats)
    body = wrap_tables(body, stats)
    body = wrap_images(body, stats)
    body = promote_paragraphs(body, stats)

    head = head.rstrip(">") + ' data-%s="1">' % MARKER
    return source[: m.start()] + head + body + tail + source[m.end():], stats


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


def main(argv: list[str]) -> int:
    check_only = "--check" in argv
    if not os.path.isdir(ASSETS):
        print("assets folder not found: %s" % ASSETS, file=sys.stderr)
        return 1

    files = content_files()
    totals: Counter = Counter()
    changed = skipped = 0

    for path in files:
        source = read_text(path)
        if MARKER in source:
            skipped += 1
            continue
        new_source, stats = enrich(source)
        if new_source == source:
            skipped += 1
            continue
        changed += 1
        totals.update(stats)
        if not check_only:
            io.open(path, "w", encoding="utf-8", newline="\n").write(new_source)

    verb = "would enrich" if check_only else "enriched"
    print("%d articles scanned" % len(files))
    print("  %s %d, already done %d\n" % (verb, changed, skipped))

    labels = {
        "code": "code panels",
        "table": "tables wrapped",
        "figure": "figures (local)",
        "figure_remote": "figures (remote)",
        "formula": "formula plaques",
        "step": "step paragraphs",
        "gm-note": "Note callouts",
        "gm-example": "Example callouts",
        "gm-key": "Key-point callouts",
        "gm-warn": "Warning callouts",
        "gm-syntax": "Syntax callouts",
        "gm-solution": "Solution callouts",
        "gm-definition": "Definition callouts",
        "gm-complexity": "Complexity chips",
        "lead": "section labels",
    }
    for key, label in labels.items():
        if totals.get(key):
            print("    %-22s %5d" % (label, totals[key]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
