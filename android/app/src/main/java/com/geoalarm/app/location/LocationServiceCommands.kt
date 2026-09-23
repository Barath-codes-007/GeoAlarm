package com.geoalarm.app.location

import android.content.Context
import android.content.Intent
import android.os.Build

/** Small helpers so UI code doesn't need to know Intent/Service plumbing details. */
object LocationServiceCommands {
    /** Start (or nudge) the monitoring service — safe to call even if it is already running,
     * and safe to call when there happen to be zero monitorable alarms (the service checks
     * and stops itself immediately in that case, per Section 9 "do not create a permanently
     * running location service when the user has no active GeoAlarm"). */
    fun refresh(context: Context) {
        val intent = Intent(context, LocationMonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, LocationMonitoringService::class.java))
    }
}
