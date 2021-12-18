package com.rainyseason.cj.ticker.usecase

import com.rainyseason.cj.common.changePercent
import com.rainyseason.cj.common.findApproxIndex
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.data.kraken.KrakenService
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class GetKrakenDisplayData @Inject constructor(
    private val krakenService: KrakenService,
) {
    suspend operator fun invoke(
        param: CoinTickerDisplayData.LoadParam
    ): CoinTickerDisplayData {
        return coroutineScope {
            val tickerAsync = async {
                krakenService.getPrice(param.coinId)
            }

            val candlesAsync = async {
                val period = when (param.changeInterval) {
                    TimeInterval.I_24H -> 300
                    TimeInterval.I_7D -> 1800
                    TimeInterval.I_30D -> 7200
                    else -> error("not support")
                }.toInt()
                krakenService.getGraph(
                    id = param.coinId,
                    period = period
                ).result[period.toString()]!!
                    .map {
                        listOf(it[0] * 1000, it[4])
                    }
                    .sortedBy { it[0] }
            }

            val ticker = tickerAsync.await()
            val candles = candlesAsync.await()
            val intervalMilis = param.changeInterval.toMilis()
            val endMilis = candles.last()[0]
            val startMilis = endMilis - intervalMilis
            val startIndex = candles.findApproxIndex(startMilis)
            val approxCandles = candles.subList(startIndex, candles.size)
            reportIntervalPercent(param, approxCandles)

            CoinTickerDisplayData(
                iconUrl = Backend.Kraken.iconUrl,
                symbol = param.coinId,
                name = Backend.Kraken.displayName,
                price = ticker.result.price,
                priceChangePercent = approxCandles.changePercent()?.let { it * 100 },
                priceGraph = approxCandles
            )
        }
    }
}
