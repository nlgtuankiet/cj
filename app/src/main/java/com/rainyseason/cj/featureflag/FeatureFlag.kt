package com.rainyseason.cj.featureflag

import java.util.Collections

interface FlagKey {
    val value: String
}

fun <T> T.withDefault(value: String?): T where T : FlagKey {
    DefaultValues.add(this, value)
    return this
}

interface FlagValueProvider {
    fun get(flagKey: FlagKey): String?
}

object NoopFlagValueProvider : FlagValueProvider {
    override fun get(flagKey: FlagKey): String? {
        return null
    }
}

object DefaultValues {
    private val values = Collections.synchronizedMap<FlagKey, String?>(HashMap())

    fun add(flagKey: FlagKey, default: String?) {
        values[flagKey] = default
    }

    fun get(flagKey: FlagKey): String? {
        return values[flagKey]
    }
}

object MainFlagValueProvider {

    private lateinit var mainProvider: FlagValueProvider

    fun setDelegate(provider: FlagValueProvider) {
        mainProvider = provider
    }

    fun get(flagKey: FlagKey): String? {
        return mainProvider.get(flagKey)
    }
}

val FlagKey.isEnable: Boolean
    get() {
        val string = MainFlagValueProvider.get(this) ?: DefaultValues.get(this)
        val cleanString = string?.trim()
        if (cleanString.isNullOrEmpty()) {
            return false
        }
        if (cleanString.equals("false", true)) {
            return false
        }
        if (cleanString == "0") {
            return false
        }
        return true
    }
