package com.rainyseason.cj.setting

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.setCancelButton
import com.rainyseason.cj.common.view.settingTitleSummaryView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class SettingController @AssistedInject constructor(
    @Assisted private val viewModel: SettingViewModel,
    @Assisted private val context: Context,

    ) : AsyncEpoxyController() {

    @AssistedFactory
    interface Factory {
        fun create(viewModel: SettingViewModel, context: Context): SettingController
    }

    override fun buildModels() {
        val state = withState(viewModel) { it }

        val userSetting = state.userSetting.invoke() ?: return
        val currencyCode = userSetting.currencyCode

        settingTitleSummaryView {
            id("setting_currency")
            title(R.string.coin_ticker_preview_setting_header_currency)
            summary(SUPPORTED_CURRENCY[currencyCode]!!.name)
            onClickListener { _ ->
                val currentState = withState(viewModel) { it }
                val currentOption = currentState.userSetting.invoke()?.currencyCode
                    ?: return@onClickListener
                val options = SUPPORTED_CURRENCY.values.toList()
                AlertDialog.Builder(context)
                    .setTitle(R.string.coin_ticker_preview_setting_header_currency)
                    .setCancelButton()
                    .setSingleChoiceItems(
                        options.map { it.name }.toTypedArray(),
                        options.indexOfFirst { it.code == currentOption }
                    ) { dialog, which ->
                        val selectedCurrency = options[which].code
                        viewModel.updateSetting { copy(currencyCode = selectedCurrency) }
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }
}