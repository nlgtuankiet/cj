package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.model.Backend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

class SearchBackendProducts @Inject constructor(
    private val searchDexScreenerProduct: SearchDexScreenerProduct
) {

    operator fun invoke(backend: Backend, query: String): Flow<List<BackendProduct>> {
        Timber.d("backend: $backend query: $query")
        return when (backend) {
            Backend.DexScreener -> searchDexScreenerProduct.invoke(query)
            else -> flow {
                throw IllegalArgumentException("Not support backend: ${backend.id}")
            }
        }
    }
}
