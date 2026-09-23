package com.geoalarm.app.domain.model

/**
 * A single destination alarm. This is the domain-layer representation used by the
 * location engine and UI; [com.geoalarm.app.data.db.entity.GeoAlarmEntity] is its
 * persisted form.
 */
data class GeoAlarm(
    val id: Long = 0,
    val destinationName: String,
    val latitude: Double,
    val longitude: Double,
    /** Null means "at destination" (warn and ring at the same time). */
    val warningDistanceMeters: Int?,
    val arrivalRadiusMeters: Int = ArrivalRadiusPreset.default.meters,
    val soundType: AlarmSoundType,
    /** content:// URI string, only meaningful when [soundType] is CUSTOM_AUDIO. */
    val soundUri: String? = null,
    val note: String? = null,
    val status: AlarmStatus = AlarmStatus.CREATED,
    val createdAt: Long,
    val activatedAt: Long? = null,
    val warningTriggeredAt: Long? = null,
    val completedAt: Long? = null
) {
    init {
        require(destinationName.isNotBlank()) { "destinationName must not be blank" }
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
        require(warningDistanceMeters == null || warningDistanceMeters > 0) {
            "warningDistanceMeters must be positive or null"
        }
        require(arrivalRadiusMeters > 0) { "arrivalRadiusMeters must be positive" }
    }

    /** A separate warning notification only makes sense if it fires meaningfully before arrival. */
    val hasSeparateWarning: Boolean
        get() = warningDistanceMeters != null && warningDistanceMeters > arrivalRadiusMeters
}
