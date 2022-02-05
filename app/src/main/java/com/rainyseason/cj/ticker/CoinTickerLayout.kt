package com.rainyseason.cj.ticker

import android.util.Size
import com.rainyseason.cj.R
import com.rainyseason.cj.common.model.WidgetLayout
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class CoinTickerLayout(
    val id: String,
    override val providerName: String,
    val layout: Int,
    val titleRes: Int,
    val ratio: Size,
    val hasIcon: Boolean = false
) : WidgetLayout {
    Default2x2(
        "default",
        CoinTickerProviderDefault::class.java.name,
        R.layout.widget_coin_ticker_2x2,
        R.string.coin_ticket_style_default,
        Size(2, 2),
    ),
    Graph2x2(
        "graph",
        CoinTickerProviderGraph::class.java.name,
        R.layout.widget_coin_ticker_2x2,
        R.string.coin_ticket_style_graph,
        Size(2, 2),
    ),
    Coin3602x2(
        "coin360",
        CoinTickerProviderCoin360::class.java.name,
        R.layout.widget_coin_ticker_2x2,
        R.string.coin_ticket_style_coin360,
        Size(2, 2),
    ),
    Coin3601x1(
        "coin360_mini",
        CoinTickerProviderCoin360Mini::class.java.name,
        R.layout.widget_coin_ticker_1x1,
        R.string.coin_ticket_style_coin360_mini,
        Size(1, 1),
    ),
    Graph2x1(
        "mini",
        CoinTickerProviderMini::class.java.name,
        R.layout.widget_coin_ticker_2x1,
        R.string.coin_ticket_style_mini,
        Size(2, 1),
    ),
    Nano1x1(
        "nano",
        CoinTickerProviderNano::class.java.name,
        R.layout.widget_coin_ticker_1x1,
        R.string.coin_ticket_style_nano,
        Size(1, 1),
    ),
    Icon2x1(
        "icon_small",
        CoinTickerProviderIconSmall::class.java.name,
        R.layout.widget_coin_ticker_2x1,
        R.string.coin_ticket_style_icon_small,
        Size(2, 1),
        hasIcon = true
    );

    val isNano: Boolean
        get() = ratio == Size(1, 1)

    fun alternativeLayouts(): List<CoinTickerLayout> {
        return values().filter { it.ratio == ratio }
    }

    companion object {
        fun fromComponentName(name: String): CoinTickerLayout {
            return values().first { it.providerName == name }
        }
    }
}

object CoinTickerLayoutJsonAdapter : JsonAdapter<CoinTickerLayout>() {

    private val stringToEntry: Map<String, CoinTickerLayout> = CoinTickerLayout.values()
        .associateBy { it.id }

    override fun fromJson(reader: JsonReader): CoinTickerLayout? {
        val stringValue = reader.nextString() ?: return null
        return stringToEntry[stringValue]
    }

    override fun toJson(writer: JsonWriter, value: CoinTickerLayout?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.id)
        }
    }
}
