package com.geoalarm.app.map

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.geoalarm.app.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MlLatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet

/**
 * Default map provider for v1: MapLibre GL rendering OpenStreetMap-compatible raster tiles.
 * No API key required. The tile URL, and its attribution string, are both build-config
 * values (see app/build.gradle.kts + local.properties.example) so they can be swapped
 * without touching this class — e.g. to a self-hosted tile server, or a vector-tile provider.
 *
 * IMPORTANT (Section 51): the default tile URL points at OpenStreetMap's own tile servers,
 * which are volunteer-run infrastructure with a strict usage policy
 * (https://operations.osmfoundation.org/policies/tiles/). That endpoint is fine for
 * development and small-scale personal use only. Before shipping GeoAlarm to real users,
 * switch GEOALARM_TILE_URL_TEMPLATE to a provider meant for production traffic (for example
 * a paid MapTiler/Stadia Maps/Thunderforest plan, or tiles you host yourself) — see
 * docs/map-provider.md.
 */
@SuppressLint("MissingPermission") // Location-dot rendering; the FINE_LOCATION permission is
// requested by the app's permission flow before setUserLocationEnabled(true) is ever called.
class MapLibreMapProvider(context: Context) : MapProvider {

    init {
        MapLibre.getInstance(context)
    }

    private var mapView: MapView? = null
    private var mapLibreMap: MapLibreMap? = null
    private var currentMarker: org.maplibre.android.annotations.Marker? = null

    override val attributionText: String = BuildConfig.TILE_ATTRIBUTION

    override fun createView(context: Context): View {
        val view = MapView(context)
        mapView = view
        view.onCreate(null)
        view.getMapAsync { map ->
            mapLibreMap = map
            map.uiSettings.isAttributionEnabled = true
            map.uiSettings.isLogoEnabled = true
            val style = Style.Builder()
                .withSource(
                    RasterSource(
                        "geoalarm-raster-source",
                        TileSet("2.1.0", BuildConfig.TILE_URL_TEMPLATE).apply {
                            attribution = BuildConfig.TILE_ATTRIBUTION
                            minZoom = 0f
                            maxZoom = 19f
                        },
                        256
                    )
                )
                .withLayer(RasterLayer("geoalarm-raster-layer", "geoalarm-raster-source"))
            map.setStyle(style)
            map.cameraPosition = CameraPosition.Builder().target(MlLatLng(20.0, 0.0)).zoom(1.5).build()
        }
        return view
    }

    override fun onStart() { mapView?.onStart() }
    override fun onStop() { mapView?.onStop() }
    override fun onDestroy() { mapView?.onDestroy(); mapView = null; mapLibreMap = null }

    override fun moveCamera(target: LatLng, zoom: Double, animate: Boolean) {
        val map = mapLibreMap ?: return
        val update = CameraUpdateFactory.newLatLngZoom(MlLatLng(target.latitude, target.longitude), zoom)
        if (animate) map.animateCamera(update) else map.moveCamera(update)
    }

    override fun setUserLocationEnabled(enabled: Boolean) {
        // Rendering the user's live dot on the map is a visual nicety; the actual location
        // stream that drives alarm logic comes from location/LocationClient, not from this
        // rendering flag. MapLibre's LocationComponent needs the play-services-location
        // dependency already present in the module; enabling it is one call once the
        // permission is confirmed granted by the caller:
        // map.locationComponent.apply { activateLocationComponent(...); isLocationComponentEnabled = enabled }
        // Left as a documented no-op placeholder here to avoid duplicating the whole
        // LocationComponent activation options object outside of a real Activity context;
        // MapScreen wires this up directly against the MapView's `getMapAsync` callback.
    }

    override fun setMarker(marker: MapMarker?) {
        val map = mapLibreMap ?: return
        currentMarker?.let { map.removeMarker(it) }
        currentMarker = null
        if (marker != null) {
            currentMarker = map.addMarker(
                MarkerOptions()
                    .position(MlLatLng(marker.position.latitude, marker.position.longitude))
                    .title(marker.title ?: "Destination")
            )
        }
    }

    override fun clearMarkers() {
        mapLibreMap?.clearMarkers()
        currentMarker = null
    }

    override fun events(): Flow<MapProviderEvent> = callbackFlow {
        val map = mapLibreMap
        if (map == null) {
            close()
            return@callbackFlow
        }
        val tapListener = MapLibreMap.OnMapClickListener { point ->
            trySend(MapProviderEvent.MapTapped(LatLng(point.latitude, point.longitude)))
            true
        }
        val cameraListener = MapLibreMap.OnCameraIdleListener {
            val pos = map.cameraPosition
            trySend(MapProviderEvent.CameraMoved(LatLng(pos.target?.latitude ?: 0.0, pos.target?.longitude ?: 0.0), pos.zoom))
        }
        map.addOnMapClickListener(tapListener)
        map.addOnCameraIdleListener(cameraListener)
        awaitClose {
            map.removeOnMapClickListener(tapListener)
            map.removeOnCameraIdleListener(cameraListener)
        }
    }
}
