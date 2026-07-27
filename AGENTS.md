# AGENTS.md

Guidance for future agents working on Kiosk.

## Project

Kiosk is a deliberately boring, durable Hacker News reader for Android, built with Kotlin,
Gradle, and Jetpack Compose Material 3 Expressive. No accounts, no analytics, no tracking SDKs.
The package/application id is:

```text
com.shapeshed.kiosk
```

Use JDK 17. The app compiles with Android SDK 37, targets SDK 37, and has a minimum SDK of 26.

## Common Commands

Build and install a debug build:

```sh
./gradlew :app:installDebug
```

Run the quality gate (compile, lint, unit tests):

```sh
./gradlew quality
```

Build only debug Kotlin when checking a small UI/code change:

```sh
./gradlew :app:compileDebugKotlin
```

Run just the unit tests:

```sh
./gradlew :app:testDebugUnitTest
```

## Architecture

- `KioskApp` owns the app's few long-lived singletons by hand — no DI framework. It builds the
  shared OkHttp client (with a disk HTTP cache) and the `HnRepository`, and is Coil's image-loader
  factory so favicons load through the same client.
- Feed and article data come from the official read-only [Hacker News API](https://github.com/HackerNews/API)
  over OkHttp — no key, no auth. It's N+1 by nature; the repository fans requests out on
  `Dispatchers.IO`. Historical search uses the unauthenticated [Algolia HN Search API](https://hn.algolia.com/api)
  via the same OkHttp client.
- Pure logic (JSON parsing, comment-thread flattening, HTML→text, relative time, host extraction)
  lives in `data/` as plain functions and is unit-tested — keep it free of Android/Compose types.
- UI is Jetpack Compose with an adaptive `NavigableListDetailPaneScaffold` (list on phones,
  list+article on tablets/foldables). Articles open in an in-app `WebView` with a Reader view
  (vendored Mozilla Readability.js) and comments in a summoned `ModalBottomSheet`.
- Persisted settings (reader theme, reader/web preference, viewed story ids, selected feed) live in
  a Preferences DataStore (`SettingsStore`).

## UI Notes

- Favor a minimal, calm UI. Keep controls direct and preserve the distraction-free reading feel.
- Prefer Material 3 / Material 3 Expressive components and patterns already used in the app rather
  than introducing custom interaction styles. Check styling against the M3 spec (tab labels, shape,
  colour roles) rather than approximating.
- Search follows the same Material 3 contained search bar pattern as Aerial: collapsed
  `SearchBar`, expanded `ExpandedFullScreenContainedSearchBar`, and `FilterChip` controls for
  result filters.
- Colour is adaptive: dynamic (wallpaper-derived) on Android 12+, the expressive baseline scheme
  otherwise. Read text/icon colours from the same `MaterialTheme.colorScheme` roles as their
  backgrounds — no hard-coded `Color.Black`/`Color.White` over themed surfaces.
- Do not add third-party libraries without asking first.

## Release Versioning

Release versions use:

- Gradle `versionName`: plain semver, for example `0.1.1`
- Gradle `versionCode`: monotonically increasing integer
- Git tags: prefixed with `v`, for example `v0.1.1`

For a release, update `versionCode`/`versionName` in `app/build.gradle`, then commit. Do not tag
or push unless explicitly asked.

## Git Hygiene

Use Conventional Commits:

```text
type(scope): description
```

Never commit `local.properties` (it holds the machine's `sdk.dir`), build output, or IDE files —
`.gitignore` covers these. The worktree may contain user changes; do not revert unrelated changes.

Reference: https://www.conventionalcommits.org/
