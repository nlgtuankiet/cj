package com.rainyseason.cj.watch

import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.watch.view.watchEntryView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class WatchListController @AssistedInject constructor(
    @Assisted val viewModel: WatchListViewModel,
) : AsyncEpoxyController() {
    override fun buildModels() {
        val state = withState(viewModel) { it }
        val list = state.markets.invoke().orEmpty()
        list.forEach { entry ->
            watchEntryView {
                id(entry.id)
                symbol(entry.symbol)
                name(entry.name)
            }

        }
    }


    @AssistedFactory
    interface Factory {
        fun create(viewModel: WatchListViewModel): WatchListController
    }
}