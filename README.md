# GateMaster

GATE Computer Science preparation app — notes, previous-year papers, and (next)
a real mock-test engine.

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

## Not done yet

- Mock-test engine (MCQ / MSQ / NAT, timer, negative marking, scorecard)
- Room + DataStore for progress, bookmarks, and attempt history
- Hilt (the app currently uses a hand-rolled `AppContainer`)
- Accounts, sync, and serving content from a backend instead of the APK
- Release signing config — required before the first Play upload
- Computer Networks and Discrete Mathematics have no content
