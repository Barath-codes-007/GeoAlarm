package com.geoalarm.app.data.db

import androidx.room.TypeConverter
import com.geoalarm.app.domain.model.AlarmSoundType
import com.geoalarm.app.domain.model.AlarmStatus
import com.geoalarm.app.domain.model.PlaceIcon

/** Enums are stored as their name(), not ordinal(), so reordering entries in the future
 * cannot silently corrupt existing rows. */
class Converters {
    @TypeConverter
    fun fromAlarmStatus(value: AlarmStatus): String = value.name

    @TypeConverter
    fun toAlarmStatus(value: String): AlarmStatus =
        runCatching { AlarmStatus.valueOf(value) }.getOrDefault(AlarmStatus.CANCELLED)

    @TypeConverter
    fun fromSoundType(value: AlarmSoundType): String = value.name

    @TypeConverter
    fun toSoundType(value: String): AlarmSoundType =
        runCatching { AlarmSoundType.valueOf(value) }.getOrDefault(AlarmSoundType.DEFAULT_RINGTONE)

    @TypeConverter
    fun fromPlaceIcon(value: PlaceIcon): String = value.name

    @TypeConverter
    fun toPlaceIcon(value: String): PlaceIcon =
        runCatching { PlaceIcon.valueOf(value) }.getOrDefault(PlaceIcon.CUSTOM)
}
