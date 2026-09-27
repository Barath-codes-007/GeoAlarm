package com.geoalarm.app.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** One row in the permission onboarding flow (Section 11). Each step is requested
 * separately, with an explanation shown before the system dialog. */
enum class PermissionStep(
    val manifestPermission: String?,
    val title: String,
    val rationale: String,
    /** Some "permissions" are really settings screens (background location on some OEMs
     * can only be granted from Settings once foreground location is already granted, and
     * exact-alarm/notification-policy screens are settings, not runtime permissions). */
    val isSettingsRedirect: Boolean = false
) {
    PRECISE_LOCATION(
        Manifest.permission.ACCESS_FINE_LOCATION,
        "Location access",
        "GeoAlarm needs location access to know when you are approaching your destination. " +
            "This is used only to measure distance — it is not shared or uploaded."
    ),
    BACKGROUND_LOCATION(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        "Location access — all the time",
        "To keep watching your destination while GeoAlarm is closed or your screen is off, " +
            "Android needs \"Allow all the time\" instead of \"Allow only while using the app\". " +
            "You can skip this, but alarms will only run while GeoAlarm is open."
    ),
    NOTIFICATIONS(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
        "Notifications",
        "GeoAlarm uses notifications to warn you before you arrive, to ring the arrival alarm, " +
            "and to show that it's actively monitoring."
    );

    companion object {
        /** Steps relevant on the current OS version, in the order they should be requested. */
        fun applicableSteps(): List<PermissionStep> = entries.filter {
            it.manifestPermission != null || it.isSettingsRedirect
        }
    }
}

object PermissionChecks {
    fun isGranted(context: Context, step: PermissionStep): Boolean {
        val permission = step.manifestPermission ?: return true
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCoreLocationAndNotifications(context: Context): Boolean =
        isGranted(context, PermissionStep.PRECISE_LOCATION) && isGranted(context, PermissionStep.NOTIFICATIONS)

    fun hasBackgroundLocation(context: Context): Boolean = isGranted(context, PermissionStep.BACKGROUND_LOCATION)
}
