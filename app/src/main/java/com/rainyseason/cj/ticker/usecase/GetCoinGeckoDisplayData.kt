package com.rainyseason.cj.ticker.usecase

import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.model.asDayString
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.currentPrice
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import javax.inject.Inject

class GetCoinGeckoDisplayData @Inject constructor(
    private val coinGeckoService: CoinGeckoService
) {

    suspend operator fun invoke(
        param: CoinTickerDisplayData.LoadParam
    ): CoinTickerDisplayData {
        val coinDetail = coinGeckoService.getCoinDetail(param.coinId)
        val graphResponse = coinGeckoService.getMarketChart(
            id = param.coinId,
            vsCurrency = param.currency,
            day = param.changeInterval.asDayString()!!
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

        val price = marketPrice ?: coinDetail.marketData.currentPrice[param.currency]

        val priceChangePercent = when (param.changeInterval) {
            TimeInterval.I_7D ->
                coinDetail.marketData
                    .priceChangePercentage7dInCurrency[param.currency]
            TimeInterval.I_14D ->
                coinDetail.marketData
                    .priceChangePercentage14dInCurrency[param.currency]
            TimeInterval.I_30D ->
                coinDetail.marketData
                    .priceChangePercentage30dInCurrency[param.currency]
            TimeInterval.I_1Y ->
                coinDetail.marketData
                    .priceChangePercentage1yInCurrency[param.currency]
            else ->
                coinDetail.marketData
                    .priceChangePercentage24hInCurrency[param.currency]
        }

        return CoinTickerDisplayData(
            iconUrl = coinDetail.image.large,
            symbol = coinDetail.symbol,
            name = coinDetail.name,
            price = price,
            priceChangePercent = priceChangePercent,
            priceGraph = graphResponse.prices,
        )
    }
}
