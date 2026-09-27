# Map provider

GeoAlarm renders its map through a single abstraction, `map/MapProvider.kt`
(`MapProvider` + `GeocoderProvider`). No other part of the app imports a MapLibre or
Google Maps type directly — replacing the map means writing a new implementation of
these two interfaces, not touching every screen (Section 50 in the original spec).

## Default (v1): MapLibre + OpenStreetMap raster tiles

`map/MapLibreMapProvider.kt` renders raster tiles with
[MapLibre GL Native for Android](https://github.com/maplibre/maplibre-native) (a
BSD-2-licensed, community-maintained fork of the last open-source Mapbox Android SDK).
No API key is required. `map/NominatimGeocoderProvider.kt` provides search and reverse
geocoding via [Nominatim](https://nominatim.org/).

Both are configured through build-time values, not hard-coded:

| `local.properties` key | Default | Purpose |
|---|---|---|
| `GEOALARM_TILE_URL_TEMPLATE` | `https://tile.openstreetmap.org/{z}/{x}/{y}.png` | Raster tile URL template |
| `GEOALARM_TILE_ATTRIBUTION` | `© OpenStreetMap contributors` | Shown on-screen over the map (required by OSM's license) |
| `GEOALARM_GEOCODER_BASE_URL` | `https://nominatim.openstreetmap.org` | Search/reverse-geocoding endpoint |
| `GEOALARM_GEOCODER_USER_AGENT` | `GeoAlarm/<version> (no contact configured)` | Sent on every geocoding request |

## ⚠️ Before shipping to real users

The default OpenStreetMap tile servers and the public Nominatim instance are
**volunteer-run infrastructure for light, non-commercial use** — not a production CDN:

- Tile usage policy: <https://operations.osmfoundation.org/policies/tiles/>
- Nominatim usage policy: <https://operations.osmfoundation.org/policies/nominatim/>
  (max ~1 request/second, a real identifying User-Agent, no heavy automated use)

For any real user base, switch to one of:

- A paid tile/geocoding provider built for production traffic (MapTiler, Stadia Maps,
  Thunderforest, LocationIQ, Mapbox, Google Maps, etc.) — just change the two
  `local.properties` values above (and the geocoder base URL / API key handling in
  `NominatimGeocoderProvider` if the new provider's request format differs).
- Self-hosting your own tile server and/or Nominatim instance.

`NominatimGeocoderProvider` already debounces search input (one request per 500 ms of
typing inactivity, in `MapPickDestinationViewModel`) and sends a configurable
`User-Agent`, but it does not implement its own rate limiter — do not point it at the
public instance for anything beyond development and personal use.

## Replacing the provider entirely

1. Implement `MapProvider` (map rendering, camera, markers, tap/camera events) and/or
   `GeocoderProvider` (search, reverse geocode) with the new SDK.
2. Swap the construction site in `ui/map/MapPickDestinationScreen.kt`
   (`MapLibreMapProvider(context)`) and in `AppContainer` (`geocoderProvider`).
3. Remove the MapLibre Gradle dependency and Maven repository from
   `app/build.gradle.kts` / `settings.gradle.kts` if no longer needed.

No other file should need to change — that is what the abstraction is for.
