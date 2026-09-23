package com.geoalarm.app.ui.alarmedit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoalarm.app.data.repository.AlarmRepository
import com.geoalarm.app.domain.model.AlarmSoundType
import com.geoalarm.app.domain.model.ArrivalRadiusPreset
import com.geoalarm.app.domain.model.GeoAlarm
import com.geoalarm.app.domain.model.WarningDistancePreset
import com.geoalarm.app.location.LocationServiceCommands
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlarmCreateUiState(
    val destinationName: String,
    val latitude: Double,
    val longitude: Double,
    val warningPreset: WarningDistancePreset = WarningDistancePreset.FIVE_HUNDRED,
    val customWarningMeters: String = "",
    val useCustomWarning: Boolean = false,
    val arrivalRadius: ArrivalRadiusPreset = ArrivalRadiusPreset.default,
    val soundType: AlarmSoundType = AlarmSoundType.DEFAULT_RINGTONE,
    val soundUri: String? = null,
    val soundLabel: String? = null,
    val note: String = "",
    val created: Boolean = false,
    val createdAlarmId: Long? = null
) {
    val effectiveWarningMeters: Int?
        get() = when {
            useCustomWarning -> customWarningMeters.toIntOrNull()?.takeIf { it > 0 }
            else -> warningPreset.meters
        }
}

class AlarmCreateViewModel(
    private val alarmRepository: AlarmRepository,
    destinationName: String,
    latitude: Double,
    longitude: Double
) : ViewModel() {

    private val _state = MutableStateFlow(AlarmCreateUiState(destinationName, latitude, longitude))
    val state: StateFlow<AlarmCreateUiState> = _state.asStateFlow()

    fun onWarningPresetSelected(preset: WarningDistancePreset) {
        _state.value = _state.value.copy(warningPreset = preset, useCustomWarning = false)
    }

    fun onCustomWarningToggled(enabled: Boolean) {
        _state.value = _state.value.copy(useCustomWarning = enabled)
    }

    fun onCustomWarningChanged(value: String) {
        _state.value = _state.value.copy(customWarningMeters = value.filter { it.isDigit() })
    }

    fun onArrivalRadiusSelected(preset: ArrivalRadiusPreset) {
        _state.value = _state.value.copy(arrivalRadius = preset)
    }

    fun onSoundTypeSelected(type: AlarmSoundType) {
        _state.value = _state.value.copy(soundType = type, soundUri = null, soundLabel = null)
    }

    fun onCustomAudioSelected(uri: String, label: String?) {
        _state.value = _state.value.copy(soundType = AlarmSoundType.CUSTOM_AUDIO, soundUri = uri, soundLabel = label)
    }

    fun onNoteChanged(note: String) {
        _state.value = _state.value.copy(note = note.take(140))
    }

    fun createAlarm(context: Context) {
        val s = _state.value
        viewModelScope.launch {
            val alarm = GeoAlarm(
                destinationName = s.destinationName,
                latitude = s.latitude,
                longitude = s.longitude,
                warningDistanceMeters = s.effectiveWarningMeters,
                arrivalRadiusMeters = s.arrivalRadius.meters,
                soundType = s.soundType,
                soundUri = s.soundUri,
                note = s.note.ifBlank { null },
                createdAt = System.currentTimeMillis()
            )
            val id = alarmRepository.create(alarm)
            alarmRepository.activate(id)
            LocationServiceCommands.refresh(context)
            _state.value = _state.value.copy(created = true, createdAlarmId = id)
        }
    }
}
