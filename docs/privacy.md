# Privacy (developer reference)

This is the implementation-facing companion to `web/privacy.html`, which is what real
users read. Keep the two in sync — the website must never claim more privacy protection
than the code actually delivers, and this file must never gloss over a leak the website
promises not to have.

## What the code actually does

| Data | Where it lives | Leaves the device? |
|---|---|---|
| Alarms, saved places, history, settings | `GeoAlarmDatabase` (Room, on-device SQLite) + DataStore | No. `android:allowBackup="false"` and `data_extraction_rules.xml` exclude both from cloud backup and device-transfer. |
| Live location while monitoring | Held in memory by `ArrivalDetectionEngine` / `LocationMonitoringService`; not written to any log or database. | No. |
| Chosen custom alarm audio | Referenced by a persisted content URI (`GeoAlarm.soundUri`); the file itself stays wherever the user picked it from. | No. |
| Map tile requests | `MapLibreMapProvider` → configured tile URL | Yes — the visible map area and the device's IP, like any web request. Default: OpenStreetMap tile servers. |
| Search / reverse-geocode text or coordinates | `NominatimGeocoderProvider` → configured geocoder URL | Yes, when the user searches or taps the map (reverse geocoding can be turned off in Settings → Privacy; see `SettingsRepository.reverseGeocodeTappedPoints`). Default: Nominatim. |
| Backend calls | Website only (`web/js/main.js`); nothing in `android/` calls the backend. | N/A to the app. |

## Verifying this stays true

Before each release, re-check:

- `grep -r "location" android/app/src/main/java/com/geoalarm/app/data` should show no
  network client — only Room/DataStore.
- No `okhttp3` or `HttpURLConnection` usage outside `map/NominatimGeocoderProvider.kt`
  (and any future replacement provider under `map/`).
- `AndroidManifest.xml`'s `allowBackup` is still `false` and
  `data_extraction_rules.xml` still excludes `database` and `sharedpref`.
- The website's privacy page (`web/privacy.html`) still matches the table above.

## Children

GeoAlarm has no account system and does not knowingly collect personal information from
anyone, including children, because — outside the map/geocoding requests above — it
collects none.
