package com.geoalarm.app

import android.app.Application
import com.geoalarm.app.data.db.GeoAlarmDatabase
import com.geoalarm.app.data.repository.AlarmRepository
import com.geoalarm.app.data.repository.HistoryRepository
import com.geoalarm.app.data.repository.SavedPlaceRepository
import com.geoalarm.app.data.repository.SettingsRepository
import com.geoalarm.app.map.GeocoderProvider
import com.geoalarm.app.map.NominatimGeocoderProvider
import com.geoalarm.app.notification.NotificationChannels

/**
 * Manual, lightweight dependency container. The app is intentionally small enough that a
 * DI framework (Hilt/Koin) would add more ceremony than value; [AppContainer] is the single
 * seam to swap in one later without touching call sites, which mostly go through
 * `(application as GeoAlarmApp).container`.
 */
class AppContainer(app: Application) {
    private val database by lazy { GeoAlarmDatabase.getInstance(app) }

    val alarmRepository by lazy { AlarmRepository(database.geoAlarmDao(), database.alarmHistoryDao()) }
    val savedPlaceRepository by lazy { SavedPlaceRepository(database.savedPlaceDao()) }
    val historyRepository by lazy { HistoryRepository(database.alarmHistoryDao()) }
    val settingsRepository by lazy { SettingsRepository(app) }
    val geocoderProvider: GeocoderProvider by lazy { NominatimGeocoderProvider() }
}

class GeoAlarmApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.ensureCreated(this)
    }
}
