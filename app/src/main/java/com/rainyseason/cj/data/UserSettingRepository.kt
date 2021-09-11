package com.rainyseason.cj.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

data class UserCurrency(
    val id: String,
    val symbol: String,
    val placeOnTheLeft: Boolean,
    val separator: String,
)

@Singleton
class UserSettingRepository @Inject constructor(
    private val store: UserSettingStorage,
) {

    private val key = stringPreferencesKey("currency_code")

    suspend fun getCurrencyCode(): String {
        return getCurrencyCodeFlow().first()
    }

    fun getCurrencyCodeFlow(): Flow<String> {
        return store.data.map { pref ->
            pref[key]
        }
            .distinctUntilChanged()
            .mapNotNull { code ->
                if (code == null) {
                    setCurrencyCode("usd")
                }
                code
            }
    }

    suspend fun setCurrencyCode(code: String) {
        store.edit { pref -> pref[key] = code }
    }

}

class UserSettingStorage(
    private val delegate: DataStore<Preferences>,
) : DataStore<Preferences> by delegate