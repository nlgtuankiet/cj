package com.rainyseason.cj.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.yield
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
    private val store: DataStore<Preferences>
) {
    suspend fun getCurrency(): UserCurrency {
        yield()
        return UserCurrency(
            id = "usd",
            symbol = "$",
            placeOnTheLeft = true,
            separator = ""
        )
    }
}