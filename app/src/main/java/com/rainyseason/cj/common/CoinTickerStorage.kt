package com.rainyseason.cj.common

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

class CoinTickerStorage(
    private val delegate: DataStore<Preferences>,
) : DataStore<Preferences> by delegate
