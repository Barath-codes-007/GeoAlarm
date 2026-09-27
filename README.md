<p align="center">
  <img src="web/assets/logo.svg" width="88" height="88" alt="GeoAlarm logo">
</p>

<h1 align="center">GeoAlarm</h1>
<p align="center"><strong>Never Miss Your Destination.</strong></p>

<p align="center">
  A location-based alarm for Android. Pick a destination, choose a warning distance,
  and get alerted as you approach and arrive — even with the app closed.
</p>

---

## What's in this repository

| Path | What it is |
|---|---|
| `android/` | The product: a Kotlin + Jetpack Compose Android app. |
| `web/` | A static marketing/docs website (GitHub Pages). |
| `backend/` | A small FastAPI service (Render) that publishes release metadata. Optional — the app never depends on it. |
| `docs/` | Architecture, setup, deployment and testing documentation. |
| `.github/workflows/` | CI: Android build + unit tests, backend tests, website deploy. |

## Features

- 📍 Location-based alarms with a live map, search, and manual tap selection
- 📏 Warning distance: 500 m / 250 m / 100 m / at destination / custom
- 🎯 Configurable arrival radius (50 / 100 / 200 m)
- 🎵 Default ringtone, a custom audio file, vibration-only, or silent
- 🔋 Battery-aware monitoring: GPS update frequency adapts to distance
- 📱 Background monitoring via a single foreground service for all alarms
- ⭐ Saved places and recent destinations
- 📚 Alarm history
- 🌗 Light / dark / system theme
- 🔒 Local-first: alarms, places and history never leave the device; see
  [`docs/privacy.md`](docs/privacy.md)

## Architecture

See [`docs/architecture.md`](docs/architecture.md) for the full picture, including the
alarm state machine and the GPS-noise-tolerant arrival detection engine
(`android/app/src/main/java/com/geoalarm/app/domain/engine/ArrivalDetectionEngine.kt`).

## Quick start

### Android app

```bash
cd android
cp local.properties.example local.properties   # set sdk.dir to your Android SDK
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Full details, including release signing (never committed to this repo) and map/geocoder
configuration: [`docs/android-setup.md`](docs/android-setup.md).

### Website

The website is static HTML/CSS/JS — open `web/index.html` directly, or serve the folder:

```bash
cd web && python3 -m http.server 8080
```

Deploys automatically to GitHub Pages on push to `main`; see
[`docs/github-pages.md`](docs/github-pages.md).

### Backend (optional)

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r backend/requirements-dev.txt
cp .env.example .env
uvicorn app.main:app --app-dir backend --reload --port 8000
```

Details: [`docs/backend-setup.md`](docs/backend-setup.md). Deploying to Render:
[`docs/render-deployment.md`](docs/render-deployment.md).

## API (backend)

| Endpoint | Purpose |
|---|---|
| `GET /api/health` | Liveness check |
| `GET /api/version` | Latest version, release notes, download URL |
| `GET /api/config` | Public, non-secret configuration for the website |

## Permissions the Android app requests, and why

Explained to the user, one at a time, before each system dialog — see
[`docs/android-setup.md`](docs/android-setup.md) and the in-app permission flow
(`ui/permissions/`). Full table in [`web/docs.html`](web/docs.html) (published at your
GitHub Pages URL once deployed — see [`docs/github-pages.md`](docs/github-pages.md)).

## Privacy

Location stays on the device by default. Only map tiles and place search reach a
third-party provider (OpenStreetMap tiles / Nominatim, by default), and only while
you're actively using the map. Full details: [`docs/privacy.md`](docs/privacy.md) and
[`web/privacy.html`](web/privacy.html).

## Development setup / environment variables

- Android: `android/local.properties.example` (map/geocoder config; SDK path)
- Backend + website: `.env.example` at the repository root
- Version: edit the single `VERSION` file at the repository root, add a matching entry
  to `CHANGELOG.md`; `scripts/sync_version.py --check` verifies they agree, and the
  Android Gradle config / backend `/api/version` both read `VERSION` directly.

## Build instructions / APK release

Debug APK: see "Quick start" above, or let `.github/workflows/android-build.yml` build
one on every push (download it from the workflow run's Artifacts). **Release builds are
unsigned by default** — no signing key is or should ever be committed to this
repository. See [`docs/android-setup.md`](docs/android-setup.md#release-signing).

## Testing

- Backend: `cd backend && python -m pytest -v` — **9/9 passing**, run as part of CI.
- Android unit tests: `cd android && ./gradlew testDebugUnitTest` — 15 tests covering
  the arrival-detection engine's GPS-noise handling. **Not executed via Gradle in the
  environment that produced this repository** (no Android SDK there); run them
  yourself, and CI runs them on every push. See [`docs/testing.md`](docs/testing.md) for
  what was and wasn't verified, including the full manual device checklist (currently
  all rows marked NOT TESTED, as they must be until a person with a real device runs
  them).

## Known Android limitations

No app can guarantee background execution on every device — manufacturers vary widely
in how aggressively they kill background services and restrict autostart. GeoAlarm uses
Android's recommended mechanism (a foreground service with a visible notification), asks
for background location, and resumes monitoring after reboot when the OS allows it — but
some phones will still need the user to manually exempt GeoAlarm from battery
optimization or an OEM-specific "protected apps" list. See
[`docs/android-setup.md`](docs/android-setup.md) and the website's
[background-limits section](web/docs.html#background).

## Future roadmap

Section "44" of the original product spec (not all implemented, architecture left room
for): account sync, cloud backup, Wear OS, Android Auto, recurring/commute alarms,
public-transport alerts, shared/family destinations, home-screen widgets, a Quick
Settings tile, and voice commands.

## License

[MIT](LICENSE).
