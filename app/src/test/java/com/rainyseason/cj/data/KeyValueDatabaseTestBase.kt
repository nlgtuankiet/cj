package com.rainyseason.cj.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rainyseason.cj.CJApplication
import com.rainyseason.cj.data.database.kv.KeyValueDao
import com.rainyseason.cj.data.database.kv.KeyValueDatabase
import org.junit.After
import org.junit.Before

abstract class KeyValueDatabaseTestBase {
    lateinit var dao: KeyValueDao
    lateinit var db: KeyValueDatabase

    @Before
    fun createDb() {
        val appContext = ApplicationProvider.getApplicationContext<CJApplication>()
        db = Room.inMemoryDatabaseBuilder(appContext, KeyValueDatabase::class.java)
            .build()
        dao = db.entryDao()
    }

    @After
    fun close() {
        db.close()
    }
}
