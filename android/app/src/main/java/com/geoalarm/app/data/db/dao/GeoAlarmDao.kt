package com.geoalarm.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.geoalarm.app.data.db.entity.GeoAlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeoAlarmDao {

    @Query("SELECT * FROM geo_alarms ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GeoAlarmEntity>>

    /** Alarms whose location must be actively monitored (Section 13: only these use
     * background location resources). */
    @Query("SELECT * FROM geo_alarms WHERE status IN ('ACTIVE', 'WARNING_TRIGGERED')")
    fun observeMonitorable(): Flow<List<GeoAlarmEntity>>

    @Query("SELECT * FROM geo_alarms WHERE status IN ('ACTIVE', 'WARNING_TRIGGERED')")
    suspend fun getMonitorableOnce(): List<GeoAlarmEntity>

    @Query("SELECT * FROM geo_alarms WHERE id = :id")
    suspend fun getById(id: Long): GeoAlarmEntity?

    @Query("SELECT * FROM geo_alarms WHERE id = :id")
    fun observeById(id: Long): Flow<GeoAlarmEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: GeoAlarmEntity): Long

    @Update
    suspend fun update(entity: GeoAlarmEntity)

    @Query("UPDATE geo_alarms SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: com.geoalarm.app.domain.model.AlarmStatus)

    @Query(
        "UPDATE geo_alarms SET status = :status, warningTriggeredAt = :at WHERE id = :id"
    )
    suspend fun markWarningTriggered(id: Long, status: com.geoalarm.app.domain.model.AlarmStatus, at: Long)

    @Query(
        "UPDATE geo_alarms SET status = :status, completedAt = :at WHERE id = :id"
    )
    suspend fun markCompleted(id: Long, status: com.geoalarm.app.domain.model.AlarmStatus, at: Long)

    @Query("UPDATE geo_alarms SET status = :status, activatedAt = :at WHERE id = :id")
    suspend fun activate(id: Long, status: com.geoalarm.app.domain.model.AlarmStatus, at: Long)

    @Query("DELETE FROM geo_alarms WHERE id = :id")
    suspend fun deleteById(id: Long)
}
