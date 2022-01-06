package com.rainyseason.cj.ticker

import android.graphics.Bitmap
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.lang.Exception

// TODO improve by load bitmap from a bitmap pool
class TickerWidgetGlideFetcher @AssistedInject constructor(
    @Assisted private val params: CoinTickerRenderParams,
    private val renderer: TickerWidgetRenderer,
) : DataFetcher<Bitmap> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap>) {
        try {
            val bitmap = renderer.createBitmap(params, null)
            callback.onDataReady(bitmap)
        } catch (ex: Exception) {
            callback.onLoadFailed(ex)
        }
    }

    override fun cleanup() {
    }

    override fun cancel() {
    }

    override fun getDataClass(): Class<Bitmap> {
        return Bitmap::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.MEMORY_CACHE
    }

    @AssistedFactory
    interface Factory {
        fun create(params: CoinTickerRenderParams): TickerWidgetGlideFetcher
    }
}