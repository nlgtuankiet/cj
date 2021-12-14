package com.rainyseason.cj.data

import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.data.database.kv.KeyValueStore
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Singleton
@Suppress("BlockingMethodInNonBlockingContext")
class CoinHistoryRepository @Inject constructor(
    private val keyValueStore: KeyValueStore,
    moshi: Moshi,
) {
    private val key = "entries"
    private val entriesAdapter = moshi.adapter(CoinHistoryEntries::class.java)

    fun getHistory(): Flow<List<CoinHistoryEntry>> {
        return keyValueStore.getStringFlow(key)
            .map { json ->
                if (json.isNullOrEmpty()) {
                    emptyList()
                } else {
                    entriesAdapter.fromJson(json)?.entries.orEmpty().distinctBy { it.id }
                }
            }
    }

    private suspend fun updateEntries(
        block: (List<CoinHistoryEntry>) -> List<CoinHistoryEntry>,
    ) {
        val oldEntryJson = keyValueStore.getString(key)
        val oldEntries = if (oldEntryJson.isNullOrBlank()) {
            emptyList()
        } else {
            entriesAdapter.fromJson(oldEntryJson)?.entries.orEmpty()
        }
        val newEntries = block.invoke(oldEntries).take(5)
        keyValueStore.setString(key, entriesAdapter.toJson(CoinHistoryEntries(newEntries)))
    }

    suspend fun add(
        entry: CoinHistoryEntry
    ) {
        updateEntries { oldEntries ->
            listOf(entry) + oldEntries.filterNot { it.id == entry.id }
        }
    }

    suspend fun remove(
        id: String
    ) {
        updateEntries { oldEntries ->
            oldEntries.filterNot { it.id == id }
        }
    }
}

@JsonClass(generateAdapter = true)
internal data class CoinHistoryEntries(
    @Json(name = "entries")
    val entries: List<CoinHistoryEntry>
)

@JsonClass(generateAdapter = true)
data class CoinHistoryEntry(
    @Json(name = "id")
    val id: String,
    @Json(name = "symbol")
    val symbol: String,
    @Json(name = "name")
    val name: String,
    @NullToCoinGeckoUrl // iconUrl is String? in the pass
    @Json(name = "icon_url")
    val iconUrl: String = Backend.CoinGecko.iconUrl,
    @Json(name = "backend")
    val backend: Backend = Backend.CoinGecko
) {
    val uniqueId: String = "${backend.id}_$id"

    @Retention(AnnotationRetention.RUNTIME)
    @JsonQualifier
    annotation class NullToCoinGeckoUrl

    object NullIconUrlToCoinGeckoUrlAdapter : JsonAdapter<String>() {
        override fun fromJson(reader: JsonReader): String {
            return if (reader.peek() == JsonReader.Token.NULL) {
                reader.nextNull<String>()
                Backend.CoinGecko.iconUrl
            } else {
                reader.nextString()
            }
        }

        override fun toJson(writer: JsonWriter, value: String?) {
            if (value == null) {
                writer.value(Backend.CoinGecko.iconUrl)
            } else {
                writer.value(value)
            }
        }
    }
}

@Qualifier
annotation class CoinHistory
