package com.rainyseason.cj.ticker.preview

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.ticker.TickerWidgetRenderParams
import com.rainyseason.cj.ticker.view.coinTickerPreviewView
import com.rainyseason.cj.ticker.view.settingTitleSummaryView
import java.util.concurrent.TimeUnit

class CoinTickerPreviewController(
    private val viewModel: CoinTickerPreviewViewModel,
    private val context: Context
) : AsyncEpoxyController() {


    private fun createInterval(interval: Long, unit: TimeUnit): String {
        val res = when (unit) {
            TimeUnit.MINUTES -> R.plurals.coin_ticker_preview_internal_minute_template
            TimeUnit.HOURS -> R.plurals.coin_ticker_preview_internal_hour_template
            TimeUnit.DAYS -> R.plurals.coin_ticker_preview_internal_day_template
            else -> error("not support")
        }
        return context.resources.getQuantityString(res, interval.toInt(), interval)
    }

    private fun buildRefreshInternal(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val refreshInterval = config.refreshInterval
        val refreshInternalUnit = config.refreshIntervalUnit
        settingTitleSummaryView {
            id("refresh_internal")
            title(R.string.coin_ticker_preview_refresh_internal)
            summary(createInterval(refreshInterval, refreshInternalUnit))
            onClickListener { _ ->
                val currentConfig = withState(viewModel) { it.config } ?: return@onClickListener
                val options = listOf(
                    15L to TimeUnit.MINUTES,
                    30L to TimeUnit.MINUTES,
                    1L to TimeUnit.HOURS,
                    2L to TimeUnit.HOURS,
                    3L to TimeUnit.HOURS,
                    6L to TimeUnit.HOURS,
                    12L to TimeUnit.HOURS,
                    1L to TimeUnit.DAYS,
                )
                val optionsString = options.map { createInterval(it.first, it.second) }
                val currentRefreshInterval = currentConfig.refreshInterval
                val currentRefreshInternalUnit = currentConfig.refreshIntervalUnit

                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_refresh_internal)
                    .setSingleChoiceItems(
                        optionsString.toTypedArray(),
                        options.indexOfFirst {
                            it.first == currentRefreshInterval && it.second == currentRefreshInternalUnit
                        }
                    ) { dialog, which ->
                        val select = options[which]
                        viewModel.setRefreshInternal(select.first, select.second)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

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

        val config = state.savedConfig.invoke() ?: return


        val priceDecimal = config.numberOfPriceDecimal
        settingTitleSummaryView {
            id("price-decimal")
            title(R.string.number_of_price_decimal)
            if (priceDecimal == null) {
                summary(R.string.setting_keep_original_price)
            } else {
                summary("$priceDecimal")
            }
            onClickListener { _ ->
                val options = listOf<Int?>(null) + (0..100)
                val optionsString = options.map {
                    it?.toString() ?: context.getString(R.string.setting_keep_original_price)
                }
                val currentState = withState(viewModel) { it }
                val currentPriceDecimal = currentState.config?.numberOfPriceDecimal
                AlertDialog.Builder(context)
                    .setTitle(R.string.number_of_price_decimal)
                    .setSingleChoiceItems(
                        optionsString.toTypedArray(),
                        options.indexOf(currentPriceDecimal)
                    ) { dialog, which ->
                        viewModel.setNumberOfDecimal(options[which].toString())
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        val percentDecimal = config.numberOfChangePercentDecimal
        settingTitleSummaryView {
            id("percent-decimal")
            title(R.string.number_of_change_percent_decimal)
            if (percentDecimal == null) {
                summary(R.string.setting_keep_original_price)
            } else {
                summary("$percentDecimal")
            }
            onClickListener { _ ->
                val options = listOf<Int?>(null) + (0..3)
                val optionsString = options.map {
                    it?.toString() ?: context.getString(R.string.setting_keep_original_price)
                }
                val currentState = withState(viewModel) { it }
                val currentPercentDecimal = currentState.config?.numberOfChangePercentDecimal
                AlertDialog.Builder(context)
                    .setTitle(R.string.number_of_price_decimal)
                    .setSingleChoiceItems(
                        optionsString.toTypedArray(),
                        options.indexOf(currentPercentDecimal)
                    ) { dialog, which ->
                        viewModel.setNumberOfChangePercentDecimal(options[which].toString())
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        buildRefreshInternal(state)
    }
}