package com.rainyseason.cj.data.database.kv

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.Instant

@Entity(tableName = "entry")
data class KeyValueEntry(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "key")
    val key: String,
    @ColumnInfo(name = "string_value")
    val stringValue: String? = null,
    @ColumnInfo(name = "long_value")
    val longValue: Long? = null,
    @ColumnInfo(name = "double_value")
    val doubleValue: Double? = null,
    @ColumnInfo(name = "update_at")
    val updateAt: Instant = Instant.now()
)
