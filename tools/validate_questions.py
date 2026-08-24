#!/usr/bin/env python3
"""
Checks the question banks before they ship.

Two wrong answers have already reached a bank and been caught by re-deriving
them by hand, so everything mechanical is checked here instead: that every
question has the fields the player reads, that MCQs have exactly one correct
option, that every topicId names a topic that actually exists, and that no
question is duplicated.

It also reports depth, because a bank spread one question per topic gives the
subject practice something to draw on but never produces a per-topic set.

Run:  python tools/validate_questions.py
"""

from __future__ import annotations

import io
import json
import os
import sys
from collections import Counter, defaultdict

ROOT = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(ROOT, "..", "app", "src", "main", "assets"))
BANKS = os.path.join(ASSETS, "questions")

# Mirrors TestRepository.MIN_TOPIC_QUESTIONS; below this a topic gets no set.
MIN_TOPIC_QUESTIONS = 3
VALID_TYPES = {"mcq", "msq", "nat"}
VALID_MARKS = {1, 2}
VALID_DIFFICULTY = {"easy", "medium", "hard"}


def load(path: str):
    return json.load(io.open(path, encoding="utf-8"))


def known_topic_ids() -> set[str]:
    index = load(os.path.join(ASSETS, "content_index.json"))
    ids = set()
    for branch in index["branches"]:
        for subject in branch["subjects"]:
            for topic in subject.get("topics", []):
                ids.add(topic["id"])
    return ids


def check_question(q, seen_ids, seen_text, topics, problems, bank):
    qid = q.get("id", "<no id>")
    where = "%s/%s" % (bank, qid)

    for field in ("id", "topicId", "type", "marks", "text", "solution"):
        if not q.get(field):
            problems.append("%s: missing '%s'" % (where, field))

    if qid in seen_ids:
        problems.append("%s: duplicate question id" % where)
    seen_ids.add(qid)

    text = " ".join(str(q.get("text", "")).split()).lower()
    if text and text in seen_text:
        problems.append("%s: same wording as %s" % (where, seen_text[text]))
    elif text:
        seen_text[text] = where

    kind = q.get("type")
    if kind not in VALID_TYPES:
        problems.append("%s: type %r is not one of %s" % (where, kind, sorted(VALID_TYPES)))
    if q.get("marks") not in VALID_MARKS:
        problems.append("%s: marks %r should be 1 or 2" % (where, q.get("marks")))
    if q.get("difficulty") not in VALID_DIFFICULTY:
        problems.append("%s: difficulty %r unexpected" % (where, q.get("difficulty")))

    topic = q.get("topicId")
    if topic and topic not in topics:
        problems.append("%s: topicId %r matches no topic in content_index" % (where, topic))

    options = q.get("options") or []
    correct = q.get("correctOptionIds") or []

    if kind == "nat":
        if options:
            problems.append("%s: a NAT question should not carry options" % where)
        answer = q.get("numericAnswer") or {}
        if "min" not in answer or "max" not in answer:
            problems.append("%s: NAT needs numericAnswer.min and .max" % where)
        elif answer["min"] > answer["max"]:
            problems.append("%s: numericAnswer min > max" % where)
    else:
        if len(options) < 2:
            problems.append("%s: needs at least two options" % where)
        ids = [o.get("id") for o in options]
        if len(set(ids)) != len(ids):
            problems.append("%s: option ids repeat" % where)
        for c in correct:
            if c not in ids:
                problems.append("%s: correct option %r is not among the options" % (where, c))
        if kind == "mcq" and len(correct) != 1:
            problems.append("%s: an MCQ needs exactly one correct option, found %d"
                            % (where, len(correct)))
        if kind == "msq" and not correct:
            problems.append("%s: an MSQ needs at least one correct option" % where)
        if kind == "msq" and len(correct) == 1:
            problems.append("%s: MSQ with a single answer should be an MCQ" % where)


def main() -> int:
    topics = known_topic_ids()
    registry = load(os.path.join(BANKS, "index.json"))
    problems: list[str] = []
    seen_ids: set[str] = set()
    seen_text: dict[str, str] = {}
    depth: dict[str, Counter] = defaultdict(Counter)
    totals = Counter()

    for subject, rel in sorted(registry["banks"].items()):
        path = os.path.join(ASSETS, rel.replace("/", os.sep))
        if not os.path.isfile(path):
            problems.append("registry points at a missing bank: %s" % rel)
            continue
        bank = load(path)
        if bank.get("subjectId") != subject:
            problems.append("%s: subjectId is %r but registered as %r"
                            % (rel, bank.get("subjectId"), subject))
        for q in bank.get("questions", []):
            check_question(q, seen_ids, seen_text, topics, problems, subject)
            depth[subject][q.get("topicId")] += 1
            totals[subject] += 1

    print("%d questions across %d subjects\n" % (sum(totals.values()), len(totals)))
    grand_deep = 0
    for subject in sorted(totals):
        counts = depth[subject]
        deep = [t for t, n in counts.items() if n >= MIN_TOPIC_QUESTIONS]
        grand_deep += len(deep)
        print("  %-6s %3d questions  %2d topics  %2d with their own set"
              % (subject, totals[subject], len(counts), len(deep)))

    print("\n%d topic(s) have at least %d questions, so they get a per-topic set."
          % (grand_deep, MIN_TOPIC_QUESTIONS))

    if problems:
        print("\n%d problem(s):" % len(problems))
        for p in problems:
            print("  ! %s" % p)
        return 1
    print("\nNo problems found.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
