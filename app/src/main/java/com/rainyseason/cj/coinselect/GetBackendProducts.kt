package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.model.Backend
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetBackendProducts @Inject constructor(
    private val getBinanceProducts: GetBinanceProducts,
    private val getCoinMarketCapProducts: GetCoinMarketCapProducts,
    private val getCoinGeckoProducts: GetCoinGeckoProducts,
    private val getCoinbaseProducts: GetCoinbaseProducts,
    private val getFtxProducts: GetFtxProducts,
) {

    operator fun invoke(backend: Backend): Flow<List<BackendProduct>> {
        return when (backend) {
            Backend.Binance -> getBinanceProducts.invoke()
            Backend.CoinMarketCap -> getCoinMarketCapProducts.invoke()
            Backend.CoinGecko -> getCoinGeckoProducts.invoke()
            Backend.Coinbase -> getCoinbaseProducts.invoke()
            Backend.Ftx -> getFtxProducts.invoke()
        }
    }
}
