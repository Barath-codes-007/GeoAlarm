package com.geoalarm.app.ui.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Walks through [PermissionStep.applicableSteps] one at a time. Each step shows the
 * explanation first (Section 11: "never request unnecessary permissions... explain why").
 * Background location is presented as skippable, since GeoAlarm remains useful (foreground
 * only) without it. Denials — including "permanently denied" — are handled by simply
 * moving on rather than blocking the user; MainActivity re-checks permissions each time
 * the app returns to the foreground so the user can grant them later from Settings.
 */
@Composable
fun PermissionFlowScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var stepIndex by remember { mutableIntStateOf(0) }
    val steps = remember { PermissionStep.applicableSteps() }
    var awaitingSystemDialog by remember { mutableStateOf(false) }

    if (steps.isEmpty() || stepIndex >= steps.size) {
        onFinished()
        return
    }
    val step = steps[stepIndex]

    val singleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        stepIndex += 1
    }

    fun advanceOrOpenSettings() {
        when (step) {
            PermissionStep.BACKGROUND_LOCATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ requires background location to be granted from Settings,
                    // not a runtime dialog, once foreground location is already granted.
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                    stepIndex += 1
                } else {
                    step.manifestPermission?.let { singleLauncher.launch(it) } ?: run { stepIndex += 1 }
                }
            }
            else -> step.manifestPermission?.let { singleLauncher.launch(it) } ?: run { stepIndex += 1 }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (step == PermissionStep.NOTIFICATIONS) Icons.Filled.Notifications else Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.height(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text(step.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                step.rationale,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = { advanceOrOpenSettings() }) {
                Text(if (step == PermissionStep.BACKGROUND_LOCATION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "Open settings" else "Continue")
            }
            if (step == PermissionStep.BACKGROUND_LOCATION) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { stepIndex += 1 }) { Text("Skip for now") }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Step ${stepIndex + 1} of ${steps.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
