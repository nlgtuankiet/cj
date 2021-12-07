package com.rainyseason.cj.data.database.kv

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [KeyValueEntry::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(InstantTypeConverter::class)
abstract class KeyValueDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
