package com.rainyseason.cj.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rainyseason.cj.data.database.kv.KeyValueEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.Instant
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class KeyValueDatabaseTest : KeyValueDatabaseTestBase() {

    @Test
    fun insertString() = runBlocking {
        var entry = KeyValueEntry(
            key = "string_key",
            stringValue = "test_string",
            updateAt = Instant.now()
        )

        val nullEntry = dao.select(key = entry.key)
        Assert.assertEquals(null, nullEntry)

        dao.insert(entry)
        val valueEntry = dao.select(key = entry.key)
        Assert.assertEquals(entry, valueEntry)

        entry = entry.copy(stringValue = "test_string2", updateAt = Instant.now())
        dao.insert(entry)
        val updatedEntry = dao.select(key = entry.key)
        Assert.assertEquals(entry, updatedEntry)
    }

    @Test
    fun insertLong() = runBlocking {
        var entry = KeyValueEntry(
            key = "long_key",
            longValue = 420,
            updateAt = Instant.now()
        )

        val nullEntry = dao.select(key = entry.key)
        Assert.assertEquals(null, nullEntry)

        dao.insert(entry)
        val valueEntry = dao.select(key = entry.key)
        Assert.assertEquals(entry, valueEntry)

        entry = entry.copy(longValue = 421, updateAt = Instant.now())
        dao.insert(entry)
        val updatedEntry = dao.select(key = entry.key)
        Assert.assertEquals(entry, updatedEntry)
    }

    @Test
    fun insertDouble() = runBlocking {
        var entry = KeyValueEntry(
            key = "double_key",
            doubleValue = 420.0,
            updateAt = Instant.now()
        )

        val nullEntry = dao.select(key = entry.key)
        Assert.assertEquals(null, nullEntry)

        dao.insert(entry)
        val valueEntry = dao.select(key = entry.key)
        Assert.assertEquals(entry, valueEntry)

        entry = entry.copy(doubleValue = 420.1, updateAt = Instant.now())
        dao.insert(entry)
        val updatedEntry = dao.select(key = entry.key)
        Assert.assertEquals(entry, updatedEntry)
    }

    @Test
    fun flowString() = runBlocking {
        val entry1 = KeyValueEntry(
            key = "string_key",
            stringValue = "a",
            updateAt = Instant.now()
        )
        val entry2 = KeyValueEntry(
            key = "string_key",
            stringValue = "b",
            updateAt = Instant.now().plusMillis(10000)
        )
        val expected = listOf(entry1, entry2)
        val countDownLatch = CountDownLatch(1)

        val startFlowLatch = CountDownLatch(1)
        launch(Dispatchers.IO) {
            val actual = dao.selectFlow("string_key")
                .filterNotNull()
                .onEach {
                    startFlowLatch.countDown()
                    println("debugflow onEach $it")
                }
                .take(2)
                .toList()
            Assert.assertEquals(expected, actual)
            countDownLatch.countDown()
        }
        dao.insert(entry1)
        startFlowLatch.await()
        dao.insert(entry2)
        countDownLatch.await()
    }
}
