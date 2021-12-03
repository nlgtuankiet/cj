package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.data.binance.BinanceService
import javax.inject.Inject

class GetBinanceExchangeEntry @Inject constructor(
    private val binanceService: BinanceService
) {
    suspend operator fun invoke(): List<ExchangeEntry> {
        val response = binanceService.getSymbolDetail()
        return response.symbols.map {
            ExchangeEntry(
                symbol = it.symbol,
                displayName = it.displayName(),
                backend = Backend.Binance
            )
        }
    }
}
