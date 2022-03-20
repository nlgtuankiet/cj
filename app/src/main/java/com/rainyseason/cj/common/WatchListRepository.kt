package com.rainyseason.cj.common

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.common.model.Watchlist
import com.rainyseason.cj.common.model.WatchlistCollection
import com.rainyseason.cj.data.coc.CoinOmegaCoinService
import com.rainyseason.cj.data.database.kv.KeyValueStore
import com.rainyseason.cj.widget.watch.WatchWidget4x2Provider
import com.rainyseason.cj.widget.watch.WatchWidget4x4Provider
import com.rainyseason.cj.widget.watch.WatchWidgetHandler
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchListRepository @Inject constructor(
    private val context: Context,
    private val appWidgetManager: AppWidgetManager,
    private val watchWidgetHandler: WatchWidgetHandler,
    private val keyValueStore: KeyValueStore,
    private val coinOmegaCoinService: CoinOmegaCoinService,
    private val firebaseAuth: FirebaseAuth,
    private val moshi: Moshi,
) : LifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val legacyPopulateDefaultWatchList = "populate_default_watchlist"
    private val legacyWatchListIdsKey = "watchlist_ids"
    private val watchlistCollectionKey = "watchlist_collection"
    private val watchlistCollectionAdapter = moshi.adapter(WatchlistCollection::class.java)
    private var shouldRefreshWatchListWidgets = false

    private val backupTrigger = Channel<Unit>(1, BufferOverflow.DROP_OLDEST)
    private val firstBackupDoneKey = "watchlist_first_backup_done"

    private val migrateToV2Job: Job = scope.launch {
        migrateToV2()
    }

    private suspend fun waitForMigrate() {
        migrateToV2Job.join()
    }

    private suspend fun migrateToV2() {
        val legacyIds = keyValueStore.getString(legacyWatchListIdsKey) ?: return
        val ids = legacyIds.split(",").filter { key -> key.isNotBlank() }
        val watchList = Watchlist(
            id = Watchlist.DEFAULT_ID,
            name = null,
            coins = ids.map {
                Coin(
                    id = it,
                    backend = Backend.CoinGecko
                )
            }
        )
        addOrReplaceWatchlist(
            watchList,
            false
        )
        keyValueStore.delete(legacyWatchListIdsKey)
        keyValueStore.delete(legacyPopulateDefaultWatchList)
    }

    private suspend fun addOrReplaceWatchlist(
        watchlist: Watchlist,
        waitForMigration: Boolean = true,
    ) {
        if (waitForMigration) {
            waitForMigrate()
        }
        val collection = getWatchlistCollection(waitForMigration = waitForMigration)
        val list = collection.list.toMutableList()
        val oldIndex = list.indexOfFirst { it.id == watchlist.id }
        if (oldIndex != -1) {
            list[oldIndex] = watchlist
        } else {
            list.add(watchlist)
        }
        val newCollection = collection.copy(list = list)
        val newJson = watchlistCollectionAdapter.toJson(newCollection)
        keyValueStore.setString(watchlistCollectionKey, newJson)
        triggerWatchlistBackup()
    }

    suspend fun getWatchlistCollection(
        waitForMigration: Boolean = true,
    ): WatchlistCollection {
        if (waitForMigration) {
            waitForMigrate()
        }
        val json = keyValueStore.getString(watchlistCollectionKey)
        return decodeWatchlistCollection(json)
    }

    private fun decodeWatchlistCollection(json: String?): WatchlistCollection {
        if (json.isNullOrBlank()) {
            return WatchlistCollection.EMPTY
        }
        return watchlistCollectionAdapter.fromJson(json).notNull()
    }

    fun getWatchlistCollectionFlow(): Flow<WatchlistCollection> {
        return keyValueStore.getStringFlow(watchlistCollectionKey)
            .onStart { waitForMigrate() }
            .map {
                decodeWatchlistCollection(it)
            }
            .distinctUntilChanged()
    }

    /**
     * If [watchlistId] is not found then create a new one and add to it
     */
    suspend fun addOrRemove(coin: Coin, watchlistId: String = Watchlist.DEFAULT_ID) {
        waitForMigrate()
        val watchList = findWatchlist(watchlistId)
            ?: Watchlist(id = watchlistId, coins = emptyList())
        val coins = watchList.coins.toMutableList()
        val index = coins.indexOfFirst { it == coin }
        if (index == -1) {
            coins.add(coin)
        } else {
            coins.removeAt(index)
        }
        val newWatchlist = watchList.copy(coins = coins)
        addOrReplaceWatchlist(newWatchlist)
    }

    private suspend fun findWatchlist(watchlistId: String): Watchlist? {
        return getWatchlistCollection(waitForMigration = true).list
            .firstOrNull { it.id == watchlistId } ?: if (watchlistId == Watchlist.DEFAULT_ID) {
            Watchlist.DEFAULT_EMPTY
        } else {
            null
        }
    }

    suspend fun remove(coin: Coin, watchlistId: String = Watchlist.DEFAULT_ID) {
        waitForMigrate()
        val watchList = getWatchlistCollection(waitForMigration = true).list
            .firstOrNull { it.id == watchlistId } ?: return
        val newCoins = watchList.coins.filter { it != coin }
        val newWatchlist = watchList.copy(coins = newCoins)
        addOrReplaceWatchlist(newWatchlist)
    }

    suspend fun drag(from: Coin, to: Coin, watchlistId: String = Watchlist.DEFAULT_ID) {
        waitForMigrate()
        val watchlist = findWatchlist(watchlistId) ?: return
        val coins = watchlist.coins.toMutableList()
        val fromIndex = coins.indexOf(from)
        val toIndex = coins.indexOf(to)
        if (fromIndex != -1 && toIndex != -1) {
            coins.remove(from)
            coins.add(toIndex, from)
            val newWatchlist = watchlist.copy(coins = coins)
            addOrReplaceWatchlist(newWatchlist)
        }
    }

    init {
        scope.launch(Dispatchers.Main) {
            getWatchlistCollectionFlow()
                .drop(1)
                .flowOn(Dispatchers.IO)
                .collect {
                    shouldRefreshWatchListWidgets = true
                }
        }
        scope.launch {
            for (event in backupTrigger) {
                try {
                    backupWatchlist()
                } catch (ex: Exception) {
                    if (BuildConfig.DEBUG) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        // check for first backup
        scope.launch {
            val firstBackupDone = keyValueStore.getBoolean(firstBackupDoneKey) ?: false
            if (!firstBackupDone) {
                triggerWatchlistBackup()
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

    private fun triggerWatchlistBackup() {
        backupTrigger.trySend(Unit)
    }

    private suspend fun backupWatchlist() {
        Timber.d("Start backup watchlist")
        ensureLogin()
        val collection = getWatchlistCollection()
        coinOmegaCoinService.backupWatchlist(collection)
        keyValueStore.setBoolean(firstBackupDoneKey, true)
        Timber.d("Backup watchlist done")
    }

    private suspend fun ensureLogin() {
        if (firebaseAuth.currentUser != null) {
            return
        }
        firebaseAuth.signInAnonymously().await()
    }

    init {
        // avoid called from background thread
        scope.launch(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this@WatchListRepository)
        }
    }
}
