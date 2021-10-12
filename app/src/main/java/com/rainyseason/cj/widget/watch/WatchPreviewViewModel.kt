package com.rainyseason.cj.widget.watch

import android.os.Parcelable
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.getWidgetId
import com.rainyseason.cj.data.CommonRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.parcelize.Parcelize
import timber.log.Timber

data class WatchPreviewState(
    val holder: Int = 1,
) : MavericksState

@Parcelize
data class WatchPreviewArgs(
    val widgetId: Int,
) : Parcelable

class WatchPreviewViewModel @AssistedInject constructor(
    @Assisted val initState: WatchPreviewState,
    @Assisted val args: WatchPreviewArgs,
    private val commonRepository: CommonRepository,
) : MavericksViewModel<WatchPreviewState>(initState) {

    @AssistedFactory
    interface Factory {
        fun create(
            initState: WatchPreviewState,
            args: WatchPreviewArgs,
        ): WatchPreviewViewModel
    }

    init {
        Timber.d("WatchPreviewViewModel created")
    }

    companion object : MavericksViewModelFactory<WatchPreviewViewModel, WatchPreviewState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: WatchPreviewState
        ): WatchPreviewViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext)
                .fragment<WatchPreviewFragment>()
            val factory = fragment.viewModelFactory
            val widgetId = fragment.requireActivity().intent.extras?.getWidgetId()
                ?: throw IllegalArgumentException("missing widget id")
            return factory.create(state, WatchPreviewArgs(widgetId))
        }
    }
}