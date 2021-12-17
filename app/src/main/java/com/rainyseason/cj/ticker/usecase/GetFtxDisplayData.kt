package com.rainyseason.cj.ticker.usecase

import com.rainyseason.cj.common.changePercent
import com.rainyseason.cj.common.findApproxIndex
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.data.ftx.FtxService
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GetFtxDisplayData @Inject constructor(
    private val ftxService: FtxService,
) {
    suspend operator fun invoke(
        param: CoinTickerDisplayData.LoadParam
    ): CoinTickerDisplayData {
        return coroutineScope {
            val tickerAsync = async {
                ftxService.getMarket(param.coinId)
            }

            val candlesAsync = async {
                ftxService.getCandles(
                    id = param.coinId,
                    resolution = when (param.changeInterval) {
                        TimeInterval.I_24H -> TimeUnit.MINUTES.toSeconds(1)
                        TimeInterval.I_7D -> TimeUnit.MINUTES.toSeconds(15)
                        TimeInterval.I_30D -> TimeUnit.MINUTES.toSeconds(60)
                        else -> error("not support")
                    }.toInt()
                ).result.map { listOf(it.time, it.close) }
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
                iconUrl = Backend.Ftx.iconUrl,
                symbol = param.coinId,
                name = Backend.Ftx.displayName,
                price = ticker.result.price,
                priceChangePercent = approxCandles.changePercent()?.let { it * 100 },
                priceGraph = approxCandles
            )
        }
    }
}
