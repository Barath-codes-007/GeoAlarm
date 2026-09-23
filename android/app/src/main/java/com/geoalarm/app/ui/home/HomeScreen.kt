package com.geoalarm.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geoalarm.app.domain.model.WarningDistancePreset
import com.geoalarm.app.ui.common.rememberContainerViewModel

@Composable
fun HomeScreen(
    onChooseDestination: () -> Unit,
    onMyAlarms: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onViewActiveAlarm: (Long) -> Unit
) {
    val viewModel = rememberContainerViewModel { HomeViewModel(it.alarmRepository) }
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("GeoAlarm", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Never Miss Your Destination.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("Where are you going?", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))

            val next = state.nextAlarm
            if (next != null) {
                Card(
                    onClick = { onViewActiveAlarm(next.id) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("NEXT DESTINATION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(next.destinationName, style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.height(8.dp))
                        val warnLabel = WarningDistancePreset.fromMeters(next.warningDistanceMeters).label
                        Text("Warning: $warnLabel", style = MaterialTheme.typography.bodyMedium)
                        Text("Arrival radius: ${next.arrivalRadiusMeters} m", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { onViewActiveAlarm(next.id) }) { Text("View map") }
                            OutlinedButton(onClick = { viewModel.cancelAlarm(next.id) }) { Text("Cancel alarm") }
                        }
                    }
                }
            } else if (!state.loading) {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("No active destination yet.", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("Choose a destination to create your first GeoAlarm.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onChooseDestination, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose Destination")
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onMyAlarms, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.List, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("My Alarms")
                }
                OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.History, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("History")
                }
            }
            if (state.activeCount > 1) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${state.activeCount} alarms currently active",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
