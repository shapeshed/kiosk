# GitLab CI

Kiosk CI builds verification, nightly artifacts, and tag artifacts. It does not upload to Google
Play or any other store.

## Jobs

- `quality`: compile debug Kotlin, run lint, and run debug unit tests.
- `nightly`: scheduled build that produces debug and release APK artifacts.
- `tag-build`: tag-only build that runs `quality`, then produces release APK and AAB artifacts.

## Optional Signing Variables

Set these protected GitLab CI variables to sign release artifacts:

- `KIOSK_KEYSTORE_BASE64`: base64-encoded Java keystore.
- `KIOSK_KEYSTORE_PASSWORD`
- `KIOSK_KEY_ALIAS`
- `KIOSK_KEY_PASSWORD`

If `KIOSK_KEYSTORE_FILE` already points to a keystore available in the runner, `KIOSK_KEYSTORE_BASE64`
can be omitted.
