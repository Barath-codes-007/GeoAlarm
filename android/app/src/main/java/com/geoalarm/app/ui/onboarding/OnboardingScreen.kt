package com.geoalarm.app.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class OnboardingPage(val icon: ImageVector, val title: String, val body: String)

private val pages = listOf(
    OnboardingPage(Icons.Filled.Place, "Meet GeoAlarm", "A location-based alarm that wakes you up right before you need to get off — a train, a bus, or your own drive."),
    OnboardingPage(Icons.Filled.NearMe, "Choose any destination", "Search for a place, tap the map, or reuse a saved place. GeoAlarm works anywhere in the world."),
    OnboardingPage(Icons.Filled.NotificationsActive, "Get warned before you arrive", "Pick how early you want a heads-up — 500 m, 250 m, 100 m, or right at arrival."),
    OnboardingPage(Icons.Filled.LocationOn, "Allow location access", "Next, GeoAlarm will explain each permission it needs and why, one at a time.")
)

@Composable
fun OnboardingScreen(onDone: () -> Unit, onSkip: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val current = pages[page]

    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) { Text("Skip") }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pages.indices.forEach { i -> Dot(active = i == page) }
                    }
                    Button(onClick = { if (page < pages.lastIndex) page += 1 else onDone() }) {
                        Text(if (page < pages.lastIndex) "Next" else "Get started")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(current.icon, contentDescription = null, modifier = Modifier.height(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text(current.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(current.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Dot(active: Boolean) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.size(if (active) 10.dp else 8.dp).clip(CircleShape)) {
        drawCircle(color = color)
    }
}
