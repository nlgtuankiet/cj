package com.rainyseason.cj.widget.watch

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.MainActivity
import com.rainyseason.cj.R
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.model.Theme
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.setCancelButton
import com.rainyseason.cj.common.view.IntLabelFormater
import com.rainyseason.cj.common.view.PercentLabelFormatrer
import com.rainyseason.cj.common.view.SizeLabelFormatter
import com.rainyseason.cj.common.view.settingAdvanceView
import com.rainyseason.cj.common.view.settingHeaderView
import com.rainyseason.cj.common.view.settingSliderView
import com.rainyseason.cj.common.view.settingSwitchView
import com.rainyseason.cj.common.view.settingTitleSummaryView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

class WatchController @AssistedInject constructor(
    @Assisted val viewModel: WatchPreviewViewModel,
    @Assisted private val context: Context,
) : AsyncEpoxyController() {

    override fun buildModels() {
        val state: WatchPreviewState = withState(viewModel) { it }
        buildEditWatchList(state)
        buildRefreshInternal(state)
        buildCurrency(state)
        buildTheme(state)
        buildSizeAdjustment(state)
        buildBackgroundTransparency(state)

        buildAdvanceSettingTitle(state)
        buildAdvanceSettings(state)
    }

    private fun buildEditWatchList(state: WatchPreviewState) {
        maybeBuildHorizontalSeparator(id = "edit_watchlist_separator")

        settingTitleSummaryView {
            id("edit_watchlist")
            title(R.string.watch_edit_watch_list)
            summary(R.string.watch_edit_watch_list_summary)
            onClickListener { _ ->
                context.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        putExtra(MainActivity.SCREEN_TO_OPEN_EXTRA, R.id.watch_list_screen)
                    }
                )
            }
        }
    }

    private fun buildAdvanceSettings(state: WatchPreviewState) {
        if (!state.showAdvanceSetting) {
            return
        }

        buildPriceFormatGroup(state)
        buildChangePercentGroup(state)
        buildBehaviorGroup(state)
    }

    private fun buildBehaviorGroup(state: WatchPreviewState) {
        settingHeaderView {
            id("setting_header_behavior")
            content(R.string.setting_behavior_title)
        }

        buildBatteryWarning(state)
        buildClickAction(state)
    }

    private fun buildClickAction(state: WatchPreviewState) {
        val config = state.config ?: return
        val optionsToString = listOf(
            WatchClickAction.OpenWatchlist
                to R.string.watch_preview_click_action_open_watchlist,
            WatchClickAction.Refresh
                to R.string.coin_ticker_preview_setting_header_click_action_refresh,
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

    private fun buildBatteryWarning(state: WatchPreviewState) {
        val config = state.config ?: return
        maybeBuildHorizontalSeparator(id = "show_battery_warning_separator")

        settingSwitchView {
            id("show_battery_warning")
            title(R.string.coin_ticker_preview_setting_show_battery_warning)
            checked(config.showBatteryWarning)
            onClickListener { _ ->
                viewModel.switchShowBatteryWarning()
            }
        }
    }

    private fun buildChangePercentGroup(state: WatchPreviewState) {

        settingHeaderView {
            id("setting_header_change_percent")
            content(R.string.setting_price_change_percent_title)
        }

        buildChangePercentInterval(state)
        buildPercentDecimal(state)
    }

    private fun buildPercentDecimal(state: WatchPreviewState) {
        val config = state.config ?: return
        val percentDecimal = config.numberOfChangePercentDecimal
        maybeBuildHorizontalSeparator(id = "percent_decimal_separator")

        settingSliderView {
            id("percent_decimal")
            title(R.string.number_of_change_percent_decimal)
            valueFrom(0)
            valueTo(5)
            value(percentDecimal)
            stepSize(1)
            labelFormatter(IntLabelFormater)
            onChangeListener { value ->
                viewModel.setNumberOfChangePercentDecimal(value)
            }
        }
    }

    private fun buildChangePercentInterval(state: WatchPreviewState) {
        val config = state.config ?: return
        val mapping = listOf(
            TimeInterval.I_24H
                to R.string.coin_ticker_preview_setting_bottom_change_percent_interval_24h,
            TimeInterval.I_7D
                to R.string.coin_ticker_preview_setting_bottom_change_percent_interval_7d,
            TimeInterval.I_30D
                to R.string.coin_ticker_preview_setting_bottom_change_percent_interval_30d,
            TimeInterval.I_90D
                to R.string.coin_ticker_preview_setting_bottom_change_percent_interval_90d,
            TimeInterval.I_1Y
                to R.string.coin_ticker_preview_setting_bottom_change_percent_interval_1y,
        ).toMap()

        val interval = config.interval

        maybeBuildHorizontalSeparator(id = "change_interval_separator")

        settingTitleSummaryView {
            id("change_interval")
            title(R.string.coin_ticker_preview_setting_bottom_change_percent_internal_header)
            summary(context.getString(mapping[interval]!!))
            onClickListener { _ ->
                val currentState = withState(viewModel) { it }
                val currentConfig = currentState.config!!
                val options = listOf(
                    TimeInterval.I_24H,
                    TimeInterval.I_7D,
                    TimeInterval.I_30D,
                    TimeInterval.I_90D,
                    TimeInterval.I_1Y,
                )
                val currentInterval = currentConfig.interval
                AlertDialog.Builder(context)
                    .setTitle(
                        R.string.coin_ticker_preview_setting_bottom_change_percent_internal_header
                    )
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

    private fun buildPriceFormatGroup(state: WatchPreviewState) {

        settingHeaderView {
            id("setting_header_price_format")
            content(R.string.setting_price_format_title)
        }
        buildAmountDecimal(state)
        buildRoundToMillion(state)
        buildShowThousandSeparator(state)
        buildHideDecimalOnLargePrice(state)
        buildShowCurrencySymbol(state)
    }

    private fun buildShowCurrencySymbol(state: WatchPreviewState) {
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

    private fun buildHideDecimalOnLargePrice(state: WatchPreviewState) {
        val config = state.config ?: return
        maybeBuildHorizontalSeparator(id = "hide_decimal_separator")

        settingSwitchView {
            id("hide_decimal")
            title(R.string.coin_ticker_preview_setting_hide_decimal)
            checked(config.hideDecimalOnLargePrice)
            onClickListener { _ ->
                viewModel.switchHideDecimalOnLargePrice()
            }
        }
    }

    private fun buildShowThousandSeparator(state: WatchPreviewState) {
        val config = state.config ?: return

        maybeBuildHorizontalSeparator(id = "show_thousand_separator_separator")

        settingSwitchView {
            id("show_thousand_separator")
            title(R.string.coin_ticker_preview_setting_show_thousands_separator)
            checked(config.showThousandsSeparator)
            onClickListener { _ ->
                viewModel.switchShowThousandsSeparator()
            }
        }
    }

    private fun buildRoundToMillion(state: WatchPreviewState) {
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

    private fun buildAmountDecimal(state: WatchPreviewState) {
        val config = state.config ?: return
        val priceDecimal = config.numberOfAmountDecimal

        maybeBuildHorizontalSeparator(id = "amount_decimal_separator")

        settingSliderView {
            id("amount_decimal")
            title(R.string.number_of_price_decimal)
            labelFormatter(IntLabelFormater)
            valueFrom(0)
            valueTo(15)
            stepSize(1)
            value(priceDecimal)
            onChangeListener { value ->
                viewModel.setNumberOfDecimal(value)
            }
        }
    }

    private fun buildAdvanceSettingTitle(state: WatchPreviewState) {
        if (state.showAdvanceSetting) {
            return
        }
        settingAdvanceView {
            id("setting_advance_view")
            title(R.string.setting_show_advance)
            summary(R.string.setting_show_advance_summary)
            onClickListener { _ ->
                viewModel.showAdvanced()
            }
        }
    }

    private fun buildBackgroundTransparency(state: WatchPreviewState) {
        val config = state.config ?: return
        maybeBuildHorizontalSeparator(id = "separator_background_transparency")

        settingSliderView {
            id("setting_background_transparency")
            title(R.string.coin_ticker_preview_setting_background_transparency)
            valueFrom(0)
            valueTo(100)
            stepSize(5)
            value(config.backgroundTransparency)
            labelFormatter(PercentLabelFormatrer)
            onChangeListener { newValue ->
                viewModel.setBackgroundTransparency(newValue)
            }
        }
    }

    private fun buildSizeAdjustment(state: WatchPreviewState) {
        val config = state.config ?: return
        maybeBuildHorizontalSeparator(id = "size_adjustment_separator")

        settingSliderView {
            id("setting_size_adjustment")
            title(R.string.coin_ticker_preview_setting_size_adjustment)
            valueFrom(-24)
            valueTo(24)
            stepSize(4)
            value(config.sizeAdjustment)
            labelFormatter(SizeLabelFormatter)
            onChangeListener { value ->
                viewModel.setAdjustment(value)
            }
        }
    }

    private fun buildTheme(state: WatchPreviewState) {
        val config = state.config ?: return
        val theme = config.theme
        val themeToSummary = mapOf(
            Theme.Auto to R.string.coin_ticker_preview_setting_theme_default,
            Theme.Light to R.string.coin_ticker_preview_setting_theme_light,
            Theme.Dark to R.string.coin_ticker_preview_setting_theme_dark,
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

    private fun buildCurrency(state: WatchPreviewState) {
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

    private var addSeparator = true

    private fun maybeBuildHorizontalSeparator(id: String) {
        if (!addSeparator) {
            addSeparator = true
            return
        }
        // temporary disable for dark mode
        // horizontalSeparatorView {
        //     id(id)
        // }
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

    private fun buildRefreshInternal(state: WatchPreviewState) {
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
                            it.first == currentRefreshInterval &&
                                it.second == currentRefreshInternalUnit
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

    @AssistedFactory
    interface Factory {
        fun create(viewModel: WatchPreviewViewModel, context: Context): WatchController
    }
}
