package com.rainyseason.cj.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchListRepository @Inject constructor() {

    private val state = mutableListOf("bitcoin", "ethereum", "dogecoin")
    private var stateFlow = MutableStateFlow(state.toList())
    fun getWatchList(): Flow<List<String>> {
        return stateFlow.asStateFlow()
    }

    suspend fun add(id: String) {
        delay(500)
        state.remove(id)
        state.add(id)
        stateFlow.value = state.toList()
    }

    suspend fun remove(id: String) {
        delay(500)
        state.remove(id)
        stateFlow.value = state.toList()
    }
}
