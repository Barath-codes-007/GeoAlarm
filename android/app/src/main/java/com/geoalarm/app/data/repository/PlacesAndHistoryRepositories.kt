package com.geoalarm.app.data.repository

import com.geoalarm.app.data.db.dao.AlarmHistoryDao
import com.geoalarm.app.data.db.dao.SavedPlaceDao
import com.geoalarm.app.data.db.entity.toDomain
import com.geoalarm.app.data.db.entity.toEntity
import com.geoalarm.app.domain.model.AlarmHistoryEntry
import com.geoalarm.app.domain.model.RecentDestination
import com.geoalarm.app.domain.model.SavedPlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedPlaceRepository(private val dao: SavedPlaceDao) {
    fun observeAll(): Flow<List<SavedPlace>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    suspend fun save(place: SavedPlace): Long = dao.insert(place.toEntity())
    suspend fun delete(id: Long) = dao.deleteById(id)
}

class HistoryRepository(private val dao: AlarmHistoryDao) {
    fun observeRecent(limit: Int = 200): Flow<List<AlarmHistoryEntry>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    fun observeRecentDestinations(limit: Int = 10): Flow<List<RecentDestination>> =
        dao.observeRecentDestinations(limit).map { rows ->
            rows.map { RecentDestination(it.destinationName, it.latitude, it.longitude, it.createdAt) }
        }

    suspend fun clear() = dao.clearAll()
}
