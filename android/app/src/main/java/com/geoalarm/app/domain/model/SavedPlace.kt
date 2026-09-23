package com.geoalarm.app.domain.model

data class SavedPlace(
    val id: Long = 0,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val icon: PlaceIcon = PlaceIcon.CUSTOM,
    val createdAt: Long
)

enum class PlaceIcon { HOME, WORK, SCHOOL, STATION, AIRPORT, CUSTOM }

/** A completed or cancelled alarm, kept for the History screen. */
data class AlarmHistoryEntry(
    val id: Long = 0,
    val destinationName: String,
    val latitude: Double,
    val longitude: Double,
    val warningDistanceMeters: Int?,
    val finalStatus: AlarmStatus,
    val createdAt: Long,
    val activatedAt: Long?,
    val completedAt: Long?
)

/** A recently used destination, shown on the home/create-alarm screens. */
data class RecentDestination(
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val lastUsedAt: Long
)
