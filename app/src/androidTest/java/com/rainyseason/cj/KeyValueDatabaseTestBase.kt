package com.rainyseason.cj

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rainyseason.cj.data.database.kv.EntryDao
import com.rainyseason.cj.data.database.kv.KeyValueDatabase
import org.junit.After
import org.junit.Before

abstract class KeyValueDatabaseTestBase {
    lateinit var dao: EntryDao
    lateinit var db: KeyValueDatabase

    @Before
    fun createDb() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(appContext, KeyValueDatabase::class.java)
            .build()
        dao = db.entryDao()
    }

    @After
    fun close() {
        db.close()
    }
}
