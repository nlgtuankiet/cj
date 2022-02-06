package com.rainyseason.cj.coinselect

import android.content.Context
import android.view.View
import androidx.core.text.buildSpannedString
import androidx.navigation.findNavController
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.withState
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.R
import com.rainyseason.cj.coinselect.view.entryView
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.DEFAULT_PER_PAGE
import com.rainyseason.cj.common.TraceManager
import com.rainyseason.cj.common.dismissKeyboard
import com.rainyseason.cj.common.forEachPaged
import com.rainyseason.cj.common.getUserErrorMessage
import com.rainyseason.cj.common.loadingView
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.view.emptyView
import com.rainyseason.cj.common.view.retryView
import com.rainyseason.cj.common.view.settingHeaderView
import com.rainyseason.cj.data.CoinHistoryEntry
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logKeyParamsEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.channels.Channel
import timber.log.Timber
import kotlin.system.measureTimeMillis

class CoinSelectController @AssistedInject constructor(
    @Assisted private val viewModel: CoinSelectViewModel,
    private val context: Context,
    private val tracker: Tracker,
    private val traceManager: TraceManager,
) : AsyncEpoxyController() {

    val requestSearchBoxFocus = Channel<Unit>(Channel.UNLIMITED)

    @AssistedFactory
    interface Factory {
        fun create(
            viewModel: CoinSelectViewModel,
        ): CoinSelectController
    }

    override fun buildModels() {
        if (BuildConfig.DEBUG) {
            val buildModelTime = measureTimeMillis {
                buildModelsInternal()
            }
            Timber.d("build model take ${buildModelTime}ms")
        } else {
            buildModelsInternal()
        }
    }

    private fun buildModelsInternal() {
        val state = withState(viewModel) { it }
        emptyView {
            id("holder")
        }
        listOf(
            ::buildHistory,
            ::buildExchanges,
            ::buildOtherSources,
            ::buildBackendEntries,
        ).forEach { builder ->
            val buildResult = builder.invoke(state)
            if (buildResult == BuildState.Stop) {
                return
            }
            if (buildResult == BuildState.StopWithLoading) {
                loadingView {
                    id("loading")
                }
            }
        }
    }

    private fun buildBackendEntries(state: CoinSelectState): BuildState {
        val backend = state.backend
        val data: Async<List<BackendProduct>> = if (state.backend.canSearchProduct) {
            state.backendSearchProduct
        } else {
            state.backendProductMap[backend]
        } ?: return BuildState.Next

        if (data is Fail) {
            retryView {
                id("retry")
                reason(data.error.getUserErrorMessage(context = context))
                buttonText(R.string.reload)
                onRetryClickListener { _ ->
                    viewModel.reload()
                }
            }
            if (BuildConfig.DEBUG) {
                data.error.printStackTrace()
            }
            return BuildState.Stop
        }

        if (data is Loading) {
            return BuildState.StopWithLoading
        }

        val list: List<BackendProduct> = data.invoke() ?: return BuildState.Next
        val keyword = state.keyword
        val filteredList = if (backend.canSearchProduct) {
            list
        } else {
            list.filter {
                it.symbol.contains(keyword, true) ||
                    it.displayName.contains(keyword, true)
            }
        }

        settingHeaderView {
            id("header_backend")
            content(backend.displayName)
        }

        filteredList.forEachPaged(
            page = state.currentPage,
            perPage = DEFAULT_PER_PAGE,
            collector = this,
            showMore = {
                viewModel.displayNextPage()
            }
        ) { entry ->
            entryView {
                id("backend_product_${entry.uniqueId}")
                title(entry.displayName)
                subTitle(getSubtitle(backend, entry.symbol, entry.network, entry.dex))
                iconUrl(entry.iconUrl)
                onClickListener { view ->
                    viewModel.addToHistory(
                        CoinHistoryEntry(
                            id = entry.id,
                            symbol = entry.symbol,
                            name = entry.displayName,
                            iconUrl = entry.iconUrl,
                            backend = backend,
                            network = entry.network,
                            dex = entry.dex
                        )
                    )
                    moveToResult(view, entry.id, entry.backend, entry.network, entry.dex)
                }
            }
        }

        return BuildState.Next
    }

    private fun getSubtitle(
        backend: Backend,
        symbol: String,
        network: String?,
        dex: String?
    ): CharSequence {
        if (backend.isExchange) {
            return ""
        }
        return buildSpannedString {
            append(symbol.uppercase())
            if (!network.isNullOrBlank()) {
                append(" • ${network.uppercase()}")
            }
            if (!dex.isNullOrBlank()) {
                append(" • ${dex.uppercase()}")
            }
        }
    }

    private fun buildExchanges(state: CoinSelectState): BuildState {
        if (!state.backend.isDefault) {
            return BuildState.Next
        }
        val keyword = state.keyword

        val exchanges = Backend.values().filter { it.isExchange }
            .filter { it.displayName.contains(keyword, true) }

        if (exchanges.isEmpty()) {
            return BuildState.Next
        }

        settingHeaderView {
            id("header_exchanges")
            content(R.string.coin_select_exchange_header)
        }

        exchanges.forEach { backend ->
            entryView {
                id("backend_${backend.id}")
                title(backend.displayName)
                subTitle("")
                iconUrl(backend.iconUrl)
                onClickListener { _ ->
                    viewModel.setBackend(backend)
                    requestSearchBoxFocus.trySend(Unit)
                    viewModel.submitNewKeyword("")
                }
            }
        }

        return BuildState.Next
    }

    private fun buildOtherSources(state: CoinSelectState): BuildState {
        if (!state.backend.isDefault) {
            return BuildState.Next
        }
        val keyword = state.keyword

        val exchanges = Backend.values().filter { !it.isExchange && !it.isDefault }
            .filter { it.displayName.contains(keyword, true) }

        if (exchanges.isEmpty()) {
            return BuildState.Next
        }

        settingHeaderView {
            id("header_other_sources")
            content(R.string.coin_select_other_source_header)
        }

        exchanges.forEach { backend ->
            entryView {
                id("backend_${backend.id}")
                title(backend.displayName)
                subTitle("")
                iconUrl(backend.iconUrl)
                onClickListener { _ ->
                    viewModel.setBackend(backend)
                    requestSearchBoxFocus.trySend(Unit)
                    viewModel.submitNewKeyword("")
                }
            }
        }

        return BuildState.Next
    }

    private fun buildHistory(state: CoinSelectState): BuildState {
        if (!state.backend.isDefault) {
            return BuildState.Next
        }
        val historyEntries = state.history.invoke().orEmpty()
        if (historyEntries.isEmpty()) {
            return BuildState.Next
        }

        if (state.keyword.isNotEmpty()) {
            return BuildState.Next
        }

        settingHeaderView {
            id("header_history")
            content(R.string.coin_history_header)
        }

        historyEntries.forEach { entry: CoinHistoryEntry ->
            entryView {
                id("history_${entry.uniqueId}")
                title(entry.name)
                if (entry.backend.isExchange) {
                    subTitle(entry.backend.displayName)
                } else {
                    subTitle(getSubtitle(entry.backend, entry.symbol, entry.network, entry.dex))
                }
                iconUrl(entry.iconUrl)
                onClearClickListener { _ ->
                    viewModel.removeHistory(id = entry.id)
                }
                onClickListener { view ->
                    viewModel.addToHistory(entry)
                    moveToResult(
                        view,
                        coinId = entry.id,
                        backend = entry.backend,
                        network = entry.network,
                        dex = entry.dex,
                    )
                }
            }
        }

        return BuildState.Next
    }

    private fun moveToResult(
        view: View,
        coinId: String,
        backend: Backend = Backend.CoinGecko,
        network: String?,
        dex: String?,
    ) {
        view.dismissKeyboard()
        val controller = view.findNavController()
        tracker.logKeyParamsEvent(
            "coin_select",
            mapOf(
                "coin_id" to coinId,
                "backend_id" to backend.id,
                "network" to network,
                "dex" to dex,
            )
        )
        controller.previousBackStackEntry?.savedStateHandle
            ?.set("result", CoinSelectResult(coinId, backend, network, dex))
        controller.popBackStack()
    }
}
