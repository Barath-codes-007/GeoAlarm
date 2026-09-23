package com.geoalarm.app.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoalarm.app.data.repository.AlarmRepository
import com.geoalarm.app.domain.model.AlarmStatus
import com.geoalarm.app.domain.model.GeoAlarm
import com.geoalarm.app.domain.model.WarningDistancePreset
import com.geoalarm.app.location.LocationServiceCommands
import com.geoalarm.app.ui.common.rememberContainerViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyAlarmsViewModel(private val repository: AlarmRepository) : ViewModel() {
    val alarms = repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun pause(id: Long) = viewModelScope.launch { repository.pause(id) }
    fun resume(context: android.content.Context, id: Long) = viewModelScope.launch {
        repository.resume(id)
        LocationServiceCommands.refresh(context)
    }
    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
}

@Composable
fun MyAlarmsScreen(onBack: () -> Unit, onOpenAlarm: (Long) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel = rememberContainerViewModel { MyAlarmsViewModel(it.alarmRepository) }
    val alarms by viewModel.alarms.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My GeoAlarms") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(32.dp)) {
                Text("No GeoAlarms yet.", style = MaterialTheme.typography.titleMedium)
                Text("Create one from the home screen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onOpen = { onOpenAlarm(alarm.id) },
                    onPause = { viewModel.pause(alarm.id) },
                    onResume = { viewModel.resume(context, alarm.id) },
                    onDelete = { viewModel.delete(alarm.id) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AlarmCard(alarm: GeoAlarm, onOpen: () -> Unit, onPause: () -> Unit, onResume: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(alarm.destinationName, style = MaterialTheme.typography.titleLarge)
            Text(
                "Warning: ${WarningDistancePreset.fromMeters(alarm.warningDistanceMeters).label} · Arrival: ${alarm.arrivalRadiusMeters} m",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Status: ${statusLabel(alarm.status)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (alarm.status) {
                    AlarmStatus.PAUSED -> OutlinedButton(onClick = onResume) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Activate")
                    }
                    AlarmStatus.ACTIVE, AlarmStatus.WARNING_TRIGGERED -> OutlinedButton(onClick = onPause) {
                        Icon(Icons.Filled.PauseCircle, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Pause")
                    }
                    else -> Unit
                }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Delete")
                }
            }
        }
    }
}

private fun statusLabel(status: AlarmStatus) = when (status) {
    AlarmStatus.CREATED -> "Created"
    AlarmStatus.ACTIVE -> "Monitoring"
    AlarmStatus.PAUSED -> "Paused"
    AlarmStatus.WARNING_TRIGGERED -> "Approaching"
    AlarmStatus.DESTINATION_REACHED -> "Reached"
    AlarmStatus.COMPLETED -> "Completed"
    AlarmStatus.CANCELLED -> "Cancelled"
}
