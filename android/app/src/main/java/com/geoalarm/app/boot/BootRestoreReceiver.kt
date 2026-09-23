package com.geoalarm.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.geoalarm.app.GeoAlarmApp
import com.geoalarm.app.location.LocationServiceCommands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Section 14: after a reboot, resume monitoring only if (a) there are alarms that were
 * actively monitorable before shutdown, and (b) the OS actually delivers BOOT_COMPLETED to
 * us — which some manufacturers restrict unless the user has allowed "autostart" for the
 * app (documented in docs/testing.md and the website's background-limits section). This
 * receiver does not, and cannot, guarantee recovery on every device.
 */
class BootRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val app = context.applicationContext as GeoAlarmApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val monitorable = app.container.alarmRepository.getMonitorableOnce()
                if (monitorable.isNotEmpty()) {
                    Log.i(TAG, "Resuming monitoring for ${monitorable.size} alarm(s) after boot")
                    ContextCompat.getMainExecutor(context).execute {
                        LocationServiceCommands.refresh(context)
                    }
                } else {
                    Log.i(TAG, "No monitorable alarms after boot; nothing to resume")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootRestoreReceiver"
    }
}
