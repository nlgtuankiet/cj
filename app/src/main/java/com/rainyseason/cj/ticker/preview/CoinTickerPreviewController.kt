package com.rainyseason.cj.ticker.preview

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.Theme
import com.rainyseason.cj.ticker.BottomContentType
import com.rainyseason.cj.ticker.ChangeInterval
import com.rainyseason.cj.ticker.TickerWidgetRenderParams
import com.rainyseason.cj.ticker.list.view.coinTickerListHeaderView
import com.rainyseason.cj.ticker.view.coinTickerPreviewView
import com.rainyseason.cj.ticker.view.settingSwitchView
import com.rainyseason.cj.ticker.view.settingTitleSummaryView
import java.util.concurrent.TimeUnit

class CoinTickerPreviewController(
    private val viewModel: CoinTickerPreviewViewModel,
    private val context: Context
) : AsyncEpoxyController() {

    override fun buildModels() {
        val state = withState(viewModel) { it }
        val config = state.savedConfig.invoke() ?: return


        buildPreview(state)
        buildCommonSetting(state)
        buildBottomSetting(state)

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


        buildShowThousandSeparator(state)
    }

    private fun buildShowThousandSeparator(state: CoinTickerPreviewState) {
        val config = state.config ?: return

        settingSwitchView {
            id("show-thousand_separator")
            title(R.string.coin_ticker_preview_setting_show_thousands_separator)
            checked(config.showThousandsSeparator)
            onClickListener { v ->
                viewModel.switchThousandsSeparator()
            }
        }
    }


    private fun buildExtraSize(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val extra = config.extraSize
        val values = listOf(0, 10, 20, 30)
        val valueToSummary = values.associateWith {
            if (it == 0) {
                context.getString(R.string.coin_ticker_preview_setting_extra_size_none)
            } else {
                "+$it"
            }
        }

        settingTitleSummaryView {
            id("setting-extra-size")
            title(R.string.coin_ticker_preview_setting_extra_size)
            summary(valueToSummary.values.toList()[values.indexOf(extra)])
            onClickListener { _ ->
                val currentConfig = withState(viewModel) { it.config } ?: return@onClickListener
                val selectedExtra = currentConfig.extraSize

                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_refresh_internal)
                    .setSingleChoiceItems(
                        valueToSummary.values.toTypedArray(),
                        valueToSummary.keys.indexOfFirst { selectedExtra == it },
                    ) { dialog, which ->
                        val select = values[which]
                        viewModel.setExtraSize(select)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun createInterval(interval: Long, unit: TimeUnit): String {
        val res = when (unit) {
            TimeUnit.MINUTES -> R.plurals.coin_ticker_preview_internal_minute_template
            TimeUnit.HOURS -> R.plurals.coin_ticker_preview_internal_hour_template
            TimeUnit.DAYS -> R.plurals.coin_ticker_preview_internal_day_template
            else -> error("not support")
        }
        return context.resources.getQuantityString(res, interval.toInt(), interval)
    }

    private fun buildTheme(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val theme = config.theme
        val themeToSummary = mapOf(
            Theme.AUTO to R.string.coin_ticker_preview_setting_theme_default,
            Theme.LIGHT to R.string.coin_ticker_preview_setting_theme_light,
            Theme.DARK to R.string.coin_ticker_preview_setting_theme_dark,
        )

        settingTitleSummaryView {
            id("setting-theme")
            title(R.string.coin_ticker_preview_setting_theme)
            summary(context.getString(themeToSummary[theme]!!))
            onClickListener { _ ->
                val currentConfig = withState(viewModel) { it.config } ?: return@onClickListener
                val themeToSummaryString = themeToSummary.mapValues { context.getString(it.value) }
                val selectedTheme = currentConfig.theme
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_refresh_internal)
                    .setSingleChoiceItems(
                        themeToSummaryString.values.toTypedArray(),
                        themeToSummaryString.keys.indexOfFirst { selectedTheme == it },
                    ) { dialog, which ->
                        val select = themeToSummaryString.keys.toList()[which]
                        viewModel.setTheme(select)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
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


    private fun buildPreview(state: CoinTickerPreviewState) {
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
    }


    private fun buildCommonSetting(state: CoinTickerPreviewState) {
        coinTickerListHeaderView {
            id("common-header")
            content(R.string.coin_ticker_preview_setting_header_common)
        }
        buildRefreshInternal(state)
        buildTheme(state)
        buildExtraSize(state)
    }

    private fun buildBottomContentType(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val contentType = config.bottomContentType
        val summary = when (contentType) {
            BottomContentType.PRICE -> R.string.coin_ticker_preview_setting_bottom_content_price
            BottomContentType.MARKET_CAP -> R.string.coin_ticker_preview_setting_bottom_content_market_cap
            else -> error("contentType: $contentType")
        }
        settingTitleSummaryView {
            id("bottom-content-type")
            title(R.string.coin_ticker_preview_setting_bottom_content_type)
            summary(summary)
            onClickListener { _ ->
                val options = listOf(
                    BottomContentType.PRICE to R.string.coin_ticker_preview_setting_bottom_content_price,
                    BottomContentType.MARKET_CAP to R.string.coin_ticker_preview_setting_bottom_content_market_cap,
                )
                val currentState = withState(viewModel) { it }
                val currentContentType = currentState.config!!.bottomContentType
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_setting_bottom_content_type)
                    .setSingleChoiceItems(
                        options.map { context.getString(it.second) }.toTypedArray(),
                        options.indexOfFirst { it.first == currentContentType }
                    ) { dialog, which ->
                        viewModel.setBottomContentType(options[which].first)
                        dialog.dismiss()
                    }
                    .show()
            }
        }

    }

    private fun buildChangePercentInternal(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val mapping = listOf(
            ChangeInterval._24H to R.string.coin_ticker_preview_setting_bottom_change_percent_interval_24h,
            ChangeInterval._7D to R.string.coin_ticker_preview_setting_bottom_change_percent_interval_7d,
            ChangeInterval._14D to R.string.coin_ticker_preview_setting_bottom_change_percent_interval_14d,
            ChangeInterval._30D to R.string.coin_ticker_preview_setting_bottom_change_percent_interval_30d,
            ChangeInterval._60D to R.string.coin_ticker_preview_setting_bottom_change_percent_interval_60d,
            ChangeInterval._1Y to R.string.coin_ticker_preview_setting_bottom_change_percent_interval_1y,
        ).toMap()

        val interval = config.bottomInterval

        settingTitleSummaryView {
            id("bottom-change-interval")
            title(R.string.coin_ticker_preview_setting_bottom_change_percent_internal_header)
            summary(context.getString(mapping[interval]!!))
            onClickListener { _ ->
                val currentState = withState(viewModel) { it }
                val currentConfig = currentState.config!!
                val options = when (currentConfig.bottomContentType) {
                    BottomContentType.PRICE -> ChangeInterval.ALL_PRICE_INTERVAL
                    BottomContentType.MARKET_CAP -> ChangeInterval.ALL_MARKET_CAP_INTERVAL
                    else -> error("unknown ${config.bottomContentType}")
                }


                val currentInterval = currentConfig.bottomInterval
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_setting_bottom_content_type)
                    .setSingleChoiceItems(
                        options.map { context.getString(mapping[it]!!) }.toTypedArray(),
                        options.indexOfFirst { it == currentInterval }
                    ) { dialog, which ->
                        val selectedInterval = options[which]
                        when (currentConfig.bottomContentType) {
                            BottomContentType.PRICE -> viewModel.setPriceChangeInterval(
                                selectedInterval
                            )
                            BottomContentType.MARKET_CAP -> viewModel.setMarketCapChangeInterval(
                                selectedInterval
                            )
                        }
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun buildBottomSetting(state: CoinTickerPreviewState) {
        coinTickerListHeaderView {
            id("bottom-header")
            content(R.string.coin_ticker_preview_setting_header_bottom)
        }
        buildBottomContentType(state)
        buildChangePercentInternal(state)
    }

}