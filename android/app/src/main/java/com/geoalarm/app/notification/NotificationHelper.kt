package com.geoalarm.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.geoalarm.app.MainActivity
import com.geoalarm.app.R
import com.geoalarm.app.domain.model.GeoAlarm
import com.geoalarm.app.ui.activealarm.AlarmRingingActivity

object NotificationChannels {
    const val MONITORING = "geoalarm_monitoring"
    const val WARNING = "geoalarm_warning"
    const val ALARM = "geoalarm_alarm"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(MONITORING, "Monitoring", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows while GeoAlarm is watching your location for an active alarm."
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(WARNING, "Approach warnings", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Tells you when you're getting close to a destination."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(ALARM, "Arrival alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "The alarm that rings when you reach your destination."
                setBypassDnd(true)
            }
        )
    }
}

const val ACTION_STOP_ALARM = "com.geoalarm.app.action.STOP_ALARM"
const val ACTION_CANCEL_ALARM = "com.geoalarm.app.action.CANCEL_ALARM"
const val ACTION_VIEW_ALARM = "com.geoalarm.app.action.VIEW_ALARM"
const val EXTRA_ALARM_ID = "extra_alarm_id"

class NotificationHelper(private val context: Context) {
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    private fun contentIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getActivity(
            context, alarmId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(action: String, alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmNotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context, (action + alarmId).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Persistent, low-priority notification required to run the foreground service (Section 10). */
    fun monitoringNotification(activeCount: Int, nearestDescription: String?): Notification {
        val text = when {
            activeCount == 0 -> "No active destinations"
            nearestDescription != null -> nearestDescription
            else -> "Monitoring $activeCount destination${if (activeCount == 1) "" else "s"}"
        }
        return NotificationCompat.Builder(context, NotificationChannels.MONITORING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("GeoAlarm is monitoring your destination")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun showWarning(alarm: GeoAlarm, distanceMeters: Double) {
        val distanceLabel = formatDistance(distanceMeters)
        val text = buildString {
            append("You are approximately $distanceLabel from ${alarm.destinationName}.")
            alarm.note?.takeIf { it.isNotBlank() }?.let { append(" ") ; append(it) }
        }
        val notification = NotificationCompat.Builder(context, NotificationChannels.WARNING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Approaching ${alarm.destinationName}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(alarm.id))
            .addAction(0, "Cancel alarm", actionIntent(ACTION_CANCEL_ALARM, alarm.id))
            .build()
        manager?.notify(warningNotificationId(alarm.id), notification)
    }

    /** High-priority + full-screen intent so the alarm shows even over the lock screen. */
    fun showArrivalAlarm(alarm: GeoAlarm) {
        val fullScreenIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, alarm.id.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.ALARM)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Destination reached")
            .setContentText(alarm.destinationName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .addAction(0, "Stop", actionIntent(ACTION_STOP_ALARM, alarm.id))
            .build()
        manager?.notify(alarmNotificationId(alarm.id), notification)
    }

    fun clearWarning(alarmId: Long) = manager?.cancel(warningNotificationId(alarmId))
    fun clearArrival(alarmId: Long) = manager?.cancel(alarmNotificationId(alarmId))

    companion object {
        const val MONITORING_NOTIFICATION_ID = 1
        fun warningNotificationId(alarmId: Long) = (2_000_000_000L + alarmId).toInt()
        fun alarmNotificationId(alarmId: Long) = (3_000_000_000L % Int.MAX_VALUE + alarmId).toInt()

        fun formatDistance(meters: Double): String = if (meters >= 1000) {
            "%.1f km".format(meters / 1000)
        } else {
            "${(meters / 10).toInt() * 10} m"
        }
    }
}
