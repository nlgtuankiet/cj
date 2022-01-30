package com.rainyseason.cj.ticker.usecase

import com.rainyseason.cj.common.changePercent
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.model.asDayString
import com.rainyseason.cj.common.notNull
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.currentPrice
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class GetCoinGeckoDisplayData @Inject constructor(
    private val coinGeckoService: CoinGeckoService
) {

    /**
     * TODO get correct price
     */
    suspend operator fun invoke(
        param: CoinTickerDisplayData.LoadParam
    ): CoinTickerDisplayData {
        return coroutineScope {
            val coinDetailAsync = async { coinGeckoService.getCoinDetail(param.coinId) }
            val graphResponse = coinGeckoService.getMarketChart(
                id = param.coinId,
                vsCurrency = param.currency,
                day = param.changeInterval.asDayString().notNull()
            )

            val marketPrice = if (param.changeInterval == TimeInterval.I_24H) {
                graphResponse.currentPrice()
            } else {
                coinGeckoService.getMarketChart(
                    id = param.coinId,
                    vsCurrency = param.currency,
                    day = "1",
                ).currentPrice()
            }

            val coinDetail = coinDetailAsync.await()
            val price = marketPrice ?: coinDetail.marketData.currentPrice[param.currency]
            val changePercent = graphResponse.prices.changePercent()?.times(100)
            reportIntervalPercent(param, graphResponse.prices)
            CoinTickerDisplayData(
                iconUrl = coinDetail.image.large,
                symbol = coinDetail.symbol,
                name = coinDetail.name,
                price = price,
                priceChangePercent = changePercent,
                priceGraph = graphResponse.prices,
            )
        }
    }
}
