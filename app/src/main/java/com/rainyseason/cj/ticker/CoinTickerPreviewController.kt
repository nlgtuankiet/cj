package com.rainyseason.cj.ticker

import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.ticker.view.coinTickerPreviewView
import com.rainyseason.cj.ticker.view.settingNumberView

class CoinTickerPreviewController(
    private val viewModel: CoinTickerSettingViewModel,
) : AsyncEpoxyController() {
    override fun buildModels() {
        val state = withState(viewModel) { it }
        val viewModel = viewModel

        val savedConfig = state.savedConfig.invoke()
        val savedDisplayData = state.savedDisplayData.invoke()
        val userCurrency = state.userCurrency.invoke()
        val params = if (savedConfig != null && savedDisplayData != null && userCurrency != null) {
            TickerWidgetRenderParams(
                userCurrency = userCurrency,
                config = savedConfig,
                data = savedDisplayData,
                showLoading = false,
                clickToUpdate = false
            )
        } else {
            null
        }

        coinTickerPreviewView {
            id("preview")
            renderParams(params)
        }

        settingNumberView {
            id("number-of-decimal")
            hint("Number of decimal")
            value(state.numberOfDecimal?.toString() ?: "")
            textChangeListener {
                viewModel.setNumberOfDecimal(it)
            }
        }
    }
}