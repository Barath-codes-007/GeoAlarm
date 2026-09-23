package com.geoalarm.app.ui

/** Centralised route strings so screens never hand-write navigation paths. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions"
    const val HOME = "home"
    const val MAP_PICK_DESTINATION = "map_pick_destination"
    const val ALARM_CREATE = "alarm_create/{lat}/{lon}/{label}"
    const val MY_ALARMS = "my_alarms"
    const val ACTIVE_ALARM = "active_alarm/{alarmId}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun alarmCreate(lat: Double, lon: Double, label: String): String {
        val encoded = java.net.URLEncoder.encode(label, "UTF-8")
        return "alarm_create/$lat/$lon/$encoded"
    }

    fun activeAlarm(alarmId: Long): String = "active_alarm/$alarmId"
}
