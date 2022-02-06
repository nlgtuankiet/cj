package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.data.dexscreener.DexScreenerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SearchDexScreenerProduct @Inject constructor(
    private val dexScreenerService: DexScreenerService,
) {

    fun invoke(query: String): Flow<List<BackendProduct>> {
        return flow {
            val searchResult = dexScreenerService.search(query)
            val mapResult = searchResult.pairs.map { pair ->
                BackendProduct(
                    id = pair.pairAddress,
                    symbol = pair.symbol(),
                    displayName = pair.baseToken.name,
                    backend = Backend.DexScreener,
                    iconUrl = Backend.DexScreener.iconUrl,
                    network = pair.platformId,
                    dex = pair.dexId,
                )
            }
            emit(mapResult)
        }
    }
}
