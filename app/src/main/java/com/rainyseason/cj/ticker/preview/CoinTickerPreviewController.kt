package com.rainyseason.cj.ticker.preview

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.Theme
import com.rainyseason.cj.common.getUserErrorMessage
import com.rainyseason.cj.common.setCancelButton
import com.rainyseason.cj.common.view.horizontalSeparatorView
import com.rainyseason.cj.common.view.retryView
import com.rainyseason.cj.common.view.settingHeaderView
import com.rainyseason.cj.common.view.settingSwitchView
import com.rainyseason.cj.common.view.settingTitleSummaryView
import com.rainyseason.cj.ticker.BottomContentType
import com.rainyseason.cj.ticker.ChangeInterval
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.ticker.view.CoinTickerPreviewViewModel_
import java.util.concurrent.TimeUnit

class CoinTickerPreviewController(
    private val viewModel: CoinTickerPreviewViewModel,
    private val context: Context
) : AsyncEpoxyController() {

    private var addSeparator = true

    private fun maybeBuildHorizontalSeparator(id: String) {
        if (!addSeparator) {
            addSeparator = true
            return
        }
        horizontalSeparatorView {
            id(id)
        }
    }

    override fun buildModels() {
        val state = withState(viewModel) { it }
        val buildRetryResult = buildRetry(state)
        if (buildRetryResult == BuildState.Stop) {
            return
        }
        buildStyle(state)
        buildBehaviorSetting(state)
        buildCurrencyGroup(state)
        buildBottomSetting(state)
    }

    private fun buildRetry(state: CoinTickerPreviewState): BuildState {
        val errors = listOf(
            (state.coinDetailResponse as? Fail)?.error,
        ) + state.marketChartResponse.values.map {
            (it as? Fail)?.error
        }
        val error = errors.firstOrNull { it != null } ?: return BuildState.Next
        retryView {
            id("retry")
            reason(error.getUserErrorMessage(context = context))
            buttonText(R.string.reload)
            onRetryClickListener { _ ->
                viewModel.reload()
            }
        }
        return BuildState.Stop
    }


    private fun buildCurrencyGroup(state: CoinTickerPreviewState) {
        settingHeaderView {
            id("header_currency_group")
            content(R.string.coin_ticker_preview_setting_header_currency_group)
        }
        addSeparator = false
        buildCurrency(state)
        buildAmountDecimal(state)
        buildRoundToMillion(state)
        buildShowCurrencySymbol(state)
        buildShowThousandSeparator(state)
    }

    private fun buildStyle(state: CoinTickerPreviewState) {
        settingHeaderView {
            id("header_style")
            content(R.string.coin_ticker_preview_setting_header_style)
        }
        addSeparator = false
        buildLayout(state)
        buildTheme(state)
    }

    private fun buildLayout(state: CoinTickerPreviewState) {
        val config = state.config ?: return

        val layoutToString = listOf(
            CoinTickerConfig.Layout.DEFAULT to R.string.coin_ticket_style_default,
            CoinTickerConfig.Layout.GRAPH to R.string.coin_ticket_style_graph,
            CoinTickerConfig.Layout.COIN360 to R.string.coin_ticket_style_coin360,
        ).map { it.first to context.getString(it.second) }

        maybeBuildHorizontalSeparator(id = "header_layout_separator")

        settingTitleSummaryView {
            id("setting_layout")
            title(R.string.coin_ticker_preview_setting_layout)
            summary(layoutToString.first { it.first == config.layout }.second)
            onClickListener { _ ->
                val currentLayout =
                    withState(viewModel) { it.config?.layout } ?: return@onClickListener
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_setting_layout)
                    .setCancelButton()
                    .setSingleChoiceItems(
                        layoutToString.map { it.second }.toTypedArray(),
                        layoutToString.indexOfFirst { currentLayout == it.first },
                    ) { dialog, which ->
                        val select = layoutToString[which].first
                        viewModel.setLayout(select)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }


    private fun buildShowCurrencySymbol(state: CoinTickerPreviewState) {
        val config = state.config ?: return

        maybeBuildHorizontalSeparator(id = "show_currency_separator")

        settingSwitchView {
            id("setting_show_currency")
            title(R.string.coin_ticker_preview_setting_show_currency_symbol)
            checked(config.showCurrencySymbol)
            onClickListener { _ ->
                viewModel.switchShowCurrency()
            }
        }
    }


    private fun buildShowThousandSeparator(state: CoinTickerPreviewState) {
        val config = state.config ?: return

        maybeBuildHorizontalSeparator(id = "show_thousand_separator_separator")

        settingSwitchView {
            id("show_thousand_separator")
            title(R.string.coin_ticker_preview_setting_show_thousands_separator)
            checked(config.showThousandsSeparator)
            onClickListener { _ ->
                viewModel.switchThousandsSeparator()
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
        maybeBuildHorizontalSeparator(id = "setting_theme_separator")
        settingTitleSummaryView {
            id("setting-theme")
            title(R.string.coin_ticker_preview_setting_theme)
            summary(context.getString(themeToSummary[theme]!!))
            onClickListener { _ ->
                val currentConfig = withState(viewModel) { it.config } ?: return@onClickListener
                val themeToSummaryString = themeToSummary.mapValues { context.getString(it.value) }
                val selectedTheme = currentConfig.theme
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_setting_theme)
                    .setCancelButton()
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
        maybeBuildHorizontalSeparator(id = "refresh_internal_separator")
        settingTitleSummaryView {
            id("refresh_internal")
            title(R.string.coin_ticker_preview_refresh_interval)
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
                    .setTitle(R.string.coin_ticker_preview_refresh_interval)
                    .setCancelButton()
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


    private fun buildBehaviorSetting(state: CoinTickerPreviewState) {
        settingHeaderView {
            id("behavior_header")
            content(R.string.coin_ticker_preview_setting_header_behavior)
        }
        addSeparator = false
        buildClickAction(state)
        buildRefreshInternal(state)
    }

    private fun buildCurrency(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val currencyCodeToString = SUPPORTED_CURRENCY.mapValues {
            it.value.name
        }.toList().sortedBy { it.first }


        val selectedOption = config.currency
        maybeBuildHorizontalSeparator(id = "setting_currency_separator")
        settingTitleSummaryView {
            id("setting_currency")
            title(R.string.coin_ticker_preview_setting_header_currency)
            summary(currencyCodeToString.toMap()[selectedOption]!!)
            onClickListener { _ ->
                val currentState = withState(viewModel) { it }
                val currentOption = currentState.config!!.currency
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_setting_header_currency)
                    .setCancelButton()
                    .setSingleChoiceItems(
                        currencyCodeToString.map { it.second }.toTypedArray(),
                        currencyCodeToString.indexOfFirst { it.first == currentOption }
                    ) { dialog, which ->
                        viewModel.setCurrency(currencyCodeToString[which].first)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun buildClickAction(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val optionsToString = listOf(
            CoinTickerConfig.ClickAction.REFRESH to R.string.coin_ticker_preview_setting_header_click_action_refresh,
            CoinTickerConfig.ClickAction.SETTING to R.string.coin_ticker_preview_setting_header_click_action_setting,
            CoinTickerConfig.ClickAction.SWITCH_PRICE_MARKET_CAP to R.string.coin_ticker_preview_setting_header_click_action_switch,
        ).map { it.first to context.getString(it.second) }
        val selectedOption = config.clickAction
        maybeBuildHorizontalSeparator(id = "setting_click_action_separator")
        settingTitleSummaryView {
            id("setting_click_action")
            title(R.string.coin_ticker_preview_setting_header_click_action)
            summary(optionsToString.toMap()[selectedOption]!!)
            onClickListener { _ ->
                val currentState = withState(viewModel) { it }
                val currentOption = currentState.config!!.clickAction
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_setting_header_click_action)
                    .setCancelButton()
                    .setSingleChoiceItems(
                        optionsToString.map { it.second }.toTypedArray(),
                        optionsToString.indexOfFirst { it.first == currentOption }
                    ) { dialog, which ->
                        viewModel.setClickAction(optionsToString[which].first)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun buildBottomContentType(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val summary = when (val contentType = config.bottomContentType) {
            BottomContentType.PRICE -> R.string.coin_ticker_preview_setting_bottom_content_price
            BottomContentType.MARKET_CAP -> R.string.coin_ticker_preview_setting_bottom_content_market_cap
            else -> error("contentType: $contentType")
        }
        maybeBuildHorizontalSeparator(id = "bottom_content_type_separator")
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
                    .setCancelButton()
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
            ChangeInterval._1Y to R.string.coin_ticker_preview_setting_bottom_change_percent_interval_1y,
        ).toMap()

        val interval = config.changeInterval

        maybeBuildHorizontalSeparator(id = "bottom_change_interval_separator")

        settingTitleSummaryView {
            id("bottom_change_interval")
            title(R.string.coin_ticker_preview_setting_bottom_change_percent_internal_header)
            summary(context.getString(mapping[interval]!!))
            onClickListener { _ ->
                val currentState = withState(viewModel) { it }
                val currentConfig = currentState.config!!
                val options = ChangeInterval.ALL_PRICE_INTERVAL
                val currentInterval = currentConfig.changeInterval
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_setting_bottom_change_percent_internal_header)
                    .setCancelButton()
                    .setSingleChoiceItems(
                        options.map { context.getString(mapping[it]!!) }.toTypedArray(),
                        options.indexOfFirst { it == currentInterval }
                    ) { dialog, which ->
                        val selectedInterval = options[which]
                        viewModel.setPriceChangeInterval(selectedInterval)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun buildBottomSetting(state: CoinTickerPreviewState) {
        settingHeaderView {
            id("bottom-header")
            content(R.string.coin_ticker_preview_setting_header_bottom)
        }
        addSeparator = false
        buildBottomContentType(state)
        buildChangePercentInternal(state)
        buildPercentDecimal(state)
    }


    private fun buildRoundToMillion(state: CoinTickerPreviewState) {
        val config = state.config ?: return

        maybeBuildHorizontalSeparator(id = "round_to_million_separator")

        settingSwitchView {
            id("round_to_million")
            title(R.string.coin_ticker_preview_setting_round_to_million)
            checked(config.roundToMillion)
            onClickListener { _ ->
                viewModel.switchRoundToMillion()
            }
        }
    }

    private fun buildPercentDecimal(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val percentDecimal = config.numberOfChangePercentDecimal
        maybeBuildHorizontalSeparator(id = "percent_decimal_separator")
        settingTitleSummaryView {
            id("percent_decimal")
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
                    .setTitle(R.string.number_of_change_percent_decimal)
                    .setCancelButton()
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
    }

    private fun buildAmountDecimal(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val priceDecimal = config.numberOfAmountDecimal

        maybeBuildHorizontalSeparator(id = "amount_decimal_separator")
        settingTitleSummaryView {
            id("amount_decimal")
            title(R.string.number_of_price_decimal)
            if (priceDecimal == null) {
                summary(R.string.setting_keep_original_price)
            } else {
                summary("$priceDecimal ${context.getString(R.string.number_of_price_decimal_explain)}")
            }
            onClickListener { _ ->
                val options = listOf<Int?>(null) + (0..100)
                val optionsString = options.map {
                    it?.toString() ?: context.getString(R.string.setting_keep_original_price)
                }
                val currentState = withState(viewModel) { it }
                val currentPriceDecimal = currentState.config?.numberOfAmountDecimal
                AlertDialog.Builder(context)
                    .setTitle(R.string.number_of_price_decimal)
                    .setCancelButton()
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
    }

    override fun isStickyHeader(position: Int): Boolean {
        return adapter.getModelAtPosition(position) is CoinTickerPreviewViewModel_
    }

}