package com.geoalarm.app.map

import android.content.Context
import android.view.View
import kotlinx.coroutines.flow.Flow

data class LatLng(val latitude: Double, val longitude: Double)

data class MapMarker(val id: String, val position: LatLng, val title: String? = null)

data class GeocodeResult(val label: String, val position: LatLng)

sealed interface MapProviderEvent {
    data class MapTapped(val position: LatLng) : MapProviderEvent
    data class CameraMoved(val center: LatLng, val zoom: Double) : MapProviderEvent
}

/**
 * Everything the rest of the app needs from a map, independent of the underlying SDK
 * (Section 50). The Android app should never import a MapLibre/Google type outside the
 * `map` package. Swapping providers means implementing this interface again, not touching
 * every screen that shows a map.
 */
interface MapProvider {
    /** Returns the platform view to embed via AndroidView in Compose. Call once per screen. */
    fun createView(context: Context): View

    fun onStart()
    fun onStop()
    fun onDestroy()

    fun moveCamera(target: LatLng, zoom: Double, animate: Boolean = true)
    fun setUserLocationEnabled(enabled: Boolean)

    fun setMarker(marker: MapMarker?)
    fun clearMarkers()

    /** Emits taps and camera movement so the ViewModel stays in control of state. */
    fun events(): Flow<MapProviderEvent>

    /** Attribution text required by the tile provider's terms; must be shown on screen. */
    val attributionText: String
}

/** Search-by-text / reverse geocoding, kept separate from [MapProvider] because a provider
 * swap (e.g. changing tile hosts) does not necessarily mean changing the geocoder too. */
interface GeocoderProvider {
    suspend fun search(query: String): Result<List<GeocodeResult>>
    suspend fun reverseGeocode(position: LatLng): Result<String>
}
