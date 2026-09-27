package com.geoalarm.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.geoalarm.app.domain.model.AlarmSoundType
import com.geoalarm.app.domain.model.AlarmStatus
import com.geoalarm.app.domain.model.GeoAlarm

@Entity(tableName = "geo_alarms")
data class GeoAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val destinationName: String,
    val latitude: Double,
    val longitude: Double,
    val warningDistanceMeters: Int?,
    val arrivalRadiusMeters: Int,
    val soundType: AlarmSoundType,
    val soundUri: String?,
    val note: String?,
    val status: AlarmStatus,
    val createdAt: Long,
    val activatedAt: Long?,
    val warningTriggeredAt: Long?,
    val completedAt: Long?
)

fun GeoAlarmEntity.toDomain() = GeoAlarm(
    id = id,
    destinationName = destinationName,
    latitude = latitude,
    longitude = longitude,
    warningDistanceMeters = warningDistanceMeters,
    arrivalRadiusMeters = arrivalRadiusMeters,
    soundType = soundType,
    soundUri = soundUri,
    note = note,
    status = status,
    createdAt = createdAt,
    activatedAt = activatedAt,
    warningTriggeredAt = warningTriggeredAt,
    completedAt = completedAt
)

fun GeoAlarm.toEntity() = GeoAlarmEntity(
    id = id,
    destinationName = destinationName,
    latitude = latitude,
    longitude = longitude,
    warningDistanceMeters = warningDistanceMeters,
    arrivalRadiusMeters = arrivalRadiusMeters,
    soundType = soundType,
    soundUri = soundUri,
    note = note,
    status = status,
    createdAt = createdAt,
    activatedAt = activatedAt,
    warningTriggeredAt = warningTriggeredAt,
    completedAt = completedAt
)
