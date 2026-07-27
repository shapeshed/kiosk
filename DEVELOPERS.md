# Developer Guide

Local development for Kiosk.

## Requirements

- JDK 17
- Android SDK with platform 37 installed
- Android Studio or the Gradle wrapper

The app targets SDK 37 and has a minimum SDK of 26. The Android SDK location is read from
`local.properties` (`sdk.dir=...`), which is machine-specific and git-ignored — Android Studio
writes it for you, or set `ANDROID_HOME`/`ANDROID_SDK_ROOT` and Gradle will find it.

## Build

```sh
./gradlew :app:assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Test, Lint, and the Quality Gate

```sh
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew :app:lintDebug           # Android lint
./gradlew quality                  # compile + lint + unit tests in one gate
```

`quality` is the maintenance gate to run before committing a change.

## Install On A Device

Connect a device with USB debugging enabled, then:

```sh
adb devices
./gradlew :app:installDebug
```

## Architecture

```
app/src/main/java/com/shapeshed/kiosk/
  KioskApp.kt            Application; owns the OkHttp client (+ HTTP cache) and repository
  MainActivity.kt        Splash, edge-to-edge, theme, entry point
  data/                  Models, HN API client, repository, pure parse/format helpers (unit-tested)
  ui/                    Stories + Article/Comments screens and ViewModels, theme, reader HTML
```

- Data comes from the official read-only [Hacker News API](https://github.com/HackerNews/API) over
  OkHttp (no key, no auth). Feeds (Top/New/Best/Ask/Show/Jobs) are ordered id lists paged 25 at a
  time; items are fetched concurrently on `Dispatchers.IO`.
- A disk HTTP cache accelerates relaunches and stories shared across feeds; feed id lists stay
  `no-cache` so pull-to-refresh is always fresh.
- Articles render in an in-app `WebView` with a Reader view (vendored Mozilla Readability.js at
  `app/src/main/assets/readability.js`); comments open in a modal bottom sheet.
- Settings persist in a Preferences DataStore (`SettingsStore`).

The parsing, thread-flattening, and text helpers in `data/` are pure and covered by unit tests;
keep them free of Android and Compose types so they stay testable without Robolectric.

## Signing / Release

There is no release signing or store-publishing pipeline set up yet. Local release builds are
unsigned:

```sh
./gradlew :app:assembleRelease   # app/build/outputs/apk/release/app-release-unsigned.apk
```
