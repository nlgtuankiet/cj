package com.rainyseason.cj.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rainyseason.cj.common.model.Backend
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Singleton
class CoinHistoryRepository @Inject constructor(
    @CoinHistory
    private val storage: DataStore<Preferences>,
    private val moshi: Moshi,
) {
    private val key = stringPreferencesKey("entries")
    private val entriesAdapter = moshi.adapter(CoinHistoryEntries::class.java)

    fun getHistory(): Flow<List<CoinHistoryEntry>> {
        return storage.data.map {
            val oldEntryJson = storage.data.first()[key]
            val oldEntries = if (oldEntryJson == null) {
                emptyList()
            } else {
                @Suppress("BlockingMethodInNonBlockingContext")
                entriesAdapter.fromJson(oldEntryJson)?.entries.orEmpty()
            }
            oldEntries.distinctBy { it.id }
        }.distinctUntilChanged()
    }

    private suspend fun updateEntries(
        block: (List<CoinHistoryEntry>) -> List<CoinHistoryEntry>,
    ) {
        val oldEntryJson = storage.data.first()[key]
        val oldEntries = if (oldEntryJson == null) {
            emptyList()
        } else {
            @Suppress("BlockingMethodInNonBlockingContext")
            entriesAdapter.fromJson(oldEntryJson)?.entries.orEmpty()
        }
        val newEntries = block.invoke(oldEntries).take(5)
        storage.edit {
            it[key] = entriesAdapter.toJson(CoinHistoryEntries(newEntries))
        }
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
    @Json(name = "icon_url")
    val iconUrl: String = Backend.CoinGecko.iconUrl,
    @Json(name = "backend")
    val backend: Backend = Backend.CoinGecko
) {
    val uniqueId: String = "${backend.id}_$id"
}

@Qualifier
annotation class CoinHistory
