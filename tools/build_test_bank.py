#!/usr/bin/env python3
"""
Converts the legacy assets/mock1.json into the new test schema and writes the
test catalogue.

The legacy format had one question type, no marks, no section, no solution and
a 1-based answer index that happened to line up with the RadioButton ids the
old engine assigned. The new schema is GATE-shaped: MCQ / MSQ / NAT, per-question
marks, sections, and a numeric answer range for NAT.

Questions that cannot be converted are reported and skipped rather than shipped
broken -- two of them store the answer under an "answer" key with no option
index, so there is no way to know which option is correct.

Run:  python tools/build_test_bank.py
"""

from __future__ import annotations

import io
import json
import os
import re
import sys

ASSETS = os.path.normpath(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets")
)
LEGACY = os.path.join(ASSETS, "mock1.json")
TESTS_DIR = os.path.join(ASSETS, "tests")
CATALOGUE = os.path.join(TESTS_DIR, "catalogue.json")

SCHEMA_VERSION = 1

TEST_ID = "aptitude-practice-1"
TEST_TITLE = "General Aptitude — Practice Test 1"
TEST_DESCRIPTION = (
    "Verbal, quantitative and logical reasoning questions in the GATE "
    "General Aptitude style."
)
# 57 one-mark MCQs. Kept honest: this is an aptitude practice set, not a
# reconstruction of a full 65-question GATE paper.
DURATION_MINUTES = 60
SECTION_ID = "ga"
SECTION_NAME = "General Aptitude"

OPTION_LABELS = ["A", "B", "C", "D", "E", "F"]

# The legacy file has two questions whose option arrays are placeholder
# copy-paste from a neighbouring question. They are dropped.
PLACEHOLDER_OPTIONS = {("1", "5", "25", "4")}


def read_json(path: str):
    raw = io.open(path, "rb").read()
    for enc in ("utf-8-sig", "utf-8", "cp1252"):
        try:
            return json.loads(raw.decode(enc))
        except (UnicodeDecodeError, json.JSONDecodeError):
            continue
    raise SystemExit("could not parse %s" % path)


# Options arrive as "(A) I only" or plain text; strip any leading label so the
# UI can render its own consistently.
LABEL_PREFIX = re.compile(r"^\s*[\(\[]?\s*([A-Fa-f])\s*[\)\].:]\s+")


def clean_option(text: str) -> str:
    return LABEL_PREFIX.sub("", text).strip()


def convert() -> tuple[list, list]:
    data = read_json(LEGACY)
    questions, skipped = [], []

    for raw in data.get("questions", []):
        number_text = str(raw.get("question_number", "")).strip()
        text = (raw.get("question") or "").strip()
        options = [str(o) for o in raw.get("options", [])]

        if "correct_option" not in raw:
            skipped.append((number_text, "no correct_option key"))
            continue
        if not text or len(options) < 2:
            skipped.append((number_text, "missing text or options"))
            continue
        if tuple(options) in PLACEHOLDER_OPTIONS:
            skipped.append((number_text, "placeholder option set"))
            continue

        # Legacy answers are 1-based.
        index = int(raw["correct_option"]) - 1
        if not 0 <= index < len(options):
            skipped.append((number_text, "answer index %d out of range" % (index + 1)))
            continue

        try:
            number = int(number_text)
        except ValueError:
            number = len(questions) + 1

        option_objs = []
        for i, opt in enumerate(options):
            option_objs.append({"id": OPTION_LABELS[i], "text": clean_option(opt)})

        questions.append({
            "id": "%s-q%02d" % (TEST_ID, number),
            "number": number,
            "type": "mcq",
            "marks": 1,
            "text": text,
            "options": option_objs,
            "correctOptionIds": [OPTION_LABELS[index]],
            "subjectId": "aptitude",
        })

    questions.sort(key=lambda q: q["number"])
    # Renumber so the paper reads 1..n with no gaps where questions were dropped.
    for i, q in enumerate(questions, start=1):
        q["number"] = i

    return questions, skipped


def main() -> int:
    if not os.path.isfile(LEGACY):
        print("legacy mock1.json not found at %s" % LEGACY, file=sys.stderr)
        return 1

    questions, skipped = convert()
    if not questions:
        print("nothing converted", file=sys.stderr)
        return 1

    os.makedirs(TESTS_DIR, exist_ok=True)

    test = {
        "id": TEST_ID,
        "title": TEST_TITLE,
        "description": TEST_DESCRIPTION,
        "durationMinutes": DURATION_MINUTES,
        "sections": [{
            "id": SECTION_ID,
            "name": SECTION_NAME,
            "questionIds": [q["id"] for q in questions],
        }],
        "questions": questions,
    }

    test_file = "%s.json" % TEST_ID
    with io.open(os.path.join(TESTS_DIR, test_file), "w", encoding="utf-8", newline="\n") as fh:
        json.dump(test, fh, ensure_ascii=False, indent=2)
        fh.write("\n")

    catalogue = {
        "schemaVersion": SCHEMA_VERSION,
        "tests": [{
            "id": TEST_ID,
            "title": TEST_TITLE,
            "description": TEST_DESCRIPTION,
            "durationMinutes": DURATION_MINUTES,
            "questionCount": len(questions),
            "totalMarks": sum(q["marks"] for q in questions),
            "file": "tests/%s" % test_file,
        }],
    }
    with io.open(CATALOGUE, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(catalogue, fh, ensure_ascii=False, indent=2)
        fh.write("\n")

    print("wrote tests/%s and tests/catalogue.json" % test_file)
    print("  %d questions, %d marks, %d minutes"
          % (len(questions), sum(q["marks"] for q in questions), DURATION_MINUTES))
    if skipped:
        print("  skipped %d unusable question(s):" % len(skipped))
        for number, why in skipped:
            print("    Q%-4s %s" % (number, why))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
