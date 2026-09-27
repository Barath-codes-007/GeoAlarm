package com.geoalarm.app.ui.history

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoalarm.app.data.repository.HistoryRepository
import com.geoalarm.app.domain.model.AlarmHistoryEntry
import com.geoalarm.app.domain.model.AlarmStatus
import com.geoalarm.app.domain.model.WarningDistancePreset
import com.geoalarm.app.ui.common.rememberContainerViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {
    val entries = repository.observeRecent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun clear() = viewModelScope.launch { repository.clear() }
}

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val viewModel = rememberContainerViewModel { HistoryViewModel(it.historyRepository) }
    val entries by viewModel.entries.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarm History") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (entries.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirm = true }) { Text("Clear") }
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(32.dp)) {
                Text("No history yet.", style = MaterialTheme.typography.titleMedium)
                Text("Completed and cancelled alarms will show up here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(entries, key = { it.id }) { entry -> HistoryCard(entry); Spacer(Modifier.height(10.dp)) }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear history?") },
            text = { Text("This removes all past alarm records. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { viewModel.clear(); showClearConfirm = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HistoryCard(entry: AlarmHistoryEntry) {
    val formatter = remember { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.destinationName, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (entry.finalStatus == AlarmStatus.COMPLETED) "Completed" else "Cancelled",
                    color = if (entry.finalStatus == AlarmStatus.COMPLETED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
            }
            Text(
                formatter.format(Date(entry.completedAt ?: entry.createdAt)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Warning: ${WarningDistancePreset.fromMeters(entry.warningDistanceMeters).label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
