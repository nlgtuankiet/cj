package com.rainyseason.cj.widget.watch

data class WatchWidgetRenderParams(
    val config: WatchConfig,
    val data: WatchDisplayData,
    val showLoading: Boolean = false,
    val isPreview: Boolean = false,
)