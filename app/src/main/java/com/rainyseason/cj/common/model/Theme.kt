package com.rainyseason.cj.common.model

import androidx.core.os.BuildCompat
import com.rainyseason.cj.R
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class Theme(
    val id: String,
    val titleRes: Int,
    val isMaterialYou: Boolean = false
) {
    Auto(
        "auto",
        R.string.coin_ticker_preview_setting_theme_default
    ),
    Light(
        "light",
        R.string.coin_ticker_preview_setting_theme_light
    ),
    Dark(
        "dark",
        R.string.coin_ticker_preview_setting_theme_dark
    ),
    MaterialYou(
        "material_you_auto",
        R.string.coin_ticker_preview_setting_theme_mu_light_dark,
        true,
    ),
    MaterialYouLight(
        "material_you_light",
        R.string.coin_ticker_preview_setting_theme_mu_light,
        true,
    ),
    MaterialYouDark(
        "material_you_dark",
        R.string.coin_ticker_preview_setting_theme_mu_dark,
        true,
    ),

    ;
    companion object {
        val VALUES_COMPAT: Array<Theme> = if (BuildCompat.isAtLeastS()) {
            values()
        } else {
            values().filter { !it.isMaterialYou }.toTypedArray()
        }
    }
}

object ThemeJsonAdapter : JsonAdapter<Theme>() {

    private val stringToEntry: Map<String, Theme> = Theme.values()
        .associateBy { it.id }

    override fun fromJson(reader: JsonReader): Theme? {
        val stringValue = reader.nextString() ?: return null
        return stringToEntry[stringValue]
    }

    override fun toJson(writer: JsonWriter, value: Theme?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.id)
        }
    }
}
