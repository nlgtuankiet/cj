package com.rainyseason.cj.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


@JsonClass(generateAdapter = true)
data class UserSetting(
    @Json(name = "currency_code")
    val currencyCode: String = "usd",
    @Json(name = "refresh_interval")
    val refreshInterval: Long = 15,
    @Json(name = "refresh_interval_unit")
    val refreshIntervalUnit: TimeUnit = TimeUnit.MINUTES,
    @Json(name = "amount_decimals")
    val amountDecimals: Int? = 2,
    @Json(name = "round_to_million")
    val roundToMillion: Boolean = true,
    @Json(name = "show_currency_symbol")
    val showCurrencySymbol: Boolean = true,
    @Json(name = "show_thousand_separator")
    val showThousandsSeparator: Boolean = true,
    @Json(name = "number_of_change_percent_decimal")
    val numberOfChangePercentDecimal: Int? = 1,
)

@Suppress("MemberVisibilityCanBePrivate")
@Singleton
class UserSettingRepository @Inject constructor(
    private val store: UserSettingStorage,
    moshi: Moshi,
) {

    private val settingsKey = stringPreferencesKey("settings")
    private val settingsAdapter = moshi.adapter(UserSetting::class.java)

    suspend fun setUserSetting(userSetting: UserSetting) {
        store.edit {
            it[settingsKey] = settingsAdapter.toJson(userSetting)
        }
    }

    suspend fun getUserSetting(): UserSetting {
        return getUserSettingFlow().first()
    }

    fun getUserSettingFlow(): Flow<UserSetting> {
        return store.data.map { pref ->
            if (pref.contains(settingsKey)) {
                @Suppress("BlockingMethodInNonBlockingContext")
                settingsAdapter.fromJson(pref[settingsKey]!!)!!
            } else {
                UserSetting()
            }
        }.distinctUntilChanged()
    }
}

class UserSettingStorage(
    private val delegate: DataStore<Preferences>,
) : DataStore<Preferences> by delegate