package com.rainyseason.cj.ticker.preview

import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.navigation.findNavController
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.withState
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.R
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.NumberFormater
import com.rainyseason.cj.common.RefreshIntervals
import com.rainyseason.cj.common.getNonNullCurrencyInfo
import com.rainyseason.cj.common.getUserErrorMessage
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.common.model.Theme
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.resolveColorAttr
import com.rainyseason.cj.common.setCancelButton
import com.rainyseason.cj.common.setResetButton
import com.rainyseason.cj.common.showKeyboard
import com.rainyseason.cj.common.view.IntLabelFormater
import com.rainyseason.cj.common.view.PercentLabelFormatrer
import com.rainyseason.cj.common.view.SizeLabelFormatter
import com.rainyseason.cj.common.view.retryView
import com.rainyseason.cj.common.view.settingAdvanceView
import com.rainyseason.cj.common.view.settingHeaderView
import com.rainyseason.cj.common.view.settingSliderView
import com.rainyseason.cj.common.view.settingSwitchView
import com.rainyseason.cj.common.view.settingTitleSummaryView
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.stringValue
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.ticker.TickerWidgetFeature
import com.rainyseason.cj.ticker.TickerWidgetRenderer
import com.rainyseason.cj.ticker.view.CoinTickerPreviewViewModel_

class CoinTickerPreviewController(
    private val viewModel: CoinTickerPreviewViewModel,
    private val context: Context,
    private val numberFormater: NumberFormater,
    private val renderer: TickerWidgetRenderer
) : AsyncEpoxyController() {

    private var addSeparator = true

    companion object {
        const val COIN_SELECT_ID = "setting_coin_id"
        const val REFRESH_ID = "setting_refresh_id"
        const val THEME_ID = "setting_theme_id"
        const val FULL_SIZE_ID = "setting_full_size_id"
        const val STICKY_NOTIFICATION = "setting_sticky_notification"
    }

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

    override fun buildModels() {
        val state = withState(viewModel) { it }
        val buildRetryResult = buildRetry(state)
        if (buildRetryResult == BuildState.Stop) {
            return
        }
        buildDebugConfig(state)

        buildCoinId(state)
        buildTheme(state)
        buildStickyNotification(state)
        buildFullSize(state)
        buildRefreshInternal(state)
        buildCurrency(state)
        buildShowCurrencySymbol(state)
        buildLayout(state)

        buildAdvanceHeader(state)
        buildAdvanceSettings(state)
    }

    private fun buildDebugConfig(state: CoinTickerPreviewState) {
        if (!BuildConfig.DEBUG) {
            return
        }
        val debugConfig = DebugFlag.DEBUG_COIN_TICKER_CONFIG.stringValue ?: return
        when (debugConfig) {
            "change_interval" -> buildChangePercentGroup(state)
        }
    }

    private val circularProcess = CircularProgressDrawable(context).apply {
        setStyle(CircularProgressDrawable.LARGE)
        setColorSchemeColors(context.resolveColorAttr(R.attr.colorPrimary))
        start()
    }

    private fun buildCoinId(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val currentDisplayData = state.currentDisplayData
        val summary = if (config.backend.isExchange) {
            state.currentDisplayData?.symbol
        } else {
            state.currentDisplayData?.name
        }?.let {
            buildString {
                append("$it (${config.backend.displayName})")
                if (!config.network.isNullOrBlank()) {
                    append(" • ${config.network.uppercase()}")
                }
                if (!config.dex.isNullOrBlank()) {
                    append(" • ${config.dex.uppercase()}")
                }
            }
        }
        settingTitleSummaryView {
            id(COIN_SELECT_ID)
            title(R.string.coin_ticker_preview_setting_coin_id)
            if (summary.isNullOrBlank()) {
                summary(R.string.loading)
            } else {
                summary(summary)
            }
            if (config.backend.hasCoinUrl) {
                imagePrimary(currentDisplayData?.iconUrl ?: circularProcess)
                imageSecondary(config.backend.iconUrl)
            } else {
                imagePrimary(config.backend.iconUrl)
            }
            onClickListener { view ->
                view.findNavController()
                    .navigate(R.id.coin_select_screen)
            }
        }
    }

    private fun buildAdvanceSettings(state: CoinTickerPreviewState) {
        if (!state.showAdvanceSetting) {
            return
        }
        buildChangePercentGroup(state)
        buildPriceFormatGroup(state)
        buildStyleGroup(state)
        buildBehaviorGroup(state)
        buildContentGroup(state)
    }

    private fun buildStyleGroup(state: CoinTickerPreviewState) {
        settingHeaderView {
            id("setting_header_style")
            content(R.string.setting_style_title)
        }
        buildSizeAdjustment(state)
        buildBackgroundTransparency(state)
    }

    private fun buildContentGroup(state: CoinTickerPreviewState) {
        settingHeaderView {
            id("setting_header_content")
            content(R.string.setting_content_title)
        }
        buildReversePair(state)
        buildAmount(state)
        buildDisplaySymbol(state)
        buildDisplayName(state)
        buildReversePositiveColor(state)
    }

    private fun buildReversePositiveColor(state: CoinTickerPreviewState) {
        maybeBuildHorizontalSeparator(id = "reverse_positive_color_separator")
        settingSwitchView {
            id("reverse_positive_color")
            title(R.string.coin_ticker_preview_setting_reverse_positive_color)
            checked(state.config?.reversePositiveColor == true)
            onClickListener { _ ->
                viewModel.switchReversePositiveColor()
            }
        }
    }

    private fun buildDisplaySymbol(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        state.currentDisplayData
        maybeBuildHorizontalSeparator(id = "display_symbol_separator")
        val displaySymbol = renderer.getDisplaySymbol(config, state.currentDisplayData)
        settingTitleSummaryView {
            id("display_symbol_$displaySymbol")
            title(R.string.coin_ticker_preview_display_symbol)
            summary(displaySymbol)
            onClickListener { view ->
                val container = view.inflater
                    .inflate(R.layout.dialog_text_input, null, false)
                val editText = container.findViewById<EditText>(R.id.edit_text)
                editText.showSoftInputOnFocus = true
                if (state.currentDisplayData == null) {
                    editText.setText("")
                    editText.setSelection(0)
                } else {
                    editText.setText(displaySymbol)
                    editText.setSelection(displaySymbol.length)
                }
                editText.doOnPreDraw {
                    editText.requestFocus()
                    editText.showKeyboard()
                }
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_display_symbol)
                    .setView(container)
                    .setCancelButton()
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        viewModel.setDisplaySymbol(editText.text?.toString())
                    }
                    .setResetButton {
                        viewModel.setDisplaySymbol(null)
                    }
                    .show()
            }
        }
    }

    private fun buildDisplayName(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        state.currentDisplayData
        maybeBuildHorizontalSeparator(id = "display_name_separator")
        val displayName = renderer.getDisplayName(config, state.currentDisplayData)
        settingTitleSummaryView {
            id("display_name_$displayName")
            title(R.string.coin_ticker_preview_display_symbol)
            summary(displayName)
            onClickListener { view ->
                val container = view.inflater
                    .inflate(R.layout.dialog_text_input, null, false)
                val editText = container.findViewById<EditText>(R.id.edit_text)
                editText.showSoftInputOnFocus = true
                if (state.currentDisplayData == null) {
                    editText.setText("")
                    editText.setSelection(0)
                } else {
                    editText.setText(displayName)
                    editText.setSelection(displayName.length)
                }
                editText.doOnPreDraw {
                    editText.requestFocus()
                    editText.showKeyboard()
                }
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_display_name)
                    .setView(container)
                    .setCancelButton()
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        viewModel.setDisplayName(editText.text?.toString())
                    }
                    .setResetButton {
                        viewModel.setDisplayName(null)
                    }
                    .show()
            }
        }
    }

    private fun buildReversePair(state: CoinTickerPreviewState) {
        maybeBuildHorizontalSeparator(id = "reverse_pair_separator")
        settingSwitchView {
            id("reverse_pair")
            title(R.string.coin_ticker_preview_setting_reverse_pair)
            checked(state.config?.reversePair == true)
            onClickListener { _ ->
                viewModel.switchReversePair()
            }
        }
    }

    private fun buildChangePercentGroup(state: CoinTickerPreviewState) {
        settingHeaderView {
            id("setting_header_change_percent")
            content(R.string.setting_price_change_percent_title)
        }

        buildChangePercentInternal(state)
        buildPercentDecimal(state)
    }

    private fun buildBehaviorGroup(state: CoinTickerPreviewState) {
        settingHeaderView {
            id("setting_header_behavior")
            content(R.string.setting_behavior_title)
        }

        buildBatteryWarning(state)
        buildClickAction(state)
    }

    private fun buildPriceFormatGroup(state: CoinTickerPreviewState) {
        settingHeaderView {
            id("setting_header_price_format")
            content(R.string.setting_price_format_title)
        }
        buildAmountDecimal(state)
        buildHideDecimalOnLargePrice(state)
        buildRoundToMillion(state)
        buildShowThousandSeparator(state)
    }

    private fun buildAdvanceHeader(state: CoinTickerPreviewState) {
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

    private fun buildRetry(state: CoinTickerPreviewState): BuildState {
        val loadDataParams = state.loadDataParams ?: return BuildState.Stop
        val data = state.displayDataCache[loadDataParams]
        val error = (data as? Fail)?.error ?: return BuildState.Next
        retryView {
            id("retry")
            reason(error.getUserErrorMessage(context = context))
            buttonText(R.string.reload)
            onRetryClickListener { _ ->
                viewModel.reload()
            }
        }
        if (BuildConfig.DEBUG) {
            error.printStackTrace()
        }
        return BuildState.Stop
    }

    private fun buildHideDecimalOnLargePrice(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        maybeBuildHorizontalSeparator(id = "hide_decimal_separator")

        settingSwitchView {
            id("hide_decimal")
            title(R.string.coin_ticker_preview_setting_hide_decimal)
            checked(config.hideDecimalOnLargePrice)
            onClickListener { _ ->
                viewModel.switchHideDecimal()
            }
        }
    }

    private fun buildAmount(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val displayAmount = numberFormater.formatAmount(
            amount = config.amountOrDefault,
            currencyCode = config.currency,
            roundToMillion = false,
            numberOfDecimal = 5,
            hideOnLargeAmount = false,
            showCurrencySymbol = false,
            showThousandsSeparator = true
        )
        maybeBuildHorizontalSeparator(id = "separator_amount")
        settingTitleSummaryView {
            id("setting_amount_$displayAmount")
            title(R.string.coin_ticker_preview_amount)
            summary(displayAmount)
            onClickListener { view ->
                val container = view.inflater
                    .inflate(R.layout.dialog_number_input, null, false)
                val editText = container.findViewById<EditText>(R.id.edit_text)
                editText.showSoftInputOnFocus = true
                editText.setText(displayAmount)
                editText.setSelection(displayAmount.length)
                editText.doOnPreDraw {
                    editText.requestFocus()
                    editText.showKeyboard()
                }
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_amount)
                    .setView(container)
                    .setCancelButton()
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        dialog.dismiss()
                        viewModel.setAmount(editText.text?.toString())
                    }
                    .setResetButton {
                        viewModel.setAmount(null)
                    }
                    .show()
            }
        }
    }

    private fun buildBackgroundTransparency(state: CoinTickerPreviewState) {
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

    private fun buildSizeAdjustment(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        if (config.layout.isNano) {
            return
        }
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

    private fun buildBatteryWarning(state: CoinTickerPreviewState) {
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

    private fun buildStickyNotification(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        maybeBuildHorizontalSeparator(id = "sticky_noti_separator")
        settingSwitchView {
            id(TickerWidgetFeature.StickyNotification.viewId)
            title(R.string.coin_ticker_setting_noti_title)
            checked(config.showNotification)
            onClickListener { _ ->
                viewModel.toggleNotification()
            }
        }
    }

    private fun buildFullSize(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        maybeBuildHorizontalSeparator(id = "full_size_separator")
        settingSwitchView {
            id(TickerWidgetFeature.FullSize.viewId)
            title(R.string.widget_config_maintain_aspect_ratio)
            checked(config.maintainAspectRatio)
            onClickListener { _ ->
                viewModel.toggleFullSize()
            }
        }
    }

    private fun buildLayout(state: CoinTickerPreviewState) {
        val config = state.config ?: return

        maybeBuildHorizontalSeparator(id = "header_layout_separator")

        settingTitleSummaryView {
            id("setting_layout")
            title(R.string.coin_ticker_preview_setting_layout)
            summary(config.layout.titleRes)
            onClickListener { _ ->
                val currentLayout =
                    withState(viewModel) { it.config?.layout } ?: return@onClickListener
                val alternativeLayouts = currentLayout.alternativeLayouts()
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_setting_layout)
                    .setCancelButton()
                    .setSingleChoiceItems(
                        alternativeLayouts.map { context.getString(it.titleRes) }
                            .toTypedArray(),
                        alternativeLayouts.indexOfFirst { currentLayout == it },
                    ) { dialog, which ->
                        val select = alternativeLayouts[which]
                        viewModel.setLayout(select)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun buildShowCurrencySymbol(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        if (config.backend.isExchange) {
            return
        }
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

    private fun buildTheme(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val theme = config.theme
        maybeBuildHorizontalSeparator(id = "setting_theme_separator")
        settingTitleSummaryView {
            id(THEME_ID)
            title(R.string.coin_ticker_preview_setting_theme)
            summary(context.getString(theme.titleRes))
            onClickListener { _ ->
                val currentConfig = withState(viewModel) { it.config } ?: return@onClickListener
                val themeToSummaryString = Theme.VALUES_COMPAT
                    .associate { it to context.getString(it.titleRes) }
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
            id(REFRESH_ID)
            title(R.string.coin_ticker_preview_refresh_interval)
            summary(RefreshIntervals.createString(context, refreshInterval, refreshInternalUnit))
            onClickListener { _ ->
                val currentConfig = withState(viewModel) { it.config } ?: return@onClickListener
                val optionsString = RefreshIntervals.VALUES.map {
                    RefreshIntervals.createString(context, it.first, it.second)
                }
                val currentRefreshInterval = currentConfig.refreshInterval
                val currentRefreshInternalUnit = currentConfig.refreshIntervalUnit

                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_refresh_interval)
                    .setCancelButton()
                    .setSingleChoiceItems(
                        optionsString.toTypedArray(),
                        RefreshIntervals.VALUES.indexOfFirst {
                            it.first == currentRefreshInterval &&
                                it.second == currentRefreshInternalUnit
                        }
                    ) { dialog, which ->
                        val select = RefreshIntervals.VALUES[which]
                        viewModel.setRefreshInternal(select.first, select.second)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun buildCurrency(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        maybeBuildHorizontalSeparator(id = "setting_currency_separator")
        settingTitleSummaryView {
            id("setting_currency")
            title(R.string.coin_ticker_preview_setting_header_currency)
            summary(getNonNullCurrencyInfo(config.currency).displayName())
            onClickListener { _ ->
                val currentState = withState(viewModel) { it }
                val currentConfig = currentState.config ?: return@onClickListener
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_setting_header_currency)
                    .setCancelButton()
                    .setSingleChoiceItems(
                        currentConfig.backend.supportedCurrency
                            .map { it.displayName() }
                            .toTypedArray(),
                        currentConfig.backend.supportedCurrency
                            .indexOfFirst { it.code == currentConfig.currency }
                    ) { dialog, which ->
                        viewModel.setCurrency(currentConfig.backend.supportedCurrency[which].code)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun buildClickAction(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val optionsToString = listOf(
            CoinTickerConfig.ClickAction.OPEN_COIN_DETAIL
                to R.string.coin_ticker_preview_setting_header_click_action_open_coin_detail,
            CoinTickerConfig.ClickAction.REFRESH
                to R.string.coin_ticker_preview_setting_header_click_action_refresh,
            CoinTickerConfig.ClickAction.SETTING
                to R.string.coin_ticker_preview_setting_header_click_action_setting,
        ).map { it.first to context.getString(it.second) }
            .let {
                if (!config.backend.isDefault) {
                    it.filter { p -> p.first != CoinTickerConfig.ClickAction.OPEN_COIN_DETAIL }
                } else {
                    it
                }
            }
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

    private fun buildChangePercentInternal(state: CoinTickerPreviewState) {
        val config = state.config ?: return

        maybeBuildHorizontalSeparator(id = "bottom_change_interval_separator")

        settingTitleSummaryView {
            id("bottom_change_interval")
            title(R.string.coin_ticker_preview_setting_bottom_change_percent_internal_header)
            summary(config.changeInterval.titleRes)
            onClickListener { _ ->
                val currentState = withState(viewModel) { it }
                val currentConfig = currentState.config!!
                val currentInterval = currentConfig.changeInterval
                val currentBackend = currentConfig.backend
                AlertDialog.Builder(context)
                    .setTitle(
                        R.string.coin_ticker_preview_setting_bottom_change_percent_internal_header
                    )
                    .setCancelButton()
                    .setSingleChoiceItems(
                        currentBackend.supportedTimeRange.map {
                            context.getString(it.titleRes)
                        }.toTypedArray(),
                        currentBackend.supportedTimeRange.indexOfFirst { it == currentInterval }
                    ) { dialog, which ->
                        val selectedInterval = currentBackend.supportedTimeRange.toList()
                            .getOrNull(which) ?: TimeInterval.I_24H
                        viewModel.setPriceChangeInterval(selectedInterval)
                        dialog.dismiss()
                    }
                    .show()
            }
        }
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
        val percentDecimal = config.numberOfChangePercentDecimal ?: 1
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

    private fun buildAmountDecimal(state: CoinTickerPreviewState) {
        val config = state.config ?: return
        val priceDecimal = config.numberOfAmountDecimal ?: 2

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

    override fun isStickyHeader(position: Int): Boolean {
        return adapter.getModelAtPosition(position) is CoinTickerPreviewViewModel_
    }
}
