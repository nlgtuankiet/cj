package com.rainyseason.cj.widget.watch

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.model.Theme
import com.rainyseason.cj.common.setCancelButton
import com.rainyseason.cj.common.view.SizeLabelFormatter
import com.rainyseason.cj.common.view.settingSliderView
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

        buildRefreshInternal(state)
        buildCurrency(state)
        buildTheme(state)
        buildSizeAdjustment(state)
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