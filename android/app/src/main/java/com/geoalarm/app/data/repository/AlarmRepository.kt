package com.geoalarm.app.data.repository

import com.geoalarm.app.data.db.dao.AlarmHistoryDao
import com.geoalarm.app.data.db.dao.GeoAlarmDao
import com.geoalarm.app.data.db.entity.AlarmHistoryEntity
import com.geoalarm.app.data.db.entity.toDomain
import com.geoalarm.app.data.db.entity.toEntity
import com.geoalarm.app.domain.model.AlarmStatus
import com.geoalarm.app.domain.model.GeoAlarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for alarm data and the only place that writes an
 * [AlarmStatus] transition, so the state machine described in domain/model/AlarmStatus.kt
 * is enforced in one place rather than scattered across the UI and the location service.
 */
class AlarmRepository(
    private val alarmDao: GeoAlarmDao,
    private val historyDao: AlarmHistoryDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    fun observeAll(): Flow<List<GeoAlarm>> = alarmDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeMonitorable(): Flow<List<GeoAlarm>> =
        alarmDao.observeMonitorable().map { list -> list.map { it.toDomain() } }

    fun observeById(id: Long): Flow<GeoAlarm?> = alarmDao.observeById(id).map { it?.toDomain() }

    suspend fun getMonitorableOnce(): List<GeoAlarm> = alarmDao.getMonitorableOnce().map { it.toDomain() }

    suspend fun getById(id: Long): GeoAlarm? = alarmDao.getById(id)?.toDomain()

    suspend fun create(alarm: GeoAlarm): Long = alarmDao.insert(alarm.toEntity())

    suspend fun activate(id: Long) {
        alarmDao.activate(id, AlarmStatus.ACTIVE, clock())
    }

    suspend fun pause(id: Long) {
        alarmDao.updateStatus(id, AlarmStatus.PAUSED)
    }

    suspend fun resume(id: Long) {
        alarmDao.updateStatus(id, AlarmStatus.ACTIVE)
    }

    suspend fun markWarningTriggered(id: Long) {
        alarmDao.markWarningTriggered(id, AlarmStatus.WARNING_TRIGGERED, clock())
    }

    suspend fun markDestinationReached(id: Long) {
        alarmDao.markCompleted(id, AlarmStatus.DESTINATION_REACHED, clock())
    }

    /** Called once the user dismisses/stops a ringing alarm: moves it to COMPLETED and archives it. */
    suspend fun complete(id: Long) {
        val alarm = alarmDao.getById(id) ?: return
        alarmDao.markCompleted(id, AlarmStatus.COMPLETED, clock())
        archive(alarm.toDomain().copy(status = AlarmStatus.COMPLETED, completedAt = clock()))
    }

    suspend fun cancel(id: Long) {
        val alarm = alarmDao.getById(id) ?: return
        alarmDao.updateStatus(id, AlarmStatus.CANCELLED)
        archive(alarm.toDomain().copy(status = AlarmStatus.CANCELLED, completedAt = clock()))
    }

    suspend fun delete(id: Long) {
        alarmDao.deleteById(id)
    }

    suspend fun update(alarm: GeoAlarm) {
        alarmDao.update(alarm.toEntity())
    }

    private suspend fun archive(alarm: GeoAlarm) {
        historyDao.insert(
            AlarmHistoryEntity(
                destinationName = alarm.destinationName,
                latitude = alarm.latitude,
                longitude = alarm.longitude,
                warningDistanceMeters = alarm.warningDistanceMeters,
                finalStatus = alarm.status,
                createdAt = alarm.createdAt,
                activatedAt = alarm.activatedAt,
                completedAt = alarm.completedAt
            )
        )
    }
}
