package com.geoalarm.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.geoalarm.app.GeoAlarmApp
import com.geoalarm.app.alarm.AlarmAudioController
import com.geoalarm.app.location.LocationServiceCommands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the "Stop" action on the arrival alarm notification and the "Cancel alarm"
 * action on the warning notification, without needing to open the app. */
class AlarmNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return
        val app = context.applicationContext as GeoAlarmApp
        val notifications = NotificationHelper(context)

        when (intent.action) {
            ACTION_STOP_ALARM -> {
                AlarmAudioController.stop()
                notifications.clearArrival(alarmId)
                CoroutineScope(Dispatchers.IO).launch {
                    app.container.alarmRepository.complete(alarmId)
                }
            }
            ACTION_CANCEL_ALARM -> {
                notifications.clearWarning(alarmId)
                CoroutineScope(Dispatchers.IO).launch {
                    app.container.alarmRepository.cancel(alarmId)
                    LocationServiceCommands.refresh(context)
                }
            }
        }
    }
}
