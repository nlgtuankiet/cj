package com.rainyseason.cj.watch

import android.content.Context
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.WatchListRepository
import com.rainyseason.cj.common.fragment
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.model.Watchlist
import com.rainyseason.cj.common.model.WatchlistCollection
import com.rainyseason.cj.common.realtimeFlowOf
import com.rainyseason.cj.common.update
import com.rainyseason.cj.common.usecase.GetWatchDisplayEntry
import com.rainyseason.cj.data.UserSetting
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.widget.watch.WatchDisplayEntryContent
import com.rainyseason.cj.widget.watch.WatchDisplayEntryLoadParam
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Collections

private typealias State = WatchListState

data class WatchListState(
    val page: Int = 0,
    val userSetting: Async<UserSetting> = Uninitialized,
    val selectedWatchlistId: String = Watchlist.DEFAULT_ID,
    val watchListCollection: Async<WatchlistCollection> = Uninitialized,
    val watchDisplayData: Map<WatchDisplayEntryLoadParam, Async<WatchDisplayEntryContent>> =
        emptyMap(),
    val isInEditMode: Boolean = false,
) : MavericksState {
    val currentWatchlist = watchListCollection.invoke()
        ?.list?.firstOrNull { it.id == selectedWatchlistId }
}

@OptIn(FlowPreview::class)
class WatchListViewModel @AssistedInject constructor(
    @Assisted state: WatchListState,
    @Assisted args: WatchlistArgs,
    private val userSettingRepository: UserSettingRepository,
    private val watchListRepository: WatchListRepository,
    private val context: Context,
    private val getWatchDisplayEntry: GetWatchDisplayEntry,
) : MavericksViewModel<WatchListState>(state) {
    private val loadWatchEntryDataJob: MutableMap<WatchDisplayEntryLoadParam, Job> =
        Collections.synchronizedMap(mutableMapOf())

    private var userSettingJob: Job? = null
    private var watchListJob: Job? = null
    private var loadWatchListEntriesJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        loadWatchEntryDataJob.values.forEach { it.cancel() }
        loadWatchEntryDataJob.clear()

        setState { copy(watchDisplayData = emptyMap()) }

        userSettingJob?.cancel()
        userSettingJob = userSettingRepository.getUserSettingFlow()
            .execute {
                copy(userSetting = it)
            }

        watchListJob?.cancel()
        watchListJob = watchListRepository.getWatchlistCollectionFlow()
            .execute { copy(watchListCollection = it) }

        loadWatchListEntriesJob?.cancel()
        loadWatchListEntriesJob = onEach(
            State::userSetting,
            State::watchListCollection,
            State::selectedWatchlistId,
        ) { userSetting, watchListCollection, selectedWatchlistId ->
            val setting = userSetting.invoke() ?: return@onEach
            val collection = watchListCollection.invoke() ?: return@onEach
            val watchlist = collection.list.firstOrNull { it.id == selectedWatchlistId }
                ?: return@onEach
            watchlist.coins.forEach { coin ->
                val param = WatchDisplayEntryLoadParam(
                    coin = coin,
                    currency = setting.currencyCode,
                    changeInterval = TimeInterval.I_24H,
                )
                maybeLoadWatchEntry(param)
            }
        }
    }

    private fun maybeLoadWatchEntry(param: WatchDisplayEntryLoadParam) {
        withState { state ->
            val currentAsync = state.watchDisplayData[param]
            if (currentAsync == null || currentAsync is Fail) {
                Timber.d("load $param")
                loadWatchEntryDataJob[param]?.cancel()
                loadWatchEntryDataJob[param] = context.realtimeFlowOf {
                    getWatchDisplayEntry.invoke(param)
                }.execute {
                    copy(watchDisplayData = watchDisplayData.update { put(param, it) })
                }
            }
        }
    }

    fun onRemoveClick(coin: Coin, watchlistId: String) {
        viewModelScope.launch {
            watchListRepository.remove(coin, watchlistId)
        }
    }

    fun addOrRemove(coin: Coin) {
        withState { state ->
            viewModelScope.launch {
                watchListRepository.addOrRemove(coin, state.selectedWatchlistId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onClear")
    }

    fun exitEditMode() {
        setState { copy(isInEditMode = false) }
    }

    fun switchEditMode() {
        setState { copy(isInEditMode = !isInEditMode) }
    }

    fun drag(from: Coin, to: Coin) {
        viewModelScope.launch {
            watchListRepository.drag(from, to)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(state: WatchListState, args: WatchlistArgs,): WatchListViewModel
    }

    companion object : MavericksViewModelFactory<WatchListViewModel, WatchListState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: WatchListState,
        ): WatchListViewModel {
            val fragment = viewModelContext.fragment<WatchListFragment>()
            return fragment.viewModelFatory.create(state, fragment.args)
        }
    }
}
