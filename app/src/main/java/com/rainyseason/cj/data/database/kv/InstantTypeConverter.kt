package com.rainyseason.cj.data.database.kv

import androidx.room.TypeConverter
import org.threeten.bp.Instant

class InstantTypeConverter {

    @TypeConverter
    fun longToInstant(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(value) }
    }

    @TypeConverter
    fun instantToLong(value: Instant?): Long? {
        return value?.toEpochMilli()
    }
}
