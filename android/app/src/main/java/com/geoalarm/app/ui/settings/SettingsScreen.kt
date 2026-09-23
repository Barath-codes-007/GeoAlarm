package com.geoalarm.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoalarm.app.data.repository.SettingsRepository
import com.geoalarm.app.data.repository.ThemeMode
import com.geoalarm.app.ui.common.rememberContainerViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val theme = repository.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val reverseGeocode = repository.reverseGeocodeTappedPoints.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
    fun setReverseGeocode(enabled: Boolean) = viewModelScope.launch { repository.setReverseGeocodeTappedPoints(enabled) }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = rememberContainerViewModel { SettingsViewModel(it.settingsRepository) }
    val theme by viewModel.theme.collectAsState()
    val reverseGeocode by viewModel.reverseGeocode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("APPEARANCE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(ThemeMode.SYSTEM to "System default", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark").forEach { (mode, label) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = theme == mode, onClick = { viewModel.setTheme(mode) })
                    Text(label)
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))
            Text("PRIVACY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Look up an address when I tap the map")
                    Text(
                        "Sends the tapped point to the configured geocoding service. Turn this off to keep tapped points as \"Dropped pin\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = reverseGeocode, onCheckedChange = { viewModel.setReverseGeocode(it) })
            }
        }
    }
}
