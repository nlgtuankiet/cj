package com.rainyseason.cj.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.data.database.kv.KeyValueStore
import com.rainyseason.cj.ticker.CoinTickerProviderCoin360
import com.rainyseason.cj.ticker.CoinTickerProviderDefault
import com.rainyseason.cj.ticker.CoinTickerProviderGraph
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommonRepository @Inject constructor(
    private val appWidgetManager: AppWidgetManager,
    private val keyValueStore: KeyValueStore,
    private val context: Context,
) {

    private val widgetsUsedKey = "widgets_used"
    private val userLikeAppKey = "user_like_app"
    private val lastDislikeKey = "last_dislike"
    private val populateDefaultWatchList = "populate_default_watchlist"
    private val watchListIds = "watchlist_ids"
    private val readReleaseNoteVersion = "release_note"
    private val appHashKey = "app_hash"
    private val doneShowAddWidgetTutorial = "done_show_add_widget_tutorial"

    suspend fun getAppHash(): String {
        return keyValueStore.withTransaction {
            val appHash: String? = keyValueStore.getString(appHashKey)
            if (appHash.isNullOrBlank()) {
                val newAppHash = UUID.randomUUID().toString().replace("-", "")
                Timber.d("new app hash $newAppHash")
                keyValueStore.setString(appHashKey, newAppHash)
                return@withTransaction newAppHash
            }
            appHash
        }
    }

    suspend fun isDoneShowAddWidgetTutorial(): Boolean {
        return keyValueStore.getBoolean(doneShowAddWidgetTutorial) ?: false
    }

    suspend fun setDoneShowAddWidgetTutorial() {
        keyValueStore.setBoolean(doneShowAddWidgetTutorial, true)
    }

    fun hasUnreadReleaseNoteFlow(): Flow<Boolean> {
        return keyValueStore.getStringFlow(readReleaseNoteVersion).map {
            it != BuildConfig.VERSION_NAME
        }
    }

    suspend fun setReadReleaseNote() {
        keyValueStore.setString(readReleaseNoteVersion, BuildConfig.VERSION_NAME)
    }

    suspend fun populateDefaultWatchlist(): Boolean {
        return keyValueStore.getBoolean(populateDefaultWatchList) ?: false
    }

    suspend fun donePopulateDefaultWatchlist() {
        return keyValueStore.setBoolean(populateDefaultWatchList, true)
    }

    fun watchListIdsFlow(): Flow<List<String>> {
        return keyValueStore.getStringFlow(watchListIds).map {
            it.orEmpty().split(",").filter { key -> key.isNotBlank() }
        }
    }

    suspend fun setWatchListIds(ids: List<String>) {
        keyValueStore.setString(watchListIds, ids.joinToString(separator = ","))
    }

    suspend fun isUserLikeTheApp(): Boolean {
        return keyValueStore.getBoolean(userLikeAppKey) ?: false
    }

    suspend fun setUserLikeTheApp(value: Boolean) {
        keyValueStore.setBoolean(userLikeAppKey, value)
        if (!value) {
            setDislikeMilis(System.currentTimeMillis())
        }
    }

    suspend fun lastDislikeMilis(): Long? {
        return keyValueStore.getLong(lastDislikeKey)
    }

    private suspend fun setDislikeMilis(milis: Long) {
        keyValueStore.setLong(lastDislikeKey, milis)
    }

    suspend fun getWidgetsUsed(): Int {
        val valueFromStorage: Int? = keyValueStore.getLong(widgetsUsedKey)?.toInt()
        val widgetUsed = valueFromStorage ?: countWidgets()
        if (valueFromStorage == null) {
            keyValueStore.setLong(widgetsUsedKey, widgetUsed.toLong())
        }
        return widgetUsed
    }

    suspend fun increaseWidgetUsed() {
        val valueFromStorage: Int? = keyValueStore.getLong(widgetsUsedKey)?.toInt()
        val widgetUsed = valueFromStorage ?: countWidgets()
        keyValueStore.setLong(widgetsUsedKey, widgetUsed.toLong() + 1)
    }

    private fun countWidgets(): Int {
        // we don't use max widget id because in case user reinstall the app, widget id still kepp
        return listOf(
            CoinTickerProviderDefault::class.java,
            CoinTickerProviderGraph::class.java,
            CoinTickerProviderCoin360::class.java,
        ).map { clazz ->
            appWidgetManager.getAppWidgetIds(ComponentName(context, clazz)).size
        }.sum()
    }
}
