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
}
