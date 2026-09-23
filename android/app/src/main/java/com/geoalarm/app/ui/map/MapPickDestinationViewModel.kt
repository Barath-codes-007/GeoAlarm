package com.geoalarm.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoalarm.app.data.repository.SavedPlaceRepository
import com.geoalarm.app.data.repository.SettingsRepository
import com.geoalarm.app.domain.engine.GeoMath
import com.geoalarm.app.map.GeocodeResult
import com.geoalarm.app.map.GeocoderProvider
import com.geoalarm.app.map.LatLng
import com.geoalarm.app.domain.model.SavedPlace
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SelectedDestination(val label: String, val position: LatLng, val isFromReverseGeocode: Boolean)

data class MapPickUiState(
    val query: String = "",
    val searching: Boolean = false,
    val results: List<GeocodeResult> = emptyList(),
    val selected: SelectedDestination? = null,
    val currentLocation: LatLng? = null,
    val distanceMetersFromCurrent: Double? = null,
    val savedPlaces: List<SavedPlace> = emptyList(),
    val error: String? = null
)

class MapPickDestinationViewModel(
    private val geocoder: GeocoderProvider,
    private val savedPlaceRepository: SavedPlaceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MapPickUiState())
    val state: StateFlow<MapPickUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            savedPlaceRepository.observeAll().collect { places ->
                _state.value = _state.value.copy(savedPlaces = places)
            }
        }
    }

    fun onQueryChanged(query: String) {
        _state.value = _state.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.value = _state.value.copy(results = emptyList(), searching = false)
            return
        }
        // Debounce: Nominatim's usage policy forbids rapid-fire requests per keystroke.
        searchJob = viewModelScope.launch {
            delay(500)
            _state.value = _state.value.copy(searching = true, error = null)
            val result = geocoder.search(query)
            result.fold(
                onSuccess = { results -> _state.value = _state.value.copy(results = results, searching = false) },
                onFailure = { _state.value = _state.value.copy(searching = false, error = "Search failed. Check your connection and try again.") }
            )
        }
    }

    fun onResultSelected(result: GeocodeResult) {
        _state.value = _state.value.copy(
            selected = SelectedDestination(result.label, result.position, isFromReverseGeocode = false),
            results = emptyList(),
            query = result.label
        )
        recomputeDistance()
    }

    fun onSavedPlaceSelected(place: SavedPlace) {
        _state.value = _state.value.copy(
            selected = SelectedDestination(place.label, LatLng(place.latitude, place.longitude), isFromReverseGeocode = false)
        )
        recomputeDistance()
    }

    /** Section 5: manual tap selection. Reverse-geocodes to a human label unless the user
     * has turned that off in Settings (see web/privacy.html — this sends coordinates to the
     * configured geocoder). */
    fun onMapTapped(position: LatLng) {
        _state.value = _state.value.copy(
            selected = SelectedDestination("Dropped pin", position, isFromReverseGeocode = false)
        )
        recomputeDistance()
        viewModelScope.launch {
            val enabled = settingsRepository.reverseGeocodeTappedPoints.first()
            if (!enabled) return@launch
            geocoder.reverseGeocode(position).onSuccess { label ->
                val stillSameSelection = _state.value.selected?.position == position
                if (stillSameSelection) {
                    _state.value = _state.value.copy(selected = SelectedDestination(label, position, isFromReverseGeocode = true))
                }
            }
        }
    }

    fun onCurrentLocationAvailable(position: LatLng) {
        _state.value = _state.value.copy(currentLocation = position)
        recomputeDistance()
    }

    fun onUseCurrentLocationAsDestination() {
        val loc = _state.value.currentLocation ?: return
        _state.value = _state.value.copy(selected = SelectedDestination("Current location", loc, isFromReverseGeocode = false))
        recomputeDistance()
    }

    private fun recomputeDistance() {
        val dest = _state.value.selected?.position ?: return
        val current = _state.value.currentLocation ?: return
        _state.value = _state.value.copy(
            distanceMetersFromCurrent = GeoMath.distanceMeters(current.latitude, current.longitude, dest.latitude, dest.longitude)
        )
    }
}
