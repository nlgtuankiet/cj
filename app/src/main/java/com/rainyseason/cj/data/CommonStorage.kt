package com.rainyseason.cj.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.rainyseason.cj.ticker.CoinTickerProviderCoin360
import com.rainyseason.cj.ticker.CoinTickerProviderDefault
import com.rainyseason.cj.ticker.CoinTickerProviderGraph
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class CommonStorage

@Singleton
class CommonRepository @Inject constructor(
    @CommonStorage
    private val storage: DataStore<Preferences>,
    private val appWidgetManager: AppWidgetManager,
    private val context: Context,
) {

    private val widgetsUsedKey = intPreferencesKey("widgets_used")
    private val userLikeAppKey = booleanPreferencesKey("user_like_app")
    private val lastDislikeKey = longPreferencesKey("last_dislike")

    suspend fun isUserLikeTheApp(): Boolean {
        return storage.data.first()[userLikeAppKey] ?: false
    }

    suspend fun setUserLikeTheApp(value: Boolean) {
        storage.edit { it[userLikeAppKey] = value }
        if (!value) {
            setDislikeMilis(System.currentTimeMillis())
        }
    }

    suspend fun lastDislikeMilis(): Long? {
        return storage.data.first()[lastDislikeKey]
    }

    private suspend fun setDislikeMilis(milis: Long) {
        storage.edit { it[lastDislikeKey] = milis }
    }

    suspend fun getWidgetsUsed(): Int {
        val valueFromStorage: Int? = storage.data.first()[widgetsUsedKey]
        val widgetUsed = valueFromStorage ?: countWidgets()
        if (valueFromStorage == null) {
            storage.edit { it[widgetsUsedKey] = widgetUsed }
        }
        return widgetUsed
    }

    suspend fun increaseWidgetUsed() {
        val valueFromStorage: Int? = storage.data.first()[widgetsUsedKey]
        val widgetUsed = valueFromStorage ?: countWidgets()
        storage.edit { it[widgetsUsedKey] = (widgetUsed + 1) }
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