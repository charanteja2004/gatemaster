# GateMaster

GATE preparation app covering **all 30 GATE 2026 papers** — notes,
previous-year papers, and timed mock tests with GATE's real marking scheme.

Being rebuilt from the ground up in Kotlin + Jetpack Compose. The pre-rewrite
Java/XML app is preserved at `legacy/` and is **not** part of the build.

---

## Requirements

| | |
|---|---|
| JDK | 17+ (Android Studio's bundled JBR 21 is what this is built with) |
| Android Studio | any version shipping AGP 9.3+ support |
| SDK platforms | 37 (compile), 36 (target) — Gradle installs both on first build |

From the command line:

```sh
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Toolchain notes

These are the non-obvious constraints. Change them only deliberately.

- **AGP 9 has built-in Kotlin.** Applying `org.jetbrains.kotlin.android` is a
  hard error. The Compose and serialization compiler plugins are still applied
  normally, and KGP is pinned on the buildscript classpath in the root
  `build.gradle.kts` so it matches the version those plugins expect.
- **`jvmTarget` lives inside `android { kotlin { compilerOptions { … } } }`**,
  not a top-level `kotlin {}` block.
- **Kotlin is held at 2.3.x**, not 2.4, because KSP — which Room and Hilt both
  need — has no 2.4 release yet.
- **`compileSdk` is 37, `targetSdk` is 36.** Compose 1.12 (BOM 2026.08.00)
  refuses to compile against anything below 37; 36 is what Google Play requires
  and what the app is actually tested against.
- **`minSdk` is 24.** The old app used 31, which excluded every device on
  Android 11 or older for no technical reason.

## Content pipeline

All study material lives in `app/src/main/assets/` and is described by a single
generated file, `assets/content_index.json`. Nothing about content is hardcoded
in Kotlin.

To add or change material: drop files into the right assets folder, then

```sh
python tools/build_content_index.py      # rebuild the index
python tools/normalize_content_html.py   # make new HTML phone-readable
python tools/fix_content.py              # mechanical defect fixes
python tools/enrich_content.py           # add the reader's semantic hooks
python tools/build_test_bank.py          # rebuild the mock-test question bank
./gradlew :app:testDebugUnitTest         # verify nothing is orphaned
```

### `tools/build_content_index.py`

Scans the assets folders and emits `content_index.json`: subjects, topics,
per-subject reference PDFs, short notes, and previous-year papers paired with
their answer keys. Topic titles come from each document's own heading rather
than its filename, with an override table for the handful that have no usable
heading.

Replaces the old `generate_json.ps1`, which globbed `*.html` only — which is
why Previous Papers shipped with zero topics and 22 MB of PDFs were unreachable.

### `tools/normalize_content_html.py`

Injects `<meta charset>`, `<meta name="viewport">`, and a link to the shared
`assets/reader.css` into every bundled article, so they render at phone width
and follow the system dark theme. Idempotent — safe to re-run. Pass `--check`
to report without writing.

`assets/reader.css` is loaded last so it overrides the per-file `<style>`
blocks by cascade order rather than by deleting anything.

### `tools/audit_content.py`

Finds the defects in the bundled notes that can be caught mechanically: links
that go nowhere, images that were never copied across, articles that stop
mid-sentence, placeholder text, duplicated bodies, and documents with no
heading. It does **not** verify that the physics is right — that needs a human
who knows the subject.

### `tools/fix_content.py`

Applies the fixes the audit surfaces. Every rewrite is from an explicit,
reviewed table rather than fuzzy matching at run time — a close-string match
would happily rewrite a link to Strassen's algorithm into a link to Dijkstra's.

### `tools/syllabus.py`

Branch, subject and syllabus data for all 30 GATE 2026 papers. Paper names and
codes are the official list from gate2026.iitg.ac.in. Eight papers (CS, ME, EE,
EC, CE, CH, IN, DA) carry a full subject breakdown with syllabus bullets and
mark weightage; the remaining 22 carry their section names, and the app says so
rather than pretending otherwise.

### `tools/enrich_content.py`

Adds the semantic structure `reader.css` styles. The source articles are
undifferentiated prose — a note, a worked example, a formula and a complexity
result all look identical, which is what makes them tiring to read on a phone.
This pass finds them by the conventions the authors already used and tags them:

- runs of SyntaxHighlighter `.line` divs become a single code panel
- `<table>` gets a scroll container so wide tables stop stretching the page
- `<img>` becomes a `<figure>` with its alt text as a caption, marked when the
  source is remote
- lead-in paragraphs become callouts (Note, Example, Syntax, Solution,
  Definition, Time/Space Complexity, Step N)
- short relational lines become formula plaques

A bare "Example:" introducing a list becomes a compact section label rather than
a box, since framing a lone word just repeats it. The formula detector is
deliberately conservative: a paragraph wrongly promoted to a plaque looks worse
than one left as plain prose.

Idempotent and marker-guarded.

### `tools/build_test_bank.py`

Converts the legacy `assets/mock1.json` into `assets/tests/` — the GATE-shaped
schema with MCQ / MSQ / NAT, per-question marks and sections — and writes
`assets/tests/catalogue.json`. Questions that cannot be converted are reported
and skipped rather than shipped broken.

## Reading progress and bookmarks

`StudyProgressRepository` remembers what has been opened, how far it was read,
and what was saved. It backs the Continue-reading card on home, the read ticks
and "N of M read" counts in the subject lists, and the bookmark action in the
reader.

Two rules worth knowing:

- **Furthest-read only moves forward.** Scrolling back up does not un-read what
  has been read.
- **Reaching the last tenth counts as read**, and an article that fits on one
  screen counts as read once it is shown. Requiring a full scroll to 100% would
  leave every short topic permanently unread, because there is nothing to
  scroll to.

Stored as one JSON document in the app's files directory rather than Room: it is
small, read once at startup, and every screen wants all of it.

## Theme

Light, dark or match-system, chosen in Settings and stored in DataStore. The app
owns this rather than deferring to the phone, because reading is not the same as
using an app: plenty of people keep their phone in dark mode all day and still
want study notes on a light page.

The notes follow the same choice. `reader.css` carries the light palette and
`reader-dark.css` the dark one, and `ReaderCssHandler` assembles the stylesheet
at request time through `WebViewAssetLoader`. A `@media (prefers-color-scheme)`
block would have handed the decision back to the system — the exact bug this
replaced. Doing it in the loader also keeps JavaScript switched off and avoids
the flash of restyling a page after it has painted.

## Reading experience

`assets/reader.css` is the single stylesheet behind every article. It gives each
heading level its own visual anchor, puts code in a dark editor panel in both
themes (mapping the source's own syntax tokens to a legible palette), frames
diagrams so they stay visible against a dark background, and gives callouts,
formula plaques and complexity chips distinct, skimmable forms.

The reader itself adds a scroll-progress bar, a text-size control persisted in
DataStore, and previous/next topic navigation so studying is a sequence rather
than repeated trips back to a list.

**Known limitation:** 899 of the 918 images in the bundled notes are hotlinks to
external CDNs, so diagrams need an internet connection and will break if those
URLs change. Figures are framed and labelled so a missing diagram reads as an
explained placeholder, but hosting the images is unresolved.

## Practice tests

A three-hour mock is the wrong shape for a phone. `assets/questions/<subject>.json`
holds questions tagged with the topic they belong to, and `TestRepository`
assembles a paper on demand:

- **Topic practice** — up to 10 questions from one topic, offered on a topic row
  once that topic has at least 3 questions
- **Subject practice** — up to 20 questions across the subject

Duration is roughly two minutes a question, which is the GATE pace. Questions are
shuffled so a second attempt is not the same paper in the same order.

The generated test's id encodes what to build (`quick:<subject>:<topic>`), so it
reaches the player through exactly the same route as a bundled test — no second
player and no second ViewModel.

## The test engine

`core/model/TestModels.kt` and `core/model/Attempt.kt` model the real paper:

- **Three question types.** MCQ (single correct), MSQ (one or more, no partial
  credit), NAT (typed number, matched against the published tolerance range).
- **GATE marking.** A wrong MCQ costs a third of its marks; MSQ and NAT carry
  no penalty; unattempted always scores zero.
- **A question palette** with answered / marked / skipped states, a countdown
  that pauses when the screen is not visible, mark-for-review, clear response,
  and free navigation in both directions.
- **A scorecard** with per-section totals, accuracy on attempted questions,
  marks lost to negative marking, and a per-question review showing the chosen
  and correct options.

Scoring is a pure function of the attempt plus the paper (`scoreAttempt`), so
the rules are unit-tested without a device.

Attempts are persisted as JSON in the app's files directory: an in-progress
attempt survives being killed, and finished attempts are kept as history. Room
replaces this once attempt history needs querying for analytics.

## Tests

`ContentIndexTest` validates the material that actually ships: every referenced
asset exists on disk, ids are unique, no HTML article is orphaned, titles are
presentable, and papers are ordered. Content drift breaks the build rather than
the app.

## Project layout

```
app/                     the Kotlin + Compose app
  src/main/assets/       study material + generated content_index.json
  src/main/java/com/gatemaster/app/
    core/model/          serializable content model
    core/data/           ContentRepository (assets -> model)
    navigation/          type-safe routes
    ui/                  theme, screens, reader
tools/                   content pipeline scripts
legacy/                  pre-rewrite Java/XML app, excluded from the build
```

## Branches

All 30 GATE 2026 papers are selectable. The paper is chosen on first launch and
switchable from the chip in the top bar.

General Aptitude is 15 marks in **every** paper, so its 26 articles are shared
across all 30 branches — every paper has real content from day one. Beyond that,
CS has the full note set; the other papers currently offer their official
syllabus, which is what candidates look up most often anyway.

## Not done yet

- Room + DataStore for progress, bookmarks, and richer attempt analytics
- Hilt (the app currently uses a hand-rolled `AppContainer`)
- Accounts, sync, and serving content from a backend instead of the APK
- Release signing config — required before the first Play upload
- Notes for papers other than CS — the structure and syllabus are in place,
  the articles are not
- Detailed syllabus for the 22 outline papers
- Computer Networks and Discrete Mathematics have no notes in CS
- Only one practice test ships; there is no per-subject question bank yet
