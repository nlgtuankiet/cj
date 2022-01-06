package com.rainyseason.cj.ticker

import android.graphics.Bitmap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TickerWidgetGlideLoader @Inject constructor(
    private val fetcherFactory: TickerWidgetGlideFetcher.Factory
) : ModelLoader<CoinTickerRenderParams, Bitmap>,
    ModelLoaderFactory<CoinTickerRenderParams, Bitmap> {
    override fun buildLoadData(
        model: CoinTickerRenderParams,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<Bitmap> {
        return ModelLoader.LoadData(ObjectKey(model), fetcherFactory.create(model))
    }

    override fun handles(model: CoinTickerRenderParams): Boolean {
        return true
    }

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<CoinTickerRenderParams, Bitmap> {
        return this
    }

    override fun teardown() {
    }
}