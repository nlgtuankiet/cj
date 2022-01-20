package com.rainyseason.cj.widget.watch.fullsize

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.airbnb.epoxy.IdUtils
import com.rainyseason.cj.common.widgetId
import com.rainyseason.cj.widget.watch.WatchConfig
import com.rainyseason.cj.widget.watch.WatchDisplayEntry
import com.rainyseason.cj.widget.watch.WatchWidgetRender
import com.rainyseason.cj.widget.watch.WatchWidgetRepository
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@Module
interface WatchWidgetServiceModule {
    @ContributesAndroidInjector
    fun service(): WatchWidgetService
}

class WatchWidgetService : RemoteViewsService() {

    @Inject
    lateinit var factory: WatchRemoteViewFactory.Factory

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")
        AndroidInjection.inject(this)
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetId = intent.widgetId() ?: throw IllegalStateException("Missing widgetId")
        return factory.create(widgetId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
    }
}

private sealed class WatchWidgetViewModel

private data class WatchWidgetEntryViewModel(
    val data: WatchDisplayEntry,
    val config: WatchConfig,
) : WatchWidgetViewModel()

private data class WatchWidgetSeparator(
    val config: WatchConfig,
) : WatchWidgetViewModel()

class WatchRemoteViewFactory @AssistedInject constructor(
    @Assisted private val widgetId: Int,
    private val watchWidgetRepository: WatchWidgetRepository,
    private val renderer: WatchWidgetRender,
) : RemoteViewsService.RemoteViewsFactory {

    private var models: List<WatchWidgetViewModel> = emptyList()

    // only on create is main thread
    // other is background thread
    override fun onCreate() {
        Timber.d("onCreate")
    }

    // should load data
    @OptIn(ExperimentalStdlibApi::class)
    override fun onDataSetChanged() {
        Timber.d("onDataSetChanged")
        runBlocking {
            val data = watchWidgetRepository.getDisplayData(widgetId)
            val config = watchWidgetRepository.getConfig(widgetId)
            if (data == null || config == null) {
                models = emptyList()
                return@runBlocking
            }
            var skipSeparator = true
            models = buildList {
                data.entries.forEach { entry ->
                    if (skipSeparator) {
                        skipSeparator = false
                    } else {
                        add(WatchWidgetSeparator(config))
                    }
                    add(WatchWidgetEntryViewModel(entry, config))
                }
            }
        }
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
    }

    override fun getCount(): Int {
        Timber.d("getCount")
        return models.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        Timber.d("getViewAt $position")
        val model = models.getOrNull(position) ?: return renderer.createEmptyView()
        return when (model) {
            is WatchWidgetEntryViewModel -> renderer.createFullSizeEntryView(
                entry = model.data,
                config = model.config,
            )
            is WatchWidgetSeparator -> renderer.createFullSizeSeparatorView(config = model.config)
        }
    }

    override fun getLoadingView(): RemoteViews {
        Timber.d("getLoadingView")
        return renderer.createEmptyView()
    }

    override fun getViewTypeCount(): Int {
        Timber.d("getViewTypeCount")
        return 2
    }

    override fun getItemId(position: Int): Long {
        Timber.d("getItemId $position")
        val model = models.getOrNull(position) ?: return -1
        return when (model) {
            is WatchWidgetEntryViewModel -> "entry_${model.data.coinId}"
            is WatchWidgetSeparator -> "separator_$position"
        }.let { IdUtils.hashString64Bit(it) }
    }

    override fun hasStableIds(): Boolean {
        Timber.d("hasStableIds")
        return true
    }

    @AssistedFactory
    interface Factory {
        fun create(widgetId: Int): WatchRemoteViewFactory
    }
}
