# Releasing TripLog

Releases are built by the `Release` GitHub Actions workflow. It signs the app, builds an APK and an AAB, and attaches both to the GitHub release for the tag.

## One-off setup: signing key

1. Create a keystore. Keep the file and its passwords somewhere safe. If you lose the key you cannot publish updates to the same Play Store listing.

   ```bash
   keytool -genkeypair -v -keystore triplog-release.jks -alias triplog -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Add four repository secrets (Settings, Secrets and variables, Actions):

   | Secret | Value |
   |---|---|
   | `KEYSTORE_BASE64` | The keystore encoded as base64, for example `base64 -w0 triplog-release.jks` |
   | `KEYSTORE_PASSWORD` | The keystore password |
   | `KEY_ALIAS` | The key alias (`triplog` in the example above) |
   | `KEY_PASSWORD` | The key password |

Never commit the keystore. `*.jks`, `*.keystore` and `keystore.properties` are already git-ignored.

## Cutting a release

1. Update `versionCode` and `versionName` in `app/build.gradle.kts` and merge to `main`.
2. Tag and push:

   ```bash
   git tag v1.1.0
   git push origin v1.1.0
   ```

3. The workflow checks that the tag matches `versionName`, runs the unit tests, builds the signed files and uploads `triplog-v1.1.0.apk` and `triplog-v1.1.0.aab` to the release. If the release does not exist yet it is created with generated notes.

The manual run option rebuilds and re-attaches files for a tag that already contains this workflow and the signing configuration. Tags created before that (such as `v1.0.0`) cannot be rebuilt this way.

## Building a signed release locally

Create `keystore.properties` in the project root (it is git-ignored):

```properties
storeFile=triplog-release.jks
storePassword=your-store-password
keyAlias=triplog
keyPassword=your-key-password
```

Then run `./gradlew assembleRelease`. Without signing details the release build is produced unsigned, so a fresh clone still builds.

## Backend URL

Release builds read the backend URL from the `API_BASE_URL` environment variable. In GitHub, add it as a repository **variable** (not a secret) under Settings, Secrets and variables, Actions, Variables. If it is missing the workflow still builds, but warns that the app ships with the placeholder URL and sync stays off. For a local release build, put `apiBaseUrl=https://...` in `local.properties`.
