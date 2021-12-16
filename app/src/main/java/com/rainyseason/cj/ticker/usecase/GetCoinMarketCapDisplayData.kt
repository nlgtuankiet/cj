package com.rainyseason.cj.ticker.usecase

import com.rainyseason.cj.common.currencyInfoOf
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.data.cmc.CmcService
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class GetCoinMarketCapDisplayData @Inject constructor(
    private val cmcService: CmcService,
) {
    suspend operator fun invoke(param: CoinTickerDisplayData.LoadParam): CoinTickerDisplayData {
        return coroutineScope {
            val currencyId = currencyInfoOf(param.currency).cmcId
            val quoteAsync = async {
                cmcService.getQuote(id = param.coinId, convertId = currencyId)
            }

            val chartAsync = async {
                cmcService.getChart(
                    id = param.coinId,
                    convertId = currencyId,
                    range = when (param.changeInterval) {
                        TimeInterval.I_24H -> "1D"
                        TimeInterval.I_7D -> "7D"
                        TimeInterval.I_30D -> "1M"
                        else -> error("not support ${param.changeInterval}")
                    }
                )
            }

            val quote = quoteAsync.await().data.first()
            val chart = chartAsync.await()
            val chartConvert: List<List<Double>> = chart.data.points.map {
                listOf(
                    it.key,
                    (it.value.c ?: it.value.v).first()
                )
            }
                .sortedBy { it[0] }

            val quoteData = quote.quotes.first()
            CoinTickerDisplayData(
                iconUrl = CmcService.getCmcIconUrl(param.coinId),
                symbol = quote.symbol,
                name = quote.name,
                price = quoteData.price,
                priceChangePercent = when (param.changeInterval) {
                    TimeInterval.I_24H -> quoteData.percentChange24h
                    TimeInterval.I_7D -> quoteData.percentChange7d
                    TimeInterval.I_30D -> quoteData.percentChange30d
                    else -> error("not support ${param.changeInterval}")
                },
                priceGraph = chartConvert
            )
        }
    }
}
