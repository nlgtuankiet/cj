package com.rainyseason.cj.ticker.usecase

import android.content.Context
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.ticker.CoinTickerLayout
import javax.inject.Inject

class CreateDefaultTickerWidgetConfig @Inject constructor(
    private val context: Context,
) {
    operator fun invoke(
        widgetId: Int,
    ): CoinTickerConfig {
        val layout = CoinTickerLayout.fromWidgetId(context, widgetId)
        return CoinTickerConfig(
            widgetId = widgetId,
            coinId = "bitcoin",
            layout = layout,
            backend = Backend.CoinGecko,
        )
    }
}
