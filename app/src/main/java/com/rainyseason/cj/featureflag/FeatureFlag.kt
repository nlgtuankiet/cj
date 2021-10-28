package com.rainyseason.cj.featureflag

import java.util.Collections
import java.util.regex.Pattern

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

val TRUE_REGEX = Pattern.compile("^(1|true|t|yes|y|on)$", Pattern.CASE_INSENSITIVE)

val FALSE_REGEX = Pattern.compile("^(0|false|f|no|n|off|)$", Pattern.CASE_INSENSITIVE)

val FlagKey.isEnable: Boolean
    get() {
        val string = MainFlagValueProvider.get(this) ?: DefaultValues.get(this)
        if (string != null) {
            if (TRUE_REGEX.matcher(string).matches()) {
                return true
            } else if (FALSE_REGEX.matcher(string).matches()) {
                return false
            }
        }
        return false
    }
