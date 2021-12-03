package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.model.Backend
import javax.inject.Inject

class GetExchangeEntry @Inject constructor(
    private val getBinanceExchangeEntry: GetBinanceExchangeEntry,
) {
    suspend operator fun invoke(backend: Backend): List<ExchangeEntry> {
        return when (backend) {
            Backend.Binance -> getBinanceExchangeEntry.invoke()
            Backend.CoinGecko -> error("no need")
        }
    }
}
