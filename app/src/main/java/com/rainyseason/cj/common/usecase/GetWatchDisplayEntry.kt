package com.rainyseason.cj.common.usecase

import com.rainyseason.cj.ticker.CoinTickerDisplayData
import com.rainyseason.cj.ticker.usecase.GetDisplayData
import com.rainyseason.cj.widget.watch.WatchDisplayEntryContent
import com.rainyseason.cj.widget.watch.WatchDisplayEntryLoadParam
import javax.inject.Inject

class GetWatchDisplayEntry @Inject constructor(
    private val getDisplayData: GetDisplayData,
) {
    suspend operator fun invoke(param: WatchDisplayEntryLoadParam): WatchDisplayEntryContent {
        val params = CoinTickerDisplayData.LoadParam(
            coinId = param.coin.id,
            backend = param.coin.backend,
            dex = param.coin.dex,
            network = param.coin.network,
            currency = param.currency,
            changeInterval = param.changeInterval,
        )
        val data = getDisplayData(params)
        return WatchDisplayEntryContent(
            symbol = data.symbol,
            name = data.name,
            graph = data.priceGraph,
            price = data.price ?: 0.0,
            changePercent = data.priceChangePercent
        )
    }
}
