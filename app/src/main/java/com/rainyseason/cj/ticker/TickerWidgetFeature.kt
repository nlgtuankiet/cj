package com.rainyseason.cj.ticker

import com.rainyseason.cj.R
import com.rainyseason.cj.ticker.preview.CoinTickerPreviewController

enum class TickerWidgetFeature(
    val featureName: String,
    val viewId: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val predicate: () -> Boolean = { true },
) {
    CoinId(
        featureName = "coin_select",
        viewId = CoinTickerPreviewController.COIN_SELECT_ID,
        titleRes = R.string.coin_ticker_onboard_coin_select_title,
        descriptionRes = R.string.coin_ticker_onboard_coin_select_description
    ),
    MaterialYou(
        featureName = "widget_ticker_material_you",
        viewId = CoinTickerPreviewController.THEME_ID,
        titleRes = R.string.coin_ticker_onboard_mu_theme_title,
        descriptionRes = R.string.coin_ticker_onboard_mu_theme_description,
        predicate = {
            androidx.core.os.BuildCompat.isAtLeastS()
        }
    ),
    StickyNotification(
        featureName = "widget_ticker_notification",
        viewId = CoinTickerPreviewController.STICKY_NOTIFICATION,
        titleRes = R.string.coin_ticker_onboard_noti_title,
        descriptionRes = R.string.coin_ticker_onboard_noti_description,
    ),
    FullSize(
        featureName = "widget_ticker_full_size",
        viewId = CoinTickerPreviewController.FULL_SIZE_ID,
        titleRes = R.string.widget_ticker_onboard_maintain_aspect_ratio_title,
        descriptionRes = R.string.widget_ticker_onboard_maintain_aspect_ratio_description,
    ),
}
