package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.data.binance.BinanceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetBinanceProducts @Inject constructor(
    private val binanceService: BinanceService
) {
    operator fun invoke(): Flow<List<BackendProduct>> {
        return flow {
            val response = binanceService.getSymbolDetail()
            val products = response.symbols.map {
                BackendProduct(
                    id = it.symbol,
                    symbol = it.symbol,
                    displayName = it.displayName(),
                    backend = Backend.Binance,
                    iconUrl = Backend.Binance.iconUrl
                )
            }
            emit(products)
        }
    }
}
