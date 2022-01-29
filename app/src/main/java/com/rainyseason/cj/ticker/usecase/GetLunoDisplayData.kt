package com.rainyseason.cj.ticker.usecase

import com.rainyseason.cj.common.changePercent
import com.rainyseason.cj.common.findApproxIndex
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.data.luno.LunoWebService
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetLunoDisplayData @Inject constructor(
    private val service: LunoWebService,
) {
    suspend operator fun invoke(
        param: CoinTickerDisplayData.LoadParam
    ): CoinTickerDisplayData {
        return coroutineScope {
            val tickerAsync = async {
                service.ticker(param.coinId)
            }
            val timeCloseAsync = async {
                val resToDelta = when (param.changeInterval) {
                    TimeInterval.I_24H -> "15" to TimeUnit.DAYS.toSeconds(30)
                    TimeInterval.I_7D -> "15" to TimeUnit.DAYS.toSeconds(30)
                    TimeInterval.I_30D -> "30" to TimeUnit.DAYS.toSeconds(60)
                    else -> error("not support")
                }
                val currentSeconds = System.currentTimeMillis() / 1000
                val ohlc = service.ohlc(
                    id = param.coinId,
                    resolution = resToDelta.first,
                    from = currentSeconds - resToDelta.second,
                    to = currentSeconds,
                )
                val timeClose = mutableListOf<List<Double>>()
                ohlc.time.forEachIndexed { index, d ->
                    timeClose.add(listOf(d, ohlc.close[index]))
                }
                timeClose
            }

            val timeClose = timeCloseAsync.await()
            val intervalMilis = param.changeInterval.toMilis()
            val endMilis = timeClose.last()[0]
            val startMilis = endMilis - intervalMilis
            val startIndex = timeClose.findApproxIndex(startMilis)
            val approxCandles = timeClose.subList(startIndex, timeClose.size)
            reportIntervalPercent(param, approxCandles)

            val ticker = tickerAsync.await()
            CoinTickerDisplayData(
                iconUrl = Backend.Luno.iconUrl,
                symbol = ticker.currencyPair.run { "$base/$quote" },
                name = Backend.Luno.displayName,
                price = ticker.run { (ask + bid) / 2 },
                priceChangePercent = approxCandles.changePercent()?.let { it * 100 },
                priceGraph = approxCandles
            )
        }
    }
}
