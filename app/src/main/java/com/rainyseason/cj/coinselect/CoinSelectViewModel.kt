package com.rainyseason.cj.coinselect

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.fragment
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.update
import com.rainyseason.cj.data.CoinHistoryEntry
import com.rainyseason.cj.data.CoinHistoryRepository
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
import java.util.Collections
import java.util.UUID

data class CoinSelectState(
    val history: Async<List<CoinHistoryEntry>> = Uninitialized,
    val keyword: String = "",
    val backend: Backend,
    val backendProductMap: Map<Backend, Async<List<BackendProduct>>> = emptyMap(),
    val currentPage: Int = 0,
) : MavericksState

class CoinSelectViewModel @AssistedInject constructor(
    @Assisted private val initState: CoinSelectState,
    private val coinHistoryRepository: CoinHistoryRepository,
    private val getBackendProducts: GetBackendProducts,
) : MavericksViewModel<CoinSelectState>(initState) {

    val id = UUID.randomUUID().toString()
    private val keywordDebound = MutableStateFlow("")
    private val loadBackendJobs: MutableMap<Backend, Job> = Collections
        .synchronizedMap(mutableMapOf())
    private var historyJob: Job? = null
    private var listenBackendChangeJob: Job? = null

    init {
        reload()
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            keywordDebound.drop(1)
                .debounce(300)
                .distinctUntilChanged()
                .collect { setState { copy(keyword = it) } }
        }

        onEach(
            CoinSelectState::keyword,
            CoinSelectState::backend,
        ) { _, _ ->
            setState { copy(currentPage = 0) }
        }
    }

    fun displayNextPage() {
        setState { copy(currentPage = currentPage + 1) }
    }

    private fun listenBackendChange() {
        listenBackendChangeJob?.cancel()
        setState { copy(backendProductMap = emptyMap()) }
        listenBackendChangeJob = onEach(CoinSelectState::backend) { backend ->
            loadBackendProducts(backend)
        }
    }

    private fun loadBackendProducts(backend: Backend) {
        withState { state ->
            val oldDataAsync = state.backendProductMap[backend]
            if (oldDataAsync != null && oldDataAsync is Success) {
                return@withState
            }
            loadBackendJobs[backend]?.cancel()
            loadBackendJobs[backend] = getBackendProducts.invoke(backend).execute {
                copy(
                    backendProductMap = backendProductMap.update {
                        put(backend, it)
                    }
                )
            }
        }
    }

    fun back() {
        setState {
            copy(
                backend = Backend.CoinGecko,
                keyword = "",
            )
        }
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

    fun reload() {
        listenBackendChange()

        historyJob?.cancel()
        historyJob = coinHistoryRepository.getHistory()
            .execute {
                copy(history = it)
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

    @AssistedFactory
    interface Factory {
        fun create(initState: CoinSelectState): CoinSelectViewModel
    }
}
