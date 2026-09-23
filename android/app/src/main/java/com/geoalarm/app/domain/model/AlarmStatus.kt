package com.geoalarm.app.domain.model

/**
 * Lifecycle of one GeoAlarm.
 *
 * ```
 * CREATED -> ACTIVE -> WARNING_TRIGGERED -> DESTINATION_REACHED -> COMPLETED
 *                \-> PAUSED -> ACTIVE
 *                \-> CANCELLED
 * ```
 *
 * WARNING_TRIGGERED is skipped when [GeoAlarm.warningDistanceMeters] is null (an
 * "at destination"-only alarm) or is not larger than the arrival radius.
 */
enum class AlarmStatus {
    CREATED,
    ACTIVE,
    PAUSED,
    WARNING_TRIGGERED,
    DESTINATION_REACHED,
    COMPLETED,
    CANCELLED;

    val isMonitorable: Boolean
        get() = this == ACTIVE || this == WARNING_TRIGGERED

    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED
}

enum class WarningDistancePreset(val meters: Int?, val label: String) {
    FIVE_HUNDRED(500, "500 m before"),
    TWO_HUNDRED_FIFTY(250, "250 m before"),
    ONE_HUNDRED(100, "100 m before"),
    AT_DESTINATION(null, "At destination");

    companion object {
        /** Supports a future custom distance without changing the schema: any positive value not
         * matching a preset is simply shown as "Custom (<n> m)" by the UI layer. */
        fun fromMeters(meters: Int?): WarningDistancePreset =
            entries.firstOrNull { it.meters == meters } ?: AT_DESTINATION
    }
}

enum class ArrivalRadiusPreset(val meters: Int) {
    TIGHT(50),
    STANDARD(100),
    RELAXED(200);

    companion object {
        val default = STANDARD
    }
}

enum class AlarmSoundType {
    DEFAULT_RINGTONE,
    CUSTOM_AUDIO,
    NOTIFICATION_SOUND,
    VIBRATION_ONLY,
    SILENT
}
