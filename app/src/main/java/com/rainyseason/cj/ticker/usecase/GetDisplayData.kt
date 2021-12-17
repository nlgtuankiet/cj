package com.rainyseason.cj.ticker.usecase

import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import javax.inject.Inject

class GetDisplayData @Inject constructor(
    private val getCoinGeckoDisplayData: GetCoinGeckoDisplayData,
    private val getBinanceDisplayData: GetBinanceDisplayData,
    private val getCoinMarketCapDisplayData: GetCoinMarketCapDisplayData,
    private val getCoinbaseDisplayData: GetCoinbaseDisplayData,
) {

    suspend operator fun invoke(param: CoinTickerDisplayData.LoadParam): CoinTickerDisplayData {
        return when (param.backend) {
            Backend.Binance -> getBinanceDisplayData(param)
            Backend.CoinGecko -> getCoinGeckoDisplayData(param)
            Backend.CoinMarketCap -> getCoinMarketCapDisplayData(param)
            Backend.Coinbase -> getCoinbaseDisplayData(param)
        }
    }
}
