#!/usr/bin/env python3
"""
Generates app/src/main/assets/content_index.json from whatever is in the
assets folder.

Replaces the old generate_json.ps1, which globbed *.html only. That is why
Previous Papers shipped with zero topics and why all 29 reference PDFs and
28 previous-year papers were unreachable: nothing ever referenced them.

What this does differently:
  * indexes PDFs as well as HTML
  * pairs each previous-year paper with its answer key
  * derives a readable topic title from the document's own heading instead of
    title-casing the filename ("krushkalminspanningtree" -> "Kruskal's
    Algorithm", not "Krushkalminspanningtree")
  * orders topics for study rather than alphabetically
  * records subject weightage so the UI can show what is worth revising

Run:  python tools/build_content_index.py
"""

from __future__ import annotations

import html
import io
import json
import os
import re
import sys
from datetime import datetime, timezone

ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets")
ASSETS = os.path.normpath(ASSETS)
OUT = os.path.join(ASSETS, "content_index.json")

SCHEMA_VERSION = 2

# --------------------------------------------------------------------------
# Subjects, in GATE CS paper order, with approximate mark weightage.
# `folder` is the assets directory; None means we have no long-form notes yet.
# --------------------------------------------------------------------------
SUBJECTS = [
    dict(id="aptitude", folder="aptitude", name="General Aptitude",     short="GA",    weight=15),
    dict(id="maths",    folder=None,       name="Engineering Mathematics", short="Math", weight=13),
    dict(id="ds",       folder="ds",       name="Programming & Data Structures", short="PDS", weight=12),
    dict(id="os",       folder="os",       name="Operating Systems",     short="OS",    weight=9),
    dict(id="algo",     folder="algo",     name="Algorithms",            short="Algo",  weight=8),
    dict(id="dbms",     folder="dbms",     name="Databases",             short="DBMS",  weight=8),
    dict(id="cao",      folder="cao",      name="Computer Organisation", short="COA",   weight=8),
    dict(id="toc",      folder="toc",      name="Theory of Computation", short="TOC",   weight=8),
    dict(id="cn",       folder=None,       name="Computer Networks",     short="CN",    weight=8),
    dict(id="dl",       folder="dl",       name="Digital Logic",         short="DL",    weight=6),
    dict(id="cd",       folder="cd",       name="Compiler Design",       short="CD",    weight=5),
]

# --------------------------------------------------------------------------
# Study order. Slugs listed here come first, in this order; anything in the
# folder that is not listed is appended alphabetically so new files never go
# missing just because someone forgot to update this table.
# --------------------------------------------------------------------------
STUDY_ORDER = {
    "algo": [
        "algorithms", "asymptotic", "search", "binearysearch", "sort",
        "bubblesort", "selectionsort", "insertionsort", "mergesort", "quicksort",
        "heapsort", "countsort", "radixsort", "bucketsort", "divide&conq",
        "greedyalg", "huffman", "spanningtree", "prims", "krushkalminspanningtree",
        "dijasthras", "bellmenford", "floydwarshall", "dynamicp", "matrixchain",
        "longestcommensub", "knapsack01", "subsetproblemsum",
    ],
    "ds": [
        "introtoC", "data types", "operators", "typecasting", "decision making",
        "loops", "functions", "recursion", "fibonachi", "pointers", "array",
        "strings", "struct", "ll", "sll", "stack", "infixtopostfix", "queue",
        "circularqueue", "priorityqueue", "trees", "heap", "graphs",
    ],
    "os": [
        "os_basic1", "types", "structure", "systemCalls", "process", "pcb",
        "processState", "contactSwitching", "multitasking", "cs", "preemptive",
        "fcfs", "sjf", "ljf", "lrtf", "rr", "priority", "hrrn", "mq",
        "ipc", "semaphores", "pcproblem", "rwproblem", "dpproblem",
        "dead", "resource", "detection", "bankers", "methods", "starvation",
        "memoryManagement", "swapping", "pageing", "segmentation",
        "pagereplacement", "vm", "swappingTrashing",
        "discmanagement", "discscheduling",
    ],
    "dbms": [
        "introtoDBMS", "independence", "12tire", "3tire", "introtoER",
        "recursiveRelationship", "miner", "keys", "relationalAlgebra",
        "sql", "typesofsql", "where", "joins", "objects",
        "functionaldependency", "normalization", "normalforms", "anomali",
        "acid", "recoverytech", "deadlockindbms", "starvation",
        "file", "hashing",
    ],
    "cao": [
        "basic_computer", "computerInstructions", "machineInstructions",
        "instructionformate", "addressingmodes", "instructioncycle",
        "Datamanupulation_pc", "pcinstruction", "datapath", "aludatapath",
        "hardwiredMicroprogrammed", "risccisc", "pipelining",
        "arithmeticinstructionpipeline", "datahazaed",
        "Memoryunit", "memorymapping", "cache",
        "iointerface", "ioprocessor", "dma",
    ],
    "dl": [
        "numbersystem", "12", "greycode", "binarytogray", "floatpoint",
        "boolean", "rep_boolean", "min_boolean", "conical", "kmap",
        "logicGates", "half", "full", "carry", "parallel",
        "encode", "multiplex", "demux",
        "introsequntial", "flipflop", "masterslave", "counters",
        "shiftregistors", "sync",
    ],
    "cd": [
        "into_cd", "phases", "lexical", "syntax", "parsing", "ambigious",
        "topdown", "slr", "clr", "lalr", "sematic", "s-l_attribute",
        "symbolic_table", "error", "intermediate_code_generator",
        "threeaddress", "basicblock", "codeoptimization", "loop", "peephole",
    ],
    "toc": [
        "intro_toc", "finiteAutomata", "mealyMoore", "grammer",
        "chomsky_hirarky", "application_of_autometa", "computable",
    ],
    "aptitude": [
        "quantitative", "numbers", "percentage", "profit", "simple_intrest",
        "progression", "speed", "work", "pipes", "clock", "probability", "2d",
        "puzzle", "Seating_Arrangement", "Order", "Inequality_Reasoning",
        "alphanumeric",
        "verbal", "grammer", "article", "Determiners", "Modifiers", "passive",
        "agreement", "verbal_analog", "paragraph",
    ],
}

# --------------------------------------------------------------------------
# Titles for the 15 documents with no usable heading, plus overrides where the
# heading in the file is wrong, truncated, or a typo.
# --------------------------------------------------------------------------
TITLE_OVERRIDES = {
    # no heading in the file at all
    "aptitude/2d": "Data Interpretation",
    "aptitude/Modifiers": "Modifiers",
    "aptitude/Order": "Ranking & Order",
    "aptitude/Seating_Arrangement": "Seating Arrangement",
    "aptitude/agreement": "Subject-Verb Agreement",
    "aptitude/article": "Articles",
    "aptitude/grammer": "Grammar Basics",
    "aptitude/paragraph": "Reading Comprehension",
    "aptitude/percentage": "Percentages",
    "aptitude/profit": "Profit & Loss",
    "aptitude/quantitative": "Quantitative Aptitude",
    "aptitude/verbal": "Verbal Ability",
    "os/semaphores": "Semaphores",
    "shortnotes/algorithms": "Algorithms",
    # heading present but wrong or unhelpful
    "shortnotes/cd": "Compiler Design",
    "algo/algorithms": "Introduction to Algorithms",
    "dbms/12tire": "One-Tier & Two-Tier Architecture",
    "dbms/3tire": "Three-Tier Architecture",
    "dl/12": "1's & 2's Complement",
    "ds/data types": "Data Types in C",
    "ds/decision making": "Decision Making in C",
    # source headings that run far too long for a list row
    "os/sjf": "Shortest Job First (SJF)",
    "os/lrtf": "Longest Remaining Time First (LRTF)",
    "os/priority": "Priority Scheduling",
    "cao/cache": "Cache Memory & Locality of Reference",
    "cd/s-l_attribute": "S- and L-Attributed SDTs",
}

# --------------------------------------------------------------------------
# The 29 reference PDFs, mapped to the subject they belong to. Six of these are
# Engineering Mathematics, which the app previously advertised with no content
# behind it at all.
# --------------------------------------------------------------------------
PDF_SUBJECT = {
    "maths": [
        ("calculus.pdf", "Calculus"),
        ("linear algebra.pdf", "Linear Algebra"),
        ("differentialeqyation.pdf", "Differential Equations"),
        ("partial differential equations.pdf", "Partial Differential Equations"),
        ("probability and statistics.pdf", "Probability & Statistics"),
        ("numerical methods.pdf", "Numerical Methods"),
    ],
    "dl": [
        ("number systems.pdf", "Number Systems"),
        ("Boolean Algebra and minimisation of functions of cse.pdf", "Boolean Algebra & Minimisation"),
        ("combinational circuits.pdf", "Combinational Circuits"),
        ("sequential circuits .pdf", "Sequential Circuits"),
    ],
    "cao": [
        ("Machine instructions, addressing modes.pdf", "Machine Instructions & Addressing Modes"),
        ("ALU and data path CPU control design .pdf", "ALU, Data Path & Control Design"),
        ("instruction pipelining .pdf", "Instruction Pipelining"),
        ("cache and main memory secondary storage.pdf", "Cache, Main Memory & Secondary Storage"),
        ("memory interface I_o interface.pdf", "Memory & I/O Interface"),
    ],
    "ds": [
        ("programming in C.pdf", "Programming in C"),
        ("arrays pointers and structures.pdf", "Arrays, Pointers & Structures"),
        ("linked lists and stacked queues .pdf", "Linked Lists, Stacks & Queues"),
        ("trees.pdf", "Trees"),
    ],
    "algo": [
        ("asymptotic anyalsis.pdf", "Asymptotic Analysis"),
        ("sorting algorithm .pdf", "Sorting Algorithms"),
        ("divide and conquer.pdf", "Divide & Conquer"),
        ("greedy approch.pdf", "Greedy Approach"),
        ("dynamicProgramming.pdf", "Dynamic Programming"),
    ],
    "dbms": [
        ("ermodels.pdf", "ER Models"),
        ("sql.pdf", "SQL"),
        ("normalization.pdf", "Normalization"),
        ("transactionandconcorency.pdf", "Transactions & Concurrency"),
        ("filemanagement.pdf", "File Organization"),
    ],
}


# --------------------------------------------------------------------------

def read_text(path: str) -> str:
    """Assets are a mix of UTF-8 and cp1252. Try UTF-8, then fall back."""
    raw = io.open(path, "rb").read()
    for enc in ("utf-8-sig", "utf-8", "cp1252", "latin-1"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            continue
    return raw.decode("utf-8", errors="replace")


HEADING_RE = [re.compile(r"<%s[^>]*>(.*?)</%s>" % (t, t), re.S | re.I) for t in ("h1", "h2", "h3")]
TAG_RE = re.compile(r"<[^>]+>")


# Words that must not be title-cased when rescuing an ALL-CAPS heading.
ACRONYMS = {
    "SQL", "DBMS", "ER", "OS", "CPU", "ALU", "DMA", "IO", "I/O", "RISC", "CISC",
    "FCFS", "SJF", "LRU", "FIFO", "LIFO", "NP", "DFA", "NFA", "CFG", "BNF",
    "LR", "LL", "SLR", "CLR", "LALR", "AVL", "BST", "TCP", "IP", "GATE", "ACID",
}


def smart_case(text: str) -> str:
    """Rescue an ALL-CAPS heading without flattening genuine acronyms."""
    letters = [c for c in text if c.isalpha()]
    if not letters or not all(c.isupper() for c in letters):
        return text
    out = []
    for word in text.split():
        bare = word.strip("()[],.:;'\"")
        out.append(word if bare in ACRONYMS else word.capitalize())
    return " ".join(out)


def shorten(text: str) -> str:
    """A trailing gloss reads as noise in a list row."""
    stripped = re.sub(r"\s*\([^)]*\)\s*$", "", text).strip()
    if len(text) > 46 and len(stripped) >= 12:
        return stripped
    return text


def clean_title(text: str) -> str:
    text = TAG_RE.sub("", text)
    text = html.unescape(text)
    text = text.replace(" ", " ")
    text = " ".join(text.split())
    # "SQL Commands | DDL, DQL, ..." -> "SQL Commands"
    text = text.split("|")[0].strip()
    # These articles were written for a general audience; the suffix is noise
    # once the reader is already inside the Databases section.
    for suffix in (" in DBMS", " in Operating System", " in Compiler Design",
                   " in Computer Networks", " in Data Structure"):
        if text.endswith(suffix):
            text = text[: -len(suffix)]
    text = re.sub(r"^(Introduction of|Introduction to)\s+", "Introduction to ", text)
    text = shorten(smart_case(text))
    return text.strip(" -–—:")


def prettify(slug: str) -> str:
    s = slug.replace("_", " ").replace("&", " and ").replace("-", " ")
    s = re.sub(r"(?<=[a-z])(?=[A-Z])", " ", s)
    return " ".join(w.capitalize() for w in s.split())


def title_for(rel_key: str, path: str) -> str:
    if rel_key in TITLE_OVERRIDES:
        return TITLE_OVERRIDES[rel_key]
    src = read_text(path)
    for rx in HEADING_RE:
        m = rx.search(src)
        if m:
            t = clean_title(m.group(1))
            if t:
                return t
    return prettify(os.path.basename(rel_key))


def slug_id(subject_id: str, slug: str) -> str:
    s = re.sub(r"[^a-z0-9]+", "_", slug.lower()).strip("_")
    return "%s_%s" % (subject_id, s)


def build_topics(subject: dict) -> list:
    folder = subject["folder"]
    if not folder:
        return []
    d = os.path.join(ASSETS, folder)
    if not os.path.isdir(d):
        return []

    slugs = [f[:-5] for f in sorted(os.listdir(d)) if f.lower().endswith(".html")]
    order = STUDY_ORDER.get(subject["id"], [])
    ranked = [s for s in order if s in slugs]
    ranked += sorted(s for s in slugs if s not in order)

    topics = []
    for i, slug in enumerate(ranked):
        rel = "%s/%s" % (folder, slug)
        topics.append({
            "id": slug_id(subject["id"], slug),
            "title": title_for(rel, os.path.join(d, slug + ".html")),
            "order": i,
            "content": {"type": "html", "path": rel + ".html"},
        })
    return topics


def build_pdf_notes(subject_id: str) -> list:
    out = []
    for i, (fname, title) in enumerate(PDF_SUBJECT.get(subject_id, [])):
        path = os.path.join(ASSETS, "pdfs", fname)
        if not os.path.isfile(path):
            print("  ! missing PDF: %s" % fname, file=sys.stderr)
            continue
        out.append({
            "id": slug_id(subject_id, "pdf_" + os.path.splitext(fname)[0]),
            "title": title,
            "order": i,
            "sizeBytes": os.path.getsize(path),
            "content": {"type": "pdf", "path": "pdfs/" + fname},
        })
    return out


def build_short_notes(subject_id: str) -> dict | None:
    # shortnotes filenames do not match subject ids one-to-one.
    alias = {"ds": "dataStructure", "maths": "math", "algo": "algorithms"}
    name = alias.get(subject_id, subject_id)
    rel = "shortnotes/%s.html" % name
    if os.path.isfile(os.path.join(ASSETS, rel)):
        return {"type": "html", "path": rel}
    return None


PAPER_RE = re.compile(r"^(\d{4})(key)?(_compressed)?\.pdf$", re.I)


def build_papers() -> list:
    d = os.path.join(ASSETS, "previousPapers")
    if not os.path.isdir(d):
        return []

    papers, keys = {}, {}
    for f in sorted(os.listdir(d)):
        m = PAPER_RE.match(f)
        if not m:
            print("  ! unrecognised paper file: %s" % f, file=sys.stderr)
            continue
        year, is_key = int(m.group(1)), bool(m.group(2))
        (keys if is_key else papers)[year] = f

    out = []
    for year in sorted(papers, reverse=True):
        paper = papers[year]
        key = keys.get(year)
        out.append({
            "id": "gate_cs_%d" % year,
            "year": year,
            "title": "GATE %d — Computer Science" % year,
            "paper": {"type": "pdf", "path": "previousPapers/" + paper},
            "answerKey": {"type": "pdf", "path": "previousPapers/" + key} if key else None,
            "sizeBytes": os.path.getsize(os.path.join(d, paper)),
        })
    return out


def main() -> int:
    if not os.path.isdir(ASSETS):
        print("assets folder not found: %s" % ASSETS, file=sys.stderr)
        return 1

    subjects = []
    for s in SUBJECTS:
        topics = build_topics(s)
        pdf_notes = build_pdf_notes(s["id"])
        subjects.append({
            "id": s["id"],
            "name": s["name"],
            "shortName": s["short"],
            "weightage": s["weight"],
            "order": len(subjects),
            "topics": topics,
            "referenceNotes": pdf_notes,
            "shortNotes": build_short_notes(s["id"]),
        })
        print("  %-9s %3d topics  %2d pdfs  shortNotes=%s"
              % (s["id"], len(topics), len(pdf_notes),
                 "yes" if subjects[-1]["shortNotes"] else "no"))

    papers = build_papers()

    doc = {
        "schemaVersion": SCHEMA_VERSION,
        "generatedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "subjects": subjects,
        "papers": papers,
    }

    with io.open(OUT, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(doc, fh, ensure_ascii=False, indent=2)
        fh.write("\n")

    n_topics = sum(len(s["topics"]) for s in subjects)
    n_pdfs = sum(len(s["referenceNotes"]) for s in subjects)
    n_keys = sum(1 for p in papers if p["answerKey"])
    print("\nwrote %s" % os.path.relpath(OUT, os.getcwd()))
    print("  %d subjects, %d topics, %d reference PDFs" % (len(subjects), n_topics, n_pdfs))
    print("  %d previous-year papers (%d with answer keys)" % (len(papers), n_keys))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
