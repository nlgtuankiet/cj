package com.rainyseason.cj.ticker.usecase

import com.rainyseason.cj.common.changePercent
import com.rainyseason.cj.common.findApproxIndex
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.data.coinbase.CoinbaseService
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GetCoinbaseDisplayData @Inject constructor(
    private val coinbaseService: CoinbaseService,
) {
    suspend operator fun invoke(
        param: CoinTickerDisplayData.LoadParam
    ): CoinTickerDisplayData {
        return coroutineScope {
            val tickerAsync = async {
                coinbaseService.getTicker(param.coinId)
            }

            val candlesAsync = async {
                coinbaseService.getCandles(
                    id = param.coinId,
                    granularity = when (param.changeInterval) {
                        TimeInterval.I_24H -> TimeUnit.MINUTES.toSeconds(5)
                        TimeInterval.I_7D -> TimeUnit.HOURS.toSeconds(1)
                        TimeInterval.I_30D -> TimeUnit.HOURS.toSeconds(6)
                        else -> error("not support")
                    }.toInt()
                ).map { listOf(it[0] * 1000, it[4]) }
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
                iconUrl = Backend.Coinbase.iconUrl,
                symbol = param.coinId.replace("-", "/"),
                name = Backend.Coinbase.displayName,
                price = ticker.price,
                priceChangePercent = approxCandles.changePercent()?.let { it * 100 },
                priceGraph = approxCandles
            )
        }
    }
}
