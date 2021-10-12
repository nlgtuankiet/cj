package com.rainyseason.cj.widget.watch

import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber

class WatchController @AssistedInject constructor(
    @Assisted val viewModel: WatchPreviewViewModel,
) : AsyncEpoxyController() {

    override fun buildModels() {
        val state = withState(viewModel) { it }
        Timber.d("state: $state")
    }

    @AssistedFactory
    interface Factory {
        fun create(viewModel: WatchPreviewViewModel): WatchController
    }
}