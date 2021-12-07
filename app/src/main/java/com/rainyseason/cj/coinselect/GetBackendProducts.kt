package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.model.Backend
import javax.inject.Inject

class GetBackendProducts @Inject constructor(
    private val getBinanceProducts: GetBinanceProducts,
) {
    suspend operator fun invoke(backend: Backend): List<BackendProduct> {
        return when (backend) {
            Backend.Binance -> getBinanceProducts.invoke()
            Backend.CoinGecko -> error("no need")
        }
    }
}
