#!/usr/bin/env python3
"""
Finds articles whose prose has been through a synonym spinner.

Reading Paging on a phone turned up sentences like "used by the memory
controller to get admission to the reminiscence" and "the going for walks tool
keeps a web internet web page desk". That is not clumsy writing, it is
automated paraphrasing: a tool has replaced words with thesaurus entries
without regard for whether the term was a fixed technical phrase.

`memory` -> `reminiscence` and `page table` -> `web page desk` are not
judgement calls, so they can be detected exactly. Each entry below is a
substitution no author would make, paired with what the text should say.

Run:  python tools/audit_prose.py [--fix]
"""

from __future__ import annotations

import io
import os
import re
import sys
from collections import Counter

ROOT = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(ROOT, "..", "app", "src", "main", "assets"))

# (what the spinner produced, what it means). Ordered longest-first at use so
# that "web page desk" is matched before "web page".
SPUN = [
    ("reminiscence",            "memory"),
    ("web page desk",           "page table"),
    ("page desk",               "page table"),
    ("web internet web page",   "page"),
    ("internet web page",       "page"),
    ("going for walks",         "running"),
    ("get admission to",        "access"),
    ("deal with area",          "address space"),
    ("deal with space",         "address space"),
    ("deal with translation",   "address translation"),
    ("bodily deal with",        "physical address"),
    ("logical deal with",       "logical address"),
    ("bodily addresses",        "physical addresses"),
    ("bodily address",          "physical address"),
    ("bodily memory",           "physical memory"),
    ("bodily body",             "physical frame"),
    ("working gadget",          "operating system"),
    ("working device",          "operating system"),
    ("working machine",         "operating system"),
    ("steady-duration",         "fixed-length"),
    ("set web page length",     "fixed page size"),
    ("frame range",             "frame number"),
    ("manipulate bits",         "control bits"),
    ("reminiscence control",    "memory management"),
    ("outside fragmentation",   "external fragmentation"),
    ("predominant memory",      "main memory"),
    ("contains facts approximately", "contains information about"),
]

TAG_RE = re.compile(r"<[^>]+>")


def read_text(path: str) -> str:
    raw = io.open(path, "rb").read()
    for enc in ("utf-8-sig", "utf-8", "cp1252", "latin-1"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            continue
    return raw.decode("utf-8", errors="replace")


def main(argv: list[str]) -> int:
    fix = "--fix" in argv
    patterns = sorted(SPUN, key=lambda p: -len(p[0]))

    per_file: dict[str, Counter] = {}
    totals: Counter = Counter()
    files = 0

    for dirpath, _dirs, names in os.walk(ASSETS):
        for name in sorted(names):
            if not name.lower().endswith(".html"):
                continue
            path = os.path.join(dirpath, name)
            rel = os.path.relpath(path, ASSETS).replace("\\", "/")
            files += 1
            source = read_text(path)
            visible = TAG_RE.sub(" ", source)

            found: Counter = Counter()
            for spun, _meant in patterns:
                n = len(re.findall(r"\b%s\b" % re.escape(spun), visible, re.I))
                if n:
                    found[spun] = n
                    totals[spun] += n
            if found:
                per_file[rel] = found
                if fix:
                    fixed = source
                    for spun, meant in patterns:
                        fixed = re.sub(r"\b%s\b" % re.escape(spun), meant, fixed, flags=re.I)
                    if fixed != source:
                        io.open(path, "w", encoding="utf-8", newline="\n").write(fixed)

    print("Scanned %d HTML articles\n" % files)
    if not per_file:
        print("No spun phrasing found.")
        return 0

    print("%d article(s) contain machine-spun phrasing:\n" % len(per_file))
    for rel in sorted(per_file, key=lambda r: -sum(per_file[r].values())):
        hits = per_file[rel]
        print("  %-28s %3d hits  %s" % (rel, sum(hits.values()),
                                        ", ".join(sorted(hits))[:70]))

    print("\nMost common substitutions:")
    meant = dict(SPUN)
    for spun, n in totals.most_common(12):
        print("  %-32s -> %-24s x%d" % (spun, meant[spun], n))

    print("\n%d occurrences across %d files." % (sum(totals.values()), len(per_file)))
    if fix:
        print("Rewritten in place.")
    else:
        print("Re-run with --fix to substitute the intended terms.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
