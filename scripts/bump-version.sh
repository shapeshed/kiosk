#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_FILE="$ROOT_DIR/app/build.gradle"
CHANGELOG_FILE="$ROOT_DIR/CHANGELOG.md"
FASTLANE_CHANGELOG_DIR="$ROOT_DIR/fastlane/metadata/android/en-US/changelogs"

usage() {
  cat <<'USAGE'
Bump the Kiosk release version.

Usage:
  scripts/bump-version.sh <versionName> [versionCode]

Examples:
  scripts/bump-version.sh 0.1.1
  scripts/bump-version.sh 0.1.1 2

When versionCode is omitted, the current Gradle versionCode is incremented by 1.
The script updates app/build.gradle and creates the Fastlane changelog file.
It prints the CHANGELOG.md section to add but does not edit the changelog.
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

VERSION_NAME="${1:-}"
if [[ -z "$VERSION_NAME" ]]; then
  usage >&2
  exit 1
fi

if [[ ! "$VERSION_NAME" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]]; then
  echo "Invalid versionName: $VERSION_NAME" >&2
  echo "Expected semver-like version, for example 0.1.1" >&2
  exit 1
fi

CURRENT_CODE="$(awk '/versionCode/ { print $2; exit }' "$BUILD_FILE")"
CURRENT_NAME="$(awk -F\" '/versionName/ { print $2; exit }' "$BUILD_FILE")"
VERSION_CODE="${2:-$((CURRENT_CODE + 1))}"

if [[ ! "$VERSION_CODE" =~ ^[0-9]+$ ]]; then
  echo "Invalid versionCode: $VERSION_CODE" >&2
  exit 1
fi

if (( VERSION_CODE <= CURRENT_CODE )); then
  echo "versionCode must increase: current=$CURRENT_CODE new=$VERSION_CODE" >&2
  exit 1
fi

if grep -q "versionName \"$VERSION_NAME\"" "$BUILD_FILE"; then
  echo "versionName is already $VERSION_NAME" >&2
  exit 1
fi

perl -0pi -e "s/versionCode\s+\d+/versionCode $VERSION_CODE/; s/versionName\s+\"[^\"]+\"/versionName \"$VERSION_NAME\"/;" "$BUILD_FILE"

mkdir -p "$FASTLANE_CHANGELOG_DIR"
FASTLANE_CHANGELOG="$FASTLANE_CHANGELOG_DIR/${VERSION_CODE}.txt"
if [[ ! -f "$FASTLANE_CHANGELOG" ]]; then
  printf '%s\n' '- Update release notes before publishing.' > "$FASTLANE_CHANGELOG"
fi

echo "Bumped Kiosk from $CURRENT_NAME ($CURRENT_CODE) to $VERSION_NAME ($VERSION_CODE)."
echo
echo "Add this section to CHANGELOG.md:"
echo
echo "## [$VERSION_NAME] - $(date +%F)"
echo
echo "Then move the relevant entries from [Unreleased] into that section."
