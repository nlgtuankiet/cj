package com.rainyseason.cj.widget.watch

import android.os.Parcelable
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.WatchListRepository
import com.rainyseason.cj.common.getWidgetId
import com.rainyseason.cj.data.UserSettingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

data class WatchPreviewState(
    val savedDisplayData: Async<WatchDisplayData> = Uninitialized,
    val savedConfig: Async<WatchConfig> = Uninitialized,
    val watchlist: Async<List<String>> = Uninitialized,
) : MavericksState {
    val config: WatchConfig?
        get() = savedConfig.invoke()
}

@Parcelize
data class WatchPreviewArgs(
    val widgetId: Int,
) : Parcelable

class WatchPreviewViewModel @AssistedInject constructor(
    @Assisted val initState: WatchPreviewState,
    @Assisted val args: WatchPreviewArgs,
    private val watchWidgetRepository: WatchWidgetRepository,
    private val watchListRepository: WatchListRepository,
    private val userSettingRepository: UserSettingRepository,
) : MavericksViewModel<WatchPreviewState>(initState) {
    private var saved = false

    init {
        loadDisplayData()
        loadWatchList()

        onEach { state ->
            maybeSaveDisplayData(state)
        }

        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            saveInitialConfig()
        }
    }

    private var loadEachEntryDataJob: Job? = null
    private fun loadEachEntryData() {
        loadEachEntryDataJob?.cancel()
        loadEachEntryDataJob = onAsync(WatchPreviewState::watchlist) { watchlist ->
            // TODO load data for each entry
        }
    }

    private fun maybeSaveDisplayData(state: WatchPreviewState) {
    }

    private fun loadWatchList() {
        watchListRepository.getWatchList()
            .execute { copy(watchlist = it) }
    }

    private suspend fun saveInitialConfig() {
        val userSetting = userSettingRepository.getUserSetting()
        val config = WatchConfig(
            // widgetId = widgetId,
            // coinId = args.coinId,
            // layout = args.layout,
            // numberOfAmountDecimal = userSetting.amountDecimals,
            // numberOfChangePercentDecimal = userSetting.numberOfChangePercentDecimal,
            // refreshInterval = userSetting.refreshInterval,
            // refreshIntervalUnit = userSetting.refreshIntervalUnit,
            // showThousandsSeparator = userSetting.showThousandsSeparator,
            // showCurrencySymbol = userSetting.showCurrencySymbol,
            // roundToMillion = userSetting.roundToMillion,
            // currency = userSetting.currencyCode,
            // sizeAdjustment = userSetting.sizeAdjustment,
        )
        watchWidgetRepository.setConfig(args.widgetId, config)
        loadConfig()
    }

    private fun loadDisplayData() {
        watchWidgetRepository.getDisplayDataStream(args.widgetId)
            .execute { copy(savedDisplayData = it) }
    }

    private var loadConfigJob: Job? = null
    private fun loadConfig() {
        loadConfigJob?.cancel()
        loadConfigJob = watchWidgetRepository.getConfigStream(args.widgetId)
            .execute { copy(savedConfig = it) }
    }

    override fun onCleared() {
        if (!saved) {
            viewModelScope.launch(NonCancellable) {
                watchWidgetRepository.clearDisplayData(args.widgetId)
            }
        }
        super.onCleared()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            initState: WatchPreviewState,
            args: WatchPreviewArgs,
        ): WatchPreviewViewModel
    }

    companion object : MavericksViewModelFactory<WatchPreviewViewModel, WatchPreviewState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: WatchPreviewState
        ): WatchPreviewViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext)
                .fragment<WatchPreviewFragment>()
            val factory = fragment.viewModelFactory
            val widgetId = fragment.requireActivity().intent.extras?.getWidgetId()
                ?: throw IllegalArgumentException("missing widget id")
            return factory.create(state, WatchPreviewArgs(widgetId))
        }
    }
}