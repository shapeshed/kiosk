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
- Articles render in a native reader. A hidden, image-blocking `WebView` is used only to run the
  vendored Mozilla Readability.js extractor at `app/src/main/assets/readability.js`; PDFs,
  X/Twitter, YouTube, and links open in the default external app/browser.
- Reader extractions are warmed around the visible story list and stored in memory and Room-backed
  disk cache. Comments for the active article are preloaded shortly after the article opens.
- Settings persist in a Preferences DataStore (`SettingsStore`).

The parsing, thread-flattening, and text helpers in `data/` are pure and covered by unit tests;
keep them free of Android and Compose types so they stay testable without Robolectric.

## Signing / Release

Local unsigned release build:

```sh
./gradlew :app:assembleRelease   # app/build/outputs/apk/release/app-release-unsigned.apk
```

Local signed release builds use environment variables:

```sh
export KIOSK_KEYSTORE_FILE=/path/to/kiosk-release.jks
export KIOSK_KEYSTORE_PASSWORD=...
export KIOSK_KEY_ALIAS=...
export KIOSK_KEY_PASSWORD=...
./gradlew :app:assembleRelease :app:bundleRelease
```

CI can also decode `KIOSK_KEYSTORE_BASE64` into a temporary keystore. The secret values must be set
in the Kiosk repository or as organization secrets available to this repository; they cannot be read
back or copied from another repository once stored.

## CI

GitHub Actions:

- `CI`: quality gate and debug APK artifact on pull requests and `main`.
- `Nightly Build`: scheduled/manual signed release APK, replacing the `nightly` prerelease.
- `GitHub Release`: `v*` tag build, producing signed APK/AAB artifacts and a draft GitHub release.

GitLab CI:

- `quality`: quality gate.
- `nightly`: scheduled/manual APK artifacts.
- `tag-build`: tag APK/AAB artifacts.

Neither CI setup uploads to Google Play.
