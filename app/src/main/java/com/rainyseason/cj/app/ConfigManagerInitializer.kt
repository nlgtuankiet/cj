package com.rainyseason.cj.app

import com.rainyseason.cj.common.ConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class ConfigManagerInitializer @Inject constructor(
    private val configManagerProvider: Provider<ConfigManager>,
    private val scope: CoroutineScope,
) : Function0<Unit> {
    override fun invoke() {
        scope.launch {
            configManagerProvider.get()
        }
    }
}
