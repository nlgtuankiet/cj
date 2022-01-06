package com.rainyseason.cj.widget.watch

import com.rainyseason.cj.common.model.WidgetRenderParams

data class WatchWidgetRenderParams(
    val config: WatchConfig,
    val data: WatchDisplayData,
    val showLoading: Boolean = false,
    val isPreview: Boolean = false,
) : WidgetRenderParams
