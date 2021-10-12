package com.rainyseason.cj.widget.watch

data class WatchRenderParams(
    val config: WatchConfig,
    val data: WatchDisplayData,
    val showLoading: Boolean = false,
    val isPreview: Boolean = false,
)