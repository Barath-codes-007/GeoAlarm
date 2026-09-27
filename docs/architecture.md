# Architecture

GeoAlarm is three independent pieces that share a version number and branding:

```
android/   Kotlin + Jetpack Compose app (the product)
web/       Static marketing + docs site (GitHub Pages)
backend/   FastAPI service (Render) — release metadata only, never required by the app
```

## Android app

Layered, roughly Clean-Architecture:

```
domain/   Pure Kotlin: models (GeoAlarm, AlarmStatus), the arrival-detection engine,
          Haversine math. No Android imports — unit-testable with plain JUnit.
data/     Room database (entities, DAOs), repositories (single source of truth per
          concern), DataStore-backed settings.
map/      MapProvider + GeocoderProvider abstractions, with a MapLibre/OpenStreetMap
          implementation. Nothing outside this package imports a MapLibre type.
location/ LocationClient (adaptive-frequency wrapper over FusedLocationProvider) and
          LocationMonitoringService, the single foreground service for every alarm.
alarm/    AlarmAudioController — ringtone/custom-file/vibration playback with
          never-crash fallback.
notification/  Channels, notification builders, the action-button BroadcastReceiver.
boot/     BootRestoreReceiver — resumes monitoring after reboot if the OS allows it.
ui/       Compose screens + small per-screen ViewModels, navigated via Jetpack
          Navigation Compose (see MainActivity.kt / ui/GeoAlarmDestinations.kt).
```

Dependency injection is a single hand-written container (`AppContainer` in
`GeoAlarmApp.kt`) rather than Hilt/Koin — the app is small enough that a DI framework
would add ceremony without changing the architecture; it is the one seam to swap in a
framework later without touching call sites.

### The alarm state machine

```
CREATED -> ACTIVE -> WARNING_TRIGGERED -> DESTINATION_REACHED -> COMPLETED
              \-> PAUSED -> ACTIVE
              \-> CANCELLED
```

`AlarmRepository` is the *only* place that writes a status transition, so the state
machine lives in one place instead of being re-implemented in the UI and the service.

### Reliability: the arrival-detection engine

`domain/engine/ArrivalDetectionEngine.kt` decides when a warning/arrival actually fires:

- A reading needs accuracy ≤ 100 m to count toward a decision at all.
- Two consecutive acceptable readings inside a zone are required (filters a single noisy
  point).
- A physically implausible jump between two readings (faster than ~360 km/h) is rejected
  and does not reset the streak.
- The line segment between two accepted readings is also checked against the geofence,
  so a fast-moving user with sparse updates can't "skip over" a small arrival radius.

See `domain/engine/ArrivalDetectionEngineTest.kt` for the behaviour this is meant to
guarantee, and `docs/testing.md` for how to exercise it on a real device.

### Background monitoring

One `LocationMonitoringService` (not one per alarm) subscribes to every
`ACTIVE`/`WARNING_TRIGGERED` alarm, requests location at a frequency driven by whichever
alarm is closest (`location/LocationClient.kt`'s `UpdateFrequency`), and stops itself the
moment there is nothing left to monitor.

## Map provider abstraction

`map/MapProvider.kt` is the only contract the rest of the app depends on. The v1
implementation, `MapLibreMapProvider`, renders OpenStreetMap-compatible raster tiles via
MapLibre (a BSD-licensed, API-key-free fork of the Mapbox Android SDK). Swapping to a
different renderer or tile host means writing a new `MapProvider`, not touching every
screen. See `docs/map-provider.md`.

## Website and backend

The website (`web/`) is fully static and GitHub Pages-compatible; it does not require
server-side rendering. The backend (`backend/`) is a small FastAPI app whose only jobs
are `/api/health`, `/api/version` and `/api/config` — the Android app never calls it for
alarm monitoring. See `docs/backend-setup.md`, `docs/render-deployment.md` and
`docs/github-pages.md`.
