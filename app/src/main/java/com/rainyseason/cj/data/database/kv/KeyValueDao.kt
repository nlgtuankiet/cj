package com.rainyseason.cj.data.database.kv

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyValueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: KeyValueEntry)

    @Query("select * from entry where `key` = :key")
    suspend fun select(key: String): KeyValueEntry?

    @Query("delete from entry where `key` = :key")
    suspend fun delete(key: String)

    @Query("select * from entry where `key` = :key limit 1")
    fun selectFlow(key: String): Flow<KeyValueEntry?>
}
