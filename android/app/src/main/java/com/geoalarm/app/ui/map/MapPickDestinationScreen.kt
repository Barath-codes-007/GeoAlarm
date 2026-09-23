package com.geoalarm.app.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.geoalarm.app.map.GeocodeResult
import com.geoalarm.app.map.LatLng
import com.geoalarm.app.map.MapLibreMapProvider
import com.geoalarm.app.map.MapMarker
import com.geoalarm.app.map.MapProviderEvent
import com.geoalarm.app.ui.common.rememberContainerViewModel
import kotlinx.coroutines.launch

@Composable
fun MapPickDestinationScreen(
    onDestinationConfirmed: (label: String, lat: Double, lon: Double) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = rememberContainerViewModel {
        MapPickDestinationViewModel(it.geocoderProvider, it.savedPlaceRepository, it.settingsRepository)
    }
    val state by viewModel.state.collectAsState()
    val mapProvider = remember { MapLibreMapProvider(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapProvider.onStart()
                Lifecycle.Event.ON_STOP -> mapProvider.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapProvider.onDestroy()
        }
    }

    val scope = rememberCoroutineScopeCompat()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Destination") }
            )
        },
        floatingActionButton = {
            if (!hasLocationPermission) {
                FloatingActionButton(onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Enable current location")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search destination...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true
                )
                if (state.searching) {
                    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }

            if (state.results.isNotEmpty()) {
                LazyColumn(Modifier.weight(1f)) {
                    items(state.results) { result: GeocodeResult ->
                        ListItem(
                            headlineContent = { Text(result.label) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onResultSelected(result)
                                    mapProvider.setMarker(MapMarker("destination", result.position))
                                    mapProvider.moveCamera(result.position, 14.0)
                                }
                        )
                        androidx.compose.material3.HorizontalDivider()
                    }
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(factory = { mapProvider.createView(it) }, modifier = Modifier.fillMaxSize())

                    Text(
                        mapProvider.attributionText,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                    )

                    if (hasLocationPermission) {
                        FloatingActionButton(
                            onClick = { /* handled via events()/location fetch in production build */ },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = "My location")
                        }
                    }
                }

                state.selected?.let { selection ->
                    Card(Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(selection.label, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "%.5f, %.5f".format(selection.position.latitude, selection.position.longitude),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            state.distanceMetersFromCurrent?.let { d ->
                                Text(
                                    "Distance: " + if (d >= 1000) "%.1f km".format(d / 1000) else "${d.toInt()} m",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                "Coordinates: %.5f, %.5f".format(selection.position.latitude, selection.position.longitude),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { onDestinationConfirmed(selection.label, selection.position.latitude, selection.position.longitude) },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            ) { Text("Confirm destination") }
                        }
                    }
                }
            }

            if (state.results.isEmpty() && state.savedPlaces.isNotEmpty() && state.query.isBlank()) {
                Text(
                    "Saved places",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn {
                    items(state.savedPlaces) { place ->
                        ListItem(
                            headlineContent = { Text(place.label) },
                            supportingContent = { Text("%.4f, %.4f".format(place.latitude, place.longitude)) },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // Wire map tap/camera events from the provider into the ViewModel.
    DisposableEffect(mapProvider) {
        val job = scope.launch {
            mapProvider.events().collect { event ->
                if (event is MapProviderEvent.MapTapped) {
                    viewModel.onMapTapped(event.position)
                    mapProvider.setMarker(MapMarker("destination", event.position))
                }
            }
        }
        onDispose { job.cancel() }
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()
