package com.geoalarm.app.map

import com.geoalarm.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Search/reverse-geocoding against Nominatim (default: the OpenStreetMap Foundation's public
 * instance). Section 51 requires respecting the provider's usage policy
 * (https://operations.osmfoundation.org/policies/nominatim/):
 *  - a descriptive User-Agent identifying this app (configure GEOALARM_GEOCODER_USER_AGENT),
 *  - at most one request at a time / no parallel bursts (the caller in ui/map debounces
 *    search input; this class does not fan out multiple in-flight requests itself),
 *  - no heavy/automated bulk use.
 * For production traffic beyond light personal use, point GEOALARM_GEOCODER_BASE_URL at a
 * paid or self-hosted Nominatim instance instead (see docs/map-provider.md).
 */
class NominatimGeocoderProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : GeocoderProvider {

    override suspend fun search(query: String): Result<List<GeocodeResult>> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())
        runCatching {
            val url = "${BuildConfig.GEOCODER_BASE_URL}/search" +
                "?q=${URLEncoder.encode(query, "UTF-8")}&format=json&limit=8"
            val body = execute(url) ?: return@runCatching emptyList()
            val arr = JSONArray(body)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                GeocodeResult(
                    label = obj.optString("display_name", query),
                    position = LatLng(obj.getString("lat").toDouble(), obj.getString("lon").toDouble())
                )
            }
        }
    }

    override suspend fun reverseGeocode(position: LatLng): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${BuildConfig.GEOCODER_BASE_URL}/reverse" +
                "?lat=${position.latitude}&lon=${position.longitude}&format=json"
            val body = execute(url) ?: return@runCatching "Dropped pin"
            org.json.JSONObject(body).optString("display_name", "Dropped pin")
        }
    }

    private fun execute(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BuildConfig.GEOCODER_USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Geocoder HTTP ${response.code}")
            return response.body?.string()
        }
    }
}
