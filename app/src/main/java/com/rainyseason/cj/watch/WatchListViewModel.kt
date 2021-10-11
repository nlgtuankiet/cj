package com.rainyseason.cj.watch

import android.content.Context
import android.content.Intent
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.firebase.ui.auth.AuthUI
import com.rainyseason.cj.R
import com.rainyseason.cj.common.WatchListRepository
import com.rainyseason.cj.common.update
import com.rainyseason.cj.data.UserSetting
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinDetailResponse
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.CoinListEntry
import com.rainyseason.cj.data.coingecko.MarketChartResponse
import com.rainyseason.cj.data.coingecko.MarketsResponseEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.Collections

private typealias State = WatchListState

data class WatchListState(
    val page: Int = 0,
    val userSetting: Async<UserSetting> = Uninitialized,
    val watchList: Async<List<String>> = Uninitialized,
    val watchEntryDetail: Map<String, Async<CoinDetailResponse>> = emptyMap(),
    val watchEntryMarket: Map<String, Async<MarketChartResponse>> = emptyMap(),
    val coinList: Async<List<CoinListEntry>> = Uninitialized,
    val coinMarket: Async<List<MarketsResponseEntry>> = Uninitialized,
    val keyword: String = "",
    val addTasks: Map<String, Async<*>> = emptyMap(),
) : MavericksState

@OptIn(FlowPreview::class)
class WatchListViewModel @AssistedInject constructor(
    @Assisted state: WatchListState,
    private val coinGeckoService: CoinGeckoService,
    private val userSettingRepository: UserSettingRepository,
    private val watchListRepository: WatchListRepository,
    private val context: Context,
) : MavericksViewModel<WatchListState>(state) {

    init {
        reload()
    }

    private var coinListJob: Job? = null
    private var userSettingJob: Job? = null
    private var watchListJob: Job? = null
    private var loadWatchListEntriesJob: Job? = null
    private var marketJob: Job? = null

    private fun reload() {
        coinListJob?.cancel()
        coinListJob = suspend {
            coinGeckoService.getCoinList()
        }.execute {
            copy(coinList = it)
        }

        marketJob?.cancel()
        marketJob = suspend {
            val currencyCode = userSettingRepository.getUserSetting().currencyCode
            coinGeckoService.getCoinMarkets(
                vsCurrency = currencyCode,
                perPage = 250,
                page = 1,
                sparkline = false
            )
        }.execute {
            copy(coinMarket = it)
        }

        userSettingJob?.cancel()
        userSettingJob = userSettingRepository.getUserSettingFlow()
            .execute {
                copy(userSetting = it)
            }

        watchListJob?.cancel()
        watchListJob = watchListRepository.getWatchList()
            .execute {
                copy(watchList = it)
            }

        loadWatchListEntriesJob?.cancel()
        loadWatchListEntriesJob = onEach(
            State::userSetting,
            State::watchList,
        ) { userSetting, watchList ->
            if (userSetting is Success && watchList is Success) {
                val currency = userSetting.invoke().currencyCode
                withState { state ->
                    watchList.invoke().forEach { coinId ->
                        val detailAsync = state.watchEntryDetail[coinId]
                        val marketAsync = state.watchEntryMarket[coinId]
                        val needLoad = listOf(detailAsync, marketAsync)
                            .any { it == null || it is Fail }
                        if (needLoad) {
                            loadWatchEntry(coinId, currency)
                        }
                    }
                }
            }
        }
    }

    private val wachEntryDetailJob: MutableMap<String, Job> =
        Collections.synchronizedMap(mutableMapOf())
    private val wachEntryMarketlJob: MutableMap<String, Job> =
        Collections.synchronizedMap(mutableMapOf())

    private fun loadWatchEntry(id: String, currencyCode: String) {
        wachEntryDetailJob.remove(id)?.cancel()
        wachEntryDetailJob[id] = suspend {
            coinGeckoService.getCoinDetail(id)
        }.execute {
            copy(
                watchEntryDetail = watchEntryDetail.update { put(id, it) }
            )
        }

        wachEntryMarketlJob.remove(id)?.cancel()
        wachEntryMarketlJob[id] = suspend {
            coinGeckoService.getMarketChart(id, currencyCode, 1)
        }.execute {
            copy(
                watchEntryMarket = watchEntryMarket.update { put(id, it) }
            )
        }
    }

    suspend fun onAuth(action: suspend () -> Unit) {
        // val currentUser = FirebaseAuth.getInstance().currentUser
        // if (currentUser != null) {
        //     action.invoke()
        //     return
        // }
        //
        val providers = listOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.AnonymousBuilder().build(),
        )
        // val silentSignInResult = runCatching {
        //     AuthUI.getInstance().silentSignIn(context, providers).await()
        // }
        // Timber.d("silentSignInResult: $silentSignInResult")
        //
        // if (silentSignInResult.isSuccess) {
        //     action()
        //     return
        // }
        // val signInAnonResult = runCatching {
        //     FirebaseAuth.getInstance().signInAnonymously().await()
        // }
        //
        // Timber.d("signInAnonResult: $signInAnonResult")
        // if (signInAnonResult.isSuccess) {
        //     action()
        //     return
        // }

        val signInIntent = AuthUI.getInstance().createSignInIntentBuilder()
            .setIsSmartLockEnabled(false)
            .setLogo(R.drawable.sign_in_logo_small)
            .setTheme(R.style.Theme_CryptoJet)
            .enableAnonymousUsersAutoUpgrade()
            .setAvailableProviders(providers)
            .build()
        signInIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(signInIntent)
    }

    fun onRemoveClick(id: String) {

        viewModelScope.launch {
            onAuth {
                watchListRepository.remove(id)
            }

        }
    }

    fun onAddClick(id: String) {
        viewModelScope.launch {
            onAuth {
                withState { state ->
                    if (state.addTasks[id] is Loading) {
                        return@withState
                    }

                    val watchList = state.watchList.invoke() ?: return@withState
                    suspend {
                        if (watchList.contains(id)) {
                            watchListRepository.remove(id)
                        } else {
                            watchListRepository.add(id)
                        }
                    }.execute { copy(addTasks = addTasks.update { put(id, it) }) }
                }
            }
        }
    }

    private val keywordDeboucer = MutableStateFlow("")

    init {
        viewModelScope.launch {
            keywordDeboucer.debounce(300)
                .collect { setState { copy(keyword = it) } }
        }
    }

    fun onKeywordChange(value: String) {
        keywordDeboucer.value = value.trim()
    }

    @AssistedFactory
    interface Factory {
        fun create(state: WatchListState): WatchListViewModel
    }

    companion object : MavericksViewModelFactory<WatchListViewModel, WatchListState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: WatchListState,
        ): WatchListViewModel {
            return (viewModelContext as FragmentViewModelContext).fragment<WatchListFragment>()
                .viewModelFatory.create(state)
        }
    }
}
