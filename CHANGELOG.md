# Changelog

All notable changes to Kiosk will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and version numbers
should follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- GitLab CI and GitHub Actions release-prep workflows for quality, nightly, and tag builds.
- Release signing support through environment variables.

## [0.1.0] - 2026-08-09

### Added

- Initial release baseline for a reader-first Hacker News Android app.
- Six HN feeds, historical search, read/unread styling, and pull-to-refresh.
- Native article reader powered by vendored Mozilla Readability.
- Reader extraction warming around the visible feed list, with memory and disk caches.
- Comments bottom sheet with active-article comment preloading.
- Reader appearance settings, Speed Reader, Read Aloud, and zoomable article images.
- External-app handling for PDFs, X/Twitter, YouTube, and arbitrary links.
