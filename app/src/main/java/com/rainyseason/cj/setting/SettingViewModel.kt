package com.rainyseason.cj.setting

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.data.UserSetting
import com.rainyseason.cj.data.UserSettingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

data class SettingState(
    val userSetting: Async<UserSetting> = Uninitialized,
) : MavericksState

class SettingViewModel @AssistedInject constructor(
    @Assisted initialState: SettingState,
    private val settingRepository: UserSettingRepository,
) : MavericksViewModel<SettingState>(initialState) {

    fun updateSetting(block: UserSetting.() -> UserSetting) {
        withState { state ->
            val currentSetting = state.userSetting.invoke() ?: return@withState
            val newSetting = block.invoke(currentSetting)
            viewModelScope.launch {
                settingRepository.setUserSetting(newSetting)
            }
        }
    }

    init {
        settingRepository.getUserSettingFlow()
            .execute { copy(userSetting = it) }
    }

    @AssistedFactory
    interface Factory {
        fun create(state: SettingState): SettingViewModel
    }

    companion object : MavericksViewModelFactory<SettingViewModel, SettingState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: SettingState
        ): SettingViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext)
                .fragment<SettingFragment>()
            return fragment.viewModelFactory.create(state)
        }
    }
}