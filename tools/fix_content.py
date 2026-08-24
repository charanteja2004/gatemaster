#!/usr/bin/env python3
"""
Applies the mechanical content fixes that tools/audit_content.py surfaces.

Everything here is an explicit, reviewed rewrite. Nothing is fuzzy-matched at
run time: a close-string match would happily rewrite a link to Strassen's
algorithm into a link to Dijkstra's, which is worse than leaving it broken.

Fixes applied:
  * misspelled internal links, from the table below
  * <img> tags pointing at absolute paths on the original author's PC, and one
    with a malformed URL -- both are removed, since neither can ever resolve
  * leftover "/#section" navigation anchors from the source site, unwrapped to
    plain text
  * a top-level <h1> for articles that have no heading at all, so the reader
    is not staring at an untitled wall of text

Run:  python tools/fix_content.py [--check]
"""

from __future__ import annotations

import io
import os
import re
import sys

ASSETS = os.path.normpath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets")
)

# (file, wrong target, correct target). Each verified against the real filename.
LINK_FIXES = [
    ("dbms/3tire.html", "intotoDBMS.html", "introtoDBMS.html"),
    ("dbms/anomali.html", "intotloDBMS.html", "introtoDBMS.html"),
    ("dbms/relationalAlgebra.html", "join.html", "joins.html"),
    ("dl/flipflop.html", "shiftregisters.html", "shiftregistors.html"),
    ("dl/flipflop.html", "introsequential.html", "introsequntial.html"),
    ("dl/greycode.html", "binaryconverstion.html", "binarytogray.html"),
    ("dl/multiplex.html", "logicGats.html", "logicGates.html"),
    ("ds/struct.html", "data type.html", "data types.html"),
    ("ds/typecasting.html", "datatype.html", "data types.html"),
    ("ds/stack.html", "linkedlist.html", "ll.html"),
    # These now resolve because the articles they wanted are being written.
    ("algo/algorithms.html", "spaceComplexity.html", "spacecomplexity.html"),
    ("algo/divide&conq.html", "strassen.html", "strassen.html"),
    ("ds/graphs.html", "bfs.html", "bfs.html"),
    ("ds/graphs.html", "dfs.html", "dfs.html"),
    ("ds/ll.html", "dll.html", "dll.html"),
    ("ds/functions.html", "variable.html", "variables.html"),
]

# <img> tags that can never resolve: absolute paths on someone else's machine,
# and one src wrapped in angle brackets.
DROP_IMAGE_PATTERNS = [
    re.compile(r'<img[^>]+src\s*=\s*["\'][A-Za-z]:\\[^"\']*["\'][^>]*>', re.I),
    re.compile(r'<img[^>]+src\s*=\s*["\']\s*<[^"\']*["\'][^>]*>', re.I),
]

# "/#some-section" anchors left over from the source site's in-page navigation.
STRAY_ANCHOR = re.compile(r'<a[^>]+href\s*=\s*["\']/#[^"\']*["\'][^>]*>(.*?)</a>', re.S | re.I)

# Empty paragraphs and stacked line breaks leave big dead gaps mid-article.
EMPTY_PARA = re.compile(r"<p[^>]*>(?:\s|&nbsp;|<br\s*/?>)*</p>", re.I)
BR_RUN = re.compile(r"(?:\s*<br\s*/?>\s*){3,}", re.I)

# Titles for the articles that carry no heading of their own.
MISSING_HEADINGS = {
    "aptitude/2d.html": "Data Interpretation",
    "aptitude/Modifiers.html": "Modifiers",
    "aptitude/Order.html": "Ranking &amp; Order",
    "aptitude/Seating_Arrangement.html": "Seating Arrangement",
    "aptitude/agreement.html": "Subject-Verb Agreement",
    "aptitude/article.html": "Articles",
    "aptitude/grammer.html": "Grammar Basics",
    "aptitude/paragraph.html": "Reading Comprehension",
    "aptitude/percentage.html": "Percentages",
    "aptitude/profit.html": "Profit &amp; Loss",
    "aptitude/quantitative.html": "Quantitative Aptitude",
    "aptitude/verbal.html": "Verbal Ability",
    "os/semaphores.html": "Semaphores",
    "shortnotes/algorithms.html": "Algorithms — Short Notes",
}

BODY_OPEN = re.compile(r"<body[^>]*>", re.I)
HEADING_RE = re.compile(r"<h[1-3][^>]*>", re.I)


def read_text(path: str) -> str:
    raw = io.open(path, "rb").read()
    for enc in ("utf-8-sig", "utf-8", "cp1252", "latin-1"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            continue
    return raw.decode("utf-8", errors="replace")


def write_text(path: str, text: str) -> None:
    io.open(path, "w", encoding="utf-8", newline="\n").write(text)


def main(argv: list[str]) -> int:
    check_only = "--check" in argv
    changed: dict[str, list[str]] = {}

    def note(rel: str, what: str) -> None:
        changed.setdefault(rel, []).append(what)

    # 1. link typos
    for rel, wrong, right in LINK_FIXES:
        path = os.path.join(ASSETS, rel)
        if not os.path.isfile(path):
            print("  ! missing file: %s" % rel, file=sys.stderr)
            continue
        src = read_text(path)
        pattern = re.compile(
            r'(href\s*=\s*["\'])%s(["\'])' % re.escape(wrong), re.I
        )
        new, n = pattern.subn(r"\g<1>%s\g<2>" % right, src)
        if n:
            note(rel, "link %s -> %s (x%d)" % (wrong, right, n))
            if not check_only:
                write_text(path, new)

    # 2 & 3. unresolvable images and stray anchors, across every article
    for folder in sorted(os.listdir(ASSETS)):
        d = os.path.join(ASSETS, folder)
        if not os.path.isdir(d) or folder in ("pdfs", "previousPapers", "tests"):
            continue
        for name in sorted(os.listdir(d)):
            if not name.lower().endswith(".html"):
                continue
            rel = "%s/%s" % (folder, name)
            path = os.path.join(d, name)
            src = read_text(path)
            new = src

            for pattern in DROP_IMAGE_PATTERNS:
                new, n = pattern.subn("", new)
                if n:
                    note(rel, "removed %d unresolvable <img>" % n)

            new, n = STRAY_ANCHOR.subn(r"\1", new)
            if n:
                note(rel, "unwrapped %d stray /#anchor link(s)" % n)

            new, n = EMPTY_PARA.subn("", new)
            if n:
                note(rel, "removed %d empty paragraph(s)" % n)

            new, n = BR_RUN.subn("<br><br>", new)
            if n:
                note(rel, "collapsed %d run(s) of line breaks" % n)

            if new != src and not check_only:
                write_text(path, new)

    # 4. missing headings
    for rel, title in MISSING_HEADINGS.items():
        path = os.path.join(ASSETS, rel)
        if not os.path.isfile(path):
            continue
        src = read_text(path)
        if HEADING_RE.search(src):
            continue
        m = BODY_OPEN.search(src)
        heading = "\n<h1>%s</h1>\n" % title
        new = (src[: m.end()] + heading + src[m.end():]) if m else heading + src
        note(rel, "added <h1>%s</h1>" % title)
        if not check_only:
            write_text(path, new)

    verb = "would change" if check_only else "changed"
    print("%s %d file(s)\n" % (verb, len(changed)))
    for rel in sorted(changed):
        print("  %s" % rel)
        for what in changed[rel]:
            print("      %s" % what)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
