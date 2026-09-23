package com.geoalarm.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.geoalarm.app.domain.model.AlarmHistoryEntry
import com.geoalarm.app.domain.model.AlarmStatus
import com.geoalarm.app.domain.model.PlaceIcon
import com.geoalarm.app.domain.model.SavedPlace

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val icon: PlaceIcon,
    val createdAt: Long
)

fun SavedPlaceEntity.toDomain() = SavedPlace(id, label, latitude, longitude, icon, createdAt)
fun SavedPlace.toEntity() = SavedPlaceEntity(id, label, latitude, longitude, icon, createdAt)

@Entity(tableName = "alarm_history")
data class AlarmHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val destinationName: String,
    val latitude: Double,
    val longitude: Double,
    val warningDistanceMeters: Int?,
    val finalStatus: AlarmStatus,
    val createdAt: Long,
    val activatedAt: Long?,
    val completedAt: Long?
)

fun AlarmHistoryEntity.toDomain() = AlarmHistoryEntry(
    id, destinationName, latitude, longitude, warningDistanceMeters, finalStatus, createdAt, activatedAt, completedAt
)

/** Settings are stored via DataStore (see data/repository/SettingsRepository.kt), not Room —
 * they are simple key/value preferences, not records with an identity or history. */
