# Kiosk

Kiosk is a calm Hacker News reader for Android.

It shows the Hacker News feeds and their discussions, searches historical stories, and opens
articles in a built-in, distraction-free reader. That's it — no accounts, no analytics, no tracking
SDKs, no sexy features. Boring, stable tech chosen to last.

## Stack

- Kotlin + Jetpack Compose, **Material 3 Expressive** (dynamic colour on Android 12+, HN-orange
  otherwise)
- The official read-only [Hacker News API](https://github.com/HackerNews/API) over OkHttp — no
  key, no auth
- The unauthenticated [Algolia HN Search API](https://hn.algolia.com/api) for historical search
- Adaptive list-detail layout (list on phones, list+article on tablets/foldables), plain
  `ViewModel`s; no DI framework
- `minSdk 26`, Java/Kotlin 17

## Features

- Swipe (or tap) between all six HN feeds — Top, New, Best, Ask, Show, Jobs — with the last-viewed
  feed remembered
- Infinite scroll, pull-to-refresh, and read/unread styling
- In-app reader (vendored Mozilla Readability.js) with a Web ⇄ Reader toggle and light/sepia/dark
  themes, plus comments in a summoned bottom sheet

## Structure

```
app/src/main/java/com/shapeshed/kiosk/
  KioskApp.kt            Application; owns the OkHttp client (+ HTTP cache) and repository
  MainActivity.kt        Splash, edge-to-edge, theme, entry point
  data/                  Models, HN API client, repository, pure parse/format helpers
  ui/                    Stories, Article, and Comments screens and ViewModels, theme, reader HTML
```

The parsing, thread-flattening, and text helpers in `data/` are pure and covered by unit tests.

## Build

```
./gradlew :app:assembleDebug        # build
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:installDebug         # install to a connected device
./gradlew quality                   # compile + lint + tests
```

## Not here yet

Offline caching (Room) and a settings screen are deliberately left for later.
