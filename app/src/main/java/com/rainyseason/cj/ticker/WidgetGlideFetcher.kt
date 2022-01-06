package com.rainyseason.cj.ticker

import android.graphics.Bitmap
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.rainyseason.cj.common.model.WidgetRenderParams
import com.rainyseason.cj.widget.watch.WatchWidgetRender
import com.rainyseason.cj.widget.watch.WatchWidgetRenderParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.lang.Exception

// TODO improve by load bitmap from a bitmap pool
class WidgetGlideFetcher @AssistedInject constructor(
    @Assisted private val params: WidgetRenderParams,
    private val tickerWidgetRenderer: TickerWidgetRenderer,
    private val watchWidgetRender: WatchWidgetRender,
) : DataFetcher<Bitmap> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap>) {
        try {
            val bitmap = when (params) {
                is WatchWidgetRenderParams -> watchWidgetRender.createBitmap(params)
                is CoinTickerRenderParams -> tickerWidgetRenderer.createBitmap(params, null)
                else -> error("Unknown param $params")
            }
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
        fun create(params: WidgetRenderParams): WidgetGlideFetcher
    }
}
