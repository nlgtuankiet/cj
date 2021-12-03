package com.rainyseason.cj

import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.data.CoinHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class NullToDefaultTest {
    @Test
    fun `icon url from nullable to non null`() {
        val jsonString1 = """{"id":"fake_id","symbol":"fake","name":"Fake","icon_url":null}"""
        val jsonString2 = """{"id":"fake_id","symbol":"fake","name":"Fake"}"""
        val expectedEntity = CoinHistoryEntry(
            "fake_id",
            "fake",
            "Fake",
            Backend.CoinGecko.iconUrl,
            Backend.CoinGecko
        )
        val adapter = AppProvides.moshi().adapter(CoinHistoryEntry::class.java)
        val entity1 = adapter.fromJson(jsonString1)
        assertEquals(expectedEntity, entity1)
        val entity2 = adapter.fromJson(jsonString2)
        assertEquals(expectedEntity, entity2)
    }
}
