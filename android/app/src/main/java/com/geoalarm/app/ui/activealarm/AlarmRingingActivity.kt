package com.geoalarm.app.ui.activealarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.geoalarm.app.GeoAlarmApp
import com.geoalarm.app.alarm.AlarmAudioController
import com.geoalarm.app.data.repository.ThemeMode
import com.geoalarm.app.notification.NotificationHelper
import com.geoalarm.app.ui.theme.GeoAlarmTheme
import kotlinx.coroutines.launch

/** Section 7 arrival screen. Shown full-screen, over the lock screen, with looping alarm
 * audio already started by the foreground service before this activity launches. */
class AlarmRingingActivity : ComponentActivity() {

    private var alarmId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmId = intent.getLongExtra(com.geoalarm.app.notification.EXTRA_ALARM_ID, -1L)
        showOverLockScreen()

        val container = (application as GeoAlarmApp).container
        setContent {
            val theme by container.settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            GeoAlarmTheme(theme) {
                val alarmFlow = container.alarmRepository.observeById(alarmId).collectAsState(initial = null)
                val alarm = alarmFlow.value
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🚨", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "DESTINATION REACHED",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            alarm?.destinationName ?: "Your destination",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                        alarm?.note?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.height(40.dp))
                        Text("You have reached your destination.", color = MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(40.dp))
                        Button(onClick = { stopAndFinish() }) {
                            Text("STOP ALARM")
                        }
                    }
                }
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun stopAndFinish() {
        AlarmAudioController.stop()
        NotificationHelper(this).clearArrival(alarmId)
        lifecycleScope.launch {
            (application as GeoAlarmApp).container.alarmRepository.complete(alarmId)
        }
        finish()
    }

    override fun onDestroy() {
        // Safety net: if the user backs out without pressing Stop, do not leave audio looping.
        if (isFinishing) AlarmAudioController.stop()
        super.onDestroy()
    }
}
