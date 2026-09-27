package com.geoalarm.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.geoalarm.app.data.db.entity.AlarmHistoryEntity
import com.geoalarm.app.data.db.entity.SavedPlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedPlaceEntity>>

    @Insert
    suspend fun insert(entity: SavedPlaceEntity): Long

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AlarmHistoryDao {
    @Query("SELECT * FROM alarm_history ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<AlarmHistoryEntity>>

    @Insert
    suspend fun insert(entity: AlarmHistoryEntity): Long

    @Query("DELETE FROM alarm_history")
    suspend fun clearAll()

    /** Recently used distinct destinations, most recent first (Section 15F). */
    @Query(
        """
        SELECT destinationName, latitude, longitude, MAX(createdAt) as createdAt
        FROM alarm_history
        GROUP BY destinationName, latitude, longitude
        ORDER BY createdAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentDestinations(limit: Int = 10): Flow<List<RecentDestinationRow>>
}

/** Projection for the GROUP BY query above; mapped to [com.geoalarm.app.domain.model.RecentDestination]. */
data class RecentDestinationRow(
    val destinationName: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long
)
