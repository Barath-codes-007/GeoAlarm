# Testing

## Automated tests (can be run without a device)

| Suite | Command | Status here |
|---|---|---|
| Backend (`backend/tests/test_api.py`, 9 tests) | `cd backend && python -m pytest -v` | **Run in this environment: all 9 passed.** |
| Android unit tests (`domain/engine/*Test.kt`, 15 tests) | `cd android && ./gradlew testDebugUnitTest` | **Not run via Gradle in this environment** (no Android SDK / Maven access here — see note below). The arrival-detection logic these tests cover was verified with a standalone `kotlinc` scratch program during development (four scenarios: multi-reading confirmation, poor-accuracy rejection, GPS-jump rejection, fast-mover segment crossing — all passed). Run `./gradlew testDebugUnitTest` yourself before relying on this; CI (`android-build.yml`) runs it on every push. |
| Android instrumented/UI tests | none written | Not started — `app/src/androidTest` is scaffolded but empty. |

## Manual device checklist

Every row is **NOT TESTED** until a person actually performs it on a real device and
updates this table (do not mark a row tested without doing so — see Section 64/45 of the
original spec: no fake test results, no fake claims of device testing).

| # | Test | Status |
|---|---|---|
| 1 | Fresh installation | NOT TESTED |
| 2 | First launch shows onboarding | NOT TESTED |
| 3 | Location permission — grant | NOT TESTED |
| 4 | Location permission — deny, then grant later from Settings | NOT TESTED |
| 5 | Background location permission flow (Android 11+ Settings redirect) | NOT TESTED |
| 6 | Notification permission (Android 13+) | NOT TESTED |
| 7 | Map loads and renders tiles | NOT TESTED |
| 8 | Destination search returns results | NOT TESTED |
| 9 | Manual map-tap destination selection + reverse geocode | NOT TESTED |
| 10 | "Use current location" as destination | NOT TESTED |
| 11 | Create alarm with 500 m warning | NOT TESTED |
| 12 | Create alarm with 250 m warning | NOT TESTED |
| 13 | Create alarm with 100 m warning | NOT TESTED |
| 14 | Create alarm with "At destination" (no separate warning) | NOT TESTED |
| 15 | Custom warning distance | NOT TESTED |
| 16 | Default ringtone plays on arrival | NOT TESTED |
| 17 | Custom audio file plays on arrival | NOT TESTED |
| 18 | Vibration-only alarm | NOT TESTED |
| 19 | Silent notification alarm | NOT TESTED |
| 20 | Warning notification appears once, not repeatedly | NOT TESTED |
| 21 | Arrival alarm shows full-screen over a locked device | NOT TESTED |
| 22 | Stop button on the ringing screen stops audio/vibration and marks COMPLETED | NOT TESTED |
| 23 | Stop action on the notification (app closed) also works | NOT TESTED |
| 24 | Screen locked throughout the whole trip | NOT TESTED |
| 25 | App swiped from recents (not force-stopped) — does monitoring continue? | NOT TESTED |
| 26 | Move toward a real destination outdoors and confirm accurate arrival | NOT TESTED |
| 27 | GPS signal loss mid-trip, then recovery | NOT TESTED |
| 28 | Poor GPS accuracy (indoors/urban canyon) does not falsely trigger arrival | NOT TESTED |
| 29 | Cancel an active alarm from Home / My Alarms / the active-alarm screen | NOT TESTED |
| 30 | Pause and resume an alarm | NOT TESTED |
| 31 | Multiple simultaneous alarms — only one foreground service, correct nearest-alarm frequency | NOT TESTED |
| 32 | Alarm history records completed and cancelled alarms correctly | NOT TESTED |
| 33 | Clear history | NOT TESTED |
| 34 | Device reboot with an active alarm — does monitoring resume? | NOT TESTED |
| 35 | Device reboot with no active alarms — no service starts | NOT TESTED |
| 36 | Dark mode / light mode / system default | NOT TESTED |
| 37 | Fully offline after alarm creation (airplane mode, GPS only) | NOT TESTED |
| 38 | Deleted custom audio file — falls back to default ringtone without crashing | NOT TESTED |
| 39 | Permission revoked while an alarm is active (via system Settings) | NOT TESTED |
| 40 | Battery-optimization "restricted" setting applied to GeoAlarm — document actual behavior | NOT TESTED |
| 41 | Manufacturer-specific auto-start restriction (Xiaomi/Huawei/OnePlus/etc. — see dontkillmyapp.com) | NOT TESTED |
| 42 | Website renders correctly on mobile/tablet/desktop widths | NOT TESTED — no browser was available in the build environment; only static HTML/CSS validation (tag balance) was performed. See the final response for what this means. |
| 43 | Website dark/light theme toggle and persistence | NOT TESTED |
| 44 | Website download button — enabled/disabled states, real vs. missing APK URL | NOT TESTED |

## Why some things are marked "not run" instead of tested

This repository was produced in an environment without the Android SDK, an emulator, a
physical device, or a browser with network access. Everywhere that matters, the
response accompanying this repository says so explicitly rather than claiming
untested work was tested (see Section 45/64 of the original spec). The one exception is
the backend, which **was** actually executed and tested here (Python has no such
restriction in this environment).
