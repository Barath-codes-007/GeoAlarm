package com.geoalarm.app.ui.activealarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoalarm.app.data.repository.AlarmRepository
import com.geoalarm.app.domain.engine.GeoMath
import com.geoalarm.app.domain.model.GeoAlarm
import com.geoalarm.app.location.LocationClient
import com.geoalarm.app.location.UpdateFrequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveAlarmUiState(
    val alarm: GeoAlarm? = null,
    val distanceMeters: Double? = null,
    val accuracyMeters: Float? = null,
    val lastUpdateMillis: Long? = null
)

/** Read-only live view for Section 43 — the actual warning/arrival decisions are made by
 * [com.geoalarm.app.location.LocationMonitoringService], which keeps running even if this
 * screen (and the whole app) is closed. This ViewModel only displays what is happening. */
class ActiveAlarmViewModel(
    private val alarmRepository: AlarmRepository,
    private val locationClient: LocationClient,
    alarmId: Long
) : ViewModel() {

    private val liveLocation = MutableStateFlow<Pair<Double, Float>?>(null)
    private var locationJob: kotlinx.coroutines.Job? = null

    val uiState: StateFlow<ActiveAlarmUiState> = combine(
        alarmRepository.observeById(alarmId),
        liveLocation
    ) { alarm, live ->
        ActiveAlarmUiState(
            alarm = alarm,
            distanceMeters = live?.first,
            accuracyMeters = live?.second,
            lastUpdateMillis = if (live != null) System.currentTimeMillis() else null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveAlarmUiState())

    init {
        // A light, display-only location stream — independent of the monitoring service's
        // own stream, so this screen still shows live distance even if permissions or state
        // mean the service isn't currently emitting a distance for the same alarm.
        viewModelScope.launch {
            alarmRepository.observeById(alarmId).collect { alarm ->
                if (alarm != null && locationJob == null) {
                    locationJob = viewModelScope.launch {
                        locationClient.observe(UpdateFrequency.APPROACHING).collect { reading ->
                            val d = GeoMath.distanceMeters(reading.latitude, reading.longitude, alarm.latitude, alarm.longitude)
                            liveLocation.value = d to reading.accuracyMeters
                        }
                    }
                }
            }
        }
    }

    fun cancel(alarmId: Long) = viewModelScope.launch { alarmRepository.cancel(alarmId) }

    override fun onCleared() {
        locationJob?.cancel()
        super.onCleared()
    }
}
