package com.rainyseason.cj.ticker

import android.content.Context
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.ticker.view.coinTickerPreviewView
import com.rainyseason.cj.ticker.view.settingNumberView

class CoinTickerPreviewController(
    private val viewModel: CoinTickerSettingViewModel,
    private val context: Context
) : AsyncEpoxyController() {
    override fun buildModels() {
        val state = withState(viewModel) { it }

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

        val numberOfPriceDecimal = state.config?.numberOfPriceDecimal
        settingNumberView {
            id("number-of-price-decimal")
            hint(R.string.number_of_price_decimal)
            value(numberOfPriceDecimal?.toString() ?: "")
            textChangeListener {
                viewModel.setNumberOfDecimal(it)
            }
        }

        val numberOfChangePercentDecimal = state.config?.numberOfChangePercentDecimal
        settingNumberView {
            id("number-of-change-percent-decimal")
            hint(R.string.number_of_change_percent_decimal)
            value(numberOfChangePercentDecimal?.toString() ?: "")
            textChangeListener {
                viewModel.setNumberOfChangePercentDecimal(it)
            }
        }
    }
}