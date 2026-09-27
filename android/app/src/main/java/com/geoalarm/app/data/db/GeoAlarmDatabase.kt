package com.geoalarm.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.geoalarm.app.data.db.dao.AlarmHistoryDao
import com.geoalarm.app.data.db.dao.GeoAlarmDao
import com.geoalarm.app.data.db.dao.SavedPlaceDao
import com.geoalarm.app.data.db.entity.AlarmHistoryEntity
import com.geoalarm.app.data.db.entity.GeoAlarmEntity
import com.geoalarm.app.data.db.entity.SavedPlaceEntity

@Database(
    entities = [GeoAlarmEntity::class, SavedPlaceEntity::class, AlarmHistoryEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GeoAlarmDatabase : RoomDatabase() {
    abstract fun geoAlarmDao(): GeoAlarmDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun alarmHistoryDao(): AlarmHistoryDao

    companion object {
        @Volatile private var instance: GeoAlarmDatabase? = null

        /**
         * Schema history: bump `version` and add a `Migration` object here for every future
         * change. Never use `fallbackToDestructiveMigration()` — Section 12 requires user
         * data to survive app upgrades.
         */
        fun getInstance(context: Context): GeoAlarmDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GeoAlarmDatabase::class.java,
                    "geoalarm.db"
                ).build().also { instance = it }
            }
    }
}
