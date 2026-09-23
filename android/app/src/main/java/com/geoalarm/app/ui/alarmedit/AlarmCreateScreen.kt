package com.geoalarm.app.ui.alarmedit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.geoalarm.app.domain.model.AlarmSoundType
import com.geoalarm.app.domain.model.ArrivalRadiusPreset
import com.geoalarm.app.domain.model.WarningDistancePreset
import com.geoalarm.app.ui.common.rememberContainerViewModel

@Composable
fun AlarmCreateScreen(
    destinationName: String,
    latitude: Double,
    longitude: Double,
    onCreated: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = rememberContainerViewModel {
        AlarmCreateViewModel(it.alarmRepository, destinationName, latitude, longitude)
    }
    val state by viewModel.state.collectAsState()

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.onCustomAudioSelected(uri.toString(), uri.lastPathSegment)
        }
    }

    LaunchedEffect(state.created) {
        if (state.created) state.createdAlarmId?.let(onCreated)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("New GeoAlarm") }) }) { padding ->
        Column(
            Modifier.fillMaxWidth().padding(padding).padding(20.dp).verticalScroll(rememberScrollState())
        ) {
            Text("DESTINATION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(state.destinationName, style = MaterialTheme.typography.headlineMedium)
            Text(
                "Coordinates: %.5f, %.5f".format(state.latitude, state.longitude),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            Text("WARNING DISTANCE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            WarningDistancePreset.entries.forEach { preset ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !state.useCustomWarning && state.warningPreset == preset,
                        onClick = { viewModel.onWarningPresetSelected(preset) }
                    )
                    Text(preset.label)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = state.useCustomWarning, onClick = { viewModel.onCustomWarningToggled(true) })
                Text("Custom distance:")
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = state.customWarningMeters,
                    onValueChange = { viewModel.onCustomWarningChanged(it); viewModel.onCustomWarningToggled(true) },
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    suffix = { Text("m") }
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("ARRIVAL RADIUS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("How close counts as \"arrived\".", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                ArrivalRadiusPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = state.arrivalRadius == preset,
                        onClick = { viewModel.onArrivalRadiusSelected(preset) },
                        label = { Text("${preset.meters} m") }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("ALARM SOUND", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SoundOptionRow("Phone default ringtone", state.soundType == AlarmSoundType.DEFAULT_RINGTONE) {
                viewModel.onSoundTypeSelected(AlarmSoundType.DEFAULT_RINGTONE)
            }
            SoundOptionRow(
                "Select audio file" + (state.soundLabel?.let { " ($it)" } ?: ""),
                state.soundType == AlarmSoundType.CUSTOM_AUDIO
            ) { audioPicker.launch(arrayOf("audio/*")) }
            SoundOptionRow("Notification sound", state.soundType == AlarmSoundType.NOTIFICATION_SOUND) {
                viewModel.onSoundTypeSelected(AlarmSoundType.NOTIFICATION_SOUND)
            }
            SoundOptionRow("Vibration only", state.soundType == AlarmSoundType.VIBRATION_ONLY) {
                viewModel.onSoundTypeSelected(AlarmSoundType.VIBRATION_ONLY)
            }
            SoundOptionRow("Silent notification", state.soundType == AlarmSoundType.SILENT) {
                viewModel.onSoundTypeSelected(AlarmSoundType.SILENT)
            }

            Spacer(Modifier.height(24.dp))
            Text("NOTE (OPTIONAL)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChanged,
                placeholder = { Text("e.g. \"Get down at Platform 3\"") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
            Button(onClick = { viewModel.createAlarm(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("CREATE GEOALARM")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SoundOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}
