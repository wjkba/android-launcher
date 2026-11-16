# Minimal launcher

Text-only Android home screen app for one device: Unihertz Jelly Star (Android 13 / API 33, 480x854 px at 220 dpi
= 349x621 dp, system font scale 1.3, gesture navigation). Owner is a web developer new to Android; explain
Android-specific choices briefly when they come up.

## Commands
- `./gradlew spotlessApply testDebugUnitTest` before finishing any change (format + unit, architecture and screenshot tests).
- `./gradlew recordRoborazziDebug` after an intended UI change; then look at `app/src/test/screenshots/*.png`.
- `./gradlew installRelease` installs on the connected phone (release = R8-optimized, debug-signed).
- Launch: `adb shell am start -n dev.minimal.launcher/.app.MainActivity`.
- `python3 -I scripts/import_icons.py` regenerates folder/UI icons (Material Symbols) and `FolderIconSet.kt`; edit the lists there.

## Git
- Conventional Commits: `type(scope): summary` in English, imperative, ≤ 72 chars. Types: feat, fix, refactor,
  test, docs, build, chore, style, perf. Scope = feature or core package (`folders`, `drawer`, `core-data`…).
- Breaking changes to stored data (`home_layout.json`, DataStore keys) get a `!` and a migration.

## Structure (see docs/architecture.md)
- Feature-first packages: `app` → `feature.*` → `core.*`. Features never import each other; `core.domain`
  is plain Kotlin. Enforced by `ArchitectureTest` (Konsist). Promote code to `core` only when a second feature needs it.
- Screens are stateless composables taking state + lambdas + `modifier`; ViewModels expose `StateFlow`.
- Manual DI via `AppContainer` in `app/LauncherApplication.kt`.
- UI strings in `values/strings.xml` (English, the phone's language) and `values-pl/strings.xml`.

## Gotchas
- Size UI for 349dp width at font scale 1.3; touch targets ≥ 40dp, prefer 48dp. Thumb-reachable actions go at the bottom.
- Compose animation specs follow the system animator scale; time-critical waits (the breathing pause) use frame-clock wall time instead.
- The launcher must never finish on Back; Home press arrives as `onNewIntent`. Navigation 3 back stack lives in `LauncherViewModel`; `goHome()` trims it to `Home`.
- `home_layout.json` field names and `@SerialName`s are a file format: changing them needs a migration.
- Never change the default home app on the phone (role dialog: tap Cancel); the owner picks it.
