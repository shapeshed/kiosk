# Contributing

Thanks for considering contributing to Kiosk.

## Code Of Conduct

This project follows the [Code of Conduct](CODE_OF_CONDUCT.md). By
participating, you are expected to uphold it.

## Reporting Bugs And Requesting Features

Please use the [issue tracker](https://github.com/shapeshed/kiosk/issues).
Search existing issues first to avoid duplicates. For security
vulnerabilities, see [SECURITY.md](SECURITY.md) instead of opening a public
issue.

## Development Setup

Build, test, and release instructions live in
[DEVELOPERS.md](../DEVELOPERS.md). Run the quality gate before opening a PR:

```sh
./gradlew quality
```

## Submitting A Pull Request

1. Create a branch off `main`.
2. Keep pull requests focused on a single change where possible.
3. Run `./gradlew quality` locally before pushing; CI runs the same check.
4. Use the [Conventional Commits](https://www.conventionalcommits.org/)
   style used throughout the project, for example `fix(reader): ...` or
   `feat(search): ...`.
5. Open a pull request describing what changed and why. Link related issues.

Keep the app reader-focused and minimal. Avoid accounts,
analytics, tracking SDKs, Firebase, and unnecessary third-party libraries.
Prefer the existing Kotlin, Compose, Material 3, and ViewModel patterns.

By contributing, you agree that your contributions will be licensed under the
project's [Apache License 2.0](../LICENSE).
