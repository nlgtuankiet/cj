package com.rainyseason.cj.watch

import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import javax.inject.Inject

class GetTopCoins @Inject constructor(
    private val coinGeckoService: CoinGeckoService,
) {
    suspend operator fun invoke(): List<Coin> {
        val markets = coinGeckoService.getCoinMarkets("usd", 10, 1)
        return markets.map {
            Coin(
                id = it.id,
                backend = Backend.CoinGecko,
            )
        }
    }
}
