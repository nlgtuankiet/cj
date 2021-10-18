package com.rainyseason.cj.common

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.rainyseason.cj.data.CommonRepository
import com.rainyseason.cj.widget.watch.WatchWidget4x2Provider
import com.rainyseason.cj.widget.watch.WatchWidget4x4Provider
import com.rainyseason.cj.widget.watch.WatchWidgetHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchListRepository @Inject constructor(
    private val commonRepository: CommonRepository,
    private val appWidgetManager: AppWidgetManager,
    private val context: Context,
    private val watchWidgetHandler: WatchWidgetHandler,
) : LifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val stateFlow: MutableStateFlow<List<String>> by lazy {
        scope.launch {
            commonRepository.watchListIdsFlow()
                .onStart {
                    if (!commonRepository.populateDefaultWatchlist()) {
                        commonRepository.setWatchListIds(listOf("bitcoin", "ethereum", "dogecoin"))
                        commonRepository.donePopulateDefaultWatchlist()
                    }
                }
                .collect {
                    stateFlow.value = it
                }
        }
        MutableStateFlow(emptyList())
    }

    fun getWatchList(): Flow<List<String>> {
        return stateFlow.asStateFlow()
            .filter { commonRepository.populateDefaultWatchlist() }
            .distinctUntilChanged()
    }

    suspend fun add(id: String) {
        val currentState = stateFlow.value.toMutableList()
        currentState.remove(id)
        currentState.add(id)
        commonRepository.setWatchListIds(currentState)
    }

    suspend fun remove(id: String) {
        val currentState = stateFlow.value.toMutableList()
        currentState.remove(id)
        commonRepository.setWatchListIds(currentState)
    }

    suspend fun drag(fromId: String, toId: String) {
        // btc eth dog
        // ex: btc to dog
        // step 1 remove btc
        // step 2 insert btc to dog index
        val currentList = stateFlow.value

        val fromIndex = currentList.indexOf(fromId)
        val toIndex = currentList.indexOf(toId)
        val newList = currentList.toMutableList()
        if (fromIndex != -1 && toIndex != -1) {
            newList.remove(fromId)
            newList.add(toIndex, fromId)
            Timber.d("swap from $fromId to $toId current list: $currentList new list: $newList")
            stateFlow.emit(newList.toList()) // instant reflex the change
            commonRepository.setWatchListIds(newList.toList())
        }
    }

    private var shouldRefreshWatchListWidgets = false

    init {
        scope.launch(Dispatchers.Main) {
            getWatchList()
                .drop(1)
                .flowOn(Dispatchers.IO)
                .collect {
                    shouldRefreshWatchListWidgets = true
                }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onAppBackground() {
        if (shouldRefreshWatchListWidgets) {
            shouldRefreshWatchListWidgets = false
            Timber.d("refresh watch widgets")
            val widgetIds = listOf(
                WatchWidget4x2Provider::class.java,
                WatchWidget4x4Provider::class.java,
            ).flatMap {
                appWidgetManager.getAppWidgetIds(ComponentName(context, it)).toList()
            }

            widgetIds.forEach { widgetId ->
                scope.launch {
                    watchWidgetHandler.enqueueRefreshWidget(widgetId)
                }
            }
        }
    }

    init {
        // avoid called from background thread
        scope.launch(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this@WatchListRepository)
        }
    }
}
