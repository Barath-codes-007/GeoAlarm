# Changelog

All notable changes to GeoAlarm are documented here.
The top entry must match the `VERSION` file (checked by `python scripts/sync_version.py --check`).

## [1.0.0] - 2026-09-21

### Added
- Android app (Kotlin, Jetpack Compose) with destination alarms, warning distances, arrival radius, custom alarm audio, history, saved places and recent destinations.
- Location engine with GPS-noise filtering, arrival confirmation and adaptive update frequency.
- Foreground-service monitoring, boot recovery and notification actions.
- OpenStreetMap-compatible MapLibre map behind a `MapProvider` abstraction.
- Static website (GitHub Pages compatible) and FastAPI backend (Render ready).
- Documentation, CI workflows and manual test checklist (all tests NOT TESTED until performed on a device).
