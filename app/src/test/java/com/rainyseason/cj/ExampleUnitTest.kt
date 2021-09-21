package com.rainyseason.cj

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @JsonClass(generateAdapter = true)
    data class ExampleData(
        @Json(name = "show")
        val show: Boolean = true,
    )

    @Test
    fun addition_isCorrect() {
        val jsonString = """{}"""
        val adapter = ExampleUnitTest_ExampleDataJsonAdapter(Moshi.Builder().build())
        val entity = adapter.fromJson(jsonString)
        assertEquals(ExampleData(), entity)
    }
}