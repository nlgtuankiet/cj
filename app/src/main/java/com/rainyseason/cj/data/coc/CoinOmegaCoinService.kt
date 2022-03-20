package com.rainyseason.cj.data.coc

import com.rainyseason.cj.common.model.WatchlistCollection
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface CoinOmegaCoinService {
    @POST("backup/watchlist")
    suspend fun backupWatchlist(
        @Body collection: WatchlistCollection,
        @Query("schema_version") version: Int = 1,
    )
}
