package com.geoalarm.app.location

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.geoalarm.app.GeoAlarmApp
import com.geoalarm.app.alarm.AlarmAudioController
import com.geoalarm.app.domain.engine.ArrivalDetectionEngine
import com.geoalarm.app.domain.engine.ArrivalEvent
import com.geoalarm.app.domain.model.AlarmStatus
import com.geoalarm.app.domain.model.GeoAlarm
import com.geoalarm.app.notification.NotificationChannels
import com.geoalarm.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * The single foreground service that watches location for every active alarm (Section 13:
 * "do not start one separate foreground service for every alarm"). It:
 *  - subscribes to the repository for the list of ACTIVE/WARNING_TRIGGERED alarms,
 *  - requests location updates at a frequency driven by whichever alarm is most urgent
 *    (Section 10 / 15B adaptive frequency),
 *  - runs every reading through [ArrivalDetectionEngine] per alarm,
 *  - persists status transitions and fires notifications/audio,
 *  - stops itself the moment there is nothing left to monitor.
 */
class LocationMonitoringService : Service() {

    private lateinit var scope: CoroutineScope
    private lateinit var locationClient: LocationClient
    private lateinit var notificationHelper: NotificationHelper
    private val engine = ArrivalDetectionEngine()

    private val frequency = MutableStateFlow(UpdateFrequency.NORMAL)

    @Volatile private var currentAlarms: List<GeoAlarm> = emptyList()
    private var alarmsJob: Job? = null
    private var locationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        locationClient = LocationClient(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
        NotificationChannels.ensureCreated(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            NotificationHelper.MONITORING_NOTIFICATION_ID,
            notificationHelper.monitoringNotification(0, null),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
        startWatchingAlarms()
        return START_STICKY
    }

    private fun startWatchingAlarms() {
        if (alarmsJob != null) return
        val repository = (application as GeoAlarmApp).container.alarmRepository
        alarmsJob = repository.observeMonitorable()
            .onEach { alarms ->
                currentAlarms = alarms
                if (alarms.isEmpty()) {
                    Log.i(TAG, "No monitorable alarms left; stopping service")
                    stopMonitoringAndSelf()
                    return@onEach
                }
                updateNotification(alarms)
                startLocationUpdatesIfNeeded()
            }
            .launchIn(scope)
    }

    private fun startLocationUpdatesIfNeeded() {
        if (locationJob != null) return
        locationJob = frequency
            .flatMapLatest { freq -> locationClient.observe(freq) }
            .onEach { reading -> handleReading(reading) }
            .launchIn(scope)
    }

    private suspend fun handleReading(reading: com.geoalarm.app.domain.engine.LocationReading) {
        val repository = (application as GeoAlarmApp).container.alarmRepository
        val alarms = currentAlarms
        if (alarms.isEmpty()) return

        var nearestDistance = Double.MAX_VALUE
        var nearestAlarm: GeoAlarm? = null

        for (alarm in alarms) {
            val (status, event) = engine.onLocationUpdate(alarm, alarm.status, reading)
            if (status.distanceMeters < nearestDistance) {
                nearestDistance = status.distanceMeters
                nearestAlarm = alarm
            }
            when (event) {
                is ArrivalEvent.WarningTriggered -> {
                    repository.markWarningTriggered(alarm.id)
                    notificationHelper.showWarning(alarm, event.distanceMeters)
                }
                is ArrivalEvent.DestinationReached -> {
                    repository.markDestinationReached(alarm.id)
                    engine.forget(alarm.id)
                    notificationHelper.clearWarning(alarm.id)
                    notificationHelper.showArrivalAlarm(alarm)
                    AlarmAudioController.start(applicationContext, alarm)
                }
                null -> Unit
            }
        }

        val nearest = nearestAlarm
        if (nearest != null) {
            val newFrequency = UpdateFrequency.forDistance(nearestDistance, nearest.warningDistanceMeters, nearest.arrivalRadiusMeters)
            if (newFrequency != frequency.value) frequency.value = newFrequency
            updateNotification(alarms, nearest, nearestDistance)
        }
    }

    private fun updateNotification(alarms: List<GeoAlarm>, nearest: GeoAlarm? = null, distanceMeters: Double? = null) {
        val description = if (nearest != null && distanceMeters != null) {
            "${NotificationHelper.formatDistance(distanceMeters)} from ${nearest.destinationName}"
        } else null
        val notification = notificationHelper.monitoringNotification(alarms.size, description)
        (getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager)
            ?.notify(NotificationHelper.MONITORING_NOTIFICATION_ID, notification)
    }

    private fun stopMonitoringAndSelf() {
        locationJob?.cancel()
        locationJob = null
        ServiceCompat.stopForeground(this, Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        alarmsJob?.cancel()
        locationJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationMonitoring"
    }
}
