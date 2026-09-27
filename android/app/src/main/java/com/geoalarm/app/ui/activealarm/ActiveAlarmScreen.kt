package com.geoalarm.app.ui.activealarm

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geoalarm.app.notification.NotificationHelper
import com.geoalarm.app.ui.common.rememberContainerViewModel

@Composable
fun ActiveAlarmScreen(alarmId: Long, onBack: () -> Unit, onCancelled: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel = rememberContainerViewModel {
        ActiveAlarmViewModel(it.alarmRepository, com.geoalarm.app.location.LocationClient(context), alarmId)
    }
    val state by viewModel.uiState.collectAsState()
    val alarm = state.alarm

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitoring Destination") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            if (alarm == null) {
                Text("This alarm no longer exists.")
                return@Column
            }
            Text("📍 ${alarm.destinationName}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            val distanceText = state.distanceMeters?.let { NotificationHelper.formatDistance(it) } ?: "—"
            Text(distanceText, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text("remaining", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(20.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    StatusRow("Warning", com.geoalarm.app.domain.model.WarningDistancePreset.fromMeters(alarm.warningDistanceMeters).label)
                    StatusRow("Arrival radius", "${alarm.arrivalRadiusMeters} m")
                    StatusRow("GPS accuracy", state.accuracyMeters?.let { "±${it.toInt()} m" } ?: "Waiting for fix…")
                    StatusRow("GPS quality", state.accuracyMeters?.let { qualityLabel(it) } ?: "—")
                    StatusRow("Monitoring", if (alarm.status.isMonitorable) "● Active" else alarm.status.name)
                    alarm.note?.let { StatusRow("Note", it) }
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = { viewModel.cancel(alarmId); onCancelled() }, modifier = Modifier.fillMaxWidth()) {
                Text("CANCEL ALARM")
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun qualityLabel(accuracy: Float): String = when {
    accuracy <= 20f -> "Good"
    accuracy <= 50f -> "Fair"
    else -> "Poor"
}
