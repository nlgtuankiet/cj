package com.rainyseason.cj.data

import com.rainyseason.cj.common.getNonNullCurrencyInfo
import com.rainyseason.cj.data.database.kv.KeyValueStore
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class UserSetting(
    @Json(name = "currency_code")
    val currencyCode: String = "usd",
    @Json(name = "refresh_interval")
    val refreshInterval: Long = 1,
    @Json(name = "refresh_interval_unit")
    val refreshIntervalUnit: TimeUnit = TimeUnit.HOURS,
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
    @Json(name = "size_adjustment")
    val sizeAdjustment: Int = 0,
    @Json(name = "realtime_interval_ms")
    val realtimeIntervalMs: Long = 1 * 60 * 1000L, // 60 seconds
)

val UserSetting.locale: Locale
    get() = getNonNullCurrencyInfo(currencyCode).locale

@Suppress("BlockingMethodInNonBlockingContext")
@Singleton
class UserSettingRepository @Inject constructor(
    private val keyValueStore: KeyValueStore,
    moshi: Moshi,
) {

    private val settingsKey = "settings"
    private val settingsAdapter = moshi.adapter(UserSetting::class.java)

    suspend fun setUserSetting(userSetting: UserSetting) {
        keyValueStore.setString(settingsKey, settingsAdapter.toJson(userSetting))
    }

    suspend fun getUserSetting(): UserSetting {
        return getUserSettingFlow().first()
    }

    fun getUserSettingFlow(): Flow<UserSetting> {
        return keyValueStore.getStringFlow(settingsKey)
            .map {
                if (it.isNullOrEmpty()) {
                    UserSetting()
                } else {
                    settingsAdapter.fromJson(it)!!
                }
            }
            .distinctUntilChanged()
    }
}
