package com.rainyseason.cj.ticker

import android.graphics.Bitmap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.rainyseason.cj.common.model.WidgetRenderParams
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetGlideLoader @Inject constructor(
    private val fetcherFactory: WidgetGlideFetcher.Factory
) : ModelLoader<WidgetRenderParams, Bitmap>,
    ModelLoaderFactory<WidgetRenderParams, Bitmap> {
    override fun buildLoadData(
        model: WidgetRenderParams,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<Bitmap> {
        return ModelLoader.LoadData(ObjectKey(model), fetcherFactory.create(model))
    }

    override fun handles(model: WidgetRenderParams): Boolean {
        return true
    }

    override fun build(
        multiFactory: MultiModelLoaderFactory
    ): ModelLoader<WidgetRenderParams, Bitmap> {
        return this
    }

    override fun teardown() {
    }
}
