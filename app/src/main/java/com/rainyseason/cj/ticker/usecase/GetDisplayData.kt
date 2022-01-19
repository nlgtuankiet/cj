package com.rainyseason.cj.ticker.usecase

import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import kotlinx.coroutines.delay
import javax.inject.Inject

class GetDisplayData @Inject constructor(
    private val getCoinGeckoDisplayData: GetCoinGeckoDisplayData,
    private val getBinanceDisplayData: GetBinanceDisplayData,
    private val getCoinMarketCapDisplayData: GetCoinMarketCapDisplayData,
    private val getCoinbaseDisplayData: GetCoinbaseDisplayData,
    private val getFtxDisplayData: GetFtxDisplayData,
    private val getKrakenDisplayData: GetKrakenDisplayData,
) {

    suspend operator fun invoke(param: CoinTickerDisplayData.LoadParam): CoinTickerDisplayData {
        val result = when (param.backend) {
            Backend.Binance -> getBinanceDisplayData(param)
            Backend.CoinGecko -> getCoinGeckoDisplayData(param)
            Backend.CoinMarketCap -> getCoinMarketCapDisplayData(param)
            Backend.Coinbase -> getCoinbaseDisplayData(param)
            Backend.Ftx -> getFtxDisplayData(param)
            Backend.Kraken -> getKrakenDisplayData(param)
        }
        if (DebugFlag.SLOW_TICKER_PREVIEW.isEnable) {
            delay(3000)
        }
        return result
    }
}
