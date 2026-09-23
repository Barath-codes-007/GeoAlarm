package com.geoalarm.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoalarm.app.data.repository.AlarmRepository
import com.geoalarm.app.domain.engine.GeoMath
import com.geoalarm.app.domain.model.GeoAlarm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val nextAlarm: GeoAlarm? = null,
    val activeCount: Int = 0,
    val loading: Boolean = true
)

class HomeViewModel(private val alarmRepository: AlarmRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = alarmRepository.observeAll()
        .map { alarms ->
            val monitorable = alarms.filter { it.status.isMonitorable }
            HomeUiState(
                nextAlarm = monitorable.minByOrNull { it.createdAt },
                activeCount = monitorable.size,
                loading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun cancelAlarm(alarmId: Long) {
        viewModelScope.launch { alarmRepository.cancel(alarmId) }
    }

    /** Straight-line distance for the home-screen "X away" summary; live tracking with GPS
     * happens on the active-alarm screen, not here. */
    fun distanceFromKnownLocation(alarm: GeoAlarm, lat: Double, lon: Double): Double =
        GeoMath.distanceMeters(lat, lon, alarm.latitude, alarm.longitude)
}
