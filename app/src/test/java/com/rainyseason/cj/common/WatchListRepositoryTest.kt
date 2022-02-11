package com.rainyseason.cj.common

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.WorkManagerTestInitHelper
import com.rainyseason.cj.AppProvides
import com.rainyseason.cj.CJApplication
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.common.model.Watchlist
import com.rainyseason.cj.common.model.WatchlistCollection
import com.rainyseason.cj.data.database.kv.KeyValueDao
import com.rainyseason.cj.data.database.kv.KeyValueDatabase
import com.rainyseason.cj.data.database.kv.KeyValueStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class WatchListRepositoryTest {

    private lateinit var context: Context
    private lateinit var keyValueDatabase: KeyValueDatabase
    private lateinit var keyValueDao: KeyValueDao
    private lateinit var keyValueStore: KeyValueStore
    private lateinit var watchListRepository: WatchListRepository

    @Before
    fun setup() {
        context = getApplicationContext<CJApplication>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        keyValueDatabase = Room.inMemoryDatabaseBuilder(context, KeyValueDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        keyValueDao = keyValueDatabase.entryDao()
        keyValueStore = KeyValueStore(
            { keyValueDao },
            { keyValueDatabase },
            mock()
        )
    }

    private fun initWatchlistRepository() {
        watchListRepository = WatchListRepository(
            context = context,
            appWidgetManager = mock(),
            watchWidgetHandler = mock(),
            keyValueStore = keyValueStore,
            moshi = AppProvides.moshi(),
        )
    }

    @After
    fun after() {
        keyValueDatabase.close()
    }

    @Test
    fun `fresh app install`() = runBlocking {
        initWatchlistRepository()
        val collection = watchListRepository.getWatchlistCollection()
        Assert.assertEquals(WatchlistCollection.EMPTY, collection)
    }

    @Test
    fun `fresh app install flow`() = runBlocking {
        initWatchlistRepository()
        val collection = watchListRepository.getWatchlistCollectionFlow().first()
        Assert.assertEquals(WatchlistCollection.EMPTY, collection)
    }

    @Test
    fun `with old watchlist ids`() = runBlocking {
        keyValueStore.setBoolean("populate_default_watchlist", true)
        keyValueStore.setString("watchlist_ids", "bitcoin,ethereum,dogecoin")

        initWatchlistRepository()
        val collection = watchListRepository.getWatchlistCollection()

        val expectedCollection = WatchlistCollection(
            listOf(
                Watchlist(
                    id = Watchlist.DEFAULT_ID,
                    coins = "bitcoin,ethereum,dogecoin".split(",")
                        .map { Coin(it) }
                )
            )
        )

        Assert.assertEquals(expectedCollection, collection)
        Assert.assertTrue(keyValueStore.getString("watch_list_ids") == null)
        Assert.assertTrue(keyValueStore.getBoolean("populate_default_watchlist") == null)
    }

    @Test
    fun `with old watchlist ids flow`() = runBlocking {
        keyValueStore.setBoolean("populate_default_watchlist", true)
        keyValueStore.setString("watchlist_ids", "bitcoin,ethereum,dogecoin")

        initWatchlistRepository()
        val collection = watchListRepository.getWatchlistCollectionFlow().first()

        val expectedCollection = WatchlistCollection(
            listOf(
                Watchlist(
                    id = Watchlist.DEFAULT_ID,
                    coins = "bitcoin,ethereum,dogecoin".split(",")
                        .map { Coin(it) }
                )
            )
        )

        Assert.assertEquals(expectedCollection, collection)
        Assert.assertTrue(keyValueStore.getString("watch_list_ids") == null)
        Assert.assertTrue(keyValueStore.getBoolean("populate_default_watchlist") == null)
    }

    @Test
    fun `with old watchlist ids user`() = runBlocking {
        keyValueStore.setBoolean("populate_default_watchlist", true)
        keyValueStore.setString("watchlist_ids", "bitcoin,dogecoin,ethereum,xmr")

        initWatchlistRepository()
        val collection = watchListRepository.getWatchlistCollection()

        val expectedCollection = WatchlistCollection(
            listOf(
                Watchlist(
                    id = Watchlist.DEFAULT_ID,
                    coins = "bitcoin,dogecoin,ethereum,xmr".split(",")
                        .map { Coin(it) }
                )
            )
        )

        Assert.assertEquals(expectedCollection, collection)
        Assert.assertTrue(keyValueStore.getString("watch_list_ids") == null)
        Assert.assertTrue(keyValueStore.getBoolean("populate_default_watchlist") == null)
    }

    @Test
    fun `with old watchlist ids user flow`() = runBlocking {
        keyValueStore.setBoolean("populate_default_watchlist", true)
        keyValueStore.setString("watchlist_ids", "bitcoin,dogecoin,ethereum,xmr")

        initWatchlistRepository()
        val collection = watchListRepository.getWatchlistCollectionFlow().first()

        val expectedCollection = WatchlistCollection(
            listOf(
                Watchlist(
                    id = Watchlist.DEFAULT_ID,
                    coins = "bitcoin,dogecoin,ethereum,xmr".split(",")
                        .map { Coin(it) }
                )
            )
        )

        Assert.assertEquals(expectedCollection, collection)
        Assert.assertTrue(keyValueStore.getString("watch_list_ids") == null)
        Assert.assertTrue(keyValueStore.getBoolean("populate_default_watchlist") == null)
    }

    @Test
    fun `with old empty watchlist ids user`() = runBlocking {
        keyValueStore.setBoolean("populate_default_watchlist", true)
        keyValueStore.setString("watchlist_ids", "")

        initWatchlistRepository()
        val collection = watchListRepository.getWatchlistCollection()

        val expectedCollection = WatchlistCollection(
            listOf(
                Watchlist(
                    id = Watchlist.DEFAULT_ID,
                    coins = emptyList()
                )
            )
        )

        Assert.assertEquals(expectedCollection, collection)
        Assert.assertTrue(keyValueStore.getString("watch_list_ids") == null)
        Assert.assertTrue(keyValueStore.getBoolean("populate_default_watchlist") == null)
    }

    @Test
    fun `with old empty watchlist ids user flow`() = runBlocking {
        keyValueStore.setBoolean("populate_default_watchlist", true)
        keyValueStore.setString("watchlist_ids", "")

        initWatchlistRepository()
        val collection = watchListRepository.getWatchlistCollectionFlow().first()

        val expectedCollection = WatchlistCollection(
            listOf(
                Watchlist(
                    id = Watchlist.DEFAULT_ID,
                    coins = emptyList()
                )
            )
        )

        Assert.assertEquals(expectedCollection, collection)
        Assert.assertTrue(keyValueStore.getString("watch_list_ids") == null)
        Assert.assertTrue(keyValueStore.getBoolean("populate_default_watchlist") == null)
    }
}
