package com.rainyseason.cj.ticker.usecase

import com.rainyseason.cj.common.changePercent
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.data.binance.BinanceService
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class GetBinanceDisplayData @Inject constructor(
    private val binanceService: BinanceService
) {
    suspend operator fun invoke(param: CoinTickerDisplayData.LoadParam): CoinTickerDisplayData {
        return coroutineScope {
            val symbolDetailAsync = async {
                binanceService.getSymbolDetail(param.coinId)
            }

            val intervalToLimit = when (param.changeInterval) {
                TimeInterval.I_24H -> "3m" to 480
                TimeInterval.I_7D -> "15m" to 672
                TimeInterval.I_14D -> "1h" to 336
                TimeInterval.I_30D -> "1h" to 720
                TimeInterval.I_1Y -> "12h" to 730
                else -> error("Not support time interval ${param.changeInterval}")
            }

            val kLinesResponseAsync = async {
                binanceService.getKLines(
                    param.coinId,
                    intervalToLimit.first,
                    intervalToLimit.second
                ).map {
                    listOf(
                        it[6], // close time in milis
                        it[4], // close price
                    )
                }
            }

            val tickerPriceAsync = async { binanceService.getTickerPrice(param.coinId) }

            // TODO no need to call another api for display name only
            val symbolDetail = symbolDetailAsync.await().symbols.first()
            val kLinesResponse = kLinesResponseAsync.await()
            val tickerPrice = tickerPriceAsync.await()

            CoinTickerDisplayData(
                iconUrl = Backend.Binance.iconUrl,
                symbol = symbolDetail.displayName(),
                name = param.backend.displayName,
                price = tickerPrice.price,
                priceChangePercent = kLinesResponse.changePercent()?.let { it * 100 },
                priceGraph = kLinesResponse
            )
        }
    }
}
