package com.rainyseason.cj.coinselect

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.fragment
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.update
import com.rainyseason.cj.data.CoinHistoryEntry
import com.rainyseason.cj.data.CoinHistoryRepository
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.CoinListEntry
import com.rainyseason.cj.data.coingecko.MarketsResponseEntry
import com.rainyseason.cj.data.coingecko.getCoinListFlow
import com.rainyseason.cj.data.coingecko.getCoinMarketsFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.UUID

data class CoinSelectState(
    val markets: Async<List<MarketsResponseEntry>> = Uninitialized,
    val list: Async<List<CoinListEntry>> = Uninitialized,
    val history: Async<List<CoinHistoryEntry>> = Uninitialized,
    val keyword: String = "",
    val backend: Backend,
    val backendProductMap: Map<Backend, Async<List<BackendProduct>>> = emptyMap(),
) : MavericksState

class CoinSelectViewModel @AssistedInject constructor(
    @Assisted private val initState: CoinSelectState,
    private val userSettingRepository: UserSettingRepository,
    private val coinGeckoService: CoinGeckoService,
    private val coinHistoryRepository: CoinHistoryRepository,
    private val getBackendProducts: GetBackendProducts,
) : MavericksViewModel<CoinSelectState>(initState) {

    val id = UUID.randomUUID().toString()

    @AssistedFactory
    interface Factory {
        fun create(initState: CoinSelectState): CoinSelectViewModel
    }

    private val keywordDebound = MutableStateFlow("")

    init {
        reload()
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            keywordDebound.drop(1)
                .debounce(300)
                .distinctUntilChanged()
                .collect { setState { copy(keyword = it) } }
        }
    }

    private var listenBackendChangeJob: Job? = null
    private fun listenBackendChange() {
        listenBackendChangeJob?.cancel()
        setState { copy(backendProductMap = emptyMap()) }
        listenBackendChangeJob = onEach(CoinSelectState::backend) { backend ->
            loadBackendProducts(backend)
        }
    }

    private fun loadBackendProducts(backend: Backend) {
        if (backend.isDefault) {
            return
        }
        withState { state ->
            val oldDataAsync = state.backendProductMap[backend]
            if (oldDataAsync != null && !oldDataAsync.shouldLoad) {
                return@withState
            }
            suspend {
                getBackendProducts.invoke(backend)
            }.execute {
                copy(
                    backendProductMap = backendProductMap.update {
                        put(backend, it)
                    }
                )
            }
        }
    }

    fun back() {
        setState { copy(backend = Backend.CoinGecko) }
    }

    fun submitNewKeyword(newKeyword: String) {
        keywordDebound.value = newKeyword.trim()
    }

    fun setBackend(backend: Backend) {
        setState { copy(backend = backend) }
    }

    fun addToHistory(entry: CoinHistoryEntry) {
        viewModelScope.launch {
            coinHistoryRepository.add(entry)
        }
    }

    fun removeHistory(id: String) {
        viewModelScope.launch {
            coinHistoryRepository.remove(id)
        }
    }

    private var listJob: Job? = null
    private var marketJob: Job? = null
    private var historyJob: Job? = null

    fun reload() {
        listenBackendChange()

        historyJob?.cancel()
        historyJob = coinHistoryRepository.getHistory()
            .execute {
                copy(history = it)
            }

        listJob?.cancel()
        listJob = coinGeckoService.getCoinListFlow().execute { copy(list = it) }

        marketJob?.cancel()
        marketJob = viewModelScope.launch {
            val setting = userSettingRepository.getUserSetting()
            coinGeckoService.getCoinMarketsFlow(vsCurrency = setting.currencyCode, perPage = 1000)
                .execute { copy(markets = it) }
        }
    }

    companion object : MavericksViewModelFactory<CoinSelectViewModel, CoinSelectState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: CoinSelectState,
        ): CoinSelectViewModel {
            val fragment = viewModelContext.fragment<CoinSelectFragment>()
            return fragment.viewModelFactory.create(state)
        }

        override fun initialState(viewModelContext: ViewModelContext): CoinSelectState {
            val fragment = viewModelContext.fragment<CoinSelectFragment>()
            return CoinSelectState(
                backend = Backend.from(fragment.arguments?.getString("backend_id"))
            )
        }
    }
}
