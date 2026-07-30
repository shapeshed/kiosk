#!/usr/bin/env sh
set -eu

if [ -n "${KIOSK_KEYSTORE_BASE64:-}" ]; then
  keystore_file="$CI_PROJECT_DIR/kiosk-release.jks"
  printf '%s' "$KIOSK_KEYSTORE_BASE64" | base64 -d > "$keystore_file"
  export KIOSK_KEYSTORE_FILE="$keystore_file"
fi

if [ -n "${KIOSK_KEYSTORE_FILE:-}" ]; then
  echo "Kiosk release signing configured."
else
  echo "Kiosk release signing not configured; release artifact will be unsigned."
fi
