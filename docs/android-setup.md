# Android setup

## Requirements

- Android Studio (Ladybug or newer) or a command-line JDK 17 + Android SDK setup.
- Android SDK Platform 35, Build-Tools matching Android Gradle Plugin 8.6.1.
- A physical device or emulator running Android 8.0 (API 26) or newer, **with Google
  Play services** (the app uses `play-services-location`; a bare AOSP emulator image
  without Play services will fail location requests).

## Building

```bash
cd android
cp local.properties.example local.properties   # then set sdk.dir to your SDK path
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Opening the `android/` folder directly in Android Studio also works and fills in
`local.properties` for you automatically.

### Configuring the map/geocoder (optional)

`local.properties.example` documents four optional keys
(`GEOALARM_TILE_URL_TEMPLATE`, `GEOALARM_TILE_ATTRIBUTION`, `GEOALARM_GEOCODER_BASE_URL`,
`GEOALARM_GEOCODER_USER_AGENT`). Leaving them unset uses OpenStreetMap's public tile
servers and Nominatim, which is fine for development but not for production traffic —
see `docs/map-provider.md` before shipping the app to real users.

## Release signing

The `release` build type in `app/build.gradle.kts` has **no signing config attached on
purpose** — a signing key must never be committed to the repository. `./gradlew
assembleRelease` will therefore produce an *unsigned* APK until you configure signing:

1. Create a keystore (keep it outside the repository):
   ```bash
   keytool -genkeypair -v -keystore geoalarm-release.jks -alias geoalarm \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Locally: add to `~/.gradle/gradle.properties` (never to the project):
   ```properties
   GEOALARM_RELEASE_STORE_FILE=/absolute/path/to/geoalarm-release.jks
   GEOALARM_RELEASE_STORE_PASSWORD=...
   GEOALARM_RELEASE_KEY_ALIAS=geoalarm
   GEOALARM_RELEASE_KEY_PASSWORD=...
   ```
3. In CI: store the same four values as GitHub Actions **secrets** and add a
   `signingConfigs { release { ... } }` block sourced from `System.getenv(...)` — this is
   deliberately left out of `android-build.yml`, which only builds the unsigned debug
   APK, so that no example signing plumbing exists for a key that was never generated.

Never commit `local.properties`, any `.jks`/`.keystore` file, or `keystore.properties` —
all three are already in `.gitignore`.

## Common issues

| Symptom | Cause |
|---|---|
| `SDK location not found` | Set `sdk.dir` in `local.properties`, or `ANDROID_HOME`. |
| Map is blank | No internet, or `GEOALARM_TILE_URL_TEMPLATE` is unreachable. |
| Location updates never arrive on an emulator | The AVD image lacks Google Play services — use a "Google Play" system image, not "Google APIs". |
| `assembleRelease` produces an unsigned APK | Expected — see "Release signing" above. |
