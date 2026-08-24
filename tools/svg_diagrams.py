#!/usr/bin/env python3
"""
Replaces hotlinked diagram images with inline SVG that we own.

899 of the 918 images in the bundled notes are hotlinks to external CDNs. That
means diagrams need an internet connection, break whenever those URLs change,
and are somebody else's images. Rehosting them would be a clearer infringement
than hotlinking, and bundling them would add tens of megabytes for the same
legal exposure.

Inline SVG solves all three at once: a few KB each, embedded in the article, no
hosting, no copyright question, works offline — and because it uses
`currentColor` it follows the reader's light/dark palette instead of being a
white screenshot on a dark page.

Diagrams live in tools/diagrams/<name>.svg. The table below says which figure
in which article each one replaces, matched on a distinctive fragment of the
image URL so the mapping survives reformatting.

Run:  python tools/svg_diagrams.py [--check]
"""

from __future__ import annotations

import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(ROOT, "..", "app", "src", "main", "assets"))
DIAGRAMS = os.path.join(ROOT, "diagrams")

MARKER = 'data-gm-svg="1"'

# (article, url fragment identifying the <figure>, diagram file, caption)
REPLACEMENTS = [
    ("ds/stack.html", "/Stack-", "stack-operations.svg",
     "Push and pop act on the same end, so the last item in is the first out"),
    ("ds/queue.html", "Queue-Data-Structures", "queue-operations.svg",
     "Items enter at the rear and leave from the front"),
    ("algo/bubblesort.html", "/1.webp", "bubble-sort-pass1.svg",
     "First pass: each comparison walks 6 one place right, until it reaches the end"),
    ("algo/bubblesort.html", "/2.webp", "bubble-sort-pass2.svg",
     "Second pass: nothing swaps, and 5 settles into place"),
    ("algo/bubblesort.html", "/3.webp", "bubble-sort-pass3.svg",
     "Third pass: the last two are already ordered, so the array is sorted"),
    ("algo/binearysearch.html", "mid-in-binary-search", "binary-search.svg",
     "Each comparison discards half of the remaining range"),
    ("os/processstate.html", "states_modified", "process-states.svg",
     "The core cycle. The two suspended states described below are just Ready and Waiting with the process swapped out to secondary memory"),
    ("os/pageing.html", "/paging.webp", "paging-translation.svg",
     "The page number indexes the page table; the offset passes straight through"),
]

# The source articles carry their own caption under each image. Once a figure
# becomes one of our diagrams it has a <figcaption> of its own, and leaving the
# old paragraph in place prints the caption twice.
STALE_CAPTION_RE = re.compile(
    r'(</figure>)\s*<p class="wp-caption-text">.*?</p>', re.S | re.I)

FIGURE_RE = re.compile(
    r'<figure class="gm-figure gm-remote">\s*<img\b[^>]*>\s*(?:<figcaption>.*?</figcaption>)?\s*</figure>',
    re.S | re.I,
)


def read_text(path: str) -> str:
    raw = io.open(path, "rb").read()
    for enc in ("utf-8-sig", "utf-8", "cp1252", "latin-1"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            continue
    return raw.decode("utf-8", errors="replace")


def build_figure(svg: str, caption: str) -> str:
    """Wraps a diagram so it matches the reader's existing figure styling."""
    return (
        '<figure class="gm-figure gm-svg" %s>\n%s\n<figcaption>%s</figcaption>\n</figure>'
        % (MARKER, svg.strip(), caption)
    )


def main(argv: list[str]) -> int:
    check_only = "--check" in argv
    replaced = missing = skipped = 0

    for article, fragment, diagram, caption in REPLACEMENTS:
        article_path = os.path.join(ASSETS, article)
        diagram_path = os.path.join(DIAGRAMS, diagram)

        if not os.path.isfile(article_path):
            print("  ! no article %s" % article, file=sys.stderr)
            missing += 1
            continue
        if not os.path.isfile(diagram_path):
            print("  ! no diagram %s" % diagram, file=sys.stderr)
            missing += 1
            continue

        source = read_text(article_path)
        svg = read_text(diagram_path)

        # Find the first remote figure whose <img> mentions the fragment.
        target = None
        for match in FIGURE_RE.finditer(source):
            if fragment.lower() in match.group(0).lower():
                target = match
                break

        if target is None:
            # Already replaced, or the figure moved.
            if MARKER in source:
                skipped += 1
            else:
                print("  ! no figure matching '%s' in %s" % (fragment, article))
                missing += 1
            continue

        new_source = (
            source[: target.start()]
            + build_figure(svg, caption)
            + source[target.end():]
        )
        replaced += 1
        if not check_only:
            io.open(article_path, "w", encoding="utf-8", newline="\n").write(new_source)
        print("  %-26s <- %s" % (article, diagram))

    # Second pass: drop captions orphaned by a swap, in whichever articles the
    # table touches. Runs every time, so it also cleans up after earlier runs.
    dropped = 0
    for article in sorted({row[0] for row in REPLACEMENTS}):
        path = os.path.join(ASSETS, article)
        if not os.path.isfile(path):
            continue
        source = read_text(path)
        cleaned = source
        # Only captions attached to a figure of ours; remote figures keep theirs.
        for match in list(STALE_CAPTION_RE.finditer(source)):
            figure_start = source.rfind("<figure", 0, match.start())
            if figure_start != -1 and MARKER in source[figure_start:match.start()]:
                cleaned = cleaned.replace(match.group(0), match.group(1), 1)
                dropped += 1
        if cleaned != source and not check_only:
            io.open(path, "w", encoding="utf-8", newline="\n").write(cleaned)

    if dropped:
        print("  dropped %d caption(s) the diagrams made redundant" % dropped)

    verb = "would replace" if check_only else "replaced"
    print("\n%s %d figure(s); %d already done, %d unmatched"
          % (verb, replaced, skipped, missing))
    return 0 if missing == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
